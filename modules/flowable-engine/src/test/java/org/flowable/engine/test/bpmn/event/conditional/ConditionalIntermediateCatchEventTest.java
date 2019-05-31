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

import java.util.Collections;

import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.Test;

/**
 * @author Tijs Rademakers
 */
public class ConditionalIntermediateCatchEventTest extends PluggableFlowableTestCase {

    @Test
    @Deployment
    public void testConditionalIntermediateCatchEvent() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("conditionalCatchEvent", 
                        Collections.singletonMap("myVar", "empty"));

        // After process start, usertask in subprocess should exist
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("taskBeforeConditionalCatch", task.getTaskDefinitionKey());
        
        taskService.complete(task.getId());
        
        Execution execution = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).activityId("catchConditional").singleResult();
        assertNotNull(execution);
        
        runtimeService.trigger(execution.getId());
        
        assertEquals(0, taskService.createTaskQuery().processInstanceId(processInstance.getId()).count());
        
        runtimeService.setVariable(processInstance.getId(), "myVar", "test");
        
        runtimeService.trigger(execution.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("taskAfterConditionalCatch", task.getTaskDefinitionKey());
        
        taskService.complete(task.getId());
        assertProcessEnded(processInstance.getId());
    }
    
    @Test
    @Deployment(resources = "org/flowable/engine/test/bpmn/event/conditional/ConditionalIntermediateCatchEventTest.testConditionalIntermediateCatchEvent.bpmn20.xml")
    public void testConditionalIntermediateCatchEventWithEvaluation() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("conditionalCatchEvent", 
                        Collections.singletonMap("myVar", "empty"));

        // After process start, usertask in subprocess should exist
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("taskBeforeConditionalCatch", task.getTaskDefinitionKey());
        
        taskService.complete(task.getId());
        
        Execution execution = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).activityId("catchConditional").singleResult();
        assertNotNull(execution);
        
        runtimeService.evaluateConditionalEvents(processInstance.getId());
        
        assertEquals(0, taskService.createTaskQuery().processInstanceId(processInstance.getId()).count());
        
        runtimeService.evaluateConditionalEvents(processInstance.getId(), Collections.singletonMap("myVar", "test"));

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("taskAfterConditionalCatch", task.getTaskDefinitionKey());
        
        taskService.complete(task.getId());
        assertProcessEnded(processInstance.getId());
    }
}
