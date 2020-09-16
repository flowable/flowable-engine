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
package org.flowable.standalone.cfg;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.impl.test.ResourceFlowableTestCase;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.task.Attachment;
import org.junit.jupiter.api.Test;

/**
 * @author Bassam Al-Sarori
 */
public class CustomMybatisXMLMapperTest extends ResourceFlowableTestCase {

    public CustomMybatisXMLMapperTest() {
        super("org/flowable/standalone/cfg/custom-mybatis-xml-mappers-flowable.cfg.xml");
    }

    @Test
    public void testSelectOneTask() {
        // Create test data
        for (int i = 0; i < 4; i++) {
            createTask(String.valueOf(i), null, null, 0);
        }

        final String taskId = createTask("4", null, null, 0);

        CustomTask customTask = managementService.executeCommand(new Command<CustomTask>() {

            @Override
            public CustomTask execute(CommandContext commandContext) {
                return (CustomTask) CommandContextUtil.getDbSqlSession(commandContext).selectOne("selectOneCustomTask", taskId);
            }
        });

        assertThat(customTask.getName()).isEqualTo("4");

        // test default query as well
        List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().list();
        assertThat(tasks).hasSize(5);

        org.flowable.task.api.Task task = taskService.createTaskQuery().taskName("2").singleResult();
        assertThat(task.getName()).isEqualTo("2");

        // Cleanup
        deleteTasks(taskService.createTaskQuery().list());
    }

    @Test
    public void testSelectTaskList() {
        // Create test data
        for (int i = 0; i < 5; i++) {
            createTask(String.valueOf(i), null, null, 0);
        }

        List<CustomTask> tasks = managementService.executeCommand(new Command<List<CustomTask>>() {

            @SuppressWarnings("unchecked")
            @Override
            public List<CustomTask> execute(CommandContext commandContext) {
                return (List<CustomTask>) CommandContextUtil.getDbSqlSession(commandContext).selectList("selectCustomTaskList");
            }
        });

        assertThat(tasks).hasSize(5);

        // Cleanup
        deleteCustomTasks(tasks);
    }

    @Test
    public void testSelectTasksByCustomQuery() {
        // Create test data
        for (int i = 0; i < 5; i++) {
            createTask(String.valueOf(i), null, null, 0);
        }
        createTask("Owned task", "kermit", null, 0);

        List<CustomTask> tasks = new CustomTaskQuery(processEngineConfiguration.getCommandExecutor()).unOwned().list();

        assertThat(tasks).hasSize(5);
        assertThat(new CustomTaskQuery(processEngineConfiguration.getCommandExecutor()).unOwned().count()).isEqualTo(5);

        tasks = new CustomTaskQuery(processEngineConfiguration.getCommandExecutor()).list();

        // Cleanup
        deleteCustomTasks(tasks);
    }

    @Test
    public void testSelectTaskByCustomQuery() {
        // Create test data
        for (int i = 0; i < 5; i++) {
            createTask(String.valueOf(i), null, null, 0);
        }
        createTask("Owned task", "kermit", null, 0);

        CustomTask task = new CustomTaskQuery(processEngineConfiguration.getCommandExecutor()).taskOwner("kermit").singleResult();

        assertThat(task.getOwner()).isEqualTo("kermit");

        List<CustomTask> tasks = new CustomTaskQuery(processEngineConfiguration.getCommandExecutor()).list();
        // Cleanup
        deleteCustomTasks(tasks);
    }

    @Test
    public void testCustomQueryListPage() {
        // Create test data
        for (int i = 0; i < 15; i++) {
            createTask(String.valueOf(i), null, null, 0);
        }

        List<CustomTask> tasks = new CustomTaskQuery(processEngineConfiguration.getCommandExecutor()).listPage(0, 10);

        assertThat(tasks).hasSize(10);

        tasks = new CustomTaskQuery(processEngineConfiguration.getCommandExecutor()).list();

        // Cleanup
        deleteCustomTasks(tasks);
    }

    @Test
    public void testCustomQueryOrderBy() {
        // Create test data
        for (int i = 0; i < 5; i++) {
            createTask(String.valueOf(i), null, null, i * 20);
        }

        List<CustomTask> tasks = new CustomTaskQuery(processEngineConfiguration.getCommandExecutor()).orderByTaskPriority().desc().list();

        assertThat(tasks).hasSize(5);

        for (int i = 0, j = 4; i < 5; i++, j--) {
            CustomTask task = tasks.get(i);
            assertThat(task.getPriority()).isEqualTo(j * 20);
        }

        tasks = new CustomTaskQuery(processEngineConfiguration.getCommandExecutor()).orderByTaskPriority().asc().list();

        assertThat(tasks).hasSize(5);

        for (int i = 0; i < 5; i++) {
            CustomTask task = tasks.get(i);
            assertThat(task.getPriority()).isEqualTo(i * 20);
        }
        // Cleanup
        deleteCustomTasks(tasks);
    }

    @Test
    public void testAttachmentQuery() {
        String taskId = createTask("task1", null, null, 0);

        identityService.setAuthenticatedUserId("kermit");

        String attachmentId = taskService.createAttachment("image/png", taskId, null, "attachment1", "", "http://activiti.org/").getId();
        taskService.createAttachment("image/jpeg", taskId, null, "attachment2", "Attachment Description", "http://activiti.org/");

        identityService.setAuthenticatedUserId("gonzo");

        taskService.createAttachment("image/png", taskId, null, "zattachment3", "Attachment Description", "http://activiti.org/");

        identityService.setAuthenticatedUserId("fozzie");

        for (int i = 0; i < 15; i++) {
            taskService.createAttachment(null, createTask(String.valueOf(i), null, null, 0), null, "attachmentName" + i, "", "http://activiti.org/" + i);
        }

        assertThat(new AttachmentQuery(processEngineConfiguration.getCommandExecutor()).attachmentId(attachmentId).singleResult().getId())
                .isEqualTo(attachmentId);

        assertThat(new AttachmentQuery(processEngineConfiguration.getCommandExecutor()).attachmentName("attachment1").singleResult().getName())
                .isEqualTo("attachment1");

        assertThat(new AttachmentQuery(processEngineConfiguration.getCommandExecutor()).count()).isEqualTo(18);
        List<Attachment> attachments = new AttachmentQuery(processEngineConfiguration.getCommandExecutor()).list();
        assertThat(attachments).hasSize(18);

        attachments = new AttachmentQuery(processEngineConfiguration.getCommandExecutor()).listPage(0, 10);
        assertThat(attachments).hasSize(10);

        assertThat(new AttachmentQuery(processEngineConfiguration.getCommandExecutor()).taskId(taskId).count()).isEqualTo(3);
        attachments = new AttachmentQuery(processEngineConfiguration.getCommandExecutor()).taskId(taskId).list();
        assertThat(attachments).hasSize(3);

        assertThat(new AttachmentQuery(processEngineConfiguration.getCommandExecutor()).userId("kermit").count()).isEqualTo(2);
        attachments = new AttachmentQuery(processEngineConfiguration.getCommandExecutor()).userId("kermit").list();
        assertThat(attachments).hasSize(2);

        assertThat(new AttachmentQuery(processEngineConfiguration.getCommandExecutor()).attachmentType("image/jpeg").count()).isEqualTo(1);
        attachments = new AttachmentQuery(processEngineConfiguration.getCommandExecutor()).attachmentType("image/jpeg").list();
        assertThat(attachments).hasSize(1);

        assertThat(new AttachmentQuery(processEngineConfiguration.getCommandExecutor()).orderByAttachmentName().desc().list().get(0).getName())
                .isEqualTo("zattachment3");

        // Cleanup
        deleteTasks(taskService.createTaskQuery().list());
    }

    protected String createTask(String name, String owner, String assignee, int priority) {
        org.flowable.task.api.Task task = taskService.newTask();
        task.setName(name);
        task.setOwner(owner);
        task.setAssignee(assignee);
        task.setPriority(priority);
        taskService.saveTask(task);
        return task.getId();
    }

    protected void deleteTask(String taskId) {
        taskService.deleteTask(taskId, true);
    }

    protected void deleteTasks(List<org.flowable.task.api.Task> tasks) {
        for (org.flowable.task.api.Task task : tasks)
            deleteTask(task.getId());
    }

    protected void deleteCustomTasks(List<CustomTask> tasks) {
        for (CustomTask task : tasks)
            deleteTask(task.getId());
    }
}
