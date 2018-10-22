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
import static org.junit.Assert.assertEquals;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.api.runtime.PlanItemDefinitionType;
import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstanceState;
import org.flowable.cmmn.api.runtime.UserEventListenerInstance;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.engine.test.FlowableCmmnTestCase;
import org.flowable.job.api.Job;
import org.flowable.task.api.Task;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Joram Barrez
 */
public class PlanItemInstanceQueryTest extends FlowableCmmnTestCase {

    protected String deploymentId;
    protected String caseDefinitionId;

    @Before
    public void deployCaseDefinition() {
        this.deploymentId = cmmnRepositoryService.createDeployment()
            .addClasspathResource("org/flowable/cmmn/test/runtime/PlanItemInstanceQueryTest.testPlanItemInstanceQuery.cmmn")
            .deploy()
            .getId();
        caseDefinitionId = cmmnRepositoryService.createCaseDefinitionQuery()
            .deploymentId(deploymentId)
            .singleResult()
            .getId();
    }

    @After
    public void deleteDeployment() {
        cmmnRepositoryService.deleteDeployment(deploymentId, true);
    }

    @Test
    public void testByCaseDefinitionId() {
        startInstances(5);
        assertEquals(20, cmmnRuntimeService.createPlanItemInstanceQuery().list().size());
    }

    @Test
    public void testByCaseInstanceId() {
        List<String> caseInstanceIds = startInstances(3);
        for (String caseInstanceId : caseInstanceIds) {
            assertEquals(4, cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstanceId).list().size());
        }
    }

    @Test
    public void testByStageInstanceId() {
        startInstances(1);
        PlanItemInstance planItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery()
            .planItemDefinitionType(PlanItemDefinitionType.STAGE)
            .planItemInstanceName("Stage one")
            .singleResult();
        assertEquals(2, cmmnRuntimeService.createPlanItemInstanceQuery().stageInstanceId(planItemInstance.getId()).count());
    }

    @Test
    public void testByPlanItemInstanceId() {
        startInstances(1);
        List<PlanItemInstance> planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery().list();
        for (PlanItemInstance planItemInstance : planItemInstances) {
            assertEquals(1L, cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceId(planItemInstance.getId()).count());
        }
    }

    @Test
    public void testByElementId() {
        startInstances(4);
        assertEquals(4, cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceElementId("planItem3").list().size());
    }

    @Test
    public void testByName() {
        startInstances(9);
        assertEquals(9, cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceName("B").list().size());
    }

    @Test
    public void testByState() {
        startInstances(1);
        assertEquals(2, cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceState(PlanItemInstanceState.ACTIVE).list().size());
        assertEquals(2, cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceStateActive().list().size());

        assertEquals(1, cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceState(PlanItemInstanceState.AVAILABLE).list().size());
        assertEquals(1, cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceStateAvailable().list().size());

        assertEquals(1, cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceState(PlanItemInstanceState.ENABLED).list().size());
        assertEquals(1, cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceStateEnabled().list().size());
    }

    @Test
    public void testByPlanItemDefinitionType() {
        startInstances(3);
        assertEquals(6, cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionType(PlanItemDefinitionType.HUMAN_TASK).list().size());
        assertEquals(6, cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionType(PlanItemDefinitionType.STAGE).list().size());
    }

    @Test
    public void testByPlanItemDefinitionTypes() {
        startInstances(2);
        assertEquals(8, cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionTypes(Arrays.asList(PlanItemDefinitionType.STAGE, PlanItemDefinitionType.HUMAN_TASK)).list().size());
    }


    @Test
    public void testByStateEnabled() {
        startInstances(4);
        List<PlanItemInstance> planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
            .planItemInstanceStateEnabled()
            .list();
        assertThat(planItemInstances)
            .hasSize(4)
            .extracting(PlanItemInstance::getName).containsOnly("B");
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
            .extracting(PlanItemInstance::getName).containsOnly("Stage two");
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
            .extracting(PlanItemInstance::getName).containsExactly("A", "A", "Stage one", "Stage one");
    }

    @Test
    public void testByStateAndType() {
        startInstances(3);
        assertEquals(3, cmmnRuntimeService.createPlanItemInstanceQuery()
            .planItemInstanceState(PlanItemInstanceState.ACTIVE)
            .planItemDefinitionType(PlanItemDefinitionType.HUMAN_TASK)
            .list().size());

        assertEquals(3, cmmnRuntimeService.createPlanItemInstanceQuery()
            .planItemInstanceState(PlanItemInstanceState.ENABLED)
            .planItemDefinitionType(PlanItemDefinitionType.HUMAN_TASK)
            .list().size());
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
            .extracting(PlanItemInstance::getName).containsOnly("A");

        // includeEnded should also return the same result
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceStateCompleted().includeEnded().list()).hasSize(3);

        // Without ended, should only return runtime plan item instances
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceStateCompleted().list()).hasSize(0);
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
        assertThat(planItemInstances).hasSize(0);

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
            .hasSize(3)
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

    private List<String> startInstances(int numberOfInstances) {
        List<String> caseInstanceIds = new ArrayList<>();
        for (int i = 0; i < numberOfInstances; i++) {
            caseInstanceIds.add(cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testPlanItemInstanceQuery").start().getId());
        }
        return caseInstanceIds;
    }

}
