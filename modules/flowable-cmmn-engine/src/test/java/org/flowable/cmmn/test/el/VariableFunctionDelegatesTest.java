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
package org.flowable.cmmn.test.el;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.engine.test.FlowableCmmnTestCase;
import org.flowable.task.api.Task;
import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

/**
 * @author Joram Barrez
 */
public class VariableFunctionDelegatesTest extends FlowableCmmnTestCase {

    @Test
    @CmmnDeployment
    public void testVariableEquals() {
       CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testElFunction").start();
       
       Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
       assertEquals("The Task", task.getName());
       
       // Setting the variable should satisfy the sentry of the second task
       cmmnRuntimeService.setVariable(caseInstance.getId(), "myVar", 123);
       List<Task> tasks = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).orderByTaskName().asc().list();
       assertEquals(2, tasks.size());
       assertEquals("Guarded Task", tasks.get(0).getName());
       assertEquals("The Task", tasks.get(1).getName());
    }
    
    @Test
    @CmmnDeployment
    public void testVariableEqualsVariableNotQuoted() {
        
        // Same as testVariableEquals, but now the variable name doesn't have quotes: ${variables:equals(myVar, 123)}
        // (This is to test if the expression enhancer works correctly).
        
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testElFunction").start();
        
        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertEquals("The Task", task.getName());
        
        // Setting the variable should satisfy the sentry of the second task
        cmmnRuntimeService.setVariable(caseInstance.getId(), "myVar", 123);
        List<Task> tasks = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).orderByTaskName().asc().list();
        assertEquals(2, tasks.size());
        assertEquals("Guarded Task", tasks.get(0).getName());
        assertEquals("The Task", tasks.get(1).getName());
    }
    
    @Test
    @CmmnDeployment
    public void testVariableNotEquals() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testElFunction").start();
        
        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertEquals("The Task", task.getName());
        
        // Setting the variable to 123 should NOT satisfy the sentry of the second task, as the notEquals is with 123
        cmmnRuntimeService.setVariable(caseInstance.getId(), "myVar", 123);
        assertEquals(1, cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).count());
        
        // Setting the variable to another value should satisfy the sentry
        cmmnRuntimeService.setVariable(caseInstance.getId(), "myVar", 1);
        List<Task> tasks = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).orderByTaskName().asc().list();
        assertEquals(2, tasks.size());
        assertEquals("Guarded Task", tasks.get(0).getName());
        assertEquals("The Task", tasks.get(1).getName());
    }
    
    @Test
    @CmmnDeployment
    public void testVariableExists() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testElFunction").start();
        
        // Variable is not set, only one  task should be created
        assertEquals(1, cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).count());
        
        // Variable is set, two tasks should be created
        cmmnRuntimeService.setVariable(caseInstance.getId(), "myVar", "someValue");
        assertEquals(2, cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).count());
        
        // Passing the variable on caseInstance start should immediately create two tasks
        caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("testElFunction")
                .variable("myVar", "Hello World")
                .start();
        assertEquals(2, cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).count());
    }
    
    @Test
    @CmmnDeployment
    public void testVariableComperatorFunctionsForInteger() {
        
        // 3 -> 2 tasks (LT 10 / LTE  10)
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testElFunctions").start();
        cmmnRuntimeService.setVariable(caseInstance.getId(), "myVar", 3);
        List<Task> tasks = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).orderByTaskName().asc().list();
        assertEquals(2, tasks.size());
        assertEquals("LT 10", tasks.get(0).getName());
        assertEquals("LTE 10", tasks.get(1).getName());
        
        // 10 -> 2 tasks (LTE 10 / GTE  10)
        caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testElFunctions").start();
        cmmnRuntimeService.setVariable(caseInstance.getId(), "myVar", 10);
        tasks = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).orderByTaskName().asc().list();
        assertEquals(2, tasks.size());
        assertEquals("GTE 10", tasks.get(0).getName());
        assertEquals("LTE 10", tasks.get(1).getName());
        
        // 13 -> 2 tasks (GT 10 / GTE 10)
        caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testElFunctions").start();
        cmmnRuntimeService.setVariable(caseInstance.getId(), "myVar", 13);
        tasks = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).orderByTaskName().asc().list();
        assertEquals(2, tasks.size());
        assertEquals("GT 10", tasks.get(0).getName());
        assertEquals("GTE 10", tasks.get(1).getName());
    }
    
    @Test
    @CmmnDeployment
    public void testVariableComperatorFunctionsForDate() {
        Instant now = Instant.now();
        Date yesterday = new Date(now.minus(Duration.ofDays(1)).toEpochMilli());
        Date tomorrow = new Date(now.plus(Duration.ofDays(1)).toEpochMilli());
        
        // Test 1 : date LT
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("testElFunctions")
                .variable("yesterday", yesterday)
                .variable("tomorrow", tomorrow)
                .start();
        assertEquals(0, cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).count());
        
        Date myVar = new Date(yesterday.getTime() - (60  * 60  * 1000)); // day before yesterday
        cmmnRuntimeService.setVariable(caseInstance.getId(), "myVar", myVar);
        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertEquals("Yesterday", task.getName());
        
        // Test 2 : date GT
        caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("testElFunctions")
                .variable("yesterday", yesterday)
                .variable("tomorrow", tomorrow)
                .start();
        assertEquals(0, cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).count());
        
        myVar = new Date(tomorrow.getTime() + (60  * 60  * 1000)); // day after tomorrow
        cmmnRuntimeService.setVariable(caseInstance.getId(), "myVar", myVar);
        task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertEquals("Tomorrow", task.getName());
    }
    
    @Test
    @CmmnDeployment
    public void testVariableIsEmpty() {
        
        //  String
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("testIsEmptyFunction")
                .variable("myVar", "hello world")
                .start();
        assertEquals(1, cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).count());
        cmmnRuntimeService.setVariable(caseInstance.getId(), "myVar", "");
        assertEquals(2, cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).count());
        
        caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("testIsEmptyFunction")
                .variable("myVar", "")
                .start();
        assertEquals(2, cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).count());
        
        caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("testIsEmptyFunction")
                .variable("myVar", "hello world")
                .start();
        assertEquals(1, cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).count());
        cmmnRuntimeService.setVariable(caseInstance.getId(), "myVar", "other value");
        assertEquals(1, cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).count());
        
        // Collection
        caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("testIsEmptyFunction")
                .variable("myVar", Arrays.asList("one", "two"))
                .start();
        assertEquals(1, cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).count());
        cmmnRuntimeService.setVariable(caseInstance.getId(), "myVar", new ArrayList<>());
        assertEquals(2, cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).count());
        
        // ArrayNode
        ObjectMapper objectMapper = new ObjectMapper();
        ArrayNode arrayNode = objectMapper.createArrayNode();
        arrayNode.add(1);
        arrayNode.add(2);
        
        caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("testIsEmptyFunction")
                .variable("myVar", arrayNode)
                .start();
        assertEquals(1, cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).count());
        cmmnRuntimeService.setVariable(caseInstance.getId(), "myVar", objectMapper.createArrayNode());
        assertEquals(2, cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).count());
    }
    
    @Test
    @CmmnDeployment
    public void testVariableIsNotEmpty() {
        
        //  String
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("testIsNotEmptyFunction")
                .variable("myVar", "")
                .start();
        assertEquals(1, cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).count());
        cmmnRuntimeService.setVariable(caseInstance.getId(), "myVar", "hello world");
        assertEquals(2, cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).count());
        
        caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("testIsNotEmptyFunction")
                .variable("myVar", "hello world")
                .start();
        assertEquals(2, cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).count());
        
        caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("testIsNotEmptyFunction")
                .variable("myVar", "")
                .start();
        assertEquals(1, cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).count());
        cmmnRuntimeService.setVariable(caseInstance.getId(), "myVar", "");
        assertEquals(1, cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).count());
        
        // Collection
        caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("testIsNotEmptyFunction")
                .variable("myVar", new ArrayList<>())
                .start();
        assertEquals(1, cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).count());
        cmmnRuntimeService.setVariable(caseInstance.getId(), "myVar",  Arrays.asList("one", "two"));
        assertEquals(2, cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).count());
        
        // ArrayNode
        ObjectMapper objectMapper = new ObjectMapper();        
        caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("testIsNotEmptyFunction")
                .variable("myVar", objectMapper.createArrayNode())
                .start();
        assertEquals(1, cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).count());
        
        ArrayNode arrayNode = objectMapper.createArrayNode();
        arrayNode.add(1);
        arrayNode.add(2);
        cmmnRuntimeService.setVariable(caseInstance.getId(), "myVar", arrayNode);
        assertEquals(2, cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).count());
    }
    
    @Test
    @CmmnDeployment
    public void testVariableContains() {
        
        //  String
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("testContainsFunction")
                .start();
        assertEquals(1, cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).count());
        cmmnRuntimeService.setVariable(caseInstance.getId(), "myVar", "test");
        assertEquals(1, cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).count());
        cmmnRuntimeService.setVariable(caseInstance.getId(), "myVar", "hello world");
        assertEquals(2, cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).count());
        
        caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("testContainsFunction")
                .variable("myVar", "why, hello world!")
                .start();
        assertEquals(2, cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).count());
        
        // Collection
        caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("testContainsFunction")
                .variable("myVar", new ArrayList<>())
                .start();
        assertEquals(1, cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).count());
        cmmnRuntimeService.setVariable(caseInstance.getId(), "myVar",  Arrays.asList("a", "hello world", "b"));
        assertEquals(2, cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).count());
        
        // ArrayNode
        ObjectMapper objectMapper = new ObjectMapper();        
        caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("testContainsFunction")
                .variable("myVar", objectMapper.createArrayNode())
                .start();
        assertEquals(1, cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).count());
        
        ArrayNode arrayNode = objectMapper.createArrayNode();
        arrayNode.add("one");
        arrayNode.add("two");
        arrayNode.add(123);
        arrayNode.add("hello world");
        cmmnRuntimeService.setVariable(caseInstance.getId(), "myVar", arrayNode);
        assertEquals(2, cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).count());
    }
    
    @Test
    @CmmnDeployment
    public void testVariableContainsAllString() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("testContainsFunction")
                .start();
        assertEquals(1, cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).count());
        cmmnRuntimeService.setVariable(caseInstance.getId(), "myVar", "test");
        assertEquals(1, cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).count());
        cmmnRuntimeService.setVariable(caseInstance.getId(), "myVar", "developers typically write a hello world when learning a new programming language");
        assertEquals(2, cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).count());
        
        caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("testContainsFunction")
                .variable("myVar", "why, hello world!")
                .start();
        assertEquals(2, cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).count());
        
    }
    
    @Test
    @CmmnDeployment
    public void testVariableContainsAllCollection() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("testContainsFunction")
                .variable("myVar", new ArrayList<>())
                .start();
        assertEquals(1, cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).count());
        cmmnRuntimeService.setVariable(caseInstance.getId(), "myVar",  Arrays.asList(1,3,4,6));
        assertEquals(1, cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).count());
        cmmnRuntimeService.setVariable(caseInstance.getId(), "myVar",  Arrays.asList(1,2,3,4,5,6));
        assertEquals(2, cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).count());
    }
    
    @Test
    @CmmnDeployment
    public void testVariableContainsAllCollection2() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("testContainsFunction")
                .variable("myVar", new ArrayList<>())
                .start();
        assertEquals(1, cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).count());
        cmmnRuntimeService.setVariable(caseInstance.getId(), "myVar",  Arrays.asList("a"));
        assertEquals(1, cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).count());
        cmmnRuntimeService.setVariable(caseInstance.getId(), "myVar",  Arrays.asList("a", "b", "c", "d", "e", "f", "g"));
        assertEquals(2, cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).count());
    }
    
    @Test
    @CmmnDeployment
    public void testVariableContainsAllArrayNode() {
        ObjectMapper objectMapper = new ObjectMapper();        
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("testContainsFunction")
                .variable("myVar", objectMapper.createArrayNode())
                .start();
        assertEquals(1, cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).count());
        
        ArrayNode arrayNode = objectMapper.createArrayNode();
        arrayNode.add(1.1);
        arrayNode.add(2.5);
        arrayNode.add(9.8);
        arrayNode.add(3.2);
        cmmnRuntimeService.setVariable(caseInstance.getId(), "myVar", arrayNode);
        assertEquals(1, cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).count());
        
        arrayNode.add(3.1);
        cmmnRuntimeService.setVariable(caseInstance.getId(), "myVar", arrayNode);
        assertEquals(2, cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).count());
    }
    
    @Test
    @CmmnDeployment
    public void testVariableContainsAny() {
        
        //  String
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("testContainsAnyFunction")
                .start();
        assertEquals(1, cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).count());
        cmmnRuntimeService.setVariable(caseInstance.getId(), "myVar", "test");
        assertEquals(1, cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).count());
        cmmnRuntimeService.setVariable(caseInstance.getId(), "myVar", "hello there");
        assertEquals(2, cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).count());
        
        caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("testContainsAnyFunction")
                .variable("myVar", "what a world!")
                .start();
        assertEquals(2, cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).count());
        
        // Collection
        caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("testContainsAnyFunction")
                .variable("myVar", new ArrayList<>())
                .start();
        assertEquals(1, cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).count());
        cmmnRuntimeService.setVariable(caseInstance.getId(), "myVar",  Arrays.asList("a", "world", "b"));
        assertEquals(2, cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).count());
        
        // ArrayNode
        ObjectMapper objectMapper = new ObjectMapper();        
        caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("testContainsAnyFunction")
                .variable("myVar", objectMapper.createArrayNode())
                .start();
        assertEquals(1, cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).count());
        
        ArrayNode arrayNode = objectMapper.createArrayNode();
        arrayNode.add("one");
        arrayNode.add("two");
        arrayNode.add(123);
        arrayNode.add("hello");
        cmmnRuntimeService.setVariable(caseInstance.getId(), "myVar", arrayNode);
        assertEquals(2, cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).count());
    }
    
    @Test
    @CmmnDeployment
    public void testVariableContainsAnyString() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("testContainsAnyFunction")
                .start();
        assertEquals(1, cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).count());
        cmmnRuntimeService.setVariable(caseInstance.getId(), "myVar", "test");
        assertEquals(1, cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).count());
        cmmnRuntimeService.setVariable(caseInstance.getId(), "myVar", "developers typically write a hello world when learning a new programming language");
        assertEquals(2, cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).count());
        
        caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("testContainsAnyFunction")
                .variable("myVar", "The world is a big place")
                .start();
        assertEquals(2, cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).count());
        
    }
    
    @Test
    @CmmnDeployment
    public void testVariableContainsAnyCollection() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("testContainsAnyFunction")
                .variable("myVar", new ArrayList<>())
                .start();
        assertEquals(1, cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).count());
        cmmnRuntimeService.setVariable(caseInstance.getId(), "myVar",  Arrays.asList("c"));
        assertEquals(1, cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).count());
        cmmnRuntimeService.setVariable(caseInstance.getId(), "myVar",  Arrays.asList("a", "b", "c", "d", "e", "f", "g"));
        assertEquals(2, cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).count());
    }
    
    @Test
    @CmmnDeployment
    public void testVariableContainsAnyArrayNode() {
        ObjectMapper objectMapper = new ObjectMapper();        
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("testContainsAnyFunction")
                .variable("myVar", objectMapper.createArrayNode())
                .start();
        assertEquals(1, cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).count());
        
        ArrayNode arrayNode = objectMapper.createArrayNode();
        arrayNode.add(3);
        arrayNode.add(4);
        cmmnRuntimeService.setVariable(caseInstance.getId(), "myVar", arrayNode);
        assertEquals(1, cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).count());
        
        arrayNode.add(1);
        cmmnRuntimeService.setVariable(caseInstance.getId(), "myVar", arrayNode);
        assertEquals(2, cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).count());
        
        arrayNode.add(2);
        caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("testContainsAnyFunction")
                .variable("myVar", arrayNode)
                .start();
        assertEquals(2, cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).count());
    }
    
    @Test
    @CmmnDeployment
    public void testVariableGet() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("testVariableGet")
                .variable("myVar", "")
                .start();
        assertEquals(1, cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).count());
        
        cmmnRuntimeService.setVariable(caseInstance.getId(), "myVar", "hello");
        assertEquals(2, cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).count());
    }
    
    @Test
    @CmmnDeployment
    public void testVariableGetWithDefaultValue() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("testVariableGet")
                .start();
        assertEquals(2, cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).count());
    }

}
