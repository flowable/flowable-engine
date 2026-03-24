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
import org.flowable.task.api.Task;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests that historic entity links are correctly created or suppressed based on the history level
 * of parent and child sub-instances.
 */
public class EntityLinkHistoryLevelTest extends AbstractProcessEngineIntegrationTest {

    @Before
    public void enableDefinitionHistoryLevel() {
        processEngineConfiguration.setEnableProcessDefinitionHistoryLevel(true);
        cmmnEngineConfiguration.setEnableCaseDefinitionHistoryLevel(true);
    }

    @After
    public void disableDefinitionHistoryLevel() {
        processEngineConfiguration.setEnableProcessDefinitionHistoryLevel(false);
        cmmnEngineConfiguration.setEnableCaseDefinitionHistoryLevel(false);
    }


    // Child history NONE tests

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

            CaseInstance childCaseInstance = cmmnRuntimeService.createCaseInstanceQuery().singleResult();
            assertThat(childCaseInstance).isNotNull();

            Task childTask = cmmnTaskService.createTaskQuery().caseInstanceId(childCaseInstance.getId()).singleResult();
            assertThat(childTask).isNotNull();

            // Runtime entity links should exist (parent process -> child case)
            List<EntityLink> entityLinks = processEngineRuntimeService.getEntityLinkChildrenForProcessInstance(processInstance.getId());
            assertThat(entityLinks)
                    .filteredOn(el -> ScopeTypes.CMMN.equals(el.getReferenceScopeType())
                            && childCaseInstance.getId().equals(el.getReferenceScopeId()))
                    .hasSize(1);
            assertThat(entityLinks)
                    .filteredOn(el -> ScopeTypes.TASK.equals(el.getReferenceScopeType())
                            && childTask.getId().equals(el.getReferenceScopeId()))
                    .hasSize(1);

            // Historic entity link to the child CMMN case should NOT exist because the child case has historyLevel=none
            List<HistoricEntityLink> historicEntityLinks = processEngineHistoryService
                    .getHistoricEntityLinkChildrenForProcessInstance(processInstance.getId());
            assertThat(historicEntityLinks)
                    .filteredOn(el -> ScopeTypes.CMMN.equals(el.getReferenceScopeType())
                            && childCaseInstance.getId().equals(el.getReferenceScopeId()))
                    .isEmpty();
            assertThat(historicEntityLinks)
                    .filteredOn(el -> ScopeTypes.TASK.equals(el.getReferenceScopeType())
                            && childTask.getId().equals(el.getReferenceScopeId()))
                    .isEmpty();

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

            ProcessInstance childProcessInstance = processEngineRuntimeService.createProcessInstanceQuery().singleResult();
            assertThat(childProcessInstance).isNotNull();

            Task childTask = processEngineTaskService.createTaskQuery().processInstanceId(childProcessInstance.getId()).singleResult();
            assertThat(childTask).isNotNull();

            // Runtime entity links should exist (parent case -> child process)
            List<EntityLink> entityLinks = cmmnRuntimeService.getEntityLinkChildrenForCaseInstance(caseInstance.getId());
            assertThat(entityLinks)
                    .filteredOn(el -> ScopeTypes.BPMN.equals(el.getReferenceScopeType())
                            && childProcessInstance.getId().equals(el.getReferenceScopeId()))
                    .hasSize(1);
            assertThat(entityLinks)
                    .filteredOn(el -> ScopeTypes.TASK.equals(el.getReferenceScopeType())
                            && childTask.getId().equals(el.getReferenceScopeId()))
                    .hasSize(1);

            // Historic entity link to the child BPMN process should NOT exist because the child process has historyLevel=none
            List<HistoricEntityLink> historicEntityLinks = cmmnHistoryService
                    .getHistoricEntityLinkChildrenForCaseInstance(caseInstance.getId());
            assertThat(historicEntityLinks)
                    .filteredOn(el -> ScopeTypes.BPMN.equals(el.getReferenceScopeType())
                            && childProcessInstance.getId().equals(el.getReferenceScopeId()))
                    .isEmpty();
            assertThat(historicEntityLinks)
                    .filteredOn(el -> ScopeTypes.TASK.equals(el.getReferenceScopeType())
                            && childTask.getId().equals(el.getReferenceScopeId()))
                    .isEmpty();

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

            ProcessInstance childProcessInstance = processEngineRuntimeService.createProcessInstanceQuery()
                    .superProcessInstanceId(processInstance.getId())
                    .singleResult();
            assertThat(childProcessInstance).isNotNull();

            Task childTask = processEngineTaskService.createTaskQuery().processInstanceId(childProcessInstance.getId()).singleResult();
            assertThat(childTask).isNotNull();

            // Runtime entity links should exist (parent process -> child process)
            List<EntityLink> entityLinks = processEngineRuntimeService.getEntityLinkChildrenForProcessInstance(processInstance.getId());
            assertThat(entityLinks)
                    .filteredOn(el -> ScopeTypes.BPMN.equals(el.getReferenceScopeType())
                            && childProcessInstance.getId().equals(el.getReferenceScopeId()))
                    .hasSize(1);
            assertThat(entityLinks)
                    .filteredOn(el -> ScopeTypes.TASK.equals(el.getReferenceScopeType())
                            && childTask.getId().equals(el.getReferenceScopeId()))
                    .hasSize(1);

            // Historic entity link to the child BPMN process should NOT exist because the child process has historyLevel=none
            List<HistoricEntityLink> historicEntityLinks = processEngineHistoryService
                    .getHistoricEntityLinkChildrenForProcessInstance(processInstance.getId());
            assertThat(historicEntityLinks)
                    .filteredOn(el -> ScopeTypes.BPMN.equals(el.getReferenceScopeType())
                            && childProcessInstance.getId().equals(el.getReferenceScopeId()))
                    .isEmpty();
            assertThat(historicEntityLinks)
                    .filteredOn(el -> ScopeTypes.TASK.equals(el.getReferenceScopeType())
                            && childTask.getId().equals(el.getReferenceScopeId()))
                    .isEmpty();

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

            CaseInstance childCaseInstance = cmmnRuntimeService.createCaseInstanceQuery()
                    .caseInstanceParentId(caseInstance.getId())
                    .singleResult();
            assertThat(childCaseInstance).isNotNull();

            Task childTask = cmmnTaskService.createTaskQuery().caseInstanceId(childCaseInstance.getId()).singleResult();
            assertThat(childTask).isNotNull();

            // Runtime entity links should exist (parent case -> child case)
            List<EntityLink> entityLinks = cmmnRuntimeService.getEntityLinkChildrenForCaseInstance(caseInstance.getId());
            assertThat(entityLinks)
                    .filteredOn(el -> ScopeTypes.CMMN.equals(el.getReferenceScopeType())
                            && childCaseInstance.getId().equals(el.getReferenceScopeId()))
                    .hasSize(1);
            assertThat(entityLinks)
                    .filteredOn(el -> ScopeTypes.TASK.equals(el.getReferenceScopeType())
                            && childTask.getId().equals(el.getReferenceScopeId()))
                    .hasSize(1);

            // Historic entity link to the child CMMN case should NOT exist because the child case has historyLevel=none
            List<HistoricEntityLink> historicEntityLinks = cmmnHistoryService
                    .getHistoricEntityLinkChildrenForCaseInstance(caseInstance.getId());
            assertThat(historicEntityLinks)
                    .filteredOn(el -> ScopeTypes.CMMN.equals(el.getReferenceScopeType())
                            && childCaseInstance.getId().equals(el.getReferenceScopeId()))
                    .isEmpty();
            assertThat(historicEntityLinks)
                    .filteredOn(el -> ScopeTypes.TASK.equals(el.getReferenceScopeType())
                            && childTask.getId().equals(el.getReferenceScopeId()))
                    .isEmpty();

        } finally {
            cmmnRepositoryService.deleteDeployment(parentDeployment.getId(), true);
            cmmnRepositoryService.deleteDeployment(childDeployment.getId(), true);
        }
    }

    // History enabled tests

    @Test
    public void testBpmnParentWithCmmnChildHistoryEnabled() {
        CmmnDeployment cmmnDeployment = cmmnRepositoryService.createDeployment()
                .addClasspathResource("org/flowable/cmmn/test/EntityLinkHistoryLevelTest.oneHumanTaskCase.cmmn")
                .deploy();
        Deployment bpmnDeployment = processEngineRepositoryService.createDeployment()
                .addClasspathResource("org/flowable/cmmn/test/EntityLinkHistoryLevelTest.caseTaskProcess.bpmn20.xml")
                .deploy();

        try {
            ProcessInstance processInstance = processEngineRuntimeService.startProcessInstanceByKey("caseTaskProcess");

            CaseInstance childCaseInstance = cmmnRuntimeService.createCaseInstanceQuery().singleResult();
            assertThat(childCaseInstance).isNotNull();

            Task childTask = cmmnTaskService.createTaskQuery().caseInstanceId(childCaseInstance.getId()).singleResult();
            assertThat(childTask).isNotNull();

            // Runtime entity links should exist (parent process -> child case)
            List<EntityLink> entityLinks = processEngineRuntimeService.getEntityLinkChildrenForProcessInstance(processInstance.getId());
            assertThat(entityLinks)
                    .filteredOn(el -> ScopeTypes.CMMN.equals(el.getReferenceScopeType())
                            && childCaseInstance.getId().equals(el.getReferenceScopeId()))
                    .hasSize(1);
            assertThat(entityLinks)
                    .filteredOn(el -> ScopeTypes.TASK.equals(el.getReferenceScopeType())
                            && childTask.getId().equals(el.getReferenceScopeId()))
                    .hasSize(1);

            // Historic entity links should also exist because child case has default history
            List<HistoricEntityLink> historicEntityLinks = processEngineHistoryService
                    .getHistoricEntityLinkChildrenForProcessInstance(processInstance.getId());
            assertThat(historicEntityLinks)
                    .filteredOn(el -> ScopeTypes.CMMN.equals(el.getReferenceScopeType())
                            && childCaseInstance.getId().equals(el.getReferenceScopeId()))
                    .hasSize(1);
            assertThat(historicEntityLinks)
                    .filteredOn(el -> ScopeTypes.TASK.equals(el.getReferenceScopeType())
                            && childTask.getId().equals(el.getReferenceScopeId()))
                    .hasSize(1);

        } finally {
            cmmnRepositoryService.deleteDeployment(cmmnDeployment.getId(), true);
            processEngineRepositoryService.deleteDeployment(bpmnDeployment.getId(), true);
        }
    }

    @Test
    public void testCmmnParentWithBpmnChildHistoryEnabled() {
        CmmnDeployment cmmnDeployment = cmmnRepositoryService.createDeployment()
                .addClasspathResource("org/flowable/cmmn/test/EntityLinkHistoryLevelTest.processTaskCase.cmmn")
                .deploy();
        Deployment bpmnDeployment = processEngineRepositoryService.createDeployment()
                .addClasspathResource("org/flowable/cmmn/test/EntityLinkHistoryLevelTest.oneTaskProcess.bpmn20.xml")
                .deploy();

        try {
            CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                    .caseDefinitionKey("processTaskCase")
                    .start();

            ProcessInstance childProcessInstance = processEngineRuntimeService.createProcessInstanceQuery().singleResult();
            assertThat(childProcessInstance).isNotNull();

            Task childTask = processEngineTaskService.createTaskQuery().processInstanceId(childProcessInstance.getId()).singleResult();
            assertThat(childTask).isNotNull();

            // Runtime entity links should exist (parent case -> child process)
            List<EntityLink> entityLinks = cmmnRuntimeService.getEntityLinkChildrenForCaseInstance(caseInstance.getId());
            assertThat(entityLinks)
                    .filteredOn(el -> ScopeTypes.BPMN.equals(el.getReferenceScopeType())
                            && childProcessInstance.getId().equals(el.getReferenceScopeId()))
                    .hasSize(1);
            assertThat(entityLinks)
                    .filteredOn(el -> ScopeTypes.TASK.equals(el.getReferenceScopeType())
                            && childTask.getId().equals(el.getReferenceScopeId()))
                    .hasSize(1);

            // Historic entity links should also exist because child process has default history
            List<HistoricEntityLink> historicEntityLinks = cmmnHistoryService
                    .getHistoricEntityLinkChildrenForCaseInstance(caseInstance.getId());
            assertThat(historicEntityLinks)
                    .filteredOn(el -> ScopeTypes.BPMN.equals(el.getReferenceScopeType())
                            && childProcessInstance.getId().equals(el.getReferenceScopeId()))
                    .hasSize(1);
            assertThat(historicEntityLinks)
                    .filteredOn(el -> ScopeTypes.TASK.equals(el.getReferenceScopeType())
                            && childTask.getId().equals(el.getReferenceScopeId()))
                    .hasSize(1);

        } finally {
            cmmnRepositoryService.deleteDeployment(cmmnDeployment.getId(), true);
            processEngineRepositoryService.deleteDeployment(bpmnDeployment.getId(), true);
        }
    }

    @Test
    public void testBpmnParentWithBpmnChildHistoryEnabled() {
        Deployment childDeployment = processEngineRepositoryService.createDeployment()
                .addClasspathResource("org/flowable/cmmn/test/EntityLinkHistoryLevelTest.oneTaskProcess.bpmn20.xml")
                .deploy();
        Deployment parentDeployment = processEngineRepositoryService.createDeployment()
                .addClasspathResource("org/flowable/cmmn/test/EntityLinkHistoryLevelTest.callActivityProcess.bpmn20.xml")
                .deploy();

        try {
            ProcessInstance processInstance = processEngineRuntimeService.startProcessInstanceByKey("callActivityProcess");

            ProcessInstance childProcessInstance = processEngineRuntimeService.createProcessInstanceQuery()
                    .superProcessInstanceId(processInstance.getId())
                    .singleResult();
            assertThat(childProcessInstance).isNotNull();

            Task childTask = processEngineTaskService.createTaskQuery().processInstanceId(childProcessInstance.getId()).singleResult();
            assertThat(childTask).isNotNull();

            // Runtime entity links should exist (parent process -> child process)
            List<EntityLink> entityLinks = processEngineRuntimeService.getEntityLinkChildrenForProcessInstance(processInstance.getId());
            assertThat(entityLinks)
                    .filteredOn(el -> ScopeTypes.BPMN.equals(el.getReferenceScopeType())
                            && childProcessInstance.getId().equals(el.getReferenceScopeId()))
                    .hasSize(1);
            assertThat(entityLinks)
                    .filteredOn(el -> ScopeTypes.TASK.equals(el.getReferenceScopeType())
                            && childTask.getId().equals(el.getReferenceScopeId()))
                    .hasSize(1);

            // Historic entity links should also exist because child process has default history
            List<HistoricEntityLink> historicEntityLinks = processEngineHistoryService
                    .getHistoricEntityLinkChildrenForProcessInstance(processInstance.getId());
            assertThat(historicEntityLinks)
                    .filteredOn(el -> ScopeTypes.BPMN.equals(el.getReferenceScopeType())
                            && childProcessInstance.getId().equals(el.getReferenceScopeId()))
                    .hasSize(1);
            assertThat(historicEntityLinks)
                    .filteredOn(el -> ScopeTypes.TASK.equals(el.getReferenceScopeType())
                            && childTask.getId().equals(el.getReferenceScopeId()))
                    .hasSize(1);

        } finally {
            processEngineRepositoryService.deleteDeployment(parentDeployment.getId(), true);
            processEngineRepositoryService.deleteDeployment(childDeployment.getId(), true);
        }
    }

    @Test
    public void testCmmnParentWithCmmnChildHistoryEnabled() {
        CmmnDeployment childDeployment = cmmnRepositoryService.createDeployment()
                .addClasspathResource("org/flowable/cmmn/test/EntityLinkHistoryLevelTest.oneHumanTaskCase.cmmn")
                .deploy();
        CmmnDeployment parentDeployment = cmmnRepositoryService.createDeployment()
                .addClasspathResource("org/flowable/cmmn/test/EntityLinkHistoryLevelTest.caseTaskCase.cmmn")
                .deploy();

        try {
            CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                    .caseDefinitionKey("caseTaskCase")
                    .start();

            CaseInstance childCaseInstance = cmmnRuntimeService.createCaseInstanceQuery()
                    .caseInstanceParentId(caseInstance.getId())
                    .singleResult();
            assertThat(childCaseInstance).isNotNull();

            Task childTask = cmmnTaskService.createTaskQuery().caseInstanceId(childCaseInstance.getId()).singleResult();
            assertThat(childTask).isNotNull();

            // Runtime entity links should exist (parent case -> child case)
            List<EntityLink> entityLinks = cmmnRuntimeService.getEntityLinkChildrenForCaseInstance(caseInstance.getId());
            assertThat(entityLinks)
                    .filteredOn(el -> ScopeTypes.CMMN.equals(el.getReferenceScopeType())
                            && childCaseInstance.getId().equals(el.getReferenceScopeId()))
                    .hasSize(1);
            assertThat(entityLinks)
                    .filteredOn(el -> ScopeTypes.TASK.equals(el.getReferenceScopeType())
                            && childTask.getId().equals(el.getReferenceScopeId()))
                    .hasSize(1);

            // Historic entity links should also exist because child case has default history
            List<HistoricEntityLink> historicEntityLinks = cmmnHistoryService
                    .getHistoricEntityLinkChildrenForCaseInstance(caseInstance.getId());
            assertThat(historicEntityLinks)
                    .filteredOn(el -> ScopeTypes.CMMN.equals(el.getReferenceScopeType())
                            && childCaseInstance.getId().equals(el.getReferenceScopeId()))
                    .hasSize(1);
            assertThat(historicEntityLinks)
                    .filteredOn(el -> ScopeTypes.TASK.equals(el.getReferenceScopeType())
                            && childTask.getId().equals(el.getReferenceScopeId()))
                    .hasSize(1);

        } finally {
            cmmnRepositoryService.deleteDeployment(parentDeployment.getId(), true);
            cmmnRepositoryService.deleteDeployment(childDeployment.getId(), true);
        }
    }

    // Parent history NONE edge case tests

    @Test
    public void testBpmnParentHistoryNoneWithCmmnChildHistoryEnabled() {
        CmmnDeployment cmmnDeployment = cmmnRepositoryService.createDeployment()
                .addClasspathResource("org/flowable/cmmn/test/EntityLinkHistoryLevelTest.oneHumanTaskCase.cmmn")
                .deploy();
        Deployment bpmnDeployment = processEngineRepositoryService.createDeployment()
                .addClasspathResource("org/flowable/cmmn/test/EntityLinkHistoryLevelTest.caseTaskProcessHistoryNoneParent.bpmn20.xml")
                .deploy();

        try {
            ProcessInstance processInstance = processEngineRuntimeService.startProcessInstanceByKey("caseTaskHistoryNoneParent");

            CaseInstance childCaseInstance = cmmnRuntimeService.createCaseInstanceQuery().singleResult();
            assertThat(childCaseInstance).isNotNull();

            Task childTask = cmmnTaskService.createTaskQuery().caseInstanceId(childCaseInstance.getId()).singleResult();
            assertThat(childTask).isNotNull();

            // Runtime entity links should exist
            List<EntityLink> entityLinks = processEngineRuntimeService.getEntityLinkChildrenForProcessInstance(processInstance.getId());
            assertThat(entityLinks)
                    .filteredOn(el -> ScopeTypes.CMMN.equals(el.getReferenceScopeType())
                            && childCaseInstance.getId().equals(el.getReferenceScopeId()))
                    .hasSize(1);
            assertThat(entityLinks)
                    .filteredOn(el -> ScopeTypes.TASK.equals(el.getReferenceScopeType())
                            && childTask.getId().equals(el.getReferenceScopeId()))
                    .hasSize(1);

            // Historic entity links should NOT exist because parent has historyLevel=none
            List<HistoricEntityLink> historicEntityLinks = processEngineHistoryService
                    .getHistoricEntityLinkChildrenForProcessInstance(processInstance.getId());
            assertThat(historicEntityLinks)
                    .filteredOn(el -> ScopeTypes.CMMN.equals(el.getReferenceScopeType())
                            && childCaseInstance.getId().equals(el.getReferenceScopeId()))
                    .isEmpty();
            assertThat(historicEntityLinks)
                    .filteredOn(el -> ScopeTypes.TASK.equals(el.getReferenceScopeType())
                            && childTask.getId().equals(el.getReferenceScopeId()))
                    .isEmpty();

        } finally {
            cmmnRepositoryService.deleteDeployment(cmmnDeployment.getId(), true);
            processEngineRepositoryService.deleteDeployment(bpmnDeployment.getId(), true);
        }
    }

    @Test
    public void testCmmnParentHistoryNoneWithBpmnChildHistoryEnabled() {
        CmmnDeployment cmmnDeployment = cmmnRepositoryService.createDeployment()
                .addClasspathResource("org/flowable/cmmn/test/EntityLinkHistoryLevelTest.processTaskCaseHistoryNoneParent.cmmn")
                .deploy();
        Deployment bpmnDeployment = processEngineRepositoryService.createDeployment()
                .addClasspathResource("org/flowable/cmmn/test/EntityLinkHistoryLevelTest.oneTaskProcess.bpmn20.xml")
                .deploy();

        try {
            CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                    .caseDefinitionKey("processTaskCaseHistoryNoneParent")
                    .start();

            ProcessInstance childProcessInstance = processEngineRuntimeService.createProcessInstanceQuery().singleResult();
            assertThat(childProcessInstance).isNotNull();

            Task childTask = processEngineTaskService.createTaskQuery().processInstanceId(childProcessInstance.getId()).singleResult();
            assertThat(childTask).isNotNull();

            // Runtime entity links should exist
            List<EntityLink> entityLinks = cmmnRuntimeService.getEntityLinkChildrenForCaseInstance(caseInstance.getId());
            assertThat(entityLinks)
                    .filteredOn(el -> ScopeTypes.BPMN.equals(el.getReferenceScopeType())
                            && childProcessInstance.getId().equals(el.getReferenceScopeId()))
                    .hasSize(1);
            assertThat(entityLinks)
                    .filteredOn(el -> ScopeTypes.TASK.equals(el.getReferenceScopeType())
                            && childTask.getId().equals(el.getReferenceScopeId()))
                    .hasSize(1);

            // Historic entity links should NOT exist because parent has historyLevel=none
            List<HistoricEntityLink> historicEntityLinks = cmmnHistoryService
                    .getHistoricEntityLinkChildrenForCaseInstance(caseInstance.getId());
            assertThat(historicEntityLinks)
                    .filteredOn(el -> ScopeTypes.BPMN.equals(el.getReferenceScopeType())
                            && childProcessInstance.getId().equals(el.getReferenceScopeId()))
                    .isEmpty();
            assertThat(historicEntityLinks)
                    .filteredOn(el -> ScopeTypes.TASK.equals(el.getReferenceScopeType())
                            && childTask.getId().equals(el.getReferenceScopeId()))
                    .isEmpty();

        } finally {
            cmmnRepositoryService.deleteDeployment(cmmnDeployment.getId(), true);
            processEngineRepositoryService.deleteDeployment(bpmnDeployment.getId(), true);
        }
    }

    @Test
    public void testBpmnParentHistoryNoneWithCmmnChildHistoryNone() {
        CmmnDeployment cmmnDeployment = cmmnRepositoryService.createDeployment()
                .addClasspathResource("org/flowable/cmmn/test/EntityLinkHistoryLevelTest.oneHumanTaskCaseHistoryNone.cmmn")
                .deploy();
        Deployment bpmnDeployment = processEngineRepositoryService.createDeployment()
                .addClasspathResource("org/flowable/cmmn/test/EntityLinkHistoryLevelTest.caseTaskProcessHistoryNoneParentAndChild.bpmn20.xml")
                .deploy();

        try {
            ProcessInstance processInstance = processEngineRuntimeService.startProcessInstanceByKey("caseTaskHistoryNoneParentAndChild");

            CaseInstance childCaseInstance = cmmnRuntimeService.createCaseInstanceQuery().singleResult();
            assertThat(childCaseInstance).isNotNull();

            Task childTask = cmmnTaskService.createTaskQuery().caseInstanceId(childCaseInstance.getId()).singleResult();
            assertThat(childTask).isNotNull();

            // Runtime entity links should exist
            List<EntityLink> entityLinks = processEngineRuntimeService.getEntityLinkChildrenForProcessInstance(processInstance.getId());
            assertThat(entityLinks)
                    .filteredOn(el -> ScopeTypes.CMMN.equals(el.getReferenceScopeType())
                            && childCaseInstance.getId().equals(el.getReferenceScopeId()))
                    .hasSize(1);
            assertThat(entityLinks)
                    .filteredOn(el -> ScopeTypes.TASK.equals(el.getReferenceScopeType())
                            && childTask.getId().equals(el.getReferenceScopeId()))
                    .hasSize(1);

            // Historic entity links should NOT exist because both parent and child have historyLevel=none
            List<HistoricEntityLink> historicEntityLinks = processEngineHistoryService
                    .getHistoricEntityLinkChildrenForProcessInstance(processInstance.getId());
            assertThat(historicEntityLinks)
                    .filteredOn(el -> ScopeTypes.CMMN.equals(el.getReferenceScopeType())
                            && childCaseInstance.getId().equals(el.getReferenceScopeId()))
                    .isEmpty();
            assertThat(historicEntityLinks)
                    .filteredOn(el -> ScopeTypes.TASK.equals(el.getReferenceScopeType())
                            && childTask.getId().equals(el.getReferenceScopeId()))
                    .isEmpty();

        } finally {
            cmmnRepositoryService.deleteDeployment(cmmnDeployment.getId(), true);
            processEngineRepositoryService.deleteDeployment(bpmnDeployment.getId(), true);
        }
    }

    @Test
    public void testCmmnParentHistoryNoneWithBpmnChildHistoryNone() {
        CmmnDeployment cmmnDeployment = cmmnRepositoryService.createDeployment()
                .addClasspathResource("org/flowable/cmmn/test/EntityLinkHistoryLevelTest.processTaskCaseHistoryNoneParentAndChild.cmmn")
                .deploy();
        Deployment bpmnDeployment = processEngineRepositoryService.createDeployment()
                .addClasspathResource("org/flowable/cmmn/test/EntityLinkHistoryLevelTest.oneTaskProcessHistoryNone.bpmn20.xml")
                .deploy();

        try {
            CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                    .caseDefinitionKey("processTaskCaseHistoryNoneParentAndChild")
                    .start();

            ProcessInstance childProcessInstance = processEngineRuntimeService.createProcessInstanceQuery().singleResult();
            assertThat(childProcessInstance).isNotNull();

            Task childTask = processEngineTaskService.createTaskQuery().processInstanceId(childProcessInstance.getId()).singleResult();
            assertThat(childTask).isNotNull();

            // Runtime entity links should exist
            List<EntityLink> entityLinks = cmmnRuntimeService.getEntityLinkChildrenForCaseInstance(caseInstance.getId());
            assertThat(entityLinks)
                    .filteredOn(el -> ScopeTypes.BPMN.equals(el.getReferenceScopeType())
                            && childProcessInstance.getId().equals(el.getReferenceScopeId()))
                    .hasSize(1);
            assertThat(entityLinks)
                    .filteredOn(el -> ScopeTypes.TASK.equals(el.getReferenceScopeType())
                            && childTask.getId().equals(el.getReferenceScopeId()))
                    .hasSize(1);

            // Historic entity links should NOT exist because both parent and child have historyLevel=none
            List<HistoricEntityLink> historicEntityLinks = cmmnHistoryService
                    .getHistoricEntityLinkChildrenForCaseInstance(caseInstance.getId());
            assertThat(historicEntityLinks)
                    .filteredOn(el -> ScopeTypes.BPMN.equals(el.getReferenceScopeType())
                            && childProcessInstance.getId().equals(el.getReferenceScopeId()))
                    .isEmpty();
            assertThat(historicEntityLinks)
                    .filteredOn(el -> ScopeTypes.TASK.equals(el.getReferenceScopeType())
                            && childTask.getId().equals(el.getReferenceScopeId()))
                    .isEmpty();

        } finally {
            cmmnRepositoryService.deleteDeployment(cmmnDeployment.getId(), true);
            processEngineRepositoryService.deleteDeployment(bpmnDeployment.getId(), true);
        }
    }

    // Cross-engine deep nesting tests (3-level chains)

    @Test
    public void testCmmnParentWithCallActivityGrandchildHistoryEnabled() {
        // Chain: Case -> Process (call activity) -> Grandchild Process (all history enabled)
        Deployment grandchildDeployment = processEngineRepositoryService.createDeployment()
                .addClasspathResource("org/flowable/cmmn/test/EntityLinkHistoryLevelTest.oneTaskProcess.bpmn20.xml")
                .deploy();
        Deployment childDeployment = processEngineRepositoryService.createDeployment()
                .addClasspathResource("org/flowable/cmmn/test/EntityLinkHistoryLevelTest.callActivityProcess.bpmn20.xml")
                .deploy();
        CmmnDeployment cmmnDeployment = cmmnRepositoryService.createDeployment()
                .addClasspathResource("org/flowable/cmmn/test/EntityLinkHistoryLevelTest.processTaskCaseWithCallActivity.cmmn")
                .deploy();

        try {
            CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                    .caseDefinitionKey("processTaskCaseWithCallActivity")
                    .start();

            ProcessInstance childProcessInstance = processEngineRuntimeService.createProcessInstanceQuery()
                    .processDefinitionKey("callActivityProcess")
                    .singleResult();
            assertThat(childProcessInstance).isNotNull();

            ProcessInstance grandchildProcessInstance = processEngineRuntimeService.createProcessInstanceQuery()
                    .superProcessInstanceId(childProcessInstance.getId())
                    .singleResult();
            assertThat(grandchildProcessInstance).isNotNull();

            Task grandchildTask = processEngineTaskService.createTaskQuery()
                    .processInstanceId(grandchildProcessInstance.getId())
                    .singleResult();
            assertThat(grandchildTask).isNotNull();

            // Runtime entity links on the root case should include both child and grandchild
            List<EntityLink> entityLinks = cmmnRuntimeService.getEntityLinkChildrenForCaseInstance(caseInstance.getId());
            assertThat(entityLinks)
                    .filteredOn(el -> ScopeTypes.BPMN.equals(el.getReferenceScopeType())
                            && childProcessInstance.getId().equals(el.getReferenceScopeId()))
                    .hasSize(1);
            assertThat(entityLinks)
                    .filteredOn(el -> ScopeTypes.BPMN.equals(el.getReferenceScopeType())
                            && grandchildProcessInstance.getId().equals(el.getReferenceScopeId()))
                    .hasSize(1);
            assertThat(entityLinks)
                    .filteredOn(el -> ScopeTypes.TASK.equals(el.getReferenceScopeType())
                            && grandchildTask.getId().equals(el.getReferenceScopeId()))
                    .hasSize(1);

            // Historic entity links should exist for both (all history enabled)
            List<HistoricEntityLink> historicEntityLinks = cmmnHistoryService
                    .getHistoricEntityLinkChildrenForCaseInstance(caseInstance.getId());
            assertThat(historicEntityLinks)
                    .filteredOn(el -> ScopeTypes.BPMN.equals(el.getReferenceScopeType())
                            && childProcessInstance.getId().equals(el.getReferenceScopeId()))
                    .hasSize(1);
            assertThat(historicEntityLinks)
                    .filteredOn(el -> ScopeTypes.BPMN.equals(el.getReferenceScopeType())
                            && grandchildProcessInstance.getId().equals(el.getReferenceScopeId()))
                    .hasSize(1);
            assertThat(historicEntityLinks)
                    .filteredOn(el -> ScopeTypes.TASK.equals(el.getReferenceScopeType())
                            && grandchildTask.getId().equals(el.getReferenceScopeId()))
                    .hasSize(1);

        } finally {
            cmmnRepositoryService.deleteDeployment(cmmnDeployment.getId(), true);
            processEngineRepositoryService.deleteDeployment(childDeployment.getId(), true);
            processEngineRepositoryService.deleteDeployment(grandchildDeployment.getId(), true);
        }
    }

    @Test
    public void testCmmnParentHistoryNoneWithCallActivityGrandchild() {
        // Chain: Case (NONE) -> Process (call activity) -> Grandchild Process
        // Cross-engine: BPMN engine must delegate to CMMN engine for the cmmn-scoped entity link on the grandchild
        Deployment grandchildDeployment = processEngineRepositoryService.createDeployment()
                .addClasspathResource("org/flowable/cmmn/test/EntityLinkHistoryLevelTest.oneTaskProcess.bpmn20.xml")
                .deploy();
        Deployment childDeployment = processEngineRepositoryService.createDeployment()
                .addClasspathResource("org/flowable/cmmn/test/EntityLinkHistoryLevelTest.callActivityProcess.bpmn20.xml")
                .deploy();
        CmmnDeployment cmmnDeployment = cmmnRepositoryService.createDeployment()
                .addClasspathResource("org/flowable/cmmn/test/EntityLinkHistoryLevelTest.processTaskCaseWithCallActivityHistoryNone.cmmn")
                .deploy();

        try {
            CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                    .caseDefinitionKey("processTaskCaseWithCallActivityHistoryNone")
                    .start();

            ProcessInstance childProcessInstance = processEngineRuntimeService.createProcessInstanceQuery()
                    .processDefinitionKey("callActivityProcess")
                    .singleResult();
            assertThat(childProcessInstance).isNotNull();

            ProcessInstance grandchildProcessInstance = processEngineRuntimeService.createProcessInstanceQuery()
                    .superProcessInstanceId(childProcessInstance.getId())
                    .singleResult();
            assertThat(grandchildProcessInstance).isNotNull();

            Task grandchildTask = processEngineTaskService.createTaskQuery()
                    .processInstanceId(grandchildProcessInstance.getId())
                    .singleResult();
            assertThat(grandchildTask).isNotNull();

            // Runtime entity links on the root case should include both child and grandchild
            List<EntityLink> entityLinks = cmmnRuntimeService.getEntityLinkChildrenForCaseInstance(caseInstance.getId());
            assertThat(entityLinks)
                    .filteredOn(el -> ScopeTypes.BPMN.equals(el.getReferenceScopeType())
                            && childProcessInstance.getId().equals(el.getReferenceScopeId()))
                    .hasSize(1);
            assertThat(entityLinks)
                    .filteredOn(el -> ScopeTypes.BPMN.equals(el.getReferenceScopeType())
                            && grandchildProcessInstance.getId().equals(el.getReferenceScopeId()))
                    .hasSize(1);
            assertThat(entityLinks)
                    .filteredOn(el -> ScopeTypes.TASK.equals(el.getReferenceScopeType())
                            && grandchildTask.getId().equals(el.getReferenceScopeId()))
                    .hasSize(1);

            // Historic entity links on root case should not exist (root case has history NONE)
            List<HistoricEntityLink> historicEntityLinks = cmmnHistoryService
                    .getHistoricEntityLinkChildrenForCaseInstance(caseInstance.getId());
            assertThat(historicEntityLinks)
                    .filteredOn(el -> ScopeTypes.BPMN.equals(el.getReferenceScopeType()))
                    .isEmpty();
            assertThat(historicEntityLinks)
                    .filteredOn(el -> ScopeTypes.TASK.equals(el.getReferenceScopeType()))
                    .isEmpty();

            // Historic entity links on child process should exist (child process -> grandchild, both have history enabled)
            List<HistoricEntityLink> childHistoricEntityLinks = processEngineHistoryService
                    .getHistoricEntityLinkChildrenForProcessInstance(childProcessInstance.getId());
            assertThat(childHistoricEntityLinks)
                    .filteredOn(el -> ScopeTypes.BPMN.equals(el.getReferenceScopeType())
                            && grandchildProcessInstance.getId().equals(el.getReferenceScopeId()))
                    .hasSize(1);
            assertThat(childHistoricEntityLinks)
                    .filteredOn(el -> ScopeTypes.TASK.equals(el.getReferenceScopeType())
                            && grandchildTask.getId().equals(el.getReferenceScopeId()))
                    .hasSize(1);

        } finally {
            cmmnRepositoryService.deleteDeployment(cmmnDeployment.getId(), true);
            processEngineRepositoryService.deleteDeployment(childDeployment.getId(), true);
            processEngineRepositoryService.deleteDeployment(grandchildDeployment.getId(), true);
        }
    }

    @Test
    public void testBpmnParentWithCaseTaskGrandchildHistoryEnabled() {
        // Chain: Process -> Case (case task) -> Grandchild Case (all history enabled)
        CmmnDeployment grandchildDeployment = cmmnRepositoryService.createDeployment()
                .addClasspathResource("org/flowable/cmmn/test/EntityLinkHistoryLevelTest.oneHumanTaskCase.cmmn")
                .deploy();
        CmmnDeployment childDeployment = cmmnRepositoryService.createDeployment()
                .addClasspathResource("org/flowable/cmmn/test/EntityLinkHistoryLevelTest.caseTaskCase.cmmn")
                .deploy();
        Deployment bpmnDeployment = processEngineRepositoryService.createDeployment()
                .addClasspathResource("org/flowable/cmmn/test/EntityLinkHistoryLevelTest.caseTaskProcessWithNestedCase.bpmn20.xml")
                .deploy();

        try {
            ProcessInstance processInstance = processEngineRuntimeService.startProcessInstanceByKey("caseTaskProcessWithNestedCase");

            CaseInstance childCaseInstance = cmmnRuntimeService.createCaseInstanceQuery()
                    .caseDefinitionKey("caseTaskCase")
                    .singleResult();
            assertThat(childCaseInstance).isNotNull();

            CaseInstance grandchildCaseInstance = cmmnRuntimeService.createCaseInstanceQuery()
                    .caseDefinitionKey("oneHumanTaskCase")
                    .singleResult();
            assertThat(grandchildCaseInstance).isNotNull();

            Task grandchildTask = cmmnTaskService.createTaskQuery()
                    .caseInstanceId(grandchildCaseInstance.getId())
                    .singleResult();
            assertThat(grandchildTask).isNotNull();

            // Runtime entity links on the root process should include both child and grandchild
            List<EntityLink> entityLinks = processEngineRuntimeService.getEntityLinkChildrenForProcessInstance(processInstance.getId());
            assertThat(entityLinks)
                    .filteredOn(el -> ScopeTypes.CMMN.equals(el.getReferenceScopeType())
                            && childCaseInstance.getId().equals(el.getReferenceScopeId()))
                    .hasSize(1);
            assertThat(entityLinks)
                    .filteredOn(el -> ScopeTypes.CMMN.equals(el.getReferenceScopeType())
                            && grandchildCaseInstance.getId().equals(el.getReferenceScopeId()))
                    .hasSize(1);
            assertThat(entityLinks)
                    .filteredOn(el -> ScopeTypes.TASK.equals(el.getReferenceScopeType())
                            && grandchildTask.getId().equals(el.getReferenceScopeId()))
                    .hasSize(1);

            // Historic entity links should exist for both (all history enabled)
            List<HistoricEntityLink> historicEntityLinks = processEngineHistoryService
                    .getHistoricEntityLinkChildrenForProcessInstance(processInstance.getId());
            assertThat(historicEntityLinks)
                    .filteredOn(el -> ScopeTypes.CMMN.equals(el.getReferenceScopeType())
                            && childCaseInstance.getId().equals(el.getReferenceScopeId()))
                    .hasSize(1);
            assertThat(historicEntityLinks)
                    .filteredOn(el -> ScopeTypes.CMMN.equals(el.getReferenceScopeType())
                            && grandchildCaseInstance.getId().equals(el.getReferenceScopeId()))
                    .hasSize(1);
            assertThat(historicEntityLinks)
                    .filteredOn(el -> ScopeTypes.TASK.equals(el.getReferenceScopeType())
                            && grandchildTask.getId().equals(el.getReferenceScopeId()))
                    .hasSize(1);

        } finally {
            processEngineRepositoryService.deleteDeployment(bpmnDeployment.getId(), true);
            cmmnRepositoryService.deleteDeployment(childDeployment.getId(), true);
            cmmnRepositoryService.deleteDeployment(grandchildDeployment.getId(), true);
        }
    }

    @Test
    public void testBpmnParentHistoryNoneWithCaseTaskGrandchild() {
        // Chain: Process (NONE) -> Case (case task) -> Grandchild Case
        // Cross-engine: CMMN engine must delegate to BPMN engine for the bpmn-scoped entity link on the grandchild
        CmmnDeployment grandchildDeployment = cmmnRepositoryService.createDeployment()
                .addClasspathResource("org/flowable/cmmn/test/EntityLinkHistoryLevelTest.oneHumanTaskCase.cmmn")
                .deploy();
        CmmnDeployment childDeployment = cmmnRepositoryService.createDeployment()
                .addClasspathResource("org/flowable/cmmn/test/EntityLinkHistoryLevelTest.caseTaskCase.cmmn")
                .deploy();
        Deployment bpmnDeployment = processEngineRepositoryService.createDeployment()
                .addClasspathResource("org/flowable/cmmn/test/EntityLinkHistoryLevelTest.caseTaskProcessWithNestedCaseHistoryNone.bpmn20.xml")
                .deploy();

        try {
            ProcessInstance processInstance = processEngineRuntimeService.startProcessInstanceByKey("caseTaskProcessWithNestedCaseHistoryNone");

            CaseInstance childCaseInstance = cmmnRuntimeService.createCaseInstanceQuery()
                    .caseDefinitionKey("caseTaskCase")
                    .singleResult();
            assertThat(childCaseInstance).isNotNull();

            CaseInstance grandchildCaseInstance = cmmnRuntimeService.createCaseInstanceQuery()
                    .caseDefinitionKey("oneHumanTaskCase")
                    .singleResult();
            assertThat(grandchildCaseInstance).isNotNull();

            Task grandchildTask = cmmnTaskService.createTaskQuery()
                    .caseInstanceId(grandchildCaseInstance.getId())
                    .singleResult();
            assertThat(grandchildTask).isNotNull();

            // Runtime entity links on the root process should include both child and grandchild
            List<EntityLink> entityLinks = processEngineRuntimeService.getEntityLinkChildrenForProcessInstance(processInstance.getId());
            assertThat(entityLinks)
                    .filteredOn(el -> ScopeTypes.CMMN.equals(el.getReferenceScopeType())
                            && childCaseInstance.getId().equals(el.getReferenceScopeId()))
                    .hasSize(1);
            assertThat(entityLinks)
                    .filteredOn(el -> ScopeTypes.CMMN.equals(el.getReferenceScopeType())
                            && grandchildCaseInstance.getId().equals(el.getReferenceScopeId()))
                    .hasSize(1);
            assertThat(entityLinks)
                    .filteredOn(el -> ScopeTypes.TASK.equals(el.getReferenceScopeType())
                            && grandchildTask.getId().equals(el.getReferenceScopeId()))
                    .hasSize(1);

            // Historic entity links on root process should NOT exist (root process has history NONE)
            List<HistoricEntityLink> historicEntityLinks = processEngineHistoryService
                    .getHistoricEntityLinkChildrenForProcessInstance(processInstance.getId());
            assertThat(historicEntityLinks)
                    .filteredOn(el -> ScopeTypes.CMMN.equals(el.getReferenceScopeType()))
                    .isEmpty();
            assertThat(historicEntityLinks)
                    .filteredOn(el -> ScopeTypes.TASK.equals(el.getReferenceScopeType()))
                    .isEmpty();

            // Historic entity links on child case should exist (child case -> grandchild case, both have history enabled)
            List<HistoricEntityLink> childHistoricEntityLinks = cmmnHistoryService
                    .getHistoricEntityLinkChildrenForCaseInstance(childCaseInstance.getId());
            assertThat(childHistoricEntityLinks)
                    .filteredOn(el -> ScopeTypes.CMMN.equals(el.getReferenceScopeType())
                            && grandchildCaseInstance.getId().equals(el.getReferenceScopeId()))
                    .hasSize(1);
            assertThat(childHistoricEntityLinks)
                    .filteredOn(el -> ScopeTypes.TASK.equals(el.getReferenceScopeType())
                            && grandchildTask.getId().equals(el.getReferenceScopeId()))
                    .hasSize(1);

        } finally {
            processEngineRepositoryService.deleteDeployment(bpmnDeployment.getId(), true);
            cmmnRepositoryService.deleteDeployment(childDeployment.getId(), true);
            cmmnRepositoryService.deleteDeployment(grandchildDeployment.getId(), true);
        }
    }

    @Test
    public void testCmmnParentWithCallActivityGrandchildHistoryNone() {
        // Chain: Case -> Process (call activity) -> Grandchild Process (NONE)
        Deployment grandchildDeployment = processEngineRepositoryService.createDeployment()
                .addClasspathResource("org/flowable/cmmn/test/EntityLinkHistoryLevelTest.oneTaskProcessHistoryNone.bpmn20.xml")
                .deploy();
        Deployment childDeployment = processEngineRepositoryService.createDeployment()
                .addClasspathResource("org/flowable/cmmn/test/EntityLinkHistoryLevelTest.callActivityProcessHistoryNoneChild.bpmn20.xml")
                .deploy();
        CmmnDeployment cmmnDeployment = cmmnRepositoryService.createDeployment()
                .addClasspathResource("org/flowable/cmmn/test/EntityLinkHistoryLevelTest.processTaskCaseWithCallActivityHistoryNoneGrandchild.cmmn")
                .deploy();

        try {
            CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                    .caseDefinitionKey("processTaskCaseWithCallActivityHistoryNoneGrandchild")
                    .start();

            ProcessInstance childProcessInstance = processEngineRuntimeService.createProcessInstanceQuery()
                    .processDefinitionKey("callActivityHistoryNoneChild")
                    .singleResult();
            assertThat(childProcessInstance).isNotNull();

            ProcessInstance grandchildProcessInstance = processEngineRuntimeService.createProcessInstanceQuery()
                    .superProcessInstanceId(childProcessInstance.getId())
                    .singleResult();
            assertThat(grandchildProcessInstance).isNotNull();

            Task grandchildTask = processEngineTaskService.createTaskQuery()
                    .processInstanceId(grandchildProcessInstance.getId())
                    .singleResult();
            assertThat(grandchildTask).isNotNull();

            // Runtime entity links on the root case should include both child and grandchild
            List<EntityLink> entityLinks = cmmnRuntimeService.getEntityLinkChildrenForCaseInstance(caseInstance.getId());
            assertThat(entityLinks)
                    .filteredOn(el -> ScopeTypes.BPMN.equals(el.getReferenceScopeType())
                            && childProcessInstance.getId().equals(el.getReferenceScopeId()))
                    .hasSize(1);
            assertThat(entityLinks)
                    .filteredOn(el -> ScopeTypes.BPMN.equals(el.getReferenceScopeType())
                            && grandchildProcessInstance.getId().equals(el.getReferenceScopeId()))
                    .hasSize(1);
            assertThat(entityLinks)
                    .filteredOn(el -> ScopeTypes.TASK.equals(el.getReferenceScopeType())
                            && grandchildTask.getId().equals(el.getReferenceScopeId()))
                    .hasSize(1);

            // Historic entity link to child process should exist (root case and child process both have history enabled)
            List<HistoricEntityLink> historicEntityLinks = cmmnHistoryService
                    .getHistoricEntityLinkChildrenForCaseInstance(caseInstance.getId());
            assertThat(historicEntityLinks)
                    .filteredOn(el -> ScopeTypes.BPMN.equals(el.getReferenceScopeType())
                            && childProcessInstance.getId().equals(el.getReferenceScopeId()))
                    .hasSize(1);

            // Historic entity link to grandchild process should not exist (grandchild has history NONE)
            assertThat(historicEntityLinks)
                    .filteredOn(el -> ScopeTypes.BPMN.equals(el.getReferenceScopeType())
                            && grandchildProcessInstance.getId().equals(el.getReferenceScopeId()))
                    .isEmpty();

            // Historic entity link to grandchild task should NOT exist (grandchild has history NONE, task links are also suppressed)
            assertThat(historicEntityLinks)
                    .filteredOn(el -> ScopeTypes.TASK.equals(el.getReferenceScopeType())
                            && grandchildTask.getId().equals(el.getReferenceScopeId()))
                    .isEmpty();

            // Historic entity links on child process should be empty for grandchild instance (grandchild has history NONE)
            List<HistoricEntityLink> childHistoricEntityLinks = processEngineHistoryService
                    .getHistoricEntityLinkChildrenForProcessInstance(childProcessInstance.getId());
            assertThat(childHistoricEntityLinks)
                    .filteredOn(el -> ScopeTypes.BPMN.equals(el.getReferenceScopeType())
                            && grandchildProcessInstance.getId().equals(el.getReferenceScopeId()))
                    .isEmpty();
            // Task entity link on child should NOT exist (grandchild has history NONE)
            assertThat(childHistoricEntityLinks)
                    .filteredOn(el -> ScopeTypes.TASK.equals(el.getReferenceScopeType())
                            && grandchildTask.getId().equals(el.getReferenceScopeId()))
                    .isEmpty();

        } finally {
            cmmnRepositoryService.deleteDeployment(cmmnDeployment.getId(), true);
            processEngineRepositoryService.deleteDeployment(childDeployment.getId(), true);
            processEngineRepositoryService.deleteDeployment(grandchildDeployment.getId(), true);
        }
    }

    @Test
    public void testBpmnParentWithCaseTaskGrandchildHistoryNone() {
        // Chain: Process -> Case (case task) -> Grandchild Case (NONE)
        CmmnDeployment grandchildDeployment = cmmnRepositoryService.createDeployment()
                .addClasspathResource("org/flowable/cmmn/test/EntityLinkHistoryLevelTest.oneHumanTaskCaseHistoryNone.cmmn")
                .deploy();
        CmmnDeployment childDeployment = cmmnRepositoryService.createDeployment()
                .addClasspathResource("org/flowable/cmmn/test/EntityLinkHistoryLevelTest.caseTaskCaseHistoryNoneChild.cmmn")
                .deploy();
        Deployment bpmnDeployment = processEngineRepositoryService.createDeployment()
                .addClasspathResource("org/flowable/cmmn/test/EntityLinkHistoryLevelTest.caseTaskProcessWithNestedCaseHistoryNoneGrandchild.bpmn20.xml")
                .deploy();

        try {
            ProcessInstance processInstance = processEngineRuntimeService.startProcessInstanceByKey("caseTaskProcessWithNestedCaseHistoryNoneGrandchild");

            CaseInstance childCaseInstance = cmmnRuntimeService.createCaseInstanceQuery()
                    .caseDefinitionKey("caseTaskCaseHistoryNoneChild")
                    .singleResult();
            assertThat(childCaseInstance).isNotNull();

            CaseInstance grandchildCaseInstance = cmmnRuntimeService.createCaseInstanceQuery()
                    .caseInstanceParentId(childCaseInstance.getId())
                    .singleResult();
            assertThat(grandchildCaseInstance).isNotNull();

            Task grandchildTask = cmmnTaskService.createTaskQuery()
                    .caseInstanceId(grandchildCaseInstance.getId())
                    .singleResult();
            assertThat(grandchildTask).isNotNull();

            // Runtime entity links on the root process should include both child and grandchild
            List<EntityLink> entityLinks = processEngineRuntimeService.getEntityLinkChildrenForProcessInstance(processInstance.getId());
            assertThat(entityLinks)
                    .filteredOn(el -> ScopeTypes.CMMN.equals(el.getReferenceScopeType())
                            && childCaseInstance.getId().equals(el.getReferenceScopeId()))
                    .hasSize(1);
            assertThat(entityLinks)
                    .filteredOn(el -> ScopeTypes.CMMN.equals(el.getReferenceScopeType())
                            && grandchildCaseInstance.getId().equals(el.getReferenceScopeId()))
                    .hasSize(1);
            assertThat(entityLinks)
                    .filteredOn(el -> ScopeTypes.TASK.equals(el.getReferenceScopeType())
                            && grandchildTask.getId().equals(el.getReferenceScopeId()))
                    .hasSize(1);

            // Historic entity link to child case SHOULD exist (root process and child case both have history enabled)
            List<HistoricEntityLink> historicEntityLinks = processEngineHistoryService
                    .getHistoricEntityLinkChildrenForProcessInstance(processInstance.getId());
            assertThat(historicEntityLinks)
                    .filteredOn(el -> ScopeTypes.CMMN.equals(el.getReferenceScopeType())
                            && childCaseInstance.getId().equals(el.getReferenceScopeId()))
                    .hasSize(1);

            // Historic entity link to grandchild case should NOT exist (grandchild has history NONE)
            assertThat(historicEntityLinks)
                    .filteredOn(el -> ScopeTypes.CMMN.equals(el.getReferenceScopeType())
                            && grandchildCaseInstance.getId().equals(el.getReferenceScopeId()))
                    .isEmpty();

            // Historic entity link to grandchild task should NOT exist (grandchild has history NONE, task links are also suppressed)
            assertThat(historicEntityLinks)
                    .filteredOn(el -> ScopeTypes.TASK.equals(el.getReferenceScopeType())
                            && grandchildTask.getId().equals(el.getReferenceScopeId()))
                    .isEmpty();

            // Historic entity links on child case should be empty for grandchild instance (grandchild has history NONE)
            List<HistoricEntityLink> childHistoricEntityLinks = cmmnHistoryService
                    .getHistoricEntityLinkChildrenForCaseInstance(childCaseInstance.getId());
            assertThat(childHistoricEntityLinks)
                    .filteredOn(el -> ScopeTypes.CMMN.equals(el.getReferenceScopeType())
                            && grandchildCaseInstance.getId().equals(el.getReferenceScopeId()))
                    .isEmpty();
            // Task entity link on child should NOT exist (grandchild has history NONE)
            assertThat(childHistoricEntityLinks)
                    .filteredOn(el -> ScopeTypes.TASK.equals(el.getReferenceScopeType())
                            && grandchildTask.getId().equals(el.getReferenceScopeId()))
                    .isEmpty();

        } finally {
            processEngineRepositoryService.deleteDeployment(bpmnDeployment.getId(), true);
            cmmnRepositoryService.deleteDeployment(childDeployment.getId(), true);
            cmmnRepositoryService.deleteDeployment(grandchildDeployment.getId(), true);
        }
    }
}
