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

package org.flowable.engine.test.api.history;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import org.flowable.engine.impl.test.HistoryTestHelper;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.task.api.Task;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.flowable.variable.api.history.HistoricVariableInstance;
import org.junit.jupiter.api.Test;

/**
 * @author Tijs Rademakers
 */
public class HistoryLevelServiceTest extends PluggableFlowableTestCase {

    @Deployment(resources = { "org/flowable/engine/test/api/history/oneTaskHistoryLevelNoneProcess.bpmn20.xml" })
    @Test
    public void testNoneHistoryLevel() {
        // With a clean ProcessEngine, no instances should be available
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");

        HistoryTestHelper.waitForJobExecutorToProcessAllHistoryJobs(processEngineConfiguration, managementService, 7000, 200);

        assertThat(historyService.createHistoricProcessInstanceQuery().processInstanceId(processInstance.getId()).count()).isZero();

        // Complete the task and check if the size is count 1
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).isNotNull();
        taskService.claim(task.getId(), "test");
        taskService.setOwner(task.getId(), "test");
        taskService.setAssignee(task.getId(), "anotherTest");
        taskService.setPriority(task.getId(), 40);
        taskService.setDueDate(task.getId(), new Date());
        taskService.setVariable(task.getId(), "var1", "test");
        taskService.setVariableLocal(task.getId(), "localVar1", "test2");
        taskService.complete(task.getId());

        HistoryTestHelper.waitForJobExecutorToProcessAllHistoryJobs(processEngineConfiguration, managementService, 7000, 200);

        assertThat(historyService.createHistoricProcessInstanceQuery().processInstanceId(processInstance.getId()).count()).isZero();
        assertThat(historyService.createHistoricTaskInstanceQuery().processInstanceId(processInstance.getId()).count()).isZero();
        assertThat(historyService.createHistoricActivityInstanceQuery().processInstanceId(processInstance.getId()).count()).isZero();
        assertThat(historyService.createHistoricVariableInstanceQuery().processInstanceId(processInstance.getId()).count()).isZero();
    }

    @Deployment(resources = { "org/flowable/engine/test/api/history/oneTaskHistoryLevelActivityProcess.bpmn20.xml" })
    @Test
    public void testActivityHistoryLevel() {
        // With a clean ProcessEngine, no instances should be available
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");

        HistoryTestHelper.waitForJobExecutorToProcessAllHistoryJobs(processEngineConfiguration, managementService, 7000, 200);

        assertThat(historyService.createHistoricProcessInstanceQuery().processInstanceId(processInstance.getId()).count()).isEqualTo(1);

        // Complete the task and check if the size is count 1
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).isNotNull();

        assertThat(historyService.createHistoricActivityInstanceQuery().processInstanceId(processInstance.getId()).count()).isEqualTo(3);

        taskService.claim(task.getId(), "test");
        taskService.setOwner(task.getId(), "test");
        taskService.setAssignee(task.getId(), "anotherTest");
        taskService.setPriority(task.getId(), 40);
        Date dueDateValue = new Date();
        taskService.setDueDate(task.getId(), dueDateValue);
        taskService.setVariable(task.getId(), "var1", "test");
        taskService.setVariableLocal(task.getId(), "localVar1", "test2");
        taskService.complete(task.getId());

        HistoryTestHelper.waitForJobExecutorToProcessAllHistoryJobs(processEngineConfiguration, managementService, 7000, 200);

        assertThat(historyService.createHistoricProcessInstanceQuery().processInstanceId(processInstance.getId()).count()).isEqualTo(1);
        assertThat(historyService.createHistoricTaskInstanceQuery().processInstanceId(processInstance.getId()).count()).isZero();
        assertThat(historyService.createHistoricActivityInstanceQuery().processInstanceId(processInstance.getId()).count()).isEqualTo(5);

        assertThat(historyService.createHistoricVariableInstanceQuery().processInstanceId(processInstance.getId()).count()).isEqualTo(2);

        List<HistoricVariableInstance> variables = historyService.createHistoricVariableInstanceQuery().processInstanceId(processInstance.getId()).list();
        assertThat(variables)
                .extracting(
                        HistoricVariableInstance::getVariableName,
                        HistoricVariableInstance::getValue,
                        HistoricVariableInstance::getProcessInstanceId,
                        HistoricVariableInstance::getTaskId)
                .containsOnly(
                        tuple("var1", "test", processInstance.getProcessInstanceId(), null),
                        tuple("localVar1", "test2", processInstance.getProcessInstanceId(), task.getId())
                );

        assertThat(historyService.createHistoricDetailQuery().processInstanceId(processInstance.getId()).count()).isZero();
    }

    @Deployment(resources = { "org/flowable/engine/test/api/history/oneTaskHistoryLevelAuditProcess.bpmn20.xml" })
    @Test
    public void testAuditHistoryLevel() {
        // With a clean ProcessEngine, no instances should be available
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");

        HistoryTestHelper.waitForJobExecutorToProcessAllHistoryJobs(processEngineConfiguration, managementService, 7000, 200);

        assertThat(historyService.createHistoricProcessInstanceQuery().processInstanceId(processInstance.getId()).count()).isEqualTo(1);

        // Complete the task and check if the size is count 1
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).isNotNull();

        assertThat(historyService.createHistoricActivityInstanceQuery().processInstanceId(processInstance.getId()).count()).isEqualTo(3);

        taskService.claim(task.getId(), "test");
        taskService.setOwner(task.getId(), "test");
        taskService.setAssignee(task.getId(), "anotherTest");
        taskService.setPriority(task.getId(), 40);
        Calendar dueDateCalendar = new GregorianCalendar();
        taskService.setDueDate(task.getId(), dueDateCalendar.getTime());
        taskService.setVariable(task.getId(), "var1", "test");
        taskService.setVariableLocal(task.getId(), "localVar1", "test2");
        taskService.complete(task.getId());

        HistoryTestHelper.waitForJobExecutorToProcessAllHistoryJobs(processEngineConfiguration, managementService, 7000, 200);

        assertThat(historyService.createHistoricProcessInstanceQuery().processInstanceId(processInstance.getId()).count()).isEqualTo(1);
        assertThat(historyService.createHistoricTaskInstanceQuery().processInstanceId(processInstance.getId()).count()).isEqualTo(1);
        assertThat(historyService.createHistoricActivityInstanceQuery().processInstanceId(processInstance.getId()).count()).isEqualTo(5);

        HistoricTaskInstance historicTask = historyService.createHistoricTaskInstanceQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(historicTask.getOwner()).isEqualTo("test");
        assertThat(historicTask.getAssignee()).isEqualTo("anotherTest");
        assertThat(historicTask.getPriority()).isEqualTo(40);

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        assertThat(simpleDateFormat.format(historicTask.getDueDate())).isEqualTo(simpleDateFormat.format(dueDateCalendar.getTime()));

        assertThat(historyService.createHistoricVariableInstanceQuery().processInstanceId(processInstance.getId()).count()).isEqualTo(2);

        List<HistoricVariableInstance> variables = historyService.createHistoricVariableInstanceQuery().processInstanceId(processInstance.getId()).list();
        assertThat(variables)
                .extracting(
                        HistoricVariableInstance::getVariableName,
                        HistoricVariableInstance::getValue,
                        HistoricVariableInstance::getProcessInstanceId,
                        HistoricVariableInstance::getTaskId)
                .containsOnly(
                        tuple("var1", "test", processInstance.getProcessInstanceId(), null),
                        tuple("localVar1", "test2", processInstance.getProcessInstanceId(), task.getId())
                );

        assertThat(historyService.createHistoricDetailQuery().processInstanceId(processInstance.getId()).count()).isZero();
    }

    @Deployment(resources = { "org/flowable/engine/test/api/history/oneTaskHistoryLevelFullProcess.bpmn20.xml" })
    @Test
    public void testFullHistoryLevel() {
        // With a clean ProcessEngine, no instances should be available
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");

        HistoryTestHelper.waitForJobExecutorToProcessAllHistoryJobs(processEngineConfiguration, managementService, 7000, 200);

        assertThat(historyService.createHistoricProcessInstanceQuery().processInstanceId(processInstance.getId()).count()).isEqualTo(1);

        // Complete the task and check if the size is count 1
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).isNotNull();

        assertThat(historyService.createHistoricActivityInstanceQuery().processInstanceId(processInstance.getId()).count()).isEqualTo(3);

        taskService.claim(task.getId(), "test");
        taskService.setOwner(task.getId(), "test");
        taskService.setAssignee(task.getId(), "anotherTest");
        taskService.setPriority(task.getId(), 40);
        Date dueDateValue = new Date();
        taskService.setDueDate(task.getId(), dueDateValue);
        taskService.setVariable(task.getId(), "var1", "test");
        taskService.setVariableLocal(task.getId(), "localVar1", "test2");
        taskService.complete(task.getId());

        HistoryTestHelper.waitForJobExecutorToProcessAllHistoryJobs(processEngineConfiguration, managementService, 7000, 200);

        assertThat(historyService.createHistoricProcessInstanceQuery().processInstanceId(processInstance.getId()).count()).isEqualTo(1);
        assertThat(historyService.createHistoricTaskInstanceQuery().processInstanceId(processInstance.getId()).count()).isEqualTo(1);
        assertThat(historyService.createHistoricActivityInstanceQuery().processInstanceId(processInstance.getId()).count()).isEqualTo(5);

        HistoricTaskInstance historicTask = historyService.createHistoricTaskInstanceQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(historicTask.getOwner()).isEqualTo("test");
        assertThat(historicTask.getAssignee()).isEqualTo("anotherTest");
        assertThat(historicTask.getPriority()).isEqualTo(40);

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        assertThat(simpleDateFormat.format(historicTask.getDueDate())).isEqualTo(simpleDateFormat.format(dueDateValue));

        assertThat(historyService.createHistoricVariableInstanceQuery().processInstanceId(processInstance.getId()).count()).isEqualTo(2);

        List<HistoricVariableInstance> variables = historyService.createHistoricVariableInstanceQuery().processInstanceId(processInstance.getId()).list();
        assertThat(variables)
                .extracting(
                        HistoricVariableInstance::getVariableName,
                        HistoricVariableInstance::getValue,
                        HistoricVariableInstance::getProcessInstanceId,
                        HistoricVariableInstance::getTaskId)
                .containsOnly(
                        tuple("var1", "test", processInstance.getProcessInstanceId(), null),
                        tuple("localVar1", "test2", processInstance.getProcessInstanceId(), task.getId())
                );

        assertThat(historyService.createHistoricDetailQuery().processInstanceId(processInstance.getId()).count()).isEqualTo(2);
    }

}