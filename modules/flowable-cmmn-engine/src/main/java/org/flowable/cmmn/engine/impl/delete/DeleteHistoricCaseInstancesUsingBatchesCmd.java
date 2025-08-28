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
package org.flowable.cmmn.engine.impl.delete;

import static org.flowable.job.service.impl.history.async.util.AsyncHistoryJsonUtil.putIfNotNull;
import static org.flowable.job.service.impl.history.async.util.AsyncHistoryJsonUtil.putIfNotNullOrEmpty;
import static org.flowable.job.service.impl.history.async.util.AsyncHistoryJsonUtil.putIfTrue;

import org.flowable.batch.api.Batch;
import org.flowable.batch.api.BatchPart;
import org.flowable.batch.api.BatchService;
import org.flowable.cmmn.api.CmmnManagementService;
import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.impl.IdentityLinkQueryObject;
import org.flowable.cmmn.engine.impl.history.HistoricCaseInstanceQueryImpl;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.common.engine.impl.calendar.BusinessCalendar;
import org.flowable.common.engine.impl.calendar.CycleBusinessCalendar;
import org.flowable.common.engine.impl.identity.Authentication;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
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
public class DeleteHistoricCaseInstancesUsingBatchesCmd implements Command<String> {

    protected HistoricCaseInstanceQueryImpl historicCaseInstanceQuery;
    protected int batchSize;
    protected boolean sequential;
    protected String batchName;

    public DeleteHistoricCaseInstancesUsingBatchesCmd(HistoricCaseInstanceQueryImpl query, int batchSize, String batchName, boolean sequential) {
        this.historicCaseInstanceQuery = query;
        this.batchSize = batchSize;
        this.batchName = batchName;
        this.sequential = sequential;
    }

    @Override
    public String execute(CommandContext commandContext) {
        if (historicCaseInstanceQuery == null) {
            throw new FlowableIllegalArgumentException("query is null");
        }

        if (batchSize <= 0) {
            throw new FlowableIllegalArgumentException("batchSize has to be larger than 0");
        }

        CmmnEngineConfiguration engineConfiguration = CommandContextUtil.getCmmnEngineConfiguration(commandContext);
        BatchService batchService = engineConfiguration.getBatchServiceConfiguration()
                .getBatchService();

        long numberOfCaseInstancesToDelete = historicCaseInstanceQuery.count();

        ObjectNode batchConfiguration = engineConfiguration.getObjectMapper().createObjectNode();
        batchConfiguration.put("numberOfInstances", numberOfCaseInstancesToDelete);
        batchConfiguration.put("batchSize", batchSize);
        if (sequential) {
            batchConfiguration.put("sequential", true);
        }

        ObjectNode queryNode = batchConfiguration.putObject("query");

        populateQueryNode(queryNode, historicCaseInstanceQuery);
        for (HistoricCaseInstanceQueryImpl orQueryObject : historicCaseInstanceQuery.getOrQueryObjects()) {
            ObjectNode orQueryNode = queryNode.withArray("orQueryObjects")
                    .addObject();
            populateQueryNode(orQueryNode, orQueryObject);
        }

        String tenantId = historicCaseInstanceQuery.getTenantId();
        if (historicCaseInstanceQuery.isWithoutTenantId()) {
            tenantId = CmmnEngineConfiguration.NO_TENANT_ID;
        }

        Batch batch = batchService.createBatchBuilder()
                .batchType(Batch.HISTORIC_CASE_DELETE_TYPE)
                .tenantId(tenantId)
                .searchKey(batchName)
                .searchKey2(Authentication.getAuthenticatedUserId())
                .status(DeleteCaseInstanceBatchConstants.STATUS_IN_PROGRESS)
                .batchDocumentJson(batchConfiguration.toString())
                .create();

        if (numberOfCaseInstancesToDelete > 0) {
            // We convert to double, and tet the ceiling of the division to get the parts
            long numberOfBatchParts = (long) Math.ceil(((double) numberOfCaseInstancesToDelete) / batchSize);

            if (sequential) {
                createBatchPartsForSequentialExecution(engineConfiguration, batch, numberOfBatchParts);
            } else {
                createBatchPartsForParallelExecution(engineConfiguration, batch, numberOfBatchParts);
            }
        } else {
            batchService.completeBatch(batch.getId(), DeleteCaseInstanceBatchConstants.STATUS_COMPLETED);
        }

        return batch.getId();
    }

    protected void createBatchPartsForParallelExecution(CmmnEngineConfiguration engineConfiguration, Batch batch, long numberOfBatchParts) {
        JobService jobService = engineConfiguration.getJobServiceConfiguration()
                .getJobService();

        CmmnManagementService managementService = engineConfiguration.getCmmnManagementService();

        for (int i = 0; i < numberOfBatchParts; i++) {

            BatchPart batchPart = managementService.createBatchPartBuilder(batch)
                    .type(DeleteCaseInstanceBatchConstants.BATCH_PART_COMPUTE_IDS_TYPE)
                    .searchKey(Integer.toString(i))
                    .status(DeleteCaseInstanceBatchConstants.STATUS_WAITING)
                    .create();

            JobEntity job = jobService.createJob();
            job.setJobHandlerType(ComputeDeleteHistoricCaseInstanceIdsJobHandler.TYPE);
            job.setJobHandlerConfiguration(batchPart.getId());
            job.setScopeType(ScopeTypes.CMMN);
            jobService.createAsyncJob(job, false);
            jobService.scheduleAsyncJob(job);
        }

        TimerJobService timerJobService = engineConfiguration.getJobServiceConfiguration()
                .getTimerJobService();
        TimerJobEntity timerJob = timerJobService.createTimerJob();
        timerJob.setJobType(Job.JOB_TYPE_TIMER);
        timerJob.setRevision(1);
        timerJob.setJobHandlerType(ComputeDeleteHistoricCaseInstanceStatusJobHandler.TYPE);
        timerJob.setJobHandlerConfiguration(batch.getId());
        timerJob.setScopeType(ScopeTypes.CMMN);

        BusinessCalendar businessCalendar = engineConfiguration.getBusinessCalendarManager().getBusinessCalendar(CycleBusinessCalendar.NAME);
        timerJob.setDuedate(businessCalendar.resolveDuedate(engineConfiguration.getBatchStatusTimeCycleConfig()));
        timerJob.setRepeat(engineConfiguration.getBatchStatusTimeCycleConfig());

        timerJobService.scheduleTimerJob(timerJob);
    }

    protected void createBatchPartsForSequentialExecution(CmmnEngineConfiguration engineConfiguration, Batch batch, long numberOfBatchParts) {
        CmmnManagementService managementService = engineConfiguration.getCmmnManagementService();

        BatchPart firstBatchPart = managementService.createBatchPartBuilder(batch)
                .type(DeleteCaseInstanceBatchConstants.BATCH_PART_DELETE_CASE_INSTANCES_TYPE)
                .searchKey(Integer.toString(0))
                .status(DeleteCaseInstanceBatchConstants.STATUS_WAITING)
                .create();

        JobService jobService = engineConfiguration.getJobServiceConfiguration().getJobService();

        JobEntity job = jobService.createJob();
        job.setJobHandlerType(DeleteHistoricCaseInstancesSequentialJobHandler.TYPE);
        job.setJobHandlerConfiguration(firstBatchPart.getId());
        job.setScopeType(ScopeTypes.CMMN);
        jobService.createAsyncJob(job, false);
        jobService.scheduleAsyncJob(job);
    }

    protected void populateQueryNode(ObjectNode queryNode, HistoricCaseInstanceQueryImpl query) {

        putIfNotNull(queryNode, "caseDefinitionId", query.getCaseDefinitionId());
        putIfNotNull(queryNode, "caseDefinitionKey", query.getCaseDefinitionKey());
        putIfNotNull(queryNode, "caseDefinitionKeyLike", query.getCaseDefinitionKeyLike());
        putIfNotNull(queryNode, "caseDefinitionKeyLikeIgnoreCase", query.getCaseDefinitionKeyLikeIgnoreCase());
        putIfNotNullOrEmpty(queryNode, "caseDefinitionKeys", query.getCaseDefinitionKeys());
        putIfNotNullOrEmpty(queryNode, "excludeCaseDefinitionKeys", query.getExcludeCaseDefinitionKeys());
        putIfNotNullOrEmpty(queryNode, "caseDefinitionIds", query.getCaseDefinitionIds());
        putIfNotNull(queryNode, "caseDefinitionName", query.getCaseDefinitionName());
        putIfNotNull(queryNode, "caseDefinitionNameLike", query.getCaseDefinitionNameLike());
        putIfNotNull(queryNode, "caseDefinitionNameLikeIgnoreCase", query.getCaseDefinitionNameLikeIgnoreCase());
        putIfNotNull(queryNode, "caseDefinitionCategory", query.getCaseDefinitionCategory());
        putIfNotNull(queryNode, "caseDefinitionCategoryLike", query.getCaseDefinitionCategoryLike());
        putIfNotNull(queryNode, "caseDefinitionCategoryLikeIgnoreCase", query.getCaseDefinitionCategoryLikeIgnoreCase());
        putIfNotNull(queryNode, "caseDefinitionVersion", query.getCaseDefinitionVersion());
        putIfNotNull(queryNode, "caseInstanceId", query.getCaseInstanceId());
        putIfNotNullOrEmpty(queryNode, "caseInstanceIds", query.getCaseInstanceIds());
        putIfNotNull(queryNode, "caseInstanceName", query.getCaseInstanceName());
        putIfNotNull(queryNode, "caseInstanceNameLike", query.getCaseInstanceNameLike());
        putIfNotNull(queryNode, "caseInstanceNameLikeIgnoreCase", query.getCaseInstanceNameLikeIgnoreCase());
        putIfNotNull(queryNode, "caseInstanceRootScopeId", query.getRootScopeId());
        putIfNotNull(queryNode, "caseInstanceParentScopeId", query.getParentScopeId());
        putIfNotNull(queryNode, "businessKey", query.getBusinessKey());
        putIfNotNull(queryNode, "businessKeyLike", query.getBusinessKeyLike());
        putIfNotNull(queryNode, "businessKeyLikeIgnoreCase", query.getBusinessKeyLikeIgnoreCase());
        putIfNotNull(queryNode, "businessStatus", query.getBusinessStatus());
        putIfNotNull(queryNode, "businessStatusLike", query.getBusinessStatusLike());
        putIfNotNull(queryNode, "businessStatusLikeIgnoreCase", query.getBusinessStatusLikeIgnoreCase());
        putIfNotNull(queryNode, "state", query.getState());
        putIfNotNull(queryNode, "caseInstanceParentId", query.getCaseInstanceParentId());
        putIfTrue(queryNode, "withoutCaseInstanceParentId", query.isWithoutCaseInstanceParentId());
        putIfNotNull(queryNode, "deploymentId", query.getDeploymentId());
        putIfNotNullOrEmpty(queryNode, "deploymentIds", query.getDeploymentIds());
        putIfTrue(queryNode, "finished", query.isFinished());
        putIfTrue(queryNode, "unfinished", query.isUnfinished());
        putIfNotNull(queryNode, "startedBefore", query.getStartedBefore());
        putIfNotNull(queryNode, "startedAfter", query.getStartedAfter());
        putIfNotNull(queryNode, "finishedBefore", query.getFinishedBefore());
        putIfNotNull(queryNode, "finishedAfter", query.getFinishedAfter());
        putIfNotNull(queryNode, "startedBy", query.getStartedBy());
        putIfNotNull(queryNode, "finishedBy", query.getFinishedBy());
        putIfNotNull(queryNode, "lastReactivatedBefore", query.getLastReactivatedBefore());
        putIfNotNull(queryNode, "lastReactivatedAfter", query.getLastReactivatedAfter());
        putIfNotNull(queryNode, "lastReactivatedBy", query.getLastReactivatedBy());
        putIfNotNull(queryNode, "callbackId", query.getCallbackId());
        putIfNotNullOrEmpty(queryNode, "callbackIds", query.getCallbackIds());
        putIfNotNull(queryNode, "callbackType", query.getCallbackType());
        putIfTrue(queryNode, "withoutCallbackId", query.isWithoutCallbackId());
        putIfNotNull(queryNode, "referenceId", query.getReferenceId());
        putIfNotNull(queryNode, "referenceType", query.getReferenceType());
        putIfNotNull(queryNode, "tenantId", query.getTenantId());
        putIfNotNull(queryNode, "tenantIdLike", query.getTenantIdLike());
        putIfNotNull(queryNode, "tenantIdLikeIgnoreCase", query.getTenantIdLikeIgnoreCase());
        putIfTrue(queryNode, "withoutTenantId", query.isWithoutTenantId());
        putIfNotNull(queryNode, "activePlanItemDefinitionId", query.getActivePlanItemDefinitionId());
        putIfNotNullOrEmpty(queryNode, "activePlanItemDefinitionIds", query.getActivePlanItemDefinitionIds());
        putIfNotNull(queryNode, "involvedUser", query.getInvolvedUser());
        putIdentityLinkQuery(queryNode, "involvedUserIdentityLink", query.getInvolvedUserIdentityLink());
        putIfNotNullOrEmpty(queryNode, "involvedGroups", query.getInvolvedGroups());
        putIdentityLinkQuery(queryNode, "involvedGroupIdentityLink", query.getInvolvedGroupIdentityLink());
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
