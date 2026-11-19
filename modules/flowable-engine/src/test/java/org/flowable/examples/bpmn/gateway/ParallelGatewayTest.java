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

package org.flowable.examples.bpmn.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.task.api.Task;
import org.flowable.task.api.TaskQuery;
import org.junit.jupiter.api.Test;

/**
 * @author Joram Barrez
 */
public class ParallelGatewayTest extends PluggableFlowableTestCase {

    @Test
    @Deployment
    public void testForkJoin() {

        ProcessInstance pi = runtimeService.startProcessInstanceByKey("forkJoin");
        TaskQuery query = taskService.createTaskQuery().processInstanceId(pi.getId()).orderByTaskName().asc();

        List<org.flowable.task.api.Task> tasks = query.list();
        // the tasks are ordered by name (see above)
        assertThat(tasks)
                .extracting(Task::getName)
                .containsExactly("Receive Payment", "Ship Order");

        // Completing both tasks will join the concurrent executions
        taskService.complete(tasks.get(0).getId());
        taskService.complete(tasks.get(1).getId());

        tasks = query.list();
        assertThat(tasks)
                .extracting(Task::getName)
                .containsExactly("Archive Order");
    }

    @Test
    @Deployment
    public void testUnbalancedForkJoin() {

        ProcessInstance pi = runtimeService.startProcessInstanceByKey("UnbalancedForkJoin");
        TaskQuery query = taskService.createTaskQuery().processInstanceId(pi.getId()).orderByTaskName().asc();

        List<org.flowable.task.api.Task> tasks = query.list();
        // the tasks are ordered by name (see above)
        assertThat(tasks)
                .extracting(Task::getName)
                .containsExactly("Task 1", "Task 2", "Task 3");

        // Completing the first task should *not* trigger the join
        taskService.complete(tasks.get(0).getId());

        // Completing the second task should trigger the first join
        taskService.complete(tasks.get(1).getId());

        tasks = query.list();
        assertThat(tasks)
                .extracting(Task::getName)
                .containsExactly("Task 3", "Task 4");

        // Completing the remaining tasks should trigger the second join and end
        // the process
        taskService.complete(tasks.get(0).getId());
        taskService.complete(tasks.get(1).getId());

        assertProcessEnded(pi.getId());
    }

    @Test
    @Deployment
    public void testMultipleTokensJoin() {

        ProcessInstance pi = runtimeService.startProcessInstanceByKey("multipleTokensJoin");
        TaskQuery query = taskService.createTaskQuery().processInstanceId(pi.getId()).orderByTaskName().asc();

        // There should be two tokens waiting at Wait Task 1 (two sequence flows lead to it in bpmn)
        List<org.flowable.task.api.Task> tasks = query.list();
        assertThat(tasks)
                .extracting(Task::getName)
                .containsExactly("Wait Task 1", "Wait Task 1", "Wait Task 2");

        // Completing both Wait Task 1s should not satisfy the joining parallel gateway, and Final Task should still not be reached
        // We should still be waiting at Wait Task 2
        tasks.stream().filter(task -> task.getName().equals("Wait Task 1")).forEach(task -> {
            taskService.complete(task.getId());
        });

        tasks = query.list();
        assertThat(tasks)
                .extracting(Task::getName)
                .containsExactly("Wait Task 2");

        // Completing Wait Task 2 should satisfy the Parallel Gateway, and we progress to the final task
        taskService.complete(tasks.get(0).getId());

        tasks = query.list();
        assertThat(tasks)
                .extracting(Task::getName)
                .containsExactly("Final Task");
    }

}
