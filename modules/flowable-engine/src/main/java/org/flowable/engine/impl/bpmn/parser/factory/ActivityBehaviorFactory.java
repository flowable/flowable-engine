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
package org.flowable.engine.impl.bpmn.parser.factory;

import org.flowable.bpmn.model.Activity;
import org.flowable.bpmn.model.BoundaryEvent;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.BusinessRuleTask;
import org.flowable.bpmn.model.CallActivity;
import org.flowable.bpmn.model.CancelEventDefinition;
import org.flowable.bpmn.model.CompensateEventDefinition;
import org.flowable.bpmn.model.EndEvent;
import org.flowable.bpmn.model.ErrorEventDefinition;
import org.flowable.bpmn.model.EventGateway;
import org.flowable.bpmn.model.EventSubProcess;
import org.flowable.bpmn.model.ExclusiveGateway;
import org.flowable.bpmn.model.InclusiveGateway;
import org.flowable.bpmn.model.IntermediateCatchEvent;
import org.flowable.bpmn.model.ManualTask;
import org.flowable.bpmn.model.MessageEventDefinition;
import org.flowable.bpmn.model.ParallelGateway;
import org.flowable.bpmn.model.ReceiveTask;
import org.flowable.bpmn.model.ScriptTask;
import org.flowable.bpmn.model.SendTask;
import org.flowable.bpmn.model.ServiceTask;
import org.flowable.bpmn.model.Signal;
import org.flowable.bpmn.model.SignalEventDefinition;
import org.flowable.bpmn.model.StartEvent;
import org.flowable.bpmn.model.SubProcess;
import org.flowable.bpmn.model.Task;
import org.flowable.bpmn.model.ThrowEvent;
import org.flowable.bpmn.model.TimerEventDefinition;
import org.flowable.bpmn.model.Transaction;
import org.flowable.bpmn.model.UserTask;
import org.flowable.engine.impl.bpmn.behavior.AbstractBpmnActivityBehavior;
import org.flowable.engine.impl.bpmn.behavior.AdhocSubProcessActivityBehavior;
import org.flowable.engine.impl.bpmn.behavior.BoundaryCancelEventActivityBehavior;
import org.flowable.engine.impl.bpmn.behavior.BoundaryCompensateEventActivityBehavior;
import org.flowable.engine.impl.bpmn.behavior.BoundaryEventActivityBehavior;
import org.flowable.engine.impl.bpmn.behavior.BoundaryMessageEventActivityBehavior;
import org.flowable.engine.impl.bpmn.behavior.BoundarySignalEventActivityBehavior;
import org.flowable.engine.impl.bpmn.behavior.BoundaryTimerEventActivityBehavior;
import org.flowable.engine.impl.bpmn.behavior.CallActivityBehavior;
import org.flowable.engine.impl.bpmn.behavior.CancelEndEventActivityBehavior;
import org.flowable.engine.impl.bpmn.behavior.ErrorEndEventActivityBehavior;
import org.flowable.engine.impl.bpmn.behavior.EventBasedGatewayActivityBehavior;
import org.flowable.engine.impl.bpmn.behavior.EventSubProcessActivityBehavior;
import org.flowable.engine.impl.bpmn.behavior.EventSubProcessErrorStartEventActivityBehavior;
import org.flowable.engine.impl.bpmn.behavior.EventSubProcessMessageStartEventActivityBehavior;
import org.flowable.engine.impl.bpmn.behavior.EventSubProcessSignalStartEventActivityBehavior;
import org.flowable.engine.impl.bpmn.behavior.EventSubProcessTimerStartEventActivityBehavior;
import org.flowable.engine.impl.bpmn.behavior.ExclusiveGatewayActivityBehavior;
import org.flowable.engine.impl.bpmn.behavior.InclusiveGatewayActivityBehavior;
import org.flowable.engine.impl.bpmn.behavior.IntermediateCatchEventActivityBehavior;
import org.flowable.engine.impl.bpmn.behavior.IntermediateCatchMessageEventActivityBehavior;
import org.flowable.engine.impl.bpmn.behavior.IntermediateCatchSignalEventActivityBehavior;
import org.flowable.engine.impl.bpmn.behavior.IntermediateCatchTimerEventActivityBehavior;
import org.flowable.engine.impl.bpmn.behavior.IntermediateThrowCompensationEventActivityBehavior;
import org.flowable.engine.impl.bpmn.behavior.IntermediateThrowNoneEventActivityBehavior;
import org.flowable.engine.impl.bpmn.behavior.IntermediateThrowSignalEventActivityBehavior;
import org.flowable.engine.impl.bpmn.behavior.MailActivityBehavior;
import org.flowable.engine.impl.bpmn.behavior.ManualTaskActivityBehavior;
import org.flowable.engine.impl.bpmn.behavior.NoneEndEventActivityBehavior;
import org.flowable.engine.impl.bpmn.behavior.NoneStartEventActivityBehavior;
import org.flowable.engine.impl.bpmn.behavior.ParallelGatewayActivityBehavior;
import org.flowable.engine.impl.bpmn.behavior.ParallelMultiInstanceBehavior;
import org.flowable.engine.impl.bpmn.behavior.ReceiveTaskActivityBehavior;
import org.flowable.engine.impl.bpmn.behavior.ScriptTaskActivityBehavior;
import org.flowable.engine.impl.bpmn.behavior.SequentialMultiInstanceBehavior;
import org.flowable.engine.impl.bpmn.behavior.ServiceTaskDelegateExpressionActivityBehavior;
import org.flowable.engine.impl.bpmn.behavior.ServiceTaskExpressionActivityBehavior;
import org.flowable.engine.impl.bpmn.behavior.ShellActivityBehavior;
import org.flowable.engine.impl.bpmn.behavior.SubProcessActivityBehavior;
import org.flowable.engine.impl.bpmn.behavior.TaskActivityBehavior;
import org.flowable.engine.impl.bpmn.behavior.TerminateEndEventActivityBehavior;
import org.flowable.engine.impl.bpmn.behavior.TransactionActivityBehavior;
import org.flowable.engine.impl.bpmn.behavior.UserTaskActivityBehavior;
import org.flowable.engine.impl.bpmn.behavior.WebServiceActivityBehavior;
import org.flowable.engine.impl.bpmn.helper.ClassDelegate;
import org.flowable.engine.impl.bpmn.parser.BpmnParse;
import org.flowable.engine.impl.bpmn.parser.BpmnParser;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.delegate.ActivityBehavior;

/**
 * Factory class used by the {@link BpmnParser} and {@link BpmnParse} to instantiate the behaviour classes. For example when parsing an exclusive gateway, this factory will be requested to create a
 * new {@link ActivityBehavior} that will be set on the {@link ActivityImpl} of that step of the process and will implement the spec-compliant behavior of the exclusive gateway.
 * 
 * You can provide your own implementation of this class. This way, you can give different execution semantics to a standard bpmn xml construct. Eg. you could tweak the exclusive gateway to do
 * something completely different if you would want that. Creating your own {@link ActivityBehaviorFactory} is only advisable if you want to change the default behavior of any BPMN default construct.
 * And even then, think twice, because it won't be spec compliant bpmn anymore.
 * 
 * Note that you can always express any custom step as a service task with a class delegation.
 * 
 * The easiest and advisable way to implement your own {@link ActivityBehaviorFactory} is to extend the {@link DefaultActivityBehaviorFactory} class and override the method specific to the
 * {@link ActivityBehavior} you want to change.
 * 
 * An instance of this interface can be injected in the {@link ProcessEngineConfigurationImpl} and its subclasses.
 * 
 * @author Joram Barrez
 */
public interface ActivityBehaviorFactory {

    public abstract NoneStartEventActivityBehavior createNoneStartEventActivityBehavior(StartEvent startEvent);

    public abstract TaskActivityBehavior createTaskActivityBehavior(Task task);

    public abstract ManualTaskActivityBehavior createManualTaskActivityBehavior(ManualTask manualTask);

    public abstract ReceiveTaskActivityBehavior createReceiveTaskActivityBehavior(ReceiveTask receiveTask);

    public abstract UserTaskActivityBehavior createUserTaskActivityBehavior(UserTask userTask);

    public abstract ClassDelegate createClassDelegateServiceTask(ServiceTask serviceTask);

    public abstract ServiceTaskDelegateExpressionActivityBehavior createServiceTaskDelegateExpressionActivityBehavior(ServiceTask serviceTask);

    public abstract ServiceTaskExpressionActivityBehavior createServiceTaskExpressionActivityBehavior(ServiceTask serviceTask);

    public abstract WebServiceActivityBehavior createWebServiceActivityBehavior(ServiceTask serviceTask, BpmnModel bpmnModel);

    public abstract WebServiceActivityBehavior createWebServiceActivityBehavior(SendTask sendTask, BpmnModel bpmnModel);

    public abstract MailActivityBehavior createMailActivityBehavior(ServiceTask serviceTask);

    public abstract MailActivityBehavior createMailActivityBehavior(SendTask sendTask);

    // We do not want a hard dependency on the Mule module, hence we return
    // ActivityBehavior and instantiate the delegate instance using a string instead of the Class itself.
    public abstract ActivityBehavior createMuleActivityBehavior(ServiceTask serviceTask);

    public abstract ActivityBehavior createMuleActivityBehavior(SendTask sendTask);

    public abstract ActivityBehavior createCamelActivityBehavior(ServiceTask serviceTask);

    public abstract ActivityBehavior createCamelActivityBehavior(SendTask sendTask);

    public abstract ActivityBehavior createDmnActivityBehavior(ServiceTask serviceTask);

    public abstract ActivityBehavior createDmnActivityBehavior(SendTask sendTask);

    public abstract ActivityBehavior createHttpActivityBehavior(ServiceTask serviceTask);

    public abstract ShellActivityBehavior createShellActivityBehavior(ServiceTask serviceTask);

    public abstract ActivityBehavior createBusinessRuleTaskActivityBehavior(BusinessRuleTask businessRuleTask);

    public abstract ScriptTaskActivityBehavior createScriptTaskActivityBehavior(ScriptTask scriptTask);

    public abstract ExclusiveGatewayActivityBehavior createExclusiveGatewayActivityBehavior(ExclusiveGateway exclusiveGateway);

    public abstract ParallelGatewayActivityBehavior createParallelGatewayActivityBehavior(ParallelGateway parallelGateway);

    public abstract InclusiveGatewayActivityBehavior createInclusiveGatewayActivityBehavior(InclusiveGateway inclusiveGateway);

    public abstract EventBasedGatewayActivityBehavior createEventBasedGatewayActivityBehavior(EventGateway eventGateway);

    public abstract SequentialMultiInstanceBehavior createSequentialMultiInstanceBehavior(Activity activity, AbstractBpmnActivityBehavior innerActivityBehavior);

    public abstract ParallelMultiInstanceBehavior createParallelMultiInstanceBehavior(Activity activity, AbstractBpmnActivityBehavior innerActivityBehavior);

    public abstract SubProcessActivityBehavior createSubprocessActivityBehavior(SubProcess subProcess);

    public abstract EventSubProcessActivityBehavior createEventSubprocessActivityBehavior(EventSubProcess eventSubProcess); 

    public abstract EventSubProcessErrorStartEventActivityBehavior createEventSubProcessErrorStartEventActivityBehavior(StartEvent startEvent);

    public abstract EventSubProcessMessageStartEventActivityBehavior createEventSubProcessMessageStartEventActivityBehavior(StartEvent startEvent, MessageEventDefinition messageEventDefinition);

    public abstract EventSubProcessSignalStartEventActivityBehavior createEventSubProcessSignalStartEventActivityBehavior(StartEvent startEvent, SignalEventDefinition signalEventDefinition, Signal signal);

    public abstract EventSubProcessTimerStartEventActivityBehavior createEventSubProcessTimerStartEventActivityBehavior(StartEvent startEvent, TimerEventDefinition timerEventDefinition);

    public abstract AdhocSubProcessActivityBehavior createAdhocSubprocessActivityBehavior(SubProcess subProcess);

    public abstract CallActivityBehavior createCallActivityBehavior(CallActivity callActivity);

    public abstract TransactionActivityBehavior createTransactionActivityBehavior(Transaction transaction);

    public abstract IntermediateCatchEventActivityBehavior createIntermediateCatchEventActivityBehavior(IntermediateCatchEvent intermediateCatchEvent);

    public abstract IntermediateCatchMessageEventActivityBehavior createIntermediateCatchMessageEventActivityBehavior(IntermediateCatchEvent intermediateCatchEvent,
            MessageEventDefinition messageEventDefinition);

    public abstract IntermediateCatchTimerEventActivityBehavior createIntermediateCatchTimerEventActivityBehavior(IntermediateCatchEvent intermediateCatchEvent, TimerEventDefinition timerEventDefinition);

    public abstract IntermediateCatchSignalEventActivityBehavior createIntermediateCatchSignalEventActivityBehavior(IntermediateCatchEvent intermediateCatchEvent,
            SignalEventDefinition signalEventDefinition, Signal signal);

    public abstract IntermediateThrowNoneEventActivityBehavior createIntermediateThrowNoneEventActivityBehavior(ThrowEvent throwEvent);

    public abstract IntermediateThrowSignalEventActivityBehavior createIntermediateThrowSignalEventActivityBehavior(ThrowEvent throwEvent, SignalEventDefinition signalEventDefinition, Signal signal);

    public abstract IntermediateThrowCompensationEventActivityBehavior createIntermediateThrowCompensationEventActivityBehavior(ThrowEvent throwEvent, CompensateEventDefinition compensateEventDefinition);

    public abstract NoneEndEventActivityBehavior createNoneEndEventActivityBehavior(EndEvent endEvent);

    public abstract ErrorEndEventActivityBehavior createErrorEndEventActivityBehavior(EndEvent endEvent, ErrorEventDefinition errorEventDefinition);

    public abstract CancelEndEventActivityBehavior createCancelEndEventActivityBehavior(EndEvent endEvent);

    public abstract TerminateEndEventActivityBehavior createTerminateEndEventActivityBehavior(EndEvent endEvent);

    public abstract BoundaryEventActivityBehavior createBoundaryEventActivityBehavior(BoundaryEvent boundaryEvent, boolean interrupting);

    public abstract BoundaryCancelEventActivityBehavior createBoundaryCancelEventActivityBehavior(CancelEventDefinition cancelEventDefinition);

    public abstract BoundaryTimerEventActivityBehavior createBoundaryTimerEventActivityBehavior(BoundaryEvent boundaryEvent, TimerEventDefinition timerEventDefinition, boolean interrupting);

    public abstract BoundarySignalEventActivityBehavior createBoundarySignalEventActivityBehavior(BoundaryEvent boundaryEvent, SignalEventDefinition signalEventDefinition, Signal signal, boolean interrupting);

    public abstract BoundaryMessageEventActivityBehavior createBoundaryMessageEventActivityBehavior(BoundaryEvent boundaryEvent, MessageEventDefinition messageEventDefinition, boolean interrupting);

    public abstract BoundaryCompensateEventActivityBehavior createBoundaryCompensateEventActivityBehavior(BoundaryEvent boundaryEvent, CompensateEventDefinition compensateEventDefinition, boolean interrupting);
}