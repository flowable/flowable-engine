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
package org.flowable.cmmn.test.tenant;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.flowable.cmmn.api.CmmnChangeTenantIdEntityTypes;
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.api.runtime.CaseInstanceBuilder;
import org.flowable.cmmn.engine.test.FlowableCmmnTestCase;
import org.flowable.cmmn.engine.test.impl.CmmnHistoryTestHelper;
import org.flowable.common.engine.api.delegate.event.AbstractFlowableEventListener;
import org.flowable.common.engine.api.delegate.event.FlowableChangeTenantIdEvent;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;
import org.flowable.common.engine.api.delegate.event.FlowableEvent;
import org.flowable.common.engine.api.delegate.event.FlowableEventType;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.common.engine.api.tenant.ChangeTenantIdResult;
import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.job.api.Job;
import org.flowable.task.api.Task;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ChangeTenantIdCaseInstanceTest extends FlowableCmmnTestCase {

    private static final String TEST_TENANT_A = "test-tenant-a";
    private static final String TEST_TENANT_B = "test-tenant-b";
    private static final String TEST_TENANT_C = "test-tenant-c";

    protected String deploymentIdWithTenantA;
    protected String deploymentIdWithTenantB;
    protected String deploymentIdWithTenantC;
    protected String deploymentIdWithoutTenant;
    protected String deploymentIdWithTenantAForJobs;
    protected TestEventListener eventListener = new TestEventListener();

    @Before
    public void setUp() {
        this.deploymentIdWithTenantA = cmmnRepositoryService.createDeployment().addClasspathResource("org/flowable/cmmn/test/tenant/caseWithMilestone.cmmn")
                .tenantId(TEST_TENANT_A).deploy().getId();
        addDeploymentForAutoCleanup(deploymentIdWithTenantA);
        this.deploymentIdWithTenantB = cmmnRepositoryService.createDeployment().addClasspathResource("org/flowable/cmmn/test/tenant/caseWithMilestone.cmmn")
                .tenantId(TEST_TENANT_B).deploy().getId();
        addDeploymentForAutoCleanup(deploymentIdWithTenantB);
        this.deploymentIdWithTenantC = cmmnRepositoryService.createDeployment().addClasspathResource("org/flowable/cmmn/test/tenant/caseWithMilestone.cmmn")
                .tenantId(TEST_TENANT_C).deploy().getId();
        addDeploymentForAutoCleanup(deploymentIdWithTenantC);
        this.deploymentIdWithoutTenant = cmmnRepositoryService.createDeployment()
                .addClasspathResource("org/flowable/cmmn/test/tenant/caseWithMilestoneDup.cmmn").deploy().getId();
        addDeploymentForAutoCleanup(deploymentIdWithoutTenant);
        this.deploymentIdWithTenantAForJobs = cmmnRepositoryService.createDeployment()
                .addClasspathResource("org/flowable/cmmn/test/tenant/caseForJobsAndEventSubscriptions.cmmn.xml").tenantId(TEST_TENANT_A).deploy().getId();
        addDeploymentForAutoCleanup(deploymentIdWithTenantAForJobs);
        cmmnEngineConfiguration.getEventDispatcher().addEventListener(eventListener);
    }

    @After
    public void tearDown() {
        cmmnEngineConfiguration.getEventDispatcher().removeEventListener(eventListener);
    }

    @Test
    public void testChangeTenantIdCaseInstance() {
        //testDeployments {
        assertThat(cmmnRepositoryService.createDeploymentQuery().count()).isEqualTo(5);
        assertThat(cmmnRepositoryService.createDeploymentQuery().deploymentWithoutTenantId().count()).isEqualTo(1);
        assertThat(cmmnRepositoryService.createDeploymentQuery().deploymentTenantId(TEST_TENANT_A).count()).isEqualTo(2);
        assertThat(cmmnRepositoryService.createDeploymentQuery().deploymentTenantId(TEST_TENANT_B).count()).isEqualTo(1);
        assertThat(cmmnRepositoryService.createDeploymentQuery().deploymentTenantId(TEST_TENANT_C).count()).isEqualTo(1);

        // Starting case instances that will be sent to the history
        String caseInstanceIdACompleted = startCase(TEST_TENANT_A, "caseWithMilestone", "caseInstanceACompleted", 2);
        String caseInstanceIdBCompleted = startCase(TEST_TENANT_B, "caseWithMilestone", "caseInstanceBCompleted", 2);
        String caseInstanceIdCCompleted = startCase(TEST_TENANT_C, "caseWithMilestone", "caseInstanceCCompleted", 2);

        // Starting case instances that will be kept active
        String caseInstanceIdAActive = startCase(TEST_TENANT_A, "caseWithMilestone", "caseInstanceAActive", 1);
        String caseInstanceIdBActive = startCase(TEST_TENANT_B, "caseWithMilestone", "caseInstanceBActive", 1);
        String caseInstanceIdCActive = startCase(TEST_TENANT_C, "caseWithMilestone", "caseInstanceCActive", 1);
        String caseInstanceIdAForJobs = startCase(TEST_TENANT_A, "caseForJobsAndEventSubscriptions", "caseInstanceAForJobs", 0);
        Job jobToBeSentToDeadLetter = cmmnManagementService.createTimerJobQuery().caseInstanceId(caseInstanceIdAForJobs)
                .elementName("Timer for a Deadletter Job").singleResult();
        Job jobInTheDeadLetterQueue = cmmnManagementService.moveJobToDeadLetterJob(jobToBeSentToDeadLetter.getId());
        assertThat(jobInTheDeadLetterQueue).as("We have a job in the deadletter queue.").isNotNull();

        Set<String> caseInstanceIdsTenantA = new HashSet<>(Arrays.asList(caseInstanceIdACompleted, caseInstanceIdAActive));
        Set<String> caseInstanceIdsTenantB = new HashSet<>(Arrays.asList(caseInstanceIdBCompleted, caseInstanceIdBActive));
        Set<String> caseInstanceIdsTenantC = new HashSet<>(Arrays.asList(caseInstanceIdCCompleted, caseInstanceIdCActive));

        // Prior to changing the Tenant Id, all elements are associated to the original tenant
        checkTenantIdForAllInstances(caseInstanceIdsTenantA, TEST_TENANT_A, "prior to changing to " + TEST_TENANT_B);
        checkTenantIdForAllInstances(caseInstanceIdsTenantB, TEST_TENANT_B, "prior to changing to " + TEST_TENANT_B);
        checkTenantIdForAllInstances(caseInstanceIdsTenantC, TEST_TENANT_C, "prior to changing to " + TEST_TENANT_B);

        // First we simulate the change
        ChangeTenantIdResult simulationResult = cmmnManagementService.createChangeTenantIdBuilder(TEST_TENANT_A, TEST_TENANT_B).simulate();

        // All the instances should stay in the original tenant after the simulation
        checkTenantIdForAllInstances(caseInstanceIdsTenantA, TEST_TENANT_A, "after simulating the change to " + TEST_TENANT_B);
        checkTenantIdForAllInstances(caseInstanceIdsTenantB, TEST_TENANT_B, "after simulating the change to " + TEST_TENANT_B);
        checkTenantIdForAllInstances(caseInstanceIdsTenantC, TEST_TENANT_C, "after simulating the change to " + TEST_TENANT_B);

        // We now proceed with the changeTenantId operation for all the instances
        ChangeTenantIdResult result = cmmnManagementService.createChangeTenantIdBuilder(TEST_TENANT_A, TEST_TENANT_B).complete();

        // All the instances should now be assigned to the tenant B
        checkTenantIdForAllInstances(caseInstanceIdsTenantA, TEST_TENANT_B, "after the change to " + TEST_TENANT_B);
        checkTenantIdForAllInstances(caseInstanceIdsTenantB, TEST_TENANT_B, "after the change to " + TEST_TENANT_B);

        // The instances for tenant C remain untouched
        checkTenantIdForAllInstances(caseInstanceIdsTenantC, TEST_TENANT_C, "after the change to " + TEST_TENANT_B);

        //Expected results map
        Map<String, Long> resultMap = new HashMap<>();
        resultMap.put(CmmnChangeTenantIdEntityTypes.CASE_INSTANCES, 2L);
        resultMap.put(CmmnChangeTenantIdEntityTypes.PLAN_ITEM_INSTANCES, 10L);
        resultMap.put(CmmnChangeTenantIdEntityTypes.MILESTONE_INSTANCES, 1L);
        resultMap.put(CmmnChangeTenantIdEntityTypes.HISTORIC_CASE_INSTANCES, 3L);
        resultMap.put(CmmnChangeTenantIdEntityTypes.HISTORIC_PLAN_ITEM_INSTANCES, 15L);
        resultMap.put(CmmnChangeTenantIdEntityTypes.HISTORIC_MILESTONE_INSTANCES, 2L);
        resultMap.put(CmmnChangeTenantIdEntityTypes.DEADLETTER_JOBS, 1L);
        resultMap.put(CmmnChangeTenantIdEntityTypes.EVENT_SUBSCRIPTIONS, 1L);
        resultMap.put(CmmnChangeTenantIdEntityTypes.EXTERNAL_WORKER_JOBS, 1L);
        resultMap.put(CmmnChangeTenantIdEntityTypes.HISTORIC_TASK_INSTANCES, 4L);
        resultMap.put(CmmnChangeTenantIdEntityTypes.HISTORIC_TASK_LOG_ENTRIES, 7L);
        resultMap.put(CmmnChangeTenantIdEntityTypes.JOBS, 1L);
        resultMap.put(CmmnChangeTenantIdEntityTypes.SUSPENDED_JOBS, 0L);
        resultMap.put(CmmnChangeTenantIdEntityTypes.TASKS, 1L);
        resultMap.put(CmmnChangeTenantIdEntityTypes.TIMER_JOBS, 1L);

        //Check that all the entities are returned
        assertThat(simulationResult.getChangedEntityTypes()).containsExactlyInAnyOrderElementsOf(resultMap.keySet());
        assertThat(result.getChangedEntityTypes()).containsExactlyInAnyOrderElementsOf(resultMap.keySet());

        resultMap.forEach((key, value) -> {
            //Check simulation result content
            assertThat(simulationResult.getChangedInstances(key))
                    .as(key)
                    .isEqualTo(value);

            //Check result content
            assertThat(result.getChangedInstances(key))
                    .as(key)
                    .isEqualTo(value);
        });

        //Check that we can complete the active instances that we have changed
        completeTask(caseInstanceIdAActive);
        assertCaseInstanceEnded(caseInstanceIdAActive);
        completeTask(caseInstanceIdBActive);
        assertCaseInstanceEnded(caseInstanceIdBActive);
        completeTask(caseInstanceIdCActive);
        assertCaseInstanceEnded(caseInstanceIdCActive);

        assertThat(eventListener.events).hasSize(1);
        FlowableChangeTenantIdEvent event = eventListener.events.get(0);
        assertThat(event.getEngineScopeType()).isEqualTo(ScopeTypes.CMMN);
        assertThat(event.getSourceTenantId()).isEqualTo(TEST_TENANT_A);
        assertThat(event.getTargetTenantId()).isEqualTo(TEST_TENANT_B);
        assertThat(event.getDefinitionTenantId()).isNull();
    }

    @Test
    public void testChangeTenantIdCaseInstanceFromEmptyTenant() {
        //testDeployments {
        assertThat(cmmnRepositoryService.createDeploymentQuery().count()).isEqualTo(5);
        assertThat(cmmnRepositoryService.createDeploymentQuery().deploymentWithoutTenantId().count()).isEqualTo(1);
        assertThat(cmmnRepositoryService.createDeploymentQuery().deploymentTenantId(TEST_TENANT_A).count()).isEqualTo(2);
        assertThat(cmmnRepositoryService.createDeploymentQuery().deploymentTenantId(TEST_TENANT_B).count()).isEqualTo(1);
        assertThat(cmmnRepositoryService.createDeploymentQuery().deploymentTenantId(TEST_TENANT_C).count()).isEqualTo(1);

        // Starting case instances that will be sent to the history
        String caseInstanceIdNoTenantCompleted = startCase(TEST_TENANT_A, "caseWithMilestone", "caseInstanceACompleted", 2, "");
        String caseInstanceIdBCompleted = startCase(TEST_TENANT_B, "caseWithMilestone", "caseInstanceBCompleted", 2);
        String caseInstanceIdCCompleted = startCase(TEST_TENANT_C, "caseWithMilestone", "caseInstanceCCompleted", 2);

        // Starting case instances that will be kept active
        String caseInstanceIdNoTenantActive = startCase(TEST_TENANT_A, "caseWithMilestone", "caseInstanceAActive", 1, "");
        String caseInstanceIdBActive = startCase(TEST_TENANT_B, "caseWithMilestone", "caseInstanceBActive", 1);
        String caseInstanceIdCActive = startCase(TEST_TENANT_C, "caseWithMilestone", "caseInstanceCActive", 1);
        String caseInstanceIdNoTenantForJobs = startCase(TEST_TENANT_A, "caseForJobsAndEventSubscriptions", "caseInstanceAForJobs", 0, "");
        Job jobToBeSentToDeadLetter = cmmnManagementService.createTimerJobQuery().caseInstanceId(caseInstanceIdNoTenantForJobs)
                .elementName("Timer for a Deadletter Job").singleResult();
        Job jobInTheDeadLetterQueue = cmmnManagementService.moveJobToDeadLetterJob(jobToBeSentToDeadLetter.getId());
        assertThat(jobInTheDeadLetterQueue).as("We have a job in the deadletter queue.").isNotNull();

        Set<String> caseInstanceIdsNoTenant = new HashSet<>(Arrays.asList(caseInstanceIdNoTenantCompleted, caseInstanceIdNoTenantActive));
        Set<String> caseInstanceIdsTenantB = new HashSet<>(Arrays.asList(caseInstanceIdBCompleted, caseInstanceIdBActive));
        Set<String> caseInstanceIdsTenantC = new HashSet<>(Arrays.asList(caseInstanceIdCCompleted, caseInstanceIdCActive));

        // Prior to changing the Tenant Id, all elements are associated to the original tenant
        checkTenantIdForAllInstances(caseInstanceIdsNoTenant, "", "prior to changing to " + TEST_TENANT_B);
        checkTenantIdForAllInstances(caseInstanceIdsTenantB, TEST_TENANT_B, "prior to changing to " + TEST_TENANT_B);
        checkTenantIdForAllInstances(caseInstanceIdsTenantC, TEST_TENANT_C, "prior to changing to " + TEST_TENANT_B);

        // First we simulate the change
        ChangeTenantIdResult simulationResult = cmmnManagementService.createChangeTenantIdBuilder("", TEST_TENANT_B).simulate();

        // All the instances should stay in the original tenant after the simulation
        checkTenantIdForAllInstances(caseInstanceIdsNoTenant, "", "after simulating the change to " + TEST_TENANT_B);
        checkTenantIdForAllInstances(caseInstanceIdsTenantB, TEST_TENANT_B, "after simulating the change to " + TEST_TENANT_B);
        checkTenantIdForAllInstances(caseInstanceIdsTenantC, TEST_TENANT_C, "after simulating the change to " + TEST_TENANT_B);

        // We now proceed with the changeTenantId operation for all the instances
        ChangeTenantIdResult result = cmmnManagementService.createChangeTenantIdBuilder("", TEST_TENANT_B).complete();

        // All the instances should now be assigned to the tenant B
        checkTenantIdForAllInstances(caseInstanceIdsNoTenant, TEST_TENANT_B, "after the change to " + TEST_TENANT_B);
        checkTenantIdForAllInstances(caseInstanceIdsTenantB, TEST_TENANT_B, "after the change to " + TEST_TENANT_B);

        // The instances for tenant C remain untouched
        checkTenantIdForAllInstances(caseInstanceIdsTenantC, TEST_TENANT_C, "after the change to " + TEST_TENANT_B);

        //Expected results map
        Map<String, Long> resultMap = new HashMap<>();
        resultMap.put(CmmnChangeTenantIdEntityTypes.CASE_INSTANCES, 2L);
        resultMap.put(CmmnChangeTenantIdEntityTypes.PLAN_ITEM_INSTANCES, 10L);
        resultMap.put(CmmnChangeTenantIdEntityTypes.MILESTONE_INSTANCES, 1L);
        resultMap.put(CmmnChangeTenantIdEntityTypes.HISTORIC_CASE_INSTANCES, 3L);
        resultMap.put(CmmnChangeTenantIdEntityTypes.HISTORIC_PLAN_ITEM_INSTANCES, 15L);
        resultMap.put(CmmnChangeTenantIdEntityTypes.HISTORIC_MILESTONE_INSTANCES, 2L);
        resultMap.put(CmmnChangeTenantIdEntityTypes.DEADLETTER_JOBS, 1L);
        resultMap.put(CmmnChangeTenantIdEntityTypes.EVENT_SUBSCRIPTIONS, 1L);
        resultMap.put(CmmnChangeTenantIdEntityTypes.EXTERNAL_WORKER_JOBS, 1L);
        resultMap.put(CmmnChangeTenantIdEntityTypes.HISTORIC_TASK_INSTANCES, 4L);
        resultMap.put(CmmnChangeTenantIdEntityTypes.HISTORIC_TASK_LOG_ENTRIES, 7L);
        resultMap.put(CmmnChangeTenantIdEntityTypes.JOBS, 1L);
        resultMap.put(CmmnChangeTenantIdEntityTypes.SUSPENDED_JOBS, 0L);
        resultMap.put(CmmnChangeTenantIdEntityTypes.TASKS, 1L);
        resultMap.put(CmmnChangeTenantIdEntityTypes.TIMER_JOBS, 1L);

        //Check that all the entities are returned
        assertThat(simulationResult.getChangedEntityTypes()).containsExactlyInAnyOrderElementsOf(resultMap.keySet());
        assertThat(result.getChangedEntityTypes()).containsExactlyInAnyOrderElementsOf(resultMap.keySet());

        resultMap.forEach((key, value) -> {
            //Check simulation result content
            assertThat(simulationResult.getChangedInstances(key))
                    .as(key)
                    .isEqualTo(value);

            //Check result content
            assertThat(result.getChangedInstances(key))
                    .as(key)
                    .isEqualTo(value);
        });

        //Check that we can complete the active instances that we have changed
        completeTask(caseInstanceIdNoTenantActive);
        assertCaseInstanceEnded(caseInstanceIdNoTenantActive);
        completeTask(caseInstanceIdBActive);
        assertCaseInstanceEnded(caseInstanceIdBActive);
        completeTask(caseInstanceIdCActive);
        assertCaseInstanceEnded(caseInstanceIdCActive);

        assertThat(eventListener.events).hasSize(1);
        FlowableChangeTenantIdEvent event = eventListener.events.get(0);
        assertThat(event.getEngineScopeType()).isEqualTo(ScopeTypes.CMMN);
        assertThat(event.getSourceTenantId()).isEqualTo("");
        assertThat(event.getTargetTenantId()).isEqualTo(TEST_TENANT_B);
        assertThat(event.getDefinitionTenantId()).isNull();
    }

    private String startCase(String tenantId, String caseDefinitionKey, String name, int completeTaskLoops) {
        return startCase(tenantId, caseDefinitionKey, name, completeTaskLoops, null);
    }

    private String startCase(String tenantId, String caseDefinitionKey, String name, int completeTaskLoops, String overrideTenantId) {
        CaseInstanceBuilder caseInstanceBuilder = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey(caseDefinitionKey).name(name)
                .tenantId(tenantId).fallbackToDefaultTenant();
        if (overrideTenantId != null) {
            caseInstanceBuilder.overrideCaseDefinitionTenantId(overrideTenantId);
        }
        CaseInstance caseInstance = caseInstanceBuilder.start();
        for (int i = 0; i < completeTaskLoops; i++) {
            completeTask(caseInstance.getId());
        }
        return caseInstance.getId();
    }

    private void completeTask(String caseInstanceId) {
        List<Task> tasks;
        tasks = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstanceId).list();
        cmmnTaskService.complete(tasks.get(0).getId());
    }

    private void checkTenantIdForAllInstances(Set<String> caseInstanceIds, String expectedTenantId, String moment) {
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceIds(caseInstanceIds).list()).isNotEmpty().allSatisfy(ci -> {
            assertThat(StringUtils.defaultIfEmpty(ci.getTenantId(), "")).as("Active case instance '%s' %s must belong to %s but belongs to %s.", ci.getName(),
                    moment, expectedTenantId, ci.getTenantId()).isEqualTo(expectedTenantId);

            assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(ci.getId()).list()).isNotEmpty().allSatisfy(
                    pii -> assertThat(StringUtils.defaultIfEmpty(pii.getTenantId(), "")).as(
                            "Active plan item instance %s from %s %s must belong to %s but belongs to %s.", pii.getName(), ci.getName(), moment,
                            expectedTenantId, pii.getTenantId()).isEqualTo(expectedTenantId));

            cmmnRuntimeService.createMilestoneInstanceQuery().milestoneInstanceCaseInstanceId(ci.getId()).list().forEach(
                    am -> assertThat(StringUtils.defaultIfEmpty(am.getTenantId(), "")).as(
                            "Active milestone instance %s from %s %s must belong to %s but belongs to %s.", am.getName(), ci.getName(), moment,
                            expectedTenantId, am.getTenantId()).isEqualTo(expectedTenantId));

            assertThat(cmmnRuntimeService.createEventSubscriptionQuery().scopeType(ScopeTypes.CMMN).scopeId(ci.getId()).list()).allSatisfy(
                    eve -> assertThat(StringUtils.defaultIfEmpty(eve.getTenantId(), "")).as(
                            "Event Subscription %s from %s %s must belong to %s but belongs to %s.", eve.getId(), ci.getName(), moment, expectedTenantId,
                            eve.getTenantId()).isEqualTo(expectedTenantId));

            assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(ci.getId()).list()).allSatisfy(
                    task -> assertThat(StringUtils.defaultIfEmpty(task.getTenantId(), "")).as("Task %s from %s %s must belong to %s but belongs to %s.",
                            task.getName(), ci.getName(), moment, expectedTenantId, task.getTenantId()).isEqualTo(expectedTenantId));

            assertThat(cmmnManagementService.createJobQuery().caseInstanceId(ci.getId()).list()).allSatisfy(
                    job -> assertThat(StringUtils.defaultIfEmpty(job.getTenantId(), "")).as("Job %s from %s %s must belong to %s but belongs to %s.",
                            job.getId(), ci.getName(), moment, expectedTenantId, job.getTenantId()).isEqualTo(expectedTenantId));

            assertThat(cmmnManagementService.createDeadLetterJobQuery().processInstanceId(ci.getId()).list()).allSatisfy(
                    job -> assertThat(StringUtils.defaultIfEmpty(job.getTenantId(), "")).as(
                            "Dead Letter Job %s from %s %s must belong to %s but belongs to %s.", job.getId(), ci.getName(), moment, expectedTenantId,
                            job.getTenantId()).isEqualTo(expectedTenantId));

            assertThat(cmmnManagementService.createTimerJobQuery().caseInstanceId(ci.getId()).list()).allSatisfy(
                    job -> assertThat(StringUtils.defaultIfEmpty(job.getTenantId(), "")).as("Timer Job %s from %s %s must belong to %s but belongs to %s.",
                            job.getId(), ci.getName(), moment, expectedTenantId, job.getTenantId()).isEqualTo(expectedTenantId));

            assertThat(cmmnManagementService.createExternalWorkerJobQuery().caseInstanceId(ci.getId()).list()).allSatisfy(
                    job -> assertThat(StringUtils.defaultIfEmpty(job.getTenantId(), "")).as(
                            "External Worker Job %s from %s %s must belong to %s but belongs to %s.", job.getId(), ci.getName(), moment, expectedTenantId,
                            job.getTenantId()).isEqualTo(expectedTenantId));
        });

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {

            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceIds(caseInstanceIds).list()).hasSize(caseInstanceIds.size())
                    .allSatisfy(hci -> {
                        assertThat(StringUtils.defaultIfEmpty(hci.getTenantId(), "")).as("Historic case instances '%s' %s must belong to %s but belongs to %s.",
                                hci.getName(), moment, expectedTenantId, hci.getTenantId()).isEqualTo(expectedTenantId);

                        assertThat(cmmnHistoryService.createHistoricPlanItemInstanceQuery().planItemInstanceCaseInstanceId(hci.getId()).list()).isNotEmpty()
                                .allSatisfy(hpii -> assertThat(StringUtils.defaultIfEmpty(hpii.getTenantId(), "")).as(
                                        "Historic plan item instance %s from %s %s must belong to %s but belongs to %s.", hpii.getName(), hci.getName(), moment,
                                        expectedTenantId, hpii.getTenantId()).isEqualTo(expectedTenantId));

                        assertThat(cmmnHistoryService.createHistoricMilestoneInstanceQuery().milestoneInstanceCaseInstanceId(hci.getId()).list()).allSatisfy(
                                hm -> assertThat(StringUtils.defaultIfEmpty(hm.getTenantId(), "")).as(
                                        "Historic milestone instance %s from %s %s must belong to %s but belongs to %s.", hm.getName(), hci.getName(), moment,
                                        expectedTenantId, hm.getTenantId()).isEqualTo(expectedTenantId));

                        assertThat(cmmnHistoryService.createHistoricTaskInstanceQuery().caseInstanceId(hci.getId()).list()).allSatisfy(
                                task -> assertThat(StringUtils.defaultIfEmpty(task.getTenantId(), "")).as(
                                        "Historic Task %s from %s %s must belong to %s but belongs to %s.", task.getName(), hci.getName(), moment,
                                        expectedTenantId, task.getTenantId()).isEqualTo(expectedTenantId));

                        assertThat(cmmnHistoryService.createHistoricTaskLogEntryQuery().caseInstanceId(hci.getId()).list()).allSatisfy(
                                log -> assertThat(StringUtils.defaultIfEmpty(log.getTenantId(), "")).as(
                                        "Historic Task Log Entry %s from %s %s must belong to %s but belongs to %s.", log.getLogNumber(), hci.getName(), moment,
                                        expectedTenantId, log.getTenantId()).isEqualTo(expectedTenantId));
                    });
        }
    }

    @Test
    public void changeTenantIdCaseInstanceWithDefinedDefinitionTenant() {
        // In this test we will mark the instances created with a definition from the
        // default tenant with DT

        // Starting case instances that will be sent to the history
        String caseInstanceIdACompleted = startCase(TEST_TENANT_A, "caseWithMilestone", "caseInstanceACompleted", 2);
        String caseInstanceIdADTCompleted = startCase(TEST_TENANT_A, "caseWithMilestoneDup", "caseInstanceADTCompleted", 2,
                TEST_TENANT_A); // For this instance we want to override the tenant Id.
        String caseInstanceIdBCompleted = startCase(TEST_TENANT_B, "caseWithMilestone", "caseInstanceBCompleted", 2);
        String caseInstanceIdCCompleted = startCase(TEST_TENANT_C, "caseWithMilestone", "caseInstanceCCompleted", 2);

        // Starting case instances that will be kept active
        String caseInstanceIdAActive = startCase(TEST_TENANT_A, "caseWithMilestone", "caseInstanceAActive", 1);
        String caseInstanceIdADTActive = startCase(TEST_TENANT_A, "caseWithMilestoneDup", "caseInstanceADTActive", 1,
                TEST_TENANT_A); // For this instance we want to override the tenant Id.
        String caseInstanceIdBActive = startCase(TEST_TENANT_B, "caseWithMilestone", "caseInstanceBActive", 1);
        String caseInstanceIdCActive = startCase(TEST_TENANT_C, "caseWithMilestone", "caseInstanceCActive", 1);

        Set<String> caseInstanceIdsTenantADTOnly = new HashSet<>(Arrays.asList(caseInstanceIdADTCompleted, caseInstanceIdADTActive));
        Set<String> caseInstanceIdsTenantANotDT = new HashSet<>(Arrays.asList(caseInstanceIdACompleted, caseInstanceIdAActive));
        Set<String> caseInstanceIdsTenantAAll = new HashSet<>(
                Arrays.asList(caseInstanceIdADTCompleted, caseInstanceIdADTActive, caseInstanceIdACompleted, caseInstanceIdAActive));
        Set<String> caseInstanceIdsTenantB = new HashSet<>(Arrays.asList(caseInstanceIdBCompleted, caseInstanceIdBActive));
        Set<String> caseInstanceIdsTenantC = new HashSet<>(Arrays.asList(caseInstanceIdCCompleted, caseInstanceIdCActive));

        // Prior to changing the Tenant Id, all elements are associate to the original
        // tenant
        checkTenantIdForAllInstances(caseInstanceIdsTenantAAll, TEST_TENANT_A, "prior to changing to " + TEST_TENANT_B);
        checkTenantIdForAllInstances(caseInstanceIdsTenantB, TEST_TENANT_B, "prior to changing to " + TEST_TENANT_B);
        checkTenantIdForAllInstances(caseInstanceIdsTenantC, TEST_TENANT_C, "prior to changing to " + TEST_TENANT_B);

        // First we simulate the change
        ChangeTenantIdResult simulationResult = cmmnManagementService.createChangeTenantIdBuilder(TEST_TENANT_A, TEST_TENANT_B)
                .definitionTenantId("").simulate();

        // All the instances should stay in the original tenant after the simulation
        checkTenantIdForAllInstances(caseInstanceIdsTenantAAll, TEST_TENANT_A, "after simulating the change to " + TEST_TENANT_B);
        checkTenantIdForAllInstances(caseInstanceIdsTenantB, TEST_TENANT_B, "after simulating the change to " + TEST_TENANT_B);
        checkTenantIdForAllInstances(caseInstanceIdsTenantC, TEST_TENANT_C, "after simulating the change to " + TEST_TENANT_B);

        // We now proceed with the changeTenantId operation for all the instances
        ChangeTenantIdResult result = cmmnManagementService.createChangeTenantIdBuilder(TEST_TENANT_A, TEST_TENANT_B)
                .definitionTenantId("").complete();

        // All the instances from the default tenant should now be assigned to the tenant B
        checkTenantIdForAllInstances(caseInstanceIdsTenantADTOnly, TEST_TENANT_B, "after the change to " + TEST_TENANT_B);

        // But the instances that were created with a definition from tenant A must stay in tenant A
        checkTenantIdForAllInstances(caseInstanceIdsTenantANotDT, TEST_TENANT_A, "after the change to " + TEST_TENANT_B);

        // The instances from Tenant B are still associated to tenant B
        checkTenantIdForAllInstances(caseInstanceIdsTenantB, TEST_TENANT_B, "after the change to " + TEST_TENANT_B);

        // The instances for tenant C remain untouched in tenant C
        checkTenantIdForAllInstances(caseInstanceIdsTenantC, TEST_TENANT_C, "after the change to " + TEST_TENANT_B);

        //Expected results map
        Map<String, Long> resultMap = new HashMap<>();
        resultMap.put(CmmnChangeTenantIdEntityTypes.CASE_INSTANCES, 1L);
        resultMap.put(CmmnChangeTenantIdEntityTypes.PLAN_ITEM_INSTANCES, 5L);
        resultMap.put(CmmnChangeTenantIdEntityTypes.MILESTONE_INSTANCES, 1L);
        resultMap.put(CmmnChangeTenantIdEntityTypes.HISTORIC_CASE_INSTANCES, 2L);
        resultMap.put(CmmnChangeTenantIdEntityTypes.HISTORIC_PLAN_ITEM_INSTANCES, 10L);
        resultMap.put(CmmnChangeTenantIdEntityTypes.HISTORIC_MILESTONE_INSTANCES, 2L);
        resultMap.put(CmmnChangeTenantIdEntityTypes.DEADLETTER_JOBS, 0L);
        resultMap.put(CmmnChangeTenantIdEntityTypes.EVENT_SUBSCRIPTIONS, 0L);
        resultMap.put(CmmnChangeTenantIdEntityTypes.EXTERNAL_WORKER_JOBS, 0L);
        resultMap.put(CmmnChangeTenantIdEntityTypes.HISTORIC_TASK_INSTANCES, 4L);
        resultMap.put(CmmnChangeTenantIdEntityTypes.HISTORIC_TASK_LOG_ENTRIES, 7L);
        resultMap.put(CmmnChangeTenantIdEntityTypes.JOBS, 0L);
        resultMap.put(CmmnChangeTenantIdEntityTypes.SUSPENDED_JOBS, 0L);
        resultMap.put(CmmnChangeTenantIdEntityTypes.TASKS, 1L);
        resultMap.put(CmmnChangeTenantIdEntityTypes.TIMER_JOBS, 0L);

        //Check that all the entities are returned
        assertThat(simulationResult.getChangedEntityTypes())
                .containsExactlyInAnyOrderElementsOf(resultMap.keySet());
        assertThat(result.getChangedEntityTypes())
                .containsExactlyInAnyOrderElementsOf(resultMap.keySet());

        resultMap.forEach((key, value) -> {
            //Check simulation result content
            assertThat(simulationResult.getChangedInstances(key))
                    .as(key)
                    .isEqualTo(value);

            //Check result content
            assertThat(result.getChangedInstances(key))
                    .as(key)
                    .isEqualTo(value);
        });

        //Check that we can complete the active instances that we have changed
        completeTask(caseInstanceIdAActive);
        assertCaseInstanceEnded(caseInstanceIdAActive);
        completeTask(caseInstanceIdADTActive);
        assertCaseInstanceEnded(caseInstanceIdADTActive);
        completeTask(caseInstanceIdBActive);
        assertCaseInstanceEnded(caseInstanceIdBActive);
        completeTask(caseInstanceIdCActive);
        assertCaseInstanceEnded(caseInstanceIdCActive);

        assertThat(eventListener.events).hasSize(1);
        FlowableChangeTenantIdEvent event = eventListener.events.get(0);
        assertThat(event.getEngineScopeType()).isEqualTo(ScopeTypes.CMMN);
        assertThat(event.getSourceTenantId()).isEqualTo(TEST_TENANT_A);
        assertThat(event.getTargetTenantId()).isEqualTo(TEST_TENANT_B);
        assertThat(event.getDefinitionTenantId()).isEqualTo("");
    }

    protected static class TestEventListener extends AbstractFlowableEventListener {

        protected final List<FlowableChangeTenantIdEvent> events = new ArrayList<>();

        @Override
        public void onEvent(FlowableEvent event) {
            if (event instanceof FlowableChangeTenantIdEvent) {
                events.add((FlowableChangeTenantIdEvent) event);
            }
        }

        @Override
        public boolean isFailOnException() {
            return true;
        }

        @Override
        public Collection<? extends FlowableEventType> getTypes() {
            return Collections.singleton(FlowableEngineEventType.CHANGE_TENANT_ID);
        }
    }
}
