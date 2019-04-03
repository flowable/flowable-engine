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
package org.flowable.cmmn.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.engine.repository.Deployment;
import org.flowable.eventsubscription.api.EventSubscription;
import org.flowable.task.api.Task;
import org.junit.Test;

public class SignalEventTest extends AbstractProcessEngineIntegrationTest {
    
    @Test
    @CmmnDeployment(resources = {"org/flowable/cmmn/test/processTaskWithSignalListener.cmmn"})
    public void testSignal() {
        Deployment deployment = this.processEngineRepositoryService.createDeployment().
            addClasspathResource("org/flowable/cmmn/test/signalProcess.bpmn20.xml").
            deploy();
        
        try {
            CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("signalCase").start();
            
            assertEquals(0, cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).count());
            
            Task task = processEngineTaskService.createTaskQuery().caseInstanceIdWithChildren(caseInstance.getId()).singleResult();
            assertNotNull(task);
            assertEquals("theTask", task.getTaskDefinitionKey());
            assertEquals("my task", task.getName());
            
            EventSubscription eventSubscription = cmmnRuntimeService.createEventSubscriptionQuery().scopeId(caseInstance.getId()).singleResult();
            assertEquals("testSignal", eventSubscription.getEventName());
            
            processEngineTaskService.complete(task.getId());
            
            eventSubscription = cmmnRuntimeService.createEventSubscriptionQuery().scopeId(caseInstance.getId()).singleResult();
            assertNull(eventSubscription);
            
            Task task2 = processEngineTaskService.createTaskQuery().processInstanceId(task.getProcessInstanceId()).singleResult();
            assertNotNull(task2);
            assertEquals("theTask2", task2.getTaskDefinitionKey());
            assertEquals("my task2", task2.getName());
            
            Task cmmnTask = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
            assertNotNull(cmmnTask);
            assertEquals("theTask", cmmnTask.getTaskDefinitionKey());
            assertEquals("Test task", cmmnTask.getName());
            
            processEngineTaskService.complete(task2.getId());
            
            assertEquals(0, processEngineRuntimeService.createProcessInstanceQuery().processInstanceId(task.getProcessInstanceId()).count());
            
            cmmnTaskService.complete(cmmnTask.getId());
            
            assertEquals(0, cmmnRuntimeService.createCaseInstanceQuery().caseInstanceId(caseInstance.getId()).count());
            
        
        } finally {
            processEngine.getRepositoryService().deleteDeployment(deployment.getId(), true);
        }
    }

}
