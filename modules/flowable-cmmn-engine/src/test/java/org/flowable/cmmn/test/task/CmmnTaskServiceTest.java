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

import static java.util.stream.Collectors.toSet;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.flowable.cmmn.api.history.HistoricCaseInstance;
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.flowable.cmmn.engine.interceptor.CreateHumanTaskAfterContext;
import org.flowable.cmmn.engine.interceptor.CreateHumanTaskBeforeContext;
import org.flowable.cmmn.engine.interceptor.CreateHumanTaskInterceptor;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.engine.test.FlowableCmmnTestCase;
import org.flowable.cmmn.engine.test.impl.CmmnHistoryTestHelper;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.common.engine.impl.interceptor.CommandExecutor;
import org.flowable.entitylink.api.EntityLink;
import org.flowable.entitylink.api.EntityLinkService;
import org.flowable.entitylink.api.EntityLinkType;
import org.flowable.entitylink.api.HierarchyType;
import org.flowable.entitylink.api.history.HistoricEntityLink;
import org.flowable.entitylink.api.history.HistoricEntityLinkService;
import org.flowable.identitylink.api.IdentityLinkType;
import org.flowable.identitylink.service.impl.persistence.entity.IdentityLinkEntityImpl;
import org.flowable.task.api.Task;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

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
        assertThat(task)
                .extracting(Task::getName,
                        Task::getDescription,
                        Task::getAssignee)
                .containsExactly("The Task", "This is a test documentation", "johnDoe");

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            HistoricTaskInstance historicTaskInstance = cmmnHistoryService.createHistoricTaskInstanceQuery().caseInstanceId(caseInstance.getId())
                    .singleResult();
            assertThat(historicTaskInstance).isNotNull();
            assertThat(historicTaskInstance.getEndTime()).isNull();
        }

        cmmnTaskService.complete(task.getId());
        assertCaseInstanceEnded(caseInstance);

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            HistoricTaskInstance historicTaskInstance = cmmnHistoryService.createHistoricTaskInstanceQuery().caseInstanceId(caseInstance.getId())
                    .singleResult();
            assertThat(historicTaskInstance)
                    .extracting(HistoricTaskInstance::getName, HistoricTaskInstance::getDescription)
                    .containsExactly("The Task", "This is a test documentation");
            assertThat(historicTaskInstance.getEndTime()).isNotNull();
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
        assertThat(task)
                .extracting(Task::getName,
                        Task::getDescription,
                        Task::getAssignee)
                .containsExactly("The Task A", "This is a test YES", "johnDoe");

        cmmnTaskService.complete(task.getId());
        assertCaseInstanceEnded(caseInstance);

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            HistoricTaskInstance historicTaskInstance = cmmnHistoryService.createHistoricTaskInstanceQuery().caseInstanceId(caseInstance.getId())
                    .singleResult();
            assertThat(historicTaskInstance)
                    .extracting(HistoricTaskInstance::getName, HistoricTaskInstance::getDescription)
                    .containsExactly("The Task A", "This is a test YES");
            assertThat(historicTaskInstance.getEndTime()).isNotNull();
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

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            HistoricCaseInstance historicCaseInstance = cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceId(caseInstance.getId()).
                includeCaseVariables().singleResult();
            assertThat(historicCaseInstance.getCaseVariables()).containsEntry("variableToUpdate", "updatedVariableValue");
        }
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

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            HistoricTaskInstance historicTaskInstance = cmmnHistoryService.createHistoricTaskInstanceQuery().caseInstanceId(caseInstance.getId()).
                includeTaskLocalVariables().singleResult();
            assertThat(historicTaskInstance.getTaskLocalVariables()).containsEntry("variableToUpdate", "updatedVariableValue");
        }
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
        assertThat(updatedCaseInstance.getCaseVariables()).containsEntry("varToUpdate", "newValue");
    }

    @Test
    @CmmnDeployment
    public void testTriggerOneHumanTaskCaseProgrammatically() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("oneHumanTaskCase").start();
        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();

        PlanItemInstance planItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceStateActive().singleResult();
        assertThat(task)
                .extracting(Task::getSubScopeId,
                        Task::getScopeId,
                        Task::getScopeDefinitionId,
                        Task::getScopeType)
                .containsExactly(planItemInstance.getId(),
                        planItemInstance.getCaseInstanceId(),
                        planItemInstance.getCaseDefinitionId(),
                        ScopeTypes.CMMN);

        cmmnRuntimeService.triggerPlanItemInstance(planItemInstance.getId());
        assertThat(cmmnTaskService.createTaskQuery().count()).isZero();
        assertCaseInstanceEnded(caseInstance);
    }

    @Test
    @CmmnDeployment
    public void testEntityLinkCreation() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("entityLinkCreation").start();
        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertThat(task).isNotNull();

        CommandExecutor commandExecutor = cmmnEngine.getCmmnEngineConfiguration().getCommandExecutor();

        List<EntityLink> entityLinks = commandExecutor.execute(commandContext -> {
            EntityLinkService entityLinkService = cmmnEngineConfiguration.getEntityLinkServiceConfiguration().getEntityLinkService();

            return entityLinkService.findEntityLinksByScopeIdAndType(caseInstance.getId(), ScopeTypes.CMMN, EntityLinkType.CHILD);
        });

        assertThat(entityLinks).hasSize(1);
        assertThat(entityLinks.get(0).getHierarchyType()).isEqualTo(HierarchyType.ROOT);

        cmmnTaskService.complete(task.getId());
        assertCaseInstanceEnded(caseInstance);

        List<HistoricEntityLink> entityLinksByScopeIdAndType = commandExecutor.execute(commandContext -> {
            HistoricEntityLinkService historicEntityLinkService = cmmnEngineConfiguration.getEntityLinkServiceConfiguration().getHistoricEntityLinkService();

            return historicEntityLinkService.findHistoricEntityLinksByScopeIdAndScopeType(caseInstance.getId(), ScopeTypes.CMMN, EntityLinkType.CHILD);
        });

        assertThat(entityLinksByScopeIdAndType).hasSize(1);
        assertThat(entityLinksByScopeIdAndType.get(0).getHierarchyType()).isEqualTo(HierarchyType.ROOT);
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/task/CmmnTaskServiceTest.testOneHumanTaskCase.cmmn")
    public void testCreateHumanTaskInterceptor() {
        TestCreateHumanTaskInterceptor testCreateHumanTaskInterceptor = new TestCreateHumanTaskInterceptor();
        cmmnEngineConfiguration.setCreateHumanTaskInterceptor(testCreateHumanTaskInterceptor);

        try {
            CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("oneHumanTaskCase").start();
            Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
            assertThat(task)
                    .extracting(Task::getName,
                            Task::getDescription,
                            Task::getAssignee,
                            Task::getCategory)
                    .containsExactly("The Task", "This is a test documentation", "johnDoe", "testCategory");

            assertThat(testCreateHumanTaskInterceptor.getBeforeCreateHumanTaskCounter()).isEqualTo(1);
            assertThat(testCreateHumanTaskInterceptor.getAfterCreateHumanTaskCounter()).isEqualTo(1);

        } finally {
            cmmnEngineConfiguration.setCreateHumanTaskInterceptor(null);
        }
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

    protected class TestCreateHumanTaskInterceptor implements CreateHumanTaskInterceptor {

        protected int beforeCreateHumanTaskCounter = 0;
        protected int afterCreateHumanTaskCounter = 0;

        @Override
        public void beforeCreateHumanTask(CreateHumanTaskBeforeContext context) {
            beforeCreateHumanTaskCounter++;
            context.setCategory("testCategory");
        }

        @Override
        public void afterCreateHumanTask(CreateHumanTaskAfterContext context) {
            afterCreateHumanTaskCounter++;
        }

        public int getBeforeCreateHumanTaskCounter() {
            return beforeCreateHumanTaskCounter;
        }

        public int getAfterCreateHumanTaskCounter() {
            return afterCreateHumanTaskCounter;
        }
    }

}
