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

import com.google.common.collect.Lists;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.index.IndexNotFoundException;
import org.elasticsearch.index.query.IdsQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.obiba.mica.core.service.DocumentService;
import org.obiba.mica.micaConfig.service.MicaConfigService;
import org.obiba.mica.security.service.SubjectAclService;
import org.obiba.mica.spi.search.Indexer;
import org.obiba.mica.spi.search.Searcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.util.locale.LanguageTag;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class AbstractDocumentService<T> implements DocumentService<T> {

  private static final Logger log = LoggerFactory.getLogger(AbstractDocumentService.class);

  protected static final int MAX_SIZE = 10000;

  @Inject
  protected Searcher searcher;

  @Inject
  protected Indexer indexer;

  @Inject
  private MicaConfigService micaConfigService;

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

    Searcher.TermFilter studyIdFilter = getStudyIdFilter(studyId);
    Searcher.IdFilter idFilter = getAccessibleIdFilter();

    Searcher.DocumentResults results = searcher.getDocuments(getIndexName(), getType(), from, limit, sort, order, queryString, studyIdFilter, idFilter, fields, excludedFields);

    Documents<T> documents = new Documents<>(Long.valueOf(results.getTotal()).intValue(), from, limit);
    results.getDocuments().forEach(res -> {
      try {
        documents.add(processHit(res));
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
    getDefaultLocalizedFields().forEach(df -> suggestions.addAll(searcher.suggest(getIndexName(), getType(), limit, locale, queryString, df)));
    return suggestions;
  }

  @Override
  public long getCount() {
    return getCountByRql("");
  }

  protected long getCountByRql(String rql) {
    try {
      return searcher.count(getIndexName(), getType(), rql, null).getTotal();
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
  // TODO remove when will be ES independent
  protected T processHit(SearchHit hit) throws IOException {
    return processHit(new Searcher.DocumentResult() {
      @Override
      public String getId() {
        return hit.getId();
      }

      @Override
      public InputStream getSourceInputStream() {
        return new ByteArrayInputStream(hit.getSourceAsString().getBytes());
      }

      @Override
      public String getClassName() {
        return (String) hit.getSource().get("className");
      }
    });
  }

  /**
   * Turns a search result input stream into document's pojo.
   *
   * @param res
   * @return
   * @throws IOException
   */
  protected abstract T processHit(Searcher.DocumentResult res) throws IOException;

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

  /**
   * If access check apply, get the corresponding filter.
   *
   * @return
   */
  @Nullable
  protected Searcher.IdFilter getAccessibleIdFilter() {
    return null;
  }

  protected String getStudyIdField() {
    return "studyIds";
  }

  protected List<T> executeQuery(QueryBuilder queryBuilder, int from, int size) {
    return executeQueryInternal(queryBuilder, from, size, null);
  }

  protected boolean isOpenAccess() {
    return micaConfigService.getConfig().isOpenAccess();
  }

  //
  // Private methods
  //

  private List<T> executeQueryByIds(QueryBuilder queryBuilder, int from, int size, List<String> ids) {
    return executeQueryInternal(queryBuilder, from, size, ids);
  }

  private boolean indexExists() {
    return indexer.hasIndex(getIndexName());
  }

  private List<T> executeQueryInternal(QueryBuilder queryBuilder, int from, int size, List<String> ids) {
    QueryBuilder accessFilter = filterByAccess();

    SearchRequestBuilder requestBuilder = searcher.prepareSearch(getIndexName()) //
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

  private List<T> processHitsOrderByIds(SearchHits hits, List<String> ids) {
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

  private List<T> processHits(SearchHits hits) {
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

  private QueryBuilder buildFilteredQuery(List<String> ids) {
    IdsQueryBuilder builder = QueryBuilders.idsQuery(getType());
    ids.forEach(builder::addIds);
    return builder;
  }

  private Searcher.TermFilter getStudyIdFilter(@Nullable final String studyId) {
    if (studyId == null) return null;
    return new Searcher.TermFilter() {
      @Override
      public String getField() {
        return getStudyIdField();
      }

      @Override
      public String getValue() {
        return studyId;
      }
    };
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
