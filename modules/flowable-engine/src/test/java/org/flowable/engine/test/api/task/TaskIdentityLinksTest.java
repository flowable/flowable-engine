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

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.engine.impl.test.HistoryTestHelper;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.task.Event;
import org.flowable.engine.test.Deployment;
import org.flowable.identitylink.api.IdentityLink;
import org.flowable.identitylink.api.history.HistoricIdentityLink;
import org.flowable.identitylink.api.IdentityLinkType;

import junit.framework.AssertionFailedError;

/**
 * @author Tom Baeyens
 * @author Falko Menge
 */
public class TaskIdentityLinksTest extends PluggableFlowableTestCase {
    
    private static final String IDENTITY_LINKS_PROCESS_BPMN20_XML = "org/flowable/engine/test/api/task/IdentityLinksProcess.bpmn20.xml";
    private static final String IDENTITY_LINKS_PROCESS = "IdentityLinksProcess";

    @Deployment(resources = "org/flowable/engine/test/api/task/IdentityLinksProcess.bpmn20.xml")
    public void testCandidateUserLink() {
        runtimeService.startProcessInstanceByKey("IdentityLinksProcess");

        String taskId = taskService.createTaskQuery().singleResult().getId();

        taskService.addCandidateUser(taskId, "kermit");

        List<IdentityLink> identityLinks = taskService.getIdentityLinksForTask(taskId);
        IdentityLink identityLink = identityLinks.get(0);

        assertNull(identityLink.getGroupId());
        assertEquals("kermit", identityLink.getUserId());
        assertEquals(IdentityLinkType.CANDIDATE, identityLink.getType());
        assertEquals(taskId, identityLink.getTaskId());

        assertEquals(1, identityLinks.size());

        taskService.deleteCandidateUser(taskId, "kermit");

        assertEquals(0, taskService.getIdentityLinksForTask(taskId).size());
    }

    @Deployment(resources = "org/flowable/engine/test/api/task/IdentityLinksProcess.bpmn20.xml")
    public void testCandidateGroupLink() {
        runtimeService.startProcessInstanceByKey("IdentityLinksProcess");

        String taskId = taskService.createTaskQuery().singleResult().getId();

        taskService.addCandidateGroup(taskId, "muppets");

        List<IdentityLink> identityLinks = taskService.getIdentityLinksForTask(taskId);
        IdentityLink identityLink = identityLinks.get(0);

        assertEquals("muppets", identityLink.getGroupId());
        assertNull("kermit", identityLink.getUserId());
        assertEquals(IdentityLinkType.CANDIDATE, identityLink.getType());
        assertEquals(taskId, identityLink.getTaskId());

        assertEquals(1, identityLinks.size());

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
            List<Event> taskEvents = taskService.getTaskEvents(taskId);
            assertEquals(1, taskEvents.size());
            Event taskEvent = taskEvents.get(0);
            assertEquals(Event.ACTION_ADD_GROUP_LINK, taskEvent.getAction());
            List<String> taskEventMessageParts = taskEvent.getMessageParts();
            assertEquals("muppets", taskEventMessageParts.get(0));
            assertEquals(IdentityLinkType.CANDIDATE, taskEventMessageParts.get(1));
            assertEquals(2, taskEventMessageParts.size());
        }

        taskService.deleteCandidateGroup(taskId, "muppets");

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
            List<Event> taskEvents = taskService.getTaskEvents(taskId);
            Event taskEvent = findTaskEvent(taskEvents, Event.ACTION_DELETE_GROUP_LINK);
            assertEquals(Event.ACTION_DELETE_GROUP_LINK, taskEvent.getAction());
            List<String> taskEventMessageParts = taskEvent.getMessageParts();
            assertEquals("muppets", taskEventMessageParts.get(0));
            assertEquals(IdentityLinkType.CANDIDATE, taskEventMessageParts.get(1));
            assertEquals(2, taskEventMessageParts.size());
            assertEquals(2, taskEvents.size());
        }

        assertEquals(0, taskService.getIdentityLinksForTask(taskId).size());
    }
    
    @Deployment(resources = IDENTITY_LINKS_PROCESS_BPMN20_XML)
    public void testAssigneeIdentityLinkHistory() {
        if (!HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
            return;
        }

        runtimeService.startProcessInstanceByKey(IDENTITY_LINKS_PROCESS);

        String taskId = taskService.createTaskQuery().singleResult().getId();

        taskService.setAssignee(taskId, "kermit");

        assertEquals(1, taskService.getIdentityLinksForTask(taskId).size());

        assertTaskEvent(taskId, 1, Event.ACTION_ADD_USER_LINK, "kermit", IdentityLinkType.ASSIGNEE);

        taskService.setAssignee(taskId, null);

        assertEquals(0, taskService.getIdentityLinksForTask(taskId).size());

        assertTaskEvent(taskId, 2, Event.ACTION_DELETE_USER_LINK, "kermit", IdentityLinkType.ASSIGNEE);

        waitForHistoryJobExecutorToProcessAllJobs(5000, 100);
        List<HistoricIdentityLink> history = historyService.getHistoricIdentityLinksForTask(taskId);
        assertEquals(2, history.size());
        
        Collections.sort(history, new Comparator<HistoricIdentityLink>() {

            @Override
            public int compare(HistoricIdentityLink hi1, HistoricIdentityLink hi2) {
               return hi1.getCreateTime().compareTo(hi2.getCreateTime());
            }
            
        });
        
        HistoricIdentityLink assigned = history.get(0);
        assertEquals(IdentityLinkType.ASSIGNEE, assigned.getType());
        assertEquals("kermit", assigned.getUserId());
        HistoricIdentityLink unassigned = history.get(1);
        assertNull(unassigned.getUserId());
        assertEquals(IdentityLinkType.ASSIGNEE, unassigned.getType());
        assertNull(unassigned.getUserId());
    }

    @Deployment(resources = IDENTITY_LINKS_PROCESS_BPMN20_XML)
    public void testClaimingIdentityLinkHistory() {
        if (!HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
            return;
        }

        runtimeService.startProcessInstanceByKey(IDENTITY_LINKS_PROCESS);

        String taskId = taskService.createTaskQuery().singleResult().getId();

        taskService.claim(taskId, "kermit");

        assertEquals(1, taskService.getIdentityLinksForTask(taskId).size());

        assertTaskEvent(taskId, 1, Event.ACTION_ADD_USER_LINK, "kermit", IdentityLinkType.ASSIGNEE);

        taskService.unclaim(taskId);

        assertEquals(0, taskService.getIdentityLinksForTask(taskId).size());

        assertTaskEvent(taskId, 2, Event.ACTION_DELETE_USER_LINK, "kermit", IdentityLinkType.ASSIGNEE);

        waitForHistoryJobExecutorToProcessAllJobs(5000, 100);
        List<HistoricIdentityLink> history = historyService.getHistoricIdentityLinksForTask(taskId);
        assertEquals(2, history.size());
        
        Collections.sort(history, new Comparator<HistoricIdentityLink>() {

            @Override
            public int compare(HistoricIdentityLink hi1, HistoricIdentityLink hi2) {
               return hi1.getCreateTime().compareTo(hi2.getCreateTime());
            }
            
        });
        
        HistoricIdentityLink assigned = history.get(0);
        assertEquals(IdentityLinkType.ASSIGNEE, assigned.getType());
        assertEquals("kermit", assigned.getUserId());
        HistoricIdentityLink unassigned = history.get(1);
        assertNull(unassigned.getUserId());
        assertEquals(IdentityLinkType.ASSIGNEE, unassigned.getType());
        assertNull(unassigned.getUserId());
    }

    @Deployment(resources = IDENTITY_LINKS_PROCESS_BPMN20_XML)
    public void testOwnerIdentityLinkHistory() {
        if (!HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
            return;
        }

        runtimeService.startProcessInstanceByKey(IDENTITY_LINKS_PROCESS);

        String taskId = taskService.createTaskQuery().singleResult().getId();

        taskService.setOwner(taskId, "kermit");

        assertEquals(1, taskService.getIdentityLinksForTask(taskId).size());

        assertTaskEvent(taskId, 1, Event.ACTION_ADD_USER_LINK, "kermit", IdentityLinkType.OWNER);

        taskService.setOwner(taskId, null);

        assertEquals(0, taskService.getIdentityLinksForTask(taskId).size());

        assertTaskEvent(taskId, 2, Event.ACTION_DELETE_USER_LINK, "kermit", IdentityLinkType.OWNER);

        waitForHistoryJobExecutorToProcessAllJobs(5000, 100);
        List<HistoricIdentityLink> history = historyService.getHistoricIdentityLinksForTask(taskId);
        assertEquals(2, history.size());
        Collections.sort(history, new Comparator<HistoricIdentityLink>() {

            @Override
            public int compare(HistoricIdentityLink hi1, HistoricIdentityLink hi2) {
               return hi1.getCreateTime().compareTo(hi2.getCreateTime());
            }
            
        });
        
        HistoricIdentityLink assigned = history.get(0);
        assertEquals(IdentityLinkType.OWNER, assigned.getType());
        assertEquals("kermit", assigned.getUserId());
        HistoricIdentityLink unassigned = history.get(1);
        assertNull(unassigned.getUserId());
        assertEquals(IdentityLinkType.OWNER, unassigned.getType());
        assertNull(unassigned.getUserId());
    }
    
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

        assertEquals(1, taskService.getIdentityLinksForTask(taskId).size());

        assertTaskEvent(taskId, 1, Event.ACTION_ADD_USER_LINK, "kermit", IdentityLinkType.ASSIGNEE);

        waitForHistoryJobExecutorToProcessAllJobs(5000, 100);
        List<HistoricIdentityLink> history = historyService.getHistoricIdentityLinksForTask(taskId);
        assertEquals(1, history.size());
        HistoricIdentityLink assigned = history.get(0);
        assertEquals(IdentityLinkType.ASSIGNEE, assigned.getType());
        assertEquals("kermit", assigned.getUserId());
    }

    @Deployment(resources = IDENTITY_LINKS_PROCESS_BPMN20_XML)
    public void testNullIdentityIdCreatesNoLinks() {
        if (!HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
            return;
        }

        runtimeService.startProcessInstanceByKey(IDENTITY_LINKS_PROCESS);

        String taskId = taskService.createTaskQuery().singleResult().getId();

        taskService.claim(taskId, null);
        taskService.setAssignee(taskId, null);

        assertEquals(0, taskService.getIdentityLinksForTask(taskId).size());

        assertTaskEvent(taskId, 0, null, null, null);
        
        waitForHistoryJobExecutorToProcessAllJobs(5000, 100);
        List<HistoricIdentityLink> history = historyService.getHistoricIdentityLinksForTask(taskId);
        assertEquals(0, history.size());
    }

    @Deployment(resources = "org/flowable/engine/test/api/task/IdentityLinksProcess.bpmn20.xml")
    public void testCustomTypeUserLink() {
        runtimeService.startProcessInstanceByKey("IdentityLinksProcess");

        String taskId = taskService.createTaskQuery().singleResult().getId();

        taskService.addUserIdentityLink(taskId, "kermit", "interestee");

        List<IdentityLink> identityLinks = taskService.getIdentityLinksForTask(taskId);
        IdentityLink identityLink = identityLinks.get(0);

        assertNull(identityLink.getGroupId());
        assertEquals("kermit", identityLink.getUserId());
        assertEquals("interestee", identityLink.getType());
        assertEquals(taskId, identityLink.getTaskId());

        assertEquals(1, identityLinks.size());

        taskService.deleteUserIdentityLink(taskId, "kermit", "interestee");

        assertEquals(0, taskService.getIdentityLinksForTask(taskId).size());
    }

    @Deployment(resources = "org/flowable/engine/test/api/task/IdentityLinksProcess.bpmn20.xml")
    public void testCustomLinkGroupLink() {
        runtimeService.startProcessInstanceByKey("IdentityLinksProcess");

        String taskId = taskService.createTaskQuery().singleResult().getId();

        taskService.addGroupIdentityLink(taskId, "muppets", "playing");

        List<IdentityLink> identityLinks = taskService.getIdentityLinksForTask(taskId);
        IdentityLink identityLink = identityLinks.get(0);

        assertEquals("muppets", identityLink.getGroupId());
        assertNull("kermit", identityLink.getUserId());
        assertEquals("playing", identityLink.getType());
        assertEquals(taskId, identityLink.getTaskId());

        assertEquals(1, identityLinks.size());

        taskService.deleteGroupIdentityLink(taskId, "muppets", "playing");

        assertEquals(0, taskService.getIdentityLinksForTask(taskId).size());
    }

    public void testDeleteAssignee() {
        org.flowable.task.api.Task task = taskService.newTask();
        task.setAssignee("nonExistingUser");
        taskService.saveTask(task);

        taskService.deleteUserIdentityLink(task.getId(), "nonExistingUser", IdentityLinkType.ASSIGNEE);

        task = taskService.createTaskQuery().taskId(task.getId()).singleResult();
        assertNull(task.getAssignee());
        assertEquals(0, taskService.getIdentityLinksForTask(task.getId()).size());

        // cleanup
        taskService.deleteTask(task.getId(), true);
    }

    public void testDeleteOwner() {
        org.flowable.task.api.Task task = taskService.newTask();
        task.setOwner("nonExistingUser");
        taskService.saveTask(task);

        taskService.deleteUserIdentityLink(task.getId(), "nonExistingUser", IdentityLinkType.OWNER);

        task = taskService.createTaskQuery().taskId(task.getId()).singleResult();
        assertNull(task.getOwner());
        assertEquals(0, taskService.getIdentityLinksForTask(task.getId()).size());

        // cleanup
        taskService.deleteTask(task.getId(), true);
    }

    @Deployment(resources = "org/flowable/engine/test/api/task/TaskIdentityLinksTest.testDeleteCandidateUser.bpmn20.xml")
    public void testDeleteCandidateUser() {
        runtimeService.startProcessInstanceByKey("TaskIdentityLinks");

        String taskId = taskService.createTaskQuery().singleResult().getId();

        List<IdentityLink> identityLinks = taskService.getIdentityLinksForTask(taskId);

        assertEquals(1, identityLinks.size());
        IdentityLink identityLink = identityLinks.get(0);

        assertEquals("user", identityLink.getUserId());
    }

    @Deployment(resources = "org/flowable/engine/test/api/task/IdentityLinksProcess.bpmn20.xml")
    public void testEmptyCandidateUserLink() {
        runtimeService.startProcessInstanceByKey("IdentityLinksProcess");

        String taskId = taskService.createTaskQuery().singleResult().getId();

        taskService.addCandidateGroup(taskId, "muppets");
        taskService.deleteCandidateUser(taskId, "kermit");

        List<IdentityLink> identityLinks = taskService.getIdentityLinksForTask(taskId);
        assertNotNull(identityLinks);
        assertEquals(1, identityLinks.size());

        IdentityLink identityLink = identityLinks.get(0);
        assertEquals("muppets", identityLink.getGroupId());
        assertNull(identityLink.getUserId());
        assertEquals(IdentityLinkType.CANDIDATE, identityLink.getType());
        assertEquals(taskId, identityLink.getTaskId());

        taskService.deleteCandidateGroup(taskId, "muppets");

        assertEquals(0, taskService.getIdentityLinksForTask(taskId).size());
    }

    // Test custom identity links
    @Deployment
    public void testCustomIdentityLink() {
        runtimeService.startProcessInstanceByKey("customIdentityLink");

        List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().taskInvolvedUser("kermit").list();
        assertEquals(1, tasks.size());

        List<IdentityLink> identityLinks = taskService.getIdentityLinksForTask(tasks.get(0).getId());
        assertEquals(2, identityLinks.size());

        for (IdentityLink idLink : identityLinks) {
            assertEquals("businessAdministrator", idLink.getType());
            String userId = idLink.getUserId();
            if (userId == null) {
                assertEquals("management", idLink.getGroupId());
            } else {
                assertEquals("kermit", userId);
            }
        }
    }
    
    private void assertTaskEvent(String taskId, int expectedCount, String expectedAction,
                    String expectedIdentityId, String expectedIdentityType) {
        
        List<Event> taskEvents = taskService.getTaskEvents(taskId);
        assertEquals(expectedCount, taskEvents.size());

        if (expectedCount == 0) {
            return;
        }

        Event lastEvent = taskEvents.get(0);
        assertEquals(expectedAction, lastEvent.getAction());
        List<String> taskEventMessageParts = lastEvent.getMessageParts();
        assertEquals(expectedIdentityId, taskEventMessageParts.get(0));
        assertEquals(expectedIdentityType, taskEventMessageParts.get(1));
        assertEquals(2, taskEventMessageParts.size());
    }
    
    private Event findTaskEvent(List<Event> taskEvents, String action) {
        for (Event event : taskEvents) {
            if (action.equals(event.getAction())) {
                return event;
            }
        }
        throw new AssertionFailedError("no task event found with action " + action);
    }
}
