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
package org.flowable.engine.test.api.task;

import static org.assertj.core.api.Assertions.assertThat;

import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.task.api.Task;
import org.flowable.task.api.TaskInfo;
import org.junit.jupiter.api.Test;

/**
 * @author Filip Hrisafov
 */
class TaskFindByProcessInstanceTest extends PluggableFlowableTestCase {

    @Test
    @Deployment(resources = "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml")
    void testFindByProcessInstanceWithinSameCommandContext() {
        managementService.executeCommand(commandContext -> {
            ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
            assertThat(processEngineConfiguration.getTaskServiceConfiguration().getTaskEntityManager().findTasksByProcessInstanceId(processInstance.getId()))
                .extracting(TaskInfo::getTaskDefinitionKey)
                .containsExactly("theTask");
            return null;
        });
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml")
    void testFindByProcessInstance() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
        assertThat(taskService.createTaskQuery()
            .processInstanceId(processInstance.getId())
            .list())
            .extracting(TaskInfo::getTaskDefinitionKey)
            .containsExactly("theTask");
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/api/parallelTask.bpmn20.xml")
    void testFindByProcessInstanceWithCompletionWithinSameCommandContext() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("startParallelProcess");
        assertThat(taskService.createTaskQuery()
            .processInstanceId(processInstance.getId())
            .list())
            .extracting(TaskInfo::getTaskDefinitionKey)
            .containsExactly("taskBefore");
        managementService.executeCommand(commandContext -> {
            taskService.complete(taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult().getId());
            assertThat(processEngineConfiguration.getTaskServiceConfiguration().getTaskEntityManager().findTasksByProcessInstanceId(processInstance.getId()))
                .extracting(TaskInfo::getTaskDefinitionKey)
                .containsExactlyInAnyOrder("task1", "task2");
            return null;
        });
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/api/parallelTask.bpmn20.xml")
    void testFindByProcessInstanceWithCompletion() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("startParallelProcess");
        Task task = taskService.createTaskQuery()
            .processInstanceId(processInstance.getId())
            .singleResult();
        taskService.complete(task.getId());

        assertThat(taskService.createTaskQuery()
            .processInstanceId(processInstance.getId())
            .list())
            .extracting(TaskInfo::getTaskDefinitionKey)
            .containsExactlyInAnyOrder("task1", "task2");
    }

}
