<!-- ChartJS -->
<script src="${adminLTEPath}/plugins/chart.js/Chart.min.js"></script>
<script src="${assetsPath}/js/mica-charts.js"></script>
<!-- Select2 -->
<script src="${adminLTEPath}/plugins/select2/js/select2.js"></script>
<script src="${adminLTEPath}/plugins/select2/js/i18n/${.lang}.js"></script>

<!-- Files -->
<script src="${assetsPath}/libs/node_modules/vue/dist/vue.js"></script>
<script src="${assetsPath}/js/mica-files.js"></script>

<!-- Repository -->
<script src="${assetsPath}/js/mica-repo.js"></script>

<!-- ChartJS -->
<script src="${adminLTEPath}/plugins/chart.js/Chart.min.js"></script>
<script src="${assetsPath}/libs/node_modules/chartjs-chart-geo/build/Chart.Geo.min.js"></script>

<!-- Mica Search and dependencies -->
<script src="${assetsPath}/libs/node_modules/rql/dist/rql.js"></script>
<script src="${assetsPath}/libs/node_modules/vue-mica-search/dist/VueMicaSearch.umd.js"></script>

<script>
  const Mica = {
    options: {},
    tr: {
      "studies": "<@message "studies"/>",
      "geographical-distribution-chart-title": "<@message "geographical-distribution-chart-title"/>",
      "geographical-distribution-chart-text": "<@message "geographical-distribution-chart-text"/>",
      "study-design-chart-title": "<@message "study-design-chart-title"/>",
      "study-design-chart-text": "<@message "study-design-chart-text"/>",
      "number-participants-chart-title": "<@message "number-participants-chart-title"/>",
      "number-participants-chart-text": "<@message "number-participants-chart-text"/>",
      "bio-samples-chart-title": "<@message "bio-samples-chart-title"/>",
      "bio-samples-chart-text": "<@message "bio-samples-chart-text"/>",
      "study-start-year-chart-title": "<@message "study-start-year-chart-title"/>",
      "study-start-year-chart-text": "<@message "study-start-year-chart-text"/>",
    }
  };

  Mica.charts = {
    backgroundColor: '${barChartBackgroundColor}',
    borderColor: '${barChartBorderColor}',
    backgroundColors: ['${colors?join("', '")}'],
    chartIds: ['${searchCharts?join("', '")}']
  };

  <#if studyAcronyms??>
    <#list studyAcronyms as key, value>
    Mica.options['${key}'] = '${localize(value)}';
    </#list>
  </#if>

  fetch('${contextPath}/assets/topojson/${mapName}.json').then(r => r.json())
    .then(data => Mica.map = {
      name: '${mapName}',
      topo: data
    });

  // an EventBus is a Vue app without element
  // its data are callback functions, registered by event name
  const EventBus = new Vue({
    data: {
      callbacks: {}
    },
    methods: {
      register: function (eventName, callback) {
        if (!this.callbacks[eventName]) {
          this.callbacks[eventName] = [];
          this.$on(eventName, function (payload) {
            for (let callback of this.callbacks[eventName]) {
              callback(payload);
            }
          });
        }
        this.callbacks[eventName].push(callback);
        //console.dir(this.callbacks)
      },
      unregister: function (eventName) {
        this.callbacks[eventName] = undefined;
      }
    }
  });

  // global translate filter for use in imported components
  Vue.filter("translate", (key) => {
    let value = Mica.tr[key];
    return typeof value === "string" ? value : key;
  });

  class ChartTableTermSorters {
    initialize(taxonomy) {
      this.taxonomy = taxonomy;
    }

    __findVocabulary(target) {
      return this.taxonomy.vocabularies.filter(vocabulary => vocabulary.name === target).pop();
    }

    sort(vocabulary, rows) {
      if (['methods-design', 'populations-dataCollectionEvents-bioSamples'].includes(vocabulary) && (rows || []).length > 0) {
        const found = this.__findVocabulary(vocabulary);
        if (found) {
          console.debug('FOUND', vocabulary)
          const terms = found.terms.map(term => term.name);
          rows.sort((a, b) => {
            return terms.indexOf(a.key) - terms.indexOf(b.key);
          })
        }
      }

      return rows;
    }
  }

  const chartTableTermSorters = new ChartTableTermSorters();

  function genericParseForChart(chartData, vocabulary) {
    let labels = [];
    let data = [];

    if (vocabulary) {
      chartTableTermSorters.sort(vocabulary, chartData);
    }

    chartData.forEach(term => {
      labels.push(term.title);
      data.push(term.count);
    });

    return [labels, { data: data }];
  }

  function genericParseForTable(vocabulary, chartData, forSubAggData) {
    return chartTableTermSorters.sort(vocabulary, chartData).map(term => {
      let row = {
        vocabulary: vocabulary.replace(/model-/, ""),
        key: term.key,
        title: term.title,
        count: term.count
      };

      if (forSubAggData) {
        const subAgg = term.aggs.filter((agg) => agg.aggregation === forSubAggData.agg)[0];
        row.subAgg = (subAgg[forSubAggData.dataKey] || {data: {}}).data[forSubAggData.data] || 0;
      }

      return row;
    });
  }

  const chartOptions = {
    'geographical-distribution-chart': {

      id: 'geographical-distribution-chart',
      title: Mica.tr['geographical-distribution-chart-title'],
      text: Mica.tr['geographical-distribution-chart-text'],
      type: 'choropleth',
      borderColor: Mica.charts.borderColor,
      agg: 'populations-model-selectionCriteria-countriesIso',
      vocabulary: 'populations-selectionCriteria-countriesIso',
      dataKey: 'obiba.mica.TermsAggregationResultDto.terms',
      withSort: true,
      parseForChart: function(chartData) {
        let labels = [];
        let data = [];

        let states;
        let featureFinder = function(key) {
          return states.filter(state => state.id === key).pop();
        };
        if (['world'].includes(Mica.map.name)) {
          states = ChartGeo.topojson.feature(Mica.map.topo, Mica.map.topo.objects.countries1).features;
        } else {
          states = ChartGeo.topojson.feature(Mica.map.topo, Mica.map.topo.objects.collection).features;
        }
        chartData.filter(term => term.count>0).forEach(term => {
          labels.push(term.title);
          data.push({
            feature: featureFinder(term.key),
            value: term.count
          });
        });

        return [labels, {
          outline: states,
          data: data
        }];
      },
      parseForTable: genericParseForTable,
      options: {
        showOutline: true,
        showGraticule: false,
        legend: {
          display: false
        },
        scale: {
          projection: 'mercator'//'equalEarth'//'naturalEarth1'
        },
        geo: {
          colorScale: {
            display: true,
          },
        }
      }
    },
    'study-design-chart': {
      id: 'study-design-chart',
      title: Mica.tr['study-design-chart-title'],
      text: Mica.tr['study-design-chart-text'],
      type: 'horizontalBar',
      backgroundColor: Mica.charts.backgroundColor,
      borderColor: Mica.charts.borderColor,
      parseForChart: genericParseForChart,
      parseForTable: genericParseForTable,
      agg: 'model-methods-design',
      vocabulary: 'methods-design',
      dataKey: 'obiba.mica.TermsAggregationResultDto.terms',
    },
    'number-participants-chart': {
      id: 'number-participants-chart',
      title: Mica.tr['number-participants-chart-title'],
      text: Mica.tr['number-participants-chart-text'],
      type: 'doughnut',
      backgroundColor: Mica.charts.backgroundColors,
      borderColor: Mica.charts.borderColor,
      parseForChart: genericParseForChart,
      parseForTable: genericParseForTable,
      agg: 'model-numberOfParticipants-participant-number-range',
      vocabulary: 'numberOfParticipants-participant-range',
      dataKey: 'obiba.mica.RangeAggregationResultDto.ranges',
      legend: {
        display: true,
        position: 'right',
        align: 'start',
      }
    },
    'bio-samples-chart': {
      id: 'bio-samples-chart',
      title: Mica.tr['bio-samples-chart-title'],
      text: Mica.tr['bio-samples-chart-text'],
      type: 'horizontalBar',
      backgroundColor: Mica.charts.backgroundColor,
      borderColor: Mica.charts.borderColor,
      parseForChart: genericParseForChart,
      parseForTable: genericParseForTable,
      agg: 'populations-dataCollectionEvents-model-bioSamples',
      vocabulary: 'populations-dataCollectionEvents-bioSamples',
      dataKey: 'obiba.mica.TermsAggregationResultDto.terms',
    },
    'study-start-year-chart': {
      id: 'study-start-year-chart',
      title: Mica.tr['study-start-year-chart-title'],
      text: Mica.tr['study-start-year-chart-text'],
      type: 'horizontalBar',
      backgroundColor: Mica.charts.backgroundColor,
      borderColor: Mica.charts.borderColor,
      parseForChart: genericParseForChart,
      parseForTable: genericParseForTable,
      agg: 'model-startYear-range',
      vocabulary: 'start-range',
      dataKey: 'obiba.mica.RangeAggregationResultDto.ranges',
    }
  };

  EventBus.register('query-type-updates-selection', (payload) => {
    let query = new RQL.Query('and');
    let networkQuery = new RQL.Query('network', [new RQL.Query('in', ['Mica_network.id', '${network.id}'])]);
    let studyQuery = new RQL.Query('study');

    query.push(networkQuery);

    (payload.updates || []).forEach(info => {
      studyQuery.push(info.query);
    });

    query.push(studyQuery);

    let params = ['query=' + RQL.Query.serializeArgs(query, ','), 'type=' + payload.type];
    window.location.href = '${contextPath}/search#lists?' + params.join('&');
  });

  /**
   * Registering plugins defined in VueMicaSearch
   */
  Vue.use(VueMicaSearch, {
    mixin: {
      methods: {
        getEventBus: () => EventBus,
        getMicaConfig: () => {},
        getLocale: () => '${.lang}',
        getDisplayOptions: () => {},
        normalizePath: (path) => {
          return contextPath + path;
        },
        localize: (entries) => StringLocalizer.localize(entries),
        registerDataTable: (tableId, options) => {
          const mergedOptions = Object.assign(options, DataTableDefaults);
          mergedOptions.language = {
            url: contextPath + '/assets/i18n/datatables.' + Mica.locale + '.json'
          };
          const dTable = $('#' + tableId).DataTable(mergedOptions);
          dTable.on('draw', function() {
            // bs tooltip
            $('[data-toggle="tooltip"]').tooltip();
          });

          return dTable;
        }
      }
    }
  });

  function callForSummaryStatistics() {
    const buckets = [
      'Mica_study.methods-design',
      'Mica_study.start-range',
      'Mica_study.populations-dataCollectionEvents-bioSamples',
      'Mica_study.populations-selectionCriteria-countriesIso',
      'Mica_study.populations-selectionCriteria-canadianProvinces',
    ];

    const aggregations = [
      'Mica_study.populations-selectionCriteria-countriesIso',
      'Mica_study.populations-dataCollectionEvents-bioSamples',
      'Mica_study.numberOfParticipants-participant-number'
    ];

    const tree = new RQL.QueryTree();

    let networkQuery = new RQL.Query('network', [new RQL.Query('in', ['Mica_network.id', '${network.id}'])]);
    tree.addQuery(null, networkQuery);

    let studyQuery = new RQL.Query('study', [new RQL.Query('in', ['Mica_study.className', 'Study'])]);

    tree.addQuery(null, studyQuery);
    tree.addQuery(studyQuery, new RQL.Query('aggregate', [...aggregations, ...[new RQL.Query('bucket', buckets)]]));

    tree.addQuery(null, new RQL.Query('facet'));

    let url = '/ws/studies/_rql';
    url = url + '?query=' + tree.serialize();

    const requests = [MicaService.normalizeUrl('/ws/taxonomies/_filter?target=study'), MicaService.normalizeUrl(url)];
    axios.all(requests.map(request => axios.get(request)))
      .then(axios.spread((...responses) => {
        chartTableTermSorters.initialize((responses[0].data||[]).pop());
        EventBus.$emit('query-type-graphics-results', {response: responses[1].data})
      }));
  }

  new Vue({
    el: '#summary-statistics',
    data() {
      return {
        hasGraphicsResult: false,
        chartOptions: Mica.charts.chartIds.map(id => chartOptions[id])
      };
    },
    methods: {
      onGraphicsResult(payload) {
        this.hasGraphicsResult = payload.response.studyResultDto.totalHits > 0;
        if (!this.hasGraphicsResult) {
          $('#summary-statistics-container').addClass('d-none');
        }
      },
      onTabClick(index) {
        $('#summary-statistics .card').each((i, card) => {
          if (i === index) {
            $(card).removeClass('d-none');
          } else {
            $(card).addClass('d-none');
          }
        });

        $('#summary-statistics .nav-link').each((i, link) => {
          if (i === index) {
            $(link).addClass('active');
          } else {
            $(link).removeClass('active');
          }
        });
      }
    },
    updated() {
      $('#summary-statistics .card').removeClass('card-outline').removeClass('card-primary').addClass('card-none');
      $('#summary-statistics .card-tools').addClass('d-none');
      this.onTabClick(0);
    },
    beforeMount() {
      EventBus.register('query-type-graphics-results', this.onGraphicsResult.bind(this));
    },
    beforeDestory() {
      EventBus.unregister('query-type-graphics-results', this.onGraphicsResult);
    }
  });

  const renderVariablesClassifications = function(key) {
    $('#loadingClassifications').hide();
    const chartsElem = $('#chartsContainer');
    chartsElem.children().remove();
    if (Mica.variablesCoverage) {
      Mica.variablesCoverage.forEach(chartData => {
        chartsElem.append('<h5>' + chartData.title + '</h5>');
        chartsElem.append('<p>' + chartData.subtitle + '</p>');
        chartsElem.append('<canvas class="mb-4"></canvas>');
        const chartCanvas = $('#chartsContainer canvas:last-child').get(0).getContext('2d');
        new Chart(chartCanvas, makeVariablesClassificationsChartSettings(chartData, {
          key: key,
          label: "<@message "variables"/>",
          borderColor: '${barChartBorderColor}',
          backgroundColor: '${barChartBackgroundColor}'
        }));
      });
      $('#classificationsContainer').show();
    } else {
      $('#noVariablesClassifications').show();
    }
  };

  const initSelectBucket = function() {
    // scan for bucket ids
    if (Mica.variablesCoverage) {
      const buckets = [];
      Mica.variablesCoverage.forEach(chartData => {
        Object.keys(chartData.itemCounts).forEach(k => {
          if (k !== '_all' && !buckets.includes(k)) {
            buckets.push(k);
          }
        });
      });
      const selectBucketElem = $('#select-bucket');
      selectBucketElem.select2({
        theme: 'bootstrap4'
      }).on('select2:select', function (e) {
        let data = e.params.data;
        //console.log(data);
        $('#classificationsContainer').hide();
        renderVariablesClassifications(data.id);
      });
      buckets.forEach(k => {
        let newOption = new Option(Mica.options[k], k, false, false);
        selectBucketElem.append(newOption);
      });
    }
  };

  const toMembershipsAnchors = function(urlPrefix, membershipsArray) {
    const roleTr = {'contact': "<@message "contact.label.contact"/>", 'investigator': "<@message "contact.label.investigator"/>"};

    const memberships = (membershipsArray || []).reduce((accumulator, membership) => {
      const item = accumulator[membership.parentId] || {};
      accumulator[membership.parentId] = item;

      if (!Array.isArray(item.roles)) {
        item.roles = [];
      }

      item.roles.push(roleTr[membership.role]);
      item.acronym = LocalizedValues.forLang(membership.parentAcronym, '${.lang}');

      return accumulator;
    }, {});

    return (Object.keys(memberships) || []).map(key => {
      let url = MicaService.normalizeUrl(urlPrefix + key);
      let acronym = memberships[key].acronym;
      let roles = '<span class="ml-1">(' + (memberships[key].roles || []).join(", ") + ')</span>';

      return '<a href="' + url + '">' + acronym + '</a>' + roles;
    });
  };

  $(function () {
    callForSummaryStatistics();

    $("#networks").DataTable(dataTablesDefaultOpts);
    $("#individual-studies").DataTable(dataTablesDefaultOpts);
    $("#harmonization-studies").DataTable(dataTablesDefaultOpts);
    QueryService.getCounts('networks', { query: "network(in(Mica_network.id,${network.id}))" }, function(stats) {
      $('#study-hits').text(numberFormatter.format(stats.studyResultDto.totalHits));
      $('#dataset-hits').text(numberFormatter.format(stats.datasetResultDto.totalHits));
      $('#variable-hits').text(numberFormatter.format(stats.variableResultDto.totalHits));
    });

    <!-- Affiliated Members -->
    <#if affiliatedMembersQuery??>
      NetworkService.getAffiliatedMembers('${affiliatedMembersQuery}', function(data) {
        if (data && data.total > 0) {
          const dataset = data.persons.map(person => {
            const item = [];
            item.push(person.firstName + ' ' + person.lastName);
            item.push(person.email || '');

            const studyMemberships = toMembershipsAnchors('/study/', person.studyMemberships);
            const networkMemberships = toMembershipsAnchors('/network', person.networkMemberships);

            item.push(studyMemberships.length > 0 ? studyMemberships.join(', ') : '');
            item.push(networkMemberships.length > 0 ? networkMemberships.join(', ') : '');

            return item;
          });

          $('#affiliatedMembersTable').dataTable({
            columns: [{ title: "<@message "full-name"/>" }, { title: "<@message "email"/>" }, { title: "<@message "studies"/>" }, { title: "<@message "networks"/>" }],
            data: dataset
          });

          $('#affiliatedMembersModal').modal('handleUpdate');
        }
      }, function(response) {

      });
    </#if>

    <!-- Files -->
    <#if showNetworkFiles>
      makeFilesVue('#files-app', {
        type: 'network',
        id: '${network.id}',
        basePath: '',
        path: '/',
        folder: {},
        tr: {
          "item": "<@message "item"/>",
          "items": "<@message "items"/>",
          "download": "<@message "download"/>"
        },
        locale: '${.lang}',
        contextPath: '${contextPath}'
      });
    </#if>

    <#if networkVariablesClassificationsTaxonomies?? && networkVariablesClassificationsTaxonomies?size gt 0 && studyAcronyms?? && studyAcronyms?size gt 0>
      const taxonomies = ['${networkVariablesClassificationsTaxonomies?join("', '")}'];
      $('#classificationsContainer').hide();
      NetworkService.getVariablesCoverage('${network.id}', taxonomies, '${.lang}', function(data) {
        if (data && data.charts) {
          Mica.variablesCoverage = data.charts.map(chart => prepareVariablesClassificationsData(chart));
        }
        initSelectBucket();
        renderVariablesClassifications('_all');
      }, function(response) {

      });
    </#if>
  });
</script>
