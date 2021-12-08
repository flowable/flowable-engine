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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.flowable.cmmn.api.runtime.PlanItemInstanceState.ACTIVE;
import static org.flowable.cmmn.api.runtime.PlanItemInstanceState.COMPLETED;
import static org.flowable.cmmn.api.runtime.PlanItemInstanceState.TERMINATED;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.flowable.cmmn.api.history.HistoricCaseInstance;
import org.flowable.cmmn.api.history.HistoricPlanItemInstance;
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstanceState;
import org.flowable.cmmn.converter.CmmnXMLException;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.engine.test.FlowableCmmnTestCase;
import org.flowable.cmmn.engine.test.impl.CmmnHistoryTestHelper;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.FlowableIllegalStateException;
import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.common.engine.impl.identity.Authentication;
import org.flowable.identitylink.api.IdentityLink;
import org.flowable.identitylink.api.IdentityLinkInfo;
import org.flowable.identitylink.api.IdentityLinkType;
import org.flowable.identitylink.api.history.HistoricIdentityLink;
import org.flowable.identitylink.service.impl.persistence.entity.HistoricIdentityLinkEntity;
import org.flowable.identitylink.service.impl.persistence.entity.IdentityLinkEntity;
import org.flowable.task.api.Task;
import org.flowable.variable.api.history.HistoricVariableInstance;
import org.flowable.variable.api.persistence.entity.VariableInstance;
import org.junit.Test;

public class SimpleCaseReactivationTest extends FlowableCmmnTestCase {
    
    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/reactivation/Simple_Reactivation_Test_Case_No_Event.cmmn.xml")
    public void simpleCaseReactivationMissingCaseFailureTest() {
        cmmnRuntimeService.createCaseInstanceBuilder()
            .caseDefinitionKey("simpleReactivationTestCaseNoEvent")
            .start();

        assertThatThrownBy(() -> cmmnHistoryService.createCaseReactivationBuilder("nonexistentCaseId").reactivate())
                .isExactlyInstanceOf(FlowableObjectNotFoundException.class)
                .hasMessageContaining("No historic case instance to be reactivated found with id: nonexistentCaseId");
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/reactivation/Simple_Reactivation_Test_Case_No_Event.cmmn.xml")
    public void simpleCaseReactivationActiveCaseFailureTest() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
            .caseDefinitionKey("simpleReactivationTestCaseNoEvent")
            .start();

        assertThatThrownBy(() -> cmmnHistoryService.createCaseReactivationBuilder(caseInstance.getId()).reactivate())
                .isExactlyInstanceOf(FlowableIllegalStateException.class)
                .hasMessageContaining("Case instance is still running, cannot reactivate historic case instance: " + caseInstance.getId());
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/reactivation/Simple_Reactivation_Test_Case_No_Event.cmmn.xml")
    public void simpleCaseReactivationNoReactivationEventFailureTest() {
        final HistoricCaseInstance historicCase = createAndFinishSimpleCase("simpleReactivationTestCaseNoEvent");

        assertThatThrownBy(() -> cmmnHistoryService.createCaseReactivationBuilder(historicCase.getId()).reactivate())
                .isExactlyInstanceOf(FlowableIllegalStateException.class)
                .hasMessageContaining("The historic case instance " + historicCase.getId() +
                    " cannot be reactivated as there is no reactivation event in its CMMN model. You need to explicitly model the reactivation event in order to support case reactivation.");
    }

    @Test
    public void simpleCaseReactivationMultiReactivationEventFailureTest() {
        assertThatThrownBy(() -> addDeploymentForAutoCleanup(cmmnRepositoryService.createDeployment()
                .addClasspathResource("org/flowable/cmmn/test/reactivation/Simple_Reactivation_Test_Case_Multi_Reactivation_Elements.cmmn.xml")
                .deploy()
            ))
                .isExactlyInstanceOf(CmmnXMLException.class)
                .hasRootCauseInstanceOf(FlowableIllegalArgumentException.class)
                .getRootCause()
                .hasMessageContaining("There can only be one reactivation listener on a case model, not multiple ones. Use a start form on the listener, "
                    + "if there are several options on how to reactivate a case and use conditions to handle the different options on reactivation.");
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/reactivation/Simple_Reactivation_Test_Case.cmmn.xml")
    public void reactivationListenerNotAvailableAtCaseRuntime() {
        String previousUserId = Authentication.getAuthenticatedUserId();
        try {
            Authentication.setAuthenticatedUserId("reactivationListenerNotAvailableAtCaseRuntime_user");
            CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("simpleReactivationTestCase")
                .start();

            assertThat(caseInstance).isNotNull();
            List<PlanItemInstance> planItemInstances = getAllPlanItemInstances(caseInstance.getId());
            assertThat(planItemInstances).isNotNull().hasSize(5);
            assertPlanItemInstanceState(caseInstance, "Reactivate case", PlanItemInstanceState.UNAVAILABLE);
        } finally {
            Authentication.setAuthenticatedUserId(previousUserId);
        }
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/reactivation/Simple_Reactivation_Test_Case.cmmn.xml")
    public void reactivationListenerHavingNoImpactAtCaseCompletion() {
        String previousUserId = Authentication.getAuthenticatedUserId();
        try {
            Authentication.setAuthenticatedUserId("reactivationListenerHavingNoImpactAtCaseCompletion_user");
            CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("simpleReactivationTestCase")
                .start();
            List<PlanItemInstance> planItemInstances = getPlanItemInstances(caseInstance.getId());
            cmmnRuntimeService.triggerPlanItemInstance(getPlanItemInstanceIdByName(planItemInstances, "Task A"));
            planItemInstances = getPlanItemInstances(caseInstance.getId());
            cmmnRuntimeService.triggerPlanItemInstance(getPlanItemInstanceIdByName(planItemInstances, "Task B"));

            assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().count()).isZero();
            assertThat(cmmnRuntimeService.createCaseInstanceQuery().count()).isZero();
            assertCaseInstanceEnded(caseInstance);
        } finally {
            Authentication.setAuthenticatedUserId(previousUserId);
        }
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/reactivation/Simple_Reactivation_Test_Case.cmmn.xml")
    public void simpleCaseReactivationTest() {
        String previousUserId = Authentication.getAuthenticatedUserId();
        try {
            Authentication.setAuthenticatedUserId("simpleCaseReactivationTest_user");
            final HistoricCaseInstance historicCase = createAndFinishSimpleCase("simpleReactivationTestCase");

            CaseInstance reactivatedCase = cmmnHistoryService.createCaseReactivationBuilder(historicCase.getId()).reactivate();
            assertThat(reactivatedCase).isNotNull();

            List<PlanItemInstance> planItemInstances = getAllPlanItemInstances(reactivatedCase.getId());
            assertThat(planItemInstances).isNotNull().hasSize(8);

            // we need to have two reactivation listeners by now, one in terminated state (from the first case completion) and the second one needs to be
            // in completion state as we just triggered it for case reactivation
            assertPlanItemInstanceState(planItemInstances, "Reactivate case", TERMINATED, COMPLETED);

            // the same for the task C, one instance needs to be active, the old one completed
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
            
        } finally {
            Authentication.setAuthenticatedUserId(previousUserId);
        }
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/reactivation/Simple_Reactivation_Test_Case.cmmn.xml")
    public void simpleCaseReactivationHistoryTest() {
        String previousUserId = Authentication.getAuthenticatedUserId();
        try {
            Authentication.setAuthenticatedUserId("simpleCaseReactivationHistoryTest_user");
            final HistoricCaseInstance historicCase = createAndFinishSimpleCase("simpleReactivationTestCase");

            CaseInstance reactivatedCase = cmmnHistoryService.createCaseReactivationBuilder(historicCase.getId()).reactivate();
            assertThat(reactivatedCase).isNotNull();

            HistoricCaseInstance historicCaseInstance = cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceId(historicCase.getId()).singleResult();
            assertThat(historicCaseInstance).isNotNull();

            // check the state being in sync with the runtime instance and the end time being reset as the case is not ended anymore
            assertThat(historicCaseInstance.getState()).isEqualTo(reactivatedCase.getState());
            assertThat(historicCaseInstance.getEndTime()).isNull();
            
        } finally {
            Authentication.setAuthenticatedUserId(previousUserId);
        }
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/reactivation/Simple_Reactivation_Test_Case.cmmn.xml")
    public void simpleCaseReactivationDataTest() {
        String previousUserId = Authentication.getAuthenticatedUserId();
        try {
            Authentication.setAuthenticatedUserId("simpleCaseReactivationDataTest_user");
            final HistoricCaseInstance historicCase = createAndFinishSimpleCase("simpleReactivationTestCase");

            if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
                CaseInstance reactivatedCase = cmmnHistoryService.createCaseReactivationBuilder(historicCase.getId()).reactivate();
                assertThat(reactivatedCase).isNotNull();
    
                HistoricCaseInstance historicCaseInstance = cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceId(historicCase.getId()).singleResult();
                assertThat(historicCaseInstance).isNotNull();
    
                // also check, if the reactivation user and timestamp have been set on both, the runtime and the historic case instance
                assertThat(historicCaseInstance.getLastReactivationTime()).isNotNull();
                assertThat(historicCaseInstance.getLastReactivationUserId()).isEqualTo("simpleCaseReactivationDataTest_user");
    
                assertThat(reactivatedCase.getLastReactivationTime()).isNotNull();
                assertThat(reactivatedCase.getLastReactivationUserId()).isEqualTo("simpleCaseReactivationDataTest_user");
    
                List<IdentityLink> identityLinks = cmmnRuntimeService.getIdentityLinksForCaseInstance(reactivatedCase.getId());
                assertThat(identityLinks)
                    .extracting(IdentityLinkInfo::getType)
                    .containsExactlyInAnyOrder(IdentityLinkType.STARTER, IdentityLinkType.PARTICIPANT, IdentityLinkType.REACTIVATOR);
    
                List<HistoricIdentityLink> historicIdentityLinks = cmmnHistoryService.getHistoricIdentityLinksForCaseInstance(historicCase.getId());
                assertThat(historicIdentityLinks)
                    .extracting(IdentityLinkInfo::getType)
                    .containsExactlyInAnyOrder(IdentityLinkType.STARTER, IdentityLinkType.PARTICIPANT, IdentityLinkType.REACTIVATOR);
                
                List<Task> tasks = cmmnTaskService.createTaskQuery().caseInstanceId(reactivatedCase.getId()).list();
                assertThat(tasks).hasSize(1);
                assertThat(tasks.get(0).getName()).isEqualTo("Task C");
            }
            
        } finally {
            Authentication.setAuthenticatedUserId(previousUserId);
        }
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/reactivation/Simple_Reactivation_Test_Case.cmmn.xml")
    public void simpleCaseReactivationVariableTest() {
        String previousUserId = Authentication.getAuthenticatedUserId();
        try {
            Authentication.setAuthenticatedUserId("simpleCaseReactivationVariableTest_user");
            final HistoricCaseInstance historicCase = createAndFinishSimpleCase("simpleReactivationTestCase");

            CaseInstance reactivatedCase = cmmnHistoryService.createCaseReactivationBuilder(historicCase.getId())
                .variable("varA", "varAValue")
                .transientVariable("varB", "varBValue")
                .reactivate();
            assertThat(reactivatedCase).isNotNull();

            HistoricCaseInstance historicCaseInstance = cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceId(historicCase.getId()).singleResult();
            assertThat(historicCaseInstance).isNotNull();
            assertThat(historicCaseInstance.getState()).isEqualTo(reactivatedCase.getState());
            assertThat(historicCaseInstance.getEndTime()).isNull();

            Map<String, VariableInstance> reactivatedVars = cmmnRuntimeService.getVariableInstances(reactivatedCase.getId());
            assertThat(reactivatedVars).isNotNull().hasSize(4);
            assertVariableValue(reactivatedVars, "varA", "varAValue");
            assertThat(reactivatedVars).doesNotContainKey("varB");
            
            HistoricVariableInstance historicVariableInstance = cmmnHistoryService.createHistoricVariableInstanceQuery().caseInstanceId(reactivatedCase.getId()).variableName("varA").singleResult();
            assertThat(historicVariableInstance).isNotNull();
            assertThat(historicVariableInstance.getScopeId()).isEqualTo(reactivatedCase.getId());
            
            VariableInstance variableInstance = cmmnRuntimeService.getVariableInstance(reactivatedCase.getId(), "varA");
            assertThat(variableInstance.getId()).isEqualTo(historicVariableInstance.getId());

        } finally {
            Authentication.setAuthenticatedUserId(previousUserId);
        }
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/reactivation/Reactivation_Test_Case.cmmn.xml")
    public void simpleCaseReactivationTransientVariableTest() {
        String previousUserId = Authentication.getAuthenticatedUserId();
        try {
            Authentication.setAuthenticatedUserId("simpleCaseReactivationTransientVariableTest_user");
            final HistoricCaseInstance historicCase = createAndFinishSimpleCase("reactivationTestCase");

            CaseInstance reactivatedCase = cmmnHistoryService.createCaseReactivationBuilder(historicCase.getId())
                .transientVariable("tempVar", "tempVarValue")
                .transientVariable("tempIntVar", 100)
                .reactivate();
            assertThat(reactivatedCase).isNotNull();

            List<HistoricVariableInstance> vars = cmmnEngineConfiguration.getCmmnHistoryService().createHistoricVariableInstanceQuery()
                .caseInstanceId(reactivatedCase.getId())
                .list();
            assertThat(vars).isNotNull().hasSize(5);
            assertHistoricVariableValue(vars, "testExpression", 1000L);
            assertHistoricVariableValue(vars, "testValue", "tempVarValue");
            assertHistoricVariableNotExisting(vars, "tempVar");

            assertCaseInstanceEnded(reactivatedCase);
        } finally {
            Authentication.setAuthenticatedUserId(previousUserId);
        }
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/reactivation/Simple_Reactivation_Test_Case.cmmn.xml")
    public void simpleCaseReactivationIdentityLinkTest() {
        String previousUserId = Authentication.getAuthenticatedUserId();
        try {
            Authentication.setAuthenticatedUserId("simpleCaseReactivationVariableTest_user");
            final HistoricCaseInstance historicCase = createAndFinishSimpleCase("simpleReactivationTestCase");

            List<HistoricIdentityLink> historicIdentityLinks = cmmnHistoryService.getHistoricIdentityLinksForCaseInstance(historicCase.getId());
            assertThat(historicIdentityLinks).hasSize(2);
            assertThat(historicIdentityLinks)
                .extracting(IdentityLinkInfo::getType)
                .containsExactlyInAnyOrder(IdentityLinkType.STARTER, IdentityLinkType.PARTICIPANT);

            CaseInstance reactivatedCase = cmmnHistoryService.createCaseReactivationBuilder(historicCase.getId()).reactivate();
            assertThat(reactivatedCase).isNotNull();

            List<IdentityLink> identityLinks = cmmnRuntimeService.getIdentityLinksForCaseInstance(reactivatedCase.getId());
            assertThat(identityLinks).hasSize(3);
            assertThat(identityLinks)
                .extracting(IdentityLinkInfo::getType)
                .containsExactlyInAnyOrder(IdentityLinkType.STARTER, IdentityLinkType.PARTICIPANT, IdentityLinkType.REACTIVATOR);
            
            Map<String, HistoricIdentityLink> historicIdentityLinkMap = new HashMap<>();
            for (HistoricIdentityLink historicIdentityLink : historicIdentityLinks) {
                historicIdentityLinkMap.put(((HistoricIdentityLinkEntity) historicIdentityLink).getId(), historicIdentityLink);
            }
            
            for (IdentityLink identityLink : identityLinks) {
                if (!IdentityLinkType.REACTIVATOR.equals(identityLink.getType())) {
                    String identityLinkId = ((IdentityLinkEntity) identityLink).getId();
                    assertThat(historicIdentityLinkMap.containsKey(identityLinkId)).isTrue();
                    
                    HistoricIdentityLink historicIdentityLink = historicIdentityLinkMap.get(identityLinkId);
                    assertThat(historicIdentityLink.getScopeId()).isEqualTo(identityLink.getScopeId());
                    assertThat(historicIdentityLink.getScopeDefinitionId()).isEqualTo(identityLink.getScopeDefinitionId());
                    assertThat(historicIdentityLink.getType()).isEqualTo(identityLink.getType());
                    assertThat(historicIdentityLink.getUserId()).isEqualTo(identityLink.getUserId());
                }
            }
            
        } finally {
            Authentication.setAuthenticatedUserId(previousUserId);
        }
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/reactivation/Simple_Reactivation_Test_Case.cmmn.xml")
    public void simpleCaseReactivationWithoutAuthenticatedUser() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("simpleReactivationTestCase")
                .variable("foo", "fooValue")
                .variable("bar", "barValue")
                .start();

        String caseInstanceId = caseInstance.getId();

        cmmnRuntimeService.addUserIdentityLink(caseInstanceId, "kermit", IdentityLinkType.ASSIGNEE);
        cmmnRuntimeService.addGroupIdentityLink(caseInstanceId, "frogs", IdentityLinkType.OWNER);

        List<PlanItemInstance> planItemInstances = getPlanItemInstances(caseInstanceId);
        assertPlanItemInstanceState(planItemInstances, "Task A", ACTIVE);
        cmmnRuntimeService.triggerPlanItemInstance(getPlanItemInstanceIdByName(planItemInstances, "Task A"));

        planItemInstances = getPlanItemInstances(caseInstanceId);
        assertPlanItemInstanceState(planItemInstances, "Task B", ACTIVE);
        cmmnRuntimeService.triggerPlanItemInstance(getPlanItemInstanceIdByName(planItemInstances, "Task B"));

        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceId(caseInstanceId).singleResult()).isNull();

        List<HistoricIdentityLink> historicIdentityLinks = cmmnHistoryService.getHistoricIdentityLinksForCaseInstance(caseInstanceId);
        assertThat(historicIdentityLinks)
                .extracting(IdentityLinkInfo::getUserId, IdentityLinkInfo::getGroupId, IdentityLinkInfo::getType)
                .containsExactlyInAnyOrder(
                        tuple("kermit", null, IdentityLinkType.ASSIGNEE),
                        tuple(null, "frogs", IdentityLinkType.OWNER)
                );

        CaseInstance reactivatedCase = cmmnHistoryService.createCaseReactivationBuilder(caseInstanceId).reactivate();
        assertThat(reactivatedCase).isNotNull();

        assertThat(cmmnRuntimeService.getIdentityLinksForCaseInstance(reactivatedCase.getId()))
                .extracting(IdentityLinkInfo::getUserId, IdentityLinkInfo::getGroupId, IdentityLinkInfo::getType)
                .containsExactlyInAnyOrder(
                        tuple("kermit", null, IdentityLinkType.ASSIGNEE),
                        tuple(null, "frogs", IdentityLinkType.OWNER)
                );
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
