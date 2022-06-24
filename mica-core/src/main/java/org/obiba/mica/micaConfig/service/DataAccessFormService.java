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

import org.joda.time.DateTime;
import org.obiba.mica.file.FileStoreService;
import org.obiba.mica.micaConfig.domain.DataAccessForm;
import org.obiba.mica.micaConfig.repository.DataAccessFormRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.Optional;

@Component
public class DataAccessFormService extends AbstractDataAccessEntityFormService<DataAccessForm> {

  private FileStoreService fileStoreService;

  private DataAccessFormRepository dataAccessFormRepository;

  @Inject
  public DataAccessFormService(
    FileStoreService fileStoreService,
    DataAccessFormRepository dataAccessFormRepository) {
    this.fileStoreService = fileStoreService;
    this.dataAccessFormRepository = dataAccessFormRepository;
  }

  @Override
  String getDataAccessEntityFormResourceLocation() {
    return "classpath:config/data-access-form/";
  }

  @Override
  public DataAccessForm createOrUpdate(DataAccessForm dataAccessForm) {
    validateForm(dataAccessForm);
    dataAccessForm.getPdfTemplates().forEach((k, v) -> {
      if (v.isJustUploaded()) {
        fileStoreService.save(v.getId());
        v.setJustUploaded(false);
      }
    });
    dataAccessForm.setRevision(0);
    dataAccessForm.setLastUpdateDate(DateTime.now());
    return dataAccessFormRepository.save(dataAccessForm);
  }

  @Override
  public DataAccessForm findDraft() {
    DataAccessForm form = dataAccessFormRepository.findOne(DataAccessForm.DEFAULT_ID);
    if (form == null) {
      createOrUpdate(createDefaultDataAccessForm());
      form = dataAccessFormRepository.findOne(DataAccessForm.DEFAULT_ID);
    }
    return form;
  }

  @Override
  DataAccessForm findLatest() {
    Optional<DataAccessForm> latest = findFirstSortByRevisionDesc();
    if (!latest.isPresent()) {
      publish();
      latest = findFirstSortByRevisionDesc();
    }
    return latest.get();
  }

  @Override
  DataAccessForm findByRevision(int revision) {
    return dataAccessFormRepository.findFirstByRevision(revision);
  }

  @Override
  public void publish() {
    DataAccessForm draft = findDraft();
    draft.setId(null);
    Optional<DataAccessForm> latest = findFirstSortByRevisionDesc();
    draft.setRevision(latest.isPresent() ? latest.get().getRevision() + 1 : 1);
    dataAccessFormRepository.save(draft);
  }

  //
  // Private methods
  //

  private Optional<DataAccessForm> findFirstSortByRevisionDesc() {
    return dataAccessFormRepository.findAll(new Sort(Sort.Direction.DESC, "revision")).stream()
      .filter(form -> form.getRevision() > 0)
      .findFirst();
  }

  private DataAccessForm createDefaultDataAccessForm() {
    DataAccessForm form = new DataAccessForm();
    form.setDefinition(getDefaultDataAccessFormResourceAsString("definition.json"));
    form.setSchema(getDefaultDataAccessFormResourceAsString("schema.json"));
    form.setTitleFieldPath("projectTitle");
    form.setSummaryFieldPath("summary");
    form.setEndDateFieldPath("endDate");
    form.setRevision(0);
    return form;
  }
}
