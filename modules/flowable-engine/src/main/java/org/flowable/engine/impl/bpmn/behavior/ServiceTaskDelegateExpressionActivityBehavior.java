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
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.FlowableIllegalStateException;
import org.flowable.common.engine.api.delegate.Expression;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.logging.LoggingSessionConstants;
import org.flowable.engine.DynamicBpmnConstants;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.FutureJavaDelegate;
import org.flowable.engine.delegate.JavaDelegate;
import org.flowable.engine.impl.bpmn.helper.DelegateExpressionUtil;
import org.flowable.engine.impl.bpmn.helper.ErrorPropagation;
import org.flowable.engine.impl.bpmn.helper.SkipExpressionUtil;
import org.flowable.engine.impl.bpmn.parser.FieldDeclaration;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.context.BpmnOverrideContext;
import org.flowable.engine.impl.delegate.ActivityBehavior;
import org.flowable.engine.impl.delegate.ActivityBehaviorInvocation;
import org.flowable.engine.impl.delegate.TriggerableActivityBehavior;
import org.flowable.engine.impl.delegate.invocation.DelegateInvocation;
import org.flowable.engine.impl.delegate.invocation.FutureJavaDelegateInvocation;
import org.flowable.engine.impl.delegate.invocation.JavaDelegateInvocation;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.util.BpmnLoggingSessionUtil;
import org.flowable.engine.impl.util.CommandContextUtil;

import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * {@link ActivityBehavior} used when 'delegateExpression' is used for a serviceTask.
 *
 * @author Joram Barrez
 * @author Josh Long
 * @author Slawomir Wojtasiak (Patch for ACT-1159)
 * @author Falko Menge
 */
public class ServiceTaskDelegateExpressionActivityBehavior extends TaskActivityBehavior {

    private static final long serialVersionUID = 1L;

    protected String serviceTaskId;
    protected Expression expression;
    protected Expression skipExpression;
    protected List<FieldDeclaration> fieldDeclarations;
    protected List<MapExceptionEntry> mapExceptions;
    protected boolean triggerable;

    public ServiceTaskDelegateExpressionActivityBehavior(String serviceTaskId, Expression expression, Expression skipExpression,
            List<FieldDeclaration> fieldDeclarations, List<MapExceptionEntry> mapExceptions, boolean triggerable) {
        this.serviceTaskId = serviceTaskId;
        this.expression = expression;
        this.skipExpression = skipExpression;
        this.fieldDeclarations = fieldDeclarations;
        this.mapExceptions = mapExceptions;
        this.triggerable = triggerable;
    }

    @Override
    public void trigger(DelegateExecution execution, String signalName, Object signalData) {
        Object delegate = DelegateExpressionUtil.resolveDelegateExpression(expression, execution, fieldDeclarations);
        ProcessEngineConfigurationImpl processEngineConfiguration = CommandContextUtil.getProcessEngineConfiguration();
        boolean loggingSessionEnabled = processEngineConfiguration.isLoggingSessionEnabled();
        if (triggerable && delegate instanceof TriggerableActivityBehavior) {
            if (loggingSessionEnabled) {
                BpmnLoggingSessionUtil.addLoggingData(LoggingSessionConstants.TYPE_SERVICE_TASK_BEFORE_TRIGGER,
                        "Triggering service task with delegate " + delegate, execution);
            }

            ((TriggerableActivityBehavior) delegate).trigger(execution, signalName, signalData);

            if (loggingSessionEnabled) {
                BpmnLoggingSessionUtil.addLoggingData(LoggingSessionConstants.TYPE_SERVICE_TASK_AFTER_TRIGGER,
                        "Triggered service task with delegate " + delegate, execution);
            }

        } else if (loggingSessionEnabled) {
            if (!triggerable) {
                BpmnLoggingSessionUtil.addLoggingData(LoggingSessionConstants.TYPE_SERVICE_TASK_WRONG_TRIGGER,
                        "Service task with delegate expression triggered but not triggerable " + delegate, execution);
            } else {
                BpmnLoggingSessionUtil.addLoggingData(LoggingSessionConstants.TYPE_SERVICE_TASK_WRONG_TRIGGER,
                        "Service task with delegate expression triggered but not implementing TriggerableActivityBehavior " + delegate, execution);
            }

        }
        leave(execution);
    }

    @Override
    public void execute(DelegateExecution execution) {

        CommandContext commandContext = CommandContextUtil.getCommandContext();
        ProcessEngineConfigurationImpl processEngineConfiguration = CommandContextUtil.getProcessEngineConfiguration(commandContext);
        boolean loggingSessionEnabled = processEngineConfiguration.isLoggingSessionEnabled();
        try {

            String skipExpressionText = null;
            if (skipExpression != null) {
                skipExpressionText = skipExpression.getExpressionText();
            }
            boolean isSkipExpressionEnabled = SkipExpressionUtil.isSkipExpressionEnabled(skipExpressionText, serviceTaskId, execution, commandContext);
            if (!isSkipExpressionEnabled || !SkipExpressionUtil.shouldSkipFlowElement(skipExpressionText, serviceTaskId, execution, commandContext)) {


                if (processEngineConfiguration.isEnableProcessDefinitionInfoCache()) {
                    ObjectNode taskElementProperties = BpmnOverrideContext.getBpmnOverrideElementProperties(serviceTaskId, execution.getProcessDefinitionId());
                    if (taskElementProperties != null && taskElementProperties.has(DynamicBpmnConstants.SERVICE_TASK_DELEGATE_EXPRESSION)) {
                        String overrideExpression = taskElementProperties.get(DynamicBpmnConstants.SERVICE_TASK_DELEGATE_EXPRESSION).asText();
                        if (StringUtils.isNotEmpty(overrideExpression) && !overrideExpression.equals(expression.getExpressionText())) {
                            expression = processEngineConfiguration.getExpressionManager().createExpression(overrideExpression);
                        }
                    }
                }

                Object delegate = DelegateExpressionUtil.resolveDelegateExpression(expression, execution, fieldDeclarations);

                if (loggingSessionEnabled) {
                    BpmnLoggingSessionUtil.addLoggingData(LoggingSessionConstants.TYPE_SERVICE_TASK_ENTER,
                            "Executing service task with delegate " + delegate, execution);
                }

                if (delegate instanceof ActivityBehavior) {

                    if (delegate instanceof AbstractBpmnActivityBehavior) {
                        ((AbstractBpmnActivityBehavior) delegate).setMultiInstanceActivityBehavior(getMultiInstanceActivityBehavior());
                    }

                    processEngineConfiguration
                            .getDelegateInterceptor().handleInvocation(new ActivityBehaviorInvocation((ActivityBehavior) delegate, execution));

                    if (loggingSessionEnabled) {
                        BpmnLoggingSessionUtil.addLoggingData(LoggingSessionConstants.TYPE_SERVICE_TASK_EXIT,
                                "Executed service task with delegate " + delegate, execution);
                    }

                } else if (delegate instanceof JavaDelegate) {
                    processEngineConfiguration
                            .getDelegateInterceptor().handleInvocation(new JavaDelegateInvocation((JavaDelegate) delegate, execution));

                    if (!triggerable) {
                        leave(execution);
                    }

                    if (loggingSessionEnabled) {
                        BpmnLoggingSessionUtil.addLoggingData(LoggingSessionConstants.TYPE_SERVICE_TASK_EXIT,
                                "Executed service task with delegate " + delegate, execution);
                    }
                } else if (delegate instanceof FutureJavaDelegate) {
                    FutureJavaDelegate<Object> futureJavaDelegate = (FutureJavaDelegate<Object>) delegate;
                    DelegateInvocation invocation = new FutureJavaDelegateInvocation(futureJavaDelegate, execution,
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


                } else {
                    throw new FlowableIllegalArgumentException("Delegate expression " + expression + " did neither resolve to an implementation of " + ActivityBehavior.class + " nor " + JavaDelegate.class);
                }

            } else {
                if (loggingSessionEnabled) {
                    BpmnLoggingSessionUtil.addLoggingData(LoggingSessionConstants.TYPE_SKIP_TASK, "Skipped service task " + execution.getCurrentActivityId() +
                            " with skip expression " + skipExpressionText, execution);
                }
                leave(execution);
            }
        } catch (Exception exc) {

            handleException(exc, execution, loggingSessionEnabled);

        }
    }

    protected void handleException(Throwable exc, DelegateExecution execution, boolean loggingSessionEnabled) {
        if (loggingSessionEnabled) {
            BpmnLoggingSessionUtil.addErrorLoggingData(LoggingSessionConstants.TYPE_SERVICE_TASK_EXCEPTION,
                    "Service task with delegate expression " + expression + " threw exception " + exc.getMessage(), exc, execution);
        }

        ErrorPropagation.handleException(exc, (ExecutionEntity) execution, mapExceptions);
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
                    if (!triggerable) {
                        leave(execution);
                    }
                } catch (Exception ex) {
                    handleException(ex, execution, loggingSessionEnabled);
                }

                if (loggingSessionEnabled) {
                    BpmnLoggingSessionUtil.addLoggingData(LoggingSessionConstants.TYPE_SERVICE_TASK_EXIT,
                            "Executed service task with delegate " + delegateInstance, execution);
                }
            } else {
                handleException(throwable, execution, loggingSessionEnabled);
            }
        }
    }
}
