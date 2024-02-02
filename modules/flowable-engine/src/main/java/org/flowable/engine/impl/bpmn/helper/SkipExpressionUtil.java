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
package org.flowable.engine.impl.bpmn.helper;

import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.delegate.Expression;
import org.flowable.common.engine.impl.el.ExpressionManager;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.DynamicBpmnConstants;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.context.BpmnOverrideContext;
import org.flowable.engine.impl.util.CommandContextUtil;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class SkipExpressionUtil {

    public static boolean isSkipExpressionEnabled(String skipExpression, String activityId, DelegateExecution execution, CommandContext commandContext) {
        if (skipExpression == null) {
            ProcessEngineConfigurationImpl processEngineConfiguration = CommandContextUtil.getProcessEngineConfiguration(commandContext);
            
            if (processEngineConfiguration.isEnableProcessDefinitionInfoCache()) {
                ObjectNode taskElementProperties = BpmnOverrideContext.getBpmnOverrideElementProperties(activityId, execution.getProcessDefinitionId());
                String overrideSkipExpression = DynamicPropertyUtil.getActiveValue(null, DynamicBpmnConstants.TASK_SKIP_EXPRESSION, taskElementProperties);
                if (overrideSkipExpression == null) {
                    return false;
                }
                
            } else {
                return false;
            }
        }
        return checkSkipExpressionVariable(activityId, execution, commandContext);
    }

    protected static boolean checkSkipExpressionVariable(String activityId, DelegateExecution execution, CommandContext commandContext) {
        if (CommandContextUtil.getProcessEngineConfiguration(commandContext).isEnableProcessDefinitionInfoCache()) {
            ObjectNode globalProperties = BpmnOverrideContext.getBpmnOverrideElementProperties(
                            DynamicBpmnConstants.GLOBAL_PROCESS_DEFINITION_PROPERTIES, execution.getProcessDefinitionId());
            if (isEnableSkipExpression(globalProperties)) {
                return true;
            }
        }
        
        String skipExpressionEnabledVariable = "_ACTIVITI_SKIP_EXPRESSION_ENABLED";
        Object isSkipExpressionEnabled = execution.getVariable(skipExpressionEnabledVariable);

        if (isSkipExpressionEnabled instanceof Boolean) {
            return ((Boolean) isSkipExpressionEnabled).booleanValue();
        }

        skipExpressionEnabledVariable = "_FLOWABLE_SKIP_EXPRESSION_ENABLED";
        isSkipExpressionEnabled = execution.getVariable(skipExpressionEnabledVariable);

        if (isSkipExpressionEnabled == null) {
            return false;

        } else if (isSkipExpressionEnabled instanceof Boolean) {
            return ((Boolean) isSkipExpressionEnabled).booleanValue();

        } else {
            throw new FlowableIllegalArgumentException("Skip expression variable does not resolve to a boolean. " + isSkipExpressionEnabled);
        }
    }

    public static boolean shouldSkipFlowElement(String skipExpressionString, String activityId, DelegateExecution execution, CommandContext commandContext) {
        ExpressionManager expressionManager = CommandContextUtil.getProcessEngineConfiguration(commandContext).getExpressionManager();
        Expression skipExpression = expressionManager.createExpression(resolveActiveSkipExpression(skipExpressionString, activityId, 
                        execution.getProcessDefinitionId(), commandContext));
        
        Object value = skipExpression.getValue(execution);

        if (value instanceof Boolean) {
            return ((Boolean) value).booleanValue();

        } else {
            throw new FlowableIllegalArgumentException("Skip expression does not resolve to a boolean: " + skipExpression.getExpressionText());
        }
    }
    
    protected static boolean isEnableSkipExpression(ObjectNode globalProperties) {
        if (globalProperties != null) {
            JsonNode overrideValueNode = globalProperties.get(DynamicBpmnConstants.ENABLE_SKIP_EXPRESSION);
            if (overrideValueNode != null && !overrideValueNode.isNull() && "true".equalsIgnoreCase(overrideValueNode.asText())) {
                return true;
            }
        }
        return false;
    }
    
    protected static String resolveActiveSkipExpression(String skipExpression, String activityId, String processDefinitionId, CommandContext commandContext) {
        ProcessEngineConfigurationImpl processEngineConfiguration = CommandContextUtil.getProcessEngineConfiguration(commandContext);
        
        String activeTaskSkipExpression = null;
        if (processEngineConfiguration.isEnableProcessDefinitionInfoCache()) {
            ObjectNode taskElementProperties = BpmnOverrideContext.getBpmnOverrideElementProperties(activityId, processDefinitionId);
            activeTaskSkipExpression = DynamicPropertyUtil.getActiveValue(skipExpression, DynamicBpmnConstants.TASK_SKIP_EXPRESSION, taskElementProperties);
        } else {
            activeTaskSkipExpression = skipExpression;
        }
        
        return activeTaskSkipExpression;
    }
}
