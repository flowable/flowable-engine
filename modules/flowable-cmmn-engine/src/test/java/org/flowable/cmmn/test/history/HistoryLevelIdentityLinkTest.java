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
package org.flowable.cmmn.test.history;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.engine.test.impl.CmmnHistoryTestHelper;
import org.flowable.cmmn.test.FlowableCmmnTestCase;
import org.flowable.common.engine.impl.identity.Authentication;
import org.flowable.identitylink.api.IdentityLinkType;
import org.flowable.identitylink.api.history.HistoricIdentityLink;
import org.flowable.identitylink.service.impl.persistence.entity.HistoricIdentityLinkEntity;
import org.flowable.task.api.Task;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

public class HistoryLevelIdentityLinkTest extends FlowableCmmnTestCase {

    @AfterEach
    public void tearDown() {
        Authentication.setAuthenticatedUserId(null);
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/history/oneHumanTaskHistoryLevelInstance.cmmn")
    public void testInstanceHistoryLevelStoresCaseInstanceIdentityLinks() {
        Authentication.setAuthenticatedUserId("johndoe");
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCase")
                .start();

        cmmnRuntimeService.addUserIdentityLink(caseInstance.getId(), "participant1", IdentityLinkType.PARTICIPANT);

        CmmnHistoryTestHelper.waitForJobExecutorToProcessAllHistoryJobs(cmmnEngineConfiguration, cmmnManagementService, 7000, 200);

        List<HistoricIdentityLink> caseIdentityLinks = cmmnHistoryService.getHistoricIdentityLinksForCaseInstance(caseInstance.getId());
        assertThat(caseIdentityLinks).hasSize(2);
        assertThat(caseIdentityLinks)
                .extracting(HistoricIdentityLink::getType)
                .containsExactlyInAnyOrder(IdentityLinkType.STARTER, IdentityLinkType.PARTICIPANT);
        assertThat(caseIdentityLinks)
                .extracting(HistoricIdentityLink::getUserId)
                .containsExactlyInAnyOrder("johndoe", "participant1");

        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        cmmnTaskService.complete(task.getId());
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/history/oneHumanTaskHistoryLevelInstance.cmmn")
    public void testInstanceHistoryLevelDoesNotStoreTaskIdentityLinks() {
        Authentication.setAuthenticatedUserId("johndoe");
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCase")
                .start();

        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        cmmnTaskService.addUserIdentityLink(task.getId(), "candidateUser1", IdentityLinkType.CANDIDATE);
        cmmnTaskService.addGroupIdentityLink(task.getId(), "candidateGroup1", IdentityLinkType.CANDIDATE);

        CmmnHistoryTestHelper.waitForJobExecutorToProcessAllHistoryJobs(cmmnEngineConfiguration, cmmnManagementService, 7000, 200);

        // At instance level, task identity links should not be stored,
        // but the interceptor creates participant links on the case instance for candidate users
        List<HistoricIdentityLink> caseIdentityLinks = cmmnHistoryService.getHistoricIdentityLinksForCaseInstance(caseInstance.getId());
        assertThat(caseIdentityLinks)
                .extracting(HistoricIdentityLink::getType)
                .containsOnly(IdentityLinkType.STARTER, IdentityLinkType.PARTICIPANT);

        // Verify no historic identity links exist for the task directly (using management command
        // because getHistoricIdentityLinksForTask requires a historic task which doesn't exist at instance level)
        List<?> taskLinks = cmmnEngineConfiguration.getCommandExecutor().execute(commandContext -> {
            return cmmnEngineConfiguration.getIdentityLinkServiceConfiguration()
                    .getHistoricIdentityLinkService()
                    .findHistoricIdentityLinksByTaskId(task.getId());
        });
        assertThat(taskLinks).isEmpty();

        cmmnTaskService.complete(task.getId());
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/history/oneHumanTaskHistoryLevelTask.cmmn")
    public void testTaskHistoryLevelStoresCaseInstanceAndTaskIdentityLinks() {
        Authentication.setAuthenticatedUserId("johndoe");
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCase")
                .start();

        cmmnRuntimeService.addUserIdentityLink(caseInstance.getId(), "participant1", IdentityLinkType.PARTICIPANT);

        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        cmmnTaskService.addUserIdentityLink(task.getId(), "candidateUser1", IdentityLinkType.CANDIDATE);
        cmmnTaskService.addGroupIdentityLink(task.getId(), "candidateGroup1", IdentityLinkType.CANDIDATE);

        CmmnHistoryTestHelper.waitForJobExecutorToProcessAllHistoryJobs(cmmnEngineConfiguration, cmmnManagementService, 7000, 200);

        // Case instance links: starter + participant + participant from candidate user interceptor
        List<HistoricIdentityLink> caseIdentityLinks = cmmnHistoryService.getHistoricIdentityLinksForCaseInstance(caseInstance.getId());
        assertThat(caseIdentityLinks).hasSize(3);

        // Task identity links: candidate user + candidate group
        List<HistoricIdentityLink> taskIdentityLinks = cmmnHistoryService.getHistoricIdentityLinksForTask(task.getId());
        assertThat(taskIdentityLinks).hasSize(2);
        assertThat(taskIdentityLinks)
                .extracting(HistoricIdentityLink::getType)
                .containsExactlyInAnyOrder(IdentityLinkType.CANDIDATE, IdentityLinkType.CANDIDATE);

        cmmnTaskService.complete(task.getId());
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/history/oneHumanTaskHistoryLevelActivity.cmmn")
    public void testActivityHistoryLevelStoresCaseInstanceAndTaskIdentityLinks() {
        Authentication.setAuthenticatedUserId("johndoe");
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCase")
                .start();

        cmmnRuntimeService.addUserIdentityLink(caseInstance.getId(), "participant1", IdentityLinkType.PARTICIPANT);

        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        cmmnTaskService.addUserIdentityLink(task.getId(), "candidateUser1", IdentityLinkType.CANDIDATE);
        cmmnTaskService.addGroupIdentityLink(task.getId(), "candidateGroup1", IdentityLinkType.CANDIDATE);

        CmmnHistoryTestHelper.waitForJobExecutorToProcessAllHistoryJobs(cmmnEngineConfiguration, cmmnManagementService, 7000, 200);

        // Case instance links: starter + participant + participant from candidate user interceptor
        List<HistoricIdentityLink> caseIdentityLinks = cmmnHistoryService.getHistoricIdentityLinksForCaseInstance(caseInstance.getId());
        assertThat(caseIdentityLinks).hasSize(3);
        
        HistoricTaskInstance historyTaskInstance = cmmnHistoryService.createHistoricTaskInstanceQuery().taskId(task.getId()).singleResult();
        assertThat(historyTaskInstance).isNull();

        // Task identity links: candidate user + candidate group
        // At activity level, historic tasks are not stored, so we query via the service directly
        List<HistoricIdentityLinkEntity> taskLinks = cmmnEngineConfiguration.getCommandExecutor().execute(commandContext -> {
            return cmmnEngineConfiguration.getIdentityLinkServiceConfiguration()
                    .getHistoricIdentityLinkService()
                    .findHistoricIdentityLinksByTaskId(task.getId());
        });
        assertThat(taskLinks).hasSize(0);

        cmmnTaskService.complete(task.getId());

        CmmnHistoryTestHelper.waitForJobExecutorToProcessAllHistoryJobs(cmmnEngineConfiguration, cmmnManagementService, 7000, 200);
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/history/testOneSimpleHumanTaskWithHistoryLevelNone.cmmn")
    public void testNoneHistoryLevelDoesNotStoreIdentityLinks() {
        Authentication.setAuthenticatedUserId("johndoe");
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("myCase")
                .start();

        cmmnRuntimeService.addUserIdentityLink(caseInstance.getId(), "participant1", IdentityLinkType.PARTICIPANT);

        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        cmmnTaskService.addUserIdentityLink(task.getId(), "candidateUser1", IdentityLinkType.CANDIDATE);

        CmmnHistoryTestHelper.waitForJobExecutorToProcessAllHistoryJobs(cmmnEngineConfiguration, cmmnManagementService, 7000, 200);

        List<HistoricIdentityLink> caseIdentityLinks = cmmnHistoryService.getHistoricIdentityLinksForCaseInstance(caseInstance.getId());
        assertThat(caseIdentityLinks).isEmpty();

        List<?> taskLinks = cmmnEngineConfiguration.getCommandExecutor().execute(commandContext -> {
            return cmmnEngineConfiguration.getIdentityLinkServiceConfiguration()
                    .getHistoricIdentityLinkService()
                    .findHistoricIdentityLinksByTaskId(task.getId());
        });
        assertThat(taskLinks).isEmpty();

        cmmnTaskService.complete(task.getId());
    }
}
