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
package org.flowable.engine.test.bpmn.event.escalation;

import static org.assertj.core.api.Assertions.assertThat;

import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.test.Deployment;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.Test;

/**
 * @author Tijs Rademakers
 */
public class EscalationEventSubProcessTest extends PluggableFlowableTestCase {
    
    @Test
    @Deployment
    // an event subprocesses takes precedence over a boundary event
    public void testEventSubprocessTakesPrecedence() {
        String procId = runtimeService.startProcessInstanceByKey("catchEscalationInEmbeddedSubProcess").getId();
        assertThatEscalationHasBeenCaught(procId);
    }

    @Test
    @Deployment
    // an event subprocess with errorCode takes precedence over a catch-all handler
    public void testEscalationCodeTakesPrecedence() {
        String procId = runtimeService.startProcessInstanceByKey("catchEscalationInEmbeddedSubProcess").getId();

        // The process will throw an escalation event, which is caught and escalated by a User org.flowable.task.service.Task
        assertThat(taskService.createTaskQuery().taskDefinitionKey("taskAfterEscalationCatch2").count()).isEqualTo(1);
        Task task = taskService.createTaskQuery().singleResult();
        assertThat(task.getName()).isEqualTo("Escalated Task");

        // Completing the task will end the process instance
        taskService.complete(task.getId());
        assertProcessEnded(procId);

    }

    @Test
    @Deployment
    public void testCatchEscalationInEmbeddedSubProcess() {
        String procId = runtimeService.startProcessInstanceByKey("catchEscalationInEmbeddedSubProcess").getId();
        assertThatEscalationHasBeenCaught(procId);
    }
    
    @Test
    @Deployment(resources = {"org/flowable/engine/test/bpmn/event/escalation/EscalationEventSubProcessTest.testCatchEscalationInParentSubProcess.bpmn20.xml", 
                    "org/flowable/engine/test/bpmn/event/escalation/BoundaryEscalationEventTest.subprocess.bpmn20.xml"})
    public void testCatchChildProcessInEventSubprocess() {
        String procId = runtimeService.startProcessInstanceByKey("catchEscalationInEventSubProcess").getId();
        
        // task in sub process
        Task task = taskService.createTaskQuery().singleResult();
        taskService.complete(task.getId());
        
        assertThatEscalationHasBeenCaught(procId);
    }
    
    @Test
    @Deployment(resources = {"org/flowable/engine/test/bpmn/event/escalation/EscalationEventSubProcessTest.testCatchChildProcessThrowEventInEventSubProcess.bpmn20.xml", 
                    "org/flowable/engine/test/bpmn/event/escalation/throwEscalationEventSubProcess.bpmn20.xml"})
    public void testCatchChildProcessThrowEventInEventSubprocess() {
        String procId = runtimeService.startProcessInstanceByKey("catchEscalationInEventSubProcess").getId();
        
        // task before in sub process
        Task task = taskService.createTaskQuery().singleResult();
        assertThat(task.getTaskDefinitionKey()).isEqualTo("taskBefore");
        taskService.complete(task.getId());
        
        task = taskService.createTaskQuery().processInstanceId(task.getProcessInstanceId()).singleResult();
        assertThat(task.getTaskDefinitionKey()).isEqualTo("taskAfter");
        
        Task escalationTask = taskService.createTaskQuery().processInstanceId(procId).singleResult();
        assertThat(escalationTask.getName()).isEqualTo("Escalated Task");
        taskService.complete(escalationTask.getId());
        
        assertThat(runtimeService.createProcessInstanceQuery().processInstanceId(procId).count()).isEqualTo(1);
        taskService.complete(task.getId());
        
        assertProcessEnded(procId);
    }

    private void assertThatEscalationHasBeenCaught(String procId) {
        // The process will throw an error event,
        // which is caught and escalated by a User org.flowable.task.service.Task
        assertThat(taskService.createTaskQuery().count()).as("No tasks found in task list.").isEqualTo(1);
        Task task = taskService.createTaskQuery().singleResult();
        assertThat(task.getName()).isEqualTo("Escalated Task");

        // Completing the org.flowable.task.service.Task will end the process instance
        taskService.complete(task.getId());
        assertProcessEnded(procId);
    }

}
