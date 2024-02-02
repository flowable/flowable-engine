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
package org.flowable.cmmn.test.el;

import static org.assertj.core.api.Assertions.assertThat;

import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.engine.impl.persistence.entity.CaseInstanceEntity;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.engine.test.FlowableCmmnTestCase;
import org.flowable.cmmn.engine.test.impl.CmmnHistoryTestHelper;
import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.task.api.Task;
import org.flowable.variable.api.history.HistoricVariableInstance;
import org.junit.Test;

/**
 * @author Filip Hrisafov
 */
public class TaskFunctionDelegatesTest extends FlowableCmmnTestCase {

    @Test
    @CmmnDeployment
    public void testTaskGet() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("testTaskGetFunction")
                .start();

        Object taskAssignee = cmmnEngineConfiguration.getCommandExecutor().execute(commandContext -> {
            CaseInstanceEntity instance = CommandContextUtil.getCaseInstanceEntityManager(commandContext).findById(caseInstance.getId());
            return CommandContextUtil.getCmmnEngineConfiguration(commandContext)
                    .getExpressionManager()
                    .createExpression("${task:get(taskId).assignee}")
                    .getValue(instance);
        });

        assertThat(taskAssignee).isNull();

        Task task = cmmnTaskService.createTaskQuery().singleResult();
        assertThat(task).isNotNull();
        assertThat(task.getName()).isEqualTo("Task One");
        cmmnTaskService.setAssignee(task.getId(), "kermit");

        taskAssignee = cmmnEngineConfiguration.getCommandExecutor().execute(commandContext -> {
            CaseInstanceEntity instance = CommandContextUtil.getCaseInstanceEntityManager(commandContext).findById(caseInstance.getId());
            return CommandContextUtil.getCmmnEngineConfiguration(commandContext)
                    .getExpressionManager()
                    .createExpression("${task:get(taskId).assignee}")
                    .getValue(instance);
        });

        assertThat(taskAssignee).isEqualTo("kermit");

        cmmnTaskService.complete(task.getId());

        assertThat(cmmnRuntimeService.getVariable(caseInstance.getId(), "taskAssignee")).isEqualTo("kermit");

        task = cmmnTaskService.createTaskQuery().singleResult();
        assertThat(task).isNotNull();
        assertThat(task.getName()).isEqualTo("The Case");

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            HistoricVariableInstance historicTaskAssignee = cmmnHistoryService.createHistoricVariableInstanceQuery()
                    .caseInstanceId(caseInstance.getId())
                    .variableName("taskAssignee")
                    .singleResult();
            assertThat(historicTaskAssignee).isNotNull();
            assertThat(historicTaskAssignee.getValue()).isEqualTo("kermit");
        }

        taskAssignee = cmmnEngineConfiguration.getCommandExecutor().execute(commandContext -> {
            CaseInstanceEntity instance = CommandContextUtil.getCaseInstanceEntityManager(commandContext).findById(caseInstance.getId());
            return CommandContextUtil.getCmmnEngineConfiguration(commandContext)
                    .getExpressionManager()
                    .createExpression("${task:get(taskId).assignee}")
                    .getValue(instance);
        });

        assertThat(taskAssignee).isEqualTo("kermit");

    }

}
