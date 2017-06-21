/*
 * Copyright (c) 2017 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.micaConfig.service.helper;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.obiba.mica.core.domain.AbstractGitPersistable;
import org.obiba.mica.study.domain.BaseStudy;
import org.obiba.mica.study.service.PublishedStudyService;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import static org.obiba.mica.security.SubjectUtils.sudo;

@Component
public class StudyIdAggregationMetaDataHelper extends AbstractIdAggregationMetaDataHelper {

  @Inject
  private PublishedStudyService publishedStudyService;

  @Cacheable(value = "aggregations-metadata", key = "'study'")
  public Map<String, AggregationMetaDataProvider.LocalizedMetaData> getStudies() {
    List<BaseStudy> studies = sudo(() -> publishedStudyService.findAll());

    return studies.stream().collect(Collectors.toMap(AbstractGitPersistable::getId,
      m -> new AggregationMetaDataProvider.LocalizedMetaData(m.getAcronym(), m.getName())));
  }

  @Override
  protected Map<String, AggregationMetaDataProvider.LocalizedMetaData> getIdAggregationMap() {
    return getStudies();
  }
}
