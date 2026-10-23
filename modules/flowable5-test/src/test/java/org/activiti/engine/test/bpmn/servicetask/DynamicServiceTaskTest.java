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

package org.activiti.engine.test.bpmn.servicetask;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.activiti.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.variable.api.history.HistoricVariableInstance;

import tools.jackson.databind.node.ObjectNode;

/**
 * @author Tijs Rademakers
 */
public class DynamicServiceTaskTest extends PluggableFlowableTestCase {

    @Deployment
    public void testChangeClassName() {
        // first test without changing the class name
        Map<String, Object> varMap = new HashMap<String, Object>();
        varMap.put("count", 0);
        varMap.put("count2", 0);
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("dynamicServiceTask", varMap);

        org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        taskService.complete(task.getId());

        assertEquals(1, runtimeService.getVariable(processInstance.getId(), "count"));
        assertEquals(0, runtimeService.getVariable(processInstance.getId(), "count2"));

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        taskService.complete(task.getId());

        assertProcessEnded(processInstance.getId());

        // now test with changing the class name
        varMap = new HashMap<String, Object>();
        varMap.put("count", 0);
        varMap.put("count2", 0);
        processInstance = runtimeService.startProcessInstanceByKey("dynamicServiceTask", varMap);

        String processDefinitionId = processInstance.getProcessDefinitionId();
        ObjectNode infoNode = dynamicBpmnService.changeServiceTaskClassName("service", "org.activiti.engine.test.bpmn.servicetask.DummyServiceTask2");
        dynamicBpmnService.saveProcessDefinitionInfo(processDefinitionId, infoNode);

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        taskService.complete(task.getId());

        assertEquals(0, runtimeService.getVariable(processInstance.getId(), "count"));
        assertEquals(1, runtimeService.getVariable(processInstance.getId(), "count2"));

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        taskService.complete(task.getId());

        assertProcessEnded(processInstance.getId());
    }

    @Deployment
    public void testChangeExpression() {
        // first test without changing the class name
        // The bean is only needed by the service task, which is executed when the user task is completed
        DummyTestBean testBean = new DummyTestBean();
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("dynamicServiceTask");

        org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        Map<String, Object> transientVarMap = new HashMap<String, Object>();
        transientVarMap.put("bean", testBean);
        taskService.complete(task.getId(), Collections.<String, Object> emptyMap(), transientVarMap);

        if (processEngineConfiguration.getHistoryLevel().isAtLeast(HistoryLevel.ACTIVITY)) {
            HistoricVariableInstance historicVariableInstance = historyService.createHistoricVariableInstanceQuery()
                    .processInstanceId(processInstance.getId())
                    .variableName("executed")
                    .singleResult();
            assertNotNull(historicVariableInstance);
            assertTrue((Boolean) historicVariableInstance.getValue());
        }

        assertProcessEnded(processInstance.getId());

        // now test with changing the class name
        testBean = new DummyTestBean();
        processInstance = runtimeService.startProcessInstanceByKey("dynamicServiceTask");

        String processDefinitionId = processInstance.getProcessDefinitionId();
        ObjectNode infoNode = dynamicBpmnService.changeServiceTaskExpression("service", "${bean2.test(execution)}");
        dynamicBpmnService.saveProcessDefinitionInfo(processDefinitionId, infoNode);

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        transientVarMap = new HashMap<String, Object>();
        transientVarMap.put("bean2", testBean);
        taskService.complete(task.getId(), Collections.<String, Object> emptyMap(), transientVarMap);

        if (processEngineConfiguration.getHistoryLevel().isAtLeast(HistoryLevel.ACTIVITY)) {
            HistoricVariableInstance historicVariableInstance = historyService.createHistoricVariableInstanceQuery()
                    .processInstanceId(processInstance.getId())
                    .variableName("executed")
                    .singleResult();
            assertNotNull(historicVariableInstance);
            assertTrue((Boolean) historicVariableInstance.getValue());
        }

        assertProcessEnded(processInstance.getId());
    }

    @Deployment
    public void testChangeDelegateExpression() {
        // first test without changing the class name
        // The bean is only needed by the service task, which is executed when the user task is completed
        DummyTestDelegateBean testBean = new DummyTestDelegateBean();
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("dynamicServiceTask");

        org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        Map<String, Object> transientVarMap = new HashMap<String, Object>();
        transientVarMap.put("bean", testBean);
        taskService.complete(task.getId(), Collections.<String, Object> emptyMap(), transientVarMap);

        if (processEngineConfiguration.getHistoryLevel().isAtLeast(HistoryLevel.ACTIVITY)) {
            HistoricVariableInstance historicVariableInstance = historyService.createHistoricVariableInstanceQuery()
                    .processInstanceId(processInstance.getId())
                    .variableName("executed")
                    .singleResult();
            assertNotNull(historicVariableInstance);
            assertTrue((Boolean) historicVariableInstance.getValue());
        }

        assertProcessEnded(processInstance.getId());

        // now test with changing the class name
        testBean = new DummyTestDelegateBean();
        processInstance = runtimeService.startProcessInstanceByKey("dynamicServiceTask");

        String processDefinitionId = processInstance.getProcessDefinitionId();
        ObjectNode infoNode = dynamicBpmnService.changeServiceTaskDelegateExpression("service", "${bean2}");
        dynamicBpmnService.saveProcessDefinitionInfo(processDefinitionId, infoNode);

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        transientVarMap = new HashMap<String, Object>();
        transientVarMap.put("bean2", testBean);
        taskService.complete(task.getId(), Collections.<String, Object> emptyMap(), transientVarMap);

        if (processEngineConfiguration.getHistoryLevel().isAtLeast(HistoryLevel.ACTIVITY)) {
            HistoricVariableInstance historicVariableInstance = historyService.createHistoricVariableInstanceQuery()
                    .processInstanceId(processInstance.getId())
                    .variableName("executed")
                    .singleResult();
            assertNotNull(historicVariableInstance);
            assertTrue((Boolean) historicVariableInstance.getValue());
        }

        assertProcessEnded(processInstance.getId());
    }
}