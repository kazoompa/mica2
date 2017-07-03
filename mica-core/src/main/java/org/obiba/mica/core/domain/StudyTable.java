/*
 * Copyright (c) 2017 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.core.domain;

import java.io.Serializable;

import com.google.common.base.MoreObjects;

/**
 * Represents a opal table that is associated to a {@link org.obiba.mica.study.domain.Study}
 * {@link org.obiba.mica.study.domain.Population} {@link org.obiba.mica.study.domain.DataCollectionEvent}.
 */
public class StudyTable extends BaseStudyTable implements Serializable {

  private static final long serialVersionUID = -2466526849186256653L;

  private String dataCollectionEventId;

  /**
   * {@link org.obiba.mica.study.domain.DataCollectionEvent} unique ID (including {@link org.obiba.mica.study.domain.Study} ID
   * and {@link org.obiba.mica.study.domain.Population} ID.
   *
   * @return
   */
  public String getDataCollectionEventUId() {
    return getDataCollectionEventUId(getStudyId(), getPopulationId(), getDataCollectionEventId());
  }

  public void setDataCollectionEventUId(String ignored) {
    // for jackson serializer
  }

  public String getDataCollectionEventId() {
    return dataCollectionEventId;
  }

  public void setDataCollectionEventId(String dataCollectionEventId) {
    this.dataCollectionEventId = dataCollectionEventId;
  }

  public boolean appliesTo(String studyId, String populationId, String dataCollectionEventId) {
    return this.studyId.equals(studyId) && this.populationId.equals(populationId) &&
      this.dataCollectionEventId.equals(dataCollectionEventId);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this).add("project", getProject()).add("table", getTable())
      .add("dceId", getDataCollectionEventUId()).toString();
  }

}
