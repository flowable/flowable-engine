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
package org.activiti.engine.impl.history.parse;

import java.util.HashSet;
import java.util.Set;

import org.activiti.engine.impl.bpmn.parser.BpmnParse;
import org.activiti.engine.impl.history.handler.ActivityInstanceEndHandler;
import org.activiti.engine.impl.history.handler.ActivityInstanceStartHandler;
import org.activiti.engine.impl.pvm.process.ActivityImpl;
import org.activiti.engine.parse.BpmnParseHandler;
import org.flowable.bpmn.model.BaseElement;
import org.flowable.bpmn.model.BoundaryEvent;
import org.flowable.bpmn.model.BusinessRuleTask;
import org.flowable.bpmn.model.CallActivity;
import org.flowable.bpmn.model.EndEvent;
import org.flowable.bpmn.model.EventGateway;
import org.flowable.bpmn.model.ExclusiveGateway;
import org.flowable.bpmn.model.InclusiveGateway;
import org.flowable.bpmn.model.IntermediateCatchEvent;
import org.flowable.bpmn.model.ManualTask;
import org.flowable.bpmn.model.ParallelGateway;
import org.flowable.bpmn.model.ReceiveTask;
import org.flowable.bpmn.model.ScriptTask;
import org.flowable.bpmn.model.SendTask;
import org.flowable.bpmn.model.ServiceTask;
import org.flowable.bpmn.model.SubProcess;
import org.flowable.bpmn.model.Task;
import org.flowable.bpmn.model.ThrowEvent;
import org.flowable.bpmn.model.UserTask;

/**
 * @author Joram Barrez
 */
public class FlowNodeHistoryParseHandler implements BpmnParseHandler {

    protected static final ActivityInstanceEndHandler ACTIVITI_INSTANCE_END_LISTENER = new ActivityInstanceEndHandler();

    protected static final ActivityInstanceStartHandler ACTIVITY_INSTANCE_START_LISTENER = new ActivityInstanceStartHandler();

    protected static Set<Class<? extends BaseElement>> supportedElementClasses = new HashSet<>();

    static {
        supportedElementClasses.add(EndEvent.class);
        supportedElementClasses.add(ThrowEvent.class);
        supportedElementClasses.add(BoundaryEvent.class);
        supportedElementClasses.add(IntermediateCatchEvent.class);

        supportedElementClasses.add(ExclusiveGateway.class);
        supportedElementClasses.add(InclusiveGateway.class);
        supportedElementClasses.add(ParallelGateway.class);
        supportedElementClasses.add(EventGateway.class);

        supportedElementClasses.add(Task.class);
        supportedElementClasses.add(ManualTask.class);
        supportedElementClasses.add(ReceiveTask.class);
        supportedElementClasses.add(ScriptTask.class);
        supportedElementClasses.add(ServiceTask.class);
        supportedElementClasses.add(BusinessRuleTask.class);
        supportedElementClasses.add(SendTask.class);
        supportedElementClasses.add(UserTask.class);

        supportedElementClasses.add(CallActivity.class);
        supportedElementClasses.add(SubProcess.class);
    }

    @Override
    public Set<Class<? extends BaseElement>> getHandledTypes() {
        return supportedElementClasses;
    }

    @Override
    public void parse(BpmnParse bpmnParse, BaseElement element) {
        ActivityImpl activity = bpmnParse.getCurrentScope().findActivity(element.getId());
        if (element instanceof BoundaryEvent) {
            // A boundary-event never receives an activity start-event
            activity.addExecutionListener(org.activiti.engine.impl.pvm.PvmEvent.EVENTNAME_END, ACTIVITY_INSTANCE_START_LISTENER, 0);
            activity.addExecutionListener(org.activiti.engine.impl.pvm.PvmEvent.EVENTNAME_END, ACTIVITI_INSTANCE_END_LISTENER, 1);
        } else {
            activity.addExecutionListener(org.activiti.engine.impl.pvm.PvmEvent.EVENTNAME_START, ACTIVITY_INSTANCE_START_LISTENER, 0);
            activity.addExecutionListener(org.activiti.engine.impl.pvm.PvmEvent.EVENTNAME_END, ACTIVITI_INSTANCE_END_LISTENER);
        }
    }

}
