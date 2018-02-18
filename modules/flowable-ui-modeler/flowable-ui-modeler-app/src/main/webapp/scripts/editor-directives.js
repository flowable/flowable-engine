/* Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
angular.module('flowableModeler')
    .directive('loading', ['$translate', function ($translate) {
        return {
            restrict: 'E',
            template: '<div class=\'loading pull-right\' ng-show=\'status.loading\'><div class=\'l1\'></div><div class=\'l2\'></div><div class=\'l2\'></div></div>'
        };
    }]);

angular.module('flowableModeler')
    .directive('loadingLeftPull', ['$translate', function ($translate) {
        return {
            restrict: 'E',
            template: '<div class=\'loading pull-left\' ng-show=\'status.loading\'><div class=\'l1\'></div><div class=\'l2\'></div><div class=\'l2\'></div></div>'
        };
    }]);

/**
 * This is a template for the icon of a stencil item.
 */
angular.module('flowableModeler')
    .directive('stencilItemIcon', [function () {
        return {
            scope: {
                item: '=stencilItem'
            },
            restrict: 'E',
            template: '<img class="stencil-item-list-icon" ng-if=\"item.customIconId != null && item.customIconId != undefined\" ng-src=\"' + getImageUrl(item.customIconId) + '\" width=\"16px\" height=\"16px\"/>' +
            '<img class="stencil-item-list-icon" ng-if=\"(item.customIconId == null || item.customIconId == undefined) && item.icon != null && item.icon != undefined\" ng-src=\"editor-app/stencilsets/bpmn2.0/icons/{{item.icon}}\" width=\"16px\" height=\"16px\"/>'
        };
    }]);

// Workaround for https://github.com/twbs/bootstrap/issues/8379 :
// prototype.js interferes with regular dropdown behavior
angular.module('flowableModeler')
    .directive('activitiFixDropdownBug', function () {
        return {
            restrict: 'AEC',
            link: function (scope, element, attrs) {
                if (!element.hasClass('btn-group')) {
                    // Fix applied to button, use parent instead
                    element = element.parent();
                }
                element.on('hidden.bs.dropdown	', function () {
                    element.show(); // evil prototype.js has added display:none to it ...
                })
            }
        };
    });


//form builder element renderer
angular.module('flowableModeler').directive('formBuilderElement', ['$rootScope', '$timeout', '$modal', '$http', '$templateCache', '$translate', 'RecursionHelper', 'FormBuilderService',
    function ($rootScope, $timeout, $modal, $http, $templateCache, $translate, RecursionHelper, FormBuilderService) {
    return {
        restrict: 'AE',
        templateUrl: 'views/templates/form-builder-element-template.html',
        transclude: false,
        scope: {
            formElement: '=formElement',
            editState: '=editState',
            formMode: '=formMode',
            drop: "&",
            moved: "&"
        },
        compile: function(element) {
            return RecursionHelper.compile(element, this.link);
        },
        link: function ($scope, $element, attributes) {

            $scope.formTabs = [
                {
                    "id": "general",
                    "name": $translate.instant('FORM-BUILDER.TABS.GENERAL')
                },
                {
                    "id": "options",
                    "name": $translate.instant('FORM-BUILDER.TABS.OPTIONS'),
                    "show": ['dropdown', 'radio-buttons']
                },
                {
                    "id": "upload",
                    "name": $translate.instant('FORM-BUILDER.TABS.UPLOAD-OPTIONS'),
                    "show": ['upload']
                },
                {
                    "id": "advanced",
                    "name": $translate.instant('FORM-BUILDER.TABS.ADVANCED-OPTIONS'),
                    "show": ['text', 'password', 'multi-line-text', 'integer', 'decimal','hyperlink']
                }
            ];

            $scope.activeTab = $scope.formTabs[0];

            $scope.tabClicked = function (tab) {
                $scope.activeTab = tab;
            };

            var templateUrl = 'views/popover/formfield-edit-popover.html';


            $scope.removeFormElement = function (formElement) {
                if ($rootScope.formItems.indexOf(formElement) >= 0) {
                    $rootScope.formItems.splice($rootScope.formItems.indexOf(formElement), 1);
                }
            };

            $scope.pristine = true;
            $scope.newOption = {
                name: ''
            };

            $scope.insertFormField = {
                position: 0
            };

            $scope.openFieldPopover = function () {

                // Storing original values. In case the changes would trigger a layout change
                var originalFormElementType = $scope.formElement.type;
                var originalDisplayFieldType = undefined;
                if (originalFormElementType === 'readonly') {
                    if ($scope.formElement.params
                        && $scope.formElement.params.field
                        && $scope.formElement.params.field.type) {
                        originalDisplayFieldType = $scope.formElement.params.field.type;
                    }
                }

                // Create popover
                $scope.fieldEditPopup = _internalCreateModal({
                    template: 'views/popover/formfield-edit-popover.html?version=' + Date.now(),
                    scope: $scope,
                    backdrop: 'static',
                    keyboard: false
                }, $modal, $scope);

                // Check for layout changes
                var deregisterHideListener = $scope.$on('modal.hide', function() {
                    if ($scope.formElement.type === 'readonly') {

                        if ($scope.formElement.params && $scope.formElement.params.field && $scope.formElement.params.field.type
                            && $scope.formElement.params.field.type !== originalFormElementType) {

                            $scope.$emit('readonly-field-referenced-field-changed', {
                                formElement: $scope.formElement,
                                originalDisplayFieldType: originalDisplayFieldType
                            });

                        }
                    }

                    deregisterHideListener();
                });

            };

            $scope.formElementNameChanged = function (field) {
                if (!field.overrideId) {
                    var fieldId;
                    if (field.name && field.name.length > 0) {
                        fieldId = field.name.toLowerCase();
                        fieldId = fieldId.replace(new RegExp(' ', 'g'), '');
                        fieldId = fieldId.replace(/[&\/\\#,+~%.'":*?!<>{}()$@;]/g, '');
                    } else {
                        var index = 1;
                        if (field.layout) {
                            index = 1 + (2 * field.layout.row) + field.layout.column;
                        }
                        fieldId = 'field' + index;
                    }
                    field.id = fieldId;
                }
            };

            $scope.confirmNewOption = function ($event) {
                if ($scope.newOption.name) {
                    var options = $scope.formElement.options;
                    options.push($scope.newOption);

                    $scope.newOption = {name: ''};

                    // if first additional option; first option is defaulted
                    if (options.length == 2) {
                        $scope.formElement.value = $scope.formElement.options[0].name;
                    }

                    if ($event) {
                        // Focus the input field again, to make adding more options possible immediatly
                        $($event.target).focus();
                    }
                }
            };

            $scope.optionKeyDown = function ($event) {
                if ($event.keyCode == 13) {
                    $scope.confirmNewOption($event);
                }
            };

            $scope.removeOption = function (index) {
                $scope.formElement.options.splice(index, 1);


                // if only 1 option left; reset default
                if ($scope.formElement.options == 1) {
                    $scope.formElement.value = '';
                } else {

                    // if removed element is the default option; first option is defaulted
                    var isPresent = false;
                    for (var i = 0; i < $scope.formElement.options.length; i++) {
                        if ($scope.formElement.options[i].name == $scope.formElement.value) {
                            isPresent = true;
                        }
                    }
                    if (!isPresent) {
                        $scope.formElement.value = $scope.formElement.options[0].name;
                    }
                }
            };

            $scope.doneEditing = function () {

                if ($scope.fieldEditPopup) {
                    $scope.fieldEditPopup.$scope.$hide();
                }
            };

            // Readonly field
            $scope.$watch('formElement.params.field', function (newValue, oldValue) {
                if (!$scope.pristine || (oldValue !== undefined && oldValue.id != newValue.id)) {
                    if (newValue && newValue.name) {
                        // Update the element's name
                        $scope.formElement.name = newValue.name;
                    }
                } else {
                    $scope.pristine = false;
                }

            });
        }
    };
}]);


angular.module('flowableModeler').directive('storeCursorPosition', ['$rootScope', '$timeout', '$popover', '$http', '$templateCache', function ($rootScope, $timeout, $popover, $http, $templateCache) {
    return {
        restrict: 'A',
        scope: {
            storeCursorPosition: '=storeCursorPosition'
        },
        link: function ($scope, $element, attributes) {
            $element.on('click change keypress', function () {
                if ($scope.storeCursorPosition !== undefined) {
                    $scope.storeCursorPosition = $element[0].selectionStart;
                }
            });
        }
    };
}]);

angular.module('flowableModeler').
    directive('editorInputCheck', function () {

        return {
            require: 'ngModel',
            link: function (scope, element, attrs, modelCtrl) {

                modelCtrl.$parsers.push(function (inputValue) {

                    var transformedInput = inputValue.replace(/[&\/\\#,+~%.'":*?<>{}()$@;]/g, '');

                    if (transformedInput != inputValue) {
                        modelCtrl.$setViewValue(transformedInput);
                        modelCtrl.$render();
                    }

                    return transformedInput;
                });
            }
        };
    });

angular.module('flowableModeler').
    directive('hotAutoDestroy',["hotRegisterer",function(hotRegisterer) {
    return {
        restrict: 'A',
        link: function (scope, element, attr){
            element.on("$destroy", function() {
                try{
                    var hotInstance = hotRegisterer.getInstance(attr.hotId);
                    hotInstance.destroy();
                }
                catch(er){
                    console.log(er);
                }
            });
        }
    };
}]);
