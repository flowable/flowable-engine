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
package org.flowable.standalone.history.async;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.flowable.engine.common.impl.util.CollectionUtil;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.history.HistoricTaskInstance;
import org.flowable.engine.impl.history.async.AsyncHistoryJobHandler;
import org.flowable.engine.impl.interceptor.Command;
import org.flowable.engine.impl.interceptor.CommandContext;
import org.flowable.engine.impl.persistence.entity.AbstractJobEntity;
import org.flowable.engine.impl.persistence.entity.JobEntity;
import org.flowable.engine.impl.test.ResourceFlowableTestCase;
import org.flowable.engine.runtime.Job;
import org.flowable.engine.task.Task;
import org.flowable.engine.test.Deployment;

public class AsyncHistoryTest extends ResourceFlowableTestCase {
  
  public AsyncHistoryTest() {
    super("org/flowable/standalone/history/async/async.history.flowable.cfg.xml");
  }
  
  @Override
  protected void tearDown() throws Exception {
    
    for (Job job : managementService.createJobQuery().list()) {
      if (job.getJobHandlerType().equals(AsyncHistoryJobHandler.JOB_TYPE)) {
        managementService.deleteJob(job.getId());
      }
    }
    
    super.tearDown();
  }
  
  public void testOneTaskProcess() {
    deployOneTaskTestProcess();
    for (int i=0; i<10; i++) { // Run this multiple times, as order of jobs processing can be different each run
      String processInstanceId = runtimeService.startProcessInstanceByKey("oneTaskProcess").getId();
      taskService.complete(taskService.createTaskQuery().singleResult().getId());
      
      List<Job> jobs = managementService.createJobQuery().list();
      assertEquals(2, jobs.size());
      for (Job job : jobs) {
        assertEquals(AsyncHistoryJobHandler.JOB_TYPE, job.getJobHandlerType());
        assertNotNull(((AbstractJobEntity) job).getAdvancedJobHandlerConfigurationByteArrayRef());
      }
      
      waitForJobExecutorToProcessAllJobs(5000L, 100L);
  
      HistoricProcessInstance historicProcessInstance = historyService.createHistoricProcessInstanceQuery()
          .processInstanceId(processInstanceId).singleResult();
      assertNotNull(historicProcessInstance);
      assertNotNull(historicProcessInstance.getEndTime());
      
      HistoricTaskInstance historicTaskInstance = historyService.createHistoricTaskInstanceQuery()
          .processInstanceId(processInstanceId).singleResult();
      assertNotNull(historicTaskInstance.getName());
      assertNotNull(historicTaskInstance.getExecutionId());
      assertNotNull(historicTaskInstance.getProcessInstanceId());
      assertNotNull(historicTaskInstance.getProcessDefinitionId());
      assertNotNull(historicTaskInstance.getTaskDefinitionKey());
      assertNotNull(historicTaskInstance.getStartTime());
      assertNotNull(historicTaskInstance.getEndTime());
      assertNotNull(historicTaskInstance.getDurationInMillis());
      
      List<HistoricActivityInstance> historicActivityInstances = historyService.createHistoricActivityInstanceQuery()
          .processInstanceId(processInstanceId).list();
      assertEquals(3, historicActivityInstances.size());
      for (HistoricActivityInstance historicActivityInstance : historicActivityInstances) {
        assertNotNull(historicActivityInstance.getActivityId());
        assertNotNull(historicActivityInstance.getActivityName());
        assertNotNull(historicActivityInstance.getActivityType());
        assertNotNull(historicActivityInstance.getProcessDefinitionId());
        assertNotNull(historicActivityInstance.getProcessInstanceId());
        assertNotNull(historicActivityInstance.getExecutionId());
        assertNotNull(historicActivityInstance.getDurationInMillis());
        assertNotNull(historicActivityInstance.getStartTime());
        assertNotNull(historicActivityInstance.getEndTime());
      }
      
      for (String activityId : Arrays.asList("start", "theTask", "theEnd")) {
        assertNotNull(historyService.createHistoricActivityInstanceQuery()
          .activityId(activityId).processInstanceId(processInstanceId).list());
      }
    }
  }
  
  @Deployment
  public void testSimpleStraightThroughProcess() {
    String processInstanceId = runtimeService
        .startProcessInstanceByKey("testSimpleStraightThroughProcess", CollectionUtil.singletonMap("counter", 0)).getId();
    
    final Job job = managementService.createJobQuery().singleResult();
    assertNotNull(job);
    
    // Special check to make sure we're over 4000 chars (the varchar column limit and the reason why going for a byte array)
    String jobConfig = managementService.executeCommand(new Command<String>() {
      public String execute(CommandContext commandContext) {
        try {
          JobEntity jobEntity = commandContext.getJobEntityManager().findById(job.getId());
          return new String(jobEntity.getAdvancedJobHandlerConfigurationByteArrayRef().getBytes(), "UTF-8");
        } catch (Exception e) {
          return null;
        }
      }
    });
    
    assertTrue("config length should be at least 4000, but was " + jobConfig.length(), jobConfig.length() > 4000);
    
    waitForJobExecutorToProcessAllJobs(5000L, 100L);
    assertNull(managementService.createJobQuery().singleResult());
    
    // 1002 -> (start, 1) + (end, 1) + (gateway, 1000), + (service task, 1000)
    assertEquals(2002, historyService.createHistoricActivityInstanceQuery().processInstanceId(processInstanceId).count());
  }
  
  public void testTaskAssigneeChange() {
    Task task = startOneTaskprocess();
    
    waitForJobExecutorToProcessAllJobs(5000L, 100L);
    HistoricActivityInstance historicActivityInstance = historyService.createHistoricActivityInstanceQuery()
        .activityId("theTask").singleResult();
    assertEquals("kermit", historicActivityInstance.getAssignee());
    
    task = taskService.createTaskQuery().singleResult();
    taskService.setAssignee(task.getId(), "johnDoe");
    waitForJobExecutorToProcessAllJobs(5000L, 100L);
    historicActivityInstance = historyService.createHistoricActivityInstanceQuery().activityId("theTask").singleResult();
    assertEquals("johnDoe", historicActivityInstance.getAssignee());
    
    finishOneTaskProcess(task);
  }
  
  public void testTaskAssigneeChangeToNull() {
    Task task = startOneTaskprocess();
    
    waitForJobExecutorToProcessAllJobs(5000L, 100L);
    HistoricActivityInstance historicActivityInstance = historyService.createHistoricActivityInstanceQuery()
        .activityId("theTask").singleResult();
    assertEquals("kermit", historicActivityInstance.getAssignee());
    
    task = taskService.createTaskQuery().singleResult();
    taskService.setAssignee(task.getId(), null);
    waitForJobExecutorToProcessAllJobs(5000L, 100L);
    historicActivityInstance = historyService.createHistoricActivityInstanceQuery().activityId("theTask").singleResult();
    assertNull(historicActivityInstance.getAssignee());
    
    finishOneTaskProcess(task);
  }
  
  public void testClaimTask() {
    Task task = startOneTaskprocess();
    taskService.setAssignee(task.getId(), null);
    
    waitForJobExecutorToProcessAllJobs(5000L, 100L);
    HistoricTaskInstance historicTaskInstance = historyService.createHistoricTaskInstanceQuery().singleResult();
    assertNull(historicTaskInstance.getClaimTime());
    
    taskService.claim(historicTaskInstance.getId(), "johnDoe");
    waitForJobExecutorToProcessAllJobs(5000L, 100L);
    historicTaskInstance = historyService.createHistoricTaskInstanceQuery().taskId(historicTaskInstance.getId()).singleResult();
    assertNotNull(historicTaskInstance.getClaimTime());
    assertNotNull(historicTaskInstance.getAssignee());
    
    finishOneTaskProcess(task);
  }
  
  public void testSetTaskOwner() {
    Task task = startOneTaskprocess();
    assertNull(task.getOwner());
    
    waitForJobExecutorToProcessAllJobs(5000L, 100L);
    HistoricTaskInstance historicTaskInstance = historyService.createHistoricTaskInstanceQuery().singleResult();
    assertNull(historicTaskInstance.getOwner());
    
    taskService.setOwner(task.getId(), "johnDoe");
    
    waitForJobExecutorToProcessAllJobs(5000L, 100L);
    historicTaskInstance = historyService.createHistoricTaskInstanceQuery().singleResult();
    assertEquals("johnDoe", historicTaskInstance.getOwner());
    
    taskService.setOwner(task.getId(), null);
    waitForJobExecutorToProcessAllJobs(5000L, 100L);
    historicTaskInstance = historyService.createHistoricTaskInstanceQuery().taskId(historicTaskInstance.getId()).singleResult();
    assertNull(historicTaskInstance.getOwner());
    
    finishOneTaskProcess(task);
  }
  
  public void testSetTaskName() {
    Task task = startOneTaskprocess();
    assertEquals("The Task", task.getName());
    
    waitForJobExecutorToProcessAllJobs(5000L, 100L);
    HistoricTaskInstance historicTaskInstance = historyService.createHistoricTaskInstanceQuery().singleResult();
    assertEquals("The Task", historicTaskInstance.getName());
    
    task.setName("new name");
    taskService.saveTask(task);
    
    waitForJobExecutorToProcessAllJobs(5000L, 100L);
    historicTaskInstance = historyService.createHistoricTaskInstanceQuery().singleResult();
    assertEquals("new name", historicTaskInstance.getName());
    
    finishOneTaskProcess(task);
  }
  
  public void testSetTaskDescription() {
    Task task = startOneTaskprocess();
    assertNull(task.getDescription());
    
    waitForJobExecutorToProcessAllJobs(5000L, 100L);
    HistoricTaskInstance historicTaskInstance = historyService.createHistoricTaskInstanceQuery().singleResult();
    assertNull(historicTaskInstance.getDescription());
    
    task.setDescription("test description");
    taskService.saveTask(task);
    
    waitForJobExecutorToProcessAllJobs(5000L, 100L);
    historicTaskInstance = historyService.createHistoricTaskInstanceQuery().singleResult();
    assertNotNull(historicTaskInstance.getDescription());

    task = taskService.createTaskQuery().taskId(task.getId()).singleResult();
    task.setDescription(null);
    taskService.saveTask(task);
    
    waitForJobExecutorToProcessAllJobs(5000L, 100L);
    historicTaskInstance = historyService.createHistoricTaskInstanceQuery().taskId(historicTaskInstance.getId()).singleResult();
    assertNull(historicTaskInstance.getDescription());
    
    finishOneTaskProcess(task);
  }
  
  public void testSetTaskDueDate() {
    Task task = startOneTaskprocess();
    assertNull(task.getDueDate());
    
    waitForJobExecutorToProcessAllJobs(5000L, 100L);
    HistoricTaskInstance historicTaskInstance = historyService.createHistoricTaskInstanceQuery().singleResult();
    assertNull(historicTaskInstance.getDueDate());
    
    taskService.setDueDate(task.getId(), new Date());
    
    waitForJobExecutorToProcessAllJobs(5000L, 100L);
    historicTaskInstance = historyService.createHistoricTaskInstanceQuery().singleResult();
    assertNotNull(historicTaskInstance.getDueDate());
    
    taskService.setDueDate(task.getId(), null);
    waitForJobExecutorToProcessAllJobs(5000L, 100L);
    historicTaskInstance = historyService.createHistoricTaskInstanceQuery().taskId(historicTaskInstance.getId()).singleResult();
    assertNull(historicTaskInstance.getDueDate());
    
    finishOneTaskProcess(task);
  }
  
  public void testSetTaskPriority() {
    Task task = startOneTaskprocess();
    assertEquals(Task.DEFAULT_PRIORITY, task.getPriority());
    
    waitForJobExecutorToProcessAllJobs(5000L, 100L);
    HistoricTaskInstance historicTaskInstance = historyService.createHistoricTaskInstanceQuery().singleResult();
    assertEquals(Task.DEFAULT_PRIORITY, historicTaskInstance.getPriority());
    
    taskService.setPriority(task.getId(), 1);
    
    waitForJobExecutorToProcessAllJobs(5000L, 100L);
    historicTaskInstance = historyService.createHistoricTaskInstanceQuery().singleResult();
    assertEquals(1, historicTaskInstance.getPriority());
    
    finishOneTaskProcess(task);
  }
  
  public void testSetTaskCategory() {
    Task task = startOneTaskprocess();
    assertNull(task.getCategory());
    
    waitForJobExecutorToProcessAllJobs(5000L, 100L);
    HistoricTaskInstance historicTaskInstance = historyService.createHistoricTaskInstanceQuery().singleResult();
    assertNull(historicTaskInstance.getCategory());
    
    task.setCategory("test category");
    taskService.saveTask(task);
    
    waitForJobExecutorToProcessAllJobs(5000L, 100L);
    historicTaskInstance = historyService.createHistoricTaskInstanceQuery().singleResult();
    assertEquals("test category", historicTaskInstance.getCategory());
    
    finishOneTaskProcess(task);
  }
  
  public void testSetTaskFormKey() {
    Task task = startOneTaskprocess();
    assertNull(task.getFormKey());
    
    waitForJobExecutorToProcessAllJobs(5000L, 100L);
    HistoricTaskInstance historicTaskInstance = historyService.createHistoricTaskInstanceQuery().singleResult();
    assertNull(historicTaskInstance.getFormKey());
    task.setFormKey("test form key");
    taskService.saveTask(task);
    
    waitForJobExecutorToProcessAllJobs(5000L, 100L);
    historicTaskInstance = historyService.createHistoricTaskInstanceQuery().singleResult();
    assertEquals("test form key", historicTaskInstance.getFormKey());
    
    finishOneTaskProcess(task);
  }
  
  /* TODO: currently no history for standalone tasks
   * 
  public void testSetTaskParentId() {
    Task parentTask1 = taskService.newTask();
    parentTask1.setName("Parent task 1");
    taskService.saveTask(parentTask1);
    taskService.saveTask(parentTask1);
    
    Task parentTask2 = taskService.newTask();
    parentTask2.setName("Parent task 2");
    taskService.saveTask(parentTask2);
    
    Task childTask = taskService.newTask();
    childTask.setName("child task");
    childTask.setParentTaskId(parentTask1.getId());
    taskService.saveTask(childTask);
    assertEquals(1, taskService.getSubTasks(parentTask1.getId()).size());
    assertEquals(0, taskService.getSubTasks(parentTask2.getId()).size());
    
    HistoricTaskInstance historicTaskInstance = historyService.createHistoricTaskInstanceQuery().taskId(childTask.getId()).singleResult();
    assertEquals(parentTask2.getId(), historicTaskInstance.getParentTaskId());
    
    childTask = taskService.createTaskQuery().taskId(childTask.getId()).singleResult();
    childTask.setParentTaskId(parentTask2.getId());
    taskService.saveTask(childTask);
    assertEquals(0, taskService.getSubTasks(parentTask1.getId()).size());
    assertEquals(1, taskService.getSubTasks(parentTask2.getId()).size());
    
    historicTaskInstance = historyService.createHistoricTaskInstanceQuery().taskId(childTask.getId()).singleResult();
    assertEquals(parentTask1.getId(), historicTaskInstance.getParentTaskId());
  }
  */
  
  protected Task startOneTaskprocess() {
    deployOneTaskTestProcess();
    String processInstanceId = runtimeService.startProcessInstanceByKey("oneTaskProcess").getId();
    Task task = taskService.createTaskQuery().processInstanceId(processInstanceId).singleResult();
    return task;
  }
  
  protected void finishOneTaskProcess(Task task) {
    taskService.complete(task.getId());
    waitForJobExecutorToProcessAllJobs(5000L, 100L);
    assertNull(managementService.createJobQuery().singleResult());
  }

}
