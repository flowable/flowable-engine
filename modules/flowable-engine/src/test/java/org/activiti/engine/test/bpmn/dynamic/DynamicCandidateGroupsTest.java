package org.activiti.engine.test.bpmn.dynamic;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.activiti.engine.DynamicBpmnConstants;
import org.activiti.engine.impl.test.PluggableActivitiTestCase;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.test.Deployment;

import java.util.ArrayList;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * Created by Pardo David on 7/12/2016.
 */
public class DynamicCandidateGroupsTest extends PluggableActivitiTestCase implements DynamicBpmnConstants {
	private final String TASK_ONE_SID = "sid-B94D5D22-E93E-4401-ADC5-C5C073E1EEB4";
	private final String TASK_TWO_SID = "sid-B1C37EBE-A273-4DDE-B909-89302638526A";
	private final String SCRIPT_TASK_SID = "sid-A403BAE0-E367-449A-90B2-48834FCAA2F9";

	@Deployment(resources = {"org/activiti/engine/test/bpmn/dynamic/dynamic-bpmn-test-process.bpmn20.xml"})
	public void testIsShouldBePossibleToChangeCandidateGroups(){
		ProcessInstance instance = runtimeService.startProcessInstanceByKey("dynamicServiceTest");
		ArrayList<String> candidateGroups = new ArrayList<String>(2);
		candidateGroups.add("HR");
		candidateGroups.add("SALES");

		ObjectNode processInfo = dynamicBpmnService.changeUserTaskCandidateGroups(TASK_ONE_SID, candidateGroups);
		dynamicBpmnService.saveProcessDefinitionInfo(instance.getProcessDefinitionId(),processInfo);

		runtimeService.startProcessInstanceByKey("dynamicServiceTest");

		long hrTaskCount = taskService.createTaskQuery().taskCandidateGroup("HR").count();
		long salesTaskCount = taskService.createTaskQuery().taskCandidateGroup("SALES").count();

		assertThat(hrTaskCount,is(1L));
		assertThat(salesTaskCount,is(1L));
	}

	@Deployment(resources = {"org/activiti/engine/test/bpmn/dynamic/dynamic-bpmn-test-process.bpmn20.xml"})
	public void testIsShouldBePossibleToResetChangeCandidateGroups(){
		ProcessInstance instance = runtimeService.startProcessInstanceByKey("dynamicServiceTest");
		ArrayList<String> candidateGroups = new ArrayList<String>(2);
		candidateGroups.add("HR");
		candidateGroups.add("SALES");

		ObjectNode processInfo = dynamicBpmnService.changeUserTaskCandidateGroups(TASK_ONE_SID, candidateGroups);
		dynamicBpmnService.saveProcessDefinitionInfo(instance.getProcessDefinitionId(),processInfo);

		//reset
		dynamicBpmnService.resetProperty(TASK_ONE_SID,USER_TASK_CANDIDATE_GROUPS,processInfo);
		dynamicBpmnService.saveProcessDefinitionInfo(instance.getProcessDefinitionId(),processInfo);

		runtimeService.startProcessInstanceByKey("dynamicServiceTest");

		long hrTaskCount = taskService.createTaskQuery().taskCandidateGroup("HR").count();
		long salesTaskCount = taskService.createTaskQuery().taskCandidateGroup("SALES").count();

		assertThat(hrTaskCount,is(0L));
		assertThat(salesTaskCount,is(0L));

	}


}
