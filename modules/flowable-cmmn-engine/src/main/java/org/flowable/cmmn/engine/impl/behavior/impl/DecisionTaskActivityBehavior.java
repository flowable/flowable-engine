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

import java.util.List;
import java.util.Map;

import org.flowable.cmmn.api.delegate.DelegatePlanItemInstance;
import org.flowable.cmmn.engine.impl.behavior.PlanItemActivityBehavior;
import org.flowable.cmmn.engine.impl.persistence.entity.PlanItemInstanceEntity;
import org.flowable.cmmn.engine.impl.repository.CaseDefinitionUtil;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.cmmn.model.DecisionTask;
import org.flowable.cmmn.model.FieldExtension;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.delegate.Expression;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.dmn.api.DecisionExecutionAuditContainer;
import org.flowable.dmn.api.DmnRuleService;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import liquibase.util.StringUtils;

/**
 * @author martin.grofcik
 */
public class DecisionTaskActivityBehavior extends TaskActivityBehavior implements PlanItemActivityBehavior {

    protected static final String EXPRESSION_DECISION_TABLE_THROW_ERROR_FLAG = "decisionTaskThrowErrorOnNoHits";

    protected DecisionTask decisionTask;
    protected Expression decisionRefExpression;

    public DecisionTaskActivityBehavior(Expression decisionRefExpression, DecisionTask decisionTask) {
        super(decisionTask.isBlocking(), decisionTask.getBlockingExpression());
        this.decisionTask = decisionTask;
        this.decisionRefExpression = decisionRefExpression;
    }

    @Override
    public void execute(CommandContext commandContext, PlanItemInstanceEntity planItemInstanceEntity) {
        DmnRuleService dmnRuleService = CommandContextUtil.getDmnRuleService(commandContext);
        if (dmnRuleService == null) {
            throw new FlowableException("Could not execute decision instance: no dmn service found.");
        }

        String externalRef = null;
        if (decisionTask != null && decisionTask.getDecision() != null &&
                StringUtils.isNotEmpty(decisionTask.getDecision().getExternalRef())) {
            
            externalRef = decisionTask.getDecision().getExternalRef();
            
        } else if (decisionRefExpression != null) {
            Object externalRefValue = decisionRefExpression.getValue(planItemInstanceEntity);
            if (externalRefValue != null) {
                externalRef = externalRefValue.toString();
            }
            
            if (StringUtils.isEmpty(externalRef)) {
                throw new FlowableException("Could not execute decision: no externalRef defined");
            }
        }

        DecisionExecutionAuditContainer decisionExecutionAuditContainer = dmnRuleService.createExecuteDecisionBuilder().
                parentDeploymentId(CaseDefinitionUtil.getDefinitionDeploymentId(planItemInstanceEntity.getCaseDefinitionId())).
                decisionKey(externalRef).
                instanceId(planItemInstanceEntity.getCaseInstanceId()).
                executionId(planItemInstanceEntity.getId()).
                activityId(decisionTask.getId()).
                scopeType(ScopeTypes.CMMN).
                variables(planItemInstanceEntity.getVariables()).
                tenantId(planItemInstanceEntity.getTenantId()).
                executeWithAuditTrail();

        if (decisionExecutionAuditContainer == null) {
            throw new FlowableException("DMN decision table with key " + externalRef + " was not executed.");
        }
        
        if (decisionExecutionAuditContainer.isFailed()) {
            throw new FlowableException("DMN decision table with key " + externalRef + " execution failed. Cause: " + decisionExecutionAuditContainer.getExceptionMessage());
        }

        /* Throw error if there were no rules hit when the flag indicates to do this. */
        String throwErrorFieldValue = getFieldString(EXPRESSION_DECISION_TABLE_THROW_ERROR_FLAG);
        if (decisionExecutionAuditContainer.getDecisionResult().isEmpty() && throwErrorFieldValue != null) {
            if ("true".equalsIgnoreCase(throwErrorFieldValue)) {
                throw new FlowableException("DMN decision table with key " + externalRef + " did not hit any rules for the provided input.");
            
            } else if (!"false".equalsIgnoreCase(throwErrorFieldValue)) {
                Expression expression = CommandContextUtil.getExpressionManager(commandContext).createExpression(throwErrorFieldValue);
                Object expressionValue = expression.getValue(planItemInstanceEntity);
                
                if (expressionValue != null && expressionValue instanceof Boolean && ((Boolean) expressionValue)) {
                    throw new FlowableException("DMN decision table with key " + externalRef + " did not hit any rules for the provided input.");
                }
            }
        }

        setVariables(decisionExecutionAuditContainer.getDecisionResult(), externalRef, planItemInstanceEntity, 
                        CommandContextUtil.getCmmnEngineConfiguration(commandContext).getObjectMapper());

        CommandContextUtil.getAgenda().planCompletePlanItemInstanceOperation(planItemInstanceEntity);
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

    protected String getFieldString(String fieldName) {
        for (FieldExtension fieldExtension : decisionTask.getFieldExtensions()) {
            if (fieldName.equals(fieldExtension.getFieldName())) {
                if (StringUtils.isNotEmpty(fieldExtension.getStringValue())) {
                    return fieldExtension.getStringValue();
                    
                } else if (StringUtils.isNotEmpty(fieldExtension.getExpression())) {
                    return fieldExtension.getExpression();
                }
            }
        }
        
        return null;
    }

    @Override
    public void onStateTransition(CommandContext commandContext, DelegatePlanItemInstance planItemInstance, String transition) {

    }
}
