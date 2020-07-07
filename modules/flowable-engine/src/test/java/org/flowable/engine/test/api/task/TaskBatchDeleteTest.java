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
import org.junit.jupiter.api.Test;

public class TaskBatchDeleteTest extends PluggableFlowableTestCase {

    /**
     * Validating fix for ACT-2070
     */
    @Test
    @Deployment
    public void testDeleteTaskWithChildren() throws Exception {

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("testBatchDeleteOfTask");
        assertThat(processInstance).isNotNull();
        assertThat(processInstance.isEnded()).isFalse();

        // Get first task and finish. This should destroy the scope and trigger
        // some deletes, including:
        // org.flowable.task.service.Task 1, Identity link pointing to task 1, org.flowable.task.service.Task 2
        // The task deletes shouldn't be batched in this case, keeping the
        // related entity delete order
        org.flowable.task.api.Task firstTask = taskService.createTaskQuery().processInstanceId(processInstance.getId()).taskDefinitionKey("taskOne").singleResult();
        assertThat(firstTask).isNotNull();

        taskService.complete(firstTask.getId());

        // Process should have ended fine
        processInstance = runtimeService.createProcessInstanceQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(processInstance).isNull();

    }

    @Test
    @Deployment
    public void testDeleteCancelledMultiInstanceTasks() throws Exception {

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("testBatchDeleteOfTask");
        assertThat(processInstance).isNotNull();
        assertThat(processInstance.isEnded()).isFalse();

        org.flowable.task.api.Task lastTask = taskService.createTaskQuery().processInstanceId(processInstance.getId()).taskDefinitionKey("multiInstance").listPage(4, 1).get(0);

        taskService.addCandidateGroup(lastTask.getId(), "sales");

        org.flowable.task.api.Task firstTask = taskService.createTaskQuery().processInstanceId(processInstance.getId()).taskDefinitionKey("multiInstance").listPage(0, 1).get(0);
        assertThat(firstTask).isNotNull();

        taskService.complete(firstTask.getId());

        // Process should have ended fine
        processInstance = runtimeService.createProcessInstanceQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(processInstance).isNull();
        
        waitForHistoryJobExecutorToProcessAllJobs(7000, 100);
        
    }
}
