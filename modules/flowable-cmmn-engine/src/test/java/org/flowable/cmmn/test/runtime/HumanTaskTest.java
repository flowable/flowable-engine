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
package org.flowable.cmmn.test.runtime;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

import java.util.List;

import org.flowable.cmmn.api.repository.CaseDefinition;
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.engine.test.FlowableCmmnTestCase;
import org.flowable.common.engine.impl.identity.Authentication;
import org.flowable.task.api.Task;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.junit.Test;

/**
 * @author Tijs Rademakers
 * @author Joram Barrez
 */
public class HumanTaskTest extends FlowableCmmnTestCase {

    @Test
    @CmmnDeployment
    public void testHumanTask() {
        Authentication.setAuthenticatedUserId("JohnDoe");

        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                        .caseDefinitionKey("myCase")
                        .start();
        assertNotNull(caseInstance);

        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertEquals("Task 1", task.getName());
        assertEquals("JohnDoe", task.getAssignee());

        cmmnTaskService.complete(task.getId());

        task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertEquals("Task 2", task.getName());
        assertNull(task.getAssignee());

        task = cmmnTaskService.createTaskQuery().taskCandidateGroup("test").caseInstanceId(caseInstance.getId()).singleResult();
        assertEquals("Task 2", task.getName());

        task = cmmnTaskService.createTaskQuery().taskCandidateUser("test2").caseInstanceId(caseInstance.getId()).singleResult();
        assertEquals("Task 2", task.getName());

        cmmnTaskService.complete(task.getId());

        assertEquals(0, cmmnRuntimeService.createCaseInstanceQuery().count());

        assertEquals("JohnDoe", cmmnHistoryService.createHistoricVariableInstanceQuery()
                        .caseInstanceId(caseInstance.getId())
                        .variableName("var1")
                        .singleResult().getValue());

        Authentication.setAuthenticatedUserId(null);
    }

    @Test
    public void testCreateHumanTaskUnderTenantByKey() {
        Authentication.setAuthenticatedUserId("JohnDoe");
        org.flowable.cmmn.api.repository.CmmnDeployment deployment = cmmnRepositoryService.createDeployment().tenantId("flowable").
                addClasspathResource("org/flowable/cmmn/test/runtime/HumanTaskTest.testHumanTask.cmmn").deploy();
        try {
            assertThat(deployment.getTenantId(), is("flowable"));

            CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                    .caseDefinitionKey("myCase")
                    .tenantId("flowable")
                    .start();
            assertNotNull(caseInstance);
            assertEquals("flowable", caseInstance.getTenantId());

            Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
            assertEquals("Task 1", task.getName());
            assertEquals("JohnDoe", task.getAssignee());
            assertEquals("flowable", task.getTenantId());

            cmmnTaskService.complete(task.getId());

            task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
            assertEquals("Task 2", task.getName());
            assertEquals("flowable", task.getTenantId());
            cmmnTaskService.complete(task.getId());

            assertEquals(0, cmmnRuntimeService.createCaseInstanceQuery().count());

        } finally {
            cmmnRepositoryService.deleteDeployment(deployment.getId(), true);
            Authentication.setAuthenticatedUserId(null);
        }

    }

    @Test
    public void testCreateHumanTaskUnderTenantById() {
        Authentication.setAuthenticatedUserId("JohnDoe");
        org.flowable.cmmn.api.repository.CmmnDeployment deployment = cmmnRepositoryService.createDeployment().tenantId("flowable").
                addClasspathResource("org/flowable/cmmn/test/runtime/HumanTaskTest.testHumanTask.cmmn").deploy();
        try {
            assertThat(deployment.getTenantId(), is("flowable"));
            CaseDefinition caseDefinition = cmmnRepositoryService.createCaseDefinitionQuery().deploymentId(deployment.getId()).singleResult();
            assertThat(caseDefinition.getTenantId(), is("flowable"));

            CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                    .caseDefinitionId(caseDefinition.getId())
                    .tenantId("flowable")
                    .start();
            assertNotNull(caseInstance);
            assertEquals("flowable", caseInstance.getTenantId());

            Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
            assertEquals("Task 1", task.getName());
            assertEquals("JohnDoe", task.getAssignee());
            assertEquals("flowable", task.getTenantId());

            cmmnTaskService.complete(task.getId());

            task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
            assertEquals("flowable", task.getTenantId());
            cmmnTaskService.complete(task.getId());

            assertEquals(0, cmmnRuntimeService.createCaseInstanceQuery().count());
        } finally {
            cmmnRepositoryService.deleteDeployment(deployment.getId(), true);
            Authentication.setAuthenticatedUserId(null);
        }

    }

    @Test
    @CmmnDeployment
    public void testTaskCompletionExitsStage() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("humanTaskCompletionExits")
                .start();
        assertNotNull(caseInstance);

        List<Task> tasks = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).orderByTaskName().asc().list();
        assertEquals("A", tasks.get(0).getName());
        assertEquals("B", tasks.get(1).getName());
        assertEquals("C", tasks.get(2).getName());

        // Completing A should delete B and C
        cmmnTaskService.complete(tasks.get(0).getId());
        assertEquals(0, cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).count());
        assertCaseInstanceEnded(caseInstance);

        List<HistoricTaskInstance> historicTaskInstances = cmmnHistoryService.createHistoricTaskInstanceQuery().list();
        assertEquals(3, historicTaskInstances.size());
        for (HistoricTaskInstance historicTaskInstance : historicTaskInstances) {
            assertNotNull(historicTaskInstance.getStartTime());
            assertNotNull(historicTaskInstance.getEndTime());
            if (!historicTaskInstance.getName().equals("A")) {
                assertEquals("cmmn-state-transition-terminate-case", historicTaskInstance.getDeleteReason());
            }
        }

        // Completing C should delete B
        CaseInstance caseInstance2 = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("humanTaskCompletionExits")
                .start();
        assertNotNull(caseInstance2);
        tasks = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance2.getId()).orderByTaskName().asc().list();
        cmmnTaskService.complete(tasks.get(2).getId());

        Task taskA = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance2.getId()).orderByTaskName().asc().singleResult();
        assertNotNull(taskA);
        cmmnTaskService.complete(taskA.getId());
        assertCaseInstanceEnded(caseInstance2);

        historicTaskInstances = cmmnHistoryService.createHistoricTaskInstanceQuery().caseInstanceId(caseInstance2.getId()).list();
        assertEquals(3, historicTaskInstances.size());
        for (HistoricTaskInstance historicTaskInstance : historicTaskInstances) {
            assertNotNull(historicTaskInstance.getStartTime());
            assertNotNull(historicTaskInstance.getEndTime());
            if (historicTaskInstance.getName().equals("B")) {
                assertEquals("cmmn-state-transition-exit", historicTaskInstance.getDeleteReason());
            }
        }
    }

}
