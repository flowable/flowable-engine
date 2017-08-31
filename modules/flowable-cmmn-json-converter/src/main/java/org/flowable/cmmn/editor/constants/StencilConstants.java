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
 */
public interface StencilConstants {

    // stencil items
    final String STENCIL_PLANMODEL = "PlanModel";
    final String STENCIL_STAGE = "Stage";
    final String STENCIL_TASK_HUMAN = "HumanTask";
    final String STENCIL_TASK_DECISION = "DecisionTask";

    final String STENCIL_ENTRY_CRITERION = "EntryCriterion";
    final String STENCIL_EXIT_CRITERION = "ExitCriterion";
    
    final String STENCIL_ASSOCIATION = "Association";
    
    final String PROPERTY_VALUE_YES = "Yes";
    final String PROPERTY_VALUE_NO = "No";
    
    // stencil properties
    final String PROPERTY_OVERRIDE_ID = "overrideid";
    final String PROPERTY_NAME = "name";
    final String PROPERTY_DOCUMENTATION = "documentation";

    final String PROPERTY_PROCESS_ID = "process_id";
    final String PROPERTY_PROCESS_VERSION = "process_version";
    final String PROPERTY_PROCESS_AUTHOR = "process_author";
    final String PROPERTY_PROCESS_NAMESPACE = "process_namespace";
    final String PROPERTY_PROCESS_EXECUTABLE = "process_executable";
    
    final String PROPERTY_TIMER_DURATON = "timerdurationdefinition";
    final String PROPERTY_TIMER_DATE = "timerdatedefinition";
    final String PROPERTY_TIMER_CYCLE = "timercycledefinition";
    final String PROPERTY_TIMER_CYCLE_END_DATE = "timerenddatedefinition";

    final String PROPERTY_FORMKEY = "formkeydefinition";
    final String PROPERTY_FORM_REFERENCE = "formreference";

    final String PROPERTY_USERTASK_ASSIGNMENT = "usertaskassignment";
    final String PROPERTY_USERTASK_PRIORITY = "prioritydefinition";
    final String PROPERTY_USERTASK_DUEDATE = "duedatedefinition";
    final String PROPERTY_USERTASK_ASSIGNEE = "assignee";
    final String PROPERTY_USERTASK_OWNER = "owner";
    final String PROPERTY_USERTASK_CANDIDATE_USERS = "candidateUsers";
    final String PROPERTY_USERTASK_CANDIDATE_GROUPS = "candidateGroups";
    final String PROPERTY_USERTASK_CATEGORY = "categorydefinition";

    final String PROPERTY_SERVICETASK_CLASS = "servicetaskclass";
    final String PROPERTY_SERVICETASK_EXPRESSION = "servicetaskexpression";
    final String PROPERTY_SERVICETASK_DELEGATE_EXPRESSION = "servicetaskdelegateexpression";
    final String PROPERTY_SERVICETASK_RESULT_VARIABLE = "servicetaskresultvariable";
    final String PROPERTY_SERVICETASK_FIELDS = "servicetaskfields";
    final String PROPERTY_SERVICETASK_FIELD_NAME = "name";
    final String PROPERTY_SERVICETASK_FIELD_STRING_VALUE = "stringValue";
    final String PROPERTY_SERVICETASK_FIELD_STRING = "string";
    final String PROPERTY_SERVICETASK_FIELD_EXPRESSION = "expression";

    final String PROPERTY_SCRIPT_FORMAT = "scriptformat";
    final String PROPERTY_SCRIPT_TEXT = "scripttext";

    final String PROPERTY_RULETASK_CLASS = "ruletask_class";
    final String PROPERTY_RULETASK_VARIABLES_INPUT = "ruletask_variables_input";
    final String PROPERTY_RULETASK_RESULT = "ruletask_result";
    final String PROPERTY_RULETASK_RULES = "ruletask_rules";
    final String PROPERTY_RULETASK_EXCLUDE = "ruletask_exclude";

    final String PROPERTY_DECISIONTABLE_REFERENCE = "decisiontaskdecisiontablereference";
    final String PROPERTY_DECISIONTABLE_REFERENCE_ID = "decisiontablereferenceid";
    final String PROPERTY_DECISIONTABLE_REFERENCE_NAME = "decisiontablereferencename";
    final String PROPERTY_DECISIONTABLE_REFERENCE_KEY = "decisionTableReferenceKey";
}
