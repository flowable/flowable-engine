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
package org.flowable.engine.test.api.mgmt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.interceptor.CommandExecutor;
import org.flowable.engine.impl.jobexecutor.TriggerTimerEventJobHandler;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.runtime.Execution;
import org.flowable.job.api.Job;
import org.flowable.job.api.JobInfo;
import org.flowable.job.service.JobService;
import org.flowable.job.service.impl.persistence.entity.JobEntity;
import org.flowable.job.service.impl.persistence.entity.TimerJobEntity;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Joram Barrez
 */
public class TimerJobQueryTest extends PluggableFlowableTestCase {

    private Date testStartTime;
    private String processInstanceId;

    @BeforeEach
    protected void setUp() throws Exception {
        this.testStartTime = new Date();
        repositoryService.createDeployment().addClasspathResource("org/flowable/engine/test/api/mgmt/TimerJobQueryTest.bpmn20.xml").deploy();
        this.processInstanceId = runtimeService.startProcessInstanceByKey("timerJobQueryTest").getId();
    }

    @AfterEach
    protected void tearDown() throws Exception {
        for (Deployment deployment : repositoryService.createDeploymentQuery().list()) {
            repositoryService.deleteDeployment(deployment.getId(), true);
        }
    }

    @Test
    public void testByJobId() {
        List<Job> timerJobs = managementService.createTimerJobQuery().list();
        assertThat(timerJobs).hasSize(3);
        assertThat(managementService.createJobQuery().count()).isEqualTo(1);

        for (Job job : timerJobs) {
            assertThat(job).isInstanceOf(TimerJobEntity.class);
            assertThat(managementService.createTimerJobQuery().jobId(job.getId())).isNotNull();
        }
    }

    @Test
    public void testByProcessInstanceId() {
        assertThat(managementService.createTimerJobQuery().processInstanceId(processInstanceId).list()).hasSize(3);
    }
    
    @Test
    public void testWithoutProcessInstanceId() {
        assertThat(managementService.createTimerJobQuery().withoutProcessInstanceId().list()).hasSize(0);
    }

    @Test
    public void testByExecutionId() {
        for (String id : Arrays.asList("timerA", "timerB", "timerC")) {
            Execution execution = runtimeService.createExecutionQuery().activityId(id).singleResult();
            assertThat(managementService.createTimerJobQuery().executionId(execution.getId()).singleResult()).isNotNull();
            assertThat(managementService.createTimerJobQuery().executionId(execution.getId()).count()).isPositive();
        }
    }

    @Test
    public void testByProcessDefinitionId() {
        String processDefinitionId = repositoryService.createProcessDefinitionQuery().singleResult().getId();
        assertThat(managementService.createTimerJobQuery().processDefinitionId(processDefinitionId).count()).isEqualTo(3);
        assertThat(managementService.createTimerJobQuery().processDefinitionId(processDefinitionId).list()).hasSize(3);
    }

    @Test
    public void testByProcessDefinitionKey() {
        String processDefinitionKey = repositoryService.createProcessDefinitionQuery().singleResult().getKey();
        assertThat(managementService.createTimerJobQuery().processDefinitionKey(processDefinitionKey).count()).isEqualTo(3);
        assertThat(managementService.createTimerJobQuery().processDefinitionKey(processDefinitionKey).list()).hasSize(3);

        assertThat(managementService.createTimerJobQuery().processDefinitionKey("invalid").singleResult()).isNull();
        assertThat(managementService.createTimerJobQuery().processDefinitionKey("invalid").count()).isZero();
        assertThat(managementService.createTimerJobQuery().processDefinitionKey("invalid").list()).isEmpty();
    }
    
    @Test
    public void testWithoutScopeId() {
        assertThat(managementService.createTimerJobQuery().withoutScopeId().list()).hasSize(3);
    }

    @Test
    public void testByExecutable() {
        assertThat(managementService.createTimerJobQuery().executable().count()).isZero();
        assertThat(managementService.createTimerJobQuery().executable().list()).isEmpty();
        processEngineConfiguration.getClock().setCurrentTime(new Date(testStartTime.getTime() + (5 * 60 * 1000 * 1000)));
        assertThat(managementService.createTimerJobQuery().executable().count()).isEqualTo(3);
        assertThat(managementService.createTimerJobQuery().executable().list()).hasSize(3);
    }

    @Test
    public void testByHandlerType() {
        assertThat(managementService.createTimerJobQuery().handlerType(TriggerTimerEventJobHandler.TYPE).count()).isEqualTo(3);
        assertThat(managementService.createTimerJobQuery().handlerType(TriggerTimerEventJobHandler.TYPE).list()).hasSize(3);
        assertThat(managementService.createTimerJobQuery().handlerType("invalid").count()).isZero();
    }

    @Test
    public void testTimerJobQueryByHandlerTypes() {

        List<String> testTypes = new ArrayList<>();
        createTimerJobWithHandlerType("Type1");
        createTimerJobWithHandlerType("Type2");

        assertThat(managementService.createTimerJobQuery().handlerType("Type1").singleResult()).isNotNull();
        assertThat(managementService.createTimerJobQuery().handlerType("Type2").singleResult()).isNotNull();

        testTypes.add("TestType");
        assertThat(managementService.createTimerJobQuery().handlerTypes(testTypes).singleResult()).isNull();

        testTypes.add("Type1");
        assertThat(managementService.createTimerJobQuery().handlerTypes(testTypes).count()).isEqualTo(1);
        assertThat(managementService.createTimerJobQuery().handlerTypes(testTypes).singleResult().getJobHandlerType()).isEqualTo("Type1");

        testTypes.add("Type2");
        assertThat(managementService.createTimerJobQuery().handlerTypes(testTypes).count()).isEqualTo(2);
        assertThat(managementService.createTimerJobQuery().handlerTypes(testTypes).list())
                .extracting(JobInfo::getJobHandlerType)
                .containsExactlyInAnyOrder("Type1", "Type2");
        managementService.deleteTimerJob(managementService.createTimerJobQuery().handlerType("Type1").singleResult().getId());
        managementService.deleteTimerJob(managementService.createTimerJobQuery().handlerType("Type2").singleResult().getId());
    }

    @Test
    public void testByJobType() {
        assertThat(managementService.createTimerJobQuery().timers().count()).isEqualTo(3);
        assertThat(managementService.createTimerJobQuery().timers().list()).hasSize(3);
        assertThat(managementService.createTimerJobQuery().messages().count()).isZero();
        assertThat(managementService.createTimerJobQuery().messages().list()).isEmpty();

        // Executing the async job throws an exception -> job retry + creation of timer
        Job asyncJob = managementService.createJobQuery().singleResult();
        assertThat(asyncJob).isNotNull();
        assertThatThrownBy(() -> managementService.executeJob(asyncJob.getId()))
                .isInstanceOf(Exception.class);

        assertThat(managementService.createJobQuery().count()).isZero();
        assertThat(managementService.createTimerJobQuery().timers().count()).isEqualTo(3);
        assertThat(managementService.createTimerJobQuery().timers().list()).hasSize(3);
        assertThat(managementService.createTimerJobQuery().messages().count()).isEqualTo(1);
        assertThat(managementService.createTimerJobQuery().messages().list()).hasSize(1);

        assertThat(managementService.createTimerJobQuery().withException().count()).isEqualTo(1);
        assertThat(managementService.createTimerJobQuery().withException().list()).hasSize(1);
    }

    @Test
    public void testByDuedateLowerThan() {
        Date date = new Date(testStartTime.getTime() + (10 * 60 * 1000 * 1000));
        assertThat(managementService.createTimerJobQuery().timers().duedateLowerThan(date).count()).isEqualTo(3);
        assertThat(managementService.createTimerJobQuery().timers().duedateLowerThan(date).list()).hasSize(3);
    }

    @Test
    public void testByDuedateHigherThan() {
        assertThat(managementService.createTimerJobQuery().timers().duedateLowerThan(testStartTime).count()).isZero();
        assertThat(managementService.createTimerJobQuery().timers().duedateLowerThan(testStartTime).list()).isEmpty();
    }

    @Test
    public void testByCorrelationId() {
        Job timerA = managementService.createTimerJobQuery().elementId("timerA").singleResult();

        Job job = managementService.createTimerJobQuery().correlationId(timerA.getCorrelationId()).singleResult();
        assertThat(job).isNotNull();
        assertThat(job.getId()).isEqualTo(timerA.getId());
        assertThat(job.getCorrelationId()).isEqualTo(timerA.getCorrelationId());
        assertThat(managementService.createTimerJobQuery().correlationId(timerA.getCorrelationId()).list()).hasSize(1);
        assertThat(managementService.createTimerJobQuery().correlationId(timerA.getCorrelationId()).count()).isEqualTo(1);
    }

    @Test
    public void testByInvalidCorrelationId() {
        assertThat(managementService.createTimerJobQuery().correlationId("invalid").singleResult()).isNull();
        assertThat(managementService.createTimerJobQuery().correlationId("invalid").list()).isEmpty();
        assertThat(managementService.createTimerJobQuery().correlationId("invalid").count()).isZero();
    }

    @Test
    public void testOrQuery() {
        // Get specific timer jobs by element id, scoped to this test's process instance
        Job timerA = managementService.createTimerJobQuery().processInstanceId(processInstanceId).elementId("timerA").singleResult();
        Job timerB = managementService.createTimerJobQuery().processInstanceId(processInstanceId).elementId("timerB").singleResult();
        Job timerC = managementService.createTimerJobQuery().processInstanceId(processInstanceId).elementId("timerC").singleResult();
        assertThat(timerA).isNotNull();
        assertThat(timerB).isNotNull();
        assertThat(timerC).isNotNull();

        // OR query: match by jobId OR elementId
        List<Job> jobs = managementService.createTimerJobQuery()
                .or()
                    .jobId(timerA.getId())
                    .elementId("timerB")
                .endOr()
                .list();
        assertThat(jobs).hasSize(2);
        assertThat(jobs).extracting(JobInfo::getId)
                .containsExactlyInAnyOrder(timerA.getId(), timerB.getId());

        // OR query: match by executionId OR elementId
        jobs = managementService.createTimerJobQuery()
                .or()
                    .executionId(timerA.getExecutionId())
                    .elementId("timerC")
                .endOr()
                .list();
        assertThat(jobs).hasSize(2);
        assertThat(jobs).extracting(JobInfo::getId)
                .containsExactlyInAnyOrder(timerA.getId(), timerC.getId());

        // OR with no match
        assertThat(managementService.createTimerJobQuery()
                .or()
                    .processInstanceId("nonexistent")
                    .executionId("nonexistent")
                .endOr()
                .count()).isZero();
    }

    @Test
    public void testOrWithAndQuery() {
        Job timerA = managementService.createTimerJobQuery().elementId("timerA").singleResult();
        Job timerB = managementService.createTimerJobQuery().elementId("timerB").singleResult();

        // AND (processInstanceId) + OR (jobId or elementId)
        List<Job> jobs = managementService.createTimerJobQuery()
                .processInstanceId(processInstanceId)
                .or()
                    .jobId(timerA.getId())
                    .elementId("timerB")
                .endOr()
                .list();
        assertThat(jobs).hasSize(2);

        // AND (wrong processInstanceId) + OR should return nothing
        assertThat(managementService.createTimerJobQuery()
                .processInstanceId("nonexistent")
                .or()
                    .jobId(timerA.getId())
                    .elementId("timerB")
                .endOr()
                .count()).isZero();
    }

    @Test
    public void testOrQueryErrors() {
        assertThatThrownBy(() -> managementService.createTimerJobQuery().or().or())
                .isInstanceOf(FlowableException.class)
                .hasMessageContaining("the query is already in an or statement");

        assertThatThrownBy(() -> managementService.createTimerJobQuery().endOr())
                .isInstanceOf(FlowableException.class)
                .hasMessageContaining("endOr() can only be called after calling or()");
    }

    private void createTimerJobWithHandlerType(String handlerType) {
        CommandExecutor commandExecutor = processEngineConfiguration.getCommandExecutor();
        commandExecutor.execute(new Command<Void>() {

            @Override
            public Void execute(CommandContext commandContext) {
                JobService jobService = CommandContextUtil.getJobService(commandContext);

                JobEntity jobEntity = jobService.createJob();
                jobEntity.setJobType(Job.JOB_TYPE_TIMER);
                jobEntity.setRetries(0);
                jobEntity.setJobHandlerType(handlerType);
                jobService.insertJob(jobEntity);
                CommandContextUtil.getTimerJobService(commandContext).moveJobToTimerJob(jobEntity);
                assertThat(jobEntity.getId()).isNotNull();
                return null;
            }
        });
    }
}
