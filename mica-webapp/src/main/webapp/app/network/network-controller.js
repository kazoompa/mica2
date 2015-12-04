/*
 * Copyright (c) 2014 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

'use strict';

mica.network

  .constant('NETWORK_EVENTS', {
    networkUpdated: 'event:network-updated'
  })

  .controller('NetworkListController', ['$rootScope',
    '$scope',
    '$filter',
    '$translate',
    'NetworksResource',
    'NetworkService',

    function ($rootScope,
              $scope,
              $filter,
              $translate,
              NetworksResource,
              NetworkService
    ) {
      var onSuccess = function(response) {
        $scope.networks = response;
        $scope.loading = false;
      };

      var onError = function() {
        $scope.loading = false;
      };

      $scope.loading = true;
      NetworksResource.query({}, onSuccess, onError);

      $scope.deleteNetwork = function(network) {
        NetworkService.deleteNetwork(network, function() {
          $scope.loading = true;
          NetworksResource.query({}, onSuccess, onError);
        });
      };
    }])

  .controller('NetworkEditController', [
    '$rootScope',
    '$scope',
    '$routeParams',
    '$log',
    '$locale',
    '$location',
    'DraftNetworkResource',
    'DraftNetworksResource',
    'DraftNetworkPublicationResource',
    'MicaConfigResource',
    'FormServerValidation',
    'ActiveTabService',
    function ($rootScope,
              $scope,
              $routeParams,
              $log,
              $locale,
              $location,
              DraftNetworkResource,
              DraftNetworksResource,
              DraftNetworkPublicationResource,
              MicaConfigResource,
              FormServerValidation,
              ActiveTabService) {

      $scope.getActiveTab = ActiveTabService.getActiveTab;
      $scope.files = [];
      $scope.newNetwork= !$routeParams.id;
      $scope.network = $routeParams.id ?
        DraftNetworkResource.get({id: $routeParams.id}, function(response) {
          $scope.files = response.logo ? [response.logo] : [];
          return response;
        }) : {published: false};

      MicaConfigResource.get(function (micaConfig) {
        $scope.tabs = [];
        micaConfig.languages.forEach(function (lang) {
          $scope.tabs.push({lang: lang});
        });
      });

      $scope.save = function () {
        $scope.network.logo = $scope.files.length > 0 ? $scope.files[0] : null;

        if(!$scope.network.logo) { //protobuf doesnt like null values
          delete $scope.network.logo;
        }

        if (!$scope.form.$valid) {
          $scope.form.saveAttempted = true;
          return;
        }
        if ($scope.network.id) {
          updateNetwork();
        } else {
          createNetwork();
        }
      };

      $scope.cancel = function () {
        $location.path('/network' + ($scope.network.id ? '/' + $scope.network.id : '')).replace();
      };

      var updateNetwork = function () {
        $scope.network.$save(
          function (network) {
            $location.path('/network/' + network.id).replace();
          },
          saveErrorHandler);
      };

      var createNetwork = function () {
        DraftNetworksResource.save($scope.network,
          function (resource, getResponseHeaders) {
            var parts = getResponseHeaders().location.split('/');
            $location.path('/network/' + parts[parts.length - 1]).replace();
          },
          saveErrorHandler);
      };

      var saveErrorHandler = function (response) {
        FormServerValidation.error(response, $scope.form, $scope.languages);
      };
    }])

  .controller('NetworkLinksModalController', ['$scope', '$modalInstance', 'entityStatesResource', 'currentLinks', 'type', 'lang',
    function ($scope, $modalInstance, entityStatesResource, currentLinks, type, lang) {
      $scope.type = type;
      $scope.lang = lang;
      $scope.entities = [];

      entityStatesResource.query().$promise.then(function(entities) {
        $scope.entities = entities.filter(function(s) {
          return currentLinks === undefined || currentLinks.indexOf(s.id) < 0;
        });
      });

      $scope.hasSelectedEntities = function() {
        for(var i = $scope.entities.length; i--; ){
          if($scope.entities[i].selected) {
            return true;
          }
        }

        return false;
      };

      $scope.removeSelectedEntity = function(entity) {
        entity.selected = false;
      };

      $scope.save = function () {
        var selectedIds = $scope.entities //
          .filter(function(s){ return s.selected; }) //
          .map(function(s) { return s.id; });

        $modalInstance.close(selectedIds);
      };

      $scope.cancel = function () {
        $modalInstance.dismiss('cancel');
      };
    }])

  .controller('NetworkContactsModalController', ['$scope', '$modalInstance', 'ContactsSearchResource', 'network', 'lang',
    function ($scope, $modalInstance, ContactsSearchResource, network, lang) {
      $scope.network = network;
      $scope.lang = lang;
      var studyIds = network.studyIds.join(' ');
      $scope.persons = [];

      if(network.studyIds.length > 0) {
        ContactsSearchResource.get({
          query: 'studyMemberships.parentId:(' + studyIds + ')',
          limit: 999
        }).$promise.then(function (result) {
            $scope.persons = result.persons.filter(function (p) {
              return p.studyMemberships && p.studyMemberships.length > 0;
            });
          });
      }

      $scope.getDownloadAllContactsParams = function () {
        return $.param({
          query: 'studyMemberships.parentId:(' + studyIds + ')',
          limit: 999
        });
      };

      $scope.cancel = function () {
        $modalInstance.dismiss('cancel');
      };
    }])

  .controller('NetworkViewController', ['$rootScope',
    '$scope',
    '$routeParams',
    '$log',
    '$locale',
    '$location',
    '$translate',
    '$timeout',
    'DraftNetworkResource',
    'DraftNetworksResource',
    'DraftNetworkPublicationResource',
    'DraftNetworkStatusResource',
    'DraftNetworkViewRevisionResource',
    'DraftNetworkRevisionsResource',
    'DraftNetworkRestoreRevisionResource',
    'MicaConfigResource',
    'CONTACT_EVENTS',
    'NETWORK_EVENTS',
    'NOTIFICATION_EVENTS',
    'DraftStudiesSummariesResource',
    'DraftFileSystemSearchResource',
    'StudyStatesResource',
    '$modal',
    'LocalizedValues',
    'ActiveTabService',
    '$filter',
    'NetworkService',

    function ($rootScope,
              $scope,
              $routeParams,
              $log,
              $locale,
              $location,
              $translate,
              $timeout,
              DraftNetworkResource,
              DraftNetworksResource,
              DraftNetworkPublicationResource,
              DraftNetworkStatusResource,
              DraftNetworkViewRevisionResource,
              DraftNetworkRevisionsResource,
              DraftNetworkRestoreRevisionResource,
              MicaConfigResource,
              CONTACT_EVENTS,
              NETWORK_EVENTS,
              NOTIFICATION_EVENTS,
              DraftStudiesSummariesResource,
              DraftFileSystemSearchResource,
              StudyStatesResource,
              $modal,
              LocalizedValues,
              ActiveTabService,
              $filter,
              NetworkService) {
      var initializeNetwork = function(network){
        if (network.logo) {
          $scope.logoUrl = 'ws/draft/network/'+network.id+'/file/'+network.logo.id+'/_download';
        }

        $scope.studySummaries = [];

        if (network.studyIds && network.studyIds.length > 0) {
          DraftStudiesSummariesResource.summaries({id: network.studyIds},function (summaries){
            $scope.studySummaries = summaries;
          });
        } else  {
          network.studyIds = [];
        }

        $scope.network.networkIds = $scope.network.networkIds || [];

        $scope.memberships = network.memberships.map(function (m) {
          if (!m.members) {
            m.members = [];
          }

          return m;
        }).reduce(function (res, m) {
          res[m.role] = m.members;
          return res;
        }, {});
      };

      $scope.memberships = {};

      $scope.isOrderingContacts = false; //prevent opening contact modal on reordering (firefox)

      $scope.sortableOptions = {
        start: function() {
          $scope.isOrderingContacts = true;
        },
        stop: function () {
          $scope.emitNetworkUpdated();
          $timeout(function () {
            $scope.isOrderingContacts = false;
          }, 300);
        }
      };

      $scope.Mode = {View: 0, Revision: 1, File: 2, Permission: 3};

      $scope.getActiveTab = ActiveTabService.getActiveTab;

      MicaConfigResource.get(function (micaConfig) {
        $scope.tabs = [];
        micaConfig.languages.forEach(function (lang) {
          $scope.tabs.push({lang: lang});
        });

        $scope.roles = micaConfig.roles;
        $scope.openAccess = micaConfig.openAccess;
      });

      $scope.networkId = $routeParams.id;
      $scope.studySummaries = [];
      $scope.network = DraftNetworkResource.get({id: $routeParams.id}, initializeNetwork);

      $scope.publish = function (publish) {
        if (publish) {
          DraftFileSystemSearchResource.searchUnderReview({path: '/network/' + $scope.network.id},
            function onSuccess(response) {
              DraftNetworkPublicationResource.publish(
                {id: $scope.network.id, cascading: response.length > 0 ? 'UNDER_REVIEW' : 'NONE'},
                function () {
                  $scope.network = DraftNetworkResource.get({id: $routeParams.id});
                });
            },
            function onError() {
              $log.error('Failed to search for Under Review files.');
            }
          );
        } else {
          DraftNetworkPublicationResource.unPublish({id: $scope.network.id}, function () {
            $scope.network = DraftNetworkResource.get({id: $routeParams.id});
          });
        }
      };

      $scope.toStatus = function (value) {
        DraftNetworkStatusResource.toStatus({id: $scope.network.id, value: value}, function () {
          $scope.network = DraftNetworkResource.get({id: $routeParams.id});
        });
      };

      $scope.delete = function () {
        NetworkService.deleteNetwork($scope.network, function() {
          $location.path('/network');
        });
      };

      var getViewMode = function() {
        var result = /\/(revision[s\/]*|files|permissions)/.exec($location.path());
        if (result && result.length > 1) {
          switch (result[1]) {
            case 'revision':
            case 'revisions':
              return $scope.Mode.Revision;
            case 'files':
              return $scope.Mode.File;
            case 'permissions':
              return $scope.Mode.Permission;
          }
        }

        return $scope.Mode.View;
      };

      $scope.viewMode = getViewMode();
      $scope.inViewMode = function () {
        return $scope.viewMode === $scope.Mode.View;
      };

      var viewRevision = function (networkId, commitInfo) {
        $scope.commitInfo = commitInfo;
        $scope.network = DraftNetworkViewRevisionResource.view({
          id: networkId,
          commitId: commitInfo.commitId
        }, initializeNetwork);
      };

      var fetchNetwork = function (networkId) {
        $scope.network = DraftNetworkResource.get({id: networkId}, initializeNetwork);
      };

      var fetchRevisions = function (networkId, onSuccess) {
        DraftNetworkRevisionsResource.query({id: networkId}, function (response) {
          if (onSuccess) {
            onSuccess(response);
          }
        });
      };

      var restoreRevision = function (networkId, commitInfo, onSuccess) {
        if (commitInfo && $scope.networkId === networkId) {
          var args = {commitId: commitInfo.commitId, restoreSuccessCallback: onSuccess};

          $rootScope.$broadcast(NOTIFICATION_EVENTS.showConfirmDialog,
            {
              titleKey: 'network.restore-dialog.title',
              messageKey: 'network.restore-dialog.message',
              messageArgs: [$filter('amDateFormat')(commitInfo.date, 'lll')]
            }, args
          );
        }
      };

      var onRestore = function (event, args) {
        if (args.commitId) {
          DraftNetworkRestoreRevisionResource.restore({id: $scope.networkId, commitId: args.commitId},
            function () {
              fetchNetwork($routeParams.id);
              $scope.networkId = $routeParams.id;
              if (args.restoreSuccessCallback) {
                args.restoreSuccessCallback();
              }
            });
        }
      };

      $scope.fetchNetwork = fetchNetwork;
      $scope.viewRevision = viewRevision;
      $scope.restoreRevision = restoreRevision;
      $scope.fetchRevisions = fetchRevisions;

      $scope.$on(NOTIFICATION_EVENTS.confirmDialogAccepted, onRestore);

      $scope.emitNetworkUpdated = function () {
        $scope.$emit(NETWORK_EVENTS.networkUpdated, $scope.network);
      };

      $scope.$on(NETWORK_EVENTS.networkUpdated, function (event, networkUpdated) {
        if (networkUpdated === $scope.network) {
          $log.debug('save network', networkUpdated);

          $scope.network.$save(function () {
              $scope.network = DraftNetworkResource.get({id: $routeParams.id}, initializeNetwork);
            },
            function (response) {
              $log.error('Error on network save:', response);
              $rootScope.$broadcast(NOTIFICATION_EVENTS.showNotificationDialog, {
                message: response.data ? response.data : angular.fromJson(response)
              });
            });
        }
      });

      function updateExistingContact(contact, contacts) {
        var existingContact = contacts.filter(function (c) {
          return c.id === contact.id && !angular.equals(c, contact);
        })[0];

        if (existingContact) {
          angular.copy(contact, existingContact);
        }
      }

      $scope.$on(CONTACT_EVENTS.addContact, function (event, network, contact, type) {
        if (network === $scope.network) {
          var roleMemberships = $scope.network.memberships.filter(function(m) {
            if (m.role === type) {
              return true;
            }

            return false;
          })[0];

          if (!roleMemberships) {
            roleMemberships = {role: type, members: []};
            $scope.network.memberships.push(roleMemberships);
          }

          updateExistingContact(contact, roleMemberships.members || []);
          roleMemberships.members.push(contact);

          $scope.emitNetworkUpdated();
        }
      });

      $scope.$on(CONTACT_EVENTS.contactUpdated, function (event, network, contact, type) {
        var roleMemberships = $scope.network.memberships.filter(function (m) {
          return m.role === type;
        })[0];

        updateExistingContact(contact, roleMemberships.members || []);

        if (network === $scope.network) {
          $scope.emitNetworkUpdated();
        }
      });

      $scope.$on(CONTACT_EVENTS.contactEditionCanceled, function (event, network) {
        if (network === $scope.network) {
          $scope.network = DraftNetworkResource.get({id: $scope.network.id}, initializeNetwork);
        }
      });

      $scope.$on(CONTACT_EVENTS.contactDeleted, function (event, network, contact, type) {
        if (network === $scope.network) {
          var roleMemberships = $scope.network.memberships.filter(function (m) {
              return m.role === type;
            })[0] || { members: [] };

          var idx = roleMemberships.members.indexOf(contact);

          if (idx !== -1) {
            roleMemberships.members.splice(idx, 1);
          }

          $scope.emitNetworkUpdated();
        }
      });

      $scope.addStudyEvent = function () {
        $modal.open({
          templateUrl: 'app/network/views/network-modal-add-links.html',
          controller: 'NetworkLinksModalController',
          resolve: {
            entityStatesResource: function() {
              return StudyStatesResource;
            },
            type: function() {
              return 'study';
            },
            currentLinks: function() {
              return $scope.network.studyIds;
            },
            lang: function() {
              return ActiveTabService.getActiveTab($scope.tabs).lang;
            }
          }
        }).result.then(function(selectedIds) {
            $scope.network.studyIds = ($scope.network.studyIds || []).concat(selectedIds);
            $scope.emitNetworkUpdated();
          });
      };

      $scope.deleteStudyEvent = function (network, summary) {
        $rootScope.$broadcast(NOTIFICATION_EVENTS.showConfirmDialog,
          {
            titleKey: 'network.study-delete-dialog.title',
            messageKey:'network.study-delete-dialog.message',
            messageArgs: [LocalizedValues.forLang(summary.name, $translate.use())]
          }, {type: 'study', summary: summary}
        );
      };

      $scope.addNetwork = function () {
        $modal.open({
          templateUrl: 'app/network/views/network-modal-add-links.html',
          controller: 'NetworkLinksModalController',
          resolve: {
            entityStatesResource: function() {
              return DraftNetworksResource;
            },
            type: function() {
              return 'network';
            },
            currentLinks: function() {
              return ($scope.network.networkIds || []).concat($scope.network.id);
            },
            lang: function() {
              return ActiveTabService.getActiveTab($scope.tabs).lang;
            }
          }
        }).result.then(function(selectedIds) {
            $scope.network.networkIds = ($scope.network.networkIds || []).concat(selectedIds);
            $scope.emitNetworkUpdated();
          });
      };

      $scope.deleteNetwork = function (network, summary) {
        $rootScope.$broadcast(NOTIFICATION_EVENTS.showConfirmDialog,
          {
            titleKey: 'network.network-delete-dialog.title',
            messageKey: 'network.network-delete-dialog.message',
            messageArgs: [LocalizedValues.forLang(summary.name, $translate.use())]
          }, {type: 'network', summary: summary}
        );
      };

      $scope.showAllContacts = function () {
        $modal.open({
          templateUrl: 'app/network/views/network-modal-contacts.html',
          controller: 'NetworkContactsModalController',
          resolve: {
            network: function() {
              return $scope.network;
            },
            lang: function() {
              return ActiveTabService.getActiveTab($scope.tabs).lang;
            }
          }
        });
      };

      $scope.$on(NOTIFICATION_EVENTS.confirmDialogAccepted, function (event, data) {
        var deleteIndex;

        if (!data.summary) { return; }

        if (data.type === 'study') {
          deleteIndex = $scope.network.studyIds.indexOf(data.summary.id);

          if (deleteIndex > -1) {
            $scope.network.studyIds.splice(deleteIndex, 1);
            $scope.emitNetworkUpdated();
          } else {
            $log.error('The id was not found: ', data.summary.id);
          }
        } else {
          deleteIndex = $scope.network.networkIds.indexOf(data.summary.id);

          if (deleteIndex > -1) {
            $scope.network.networkIds.splice(deleteIndex, 1);
            $scope.emitNetworkUpdated();
          } else {
            $log.error('The id was not found: ', data.summary.id);
          }
        }
      });
    }])

  .controller('NetworkPermissionsController', ['$scope','$routeParams', 'DraftNetworkPermissionsResource', 'DraftNetworkAccessesResource',
    function ($scope, $routeParams, DraftNetworkPermissionsResource, DraftNetworkAccessesResource) {
    $scope.permissions = [];
    $scope.accesses = [];

    $scope.loadPermissions = function () {
      $scope.permissions = DraftNetworkPermissionsResource.query({id: $routeParams.id});
      return $scope.permissions;
    };

    $scope.deletePermission = function (permission) {
      return DraftNetworkPermissionsResource.delete({id: $routeParams.id}, permission);
    };

    $scope.addPermission = function (permission) {
      return DraftNetworkPermissionsResource.save({id: $routeParams.id}, permission);
    };

    $scope.loadAccesses = function () {
      $scope.accesses = DraftNetworkAccessesResource.query({id: $routeParams.id});
      return $scope.accesses;
    };

    $scope.deleteAccess = function (access) {
      return DraftNetworkAccessesResource.delete({id: $routeParams.id}, access);
    };

    $scope.addAccess = function (access) {
      return DraftNetworkAccessesResource.save({id: $routeParams.id}, access);
    };
  }]);
