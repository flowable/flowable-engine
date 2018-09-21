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

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import org.flowable.engine.impl.test.HistoryTestHelper;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.task.api.Task;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.flowable.variable.api.history.HistoricVariableInstance;
import org.junit.jupiter.api.Test;

/**
 * @author Tijs Rademakers
 */
public class HistoryLevelServiceTest extends PluggableFlowableTestCase {

  @Deployment(resources = { "org/flowable/engine/test/api/history/oneTaskHistoryLevelNoneProcess.bpmn20.xml" })
  @Test
  public void testNoneHistoryLevel() {
    // With a clean ProcessEngine, no instances should be available
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
    
    HistoryTestHelper.waitForJobExecutorToProcessAllHistoryJobs(processEngineConfiguration, managementService, 7000, 200);
    
    assertTrue(historyService.createHistoricProcessInstanceQuery().processInstanceId(processInstance.getId()).count() == 0);

    // Complete the task and check if the size is count 1
    Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
    assertNotNull(task);
    taskService.claim(task.getId(), "test");
    taskService.setOwner(task.getId(), "test");
    taskService.setAssignee(task.getId(), "anotherTest");
    taskService.setPriority(task.getId(), 40);
    taskService.setDueDate(task.getId(), new Date());
    taskService.setVariable(task.getId(), "var1", "test");
    taskService.setVariableLocal(task.getId(), "localVar1", "test2");
    taskService.complete(task.getId());
    
    HistoryTestHelper.waitForJobExecutorToProcessAllHistoryJobs(processEngineConfiguration, managementService, 7000, 200);
    
    assertTrue(historyService.createHistoricProcessInstanceQuery().processInstanceId(processInstance.getId()).count() == 0);
    assertTrue(historyService.createHistoricTaskInstanceQuery().processInstanceId(processInstance.getId()).count() == 0);
    assertTrue(historyService.createHistoricActivityInstanceQuery().processInstanceId(processInstance.getId()).count() == 0);
    assertTrue(historyService.createHistoricVariableInstanceQuery().processInstanceId(processInstance.getId()).count() == 0);
  }
  
  @Deployment(resources = { "org/flowable/engine/test/api/history/oneTaskHistoryLevelActivityProcess.bpmn20.xml" })
  @Test
  public void testActivityHistoryLevel() {
    // With a clean ProcessEngine, no instances should be available
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
    
    HistoryTestHelper.waitForJobExecutorToProcessAllHistoryJobs(processEngineConfiguration, managementService, 7000, 200);
    
    assertTrue(historyService.createHistoricProcessInstanceQuery().processInstanceId(processInstance.getId()).count() == 1);

    // Complete the task and check if the size is count 1
    Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
    assertNotNull(task);
    
    assertEquals(2, historyService.createHistoricActivityInstanceQuery().processInstanceId(processInstance.getId()).count());
    
    taskService.claim(task.getId(), "test");
    taskService.setOwner(task.getId(), "test");
    taskService.setAssignee(task.getId(), "anotherTest");
    taskService.setPriority(task.getId(), 40);
    Date dueDateValue = new Date();
    taskService.setDueDate(task.getId(), dueDateValue);
    taskService.setVariable(task.getId(), "var1", "test");
    taskService.setVariableLocal(task.getId(), "localVar1", "test2");
    taskService.complete(task.getId());
    
    HistoryTestHelper.waitForJobExecutorToProcessAllHistoryJobs(processEngineConfiguration, managementService, 7000, 200);
    
    assertTrue(historyService.createHistoricProcessInstanceQuery().processInstanceId(processInstance.getId()).count() == 1);
    assertTrue(historyService.createHistoricTaskInstanceQuery().processInstanceId(processInstance.getId()).count() == 0);
    assertEquals(3, historyService.createHistoricActivityInstanceQuery().processInstanceId(processInstance.getId()).count());
    
    assertEquals(2, historyService.createHistoricVariableInstanceQuery().processInstanceId(processInstance.getId()).count());
    
    boolean hasProcessVariable = false;
    boolean hasTaskVariable = false;
    List<HistoricVariableInstance> variables = historyService.createHistoricVariableInstanceQuery().processInstanceId(processInstance.getId()).list();
    for (HistoricVariableInstance historicVariableInstance : variables) {
      if ("var1".equals(historicVariableInstance.getVariableName())) {
        hasProcessVariable = true;
        assertEquals("test", historicVariableInstance.getValue());
        assertEquals(processInstance.getProcessInstanceId(), historicVariableInstance.getProcessInstanceId());
        assertNull(historicVariableInstance.getTaskId());
      
      } else if ("localVar1".equals(historicVariableInstance.getVariableName())) {
        hasTaskVariable = true;
        assertEquals("test2", historicVariableInstance.getValue());
        assertEquals(processInstance.getProcessInstanceId(), historicVariableInstance.getProcessInstanceId());
        assertEquals(task.getId(), historicVariableInstance.getTaskId());
      }
    }
    
    assertTrue(hasProcessVariable);
    assertTrue(hasTaskVariable);
    
    assertEquals(0, historyService.createHistoricDetailQuery().processInstanceId(processInstance.getId()).count());
  }
  
  @Deployment(resources = { "org/flowable/engine/test/api/history/oneTaskHistoryLevelAuditProcess.bpmn20.xml" })
  @Test
  public void testAuditHistoryLevel() {
    // With a clean ProcessEngine, no instances should be available
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
    
    HistoryTestHelper.waitForJobExecutorToProcessAllHistoryJobs(processEngineConfiguration, managementService, 7000, 200);
    
    assertTrue(historyService.createHistoricProcessInstanceQuery().processInstanceId(processInstance.getId()).count() == 1);

    // Complete the task and check if the size is count 1
    Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
    assertNotNull(task);
    
    assertEquals(2, historyService.createHistoricActivityInstanceQuery().processInstanceId(processInstance.getId()).count());
    
    taskService.claim(task.getId(), "test");
    taskService.setOwner(task.getId(), "test");
    taskService.setAssignee(task.getId(), "anotherTest");
    taskService.setPriority(task.getId(), 40);
    Calendar dueDateCalendar = new GregorianCalendar();
    taskService.setDueDate(task.getId(), dueDateCalendar.getTime());
    taskService.setVariable(task.getId(), "var1", "test");
    taskService.setVariableLocal(task.getId(), "localVar1", "test2");
    taskService.complete(task.getId());
    
    HistoryTestHelper.waitForJobExecutorToProcessAllHistoryJobs(processEngineConfiguration, managementService, 7000, 200);
    
    assertTrue(historyService.createHistoricProcessInstanceQuery().processInstanceId(processInstance.getId()).count() == 1);
    assertTrue(historyService.createHistoricTaskInstanceQuery().processInstanceId(processInstance.getId()).count() == 1);
    assertEquals(3, historyService.createHistoricActivityInstanceQuery().processInstanceId(processInstance.getId()).count());
    
    HistoricTaskInstance historicTask = historyService.createHistoricTaskInstanceQuery().processInstanceId(processInstance.getId()).singleResult();
    assertEquals("test", historicTask.getOwner());
    assertEquals("anotherTest", historicTask.getAssignee());
    assertEquals(40, historicTask.getPriority());
    
    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    assertEquals(simpleDateFormat.format(dueDateCalendar.getTime()), simpleDateFormat.format(historicTask.getDueDate()));
    
    assertEquals(2, historyService.createHistoricVariableInstanceQuery().processInstanceId(processInstance.getId()).count());
    
    boolean hasProcessVariable = false;
    boolean hasTaskVariable = false;
    List<HistoricVariableInstance> variables = historyService.createHistoricVariableInstanceQuery().processInstanceId(processInstance.getId()).list();
    for (HistoricVariableInstance historicVariableInstance : variables) {
      if ("var1".equals(historicVariableInstance.getVariableName())) {
        hasProcessVariable = true;
        assertEquals("test", historicVariableInstance.getValue());
        assertEquals(processInstance.getProcessInstanceId(), historicVariableInstance.getProcessInstanceId());
        assertNull(historicVariableInstance.getTaskId());
      
      } else if ("localVar1".equals(historicVariableInstance.getVariableName())) {
        hasTaskVariable = true;
        assertEquals("test2", historicVariableInstance.getValue());
        assertEquals(processInstance.getProcessInstanceId(), historicVariableInstance.getProcessInstanceId());
        assertEquals(task.getId(), historicVariableInstance.getTaskId());
      }
    }
    
    assertTrue(hasProcessVariable);
    assertTrue(hasTaskVariable);
    
    assertEquals(0, historyService.createHistoricDetailQuery().processInstanceId(processInstance.getId()).count());
  }
  
  @Deployment(resources = { "org/flowable/engine/test/api/history/oneTaskHistoryLevelFullProcess.bpmn20.xml" })
  @Test
  public void testFullHistoryLevel() {
    // With a clean ProcessEngine, no instances should be available
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
    
    HistoryTestHelper.waitForJobExecutorToProcessAllHistoryJobs(processEngineConfiguration, managementService, 7000, 200);
    
    assertTrue(historyService.createHistoricProcessInstanceQuery().processInstanceId(processInstance.getId()).count() == 1);

    // Complete the task and check if the size is count 1
    Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
    assertNotNull(task);
    
    assertEquals(2, historyService.createHistoricActivityInstanceQuery().processInstanceId(processInstance.getId()).count());
    
    taskService.claim(task.getId(), "test");
    taskService.setOwner(task.getId(), "test");
    taskService.setAssignee(task.getId(), "anotherTest");
    taskService.setPriority(task.getId(), 40);
    Date dueDateValue = new Date();
    taskService.setDueDate(task.getId(), dueDateValue);
    taskService.setVariable(task.getId(), "var1", "test");
    taskService.setVariableLocal(task.getId(), "localVar1", "test2");
    taskService.complete(task.getId());
    
    HistoryTestHelper.waitForJobExecutorToProcessAllHistoryJobs(processEngineConfiguration, managementService, 7000, 200);
    
    assertTrue(historyService.createHistoricProcessInstanceQuery().processInstanceId(processInstance.getId()).count() == 1);
    assertTrue(historyService.createHistoricTaskInstanceQuery().processInstanceId(processInstance.getId()).count() == 1);
    assertEquals(3, historyService.createHistoricActivityInstanceQuery().processInstanceId(processInstance.getId()).count());
    
    HistoricTaskInstance historicTask = historyService.createHistoricTaskInstanceQuery().processInstanceId(processInstance.getId()).singleResult();
    assertEquals("test", historicTask.getOwner());
    assertEquals("anotherTest", historicTask.getAssignee());
    assertEquals(40, historicTask.getPriority());
    
    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    assertEquals(simpleDateFormat.format(dueDateValue), simpleDateFormat.format(historicTask.getDueDate()));
    
    assertEquals(2, historyService.createHistoricVariableInstanceQuery().processInstanceId(processInstance.getId()).count());
    
    boolean hasProcessVariable = false;
    boolean hasTaskVariable = false;
    List<HistoricVariableInstance> variables = historyService.createHistoricVariableInstanceQuery().processInstanceId(processInstance.getId()).list();
    for (HistoricVariableInstance historicVariableInstance : variables) {
      if ("var1".equals(historicVariableInstance.getVariableName())) {
        hasProcessVariable = true;
        assertEquals("test", historicVariableInstance.getValue());
        assertEquals(processInstance.getProcessInstanceId(), historicVariableInstance.getProcessInstanceId());
        assertNull(historicVariableInstance.getTaskId());
      
      } else if ("localVar1".equals(historicVariableInstance.getVariableName())) {
        hasTaskVariable = true;
        assertEquals("test2", historicVariableInstance.getValue());
        assertEquals(processInstance.getProcessInstanceId(), historicVariableInstance.getProcessInstanceId());
        assertEquals(task.getId(), historicVariableInstance.getTaskId());
      }
    }
    
    assertTrue(hasProcessVariable);
    assertTrue(hasTaskVariable);
    
    assertEquals(2, historyService.createHistoricDetailQuery().processInstanceId(processInstance.getId()).count());
  }
  
}