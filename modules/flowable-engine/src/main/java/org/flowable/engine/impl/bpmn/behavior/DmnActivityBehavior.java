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

import org.flowable.bpmn.model.FieldExtension;
import org.flowable.bpmn.model.Task;
import org.flowable.dmn.api.DmnRuleService;
import org.flowable.dmn.api.RuleEngineExecutionResult;
import org.flowable.engine.DynamicBpmnConstants;
import org.flowable.engine.common.api.FlowableException;
import org.flowable.engine.common.api.FlowableIllegalArgumentException;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.DelegateHelper;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.context.Context;
import org.flowable.engine.impl.el.ExpressionManager;
import org.flowable.engine.impl.util.ProcessDefinitionUtil;
import org.flowable.engine.repository.ProcessDefinition;

import com.fasterxml.jackson.databind.node.ObjectNode;

public class DmnActivityBehavior extends TaskActivityBehavior {

    private static final long serialVersionUID = 1L;

    protected static final String EXPRESSION_DECISION_TABLE_REFERENCE_KEY = "decisionTableReferenceKey";

    protected Task task;

    public DmnActivityBehavior(Task task) {
        this.task = task;
    }

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

        ProcessEngineConfigurationImpl processEngineConfiguration = Context.getProcessEngineConfiguration();
        ExpressionManager expressionManager = processEngineConfiguration.getExpressionManager();

        if (processEngineConfiguration.isEnableProcessDefinitionInfoCache()) {
            ObjectNode taskElementProperties = Context.getBpmnOverrideElementProperties(task.getId(), execution.getProcessDefinitionId());
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

        DmnRuleService ruleService = processEngineConfiguration.getDmnEngineRuleService();
        RuleEngineExecutionResult executionResult = ruleService.executeDecisionByKeyParentDeploymentIdAndTenantId(finaldecisionTableKeyValue,
                processDefinition.getDeploymentId(), execution.getVariables(), execution.getTenantId());

        execution.setVariables(executionResult.getResultVariables());

        leave(execution);
    }
}
