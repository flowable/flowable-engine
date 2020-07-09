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
public class BoundaryEscalationEventTest extends PluggableFlowableTestCase {

    @Test
    @Deployment
    public void testCatchEscalationOnEmbeddedSubprocess() {
        runtimeService.startProcessInstanceByKey("boundaryEscalationOnEmbeddedSubprocess");

        // After process start, usertask in subprocess should exist
        Task task = taskService.createTaskQuery().singleResult();
        assertThat(task.getName()).isEqualTo("subprocessTask");

        // After task completion, escalation end event is reached and caught
        taskService.complete(task.getId());
        task = taskService.createTaskQuery().singleResult();
        assertThat(task.getName()).isEqualTo("task after catching the escalation");
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/bpmn/event/escalation/BoundaryEscalationEventTest.testCatchEscalationOnCallActivity-parent.bpmn20.xml",
            "org/flowable/engine/test/bpmn/event/escalation/BoundaryEscalationEventTest.subprocess.bpmn20.xml" })
    public void testCatchEscalationOnCallActivity() {
        String procId = runtimeService.startProcessInstanceByKey("catchEscalationOnCallActivity").getId();
        Task task = taskService.createTaskQuery().singleResult();
        assertThat(task.getName()).isEqualTo("Task in subprocess");

        // Completing the task will reach the end error event,
        // which is caught on the call activity boundary
        taskService.complete(task.getId());
        task = taskService.createTaskQuery().singleResult();
        assertThat(task.getName()).isEqualTo("Escalated Task");

        // Completing the task will end the process instance
        taskService.complete(task.getId());
        assertProcessEnded(procId);
    }
}
