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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.assertj.core.groups.Tuple;
import org.flowable.cmmn.api.CallbackTypes;
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.constant.ReferenceTypes;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.impl.ExecutionQueryImpl;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.persistence.entity.ExecutionEntityManager;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.entitylink.api.EntityLink;
import org.flowable.entitylink.api.EntityLinkType;
import org.flowable.entitylink.api.HierarchyType;
import org.flowable.entitylink.api.history.HistoricEntityLink;
import org.flowable.task.api.Task;
import org.flowable.variable.api.history.HistoricVariableInstance;
import org.junit.Test;

/**
 * @author Tijs Rademakers
 * @author Joram Barrez
 * @author Valentin Zickner
 * @author Filip Hrisafov
 */
public class CaseTaskTest extends AbstractProcessEngineIntegrationTest {

    @Test
    @CmmnDeployment
    public void testCaseTask() {
        Deployment deployment = processEngineRepositoryService.createDeployment()
                .addClasspathResource("org/flowable/cmmn/test/caseTaskProcess.bpmn20.xml")
                .deploy();

        try {
            ProcessInstance processInstance = processEngineRuntimeService.startProcessInstanceByKey("caseTask");
            List<Task> processTasks = processEngineTaskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
            assertThat(processTasks).hasSize(1);

            processEngineTaskService.complete(processTasks.get(0).getId());

            Execution execution = processEngineRuntimeService.createExecutionQuery().onlyChildExecutions()
                    .processInstanceId(processInstance.getId())
                    .singleResult();

            CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceQuery().caseInstanceCallbackId(execution.getId())
                    .caseInstanceCallbackType(CallbackTypes.EXECUTION_CHILD_CASE)
                    .singleResult();

            assertThat(caseInstance).isNotNull();

            Execution caseTaskExecution = processEngineRuntimeService.createExecutionQuery()
                    .executionReferenceId(caseInstance.getId())
                    .executionReferenceType(ReferenceTypes.EXECUTION_CHILD_CASE)
                    .singleResult();
            assertThat(caseTaskExecution).isNotNull();

            List<Task> caseTasks = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).list();
            assertThat(caseTasks).hasSize(1);

            cmmnTaskService.complete(caseTasks.get(0).getId());

            CaseInstance dbCaseInstance = cmmnRuntimeService.createCaseInstanceQuery().caseInstanceId(caseInstance.getId()).singleResult();
            assertThat(dbCaseInstance).isNull();

            processTasks = processEngineTaskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
            assertThat(processTasks).hasSize(1);

            processEngine.getTaskService().complete(processTasks.get(0).getId());
            assertThat(processEngineRuntimeService.createProcessInstanceQuery().count()).isZero();

        } finally {
            processEngineRepositoryService.deleteDeployment(deployment.getId(), true);
        }
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/oneHumanTaskCase.cmmn")
    public void testDeeplyNestedCaseTask() {
        Deployment deployment = processEngineRepositoryService.createDeployment()
                .addClasspathResource("org/flowable/cmmn/test/nestedCallActivityProcess.bpmn20.xml")
                .addClasspathResource("org/flowable/cmmn/test/oneCallActivityHumanTaskCaseProcess.bpmn20.xml")
                .deploy();

        // nestedCallActivityProcess
        //   - oneCallActivity (CallActivity)
        //    - oneHumanTaskCase (CaseTask)
        //      - humanTask

        try {
            // starting one extra process instance so that there are multiple entity links
            processEngineRuntimeService.startProcessInstanceByKey("nestedCallActivity");

            ProcessInstance processInstance = processEngineRuntimeService.startProcessInstanceByKey("nestedCallActivity");
            String processInstanceId = processInstance.getId();
            Task caseTask = processEngineTaskService.createTaskQuery().processInstanceIdWithChildren(processInstance.getId()).singleResult();

            ProcessInstance subProcessInstance = processEngineRuntimeService.createProcessInstanceQuery()
                    .superProcessInstanceId(processInstanceId)
                    .singleResult();
            String subProcessInstanceId = subProcessInstance.getId();

            CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceQuery().caseInstanceId(caseTask.getScopeId()).singleResult();
            String caseInstanceId = caseInstance.getId();

            List<EntityLink> entityLinks = processEngineRuntimeService.getEntityLinkChildrenForProcessInstance(processInstanceId);

            assertThat(entityLinks)
                    .extracting(EntityLink::getScopeId, EntityLink::getScopeType, EntityLink::getHierarchyType, EntityLink::getReferenceScopeId,
                            EntityLink::getReferenceScopeType, EntityLink::getLinkType)
                    .as("scopeId, scopeType, hierarchyType, referenceScopeId, referenceScopeType, linkType")
                    .containsExactlyInAnyOrder(
                            tuple(processInstanceId, ScopeTypes.BPMN, HierarchyType.ROOT, subProcessInstanceId, ScopeTypes.BPMN, EntityLinkType.CHILD),
                            tuple(processInstanceId, ScopeTypes.BPMN, HierarchyType.ROOT, caseInstanceId, ScopeTypes.CMMN, EntityLinkType.CHILD),
                            tuple(processInstanceId, ScopeTypes.BPMN, HierarchyType.ROOT, caseTask.getId(), ScopeTypes.TASK, EntityLinkType.CHILD)
                    );

            assertThat(entityLinks)
                    .extracting(EntityLink::getRootScopeId, EntityLink::getRootScopeType)
                    .containsOnly(
                            tuple(processInstanceId, ScopeTypes.BPMN)
                    );
            
            entityLinks = processEngineRuntimeService.getEntityLinkChildrenForProcessInstance(subProcessInstanceId);

            assertThat(entityLinks)
                    .extracting(EntityLink::getScopeId, EntityLink::getScopeType, EntityLink::getHierarchyType, EntityLink::getReferenceScopeId,
                            EntityLink::getReferenceScopeType, EntityLink::getLinkType)
                    .as("scopeId, scopeType, hierarchyType, referenceScopeId, referenceScopeType, linkType")
                    .containsExactlyInAnyOrder(
                            tuple(subProcessInstanceId, ScopeTypes.BPMN, HierarchyType.PARENT, caseInstanceId, ScopeTypes.CMMN, EntityLinkType.CHILD),
                            tuple(subProcessInstanceId, ScopeTypes.BPMN, HierarchyType.GRAND_PARENT, caseTask.getId(), ScopeTypes.TASK, EntityLinkType.CHILD)
                    );

            assertThat(entityLinks)
                    .extracting(EntityLink::getRootScopeId, EntityLink::getRootScopeType)
                    .containsOnly(
                            tuple(processInstanceId, ScopeTypes.BPMN)
                    );
            
            entityLinks = cmmnRuntimeService.getEntityLinkChildrenForCaseInstance(caseInstanceId);

            assertThat(entityLinks)
                    .extracting(EntityLink::getScopeId, EntityLink::getScopeType, EntityLink::getHierarchyType, EntityLink::getReferenceScopeId,
                            EntityLink::getReferenceScopeType, EntityLink::getLinkType)
                    .as("scopeId, scopeType, hierarchyType, referenceScopeId, referenceScopeType, linkType")
                    .containsExactlyInAnyOrder(
                            tuple(caseInstanceId, ScopeTypes.CMMN, HierarchyType.PARENT, caseTask.getId(), ScopeTypes.TASK, EntityLinkType.CHILD)
                    );

            assertThat(entityLinks)
                    .extracting(EntityLink::getRootScopeId, EntityLink::getRootScopeType)
                    .containsOnly(
                            tuple(processInstanceId, ScopeTypes.BPMN)
                    );

            entityLinks = processEngineRuntimeService.getEntityLinkParentsForTask(caseTask.getId());

            assertThat(entityLinks)
                    .extracting(EntityLink::getScopeId, EntityLink::getScopeType, EntityLink::getHierarchyType, EntityLink::getReferenceScopeId,
                            EntityLink::getReferenceScopeType, EntityLink::getLinkType)
                    .as("scopeId, scopeType, hierarchyType, referenceScopeId, referenceScopeType, linkType")
                    .containsExactlyInAnyOrder(
                            tuple(processInstanceId, ScopeTypes.BPMN, HierarchyType.ROOT, caseTask.getId(), ScopeTypes.TASK, EntityLinkType.CHILD),
                            tuple(subProcessInstanceId, ScopeTypes.BPMN, HierarchyType.GRAND_PARENT, caseTask.getId(), ScopeTypes.TASK, EntityLinkType.CHILD),
                            tuple(caseInstanceId, ScopeTypes.CMMN, HierarchyType.PARENT, caseTask.getId(), ScopeTypes.TASK, EntityLinkType.CHILD)
                    );

            assertThat(entityLinks)
                    .extracting(EntityLink::getRootScopeId, EntityLink::getRootScopeType)
                    .containsOnly(
                            tuple(processInstanceId, ScopeTypes.BPMN)
                    );

            List<Tuple> sameRootEntityLinks = Arrays.asList(
                    tuple(processInstanceId, ScopeTypes.BPMN, HierarchyType.ROOT, subProcessInstanceId, ScopeTypes.BPMN, EntityLinkType.CHILD),
                    tuple(processInstanceId, ScopeTypes.BPMN, HierarchyType.ROOT, caseInstanceId, ScopeTypes.CMMN, EntityLinkType.CHILD),
                    tuple(processInstanceId, ScopeTypes.BPMN, HierarchyType.ROOT, caseTask.getId(), ScopeTypes.TASK, EntityLinkType.CHILD),

                    tuple(subProcessInstanceId, ScopeTypes.BPMN, HierarchyType.PARENT, caseInstanceId, ScopeTypes.CMMN, EntityLinkType.CHILD),
                    tuple(subProcessInstanceId, ScopeTypes.BPMN, HierarchyType.GRAND_PARENT, caseTask.getId(), ScopeTypes.TASK, EntityLinkType.CHILD),

                    tuple(caseInstanceId, ScopeTypes.CMMN, HierarchyType.PARENT, caseTask.getId(), ScopeTypes.TASK, EntityLinkType.CHILD)
            );

            assertThat(processEngineRuntimeService.getEntityLinkChildrenWithSameRootAsProcessInstance(processInstanceId))
                    .extracting(EntityLink::getScopeId, EntityLink::getScopeType, EntityLink::getHierarchyType, EntityLink::getReferenceScopeId,
                            EntityLink::getReferenceScopeType, EntityLink::getLinkType)
                    .as("scopeId, scopeType, hierarchyType, referenceScopeId, referenceScopeType, linkType")
                    .containsExactlyInAnyOrderElementsOf(sameRootEntityLinks);

            assertThat(processEngineRuntimeService.getEntityLinkChildrenWithSameRootAsProcessInstance(subProcessInstanceId))
                    .extracting(EntityLink::getScopeId, EntityLink::getScopeType, EntityLink::getHierarchyType, EntityLink::getReferenceScopeId,
                            EntityLink::getReferenceScopeType, EntityLink::getLinkType)
                    .as("scopeId, scopeType, hierarchyType, referenceScopeId, referenceScopeType, linkType")
                    .containsExactlyInAnyOrderElementsOf(sameRootEntityLinks);

            assertThat(cmmnRuntimeService.getEntityLinkChildrenWithSameRootAsCaseInstance(caseInstanceId))
                    .extracting(EntityLink::getScopeId, EntityLink::getScopeType, EntityLink::getHierarchyType, EntityLink::getReferenceScopeId,
                            EntityLink::getReferenceScopeType, EntityLink::getLinkType)
                    .as("scopeId, scopeType, hierarchyType, referenceScopeId, referenceScopeType, linkType")
                    .containsExactlyInAnyOrderElementsOf(sameRootEntityLinks);

            cmmnTaskService.complete(caseTask.getId());

            assertCaseInstanceEnded(caseInstance);

            List<HistoricEntityLink> historicEntityLinks = processEngineHistoryService.getHistoricEntityLinkChildrenForProcessInstance(processInstanceId);

            assertThat(historicEntityLinks)
                    .extracting(HistoricEntityLink::getScopeId, HistoricEntityLink::getScopeType, HistoricEntityLink::getHierarchyType, HistoricEntityLink::getReferenceScopeId,
                            HistoricEntityLink::getReferenceScopeType, HistoricEntityLink::getLinkType)
                    .as("scopeId, scopeType, hierarchyType, referenceScopeId, referenceScopeType, linkType")
                    .containsExactlyInAnyOrder(
                            tuple(processInstanceId, ScopeTypes.BPMN, HierarchyType.ROOT, subProcessInstanceId, ScopeTypes.BPMN, EntityLinkType.CHILD),
                            tuple(processInstanceId, ScopeTypes.BPMN, HierarchyType.ROOT, caseInstanceId, ScopeTypes.CMMN, EntityLinkType.CHILD),
                            tuple(processInstanceId, ScopeTypes.BPMN, HierarchyType.ROOT, caseTask.getId(), ScopeTypes.TASK, EntityLinkType.CHILD)
                    );

            assertThat(historicEntityLinks)
                    .extracting(HistoricEntityLink::getRootScopeId, HistoricEntityLink::getRootScopeType)
                    .containsOnly(
                            tuple(processInstanceId, ScopeTypes.BPMN)
                    );


            historicEntityLinks = processEngineHistoryService.getHistoricEntityLinkChildrenForProcessInstance(subProcessInstanceId);

            assertThat(historicEntityLinks)
                    .extracting(HistoricEntityLink::getScopeId, HistoricEntityLink::getScopeType, HistoricEntityLink::getHierarchyType, HistoricEntityLink::getReferenceScopeId,
                            HistoricEntityLink::getReferenceScopeType, HistoricEntityLink::getLinkType)
                    .as("scopeId, scopeType, hierarchyType, referenceScopeId, referenceScopeType, linkType")
                    .containsExactlyInAnyOrder(
                            tuple(subProcessInstanceId, ScopeTypes.BPMN, HierarchyType.PARENT, caseInstanceId, ScopeTypes.CMMN, EntityLinkType.CHILD),
                            tuple(subProcessInstanceId, ScopeTypes.BPMN, HierarchyType.GRAND_PARENT, caseTask.getId(), ScopeTypes.TASK, EntityLinkType.CHILD)
                    );

            assertThat(historicEntityLinks)
                    .extracting(HistoricEntityLink::getRootScopeId, HistoricEntityLink::getRootScopeType)
                    .containsOnly(
                            tuple(processInstanceId, ScopeTypes.BPMN)
                    );

            historicEntityLinks = cmmnHistoryService.getHistoricEntityLinkChildrenForCaseInstance(caseInstanceId);

            assertThat(historicEntityLinks)
                    .extracting(HistoricEntityLink::getScopeId, HistoricEntityLink::getScopeType, HistoricEntityLink::getHierarchyType, HistoricEntityLink::getReferenceScopeId,
                            HistoricEntityLink::getReferenceScopeType, HistoricEntityLink::getLinkType)
                    .as("scopeId, scopeType, hierarchyType, referenceScopeId, referenceScopeType, linkType")
                    .containsExactlyInAnyOrder(
                            tuple(caseInstanceId, ScopeTypes.CMMN, HierarchyType.PARENT, caseTask.getId(), ScopeTypes.TASK, EntityLinkType.CHILD)
                    );

            assertThat(historicEntityLinks)
                    .extracting(HistoricEntityLink::getRootScopeId, HistoricEntityLink::getRootScopeType)
                    .containsOnly(
                            tuple(processInstanceId, ScopeTypes.BPMN)
                    );

            historicEntityLinks = processEngineHistoryService.getHistoricEntityLinkParentsForTask(caseTask.getId());

            assertThat(historicEntityLinks)
                    .extracting(HistoricEntityLink::getScopeId, HistoricEntityLink::getScopeType, HistoricEntityLink::getHierarchyType, HistoricEntityLink::getReferenceScopeId,
                            HistoricEntityLink::getReferenceScopeType, HistoricEntityLink::getLinkType)
                    .as("scopeId, scopeType, hierarchyType, referenceScopeId, referenceScopeType, linkType")
                    .containsExactlyInAnyOrder(
                            tuple(processInstanceId, ScopeTypes.BPMN, HierarchyType.ROOT, caseTask.getId(), ScopeTypes.TASK, EntityLinkType.CHILD),
                            tuple(subProcessInstanceId, ScopeTypes.BPMN, HierarchyType.GRAND_PARENT, caseTask.getId(), ScopeTypes.TASK, EntityLinkType.CHILD),
                            tuple(caseInstanceId, ScopeTypes.CMMN, HierarchyType.PARENT, caseTask.getId(), ScopeTypes.TASK, EntityLinkType.CHILD)
                    );

            assertThat(historicEntityLinks)
                    .extracting(HistoricEntityLink::getRootScopeId, HistoricEntityLink::getRootScopeType)
                    .containsOnly(
                            tuple(processInstanceId, ScopeTypes.BPMN)
                    );

            assertThat(processEngineHistoryService.getHistoricEntityLinkChildrenWithSameRootAsProcessInstance(processInstanceId))
                    .extracting(HistoricEntityLink::getScopeId, HistoricEntityLink::getScopeType, HistoricEntityLink::getHierarchyType, HistoricEntityLink::getReferenceScopeId,
                            HistoricEntityLink::getReferenceScopeType, HistoricEntityLink::getLinkType)
                    .as("scopeId, scopeType, hierarchyType, referenceScopeId, referenceScopeType, linkType")
                    .containsExactlyInAnyOrderElementsOf(sameRootEntityLinks);

            assertThat(processEngineHistoryService.getHistoricEntityLinkChildrenWithSameRootAsProcessInstance(subProcessInstanceId))
                    .extracting(HistoricEntityLink::getScopeId, HistoricEntityLink::getScopeType, HistoricEntityLink::getHierarchyType, HistoricEntityLink::getReferenceScopeId,
                            HistoricEntityLink::getReferenceScopeType, HistoricEntityLink::getLinkType)
                    .as("scopeId, scopeType, hierarchyType, referenceScopeId, referenceScopeType, linkType")
                    .containsExactlyInAnyOrderElementsOf(sameRootEntityLinks);

            assertThat(cmmnHistoryService.getHistoricEntityLinkChildrenWithSameRootAsCaseInstance(caseInstanceId))
                    .extracting(HistoricEntityLink::getScopeId, HistoricEntityLink::getScopeType, HistoricEntityLink::getHierarchyType, HistoricEntityLink::getReferenceScopeId,
                            HistoricEntityLink::getReferenceScopeType, HistoricEntityLink::getLinkType)
                    .as("scopeId, scopeType, hierarchyType, referenceScopeId, referenceScopeType, linkType")
                    .containsExactlyInAnyOrderElementsOf(sameRootEntityLinks);

        } finally {
            processEngineRepositoryService.deleteDeployment(deployment.getId(), true);
        }
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/oneHumanTaskCase.cmmn")
    public void testCaseTaskWithCaseDefinitionKeyExpression() {
        Deployment deployment = processEngineRepositoryService.createDeployment()
                .addClasspathResource("org/flowable/cmmn/test/caseTaskProcessWithCaseDefinitionKeyExpression.bpmn20.xml")
                .deploy();

        try {
            ProcessInstance processInstance = processEngineRuntimeService.createProcessInstanceBuilder()
                    .processDefinitionKey("caseTask")
                    .variable("caseKeyVar", "oneHumanTaskCase")
                    .start();
            List<Task> processTasks = processEngineTaskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
            assertThat(processTasks).hasSize(1);

            processEngineTaskService.complete(processTasks.get(0).getId());

            String caseInstanceId = processEngineRuntimeService.getVariable(processInstance.getId(), "myCaseInstanceId", String.class);

            CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceQuery()
                    .caseInstanceId(caseInstanceId)
                    .singleResult();

            assertThat(caseInstance).isNotNull();

            List<Task> caseTasks = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).list();
            assertThat(caseTasks).hasSize(1);

            cmmnTaskService.complete(caseTasks.get(0).getId());

            CaseInstance dbCaseInstance = cmmnRuntimeService.createCaseInstanceQuery().caseInstanceId(caseInstance.getId()).singleResult();
            assertThat(dbCaseInstance).isNull();

            processTasks = processEngineTaskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
            assertThat(processTasks).hasSize(1);

            processEngine.getTaskService().complete(processTasks.get(0).getId());
            assertThat(processEngineRuntimeService.createProcessInstanceQuery().count()).isZero();

        } finally {
            processEngineRepositoryService.deleteDeployment(deployment.getId(), true);
        }
    }

    @Test
    public void testCaseTaskWithSameDeployment() {
        Set<String> deploymentsToDelete = new HashSet<>();
        Deployment deploymentV1 = processEngineRepositoryService.createDeployment()
                .addClasspathResource("org/flowable/cmmn/test/caseTaskProcessWithSameDeployment.bpmn20.xml")
                .addClasspathResource("org/flowable/cmmn/test/oneHumanTaskCase.cmmn")
                .deploy();

        deploymentsToDelete.add(deploymentV1.getId());

        try {
            ProcessInstance processInstance = processEngineRuntimeService.createProcessInstanceBuilder()
                    .processDefinitionKey("caseTask")
                    .start();

            String caseInstanceId = processEngineRuntimeService.getVariable(processInstance.getId(), "caseInstanceId", String.class);

            Task task = processEngineTaskService.createTaskQuery()
                    .caseInstanceId(caseInstanceId)
                    .singleResult();
            assertThat(task).isNotNull();
            assertThat(task.getName()).isEqualTo("The Task");

            // Starting after V2 deployment should use the same deployment task
            Deployment deploymentV2 = processEngineRepositoryService.createDeployment()
                    .addClasspathResource("org/flowable/cmmn/test/oneHumanTaskCaseV2.cmmn")
                    .deploy();

            deploymentsToDelete.add(deploymentV2.getId());

            processInstance = processEngineRuntimeService.createProcessInstanceBuilder()
                    .processDefinitionKey("caseTask")
                    .start();

            caseInstanceId = processEngineRuntimeService.getVariable(processInstance.getId(), "caseInstanceId", String.class);

            task = processEngineTaskService.createTaskQuery()
                    .caseInstanceId(caseInstanceId)
                    .singleResult();
            assertThat(task).isNotNull();
            assertThat(task.getName()).isEqualTo("The Task");

        } finally {
            for (String deploymentId : deploymentsToDelete) {
                processEngineRepositoryService.deleteDeployment(deploymentId, true);
            }
        }
    }

    @Test
    public void testCaseTaskWithSameDeploymentInTenant() {
        Set<String> deploymentsToDelete = new HashSet<>();
        Deployment deploymentV1 = processEngineRepositoryService.createDeployment()
                .addClasspathResource("org/flowable/cmmn/test/caseTaskProcessWithSameDeployment.bpmn20.xml")
                .addClasspathResource("org/flowable/cmmn/test/oneHumanTaskCase.cmmn")
                .tenantId("flowable")
                .deploy();

        deploymentsToDelete.add(deploymentV1.getId());

        try {
            ProcessInstance processInstance = processEngineRuntimeService.createProcessInstanceBuilder()
                    .processDefinitionKey("caseTask")
                    .tenantId("flowable")
                    .start();

            String caseInstanceId = processEngineRuntimeService.getVariable(processInstance.getId(), "caseInstanceId", String.class);

            Task task = processEngineTaskService.createTaskQuery()
                    .caseInstanceId(caseInstanceId)
                    .singleResult();
            assertThat(task).isNotNull();
            assertThat(task)
                    .extracting(Task::getName, Task::getTenantId)
                    .containsExactly("The Task", "flowable");

            // Starting after V2 deployment should use the same deployment task
            Deployment deploymentV2 = processEngineRepositoryService.createDeployment()
                    .addClasspathResource("org/flowable/cmmn/test/oneHumanTaskCaseV2.cmmn")
                    .tenantId("flowable")
                    .deploy();

            deploymentsToDelete.add(deploymentV2.getId());

            processInstance = processEngineRuntimeService.createProcessInstanceBuilder()
                    .processDefinitionKey("caseTask")
                    .tenantId("flowable")
                    .start();

            caseInstanceId = processEngineRuntimeService.getVariable(processInstance.getId(), "caseInstanceId", String.class);

            task = processEngineTaskService.createTaskQuery()
                    .caseInstanceId(caseInstanceId)
                    .singleResult();
            assertThat(task).isNotNull();
            assertThat(task)
                    .extracting(Task::getName, Task::getTenantId)
                    .containsExactly("The Task", "flowable");

        } finally {
            for (String deploymentId : deploymentsToDelete) {
                processEngineRepositoryService.deleteDeployment(deploymentId, true);
            }
        }
    }

    @Test
    public void testCaseTaskWithSameDeploymentFalse() {
        Set<String> deploymentsToDelete = new HashSet<>();
        Deployment deploymentV1 = processEngineRepositoryService.createDeployment()
                .addClasspathResource("org/flowable/cmmn/test/caseTaskProcessWithSameDeploymentFalse.bpmn20.xml")
                .addClasspathResource("org/flowable/cmmn/test/oneHumanTaskCase.cmmn")
                .deploy();

        deploymentsToDelete.add(deploymentV1.getId());

        try {
            ProcessInstance processInstance = processEngineRuntimeService.createProcessInstanceBuilder()
                    .processDefinitionKey("caseTask")
                    .start();

            String caseInstanceId = processEngineRuntimeService.getVariable(processInstance.getId(), "caseInstanceId", String.class);

            Task task = processEngineTaskService.createTaskQuery()
                    .caseInstanceId(caseInstanceId)
                    .singleResult();
            assertThat(task).isNotNull();
            assertThat(task.getName()).isEqualTo("The Task");

            // Starting after V2 deployment should use the same deployment task
            Deployment deploymentV2 = processEngineRepositoryService.createDeployment()
                    .addClasspathResource("org/flowable/cmmn/test/oneHumanTaskCaseV2.cmmn")
                    .deploy();

            deploymentsToDelete.add(deploymentV2.getId());

            processInstance = processEngineRuntimeService.createProcessInstanceBuilder()
                    .processDefinitionKey("caseTask")
                    .start();

            caseInstanceId = processEngineRuntimeService.getVariable(processInstance.getId(), "caseInstanceId", String.class);

            task = processEngineTaskService.createTaskQuery()
                    .caseInstanceId(caseInstanceId)
                    .singleResult();
            assertThat(task).isNotNull();
            assertThat(task.getName()).isEqualTo("The Task V2");

        } finally {
            for (String deploymentId : deploymentsToDelete) {
                processEngineRepositoryService.deleteDeployment(deploymentId, true);
            }
        }
    }

    @Test
    @CmmnDeployment
    public void testCaseTaskWithParameters() {
        Deployment deployment = processEngineRepositoryService.createDeployment()
                .addClasspathResource("org/flowable/cmmn/test/caseTaskProcessWithParameters.bpmn20.xml")
                .deploy();

        try {
            Map<String, Object> variables = new HashMap<>();
            variables.put("testVar", "test");
            variables.put("testNumVar", 43);
            ProcessInstance processInstance = processEngineRuntimeService.startProcessInstanceByKey("caseTask", variables);
            List<Task> processTasks = processEngineTaskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
            assertThat(processTasks).hasSize(1);

            processEngineTaskService.complete(processTasks.get(0).getId());

            Execution execution = processEngineRuntimeService.createExecutionQuery().onlyChildExecutions()
                    .processInstanceId(processInstance.getId())
                    .singleResult();

            CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceQuery().caseInstanceCallbackId(execution.getId())
                    .caseInstanceCallbackType(CallbackTypes.EXECUTION_CHILD_CASE)
                    .singleResult();

            assertThat(caseInstance).isNotNull();

            assertThat(cmmnRuntimeService.getVariable(caseInstance.getId(), "caseTestVar")).isEqualTo("test");
            assertThat(cmmnRuntimeService.getVariable(caseInstance.getId(), "caseTestNumVar")).isEqualTo(43);

            List<Task> caseTasks = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).list();
            assertThat(caseTasks).hasSize(1);

            cmmnRuntimeService.setVariable(caseInstance.getId(), "caseResult", "someResult");

            cmmnTaskService.complete(caseTasks.get(0).getId());

            CaseInstance dbCaseInstance = cmmnRuntimeService.createCaseInstanceQuery().caseInstanceId(caseInstance.getId()).singleResult();
            assertThat(dbCaseInstance).isNull();

            processTasks = processEngineTaskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
            assertThat(processTasks).hasSize(1);

            assertThat(processEngineRuntimeService.getVariable(processInstance.getId(), "processResult")).isEqualTo("someResult");

            processEngine.getTaskService().complete(processTasks.get(0).getId());
            assertThat(processEngineRuntimeService.createProcessInstanceQuery().count()).isZero();

        } finally {
            processEngineRepositoryService.deleteDeployment(deployment.getId(), true);
        }
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/CaseTaskTest.testCaseTaskWithParameters.cmmn")
    public void testCaseTaskWithCaseNameAndBusinessKey() {
        Deployment deployment = processEngineRepositoryService.createDeployment()
                .addClasspathResource("org/flowable/cmmn/test/caseTaskProcessWithCaseNameAndBusinessKey.bpmn20.xml")
                .deploy();

        try {
            ProcessInstance processInstance = processEngineRuntimeService.createProcessInstanceBuilder()
                    .processDefinitionKey("caseTask")
                    .variable("caseName", "Test")
                    .variable("caseBusinessKey", "case-test-business")
                    .start();
            List<Task> processTasks = processEngineTaskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
            assertThat(processTasks).hasSize(1);

            processEngineTaskService.complete(processTasks.get(0).getId());

            Execution execution = processEngineRuntimeService.createExecutionQuery().onlyChildExecutions()
                    .processInstanceId(processInstance.getId())
                    .singleResult();

            CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceQuery().caseInstanceCallbackId(execution.getId())
                    .caseInstanceCallbackType(CallbackTypes.EXECUTION_CHILD_CASE)
                    .singleResult();

            assertThat(caseInstance).isNotNull();
            assertThat(caseInstance)
                    .extracting(CaseInstance::getName, CaseInstance::getBusinessKey)
                    .containsExactly("Custom Test Name", "case-test-business");
        } finally {
            processEngineRepositoryService.deleteDeployment(deployment.getId(), true);
        }
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/CaseTaskTest.testCaseTask.cmmn")
    public void testDeleteCaseTaskShouldNotBePossible() {
        Deployment deployment = processEngineRepositoryService.createDeployment()
                .addClasspathResource("org/flowable/cmmn/test/caseTaskProcess.bpmn20.xml")
                .deploy();

        try {
            ProcessInstance processInstance = processEngineRuntimeService.startProcessInstanceByKey("caseTask");
            List<Task> processTasks = processEngineTaskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
            assertThat(processTasks).hasSize(1);

            processEngineTaskService.complete(processTasks.get(0).getId());

            Execution execution = processEngineRuntimeService.createExecutionQuery().onlyChildExecutions()
                    .processInstanceId(processInstance.getId())
                    .singleResult();

            CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceQuery().caseInstanceCallbackId(execution.getId())
                    .caseInstanceCallbackType(CallbackTypes.EXECUTION_CHILD_CASE)
                    .singleResult();

            assertThat(caseInstance).isNotNull();

            List<Task> caseTasks = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).list();
            assertThat(caseTasks).hasSize(1);

            assertThatThrownBy(() -> processEngineTaskService.deleteTask(caseTasks.get(0).getId()))
                    .isExactlyInstanceOf(FlowableException.class)
                    .hasMessageContaining("The task cannot be deleted")
                    .hasMessageContaining("running case");

            cmmnTaskService.complete(caseTasks.get(0).getId());

            CaseInstance dbCaseInstance = cmmnRuntimeService.createCaseInstanceQuery().caseInstanceId(caseInstance.getId()).singleResult();
            assertThat(dbCaseInstance).isNull();

            processTasks = processEngineTaskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
            assertThat(processTasks).hasSize(1);

            processEngine.getTaskService().complete(processTasks.get(0).getId());
            assertThat(processEngineRuntimeService.createProcessInstanceQuery().count()).isZero();

        } finally {
            processEngineRepositoryService.deleteDeployment(deployment.getId(), true);
        }
    }

    @Test
    @CmmnDeployment(resources = { "org/flowable/cmmn/test/oneProcessTask.cmmn", "org/flowable/cmmn/test/oneServiceTask.cmmn" })
    public void testCaseHierarchyResolvement() {
        Deployment deployment = processEngineRepositoryService.createDeployment()
                .addClasspathResource("org/flowable/cmmn/test/oneCaseTaskProcess.bpmn20.xml")
                .deploy();

        try {
            CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("oneProcessTaskCase").start();
            assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceId(caseInstance.getId()).count()).isZero();
            HistoricVariableInstance variableInstance = cmmnHistoryService.createHistoricVariableInstanceQuery()
                    .variableName("linkCount")
                    .singleResult();
            assertThat(variableInstance.getValue()).isEqualTo(2);

        } finally {
            processEngineRepositoryService.deleteDeployment(deployment.getId(), true);
        }
    }

    @Test
    @CmmnDeployment(resources = { "org/flowable/cmmn/test/oneProcessTask.cmmn", "org/flowable/cmmn/test/oneServiceTask.cmmn" })
    public void testCaseHierarchyResolvementWithUserTask() {
        Deployment deployment = processEngineRepositoryService.createDeployment()
                .addClasspathResource("org/flowable/cmmn/test/userTaskAndCaseTaskProcess.bpmn20.xml")
                .deploy();

        try {
            CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("oneProcessTaskCase").start();
            Task task = processEngineTaskService.createTaskQuery().taskDefinitionKey("userTask").singleResult();
            processEngineTaskService.complete(task.getId());

            assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceId(caseInstance.getId()).count()).isZero();
            HistoricVariableInstance variableInstance = cmmnHistoryService.createHistoricVariableInstanceQuery()
                    .variableName("linkCount")
                    .singleResult();
            assertThat(variableInstance.getValue()).isEqualTo(2);

        } finally {
            processEngineRepositoryService.deleteDeployment(deployment.getId(), true);
        }
    }

    @Test
    @CmmnDeployment(resources = { "org/flowable/cmmn/test/CaseTaskTest.testCaseTask.cmmn" })
    public void testCaseTaskIdVariableName() {
        Deployment deployment = processEngineRepositoryService.createDeployment()
                .addClasspathResource("org/flowable/cmmn/test/caseTaskProcess.bpmn20.xml")
                .deploy();

        try {
            ProcessInstance processInstance = processEngineRuntimeService.startProcessInstanceByKey("caseTask");
            processEngineTaskService.complete(processEngineTaskService.createTaskQuery().singleResult().getId());
            CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceQuery().singleResult();
            assertThat(processEngineRuntimeService.getVariable(processInstance.getId(), "myCaseInstanceId")).isEqualTo(caseInstance.getId());

        } finally {
            processEngineRepositoryService.deleteDeployment(deployment.getId(), true);
        }
    }

    @Test
    @CmmnDeployment(resources = { "org/flowable/cmmn/test/CaseTaskTest.testCaseTask.cmmn" })
    public void testCaseTaskIdVariableNameExpression() {
        Deployment deployment = processEngineRepositoryService.createDeployment()
                .addClasspathResource("org/flowable/cmmn/test/caseTaskProcessWithIdNameExpressions.bpmn20.xml")
                .deploy();

        try {
            ProcessInstance processInstance = processEngineRuntimeService.createProcessInstanceBuilder()
                    .processDefinitionKey("caseTask")
                    .variable("counter", 1)
                    .start();
            processEngineTaskService.complete(processEngineTaskService.createTaskQuery().singleResult().getId());
            CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceQuery().singleResult();
            assertThat(processEngineRuntimeService.getVariable(processInstance.getId(), "myVariable-1")).isEqualTo(caseInstance.getId());

        } finally {
            processEngineRepositoryService.deleteDeployment(deployment.getId(), true);
        }
    }

    @Test
    @CmmnDeployment(resources = { "org/flowable/cmmn/test/CaseTaskTest.testCaseTask.cmmn" })
    public void testCaseTaskWithTerminateEndEvent() {
        Deployment deployment = processEngineRepositoryService.createDeployment()
                .addClasspathResource("org/flowable/cmmn/test/caseTaskProcessWithTerminateEndEventOnDifferentExecution.bpmn20.xml")
                .deploy();

        try {
            // Arrange
            ProcessInstance processInstance = processEngineRuntimeService.startProcessInstanceByKey("terminateByEndEvent");
            Task task = processEngineTaskService.createTaskQuery()
                    .processInstanceId(processInstance.getProcessInstanceId())
                    .singleResult();
            assertThat(task).isNotNull();
            assertThat(task.getTaskDefinitionKey()).isEqualTo("formTask1");

            long numberOfActiveCaseInstances = cmmnRuntimeService.createCaseInstanceQuery().count();
            assertThat(numberOfActiveCaseInstances).isEqualTo(1);

            Execution caseTask1Execution = processEngineRuntimeService.createExecutionQuery().activityId("caseTask1").singleResult();
            assertThat(caseTask1Execution.getReferenceId()).isNotNull();
            assertThat(caseTask1Execution.getReferenceType()).isEqualTo(ReferenceTypes.EXECUTION_CHILD_CASE);

            // Act
            processEngineTaskService.complete(task.getId());

            // Assert
            long numberOfActiveCaseInstancesAfterCompletion = cmmnRuntimeService.createCaseInstanceQuery()
                    .count();
            assertThat(numberOfActiveCaseInstancesAfterCompletion).isZero();
        } finally {
            processEngineRepositoryService.deleteDeployment(deployment.getId(), true);
        }
    }

    @Test
    @CmmnDeployment(resources = { "org/flowable/cmmn/test/CaseTaskTest.testCaseTask.cmmn" })
    public void testCaseTaskWithTwoCaseTasksWithTerminateEndEvent() {
        Deployment deployment = processEngineRepositoryService.createDeployment()
                .addClasspathResource("org/flowable/cmmn/test/caseTaskProcessWithTwoParallelCasesAndWithTerminateEndEventOnDifferentExecution.bpmn20.xml")
                .deploy();

        try {
            // Arrange
            ProcessInstance processInstance = processEngineRuntimeService.startProcessInstanceByKey("terminateByEndEvent");
            Task task = processEngineTaskService.createTaskQuery()
                    .processInstanceId(processInstance.getProcessInstanceId())
                    .singleResult();
            assertThat(task).isNotNull();
            assertThat(task.getTaskDefinitionKey()).isEqualTo("formTask1");

            long numberOfActiveCaseInstances = cmmnRuntimeService.createCaseInstanceQuery()
                    .count();
            assertThat(numberOfActiveCaseInstances).isEqualTo(2);

            Execution caseTask1Execution = processEngineRuntimeService.createExecutionQuery().activityId("caseTask1").singleResult();
            assertThat(caseTask1Execution.getReferenceId()).isNotNull();
            assertThat(caseTask1Execution.getReferenceType()).isEqualTo(ReferenceTypes.EXECUTION_CHILD_CASE);

            // Act
            processEngineTaskService.complete(task.getId());

            // Assert
            long numberOfActiveCaseInstancesAfterCompletion = cmmnRuntimeService.createCaseInstanceQuery()
                    .count();
            assertThat(numberOfActiveCaseInstancesAfterCompletion).isZero();
        } finally {
            processEngineRepositoryService.deleteDeployment(deployment.getId(), true);
        }
    }

    @Test
    @CmmnDeployment(resources = { "org/flowable/cmmn/test/CaseTaskTest.testCaseTask.cmmn" })
    public void testCaseTaskWithTwoCaseTasksWithTerminateEndEventWithoutReference() {
        Deployment deployment = processEngineRepositoryService.createDeployment()
                .addClasspathResource("org/flowable/cmmn/test/caseTaskProcessWithTwoParallelCasesAndWithTerminateEndEventOnDifferentExecution.bpmn20.xml")
                .deploy();

        try {
            // Arrange
            ProcessInstance processInstance = processEngineRuntimeService.startProcessInstanceByKey("terminateByEndEvent");
            Task task = processEngineTaskService.createTaskQuery()
                    .processInstanceId(processInstance.getProcessInstanceId())
                    .singleResult();
            assertThat(task).isNotNull();
            assertThat(task.getTaskDefinitionKey()).isEqualTo("formTask1");

            processEngineManagementService.executeCommand(new ClearExecutionReferenceCmd());

            long numberOfActiveCaseInstances = cmmnRuntimeService.createCaseInstanceQuery()
                    .count();
            assertThat(numberOfActiveCaseInstances).isEqualTo(2);

            Execution caseTask1Execution = processEngineRuntimeService.createExecutionQuery().activityId("caseTask1").singleResult();
            assertThat(caseTask1Execution.getReferenceId()).isNull();
            assertThat(caseTask1Execution.getReferenceType()).isNull();
            // Act
            processEngineTaskService.complete(task.getId());

            // Assert
            long numberOfActiveCaseInstancesAfterCompletion = cmmnRuntimeService.createCaseInstanceQuery()
                    .count();
            assertThat(numberOfActiveCaseInstancesAfterCompletion).isZero();
        } finally {
            processEngineRepositoryService.deleteDeployment(deployment.getId(), true);
        }
    }

    @Test
    @CmmnDeployment(resources = { "org/flowable/cmmn/test/CaseTaskTest.testCaseTask.cmmn" })
    public void testCaseTaskWithTerminatingSignalBoundaryEvent() {
        Deployment deployment = processEngineRepositoryService.createDeployment()
                .addClasspathResource("org/flowable/cmmn/test/caseTaskProcessWithTerminatingSignalBoundaryEvent.bpmn20.xml")
                .deploy();

        try {
            // Arrange
            ProcessInstance processInstance = processEngineRuntimeService.startProcessInstanceByKey("terminateBySignalTestCase");
            long numberOfActiveCaseInstances = cmmnRuntimeService.createCaseInstanceQuery()
                    .count();
            assertThat(numberOfActiveCaseInstances).isEqualTo(1);

            Execution myExternalSignalExecution = processEngineRuntimeService.createExecutionQuery()
                    .processInstanceId(processInstance.getProcessInstanceId())
                    .signalEventSubscriptionName("myExternalSignal")
                    .singleResult();

            Execution caseTask1Execution = processEngineRuntimeService.createExecutionQuery().activityId("caseTask1").singleResult();
            assertThat(caseTask1Execution.getReferenceId()).isNotNull();
            assertThat(caseTask1Execution.getReferenceType()).isEqualTo(ReferenceTypes.EXECUTION_CHILD_CASE);

            CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceQuery().singleResult();
            assertThat(caseInstance.getId()).isEqualTo(caseTask1Execution.getReferenceId());

            // Act
            processEngineRuntimeService.signalEventReceived("myExternalSignal", myExternalSignalExecution.getId());

            // Assert
            long numberOfActiveCaseInstancesAfterCompletion = cmmnRuntimeService.createCaseInstanceQuery()
                    .count();
            assertThat(numberOfActiveCaseInstancesAfterCompletion).isZero();
        } finally {
            processEngineRepositoryService.deleteDeployment(deployment.getId(), true);
        }

    }

    @Test
    @CmmnDeployment(resources = { "org/flowable/cmmn/test/CaseTaskTest.testCaseTask.cmmn" })
    public void testCaseTaskWithTerminatingSignalBoundaryEventWithoutReference() {
        Deployment deployment = processEngineRepositoryService.createDeployment()
                .addClasspathResource("org/flowable/cmmn/test/caseTaskProcessWithTerminatingSignalBoundaryEvent.bpmn20.xml")
                .deploy();

        try {
            // Arrange
            ProcessInstance processInstance = processEngineRuntimeService.startProcessInstanceByKey("terminateBySignalTestCase");
            long numberOfActiveCaseInstances = cmmnRuntimeService.createCaseInstanceQuery()
                    .count();
            assertThat(numberOfActiveCaseInstances).isEqualTo(1);

            processEngineManagementService.executeCommand(new ClearExecutionReferenceCmd());

            Execution myExternalSignalExecution = processEngineRuntimeService.createExecutionQuery()
                    .processInstanceId(processInstance.getProcessInstanceId())
                    .signalEventSubscriptionName("myExternalSignal")
                    .singleResult();

            Execution caseTask1Execution = processEngineRuntimeService.createExecutionQuery().activityId("caseTask1").singleResult();
            assertThat(caseTask1Execution.getReferenceId()).isNull();
            assertThat(caseTask1Execution.getReferenceType()).isNull();

            // Act
            processEngineRuntimeService.signalEventReceived("myExternalSignal", myExternalSignalExecution.getId());

            // Assert
            long numberOfActiveCaseInstancesAfterCompletion = cmmnRuntimeService.createCaseInstanceQuery()
                    .count();
            assertThat(numberOfActiveCaseInstancesAfterCompletion).isZero();
        } finally {
            processEngineRepositoryService.deleteDeployment(deployment.getId(), true);
        }

    }

    @Test
    @CmmnDeployment(resources = { "org/flowable/cmmn/test/CaseTaskTest.testCaseTask.cmmn" })
    public void testCaseTasksInSubProcessWithTerminatingSignalBoundaryEvent() {
        Deployment deployment = processEngineRepositoryService.createDeployment()
                .addClasspathResource("org/flowable/cmmn/test/caseTasksInSubProcessWithTerminatingSignalBoundaryEvent.bpmn20.xml")
                .deploy();

        try {
            // Arrange
            ProcessInstance processInstance = processEngineRuntimeService.startProcessInstanceByKey("terminateTwoCasesWithinSubprocessBySignalEvent");
            long numberOfActiveCaseInstances = cmmnRuntimeService.createCaseInstanceQuery()
                    .count();
            assertThat(numberOfActiveCaseInstances).isEqualTo(2);

            Execution myExternalSignalExecution = processEngineRuntimeService.createExecutionQuery()
                    .processInstanceId(processInstance.getProcessInstanceId())
                    .signalEventSubscriptionName("myExternalSignal")
                    .singleResult();

            Execution caseTask1Execution = processEngineRuntimeService.createExecutionQuery().activityId("caseTask1").singleResult();
            assertThat(caseTask1Execution.getReferenceId()).isNotNull();
            assertThat(caseTask1Execution.getReferenceType()).isEqualTo(ReferenceTypes.EXECUTION_CHILD_CASE);

            // Act
            processEngineRuntimeService.signalEventReceived("myExternalSignal", myExternalSignalExecution.getId());

            // Assert
            long numberOfActiveCaseInstancesAfterCompletion = cmmnRuntimeService.createCaseInstanceQuery()
                    .count();
            assertThat(numberOfActiveCaseInstancesAfterCompletion).isZero();
        } finally {
            processEngineRepositoryService.deleteDeployment(deployment.getId(), true);
        }

    }

    @Test
    @CmmnDeployment(resources = { "org/flowable/cmmn/test/CaseTaskTest.testCaseTask.cmmn" })
    public void testCaseTasksInSubProcessWithTerminatingSignalBoundaryEventWithoutReference() {
        Deployment deployment = processEngineRepositoryService.createDeployment()
                .addClasspathResource("org/flowable/cmmn/test/caseTasksInSubProcessWithTerminatingSignalBoundaryEvent.bpmn20.xml")
                .deploy();

        try {
            // Arrange
            ProcessInstance processInstance = processEngineRuntimeService.startProcessInstanceByKey("terminateTwoCasesWithinSubprocessBySignalEvent");
            long numberOfActiveCaseInstances = cmmnRuntimeService.createCaseInstanceQuery()
                    .count();
            assertThat(numberOfActiveCaseInstances).isEqualTo(2);

            processEngineManagementService.executeCommand(new ClearExecutionReferenceCmd());

            Execution myExternalSignalExecution = processEngineRuntimeService.createExecutionQuery()
                    .processInstanceId(processInstance.getProcessInstanceId())
                    .signalEventSubscriptionName("myExternalSignal")
                    .singleResult();

            Execution caseTask1Execution = processEngineRuntimeService.createExecutionQuery().activityId("caseTask1").singleResult();
            assertThat(caseTask1Execution.getReferenceId()).isNull();
            assertThat(caseTask1Execution.getReferenceType()).isNull();

            // Act
            processEngineRuntimeService.signalEventReceived("myExternalSignal", myExternalSignalExecution.getId());

            // Assert
            long numberOfActiveCaseInstancesAfterCompletion = cmmnRuntimeService.createCaseInstanceQuery()
                    .count();
            assertThat(numberOfActiveCaseInstancesAfterCompletion).isZero();
        } finally {
            processEngineRepositoryService.deleteDeployment(deployment.getId(), true);
        }

    }

    @Test
    @CmmnDeployment(resources = { "org/flowable/cmmn/test/CaseTaskTest.testCaseTask.cmmn" })
    public void testCaseTasksWithTerminatingSignalBoundaryEventOnOnlyOneOfTwoCaseTask() {
        Deployment deployment = processEngineRepositoryService.createDeployment()
                .addClasspathResource("org/flowable/cmmn/test/caseTaskProcessWithTerminatingSignalBoundaryEventOnOnlyOneOfTwoCaseTask.bpmn20.xml")
                .deploy();

        try {
            // Arrange
            ProcessInstance processInstance = processEngineRuntimeService.startProcessInstanceByKey("terminateOneOfTwoCasesBySignalTestCase");
            long numberOfActiveCaseInstances = cmmnRuntimeService.createCaseInstanceQuery()
                    .count();
            assertThat(numberOfActiveCaseInstances).isEqualTo(2);

            Execution myExternalSignalExecution = processEngineRuntimeService.createExecutionQuery()
                    .processInstanceId(processInstance.getProcessInstanceId())
                    .signalEventSubscriptionName("myExternalSignal")
                    .singleResult();

            Execution caseTask1Execution = processEngineRuntimeService.createExecutionQuery().activityId("caseTask1").singleResult();
            assertThat(caseTask1Execution.getReferenceId()).isNotNull();
            assertThat(caseTask1Execution.getReferenceType()).isEqualTo(ReferenceTypes.EXECUTION_CHILD_CASE);

            // Act
            processEngineRuntimeService.signalEventReceived("myExternalSignal", myExternalSignalExecution.getId());

            // Assert
            long numberOfActiveCaseInstancesAfterCompletion = cmmnRuntimeService.createCaseInstanceQuery()
                    .count();
            assertThat(numberOfActiveCaseInstancesAfterCompletion).isEqualTo(1);
            CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceQuery()
                    .singleResult();
            cmmnRuntimeService.terminateCaseInstance(caseInstance.getId());
        } finally {
            processEngineRepositoryService.deleteDeployment(deployment.getId(), true);
        }

    }

    @Test
    @CmmnDeployment(resources = { "org/flowable/cmmn/test/CaseTaskTest.testCaseTask.cmmn" })
    public void testCaseTasksWithTerminatingSignalBoundaryEventOnOnlyOneOfTwoCaseTaskWithoutReference() {
        Deployment deployment = processEngineRepositoryService.createDeployment()
                .addClasspathResource("org/flowable/cmmn/test/caseTaskProcessWithTerminatingSignalBoundaryEventOnOnlyOneOfTwoCaseTask.bpmn20.xml")
                .deploy();

        try {
            // Arrange
            ProcessInstance processInstance = processEngineRuntimeService.startProcessInstanceByKey("terminateOneOfTwoCasesBySignalTestCase");
            long numberOfActiveCaseInstances = cmmnRuntimeService.createCaseInstanceQuery()
                    .count();
            assertThat(numberOfActiveCaseInstances).isEqualTo(2);

            processEngineManagementService.executeCommand(new ClearExecutionReferenceCmd());

            Execution myExternalSignalExecution = processEngineRuntimeService.createExecutionQuery()
                    .processInstanceId(processInstance.getProcessInstanceId())
                    .signalEventSubscriptionName("myExternalSignal")
                    .singleResult();

            Execution caseTask1Execution = processEngineRuntimeService.createExecutionQuery().activityId("caseTask1").singleResult();
            assertThat(caseTask1Execution.getReferenceId()).isNull();
            assertThat(caseTask1Execution.getReferenceType()).isNull();

            // Act
            processEngineRuntimeService.signalEventReceived("myExternalSignal", myExternalSignalExecution.getId());

            // Assert
            long numberOfActiveCaseInstancesAfterCompletion = cmmnRuntimeService.createCaseInstanceQuery()
                    .count();
            assertThat(numberOfActiveCaseInstancesAfterCompletion).isEqualTo(1);
            CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceQuery()
                    .singleResult();
            cmmnRuntimeService.terminateCaseInstance(caseInstance.getId());
        } finally {
            processEngineRepositoryService.deleteDeployment(deployment.getId(), true);
        }

    }

    @Test
    @CmmnDeployment(resources = { "org/flowable/cmmn/test/CaseTaskTest.testCaseTask.cmmn" })
    public void testCompleteCaseTaskWithSuspendedParentProcessInstance() {
        Deployment deployment = processEngineRepositoryService.createDeployment()
            .addClasspathResource("org/flowable/cmmn/test/caseTaskProcess.bpmn20.xml")
            .deploy();

        try {
            ProcessInstance processInstance = processEngineRuntimeService.startProcessInstanceByKey("caseTask");
            processEngineTaskService.complete(processEngineTaskService.createTaskQuery().singleResult().getId());
            CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceQuery().singleResult();

            processEngineRuntimeService.suspendProcessInstanceById(processInstance.getId());

            Task taskInCaseInstance = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();

            assertThatThrownBy(() -> cmmnTaskService.complete(taskInCaseInstance.getId()))
                    .isInstanceOf(FlowableException.class)
                    .hasMessageContaining("Cannot complete case task. Parent process instance")
                    .hasMessageContaining("is suspended");

            processEngineRuntimeService.activateProcessInstanceById(processInstance.getId());
            cmmnTaskService.complete(taskInCaseInstance.getId());
            assertCaseInstanceEnded(caseInstance);

        } finally {
            processEngineRepositoryService.deleteDeployment(deployment.getId(), true);
        }
    }

    static class ClearExecutionReferenceCmd implements Command<Void> {

        @Override
        public Void execute(CommandContext commandContext) {
            ProcessEngineConfigurationImpl processEngineConfiguration = (ProcessEngineConfigurationImpl) processEngine.getProcessEngineConfiguration();
            List<Execution> query = new ExecutionQueryImpl(processEngineConfiguration.getCommandExecutor(), processEngineConfiguration).list();
            ExecutionEntityManager entityManager = CommandContextUtil.getExecutionEntityManager(commandContext);
            for (Execution execution : query) {
                ExecutionEntity executionEntity = (ExecutionEntity) execution;
                executionEntity.setReferenceId(null);
                executionEntity.setReferenceType(null);
                entityManager.update(executionEntity);
            }

            return null;
        }
    }

}
