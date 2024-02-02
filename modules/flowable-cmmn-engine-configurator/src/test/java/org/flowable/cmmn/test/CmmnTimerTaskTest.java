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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Date;
import java.util.List;

import org.flowable.cmmn.api.repository.CmmnDeployment;
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.api.runtime.PlanItemDefinitionType;
import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstanceState;
import org.flowable.cmmn.engine.test.impl.CmmnJobTestHelper;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.test.JobTestHelper;
import org.flowable.job.api.Job;
import org.flowable.task.api.Task;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Tijs Rademakers
 */
public class CmmnTimerTaskTest extends AbstractProcessEngineIntegrationTest {

    @Before
    public void deployTimerProcess() {
        if (cmmnRepositoryService.createDeploymentQuery().count() == 0) {
            cmmnRepositoryService.createDeployment().addClasspathResource("org/flowable/cmmn/test/timerInStage.cmmn").deploy();
        }
    }

    @Test
    public void testCmmnTimerTask() {
        Date startTime = setCmmnClockFixedToCurrentTime();
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testTimerInStage").start();

        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemInstanceState(PlanItemInstanceState.ACTIVE)
                .count()).isEqualTo(1);
        PlanItemInstance planItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .planItemInstanceState(PlanItemInstanceState.AVAILABLE)
                .planItemDefinitionType(PlanItemDefinitionType.TIMER_EVENT_LISTENER)
                .singleResult();
        assertThat(planItemInstance).isNotNull();

        assertThat(cmmnTaskService.createTaskQuery().count()).isEqualTo(1);

        List<Job> timerJobs = processEngineManagementService.createTimerJobQuery().scopeId(caseInstance.getId()).scopeType(ScopeTypes.CMMN).list();
        assertThat(timerJobs).hasSize(1);
        String timerJobId = timerJobs.get(0).getId();

        timerJobs = processEngineManagementService.createTimerJobQuery().scopeId(caseInstance.getId()).scopeType(ScopeTypes.CMMN).executable().list();
        assertThat(timerJobs).isEmpty();

        processEngine.getProcessEngineConfiguration().getClock().setCurrentTime(new Date(startTime.getTime() + (3 * 60 * 60 * 1000 + 1)));

        timerJobs = processEngineManagementService.createTimerJobQuery().scopeId(caseInstance.getId()).scopeType(ScopeTypes.CMMN).executable().list();
        assertThat(timerJobs).hasSize(1);

        assertThatThrownBy(() -> JobTestHelper
                .waitForJobExecutorToProcessAllJobsAndExecutableTimerJobs(processEngine.getProcessEngineConfiguration(), processEngineManagementService, 7000,
                        200, true))
                .isInstanceOf(FlowableException.class)
                .hasMessage("time limit of 7000 was exceeded");

        // Timer fires after 3 hours, so setting it to 3 hours + 1 second
        cmmnEngineConfiguration.getClock().setCurrentTime(new Date(startTime.getTime() + (3 * 60 * 60 * 1000 + 1)));

        timerJobs = cmmnManagementService.createTimerJobQuery().caseInstanceId(caseInstance.getId()).executable().list();
        assertThat(timerJobs)
                .extracting(Job::getId)
                .containsExactly(timerJobId);

        CmmnJobTestHelper.waitForJobExecutorToProcessAllJobs(cmmnEngineConfiguration, 7000L, 200L, true);

        // User task should be active after the timer has triggered
        List<Task> tasks = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).orderByTaskName().asc().list();
        assertThat(tasks)
                .extracting(Task::getName)
                .containsExactly("A", "B");

        cmmnEngineConfiguration.resetClock();
        ((ProcessEngineConfigurationImpl) processEngine.getProcessEngineConfiguration()).resetClock();

        for (CmmnDeployment deployment : cmmnRepositoryService.createDeploymentQuery().list()) {
            cmmnRepositoryService.deleteDeployment(deployment.getId(), true);
        }
    }

}
