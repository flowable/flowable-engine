package org.activiti.engine.test.regression;

import java.util.HashMap;
import java.util.Map;

import org.activiti.engine.impl.test.PluggableActivitiTestCase;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.impl.history.HistoryLevel;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.Job;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.task.Task;
import org.flowable.engine.test.Deployment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * From http://forums.activiti.org/content/inability-completely-delete-process-instance-when
 */
public class DeleteProcessInstanceTest extends PluggableActivitiTestCase{
	
	private static Logger log = LoggerFactory.getLogger(DeleteProcessInstanceTest.class);
	
	@Deployment
	public void testNoEndTimeSet() {
		
		//Note that the instance with a Task Type of "user" is being started.
		log.info("Starting an instance of \"Demo Partial Deletion\" with a Task Type of \"user\".");
		
		//Set the inputs for the first process instance, which we will be able to completely delete.
		Map<String,Object> inputParamsUser = new HashMap<String,Object>();
		inputParamsUser.put("taskType", "user");
		
		//Start the process instance & ensure it's started.
		ProcessInstance instanceUser = runtimeService.startProcessInstanceByKey("DemoPartialDeletion", inputParamsUser);
		assertNotNull(instanceUser);
		log.info("Process instance (of process model {}) started with id: {}.", instanceUser.getProcessDefinitionId(), instanceUser.getId());
		
		//Assert that the process instance is active.
		Execution executionUser = runtimeService.createExecutionQuery().processInstanceId(instanceUser.getProcessInstanceId()).singleResult();
		assertFalse(executionUser.isEnded());
		
		//Assert that a user task is available for claiming.
		Task taskUser = taskService.createTaskQuery().processInstanceId(instanceUser.getProcessInstanceId()).singleResult();
		assertNotNull(taskUser);
		
		//Delete the process instance.
		runtimeService.deleteProcessInstance(instanceUser.getId(), null);
		
		if (processEngineConfiguration.getHistoryLevel().isAtLeast(HistoryLevel.ACTIVITY)) {
  		//Retrieve the HistoricProcessInstance and assert that there is an end time.
  		HistoricProcessInstance hInstanceUser = historyService.createHistoricProcessInstanceQuery().processInstanceId(instanceUser.getId()).singleResult();
  		assertNotNull(hInstanceUser.getEndTime());
  		log.info("End time for the deleted instance of \"Demo Partial Deletion\" that was started with a Task Type of \"user\": {}.", hInstanceUser.getEndTime()_
  		log.info("Successfully deleted the instance of \"Demo Partial Deletion\" that was started with a Task Type of \"user\".");
		}
		
		//Note that the instance with a Task Type of "java" is being started.
		log.info("Starting an instance of \"Demo Partial Deletion\" with a Task Type of \"java\".");
				
		//Set the inputs for the second process instance, which we will NOT be able to completely delete.
		Map<String,Object> inputParamsJava = new HashMap<String,Object>();
		inputParamsJava.put("taskType", "java");
				
		//Start the process instance & ensure it's started.
		ProcessInstance instanceJava = runtimeService.startProcessInstanceByKey("DemoPartialDeletion", inputParamsJava);
		assertNotNull(instanceJava);
		log.info("Process instance (of process model {}) started with id: {}.", instanceJava.getProcessDefinitionId(), instanceJava.getId());
				
		//Assert that the process instance is active.
		Execution executionJava = runtimeService.createExecutionQuery().processInstanceId(instanceJava.getProcessInstanceId()).singleResult();
		assertFalse(executionJava.isEnded());
		
		// Try to execute job 3 times
		Job jobJava = managementService.createJobQuery().processInstanceId(instanceJava.getId()).singleResult();
    assertNotNull(jobJava);
		
    try {
      managementService.executeJob(jobJava.getId());
      fail("Expected exception");
    } catch (Exception e) {
      // expected
    }
    
    jobJava = managementService.createTimerJobQuery().processInstanceId(instanceJava.getId()).singleResult();
    try {
      managementService.moveTimerToExecutableJob(jobJava.getId());
      managementService.executeJob(jobJava.getId());
      fail("Expected exception");
    } catch (Exception e) {
      // expected
    }
    
    jobJava = managementService.createTimerJobQuery().processInstanceId(instanceJava.getId()).singleResult();
    try {
      managementService.moveTimerToExecutableJob(jobJava.getId());
      managementService.executeJob(jobJava.getId());
      fail("Expected exception");
    } catch (Exception e) {
      // expected
    }
				
		//Assert that there is a failed job.
		jobJava = managementService.createDeadLetterJobQuery().processInstanceId(instanceJava.getId()).singleResult();
		assertNotNull(jobJava);
		assertEquals(0, jobJava.getRetries());
		
		assertEquals(0, managementService.createTimerJobQuery().processInstanceId(instanceJava.getId()).count());
		assertEquals(0, managementService.createJobQuery().processInstanceId(instanceJava.getId()).count());
		
		//Delete the process instance.
		runtimeService.deleteProcessInstance(instanceJava.getId(), null);
		
		if (processEngineConfiguration.getHistoryLevel().isAtLeast(HistoryLevel.ACTIVITY)) {
  		//Retrieve the HistoricProcessInstance and assert that there is no end time.
  		HistoricProcessInstance hInstanceJava = historyService.createHistoricProcessInstanceQuery().processInstanceId(instanceJava.getId()).singleResult();
  		assertNotNull(hInstanceJava.getEndTime());
		}
	}

}
