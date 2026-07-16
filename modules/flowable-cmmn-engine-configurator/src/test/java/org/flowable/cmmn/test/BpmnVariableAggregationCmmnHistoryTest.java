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

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.engine.impl.persistence.entity.PlanItemInstanceEntity;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.flowable.variable.service.VariableServiceConfiguration;
import org.flowable.variable.service.impl.persistence.entity.VariableInstanceEntity;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Guards against the NPE in {@code DefaultCmmnHistoryConfigurationSettings#isHistoryEnabledForVariableInstance}
 * when a BPMN multi-instance variable-aggregation variable is historized through the CMMN history manager in a
 * combined BPMN+CMMN engine.
 *
 * <p>{@code BpmnAggregation} stores the aggregation variable with {@code scopeType = "bpmnVariableAggregation"}
 * and {@code scopeId = <process instance id>}. In a combined deployment a JSON variable's history update is
 * recorded (at command-context close) via the CMMN history manager, which must resolve the owning case
 * definition of the (possibly nested) process rather than treating the scopeId as a case instance id.
 *
 * <p>Uses the shared engine of {@link AbstractProcessEngineIntegrationTest}; only the
 * {@code enableCaseDefinitionHistoryLevel} flag (which activates the affected branch) is toggled on for the
 * duration of each test.
 */
public class BpmnVariableAggregationCmmnHistoryTest extends AbstractProcessEngineIntegrationTest {

    protected boolean originalEnableCaseDefinitionHistoryLevel;

    @BeforeEach
    public void enableCaseDefinitionHistoryLevel() {
        originalEnableCaseDefinitionHistoryLevel = cmmnEngineConfiguration.isEnableCaseDefinitionHistoryLevel();
        cmmnEngineConfiguration.setEnableCaseDefinitionHistoryLevel(true);
    }

    @AfterEach
    public void restoreCaseDefinitionHistoryLevel() {
        cmmnEngineConfiguration.setEnableCaseDefinitionHistoryLevel(originalEnableCaseDefinitionHistoryLevel);
    }

    @Test
    public void aggregationVariableOfProcessUnderCaseResolvesOwningCaseDefinition() {
        processEngineRepositoryService.createDeployment()
                .addClasspathResource("org/flowable/cmmn/test/BpmnVariableAggregationCmmnHistoryTest.bpmn20.xml")
                .deploy();
        cmmnRepositoryService.createDeployment()
                .addClasspathResource("org/flowable/cmmn/test/BpmnVariableAggregationCmmnHistoryTest.cmmn")
                .deploy();

        // The case starts the multi-instance aggregation process through a (blocking) process task.
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("aggregationCase")
                .start();

        ProcessInstance processInstance = processEngineRuntimeService.createProcessInstanceQuery()
                .processDefinitionKey("myProcess").singleResult();
        assertThat(processInstance).isNotNull();

        completeOneMultiInstanceTask(processInstance.getId());

        recordBpmnAggregationVariableHistory(processInstance.getId(), caseInstance.getCaseDefinitionId());
    }

    @Test
    public void aggregationVariableOfStandaloneProcessFallsBackToEngineLevelWithoutNpe() {
        processEngineRepositoryService.createDeployment()
                .addClasspathResource("org/flowable/cmmn/test/BpmnVariableAggregationCmmnHistoryTest.bpmn20.xml")
                .deploy();

        ProcessInstance processInstance = processEngineRuntimeService.createProcessInstanceBuilder()
                .processDefinitionKey("myProcess")
                .start();

        completeOneMultiInstanceTask(processInstance.getId());

        // No owning case -> no case definition -> engine default history level, and (crucially) no NPE.
        recordBpmnAggregationVariableHistory(processInstance.getId(), null);
    }

    @Test
    public void aggregationVariableOfProcessNestedViaCallActivityUnderCaseResolvesOwningCaseDefinition() {
        // Transitive nesting: case -> process task -> outer process -> BPMN call activity -> inner process,
        // and the inner process does the MI JSON aggregation. The aggregation variable's scopeId is the inner
        // process instance id, which no plan item references directly.
        processEngineRepositoryService.createDeployment()
                .addClasspathResource("org/flowable/cmmn/test/BpmnVariableAggregationCmmnHistoryTest.bpmn20.xml")
                .addClasspathResource("org/flowable/cmmn/test/BpmnVariableAggregationCmmnHistoryTest.outerProcess.bpmn20.xml")
                .deploy();
        cmmnRepositoryService.createDeployment()
                .addClasspathResource("org/flowable/cmmn/test/BpmnVariableAggregationCmmnHistoryTest.transitiveCase.cmmn")
                .deploy();

        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("transitiveCase")
                .start();

        ProcessInstance outerProcess = processEngineRuntimeService.createProcessInstanceQuery()
                .processDefinitionKey("outerProcess").singleResult();
        ProcessInstance innerProcess = processEngineRuntimeService.createProcessInstanceQuery()
                .processDefinitionKey("myProcess").singleResult();
        assertThat(outerProcess).isNotNull();
        assertThat(innerProcess).isNotNull();

        completeOneMultiInstanceTask(innerProcess.getId());

        cmmnEngineConfiguration.getCommandExecutor().execute(commandContext -> {
            VariableServiceConfiguration variableServiceConfiguration = cmmnEngineConfiguration.getVariableServiceConfiguration();

            VariableInstanceEntity aggregationVariable = findAggregationVariable(variableServiceConfiguration, ScopeTypes.BPMN_VARIABLE_AGGREGATION);
            // scopeId is the INNER (call-activity) process instance id; no plan item references it directly ...
            assertThat(aggregationVariable.getScopeId()).isEqualTo(innerProcess.getId());
            assertThat(cmmnEngineConfiguration.getPlanItemInstanceEntityManager().findByReferenceId(innerProcess.getId()))
                    .as("inner (call-activity) process instance is not referenced by any plan item")
                    .isEmpty();

            // ... so the owning case is resolved by walking up to the ROOT (outer) process instance, which the
            // case's process task started (referenceId == outer process instance id).
            assertThat(cmmnEngineConfiguration.getProcessInstanceService().getRootProcessInstanceId(innerProcess.getId()))
                    .isEqualTo(outerProcess.getId());
            List<PlanItemInstanceEntity> rootPlanItems = cmmnEngineConfiguration.getPlanItemInstanceEntityManager()
                    .findByReferenceId(outerProcess.getId());
            assertThat(rootPlanItems).isNotEmpty();
            assertThat(rootPlanItems.get(0).getCaseDefinitionId()).isEqualTo(caseInstance.getCaseDefinitionId());

            // Must not NPE and must resolve the owning case definition via the root process instance. Calling
            // the (read-only) settings method directly avoids writing a historic variable that the shared-engine
            // db-clean check would flag.
            cmmnEngineConfiguration.getCmmnHistoryConfigurationSettings().isHistoryEnabledForVariableInstance(aggregationVariable);

            return null;
        });
    }

    @Test
    public void aggregationVariableOfSubCaseStartedByRootProcessResolvesSubCaseDefinition() {
        // Reverse nesting: a root process starts a sub case via a case task, and the sub case does the JSON
        // variable aggregation (CMMN aggregation).
        processEngineRepositoryService.createDeployment()
                .addClasspathResource("org/flowable/cmmn/test/BpmnVariableAggregationCmmnHistoryTest.rootProcessWithCaseTask.bpmn20.xml")
                .deploy();
        cmmnRepositoryService.createDeployment()
                .addClasspathResource("org/flowable/cmmn/test/BpmnVariableAggregationCmmnHistoryTest.subAggregationCase.cmmn")
                .deploy();

        processEngineRuntimeService.createProcessInstanceBuilder()
                .processDefinitionKey("rootProcessWithCaseTask")
                .start();

        CaseInstance subCase = cmmnRuntimeService.createCaseInstanceQuery()
                .caseDefinitionKey("subAggregationCase").singleResult();
        assertThat(subCase).isNotNull();

        // Completing one repetition creates the CMMN aggregation variable.
        Task reviewTask = cmmnTaskService.createTaskQuery().caseInstanceId(subCase.getId()).singleResult();
        Map<String, Object> variables = new HashMap<>();
        variables.put("approved", true);
        variables.put("description", "description review 0");
        cmmnTaskService.complete(reviewTask.getId(), variables);

        cmmnEngineConfiguration.getCommandExecutor().execute(commandContext -> {
            VariableServiceConfiguration variableServiceConfiguration = cmmnEngineConfiguration.getVariableServiceConfiguration();

            VariableInstanceEntity aggregationVariable = findAggregationVariable(variableServiceConfiguration, ScopeTypes.CMMN_VARIABLE_AGGREGATION);
            // CMMN aggregation stores the case instance id as scopeId, so it resolves directly through the case
            // instance (this path was never affected by the NPE, unlike BPMN aggregation which stores the
            // process instance id).
            assertThat(aggregationVariable.getScopeId()).isEqualTo(subCase.getId());

            // Resolves directly through the case instance; must not NPE (read-only, writes no history).
            cmmnEngineConfiguration.getCmmnHistoryConfigurationSettings().isHistoryEnabledForVariableInstance(aggregationVariable);

            return null;
        });
    }

    protected void completeOneMultiInstanceTask(String processInstanceId) {
        List<Task> tasks = processEngineTaskService.createTaskQuery().processInstanceId(processInstanceId).list();
        assertThat(tasks).hasSize(3);

        Map<String, Object> variables = new HashMap<>();
        variables.put("approved", true);
        variables.put("description", "description task 0");
        // Completing one multi-instance task makes the engine create the (JSON) per-instance aggregation
        // variable, stored with scopeType=bpmnVariableAggregation and scopeId=<process instance id>.
        processEngineTaskService.complete(tasks.get(0).getId(), variables);
    }

    protected void recordBpmnAggregationVariableHistory(String processInstanceId, String expectedCaseDefinitionId) {
        cmmnEngineConfiguration.getCommandExecutor().execute(commandContext -> {
            VariableServiceConfiguration variableServiceConfiguration = cmmnEngineConfiguration.getVariableServiceConfiguration();

            VariableInstanceEntity aggregationVariable = findAggregationVariable(variableServiceConfiguration, ScopeTypes.BPMN_VARIABLE_AGGREGATION);
            // The bug precondition: a non-null scopeId that is a process instance id, not a case instance id.
            assertThat(aggregationVariable.getScopeType()).isEqualTo(ScopeTypes.BPMN_VARIABLE_AGGREGATION);
            assertThat(aggregationVariable.getScopeId()).isEqualTo(processInstanceId);

            // The fix resolves the owning case (if any) via the plan item instance that started the process.
            List<PlanItemInstanceEntity> planItemInstances = cmmnEngineConfiguration.getPlanItemInstanceEntityManager()
                    .findByReferenceId(processInstanceId);
            if (expectedCaseDefinitionId != null) {
                assertThat(planItemInstances).isNotEmpty();
                assertThat(planItemInstances.get(0).getCaseDefinitionId()).isEqualTo(expectedCaseDefinitionId);
            } else {
                assertThat(planItemInstances).isEmpty();
            }

            // This is the method the traceable-variables close listener ultimately calls; invoking it directly
            // reproduces the failure without writing history. Before the fix it threw a NullPointerException in
            // DefaultCmmnHistoryConfigurationSettings (scopeId treated as a case instance id).
            cmmnEngineConfiguration.getCmmnHistoryConfigurationSettings().isHistoryEnabledForVariableInstance(aggregationVariable);

            return null;
        });
    }

    protected VariableInstanceEntity findAggregationVariable(VariableServiceConfiguration variableServiceConfiguration, String scopeType) {
        List<VariableInstanceEntity> aggregationVariables = variableServiceConfiguration.getVariableService()
                .createInternalVariableInstanceQuery()
                .scopeType(scopeType)
                .names(Collections.singletonList("reviews"))
                .list();
        assertThat(aggregationVariables)
                .as("engine should have created a %s aggregation variable named 'reviews'", scopeType)
                .isNotEmpty();
        return aggregationVariables.get(0);
    }
}
