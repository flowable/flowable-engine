package org.flowable.spring.test.jobexecutor;

import java.util.List;

import org.flowable.engine.ManagementService;
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.task.Task;
import org.flowable.spring.impl.test.CleanTestExecutionListener;
import org.flowable.spring.impl.test.SpringFlowableTestCase;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Pablo Ganga
 * @author Joram Barrez
 */
@RunWith(SpringJUnit4ClassRunner.class)
@TestExecutionListeners(CleanTestExecutionListener.class)
@ContextConfiguration("classpath:org/flowable/spring/test/components/SpringjobExecutorTest-context.xml")
public class SpringAsyncExecutorTest extends SpringFlowableTestCase {
  
  @Autowired
  protected ManagementService managementService;

  @Autowired
  protected RuntimeService runtimeService;

  @Autowired
  protected TaskService taskService;
  
  @Autowired
  protected ProcessEngineConfiguration processEngineConfiguration;

  @Test
  public void testHappyJobExecutorPath() throws Exception {
    ProcessInstance instance = runtimeService.startProcessInstanceByKey("process1");
    assertNotNull(instance);
    
    processEngineConfiguration.getAsyncExecutor().start();
    
    waitForTasksToExpire(instance, 0);

    List<Task> activeTasks = taskService.createTaskQuery().processInstanceId(instance.getId()).list();
    assertTrue(activeTasks.isEmpty());
    
    processEngineConfiguration.getAsyncExecutor().shutdown();
  }

  @Test
  public void testAsyncJobExecutorPath() throws Exception {
    ProcessInstance instance = runtimeService.startProcessInstanceByKey("asyncProcess");
    assertNotNull(instance);
    
    processEngineConfiguration.getAsyncExecutor().start();
    
    waitForProcessInstanceToEnd(instance);

    ProcessInstance processInstance = runtimeService.createProcessInstanceQuery()
        .processInstanceId(instance.getId())
        .singleResult();
    assertNull(processInstance);
    
    processEngineConfiguration.getAsyncExecutor().shutdown();
  }
  
  @Test
  public void testRollbackJobExecutorPath() throws Exception {
    ProcessInstance instance = runtimeService.startProcessInstanceByKey("errorProcess1");
    assertNotNull(instance);
    
    processEngineConfiguration.getAsyncExecutor().start();
    
    waitForTasksToExpire(instance, 1);

    List<Task> activeTasks = taskService.createTaskQuery().processInstanceId(instance.getId()).list();
    assertEquals(1, activeTasks.size());
    
    processEngineConfiguration.getAsyncExecutor().shutdown();
  }

  protected void waitForTasksToExpire(ProcessInstance instance, int expectedTaskSize) throws Exception {
    boolean finished = false;
    int nrOfSleeps = 0;
    while (!finished) {
      long jobCount = managementService.createJobQuery().count();
      long timerCount = managementService.createTimerJobQuery().count();
      if (jobCount == 0 && timerCount == 0) {
        List<Task> activeTasks = taskService.createTaskQuery().processInstanceId(instance.getId()).list();
        if (activeTasks.size() == expectedTaskSize) {
          finished = true;
        } else {
          Thread.sleep(500L);
        }
        
      } else if (nrOfSleeps < 20){
        nrOfSleeps++;
        Thread.sleep(500L);
        
      } else {
        finished = true;
      }
    }
  }
  
  protected void waitForProcessInstanceToEnd(ProcessInstance instance) throws Exception {
    boolean finished = false;
    int nrOfSleeps = 0;
    while (!finished) {
      long jobCount = managementService.createJobQuery().count();
      long timerCount = managementService.createTimerJobQuery().count();
      if (jobCount == 0 && timerCount == 0) {
        long instancesFound = runtimeService.createProcessInstanceQuery().processInstanceId(instance.getId()).count();
        if (instancesFound == 0) {
          finished = true;
        } else {
          Thread.sleep(500L);
        }
        
      } else if (nrOfSleeps < 20){
        nrOfSleeps++;
        Thread.sleep(500L);
        
      } else {
        finished = true;
      }
    }
  }

}
