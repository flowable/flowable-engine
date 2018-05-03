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
package org.flowable.cmmn.test.task;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.flowable.cmmn.api.history.HistoricCaseInstance;
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.engine.test.FlowableCmmnTestCase;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.identitylink.service.IdentityLinkType;
import org.flowable.identitylink.service.impl.persistence.entity.IdentityLinkEntityImpl;
import org.flowable.task.api.Task;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * @author Joram Barrez
 */
public class CmmnTaskServiceTest extends FlowableCmmnTestCase {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
    @CmmnDeployment
    public void testOneHumanTaskCase() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("oneHumanTaskCase").start();
        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertNotNull(task);
        assertEquals("The Task", task.getName());
        assertEquals("This is a test documentation", task.getDescription());
        assertEquals("johnDoe", task.getAssignee());

        if (cmmnEngineConfiguration.getHistoryLevel().isAtLeast(HistoryLevel.ACTIVITY)) {
            HistoricTaskInstance historicTaskInstance = cmmnHistoryService.createHistoricTaskInstanceQuery().caseInstanceId(caseInstance.getId()).singleResult();
            assertNotNull(historicTaskInstance);
            assertNull(historicTaskInstance.getEndTime());
        }

        cmmnTaskService.complete(task.getId());
        assertCaseInstanceEnded(caseInstance);

        if (cmmnEngineConfiguration.getHistoryLevel().isAtLeast(HistoryLevel.ACTIVITY)) {
            HistoricTaskInstance historicTaskInstance = cmmnHistoryService.createHistoricTaskInstanceQuery().caseInstanceId(caseInstance.getId()).singleResult();
            assertNotNull(historicTaskInstance);
            assertEquals("The Task", historicTaskInstance.getName());
            assertEquals("This is a test documentation", historicTaskInstance.getDescription());
            assertNotNull(historicTaskInstance.getEndTime());
        }
    }

    @Test
    @CmmnDeployment
    public void testOneHumanTaskExpressionCase() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                        .caseDefinitionKey("oneHumanTaskCase")
                        .variable("var1", "A")
                        .variable("var2", "YES")
                        .start();
        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertNotNull(task);
        assertEquals("The Task A", task.getName());
        assertEquals("This is a test YES", task.getDescription());
        assertEquals("johnDoe", task.getAssignee());
        
        cmmnTaskService.complete(task.getId());
        assertCaseInstanceEnded(caseInstance);
        
        if (cmmnEngineConfiguration.getHistoryLevel().isAtLeast(HistoryLevel.ACTIVITY)) {
            HistoricTaskInstance historicTaskInstance = cmmnHistoryService.createHistoricTaskInstanceQuery().caseInstanceId(caseInstance.getId()).singleResult();
            assertNotNull(historicTaskInstance);
            assertEquals("The Task A", historicTaskInstance.getName());
            assertEquals("This is a test YES", historicTaskInstance.getDescription());
            assertNotNull(historicTaskInstance.getEndTime());
        }
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/task/CmmnTaskServiceTest.testOneHumanTaskCase.cmmn")
    public void testOneHumanTaskVariableScopeExpressionCase() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("oneHumanTaskCase").start();
        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();

        this.expectedException.expect(FlowableException.class);
        this.expectedException.expectMessage("Error while evaluating expression: ${caseInstance.name}");
        cmmnTaskService.complete(task.getId(), Collections.singletonMap(
                "${caseInstance.name}", "newCaseName"
            )
        );
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/task/CmmnTaskServiceTest.testOneHumanTaskCase.cmmn")
    public void testOneHumanTaskCompleteSetCaseName() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("oneHumanTaskCase").start();
        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();

        this.expectedException.expect(FlowableException.class);
        this.expectedException.expectMessage("Error while evaluating expression: ${name}");
        cmmnTaskService.complete(task.getId(), Collections.singletonMap(
                "${name}", "newCaseName"
            )
        );
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/task/CmmnTaskServiceTest.testOneHumanTaskCase.cmmn")
    public void testOneHumanTaskCaseScopeExpression() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                        .caseDefinitionKey("oneHumanTaskCase")
                        .start();
        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        cmmnTaskService.setVariable(task.getId(), "variableToUpdate", "VariableValue");

        cmmnTaskService.complete(task.getId(), Collections.singletonMap(
                "${variableToUpdate}", "updatedVariableValue"
            )
        );
        HistoricCaseInstance historicCaseInstance = cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceId(caseInstance.getId()).
                includeCaseVariables().singleResult();
        assertThat(historicCaseInstance.getCaseVariables().get("variableToUpdate"), is("updatedVariableValue"));
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/task/CmmnTaskServiceTest.testOneHumanTaskCase.cmmn")
    public void testOneHumanTaskTaskScopeExpression() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                        .caseDefinitionKey("oneHumanTaskCase")
                        .start();
        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        cmmnTaskService.setVariableLocal(task.getId(), "variableToUpdate", "VariableValue");

        cmmnTaskService.complete(task.getId(), Collections.singletonMap(
                "${variableToUpdate}", "updatedVariableValue"
            )
        );
        HistoricTaskInstance historicTaskInstance = cmmnHistoryService.createHistoricTaskInstanceQuery().caseInstanceId(caseInstance.getId()).
                includeTaskLocalVariables().singleResult();
        assertThat(historicTaskInstance.getTaskLocalVariables().get("variableToUpdate"), is("updatedVariableValue"));
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/task/CmmnTaskServiceTest.testOneHumanTaskCase.cmmn")
    public void testSetCaseNameByExpression() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .variable("varToUpdate", "initialValue")
                .caseDefinitionKey("oneHumanTaskCase")
                .start();

        cmmnRuntimeService.setVariable(caseInstance.getId(), "${varToUpdate}", "newValue");

        CaseInstance updatedCaseInstance = cmmnRuntimeService.createCaseInstanceQuery().
                caseInstanceId(caseInstance.getId()).
                includeCaseVariables().
                singleResult();
        assertThat(updatedCaseInstance.getCaseVariables().get("varToUpdate"), is("newValue"));
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/task/CmmnTaskServiceTest.testOneHumanTaskCase.cmmn")
    public void setCaseInstanceLocalVariableOnRootCase() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .variable("varToUpdate", "initialValue")
                .caseDefinitionKey("oneHumanTaskCase")
                .start();

        cmmnRuntimeService.setCaseInstanceLocalVariable(caseInstance.getId(), "varToUpdate", "newValue");

        CaseInstance updatedCaseInstance = cmmnRuntimeService.createCaseInstanceQuery().
                caseInstanceId(caseInstance.getId()).
                includeCaseVariables().
                singleResult();
        assertThat(updatedCaseInstance.getCaseVariables().get("varToUpdate"), is("newValue"));
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/task/CmmnTaskServiceTest.testOneHumanTaskCase.cmmn")
    public void setCaseInstanceLocalVariableOnNonExistingCase() {
        this.expectedException.expect(FlowableObjectNotFoundException.class);
        this.expectedException.expectMessage("No case instance found for id NON-EXISTING-CASE");

        cmmnRuntimeService.setCaseInstanceLocalVariable("NON-EXISTING-CASE", "varToUpdate", "newValue");
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/task/CmmnTaskServiceTest.testOneHumanTaskCase.cmmn")
    public void setCaseInstanceLocalVariableWithoutName() {
        this.expectedException.expect(FlowableIllegalArgumentException.class);
        this.expectedException.expectMessage("variable name is null");

        cmmnRuntimeService.setCaseInstanceLocalVariable("NON-EXISTING-CASE", null, "newValue");
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/task/CmmnTaskServiceTest.testOneHumanTaskCase.cmmn")
    public void setCaseInstanceLocalVariableOnRootCaseWithExpression() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .variable("varToUpdate", "initialValue")
                .caseDefinitionKey("oneHumanTaskCase")
                .start();

        cmmnRuntimeService.setCaseInstanceLocalVariable(caseInstance.getId(), "${varToUpdate}", "newValue");

        CaseInstance updatedCaseInstance = cmmnRuntimeService.createCaseInstanceQuery().
                caseInstanceId(caseInstance.getId()).
                includeCaseVariables().
                singleResult();
        assertThat("resolving variable name expressions does not make sense when it is set locally",
                updatedCaseInstance.getCaseVariables().get("${varToUpdate}"), is("newValue"));
    }

    @Test
    @CmmnDeployment(resources = {
            "org/flowable/cmmn/test/task/CmmnTaskServiceTest.testOneHumanTaskCase.cmmn",
            "org/flowable/cmmn/test/task/CmmnTaskServiceTest.rootProcess.cmmn"
    })
    public void setCaseInstanceLocalVariableOnSubCase() {
        cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("rootCase")
                .start();
        CaseInstance subCaseInstance = cmmnRuntimeService.createCaseInstanceQuery().caseDefinitionKey("oneHumanTaskCase").singleResult();

        cmmnRuntimeService.setCaseInstanceLocalVariable(subCaseInstance.getId(), "varToUpdate", "newValue");

        CaseInstance updatedCaseInstance = cmmnRuntimeService.createCaseInstanceQuery().
                caseInstanceId(subCaseInstance.getId()).
                includeCaseVariables().
                singleResult();
        assertThat(updatedCaseInstance.getCaseVariables().get("varToUpdate"), is("newValue"));
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/task/CmmnTaskServiceTest.testOneHumanTaskCase.cmmn")
    public void setCaseInstanceLocalVariablesOnRootCase() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .variable("varToUpdate", "initialValue")
                .caseDefinitionKey("oneHumanTaskCase")
                .start();
        Map<String, Object> variables = Stream.of( new ImmutablePair<String, Object>("varToUpdate", "newValue")).collect(
                toMap(Pair::getKey, Pair::getValue)
        );
        cmmnRuntimeService.setCaseInstanceLocalVariables(caseInstance.getId(), variables);

        CaseInstance updatedCaseInstance = cmmnRuntimeService.createCaseInstanceQuery().
                caseInstanceId(caseInstance.getId()).
                includeCaseVariables().
                singleResult();
        assertThat(updatedCaseInstance.getCaseVariables(), is(variables));
    }

    @SuppressWarnings("unchecked")
    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/task/CmmnTaskServiceTest.testOneHumanTaskCase.cmmn")
    public void setCaseInstanceLocalVariablesOnNonExistingCase() {
        this.expectedException.expect(FlowableObjectNotFoundException.class);
        this.expectedException.expectMessage("No case  instance found for id NON-EXISTING-CASE");
        Map<String, Object> variables = Stream.of(new ImmutablePair<String, Object>("varToUpdate", "newValue")).collect(
                toMap(Pair::getKey, Pair::getValue)
        );

        cmmnRuntimeService.setCaseInstanceLocalVariables("NON-EXISTING-CASE", variables);
    }

    @SuppressWarnings("unchecked")
    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/task/CmmnTaskServiceTest.testOneHumanTaskCase.cmmn")
    public void setCaseInstanceLocalVariablesWithEmptyMap() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .variable("varToUpdate", "initialValue")
                .caseDefinitionKey("oneHumanTaskCase")
                .start();

        this.expectedException.expect(FlowableIllegalArgumentException.class);
        this.expectedException.expectMessage("variables are empty");

        cmmnRuntimeService.setCaseInstanceLocalVariables(caseInstance.getId(), Collections.EMPTY_MAP);
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/task/CmmnTaskServiceTest.testOneHumanTaskCase.cmmn")
    public void setCaseInstanceLocalVariablesOnRootCaseWithExpression() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .variable("varToUpdate", "initialValue")
                .caseDefinitionKey("oneHumanTaskCase")
                .start();
        Map<String, Object> variables = Stream.of(new ImmutablePair<String, Object>("${varToUpdate}", "newValue")).collect(
                toMap(Pair::getKey, Pair::getValue)
        );

        cmmnRuntimeService.setCaseInstanceLocalVariables(caseInstance.getId(), variables);

        CaseInstance updatedCaseInstance = cmmnRuntimeService.createCaseInstanceQuery().
                caseInstanceId(caseInstance.getId()).
                includeCaseVariables().
                singleResult();
        assertThat("resolving variable name expressions does not make sense when it is set locally",
                updatedCaseInstance.getCaseVariables(), is(
                        Stream.of(
                                new ImmutablePair<String, Object>("${varToUpdate}", "newValue"),
                                new ImmutablePair<String, Object>("varToUpdate", "initialValue")
                        ).collect(
                                toMap(Pair::getKey, Pair::getValue)
                        )
                ));
    }

    @Test
    @CmmnDeployment(resources = {
            "org/flowable/cmmn/test/task/CmmnTaskServiceTest.testOneHumanTaskCase.cmmn",
            "org/flowable/cmmn/test/task/CmmnTaskServiceTest.rootProcess.cmmn"
    })
    public void setCaseInstanceLocalVariablesOnSubCase() {
        cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("rootCase")
                .start();
        CaseInstance subCaseInstance = cmmnRuntimeService.createCaseInstanceQuery().caseDefinitionKey("oneHumanTaskCase").singleResult();
        Map<String, Object> variables = Stream.of(new ImmutablePair<String, Object>("${varToUpdate}", "newValue")).collect(
                toMap(Pair::getKey, Pair::getValue)
        );

        cmmnRuntimeService.setCaseInstanceLocalVariables(subCaseInstance.getId(), variables);

        CaseInstance updatedCaseInstance = cmmnRuntimeService.createCaseInstanceQuery().
                caseInstanceId(subCaseInstance.getId()).
                includeCaseVariables().
                singleResult();
        assertThat(updatedCaseInstance.getCaseVariables(), is(variables));
    }

    @Test
    @CmmnDeployment
    public void testTriggerOneHumanTaskCaseProgrammatically() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("oneHumanTaskCase").start();
        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        
        PlanItemInstance planItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceStateActive().singleResult();
        assertEquals(planItemInstance.getId(), task.getSubScopeId());
        assertEquals(planItemInstance.getCaseInstanceId(), task.getScopeId());
        assertEquals(planItemInstance.getCaseDefinitionId(), task.getScopeDefinitionId());
        assertEquals(ScopeTypes.CMMN, task.getScopeType());
        
        cmmnRuntimeService.triggerPlanItemInstance(planItemInstance.getId());
        assertEquals(0, cmmnTaskService.createTaskQuery().count());
        assertCaseInstanceEnded(caseInstance);
    }

    private static Set<IdentityLinkEntityImpl> getDefaultIdentityLinks() {
        IdentityLinkEntityImpl identityLinkEntityCandidateUser = new IdentityLinkEntityImpl();
        identityLinkEntityCandidateUser.setUserId("testUserFromBuilder");
        identityLinkEntityCandidateUser.setType(IdentityLinkType.CANDIDATE);
        IdentityLinkEntityImpl identityLinkEntityCandidateGroup = new IdentityLinkEntityImpl();
        identityLinkEntityCandidateGroup.setGroupId("testGroupFromBuilder");
        identityLinkEntityCandidateGroup.setType(IdentityLinkType.CANDIDATE);

        return Stream.of(
                identityLinkEntityCandidateUser,
                identityLinkEntityCandidateGroup
        ).collect(toSet());
    }

}
