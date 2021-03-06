package org.obiba.mica.web.controller.domain;

import org.joda.time.DateTime;
import org.obiba.mica.access.domain.DataAccessEntityStatus;
import org.obiba.mica.access.domain.DataAccessRequest;

public class DataAccessRequestBundle {

  private final String id;
  private final DataAccessRequest request;
  private final String title;
  private final int totalAmendments;
  private final int pendingAmendments;
  private final int totalFeasibilities;
  private final int pendingFeasibilities;
  private DateTime submitDate;

  public DataAccessRequestBundle(DataAccessRequest request, String title, int totalAmendments, int pendingAmendments, int totalFeasibilities, int pendingFeasibilities) {
    this.id = request.getId();
    this.request = request;
    this.title = title;
    this.submitDate = request.getSubmissionDate();
    this.totalAmendments = totalAmendments;
    this.pendingAmendments = pendingAmendments;
    this.totalFeasibilities = totalFeasibilities;
    this.pendingFeasibilities = pendingFeasibilities;
  }

  public String getId() {
    return id;
  }

  public String getApplicant() {
    return request.getApplicant();
  }

  public DataAccessRequest getRequest() {
    return request;
  }

  public String getTitle() {
    return title;
  }

  public DateTime getLastUpdate() {
    return request.getLastModifiedDate();
  }

  public DateTime getSubmitDate() {
    return submitDate;
  }

  public DataAccessEntityStatus getStatus() {
    return request.getStatus();
  }

  public int getTotalAmendments() {
    return totalAmendments;
  }

  public int getPendingAmendments() {
    return pendingAmendments;
  }

  public int getTotalFeasibilities() {
    return totalFeasibilities;
  }

  public int getPendingFeasibilities() {
    return pendingFeasibilities;
  }

  public boolean isArchived() {
    return request.isArchived();
  }
}
