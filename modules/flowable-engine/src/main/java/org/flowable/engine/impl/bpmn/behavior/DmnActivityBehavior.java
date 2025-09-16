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
package org.flowable.engine.impl.bpmn.behavior;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.flowable.bpmn.model.FieldExtension;
import org.flowable.bpmn.model.Task;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.delegate.Expression;
import org.flowable.common.engine.impl.el.ExpressionManager;
import org.flowable.dmn.api.DecisionExecutionAuditContainer;
import org.flowable.dmn.api.DecisionServiceExecutionAuditContainer;
import org.flowable.dmn.api.DmnDecisionService;
import org.flowable.dmn.api.ExecuteDecisionBuilder;
import org.flowable.engine.DynamicBpmnConstants;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.DelegateHelper;
import org.flowable.engine.impl.bpmn.helper.DynamicPropertyUtil;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.context.BpmnOverrideContext;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.impl.util.ProcessDefinitionUtil;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class DmnActivityBehavior extends TaskActivityBehavior {

    private static final long serialVersionUID = 1L;

    protected static final String EXPRESSION_DECISION_TABLE_REFERENCE_KEY = "decisionTableReferenceKey";
    protected static final String EXPRESSION_DECISION_TABLE_THROW_ERROR_FLAG = "decisionTaskThrowErrorOnNoHits";
    protected static final String EXPRESSION_DECISION_TABLE_FALLBACK_TO_DEFAULT_TENANT = "fallbackToDefaultTenant";
    protected static final String EXPRESSION_DECISION_TABLE_SAME_DEPLOYMENT = "sameDeployment";

    protected Task task;

    public DmnActivityBehavior(Task task) {
        this.task = task;
    }

    @Override
    public void execute(DelegateExecution execution) {
        FieldExtension fieldExtension = DelegateHelper.getFlowElementField(execution, EXPRESSION_DECISION_TABLE_REFERENCE_KEY);
        if (fieldExtension == null || ((fieldExtension.getStringValue() == null || fieldExtension.getStringValue().length() == 0) &&
                (fieldExtension.getExpression() == null || fieldExtension.getExpression().length() == 0))) {

            throw new FlowableException("decisionTableReferenceKey is a required field extension for the dmn task " + task.getId() + " in " + execution);
        }

        String activeDecisionKey = null;
        if (fieldExtension.getExpression() != null && fieldExtension.getExpression().length() > 0) {
            activeDecisionKey = fieldExtension.getExpression();

        } else {
            activeDecisionKey = fieldExtension.getStringValue();
        }

        ProcessEngineConfigurationImpl processEngineConfiguration = CommandContextUtil.getProcessEngineConfiguration();
        ExpressionManager expressionManager = processEngineConfiguration.getExpressionManager();

        if (processEngineConfiguration.isEnableProcessDefinitionInfoCache()) {
            ObjectNode taskElementProperties = BpmnOverrideContext.getBpmnOverrideElementProperties(task.getId(), execution.getProcessDefinitionId());
            activeDecisionKey = DynamicPropertyUtil.getActiveValue(activeDecisionKey, DynamicBpmnConstants.DMN_TASK_DECISION_TABLE_KEY, taskElementProperties);
        }

        String finalDecisionKeyValue = null;
        Object decisionKeyValue = expressionManager.createExpression(activeDecisionKey).getValue(execution);
        if (decisionKeyValue != null) {
            if (decisionKeyValue instanceof String) {
                finalDecisionKeyValue = (String) decisionKeyValue;
            } else {
                throw new FlowableIllegalArgumentException("decisionTableReferenceKey expression does not resolve to a string: " + decisionKeyValue);
            }
        }

        if (finalDecisionKeyValue == null || finalDecisionKeyValue.length() == 0) {
            throw new FlowableIllegalArgumentException("decisionTableReferenceKey expression resolves to an empty value: " + decisionKeyValue);
        }

        DmnDecisionService ruleService = CommandContextUtil.getDmnRuleService();

        ExecuteDecisionBuilder executeDecisionBuilder = ruleService.createExecuteDecisionBuilder()
            .decisionKey(finalDecisionKeyValue)
            .instanceId(execution.getProcessInstanceId())
            .executionId(execution.getId())
            .activityId(task.getId())
            .variables(execution.getVariables())
            .tenantId(execution.getTenantId());

        applyFallbackToDefaultTenant(execution, executeDecisionBuilder);
        applyParentDeployment(execution, executeDecisionBuilder, processEngineConfiguration);

        DecisionExecutionAuditContainer decisionExecutionAuditContainer = executeDecisionBuilder.executeWithAuditTrail();

        if (decisionExecutionAuditContainer.isFailed()) {
            throw new FlowableException("DMN decision with key " + finalDecisionKeyValue + " execution failed. Cause: " + decisionExecutionAuditContainer.getExceptionMessage() + " in " + execution);
        }

        /*Throw error if there were no rules hit when the flag indicates to do this.*/
        FieldExtension throwErrorFieldExtension = DelegateHelper.getFlowElementField(execution, EXPRESSION_DECISION_TABLE_THROW_ERROR_FLAG);
        if (throwErrorFieldExtension != null) {
            String throwErrorString = null;
            if (StringUtils.isNotEmpty(throwErrorFieldExtension.getStringValue())) {
                throwErrorString = throwErrorFieldExtension.getStringValue();
                
            } else if (StringUtils.isNotEmpty(throwErrorFieldExtension.getExpression())) {
                throwErrorString = throwErrorFieldExtension.getExpression();
            }
            
            if (decisionExecutionAuditContainer.getDecisionResult().isEmpty() && throwErrorString != null) {
                if ("true".equalsIgnoreCase(throwErrorString)) {
                    throw new FlowableException("DMN decision with key " + finalDecisionKeyValue + " did not hit any rules for the provided input. In " + execution);
                    
                } else if (!"false".equalsIgnoreCase(throwErrorString)) {
                    Expression expression = expressionManager.createExpression(throwErrorString);
                    Object expressionValue = expression.getValue(execution);
                    
                    if (expressionValue instanceof Boolean && ((Boolean) expressionValue)) {
                        throw new FlowableException("DMN decision with key " + finalDecisionKeyValue + " did not hit any rules for the provided input. In " + execution);
                    }
                }
            }
        }

        if (processEngineConfiguration.getDecisionTableVariableManager() != null) {
            if (decisionExecutionAuditContainer instanceof DecisionServiceExecutionAuditContainer decisionServiceExecutionAuditContainer) {
                processEngineConfiguration.getDecisionTableVariableManager().setDecisionServiceVariablesOnExecution(decisionServiceExecutionAuditContainer.getDecisionServiceResult(),
                    finalDecisionKeyValue, execution, processEngineConfiguration.getObjectMapper(), decisionExecutionAuditContainer.isMultipleResults());
            } else {
                processEngineConfiguration.getDecisionTableVariableManager().setVariablesOnExecution(decisionExecutionAuditContainer.getDecisionResult(),
                    finalDecisionKeyValue, execution, processEngineConfiguration.getObjectMapper(), decisionExecutionAuditContainer.isMultipleResults());
            }
            
        } else {
            boolean multipleResults = decisionExecutionAuditContainer.isMultipleResults() && processEngineConfiguration.isAlwaysUseArraysForDmnMultiHitPolicies();

            if (decisionExecutionAuditContainer instanceof DecisionServiceExecutionAuditContainer decisionServiceExecutionAuditContainer) {
                setDecisionServiceVariablesOnExecution(decisionServiceExecutionAuditContainer.getDecisionServiceResult(), finalDecisionKeyValue,
                    execution, processEngineConfiguration.getObjectMapper(), multipleResults);
            } else {
                setVariablesOnExecution(decisionExecutionAuditContainer.getDecisionResult(), finalDecisionKeyValue,
                    execution, processEngineConfiguration.getObjectMapper(), multipleResults);
            }
        }

        leave(execution);
    }

    protected void applyFallbackToDefaultTenant(DelegateExecution execution, ExecuteDecisionBuilder executeDecisionBuilder) {
        FieldExtension fallbackfieldExtension = DelegateHelper.getFlowElementField(execution, EXPRESSION_DECISION_TABLE_FALLBACK_TO_DEFAULT_TENANT);
        if (fallbackfieldExtension != null && ((fallbackfieldExtension.getStringValue() != null && fallbackfieldExtension.getStringValue().length() != 0))) {
            String fallbackToDefaultTenant = fallbackfieldExtension.getStringValue();
            if (StringUtils.isNotEmpty(fallbackToDefaultTenant) && Boolean.parseBoolean(fallbackToDefaultTenant)) {
                executeDecisionBuilder.fallbackToDefaultTenant();
            }
        }
    }

    protected void applyParentDeployment(DelegateExecution execution, ExecuteDecisionBuilder executeDecisionBuilder,
            ProcessEngineConfigurationImpl processEngineConfiguration) {

        FieldExtension sameDeploymentFieldExtension = DelegateHelper.getFlowElementField(execution, EXPRESSION_DECISION_TABLE_SAME_DEPLOYMENT);
        String parentDeploymentId;
        if (sameDeploymentFieldExtension != null) {
            if (Boolean.parseBoolean(sameDeploymentFieldExtension.getStringValue())) {
                parentDeploymentId = ProcessDefinitionUtil.getDefinitionDeploymentId(execution.getProcessDefinitionId(), processEngineConfiguration);
            } else {
                // If same deployment has not been requested then don't pass parentDeploymentId
                parentDeploymentId = null;
            }
        } else {
            // backwards compatibility (always apply parent deployment id)
            parentDeploymentId = ProcessDefinitionUtil.getDefinitionDeploymentId(execution.getProcessDefinitionId(), processEngineConfiguration);

        }
        executeDecisionBuilder.parentDeploymentId(parentDeploymentId);
    }

    protected void setDecisionServiceVariablesOnExecution(Map<String, List<Map<String, Object>>> executionResult, String decisionServiceKey, DelegateExecution execution, ObjectMapper objectMapper, boolean multipleResults) {
        if (executionResult == null || (executionResult.isEmpty() && !multipleResults)) {
            return;
        }

        // multiple rule results
        // put on execution as JSON array; each entry contains output id (key) and output value (value)
        // this should be always done for decision tables of type rule order and output order
        if (hasMultipleResults(executionResult) || multipleResults) {
            ObjectNode decisionResultNode = objectMapper.createObjectNode();

            for (Map.Entry<String, List<Map<String, Object>>> decisionExecutionResult : executionResult.entrySet()) {
                ArrayNode ruleResultNode = objectMapper.createArrayNode();
                for (Map<String, Object> ruleResult : decisionExecutionResult.getValue()) {
                    ObjectNode outputResultNode = objectMapper.createObjectNode();
                    for (Map.Entry<String, Object> outputResult : ruleResult.entrySet()) {
                        outputResultNode.set(outputResult.getKey(), objectMapper.convertValue(outputResult.getValue(), JsonNode.class));
                    }
                    ruleResultNode.add(outputResultNode);
                }

                decisionResultNode.set(decisionExecutionResult.getKey(), ruleResultNode);
            }

            execution.setVariable(decisionServiceKey, decisionResultNode);
        } else {
            // single rule result (also in multiple decisions)
            // put on execution output id (key) and output value (value)
            // mind: when using the same variable multiple times (f.e. in multiple decisions)
            // the last value will be set on the execution.
                executionResult.values().forEach(decisionResult -> {
                for (Map.Entry<String, Object> outputResult : decisionResult.get(0).entrySet()) {
                    execution.setVariable(outputResult.getKey(), outputResult.getValue());
                }
            });

        }
    }

    protected void setVariablesOnExecution(List<Map<String, Object>> executionResult, String decisionKey, DelegateExecution execution, ObjectMapper objectMapper, boolean multipleResults) {
        if (executionResult == null || (executionResult.isEmpty() && !multipleResults)) {
            return;
        }

        // multiple rule results
        // put on execution as JSON array; each entry contains output id (key) and output value (value)
        // this should be always done for decision tables of type rule order and output order
        if (executionResult.size() > 1 || multipleResults) {
            ArrayNode ruleResultNode = objectMapper.createArrayNode();

            for (Map<String, Object> ruleResult : executionResult) {
                ObjectNode outputResultNode = objectMapper.createObjectNode();

                for (Map.Entry<String, Object> outputResult : ruleResult.entrySet()) {
                    outputResultNode.set(outputResult.getKey(), objectMapper.convertValue(outputResult.getValue(), JsonNode.class));
                }

                ruleResultNode.add(outputResultNode);
            }

            execution.setVariable(decisionKey, ruleResultNode);
        } else {
            // single rule result
            // put on execution output id (key) and output value (value)
            Map<String, Object> ruleResult = executionResult.get(0);

            for (Map.Entry<String, Object> outputResult : ruleResult.entrySet()) {
                execution.setVariable(outputResult.getKey(), outputResult.getValue());
            }
        }
    }

    protected boolean hasMultipleResults(Map<String, List<Map<String, Object>>> executionResult) {
        boolean hasMultipleResults = false;

        // check if at least one of the decisions in the decision service result has more than 1 rule result.
        for (Map.Entry<String, List<Map<String, Object>>> entry : executionResult.entrySet()) {
            List<Map<String, Object>> decisionResult = entry.getValue();
            if (decisionResult.size() > 1) {
                hasMultipleResults = true;
                break;
            }
        }

        return hasMultipleResults;
    }
}
