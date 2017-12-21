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
package org.activiti.standalone.cfg;

import java.util.List;

import org.activiti.engine.impl.interceptor.Command;
import org.activiti.engine.impl.interceptor.CommandContext;
import org.activiti.engine.impl.test.ResourceFlowableTestCase;
import org.activiti.engine.task.Attachment;

/**
 * @author Bassam Al-Sarori
 */
public class CustomMybatisXMLMapperTest extends ResourceFlowableTestCase {

    public CustomMybatisXMLMapperTest() {
        super("org/activiti/standalone/cfg/custom-mybatis-xml-mappers-flowable.cfg.xml");
    }

    public void testSelectOneTask() {
        // Create test data
        for (int i = 0; i < 4; i++) {
            createTask(String.valueOf(i), null, null, 0);
        }

        final String taskId = createTask("4", null, null, 0);

        org.activiti.engine.impl.cfg.ProcessEngineConfigurationImpl activiti5ProcessEngineConfig = (org.activiti.engine.impl.cfg.ProcessEngineConfigurationImpl) processEngineConfiguration.getFlowable5CompatibilityHandler()
                .getRawProcessConfiguration();

        CustomTask customTask = activiti5ProcessEngineConfig.getManagementService().executeCommand(new Command<CustomTask>() {
            @Override
            public CustomTask execute(CommandContext commandContext) {
                return (CustomTask) commandContext.getDbSqlSession().selectOne("selectOneCustomTask", taskId);
            }
        });

        assertEquals("4", customTask.getName());

        // test default query as well
        List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().list();
        assertEquals(5, tasks.size());

        org.flowable.task.api.Task task = taskService.createTaskQuery().taskName("2").singleResult();
        assertEquals("2", task.getName());

        // Cleanup
        deleteTasks(taskService.createTaskQuery().list());
    }

    public void testSelectTaskList() {
        // Create test data
        for (int i = 0; i < 5; i++) {
            createTask(String.valueOf(i), null, null, 0);
        }

        org.activiti.engine.impl.cfg.ProcessEngineConfigurationImpl activiti5ProcessEngineConfig = (org.activiti.engine.impl.cfg.ProcessEngineConfigurationImpl) processEngineConfiguration.getFlowable5CompatibilityHandler()
                .getRawProcessConfiguration();

        List<CustomTask> tasks = activiti5ProcessEngineConfig.getManagementService().executeCommand(new Command<List<CustomTask>>() {

            @SuppressWarnings("unchecked")
            @Override
            public List<CustomTask> execute(CommandContext commandContext) {
                return (List<CustomTask>) commandContext.getDbSqlSession().selectList("selectCustomTaskList");
            }
        });

        assertEquals(5, tasks.size());

        // Cleanup
        deleteCustomTasks(tasks);
    }

    public void testSelectTasksByCustomQuery() {
        // Create test data
        for (int i = 0; i < 5; i++) {
            createTask(String.valueOf(i), null, null, 0);
        }
        createTask("Owned task", "kermit", null, 0);

        org.activiti.engine.impl.cfg.ProcessEngineConfigurationImpl activiti5ProcessEngineConfig = (org.activiti.engine.impl.cfg.ProcessEngineConfigurationImpl) processEngineConfiguration.getFlowable5CompatibilityHandler()
                .getRawProcessConfiguration();

        List<CustomTask> tasks = new CustomTaskQuery(activiti5ProcessEngineConfig.getManagementService()).unOwned().list();

        assertEquals(5, tasks.size());
        assertEquals(5, new CustomTaskQuery(activiti5ProcessEngineConfig.getManagementService()).unOwned().count());

        tasks = new CustomTaskQuery(activiti5ProcessEngineConfig.getManagementService()).list();

        // Cleanup
        deleteCustomTasks(tasks);
    }

    public void testSelectTaskByCustomQuery() {
        // Create test data
        for (int i = 0; i < 5; i++) {
            createTask(String.valueOf(i), null, null, 0);
        }
        createTask("Owned task", "kermit", null, 0);

        org.activiti.engine.impl.cfg.ProcessEngineConfigurationImpl activiti5ProcessEngineConfig = (org.activiti.engine.impl.cfg.ProcessEngineConfigurationImpl) processEngineConfiguration.getFlowable5CompatibilityHandler()
                .getRawProcessConfiguration();

        CustomTask task = new CustomTaskQuery(activiti5ProcessEngineConfig.getManagementService()).taskOwner("kermit").singleResult();

        assertEquals("kermit", task.getOwner());

        List<CustomTask> tasks = new CustomTaskQuery(activiti5ProcessEngineConfig.getManagementService()).list();
        // Cleanup
        deleteCustomTasks(tasks);
    }

    public void testCustomQueryListPage() {
        // Create test data
        for (int i = 0; i < 15; i++) {
            createTask(String.valueOf(i), null, null, 0);
        }

        org.activiti.engine.impl.cfg.ProcessEngineConfigurationImpl activiti5ProcessEngineConfig = (org.activiti.engine.impl.cfg.ProcessEngineConfigurationImpl) processEngineConfiguration.getFlowable5CompatibilityHandler()
                .getRawProcessConfiguration();

        List<CustomTask> tasks = new CustomTaskQuery(activiti5ProcessEngineConfig.getManagementService()).listPage(0, 10);

        assertEquals(10, tasks.size());

        tasks = new CustomTaskQuery(activiti5ProcessEngineConfig.getManagementService()).list();

        // Cleanup
        deleteCustomTasks(tasks);
    }

    public void testCustomQueryOrderBy() {
        // Create test data
        for (int i = 0; i < 5; i++) {
            createTask(String.valueOf(i), null, null, i * 20);
        }

        org.activiti.engine.impl.cfg.ProcessEngineConfigurationImpl activiti5ProcessEngineConfig = (org.activiti.engine.impl.cfg.ProcessEngineConfigurationImpl) processEngineConfiguration.getFlowable5CompatibilityHandler()
                .getRawProcessConfiguration();

        List<CustomTask> tasks = new CustomTaskQuery(activiti5ProcessEngineConfig.getManagementService()).orderByTaskPriority().desc().list();

        assertEquals(5, tasks.size());

        for (int i = 0, j = 4; i < 5; i++, j--) {
            CustomTask task = tasks.get(i);
            assertEquals(j * 20, task.getPriority());
        }

        tasks = new CustomTaskQuery(activiti5ProcessEngineConfig.getManagementService()).orderByTaskPriority().asc().list();

        assertEquals(5, tasks.size());

        for (int i = 0; i < 5; i++) {
            CustomTask task = tasks.get(i);
            assertEquals(i * 20, task.getPriority());
        }
        // Cleanup
        deleteCustomTasks(tasks);
    }

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

        org.activiti.engine.impl.cfg.ProcessEngineConfigurationImpl activiti5ProcessEngineConfig = (org.activiti.engine.impl.cfg.ProcessEngineConfigurationImpl) processEngineConfiguration.getFlowable5CompatibilityHandler()
                .getRawProcessConfiguration();

        assertEquals(attachmentId, new AttachmentQuery(activiti5ProcessEngineConfig.getManagementService()).attachmentId(attachmentId).singleResult().getId());

        assertEquals("attachment1", new AttachmentQuery(activiti5ProcessEngineConfig.getManagementService()).attachmentName("attachment1").singleResult().getName());

        assertEquals(18, new AttachmentQuery(activiti5ProcessEngineConfig.getManagementService()).count());
        List<Attachment> attachments = new AttachmentQuery(activiti5ProcessEngineConfig.getManagementService()).list();
        assertEquals(18, attachments.size());

        attachments = new AttachmentQuery(activiti5ProcessEngineConfig.getManagementService()).listPage(0, 10);
        assertEquals(10, attachments.size());

        assertEquals(3, new AttachmentQuery(activiti5ProcessEngineConfig.getManagementService()).taskId(taskId).count());
        attachments = new AttachmentQuery(activiti5ProcessEngineConfig.getManagementService()).taskId(taskId).list();
        assertEquals(3, attachments.size());

        assertEquals(2, new AttachmentQuery(activiti5ProcessEngineConfig.getManagementService()).userId("kermit").count());
        attachments = new AttachmentQuery(activiti5ProcessEngineConfig.getManagementService()).userId("kermit").list();
        assertEquals(2, attachments.size());

        assertEquals(1, new AttachmentQuery(activiti5ProcessEngineConfig.getManagementService()).attachmentType("image/jpeg").count());
        attachments = new AttachmentQuery(activiti5ProcessEngineConfig.getManagementService()).attachmentType("image/jpeg").list();
        assertEquals(1, attachments.size());

        assertEquals("zattachment3", new AttachmentQuery(activiti5ProcessEngineConfig.getManagementService()).orderByAttachmentName().desc().list().get(0).getName());

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
        taskService.deleteTask(taskId);
        historyService.deleteHistoricTaskInstance(taskId);
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
