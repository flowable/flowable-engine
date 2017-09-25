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
    String STENCIL_PLANMODEL = "PlanModel";
    String STENCIL_STAGE = "Stage";
    String STENCIL_TASK = "Task";
    String STENCIL_TASK_HUMAN = "HumanTask";
    String STENCIL_TASK_DECISION = "DecisionTask";
    String STENCIL_MILESTONE = "Milestone";

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
}
