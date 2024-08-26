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

package org.flowable.engine.test.bpmn.servicetask;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flowable.engine.impl.jobexecutor.AsyncContinuationJobHandler;
import org.flowable.engine.impl.jobexecutor.AsyncTriggerJobHandler;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.test.Deployment;
import org.flowable.job.api.Job;
import org.junit.jupiter.api.Test;

public class TriggerableJavaDelegateServiceTaskTest extends PluggableFlowableTestCase {

    @Test
    @Deployment
    public void testClassDelegate() {
        String processId = runtimeService.startProcessInstanceByKey("process").getProcessInstanceId();

        Execution execution = runtimeService.createExecutionQuery().processInstanceId(processId).activityId("service1").singleResult();
        assertThat(execution).isNotNull();
        int count = (int) runtimeService.getVariable(processId, "count");
        assertThat(count).isEqualTo(1);

        Map<String,Object> processVariables = new HashMap<>();
        processVariables.put("count", ++count);
        runtimeService.trigger(execution.getId(), processVariables, null);

        execution = runtimeService.createExecutionQuery().processInstanceId(processId).activityId("usertask1").singleResult();
        assertThat(execution).isNotNull();
        assertThat(runtimeService.getVariable(processId, "count")).isEqualTo(3);
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/bpmn/servicetask/TriggerableJavaDelegateServiceTaskTest.testClassDelegateTriggerException.bpmn20.xml")
    public void classDelegateTriggerBpmnException() {
        String processId = runtimeService.startProcessInstanceByKey("process").getProcessInstanceId();
        Execution execution = runtimeService.createExecutionQuery().processInstanceId(processId).activityId("service1").singleResult();
        runtimeService.trigger(execution.getId());

        assertThatBpmnSubProcessActive(processId);
    }

    @Test
    @Deployment
    void classDelegateTriggerExceptionWithoutWaitStateInCatch() {
        String processId = runtimeService.startProcessInstanceByKey("process").getProcessInstanceId();
        Execution execution = runtimeService.createExecutionQuery().processInstanceId(processId).activityId("service1").singleResult();
        runtimeService.trigger(execution.getId());

        assertThat(runtimeService.createProcessInstanceQuery().processInstanceId(processId).count()).isZero();
    }

    @Test
    @Deployment
    public void testDelegateExpression() {
        Map<String, Object> varMap = new HashMap<>();
        varMap.put("triggerableServiceTask", new TriggerableJavaDelegateServiceTask());

        String processId = runtimeService.startProcessInstanceByKey("process", varMap).getProcessInstanceId();

        Execution execution = runtimeService.createExecutionQuery().processInstanceId(processId).activityId("service1").singleResult();
        assertThat(execution).isNotNull();
        int count = (int) runtimeService.getVariable(processId, "count");
        assertThat(count).isEqualTo(1);

        Map<String,Object> processVariables = new HashMap<>();
        processVariables.put("count", ++count);
        runtimeService.trigger(execution.getId(), processVariables, null);

        execution = runtimeService.createExecutionQuery().processInstanceId(processId).activityId("usertask1").singleResult();
        assertThat(execution).isNotNull();
        assertThat(runtimeService.getVariable(processId, "count")).isEqualTo(3);
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/bpmn/servicetask/TriggerableJavaDelegateServiceTaskTest.testDelegateExpression.bpmn20.xml")
    public void delegateExpressionTriggerBmnError() {
        Map<String, Object> varMap = new HashMap<>();
        varMap.put("triggerableServiceTask", new ThrowBpmnErrorTriggerableJavaDelegateServiceTask());

        String processId = runtimeService.startProcessInstanceByKey("process", varMap).getProcessInstanceId();
        Execution execution = runtimeService.createExecutionQuery().processInstanceId(processId).activityId("service1").singleResult();
        runtimeService.trigger(execution.getId());

        assertThatBpmnSubProcessActive(processId);
    }

    @Test
    @Deployment
    public void testAsyncJobs() {
        String processId = runtimeService.startProcessInstanceByKey("process").getProcessInstanceId();
        
        List<Job> jobs = managementService.createJobQuery().processInstanceId(processId).list();
        assertThat(jobs).hasSize(1);
        assertThat(jobs.get(0).getJobHandlerType()).isEqualTo(AsyncContinuationJobHandler.TYPE);
        
        waitForJobExecutorToProcessAllJobs(7000L, 250L);

        Execution execution = runtimeService.createExecutionQuery().processInstanceId(processId).activityId("service1").singleResult();
        assertThat(execution).isNotNull();
        int count = (int) runtimeService.getVariable(processId, "count");
        assertThat(count).isEqualTo(1);

        Map<String,Object> processVariables = new HashMap<>();
        processVariables.put("count", ++count);
        runtimeService.triggerAsync(execution.getId(), processVariables);
        
        jobs = managementService.createJobQuery().processInstanceId(processId).list();
        assertThat(jobs).hasSize(1);
        assertThat(jobs.get(0).getJobHandlerType()).isEqualTo(AsyncTriggerJobHandler.TYPE);
        
        waitForJobExecutorToProcessAllJobs(7000L, 250L);

        execution = runtimeService.createExecutionQuery().processInstanceId(processId).activityId("usertask1").singleResult();
        assertThat(execution).isNotNull();
        assertThat(runtimeService.getVariable(processId, "count")).isEqualTo(3);
    }

    @Test
    @Deployment
    public void throwBpmnErrorInTrigger() {
        Map<String, Object> varMap = new HashMap<>();
        varMap.put("triggerableServiceTask", new ThrowBpmnErrorTriggerableJavaDelegateServiceTask());

        String processId = runtimeService.startProcessInstanceByKey("process", varMap).getProcessInstanceId();

        Execution execution = runtimeService.createExecutionQuery().processInstanceId(processId).activityId("service1").singleResult();
        assertThat(execution).isNotNull();
        runtimeService.trigger(execution.getId(), Collections.emptyMap(), null);

        execution = runtimeService.createExecutionQuery().processInstanceId(processId).activityId("taskAfterErrorCatch").singleResult();
        assertThat(execution).isNotNull();
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/bpmn/servicetask/TriggerableJavaDelegateServiceTaskTest.testDelegateExpression.bpmn20.xml")
    public void testActivityIsNotLeaving() {
        Map<String, Object> varMap = new HashMap<>();
        varMap.put("triggerableServiceTask", new TriggerableJavaDelegateServiceTask(true));

        String processId = runtimeService.startProcessInstanceByKey("process", varMap).getProcessInstanceId();

        Execution execution = runtimeService.createExecutionQuery().processInstanceId(processId).activityId("service1").singleResult();
        assertThat(execution).isNotNull();
        runtimeService.trigger(execution.getId(), Collections.emptyMap(), null);

        execution = runtimeService.createExecutionQuery().processInstanceId(processId).activityId("service1").singleResult();
        assertThat(execution).isNotNull();
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/bpmn/servicetask/TriggerableJavaDelegateServiceTaskTest.testDelegateExpression.bpmn20.xml")
    public void testActivityIsLeaving() {
        Map<String, Object> varMap = new HashMap<>();
        varMap.put("triggerableServiceTask", new TriggerableJavaDelegateServiceTask());

        String processId = runtimeService.startProcessInstanceByKey("process", varMap).getProcessInstanceId();

        Execution execution = runtimeService.createExecutionQuery().processInstanceId(processId).activityId("service1").singleResult();
        assertThat(execution).isNotNull();
        runtimeService.trigger(execution.getId(), Collections.emptyMap(), null);

        execution = runtimeService.createExecutionQuery().processInstanceId(processId).activityId("usertask1").singleResult();
        assertThat(execution).isNotNull();
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/bpmn/servicetask/TriggerableJavaDelegateServiceTaskTest.testJavaDelegateNotLeaving.bpmn20.xml")
    public void testClassDelegateIsNotLeaving() {
        String processId = runtimeService.startProcessInstanceByKey("process").getProcessInstanceId();

        Execution execution = runtimeService.createExecutionQuery().processInstanceId(processId).activityId("service1").singleResult();
        assertThat(execution).isNotNull();
        int count = (int) runtimeService.getVariable(processId, "count");
        assertThat(count).isEqualTo(1);

        Map<String, Object> processVariables = new HashMap<>();
        processVariables.put("count", ++count);
        runtimeService.trigger(execution.getId(), processVariables);

        execution = runtimeService.createExecutionQuery().processInstanceId(processId).activityId("service1").singleResult();
        assertThat(execution).isNotNull();
        count = (int) runtimeService.getVariable(processId, "count");
        assertThat(count).isEqualTo(3);
    }
    @Test
    @Deployment(resources = "org/flowable/engine/test/bpmn/servicetask/TriggerableJavaDelegateServiceTaskTest.testLeavingJavaDelegate.bpmn20.xml")
    public void testClassDelegateIsLeaving() {
        String processId = runtimeService.startProcessInstanceByKey("process").getProcessInstanceId();

        Execution execution = runtimeService.createExecutionQuery().processInstanceId(processId).activityId("service1").singleResult();
        assertThat(execution).isNotNull();
        int count = (int) runtimeService.getVariable(processId, "count");
        assertThat(count).isEqualTo(1);

        Map<String, Object> processVariables = new HashMap<>();
        processVariables.put("count", ++count);
        runtimeService.trigger(execution.getId(), processVariables);

        execution = runtimeService.createExecutionQuery().processInstanceId(processId).activityId("usertask1").singleResult();
        assertThat(execution).isNotNull();
        count = (int) runtimeService.getVariable(processId, "count");
        assertThat(count).isEqualTo(3);

    }

    @Test
    @Deployment
    public void testFutureJavaDelegateNotLeaving() {
        NotLeavingFutureJavaDelegateServiceTask.count = 0;
        String processId = runtimeService.startProcessInstanceByKey("process").getProcessInstanceId();

        Execution execution = runtimeService.createExecutionQuery().processInstanceId(processId).activityId("service1").singleResult();
        assertThat(execution).isNotNull();
        assertThat(NotLeavingFutureJavaDelegateServiceTask.count).isEqualTo(1);

        Map<String, Object> processVariables = new HashMap<>();
        processVariables.put("count", NotLeavingFutureJavaDelegateServiceTask.count++);
        runtimeService.trigger(execution.getId(), processVariables);

        execution = runtimeService.createExecutionQuery().processInstanceId(processId).activityId("service1").singleResult();
        assertThat(execution).isNotNull();
        assertThat(NotLeavingFutureJavaDelegateServiceTask.count).isEqualTo(3);
    }

    @Test
    @Deployment
    public void testFutureJavaDelegateIsLeaving() {
        String processId = runtimeService.startProcessInstanceByKey("process").getProcessInstanceId();

        Execution execution = runtimeService.createExecutionQuery().processInstanceId(processId).activityId("service1").singleResult();
        assertThat(execution).isNotNull();
        assertThat(LeavingFutureJavaDelegateServiceTask.count).isEqualTo(1);

        Map<String, Object> processVariables = new HashMap<>();
        runtimeService.trigger(execution.getId(), processVariables);

        execution = runtimeService.createExecutionQuery().processInstanceId(processId).activityId("usertask1").singleResult();
        assertThat(execution).isNotNull();
        assertThat(LeavingFutureJavaDelegateServiceTask.count).isEqualTo(2);
    }

    protected void assertThatBpmnSubProcessActive(String processId) {
        assertThat(taskService.createTaskQuery().processInstanceId(processId).taskName("Escalated Task").singleResult()).isNotNull();
    }
}
