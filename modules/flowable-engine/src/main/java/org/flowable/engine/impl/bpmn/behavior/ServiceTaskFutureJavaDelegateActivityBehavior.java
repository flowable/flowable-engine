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

import static org.flowable.common.engine.impl.util.ExceptionUtil.sneakyThrow;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

import org.flowable.bpmn.model.MapExceptionEntry;
import org.flowable.common.engine.api.FlowableIllegalStateException;
import org.flowable.common.engine.api.delegate.Expression;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.logging.LoggingSessionConstants;
import org.flowable.engine.delegate.BpmnError;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.FutureJavaDelegate;
import org.flowable.engine.impl.bpmn.helper.ErrorPropagation;
import org.flowable.engine.impl.bpmn.helper.SkipExpressionUtil;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.delegate.ActivityBehavior;
import org.flowable.engine.impl.delegate.TriggerableActivityBehavior;
import org.flowable.engine.impl.delegate.invocation.FutureJavaDelegateInvocation;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.util.BpmnLoggingSessionUtil;
import org.flowable.engine.impl.util.CommandContextUtil;

/**
 * @author Filip Hrisafov
 */
public class ServiceTaskFutureJavaDelegateActivityBehavior extends TaskActivityBehavior implements ActivityBehavior {

    private static final long serialVersionUID = 1L;

    protected FutureJavaDelegate<?> futureJavaDelegate;
    protected Expression skipExpression;
    protected boolean triggerable;
    protected List<MapExceptionEntry> mapExceptions;

    protected ServiceTaskFutureJavaDelegateActivityBehavior() {
    }

    public ServiceTaskFutureJavaDelegateActivityBehavior(FutureJavaDelegate<?> futureJavaDelegate, boolean triggerable, Expression skipExpression, List<MapExceptionEntry> mapExceptions) {
        this.futureJavaDelegate = futureJavaDelegate;
        this.triggerable = triggerable;
        this.skipExpression = skipExpression;
        this.mapExceptions = mapExceptions;
    }

    @Override
    public void trigger(DelegateExecution execution, String signalName, Object signalData) {
        CommandContext commandContext = CommandContextUtil.getCommandContext();
        ProcessEngineConfigurationImpl processEngineConfiguration = CommandContextUtil.getProcessEngineConfiguration(commandContext);

        if (triggerable && futureJavaDelegate instanceof TriggerableActivityBehavior) {
            if (processEngineConfiguration.isLoggingSessionEnabled()) {
                BpmnLoggingSessionUtil.addLoggingData(LoggingSessionConstants.TYPE_SERVICE_TASK_BEFORE_TRIGGER,
                                "Triggering service task with java class " + futureJavaDelegate.getClass().getName(), execution);
            }

            ((TriggerableActivityBehavior) futureJavaDelegate).trigger(execution, signalName, signalData);

            if (processEngineConfiguration.isLoggingSessionEnabled()) {
                BpmnLoggingSessionUtil.addLoggingData(LoggingSessionConstants.TYPE_SERVICE_TASK_AFTER_TRIGGER,
                                "Triggered service task with java class " + futureJavaDelegate.getClass().getName(), execution);
            }

            leave(execution);

        } else {
            if (processEngineConfiguration.isLoggingSessionEnabled()) {
                if (!triggerable) {
                    BpmnLoggingSessionUtil.addLoggingData(LoggingSessionConstants.TYPE_SERVICE_TASK_WRONG_TRIGGER,
                                    "Service task with java class triggered but not triggerable " + futureJavaDelegate.getClass().getName(), execution);
                } else {
                    BpmnLoggingSessionUtil.addLoggingData(LoggingSessionConstants.TYPE_SERVICE_TASK_WRONG_TRIGGER,
                                    "Service task with java class triggered but not implementing TriggerableActivityBehavior " + futureJavaDelegate.getClass().getName(), execution);
                }
            }
        }
    }

    @Override
    public void execute(DelegateExecution execution) {
        CommandContext commandContext = CommandContextUtil.getCommandContext();
        String skipExpressionText = null;
        if (skipExpression != null) {
            skipExpressionText = skipExpression.getExpressionText();
        }
        boolean isSkipExpressionEnabled = SkipExpressionUtil.isSkipExpressionEnabled(skipExpressionText,
                        execution.getCurrentActivityId(), execution, commandContext);

        ProcessEngineConfigurationImpl processEngineConfiguration = CommandContextUtil.getProcessEngineConfiguration(commandContext);
        boolean loggingSessionEnabled = processEngineConfiguration.isLoggingSessionEnabled();
        if (!isSkipExpressionEnabled || !SkipExpressionUtil.shouldSkipFlowElement(skipExpressionText,
                        execution.getCurrentActivityId(), execution, commandContext)) {

            try {
                if (loggingSessionEnabled) {
                    BpmnLoggingSessionUtil.addLoggingData(LoggingSessionConstants.TYPE_SERVICE_TASK_ENTER,
                                    "Executing service task with java class " + futureJavaDelegate.getClass().getName(), execution);
                }

                FutureJavaDelegate<Object> futureJavaDelegate = (FutureJavaDelegate<Object>) this.futureJavaDelegate;

                FutureJavaDelegateInvocation invocation = new FutureJavaDelegateInvocation(futureJavaDelegate, execution,
                        processEngineConfiguration.getAsyncTaskInvoker());
                processEngineConfiguration.getDelegateInterceptor().handleInvocation(invocation);

                Object invocationResult = invocation.getInvocationResult();
                if (invocationResult instanceof CompletableFuture) {
                    CompletableFuture<Object> future = (CompletableFuture<Object>) invocationResult;

                    CommandContextUtil.getAgenda(commandContext).planFutureOperation(future, new FutureJavaDelegateCompleteAction(futureJavaDelegate, execution, loggingSessionEnabled));
                } else {
                    throw new FlowableIllegalStateException(
                            "Invocation result " + invocationResult + " from invocation " + invocation + " was not a CompletableFuture");
                }

            } catch (RuntimeException e) {
                if (loggingSessionEnabled) {
                    BpmnLoggingSessionUtil.addErrorLoggingData(LoggingSessionConstants.TYPE_SERVICE_TASK_EXCEPTION,
                                    "Service task with java class " + futureJavaDelegate.getClass().getName() + " threw exception " + e.getMessage(), e, execution);
                }

                throw e;
            }

        } else {
            if (loggingSessionEnabled) {
                BpmnLoggingSessionUtil.addLoggingData(LoggingSessionConstants.TYPE_SKIP_TASK, "Skipped service task " + execution.getCurrentActivityId() +
                                " with skip expression " + skipExpressionText, execution);
            }
            if (!triggerable) {
                leave(execution);
            }
        }

    }

    protected void handleException(Throwable throwable, DelegateExecution execution, boolean loggingSessionEnabled) {
        if (loggingSessionEnabled) {
            BpmnLoggingSessionUtil.addErrorLoggingData(LoggingSessionConstants.TYPE_SERVICE_TASK_EXCEPTION,
                    "Service task with java class " + futureJavaDelegate.getClass().getName() + " threw exception " + throwable.getMessage(), throwable, execution);
        }

        if (throwable instanceof BpmnError) {
            ErrorPropagation.propagateError((BpmnError) throwable, execution);
        } else if (throwable instanceof Exception) {
            if (!ErrorPropagation.mapException((Exception) throwable, (ExecutionEntity) execution, mapExceptions)) {
                sneakyThrow(throwable);
            }
        } else {
            sneakyThrow(throwable);
        }
    }

    protected class FutureJavaDelegateCompleteAction implements BiConsumer<Object, Throwable> {

        protected final FutureJavaDelegate<Object> delegateInstance;
        protected final DelegateExecution execution;
        protected final boolean loggingSessionEnabled;

        public FutureJavaDelegateCompleteAction(FutureJavaDelegate<Object> delegateInstance,
                DelegateExecution execution, boolean loggingSessionEnabled) {
            this.delegateInstance = delegateInstance;
            this.execution = execution;
            this.loggingSessionEnabled = loggingSessionEnabled;
        }

        @Override
        public void accept(Object value, Throwable throwable) {
            if (throwable == null) {
                try {
                    delegateInstance.afterExecution(execution, value);
                    if (loggingSessionEnabled) {
                        BpmnLoggingSessionUtil.addLoggingData(LoggingSessionConstants.TYPE_SERVICE_TASK_EXIT,
                                "Executed service task with java class " + futureJavaDelegate.getClass().getName(), execution);
                    }
                    if (!triggerable) {
                        leave(execution);
                    }
                } catch (Exception ex) {
                    handleException(ex, execution, loggingSessionEnabled);
                }
            } else {
                handleException(throwable, execution, loggingSessionEnabled);
            }
        }
    }
}
