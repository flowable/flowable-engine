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
package org.flowable.cmmn.test.itemcontrol;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.api.runtime.PlanItemDefinitionType;
import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstanceState;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.engine.test.FlowableCmmnTestCase;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.FlowableIllegalStateException;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.util.CollectionUtil;
import org.flowable.task.api.Task;
import org.junit.Test;

/**
 * @author Joram Barrez
 */
public class ManualActivationRuleTest extends FlowableCmmnTestCase {

    @Test
    @CmmnDeployment
    public void testSingleHumanTask() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testManualActivatedHumanTask").start();
        
        PlanItemInstance planItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertThat(planItemInstance.getState()).isEqualTo(PlanItemInstanceState.ENABLED);
        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).count()).isZero();
        
        cmmnRuntimeService.startPlanItemInstance(planItemInstance.getId());
        planItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertThat(planItemInstance.getState()).isEqualTo(PlanItemInstanceState.ACTIVE);
        
        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertThat(task.getName()).isEqualTo("The Task");
        
        cmmnTaskService.complete(task.getId());
        assertCaseInstanceEnded(caseInstance);
    }
    
    @Test
    @CmmnDeployment
    public void testDisableSingleHumanTask() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testDisableSingleHumanTask").start();
        
        PlanItemInstance planItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertThat(planItemInstance.getState()).isEqualTo(PlanItemInstanceState.ENABLED);
        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).count()).isZero();
        
        // Disabling the single plan item will terminate the case
        cmmnRuntimeService.disablePlanItemInstance(planItemInstance.getId());
        assertCaseInstanceEnded(caseInstance);
    }
    
    @Test
    @CmmnDeployment
    public void testDisableHumanTask() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testDisableHumanTask").start();
        
        List<PlanItemInstance> planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).list();
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getState)
                .contains(PlanItemInstanceState.ENABLED);
        for (PlanItemInstance planItemInstance : planItemInstances) {
            assertThat(planItemInstance.getState()).isEqualTo(PlanItemInstanceState.ENABLED);
        }
        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).count()).isZero();
        
        PlanItemInstance planItemInstance = planItemInstances.get(0);
        cmmnRuntimeService.disablePlanItemInstance(planItemInstance.getId());
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemInstanceStateEnabled().count()).isEqualTo(1);
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemInstanceStateDisabled().count()).isEqualTo(1);
        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).count()).isZero();

        cmmnRuntimeService.enablePlanItemInstance(planItemInstance.getId());
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemInstanceStateEnabled().count()).isEqualTo(2);
    }
    
    @Test
    @CmmnDeployment
    public void testManualActivationWithSentries() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testManualActivationWithSentries").start();
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemInstanceStateEnabled().count()).isZero();
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemInstanceStateAvailable().count()).isEqualTo(1);
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemInstanceStateActive().count()).isEqualTo(1);
        
        cmmnRuntimeService.setVariables(caseInstance.getId(), CollectionUtil.singletonMap("variable", "startStage"));
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemInstanceStateEnabled().count()).isEqualTo(1);
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemInstanceStateActive().count()).isEqualTo(1);
        cmmnRuntimeService.startPlanItemInstance(cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemInstanceStateEnabled().singleResult().getId());
        
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemInstanceStateActive().count()).isEqualTo(4);
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemInstanceStateAvailable().count()).isEqualTo(1);
        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).count()).isEqualTo(3);
        
        // Completing C should enable the nested stage
        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).taskName("C").singleResult();
        cmmnTaskService.complete(task.getId());
        
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemInstanceStateEnabled().count()).isEqualTo(1);
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemInstanceStateActive().count()).isEqualTo(3);
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemInstanceStateAvailable().count()).isZero();
        
        // Enabling the nested stage activates task D
        PlanItemInstance planItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemInstanceStateEnabled().singleResult();
        cmmnRuntimeService.startPlanItemInstance(planItemInstance.getId());
        
        List<Task> tasks = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).orderByTaskName().asc().list();
        assertThat(tasks)
                .extracting(Task::getName)
                .containsExactly("A", "B", "D");
        
        // Completing all the tasks ends the case instance
        for (Task t : cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).list()) {
            cmmnTaskService.complete(t.getId());
        }
        assertCaseInstanceEnded(caseInstance);
    }
    
    @Test
    @CmmnDeployment
    public void testExitEnabledPlanItem() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testExitEnabledPlanItem").start();
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemInstanceStateEnabled().count()).isEqualTo(1);
        
        List<Task> tasks = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).orderByTaskName().asc().list();
        assertThat(tasks)
                .extracting(Task::getName)
                .containsExactly("A", "C");

        // Completing task A will exit the enabled stage
        cmmnTaskService.complete(tasks.get(0).getId());
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemInstanceStateEnabled().count()).isZero();
        
        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertThat(task.getName()).isEqualTo("C");
        
        cmmnTaskService.complete(task.getId());
        assertCaseInstanceEnded(caseInstance);
    }
    
    @Test
    @CmmnDeployment
    public void testManuallyActivatedServiceTask() {
        // Manual Activation enabled
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("testManuallyActivatedServiceTask")
                .variable("manual", true)
                .start();
        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).count()).isEqualTo(1);
        
        PlanItemInstance planItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceStateEnabled().planItemDefinitionType(PlanItemDefinitionType.SERVICE_TASK).singleResult();
        assertThat(planItemInstance).isNotNull();
        cmmnRuntimeService.startPlanItemInstance(planItemInstance.getId());
        assertThat(cmmnRuntimeService.getVariable(caseInstance.getId(), "variable")).isEqualTo("test");
        
        // Manual Activation disabled
        caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("testManuallyActivatedServiceTask")
                .variable("manual", false)
                .start();
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemInstanceStateEnabled().count()).isZero();
        assertThat(cmmnRuntimeService.getVariable(caseInstance.getId(), "variable")).isEqualTo("test");
    }
    
    @Test
    @CmmnDeployment
    public void testManuallyActivatedStage() {
        // Manual Activation enabled
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("manualStage")
                .start();
        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).count()).isZero();
        
        cmmnEngineConfiguration.getCommandExecutor().execute(new Command<Void>() {

            @Override
            public Void execute(CommandContext commandContext) {
                PlanItemInstance planItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceStateEnabled().planItemDefinitionType(PlanItemDefinitionType.STAGE).singleResult();
                assertThat(planItemInstance).isNotNull();
                cmmnRuntimeService.startPlanItemInstance(planItemInstance.getId());
                
                return null;
            }
            
        });
        
        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).count()).isEqualTo(1);
    }
    
    @Test
    @CmmnDeployment
    public void testRepeatedManualActivatedHumanTask() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("testRepeatedManualActivatedHumanTask")
                .variable("stopRepeat", false)
                .start();
        
        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).count()).isEqualTo(1);
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemInstanceStateEnabled().count()).isEqualTo(1);
        cmmnRuntimeService.startPlanItemInstance(cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemInstanceStateEnabled().singleResult().getId());
        
        // This can go on forever (but testing 100 here), as it's repeated without stop
        for (int i = 0; i < 100; i++) {
            
            List<Task> tasks = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).orderByTaskName().asc().list();
            assertThat(tasks)
                    .extracting(Task::getName)
                    .containsExactly("Non-repeated task", "Repeated task");

            // Completing the repeated task should again lead to an enabled task
            cmmnTaskService.complete(tasks.get(1).getId());
            assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemInstanceStateEnabled().count())
                    .isEqualTo(1);
            cmmnRuntimeService.startPlanItemInstance(cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemInstanceStateEnabled().singleResult().getId());
        }
        
        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).count()).isEqualTo(2);
        cmmnRuntimeService.setVariables(caseInstance.getId(), CollectionUtil.singletonMap("stopRepeat", true));
        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).count()).isEqualTo(1);
    }
    
    @Test
    @CmmnDeployment
    public void testInvalidDisable() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testInvalidDisable").start();
        
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemInstanceStateEnabled().count()).isEqualTo(1);
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemInstanceStateActive().count()).isEqualTo(1);
        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).count()).isEqualTo(1);

        PlanItemInstance planItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemInstanceStateActive().singleResult();
        assertThatThrownBy(() -> cmmnRuntimeService.disablePlanItemInstance(planItemInstance.getId()))
                .isInstanceOf(FlowableIllegalStateException.class)
                .hasMessage("Can only disable a plan item instance which is in state ENABLED");
    }
    
    @Test
    @CmmnDeployment
    public void testInvalidEnable() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testInvalidEnable").start();
        
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemInstanceStateEnabled().count()).isEqualTo(1);
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemInstanceStateActive().count()).isEqualTo(1);
        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).count()).isEqualTo(1);

        PlanItemInstance planItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemInstanceStateActive().singleResult();
        assertThatThrownBy(() -> cmmnRuntimeService.enablePlanItemInstance(planItemInstance.getId()))
                .isInstanceOf(FlowableIllegalStateException.class)
                .hasMessage("Can only enable a plan item instance which is in state AVAILABLE or DISABLED");
    }
    
    @Test
    @CmmnDeployment
    public void testInvalidStart() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testInvalidStart").start();
        
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemInstanceStateEnabled().count()).isEqualTo(1);
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemInstanceStateActive().count()).isEqualTo(1);
        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).count()).isEqualTo(1);

        PlanItemInstance planItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemInstanceStateActive().singleResult();
        assertThatThrownBy(() -> cmmnRuntimeService.startPlanItemInstance(planItemInstance.getId()))
                .isInstanceOf(FlowableIllegalStateException.class)
                .hasMessage("Can only enable a plan item instance which is in state ENABLED");
    }

    // Test specifically made for testing a plan item instance caching issue
    @Test
    @CmmnDeployment
    public void testCompleteManualActivatedTaskWithCustomCommand() {
        cmmnRuntimeService.createCaseInstanceBuilder()
            .caseDefinitionKey("testManualActivation")
            .variable("initiator", "test123")
            .start();

        Task taskA = cmmnTaskService.createTaskQuery().singleResult();
        assertThat(taskA.getName()).isEqualTo("A");
        cmmnTaskService.complete(taskA.getId());

        final PlanItemInstance planItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceState(PlanItemInstanceState.ENABLED).singleResult();
        assertThat(planItemInstance.getName()).isEqualTo("B");

        cmmnEngineConfiguration.getCommandExecutor().execute(new Command<Void>() {
            @Override
            public Void execute(CommandContext commandContext) {

                // Fetch the plan item instance before the next command (already putting it in the cache)
                // to trigger the caching issue (when eagerly fetching plan items the old state was being overwritten)
                PlanItemInstance p = cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceId(planItemInstance.getId()).singleResult();
                assertThat(p).isNotNull();

                cmmnRuntimeService.startPlanItemInstance(planItemInstance.getId());

                return null;
            }
        });

        Task taskB = cmmnTaskService.createTaskQuery().singleResult();
        assertThat(taskB.getName()).isEqualTo("B");
        PlanItemInstance planItemInstanceAfterCommand = cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceId(planItemInstance.getId()).singleResult();
        assertThat(planItemInstanceAfterCommand.getState()).isEqualTo(PlanItemInstanceState.ACTIVE);

        cmmnTaskService.complete(taskB.getId());
        Task taskC = cmmnTaskService.createTaskQuery().singleResult();
        assertThat(taskC.getName()).isEqualTo("C");

        cmmnTaskService.complete(taskC.getId());
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().count()).isZero();
    }

    @Test
    @CmmnDeployment
    public void testManuallyActivatedRequiredAndRepeatingTask() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("test").start();
        cmmnTaskService.complete(cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).taskName("A").singleResult().getId());
        assertCaseInstanceNotEnded(caseInstance);

        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).taskName("B").count()).isZero();

        PlanItemInstance planItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceState(PlanItemInstanceState.ENABLED).singleResult();
        cmmnRuntimeService.startPlanItemInstance(planItemInstance.getId());
        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).taskName("B").count()).isEqualTo(1);

        // 1 instance is required, the others aren't
        cmmnTaskService.complete(cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).taskName("B").singleResult().getId());

        assertCaseInstanceEnded(caseInstance);

    }

    @Test
    @CmmnDeployment
    public void testManuallyActivateStage() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testManualActivatedStage").start();
        PlanItemInstance stagePlanItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceStateEnabled().singleResult();
        assertThat(stagePlanItemInstance.getName()).isEqualTo("Stage one");

        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).count()).isZero();
        cmmnRuntimeService.createPlanItemInstanceTransitionBuilder(stagePlanItemInstance.getId()).start();
        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).count()).isEqualTo(1);

        cmmnTaskService.complete(cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult().getId());
        assertCaseInstanceEnded(caseInstance);
    }

    @Test
    @CmmnDeployment
    public void testManuallyActivateStageWithQueryAndStartInOneTransaction() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testManualActivatedStage").start();
        PlanItemInstance stagePlanItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceStateEnabled().singleResult();
        assertThat(stagePlanItemInstance.getName()).isEqualTo("Stage one");

        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).count()).isZero();
        cmmnEngineConfiguration.getCommandExecutor().execute(new Command<Void>() {
            @Override
            public Void execute(CommandContext commandContext) {
                PlanItemInstance planItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceStateEnabled().planItemDefinitionType(PlanItemDefinitionType.STAGE).singleResult();
                assertThat(planItemInstance).isNotNull();
                cmmnRuntimeService.startPlanItemInstance(planItemInstance.getId());

                return null;
            }

        });
        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).count()).isEqualTo(1);

        cmmnTaskService.complete(cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult().getId());
        assertCaseInstanceEnded(caseInstance);
    }

    @Test
    @CmmnDeployment
    public void testRequiredHumanTaskInManuallyActivatedStage() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("stageTest").start();

        // Activate the manually activated stage
        PlanItemInstance stagePlanItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceName("Stage with blocking task").planItemInstanceStateEnabled().singleResult();
        assertThat(stagePlanItemInstance.getName()).isEqualTo("Stage with blocking task");

        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId())
            .planItemDefinitionType(PlanItemDefinitionType.HUMAN_TASK).planItemInstanceStateEnabled().count()).isZero();
        cmmnRuntimeService.createPlanItemInstanceTransitionBuilder(stagePlanItemInstance.getId()).start();
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId())
            .planItemDefinitionType(PlanItemDefinitionType.HUMAN_TASK).planItemInstanceStateEnabled().count()).isEqualTo(1);

        // Manually complete the stage should throw an exception
        assertThatThrownBy(() -> cmmnRuntimeService.completeStagePlanItemInstance(stagePlanItemInstance.getId()))
                .isInstanceOf(FlowableIllegalArgumentException.class)
                .hasMessageContaining("Can only complete a stage plan item instance that is marked as completable (there might still be active plan item instance).");
    }
    
}
