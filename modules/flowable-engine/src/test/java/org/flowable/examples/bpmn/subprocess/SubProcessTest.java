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

package org.flowable.examples.bpmn.subprocess;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.Test;

/**
 * @author Joram Barrez
 */
public class SubProcessTest extends PluggableFlowableTestCase {

    @Test
    public void testSimpleSubProcess() {

        Deployment deployment = repositoryService.createDeployment().addClasspathResource("org/flowable/examples/bpmn/subprocess/SubProcessTest.fixSystemFailureProcess.bpmn20.xml").deploy();

        // After staring the process, both tasks in the subprocess should be
        // active
        ProcessInstance pi = runtimeService.startProcessInstanceByKey("fixSystemFailure");
        List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().processInstanceId(pi.getId()).orderByTaskName().asc().list();

        // Tasks are ordered by name (see query)
        assertThat(tasks)
                .extracting(Task::getName)
                .containsExactly("Investigate hardware", "Investigate software");

        // Completing both the tasks finishes the subprocess and enables the
        // task after the subprocess
        taskService.complete(tasks.get(0).getId());
        taskService.complete(tasks.get(1).getId());

        org.flowable.task.api.Task writeReportTask = taskService.createTaskQuery().processInstanceId(pi.getId()).singleResult();
        assertThat(writeReportTask.getName()).isEqualTo("Write report");

        // Clean up
        repositoryService.deleteDeployment(deployment.getId(), true);
    }

}
