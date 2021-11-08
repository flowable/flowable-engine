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
import static org.assertj.core.api.Assertions.entry;
import static org.assertj.core.api.Assertions.tuple;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flowable.cmmn.api.CallbackTypes;
import org.flowable.cmmn.api.history.HistoricCaseInstance;
import org.flowable.cmmn.api.history.HistoricMilestoneInstance;
import org.flowable.cmmn.api.history.HistoricPlanItemInstance;
import org.flowable.cmmn.api.repository.CaseDefinitionQuery;
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.api.runtime.CaseInstanceBuilder;
import org.flowable.cmmn.api.runtime.CaseInstanceState;
import org.flowable.cmmn.api.runtime.PlanItemDefinitionType;
import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstanceState;
import org.flowable.cmmn.api.runtime.UserEventListenerInstance;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.engine.test.impl.CmmnHistoryTestHelper;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.common.engine.api.constant.ReferenceTypes;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.common.engine.impl.DefaultTenantProvider;
import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.common.engine.impl.util.CollectionUtil;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.test.HistoryTestHelper;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.entitylink.api.EntityLink;
import org.flowable.entitylink.api.EntityLinkType;
import org.flowable.entitylink.api.HierarchyType;
import org.flowable.entitylink.api.history.HistoricEntityLink;
import org.flowable.task.api.Task;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.flowable.task.api.history.HistoricTaskLogEntry;
import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * @author Joram Barrez
 * @author Filip Hrisafov
 */
public class ProcessTaskTest extends AbstractProcessEngineIntegrationTest {

    @Before
    public void deployOneTaskProcess() {
        if (processEngineRepositoryService.createDeploymentQuery().count() == 0) {
            processEngineRepositoryService.createDeployment().addClasspathResource("org/flowable/cmmn/test/oneTaskProcess.bpmn20.xml").deploy();
        }
    }

    @Test
    @CmmnDeployment
    public void testOneTaskProcessNonBlocking() {
        CaseInstance caseInstance = startCaseInstanceWithOneTaskProcess();
        List<Task> processTasks = processEngine.getTaskService().createTaskQuery().list();
        assertThat(processTasks).hasSize(1);

        // Non-blocking process task, plan item should have been completed
        List<PlanItemInstance> planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .planItemInstanceState(PlanItemInstanceState.ACTIVE)
                .list();
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getName)
                .containsExactly("Task Two");

        PlanItemInstance processTaskPlanItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .planItemDefinitionType(PlanItemDefinitionType.PROCESS_TASK)
                .includeEnded()
                .singleResult();
        assertThat(processTaskPlanItemInstance).isNotNull();
        assertThat(processTaskPlanItemInstance.getState()).isEqualTo(PlanItemInstanceState.COMPLETED);

        ProcessInstance childProcessInstance = processEngine.getRuntimeService()
                .createProcessInstanceQuery()
                .singleResult();
        assertThat(childProcessInstance).isNotNull();

        // There is no callback id since the task is non blocking
        assertThat(childProcessInstance.getCallbackId()).isNull();
        assertThat(childProcessInstance.getCallbackType()).isNull();

        assertThat(processTaskPlanItemInstance.getReferenceId()).isEqualTo(childProcessInstance.getId());
        assertThat(processTaskPlanItemInstance.getReferenceType()).isEqualTo(ReferenceTypes.PLAN_ITEM_CHILD_PROCESS);

        if (cmmnEngineConfiguration.isEnableEntityLinks()) {
            assertThat(cmmnRuntimeService.getEntityLinkChildrenForCaseInstance(caseInstance.getId())).isEmpty();
        }

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            assertThat(cmmnHistoryService.createHistoricMilestoneInstanceQuery().count()).isEqualTo(1);
            HistoricPlanItemInstance historicProcessTaskPlanItemInstance = cmmnHistoryService.createHistoricPlanItemInstanceQuery()
                    .planItemInstanceId(processTaskPlanItemInstance.getId())
                    .singleResult();
            assertThat(historicProcessTaskPlanItemInstance.getState()).isEqualTo(PlanItemInstanceState.COMPLETED);
            assertThat(historicProcessTaskPlanItemInstance.getReferenceId()).isEqualTo(childProcessInstance.getId());
            assertThat(historicProcessTaskPlanItemInstance.getReferenceType()).isEqualTo(ReferenceTypes.PLAN_ITEM_CHILD_PROCESS);

            assertThat(cmmnHistoryService.getHistoricEntityLinkChildrenForCaseInstance(caseInstance.getId())).isEmpty();
        }

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, (ProcessEngineConfigurationImpl) processEngine.getProcessEngineConfiguration())) {
            HistoricProcessInstance historicChildProcessInstance = processEngine.getHistoryService()
                    .createHistoricProcessInstanceQuery()
                    .processInstanceId(childProcessInstance.getId())
                    .singleResult();
            // There is no callback id since the task is non blocking
            assertThat(historicChildProcessInstance.getCallbackId()).isNull();
            assertThat(historicChildProcessInstance.getCallbackType()).isNull();
        }

        processEngine.getTaskService().complete(processTasks.get(0).getId());
        assertThat(processEngineRuntimeService.createProcessInstanceQuery().count()).isZero();
    }

    @Test
    @CmmnDeployment
    public void testOneCallActivityProcessBlocking() {
        Deployment deployment = processEngine.getRepositoryService().createDeployment()
                .addClasspathResource("org/flowable/cmmn/test/oneCallActivityProcess.bpmn20.xml")
                .addClasspathResource("org/flowable/cmmn/test/oneTaskProcess.bpmn20.xml")
                .deploy();

        try {
            CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                    .caseDefinitionKey("myCase")
                    .start();

            List<PlanItemInstance> planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
                    .caseInstanceId(caseInstance.getId())
                    .planItemInstanceState(PlanItemInstanceState.ACTIVE)
                    .list();

            assertThat(planItemInstances).hasSize(1);
            cmmnRuntimeService.triggerPlanItemInstance(planItemInstances.get(0).getId());

            List<Task> processTasks = processEngine.getTaskService().createTaskQuery().list();
            assertThat(processTasks).hasSize(1);
            Task processTask = processTasks.get(0);
            String subProcessInstanceId = processTask.getProcessInstanceId();
            ProcessInstance processInstance = processEngine.getRuntimeService().createProcessInstanceQuery().subProcessInstanceId(subProcessInstanceId)
                    .singleResult();

            Task task = cmmnTaskService.createTaskQuery().caseInstanceIdWithChildren(caseInstance.getId()).singleResult();
            assertThat(task.getId()).isEqualTo(processTask.getId());

            task = cmmnTaskService.createTaskQuery().processInstanceIdWithChildren(processInstance.getId()).singleResult();
            assertThat(task.getId()).isEqualTo(processTask.getId());

            List<EntityLink> entityLinks = cmmnRuntimeService.getEntityLinkChildrenForCaseInstance(caseInstance.getId());
            assertThat(entityLinks).hasSize(3);
            EntityLink processEntityLink = null;
            EntityLink subProcessEntityLink = null;
            EntityLink taskEntityLink = null;
            for (EntityLink entityLink : entityLinks) {
                if (ScopeTypes.BPMN.equals(entityLink.getReferenceScopeType())) {
                    if (processInstance.getId().equals(entityLink.getReferenceScopeId())) {
                        processEntityLink = entityLink;
                    } else {
                        subProcessEntityLink = entityLink;
                    }

                } else if (ScopeTypes.TASK.equals(entityLink.getReferenceScopeType())) {
                    taskEntityLink = entityLink;
                }
            }

            assertThat(processEntityLink.getLinkType()).isEqualTo(EntityLinkType.CHILD);
            assertThat(processEntityLink.getCreateTime()).isNotNull();
            assertThat(processEntityLink.getScopeId()).isEqualTo(caseInstance.getId());
            assertThat(processEntityLink.getScopeType()).isEqualTo(ScopeTypes.CMMN);
            assertThat(processEntityLink.getScopeDefinitionId()).isNull();
            assertThat(processEntityLink.getReferenceScopeId()).isEqualTo(processInstance.getId());
            assertThat(processEntityLink.getReferenceScopeType()).isEqualTo(ScopeTypes.BPMN);
            assertThat(processEntityLink.getReferenceScopeDefinitionId()).isNull();
            assertThat(processEntityLink.getHierarchyType()).isEqualTo(HierarchyType.ROOT);

            assertThat(subProcessEntityLink.getLinkType()).isEqualTo(EntityLinkType.CHILD);
            assertThat(subProcessEntityLink.getCreateTime()).isNotNull();
            assertThat(subProcessEntityLink.getScopeId()).isEqualTo(caseInstance.getId());
            assertThat(subProcessEntityLink.getScopeType()).isEqualTo(ScopeTypes.CMMN);
            assertThat(subProcessEntityLink.getScopeDefinitionId()).isNull();
            assertThat(subProcessEntityLink.getReferenceScopeId()).isEqualTo(subProcessInstanceId);
            assertThat(subProcessEntityLink.getReferenceScopeType()).isEqualTo(ScopeTypes.BPMN);
            assertThat(subProcessEntityLink.getReferenceScopeDefinitionId()).isNull();
            assertThat(subProcessEntityLink.getHierarchyType()).isEqualTo(HierarchyType.ROOT);

            assertThat(taskEntityLink.getLinkType()).isEqualTo(EntityLinkType.CHILD);
            assertThat(taskEntityLink.getCreateTime()).isNotNull();
            assertThat(taskEntityLink.getScopeId()).isEqualTo(caseInstance.getId());
            assertThat(taskEntityLink.getScopeType()).isEqualTo(ScopeTypes.CMMN);
            assertThat(taskEntityLink.getScopeDefinitionId()).isNull();
            assertThat(taskEntityLink.getReferenceScopeId()).isEqualTo(processTasks.get(0).getId());
            assertThat(taskEntityLink.getReferenceScopeType()).isEqualTo(ScopeTypes.TASK);
            assertThat(taskEntityLink.getReferenceScopeDefinitionId()).isNull();
            assertThat(taskEntityLink.getHierarchyType()).isEqualTo(HierarchyType.ROOT);

            entityLinks = processEngine.getRuntimeService().getEntityLinkChildrenForProcessInstance(processInstance.getId());
            assertThat(entityLinks).hasSize(2);

            entityLinks = processEngine.getRuntimeService().getEntityLinkChildrenForProcessInstance(subProcessInstanceId);
            assertThat(entityLinks).hasSize(1);
            EntityLink entityLink = entityLinks.get(0);
            assertThat(entityLink.getLinkType()).isEqualTo(EntityLinkType.CHILD);
            assertThat(entityLink.getCreateTime()).isNotNull();
            assertThat(entityLink.getScopeId()).isEqualTo(subProcessInstanceId);
            assertThat(entityLink.getScopeType()).isEqualTo(ScopeTypes.BPMN);
            assertThat(entityLink.getScopeDefinitionId()).isNull();
            assertThat(entityLink.getReferenceScopeId()).isEqualTo(processTasks.get(0).getId());
            assertThat(entityLink.getReferenceScopeType()).isEqualTo(ScopeTypes.TASK);
            assertThat(entityLink.getReferenceScopeDefinitionId()).isNull();
            assertThat(entityLink.getHierarchyType()).isEqualTo(HierarchyType.PARENT);

            entityLinks = processEngine.getRuntimeService().getEntityLinkParentsForTask(processTask.getId());
            assertThat(entityLinks).hasSize(3);
            entityLink = entityLinks.get(0);
            assertThat(entityLink.getLinkType()).isEqualTo(EntityLinkType.CHILD);
            assertThat(entityLink.getCreateTime()).isNotNull();

            processEngine.getTaskService().complete(processTasks.get(0).getId());
            assertThat(processEngineRuntimeService.createProcessInstanceQuery().count()).isZero();

            List<HistoricEntityLink> historicEntityLinks = cmmnHistoryService.getHistoricEntityLinkChildrenForCaseInstance(caseInstance.getId());
            assertThat(historicEntityLinks).hasSize(3);

            historicEntityLinks = processEngine.getHistoryService().getHistoricEntityLinkChildrenForProcessInstance(processInstance.getId());
            assertThat(historicEntityLinks).hasSize(2);

            historicEntityLinks = processEngine.getHistoryService().getHistoricEntityLinkChildrenForProcessInstance(subProcessInstanceId);
            assertThat(historicEntityLinks).hasSize(1);
            assertThat(historicEntityLinks.get(0).getHierarchyType()).isEqualTo(HierarchyType.PARENT);

            historicEntityLinks = processEngine.getHistoryService().getHistoricEntityLinkParentsForTask(processTasks.get(0).getId());
            assertThat(historicEntityLinks).hasSize(3);

            HistoricTaskInstance historicTask = cmmnHistoryService.createHistoricTaskInstanceQuery().caseInstanceIdWithChildren(caseInstance.getId())
                    .singleResult();
            assertThat(historicTask.getId()).isEqualTo(processTask.getId());

            historicTask = cmmnHistoryService.createHistoricTaskInstanceQuery().processInstanceIdWithChildren(processInstance.getId()).singleResult();
            assertThat(historicTask.getId()).isEqualTo(processTask.getId());

        } finally {
            processEngine.getRepositoryService().deleteDeployment(deployment.getId(), true);
        }
    }

    @Test
    @CmmnDeployment
    public void testNestedCallActivityProcessBlocking() {
        Deployment deployment = processEngine.getRepositoryService().createDeployment()
                .addClasspathResource("org/flowable/cmmn/test/nestedCallActivityProcess.bpmn20.xml")
                .addClasspathResource("org/flowable/cmmn/test/oneCallActivityProcess.bpmn20.xml")
                .addClasspathResource("org/flowable/cmmn/test/oneTaskProcess.bpmn20.xml")
                .deploy();

        try {
            CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                    .caseDefinitionKey("myCase")
                    .start();
            String caseInstanceId = caseInstance.getId();
            
            PlanItemInstance processTaskPlanItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery()
                    .caseInstanceId(caseInstance.getId())
                    .planItemDefinitionId("theProcess")
                    .singleResult();

            List<PlanItemInstance> planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
                    .caseInstanceId(caseInstance.getId())
                    .planItemInstanceState(PlanItemInstanceState.ACTIVE)
                    .list();

            assertThat(planItemInstances).hasSize(1);
            cmmnRuntimeService.triggerPlanItemInstance(planItemInstances.get(0).getId());

            Task processTask = processEngine.getTaskService().createTaskQuery().singleResult();
            String oneTaskProcessId = processTask.getProcessInstanceId();

            ProcessInstance oneTaskProcess = processEngine.getRuntimeService().createProcessInstanceQuery().processInstanceId(oneTaskProcessId).singleResult();
            assertThat(oneTaskProcess.getProcessDefinitionKey()).isEqualTo("oneTask");

            ProcessInstance callActivityProcess = processEngine.getRuntimeService().createProcessInstanceQuery().subProcessInstanceId(oneTaskProcessId).singleResult();
            assertThat(callActivityProcess.getProcessDefinitionKey()).isEqualTo("oneCallActivity");
            String callActivityProcessId = callActivityProcess.getId();
            Execution callActivityExecution = processEngine.getRuntimeService().createExecutionQuery().processInstanceId(callActivityProcessId).onlyChildExecutions().singleResult();

            ProcessInstance nestedCallActivityProcess = processEngine.getRuntimeService().createProcessInstanceQuery().subProcessInstanceId(callActivityProcessId).singleResult();
            assertThat(nestedCallActivityProcess.getProcessDefinitionKey()).isEqualTo("nestedCallActivity");
            String nestedCallActivityProcessId = nestedCallActivityProcess.getId();
            Execution nestedCallActivityExecution = processEngine.getRuntimeService().createExecutionQuery().processInstanceId(nestedCallActivityProcessId).onlyChildExecutions().singleResult();

            Task task = cmmnTaskService.createTaskQuery().caseInstanceIdWithChildren(caseInstance.getId()).singleResult();
            assertThat(task.getId()).isEqualTo(processTask.getId());

            task = cmmnTaskService.createTaskQuery().processInstanceIdWithChildren(nestedCallActivityProcessId).singleResult();
            assertThat(task.getId()).isEqualTo(processTask.getId());

            List<EntityLink> entityLinks = cmmnRuntimeService.getEntityLinkChildrenForCaseInstance(caseInstanceId);

            assertThat(entityLinks)
                    .extracting(EntityLink::getScopeId, EntityLink::getSubScopeId, EntityLink::getParentElementId, 
                            EntityLink::getScopeType, EntityLink::getHierarchyType, EntityLink::getReferenceScopeId,
                            EntityLink::getReferenceScopeType, EntityLink::getLinkType)
                    .as("scopeId, subScopeId, parentElementId, scopeType, hierarchyType, referenceScopeId, referenceScopeType, linkType")
                    .containsExactlyInAnyOrder(
                            tuple(caseInstanceId, processTaskPlanItemInstance.getId(), "theProcess", ScopeTypes.CMMN, HierarchyType.ROOT, nestedCallActivityProcessId, ScopeTypes.BPMN, EntityLinkType.CHILD),
                            tuple(caseInstanceId, nestedCallActivityExecution.getId(), "theTask", ScopeTypes.CMMN, HierarchyType.ROOT, callActivityProcessId, ScopeTypes.BPMN, EntityLinkType.CHILD),
                            tuple(caseInstanceId, callActivityExecution.getId(), "theTask", ScopeTypes.CMMN, HierarchyType.ROOT, oneTaskProcessId, ScopeTypes.BPMN, EntityLinkType.CHILD),
                            tuple(caseInstanceId, processTask.getExecutionId(), "theTask", ScopeTypes.CMMN, HierarchyType.ROOT, processTask.getId(), ScopeTypes.TASK, EntityLinkType.CHILD)
                    );

            assertThat(entityLinks)
                    .extracting(EntityLink::getRootScopeId, EntityLink::getRootScopeType)
                    .containsOnly(
                            tuple(caseInstanceId, ScopeTypes.CMMN)
                    );

            entityLinks = processEngineRuntimeService.getEntityLinkChildrenForProcessInstance(nestedCallActivityProcessId);

            assertThat(entityLinks)
                    .extracting(EntityLink::getScopeId, EntityLink::getSubScopeId, EntityLink::getParentElementId, 
                            EntityLink::getScopeType, EntityLink::getHierarchyType, EntityLink::getReferenceScopeId,
                            EntityLink::getReferenceScopeType, EntityLink::getLinkType)
                    .as("scopeId, subScopeId, parentElementId, scopeType, hierarchyType, referenceScopeId, referenceScopeType, linkType")
                    .containsExactlyInAnyOrder(
                            tuple(nestedCallActivityProcessId, nestedCallActivityExecution.getId(), "theTask", ScopeTypes.BPMN, HierarchyType.PARENT, callActivityProcessId, ScopeTypes.BPMN, EntityLinkType.CHILD),
                            tuple(nestedCallActivityProcessId, callActivityExecution.getId(), "theTask", ScopeTypes.BPMN, null, oneTaskProcessId, ScopeTypes.BPMN, EntityLinkType.CHILD),
                            tuple(nestedCallActivityProcessId, processTask.getExecutionId(), "theTask", ScopeTypes.BPMN, null, processTask.getId(), ScopeTypes.TASK, EntityLinkType.CHILD)
                    );

            assertThat(entityLinks)
                    .extracting(EntityLink::getRootScopeId, EntityLink::getRootScopeType)
                    .containsOnly(
                            tuple(caseInstanceId, ScopeTypes.CMMN)
                    );

            entityLinks = processEngineRuntimeService.getEntityLinkChildrenForProcessInstance(callActivityProcessId);

            assertThat(entityLinks)
                    .extracting(EntityLink::getScopeId, EntityLink::getSubScopeId, EntityLink::getParentElementId,
                            EntityLink::getScopeType, EntityLink::getHierarchyType, EntityLink::getReferenceScopeId,
                            EntityLink::getReferenceScopeType, EntityLink::getLinkType)
                    .as("scopeId, subScopeId, parentElementId, scopeType, hierarchyType, referenceScopeId, referenceScopeType, linkType")
                    .containsExactlyInAnyOrder(
                            tuple(callActivityProcessId, callActivityExecution.getId(), "theTask", ScopeTypes.BPMN, HierarchyType.PARENT, oneTaskProcessId, ScopeTypes.BPMN, EntityLinkType.CHILD),
                            tuple(callActivityProcessId, processTask.getExecutionId(), "theTask", ScopeTypes.BPMN, HierarchyType.GRAND_PARENT, processTask.getId(), ScopeTypes.TASK, EntityLinkType.CHILD)
                    );

            assertThat(entityLinks)
                    .extracting(EntityLink::getRootScopeId, EntityLink::getRootScopeType)
                    .containsOnly(
                            tuple(caseInstanceId, ScopeTypes.CMMN)
                    );

            entityLinks = processEngineRuntimeService.getEntityLinkChildrenForProcessInstance(oneTaskProcessId);

            assertThat(entityLinks)
                    .extracting(EntityLink::getScopeId, EntityLink::getSubScopeId, EntityLink::getParentElementId,
                            EntityLink::getScopeType, EntityLink::getHierarchyType, EntityLink::getReferenceScopeId,
                            EntityLink::getReferenceScopeType, EntityLink::getLinkType)
                    .as("scopeId, subScopeId, parentElementId, scopeType, hierarchyType, referenceScopeId, referenceScopeType, linkType")
                    .containsExactlyInAnyOrder(
                            tuple(oneTaskProcessId, processTask.getExecutionId(), "theTask", ScopeTypes.BPMN, HierarchyType.PARENT, processTask.getId(), ScopeTypes.TASK, EntityLinkType.CHILD)
                    );

            assertThat(entityLinks)
                    .extracting(EntityLink::getRootScopeId, EntityLink::getRootScopeType)
                    .containsOnly(
                            tuple(caseInstanceId, ScopeTypes.CMMN)
                    );

            entityLinks = processEngineRuntimeService.getEntityLinkParentsForTask(processTask.getId());

            assertThat(entityLinks)
                    .extracting(EntityLink::getScopeId, EntityLink::getSubScopeId, EntityLink::getParentElementId,
                            EntityLink::getScopeType, EntityLink::getHierarchyType, EntityLink::getReferenceScopeId,
                            EntityLink::getReferenceScopeType, EntityLink::getLinkType)
                    .as("scopeId, subScopeId, parentElementId, scopeType, hierarchyType, referenceScopeId, referenceScopeType, linkType")
                    .containsExactlyInAnyOrder(
                            tuple(caseInstanceId, processTask.getExecutionId(), "theTask", ScopeTypes.CMMN, HierarchyType.ROOT, processTask.getId(), ScopeTypes.TASK, EntityLinkType.CHILD),
                            tuple(nestedCallActivityProcessId, processTask.getExecutionId(), "theTask", ScopeTypes.BPMN, null, processTask.getId(), ScopeTypes.TASK, EntityLinkType.CHILD),
                            tuple(callActivityProcessId, processTask.getExecutionId(), "theTask", ScopeTypes.BPMN, HierarchyType.GRAND_PARENT, processTask.getId(), ScopeTypes.TASK, EntityLinkType.CHILD),
                            tuple(oneTaskProcessId, processTask.getExecutionId(), "theTask", ScopeTypes.BPMN, HierarchyType.PARENT, processTask.getId(), ScopeTypes.TASK, EntityLinkType.CHILD)
                    );

            assertThat(entityLinks)
                    .extracting(EntityLink::getRootScopeId, EntityLink::getRootScopeType)
                    .containsOnly(
                            tuple(caseInstanceId, ScopeTypes.CMMN)
                    );

            processEngineTaskService.complete(processTask.getId());
            assertThat(processEngineRuntimeService.createProcessInstanceQuery().count()).isZero();

            List<HistoricEntityLink> historicEntityLinks = cmmnHistoryService.getHistoricEntityLinkChildrenForCaseInstance(caseInstanceId);
            assertThat(historicEntityLinks)
                    .extracting(HistoricEntityLink::getScopeId, HistoricEntityLink::getSubScopeId, HistoricEntityLink::getParentElementId,
                            HistoricEntityLink::getScopeType, HistoricEntityLink::getHierarchyType,
                            HistoricEntityLink::getReferenceScopeId, HistoricEntityLink::getReferenceScopeType, HistoricEntityLink::getLinkType)
                    .as("scopeId, subScopeId, parentElementId, scopeType, hierarchyType, referenceScopeId, referenceScopeType, linkType")
                    .containsExactlyInAnyOrder(
                            tuple(caseInstanceId, processTaskPlanItemInstance.getId(), "theProcess", ScopeTypes.CMMN, HierarchyType.ROOT, nestedCallActivityProcessId, ScopeTypes.BPMN, EntityLinkType.CHILD),
                            tuple(caseInstanceId, nestedCallActivityExecution.getId(), "theTask", ScopeTypes.CMMN, HierarchyType.ROOT, callActivityProcessId, ScopeTypes.BPMN, EntityLinkType.CHILD),
                            tuple(caseInstanceId, callActivityExecution.getId(), "theTask", ScopeTypes.CMMN, HierarchyType.ROOT, oneTaskProcessId, ScopeTypes.BPMN, EntityLinkType.CHILD),
                            tuple(caseInstanceId, processTask.getExecutionId(), "theTask", ScopeTypes.CMMN, HierarchyType.ROOT, processTask.getId(), ScopeTypes.TASK, EntityLinkType.CHILD)
                    );

            assertThat(historicEntityLinks)
                    .extracting(HistoricEntityLink::getRootScopeId, HistoricEntityLink::getRootScopeType)
                    .containsOnly(
                            tuple(caseInstanceId, ScopeTypes.CMMN)
                    );

            historicEntityLinks = processEngineHistoryService.getHistoricEntityLinkChildrenForProcessInstance(nestedCallActivityProcessId);
            assertThat(historicEntityLinks)
                    .extracting(HistoricEntityLink::getScopeId, HistoricEntityLink::getSubScopeId, HistoricEntityLink::getParentElementId,
                            HistoricEntityLink::getScopeType, HistoricEntityLink::getHierarchyType,
                            HistoricEntityLink::getReferenceScopeId, HistoricEntityLink::getReferenceScopeType, HistoricEntityLink::getLinkType)
                    .as("scopeId, subScopeId, parentElementId, scopeType, hierarchyType, referenceScopeId, referenceScopeType, linkType")
                    .containsExactlyInAnyOrder(
                            tuple(nestedCallActivityProcessId, nestedCallActivityExecution.getId(), "theTask", ScopeTypes.BPMN, HierarchyType.PARENT, callActivityProcessId, ScopeTypes.BPMN, EntityLinkType.CHILD),
                            tuple(nestedCallActivityProcessId, callActivityExecution.getId(), "theTask", ScopeTypes.BPMN, null, oneTaskProcessId, ScopeTypes.BPMN, EntityLinkType.CHILD),
                            tuple(nestedCallActivityProcessId, processTask.getExecutionId(), "theTask", ScopeTypes.BPMN, null, processTask.getId(), ScopeTypes.TASK, EntityLinkType.CHILD)
                    );

            assertThat(historicEntityLinks)
                    .extracting(HistoricEntityLink::getRootScopeId, HistoricEntityLink::getRootScopeType)
                    .containsOnly(
                            tuple(caseInstanceId, ScopeTypes.CMMN)
                    );

            historicEntityLinks = processEngineHistoryService.getHistoricEntityLinkChildrenForProcessInstance(callActivityProcessId);
            assertThat(historicEntityLinks)
                    .extracting(HistoricEntityLink::getScopeId, HistoricEntityLink::getSubScopeId, HistoricEntityLink::getParentElementId,
                            HistoricEntityLink::getScopeType, HistoricEntityLink::getHierarchyType,
                            HistoricEntityLink::getReferenceScopeId, HistoricEntityLink::getReferenceScopeType, HistoricEntityLink::getLinkType)
                    .as("scopeId, subScopeId, parentElementId, scopeType, hierarchyType, referenceScopeId, referenceScopeType, linkType")
                    .containsExactlyInAnyOrder(
                            tuple(callActivityProcessId, callActivityExecution.getId(), "theTask", ScopeTypes.BPMN, HierarchyType.PARENT, oneTaskProcessId, ScopeTypes.BPMN, EntityLinkType.CHILD),
                            tuple(callActivityProcessId, processTask.getExecutionId(), "theTask", ScopeTypes.BPMN, HierarchyType.GRAND_PARENT, processTask.getId(), ScopeTypes.TASK, EntityLinkType.CHILD)
                    );

            assertThat(historicEntityLinks)
                    .extracting(HistoricEntityLink::getRootScopeId, HistoricEntityLink::getRootScopeType)
                    .containsOnly(
                            tuple(caseInstanceId, ScopeTypes.CMMN)
                    );

            historicEntityLinks = processEngineHistoryService.getHistoricEntityLinkChildrenForProcessInstance(oneTaskProcessId);
            assertThat(historicEntityLinks)
                    .extracting(HistoricEntityLink::getScopeId, HistoricEntityLink::getSubScopeId, HistoricEntityLink::getParentElementId,
                            HistoricEntityLink::getScopeType, HistoricEntityLink::getHierarchyType,
                            HistoricEntityLink::getReferenceScopeId, HistoricEntityLink::getReferenceScopeType, HistoricEntityLink::getLinkType)
                    .as("scopeId, subScopeId, parentElementId, scopeType, hierarchyType, referenceScopeId, referenceScopeType, linkType")
                    .containsExactlyInAnyOrder(
                            tuple(oneTaskProcessId, processTask.getExecutionId(), "theTask", ScopeTypes.BPMN, HierarchyType.PARENT, processTask.getId(), ScopeTypes.TASK, EntityLinkType.CHILD)
                    );

            assertThat(historicEntityLinks)
                    .extracting(HistoricEntityLink::getRootScopeId, HistoricEntityLink::getRootScopeType)
                    .containsOnly(
                            tuple(caseInstanceId, ScopeTypes.CMMN)
                    );

            historicEntityLinks = processEngineHistoryService.getHistoricEntityLinkParentsForTask(processTask.getId());
            assertThat(historicEntityLinks)
                    .extracting(HistoricEntityLink::getScopeId, HistoricEntityLink::getSubScopeId, HistoricEntityLink::getParentElementId,
                            HistoricEntityLink::getScopeType, HistoricEntityLink::getHierarchyType,
                            HistoricEntityLink::getReferenceScopeId, HistoricEntityLink::getReferenceScopeType, HistoricEntityLink::getLinkType)
                    .as("scopeId, subScopeId, parentElementId, scopeType, hierarchyType, referenceScopeId, referenceScopeType, linkType")
                    .containsExactlyInAnyOrder(
                            tuple(caseInstanceId, processTask.getExecutionId(), "theTask", ScopeTypes.CMMN, HierarchyType.ROOT, processTask.getId(), ScopeTypes.TASK, EntityLinkType.CHILD),
                            tuple(nestedCallActivityProcessId, processTask.getExecutionId(), "theTask", ScopeTypes.BPMN, null, processTask.getId(), ScopeTypes.TASK, EntityLinkType.CHILD),
                            tuple(callActivityProcessId, processTask.getExecutionId(), "theTask", ScopeTypes.BPMN, HierarchyType.GRAND_PARENT, processTask.getId(), ScopeTypes.TASK, EntityLinkType.CHILD),
                            tuple(oneTaskProcessId, processTask.getExecutionId(), "theTask", ScopeTypes.BPMN, HierarchyType.PARENT, processTask.getId(), ScopeTypes.TASK, EntityLinkType.CHILD)
                    );

            assertThat(historicEntityLinks)
                    .extracting(HistoricEntityLink::getRootScopeId, HistoricEntityLink::getRootScopeType)
                    .containsOnly(
                            tuple(caseInstanceId, ScopeTypes.CMMN)
                    );

            HistoricTaskInstance historicTask = cmmnHistoryService.createHistoricTaskInstanceQuery()
                    .caseInstanceIdWithChildren(caseInstanceId)
                    .singleResult();
            assertThat(historicTask.getId()).isEqualTo(processTask.getId());

            historicTask = cmmnHistoryService.createHistoricTaskInstanceQuery().processInstanceIdWithChildren(nestedCallActivityProcessId).singleResult();
            assertThat(historicTask.getId()).isEqualTo(processTask.getId());

            historicTask = cmmnHistoryService.createHistoricTaskInstanceQuery().processInstanceIdWithChildren(oneTaskProcessId).singleResult();
            assertThat(historicTask.getId()).isEqualTo(processTask.getId());

        } finally {
            processEngine.getRepositoryService().deleteDeployment(deployment.getId(), true);
        }
    }


    @Test
    @CmmnDeployment
    public void testNestedCallActivityTerminate() {
        Deployment deployment = processEngine.getRepositoryService().createDeployment()
            .addClasspathResource("org/flowable/cmmn/test/nestedCallActivityProcess.bpmn20.xml")
            .addClasspathResource("org/flowable/cmmn/test/oneCallActivityProcess.bpmn20.xml")
            .addClasspathResource("org/flowable/cmmn/test/oneTaskProcess.bpmn20.xml")
            .deploy();

        try {
            CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("myCase")
                .start();
            String caseInstanceId = caseInstance.getId();

            cmmnRuntimeService.terminateCaseInstance(caseInstanceId);
            HistoricPlanItemInstance processHistoricPlanItemInstance = cmmnHistoryService.createHistoricPlanItemInstanceQuery()
                .planItemInstanceDefinitionType(PlanItemDefinitionType.PROCESS_TASK)
                .singleResult();
            assertThat(processHistoricPlanItemInstance.getState()).isEqualTo(PlanItemInstanceState.TERMINATED);

        } finally {
            processEngine.getRepositoryService().deleteDeployment(deployment.getId(), true);
        }
    }

    @Test
    @CmmnDeployment
    public void testNestedCallActivityProcessWithProcessTaskInStage() {
        Deployment deployment = processEngine.getRepositoryService().createDeployment()
                .addClasspathResource("org/flowable/cmmn/test/nestedCallActivityProcess.bpmn20.xml")
                .addClasspathResource("org/flowable/cmmn/test/oneCallActivityProcess.bpmn20.xml")
                .addClasspathResource("org/flowable/cmmn/test/oneTaskProcess.bpmn20.xml")
                .deploy();

        try {
            CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                    .caseDefinitionKey("myCase")
                    .start();

            PlanItemInstance taskPlanItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery()
                    .caseInstanceId(caseInstance.getId())
                    .planItemInstanceStateActive()
                    .planItemDefinitionType("task")
                    .singleResult();
            cmmnRuntimeService.triggerPlanItemInstance(taskPlanItemInstance.getId());

            PlanItemInstance processTaskPlanItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery()
                    .caseInstanceId(caseInstance.getId())
                    .planItemInstanceStateActive()
                    .planItemDefinitionType(PlanItemDefinitionType.PROCESS_TASK)
                    .singleResult();

            String stageInstanceId = processTaskPlanItemInstance.getStageInstanceId();
            assertThat(stageInstanceId).isNotNull();

            // The stage instance id should be propagated to all entities

            Task processTask = processEngine.getTaskService().createTaskQuery().singleResult();
            assertThat(processTask.getPropagatedStageInstanceId()).isEqualTo(stageInstanceId);
            String oneTaskProcessId = processTask.getProcessInstanceId();

            ProcessInstance oneTaskProcess = processEngine.getRuntimeService().createProcessInstanceQuery().processInstanceId(oneTaskProcessId).singleResult();
            assertThat(oneTaskProcess.getPropagatedStageInstanceId()).isEqualTo(stageInstanceId);
            assertThat(oneTaskProcess.getProcessDefinitionKey()).isEqualTo("oneTask");

            ProcessInstance callActivityProcess = processEngine.getRuntimeService().createProcessInstanceQuery().subProcessInstanceId(oneTaskProcessId).singleResult();
            assertThat(callActivityProcess.getPropagatedStageInstanceId()).isEqualTo(stageInstanceId);
            assertThat(callActivityProcess.getProcessDefinitionKey()).isEqualTo("oneCallActivity");
            String callActivityProcessId = callActivityProcess.getId();
            Execution callActivityExecution = processEngine.getRuntimeService().createExecutionQuery().processInstanceId(callActivityProcessId).onlyChildExecutions().singleResult();
            assertThat(callActivityExecution.getPropagatedStageInstanceId()).isEqualTo(stageInstanceId);

            ProcessInstance nestedCallActivityProcess = processEngine.getRuntimeService().createProcessInstanceQuery().subProcessInstanceId(callActivityProcessId).singleResult();
            assertThat(nestedCallActivityProcess.getPropagatedStageInstanceId()).isEqualTo(stageInstanceId);
            assertThat(nestedCallActivityProcess.getProcessDefinitionKey()).isEqualTo("nestedCallActivity");
            String nestedCallActivityProcessId = nestedCallActivityProcess.getId();
            Execution nestedCallActivityExecution = processEngine.getRuntimeService().createExecutionQuery().processInstanceId(nestedCallActivityProcessId).onlyChildExecutions().singleResult();
            assertThat(nestedCallActivityExecution.getPropagatedStageInstanceId()).isEqualTo(stageInstanceId);

            if (HistoryTestHelper
                    .isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, (ProcessEngineConfigurationImpl) processEngine.getProcessEngineConfiguration())) {

                HistoricTaskInstance historicTask = processEngineHistoryService.createHistoricTaskInstanceQuery()
                        .taskId(processTask.getId())
                        .singleResult();
                assertThat(historicTask.getPropagatedStageInstanceId()).isEqualTo(stageInstanceId);

                HistoricProcessInstance historicOneTaskProcess = processEngineHistoryService.createHistoricProcessInstanceQuery()
                        .processInstanceId(callActivityProcessId)
                        .singleResult();
                assertThat(historicOneTaskProcess.getPropagatedStageInstanceId()).isEqualTo(stageInstanceId);

                HistoricProcessInstance historicCallActivityProcess = processEngineHistoryService.createHistoricProcessInstanceQuery()
                        .processInstanceId(callActivityProcessId)
                        .singleResult();
                assertThat(historicCallActivityProcess.getPropagatedStageInstanceId()).isEqualTo(stageInstanceId);

                HistoricProcessInstance historicNestedCallActivityProcess = processEngineHistoryService.createHistoricProcessInstanceQuery()
                        .processInstanceId(callActivityProcessId)
                        .singleResult();
                assertThat(historicNestedCallActivityProcess.getPropagatedStageInstanceId()).isEqualTo(stageInstanceId);
            }
        } finally {
            processEngine.getRepositoryService().deleteDeployment(deployment.getId(), true);
        }
    }


    @Test
    @CmmnDeployment
    public void testOneTaskProcessBlocking() {
        CaseInstance caseInstance = startCaseInstanceWithOneTaskProcess();

        Task task = processEngine.getTaskService().createTaskQuery().singleResult();

        // Blocking process task, plan item should be in state ACTIVE
        List<PlanItemInstance> planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .planItemInstanceState(PlanItemInstanceState.ACTIVE)
                .list();
        assertThat(planItemInstances).hasSize(1);
        assertThat(planItemInstances.get(0).getName()).isEqualTo("The Process");
        assertThat(planItemInstances.get(0).getReferenceId()).isNotNull();
        assertThat(planItemInstances.get(0).getReferenceType()).isEqualTo(ReferenceTypes.PLAN_ITEM_CHILD_PROCESS);
        assertThat(cmmnHistoryService.createHistoricMilestoneInstanceQuery().count()).isZero();

        ProcessInstance processInstance = processEngine.getRuntimeService().createProcessInstanceQuery().singleResult();
        assertThat(processInstance).isNotNull();

        PlanItemInstance processTaskPlanItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery()
                .planItemDefinitionType(PlanItemDefinitionType.PROCESS_TASK).singleResult();
        assertThat(processInstance.getCallbackId()).isEqualTo(processTaskPlanItemInstance.getId());
        assertThat(processInstance.getCallbackType()).isEqualTo(CallbackTypes.PLAN_ITEM_CHILD_PROCESS);
        // Process Task is not in a stage, therefore there is no propagated stage instance id
        assertThat(processInstance.getPropagatedStageInstanceId()).isNull();

        assertThat(processEngine.getRuntimeService().createProcessInstanceQuery()
                .processInstanceCallbackId(processInstance.getCallbackId()).singleResult().getId()).isEqualTo(processInstance.getId());
        assertThat(processEngine.getRuntimeService().createProcessInstanceQuery()
                .processInstanceCallbackType(CallbackTypes.PLAN_ITEM_CHILD_PROCESS).singleResult().getId()).isEqualTo(processInstance.getId());
        assertThat(processEngine.getRuntimeService().createProcessInstanceQuery()
                .processInstanceCallbackId(processTaskPlanItemInstance.getId())
                .processInstanceCallbackType(CallbackTypes.PLAN_ITEM_CHILD_PROCESS).singleResult().getId()).isEqualTo(processInstance.getId());

        if (processEngine.getProcessEngineConfiguration().getHistoryLevel().isAtLeast(HistoryLevel.ACTIVITY)) {
            HistoricProcessInstance historicProcessInstance = processEngine.getHistoryService().createHistoricProcessInstanceQuery()
                    .processInstanceId(processInstance.getId()).singleResult();
            assertThat(historicProcessInstance.getCallbackId()).isEqualTo(processInstance.getCallbackId());
            assertThat(historicProcessInstance.getCallbackType()).isEqualTo(processInstance.getCallbackType());
            assertThat(historicProcessInstance.getPropagatedStageInstanceId()).isNull();

            assertThat(processEngine.getHistoryService().createHistoricProcessInstanceQuery()
                    .processInstanceCallbackId(processInstance.getCallbackId()).singleResult().getId()).isEqualTo(processInstance.getId());
            assertThat(processEngine.getHistoryService().createHistoricProcessInstanceQuery()
                    .processInstanceCallbackType(CallbackTypes.PLAN_ITEM_CHILD_PROCESS).singleResult().getId()).isEqualTo(processInstance.getId());
            assertThat(processEngine.getHistoryService().createHistoricProcessInstanceQuery()
                    .processInstanceCallbackId(processTaskPlanItemInstance.getId())
                    .processInstanceCallbackType(CallbackTypes.PLAN_ITEM_CHILD_PROCESS).singleResult().getId()).isEqualTo(processInstance.getId());
            assertThat(processEngine.getHistoryService().createHistoricProcessInstanceQuery()
                    .withoutProcessInstanceCallbackId()
                    .singleResult()).isNull();
        }

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            HistoricPlanItemInstance historicProcessTaskPlanItemInstance = cmmnHistoryService.createHistoricPlanItemInstanceQuery()
                    .planItemInstanceId(processTaskPlanItemInstance.getId())
                    .singleResult();
            assertThat(historicProcessTaskPlanItemInstance.getState()).isEqualTo(PlanItemInstanceState.ACTIVE);
            assertThat(historicProcessTaskPlanItemInstance.getReferenceId()).isEqualTo(processInstance.getId());
            assertThat(historicProcessTaskPlanItemInstance.getReferenceType()).isEqualTo(ReferenceTypes.PLAN_ITEM_CHILD_PROCESS);
        }

        // Completing task will trigger completion of process task plan item
        processEngine.getTaskService().complete(task.getId());

        planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .planItemInstanceState(PlanItemInstanceState.ACTIVE)
                .list();
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getName)
                .containsExactly("Task Two");
        assertThat(cmmnHistoryService.createHistoricMilestoneInstanceQuery().count()).isEqualTo(1);
    }

    @Test
    @CmmnDeployment(tenantId = "flowable",
            resources = "org/flowable/cmmn/test/ProcessTaskTest.testOneTaskProcessBlocking.cmmn")
    public void testOneTaskProcessBlockingWithTenant() {
        try {
            if (processEngineRepositoryService.createDeploymentQuery().count() == 1) {
                Deployment deployment = processEngineRepositoryService.createDeploymentQuery().singleResult();
                processEngineRepositoryService.deleteDeployment(deployment.getId(), true);
            }
            processEngineRepositoryService.createDeployment().
                    addClasspathResource("org/flowable/cmmn/test/oneTaskProcess.bpmn20.xml").
                    tenantId("flowable").
                    deploy();

            CaseInstance caseInstance = startCaseInstanceWithOneTaskProcess("flowable");

            ProcessInstance processInstance = processEngine.getRuntimeService().createProcessInstanceQuery().singleResult();
            assertThat(processInstance.getTenantId()).isEqualTo("flowable");

            Task task = processEngine.getTaskService().createTaskQuery().singleResult();
            assertThat(task.getTenantId()).isEqualTo("flowable");

            this.cmmnRuntimeService.terminateCaseInstance(caseInstance.getId());
        } finally {
            if (processEngineRepositoryService.createDeploymentQuery().count() == 1) {
                Deployment deployment = processEngineRepositoryService.createDeploymentQuery().singleResult();
                processEngineRepositoryService.deleteDeployment(deployment.getId(), true);
            }
            if (processEngine.getProcessEngineConfiguration().getHistoryService().createHistoricTaskInstanceQuery().count() == 1) {
                HistoricTaskInstance historicTaskInstance = processEngine.getProcessEngineConfiguration().getHistoryService().createHistoricTaskInstanceQuery()
                        .singleResult();
                processEngine.getProcessEngineConfiguration().getHistoryService().deleteHistoricTaskInstance(historicTaskInstance.getId());
            }

            if (processEngine.getHistoryService().createHistoricTaskLogEntryQuery().count() > 0) {
                List<HistoricTaskLogEntry> historicTaskLogEntries = processEngine.getHistoryService().createHistoricTaskLogEntryQuery().list();
                for (HistoricTaskLogEntry historicTaskLogEntry : historicTaskLogEntries) {
                    processEngine.getHistoryService().deleteHistoricTaskLogEntry(historicTaskLogEntry.getLogNumber());
                }
            }
        }
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/ProcessTaskTest.testOneTaskProcessBlocking.cmmn")
    public void testTwoTaskProcessBlocking() {
        try {
            if (processEngineRepositoryService.createDeploymentQuery().count() == 1) {
                Deployment deployment = processEngineRepositoryService.createDeploymentQuery().singleResult();
                processEngineRepositoryService.deleteDeployment(deployment.getId(), true);
            }
            processEngineRepositoryService.createDeployment().
                    addClasspathResource("org/flowable/cmmn/test/twoTaskProcess.bpmn20.xml").
                    deploy();

            CaseInstance caseInstance = startCaseInstanceWithOneTaskProcess();

            Task task = processEngineTaskService.createTaskQuery().singleResult();
            assertThat(task.getName()).isEqualTo("my task");

            EntityLink taskEntityLink = null;
            List<EntityLink> entityLinks = cmmnRuntimeService.getEntityLinkChildrenForCaseInstance(caseInstance.getId());
            for (EntityLink entityLink : entityLinks) {
                if (task.getId().equals(entityLink.getReferenceScopeId())) {
                    taskEntityLink = entityLink;
                }
            }

            assertThat(taskEntityLink).isNotNull();
            assertThat(taskEntityLink.getReferenceScopeId()).isEqualTo(task.getId());
            assertThat(taskEntityLink.getReferenceScopeType()).isEqualTo(ScopeTypes.TASK);
            assertThat(taskEntityLink.getHierarchyType()).isEqualTo(HierarchyType.ROOT);

            processEngineTaskService.complete(task.getId());

            Task task2 = processEngineTaskService.createTaskQuery().singleResult();
            assertThat(task2.getName()).isEqualTo("my task2");

            EntityLink taskEntityLink2 = null;
            entityLinks = cmmnRuntimeService.getEntityLinkChildrenForCaseInstance(caseInstance.getId());
            for (EntityLink entityLink : entityLinks) {
                if (task2.getId().equals(entityLink.getReferenceScopeId())) {
                    taskEntityLink2 = entityLink;
                }
            }

            assertThat(taskEntityLink2).isNotNull();
            assertThat(taskEntityLink2.getReferenceScopeId()).isEqualTo(task2.getId());
            assertThat(taskEntityLink2.getReferenceScopeType()).isEqualTo(ScopeTypes.TASK);
            assertThat(taskEntityLink2.getHierarchyType()).isEqualTo(HierarchyType.ROOT);

            processEngineTaskService.complete(task2.getId());

            List<ProcessInstance> processInstances = processEngineRuntimeService.createProcessInstanceQuery().processDefinitionKey("oneTask").list();
            assertThat(processInstances).isEmpty();

            this.cmmnRuntimeService.terminateCaseInstance(caseInstance.getId());

        } finally {
            if (processEngineRepositoryService.createDeploymentQuery().count() == 1) {
                Deployment deployment = processEngineRepositoryService.createDeploymentQuery().singleResult();
                processEngineRepositoryService.deleteDeployment(deployment.getId(), true);
            }
            if (processEngine.getProcessEngineConfiguration().getHistoryService().createHistoricTaskInstanceQuery().count() == 1) {
                HistoricTaskInstance historicTaskInstance = processEngine.getProcessEngineConfiguration().getHistoryService().createHistoricTaskInstanceQuery()
                        .singleResult();
                processEngine.getProcessEngineConfiguration().getHistoryService().deleteHistoricTaskInstance(historicTaskInstance.getId());
            }
        }
    }

    @Test
    @CmmnDeployment
    public void testProcessRefExpression() {
        Map<String, Object> variables = new HashMap<>();
        variables.put("processDefinitionKey", "oneTask");
        cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionId(cmmnRepositoryService.createCaseDefinitionQuery().singleResult().getId())
                .variables(variables)
                .start();

        Task task = processEngine.getTaskService().createTaskQuery().singleResult();
        assertThat(task).isNotNull();

        // Completing task will trigger completion of process task plan item
        processEngine.getTaskService().complete(task.getId());

        List<PlanItemInstance> planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
                .planItemInstanceStateActive()
                .list();
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getName)
                .containsExactly("Task Two");
        assertThat(cmmnHistoryService.createHistoricMilestoneInstanceQuery().count()).isEqualTo(1);
    }

    @Test
    @CmmnDeployment
    public void testProcessIOParameter() {
        Map<String, Object> variables = new HashMap<>();
        variables.put("processDefinitionKey", "oneTask");
        variables.put("num2", 123);
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionId(cmmnRepositoryService.createCaseDefinitionQuery().singleResult().getId())
                .variables(variables)
                .start();

        Task task = processEngine.getTaskService().createTaskQuery().singleResult();
        assertThat(task).isNotNull();

        // Completing task will trigger completion of process task plan item
        processEngine.getTaskService().complete(task.getId());

        List<PlanItemInstance> planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
                .planItemInstanceStateActive()
                .list();

        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getName)
                .containsExactly("Task Two");
        assertThat(cmmnRuntimeService.getVariable(caseInstance.getId(), "num3")).isEqualTo(123);
        assertThat(cmmnHistoryService.createHistoricMilestoneInstanceQuery().count()).isEqualTo(1);

    }

    @Test
    @CmmnDeployment
    public void testProcessIOParameterExpressions() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionId(cmmnRepositoryService.createCaseDefinitionQuery().singleResult().getId())
                .variable("processDefinitionKey", "oneTask")
                .start();

        Task task = processEngine.getTaskService().createTaskQuery().singleResult();
        assertThat(task).isNotNull();

        // Completing task will trigger completion of process task plan item
        assertThat(((Number) processEngine.getRuntimeService().getVariable(task.getProcessInstanceId(), "numberVariable")).longValue()).isEqualTo(2);
        processEngine.getTaskService().complete(task.getId(), Collections.singletonMap("processVariable", "Hello World"));

        assertThat(cmmnRuntimeService.getVariable(caseInstance.getId(), "stringVariable")).isEqualTo("Hello World");

    }

    @Test
    @CmmnDeployment
    public void testIOParameterCombinations() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("testProcessTaskParameterExpressions")
                .variable("caseVariableA", "variable A from the case instance")
                .variable("caseVariableName1", "aCaseString1")
                .variable("caseVariableName2", "aCaseString2")
                .start();

        PlanItemInstance processTaskPlanItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .planItemDefinitionType(PlanItemDefinitionType.PROCESS_TASK)
                .singleResult();
        String processInstanceId = processTaskPlanItemInstance.getReferenceId();
        ProcessInstance processInstance = processEngineRuntimeService.createProcessInstanceQuery().processInstanceId(processInstanceId).singleResult();
        assertThat(processInstance).isNotNull();

        // In parameters
        assertThat(processEngineRuntimeService.getVariable(processInstanceId, "caseVariableA")).isNull();
        assertThat(processEngineRuntimeService.getVariable(processInstanceId, "caseVariableName1")).isNull();
        assertThat(processEngineRuntimeService.getVariable(processInstanceId, "caseVariableName2")).isNull();

        assertThat(processEngineRuntimeService.getVariable(processInstanceId, "processVariableA")).isEqualTo("variable A from the case instance");
        assertThat(processEngineRuntimeService.getVariable(processInstanceId, "aCaseString1")).isEqualTo("variable A from the case instance");
        assertThat(processEngineRuntimeService.getVariable(processInstanceId, "processVariableB")).isEqualTo(2L);
        assertThat(processEngineRuntimeService.getVariable(processInstanceId, "aCaseString2")).isEqualTo(4L);

        // Out parameters
        Task task = processEngineTaskService.createTaskQuery().processInstanceId(processInstanceId).singleResult();
        Map<String, Object> variables = new HashMap<>();
        variables.put("processVariableC", "hello");
        variables.put("processVariableD", 123);
        variables.put("processVariableName1", "processString1");
        variables.put("processVariableName2", "processString2");
        processEngineTaskService.complete(task.getId(), variables);

        assertThat(cmmnRuntimeService.getVariable(caseInstance.getId(), "processVariableC")).isNull();
        assertThat(cmmnRuntimeService.getVariable(caseInstance.getId(), "processVariableD")).isNull();
        assertThat(cmmnRuntimeService.getVariable(caseInstance.getId(), "processVariableName1")).isNull();
        assertThat(cmmnRuntimeService.getVariable(caseInstance.getId(), "processVariableName2")).isNull();

        assertThat(cmmnRuntimeService.getVariable(caseInstance.getId(), "caseVariableC")).isEqualTo("hello");
        assertThat(cmmnRuntimeService.getVariable(caseInstance.getId(), "processString1")).isEqualTo("hello");
        assertThat(cmmnRuntimeService.getVariable(caseInstance.getId(), "caseVariableD")).isEqualTo(124L);
        assertThat(cmmnRuntimeService.getVariable(caseInstance.getId(), "processString2")).isEqualTo(6L);

        assertThat(cmmnRuntimeService.getVariables(caseInstance.getId())).hasSize(3 + 4); // 3 from start, 4 from out mapping
        assertThat(processEngineHistoryService.createHistoricVariableInstanceQuery().processInstanceId(processInstanceId).list())
                .hasSize(4 + 4); // 4 from in mapping, 4 from task complete
    }

    @Test
    @CmmnDeployment
    public void testProcessTaskWithSkipExpressions() {
        processEngineRepositoryService.createDeployment().addClasspathResource("org/flowable/cmmn/test/processWithSkipExpressions.bpmn20.xml").deploy();

        ProcessDefinition processDefinition = processEngineRepositoryService.createProcessDefinitionQuery().processDefinitionKey("testSkipExpressionProcess")
                .singleResult();
        ObjectNode infoNode = processEngineDynamicBpmnService.enableSkipExpression();

        // skip test user task
        processEngineDynamicBpmnService.changeSkipExpression("sequenceflow2", "${true}", infoNode);
        processEngineDynamicBpmnService.saveProcessDefinitionInfo(processDefinition.getId(), infoNode);

        cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("skipExpressionCaseTest").start();

        assertThat(processEngineTaskService.createTaskQuery().list()).isEmpty();
        assertThat(processEngineRuntimeService.createProcessInstanceQuery().list()).isEmpty();
    }

    @Test
    @CmmnDeployment
    public void testProcessTaskWithInclusiveGateway() {
        Deployment deployment = processEngineRepositoryService.createDeployment()
                .addClasspathResource("org/flowable/cmmn/test/processWithInclusiveGateway.bpmn20.xml").deploy();
        try {
            CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("myCase").start();
            assertThat(cmmnHistoryService.createHistoricMilestoneInstanceQuery().count()).isZero();
            assertThat(processEngineRuntimeService.createProcessInstanceQuery().count()).isZero();

            List<PlanItemInstance> planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
                    .caseInstanceId(caseInstance.getId())
                    .planItemDefinitionId("theTask")
                    .planItemInstanceState(PlanItemInstanceState.ACTIVE)
                    .list();
            assertThat(planItemInstances).hasSize(1);
            cmmnRuntimeService.triggerPlanItemInstance(planItemInstances.get(0).getId());
            assertThat(processEngineRuntimeService.createProcessInstanceQuery().count()).as("No process instance started").isEqualTo(1);

            assertThat(processEngineTaskService.createTaskQuery().count()).isEqualTo(2);

            List<Task> tasks = processEngineTaskService.createTaskQuery().list();
            processEngine.getTaskService().complete(tasks.get(0).getId());
            processEngine.getTaskService().complete(tasks.get(1).getId());

            assertThat(processEngineTaskService.createTaskQuery().count()).isZero();
            assertThat(processEngineRuntimeService.createProcessInstanceQuery().count()).isZero();

            planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
                    .caseInstanceId(caseInstance.getId())
                    .planItemDefinitionId("theTask2")
                    .list();
            assertThat(planItemInstances)
                    .extracting(PlanItemInstance::getName, PlanItemInstance::getState)
                    .containsExactly(tuple("Task Two", PlanItemInstanceState.ENABLED));

        } finally {
            processEngineRepositoryService.deleteDeployment(deployment.getId(), true);
        }
    }
    
    @Test
    @CmmnDeployment
    public void testOneTaskProcessWithListener() {
        Deployment deployment = processEngineRepositoryService.createDeployment()
                .addClasspathResource("org/flowable/cmmn/test/oneTaskProcessWithListener.bpmn20.xml").deploy();
        try {
            CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("myCase").start();
            assertThat(processEngineRuntimeService.createProcessInstanceQuery().count()).isZero();

            List<PlanItemInstance> planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
                    .caseInstanceId(caseInstance.getId())
                    .planItemDefinitionId("theTask")
                    .planItemInstanceState(PlanItemInstanceState.ACTIVE)
                    .list();
            assertThat(planItemInstances).hasSize(1);
            cmmnRuntimeService.triggerPlanItemInstance(planItemInstances.get(0).getId());
            assertThat(processEngineRuntimeService.createProcessInstanceQuery().count()).as("No process instance started").isEqualTo(1);

            ProcessInstance processInstance = processEngineRuntimeService.createProcessInstanceQuery().processDefinitionKey("oneTask").singleResult();
            assertThat(processInstance).isNotNull();
            
            assertThat(processEngineTaskService.createTaskQuery().processInstanceId(processInstance.getId()).count()).isEqualTo(1);

            assertThatThrownBy(() -> {
                cmmnRuntimeService.terminateCaseInstance(caseInstance.getId());
            }).isInstanceOf(FlowableException.class);
            
            cmmnRuntimeService.deleteCaseInstance(caseInstance.getId());
            
            assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceId(caseInstance.getId()).list().size()).isZero();
            assertThat(processEngineRuntimeService.createProcessInstanceQuery().processInstanceId(processInstance.getId()).list().size()).isZero();
            
            if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
                assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceId(caseInstance.getId()).count()).isEqualTo(1);
                HistoricCaseInstance historicCaseInstance = cmmnHistoryService.createHistoricCaseInstanceQuery()
                        .caseInstanceId(caseInstance.getId())
                        .singleResult();
                assertThat(historicCaseInstance.getEndTime()).isNotNull();
                assertThat(historicCaseInstance.getState()).isEqualTo(CaseInstanceState.TERMINATED);
                
                assertThat(processEngineHistoryService.createHistoricProcessInstanceQuery().processInstanceId(processInstance.getId()).count()).isEqualTo(1);
                HistoricProcessInstance historicProcessInstance = processEngineHistoryService.createHistoricProcessInstanceQuery()
                        .processInstanceId(processInstance.getId())
                        .singleResult();
                assertThat(historicProcessInstance.getEndTime()).isNotNull();
            }

        } finally {
            processEngineRepositoryService.deleteDeployment(deployment.getId(), true);
        }
    }

    protected CaseInstance startCaseInstanceWithOneTaskProcess() {
        return startCaseInstanceWithOneTaskProcess(null);
    }

    protected CaseInstance startCaseInstanceWithOneTaskProcess(String tenantId) {
        CaseDefinitionQuery caseDefinitionQuery = cmmnRepositoryService.createCaseDefinitionQuery();
        if (tenantId != null) {
            caseDefinitionQuery.caseDefinitionTenantId(tenantId);
        }

        String caseDefinitionId = caseDefinitionQuery.singleResult().getId();
        CaseInstanceBuilder caseInstanceBuilder = cmmnRuntimeService.createCaseInstanceBuilder().
                caseDefinitionId(caseDefinitionId);
        if (tenantId != null) {
            caseInstanceBuilder.tenantId(tenantId);
        }

        CaseInstance caseInstance = caseInstanceBuilder.start();

        assertThat(cmmnHistoryService.createHistoricMilestoneInstanceQuery().count()).isZero();
        assertThat(processEngineRuntimeService.createProcessInstanceQuery().count()).isZero();

        List<PlanItemInstance> planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .planItemInstanceState(PlanItemInstanceState.ACTIVE)
                .list();
        assertThat(planItemInstances).hasSize(1);
        cmmnRuntimeService.triggerPlanItemInstance(planItemInstances.get(0).getId());
        assertThat(processEngineRuntimeService.createProcessInstanceQuery().count()).as("No process instance started").isEqualTo(1);
        return caseInstance;
    }

    @Test
    @CmmnDeployment
    public void testTransactionRollback() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionId(cmmnRepositoryService.createCaseDefinitionQuery().singleResult().getId()).start();

        final PlanItemInstance planItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .planItemInstanceState(PlanItemInstanceState.ACTIVE)
                .singleResult();
        assertThat(planItemInstance.getName()).isEqualTo("Task One");
        assertThat(processEngineRuntimeService.createProcessInstanceQuery().count()).isZero();

        /*
         * Triggering the plan item will lead to the plan item that starts the one task process in a non-blocking way.
         * Due to the non-blocking, the plan item completes and the new task, mile stone and service task are called.
         * The service task throws an exception. The process should also roll back now and never have been inserted.
         */
        assertThatThrownBy(() -> cmmnRuntimeService.triggerPlanItemInstance(planItemInstance.getId()))
                .isInstanceOf(RuntimeException.class);

        // Without shared transaction, following would be 1
        assertThat(processEngineRuntimeService.createProcessInstanceQuery().count()).isZero();

        PlanItemInstance planItemInstance2 = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .planItemInstanceState(PlanItemInstanceState.ACTIVE)
                .singleResult();

        // Both case and process should have rolled back
        assertThat(planItemInstance2.getName()).isEqualTo("Task One");
    }

    @Test
    @CmmnDeployment
    public void testTriggerUnfinishedProcessPlanItem() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("myCase").start();
        PlanItemInstance planItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .planItemInstanceState(PlanItemInstanceState.ACTIVE)
                .singleResult();
        assertThat(planItemInstance.getName()).isEqualTo("The Process");
        assertThat(processEngine.getTaskService().createTaskQuery().singleResult().getName()).isEqualTo("my task");

        // Triggering the process plan item should cancel the process instance
        cmmnRuntimeService.triggerPlanItemInstance(planItemInstance.getId());
        assertThat(processEngine.getTaskService().createTaskQuery().count()).isZero();
        assertThat(processEngineRuntimeService.createProcessInstanceQuery().count()).isZero();

        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().count()).isZero();
        HistoricMilestoneInstance historicMilestoneInstance = cmmnHistoryService.createHistoricMilestoneInstanceQuery().singleResult();
        assertThat(historicMilestoneInstance.getName()).isEqualTo("Process planitem done");
        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().finished().count()).isEqualTo(1);
    }

    @Test
    @CmmnDeployment
    public void testStartProcessInstanceNonBlockingAndCaseInstanceFinished() {
        cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("myCase").start();

        assertThat(processEngine.getTaskService().createTaskQuery().count()).isEqualTo(1);
        assertThat(processEngineRuntimeService.createProcessInstanceQuery().count()).isEqualTo(1);

        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().count()).isZero();
        HistoricMilestoneInstance historicMilestoneInstance = cmmnHistoryService.createHistoricMilestoneInstanceQuery().singleResult();
        assertThat(historicMilestoneInstance.getName()).isEqualTo("Process planitem done");
        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().finished().count()).isEqualTo(1);
    }

    @Test
    @CmmnDeployment
    public void testStartMultipleProcessInstancesBlocking() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("myCase").start();

        PlanItemInstance planItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .planItemInstanceState(PlanItemInstanceState.ACTIVE)
                .singleResult();
        assertThat(planItemInstance.getName()).isEqualTo("Task One");
        cmmnRuntimeService.triggerPlanItemInstance(planItemInstance.getId());

        assertThat(processEngine.getTaskService().createTaskQuery().count()).isEqualTo(4);
        assertThat(processEngineRuntimeService.createProcessInstanceQuery().count()).isEqualTo(4);

        // Completing all the tasks should lead to the milestone
        for (Task task : processEngineTaskService.createTaskQuery().list()) {
            processEngineTaskService.complete(task.getId());
        }

        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().count()).isZero();
        HistoricMilestoneInstance historicMilestoneInstance = cmmnHistoryService.createHistoricMilestoneInstanceQuery().singleResult();
        assertThat(historicMilestoneInstance.getName()).isEqualTo("Processes done");
        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().finished().count()).isEqualTo(1);
    }

    @Test
    @CmmnDeployment
    public void testTerminateCaseInstanceWithBlockingProcessTask() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("myCase").start();
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).count()).isEqualTo(8);
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId())
                .planItemInstanceState(PlanItemInstanceState.ACTIVE).count()).isEqualTo(3);

        PlanItemInstance planItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .planItemInstanceState(PlanItemInstanceState.ACTIVE)
                .planItemInstanceName("Task One")
                .singleResult();
        assertThat(planItemInstance).isNotNull();

        cmmnRuntimeService.triggerPlanItemInstance(planItemInstance.getId());

        assertThat(processEngine.getTaskService().createTaskQuery().count()).isEqualTo(4);
        assertThat(processEngineRuntimeService.createProcessInstanceQuery().count()).isEqualTo(4);

        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().finished().count()).isZero();
        cmmnRuntimeService.terminateCaseInstance(caseInstance.getId());

        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().count()).isZero();
        assertThat(processEngine.getTaskService().createTaskQuery().count()).isZero();
        assertThat(processEngineRuntimeService.createProcessInstanceQuery().count()).isZero();
        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().finished().count()).isEqualTo(1);
    }

    @Test
    @CmmnDeployment(resources = {
            "org/flowable/cmmn/test/ProcessTaskTest.testParentStageTerminatedBeforeProcessStarted.cmmn",
            "org/flowable/cmmn/test/oneTaskProcess.bpmn20.xml"
    })
    public void testParentStageTerminatedBeforeProcessStarted() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testProcessTask").start();

        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertThat(task.getName()).isEqualTo("A");

        UserEventListenerInstance userEventListenerInstance = cmmnRuntimeService.createUserEventListenerInstanceQuery().caseInstanceId(caseInstance.getId())
                .singleResult();
        assertThat(userEventListenerInstance.getName()).isEqualTo("Complete stage");
        cmmnRuntimeService.completeUserEventListenerInstance(userEventListenerInstance.getId());

        assertThat(cmmnRuntimeService.createCaseInstanceQuery().count()).isZero();
    }

    @Test
    @CmmnDeployment(
            resources = { "org/flowable/cmmn/test/ProcessTaskTest.testOneTaskProcessFallbackToDefaultTenant.cmmn" },
            tenantId = "flowable"
    )
    public void testOneTaskProcessFallbackToDefaultTenant() {
        Deployment deployment = this.processEngineRepositoryService.createDeployment().
                addClasspathResource("org/flowable/cmmn/test/oneTaskProcess.bpmn20.xml").
                deploy();
        try {
            CaseInstance caseInstance = startCaseInstanceWithOneTaskProcess("flowable");
            List<Task> processTasks = processEngine.getTaskService().createTaskQuery().list();
            assertThat(processTasks).hasSize(1);
            assertThat(processTasks.get(0).getTenantId()).isEqualTo("flowable");
            ProcessInstance processInstance = processEngine.getRuntimeService().createProcessInstanceQuery()
                    .processInstanceId(processTasks.get(0).getProcessInstanceId()).singleResult();
            assertThat(processInstance).isNotNull();
            assertThat(processInstance.getTenantId()).isEqualTo("flowable");

            // Non-blocking process task, plan item should have been completed
            List<PlanItemInstance> planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
                    .caseInstanceId(caseInstance.getId())
                    .planItemInstanceState(PlanItemInstanceState.ACTIVE)
                    .list();
            assertThat(planItemInstances)
                    .extracting(PlanItemInstance::getName)
                    .containsExactly("Task Two");

            assertThat(cmmnHistoryService.createHistoricMilestoneInstanceQuery().count()).isEqualTo(1);

            processEngine.getTaskService().complete(processTasks.get(0).getId());
            assertThat(processEngineRuntimeService.createProcessInstanceQuery().count()).isZero();
        } finally {
            processEngineRepositoryService.deleteDeployment(deployment.getId(), true);
        }
    }

    @Test
    @CmmnDeployment(
            resources = { "org/flowable/cmmn/test/ProcessTaskTest.testOneTaskProcessNonBlocking.cmmn" },
            tenantId = "someTenant"
    )
    public void testOneTaskProcessGlobalFallbackToDefaultTenant() {
        Deployment deployment = this.processEngineRepositoryService.createDeployment().
                addClasspathResource("org/flowable/cmmn/test/oneTaskProcess.bpmn20.xml").
                tenantId("defaultFlowable").
                deploy();

        DefaultTenantProvider originalDefaultTenantProvider = this.processEngineConfiguration.getDefaultTenantProvider();
        this.processEngineConfiguration.setFallbackToDefaultTenant(true);
        this.processEngineConfiguration.setDefaultTenantValue("defaultFlowable");

        try {
            CaseInstance caseInstance = startCaseInstanceWithOneTaskProcess("someTenant");
            List<Task> processTasks = processEngine.getTaskService().createTaskQuery().list();
            assertThat(processTasks)
                    .extracting(Task::getTenantId)
                    .containsExactly("someTenant");
            ProcessInstance processInstance = processEngine.getRuntimeService().createProcessInstanceQuery()
                    .processInstanceId(processTasks.get(0).getProcessInstanceId()).singleResult();
            assertThat(processInstance).isNotNull();
            assertThat(processInstance.getTenantId()).isEqualTo("someTenant");

            // Non-blocking process task, plan item should have been completed
            List<PlanItemInstance> planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
                    .caseInstanceId(caseInstance.getId())
                    .planItemInstanceState(PlanItemInstanceState.ACTIVE)
                    .list();
            assertThat(planItemInstances)
                    .extracting(PlanItemInstance::getName)
                    .containsExactly("Task Two");

            assertThat(cmmnHistoryService.createHistoricMilestoneInstanceQuery().count()).isEqualTo(1);

            processEngine.getTaskService().complete(processTasks.get(0).getId());
            assertThat(processEngineRuntimeService.createProcessInstanceQuery().count()).isZero();

        } finally {
            this.processEngineConfiguration.setFallbackToDefaultTenant(false);
            this.processEngineConfiguration.setDefaultTenantProvider(originalDefaultTenantProvider);
            processEngineRepositoryService.deleteDeployment(deployment.getId(), true);
        }
    }

    @Test
    @CmmnDeployment(
            resources = { "org/flowable/cmmn/test/ProcessTaskTest.testOneTaskProcessNonBlocking.cmmn" },
            tenantId = "someTenant"
    )
    public void testOneTaskProcessGlobalFallbackToDefaultTenantNoDefinition() {
        Deployment deployment = this.processEngineRepositoryService.createDeployment().
                addClasspathResource("org/flowable/cmmn/test/oneTaskProcess.bpmn20.xml").
                tenantId("tenant1").
                deploy();

        DefaultTenantProvider originalDefaultTenantProvider = this.processEngineConfiguration.getDefaultTenantProvider();
        this.processEngineConfiguration.setFallbackToDefaultTenant(true);
        this.processEngineConfiguration.setDefaultTenantValue("defaultFlowable");

        assertThatThrownBy(() -> startCaseInstanceWithOneTaskProcess("someTenant"))
                .isInstanceOf(FlowableObjectNotFoundException.class);

        this.processEngineConfiguration.setFallbackToDefaultTenant(false);
        this.processEngineConfiguration.setDefaultTenantProvider(originalDefaultTenantProvider);
        processEngineRepositoryService.deleteDeployment(deployment.getId(), true);
    }

    @Test
    @CmmnDeployment(
            resources = { "org/flowable/cmmn/test/ProcessTaskTest.testOneTaskProcessFallbackToDefaultTenantFalse.cmmn" },
            tenantId = "flowable"
    )
    public void testOneTaskProcessFallbackToDefaultTenantFalse() {
        Deployment deployment = this.processEngineRepositoryService.createDeployment().
                addClasspathResource("org/flowable/cmmn/test/oneTaskProcess.bpmn20.xml").
                deploy();

        assertThatThrownBy(() -> startCaseInstanceWithOneTaskProcess("flowable"))
                .isInstanceOf(FlowableObjectNotFoundException.class)
                .hasMessage("Process definition with key 'oneTask' and tenantId 'flowable' was not found");

        processEngineRepositoryService.deleteDeployment(deployment.getId(), true);
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/ProcessTaskTest.testOneTaskProcessBlocking.cmmn")
    public void testDeleteProcessTaskShouldNotBePossible() {
        startCaseInstanceWithOneTaskProcess();

        Task task = processEngine.getTaskService().createTaskQuery().singleResult();

        assertThatThrownBy(() -> cmmnTaskService.deleteTask(task.getId()))
                .isExactlyInstanceOf(FlowableException.class)
                .hasMessageContaining("The task cannot be deleted")
                .hasMessageContaining("running process");
    }

    @Test
    @CmmnDeployment
    public void testChangeActiveStagesWithProcessTasks() {

        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("activatePlanItemTest").start();

        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).list())
                .extracting(PlanItemInstance::getPlanItemDefinitionId, PlanItemInstance::getState)
                .containsExactlyInAnyOrder(
                        tuple("oneexpandedstage2", "available"),
                        tuple("oneexpandedstage1", "available"),
                        tuple("oneprocesstask1", "active")
                );

        Task task = processEngineTaskService.createTaskQuery().taskDefinitionKey("theTask").singleResult();
        processEngineTaskService.complete(task.getId());

        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).list())
                .extracting(PlanItemInstance::getPlanItemDefinitionId, PlanItemInstance::getState)
                .containsExactlyInAnyOrder(
                        tuple("oneexpandedstage1", "active"),
                        tuple("oneexpandedstage2", "available"),
                        tuple("oneexpandedstage1", "wait_repetition"),
                        tuple("oneprocesstask2", "active"),
                        tuple("oneprocesstask4", "active")
                );

        cmmnRuntimeService.createChangePlanItemStateBuilder()
                .caseInstanceId(caseInstance.getId())
                .terminatePlanItemDefinitionId("oneexpandedstage1")
                .activatePlanItemDefinitionId("oneprocesstask3")
                .changeState();

        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).list())
                .extracting(PlanItemInstance::getPlanItemDefinitionId, PlanItemInstance::getState)
                .containsExactlyInAnyOrder(
                        tuple("oneexpandedstage1", "wait_repetition"),
                        tuple("oneexpandedstage2", "active"),
                        tuple("oneexpandedstage2", "wait_repetition"),
                        tuple("oneprocesstask3", "active")
                );

        assertThat(processEngineRuntimeService.createProcessInstanceQuery().processDefinitionKey("oneTask").count()).isEqualTo(1);
        task = processEngineTaskService.createTaskQuery().taskDefinitionKey("theTask").singleResult();
        processEngineTaskService.complete(task.getId());

        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).list())
                .extracting(PlanItemInstance::getPlanItemDefinitionId, PlanItemInstance::getState)
                .containsExactlyInAnyOrder(
                        tuple("oneexpandedstage1", "active"),
                        tuple("oneexpandedstage1", "wait_repetition"),
                        tuple("oneexpandedstage2", "wait_repetition"),
                        tuple("oneprocesstask2", "active"),
                        tuple("oneprocesstask4", "active")
                );

        assertThat(processEngineRuntimeService.createProcessInstanceQuery().processDefinitionKey("oneTask").count()).isEqualTo(2);
    }

    @Test
    @CmmnDeployment
    public void testChangeActiveStagesWithManualProcessTasks() {

        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("activatePlanItemTest").start();

        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).list())
                .extracting(PlanItemInstance::getPlanItemDefinitionId, PlanItemInstance::getState)
                .containsExactlyInAnyOrder(
                        tuple("oneexpandedstage2", "available"),
                        tuple("oneexpandedstage1", "available"),
                        tuple("oneprocesstask1", "active")
                );

        Task task = processEngineTaskService.createTaskQuery().taskDefinitionKey("theTask").singleResult();
        processEngineTaskService.complete(task.getId());

        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).list())
                .extracting(PlanItemInstance::getPlanItemDefinitionId, PlanItemInstance::getState)
                .containsExactlyInAnyOrder(
                        tuple("oneexpandedstage1", "active"),
                        tuple("oneexpandedstage2", "available"),
                        tuple("oneexpandedstage1", "wait_repetition"),
                        tuple("oneprocesstask2", "enabled"),
                        tuple("oneprocesstask4", "enabled")
                );

        cmmnRuntimeService.createChangePlanItemStateBuilder()
                .caseInstanceId(caseInstance.getId())
                .terminatePlanItemDefinitionId("oneexpandedstage1")
                .activatePlanItemDefinitionId("oneprocesstask3")
                .changeState();

        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).list())
                .extracting(PlanItemInstance::getPlanItemDefinitionId, PlanItemInstance::getState)
                .containsExactlyInAnyOrder(
                        tuple("oneexpandedstage1", "wait_repetition"),
                        tuple("oneexpandedstage2", "active"),
                        tuple("oneexpandedstage2", "wait_repetition"),
                        tuple("oneprocesstask3", "active")
                );

        assertThat(processEngineRuntimeService.createProcessInstanceQuery().processDefinitionKey("oneTask").count()).isEqualTo(1);
        task = processEngineTaskService.createTaskQuery().taskDefinitionKey("theTask").singleResult();
        processEngineTaskService.complete(task.getId());

        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).list())
                .extracting(PlanItemInstance::getPlanItemDefinitionId, PlanItemInstance::getState)
                .containsExactlyInAnyOrder(
                        tuple("oneexpandedstage1", "active"),
                        tuple("oneexpandedstage1", "wait_repetition"),
                        tuple("oneexpandedstage2", "wait_repetition"),
                        tuple("oneprocesstask2", "enabled"),
                        tuple("oneprocesstask4", "enabled")
                );

        assertThat(processEngineRuntimeService.createProcessInstanceQuery().processDefinitionKey("oneTask").count()).isZero();
    }

    @Test
    @CmmnDeployment
    public void testChangeActiveStages() {

        Deployment deployment = this.processEngineRepositoryService.createDeployment().
                addClasspathResource("org/flowable/cmmn/test/emptyProcess.bpmn20.xml").
                deploy();

        try {
            CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("activatePlanItemTest").start();

            assertThat(processEngineHistoryService.createHistoricProcessInstanceQuery().processDefinitionKey("oneTask").count()).isEqualTo(1);
            assertThat(processEngineHistoryService.createHistoricProcessInstanceQuery().processDefinitionKey("oneTask").finished().count()).isZero();

            assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).list())
                    .extracting(PlanItemInstance::getPlanItemDefinitionId, PlanItemInstance::getState)
                    .containsExactlyInAnyOrder(
                            tuple("oneprocesstask1", "active"),
                            tuple("oneexpandedstage1", "available"),
                            tuple("oneexpandedstage2", "available")
                    );

            Task task = processEngineTaskService.createTaskQuery().processDefinitionKey("oneTask").singleResult();
            processEngineTaskService.complete(task.getId());

            assertThat(processEngineHistoryService.createHistoricProcessInstanceQuery().processDefinitionKey("oneTask").finished().count()).isEqualTo(1);

            assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).list())
                    .extracting(PlanItemInstance::getPlanItemDefinitionId, PlanItemInstance::getState)
                    .containsExactlyInAnyOrder(
                            tuple("oneexpandedstage1", "active"),
                            tuple("oneexpandedstage2", "available"),
                            tuple("oneexpandedstage1", "wait_repetition"),
                            tuple("oneprocesstask2", "enabled"),
                            tuple("oneprocesstask4", "enabled")
                    );

            cmmnRuntimeService.createChangePlanItemStateBuilder()
                    .caseInstanceId(caseInstance.getId())
                    .terminatePlanItemDefinitionId("oneexpandedstage1")
                    .activatePlanItemDefinitionId("oneprocesstask3")
                    .changeState();

            assertThat(processEngineHistoryService.createHistoricProcessInstanceQuery().processDefinitionKey("oneTask").count()).isEqualTo(2);
            assertThat(processEngineHistoryService.createHistoricProcessInstanceQuery().processDefinitionKey("oneTask").finished().count()).isEqualTo(1);

            task = processEngineTaskService.createTaskQuery().processDefinitionKey("oneTask").singleResult();
            processEngineTaskService.complete(task.getId());

            assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).list())
                    .extracting(PlanItemInstance::getPlanItemDefinitionId, PlanItemInstance::getState)
                    .containsExactlyInAnyOrder(
                            tuple("oneexpandedstage1", "active"),
                            tuple("oneexpandedstage1", "wait_repetition"),
                            tuple("oneexpandedstage2", "wait_repetition"),
                            tuple("oneprocesstask2", "enabled"),
                            tuple("oneprocesstask4", "enabled")
                    );

            PlanItemInstance deactivateInstance = cmmnRuntimeService.createPlanItemInstanceQuery()
                    .planItemDefinitionId("oneprocesstask4")
                    .planItemInstanceState("enabled")
                    .singleResult();
            cmmnRuntimeService.startPlanItemInstance(deactivateInstance.getId());

            assertThat(processEngineHistoryService.createHistoricProcessInstanceQuery().processDefinitionKey("oneTask").finished().count()).isEqualTo(2);
            assertThat(processEngineHistoryService.createHistoricProcessInstanceQuery().processDefinitionKey("emptyProcess").finished().count()).isEqualTo(1);

            assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).list())
                    .extracting(PlanItemInstance::getPlanItemDefinitionId, PlanItemInstance::getState)
                    .containsExactlyInAnyOrder(
                            tuple("oneexpandedstage1", "wait_repetition"),
                            tuple("oneexpandedstage2", "active"),
                            tuple("oneexpandedstage2", "wait_repetition"),
                            tuple("oneprocesstask3", "enabled")
                    );

        } finally {
            processEngineRepositoryService.deleteDeployment(deployment.getId(), true);
        }
    }

    @Test
    @CmmnDeployment
    public void testPassChildTaskVariables() {
        assertThat(processEngineRuntimeService.createProcessInstanceQuery().count()).isZero();

        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("startChildProcess").start();
        PlanItemInstance processPlanItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .planItemInstanceStateEnabled()
                .singleResult();

        cmmnRuntimeService.createPlanItemInstanceTransitionBuilder(processPlanItemInstance.getId())
                .variable("caseVar", "caseValue")
                .childTaskVariable("processVar1", "processValue")
                .childTaskVariables(CollectionUtil.map("processVar2", 123, "processVar3", 456))
                .start();

        Map<String, Object> variables = cmmnRuntimeService.getVariables(caseInstance.getId());
        // also includes initiator variable
        assertThat(variables)
                .hasSize(2)
                .containsEntry("caseVar", "caseValue");

        ProcessInstance processInstance = processEngineRuntimeService.createProcessInstanceQuery()
                .processInstanceCallbackId(processPlanItemInstance.getId())
                .singleResult();
        Map<String, Object> processInstanceVariables = processEngineRuntimeService.getVariables(processInstance.getId());
        assertThat(processInstanceVariables)
                .containsOnly(
                        entry("processVar1", "processValue"),
                        entry("processVar2", 123),
                        entry("processVar3", 456)
                );
    }

    @Test
    @CmmnDeployment
    public void testExitAvailableProcessTaskThroughExitSentryOnStage() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("testExitAvailableProcessTaskThroughExitSentryOnStage")
                .start();

        PlanItemInstance planItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery()
                .planItemInstanceStateAvailable()
                .planItemDefinitionType(PlanItemDefinitionType.PROCESS_TASK)
                .singleResult();
        assertThat(planItemInstance.getName()).isEqualTo("theProcess");

        assertThat(cmmnTaskService.createTaskQuery().taskName("task2").singleResult()).isNull();

        // When the event listener now occurs, the stage should be exited, also exiting the process task plan item
        UserEventListenerInstance userEventListenerInstance = cmmnRuntimeService.createUserEventListenerInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .singleResult();
        cmmnRuntimeService.completeUserEventListenerInstance(userEventListenerInstance.getId());

        assertThat(cmmnTaskService.createTaskQuery().taskName("task2").singleResult()).isNotNull();
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceStateAvailable().planItemDefinitionType(PlanItemDefinitionType.PROCESS_TASK)
                .singleResult()).isNull();
    }

    @Test
    @CmmnDeployment
    public void testExitActiveProcessTaskThroughExitSentryOnStage() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("testExitActiveProcessTaskThroughExitSentryOnStage")
                .variable("myVar", "test")
                .start();

        PlanItemInstance planItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery()
                .planItemInstanceStateActive()
                .planItemDefinitionType(PlanItemDefinitionType.PROCESS_TASK)
                .singleResult();
        assertThat(planItemInstance.getName()).isEqualTo("theProcess");

        assertThat(cmmnTaskService.createTaskQuery().taskName("task2").singleResult()).isNull();
        assertThat(processEngineRuntimeService.createProcessInstanceQuery().processInstanceId(planItemInstance.getReferenceId()).singleResult()).isNotNull();

        // When the event listener now occurs, the stage should be exited, also exiting the process task plan item
        UserEventListenerInstance userEventListenerInstance = cmmnRuntimeService.createUserEventListenerInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .singleResult();
        cmmnRuntimeService.completeUserEventListenerInstance(userEventListenerInstance.getId());

        assertThat(cmmnTaskService.createTaskQuery().taskName("task2").singleResult()).isNotNull();
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceStateAvailable().planItemDefinitionType(PlanItemDefinitionType.PROCESS_TASK)
                .singleResult()).isNull();
        assertThat(processEngineRuntimeService.createProcessInstanceQuery().processInstanceId(planItemInstance.getReferenceId()).singleResult()).isNull();
    }

    @Test
    @CmmnDeployment
    public void testExitCaseInstanceOnProcessInstanceComplete() {
        cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("testExitCaseInstanceOnProcessInstanceComplete")
                .start();

        assertThat(processEngineRuntimeService.createProcessInstanceQuery().count()).isZero();
        assertThat(cmmnTaskService.createTaskQuery().count()).isEqualTo(1);

        // Process task is manually activated
        cmmnRuntimeService.startPlanItemInstance(cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceStateEnabled().singleResult().getId());
        assertThat(processEngineRuntimeService.createProcessInstanceQuery().count()).isEqualTo(1);
        assertThat(cmmnTaskService.createTaskQuery().count()).isEqualTo(2);

        // Completing the task from the process should terminate the case
        Task userTaskFromProcess = processEngineTaskService.createTaskQuery()
                .processInstanceId(processEngineRuntimeService.createProcessInstanceQuery().singleResult().getId())
                .singleResult();
        processEngineTaskService.complete(userTaskFromProcess.getId());

        assertThat(cmmnRuntimeService.createCaseInstanceQuery().count()).isZero();
    }

    @Test
    @CmmnDeployment
    public void testIdVariableName() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("testIdVariableName")
                .start();

        assertThat(processEngineRuntimeService.createProcessInstanceQuery().count()).isEqualTo(1);

        String processIdVariable = (String) cmmnRuntimeService.getVariable(caseInstance.getId(), "processIdVariable");
        assertThat(processIdVariable).isEqualTo(processEngineRuntimeService.createProcessInstanceQuery().singleResult().getId());
    }

    @Test
    @CmmnDeployment
    public void testIdVariableNameExpression() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("testIdVariableName")
                .variable("testVar", "A")
                .start();

        assertThat(processEngineRuntimeService.createProcessInstanceQuery().count()).isEqualTo(1);

        String processIdVariable = (String) cmmnRuntimeService.getVariable(caseInstance.getId(), "variableA");
        assertThat(processIdVariable).isEqualTo(processEngineRuntimeService.createProcessInstanceQuery().singleResult().getId());
    }

    @Test
    public void testSameDeploymentTrue() {
        processEngineRepositoryService.createDeployment()
                .addClasspathResource("org/flowable/cmmn/test/processTaskSameDeploymentTrue.cmmn")
                .addClasspathResource("org/flowable/cmmn/test/oneTaskProcess.bpmn20.xml")
                .deploy();

        cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneProcessTaskCase")
                .start();

        ProcessInstance processInstance = processEngineRuntimeService.createProcessInstanceQuery().singleResult();
        assertThat(processInstance).isNotNull();
        assertThat(processInstance.getProcessDefinitionName()).isEqualTo("One Task");

        Task task = processEngineTaskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).isNotNull();
        assertThat(task.getName()).isEqualTo("my task");

        processEngineTaskService.complete(task.getId());

        processEngineRepositoryService.createDeployment()
                .addClasspathResource("org/flowable/cmmn/test/oneTaskProcessV2.bpmn20.xml")
                .deploy();

        cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneProcessTaskCase")
                .start();

        processInstance = processEngineRuntimeService.createProcessInstanceQuery().singleResult();
        assertThat(processInstance).isNotNull();
        assertThat(processInstance.getProcessDefinitionName()).isEqualTo("One Task");

        task = processEngineTaskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).isNotNull();
        assertThat(task.getName()).isEqualTo("my task");
    }

    @Test
    public void testSameDeploymentTrueWithTenant() {
        processEngineRepositoryService.createDeployment()
                .addClasspathResource("org/flowable/cmmn/test/processTaskSameDeploymentTrue.cmmn")
                .addClasspathResource("org/flowable/cmmn/test/oneTaskProcess.bpmn20.xml")
                .tenantId("flowable")
                .deploy();

        cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneProcessTaskCase")
                .tenantId("flowable")
                .start();

        ProcessInstance processInstance = processEngineRuntimeService.createProcessInstanceQuery().singleResult();
        assertThat(processInstance).isNotNull();
        assertThat(processInstance.getProcessDefinitionName()).isEqualTo("One Task");
        assertThat(processInstance.getTenantId()).isEqualTo("flowable");

        Task task = processEngineTaskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).isNotNull();
        assertThat(task.getName()).isEqualTo("my task");

        processEngineTaskService.complete(task.getId());

        processEngineRepositoryService.createDeployment()
                .addClasspathResource("org/flowable/cmmn/test/oneTaskProcessV2.bpmn20.xml")
                .tenantId("flowable")
                .deploy();

        cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneProcessTaskCase")
                .tenantId("flowable")
                .start();

        processInstance = processEngineRuntimeService.createProcessInstanceQuery().singleResult();
        assertThat(processInstance).isNotNull();
        assertThat(processInstance.getProcessDefinitionName()).isEqualTo("One Task");
        assertThat(processInstance.getTenantId()).isEqualTo("flowable");

        task = processEngineTaskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).isNotNull();
        assertThat(task.getName()).isEqualTo("my task");
    }

    @Test
    public void testWithoutSameDeployment() {
        processEngineRepositoryService.createDeployment()
                .addClasspathResource("org/flowable/cmmn/test/processTaskWithoutSameDeployment.cmmn")
                .addClasspathResource("org/flowable/cmmn/test/oneTaskProcess.bpmn20.xml")
                .deploy();

        cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneProcessTaskCase")
                .start();

        ProcessInstance processInstance = processEngineRuntimeService.createProcessInstanceQuery().singleResult();
        assertThat(processInstance).isNotNull();
        assertThat(processInstance.getProcessDefinitionName()).isEqualTo("One Task");

        Task task = processEngineTaskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).isNotNull();
        assertThat(task.getName()).isEqualTo("my task");

        processEngineTaskService.complete(task.getId());

        processEngineRepositoryService.createDeployment()
                .addClasspathResource("org/flowable/cmmn/test/oneTaskProcessV2.bpmn20.xml")
                .deploy();

        cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneProcessTaskCase")
                .start();

        processInstance = processEngineRuntimeService.createProcessInstanceQuery().singleResult();
        assertThat(processInstance).isNotNull();
        assertThat(processInstance.getProcessDefinitionName()).isEqualTo("One Task V2");

        task = processEngineTaskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).isNotNull();
        assertThat(task.getName()).isEqualTo("Task V2");
    }
    
    @Test
    @CmmnDeployment
    public void testProcessWithVariableListener() {
        Deployment deployment = processEngine.getRepositoryService().createDeployment()
                .addClasspathResource("org/flowable/cmmn/test/processWithVariableListener.bpmn20.xml")
                .deploy();

        try {
            CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                    .caseDefinitionKey("myCase")
                    .start();

            List<PlanItemInstance> planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
                    .caseInstanceId(caseInstance.getId())
                    .planItemInstanceState(PlanItemInstanceState.ACTIVE)
                    .list();

            assertThat(planItemInstances).hasSize(1);
            cmmnRuntimeService.triggerPlanItemInstance(planItemInstances.get(0).getId());

            List<Task> processTasks = processEngine.getTaskService().createTaskQuery().list();
            assertThat(processTasks).hasSize(1);
            Task processTask = processTasks.iterator().next();
            assertThat(processTask.getName()).isEqualTo("my task");
            String subProcessInstanceId = processTask.getProcessInstanceId();
            
            processEngine.getRuntimeService().setVariable(subProcessInstanceId, "var1", "test");
            
            processTask = processEngine.getTaskService().createTaskQuery().processInstanceId(subProcessInstanceId).singleResult();
            assertThat(processTask.getName()).isEqualTo("after task");
            
            processEngine.getTaskService().complete(processTask.getId());
            assertThat(processEngine.getRuntimeService().createProcessInstanceQuery().processInstanceId(subProcessInstanceId).count()).isZero();
            
            planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
                    .caseInstanceId(caseInstance.getId())
                    .planItemInstanceState(PlanItemInstanceState.ACTIVE)
                    .list();
            
            assertThat(planItemInstances).hasSize(1);
            cmmnRuntimeService.triggerPlanItemInstance(planItemInstances.get(0).getId());
            
            assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceId(caseInstance.getId()).count()).isZero();

        } finally {
            processEngine.getRepositoryService().deleteDeployment(deployment.getId(), true);
        }
    }
    
    @Test
    @CmmnDeployment
    public void testMultipleVariableListeners() {
        Deployment deployment = processEngine.getRepositoryService().createDeployment()
                .addClasspathResource("org/flowable/cmmn/test/processWithVariableListener.bpmn20.xml")
                .deploy();

        try {
            CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                    .caseDefinitionKey("myCase")
                    .start();

            List<PlanItemInstance> planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
                    .caseInstanceId(caseInstance.getId())
                    .planItemInstanceState(PlanItemInstanceState.ACTIVE)
                    .list();

            assertThat(planItemInstances).hasSize(1);
            cmmnRuntimeService.triggerPlanItemInstance(planItemInstances.get(0).getId());

            List<Task> processTasks = processEngine.getTaskService().createTaskQuery().list();
            assertThat(processTasks).hasSize(1);
            Task processTask = processTasks.iterator().next();
            assertThat(processTask.getName()).isEqualTo("my task");
            String subProcessInstanceId = processTask.getProcessInstanceId();
            
            assertThat(cmmnRuntimeService.createEventSubscriptionQuery().scopeId(caseInstance.getId()).count()).isEqualTo(1);
            assertThat(processEngine.getRuntimeService().createEventSubscriptionQuery().processInstanceId(subProcessInstanceId).count()).isEqualTo(1);
            assertThat(cmmnRuntimeService.createEventSubscriptionQuery().withoutScopeId().count()).isEqualTo(1);
            assertThat(processEngine.getRuntimeService().createEventSubscriptionQuery().withoutProcessInstanceId().count()).isEqualTo(1);
            
            cmmnRuntimeService.setVariable(caseInstance.getId(), "var1", "test");
            assertThat(cmmnRuntimeService.createEventSubscriptionQuery().scopeId(caseInstance.getId()).count()).isZero();
            planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
                    .caseInstanceId(caseInstance.getId())
                    .planItemInstanceState(PlanItemInstanceState.ACTIVE)
                    .list();
            
            assertThat(planItemInstances).hasSize(2);
            assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionId("listenerTask").count()).isEqualTo(1);
            
            processEngine.getRuntimeService().setVariable(subProcessInstanceId, "var1", "test");
            
            processTask = processEngine.getTaskService().createTaskQuery().processInstanceId(subProcessInstanceId).singleResult();
            assertThat(processTask.getName()).isEqualTo("after task");
            
            processEngine.getTaskService().complete(processTask.getId());
            assertThat(processEngine.getRuntimeService().createProcessInstanceQuery().processInstanceId(subProcessInstanceId).count()).isZero();
            
            planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
                    .caseInstanceId(caseInstance.getId())
                    .planItemDefinitionId("theTask")
                    .planItemInstanceState(PlanItemInstanceState.ACTIVE)
                    .list();
            
            assertThat(planItemInstances).hasSize(1);
            cmmnRuntimeService.triggerPlanItemInstance(planItemInstances.get(0).getId());
            
            assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceId(caseInstance.getId()).count()).isEqualTo(1);
            
            PlanItemInstance planItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .planItemDefinitionId("listenerTask")
                .planItemInstanceState(PlanItemInstanceState.ACTIVE)
                .singleResult();
    
            cmmnRuntimeService.triggerPlanItemInstance(planItemInstance.getId());
            
            assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceId(caseInstance.getId()).count()).isZero();

        } finally {
            processEngine.getRepositoryService().deleteDeployment(deployment.getId(), true);
        }
    }
}
