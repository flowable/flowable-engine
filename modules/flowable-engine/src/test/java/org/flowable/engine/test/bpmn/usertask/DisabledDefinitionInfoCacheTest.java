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
import java.util.Map;

import org.flowable.engine.impl.test.ResourceFlowableTestCase;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * @author Tijs Rademakers
 */
public class DisabledDefinitionInfoCacheTest extends ResourceFlowableTestCase {

    public DisabledDefinitionInfoCacheTest() {
        super("org/flowable/engine/test/bpmn/usertask/flowable.cfg.xml");
    }

    @Test
    @Deployment
    public void testChangeFormKey() {
        // first test without changing the form key
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("dynamicUserTask");
        String processDefinitionId = processInstance.getProcessDefinitionId();

        org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("test", task.getFormKey());
        taskService.complete(task.getId());

        assertProcessEnded(processInstance.getId());

        // now test with changing the form key
        ObjectNode infoNode = dynamicBpmnService.changeUserTaskFormKey("task1", "test2");
        dynamicBpmnService.saveProcessDefinitionInfo(processDefinitionId, infoNode);

        processInstance = runtimeService.startProcessInstanceByKey("dynamicUserTask");

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("test", task.getFormKey());
        taskService.complete(task.getId());

        assertProcessEnded(processInstance.getId());
    }

    @Test
    @Deployment
    public void testChangeClassName() {
        // first test without changing the class name
        Map<String, Object> varMap = new HashMap<>();
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
        varMap = new HashMap<>();
        varMap.put("count", 0);
        varMap.put("count2", 0);
        processInstance = runtimeService.startProcessInstanceByKey("dynamicServiceTask", varMap);

        String processDefinitionId = processInstance.getProcessDefinitionId();
        ObjectNode infoNode = dynamicBpmnService.changeServiceTaskClassName("service", "org.flowable.engine.test.bpmn.servicetask.DummyServiceTask2");
        dynamicBpmnService.saveProcessDefinitionInfo(processDefinitionId, infoNode);

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        taskService.complete(task.getId());

        assertEquals(1, runtimeService.getVariable(processInstance.getId(), "count"));
        assertEquals(0, runtimeService.getVariable(processInstance.getId(), "count2"));

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        taskService.complete(task.getId());

        assertProcessEnded(processInstance.getId());
    }

}
