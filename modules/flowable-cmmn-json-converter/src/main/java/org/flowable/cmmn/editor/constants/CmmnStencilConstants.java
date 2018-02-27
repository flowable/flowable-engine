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
package org.flowable.cmmn.editor.constants;

/**
 * @author Tijs Rademakers
 * @author Joram Barrez
 */
public interface CmmnStencilConstants {

    // stencil items

    String STENCIL_PLANMODEL = "CasePlanModel";
    String STENCIL_STAGE = "Stage";
    String STENCIL_TASK = "Task";
    String STENCIL_TASK_HUMAN = "HumanTask";
    String STENCIL_TASK_SERVICE = "ServiceTask";
    String STENCIL_TASK_DECISION = "DecisionTask";
    String STENCIL_TASK_HTTP = "HttpTask";
    String STENCIL_TASK_CASE = "CaseTask";
    String STENCIL_TASK_PROCESS = "ProcessTask";
    String STENCIL_MILESTONE = "Milestone";
    String STENCIL_TIMER_EVENT_LISTENER ="TimerEventListener";

    String STENCIL_ENTRY_CRITERION = "EntryCriterion";
    String STENCIL_EXIT_CRITERION = "ExitCriterion";

    String STENCIL_ASSOCIATION = "Association";

    String PROPERTY_VALUE_YES = "Yes";
    String PROPERTY_VALUE_NO = "No";

    // stencil properties

    String PROPERTY_OVERRIDE_ID = "overrideid";
    String PROPERTY_NAME = "name";
    String PROPERTY_DOCUMENTATION = "documentation";

    String PROPERTY_CASE_ID = "case_id";
    String PROPERTY_CASE_INITIATOR_VARIABLE_NAME = "case_initiatorvariablename";
    String PROPERTY_CASE_VERSION = "case_version";
    String PROPERTY_CASE_AUTHOR = "case_author";
    String PROPERTY_CASE_NAMESPACE = "case_namespace";

    String PROPERTY_TIMER_DURATON = "timerdurationdefinition";
    String PROPERTY_TIMER_DATE = "timerdatedefinition";
    String PROPERTY_TIMER_CYCLE = "timercycledefinition";
    String PROPERTY_TIMER_CYCLE_END_DATE = "timerenddatedefinition";

    String PROPERTY_FORMKEY = "formkeydefinition";
    String PROPERTY_FORM_REFERENCE = "formreference";

    String PROPERTY_IS_BLOCKING = "isblocking";
    String PROPERTY_IS_BLOCKING_EXPRESSION = "isblockingexpression";
    
    String PROPERTY_IS_AUTOCOMPLETE = "autocompleteenabled";
    String PROPERTY_AUTOCOMPLETE_CONDITION = "autocompletecondition";
    
    String PROPERTY_IS_ASYNC = "isasync";
    String PROPERTY_IS_EXCLUSIVE = "isexclusive";
    
    String PROPERTY_REQUIRED_ENABLED = "requiredenabled";
    String PROPERTY_REQUIRED_RULE_CONDITION = "requiredrulecondition";
    
    String PROPERTY_REPETITION_ENABLED = "repetitionenabled";
    String PROPERTY_REPETITION_RULE_CONDITION = "repetitionrulecondition";
    String PROPERTY_REPETITION_RULE_VARIABLE_NAME = "repetitioncountervariablename";
    
    String PROPERTY_MANUAL_ACTIVATION_ENABLED = "manualactivationenabled";
    String PROPERTY_MANUAL_ACTIVATION_RULE_CONDITION = "manualactivationrulecondition";

    String PROPERTY_USERTASK_ASSIGNMENT = "usertaskassignment";
    String PROPERTY_USERTASK_PRIORITY = "prioritydefinition";
    String PROPERTY_USERTASK_DUEDATE = "duedatedefinition";
    String PROPERTY_USERTASK_ASSIGNEE = "assignee";
    String PROPERTY_USERTASK_OWNER = "owner";
    String PROPERTY_USERTASK_CANDIDATE_USERS = "candidateUsers";
    String PROPERTY_USERTASK_CANDIDATE_GROUPS = "candidateGroups";
    String PROPERTY_USERTASK_CATEGORY = "categorydefinition";

    String PROPERTY_SERVICETASK_CLASS = "servicetaskclass";
    String PROPERTY_SERVICETASK_EXPRESSION = "servicetaskexpression";
    String PROPERTY_SERVICETASK_DELEGATE_EXPRESSION = "servicetaskdelegateexpression";
    String PROPERTY_SERVICETASK_RESULT_VARIABLE = "servicetaskresultvariable";
    String PROPERTY_SERVICETASK_FIELDS = "servicetaskfields";
    String PROPERTY_SERVICETASK_FIELD_NAME = "name";
    String PROPERTY_SERVICETASK_FIELD_STRING_VALUE = "stringValue";
    String PROPERTY_SERVICETASK_FIELD_STRING = "string";
    String PROPERTY_SERVICETASK_FIELD_EXPRESSION = "expression";

    String PROPERTY_SCRIPT_FORMAT = "scriptformat";
    String PROPERTY_SCRIPT_TEXT = "scripttext";

    String PROPERTY_DECISIONTABLE_REFERENCE = "decisiontaskdecisiontablereference";
    String PROPERTY_DECISIONTABLE_REFERENCE_ID = "decisiontablereferenceid";
    String PROPERTY_DECISIONTABLE_REFERENCE_NAME = "decisiontablereferencename";
    String PROPERTY_DECISIONTABLE_REFERENCE_KEY = "decisionTableReferenceKey";
    String PROPERTY_DECISIONTABLE_THROW_ERROR_NO_HITS = "decisiontaskthrowerroronnohits";
    String PROPERTY_DECISIONTABLE_THROW_ERROR_NO_HITS_KEY = "decisionTaskThrowErrorOnNoHits";

    String PROPERTY_CASE_REFERENCE = "casetaskcasereference";

    String PROPERTY_PROCESS_REFERENCE = "processtaskprocessreference";

    String PROPERTY_IF_PART_CONDITION = "ifpartcondition";

    String PROPERTY_TRANSITION_EVENT = "transitionevent";

    String PROPERTY_HTTPTASK_REQ_METHOD = "httptaskrequestmethod";
    String PROPERTY_HTTPTASK_REQ_URL = "httptaskrequesturl";
    String PROPERTY_HTTPTASK_REQ_HEADERS = "httptaskrequestheaders";
    String PROPERTY_HTTPTASK_REQ_BODY  = "httptaskrequestbody";
    String PROPERTY_HTTPTASK_REQ_TIMEOUT = "httptaskrequesttimeout";
    String PROPERTY_HTTPTASK_REQ_DISALLOW_REDIRECTS = "httptaskdisallowredirects";
    String PROPERTY_HTTPTASK_REQ_FAIL_STATUS_CODES = "httptaskfailstatuscodes";
    String PROPERTY_HTTPTASK_REQ_HANDLE_STATUS_CODES = "httptaskhandlestatuscodes";
    String PROPERTY_HTTPTASK_REQ_IGNORE_EXCEPTION = "httptaskignoreexception";
    String PROPERTY_HTTPTASK_RESPONSE_VARIABLE_NAME = "httptaskresponsevariablename";
    String PROPERTY_HTTPTASK_SAVE_REQUEST_VARIABLES = "httptasksaverequestvariables";
    String PROPERTY_HTTPTASK_SAVE_RESPONSE_PARAMETERS = "httptasksaveresponseparameters";
    String PROPERTY_HTTPTASK_RESULT_VARIABLE_PREFIX = "httptaskresultvariableprefix";
    String PROPERTY_HTTPTASK_SAVE_RESPONSE_TRANSIENT = "httptasksaveresponseparameterstransient";
    String PROPERTY_HTTPTASK_SAVE_RESPONSE_AS_JSON = "httptasksaveresponseasjson";

    String PROPERTY_TIMER_EXPRESSION = "timerexpression";
    String PROPERTY_TIMER_START_TRIGGER_SOURCE_REF = "timerstarttriggersourceref";
    String PROPERTY_TIMER_START_TRIGGER_STANDARD_EVENT = "transitionevent";

}
