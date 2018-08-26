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

import java.util.List;

import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.engine.test.FlowableCmmnTestCase;
import org.flowable.task.api.Task;
import org.junit.Test;

/**
 * @author Joram Barrez
 */
public class FunctionDelegatesTest extends FlowableCmmnTestCase {

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

}
