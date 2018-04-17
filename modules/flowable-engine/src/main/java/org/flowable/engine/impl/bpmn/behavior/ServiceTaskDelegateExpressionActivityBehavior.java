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

import org.apache.commons.lang3.StringUtils;
import org.flowable.bpmn.model.MapExceptionEntry;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.delegate.Expression;
import org.flowable.engine.DynamicBpmnConstants;
import org.flowable.engine.delegate.BpmnError;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.flowable.engine.impl.bpmn.helper.DelegateExpressionUtil;
import org.flowable.engine.impl.bpmn.helper.ErrorPropagation;
import org.flowable.engine.impl.bpmn.helper.SkipExpressionUtil;
import org.flowable.engine.impl.bpmn.parser.FieldDeclaration;
import org.flowable.engine.impl.context.BpmnOverrideContext;
import org.flowable.engine.impl.delegate.ActivityBehavior;
import org.flowable.engine.impl.delegate.ActivityBehaviorInvocation;
import org.flowable.engine.impl.delegate.TriggerableActivityBehavior;
import org.flowable.engine.impl.delegate.invocation.JavaDelegateInvocation;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
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
        if (triggerable && delegate instanceof TriggerableActivityBehavior) {
            ((TriggerableActivityBehavior) delegate).trigger(execution, signalName, signalData);
        }
        leave(execution);
    }

    @Override
    public void execute(DelegateExecution execution) {

        try {
            boolean isSkipExpressionEnabled = SkipExpressionUtil.isSkipExpressionEnabled(execution, skipExpression);
            if (!isSkipExpressionEnabled || (isSkipExpressionEnabled && !SkipExpressionUtil.shouldSkipFlowElement(execution, skipExpression))) {

                if (CommandContextUtil.getProcessEngineConfiguration().isEnableProcessDefinitionInfoCache()) {
                    ObjectNode taskElementProperties = BpmnOverrideContext.getBpmnOverrideElementProperties(serviceTaskId, execution.getProcessDefinitionId());
                    if (taskElementProperties != null && taskElementProperties.has(DynamicBpmnConstants.SERVICE_TASK_DELEGATE_EXPRESSION)) {
                        String overrideExpression = taskElementProperties.get(DynamicBpmnConstants.SERVICE_TASK_DELEGATE_EXPRESSION).asText();
                        if (StringUtils.isNotEmpty(overrideExpression) && !overrideExpression.equals(expression.getExpressionText())) {
                            expression = CommandContextUtil.getProcessEngineConfiguration().getExpressionManager().createExpression(overrideExpression);
                        }
                    }
                }

                Object delegate = DelegateExpressionUtil.resolveDelegateExpression(expression, execution, fieldDeclarations);
                if (delegate instanceof ActivityBehavior) {

                    if (delegate instanceof AbstractBpmnActivityBehavior) {
                        ((AbstractBpmnActivityBehavior) delegate).setMultiInstanceActivityBehavior(getMultiInstanceActivityBehavior());
                    }

                    CommandContextUtil.getProcessEngineConfiguration().getDelegateInterceptor().handleInvocation(new ActivityBehaviorInvocation((ActivityBehavior) delegate, execution));

                } else if (delegate instanceof JavaDelegate) {
                    CommandContextUtil.getProcessEngineConfiguration().getDelegateInterceptor().handleInvocation(new JavaDelegateInvocation((JavaDelegate) delegate, execution));

                    if (!triggerable) {
                        leave(execution);
                    }
                } else {
                    throw new FlowableIllegalArgumentException("Delegate expression " + expression + " did neither resolve to an implementation of " + ActivityBehavior.class + " nor " + JavaDelegate.class);
                }
                
            } else {
                leave(execution);
            }
        } catch (Exception exc) {

            Throwable cause = exc;
            BpmnError error = null;
            while (cause != null) {
                if (cause instanceof BpmnError) {
                    error = (BpmnError) cause;
                    break;

                } else if (cause instanceof RuntimeException) {
                    if (ErrorPropagation.mapException((RuntimeException) cause, (ExecutionEntity) execution, mapExceptions)) {
                        return;
                    }
                }
                cause = cause.getCause();
            }

            if (error != null) {
                ErrorPropagation.propagateError(error, execution);
            } else {
                throw new FlowableException(exc.getMessage(), exc);
            }

        }
    }
}
