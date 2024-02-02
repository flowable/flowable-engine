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
package org.flowable.engine.test.el.function;

import static org.assertj.core.api.Assertions.assertThat;

import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.test.HistoryTestHelper;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.task.api.Task;
import org.flowable.variable.api.history.HistoricVariableInstance;
import org.junit.jupiter.api.Test;

/**
 * @author Filip Hrisafov
 */
class TaskFunctionDelegatesTest extends PluggableFlowableTestCase {

    @Test
    @Deployment
    void testTaskGet() {
        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("testTaskGetFunction")
                .start();

        Object taskAssignee = managementService.executeCommand(commandContext -> {
            ExecutionEntity instance = CommandContextUtil.getExecutionEntityManager(commandContext).findById(processInstance.getId());
            return CommandContextUtil.getProcessEngineConfiguration(commandContext)
                    .getExpressionManager()
                    .createExpression("${task:get(taskId).assignee}")
                    .getValue(instance);
        });

        assertThat(taskAssignee).isNull();

        Task task = taskService.createTaskQuery().singleResult();
        assertThat(task).isNotNull();
        assertThat(task.getName()).isEqualTo("my task");
        taskService.setAssignee(task.getId(), "kermit");

        taskAssignee = managementService.executeCommand(commandContext -> {
            ExecutionEntity instance = CommandContextUtil.getExecutionEntityManager(commandContext).findById(processInstance.getId());
            return CommandContextUtil.getProcessEngineConfiguration(commandContext)
                    .getExpressionManager()
                    .createExpression("${task:get(taskId).assignee}")
                    .getValue(instance);
        });

        assertThat(taskAssignee).isEqualTo("kermit");

        taskService.complete(task.getId());

        assertThat(runtimeService.getVariable(processInstance.getId(), "taskAssignee")).isEqualTo("kermit");

        task = taskService.createTaskQuery().singleResult();
        assertThat(task).isNotNull();
        assertThat(task.getName()).isEqualTo("my second task");

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            HistoricVariableInstance historicTaskAssignee = historyService.createHistoricVariableInstanceQuery()
                    .processInstanceId(processInstance.getId())
                    .variableName("taskAssignee")
                    .singleResult();
            assertThat(historicTaskAssignee).isNotNull();
            assertThat(historicTaskAssignee.getValue()).isEqualTo("kermit");
        }

        taskAssignee = managementService.executeCommand(commandContext -> {
            ExecutionEntity instance = CommandContextUtil.getExecutionEntityManager(commandContext).findById(processInstance.getId());
            return CommandContextUtil.getProcessEngineConfiguration(commandContext)
                    .getExpressionManager()
                    .createExpression("${task:get(taskId).assignee}")
                    .getValue(instance);
        });

        assertThat(taskAssignee).isEqualTo("kermit");

    }

}
