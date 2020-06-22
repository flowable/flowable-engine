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
package org.flowable.examples.bpmn.usertask.taskassignee;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.util.List;

import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.Test;

/**
 * Simple process test to validate the current implementation prototype.
 * 
 * @author Joram Barrez
 */
public class TaskAssigneeTest extends PluggableFlowableTestCase {

    @Test
    @Deployment
    public void testTaskAssignee() {

        // Start process instance
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("taskAssigneeExampleProcess");

        // Get task list
        List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().taskAssignee("kermit").list();
        assertThat(tasks)
                .extracting(Task::getName, Task::getDescription)
                .containsExactly(tuple("Schedule meeting", "Schedule an engineering meeting for next week with the new hire."));

        // Complete task. Process is now finished
        taskService.complete(tasks.get(0).getId());
        // assert if the process instance completed
        assertProcessEnded(processInstance.getId());
    }

}
