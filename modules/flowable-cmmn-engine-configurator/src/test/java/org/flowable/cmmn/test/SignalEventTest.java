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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

import java.util.HashMap;
import java.util.Map;

import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.cmmn.CaseInstanceService;
import org.flowable.engine.repository.Deployment;
import org.flowable.eventsubscription.api.EventSubscription;
import org.flowable.eventsubscription.service.impl.persistence.entity.EventSubscriptionEntity;
import org.flowable.task.api.Task;
import org.junit.Test;

public class SignalEventTest extends AbstractProcessEngineIntegrationTest {

    @Test
    @CmmnDeployment(resources = { "org/flowable/cmmn/test/processTaskWithSignalListener.cmmn" })
    public void testSignal() {
        Deployment deployment = this.processEngineRepositoryService.createDeployment().
                addClasspathResource("org/flowable/cmmn/test/signalProcess.bpmn20.xml").
                deploy();

        try {
            CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("signalCase").start();

            assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).count()).isZero();

            Task task = processEngineTaskService.createTaskQuery().caseInstanceIdWithChildren(caseInstance.getId()).singleResult();
            assertThat(task).isNotNull();
            assertThat(task.getTaskDefinitionKey()).isEqualTo("theTask");
            assertThat(task.getName()).isEqualTo("my task");

            EventSubscription eventSubscription = cmmnRuntimeService.createEventSubscriptionQuery().scopeId(caseInstance.getId()).singleResult();
            assertThat(eventSubscription.getEventName()).isEqualTo("testSignal");

            processEngineTaskService.complete(task.getId());

            eventSubscription = cmmnRuntimeService.createEventSubscriptionQuery().scopeId(caseInstance.getId()).singleResult();
            assertThat(eventSubscription).isNull();

            Task task2 = processEngineTaskService.createTaskQuery().processInstanceId(task.getProcessInstanceId()).singleResult();
            assertThat(task2).isNotNull();
            assertThat(task2.getTaskDefinitionKey()).isEqualTo("theTask2");
            assertThat(task2.getName()).isEqualTo("my task2");

            Task cmmnTask = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
            assertThat(cmmnTask).isNotNull();
            assertThat(cmmnTask.getTaskDefinitionKey()).isEqualTo("theTask");
            assertThat(cmmnTask.getName()).isEqualTo("Test task");

            processEngineTaskService.complete(task2.getId());

            assertThat(processEngineRuntimeService.createProcessInstanceQuery().processInstanceId(task.getProcessInstanceId()).count()).isZero();

            cmmnTaskService.complete(cmmnTask.getId());

            assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceId(caseInstance.getId()).count()).isZero();

        } finally {
            processEngine.getRepositoryService().deleteDeployment(deployment.getId(), true);
        }
    }

    @Test
    @CmmnDeployment(resources = { "org/flowable/cmmn/test/processTaskWithSignalListener.cmmn" })
    public void testMultipleSignals() {
        Deployment deployment = this.processEngineRepositoryService.createDeployment().
                addClasspathResource("org/flowable/cmmn/test/signalProcess.bpmn20.xml").
                deploy();

        try {
            CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("signalCase").start();

            CaseInstance anotherCaseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("signalCase").start();

            assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).count()).isZero();

            Task task = processEngineTaskService.createTaskQuery().caseInstanceIdWithChildren(caseInstance.getId()).singleResult();
            assertThat(task).isNotNull();
            assertThat(task.getTaskDefinitionKey()).isEqualTo("theTask");
            assertThat(task.getName()).isEqualTo("my task");

            EventSubscription eventSubscription = cmmnRuntimeService.createEventSubscriptionQuery().scopeId(caseInstance.getId()).singleResult();
            assertThat(eventSubscription.getEventName()).isEqualTo("testSignal");

            EventSubscription anotherEventSubscription = cmmnRuntimeService.createEventSubscriptionQuery().scopeId(anotherCaseInstance.getId()).singleResult();
            assertThat(anotherEventSubscription.getEventName()).isEqualTo("testSignal");

            processEngineTaskService.complete(task.getId());

            eventSubscription = cmmnRuntimeService.createEventSubscriptionQuery().scopeId(caseInstance.getId()).singleResult();
            assertThat(eventSubscription).isNull();

            anotherEventSubscription = cmmnRuntimeService.createEventSubscriptionQuery().scopeId(anotherCaseInstance.getId()).singleResult();
            assertThat(anotherEventSubscription).isNull();

            Task task2 = processEngineTaskService.createTaskQuery().processInstanceId(task.getProcessInstanceId()).singleResult();
            assertThat(task2).isNotNull();
            assertThat(task2.getTaskDefinitionKey()).isEqualTo("theTask2");
            assertThat(task2.getName()).isEqualTo("my task2");

            Task cmmnTask = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
            assertThat(cmmnTask).isNotNull();
            assertThat(cmmnTask.getTaskDefinitionKey()).isEqualTo("theTask");
            assertThat(cmmnTask.getName()).isEqualTo("Test task");

            Task anotherCmmnTask = cmmnTaskService.createTaskQuery().caseInstanceId(anotherCaseInstance.getId()).singleResult();
            assertThat(anotherCmmnTask).isNotNull();
            assertThat(cmmnTask.getTaskDefinitionKey()).isEqualTo("theTask");

            processEngineTaskService.complete(task2.getId());

            assertThat(processEngineRuntimeService.createProcessInstanceQuery().processInstanceId(task.getProcessInstanceId()).count()).isZero();

            cmmnTaskService.complete(cmmnTask.getId());

            assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceId(caseInstance.getId()).count()).isZero();

        } finally {
            processEngine.getRepositoryService().deleteDeployment(deployment.getId(), true);
        }
    }

    @Test
    @CmmnDeployment(resources = { "org/flowable/cmmn/test/processTaskWithSignalListener.cmmn" })
    public void testSignalWithInstanceScope() {
        Deployment deployment = this.processEngineRepositoryService.createDeployment().
                addClasspathResource("org/flowable/cmmn/test/instanceScopeSignalProcess.bpmn20.xml").
                deploy();

        try {
            CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("signalCase").start();

            assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).count()).isZero();

            Task task = processEngineTaskService.createTaskQuery().caseInstanceIdWithChildren(caseInstance.getId()).singleResult();
            assertThat(task).isNotNull();
            assertThat(task.getTaskDefinitionKey()).isEqualTo("theTask");
            assertThat(task.getName()).isEqualTo("my task");

            EventSubscription eventSubscription = cmmnRuntimeService.createEventSubscriptionQuery().scopeId(caseInstance.getId()).singleResult();
            assertThat(eventSubscription.getEventName()).isEqualTo("testSignal");

            processEngineTaskService.complete(task.getId());

            eventSubscription = cmmnRuntimeService.createEventSubscriptionQuery().scopeId(caseInstance.getId()).singleResult();
            assertThat(eventSubscription).isNull();

            Task task2 = processEngineTaskService.createTaskQuery().processInstanceId(task.getProcessInstanceId()).singleResult();
            assertThat(task2).isNotNull();
            assertThat(task2.getTaskDefinitionKey()).isEqualTo("theTask2");
            assertThat(task2.getName()).isEqualTo("my task2");

            Task cmmnTask = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
            assertThat(cmmnTask).isNotNull();
            assertThat(cmmnTask.getTaskDefinitionKey()).isEqualTo("theTask");
            assertThat(cmmnTask.getName()).isEqualTo("Test task");

            processEngineTaskService.complete(task2.getId());

            assertThat(processEngineRuntimeService.createProcessInstanceQuery().processInstanceId(task.getProcessInstanceId()).count()).isZero();

            cmmnTaskService.complete(cmmnTask.getId());

            assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceId(caseInstance.getId()).count()).isZero();

        } finally {
            processEngine.getRepositoryService().deleteDeployment(deployment.getId(), true);
        }
    }

    @Test
    @CmmnDeployment(resources = { "org/flowable/cmmn/test/processTaskWithSignalListener.cmmn" })
    public void testMultipleSignalWithInstanceScope() {
        Deployment deployment = this.processEngineRepositoryService.createDeployment().
                addClasspathResource("org/flowable/cmmn/test/instanceScopeSignalProcess.bpmn20.xml").
                deploy();

        try {
            CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("signalCase").start();

            CaseInstance anotherCaseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("signalCase").start();

            assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).count()).isZero();

            EventSubscription eventSubscription = cmmnRuntimeService.createEventSubscriptionQuery().scopeId(caseInstance.getId()).singleResult();
            assertThat(eventSubscription.getEventName()).isEqualTo("testSignal");

            EventSubscription anotherEventSubscription = cmmnRuntimeService.createEventSubscriptionQuery().scopeId(anotherCaseInstance.getId()).singleResult();
            assertThat(anotherEventSubscription.getEventName()).isEqualTo("testSignal");

            Task task = processEngineTaskService.createTaskQuery().caseInstanceIdWithChildren(caseInstance.getId()).singleResult();
            processEngineTaskService.complete(task.getId());

            eventSubscription = cmmnRuntimeService.createEventSubscriptionQuery().scopeId(caseInstance.getId()).singleResult();
            assertThat(eventSubscription).isNull();

            anotherEventSubscription = cmmnRuntimeService.createEventSubscriptionQuery().scopeId(anotherCaseInstance.getId()).singleResult();
            assertThat(anotherEventSubscription.getEventName()).isEqualTo("testSignal");

            Task task2 = processEngineTaskService.createTaskQuery().processInstanceId(task.getProcessInstanceId()).singleResult();
            assertThat(task2).isNotNull();
            assertThat(task2.getTaskDefinitionKey()).isEqualTo("theTask2");
            assertThat(task2.getName()).isEqualTo("my task2");

            Task cmmnTask = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
            assertThat(cmmnTask).isNotNull();
            assertThat(cmmnTask.getTaskDefinitionKey()).isEqualTo("theTask");
            assertThat(cmmnTask.getName()).isEqualTo("Test task");

            processEngineTaskService.complete(task2.getId());

            assertThat(processEngineRuntimeService.createProcessInstanceQuery().processInstanceId(task.getProcessInstanceId()).count()).isZero();

            cmmnTaskService.complete(cmmnTask.getId());

            assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceId(caseInstance.getId()).count()).isZero();

            anotherEventSubscription = cmmnRuntimeService.createEventSubscriptionQuery().scopeId(anotherCaseInstance.getId()).singleResult();
            assertThat(anotherEventSubscription.getEventName()).isEqualTo("testSignal");

            assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceId(anotherCaseInstance.getId()).count()).isEqualTo(1);

        } finally {
            processEngine.getRepositoryService().deleteDeployment(deployment.getId(), true);
        }
    }

    @Test
    @CmmnDeployment(resources = { "org/flowable/cmmn/test/processTaskWithSignalListener.cmmn" })
    public void testTerminateCaseInstanceWithSignal() {
        Deployment deployment = this.processEngineRepositoryService.createDeployment().
                addClasspathResource("org/flowable/cmmn/test/signalProcess.bpmn20.xml").
                deploy();

        try {
            CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("signalCase").start();

            Task task = processEngineTaskService.createTaskQuery().caseInstanceIdWithChildren(caseInstance.getId()).singleResult();
            assertThat(task).isNotNull();

            assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).count()).isZero();

            EventSubscription eventSubscription = cmmnRuntimeService.createEventSubscriptionQuery().scopeId(caseInstance.getId()).singleResult();
            assertThat(eventSubscription.getEventName()).isEqualTo("testSignal");

            cmmnRuntimeService.terminateCaseInstance(caseInstance.getId());

            assertThat(processEngineRuntimeService.createProcessInstanceQuery().processInstanceId(task.getProcessInstanceId()).count()).isZero();

            assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceId(caseInstance.getId()).count()).isZero();

        } finally {
            processEngine.getRepositoryService().deleteDeployment(deployment.getId(), true);
        }
    }

    @Test
    @CmmnDeployment
    public void testPassVariablesThroughCaseInstanceService() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testSignalEventListener").start();
        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).list())
            .extracting(Task::getName)
            .containsOnly("A");

        EventSubscription eventSubscription = cmmnRuntimeService.createEventSubscriptionQuery()
            .scopeId(caseInstance.getId()).scopeType(ScopeTypes.CMMN).singleResult();
        assertThat(eventSubscription.getEventType()).isEqualTo("signal");

        Map<String, Object> variables = new HashMap<>();
        variables.put("hello", "world");
        variables.put("someNumber", 12345);

        CaseInstanceService caseInstanceService = ((ProcessEngineConfigurationImpl) processEngineConfiguration).getCaseInstanceService();
        caseInstanceService.handleSignalEvent((EventSubscriptionEntity) eventSubscription, variables);

        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).list())
            .extracting(Task::getName)
            .containsOnly("A", "B");

        assertThat(cmmnRuntimeService.getVariables(caseInstance.getId()))
            .containsOnly(entry("hello", "world"), entry("someNumber", 12345));

    }

}
