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
package org.activiti.engine.impl.bpmn.parser.factory;

import org.activiti.engine.ActivitiException;
import org.activiti.engine.impl.bpmn.behavior.AbstractBpmnActivityBehavior;
import org.activiti.engine.impl.bpmn.behavior.BoundaryEventActivityBehavior;
import org.activiti.engine.impl.bpmn.behavior.BusinessRuleTaskActivityBehavior;
import org.activiti.engine.impl.bpmn.behavior.CallActivityBehavior;
import org.activiti.engine.impl.bpmn.behavior.CancelBoundaryEventActivityBehavior;
import org.activiti.engine.impl.bpmn.behavior.CancelEndEventActivityBehavior;
import org.activiti.engine.impl.bpmn.behavior.ErrorEndEventActivityBehavior;
import org.activiti.engine.impl.bpmn.behavior.EventBasedGatewayActivityBehavior;
import org.activiti.engine.impl.bpmn.behavior.EventSubProcessStartEventActivityBehavior;
import org.activiti.engine.impl.bpmn.behavior.ExclusiveGatewayActivityBehavior;
import org.activiti.engine.impl.bpmn.behavior.InclusiveGatewayActivityBehavior;
import org.activiti.engine.impl.bpmn.behavior.IntermediateCatchEventActivityBehavior;
import org.activiti.engine.impl.bpmn.behavior.IntermediateThrowCompensationEventActivityBehavior;
import org.activiti.engine.impl.bpmn.behavior.IntermediateThrowNoneEventActivityBehavior;
import org.activiti.engine.impl.bpmn.behavior.IntermediateThrowSignalEventActivityBehavior;
import org.activiti.engine.impl.bpmn.behavior.MailActivityBehavior;
import org.activiti.engine.impl.bpmn.behavior.ManualTaskActivityBehavior;
import org.activiti.engine.impl.bpmn.behavior.NoneEndEventActivityBehavior;
import org.activiti.engine.impl.bpmn.behavior.NoneStartEventActivityBehavior;
import org.activiti.engine.impl.bpmn.behavior.ParallelGatewayActivityBehavior;
import org.activiti.engine.impl.bpmn.behavior.ParallelMultiInstanceBehavior;
import org.activiti.engine.impl.bpmn.behavior.ReceiveTaskActivityBehavior;
import org.activiti.engine.impl.bpmn.behavior.ScriptTaskActivityBehavior;
import org.activiti.engine.impl.bpmn.behavior.SequentialMultiInstanceBehavior;
import org.activiti.engine.impl.bpmn.behavior.ServiceTaskDelegateExpressionActivityBehavior;
import org.activiti.engine.impl.bpmn.behavior.ServiceTaskExpressionActivityBehavior;
import org.activiti.engine.impl.bpmn.behavior.ShellActivityBehavior;
import org.activiti.engine.impl.bpmn.behavior.SubProcessActivityBehavior;
import org.activiti.engine.impl.bpmn.behavior.TaskActivityBehavior;
import org.activiti.engine.impl.bpmn.behavior.TerminateEndEventActivityBehavior;
import org.activiti.engine.impl.bpmn.behavior.TransactionActivityBehavior;
import org.activiti.engine.impl.bpmn.behavior.UserTaskActivityBehavior;
import org.activiti.engine.impl.bpmn.behavior.WebServiceActivityBehavior;
import org.activiti.engine.impl.bpmn.helper.ClassDelegate;
import org.activiti.engine.impl.bpmn.parser.CompensateEventDefinition;
import org.activiti.engine.impl.bpmn.parser.EventSubscriptionDeclaration;
import org.activiti.engine.impl.bpmn.parser.FieldDeclaration;
import org.activiti.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.activiti.engine.impl.pvm.process.ActivityImpl;
import org.activiti.engine.impl.scripting.ScriptingEngines;
import org.activiti.engine.impl.task.TaskDefinition;
import org.apache.commons.lang3.StringUtils;
import org.flowable.bpmn.model.BoundaryEvent;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.BusinessRuleTask;
import org.flowable.bpmn.model.CallActivity;
import org.flowable.bpmn.model.CancelEventDefinition;
import org.flowable.bpmn.model.EndEvent;
import org.flowable.bpmn.model.ErrorEventDefinition;
import org.flowable.bpmn.model.EventGateway;
import org.flowable.bpmn.model.ExclusiveGateway;
import org.flowable.bpmn.model.FieldExtension;
import org.flowable.bpmn.model.IOParameter;
import org.flowable.bpmn.model.InclusiveGateway;
import org.flowable.bpmn.model.IntermediateCatchEvent;
import org.flowable.bpmn.model.ManualTask;
import org.flowable.bpmn.model.MapExceptionEntry;
import org.flowable.bpmn.model.ParallelGateway;
import org.flowable.bpmn.model.ReceiveTask;
import org.flowable.bpmn.model.ScriptTask;
import org.flowable.bpmn.model.SendTask;
import org.flowable.bpmn.model.ServiceTask;
import org.flowable.bpmn.model.Signal;
import org.flowable.bpmn.model.StartEvent;
import org.flowable.bpmn.model.SubProcess;
import org.flowable.bpmn.model.Task;
import org.flowable.bpmn.model.TaskWithFieldExtensions;
import org.flowable.bpmn.model.ThrowEvent;
import org.flowable.bpmn.model.Transaction;
import org.flowable.bpmn.model.UserTask;
import org.flowable.common.engine.api.delegate.Expression;
import org.flowable.engine.delegate.BusinessRuleTaskDelegate;
import org.flowable.engine.impl.bpmn.data.SimpleDataInputAssociation;
import org.flowable.engine.impl.bpmn.webservice.MessageImplicitDataOutputAssociation;
import org.flowable.engine.impl.delegate.ActivityBehavior;

import java.util.List;

/**
 * Default implementation of the {@link ActivityBehaviorFactory}. Used when no custom {@link ActivityBehaviorFactory} is injected on the {@link ProcessEngineConfigurationImpl}.
 *
 * @author Joram Barrez
 */
public class DefaultActivityBehaviorFactory extends AbstractBehaviorFactory implements ActivityBehaviorFactory {

    // Start event
    public static final String EXCEPTION_MAP_FIELD = "mapExceptions";

    @Override
    public NoneStartEventActivityBehavior createNoneStartEventActivityBehavior(StartEvent startEvent) {
        return new NoneStartEventActivityBehavior();
    }

    @Override
    public EventSubProcessStartEventActivityBehavior createEventSubProcessStartEventActivityBehavior(StartEvent startEvent, String activityId) {
        return new EventSubProcessStartEventActivityBehavior(activityId);
    }

    // Task

    @Override
    public TaskActivityBehavior createTaskActivityBehavior(Task task) {
        return new TaskActivityBehavior();
    }

    @Override
    public ManualTaskActivityBehavior createManualTaskActivityBehavior(ManualTask manualTask) {
        return new ManualTaskActivityBehavior();
    }

    @Override
    public ReceiveTaskActivityBehavior createReceiveTaskActivityBehavior(ReceiveTask receiveTask) {
        return new ReceiveTaskActivityBehavior();
    }

    @Override
    public UserTaskActivityBehavior createUserTaskActivityBehavior(UserTask userTask, TaskDefinition taskDefinition) {
        return new UserTaskActivityBehavior(userTask.getId(), taskDefinition);
    }

    // Service task

    @Override
    public ClassDelegate createClassDelegateServiceTask(ServiceTask serviceTask) {
        Expression skipExpression;
        if (StringUtils.isNotEmpty(serviceTask.getSkipExpression())) {
            skipExpression = expressionManager.createExpression(serviceTask.getSkipExpression());
        } else {
            skipExpression = null;
        }
        return new ClassDelegate(serviceTask.getId(), serviceTask.getImplementation(), createFieldDeclarations(serviceTask.getFieldExtensions()),
                skipExpression, serviceTask.getMapExceptions());
    }

    @Override
    public ServiceTaskDelegateExpressionActivityBehavior createServiceTaskDelegateExpressionActivityBehavior(ServiceTask serviceTask) {
        Expression delegateExpression = expressionManager.createExpression(serviceTask.getImplementation());
        Expression skipExpression;
        if (StringUtils.isNotEmpty(serviceTask.getSkipExpression())) {
            skipExpression = expressionManager.createExpression(serviceTask.getSkipExpression());
        } else {
            skipExpression = null;
        }
        return new ServiceTaskDelegateExpressionActivityBehavior(serviceTask.getId(), delegateExpression,
                skipExpression, createFieldDeclarations(serviceTask.getFieldExtensions()), serviceTask.getMapExceptions());
    }

    @Override
    public ServiceTaskExpressionActivityBehavior createServiceTaskExpressionActivityBehavior(ServiceTask serviceTask) {
        Expression expression = expressionManager.createExpression(serviceTask.getImplementation());
        Expression skipExpression;
        if (StringUtils.isNotEmpty(serviceTask.getSkipExpression())) {
            skipExpression = expressionManager.createExpression(serviceTask.getSkipExpression());
        } else {
            skipExpression = null;
        }
        return new ServiceTaskExpressionActivityBehavior(serviceTask.getId(), expression, skipExpression,
                serviceTask.getResultVariableName(), serviceTask.getMapExceptions());
    }

    @Override
    public WebServiceActivityBehavior createWebServiceActivityBehavior(ServiceTask serviceTask) {
        return new WebServiceActivityBehavior();
    }

    @Override
    public WebServiceActivityBehavior createWebServiceActivityBehavior(SendTask sendTask) {
        return new WebServiceActivityBehavior();
    }

    @Override
    public MailActivityBehavior createMailActivityBehavior(ServiceTask serviceTask) {
        return createMailActivityBehavior(serviceTask.getId(), serviceTask.getFieldExtensions());
    }

    @Override
    public MailActivityBehavior createMailActivityBehavior(SendTask sendTask) {
        return createMailActivityBehavior(sendTask.getId(), sendTask.getFieldExtensions());
    }

    protected MailActivityBehavior createMailActivityBehavior(String taskId, List<FieldExtension> fields) {
        List<FieldDeclaration> fieldDeclarations = createFieldDeclarations(fields);
        return (MailActivityBehavior) ClassDelegate.defaultInstantiateDelegate(MailActivityBehavior.class, fieldDeclarations);
    }

    // We do not want a hard dependency on Mule, hence we return ActivityBehavior and instantiate
    // the delegate instance using a string instead of the Class itself.
    @Override
    public ActivityBehavior createMuleActivityBehavior(ServiceTask serviceTask, BpmnModel bpmnModel) {
        return createMuleActivityBehavior(serviceTask, serviceTask.getFieldExtensions(), bpmnModel);
    }

    @Override
    public ActivityBehavior createMuleActivityBehavior(SendTask sendTask, BpmnModel bpmnModel) {
        return createMuleActivityBehavior(sendTask, sendTask.getFieldExtensions(), bpmnModel);
    }

    protected ActivityBehavior createMuleActivityBehavior(TaskWithFieldExtensions task, List<FieldExtension> fieldExtensions, BpmnModel bpmnModel) {
        try {

            Class<?> theClass = Class.forName("org.flowable.mule.MuleSendActivityBehavior");
            List<FieldDeclaration> fieldDeclarations = createFieldDeclarations(fieldExtensions);
            return (ActivityBehavior) ClassDelegate.defaultInstantiateDelegate(theClass, fieldDeclarations);

        } catch (ClassNotFoundException e) {
            throw new ActivitiException("Could not find org.flowable.mule.MuleSendActivityBehavior: ", e);
        }
    }

    // We do not want a hard dependency on Camel, hence we return ActivityBehavior and instantiate
    // the delegate instance using a string instead of the Class itself.
    @Override
    public ActivityBehavior createCamelActivityBehavior(ServiceTask serviceTask, BpmnModel bpmnModel) {
        return createCamelActivityBehavior(serviceTask, serviceTask.getFieldExtensions(), bpmnModel);
    }

    @Override
    public ActivityBehavior createCamelActivityBehavior(SendTask sendTask, BpmnModel bpmnModel) {
        return createCamelActivityBehavior(sendTask, sendTask.getFieldExtensions(), bpmnModel);
    }

    protected ActivityBehavior createCamelActivityBehavior(TaskWithFieldExtensions task, List<FieldExtension> fieldExtensions, BpmnModel bpmnModel) {
        try {
            Class<?> theClass = null;
            FieldExtension behaviorExtension = null;
            for (FieldExtension fieldExtension : fieldExtensions) {
                if ("camelBehaviorClass".equals(fieldExtension.getFieldName()) && StringUtils.isNotEmpty(fieldExtension.getStringValue())) {
                    theClass = Class.forName(fieldExtension.getStringValue());
                    behaviorExtension = fieldExtension;
                    break;
                }
            }

            if (behaviorExtension != null) {
                fieldExtensions.remove(behaviorExtension);
            }

            if (theClass == null) {
                // Default Camel behavior class
                theClass = Class.forName("org.flowable.camel.impl.CamelBehaviorDefaultImpl");
            }

            List<FieldDeclaration> fieldDeclarations = createFieldDeclarations(fieldExtensions);
            addExceptionMapAsFieldDeclaration(fieldDeclarations, task.getMapExceptions());
            return (ActivityBehavior) ClassDelegate.defaultInstantiateDelegate(theClass, fieldDeclarations);

        } catch (ClassNotFoundException e) {
            throw new ActivitiException("Could not find org.flowable.camel.impl.CamelBehaviorDefaultImpl: ", e);
        }
    }

    private void addExceptionMapAsFieldDeclaration(List<FieldDeclaration> fieldDeclarations, List<MapExceptionEntry> mapExceptions) {
        FieldDeclaration exceptionMapsFieldDeclaration = new FieldDeclaration(EXCEPTION_MAP_FIELD, mapExceptions.getClass().toString(), mapExceptions);
        fieldDeclarations.add(exceptionMapsFieldDeclaration);

    }

    @Override
    public ShellActivityBehavior createShellActivityBehavior(ServiceTask serviceTask) {
        List<FieldDeclaration> fieldDeclarations = createFieldDeclarations(serviceTask.getFieldExtensions());
        return (ShellActivityBehavior) ClassDelegate.defaultInstantiateDelegate(ShellActivityBehavior.class, fieldDeclarations);
    }

    @Override
    public ActivityBehavior createBusinessRuleTaskActivityBehavior(BusinessRuleTask businessRuleTask) {
        BusinessRuleTaskDelegate ruleActivity = null;
        if (StringUtils.isNotEmpty(businessRuleTask.getClassName())) {
            try {
                Class<?> clazz = Class.forName(businessRuleTask.getClassName());
                ruleActivity = (BusinessRuleTaskDelegate) clazz.newInstance();
            } catch (Exception e) {
                throw new ActivitiException("Could not instantiate businessRuleTask (id:" + businessRuleTask.getId() + ") class: " +
                        businessRuleTask.getClassName(), e);
            }
        } else {
            ruleActivity = new BusinessRuleTaskActivityBehavior();
        }

        for (String ruleVariableInputObject : businessRuleTask.getInputVariables()) {
            ruleActivity.addRuleVariableInputIdExpression(expressionManager.createExpression(ruleVariableInputObject.trim()));
        }

        for (String rule : businessRuleTask.getRuleNames()) {
            ruleActivity.addRuleIdExpression(expressionManager.createExpression(rule.trim()));
        }

        ruleActivity.setExclude(businessRuleTask.isExclude());

        if (businessRuleTask.getResultVariableName() != null && businessRuleTask.getResultVariableName().length() > 0) {
            ruleActivity.setResultVariable(businessRuleTask.getResultVariableName());
        } else {
            ruleActivity.setResultVariable("org.activiti.engine.rules.OUTPUT");
        }

        return ruleActivity;
    }

    // Script task

    @Override
    public ScriptTaskActivityBehavior createScriptTaskActivityBehavior(ScriptTask scriptTask) {
        String language = scriptTask.getScriptFormat();
        if (language == null) {
            language = ScriptingEngines.DEFAULT_SCRIPTING_LANGUAGE;
        }
        return new ScriptTaskActivityBehavior(scriptTask.getId(), scriptTask.getScript(), language, scriptTask.getResultVariable(), scriptTask.isAutoStoreVariables());
    }

    // Gateways

    @Override
    public ExclusiveGatewayActivityBehavior createExclusiveGatewayActivityBehavior(ExclusiveGateway exclusiveGateway) {
        return new ExclusiveGatewayActivityBehavior();
    }

    @Override
    public ParallelGatewayActivityBehavior createParallelGatewayActivityBehavior(ParallelGateway parallelGateway) {
        return new ParallelGatewayActivityBehavior();
    }

    @Override
    public InclusiveGatewayActivityBehavior createInclusiveGatewayActivityBehavior(InclusiveGateway inclusiveGateway) {
        return new InclusiveGatewayActivityBehavior();
    }

    @Override
    public EventBasedGatewayActivityBehavior createEventBasedGatewayActivityBehavior(EventGateway eventGateway) {
        return new EventBasedGatewayActivityBehavior();
    }

    // Multi Instance

    @Override
    public SequentialMultiInstanceBehavior createSequentialMultiInstanceBehavior(ActivityImpl activity, AbstractBpmnActivityBehavior innerActivityBehavior) {
        return new SequentialMultiInstanceBehavior(activity, innerActivityBehavior);
    }

    @Override
    public ParallelMultiInstanceBehavior createParallelMultiInstanceBehavior(ActivityImpl activity, AbstractBpmnActivityBehavior innerActivityBehavior) {
        return new ParallelMultiInstanceBehavior(activity, innerActivityBehavior);
    }

    // Subprocess

    @Override
    public SubProcessActivityBehavior createSubprocActivityBehavior(SubProcess subProcess) {
        return new SubProcessActivityBehavior();
    }

    // Call activity

    @Override
    public CallActivityBehavior createCallActivityBehavior(CallActivity callActivity) {
        String expressionRegex = "\\$+\\{+.+\\}";

        CallActivityBehavior callActivityBehaviour = null;
        if (StringUtils.isNotEmpty(callActivity.getCalledElement()) && callActivity.getCalledElement().matches(expressionRegex)) {
            callActivityBehaviour = new CallActivityBehavior(expressionManager.createExpression(callActivity.getCalledElement()), callActivity.getMapExceptions());
        } else {
            callActivityBehaviour = new CallActivityBehavior(callActivity.getCalledElement(), callActivity.getMapExceptions());
        }

        callActivityBehaviour.setInheritVariables(callActivity.isInheritVariables());
        callActivityBehaviour.setSameDeployment(callActivity.isSameDeployment());

        for (IOParameter ioParameter : callActivity.getInParameters()) {
            if (StringUtils.isNotEmpty(ioParameter.getSourceExpression())) {
                Expression expression = expressionManager.createExpression(ioParameter.getSourceExpression().trim());
                callActivityBehaviour.addDataInputAssociation(new SimpleDataInputAssociation(expression, ioParameter.getTarget()));
            } else {
                callActivityBehaviour.addDataInputAssociation(new SimpleDataInputAssociation(ioParameter.getSource(), ioParameter.getTarget()));
            }
        }

        for (IOParameter ioParameter : callActivity.getOutParameters()) {
            if (StringUtils.isNotEmpty(ioParameter.getSourceExpression())) {
                Expression expression = expressionManager.createExpression(ioParameter.getSourceExpression().trim());
                callActivityBehaviour.addDataOutputAssociation(new MessageImplicitDataOutputAssociation(ioParameter.getTarget(), expression));
            } else {
                callActivityBehaviour.addDataOutputAssociation(new MessageImplicitDataOutputAssociation(ioParameter.getTarget(), ioParameter.getSource()));
            }
        }

        return callActivityBehaviour;
    }

    // Transaction

    @Override
    public TransactionActivityBehavior createTransactionActivityBehavior(Transaction transaction) {
        return new TransactionActivityBehavior();
    }

    // Intermediate Events

    @Override
    public IntermediateCatchEventActivityBehavior createIntermediateCatchEventActivityBehavior(IntermediateCatchEvent intermediateCatchEvent) {
        return new IntermediateCatchEventActivityBehavior();
    }

    @Override
    public IntermediateThrowNoneEventActivityBehavior createIntermediateThrowNoneEventActivityBehavior(ThrowEvent throwEvent) {
        return new IntermediateThrowNoneEventActivityBehavior();
    }

    @Override
    public IntermediateThrowSignalEventActivityBehavior createIntermediateThrowSignalEventActivityBehavior(ThrowEvent throwEvent,
            Signal signal, EventSubscriptionDeclaration eventSubscriptionDeclaration) {
        return new IntermediateThrowSignalEventActivityBehavior(throwEvent, signal, eventSubscriptionDeclaration);
    }

    @Override
    public IntermediateThrowCompensationEventActivityBehavior createIntermediateThrowCompensationEventActivityBehavior(ThrowEvent throwEvent,
            CompensateEventDefinition compensateEventDefinition) {
        return new IntermediateThrowCompensationEventActivityBehavior(compensateEventDefinition);
    }

    // End events

    @Override
    public NoneEndEventActivityBehavior createNoneEndEventActivityBehavior(EndEvent endEvent) {
        return new NoneEndEventActivityBehavior();
    }

    @Override
    public ErrorEndEventActivityBehavior createErrorEndEventActivityBehavior(EndEvent endEvent, ErrorEventDefinition errorEventDefinition) {
        return new ErrorEndEventActivityBehavior(errorEventDefinition.getErrorCode());
    }

    @Override
    public CancelEndEventActivityBehavior createCancelEndEventActivityBehavior(EndEvent endEvent) {
        return new CancelEndEventActivityBehavior();
    }

    @Override
    public TerminateEndEventActivityBehavior createTerminateEndEventActivityBehavior(EndEvent endEvent) {
        return new TerminateEndEventActivityBehavior(endEvent);
    }

    // Boundary Events

    @Override
    public BoundaryEventActivityBehavior createBoundaryEventActivityBehavior(BoundaryEvent boundaryEvent, boolean interrupting, ActivityImpl activity) {
        return new BoundaryEventActivityBehavior(interrupting, activity.getId());
    }

    @Override
    public CancelBoundaryEventActivityBehavior createCancelBoundaryEventActivityBehavior(CancelEventDefinition cancelEventDefinition) {
        return new CancelBoundaryEventActivityBehavior();
    }

}
