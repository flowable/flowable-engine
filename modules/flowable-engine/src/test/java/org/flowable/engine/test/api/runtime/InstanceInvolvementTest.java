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

package org.flowable.engine.test.api.runtime;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.engine.impl.test.HistoryTestHelper;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.identitylink.api.IdentityLink;
import org.flowable.identitylink.api.IdentityLinkType;
import org.flowable.idm.api.Group;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Marcus Klimstra
 */
public class InstanceInvolvementTest extends PluggableFlowableTestCase {

    @BeforeEach
    public void setUp() throws Exception {
        Group testGroup = identityService.newGroup("testGroup");
        identityService.saveGroup(testGroup);
        testGroup = identityService.newGroup("testGroup2");
        identityService.saveGroup(testGroup);
    }

    @AfterEach
    public void tearDown() {
        identityService.deleteGroup("testGroup");
        identityService.deleteGroup("testGroup2");
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/runtime/threeParallelTasks.bpmn20.xml" })
    public void testInvolvements() {
        // "user1", "user2", "user3" and "user4 should not be involved with any process instance
        assertNoInvolvement("user1");
        assertNoInvolvement("user2");
        assertNoInvolvement("user3");
        assertNoInvolvement("user4");

        // start a new process instance as "user1"
        String instanceId = startProcessAsUser("threeParallelTasks", "user1");

        // there are supposed to be 3 tasks
        List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().processInstanceId(instanceId).list();
        assertEquals(3, tasks.size());

        // "user1" should now be involved as the starter of the new process
        // instance. "user2" is still not involved.
        assertInvolvement("user1", instanceId);
        assertNoInvolvement("user2");

        // "user2" should be involved with the new process instance after
        // claiming a task
        taskService.claim(tasks.get(0).getId(), "user2");
        assertInvolvement("user2", instanceId);

        // "user2" should still be involved with the new process instance even
        // after completing his task
        taskService.complete(tasks.get(0).getId());
        assertInvolvement("user2", instanceId);

        // "user3" should be involved after completing a task even without
        // claiming it
        completeTaskAsUser(tasks.get(1).getId(), "user3");
        assertInvolvement("user3", instanceId);

        // "user4" should be involved after manually adding an identity link
        runtimeService.addUserIdentityLink(instanceId, "user4", "custom");
        assertInvolvement("user4", instanceId);

        // verify all identity links for this instance
        // note that since "user1" already is the starter, he is not involved as
        // a participant as well
        List<IdentityLink> identityLinks = runtimeService.getIdentityLinksForProcessInstance(instanceId);
        assertTrue(containsIdentityLink(identityLinks, "user1", "starter"));
        assertTrue(containsIdentityLink(identityLinks, "user2", "participant"));
        assertTrue(containsIdentityLink(identityLinks, "user3", "participant"));
        assertTrue(containsIdentityLink(identityLinks, "user4", "custom"));
        assertEquals(4, identityLinks.size());

        // "user1" completes the remaining task, ending the process
        completeTaskAsUser(tasks.get(2).getId(), "user1");

        // none of the users should now be involved with any process instance
        assertNoInvolvement("user1");
        assertNoInvolvement("user2");
        assertNoInvolvement("user3");
        assertNoInvolvement("user4");
        
        waitForHistoryJobExecutorToProcessAllJobs(7000, 100);
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/runtime/threeParallelTasks.bpmn20.xml" })
    public void testInstanceRemoval() {
        String instanceId = startProcessAsUser("threeParallelTasks", "user1");
        assertInvolvement("user1", instanceId);
        runtimeService.deleteProcessInstance(instanceId, "Testing instance removal");
        assertNoInvolvement("user1");
        // this will fail with a "DB NOT CLEAN" if the identity links are not
        // removed
    }

    /**
     * Test for ACT-1686
     */
    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/runtime/oneTaskProcess.bpmn20.xml" })
    public void testUserMultipleTimesinvolvedWithProcessInstance() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");

        // Add 2 links of a different type for the same user
        runtimeService.addUserIdentityLink(processInstance.getId(), "kermit", "type1");
        runtimeService.addUserIdentityLink(processInstance.getId(), "kermit", "type2");

        assertEquals(1L, runtimeService.createProcessInstanceQuery().involvedUser("kermit").count());
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/runtime/oneTaskProcess.bpmn20.xml" })
    public void testInvolvedGroupsWithProcessInstance() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");

        runtimeService.addGroupIdentityLink(processInstance.getId(), "testGroup", IdentityLinkType.PARTICIPANT);

        assertEquals(1L, runtimeService.createProcessInstanceQuery().involvedGroups(Collections.singleton("testGroup")).count());
        assertEquals(processInstance.getId(), runtimeService.createProcessInstanceQuery().involvedGroups(Collections.singleton("testGroup")).list().get(0).getId());
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/runtime/oneTaskProcess.bpmn20.xml" })
    public void testOneInvolvedGroupsInTwo() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");

        runtimeService.addGroupIdentityLink(processInstance.getId(), "testGroup", IdentityLinkType.PARTICIPANT);
        runtimeService.addGroupIdentityLink(processInstance.getId(), "testGroup2", IdentityLinkType.CANDIDATE);

        assertEquals(1L, runtimeService.createProcessInstanceQuery().involvedGroups(Collections.singleton("testGroup")).count());
        assertEquals(processInstance.getId(), runtimeService.createProcessInstanceQuery().involvedGroups(Collections.singleton("testGroup")).list().get(0).getId());
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/runtime/oneTaskProcess.bpmn20.xml" })
    public void testTwoInvolvedGroupsInTwo() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");

        runtimeService.addGroupIdentityLink(processInstance.getId(), "testGroup", IdentityLinkType.PARTICIPANT);
        runtimeService.addGroupIdentityLink(processInstance.getId(), "testGroup2", IdentityLinkType.CANDIDATE);

        assertEquals(1L, runtimeService.createProcessInstanceQuery().involvedGroups(Stream.of("testGroup", "testGroup2").collect(Collectors.toSet())).count());
        assertEquals(processInstance.getId(), runtimeService.createProcessInstanceQuery().involvedGroups(Collections.singleton("testGroup")).list().get(0).getId());
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/runtime/oneTaskProcess.bpmn20.xml" })
    public void testTwoInvolvedGroupsInOne() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");

        runtimeService.addGroupIdentityLink(processInstance.getId(), "testGroup", IdentityLinkType.PARTICIPANT);

        assertEquals(1L, runtimeService.createProcessInstanceQuery().involvedGroups(Stream.of("testGroup", "testGroup2").collect(Collectors.toSet())).count());
        assertEquals(processInstance.getId(), runtimeService.createProcessInstanceQuery().involvedGroups(Collections.singleton("testGroup")).list().get(0).getId());
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/runtime/oneTaskProcess.bpmn20.xml" })
    public void testNoInvolvedGroupsInTwo() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");

        runtimeService.addGroupIdentityLink(processInstance.getId(), "testGroup", IdentityLinkType.PARTICIPANT);
        runtimeService.addGroupIdentityLink(processInstance.getId(), "testGroup2", IdentityLinkType.CANDIDATE);

        assertEquals(0L, runtimeService.createProcessInstanceQuery().involvedGroups(Collections.singleton("nonInvolvedGroup")).count());
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/runtime/oneTaskProcess.bpmn20.xml" })
    public void testOneInvolvedGroupsInMultiple() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");

        runtimeService.addGroupIdentityLink(processInstance.getId(), "testGroup", IdentityLinkType.PARTICIPANT);
        runtimeService.addGroupIdentityLink(processInstance.getId(), "testGroup", IdentityLinkType.CANDIDATE);
        runtimeService.addGroupIdentityLink(processInstance.getId(), "testGroup2", IdentityLinkType.CANDIDATE);

        assertEquals(1L, runtimeService.createProcessInstanceQuery().involvedGroups(Collections.singleton("testGroup")).count());
        assertEquals(processInstance.getId(),
            runtimeService.createProcessInstanceQuery().involvedGroups(Collections.singleton("testGroup")).list().get(0).getId());
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/runtime/oneTaskProcess.bpmn20.xml" })
    public void testOneInvolvedGroupInNone() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");

        runtimeService.addGroupIdentityLink(processInstance.getId(), "testGroup", IdentityLinkType.PARTICIPANT);
        runtimeService.deleteGroupIdentityLink(processInstance.getId(), "testGroup", IdentityLinkType.PARTICIPANT);

        assertEquals(0L, runtimeService.createProcessInstanceQuery().involvedGroups(Collections.singleton("testGroup")).count());
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/runtime/oneTaskProcess.bpmn20.xml" })
    public void testOrInvolvedGroupsWithProcessInstance() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");

        runtimeService.addGroupIdentityLink(processInstance.getId(), "testGroup", IdentityLinkType.PARTICIPANT);

        assertEquals(1L, runtimeService.createProcessInstanceQuery().
            or().processInstanceId("undefinedId").involvedGroups(Collections.singleton("testGroup")).endOr().count());
        assertEquals(processInstance.getId(), runtimeService.createProcessInstanceQuery().involvedGroups(Collections.singleton("testGroup")).list().get(0).getId());
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/runtime/oneTaskProcess.bpmn20.xml" })
    public void testOrOneInvolvedGroupsInTwo() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");

        runtimeService.addGroupIdentityLink(processInstance.getId(), "testGroup", IdentityLinkType.PARTICIPANT);
        runtimeService.addGroupIdentityLink(processInstance.getId(), "testGroup2", IdentityLinkType.CANDIDATE);

        assertEquals(1L, runtimeService.createProcessInstanceQuery().
            or().processInstanceId("undefinedId").involvedGroups(Collections.singleton("testGroup")).endOr().count());
        assertEquals(processInstance.getId(), runtimeService.createProcessInstanceQuery().involvedGroups(Collections.singleton("testGroup")).list().get(0).getId());
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/runtime/oneTaskProcess.bpmn20.xml" })
    public void testOrTwoInvolvedGroupsInTwo() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");

        runtimeService.addGroupIdentityLink(processInstance.getId(), "testGroup", IdentityLinkType.PARTICIPANT);
        runtimeService.addGroupIdentityLink(processInstance.getId(), "testGroup2", IdentityLinkType.CANDIDATE);

        assertEquals(1L, runtimeService.createProcessInstanceQuery().
            or().processInstanceId("undefinedId").involvedGroups(Stream.of("testGroup", "testGroup2").collect(Collectors.toSet())).endOr().count());
        assertEquals(processInstance.getId(), runtimeService.createProcessInstanceQuery().involvedGroups(Collections.singleton("testGroup")).list().get(0).getId());
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/runtime/oneTaskProcess.bpmn20.xml" })
    public void testOrTwoInvolvedGroupsInOne() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");

        runtimeService.addGroupIdentityLink(processInstance.getId(), "testGroup", IdentityLinkType.PARTICIPANT);

        assertEquals(1L, runtimeService.createProcessInstanceQuery().
            or().processInstanceId("undefinedId").involvedGroups(Stream.of("testGroup", "testGroup2").collect(Collectors.toSet())).endOr().count());
        assertEquals(processInstance.getId(), runtimeService.createProcessInstanceQuery().
            or().processInstanceId("undefinedId").involvedGroups(Collections.singleton("testGroup")).endOr().list().get(0).getId());
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/runtime/oneTaskProcess.bpmn20.xml" })
    public void testOrNoInvolvedGroupsInTwo() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");

        runtimeService.addGroupIdentityLink(processInstance.getId(), "testGroup", IdentityLinkType.PARTICIPANT);
        runtimeService.addGroupIdentityLink(processInstance.getId(), "testGroup2", IdentityLinkType.CANDIDATE);

        assertEquals(0L, runtimeService.createProcessInstanceQuery().
            or().processInstanceId("undefinedId").involvedGroups(Collections.singleton("nonInvolvedGroup")).endOr().count());
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/runtime/oneTaskProcess.bpmn20.xml" })
    public void testOrOneInvolvedGroupsInMultiple() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");

        runtimeService.addGroupIdentityLink(processInstance.getId(), "testGroup", IdentityLinkType.PARTICIPANT);
        runtimeService.addGroupIdentityLink(processInstance.getId(), "testGroup", IdentityLinkType.CANDIDATE);
        runtimeService.addGroupIdentityLink(processInstance.getId(), "testGroup2", IdentityLinkType.CANDIDATE);

        assertEquals(1L, runtimeService.createProcessInstanceQuery().
            or().processInstanceId("undefinedId").involvedGroups(Collections.singleton("testGroup")).endOr().count());
        assertEquals(processInstance.getId(),
            runtimeService.createProcessInstanceQuery().
                or().processInstanceId("undefinedId").involvedGroups(Collections.singleton("testGroup")).endOr().list().get(0).getId());
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/runtime/oneTaskProcess.bpmn20.xml" })
    public void testOrOneInvolvedGroupInNone() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");

        runtimeService.addGroupIdentityLink(processInstance.getId(), "testGroup", IdentityLinkType.PARTICIPANT);
        runtimeService.deleteGroupIdentityLink(processInstance.getId(), "testGroup", IdentityLinkType.PARTICIPANT);

        assertEquals(0L, runtimeService.createProcessInstanceQuery().
            or().processInstanceId("undefinedId").involvedGroups(Collections.singleton("testGroup")).endOr().count());
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/runtime/oneTaskProcess.bpmn20.xml" })
    public void testOrOneInvolvedGroupWithUser() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");

        runtimeService.addGroupIdentityLink(processInstance.getId(), "testGroup", IdentityLinkType.PARTICIPANT);
        runtimeService.addUserIdentityLink(processInstance.getId(), "kermit", IdentityLinkType.PARTICIPANT);

        assertEquals(1L, runtimeService.createProcessInstanceQuery().
            or().involvedUser("kermit").involvedGroups(Collections.singleton("testGroup")).endOr().count());
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/runtime/oneTaskProcess.bpmn20.xml" })
    public void testOneInvolvedGroupWithUser() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");

        runtimeService.addGroupIdentityLink(processInstance.getId(), "testGroup", IdentityLinkType.PARTICIPANT);
        runtimeService.addUserIdentityLink(processInstance.getId(), "kermit", IdentityLinkType.PARTICIPANT);

        assertEquals(1L, runtimeService.createProcessInstanceQuery().
            involvedUser("kermit").involvedGroups(Collections.singleton("testGroup")).count());
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/runtime/oneTaskProcess.bpmn20.xml" })
    public void testOneInvolvedGroupTogetherWithUser() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");

        runtimeService.addGroupIdentityLink(processInstance.getId(), "testGroup", IdentityLinkType.PARTICIPANT);

        assertEquals(0L, runtimeService.createProcessInstanceQuery().
            involvedUser("kermit").involvedGroups(Collections.singleton("testGroup")).count());
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/runtime/oneTaskProcess.bpmn20.xml" })
    public void testOneInvolvedUserTogetherWithGroup() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");

        runtimeService.addUserIdentityLink(processInstance.getId(), "kermit", IdentityLinkType.PARTICIPANT);

        assertEquals(0L, runtimeService.createProcessInstanceQuery().
            involvedUser("kermit").involvedGroups(Collections.singleton("testGroup")).count());
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/runtime/oneTaskProcess.bpmn20.xml" })
    public void testOrOneInvolvedUserTogetherWithGroup() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");

        runtimeService.addUserIdentityLink(processInstance.getId(), "kermit", IdentityLinkType.PARTICIPANT);

        assertEquals(1L, runtimeService.createProcessInstanceQuery().
            or().
            involvedUser("kermit").involvedGroups(Collections.singleton("testGroup")).
            endOr().
            count());
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/runtime/oneTaskProcess.bpmn20.xml" })
    public void testInvolvedGroupsWithHistoricProcessInstance() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
        runtimeService.addGroupIdentityLink(processInstance.getId(), "testGroup", IdentityLinkType.PARTICIPANT);
        taskService.complete(taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult().getId());

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
            assertEquals(1L, historyService.createHistoricProcessInstanceQuery().involvedGroups(Collections.singleton("testGroup")).count());
            assertEquals(processInstance.getId(), historyService.createHistoricProcessInstanceQuery().involvedGroups(Collections.singleton("testGroup")).list().get(0).getId());
        }
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/runtime/oneTaskProcess.bpmn20.xml" })
    public void testHistoryOneInvolvedGroupsInTwo() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
        runtimeService.addGroupIdentityLink(processInstance.getId(), "testGroup", IdentityLinkType.PARTICIPANT);
        runtimeService.addGroupIdentityLink(processInstance.getId(), "testGroup2", IdentityLinkType.CANDIDATE);
        taskService.complete(taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult().getId());
        
        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
            assertEquals(1L, historyService.createHistoricProcessInstanceQuery().involvedGroups(Collections.singleton("testGroup")).count());
            assertEquals(processInstance.getId(),
                    historyService.createHistoricProcessInstanceQuery().involvedGroups(Collections.singleton("testGroup")).list().get(0).getId());
        }
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/runtime/oneTaskProcess.bpmn20.xml" })
    public void testHistoryTwoInvolvedGroupsInTwo() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
        runtimeService.addGroupIdentityLink(processInstance.getId(), "testGroup", IdentityLinkType.PARTICIPANT);
        runtimeService.addGroupIdentityLink(processInstance.getId(), "testGroup2", IdentityLinkType.CANDIDATE);
        taskService.complete(taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult().getId());

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
            assertEquals(1L, historyService.createHistoricProcessInstanceQuery().involvedGroups(Stream.of("testGroup", "testGroup2").collect(Collectors.toSet())).count());
            assertEquals(processInstance.getId(),
                    historyService.createHistoricProcessInstanceQuery().involvedGroups(Collections.singleton("testGroup")).list().get(0).getId());
        }
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/runtime/oneTaskProcess.bpmn20.xml" })
    public void testHistoryTwoInvolvedGroupsInOne() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");

        runtimeService.addGroupIdentityLink(processInstance.getId(), "testGroup", IdentityLinkType.PARTICIPANT);

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
            assertEquals(1L, historyService.createHistoricProcessInstanceQuery().involvedGroups(Stream.of("testGroup", "testGroup2").collect(Collectors.toSet())).count());
            assertEquals(processInstance.getId(), historyService.createHistoricProcessInstanceQuery().involvedGroups(Collections.singleton("testGroup")).list().get(0).getId());
        }
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/runtime/oneTaskProcess.bpmn20.xml" })
    public void testHistoryNoInvolvedGroupsInTwo() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
        runtimeService.addGroupIdentityLink(processInstance.getId(), "testGroup", IdentityLinkType.PARTICIPANT);
        runtimeService.addGroupIdentityLink(processInstance.getId(), "testGroup2", IdentityLinkType.CANDIDATE);
        taskService.complete(taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult().getId());

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
            assertEquals(0L, historyService.createHistoricProcessInstanceQuery().involvedGroups(Collections.singleton("nonInvolvedGroup")).count());
        }
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/runtime/oneTaskProcess.bpmn20.xml" })
    public void testHistoryOneInvolvedGroupsInMultiple() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
        runtimeService.addGroupIdentityLink(processInstance.getId(), "testGroup", IdentityLinkType.PARTICIPANT);
        runtimeService.addGroupIdentityLink(processInstance.getId(), "testGroup", IdentityLinkType.CANDIDATE);
        runtimeService.addGroupIdentityLink(processInstance.getId(), "testGroup2", IdentityLinkType.CANDIDATE);
        taskService.complete(taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult().getId());

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
            assertEquals(1L, historyService.createHistoricProcessInstanceQuery().involvedGroups(Collections.singleton("testGroup")).count());
            assertEquals(processInstance.getId(),
                    historyService.createHistoricProcessInstanceQuery().involvedGroups(Collections.singleton("testGroup")).list().get(0).getId());
        }
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/runtime/oneTaskProcess.bpmn20.xml" })
    public void testHistoryOneInvolvedGroupInNone() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
        runtimeService.addGroupIdentityLink(processInstance.getId(), "testGroup", IdentityLinkType.PARTICIPANT);
        runtimeService.deleteGroupIdentityLink(processInstance.getId(), "testGroup", IdentityLinkType.PARTICIPANT);
        taskService.complete(taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult().getId());

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
            assertEquals(0L, historyService.createHistoricProcessInstanceQuery().involvedGroups(Collections.singleton("testGroup")).count());
        }
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/runtime/oneTaskProcess.bpmn20.xml" })
    public void testHistoryOrInvolvedGroupsWithProcessInstance() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
        runtimeService.addGroupIdentityLink(processInstance.getId(), "testGroup", IdentityLinkType.PARTICIPANT);
        taskService.complete(taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult().getId());
        
        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
            assertEquals(1L, historyService.createHistoricProcessInstanceQuery().
                or().processInstanceId("undefinedId").involvedGroups(Collections.singleton("testGroup")).endOr().count());
            assertEquals(processInstance.getId(), historyService.createHistoricProcessInstanceQuery().involvedGroups(Collections.singleton("testGroup")).list().get(0).getId());
        }
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/runtime/oneTaskProcess.bpmn20.xml" })
    public void testHistoryOrOneInvolvedGroupsInTwo() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
        runtimeService.addGroupIdentityLink(processInstance.getId(), "testGroup", IdentityLinkType.PARTICIPANT);
        runtimeService.addGroupIdentityLink(processInstance.getId(), "testGroup2", IdentityLinkType.CANDIDATE);
        taskService.complete(taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult().getId());

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
            assertEquals(1L, historyService.createHistoricProcessInstanceQuery().
                    or().processInstanceId("undefinedId").involvedGroups(Collections.singleton("testGroup")).endOr().count());
            assertEquals(processInstance.getId(), historyService.createHistoricProcessInstanceQuery().involvedGroups(Collections.singleton("testGroup")).list().get(0).getId());
        }
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/runtime/oneTaskProcess.bpmn20.xml" })
    public void testHistoryOrTwoInvolvedGroupsInTwo() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
        runtimeService.addGroupIdentityLink(processInstance.getId(), "testGroup", IdentityLinkType.PARTICIPANT);
        runtimeService.addGroupIdentityLink(processInstance.getId(), "testGroup2", IdentityLinkType.CANDIDATE);
        taskService.complete(taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult().getId());

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
            assertEquals(1L, historyService.createHistoricProcessInstanceQuery().
                    or().processInstanceId("undefinedId").involvedGroups(Stream.of("testGroup", "testGroup2").collect(Collectors.toSet())).endOr().count());
            assertEquals(processInstance.getId(), historyService.createHistoricProcessInstanceQuery().involvedGroups(Collections.singleton("testGroup")).list().get(0).getId());
        }
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/runtime/oneTaskProcess.bpmn20.xml" })
    public void testHistoryOrTwoInvolvedGroupsInOne() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
        runtimeService.addGroupIdentityLink(processInstance.getId(), "testGroup", IdentityLinkType.PARTICIPANT);
        taskService.complete(taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult().getId());

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
            assertEquals(1L, historyService.createHistoricProcessInstanceQuery().
                    or().processInstanceId("undefinedId").involvedGroups(Stream.of("testGroup", "testGroup2").collect(Collectors.toSet())).endOr().count());
            assertEquals(processInstance.getId(), historyService.createHistoricProcessInstanceQuery().
                    or().processInstanceId("undefinedId").involvedGroups(Collections.singleton("testGroup")).endOr().list().get(0).getId());
        }
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/runtime/oneTaskProcess.bpmn20.xml" })
    public void testHistoryOrNoInvolvedGroupsInTwo() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
        runtimeService.addGroupIdentityLink(processInstance.getId(), "testGroup", IdentityLinkType.PARTICIPANT);
        runtimeService.addGroupIdentityLink(processInstance.getId(), "testGroup2", IdentityLinkType.CANDIDATE);
        taskService.complete(taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult().getId());

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
            assertEquals(0L, historyService.createHistoricProcessInstanceQuery().
                    or().processInstanceId("undefinedId").involvedGroups(Collections.singleton("nonInvolvedGroup")).endOr().count());
        }
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/runtime/oneTaskProcess.bpmn20.xml" })
    public void testHistoryOrOneInvolvedGroupsInMultiple() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
        runtimeService.addGroupIdentityLink(processInstance.getId(), "testGroup", IdentityLinkType.PARTICIPANT);
        runtimeService.addGroupIdentityLink(processInstance.getId(), "testGroup", IdentityLinkType.CANDIDATE);
        runtimeService.addGroupIdentityLink(processInstance.getId(), "testGroup2", IdentityLinkType.CANDIDATE);
        taskService.complete(taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult().getId());

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
            assertEquals(1L, historyService.createHistoricProcessInstanceQuery().
                    or().processInstanceId("undefinedId").involvedGroups(Collections.singleton("testGroup")).endOr().count());
            assertEquals(processInstance.getId(),
                    historyService.createHistoricProcessInstanceQuery().
                    or().processInstanceId("undefinedId").involvedGroups(Collections.singleton("testGroup")).endOr().list().get(0).getId());
        }
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/runtime/oneTaskProcess.bpmn20.xml" })
    public void testHistoryOrOneInvolvedGroupInNone() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
        runtimeService.addGroupIdentityLink(processInstance.getId(), "testGroup", IdentityLinkType.PARTICIPANT);
        runtimeService.deleteGroupIdentityLink(processInstance.getId(), "testGroup", IdentityLinkType.PARTICIPANT);
        taskService.complete(taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult().getId());

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
            assertEquals(0L, historyService.createHistoricProcessInstanceQuery().
                    or().processInstanceId("undefinedId").involvedGroups(Collections.singleton("testGroup")).endOr().count());
        }
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/runtime/oneTaskProcess.bpmn20.xml" })
    public void testHistoryOrOneInvolvedGroupWithUser() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
        runtimeService.addGroupIdentityLink(processInstance.getId(), "testGroup", IdentityLinkType.PARTICIPANT);
        runtimeService.addUserIdentityLink(processInstance.getId(), "kermit", IdentityLinkType.PARTICIPANT);
        taskService.complete(taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult().getId());

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
            assertEquals(1L, historyService.createHistoricProcessInstanceQuery().
                    or().involvedUser("kermit").involvedGroups(Collections.singleton("testGroup")).endOr().count());
        }
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/runtime/oneTaskProcess.bpmn20.xml" })
    public void testHistoryOneInvolvedGroupWithUser() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
        runtimeService.addGroupIdentityLink(processInstance.getId(), "testGroup", IdentityLinkType.PARTICIPANT);
        runtimeService.addUserIdentityLink(processInstance.getId(), "kermit", IdentityLinkType.PARTICIPANT);
        taskService.complete(taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult().getId());

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
            assertEquals(1L, historyService.createHistoricProcessInstanceQuery().
                    involvedUser("kermit").involvedGroups(Collections.singleton("testGroup")).count());
        }
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/runtime/oneTaskProcess.bpmn20.xml" })
    public void testHistoryOneInvolvedGroupTogetherWithUser() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
        runtimeService.addGroupIdentityLink(processInstance.getId(), "testGroup", IdentityLinkType.PARTICIPANT);
        taskService.complete(taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult().getId());

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
            assertEquals(0L, historyService.createHistoricProcessInstanceQuery().
                    involvedUser("kermit").involvedGroups(Collections.singleton("testGroup")).count());
        }
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/runtime/oneTaskProcess.bpmn20.xml" })
    public void testHistoryOneInvolvedUserTogetherWithGroup() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
        runtimeService.addUserIdentityLink(processInstance.getId(), "kermit", IdentityLinkType.PARTICIPANT);
        taskService.complete(taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult().getId());

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
            assertEquals(0L, historyService.createHistoricProcessInstanceQuery().
                    involvedUser("kermit").involvedGroups(Collections.singleton("testGroup")).count());
        }
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/runtime/oneTaskProcess.bpmn20.xml" })
    public void testHistoryOrOneInvolvedUserTogetherWithGroup() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
        runtimeService.addUserIdentityLink(processInstance.getId(), "kermit", IdentityLinkType.PARTICIPANT);
        taskService.complete(taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult().getId());

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
            assertEquals(1L, historyService.createHistoricProcessInstanceQuery().
                    or().
                    involvedUser("kermit").involvedGroups(Collections.singleton("testGroup")).
                    endOr().
                    count());
        }
    }

    private void assertNoInvolvement(String userId) {
        assertEquals(0L, runtimeService.createProcessInstanceQuery().involvedUser(userId).count());
    }

    private void assertInvolvement(String userId, String instanceId) {
        ProcessInstance involvedInstance = runtimeService.createProcessInstanceQuery().involvedUser(userId).singleResult();
        assertEquals(instanceId, involvedInstance.getId());
    }

    private String startProcessAsUser(String processId, String userId) {
        try {
            identityService.setAuthenticatedUserId(userId);
            return runtimeService.startProcessInstanceByKey(processId).getId();
        } finally {
            identityService.setAuthenticatedUserId(null);
        }
    }

    private void completeTaskAsUser(String taskId, String userId) {
        try {
            identityService.setAuthenticatedUserId(userId);
            taskService.complete(taskId);
        } finally {
            identityService.setAuthenticatedUserId(null);
        }
    }

    private boolean containsIdentityLink(List<IdentityLink> identityLinks, String userId, String type) {
        for (IdentityLink identityLink : identityLinks) {
            if (userId.equals(identityLink.getUserId()) && type.equals(identityLink.getType())) {
                return true;
            }
        }
        return false;
    }

}
