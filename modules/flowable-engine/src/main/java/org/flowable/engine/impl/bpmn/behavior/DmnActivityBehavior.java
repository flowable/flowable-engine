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
import org.flowable.dmn.api.DecisionExecutionAuditContainer;
import org.flowable.dmn.api.DmnRuleService;
import org.flowable.engine.DynamicBpmnConstants;
import org.flowable.engine.common.api.FlowableException;
import org.flowable.engine.common.api.FlowableIllegalArgumentException;
import org.flowable.engine.common.api.delegate.Expression;
import org.flowable.engine.common.impl.el.ExpressionManager;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.DelegateHelper;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.context.BpmnOverrideContext;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.impl.util.ProcessDefinitionUtil;
import org.flowable.engine.repository.ProcessDefinition;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class DmnActivityBehavior extends TaskActivityBehavior {

    private static final long serialVersionUID = 1L;

    protected static final String EXPRESSION_DECISION_TABLE_REFERENCE_KEY = "decisionTableReferenceKey";
    protected static final String EXPRESSION_DECISION_TABLE_THROW_ERROR_FLAG = "decisionTaskThrowErrorOnNoHits";
    protected static final String EXPRESSION_DECISION_TABLE_RESPONCE_HANDLER = "decisionTaskReponseHandler";

    protected Task task;
    
    protected DmnResponseHandler handler;

    public DmnActivityBehavior(Task task) {
        this.task = task;
    }

    @Override
    public void execute(DelegateExecution execution) {
        FieldExtension fieldExtension = DelegateHelper.getFlowElementField(execution, EXPRESSION_DECISION_TABLE_REFERENCE_KEY);
        if (fieldExtension == null || ((fieldExtension.getStringValue() == null || fieldExtension.getStringValue().length() == 0) &&
                (fieldExtension.getExpression() == null || fieldExtension.getExpression().length() == 0))) {

            throw new FlowableException("decisionTableReferenceKey is a required field extension for the dmn task " + task.getId());
        }

        String activeDecisionTableKey = null;
        if (fieldExtension.getExpression() != null && fieldExtension.getExpression().length() > 0) {
            activeDecisionTableKey = fieldExtension.getExpression();

        } else {
            activeDecisionTableKey = fieldExtension.getStringValue();
        }

        ProcessEngineConfigurationImpl processEngineConfiguration = CommandContextUtil.getProcessEngineConfiguration();
        ExpressionManager expressionManager = processEngineConfiguration.getExpressionManager();

        if (processEngineConfiguration.isEnableProcessDefinitionInfoCache()) {
            ObjectNode taskElementProperties = BpmnOverrideContext.getBpmnOverrideElementProperties(task.getId(), execution.getProcessDefinitionId());
            activeDecisionTableKey = getActiveValue(activeDecisionTableKey, DynamicBpmnConstants.DMN_TASK_DECISION_TABLE_KEY, taskElementProperties);
        }

        String finaldecisionTableKeyValue = null;
        Object decisionTableKeyValue = expressionManager.createExpression(activeDecisionTableKey).getValue(execution);
        if (decisionTableKeyValue != null) {
            if (decisionTableKeyValue instanceof String) {
                finaldecisionTableKeyValue = (String) decisionTableKeyValue;
            } else {
                throw new FlowableIllegalArgumentException("decisionTableReferenceKey expression does not resolve to a string: " + decisionTableKeyValue);
            }
        }

        if (finaldecisionTableKeyValue == null || finaldecisionTableKeyValue.length() == 0) {
            throw new FlowableIllegalArgumentException("decisionTableReferenceKey expression resolves to an empty value: " + decisionTableKeyValue);
        }

        ProcessDefinition processDefinition = ProcessDefinitionUtil.getProcessDefinition(execution.getProcessDefinitionId());

        DmnRuleService ruleService = CommandContextUtil.getDmnRuleService();

        DecisionExecutionAuditContainer decisionExecutionAuditContainer = ruleService.createExecuteDecisionBuilder()
                .decisionKey(finaldecisionTableKeyValue)
                .parentDeploymentId(processDefinition.getDeploymentId())
                .instanceId(execution.getProcessInstanceId())
                .executionId(execution.getId())
                .activityId(task.getId())
                .variables(execution.getVariables())
                .tenantId(execution.getTenantId())
                .executeWithAuditTrail();
        
        if (decisionExecutionAuditContainer.isFailed()) {
            throw new FlowableException("DMN decision table with key " + finaldecisionTableKeyValue + " execution failed. Cause: " + decisionExecutionAuditContainer.getExceptionMessage());
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
                    throw new FlowableException("DMN decision table with key " + finaldecisionTableKeyValue + " did not hit any rules for the provided input.");
                    
                } else if (!"false".equalsIgnoreCase(throwErrorString)) {
                    Expression expression = expressionManager.createExpression(throwErrorString);
                    Object expressionValue = expression.getValue(execution);
                    
                    if (expressionValue != null && expressionValue instanceof Boolean && ((Boolean) expressionValue)) {
                        throw new FlowableException("DMN decision table with key " + finaldecisionTableKeyValue + " did not hit any rules for the provided input.");
                    }
                }
            }
        }
        //Call custom decision table response handler if present
        FieldExtension handlerFieldExtension = DelegateHelper.getFlowElementField(execution, EXPRESSION_DECISION_TABLE_RESPONCE_HANDLER);
        boolean handlerFound = false;
        if(handlerFieldExtension != null) {
	        String handlerName = handlerFieldExtension.getStringValue();
	        if(!StringUtils.isBlank(handlerName)) {
		        try {
					Class<?> handlerClass = Class.forName(handlerName);
					handler = (DmnResponseHandler) handlerClass.newInstance();
					handler.handleResponse(execution, decisionExecutionAuditContainer.getDecisionResult(), finaldecisionTableKeyValue);
					handlerFound = true;
				} catch (ClassNotFoundException e) {
	                throw new FlowableException("DMN response handler with reference " + handlerName + " is not found.");
				} catch (InstantiationException e) {
					throw new FlowableException("DMN response handler with reference " + handlerName + " could not be instatiated.");
				} catch (IllegalAccessException e) {
					throw new FlowableException("DMN response handler with reference " + handlerName + " is not accessible.");
				} catch (ClassCastException e) {
					throw new FlowableException("DMN response handler with reference " + handlerName + " is not an instance of DmnResponseHandler.");
				}
	        }
        }
        if(!handlerFound && handler != null) {
			handler.handleResponse(execution, decisionExecutionAuditContainer.getDecisionResult(), finaldecisionTableKeyValue);
        }
        setVariablesOnExecution(decisionExecutionAuditContainer.getDecisionResult(), finaldecisionTableKeyValue, execution, processEngineConfiguration.getObjectMapper());
        leave(execution);
    }

    protected void setVariablesOnExecution(List<Map<String, Object>> executionResult, String decisionKey, DelegateExecution execution, ObjectMapper objectMapper) {
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
}
