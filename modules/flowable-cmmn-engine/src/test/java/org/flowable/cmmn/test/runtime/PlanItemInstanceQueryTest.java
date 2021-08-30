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
package org.flowable.cmmn.test.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.api.runtime.PlanItemDefinitionType;
import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstanceState;
import org.flowable.cmmn.api.runtime.UserEventListenerInstance;
import org.flowable.cmmn.api.history.HistoricPlanItemInstance;
import org.flowable.cmmn.engine.PlanItemLocalizationManager;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.engine.test.FlowableCmmnTestCase;
import org.flowable.task.api.Task;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Joram Barrez
 */
public class PlanItemInstanceQueryTest extends FlowableCmmnTestCase {

    protected String caseDefinitionId;

    @Before
    public void deployCaseDefinition() {
        String deploymentId = addDeploymentForAutoCleanup(cmmnRepositoryService.createDeployment()
                .addClasspathResource("org/flowable/cmmn/test/runtime/PlanItemInstanceQueryTest.testPlanItemInstanceQuery.cmmn")
                .deploy());
        caseDefinitionId = cmmnRepositoryService.createCaseDefinitionQuery()
                .deploymentId(deploymentId)
                .singleResult()
                .getId();
    }

    @Test
    public void testByCaseDefinitionId() {
        startInstances(5);
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().list()).hasSize(20);
    }

    @Test
    public void testByCaseInstanceId() {
        List<String> caseInstanceIds = startInstances(3);
        for (String caseInstanceId : caseInstanceIds) {
            assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstanceId).list()).hasSize(4);
        }
    }

    @Test
    public void testByStageInstanceId() {
        startInstances(1);
        PlanItemInstance planItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery()
                .planItemDefinitionType(PlanItemDefinitionType.STAGE)
                .planItemInstanceName("Stage one")
                .singleResult();
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().stageInstanceId(planItemInstance.getId()).count()).isEqualTo(2);
    }

    @Test
    public void testByPlanItemInstanceId() {
        startInstances(1);
        List<PlanItemInstance> planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery().list();
        for (PlanItemInstance planItemInstance : planItemInstances) {
            assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceId(planItemInstance.getId()).count()).isEqualTo(1);
        }
    }

    @Test
    public void testByElementId() {
        startInstances(4);
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceElementId("planItem3").list()).hasSize(4);
    }

    @Test
    public void testByName() {
        startInstances(9);
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceName("B").list()).hasSize(9);
    }

    @Test
    public void testByState() {
        startInstances(1);
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceState(PlanItemInstanceState.ACTIVE).list()).hasSize(2);
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceStateActive().list()).hasSize(2);

        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceState(PlanItemInstanceState.AVAILABLE).list()).hasSize(1);
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceStateAvailable().list()).hasSize(1);

        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceState(PlanItemInstanceState.ENABLED).list()).hasSize(1);
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceStateEnabled().list()).hasSize(1);
    }

    @Test
    public void testByPlanItemDefinitionType() {
        startInstances(3);
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionType(PlanItemDefinitionType.HUMAN_TASK).list()).hasSize(6);
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionType(PlanItemDefinitionType.STAGE).list()).hasSize(6);
    }

    @Test
    public void testByPlanItemDefinitionTypes() {
        startInstances(2);
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery()
                .planItemDefinitionTypes(Arrays.asList(PlanItemDefinitionType.STAGE, PlanItemDefinitionType.HUMAN_TASK)).list()).hasSize(8);
    }

    @Test
    public void testByStateEnabled() {
        startInstances(4);
        List<PlanItemInstance> planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
                .planItemInstanceStateEnabled()
                .list();
        assertThat(planItemInstances)
                .hasSize(4)
                .extracting(PlanItemInstance::getName)
                .containsOnly("B");
    }

    @Test
    public void testByStateDisabled() {
        startInstances(5);

        List<PlanItemInstance> planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
                .planItemInstanceStateEnabled()
                .list();
        cmmnRuntimeService.disablePlanItemInstance(planItemInstances.get(0).getId());
        cmmnRuntimeService.disablePlanItemInstance(planItemInstances.get(1).getId());

        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceStateDisabled().list())
                .hasSize(2)
                .extracting(PlanItemInstance::getName).containsOnly("B");
    }

    @Test
    public void testByStateAvailable() {
        startInstances(3);

        List<PlanItemInstance> planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
                .planItemInstanceStateAvailable()
                .orderByName().asc()
                .list();

        assertThat(planItemInstances)
                .hasSize(3)
                .extracting(PlanItemInstance::getName)
                .containsOnly("Stage two");
    }

    @Test
    public void testByStateActive() {
        startInstances(2);

        List<PlanItemInstance> planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
                .planItemInstanceStateActive()
                .orderByName().asc()
                .list();

        assertThat(planItemInstances)
                .hasSize(4)
                .extracting(PlanItemInstance::getName)
                .containsExactly("A", "A", "Stage one", "Stage one");
    }

    @Test
    public void testByStateAndType() {
        startInstances(3);
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery()
                .planItemInstanceState(PlanItemInstanceState.ACTIVE)
                .planItemDefinitionType(PlanItemDefinitionType.HUMAN_TASK)
                .list()).hasSize(3);

        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery()
                .planItemInstanceState(PlanItemInstanceState.ENABLED)
                .planItemDefinitionType(PlanItemDefinitionType.HUMAN_TASK)
                .list()).hasSize(3);
    }

    @Test
    public void testByStateCompleted() {
        startInstances(4);

        List<Task> tasks = cmmnTaskService.createTaskQuery().list();
        cmmnTaskService.complete(tasks.get(0).getId());
        cmmnTaskService.complete(tasks.get(1).getId());
        cmmnTaskService.complete(tasks.get(3).getId());

        List<PlanItemInstance> planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
                .planItemInstanceStateCompleted()
                .ended()
                .list();
        assertThat(planItemInstances)
                .hasSize(3)
                .extracting(PlanItemInstance::getName)
                .containsOnly("A");

        // includeEnded should also return the same result
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceStateCompleted().includeEnded().list()).hasSize(3);

        // Without ended, should only return runtime plan item instances
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceStateCompleted().list()).isEmpty();
    }

    @Test
    @CmmnDeployment
    public void testByStateTerminated() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("testQueryByStateTerminated").start();

        List<PlanItemInstance> planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
                .ended()
                .planItemInstanceStateTerminated()
                .list();
        assertThat(planItemInstances).isEmpty();

        // Completing the user event will terminate A/C/Stage for c
        UserEventListenerInstance userEventListenerInstance = cmmnRuntimeService.createUserEventListenerInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .singleResult();
        cmmnRuntimeService.completeUserEventListenerInstance(userEventListenerInstance.getId());

        planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
                .ended()
                .planItemInstanceStateTerminated()
                .orderByName().asc()
                .list();
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getName)
                .containsExactly("A", "C", "The Stage");
    }

    @Test
    public void testIncludeEnded() {
        startInstances(11);

        List<Task> tasks = cmmnTaskService.createTaskQuery().list();
        cmmnTaskService.complete(tasks.get(0).getId());
        cmmnTaskService.complete(tasks.get(1).getId());
        cmmnTaskService.complete(tasks.get(2).getId());
        cmmnTaskService.complete(tasks.get(3).getId());

        List<PlanItemInstance> planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceName("A").list();
        assertThat(planItemInstances).hasSize(7); // 11 - 4 (runtime only)

        planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceName("A").includeEnded().list();
        assertThat(planItemInstances).hasSize(11);

        planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceName("A").ended().list();
        assertThat(planItemInstances).hasSize(4);
    }

    @Test
    public void testCreatedBefore() {
        Date now = new Date();
        setClockTo(now);
        startInstances(3);
        setClockTo(new Date(now.getTime() + 20000));
        startInstances(4);

        List<PlanItemInstance> planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
                .planItemInstanceName("A")
                .planItemDefinitionType(PlanItemDefinitionType.HUMAN_TASK)
                .planItemInstanceCreatedBefore(new Date(now.getTime() + 10000))
                .list();
        assertThat(planItemInstances).hasSize(3);

        planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
                .planItemInstanceName("A")
                .planItemDefinitionType(PlanItemDefinitionType.HUMAN_TASK)
                .planItemInstanceCreatedBefore(new Date(now.getTime() + 30000))
                .list();
        assertThat(planItemInstances).hasSize(7);
    }

    @Test
    public void testCreatedAfter() {
        Date now = new Date();
        setClockTo(now);
        startInstances(2);
        setClockTo(new Date(now.getTime() + 20000));
        startInstances(8);

        List<PlanItemInstance> planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
                .planItemInstanceName("A")
                .planItemDefinitionType(PlanItemDefinitionType.HUMAN_TASK)
                .planItemInstanceCreatedAfter(new Date(now.getTime() - 10000))
                .list();
        assertThat(planItemInstances).hasSize(10);

        planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
                .planItemInstanceName("A")
                .planItemDefinitionType(PlanItemDefinitionType.HUMAN_TASK)
                .planItemInstanceCreatedAfter(new Date(now.getTime() + 10000))
                .list();
        assertThat(planItemInstances).hasSize(8);
    }

    @Test
    public void testLastAvailableBeforeAndAfter() {
        Date now = new Date();
        setClockTo(now);
        startInstances(3);
        setClockTo(new Date(now.getTime() + 20000));
        startInstances(5);

        List<PlanItemInstance> planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
                .planItemInstanceName("A")
                .planItemDefinitionType(PlanItemDefinitionType.HUMAN_TASK)
                .planItemInstanceLastAvailableAfter(new Date(now.getTime() - 10000))
                .list();
        assertThat(planItemInstances).hasSize(8);

        planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
                .planItemInstanceName("A")
                .planItemDefinitionType(PlanItemDefinitionType.HUMAN_TASK)
                .planItemInstanceLastAvailableBefore(new Date(now.getTime() - 10000))
                .list();
        assertThat(planItemInstances).isEmpty();

        planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
                .planItemInstanceName("A")
                .planItemDefinitionType(PlanItemDefinitionType.HUMAN_TASK)
                .planItemInstanceLastAvailableAfter(new Date(now.getTime() + 10000))
                .list();
        assertThat(planItemInstances).hasSize(5);

        planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
                .planItemInstanceName("A")
                .planItemDefinitionType(PlanItemDefinitionType.HUMAN_TASK)
                .planItemInstanceLastAvailableBefore(new Date(now.getTime() + 10000))
                .list();
        assertThat(planItemInstances).hasSize(3);
    }

    @Test
    public void testLastEnabledBeforeAndAfter() {
        Date now = new Date();
        setClockTo(now);
        startInstances(2);
        setClockTo(now.getTime() + 10000);
        startInstances(3);

        // Before
        List<PlanItemInstance> planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
                .planItemInstanceLastEnabledBefore(new Date(now.getTime() + 30000)).list();
        assertThat(planItemInstances).hasSize(5);

        planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
                .planItemInstanceLastEnabledBefore(new Date(now.getTime() + 5000)).list();
        assertThat(planItemInstances).hasSize(2);

        planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
                .planItemInstanceLastEnabledBefore(new Date(now.getTime() - 1000)).list();
        assertThat(planItemInstances).isEmpty();

        // After
        planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
                .planItemInstanceLastEnabledAfter(new Date(now.getTime() - 5000)).list();
        assertThat(planItemInstances).hasSize(5);

        planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
                .planItemInstanceLastEnabledAfter(new Date(now.getTime() + 250000)).list();
        assertThat(planItemInstances).isEmpty();
    }

    @Test
    public void testLastDisabledBeforeAndAfter() {
        Date now = setClockFixedToCurrentTime();
        setClockTo(now);
        startInstances(3);

        String planItemInstanceId = cmmnRuntimeService.createPlanItemInstanceQuery()
                .planItemInstanceName("B").planItemInstanceStateEnabled().listPage(0, 1).get(0).getId();
        cmmnRuntimeService.disablePlanItemInstance(planItemInstanceId);

        setClockTo(now.getTime() + 10000);
        cmmnRuntimeService.disablePlanItemInstance(cmmnRuntimeService.createPlanItemInstanceQuery()
                .planItemInstanceName("B").planItemInstanceStateEnabled().listPage(0, 1).get(0).getId());

        // Before
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceLastDisabledBefore(new Date(now.getTime() + 20000)).list()).hasSize(2);
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceLastDisabledBefore(new Date(now.getTime() + 5000)).list()).hasSize(1);
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceLastDisabledBefore(new Date(now.getTime() - 5000)).list()).isEmpty();

        // After
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceLastDisabledAfter(new Date(now.getTime())).list()).hasSize(2);
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceLastDisabledAfter(new Date(now.getTime() + 5000)).list()).hasSize(1);
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceLastDisabledAfter(new Date(now.getTime() + 11000)).list()).isEmpty();

        // Re-enable and disable
        PlanItemInstance planItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceId(planItemInstanceId).singleResult();
        Date lastEnabledTime = planItemInstance.getLastEnabledTime();
        assertThat(lastEnabledTime).isNotNull();

        setClockTo(now.getTime() + 30000);
        cmmnRuntimeService.enablePlanItemInstance(planItemInstanceId);
        cmmnRuntimeService.disablePlanItemInstance(planItemInstanceId);

        planItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceId(planItemInstanceId).singleResult();
        assertThat(planItemInstance.getLastEnabledTime()).isNotEqualTo(lastEnabledTime);

        // Recheck queries
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceLastDisabledBefore(new Date(now.getTime() + 20000)).list()).hasSize(1);
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceLastDisabledBefore(new Date(now.getTime() + 5000)).list()).isEmpty();
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceLastDisabledAfter(new Date(now.getTime())).list()).hasSize(2);
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceLastDisabledAfter(new Date(now.getTime() + 15000)).list()).hasSize(1);
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceLastDisabledAfter(new Date(now.getTime() + 35000)).list()).isEmpty();
    }

    @Test
    public void testLastStartedBeforeAndAfter() {
        Date now = new Date();
        setClockTo(now);
        startInstances(4);

        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceLastStartedAfter(new Date(now.getTime() - 1000)).list()).hasSize(8);
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceLastStartedAfter(new Date(now.getTime() + 1000)).list()).isEmpty();

        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceLastStartedBefore(new Date(now.getTime() + 1000)).list()).hasSize(8);
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceLastStartedBefore(new Date(now.getTime() - 1000)).list()).isEmpty();

        // Starting an enabled planitem
        setClockTo(now.getTime() + 10000);
        cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceName("B").listPage(0, 2)
                .forEach(p -> cmmnRuntimeService.startPlanItemInstance(p.getId()));

        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceLastStartedAfter(new Date(now.getTime() - 1000)).list()).hasSize(10);
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceLastStartedAfter(new Date(now.getTime() + 5000)).list()).hasSize(2);
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceLastStartedAfter(new Date(now.getTime() + 15000)).list()).isEmpty();

        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceLastStartedBefore(new Date(now.getTime() + 1000)).list()).hasSize(8);
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceLastStartedBefore(new Date(now.getTime() + 15000)).list()).hasSize(10);
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceLastStartedBefore(new Date(now.getTime() - 1000)).list()).isEmpty();

    }

    @Test
    public void testCompletedBeforeAndAfter() {
        startInstances(5);
        Date now = new Date();
        setClockTo(now);

        List<Task> tasks = cmmnTaskService.createTaskQuery().listPage(0, 2);
        setClockTo(now.getTime() + 10000);
        cmmnTaskService.complete(tasks.get(0).getId());
        setClockTo(now.getTime() + 20000);
        cmmnTaskService.complete(tasks.get(1).getId());

        // Completed
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceCompletedBefore(new Date(now.getTime() + 30000)).list()).isEmpty();
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceCompletedBefore(new Date(now.getTime() + 30000)).includeEnded().list())
                .hasSize(2);
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceCompletedBefore(new Date(now.getTime() + 15000)).includeEnded().list())
                .hasSize(1);

        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceCompletedAfter(new Date(now.getTime())).list()).isEmpty();
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceCompletedAfter(new Date(now.getTime())).includeEnded().list()).hasSize(2);
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceCompletedAfter(new Date(now.getTime())).ended().list()).hasSize(2);
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceCompletedAfter(new Date(now.getTime() + 15000)).includeEnded().list())
                .hasSize(1);

        // Same queries, but with endedBefore/After
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceEndedBefore(new Date(now.getTime() + 30000)).list()).isEmpty();
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceEndedBefore(new Date(now.getTime() + 30000)).includeEnded().list())
                .hasSize(2);
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceEndedBefore(new Date(now.getTime() + 15000)).includeEnded().list())
                .hasSize(1);

        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceEndedAfter(new Date(now.getTime())).list()).isEmpty();
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceEndedAfter(new Date(now.getTime())).includeEnded().list()).hasSize(2);
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceEndedAfter(new Date(now.getTime())).ended().list()).hasSize(2);
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceEndedAfter(new Date(now.getTime() + 15000)).includeEnded().list())
                .hasSize(1);
    }

    @Test
    public void testLastOccurredBeforeAndAfter() {
        Date now = new Date();
        setClockTo(now);
        startInstances(2);

        cmmnRuntimeService.completeStagePlanItemInstance(cmmnRuntimeService.createPlanItemInstanceQuery()
                .planItemInstanceName("Stage one").listPage(0, 1).get(0).getId(), true);

        setClockTo(now.getTime() + 10000);
        cmmnRuntimeService.completeStagePlanItemInstance(cmmnRuntimeService.createPlanItemInstanceQuery()
                .planItemInstanceName("Stage one").listPage(0, 1).get(0).getId(), true);

        // Occurred (milestone)
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceOccurredAfter(new Date(now.getTime() - 1000)).list()).isEmpty();
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceOccurredAfter(new Date(now.getTime() - 1000)).includeEnded().list())
                .hasSize(2);
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceOccurredAfter(new Date(now.getTime() + 1000)).includeEnded().list())
                .hasSize(1);
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceOccurredAfter(new Date(now.getTime() + 15000)).includeEnded().list())
                .isEmpty();

        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceOccurredBefore(new Date(now.getTime() + 20000)).includeEnded().list())
                .hasSize(2);
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceOccurredBefore(new Date(now.getTime() + 5000)).includeEnded().list())
                .hasSize(1);
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceOccurredBefore(new Date(now.getTime() - 1000)).includeEnded().list())
                .isEmpty();
    }

    @Test
    @CmmnDeployment
    public void testExitBeforeAndAfter() {
        Date now = new Date();
        setClockTo(now);

        cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testQueryByStateTerminated").start();
        UserEventListenerInstance userEventListenerInstance = cmmnRuntimeService.createUserEventListenerInstanceQuery().singleResult();
        cmmnRuntimeService.completeUserEventListenerInstance(userEventListenerInstance.getId());

        setClockTo(now.getTime() + 10000);
        cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testQueryByStateTerminated").start();
        userEventListenerInstance = cmmnRuntimeService.createUserEventListenerInstanceQuery().singleResult();
        cmmnRuntimeService.completeUserEventListenerInstance(userEventListenerInstance.getId());

        // Terminated before/after
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().includeEnded().planItemInstanceExitAfter(new Date(now.getTime() - 1000)).list()).hasSize(6);
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().includeEnded().planItemInstanceExitAfter(new Date(now.getTime() + 1000)).list()).hasSize(3);
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().includeEnded().planItemInstanceExitAfter(new Date(now.getTime() + 20000)).list())
                .isEmpty();

        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().includeEnded().planItemInstanceExitBefore(new Date(now.getTime() - 1000)).list())
                .isEmpty();
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().includeEnded().planItemInstanceExitBefore(new Date(now.getTime() + 1000)).list())
                .hasSize(3);
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().includeEnded().planItemInstanceExitBefore(new Date(now.getTime() + 20000)).list())
                .hasSize(6);

        // Ended before/after
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().includeEnded().planItemInstanceEndedAfter(new Date(now.getTime() - 1000)).list())
                .hasSize(8); // + 2 for user event listener
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().includeEnded().planItemInstanceEndedAfter(new Date(now.getTime() + 1000)).list())
                .hasSize(4);
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().includeEnded().planItemInstanceEndedAfter(new Date(now.getTime() + 20000)).list())
                .isEmpty();

        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().includeEnded().planItemInstanceEndedBefore(new Date(now.getTime() - 1000)).list())
                .isEmpty();
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().includeEnded().planItemInstanceEndedBefore(new Date(now.getTime() + 1000)).list())
                .hasSize(4);
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().includeEnded().planItemInstanceEndedBefore(new Date(now.getTime() + 20000)).list())
                .hasSize(8);

        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().includeEnded()
                .planItemDefinitionType(PlanItemDefinitionType.USER_EVENT_LISTENER)
                .planItemInstanceEndedAfter(new Date(now.getTime() - 1000)).list()).hasSize(2);

    }

    @Test
    @CmmnDeployment
    public void testTerminateBeforeAndAfter() {
        Date now = new Date();
        setClockTo(now);
        cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testTerminate").start();

        PlanItemInstance stagePlanItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery()
                .planItemDefinitionType(PlanItemDefinitionType.STAGE)
                .planItemInstanceName("The Stage")
                .singleResult();

        cmmnRuntimeService.terminatePlanItemInstance(stagePlanItemInstance.getId());
        assertThat(cmmnTaskService.createTaskQuery().taskName("D").singleResult()).isNotNull();

        // Terminated
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().includeEnded().planItemInstanceTerminatedBefore(new Date(now.getTime() + 1000)).list())
                .hasSize(2); // 2 -> stage and C
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().includeEnded().planItemInstanceTerminatedBefore(new Date(now.getTime() - 1000)).list())
                .isEmpty();

        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().includeEnded().planItemInstanceTerminatedAfter(new Date(now.getTime() + 1000)).list())
                .isEmpty();
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().includeEnded().planItemInstanceTerminatedAfter(new Date(now.getTime() - 1000)).list())
                .hasSize(2);

        // Ended
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().includeEnded().planItemInstanceEndedBefore(new Date(now.getTime() + 1000)).list())
                .hasSize(2); // 2 -> stage and C
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().includeEnded().planItemInstanceEndedBefore(new Date(now.getTime() - 1000)).list())
                .isEmpty();

        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().includeEnded().planItemInstanceEndedAfter(new Date(now.getTime() + 1000)).list())
                .isEmpty();
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().includeEnded().planItemInstanceEndedAfter(new Date(now.getTime() - 1000)).list())
                .hasSize(2);
    }

    @Test
    @CmmnDeployment
    public void testWaitRepetitionOfNestedStages() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("testWaitRepetitionOfNestedStages")
                .variable("stage1", false)
                .variable("stage11", false)
                .variable("stage12", false)
                .start();

        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().list())
                .extracting(PlanItemInstance::getPlanItemDefinitionId, PlanItemInstance::getState)
                .containsExactlyInAnyOrder(
                        tuple("oneexpandedstage1", "available"),
                        tuple("oneexpandedstage2", "available")
                );

        cmmnRuntimeService.createChangePlanItemStateBuilder()
                .caseInstanceId(caseInstance.getId())
                .activatePlanItemDefinitionIds(Arrays.asList("oneexpandedstage4"))
                .changeState();

        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().list())
                .extracting(PlanItemInstance::getPlanItemDefinitionId, PlanItemInstance::getState)
                .containsExactlyInAnyOrder(
                        tuple("oneexpandedstage1", "active"),
                        tuple("oneexpandedstage1", "wait_repetition"),
                        tuple("oneexpandedstage2", "available"),
                        tuple("oneexpandedstage6", "available"),
                        tuple("oneexpandedstage4", "active"),
                        tuple("oneexpandedstage4", "wait_repetition"), // FIXME: this plan item instance is missing
                        tuple("oneeventlistener1", "available"),
                        tuple("onehumantask1", "available")
                );
    }

    @Test
    public void testLocalization() {
        startInstances(1);

        cmmnEngineConfiguration.setPlanItemLocalizationManager(new PlanItemLocalizationManager() {
            @Override
            public void localize(PlanItemInstance planItemInstance, String locale, boolean withLocalizationFallback) {
                if ("pt".equals(locale)) {
                    planItemInstance.setLocalizedName("Plano traduzido");
                }
            }

            @Override
            public void localize(HistoricPlanItemInstance historicPlanItemInstance, String locale, boolean withLocalizationFallback) {

            }
        });

        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().list())
                .extracting(PlanItemInstance::getName)
                .containsExactlyInAnyOrder(
                        "Stage one",
                        "Stage two",
                        "A",
                        "B"
                );

        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().locale("pt").list())
                .extracting(PlanItemInstance::getName)
                .containsExactlyInAnyOrder(
                        "Plano traduzido",
                        "Plano traduzido",
                        "Plano traduzido",
                        "Plano traduzido"
                );
    }

    public void testQueryVariableValueEqualsAndNotEquals() {
        CaseInstance caseWithStringValue = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("testPlanItemInstanceQuery")
                .name("With string value")
                .start();

        CaseInstance caseWithNullValue = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("testPlanItemInstanceQuery")
                .name("With null value")
                .start();

        CaseInstance caseWithLongValue = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("testPlanItemInstanceQuery")
                .name("With long value")
                .start();

        CaseInstance caseWithDoubleValue = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("testPlanItemInstanceQuery")
                .name("With double value")
                .start();

        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceName("Stage one").list())
                .hasSize(4);

        PlanItemInstance planItemWithStringValue = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseWithStringValue.getId())
                .planItemInstanceName("Stage one")
                .singleResult();

        assertThat(planItemWithStringValue).isNotNull();

        PlanItemInstance planItemWithNullValue = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseWithNullValue.getId())
                .planItemInstanceName("Stage one")
                .singleResult();

        assertThat(planItemWithNullValue).isNotNull();

        PlanItemInstance planItemWithLongValue = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseWithLongValue.getId())
                .planItemInstanceName("Stage one")
                .singleResult();

        assertThat(planItemWithLongValue).isNotNull();

        PlanItemInstance planItemWithDoubleValue = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseWithDoubleValue.getId())
                .planItemInstanceName("Stage one")
                .singleResult();

        assertThat(planItemWithDoubleValue).isNotNull();

        cmmnRuntimeService.setLocalVariable(planItemWithStringValue.getId(), "var", "TEST");
        cmmnRuntimeService.setLocalVariable(planItemWithNullValue.getId(), "var", null);
        cmmnRuntimeService.setLocalVariable(planItemWithLongValue.getId(), "var", 100L);
        cmmnRuntimeService.setLocalVariable(planItemWithDoubleValue.getId(), "var", 45.55);

        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().variableValueNotEquals("var", "TEST").list())
                .extracting(PlanItemInstance::getName, PlanItemInstance::getCaseInstanceId)
                .containsExactlyInAnyOrder(
                        tuple("Stage one", caseWithNullValue.getId()),
                        tuple("Stage one", caseWithLongValue.getId()),
                        tuple("Stage one", caseWithDoubleValue.getId())
                );

        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().variableValueEquals("var", "TEST").list())
                .extracting(PlanItemInstance::getName, PlanItemInstance::getCaseInstanceId)
                .containsExactlyInAnyOrder(
                        tuple("Stage one", caseWithStringValue.getId())
                );

        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().variableValueNotEquals("var", 100L).list())
                .extracting(PlanItemInstance::getName, PlanItemInstance::getCaseInstanceId)
                .containsExactlyInAnyOrder(
                        tuple("Stage one", caseWithStringValue.getId()),
                        tuple("Stage one", caseWithNullValue.getId()),
                        tuple("Stage one", caseWithDoubleValue.getId())
                );

        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().variableValueEquals("var", 100L).list())
                .extracting(PlanItemInstance::getName, PlanItemInstance::getCaseInstanceId)
                .containsExactlyInAnyOrder(
                        tuple("Stage one", caseWithLongValue.getId())
                );

        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().variableValueNotEquals("var", 45.55).list())
                .extracting(PlanItemInstance::getName, PlanItemInstance::getCaseInstanceId)
                .containsExactlyInAnyOrder(
                        tuple("Stage one", caseWithStringValue.getId()),
                        tuple("Stage one", caseWithNullValue.getId()),
                        tuple("Stage one", caseWithLongValue.getId())
                );

        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().variableValueEquals("var", 45.55).list())
                .extracting(PlanItemInstance::getName, PlanItemInstance::getCaseInstanceId)
                .containsExactlyInAnyOrder(
                        tuple("Stage one", caseWithDoubleValue.getId())
                );

        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().variableValueNotEquals("var", "test").list())
                .extracting(PlanItemInstance::getName, PlanItemInstance::getCaseInstanceId)
                .containsExactlyInAnyOrder(
                        tuple("Stage one", caseWithStringValue.getId()),
                        tuple("Stage one", caseWithNullValue.getId()),
                        tuple("Stage one", caseWithLongValue.getId()),
                        tuple("Stage one", caseWithDoubleValue.getId())
                );

        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().variableValueNotEqualsIgnoreCase("var", "test").list())
                .extracting(PlanItemInstance::getName, PlanItemInstance::getCaseInstanceId)
                .containsExactlyInAnyOrder(
                        tuple("Stage one", caseWithNullValue.getId()),
                        tuple("Stage one", caseWithLongValue.getId()),
                        tuple("Stage one", caseWithDoubleValue.getId())
                );

        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().variableValueEquals("var", "test").list())
                .extracting(PlanItemInstance::getName, PlanItemInstance::getCaseInstanceId)
                .isEmpty();

        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().variableValueEqualsIgnoreCase("var", "test").list())
                .extracting(PlanItemInstance::getName, PlanItemInstance::getCaseInstanceId)
                .containsExactlyInAnyOrder(
                        tuple("Stage one", caseWithStringValue.getId())
                );
    }

    private List<String> startInstances(int numberOfInstances) {
        List<String> caseInstanceIds = new ArrayList<>();
        for (int i = 0; i < numberOfInstances; i++) {
            caseInstanceIds.add(cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testPlanItemInstanceQuery").start().getId());
        }
        return caseInstanceIds;
    }

}
