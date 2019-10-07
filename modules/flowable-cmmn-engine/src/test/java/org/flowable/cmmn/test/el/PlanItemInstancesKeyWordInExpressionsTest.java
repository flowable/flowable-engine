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
package org.flowable.cmmn.test.el;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

import java.util.List;

import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.api.runtime.PlanItemDefinitionType;
import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.flowable.cmmn.engine.impl.persistence.entity.CaseInstanceEntity;
import org.flowable.cmmn.engine.impl.persistence.entity.PlanItemInstanceEntity;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.engine.test.FlowableCmmnTestCase;
import org.flowable.common.engine.api.delegate.Expression;
import org.flowable.common.engine.impl.el.ExpressionManager;
import org.flowable.common.engine.impl.interceptor.Command;
import org.junit.Test;

/**
 * @author Joram Barrez
 */
public class PlanItemInstancesKeyWordInExpressionsTest extends FlowableCmmnTestCase {

    @Test
    @CmmnDeployment
    public void testWithState() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
            .caseDefinitionKey("testPlanItemInstancesKeyWord").start();

        assertEquals(8, evaluateExpression(caseInstance.getId(), "${planItemInstances.count()}"));
        assertEquals(4, evaluateExpression(caseInstance.getId(), "${planItemInstances.active().count()}"));
        assertEquals(1, evaluateExpression(caseInstance.getId(), "${planItemInstances.available().count()}"));
        assertEquals(3, evaluateExpression(caseInstance.getId(), "${planItemInstances.enabled().count()}"));
        assertEquals(0, evaluateExpression(caseInstance.getId(), "${planItemInstances.completed().count()}"));
        assertEquals(0, evaluateExpression(caseInstance.getId(), "${planItemInstances.suspended().count()}"));
        assertEquals(0, evaluateExpression(caseInstance.getId(), "${planItemInstances.terminated().count()}"));
        assertEquals(0, evaluateExpression(caseInstance.getId(), "${planItemInstances.waitingForRepetition().count()}"));
        assertEquals(0, evaluateExpression(caseInstance.getId(), "${planItemInstances.unavailable().count()}"));
        assertEquals(0, evaluateExpression(caseInstance.getId(), "${planItemInstances.asyncActive().count()}"));

        cmmnRuntimeService.startPlanItemInstance(cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceName("E").singleResult().getId());

        assertEquals(8, evaluateExpression(caseInstance.getId(), "${planItemInstances.count()}"));
        assertEquals(5, evaluateExpression(caseInstance.getId(), "${planItemInstances.active().count()}"));
        assertEquals(1, evaluateExpression(caseInstance.getId(), "${planItemInstances.available().count()}"));
        assertEquals(2, evaluateExpression(caseInstance.getId(), "${planItemInstances.enabled().count()}"));
        assertEquals(0, evaluateExpression(caseInstance.getId(), "${planItemInstances.completed().count()}"));
        assertEquals(0, evaluateExpression(caseInstance.getId(), "${planItemInstances.terminated().count()}"));

        cmmnTaskService.complete(cmmnTaskService.createTaskQuery().taskName("F").singleResult().getId());

        assertEquals(10, evaluateExpression(caseInstance.getId(), "${planItemInstances.count()}"));
        assertEquals(7, evaluateExpression(caseInstance.getId(), "${planItemInstances.active().count()}"));
        assertEquals(0, evaluateExpression(caseInstance.getId(), "${planItemInstances.available().count()}"));
        assertEquals(2, evaluateExpression(caseInstance.getId(), "${planItemInstances.enabled().count()}"));
        assertEquals(1, evaluateExpression(caseInstance.getId(), "${planItemInstances.completed().count()}"));
        assertEquals(0, evaluateExpression(caseInstance.getId(), "${planItemInstances.terminated().count()}"));

        cmmnTaskService.complete(cmmnTaskService.createTaskQuery().taskName("E").singleResult().getId());

        assertEquals(10, evaluateExpression(caseInstance.getId(), "${planItemInstances.count()}"));
        assertEquals(2, evaluateExpression(caseInstance.getId(), "${planItemInstances.active().count()}"));
        assertEquals(0, evaluateExpression(caseInstance.getId(), "${planItemInstances.available().count()}"));
        assertEquals(2, evaluateExpression(caseInstance.getId(), "${planItemInstances.enabled().count()}"));
        assertEquals(2, evaluateExpression(caseInstance.getId(), "${planItemInstances.completed().count()}"));
        assertEquals(4, evaluateExpression(caseInstance.getId(), "${planItemInstances.terminated().count()}"));

        PlanItemInstanceEntity planItemInstanceEntity = (PlanItemInstanceEntity) cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceName("A").singleResult();
        assertEquals(10, evaluateExpression(planItemInstanceEntity, "${planItemInstances.count()}"));
        assertEquals(2, evaluateExpression(planItemInstanceEntity, "${planItemInstances.active().count()}"));
        assertEquals(0, evaluateExpression(planItemInstanceEntity, "${planItemInstances.available().count()}"));
        assertEquals(2, evaluateExpression(planItemInstanceEntity, "${planItemInstances.enabled().count()}"));
        assertEquals(2, evaluateExpression(planItemInstanceEntity, "${planItemInstances.completed().count()}"));
        assertEquals(4, evaluateExpression(planItemInstanceEntity, "${planItemInstances.terminated().count()}"));
    }

    @Test
    @CmmnDeployment
    public void testWithTerminalOrNonTerminal() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
            .caseDefinitionKey("testPlanItemInstancesKeyWord").start();

        assertEquals(8, evaluateExpression(caseInstance.getId(), "${planItemInstances.onlyNonTerminal().count()}"));
        assertEquals(0, evaluateExpression(caseInstance.getId(), "${planItemInstances.onlyTerminal().count()}"));

        assertEquals(4, evaluateExpression(caseInstance.getId(), "${planItemInstances.onlyNonTerminal().active().count()}"));

        cmmnTaskService.complete(cmmnTaskService.createTaskQuery().taskName("F").singleResult().getId());
        assertEquals(9, evaluateExpression(caseInstance.getId(), "${planItemInstances.onlyNonTerminal().count()}"));
        assertEquals(1, evaluateExpression(caseInstance.getId(), "${planItemInstances.onlyTerminal().count()}"));

        cmmnRuntimeService.startPlanItemInstance(cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceName("E").singleResult().getId());
        cmmnTaskService.complete(cmmnTaskService.createTaskQuery().taskName("E").singleResult().getId());

        assertEquals(4, evaluateExpression(caseInstance.getId(), "${planItemInstances.onlyNonTerminal().count()}"));
        assertEquals(6, evaluateExpression(caseInstance.getId(), "${planItemInstances.onlyTerminal().count()}"));

        assertEquals(2, evaluateExpression(caseInstance.getId(), "${planItemInstances.onlyTerminal().completed().count()}"));
        assertEquals(2, evaluateExpression(caseInstance.getId(), "${planItemInstances.completed().onlyTerminal().count()}"));
        assertEquals(4, evaluateExpression(caseInstance.getId(), "${planItemInstances.terminated().onlyTerminal().count()}"));
    }

    @Test
    @CmmnDeployment
    public void testWithId() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
            .caseDefinitionKey("testPlanItemInstancesKeyWord").start();

        assertEquals(1, evaluateExpression(caseInstance.getId(), "${planItemInstances.definitionId('a').count()}"));
        assertEquals(2, evaluateExpression(caseInstance.getId(), "${planItemInstances.definitionId('a', 'b').count()}"));
        assertEquals(2, evaluateExpression(caseInstance.getId(), "${planItemInstances.definitionIds('a', 'b').count()}"));
        assertEquals(3, evaluateExpression(caseInstance.getId(), "${planItemInstances.definitionId('a', 'b', 'stage1').count()}"));

        assertEquals(0, evaluateExpression(caseInstance.getId(), "${planItemInstances.definitionId('invalid').count()}"));
        assertEquals(1, evaluateExpression(caseInstance.getId(), "${planItemInstances.definitionId('invalid', 'a').count()}"));
    }

    @Test
    @CmmnDeployment
    public void testWithName() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
            .caseDefinitionKey("testPlanItemInstancesKeyWord").start();

        assertEquals(1, evaluateExpression(caseInstance.getId(), "${planItemInstances.name('A').count()}"));
        assertEquals(2, evaluateExpression(caseInstance.getId(), "${planItemInstances.name('A', 'B').count()}"));
        assertEquals(2, evaluateExpression(caseInstance.getId(), "${planItemInstances.names('A', 'B').count()}"));
        assertEquals(3, evaluateExpression(caseInstance.getId(), "${planItemInstances.name('A', 'B', 'Stage1').count()}"));

        assertEquals(0, evaluateExpression(caseInstance.getId(), "${planItemInstances.name('invalid').count()}"));
        assertEquals(1, evaluateExpression(caseInstance.getId(), "${planItemInstances.name('invalid', 'A').count()}"));
    }

    @Test
    @CmmnDeployment
    public void testWithStateAndId() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
            .caseDefinitionKey("testPlanItemInstancesKeyWord").start();


        assertEquals(1, evaluateExpression(caseInstance.getId(), "${planItemInstances.definitionId('a').active().count()}"));
        assertEquals(0, evaluateExpression(caseInstance.getId(), "${planItemInstances.definitionId('a').enabled().count()}"));

        assertEquals(1, evaluateExpression(caseInstance.getId(), "${planItemInstances.active().definitionId('a').count()}"));
        assertEquals(0, evaluateExpression(caseInstance.getId(), "${planItemInstances.enabled().definitionId('a').count()}"));
    }

    @Test
    @CmmnDeployment
    public void testWithStage() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
            .caseDefinitionKey("testPlanItemInstancesKeyWord").start();

        PlanItemInstanceEntity planItemInstanceEntity = (PlanItemInstanceEntity) cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceName("F").singleResult();
        assertEquals(2, evaluateExpression(planItemInstanceEntity, "${planItemInstances.currentStage().count()}"));
        assertEquals(1, evaluateExpression(planItemInstanceEntity, "${planItemInstances.currentStage().active().count()}"));
        assertEquals(1, evaluateExpression(planItemInstanceEntity, "${planItemInstances.currentStage().available().count()}"));
        assertEquals(0, evaluateExpression(planItemInstanceEntity, "${planItemInstances.currentStage().suspended().count()}"));
        assertEquals(0, evaluateExpression(planItemInstanceEntity, "${planItemInstances.currentStage().terminated().count()}"));
        assertEquals(0, evaluateExpression(planItemInstanceEntity, "${planItemInstances.currentStage().waitingForRepetition().count()}"));
        assertEquals(0, evaluateExpression(planItemInstanceEntity, "${planItemInstances.currentStage().unavailable().count()}"));
        assertEquals(0, evaluateExpression(planItemInstanceEntity, "${planItemInstances.currentStage().asyncActive().count()}"));

        assertEquals(1, evaluateExpression(planItemInstanceEntity, "${planItemInstances.currentStage().definitionId('f').active().count()}"));
        assertEquals(0, evaluateExpression(planItemInstanceEntity, "${planItemInstances.currentStage().definitionId('g').active().count()}"));

        cmmnTaskService.complete(cmmnTaskService.createTaskQuery().taskName("F").singleResult().getId());
        cmmnTaskService.complete(cmmnTaskService.createTaskQuery().taskName("G").singleResult().getId());
        planItemInstanceEntity = (PlanItemInstanceEntity) cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceName("H").singleResult();
        assertEquals(2, evaluateExpression(planItemInstanceEntity, "${planItemInstances.currentStage().count()}"));
        assertEquals(1, evaluateExpression(planItemInstanceEntity, "${planItemInstances.currentStage().active().count()}"));
        assertEquals(0, evaluateExpression(planItemInstanceEntity, "${planItemInstances.currentStage().available().count()}"));
        assertEquals(1, evaluateExpression(planItemInstanceEntity, "${planItemInstances.currentStage().completed().count()}"));
        assertEquals(0, evaluateExpression(planItemInstanceEntity, "${planItemInstances.currentStage().suspended().count()}"));
        assertEquals(0, evaluateExpression(planItemInstanceEntity, "${planItemInstances.currentStage().terminated().count()}"));
        assertEquals(0, evaluateExpression(planItemInstanceEntity, "${planItemInstances.currentStage().waitingForRepetition().count()}"));
        assertEquals(0, evaluateExpression(planItemInstanceEntity, "${planItemInstances.currentStage().unavailable().count()}"));
        assertEquals(0, evaluateExpression(planItemInstanceEntity, "${planItemInstances.currentStage().asyncActive().count()}"));

        assertEquals(1, evaluateExpression(planItemInstanceEntity, "${planItemInstances.currentStage().onlyTerminal().count()}"));
        assertEquals(1, evaluateExpression(planItemInstanceEntity, "${planItemInstances.currentStage().onlyTerminal().completed().count()}"));
        assertEquals(0, evaluateExpression(planItemInstanceEntity, "${planItemInstances.currentStage().onlyTerminal().terminated().count()}"));
    }

    @Test
    @CmmnDeployment
    public void testReturnIdsAndNames() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
            .caseDefinitionKey("testPlanItemInstancesKeyWord").start();

        List<String> ids = (List<String>) evaluateExpression(caseInstance.getId(), "${planItemInstances.active().getDefinitionIds()}");
        assertThat(ids).contains("a", "b", "stage1", "f");

        ids = (List<String>) evaluateExpression(caseInstance.getId(), "${planItemInstances.active().getDefinitionId()}");
        assertThat(ids).contains("a", "b", "stage1", "f");

        List<String> names = (List<String>) evaluateExpression(caseInstance.getId(), "${planItemInstances.active().getDefinitionNames()}");
        assertThat(names).contains("A", "B", "Stage1", "F");

        names = (List<String>) evaluateExpression(caseInstance.getId(), "${planItemInstances.active().getDefinitionName()}");
        assertThat(names).contains("A", "B", "Stage1", "F");

        names = (List<String>) evaluateExpression(caseInstance.getId(), "${planItemInstances.terminated().getDefinitionNames()}");
        assertThat(names).isEmpty();
    }

    @Test
    @CmmnDeployment
    public void testGetList() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
            .caseDefinitionKey("testPlanItemInstancesKeyWord").start();

        assertThat((List<PlanItemInstanceEntity>) evaluateExpression(caseInstance.getId(), "${planItemInstances.active().getList()}"))
            .extracting(planItemInstance -> planItemInstance.getPlanItem().getPlanItemDefinition().getName())
            .contains("A", "B", "F", "Stage1");

        assertThat((List<PlanItemInstanceEntity>) evaluateExpression(caseInstance.getId(), "${planItemInstances.enabled().getList()}"))
            .extracting(planItemInstance -> planItemInstance.getPlanItem().getPlanItemDefinition().getName())
            .contains("C", "D", "E");

        assertThat((List<PlanItemInstanceEntity>) evaluateExpression(caseInstance.getId(), "${planItemInstances.completed().getList()}"))
            .extracting(planItemInstance -> planItemInstance.getPlanItem().getPlanItemDefinition().getName()).isEmpty();

        PlanItemInstanceEntity planItemInstanceEntity = (PlanItemInstanceEntity) cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceName("F").singleResult();
        assertThat((List<PlanItemInstanceEntity>) evaluateExpression(planItemInstanceEntity, "${planItemInstances.currentStage().getList()}"))
            .extracting(planItemInstance -> planItemInstance.getPlanItem().getPlanItemDefinition().getName())
            .contains("F", "Stage2");

        assertThat((List<PlanItemInstanceEntity>) evaluateExpression(planItemInstanceEntity, "${planItemInstances.currentStage().active().getList()}"))
            .extracting(planItemInstance -> planItemInstance.getPlanItem().getPlanItemDefinition().getName())
            .contains("F");

        assertThat((List<PlanItemInstanceEntity>) evaluateExpression(planItemInstanceEntity, "${planItemInstances.currentStage().definitionId('stage2').getList()}"))
            .extracting(planItemInstance -> planItemInstance.getPlanItem().getPlanItemDefinition().getName())
            .contains("Stage2");

        assertThat((List<PlanItemInstanceEntity>) evaluateExpression(planItemInstanceEntity, "${planItemInstances.currentStage().name('Stage2').getList()}"))
            .extracting(planItemInstance -> planItemInstance.getPlanItem().getPlanItemDefinition().getName())
            .contains("Stage2");

        assertThat((List<PlanItemInstanceEntity>) evaluateExpression(planItemInstanceEntity, "${planItemInstances.currentStage().definitionId('Stage2').active().getList()}"))
            .extracting(planItemInstance -> planItemInstance.getPlanItem().getPlanItemDefinition().getName()).isEmpty();
    }

    @Test
    @CmmnDeployment
    public void testUseInSentryCondition() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
            .caseDefinitionKey("testPlanItemInstancesKeyWord").start();

        assertEquals(0, cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceStateActive().count());

        PlanItemInstance planItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceStateEnabled().singleResult();
        assertEquals("A", planItemInstance.getName());

        cmmnRuntimeService.startPlanItemInstance(planItemInstance.getId());

        // B gets activated through expression
        assertEquals(2, cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceStateActive().count());
    }

    @Test
    @CmmnDeployment
    public void testStoreCountInVariable() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
            .caseDefinitionKey("testPlanItemInstancesKeyWord").start();

        PlanItemInstance planItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceStateEnabled().singleResult();
        cmmnRuntimeService.startPlanItemInstance(planItemInstance.getId());

        assertThat(cmmnRuntimeService.getVariable(caseInstance.getId(), "myVar")).isEqualTo(2);
    }

    @Test
    @CmmnDeployment
    public void testUsageInAvailableCondition() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
            .caseDefinitionKey("testPlanItemInstancesKeyWord").start();

        // The user event listeners should not be active after start
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionType(PlanItemDefinitionType.USER_EVENT_LISTENER).planItemInstanceStateAvailable().count()).isEqualTo(0);

        // Starting the human tasks should make the user event listeners available
        cmmnRuntimeService.startPlanItemInstance(cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceStateEnabled().planItemInstanceName("Human task 1").singleResult().getId());
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionType(PlanItemDefinitionType.USER_EVENT_LISTENER).planItemInstanceStateAvailable().count()).isEqualTo(1);

        cmmnRuntimeService.startPlanItemInstance(cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceStateEnabled().planItemInstanceName("Human task 2").singleResult().getId());
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionType(PlanItemDefinitionType.USER_EVENT_LISTENER).planItemInstanceStateAvailable().count()).isEqualTo(2);

        // Triggering an event listener should make the tasks again enabled (as they are manual activation)
        cmmnRuntimeService.completeUserEventListenerInstance(cmmnRuntimeService.createUserEventListenerInstanceQuery().name("cancel 1").singleResult().getId());
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionType(PlanItemDefinitionType.USER_EVENT_LISTENER)
            .planItemInstanceStateAvailable().list()).extracting(PlanItemInstance::getName)
            .contains("cancel 2");

        cmmnRuntimeService.completeUserEventListenerInstance(cmmnRuntimeService.createUserEventListenerInstanceQuery().name("cancel 2").singleResult().getId());
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionType(PlanItemDefinitionType.USER_EVENT_LISTENER).planItemInstanceStateAvailable().count()).isEqualTo(0);


    }


    private Object evaluateExpression(String caseInstanceId, String expressionText) {
        return cmmnEngineConfiguration.getCommandExecutor().execute((Command<Object>) commandContext -> {
            CaseInstanceEntity caseInstanceEntity = CommandContextUtil.getCaseInstanceEntityManager(commandContext).findById(caseInstanceId);
            ExpressionManager expressionManager = CommandContextUtil.getCmmnEngineConfiguration(commandContext).getExpressionManager();
            Expression expression = expressionManager.createExpression(expressionText);
            return expression.getValue(caseInstanceEntity);
        });
    }

    private Object evaluateExpression(PlanItemInstanceEntity planItemInstanceEntity, String expressionText) {
        return cmmnEngineConfiguration.getCommandExecutor().execute((Command<Object>) commandContext -> {
            ExpressionManager expressionManager = CommandContextUtil.getCmmnEngineConfiguration(commandContext).getExpressionManager();
            Expression expression = expressionManager.createExpression(expressionText);
            return expression.getValue(planItemInstanceEntity);
        });
    }

}
