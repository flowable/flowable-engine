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
    
    @Test
    @CmmnDeployment(resources = {"org/flowable/cmmn/test/processTaskWithSignalListener.cmmn"})
    public void testMultipleSignals() {
        Deployment deployment = this.processEngineRepositoryService.createDeployment().
            addClasspathResource("org/flowable/cmmn/test/signalProcess.bpmn20.xml").
            deploy();
        
        try {
            CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("signalCase").start();
            
            CaseInstance anotherCaseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("signalCase").start();
            
            assertEquals(0, cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).count());
            
            Task task = processEngineTaskService.createTaskQuery().caseInstanceIdWithChildren(caseInstance.getId()).singleResult();
            assertNotNull(task);
            assertEquals("theTask", task.getTaskDefinitionKey());
            assertEquals("my task", task.getName());
            
            EventSubscription eventSubscription = cmmnRuntimeService.createEventSubscriptionQuery().scopeId(caseInstance.getId()).singleResult();
            assertEquals("testSignal", eventSubscription.getEventName());
            
            EventSubscription anotherEventSubscription = cmmnRuntimeService.createEventSubscriptionQuery().scopeId(anotherCaseInstance.getId()).singleResult();
            assertEquals("testSignal", anotherEventSubscription.getEventName());
            
            processEngineTaskService.complete(task.getId());
            
            eventSubscription = cmmnRuntimeService.createEventSubscriptionQuery().scopeId(caseInstance.getId()).singleResult();
            assertNull(eventSubscription);
            
            anotherEventSubscription = cmmnRuntimeService.createEventSubscriptionQuery().scopeId(anotherCaseInstance.getId()).singleResult();
            assertNull(anotherEventSubscription);
            
            Task task2 = processEngineTaskService.createTaskQuery().processInstanceId(task.getProcessInstanceId()).singleResult();
            assertNotNull(task2);
            assertEquals("theTask2", task2.getTaskDefinitionKey());
            assertEquals("my task2", task2.getName());
            
            Task cmmnTask = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
            assertNotNull(cmmnTask);
            assertEquals("theTask", cmmnTask.getTaskDefinitionKey());
            assertEquals("Test task", cmmnTask.getName());
            
            Task anotherCmmnTask = cmmnTaskService.createTaskQuery().caseInstanceId(anotherCaseInstance.getId()).singleResult();
            assertNotNull(anotherCmmnTask);
            assertEquals("theTask", cmmnTask.getTaskDefinitionKey());
            
            processEngineTaskService.complete(task2.getId());
            
            assertEquals(0, processEngineRuntimeService.createProcessInstanceQuery().processInstanceId(task.getProcessInstanceId()).count());
            
            cmmnTaskService.complete(cmmnTask.getId());
            
            assertEquals(0, cmmnRuntimeService.createCaseInstanceQuery().caseInstanceId(caseInstance.getId()).count());
            
        
        } finally {
            processEngine.getRepositoryService().deleteDeployment(deployment.getId(), true);
        }
    }
    
    @Test
    @CmmnDeployment(resources = {"org/flowable/cmmn/test/processTaskWithSignalListener.cmmn"})
    public void testSignalWithInstanceScope() {
        Deployment deployment = this.processEngineRepositoryService.createDeployment().
            addClasspathResource("org/flowable/cmmn/test/instanceScopeSignalProcess.bpmn20.xml").
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
    
    @Test
    @CmmnDeployment(resources = {"org/flowable/cmmn/test/processTaskWithSignalListener.cmmn"})
    public void testMultipleSignalWithInstanceScope() {
        Deployment deployment = this.processEngineRepositoryService.createDeployment().
            addClasspathResource("org/flowable/cmmn/test/instanceScopeSignalProcess.bpmn20.xml").
            deploy();
        
        try {
            CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("signalCase").start();
            
            CaseInstance anotherCaseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("signalCase").start();
            
            assertEquals(0, cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).count());
            
            EventSubscription eventSubscription = cmmnRuntimeService.createEventSubscriptionQuery().scopeId(caseInstance.getId()).singleResult();
            assertEquals("testSignal", eventSubscription.getEventName());
            
            EventSubscription anotherEventSubscription = cmmnRuntimeService.createEventSubscriptionQuery().scopeId(anotherCaseInstance.getId()).singleResult();
            assertEquals("testSignal", anotherEventSubscription.getEventName());
            
            Task task = processEngineTaskService.createTaskQuery().caseInstanceIdWithChildren(caseInstance.getId()).singleResult();
            processEngineTaskService.complete(task.getId());
            
            eventSubscription = cmmnRuntimeService.createEventSubscriptionQuery().scopeId(caseInstance.getId()).singleResult();
            assertNull(eventSubscription);
            
            anotherEventSubscription = cmmnRuntimeService.createEventSubscriptionQuery().scopeId(anotherCaseInstance.getId()).singleResult();
            assertEquals("testSignal", anotherEventSubscription.getEventName());
            
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
            
            anotherEventSubscription = cmmnRuntimeService.createEventSubscriptionQuery().scopeId(anotherCaseInstance.getId()).singleResult();
            assertEquals("testSignal", anotherEventSubscription.getEventName());
            
            assertEquals(1, cmmnRuntimeService.createCaseInstanceQuery().caseInstanceId(anotherCaseInstance.getId()).count());
            
        
        } finally {
            processEngine.getRepositoryService().deleteDeployment(deployment.getId(), true);
        }
    }
    
    @Test
    @CmmnDeployment(resources = {"org/flowable/cmmn/test/processTaskWithSignalListener.cmmn"})
    public void testTerminateCaseInstanceWithSignal() {
        Deployment deployment = this.processEngineRepositoryService.createDeployment().
            addClasspathResource("org/flowable/cmmn/test/signalProcess.bpmn20.xml").
            deploy();
        
        try {
            CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("signalCase").start();
            
            Task task = processEngineTaskService.createTaskQuery().caseInstanceIdWithChildren(caseInstance.getId()).singleResult();
            assertNotNull(task);
            
            assertEquals(0, cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).count());
            
            EventSubscription eventSubscription = cmmnRuntimeService.createEventSubscriptionQuery().scopeId(caseInstance.getId()).singleResult();
            assertEquals("testSignal", eventSubscription.getEventName());
            
            cmmnRuntimeService.terminateCaseInstance(caseInstance.getId());
            
            assertEquals(0, processEngineRuntimeService.createProcessInstanceQuery().processInstanceId(task.getProcessInstanceId()).count());
            
            assertEquals(0, cmmnRuntimeService.createCaseInstanceQuery().caseInstanceId(caseInstance.getId()).count());
            
        
        } finally {
            processEngine.getRepositoryService().deleteDeployment(deployment.getId(), true);
        }
    }

}
