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

import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.junit.Test;

/**
 * Tests that verify the child instance gets the parent's tenant when falling back to the default tenant
 * for cross-engine and same-engine scenarios.
 */
public class FallbackTenantTest extends AbstractProcessEngineIntegrationTest {

    @Test
    public void testBpmnCaseTaskFallbackToDefaultTenant() {
        // Deploy the CMMN case to the default tenant (no tenant)
        org.flowable.cmmn.api.repository.CmmnDeployment cmmnDeployment = cmmnRepositoryService.createDeployment()
                .addClasspathResource("org/flowable/cmmn/test/oneHumanTaskCase.cmmn")
                .deploy();

        // Deploy the BPMN process with a case task (fallbackToDefaultTenant=true) to a specific tenant
        Deployment bpmnDeployment = processEngineRepositoryService.createDeployment()
                .addClasspathResource("org/flowable/cmmn/test/caseTaskProcessFallbackToDefaultTenant.bpmn20.xml")
                .tenantId("acme")
                .deploy();

        try {
            ProcessInstance processInstance = processEngineRuntimeService.createProcessInstanceBuilder()
                    .processDefinitionKey("caseTask")
                    .tenantId("acme")
                    .start();

            CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceQuery().singleResult();
            assertThat(caseInstance).isNotNull();
            assertThat(caseInstance.getTenantId()).isEqualTo("acme");

            Task caseTask = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
            assertThat(caseTask.getTenantId()).isEqualTo("acme");

            cmmnTaskService.complete(caseTask.getId());

            Task processTask = processEngineTaskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
            assertThat(processTask).isNotNull();
            processEngineTaskService.complete(processTask.getId());

            assertThat(processEngineRuntimeService.createProcessInstanceQuery().count()).isZero();
        } finally {
            processEngineRepositoryService.deleteDeployment(bpmnDeployment.getId(), true);
            cmmnRepositoryService.deleteDeployment(cmmnDeployment.getId(), true);
        }
    }

    @Test
    @CmmnDeployment(
            resources = "org/flowable/cmmn/test/FallbackTenantTest.processTaskFallbackToDefaultTenant.cmmn",
            tenantId = "acme"
    )
    public void testCmmnProcessTaskFallbackToDefaultTenant() {
        // Deploy the BPMN process to the default tenant (no tenant)
        Deployment bpmnDeployment = processEngineRepositoryService.createDeployment()
                .addClasspathResource("org/flowable/cmmn/test/oneTaskProcess.bpmn20.xml")
                .deploy();

        try {
            CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                    .caseDefinitionKey("processTaskCase")
                    .tenantId("acme")
                    .start();

            Task processTask = processEngineTaskService.createTaskQuery().singleResult();
            assertThat(processTask).isNotNull();
            assertThat(processTask.getTenantId()).isEqualTo("acme");

            ProcessInstance processInstance = processEngineRuntimeService.createProcessInstanceQuery()
                    .processInstanceId(processTask.getProcessInstanceId()).singleResult();
            assertThat(processInstance.getTenantId()).isEqualTo("acme");

            processEngineTaskService.complete(processTask.getId());
            assertThat(processEngineRuntimeService.createProcessInstanceQuery().count()).isZero();
        } finally {
            processEngineRepositoryService.deleteDeployment(bpmnDeployment.getId(), true);
        }
    }

    @Test
    public void testBpmnCallActivityFallbackToDefaultTenant() {
        // Deploy the child process to the default tenant (no tenant)
        Deployment childDeployment = processEngineRepositoryService.createDeployment()
                .addClasspathResource("org/flowable/cmmn/test/oneTaskProcess.bpmn20.xml")
                .deploy();

        // Deploy the parent process with a call activity (fallbackToDefaultTenant=true) to a specific tenant
        Deployment parentDeployment = processEngineRepositoryService.createDeployment()
                .addClasspathResource("org/flowable/cmmn/test/FallbackTenantTest.callActivityFallbackToDefaultTenant.bpmn20.xml")
                .tenantId("acme")
                .deploy();

        try {
            ProcessInstance processInstance = processEngineRuntimeService.createProcessInstanceBuilder()
                    .processDefinitionKey("callActivityProcess")
                    .tenantId("acme")
                    .start();

            Task task = processEngineTaskService.createTaskQuery().singleResult();
            assertThat(task).isNotNull();
            assertThat(task.getTenantId()).isEqualTo("acme");

            ProcessInstance childProcess = processEngineRuntimeService.createProcessInstanceQuery()
                    .superProcessInstanceId(processInstance.getId()).singleResult();
            assertThat(childProcess).isNotNull();
            assertThat(childProcess.getTenantId()).isEqualTo("acme");

            processEngineTaskService.complete(task.getId());
            assertThat(processEngineRuntimeService.createProcessInstanceQuery().count()).isZero();
        } finally {
            processEngineRepositoryService.deleteDeployment(parentDeployment.getId(), true);
            processEngineRepositoryService.deleteDeployment(childDeployment.getId(), true);
        }
    }

    @Test
    @CmmnDeployment(
            resources = "org/flowable/cmmn/test/FallbackTenantTest.caseTaskFallbackToDefaultTenant.cmmn",
            tenantId = "acme"
    )
    public void testCmmnCaseTaskFallbackToDefaultTenant() {
        // Deploy the child case to the default tenant (no tenant)
        org.flowable.cmmn.api.repository.CmmnDeployment childDeployment = cmmnRepositoryService.createDeployment()
                .addClasspathResource("org/flowable/cmmn/test/oneHumanTaskCase.cmmn")
                .deploy();

        try {
            CaseInstance parentCase = cmmnRuntimeService.createCaseInstanceBuilder()
                    .caseDefinitionKey("caseTaskCase")
                    .tenantId("acme")
                    .start();

            // The child case should exist with the parent's tenant
            CaseInstance childCase = cmmnRuntimeService.createCaseInstanceQuery()
                    .caseDefinitionKey("oneHumanTaskCase").singleResult();
            assertThat(childCase).isNotNull();
            assertThat(childCase.getTenantId()).isEqualTo("acme");

            Task task = cmmnTaskService.createTaskQuery().caseInstanceId(childCase.getId()).singleResult();
            assertThat(task).isNotNull();
            assertThat(task.getTenantId()).isEqualTo("acme");

            cmmnTaskService.complete(task.getId());
            assertThat(cmmnRuntimeService.createCaseInstanceQuery().count()).isZero();
        } finally {
            cmmnRepositoryService.deleteDeployment(childDeployment.getId(), true);
        }
    }
}
