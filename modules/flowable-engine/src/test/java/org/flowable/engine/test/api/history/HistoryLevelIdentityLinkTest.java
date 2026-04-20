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

import java.util.List;

import org.flowable.engine.impl.test.HistoryTestHelper;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.identitylink.api.IdentityLinkType;
import org.flowable.identitylink.api.history.HistoricIdentityLink;
import org.flowable.identitylink.service.HistoricIdentityLinkService;
import org.flowable.identitylink.service.impl.persistence.entity.HistoricIdentityLinkEntity;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.Test;

public class HistoryLevelIdentityLinkTest extends PluggableFlowableTestCase {

    @Deployment(resources = { "org/flowable/engine/test/api/history/oneTaskHistoryLevelInstanceProcess.bpmn20.xml" })
    @Test
    public void testInstanceHistoryLevelStoresProcessInstanceIdentityLinks() {
        identityService.setAuthenticatedUserId("johndoe");
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");

        runtimeService.addParticipantUser(processInstance.getId(), "participant1");

        HistoryTestHelper.waitForJobExecutorToProcessAllHistoryJobs(processEngineConfiguration, managementService, 7000, 200);

        List<HistoricIdentityLink> processIdentityLinks = historyService.getHistoricIdentityLinksForProcessInstance(processInstance.getId());
        assertThat(processIdentityLinks).hasSize(2);
        assertThat(processIdentityLinks)
                .extracting(HistoricIdentityLink::getType)
                .containsExactlyInAnyOrder(IdentityLinkType.STARTER, IdentityLinkType.PARTICIPANT);
        assertThat(processIdentityLinks)
                .extracting(HistoricIdentityLink::getUserId)
                .containsExactlyInAnyOrder("johndoe", "participant1");

        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        taskService.complete(task.getId());
    }

    @Deployment(resources = { "org/flowable/engine/test/api/history/oneTaskHistoryLevelInstanceProcess.bpmn20.xml" })
    @Test
    public void testInstanceHistoryLevelDoesNotStoreTaskIdentityLinks() {
        identityService.setAuthenticatedUserId("johndoe");
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");

        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        taskService.addCandidateUser(task.getId(), "candidateUser1");
        taskService.addCandidateGroup(task.getId(), "candidateGroup1");

        HistoryTestHelper.waitForJobExecutorToProcessAllHistoryJobs(processEngineConfiguration, managementService, 7000, 200);

        // At instance level, task identity links should not be stored,
        // but the interceptor creates participant links on the process instance for candidate users
        List<HistoricIdentityLink> processIdentityLinks = historyService.getHistoricIdentityLinksForProcessInstance(processInstance.getId());
        assertThat(processIdentityLinks)
                .extracting(HistoricIdentityLink::getType)
                .containsOnly(IdentityLinkType.STARTER, IdentityLinkType.PARTICIPANT);

        // Verify no historic identity links exist for the task directly (using management command
        // because getHistoricIdentityLinksForTask requires a historic task which doesn't exist at instance level)
        List<?> taskLinks = managementService.executeCommand(commandContext -> {
            return processEngineConfiguration.getIdentityLinkServiceConfiguration()
                    .getHistoricIdentityLinkService()
                    .findHistoricIdentityLinksByTaskId(task.getId());
        });
        assertThat(taskLinks).isEmpty();

        taskService.complete(task.getId());
    }

    @Deployment(resources = { "org/flowable/engine/test/api/history/oneTaskHistoryLevelTaskProcess.bpmn20.xml" })
    @Test
    public void testTaskHistoryLevelStoresProcessInstanceAndTaskIdentityLinks() {
        identityService.setAuthenticatedUserId("johndoe");
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");

        runtimeService.addParticipantUser(processInstance.getId(), "participant1");

        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        taskService.addCandidateUser(task.getId(), "candidateUser1");
        taskService.addCandidateGroup(task.getId(), "candidateGroup1");

        HistoryTestHelper.waitForJobExecutorToProcessAllHistoryJobs(processEngineConfiguration, managementService, 7000, 200);

        // Process instance links: starter + participant + participant from candidate user interceptor
        List<HistoricIdentityLink> processIdentityLinks = historyService.getHistoricIdentityLinksForProcessInstance(processInstance.getId());
        assertThat(processIdentityLinks).hasSize(3);

        // Task identity links: candidate user + candidate group
        List<HistoricIdentityLink> taskIdentityLinks = historyService.getHistoricIdentityLinksForTask(task.getId());
        assertThat(taskIdentityLinks).hasSize(2);
        assertThat(taskIdentityLinks)
                .extracting(HistoricIdentityLink::getType)
                .containsExactlyInAnyOrder(IdentityLinkType.CANDIDATE, IdentityLinkType.CANDIDATE);

        taskService.complete(task.getId());
    }

    @Deployment(resources = { "org/flowable/engine/test/api/history/oneTaskHistoryLevelActivityProcess.bpmn20.xml" })
    @Test
    public void testActivityHistoryLevelStoresProcessInstanceAndTaskIdentityLinks() {
        identityService.setAuthenticatedUserId("johndoe");
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");

        runtimeService.addParticipantUser(processInstance.getId(), "participant1");

        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        taskService.addCandidateUser(task.getId(), "candidateUser1");
        taskService.addCandidateGroup(task.getId(), "candidateGroup1");

        HistoryTestHelper.waitForJobExecutorToProcessAllHistoryJobs(processEngineConfiguration, managementService, 7000, 200);

        // Process instance links: starter + participant + participant from candidate user interceptor
        List<HistoricIdentityLink> processIdentityLinks = historyService.getHistoricIdentityLinksForProcessInstance(processInstance.getId());
        assertThat(processIdentityLinks).hasSize(3);

        // Task identity links: candidate user + candidate group
        // At activity level, historic tasks are not stored, so we query via the service directly
        List<HistoricIdentityLinkEntity> taskLinks = managementService.executeCommand(commandContext -> {
            return processEngineConfiguration.getIdentityLinkServiceConfiguration()
                    .getHistoricIdentityLinkService()
                    .findHistoricIdentityLinksByTaskId(task.getId());
        });
        assertThat(taskLinks).hasSize(2);

        taskService.complete(task.getId());

        HistoryTestHelper.waitForJobExecutorToProcessAllHistoryJobs(processEngineConfiguration, managementService, 7000, 200);

        // Clean up task identity links that are not associated with a process instance
        // (at activity level, no historic task exists so these are not cascade-deleted)
        managementService.executeCommand(commandContext -> {
            HistoricIdentityLinkService historicIdentityLinkService = processEngineConfiguration.getIdentityLinkServiceConfiguration()
                    .getHistoricIdentityLinkService();
            for (HistoricIdentityLinkEntity link : taskLinks) {
                historicIdentityLinkService.deleteHistoricIdentityLink(link.getId());
            }
            return null;
        });
    }

    @Deployment(resources = { "org/flowable/engine/test/api/history/oneTaskHistoryLevelNoneProcess.bpmn20.xml" })
    @Test
    public void testNoneHistoryLevelDoesNotStoreIdentityLinks() {
        identityService.setAuthenticatedUserId("johndoe");
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");

        runtimeService.addParticipantUser(processInstance.getId(), "participant1");

        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        taskService.addCandidateUser(task.getId(), "candidateUser1");

        HistoryTestHelper.waitForJobExecutorToProcessAllHistoryJobs(processEngineConfiguration, managementService, 7000, 200);

        List<HistoricIdentityLink> processIdentityLinks = historyService.getHistoricIdentityLinksForProcessInstance(processInstance.getId());
        assertThat(processIdentityLinks).isEmpty();

        List<?> taskLinks = managementService.executeCommand(commandContext -> {
            return processEngineConfiguration.getIdentityLinkServiceConfiguration()
                    .getHistoricIdentityLinkService()
                    .findHistoricIdentityLinksByTaskId(task.getId());
        });
        assertThat(taskLinks).isEmpty();

        taskService.complete(task.getId());
    }
}
