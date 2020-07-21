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

package org.flowable.engine.test.history;

import static org.assertj.core.api.Assertions.assertThat;

import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.junit.jupiter.api.Test;

/**
 * @author Frederik Heremans
 */
public class HistoricTaskInstanceUpdateTest extends PluggableFlowableTestCase {

    @Test
    @Deployment
    public void testHistoricTaskInstanceUpdate() {
        runtimeService.startProcessInstanceByKey("HistoricTaskInstanceTest").getId();

        org.flowable.task.api.Task task = taskService.createTaskQuery().singleResult();

        // Update and save the task's fields before it is finished
        task.setPriority(12345);
        task.setDescription("Updated description");
        task.setName("Updated name");
        task.setAssignee("gonzo");
        taskService.saveTask(task);

        taskService.complete(task.getId());
        
        waitForHistoryJobExecutorToProcessAllJobs(7000, 100);
        
        assertThat(historyService.createHistoricTaskInstanceQuery().count()).isEqualTo(1);

        HistoricTaskInstance historicTaskInstance = historyService.createHistoricTaskInstanceQuery().singleResult();
        assertThat(historicTaskInstance.getName()).isEqualTo("Updated name");
        assertThat(historicTaskInstance.getDescription()).isEqualTo("Updated description");
        assertThat(historicTaskInstance.getAssignee()).isEqualTo("gonzo");
        assertThat(historicTaskInstance.getTaskDefinitionKey()).isEqualTo("task");

        // Validate fix of ACT-1923: updating assignee to null should be reflected in history
        ProcessInstance secondInstance = runtimeService.startProcessInstanceByKey("HistoricTaskInstanceTest");

        task = taskService.createTaskQuery().singleResult();

        task.setDescription(null);
        task.setName(null);
        task.setAssignee(null);
        taskService.saveTask(task);

        taskService.complete(task.getId());
        
        waitForHistoryJobExecutorToProcessAllJobs(7000, 100);
        
        assertThat(historyService.createHistoricTaskInstanceQuery().processInstanceId(secondInstance.getId()).count()).isEqualTo(1);

        historicTaskInstance = historyService.createHistoricTaskInstanceQuery().processInstanceId(secondInstance.getId()).singleResult();
        assertThat(historicTaskInstance.getName()).isNull();
        assertThat(historicTaskInstance.getDescription()).isNull();
        assertThat(historicTaskInstance.getAssignee()).isNull();
    }
}
