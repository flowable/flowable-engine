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
package org.flowable.cdi.impl.event;

import java.util.HashSet;
import java.util.Set;

import org.flowable.bpmn.model.BaseElement;
import org.flowable.bpmn.model.BusinessRuleTask;
import org.flowable.bpmn.model.CallActivity;
import org.flowable.bpmn.model.EndEvent;
import org.flowable.bpmn.model.ErrorEventDefinition;
import org.flowable.bpmn.model.EventGateway;
import org.flowable.bpmn.model.EventSubProcess;
import org.flowable.bpmn.model.ExclusiveGateway;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.FlowableListener;
import org.flowable.bpmn.model.ImplementationType;
import org.flowable.bpmn.model.InclusiveGateway;
import org.flowable.bpmn.model.ManualTask;
import org.flowable.bpmn.model.ParallelGateway;
import org.flowable.bpmn.model.ReceiveTask;
import org.flowable.bpmn.model.ScriptTask;
import org.flowable.bpmn.model.SendTask;
import org.flowable.bpmn.model.SequenceFlow;
import org.flowable.bpmn.model.ServiceTask;
import org.flowable.bpmn.model.SignalEventDefinition;
import org.flowable.bpmn.model.StartEvent;
import org.flowable.bpmn.model.SubProcess;
import org.flowable.bpmn.model.Task;
import org.flowable.bpmn.model.ThrowEvent;
import org.flowable.bpmn.model.TimerEventDefinition;
import org.flowable.bpmn.model.Transaction;
import org.flowable.bpmn.model.UserTask;
import org.flowable.cdi.BusinessProcessEventType;
import org.flowable.engine.delegate.ExecutionListener;
import org.flowable.engine.delegate.TaskListener;
import org.flowable.engine.impl.bpmn.parser.BpmnParse;
import org.flowable.engine.parse.BpmnParseHandler;

/**
 * {@link BpmnParseHandler} registering the {@link CdiExecutionListener} for distributing execution events using the cdi event infrastructure
 *
 * @author Daniel Meyer
 * @author Joram Barrez
 */
public class CdiEventSupportBpmnParseHandler implements BpmnParseHandler {

    protected static final Set<Class<? extends BaseElement>> supportedTypes = new HashSet<>();

    static {
        supportedTypes.add(StartEvent.class);
        supportedTypes.add(EndEvent.class);
        supportedTypes.add(ExclusiveGateway.class);
        supportedTypes.add(InclusiveGateway.class);
        supportedTypes.add(ParallelGateway.class);
        supportedTypes.add(ScriptTask.class);
        supportedTypes.add(ServiceTask.class);
        supportedTypes.add(BusinessRuleTask.class);
        supportedTypes.add(Task.class);
        supportedTypes.add(ManualTask.class);
        supportedTypes.add(UserTask.class);
        supportedTypes.add(SubProcess.class);
        supportedTypes.add(EventSubProcess.class);
        supportedTypes.add(CallActivity.class);
        supportedTypes.add(SendTask.class);
        supportedTypes.add(ReceiveTask.class);
        supportedTypes.add(EventGateway.class);
        supportedTypes.add(Transaction.class);
        supportedTypes.add(ThrowEvent.class);

        supportedTypes.add(TimerEventDefinition.class);
        supportedTypes.add(ErrorEventDefinition.class);
        supportedTypes.add(SignalEventDefinition.class);

        supportedTypes.add(SequenceFlow.class);
    }

    @Override
    public Set<Class<? extends BaseElement>> getHandledTypes() {
        return supportedTypes;
    }

    @Override
    public void parse(BpmnParse bpmnParse, BaseElement element) {

        if (element instanceof SequenceFlow) {

            SequenceFlow sequenceFlow = (SequenceFlow) element;
            CdiExecutionListener listener = new CdiExecutionListener(sequenceFlow.getId());
            addListenerToElement(sequenceFlow, ExecutionListener.EVENTNAME_TAKE, listener);

        } else {

            if (element instanceof UserTask) {

                UserTask userTask = (UserTask) element;

                addCreateListener(userTask);
                addAssignListener(userTask);
                addCompleteListener(userTask);
                addDeleteListener(userTask);
            }

            if (element instanceof FlowElement) {

                FlowElement flowElement = (FlowElement) element;

                addStartEventListener(flowElement);
                addEndEventListener(flowElement);
            }

        }
    }

    private void addCompleteListener(UserTask userTask) {
        addListenerToUserTask(userTask, TaskListener.EVENTNAME_COMPLETE, new CdiTaskListener(userTask.getId(), BusinessProcessEventType.COMPLETE_TASK));
    }

    private void addAssignListener(UserTask userTask) {
        addListenerToUserTask(userTask, TaskListener.EVENTNAME_ASSIGNMENT, new CdiTaskListener(userTask.getId(), BusinessProcessEventType.ASSIGN_TASK));
    }

    private void addCreateListener(UserTask userTask) {
        addListenerToUserTask(userTask, TaskListener.EVENTNAME_CREATE, new CdiTaskListener(userTask.getId(), BusinessProcessEventType.CREATE_TASK));
    }

    protected void addDeleteListener(UserTask userTask) {
        addListenerToUserTask(userTask, TaskListener.EVENTNAME_DELETE, new CdiTaskListener(userTask.getId(), BusinessProcessEventType.DELETE_TASK));
    }

    protected void addStartEventListener(FlowElement flowElement) {
        CdiExecutionListener listener = new CdiExecutionListener(flowElement.getId(), BusinessProcessEventType.START_ACTIVITY);
        addListenerToElement(flowElement, ExecutionListener.EVENTNAME_START, listener);
    }

    protected void addEndEventListener(FlowElement flowElement) {
        CdiExecutionListener listener = new CdiExecutionListener(flowElement.getId(), BusinessProcessEventType.END_ACTIVITY);
        addListenerToElement(flowElement, ExecutionListener.EVENTNAME_END, listener);
    }

    protected void addListenerToElement(FlowElement flowElement, String event, Object instance) {
        FlowableListener listener = new FlowableListener();
        listener.setEvent(event);
        listener.setImplementationType(ImplementationType.IMPLEMENTATION_TYPE_INSTANCE);
        listener.setInstance(instance);
        flowElement.getExecutionListeners().add(listener);
    }

    protected void addListenerToUserTask(UserTask userTask, String event, Object instance) {
        FlowableListener listener = new FlowableListener();
        listener.setEvent(event);
        listener.setImplementationType(ImplementationType.IMPLEMENTATION_TYPE_INSTANCE);
        listener.setInstance(instance);
        userTask.getTaskListeners().add(listener);
    }

}
