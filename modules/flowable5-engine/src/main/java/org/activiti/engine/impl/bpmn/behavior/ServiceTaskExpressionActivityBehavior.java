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

package org.activiti.engine.impl.bpmn.behavior;

import java.util.List;

import org.activiti.engine.ActivitiException;
import org.activiti.engine.delegate.BpmnError;
import org.activiti.engine.impl.bpmn.helper.ErrorPropagation;
import org.activiti.engine.impl.bpmn.helper.SkipExpressionUtil;
import org.activiti.engine.impl.context.Context;
import org.activiti.engine.impl.pvm.delegate.ActivityExecution;
import org.apache.commons.lang3.StringUtils;
import org.flowable.bpmn.model.MapExceptionEntry;
import org.flowable.common.engine.api.delegate.Expression;
import org.flowable.engine.DynamicBpmnConstants;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.impl.delegate.ActivityBehavior;

import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * {@link ActivityBehavior} that evaluates an expression when executed. Optionally, it sets the result of the expression as a variable on the execution.
 * 
 * @author Tom Baeyens
 * @author Christian Stettler
 * @author Frederik Heremans
 * @author Slawomir Wojtasiak (Patch for ACT-1159)
 * @author Falko Menge
 */
public class ServiceTaskExpressionActivityBehavior extends TaskActivityBehavior {

    protected String serviceTaskId;
    protected Expression expression;
    protected Expression skipExpression;
    protected String resultVariable;
    protected List<MapExceptionEntry> mapExceptions;

    public ServiceTaskExpressionActivityBehavior(String serviceTaskId, Expression expression, Expression skipExpression,
            String resultVariable, List<MapExceptionEntry> mapExceptions) {

        this.serviceTaskId = serviceTaskId;
        this.expression = expression;
        this.skipExpression = skipExpression;
        this.resultVariable = resultVariable;
        this.mapExceptions = mapExceptions;
    }

    @Override
    public void execute(DelegateExecution execution) {
        ActivityExecution activityExecution = (ActivityExecution) execution;
        Object value = null;
        try {
            boolean isSkipExpressionEnabled = SkipExpressionUtil.isSkipExpressionEnabled(activityExecution, skipExpression);
            if (!isSkipExpressionEnabled ||
                    (isSkipExpressionEnabled && !SkipExpressionUtil.shouldSkipFlowElement(activityExecution, skipExpression))) {

                if (Context.getProcessEngineConfiguration().isEnableProcessDefinitionInfoCache()) {
                    ObjectNode taskElementProperties = Context.getBpmnOverrideElementProperties(serviceTaskId, execution.getProcessDefinitionId());
                    if (taskElementProperties != null && taskElementProperties.has(DynamicBpmnConstants.SERVICE_TASK_EXPRESSION)) {
                        String overrideExpression = taskElementProperties.get(DynamicBpmnConstants.SERVICE_TASK_EXPRESSION).asText();
                        if (StringUtils.isNotEmpty(overrideExpression) && !overrideExpression.equals(expression.getExpressionText())) {
                            expression = Context.getProcessEngineConfiguration().getExpressionManager().createExpression(overrideExpression);
                        }
                    }
                }

                value = expression.getValue(execution);
                if (resultVariable != null) {
                    execution.setVariable(resultVariable, value);
                }
            }

            leave(activityExecution);
        } catch (Exception exc) {

            Throwable cause = exc;
            BpmnError error = null;
            while (cause != null) {
                if (cause instanceof BpmnError) {
                    error = (BpmnError) cause;
                    break;

                } else if (cause instanceof RuntimeException) {
                    if (ErrorPropagation.mapException((RuntimeException) cause, activityExecution, mapExceptions)) {
                        return;
                    }
                }
                cause = cause.getCause();
            }

            if (error != null) {
                ErrorPropagation.propagateError(error, activityExecution);
            } else {
                throw new ActivitiException(exc.getMessage(), exc);
            }
        }
    }
}
