/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.micaConfig.service;

import javax.inject.Inject;

import org.obiba.magma.NoSuchVariableException;
import org.obiba.mica.dataset.domain.Dataset;
import org.obiba.mica.dataset.service.CollectedDatasetService;
import org.obiba.mica.dataset.service.HarmonizedDatasetService;
import org.obiba.mica.security.event.SubjectAclUpdatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.google.common.eventbus.EventBus;

@Component
public class CacheService {

  private static final Logger log = LoggerFactory.getLogger(CacheService.class);

  @Inject
  private Helper helper;

  @Inject
  private HarmonizedDatasetService harmonizedDatasetService;

  @Inject
  private CollectedDatasetService collectedDatasetService;

  @Inject
  private EventBus eventBus;

  @Inject
  private TaxonomyService taxonomyService;

  @CacheEvict(value = "opal-taxonomies", allEntries = true, beforeInvocation = true)
  public void clearOpalTaxonomiesCache() {
    log.info("Clearing opal taxonomies cache");
    taxonomyService.getOpalTaxonomies();
  }

  @CacheEvict(value = "micaConfig", allEntries = true)
  public void clearMicaConfigCache() {
    log.info("Clearing mica config cache");
    taxonomyService.refresh();
  }

  @CacheEvict(value = "aggregations-metadata", allEntries = true)
  public void clearAggregationsMetadataCache() {
    log.info("Clearing aggregations metadata cache");
  }

  public void clearDatasetVariablesCache() {
    harmonizedDatasetService.findAllDatasets().forEach(dataset -> helper.clearDatasetVariablesCache(dataset));
    collectedDatasetService.findAllDatasets().forEach(dataset -> helper.clearDatasetVariablesCache(dataset));
  }

  public void clearAuthorizationCache() {
    eventBus.post(new SubjectAclUpdatedEvent());
  }

  public void buildDatasetVariablesCache() {
    helper.buildDatasetVariablesCache();
  }

  public void clearAllCaches() {
    log.info("Clearing all caches");
    clearOpalTaxonomiesCache();
    clearMicaConfigCache();
    clearAggregationsMetadataCache();
    clearDatasetVariablesCache();
    clearAuthorizationCache();
  }

  @Component
  public static class Helper {

    private static final Logger log = LoggerFactory.getLogger(CacheService.Helper.class);

    @Inject
    private HarmonizedDatasetService harmonizedDatasetService;

    @Inject
    private CollectedDatasetService collectedDatasetService;

    @CacheEvict(value = "dataset-variables", cacheResolver = "datasetVariablesCacheResolver", allEntries = true, beforeInvocation = true)
    public void clearDatasetVariablesCache(Dataset dataset) {
      log.info("Clearing dataset variables cache dataset-{}", dataset.getId());
    }

    @Async
    public void buildDatasetVariablesCache() {
      harmonizedDatasetService.findAllPublishedDatasets().forEach(
        dataset -> harmonizedDatasetService.getDatasetVariables(dataset)
          .forEach(v -> dataset.getBaseStudyTables().forEach(st -> {
            String studyId = st.getStudyId();
            try {
              harmonizedDatasetService
                .getVariableSummary(dataset, v.getName(), studyId, st.getProject(), st.getTable());
            } catch(NoSuchVariableException ex) {
              //ignore
            } catch(Exception e) {
              log.warn("Error building dataset variable cache of harmonization dataset {}: {} {}", dataset.getId(), st,
                v, e);
            }
          })));

      collectedDatasetService.findAllDatasets()
        .forEach(dataset -> collectedDatasetService.getDatasetVariables(dataset).forEach(v -> {
          try {
            collectedDatasetService.getVariableSummary(dataset, v.getName());
          } catch(NoSuchVariableException ex) {
            //ignore
          } catch(Exception e) {
            log.warn("Error building dataset variable cache of study dataset {}: {}", dataset.getId(), v, e);
          }
        }));
    }
  }
}
