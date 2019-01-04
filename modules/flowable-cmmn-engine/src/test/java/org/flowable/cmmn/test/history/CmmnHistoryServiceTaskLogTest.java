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
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.function.Consumer;

import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.test.impl.CustomCmmnConfigurationFlowableTestCase;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.common.engine.impl.identity.Authentication;
import org.flowable.identitylink.api.IdentityLinkType;
import org.flowable.task.api.Task;
import org.flowable.task.api.history.HistoricTaskLogEntry;
import org.flowable.task.api.history.HistoricTaskLogEntryBuilder;
import org.flowable.task.api.history.HistoricTaskLogEntryQuery;
import org.hamcrest.MatcherAssert;
import org.junit.After;
import org.junit.Test;

/**
 * @author martin.grofcik
 */
public class CmmnHistoryServiceTaskLogTest extends CustomCmmnConfigurationFlowableTestCase {

    protected Task task;

    @Override
    protected String getEngineName() {
        return "cmmnEngineWithUserTaskEventLogging";
    }

    @Override
    protected void configureConfiguration(CmmnEngineConfiguration cmmnEngineConfiguration) {
        cmmnEngineConfiguration.setEnableHistoricTaskLogging(true);
    }

    @After
    public void deleteTasks() {
        if (task != null) {
            deleteTaskWithLogEntries(task.getId());
        }
    }

    protected void deleteTaskWithLogEntries(String taskId) {
        cmmnHistoryService.deleteHistoricTaskInstance(taskId);
        cmmnTaskService.deleteTask(taskId,true);
    }

    @Test
    public void createTaskEvent() {
        task = cmmnTaskService.createTaskBuilder().
            assignee("testAssignee").
            create();

        List<HistoricTaskLogEntry> taskLogsByTaskInstanceId = cmmnHistoryService.createHistoricTaskLogEntryQuery().taskId(task.getId()).list();
        assertThat(taskLogsByTaskInstanceId).size().isEqualTo(1);

        assertThat(taskLogsByTaskInstanceId.get(0)).
            extracting(HistoricTaskLogEntry::getTaskId).isEqualTo(task.getId());
        assertThat(taskLogsByTaskInstanceId.get(0)).
            extracting(HistoricTaskLogEntry::getType).isEqualTo("USER_TASK_CREATED");
        assertThat(taskLogsByTaskInstanceId.get(0)).
            extracting(HistoricTaskLogEntry::getTimeStamp).isEqualTo(task.getCreateTime());
        assertThat(taskLogsByTaskInstanceId.get(0)).
            extracting(HistoricTaskLogEntry::getUserId).isNull();
    }

    @Test
    public void createTaskEventAsAuthenticatedUser() {
        String previousUserId = Authentication.getAuthenticatedUserId();
        Authentication.setAuthenticatedUserId("testUser");
        try {
            task = cmmnTaskService.createTaskBuilder().
                assignee("testAssignee").
                create();

            List<HistoricTaskLogEntry> taskLogsByTaskInstanceId = cmmnHistoryService.createHistoricTaskLogEntryQuery().taskId(task.getId()).list();
            assertThat(taskLogsByTaskInstanceId).size().isEqualTo(1);

            assertThat(taskLogsByTaskInstanceId.get(0)).
                extracting(HistoricTaskLogEntry::getUserId).isEqualTo("testUser");
        } finally {
            Authentication.setAuthenticatedUserId(previousUserId);
        }
    }

    @Test
    public void queryForNonExistingTaskLogEntries() {
        task = cmmnTaskService.createTaskBuilder().create();

        List<HistoricTaskLogEntry> taskLogsByTaskInstanceId = cmmnHistoryService.createHistoricTaskLogEntryQuery().taskId("NON-EXISTING-TASK-ID").list();

        assertThat(taskLogsByTaskInstanceId).isEmpty();
    }

    @Test
    public void queryForNullTaskLogEntries_returnsAll() {
        Task taskA = cmmnTaskService.createTaskBuilder().create();
        Task taskB = cmmnTaskService.createTaskBuilder().create();
        Task taskC = cmmnTaskService.createTaskBuilder().create();

        try {
            List<HistoricTaskLogEntry> taskLogsByTaskInstanceId = cmmnHistoryService.createHistoricTaskLogEntryQuery().taskId(null).list();

            assertThat(taskLogsByTaskInstanceId).size().isEqualTo(3L);
            
        } finally {
            deleteTaskWithLogEntries(taskC.getId());
            deleteTaskWithLogEntries(taskB.getId());
            deleteTaskWithLogEntries(taskA.getId());
        }
    }

    @Test
    public void deleteTaskEventLogEntry() {
        task = cmmnTaskService.createTaskBuilder().
            assignee("testAssignee").
            create();
        List<HistoricTaskLogEntry> taskLogsByTaskInstanceId = cmmnHistoryService.createHistoricTaskLogEntryQuery().taskId(task.getId()).list();
        assertThat(
            taskLogsByTaskInstanceId
        ).size().isEqualTo(1);

        cmmnHistoryService.deleteHistoricTaskLogEntry(taskLogsByTaskInstanceId.get(0).getLogNumber());

        taskLogsByTaskInstanceId = cmmnHistoryService.createHistoricTaskLogEntryQuery().taskId(task.getId()).list();
        assertThat(
            taskLogsByTaskInstanceId
        ).isEmpty();
    }

    @Test
    public void deleteNonExistingTaskEventLogEntry() {
        task = cmmnTaskService.createTaskBuilder().create();
        
        // non existing log entry delete should be successful
        cmmnHistoryService.deleteHistoricTaskLogEntry(9999);

        assertThat(cmmnHistoryService.createHistoricTaskLogEntryQuery().taskId(task.getId()).list().size()).isEqualTo(1);
    }

    @Test
    public void taskAssigneeEvent() {
        task = cmmnTaskService.createTaskBuilder().
            assignee("initialAssignee").
            create();

        cmmnTaskService.setAssignee(task.getId(), "newAssignee");
        List<HistoricTaskLogEntry> taskLogEntries = cmmnHistoryService.createHistoricTaskLogEntryQuery().taskId(task.getId()).list();

        assertThat(taskLogEntries).size().isEqualTo(2);
        assertThat(taskLogEntries.get(1)).
            extracting(assigneeTaskLogEntry -> new String(assigneeTaskLogEntry.getData())).
            isEqualTo("{\"newAssigneeId\":\"newAssignee\",\"previousAssigneeId\":\"initialAssignee\"}");
        assertThat(taskLogEntries.get(1)).extracting(HistoricTaskLogEntry::getTimeStamp).isNotNull();
        assertThat(taskLogEntries.get(1)).extracting(HistoricTaskLogEntry::getTaskId).isEqualTo(task.getId());
        assertThat(taskLogEntries.get(1)).extracting(HistoricTaskLogEntry::getUserId).isNull();
        assertThat(taskLogEntries.get(1)).extracting(HistoricTaskLogEntry::getType).isEqualTo("USER_TASK_ASSIGNEE_CHANGED");
    }

    @Test
    public void changeAssigneeTaskEventAsAuthenticatedUser() {
        assertThatAuthenticatedUserIsSet(
            taskId -> cmmnTaskService.setAssignee(taskId, "newAssignee")
        );
    }

    @Test
    public void taskOwnerEvent() {
        task = cmmnTaskService.createTaskBuilder().
            assignee("initialAssignee").
            create();

        cmmnTaskService.setOwner(task.getId(), "newOwner");
        List<HistoricTaskLogEntry> taskLogEntries = cmmnHistoryService.createHistoricTaskLogEntryQuery().taskId(task.getId()).list();

        assertThat(taskLogEntries).size().isEqualTo(2);
        assertThat(taskLogEntries.get(1).getData()).
            contains("\"previousOwnerId\":null").
            contains("\"newOwnerId\":\"newOwner\"");
        assertThat(taskLogEntries.get(1)).extracting(HistoricTaskLogEntry::getTimeStamp).isNotNull();
        assertThat(taskLogEntries.get(1)).extracting(HistoricTaskLogEntry::getTaskId).isEqualTo(task.getId());
        assertThat(taskLogEntries.get(1)).extracting(HistoricTaskLogEntry::getUserId).isNull();
        assertThat(taskLogEntries.get(1)).extracting(HistoricTaskLogEntry::getType).isEqualTo("USER_TASK_OWNER_CHANGED");
    }

    @Test
    public void changeOwnerTaskEventAsAuthenticatedUser() {
        assertThatAuthenticatedUserIsSet(
            taskId -> cmmnTaskService.setOwner(taskId, "newOwner")
        );
    }

    @Test
    public void claimTaskEvent() {
        task = cmmnTaskService.createTaskBuilder().
            create();

        cmmnTaskService.claim(task.getId(), "testUser");

        List<HistoricTaskLogEntry> taskLogEntries = cmmnHistoryService.createHistoricTaskLogEntryQuery().taskId(task.getId()).list();
        assertThat(taskLogEntries).size().isEqualTo(2);
        assertThat(taskLogEntries.get(1).getData()).
            contains("\"newAssigneeId\":\"testUser\"").
            contains("\"previousAssigneeId\":null");
        assertThat(taskLogEntries.get(1)).extracting(HistoricTaskLogEntry::getTimeStamp).isNotNull();
        assertThat(taskLogEntries.get(1)).extracting(HistoricTaskLogEntry::getTaskId).isEqualTo(task.getId());
        assertThat(taskLogEntries.get(1)).extracting(HistoricTaskLogEntry::getUserId).isNull();
        assertThat(taskLogEntries.get(1)).extracting(HistoricTaskLogEntry::getType).isEqualTo("USER_TASK_ASSIGNEE_CHANGED");
    }

    @Test
    public void unClaimTaskEvent() {
        task = cmmnTaskService.createTaskBuilder().
            assignee("initialAssignee").
            create();

        cmmnTaskService.unclaim(task.getId());

        List<HistoricTaskLogEntry> taskLogEntries = cmmnHistoryService.createHistoricTaskLogEntryQuery().taskId(task.getId()).list();
        assertThat(taskLogEntries).size().isEqualTo(2);
        assertThat(taskLogEntries.get(1).getData()).
            contains("\"newAssigneeId\":null").
            contains("\"previousAssigneeId\":\"initialAssignee\"");
        assertThat(taskLogEntries.get(1)).extracting(HistoricTaskLogEntry::getTimeStamp).isNotNull();
        assertThat(taskLogEntries.get(1)).extracting(HistoricTaskLogEntry::getTaskId).isEqualTo(task.getId());
        assertThat(taskLogEntries.get(1)).extracting(HistoricTaskLogEntry::getUserId).isNull();
        assertThat(taskLogEntries.get(1)).extracting(HistoricTaskLogEntry::getType).isEqualTo("USER_TASK_ASSIGNEE_CHANGED");
    }

    @Test
    public void changePriority() {
        task = cmmnTaskService.createTaskBuilder().create();

        cmmnTaskService.setPriority(task.getId(), Integer.MAX_VALUE);
        List<HistoricTaskLogEntry> taskLogEntries = cmmnHistoryService.createHistoricTaskLogEntryQuery().taskId(task.getId()).list();

        assertThat(taskLogEntries).size().isEqualTo(2);
        assertThat(taskLogEntries.get(1).getData()).
            contains("\"newPriority\":2147483647").
            contains("\"previousPriority\":50");
        assertThat(taskLogEntries.get(1)).extracting(HistoricTaskLogEntry::getTimeStamp).isNotNull();
        assertThat(taskLogEntries.get(1)).extracting(HistoricTaskLogEntry::getTaskId).isEqualTo(task.getId());
        assertThat(taskLogEntries.get(1)).extracting(HistoricTaskLogEntry::getUserId).isNull();
        assertThat(taskLogEntries.get(1)).extracting(HistoricTaskLogEntry::getType).isEqualTo("USER_TASK_PRIORITY_CHANGED");
    }

    @Test
    public void changePriorityEventAsAuthenticatedUser() {
        assertThatAuthenticatedUserIsSet(
            taskId ->  cmmnTaskService.setPriority(taskId, Integer.MAX_VALUE)
        );
    }

    protected void assertThatAuthenticatedUserIsSet(Consumer<String> functionToAssert) {
        String previousUserId = Authentication.getAuthenticatedUserId();
        task = cmmnTaskService.createTaskBuilder().
            assignee("testAssignee").
            create();
        Authentication.setAuthenticatedUserId("testUser");
        try {
            functionToAssert.accept(task.getId());

            List<HistoricTaskLogEntry> taskLogsByTaskInstanceId = cmmnHistoryService.createHistoricTaskLogEntryQuery().taskId(task.getId()).list();
            assertThat(taskLogsByTaskInstanceId).size().isEqualTo(2);

            assertThat(taskLogsByTaskInstanceId.get(1)).
                extracting(HistoricTaskLogEntry::getUserId).isEqualTo("testUser");
        } finally {
            Authentication.setAuthenticatedUserId(previousUserId);
        }
    }

    @Test
    public void changeDueDate() {
        task = cmmnTaskService.createTaskBuilder().create();

        cmmnTaskService.setDueDate(task.getId(), new Date());
        List<HistoricTaskLogEntry> taskLogEntries = cmmnHistoryService.createHistoricTaskLogEntryQuery().taskId(task.getId()).list();

        assertThat(taskLogEntries).size().isEqualTo(2);
        assertThat(taskLogEntries.get(1).getData()).
            contains("\"newDueDate\":").
            contains("\"previousDueDate\":null");
        assertThat(taskLogEntries.get(1)).extracting(HistoricTaskLogEntry::getTimeStamp).isNotNull();
        assertThat(taskLogEntries.get(1)).extracting(HistoricTaskLogEntry::getTaskId).isEqualTo(task.getId());
        assertThat(taskLogEntries.get(1)).extracting(HistoricTaskLogEntry::getUserId).isNull();
        assertThat(taskLogEntries.get(1)).extracting(HistoricTaskLogEntry::getType).isEqualTo("USER_TASK_DUEDATE_CHANGED");
    }

    @Test
    public void saveTask() {
        task = cmmnTaskService.createTaskBuilder().create();

        task.setAssignee("newAssignee");
        task.setOwner("newOwner");
        task.setPriority(Integer.MAX_VALUE);
        task.setDueDate(new Date());
        cmmnTaskService.saveTask(task);

        List<HistoricTaskLogEntry> taskLogEntries = cmmnHistoryService.createHistoricTaskLogEntryQuery().taskId(task.getId()).list();
        assertThat(taskLogEntries).as("Only User task created log entry is expected").size().isEqualTo(1);
    }

    @Test
    public void changeDueDateEventAsAuthenticatedUser() {
        assertThatAuthenticatedUserIsSet(
            taskId ->  cmmnTaskService.setDueDate(taskId, new Date())
        );
    }

    @Test
    public void createCustomTaskEventLog() {
        task = cmmnTaskService.createTaskBuilder().create();
        HistoricTaskLogEntryBuilder historicTaskLogEntryBuilder = cmmnHistoryService.createHistoricTaskLogEntryBuilder(task);
        historicTaskLogEntryBuilder.timeStamp(getInsertDate());
        historicTaskLogEntryBuilder.userId("testUser");
        historicTaskLogEntryBuilder.type("customType");
        historicTaskLogEntryBuilder.data("testData");
        historicTaskLogEntryBuilder.create();

        List<HistoricTaskLogEntry> logEntries = cmmnHistoryService.createHistoricTaskLogEntryQuery().taskId(task.getId()).list();

        MatcherAssert.assertThat(logEntries.size(), is(2));
        HistoricTaskLogEntry historicTaskLogEntry = logEntries.get(1);
        assertThat(historicTaskLogEntry.getLogNumber()).isNotNull();
        assertThat(historicTaskLogEntry.getUserId()).isEqualTo("testUser");
        assertThat(historicTaskLogEntry.getTaskId()).isEqualTo(task.getId());
        assertThat(historicTaskLogEntry.getType()).isEqualTo("customType");
        assertThat(historicTaskLogEntry.getTimeStamp()).isEqualTo(getInsertDate());
        assertThat(historicTaskLogEntry.getData()).isEqualTo("testData");
        cmmnHistoryService.deleteHistoricTaskLogEntry(logEntries.get(0).getLogNumber());
    }

    @Test
    public void createCustomTaskEventLog_taskIdIsEnoughToCreateTaskLogEntry() {
        task = cmmnTaskService.createTaskBuilder().create();

        HistoricTaskLogEntryBuilder historicTaskLogEntryBuilder = cmmnHistoryService.createHistoricTaskLogEntryBuilder(task);
        historicTaskLogEntryBuilder.create();
        List<HistoricTaskLogEntry> logEntries = cmmnHistoryService.createHistoricTaskLogEntryQuery().taskId(task.getId()).list();

        MatcherAssert.assertThat(logEntries.size(), is(2));
        HistoricTaskLogEntry historicTaskLogEntry = logEntries.get(1);
        assertThat(historicTaskLogEntry.getLogNumber()).isNotNull();
        assertThat(historicTaskLogEntry.getUserId()).isNull();
        assertThat(historicTaskLogEntry.getTaskId()).isEqualTo(task.getId());
        assertThat(historicTaskLogEntry.getType()).isNull();
        assertThat(historicTaskLogEntry.getTimeStamp()).isNotNull();
        assertThat(historicTaskLogEntry.getData()).isNull();
    }

    @Test
    public void createCustomTaskEventLog_withoutTimeStamp_addsDefault() {
        task = cmmnTaskService.createTaskBuilder().create();

        HistoricTaskLogEntryBuilder historicTaskLogEntryBuilder = cmmnHistoryService.createHistoricTaskLogEntryBuilder(task);
        historicTaskLogEntryBuilder.userId("testUser");
        historicTaskLogEntryBuilder.type("customType");
        historicTaskLogEntryBuilder.data("testData");
        historicTaskLogEntryBuilder.create();

        List<HistoricTaskLogEntry> logEntries = cmmnHistoryService.createHistoricTaskLogEntryQuery().taskId(task.getId()).list();

        MatcherAssert.assertThat(logEntries.size(), is(2));
        HistoricTaskLogEntry historicTaskLogEntry = logEntries.get(1);
        assertThat(historicTaskLogEntry.getLogNumber()).isNotNull();
        assertThat(historicTaskLogEntry.getTimeStamp()).isNotNull();
    }

    @Test
    public void logCaseTaskEvents() {
        deployOneHumanTaskCaseModel();
        CaseInstance oneTaskCase = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("oneTaskCase").start();
        assertNotNull(oneTaskCase);

        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(oneTaskCase.getId()).singleResult();
        assertNotNull(task);
        try {
            cmmnTaskService.setAssignee(task.getId(), "newAssignee");
            cmmnTaskService.setOwner(task.getId(), "newOwner");
            cmmnTaskService.complete(task.getId());

            List<HistoricTaskLogEntry> logEntries = cmmnHistoryService.createHistoricTaskLogEntryQuery().taskId(task.getId()).list();
            assertThat(logEntries).size().isEqualTo(4);
            assertThat(logEntries.get(0)).
                extracting(taskLogEntry -> taskLogEntry.getType()).isEqualTo("USER_TASK_CREATED")
            ;
            assertThat(logEntries.get(0)).
                extracting(taskLogEntry -> taskLogEntry.getScopeType()).isEqualTo(ScopeTypes.CMMN)
            ;
            assertThat(logEntries.get(0)).
                extracting(taskLogEntry -> taskLogEntry.getScopeId()).isEqualTo(oneTaskCase.getId())
            ;
            assertThat(logEntries.get(0)).
                extracting(taskLogEntry -> taskLogEntry.getSubScopeId()).isEqualTo(task.getSubScopeId())
            ;
            assertThat(logEntries.get(0)).
                extracting(taskLogEntry -> taskLogEntry.getScopeDefinitionId()).isEqualTo(oneTaskCase.getCaseDefinitionId())
            ;
            assertThat(logEntries.get(1)).
                extracting(taskLogEntry -> taskLogEntry.getType()).isEqualTo("USER_TASK_ASSIGNEE_CHANGED")
            ;
            assertThat(logEntries.get(2)).
                extracting(taskLogEntry -> taskLogEntry.getType()).isEqualTo("USER_TASK_OWNER_CHANGED")
            ;
            assertThat(logEntries.get(3)).
                extracting(taskLogEntry -> taskLogEntry.getType()).isEqualTo("USER_TASK_COMPLETED")
            ;
        } finally {
            deleteTaskWithLogEntries(task.getId());
        }
    }

    @Test
    public void logAddCandidateUser() {
        deployOneHumanTaskCaseModel();
        CaseInstance oneTaskCase = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("oneTaskCase").start();
        assertNotNull(oneTaskCase);
        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(oneTaskCase.getId()).singleResult();

        try {
            assertNotNull(oneTaskCase);
            assertNotNull(task);

            cmmnTaskService.addUserIdentityLink(task.getId(), "newCandidateUser", IdentityLinkType.CANDIDATE);

            List<HistoricTaskLogEntry> logEntries = cmmnHistoryService.createHistoricTaskLogEntryQuery().taskId(task.getId()).list();
            assertThat(logEntries).size().isEqualTo(2);
            assertThat(logEntries.get(1)).
                extracting(HistoricTaskLogEntry::getType).isEqualTo("USER_TASK_IDENTITY_LINK_ADDED");
            assertThat(logEntries.get(1).getData()).contains(
                "\"type\":\"candidate\"",
                "\"userId\":\"newCandidateUser\""
            );
        } finally {
            cmmnTaskService.complete(task.getId());
            deleteTaskWithLogEntries(task.getId());
        }
    }

    @Test
    public void logAddCandidateGroup() {
        deployOneHumanTaskCaseModel();
        CaseInstance oneTaskCase = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("oneTaskCase").start();
        assertNotNull(oneTaskCase);
        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(oneTaskCase.getId()).singleResult();
        assertNotNull(task);
        try {

            cmmnTaskService.addGroupIdentityLink(task.getId(), "newCandidateGroup", IdentityLinkType.CANDIDATE);

            List<HistoricTaskLogEntry> logEntries = cmmnHistoryService.createHistoricTaskLogEntryQuery().taskId(task.getId()).list();
            assertThat(logEntries).size().isEqualTo(2);
            assertThat(logEntries.get(1)).
                extracting(HistoricTaskLogEntry::getType).isEqualTo("USER_TASK_IDENTITY_LINK_ADDED");
            assertThat(new String(logEntries.get(1).getData())).contains(
                "\"type\":\"candidate\"",
                "\"groupId\":\"newCandidateGroup\""
            );
        } finally {
            cmmnTaskService.complete(task.getId());
            deleteTaskWithLogEntries(task.getId());
        }
    }

    @Test
    public void logDeleteCandidateGroup() {
        deployOneHumanTaskCaseModel();
        CaseInstance oneTaskCase = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("oneTaskCase").start();
        assertNotNull(oneTaskCase);
        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(oneTaskCase.getId()).singleResult();
        assertNotNull(task);

        cmmnTaskService.addGroupIdentityLink(task.getId(), "newCandidateGroup", IdentityLinkType.CANDIDATE);
        try {

            cmmnTaskService.deleteGroupIdentityLink(task.getId(), "newCandidateGroup", IdentityLinkType.CANDIDATE);

            List<HistoricTaskLogEntry> logEntries = cmmnHistoryService.createHistoricTaskLogEntryQuery().taskId(task.getId()).list();
            assertThat(logEntries).size().isEqualTo(3);
            assertThat(logEntries.get(2)).
                extracting(HistoricTaskLogEntry::getType).isEqualTo("USER_TASK_IDENTITY_LINK_REMOVED");
            assertThat(new String(logEntries.get(2).getData())).contains(
                "\"type\":\"candidate\"",
                "\"groupId\":\"newCandidateGroup\""
            );
        } finally {
            cmmnTaskService.complete(task.getId());
            deleteTaskWithLogEntries(task.getId());
        }
    }

    @Test
    public void logDeleteCandidateUser() {
        deployOneHumanTaskCaseModel();
        CaseInstance oneTaskCase = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("oneTaskCase").start();
        assertNotNull(oneTaskCase);
        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(oneTaskCase.getId()).singleResult();
        assertNotNull(task);
        cmmnTaskService.addUserIdentityLink(task.getId(), "newCandidateUser", IdentityLinkType.CANDIDATE);

        try {
            cmmnTaskService.deleteUserIdentityLink(task.getId(), "newCandidateUser", IdentityLinkType.CANDIDATE);
            List<HistoricTaskLogEntry> logEntries = cmmnHistoryService.createHistoricTaskLogEntryQuery().taskId(task.getId()).list();
            assertThat(logEntries).size().isEqualTo(3);
            assertThat(logEntries.get(2)).
                extracting(HistoricTaskLogEntry::getType).isEqualTo("USER_TASK_IDENTITY_LINK_REMOVED");
            assertThat(new String(logEntries.get(2).getData())).contains(
                "\"type\":\"candidate\"",
                "\"userId\":\"newCandidateUser\""
            );
        } finally {
            cmmnTaskService.complete(task.getId());
            deleteTaskWithLogEntries(task.getId());
        }
    }

    @Test
    public void queryForTaskLogEntriesByTaskId() {
        task = cmmnTaskService.createTaskBuilder().
            assignee("testAssignee").
            create();
        Task anotherTask = cmmnTaskService.createTaskBuilder().create();

        try {
            List<HistoricTaskLogEntry> logEntries = cmmnHistoryService.createHistoricTaskLogEntryQuery().taskId(task.getId()).list();
            assertThat(logEntries.size()).isEqualTo(1);
            assertThat(logEntries.get(0)).extracting(HistoricTaskLogEntry::getTaskId).isEqualTo(task.getId());

            assertThat(
                cmmnHistoryService.createHistoricTaskLogEntryQuery().taskId(task.getId()).count()
            ).isEqualTo(1l);
        } finally {
            deleteTaskWithLogEntries(anotherTask.getId());
        }
    }

    @Test
    public void queryForTaskLogEntriesByUserId() {
        assertThatTaskLogIsFetched(
            cmmnHistoryService.createHistoricTaskLogEntryBuilder().userId("testUser"),
            cmmnHistoryService.createHistoricTaskLogEntryQuery().userId("testUser")
        );
    }

    protected void assertThatTaskLogIsFetched(HistoricTaskLogEntryBuilder historicTaskLogEntryBuilder, HistoricTaskLogEntryQuery historicTaskLogEntryQuery) {
        task = cmmnTaskService.createTaskBuilder().
            assignee("testAssignee").
            create();
        Task anotherTask = cmmnTaskService.createTaskBuilder().create();
        historicTaskLogEntryBuilder.taskId(task.getId()).create();
        historicTaskLogEntryBuilder.taskId(task.getId()).create();
        historicTaskLogEntryBuilder.taskId(task.getId()).create();

        try {
            List<HistoricTaskLogEntry> logEntries = historicTaskLogEntryQuery.list();
            assertThat(logEntries.size()).isEqualTo(3);
            assertThat(logEntries).extracting(HistoricTaskLogEntry::getTaskId).containsExactly(task.getId(), task.getId(), task.getId());

            assertThat(historicTaskLogEntryQuery.count()).isEqualTo(3);

            List<HistoricTaskLogEntry> pagedLogEntries = historicTaskLogEntryQuery.listPage(1, 1);
            assertThat(pagedLogEntries.size()).isEqualTo(1);
            assertThat(pagedLogEntries.get(0)).isEqualToComparingFieldByField(logEntries.get(1));
            
        } finally {
            deleteTaskWithLogEntries(anotherTask.getId());
            cmmnTaskService.deleteTask(anotherTask.getId());
        }
    }

    @Test
    public void queryForTaskLogEntriesByType() {
        assertThatTaskLogIsFetched(
            cmmnHistoryService.createHistoricTaskLogEntryBuilder().type("testType"),
            cmmnHistoryService.createHistoricTaskLogEntryQuery().type("testType")
        );
    }

    @Test
    public void queryForTaskLogEntriesByProcessInstanceId() {
        assertThatTaskLogIsFetched(
            cmmnHistoryService.createHistoricTaskLogEntryBuilder().processInstanceId("testProcess"),
            cmmnHistoryService.createHistoricTaskLogEntryQuery().processInstanceId("testProcess")
        );
    }

    @Test
    public void queryForTaskLogEntriesByScopeId() {
        assertThatTaskLogIsFetched(
            cmmnHistoryService.createHistoricTaskLogEntryBuilder().scopeId("testScopeId"),
            cmmnHistoryService.createHistoricTaskLogEntryQuery().scopeId("testScopeId")
        );
    }

    @Test
    public void queryForTaskLogEntriesBySubScopeId() {
        assertThatTaskLogIsFetched(
            cmmnHistoryService.createHistoricTaskLogEntryBuilder().subScopeId("testSubScopeId"),
            cmmnHistoryService.createHistoricTaskLogEntryQuery().subScopeId("testSubScopeId")
        );
    }

    @Test
    public void queryForTaskLogEntriesByScopeType() {
        assertThatTaskLogIsFetched(
            cmmnHistoryService.createHistoricTaskLogEntryBuilder().scopeType("testScopeType"),
            cmmnHistoryService.createHistoricTaskLogEntryQuery().scopeType("testScopeType")
        );
    }

    @Test
    public void queryForTaskLogEntriesByFromTimeStamp() {
        assertThatTaskLogIsFetched(
            cmmnHistoryService.createHistoricTaskLogEntryBuilder().timeStamp(getInsertDate()),
            cmmnHistoryService.createHistoricTaskLogEntryQuery().from(getCompareBeforeDate())
        );
    }

    @Test
    public void queryForTaskLogEntriesByFromIncludedTimeStamp() {
        assertThatTaskLogIsFetched(
            cmmnHistoryService.createHistoricTaskLogEntryBuilder().timeStamp(getInsertDate()),
            cmmnHistoryService.createHistoricTaskLogEntryQuery().from(getInsertDate())
        );
    }

    @Test
    public void queryForTaskLogEntriesByToIncludedTimeStamp() {
        HistoricTaskLogEntryBuilder historicTaskLogEntryBuilder = cmmnHistoryService.createHistoricTaskLogEntryBuilder().timeStamp(getInsertDate());
        HistoricTaskLogEntryQuery historicTaskLogEntryQuery = cmmnHistoryService.createHistoricTaskLogEntryQuery().to(getCompareAfterDate());
        
        task = cmmnTaskService.createTaskBuilder().
            assignee("testAssignee").
            create();
        Task anotherTask = cmmnTaskService.createTaskBuilder().create();
        historicTaskLogEntryBuilder.taskId(task.getId()).create();
        historicTaskLogEntryBuilder.taskId(task.getId()).create();
        historicTaskLogEntryBuilder.taskId(task.getId()).create();
    
        try {
            List<HistoricTaskLogEntry> logEntries = historicTaskLogEntryQuery.list();
            assertThat(logEntries.size()).isEqualTo(5);
            assertThat(logEntries).extracting(HistoricTaskLogEntry::getTaskId).containsExactly(task.getId(), anotherTask.getId(), 
                            task.getId(), task.getId(), task.getId());
    
            assertThat(historicTaskLogEntryQuery.count()).isEqualTo(5);
    
            List<HistoricTaskLogEntry> pagedLogEntries = historicTaskLogEntryQuery.listPage(1, 1);
            assertThat(pagedLogEntries.size()).isEqualTo(1);
            assertThat(pagedLogEntries.get(0)).isEqualToComparingFieldByField(logEntries.get(1));
            
        } finally {
            deleteTaskWithLogEntries(anotherTask.getId());
            cmmnTaskService.deleteTask(anotherTask.getId());
        }
    }

    @Test
    public void queryForTaskLogEntriesByFromToTimeStamp() {
        assertThatTaskLogIsFetched(
            cmmnHistoryService.createHistoricTaskLogEntryBuilder().timeStamp(getInsertDate()),
            cmmnHistoryService.createHistoricTaskLogEntryQuery().from(getCompareBeforeDate()).to(getCompareAfterDate())
        );
    }

    @Test
    public void queryForTaskLogEntriesByTenantId() {
        assertThatTaskLogIsFetched(
            cmmnHistoryService.createHistoricTaskLogEntryBuilder().timeStamp(getInsertDate()),
            cmmnHistoryService.createHistoricTaskLogEntryQuery().from(getCompareBeforeDate()).to(getCompareAfterDate())
        );
    }

    @Test
    public void queryForTaskLogEntriesByProcessDefinitionId() {
        assertThatTaskLogIsFetched(
            cmmnHistoryService.createHistoricTaskLogEntryBuilder().processDefinitionId("testProcessDefinitionId"),
            cmmnHistoryService.createHistoricTaskLogEntryQuery().processDefinitionId("testProcessDefinitionId")
        );
    }

    @Test
    public void queryForTaskLogEntriesByScopeDefinitionId() {
        assertThatTaskLogIsFetched(
            cmmnHistoryService.createHistoricTaskLogEntryBuilder().scopeDefinitionId("testScopeDefinitionId"),
            cmmnHistoryService.createHistoricTaskLogEntryQuery().scopeDefinitionId("testScopeDefinitionId")
        );
    }

    @Test
    public void queryForTaskLogEntriesByLogNumber() {
        task = cmmnTaskService.createTaskBuilder().
            assignee("testAssignee").
            create();
        Task anotherTask = cmmnTaskService.createTaskBuilder().create();
        HistoricTaskLogEntryBuilder historicTaskLogEntryBuilder = cmmnHistoryService.createHistoricTaskLogEntryBuilder();
        historicTaskLogEntryBuilder.taskId(task.getId()).create();
        historicTaskLogEntryBuilder.taskId(task.getId()).create();
        historicTaskLogEntryBuilder.taskId(task.getId()).create();

        List<HistoricTaskLogEntry> allLogEntries = cmmnHistoryService.createHistoricTaskLogEntryQuery().list();

        try {
            HistoricTaskLogEntryQuery historicTaskLogEntryQuery = cmmnHistoryService.createHistoricTaskLogEntryQuery().
                fromLogNumber(allLogEntries.get(1).getLogNumber()).
                toLogNumber(allLogEntries.get(allLogEntries.size() - 2).getLogNumber());
            
            List<HistoricTaskLogEntry> logEntries = historicTaskLogEntryQuery.list();
            assertThat(logEntries.size()).isEqualTo(3);
            assertThat(logEntries).extracting(HistoricTaskLogEntry::getTaskId).containsExactly(anotherTask.getId(), task.getId(), task.getId());

            assertThat(historicTaskLogEntryQuery.count()).isEqualTo(3l);

            List<HistoricTaskLogEntry> pagedLogEntries = historicTaskLogEntryQuery.listPage(1, 1);
            assertThat(pagedLogEntries.size()).isEqualTo(1);
            assertThat(pagedLogEntries.get(0)).isEqualToComparingFieldByField(logEntries.get(1));
            
        } finally {
            deleteTaskWithLogEntries(anotherTask.getId());
            cmmnTaskService.deleteTask(anotherTask.getId(), true);
        }
    }

    @Test
    public void queryForTaskLogEntriesByNativeQuery() {
        HistoricTaskLogEntryBuilder historicTaskLogEntryBuilder = cmmnHistoryService.createHistoricTaskLogEntryBuilder();
        historicTaskLogEntryBuilder.taskId("1").create();
        historicTaskLogEntryBuilder.taskId("2").create();
        historicTaskLogEntryBuilder.taskId("3").create();

        try {
            assertEquals(3,
                cmmnHistoryService.createNativeHistoricTaskLogEntryQuery().sql("SELECT * FROM ACT_HI_TSK_LOG").list().size());
            assertEquals(3,
                cmmnHistoryService.createNativeHistoricTaskLogEntryQuery().sql("SELECT count(*) FROM ACT_HI_TSK_LOG").count());

            assertEquals(1, cmmnHistoryService.createNativeHistoricTaskLogEntryQuery().parameter("taskId", "1").
                sql("SELECT * FROM ACT_HI_TSK_LOG WHERE TASK_ID_ = #{taskId}").list().size());
            assertEquals(1, cmmnHistoryService.createNativeHistoricTaskLogEntryQuery().parameter("taskId", "1").
                sql("SELECT count(*) FROM ACT_HI_TSK_LOG WHERE TASK_ID_ = #{taskId}").count());
        } finally {
            deleteTaskWithLogEntries("1");
            deleteTaskWithLogEntries("2");
            deleteTaskWithLogEntries("3");
        }
    }
    
    protected Date getInsertDate() {
        Calendar cal = new GregorianCalendar(2020, 3, 10);
        return cal.getTime();
    }
    
    protected Date getCompareBeforeDate() {
        Calendar cal = new GregorianCalendar(2020, 3, 9);
        return cal.getTime();
    }
    
    protected Date getCompareAfterDate() {
        Calendar cal = new GregorianCalendar(2020, 3, 11);
        return cal.getTime();
    }
}
