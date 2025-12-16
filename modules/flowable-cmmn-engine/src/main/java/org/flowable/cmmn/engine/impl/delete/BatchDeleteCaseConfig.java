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
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.flowable.batch.api.Batch;
import org.flowable.batch.api.BatchPart;
import org.flowable.batch.api.BatchService;
import org.flowable.cmmn.api.history.HistoricCaseInstanceQuery;
import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.impl.interceptor.EngineConfigurationConstants;
import org.flowable.job.service.impl.history.async.AsyncHistoryDateUtil;
import org.flowable.variable.api.types.ValueFields;
import org.flowable.variable.api.types.VariableType;
import org.flowable.variable.service.impl.QueryOperator;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * @author Filip Hrisafov
 */
public class BatchDeleteCaseConfig {

    protected final Batch batch;
    protected final BatchPart batchPart;
    protected final String error;
    protected final boolean sequentialExecution;
    protected int batchSize;
    protected HistoricCaseInstanceQuery query;

    protected BatchDeleteCaseConfig(Batch batch, BatchPart batchPart, String error, boolean sequentialExecution) {
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

    public HistoricCaseInstanceQuery getQuery() {
        return query;
    }

    public static BatchDeleteCaseConfig create(String batchPartId, CmmnEngineConfiguration engineConfiguration) {
        BatchService batchService = engineConfiguration.getBatchServiceConfiguration().getBatchService();
        BatchPart batchPart = batchService.getBatchPart(batchPartId);
        if (batchPart == null) {
            throw new FlowableIllegalArgumentException("There is no batch part with the id " + batchPartId);
        }

        Batch batch = batchService.getBatch(batchPart.getBatchId());
        JsonNode batchConfiguration = getBatchConfiguration(batch, engineConfiguration);
        boolean sequentialExecution = batchConfiguration.path("sequential").booleanValue();

        JsonNode queryNode = batchConfiguration.path("query");
        if (queryNode.isMissingNode()) {
            return new BatchDeleteCaseConfig(batch, batchPart,
                    prepareFailedResultAsJsonString("Batch configuration has no query definition", engineConfiguration), sequentialExecution);
        }

        JsonNode batchSizeNode = batchConfiguration.path("batchSize");
        if (batchSizeNode.isMissingNode()) {
            return new BatchDeleteCaseConfig(batch, batchPart, prepareFailedResultAsJsonString("Batch configuration has no batch size", engineConfiguration),
                    sequentialExecution);
        }

        HistoricCaseInstanceQuery query;

        try {
            query = createQuery(queryNode, engineConfiguration);
        } catch (FlowableException exception) {
            return new BatchDeleteCaseConfig(batch, batchPart, prepareFailedResultAsJsonString("Failed to create query", exception, engineConfiguration),
                    sequentialExecution);
        }

        BatchDeleteCaseConfig config = new BatchDeleteCaseConfig(batch, batchPart, null, sequentialExecution);
        config.batchSize = batchSizeNode.intValue();
        config.query = query;

        return config;
    }

    protected static HistoricCaseInstanceQuery createQuery(JsonNode queryNode, CmmnEngineConfiguration engineConfiguration) {
        HistoricCaseInstanceQuery query = engineConfiguration.getCmmnHistoryService()
                .createHistoricCaseInstanceQuery();

        populateQuery(queryNode, query, engineConfiguration);

        if (queryNode.hasNonNull("finishedBefore") || queryNode.hasNonNull("finishedAfter") || queryNode.path("finished").asBoolean(false)) {
            // When the query has finishedBefore, finishedAfter or finished then we need to order by the end time
            // This is done in order to improve the performance when getting pages with large offsets.
            // When the properties are not set we cannot order on the end time
            // because we are not guaranteed a consistent order since the end time might be null
            query.orderByEndTime().asc();
        }

        return query;
    }

    protected static void populateQuery(JsonNode queryNode, HistoricCaseInstanceQuery query, CmmnEngineConfiguration engineConfiguration) {
        for (Map.Entry<String, JsonNode> propertyEntry : queryNode.properties()) {
            String property = propertyEntry.getKey();
            JsonNode value = propertyEntry.getValue();
            switch (property) {
                case "caseDefinitionId":
                    query.caseDefinitionId(value.stringValue());
                    break;
                case "caseDefinitionKey":
                    query.caseDefinitionKey(value.stringValue());
                    break;
                case "caseDefinitionKeys":
                    query.caseDefinitionKeys(asStringSet(value));
                    break;
                case "excludeCaseDefinitionKeys":
                    query.excludeCaseDefinitionKeys(asStringSet(value));
                    break;
                case "caseDefinitionIds":
                    query.caseDefinitionIds(asStringSet(value));
                    break;
                case "caseDefinitionName":
                    query.caseDefinitionName(value.stringValue());
                    break;
                case "caseDefinitionCategory":
                    query.caseDefinitionCategory(value.stringValue());
                    break;
                case "caseDefinitionVersion":
                    query.caseDefinitionVersion(value.intValue());
                    break;
                case "caseInstanceId":
                    query.caseInstanceId(value.stringValue());
                    break;
                case "caseInstanceIds":
                    query.caseInstanceIds(asStringSet(value));
                    break;
                case "caseInstanceName":
                    query.caseInstanceName(value.stringValue());
                case "caseInstanceNameLike":
                    query.caseInstanceNameLike(value.stringValue());
                case "caseInstanceNameLikeIgnoreCase":
                    query.caseInstanceNameLikeIgnoreCase(value.stringValue());
                    break;
                case "businessKey":
                    query.caseInstanceBusinessKey(value.stringValue());
                    break;
                case "businessStatus":
                    query.caseInstanceBusinessStatus(value.stringValue());
                    break;
                case "caseInstanceParentId":
                    query.caseInstanceParentId(value.stringValue());
                    break;
                case "withoutCaseInstanceParentId":
                    if (value.booleanValue()) {
                        query.withoutCaseInstanceParent();
                    }
                    break;
                case "deploymentId":
                    query.deploymentId(value.stringValue());
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
                case "startedBy":
                    query.startedBy(value.stringValue());
                    break;
                case "finishedBy":
                    query.finishedBy(value.stringValue());
                    break;
                case "lastReactivatedBefore":
                    query.lastReactivatedBefore(AsyncHistoryDateUtil.parseDate(value.stringValue()));
                    break;
                case "lastReactivatedAfter":
                    query.lastReactivatedAfter(AsyncHistoryDateUtil.parseDate(value.stringValue()));
                case "lastReactivatedBy":
                    query.lastReactivatedBy(value.stringValue());
                    break;
                case "callbackId":
                    query.caseInstanceCallbackId(value.stringValue());
                case "callbackIds":
                    query.caseInstanceCallbackIds(asStringSet(value));
                    break;
                case "callbackType":
                    query.caseInstanceCallbackType(value.stringValue());
                    break;
                case "withoutCallbackId":
                    if (value.booleanValue()) {
                        query.withoutCaseInstanceCallbackId();
                    }
                    break;
                case "referenceId":
                    query.caseInstanceReferenceId(value.stringValue());
                    break;
                case "referenceType":
                    query.caseInstanceReferenceType(value.stringValue());
                    break;
                case "tenantId":
                    query.caseInstanceTenantId(value.stringValue());
                    break;
                case "withoutTenantId":
                    if (value.booleanValue()) {
                        query.caseInstanceWithoutTenantId();
                    }
                    break;
                case "activePlanItemDefinitionId":
                    query.activePlanItemDefinitionId(value.stringValue());
                    break;
                case "activePlanItemDefinitionIds":
                    query.activePlanItemDefinitionIds(asStringSet(value));
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
                case "queryVariableValues":
                    populateQueryVariableValues(value, query, engineConfiguration);
                    break;
                case "orQueryObjects":
                    populateOrQueryObjects(value, query, engineConfiguration);
                    break;
                case "caseInstanceRootScopeId":
                    query.caseInstanceRootScopeId(value.stringValue());
                    break;
                case "caseInstanceParentScopeId":
                    query.caseInstanceParentScopeId(value.stringValue());
                    break;
                default:
                    throw new FlowableIllegalArgumentException("Query property " + property + " is not supported");
            }
        }
    }

    protected static void populateOrQueryObjects(JsonNode orQueryObjectsNode, HistoricCaseInstanceQuery query, CmmnEngineConfiguration engineConfiguration) {
        if (orQueryObjectsNode.isArray()) {
            for (JsonNode orQueryObjectNode : orQueryObjectsNode) {
                HistoricCaseInstanceQuery orQuery = query.or();
                populateQuery(orQueryObjectNode, orQuery, engineConfiguration);
                query.endOr();
            }
        }
    }

    protected static void populateQueryVariableValues(JsonNode variableValuesNode, HistoricCaseInstanceQuery query,
            CmmnEngineConfiguration engineConfiguration) {
        if (variableValuesNode.isArray()) {
            for (JsonNode variableValue : variableValuesNode) {
                String operatorString = variableValue.path("operator").stringValue(null);
                if (operatorString == null) {
                    throw new FlowableIllegalArgumentException("The variable value does not contain an operator value");
                }

                QueryOperator operator = QueryOperator.valueOf(operatorString);
                String variableName = variableValue.path("name").stringValue();
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

    protected static Object extractVariableValue(JsonNode variableValueNode, CmmnEngineConfiguration engineConfiguration) {
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

    protected static JsonNode getBatchConfiguration(Batch batch, CmmnEngineConfiguration engineConfiguration) {
        return engineConfiguration.getObjectMapper()
                .readTree(batch.getBatchDocumentJson(EngineConfigurationConstants.KEY_CMMN_ENGINE_CONFIG));
    }

    protected static String prepareFailedResultAsJsonString(String errorMessage, CmmnEngineConfiguration engineConfiguration) {
        return prepareFailedResultAsJsonString(errorMessage, null, engineConfiguration);
    }

    protected static String prepareFailedResultAsJsonString(String errorMessage, FlowableException exception, CmmnEngineConfiguration engineConfiguration) {
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
            return node.path("textValue").stringValue();
        }

        @Override
        public void setTextValue(String textValue) {
            throw new UnsupportedOperationException("Not supported to set text value");
        }

        @Override
        public String getTextValue2() {
            return node.path("textValues").stringValue();
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
