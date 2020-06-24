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

import org.flowable.cmmn.api.repository.CaseDefinitionQuery;
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.api.runtime.CaseInstanceBuilder;
import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstanceState;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Tijs Rademakers
 */
public class ChangeStateProcessTaskTest extends AbstractProcessEngineIntegrationTest {

    @Before
    public void deployOneTaskProcess() {
        if (processEngineRepositoryService.createDeploymentQuery().count() == 0) {
            processEngineRepositoryService.createDeployment().addClasspathResource("org/flowable/cmmn/test/oneTaskProcess.bpmn20.xml").deploy();
        }
    }

    @Test
    @CmmnDeployment
    public void testActivateProcessTask() {
        CaseInstance caseInstance = startCaseInstanceWithOneTaskProcess("activateFirstTask", false);
        cmmnRuntimeService.createChangePlanItemStateBuilder()
                .caseInstanceId(caseInstance.getId())
                .activatePlanItemDefinitionId("theProcess")
                .changeState();

        ProcessInstance processInstance = processEngineRuntimeService.createProcessInstanceQuery().singleResult();
        assertThat(processInstance).isNotNull();

        Task task = processEngineTaskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task.getName()).isEqualTo("my task");

        processEngineTaskService.complete(task.getId());

        assertThat(cmmnTaskService.createTaskQuery().count()).isZero();
        assertThat(processEngineRuntimeService.createProcessInstanceQuery().count()).isZero();

        assertThat(cmmnRuntimeService.createCaseInstanceQuery().count()).isEqualTo(1);

        cmmnRuntimeService.createChangePlanItemStateBuilder()
                .caseInstanceId(caseInstance.getId())
                .activatePlanItemDefinitionId("theTask")
                .changeState();

        List<PlanItemInstance> planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .planItemInstanceState(PlanItemInstanceState.ACTIVE)
                .list();
        assertThat(planItemInstances).hasSize(1);
        cmmnRuntimeService.triggerPlanItemInstance(planItemInstances.get(0).getId());

        assertThat(cmmnTaskService.createTaskQuery().count()).isZero();
        assertThat(processEngineRuntimeService.createProcessInstanceQuery().count()).isZero();
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().count()).isZero();
    }

    @Test
    @CmmnDeployment(resources = { "org/flowable/cmmn/test/ChangeStateProcessTaskTest.testActivateProcessTask.cmmn" })
    public void testMoveToProcessTask() {
        CaseInstance caseInstance = startCaseInstanceWithOneTaskProcess("activateFirstTask", true);

        cmmnRuntimeService.createChangePlanItemStateBuilder()
                .caseInstanceId(caseInstance.getId())
                .terminatePlanItemDefinitionId("theTask")
                .activatePlanItemDefinitionId("theProcess")
                .changeState();

        ProcessInstance processInstance = processEngineRuntimeService.createProcessInstanceQuery().singleResult();
        assertThat(processInstance).isNotNull();

        Task task = processEngineTaskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task.getName()).isEqualTo("my task");

        processEngineTaskService.complete(task.getId());

        assertThat(cmmnTaskService.createTaskQuery().count()).isZero();
        assertThat(processEngineRuntimeService.createProcessInstanceQuery().count()).isZero();
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().count()).isZero();
    }

    @Test
    @CmmnDeployment(resources = { "org/flowable/cmmn/test/ChangeStateProcessTaskTest.testActivateProcessTask.cmmn" })
    public void testActivateProcessTaskWithInitialTask() {
        CaseInstance caseInstance = startCaseInstanceWithOneTaskProcess("activateFirstTask", true);

        List<PlanItemInstance> planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .planItemInstanceState(PlanItemInstanceState.ACTIVE)
                .list();
        assertThat(planItemInstances).hasSize(1);

        cmmnRuntimeService.createChangePlanItemStateBuilder()
                .caseInstanceId(caseInstance.getId())
                .activatePlanItemDefinitionId("theProcess")
                .changeState();

        ProcessInstance processInstance = processEngineRuntimeService.createProcessInstanceQuery().singleResult();
        assertThat(processInstance).isNotNull();

        Task task = processEngineTaskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task.getName()).isEqualTo("my task");

        cmmnRuntimeService.triggerPlanItemInstance(planItemInstances.get(0).getId());

        processEngineTaskService.complete(task.getId());

        assertThat(cmmnTaskService.createTaskQuery().count()).isZero();
        assertThat(processEngineRuntimeService.createProcessInstanceQuery().count()).isZero();
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().count()).isZero();
    }

    @Test
    @CmmnDeployment
    public void testActivateProcessTaskInStage() {
        CaseInstance caseInstance = startCaseInstanceWithOneTaskProcess("activateStage", false);

        assertThat(processEngineRuntimeService.createProcessInstanceQuery().count()).isZero();

        cmmnRuntimeService.createChangePlanItemStateBuilder()
                .caseInstanceId(caseInstance.getId())
                .activatePlanItemDefinitionId("theProcess")
                .changeState();

        ProcessInstance processInstance = processEngineRuntimeService.createProcessInstanceQuery().singleResult();
        assertThat(processInstance).isNotNull();

        Task task = processEngineTaskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task.getName()).isEqualTo("my task");

        cmmnRuntimeService.createChangePlanItemStateBuilder()
                .caseInstanceId(caseInstance.getId())
                .activatePlanItemDefinitionId("theProcess2")
                .changeState();

        assertThat(processEngineRuntimeService.createProcessInstanceQuery().count()).isEqualTo(2);
        assertThat(processEngineTaskService.createTaskQuery().count()).isEqualTo(2);

        processEngineTaskService.complete(task.getId());

        ProcessInstance processInstance2 = processEngineRuntimeService.createProcessInstanceQuery().singleResult();
        assertThat(processInstance2).isNotNull();
        assertThat(processInstance2.getId()).isNotEqualTo(processInstance.getId());

        task = processEngineTaskService.createTaskQuery().processInstanceId(processInstance2.getId()).singleResult();
        assertThat(task.getName()).isEqualTo("my task");

        assertThat(processEngineRuntimeService.createProcessInstanceQuery().count()).isEqualTo(1);
        assertThat(processEngineTaskService.createTaskQuery().count()).isEqualTo(1);

        processEngineTaskService.complete(task.getId());

        assertThat(cmmnTaskService.createTaskQuery().count()).isZero();
        assertThat(processEngineRuntimeService.createProcessInstanceQuery().count()).isZero();
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().count()).isZero();
    }

    @Test
    @CmmnDeployment(resources = { "org/flowable/cmmn/test/ChangeStateProcessTaskTest.testActivateProcessTaskInStage.cmmn" })
    public void testActivateProcessTaskInStageWithInitialStage() {
        CaseInstance caseInstance = startCaseInstanceWithOneTaskProcess("activateStage", true);

        assertThat(processEngineRuntimeService.createProcessInstanceQuery().count()).isEqualTo(1);
        ProcessInstance processInstance = processEngineRuntimeService.createProcessInstanceQuery().singleResult();
        assertThat(processInstance).isNotNull();

        cmmnRuntimeService.createChangePlanItemStateBuilder()
                .caseInstanceId(caseInstance.getId())
                .activatePlanItemDefinitionId("theProcess2")
                .changeState();

        assertThat(processEngineRuntimeService.createProcessInstanceQuery().count()).isEqualTo(2);
        assertThat(processEngineTaskService.createTaskQuery().count()).isEqualTo(2);

        Task task = processEngineTaskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task.getName()).isEqualTo("my task");

        processEngineTaskService.complete(task.getId());

        ProcessInstance processInstance2 = processEngineRuntimeService.createProcessInstanceQuery().singleResult();
        assertThat(processInstance2).isNotNull();

        task = processEngineTaskService.createTaskQuery().processInstanceId(processInstance2.getId()).singleResult();
        assertThat(task.getName()).isEqualTo("my task");

        processEngineTaskService.complete(task.getId());

        assertThat(cmmnTaskService.createTaskQuery().count()).isZero();
        assertThat(processEngineRuntimeService.createProcessInstanceQuery().count()).isZero();
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().count()).isZero();
    }

    @Test
    @CmmnDeployment(resources = { "org/flowable/cmmn/test/ChangeStateProcessTaskTest.testActivateProcessTask.cmmn" })
    public void testActivateProcessTaskWithVariables() {
        CaseInstance caseInstance = startCaseInstanceWithOneTaskProcess("activateFirstTask", true);

        cmmnRuntimeService.createChangePlanItemStateBuilder()
                .caseInstanceId(caseInstance.getId())
                .activatePlanItemDefinitionId("theProcess")
                .childInstanceTaskVariable("theProcess", "textVar", "Some text")
                .childInstanceTaskVariable("theProcess", "numVar", 10)
                .changeState();

        ProcessInstance processInstance = processEngineRuntimeService.createProcessInstanceQuery().singleResult();
        assertThat(processInstance).isNotNull();

        assertThat(processEngineRuntimeService.getVariable(processInstance.getId(), "textVar")).isEqualTo("Some text");
        assertThat(processEngineRuntimeService.getVariable(processInstance.getId(), "numVar")).isEqualTo(10);
        assertThat(cmmnRuntimeService.getVariable(caseInstance.getId(), "textVar")).isNull();
        assertThat(cmmnRuntimeService.getVariable(caseInstance.getId(), "numVar")).isNull();

        Task task = processEngineTaskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task.getName()).isEqualTo("my task");

        processEngineTaskService.complete(task.getId());

        assertThat(processEngineHistoryService.createHistoricVariableInstanceQuery()
                .processInstanceId(processInstance.getId())
                .variableName("textVar")
                .singleResult().getValue()).isEqualTo("Some text");

        assertThat(processEngineHistoryService.createHistoricVariableInstanceQuery()
                .processInstanceId(processInstance.getId())
                .variableName("numVar")
                .singleResult().getValue()).isEqualTo(10);

        assertThat(cmmnRuntimeService.getVariable(caseInstance.getId(), "textVar")).isNull();
        assertThat(cmmnRuntimeService.getVariable(caseInstance.getId(), "numVar")).isNull();

        List<PlanItemInstance> planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .planItemInstanceState(PlanItemInstanceState.ACTIVE)
                .list();
        cmmnRuntimeService.triggerPlanItemInstance(planItemInstances.get(0).getId());

        assertThat(cmmnTaskService.createTaskQuery().count()).isZero();
        assertThat(processEngineRuntimeService.createProcessInstanceQuery().count()).isZero();
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().count()).isZero();
    }

    @Test
    @CmmnDeployment(resources = { "org/flowable/cmmn/test/twoTasksWithProcessTask.cmmn" })
    public void testActivateProcessTaskAndMoveStateWithVariables() {
        CaseInstance caseInstance = startCaseInstanceWithOneTaskProcess("activateFirstTask", true);

        List<PlanItemInstance> planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .planItemInstanceState(PlanItemInstanceState.ACTIVE)
                .list();
        assertThat(planItemInstances).hasSize(1);
        assertThat(planItemInstances.get(0).getPlanItemDefinitionId()).isEqualTo("theTask");

        cmmnRuntimeService.createChangePlanItemStateBuilder()
                .caseInstanceId(caseInstance.getId())
                .terminatePlanItemDefinitionId("theTask")
                .activatePlanItemDefinitionId("theTask2")
                .activatePlanItemDefinitionId("theProcess")
                .childInstanceTaskVariable("theProcess", "textVar", "Some text")
                .childInstanceTaskVariable("theProcess", "numVar", 10)
                .changeState();

        planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .planItemInstanceState(PlanItemInstanceState.ACTIVE)
                .list();
        assertThat(planItemInstances).hasSize(2);

        planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .planItemInstanceState(PlanItemInstanceState.ACTIVE)
                .planItemDefinitionId("theProcess")
                .list();
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getPlanItemDefinitionId)
                .containsExactly("theProcess");

        planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .planItemInstanceState(PlanItemInstanceState.ACTIVE)
                .planItemDefinitionId("theTask2")
                .list();
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getPlanItemDefinitionId)
                .containsExactly("theTask2");

        planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .planItemInstanceState(PlanItemInstanceState.TERMINATED)
                .planItemDefinitionId("theTask")
                .includeEnded()
                .list();
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getPlanItemDefinitionId)
                .containsExactly("theTask");

        ProcessInstance processInstance = processEngineRuntimeService.createProcessInstanceQuery().singleResult();
        assertThat(processInstance).isNotNull();

        assertThat(processEngineRuntimeService.getVariable(processInstance.getId(), "textVar")).isEqualTo("Some text");
        assertThat(processEngineRuntimeService.getVariable(processInstance.getId(), "numVar")).isEqualTo(10);
        assertThat(cmmnRuntimeService.getVariable(caseInstance.getId(), "textVar")).isNull();
        assertThat(cmmnRuntimeService.getVariable(caseInstance.getId(), "numVar")).isNull();

        Task task = processEngineTaskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task.getName()).isEqualTo("my task");

        processEngineTaskService.complete(task.getId());

        assertThat(processEngineHistoryService.createHistoricVariableInstanceQuery()
                .processInstanceId(processInstance.getId())
                .variableName("textVar")
                .singleResult().getValue()).isEqualTo("Some text");

        assertThat(processEngineHistoryService.createHistoricVariableInstanceQuery()
                .processInstanceId(processInstance.getId())
                .variableName("numVar")
                .singleResult().getValue()).isEqualTo(10);

        assertThat(cmmnRuntimeService.getVariable(caseInstance.getId(), "textVar")).isNull();
        assertThat(cmmnRuntimeService.getVariable(caseInstance.getId(), "numVar")).isNull();

        planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .planItemInstanceState(PlanItemInstanceState.ACTIVE)
                .list();
        assertThat(planItemInstances).hasSize(1);
        cmmnRuntimeService.triggerPlanItemInstance(planItemInstances.get(0).getId());

        assertThat(cmmnTaskService.createTaskQuery().count()).isZero();
        assertThat(processEngineRuntimeService.createProcessInstanceQuery().count()).isZero();
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().count()).isZero();
    }

    @Test
    @CmmnDeployment(resources = { "org/flowable/cmmn/test/ChangeStateProcessTaskTest.testActivateProcessTask.cmmn" })
    public void testMoveProcessTaskWithVariables() {
        CaseInstance caseInstance = startCaseInstanceWithOneTaskProcess("activateFirstTask", true);

        cmmnRuntimeService.createChangePlanItemStateBuilder()
                .caseInstanceId(caseInstance.getId())
                .terminatePlanItemDefinitionId("theTask")
                .activatePlanItemDefinitionId("theProcess")
                .childInstanceTaskVariable("theProcess", "textVar", "Some text")
                .childInstanceTaskVariable("theProcess", "numVar", 10)
                .changeState();

        ProcessInstance processInstance = processEngineRuntimeService.createProcessInstanceQuery().singleResult();
        assertThat(processInstance).isNotNull();

        assertThat(processEngineRuntimeService.getVariable(processInstance.getId(), "textVar")).isEqualTo("Some text");
        assertThat(processEngineRuntimeService.getVariable(processInstance.getId(), "numVar")).isEqualTo(10);
        assertThat(cmmnRuntimeService.getVariable(caseInstance.getId(), "textVar")).isNull();
        assertThat(cmmnRuntimeService.getVariable(caseInstance.getId(), "numVar")).isNull();

        Task task = processEngineTaskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task.getName()).isEqualTo("my task");

        processEngineTaskService.complete(task.getId());

        assertThat(processEngineHistoryService.createHistoricVariableInstanceQuery()
                .processInstanceId(processInstance.getId())
                .variableName("textVar")
                .singleResult().getValue()).isEqualTo("Some text");

        assertThat(processEngineHistoryService.createHistoricVariableInstanceQuery()
                .processInstanceId(processInstance.getId())
                .variableName("numVar")
                .singleResult().getValue()).isEqualTo(10);

        assertThat(cmmnTaskService.createTaskQuery().count()).isZero();
        assertThat(processEngineRuntimeService.createProcessInstanceQuery().count()).isZero();
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().count()).isZero();
    }

    protected CaseInstance startCaseInstanceWithOneTaskProcess(String variableName, boolean activate) {
        CaseDefinitionQuery caseDefinitionQuery = cmmnRepositoryService.createCaseDefinitionQuery();
        String caseDefinitionId = caseDefinitionQuery.singleResult().getId();
        CaseInstanceBuilder caseInstanceBuilder = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionId(caseDefinitionId)
                .variable(variableName, activate);

        CaseInstance caseInstance = caseInstanceBuilder.start();
        return caseInstance;
    }
}
