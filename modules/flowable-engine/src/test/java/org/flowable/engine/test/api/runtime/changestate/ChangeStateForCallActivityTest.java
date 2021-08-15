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

package org.flowable.engine.test.api.runtime.changestate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import java.util.List;

import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.engine.impl.test.HistoryTestHelper;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.entitylink.api.EntityLink;
import org.flowable.entitylink.api.EntityLinkType;
import org.flowable.entitylink.api.HierarchyType;
import org.flowable.task.api.Task;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Frederik Heremans
 * @author Joram Barrez
 * @author Dennis Federico
 */
public class ChangeStateForCallActivityTest extends PluggableFlowableTestCase {

    private ChangeStateEventListener changeStateEventListener = new ChangeStateEventListener();

    @BeforeEach
    protected void setUp() {
        processEngine.getRuntimeService().addEventListener(changeStateEventListener);
    }

    @AfterEach
    protected void tearDown() {
        processEngine.getRuntimeService().removeEventListener(changeStateEventListener);
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/twoTasksParentProcess.bpmn20.xml", "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml" })
    public void testSetCurrentActivityInParentProcess() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("twoTasksParentProcess");
        
        waitForHistoryJobExecutorToProcessAllJobs(10000, 200);
        
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task.getTaskDefinitionKey()).isEqualTo("firstTask");

        taskService.complete(task.getId());
        
        waitForHistoryJobExecutorToProcessAllJobs(10000, 200);

        ProcessInstance subProcessInstance = runtimeService.createProcessInstanceQuery().superProcessInstanceId(processInstance.getId()).singleResult();
        assertThat(subProcessInstance).isNotNull();

        task = taskService.createTaskQuery().processInstanceId(subProcessInstance.getId()).singleResult();
        assertThat(task.getTaskDefinitionKey()).isEqualTo("theTask");

        runtimeService.createChangeActivityStateBuilder()
                .processInstanceId(subProcessInstance.getId())
                .moveActivityIdToParentActivityId("theTask", "secondTask")
                .changeState();
        
        waitForHistoryJobExecutorToProcessAllJobs(10000, 200);

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task.getTaskDefinitionKey()).isEqualTo("secondTask");

        assertThat(runtimeService.createProcessInstanceQuery().superProcessInstanceId(processInstance.getId()).count()).isZero();
        assertThat(runtimeService.createProcessInstanceQuery().processInstanceId(subProcessInstance.getId()).count()).isZero();

        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions).hasSize(1);

        taskService.complete(task.getId());

        assertProcessEnded(processInstance.getId());
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/twoTasksParentProcessV2.bpmn20.xml", "org/flowable/engine/test/api/twoTasksProcess.bpmn20.xml" })
    public void testSetCurrentActivityInParentProcessV2() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("twoTasksParentProcess");
        
        waitForHistoryJobExecutorToProcessAllJobs(10000, 200);
        
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task.getTaskDefinitionKey()).isEqualTo("firstTask");
        taskService.complete(task.getId());
        
        waitForHistoryJobExecutorToProcessAllJobs(10000, 200);

        ProcessInstance subProcessInstance = runtimeService.createProcessInstanceQuery().superProcessInstanceId(processInstance.getId()).singleResult();
        assertThat(subProcessInstance).isNotNull();

        task = taskService.createTaskQuery().processInstanceId(subProcessInstance.getId()).singleResult();
        assertThat(task.getTaskDefinitionKey()).isEqualTo("firstTask");
        taskService.complete(task.getId());
        
        waitForHistoryJobExecutorToProcessAllJobs(10000, 200);

        task = taskService.createTaskQuery().processInstanceId(subProcessInstance.getId()).singleResult();
        assertThat(task.getTaskDefinitionKey()).isEqualTo("secondTask");

        runtimeService.createChangeActivityStateBuilder()
                .processInstanceId(subProcessInstance.getId())
                .moveActivityIdToParentActivityId("secondTask", "secondTask")
                .changeState();

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task.getTaskDefinitionKey()).isEqualTo("secondTask");

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration, 60000)) {
            HistoricTaskInstance historicTaskInstance = historyService.createHistoricTaskInstanceQuery().taskId(task.getId()).singleResult();
            assertThat(historicTaskInstance.getTaskDefinitionKey()).isEqualTo("secondTask");
        }

        assertThat(runtimeService.createProcessInstanceQuery().superProcessInstanceId(processInstance.getId()).count()).isZero();
        assertThat(runtimeService.createProcessInstanceQuery().processInstanceId(subProcessInstance.getId()).count()).isZero();

        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions).hasSize(1);

        taskService.complete(task.getId());

        assertProcessEnded(processInstance.getId());
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/twoTasksParentProcess.bpmn20.xml", "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml" })
    public void testSetCurrentActivityInSubProcessInstance() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("twoTasksParentProcess");
        
        waitForHistoryJobExecutorToProcessAllJobs(10000, 200);
        
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task.getTaskDefinitionKey()).isEqualTo("firstTask");

        runtimeService.createChangeActivityStateBuilder()
                .processInstanceId(processInstance.getId())
                .moveActivityIdToSubProcessInstanceActivityId("firstTask", "theTask", "callActivity")
                .changeState();
        
        waitForHistoryJobExecutorToProcessAllJobs(10000, 200);

        ProcessInstance subProcessInstance = runtimeService.createProcessInstanceQuery().superProcessInstanceId(processInstance.getId()).singleResult();
        assertThat(subProcessInstance).isNotNull();

        assertThat(taskService.createTaskQuery().processInstanceId(processInstance.getId()).count()).isZero();
        assertThat(taskService.createTaskQuery().processInstanceId(subProcessInstance.getId()).count()).isEqualTo(1);

        assertThat(runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().count()).isEqualTo(1);
        assertThat(runtimeService.createExecutionQuery().processInstanceId(subProcessInstance.getId()).onlyChildExecutions().count()).isEqualTo(1);

        task = taskService.createTaskQuery().processInstanceId(subProcessInstance.getId()).singleResult();
        assertThat(task.getTaskDefinitionKey()).isEqualTo("theTask");
        taskService.complete(task.getId());
        
        waitForHistoryJobExecutorToProcessAllJobs(10000, 200);

        assertThat(runtimeService.createProcessInstanceQuery().processInstanceId(subProcessInstance.getId()).count()).isZero();

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task.getTaskDefinitionKey()).isEqualTo("secondTask");

        taskService.complete(task.getId());

        assertProcessEnded(processInstance.getId());
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/twoTasksParentProcessV2.bpmn20.xml", "org/flowable/engine/test/api/twoTasksProcess.bpmn20.xml" })
    public void testSetCurrentActivityInSubProcessInstanceV2() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("twoTasksParentProcess");
        
        waitForHistoryJobExecutorToProcessAllJobs(10000, 200);
        
        Task firstTask = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(firstTask.getTaskDefinitionKey()).isEqualTo("firstTask");

        assertThat(runtimeService.getEntityLinkChildrenForProcessInstance(processInstance.getId()))
                .extracting(EntityLink::getHierarchyType, EntityLink::getReferenceScopeId,
                        EntityLink::getReferenceScopeType, EntityLink::getLinkType)
                .as("hierarchyType, referenceScopeId, referenceScopeType, linkType")
                .containsExactlyInAnyOrder(
                        tuple(HierarchyType.ROOT, firstTask.getId(), ScopeTypes.TASK, EntityLinkType.CHILD)
                );

        assertThat(runtimeService.getEntityLinkChildrenForProcessInstance(processInstance.getId()))
                .extracting(EntityLink::getRootScopeId, EntityLink::getRootScopeType)
                .containsOnly(
                        tuple(processInstance.getId(), ScopeTypes.BPMN)
                );

        runtimeService.createChangeActivityStateBuilder()
                .processInstanceId(processInstance.getId())
                .moveActivityIdToSubProcessInstanceActivityId("firstTask", "secondTask", "callActivity")
                .changeState();

        ProcessInstance subProcessInstance = runtimeService.createProcessInstanceQuery().superProcessInstanceId(processInstance.getId()).singleResult();
        assertThat(subProcessInstance).isNotNull();

        assertThat(taskService.createTaskQuery().processInstanceId(processInstance.getId()).count()).isZero();
        assertThat(taskService.createTaskQuery().processInstanceId(subProcessInstance.getId()).count()).isEqualTo(1);

        assertThat(runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().count()).isEqualTo(1);
        assertThat(runtimeService.createExecutionQuery().processInstanceId(subProcessInstance.getId()).onlyChildExecutions().count()).isEqualTo(1);

        Task firstSecondTask = taskService.createTaskQuery().processInstanceId(subProcessInstance.getId()).singleResult();
        assertThat(firstSecondTask.getTaskDefinitionKey()).isEqualTo("secondTask");

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            HistoricTaskInstance historicTaskInstance = historyService.createHistoricTaskInstanceQuery().taskId(firstSecondTask.getId()).singleResult();
            assertThat(historicTaskInstance.getTaskDefinitionKey()).isEqualTo("secondTask");
        }

        assertThat(runtimeService.getEntityLinkChildrenForProcessInstance(processInstance.getId()))
                .extracting(EntityLink::getHierarchyType, EntityLink::getReferenceScopeId,
                        EntityLink::getReferenceScopeType, EntityLink::getLinkType)
                .as("hierarchyType, referenceScopeId, referenceScopeType, linkType")
                .containsExactlyInAnyOrder(
                        tuple(HierarchyType.ROOT, firstTask.getId(), ScopeTypes.TASK, EntityLinkType.CHILD),
                        tuple(HierarchyType.ROOT, firstSecondTask.getId(), ScopeTypes.TASK, EntityLinkType.CHILD),
                        tuple(HierarchyType.ROOT, subProcessInstance.getId(), ScopeTypes.BPMN, EntityLinkType.CHILD)
                );

        assertThat(runtimeService.getEntityLinkChildrenForProcessInstance(processInstance.getId()))
                .extracting(EntityLink::getRootScopeId, EntityLink::getRootScopeType)
                .containsOnly(
                        tuple(processInstance.getId(), ScopeTypes.BPMN)
                );

        assertThat(runtimeService.getEntityLinkChildrenForProcessInstance(subProcessInstance.getId()))
                .extracting(EntityLink::getHierarchyType, EntityLink::getReferenceScopeId,
                        EntityLink::getReferenceScopeType, EntityLink::getLinkType)
                .as("hierarchyType, referenceScopeId, referenceScopeType, linkType")
                .containsExactlyInAnyOrder(
                        tuple(HierarchyType.PARENT, firstSecondTask.getId(), ScopeTypes.TASK, EntityLinkType.CHILD)
                );

        assertThat(runtimeService.getEntityLinkChildrenForProcessInstance(subProcessInstance.getId()))
                .extracting(EntityLink::getRootScopeId, EntityLink::getRootScopeType)
                .containsOnly(
                        tuple(processInstance.getId(), ScopeTypes.BPMN)
                );

        taskService.complete(firstSecondTask.getId());
        
        waitForHistoryJobExecutorToProcessAllJobs(10000, 200);

        assertThat(runtimeService.createProcessInstanceQuery().processInstanceId(subProcessInstance.getId()).count()).isZero();

        Task secondTask = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(secondTask.getTaskDefinitionKey()).isEqualTo("secondTask");

        taskService.complete(secondTask.getId());

        assertProcessEnded(processInstance.getId());
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/variables/callActivityWithCalledElementExpression.bpmn20.xml" })
    public void testSetCurrentActivityInSubProcessInstanceWithCalledElementExpression() {
        try {
            //Deploy second version of the process definition
            deployProcessDefinition("my deploy", "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml");
    
            ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("calledElementExpression");
            
            waitForHistoryJobExecutorToProcessAllJobs(10000, 200);
    
            Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
            assertThat(task.getTaskDefinitionKey()).isEqualTo("firstTask");
    
            //First change state attempt fails as the calledElement expression cannot be evaluated
            assertThatThrownBy(() -> runtimeService.createChangeActivityStateBuilder()
                    .processInstanceId(processInstance.getId())
                    .moveActivityIdToSubProcessInstanceActivityId("firstTask", "theTask", "callActivity")
                    .changeState())
                    .isExactlyInstanceOf(FlowableException.class)
                    .hasMessage("Cannot resolve calledElement expression '${subProcessDefId}' of callActivity 'callActivity'");
    
            //Change state specifying the variable with the value
            runtimeService.createChangeActivityStateBuilder()
                    .processInstanceId(processInstance.getId())
                    .moveActivityIdToSubProcessInstanceActivityId("firstTask", "theTask", "callActivity", 1)
                    .processVariable("subProcessDefId", "oneTaskProcess")
                    .changeState();
            
            waitForHistoryJobExecutorToProcessAllJobs(10000, 200);
    
            ProcessInstance subProcessInstance = runtimeService.createProcessInstanceQuery().superProcessInstanceId(processInstance.getId()).singleResult();
            assertThat(subProcessInstance).isNotNull();
    
            assertThat(taskService.createTaskQuery().processInstanceId(processInstance.getId()).count()).isZero();
            assertThat(taskService.createTaskQuery().processInstanceId(subProcessInstance.getId()).count()).isEqualTo(1);
    
            assertThat(runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().count()).isEqualTo(1);
            assertThat(runtimeService.createExecutionQuery().processInstanceId(subProcessInstance.getId()).onlyChildExecutions().count()).isEqualTo(1);
    
            task = taskService.createTaskQuery().processInstanceId(subProcessInstance.getId()).singleResult();
            assertThat(task.getTaskDefinitionKey()).isEqualTo("theTask");
            taskService.complete(task.getId());
            
            waitForHistoryJobExecutorToProcessAllJobs(10000, 200);
    
            assertThat(runtimeService.createProcessInstanceQuery().processInstanceId(subProcessInstance.getId()).count()).isZero();
    
            task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
            assertThat(task.getTaskDefinitionKey()).isEqualTo("lastTask");
    
            taskService.complete(task.getId());
    
            assertProcessEnded(processInstance.getId());
            
        } finally {
            deleteDeployments();
        }
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/twoTasksParentProcess.bpmn20.xml", "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml" })
    public void testSetCurrentActivityInSubProcessInstanceSpecificVersion() {
        try {
            // Deploy second version of the process definition
            deployProcessDefinition("my deploy", "org/flowable/engine/test/api/oneTaskProcessV2.bpmn20.xml");
    
            ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("twoTasksParentProcess");
            
            waitForHistoryJobExecutorToProcessAllJobs(10000, 200);
            
            Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
            assertThat(task.getTaskDefinitionKey()).isEqualTo("firstTask");
    
            assertThatThrownBy(() -> runtimeService.createChangeActivityStateBuilder()
                    .processInstanceId(processInstance.getId())
                    .moveActivityIdToSubProcessInstanceActivityId("firstTask", "theTask", "callActivity")
                    .changeState())
                    .isExactlyInstanceOf(FlowableException.class)
                    .hasMessage("Cannot find activity 'theTask' in process definition with id 'oneTaskProcess'");
    
            //Invalid "unExistent" process definition version
            assertThatThrownBy(() -> runtimeService.createChangeActivityStateBuilder()
                    .processInstanceId(processInstance.getId())
                    .moveActivityIdToSubProcessInstanceActivityId("firstTask", "theTask", "callActivity", 5)
                    .changeState())
                    .isExactlyInstanceOf(FlowableException.class)
                    .hasMessage("Cannot find activity 'theTask' in process definition with id 'oneTaskProcess'");
    
            //Change state specifying the first version
            runtimeService.createChangeActivityStateBuilder()
                    .processInstanceId(processInstance.getId())
                    .moveActivityIdToSubProcessInstanceActivityId("firstTask", "theTask", "callActivity", 1)
                    .changeState();
            
            waitForHistoryJobExecutorToProcessAllJobs(10000, 200);
    
            ProcessInstance subProcessInstance = runtimeService.createProcessInstanceQuery().superProcessInstanceId(processInstance.getId()).singleResult();
            assertThat(subProcessInstance).isNotNull();
    
            assertThat(taskService.createTaskQuery().processInstanceId(processInstance.getId()).count()).isZero();
            assertThat(taskService.createTaskQuery().processInstanceId(subProcessInstance.getId()).count()).isEqualTo(1);
    
            assertThat(runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().count()).isEqualTo(1);
            assertThat(runtimeService.createExecutionQuery().processInstanceId(subProcessInstance.getId()).onlyChildExecutions().count()).isEqualTo(1);
    
            task = taskService.createTaskQuery().processInstanceId(subProcessInstance.getId()).singleResult();
            assertThat(task.getTaskDefinitionKey()).isEqualTo("theTask");
            taskService.complete(task.getId());
            
            waitForHistoryJobExecutorToProcessAllJobs(10000, 200);
    
            assertThat(runtimeService.createProcessInstanceQuery().processInstanceId(subProcessInstance.getId()).count()).isZero();
    
            task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
            assertThat(task.getTaskDefinitionKey()).isEqualTo("secondTask");
    
            taskService.complete(task.getId());
    
            assertProcessEnded(processInstance.getId());
            
        } finally {
            deleteDeployments();
        }
    }

}
