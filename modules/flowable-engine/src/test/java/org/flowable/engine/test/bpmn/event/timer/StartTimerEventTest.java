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
package org.flowable.engine.test.bpmn.event.timer;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;

import org.flowable.common.engine.impl.Page;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.interceptor.CommandExecutor;
import org.flowable.common.engine.impl.util.IoUtil;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.runtime.ProcessInstanceQuery;
import org.flowable.engine.test.Deployment;
import org.flowable.job.api.Job;
import org.flowable.job.api.TimerJobQuery;
import org.flowable.job.service.impl.asyncexecutor.AsyncExecutor;
import org.flowable.job.service.impl.cmd.CancelJobsCmd;
import org.flowable.job.service.impl.persistence.entity.TimerJobEntity;
import org.junit.jupiter.api.Test;

/**
 * @author Joram Barrez
 * @author Filip Hrisafov
 */
public class StartTimerEventTest extends PluggableFlowableTestCase {

    @Test
    @Deployment
    public void testDurationStartTimerEvent() throws Exception {
        try {
            // After process start, there should be timer created
            TimerJobQuery jobQuery = managementService.createTimerJobQuery();
            assertThat(jobQuery.count()).isEqualTo(1);
    
            Date startTime = Date.from(Instant.now().plus(2, ChronoUnit.HOURS));
            processEngineConfiguration.getClock().setCurrentTime(startTime);
            waitForJobExecutorToProcessAllJobs(5000L, 200L);
    
            List<ProcessInstance> pi = runtimeService.createProcessInstanceQuery().processDefinitionKey("startTimerEventExample").list();
            assertThat(pi).hasSize(1);
    
            assertThat(jobQuery.count()).isZero();
            
        } finally {
            processEngineConfiguration.resetClock();
        }
    }
    
    @Test
    @Deployment
    public void testDurationStartTimerEventWithCategory() throws Exception {
        try {
            // After process start, there should be timer created
            TimerJobQuery jobQuery = managementService.createTimerJobQuery();
            assertThat(jobQuery.count()).isEqualTo(1);
            assertThat(jobQuery.singleResult().getCategory()).isEqualTo("myCategory");
    
            Date startTime = Date.from(Instant.now().plus(2, ChronoUnit.HOURS));
            processEngineConfiguration.getClock().setCurrentTime(startTime);
            waitForJobExecutorToProcessAllJobs(5000L, 200L);
    
            List<ProcessInstance> pi = runtimeService.createProcessInstanceQuery().processDefinitionKey("startTimerEventExample").list();
            assertThat(pi).hasSize(1);
    
            assertThat(jobQuery.count()).isZero();
            
        } finally {
            processEngineConfiguration.resetClock();
        }
    }
    
    @Test
    @Deployment(resources = "org/flowable/engine/test/bpmn/event/timer/StartTimerEventTest.testDurationStartTimerEventWithCategory.bpmn20.xml")
    public void testDurationStartTimerEventWithCategoryAndEnabledConfigurationSet() throws Exception {
        try {
            processEngineConfiguration.getJobServiceConfiguration().addEnabledJobCategory("testValue");
            
            // After process start, there should be timer created
            TimerJobQuery jobQuery = managementService.createTimerJobQuery();
            assertThat(jobQuery.count()).isEqualTo(1);
            assertThat(jobQuery.singleResult().getCategory()).isEqualTo("myCategory");
    
            Date startTime = Date.from(Instant.now().plus(2, ChronoUnit.HOURS));
            processEngineConfiguration.getClock().setCurrentTime(startTime);
            
            assertThat(getTimerJobsCount()).isZero();
            
            processEngineConfiguration.getJobServiceConfiguration().addEnabledJobCategory("myCategory");
            
            assertThat(getTimerJobsCount()).isEqualTo(1);
            
            waitForJobExecutorToProcessAllJobs(5000L, 200L);
    
            assertThat(runtimeService.createProcessInstanceQuery().processDefinitionKey("startTimerEventExample").count()).isEqualTo(1);
            
            assertThat(jobQuery.count()).isZero();
            
        } finally {
            processEngineConfiguration.resetClock();
            processEngineConfiguration.getJobServiceConfiguration().setEnabledJobCategories(null);
        }
    }

    @Test
    @Deployment
    public void testFixedDateStartTimerEvent() throws Exception {
        try {
            // After process start, there should be timer created
            TimerJobQuery jobQuery = managementService.createTimerJobQuery();
            assertThat(jobQuery.count()).isEqualTo(1);
    
            processEngineConfiguration.getClock().setCurrentTime(new SimpleDateFormat("dd/MM/yyyy hh:mm:ss").parse("15/11/2036 11:12:30"));
            waitForJobExecutorToProcessAllJobs(7000L, 200L);
    
            List<ProcessInstance> pi = runtimeService.createProcessInstanceQuery().processDefinitionKey("startTimerEventExample").list();
            assertThat(pi).hasSize(1);
    
            assertThat(jobQuery.count()).isZero();
            
        } finally {
            processEngineConfiguration.resetClock();
        }
    }

    @Test
    @Deployment
    public void testCycleDateStartTimerEvent() throws Exception {
        try {
            // We need to make sure the time ends on .000, .003 or .007 due to SQL Server rounding to that
            processEngineConfiguration.getClock().setCurrentTime(Date.from(Instant.now().truncatedTo(ChronoUnit.SECONDS)));
    
            // After process start, there should be timer created
            TimerJobQuery jobQuery = managementService.createTimerJobQuery();
            assertThat(jobQuery.count()).isEqualTo(1);
    
            final ProcessInstanceQuery piq = runtimeService.createProcessInstanceQuery().processDefinitionKey("startTimerEventExample");
    
            moveByMinutes(6);
            waitForJobExecutorOnCondition(4000, 500, new Callable<Boolean>() {
                @Override
                public Boolean call() throws Exception {
                    return 1 == piq.count();
                }
            });
    
            assertThat(jobQuery.count()).isEqualTo(1);
    
            moveByMinutes(6);
            waitForJobExecutorOnCondition(4000, 500, new Callable<Boolean>() {
                @Override
                public Boolean call() throws Exception {
                    return 2 == piq.count();
                }
            });
    
            assertThat(jobQuery.count()).isEqualTo(1);
            // have to manually delete pending timer
            cleanDB();
            
        } finally {
            processEngineConfiguration.resetClock();
        }
    }
    
    @Test
    @Deployment
    public void testCycleDateStartTimerEventWithCategory() throws Exception {
        try {
            // We need to make sure the time ends on .000, .003 or .007 due to SQL Server rounding to that
            processEngineConfiguration.getClock().setCurrentTime(Date.from(Instant.now().truncatedTo(ChronoUnit.SECONDS)));
    
            // After process start, there should be timer created
            TimerJobQuery jobQuery = managementService.createTimerJobQuery();
            assertThat(jobQuery.count()).isEqualTo(1);
            assertThat(jobQuery.singleResult().getCategory()).isEqualTo("myCategory");
    
            final ProcessInstanceQuery piq = runtimeService.createProcessInstanceQuery().processDefinitionKey("startTimerEventExample");
    
            moveByMinutes(6);
            waitForJobExecutorOnCondition(4000, 500, new Callable<Boolean>() {
                @Override
                public Boolean call() throws Exception {
                    return 1 == piq.count();
                }
            });
    
            assertThat(jobQuery.count()).isEqualTo(1);
            assertThat(jobQuery.singleResult().getCategory()).isEqualTo("myCategory");
    
            moveByMinutes(6);
            waitForJobExecutorOnCondition(4000, 500, new Callable<Boolean>() {
                @Override
                public Boolean call() throws Exception {
                    return 2 == piq.count();
                }
            });
    
            assertThat(jobQuery.count()).isEqualTo(1);
            assertThat(jobQuery.singleResult().getCategory()).isEqualTo("myCategory");
            
            // have to manually delete pending timer
            cleanDB();
            
        } finally {
            processEngineConfiguration.resetClock();
        }
    }

    private void moveByMinutes(int minutes) throws Exception {
        processEngineConfiguration.getClock().setCurrentTime(new Date(processEngineConfiguration.getClock().getCurrentTime().getTime() + ((minutes * 60 * 1000) + 5000)));
    }

    private void moveBy(Duration duration) {
        processEngineConfiguration.getClock().setCurrentTime(Date.from(processEngineConfiguration.getClock().getCurrentTime().toInstant().plus(duration)));
    }

    @Test
    @Deployment
    public void testCycleWithLimitStartTimerEvent() throws Exception {
        try {
            // We need to make sure the time ends on .000, .003 or .007 due to SQL Server rounding to that
            processEngineConfiguration.getClock().setCurrentTime(Date.from(Instant.now().truncatedTo(ChronoUnit.SECONDS).plusMillis(620)));
    
            // After process start, there should be timer created
            TimerJobQuery jobQuery = managementService.createTimerJobQuery();
            assertThat(jobQuery.count()).isEqualTo(1);
    
            moveByMinutes(6);
            String jobId = managementService.createTimerJobQuery().executable().singleResult().getId();
            managementService.moveTimerToExecutableJob(jobId);
            managementService.executeJob(jobId);
            assertThat(jobQuery.count()).isEqualTo(1);
    
            moveByMinutes(6);
            jobId = managementService.createTimerJobQuery().executable().singleResult().getId();
            managementService.moveTimerToExecutableJob(jobId);
            managementService.executeJob(jobId);
            assertThat(jobQuery.count()).isZero();
            
        } finally {
            processEngineConfiguration.resetClock();
        }
    }

    @Test
    public void testCycleWithRepeatStartTimeAndPeriodAndCurrentTimeBeforeStart() {
        try {
            // We need to make sure the time ends on .000, .003 or .007 due to SQL Server rounding to that
            Instant startTime = Instant.parse("2021-07-01T19:22:00.000Z");
            Instant currentTime = startTime.minus(5, ChronoUnit.MINUTES);
            processEngineConfiguration.getClock().setCurrentTime(Date.from(currentTime));

            repositoryService.createDeployment().addClasspathResource("org/flowable/engine/test/bpmn/event/timer/StartTimerEventTest.testCycleWithRepeatStartTimeAndPeriod.bpmn20.xml").deploy();

            // After process deployment, there should be timer created
            Job timerJob = managementService.createTimerJobQuery().singleResult();
            assertThat(timerJob).isNotNull();
            assertThat(timerJob.getDuedate()).isEqualTo(startTime);

            assertThat(runtimeService.createProcessInstanceQuery().count()).isEqualTo(0);

            moveBy(Duration.ofMinutes(6));
            waitForJobExecutorToProcessAllJobsAndExecutableTimerJobs(5500, 500);

            // After the first start there should be one process instance and one job
            assertThat(runtimeService.createProcessInstanceQuery().count()).isEqualTo(1);

            timerJob = managementService.createTimerJobQuery().singleResult();
            assertThat(timerJob).isNotNull();
            assertThat(timerJob.getDuedate()).isEqualTo(startTime.plus(1, ChronoUnit.DAYS));

            moveBy(Duration.ofDays(1));
            waitForJobExecutorToProcessAllJobsAndExecutableTimerJobs(5500, 500);

            // After the second start there should be two process instances and no jobs
            assertThat(runtimeService.createProcessInstanceQuery().count()).isEqualTo(2);

            timerJob = managementService.createTimerJobQuery().singleResult();
            assertThat(timerJob).isNull();
        } finally {
            processEngineConfiguration.resetClock();
            repositoryService.deleteDeployment(repositoryService.createDeploymentQuery().singleResult().getId(), true);
        }
    }

    @Test
    public void testCycleWithRepeatStartTimeAndPeriodAndCurrentTimeAfterStart() {
        try {
            // We need to make sure the time ends on .000, .003 or .007 due to SQL Server rounding to that
            Instant startTime = Instant.parse("2021-07-01T19:22:00.000Z");
            Instant currentTime = Instant.parse("2021-07-01T19:50:00.000Z");
            processEngineConfiguration.getClock().setCurrentTime(Date.from(currentTime));

            repositoryService.createDeployment().addClasspathResource("org/flowable/engine/test/bpmn/event/timer/StartTimerEventTest.testCycleWithRepeatStartTimeAndPeriod.bpmn20.xml").deploy();

            // After process deployment, there should be timer created for the next day
            Job timerJob = managementService.createTimerJobQuery().singleResult();
            assertThat(timerJob).isNotNull();
            assertThat(timerJob.getDuedate()).isEqualTo(startTime.plus(1, ChronoUnit.DAYS));

            assertThat(runtimeService.createProcessInstanceQuery().count()).isEqualTo(0);

            moveBy(Duration.ofDays(1));
            waitForJobExecutorToProcessAllJobsAndExecutableTimerJobs(5500, 500);

            // After the first start there should be one process instance and one job
            assertThat(runtimeService.createProcessInstanceQuery().count()).isEqualTo(1);

            timerJob = managementService.createTimerJobQuery().singleResult();
            assertThat(timerJob).isNotNull();
            assertThat(timerJob.getDuedate()).isEqualTo(startTime.plus(2, ChronoUnit.DAYS));

            moveBy(Duration.ofDays(1));
            waitForJobExecutorToProcessAllJobsAndExecutableTimerJobs(5500, 500);

            // After the second start there should be two process instances and no jobs
            assertThat(runtimeService.createProcessInstanceQuery().count()).isEqualTo(2);

            timerJob = managementService.createTimerJobQuery().singleResult();
            assertThat(timerJob).isNull();
        } finally {
            processEngineConfiguration.resetClock();
            repositoryService.deleteDeployment(repositoryService.createDeploymentQuery().singleResult().getId(), true);
        }
    }

    @Test
    public void testCycleWithRepeatStartTimePeriodAndEndAndCurrentTimeAfterStart() {
        try {
            // We need to make sure the time ends on .000, .003 or .007 due to SQL Server rounding to that
            Instant startTime = Instant.parse("2021-07-01T19:22:00.000Z");
            Instant currentTime = Instant.parse("2021-07-01T19:50:00.000Z");
            processEngineConfiguration.getClock().setCurrentTime(Date.from(currentTime));

            repositoryService.createDeployment().addClasspathResource("org/flowable/engine/test/bpmn/event/timer/StartTimerEventTest.testCycleWithRepeatStartTimePeriodAndEnd.bpmn20.xml").deploy();

            // After process deployment, there should be timer created for the next day
            Job timerJob = managementService.createTimerJobQuery().singleResult();
            assertThat(timerJob).isNotNull();
            assertThat(timerJob.getDuedate()).isEqualTo(startTime.plus(1, ChronoUnit.DAYS));

            assertThat(runtimeService.createProcessInstanceQuery().count()).isEqualTo(0);

            moveBy(Duration.ofDays(1));
            waitForJobExecutorToProcessAllJobsAndExecutableTimerJobs(5500, 500);

            // After the first start there should be one process instance and no jobs since the end date is reached
            assertThat(runtimeService.createProcessInstanceQuery().count()).isEqualTo(1);

            timerJob = managementService.createTimerJobQuery().singleResult();
            assertThat(timerJob).isNull();
        } finally {
            processEngineConfiguration.resetClock();
            repositoryService.deleteDeployment(repositoryService.createDeploymentQuery().singleResult().getId(), true);
        }
    }

    @Test
    @Deployment
    public void testExpressionStartTimerEvent() throws Exception {
        try {
            // ACT-1415: fixed start-date is an expression
            TimerJobQuery jobQuery = managementService.createTimerJobQuery();
            assertThat(jobQuery.count()).isEqualTo(1);
    
            processEngineConfiguration.getClock().setCurrentTime(new SimpleDateFormat("dd/MM/yyyy hh:mm:ss").parse("15/11/2036 11:12:30"));
            waitForJobExecutorToProcessAllJobs(7000L, 200L);
    
            List<ProcessInstance> pi = runtimeService.createProcessInstanceQuery().processDefinitionKey("startTimerEventExample").list();
            assertThat(pi).hasSize(1);
    
            assertThat(jobQuery.count()).isZero();
            
        } finally {
            processEngineConfiguration.resetClock();
        }
    }

    @Test
    @Deployment
    public void testVersionUpgradeShouldCancelJobs() throws Exception {
        try {
            // We need to make sure the time ends on .000, .003 or .007 due to SQL Server rounding to that
            processEngineConfiguration.getClock().setCurrentTime(Date.from(Instant.now().truncatedTo(ChronoUnit.SECONDS).plusMillis(293)));
    
            // After process start, there should be timer created
            TimerJobQuery jobQuery = managementService.createTimerJobQuery();
            assertThat(jobQuery.count()).isEqualTo(1);
    
            // we deploy new process version, with some small change
            String process = new String(IoUtil.readInputStream(getClass().getResourceAsStream("StartTimerEventTest.testVersionUpgradeShouldCancelJobs.bpmn20.xml"), "")).replaceAll("beforeChange", "changed");
            String id = repositoryService.createDeployment().addInputStream("StartTimerEventTest.testVersionUpgradeShouldCancelJobs.bpmn20.xml", new ByteArrayInputStream(process.getBytes())).deploy().getId();
    
            assertThat(jobQuery.count()).isEqualTo(1);
    
            moveByMinutes(5);
            waitForJobExecutorOnCondition(10000, 500, new Callable<Boolean>() {
                @Override
                public Boolean call() throws Exception {
                    // we check that correct version was started
                    ProcessInstance processInstance = runtimeService.createProcessInstanceQuery().processDefinitionKey("startTimerEventExample").singleResult();
                    if (processInstance != null) {
                        String pi = processInstance.getId();
                        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(pi).list();
                        Execution activityExecution = null;
                        for (Execution execution : executions) {
                            if (!execution.getProcessInstanceId().equals(execution.getId())) {
                                activityExecution = execution;
                                break;
                            }
                        }
                        return activityExecution != null && "changed".equals(activityExecution.getActivityId());
                    } else {
                        return false;
                    }
                }
            });
            assertThat(jobQuery.count()).isEqualTo(1);
    
            cleanDB();
            repositoryService.deleteDeployment(id, true);
            
        } finally {
            processEngineConfiguration.resetClock();
        }
    }

    @Test
    @Deployment
    public void testTimerShouldNotBeRecreatedOnDeploymentCacheReboot() {

        // Just to be sure, I added this test. Sounds like something that could easily happen
        // when the order of deploy/parsing is altered.

        // After process start, there should be timer created
        TimerJobQuery jobQuery = managementService.createTimerJobQuery();
        assertThat(jobQuery.count()).isEqualTo(1);

        // Reset deployment cache
        processEngineConfiguration.getProcessDefinitionCache().clear();

        // Start one instance of the process definition, this will trigger a
        // cache reload
        runtimeService.startProcessInstanceByKey("startTimer");

        // No new jobs should have been created
        assertThat(jobQuery.count()).isEqualTo(1);
    }

    // Test for ACT-1533
    @Test
    public void testTimerShouldNotBeRemovedWhenUndeployingOldVersion() throws Exception {
        // Deploy test process
        String processXml = new String(IoUtil.readInputStream(getClass().getResourceAsStream("StartTimerEventTest.testTimerShouldNotBeRemovedWhenUndeployingOldVersion.bpmn20.xml"), ""));
        String firstDeploymentId = repositoryService.createDeployment().addInputStream("StartTimerEventTest.testVersionUpgradeShouldCancelJobs.bpmn20.xml",
                new ByteArrayInputStream(processXml.getBytes())).deploy().getId();

        // After process start, there should be timer created
        TimerJobQuery jobQuery = managementService.createTimerJobQuery();
        assertThat(jobQuery.count()).isEqualTo(1);

        // we deploy new process version, with some small change
        String processChanged = processXml.replaceAll("beforeChange", "changed");
        String secondDeploymentId = repositoryService.createDeployment().addInputStream("StartTimerEventTest.testVersionUpgradeShouldCancelJobs.bpmn20.xml",
                new ByteArrayInputStream(processChanged.getBytes())).deploy().getId();
        assertThat(jobQuery.count()).isEqualTo(1);

        // Remove the first deployment
        repositoryService.deleteDeployment(firstDeploymentId, true);

        // The removal of an old version should not affect timer deletion
        // ACT-1533: this was a bug, and the timer was deleted!
        assertThat(jobQuery.count()).isEqualTo(1);

        // Cleanup
        cleanDB();
        repositoryService.deleteDeployment(secondDeploymentId, true);
    }

    @Test
    public void testOldJobsDeletedOnRedeploy() {

        for (int i = 0; i < 3; i++) {

            repositoryService.createDeployment()
                    .addClasspathResource("org/flowable/engine/test/bpmn/event/timer/StartTimerEventTest.testOldJobsDeletedOnRedeploy.bpmn20.xml")
                    .deploy();

            assertThat(repositoryService.createDeploymentQuery().count()).isEqualTo(i + 1);
            assertThat(repositoryService.createProcessDefinitionQuery().count()).isEqualTo(i + 1);
            assertThat(managementService.createTimerJobQuery().count()).isEqualTo(1);

        }

        // Cleanup
        for (ProcessDefinition processDefinition : repositoryService.createProcessDefinitionQuery().processDefinitionKey("timer").orderByProcessDefinitionVersion().desc().list()) {
            repositoryService.deleteDeployment(processDefinition.getDeploymentId(), true);
        }

        assertThat(managementService.createTimerJobQuery().count()).isZero();
        assertThat(managementService.createJobQuery().count()).isZero();

    }

    @Test
    public void testTimersRecreatedOnDeploymentDelete() {

        // v1 has timer
        // v2 has no timer
        // v3 has no timer
        // v4 has no timer

        // Deploy v1
        repositoryService.createDeployment()
                .addClasspathResource("org/flowable/engine/test/bpmn/event/timer/StartTimerEventTest.testTimersRecreatedOnDeploymentDelete_v1.bpmn20.xml")
                .deploy().getId();

        assertThat(repositoryService.createDeploymentQuery().count()).isEqualTo(1);
        assertThat(repositoryService.createProcessDefinitionQuery().count()).isEqualTo(1);
        assertThat(managementService.createTimerJobQuery().count()).isEqualTo(1);

        // Deploy v2: no timer -> previous should be deleted
        String deployment2 = repositoryService.createDeployment()
                .addClasspathResource("org/flowable/engine/test/bpmn/event/timer/StartTimerEventTest.testTimersRecreatedOnDeploymentDelete_v2.bpmn20.xml")
                .deploy().getId();

        assertThat(repositoryService.createDeploymentQuery().count()).isEqualTo(2);
        assertThat(repositoryService.createProcessDefinitionQuery().count()).isEqualTo(2);
        assertThat(managementService.createTimerJobQuery().count()).isZero();

        // Deploy v3: no timer
        String deployment3 = repositoryService.createDeployment()
                .addClasspathResource("org/flowable/engine/test/bpmn/event/timer/StartTimerEventTest.testTimersRecreatedOnDeploymentDelete_v3.bpmn20.xml")
                .deploy().getId();

        assertThat(repositoryService.createDeploymentQuery().count()).isEqualTo(3);
        assertThat(repositoryService.createProcessDefinitionQuery().count()).isEqualTo(3);
        assertThat(managementService.createTimerJobQuery().count()).isZero();

        // Deploy v4: no timer
        String deployment4 = repositoryService.createDeployment()
                .addClasspathResource("org/flowable/engine/test/bpmn/event/timer/StartTimerEventTest.testTimersRecreatedOnDeploymentDelete_v4.bpmn20.xml")
                .deploy().getId();

        assertThat(repositoryService.createDeploymentQuery().count()).isEqualTo(4);
        assertThat(repositoryService.createProcessDefinitionQuery().count()).isEqualTo(4);
        assertThat(managementService.createTimerJobQuery().count()).isEqualTo(1);

        // Delete v4 -> V3 active. No timer active anymore (v3 doesn't have a timer)
        repositoryService.deleteDeployment(deployment4, true);
        assertThat(repositoryService.createDeploymentQuery().count()).isEqualTo(3);
        assertThat(repositoryService.createProcessDefinitionQuery().count()).isEqualTo(3);
        assertThat(managementService.createTimerJobQuery().count()).isZero();

        // Delete v2 --> V3 still active, nothing changed there
        repositoryService.deleteDeployment(deployment2, true);
        assertThat(repositoryService.createDeploymentQuery().count()).isEqualTo(2);
        assertThat(repositoryService.createProcessDefinitionQuery().count()).isEqualTo(2);
        assertThat(managementService.createTimerJobQuery().count()).isZero(); // v3 is still active

        // Delete v3 -> fallback to v1
        repositoryService.deleteDeployment(deployment3, true);
        assertThat(repositoryService.createDeploymentQuery().count()).isEqualTo(1);
        assertThat(repositoryService.createProcessDefinitionQuery().count()).isEqualTo(1);
        assertThat(managementService.createTimerJobQuery().count()).isEqualTo(1);

        // Cleanup
        for (ProcessDefinition processDefinition : repositoryService.createProcessDefinitionQuery().processDefinitionKey("timer").orderByProcessDefinitionVersion().desc().list()) {
            repositoryService.deleteDeployment(processDefinition.getDeploymentId(), true);
        }

        assertThat(managementService.createTimerJobQuery().count()).isZero();

    }

    // Same test as above, but now with tenants
    @Test
    public void testTimersRecreatedOnDeploymentDeleteWithTenantId() {

        // Deploy 4 versions without tenantId
        for (int i = 1; i <= 4; i++) {
            repositoryService.createDeployment()
                    .addClasspathResource("org/flowable/engine/test/bpmn/event/timer/StartTimerEventTest.testTimersRecreatedOnDeploymentDelete_v" + i + ".bpmn20.xml")
                    .deploy();
        }

        String testTenant = "Activiti-tenant";

        // Deploy v1
        repositoryService.createDeployment()
                .addClasspathResource("org/flowable/engine/test/bpmn/event/timer/StartTimerEventTest.testTimersRecreatedOnDeploymentDelete_v1.bpmn20.xml")
                .tenantId(testTenant)
                .deploy().getId();

        assertThat(repositoryService.createDeploymentQuery().deploymentTenantId(testTenant).count()).isEqualTo(1);
        assertThat(repositoryService.createProcessDefinitionQuery().processDefinitionTenantId(testTenant).count()).isEqualTo(1);
        assertThat(managementService.createTimerJobQuery().jobTenantId(testTenant).count()).isEqualTo(1);

        // Deploy v2: no timer -> previous should be deleted
        String deployment2 = repositoryService.createDeployment()
                .addClasspathResource("org/flowable/engine/test/bpmn/event/timer/StartTimerEventTest.testTimersRecreatedOnDeploymentDelete_v2.bpmn20.xml")
                .tenantId(testTenant)
                .deploy().getId();

        assertThat(repositoryService.createDeploymentQuery().deploymentTenantId(testTenant).count()).isEqualTo(2);
        assertThat(repositoryService.createProcessDefinitionQuery().processDefinitionTenantId(testTenant).count()).isEqualTo(2);
        assertThat(managementService.createTimerJobQuery().jobTenantId(testTenant).count()).isZero();

        // Deploy v3: no timer
        String deployment3 = repositoryService.createDeployment()
                .addClasspathResource("org/flowable/engine/test/bpmn/event/timer/StartTimerEventTest.testTimersRecreatedOnDeploymentDelete_v3.bpmn20.xml")
                .tenantId(testTenant)
                .deploy().getId();

        assertThat(repositoryService.createDeploymentQuery().deploymentTenantId(testTenant).count()).isEqualTo(3);
        assertThat(repositoryService.createProcessDefinitionQuery().processDefinitionTenantId(testTenant).count()).isEqualTo(3);
        assertThat(managementService.createTimerJobQuery().jobTenantId(testTenant).count()).isZero();

        // Deploy v4: no timer
        String deployment4 = repositoryService.createDeployment()
                .addClasspathResource("org/flowable/engine/test/bpmn/event/timer/StartTimerEventTest.testTimersRecreatedOnDeploymentDelete_v4.bpmn20.xml")
                .tenantId(testTenant)
                .deploy().getId();

        assertThat(repositoryService.createDeploymentQuery().deploymentTenantId(testTenant).count()).isEqualTo(4);
        assertThat(repositoryService.createProcessDefinitionQuery().processDefinitionTenantId(testTenant).count()).isEqualTo(4);
        assertThat(managementService.createTimerJobQuery().jobTenantId(testTenant).count()).isEqualTo(1);

        // Delete v4 -> V3 active. No timer active anymore (v3 doesn't have a timer)
        repositoryService.deleteDeployment(deployment4, true);
        assertThat(repositoryService.createDeploymentQuery().deploymentTenantId(testTenant).count()).isEqualTo(3);
        assertThat(repositoryService.createProcessDefinitionQuery().processDefinitionTenantId(testTenant).count()).isEqualTo(3);
        assertThat(managementService.createTimerJobQuery().jobTenantId(testTenant).count()).isZero();

        // Delete v2 --> V3 still active, nothing changed there
        repositoryService.deleteDeployment(deployment2, true);
        assertThat(repositoryService.createDeploymentQuery().deploymentTenantId(testTenant).count()).isEqualTo(2);
        assertThat(repositoryService.createProcessDefinitionQuery().processDefinitionTenantId(testTenant).count()).isEqualTo(2);
        assertThat(managementService.createTimerJobQuery().jobTenantId(testTenant).count()).isZero();

        // Delete v3 -> fallback to v1
        repositoryService.deleteDeployment(deployment3, true);
        assertThat(repositoryService.createDeploymentQuery().deploymentTenantId(testTenant).count()).isEqualTo(1);
        assertThat(repositoryService.createProcessDefinitionQuery().processDefinitionTenantId(testTenant).count()).isEqualTo(1);
        assertThat(managementService.createTimerJobQuery().jobTenantId(testTenant).count()).isEqualTo(1);

        // Cleanup
        for (ProcessDefinition processDefinition : repositoryService.createProcessDefinitionQuery().processDefinitionKey("timer").orderByProcessDefinitionVersion().desc().list()) {
            repositoryService.deleteDeployment(processDefinition.getDeploymentId(), true);
        }

        assertThat(managementService.createTimerJobQuery().count()).isZero();

    }

    // Can't use @Deployment, we need to control the clock very strict to have a good test
    @Test
    public void testMultipleStartEvents() {
        try {
            // Human time (GMT): Tue, 10 May 2016 18:50:01 GMT
            Date startTime = new Date(1462906201000L);
            processEngineConfiguration.getClock().setCurrentTime(startTime);
    
            String deploymentId = repositoryService.createDeployment()
                    .addClasspathResource("org/flowable/engine/test/bpmn/event/timer/StartTimerEventTest.testMultipleStartEvents.bpmn20.xml")
                    .deploy().getId();
    
            // After deployment, should have 4 jobs for the 4 timer events
            assertThat(managementService.createTimerJobQuery().count()).isEqualTo(4);
            assertThat(managementService.createTimerJobQuery().executable().count()).isZero();
    
            // Path A : triggered at start + 10 seconds (18:50:11) (R2)
            // Path B: triggered at start + 5 seconds (18:50:06) (R3)
            // Path C: triggered at start + 15 seconds (18:50:16) (R1)
            // path D: triggered at 18:50:20 (Cron)
    
            // Moving 7 seconds (18:50:08) should trigger one timer (the second start timer in the process diagram)
            Date newDate = new Date(startTime.getTime() + (7 * 1000));
            processEngineConfiguration.getClock().setCurrentTime(newDate);
            List<Job> executableTimers = managementService.createTimerJobQuery().executable().list();
            assertThat(executableTimers).hasSize(1);
    
            executeJobs(executableTimers);
            validateTaskCounts(0, 1, 0, 0);
            assertThat(managementService.createTimerJobQuery().count()).isEqualTo(4);
            assertThat(managementService.createTimerJobQuery().executable().count()).isZero();
    
            // New situation:
            // Path A : triggered at start + 10 seconds (18:50:11) (R2)
            // Path B: triggered at start + 2*5 seconds (18:50:11) (R2 - was R3) [CHANGED]
            // Path C: triggered at start + 15 seconds (18:50:16) (R1)
            // path D: triggered at 18:50:20 (Cron)
    
            // Moving 4 seconds (18:50:12) should trigger both path A and B
            newDate = new Date(newDate.getTime() + (4 * 1000));
            processEngineConfiguration.getClock().setCurrentTime(newDate);
    
            executableTimers = managementService.createTimerJobQuery().executable().list();
            assertThat(executableTimers).hasSize(2);
            executeJobs(executableTimers);
            validateTaskCounts(1, 2, 0, 0);
            assertThat(managementService.createTimerJobQuery().count()).isEqualTo(4);
            assertThat(managementService.createTimerJobQuery().executable().count()).isZero();
    
            // New situation:
            // Path A : triggered at start + 2*10 seconds (18:50:21) (R1 - was R2) [CHANGED]
            // Path B: triggered at start + 3*5 seconds (18:50:16) (R1 - was R2) [CHANGED]
            // Path C: triggered at start + 15 seconds (18:50:16) (R1)
            // path D: triggered at 18:50:20 (Cron)
    
            // Moving 6 seconds (18:50:18) should trigger B and C
            newDate = new Date(newDate.getTime() + (6 * 1000));
            processEngineConfiguration.getClock().setCurrentTime(newDate);
    
            executableTimers = managementService.createTimerJobQuery().executable().list();
            assertThat(executableTimers).hasSize(2);
            executeJobs(executableTimers);
            validateTaskCounts(1, 3, 1, 0);
            assertThat(managementService.createTimerJobQuery().count()).isEqualTo(2);
            assertThat(managementService.createTimerJobQuery().executable().count()).isZero();
    
            // New situation:
            // Path A : triggered at start + 2*10 seconds (18:50:21) (R1 - was R2) [CHANGED]
            // Path B: all repeats used up
            // Path C: all repeats used up
            // path D: triggered at 18:50:20 (Cron)
    
            // Moving 10 seconds (18:50:28) should trigger A and D
            newDate = new Date(newDate.getTime() + (6 * 1000));
            processEngineConfiguration.getClock().setCurrentTime(newDate);
    
            executableTimers = managementService.createTimerJobQuery().executable().list();
            assertThat(executableTimers).hasSize(2);
            executeJobs(executableTimers);
            validateTaskCounts(2, 3, 1, 1);
            assertThat(managementService.createTimerJobQuery().count()).isEqualTo(1);
            assertThat(managementService.createTimerJobQuery().executable().count()).isZero();
    
            // New situation:
            // Path A : all repeats used up
            // Path B: all repeats used up
            // Path C: all repeats used up
            // path D: triggered at 18:50:40 (Cron)
    
            // Clean up
            repositoryService.deleteDeployment(deploymentId, true);
            
        } finally {
            processEngineConfiguration.resetClock();
        }
    }

    protected void validateTaskCounts(long taskACount, long taskBCount, long taskCCount, long taskDCount) {
        assertThat(taskService.createTaskQuery().taskName("Task A").count()).as("task A counts are incorrect").isEqualTo(taskACount);
        assertThat(taskService.createTaskQuery().taskName("Task B").count()).as("task B counts are incorrect").isEqualTo(taskBCount);
        assertThat(taskService.createTaskQuery().taskName("Task C").count()).as("task C counts are incorrect").isEqualTo(taskCCount);
        assertThat(taskService.createTaskQuery().taskName("Task D").count()).as("task D counts are incorrect").isEqualTo(taskDCount);
    }

    protected void executeJobs(List<Job> jobs) {
        for (Job job : jobs) {
            managementService.moveTimerToExecutableJob(job.getId());
            managementService.executeJob(job.getId());
        }
    }
    
    protected int getTimerJobsCount() {
        int timerJobsCount = processEngineConfiguration.getCommandExecutor().execute(new Command<Integer>() {

            @Override
            public Integer execute(CommandContext commandContext) {
                List<String> enabledCategories = processEngineConfiguration.getJobServiceConfiguration().getEnabledJobCategories();
                AsyncExecutor asyncExecutor = processEngineConfiguration.getAsyncExecutor();
                List<TimerJobEntity> timerJobs = processEngineConfiguration.getJobServiceConfiguration().getTimerJobEntityManager()
                        .findJobsToExecute(enabledCategories, new Page(0, asyncExecutor.getMaxAsyncJobsDuePerAcquisition()));
                return timerJobs.size();
            }
        });
        
        return timerJobsCount;
    }

    protected void cleanDB() {
        String jobId = managementService.createTimerJobQuery().singleResult().getId();
        CommandExecutor commandExecutor = processEngineConfiguration.getCommandExecutor();
        commandExecutor.execute(new CancelJobsCmd(jobId, processEngineConfiguration.getJobServiceConfiguration()));
    }

}
