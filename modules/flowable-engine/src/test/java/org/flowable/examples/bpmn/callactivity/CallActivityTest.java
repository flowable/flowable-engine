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

package org.flowable.examples.bpmn.callactivity;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;

import org.flowable.common.engine.impl.util.CollectionUtil;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.task.api.TaskQuery;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * @author Joram Barrez
 */
public class CallActivityTest extends PluggableFlowableTestCase {

    @Test
    @Deployment(resources = { "org/flowable/examples/bpmn/callactivity/orderProcess.bpmn20.xml", "org/flowable/examples/bpmn/callactivity/checkCreditProcess.bpmn20.xml" })
    public void testOrderProcessWithCallActivity() {
        // After the process has started, the 'verify credit history' task
        // should be active
        ProcessInstance pi = runtimeService.startProcessInstanceByKey("orderProcess");
        TaskQuery taskQuery = taskService.createTaskQuery();
        org.flowable.task.api.Task verifyCreditTask = taskQuery.singleResult();
        assertThat(verifyCreditTask.getName()).isEqualTo("Verify credit history");

        // Verify with Query API
        ProcessInstance subProcessInstance = runtimeService.createProcessInstanceQuery().superProcessInstanceId(pi.getId()).singleResult();
        assertThat(subProcessInstance).isNotNull();
        assertThat(runtimeService.createProcessInstanceQuery().subProcessInstanceId(subProcessInstance.getId()).singleResult().getId()).isEqualTo(pi.getId());

        // Completing the task with approval, will end the subprocess and
        // continue the original process
        taskService.complete(verifyCreditTask.getId(), CollectionUtil.singletonMap("creditApproved", true));
        org.flowable.task.api.Task prepareAndShipTask = taskQuery.singleResult();
        assertThat(prepareAndShipTask.getName()).isEqualTo("Prepare and Ship");
    }

    @Test
    @Deployment(resources = { "org/flowable/examples/bpmn/callactivity/mainProcess.bpmn20.xml", "org/flowable/examples/bpmn/callactivity/childProcess.bpmn20.xml" })
    public void testCallActivityWithModeledDataObjectsInSubProcess() {
        // After the process has started, the 'verify credit history' task should be active
        ProcessInstance pi = runtimeService.startProcessInstanceByKey("mainProcess");
        TaskQuery taskQuery = taskService.createTaskQuery();
        org.flowable.task.api.Task verifyCreditTask = taskQuery.singleResult();
        assertThat(verifyCreditTask.getName()).isEqualTo("User Task 1");

        // Verify with Query API
        ProcessInstance subProcessInstance = runtimeService.createProcessInstanceQuery().superProcessInstanceId(pi.getId()).singleResult();
        assertThat(subProcessInstance).isNotNull();
        assertThat(runtimeService.createProcessInstanceQuery().subProcessInstanceId(subProcessInstance.getId()).singleResult().getId()).isEqualTo(pi.getId());

        assertThat(runtimeService.getVariable(subProcessInstance.getId(), "Name")).isEqualTo("Batman");
    }

    @Test
    @Deployment(resources = { "org/flowable/examples/bpmn/callactivity/mainProcess.bpmn20.xml",
            "org/flowable/examples/bpmn/callactivity/childProcess.bpmn20.xml",
            "org/flowable/examples/bpmn/callactivity/mainProcessBusinessKey.bpmn20.xml",
            "org/flowable/examples/bpmn/callactivity/mainProcessInheritBusinessKey.bpmn20.xml" })
    public void testCallActivityWithBusinessKey() {
        // No use of business key attributes
        ProcessInstance pi = runtimeService.startProcessInstanceByKey("mainProcess");
        ProcessInstance subProcessInstance = runtimeService.createProcessInstanceQuery().superProcessInstanceId(pi.getId()).singleResult();
        assertThat(subProcessInstance.getBusinessKey()).isNull();

        // Modeled using expression: businessKey="${busKey}"
        Map<String, Object> variables = new HashMap<>();
        variables.put("busKey", "123");
        pi = runtimeService.startProcessInstanceByKey("mainProcessBusinessKey", variables);
        subProcessInstance = runtimeService.createProcessInstanceQuery().superProcessInstanceId(pi.getId()).singleResult();
        assertThat(subProcessInstance.getBusinessKey()).isEqualTo("123");

        // Inherit business key
        pi = runtimeService.startProcessInstanceByKey("mainProcessInheritBusinessKey", "123");
        subProcessInstance = runtimeService.createProcessInstanceQuery().superProcessInstanceId(pi.getId()).singleResult();
        assertThat(subProcessInstance.getBusinessKey()).isEqualTo("123");
    }

    @Test
    @Deployment(resources = {"org/flowable/examples/bpmn/callactivity/processWithDynamicCallActivity.bpmn20.xml",
            "org/flowable/examples/bpmn/callactivity/calledChildProcess.bpmn20.xml",
            "org/flowable/examples/bpmn/callactivity/dynamicallyCalledChildProcess.bpmn20.xml"})
    public void testCallActivityDynamicChange(){
        // Call original CallActivity
        runtimeService.startProcessInstanceByKey("processWithDynamicCallActivity");
        taskService.complete(taskService.createTaskQuery().singleResult().getId());
        assertThat(taskService.createTaskQuery().singleResult().getName()).isEqualTo("Original Child Process User Task");
        taskService.complete(taskService.createTaskQuery().singleResult().getId());

        // Dynamically change CallActivity
        ProcessInstance dynamicallyChangedInstance = runtimeService.startProcessInstanceByKey("processWithDynamicCallActivity");
        ObjectNode infoNode = dynamicBpmnService.changeCallActivityCalledElement("callActivity", "dynamicallyCalledChildProcess");
        dynamicBpmnService.saveProcessDefinitionInfo(dynamicallyChangedInstance.getProcessDefinitionId(), infoNode);
        taskService.complete(taskService.createTaskQuery().processInstanceId(dynamicallyChangedInstance.getProcessInstanceId()).singleResult().getId());

        assertThat(taskService.createTaskQuery().singleResult().getName()).isEqualTo("Dynamically Changed Call Activity User Task");

    }
}
