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

import java.util.List;

import org.flowable.cmmn.api.repository.CmmnDeployment;
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.entitylink.api.EntityLink;
import org.flowable.entitylink.api.history.HistoricEntityLink;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests that historic entity links are not created when the child sub-instance has history level NONE,
 * even when the parent has history enabled.
 */
public class EntityLinkHistoryLevelTest extends AbstractProcessEngineIntegrationTest {

    @BeforeEach
    public void enableDefinitionHistoryLevel() {
        processEngineConfiguration.setEnableProcessDefinitionHistoryLevel(true);
        cmmnEngineConfiguration.setEnableCaseDefinitionHistoryLevel(true);
    }

    @AfterEach
    public void disableDefinitionHistoryLevel() {
        processEngineConfiguration.setEnableProcessDefinitionHistoryLevel(false);
        cmmnEngineConfiguration.setEnableCaseDefinitionHistoryLevel(false);
    }

    @Test
    public void testBpmnParentWithCmmnChildHistoryNone() {
        CmmnDeployment cmmnDeployment = cmmnRepositoryService.createDeployment()
                .addClasspathResource("org/flowable/cmmn/test/EntityLinkHistoryLevelTest.oneHumanTaskCaseHistoryNone.cmmn")
                .deploy();
        Deployment bpmnDeployment = processEngineRepositoryService.createDeployment()
                .addClasspathResource("org/flowable/cmmn/test/EntityLinkHistoryLevelTest.caseTaskProcessHistoryNoneChild.bpmn20.xml")
                .deploy();

        try {
            ProcessInstance processInstance = processEngineRuntimeService.startProcessInstanceByKey("caseTaskHistoryNoneChild");

            // The case task should have started a child case instance
            CaseInstance childCaseInstance = cmmnRuntimeService.createCaseInstanceQuery().singleResult();
            assertThat(childCaseInstance).isNotNull();

            // Runtime entity links should exist (parent process -> child case)
            List<EntityLink> entityLinks = processEngineRuntimeService.getEntityLinkChildrenForProcessInstance(processInstance.getId());
            assertThat(entityLinks).isNotEmpty();
            assertThat(entityLinks)
                    .anyMatch(el -> ScopeTypes.CMMN.equals(el.getReferenceScopeType())
                            && childCaseInstance.getId().equals(el.getReferenceScopeId()));

            // Historic entity link to the child CMMN case should NOT exist because the child case has historyLevel=none
            List<HistoricEntityLink> historicEntityLinks = processEngineHistoryService
                    .getHistoricEntityLinkChildrenForProcessInstance(processInstance.getId());
            assertThat(historicEntityLinks)
                    .noneMatch(el -> ScopeTypes.CMMN.equals(el.getReferenceScopeType())
                            && childCaseInstance.getId().equals(el.getReferenceScopeId()));

        } finally {
            cmmnRepositoryService.deleteDeployment(cmmnDeployment.getId(), true);
            processEngineRepositoryService.deleteDeployment(bpmnDeployment.getId(), true);
        }
    }

    @Test
    public void testCmmnParentWithBpmnChildHistoryNone() {
        CmmnDeployment cmmnDeployment = cmmnRepositoryService.createDeployment()
                .addClasspathResource("org/flowable/cmmn/test/EntityLinkHistoryLevelTest.processTaskCaseHistoryNoneChild.cmmn")
                .deploy();
        Deployment bpmnDeployment = processEngineRepositoryService.createDeployment()
                .addClasspathResource("org/flowable/cmmn/test/EntityLinkHistoryLevelTest.oneTaskProcessHistoryNone.bpmn20.xml")
                .deploy();

        try {
            CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                    .caseDefinitionKey("processTaskCase")
                    .start();

            // The process task should have started a child process instance
            ProcessInstance childProcessInstance = processEngineRuntimeService.createProcessInstanceQuery().singleResult();
            assertThat(childProcessInstance).isNotNull();

            // Runtime entity links should exist (parent case -> child process)
            List<EntityLink> entityLinks = cmmnRuntimeService.getEntityLinkChildrenForCaseInstance(caseInstance.getId());
            assertThat(entityLinks).isNotEmpty();
            assertThat(entityLinks)
                    .anyMatch(el -> ScopeTypes.BPMN.equals(el.getReferenceScopeType())
                            && childProcessInstance.getId().equals(el.getReferenceScopeId()));

            // Historic entity link to the child BPMN process should NOT exist because the child process has historyLevel=none
            List<HistoricEntityLink> historicEntityLinks = cmmnHistoryService
                    .getHistoricEntityLinkChildrenForCaseInstance(caseInstance.getId());
            assertThat(historicEntityLinks)
                    .noneMatch(el -> ScopeTypes.BPMN.equals(el.getReferenceScopeType())
                            && childProcessInstance.getId().equals(el.getReferenceScopeId()));

        } finally {
            cmmnRepositoryService.deleteDeployment(cmmnDeployment.getId(), true);
            processEngineRepositoryService.deleteDeployment(bpmnDeployment.getId(), true);
        }
    }

    @Test
    public void testBpmnParentWithBpmnChildHistoryNone() {
        Deployment childDeployment = processEngineRepositoryService.createDeployment()
                .addClasspathResource("org/flowable/cmmn/test/EntityLinkHistoryLevelTest.oneTaskProcessHistoryNone.bpmn20.xml")
                .deploy();
        Deployment parentDeployment = processEngineRepositoryService.createDeployment()
                .addClasspathResource("org/flowable/cmmn/test/EntityLinkHistoryLevelTest.callActivityProcessHistoryNoneChild.bpmn20.xml")
                .deploy();

        try {
            ProcessInstance processInstance = processEngineRuntimeService.startProcessInstanceByKey("callActivityHistoryNoneChild");

            // The call activity should have started a child process instance
            ProcessInstance childProcessInstance = processEngineRuntimeService.createProcessInstanceQuery()
                    .superProcessInstanceId(processInstance.getId())
                    .singleResult();
            assertThat(childProcessInstance).isNotNull();

            // Runtime entity links should exist (parent process -> child process)
            List<EntityLink> entityLinks = processEngineRuntimeService.getEntityLinkChildrenForProcessInstance(processInstance.getId());
            assertThat(entityLinks).isNotEmpty();
            assertThat(entityLinks)
                    .anyMatch(el -> ScopeTypes.BPMN.equals(el.getReferenceScopeType())
                            && childProcessInstance.getId().equals(el.getReferenceScopeId()));

            // Historic entity link to the child BPMN process should NOT exist because the child process has historyLevel=none
            List<HistoricEntityLink> historicEntityLinks = processEngineHistoryService
                    .getHistoricEntityLinkChildrenForProcessInstance(processInstance.getId());
            assertThat(historicEntityLinks)
                    .noneMatch(el -> ScopeTypes.BPMN.equals(el.getReferenceScopeType())
                            && childProcessInstance.getId().equals(el.getReferenceScopeId()));

        } finally {
            processEngineRepositoryService.deleteDeployment(parentDeployment.getId(), true);
            processEngineRepositoryService.deleteDeployment(childDeployment.getId(), true);
        }
    }

    @Test
    public void testCmmnParentWithCmmnChildHistoryNone() {
        CmmnDeployment childDeployment = cmmnRepositoryService.createDeployment()
                .addClasspathResource("org/flowable/cmmn/test/EntityLinkHistoryLevelTest.oneHumanTaskCaseHistoryNone.cmmn")
                .deploy();
        CmmnDeployment parentDeployment = cmmnRepositoryService.createDeployment()
                .addClasspathResource("org/flowable/cmmn/test/EntityLinkHistoryLevelTest.caseTaskCaseHistoryNoneChild.cmmn")
                .deploy();

        try {
            CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                    .caseDefinitionKey("caseTaskCaseHistoryNoneChild")
                    .start();

            // The case task should have started a child case instance
            CaseInstance childCaseInstance = cmmnRuntimeService.createCaseInstanceQuery()
                    .caseInstanceParentId(caseInstance.getId())
                    .singleResult();
            assertThat(childCaseInstance).isNotNull();

            // Runtime entity links should exist (parent case -> child case)
            List<EntityLink> entityLinks = cmmnRuntimeService.getEntityLinkChildrenForCaseInstance(caseInstance.getId());
            assertThat(entityLinks).isNotEmpty();
            assertThat(entityLinks)
                    .anyMatch(el -> ScopeTypes.CMMN.equals(el.getReferenceScopeType())
                            && childCaseInstance.getId().equals(el.getReferenceScopeId()));

            // Historic entity link to the child CMMN case should NOT exist because the child case has historyLevel=none
            List<HistoricEntityLink> historicEntityLinks = cmmnHistoryService
                    .getHistoricEntityLinkChildrenForCaseInstance(caseInstance.getId());
            assertThat(historicEntityLinks)
                    .noneMatch(el -> ScopeTypes.CMMN.equals(el.getReferenceScopeType())
                            && childCaseInstance.getId().equals(el.getReferenceScopeId()));

        } finally {
            cmmnRepositoryService.deleteDeployment(parentDeployment.getId(), true);
            cmmnRepositoryService.deleteDeployment(childDeployment.getId(), true);
        }
    }
}
