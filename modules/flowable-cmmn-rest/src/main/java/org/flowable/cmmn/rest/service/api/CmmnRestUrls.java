package org.flowable.cmmn.rest.service.api;

import java.text.MessageFormat;

import org.apache.commons.lang3.StringUtils;

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

/**
 * @author Frederik Heremans
 */
public final class CmmnRestUrls {

    /**
     * Base segment for all repository-related resources: <i>repository</i>
     */
    public static final String SEGMENT_REPOSITORY_RESOURCES = "cmmn-repository";
    public static final String SEGMENT_RUNTIME_RESOURCES = "cmmn-runtime";
    public static final String SEGMENT_MANAGEMENT_RESOURCES = "cmmn-management";
    public static final String SEGMENT_HISTORY_RESOURCES = "cmmn-history";
    public static final String SEGMENT_QUERY_RESOURCES = "cmmn-query";

    public static final String SEGMENT_DEPLOYMENT_RESOURCE = "deployments";
    public static final String SEGMENT_CASE_DEFINITION_RESOURCE = "case-definitions";
    public static final String SEGMENT_DEPLOYMENT_ARTIFACT_RESOURCE = "resources";
    public static final String SEGMENT_DEPLOYMENT_ARTIFACT_RESOURCE_CONTENT = "resourcedata";

    public static final String SEGMENT_TASK_RESOURCE = "tasks";
    public static final String SEGMENT_CASE_INSTANCE_RESOURCE = "case-instances";
    public static final String SEGMENT_PLAN_ITEM_INSTANCE_RESOURCE = "plan-item-instances";
    public static final String SEGMENT_VARIABLES = "variables";
    public static final String SEGMENT_SUBTASKS = "subtasks";
    public static final String SEGMENT_IDENTITYLINKS = "identitylinks";
    public static final String SEGMENT_EVENTS = "events";
    public static final String SEGMENT_IDENTITYLINKS_FAMILY_GROUPS = "groups";
    public static final String SEGMENT_IDENTITYLINKS_FAMILY_USERS = "users";
    public static final String SEGMENT_VARIABLE_DATA = "data";
    public static final String SEGMENT_HISTORIC_CASE_INSTANCE_RESOURCE = "historic-case-instances";
    public static final String SEGMENT_HISTORIC_TASK_INSTANCE_RESOURCE = "historic-task-instances";
    public static final String SEGMENT_HISTORIC_ACTIVITY_INSTANCE_RESOURCE = "historic-activity-instances";
    public static final String SEGMENT_HISTORIC_VARIABLE_INSTANCE_RESOURCE = "historic-variable-instances";
    public static final String SEGMENT_HISTORIC_MILESTONE_INSTANCE_RESOURCE = "historic-milestone-instances";
    public static final String SEGMENT_HISTORIC_PLANITEM_INSTANCE_RESOURCE = "historic-planitem-instances";
    public static final String SEGMENT_HISTORIC_DETAIL_RESOURCE = "historic-detail";
    public static final String SEGMENT_FORM_DATA = "form-data";
    public static final String SEGMENT_TABLES = "tables";
    public static final String SEGMENT_COLUMNS = "columns";
    public static final String SEGMENT_DATA = "data";
    public static final String SEGMENT_JOBS = "jobs";
    public static final String SEGMENT_TIMER_JOBS = "timer-jobs";
    public static final String SEGMENT_SUSPENDED_JOBS = "suspended-jobs";
    public static final String SEGMENT_DEADLETTER_JOBS = "deadletter-jobs";
    public static final String SEGMENT_JOB_EXCEPTION_STACKTRACE = "exception-stacktrace";
    public static final String SEGMENT_USERS = "users";
    public static final String SEGMENT_GROUPS = "groups";
    public static final String SEGMENT_PICTURE = "picture";
    public static final String SEGMENT_INFO = "info";
    public static final String SEGMENT_MEMBERS = "members";
    public static final String SEGMENT_MODEL = "model";
    public static final String SEGMENT_PROPERTIES = "properties";
    public static final String SEGMENT_ENGINE_INFO = "engine";
    public static final String SEGMENT_ACTIVITIES = "activities";
    public static final String SEGMENT_MODEL_RESOURCE = "models";
    public static final String SEGMENT_SOURCE = "source";
    public static final String SEGMENT_SOURCE_EXTRA = "source-extra";
    public static final String SEGMENT_DIAGRAM = "diagram";
    public static final String SEGMENT_SIGNALS = "signals";
    public static final String SEGMENT_IMAGE = "image";
    public static final String SEGMENT_DECISION_TABLES = "decision-tables";
    public static final String SEGMENT_FORM_DEFINITIONS = "form-definitions";

    /**
     * URL template for the deployment collection: <i>cmmn-repository/deployments</i>
     */
    public static final String[] URL_DEPLOYMENT_COLLECTION = { SEGMENT_REPOSITORY_RESOURCES, SEGMENT_DEPLOYMENT_RESOURCE };

    /**
     * URL template for a single deployment: <i>cmmn-repository/deployments/{0:deploymentId}</i>
     */
    public static final String[] URL_DEPLOYMENT = { SEGMENT_REPOSITORY_RESOURCES, SEGMENT_DEPLOYMENT_RESOURCE, "{0}" };

    /**
     * URL template listing deployment resources: <i>cmmn-repository/deployments/{0:deploymentId}/resources</i>
     */
    public static final String[] URL_DEPLOYMENT_RESOURCES = { SEGMENT_REPOSITORY_RESOURCES, SEGMENT_DEPLOYMENT_RESOURCE, "{0}", SEGMENT_DEPLOYMENT_ARTIFACT_RESOURCE };

    /**
     * URL template for a single deployment resource: <i>cmmn-repository/deployments/{0:deploymentId}/resources/{1}:resourceId</i>
     */
    public static final String[] URL_DEPLOYMENT_RESOURCE = { SEGMENT_REPOSITORY_RESOURCES, SEGMENT_DEPLOYMENT_RESOURCE, "{0}", SEGMENT_DEPLOYMENT_ARTIFACT_RESOURCE, "{1}" };

    /**
     * URL template for a single deployment resource content: <i>cmmn-repository/deployments /{0:deploymentId}/resourcedata/{1}:resourceId</i>
     */
    public static final String[] URL_DEPLOYMENT_RESOURCE_CONTENT = { SEGMENT_REPOSITORY_RESOURCES, SEGMENT_DEPLOYMENT_RESOURCE, "{0}", SEGMENT_DEPLOYMENT_ARTIFACT_RESOURCE_CONTENT, "{1}" };

    /**
     * URL template for the process definition collection: <i>cmmn-repository/process-definitions</i>
     */
    public static final String[] URL_CASE_DEFINITION_COLLECTION = { SEGMENT_REPOSITORY_RESOURCES, SEGMENT_CASE_DEFINITION_RESOURCE };

    /**
     * URL template for a single case definition: <i>cmmn-repository/case-definitions/{0:caseDefinitionId}</i>
     */
    public static final String[] URL_CASE_DEFINITION = { SEGMENT_REPOSITORY_RESOURCES, SEGMENT_CASE_DEFINITION_RESOURCE, "{0}" };

    /**
     * URL template for the resource of a single case definition: <i>cmmn-repository/case-definitions/{0:caseDefinitionId}/resourcedata</i>
     */
    public static final String[] URL_CASE_DEFINITION_RESOURCE_CONTENT = { SEGMENT_REPOSITORY_RESOURCES, SEGMENT_CASE_DEFINITION_RESOURCE, "{0}", SEGMENT_DEPLOYMENT_ARTIFACT_RESOURCE_CONTENT };
    
    /**
     * URL template for a case definition's identity links: <i>cmmn-repository/case-definitions/{0:caseDefinitionId}/identitylinks</i>
     */
    public static final String[] URL_CASE_DEFINITION_IDENTITYLINKS_COLLECTION = { SEGMENT_REPOSITORY_RESOURCES, SEGMENT_CASE_DEFINITION_RESOURCE, "{0}", SEGMENT_IDENTITYLINKS };

    /**
     * URL template for an identitylink on a case definition: <i>cmmn-repository/case-definitions/{0:caseDefinitionId}/identitylinks/{1 :family}/{2:identityId}</i>
     */
    public static final String[] URL_CASE_DEFINITION_IDENTITYLINK = { SEGMENT_REPOSITORY_RESOURCES, SEGMENT_CASE_DEFINITION_RESOURCE, "{0}", SEGMENT_IDENTITYLINKS, "{1}", "{2}" };

    /**
     * URL template for the image of a case definition: <i>cmmn-repository/case-definitions/{0:caseDefinitionId}/image</i>
     */
    public static final String[] URL_CASE_DEFINITION_IMAGE = { SEGMENT_REPOSITORY_RESOURCES, SEGMENT_CASE_DEFINITION_RESOURCE, "{0}", SEGMENT_IMAGE };

    /**
     * URL template for the image of a case definition: <i>cmmn-repository/case-definitions/{0:caseDefinitionId}/decision-tables</i>
     */
    public static final String[] URL_CASE_DEFINITION_DECISION_TABLES_COLLECTION = { SEGMENT_REPOSITORY_RESOURCES, SEGMENT_CASE_DEFINITION_RESOURCE, "{0}", SEGMENT_DECISION_TABLES };

    /**
     * URL template for the image of a case definition: <i>cmmn-repository/case-definitions/{0:caseDefinitionId}/form-definitions</i>
     */
    public static final String[] URL_CASE_DEFINITION_FORM_DEFINITIONS_COLLECTION = { SEGMENT_REPOSITORY_RESOURCES, SEGMENT_CASE_DEFINITION_RESOURCE, "{0}", SEGMENT_FORM_DEFINITIONS };

    /**
     * URL template for task collection: <i>runtime/tasks</i>
     */
    public static final String[] URL_TASK_COLLECTION = { SEGMENT_RUNTIME_RESOURCES, SEGMENT_TASK_RESOURCE };

    /**
     * URL template for task query: <i>query/tasks</i>
     */
    public static final String[] URL_TASK_QUERY = { SEGMENT_QUERY_RESOURCES, SEGMENT_TASK_RESOURCE };

    /**
     * URL template for a single task: <i>runtime/tasks/{0:taskId}</i>
     */
    public static final String[] URL_TASK = { SEGMENT_RUNTIME_RESOURCES, SEGMENT_TASK_RESOURCE, "{0}" };

    /**
     * URL template for a task's sub tasks: <i>runtime/tasks/{0:taskId}/subtasks</i>
     */
    public static final String[] URL_TASK_SUBTASKS_COLLECTION = { SEGMENT_RUNTIME_RESOURCES, SEGMENT_TASK_RESOURCE, "{0}", SEGMENT_SUBTASKS };

    /**
     * URL template for a task's variables: <i>runtime/tasks/{0:taskId}/variables</i>
     */
    public static final String[] URL_TASK_VARIABLES_COLLECTION = { SEGMENT_RUNTIME_RESOURCES, SEGMENT_TASK_RESOURCE, "{0}", SEGMENT_VARIABLES };

    /**
     * URL template for a single task variable: <i>runtime/tasks/{0:taskId}/variables/{1:variableName}</i>
     */
    public static final String[] URL_TASK_VARIABLE = { SEGMENT_RUNTIME_RESOURCES, SEGMENT_TASK_RESOURCE, "{0}", SEGMENT_VARIABLES, "{1}" };

    /**
     * URL template for a single task variable content: <i>runtime/tasks/{0:taskId}/variables/{1:variableName}/data</i>
     */
    public static final String[] URL_TASK_VARIABLE_DATA = { SEGMENT_RUNTIME_RESOURCES, SEGMENT_TASK_RESOURCE, "{0}", SEGMENT_VARIABLES, "{1}", SEGMENT_VARIABLE_DATA };

    /**
     * URL template for a task's identity links: <i>runtime/tasks/{0:taskId}/identitylinks</i>
     */
    public static final String[] URL_TASK_IDENTITYLINKS_COLLECTION = { SEGMENT_RUNTIME_RESOURCES, SEGMENT_TASK_RESOURCE, "{0}", SEGMENT_IDENTITYLINKS };

    /**
     * URL template for an identitylink on a task: <i>runtime/tasks/{0:taskId}/identitylinks /{1:family}/{2:identityId}/{3:type}</i>
     */
    public static final String[] URL_TASK_IDENTITYLINK = { SEGMENT_RUNTIME_RESOURCES, SEGMENT_TASK_RESOURCE, "{0}", SEGMENT_IDENTITYLINKS, "{1}", "{2}", "{3}" };
    
    /**
     * URL template for an task's events: <i>runtime/tasks/{0:taskId}/events</i>
     */
    public static final String[] URL_TASK_EVENT_COLLECTION = { SEGMENT_RUNTIME_RESOURCES, SEGMENT_TASK_RESOURCE, "{0}", SEGMENT_EVENTS };

    /**
     * URL template for an event on a task: <i>runtime/tasks/{0:taskId}/events/{1:eventId}</i>
     */
    public static final String[] URL_TASK_EVENT = { SEGMENT_RUNTIME_RESOURCES, SEGMENT_TASK_RESOURCE, "{0}", SEGMENT_EVENTS, "{1}" };

    /**
     * URL template for process instance collection: <i>runtime/case-instances</i>
     */
    public static final String[] URL_CASE_INSTANCE_COLLECTION = { SEGMENT_RUNTIME_RESOURCES, SEGMENT_CASE_INSTANCE_RESOURCE };

    /**
     * URL template for process instance query: <i>query/case-instances</i>
     */
    public static final String[] URL_CASE_INSTANCE_QUERY = { SEGMENT_QUERY_RESOURCES, SEGMENT_CASE_INSTANCE_RESOURCE };

    /**
     * URL template for a single case instance: <i>cmmn-runtime/case-instances/{0:caseInstanceId}</i>
     */
    public static final String[] URL_CASE_INSTANCE = { SEGMENT_RUNTIME_RESOURCES, SEGMENT_CASE_INSTANCE_RESOURCE, "{0}" };

    /**
     * URL template for the diagram for a single CASE instance: <i>cmmn-runtime/case-instances/{0:caseInstanceId}/diagram</i>
     */
    public static final String[] URL_CASE_INSTANCE_DIAGRAM = { SEGMENT_RUNTIME_RESOURCES, SEGMENT_CASE_INSTANCE_RESOURCE, "{0}", SEGMENT_DIAGRAM };

    /**
     * URL template for case instance variable collection: <i>cmmn-runtime/case-instances/{0:processInstanceId}/variables</i>
     */
    public static final String[] URL_CASE_INSTANCE_VARIABLE_COLLECTION = { SEGMENT_RUNTIME_RESOURCES, SEGMENT_CASE_INSTANCE_RESOURCE, "{0}", SEGMENT_VARIABLES };

    /**
     * URL template for a single case instance variable: <i>cmmn-runtime/case-instances /{0:caseInstanceId}/variables/{1:variableName}</i>
     */
    public static final String[] URL_CASE_INSTANCE_VARIABLE = { SEGMENT_RUNTIME_RESOURCES, SEGMENT_CASE_INSTANCE_RESOURCE, "{0}", SEGMENT_VARIABLES, "{1}" };

    /**
     * URL template for a single case instance variable data: <i>cmmn-runtime/case-instances/{0:processInstanceId}/variables/{1:variableName}/data</i>
     */
    public static final String[] URL_CASE_INSTANCE_VARIABLE_DATA = { SEGMENT_RUNTIME_RESOURCES, SEGMENT_CASE_INSTANCE_RESOURCE, "{0}", SEGMENT_VARIABLES, "{1}", SEGMENT_VARIABLE_DATA };

    /**
     * URL template for a case instance's identity links: <i>cmmn-runtime/case-instances/{0:caseInstanceId}/identitylinks</i>
     */
    public static final String[] URL_CASE_INSTANCE_IDENTITYLINKS_COLLECTION = { SEGMENT_RUNTIME_RESOURCES, SEGMENT_CASE_INSTANCE_RESOURCE, "{0}", SEGMENT_IDENTITYLINKS };

    /**
     * URL template for an identitylink on a case instance: <i>cmmn-runtime/case-instances/{0:caseInstanceId}/identitylinks/users/{1: identityId}/{2:type}</i>
     */
    public static final String[] URL_CASE_INSTANCE_IDENTITYLINK = { SEGMENT_RUNTIME_RESOURCES, SEGMENT_CASE_INSTANCE_RESOURCE, "{0}", SEGMENT_IDENTITYLINKS, 
                    SEGMENT_IDENTITYLINKS_FAMILY_USERS, "{1}", "{2}" };
    
    /**
     * URL template for plan item instance collection: <i>cmmn-runtime/plan-item-instances</i>
     */
    public static final String[] URL_PLAN_ITEM_INSTANCE_COLLECTION = { SEGMENT_RUNTIME_RESOURCES, SEGMENT_PLAN_ITEM_INSTANCE_RESOURCE };

    /**
     * URL template for process instance query: <i>cmmn-query/plan-item-instances</i>
     */
    public static final String[] URL_PLAN_ITEM_INSTANCE_QUERY = { SEGMENT_QUERY_RESOURCES, SEGMENT_PLAN_ITEM_INSTANCE_RESOURCE };

    /**
     * URL template for a single case instance: <i>cmmn-runtime/plan-item-instances/{0:planItemInstanceId}</i>
     */
    public static final String[] URL_PLAN_ITEM_INSTANCE = { SEGMENT_RUNTIME_RESOURCES, SEGMENT_PLAN_ITEM_INSTANCE_RESOURCE, "{0}" };
    
    /**
     * URL template for a single job: <i>cmmn-management/jobs/{0:jobId}</i>
     */
    public static final String[] URL_JOB = { SEGMENT_MANAGEMENT_RESOURCES, SEGMENT_JOBS, "{0}" };

    /**
     * URL template for a single job: <i>cmmn-management/timer-jobs/{0:jobId}</i>
     */
    public static final String[] URL_TIMER_JOB = { SEGMENT_MANAGEMENT_RESOURCES, SEGMENT_TIMER_JOBS, "{0}" };

    /**
     * URL template for a single job: <i>cmmn-management/suspended-jobs/{0:jobId}</i>
     */
    public static final String[] URL_SUSPENDED_JOB = { SEGMENT_MANAGEMENT_RESOURCES, SEGMENT_SUSPENDED_JOBS, "{0}" };

    /**
     * URL template for a single job: <i>cmmn-management/deadletter-jobs/{0:jobId}</i>
     */
    public static final String[] URL_DEADLETTER_JOB = { SEGMENT_MANAGEMENT_RESOURCES, SEGMENT_DEADLETTER_JOBS, "{0}" };

    /**
     * URL template for the stacktrace of a single job: <i>cmmn-management/jobs/{0:jobId}/exception-stacktrace</i>
     */
    public static final String[] URL_JOB_EXCEPTION_STRACKTRACE = { SEGMENT_MANAGEMENT_RESOURCES, SEGMENT_JOBS, "{0}", SEGMENT_JOB_EXCEPTION_STACKTRACE };

    /**
     * URL template for the stacktrace of a single job: <i>cmmn-management/timer-jobs/{jobId}/exception-stacktrace</i>
     */
    public static final String[] URL_TIMER_JOB_EXCEPTION_STRACKTRACE = { SEGMENT_MANAGEMENT_RESOURCES, SEGMENT_TIMER_JOBS, "{0}", SEGMENT_JOB_EXCEPTION_STACKTRACE };

    /**
     * URL template for the stacktrace of a single job: <i>cmmn-management/suspended-jobs/{0:jobId}/exception-stacktrace</i>
     */
    public static final String[] URL_SUSPENDED_JOB_EXCEPTION_STRACKTRACE = { SEGMENT_MANAGEMENT_RESOURCES, SEGMENT_SUSPENDED_JOBS, "{0}", SEGMENT_JOB_EXCEPTION_STACKTRACE };

    /**
     * URL template for the stacktrace of a single job: <i>cmmn-management/deadletter-jobs/{0:jobId}/exception-stacktrace</i>
     */
    public static final String[] URL_DEADLETTER_JOB_EXCEPTION_STRACKTRACE = { SEGMENT_MANAGEMENT_RESOURCES, SEGMENT_DEADLETTER_JOBS, "{0}", SEGMENT_JOB_EXCEPTION_STACKTRACE };

    /**
     * URL template for the collection of jobs: <i>management/jobs</i>
     */
    public static final String[] URL_JOB_COLLECTION = { SEGMENT_MANAGEMENT_RESOURCES, SEGMENT_JOBS };

    /**
     * URL template for the collection of timer jobs: <i>management/timer-jobs</i>
     */
    public static final String[] URL_TIMER_JOB_COLLECTION = { SEGMENT_MANAGEMENT_RESOURCES, SEGMENT_TIMER_JOBS };

    /**
     * URL template for the collection of timer jobs: <i>management/suspended-jobs</i>
     */
    public static final String[] URL_SUSPENDED_JOB_COLLECTION = { SEGMENT_MANAGEMENT_RESOURCES, SEGMENT_SUSPENDED_JOBS };

    /**
     * URL template for the collection of timer jobs: <i>management/deadletter-jobs</i>
     */
    public static final String[] URL_DEADLETTER_JOB_COLLECTION = { SEGMENT_MANAGEMENT_RESOURCES, SEGMENT_DEADLETTER_JOBS };

    /**
     * URL template for a single historic case instance: <i>cmmn-history/historic-case-instances/{0:caseInstanceId}</i>
     */
    public static final String[] URL_HISTORIC_CASE_INSTANCE = { SEGMENT_HISTORY_RESOURCES, SEGMENT_HISTORIC_CASE_INSTANCE_RESOURCE, "{0}" };

    /**
     * URL template for historic process instance query: <i>cmmn-history/historic-case-instances</i>
     */
    public static final String[] URL_HISTORIC_CASE_INSTANCES = { SEGMENT_HISTORY_RESOURCES, SEGMENT_HISTORIC_CASE_INSTANCE_RESOURCE };

    /**
     * URL template for historic case instance identity links: <i>cmmn-history/historic-case-instances/{0:caseInstanceId}/identitylinks</i>
     */
    public static final String[] URL_HISTORIC_CASE_INSTANCE_IDENTITY_LINKS = { SEGMENT_HISTORY_RESOURCES, SEGMENT_HISTORIC_CASE_INSTANCE_RESOURCE, "{0}", SEGMENT_IDENTITYLINKS };

    /**
     * URL template for historic case instance variables: <i>history/historic-case-instances/{0:caseInstanceId}/variables/{1:variableName}</i>
     */
    public static final String[] URL_HISTORIC_CASE_INSTANCE_VARIABLE_DATA = { SEGMENT_HISTORY_RESOURCES, SEGMENT_HISTORIC_CASE_INSTANCE_RESOURCE, "{0}", SEGMENT_VARIABLES, "{1}",
            SEGMENT_VARIABLE_DATA };

    /**
     * URL template for a single historic task instance: <i>history/historic-task-instances/{0:taskId}</i>
     */
    public static final String[] URL_HISTORIC_TASK_INSTANCE = { SEGMENT_HISTORY_RESOURCES, SEGMENT_HISTORIC_TASK_INSTANCE_RESOURCE, "{0}" };

    /**
     * URL template for historic task instance query: <i>history/historic-task-instances</i>
     */
    public static final String[] URL_HISTORIC_TASK_INSTANCES = { SEGMENT_HISTORY_RESOURCES, SEGMENT_HISTORIC_TASK_INSTANCE_RESOURCE };

    /**
     * URL template for historic task instance identity links: <i>history/historic-task-instances/{0:taskId}/identitylinks</i>
     */
    public static final String[] URL_HISTORIC_TASK_INSTANCE_IDENTITY_LINKS = { SEGMENT_HISTORY_RESOURCES, SEGMENT_HISTORIC_TASK_INSTANCE_RESOURCE, "{0}", SEGMENT_IDENTITYLINKS };

    /**
     * URL template for a single historic task instance: <i>history/historic-task -instances/{0:taskId}/variables/{1:variableName}</i>
     */
    public static final String[] URL_HISTORIC_TASK_INSTANCE_VARIABLE_DATA = { SEGMENT_HISTORY_RESOURCES, SEGMENT_HISTORIC_TASK_INSTANCE_RESOURCE, "{0}", SEGMENT_VARIABLES, "{1}", SEGMENT_VARIABLE_DATA };

    /**
     * URL template for historic activity instance query: <i>history/historic-activity-instances</i>
     */
    public static final String[] URL_HISTORIC_ACTIVITY_INSTANCES = { SEGMENT_HISTORY_RESOURCES, SEGMENT_HISTORIC_ACTIVITY_INSTANCE_RESOURCE };

    /**
     * URL template for historic variable instance query: <i>history/historic-variable-instances</i>
     */
    public static final String[] URL_HISTORIC_VARIABLE_INSTANCES = { SEGMENT_HISTORY_RESOURCES, SEGMENT_HISTORIC_VARIABLE_INSTANCE_RESOURCE };

    /**
     * URL template for a single historic variable instance data: <i>history/historic-variable-instances/{0:varInstanceId}/data</i>
     */
    public static final String[] URL_HISTORIC_VARIABLE_INSTANCE_DATA = { SEGMENT_HISTORY_RESOURCES, SEGMENT_HISTORIC_VARIABLE_INSTANCE_RESOURCE, "{0}", SEGMENT_VARIABLE_DATA };

    /**
     * URL template for a single historic milestone instance: <i>cmmn-history/historic-milestone-instances/{0:milestoneId}</i>
     */
    public static final String[] URL_HISTORIC_MILESTONE_INSTANCE = { SEGMENT_HISTORY_RESOURCES, SEGMENT_HISTORIC_MILESTONE_INSTANCE_RESOURCE, "{0}" };
    /**
     * URL template for a single historic milestone instance: <i>cmmn-history/historic-milestone-instances</i>
     */
    public static final String[] URL_HISTORIC_MILESTONE_INSTANCES = { SEGMENT_HISTORY_RESOURCES, SEGMENT_HISTORIC_MILESTONE_INSTANCE_RESOURCE};

    /**
     * URL template for a single historic plan item instance: <i>cmmn-history/historic-planitem-instances/{0:planItemId}</i>
     */
    public static final String[] URL_HISTORIC_PLANITEM_INSTANCE = { SEGMENT_HISTORY_RESOURCES, SEGMENT_HISTORIC_PLANITEM_INSTANCE_RESOURCE, "{0}" };

    /**
     * URL template for a single historic plan item instance: <i>cmmn-history/historic-planitem-instances</i>
     */
    public static final String[] URL_HISTORIC_PLANITEM_INSTANCES = { SEGMENT_HISTORY_RESOURCES, SEGMENT_HISTORIC_PLANITEM_INSTANCE_RESOURCE};

   /**
     * URL template for historic case instance query: <i>query/historic-case-instances</i>
     */
    public static final String[] URL_HISTORIC_CASE_INSTANCE_QUERY = { SEGMENT_QUERY_RESOURCES, SEGMENT_HISTORIC_CASE_INSTANCE_RESOURCE };

    /**
     * URL template for historic process instance query: <i>query/historic-task-instances</i>
     */
    public static final String[] URL_HISTORIC_TASK_INSTANCE_QUERY = { SEGMENT_QUERY_RESOURCES, SEGMENT_HISTORIC_TASK_INSTANCE_RESOURCE };

    /**
     * URL template for historic activity instance query: <i>query/historic-activity-instances</i>
     */
    public static final String[] URL_HISTORIC_ACTIVITY_INSTANCE_QUERY = { SEGMENT_QUERY_RESOURCES, SEGMENT_HISTORIC_ACTIVITY_INSTANCE_RESOURCE };

    /**
     * URL template for historic variable instance query: <i>query/historic-variable-instances</i>
     */
    public static final String[] URL_HISTORIC_VARIABLE_INSTANCE_QUERY = { SEGMENT_QUERY_RESOURCES, SEGMENT_HISTORIC_VARIABLE_INSTANCE_RESOURCE };

    /**
     * URL template for historic milestone instance query: <i>query/historic-milestone-instances</i>
     */
    public static final String[] URL_HISTORIC_MILESTONE_INSTANCE_QUERY = { SEGMENT_QUERY_RESOURCES, SEGMENT_HISTORIC_MILESTONE_INSTANCE_RESOURCE };

    /**
     * URL template for historic plan item instance query: <i>query/historic-planitem-instances</i>
     */
    public static final String[] URL_HISTORIC_PLANITEM_INSTANCE_QUERY = { SEGMENT_QUERY_RESOURCES, SEGMENT_HISTORIC_PLANITEM_INSTANCE_RESOURCE };

    /**
     * URL template for the collection of properties: <i>management/properties</i>
     */
    public static final String[] URL_PROPERTIES_COLLECTION = { SEGMENT_MANAGEMENT_RESOURCES, SEGMENT_PROPERTIES };

    /**
     * URL template for the collection of properties: <i>management/properties</i>
     */
    public static final String[] URL_ENGINE_INFO = { SEGMENT_MANAGEMENT_RESOURCES, SEGMENT_ENGINE_INFO };

    /**
     * Creates an url based on the passed fragments and replaces any placeholders with the given arguments. The placeholders are following the {@link MessageFormat} convention (eg. {0} is replaced by
     * first argument value).
     */
    public static String createRelativeResourceUrl(String[] segments, Object... arguments) {
        return MessageFormat.format(StringUtils.join(segments, '/'), arguments);
    }
}
