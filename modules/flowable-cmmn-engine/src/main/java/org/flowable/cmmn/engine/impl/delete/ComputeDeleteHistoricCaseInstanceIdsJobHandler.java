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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.flowable.batch.api.Batch;
import org.flowable.batch.api.BatchPart;
import org.flowable.batch.api.BatchService;
import org.flowable.cmmn.api.history.HistoricCaseInstance;
import org.flowable.cmmn.api.history.HistoricCaseInstanceQuery;
import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.interceptor.EngineConfigurationConstants;
import org.flowable.common.engine.impl.util.ExceptionUtil;
import org.flowable.job.service.JobHandler;
import org.flowable.job.service.JobService;
import org.flowable.job.service.impl.history.async.AsyncHistoryDateUtil;
import org.flowable.job.service.impl.persistence.entity.JobEntity;
import org.flowable.variable.api.delegate.VariableScope;
import org.flowable.variable.api.types.ValueFields;
import org.flowable.variable.api.types.VariableType;
import org.flowable.variable.service.impl.QueryOperator;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * @author Filip Hrisafov
 */
public class ComputeDeleteHistoricCaseInstanceIdsJobHandler implements JobHandler {

    public static final String TYPE = "compute-delete-historic-case-ids";

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public void execute(JobEntity job, String configuration, VariableScope variableScope, CommandContext commandContext) {
        CmmnEngineConfiguration engineConfiguration = CommandContextUtil.getCmmnEngineConfiguration(commandContext);
        BatchService batchService = engineConfiguration.getBatchServiceConfiguration().getBatchService();
        BatchPart batchPart = batchService.getBatchPart(configuration);
        if (batchPart == null) {
            throw new FlowableIllegalArgumentException("There is no batch part with the id " + configuration);
        }

        Batch batch = batchService.getBatch(batchPart.getBatchId());
        JsonNode batchConfiguration = getBatchConfiguration(batch, engineConfiguration);
        boolean sequentialExecution = batchConfiguration.path("sequential").booleanValue();

        JsonNode queryNode = batchConfiguration.path("query");
        if (queryNode.isMissingNode()) {
            failBatchPart(engineConfiguration, batchService, batchPart, batch,
                    prepareFailedResultAsJsonString("Batch configuration has no query definition", engineConfiguration), sequentialExecution);
            return;
        }

        JsonNode batchSizeNode = batchConfiguration.path("batchSize");
        if (batchSizeNode.isMissingNode()) {
            failBatchPart(engineConfiguration, batchService, batchPart, batch,
                    prepareFailedResultAsJsonString("Batch configuration has no batch size", engineConfiguration), sequentialExecution);
            return;
        }

        HistoricCaseInstanceQuery query;

        try {
            query = createQuery(queryNode, engineConfiguration);
        } catch (FlowableException exception) {
            failBatchPart(engineConfiguration, batchService, batchPart, batch,
                    prepareFailedResultAsJsonString("Failed to create query", exception, engineConfiguration), sequentialExecution);
            return;
        }

        int batchSize = batchSizeNode.intValue();
        int batchPartNumber = Integer.parseInt(batchPart.getSearchKey());
        // The first result is the batch part number multiplied by the batch size
        // e.g. if this is the 5th batch part (batch part number 4) and the batch size is 100 the first result should start from 400
        int firstResult = batchPartNumber * batchSize;

        List<HistoricCaseInstance> caseInstances = query.listPage(firstResult, batchSize);

        ObjectNode resultNode = engineConfiguration.getObjectMapper().createObjectNode();
        ArrayNode idsToDelete = resultNode.putArray("caseInstanceIdsToDelete");
        for (HistoricCaseInstance caseInstance : caseInstances) {
            idsToDelete.add(caseInstance.getId());
        }

        BatchPart batchPartForDelete = engineConfiguration.getCmmnManagementService()
                .createBatchPartBuilder(batch)
                .type(DeleteCaseInstanceBatchConstants.BATCH_PART_DELETE_CASE_INSTANCES_TYPE)
                .searchKey(batchPart.getId())
                .searchKey2(batchPart.getSearchKey())
                .status(DeleteCaseInstanceBatchConstants.STATUS_WAITING)
                .create();

        resultNode.put("deleteBatchPart", batchPartForDelete.getId());
        if (sequentialExecution) {
            resultNode.put("sequential", true);
            // If the computation was sequential we need to schedule the next job
            List<BatchPart> nextComputeParts = engineConfiguration.getCmmnManagementService()
                    .createBatchPartQuery()
                    .batchId(batch.getId())
                    .status(DeleteCaseInstanceBatchConstants.STATUS_WAITING)
                    .type(DeleteCaseInstanceBatchConstants.BATCH_PART_COMPUTE_IDS_TYPE)
                    .listPage(0, 2);

            // We are only going to start deletion if the batch is not failed
            boolean startDeletion = !DeleteCaseInstanceBatchConstants.STATUS_FAILED.equals(batch.getStatus());

            for (BatchPart nextComputePart : nextComputeParts) {
                if (!nextComputePart.getId().equals(batchPart.getId())) {
                    startDeletion = false;
                    JobService jobService = engineConfiguration.getJobServiceConfiguration().getJobService();

                    JobEntity nextComputeJob = jobService.createJob();
                    nextComputeJob.setJobHandlerType(ComputeDeleteHistoricCaseInstanceIdsJobHandler.TYPE);
                    nextComputeJob.setJobHandlerConfiguration(nextComputePart.getId());
                    nextComputeJob.setScopeType(ScopeTypes.CMMN);
                    jobService.createAsyncJob(nextComputeJob, false);
                    jobService.scheduleAsyncJob(nextComputeJob);
                    break;
                }
            }

            if (startDeletion) {
                JobService jobService = engineConfiguration.getJobServiceConfiguration().getJobService();
                JobEntity nextDeleteJob = jobService.createJob();
                nextDeleteJob.setJobHandlerType(DeleteHistoricCaseInstanceIdsJobHandler.TYPE);
                nextDeleteJob.setJobHandlerConfiguration(batchPartForDelete.getId());
                nextDeleteJob.setScopeType(ScopeTypes.CMMN);
                jobService.createAsyncJob(nextDeleteJob, false);
                jobService.scheduleAsyncJob(nextDeleteJob);
            }
        }

        batchService.completeBatchPart(batchPart.getId(), DeleteCaseInstanceBatchConstants.STATUS_COMPLETED, resultNode.toString());
    }

    private void failBatchPart(CmmnEngineConfiguration engineConfiguration, BatchService batchService, BatchPart batchPart, Batch batch,
            String resultJson, boolean sequentialExecution) {
        batchService.completeBatchPart(batchPart.getId(), DeleteCaseInstanceBatchConstants.STATUS_FAILED, resultJson);
        if (sequentialExecution) {
            completeBatch(batch, DeleteCaseInstanceBatchConstants.STATUS_FAILED, engineConfiguration);
        }
    }

    protected void completeBatch(Batch batch, String status, CmmnEngineConfiguration engineConfiguration) {
        engineConfiguration.getBatchServiceConfiguration()
                .getBatchService()
                .completeBatch(batch.getId(), status);
    }

    protected HistoricCaseInstanceQuery createQuery(JsonNode queryNode, CmmnEngineConfiguration engineConfiguration) {
        HistoricCaseInstanceQuery query = engineConfiguration.getCmmnHistoryService()
                .createHistoricCaseInstanceQuery();

        populateQuery(queryNode, query, engineConfiguration);
        return query;
    }

    protected void populateQuery(JsonNode queryNode, HistoricCaseInstanceQuery query, CmmnEngineConfiguration engineConfiguration) {
        Iterator<Map.Entry<String, JsonNode>> fieldIterator = queryNode.fields();
        while (fieldIterator.hasNext()) {
            Map.Entry<String, JsonNode> field = fieldIterator.next();
            String property = field.getKey();
            JsonNode value = field.getValue();
            switch (property) {
                case "caseDefinitionId":
                    query.caseDefinitionId(value.textValue());
                    break;
                case "caseDefinitionKey":
                    query.caseDefinitionKey(value.textValue());
                    break;
                case "caseDefinitionKeys":
                    query.caseDefinitionKeys(asStringSet(value));
                    break;
                case "caseDefinitionIds":
                    query.caseDefinitionIds(asStringSet(value));
                    break;
                case "caseDefinitionName":
                    query.caseDefinitionName(value.textValue());
                    break;
                case "caseDefinitionCategory":
                    query.caseDefinitionCategory(value.textValue());
                    break;
                case "caseDefinitionVersion":
                    query.caseDefinitionVersion(value.intValue());
                    break;
                case "caseInstanceId":
                    query.caseInstanceId(value.textValue());
                    break;
                case "caseInstanceIds":
                    query.caseInstanceIds(asStringSet(value));
                    break;
                case "caseInstanceNameLikeIgnoreCase":
                    query.caseInstanceNameLikeIgnoreCase(value.textValue());
                    break;
                case "businessKey":
                    query.caseInstanceBusinessKey(value.textValue());
                    break;
                case "businessStatus":
                    query.caseInstanceBusinessStatus(value.textValue());
                    break;
                case "caseInstanceParentId":
                    query.caseInstanceParentId(value.textValue());
                    break;
                case "withoutCaseInstanceParentId":
                    if (value.booleanValue()) {
                        query.withoutCaseInstanceParent();
                    }
                    break;
                case "deploymentId":
                    query.deploymentId(value.textValue());
                    break;
                case "deploymentIds":
                    query.deploymentIds(asStringList(value));
                    break;
                case "finished":
                    if (value.booleanValue()) {
                        query.finished();
                    }
                    break;
                case "unfinished":
                    if (value.booleanValue()) {
                        query.unfinished();
                    }
                    break;
                case "startedBefore":
                    query.startedBefore(AsyncHistoryDateUtil.parseDate(value.textValue()));
                    break;
                case "startedAfter":
                    query.startedAfter(AsyncHistoryDateUtil.parseDate(value.textValue()));
                    break;
                case "finishedBefore":
                    query.finishedBefore(AsyncHistoryDateUtil.parseDate(value.textValue()));
                    break;
                case "finishedAfter":
                    query.finishedAfter(AsyncHistoryDateUtil.parseDate(value.textValue()));
                    break;
                case "startedBy":
                    query.startedBy(value.textValue());
                    break;
                case "lastReactivatedBefore":
                    query.lastReactivatedBefore(AsyncHistoryDateUtil.parseDate(value.textValue()));
                    break;
                case "lastReactivatedAfter":
                    query.lastReactivatedAfter(AsyncHistoryDateUtil.parseDate(value.textValue()));
                case "lastReactivatedBy":
                    query.lastReactivatedBy(value.textValue());
                    break;
                case "callbackId":
                    query.caseInstanceCallbackId(value.textValue());
                    break;
                case "callbackType":
                    query.caseInstanceCallbackType(value.textValue());
                    break;
                case "withoutCallbackId":
                    if (value.booleanValue()) {
                        query.withoutCaseInstanceCallbackId();
                    }
                    break;
                case "referenceId":
                    query.caseInstanceReferenceId(value.textValue());
                    break;
                case "referenceType":
                    query.caseInstanceReferenceType(value.textValue());
                    break;
                case "tenantId":
                    query.caseInstanceTenantId(value.textValue());
                    break;
                case "withoutTenantId":
                    if (value.booleanValue()) {
                        query.caseInstanceWithoutTenantId();
                    }
                    break;
                case "activePlanItemDefinitionId":
                    query.activePlanItemDefinitionId(value.textValue());
                    break;
                case "activePlanItemDefinitionIds":
                    query.activePlanItemDefinitionIds(asStringSet(value));
                    break;
                case "involvedUser":
                    query.involvedUser(value.textValue());
                    break;
                case "involvedUserIdentityLink":
                    query.involvedUser(value.path("userId").textValue(), value.path("type").textValue());
                    break;
                case "involvedGroups":
                    query.involvedGroups(asStringSet(value));
                    break;
                case "involvedGroupIdentityLink":
                    query.involvedUser(value.path("groupId").textValue(), value.path("type").textValue());
                    break;
                case "queryVariableValues":
                    populateQueryVariableValues(value, query, engineConfiguration);
                    break;
                case "orQueryObjects":
                    populateOrQueryObjects(value, query, engineConfiguration);
                    break;
                default:
                    throw new FlowableIllegalArgumentException("Query property " + property + " is not supported");
            }
        }
    }

    protected void populateOrQueryObjects(JsonNode orQueryObjectsNode, HistoricCaseInstanceQuery query, CmmnEngineConfiguration engineConfiguration) {
        if (orQueryObjectsNode.isArray()) {
            for (JsonNode orQueryObjectNode : orQueryObjectsNode) {
                HistoricCaseInstanceQuery orQuery = query.or();
                populateQuery(orQueryObjectNode, orQuery, engineConfiguration);
                query.endOr();
            }
        }
    }

    protected void populateQueryVariableValues(JsonNode variableValuesNode, HistoricCaseInstanceQuery query,
            CmmnEngineConfiguration engineConfiguration) {
        if (variableValuesNode.isArray()) {
            for (JsonNode variableValue : variableValuesNode) {
                String operatorString = variableValue.path("operator").asText(null);
                if (operatorString == null) {
                    throw new FlowableIllegalArgumentException("The variable value does not contain an operator value");
                }

                QueryOperator operator = QueryOperator.valueOf(operatorString);
                String variableName = variableValue.path("name").textValue();
                switch (operator) {
                    case EQUALS:
                        if (variableName != null) {
                            query.variableValueEquals(variableName, extractVariableValue(variableValue, engineConfiguration));
                        } else {
                            query.variableValueEquals(extractVariableValue(variableValue, engineConfiguration));
                        }
                        break;
                    case NOT_EQUALS:
                        query.variableValueNotEquals(variableName, extractVariableValue(variableValue, engineConfiguration));
                        break;
                    case GREATER_THAN:
                        query.variableValueGreaterThan(variableName, extractVariableValue(variableValue, engineConfiguration));
                        break;
                    case GREATER_THAN_OR_EQUAL:
                        query.variableValueGreaterThanOrEqual(variableName, extractVariableValue(variableValue, engineConfiguration));
                        break;
                    case LESS_THAN:
                        query.variableValueLessThan(variableName, extractVariableValue(variableValue, engineConfiguration));
                        break;
                    case LESS_THAN_OR_EQUAL:
                        query.variableValueLessThanOrEqual(variableName, extractVariableValue(variableValue, engineConfiguration));
                        break;
                    case LIKE:
                        query.variableValueLike(variableName, (String) extractVariableValue(variableValue, engineConfiguration));
                        break;
                    case LIKE_IGNORE_CASE:
                        query.variableValueLikeIgnoreCase(variableName, (String) extractVariableValue(variableValue, engineConfiguration));
                        break;
                    case EQUALS_IGNORE_CASE:
                        query.variableValueEqualsIgnoreCase(variableName, (String) extractVariableValue(variableValue, engineConfiguration));
                        break;
                    case EXISTS:
                        query.variableExists(variableName);
                        break;
                    case NOT_EXISTS:
                        query.variableNotExists(variableName);
                        break;
                    case NOT_EQUALS_IGNORE_CASE:
                        //Not exposed on the public API
                        //query.variableValueNotEqualsIgnoreCase(variableName, (String) extractVariableValue(variableValue, engineConfiguration));
                        //break;
                    default:
                        throw new FlowableIllegalArgumentException("Operator " + operator + " is not supported for the variable value");
                }
            }

        }

    }

    protected Object extractVariableValue(JsonNode variableValueNode, CmmnEngineConfiguration engineConfiguration) {
        String type = variableValueNode.path("type").asText(null);
        if (type == null) {
            throw new FlowableIllegalArgumentException("The variable value does not have a type");
        }

        VariableType variableType = engineConfiguration.getVariableTypes()
                .getVariableType(type);
        return variableType.getValue(new VariableValueJsonNodeValueFields(variableValueNode));
    }

    protected List<String> asStringList(JsonNode node) {
        if (node != null && node.isArray() && node.size() > 0) {
            List<String> values = new ArrayList<>(node.size());
            for (JsonNode element : node) {
                values.add(element.textValue());
            }
            return values;
        }

        return null;
    }

    protected Set<String> asStringSet(JsonNode node) {
        if (node != null && node.isArray() && node.size() > 0) {
            List<String> values = new ArrayList<>(node.size());
            for (JsonNode element : node) {
                values.add(element.textValue());
            }
            return new HashSet<>(values);
        }

        return null;
    }

    protected JsonNode getBatchConfiguration(Batch batch, CmmnEngineConfiguration engineConfiguration) {
        try {
            return engineConfiguration.getObjectMapper()
                    .readTree(batch.getBatchDocumentJson(EngineConfigurationConstants.KEY_CMMN_ENGINE_CONFIG));
        } catch (JsonProcessingException e) {
            ExceptionUtil.sneakyThrow(e);
            return null;
        }
    }

    protected String prepareFailedResultAsJsonString(String errorMessage, CmmnEngineConfiguration engineConfiguration) {
        return prepareFailedResultAsJsonString(errorMessage, null, engineConfiguration);
    }

    protected String prepareFailedResultAsJsonString(String errorMessage, FlowableException exception, CmmnEngineConfiguration engineConfiguration) {
        ObjectNode resultNode = engineConfiguration.getObjectMapper()
                .createObjectNode();
        resultNode.put("errorMessage", errorMessage);
        if (exception != null) {
            resultNode.put("errorStacktrace", ExceptionUtils.getStackTrace(exception));
        }
        return resultNode.toString();
    }

    protected static class VariableValueJsonNodeValueFields implements ValueFields {

        protected final JsonNode node;

        public VariableValueJsonNodeValueFields(JsonNode node) {
            this.node = node;
        }

        @Override
        public String getName() {
            return node.path("name").textValue();
        }

        @Override
        public String getProcessInstanceId() {
            throw new UnsupportedOperationException("Not supported to get process instance id");
        }

        @Override
        public String getExecutionId() {
            throw new UnsupportedOperationException("Not supported to get execution id");
        }

        @Override
        public String getScopeId() {
            throw new UnsupportedOperationException("Not supported to get scope id");
        }

        @Override
        public String getSubScopeId() {
            throw new UnsupportedOperationException("Not supported to get sub scope id");
        }

        @Override
        public String getScopeType() {
            throw new UnsupportedOperationException("Not supported to scope type");
        }

        @Override
        public String getTaskId() {
            throw new UnsupportedOperationException("Not supported to get task id");
        }

        @Override
        public String getTextValue() {
            return node.path("textValue").textValue();
        }

        @Override
        public void setTextValue(String textValue) {
            throw new UnsupportedOperationException("Not supported to set text value");
        }

        @Override
        public String getTextValue2() {
            return node.path("textValues").textValue();
        }

        @Override
        public void setTextValue2(String textValue2) {
            throw new UnsupportedOperationException("Not supported to set text value2");
        }

        @Override
        public Long getLongValue() {
            JsonNode longNode = node.path("longValue");
            if (longNode.isNumber()) {
                return longNode.longValue();
            }
            return null;
        }

        @Override
        public void setLongValue(Long longValue) {
            throw new UnsupportedOperationException("Not supported to set long value");
        }

        @Override
        public Double getDoubleValue() {
            JsonNode doubleNode = node.path("doubleValue");
            if (doubleNode.isNumber()) {
                return doubleNode.doubleValue();
            }
            return null;
        }

        @Override
        public void setDoubleValue(Double doubleValue) {
            throw new UnsupportedOperationException("Not supported to set double value");
        }

        @Override
        public byte[] getBytes() {
            throw new UnsupportedOperationException("Not supported to get bytes");
        }

        @Override
        public void setBytes(byte[] bytes) {
            throw new UnsupportedOperationException("Not supported to set bytes");
        }

        @Override
        public Object getCachedValue() {
            throw new UnsupportedOperationException("Not supported to set get cached value");
        }

        @Override
        public void setCachedValue(Object cachedValue) {
            throw new UnsupportedOperationException("Not supported to set cached value");
        }
    }

}
