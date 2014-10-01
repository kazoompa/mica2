package org.obiba.mica.web.model;

import java.util.List;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;

import org.obiba.mica.core.domain.LocalizedString;
import org.obiba.mica.dataset.domain.Dataset;
import org.obiba.mica.dataset.domain.DatasetVariable;
import org.obiba.mica.dataset.domain.HarmonizationDataset;
import org.obiba.mica.dataset.domain.StudyDataset;
import org.obiba.mica.file.TempFile;
import org.obiba.mica.micaConfig.MicaConfig;
import org.obiba.mica.network.domain.Network;
import org.obiba.mica.study.domain.Study;
import org.obiba.mica.study.domain.StudyState;
import org.obiba.opal.core.domain.taxonomy.Taxonomy;
import org.springframework.stereotype.Component;

import static org.obiba.mica.web.model.Mica.MicaConfigDto;
import static org.obiba.mica.web.model.Mica.MicaConfigDtoOrBuilder;
import static org.obiba.mica.web.model.Mica.NetworkDto;
import static org.obiba.mica.web.model.Mica.NetworkDtoOrBuilder;
import static org.obiba.mica.web.model.Mica.StudyDto;
import static org.obiba.mica.web.model.Mica.StudyDtoOrBuilder;
import static org.obiba.mica.web.model.Mica.StudySummaryDto;

@Component
@SuppressWarnings("OverlyCoupledClass")
public class Dtos {

  @Inject
  private StudyDtos studyDtos;

  @Inject
  private MicaConfigDtos micaConfigDtos;

  @Inject
  private NetworkDtos networkDtos;

  @Inject
  private StudySummaryDtos studySummaryDtos;

  @Inject
  private TempFileDtos tempFileDtos;

  @Inject
  private DatasetDtos datasetDtos;

  @Inject
  private LocalizedStringDtos localizedStringDtos;

  @NotNull
  public StudyDto asDto(@NotNull Study study) {
    return studyDtos.asDto(study);
  }

  @NotNull
  public StudySummaryDto.Builder asSummaryDtoBuilder(@NotNull Study study) {
    return studySummaryDtos.asDtoBuilder(study);
  }

  @NotNull
  public StudySummaryDto asSummaryDto(@NotNull Study study) {
    return studySummaryDtos.asDto(study);
  }

  @NotNull
  public StudySummaryDto asDto(@NotNull StudyState studyState) {
    return studySummaryDtos.asDto(studyState);
  }

  @NotNull
  public Study fromDto(@NotNull StudyDtoOrBuilder dto) {
    return studyDtos.fromDto(dto);
  }

  @NotNull
  public NetworkDto.Builder asDtoBuilder(@NotNull Network network) {
    return networkDtos.asDtoBuilder(network);
  }

  @NotNull
  public NetworkDto asDto(@NotNull Network network) {
    return networkDtos.asDto(network);
  }

  @NotNull
  public Network fromDto(@NotNull NetworkDtoOrBuilder dto) {
    return networkDtos.fromDto(dto);
  }

  @NotNull
  public MicaConfigDto asDto(@NotNull MicaConfig micaConfig) {
    return micaConfigDtos.asDto(micaConfig);
  }

  @NotNull
  public MicaConfig fromDto(@NotNull MicaConfigDtoOrBuilder dto) {
    return micaConfigDtos.fromDto(dto);
  }

  @NotNull
  public Mica.TempFileDto asDto(@NotNull TempFile tempFile) {
    return tempFileDtos.asDto(tempFile);
  }

  @NotNull
  public Mica.DatasetDto asDto(@NotNull Dataset dataset) {
    if (dataset instanceof StudyDataset) {
      return datasetDtos.asDto((StudyDataset)dataset);
    } else {
      return datasetDtos.asDto((HarmonizationDataset) dataset);
    }
  }

  @NotNull
  public Mica.DatasetDto.Builder asDtoBuilder(@NotNull Dataset dataset) {
    if (dataset instanceof StudyDataset) {
      return datasetDtos.asDtoBuilder((StudyDataset)dataset);
    } else {
      return datasetDtos.asDtoBuilder((HarmonizationDataset) dataset);
    }
  }

  @NotNull
  public Dataset fromDto(Mica.DatasetDto dto) {
    return datasetDtos.fromDto(dto);
  }

  @NotNull
  public Mica.DatasetVariableDto asDto(@NotNull DatasetVariable variable) {
    return datasetDtos.asDto(variable);
  }

  @NotNull
  public Mica.DatasetVariableDto asDto(@NotNull DatasetVariable variable, List<Taxonomy> taxonomies) {
    return datasetDtos.asDto(variable, taxonomies);
  }

  @NotNull
  public Mica.DatasetVariableSummaryDto asSummaryDto(@NotNull DatasetVariable variable) {
    return datasetDtos.asSummaryDto(variable);
  }

  @NotNull
  public Mica.DatasetVariableResolverDto.Builder asDto(@NotNull DatasetVariable.IdResolver variableResolver) {
    return datasetDtos.asDto(variableResolver);
  }

  @NotNull
  public Iterable<Mica.LocalizedStringDto> asDto(LocalizedString string) {
    return localizedStringDtos.asDto(string);
  }

}
