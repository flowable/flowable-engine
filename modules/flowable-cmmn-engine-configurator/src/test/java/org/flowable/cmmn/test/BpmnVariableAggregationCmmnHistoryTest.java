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

import org.flowable.cmmn.api.CmmnTaskService;
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.configurator.CmmnEngineConfigurator;
import org.flowable.cmmn.engine.impl.persistence.entity.PlanItemInstanceEntity;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.common.engine.impl.interceptor.EngineConfigurationConstants;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.TaskService;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.cfg.StandaloneInMemProcessEngineConfiguration;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.flowable.variable.service.VariableServiceConfiguration;
import org.flowable.variable.service.impl.persistence.entity.VariableInstanceEntity;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Reproduces (and guards against) the NPE in
 * {@code DefaultCmmnHistoryConfigurationSettings#isHistoryEnabledForVariableInstance} when a BPMN
 * multi-instance variable-aggregation variable is historized through the CMMN history manager in a combined
 * BPMN+CMMN engine.
 *
 * <p>{@code BpmnAggregation} stores the aggregation variable with {@code scopeType = "bpmnVariableAggregation"}
 * and {@code scopeId = <process instance id>}. In a combined deployment a JSON variable's history update is
 * recorded (at command-context close, by {@code TraceableVariablesCommandContextCloseListener} ->
 * {@code TraceableObject#updateIfValueChanged}) via the CMMN history variable manager. It resolves the case
 * definition to determine the (per-definition) history level; the scopeId of an aggregation variable is a
 * process instance id, so treating it directly as a case instance id returned {@code null} and threw an NPE.
 * The fix resolves the owning case of the process (via the plan item instance that started it) so the correct
 * case definition history level is used.
 */
public class BpmnVariableAggregationCmmnHistoryTest {

    protected static int dbCounter = 0;

    protected ProcessEngine processEngine;
    protected ProcessEngineConfigurationImpl processEngineConfiguration;
    protected CmmnEngineConfiguration cmmnEngineConfiguration;

    @BeforeEach
    void setUp() {
        CmmnEngineConfiguration cmmnConfig = new CmmnEngineConfiguration();
        // Enabled by default in the Flowable platform; this activates the case-definition history level branch
        // in DefaultCmmnHistoryConfigurationSettings#isHistoryEnabledForVariableInstance.
        cmmnConfig.setEnableCaseDefinitionHistoryLevel(true);

        CmmnEngineConfigurator cmmnConfigurator = new CmmnEngineConfigurator();
        cmmnConfigurator.setCmmnEngineConfiguration(cmmnConfig);

        StandaloneInMemProcessEngineConfiguration configuration = new StandaloneInMemProcessEngineConfiguration();
        configuration.setJdbcUrl("jdbc:h2:mem:flowable-bpmn-aggregation-cmmn-history-" + (dbCounter++) + ";DB_CLOSE_DELAY=1000");
        configuration.setHistoryLevel(HistoryLevel.AUDIT);
        configuration.setConfigurators(Collections.singletonList(cmmnConfigurator));

        processEngine = configuration.buildProcessEngine();
        processEngineConfiguration = (ProcessEngineConfigurationImpl) processEngine.getProcessEngineConfiguration();
        cmmnEngineConfiguration = (CmmnEngineConfiguration) processEngineConfiguration.getEngineConfigurations()
                .get(EngineConfigurationConstants.KEY_CMMN_ENGINE_CONFIG);
    }

    @AfterEach
    void tearDown() {
        if (processEngine != null) {
            processEngine.close();
        }
    }

    @Test
    void aggregationVariableOfProcessUnderCaseResolvesOwningCaseDefinition() {
        processEngine.getRepositoryService().createDeployment()
                .addClasspathResource("org/flowable/cmmn/test/BpmnVariableAggregationCmmnHistoryTest.bpmn20.xml")
                .deploy();
        cmmnEngineConfiguration.getCmmnRepositoryService().createDeployment()
                .addClasspathResource("org/flowable/cmmn/test/BpmnVariableAggregationCmmnHistoryTest.cmmn")
                .deploy();

        // The case starts the multi-instance aggregation process through a (blocking) process task.
        CaseInstance caseInstance = cmmnEngineConfiguration.getCmmnRuntimeService().createCaseInstanceBuilder()
                .caseDefinitionKey("aggregationCase")
                .start();

        ProcessInstance processInstance = processEngine.getRuntimeService().createProcessInstanceQuery()
                .processDefinitionKey("myProcess").singleResult();
        assertThat(processInstance).isNotNull();

        // Completing one MI task makes the engine create the (JSON) per-instance aggregation variable, stored
        // with scopeType=bpmnVariableAggregation and scopeId=<process instance id>.
        completeOneMultiInstanceTask(processInstance.getId());

        recordAggregationVariableHistoryThroughCmmnManager(processInstance.getId(), caseInstance.getCaseDefinitionId());
    }

    @Test
    void aggregationVariableOfStandaloneProcessFallsBackToEngineLevelWithoutNpe() {
        processEngine.getRepositoryService().createDeployment()
                .addClasspathResource("org/flowable/cmmn/test/BpmnVariableAggregationCmmnHistoryTest.bpmn20.xml")
                .deploy();

        ProcessInstance processInstance = processEngine.getRuntimeService().createProcessInstanceBuilder()
                .processDefinitionKey("myProcess")
                .start();

        completeOneMultiInstanceTask(processInstance.getId());

        // No owning case -> no case definition -> engine default history level, and (crucially) no NPE.
        recordAggregationVariableHistoryThroughCmmnManager(processInstance.getId(), null);
    }

    @Test
    void aggregationVariableOfSubCaseStartedByRootProcessResolvesSubCaseDefinition() {
        // Reverse nesting: a root *process* starts a sub *case* via a case task, and the sub case does the
        // JSON variable aggregation (CMMN aggregation).
        processEngine.getRepositoryService().createDeployment()
                .addClasspathResource("org/flowable/cmmn/test/BpmnVariableAggregationCmmnHistoryTest.rootProcessWithCaseTask.bpmn20.xml")
                .deploy();
        cmmnEngineConfiguration.getCmmnRepositoryService().createDeployment()
                .addClasspathResource("org/flowable/cmmn/test/BpmnVariableAggregationCmmnHistoryTest.subAggregationCase.cmmn")
                .deploy();

        processEngine.getRuntimeService().createProcessInstanceBuilder()
                .processDefinitionKey("rootProcessWithCaseTask")
                .start();

        CaseInstance subCase = cmmnEngineConfiguration.getCmmnRuntimeService().createCaseInstanceQuery()
                .caseDefinitionKey("subAggregationCase").singleResult();
        assertThat(subCase).isNotNull();

        // Completing one repetition creates the CMMN aggregation variable.
        CmmnTaskService cmmnTaskService = cmmnEngineConfiguration.getCmmnTaskService();
        Task reviewTask = cmmnTaskService.createTaskQuery().caseInstanceId(subCase.getId()).singleResult();
        Map<String, Object> variables = new HashMap<>();
        variables.put("approved", true);
        variables.put("description", "description review 0");
        cmmnTaskService.complete(reviewTask.getId(), variables);

        cmmnEngineConfiguration.getCommandExecutor().execute(commandContext -> {
            VariableServiceConfiguration variableServiceConfiguration = cmmnEngineConfiguration.getVariableServiceConfiguration();

            List<VariableInstanceEntity> aggregationVariables = variableServiceConfiguration.getVariableService()
                    .createInternalVariableInstanceQuery()
                    .scopeType(ScopeTypes.CMMN_VARIABLE_AGGREGATION)
                    .names(Collections.singletonList("reviews"))
                    .list();
            assertThat(aggregationVariables).isNotEmpty();

            VariableInstanceEntity aggregationVariable = aggregationVariables.get(0);
            // CMMN aggregation stores the case instance id as scopeId, so it resolves directly through the
            // case instance (this path was never affected by the NPE, unlike BPMN aggregation which stores
            // the process instance id).
            assertThat(aggregationVariable.getScopeId()).isEqualTo(subCase.getId());

            variableServiceConfiguration.getInternalHistoryVariableManager()
                    .recordVariableUpdate(aggregationVariable, cmmnEngineConfiguration.getClock().getCurrentTime());

            return null;
        });
    }

    @Test
    void aggregationVariableOfProcessNestedViaCallActivityUnderCaseResolvesOwningCaseDefinition() {
        // Transitive nesting: case -> process task -> outer process -> BPMN call activity -> inner process,
        // and the inner process does the MI JSON aggregation. The aggregation variable's scopeId is the inner
        // process instance id, which no plan item references directly.
        processEngine.getRepositoryService().createDeployment()
                .addClasspathResource("org/flowable/cmmn/test/BpmnVariableAggregationCmmnHistoryTest.bpmn20.xml")
                .addClasspathResource("org/flowable/cmmn/test/BpmnVariableAggregationCmmnHistoryTest.outerProcess.bpmn20.xml")
                .deploy();
        cmmnEngineConfiguration.getCmmnRepositoryService().createDeployment()
                .addClasspathResource("org/flowable/cmmn/test/BpmnVariableAggregationCmmnHistoryTest.transitiveCase.cmmn")
                .deploy();

        CaseInstance caseInstance = cmmnEngineConfiguration.getCmmnRuntimeService().createCaseInstanceBuilder()
                .caseDefinitionKey("transitiveCase")
                .start();

        ProcessInstance outerProcess = processEngine.getRuntimeService().createProcessInstanceQuery()
                .processDefinitionKey("outerProcess").singleResult();
        ProcessInstance innerProcess = processEngine.getRuntimeService().createProcessInstanceQuery()
                .processDefinitionKey("myProcess").singleResult();
        assertThat(outerProcess).isNotNull();
        assertThat(innerProcess).isNotNull();

        completeOneMultiInstanceTask(innerProcess.getId());

        processEngineConfiguration.getCommandExecutor().execute(commandContext -> {
            VariableServiceConfiguration variableServiceConfiguration = processEngineConfiguration.getVariableServiceConfiguration();

            List<VariableInstanceEntity> aggregationVariables = variableServiceConfiguration.getVariableService()
                    .createInternalVariableInstanceQuery()
                    .scopeType(ScopeTypes.BPMN_VARIABLE_AGGREGATION)
                    .names(Collections.singletonList("reviews"))
                    .list();
            assertThat(aggregationVariables).isNotEmpty();

            VariableInstanceEntity aggregationVariable = aggregationVariables.get(0);
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

            // must not NPE and must resolve the owning case definition via the root process instance
            cmmnEngineConfiguration.getVariableServiceConfiguration().getInternalHistoryVariableManager()
                    .recordVariableUpdate(aggregationVariable, cmmnEngineConfiguration.getClock().getCurrentTime());

            return null;
        });
    }

    protected void completeOneMultiInstanceTask(String processInstanceId) {
        TaskService taskService = processEngine.getTaskService();
        List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstanceId).list();
        assertThat(tasks).hasSize(3);

        Map<String, Object> variables = new HashMap<>();
        variables.put("approved", true);
        variables.put("description", "description task 0");
        taskService.complete(tasks.get(0).getId(), variables);
    }

    protected void recordAggregationVariableHistoryThroughCmmnManager(String processInstanceId, String expectedCaseDefinitionId) {
        processEngineConfiguration.getCommandExecutor().execute(commandContext -> {
            VariableServiceConfiguration variableServiceConfiguration = processEngineConfiguration.getVariableServiceConfiguration();

            List<VariableInstanceEntity> aggregationVariables = variableServiceConfiguration.getVariableService()
                    .createInternalVariableInstanceQuery()
                    .scopeType(ScopeTypes.BPMN_VARIABLE_AGGREGATION)
                    .names(Collections.singletonList("reviews"))
                    .list();
            assertThat(aggregationVariables)
                    .as("engine should have created a bpmnVariableAggregation variable for the completed MI instance")
                    .isNotEmpty();

            VariableInstanceEntity aggregationVariable = aggregationVariables.get(0);
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

            // Exactly what TraceableVariablesCommandContextCloseListener -> TraceableObject#updateIfValueChanged
            // does for a changed JSON variable. In a combined engine the resolved manager is the CMMN one.
            // Before the fix this threw a NullPointerException in DefaultCmmnHistoryConfigurationSettings.
            cmmnEngineConfiguration.getVariableServiceConfiguration().getInternalHistoryVariableManager()
                    .recordVariableUpdate(aggregationVariable, cmmnEngineConfiguration.getClock().getCurrentTime());

            return null;
        });
    }
}
