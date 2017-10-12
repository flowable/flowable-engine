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
package org.flowable.cmmn.test.runtime;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.flowable.cmmn.engine.runtime.CaseInstance;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.engine.test.FlowableCmmnTestCase;
import org.flowable.engine.common.impl.identity.Authentication;
import org.flowable.task.service.Task;
import org.junit.Test;

/**
 * @author Tijs Rademakers
 */
public class HumanTaskTest extends FlowableCmmnTestCase {
    
    @Test
    @CmmnDeployment
    public void testHumanTask() {
        Authentication.setAuthenticatedUserId("JohnDoe");
        
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                        .caseDefinitionKey("myCase")
                        .start();
        assertNotNull(caseInstance);
        
        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertEquals("Task 1", task.getName());
        assertEquals("JohnDoe", task.getAssignee());
        
        cmmnTaskService.complete(task.getId());
        
        task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertEquals("Task 2", task.getName());
        assertNull(task.getAssignee());
        
        task = cmmnTaskService.createTaskQuery().taskCandidateGroup("test").caseInstanceId(caseInstance.getId()).singleResult();
        assertEquals("Task 2", task.getName());
        
        task = cmmnTaskService.createTaskQuery().taskCandidateUser("test2").caseInstanceId(caseInstance.getId()).singleResult();
        assertEquals("Task 2", task.getName());
        
        cmmnTaskService.complete(task.getId());
        
        assertEquals(0, cmmnRuntimeService.createCaseInstanceQuery().count());
        
        assertEquals("JohnDoe", cmmnHistoryService.createHistoricVariableInstanceQuery()
                        .caseInstanceId(caseInstance.getId())
                        .variableName("var1")
                        .singleResult().getValue());
        
        Authentication.setAuthenticatedUserId(null);
    }
    
}
