/*
 * Copyright (c) 2014 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.search.queries;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import javax.inject.Inject;

import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.obiba.mica.dataset.domain.Dataset;
import org.obiba.mica.dataset.search.DatasetIndexer;
import org.obiba.mica.dataset.service.PublishedDatasetService;
import org.obiba.mica.search.CountStatsData;
import org.obiba.mica.search.DatasetIdProvider;
import org.obiba.mica.search.rest.QueryDtoHelper;
import org.obiba.mica.web.model.Dtos;
import org.obiba.mica.web.model.Mica;
import org.obiba.mica.web.model.MicaSearch;
import org.springframework.context.annotation.Scope;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import static org.obiba.mica.search.CountStatsDtoBuilders.DatasetCountStatsBuilder;
import static org.obiba.mica.web.model.MicaSearch.DatasetResultDto;
import static org.obiba.mica.web.model.MicaSearch.QueryResultDto;

@Component
@Scope("request")
public class DatasetQuery extends AbstractDocumentQuery {


  private static final String DATASET_FACETS_YML = "dataset-facets.yml";

  public static final String STUDY_JOIN_FIELD = "studyTable.studyId";

  public static final String HARMONIZATION_JOIN_FIELD = "studyTables.studyId";

  @Inject
  Dtos dtos;

  @Inject
  PublishedDatasetService publishedDatasetService;

  private DatasetIdProvider datasetIdProvider;

  @Override
  public String getSearchIndex() {
    return DatasetIndexer.PUBLISHED_DATASET_INDEX;
  }

  @Override
  public String getSearchType() {
    return DatasetIndexer.DATASET_TYPE;
  }

  public void initialize(MicaSearch.QueryDto query, DatasetIdProvider provider) {
    datasetIdProvider = provider;
    initialize(query);
  }

  @Override
  public List<String> query(List<String> studyIds, CountStatsData counts, Scope scope) throws IOException {
    updateDatasetQuery();
    return super.query(studyIds, counts, scope);
  }

  @Override
  protected Resource getAggregationsDescription() {
    return new ClassPathResource(DATASET_FACETS_YML);
  }

  @Override
  public void processHits(QueryResultDto.Builder builder, SearchHits hits, Scope scope,
      CountStatsData counts) {
    DatasetResultDto.Builder resBuilder = DatasetResultDto.newBuilder();
    DatasetCountStatsBuilder datasetCountStatsBuilder = counts == null
        ? null
        : DatasetCountStatsBuilder.newBuilder(counts);

    Consumer<Dataset> addDto = getDatasetConsumer(scope, resBuilder, datasetCountStatsBuilder);

    for (SearchHit hit : hits) {
      addDto.accept(publishedDatasetService.findById(hit.getId()));
    }

    builder.setExtension(DatasetResultDto.result, resBuilder.build());
  }

  private void updateDatasetQuery() {
    List<String> datasetIds = datasetIdProvider.getDatasetIds();
    if(datasetIds.size() > 0) {
      if(queryDto == null) {
        queryDto = QueryDtoHelper
            .createTermFiltersQuery(Arrays.asList("id"), datasetIds, QueryDtoHelper.BoolQueryType.MUST);
      } else {
        queryDto = QueryDtoHelper
            .addTermFilters(queryDto, QueryDtoHelper.createTermFilters(Arrays.asList("id"), datasetIds),
                QueryDtoHelper.BoolQueryType.MUST);
      }
    }
  }

  private Consumer<Dataset> getDatasetConsumer(Scope scope, DatasetResultDto.Builder resBuilder,
      DatasetCountStatsBuilder datasetCountStatsBuilder) {

    return scope == Scope.DETAIL
      ? (dataset) -> {
        Mica.DatasetDto.Builder datasetBuilder = dtos.asDtoBuilder(dataset);
        if(datasetCountStatsBuilder != null) {
          datasetBuilder
              .setExtension(MicaSearch.CountStatsDto.datasetCountStats, datasetCountStatsBuilder.build(dataset))
              .build();
        }
        resBuilder.addDatasets(datasetBuilder.build());
      }
      : (dataset) -> resBuilder.addDigests(dtos.asDigestDtoBuilder(dataset).build());
  }

  @Override
  protected List<String> getJoinFields() {
    return Arrays.asList(STUDY_JOIN_FIELD, HARMONIZATION_JOIN_FIELD);
  }

  public Map<String, Integer> getStudyCounts() {
    return getDocumentCounts(STUDY_JOIN_FIELD);
  }

  public Map<String, Integer> getHarmonizationStudyCounts() {
    return getDocumentCounts(HARMONIZATION_JOIN_FIELD);
  }

}
