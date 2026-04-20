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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.flowable.batch.api.Batch;
import org.flowable.batch.api.BatchPart;
import org.flowable.batch.api.BatchService;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.impl.interceptor.EngineConfigurationConstants;
import org.flowable.engine.history.HistoricProcessInstanceQuery;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.job.service.impl.history.async.AsyncHistoryDateUtil;
import org.flowable.variable.api.types.ValueFields;
import org.flowable.variable.api.types.VariableType;
import org.flowable.variable.service.impl.QueryOperator;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * @author Filip Hrisafov
 */
public class BatchDeleteProcessConfig {

    protected final Batch batch;
    protected final BatchPart batchPart;
    protected final String error;
    protected final boolean sequentialExecution;
    protected int batchSize;
    protected HistoricProcessInstanceQuery query;

    protected BatchDeleteProcessConfig(Batch batch, BatchPart batchPart, String error, boolean sequentialExecution) {
        this.batch = batch;
        this.batchPart = batchPart;
        this.error = error;
        this.sequentialExecution = sequentialExecution;
    }

    public Batch getBatch() {
        return batch;
    }

    public BatchPart getBatchPart() {
        return batchPart;
    }

    public String getError() {
        return error;
    }

    public boolean hasError() {
        return error != null;
    }

    public boolean isSequentialExecution() {
        return sequentialExecution;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public HistoricProcessInstanceQuery getQuery() {
        return query;
    }

    public static BatchDeleteProcessConfig create(String batchPartId, ProcessEngineConfigurationImpl engineConfiguration) {
        BatchService batchService = engineConfiguration.getBatchServiceConfiguration().getBatchService();
        BatchPart batchPart = batchService.getBatchPart(batchPartId);
        if (batchPart == null) {
            throw new FlowableIllegalArgumentException("There is no batch part with the id " + batchPartId);
        }

        Batch batch = batchService.getBatch(batchPart.getBatchId());
        JsonNode batchConfiguration = getBatchConfiguration(batch, engineConfiguration);
        boolean sequentialExecution = batchConfiguration.path("sequential").booleanValue(false);

        JsonNode queryNode = batchConfiguration.path("query");
        if (queryNode.isMissingNode()) {
            return new BatchDeleteProcessConfig(batch, batchPart,
                    prepareFailedResultAsJsonString("Batch configuration has no query definition", engineConfiguration), sequentialExecution);
        }

        JsonNode batchSizeNode = batchConfiguration.path("batchSize");
        if (batchSizeNode.isMissingNode()) {
            return new BatchDeleteProcessConfig(batch, batchPart, prepareFailedResultAsJsonString("Batch configuration has no batch size", engineConfiguration),
                    sequentialExecution);
        }

        HistoricProcessInstanceQuery query;

        try {
            query = createQuery(queryNode, engineConfiguration);
        } catch (FlowableException exception) {
            return new BatchDeleteProcessConfig(batch, batchPart, prepareFailedResultAsJsonString("Failed to create query", exception, engineConfiguration),
                    sequentialExecution);
        }

        BatchDeleteProcessConfig config = new BatchDeleteProcessConfig(batch, batchPart, null, sequentialExecution);
        config.batchSize = batchSizeNode.intValue();
        config.query = query;

        return config;
    }

    protected static HistoricProcessInstanceQuery createQuery(JsonNode queryNode, ProcessEngineConfigurationImpl engineConfiguration) {
        HistoricProcessInstanceQuery query = engineConfiguration.getHistoryService()
                .createHistoricProcessInstanceQuery();

        populateQuery(queryNode, query, engineConfiguration);

        if (queryNode.hasNonNull("finishedBefore") || queryNode.hasNonNull("finishedAfter") || queryNode.path("finished").booleanValue(false)) {
            // When the query has finishedBefore, finishedAfter or finished then we need to order by the process instance end time
            // This is done in order to improve the performance when getting pages with large offsets.
            // When the properties are not set we cannot order on the end time
            // because we are not guaranteed a consistent order since the end time might be null
            query.orderByProcessInstanceEndTime().asc();
        }

        return query;
    }

    protected static void populateQuery(JsonNode queryNode, HistoricProcessInstanceQuery query, ProcessEngineConfigurationImpl engineConfiguration) {
        for (Map.Entry<String, JsonNode> propertyEntry : queryNode.properties()) {
            String property = propertyEntry.getKey();
            JsonNode value = propertyEntry.getValue();
            switch (property) {
                case "processInstanceId":
                    query.processInstanceId(value.stringValue());
                    break;
                case "processDefinitionId":
                    query.processDefinitionId(value.stringValue());
                    break;
                case "businessKey":
                    query.processInstanceBusinessKey(value.stringValue());
                    break;
                case "businessKeyLike":
                    query.processInstanceBusinessKeyLike(value.stringValue());
                    break;
                case "businessStatus":
                    query.processInstanceBusinessStatus(value.stringValue());
                    break;
                case "businessStatusLike":
                    query.processInstanceBusinessStatusLike(value.stringValue());
                    break;
                case "deploymentId":
                    query.deploymentId(value.stringValue());
                    break;
                case "deploymentIds":
                    query.deploymentIdIn(asStringList(value));
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
                case "deleted":
                    if (value.booleanValue()) {
                        query.deleted();
                    }
                    break;
                case "notDeleted":
                    if (value.booleanValue()) {
                        query.notDeleted();
                    }
                    break;
                case "startedBy":
                    query.startedBy(value.stringValue());
                    break;
                case "finishedBy":
                    query.finishedBy(value.stringValue());
                    break;
                case "state":
                    query.state(value.stringValue());
                    break;
                case "superProcessInstanceId":
                    query.superProcessInstanceId(value.stringValue());
                    break;
                case "excludeSubprocesses":
                    if (value.booleanValue()) {
                        query.excludeSubprocesses(true);
                    }
                    break;
                case "processDefinitionKeyIn":
                    query.processDefinitionKeyIn(asStringList(value));
                    break;
                case "excludeProcessDefinitionKeys":
                    query.excludeProcessDefinitionKeys(asStringList(value));
                    break;
                case "processKeyNotIn":
                    query.processDefinitionKeyNotIn(asStringList(value));
                    break;
                case "startedBefore":
                    query.startedBefore(AsyncHistoryDateUtil.parseDate(value.stringValue()));
                    break;
                case "startedAfter":
                    query.startedAfter(AsyncHistoryDateUtil.parseDate(value.stringValue()));
                    break;
                case "finishedBefore":
                    query.finishedBefore(AsyncHistoryDateUtil.parseDate(value.stringValue()));
                    break;
                case "finishedAfter":
                    query.finishedAfter(AsyncHistoryDateUtil.parseDate(value.stringValue()));
                    break;
                case "processDefinitionKey":
                    query.processDefinitionKey(value.stringValue());
                    break;
                case "processDefinitionCategory":
                    query.processDefinitionCategory(value.stringValue());
                    break;
                case "processDefinitionName":
                    query.processDefinitionName(value.stringValue());
                    break;
                case "processDefinitionVersion":
                    query.processDefinitionVersion(value.intValue());
                    break;
                case "processInstanceIds":
                    query.processInstanceIds(asStringSet(value));
                    break;
                case "activeActivityId":
                    query.activeActivityId(value.stringValue());
                    break;
                case "activeActivityIds":
                    query.activeActivityIds(asStringSet(value));
                    break;
                case "involvedUser":
                    query.involvedUser(value.stringValue());
                    break;
                case "involvedUserIdentityLink":
                    query.involvedUser(value.path("userId").stringValue(), value.path("type").stringValue());
                    break;
                case "involvedGroups":
                    query.involvedGroups(asStringSet(value));
                    break;
                case "involvedGroupIdentityLink":
                    query.involvedUser(value.path("groupId").stringValue(), value.path("type").stringValue());
                    break;
                case "withJobException":
                    if (value.booleanValue()) {
                        query.withJobException();
                    }
                    break;
                case "tenantId":
                    query.processInstanceTenantId(value.stringValue());
                    break;
                case "tenantIdLike":
                    query.processInstanceTenantIdLike(value.stringValue());
                    break;
                case "withoutTenantId":
                    if (value.booleanValue()) {
                        query.processInstanceWithoutTenantId();
                    }
                    break;
                case "name":
                    query.processInstanceName(value.stringValue());
                    break;
                case "nameLike":
                    query.processInstanceNameLike(value.stringValue());
                    break;
                case "nameLikeIgnoreCase":
                    query.processInstanceNameLikeIgnoreCase(value.stringValue());
                    break;
                case "callbackId":
                    query.processInstanceCallbackId(value.stringValue());
                case "callbackIds":
                    query.processInstanceCallbackIds(asStringSet(value));
                    break;
                case "callbackType":
                    query.processInstanceCallbackType(value.stringValue());
                    break;
                case "withoutCallbackId":
                    if (value.booleanValue()) {
                        query.withoutProcessInstanceCallbackId();
                    }
                    break;
                case "referenceId":
                    query.processInstanceReferenceId(value.stringValue());
                    break;
                case "referenceType":
                    query.processInstanceReferenceType(value.stringValue());
                    break;
                case "queryVariableValues":
                    populateQueryVariableValues(value, query, engineConfiguration);
                    break;
                case "orQueryObjects":
                    populateOrQueryObjects(value, query, engineConfiguration);
                    break;
                case "processInstanceRootScopeId":
                    query.processInstanceRootScopeId(value.stringValue());
                    break;
                case "processInstanceParentScopeId":
                    query.processInstanceParentScopeId(value.stringValue());
                    break;
                default:
                    throw new FlowableIllegalArgumentException("Query property " + property + " is not supported");
            }
        }
    }

    protected static void populateOrQueryObjects(JsonNode orQueryObjectsNode, HistoricProcessInstanceQuery query,
            ProcessEngineConfigurationImpl engineConfiguration) {
        if (orQueryObjectsNode.isArray()) {
            for (JsonNode orQueryObjectNode : orQueryObjectsNode) {
                HistoricProcessInstanceQuery orQuery = query.or();
                populateQuery(orQueryObjectNode, orQuery, engineConfiguration);
                query.endOr();
            }
        }
    }

    protected static void populateQueryVariableValues(JsonNode variableValuesNode, HistoricProcessInstanceQuery query,
            ProcessEngineConfigurationImpl engineConfiguration) {
        if (variableValuesNode.isArray()) {
            for (JsonNode variableValue : variableValuesNode) {
                String operatorString = variableValue.path("operator").stringValue(null);
                if (operatorString == null) {
                    throw new FlowableIllegalArgumentException("The variable value does not contain an operator value");
                }

                QueryOperator operator = QueryOperator.valueOf(operatorString);
                String variableName = variableValue.path("name").stringValue(null);
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
                        //break
                    default:
                        throw new FlowableIllegalArgumentException("Operator " + operator + " is not supported for the variable value");
                }
            }

        }

    }

    protected static Object extractVariableValue(JsonNode variableValueNode, ProcessEngineConfigurationImpl engineConfiguration) {
        String type = variableValueNode.path("type").stringValue(null);
        if (type == null) {
            throw new FlowableIllegalArgumentException("The variable value does not have a type");
        }

        VariableType variableType = engineConfiguration.getVariableTypes()
                .getVariableType(type);
        return variableType.getValue(new VariableValueJsonNodeValueFields(variableValueNode));
    }

    protected static List<String> asStringList(JsonNode node) {
        if (node != null && node.isArray() && !node.isEmpty()) {
            List<String> values = new ArrayList<>(node.size());
            for (JsonNode element : node) {
                values.add(element.stringValue());
            }
            return values;
        }

        return null;
    }

    protected static Set<String> asStringSet(JsonNode node) {
        if (node != null && node.isArray() && !node.isEmpty()) {
            List<String> values = new ArrayList<>(node.size());
            for (JsonNode element : node) {
                values.add(element.stringValue());
            }
            return new HashSet<>(values);
        }

        return null;
    }

    protected static JsonNode getBatchConfiguration(Batch batch, ProcessEngineConfigurationImpl engineConfiguration) {
        return engineConfiguration.getObjectMapper()
                .readTree(batch.getBatchDocumentJson(EngineConfigurationConstants.KEY_PROCESS_ENGINE_CONFIG));
    }

    protected static String prepareFailedResultAsJsonString(String errorMessage, ProcessEngineConfigurationImpl engineConfiguration) {
        return prepareFailedResultAsJsonString(errorMessage, null, engineConfiguration);
    }

    protected static String prepareFailedResultAsJsonString(String errorMessage, FlowableException exception,
            ProcessEngineConfigurationImpl engineConfiguration) {
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
            return node.path("name").stringValue();
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
            return node.path("textValue").stringValue(null);
        }

        @Override
        public void setTextValue(String textValue) {
            throw new UnsupportedOperationException("Not supported to set text value");
        }

        @Override
        public String getTextValue2() {
            return node.path("textValues").stringValue(null);
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
