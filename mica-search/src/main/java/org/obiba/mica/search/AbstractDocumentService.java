/*
 * Copyright (c) 2017 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.search;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.index.IndexNotFoundException;
import org.elasticsearch.index.query.*;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.obiba.mica.core.service.DocumentService;
import org.obiba.mica.micaConfig.service.MicaConfigService;
import org.obiba.mica.security.service.SubjectAclService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.util.locale.LanguageTag;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class AbstractDocumentService<T> implements DocumentService<T> {

  private static final Logger log = LoggerFactory.getLogger(AbstractDocumentService.class);

  protected static final int MAX_SIZE = 10000;

  @Inject
  protected SearchEngineClient client;

  @Inject
  protected MicaConfigService micaConfigService;

  @Inject
  protected SubjectAclService subjectAclService;

  @Override
  @Nullable
  public T findById(String id) {
    log.debug("findById {} {}", getClass(), id);
    List<T> results = findByIds(Collections.singletonList(id));
    return results != null && results.size() > 0 ? results.get(0) : null;
  }

  @Override
  public List<T> findAll() {
    log.debug("findAll {}", getClass());
    return executeQuery(QueryBuilders.matchAllQuery(), 0, MAX_SIZE);
  }

  @Override
  public List<T> findByIds(List<String> ids) {
    log.debug("findByIds {} {} ids", getClass(), ids.size());
    return executeQueryByIds(buildFilteredQuery(ids), 0, MAX_SIZE, ids);
  }

  @Override
  public Documents<T> find(int from, int limit, @Nullable String sort, @Nullable String order, @Nullable String studyId,
                           @Nullable String queryString) {
    return find(from, limit, sort, order, studyId, queryString, null);
  }

  @Override
  public Documents<T> find(int from, int limit, @Nullable String sort, @Nullable String order, @Nullable String studyId,
                           @Nullable String queryString, @Nullable List<String> fields) {
    return find(from, limit, sort, order, studyId, queryString, fields, null);
  }

  @Override
  public Documents<T> find(int from, int limit, @Nullable String sort, @Nullable String order, @Nullable String studyId,
                           @Nullable String queryString, @Nullable List<String> fields, @Nullable List<String> excludedFields) {
    if (!indexExists()) return new Documents<>(0, from, limit);

    QueryStringQueryBuilder query = queryString != null ? QueryBuilders.queryStringQuery(queryString) : null;

    if (query != null && fields != null) fields.forEach(query::field);

    QueryBuilder postFilter = getPostFilter(studyId);

    QueryBuilder execQuery = postFilter == null ? query : query == null ? postFilter : QueryBuilders.boolQuery().must(query).filter(postFilter);

    if (excludedFields != null) {
      BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
      excludedFields.forEach(f -> boolQueryBuilder.mustNot(
          QueryBuilders.boolQuery().must(QueryBuilders.termQuery(f, "true")).must(QueryBuilders.existsQuery(f))));
      execQuery = boolQueryBuilder.must(execQuery);
    }

    SearchRequestBuilder search = client.prepareSearch() //
        .setIndices(getIndexName()) //
        .setTypes(getType()) //
        .setQuery(execQuery) //
        .setFrom(from) //
        .setSize(limit);

    if (sort != null) {
      search.addSort(
          SortBuilders.fieldSort(sort).order(order == null ? SortOrder.ASC : SortOrder.valueOf(order.toUpperCase())));
    }

    log.debug("Request /{}/{}", getIndexName(), getType());
    if (log.isTraceEnabled()) log.trace("Request /{}/{}: {}", getIndexName(), getType(), search.toString());
    SearchResponse response = search.execute().actionGet();
    Documents<T> documents = new Documents<>(Long.valueOf(response.getHits().getTotalHits()).intValue(), from, limit);
    log.debug("Response /{}/{}", getIndexName(), getType());
    if (log.isTraceEnabled())
      log.trace("Response /{}/{}: totalHits={}", getIndexName(), getType(), response.getHits().getTotalHits());

    response.getHits().forEach(hit -> {
      try {
        documents.add(processHit(hit));
      } catch (IOException e) {
        log.error("Failed processing found hits.", e);
      }
    });

    return documents;
  }

  @Override
  public List<String> getDefaultLocalizedFields() {
    return Lists.newArrayList("acronym", "name");
  }

  @Override
  public List<String> suggest(int limit, String locale, String queryString) {
    List<String> suggestions = Lists.newArrayList();
    // query default fields separately otherwise we do not know which field has matched and suggestion might not be correct
    getDefaultLocalizedFields().forEach(df -> suggestions.addAll(suggest(limit, locale, queryString, df)));
    return suggestions;
  }

  private List<String> suggest(int limit, String locale, String queryString, String defaultFieldName) {
    String fieldName = defaultFieldName + "." + locale;

    QueryBuilder queryExec = QueryBuilders.queryStringQuery(queryString)
        .defaultField(fieldName + ".analyzed")
        .defaultOperator(QueryStringQueryBuilder.Operator.OR);

    SearchRequestBuilder search = client.prepareSearch() //
        .setIndices(getIndexName()) //
        .setTypes(getType()) //
        .setQuery(queryExec) //
        .setFrom(0) //
        .setSize(limit)
        .addSort(SortBuilders.scoreSort().order(SortOrder.DESC))
        .setFetchSource(new String[]{fieldName}, null);

    log.debug("Request /{}/{}", getIndexName(), getType());
    if (log.isTraceEnabled()) log.trace("Request /{}/{}: {}", getIndexName(), getType(), search.toString());
    SearchResponse response = search.execute().actionGet();

    List<String> names = Lists.newArrayList();
    response.getHits().forEach(hit -> {
          String value = ((Map<String, Object>) hit.getSource().get(defaultFieldName)).get(locale).toString().toLowerCase();
          names.add(Joiner.on(" ").join(Splitter.on(" ").trimResults().splitToList(value).stream()
              .filter(str -> !str.contains("[") && !str.contains("(") && !str.contains("{") && !str.contains("]") && !str.contains(")") && !str.contains("}"))
              .map(str -> str.replace(":", "").replace(",", ""))
              .filter(str -> !str.isEmpty()).collect(Collectors.toList())));
        }
    );
    return names;
  }

  @Override
  public long getCount() {
    return getCount(QueryBuilders.matchAllQuery());
  }

  protected long getCount(QueryBuilder builder) {
    try {
      SearchRequestBuilder search = client.prepareSearch().setIndices(getIndexName()).setTypes(getType())
          .setQuery(builder).setFrom(0).setSize(0);

      SearchResponse response = search.execute().actionGet();

      return response.getHits().getTotalHits();
    } catch (ElasticsearchException e) {
      return 0;
    }
  }

  /**
   * Turns a search hit into document's pojo.
   *
   * @param hit
   * @return
   * @throws IOException
   */
  protected abstract T processHit(SearchHit hit) throws IOException;

  /**
   * Get the index where the search must take place.
   *
   * @return
   */
  protected abstract String getIndexName();

  /**
   * Get the document type name.
   *
   * @return
   */
  protected abstract String getType();

  /**
   * If access check apply, make it a filter for the corresponding searched type.
   *
   * @return
   */
  @Nullable
  protected QueryBuilder filterByAccess() {
    return null;
  }

  protected List<T> executeQuery(QueryBuilder queryBuilder, int from, int size) {
    return executeQueryInternal(queryBuilder, from, size, null);
  }

  private List<T> executeQueryByIds(QueryBuilder queryBuilder, int from, int size, List<String> ids) {
    return executeQueryInternal(queryBuilder, from, size, ids);
  }

  private boolean indexExists() {
    return client.admin().indices().prepareExists(getIndexName()).get().isExists();
  }

  private List<T> executeQueryInternal(QueryBuilder queryBuilder, int from, int size, List<String> ids) {
    QueryBuilder accessFilter = filterByAccess();

    SearchRequestBuilder requestBuilder = client.prepareSearch(getIndexName()) //
        .setTypes(getType()) //
        .setSearchType(SearchType.DFS_QUERY_THEN_FETCH) //
        .setQuery(
            accessFilter == null ? queryBuilder : QueryBuilders.boolQuery().must(queryBuilder).must(accessFilter)) //
        .setFrom(from) //
        .setSize(size);

    try {
      log.debug("Request /{}/{}", getIndexName(), getType());
      if (log.isTraceEnabled()) log.trace("Request /{}/{}: {}", getIndexName(), getType(), requestBuilder);
      SearchResponse response = requestBuilder.execute().actionGet();
      log.debug("Response /{}/{}", getIndexName(), getType());
      if (log.isTraceEnabled())
        log.trace("Response /{}/{}: totalHits={}", getIndexName(), getType(), response.getHits().getTotalHits());

      SearchHits hits = response.getHits();
      return ids == null || ids.size() != hits.totalHits()
          ? processHits(response.getHits())
          : processHitsOrderByIds(response.getHits(), ids);
    } catch (IndexNotFoundException e) {
      return Lists.newArrayList(); //ignoring
    }
  }

  protected List<T> processHitsOrderByIds(SearchHits hits, List<String> ids) {
    TreeMap<Integer, T> documents = new TreeMap<>();

    hits.forEach(hit -> {
      try {
        int position = ids.indexOf(hit.getId());
        if (position != -1) documents.put(position, processHit(hit));
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    });

    return documents.entrySet().stream().map(Map.Entry::getValue).collect(Collectors.toList());
  }

  protected List<T> processHits(SearchHits hits) {
    List<T> documents = Lists.newArrayList();
    hits.forEach(hit -> {
      try {
        documents.add(processHit(hit));
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    });

    return documents;
  }

  protected QueryBuilder buildFilteredQuery(List<String> ids) {
    IdsQueryBuilder builder = QueryBuilders.idsQuery(getType());
    ids.forEach(builder::addIds);
    return builder;
  }

  @Nullable
  private QueryBuilder getPostFilter(@Nullable String studyId) {
    QueryBuilder filter = filterByAccess();

    if (studyId != null) {
      QueryBuilder filterByStudy = filterByStudy(studyId);
      filter = filter == null ? filterByStudy : QueryBuilders.boolQuery().must(filter).must(filterByStudy);
    }

    return filter;
  }

  protected QueryBuilder filterByStudy(String studyId) {
    return QueryBuilders.termQuery("studyIds", studyId);
  }

  protected List<String> getLocalizedFields(String... fieldNames) {
    List<String> fields = Lists.newArrayList();
    Stream.concat(micaConfigService.getConfig().getLocalesAsString().stream(), Stream.of(LanguageTag.UNDETERMINED))
        .forEach(locale -> {
          Arrays.stream(fieldNames).forEach(f -> {
            fields.add(f + "." + locale + ".analyzed");
          });
        });
    return fields;
  }
}
