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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import java.util.Arrays;
import java.util.List;

import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.engine.impl.test.HistoryTestHelper;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.task.Event;
import org.flowable.engine.test.Deployment;
import org.flowable.identitylink.api.IdentityLink;
import org.flowable.identitylink.api.IdentityLinkInfo;
import org.flowable.identitylink.api.IdentityLinkType;
import org.flowable.identitylink.api.history.HistoricIdentityLink;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.node.ArrayNode;

/**
 * @author Tom Baeyens
 * @author Falko Menge
 */
public class TaskIdentityLinksTest extends PluggableFlowableTestCase {
    
    private static final String IDENTITY_LINKS_PROCESS_BPMN20_XML = "org/flowable/engine/test/api/task/IdentityLinksProcess.bpmn20.xml";
    private static final String IDENTITY_LINKS_PROCESS = "IdentityLinksProcess";

    @Test
    @Deployment(resources = "org/flowable/engine/test/api/task/IdentityLinksProcess.bpmn20.xml")
    public void testCandidateUserLink() {
        runtimeService.startProcessInstanceByKey("IdentityLinksProcess");

        String taskId = taskService.createTaskQuery().singleResult().getId();

        taskService.addCandidateUser(taskId, "kermit");

        List<IdentityLink> identityLinks = taskService.getIdentityLinksForTask(taskId);
        assertThat(identityLinks)
                .extracting(IdentityLink::getGroupId, IdentityLink::getUserId, IdentityLink::getType, IdentityLink::getTaskId)
                .containsExactly(tuple(null, "kermit", IdentityLinkType.CANDIDATE, taskId));

        taskService.deleteCandidateUser(taskId, "kermit");

        assertThat(taskService.getIdentityLinksForTask(taskId)).isEmpty();
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/api/task/IdentityLinksProcess.bpmn20.xml")
    public void testCandidateGroupLink() {
        runtimeService.startProcessInstanceByKey("IdentityLinksProcess");

        String taskId = taskService.createTaskQuery().singleResult().getId();

        taskService.addCandidateGroup(taskId, "muppets");

        List<IdentityLink> identityLinks = taskService.getIdentityLinksForTask(taskId);
        assertThat(identityLinks)
                .extracting(IdentityLink::getGroupId, IdentityLink::getUserId, IdentityLink::getType, IdentityLink::getTaskId)
                .containsExactly(tuple("muppets", null, IdentityLinkType.CANDIDATE, taskId));

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
            List<Event> taskEvents = taskService.getTaskEvents(taskId);
            assertThat(taskEvents).hasSize(1);
            Event taskEvent = taskEvents.get(0);
            assertThat(taskEvent.getAction()).isEqualTo(Event.ACTION_ADD_GROUP_LINK);
            List<String> taskEventMessageParts = taskEvent.getMessageParts();
            assertThat(taskEventMessageParts)
                    .containsExactlyInAnyOrder("muppets", IdentityLinkType.CANDIDATE);
        }

        taskService.deleteCandidateGroup(taskId, "muppets");

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
            List<Event> taskEvents = taskService.getTaskEvents(taskId);
            assertThat(taskEvents).hasSize(2);
            Event taskEvent = findTaskEvent(taskEvents, Event.ACTION_DELETE_GROUP_LINK);
            assertThat(taskEvent.getAction()).isEqualTo(Event.ACTION_DELETE_GROUP_LINK);
            List<String> taskEventMessageParts = taskEvent.getMessageParts();
            assertThat(taskEventMessageParts)
                    .containsExactlyInAnyOrder("muppets", IdentityLinkType.CANDIDATE);
        }

        assertThat(taskService.getIdentityLinksForTask(taskId)).isEmpty();
    }
    
    @Test
    @Deployment(resources = IDENTITY_LINKS_PROCESS_BPMN20_XML)
    public void testAssigneeIdentityLinkHistory() {
        if (!HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
            return;
        }

        runtimeService.startProcessInstanceByKey(IDENTITY_LINKS_PROCESS);

        String taskId = taskService.createTaskQuery().singleResult().getId();

        taskService.setAssignee(taskId, "kermit");

        assertThat(taskService.getIdentityLinksForTask(taskId)).hasSize(1);

        assertTaskEvent(taskId, 1, Event.ACTION_ADD_USER_LINK, "kermit", IdentityLinkType.ASSIGNEE);

        taskService.setAssignee(taskId, null);

        assertThat(taskService.getIdentityLinksForTask(taskId)).isEmpty();

        assertTaskEvent(taskId, 2, Event.ACTION_DELETE_USER_LINK, "kermit", IdentityLinkType.ASSIGNEE);

        waitForHistoryJobExecutorToProcessAllJobs(7000, 100);
        List<HistoricIdentityLink> history = historyService.getHistoricIdentityLinksForTask(taskId);
        assertThat(history)
                .extracting(HistoricIdentityLink::getUserId, HistoricIdentityLink::getType)
                .containsExactlyInAnyOrder(
                        tuple("kermit", IdentityLinkType.ASSIGNEE),
                        tuple(null, IdentityLinkType.ASSIGNEE)
                );
    }

    @Test
    @Deployment(resources = IDENTITY_LINKS_PROCESS_BPMN20_XML)
    public void testClaimingIdentityLinkHistory() {
        if (!HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
            return;
        }

        runtimeService.startProcessInstanceByKey(IDENTITY_LINKS_PROCESS);

        String taskId = taskService.createTaskQuery().singleResult().getId();

        taskService.claim(taskId, "kermit");

        assertThat(taskService.getIdentityLinksForTask(taskId)).hasSize(1);

        assertTaskEvent(taskId, 1, Event.ACTION_ADD_USER_LINK, "kermit", IdentityLinkType.ASSIGNEE);

        taskService.unclaim(taskId);

        assertThat(taskService.getIdentityLinksForTask(taskId)).isEmpty();

        assertTaskEvent(taskId, 2, Event.ACTION_DELETE_USER_LINK, "kermit", IdentityLinkType.ASSIGNEE);

        waitForHistoryJobExecutorToProcessAllJobs(7000, 100);
        List<HistoricIdentityLink> history = historyService.getHistoricIdentityLinksForTask(taskId);
        assertThat(history)
                .extracting(HistoricIdentityLink::getUserId, HistoricIdentityLink::getType)
                .containsExactlyInAnyOrder(
                        tuple("kermit", IdentityLinkType.ASSIGNEE),
                        tuple(null, IdentityLinkType.ASSIGNEE)
                );
    }

    @Test
    @Deployment(resources = IDENTITY_LINKS_PROCESS_BPMN20_XML)
    public void testOwnerIdentityLinkHistory() {
        if (!HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
            return;
        }

        runtimeService.startProcessInstanceByKey(IDENTITY_LINKS_PROCESS);

        String taskId = taskService.createTaskQuery().singleResult().getId();

        taskService.setOwner(taskId, "kermit");

        assertThat(taskService.getIdentityLinksForTask(taskId)).hasSize(1);

        assertTaskEvent(taskId, 1, Event.ACTION_ADD_USER_LINK, "kermit", IdentityLinkType.OWNER);

        taskService.setOwner(taskId, null);

        assertThat(taskService.getIdentityLinksForTask(taskId)).isEmpty();

        assertTaskEvent(taskId, 2, Event.ACTION_DELETE_USER_LINK, "kermit", IdentityLinkType.OWNER);

        waitForHistoryJobExecutorToProcessAllJobs(7000, 100);
        List<HistoricIdentityLink> history = historyService.getHistoricIdentityLinksForTask(taskId);
        assertThat(history)
                .extracting(HistoricIdentityLink::getUserId, HistoricIdentityLink::getType)
                .containsExactlyInAnyOrder(
                        tuple("kermit", IdentityLinkType.OWNER),
                        tuple(null, IdentityLinkType.OWNER)
                );
    }
    
    @Test
    @Deployment(resources = IDENTITY_LINKS_PROCESS_BPMN20_XML)
    public void testUnchangedIdentityIdCreatesNoLinks() {
        if (!HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
            return;
        }

        runtimeService.startProcessInstanceByKey(IDENTITY_LINKS_PROCESS);

        String taskId = taskService.createTaskQuery().singleResult().getId();

        // two claims in succession, one comment
        taskService.claim(taskId, "kermit");
        taskService.setAssignee(taskId, "kermit");

        assertThat(taskService.getIdentityLinksForTask(taskId)).hasSize(1);

        assertTaskEvent(taskId, 1, Event.ACTION_ADD_USER_LINK, "kermit", IdentityLinkType.ASSIGNEE);

        waitForHistoryJobExecutorToProcessAllJobs(7000, 100);
        List<HistoricIdentityLink> history = historyService.getHistoricIdentityLinksForTask(taskId);
        assertThat(history)
                .extracting(HistoricIdentityLink::getType, HistoricIdentityLink::getUserId)
                .containsExactly(tuple(IdentityLinkType.ASSIGNEE, "kermit"));
    }

    @Test
    @Deployment(resources = IDENTITY_LINKS_PROCESS_BPMN20_XML)
    public void testNullIdentityIdCreatesNoLinks() {
        if (!HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
            return;
        }

        runtimeService.startProcessInstanceByKey(IDENTITY_LINKS_PROCESS);

        String taskId = taskService.createTaskQuery().singleResult().getId();

        taskService.claim(taskId, null);
        taskService.setAssignee(taskId, null);

        assertThat(taskService.getIdentityLinksForTask(taskId)).isEmpty();

        assertTaskEvent(taskId, 0, null, null, null);
        
        waitForHistoryJobExecutorToProcessAllJobs(7000, 100);
        List<HistoricIdentityLink> history = historyService.getHistoricIdentityLinksForTask(taskId);
        assertThat(history).isEmpty();
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/api/task/IdentityLinksProcess.bpmn20.xml")
    public void testCustomTypeUserLink() {
        runtimeService.startProcessInstanceByKey("IdentityLinksProcess");

        String taskId = taskService.createTaskQuery().singleResult().getId();

        taskService.addUserIdentityLink(taskId, "kermit", "interestee");

        List<IdentityLink> identityLinks = taskService.getIdentityLinksForTask(taskId);
        assertThat(identityLinks)
                .extracting(IdentityLink::getGroupId, IdentityLink::getUserId, IdentityLink::getType, IdentityLink::getTaskId)
                .containsExactly(tuple(null, "kermit", "interestee", taskId));

        taskService.deleteUserIdentityLink(taskId, "kermit", "interestee");

        assertThat(taskService.getIdentityLinksForTask(taskId)).isEmpty();
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/api/task/IdentityLinksProcess.bpmn20.xml")
    public void testCustomLinkGroupLink() {
        runtimeService.startProcessInstanceByKey("IdentityLinksProcess");

        String taskId = taskService.createTaskQuery().singleResult().getId();

        taskService.addGroupIdentityLink(taskId, "muppets", "playing");

        List<IdentityLink> identityLinks = taskService.getIdentityLinksForTask(taskId);
        assertThat(identityLinks)
                .extracting(IdentityLink::getGroupId, IdentityLink::getUserId, IdentityLink::getType, IdentityLink::getTaskId)
                .containsExactly(tuple("muppets", null, "playing", taskId));

        taskService.deleteGroupIdentityLink(taskId, "muppets", "playing");

        assertThat(taskService.getIdentityLinksForTask(taskId)).isEmpty();
    }

    @Test
    public void testDeleteAssignee() {
        org.flowable.task.api.Task task = taskService.newTask();
        task.setAssignee("nonExistingUser");
        taskService.saveTask(task);

        taskService.deleteUserIdentityLink(task.getId(), "nonExistingUser", IdentityLinkType.ASSIGNEE);

        task = taskService.createTaskQuery().taskId(task.getId()).singleResult();
        assertThat(task.getAssignee()).isNull();
        assertThat(taskService.getIdentityLinksForTask(task.getId())).isEmpty();

        // cleanup
        taskService.deleteTask(task.getId(), true);
    }

    @Test
    public void testDeleteOwner() {
        org.flowable.task.api.Task task = taskService.newTask();
        task.setOwner("nonExistingUser");
        taskService.saveTask(task);

        taskService.deleteUserIdentityLink(task.getId(), "nonExistingUser", IdentityLinkType.OWNER);

        task = taskService.createTaskQuery().taskId(task.getId()).singleResult();
        assertThat(task.getOwner()).isNull();
        assertThat(taskService.getIdentityLinksForTask(task.getId())).isEmpty();

        // cleanup
        taskService.deleteTask(task.getId(), true);
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/api/task/TaskIdentityLinksTest.testDeleteCandidateUser.bpmn20.xml")
    public void testDeleteCandidateUser() {
        runtimeService.startProcessInstanceByKey("TaskIdentityLinks");

        String taskId = taskService.createTaskQuery().singleResult().getId();

        List<IdentityLink> identityLinks = taskService.getIdentityLinksForTask(taskId);
        assertThat(identityLinks)
                .extracting(IdentityLink::getUserId)
                .containsExactly("user");
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/api/task/IdentityLinksProcess.bpmn20.xml")
    public void testEmptyCandidateUserLink() {
        runtimeService.startProcessInstanceByKey("IdentityLinksProcess");

        String taskId = taskService.createTaskQuery().singleResult().getId();

        taskService.addCandidateGroup(taskId, "muppets");
        taskService.deleteCandidateUser(taskId, "kermit");

        List<IdentityLink> identityLinks = taskService.getIdentityLinksForTask(taskId);
        assertThat(identityLinks)
                .extracting(IdentityLink::getGroupId, IdentityLink::getUserId, IdentityLink::getType, IdentityLink::getTaskId)
                .containsExactly(tuple("muppets", null, IdentityLinkType.CANDIDATE, taskId));

        taskService.deleteCandidateGroup(taskId, "muppets");

        assertThat(taskService.getIdentityLinksForTask(taskId)).isEmpty();
    }

    // Test custom identity links
    @Test
    @Deployment
    public void testCustomIdentityLink() {
        runtimeService.startProcessInstanceByKey("customIdentityLink");

        List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().taskInvolvedUser("kermit").list();
        assertThat(tasks).hasSize(1);

        List<IdentityLink> identityLinks = taskService.getIdentityLinksForTask(tasks.get(0).getId());
        assertThat(identityLinks)
                .extracting(IdentityLink::getGroupId, IdentityLink::getUserId, IdentityLink::getType)
                .containsExactlyInAnyOrder(
                        tuple("management", null, "businessAdministrator"),
                        tuple(null, "kermit", "businessAdministrator")
                );
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/api/task/TaskIdentityLinksTest.testCustomIdentityLinkExpression.bpmn20.xml")
    public void testCustomIdentityLinkCollectionExpression() {
        runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("customIdentityLink")
                .transientVariable("userVar", Arrays.asList("kermit", "gonzo"))
                .transientVariable("groupVar", Arrays.asList("management", "sales"))
                .start();

        Task task = taskService.createTaskQuery().singleResult();
        assertThat(task).isNotNull();

        assertThat(taskService.getIdentityLinksForTask(task.getId()))
                .extracting(IdentityLink::getType, IdentityLink::getUserId, IdentityLink::getGroupId)
                .containsExactlyInAnyOrder(
                        tuple("businessAdministrator", "kermit", null),
                        tuple("businessAdministrator", "gonzo", null),
                        tuple("businessAdministrator", null, "management"),
                        tuple("businessAdministrator", null, "sales")
                );
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/api/task/TaskIdentityLinksTest.testCustomIdentityLinkExpression.bpmn20.xml")
    public void testCustomIdentityLinkArrayNodeExpression() {
        ArrayNode userVar = processEngineConfiguration.getObjectMapper().createArrayNode();
        userVar.add("kermit").add("gonzo");
        ArrayNode groupVar = processEngineConfiguration.getObjectMapper().createArrayNode();
        groupVar.add("management").add("sales");
        runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("customIdentityLink")
                .variable("userVar", userVar)
                .variable("groupVar", groupVar)
                .start();

        Task task = taskService.createTaskQuery().singleResult();
        assertThat(task).isNotNull();

        assertThat(taskService.getIdentityLinksForTask(task.getId()))
                .extracting(IdentityLink::getType, IdentityLink::getUserId, IdentityLink::getGroupId)
                .containsExactlyInAnyOrder(
                        tuple("businessAdministrator", "kermit", null),
                        tuple("businessAdministrator", "gonzo", null),
                        tuple("businessAdministrator", null, "management"),
                        tuple("businessAdministrator", null, "sales")
                );
    }

    //Tests adding identity link in same transaction as task completion
    @Test
    @Deployment(resources = "org/flowable/engine/test/api/task/IdentityLinksProcess.bpmn20.xml")
    public void testAddGroupIdentityLinkAndCompleteTaskInSameTransaction() {
        runtimeService.startProcessInstanceByKey("IdentityLinksProcess");
        String taskId = taskService.createTaskQuery().singleResult().getId();

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
            assertThat(historyService.getHistoricIdentityLinksForTask(taskId)).isEmpty();
        }

        managementService.executeCommand(context -> {
            taskService.addGroupIdentityLink(taskId, "group1", IdentityLinkType.PARTICIPANT);
            taskService.complete(taskId);
            return null;
        });

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {

            assertThat(historyService.getHistoricIdentityLinksForTask(taskId))
                    .extracting(IdentityLinkInfo::getGroupId)
                    .containsExactlyInAnyOrder("group1");
        }
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/api/task/IdentityLinksProcess.bpmn20.xml")
    public void testAddUserIdentityLinkAndCompleteTaskInSameTransaction() {
        runtimeService.startProcessInstanceByKey("IdentityLinksProcess");
        String taskId = taskService.createTaskQuery().singleResult().getId();

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
            assertThat(historyService.getHistoricIdentityLinksForTask(taskId)).isEmpty();
        }

        managementService.executeCommand(context -> {
            taskService.addUserIdentityLink(taskId, "user1", IdentityLinkType.ASSIGNEE);
            taskService.complete(taskId);

            return null;
        });

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {

            assertThat(historyService.getHistoricIdentityLinksForTask(taskId))
                    .extracting(IdentityLinkInfo::getUserId)
                    .containsExactlyInAnyOrder("user1");
        }
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/api/task/IdentityLinksProcess.bpmn20.xml")
    public void testCompleteTaskAndAddGroupIdentityLinkAfterInSameTransaction() {
        runtimeService.startProcessInstanceByKey("IdentityLinksProcess");
        String taskId = taskService.createTaskQuery().singleResult().getId();

        assertThatThrownBy(() -> managementService.executeCommand(context -> {
            taskService.complete(taskId);
            taskService.addGroupIdentityLink(taskId, "group1", IdentityLinkType.PARTICIPANT);
            return null;
        }))
                .isInstanceOf(FlowableException.class)
                .hasMessageContaining("Task is already deleted");
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/api/task/IdentityLinksProcess.bpmn20.xml")
    public void testCompleteTaskAndAddUserIdentityLinkAfterInSameTransaction() {
        runtimeService.startProcessInstanceByKey("IdentityLinksProcess");
        String taskId = taskService.createTaskQuery().singleResult().getId();

        assertThatThrownBy(() -> managementService.executeCommand(context -> {
            taskService.complete(taskId);
            taskService.addUserIdentityLink(taskId, "user1", IdentityLinkType.ASSIGNEE);
            return null;
        }))
                .isInstanceOf(FlowableException.class)
                .hasMessageContaining("Task is already deleted");
    }

    private void assertTaskEvent(String taskId, int expectedCount, String expectedAction,
                    String expectedIdentityId, String expectedIdentityType) {
        
        List<Event> taskEvents = taskService.getTaskEvents(taskId);
        assertThat(taskEvents).hasSize(expectedCount);

        if (expectedCount == 0) {
            return;
        }

        Event lastEvent = taskEvents.get(0);
        assertThat(lastEvent.getAction()).isEqualTo(expectedAction);
        List<String> taskEventMessageParts = lastEvent.getMessageParts();
        assertThat(taskEventMessageParts)
                .containsOnly(expectedIdentityId, expectedIdentityType);
    }
    
    private Event findTaskEvent(List<Event> taskEvents, String action) {
        for (Event event : taskEvents) {
            if (action.equals(event.getAction())) {
                return event;
            }
        }
        throw new AssertionError("no task event found with action " + action);
    }
}
