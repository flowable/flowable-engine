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
package org.flowable.engine.impl.delete;

import static org.flowable.job.service.impl.history.async.util.AsyncHistoryJsonUtil.putIfNotNull;
import static org.flowable.job.service.impl.history.async.util.AsyncHistoryJsonUtil.putIfNotNullOrEmpty;
import static org.flowable.job.service.impl.history.async.util.AsyncHistoryJsonUtil.putIfTrue;

import org.flowable.batch.api.Batch;
import org.flowable.batch.api.BatchPart;
import org.flowable.batch.api.BatchService;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.impl.calendar.BusinessCalendar;
import org.flowable.common.engine.impl.calendar.CycleBusinessCalendar;
import org.flowable.common.engine.impl.identity.Authentication;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.ManagementService;
import org.flowable.engine.impl.HistoricProcessInstanceQueryImpl;
import org.flowable.engine.impl.IdentityLinkQueryObject;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.job.api.Job;
import org.flowable.job.service.JobService;
import org.flowable.job.service.TimerJobService;
import org.flowable.job.service.impl.persistence.entity.JobEntity;
import org.flowable.job.service.impl.persistence.entity.TimerJobEntity;
import org.flowable.variable.service.impl.QueryVariableValue;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * @author Filip Hrisafov
 */
public class DeleteHistoricProcessInstancesUsingBatchesCmd implements Command<String> {

    protected HistoricProcessInstanceQueryImpl historicProcessInstanceQuery;
    protected int batchSize;
    protected boolean sequential;
    protected String batchName;

    public DeleteHistoricProcessInstancesUsingBatchesCmd(HistoricProcessInstanceQueryImpl query, int batchSize, String batchName, boolean sequential) {
        this.historicProcessInstanceQuery = query;
        this.batchSize = batchSize;
        this.batchName = batchName;
        this.sequential = sequential;
    }

    @Override
    public String execute(CommandContext commandContext) {
        if (historicProcessInstanceQuery == null) {
            throw new FlowableIllegalArgumentException("query is null");
        }

        if (batchSize <= 0) {
            throw new FlowableIllegalArgumentException("batchSize has to be larger than 0");
        }

        ProcessEngineConfigurationImpl engineConfiguration = CommandContextUtil.getProcessEngineConfiguration(commandContext);
        BatchService batchService = engineConfiguration.getBatchServiceConfiguration()
                .getBatchService();

        long numberOfProcessInstancesToDelete = historicProcessInstanceQuery.count();

        ObjectNode batchConfiguration = engineConfiguration.getObjectMapper().createObjectNode();
        batchConfiguration.put("numberOfInstances", numberOfProcessInstancesToDelete);
        batchConfiguration.put("batchSize", batchSize);
        if (sequential) {
            batchConfiguration.put("sequential", true);
        }

        ObjectNode queryNode = batchConfiguration.putObject("query");

        populateQueryNode(queryNode, historicProcessInstanceQuery);
        for (HistoricProcessInstanceQueryImpl orQueryObject : historicProcessInstanceQuery.getOrQueryObjects()) {
            ObjectNode orQueryNode = queryNode.withArray("orQueryObjects")
                    .addObject();
            populateQueryNode(orQueryNode, orQueryObject);
        }

        String tenantId = historicProcessInstanceQuery.getTenantId();
        if (historicProcessInstanceQuery.isWithoutTenantId()) {
            tenantId = ProcessEngineConfigurationImpl.NO_TENANT_ID;
        }

        Batch batch = batchService.createBatchBuilder()
                .batchType(Batch.HISTORIC_PROCESS_DELETE_TYPE)
                .tenantId(tenantId)
                .searchKey(batchName)
                .searchKey2(Authentication.getAuthenticatedUserId())
                .status(DeleteProcessInstanceBatchConstants.STATUS_IN_PROGRESS)
                .batchDocumentJson(batchConfiguration.toString())
                .create();

        if (numberOfProcessInstancesToDelete > 0) {
            // We convert to double, and tet the ceiling of the division to get the parts
            long numberOfBatchParts = (long) Math.ceil(((double) numberOfProcessInstancesToDelete) / batchSize);

            if (sequential) {
                createBatchPartsForSequentialExecution(engineConfiguration, batch, numberOfBatchParts);
            } else {
                createBatchPartsForParallelExecution(engineConfiguration, batch, numberOfBatchParts);
            }
        } else {
            batchService.completeBatch(batch.getId(), DeleteProcessInstanceBatchConstants.STATUS_COMPLETED);
        }

        return batch.getId();
    }

    protected void createBatchPartsForParallelExecution(ProcessEngineConfigurationImpl engineConfiguration, Batch batch, long numberOfBatchParts) {
        JobService jobService = engineConfiguration.getJobServiceConfiguration()
                .getJobService();

        ManagementService managementService = engineConfiguration.getManagementService();

        for (int i = 0; i < numberOfBatchParts; i++) {

            BatchPart batchPart = managementService.createBatchPartBuilder(batch)
                    .type(DeleteProcessInstanceBatchConstants.BATCH_PART_COMPUTE_IDS_TYPE)
                    .searchKey(Integer.toString(i))
                    .status(DeleteProcessInstanceBatchConstants.STATUS_WAITING)
                    .create();

            JobEntity job = jobService.createJob();
            job.setJobHandlerType(ComputeDeleteHistoricProcessInstanceIdsJobHandler.TYPE);
            job.setJobHandlerConfiguration(batchPart.getId());
            jobService.createAsyncJob(job, false);
            jobService.scheduleAsyncJob(job);
        }

        TimerJobService timerJobService = engineConfiguration.getJobServiceConfiguration()
                .getTimerJobService();
        TimerJobEntity timerJob = timerJobService.createTimerJob();
        timerJob.setJobType(Job.JOB_TYPE_TIMER);
        timerJob.setRevision(1);
        timerJob.setJobHandlerType(ComputeDeleteHistoricProcessInstanceStatusJobHandler.TYPE);
        timerJob.setJobHandlerConfiguration(batch.getId());

        BusinessCalendar businessCalendar = engineConfiguration.getBusinessCalendarManager().getBusinessCalendar(CycleBusinessCalendar.NAME);
        timerJob.setDuedate(businessCalendar.resolveDuedate(engineConfiguration.getBatchStatusTimeCycleConfig()));
        timerJob.setRepeat(engineConfiguration.getBatchStatusTimeCycleConfig());

        timerJobService.scheduleTimerJob(timerJob);
    }

    protected void createBatchPartsForSequentialExecution(ProcessEngineConfigurationImpl engineConfiguration, Batch batch, long numberOfBatchParts) {
        ManagementService managementService = engineConfiguration.getManagementService();

        BatchPart firstBatchPart = managementService.createBatchPartBuilder(batch)
                .type(DeleteProcessInstanceBatchConstants.BATCH_PART_DELETE_PROCESS_INSTANCES_TYPE)
                .searchKey(Integer.toString(0))
                .status(DeleteProcessInstanceBatchConstants.STATUS_WAITING)
                .create();

        JobService jobService = engineConfiguration.getJobServiceConfiguration().getJobService();

        JobEntity job = jobService.createJob();
        job.setJobHandlerType(DeleteHistoricProcessInstancesSequentialJobHandler.TYPE);
        job.setJobHandlerConfiguration(firstBatchPart.getId());
        jobService.createAsyncJob(job, false);
        jobService.scheduleAsyncJob(job);
    }

    protected void populateQueryNode(ObjectNode queryNode, HistoricProcessInstanceQueryImpl query) {

        putIfNotNull(queryNode, "processInstanceId", query.getProcessInstanceId());
        putIfNotNull(queryNode, "processDefinitionId", query.getProcessDefinitionId());
        putIfNotNull(queryNode, "businessKey", query.getBusinessKey());
        putIfNotNull(queryNode, "businessKeyLike", query.getBusinessKeyLike());
        putIfNotNull(queryNode, "businessKeyLikeIgnoreCase", query.getBusinessKeyLikeIgnoreCase());
        putIfNotNull(queryNode, "businessStatus", query.getBusinessStatus());
        putIfNotNull(queryNode, "businessStatusLike", query.getBusinessStatusLike());
        putIfNotNull(queryNode, "businessStatusLikeIgnoreCase", query.getBusinessStatusLikeIgnoreCase());
        putIfNotNull(queryNode, "deploymentId", query.getDeploymentId());
        putIfNotNullOrEmpty(queryNode, "deploymentIds", query.getDeploymentIds());
        putIfTrue(queryNode, "finished", query.isFinished());
        putIfTrue(queryNode, "unfinished", query.isUnfinished());
        putIfTrue(queryNode, "deleted", query.isDeleted());
        putIfTrue(queryNode, "notDeleted", query.isNotDeleted());
        putIfNotNull(queryNode, "startedBy", query.getStartedBy());
        putIfNotNull(queryNode, "finishedBy", query.getFinishedBy());
        putIfNotNull(queryNode, "state", query.getState());
        putIfNotNull(queryNode, "startedBy", query.getStartedBy());
        putIfNotNull(queryNode, "superProcessInstanceId", query.getSuperProcessInstanceId());
        putIfTrue(queryNode, "excludeSubprocesses", query.isExcludeSubprocesses());
        putIfNotNullOrEmpty(queryNode, "processDefinitionKeyIn", query.getProcessDefinitionKeyIn());
        putIfNotNullOrEmpty(queryNode, "excludeProcessDefinitionKeys", query.getExcludeProcessDefinitionKeys());
        putIfNotNullOrEmpty(queryNode, "processKeyNotIn", query.getProcessKeyNotIn());
        putIfNotNull(queryNode, "startedBefore", query.getStartedBefore());
        putIfNotNull(queryNode, "startedAfter", query.getStartedAfter());
        putIfNotNull(queryNode, "finishedBefore", query.getFinishedBefore());
        putIfNotNull(queryNode, "finishedAfter", query.getFinishedAfter());
        putIfNotNull(queryNode, "processDefinitionKey", query.getProcessDefinitionKey());
        putIfNotNull(queryNode, "processDefinitionKeyLike", query.getProcessDefinitionKeyLike());
        putIfNotNull(queryNode, "processDefinitionKeyLikeIgnoreCase", query.getProcessDefinitionKeyLikeIgnoreCase());
        putIfNotNull(queryNode, "processDefinitionCategory", query.getProcessDefinitionCategory());
        putIfNotNull(queryNode, "processDefinitionCategoryLike", query.getProcessDefinitionCategoryLike());
        putIfNotNull(queryNode, "processDefinitionCategoryLikeIgnoreCase", query.getProcessDefinitionCategoryLikeIgnoreCase());
        putIfNotNull(queryNode, "processDefinitionName", query.getProcessDefinitionName());
        putIfNotNull(queryNode, "processDefinitionNameLike", query.getProcessDefinitionNameLike());
        putIfNotNull(queryNode, "processDefinitionNameLikeIgnoreCase", query.getProcessDefinitionNameLikeIgnoreCase());
        putIfNotNull(queryNode, "processDefinitionVersion", query.getProcessDefinitionVersion());
        putIfNotNullOrEmpty(queryNode, "processInstanceIds", query.getProcessInstanceIds());
        putIfNotNull(queryNode, "activeActivityId", query.getActiveActivityId());
        putIfNotNullOrEmpty(queryNode, "activeActivityIds", query.getActiveActivityIds());
        putIfNotNull(queryNode, "involvedUser", query.getInvolvedUser());
        putIdentityLinkQuery(queryNode, "involvedUserIdentityLink", query.getInvolvedUserIdentityLink());
        putIfNotNullOrEmpty(queryNode, "involvedGroups", query.getInvolvedGroups());
        putIdentityLinkQuery(queryNode, "involvedGroupIdentityLink", query.getInvolvedGroupIdentityLink());
        putIfTrue(queryNode, "withJobException", query.isWithJobException());
        putIfNotNull(queryNode, "tenantId", query.getTenantId());
        putIfNotNull(queryNode, "tenantIdLike", query.getTenantIdLike());
        putIfNotNull(queryNode, "tenantIdLikeIgnoreCase", query.getTenantIdLikeIgnoreCase());
        putIfTrue(queryNode, "withoutTenantId", query.isWithoutTenantId());
        putIfNotNull(queryNode, "name", query.getName());
        putIfNotNull(queryNode, "nameLike", query.getNameLike());
        putIfNotNull(queryNode, "nameLikeIgnoreCase", query.getNameLikeIgnoreCase());
        putIfNotNull(queryNode, "callbackId", query.getCallbackId());
        putIfNotNullOrEmpty(queryNode, "callbackIds", query.getCallbackIds());
        putIfNotNull(queryNode, "callbackType", query.getCallbackType());
        putIfTrue(queryNode, "withoutCallbackId", query.isWithoutCallbackId());
        putIfNotNull(queryNode, "referenceId", query.getReferenceId());
        putIfNotNull(queryNode, "referenceType", query.getReferenceType());
        putIfNotNull(queryNode, "processInstanceRootScopeId", query.getRootScopeId());
        putIfNotNull(queryNode, "processInstanceParentScopeId", query.getParentScopeId());
        for (QueryVariableValue queryVariableValue : query.getQueryVariableValues()) {
            ArrayNode queryVariablesArrayNode = queryNode.withArray("queryVariableValues");
            populateQueryVariableValue(queryVariablesArrayNode, queryVariableValue);
        }

    }

    protected void putIdentityLinkQuery(ObjectNode queryNode, String key, IdentityLinkQueryObject linkQueryObject) {
        if (linkQueryObject != null) {
            ObjectNode linkNode = queryNode.putObject(key);
            putIfNotNull(linkNode, "userId", linkQueryObject.getUserId());
            putIfNotNull(linkNode, "groupId", linkQueryObject.getGroupId());
            putIfNotNull(linkNode, "type", linkQueryObject.getType());
        }
    }

    protected void populateQueryVariableValue(ArrayNode queryVariablesArrayNode, QueryVariableValue variableValue) {
        if (variableValue != null) {
            ObjectNode variableNode = queryVariablesArrayNode.addObject();
            putIfNotNull(variableNode, "name", variableValue.getName());
            variableNode.put("operator", variableValue.getOperator());
            putIfNotNull(variableNode, "textValue", variableValue.getTextValue());
            putIfNotNull(variableNode, "textValue2", variableValue.getTextValue2());
            putIfNotNull(variableNode, "longValue", variableValue.getLongValue());
            putIfNotNull(variableNode, "doubleValue", variableValue.getDoubleValue());
            putIfNotNull(variableNode, "type", variableValue.getType());
            putIfTrue(variableNode, "local", variableValue.isLocal());
        }
    }
}
