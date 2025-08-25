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
package org.flowable.test.persistence;

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
        addBatchParams();
        addByteArrayParams();
        addCommentParams();
        addDeploymentParams();
        addHistoricEntityLinkParams();
        addEventLogEntryParams();
        addExecutionParams();
        addExternalWorkerJobParams();
        addHistoricActivityInstanceParams();
        addHistoricDetailParams();
        addHistoricProcessInstanceParams();
        addHistoricTaskInstanceParams();
        addJobParams();
        addModelParams();
        addProcessDefinitionParams();
        addProcessDefinitionInfoParams();
        addPropertyParams();
        addResourceParams();
        addTaskParams();
        addTimerJobParams();
        addVariableInstanceParams();

        // CMMN
        addCaseDefinitionParams();
        addCaseInstanceParams();
        addCmmnDeploymentParams();
        addCmmnResourceParams();
        addHistoricCaseInstanceParams();
        addHistoricMilestoneInstanceParams();
        addHistoricPlanItemInstanceParams();
        addMilestoneInstanceParams();
        addPlanItemInstanceParams();
        addSentryPartInstanceParams();
        
        // EVENT REGISTRY
        addChannelDefinitionParams();
        addEventDefinitionParams();
        addEventDeploymentParams();
        addEventResourceParams();

        // DMN
        addDecisionParams();
        addDmnDeploymentParams();
        addHistoricDecisionExecutionParams();
        addDmnResourceParams();

        // SERVICES
        addBatchPartParams();
        addDeadLetterJobParams();
        addEntityLinkParams();
        addEventSubscriptionParams();
        addHistoricIdentityLinkParams();
        addHistoryJobParams();
        addHistoricTaskLogEntryParams();
        addHistoricVariableInstanceParams();
        addIdentityLinkParams();
        addSuspendedJobParams();

        // App
        addAppDefinitionParams();
        addAppDeploymentParams();
        addAppResourceParams();

        // IDM
        addGroupParams();
        addIdmByteArrayParams();
        addPrivilegeParams();
        addPrivilegeMappingParams();
        addIdentityInfoParams();
        addIdmPropertyParams();
        addMembershipParams();
        addTokenParams();
        addUserParams();
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
        info.addColumn("COMPLETED_BY_", "completedBy", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("START_TIME_", "startTime", PARAMETER_TYPE_TIMESTAMP);
        info.addColumn("END_TIME_", "endTime", PARAMETER_TYPE_TIMESTAMP);
        info.addColumn("TRANSACTION_ORDER_", "transactionOrder", PARAMETER_TYPE_INTEGER);
        info.addColumn("DURATION_", "durationInMillis", PARAMETER_TYPE_BIGINT);
        info.addColumn("DELETE_REASON_", "deleteReason", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("TENANT_ID_", "tenantId", PARAMETER_TYPE_NVARCHAR);

        info.addQueryParameter("activityInstanceId", PARAMETER_TYPE_NVARCHAR);
    }

    protected static void addBatchParams() {
        ParameterInfo info = addParameterInfo("batch");
        info.addColumn("ID_", "id", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("REV_", "revision", PARAMETER_TYPE_INTEGER);
        info.addColumn("TYPE_", "batchType", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("SEARCH_KEY_", "batchSearchKey", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("SEARCH_KEY2_", "batchSearchKey2", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("CREATE_TIME_", "createTime", PARAMETER_TYPE_TIMESTAMP);
        info.addColumn("COMPLETE_TIME_", "completeTime", PARAMETER_TYPE_TIMESTAMP);
        info.addColumn("STATUS_", "status", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("BATCH_DOC_ID_", "batchDocRefId", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("TENANT_ID_", "tenantId", PARAMETER_TYPE_NVARCHAR);

        info.addQueryParameter("searchKey", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("searchKey2", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("parameter.searchKey", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("parameter.searchKey2", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("batchTypeItem", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("createTimeHigherThan", PARAMETER_TYPE_TIMESTAMP);
        info.addQueryParameter("createTimeLowerThan", PARAMETER_TYPE_TIMESTAMP);
        info.addQueryParameter("completeTimeLowerThan", PARAMETER_TYPE_TIMESTAMP);
        info.addQueryParameter("completeTimeHigherThan", PARAMETER_TYPE_TIMESTAMP);
    }

    protected static void addByteArrayParams() {
        ParameterInfo info = addParameterInfo("byteArray");
        info.addColumn("ID_", "id", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("REV_", "revision", PARAMETER_TYPE_INTEGER);
        info.addColumn("NAME_", "name", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("BYTES_", "bytes", PARAMETER_TYPE_BLOBTYPE);
        info.addColumn("DEPLOYMENT_ID_", "deploymentId", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("GENERATED_", "generated", PARAMETER_TYPE_BOOLEAN);
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

    protected static void addCommentParams() {
        ParameterInfo info = addParameterInfo("comment");
        info.addColumn("ID_", "id", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("TYPE_", "type", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("USER_ID_", "userId", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("TIME_", "time", PARAMETER_TYPE_TIMESTAMP);
        info.addColumn("TASK_ID_", "taskId", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("PROC_INST_ID_", "processInstanceId", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("ACTION_", "action", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("MESSAGE_", "message", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("FULL_MSG_", "fullMessageBytes", PARAMETER_TYPE_BLOBTYPE);
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
        info.addQueryParameter("processDefinitionKey", PARAMETER_TYPE_NVARCHAR);
    }

    protected static void addHistoricEntityLinkParams() {
        ParameterInfo info = addParameterInfo("historicEntityLink");
        info.addColumn("ID_", "id", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("LINK_TYPE_", "linkType", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("SCOPE_ID_", "scopeId", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("SUB_SCOPE_ID_", "subScopeId", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("SCOPE_TYPE_", "scopeType", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("SCOPE_DEFINITION_ID_", "scopeDefinitionId", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("PARENT_ELEMENT_ID_", "parentElementId", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("REF_SCOPE_ID_", "referenceScopeId", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("REF_SCOPE_TYPE_", "referenceScopeType", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("REF_SCOPE_DEFINITION_ID_", "referenceScopeDefinitionId", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("ROOT_SCOPE_ID_", "rootScopeId", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("ROOT_SCOPE_TYPE_", "rootScopeType", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("CREATE_TIME_", "createTime", PARAMETER_TYPE_TIMESTAMP);
        info.addColumn("HIERARCHY_TYPE_", "hierarchyType", PARAMETER_TYPE_NVARCHAR);
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
        info.addQueryParameter("superProcessInstanceId", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("subProcessInstanceId", PARAMETER_TYPE_NVARCHAR);
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
        info.addQueryParameter("variableName", PARAMETER_TYPE_NVARCHAR);
        
        info.addQueryParameter("parentCaseInstanceId", PARAMETER_TYPE_VARCHAR);

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

        // v5
        info.addColumn("CACHED_ENT_STATE_", "cachedEventState", PARAMETER_TYPE_INTEGER);
    }

    protected static void addExternalWorkerJobParams() {
        ParameterInfo info = addParameterInfo("externalWorkerJob");
        info.addColumn("ID_", "id", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("REV_", "revision", PARAMETER_TYPE_INTEGER);
        info.addColumn("CATEGORY_", "category", PARAMETER_TYPE_VARCHAR);
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

        info.addQueryParameter("deploymentId", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("workerId", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("jobId", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("topic", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("authorizedUser", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("groupId", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("jobExecutionScope", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("now", PARAMETER_TYPE_TIMESTAMP);
        info.addQueryParameter("handlerType", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("handlerConfiguration", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("processDefinitionKey", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("caseDefinitionKey", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("duedateHigherThan", PARAMETER_TYPE_TIMESTAMP);
        info.addQueryParameter("duedateLowerThan", PARAMETER_TYPE_TIMESTAMP);
        info.addQueryParameter("duedateHigherThanOrEqual", PARAMETER_TYPE_TIMESTAMP);
        info.addQueryParameter("duedateLowerThanOrEqual", PARAMETER_TYPE_TIMESTAMP);
    }

    protected static void addHistoricActivityInstanceParams() {
        ParameterInfo info = addParameterInfo("historicActivityInstance");
        info.addColumn("ID_", "id", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("REV_", "revision", PARAMETER_TYPE_INTEGER);
        info.addColumn("PROC_DEF_ID_", "processDefinitionId", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("PROC_INST_ID_", "processInstanceId", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("EXECUTION_ID_", "executionId", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("ACT_ID_", "activityId", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("TASK_ID_", "taskId", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("CALL_PROC_INST_ID_", "calledProcessInstanceId", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("ACT_NAME_", "activityName", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("ACT_TYPE_", "activityType", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("ASSIGNEE_", "assignee", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("COMPLETED_BY_", "completedBy", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("OWNER_", "owner", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("START_TIME_", "startTime", PARAMETER_TYPE_TIMESTAMP);
        info.addColumn("END_TIME_", "endTime", PARAMETER_TYPE_TIMESTAMP);
        info.addColumn("TRANSACTION_ORDER_", "transactionOrder", PARAMETER_TYPE_INTEGER);
        info.addColumn("DURATION_", "durationInMillis", PARAMETER_TYPE_BIGINT);
        info.addColumn("DELETE_REASON_", "deleteReason", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("TENANT_ID_", "tenantId", PARAMETER_TYPE_NVARCHAR);

        info.addQueryParameter("activityInstanceId", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("startedBefore", PARAMETER_TYPE_TIMESTAMP);
        info.addQueryParameter("startedAfter", PARAMETER_TYPE_TIMESTAMP);
        info.addQueryParameter("finishedBefore", PARAMETER_TYPE_TIMESTAMP);
        info.addQueryParameter("finishedAfter", PARAMETER_TYPE_TIMESTAMP);
    }

    protected static void addHistoricDetailParams() {
        ParameterInfo info = addParameterInfo("historicDetail", "historicFormProperty", "historicDetailVariableInstance");
        info.addColumn("ID_", "id", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("PROC_INST_ID_", "processInstanceId", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("ACT_INST_ID_", "activityInstanceId", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("EXECUTION_ID_", "executionId", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("TASK_ID_", "taskId", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("TIME_", "time", PARAMETER_TYPE_TIMESTAMP);
        info.addColumn("TYPE_", "type", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("NAME_", "name", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("REV_", "revision", PARAMETER_TYPE_INTEGER);
        info.addColumn("VAR_TYPE_", "variableType", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("ACTIVITY_ID_", "activityId", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("BYTEARRAY_ID_", "byteArrayRef", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("DOUBLE_", "doubleValue", PARAMETER_TYPE_DOUBLE);
        info.addColumn("TEXT_", "textValue", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("TEXT2_", "textValue2", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("LONG_", "longValue", PARAMETER_TYPE_BIGINT);

        info.addQueryParameter("detailType", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("variableName", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("propertyId", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("propertyValue", PARAMETER_TYPE_NVARCHAR);
    }

    protected static void addHistoricProcessInstanceParams() {
        ParameterInfo info = addParameterInfo("historicProcessInstance");
        info.addColumn("ID_", "id",  PARAMETER_TYPE_NVARCHAR);
        info.addColumn("REV_", "revision",  PARAMETER_TYPE_INTEGER);
        info.addColumn("PROC_INST_ID_", "processInstanceId",  PARAMETER_TYPE_NVARCHAR);
        info.addColumn("BUSINESS_KEY_", "businessKey",  PARAMETER_TYPE_NVARCHAR);
        info.addColumn("BUSINESS_STATUS_", "businessStatus",  PARAMETER_TYPE_NVARCHAR);
        info.addColumn("PROC_DEF_ID_", "processDefinitionId",  PARAMETER_TYPE_NVARCHAR);
        info.addColumn("PROC_DEF_NAME_", "processDefinitionName",  PARAMETER_TYPE_NVARCHAR);
        info.addColumn("PROC_DEF_KEY_", "processDefinitionKey",  PARAMETER_TYPE_NVARCHAR);
        info.addColumn("PROC_DEF_VERSION_", "processDefinitionVersion",  PARAMETER_TYPE_INTEGER);
        info.addColumn("PROC_DEF_CATEGORY_", "processDefinitionCategory",  PARAMETER_TYPE_NVARCHAR);
        info.addColumn("DEPLOYMENT_ID_", "deploymentId",  PARAMETER_TYPE_NVARCHAR);
        info.addColumn("START_TIME_", "startTime",  PARAMETER_TYPE_TIMESTAMP);
        info.addColumn("END_TIME_", "endTime",  PARAMETER_TYPE_TIMESTAMP);
        info.addColumn("DURATION_", "durationInMillis",  PARAMETER_TYPE_BIGINT);
        info.addColumn("START_USER_ID_", "startUserId",  PARAMETER_TYPE_NVARCHAR);
        info.addColumn("START_ACT_ID_", "startActivityId",  PARAMETER_TYPE_NVARCHAR);
        info.addColumn("END_ACT_ID_", "endActivityId",  PARAMETER_TYPE_NVARCHAR);
        info.addColumn("SUPER_PROCESS_INSTANCE_ID_", "superProcessInstanceId",  PARAMETER_TYPE_NVARCHAR);
        info.addColumn("DELETE_REASON_", "deleteReason",  PARAMETER_TYPE_NVARCHAR);
        info.addColumn("TENANT_ID_", "tenantId",  PARAMETER_TYPE_NVARCHAR);
        info.addColumn("NAME_", "name",  PARAMETER_TYPE_NVARCHAR);
        info.addColumn("CALLBACK_ID_", "callbackId",  PARAMETER_TYPE_NVARCHAR);
        info.addColumn("CALLBACK_TYPE_", "callbackType",  PARAMETER_TYPE_NVARCHAR);
        info.addColumn("REFERENCE_ID_", "referenceId",  PARAMETER_TYPE_NVARCHAR);
        info.addColumn("REFERENCE_TYPE_", "referenceType",  PARAMETER_TYPE_NVARCHAR);
        info.addColumn("PROPAGATED_STAGE_INST_ID_", "propagatedStageInstanceId",  PARAMETER_TYPE_NVARCHAR);
        info.addColumn("STATE_", "state",  PARAMETER_TYPE_NVARCHAR);
        info.addColumn("END_USER_ID_", "endUserId",  PARAMETER_TYPE_NVARCHAR);


        addVariableColumnsWhenUsedInQueries(info);

        info.addQueryParameter("deploymentId", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("superProcessInstanceId", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("subProcessInstanceId", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("activeActivityId", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("startedBy", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("startedBefore", PARAMETER_TYPE_TIMESTAMP);
        info.addQueryParameter("startedAfter", PARAMETER_TYPE_TIMESTAMP);
        info.addQueryParameter("finishedBefore", PARAMETER_TYPE_TIMESTAMP);
        info.addQueryParameter("finishedAfter", PARAMETER_TYPE_TIMESTAMP);

        info.addQueryParameter("queryVariableValue.name", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("queryVariableValue.type", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("queryVariableValue.textValue", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("queryVariableValue.textValue2", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("queryVariableValue.longValue", PARAMETER_TYPE_BIGINT);
        info.addQueryParameter("queryVariableValue.doubleValue", PARAMETER_TYPE_DOUBLE);

        info.addQueryParameter("involvedUser", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("involvedUserIdentityLink.userId", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("involvedUserIdentityLink.type", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("involvedGroup", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("involvedGroupIdentityLink.groupId", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("involvedGroupIdentityLink.type", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("group", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("variableName", PARAMETER_TYPE_NVARCHAR);
        
        info.addQueryParameter("parentCaseInstanceId", PARAMETER_TYPE_VARCHAR);

        info.addQueryParameter("state", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("finishedBy", PARAMETER_TYPE_NVARCHAR);

        // EntityLink
        info.addQueryParameter("parentScopeId", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("rootScopeId", PARAMETER_TYPE_NVARCHAR);

    }

    protected static void addHistoricTaskInstanceParams() {
        ParameterInfo info = addParameterInfo("historicTaskInstance", "historicTask", "task");
        info.addColumn("ID_", "id", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("REV_", "revision", PARAMETER_TYPE_INTEGER);
        info.addColumn("TASK_DEF_ID_", "taskDefinitionId", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("PROC_DEF_ID_", "processDefinitionId", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("PROC_INST_ID_", "processInstanceId", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("EXECUTION_ID_", "executionId", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("SCOPE_ID_", "scopeId", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("SUB_SCOPE_ID_", "subScopeId", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("SCOPE_TYPE_", "scopeType", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("SCOPE_DEFINITION_ID_", "scopeDefinitionId", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("PROPAGATED_STAGE_INST_ID_", "propagatedStageInstanceId", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("STATE_", "state", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("NAME_", "name", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("PARENT_TASK_ID_", "parentTaskId", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("DESCRIPTION_", "description", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("OWNER_", "owner", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("ASSIGNEE_", "assignee", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("START_TIME_", "createTime", PARAMETER_TYPE_TIMESTAMP);
        info.addColumn("IN_PROGRESS_TIME_", "inProgressStartTime", PARAMETER_TYPE_TIMESTAMP);
        info.addColumn("IN_PROGRESS_STARTED_BY_", "inProgressStartedBy", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("CLAIM_TIME_", "claimTime", PARAMETER_TYPE_TIMESTAMP);
        info.addColumn("CLAIMED_BY_", "claimedBy", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("SUSPENDED_TIME_", "suspendedTime", PARAMETER_TYPE_TIMESTAMP);
        info.addColumn("SUSPENDED_BY_", "suspendedBy", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("END_TIME_", "endTime", PARAMETER_TYPE_TIMESTAMP);
        info.addColumn("COMPLETED_BY_", "completedBy", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("DURATION_", "durationInMillis", PARAMETER_TYPE_BIGINT);
        info.addColumn("DELETE_REASON_", "deleteReason", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("TASK_DEF_KEY_", "taskDefinitionKey", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("FORM_KEY_", "formKey", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("PRIORITY_", "priority", PARAMETER_TYPE_INTEGER);
        info.addColumn("IN_PROGRESS_DUE_DATE_", "inProgressStartDueDate", PARAMETER_TYPE_TIMESTAMP);
        info.addColumn("DUE_DATE_", "dueDate", PARAMETER_TYPE_TIMESTAMP);
        info.addColumn("CATEGORY_", "category", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("TENANT_ID_", "tenantId", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("LAST_UPDATED_TIME_", "lastUpdateTime", PARAMETER_TYPE_TIMESTAMP);

        addVariableColumnsWhenUsedInQueries(info);
        addIdentityLinkColumsWhenUsedInQueries(info);

        info.addColumn("ILINK_CREATE_TIME_", "createTime", PARAMETER_TYPE_TIMESTAMP);

        info.addQueryParameter("deploymentId", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("cmmnDeploymentId", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("historicTaskInstanceId", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("processCategory", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("processDefinitionKey", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("processDefinitionName", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("processInstanceBusinessKey", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("caseDefinitionKey", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("processInstanceIdWithChildren", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("caseInstanceIdWithChildren", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("taskName", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("taskParentTaskId", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("taskDescription", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("taskCategory", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("taskDeleteReason", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("taskOwner", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("taskAssignee", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("assigneeId", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("taskPriority", PARAMETER_TYPE_INTEGER);
        info.addQueryParameter("taskMinPriority", PARAMETER_TYPE_INTEGER);
        info.addQueryParameter("taskMaxPriority", PARAMETER_TYPE_INTEGER);
        info.addQueryParameter("createTimeBefore", PARAMETER_TYPE_TIMESTAMP);
        info.addQueryParameter("createTimeAfter", PARAMETER_TYPE_TIMESTAMP);
        info.addQueryParameter("inProgressStartTimeAfter", PARAMETER_TYPE_TIMESTAMP);
        info.addQueryParameter("inProgressStartTimeBefore", PARAMETER_TYPE_TIMESTAMP);
        info.addQueryParameter("claimTimeBefore", PARAMETER_TYPE_TIMESTAMP);
        info.addQueryParameter("claimTimeAfter", PARAMETER_TYPE_TIMESTAMP);
        info.addQueryParameter("suspendedTimeBefore", PARAMETER_TYPE_TIMESTAMP);
        info.addQueryParameter("suspendedTimeAfter", PARAMETER_TYPE_TIMESTAMP);
        info.addQueryParameter("completedTimeBefore", PARAMETER_TYPE_TIMESTAMP);
        info.addQueryParameter("completedTime", PARAMETER_TYPE_TIMESTAMP);
        info.addQueryParameter("completedTimeAfter", PARAMETER_TYPE_TIMESTAMP);
        info.addQueryParameter("inProgressStartDueBefore", PARAMETER_TYPE_TIMESTAMP);
        info.addQueryParameter("inProgressStartDueAfter", PARAMETER_TYPE_TIMESTAMP);
        info.addQueryParameter("dueBefore", PARAMETER_TYPE_TIMESTAMP);
        info.addQueryParameter("dueAfter", PARAMETER_TYPE_TIMESTAMP);

        info.addQueryParameter("queryVar.name", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("queryVar.scopeType", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("queryVar.type", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("queryVar.textValue", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("queryVar.textValue2", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("queryVar.longValue", PARAMETER_TYPE_BIGINT);
        info.addQueryParameter("queryVar.doubleValue", PARAMETER_TYPE_DOUBLE);

        // EntityLink
        info.addQueryParameter("parentScopeId", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("rootScopeId", PARAMETER_TYPE_NVARCHAR);

        // Identitylink
        info.addQueryParameter("candidateUser", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("group", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("involvedUser", PARAMETER_TYPE_NVARCHAR);
    }

    protected static void addJobParams() {
        ParameterInfo info = addParameterInfo("job");
        info.addColumn("ID_", "id", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("REV_", "revision", PARAMETER_TYPE_INTEGER);
        info.addColumn("CATEGORY_", "category", PARAMETER_TYPE_VARCHAR);
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

        info.addQueryParameter("deploymentId", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("jobId", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("jobExecutionScope", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("now", PARAMETER_TYPE_TIMESTAMP);
        info.addQueryParameter("handlerType", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("handlerConfiguration", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("processDefinitionKey", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("caseDefinitionKey", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("duedateHigherThan", PARAMETER_TYPE_TIMESTAMP);
        info.addQueryParameter("duedateLowerThan", PARAMETER_TYPE_TIMESTAMP);
        info.addQueryParameter("duedateHigherThanOrEqual", PARAMETER_TYPE_TIMESTAMP);
        info.addQueryParameter("duedateLowerThanOrEqual", PARAMETER_TYPE_TIMESTAMP);
    }

    protected static void addModelParams() {
        ParameterInfo info = addParameterInfo("model");
        info.addColumn("ID_", "id", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("REV_", "revision", PARAMETER_TYPE_INTEGER);
        info.addColumn("NAME_", "name", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("KEY_", "key", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("CATEGORY_", "category", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("CREATE_TIME_", "createTime", PARAMETER_TYPE_TIMESTAMP);
        info.addColumn("LAST_UPDATE_TIME_", "lastUpdateTime", PARAMETER_TYPE_TIMESTAMP);
        info.addColumn("VERSION_", "version", PARAMETER_TYPE_INTEGER);
        info.addColumn("META_INFO_", "metaInfo", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("DEPLOYMENT_ID_", "deploymentId", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("EDITOR_SOURCE_VALUE_ID_", "editorSourceValueId", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("EDITOR_SOURCE_EXTRA_VALUE_ID_", "editorSourceExtraValueId", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("TENANT_ID_", "tenantId", PARAMETER_TYPE_NVARCHAR);

        info.addQueryParameter("deploymentId", PARAMETER_TYPE_NVARCHAR);
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
        info.addQueryParameter("versionGt", PARAMETER_TYPE_INTEGER);
        info.addQueryParameter("versionGte", PARAMETER_TYPE_INTEGER);
        info.addQueryParameter("versionLt", PARAMETER_TYPE_INTEGER);
        info.addQueryParameter("versionLte", PARAMETER_TYPE_INTEGER);
        info.addQueryParameter("suspensionState.stateCode", PARAMETER_TYPE_INTEGER);

        info.addQueryParameter("parentDeploymentId", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("processDefinitionKey", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("processDefinitionVersion", PARAMETER_TYPE_INTEGER);

        info.addQueryParameter("eventSubscriptionType", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("eventSubscriptionName", PARAMETER_TYPE_NVARCHAR);

        info.addQueryParameter("authorizationUserId", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("groupId", PARAMETER_TYPE_NVARCHAR);

    }

    protected static void addProcessDefinitionInfoParams() {
        ParameterInfo info = addParameterInfo("processDefinitionInfo");
        info.addColumn("ID_", "id", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("PROC_DEF_ID_", "processDefinitionId", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("INFO_JSON_ID_", "infoJsonId", PARAMETER_TYPE_NVARCHAR);
    }

    protected static void addPropertyParams() {
        ParameterInfo info = addParameterInfo("property");
        info.addColumn("NAME_", "name", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("VALUE_", "value", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("REV_", "revision", PARAMETER_TYPE_INTEGER);
    }
    
    protected static void addResourceParams() {
        ParameterInfo info = addParameterInfo("resource");
        info.addColumn("ID_", "id", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("NAME_", "name", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("BYTES_", "bytes", PARAMETER_TYPE_BLOBTYPE);
        info.addColumn("DEPLOYMENT_ID_", "deploymentId", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("GENERATED_", "generated", PARAMETER_TYPE_BOOLEAN);

        info.addQueryParameter("resourceName", PARAMETER_TYPE_NVARCHAR);
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
        info.addQueryParameter("candidateUser", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("caseDefinitionKey", PARAMETER_TYPE_VARCHAR);
        info.addQueryParameter("caseInstanceIdWithChildren", PARAMETER_TYPE_VARCHAR);
        info.addQueryParameter("cmmnDeploymentId", PARAMETER_TYPE_VARCHAR);
        info.addQueryParameter("createTimeAfter", PARAMETER_TYPE_TIMESTAMP);
        info.addQueryParameter("createTimeBefore", PARAMETER_TYPE_TIMESTAMP);
        info.addQueryParameter("dueAfter", PARAMETER_TYPE_TIMESTAMP);
        info.addQueryParameter("dueBefore", PARAMETER_TYPE_TIMESTAMP);
        info.addQueryParameter("groupId", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("inProgressStartTimeAfter", PARAMETER_TYPE_TIMESTAMP);
        info.addQueryParameter("inProgressStartTimeBefore", PARAMETER_TYPE_TIMESTAMP);
        info.addQueryParameter("involvedUser", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("key", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("maxPriority", PARAMETER_TYPE_INTEGER);
        info.addQueryParameter("minPriority", PARAMETER_TYPE_INTEGER);
        info.addQueryParameter("parentScopeId", PARAMETER_TYPE_NVARCHAR); // From entity link
        info.addQueryParameter("processCategory", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("processDefinitionKey", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("processDefinitionName", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("processInstanceBusinessKey", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("processInstanceIdWithChildren", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("rootScopeId", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("taskId", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("userIdForCandidateAndAssignee", PARAMETER_TYPE_NVARCHAR);

        // Variables are returned together with tasks
        addVariableColumnsWhenUsedInQueries(info);

        // Identitylinks
        addIdentityLinkColumsWhenUsedInQueries(info);

    }

    private static void addIdentityLinkColumsWhenUsedInQueries(ParameterInfo info) {
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
    
    protected static void addCaseDefinitionParams() {
        ParameterInfo info = addParameterInfo("caseDefinition");
        info.addColumn("ID_", "id", PARAMETER_TYPE_VARCHAR);
        info.addColumn("REV_", "revision", PARAMETER_TYPE_INTEGER);
        info.addColumn("CATEGORY_", "category", PARAMETER_TYPE_VARCHAR);
        info.addColumn("NAME_", "name", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("KEY_", "key", PARAMETER_TYPE_VARCHAR);
        info.addColumn("VERSION_", "version", PARAMETER_TYPE_INTEGER);
        info.addColumn("DEPLOYMENT_ID_", "deploymentId", PARAMETER_TYPE_VARCHAR);
        info.addColumn("RESOURCE_NAME_", "resourceName", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("DGRM_RESOURCE_NAME_", "diagramResourceName", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("DESCRIPTION_", "description", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("HAS_START_FORM_KEY_", "hasStartFormKey", PARAMETER_TYPE_BOOLEAN);
        info.addColumn("HAS_GRAPHICAL_NOTATION_", "isGraphicalNotationDefined", PARAMETER_TYPE_BOOLEAN);
        info.addColumn("TENANT_ID_", "tenantId", PARAMETER_TYPE_VARCHAR);
        
        info.addQueryParameter("caseDefinitionKey", PARAMETER_TYPE_VARCHAR);
        info.addQueryParameter("parentDeploymentId", PARAMETER_TYPE_VARCHAR);
        info.addQueryParameter("parameter.caseDefinitionKey", PARAMETER_TYPE_VARCHAR);
        info.addQueryParameter("parameter.caseDefinitionVersion", PARAMETER_TYPE_INTEGER);
        info.addQueryParameter("parameter.tenantId", PARAMETER_TYPE_VARCHAR);
        info.addQueryParameter("versionGt", PARAMETER_TYPE_INTEGER);
        info.addQueryParameter("versionGte", PARAMETER_TYPE_INTEGER);
        info.addQueryParameter("versionLt", PARAMETER_TYPE_INTEGER);
        info.addQueryParameter("versionLte", PARAMETER_TYPE_INTEGER);
        info.addQueryParameter("authorizationUserId", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("authorizationGroup", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("parameter", PARAMETER_TYPE_VARCHAR);
    }
    
    protected static void addCaseInstanceParams() {
        ParameterInfo info = addParameterInfo("caseInstance");
        info.addColumn("ID_", "id", PARAMETER_TYPE_VARCHAR);
        info.addColumn("REV_", "rev", PARAMETER_TYPE_INTEGER);
        info.addColumn("PARENT_ID_", "parentId", PARAMETER_TYPE_VARCHAR);
        info.addColumn("CASE_DEF_ID_", "caseDefinitionId", PARAMETER_TYPE_VARCHAR);
        info.addColumn("BUSINESS_KEY_", "businessKey", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("NAME_", "name", PARAMETER_TYPE_NVARCHAR);
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
        info.addColumn("BUSINESS_STATUS_", "businessStatus", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("LOCK_TIME_", "lockTime", PARAMETER_TYPE_TIMESTAMP);
        info.addColumn("LOCK_OWNER_", "lockOwner", PARAMETER_TYPE_VARCHAR);
        info.addColumn("CaseDefinitionKey", "caseDefinitionKey", PARAMETER_TYPE_VARCHAR);
        info.addColumn("CaseDefinitionName", "caseDefinitionName", PARAMETER_TYPE_NVARCHAR);
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
        info.addQueryParameter("caseDefinitionName", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("caseDefinitionVersion", PARAMETER_TYPE_INTEGER);
        info.addQueryParameter("caseInstanceParentId", PARAMETER_TYPE_VARCHAR);
        info.addQueryParameter("parentScopeId", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("rootScopeId", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("parentCaseInstanceId", PARAMETER_TYPE_VARCHAR);
        info.addQueryParameter("startedBefore", PARAMETER_TYPE_TIMESTAMP);
        info.addQueryParameter("startedAfter", PARAMETER_TYPE_TIMESTAMP);
        info.addQueryParameter("startedBy", PARAMETER_TYPE_VARCHAR);
        info.addQueryParameter("lastReactivatedBefore", PARAMETER_TYPE_TIMESTAMP);
        info.addQueryParameter("lastReactivatedAfter", PARAMETER_TYPE_TIMESTAMP);
        info.addQueryParameter("lastReactivatedBy", PARAMETER_TYPE_VARCHAR);
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
        info.addQueryParameter("parameter", PARAMETER_TYPE_VARCHAR);
        info.addQueryParameter("variableName", PARAMETER_TYPE_NVARCHAR);
    }
    
    protected static void addCmmnDeploymentParams() {
        ParameterInfo info = addParameterInfo("cmmnDeployment");
        info.addColumn("ID_", "id", PARAMETER_TYPE_VARCHAR);
        info.addColumn("NAME_", "name", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("CATEGORY_", "category", PARAMETER_TYPE_VARCHAR);
        info.addColumn("KEY_", "key", PARAMETER_TYPE_VARCHAR);
        info.addColumn("TENANT_ID_", "tenantId", PARAMETER_TYPE_VARCHAR);
        info.addColumn("DEPLOY_TIME_", "deploymentTime", PARAMETER_TYPE_TIMESTAMP);
        info.addColumn("PARENT_DEPLOYMENT_ID_", "parentDeploymentId", PARAMETER_TYPE_VARCHAR);

        info.addQueryParameter("deploymentId", PARAMETER_TYPE_VARCHAR);
        info.addQueryParameter("nameLike", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("categoryLike", PARAMETER_TYPE_VARCHAR);
        info.addQueryParameter("categoryNotEquals", PARAMETER_TYPE_VARCHAR);
        info.addQueryParameter("keyLike", PARAMETER_TYPE_VARCHAR);
        info.addQueryParameter("tenantIdLike", PARAMETER_TYPE_VARCHAR);
        info.addQueryParameter("parentDeploymentIdLike", PARAMETER_TYPE_VARCHAR);
        info.addQueryParameter("parameter", PARAMETER_TYPE_VARCHAR);
    }
    
    protected static void addCmmnResourceParams() {
        ParameterInfo info = addParameterInfo("cmmnResource");
        info.addColumn("ID_", "id", PARAMETER_TYPE_VARCHAR);
        info.addColumn("NAME_", "name", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("RESOURCE_BYTES_", "bytes", PARAMETER_TYPE_BLOBTYPE);
        info.addColumn("DEPLOYMENT_ID_", "deploymentId", PARAMETER_TYPE_VARCHAR);
        info.addColumn("GENERATED_", "generated", PARAMETER_TYPE_BOOLEAN);
        
        info.addQueryParameter("parameter", PARAMETER_TYPE_VARCHAR);
    }
    
    protected static void addHistoricCaseInstanceParams() {
        ParameterInfo info = addParameterInfo("historicCaseInstance");
        info.addColumn("ID_", "id", PARAMETER_TYPE_VARCHAR);
        info.addColumn("REV_", "rev", PARAMETER_TYPE_INTEGER);
        info.addColumn("PARENT_ID_", "parentId", PARAMETER_TYPE_VARCHAR);
        info.addColumn("CASE_DEF_ID_", "caseDefinitionId", PARAMETER_TYPE_VARCHAR);
        info.addColumn("BUSINESS_KEY_", "businessKey", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("NAME_", "name", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("STATE_", "state", PARAMETER_TYPE_VARCHAR);
        info.addColumn("START_TIME_", "startTime", PARAMETER_TYPE_TIMESTAMP);
        info.addColumn("START_USER_ID_", "startUserId", PARAMETER_TYPE_VARCHAR);
        info.addColumn("END_TIME_", "endTime", PARAMETER_TYPE_TIMESTAMP);
        info.addColumn("LAST_REACTIVATION_TIME_", "lastReactivationTime", PARAMETER_TYPE_TIMESTAMP);
        info.addColumn("LAST_REACTIVATION_USER_ID_", "lastReactivationUserId", PARAMETER_TYPE_VARCHAR);
        info.addColumn("CALLBACK_ID_", "callbackId", PARAMETER_TYPE_VARCHAR);
        info.addColumn("CALLBACK_TYPE_", "callbackType", PARAMETER_TYPE_VARCHAR);
        info.addColumn("REFERENCE_ID_", "referenceId", PARAMETER_TYPE_VARCHAR);
        info.addColumn("REFERENCE_TYPE_", "referenceType", PARAMETER_TYPE_VARCHAR);
        info.addColumn("TENANT_ID_", "tenantId", PARAMETER_TYPE_VARCHAR);
        info.addColumn("BUSINESS_STATUS_", "businessStatus", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("END_USER_ID_", "endUserId", PARAMETER_TYPE_VARCHAR);
        info.addColumn("CaseDefinitionKey", "caseDefinitionKey", PARAMETER_TYPE_VARCHAR);
        info.addColumn("CaseDefinitionName", "caseDefinitionName", PARAMETER_TYPE_NVARCHAR);
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
        info.addQueryParameter("deploymentId", PARAMETER_TYPE_VARCHAR);
        info.addQueryParameter("caseDefinitionCategory", PARAMETER_TYPE_VARCHAR);
        info.addQueryParameter("caseDefinitionName", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("caseDefinitionVersion", PARAMETER_TYPE_INTEGER);
        info.addQueryParameter("caseInstanceParentId", PARAMETER_TYPE_VARCHAR);
        info.addQueryParameter("caseInstanceName", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("parentScopeId", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("rootScopeId", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("parentCaseInstanceId", PARAMETER_TYPE_VARCHAR);
        info.addQueryParameter("startedBefore", PARAMETER_TYPE_TIMESTAMP);
        info.addQueryParameter("startedAfter", PARAMETER_TYPE_TIMESTAMP);
        info.addQueryParameter("finishedBefore", PARAMETER_TYPE_TIMESTAMP);
        info.addQueryParameter("finishedAfter", PARAMETER_TYPE_TIMESTAMP);
        info.addQueryParameter("startedBy", PARAMETER_TYPE_VARCHAR);
        info.addQueryParameter("lastReactivatedBefore", PARAMETER_TYPE_TIMESTAMP);
        info.addQueryParameter("lastReactivatedAfter", PARAMETER_TYPE_TIMESTAMP);
        info.addQueryParameter("lastReactivatedBy", PARAMETER_TYPE_VARCHAR);
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
        info.addQueryParameter("variableName", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("finishedBy", PARAMETER_TYPE_VARCHAR);
    }
    
    protected static void addHistoricMilestoneInstanceParams() {
        ParameterInfo info = addParameterInfo("historicMilestoneInstance");
        info.addColumn("ID_", "id", PARAMETER_TYPE_VARCHAR);
        info.addColumn("NAME_", "name", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("TIME_STAMP_", "timeStamp", PARAMETER_TYPE_TIMESTAMP);
        info.addColumn("CASE_INST_ID_", "caseInstanceId", PARAMETER_TYPE_VARCHAR);
        info.addColumn("CASE_DEF_ID_", "caseDefinitionId", PARAMETER_TYPE_VARCHAR);
        info.addColumn("ELEMENT_ID_", "elementId", PARAMETER_TYPE_VARCHAR);
        info.addColumn("TENANT_ID_", "tenantId", PARAMETER_TYPE_VARCHAR);
        
        info.addQueryParameter("milestoneInstanceId", PARAMETER_TYPE_VARCHAR);
        info.addQueryParameter("reachedBefore", PARAMETER_TYPE_TIMESTAMP);
        info.addQueryParameter("reachedAfter", PARAMETER_TYPE_TIMESTAMP);
    }
    
    protected static void addHistoricPlanItemInstanceParams() {
        ParameterInfo info = addParameterInfo("historicPlanItemInstance");
        info.addColumn("ID_", "id", PARAMETER_TYPE_VARCHAR);
        info.addColumn("REV_", "revision", PARAMETER_TYPE_INTEGER);
        info.addColumn("CASE_DEF_ID_", "caseDefinitionId", PARAMETER_TYPE_VARCHAR);
        info.addColumn("DERIVED_CASE_DEF_ID_", "derivedCaseDefinitionId", PARAMETER_TYPE_VARCHAR);
        info.addColumn("CASE_INST_ID_", "caseInstanceId", PARAMETER_TYPE_VARCHAR);
        info.addColumn("STAGE_INST_ID_", "stageInstanceId", PARAMETER_TYPE_VARCHAR);
        info.addColumn("IS_STAGE_", "isStage", PARAMETER_TYPE_BOOLEAN);
        info.addColumn("ELEMENT_ID_", "elementId", PARAMETER_TYPE_VARCHAR);
        info.addColumn("ITEM_DEFINITION_ID_", "planItemDefinitionId", PARAMETER_TYPE_VARCHAR);
        info.addColumn("ITEM_DEFINITION_TYPE_", "planItemDefinitionType", PARAMETER_TYPE_VARCHAR);
        info.addColumn("NAME_", "name", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("STATE_", "state", PARAMETER_TYPE_VARCHAR);
        info.addColumn("CREATE_TIME_", "createTime", PARAMETER_TYPE_TIMESTAMP);
        info.addColumn("LAST_AVAILABLE_TIME_", "lastAvailableTime", PARAMETER_TYPE_TIMESTAMP);
        info.addColumn("LAST_UNAVAILABLE_TIME_", "lastUnavailableTime", PARAMETER_TYPE_TIMESTAMP);
        info.addColumn("LAST_ENABLED_TIME_", "lastEnabledTime", PARAMETER_TYPE_TIMESTAMP);
        info.addColumn("LAST_DISABLED_TIME_", "lastDisabledTime", PARAMETER_TYPE_TIMESTAMP);
        info.addColumn("LAST_STARTED_TIME_", "lastStartedTime", PARAMETER_TYPE_TIMESTAMP);
        info.addColumn("LAST_SUSPENDED_TIME_", "lastSuspendedTime", PARAMETER_TYPE_TIMESTAMP);
        info.addColumn("COMPLETED_TIME_", "completedTime", PARAMETER_TYPE_TIMESTAMP);
        info.addColumn("OCCURRED_TIME_", "occurredTime", PARAMETER_TYPE_TIMESTAMP);
        info.addColumn("TERMINATED_TIME_", "terminatedTime", PARAMETER_TYPE_TIMESTAMP);
        info.addColumn("EXIT_TIME_", "exitTime", PARAMETER_TYPE_TIMESTAMP);
        info.addColumn("ENDED_TIME_", "endedTime", PARAMETER_TYPE_TIMESTAMP);
        info.addColumn("LAST_UPDATED_TIME_", "lastUpdatedTime", PARAMETER_TYPE_TIMESTAMP);
        info.addColumn("START_USER_ID_", "startUserId", PARAMETER_TYPE_VARCHAR);
        info.addColumn("ASSIGNEE_", "assignee", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("COMPLETED_BY_", "completedBy", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("REFERENCE_ID_", "referenceId", PARAMETER_TYPE_VARCHAR);
        info.addColumn("REFERENCE_TYPE_", "referenceType", PARAMETER_TYPE_VARCHAR);
        info.addColumn("IS_COMPLETEABLE_", "completable", PARAMETER_TYPE_BOOLEAN);
        info.addColumn("ENTRY_CRITERION_ID_", "entryCriterionId", PARAMETER_TYPE_VARCHAR);
        info.addColumn("EXIT_CRITERION_ID_", "exitCriterionId", PARAMETER_TYPE_VARCHAR);
        info.addColumn("EXTRA_VALUE_", "extraValue", PARAMETER_TYPE_VARCHAR);
        info.addColumn("SHOW_IN_OVERVIEW_", "showInOverview", PARAMETER_TYPE_BOOLEAN);
        info.addColumn("TENANT_ID_", "tenantId", PARAMETER_TYPE_VARCHAR);
        
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
        info.addQueryParameter("planItemInstanceName", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("expirationTime", PARAMETER_TYPE_TIMESTAMP);
        info.addQueryParameter("caseDefinitionCategory", PARAMETER_TYPE_VARCHAR);
        info.addQueryParameter("caseDefinitionName", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("caseDefinitionVersion", PARAMETER_TYPE_INTEGER);
        info.addQueryParameter("caseInstanceParentId", PARAMETER_TYPE_VARCHAR);
        info.addQueryParameter("parentScopeId", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("rootScopeId", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("createdBefore", PARAMETER_TYPE_TIMESTAMP);
        info.addQueryParameter("createdAfter", PARAMETER_TYPE_TIMESTAMP);
        info.addQueryParameter("lastAvailableBefore", PARAMETER_TYPE_TIMESTAMP);
        info.addQueryParameter("lastAvailableAfter", PARAMETER_TYPE_TIMESTAMP);
        info.addQueryParameter("lastUnavailableBefore", PARAMETER_TYPE_TIMESTAMP);
        info.addQueryParameter("lastUnavailableAfter", PARAMETER_TYPE_TIMESTAMP);
        info.addQueryParameter("lastEnabledBefore", PARAMETER_TYPE_TIMESTAMP);
        info.addQueryParameter("lastEnabledAfter", PARAMETER_TYPE_TIMESTAMP);
        info.addQueryParameter("lastDisabledBefore", PARAMETER_TYPE_TIMESTAMP);
        info.addQueryParameter("lastDisabledAfter", PARAMETER_TYPE_TIMESTAMP);
        info.addQueryParameter("lastStartedBefore", PARAMETER_TYPE_TIMESTAMP);
        info.addQueryParameter("lastStartedAfter", PARAMETER_TYPE_TIMESTAMP);
        info.addQueryParameter("lastSuspendedBefore", PARAMETER_TYPE_TIMESTAMP);
        info.addQueryParameter("lastSuspendedAfter", PARAMETER_TYPE_TIMESTAMP);
        info.addQueryParameter("completedBefore", PARAMETER_TYPE_TIMESTAMP);
        info.addQueryParameter("completedAfter", PARAMETER_TYPE_TIMESTAMP);
        info.addQueryParameter("occurredBefore", PARAMETER_TYPE_TIMESTAMP);
        info.addQueryParameter("occurredAfter", PARAMETER_TYPE_TIMESTAMP);
        info.addQueryParameter("terminatedBefore", PARAMETER_TYPE_TIMESTAMP);
        info.addQueryParameter("terminatedAfter", PARAMETER_TYPE_TIMESTAMP);
        info.addQueryParameter("exitBefore", PARAMETER_TYPE_TIMESTAMP);
        info.addQueryParameter("exitAfter", PARAMETER_TYPE_TIMESTAMP);
        info.addQueryParameter("endedBefore", PARAMETER_TYPE_TIMESTAMP);
        info.addQueryParameter("endedAfter", PARAMETER_TYPE_TIMESTAMP);
        info.addQueryParameter("startedBy", PARAMETER_TYPE_VARCHAR);
        info.addQueryParameter("onlyStages", PARAMETER_TYPE_BOOLEAN);
        info.addQueryParameter("formKey", PARAMETER_TYPE_VARCHAR);
        info.addQueryParameter("involvedUser", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("involvedGroup", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("queryVariableValue.name", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("queryVariableValue.type", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("queryVariableValue.textValue", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("queryVariableValue.textValue2", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("queryVariableValue.longValue", PARAMETER_TYPE_BIGINT);
        info.addQueryParameter("queryVariableValue.doubleValue", PARAMETER_TYPE_DOUBLE);
        info.addQueryParameter("parameter.caseInstanceId", PARAMETER_TYPE_VARCHAR);
        info.addQueryParameter("parameter.planItemId", PARAMETER_TYPE_VARCHAR);
        info.addQueryParameter("parameter.stageInstanceId", PARAMETER_TYPE_VARCHAR);
        info.addQueryParameter("parameter", PARAMETER_TYPE_VARCHAR);
        info.addQueryParameter("variableName", PARAMETER_TYPE_NVARCHAR);
    }
    
    protected static void addMilestoneInstanceParams() {
        ParameterInfo info = addParameterInfo("milestoneInstance");
        info.addColumn("ID_", "id", PARAMETER_TYPE_VARCHAR);
        info.addColumn("NAME_", "name", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("TIME_STAMP_", "timeStamp", PARAMETER_TYPE_TIMESTAMP);
        info.addColumn("CASE_INST_ID_", "caseInstanceId", PARAMETER_TYPE_VARCHAR);
        info.addColumn("CASE_DEF_ID_", "caseDefinitionId", PARAMETER_TYPE_VARCHAR);
        info.addColumn("ELEMENT_ID_", "elementId", PARAMETER_TYPE_VARCHAR);
        info.addColumn("TENANT_ID_", "tenantId", PARAMETER_TYPE_VARCHAR);
        
        info.addQueryParameter("milestoneInstanceId", PARAMETER_TYPE_VARCHAR);
        info.addQueryParameter("reachedBefore", PARAMETER_TYPE_TIMESTAMP);
        info.addQueryParameter("reachedAfter", PARAMETER_TYPE_TIMESTAMP);
    }
    
    protected static void addPlanItemInstanceParams() {
        ParameterInfo info = addParameterInfo("planItemInstance");
        info.addColumn("ID_", "id", PARAMETER_TYPE_VARCHAR);
        info.addColumn("REV_", "revision", PARAMETER_TYPE_INTEGER);
        info.addColumn("CASE_DEF_ID_", "caseDefinitionId", PARAMETER_TYPE_VARCHAR);
        info.addColumn("DERIVED_CASE_DEF_ID_", "derivedCaseDefinitionId", PARAMETER_TYPE_VARCHAR);
        info.addColumn("CASE_INST_ID_", "caseInstanceId", PARAMETER_TYPE_VARCHAR);
        info.addColumn("STAGE_INST_ID_", "stageInstanceId", PARAMETER_TYPE_VARCHAR);
        info.addColumn("IS_STAGE_", "isStage", PARAMETER_TYPE_BOOLEAN);
        info.addColumn("ELEMENT_ID_", "elementId", PARAMETER_TYPE_VARCHAR);
        info.addColumn("ITEM_DEFINITION_ID_", "planItemDefinitionId", PARAMETER_TYPE_VARCHAR);
        info.addColumn("ITEM_DEFINITION_TYPE_", "planItemDefinitionType", PARAMETER_TYPE_VARCHAR);
        info.addColumn("NAME_", "name", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("STATE_", "state", PARAMETER_TYPE_VARCHAR);
        info.addColumn("CREATE_TIME_", "createTime", PARAMETER_TYPE_TIMESTAMP);
        info.addColumn("LAST_AVAILABLE_TIME_", "lastAvailableTime", PARAMETER_TYPE_TIMESTAMP);
        info.addColumn("LAST_UNAVAILABLE_TIME_", "lastUnavailableTime", PARAMETER_TYPE_TIMESTAMP);
        info.addColumn("LAST_ENABLED_TIME_", "lastEnabledTime", PARAMETER_TYPE_TIMESTAMP);
        info.addColumn("LAST_DISABLED_TIME_", "lastDisabledTime", PARAMETER_TYPE_TIMESTAMP);
        info.addColumn("LAST_STARTED_TIME_", "lastStartedTime", PARAMETER_TYPE_TIMESTAMP);
        info.addColumn("LAST_SUSPENDED_TIME_", "lastSuspendedTime", PARAMETER_TYPE_TIMESTAMP);
        info.addColumn("COMPLETED_TIME_", "completedTime", PARAMETER_TYPE_TIMESTAMP);
        info.addColumn("OCCURRED_TIME_", "occurredTime", PARAMETER_TYPE_TIMESTAMP);
        info.addColumn("TERMINATED_TIME_", "terminatedTime", PARAMETER_TYPE_TIMESTAMP);
        info.addColumn("EXIT_TIME_", "exitTime", PARAMETER_TYPE_TIMESTAMP);
        info.addColumn("ENDED_TIME_", "endedTime", PARAMETER_TYPE_TIMESTAMP);
        info.addColumn("START_USER_ID_", "startUserId", PARAMETER_TYPE_VARCHAR);
        info.addColumn("ASSIGNEE_", "assignee", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("COMPLETED_BY_", "completedBy", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("REFERENCE_ID_", "referenceId", PARAMETER_TYPE_VARCHAR);
        info.addColumn("REFERENCE_TYPE_", "referenceType", PARAMETER_TYPE_VARCHAR);
        info.addColumn("IS_COMPLETEABLE_", "completable", PARAMETER_TYPE_BOOLEAN);
        info.addColumn("ENTRY_CRITERION_ID_", "entryCriterionId", PARAMETER_TYPE_VARCHAR);
        info.addColumn("EXIT_CRITERION_ID_", "exitCriterionId", PARAMETER_TYPE_VARCHAR);
        info.addColumn("EXTRA_VALUE_", "extraValue", PARAMETER_TYPE_VARCHAR);
        info.addColumn("IS_COUNT_ENABLED_", "countEnabled", PARAMETER_TYPE_BOOLEAN);
        info.addColumn("VAR_COUNT_", "variableCount", PARAMETER_TYPE_INTEGER);
        info.addColumn("SENTRY_PART_INST_COUNT_", "sentryPartInstanceCount", PARAMETER_TYPE_INTEGER);
        info.addColumn("TENANT_ID_", "tenantId", PARAMETER_TYPE_VARCHAR);
        
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
        info.addQueryParameter("caseDefinitionName", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("caseDefinitionVersion", PARAMETER_TYPE_INTEGER);
        info.addQueryParameter("caseInstanceParentId", PARAMETER_TYPE_VARCHAR);
        info.addQueryParameter("parentScopeId", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("rootScopeId", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("createdBefore", PARAMETER_TYPE_TIMESTAMP);
        info.addQueryParameter("createdAfter", PARAMETER_TYPE_TIMESTAMP);
        info.addQueryParameter("lastAvailableBefore", PARAMETER_TYPE_TIMESTAMP);
        info.addQueryParameter("lastAvailableAfter", PARAMETER_TYPE_TIMESTAMP);
        info.addQueryParameter("lastUnavailableBefore", PARAMETER_TYPE_TIMESTAMP);
        info.addQueryParameter("lastUnavailableAfter", PARAMETER_TYPE_TIMESTAMP);
        info.addQueryParameter("lastEnabledBefore", PARAMETER_TYPE_TIMESTAMP);
        info.addQueryParameter("lastEnabledAfter", PARAMETER_TYPE_TIMESTAMP);
        info.addQueryParameter("lastDisabledBefore", PARAMETER_TYPE_TIMESTAMP);
        info.addQueryParameter("lastDisabledAfter", PARAMETER_TYPE_TIMESTAMP);
        info.addQueryParameter("lastStartedBefore", PARAMETER_TYPE_TIMESTAMP);
        info.addQueryParameter("lastStartedAfter", PARAMETER_TYPE_TIMESTAMP);
        info.addQueryParameter("lastSuspendedBefore", PARAMETER_TYPE_TIMESTAMP);
        info.addQueryParameter("lastSuspendedAfter", PARAMETER_TYPE_TIMESTAMP);
        info.addQueryParameter("completedBefore", PARAMETER_TYPE_TIMESTAMP);
        info.addQueryParameter("completedAfter", PARAMETER_TYPE_TIMESTAMP);
        info.addQueryParameter("occurredBefore", PARAMETER_TYPE_TIMESTAMP);
        info.addQueryParameter("occurredAfter", PARAMETER_TYPE_TIMESTAMP);
        info.addQueryParameter("terminatedBefore", PARAMETER_TYPE_TIMESTAMP);
        info.addQueryParameter("terminatedAfter", PARAMETER_TYPE_TIMESTAMP);
        info.addQueryParameter("exitBefore", PARAMETER_TYPE_TIMESTAMP);
        info.addQueryParameter("exitAfter", PARAMETER_TYPE_TIMESTAMP);
        info.addQueryParameter("endedBefore", PARAMETER_TYPE_TIMESTAMP);
        info.addQueryParameter("endedAfter", PARAMETER_TYPE_TIMESTAMP);
        info.addQueryParameter("startedBy", PARAMETER_TYPE_VARCHAR);
        info.addQueryParameter("onlyStages", PARAMETER_TYPE_BOOLEAN);
        info.addQueryParameter("formKey", PARAMETER_TYPE_VARCHAR);
        info.addQueryParameter("involvedUser", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("involvedGroup", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("queryVariableValue.name", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("queryVariableValue.type", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("queryVariableValue.textValue", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("queryVariableValue.textValue2", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("queryVariableValue.longValue", PARAMETER_TYPE_BIGINT);
        info.addQueryParameter("queryVariableValue.doubleValue", PARAMETER_TYPE_DOUBLE);
        info.addQueryParameter("parameter.caseInstanceId", PARAMETER_TYPE_VARCHAR);
        info.addQueryParameter("parameter.planItemId", PARAMETER_TYPE_VARCHAR);
        info.addQueryParameter("parameter.stageInstanceId", PARAMETER_TYPE_VARCHAR);
        info.addQueryParameter("parameter", PARAMETER_TYPE_VARCHAR);
    }
    
    protected static void addSentryPartInstanceParams() {
        ParameterInfo info = addParameterInfo("sentryPartInstance");
        info.addColumn("ID_", "id", PARAMETER_TYPE_VARCHAR);
        info.addColumn("REV_", "revision", PARAMETER_TYPE_INTEGER);
        info.addColumn("CASE_DEF_ID_", "caseDefinitionId", PARAMETER_TYPE_VARCHAR);
        info.addColumn("CASE_INST_ID_", "caseInstanceId", PARAMETER_TYPE_VARCHAR);
        info.addColumn("PLAN_ITEM_INST_ID_", "planItemInstanceId", PARAMETER_TYPE_VARCHAR);
        info.addColumn("ON_PART_ID_", "onPartId", PARAMETER_TYPE_VARCHAR);
        info.addColumn("IF_PART_ID_", "ifPartId", PARAMETER_TYPE_VARCHAR);
        info.addColumn("TIME_STAMP_", "timeStamp", PARAMETER_TYPE_TIMESTAMP);
        
        info.addQueryParameter("milestoneInstanceId", PARAMETER_TYPE_VARCHAR);
        info.addQueryParameter("reachedBefore", PARAMETER_TYPE_TIMESTAMP);
        info.addQueryParameter("reachedAfter", PARAMETER_TYPE_TIMESTAMP);
        info.addQueryParameter("parameter", PARAMETER_TYPE_VARCHAR);
    }
    
    protected static void addChannelDefinitionParams() {
        ParameterInfo info = addParameterInfo("channelDefinition");
        info.addColumn("ID_", "id", PARAMETER_TYPE_VARCHAR);
        info.addColumn("REV_", "revision", PARAMETER_TYPE_INTEGER);
        info.addColumn("CATEGORY_", "category", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("NAME_", "name", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("KEY_", "key", PARAMETER_TYPE_VARCHAR);
        info.addColumn("VERSION_", "version", PARAMETER_TYPE_INTEGER);
        info.addColumn("TYPE_", "type", PARAMETER_TYPE_VARCHAR);
        info.addColumn("IMPLEMENTATION_", "implementation", PARAMETER_TYPE_VARCHAR);
        info.addColumn("DEPLOYMENT_ID_", "deploymentId", PARAMETER_TYPE_VARCHAR);
        info.addColumn("CREATE_TIME_", "createTime", PARAMETER_TYPE_TIMESTAMP);
        info.addColumn("RESOURCE_NAME_", "resourceName", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("DESCRIPTION_", "description", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("TENANT_ID_", "tenantId", PARAMETER_TYPE_VARCHAR);
        
        info.addQueryParameter("channelDefinitionKey", PARAMETER_TYPE_VARCHAR);
        info.addQueryParameter("parentDeploymentId", PARAMETER_TYPE_VARCHAR);
        info.addQueryParameter("categoryLike", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("categoryNotEquals", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("nameLike", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("nameLikeIgnoreCase", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("keyLike", PARAMETER_TYPE_VARCHAR);
        info.addQueryParameter("keyLikeIgnoreCase", PARAMETER_TYPE_VARCHAR);
        info.addQueryParameter("versionGt", PARAMETER_TYPE_INTEGER);
        info.addQueryParameter("versionGte", PARAMETER_TYPE_INTEGER);
        info.addQueryParameter("versionLt", PARAMETER_TYPE_INTEGER);
        info.addQueryParameter("versionLte", PARAMETER_TYPE_INTEGER);
        info.addQueryParameter("createTimeAfter", PARAMETER_TYPE_TIMESTAMP);
        info.addQueryParameter("createTimeBefore", PARAMETER_TYPE_TIMESTAMP);
        info.addQueryParameter("resourceNameLike", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("tenantIdLike", PARAMETER_TYPE_VARCHAR);
        info.addQueryParameter("eventVersion", PARAMETER_TYPE_INTEGER);
    }
    
    protected static void addEventDefinitionParams() {
        ParameterInfo info = addParameterInfo("eventDefinition");
        info.addColumn("ID_", "id", PARAMETER_TYPE_VARCHAR);
        info.addColumn("CATEGORY_", "category", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("NAME_", "name", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("KEY_", "key", PARAMETER_TYPE_VARCHAR);
        info.addColumn("VERSION_", "version", PARAMETER_TYPE_INTEGER);
        info.addColumn("DEPLOYMENT_ID_", "deploymentId", PARAMETER_TYPE_VARCHAR);
        info.addColumn("RESOURCE_NAME_", "resourceName", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("DESCRIPTION_", "description", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("TENANT_ID_", "tenantId", PARAMETER_TYPE_VARCHAR);
        
        info.addQueryParameter("eventDefinitionKey", PARAMETER_TYPE_VARCHAR);
        info.addQueryParameter("parentDeploymentId", PARAMETER_TYPE_VARCHAR);
        info.addQueryParameter("categoryLike", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("categoryNotEquals", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("nameLike", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("nameLikeIgnoreCase", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("keyLike", PARAMETER_TYPE_VARCHAR);
        info.addQueryParameter("keyLikeIgnoreCase", PARAMETER_TYPE_VARCHAR);
        info.addQueryParameter("versionGt", PARAMETER_TYPE_INTEGER);
        info.addQueryParameter("versionGte", PARAMETER_TYPE_INTEGER);
        info.addQueryParameter("versionLt", PARAMETER_TYPE_INTEGER);
        info.addQueryParameter("versionLte", PARAMETER_TYPE_INTEGER);
        info.addQueryParameter("resourceNameLike", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("tenantIdLike", PARAMETER_TYPE_VARCHAR);
        info.addQueryParameter("eventVersion", PARAMETER_TYPE_INTEGER);
    }
    
    protected static void addEventDeploymentParams() {
        ParameterInfo info = addParameterInfo("eventDeployment");
        info.addColumn("ID_", "id", PARAMETER_TYPE_VARCHAR);
        info.addColumn("NAME_", "name", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("CATEGORY_", "category", PARAMETER_TYPE_VARCHAR);
        info.addColumn("TENANT_ID_", "tenantId", PARAMETER_TYPE_VARCHAR);
        info.addColumn("DEPLOY_TIME_", "deploymentTime", PARAMETER_TYPE_TIMESTAMP);
        info.addColumn("PARENT_DEPLOYMENT_ID_", "parentDeploymentId", PARAMETER_TYPE_VARCHAR);

        info.addQueryParameter("deploymentId", PARAMETER_TYPE_VARCHAR);
        info.addQueryParameter("nameLike", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("categoryLike", PARAMETER_TYPE_VARCHAR);
        info.addQueryParameter("categoryNotEquals", PARAMETER_TYPE_VARCHAR);
        info.addQueryParameter("tenantIdLike", PARAMETER_TYPE_VARCHAR);
        info.addQueryParameter("parentDeploymentIdLike", PARAMETER_TYPE_VARCHAR);
        info.addQueryParameter("eventDefinitionKey", PARAMETER_TYPE_VARCHAR);
        info.addQueryParameter("eventDefinitionKeyLike", PARAMETER_TYPE_VARCHAR);
        info.addQueryParameter("channelDefinitionKey", PARAMETER_TYPE_VARCHAR);
        info.addQueryParameter("channelDefinitionKeyLike", PARAMETER_TYPE_VARCHAR);
    }
    
    protected static void addEventResourceParams() {
        ParameterInfo info = addParameterInfo("eventResource");
        info.addColumn("ID_", "id", PARAMETER_TYPE_VARCHAR);
        info.addColumn("NAME_", "name", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("RESOURCE_BYTES_", "bytes", PARAMETER_TYPE_BLOBTYPE);
        info.addColumn("DEPLOYMENT_ID_", "deploymentId", PARAMETER_TYPE_VARCHAR);
        
        info.addQueryParameter("resourceName", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("parameter", PARAMETER_TYPE_VARCHAR);
    }

    protected static void addTimerJobParams() {
        ParameterInfo info = addParameterInfo("timerJob");
        info.addColumn("ID_","id", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("CATEGORY_", "category", PARAMETER_TYPE_VARCHAR);
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
        info.addQueryParameter("caseDefinitionKey", PARAMETER_TYPE_VARCHAR); // careful: cmmn engine --> varchar
        info.addQueryParameter("now", PARAMETER_TYPE_TIMESTAMP);
        info.addQueryParameter("duedateHigherThan", PARAMETER_TYPE_TIMESTAMP);
        info.addQueryParameter("duedateLowerThan", PARAMETER_TYPE_TIMESTAMP);
        info.addQueryParameter("duedateHigherThanOrEqual", PARAMETER_TYPE_TIMESTAMP);
        info.addQueryParameter("duedateLowerThanOrEqual", PARAMETER_TYPE_TIMESTAMP);
        info.addQueryParameter("jobExecutionScope", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("deploymentId", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("parameter", PARAMETER_TYPE_NVARCHAR);
    }

    protected static void addVariableInstanceParams() {
        ParameterInfo info = addParameterInfo("variableInstance", "variable");
        info.addColumn("ID_", "id", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("REV_", "revision", PARAMETER_TYPE_INTEGER);
        info.addColumn("TYPE_", "type", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("NAME_", "name", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("PROC_INST_ID_", "processInstanceId", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("EXECUTION_ID_", "executionId", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("TASK_ID_", "taskId", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("SCOPE_ID_", "scopeId", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("SUB_SCOPE_ID_", "subScopeId", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("SCOPE_TYPE_", "scopeType", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("BYTEARRAY_ID_", "byteArrayRef", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("DOUBLE_", "doubleValue", PARAMETER_TYPE_DOUBLE);
        info.addColumn("TEXT_", "textValue", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("TEXT2_", "textValue2", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("LONG_", "longValue", PARAMETER_TYPE_BIGINT);
        info.addColumn("META_INFO_", "metaInfo", PARAMETER_TYPE_NVARCHAR);

        info.addQueryParameter("typeName", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("variableName", PARAMETER_TYPE_NVARCHAR);

        info.addQueryParameter("queryVariableValue.name", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("queryVariableValue.type", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("queryVariableValue.textValue", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("queryVariableValue.textValue2", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("queryVariableValue.longValue", PARAMETER_TYPE_BIGINT);
        info.addQueryParameter("queryVariableValue.doubleValue", PARAMETER_TYPE_DOUBLE);
    }

    protected static void addBatchPartParams() {
        ParameterInfo info = addParameterInfo("batchPart");
        info.addColumn("ID_", "id", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("REV_", "revision", PARAMETER_TYPE_INTEGER);
        info.addColumn("BATCH_ID_", "batchId", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("TYPE_", "type", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("SCOPE_ID_", "scopeId", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("SUB_SCOPE_ID_", "subScopeId", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("SCOPE_TYPE_", "scopeType", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("SEARCH_KEY_", "searchKey", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("SEARCH_KEY2_", "searchKey2", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("CREATE_TIME_", "createTime", PARAMETER_TYPE_TIMESTAMP);
        info.addColumn("COMPLETE_TIME_", "completeTime", PARAMETER_TYPE_TIMESTAMP);
        info.addColumn("STATUS_", "status", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("RESULT_DOC_ID_", "resultDocRefId", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("TENANT_ID_", "tenantId", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("BATCH_TYPE_", "batchType", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("BATCH_SEARCH_KEY_", "batchSearchKey", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("BATCH_SEARCH_KEY2_", "batchSearchKey2", PARAMETER_TYPE_NVARCHAR);

        info.addQueryParameter("tenantIdLike", PARAMETER_TYPE_NVARCHAR);
    }
    
    protected static void addDeadLetterJobParams() {
        ParameterInfo info = addParameterInfo("deadLetterJob");
        info.addColumn("ID_","id", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("CATEGORY_", "category", PARAMETER_TYPE_VARCHAR);
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
        info.addQueryParameter("caseDefinitionKey", PARAMETER_TYPE_VARCHAR); // careful: cmmn engine --> varchar
        info.addQueryParameter("now", PARAMETER_TYPE_TIMESTAMP);
        info.addQueryParameter("duedateHigherThan", PARAMETER_TYPE_TIMESTAMP);
        info.addQueryParameter("duedateLowerThan", PARAMETER_TYPE_TIMESTAMP);
        info.addQueryParameter("duedateHigherThanOrEqual", PARAMETER_TYPE_TIMESTAMP);
        info.addQueryParameter("duedateLowerThanOrEqual", PARAMETER_TYPE_TIMESTAMP);
        info.addQueryParameter("jobExecutionScope", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("deploymentId", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("parameter", PARAMETER_TYPE_NVARCHAR);
    }
    
    protected static void addEntityLinkParams() {
        ParameterInfo info = addParameterInfo("entityLink");
        info.addColumn("ID_", "id", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("REV_", "revision", PARAMETER_TYPE_INTEGER);
        info.addColumn("CREATE_TIME_", "createTime", PARAMETER_TYPE_TIMESTAMP);
        info.addColumn("LINK_TYPE_", "linkType", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("SUB_SCOPE_ID_", "subScopeId", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("SCOPE_ID_", "scopeId", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("SCOPE_DEFINITION_ID_", "scopeDefinitionId", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("SCOPE_TYPE_", "scopeType", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("PARENT_ELEMENT_ID_", "parentElementId", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("REF_SCOPE_ID_", "referenceScopeId", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("REF_SCOPE_DEFINITION_ID_", "referenceScopeDefinitionId", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("REF_SCOPE_TYPE_", "referenceScopeType", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("ROOT_SCOPE_ID_", "rootScopeId", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("ROOT_SCOPE_TYPE_", "rootScopeType", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("HIERARCHY_TYPE_", "hierarchyType", PARAMETER_TYPE_NVARCHAR);
    }
    
    protected static void addEventSubscriptionParams() {
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
        info.addQueryParameter("eventName", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("eventType", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("executionId", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("activityId", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("processInstanceId", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("processDefinitionId", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("tenantId", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("scopeId", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("scopeType", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("newTenantId", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("oldTenantId", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("newProcessDefinitionId", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("oldProcessDefinitionId", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("newScopeDefinitionId", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("oldScopeDefinitionId", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("currentTime", PARAMETER_TYPE_TIMESTAMP);
        info.addQueryParameter("parameter", PARAMETER_TYPE_NVARCHAR);
    }
    
    protected static void addHistoricIdentityLinkParams() {
        ParameterInfo info = addParameterInfo("historicIdentityLink");
        info.addColumn("ID_", "id", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("TYPE_", "type", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("USER_ID_", "userId", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("GROUP_ID_", "groupId", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("TASK_ID_", "taskId", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("PROC_INST_ID_", "processInstanceId", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("SCOPE_ID_", "scopeId", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("SUB_SCOPE_ID_", "subScopeId", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("SCOPE_TYPE_", "scopeType", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("SCOPE_DEFINITION_ID_", "scopeDefinitionId", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("CREATE_TIME_", "createTime", PARAMETER_TYPE_TIMESTAMP);
        
        info.addQueryParameter("scopeId", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("scopeType", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("subScopeId", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("taskId", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("userId", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("groupId", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("type", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("identityLink.id", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("parameter", PARAMETER_TYPE_NVARCHAR);
    }
    
    protected static void addHistoryJobParams() {
        ParameterInfo info = addParameterInfo("historyJob");
        info.addColumn("ID_","id", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("REV_","revision", PARAMETER_TYPE_INTEGER);
        info.addColumn("LOCK_OWNER_", "lockOwner", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("LOCK_EXP_TIME_", "lockExpirationTime", PARAMETER_TYPE_TIMESTAMP);
        info.addColumn("RETRIES_", "retries", PARAMETER_TYPE_INTEGER);
        info.addColumn("EXCEPTION_STACK_ID_", "exceptionByteArrayRef", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("EXCEPTION_MSG_", "exceptionMessage", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("ADV_HANDLER_CFG_ID_", "advancedJobHandlerConfigurationByteArrayRef", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("HANDLER_TYPE_", "jobHandlerType", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("HANDLER_CFG_", "jobHandlerConfiguration", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("CUSTOM_VALUES_ID_", "customValuesByteArrayRef", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("CREATE_TIME_", "createTime", PARAMETER_TYPE_TIMESTAMP);
        info.addColumn("SCOPE_TYPE_", "scopeType", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("TENANT_ID_", "tenantId", PARAMETER_TYPE_NVARCHAR);
        
        info.addQueryParameter("job.id", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("handlerType", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("handlerConfiguration", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("jobExecutionScope", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("now", PARAMETER_TYPE_TIMESTAMP);
        info.addQueryParameter("parameter", PARAMETER_TYPE_NVARCHAR);
    }
    
    protected static void addHistoricTaskLogEntryParams() {
        ParameterInfo info = addParameterInfo("historicTaskLogEntry");
        info.addColumn("ID_", "logNumber", PARAMETER_TYPE_BIGINT);
        info.addColumn("TYPE_", "type", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("TASK_ID_", "taskId", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("EXECUTION_ID_", "executionId", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("PROC_INST_ID_", "processInstanceId", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("PROC_DEF_ID_", "processDefinitionId", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("SCOPE_ID_", "scopeId", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("SUB_SCOPE_ID_", "subScopeId", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("SCOPE_TYPE_", "scopeType", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("SCOPE_DEFINITION_ID_", "scopeDefinitionId", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("TIME_STAMP_", "timeStamp", PARAMETER_TYPE_TIMESTAMP);
        info.addColumn("TENANT_ID_", "tenantId", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("USER_ID_", "userId", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("DATA_", "data", PARAMETER_TYPE_NVARCHAR);
        
        info.addQueryParameter("fromDate", PARAMETER_TYPE_TIMESTAMP);
        info.addQueryParameter("toDate", PARAMETER_TYPE_TIMESTAMP);
        info.addQueryParameter("fromLogNumber", PARAMETER_TYPE_BIGINT);
        info.addQueryParameter("toLogNumber", PARAMETER_TYPE_BIGINT);
    }
    
    protected static void addHistoricVariableInstanceParams() {
        ParameterInfo info = addParameterInfo("historicVariableInstance");
        info.addColumn("ID_", "id", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("NAME_", "variableName", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("VAR_TYPE_", "variableType", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("REV_", "revision", PARAMETER_TYPE_INTEGER);
        info.addColumn("PROC_INST_ID_", "processInstanceId", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("EXECUTION_ID_", "executionId", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("TASK_ID_", "taskId", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("META_INFO_", "metaInfo", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("BYTEARRAY_ID_", "byteArrayRef", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("DOUBLE_", "doubleValue", PARAMETER_TYPE_DOUBLE);
        info.addColumn("TEXT_", "textValue", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("TEXT2_", "textValue2", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("LONG_", "longValue", PARAMETER_TYPE_BIGINT);
        info.addColumn("SCOPE_ID_", "scopeId", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("SUB_SCOPE_ID_", "subScopeId", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("SCOPE_TYPE_", "scopeType", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("CREATE_TIME_", "createTime", PARAMETER_TYPE_TIMESTAMP);
        info.addColumn("LAST_UPDATED_TIME_", "lastUpdatedTime", PARAMETER_TYPE_TIMESTAMP);
        
        info.addQueryParameter("name", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("variable.id", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("variableNameLike", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("queryVariableValue.name", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("queryVariableValue.type", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("queryVariableValue.textValue", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("queryVariableValue.textValue2", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("queryVariableValue.longValue", PARAMETER_TYPE_BIGINT);
        info.addQueryParameter("queryVariableValue.doubleValue", PARAMETER_TYPE_DOUBLE);
        info.addQueryParameter("parameter", PARAMETER_TYPE_NVARCHAR);
    }
    
    protected static void addIdentityLinkParams() {
        ParameterInfo info = addParameterInfo("identityLink");
        info.addColumn("ID_", "id", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("TYPE_", "type", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("USER_ID_", "userId", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("GROUP_ID_", "groupId", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("TASK_ID_", "taskId", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("PROC_INST_ID_", "processInstanceId", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("PROC_DEF_ID_", "processDefId", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("SCOPE_ID_", "scopeId", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("SUB_SCOPE_ID_", "subScopeId", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("SCOPE_TYPE_", "scopeType", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("SCOPE_DEFINITION_ID_", "scopeDefinitionId", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("CREATE_TIME_", "createTime", PARAMETER_TYPE_TIMESTAMP);
        
        info.addQueryParameter("processDefinitionId", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("scopeId", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("scopeType", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("subScopeId", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("scopeDefinitionId", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("taskId", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("userId", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("groupId", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("type", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("identityLink.id", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("parameter", PARAMETER_TYPE_NVARCHAR);
    }
    
    protected static void addSuspendedJobParams() {
        ParameterInfo info = addParameterInfo("suspendedJob");
        info.addColumn("ID_","id", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("CATEGORY_", "category", PARAMETER_TYPE_VARCHAR);
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
        info.addQueryParameter("caseDefinitionKey", PARAMETER_TYPE_VARCHAR); // careful: cmmn engine --> varchar
        info.addQueryParameter("now", PARAMETER_TYPE_TIMESTAMP);
        info.addQueryParameter("duedateHigherThan", PARAMETER_TYPE_TIMESTAMP);
        info.addQueryParameter("duedateLowerThan", PARAMETER_TYPE_TIMESTAMP);
        info.addQueryParameter("duedateHigherThanOrEqual", PARAMETER_TYPE_TIMESTAMP);
        info.addQueryParameter("duedateLowerThanOrEqual", PARAMETER_TYPE_TIMESTAMP);
        info.addQueryParameter("jobExecutionScope", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("deploymentId", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("parameter", PARAMETER_TYPE_NVARCHAR);
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

    protected static void addDecisionParams() {
        ParameterInfo info = addParameterInfo("decision");
        info.addColumn("ID_", "id", PARAMETER_TYPE_VARCHAR);
        info.addColumn("CATEGORY_", "category", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("NAME_", "name", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("KEY_", "key", PARAMETER_TYPE_VARCHAR);
        info.addColumn("VERSION_", "version", PARAMETER_TYPE_INTEGER);
        info.addColumn("DEPLOYMENT_ID_", "deploymentId", PARAMETER_TYPE_VARCHAR);
        info.addColumn("RESOURCE_NAME_", "resourceName", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("TENANT_ID_", "tenantId", PARAMETER_TYPE_VARCHAR);
        info.addColumn("DESCRIPTION_", "description", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("DECISION_TYPE_", "decisionType", PARAMETER_TYPE_VARCHAR);

        info.addQueryParameter("decisionId", PARAMETER_TYPE_VARCHAR);
        info.addQueryParameter("deploymentId", PARAMETER_TYPE_VARCHAR);
        info.addQueryParameter("parentDeploymentId", PARAMETER_TYPE_VARCHAR);
        info.addQueryParameter("decisionKey", PARAMETER_TYPE_VARCHAR);
        info.addQueryParameter("decisionVersion", PARAMETER_TYPE_INTEGER);
        info.addQueryParameter("versionGt", PARAMETER_TYPE_INTEGER);
        info.addQueryParameter("versionGte", PARAMETER_TYPE_INTEGER);
        info.addQueryParameter("versionLt", PARAMETER_TYPE_INTEGER);
        info.addQueryParameter("versionLte", PARAMETER_TYPE_INTEGER);
    }

    protected static void addDmnDeploymentParams() {
        ParameterInfo info = addParameterInfo("dmnDeployment", "deployment");
        info.addColumn("ID_", "id", PARAMETER_TYPE_VARCHAR);
        info.addColumn("NAME_", "name", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("CATEGORY_", "category", PARAMETER_TYPE_VARCHAR);
        info.addColumn("TENANT_ID_", "tenantId", PARAMETER_TYPE_VARCHAR);
        info.addColumn("PARENT_DEPLOYMENT_ID_", "parentDeploymentId", PARAMETER_TYPE_VARCHAR);
        info.addColumn("DEPLOY_TIME_", "deploymentTime", PARAMETER_TYPE_TIMESTAMP);

        info.addQueryParameter("deploymentId", PARAMETER_TYPE_VARCHAR);
        info.addQueryParameter("decisionKey", PARAMETER_TYPE_VARCHAR);
    }

    protected static void addHistoricDecisionExecutionParams() {
        ParameterInfo info = addParameterInfo("historicDecisionExecution");
        info.addColumn("ID_", "id", PARAMETER_TYPE_VARCHAR);
        info.addColumn("DECISION_DEFINITION_ID_", "decisionDefinitionId", PARAMETER_TYPE_VARCHAR);
        info.addColumn("DEPLOYMENT_ID_", "deploymentId", PARAMETER_TYPE_VARCHAR);
        info.addColumn("START_TIME_", "startTime", PARAMETER_TYPE_TIMESTAMP);
        info.addColumn("END_TIME_", "endTime", PARAMETER_TYPE_TIMESTAMP);
        info.addColumn("INSTANCE_ID_", "instanceId", PARAMETER_TYPE_VARCHAR);
        info.addColumn("EXECUTION_ID_", "executionId", PARAMETER_TYPE_VARCHAR);
        info.addColumn("ACTIVITY_ID_", "activityId", PARAMETER_TYPE_VARCHAR);
        info.addColumn("FAILED_", "failed", PARAMETER_TYPE_BOOLEAN);
        info.addColumn("TENANT_ID_", "tenantId", PARAMETER_TYPE_VARCHAR);
        info.addColumn("EXECUTION_JSON_", "executionJson", PARAMETER_TYPE_VARCHAR);
        info.addColumn("SCOPE_TYPE_", "scopeType", PARAMETER_TYPE_VARCHAR);
        info.addColumn("DEC_DEF_KEY_", "decisionKey", PARAMETER_TYPE_VARCHAR);
        info.addColumn("DEC_DEF_NAME_", "decisionName", PARAMETER_TYPE_VARCHAR);
        info.addColumn("DEC_DEF_VERSION_", "decisionVersion", PARAMETER_TYPE_VARCHAR);

        info.addQueryParameter("deploymentId", PARAMETER_TYPE_VARCHAR);
        info.addQueryParameter("decisionKey", PARAMETER_TYPE_VARCHAR);
        info.addQueryParameter("processInstanceIdWithChildren", PARAMETER_TYPE_VARCHAR);
        info.addQueryParameter("caseInstanceIdWithChildren", PARAMETER_TYPE_VARCHAR);
    }

    protected static void addDmnResourceParams() {
        ParameterInfo info = addParameterInfo("dmnResource", "resource");
        info.addColumn("ID_", "id", PARAMETER_TYPE_VARCHAR);
        info.addColumn("NAME_", "name", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("RESOURCE_BYTES_", "bytes", PARAMETER_TYPE_BLOBTYPE);
        info.addColumn("DEPLOYMENT_ID_", "deploymentId", PARAMETER_TYPE_VARCHAR);

        info.addQueryParameter("resourceName", PARAMETER_TYPE_NVARCHAR);
    }

    protected static void addAppDefinitionParams() {
        ParameterInfo info = addParameterInfo("appDefinition");
        info.addColumn("ID_", "id", PARAMETER_TYPE_VARCHAR);
        info.addColumn("REV_", "revision", PARAMETER_TYPE_INTEGER);
        info.addColumn("CATEGORY_", "category", PARAMETER_TYPE_VARCHAR);
        info.addColumn("NAME_", "name", PARAMETER_TYPE_VARCHAR);
        info.addColumn("KEY_", "key", PARAMETER_TYPE_VARCHAR);
        info.addColumn("VERSION_", "version", PARAMETER_TYPE_INTEGER);
        info.addColumn("DESCRIPTION_", "description", PARAMETER_TYPE_VARCHAR);
        info.addColumn("RESOURCE_NAME_", "resourceName", PARAMETER_TYPE_VARCHAR);
        info.addColumn("DEPLOYMENT_ID_", "deploymentId", PARAMETER_TYPE_VARCHAR);
        info.addColumn("TENANT_ID_", "tenantId", PARAMETER_TYPE_VARCHAR);

        info.addQueryParameter("appDefinitionId", PARAMETER_TYPE_VARCHAR);
        info.addQueryParameter("appDefinitionKey", PARAMETER_TYPE_VARCHAR);
        info.addQueryParameter("appDefinitionVersion", PARAMETER_TYPE_INTEGER);
        info.addQueryParameter("versionGt", PARAMETER_TYPE_INTEGER);
        info.addQueryParameter("versionGte", PARAMETER_TYPE_INTEGER);
        info.addQueryParameter("versionLt", PARAMETER_TYPE_INTEGER);
        info.addQueryParameter("versionLte", PARAMETER_TYPE_INTEGER);
    }

    protected static void addAppDeploymentParams() {
        ParameterInfo info = addParameterInfo("appDeployment");
        info.addColumn("ID_", "id", PARAMETER_TYPE_VARCHAR);
        info.addColumn("NAME_", "name", PARAMETER_TYPE_VARCHAR);
        info.addColumn("CATEGORY_", "category", PARAMETER_TYPE_VARCHAR);
        info.addColumn("KEY_", "key", PARAMETER_TYPE_VARCHAR);
        info.addColumn("TENANT_ID_", "tenantId", PARAMETER_TYPE_VARCHAR);
        info.addColumn("DEPLOY_TIME_", "deploymentTime", PARAMETER_TYPE_TIMESTAMP);

        info.addQueryParameter("deploymentId", PARAMETER_TYPE_VARCHAR);
    }

    protected static void addAppResourceParams() {
        ParameterInfo info = addParameterInfo("appResource", "resource");
        info.addColumn("ID_", "id", PARAMETER_TYPE_VARCHAR);
        info.addColumn("NAME_", "name", PARAMETER_TYPE_VARCHAR);
        info.addColumn("RESOURCE_BYTES_", "bytes", PARAMETER_TYPE_BLOBTYPE);
        info.addColumn("DEPLOYMENT_ID_", "deploymentId", PARAMETER_TYPE_VARCHAR);

        info.addQueryParameter("resourceName", PARAMETER_TYPE_VARCHAR);
    }

    protected static void addGroupParams() {
        ParameterInfo info = addParameterInfo("group");
        info.addColumn("ID_", "id", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("REV_", "revision", PARAMETER_TYPE_INTEGER);
        info.addColumn("NAME_", "name", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("TYPE_", "type", PARAMETER_TYPE_NVARCHAR);

        info.addQueryParameter("userId", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("groupType", PARAMETER_TYPE_NVARCHAR);
    }

    protected static void addIdmByteArrayParams() {
        ParameterInfo info = addParameterInfo("idmByteArray", "byteArray");
        info.addColumn("ID_", "id", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("REV_", "revision", PARAMETER_TYPE_INTEGER);
        info.addColumn("NAME_", "name", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("BYTES_", "bytes", PARAMETER_TYPE_BLOBTYPE);
    }

    protected static void addPrivilegeMappingParams() {
        ParameterInfo info = addParameterInfo("privilege");
        info.addColumn("ID_", "id", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("NAME_", "name", PARAMETER_TYPE_NVARCHAR);

        info.addQueryParameter("groupId", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("userId", PARAMETER_TYPE_NVARCHAR);
    }

    protected static void addPrivilegeParams() {
        ParameterInfo info = addParameterInfo("privilegeMapping");
        info.addColumn("ID_", "id", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("PRIV_ID_", "privilegeId", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("USER_ID_", "userId", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("GROUP_ID_", "groupId", PARAMETER_TYPE_NVARCHAR);

        info.addQueryParameter("groupId", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("userId", PARAMETER_TYPE_NVARCHAR);
    }

    protected static void addIdentityInfoParams() {
        ParameterInfo info = addParameterInfo("identityInfo");
        info.addColumn("ID_", "id", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("REV_", "revision", PARAMETER_TYPE_INTEGER);
        info.addColumn("USER_ID_", "userId", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("TYPE_", "type", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("KEY_", "key", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("VALUE_", "value", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("PASSWORD_", "passwordBytes", PARAMETER_TYPE_BLOBTYPE);
        info.addColumn("PARENT_ID_", "parentId", PARAMETER_TYPE_NVARCHAR);
    }

    protected static void addIdmPropertyParams() {
        ParameterInfo info = addParameterInfo("idmProperty", "property");
        info.addColumn("NAME_", "name", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("VALUE_", "value", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("REV_", "revision", PARAMETER_TYPE_INTEGER);
    }

    protected static void addMembershipParams() {
        ParameterInfo info = addParameterInfo("membership");
        info.addColumn("USER_ID_", "userId", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("GROUP_ID_", "groupId", PARAMETER_TYPE_NVARCHAR);
    }

    protected static void addTokenParams() {
        ParameterInfo info = addParameterInfo("token");
        info.addColumn("ID_", "id", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("REV_", "revision", PARAMETER_TYPE_INTEGER);
        info.addColumn("TOKEN_VALUE_", "tokenValue", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("TOKEN_DATE_", "tokenDate", PARAMETER_TYPE_TIMESTAMP);
        info.addColumn("IP_ADDRESS_", "ipAddress", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("USER_AGENT_", "userAgent", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("USER_ID_", "userId", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("TOKEN_DATA_", "tokenData", PARAMETER_TYPE_NVARCHAR);

        info.addQueryParameter("tokenDateBefore", PARAMETER_TYPE_TIMESTAMP);
        info.addQueryParameter("tokenDateAfter", PARAMETER_TYPE_TIMESTAMP);
    }

    protected static void addUserParams() {
        ParameterInfo info = addParameterInfo("user");
        info.addColumn("ID_", "id", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("REV_", "revision", PARAMETER_TYPE_INTEGER);
        info.addColumn("FIRST_", "firstName", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("LAST_", "lastName", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("DISPLAY_NAME_", "displayName", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("EMAIL_", "email", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("TENANT_ID_", "tenantId", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("PWD_", "password", PARAMETER_TYPE_NVARCHAR);
        info.addColumn("PICTURE_ID_", "pictureByteArrayRef", PARAMETER_TYPE_NVARCHAR);

        info.addQueryParameter("idIgnoreCase", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("fullName", PARAMETER_TYPE_NVARCHAR);
        info.addQueryParameter("groupId", PARAMETER_TYPE_NVARCHAR);
    }

    public static String getParameterType(String entity, String parameterName) {
        return getParameterInfo(entity).getParameterType(parameterName);
    }

    public static String getColumnType(String entity, String columnName) {
        return getParameterInfo(entity).getColumnType(entity, columnName);
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

    protected static ParameterInfo addParameterInfo(String alias, String...additionalAliases) {
        ParameterInfo parameterInfo = new ParameterInfo(alias, additionalAliases);
        ALL_PARAMS.put(alias.toLowerCase(Locale.ROOT), parameterInfo);
        return parameterInfo;
    }
    
    protected static class ParameterInfo {

        protected String alias;
        protected String[] additionalAliases;
        protected Map<String, String> columnToParameterMap = new HashMap<>();
        protected Map<String, String> parameterToJdbcTypeMap = new HashMap<>();

        public ParameterInfo(String alias) {
            this.alias = alias;

            // Common for all
            addColumn("REV_", "revision", PARAMETER_TYPE_INTEGER);
            addQueryParameter("revisionNext", PARAMETER_TYPE_INTEGER);
        }

        public ParameterInfo(String alias, String ... additionalAliases) {
            this(alias);
            this.additionalAliases = additionalAliases;
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
            internalAddParameter(parameter, jdbcType);

            if (jdbcType.equals(PARAMETER_TYPE_NVARCHAR) || jdbcType.equals(PARAMETER_TYPE_VARCHAR)) {
                internalAddParameter(parameter + "Like", jdbcType);
                internalAddParameter(parameter + "LikeIgnoreCase", jdbcType);
                internalAddParameter(parameter + "NotEquals", jdbcType);
            }
        }

        protected void internalAddParameter(String parameter, String jdbcType) {
            parameterToJdbcTypeMap.put(parameter, jdbcType);
            parameterToJdbcTypeMap.put(alias + "." + parameter, jdbcType);
            parameterToJdbcTypeMap.put("parameter." + parameter, jdbcType);
            parameterToJdbcTypeMap.put("orQueryObject." + parameter, jdbcType);

            if (additionalAliases != null) {
                for (String additionalAlias : additionalAliases) {
                    parameterToJdbcTypeMap.put(additionalAlias + "." + parameter, jdbcType);
                }
            }
        }

        public String getColumnType(String entity, String columnName) {
            String parameterName = columnToParameterMap.get(columnName);
            if (StringUtils.isEmpty(parameterName)) {
                parameterName = columnToParameterMap.get(columnName.toUpperCase(Locale.ROOT)); // e.g. needed for postgres
            }
            if (StringUtils.isEmpty(parameterName)) {
                parameterName = columnToParameterMap.get(columnName.toLowerCase(Locale.ROOT));
            }
            if (StringUtils.isEmpty(parameterName)) {
                throw new RuntimeException("No parameter type set for column '" + columnName + "' for entity '" + entity + "'");
            }
            return getParameterType(parameterName);
        }

        public String getParameterType(String parameterName) {
            return parameterToJdbcTypeMap.get(parameterName);
        }

    }

}
