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
package org.flowable.cmmn.test.repetition;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.engine.test.FlowableCmmnTestCase;
import org.flowable.task.api.Task;
import org.junit.Test;

/**
 * @author Joram Barrez
 */
public class RepetitionRuleTest extends FlowableCmmnTestCase {

    @Test
    @CmmnDeployment
    public void testSimpleRepeatingTask() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("repeatingTask").start();
        for (int i=0; i<5; i++) {
            Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
            assertNotNull("No task found for index " + i, task);
            assertEquals("My Task", task.getName());
            cmmnTaskService.complete(task.getId());
        }
        assertCaseInstanceEnded(caseInstance);
    }
    
    @Test
    @CmmnDeployment
    public void testCustomCounterVariable() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("repeatingTask").start();
        for (int i=0; i<10; i++) {
            Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
            assertNotNull("No task found for index " + i, task);
            assertEquals("My Task", task.getName());
            cmmnTaskService.complete(task.getId());
        }
        assertCaseInstanceEnded(caseInstance);
    }
    
}
