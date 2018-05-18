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

package org.flowable.engine.test.bpmn.usertask;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.engine.impl.test.HistoryTestHelper;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.identitylink.api.IdentityLink;
import org.flowable.identitylink.api.IdentityLinkType;
import org.flowable.task.api.Task;
import org.flowable.task.api.history.HistoricTaskInstance;

/**
 * @author Joram Barrez
 */
public class UserTaskTest extends PluggableFlowableTestCase {

    @Deployment
    public void testTaskPropertiesNotNull() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");

        org.flowable.task.api.Task task = taskService.createTaskQuery().singleResult();
        assertNotNull(task.getId());
        assertEquals("my task", task.getName());
        assertEquals("Very important", task.getDescription());
        assertTrue(task.getPriority() > 0);
        assertEquals("kermit", task.getAssignee());
        assertEquals(processInstance.getId(), task.getProcessInstanceId());
        assertNotNull(task.getProcessDefinitionId());
        assertNotNull(task.getTaskDefinitionKey());
        assertNotNull(task.getCreateTime());
        
        // the next test verifies that if an execution creates a task, that no events are created during creation of the task.
        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            assertEquals(0, taskService.getTaskEvents(task.getId()).size());
        }
    }

    @Deployment
    public void testQuerySortingWithParameter() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
        assertEquals(1, taskService.createTaskQuery().processInstanceId(processInstance.getId()).list().size());
    }

    @Deployment
    public void testCompleteAfterParallelGateway() throws InterruptedException {
        // related to https://activiti.atlassian.net/browse/ACT-1054

        // start the process
        runtimeService.startProcessInstanceByKey("ForkProcess");
        List<org.flowable.task.api.Task> taskList = taskService.createTaskQuery().list();
        assertNotNull(taskList);
        assertEquals(2, taskList.size());

        // make sure user task exists
        org.flowable.task.api.Task task = taskService.createTaskQuery().taskDefinitionKey("SimpleUser").singleResult();
        assertNotNull(task);

        // attempt to complete the task and get PersistenceException pointing to
        // "referential integrity constraint violation"
        taskService.complete(task.getId());
    }

    @Deployment
    public void testTaskCategory() {
        runtimeService.startProcessInstanceByKey("testTaskCategory");
        org.flowable.task.api.Task task = taskService.createTaskQuery().singleResult();

        // Test if the property set in the model is shown in the task
        String testCategory = "My Category";
        assertEquals(testCategory, task.getCategory());

        // Test if can be queried by query API
        assertEquals("Task with category", taskService.createTaskQuery().taskCategory(testCategory).singleResult().getName());
        assertEquals(0, taskService.createTaskQuery().taskCategory("Does not exist").count());

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
            // Check historic task
            HistoricTaskInstance historicTaskInstance = historyService.createHistoricTaskInstanceQuery().taskId(task.getId()).singleResult();
            assertEquals(testCategory, historicTaskInstance.getCategory());
            assertEquals("Task with category", historyService.createHistoricTaskInstanceQuery().taskCategory(testCategory).singleResult().getName());
            assertEquals(0, historyService.createHistoricTaskInstanceQuery().taskCategory("Does not exist").count());
        }

        // Update category
        String newCategory = "New Test Category";
        task.setCategory(newCategory);
        taskService.saveTask(task);

        task = taskService.createTaskQuery().singleResult();
        assertEquals(newCategory, task.getCategory());
        assertEquals("Task with category", taskService.createTaskQuery().taskCategory(newCategory).singleResult().getName());
        assertEquals(0, taskService.createTaskQuery().taskCategory(testCategory).count());

        // Complete task and verify history
        taskService.complete(task.getId());
            
        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
            HistoricTaskInstance historicTaskInstance = historyService.createHistoricTaskInstanceQuery().taskId(task.getId()).singleResult();
            assertEquals(newCategory, historicTaskInstance.getCategory());
            assertEquals("Task with category", historyService.createHistoricTaskInstanceQuery().taskCategory(newCategory).singleResult().getName());
            assertEquals(0, historyService.createHistoricTaskInstanceQuery().taskCategory(testCategory).count());
        }
    }

    // See https://activiti.atlassian.net/browse/ACT-4041
    public void testTaskFormKeyWhenUsingIncludeVariables() {
        deployOneTaskTestProcess();
        runtimeService.startProcessInstanceByKey("oneTaskProcess");

        // Set variables
        org.flowable.task.api.Task task = taskService.createTaskQuery().singleResult();
        assertNotNull(task);
        Map<String, Object> vars = new HashMap<>();
        for (int i = 0; i < 20; i++) {
            vars.put("var" + i, i * 2);
        }
        taskService.setVariables(task.getId(), vars);

        // Set form key
        task = taskService.createTaskQuery().singleResult();
        task.setFormKey("test123");
        taskService.saveTask(task);

        // Verify query and check form key
        task = taskService.createTaskQuery().includeProcessVariables().singleResult();
        assertEquals(vars.size(), task.getProcessVariables().size());

        assertEquals("test123", task.getFormKey());
    }
    
    @Deployment
    public void testEmptyAssignmentExpression() {
        Map<String, Object> variableMap = new HashMap<>();
        variableMap.put("assignee", null);
        variableMap.put("candidateUsers", null);
        variableMap.put("candidateGroups", null);
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess", variableMap);
        assertNotNull(processInstance);
        
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertNotNull(task);
        assertNull(task.getAssignee());
        List<IdentityLink> identityLinks = taskService.getIdentityLinksForTask(task.getId());
        assertEquals(0, identityLinks.size());
        
        variableMap = new HashMap<>();
        variableMap.put("assignee", "");
        variableMap.put("candidateUsers", "");
        variableMap.put("candidateGroups", "");
        processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess", variableMap);
        assertNotNull(processInstance);
        
        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertNotNull(task);
        assertNull(task.getAssignee());
        identityLinks = taskService.getIdentityLinksForTask(task.getId());
        assertEquals(0, identityLinks.size());
    }
    
    @Deployment
    public void testNonStringProperties() {
        Map<String, Object> vars = new HashMap<>();
        vars.put("taskName", 1);
        vars.put("taskDescription", 2);
        vars.put("taskCategory", 3);
        vars.put("taskFormKey", 4);
        vars.put("taskAssignee", 5);
        vars.put("taskOwner", 6);
        vars.put("taskCandidateGroups", 7);
        vars.put("taskCandidateUsers", 8);
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("nonStringProperties", vars);
        
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("1", task.getName());
        assertEquals("2", task.getDescription());
        assertEquals("3", task.getCategory());
        assertEquals("4", task.getFormKey());
        assertEquals("5", task.getAssignee());
        assertEquals("6", task.getOwner());
        
        List<IdentityLink> identityLinks = taskService.getIdentityLinksForTask(task.getId());
        assertEquals(4, identityLinks.size());
        int candidateIdentityLinkCount = 0;
        for (IdentityLink identityLink : identityLinks) {
            if (identityLink.getType().equals(IdentityLinkType.CANDIDATE)) {
                candidateIdentityLinkCount++;
                if (identityLink.getGroupId() != null) {
                    assertEquals("7", identityLink.getGroupId());
                } else {
                    assertEquals("8", identityLink.getUserId());
                }
            }
        }
        assertEquals(2, candidateIdentityLinkCount);
    }

}
