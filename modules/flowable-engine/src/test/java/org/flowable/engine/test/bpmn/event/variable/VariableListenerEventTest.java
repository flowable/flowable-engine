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

package org.flowable.engine.test.bpmn.event.variable;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;

import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.Test;

public class VariableListenerEventTest extends PluggableFlowableTestCase {

    @Test
    @Deployment
    public void catchVariableListener() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("catchVariableListener");
        
        assertThat(runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).list()).hasSize(1);
        
        assertThat(runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).count()).isEqualTo(2);

        runtimeService.setVariable(processInstance.getId(), "var1", "test");
        
        assertThat(runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).list()).hasSize(0);

        assertThat(runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).count()).isEqualTo(0);
    }
    
    @Test
    @Deployment
    public void catchVariableListenerCreate() {
        Map<String, Object> variableMap = new HashMap<>();
        variableMap.put("var1", "test");
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("catchVariableListener", variableMap);
        
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        taskService.complete(task.getId());
        
        assertThat(runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).list()).hasSize(1);
        
        // updating variable, variable listener should not fire (create only)
        
        runtimeService.setVariable(processInstance.getId(), "var1", "test update");
        
        assertThat(runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).list()).hasSize(1);

        assertThat(runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).count()).isEqualTo(2);
    }

    @Test
    @Deployment
    public void catchVariableListenerUpdate() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("catchVariableListener");
        
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        taskService.complete(task.getId());
        
        assertThat(runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).list()).hasSize(1);
        
        // creating variable, variable listener should not fire (update only)
        
        runtimeService.setVariable(processInstance.getId(), "var1", "test");
        
        assertThat(runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).list()).hasSize(1);
        
        // updating variable, variable listener should fire
        
        runtimeService.setVariable(processInstance.getId(), "var1", "test update");
        
        assertThat(runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).list()).hasSize(0);
        
        assertThat(runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).count()).isEqualTo(0);
    }
    
    @Test
    @Deployment
    public void boundaryVariableListener() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("boundaryVariableListener");
        
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).isNotNull();
        
        assertThat(runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).list()).hasSize(1);
        
        runtimeService.setVariable(processInstance.getId(), "var1", "test");
        
        assertThat(runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).list()).hasSize(0);
        
        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task.getTaskDefinitionKey()).isEqualTo("aftertask");
        taskService.complete(task.getId());

        assertThat(runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).count()).isEqualTo(0);
    }
    
    @Test
    @Deployment(resources = "org/flowable/engine/test/bpmn/event/variable/VariableListenerEventTest.boundaryVariableListener.bpmn20.xml")
    public void boundaryVariableListenerCompleteTask() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("boundaryVariableListener");
        
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).isNotNull();
        taskService.complete(task.getId());
        
        assertThat(runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).list()).hasSize(0);
        
        assertThat(runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).count()).isEqualTo(0);
    }
    
    @Test
    @Deployment
    public void boundaryVariableListenerCreate() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("boundaryVariableListener");
        
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).isNotNull();
        
        assertThat(runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).list()).hasSize(1);
        
        // creating variable, variable listener should fire (create only)
        
        runtimeService.setVariable(processInstance.getId(), "var1", "test");
        
        assertThat(runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).count()).isZero();
        
        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task.getTaskDefinitionKey()).isEqualTo("aftertask");
        taskService.complete(task.getId());

        assertThat(runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).count()).isEqualTo(0);
    }
    
    @Test
    @Deployment
    public void boundaryVariableListenerNonInterrupting() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("boundaryVariableListener");
        
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).isNotNull();
        
        assertThat(runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).list()).hasSize(1);
        
        runtimeService.setVariable(processInstance.getId(), "var1", "test");
        
        assertThat(runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).list()).hasSize(1);
        
        assertThat(taskService.createTaskQuery().processInstanceId(processInstance.getId()).list()).hasSize(2);
        assertThat(taskService.createTaskQuery().taskDefinitionKey("aftertask").processInstanceId(processInstance.getId()).list()).hasSize(1);
        
        runtimeService.setVariable(processInstance.getId(), "var1", "test update");
        
        assertThat(runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).list()).hasSize(1);
        
        assertThat(taskService.createTaskQuery().processInstanceId(processInstance.getId()).list()).hasSize(3);
        assertThat(taskService.createTaskQuery().taskDefinitionKey("aftertask").processInstanceId(processInstance.getId()).list()).hasSize(2);
        
        task = taskService.createTaskQuery().taskDefinitionKey("task").processInstanceId(processInstance.getId()).singleResult();
        taskService.complete(task.getId());
        
        assertThat(runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).list()).hasSize(0);
        
        task = taskService.createTaskQuery().taskDefinitionKey("aftertask").processInstanceId(processInstance.getId()).list().iterator().next();
        taskService.complete(task.getId());
        
        task = taskService.createTaskQuery().taskDefinitionKey("aftertask").processInstanceId(processInstance.getId()).singleResult();
        taskService.complete(task.getId());

        assertThat(runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).count()).isEqualTo(0);
    }
    
    @Test
    @Deployment
    public void boundaryVariableListenerNonInterruptingCreate() {
        // trigger variable listener from the start
        
        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("boundaryVariableListener")
                .variable("var1", "test")
                .start();
        
        assertThat(runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).list()).hasSize(1);
        assertThat(taskService.createTaskQuery().processInstanceId(processInstance.getId()).list()).hasSize(2);
        assertThat(taskService.createTaskQuery().taskDefinitionKey("aftertask").processInstanceId(processInstance.getId()).list()).hasSize(1);

        
        // update does not trigger variable listener (only create)
        
        runtimeService.setVariable(processInstance.getId(), "var1", "test");
        
        assertThat(runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).list()).hasSize(1);
        
        assertThat(taskService.createTaskQuery().processInstanceId(processInstance.getId()).list()).hasSize(2);
        assertThat(taskService.createTaskQuery().taskDefinitionKey("aftertask").processInstanceId(processInstance.getId()).list()).hasSize(1);
        
        Task task = taskService.createTaskQuery().taskDefinitionKey("aftertask").processInstanceId(processInstance.getId()).singleResult();
        taskService.complete(task.getId());
        
        task = taskService.createTaskQuery().taskDefinitionKey("task").processInstanceId(processInstance.getId()).singleResult();
        taskService.complete(task.getId());

        assertThat(runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).count()).isEqualTo(0);
    }
    
    @Test
    @Deployment
    public void boundaryVariableListenerNonInterruptingUpdate() {
        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("boundaryVariableListener")
                .start();
        
        assertThat(runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).list()).hasSize(1);
        assertThat(taskService.createTaskQuery().processInstanceId(processInstance.getId()).list()).hasSize(1);
        assertThat(taskService.createTaskQuery().taskDefinitionKey("task").processInstanceId(processInstance.getId()).list()).hasSize(1);

        // variable create does not trigger variable listener (only update)
        
        runtimeService.setVariable(processInstance.getId(), "var1", "test");
        
        assertThat(runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).list()).hasSize(1);
        assertThat(taskService.createTaskQuery().processInstanceId(processInstance.getId()).list()).hasSize(1);
        assertThat(taskService.createTaskQuery().taskDefinitionKey("task").processInstanceId(processInstance.getId()).list()).hasSize(1);
        
        // variable update, triggers variable listener
        
        runtimeService.setVariable(processInstance.getId(), "var1", "update test 1");
        
        assertThat(runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).list()).hasSize(1);
        assertThat(taskService.createTaskQuery().processInstanceId(processInstance.getId()).list()).hasSize(2);
        assertThat(taskService.createTaskQuery().taskDefinitionKey("task").processInstanceId(processInstance.getId()).list()).hasSize(1);
        assertThat(taskService.createTaskQuery().taskDefinitionKey("aftertask").processInstanceId(processInstance.getId()).list()).hasSize(1);
        
        // another variable update, triggers variable listener
        
        runtimeService.setVariable(processInstance.getId(), "var1", "update test 2");
        
        assertThat(runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).list()).hasSize(1);
        assertThat(taskService.createTaskQuery().processInstanceId(processInstance.getId()).list()).hasSize(3);
        assertThat(taskService.createTaskQuery().taskDefinitionKey("task").processInstanceId(processInstance.getId()).list()).hasSize(1);
        assertThat(taskService.createTaskQuery().taskDefinitionKey("aftertask").processInstanceId(processInstance.getId()).list()).hasSize(2);
        
        Task task = taskService.createTaskQuery().taskDefinitionKey("task").processInstanceId(processInstance.getId()).singleResult();
        taskService.complete(task.getId());
        
        assertThat(runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).list()).hasSize(0);
        
        task = taskService.createTaskQuery().taskDefinitionKey("aftertask").processInstanceId(processInstance.getId()).list().get(0);
        taskService.complete(task.getId());
        
        task = taskService.createTaskQuery().taskDefinitionKey("aftertask").processInstanceId(processInstance.getId()).singleResult();
        taskService.complete(task.getId());

        assertThat(runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).count()).isEqualTo(0);
    }
}
