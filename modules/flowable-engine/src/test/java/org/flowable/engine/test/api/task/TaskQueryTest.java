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
import static org.assertj.core.api.Assertions.entry;
import static org.assertj.core.api.Assertions.tuple;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.impl.AbstractEngineConfiguration;
import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.engine.impl.test.HistoryTestHelper;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.identitylink.api.IdentityLinkInfo;
import org.flowable.identitylink.api.IdentityLinkType;
import org.flowable.task.api.DelegationState;
import org.flowable.task.api.Task;
import org.flowable.task.api.TaskInfo;
import org.flowable.task.api.TaskQuery;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.flowable.task.service.impl.persistence.entity.TaskEntity;
import org.flowable.variable.service.impl.persistence.entity.VariableInstanceEntity;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * @author Joram Barrez
 * @author Frederik Heremans
 * @author Falko Menge
 * @author Filip Hrisafov
 */
public class TaskQueryTest extends PluggableFlowableTestCase {

    private List<String> taskIds;

    @BeforeEach
    public void setUp() throws Exception {

        identityService.saveUser(identityService.newUser("kermit"));
        identityService.saveUser(identityService.newUser("gonzo"));
        identityService.saveUser(identityService.newUser("fozzie"));

        identityService.saveGroup(identityService.newGroup("management"));
        identityService.saveGroup(identityService.newGroup("accountancy"));

        identityService.createMembership("kermit", "management");
        identityService.createMembership("kermit", "accountancy");
        identityService.createMembership("fozzie", "management");

        taskIds = generateTestTasks();
    }

    @AfterEach
    public void tearDown() throws Exception {
        identityService.deleteGroup("accountancy");
        identityService.deleteGroup("management");
        identityService.deleteUser("fozzie");
        identityService.deleteUser("gonzo");
        identityService.deleteUser("kermit");
        taskService.deleteTasks(taskIds, true);
    }

    @Test
    public void testBasicTaskPropertiesNotNull() {
        org.flowable.task.api.Task task = taskService.createTaskQuery().taskId(taskIds.get(0)).singleResult();
        assertThat(task.getDescription()).isNotNull();
        assertThat(task.getId()).isNotNull();
        assertThat(task.getName()).isNotNull();
        assertThat(task.getCreateTime()).isNotNull();
    }

    @Test
    public void testQueryNoCriteria() {
        TaskQuery query = taskService.createTaskQuery();
        assertThat(query.count()).isEqualTo(12);
        assertThat(query.list()).hasSize(12);
        assertThatThrownBy(() -> query.singleResult())
                .isExactlyInstanceOf(FlowableException.class);
    }

    @Test
    public void testQueryByTaskId() {
        TaskQuery query = taskService.createTaskQuery().taskId(taskIds.get(0));
        assertThat(query.singleResult()).isNotNull();
        assertThat(query.list()).hasSize(1);
        assertThat(query.count()).isEqualTo(1);
    }

    @Test
    public void testQueryByTaskIdOr() {
        TaskQuery query = taskService.createTaskQuery().or().taskId(taskIds.get(0)).taskName("INVALID NAME").endOr();
        assertThat(query.singleResult()).isNotNull();
        assertThat(query.list()).hasSize(1);
        assertThat(query.count()).isEqualTo(1);
    }

    @Test
    public void testQueryByInvalidTaskId() {
        TaskQuery query = taskService.createTaskQuery().taskId("invalid");
        assertThat(query.singleResult()).isNull();
        assertThat(query.list()).isEmpty();
        assertThat(query.count()).isZero();

        assertThatThrownBy(() -> taskService.createTaskQuery().taskId(null))
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class);
    }

    @Test
    public void testQueryByInvalidTaskIdOr() {
        TaskQuery query = taskService.createTaskQuery().or().taskId("invalid").taskName("invalid");
        assertThat(query.singleResult()).isNull();
        assertThat(query.list()).isEmpty();
        assertThat(query.count()).isZero();

        assertThatThrownBy(() -> taskService.createTaskQuery().taskId(null))
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class);
    }

    @Test
    public void testQueryByName() {
        TaskQuery query = taskService.createTaskQuery().taskName("testTask");
        assertThat(query.list()).hasSize(6);
        assertThat(query.count()).isEqualTo(6);

        assertThatThrownBy(() -> query.singleResult())
                .isExactlyInstanceOf(FlowableException.class);
    }

    @Test
    public void testQueryByNameOr() {
        TaskQuery query = taskService.createTaskQuery().or().taskName("testTask").taskId("invalid");
        assertThat(query.list()).hasSize(6);
        assertThat(query.count()).isEqualTo(6);

        assertThatThrownBy(() -> query.singleResult())
                .isExactlyInstanceOf(FlowableException.class);
    }

    @Test
    public void testQueryByInvalidName() {
        TaskQuery query = taskService.createTaskQuery().taskName("invalid");
        assertThat(query.singleResult()).isNull();
        assertThat(query.list()).isEmpty();
        assertThat(query.count()).isZero();

        assertThatThrownBy(() -> taskService.createTaskQuery().or().taskId("invalid").taskName(null).singleResult())
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class);
    }

    @Test
    public void testQueryByInvalidNameOr() {
        TaskQuery query = taskService.createTaskQuery().or().taskId("invalid").taskName("invalid");
        assertThat(query.singleResult()).isNull();
        assertThat(query.list()).isEmpty();
        assertThat(query.count()).isZero();

        assertThatThrownBy(() -> taskService.createTaskQuery().or().taskId("invalid").taskName(null).singleResult())
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class);
    }

    @Test
    public void testQueryByNameIn() {
        final List<String> taskNameList = new ArrayList<>(2);
        taskNameList.add("testTask");
        taskNameList.add("gonzoTask");

        TaskQuery query = taskService.createTaskQuery().taskNameIn(taskNameList);
        assertThat(query.list()).hasSize(7);
        assertThat(query.count()).isEqualTo(7);

        assertThatThrownBy(() -> query.singleResult())
                .isExactlyInstanceOf(FlowableException.class);
    }

    @Test
    public void testQueryByNameInIgnoreCase() {
        final List<String> taskNameList = new ArrayList<>(2);
        taskNameList.add("testtask");
        taskNameList.add("gonzotask");

        TaskQuery query = taskService.createTaskQuery().taskNameInIgnoreCase(taskNameList);
        assertThat(query.list()).hasSize(7);
        assertThat(query.count()).isEqualTo(7);

        assertThatThrownBy(() -> query.singleResult())
                .isExactlyInstanceOf(FlowableException.class);
    }

    @Test
    public void testQueryByNameInOr() {
        final List<String> taskNameList = new ArrayList<>(2);
        taskNameList.add("testTask");
        taskNameList.add("gonzoTask");

        TaskQuery query = taskService.createTaskQuery().or().taskNameIn(taskNameList).taskId("invalid");
        assertThat(query.list()).hasSize(7);
        assertThat(query.count()).isEqualTo(7);

        assertThatThrownBy(() -> query.singleResult())
                .isExactlyInstanceOf(FlowableException.class);
    }

    @Test
    public void testQueryByNameInIgnoreCaseOr() {
        final List<String> taskNameList = new ArrayList<>(2);
        taskNameList.add("testtask");
        taskNameList.add("gonzotask");

        TaskQuery query = taskService.createTaskQuery().or().taskNameInIgnoreCase(taskNameList).taskId("invalid");
        assertThat(query.list()).hasSize(7);
        assertThat(query.count()).isEqualTo(7);

        assertThatThrownBy(() -> query.singleResult())
                .isExactlyInstanceOf(FlowableException.class);
    }

    @Test
    public void testQueryByInvalidNameIn() {
        final List<String> taskNameList = new ArrayList<>(1);
        taskNameList.add("invalid");

        TaskQuery query = taskService.createTaskQuery().taskNameIn(taskNameList);
        assertThat(query.list()).isEmpty();
        assertThat(query.count()).isZero();

        assertThatThrownBy(() -> taskService.createTaskQuery().or().taskId("invalid").taskNameIn(null).singleResult())
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class);
    }

    @Test
    public void testQueryByInvalidNameInIgnoreCase() {
        final List<String> taskNameList = new ArrayList<>(1);
        taskNameList.add("invalid");

        TaskQuery query = taskService.createTaskQuery().taskNameInIgnoreCase(taskNameList);
        assertThat(query.list()).isEmpty();
        assertThat(query.count()).isZero();

        assertThatThrownBy(() -> taskService.createTaskQuery().or().taskId("invalid").taskNameIn(null).singleResult())
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class);
    }

    @Test
    public void testQueryByInvalidNameInOr() {
        final List<String> taskNameList = new ArrayList<>(2);
        taskNameList.add("invalid");

        TaskQuery query = taskService.createTaskQuery().or().taskNameIn(taskNameList).taskId("invalid");
        assertThat(query.list()).isEmpty();
        assertThat(query.count()).isZero();

        assertThatThrownBy(() -> taskService.createTaskQuery().or().taskId("invalid").taskNameIn(null).singleResult())
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class);
    }

    @Test
    public void testQueryByInvalidNameInIgnoreCaseOr() {
        final List<String> taskNameList = new ArrayList<>(2);
        taskNameList.add("invalid");

        TaskQuery query = taskService.createTaskQuery().or().taskNameInIgnoreCase(taskNameList).taskId("invalid");
        assertThat(query.list()).isEmpty();
        assertThat(query.count()).isZero();

        assertThatThrownBy(() -> taskService.createTaskQuery().or().taskId("invalid").taskNameIn(null).singleResult())
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class);
    }

    @Test
    public void testQueryByNameLike() {
        TaskQuery query = taskService.createTaskQuery().taskNameLike("gonzo%");
        assertThat(query.singleResult()).isNotNull();
        assertThat(query.list()).hasSize(1);
        assertThat(query.count()).isEqualTo(1);
    }

    @Test
    public void testQueryByNameLikeOr() {
        TaskQuery query = taskService.createTaskQuery().or().taskId("invalid").taskNameLike("gonzo%");
        assertThat(query.singleResult()).isNotNull();
        assertThat(query.list()).hasSize(1);
        assertThat(query.count()).isEqualTo(1);
    }

    @Test
    public void testQueryByInvalidNameLike() {
        TaskQuery query = taskService.createTaskQuery().taskNameLike("1");
        assertThat(query.singleResult()).isNull();
        assertThat(query.list()).isEmpty();
        assertThat(query.count()).isZero();

        assertThatThrownBy(() -> taskService.createTaskQuery().or().taskId("invalid").taskNameLike(null).singleResult())
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class);
    }

    @Test
    public void testQueryByInvalidNameLikeOr() {
        TaskQuery query = taskService.createTaskQuery().or().taskId("invalid").taskNameLike("1");
        assertThat(query.singleResult()).isNull();
        assertThat(query.list()).isEmpty();
        assertThat(query.count()).isZero();

        assertThatThrownBy(() -> taskService.createTaskQuery().or().taskId("invalid").taskNameLike(null).singleResult())
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class);
    }

    @Test
    public void testQueryByDescription() {
        TaskQuery query = taskService.createTaskQuery().taskDescription("testTask description");
        assertThat(query.list()).hasSize(6);
        assertThat(query.count()).isEqualTo(6);

        assertThatThrownBy(() -> query.singleResult())
                .isExactlyInstanceOf(FlowableException.class);
    }

    @Test
    public void testQueryByDescriptionOr() {
        TaskQuery query = taskService.createTaskQuery().or().taskId("invalid").taskDescription("testTask description");
        assertThat(query.list()).hasSize(6);
        assertThat(query.count()).isEqualTo(6);

        assertThatThrownBy(() -> query.singleResult())
                .isExactlyInstanceOf(FlowableException.class);
    }

    @Test
    public void testQueryByInvalidDescription() {
        TaskQuery query = taskService.createTaskQuery().taskDescription("invalid");
        assertThat(query.singleResult()).isNull();
        assertThat(query.list()).isEmpty();
        assertThat(query.count()).isZero();

        assertThatThrownBy(() -> taskService.createTaskQuery().or().taskId("invalid").taskDescription(null).list())
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class);
    }

    @Test
    public void testQueryByInvalidDescriptionOr() {
        TaskQuery query = taskService.createTaskQuery().or().taskId("invalid").taskDescription("invalid");
        assertThat(query.singleResult()).isNull();
        assertThat(query.list()).isEmpty();
        assertThat(query.count()).isZero();

        assertThatThrownBy(() -> taskService.createTaskQuery().or().taskId("invalid").taskDescription(null).list())
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class);
    }

    @Test
    public void testQueryByDescriptionLike() {
        TaskQuery query = taskService.createTaskQuery().taskDescriptionLike("%gonzo%");
        assertThat(query.singleResult()).isNotNull();
        assertThat(query.list()).hasSize(1);
        assertThat(query.count()).isEqualTo(1);
    }

    @Test
    public void testQueryByDescriptionLikeOr() {
        TaskQuery query = taskService.createTaskQuery().or().taskId("invalid").taskDescriptionLike("%gonzo%");
        assertThat(query.singleResult()).isNotNull();
        assertThat(query.list()).hasSize(1);
        assertThat(query.count()).isEqualTo(1);
    }

    @Test
    public void testQueryByInvalidDescriptionLike() {
        TaskQuery query = taskService.createTaskQuery().taskDescriptionLike("invalid");
        assertThat(query.singleResult()).isNull();
        assertThat(query.list()).isEmpty();
        assertThat(query.count()).isZero();

        assertThatThrownBy(() -> taskService.createTaskQuery().or().taskId("invalid").taskDescriptionLike(null).list())
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class);
    }

    @Test
    public void testQueryByInvalidDescriptionLikeOr() {
        TaskQuery query = taskService.createTaskQuery().or().taskId("invalid").taskDescriptionLike("invalid");
        assertThat(query.singleResult()).isNull();
        assertThat(query.list()).isEmpty();
        assertThat(query.count()).isZero();

        assertThatThrownBy(() -> taskService.createTaskQuery().or().taskId("invalid").taskDescriptionLike(null).list())
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class);
    }

    @Test
    public void testQueryByFormKey() {
        Task task = taskService.newTask();
        task.setFormKey("testFormKey");
        taskService.saveTask(task);
        taskIds.add(task.getId());

        List<Task> tasks = taskService.createTaskQuery().taskFormKey("testFormKey").list();
        assertThat(tasks)
                .extracting(Task::getFormKey)
                .containsExactly("testFormKey");

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
            List<HistoricTaskInstance> historicTasks = historyService.createHistoricTaskInstanceQuery()
                    .taskFormKey("testFormKey")
                    .list();
            assertThat(historicTasks)
                    .extracting(HistoricTaskInstance::getFormKey)
                    .containsExactly("testFormKey");
        }
    }

    @Test
    public void testQueryByFormKeyOr() {
        Task task = taskService.newTask();
        task.setFormKey("testFormKey");
        taskService.saveTask(task);
        taskIds.add(task.getId());

        List<Task> tasks = taskService.createTaskQuery().or().taskId("invalid").taskFormKey("testFormKey").list();
        assertThat(tasks)
                .extracting(Task::getFormKey)
                .containsExactly("testFormKey");

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
            List<HistoricTaskInstance> historicTasks = historyService.createHistoricTaskInstanceQuery().or()
                    .taskFormKey("testFormKey")
                    .list();
            assertThat(historicTasks)
                    .extracting(HistoricTaskInstance::getFormKey)
                    .containsExactly("testFormKey");
        }
    }

    @Test
    public void testQueryWithFormKey() {
        Task task = taskService.newTask();
        task.setFormKey("testFormKey");
        taskService.saveTask(task);
        taskIds.add(task.getId());

        List<Task> tasks = taskService.createTaskQuery().taskWithFormKey().list();
        assertThat(tasks)
                .extracting(Task::getFormKey)
                .containsExactly("testFormKey");

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
            List<HistoricTaskInstance> historicTasks = historyService.createHistoricTaskInstanceQuery()
                    .taskWithFormKey()
                    .list();
            assertThat(historicTasks)
                    .extracting(HistoricTaskInstance::getFormKey)
                    .containsExactly("testFormKey");
        }
    }

    @Test
    public void testQueryByPriority() {
        TaskQuery query = taskService.createTaskQuery().taskPriority(10);
        assertThat(query.list()).hasSize(2);
        assertThat(query.count()).isEqualTo(2);

        TaskQuery finalQuery = query;
        assertThatThrownBy(() -> finalQuery.singleResult())
                .isExactlyInstanceOf(FlowableException.class);

        query = taskService.createTaskQuery().taskPriority(100);
        assertThat(query.singleResult()).isNull();
        assertThat(query.list()).isEmpty();
        assertThat(query.count()).isZero();

        query = taskService.createTaskQuery().taskMinPriority(50);
        assertThat(query.list()).hasSize(3);

        query = taskService.createTaskQuery().taskMinPriority(10);
        assertThat(query.list()).hasSize(5);

        query = taskService.createTaskQuery().taskMaxPriority(10);
        assertThat(query.list()).hasSize(9);

        query = taskService.createTaskQuery().taskMaxPriority(3);
        assertThat(query.list()).hasSize(6);
    }

    @Test
    public void testQueryByPriorityOr() {
        TaskQuery query = taskService.createTaskQuery().or().taskId("invalid").taskPriority(10);
        assertThat(query.list()).hasSize(2);
        assertThat(query.count()).isEqualTo(2);

        TaskQuery finalQuery = query;
        assertThatThrownBy(() -> finalQuery.singleResult())
                .isExactlyInstanceOf(FlowableException.class);

        query = taskService.createTaskQuery().or().taskId("invalid").taskPriority(100);
        assertThat(query.singleResult()).isNull();
        assertThat(query.list()).isEmpty();
        assertThat(query.count()).isZero();

        query = taskService.createTaskQuery().or().taskId("invalid").taskMinPriority(50);
        assertThat(query.list()).hasSize(3);

        query = taskService.createTaskQuery().or().taskId("invalid").taskMinPriority(10);
        assertThat(query.list()).hasSize(5);

        query = taskService.createTaskQuery().or().taskId("invalid").taskMaxPriority(10);
        assertThat(query.list()).hasSize(9);

        query = taskService.createTaskQuery().or().taskId("invalid").taskMaxPriority(3);
        assertThat(query.list()).hasSize(6);
    }

    @Test
    public void testQueryByInvalidPriority() {
        assertThatThrownBy(() -> taskService.createTaskQuery().taskPriority(null))
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class);
    }

    @Test
    public void testQueryByInvalidPriorityOr() {
        assertThatThrownBy(() -> taskService.createTaskQuery().or().taskId("invalid").taskPriority(null))
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class);
    }

    @Test
    public void testQueryByAssignee() {
        TaskQuery query = taskService.createTaskQuery().taskAssignee("gonzo");
        assertThat(query.count()).isEqualTo(1);
        assertThat(query.list()).hasSize(1);
        assertThat(query.singleResult()).isNotNull();

        query = taskService.createTaskQuery().taskAssignee("kermit");
        assertThat(query.count()).isZero();
        assertThat(query.list()).isEmpty();
        assertThat(query.singleResult()).isNull();
    }

    @Test
    public void testQueryByAssigneeOr() {
        TaskQuery query = taskService.createTaskQuery().or().taskId("invalid").taskAssignee("gonzo");
        assertThat(query.count()).isEqualTo(1);
        assertThat(query.list()).hasSize(1);
        assertThat(query.singleResult()).isNotNull();

        query = taskService.createTaskQuery().or().taskId("invalid").taskAssignee("kermit");
        assertThat(query.count()).isZero();
        assertThat(query.list()).isEmpty();
        assertThat(query.singleResult()).isNull();
    }

    @Test
    public void testQueryByAssigneeIds() {
        TaskQuery query = taskService.createTaskQuery().taskAssigneeIds(Arrays.asList("gonzo", "kermit"));
        assertThat(query.count()).isEqualTo(1);
        assertThat(query.list()).hasSize(1);
        assertThat(query.singleResult()).isNotNull();

        query = taskService.createTaskQuery().taskAssigneeIds(Arrays.asList("kermit", "kermit2"));
        assertThat(query.count()).isZero();
        assertThat(query.list()).isEmpty();
        assertThat(query.singleResult()).isNull();

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
            // History
            assertThat(historyService.createHistoricTaskInstanceQuery().taskAssigneeIds(Arrays.asList("gonzo", "kermit")).count()).isEqualTo(1);
            assertThat(historyService.createHistoricTaskInstanceQuery().taskAssigneeIds(Arrays.asList("kermit", "kermit2")).count()).isZero();
        }

        org.flowable.task.api.Task adhocTask = taskService.newTask();
        adhocTask.setName("test");
        adhocTask.setAssignee("testAssignee");
        taskService.saveTask(adhocTask);

        query = taskService.createTaskQuery().taskAssigneeIds(Arrays.asList("gonzo", "testAssignee"));
        assertThat(query.count()).isEqualTo(2);
        assertThat(query.list()).hasSize(2);

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
            // History
            assertThat(historyService.createHistoricTaskInstanceQuery().taskAssigneeIds(Arrays.asList("gonzo", "testAssignee")).count()).isEqualTo(2);
        }

        taskService.deleteTask(adhocTask.getId(), true);
    }

    @Test
    public void testQueryByAssigneeIdsOr() {
        TaskQuery query = taskService.createTaskQuery().or().taskId("invalid").taskAssigneeIds(Arrays.asList("gonzo", "kermit"));
        assertThat(query.count()).isEqualTo(1);
        assertThat(query.list()).hasSize(1);
        assertThat(query.singleResult()).isNotNull();

        query = taskService.createTaskQuery().or().taskId("invalid").taskAssigneeIds(Arrays.asList("kermit", "kermit2"));
        assertThat(query.count()).isZero();
        assertThat(query.list()).isEmpty();
        assertThat(query.singleResult()).isNull();

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
            // History
            assertThat(historyService.createHistoricTaskInstanceQuery().or().taskId("invalid").taskAssigneeIds(Arrays.asList("gonzo", "kermit")).count())
                    .isEqualTo(1);
            assertThat(historyService.createHistoricTaskInstanceQuery().or().taskId("invalid").taskAssigneeIds(Arrays.asList("kermit", "kermit2")).count())
                    .isZero();
        }

        org.flowable.task.api.Task adhocTask = taskService.newTask();
        adhocTask.setName("test");
        adhocTask.setAssignee("testAssignee");
        taskService.saveTask(adhocTask);

        query = taskService.createTaskQuery().or().taskId("invalid").taskAssigneeIds(Arrays.asList("gonzo", "testAssignee"));
        assertThat(query.count()).isEqualTo(2);
        assertThat(query.list()).hasSize(2);

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
            // History
            assertThat(historyService.createHistoricTaskInstanceQuery().or().taskId("invalid").taskAssigneeIds(Arrays.asList("gonzo", "testAssignee")).count())
                    .isEqualTo(2);
        }

        taskService.deleteTask(adhocTask.getId(), true);
    }

    @Test
    public void testQueryByInvolvedUser() {
        try {
            org.flowable.task.api.Task adhocTask = taskService.newTask();
            adhocTask.setAssignee("kermit");
            adhocTask.setOwner("fozzie");
            taskService.saveTask(adhocTask);
            taskService.addUserIdentityLink(adhocTask.getId(), "gonzo", "customType");

            assertThat(taskService.getIdentityLinksForTask(adhocTask.getId())).hasSize(3);

            assertThat(taskService.createTaskQuery().taskId(adhocTask.getId()).taskInvolvedUser("gonzo").count()).isEqualTo(1);
            assertThat(taskService.createTaskQuery().taskId(adhocTask.getId()).taskInvolvedUser("kermit").count()).isEqualTo(1);
            assertThat(taskService.createTaskQuery().taskId(adhocTask.getId()).taskInvolvedUser("fozzie").count()).isEqualTo(1);

        } finally {
            deleteAllTasks();
        }
    }

    @Test
    public void testQueryByInvolvedUserOr() {
        try {
            org.flowable.task.api.Task adhocTask = taskService.newTask();
            adhocTask.setAssignee("kermit");
            adhocTask.setOwner("fozzie");
            taskService.saveTask(adhocTask);
            taskService.addUserIdentityLink(adhocTask.getId(), "gonzo", "customType");

            assertThat(taskService.getIdentityLinksForTask(adhocTask.getId())).hasSize(3);

            assertThat(taskService.createTaskQuery().taskId(adhocTask.getId()).or().taskId("invalid").taskInvolvedUser("gonzo").count()).isEqualTo(1);
            assertThat(taskService.createTaskQuery().taskId(adhocTask.getId()).or().taskId("invalid").taskInvolvedUser("kermit").count()).isEqualTo(1);
            assertThat(taskService.createTaskQuery().taskId(adhocTask.getId()).or().taskId("invalid").taskInvolvedUser("fozzie").count()).isEqualTo(1);

            if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
                assertThat(historyService.createHistoricTaskInstanceQuery().taskId(adhocTask.getId())
                        .or().taskId("invalid").taskInvolvedUser("fozzie").count()).isEqualTo(1);
            }

        } finally {
            deleteAllTasks();
        }
    }

    @Test
    public void testQueryByInvolvedGroups() {
        try {
            org.flowable.task.api.Task adhocTask = taskService.newTask();
            taskService.saveTask(adhocTask);
            taskService.addGroupIdentityLink(adhocTask.getId(), "testGroup", "customType");

            assertThat(taskService.getIdentityLinksForTask(adhocTask.getId())).hasSize(1);

            assertThat(taskService.createTaskQuery().taskId(adhocTask.getId()).taskInvolvedGroups(Collections.singleton("testGroup")).count()).isEqualTo(1);

            // SQL Server has a limit of 2100 on how many parameters a query might have
            int maxGroups = AbstractEngineConfiguration.DATABASE_TYPE_MSSQL.equals(processEngineConfiguration.getDatabaseType()) ? 2050 : 2100;

            List<String> testCandidateGroups = new ArrayList<>(maxGroups);
            for (int i = 0; i < maxGroups; i++) {
                testCandidateGroups.add("group" + i);
            }
            
            TaskQuery query = taskService.createTaskQuery().taskInvolvedGroups(testCandidateGroups);
            assertThat(query.count()).isEqualTo(0);
            assertThat(query.list()).hasSize(0);
            
            testCandidateGroups.add("testGroup");
            query = taskService.createTaskQuery().taskInvolvedGroups(testCandidateGroups);
            assertThat(query.count()).isEqualTo(1);
            assertThat(query.list()).hasSize(1);

        } finally {
            deleteAllTasks();
        }
    }

    @Test
    public void testQueryByInvolvedGroupOr() {
        try {
            org.flowable.task.api.Task adhocTask = taskService.newTask();
            adhocTask.setName("testtask");
            taskService.saveTask(adhocTask);
            taskService.addGroupIdentityLink(adhocTask.getId(), "testGroup", "customType");

            assertThat(taskService.getIdentityLinksForTask(adhocTask.getId())).hasSize(1);

            assertThat(taskService.createTaskQuery().taskId(adhocTask.getId()).or().taskId("invalid").taskInvolvedGroups(Collections.singleton("testGroup"))
                    .count()).isEqualTo(1);

            // SQL Server has a limit of 2100 on how many parameters a query might have
            int maxGroups = AbstractEngineConfiguration.DATABASE_TYPE_MSSQL.equals(processEngineConfiguration.getDatabaseType()) ? 2050 : 2100;

            List<String> testCandidateGroups = new ArrayList<>(maxGroups);
            for (int i = 0; i < maxGroups; i++) {
                testCandidateGroups.add("group" + i);
            }
            
            TaskQuery query = taskService.createTaskQuery().or().taskName("testtask").taskInvolvedGroups(testCandidateGroups).endOr();
            assertThat(query.count()).isEqualTo(1);
            assertThat(query.list()).hasSize(1);
            
            query = taskService.createTaskQuery().or().taskName("undefined").taskInvolvedGroups(testCandidateGroups).endOr();
            assertThat(query.count()).isEqualTo(0);
            assertThat(query.list()).hasSize(0);

            if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
                assertThat(historyService.createHistoricTaskInstanceQuery().taskId(adhocTask.getId()).or().taskId("invalid")
                        .taskInvolvedGroups(Collections.singleton("testGroup")).count()).isEqualTo(1);
            }

        } finally {
            deleteAllTasks();
        }
    }

    private void deleteAllTasks() {
        List<org.flowable.task.api.Task> allTasks = taskService.createTaskQuery().list();
        for (org.flowable.task.api.Task task : allTasks) {
            if (task.getExecutionId() == null) {
                taskService.deleteTask(task.getId(), true);
            }
        }
    }

    @Test
    public void testQueryByInvolvedGroupOrAssignee() {
        try {
            org.flowable.task.api.Task adhocTask = taskService.newTask();
            taskService.saveTask(adhocTask);
            org.flowable.task.api.Task adhocTask2 = taskService.newTask();
            adhocTask2.setAssignee("kermit");
            taskService.saveTask(adhocTask2);
            org.flowable.task.api.Task adhocTask3 = taskService.newTask();
            taskService.saveTask(adhocTask3);
            taskService.addGroupIdentityLink(adhocTask.getId(), "testGroup", "customType");

            assertThat(taskService.getIdentityLinksForTask(adhocTask.getId())).hasSize(1);

            assertThat(taskService.createTaskQuery().or().taskAssignee("kermit").taskInvolvedGroups(Collections.singleton("testGroup")).endOr().count())
                    .isEqualTo(2);

            if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
                assertThat(historyService.createHistoricTaskInstanceQuery().
                        or().taskAssignee("kermit").taskInvolvedGroups(Collections.singleton("testGroup")).endOr().count()).isEqualTo(2);
            }

        } finally {
            deleteAllTasks();
        }
    }

    @Test
    public void testQueryByInvolvedGroupOrOwner() {
        try {
            org.flowable.task.api.Task adhocTask = taskService.newTask();
            taskService.saveTask(adhocTask);
            org.flowable.task.api.Task adhocTask2 = taskService.newTask();
            adhocTask2.setOwner("kermit");
            taskService.saveTask(adhocTask2);
            org.flowable.task.api.Task adhocTask3 = taskService.newTask();
            taskService.saveTask(adhocTask3);
            taskService.addGroupIdentityLink(adhocTask.getId(), "testGroup", "customType");

            assertThat(taskService.getIdentityLinksForTask(adhocTask.getId())).hasSize(1);

            assertThat(taskService.createTaskQuery().or().taskOwner("kermit").taskInvolvedGroups(Collections.singleton("testGroup")).endOr().count())
                    .isEqualTo(2);

            if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
                assertThat(
                        historyService.createHistoricTaskInstanceQuery().or().taskOwner("kermit").taskInvolvedGroups(Collections.singleton("testGroup")).endOr()
                                .count()).isEqualTo(2);
            }

        } finally {
            deleteAllTasks();
        }
    }

    @Test
    public void testQueryByInvolvedGroupAndAssignee() {
        try {
            org.flowable.task.api.Task adhocTask = taskService.newTask();
            taskService.saveTask(adhocTask);
            org.flowable.task.api.Task adhocTask2 = taskService.newTask();
            adhocTask2.setAssignee("kermit");
            taskService.saveTask(adhocTask2);
            org.flowable.task.api.Task adhocTask3 = taskService.newTask();
            adhocTask3.setAssignee("kermit");
            taskService.saveTask(adhocTask3);
            taskService.addGroupIdentityLink(adhocTask.getId(), "testGroup", "customType");
            taskService.addGroupIdentityLink(adhocTask3.getId(), "testGroup", "customType");

            assertThat(taskService.createTaskQuery().taskAssignee("kermit").taskInvolvedGroups(Collections.singleton("testGroup")).count()).isEqualTo(1);

            if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
                assertThat(
                        historyService.createHistoricTaskInstanceQuery().taskAssignee("kermit").taskInvolvedGroups(Collections.singleton("testGroup")).count())
                        .isEqualTo(1);
            }

        } finally {
            deleteAllTasks();
        }
    }

    @Test
    public void testQueryByInvolvedGroupAndOwner() {
        try {
            org.flowable.task.api.Task adhocTask = taskService.newTask();
            taskService.saveTask(adhocTask);
            org.flowable.task.api.Task adhocTask2 = taskService.newTask();
            adhocTask2.setOwner("kermit");
            taskService.saveTask(adhocTask2);
            org.flowable.task.api.Task adhocTask3 = taskService.newTask();
            adhocTask3.setOwner("kermit");
            taskService.saveTask(adhocTask3);
            taskService.addGroupIdentityLink(adhocTask.getId(), "testGroup", "customType");
            taskService.addGroupIdentityLink(adhocTask3.getId(), "testGroup", "customType");

            assertThat(taskService.getIdentityLinksForTask(adhocTask.getId())).hasSize(1);

            assertThat(taskService.createTaskQuery().taskOwner("kermit").taskInvolvedGroups(Collections.singleton("testGroup")).count()).isEqualTo(1);

            if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
                assertThat(historyService.createHistoricTaskInstanceQuery().taskOwner("kermit").taskInvolvedGroups(Collections.singleton("testGroup")).count())
                        .isEqualTo(1);
            }

        } finally {
            deleteAllTasks();
        }
    }

    @Test
    public void testQueryByInvolvedGroupAndOwnerLike() {
        try {
            org.flowable.task.api.Task adhocTask = taskService.newTask();
            taskService.saveTask(adhocTask);
            org.flowable.task.api.Task adhocTask2 = taskService.newTask();
            adhocTask2.setOwner("kermit");
            taskService.saveTask(adhocTask2);
            org.flowable.task.api.Task adhocTask3 = taskService.newTask();
            adhocTask3.setOwner("kermit");
            taskService.saveTask(adhocTask3);
            taskService.addGroupIdentityLink(adhocTask.getId(), "testGroup", "customType");
            taskService.addGroupIdentityLink(adhocTask3.getId(), "testGroup", "customType");

            assertThat(taskService.getIdentityLinksForTask(adhocTask.getId())).hasSize(1);

            assertThat(taskService.createTaskQuery().taskOwnerLike("ker%").taskInvolvedGroups(Collections.singleton("testGroup")).count()).isEqualTo(1);
            if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
                assertThat(
                        historyService.createHistoricTaskInstanceQuery().taskOwnerLike("ker%").taskInvolvedGroups(Collections.singleton("testGroup")).count())
                        .isEqualTo(1);
            }

        } finally {
            deleteAllTasks();
        }
    }

    @Test
    public void testQueryByInvolvedGroupAndAssigneeLike() {
        try {
            org.flowable.task.api.Task adhocTask = taskService.newTask();
            taskService.saveTask(adhocTask);
            org.flowable.task.api.Task adhocTask2 = taskService.newTask();
            adhocTask2.setAssignee("kermit");
            taskService.saveTask(adhocTask2);
            org.flowable.task.api.Task adhocTask3 = taskService.newTask();
            adhocTask3.setAssignee("kermit");
            taskService.saveTask(adhocTask3);
            taskService.addGroupIdentityLink(adhocTask.getId(), "testGroup", "customType");
            taskService.addGroupIdentityLink(adhocTask3.getId(), "testGroup", "customType");

            assertThat(taskService.getIdentityLinksForTask(adhocTask.getId())).hasSize(1);

            assertThat(taskService.createTaskQuery().taskAssigneeLike("ker%").taskInvolvedGroups(Collections.singleton("testGroup")).count()).isEqualTo(1);

            if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
                assertThat(historyService.createHistoricTaskInstanceQuery().taskAssigneeLike("ker%").taskInvolvedGroups(Collections.singleton("testGroup"))
                        .count()).isEqualTo(1);
            }

        } finally {
            deleteAllTasks();
        }
    }

    @Test
    public void testQueryByInvolvedGroupAndAssigneeIds() {
        try {
            org.flowable.task.api.Task adhocTask = taskService.newTask();
            taskService.saveTask(adhocTask);
            org.flowable.task.api.Task adhocTask2 = taskService.newTask();
            adhocTask2.setAssignee("kermit");
            taskService.saveTask(adhocTask2);
            org.flowable.task.api.Task adhocTask3 = taskService.newTask();
            adhocTask3.setAssignee("kermit");
            taskService.saveTask(adhocTask3);
            taskService.addGroupIdentityLink(adhocTask.getId(), "testGroup", "customType");
            taskService.addGroupIdentityLink(adhocTask3.getId(), "testGroup", "customType");

            assertThat(taskService.getIdentityLinksForTask(adhocTask.getId())).hasSize(1);

            assertThat(taskService.createTaskQuery().taskAssigneeIds(Collections.singletonList("kermit")).taskInvolvedGroups(Collections.singleton("testGroup"))
                    .count()).isEqualTo(1);

            if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
                assertThat(historyService.createHistoricTaskInstanceQuery().taskAssigneeIds(Collections.singletonList("kermit"))
                        .taskInvolvedGroups(Collections.singleton("testGroup")).count()).isEqualTo(1);
            }

        } finally {
            deleteAllTasks();
        }
    }

    @Test
    public void testQueryByInvolvedGroupOrOwnerLike() {
        try {
            org.flowable.task.api.Task adhocTask = taskService.newTask();
            taskService.saveTask(adhocTask);
            org.flowable.task.api.Task adhocTask2 = taskService.newTask();
            adhocTask2.setOwner("kermit");
            taskService.saveTask(adhocTask2);
            org.flowable.task.api.Task adhocTask3 = taskService.newTask();
            adhocTask3.setOwner("kermit");
            taskService.saveTask(adhocTask3);
            taskService.addGroupIdentityLink(adhocTask.getId(), "testGroup", "customType");
            taskService.addGroupIdentityLink(adhocTask3.getId(), "testGroup", "customType");

            assertThat(taskService.createTaskQuery()
                    .or()
                    .taskOwnerLike("ker%").taskInvolvedGroups(Collections.singleton("testGroup"))
                    .endOr()
                    .count()).isEqualTo(3);

            if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
                assertThat(historyService.createHistoricTaskInstanceQuery()
                        .or()
                        .taskOwnerLike("ker%").taskInvolvedGroups(Collections.singleton("testGroup"))
                        .endOr()
                        .count()).isEqualTo(3);
            }

        } finally {
            deleteAllTasks();
        }
    }

    @Test
    public void testQueryByInvolvedGroupOrAssigneeLike() {
        try {
            org.flowable.task.api.Task adhocTask = taskService.newTask();
            taskService.saveTask(adhocTask);
            org.flowable.task.api.Task adhocTask2 = taskService.newTask();
            adhocTask2.setAssignee("kermit");
            taskService.saveTask(adhocTask2);
            org.flowable.task.api.Task adhocTask3 = taskService.newTask();
            adhocTask3.setAssignee("kermit");
            taskService.saveTask(adhocTask3);
            taskService.addGroupIdentityLink(adhocTask.getId(), "testGroup", "customType");
            taskService.addGroupIdentityLink(adhocTask3.getId(), "testGroup", "customType");

            assertThat(taskService.createTaskQuery()
                    .or()
                    .taskAssigneeLike("ker%").taskInvolvedGroups(Collections.singleton("testGroup"))
                    .endOr()
                    .count()).isEqualTo(3);

            if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
                assertThat(historyService.createHistoricTaskInstanceQuery()
                        .or()
                        .taskAssigneeLike("ker%").taskInvolvedGroups(Collections.singleton("testGroup"))
                        .endOr()
                        .count()).isEqualTo(3);
            }

        } finally {
            deleteAllTasks();
        }
    }

    @Test
    public void testQueryByInvolvedGroupOrAssigneeIds() {
        try {
            org.flowable.task.api.Task adhocTask = taskService.newTask();
            taskService.saveTask(adhocTask);
            org.flowable.task.api.Task adhocTask2 = taskService.newTask();
            adhocTask2.setAssignee("kermit");
            taskService.saveTask(adhocTask2);
            org.flowable.task.api.Task adhocTask3 = taskService.newTask();
            adhocTask3.setAssignee("kermit");
            taskService.saveTask(adhocTask3);
            taskService.addGroupIdentityLink(adhocTask.getId(), "testGroup", "customType");
            taskService.addGroupIdentityLink(adhocTask3.getId(), "testGroup", "customType");

            assertThat(taskService.createTaskQuery()
                    .or()
                    .taskAssigneeIds(Collections.singletonList("kermit")).taskInvolvedGroups(Collections.singleton("testGroup"))
                    .endOr()
                    .count()).isEqualTo(3);

            if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
                assertThat(historyService.createHistoricTaskInstanceQuery()
                        .or()
                        .taskAssigneeIds(Collections.singletonList("kermit")).taskInvolvedGroups(Collections.singleton("testGroup"))
                        .endOr()
                        .count()).isEqualTo(3);
            }

        } finally {
            deleteAllTasks();
        }
    }

    @Test
    public void testQueryByInvolvedGroupOrAssigneeId() {
        try {
            org.flowable.task.api.Task adhocTask = taskService.newTask();
            taskService.saveTask(adhocTask);
            org.flowable.task.api.Task adhocTask2 = taskService.newTask();
            adhocTask2.setAssignee("kermit");
            taskService.saveTask(adhocTask2);
            org.flowable.task.api.Task adhocTask3 = taskService.newTask();
            adhocTask3.setAssignee("kermit");
            taskService.saveTask(adhocTask3);
            taskService.addGroupIdentityLink(adhocTask.getId(), "testGroup", "customType");
            taskService.addGroupIdentityLink(adhocTask3.getId(), "testGroup", "customType");

            assertThat(taskService.createTaskQuery()
                    .or()
                    .taskAssignee("kermit").taskInvolvedGroups(Collections.singleton("testGroup"))
                    .endOr()
                    .count()).isEqualTo(3);

            if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
                assertThat(historyService.createHistoricTaskInstanceQuery()
                        .or()
                        .taskAssignee("kermit").taskInvolvedGroups(Collections.singleton("testGroup"))
                        .endOr()
                        .count()).isEqualTo(3);
            }

        } finally {
            deleteAllTasks();
        }
    }

    @Test
    public void testQueryByInvolvedGroupTaskName() {
        try {
            org.flowable.task.api.Task adhocTask = taskService.newTask();
            taskService.saveTask(adhocTask);
            org.flowable.task.api.Task adhocTask2 = taskService.newTask();
            adhocTask2.setName("testName");
            taskService.saveTask(adhocTask2);
            org.flowable.task.api.Task adhocTask3 = taskService.newTask();
            taskService.saveTask(adhocTask3);
            org.flowable.task.api.Task adhocTask4 = taskService.newTask();
            adhocTask4.setName("testName");
            taskService.saveTask(adhocTask4);
            taskService.addGroupIdentityLink(adhocTask.getId(), "testGroup", "customType");
            taskService.addGroupIdentityLink(adhocTask3.getId(), "testGroup", "customType");
            taskService.addGroupIdentityLink(adhocTask4.getId(), "testGroup", "customType");

            assertThat(taskService.createTaskQuery().
                    or().
                    taskName("testName").
                    taskInvolvedGroups(Collections.singleton("testGroup")).
                    endOr().
                    count()).isEqualTo(4);
            assertThat(taskService.createTaskQuery().
                    taskName("testName").
                    taskInvolvedGroups(Collections.singleton("testGroup")).
                    count()).isEqualTo(1);
            if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
                assertThat(historyService.createHistoricTaskInstanceQuery().
                        or().
                        taskName("testName").
                        taskInvolvedGroups(Collections.singleton("testGroup")).
                        endOr().
                        count()).isEqualTo(4);
                assertThat(historyService.createHistoricTaskInstanceQuery().
                        taskName("testName").
                        taskInvolvedGroups(Collections.singleton("testGroup")).
                        count()).isEqualTo(1);
            }

        } finally {
            deleteAllTasks();
        }
    }

    @Test
    public void testQueryByCandidateGroupsTaskName() {
        try {
            org.flowable.task.api.Task adhocTask = taskService.newTask();
            taskService.saveTask(adhocTask);
            org.flowable.task.api.Task adhocTask2 = taskService.newTask();
            adhocTask2.setName("testName");
            taskService.saveTask(adhocTask2);
            org.flowable.task.api.Task adhocTask3 = taskService.newTask();
            taskService.saveTask(adhocTask3);
            org.flowable.task.api.Task adhocTask4 = taskService.newTask();
            adhocTask4.setName("testName");
            taskService.saveTask(adhocTask4);
            taskService.addGroupIdentityLink(adhocTask.getId(), "testGroup", IdentityLinkType.CANDIDATE);
            taskService.addGroupIdentityLink(adhocTask3.getId(), "testGroup", IdentityLinkType.CANDIDATE);
            taskService.addGroupIdentityLink(adhocTask4.getId(), "testGroup", IdentityLinkType.CANDIDATE);

            assertThat(taskService.createTaskQuery().
                    or().
                    taskName("testName").
                    taskCandidateGroupIn(Collections.singletonList("testGroup")).
                    endOr().
                    count()).isEqualTo(4);
            assertThat(taskService.createTaskQuery().
                    taskName("testName").
                    taskCandidateGroupIn(Collections.singletonList("testGroup")).
                    count()).isEqualTo(1);

            if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
                assertThat(historyService.createHistoricTaskInstanceQuery().
                        or().
                        taskName("testName").
                        taskCandidateGroupIn(Collections.singletonList("testGroup")).
                        endOr().
                        count()).isEqualTo(4);
                assertThat(historyService.createHistoricTaskInstanceQuery().
                        taskName("testName").
                        taskCandidateGroupIn(Collections.singletonList("testGroup")).
                        count()).isEqualTo(1);
            }

        } finally {
            deleteAllTasks();
        }
    }

    @Test
    public void testQueryByCandidateUserTaskName() {
        try {
            org.flowable.task.api.Task adhocTask = taskService.newTask();
            taskService.saveTask(adhocTask);
            org.flowable.task.api.Task adhocTask2 = taskService.newTask();
            adhocTask2.setName("testName");
            taskService.saveTask(adhocTask2);
            org.flowable.task.api.Task adhocTask3 = taskService.newTask();
            taskService.saveTask(adhocTask3);
            org.flowable.task.api.Task adhocTask4 = taskService.newTask();
            adhocTask4.setName("testName");
            taskService.saveTask(adhocTask4);
            taskService.addUserIdentityLink(adhocTask.getId(), "homer", IdentityLinkType.CANDIDATE);
            taskService.addUserIdentityLink(adhocTask3.getId(), "homer", IdentityLinkType.CANDIDATE);
            taskService.addUserIdentityLink(adhocTask4.getId(), "homer", IdentityLinkType.CANDIDATE);

            assertThat(taskService.createTaskQuery().
                    or().
                    taskName("testName").
                    taskCandidateUser("homer").
                    endOr().
                    count()).isEqualTo(4);
            assertThat(taskService.createTaskQuery().
                    taskName("testName").
                    taskCandidateUser("homer").
                    count()).isEqualTo(1);

            if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
                assertThat(historyService.createHistoricTaskInstanceQuery().
                        or().
                        taskName("testName").
                        taskCandidateUser("homer").
                        endOr().
                        count()).isEqualTo(4);
                assertThat(historyService.createHistoricTaskInstanceQuery().
                        taskName("testName").
                        taskCandidateUser("homer").
                        count()).isEqualTo(1);
            }

        } finally {
            deleteAllTasks();
        }
    }

    @Test
    public void testQueryByCandidateGroupTaskName() {
        try {
            org.flowable.task.api.Task adhocTask = taskService.newTask();
            taskService.saveTask(adhocTask);
            org.flowable.task.api.Task adhocTask2 = taskService.newTask();
            adhocTask2.setName("testName");
            taskService.saveTask(adhocTask2);
            org.flowable.task.api.Task adhocTask3 = taskService.newTask();
            taskService.saveTask(adhocTask3);
            org.flowable.task.api.Task adhocTask4 = taskService.newTask();
            adhocTask4.setName("testName");
            taskService.saveTask(adhocTask4);
            taskService.addGroupIdentityLink(adhocTask.getId(), "testGroup", IdentityLinkType.CANDIDATE);
            taskService.addGroupIdentityLink(adhocTask3.getId(), "testGroup", IdentityLinkType.CANDIDATE);
            taskService.addGroupIdentityLink(adhocTask4.getId(), "testGroup", IdentityLinkType.CANDIDATE);

            assertThat(taskService.createTaskQuery().
                    or().
                    taskName("testName").
                    taskCandidateGroup("testGroup").
                    endOr().
                    count()).isEqualTo(4);
            assertThat(taskService.createTaskQuery().
                    taskName("testName").
                    taskCandidateGroup("testGroup").
                    count()).isEqualTo(1);

            if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
                assertThat(historyService.createHistoricTaskInstanceQuery().
                        or().
                        taskName("testName").
                        taskCandidateGroup("testGroup").
                        endOr().
                        count()).isEqualTo(4);
                assertThat(historyService.createHistoricTaskInstanceQuery().
                        taskName("testName").
                        taskCandidateGroup("testGroup").
                        count()).isEqualTo(1);
            }

        } finally {
            deleteAllTasks();
        }
    }

    @Test
    public void testQueryByNullAssignee() {
        assertThatThrownBy(() -> taskService.createTaskQuery().taskAssignee(null).list())
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class);
    }

    @Test
    public void testQueryByNullAssigneeOr() {
        assertThatThrownBy(() -> taskService.createTaskQuery().or().taskId("invalid").taskAssignee(null).list())
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class);
    }

    @Test
    public void testQueryByUnassigned() {
        TaskQuery query = taskService.createTaskQuery().taskUnassigned();
        assertThat(query.count()).isEqualTo(11);
        assertThat(query.list()).hasSize(11);
    }

    @Test
    public void testQueryByUnassignedOr() {
        TaskQuery query = taskService.createTaskQuery().or().taskId("invalid").taskUnassigned();
        assertThat(query.count()).isEqualTo(11);
        assertThat(query.list()).hasSize(11);
    }
    
    @Test
    public void testQueryByAssigned() {
        TaskQuery query = taskService.createTaskQuery().taskAssigned();
        assertThat(query.count()).isEqualTo(1);
        assertThat(query.list()).hasSize(1);
    }

    @Test
    public void testQueryByAssignedOr() {
        TaskQuery query = taskService.createTaskQuery().or().taskId("invalid").taskAssigned();
        assertThat(query.count()).isEqualTo(1);
        assertThat(query.list()).hasSize(1);
    }

    @Test
    public void testQueryByCandidateUser() {
        TaskQuery query = taskService.createTaskQuery().taskCandidateUser("kermit");
        assertThat(query.count()).isEqualTo(11);
        assertThat(query.list()).hasSize(11);
        TaskQuery finalQuery = query;
        assertThatThrownBy(() -> finalQuery.singleResult())
                .isExactlyInstanceOf(FlowableException.class);

        query = taskService.createTaskQuery().taskCandidateUser("fozzie");
        assertThat(query.count()).isEqualTo(3);
        assertThat(query.list()).hasSize(3);
        TaskQuery finalQuery2 = query;
        assertThatThrownBy(() -> finalQuery2.singleResult())
                .isExactlyInstanceOf(FlowableException.class);
    }

    @Test
    public void testQueryByCandidateUserOr() {
        TaskQuery query = taskService.createTaskQuery().or().taskId("invalid").taskCandidateUser("kermit");
        assertThat(query.count()).isEqualTo(11);
        assertThat(query.list()).hasSize(11);
        TaskQuery finalQuery = query;
        assertThatThrownBy(() -> finalQuery.singleResult())
                .isExactlyInstanceOf(FlowableException.class);

        query = taskService.createTaskQuery().or().taskId("invalid").taskCandidateUser("fozzie");
        assertThat(query.count()).isEqualTo(3);
        assertThat(query.list()).hasSize(3);
        TaskQuery finalQuery2 = query;
        assertThatThrownBy(() -> finalQuery2.singleResult())
                .isExactlyInstanceOf(FlowableException.class);
    }

    @Test
    public void testQueryByNullCandidateUser() {
        assertThatThrownBy(() -> taskService.createTaskQuery().taskCandidateUser(null).list())
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class);
    }

    @Test
    public void testQueryByNullCandidateUserOr() {
        assertThatThrownBy(() -> taskService.createTaskQuery().or().taskId("invalid").taskCandidateUser(null).list())
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class);
    }

    @Test
    public void testQueryByCandidateGroup() {
        TaskQuery query = taskService.createTaskQuery().taskCandidateGroup("management");
        assertThat(query.count()).isEqualTo(3);
        assertThat(query.list()).hasSize(3);
        TaskQuery finalQuery = query;
        assertThatThrownBy(() -> finalQuery.singleResult())
                .isExactlyInstanceOf(FlowableException.class);

        query = taskService.createTaskQuery().taskCandidateGroup("sales");
        assertThat(query.count()).isZero();
        assertThat(query.list()).isEmpty();
    }

    @Test
    public void testQueryByCandidateGroupOr() {
        TaskQuery query = taskService.createTaskQuery().or().taskId("invalid").taskCandidateGroup("management");
        assertThat(query.count()).isEqualTo(3);
        assertThat(query.list()).hasSize(3);
        TaskQuery finalQuery = query;
        assertThatThrownBy(() -> finalQuery.singleResult())
                .isExactlyInstanceOf(FlowableException.class);

        query = taskService.createTaskQuery().or().taskId("invalid").taskCandidateGroup("sales");
        assertThat(query.count()).isZero();
        assertThat(query.list()).isEmpty();
    }

    @Test
    public void testQueryByCandidateOrAssigned() {
        TaskQuery query = taskService.createTaskQuery().taskCandidateOrAssigned("kermit");
        assertThat(query.count()).isEqualTo(11);
        List<org.flowable.task.api.Task> tasks = query.list();
        assertThat(tasks).hasSize(11);

        // if dbIdentityUsed set false in process engine configuration of using
        // custom session factory of GroupIdentityManager
        ArrayList<String> candidateGroups = new ArrayList<>();
        candidateGroups.add("management");
        candidateGroups.add("accountancy");
        candidateGroups.add("noexist");
        query = taskService.createTaskQuery().taskCandidateGroupIn(candidateGroups).taskCandidateOrAssigned("kermit");
        assertThat(query.count()).isEqualTo(11);
        tasks = query.list();
        assertThat(tasks).hasSize(11);

        query = taskService.createTaskQuery().taskCandidateOrAssigned("fozzie");
        assertThat(query.count()).isEqualTo(3);
        assertThat(query.list()).hasSize(3);

        // create a new task that no identity link and assignee to kermit
        org.flowable.task.api.Task task = taskService.newTask();
        task.setName("assigneeToKermit");
        task.setDescription("testTask description");
        task.setPriority(3);
        task.setAssignee("kermit");
        taskService.saveTask(task);

        query = taskService.createTaskQuery().taskCandidateOrAssigned("kermit");
        assertThat(query.count()).isEqualTo(12);
        tasks = query.list();
        assertThat(tasks).hasSize(12);

        query = taskService.createTaskQuery().taskCandidateOrAssigned("invalid");
        assertThat(query.count()).isEqualTo(0);
        tasks = query.list();
        assertThat(tasks).isEmpty();

        // SQL Server has a limit of 2100 on how many parameters a query might have
        int maxGroups = AbstractEngineConfiguration.DATABASE_TYPE_MSSQL.equals(processEngineConfiguration.getDatabaseType()) ? 2050 : 2100;

        List<String> testCandidateGroups = new ArrayList<>(maxGroups);
        for (int i = 0; i < maxGroups; i++) {
            testCandidateGroups.add("group" + i);
        }
        
        query = taskService.createTaskQuery().taskCandidateGroupIn(testCandidateGroups).taskCandidateOrAssigned("kermit");
        assertThat(query.count()).isEqualTo(7);
        assertThat(query.list()).hasSize(7);
        
        testCandidateGroups.add("management");
        query = taskService.createTaskQuery().taskCandidateGroupIn(testCandidateGroups).taskCandidateOrAssigned("kermit");
        assertThat(query.count()).isEqualTo(10);
        assertThat(query.list()).hasSize(10);

        org.flowable.task.api.Task assigneeToKermit = taskService.createTaskQuery().taskName("assigneeToKermit").singleResult();
        taskService.deleteTask(assigneeToKermit.getId(), true);
    }

    @Test
    public void testQueryByCandidateOrAssignedOr() {
        TaskQuery query = taskService.createTaskQuery().or().taskId("invalid").taskCandidateOrAssigned("kermit");
        assertThat(query.count()).isEqualTo(11);
        List<org.flowable.task.api.Task> tasks = query.list();
        assertThat(tasks).hasSize(11);

        // if dbIdentityUsed set false in process engine configuration of using
        // custom session factory of GroupIdentityManager
        ArrayList<String> candidateGroups = new ArrayList<>();
        candidateGroups.add("management");
        candidateGroups.add("accountancy");
        candidateGroups.add("noexist");
        query = taskService.createTaskQuery().or().taskId("invalid").taskCandidateGroupIn(candidateGroups).taskCandidateOrAssigned("kermit");
        assertThat(query.count()).isEqualTo(11);
        tasks = query.list();
        assertThat(tasks).hasSize(11);

        query = taskService.createTaskQuery().or().taskId("invalid").taskCandidateOrAssigned("fozzie");
        assertThat(query.count()).isEqualTo(3);
        assertThat(query.list()).hasSize(3);

        // create a new task that no identity link and assignee to kermit
        org.flowable.task.api.Task task = taskService.newTask();
        task.setName("assigneeToKermit");
        task.setDescription("testTask description");
        task.setPriority(3);
        task.setAssignee("kermit");
        taskService.saveTask(task);

        query = taskService.createTaskQuery().or().taskId("invalid").taskCandidateOrAssigned("kermit");
        assertThat(query.count()).isEqualTo(12);
        tasks = query.list();
        assertThat(tasks).hasSize(12);

        query = taskService.createTaskQuery().or().taskId("invalid").taskCandidateOrAssigned("invalid");
        assertThat(query.count()).isEqualTo(0);
        tasks = query.list();
        assertThat(tasks).isEmpty();

        // SQL Server has a limit of 2100 on how many parameters a query might have
        int maxGroups = AbstractEngineConfiguration.DATABASE_TYPE_MSSQL.equals(processEngineConfiguration.getDatabaseType()) ? 2050 : 2100;

        List<String> testCandidateGroups = new ArrayList<>(maxGroups);
        for (int i = 0; i < maxGroups; i++) {
            testCandidateGroups.add("group" + i);
        }
        
        query = taskService.createTaskQuery().or().taskName("testTask").taskCandidateGroupIn(testCandidateGroups).taskCandidateOrAssigned("kermit").endOr();
        assertThat(query.count()).isEqualTo(7);
        assertThat(query.list()).hasSize(7);
        
        testCandidateGroups.add("management");
        query = taskService.createTaskQuery().or().taskName("unexisting").taskCandidateGroupIn(testCandidateGroups).taskCandidateOrAssigned("kermit").endOr();
        assertThat(query.count()).isEqualTo(10);
        assertThat(query.list()).hasSize(10);

        org.flowable.task.api.Task assigneeToKermit = taskService.createTaskQuery().or().taskId("invalid").taskName("assigneeToKermit").singleResult();
        taskService.deleteTask(assigneeToKermit.getId(), true);
    }
    
    @Test
    public void testQueryIgnoreAssigneeValue() {
        List<String> createdTasks = new ArrayList<>();
        Task kermitAssigneeTask = taskService.newTask();
        kermitAssigneeTask.setName("new kermit assignee task");
        taskService.saveTask(kermitAssigneeTask);
        taskService.setAssignee(kermitAssigneeTask.getId(), "kermit");
        createdTasks.add(kermitAssigneeTask.getId());

        Task magementTask = taskService.newTask();
        magementTask.setName("new management task");
        taskService.saveTask(magementTask);
        taskService.setAssignee(magementTask.getId(), "gozzie");
        taskService.addCandidateGroup(magementTask.getId(), "management");
        createdTasks.add(magementTask.getId());


        List<Task> kermitCandidateTasks = taskService.createTaskQuery()
                .taskCandidateUser("kermit")
                .taskName("testTask")
                .list();

        for (Task t : kermitCandidateTasks) {
            taskService.setAssignee(t.getId(), "gonzo");
        }

        List<Task> tasks = taskService.createTaskQuery()
                .taskCandidateUser("kermit")
                .list();
        assertThat(tasks).hasSize(5);

        tasks = taskService.createTaskQuery()
                .taskCandidateUser("kermit")
                .ignoreAssigneeValue()
                .list();
        assertThat(tasks).hasSize(12);

        tasks = taskService.createTaskQuery()
                .taskCandidateOrAssigned("kermit")
                .list();
        assertThat(tasks).hasSize(6);

        tasks = taskService.createTaskQuery()
                .taskCandidateOrAssigned("kermit")
                .ignoreAssigneeValue()
                .list();
        assertThat(tasks).hasSize(13);

        tasks = taskService.createTaskQuery()
                .taskCandidateOrAssigned("gonzo")
                .taskCandidateGroup("management")
                .list();
        assertThat(tasks).hasSize(10);

        tasks = taskService.createTaskQuery()
                .taskCandidateOrAssigned("gonzo")
                .taskCandidateGroup("management")
                .ignoreAssigneeValue()
                .list();
        assertThat(tasks).hasSize(11);

        taskService.deleteTasks(createdTasks, true);
    }

    @Test
    public void testQueryIgnoreAssigneeValueOr() {
        List<String> createdTasks = new ArrayList<>();
        Task kermitAssigneeTask = taskService.newTask();
        kermitAssigneeTask.setName("new kermit assignee task");
        taskService.saveTask(kermitAssigneeTask);
        taskService.setAssignee(kermitAssigneeTask.getId(), "kermit");
        createdTasks.add(kermitAssigneeTask.getId());

        Task magementTask = taskService.newTask();
        magementTask.setName("new management task");
        taskService.saveTask(magementTask);
        taskService.setAssignee(magementTask.getId(), "gozzie");
        taskService.addCandidateGroup(magementTask.getId(), "management");
        createdTasks.add(magementTask.getId());


        List<Task> kermitCandidateTasks = taskService.createTaskQuery()
                .taskCandidateUser("kermit")
                .taskName("testTask")
                .list();

        for (Task t : kermitCandidateTasks) {
            taskService.setAssignee(t.getId(), "gonzo");
        }

        List<Task> tasks = taskService.createTaskQuery()
                .or()
                .taskCandidateUser("kermit")
                .taskCandidateGroup("management")
                .endOr()
                .list();
        assertThat(tasks).hasSize(3);

        tasks = taskService.createTaskQuery()
                .or()
                .taskCandidateUser("kermit")
                .taskCandidateGroup("management")
                .ignoreAssigneeValue()
                .endOr()
                .list();
        assertThat(tasks).hasSize(10);

        tasks = taskService.createTaskQuery()
                .or()
                .taskCandidateOrAssigned("kermit")
                .endOr()
                .list();
        assertThat(tasks).hasSize(6);

        tasks = taskService.createTaskQuery()
                .or()
                .taskCandidateOrAssigned("kermit")
                .ignoreAssigneeValue()
                .endOr()
                .list();
        assertThat(tasks).hasSize(13);

        tasks = taskService.createTaskQuery()
                .or()
                .taskCandidateOrAssigned("gonzo")
                .taskCandidateGroup("management")
                .endOr()
                .list();
        assertThat(tasks).hasSize(10);

        tasks = taskService.createTaskQuery()
                .or()
                .taskCandidateOrAssigned("gonzo")
                .taskCandidateGroup("management")
                .ignoreAssigneeValue()
                .endOr()
                .list();
        assertThat(tasks).hasSize(11);

        taskService.deleteTasks(createdTasks, true);
    }

    @Test
    public void testQueryByNullCandidateGroup() {
        assertThatThrownBy(() -> taskService.createTaskQuery().taskCandidateGroup(null).list())
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class);
    }

    @Test
    public void testQueryByNullCandidateGroupOr() {
        assertThatThrownBy(() -> taskService.createTaskQuery().or().taskId("invalid").taskCandidateGroup(null).list())
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class);
    }

    @Test
    public void testQueryByCandidateGroupIn() {
        List<String> groups = Arrays.asList("management", "accountancy");
        TaskQuery query = taskService.createTaskQuery().taskCandidateGroupIn(groups);
        assertThat(query.count()).isEqualTo(5);
        assertThat(query.list()).hasSize(5);

        TaskQuery finalQuery = query;
        assertThatThrownBy(() -> finalQuery.singleResult())
                .isExactlyInstanceOf(FlowableException.class);

        query = taskService.createTaskQuery().taskCandidateUser("kermit").taskCandidateGroupIn(groups);
        assertThat(query.count()).isEqualTo(11);
        assertThat(query.list()).hasSize(11);

        query = taskService.createTaskQuery().taskCandidateUser("kermit").taskCandidateGroup("unexisting");
        assertThat(query.count()).isEqualTo(6);
        assertThat(query.list()).hasSize(6);

        query = taskService.createTaskQuery().taskCandidateUser("unexisting").taskCandidateGroup("unexisting");
        assertThat(query.count()).isZero();
        assertThat(query.list()).isEmpty();

        // Unexisting groups or groups that don't have candidate tasks shouldn't influence other results
        groups = Arrays.asList("management", "accountancy", "sales", "unexising");
        query = taskService.createTaskQuery().taskCandidateGroupIn(groups);
        assertThat(query.count()).isEqualTo(5);
        assertThat(query.list()).hasSize(5);

        // SQL Server has a limit of 2100 on how many parameters a query might have
        int maxGroups = AbstractEngineConfiguration.DATABASE_TYPE_MSSQL.equals(processEngineConfiguration.getDatabaseType()) ? 2050 : 2100;

        List<String> testCandidateGroups = new ArrayList<>(maxGroups);
        for (int i = 0; i < maxGroups; i++) {
            testCandidateGroups.add("group" + i);
        }
        
        query = taskService.createTaskQuery().taskCandidateGroupIn(testCandidateGroups);
        assertThat(query.count()).isEqualTo(0);
        assertThat(query.list()).hasSize(0);
        
        testCandidateGroups.add("management");
        query = taskService.createTaskQuery().taskCandidateGroupIn(testCandidateGroups);
        assertThat(query.count()).isEqualTo(3);
        assertThat(query.list()).hasSize(3);
    }

    @Test
    public void testQueryByCandidateGroupInOr() {
        List<String> groups = Arrays.asList("management", "accountancy");
        TaskQuery query = taskService.createTaskQuery().or().taskId("invalid").taskCandidateGroupIn(groups);
        assertThat(query.count()).isEqualTo(5);
        assertThat(query.list()).hasSize(5);

        TaskQuery finalQuery = query;
        assertThatThrownBy(() -> finalQuery.singleResult())
                .isExactlyInstanceOf(FlowableException.class);

        query = taskService.createTaskQuery().or().taskCandidateUser("kermit").taskCandidateGroupIn(groups).endOr();
        assertThat(query.count()).isEqualTo(11);
        assertThat(query.list()).hasSize(11);

        query = taskService.createTaskQuery().or().taskCandidateUser("kermit").taskCandidateGroup("unexisting").endOr();
        assertThat(query.count()).isEqualTo(6);
        assertThat(query.list()).hasSize(6);

        query = taskService.createTaskQuery().or().taskCandidateUser("unexisting").taskCandidateGroup("unexisting").endOr();
        assertThat(query.count()).isZero();
        assertThat(query.list()).isEmpty();

        query = taskService.createTaskQuery().or().taskCandidateUser("kermit").taskCandidateGroupIn(groups).endOr()
                .or().taskCandidateUser("gonzo").taskCandidateGroupIn(groups);
        assertThat(query.count()).isEqualTo(5);
        assertThat(query.list()).hasSize(5);

        // Unexisting groups or groups that don't have candidate tasks shouldn't influence other results
        groups = Arrays.asList("management", "accountancy", "sales", "unexising");
        query = taskService.createTaskQuery().or().taskId("invalid").taskCandidateGroupIn(groups);
        assertThat(query.count()).isEqualTo(5);
        assertThat(query.list()).hasSize(5);

        // SQL Server has a limit of 2100 on how many parameters a query might have
        int maxGroups = AbstractEngineConfiguration.DATABASE_TYPE_MSSQL.equals(processEngineConfiguration.getDatabaseType()) ? 2050 : 2100;

        List<String> testCandidateGroups = new ArrayList<>(maxGroups);
        for (int i = 0; i < maxGroups; i++) {
            testCandidateGroups.add("group" + i);
        }
        
        query = taskService.createTaskQuery().or().taskName("testTask").taskCandidateGroupIn(testCandidateGroups).endOr();
        assertThat(query.count()).isEqualTo(6);
        assertThat(query.list()).hasSize(6);
        
        testCandidateGroups.add("management");
        query = taskService.createTaskQuery().or().taskName("undefined").taskCandidateGroupIn(testCandidateGroups);
        assertThat(query.count()).isEqualTo(3);
        assertThat(query.list()).hasSize(3);
    }

    @Test
    public void testQueryByNullCandidateGroupIn() {
        assertThatThrownBy(() -> taskService.createTaskQuery().taskCandidateGroupIn(null).list())
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class);
        assertThatThrownBy(() -> taskService.createTaskQuery().taskCandidateGroupIn(new ArrayList<>()).list())
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class);
    }

    @Test
    public void testQueryByNullCandidateGroupInOr() {
        assertThatThrownBy(() -> taskService.createTaskQuery().or().taskId("invalid").taskCandidateGroupIn(null).list())
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class);
        assertThatThrownBy(() -> taskService.createTaskQuery().or().taskId("invalid").taskCandidateGroupIn(new ArrayList<>()).list())
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class);
    }

    @Test
    public void testQueryByDelegationState() {
        TaskQuery query = taskService.createTaskQuery().taskDelegationState(null);
        assertThat(query.count()).isEqualTo(12);
        assertThat(query.list()).hasSize(12);
        query = taskService.createTaskQuery().taskDelegationState(DelegationState.PENDING);
        assertThat(query.count()).isZero();
        assertThat(query.list()).isEmpty();
        query = taskService.createTaskQuery().taskDelegationState(DelegationState.RESOLVED);
        assertThat(query.count()).isZero();
        assertThat(query.list()).isEmpty();

        String taskId = taskService.createTaskQuery().taskAssignee("gonzo").singleResult().getId();
        taskService.delegateTask(taskId, "kermit");

        query = taskService.createTaskQuery().taskDelegationState(null);
        assertThat(query.count()).isEqualTo(11);
        assertThat(query.list()).hasSize(11);
        query = taskService.createTaskQuery().taskDelegationState(DelegationState.PENDING);
        assertThat(query.count()).isEqualTo(1);
        assertThat(query.list()).hasSize(1);
        query = taskService.createTaskQuery().taskDelegationState(DelegationState.RESOLVED);
        assertThat(query.count()).isZero();
        assertThat(query.list()).isEmpty();

        taskService.resolveTask(taskId);

        query = taskService.createTaskQuery().taskDelegationState(null);
        assertThat(query.count()).isEqualTo(11);
        assertThat(query.list()).hasSize(11);
        query = taskService.createTaskQuery().taskDelegationState(DelegationState.PENDING);
        assertThat(query.count()).isZero();
        assertThat(query.list()).isEmpty();
        query = taskService.createTaskQuery().taskDelegationState(DelegationState.RESOLVED);
        assertThat(query.count()).isEqualTo(1);
        assertThat(query.list()).hasSize(1);
    }

    @Test
    public void testQueryByDelegationStateOr() {
        TaskQuery query = taskService.createTaskQuery().or().taskId("invalid").taskDelegationState(null);
        assertThat(query.count()).isEqualTo(12);
        assertThat(query.list()).hasSize(12);
        query = taskService.createTaskQuery().or().taskId("invalid").taskDelegationState(DelegationState.PENDING);
        assertThat(query.count()).isZero();
        assertThat(query.list()).isEmpty();
        query = taskService.createTaskQuery().or().taskId("invalid").taskDelegationState(DelegationState.RESOLVED);
        assertThat(query.count()).isZero();
        assertThat(query.list()).isEmpty();

        String taskId = taskService.createTaskQuery().or().taskId("invalid").taskAssignee("gonzo").singleResult().getId();
        taskService.delegateTask(taskId, "kermit");

        query = taskService.createTaskQuery().or().taskId("invalid").taskDelegationState(null);
        assertThat(query.count()).isEqualTo(11);
        assertThat(query.list()).hasSize(11);
        query = taskService.createTaskQuery().or().taskId("invalid").taskDelegationState(DelegationState.PENDING);
        assertThat(query.count()).isEqualTo(1);
        assertThat(query.list()).hasSize(1);
        query = taskService.createTaskQuery().or().taskId("invalid").taskDelegationState(DelegationState.RESOLVED);
        assertThat(query.count()).isZero();
        assertThat(query.list()).isEmpty();

        taskService.resolveTask(taskId);

        query = taskService.createTaskQuery().or().taskId("invalid").taskDelegationState(null);
        assertThat(query.count()).isEqualTo(11);
        assertThat(query.list()).hasSize(11);
        query = taskService.createTaskQuery().or().taskId("invalid").taskDelegationState(DelegationState.PENDING);
        assertThat(query.count()).isZero();
        assertThat(query.list()).isEmpty();
        query = taskService.createTaskQuery().or().taskId("invalid").taskDelegationState(DelegationState.RESOLVED);
        assertThat(query.count()).isEqualTo(1);
        assertThat(query.list()).hasSize(1);
    }

    @Test
    public void testQueryCreatedOn() throws Exception {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss.SSS");

        // Exact matching of createTime, should result in 6 tasks
        Date createTime = sdf.parse("01/01/2001 01:01:01.000");

        TaskQuery query = taskService.createTaskQuery().taskCreatedOn(createTime);
        assertThat(query.count()).isEqualTo(6);
        assertThat(query.list()).hasSize(6);
    }

    @Test
    public void testQueryCreatedOnOr() throws Exception {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss.SSS");

        // Exact matching of createTime, should result in 6 tasks
        Date createTime = sdf.parse("01/01/2001 01:01:01.000");

        TaskQuery query = taskService.createTaskQuery().or().taskId("invalid").taskCreatedOn(createTime);
        assertThat(query.count()).isEqualTo(6);
        assertThat(query.list()).hasSize(6);
    }

    @Test
    public void testQueryCreatedBefore() throws Exception {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss.SSS");

        // Should result in 7 tasks
        Date before = sdf.parse("03/02/2002 02:02:02.000");

        TaskQuery query = taskService.createTaskQuery().taskCreatedBefore(before);
        assertThat(query.count()).isEqualTo(7);
        assertThat(query.list()).hasSize(7);

        before = sdf.parse("01/01/2001 01:01:01.000");
        query = taskService.createTaskQuery().taskCreatedBefore(before);
        assertThat(query.count()).isZero();
        assertThat(query.list()).isEmpty();
    }

    @Test
    public void testQueryCreatedBeforeOr() throws Exception {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss.SSS");

        // Should result in 7 tasks
        Date before = sdf.parse("03/02/2002 02:02:02.000");

        TaskQuery query = taskService.createTaskQuery().or().taskId("invalid").taskCreatedBefore(before);
        assertThat(query.count()).isEqualTo(7);
        assertThat(query.list()).hasSize(7);

        before = sdf.parse("01/01/2001 01:01:01.000");
        query = taskService.createTaskQuery().or().taskId("invalid").taskCreatedBefore(before);
        assertThat(query.count()).isZero();
        assertThat(query.list()).isEmpty();
    }

    @Test
    public void testQueryCreatedAfter() throws Exception {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss.SSS");

        // Should result in 3 tasks
        Date after = sdf.parse("03/03/2003 03:03:03.000");

        TaskQuery query = taskService.createTaskQuery().taskCreatedAfter(after);
        assertThat(query.count()).isEqualTo(3);
        assertThat(query.list()).hasSize(3);

        after = sdf.parse("05/05/2005 05:05:05.000");
        query = taskService.createTaskQuery().taskCreatedAfter(after);
        assertThat(query.count()).isZero();
        assertThat(query.list()).isEmpty();
    }

    @Test
    public void testQueryCreatedAfterOr() throws Exception {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss.SSS");

        // Should result in 3 tasks
        Date after = sdf.parse("03/03/2003 03:03:03.000");

        TaskQuery query = taskService.createTaskQuery().or().taskId("invalid").taskCreatedAfter(after);
        assertThat(query.count()).isEqualTo(3);
        assertThat(query.list()).hasSize(3);

        after = sdf.parse("05/05/2005 05:05:05.000");
        query = taskService.createTaskQuery().or().taskId("invalid").taskCreatedAfter(after);
        assertThat(query.count()).isZero();
        assertThat(query.list()).isEmpty();
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/api/task/taskDefinitionProcess.bpmn20.xml")
    public void testTaskDefinitionKey() throws Exception {

        // Start process instance, 2 tasks will be available
        runtimeService.startProcessInstanceByKey("taskDefinitionKeyProcess");

        // 1 task should exist with key "taskKey1"
        List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().taskDefinitionKey("taskKey1").list();
        assertThat(tasks).isNotNull();
        assertThat(tasks)
                .extracting(Task::getTaskDefinitionKey)
                .containsExactly("taskKey1");

        // No task should be found with unexisting key
        long count = taskService.createTaskQuery().taskDefinitionKey("unexistingKey").count();
        assertThat(count).isZero();
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/api/task/taskDefinitionProcess.bpmn20.xml")
    public void testTaskDefinitionKeyOr() throws Exception {

        // Start process instance, 2 tasks will be available
        runtimeService.startProcessInstanceByKey("taskDefinitionKeyProcess");

        // 1 task should exist with key "taskKey1"
        List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().or().taskId("invalid").taskDefinitionKey("taskKey1").list();
        assertThat(tasks).isNotNull();
        assertThat(tasks)
                .extracting(Task::getTaskDefinitionKey)
                .containsExactly("taskKey1");

        // No task should be found with unexisting key
        long count = taskService.createTaskQuery().or().taskId("invalid").taskDefinitionKey("unexistingKey").count();
        assertThat(count).isZero();
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/api/task/taskDefinitionProcess.bpmn20.xml")
    public void testTaskDefinitionKeys() throws Exception {

        // Start process instance, 2 tasks will be available
        runtimeService.startProcessInstanceByKey("taskDefinitionKeyProcess");

        List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().taskDefinitionKeys(Arrays.asList("taskKey1", "taskKey123", "invalid")).list();
        assertThat(tasks)
            .extracting(TaskInfo::getTaskDefinitionKey, TaskInfo::getName)
            .containsExactlyInAnyOrder(
                tuple("taskKey1", "Task A"),
                tuple("taskKey123", "Task B")
            );

        assertThat(taskService.createTaskQuery().taskDefinitionKeys(Arrays.asList("taskKey1", "taskKey123", "invalid")).count()).isEqualTo(2);

        assertThat(taskService.createTaskQuery().taskDefinitionKeys(Arrays.asList("invalid1", "invalid2")).count()).isZero();
        assertThat(taskService.createTaskQuery().taskDefinitionKeys(Arrays.asList("invalid1", "invalid2")).list()).isEmpty();
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/api/task/taskDefinitionProcess.bpmn20.xml")
    public void testTaskDefinitionKeysOr() throws Exception {

        // Start process instance, 2 tasks will be available
        runtimeService.startProcessInstanceByKey("taskDefinitionKeyProcess");

        List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().or().taskId("invalid")
            .taskDefinitionKeys(Arrays.asList("taskKey1", "taskKey123", "invalid")).list();
        assertThat(tasks)
            .extracting(TaskInfo::getTaskDefinitionKey, TaskInfo::getName)
            .containsExactlyInAnyOrder(
                tuple("taskKey1", "Task A"),
                tuple("taskKey123", "Task B")
            );

        assertThat(taskService.createTaskQuery().or().taskId("invalid").taskDefinitionKeys(Arrays.asList("taskKey1", "taskKey123", "invalid")).endOr().count())
                .isEqualTo(2);

        assertThat(taskService.createTaskQuery().or().taskId("invalid").taskDefinitionKeys(Arrays.asList("invalid1", "invalid2")).endOr().count())
                .isZero();

        assertThat(taskService.createTaskQuery().or().taskId("invalid").taskDefinitionKeys(Arrays.asList("invalid1", "invalid2")).endOr().list())
            .isEmpty();
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/api/task/taskDefinitionProcess.bpmn20.xml")
    public void testTaskDefinitionKeyLike() throws Exception {

        // Start process instance, 2 tasks will be available
        runtimeService.startProcessInstanceByKey("taskDefinitionKeyProcess");

        // Ends with matching, TaskKey1 and TaskKey123 match
        List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().taskDefinitionKeyLike("taskKey1%").orderByTaskName().asc().list();
        assertThat(tasks).isNotNull();
        assertThat(tasks)
                .extracting(Task::getTaskDefinitionKey)
                .containsExactly("taskKey1", "taskKey123");

        // Starts with matching, TaskKey123 matches
        tasks = taskService.createTaskQuery().taskDefinitionKeyLike("%123").orderByTaskName().asc().list();
        assertThat(tasks).isNotNull();
        assertThat(tasks)
                .extracting(Task::getTaskDefinitionKey)
                .containsExactly("taskKey123");

        // Contains matching, TaskKey123 matches
        tasks = taskService.createTaskQuery().taskDefinitionKeyLike("%Key12%").orderByTaskName().asc().list();
        assertThat(tasks).isNotNull();
        assertThat(tasks)
                .extracting(Task::getTaskDefinitionKey)
                .containsExactly("taskKey123");

        // No task should be found with unexisting key
        long count = taskService.createTaskQuery().taskDefinitionKeyLike("%unexistingKey%").count();
        assertThat(count).isZero();
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/api/task/taskDefinitionProcess.bpmn20.xml")
    public void testTaskDefinitionKeyLikeOr() throws Exception {

        // Start process instance, 2 tasks will be available
        runtimeService.startProcessInstanceByKey("taskDefinitionKeyProcess");

        // Ends with matching, TaskKey1 and TaskKey123 match
        List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().or().taskId("invalid").taskDefinitionKeyLike("taskKey1%").orderByTaskName().asc()
                .list();
        assertThat(tasks).isNotNull();
        assertThat(tasks)
                .extracting(Task::getTaskDefinitionKey)
                .containsExactly("taskKey1", "taskKey123");

        // Starts with matching, TaskKey123 matches
        tasks = taskService.createTaskQuery().or().taskDefinitionKeyLike("%123").taskId("invalid").orderByTaskName().asc().list();
        assertThat(tasks).isNotNull();
        assertThat(tasks)
                .extracting(Task::getTaskDefinitionKey)
                .containsExactly("taskKey123");

        // Contains matching, TaskKey123 matches
        tasks = taskService.createTaskQuery().or().taskDefinitionKeyLike("%Key12%").taskId("invalid").orderByTaskName().asc().list();
        assertThat(tasks).isNotNull();
        assertThat(tasks)
                .extracting(Task::getTaskDefinitionKey)
                .containsExactly("taskKey123");

        // No task should be found with unexisting key
        long count = taskService.createTaskQuery().or().taskId("invalid").taskDefinitionKeyLike("%unexistingKey%").count();
        assertThat(count).isZero();
    }

    @Test
    @Deployment
    public void testTaskVariableValueEquals() throws Exception {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
        org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();

        // No task should be found for an unexisting var
        assertThat(taskService.createTaskQuery().taskVariableValueEquals("unexistingVar", "value").count()).isZero();

        // Create a map with a variable for all default types
        Map<String, Object> variables = new HashMap<>();
        variables.put("longVar", 928374L);
        variables.put("shortVar", (short) 123);
        variables.put("integerVar", 1234);
        variables.put("stringVar", "stringValue");
        variables.put("booleanVar", true);
        Date date = Calendar.getInstance().getTime();
        variables.put("dateVar", date);
        variables.put("nullVar", null);

        taskService.setVariablesLocal(task.getId(), variables);

        // Test query matches
        assertThat(taskService.createTaskQuery().taskVariableValueEquals("longVar", 928374L).count()).isEqualTo(1);
        assertThat(taskService.createTaskQuery().taskVariableValueEquals("shortVar", (short) 123).count()).isEqualTo(1);
        assertThat(taskService.createTaskQuery().taskVariableValueEquals("integerVar", 1234).count()).isEqualTo(1);
        assertThat(taskService.createTaskQuery().taskVariableValueEquals("stringVar", "stringValue").count()).isEqualTo(1);
        assertThat(taskService.createTaskQuery().taskVariableValueEquals("booleanVar", true).count()).isEqualTo(1);
        assertThat(taskService.createTaskQuery().taskVariableValueEquals("dateVar", date).count()).isEqualTo(1);
        assertThat(taskService.createTaskQuery().taskVariableValueEquals("nullVar", null).count()).isEqualTo(1);

        // Test query for other values on existing variables
        assertThat(taskService.createTaskQuery().taskVariableValueEquals("longVar", 999L).count()).isZero();
        assertThat(taskService.createTaskQuery().taskVariableValueEquals("shortVar", (short) 999).count()).isZero();
        assertThat(taskService.createTaskQuery().taskVariableValueEquals("integerVar", 999).count()).isZero();
        assertThat(taskService.createTaskQuery().taskVariableValueEquals("stringVar", "999").count()).isZero();
        assertThat(taskService.createTaskQuery().taskVariableValueEquals("booleanVar", false).count()).isZero();
        Calendar otherDate = Calendar.getInstance();
        otherDate.add(Calendar.YEAR, 1);
        assertThat(taskService.createTaskQuery().taskVariableValueEquals("dateVar", otherDate.getTime()).count()).isZero();
        assertThat(taskService.createTaskQuery().taskVariableValueEquals("nullVar", "999").count()).isZero();

        // Test query for not equals
        assertThat(taskService.createTaskQuery().taskVariableValueNotEquals("longVar", 999L).count()).isEqualTo(1);
        assertThat(taskService.createTaskQuery().taskVariableValueNotEquals("shortVar", (short) 999).count()).isEqualTo(1);
        assertThat(taskService.createTaskQuery().taskVariableValueNotEquals("integerVar", 999).count()).isEqualTo(1);
        assertThat(taskService.createTaskQuery().taskVariableValueNotEquals("stringVar", "999").count()).isEqualTo(1);
        assertThat(taskService.createTaskQuery().taskVariableValueNotEquals("booleanVar", false).count()).isEqualTo(1);

        // Test value-only variable equals
        assertThat(taskService.createTaskQuery().taskVariableValueEquals(928374L).count()).isEqualTo(1);
        assertThat(taskService.createTaskQuery().taskVariableValueEquals((short) 123).count()).isEqualTo(1);
        assertThat(taskService.createTaskQuery().taskVariableValueEquals(1234).count()).isEqualTo(1);
        assertThat(taskService.createTaskQuery().taskVariableValueEquals("stringValue").count()).isEqualTo(1);
        assertThat(taskService.createTaskQuery().taskVariableValueEquals(true).count()).isEqualTo(1);
        assertThat(taskService.createTaskQuery().taskVariableValueEquals(date).count()).isEqualTo(1);
        assertThat(taskService.createTaskQuery().taskVariableValueEquals(null).count()).isEqualTo(1);

        assertThat(taskService.createTaskQuery().taskVariableValueEquals(999999L).count()).isZero();
        assertThat(taskService.createTaskQuery().taskVariableValueEquals((short) 999).count()).isZero();
        assertThat(taskService.createTaskQuery().taskVariableValueEquals(9999).count()).isZero();
        assertThat(taskService.createTaskQuery().taskVariableValueEquals("unexistingstringvalue").count()).isZero();
        assertThat(taskService.createTaskQuery().taskVariableValueEquals(false).count()).isZero();
        assertThat(taskService.createTaskQuery().taskVariableValueEquals(otherDate.getTime()).count()).isZero();

        assertThat(taskService.createTaskQuery().taskVariableValueLike("stringVar", "string%").count()).isEqualTo(1);
        assertThat(taskService.createTaskQuery().taskVariableValueLike("stringVar", "String%").count()).isZero();
        assertThat(taskService.createTaskQuery().taskVariableValueLike("stringVar", "%Value").count()).isEqualTo(1);

        assertThat(taskService.createTaskQuery().taskVariableValueGreaterThan("integerVar", 1000).count()).isEqualTo(1);
        assertThat(taskService.createTaskQuery().taskVariableValueGreaterThan("integerVar", 1234).count()).isZero();
        assertThat(taskService.createTaskQuery().taskVariableValueGreaterThan("integerVar", 1240).count()).isZero();

        assertThat(taskService.createTaskQuery().taskVariableValueGreaterThanOrEqual("integerVar", 1000).count()).isEqualTo(1);
        assertThat(taskService.createTaskQuery().taskVariableValueGreaterThanOrEqual("integerVar", 1234).count()).isEqualTo(1);
        assertThat(taskService.createTaskQuery().taskVariableValueGreaterThanOrEqual("integerVar", 1240).count()).isZero();

        assertThat(taskService.createTaskQuery().taskVariableValueLessThan("integerVar", 1240).count()).isEqualTo(1);
        assertThat(taskService.createTaskQuery().taskVariableValueLessThan("integerVar", 1234).count()).isZero();
        assertThat(taskService.createTaskQuery().taskVariableValueLessThan("integerVar", 1000).count()).isZero();

        assertThat(taskService.createTaskQuery().taskVariableValueLessThanOrEqual("integerVar", 1240).count()).isEqualTo(1);
        assertThat(taskService.createTaskQuery().taskVariableValueLessThanOrEqual("integerVar", 1234).count()).isEqualTo(1);
        assertThat(taskService.createTaskQuery().taskVariableValueLessThanOrEqual("integerVar", 1000).count()).isZero();
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/task/TaskQueryTest.testTaskVariableValueEquals.bpmn20.xml" })
    public void testTaskVariableValueEqualsOr() throws Exception {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
        org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();

        // No task should be found for an unexisting var
        assertThat(taskService.createTaskQuery().or().taskId("invalid").taskVariableValueEquals("unexistingVar", "value").count()).isZero();

        // Create a map with a variable for all default types
        Map<String, Object> variables = new HashMap<>();
        variables.put("longVar", 928374L);
        variables.put("shortVar", (short) 123);
        variables.put("integerVar", 1234);
        variables.put("stringVar", "stringValue");
        variables.put("booleanVar", true);
        Date date = Calendar.getInstance().getTime();
        variables.put("dateVar", date);
        variables.put("nullVar", null);

        taskService.setVariablesLocal(task.getId(), variables);

        // Test query matches
        assertThat(taskService.createTaskQuery().or().taskId("invalid").taskVariableValueEquals("longVar", 928374L).count()).isEqualTo(1);
        assertThat(taskService.createTaskQuery().or().taskId("invalid").taskVariableValueEquals("shortVar", (short) 123).count()).isEqualTo(1);
        assertThat(taskService.createTaskQuery().or().taskId("invalid").taskVariableValueEquals("integerVar", 1234).count()).isEqualTo(1);
        assertThat(taskService.createTaskQuery().or().taskId("invalid").taskVariableValueEquals("stringVar", "stringValue").count()).isEqualTo(1);
        assertThat(taskService.createTaskQuery().or().taskId("invalid").taskVariableValueEquals("booleanVar", true).count()).isEqualTo(1);
        assertThat(taskService.createTaskQuery().or().taskId("invalid").taskVariableValueEquals("dateVar", date).count()).isEqualTo(1);
        assertThat(taskService.createTaskQuery().or().taskId("invalid").taskVariableValueEquals("nullVar", null).count()).isEqualTo(1);

        // Test query for other values on existing variables
        assertThat(taskService.createTaskQuery().or().taskId("invalid").taskVariableValueEquals("longVar", 999L).count()).isZero();
        assertThat(taskService.createTaskQuery().or().taskId("invalid").taskVariableValueEquals("shortVar", (short) 999).count()).isZero();
        assertThat(taskService.createTaskQuery().or().taskId("invalid").taskVariableValueEquals("integerVar", 999).count()).isZero();
        assertThat(taskService.createTaskQuery().or().taskId("invalid").taskVariableValueEquals("stringVar", "999").count()).isZero();
        assertThat(taskService.createTaskQuery().or().taskId("invalid").taskVariableValueEquals("booleanVar", false).count()).isZero();
        Calendar otherDate = Calendar.getInstance();
        otherDate.add(Calendar.YEAR, 1);
        assertThat(taskService.createTaskQuery().or().taskId("invalid").taskVariableValueEquals("dateVar", otherDate.getTime()).count()).isZero();
        assertThat(taskService.createTaskQuery().or().taskId("invalid").taskVariableValueEquals("nullVar", "999").count()).isZero();

        // Test query for not equals
        assertThat(taskService.createTaskQuery().or().taskId("invalid").taskVariableValueNotEquals("longVar", 999L).count()).isEqualTo(1);
        assertThat(taskService.createTaskQuery().or().taskId("invalid").taskVariableValueNotEquals("shortVar", (short) 999).count()).isEqualTo(1);
        assertThat(taskService.createTaskQuery().or().taskId("invalid").taskVariableValueNotEquals("integerVar", 999).count()).isEqualTo(1);
        assertThat(taskService.createTaskQuery().or().taskId("invalid").taskVariableValueNotEquals("stringVar", "999").count()).isEqualTo(1);
        assertThat(taskService.createTaskQuery().or().taskId("invalid").taskVariableValueNotEquals("booleanVar", false).count()).isEqualTo(1);

        // Test value-only variable equals
        assertThat(taskService.createTaskQuery().or().taskId("invalid").taskVariableValueEquals(928374L).count()).isEqualTo(1);
        assertThat(taskService.createTaskQuery().or().taskId("invalid").taskVariableValueEquals((short) 123).count()).isEqualTo(1);
        assertThat(taskService.createTaskQuery().or().taskId("invalid").taskVariableValueEquals(1234).count()).isEqualTo(1);
        assertThat(taskService.createTaskQuery().or().taskId("invalid").taskVariableValueEquals("stringValue").count()).isEqualTo(1);
        assertThat(taskService.createTaskQuery().or().taskId("invalid").taskVariableValueEquals(true).count()).isEqualTo(1);
        assertThat(taskService.createTaskQuery().or().taskId("invalid").taskVariableValueEquals(date).count()).isEqualTo(1);
        assertThat(taskService.createTaskQuery().or().taskId("invalid").taskVariableValueEquals(null).count()).isEqualTo(1);

        assertThat(taskService.createTaskQuery().or().taskId("invalid").taskVariableValueEquals(999999L).count()).isZero();
        assertThat(taskService.createTaskQuery().or().taskId("invalid").taskVariableValueEquals((short) 999).count()).isZero();
        assertThat(taskService.createTaskQuery().or().taskId("invalid").taskVariableValueEquals(9999).count()).isZero();
        assertThat(taskService.createTaskQuery().or().taskId("invalid").taskVariableValueEquals("unexistingstringvalue").count()).isZero();
        assertThat(taskService.createTaskQuery().or().taskId("invalid").taskVariableValueEquals(false).count()).isZero();
        assertThat(taskService.createTaskQuery().or().taskId("invalid").taskVariableValueEquals(otherDate.getTime()).count()).isZero();

        assertThat(taskService.createTaskQuery().or().taskId("invalid").taskVariableValueLike("stringVar", "string%").count()).isEqualTo(1);
        assertThat(taskService.createTaskQuery().or().taskId("invalid").taskVariableValueLike("stringVar", "String%").count()).isZero();
        assertThat(taskService.createTaskQuery().or().taskId("invalid").taskVariableValueLike("stringVar", "%Value").count()).isEqualTo(1);

        assertThat(taskService.createTaskQuery().or().taskId("invalid").taskVariableValueGreaterThan("integerVar", 1000).count()).isEqualTo(1);
        assertThat(taskService.createTaskQuery().or().taskId("invalid").taskVariableValueGreaterThan("integerVar", 1234).count()).isZero();
        assertThat(taskService.createTaskQuery().or().taskId("invalid").taskVariableValueGreaterThan("integerVar", 1240).count()).isZero();

        assertThat(taskService.createTaskQuery().or().taskId("invalid").taskVariableValueGreaterThanOrEqual("integerVar", 1000).count()).isEqualTo(1);
        assertThat(taskService.createTaskQuery().or().taskId("invalid").taskVariableValueGreaterThanOrEqual("integerVar", 1234).count()).isEqualTo(1);
        assertThat(taskService.createTaskQuery().or().taskId("invalid").taskVariableValueGreaterThanOrEqual("integerVar", 1240).count()).isZero();

        assertThat(taskService.createTaskQuery().or().taskId("invalid").taskVariableValueLessThan("integerVar", 1240).count()).isEqualTo(1);
        assertThat(taskService.createTaskQuery().or().taskId("invalid").taskVariableValueLessThan("integerVar", 1234).count()).isZero();
        assertThat(taskService.createTaskQuery().or().taskId("invalid").taskVariableValueLessThan("integerVar", 1000).count()).isZero();

        assertThat(taskService.createTaskQuery().or().taskId("invalid").taskVariableValueLessThanOrEqual("integerVar", 1240).count()).isEqualTo(1);
        assertThat(taskService.createTaskQuery().or().taskId("invalid").taskVariableValueLessThanOrEqual("integerVar", 1234).count()).isEqualTo(1);
        assertThat(taskService.createTaskQuery().or().taskId("invalid").taskVariableValueLessThanOrEqual("integerVar", 1000).count()).isZero();
    }

    @Test
    @Deployment
    public void testProcessVariableValueEquals() throws Exception {
        Map<String, Object> variables = new HashMap<>();
        variables.put("longVar", 928374L);
        variables.put("shortVar", (short) 123);
        variables.put("integerVar", 1234);
        variables.put("stringVar", "stringValue");
        variables.put("booleanVar", true);
        Date date = Calendar.getInstance().getTime();
        variables.put("dateVar", date);
        variables.put("nullVar", null);

        // Start process-instance with all types of variables
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess", variables);

        // Test query matches
        assertThat(taskService.createTaskQuery().processVariableValueEquals("longVar", 928374L).count()).isEqualTo(1);
        assertThat(taskService.createTaskQuery().processVariableValueEquals("shortVar", (short) 123).count()).isEqualTo(1);
        assertThat(taskService.createTaskQuery().processVariableValueEquals("integerVar", 1234).count()).isEqualTo(1);
        assertThat(taskService.createTaskQuery().processVariableValueEquals("stringVar", "stringValue").count()).isEqualTo(1);
        assertThat(taskService.createTaskQuery().processVariableValueEquals("booleanVar", true).count()).isEqualTo(1);
        assertThat(taskService.createTaskQuery().processVariableValueEquals("dateVar", date).count()).isEqualTo(1);
        assertThat(taskService.createTaskQuery().processVariableValueEquals("nullVar", null).count()).isEqualTo(1);

        // Test query for other values on existing variables
        assertThat(taskService.createTaskQuery().processVariableValueEquals("longVar", 999L).count()).isZero();
        assertThat(taskService.createTaskQuery().processVariableValueEquals("shortVar", (short) 999).count()).isZero();
        assertThat(taskService.createTaskQuery().processVariableValueEquals("integerVar", 999).count()).isZero();
        assertThat(taskService.createTaskQuery().processVariableValueEquals("stringVar", "999").count()).isZero();
        assertThat(taskService.createTaskQuery().processVariableValueEquals("booleanVar", false).count()).isZero();
        Calendar otherDate = Calendar.getInstance();
        otherDate.add(Calendar.YEAR, 1);
        assertThat(taskService.createTaskQuery().processVariableValueEquals("dateVar", otherDate.getTime()).count()).isZero();
        assertThat(taskService.createTaskQuery().processVariableValueEquals("nullVar", "999").count()).isZero();

        // Test querying for task variables don't match the process-variables
        assertThat(taskService.createTaskQuery().taskVariableValueEquals("longVar", 928374L).count()).isZero();
        assertThat(taskService.createTaskQuery().taskVariableValueEquals("shortVar", (short) 123).count()).isZero();
        assertThat(taskService.createTaskQuery().taskVariableValueEquals("integerVar", 1234).count()).isZero();
        assertThat(taskService.createTaskQuery().taskVariableValueEquals("stringVar", "stringValue").count()).isZero();
        assertThat(taskService.createTaskQuery().taskVariableValueEquals("booleanVar", true).count()).isZero();
        assertThat(taskService.createTaskQuery().taskVariableValueEquals("dateVar", date).count()).isZero();
        assertThat(taskService.createTaskQuery().taskVariableValueEquals("nullVar", null).count()).isZero();

        // Test querying for task variables not equals
        assertThat(taskService.createTaskQuery().processVariableValueNotEquals("longVar", 999L).count()).isEqualTo(1);
        assertThat(taskService.createTaskQuery().processVariableValueNotEquals("shortVar", (short) 999).count()).isEqualTo(1);
        assertThat(taskService.createTaskQuery().processVariableValueNotEquals("integerVar", 999).count()).isEqualTo(1);
        assertThat(taskService.createTaskQuery().processVariableValueNotEquals("stringVar", "999").count()).isEqualTo(1);
        assertThat(taskService.createTaskQuery().processVariableValueNotEquals("booleanVar", false).count()).isEqualTo(1);

        // and query for the existing variable with NOT should result in nothing found:
        assertThat(taskService.createTaskQuery().processVariableValueNotEquals("longVar", 928374L).count()).isZero();

        // Test value-only variable equals
        assertThat(taskService.createTaskQuery().processVariableValueEquals(928374L).count()).isEqualTo(1);
        assertThat(taskService.createTaskQuery().processVariableValueEquals((short) 123).count()).isEqualTo(1);
        assertThat(taskService.createTaskQuery().processVariableValueEquals(1234).count()).isEqualTo(1);
        assertThat(taskService.createTaskQuery().processVariableValueEquals("stringValue").count()).isEqualTo(1);
        assertThat(taskService.createTaskQuery().processVariableValueEquals(true).count()).isEqualTo(1);
        assertThat(taskService.createTaskQuery().processVariableValueEquals(date).count()).isEqualTo(1);
        assertThat(taskService.createTaskQuery().processVariableValueEquals(null).count()).isEqualTo(1);

        assertThat(taskService.createTaskQuery().processVariableValueEquals(999999L).count()).isZero();
        assertThat(taskService.createTaskQuery().processVariableValueEquals((short) 999).count()).isZero();
        assertThat(taskService.createTaskQuery().processVariableValueEquals(9999).count()).isZero();
        assertThat(taskService.createTaskQuery().processVariableValueEquals("unexistingstringvalue").count()).isZero();
        assertThat(taskService.createTaskQuery().processVariableValueEquals(false).count()).isZero();
        assertThat(taskService.createTaskQuery().processVariableValueEquals(otherDate.getTime()).count()).isZero();

        // Test combination of task-variable and process-variable
        org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        taskService.setVariableLocal(task.getId(), "taskVar", "theValue");
        taskService.setVariableLocal(task.getId(), "longVar", 928374L);

        assertThat(taskService.createTaskQuery().processVariableValueEquals("longVar", 928374L).taskVariableValueEquals("taskVar", "theValue").count())
                .isEqualTo(1);

        assertThat(taskService.createTaskQuery().processVariableValueEquals("longVar", 928374L).taskVariableValueEquals("longVar", 928374L).count())
                .isEqualTo(1);

        assertThat(taskService.createTaskQuery().processVariableValueEquals(928374L).taskVariableValueEquals("theValue").count()).isEqualTo(1);

        assertThat(taskService.createTaskQuery().processVariableValueEquals(928374L).taskVariableValueEquals(928374L).count()).isEqualTo(1);
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/task/TaskQueryTest.testProcessVariableValueEquals.bpmn20.xml" })
    public void testProcessVariableValueEqualsOr() throws Exception {
        Map<String, Object> variables = new HashMap<>();
        variables.put("longVar", 928374L);
        variables.put("shortVar", (short) 123);
        variables.put("integerVar", 1234);
        variables.put("stringVar", "stringValue");
        variables.put("booleanVar", true);
        Date date = Calendar.getInstance().getTime();
        variables.put("dateVar", date);
        variables.put("nullVar", null);

        // Start process-instance with all types of variables
        runtimeService.startProcessInstanceByKey("oneTaskProcess", variables);

        // Test query matches
        assertThat(taskService.createTaskQuery().or().taskId("invalid").processVariableValueEquals("longVar", 928374L).count()).isEqualTo(1);
        assertThat(taskService.createTaskQuery().or().taskId("invalid").processVariableValueEquals("shortVar", (short) 123).count()).isEqualTo(1);
        assertThat(taskService.createTaskQuery().or().taskId("invalid").processVariableValueEquals("integerVar", 1234).count()).isEqualTo(1);
        assertThat(taskService.createTaskQuery().or().taskId("invalid").processVariableValueEquals("stringVar", "stringValue").count()).isEqualTo(1);
        assertThat(taskService.createTaskQuery().or().taskId("invalid").processVariableValueEquals("booleanVar", true).count()).isEqualTo(1);
        assertThat(taskService.createTaskQuery().or().taskId("invalid").processVariableValueEquals("dateVar", date).count()).isEqualTo(1);
        assertThat(taskService.createTaskQuery().or().taskId("invalid").processVariableValueEquals("nullVar", null).count()).isEqualTo(1);

        // Test query for other values on existing variables
        assertThat(taskService.createTaskQuery().or().taskId("invalid").processVariableValueEquals("longVar", 999L).count()).isZero();
        assertThat(taskService.createTaskQuery().or().taskId("invalid").processVariableValueEquals("shortVar", (short) 999).count()).isZero();
        assertThat(taskService.createTaskQuery().or().taskId("invalid").processVariableValueEquals("integerVar", 999).count()).isZero();
        assertThat(taskService.createTaskQuery().or().taskId("invalid").processVariableValueEquals("stringVar", "999").count()).isZero();
        assertThat(taskService.createTaskQuery().or().taskId("invalid").processVariableValueEquals("booleanVar", false).count()).isZero();
        Calendar otherDate = Calendar.getInstance();
        otherDate.add(Calendar.YEAR, 1);
        assertThat(taskService.createTaskQuery().or().taskId("invalid").processVariableValueEquals("dateVar", otherDate.getTime()).count()).isZero();
        assertThat(taskService.createTaskQuery().or().taskId("invalid").processVariableValueEquals("nullVar", "999").count()).isZero();

        // Test querying for task variables don't match the process-variables
        assertThat(taskService.createTaskQuery().or().taskId("invalid").taskVariableValueEquals("longVar", 928374L).count()).isZero();
        assertThat(taskService.createTaskQuery().or().taskId("invalid").taskVariableValueEquals("shortVar", (short) 123).count()).isZero();
        assertThat(taskService.createTaskQuery().or().taskId("invalid").taskVariableValueEquals("integerVar", 1234).count()).isZero();
        assertThat(taskService.createTaskQuery().or().taskId("invalid").taskVariableValueEquals("stringVar", "stringValue").count()).isZero();
        assertThat(taskService.createTaskQuery().or().taskId("invalid").taskVariableValueEquals("booleanVar", true).count()).isZero();
        assertThat(taskService.createTaskQuery().or().taskId("invalid").taskVariableValueEquals("dateVar", date).count()).isZero();
        assertThat(taskService.createTaskQuery().or().taskId("invalid").taskVariableValueEquals("nullVar", null).count()).isZero();

        // Test querying for task variables not equals
        assertThat(taskService.createTaskQuery().or().taskId("invalid").processVariableValueNotEquals("longVar", 999L).count()).isEqualTo(1);
        assertThat(taskService.createTaskQuery().or().taskId("invalid").processVariableValueNotEquals("shortVar", (short) 999).count()).isEqualTo(1);
        assertThat(taskService.createTaskQuery().or().taskId("invalid").processVariableValueNotEquals("integerVar", 999).count()).isEqualTo(1);
        assertThat(taskService.createTaskQuery().or().taskId("invalid").processVariableValueNotEquals("stringVar", "999").count()).isEqualTo(1);
        assertThat(taskService.createTaskQuery().or().taskId("invalid").processVariableValueNotEquals("booleanVar", false).count()).isEqualTo(1);

        // and query for the existing variable with NOT should result in nothing found:
        assertThat(taskService.createTaskQuery().processVariableValueNotEquals("longVar", 928374L).count()).isZero();

        // Test value-only variable equals
        assertThat(taskService.createTaskQuery().or().taskId("invalid").processVariableValueEquals(928374L).count()).isEqualTo(1);
        assertThat(taskService.createTaskQuery().or().taskId("invalid").processVariableValueEquals((short) 123).count()).isEqualTo(1);
        assertThat(taskService.createTaskQuery().or().taskId("invalid").processVariableValueEquals(1234).count()).isEqualTo(1);
        assertThat(taskService.createTaskQuery().or().taskId("invalid").processVariableValueEquals("stringValue").count()).isEqualTo(1);
        assertThat(taskService.createTaskQuery().or().taskId("invalid").processVariableValueEquals(true).count()).isEqualTo(1);
        assertThat(taskService.createTaskQuery().or().taskId("invalid").processVariableValueEquals(date).count()).isEqualTo(1);
        assertThat(taskService.createTaskQuery().or().taskId("invalid").processVariableValueEquals(null).count()).isEqualTo(1);

        assertThat(taskService.createTaskQuery().or().taskId("invalid").processVariableValueEquals(999999L).count()).isZero();
        assertThat(taskService.createTaskQuery().or().taskId("invalid").processVariableValueEquals((short) 999).count()).isZero();
        assertThat(taskService.createTaskQuery().or().taskId("invalid").processVariableValueEquals(9999).count()).isZero();
        assertThat(taskService.createTaskQuery().or().taskId("invalid").processVariableValueEquals("unexistingstringvalue").count()).isZero();
        assertThat(taskService.createTaskQuery().or().taskId("invalid").processVariableValueEquals(false).count()).isZero();
        assertThat(taskService.createTaskQuery().or().taskId("invalid").processVariableValueEquals(otherDate.getTime()).count()).isZero();
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/api/task/TaskQueryTest.testProcessDefinition.bpmn20.xml")
    public void testVariableValueEqualsIgnoreCase() throws Exception {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");

        org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).isNotNull();

        Map<String, Object> variables = new HashMap<>();
        variables.put("mixed", "AzerTY");
        variables.put("upper", "AZERTY");
        variables.put("lower", "azerty");
        taskService.setVariablesLocal(task.getId(), variables);

        assertThat(taskService.createTaskQuery().taskVariableValueEqualsIgnoreCase("mixed", "azerTY").count()).isEqualTo(1);
        assertThat(taskService.createTaskQuery().taskVariableValueEqualsIgnoreCase("mixed", "azerty").count()).isEqualTo(1);
        assertThat(taskService.createTaskQuery().taskVariableValueEqualsIgnoreCase("mixed", "uiop").count()).isZero();

        assertThat(taskService.createTaskQuery().taskVariableValueEqualsIgnoreCase("upper", "azerTY").count()).isEqualTo(1);
        assertThat(taskService.createTaskQuery().taskVariableValueEqualsIgnoreCase("upper", "azerty").count()).isEqualTo(1);
        assertThat(taskService.createTaskQuery().taskVariableValueEqualsIgnoreCase("upper", "uiop").count()).isZero();

        assertThat(taskService.createTaskQuery().taskVariableValueEqualsIgnoreCase("lower", "azerTY").count()).isEqualTo(1);
        assertThat(taskService.createTaskQuery().taskVariableValueEqualsIgnoreCase("lower", "azerty").count()).isEqualTo(1);
        assertThat(taskService.createTaskQuery().taskVariableValueEqualsIgnoreCase("lower", "uiop").count()).isZero();

        // Test not-equals
        assertThat(taskService.createTaskQuery().taskVariableValueNotEqualsIgnoreCase("mixed", "azerTY").count()).isZero();
        assertThat(taskService.createTaskQuery().taskVariableValueNotEqualsIgnoreCase("mixed", "azerty").count()).isZero();
        assertThat(taskService.createTaskQuery().taskVariableValueNotEqualsIgnoreCase("mixed", "uiop").count()).isEqualTo(1);

        assertThat(taskService.createTaskQuery().taskVariableValueNotEqualsIgnoreCase("upper", "azerTY").count()).isZero();
        assertThat(taskService.createTaskQuery().taskVariableValueNotEqualsIgnoreCase("upper", "azerty").count()).isZero();
        assertThat(taskService.createTaskQuery().taskVariableValueNotEqualsIgnoreCase("upper", "uiop").count()).isEqualTo(1);

        assertThat(taskService.createTaskQuery().taskVariableValueNotEqualsIgnoreCase("lower", "azerTY").count()).isZero();
        assertThat(taskService.createTaskQuery().taskVariableValueNotEqualsIgnoreCase("lower", "azerty").count()).isZero();
        assertThat(taskService.createTaskQuery().taskVariableValueNotEqualsIgnoreCase("lower", "uiop").count()).isEqualTo(1);

    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/api/task/TaskQueryTest.testProcessDefinition.bpmn20.xml")
    public void testProcessVariableValueEqualsIgnoreCase() throws Exception {
        Map<String, Object> variables = new HashMap<>();
        variables.put("mixed", "AzerTY");
        variables.put("upper", "AZERTY");
        variables.put("lower", "azerty");

        runtimeService.startProcessInstanceByKey("oneTaskProcess", variables);

        assertThat(taskService.createTaskQuery().processVariableValueEqualsIgnoreCase("mixed", "azerTY").count()).isEqualTo(1);
        assertThat(taskService.createTaskQuery().processVariableValueEqualsIgnoreCase("mixed", "azerty").count()).isEqualTo(1);
        assertThat(taskService.createTaskQuery().processVariableValueEqualsIgnoreCase("mixed", "uiop").count()).isZero();

        assertThat(taskService.createTaskQuery().processVariableValueEqualsIgnoreCase("upper", "azerTY").count()).isEqualTo(1);
        assertThat(taskService.createTaskQuery().processVariableValueEqualsIgnoreCase("upper", "azerty").count()).isEqualTo(1);
        assertThat(taskService.createTaskQuery().processVariableValueEqualsIgnoreCase("upper", "uiop").count()).isZero();

        assertThat(taskService.createTaskQuery().processVariableValueEqualsIgnoreCase("lower", "azerTY").count()).isEqualTo(1);
        assertThat(taskService.createTaskQuery().processVariableValueEqualsIgnoreCase("lower", "azerty").count()).isEqualTo(1);
        assertThat(taskService.createTaskQuery().processVariableValueEqualsIgnoreCase("lower", "uiop").count()).isZero();
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/api/task/TaskQueryTest.testProcessDefinition.bpmn20.xml")
    public void testProcessVariableValueLike() throws Exception {
        Map<String, Object> variables = new HashMap<>();
        variables.put("mixed", "AzerTY");

        runtimeService.startProcessInstanceByKey("oneTaskProcess", variables);

        assertThat(taskService.createTaskQuery().processVariableValueLike("mixed", "Azer%").count()).isEqualTo(1);
        assertThat(taskService.createTaskQuery().processVariableValueLike("mixed", "A%").count()).isEqualTo(1);
        assertThat(taskService.createTaskQuery().processVariableValueLike("mixed", "a%").count()).isZero();
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/api/task/TaskQueryTest.testProcessDefinition.bpmn20.xml")
    public void testProcessVariableValueLikeIgnoreCase() throws Exception {
        Map<String, Object> variables = new HashMap<>();
        variables.put("mixed", "AzerTY");

        runtimeService.startProcessInstanceByKey("oneTaskProcess", variables);

        assertThat(taskService.createTaskQuery().processVariableValueLikeIgnoreCase("mixed", "azer%").count()).isEqualTo(1);
        assertThat(taskService.createTaskQuery().processVariableValueLikeIgnoreCase("mixed", "a%").count()).isEqualTo(1);
        assertThat(taskService.createTaskQuery().processVariableValueLikeIgnoreCase("mixed", "Azz%").count()).isZero();
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/api/task/TaskQueryTest.testProcessDefinition.bpmn20.xml")
    public void testProcessVariableValueGreaterThan() throws Exception {
        Map<String, Object> variables = new HashMap<>();
        variables.put("number", 10);

        runtimeService.startProcessInstanceByKey("oneTaskProcess", variables);

        assertThat(taskService.createTaskQuery().processVariableValueGreaterThan("number", 5).count()).isEqualTo(1);
        assertThat(taskService.createTaskQuery().processVariableValueGreaterThan("number", 10).count()).isZero();
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/api/task/TaskQueryTest.testProcessDefinition.bpmn20.xml")
    public void testProcessVariableValueGreaterThanOrEquals() throws Exception {
        Map<String, Object> variables = new HashMap<>();
        variables.put("number", 10);

        runtimeService.startProcessInstanceByKey("oneTaskProcess", variables);

        assertThat(taskService.createTaskQuery().processVariableValueGreaterThanOrEqual("number", 5).count()).isEqualTo(1);
        assertThat(taskService.createTaskQuery().processVariableValueGreaterThanOrEqual("number", 10).count()).isEqualTo(1);
        assertThat(taskService.createTaskQuery().processVariableValueGreaterThanOrEqual("number", 11).count()).isZero();
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/api/task/TaskQueryTest.testProcessDefinition.bpmn20.xml")
    public void testProcessVariableValueLessThan() throws Exception {
        Map<String, Object> variables = new HashMap<>();
        variables.put("number", 10);

        runtimeService.startProcessInstanceByKey("oneTaskProcess", variables);

        assertThat(taskService.createTaskQuery().processVariableValueLessThan("number", 12).count()).isEqualTo(1);
        assertThat(taskService.createTaskQuery().processVariableValueLessThan("number", 10).count()).isZero();
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/api/task/TaskQueryTest.testProcessDefinition.bpmn20.xml")
    public void testProcessVariableValueLessThanOrEquals() throws Exception {
        Map<String, Object> variables = new HashMap<>();
        variables.put("number", 10);

        runtimeService.startProcessInstanceByKey("oneTaskProcess", variables);

        assertThat(taskService.createTaskQuery().processVariableValueLessThanOrEqual("number", 12).count()).isEqualTo(1);
        assertThat(taskService.createTaskQuery().processVariableValueLessThanOrEqual("number", 10).count()).isEqualTo(1);
        assertThat(taskService.createTaskQuery().processVariableValueLessThanOrEqual("number", 8).count()).isZero();
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/task/TaskQueryTest.testProcessDefinition.bpmn20.xml" })
    public void testProcessDefinitionId() throws Exception {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");

        List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().processDefinitionId(processInstance.getProcessDefinitionId()).list();
        assertThat(tasks)
                .extracting(Task::getProcessInstanceId)
                .containsExactly(processInstance.getId());

        assertThat(taskService.createTaskQuery().processDefinitionId("unexisting").count()).isZero();
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/task/TaskQueryTest.testProcessDefinition.bpmn20.xml" })
    public void testProcessDefinitionIdOr() throws Exception {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");

        List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().or().taskId("invalid")
                .processDefinitionId(processInstance.getProcessDefinitionId()).list();
        assertThat(tasks)
                .extracting(Task::getProcessInstanceId)
                .containsExactly(processInstance.getId());

        tasks = taskService.createTaskQuery()
                .or()
                .taskId("invalid")
                .processDefinitionId(processInstance.getProcessDefinitionId())
                .endOr()
                .or()
                .processDefinitionKey("oneTaskProcess")
                .processDefinitionId("invalid")
                .endOr()
                .list();
        assertThat(tasks)
                .extracting(Task::getProcessInstanceId)
                .containsExactly(processInstance.getId());

        assertThat(taskService.createTaskQuery()
                .or()
                .taskId("invalid")
                .processDefinitionId("unexisting")
                .count()).isZero();
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/task/TaskQueryTest.testProcessDefinition.bpmn20.xml" })
    public void testProcessDefinitionKey() throws Exception {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");

        List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().processDefinitionKey("oneTaskProcess").list();
        assertThat(tasks)
                .extracting(Task::getProcessInstanceId)
                .containsExactly(processInstance.getId());

        assertThat(taskService.createTaskQuery().processDefinitionKey("unexisting").count()).isZero();
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/task/TaskQueryTest.testProcessDefinition.bpmn20.xml" })
    public void testProcessDefinitionKeyOr() throws Exception {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");

        List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().or().taskId("invalid").processDefinitionKey("oneTaskProcess").list();
        assertThat(tasks)
                .extracting(Task::getProcessInstanceId)
                .containsExactly(processInstance.getId());

        assertThat(taskService.createTaskQuery().or().taskId("invalid").processDefinitionKey("unexisting").count()).isZero();

        assertThat(taskService.createTaskQuery().or().taskId(taskIds.get(0)).processDefinitionKey("unexisting").endOr().count()).isEqualTo(1);
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/task/TaskQueryTest.testProcessDefinition.bpmn20.xml" })
    public void testProcessDefinitionKeyIn() throws Exception {
        runtimeService.startProcessInstanceByKey("oneTaskProcess");
        List<String> includeIds = new ArrayList<>();

        assertThat(taskService.createTaskQuery().processDefinitionKeyIn(includeIds).count()).isEqualTo(13);
        includeIds.add("unexisting");
        assertThat(taskService.createTaskQuery().processDefinitionKeyIn(includeIds).count()).isZero();
        includeIds.add("oneTaskProcess");
        assertThat(taskService.createTaskQuery().processDefinitionKeyIn(includeIds).count()).isEqualTo(1);
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/task/TaskQueryTest.testProcessDefinition.bpmn20.xml" })
    public void testProcessDefinitionKeyInOr() throws Exception {
        runtimeService.startProcessInstanceByKey("oneTaskProcess");

        List<String> includeIds = new ArrayList<>();
        assertThat(taskService.createTaskQuery()
                .or().taskId("invalid")
                .processDefinitionKeyIn(includeIds)
                .count()).isZero();

        includeIds.add("unexisting");
        assertThat(taskService.createTaskQuery()
                .or().taskId("invalid")
                .processDefinitionKeyIn(includeIds)
                .count()).isZero();

        includeIds.add("oneTaskProcess");
        assertThat(taskService.createTaskQuery()
                .or().taskId("invalid")
                .processDefinitionKeyIn(includeIds)
                .count()).isEqualTo(1);
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/task/TaskQueryTest.testProcessDefinition.bpmn20.xml" })
    public void testProcessDefinitionName() throws Exception {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");

        List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().processDefinitionName("The One Task Process").list();
        assertThat(tasks)
                .extracting(Task::getProcessInstanceId)
                .containsExactly(processInstance.getId());

        assertThat(taskService.createTaskQuery().processDefinitionName("unexisting").count()).isZero();
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/task/TaskQueryTest.testProcessDefinition.bpmn20.xml" })
    public void testProcessDefinitionNameOr() throws Exception {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");

        List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().or().taskId("invalid").processDefinitionName("The One Task Process").list();
        assertThat(tasks)
                .extracting(Task::getProcessInstanceId)
                .containsExactly(processInstance.getId());

        assertThat(taskService.createTaskQuery().or().taskId("invalid").processDefinitionName("unexisting").count()).isZero();
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/task/TaskQueryTest.testProcessDefinition.bpmn20.xml" })
    public void testProcessCategoryIn() throws Exception {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");

        final org.flowable.task.api.Task task = taskService.createTaskQuery().processCategoryIn(Collections.singletonList("Examples")).singleResult();
        assertThat(task).isNotNull();
        assertThat(task.getTaskDefinitionKey()).isEqualTo("theTask");
        assertThat(task.getProcessInstanceId()).isEqualTo(processInstance.getId());

        assertThat(taskService.createTaskQuery().processCategoryIn(Collections.singletonList("unexisting")).count()).isZero();
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/task/TaskQueryTest.testProcessDefinition.bpmn20.xml" })
    public void testProcessCategoryInOr() throws Exception {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");

        org.flowable.task.api.Task task = taskService.createTaskQuery()
                .or()
                .taskId("invalid")
                .processCategoryIn(Collections.singletonList("Examples")).singleResult();
        assertThat(task).isNotNull();
        assertThat(task.getTaskDefinitionKey()).isEqualTo("theTask");
        assertThat(task.getProcessInstanceId()).isEqualTo(processInstance.getId());

        task = taskService.createTaskQuery()
                .or()
                .taskId("invalid")
                .processCategoryIn(Collections.singletonList("Examples"))
                .endOr()
                .or()
                .taskId(task.getId())
                .processCategoryIn(Collections.singletonList("Examples2"))
                .endOr()
                .singleResult();
        assertThat(task).isNotNull();
        assertThat(task.getTaskDefinitionKey()).isEqualTo("theTask");
        assertThat(task.getProcessInstanceId()).isEqualTo(processInstance.getId());

        assertThat(taskService.createTaskQuery().or().taskId("invalid").processCategoryIn(Collections.singletonList("unexisting")).count()).isZero();
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/task/TaskQueryTest.testProcessDefinition.bpmn20.xml" })
    public void testProcessCategoryNotIn() throws Exception {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");

        final org.flowable.task.api.Task task = taskService.createTaskQuery().processCategoryNotIn(Collections.singletonList("unexisting")).singleResult();
        assertThat(task).isNotNull();
        assertThat(task.getTaskDefinitionKey()).isEqualTo("theTask");
        assertThat(task.getProcessInstanceId()).isEqualTo(processInstance.getId());

        assertThat(taskService.createTaskQuery().processCategoryNotIn(Collections.singletonList("Examples")).count()).isZero();
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/task/TaskQueryTest.testProcessDefinition.bpmn20.xml" })
    public void testProcessCategoryNotInOr() throws Exception {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");

        final org.flowable.task.api.Task task = taskService.createTaskQuery().or().taskId("invalid")
                .processCategoryNotIn(Collections.singletonList("unexisting")).singleResult();
        assertThat(task).isNotNull();
        assertThat(task.getTaskDefinitionKey()).isEqualTo("theTask");
        assertThat(task.getProcessInstanceId()).isEqualTo(processInstance.getId());

        assertThat(taskService.createTaskQuery().or().taskId("invalid").processCategoryNotIn(Collections.singletonList("Examples")).count()).isZero();
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/task/TaskQueryTest.testProcessDefinition.bpmn20.xml" })
    public void testProcessInstanceIdIn() throws Exception {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");

        final org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceIdIn(Collections.singletonList(processInstance.getId()))
                .singleResult();
        assertThat(task).isNotNull();
        assertThat(task.getTaskDefinitionKey()).isEqualTo("theTask");
        assertThat(task.getProcessInstanceId()).isEqualTo(processInstance.getId());

        assertThat(taskService.createTaskQuery().processInstanceIdIn(Collections.singletonList("unexisting")).count()).isZero();
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/task/TaskQueryTest.testProcessDefinition.bpmn20.xml" })
    public void testProcessInstanceIdInOr() throws Exception {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");

        final org.flowable.task.api.Task task = taskService.createTaskQuery().or().taskId("invalid").processInstanceIdIn(Collections.singletonList(
                processInstance.getId())).singleResult();
        assertThat(task).isNotNull();
        assertThat(task.getTaskDefinitionKey()).isEqualTo("theTask");
        assertThat(task.getProcessInstanceId()).isEqualTo(processInstance.getId());

        assertThat(taskService.createTaskQuery().or().taskId("invalid").processInstanceIdIn(Collections.singletonList("unexisting")).count()).isZero();
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/task/TaskQueryTest.testProcessDefinition.bpmn20.xml" })
    public void testProcessInstanceIdInMultiple() throws Exception {
        ProcessInstance processInstance1 = runtimeService.startProcessInstanceByKey("oneTaskProcess");
        ProcessInstance processInstance2 = runtimeService.startProcessInstanceByKey("oneTaskProcess");

        assertThat(taskService.createTaskQuery().processInstanceIdIn(Arrays.asList(processInstance1.getId(), processInstance2.getId())).count()).isEqualTo(2);
        assertThat(taskService.createTaskQuery().processInstanceIdIn(Arrays.asList(processInstance1.getId(), processInstance2.getId(), "unexisting")).count())
                .isEqualTo(2);

        assertThat(taskService.createTaskQuery().processInstanceIdIn(Arrays.asList("unexisting1", "unexisting2")).count()).isZero();
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/task/TaskQueryTest.testProcessDefinition.bpmn20.xml" })
    public void testProcessInstanceIdInOrMultiple() throws Exception {
        ProcessInstance processInstance1 = runtimeService.startProcessInstanceByKey("oneTaskProcess");
        ProcessInstance processInstance2 = runtimeService.startProcessInstanceByKey("oneTaskProcess");

        assertThat(taskService.createTaskQuery().or().taskId("invalid").processInstanceIdIn(Arrays.asList(processInstance1.getId(), processInstance2.getId()))
                .count()).isEqualTo(2);
        assertThat(taskService.createTaskQuery().or().taskId("invalid")
                .processInstanceIdIn(Arrays.asList(processInstance1.getId(), processInstance2.getId(), "unexisting")).count()).isEqualTo(2);

        assertThat(taskService.createTaskQuery().or().taskId("invalid").processInstanceIdIn(Arrays.asList("unexisting1", "unexisting2")).count()).isZero();
    }
    
    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/task/TaskQueryTest.testProcessDefinition.bpmn20.xml" })
    public void testWithoutProcessInstanceId() throws Exception {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");

        assertThat(taskService.createTaskQuery().processInstanceId(processInstance.getId()).count()).isEqualTo(1);
        assertThat(taskService.createTaskQuery().withoutProcessInstanceId().count()).isEqualTo(12);
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/task/TaskQueryTest.testProcessDefinition.bpmn20.xml" })
    public void testProcessInstanceBusinessKey() throws Exception {
        runtimeService.startProcessInstanceByKey("oneTaskProcess", "BUSINESS-KEY-1");

        assertThat(taskService.createTaskQuery().processDefinitionName("The One Task Process").processInstanceBusinessKey("BUSINESS-KEY-1").list()).hasSize(1);
        assertThat(taskService.createTaskQuery().processInstanceBusinessKey("BUSINESS-KEY-1").list()).hasSize(1);
        assertThat(taskService.createTaskQuery().processInstanceBusinessKey("NON-EXISTING").count()).isZero();
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/task/TaskQueryTest.testProcessDefinition.bpmn20.xml" })
    public void testProcessInstanceBusinessKeyOr() throws Exception {
        runtimeService.startProcessInstanceByKey("oneTaskProcess", "BUSINESS-KEY-1");

        assertThat(
                taskService.createTaskQuery().processDefinitionName("The One Task Process").or().taskId("invalid").processInstanceBusinessKey("BUSINESS-KEY-1")
                        .list()).hasSize(1);
        assertThat(taskService.createTaskQuery().or().taskId("invalid").processInstanceBusinessKey("BUSINESS-KEY-1").list()).hasSize(1);
        assertThat(taskService.createTaskQuery().or().taskId("invalid").processInstanceBusinessKey("NON-EXISTING").count()).isZero();
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/task/TaskQueryTest.testProcessDefinition.bpmn20.xml" })
    public void testTaskDueDate() throws Exception {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
        org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();

        // Set due-date on task
        Date dueDate = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss").parse("01/02/2003 01:12:13");
        task.setDueDate(dueDate);
        taskService.saveTask(task);

        assertThat(taskService.createTaskQuery().processInstanceId(processInstance.getId()).taskDueDate(dueDate).count()).isEqualTo(1);

        Calendar otherDate = Calendar.getInstance();
        otherDate.add(Calendar.YEAR, 1);
        assertThat(taskService.createTaskQuery().processInstanceId(processInstance.getId()).taskDueDate(otherDate.getTime()).count()).isZero();

        Calendar priorDate = Calendar.getInstance();
        priorDate.setTime(dueDate);
        priorDate.roll(Calendar.YEAR, -1);

        assertThat(taskService.createTaskQuery().processInstanceId(processInstance.getId()).taskDueAfter(priorDate.getTime()).count()).isEqualTo(1);

        assertThat(taskService.createTaskQuery().processInstanceId(processInstance.getId()).taskDueBefore(otherDate.getTime()).count()).isEqualTo(1);
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/task/TaskQueryTest.testProcessDefinition.bpmn20.xml" })
    public void testTaskDueDateOr() throws Exception {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
        org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();

        // Set due-date on task
        Date dueDate = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss").parse("01/02/2003 01:12:13");
        task.setDueDate(dueDate);
        taskService.saveTask(task);

        assertThat(taskService.createTaskQuery().processInstanceId(processInstance.getId()).or().taskId("invalid").taskDueDate(dueDate).count()).isEqualTo(1);

        Calendar otherDate = Calendar.getInstance();
        otherDate.add(Calendar.YEAR, 1);
        assertThat(taskService.createTaskQuery().processInstanceId(processInstance.getId()).or().taskId("invalid").taskDueDate(otherDate.getTime()).count())
                .isZero();

        Calendar priorDate = Calendar.getInstance();
        priorDate.setTime(dueDate);
        priorDate.roll(Calendar.YEAR, -1);

        assertThat(taskService.createTaskQuery().processInstanceId(processInstance.getId()).or().taskId("invalid").taskDueAfter(priorDate.getTime()).count())
                .isEqualTo(1);

        assertThat(taskService.createTaskQuery().processInstanceId(processInstance.getId()).taskDueBefore(otherDate.getTime()).count()).isEqualTo(1);
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/task/TaskQueryTest.testProcessDefinition.bpmn20.xml" })
    public void testTaskDueBefore() throws Exception {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
        org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();

        // Set due-date on task
        Calendar dueDateCal = Calendar.getInstance();
        task.setDueDate(dueDateCal.getTime());
        taskService.saveTask(task);

        Calendar oneHourAgo = Calendar.getInstance();
        oneHourAgo.setTime(dueDateCal.getTime());
        oneHourAgo.add(Calendar.HOUR, -1);

        Calendar oneHourLater = Calendar.getInstance();
        oneHourLater.setTime(dueDateCal.getTime());
        oneHourLater.add(Calendar.HOUR, 1);

        assertThat(taskService.createTaskQuery().processInstanceId(processInstance.getId()).taskDueBefore(oneHourLater.getTime()).count()).isEqualTo(1);
        assertThat(taskService.createTaskQuery().processInstanceId(processInstance.getId()).taskDueBefore(oneHourAgo.getTime()).count()).isZero();

        // Update due-date to null, shouldn't show up anymore in query that
        // matched before
        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        task.setDueDate(null);
        taskService.saveTask(task);

        assertThat(taskService.createTaskQuery().processInstanceId(processInstance.getId()).taskDueBefore(oneHourLater.getTime()).count()).isZero();
        assertThat(taskService.createTaskQuery().processInstanceId(processInstance.getId()).taskDueBefore(oneHourAgo.getTime()).count()).isZero();
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/task/TaskQueryTest.testProcessDefinition.bpmn20.xml" })
    public void testTaskDueBeforeOr() throws Exception {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
        org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();

        // Set due-date on task
        Calendar dueDateCal = Calendar.getInstance();
        task.setDueDate(dueDateCal.getTime());
        taskService.saveTask(task);

        Calendar oneHourAgo = Calendar.getInstance();
        oneHourAgo.setTime(dueDateCal.getTime());
        oneHourAgo.add(Calendar.HOUR, -1);

        Calendar oneHourLater = Calendar.getInstance();
        oneHourLater.setTime(dueDateCal.getTime());
        oneHourLater.add(Calendar.HOUR, 1);

        assertThat(
                taskService.createTaskQuery().processInstanceId(processInstance.getId()).or().taskId("invalid").taskDueBefore(oneHourLater.getTime()).count())
                .isEqualTo(1);
        assertThat(taskService.createTaskQuery().processInstanceId(processInstance.getId()).or().taskId("invalid").taskDueBefore(oneHourAgo.getTime()).count())
                .isZero();

        // Update due-date to null, shouldn't show up anymore in query that
        // matched before
        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        task.setDueDate(null);
        taskService.saveTask(task);

        assertThat(
                taskService.createTaskQuery().processInstanceId(processInstance.getId()).or().taskId("invalid").taskDueBefore(oneHourLater.getTime()).count())
                .isZero();
        assertThat(taskService.createTaskQuery().processInstanceId(processInstance.getId()).or().taskId("invalid").taskDueBefore(oneHourAgo.getTime()).count())
                .isZero();
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/task/TaskQueryTest.testProcessDefinition.bpmn20.xml" })
    public void testTaskDueAfter() throws Exception {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
        org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();

        // Set due-date on task
        Calendar dueDateCal = Calendar.getInstance();
        task.setDueDate(dueDateCal.getTime());
        taskService.saveTask(task);

        Calendar oneHourAgo = Calendar.getInstance();
        oneHourAgo.setTime(dueDateCal.getTime());
        oneHourAgo.add(Calendar.HOUR, -1);

        Calendar oneHourLater = Calendar.getInstance();
        oneHourLater.setTime(dueDateCal.getTime());
        oneHourLater.add(Calendar.HOUR, 1);

        assertThat(taskService.createTaskQuery().processInstanceId(processInstance.getId()).taskDueAfter(oneHourAgo.getTime()).count()).isEqualTo(1);
        assertThat(taskService.createTaskQuery().processInstanceId(processInstance.getId()).taskDueAfter(oneHourLater.getTime()).count()).isZero();

        // Update due-date to null, shouldn't show up anymore in query that
        // matched before
        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        task.setDueDate(null);
        taskService.saveTask(task);

        assertThat(taskService.createTaskQuery().processInstanceId(processInstance.getId()).taskDueAfter(oneHourLater.getTime()).count()).isZero();
        assertThat(taskService.createTaskQuery().processInstanceId(processInstance.getId()).taskDueAfter(oneHourAgo.getTime()).count()).isZero();
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/task/TaskQueryTest.testProcessDefinition.bpmn20.xml" })
    public void testTaskDueAfterOn() throws Exception {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
        org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();

        // Set due-date on task
        Calendar dueDateCal = Calendar.getInstance();
        task.setDueDate(dueDateCal.getTime());
        taskService.saveTask(task);

        Calendar oneHourAgo = Calendar.getInstance();
        oneHourAgo.setTime(dueDateCal.getTime());
        oneHourAgo.add(Calendar.HOUR, -1);

        Calendar oneHourLater = Calendar.getInstance();
        oneHourLater.setTime(dueDateCal.getTime());
        oneHourLater.add(Calendar.HOUR, 1);

        assertThat(taskService.createTaskQuery().processInstanceId(processInstance.getId()).or().taskId("invalid").taskDueAfter(oneHourAgo.getTime()).count())
                .isEqualTo(1);
        assertThat(taskService.createTaskQuery().processInstanceId(processInstance.getId()).or().taskId("invalid").taskDueAfter(oneHourLater.getTime()).count())
                .isZero();

        // Update due-date to null, shouldn't show up anymore in query that
        // matched before
        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        task.setDueDate(null);
        taskService.saveTask(task);

        assertThat(taskService.createTaskQuery().processInstanceId(processInstance.getId()).or().taskId("invalid").taskDueAfter(oneHourLater.getTime()).count())
                .isZero();
        assertThat(taskService.createTaskQuery().processInstanceId(processInstance.getId()).or().taskId("invalid").taskDueAfter(oneHourAgo.getTime()).count())
                .isZero();
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/task/TaskQueryTest.testProcessDefinition.bpmn20.xml" })
    public void testTaskWithoutDueDate() throws Exception {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
        org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).withoutTaskDueDate().singleResult();

        // Set due-date on task
        Date dueDate = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss").parse("01/02/2003 01:12:13");
        task.setDueDate(dueDate);
        taskService.saveTask(task);

        assertThat(taskService.createTaskQuery().processInstanceId(processInstance.getId()).withoutTaskDueDate().count()).isZero();

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();

        // Clear due-date on task
        task.setDueDate(null);
        taskService.saveTask(task);

        assertThat(taskService.createTaskQuery().processInstanceId(processInstance.getId()).withoutTaskDueDate().count()).isEqualTo(1);
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/task/TaskQueryTest.testProcessDefinition.bpmn20.xml" })
    public void testTaskWithoutDueDateOr() throws Exception {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
        org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).or().taskId("invalid").withoutTaskDueDate().singleResult();

        // Set due-date on task
        Date dueDate = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss").parse("01/02/2003 01:12:13");
        task.setDueDate(dueDate);
        taskService.saveTask(task);

        assertThat(taskService.createTaskQuery().processInstanceId(processInstance.getId()).or().taskId("invalid").withoutTaskDueDate().count()).isZero();

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();

        // Clear due-date on task
        task.setDueDate(null);
        taskService.saveTask(task);

        assertThat(taskService.createTaskQuery().processInstanceId(processInstance.getId()).or().taskId("invalid").withoutTaskDueDate().count()).isEqualTo(1);
    }

    @Test
    public void testQueryPaging() {
        TaskQuery query = taskService.createTaskQuery().taskCandidateUser("kermit");

        assertThat(query.listPage(0, Integer.MAX_VALUE)).hasSize(11);

        // Verifying the un-paged results
        assertThat(query.count()).isEqualTo(11);
        assertThat(query.list()).hasSize(11);

        // Verifying paged results
        assertThat(query.listPage(0, 2)).hasSize(2);
        assertThat(query.listPage(2, 2)).hasSize(2);
        assertThat(query.listPage(4, 3)).hasSize(3);
        assertThat(query.listPage(10, 3)).hasSize(1);
        assertThat(query.listPage(10, 1)).hasSize(1);

        // Verifying odd usages
        assertThat(query.listPage(0, 0)).isEmpty();
        assertThat(query.listPage(11, 2)).isEmpty(); // 10 is the last index
        // with a result
        assertThat(query.listPage(0, 15)).hasSize(11); // there are only 11
        // tasks
    }

    @Test
    public void testQuerySorting() {
        assertThat(taskService.createTaskQuery().orderByTaskId().asc().list()).hasSize(12);
        assertThat(taskService.createTaskQuery().orderByTaskName().asc().list()).hasSize(12);
        assertThat(taskService.createTaskQuery().orderByTaskPriority().asc().list()).hasSize(12);
        assertThat(taskService.createTaskQuery().orderByTaskAssignee().asc().list()).hasSize(12);
        assertThat(taskService.createTaskQuery().orderByTaskDescription().asc().list()).hasSize(12);
        assertThat(taskService.createTaskQuery().orderByProcessInstanceId().asc().list()).hasSize(12);
        assertThat(taskService.createTaskQuery().orderByExecutionId().asc().list()).hasSize(12);
        assertThat(taskService.createTaskQuery().orderByTaskCreateTime().asc().list()).hasSize(12);
        assertThat(taskService.createTaskQuery().orderByTaskDueDate().asc().list()).hasSize(12);
        assertThat(taskService.createTaskQuery().orderByCategory().asc().list()).hasSize(12);

        assertThat(taskService.createTaskQuery().orderByTaskId().desc().list()).hasSize(12);
        assertThat(taskService.createTaskQuery().orderByTaskName().desc().list()).hasSize(12);
        assertThat(taskService.createTaskQuery().orderByTaskPriority().desc().list()).hasSize(12);
        assertThat(taskService.createTaskQuery().orderByTaskAssignee().desc().list()).hasSize(12);
        assertThat(taskService.createTaskQuery().orderByTaskDescription().desc().list()).hasSize(12);
        assertThat(taskService.createTaskQuery().orderByProcessInstanceId().desc().list()).hasSize(12);
        assertThat(taskService.createTaskQuery().orderByExecutionId().desc().list()).hasSize(12);
        assertThat(taskService.createTaskQuery().orderByTaskCreateTime().desc().list()).hasSize(12);
        assertThat(taskService.createTaskQuery().orderByTaskDueDate().desc().list()).hasSize(12);
        assertThat(taskService.createTaskQuery().orderByCategory().desc().list()).hasSize(12);

        assertThat(taskService.createTaskQuery().orderByTaskId().taskName("testTask").asc().list()).hasSize(6);
        assertThat(taskService.createTaskQuery().orderByTaskId().taskName("testTask").desc().list()).hasSize(6);
    }

    @Test
    public void testNativeQueryPaging() {
        assertThat(managementService.getTableName(org.flowable.task.api.Task.class, false)).isEqualTo("ACT_RU_TASK");
        assertThat(managementService.getTableName(TaskEntity.class, false)).isEqualTo("ACT_RU_TASK");
        assertThat(taskService.createNativeTaskQuery().sql("SELECT * FROM " + managementService.getTableName(org.flowable.task.api.Task.class)).listPage(0, 5))
                .hasSize(5);
        assertThat(
                taskService.createNativeTaskQuery().sql("SELECT * FROM " + managementService.getTableName(org.flowable.task.api.Task.class)).listPage(10, 12))
                .hasSize(2);
    }

    @Test
    public void testNativeQuery() {
        assertThat(managementService.getTableName(org.flowable.task.api.Task.class, false)).isEqualTo("ACT_RU_TASK");
        assertThat(managementService.getTableName(TaskEntity.class, false)).isEqualTo("ACT_RU_TASK");
        assertThat(taskService.createNativeTaskQuery().sql("SELECT * FROM " + managementService.getTableName(org.flowable.task.api.Task.class)).list())
                .hasSize(12);
        assertThat(taskService.createNativeTaskQuery().sql("SELECT count(*) FROM " + managementService.getTableName(org.flowable.task.api.Task.class)).count())
                .isEqualTo(12);

        assertThat(taskService.createNativeTaskQuery().sql("SELECT count(*) FROM "
                + managementService.getTableName(Task.class) + " T1, " + managementService.getTableName(Task.class) + " T2").count()).isEqualTo(144);

        // join task and variable instances
        assertThat(taskService.createNativeTaskQuery()
                .sql("SELECT count(*) FROM " + managementService.getTableName(org.flowable.task.api.Task.class) + " T1, " + managementService
                        .getTableName(VariableInstanceEntity.class) + " V1 WHERE V1.TASK_ID_ = T1.ID_")
                .count()).isEqualTo(1);
        List<org.flowable.task.api.Task> tasks = taskService.createNativeTaskQuery()
                .sql("SELECT * FROM " + managementService.getTableName(org.flowable.task.api.Task.class) + " T1, " + managementService
                        .getTableName(VariableInstanceEntity.class) + " V1 WHERE V1.TASK_ID_ = T1.ID_").list();
        assertThat(tasks)
                .extracting(Task::getName)
                .containsExactly("gonzoTask");

        // select with distinct
        assertThat(taskService.createNativeTaskQuery().sql("SELECT DISTINCT T1.* FROM " + managementService.getTableName(Task.class) + " T1").list())
                .hasSize(12);

        assertThat(taskService.createNativeTaskQuery()
                .sql("SELECT count(*) FROM " + managementService.getTableName(org.flowable.task.api.Task.class) + " T WHERE T.NAME_ = 'gonzoTask'").count())
                .isEqualTo(1);
        assertThat(taskService.createNativeTaskQuery()
                .sql("SELECT * FROM " + managementService.getTableName(org.flowable.task.api.Task.class) + " T WHERE T.NAME_ = 'gonzoTask'").list()).hasSize(1);

        // use parameters
        assertThat(taskService.createNativeTaskQuery().sql("SELECT count(*) FROM " + managementService.getTableName(org.flowable.task.api.Task.class)
                + " T WHERE T.NAME_ = #{taskName}").parameter("taskName", "gonzoTask").count()).isEqualTo(1);
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/task/TaskQueryTest.testProcessDefinition.bpmn20.xml" })
    public void testIncludeIdentityLinks() throws Exception {
        // Start process with a binary variable
        ProcessInstance processInstance = runtimeService
                .startProcessInstanceByKey("oneTaskProcess", Collections.singletonMap("binaryVariable", (Object) "It is I, le binary".getBytes()));
        org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).isNotNull();
        taskService.setVariableLocal(task.getId(), "binaryTaskVariable", (Object) "It is I, le binary".getBytes());

        taskService.addCandidateGroup(task.getId(), "group1");

        // Query task, including identity links
        task = taskService.createTaskQuery().taskId(task.getId()).includeIdentityLinks().singleResult();
        assertThat(task).isNotNull();
        assertThat(task.getIdentityLinks()).hasSize(1);

        // Query task, including identity links, process variables, and task variables
        task = taskService.createTaskQuery().taskId(task.getId()).includeIdentityLinks().includeProcessVariables().includeTaskLocalVariables().singleResult();
        assertThat(task).isNotNull();
        assertThat(task.getIdentityLinks()).hasSize(1);
        IdentityLinkInfo identityLink = task.getIdentityLinks().get(0);
        assertThat(identityLink.getProcessInstanceId()).isNull();
        assertThat(identityLink.getType()).isEqualTo("candidate");
        assertThat(identityLink.getGroupId()).isEqualTo("group1");
        assertThat(identityLink.getUserId()).isNull();
        assertThat(identityLink.getTaskId()).isEqualTo(task.getId());

        assertThat(task.getProcessVariables()).isNotNull();
        byte[] bytes = (byte[]) task.getProcessVariables().get("binaryVariable");
        assertThat(new String(bytes)).isEqualTo("It is I, le binary");

        assertThat(task.getTaskLocalVariables()).isNotNull();
        bytes = (byte[]) task.getTaskLocalVariables().get("binaryTaskVariable");
        assertThat(new String(bytes)).isEqualTo("It is I, le binary");
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/task/TaskQueryTest.testProcessDefinition.bpmn20.xml" })
    public void testIncludeIdentityLinksWithPaging() {
        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            assertThat(historyService.createHistoricTaskInstanceQuery().count()).isEqualTo(12);
        }
        
        // We don't need the existing tasks for this test
        taskService.deleteTasks(taskIds, true);
        
        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            assertThat(historyService.createHistoricTaskInstanceQuery().count()).isEqualTo(0);
        }
        
        taskIds.clear();

        for (int i = 0; i < 10; i++) {
            ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
            Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
            assertThat(task).isNotNull();

            task.setName("task" + i);
            taskService.saveTask(task);
            
            if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
                assertThat(historyService.createHistoricTaskInstanceQuery().taskId(task.getId()).singleResult()).isNotNull();
            }
            
            taskService.setPriority(task.getId(), i);
            
            taskService.addCandidateGroup(task.getId(), "group" + i);
            taskService.addCandidateGroup(task.getId(), "otherGroup" + i);
            taskService.addCandidateUser(task.getId(), "user" + i);
        }

        assertThat(taskService.createTaskQuery().count()).isEqualTo(10);
        assertThat(taskService.createTaskQuery().list()).hasSize(10);

        List<Task> tasks = taskService.createTaskQuery()
                .orderByTaskPriority().asc()
                .listPage(0, 4);
        assertThat(tasks)
                .extracting(TaskInfo::getName)
                .containsExactly("task0", "task1", "task2", "task3");

        tasks = taskService.createTaskQuery()
                .includeIdentityLinks()
                .orderByTaskPriority().asc()
                .listPage(0, 4);
        assertThat(tasks)
                .extracting(TaskInfo::getName)
                .containsExactly("task0", "task1", "task2", "task3");

        Task task = tasks.get(1);
        assertThat(task).isNotNull();
        assertThat(task.getIdentityLinks())
                .extracting(IdentityLinkInfo::getGroupId, IdentityLinkInfo::getUserId)
                .containsExactlyInAnyOrder(
                        tuple("group1", null),
                        tuple("otherGroup1", null),
                        tuple(null, "user1")
                );

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            assertThat(historyService.createHistoricTaskInstanceQuery().count()).isEqualTo(10);
            assertThat(historyService.createHistoricTaskInstanceQuery().list()).hasSize(10);

            List<HistoricTaskInstance> historicTasks = historyService.createHistoricTaskInstanceQuery()
                    .orderByTaskPriority().asc()
                    .listPage(0, 4);
            assertThat(historicTasks)
                    .extracting(TaskInfo::getName)
                    .containsExactly("task0", "task1", "task2", "task3");

            historicTasks = historyService.createHistoricTaskInstanceQuery()
                    .includeIdentityLinks()
                    .orderByTaskPriority().asc()
                    .listPage(0, 4);
            assertThat(historicTasks)
                    .extracting(TaskInfo::getName)
                    .containsExactly("task0", "task1", "task2", "task3");

            HistoricTaskInstance historicTask = historicTasks.get(1);
            assertThat(historicTask).isNotNull();
            assertThat(historicTask.getIdentityLinks())
                    .extracting(IdentityLinkInfo::getGroupId, IdentityLinkInfo::getUserId)
                    .containsExactlyInAnyOrder(
                            tuple("group1", null),
                            tuple("otherGroup1", null),
                            tuple(null, "user1")
                    );
        }
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/task/TaskQueryTest.testProcessDefinition.bpmn20.xml" })
    public void testIncludeProcessVariablesWithPaging() {
        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            assertThat(historyService.createHistoricTaskInstanceQuery().count()).isEqualTo(12);
        }
        
        // We don't need the existing tasks for this test
        taskService.deleteTasks(taskIds, true);
        
        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            assertThat(historyService.createHistoricTaskInstanceQuery().count()).isEqualTo(0);
        }
        
        taskIds.clear();

        for (int i = 0; i < 10; i++) {
            ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess", Collections.singletonMap("processVar", "value" + i));
            Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
            assertThat(task).isNotNull();

            task.setName("task" + i);
            taskService.saveTask(task);
            
            if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
                assertThat(historyService.createHistoricTaskInstanceQuery().taskId(task.getId()).singleResult()).isNotNull();
            }
            
            taskService.setPriority(task.getId(), i);
        }

        assertThat(taskService.createTaskQuery().count()).isEqualTo(10);
        assertThat(taskService.createTaskQuery().list()).hasSize(10);

        List<Task> tasks = taskService.createTaskQuery()
                .orderByTaskPriority().asc()
                .listPage(0, 4);
        assertThat(tasks)
                .extracting(TaskInfo::getName)
                .containsExactly("task0", "task1", "task2", "task3");

        tasks = taskService.createTaskQuery()
                .includeProcessVariables()
                .orderByTaskPriority().asc()
                .listPage(0, 4);
        assertThat(tasks)
                .extracting(TaskInfo::getName)
                .containsExactly("task0", "task1", "task2", "task3");

        Task task = tasks.get(1);
        assertThat(task).isNotNull();
        assertThat(task.getProcessVariables())
                .containsOnly(entry("processVar", "value1"));

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            assertThat(historyService.createHistoricTaskInstanceQuery().count()).isEqualTo(10);
            assertThat(historyService.createHistoricTaskInstanceQuery().list()).hasSize(10);

            List<HistoricTaskInstance> historicTasks = historyService.createHistoricTaskInstanceQuery()
                    .orderByTaskPriority().asc()
                    .listPage(0, 4);
            assertThat(historicTasks)
                    .extracting(TaskInfo::getName)
                    .containsExactly("task0", "task1", "task2", "task3");

            historicTasks = historyService.createHistoricTaskInstanceQuery()
                    .includeProcessVariables()
                    .orderByTaskPriority().asc()
                    .listPage(0, 4);
            assertThat(historicTasks)
                    .extracting(TaskInfo::getName)
                    .containsExactly("task0", "task1", "task2", "task3");

            HistoricTaskInstance historicTask = historicTasks.get(1);
            assertThat(historicTask).isNotNull();
            assertThat(historicTask.getProcessVariables())
                    .containsOnly(entry("processVar", "value1"));
        }
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/task/TaskQueryTest.testProcessDefinition.bpmn20.xml" })
    public void testIncludeProcessVariablesAndTaskLocalVariablesWithPaging() {
        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            assertThat(historyService.createHistoricTaskInstanceQuery().count()).isEqualTo(12);
        }
        
        // We don't need the existing tasks for this test
        taskService.deleteTasks(taskIds, true);
        
        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            assertThat(historyService.createHistoricTaskInstanceQuery().count()).isEqualTo(0);
        }
        
        taskIds.clear();

        for (int i = 0; i < 10; i++) {
            ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess", Collections.singletonMap("processVar", "value" + i));
            Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
            assertThat(task).isNotNull();

            task.setName("task" + i);
            taskService.saveTask(task);
            
            if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
                assertThat(historyService.createHistoricTaskInstanceQuery().taskId(task.getId()).singleResult()).isNotNull();
            }
            
            taskService.setPriority(task.getId(), i);
            taskService.setVariableLocal(task.getId(), "taskVar", "taskValue" + i);
        }

        assertThat(taskService.createTaskQuery().count()).isEqualTo(10);
        assertThat(taskService.createTaskQuery().list()).hasSize(10);

        List<Task> tasks = taskService.createTaskQuery()
                .orderByTaskPriority().asc()
                .listPage(0, 4);
        assertThat(tasks)
                .extracting(TaskInfo::getName)
                .containsExactly("task0", "task1", "task2", "task3");

        tasks = taskService.createTaskQuery()
                .includeProcessVariables()
                .orderByTaskPriority().asc()
                .listPage(0, 4);
        assertThat(tasks)
                .extracting(TaskInfo::getName)
                .containsExactly("task0", "task1", "task2", "task3");

        Task task = tasks.get(1);
        assertThat(task).isNotNull();
        assertThat(task.getProcessVariables())
                .containsOnly(entry("processVar", "value1"));
        assertThat(task.getTaskLocalVariables()).isEmpty();

        tasks = taskService.createTaskQuery()
                .includeProcessVariables()
                .includeTaskLocalVariables()
                .orderByTaskPriority().asc()
                .listPage(0, 4);
        assertThat(tasks)
                .extracting(TaskInfo::getName)
                .containsExactly("task0", "task1", "task2", "task3");

        task = tasks.get(1);
        assertThat(task).isNotNull();
        assertThat(task.getProcessVariables())
                .containsOnly(entry("processVar", "value1"));
        assertThat(task.getTaskLocalVariables())
                .containsOnly(entry("taskVar", "taskValue1"));

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            assertThat(historyService.createHistoricTaskInstanceQuery().count()).isEqualTo(10);
            assertThat(historyService.createHistoricTaskInstanceQuery().list()).hasSize(10);

            List<HistoricTaskInstance> historicTasks = historyService.createHistoricTaskInstanceQuery()
                    .orderByTaskPriority().asc()
                    .listPage(0, 4);
            assertThat(historicTasks)
                    .extracting(TaskInfo::getName)
                    .containsExactly("task0", "task1", "task2", "task3");

            historicTasks = historyService.createHistoricTaskInstanceQuery()
                    .includeProcessVariables()
                    .orderByTaskPriority().asc()
                    .listPage(0, 4);
            assertThat(historicTasks)
                    .extracting(TaskInfo::getName)
                    .containsExactly("task0", "task1", "task2", "task3");

            HistoricTaskInstance historicTask = historicTasks.get(1);
            assertThat(historicTask).isNotNull();
            assertThat(historicTask.getProcessVariables())
                    .containsOnly(entry("processVar", "value1"));
            assertThat(historicTask.getTaskLocalVariables()).isEmpty();

            historicTasks = historyService.createHistoricTaskInstanceQuery()
                    .includeProcessVariables()
                    .includeTaskLocalVariables()
                    .orderByTaskPriority().asc()
                    .listPage(0, 4);
            assertThat(historicTasks)
                    .extracting(TaskInfo::getName)
                    .containsExactly("task0", "task1", "task2", "task3");

            historicTask = historicTasks.get(1);
            assertThat(historicTask).isNotNull();
            assertThat(historicTask.getProcessVariables())
                    .containsOnly(entry("processVar", "value1"));
            assertThat(historicTask.getTaskLocalVariables())
                    .containsOnly(entry("taskVar", "taskValue1"));
        }
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/task/TaskQueryTest.testProcessDefinition.bpmn20.xml" })
    public void testIncludeProcessVariablesAndTaskLocalVariablesAndIncludeIdentityLinksWithPaging() {
        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            assertThat(historyService.createHistoricTaskInstanceQuery().count()).isEqualTo(12);
        }
        
        // We don't need the existing tasks for this test
        taskService.deleteTasks(taskIds, true);
        
        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            assertThat(historyService.createHistoricTaskInstanceQuery().count()).isEqualTo(0);
        }
        
        taskIds.clear();

        for (int i = 0; i < 10; i++) {
            ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess", Collections.singletonMap("processVar", "value" + i));
            Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
            assertThat(task).isNotNull();

            task.setName("task" + i);
            taskService.saveTask(task);
            
            if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
                assertThat(historyService.createHistoricTaskInstanceQuery().taskId(task.getId()).singleResult()).isNotNull();
            }
            
            taskService.setPriority(task.getId(), i);
            
            taskService.setVariableLocal(task.getId(), "taskVar", "taskValue" + i);
            taskService.addCandidateGroup(task.getId(), "group" + i);
            taskService.addCandidateGroup(task.getId(), "otherGroup" + i);
            taskService.addCandidateUser(task.getId(), "user" + i);
        }

        assertThat(taskService.createTaskQuery().count()).isEqualTo(10);
        assertThat(taskService.createTaskQuery().list()).hasSize(10);

        List<Task> tasks = taskService.createTaskQuery()
                .orderByTaskPriority().asc()
                .listPage(0, 4);
        assertThat(tasks)
                .extracting(TaskInfo::getName)
                .containsExactly("task0", "task1", "task2", "task3");

        tasks = taskService.createTaskQuery()
                .includeProcessVariables()
                .orderByTaskPriority().asc()
                .listPage(0, 4);
        assertThat(tasks)
                .extracting(TaskInfo::getName)
                .containsExactly("task0", "task1", "task2", "task3");

        Task task = tasks.get(1);
        assertThat(task).isNotNull();
        assertThat(task.getProcessVariables())
                .containsOnly(entry("processVar", "value1"));
        assertThat(task.getTaskLocalVariables()).isEmpty();
        assertThat(task.getIdentityLinks()).isEmpty();

        tasks = taskService.createTaskQuery()
                .includeProcessVariables()
                .includeTaskLocalVariables()
                .orderByTaskPriority().asc()
                .listPage(0, 4);
        assertThat(tasks)
                .extracting(TaskInfo::getName)
                .containsExactly("task0", "task1", "task2", "task3");

        task = tasks.get(1);
        assertThat(task).isNotNull();
        assertThat(task.getProcessVariables())
                .containsOnly(entry("processVar", "value1"));
        assertThat(task.getTaskLocalVariables())
                .containsOnly(entry("taskVar", "taskValue1"));
        assertThat(task.getIdentityLinks()).isEmpty();

        tasks = taskService.createTaskQuery()
                .includeProcessVariables()
                .includeTaskLocalVariables()
                .includeIdentityLinks()
                .orderByTaskPriority().asc()
                .listPage(0, 4);
        assertThat(tasks)
                .extracting(TaskInfo::getName)
                .containsExactly("task0", "task1", "task2", "task3");

        task = tasks.get(1);
        assertThat(task).isNotNull();
        assertThat(task.getProcessVariables())
                .containsOnly(entry("processVar", "value1"));
        assertThat(task.getTaskLocalVariables())
                .containsOnly(entry("taskVar", "taskValue1"));
        assertThat(task.getIdentityLinks())
                .extracting(IdentityLinkInfo::getGroupId, IdentityLinkInfo::getUserId)
                .containsExactlyInAnyOrder(
                        tuple("group1", null),
                        tuple("otherGroup1", null),
                        tuple(null, "user1")
                );

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            assertThat(historyService.createHistoricTaskInstanceQuery().count()).isEqualTo(10);
            assertThat(historyService.createHistoricTaskInstanceQuery().list()).hasSize(10);

            List<HistoricTaskInstance> historicTasks = historyService.createHistoricTaskInstanceQuery()
                    .orderByTaskPriority().asc()
                    .listPage(0, 4);
            assertThat(historicTasks)
                    .extracting(TaskInfo::getName)
                    .containsExactly("task0", "task1", "task2", "task3");

            historicTasks = historyService.createHistoricTaskInstanceQuery()
                    .includeProcessVariables()
                    .orderByTaskPriority().asc()
                    .listPage(0, 4);
            assertThat(historicTasks)
                    .extracting(TaskInfo::getName)
                    .containsExactly("task0", "task1", "task2", "task3");

            HistoricTaskInstance historicTask = historicTasks.get(1);
            assertThat(historicTask).isNotNull();
            assertThat(historicTask.getProcessVariables())
                    .containsOnly(entry("processVar", "value1"));
            assertThat(historicTask.getTaskLocalVariables()).isEmpty();
            assertThat(historicTask.getIdentityLinks()).isEmpty();

            historicTasks = historyService.createHistoricTaskInstanceQuery()
                    .includeProcessVariables()
                    .includeTaskLocalVariables()
                    .orderByTaskPriority().asc()
                    .listPage(0, 4);
            assertThat(historicTasks)
                    .extracting(TaskInfo::getName)
                    .containsExactly("task0", "task1", "task2", "task3");

            historicTask = historicTasks.get(1);
            assertThat(historicTask).isNotNull();
            assertThat(historicTask.getProcessVariables())
                    .containsOnly(entry("processVar", "value1"));
            assertThat(historicTask.getTaskLocalVariables())
                    .containsOnly(entry("taskVar", "taskValue1"));
            assertThat(historicTask.getIdentityLinks()).isEmpty();

            historicTasks = historyService.createHistoricTaskInstanceQuery()
                    .includeProcessVariables()
                    .includeTaskLocalVariables()
                    .includeIdentityLinks()
                    .orderByTaskPriority().asc()
                    .listPage(0, 4);
            assertThat(historicTasks)
                    .extracting(TaskInfo::getName)
                    .containsExactly("task0", "task1", "task2", "task3");

            historicTask = historicTasks.get(1);
            assertThat(historicTask).isNotNull();
            assertThat(historicTask.getProcessVariables())
                    .containsOnly(entry("processVar", "value1"));
            assertThat(historicTask.getTaskLocalVariables())
                    .containsOnly(entry("taskVar", "taskValue1"));
            assertThat(historicTask.getIdentityLinks())
                    .extracting(IdentityLinkInfo::getGroupId, IdentityLinkInfo::getUserId)
                    .containsExactlyInAnyOrder(
                            tuple("group1", null),
                            tuple("otherGroup1", null),
                            tuple(null, "user1")
                    );
        }
    }

    /**
     * Test confirming fix for ACT-1731
     */
    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/task/TaskQueryTest.testProcessDefinition.bpmn20.xml" })
    public void testIncludeBinaryVariables() throws Exception {
        // Start process with a binary variable
        ProcessInstance processInstance = runtimeService
                .startProcessInstanceByKey("oneTaskProcess", Collections.singletonMap("binaryVariable", (Object) "It is I, le binary".getBytes()));
        org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).isNotNull();
        taskService.setVariableLocal(task.getId(), "binaryTaskVariable", (Object) "It is I, le binary".getBytes());

        // Query task, including processVariables
        task = taskService.createTaskQuery().taskId(task.getId()).includeProcessVariables().singleResult();
        assertThat(task).isNotNull();
        assertThat(task.getProcessVariables()).isNotNull();
        byte[] bytes = (byte[]) task.getProcessVariables().get("binaryVariable");
        assertThat(new String(bytes)).isEqualTo("It is I, le binary");

        // Query task, including taskVariables
        task = taskService.createTaskQuery().taskId(task.getId()).includeTaskLocalVariables().singleResult();
        assertThat(task).isNotNull();
        assertThat(task.getTaskLocalVariables()).isNotNull();
        bytes = (byte[]) task.getTaskLocalVariables().get("binaryTaskVariable");
        assertThat(new String(bytes)).isEqualTo("It is I, le binary");
    }

    /**
     * Test confirming fix for ACT-1731
     */
    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/task/TaskQueryTest.testProcessDefinition.bpmn20.xml" })
    public void testIncludeBinaryVariablesOr() throws Exception {
        // Start process with a binary variable
        ProcessInstance processInstance = runtimeService
                .startProcessInstanceByKey("oneTaskProcess", Collections.singletonMap("binaryVariable", (Object) "It is I, le binary".getBytes()));
        org.flowable.task.api.Task task = taskService.createTaskQuery().or().taskName("invalid").processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).isNotNull();
        taskService.setVariableLocal(task.getId(), "binaryTaskVariable", (Object) "It is I, le binary".getBytes());

        // Query task, including processVariables
        task = taskService.createTaskQuery().or().taskName("invalid").taskId(task.getId()).includeProcessVariables().singleResult();
        assertThat(task).isNotNull();
        assertThat(task.getProcessVariables()).isNotNull();
        byte[] bytes = (byte[]) task.getProcessVariables().get("binaryVariable");
        assertThat(new String(bytes)).isEqualTo("It is I, le binary");

        // Query task, including taskVariables
        task = taskService.createTaskQuery().or().taskName("invalid").taskId(task.getId()).includeTaskLocalVariables().singleResult();
        assertThat(task).isNotNull();
        assertThat(task.getTaskLocalVariables()).isNotNull();
        bytes = (byte[]) task.getTaskLocalVariables().get("binaryTaskVariable");
        assertThat(new String(bytes)).isEqualTo("It is I, le binary");
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/task/TaskQueryTest.testProcessDefinition.bpmn20.xml" })
    public void testQueryByDeploymentId() throws Exception {
        org.flowable.engine.repository.Deployment deployment = repositoryService.createDeploymentQuery().singleResult();
        runtimeService.startProcessInstanceByKey("oneTaskProcess");
        assertThat(taskService.createTaskQuery().deploymentId(deployment.getId()).singleResult()).isNotNull();
        assertThat(taskService.createTaskQuery().deploymentId(deployment.getId()).count()).isEqualTo(1);
        assertThat(taskService.createTaskQuery().deploymentId("invalid").singleResult()).isNull();
        assertThat(taskService.createTaskQuery().deploymentId("invalid").count()).isZero();
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/task/TaskQueryTest.testProcessDefinition.bpmn20.xml" })
    public void testQueryByDeploymentIdOr() throws Exception {
        org.flowable.engine.repository.Deployment deployment = repositoryService.createDeploymentQuery().singleResult();
        runtimeService.startProcessInstanceByKey("oneTaskProcess");
        assertThat(taskService.createTaskQuery().or().taskId("invalid").deploymentId(deployment.getId()).singleResult()).isNotNull();
        assertThat(taskService.createTaskQuery().or().taskId("invalid").deploymentId(deployment.getId()).count()).isEqualTo(1);
        assertThat(taskService.createTaskQuery().deploymentId("invalid").singleResult()).isNull();
        assertThat(taskService.createTaskQuery().or().taskId("invalid").deploymentId("invalid").count()).isZero();
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/task/TaskQueryTest.testProcessDefinition.bpmn20.xml" })
    public void testQueryByDeploymentIdIn() throws Exception {
        org.flowable.engine.repository.Deployment deployment = repositoryService.createDeploymentQuery().singleResult();
        runtimeService.startProcessInstanceByKey("oneTaskProcess");
        List<String> deploymentIds = new ArrayList<>();
        deploymentIds.add(deployment.getId());
        assertThat(taskService.createTaskQuery().deploymentIdIn(deploymentIds).singleResult()).isNotNull();
        assertThat(taskService.createTaskQuery().deploymentIdIn(deploymentIds).count()).isEqualTo(1);

        deploymentIds.add("invalid");
        assertThat(taskService.createTaskQuery().deploymentIdIn(deploymentIds).singleResult()).isNotNull();
        assertThat(taskService.createTaskQuery().deploymentIdIn(deploymentIds).count()).isEqualTo(1);

        deploymentIds = new ArrayList<>();
        deploymentIds.add("invalid");
        assertThat(taskService.createTaskQuery().deploymentIdIn(deploymentIds).singleResult()).isNull();
        assertThat(taskService.createTaskQuery().deploymentIdIn(deploymentIds).count()).isZero();
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/task/TaskQueryTest.testProcessDefinition.bpmn20.xml" })
    public void testQueryByDeploymentIdInOr() throws Exception {
        org.flowable.engine.repository.Deployment deployment = repositoryService.createDeploymentQuery().singleResult();
        runtimeService.startProcessInstanceByKey("oneTaskProcess");
        List<String> deploymentIds = new ArrayList<>();
        deploymentIds.add(deployment.getId());
        assertThat(taskService.createTaskQuery().or().taskId("invalid").deploymentIdIn(deploymentIds).singleResult()).isNotNull();

        assertThat(taskService.createTaskQuery().or().taskId("invalid").deploymentIdIn(deploymentIds).count()).isEqualTo(1);

        deploymentIds.add("invalid");
        assertThat(taskService.createTaskQuery().or().taskId("invalid").deploymentIdIn(deploymentIds).singleResult()).isNotNull();

        assertThat(taskService.createTaskQuery().or().taskId("invalid").deploymentIdIn(deploymentIds).count()).isEqualTo(1);

        deploymentIds = new ArrayList<>();
        deploymentIds.add("invalid");
        assertThat(taskService.createTaskQuery().deploymentIdIn(deploymentIds).singleResult()).isNull();
        assertThat(taskService.createTaskQuery().or().taskId("invalid").deploymentIdIn(deploymentIds).count()).isZero();
    }
    
    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/task/TaskQueryTest.testProcessDefinition.bpmn20.xml" })
    public void testWithoutScopeId() throws Exception {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");

        assertThat(taskService.createTaskQuery().withoutScopeId().count()).isEqualTo(13);
        assertThat(taskService.createTaskQuery().processInstanceId(processInstance.getId()).withoutScopeId().count()).isEqualTo(1);
    }

    @Test
    public void testQueryByTaskNameLikeIgnoreCase() {

        // Runtime
        assertThat(taskService.createTaskQuery().taskNameLikeIgnoreCase("%task%").count()).isEqualTo(12);
        assertThat(taskService.createTaskQuery().taskNameLikeIgnoreCase("%Task%").count()).isEqualTo(12);
        assertThat(taskService.createTaskQuery().taskNameLikeIgnoreCase("%TASK%").count()).isEqualTo(12);
        assertThat(taskService.createTaskQuery().taskNameLikeIgnoreCase("%TasK%").count()).isEqualTo(12);
        assertThat(taskService.createTaskQuery().taskNameLikeIgnoreCase("gonzo%").count()).isEqualTo(1);
        assertThat(taskService.createTaskQuery().taskNameLikeIgnoreCase("%Gonzo%").count()).isEqualTo(1);
        assertThat(taskService.createTaskQuery().taskNameLikeIgnoreCase("Task%").count()).isZero();

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
            // History
            assertThat(historyService.createHistoricTaskInstanceQuery().taskNameLikeIgnoreCase("%task%").count()).isEqualTo(12);
            assertThat(historyService.createHistoricTaskInstanceQuery().taskNameLikeIgnoreCase("%Task%").count()).isEqualTo(12);
            assertThat(historyService.createHistoricTaskInstanceQuery().taskNameLikeIgnoreCase("%TASK%").count()).isEqualTo(12);
            assertThat(historyService.createHistoricTaskInstanceQuery().taskNameLikeIgnoreCase("%TasK%").count()).isEqualTo(12);
            assertThat(historyService.createHistoricTaskInstanceQuery().taskNameLikeIgnoreCase("gonzo%").count()).isEqualTo(1);
            assertThat(historyService.createHistoricTaskInstanceQuery().taskNameLikeIgnoreCase("%Gonzo%").count()).isEqualTo(1);
            assertThat(historyService.createHistoricTaskInstanceQuery().taskNameLikeIgnoreCase("Task%").count()).isZero();
        }
    }

    @Test
    public void testQueryByTaskNameOrDescriptionLikeIgnoreCase() {

        // Runtime
        assertThat(taskService.createTaskQuery().or().taskNameLikeIgnoreCase("%task%").taskDescriptionLikeIgnoreCase("%task%").endOr().count()).isEqualTo(12);

        assertThat(taskService.createTaskQuery().or().taskNameLikeIgnoreCase("ACCOUN%").taskDescriptionLikeIgnoreCase("%ESCR%").endOr().count()).isEqualTo(9);

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
            // History
            assertThat(historyService.createHistoricTaskInstanceQuery().or().taskNameLikeIgnoreCase("%task%").taskDescriptionLikeIgnoreCase("%task%").endOr()
                    .count()).isEqualTo(12);

            assertThat(historyService.createHistoricTaskInstanceQuery().or().taskNameLikeIgnoreCase("ACCOUN%").taskDescriptionLikeIgnoreCase("%ESCR%").endOr()
                    .count()).isEqualTo(9);
        }

    }

    @Test
    public void testQueryByTaskDescriptionLikeIgnoreCase() {

        // Runtime
        assertThat(taskService.createTaskQuery().taskDescriptionLikeIgnoreCase("%task%").count()).isEqualTo(6);
        assertThat(taskService.createTaskQuery().taskDescriptionLikeIgnoreCase("%Task%").count()).isEqualTo(6);
        assertThat(taskService.createTaskQuery().taskDescriptionLikeIgnoreCase("%TASK%").count()).isEqualTo(6);
        assertThat(taskService.createTaskQuery().taskDescriptionLikeIgnoreCase("%TaSk%").count()).isEqualTo(6);
        assertThat(taskService.createTaskQuery().taskDescriptionLikeIgnoreCase("task%").count()).isZero();
        assertThat(taskService.createTaskQuery().taskDescriptionLikeIgnoreCase("gonzo%").count()).isEqualTo(1);
        assertThat(taskService.createTaskQuery().taskDescriptionLikeIgnoreCase("Gonzo%").count()).isEqualTo(1);
        assertThat(taskService.createTaskQuery().taskDescriptionLikeIgnoreCase("%manage%").count()).isZero();

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
            // History
            assertThat(historyService.createHistoricTaskInstanceQuery().taskDescriptionLikeIgnoreCase("%task%").count()).isEqualTo(6);
            assertThat(historyService.createHistoricTaskInstanceQuery().taskDescriptionLikeIgnoreCase("%Task%").count()).isEqualTo(6);
            assertThat(historyService.createHistoricTaskInstanceQuery().taskDescriptionLikeIgnoreCase("%TASK%").count()).isEqualTo(6);
            assertThat(historyService.createHistoricTaskInstanceQuery().taskDescriptionLikeIgnoreCase("%TaSk%").count()).isEqualTo(6);
            assertThat(historyService.createHistoricTaskInstanceQuery().taskDescriptionLikeIgnoreCase("task%").count()).isZero();
            assertThat(historyService.createHistoricTaskInstanceQuery().taskDescriptionLikeIgnoreCase("gonzo%").count()).isEqualTo(1);
            assertThat(historyService.createHistoricTaskInstanceQuery().taskDescriptionLikeIgnoreCase("Gonzo%").count()).isEqualTo(1);
            assertThat(historyService.createHistoricTaskInstanceQuery().taskDescriptionLikeIgnoreCase("%manage%").count()).isZero();
        }
    }

    @Test
    public void testQueryByAssigneeLikeIgnoreCase() {

        // Runtime
        assertThat(taskService.createTaskQuery().taskAssigneeLikeIgnoreCase("%gonzo%").count()).isEqualTo(1);
        assertThat(taskService.createTaskQuery().taskAssigneeLikeIgnoreCase("%GONZO%").count()).isEqualTo(1);
        assertThat(taskService.createTaskQuery().taskAssigneeLikeIgnoreCase("%Gon%").count()).isEqualTo(1);
        assertThat(taskService.createTaskQuery().taskAssigneeLikeIgnoreCase("gon%").count()).isEqualTo(1);
        assertThat(taskService.createTaskQuery().taskAssigneeLikeIgnoreCase("%nzo%").count()).isEqualTo(1);
        assertThat(taskService.createTaskQuery().taskAssigneeLikeIgnoreCase("%doesnotexist%").count()).isZero();

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
            // History
            assertThat(historyService.createHistoricTaskInstanceQuery().taskAssigneeLikeIgnoreCase("%gonzo%").count()).isEqualTo(1);
            assertThat(historyService.createHistoricTaskInstanceQuery().taskAssigneeLikeIgnoreCase("%GONZO%").count()).isEqualTo(1);
            assertThat(historyService.createHistoricTaskInstanceQuery().taskAssigneeLikeIgnoreCase("%Gon%").count()).isEqualTo(1);
            assertThat(historyService.createHistoricTaskInstanceQuery().taskAssigneeLikeIgnoreCase("gon%").count()).isEqualTo(1);
            assertThat(historyService.createHistoricTaskInstanceQuery().taskAssigneeLikeIgnoreCase("%nzo%").count()).isEqualTo(1);
            assertThat(historyService.createHistoricTaskInstanceQuery().taskAssigneeLikeIgnoreCase("%doesnotexist%").count()).isZero();
        }
    }

    @Test
    public void testQueryByOwnerLikeIgnoreCase() {

        // Runtime
        assertThat(taskService.createTaskQuery().taskOwnerLikeIgnoreCase("%gonzo%").count()).isEqualTo(6);
        assertThat(taskService.createTaskQuery().taskOwnerLikeIgnoreCase("%GONZO%").count()).isEqualTo(6);
        assertThat(taskService.createTaskQuery().taskOwnerLikeIgnoreCase("%Gon%").count()).isEqualTo(6);
        assertThat(taskService.createTaskQuery().taskOwnerLikeIgnoreCase("gon%").count()).isEqualTo(6);
        assertThat(taskService.createTaskQuery().taskOwnerLikeIgnoreCase("%nzo%").count()).isEqualTo(6);
        assertThat(taskService.createTaskQuery().taskOwnerLikeIgnoreCase("%doesnotexist%").count()).isZero();

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
            // History
            assertThat(historyService.createHistoricTaskInstanceQuery().taskOwnerLikeIgnoreCase("%gonzo%").count()).isEqualTo(6);
            assertThat(historyService.createHistoricTaskInstanceQuery().taskOwnerLikeIgnoreCase("%GONZO%").count()).isEqualTo(6);
            assertThat(historyService.createHistoricTaskInstanceQuery().taskOwnerLikeIgnoreCase("%Gon%").count()).isEqualTo(6);
            assertThat(historyService.createHistoricTaskInstanceQuery().taskOwnerLikeIgnoreCase("gon%").count()).isEqualTo(6);
            assertThat(historyService.createHistoricTaskInstanceQuery().taskOwnerLikeIgnoreCase("%nzo%").count()).isEqualTo(6);
            assertThat(historyService.createHistoricTaskInstanceQuery().taskOwnerLikeIgnoreCase("%doesnotexist%").count()).isZero();
        }
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/task/TaskQueryTest.testProcessDefinition.bpmn20.xml" })
    public void testQueryByBusinessKeyLikeIgnoreCase() {
        runtimeService.startProcessInstanceByKey("oneTaskProcess", "BUSINESS-KEY-1");
        runtimeService.startProcessInstanceByKey("oneTaskProcess", "Business-Key-2");
        runtimeService.startProcessInstanceByKey("oneTaskProcess", "KeY-3");

        // Runtime
        assertThat(taskService.createTaskQuery().processInstanceBusinessKeyLikeIgnoreCase("%key%").count()).isEqualTo(3);
        assertThat(taskService.createTaskQuery().processInstanceBusinessKeyLikeIgnoreCase("%KEY%").count()).isEqualTo(3);
        assertThat(taskService.createTaskQuery().processInstanceBusinessKeyLikeIgnoreCase("%EY%").count()).isEqualTo(3);
        assertThat(taskService.createTaskQuery().processInstanceBusinessKeyLikeIgnoreCase("%business%").count()).isEqualTo(2);
        assertThat(taskService.createTaskQuery().processInstanceBusinessKeyLikeIgnoreCase("business%").count()).isEqualTo(2);
        assertThat(taskService.createTaskQuery().processInstanceBusinessKeyLikeIgnoreCase("%doesnotexist%").count()).isZero();

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
            // History
            assertThat(historyService.createHistoricTaskInstanceQuery().processInstanceBusinessKeyLikeIgnoreCase("%key%").count()).isEqualTo(3);
            assertThat(historyService.createHistoricTaskInstanceQuery().processInstanceBusinessKeyLikeIgnoreCase("%KEY%").count()).isEqualTo(3);
            assertThat(historyService.createHistoricTaskInstanceQuery().processInstanceBusinessKeyLikeIgnoreCase("%EY%").count()).isEqualTo(3);
            assertThat(historyService.createHistoricTaskInstanceQuery().processInstanceBusinessKeyLikeIgnoreCase("%business%").count()).isEqualTo(2);
            assertThat(historyService.createHistoricTaskInstanceQuery().processInstanceBusinessKeyLikeIgnoreCase("business%").count()).isEqualTo(2);
            assertThat(historyService.createHistoricTaskInstanceQuery().processInstanceBusinessKeyLikeIgnoreCase("%doesnotexist%").count()).isZero();
        }
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/task/TaskQueryTest.testProcessDefinition.bpmn20.xml" })
    public void testQueryByProcessDefinitionKeyLikeIgnoreCase() {

        // Runtime
        runtimeService.startProcessInstanceByKey("oneTaskProcess");
        runtimeService.startProcessInstanceByKey("oneTaskProcess");
        runtimeService.startProcessInstanceByKey("oneTaskProcess");
        runtimeService.startProcessInstanceByKey("oneTaskProcess");

        assertThat(taskService.createTaskQuery().processDefinitionKeyLikeIgnoreCase("%one%").count()).isEqualTo(4);
        assertThat(taskService.createTaskQuery().processDefinitionKeyLikeIgnoreCase("%ONE%").count()).isEqualTo(4);
        assertThat(taskService.createTaskQuery().processDefinitionKeyLikeIgnoreCase("ON%").count()).isEqualTo(4);
        assertThat(taskService.createTaskQuery().processDefinitionKeyLikeIgnoreCase("%fake%").count()).isZero();

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
            // History
            assertThat(historyService.createHistoricTaskInstanceQuery().processDefinitionKeyLikeIgnoreCase("%one%").count()).isEqualTo(4);
            assertThat(historyService.createHistoricTaskInstanceQuery().processDefinitionKeyLikeIgnoreCase("%ONE%").count()).isEqualTo(4);
            assertThat(historyService.createHistoricTaskInstanceQuery().processDefinitionKeyLikeIgnoreCase("ON%").count()).isEqualTo(4);
            assertThat(historyService.createHistoricTaskInstanceQuery().processDefinitionKeyLikeIgnoreCase("%fake%").count()).isZero();
        }
    }

    @Test
    public void testCombinationOfOrAndLikeIgnoreCase() {

        // Runtime
        assertThat(
                taskService.createTaskQuery().or().taskNameLikeIgnoreCase("%task%").taskDescriptionLikeIgnoreCase("%desc%").taskAssigneeLikeIgnoreCase("Gonz%")
                        .taskOwnerLike("G%").endOr()
                        .count()).isEqualTo(12);

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
            // History
            assertThat(historyService.createHistoricTaskInstanceQuery().or().taskNameLikeIgnoreCase("%task%").taskDescriptionLikeIgnoreCase("%desc%")
                    .taskAssigneeLikeIgnoreCase("Gonz%")
                    .taskOwnerLike("G%").endOr().count()).isEqualTo(12);
        }
    }

    // Test for https://jira.codehaus.org/browse/ACT-2103
    @Test
    public void testTaskLocalAndProcessInstanceVariableEqualsInOr() {

        deployOneTaskTestProcess();
        for (int i = 0; i < 10; i++) {
            runtimeService.startProcessInstanceByKey("oneTaskProcess");
        }

        List<org.flowable.task.api.Task> allTasks = taskService.createTaskQuery().processDefinitionKey("oneTaskProcess").list();
        assertThat(allTasks).hasSize(10);

        // Give two tasks a task local variable
        taskService.setVariableLocal(allTasks.get(0).getId(), "localVar", "someValue");
        taskService.setVariableLocal(allTasks.get(1).getId(), "localVar", "someValue");

        // Give three tasks a proc inst var
        runtimeService.setVariable(allTasks.get(2).getProcessInstanceId(), "var", "theValue");
        runtimeService.setVariable(allTasks.get(3).getProcessInstanceId(), "var", "theValue");
        runtimeService.setVariable(allTasks.get(4).getProcessInstanceId(), "var", "theValue");

        assertThat(taskService.createTaskQuery().taskVariableValueEquals("localVar", "someValue").list()).hasSize(2);
        assertThat(taskService.createTaskQuery().processVariableValueEquals("var", "theValue").list()).hasSize(3);

        assertThat(taskService.createTaskQuery().or()
                .taskVariableValueEquals("localVar", "someValue")
                .processVariableValueEquals("var", "theValue")
                .endOr().list()).hasSize(5);

        assertThat(taskService.createTaskQuery()
                .or()
                .taskVariableValueEquals("localVar", "someValue")
                .processVariableValueEquals("var", "theValue")
                .endOr()
                .or()
                .processDefinitionKey("oneTaskProcess")
                .processDefinitionId("notexisting")
                .endOr()
                .list()).hasSize(5);
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml" },
        tenantId = "testTenant"
    )
    public void testIncludeTaskLocalAndProcessInstanceVariableHasTenant() {
        for (int i = 0; i < 10; i++) {
            runtimeService.startProcessInstanceByKeyAndTenantId("oneTaskProcess", Collections.singletonMap("simpleVar", "simpleVarValue"), "testTenant");
        }

        List<Task> tasks = taskService.createTaskQuery().processDefinitionKey("oneTaskProcess").
            includeProcessVariables().includeTaskLocalVariables()
            .list();

        assertThat(tasks).hasSize(10);
        for (Task task : tasks) {
            assertThat(task.getTenantId()).isEqualTo("testTenant");
        }
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/task/TaskQueryTest.testProcessDefinition.bpmn20.xml" })
    public void testLocalizeTasks() throws Exception {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");

        List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().processDefinitionId(processInstance.getProcessDefinitionId()).list();
        assertThat(tasks)
                .extracting(Task::getName, Task::getDescription)
                .containsExactly(tuple("my task", "My Task Description"));

        tasks = taskService.createTaskQuery().processDefinitionId(processInstance.getProcessDefinitionId()).locale("es").list();
        assertThat(tasks)
                .extracting(Task::getName, Task::getDescription)
                .containsExactly(tuple("Mi Tarea", "Mi Tarea Descripción"));

        tasks = taskService.createTaskQuery().processDefinitionId(processInstance.getProcessDefinitionId()).locale("it").list();
        assertThat(tasks)
                .extracting(Task::getName, Task::getDescription)
                .containsExactly(tuple("Il mio compito", "Il mio compito Descrizione"));

        ObjectNode infoNode = dynamicBpmnService.getProcessDefinitionInfo(processInstance.getProcessDefinitionId());

        dynamicBpmnService.changeLocalizationName("en-GB", "theTask", "My 'en-GB' localized name", infoNode);
        dynamicBpmnService.changeLocalizationDescription("en-GB", "theTask", "My 'en-GB' localized description", infoNode);
        dynamicBpmnService.saveProcessDefinitionInfo(processInstance.getProcessDefinitionId(), infoNode);

        dynamicBpmnService.changeLocalizationName("en", "theTask", "My 'en' localized name", infoNode);
        dynamicBpmnService.changeLocalizationDescription("en", "theTask", "My 'en' localized description", infoNode);
        dynamicBpmnService.saveProcessDefinitionInfo(processInstance.getProcessDefinitionId(), infoNode);

        tasks = taskService.createTaskQuery().processDefinitionId(processInstance.getProcessDefinitionId()).list();
        assertThat(tasks)
                .extracting(Task::getName, Task::getDescription)
                .containsExactly(tuple("my task", "My Task Description"));

        tasks = taskService.createTaskQuery().processDefinitionId(processInstance.getProcessDefinitionId()).locale("es").list();
        assertThat(tasks)
                .extracting(Task::getName, Task::getDescription)
                .containsExactly(tuple("Mi Tarea", "Mi Tarea Descripción"));

        tasks = taskService.createTaskQuery().processDefinitionId(processInstance.getProcessDefinitionId()).locale("it").list();
        assertThat(tasks)
                .extracting(Task::getName, Task::getDescription)
                .containsExactly(tuple("Il mio compito", "Il mio compito Descrizione"));

        tasks = taskService.createTaskQuery().processDefinitionId(processInstance.getProcessDefinitionId()).locale("en-GB").list();
        assertThat(tasks)
                .extracting(Task::getName, Task::getDescription)
                .containsExactly(tuple("My 'en-GB' localized name", "My 'en-GB' localized description"));

        tasks = taskService.createTaskQuery().processDefinitionId(processInstance.getProcessDefinitionId()).listPage(0, 10);
        assertThat(tasks)
                .extracting(Task::getName, Task::getDescription)
                .containsExactly(tuple("my task", "My Task Description"));

        tasks = taskService.createTaskQuery().processDefinitionId(processInstance.getProcessDefinitionId()).locale("es").listPage(0, 10);
        assertThat(tasks)
                .extracting(Task::getName, Task::getDescription)
                .containsExactly(tuple("Mi Tarea", "Mi Tarea Descripción"));

        tasks = taskService.createTaskQuery().processDefinitionId(processInstance.getProcessDefinitionId()).locale("it").listPage(0, 10);
        assertThat(tasks)
                .extracting(Task::getName, Task::getDescription)
                .containsExactly(tuple("Il mio compito", "Il mio compito Descrizione"));

        tasks = taskService.createTaskQuery().processDefinitionId(processInstance.getProcessDefinitionId()).locale("en-GB").listPage(0, 10);
        assertThat(tasks)
                .extracting(Task::getName, Task::getDescription)
                .containsExactly(tuple("My 'en-GB' localized name", "My 'en-GB' localized description"));

        org.flowable.task.api.Task task = taskService.createTaskQuery().processDefinitionId(processInstance.getProcessDefinitionId()).singleResult();
        assertThat(task.getName()).isEqualTo("my task");
        assertThat(task.getDescription()).isEqualTo("My Task Description");

        task = taskService.createTaskQuery().processDefinitionId(processInstance.getProcessDefinitionId()).locale("es").singleResult();
        assertThat(task.getName()).isEqualTo("Mi Tarea");
        assertThat(task.getDescription()).isEqualTo("Mi Tarea Descripción");

        task = taskService.createTaskQuery().processDefinitionId(processInstance.getProcessDefinitionId()).locale("it").singleResult();
        assertThat(task.getName()).isEqualTo("Il mio compito");
        assertThat(task.getDescription()).isEqualTo("Il mio compito Descrizione");

        task = taskService.createTaskQuery().processDefinitionId(processInstance.getProcessDefinitionId()).locale("en-GB").singleResult();
        assertThat(task.getName()).isEqualTo("My 'en-GB' localized name");
        assertThat(task.getDescription()).isEqualTo("My 'en-GB' localized description");

        task = taskService.createTaskQuery().processDefinitionId(processInstance.getProcessDefinitionId()).singleResult();
        assertThat(task.getName()).isEqualTo("my task");
        assertThat(task.getDescription()).isEqualTo("My Task Description");

        task = taskService.createTaskQuery().processDefinitionId(processInstance.getProcessDefinitionId()).locale("en").singleResult();
        assertThat(task.getName()).isEqualTo("My 'en' localized name");
        assertThat(task.getDescription()).isEqualTo("My 'en' localized description");

        task = taskService.createTaskQuery().processDefinitionId(processInstance.getProcessDefinitionId()).locale("en-AU").withLocalizationFallback()
                .singleResult();
        assertThat(task.getName()).isEqualTo("My 'en' localized name");
        assertThat(task.getDescription()).isEqualTo("My 'en' localized description");
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/task/TaskQueryTest.testProcessDefinition.bpmn20.xml" })
    public void testNullHandlingOrder() {
        ProcessInstance firstProcessInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
        ProcessInstance secondProcessInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");

        Task firstTask = taskService.createTaskQuery().processInstanceId(firstProcessInstance.getId()).singleResult();
        Task secondTask = taskService.createTaskQuery().processInstanceId(secondProcessInstance.getId()).singleResult();

        taskService.setDueDate(secondTask.getId(), new Date());

        List<Task> tasks = taskService.createTaskQuery()
                .processDefinitionKey("oneTaskProcess")
                .orderByDueDateNullsLast()
                .asc()
                .listPage(0, 10);

        // The order has to be exactly like defined, since we are testing the nulls last functionality
        assertThat(tasks)
                .extracting(Task::getId)
                .containsExactly(secondTask.getId(), firstTask.getId());

        tasks = taskService.createTaskQuery()
                .processDefinitionKey("oneTaskProcess")
                .orderByDueDateNullsFirst()
                .asc()
                .listPage(0, 10);

        // The order has to be exactly like defined, since we are testing the nulls last functionality
        assertThat(tasks)
                .extracting(Task::getId)
                .containsExactly(firstTask.getId(), secondTask.getId());

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            List<HistoricTaskInstance> historicTasks = historyService.createHistoricTaskInstanceQuery()
                    .processDefinitionKey("oneTaskProcess")
                    .orderByDueDateNullsLast()
                    .asc()
                    .listPage(0, 10);

            // The order has to be exactly like defined, since we are testing the nulls last functionality
            assertThat(historicTasks)
                    .extracting(HistoricTaskInstance::getId)
                    .containsExactly(secondTask.getId(), firstTask.getId());

            historicTasks = historyService.createHistoricTaskInstanceQuery()
                    .processDefinitionKey("oneTaskProcess")
                    .orderByDueDateNullsFirst()
                    .asc()
                    .listPage(0, 10);

            // The order has to be exactly like defined, since we are testing the nulls last functionality
            assertThat(historicTasks)
                    .extracting(HistoricTaskInstance::getId)
                    .containsExactly(firstTask.getId(), secondTask.getId());
        }
    }

    @Test
    public void testQueryVariableValueEqualsAndNotEquals() {
        Task taskWithStringValue = taskService.createTaskBuilder()
                .name("With string value")
                .create();
        taskIds.add(taskWithStringValue.getId());
        taskService.setVariable(taskWithStringValue.getId(), "var", "TEST");

        Task taskWithNullValue = taskService.createTaskBuilder()
                .name("With null value")
                .create();
        taskIds.add(taskWithNullValue.getId());
        taskService.setVariable(taskWithNullValue.getId(), "var", null);

        Task taskWithLongValue = taskService.createTaskBuilder()
                .name("With long value")
                .create();
        taskIds.add(taskWithLongValue.getId());
        taskService.setVariable(taskWithLongValue.getId(), "var", 100L);

        Task taskWithDoubleValue = taskService.createTaskBuilder()
                .name("With double value")
                .create();
        taskIds.add(taskWithDoubleValue.getId());
        taskService.setVariable(taskWithDoubleValue.getId(), "var", 45.55);

        assertThat(taskService.createTaskQuery().taskVariableValueNotEquals("var", "TEST").list())
                .extracting(Task::getName, Task::getId)
                .containsExactlyInAnyOrder(
                        tuple("With null value", taskWithNullValue.getId()),
                        tuple("With long value", taskWithLongValue.getId()),
                        tuple("With double value", taskWithDoubleValue.getId())
                );

        assertThat(taskService.createTaskQuery().taskVariableValueEquals("var", "TEST").list())
                .extracting(Task::getName, Task::getId)
                .containsExactlyInAnyOrder(
                        tuple("With string value", taskWithStringValue.getId())
                );

        assertThat(taskService.createTaskQuery().taskVariableValueNotEquals("var", 100L).list())
                .extracting(Task::getName, Task::getId)
                .containsExactlyInAnyOrder(
                        tuple("With string value", taskWithStringValue.getId()),
                        tuple("With null value", taskWithNullValue.getId()),
                        tuple("With double value", taskWithDoubleValue.getId())
                );

        assertThat(taskService.createTaskQuery().taskVariableValueEquals("var", 100L).list())
                .extracting(Task::getName, Task::getId)
                .containsExactlyInAnyOrder(
                        tuple("With long value", taskWithLongValue.getId())
                );

        assertThat(taskService.createTaskQuery().taskVariableValueNotEquals("var", 45.55).list())
                .extracting(Task::getName, Task::getId)
                .containsExactlyInAnyOrder(
                        tuple("With string value", taskWithStringValue.getId()),
                        tuple("With null value", taskWithNullValue.getId()),
                        tuple("With long value", taskWithLongValue.getId())
                );

        assertThat(taskService.createTaskQuery().taskVariableValueEquals("var", 45.55).list())
                .extracting(Task::getName, Task::getId)
                .containsExactlyInAnyOrder(
                        tuple("With double value", taskWithDoubleValue.getId())
                );

        assertThat(taskService.createTaskQuery().taskVariableValueNotEquals("var", "test").list())
                .extracting(Task::getName, Task::getId)
                .containsExactlyInAnyOrder(
                        tuple("With string value", taskWithStringValue.getId()),
                        tuple("With null value", taskWithNullValue.getId()),
                        tuple("With long value", taskWithLongValue.getId()),
                        tuple("With double value", taskWithDoubleValue.getId())
                );

        assertThat(taskService.createTaskQuery().taskVariableValueNotEqualsIgnoreCase("var", "test").list())
                .extracting(Task::getName, Task::getId)
                .containsExactlyInAnyOrder(
                        tuple("With null value", taskWithNullValue.getId()),
                        tuple("With long value", taskWithLongValue.getId()),
                        tuple("With double value", taskWithDoubleValue.getId())
                );

        assertThat(taskService.createTaskQuery().taskVariableValueEquals("var", "test").list())
                .extracting(Task::getName, Task::getId)
                .isEmpty();

        assertThat(taskService.createTaskQuery().taskVariableValueEqualsIgnoreCase("var", "test").list())
                .extracting(Task::getName, Task::getId)
                .containsExactlyInAnyOrder(
                        tuple("With string value", taskWithStringValue.getId())
                );
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml" })
    public void testQueryProcessVariableValueEqualsAndNotEquals() {
        ProcessInstance processWithStringValue = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("oneTaskProcess")
                .name("With string value")
                .variable("var", "TEST")
                .start();

        ProcessInstance processWithNullValue = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("oneTaskProcess")
                .name("With null value")
                .variable("var", null)
                .start();

        ProcessInstance processWithLongValue = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("oneTaskProcess")
                .name("With long value")
                .variable("var", 100L)
                .start();

        ProcessInstance processWithDoubleValue = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("oneTaskProcess")
                .name("With double value")
                .variable("var", 45.55)
                .start();

        assertThat(taskService.createTaskQuery().processVariableValueNotEquals("var", "TEST").list())
                .extracting(Task::getName, Task::getProcessInstanceId)
                .containsExactlyInAnyOrder(
                        tuple("my task", processWithNullValue.getId()),
                        tuple("my task", processWithLongValue.getId()),
                        tuple("my task", processWithDoubleValue.getId())
                );

        assertThat(taskService.createTaskQuery().processVariableValueEquals("var", "TEST").list())
                .extracting(Task::getName, Task::getProcessInstanceId)
                .containsExactlyInAnyOrder(
                        tuple("my task", processWithStringValue.getId())
                );

        assertThat(taskService.createTaskQuery().processVariableValueNotEquals("var", 100L).list())
                .extracting(Task::getName, Task::getProcessInstanceId)
                .containsExactlyInAnyOrder(
                        tuple("my task", processWithStringValue.getId()),
                        tuple("my task", processWithNullValue.getId()),
                        tuple("my task", processWithDoubleValue.getId())
                );

        assertThat(taskService.createTaskQuery().processVariableValueEquals("var", 100L).list())
                .extracting(Task::getName, Task::getProcessInstanceId)
                .containsExactlyInAnyOrder(
                        tuple("my task", processWithLongValue.getId())
                );

        assertThat(taskService.createTaskQuery().processVariableValueNotEquals("var", 45.55).list())
                .extracting(Task::getName, Task::getProcessInstanceId)
                .containsExactlyInAnyOrder(
                        tuple("my task", processWithStringValue.getId()),
                        tuple("my task", processWithNullValue.getId()),
                        tuple("my task", processWithLongValue.getId())
                );

        assertThat(taskService.createTaskQuery().processVariableValueEquals("var", 45.55).list())
                .extracting(Task::getName, Task::getProcessInstanceId)
                .containsExactlyInAnyOrder(
                        tuple("my task", processWithDoubleValue.getId())
                );

        assertThat(taskService.createTaskQuery().processVariableValueNotEquals("var", "test").list())
                .extracting(Task::getName, Task::getProcessInstanceId)
                .containsExactlyInAnyOrder(
                        tuple("my task", processWithStringValue.getId()),
                        tuple("my task", processWithNullValue.getId()),
                        tuple("my task", processWithLongValue.getId()),
                        tuple("my task", processWithDoubleValue.getId())
                );

        assertThat(taskService.createTaskQuery().processVariableValueNotEqualsIgnoreCase("var", "test").list())
                .extracting(Task::getName, Task::getProcessInstanceId)
                .containsExactlyInAnyOrder(
                        tuple("my task", processWithNullValue.getId()),
                        tuple("my task", processWithLongValue.getId()),
                        tuple("my task", processWithDoubleValue.getId())
                );

        assertThat(taskService.createTaskQuery().processVariableValueEquals("var", "test").list())
                .extracting(Task::getName, Task::getProcessInstanceId)
                .isEmpty();

        assertThat(taskService.createTaskQuery().processVariableValueEqualsIgnoreCase("var", "test").list())
                .extracting(Task::getName, Task::getProcessInstanceId)
                .containsExactlyInAnyOrder(
                        tuple("my task", processWithStringValue.getId())
                );
    }

    /**
     * Generates some test tasks. - 6 tasks where kermit is a candidate - 1 tasks where gonzo is assignee - 2 tasks assigned to management group - 2 tasks assigned to accountancy group - 1 task
     * assigned to both the management and accountancy group
     */
    private List<String> generateTestTasks() throws Exception {
        List<String> ids = new ArrayList<>();

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss.SSS");
        // 6 tasks for kermit
        processEngineConfiguration.getClock().setCurrentTime(sdf.parse("01/01/2001 01:01:01.000"));
        for (int i = 0; i < 6; i++) {
            org.flowable.task.api.Task task = taskService.newTask();
            task.setName("testTask");
            task.setDescription("testTask description");
            task.setOwner("gonzo");
            task.setPriority(3);
            taskService.saveTask(task);
            ids.add(task.getId());
            taskService.addCandidateUser(task.getId(), "kermit");
        }

        processEngineConfiguration.getClock().setCurrentTime(sdf.parse("02/02/2002 02:02:02.000"));
        // 1 task for gonzo
        org.flowable.task.api.Task task = taskService.newTask();
        task.setName("gonzoTask");
        task.setDescription("gonzo description");
        task.setPriority(4);
        taskService.saveTask(task);
        taskService.setAssignee(task.getId(), "gonzo");
        taskService.setVariable(task.getId(), "testVar", "someVariable");
        ids.add(task.getId());

        processEngineConfiguration.getClock().setCurrentTime(sdf.parse("03/03/2003 03:03:03.000"));
        // 2 tasks for management group
        for (int i = 0; i < 2; i++) {
            task = taskService.newTask();
            task.setName("managementTask");
            task.setPriority(10);
            taskService.saveTask(task);
            taskService.addCandidateGroup(task.getId(), "management");
            ids.add(task.getId());
        }

        processEngineConfiguration.getClock().setCurrentTime(sdf.parse("04/04/2004 04:04:04.000"));
        // 2 tasks for accountancy group
        for (int i = 0; i < 2; i++) {
            task = taskService.newTask();
            task.setName("accountancyTask");
            task.setDescription("accountancy description");
            taskService.saveTask(task);
            taskService.addCandidateGroup(task.getId(), "accountancy");
            ids.add(task.getId());
        }

        processEngineConfiguration.getClock().setCurrentTime(sdf.parse("05/05/2005 05:05:05.000"));
        // 1 task assigned to management and accountancy group
        task = taskService.newTask();
        task.setName("managementAndAccountancyTask");
        taskService.saveTask(task);
        taskService.addCandidateGroup(task.getId(), "management");
        taskService.addCandidateGroup(task.getId(), "accountancy");
        ids.add(task.getId());

        return ids;
    }

}
