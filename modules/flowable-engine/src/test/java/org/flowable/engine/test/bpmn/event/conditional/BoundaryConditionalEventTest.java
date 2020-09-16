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
package org.flowable.engine.test.bpmn.event.conditional;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.List;

import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.Test;

/**
 * @author Tijs Rademakers
 */
public class BoundaryConditionalEventTest extends PluggableFlowableTestCase {

    @Test
    @Deployment
    public void testCatchConditionalOnEmbeddedSubprocess() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("boundaryConditionalOnEmbeddedSubprocess", 
                        Collections.singletonMap("myVar", "empty"));

        // After process start, usertask in subprocess should exist
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task.getName()).isEqualTo("subprocessTask");
        
        Execution execution = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).activityId("catchConditional").singleResult();
        assertThat(execution).isNotNull();
        
        runtimeService.trigger(execution.getId());
        
        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task.getName()).isEqualTo("subprocessTask");
        
        runtimeService.setVariable(processInstance.getId(), "myVar", "test");
        
        runtimeService.trigger(execution.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task.getTaskDefinitionKey()).isEqualTo("taskAfterConditionalCatch");
        
        taskService.complete(task.getId());
        assertProcessEnded(processInstance.getId());
    }
    
    @Test
    @Deployment(resources = "org/flowable/engine/test/bpmn/event/conditional/BoundaryConditionalEventTest.testCatchConditionalOnEmbeddedSubprocess.bpmn20.xml")
    public void testCatchConditionalOnEmbeddedSubprocessWithoutTrigger() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("boundaryConditionalOnEmbeddedSubprocess", 
                        Collections.singletonMap("myVar", "empty"));

        // After process start, usertask in subprocess should exist
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task.getName()).isEqualTo("subprocessTask");
        
        Execution execution = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).activityId("catchConditional").singleResult();
        assertThat(execution).isNotNull();
        
        taskService.complete(task.getId());
        
        assertProcessEnded(processInstance.getId());
    }
    
    @Test
    @Deployment
    public void testCatchNonInterruptingConditionalOnEmbeddedSubprocess() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("boundaryConditionalOnEmbeddedSubprocess", 
                        Collections.singletonMap("myVar", "empty"));

        // After process start, usertask in subprocess should exist
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task.getName()).isEqualTo("subprocessTask");
        
        Execution execution = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).activityId("catchConditional").singleResult();
        assertThat(execution).isNotNull();
        
        runtimeService.trigger(execution.getId());
        
        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task.getName()).isEqualTo("subprocessTask");
        
        runtimeService.setVariable(processInstance.getId(), "myVar", "test");
        
        runtimeService.trigger(execution.getId());

        assertThat(taskService.createTaskQuery().processInstanceId(processInstance.getId()).count()).isEqualTo(2);
        
        runtimeService.trigger(execution.getId());
        
        assertThat(taskService.createTaskQuery().processInstanceId(processInstance.getId()).count()).isEqualTo(3);
        
        List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).taskDefinitionKey("taskAfterConditionalCatch").list();
        assertThat(tasks).hasSize(2);
        taskService.complete(tasks.get(0).getId());
        taskService.complete(tasks.get(1).getId());
        
        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task.getName()).isEqualTo("subprocessTask");
        taskService.complete(task.getId());
        
        assertProcessEnded(processInstance.getId());
    }
    
    @Test
    @Deployment(resources = "org/flowable/engine/test/bpmn/event/conditional/BoundaryConditionalEventTest.testCatchNonInterruptingConditionalOnEmbeddedSubprocess.bpmn20.xml")
    public void testCatchNonInterruptingConditionalOnEmbeddedSubprocessWithoutTrigger() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("boundaryConditionalOnEmbeddedSubprocess", 
                        Collections.singletonMap("myVar", "empty"));

        // After process start, usertask in subprocess should exist
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task.getName()).isEqualTo("subprocessTask");
        
        Execution execution = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).activityId("catchConditional").singleResult();
        assertThat(execution).isNotNull();
        
        taskService.complete(task.getId());
        
        assertProcessEnded(processInstance.getId());
    }
    
    @Test
    @Deployment(resources = "org/flowable/engine/test/bpmn/event/conditional/BoundaryConditionalEventTest.testCatchNonInterruptingConditionalOnEmbeddedSubprocess.bpmn20.xml")
    public void testCatchNonInterruptingConditionalOnEmbeddedSubprocessWithEvaluation() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("boundaryConditionalOnEmbeddedSubprocess", 
                        Collections.singletonMap("myVar", "empty"));

        // After process start, usertask in subprocess should exist
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task.getName()).isEqualTo("subprocessTask");
        
        Execution execution = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).activityId("catchConditional").singleResult();
        assertThat(execution).isNotNull();
        
        runtimeService.trigger(execution.getId());
        
        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task.getName()).isEqualTo("subprocessTask");
        
        runtimeService.evaluateConditionalEvents(processInstance.getId(), Collections.singletonMap("myVar", "test"));

        assertThat(taskService.createTaskQuery().processInstanceId(processInstance.getId()).count()).isEqualTo(2);
        
        runtimeService.evaluateConditionalEvents(processInstance.getId(), Collections.singletonMap("myVar", "test"));
        
        assertThat(taskService.createTaskQuery().processInstanceId(processInstance.getId()).count()).isEqualTo(3);
        
        runtimeService.evaluateConditionalEvents(processInstance.getId(), Collections.singletonMap("myVar", "test2"));
        
        assertThat(taskService.createTaskQuery().processInstanceId(processInstance.getId()).count()).isEqualTo(3);
        
        List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).taskDefinitionKey("taskAfterConditionalCatch").list();
        assertThat(tasks).hasSize(2);
        taskService.complete(tasks.get(0).getId());
        taskService.complete(tasks.get(1).getId());
        
        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task.getName()).isEqualTo("subprocessTask");
        taskService.complete(task.getId());
        
        assertProcessEnded(processInstance.getId());
    }
}
