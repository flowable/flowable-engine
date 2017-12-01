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
package org.flowable.cmmn.engine.impl.behavior.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.flowable.cmmn.api.repository.CaseDefinition;
import org.flowable.cmmn.engine.impl.behavior.CoreCmmnActivityBehavior;
import org.flowable.cmmn.engine.impl.persistence.entity.PlanItemInstanceEntity;
import org.flowable.cmmn.engine.impl.repository.CaseDefinitionUtil;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.cmmn.model.FieldExtension;
import org.flowable.cmmn.model.ServiceTask;
import org.flowable.dmn.api.DecisionExecutionAuditContainer;
import org.flowable.dmn.api.DmnRuleService;
import org.flowable.engine.common.api.FlowableException;
import org.flowable.engine.common.api.FlowableIllegalArgumentException;
import org.flowable.engine.common.api.delegate.Expression;
import org.flowable.engine.common.impl.interceptor.CommandContext;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * @author martin.grofcik
 */
public class ServiceTaskDmnActivityBehavior extends CoreCmmnActivityBehavior {

    protected static final String EXPRESSION_DECISION_TABLE_REFERENCE_KEY = "decisionTableReferenceKey";
    protected static final String EXPRESSION_DECISION_TABLE_THROW_ERROR_FLAG = "decisionTaskThrowErrorOnNoHits";

    protected ServiceTask task;

    public ServiceTaskDmnActivityBehavior(ServiceTask task) {
        this.task = task;
    }

    @Override
    public void execute(CommandContext commandContext, PlanItemInstanceEntity planItemInstanceEntity) {
        String referenceKeyValue = getExpressionValue(EXPRESSION_DECISION_TABLE_REFERENCE_KEY, String.class, null, commandContext, planItemInstanceEntity);

        if (referenceKeyValue == null) {
            throw new FlowableIllegalArgumentException("ReferenceKey must not be null");
        }

        DmnRuleService dmnRuleService = CommandContextUtil.getDmnRuleService(commandContext);
        CaseDefinition caseDefinition = CaseDefinitionUtil.getCaseDefinition(planItemInstanceEntity.getCaseDefinitionId());
        DecisionExecutionAuditContainer decisionExecutionAuditContainer = dmnRuleService.createExecuteDecisionBuilder().
                decisionKey(referenceKeyValue).
                parentDeploymentId(caseDefinition.getDeploymentId()).
                instanceId(planItemInstanceEntity.getCaseInstanceId()).
                executionId(planItemInstanceEntity.getId()).
                activityId(task.getId()).
                variables(planItemInstanceEntity.getVariables()).
                tenantId(planItemInstanceEntity.getTenantId()).
                executeWithAuditTrail();

        if (decisionExecutionAuditContainer.isFailed()) {
            throw new FlowableException("DMN decision table with key " + referenceKeyValue + " execution failed. Cause: " + decisionExecutionAuditContainer.getExceptionMessage());
        }

        /*Throw error if there were no rules hit when the flag indicates to do this.*/
        Boolean throwErrorFieldValue = getExpressionValue(EXPRESSION_DECISION_TABLE_THROW_ERROR_FLAG, Boolean.class, false, commandContext, planItemInstanceEntity);
        if (decisionExecutionAuditContainer.getDecisionResult().isEmpty() && throwErrorFieldValue) {

            throw new FlowableException("DMN decision table with key " + referenceKeyValue + " did not hit any rules for the provided input.");
        }

        setVariables(
                decisionExecutionAuditContainer.getDecisionResult(),
                referenceKeyValue,
                planItemInstanceEntity,
                CommandContextUtil.getCmmnEngineConfiguration(commandContext).getObjectMapper()
        );

        CommandContextUtil.getAgenda().planCompletePlanItemInstance(planItemInstanceEntity);
    }

    protected void setVariables(List<Map<String, Object>> executionResult,
                                String decisionKey,
                                PlanItemInstanceEntity planItemInstanceEntity,
                                ObjectMapper objectMapper) {
        if (executionResult == null || executionResult.isEmpty()) {
            return;
        }

        // multiple rule results
        // put on execution as JSON array; each entry contains output id (key) and output value (value)
        if (executionResult.size() > 1) {
            ArrayNode ruleResultNode = objectMapper.createArrayNode();

            for (Map<String, Object> ruleResult : executionResult) {
                ObjectNode outputResultNode = objectMapper.createObjectNode();

                for (Map.Entry<String, Object> outputResult : ruleResult.entrySet()) {
                    outputResultNode.set(outputResult.getKey(), objectMapper.convertValue(outputResult.getValue(), JsonNode.class));
                }

                ruleResultNode.add(outputResultNode);
            }

            planItemInstanceEntity.setVariable(decisionKey, ruleResultNode);
        } else {
            // single rule result
            // put on execution output id (key) and output value (value)
            Map<String, Object> ruleResult = executionResult.get(0);

            for (Map.Entry<String, Object> outputResult : ruleResult.entrySet()) {
                planItemInstanceEntity.setVariable(outputResult.getKey(), outputResult.getValue());
            }
        }
    }

    protected <T> T getExpressionValue(String expressionString, Class<T> expectedType, T defaultValue, CommandContext commandContext, PlanItemInstanceEntity planItemInstanceEntity) {
        Object expressionValue;
        try {
            String fieldString = getFieldString(expressionString);
            if (fieldString != null) {
                Expression referenceKeyExpression = CommandContextUtil.getCmmnEngineConfiguration(commandContext).getExpressionManager().createExpression(
                        fieldString
                );
                expressionValue = referenceKeyExpression.getValue(planItemInstanceEntity);
            } else {
                expressionValue = defaultValue;
            }
        } catch (Exception exc) {
            throw new FlowableException(exc.getMessage(), exc);
        }
        if (expressionValue != null && !expressionValue.getClass().isAssignableFrom(expectedType)) {
            throw new FlowableIllegalArgumentException("Expression '"+ expressionString +"' must be resolved to " + expectedType.getName() + " was " + expressionValue);
        }
        return (T) expressionValue;
    }

    private String getFieldString(String fieldName) {
        Iterator<FieldExtension> iterator = this.task.getFieldExtensions().iterator();
        while (iterator.hasNext()) {
            FieldExtension fieldExtension = iterator.next();
            if (fieldName.equals(fieldExtension.getFieldName())) {
                return fieldExtension.getStringValue();
            }
        }
        return null;
    }
}
