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
package org.flowable.cmmn.test.listener;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.test.impl.CustomCmmnConfigurationFlowableTestCase;
import org.flowable.task.api.Task;
import org.flowable.task.api.TaskInfo;
import org.flowable.task.service.delegate.DelegateTask;
import org.flowable.task.service.delegate.TaskListener;
import org.junit.Test;

/**
 * @author Joram Barrez
 */
public class TaskListenerTest extends CustomCmmnConfigurationFlowableTestCase {

    @Override
    protected String getEngineName() {
        return this.getClass().getName();
    }

    @Override
    protected void configureConfiguration(CmmnEngineConfiguration cmmnEngineConfiguration) {
        Map<Object, Object> beans = new HashMap<>();
        cmmnEngineConfiguration.setBeans(beans);

        beans.put("taskListenerCreateBean", new TestDelegateTaskListener());
        beans.put("taskListenerCompleteBean", new TestDelegateTaskListener());
        beans.put("taskListenerAssignBean", new TestDelegateTaskListener());
    }

    @Test
    @CmmnDeployment
    public void testCreateEvent() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testTaskListeners").start();
        assertVariable(caseInstance, "variableFromClassDelegate", "Hello World from class delegate");
        assertVariable(caseInstance, "variableFromDelegateExpression", "Hello World from delegate expression");
        assertVariable(caseInstance, "expressionVariable", "Hello World from expression");
    }

    @Test
    @CmmnDeployment
    public void testCompleteEvent() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testTaskListeners").start();
        List<Task> tasks = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).list();
        for (Task task : tasks) {
            if (!"Keepalive".equals(task.getName())) {
                cmmnTaskService.complete(task.getId());
            }
        }
        assertVariable(caseInstance, "variableFromClassDelegate", "Hello World from class delegate");
        assertVariable(caseInstance, "variableFromDelegateExpression", "Hello World from delegate expression");
        assertVariable(caseInstance, "expressionVariable", "Hello World from expression");
    }


    @Test
    @CmmnDeployment
    public void testAssignEvent() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testTaskListeners").start();
        List<Task> tasks = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).list();
        for (Task task : tasks) {
            if (!"Keepalive".equals(task.getName())) {
                cmmnTaskService.setAssignee(task.getId(), "testAssignee");
            }
        }
        assertVariable(caseInstance, "variableFromClassDelegate", "Hello World from class delegate");
        assertVariable(caseInstance, "variableFromDelegateExpression", "Hello World from delegate expression");
        assertVariable(caseInstance, "expressionVariable", "Hello World from expression");
    }

    @Test
    @CmmnDeployment
    public void testAssignEventOriginalAssignee() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testTaskListeners").start();
        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        cmmnTaskService.setAssignee(task.getId(), "testAssignee");

        assertVariable(task, "taskId", task.getId());
        assertVariable(task, "previousAssignee", "defaultAssignee");
        assertVariable(task, "currentAssignee", "testAssignee");
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/listener/TaskListenerTest.testAssignEventOriginalAssignee.cmmn")
    public void testAssignEventOnCreateByHumanTaskActivityBehaviour() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testTaskListeners").start();
        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();

        assertVariable(task, "taskId", task.getId());
        assertVariable(task, "previousAssignee", "defaultAssignee");
        assertVariable(task, "currentAssignee", "defaultAssignee");
    }

    private void assertVariable(CaseInstance caseInstance, String varName, String value) {
        String variable = (String) cmmnRuntimeService.getVariable(caseInstance.getId(), varName);
        assertThat(variable).isEqualTo(value);
    }

    private void assertVariable(TaskInfo task, String varName, String value) {
        String variable = (String) cmmnTaskService.getVariable(task.getId(), varName);
        assertThat(variable).isEqualTo(value);
    }

    static class TestDelegateTaskListener implements TaskListener {

        @Override
        public void notify(DelegateTask delegateTask) {
            delegateTask.setVariable("variableFromDelegateExpression", "Hello World from delegate expression");
        }
    }

}
