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
import static org.assertj.core.api.Assertions.tuple;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.function.Consumer;

import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.test.impl.CmmnHistoryTestHelper;
import org.flowable.cmmn.engine.test.impl.CmmnTestHelper;
import org.flowable.cmmn.test.impl.CustomCmmnConfigurationFlowableTestCase;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.common.engine.impl.identity.Authentication;
import org.flowable.identitylink.api.IdentityLinkType;
import org.flowable.task.api.Task;
import org.flowable.task.api.history.HistoricTaskLogEntry;
import org.flowable.task.api.history.HistoricTaskLogEntryBuilder;
import org.flowable.task.api.history.HistoricTaskLogEntryQuery;
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
        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            cmmnHistoryService.deleteHistoricTaskInstance(taskId);
        }
        cmmnTaskService.deleteTask(taskId, true);
    }

    @Test
    public void createTaskEvent() {
        task = cmmnTaskService.createTaskBuilder()
                .assignee("testAssignee")
                .create();

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            List<HistoricTaskLogEntry> taskLogsByTaskInstanceId = cmmnHistoryService.createHistoricTaskLogEntryQuery().taskId(task.getId()).list();
            assertThat(taskLogsByTaskInstanceId)
                .extracting(HistoricTaskLogEntry::getTaskId, HistoricTaskLogEntry::getType)
                .containsExactly(tuple(task.getId(), "USER_TASK_CREATED"));
            assertThat(taskLogsByTaskInstanceId)
                .extracting(HistoricTaskLogEntry::getTimeStamp)
                .isNotNull();
            assertThat(taskLogsByTaskInstanceId)
                .extracting(HistoricTaskLogEntry::getUserId)
                .containsOnlyNulls();
        }
    }

    @Test
    public void createTaskEventAsAuthenticatedUser() {
        String previousUserId = Authentication.getAuthenticatedUserId();
        Authentication.setAuthenticatedUserId("testUser");
        try {
            task = cmmnTaskService.createTaskBuilder()
                    .assignee("testAssignee")
                    .create();

            if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
                List<HistoricTaskLogEntry> taskLogsByTaskInstanceId = cmmnHistoryService.createHistoricTaskLogEntryQuery().taskId(task.getId()).list();
                assertThat(taskLogsByTaskInstanceId)
                    .extracting(HistoricTaskLogEntry::getUserId)
                    .containsExactly("testUser");
            }
        } finally {
            Authentication.setAuthenticatedUserId(previousUserId);
        }
    }

    @Test
    public void queryForNonExistingTaskLogEntries() {
        task = cmmnTaskService.createTaskBuilder().create();

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            List<HistoricTaskLogEntry> taskLogsByTaskInstanceId = cmmnHistoryService.createHistoricTaskLogEntryQuery().taskId("NON-EXISTING-TASK-ID").list();
            assertThat(taskLogsByTaskInstanceId).isEmpty();
        }
    }

    @Test
    public void queryForNullTaskLogEntries_returnsAll() {
        Task taskA = cmmnTaskService.createTaskBuilder().create();
        Task taskB = cmmnTaskService.createTaskBuilder().create();
        Task taskC = cmmnTaskService.createTaskBuilder().create();

        try {
            if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
                List<HistoricTaskLogEntry> taskLogsByTaskInstanceId = cmmnHistoryService.createHistoricTaskLogEntryQuery().taskId(null).list();
                assertThat(taskLogsByTaskInstanceId).hasSize(3);
            }

        } finally {
            deleteTaskWithLogEntries(taskC.getId());
            deleteTaskWithLogEntries(taskB.getId());
            deleteTaskWithLogEntries(taskA.getId());
        }
    }

    @Test
    public void deleteTaskEventLogEntry() {
        task = cmmnTaskService.createTaskBuilder()
                .assignee("testAssignee")
                .create();

        List<HistoricTaskLogEntry> taskLogsByTaskInstanceId = null;
        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            taskLogsByTaskInstanceId = cmmnHistoryService.createHistoricTaskLogEntryQuery().taskId(task.getId()).list();
            assertThat(taskLogsByTaskInstanceId).hasSize(1);
        }

        if (taskLogsByTaskInstanceId != null) {
            cmmnHistoryService.deleteHistoricTaskLogEntry(taskLogsByTaskInstanceId.get(0).getLogNumber());
            if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {

                taskLogsByTaskInstanceId = cmmnHistoryService.createHistoricTaskLogEntryQuery().taskId(task.getId()).list();
                assertThat(taskLogsByTaskInstanceId).isEmpty();
            }
        }
    }

    @Test
    public void deleteNonExistingTaskEventLogEntry() {
        task = cmmnTaskService.createTaskBuilder().create();

        // non existing log entry delete should be successful
        cmmnHistoryService.deleteHistoricTaskLogEntry(9999);

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            assertThat(cmmnHistoryService.createHistoricTaskLogEntryQuery().taskId(task.getId()).list()).hasSize(1);
        }
    }

    @Test
    public void taskAssigneeEvent() {
        task = cmmnTaskService.createTaskBuilder()
                .assignee("initialAssignee")
                .create();

        cmmnTaskService.setAssignee(task.getId(), "newAssignee");
        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            List<HistoricTaskLogEntry> taskLogEntries = cmmnHistoryService.createHistoricTaskLogEntryQuery().taskId(task.getId()).list();

            assertThat(taskLogEntries).hasSize(2);
            assertThat(taskLogEntries.get(1))
                .extracting(HistoricTaskLogEntry::getData)
                .isEqualToComparingOnlyGivenFields("{\"newAssigneeId\":\"newAssignee\",\"previousAssigneeId\":\"initialAssignee\"}");
            assertThat(taskLogEntries.get(1))
                .extracting(HistoricTaskLogEntry::getTimeStamp)
                .isNotNull();
            assertThat(taskLogEntries.get(1))
                .extracting(HistoricTaskLogEntry::getTaskId)
                .isEqualTo(task.getId());
            assertThat(taskLogEntries.get(1))
                .extracting(HistoricTaskLogEntry::getUserId)
                .isNull();
            assertThat(taskLogEntries.get(1))
                .extracting(HistoricTaskLogEntry::getType)
                .isEqualTo("USER_TASK_ASSIGNEE_CHANGED");
        }
    }

    @Test
    public void changeAssigneeTaskEventAsAuthenticatedUser() {
        assertThatAuthenticatedUserIsSet(
                taskId -> cmmnTaskService.setAssignee(taskId, "newAssignee")
        );
    }

    @Test
    public void taskOwnerEvent() {
        task = cmmnTaskService.createTaskBuilder()
                .assignee("initialAssignee")
                .create();

        cmmnTaskService.setOwner(task.getId(), "newOwner");

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            List<HistoricTaskLogEntry> taskLogEntries = cmmnHistoryService.createHistoricTaskLogEntryQuery().taskId(task.getId()).list();

            assertThat(taskLogEntries).hasSize(2);
            assertThat(taskLogEntries.get(1))
                .extracting(HistoricTaskLogEntry::getData)
                .isEqualToComparingOnlyGivenFields("{\"previousOwnerId\":null\", \"newOwnerId\":\"newOwner\"}");
            assertThat(taskLogEntries.get(1))
                .extracting(HistoricTaskLogEntry::getTimeStamp)
                .isNotNull();
            assertThat(taskLogEntries.get(1))
                .extracting(HistoricTaskLogEntry::getTaskId)
                .isEqualTo(task.getId());
            assertThat(taskLogEntries.get(1))
                .extracting(HistoricTaskLogEntry::getUserId)
                .isNull();
            assertThat(taskLogEntries.get(1))
                .extracting(HistoricTaskLogEntry::getType)
                .isEqualTo("USER_TASK_OWNER_CHANGED");
        }
    }

    @Test
    public void changeOwnerTaskEventAsAuthenticatedUser() {
        assertThatAuthenticatedUserIsSet(
                taskId -> cmmnTaskService.setOwner(taskId, "newOwner")
        );
    }

    @Test
    public void claimTaskEvent() {
        task = cmmnTaskService.createTaskBuilder().create();

        cmmnTaskService.claim(task.getId(), "testUser");

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            List<HistoricTaskLogEntry> taskLogEntries = cmmnHistoryService.createHistoricTaskLogEntryQuery().taskId(task.getId()).list();
            assertThat(taskLogEntries).hasSize(2);
            assertThat(taskLogEntries.get(1))
                .extracting(HistoricTaskLogEntry::getData)
                .isEqualToComparingOnlyGivenFields("{\"newAssigneeId\":\"testUser\", \"previousAssigneeId\":null\"}");
            assertThat(taskLogEntries.get(1))
                .extracting(HistoricTaskLogEntry::getTimeStamp)
                .isNotNull();
            assertThat(taskLogEntries.get(1))
                .extracting(HistoricTaskLogEntry::getTaskId)
                .isEqualTo(task.getId());
            assertThat(taskLogEntries.get(1))
                .extracting(HistoricTaskLogEntry::getUserId)
                .isNull();
            assertThat(taskLogEntries.get(1))
                .extracting(HistoricTaskLogEntry::getType)
                .isEqualTo("USER_TASK_ASSIGNEE_CHANGED");
        }
    }

    @Test
    public void unClaimTaskEvent() {
        task = cmmnTaskService.createTaskBuilder()
                .assignee("initialAssignee")
                .create();

        cmmnTaskService.unclaim(task.getId());

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            List<HistoricTaskLogEntry> taskLogEntries = cmmnHistoryService.createHistoricTaskLogEntryQuery().taskId(task.getId()).list();
            assertThat(taskLogEntries).hasSize(2);
            assertThat(taskLogEntries.get(1))
                .extracting(HistoricTaskLogEntry::getData)
                .isEqualToComparingOnlyGivenFields("{\"newAssigneeId\":null\", \"previousAssigneeId\":\"initialAssignee\"}");
            assertThat(taskLogEntries.get(1))
                .extracting(HistoricTaskLogEntry::getTimeStamp)
                .isNotNull();
            assertThat(taskLogEntries.get(1))
                .extracting(HistoricTaskLogEntry::getTaskId)
                .isEqualTo(task.getId());
            assertThat(taskLogEntries.get(1))
                .extracting(HistoricTaskLogEntry::getUserId)
                .isNull();
            assertThat(taskLogEntries.get(1))
                .extracting(HistoricTaskLogEntry::getType)
                .isEqualTo("USER_TASK_ASSIGNEE_CHANGED");
        }
    }

    @Test
    public void changePriority() {
        task = cmmnTaskService.createTaskBuilder().create();

        cmmnTaskService.setPriority(task.getId(), Integer.MAX_VALUE);

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            List<HistoricTaskLogEntry> taskLogEntries = cmmnHistoryService.createHistoricTaskLogEntryQuery().taskId(task.getId()).list();

            assertThat(taskLogEntries).hasSize(2);
            assertThat(taskLogEntries.get(1))
                .extracting(HistoricTaskLogEntry::getData)
                .isEqualToComparingOnlyGivenFields("{\"newPriority\":2147483647, \"previousPriority\":50}");
            assertThat(taskLogEntries.get(1))
                .extracting(HistoricTaskLogEntry::getTimeStamp)
                .isNotNull();
            assertThat(taskLogEntries.get(1))
                .extracting(HistoricTaskLogEntry::getTaskId)
                .isEqualTo(task.getId());
            assertThat(taskLogEntries.get(1))
                .extracting(HistoricTaskLogEntry::getUserId)
                .isNull();
            assertThat(taskLogEntries.get(1))
                .extracting(HistoricTaskLogEntry::getType)
                .isEqualTo("USER_TASK_PRIORITY_CHANGED");
        }
    }

    @Test
    public void changePriorityEventAsAuthenticatedUser() {
        assertThatAuthenticatedUserIsSet(
                taskId -> cmmnTaskService.setPriority(taskId, Integer.MAX_VALUE)
        );
    }

    protected void assertThatAuthenticatedUserIsSet(Consumer<String> functionToAssert) {
        String previousUserId = Authentication.getAuthenticatedUserId();
        task = cmmnTaskService.createTaskBuilder()
                .assignee("testAssignee")
                .create();
        Authentication.setAuthenticatedUserId("testUser");
        try {
            functionToAssert.accept(task.getId());

            if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
                List<HistoricTaskLogEntry> taskLogsByTaskInstanceId = cmmnHistoryService.createHistoricTaskLogEntryQuery().taskId(task.getId()).list();
                assertThat(taskLogsByTaskInstanceId).hasSize(2);
                assertThat(taskLogsByTaskInstanceId.get(1))
                    .extracting(HistoricTaskLogEntry::getUserId)
                    .isEqualTo("testUser");
            }
        } finally {
            Authentication.setAuthenticatedUserId(previousUserId);
        }
    }

    @Test
    public void changeDueDate() {
        task = cmmnTaskService.createTaskBuilder().create();

        cmmnTaskService.setDueDate(task.getId(), new Date());

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            List<HistoricTaskLogEntry> taskLogEntries = cmmnHistoryService.createHistoricTaskLogEntryQuery().taskId(task.getId()).list();

            assertThat(taskLogEntries).hasSize(2);
            assertThat(taskLogEntries.get(1))
                .extracting(HistoricTaskLogEntry::getData)
                .isEqualToComparingOnlyGivenFields("{\"newDueDate\":null, \"previousDueDate\":null}");
            assertThat(taskLogEntries.get(1))
                .extracting(HistoricTaskLogEntry::getTimeStamp)
                .isNotNull();
            assertThat(taskLogEntries.get(1))
                .extracting(HistoricTaskLogEntry::getTaskId)
                .isEqualTo(task.getId());
            assertThat(taskLogEntries.get(1))
                .extracting(HistoricTaskLogEntry::getUserId)
                .isNull();
            assertThat(taskLogEntries.get(1))
                .extracting(HistoricTaskLogEntry::getType)
                .isEqualTo("USER_TASK_DUEDATE_CHANGED");
        }
    }

    @Test
    public void saveTask() {
        task = cmmnTaskService.createTaskBuilder().create();

        task.setAssignee("newAssignee");
        task.setOwner("newOwner");
        task.setPriority(Integer.MAX_VALUE);
        task.setDueDate(new Date());
        cmmnTaskService.saveTask(task);

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            List<HistoricTaskLogEntry> taskLogEntries = cmmnHistoryService.createHistoricTaskLogEntryQuery().taskId(task.getId()).list();
            assertThat(taskLogEntries).as("Only User task created log entry is expected").hasSize(1);
        }
    }

    @Test
    public void changeDueDateEventAsAuthenticatedUser() {
        assertThatAuthenticatedUserIsSet(
                taskId -> cmmnTaskService.setDueDate(taskId, new Date())
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

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {

            List<HistoricTaskLogEntry> logEntries = cmmnHistoryService.createHistoricTaskLogEntryQuery().taskId(task.getId()).list();

            assertThat(logEntries).hasSize(2);
            HistoricTaskLogEntry historicTaskLogEntry = logEntries.get(1);
            assertThat(historicTaskLogEntry.getLogNumber()).isNotNull();
            assertThat(historicTaskLogEntry)
                .extracting(
                    HistoricTaskLogEntry::getUserId,
                    HistoricTaskLogEntry::getTaskId,
                    HistoricTaskLogEntry::getType,
                    HistoricTaskLogEntry::getTimeStamp,
                    HistoricTaskLogEntry::getData)
                .containsExactly(
                    "testUser",
                    task.getId(),
                    "customType",
                    getInsertDate(),
                    "testData"
                );
            cmmnHistoryService.deleteHistoricTaskLogEntry(logEntries.get(0).getLogNumber());
        }
    }

    @Test
    public void createCustomTaskEventLog_taskIdIsEnoughToCreateTaskLogEntry() {
        task = cmmnTaskService.createTaskBuilder().create();

        HistoricTaskLogEntryBuilder historicTaskLogEntryBuilder = cmmnHistoryService.createHistoricTaskLogEntryBuilder(task);
        historicTaskLogEntryBuilder.create();

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            List<HistoricTaskLogEntry> logEntries = cmmnHistoryService.createHistoricTaskLogEntryQuery().taskId(task.getId()).list();

            assertThat(logEntries).hasSize(2);
            HistoricTaskLogEntry historicTaskLogEntry = logEntries.get(1);
            assertThat(historicTaskLogEntry)
                .extracting(HistoricTaskLogEntry::getLogNumber, HistoricTaskLogEntry::getTimeStamp)
                .doesNotContainNull();
            assertThat(historicTaskLogEntry)
                .extracting(HistoricTaskLogEntry::getUserId, HistoricTaskLogEntry::getType, HistoricTaskLogEntry::getData)
                .containsNull();
            assertThat(historicTaskLogEntry.getTaskId()).isEqualTo(task.getId());
        }
    }

    @Test
    public void createCustomTaskEventLog_withoutTimeStamp_addsDefault() {
        task = cmmnTaskService.createTaskBuilder().create();

        HistoricTaskLogEntryBuilder historicTaskLogEntryBuilder = cmmnHistoryService.createHistoricTaskLogEntryBuilder(task);
        historicTaskLogEntryBuilder.userId("testUser");
        historicTaskLogEntryBuilder.type("customType");
        historicTaskLogEntryBuilder.data("testData");
        historicTaskLogEntryBuilder.create();

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            List<HistoricTaskLogEntry> logEntries = cmmnHistoryService.createHistoricTaskLogEntryQuery().taskId(task.getId()).list();

            assertThat(logEntries).hasSize(2);
            HistoricTaskLogEntry historicTaskLogEntry = logEntries.get(1);
            assertThat(historicTaskLogEntry)
                .extracting(HistoricTaskLogEntry::getLogNumber, HistoricTaskLogEntry::getTimeStamp)
                .doesNotContainNull();
        }
    }

    @Test
    public void logCaseTaskEvents() {
        deployOneHumanTaskCaseModel();
        CaseInstance oneTaskCase = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("oneTaskCase").start();
        assertThat(oneTaskCase).isNotNull();

        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(oneTaskCase.getId()).singleResult();
        assertThat(task).isNotNull();
        try {
            cmmnTaskService.setAssignee(task.getId(), "newAssignee");
            cmmnTaskService.setOwner(task.getId(), "newOwner");
            cmmnTaskService.complete(task.getId());

            if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
                List<HistoricTaskLogEntry> logEntries = cmmnHistoryService.createHistoricTaskLogEntryQuery().taskId(task.getId()).list();

                assertThat(logEntries.get(0))
                    .extracting(
                        HistoricTaskLogEntry::getType,
                        HistoricTaskLogEntry::getScopeType,
                        HistoricTaskLogEntry::getScopeId,
                        HistoricTaskLogEntry::getSubScopeId,
                        HistoricTaskLogEntry::getScopeDefinitionId
                    )
                    .containsExactly(
                        "USER_TASK_CREATED",
                        ScopeTypes.CMMN,
                        oneTaskCase.getId(),
                        task.getSubScopeId(),
                        oneTaskCase.getCaseDefinitionId()
                    );

                assertThat(logEntries)
                    .extracting(HistoricTaskLogEntry::getType)
                    .containsExactly("USER_TASK_CREATED", "USER_TASK_ASSIGNEE_CHANGED", "USER_TASK_OWNER_CHANGED", "USER_TASK_COMPLETED");
            }
        } finally {
            deleteTaskWithLogEntries(task.getId());
        }
    }

    @Test
    public void logCaseTaskEventsInSameTransaction() {
        deployOneHumanTaskCaseModel();
        CaseInstance oneTaskCase = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("oneTaskCase").start();
        assertThat(oneTaskCase).isNotNull();

        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(oneTaskCase.getId()).singleResult();
        assertThat(task).isNotNull();
        try {
            cmmnEngineConfiguration.getCommandExecutor().execute(commandContext -> {
                cmmnTaskService.setAssignee(task.getId(), "newAssignee");
                cmmnTaskService.setOwner(task.getId(), "newOwner");
                cmmnTaskService.complete(task.getId());

                return null;
            });

            if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
                List<HistoricTaskLogEntry> logEntries = cmmnHistoryService.createHistoricTaskLogEntryQuery().taskId(task.getId()).list();

                assertThat(logEntries.get(0))
                    .extracting(
                        HistoricTaskLogEntry::getType,
                        HistoricTaskLogEntry::getScopeType,
                        HistoricTaskLogEntry::getScopeId,
                        HistoricTaskLogEntry::getSubScopeId,
                        HistoricTaskLogEntry::getScopeDefinitionId
                    )
                    .containsExactly(
                        "USER_TASK_CREATED",
                        ScopeTypes.CMMN,
                        oneTaskCase.getId(),
                        task.getSubScopeId(),
                        oneTaskCase.getCaseDefinitionId()
                    );

                assertThat(logEntries)
                    .extracting(HistoricTaskLogEntry::getType)
                    .containsExactly(
                            "USER_TASK_CREATED",
                            "USER_TASK_ASSIGNEE_CHANGED",
                            "USER_TASK_ASSIGNEE_CHANGED",
                            "USER_TASK_OWNER_CHANGED",
                            "USER_TASK_COMPLETED"
                    );
            }
        } finally {
            deleteTaskWithLogEntries(task.getId());
        }
    }

    @Test
    public void logAddCandidateUser() {
        deployOneHumanTaskCaseModel();
        CaseInstance oneTaskCase = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("oneTaskCase").start();
        assertThat(oneTaskCase).isNotNull();
        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(oneTaskCase.getId()).singleResult();

        try {
            assertThat(oneTaskCase).isNotNull();
            assertThat(task).isNotNull();

            cmmnTaskService.addUserIdentityLink(task.getId(), "newCandidateUser", IdentityLinkType.CANDIDATE);

            if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
                List<HistoricTaskLogEntry> logEntries = cmmnHistoryService.createHistoricTaskLogEntryQuery().taskId(task.getId()).list();
                assertThat(logEntries).hasSize(2);
                assertThat(logEntries.get(1))
                    .extracting(HistoricTaskLogEntry::getType)
                    .isEqualTo("USER_TASK_IDENTITY_LINK_ADDED");
                assertThat(logEntries.get(1))
                    .extracting(HistoricTaskLogEntry::getData)
                    .isEqualToComparingOnlyGivenFields("{\"type\":\"candidate\", \"userId\":\"newCandidateUser\"}");
            }
        } finally {
            cmmnTaskService.complete(task.getId());
            deleteTaskWithLogEntries(task.getId());
        }
    }

    @Test
    public void logAddCandidateGroup() {
        deployOneHumanTaskCaseModel();
        CaseInstance oneTaskCase = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("oneTaskCase").start();
        assertThat(oneTaskCase).isNotNull();
        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(oneTaskCase.getId()).singleResult();
        assertThat(task).isNotNull();
        try {
            cmmnTaskService.addGroupIdentityLink(task.getId(), "newCandidateGroup", IdentityLinkType.CANDIDATE);

            if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
                List<HistoricTaskLogEntry> logEntries = cmmnHistoryService.createHistoricTaskLogEntryQuery().taskId(task.getId()).list();
                assertThat(logEntries).hasSize(2);
                assertThat(logEntries.get(1))
                    .extracting(HistoricTaskLogEntry::getType)
                    .isEqualTo("USER_TASK_IDENTITY_LINK_ADDED");
                assertThat(logEntries.get(1))
                    .extracting(HistoricTaskLogEntry::getData)
                    .isEqualToComparingOnlyGivenFields("{\"type\":\"candidate\", \"userId\":\"newCandidateGroup\"}");
            }
        } finally {
            cmmnTaskService.complete(task.getId());
            deleteTaskWithLogEntries(task.getId());
        }
    }

    @Test
    public void logDeleteCandidateGroup() {
        deployOneHumanTaskCaseModel();
        CaseInstance oneTaskCase = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("oneTaskCase").start();
        assertThat(oneTaskCase).isNotNull();
        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(oneTaskCase.getId()).singleResult();
        assertThat(task).isNotNull();

        cmmnTaskService.addGroupIdentityLink(task.getId(), "newCandidateGroup", IdentityLinkType.CANDIDATE);
        try {
            cmmnTaskService.deleteGroupIdentityLink(task.getId(), "newCandidateGroup", IdentityLinkType.CANDIDATE);

            if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
                List<HistoricTaskLogEntry> logEntries = cmmnHistoryService.createHistoricTaskLogEntryQuery().taskId(task.getId()).list();
                assertThat(logEntries).hasSize(3);
                assertThat(logEntries.get(2))
                    .extracting(HistoricTaskLogEntry::getType)
                    .isEqualTo("USER_TASK_IDENTITY_LINK_REMOVED");
                assertThat(logEntries.get(2))
                    .extracting(HistoricTaskLogEntry::getData)
                    .isEqualToComparingOnlyGivenFields("{\"type\":\"candidate\", \"userId\":\"newCandidateGroup\"}");
            }
        } finally {
            cmmnTaskService.complete(task.getId());
            deleteTaskWithLogEntries(task.getId());
        }
    }

    @Test
    public void logDeleteCandidateUser() {
        deployOneHumanTaskCaseModel();
        CaseInstance oneTaskCase = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("oneTaskCase").start();
        assertThat(oneTaskCase).isNotNull();
        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(oneTaskCase.getId()).singleResult();
        assertThat(task).isNotNull();
        cmmnTaskService.addUserIdentityLink(task.getId(), "newCandidateUser", IdentityLinkType.CANDIDATE);

        try {
            cmmnTaskService.deleteUserIdentityLink(task.getId(), "newCandidateUser", IdentityLinkType.CANDIDATE);

            if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
                List<HistoricTaskLogEntry> logEntries = cmmnHistoryService.createHistoricTaskLogEntryQuery().taskId(task.getId()).list();
                assertThat(logEntries).hasSize(3);
                assertThat(logEntries.get(2))
                    .extracting(HistoricTaskLogEntry::getType)
                    .isEqualTo("USER_TASK_IDENTITY_LINK_REMOVED");
                assertThat(logEntries.get(2))
                    .extracting(HistoricTaskLogEntry::getData)
                    .isEqualToComparingOnlyGivenFields("{\"type\":\"candidate\", \"userId\":\"newCandidateUser\"}");
            }
        } finally {
            cmmnTaskService.complete(task.getId());
            deleteTaskWithLogEntries(task.getId());
        }
    }

    @Test
    public void queryForTaskLogEntriesByTaskId() {
        task = cmmnTaskService.createTaskBuilder()
                .assignee("testAssignee")
                .create();
        Task anotherTask = cmmnTaskService.createTaskBuilder().create();

        try {
            if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
                List<HistoricTaskLogEntry> logEntries = cmmnHistoryService.createHistoricTaskLogEntryQuery().taskId(task.getId()).list();
                assertThat(logEntries)
                    .extracting(HistoricTaskLogEntry::getTaskId)
                    .containsExactly(task.getId());
                assertThat(cmmnHistoryService.createHistoricTaskLogEntryQuery().taskId(task.getId()).count()).isEqualTo(1);
            }
        } finally {
            deleteTaskWithLogEntries(anotherTask.getId());
        }
    }

    @Test
    public void queryForTaskLogEntriesByUserId() {
        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            assertThatTaskLogIsFetched(
                cmmnHistoryService.createHistoricTaskLogEntryBuilder().userId("testUser"),
                cmmnHistoryService.createHistoricTaskLogEntryQuery().userId("testUser")
            );
        }
    }

    protected void assertThatTaskLogIsFetched(HistoricTaskLogEntryBuilder historicTaskLogEntryBuilder, HistoricTaskLogEntryQuery historicTaskLogEntryQuery) {
        task = cmmnTaskService.createTaskBuilder()
                .assignee("testAssignee")
                .create();
        Task anotherTask = cmmnTaskService.createTaskBuilder().create();
        historicTaskLogEntryBuilder.taskId(task.getId()).create();
        historicTaskLogEntryBuilder.taskId(task.getId()).create();
        historicTaskLogEntryBuilder.taskId(task.getId()).create();

        try {
            List<HistoricTaskLogEntry> logEntries = historicTaskLogEntryQuery.list();
            assertThat(logEntries).hasSize(3);
            assertThat(logEntries).extracting(HistoricTaskLogEntry::getTaskId).containsExactly(task.getId(), task.getId(), task.getId());

            assertThat(historicTaskLogEntryQuery.count()).isEqualTo(3);

            List<HistoricTaskLogEntry> pagedLogEntries = historicTaskLogEntryQuery.listPage(1, 1);
            assertThat(pagedLogEntries).hasSize(1);
            assertThat(pagedLogEntries.get(0)).isEqualToComparingFieldByField(logEntries.get(1));

        } finally {
            deleteTaskWithLogEntries(anotherTask.getId());
            cmmnTaskService.deleteTask(anotherTask.getId());
        }
    }
    
    protected void assertThatAllTaskLogIsFetched(HistoricTaskLogEntryBuilder historicTaskLogEntryBuilder, HistoricTaskLogEntryQuery historicTaskLogEntryQuery) {
        task = cmmnTaskService.createTaskBuilder()
                .assignee("testAssignee")
                .create();
        Task anotherTask = cmmnTaskService.createTaskBuilder().create();
        historicTaskLogEntryBuilder.taskId(task.getId()).create();
        historicTaskLogEntryBuilder.taskId(task.getId()).create();
        historicTaskLogEntryBuilder.taskId(task.getId()).create();

        try {
            List<HistoricTaskLogEntry> logEntries = historicTaskLogEntryQuery.list();
            assertThat(logEntries).hasSize(5);
            assertThat(logEntries).extracting(HistoricTaskLogEntry::getTaskId).containsExactly(task.getId(), anotherTask.getId(), task.getId(), task.getId(), task.getId());

            assertThat(historicTaskLogEntryQuery.count()).isEqualTo(5);

            List<HistoricTaskLogEntry> pagedLogEntries = historicTaskLogEntryQuery.listPage(1, 1);
            assertThat(pagedLogEntries).hasSize(1);
            assertThat(pagedLogEntries.get(0)).isEqualToComparingFieldByField(logEntries.get(1));

        } finally {
            deleteTaskWithLogEntries(anotherTask.getId());
            cmmnTaskService.deleteTask(anotherTask.getId());
        }
    }

    @Test
    public void queryForTaskLogEntriesByType() {
        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            assertThatTaskLogIsFetched(
                cmmnHistoryService.createHistoricTaskLogEntryBuilder().type("testType"),
                cmmnHistoryService.createHistoricTaskLogEntryQuery().type("testType")
            );
        }
    }

    @Test
    public void queryForTaskLogEntriesByProcessInstanceId() {
        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            assertThatTaskLogIsFetched(
                cmmnHistoryService.createHistoricTaskLogEntryBuilder().processInstanceId("testProcess"),
                cmmnHistoryService.createHistoricTaskLogEntryQuery().processInstanceId("testProcess")
            );
        }
    }

    @Test
    public void queryForTaskLogEntriesByScopeId() {
        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            assertThatTaskLogIsFetched(
                cmmnHistoryService.createHistoricTaskLogEntryBuilder().scopeId("testScopeId"),
                cmmnHistoryService.createHistoricTaskLogEntryQuery().scopeId("testScopeId")
            );
        }
    }

    @Test
    public void queryForTaskLogEntriesBySubScopeId() {
        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            assertThatTaskLogIsFetched(
                cmmnHistoryService.createHistoricTaskLogEntryBuilder().subScopeId("testSubScopeId"),
                cmmnHistoryService.createHistoricTaskLogEntryQuery().subScopeId("testSubScopeId")
            );
        }
    }

    @Test
    public void queryForTaskLogEntriesByScopeType() {
        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            assertThatTaskLogIsFetched(
                cmmnHistoryService.createHistoricTaskLogEntryBuilder().scopeType("testScopeType"),
                cmmnHistoryService.createHistoricTaskLogEntryQuery().scopeType("testScopeType")
            );
        }
    }

    @Test
    public void queryForTaskLogEntriesByFromTimeStamp() {
        assertThatAllTaskLogIsFetched(
            cmmnHistoryService.createHistoricTaskLogEntryBuilder().timeStamp(getInsertDate()),
            cmmnHistoryService.createHistoricTaskLogEntryQuery().from(getCompareBeforeDate())
        );
    }

    @Test
    public void queryForTaskLogEntriesByFromIncludedTimeStamp() {
        assertThatAllTaskLogIsFetched(
            cmmnHistoryService.createHistoricTaskLogEntryBuilder().timeStamp(getInsertDate()),
            cmmnHistoryService.createHistoricTaskLogEntryQuery().from(getInsertDate())
        );
    }

    @Test
    public void queryForTaskLogEntriesByToIncludedTimeStamp() {
        HistoricTaskLogEntryBuilder historicTaskLogEntryBuilder = cmmnHistoryService.createHistoricTaskLogEntryBuilder().timeStamp(getInsertDate());
        HistoricTaskLogEntryQuery historicTaskLogEntryQuery = cmmnHistoryService.createHistoricTaskLogEntryQuery().to(getCompareAfterDate());

        task = cmmnTaskService.createTaskBuilder()
                .assignee("testAssignee")
                .create();
        Task anotherTask = cmmnTaskService.createTaskBuilder().create();
        historicTaskLogEntryBuilder.taskId(task.getId()).create();
        historicTaskLogEntryBuilder.taskId(task.getId()).create();
        historicTaskLogEntryBuilder.taskId(task.getId()).create();

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            try {
                List<HistoricTaskLogEntry> logEntries = historicTaskLogEntryQuery.list();
                assertThat(logEntries)
                    .extracting(HistoricTaskLogEntry::getTaskId)
                    .containsExactly(
                        task.getId(),
                        task.getId(),
                        task.getId()
                    );

                assertThat(historicTaskLogEntryQuery.count()).isEqualTo(3);

                List<HistoricTaskLogEntry> pagedLogEntries = historicTaskLogEntryQuery.listPage(1, 1);
                assertThat(pagedLogEntries).hasSize(1);
                assertThat(pagedLogEntries.get(0)).isEqualToComparingFieldByField(logEntries.get(1));

            } finally {
                deleteTaskWithLogEntries(anotherTask.getId());
            }
        }

        cmmnTaskService.deleteTask(anotherTask.getId());
    }

    @Test
    public void queryForTaskLogEntriesByFromToTimeStamp() {
        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            assertThatTaskLogIsFetched(
                cmmnHistoryService.createHistoricTaskLogEntryBuilder().timeStamp(getInsertDate()),
                cmmnHistoryService.createHistoricTaskLogEntryQuery().from(getCompareBeforeDate()).to(getCompareAfterDate())
            );
        }
    }

    @Test
    public void queryForTaskLogEntriesByTenantId() {
        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            assertThatTaskLogIsFetched(
                cmmnHistoryService.createHistoricTaskLogEntryBuilder().timeStamp(getInsertDate()),
                cmmnHistoryService.createHistoricTaskLogEntryQuery().from(getCompareBeforeDate()).to(getCompareAfterDate())
            );
        }
    }

    @Test
    public void queryForTaskLogEntriesByProcessDefinitionId() {
        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            assertThatTaskLogIsFetched(
                cmmnHistoryService.createHistoricTaskLogEntryBuilder().processDefinitionId("testProcessDefinitionId"),
                cmmnHistoryService.createHistoricTaskLogEntryQuery().processDefinitionId("testProcessDefinitionId")
            );
        }
    }

    @Test
    public void queryForTaskLogEntriesByScopeDefinitionId() {
        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            assertThatTaskLogIsFetched(
                cmmnHistoryService.createHistoricTaskLogEntryBuilder().scopeDefinitionId("testScopeDefinitionId"),
                cmmnHistoryService.createHistoricTaskLogEntryQuery().scopeDefinitionId("testScopeDefinitionId")
            );
        }
    }

    @Test
    public void queryForTaskLogEntriesByLogNumber() {
        task = cmmnTaskService.createTaskBuilder()
            .assignee("testAssignee")
            .create();
        Task anotherTask = cmmnTaskService.createTaskBuilder().create();
        HistoricTaskLogEntryBuilder historicTaskLogEntryBuilder = cmmnHistoryService.createHistoricTaskLogEntryBuilder();
        historicTaskLogEntryBuilder.taskId(task.getId()).create();
        historicTaskLogEntryBuilder.taskId(task.getId()).create();
        historicTaskLogEntryBuilder.taskId(task.getId()).create();

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {

            List<HistoricTaskLogEntry> allLogEntries = cmmnHistoryService.createHistoricTaskLogEntryQuery().list();

            try {
                HistoricTaskLogEntryQuery historicTaskLogEntryQuery = cmmnHistoryService.createHistoricTaskLogEntryQuery()
                    .fromLogNumber(allLogEntries.get(1).getLogNumber())
                    .toLogNumber(allLogEntries.get(allLogEntries.size() - 2).getLogNumber());

                List<HistoricTaskLogEntry> logEntries = historicTaskLogEntryQuery.list();
                assertThat(logEntries)
                    .extracting(HistoricTaskLogEntry::getTaskId)
                    .containsExactly(anotherTask.getId(), task.getId(), task.getId());

                assertThat(historicTaskLogEntryQuery.count()).isEqualTo(3);

                List<HistoricTaskLogEntry> pagedLogEntries = historicTaskLogEntryQuery.listPage(1, 1);
                assertThat(pagedLogEntries).hasSize(1);
                assertThat(pagedLogEntries.get(0)).isEqualToComparingFieldByField(logEntries.get(1));

            } finally {
                deleteTaskWithLogEntries(anotherTask.getId());
            }
        }

        cmmnTaskService.deleteTask(anotherTask.getId(), true);
    }

    @Test
    public void queryForTaskLogEntriesByNativeQuery() {

        HistoricTaskLogEntryBuilder historicTaskLogEntryBuilder = cmmnHistoryService.createHistoricTaskLogEntryBuilder();
        historicTaskLogEntryBuilder.taskId("1").create();
        historicTaskLogEntryBuilder.taskId("2").create();
        historicTaskLogEntryBuilder.taskId("3").create();

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            try {
                assertThat(cmmnHistoryService.createNativeHistoricTaskLogEntryQuery().sql("SELECT * FROM ACT_HI_TSK_LOG").list())
                    .hasSize(3);
                assertThat(cmmnHistoryService.createNativeHistoricTaskLogEntryQuery().sql("SELECT count(*) FROM ACT_HI_TSK_LOG").count())
                    .isEqualTo(3);

                assertThat(cmmnHistoryService.createNativeHistoricTaskLogEntryQuery().parameter("taskId", "1")
                    .sql("SELECT * FROM ACT_HI_TSK_LOG WHERE TASK_ID_ = #{taskId}").list())
                    .hasSize(1);
                assertThat(cmmnHistoryService.createNativeHistoricTaskLogEntryQuery().parameter("taskId", "1")
                    .sql("SELECT count(*) FROM ACT_HI_TSK_LOG WHERE TASK_ID_ = #{taskId}").count())
                    .isEqualTo(1);
            } finally {
                deleteTaskWithLogEntries("1");
                deleteTaskWithLogEntries("2");
                deleteTaskWithLogEntries("3");
            }
        }
    }

    @Test
    public void testTaskLogEntriesDeletedOnDeploymentDelete() {
        CaseInstance caseInstance = null;
        try {
            cmmnRepositoryService.createDeployment()
                    .addClasspathResource("org/flowable/cmmn/test/history/CmmnHistoryServiceTaskLogTest.testTaskLogEntriesDeletedOnDeploymentDelete.cmmn")
                    .deploy();
            caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("myCase").start();
            Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
            cmmnTaskService.complete(task.getId());

            if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
                assertThat(cmmnHistoryService.createHistoricTaskLogEntryQuery().caseInstanceId(caseInstance.getId()).count()).isEqualTo(6);
                assertThat(cmmnHistoryService.createHistoricTaskLogEntryQuery().caseDefinitionId(caseInstance.getCaseDefinitionId()).count()).isEqualTo(6);
            }

        } finally {
            CmmnTestHelper.deleteDeployment(cmmnEngineConfiguration,
                cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionId(caseInstance.getCaseDefinitionId()).singleResult().getDeploymentId());

            if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
                assertThat(cmmnHistoryService.createHistoricTaskLogEntryQuery().caseInstanceId(caseInstance.getId()).count()).isZero();
                assertThat(cmmnHistoryService.createHistoricTaskLogEntryQuery().caseDefinitionId(caseInstance.getCaseDefinitionId()).count()).isZero();
            }
        }
    }

    protected Date getInsertDate() {
        Calendar cal = new GregorianCalendar(2019, 3, 10);
        return cal.getTime();
    }

    protected Date getCompareBeforeDate() {
        Calendar cal = new GregorianCalendar(2019, 3, 9);
        return cal.getTime();
    }

    protected Date getCompareAfterDate() {
        Calendar cal = new GregorianCalendar(2019, 3, 11);
        return cal.getTime();
    }
}
