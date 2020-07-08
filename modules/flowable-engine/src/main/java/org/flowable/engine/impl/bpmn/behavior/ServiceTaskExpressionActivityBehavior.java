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
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

import org.apache.commons.lang3.StringUtils;
import org.flowable.bpmn.model.MapExceptionEntry;
import org.flowable.bpmn.model.ServiceTask;
import org.flowable.common.engine.api.delegate.Expression;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.DynamicBpmnConstants;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.impl.bpmn.helper.ErrorPropagation;
import org.flowable.engine.impl.bpmn.helper.SkipExpressionUtil;
import org.flowable.engine.impl.context.BpmnOverrideContext;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.util.CommandContextUtil;

import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * ActivityBehavior that evaluates an expression when executed. Optionally, it sets the result of the expression as a variable on the execution.
 *
 * @author Tom Baeyens
 * @author Christian Stettler
 * @author Frederik Heremans
 * @author Slawomir Wojtasiak (Patch for ACT-1159)
 * @author Falko Menge
 * @author Filip Hrisafov
 */
public class ServiceTaskExpressionActivityBehavior extends TaskActivityBehavior {

    private static final long serialVersionUID = 1L;

    protected String serviceTaskId;
    protected Expression expression;
    protected Expression skipExpression;
    protected String resultVariable;
    protected List<MapExceptionEntry> mapExceptions;
    protected boolean useLocalScopeForResultVariable;
    protected boolean triggerable;
    protected boolean storeResultVariableAsTransient;

    public ServiceTaskExpressionActivityBehavior(ServiceTask serviceTask, Expression expression, Expression skipExpression) {

        this.serviceTaskId = serviceTask.getId();
        this.expression = expression;
        this.skipExpression = skipExpression;
        this.resultVariable = serviceTask.getResultVariableName();
        this.mapExceptions = serviceTask.getMapExceptions();
        this.useLocalScopeForResultVariable = serviceTask.isUseLocalScopeForResultVariable();
        this.triggerable = serviceTask.isTriggerable();
        this.storeResultVariableAsTransient = serviceTask.isStoreResultVariableAsTransient();
    }

    @Override
    public void execute(DelegateExecution execution) {
        Object value = null;
        try {
            CommandContext commandContext = CommandContextUtil.getCommandContext();
            String skipExpressionText = null;
            if (skipExpression != null) {
                skipExpressionText = skipExpression.getExpressionText();
            }
            boolean shouldLeave = !this.triggerable;
            boolean isSkipExpressionEnabled = SkipExpressionUtil.isSkipExpressionEnabled(skipExpressionText, serviceTaskId, execution, commandContext);
            if (!isSkipExpressionEnabled || !SkipExpressionUtil.shouldSkipFlowElement(skipExpressionText, serviceTaskId, execution, commandContext)) {

                if (CommandContextUtil.getProcessEngineConfiguration(commandContext).isEnableProcessDefinitionInfoCache()) {
                    ObjectNode taskElementProperties = BpmnOverrideContext.getBpmnOverrideElementProperties(serviceTaskId, execution.getProcessDefinitionId());
                    if (taskElementProperties != null && taskElementProperties.has(DynamicBpmnConstants.SERVICE_TASK_EXPRESSION)) {
                        String overrideExpression = taskElementProperties.get(DynamicBpmnConstants.SERVICE_TASK_EXPRESSION).asText();
                        if (StringUtils.isNotEmpty(overrideExpression) && !overrideExpression.equals(expression.getExpressionText())) {
                            expression = CommandContextUtil.getProcessEngineConfiguration(commandContext).getExpressionManager().createExpression(overrideExpression);
                        }
                    }
                }

                value = expression.getValue(execution);
                if (value instanceof CompletableFuture) {
                    // We should never leave when we have a future. The FutureCompleteAction should perform the leave
                    shouldLeave = false;
                    CommandContextUtil.getAgenda(commandContext).planFutureOperation((CompletableFuture) value, new FutureCompleteAction(execution));
                } else {
                    setExecutionVariableValue(value, execution);
                }
            }

            if (shouldLeave) {
                leave(execution);
            }

        } catch (Exception exc) {

            handleException(exc, execution);
        }
    }

    protected void handleException(Throwable exc, DelegateExecution execution) {
        ErrorPropagation.handleException(exc, (ExecutionEntity) execution, mapExceptions);
    }

    protected void setExecutionVariableValue(Object value, DelegateExecution execution) {
        if (resultVariable != null) {
            if (storeResultVariableAsTransient) {
                if (useLocalScopeForResultVariable) {
                    execution.setTransientVariableLocal(resultVariable, value);
                } else {
                    execution.setTransientVariable(resultVariable, value);
                }
            } else {
                if (useLocalScopeForResultVariable) {
                    execution.setVariableLocal(resultVariable, value);
                } else {
                    execution.setVariable(resultVariable, value);
                }
            }
        }
    }

    @Override
    public void trigger(DelegateExecution execution, String signalName, Object signalData) {
        leave(execution);
    }

    protected class FutureCompleteAction implements BiConsumer<Object, Throwable> {

        protected final DelegateExecution execution;

        public FutureCompleteAction(DelegateExecution execution) {
            this.execution = execution;
        }

        @Override
        public void accept(Object value, Throwable throwable) {
            if (throwable == null) {
                setExecutionVariableValue(value, execution);
                if (!triggerable) {
                    leave(execution);
                }
            } else {
                handleException(throwable, execution);
            }

        }
    }

}
