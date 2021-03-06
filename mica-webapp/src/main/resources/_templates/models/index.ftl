<!-- Home page macros -->

<#macro homeModel>

  <#if config.repositoryEnabled>
    <div id="repository-metrics" class="row">

      <#if config.networkEnabled && !config.singleNetworkEnabled>
        <div class="col-lg-3 col-sm-6">
          <!-- small box -->
          <div class="small-box bg-info">
            <div class="inner">
              <h3 id="network-hits">-</h3>
              <p><@message "networks"/></p>
            </div>
            <div class="icon">
              <i class="${networkIcon}"></i>
            </div>
            <a href="${networksLink}" class="small-box-footer"><@message "more-info"/> <i class="fas fa-arrow-circle-right"></i></a>
          </div>
        </div>
        <!-- ./col -->
      </#if>

      <#if !config.singleStudyEnabled>
        <div class="col-lg-3 col-sm-6">
          <!-- small box -->
          <div class="small-box bg-success">
            <div class="inner">
              <h3 id="study-hits">-</h3>
              <p><@message "studies"/></p>
            </div>
            <div class="icon">
              <i class="${studyIcon}"></i>
            </div>
            <a href="${studiesLink}" class="small-box-footer"><@message "more-info"/> <i class="fas fa-arrow-circle-right"></i></a>
          </div>
        </div>
        <!-- ./col -->
      </#if>

      <#if config.studyDatasetEnabled || config.harmonizationDatasetEnabled>
        <div class="col-lg-3 col-sm-6">
          <!-- small box -->
          <div class="small-box bg-warning">
            <div class="inner">
              <h3 id="dataset-hits">-</h3>
              <p><@message "datasets"/></p>
            </div>
            <div class="icon">
              <i class="${datasetIcon}"></i>
            </div>
            <a href="${datasetsLink}" class="small-box-footer"><@message "more-info"/> <i class="fas fa-arrow-circle-right"></i></a>
          </div>
        </div>
        <!-- ./col -->
        <div class="col-lg-3 col-sm-6">
          <!-- small box -->
          <div class="small-box bg-danger">
            <div class="inner">
              <h3 id="variable-hits">-</h3>
              <p><@message "variables"/></p>
            </div>
            <div class="icon">
              <i class="${variableIcon}"></i>
            </div>
            <a href="${contextPath}/search#lists?type=variables" class="small-box-footer"><@message "more-info"/> <i class="fas fa-arrow-circle-right"></i></a>
          </div>
        </div>
        <!-- ./col -->
      </#if>
    </div>

    <#if !config.openAccess && !user??>
      <div id="sign-in-repository-callout" class="callout callout-info">
        <div class="row">
          <div class="col-sm-10">
            <p class="text-justify">
              <@message "sign-in-repository"/>
            </p>
          </div><!-- /.col -->
          <div class="col-sm-2">
            <div class="text-right">
              <button type="button"  onclick="location.href='${contextPath}/signin';" class="btn btn-primary btn-lg">
                <i class="fas fa-sign-in-alt"></i> <@message "sign-in"/>
              </button>
            </div>
          </div><!-- /.col -->
        </div><!-- /.row -->
      </div>
    </#if>

    <div id="search-portal-callout" class="callout callout-info">
      <div class="row">
        <div class="col-sm-10">
          <p class="text-justify">
            <@message "search-portal-callout"/>
          </p>
        </div><!-- /.col -->
        <div class="col-sm-2">
          <div class="text-right">
            <button type="button"  onclick="location.href='<#if !config.openAccess && !user??>${contextPath}/signin?redirect=${contextPath}/search${defaultSearchState?url('UTF-8')}<#else>${contextPath}/search${defaultSearchState}</#if>';" class="btn btn-success btn-lg">
              <i class="fas fa-search"></i> <@message "search"/>
            </button>
          </div>
        </div><!-- /.col -->
      </div><!-- /.row -->
    </div>
  </#if>

  <#if config.dataAccessEnabled>
    <div id="data-access-process-portal-callout" class="callout callout-info">
      <div class="row">
        <div class="col-sm-8">
          <p class="text-justify">
            <@message "data-access-process-portal-callout"/>
          </p>
        </div><!-- /.col -->
        <div class="col-sm-4">
          <div class="text-right">
            <button type="button"  onclick="location.href='${contextPath}/data-access-process';" class="btn btn-info btn-lg">
              <i class="fas fa-info-circle"></i> <@message "data-access-process"/>
            </button>
          </div>
        </div><!-- /.col -->
      </div><!-- /.row -->
    </div>
  </#if>
</#macro>
