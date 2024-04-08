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
package org.flowable.cmmn.test.reactivation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.flowable.cmmn.api.runtime.PlanItemInstanceState.ACTIVE;
import static org.flowable.cmmn.api.runtime.PlanItemInstanceState.COMPLETED;
import static org.flowable.cmmn.api.runtime.PlanItemInstanceState.TERMINATED;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.flowable.cmmn.api.history.HistoricCaseInstance;
import org.flowable.cmmn.api.history.HistoricPlanItemInstance;
import org.flowable.cmmn.api.repository.CaseDefinition;
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.engine.test.FlowableCmmnTestCase;
import org.flowable.cmmn.engine.test.impl.CmmnTestHelper;
import org.flowable.common.engine.impl.identity.Authentication;
import org.flowable.variable.api.history.HistoricVariableInstance;
import org.flowable.variable.api.persistence.entity.VariableInstance;
import org.junit.Test;

public class SimpleHistoricCaseReactivationTest extends FlowableCmmnTestCase {

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/reactivation/Simple_Reactivation_Test_Case.cmmn.xml")
    public void simpleMigrateCaseReactivationTest() {
        String previousUserId = Authentication.getAuthenticatedUserId();
        org.flowable.cmmn.api.repository.CmmnDeployment deployment = null;
        try {
            Authentication.setAuthenticatedUserId("simpleCaseReactivationTest_user");
            final HistoricCaseInstance historicCase = createAndFinishSimpleCase("simpleReactivationTestCase");
            
            deployment = cmmnRepositoryService.createDeployment()
                    .addClasspathResource("org/flowable/cmmn/test/reactivation/Simple_Migrate_Reactivation_Test_Case.cmmn.xml")
                    .deploy();

            CaseDefinition newCaseDefinition = cmmnRepositoryService.createCaseDefinitionQuery()
                    .deploymentId(deployment.getId())
                    .singleResult();
            
            cmmnMigrationService.createHistoricCaseInstanceMigrationBuilder()
                .migrateToCaseDefinition(newCaseDefinition.getId())
                .migrate(historicCase.getId());
            
            HistoricCaseInstance historicCaseInstanceAfterMigration = cmmnHistoryService.createHistoricCaseInstanceQuery()
                    .caseInstanceId(historicCase.getId())
                    .singleResult();
            assertThat(historicCaseInstanceAfterMigration.getCaseDefinitionId()).isEqualTo(newCaseDefinition.getId());
            assertThat(historicCaseInstanceAfterMigration.getCaseDefinitionKey()).isEqualTo("simpleReactivationTestCase");
            assertThat(historicCaseInstanceAfterMigration.getCaseDefinitionName()).isEqualTo("Simple Migrate Reactivation Test Case");
            assertThat(historicCaseInstanceAfterMigration.getCaseDefinitionVersion()).isEqualTo(2);
            assertThat(historicCaseInstanceAfterMigration.getCaseDefinitionDeploymentId()).isEqualTo(newCaseDefinition.getDeploymentId());

            CaseInstance reactivatedCase = cmmnHistoryService.createCaseReactivationBuilder(historicCase.getId()).reactivate();
            assertThat(reactivatedCase).isNotNull();

            List<PlanItemInstance> planItemInstances = getAllPlanItemInstances(reactivatedCase.getId());
            assertThat(planItemInstances).isNotNull().hasSize(8);

            // we need to have two reactivation listeners by now, one in terminated state (from the first case completion) and the second one needs to be
            // in completion state as we just triggered it for case reactivation
            assertPlanItemInstanceState(planItemInstances, "Reactivate case", TERMINATED, COMPLETED);

            assertPlanItemInstanceState(planItemInstances, "Task C", TERMINATED);
            
            // the same for the task D, one instance needs to be active
            assertPlanItemInstanceState(planItemInstances, "Task D", ACTIVE);
            assertCaseInstanceNotEnded(reactivatedCase);

            // the plan items must be equal for both the runtime as well as the history as of now
            assertSamePlanItemState(reactivatedCase);

            // make sure we have exactly the same variables as the historic case
            assertSameVariables(historicCase, reactivatedCase);
            
            List<HistoricPlanItemInstance> historicPlanItemInstances = cmmnHistoryService.createHistoricPlanItemInstanceQuery().list();
            assertThat(historicPlanItemInstances).isNotNull().hasSize(8);
            
            Map<String, HistoricPlanItemInstance> historicPlanItemInstanceMap = new HashMap<>();
            for (HistoricPlanItemInstance historicPlanItemInstance : historicPlanItemInstances) {
                historicPlanItemInstanceMap.put(historicPlanItemInstance.getId(), historicPlanItemInstance);
            }
            
            for (PlanItemInstance planItemInstance : planItemInstances) {
                assertThat(historicPlanItemInstanceMap.containsKey(planItemInstance.getId())).isTrue();
                HistoricPlanItemInstance historicPlanItemInstance = historicPlanItemInstanceMap.get(planItemInstance.getId());
                assertThat(historicPlanItemInstance.getState()).isEqualTo(planItemInstance.getState());
                assertThat(historicPlanItemInstance.getElementId()).isEqualTo(planItemInstance.getElementId());
            }
            
        } finally {
            if (deployment != null) {
                CmmnTestHelper.deleteDeployment(cmmnEngineConfiguration, deployment.getId());
            }
            Authentication.setAuthenticatedUserId(previousUserId);
        }
    }
    
    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/reactivation/Simple_Reactivation_Test_Case_No_Reactivation.cmmn.xml")
    public void simpleMigrateCaseWithNoReactivationTest() {
        String previousUserId = Authentication.getAuthenticatedUserId();
        org.flowable.cmmn.api.repository.CmmnDeployment deployment = null;
        try {
            Authentication.setAuthenticatedUserId("simpleCaseReactivationTest_user");
            final HistoricCaseInstance historicCase = createAndFinishSimpleCase("simpleReactivationTestCase");
            
            deployment = cmmnRepositoryService.createDeployment()
                    .addClasspathResource("org/flowable/cmmn/test/reactivation/Simple_Reactivation_Test_Case_Added_Reactivation.cmmn.xml")
                    .deploy();

            CaseDefinition newCaseDefinition = cmmnRepositoryService.createCaseDefinitionQuery()
                    .deploymentId(deployment.getId())
                    .singleResult();
            
            cmmnMigrationService.createHistoricCaseInstanceMigrationBuilder()
                .migrateToCaseDefinition(newCaseDefinition.getId())
                .migrate(historicCase.getId());
            
            HistoricCaseInstance historicCaseInstanceAfterMigration = cmmnHistoryService.createHistoricCaseInstanceQuery()
                    .caseInstanceId(historicCase.getId())
                    .singleResult();
            assertThat(historicCaseInstanceAfterMigration.getCaseDefinitionId()).isEqualTo(newCaseDefinition.getId());
            assertThat(historicCaseInstanceAfterMigration.getCaseDefinitionKey()).isEqualTo("simpleReactivationTestCase");
            assertThat(historicCaseInstanceAfterMigration.getCaseDefinitionName()).isEqualTo("Simple Reactivation Test Case Added Reactivaton");
            assertThat(historicCaseInstanceAfterMigration.getCaseDefinitionVersion()).isEqualTo(2);
            assertThat(historicCaseInstanceAfterMigration.getCaseDefinitionDeploymentId()).isEqualTo(newCaseDefinition.getDeploymentId());

            CaseInstance reactivatedCase = cmmnHistoryService.createCaseReactivationBuilder(historicCase.getId())
                    .addTerminatedPlanItemInstanceForPlanItemDefinition("reactivateEventListener1")
                    .addTerminatedPlanItemInstanceForPlanItemDefinition("humanTask3")
                    .reactivate();
            assertThat(reactivatedCase).isNotNull();

            List<PlanItemInstance> planItemInstances = getAllPlanItemInstances(reactivatedCase.getId());
            assertThat(planItemInstances).isNotNull().hasSize(8);

            // we need to have two reactivation listeners by now, one in terminated state (from the first case completion) and the second one needs to be
            // in completion state as we just triggered it for case reactivation
            assertPlanItemInstanceState(planItemInstances, "Reactivate case", TERMINATED, COMPLETED);

            assertPlanItemInstanceState(planItemInstances, "Task C", TERMINATED, ACTIVE);
            
            assertCaseInstanceNotEnded(reactivatedCase);

            // the plan items must be equal for both the runtime as well as the history as of now
            assertSamePlanItemState(reactivatedCase);

            // make sure we have exactly the same variables as the historic case
            assertSameVariables(historicCase, reactivatedCase);
            
            List<HistoricPlanItemInstance> historicPlanItemInstances = cmmnHistoryService.createHistoricPlanItemInstanceQuery().list();
            assertThat(historicPlanItemInstances).isNotNull().hasSize(8);
            
            Map<String, HistoricPlanItemInstance> historicPlanItemInstanceMap = new HashMap<>();
            for (HistoricPlanItemInstance historicPlanItemInstance : historicPlanItemInstances) {
                historicPlanItemInstanceMap.put(historicPlanItemInstance.getId(), historicPlanItemInstance);
            }
            
            for (PlanItemInstance planItemInstance : planItemInstances) {
                assertThat(historicPlanItemInstanceMap.containsKey(planItemInstance.getId())).isTrue();
                HistoricPlanItemInstance historicPlanItemInstance = historicPlanItemInstanceMap.get(planItemInstance.getId());
                assertThat(historicPlanItemInstance.getState()).isEqualTo(planItemInstance.getState());
                assertThat(historicPlanItemInstance.getElementId()).isEqualTo(planItemInstance.getElementId());
            }
            
            cmmnTaskService.complete(cmmnTaskService.createTaskQuery().caseInstanceId(reactivatedCase.getId()).singleResult().getId());
            assertCaseInstanceEnded(reactivatedCase);
            
        } finally {
            if (deployment != null) {
                CmmnTestHelper.deleteDeployment(cmmnEngineConfiguration, deployment.getId());
            }
            Authentication.setAuthenticatedUserId(previousUserId);
        }
    }

    protected HistoricCaseInstance createAndFinishSimpleCase(String caseDefinitionKey) {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
            .caseDefinitionKey(caseDefinitionKey)
            .variable("foo", "fooValue")
            .variable("bar", "barValue")
            .start();

        List<PlanItemInstance> planItemInstances = getPlanItemInstances(caseInstance.getId());
        assertPlanItemInstanceState(planItemInstances, "Task A", ACTIVE);
        cmmnRuntimeService.triggerPlanItemInstance(getPlanItemInstanceIdByName(planItemInstances, "Task A"));

        planItemInstances = getPlanItemInstances(caseInstance.getId());
        assertPlanItemInstanceState(planItemInstances, "Task B", ACTIVE);
        cmmnRuntimeService.triggerPlanItemInstance(getPlanItemInstanceIdByName(planItemInstances, "Task B"));

        return cmmnHistoryService.createHistoricCaseInstanceQuery().finished().singleResult();
    }

    protected void assertSameVariables(HistoricCaseInstance c1, CaseInstance c2) {
        List<HistoricVariableInstance> originalVars = cmmnEngineConfiguration.getCmmnHistoryService().createHistoricVariableInstanceQuery()
            .caseInstanceId(c1.getId())
            .list();

        Map<String, VariableInstance> reactivatedVars = cmmnEngineConfiguration.getCmmnRuntimeService().getVariableInstances(c2.getId());

        for (HistoricVariableInstance originalVar : originalVars) {
            VariableInstance reactivatedVar = reactivatedVars.remove(originalVar.getVariableName());
            assertThat(reactivatedVar).isNotNull();
            assertThat(reactivatedVar.getValue()).isEqualTo(originalVar.getValue());
        }

        assertThat(reactivatedVars).hasSize(0);
    }

    protected void assertVariableValue(Map<String, VariableInstance> variables, String name, Object value) {
        VariableInstance variable = variables.get(name);
        assertThat(variable).isNotNull();
        assertThat(variable.getValue()).isNotNull().isEqualTo(value);
    }

    protected void assertHistoricVariableValue(List<HistoricVariableInstance> variables, String name, Object value) {
        for (HistoricVariableInstance variable : variables) {
            if (variable.getVariableName().equals(name)) {
                assertThat(variable.getValue()).isEqualTo(value);
            }
        }
    }

    protected void assertHistoricVariableNotExisting(List<HistoricVariableInstance> variables, String name) {
        assertThat(variables.stream().filter(v -> v.getVariableName().equals(name)).collect(Collectors.toList())).isEmpty();
    }
}
