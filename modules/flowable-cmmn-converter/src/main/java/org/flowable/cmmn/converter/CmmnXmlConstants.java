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
package org.flowable.cmmn.converter;

/**
 * @author Joram Barrez
 */
public interface CmmnXmlConstants {

    String CMMN_NAMESPACE = "http://www.omg.org/spec/CMMN/20151109/MODEL";
    String FLOWABLE_EXTENSIONS_NAMESPACE = "http://flowable.org/cmmn";
    String FLOWABLE_EXTENSIONS_PREFIX = "flowable";
    String XSI_NAMESPACE = "http://www.w3.org/2001/XMLSchema-instance";
    String XSI_PREFIX = "xsi";
    String CMMNDI_NAMESPACE = "http://www.omg.org/spec/CMMN/20151109/CMMNDI";
    String CMMNDI_PREFIX = "cmmndi";
    String OMGDC_NAMESPACE = "http://www.omg.org/spec/CMMN/20151109/DC";
    String OMGDC_PREFIX = "dc";
    String OMGDI_NAMESPACE = "http://www.omg.org/spec/CMMN/20151109/DI";
    String OMGDI_PREFIX = "di";
    String CASE_NAMESPACE = "http://www.flowable.org/test";

    String ATTRIBUTE_TARGET_NAMESPACE = "targetNamespace";
    String ATTRIBUTE_EXPRESSION_LANGUAGE = "expressionLanguage";
    String ATTRIBUTE_EXPORTER = "exporter";
    String ATTRIBUTE_EXPORTER_VERSION = "exporterVersion";
    String ATTRIBUTE_AUTHOR = "author";
    String ATTRIBUTE_CREATION_DATE = "creationDate";

    String ELEMENT_DEFINITIONS = "definitions";
    String ELEMENT_DOCUMENTATION = "documentation";
    String ELEMENT_CASE = "case";
    String ELEMENT_PLAN_MODEL = "casePlanModel";
    String ELEMENT_STAGE = "stage";
    String ELEMENT_PLAN_FRAGMENT = "planFragment";
    String ELEMENT_MILESTONE = "milestone";
    String ELEMENT_TASK = "task";
    String ELEMENT_HUMAN_TASK = "humanTask";
    String ELEMENT_CASE_TASK = "caseTask";
    String ELEMENT_PROCESS_TASK = "processTask";
    String ELEMENT_DECISION_TASK = "decisionTask";
    String ELEMENT_TIMER_EVENT_LISTENER = "timerEventListener";
    String ELEMENT_USER_EVENT_LISTENER = "userEventListener";
    String ELEMENT_GENERIC_EVENT_LISTENER = "eventListener";
    String ELEMENT_PLAN_ITEM = "planItem";
    String ELEMENT_ITEM_CONTROL = "itemControl";
    String ELEMENT_DEFAULT_CONTROL = "defaultControl";

    String ELEMENT_TASK_LISTENER = "taskListener";
    String ELEMENT_PLAN_ITEM_LIFECYCLE_LISTENER = "planItemLifecycleListener";
    String ELEMENT_CASE_LIFECYCLE_LISTENER = "caseLifecycleListener";
    String ATTRIBUTE_LISTENER_CLASS = "class";
    String ATTRIBUTE_LISTENER_EXPRESSION = "expression";
    String ATTRIBUTE_LISTENER_DELEGATEEXPRESSION = "delegateExpression";
    String ATTRIBUTE_LISTENER_EVENT = "event";
    String ATTRIBUTE_LISTENER_SOURCE_STATE = "sourceState";
    String ATTRIBUTE_LISTENER_TARGET_STATE = "targetState";
    String ATTRIBUTE_LISTENER_ON_TRANSACTION = "onTransaction";

    String ELEMENT_SENTRY = "sentry";
    String ELEMENT_PLAN_ITEM_ON_PART = "planItemOnPart";
    String ELEMENT_STANDARD_EVENT = "standardEvent";
    String ELEMENT_ENTRY_CRITERION = "entryCriterion";
    String ELEMENT_EXIT_CRITERION = "exitCriterion";
    String ELEMENT_IF_PART = "ifPart";
    String ELEMENT_EXTENSION_ELEMENTS = "extensionElements";
    String ELEMENT_HTTP_RESPONSE_HANDLER = "httpResponseHandler";
    String ELEMENT_HTTP_REQUEST_HANDLER = "httpRequestHandler";
    String ATTRIBUTE_HTTP_PARALLEL_IN_SAME_TRANSACTION = "parallelInSameTransaction";

    String ELEMENT_TEXT_ANNOTATION = "textAnnotation";
    String ELEMENT_ASSOCIATION = "association";

    String ATTRIBUTE_TRIGGER_MODE = "triggerMode";
    String ATTRIBUTE_EXIT_EVENT_TYPE = "exitEventType";
    String ATTRIBUTE_EXIT_TYPE = "exitType";

    String ELEMENT_REQUIRED_RULE = "requiredRule";
    String ELEMENT_MANUAL_ACTIVATION_RULE = "manualActivationRule";
    String ELEMENT_REPETITION_RULE = "repetitionRule";
    String ELEMENT_COMPLETION_NEUTRAL_RULE = "completionNeutralRule";
    String ELEMENT_PARENT_COMPLETION_RULE = "parentCompletionRule";
    String ELEMENT_REACTIVATION_RULE = "reactivationRule";
    String ELEMENT_DEFAULT_REACTIVATION_RULE = "defaultReactivationRule";

    String ELEMENT_PROCESS = "process";
    String ELEMENT_DECISION = "decision";
    String ATTRIBUTE_IMPLEMENTATION_TYPE = "implementationType";
    String ATTRIBUTE_EXTERNAL_REF = "externalRef";

    String ATTRIBUTE_ID = "id";
    String ATTRIBUTE_NAME = "name";
    String ATTRIBUTE_INITIATOR_VARIABLE_NAME = "initiatorVariableName";
    String ATTRIBUTE_CASE_CANDIDATE_USERS = "candidateStarterUsers";
    String ATTRIBUTE_CASE_CANDIDATE_GROUPS = "candidateStarterGroups";
    String ELEMENT_TEXT = "text";
    String ATTRIBUTE_TEXT_FORMAT = "textFormat";
    String ATTRIBUTE_DEFINITION_REF = "definitionRef";
    String ATTRIBUTE_SOURCE_REF = "sourceRef";
    String ATTRIBUTE_TARGET_REF = "targetRef";
    String ATTRIBUTE_SENTRY_REF = "sentryRef";
    String ATTRIBUTE_IS_BLOCKING = "isBlocking";
    String ATTRIBUTE_IS_BLOCKING_EXPRESSION = "isBlockingExpression";
    String ATTRIBUTE_IS_ASYNCHRONOUS = "async";
    String ATTRIBUTE_IS_EXCLUSIVE = "exclusive";
    String ATTRIBUTE_STORE_RESULT_AS_TRANSIENT = "storeResultVariableAsTransient";

    String ATTRIBUTE_EXTERNAL_WORKER_TOPIC = "topic";

    String ATTRIBUTE_IS_AUTO_COMPLETE = "autoComplete";
    String ATTRIBUTE_AUTO_COMPLETE_CONDITION = "autoCompleteCondition";

    String ATTRIBUTE_DISPLAY_ORDER = "displayOrder";
    String ATTRIBUTE_INCLUDE_IN_STAGE_OVERVIEW = "includeInStageOverview";

    String ATTRIBUTE_MILESTONE_VARIABLE = "milestoneVariable";

    String ATTRIBUTE_CASE_REF = "caseRef";
    String ATTRIBUTE_PROCESS_REF = "processRef";
    String ATTRIBUTE_DECISION_REF = "decisionRef";
    String ATTRIBUTE_SAME_DEPLOYMENT = "sameDeployment";
    String ATTRIBUTE_FALLBACK_TO_DEFAULT_TENANT = "fallbackToDefaultTenant";
    String ATTRIBUTE_BUSINESS_KEY = "businessKey";
    String ATTRIBUTE_INHERIT_BUSINESS_KEY = "inheritBusinessKey";
    String ATTRIBUTE_ID_VARIABLE_NAME = "idVariableName";

    String ELEMENT_CASE_REF_EXPRESSION = "caseRefExpression";
    String ELEMENT_PROCESS_REF_EXPRESSION = "processRefExpression";
    String ELEMENT_DECISION_REF_EXPRESSION = "decisionRefExpression";
    String ELEMENT_CONDITION = "condition";

    String ELEMENT_PARAMETER_MAPPING = "parameterMapping";

    String ELEMENT_CHILD_TASK_IN_PARAMETERS = "in";
    String ELEMENT_CHILD_TASK_OUT_PARAMETERS = "out";
    String ELEMENT_PROCESS_TASK_IN_PARAMETERS = ELEMENT_CHILD_TASK_IN_PARAMETERS;
    String ELEMENT_PROCESS_TASK_OUT_PARAMETERS = ELEMENT_CHILD_TASK_OUT_PARAMETERS;

    String ATTRIBUTE_IOPARAMETER_SOURCE = "source";
    String ATTRIBUTE_IOPARAMETER_SOURCE_EXPRESSION = "sourceExpression";
    String ATTRIBUTE_IOPARAMETER_TARGET = "target";
    String ATTRIBUTE_IOPARAMETER_TARGET_EXPRESSION = "targetExpression";

    String ELEMENT_TIMER_EXPRESSION = "timerExpression";
    String ELEMENT_PLAN_ITEM_START_TRIGGER = "planItemStartTrigger";
    String ATTRIBUTE_PLAN_ITEM_START_TRIGGER_SRC_REF = "sourceRef";

    String ATTRIBUTE_AUTHORIZED_ROLE_REFS = "authorizedRoleRefs";

    String ATTRIBUTE_TYPE = "type";

    String ATTRIBUTE_ACTIVATE_CONDITION = "activateCondition";
    String ATTRIBUTE_IGNORE_CONDITION = "ignoreCondition";
    String ATTRIBUTE_DEFAULT_CONDITION = "defaultCondition";

    String ATTRIBUTE_CLASS = "class";
    String ATTRIBUTE_EXPRESSION = "expression";
    String ATTRIBUTE_DELEGATE_EXPRESSION = "delegateExpression";

    String ATTRIBUTE_RESULT_VARIABLE_NAME = "resultVariableName";

    String ATTRIBUTE_SCRIPT_FORMAT = "scriptFormat";
    
    String ATTRIBUTE_LABEL = "label";
    String ATTRIBUTE_ICON = "icon";

    String ELEMENT_FIELD = "field";
    String ATTRIBUTE_FIELD_STRING = "stringValue";
    String ATTRIBUTE_FIELD_EXPRESSION = "expression";
    String ELEMENT_FIELD_STRING = "string";
    String ELEMENT_FIELD_EXPRESSION = "expression";

    String ATTRIBUTE_ASSIGNEE = "assignee";
    String ATTRIBUTE_OWNER = "owner";
    String ATTRIBUTE_CANDIDATE_USERS = "candidateUsers";
    String ATTRIBUTE_CANDIDATE_GROUPS = "candidateGroups";
    String ATTRIBUTE_PRIORITY = "priority";
    String ATTRIBUTE_FORM_KEY = "formKey";
    String ATTRIBUTE_FORM_FIELD_VALIDATION = "formFieldValidation";
    String ATTRIBUTE_DUE_DATE = "dueDate";
    String ATTRIBUTE_CATEGORY = "category";
    String ATTRIBUTE_TASK_ID_VARIABLE_NAME = "taskIdVariableName";

    String ATTRIBUTE_REPETITION_COUNTER_VARIABLE_NAME = "counterVariable";
    String ATTRIBUTE_REPETITION_MAX_INSTANCE_COUNT_NAME = "maxInstanceCount";
    String ATTRIBUTE_REPETITION_COLLECTION_VARIABLE_NAME = "collectionVariable";
    String ATTRIBUTE_REPETITION_ELEMENT_VARIABLE_NAME = "elementVariable";
    String ATTRIBUTE_REPETITION_ELEMENT_INDEX_VARIABLE_NAME = "elementIndexVariable";

    String ATTRIBUTE_TASK_SCRIPT_AUTO_STORE_VARIABLE = "autoStoreVariables";

    String ATTRIBUTE_EVENT_LISTENER_TYPE = "eventType"; // Note that this is the same as ELEMENT_EVENT_TYPE. We can't change this (backwards compatibility)
    String ATTRIBUTE_EVENT_LISTENER_AVAILABLE_CONDITION = "availableCondition";
    String ATTRIBUTE_EVENT_LISTENER_SIGNAL_REF = "signalRef";
    String ATTRIBUTE_EVENT_LISTENER_VARIABLE_NAME = "variableName";
    String ATTRIBUTE_EVENT_LISTENER_VARIABLE_CHANGE_TYPE = "variableChangeType";

    String ATTRIBUTE_USER_EVENT_LISTENER_REACTIVATE = "reactivateEventListener";

    String ELEMENT_EVENT_TYPE = "eventType";
    String ELEMENT_EVENT_CORRELATION_PARAMETER = "eventCorrelationParameter";
    String ELEMENT_EVENT_IN_PARAMETER = "eventInParameter";
    String ELEMENT_EVENT_OUT_PARAMETER = "eventOutParameter";
    String START_EVENT_CORRELATION_CONFIGURATION = "startEventCorrelationConfiguration";
    String START_EVENT_CORRELATION_STORE_AS_UNIQUE_REFERENCE_ID = "storeAsUniqueReferenceId";

    String ELEMENT_VARIABLE_AGGREGATION = "variableAggregation";
    String ATTRIBUTE_VARIABLE_AGGREGATION_VARIABLE = "variable";
    String ATTRIBUTE_VARIABLE_AGGREGATION_STORE_AS_TRANSIENT_VARIABLE = "storeAsTransientVariable";
    String ATTRIBUTE_VARIABLE_AGGREGATION_CREATE_OVERVIEW = "createOverviewVariable";

    String ELEMENT_DI_CMMN = "CMMNDI";
    String ELEMENT_DI_DIAGRAM = "CMMNDiagram";
    String ELEMENT_DI_SHAPE = "CMMNShape";
    String ELEMENT_DI_EDGE = "CMMNEdge";
    String ELEMENT_DI_LABEL = "CMMNLabel";
    String ELEMENT_DI_BOUNDS = "Bounds";
    String ELEMENT_DI_WAYPOINT = "waypoint";
    String ELEMENT_DI_EXTENSION = "extension";
    String ELEMENT_DI_DOCKER = "docker";
    String ATTRIBUTE_DI_CMMN_ELEMENT_REF = "cmmnElementRef";
    String ATTRIBUTE_DI_TARGET_CMMN_ELEMENT_REF = "targetCMMNElementRef";
    String ATTRIBUTE_DI_WIDTH = "width";
    String ATTRIBUTE_DI_HEIGHT = "height";
    String ATTRIBUTE_DI_X = "x";
    String ATTRIBUTE_DI_Y = "y";

}
