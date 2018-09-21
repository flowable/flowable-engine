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
package org.flowable.engine.test.el.function;

import java.util.Arrays;

import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.junit.jupiter.api.Test;

/**
 * Note that the function expressions also are tested in the cmmn module (VariableFunctionDelegatesTest),
 * as such this test will not test the detailed behavior, but simply that it works in BPMN context.
 * 
 * @author Joram Barrez
 */
public class VariableExpressionFunctionsTest extends PluggableFlowableTestCase{
    
    @Test
    @Deployment
    public void testGetVariable() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("testExpressionFunction");
        assertEquals("B", taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult().getName());
        
        processInstance = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("testExpressionFunction")
                .variable("myVar", "go to A")
                .start();
        assertEquals("A", taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult().getName());
    }
    
    @Test
    @Deployment
    public void testGetVariableOrDefault() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("testExpressionFunction");
        assertEquals("A", taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult().getName()); // Default is 123
        
        processInstance = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("testExpressionFunction")
                .variable("myVar", 1)
                .start();
        assertEquals("B", taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult().getName());
        
        processInstance = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("testExpressionFunction")
                .variable("myVar", 999)
                .start();
        assertEquals("A", taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult().getName());
    }
    
    @Test
    @Deployment
    public void testGetVariableContains() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("testExpressionFunction");
        assertEquals("B", taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult().getName());
        
        processInstance = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("testExpressionFunction")
                .variable("myVar", Arrays.asList(2, 3, 4))
                .start();
        assertEquals("B", taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult().getName());
        
        processInstance = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("testExpressionFunction")
                .variable("myVar", Arrays.asList(1, 2, 3, 4))
                .start();
        assertEquals("A", taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult().getName());
    }
    
    @Test
    @Deployment
    public void testGetVariableContainsAny() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("testExpressionFunction");
        assertEquals("B", taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult().getName());
        
        processInstance = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("testExpressionFunction")
                .variable("myVar", Arrays.asList(3, 4))
                .start();
        assertEquals("B", taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult().getName());
        
        processInstance = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("testExpressionFunction")
                .variable("myVar", Arrays.asList(2, 3, 4))
                .start();
        assertEquals("A", taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult().getName());
        
        processInstance = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("testExpressionFunction")
                .variable("myVar", Arrays.asList(1, 2, 3, 4))
                .start();
        assertEquals("A", taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult().getName());
    }
    
    @Test
    @Deployment
    public void testGetVariableEquals() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("testExpressionFunction");
        assertEquals("B", taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult().getName());
        
        processInstance = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("testExpressionFunction")
                .variable("myVar", 12)
                .start();
        assertEquals("B", taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult().getName());
        
        processInstance = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("testExpressionFunction")
                .variable("myVar", 123)
                .start();
        assertEquals("A", taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult().getName());
    }
    
    @Test
    @Deployment
    public void testGetVariableNotEquals() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("testExpressionFunction");
        assertEquals("B", taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult().getName());
        
        processInstance = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("testExpressionFunction")
                .variable("myVar", "hello")
                .start();
        assertEquals("B", taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult().getName());
        
        processInstance = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("testExpressionFunction")
                .variable("myVar", "test")
                .start();
        assertEquals("A", taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult().getName());
    }
    
    
    @Test
    @Deployment
    public void testGetVariableExists() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("testExpressionFunction");
        assertEquals("B", taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult().getName());
        
        processInstance = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("testExpressionFunction")
                .variable("myVar", "hello")
                .start();
        assertEquals("A", taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult().getName());
    }
    
    @Test
    @Deployment
    public void testGetVariableIsEmpty() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("testExpressionFunction");
        assertEquals("A", taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult().getName());
        
        processInstance = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("testExpressionFunction")
                .variable("myVar", "")
                .start();
        assertEquals("A", taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult().getName());
        
        processInstance = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("testExpressionFunction")
                .variable("myVar", "abc")
                .start();
        assertEquals("B", taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult().getName());
    }
    
    @Test
    @Deployment
    public void testGetVariableIsNotEmpty() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("testExpressionFunction");
        assertEquals("B", taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult().getName());
        
        processInstance = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("testExpressionFunction")
                .variable("myVar", "")
                .start();
        assertEquals("B", taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult().getName());
        
        processInstance = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("testExpressionFunction")
                .variable("myVar", "abc")
                .start();
        assertEquals("A", taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult().getName());
    }
    
    @Test
    @Deployment
    public void testGetVariableLowerThan() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("testExpressionFunction");
        assertEquals("B", taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult().getName());
        
        processInstance = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("testExpressionFunction")
                .variable("myVar", 1)
                .start();
        assertEquals("A", taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult().getName());
        
        processInstance = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("testExpressionFunction")
                .variable("myVar", 10)
                .start();
        assertEquals("B", taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult().getName());
        
        processInstance = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("testExpressionFunction")
                .variable("myVar", 11)
                .start();
        assertEquals("B", taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult().getName());
    }
    
    @Test
    @Deployment
    public void testGetVariableLowerThanOrEqual() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("testExpressionFunction");
        assertEquals("B", taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult().getName());
        
        processInstance = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("testExpressionFunction")
                .variable("myVar", 1)
                .start();
        assertEquals("A", taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult().getName());
        
        processInstance = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("testExpressionFunction")
                .variable("myVar", 10)
                .start();
        assertEquals("A", taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult().getName());
        
        processInstance = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("testExpressionFunction")
                .variable("myVar", 11)
                .start();
        assertEquals("B", taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult().getName());
    }
    
    @Test
    @Deployment
    public void testGetVariableGreaterThan() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("testExpressionFunction");
        assertEquals("B", taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult().getName());
        
        processInstance = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("testExpressionFunction")
                .variable("myVar", 1)
                .start();
        assertEquals("B", taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult().getName());
        
        processInstance = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("testExpressionFunction")
                .variable("myVar", 10)
                .start();
        assertEquals("B", taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult().getName());
        
        processInstance = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("testExpressionFunction")
                .variable("myVar", 11)
                .start();
        assertEquals("A", taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult().getName());
    }
    
    @Test
    @Deployment
    public void testGetVariableGreaterThanOrEqual() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("testExpressionFunction");
        assertEquals("B", taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult().getName());
        
        processInstance = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("testExpressionFunction")
                .variable("myVar", 1)
                .start();
        assertEquals("B", taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult().getName());
        
        processInstance = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("testExpressionFunction")
                .variable("myVar", 10)
                .start();
        assertEquals("A", taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult().getName());
        
        processInstance = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("testExpressionFunction")
                .variable("myVar", 11)
                .start();
        assertEquals("A", taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult().getName());
    }
    
}
