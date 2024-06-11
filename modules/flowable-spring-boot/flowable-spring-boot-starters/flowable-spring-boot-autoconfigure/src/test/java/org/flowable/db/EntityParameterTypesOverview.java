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
package org.flowable.db;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

public abstract class EntityParameterTypesOverview {

    public static String PARAMETER_TYPE_VARCHAR = "VARCHAR";
    public static String PARAMETER_TYPE_NVARCHAR = "NVARCHAR";
    public static String PARAMETER_TYPE_INTEGER = "INTEGER";
    public static String PARAMETER_TYPE_BOOLEAN = "BOOLEAN";
    public static String PARAMETER_TYPE_TIMESTAMP = "TIMESTAMP";
    public static String PARAMETER_TYPE_BIGINT = "BIGINT";
    public static String PARAMETER_TYPE_DOUBLE = "DOUBLE";
    public static String PARAMETER_TYPE_BLOBTYPE = "${blobType}";

    public static Map<String, ParameterInfo> ALL_PARAMS = new HashMap<>();


    static {
        // For Liquibase-managed engines, the type will most often be VARCHAR
        // For 'older' engines, the type will always be nvarchar.

        // BPMN
        addActivityInstanceParams();
        addAttachmentParams();
        addDeploymentParams();
        addEventLogEntryParams();
        addExecutionParams();
        addProcessDefinitionParams();
        addTaskParams();
        addTimerJobParams();

        // CMMN
        addCaseDefinitionParams();
        addCaseInstanceParams();
        addCmmnResourceParams();
        addMilestoneInstanceParams();
        
        // SERVICES
        addEventSubscriptionParams();
    }

    protected static void addActivityInstanceParams() {
        ParameterInfo info = addParameterInfo("activityInstance");
        info.addColumn("ID_", "id", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("PROC_DEF_ID_", "processDefinitionId", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("PROC_INST_ID_", "processInstanceId", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("EXECUTION_ID_", "executionId", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("ACT_ID_", "activityId", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("TASK_ID_", "taskId", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("CALL_PROC_INST_ID_", "calledProcessInstanceId", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("ACT_NAME_", "activityName", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("ACT_TYPE_", "activityType", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("ASSIGNEE_", "assignee", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("START_TIME_", "startTime", PARAMETER_TYPE_TIMESTAMP);
        info.addColumn("END_TIME_", "endTime", PARAMETER_TYPE_TIMESTAMP);
        info.addColumn("TRANSACTION_ORDER_", "transactionOrder", PARAMETER_TYPE_INTEGER);
        info.addColumn("DURATION_", "durationInMillis", PARAMETER_TYPE_BIGINT);
        info.addColumn("DELETE_REASON_", "deleteReason", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("TENANT_ID_", "tenantId", PARAMETER_TYPE_NVARCHAR);

        info.addQueryParameter("activityInstanceId", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("tenantIdLike", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("deleteReasonLike", PARAMETER_TYPE_NVARCHAR);
    }

    protected static void addAttachmentParams() {
        ParameterInfo info = addParameterInfo("attachment");
        info.addColumn("ID_", "id", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("NAME_", "name", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("DESCRIPTION_", "description", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("TYPE_", "type", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("TASK_ID_", "taskId", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("PROC_INST_ID_", "processInstanceId", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("URL_", "url", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("CONTENT_ID_", "contentId", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("USER_ID_", "userId", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("TIME_", "time", PARAMETER_TYPE_TIMESTAMP);
    }

    protected static void addDeploymentParams() {
        ParameterInfo info = addParameterInfo("deployment");
        info.addColumn("ID_", "id", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("NAME_", "name", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("CATEGORY_", "category", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("KEY_", "key", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("TENANT_ID_", "tenantId", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("DEPLOY_TIME_", "deploymentTime", PARAMETER_TYPE_TIMESTAMP);
        info.addColumn("DERIVED_FROM_", "derivedFrom", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("DERIVED_FROM_ROOT_", "derivedFromRoot", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("PARENT_DEPLOYMENT_ID_", "parentDeploymentId", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("ENGINE_VERSION_", "engineVersion", PARAMETER_TYPE_NVARCHAR);

        info.addQueryParameter("deploymentId", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("nameLike", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("categoryLike", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("categoryNotEquals", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("keyLike", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("tenantIdLike", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("parentDeploymentIdLike", PARAMETER_TYPE_NVARCHAR);

        info.addQueryParameter("processDefinitionKey", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("processDefinitionKeyLike", PARAMETER_TYPE_NVARCHAR);
    }

    protected static void addEventLogEntryParams() {
        ParameterInfo info = addParameterInfo("eventLogEntry");
        info.addColumn("LOG_NR_", "logNr", PARAMETER_TYPE_BIGINT);
        info.addColumn("TYPE_", "type", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("PROC_DEF_ID_", "processDefinitionId", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("PROC_INST_ID_", "processInstanceId", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("EXECUTION_ID_", "executionId", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("TASK_ID_", "taskId", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("TIME_STAMP_", "timeStamp", PARAMETER_TYPE_TIMESTAMP);
        info.addColumn("USER_ID_", "userId", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("DATA_", "data", PARAMETER_TYPE_BLOBTYPE);
        info.addColumn("LOCK_OWNER_", "lockOwner", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("LOCK_TIME_", "lockTime", PARAMETER_TYPE_TIMESTAMP);
        info.addColumn("IS_PROCESSED_", "isProcessed", PARAMETER_TYPE_INTEGER);

        // Query
        info.addQueryParameter("startLogNr", PARAMETER_TYPE_BIGINT);
        info.addQueryParameter("endLogNr", PARAMETER_TYPE_BIGINT);
    }
    
    protected static void addExecutionParams() {
        ParameterInfo info = addParameterInfo("execution");
        info.addColumn("ID_", "id", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("REV_", "revision", PARAMETER_TYPE_INTEGER);
        info.addColumn("PROC_INST_ID_", "processInstanceId", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("BUSINESS_KEY_", "businessKey", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("BUSINESS_STATUS_", "businessStatus", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("PROC_DEF_ID_", "processDefinitionId", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("ACT_ID_", "activityId", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("IS_ACTIVE_", "isActive", PARAMETER_TYPE_BOOLEAN);
        info.addColumn("IS_CONCURRENT_", "isConcurrent", PARAMETER_TYPE_BOOLEAN);
        info.addColumn("IS_SCOPE_", "isScope", PARAMETER_TYPE_BOOLEAN);
        info.addColumn("IS_EVENT_SCOPE_", "isEventScope", PARAMETER_TYPE_BOOLEAN);
        info.addColumn("IS_MI_ROOT_", "isMultiInstanceRoot", PARAMETER_TYPE_BOOLEAN);
        info.addColumn("PARENT_ID_", "parentId", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("SUPER_EXEC_", "superExecutionId", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("ROOT_PROC_INST_ID_", "rootProcessInstanceId", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("SUSPENSION_STATE_", "suspensionState", PARAMETER_TYPE_INTEGER);
        info.addColumn("TENANT_ID_", "tenantId", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("NAME_", "name", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("START_ACT_ID_", "startActivityId", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("START_TIME_", "startTime", PARAMETER_TYPE_TIMESTAMP);
        info.addColumn("START_USER_ID_", "startUserId", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("IS_COUNT_ENABLED_", "isCountEnabled", PARAMETER_TYPE_BOOLEAN);
        info.addColumn("EVT_SUBSCR_COUNT_", "eventSubscriptionCount", PARAMETER_TYPE_INTEGER);
        info.addColumn("TASK_COUNT_", "taskCount", PARAMETER_TYPE_INTEGER);
        info.addColumn("JOB_COUNT_", "jobCount", PARAMETER_TYPE_INTEGER);
        info.addColumn("TIMER_JOB_COUNT_", "timerJobCount", PARAMETER_TYPE_INTEGER);
        info.addColumn("SUSP_JOB_COUNT_", "suspendedJobCount", PARAMETER_TYPE_INTEGER);
        info.addColumn("DEADLETTER_JOB_COUNT_", "deadLetterJobCount", PARAMETER_TYPE_INTEGER);
        info.addColumn("EXTERNAL_WORKER_JOB_COUNT_", "externalWorkerJobCount", PARAMETER_TYPE_INTEGER);
        info.addColumn("VAR_COUNT_", "variableCount", PARAMETER_TYPE_INTEGER);
        info.addColumn("ID_LINK_COUNT_", "identityLinkCount", PARAMETER_TYPE_INTEGER);
        info.addColumn("CALLBACK_ID_", "callbackId", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("CALLBACK_TYPE_", "callbackType", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("REFERENCE_ID_", "referenceId", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("REFERENCE_TYPE_", "referenceType", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("PROPAGATED_STAGE_INST_ID_", "propagatedStageInstanceId", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("LOCK_TIME_", "lockTime", PARAMETER_TYPE_TIMESTAMP);
        info.addColumn("LOCK_OWNER_", "lockOwner", PARAMETER_TYPE_NVARCHAR);

        // Process instance
        info.addColumn("ProcessDefinitionName", "processDefinitionName", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("ProcessDefinitionKey", "processDefinitionKey", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("ProcessDefinitionVersion", "processDefinitionVersion", PARAMETER_TYPE_INTEGER);
        info.addColumn("ProcessDefinitionCategory", "processDefinitionCategory", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("ProcessDefinitionEngineVersion", "processDefinitionEngineVersion", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("DeploymentId", "deploymentId", PARAMETER_TYPE_NVARCHAR);

        // Variables
        addVariableColumnsWhenUsedInQueries(info);

        info.addQueryParameter("executionId", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("deploymentId", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("expirationTime", PARAMETER_TYPE_TIMESTAMP);
        info.addQueryParameter("parentExecutionId", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("businessKeyLike", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("businessStatusLike", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("superProcessInstanceId", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("subProcessInstanceId", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("tenantIdLike", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("nameLike", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("nameLikeIgnoreCase", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("activeActivityId", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("involvedUser", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("queryVariableValue.name", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("queryVariableValue.type", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("queryVariableValue.textValue", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("queryVariableValue.textValue2", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("queryVariableValue.longValue", PARAMETER_TYPE_BIGINT);
        info.addQueryParameter("queryVariableValue.doubleValue", PARAMETER_TYPE_DOUBLE);
        info.addQueryParameter("startedBefore", PARAMETER_TYPE_TIMESTAMP);
        info.addQueryParameter("startedAfter", PARAMETER_TYPE_TIMESTAMP);
        info.addQueryParameter("startedBy", PARAMETER_TYPE_NVARCHAR);

        // Event
        info.addQueryParameter("eventSubscriptionValue.eventType", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("eventSubscriptionValue.eventName", PARAMETER_TYPE_NVARCHAR);

        // Identity link
        info.addQueryParameter("involvedUserIdentityLink.userId", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("involvedUserIdentityLink.type", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("groupId", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("involvedGroupIdentityLink.groupId", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("involvedGroupIdentityLink.type", PARAMETER_TYPE_NVARCHAR);

        // EntityLink
        info.addQueryParameter("parentScopeId", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("rootScopeId", PARAMETER_TYPE_NVARCHAR);
    }

    protected static void addProcessDefinitionParams() {
        ParameterInfo info = addParameterInfo("processDefinition");
        info.addColumn("ID_", "id", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("CATEGORY_", "category", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("NAME_", "name", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("KEY_", "key", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("VERSION_", "version", PARAMETER_TYPE_INTEGER);
        info.addColumn("DEPLOYMENT_ID_", "deploymentId", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("RESOURCE_NAME_", "resourceName", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("TENANT_ID_", "tenantId", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("DGRM_RESOURCE_NAME_", "diagramResourceName", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("DESCRIPTION_", "description", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("HAS_START_FORM_KEY_", "hasStartFormKey", PARAMETER_TYPE_BOOLEAN);
        info.addColumn("HAS_GRAPHICAL_NOTATION_", "isGraphicalNotationDefined", PARAMETER_TYPE_BOOLEAN);
        info.addColumn("SUSPENSION_STATE_", "suspensionState", PARAMETER_TYPE_INTEGER);
        info.addColumn("DERIVED_FROM_", "derivedFrom", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("DERIVED_FROM_ROOT_", "derivedFromRoot", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("DERIVED_VERSION_", "derivedVersion", PARAMETER_TYPE_INTEGER);
        info.addColumn("ENGINE_VERSION_", "engineVersion", PARAMETER_TYPE_NVARCHAR);

        info.addQueryParameter("processDefinitionId", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("categoryLike", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("categoryNotEquals", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("nameLike", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("nameLikeIgnoreCase", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("keyLike", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("resourceNameLike", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("versionGt", PARAMETER_TYPE_INTEGER);
        info.addQueryParameter("versionGte", PARAMETER_TYPE_INTEGER);
        info.addQueryParameter("versionLt", PARAMETER_TYPE_INTEGER);
        info.addQueryParameter("versionLte", PARAMETER_TYPE_INTEGER);
        info.addQueryParameter("tenantIdLike", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("suspensionState.stateCode", PARAMETER_TYPE_INTEGER);

        info.addQueryParameter("parentDeploymentId", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("processDefinitionKey", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("processDefinitionVersion", PARAMETER_TYPE_INTEGER);

        info.addQueryParameter("eventSubscriptionType", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("eventSubscriptionName", PARAMETER_TYPE_NVARCHAR);

        info.addQueryParameter("authorizationUserId", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("groupId", PARAMETER_TYPE_NVARCHAR);

    }

    protected static void addTaskParams() {
        ParameterInfo info = addParameterInfo("task");
        info.addColumn("ID_", "id", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("NAME_", "name", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("DESCRIPTION_", "description", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("PARENT_TASK_ID_", "parentTaskId", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("PRIORITY_", "priority", PARAMETER_TYPE_INTEGER);
        info.addColumn("CREATE_TIME_", "createTime", PARAMETER_TYPE_TIMESTAMP );
        info.addColumn("IN_PROGRESS_TIME_", "inProgressStartTime", PARAMETER_TYPE_TIMESTAMP );
        info.addColumn("IN_PROGRESS_STARTED_BY_", "inProgressStartedBy", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("CLAIM_TIME_", "claimTime", PARAMETER_TYPE_TIMESTAMP );
        info.addColumn("CLAIMED_BY_", "claimedBy", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("SUSPENDED_TIME_", "suspendedTime", PARAMETER_TYPE_TIMESTAMP );
        info.addColumn("SUSPENDED_BY_", "suspendedBy", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("OWNER_", "owner", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("ASSIGNEE_", "assignee", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("DELEGATION_", "delegationStateString", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("EXECUTION_ID_", "executionId", PARAMETER_TYPE_NVARCHAR );
        info.addColumn("PROC_INST_ID_", "processInstanceId", PARAMETER_TYPE_NVARCHAR );
        info.addColumn("PROC_DEF_ID_", "processDefinitionId", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("TASK_DEF_ID_", "taskDefinitionId", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("SCOPE_ID_", "scopeId", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("SUB_SCOPE_ID_", "subScopeId", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("SCOPE_TYPE_", "scopeType", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("SCOPE_DEFINITION_ID_", "scopeDefinitionId", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("PROPAGATED_STAGE_INST_ID_", "propagatedStageInstanceId", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("TASK_DEF_KEY_", "taskDefinitionKey", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("STATE_", "state", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("IN_PROGRESS_DUE_DATE_", "inProgressStartDueDate", PARAMETER_TYPE_TIMESTAMP);
        info.addColumn("DUE_DATE_", "dueDate", PARAMETER_TYPE_TIMESTAMP);
        info.addColumn("CATEGORY_", "category", PARAMETER_TYPE_NVARCHAR );
        info.addColumn("SUSPENSION_STATE_", "suspensionState", PARAMETER_TYPE_INTEGER );
        info.addColumn("TENANT_ID_", "tenantId", PARAMETER_TYPE_NVARCHAR );
        info.addColumn("FORM_KEY_", "formKey", PARAMETER_TYPE_NVARCHAR );
        info.addColumn("IS_COUNT_ENABLED_", "isCountEnabled", PARAMETER_TYPE_BOOLEAN );
        info.addColumn("VAR_COUNT_", "variableCount", PARAMETER_TYPE_INTEGER );
        info.addColumn("ID_LINK_COUNT_", "identityLinkCount", PARAMETER_TYPE_INTEGER );
        info.addColumn("SUB_TASK_COUNT_", "subTaskCount", PARAMETER_TYPE_INTEGER );

        // Task Query
        info.addQueryParameter("deploymentId", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("orQueryObject.deploymentId", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("assigneeId", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("assigneeLike", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("assigneeLikeIgnoreCase", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("candidateUser", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("caseDefinitionKey", PARAMETER_TYPE_VARCHAR); // CMMN = liquibase
        info.addQueryParameter("caseDefinitionKeyLike", PARAMETER_TYPE_VARCHAR); // CMMN = liquibase
        info.addQueryParameter("caseDefinitionKeyLikeIgnoreCase", PARAMETER_TYPE_VARCHAR);  // CMMN = liquibase
        info.addQueryParameter("caseInstanceIdWithChildren", PARAMETER_TYPE_VARCHAR);  // CMMN = liquibase
        info.addQueryParameter("cmmnDeploymentId", PARAMETER_TYPE_VARCHAR);  // CMMN = liquibase
        info.addQueryParameter("createTimeAfter", PARAMETER_TYPE_TIMESTAMP);
        info.addQueryParameter("createTimeBefore", PARAMETER_TYPE_TIMESTAMP);
        info.addQueryParameter("descriptionLike", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("descriptionLikeIgnoreCase", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("dueAfter", PARAMETER_TYPE_TIMESTAMP);
        info.addQueryParameter("dueBefore", PARAMETER_TYPE_TIMESTAMP);
        info.addQueryParameter("groupId", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("inProgressStartTimeAfter", PARAMETER_TYPE_TIMESTAMP);
        info.addQueryParameter("inProgressStartTimeBefore", PARAMETER_TYPE_TIMESTAMP);
        info.addQueryParameter("involvedUser", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("key", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("keyLike", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("maxPriority", PARAMETER_TYPE_INTEGER);
        info.addQueryParameter("minPriority", PARAMETER_TYPE_INTEGER);
        info.addQueryParameter("nameLike", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("nameLikeIgnoreCase", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("ownerLike", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("ownerLikeIgnoreCase", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("parentScopeId", PARAMETER_TYPE_NVARCHAR); // From entity link
        info.addQueryParameter("processCategory", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("processDefinitionKey", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("processDefinitionKeyLike", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("processDefinitionKeyLikeIgnoreCase", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("processDefinitionName", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("processDefinitionNameLike", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("processInstanceBusinessKey", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("processInstanceBusinessKeyLike", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("processInstanceBusinessKeyLikeIgnoreCase", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("processInstanceIdWithChildren", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("rootScopeId", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("taskId", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("tenantIdLike", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("userIdForCandidateAndAssignee", PARAMETER_TYPE_NVARCHAR);

        // Variables are returned together with tasks
        addVariableColumnsWhenUsedInQueries(info);

        // Identitylinks
        info.addColumn("ILINK_ID_", "id", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("ILINK_TYPE_", "type", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("ILINK_USER_ID_", "userId", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("ILINK_GROUP_ID_", "groupId", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("ILINK_TASK_ID_", "taskId", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("ILINK_PROC_INST_ID_", "processInstanceId", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("ILINK_PROC_DEF_ID_", "processDefinitionId", PARAMETER_TYPE_NVARCHAR);

    }

    protected static void addVariableColumnsWhenUsedInQueries(ParameterInfo info) {
        info.addColumn("VAR_ID_", "var.id", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("VAR_NAME_", "var.name", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("VAR_TYPE_", "var.type", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("VAR_REV_", "var.revision", PARAMETER_TYPE_INTEGER);
        info.addColumn("VAR_PROC_INST_ID_", "var.processInstanceId", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("VAR_EXECUTION_ID_", "var.executionId", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("VAR_TASK_ID_", "var.taskId", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("VAR_META_INFO_", "var.taskId", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("VAR_BYTEARRAY_ID_", "var.byteArrayRef", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("VAR_DOUBLE_", "var.doubleValue", PARAMETER_TYPE_DOUBLE);
        info.addColumn("VAR_TEXT_", "var.textValue", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("VAR_TEXT2_", "var.textValue2", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("VAR_LONG_", "var.longValue", PARAMETER_TYPE_BIGINT);
        info.addColumn("VAR_SCOPE_ID_", "var.scopeId", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("VAR_SUB_SCOPE_ID_", "var.subScopeId", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("VAR_SCOPE_TYPE_", "var.scopeType", PARAMETER_TYPE_NVARCHAR);
    }
    
    public static void addCaseDefinitionParams() {
        ParameterInfo info = addParameterInfo("caseDefinition");
        info.addColumn("ID_", "id", PARAMETER_TYPE_VARCHAR);
        info.addColumn("NAME_", "name", PARAMETER_TYPE_VARCHAR);
        info.addColumn("KEY_", "key", PARAMETER_TYPE_VARCHAR);
        info.addColumn("VERSION_", "version", PARAMETER_TYPE_INTEGER);
        info.addColumn("DEPLOYMENT_ID_", "deploymentId", PARAMETER_TYPE_VARCHAR);
        info.addColumn("RESOURCE_NAME_", "resourceName", PARAMETER_TYPE_VARCHAR);
        info.addColumn("DGRM_RESOURCE_NAME_", "diagramResourceName", PARAMETER_TYPE_VARCHAR);
        info.addColumn("DESCRIPTION_", "description", PARAMETER_TYPE_VARCHAR);
        info.addColumn("HAS_START_FORM_KEY_", "hasStartFormKey", PARAMETER_TYPE_BOOLEAN);
        info.addColumn("HAS_GRAPHICAL_NOTATION_", "isGraphicalNotationDefined", PARAMETER_TYPE_BOOLEAN);
        info.addColumn("TENANT_ID_", "tenantId", PARAMETER_TYPE_VARCHAR);
    }
    
    public static void addCaseInstanceParams() {
        ParameterInfo info = addParameterInfo("caseInstance");
        info.addColumn("ID_", "id", PARAMETER_TYPE_VARCHAR);
        info.addColumn("REV_", "rev", PARAMETER_TYPE_INTEGER);
        info.addColumn("PARENT_ID_", "parentId", PARAMETER_TYPE_VARCHAR);
        info.addColumn("CASE_DEF_ID_", "caseDefinitionId", PARAMETER_TYPE_VARCHAR);
        info.addColumn("BUSINESS_KEY_", "businessKey", PARAMETER_TYPE_VARCHAR);
        info.addColumn("NAME_", "name", PARAMETER_TYPE_VARCHAR);
        info.addColumn("STATE_", "state", PARAMETER_TYPE_VARCHAR);
        info.addColumn("START_TIME_", "startTime", PARAMETER_TYPE_TIMESTAMP);
        info.addColumn("START_USER_ID_", "startUserId", PARAMETER_TYPE_VARCHAR);
        info.addColumn("LAST_REACTIVATION_TIME_", "lastReactivationTime", PARAMETER_TYPE_TIMESTAMP);
        info.addColumn("LAST_REACTIVATION_USER_ID_", "lastReactivationUserId", PARAMETER_TYPE_VARCHAR);
        info.addColumn("CALLBACK_ID_", "callbackId", PARAMETER_TYPE_VARCHAR);
        info.addColumn("CALLBACK_TYPE_", "callbackType", PARAMETER_TYPE_VARCHAR);
        info.addColumn("REFERENCE_ID_", "referenceId", PARAMETER_TYPE_VARCHAR);
        info.addColumn("REFERENCE_TYPE_", "referenceType", PARAMETER_TYPE_VARCHAR);
        info.addColumn("IS_COMPLETEABLE_", "completeable", PARAMETER_TYPE_BOOLEAN);
        info.addColumn("TENANT_ID_", "tenantId", PARAMETER_TYPE_VARCHAR);
        info.addColumn("BUSINESS_STATUS_", "businessStatus", PARAMETER_TYPE_VARCHAR);
        info.addColumn("LOCK_TIME_", "lockTime", PARAMETER_TYPE_TIMESTAMP);
        info.addColumn("LOCK_OWNER_", "lockOwner", PARAMETER_TYPE_VARCHAR);
        info.addColumn("CaseDefinitionKey", "caseDefinitionKey", PARAMETER_TYPE_VARCHAR);
        info.addColumn("CaseDefinitionName", "caseDefinitionName", PARAMETER_TYPE_VARCHAR);
        info.addColumn("CaseDefinitionVersion", "caseDefinitionVersion", PARAMETER_TYPE_INTEGER);
        info.addColumn("CaseDefinitionDeploymentId", "caseDefinitionDeploymentId", PARAMETER_TYPE_VARCHAR);
        
        // Variables are returned together with case instances
        info.addColumn("VAR_ID_", "var.id", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("VAR_NAME_", "var.name", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("VAR_TYPE_", "var.type", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("VAR_REV_", "var.revision", PARAMETER_TYPE_INTEGER);
        info.addColumn("VAR_PROC_INST_ID_", "var.processInstanceId", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("VAR_EXECUTION_ID_", "var.executionId", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("VAR_TASK_ID_", "var.taskId", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("VAR_META_INFO_", "var.taskId", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("VAR_BYTEARRAY_ID_", "var.byteArrayRef", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("VAR_DOUBLE_", "var.doubleValue", PARAMETER_TYPE_DOUBLE);
        info.addColumn("VAR_TEXT_", "var.textValue", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("VAR_TEXT2_", "var.textValue2", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("VAR_LONG_", "var.longValue", PARAMETER_TYPE_BIGINT);
        info.addColumn("VAR_SCOPE_ID_", "var.scopeId", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("VAR_SUB_SCOPE_ID_", "var.subScopeId", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("VAR_SCOPE_TYPE_", "var.scopeType", PARAMETER_TYPE_NVARCHAR);
        
        info.addQueryParameter("caseInstanceId", PARAMETER_TYPE_VARCHAR);
        info.addQueryParameter("planItemInstanceId", PARAMETER_TYPE_VARCHAR);
        info.addQueryParameter("expirationTime", PARAMETER_TYPE_TIMESTAMP);
        info.addQueryParameter("caseDefinitionCategory", PARAMETER_TYPE_VARCHAR);
        info.addQueryParameter("caseDefinitionName", PARAMETER_TYPE_VARCHAR);
        info.addQueryParameter("caseDefinitionVersion", PARAMETER_TYPE_INTEGER);
        info.addQueryParameter("caseInstanceParentId", PARAMETER_TYPE_VARCHAR);
        info.addQueryParameter("nameLike", PARAMETER_TYPE_VARCHAR);
        info.addQueryParameter("nameLikeIgnoreCase", PARAMETER_TYPE_VARCHAR);
        info.addQueryParameter("parentScopeId", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("rootScopeId", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("startedBefore", PARAMETER_TYPE_TIMESTAMP);
        info.addQueryParameter("startedAfter", PARAMETER_TYPE_TIMESTAMP);
        info.addQueryParameter("startedBy", PARAMETER_TYPE_VARCHAR);
        info.addQueryParameter("lastReactivatedBefore", PARAMETER_TYPE_TIMESTAMP);
        info.addQueryParameter("lastReactivatedAfter", PARAMETER_TYPE_TIMESTAMP);
        info.addQueryParameter("lastReactivatedBy", PARAMETER_TYPE_VARCHAR);
        info.addQueryParameter("tenantIdLike", PARAMETER_TYPE_VARCHAR);
        info.addQueryParameter("activePlanItemDefinitionId", PARAMETER_TYPE_VARCHAR);
        info.addQueryParameter("involvedUser", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("involvedUserIdentityLink.userId", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("involvedUserIdentityLink.type", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("involvedGroup", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("involvedGroupIdentityLink.groupId", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("involvedGroupIdentityLink.type", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("queryVariableValue.name", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("queryVariableValue.type", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("queryVariableValue.textValue", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("queryVariableValue.textValue2", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("queryVariableValue.longValue", PARAMETER_TYPE_BIGINT);
        info.addQueryParameter("queryVariableValue.doubleValue", PARAMETER_TYPE_DOUBLE);
    }
    
    public static void addCmmnResourceParams() {
        ParameterInfo info = addParameterInfo("cmmnResource");
        info.addColumn("ID_", "id", PARAMETER_TYPE_VARCHAR);
        info.addColumn("NAME_", "name", PARAMETER_TYPE_VARCHAR);
        info.addColumn("RESOURCE_BYTES_", "bytes", PARAMETER_TYPE_BLOBTYPE);
        info.addColumn("DEPLOYMENT_ID_", "deploymentId", PARAMETER_TYPE_VARCHAR);
        info.addColumn("GENERATED_", "generated", PARAMETER_TYPE_BOOLEAN);
    }
    
    public static void addMilestoneInstanceParams() {
        ParameterInfo info = addParameterInfo("milestoneInstance");
        info.addColumn("ID_", "id", PARAMETER_TYPE_VARCHAR);
        info.addColumn("NAME_", "name", PARAMETER_TYPE_VARCHAR);
        info.addColumn("TIME_STAMP_", "timeStamp", PARAMETER_TYPE_TIMESTAMP);
        info.addColumn("CASE_INST_ID_", "caseInstanceId", PARAMETER_TYPE_VARCHAR);
        info.addColumn("CASE_DEF_ID_", "caseDefinitionId", PARAMETER_TYPE_VARCHAR);
        info.addColumn("ELEMENT_ID_", "elementId", PARAMETER_TYPE_VARCHAR);
        info.addColumn("TENANT_ID_", "tenantId", PARAMETER_TYPE_VARCHAR);
        
        info.addQueryParameter("milestoneInstanceId", PARAMETER_TYPE_VARCHAR);
        info.addQueryParameter("reachedBefore", PARAMETER_TYPE_TIMESTAMP);
        info.addQueryParameter("reachedAfter", PARAMETER_TYPE_TIMESTAMP);
        info.addQueryParameter("tenantIdLike", PARAMETER_TYPE_VARCHAR); 
    }

    protected static void addTimerJobParams() {
        ParameterInfo info = addParameterInfo("timerJob");
        info.addColumn("ID_","id", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("CATEGORY_", "category", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("TYPE_", "jobType", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("LOCK_OWNER_", "lockOwner", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("LOCK_EXP_TIME_", "lockExpirationTime", PARAMETER_TYPE_TIMESTAMP);
        info.addColumn("EXCLUSIVE_", "exclusive", PARAMETER_TYPE_BOOLEAN);
        info.addColumn("EXECUTION_ID_", "executionId", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("PROCESS_INSTANCE_ID_", "processInstanceId", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("PROC_DEF_ID_", "processDefinitionId", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("ELEMENT_ID_", "elementId", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("ELEMENT_NAME_", "elementName", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("SCOPE_ID_", "scopeId", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("SUB_SCOPE_ID_", "subScopeId", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("SCOPE_TYPE_", "scopeType", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("SCOPE_DEFINITION_ID_", "scopeDefinitionId", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("CORRELATION_ID_", "correlationId", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("RETRIES_", "retries", PARAMETER_TYPE_INTEGER);
        info.addColumn("EXCEPTION_STACK_ID_", "exceptionByteArrayRef", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("EXCEPTION_MSG_", "exceptionMessage", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("HANDLER_TYPE_", "jobHandlerType", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("HANDLER_CFG_", "jobHandlerConfiguration", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("CUSTOM_VALUES_ID_", "customValuesByteArrayRef", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("CREATE_TIME_", "createTime", PARAMETER_TYPE_TIMESTAMP);
        info.addColumn("TENANT_ID_", "tenantId", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("DUEDATE_", "duedate", PARAMETER_TYPE_TIMESTAMP);
        info.addColumn("REPEAT_", "repeat", PARAMETER_TYPE_NVARCHAR);

        info.addQueryParameter("handlerType", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("processDefinitionKey", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("categoryLike", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("caseDefinitionKey", PARAMETER_TYPE_VARCHAR); // careful: cmmn engine --> varchar
        info.addQueryParameter("now", PARAMETER_TYPE_TIMESTAMP);
        info.addQueryParameter("duedateHigherThan", PARAMETER_TYPE_TIMESTAMP);
        info.addQueryParameter("duedateLowerThan", PARAMETER_TYPE_TIMESTAMP);
        info.addQueryParameter("duedateHigherThanOrEqual", PARAMETER_TYPE_TIMESTAMP);
        info.addQueryParameter("duedateLowerThanOrEqual", PARAMETER_TYPE_TIMESTAMP);
        info.addQueryParameter("tenantIdLike", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("jobExecutionScope", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("deploymentId", PARAMETER_TYPE_NVARCHAR);
    }
    
    public static void addEventSubscriptionParams() {
        ParameterInfo info = addParameterInfo("eventSubscription");
        info.addColumn("ID_", "id", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("REV_", "revision", PARAMETER_TYPE_INTEGER);
        info.addColumn("EVENT_TYPE_", "eventType", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("EVENT_NAME_", "eventName", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("EXECUTION_ID_", "executionId", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("PROC_INST_ID_", "processInstanceId", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("ACTIVITY_ID_", "activityId", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("CONFIGURATION_", "configuration", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("CREATED_", "created", PARAMETER_TYPE_TIMESTAMP);
        info.addColumn("PROC_DEF_ID_", "processDefinitionId", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("SUB_SCOPE_ID_", "subScopeId", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("SCOPE_ID_", "scopeId", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("SCOPE_DEFINITION_ID_", "scopeDefinitionId", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("SCOPE_DEFINITION_KEY_", "scopeDefinitionKey", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("SCOPE_TYPE_", "scopeType", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("LOCK_TIME_", "lockTime", PARAMETER_TYPE_TIMESTAMP);
        info.addColumn("LOCK_OWNER_", "lockOwner", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("TENANT_ID_", "tenantId", PARAMETER_TYPE_NVARCHAR);
        
        addEventSubscriptionAlias("messageEventSubscription", info);
        addEventSubscriptionAlias("signalEventSubscription", info);
        addEventSubscriptionAlias("compensateEventSubscription", info);
        
        info.addQueryParameter("createdBefore", PARAMETER_TYPE_TIMESTAMP);
        info.addQueryParameter("createdAfter", PARAMETER_TYPE_TIMESTAMP);
        info.addQueryParameter("parameter.eventName", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("parameter.eventType", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("parameter.executionId", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("parameter.activityId", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("parameter.processInstanceId", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("parameter.processDefinitionId", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("parameter.tenantId", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("parameter.scopeId", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("parameter.scopeType", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("newTenantId", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("oldTenantId", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("newProcessDefinitionId", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("oldProcessDefinitionId", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("newScopeDefinitionId", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("oldScopeDefinitionId", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("currentTime", PARAMETER_TYPE_TIMESTAMP);
    }
    
    protected static void addEventSubscriptionAlias(String alias, ParameterInfo info) {
        info.addColumn("ID_", alias + ".id", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("REV_", alias + ".revision", PARAMETER_TYPE_INTEGER);
        info.addColumn("EVENT_TYPE_", alias + ".eventType", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("EVENT_NAME_", alias + ".eventName", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("EXECUTION_ID_", alias + ".executionId", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("PROC_INST_ID_", alias + ".processInstanceId", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("ACTIVITY_ID_", alias + ".activityId", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("CONFIGURATION_", alias + ".configuration", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("CREATED_", alias + ".created", PARAMETER_TYPE_TIMESTAMP);
        info.addColumn("PROC_DEF_ID_", alias + ".processDefinitionId", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("SUB_SCOPE_ID_", alias + ".subScopeId", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("SCOPE_ID_", alias + ".scopeId", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("SCOPE_DEFINITION_ID_", alias + ".scopeDefinitionId", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("SCOPE_DEFINITION_KEY_", alias + ".scopeDefinitionKey", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("SCOPE_TYPE_", alias + ".scopeType", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("LOCK_TIME_", alias + ".lockTime", PARAMETER_TYPE_TIMESTAMP);
        info.addColumn("LOCK_OWNER_", alias + ".lockOwner", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("TENANT_ID_", alias + ".tenantId", PARAMETER_TYPE_NVARCHAR);
    }

    public static String getParameterType(String entity, String parameterName) {
        return getParameterInfo(entity).getParameterType(parameterName);
    }

    public static String getColumnType(String entity, String columnName) {
        return getParameterInfo(entity).getColumnType(columnName);
    }

    private static ParameterInfo getParameterInfo(String entity) {
        String lowerCasedEntity = entity.toLowerCase(Locale.ROOT);
        if (!ALL_PARAMS.containsKey(lowerCasedEntity)) {
            throw new RuntimeException("No parameter types found for " + entity);
        }
        ParameterInfo parameterInfo = ALL_PARAMS.get(lowerCasedEntity);
        return parameterInfo;
    }

    protected static ParameterInfo addParameterInfo(String alias) {
        ParameterInfo parameterInfo = new ParameterInfo(alias);
        ALL_PARAMS.put(alias.toLowerCase(Locale.ROOT), parameterInfo);
        return parameterInfo;
    }
    
    protected static class ParameterInfo {

        protected String alias;
        protected Map<String, String> columnToParameterMap = new HashMap<>();
        protected Map<String, String> parameterToJdbcTypeMap = new HashMap<>();

        public ParameterInfo(String alias) {
            this.alias = alias;

            // Common for all
            addColumn("REV_", "revision", PARAMETER_TYPE_INTEGER);
            addQueryParameter("revisionNext", PARAMETER_TYPE_INTEGER);
        }

        public ParameterInfo addColumn(String columnName, String parameter, String jdbcType) {
            columnToParameterMap.put(columnName, parameter);
            addParameter(parameter, jdbcType);
            return this;
        }

        public ParameterInfo addQueryParameter(String parameter, String jdbcType) {
            addParameter(parameter, jdbcType);
            return this;
        }

        protected void addParameter(String parameter, String jdbcType) {
            parameterToJdbcTypeMap.put(parameter, jdbcType);
            parameterToJdbcTypeMap.put(alias + "." + parameter, jdbcType);
            parameterToJdbcTypeMap.put("parameter." + parameter, jdbcType);
            parameterToJdbcTypeMap.put("orQueryObject." + parameter, jdbcType);
        }

        public String getColumnType(String columnName) {
            String parameterName = columnToParameterMap.get(columnName);
            if (StringUtils.isEmpty(parameterName)) {
                throw new RuntimeException("No parameter type set for column " + columnName);
            }
            return getParameterType(parameterName);
        }

        public String getParameterType(String parameterName) {
            return parameterToJdbcTypeMap.get(parameterName);
        }

    }

}
