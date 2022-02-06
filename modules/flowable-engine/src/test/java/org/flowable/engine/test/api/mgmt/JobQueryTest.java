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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.interceptor.CommandExecutor;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.job.api.DeadLetterJobQuery;
import org.flowable.job.api.ExternalWorkerJob;
import org.flowable.job.api.ExternalWorkerJobQuery;
import org.flowable.job.api.HistoryJob;
import org.flowable.job.api.HistoryJobQuery;
import org.flowable.job.api.Job;
import org.flowable.job.api.JobInfo;
import org.flowable.job.api.JobQuery;
import org.flowable.job.api.SuspendedJobQuery;
import org.flowable.job.api.TimerJobQuery;
import org.flowable.job.service.HistoryJobService;
import org.flowable.job.service.JobService;
import org.flowable.job.service.JobServiceConfiguration;
import org.flowable.job.service.impl.DeadLetterJobQueryImpl;
import org.flowable.job.service.impl.ExternalWorkerJobQueryImpl;
import org.flowable.job.service.impl.HistoryJobQueryImpl;
import org.flowable.job.service.impl.SuspendedJobQueryImpl;
import org.flowable.job.service.impl.cmd.CancelJobsCmd;
import org.flowable.job.service.impl.persistence.entity.DeadLetterJobEntity;
import org.flowable.job.service.impl.persistence.entity.DeadLetterJobEntityManager;
import org.flowable.job.service.impl.persistence.entity.ExternalWorkerJobEntity;
import org.flowable.job.service.impl.persistence.entity.ExternalWorkerJobEntityManager;
import org.flowable.job.service.impl.persistence.entity.HistoryJobEntity;
import org.flowable.job.service.impl.persistence.entity.JobEntity;
import org.flowable.job.service.impl.persistence.entity.SuspendedJobEntity;
import org.flowable.job.service.impl.persistence.entity.SuspendedJobEntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Joram Barrez
 * @author Falko Menge
 */
public class JobQueryTest extends PluggableFlowableTestCase {

    private static final long ONE_HOUR = 60L * 60L * 1000L;
    private static final long ONE_SECOND = 1000L;
    private static final String EXCEPTION_MESSAGE = "problem evaluating script: javax.script.ScriptException: java.lang.RuntimeException: This is an exception thrown from scriptTask";
    private String deploymentId;
    private String messageId;
    private CommandExecutor commandExecutor;
    private JobEntity jobEntity;
    private Date testStartTime;
    private Date timerOneFireTime;
    private Date timerTwoFireTime;
    private Date timerThreeFireTime;
    private String processInstanceIdOne;
    private String processInstanceIdTwo;
    private String processInstanceIdThree;

    /**
     * Setup will create - 3 process instances, each with one timer, each firing at t1/t2/t3 + 1 hour (see process) - 1 message
     */
    @BeforeEach
    protected void setUp() throws Exception {

        this.commandExecutor = processEngineConfiguration.getCommandExecutor();

        deploymentId = repositoryService.createDeployment().addClasspathResource("org/flowable/engine/test/api/mgmt/timerOnTask.bpmn20.xml").deploy().getId();

        // Create proc inst that has timer that will fire on t1 + 1 hour
        Calendar startTime = Calendar.getInstance();
        startTime.set(Calendar.MILLISECOND, 0);

        Date t1 = startTime.getTime();
        processEngineConfiguration.getClock().setCurrentTime(t1);

        processInstanceIdOne = runtimeService.startProcessInstanceByKey("timerOnTask").getId();
        testStartTime = t1;
        timerOneFireTime = new Date(t1.getTime() + ONE_HOUR);

        // Create process instance that has timer that will fire on t2 + 1 hour
        startTime.add(Calendar.HOUR_OF_DAY, 1);
        Date t2 = startTime.getTime(); // t2 = t1 + 1 hour
        processEngineConfiguration.getClock().setCurrentTime(t2);
        processInstanceIdTwo = runtimeService.startProcessInstanceByKey("timerOnTask").getId();
        timerTwoFireTime = new Date(t2.getTime() + ONE_HOUR);

        // Create process instance that has timer that will fire on t3 + 1 hour
        startTime.add(Calendar.HOUR_OF_DAY, 1);
        Date t3 = startTime.getTime(); // t3 = t2 + 1 hour
        processEngineConfiguration.getClock().setCurrentTime(t3);
        processInstanceIdThree = runtimeService.startProcessInstanceByKey("timerOnTask").getId();
        timerThreeFireTime = new Date(t3.getTime() + ONE_HOUR);

        // Create one message
        messageId = commandExecutor.execute(new Command<String>() {

            @Override
            public String execute(CommandContext commandContext) {
                JobService jobService = CommandContextUtil.getJobService(commandContext);
                JobEntity message = jobService.createJob();
                message.setJobType(Job.JOB_TYPE_MESSAGE);
                message.setRetries(3);
                jobService.scheduleAsyncJob(message);
                return message.getId();
            }
        });
    }

    @AfterEach
    protected void tearDown() throws Exception {
        repositoryService.deleteDeployment(deploymentId, true);
        commandExecutor.execute(new CancelJobsCmd(messageId, processEngineConfiguration.getJobServiceConfiguration()));
    }

    @Test
    public void testQueryByNoCriteria() {
        JobQuery query = managementService.createJobQuery();
        verifyQueryResults(query, 1);

        TimerJobQuery timerQuery = managementService.createTimerJobQuery();
        verifyQueryResults(timerQuery, 3);
    }

    @Test
    public void testQueryByNoCriteriaWithPaging() {
        List<Job> jobs = managementService.createJobQuery().listPage(1, 2);
        assertThat(jobs).isEmpty();

        List<Job> timerJobs = managementService.createTimerJobQuery().listPage(1, 2);
        assertThat(timerJobs).hasSize(2);
    }

    @Test
    public void testQueryByProcessInstanceId() {
        TimerJobQuery query = managementService.createTimerJobQuery().processInstanceId(processInstanceIdOne);
        verifyQueryResults(query, 1);
    }

    @Test
    public void testQueryByInvalidProcessInstanceId() {
        TimerJobQuery query = managementService.createTimerJobQuery().processInstanceId("invalid");
        verifyQueryResults(query, 0);

        assertThatThrownBy(() -> managementService.createJobQuery().processInstanceId(null))
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class);
    }

    @Test
    public void testTimerQueryWithoutProcessInstanceId() {
        TimerJobQuery query = managementService.createTimerJobQuery().withoutProcessInstanceId();
        verifyQueryResults(query, 0);
    }

    @Test
    public void testJobQueryWithoutProcessInstanceId() {
        JobQuery query = managementService.createJobQuery().withoutProcessInstanceId();
        verifyQueryResults(query, 1);

        assertThat(query.singleResult().getId()).isEqualTo(messageId);
    }

    @Test
    public void testQueryByExecutionId() {
        Job job = managementService.createTimerJobQuery().processInstanceId(processInstanceIdOne).singleResult();
        TimerJobQuery query = managementService.createTimerJobQuery().executionId(job.getExecutionId());
        assertThat(job.getId()).isEqualTo(query.singleResult().getId());
        verifyQueryResults(query, 1);
    }

    @Test
    public void testQueryByInvalidExecutionId() {
        JobQuery query = managementService.createJobQuery().executionId("invalid");
        verifyQueryResults(query, 0);

        TimerJobQuery timerQuery = managementService.createTimerJobQuery().executionId("invalid");
        verifyQueryResults(timerQuery, 0);

        assertThatThrownBy(() -> managementService.createJobQuery().executionId(null).list())
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class);

        assertThatThrownBy(() -> managementService.createTimerJobQuery().executionId(null).list())
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class);
    }

    @Test
    public void testQueryByElementId() {
        TimerJobQuery query = managementService.createTimerJobQuery().elementId("escalationTimer");
        verifyQueryResults(query, 3);
    }

    @Test
    public void testQueryByInvalidElementId() {
        TimerJobQuery query = managementService.createTimerJobQuery().elementId("unknown");
        verifyQueryResults(query, 0);
    }

    @Test
    public void testQueryByElementName() {
        TimerJobQuery query = managementService.createTimerJobQuery().elementName("Escalation");
        verifyQueryResults(query, 3);
    }

    @Test
    public void testQueryByInvalidElementName() {
        TimerJobQuery query = managementService.createTimerJobQuery().elementName("unknown");
        verifyQueryResults(query, 0);
    }

    @Test
    public void testQueryByHandlerType() {
        final JobEntity job = (JobEntity) managementService.createJobQuery().singleResult();
        job.setJobHandlerType("test");
        managementService.executeCommand(new Command<Void>() {

            @Override
            public Void execute(CommandContext commandContext) {
                JobService jobService = CommandContextUtil.getJobService(commandContext);
                jobService.updateJob(job);
                return null;
            }

        });

        Job handlerTypeJob = managementService.createJobQuery().handlerType("test").singleResult();
        assertThat(handlerTypeJob).isNotNull();
    }

    @Test
    public void testQueryByHandlerTypes() {

        List<String> testTypes = new ArrayList<>();
        createJobWithHandlerType("Type1");
        createJobWithHandlerType("Type2");

        assertThat(managementService.createJobQuery().handlerType("Type1").singleResult()).isNotNull();
        assertThat(managementService.createJobQuery().handlerType("Type2").singleResult()).isNotNull();

        testTypes.add("TestType");
        assertThat(managementService.createJobQuery().handlerTypes(testTypes).singleResult()).isNull();

        testTypes.add("Type1");
        assertThat(managementService.createJobQuery().handlerTypes(testTypes).count()).isEqualTo(1);
        assertThat(managementService.createJobQuery().handlerTypes(testTypes).singleResult().getJobHandlerType()).isEqualTo("Type1");

        testTypes.add("Type2");
        assertThat(managementService.createJobQuery().handlerTypes(testTypes).count()).isEqualTo(2);

        assertThat(managementService.createJobQuery().handlerTypes(testTypes).list())
                .extracting(JobInfo::getJobHandlerType)
                .containsExactlyInAnyOrder("Type1", "Type2");

        managementService.deleteJob(managementService.createJobQuery().handlerType("Type1").singleResult().getId());
        managementService.deleteJob(managementService.createJobQuery().handlerType("Type2").singleResult().getId());
    }

    @Test
    public void testSuspendedJobQueryByHandlerTypes() {

        List<String> testTypes = new ArrayList<>();
        createSuspendedJobWithHandlerType("Type1");
        createSuspendedJobWithHandlerType("Type2");

        assertThat(managementService.createSuspendedJobQuery().handlerType("Type1").singleResult()).isNotNull();
        assertThat(managementService.createSuspendedJobQuery().handlerType("Type2").singleResult()).isNotNull();

        testTypes.add("TestType");
        assertThat(managementService.createSuspendedJobQuery().handlerTypes(testTypes).singleResult()).isNull();

        testTypes.add("Type1");
        assertThat(managementService.createSuspendedJobQuery().handlerTypes(testTypes).count()).isEqualTo(1);
        assertThat(managementService.createSuspendedJobQuery().handlerTypes(testTypes).singleResult().getJobHandlerType()).isEqualTo("Type1");

        testTypes.add("Type2");
        assertThat(managementService.createSuspendedJobQuery().handlerTypes(testTypes).count()).isEqualTo(2);
        assertThat(managementService.createSuspendedJobQuery().handlerTypes(testTypes).list())
                .extracting(JobInfo::getJobHandlerType)
                .containsExactlyInAnyOrder("Type1", "Type2");

        managementService.deleteSuspendedJob(managementService.createSuspendedJobQuery().handlerType("Type1").singleResult().getId());
        managementService.deleteSuspendedJob(managementService.createSuspendedJobQuery().handlerType("Type2").singleResult().getId());
    }

    @Test
    public void testDeadletterQueryByHandlerTypes() {

        List<String> testTypes = new ArrayList<>();
        createDeadLetterJobWithHandlerType("Type1");
        createDeadLetterJobWithHandlerType("Type2");

        assertThat(managementService.createDeadLetterJobQuery().handlerType("Type1").singleResult()).isNotNull();
        assertThat(managementService.createDeadLetterJobQuery().handlerType("Type2").singleResult()).isNotNull();

        testTypes.add("TestType");
        assertThat(managementService.createDeadLetterJobQuery().handlerTypes(testTypes).singleResult()).isNull();

        testTypes.add("Type1");
        assertThat(managementService.createDeadLetterJobQuery().handlerTypes(testTypes).count()).isEqualTo(1);
        assertThat(managementService.createDeadLetterJobQuery().handlerTypes(testTypes).singleResult().getJobHandlerType()).isEqualTo("Type1");

        testTypes.add("Type2");
        assertThat(managementService.createDeadLetterJobQuery().handlerTypes(testTypes).count()).isEqualTo(2);

        assertThat(managementService.createDeadLetterJobQuery().handlerTypes(testTypes).list())
                .extracting(JobInfo::getJobHandlerType)
                .containsExactlyInAnyOrder("Type1", "Type2");

        managementService.deleteDeadLetterJob(managementService.createDeadLetterJobQuery().handlerType("Type1").singleResult().getId());
        managementService.deleteDeadLetterJob(managementService.createDeadLetterJobQuery().handlerType("Type2").singleResult().getId());
    }

    @Test
    public void testHistoryJobQueryByHandlerTypes() {

        List<String> testTypes = new ArrayList<>();
        createHistoryobWithHandlerType("Type1");
        createHistoryobWithHandlerType("Type2");

        assertThat(managementService.createHistoryJobQuery().handlerType("Type1").singleResult()).isNotNull();
        assertThat(managementService.createHistoryJobQuery().handlerType("Type2").singleResult()).isNotNull();

        testTypes.add("TestType");
        assertThat(managementService.createHistoryJobQuery().handlerTypes(testTypes).singleResult()).isNull();

        testTypes.add("Type1");
        assertThat(managementService.createHistoryJobQuery().handlerTypes(testTypes).count()).isEqualTo(1);
        assertThat(managementService.createHistoryJobQuery().handlerTypes(testTypes).singleResult().getJobHandlerType()).isEqualTo("Type1");

        testTypes.add("Type2");
        assertThat(managementService.createHistoryJobQuery().handlerTypes(testTypes).count()).isEqualTo(2);
        assertThat(managementService.createHistoryJobQuery().handlerTypes(testTypes).list())
                .extracting(JobInfo::getJobHandlerType)
                .containsExactlyInAnyOrder("Type1", "Type2");

        managementService.deleteHistoryJob(managementService.createHistoryJobQuery().handlerType("Type1").singleResult().getId());
        managementService.deleteHistoryJob(managementService.createHistoryJobQuery().handlerType("Type2").singleResult().getId());
    }

    @Test
    public void testQueryByCorrelationId() {
        Job messageJob = managementService.createJobQuery().jobId(messageId).singleResult();
        assertThat(messageJob).isNotNull();

        Job job = managementService.createJobQuery().correlationId(messageJob.getCorrelationId()).singleResult();
        assertThat(job).isNotNull();
        assertThat(job.getId()).isEqualTo(messageId);
        assertThat(job.getCorrelationId()).isEqualTo(messageJob.getCorrelationId());
        assertThat(managementService.createJobQuery().correlationId(job.getCorrelationId()).list()).hasSize(1);
        assertThat(managementService.createJobQuery().correlationId(job.getCorrelationId()).count()).isEqualTo(1);
    }

    @Test
    public void testByInvalidCorrelationId() {
        assertThat(managementService.createJobQuery().correlationId("invalid").singleResult()).isNull();
        assertThat(managementService.createJobQuery().correlationId("invalid").list()).isEmpty();
        assertThat(managementService.createJobQuery().correlationId("invalid").count()).isZero();
    }

    @Test
    public void testQueryByInvalidJobType() {
        JobQuery query = managementService.createJobQuery().handlerType("invalid");
        verifyQueryResults(query, 0);

        TimerJobQuery timerQuery = managementService.createTimerJobQuery().executionId("invalid");
        verifyQueryResults(timerQuery, 0);

        assertThatThrownBy(() -> managementService.createJobQuery().executionId(null).list())
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class);

        assertThatThrownBy(() -> managementService.createTimerJobQuery().executionId(null).list())
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class);
    }

    @Test
    public void testQueryByRetriesLeft() {
        JobQuery query = managementService.createJobQuery();
        verifyQueryResults(query, 1);

        TimerJobQuery timerQuery = managementService.createTimerJobQuery();
        verifyQueryResults(timerQuery, 3);

        final Job job = managementService.createTimerJobQuery().processInstanceId(processInstanceIdOne).singleResult();
        managementService.setTimerJobRetries(job.getId(), 0);
        managementService.moveJobToDeadLetterJob(job.getId());

        // Re-running the query should give only 3 jobs now, since one job has retries=0
        verifyQueryResults(query, 1);
        verifyQueryResults(timerQuery, 2);
    }

    @Test
    public void testQueryByExecutable() {
        processEngineConfiguration.getClock()
                .setCurrentTime(new Date(timerThreeFireTime.getTime() + ONE_SECOND)); // all obs should be executable at t3 + 1hour.1second
        JobQuery query = managementService.createJobQuery();
        verifyQueryResults(query, 1);

        TimerJobQuery timerQuery = managementService.createTimerJobQuery().executable();
        verifyQueryResults(timerQuery, 3);

        // Setting retries of one job to 0, makes it non-executable
        final Job job = managementService.createTimerJobQuery().processInstanceId(processInstanceIdOne).singleResult();
        managementService.setTimerJobRetries(job.getId(), 0);
        managementService.moveJobToDeadLetterJob(job.getId());

        verifyQueryResults(query, 1);
        verifyQueryResults(timerQuery, 2);

        // Setting the clock before the start of the process instance, makes
        // none of the timer jobs executable
        processEngineConfiguration.getClock().setCurrentTime(testStartTime);
        verifyQueryResults(query, 1);
        verifyQueryResults(timerQuery, 0);

        // Moving the job back to be executable
        managementService.moveDeadLetterJobToExecutableJob(job.getId(), 5);
        verifyQueryResults(query, 2);
        verifyQueryResults(timerQuery, 0);
    }

    @Test
    public void testQueryByOnlyTimers() {
        JobQuery query = managementService.createJobQuery().timers();
        verifyQueryResults(query, 0);

        TimerJobQuery timerQuery = managementService.createTimerJobQuery().timers();
        verifyQueryResults(timerQuery, 3);
    }

    @Test
    public void testQueryByOnlyMessages() {
        JobQuery query = managementService.createJobQuery().messages();
        verifyQueryResults(query, 1);
    }

    @Test
    public void testInvalidOnlyTimersUsage() {
        assertThatThrownBy(() -> managementService.createJobQuery().timers().messages().list())
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class)
                .hasMessage("Cannot combine onlyTimers() with onlyMessages() in the same query");
    }

    @Test
    public void testQueryByDuedateLowerThan() {
        JobQuery query = managementService.createJobQuery().duedateLowerThan(testStartTime);
        verifyQueryResults(query, 0);

        TimerJobQuery timerQuery = managementService.createTimerJobQuery().duedateLowerThan(testStartTime);
        verifyQueryResults(timerQuery, 0);

        timerQuery = managementService.createTimerJobQuery().duedateLowerThan(new Date(timerOneFireTime.getTime() + ONE_SECOND));
        verifyQueryResults(timerQuery, 1);

        timerQuery = managementService.createTimerJobQuery().duedateLowerThan(new Date(timerTwoFireTime.getTime() + ONE_SECOND));
        verifyQueryResults(timerQuery, 2);

        timerQuery = managementService.createTimerJobQuery().duedateLowerThan(new Date(timerThreeFireTime.getTime() + ONE_SECOND));
        verifyQueryResults(timerQuery, 3);
    }

    @Test
    public void testQueryByDuedateHigherThan() {
        JobQuery query = managementService.createJobQuery().duedateHigherThan(testStartTime);
        verifyQueryResults(query, 0);

        query = managementService.createJobQuery();
        verifyQueryResults(query, 1);

        TimerJobQuery timerQuery = managementService.createTimerJobQuery().duedateHigherThan(testStartTime);
        verifyQueryResults(timerQuery, 3);

        query = managementService.createJobQuery().duedateHigherThan(timerOneFireTime);
        verifyQueryResults(query, 0);

        timerQuery = managementService.createTimerJobQuery().duedateHigherThan(timerOneFireTime);
        verifyQueryResults(timerQuery, 2);

        timerQuery = managementService.createTimerJobQuery().duedateHigherThan(timerTwoFireTime);
        verifyQueryResults(timerQuery, 1);

        timerQuery = managementService.createTimerJobQuery().duedateHigherThan(timerThreeFireTime);
        verifyQueryResults(timerQuery, 0);
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/mgmt/ManagementServiceTest.testGetJobExceptionStacktrace.bpmn20.xml" })
    public void testQueryByException() {
        TimerJobQuery query = managementService.createTimerJobQuery().withException();
        verifyQueryResults(query, 0);

        ProcessInstance processInstance = startProcessInstanceWithFailingJob();

        query = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).withException();
        verifyFailedJob(query, processInstance);
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/mgmt/ManagementServiceTest.testGetJobExceptionStacktrace.bpmn20.xml" })
    public void testQueryByExceptionMessage() {
        TimerJobQuery query = managementService.createTimerJobQuery().exceptionMessage(EXCEPTION_MESSAGE);
        verifyQueryResults(query, 0);

        ProcessInstance processInstance = startProcessInstanceWithFailingJob();

        query = managementService.createTimerJobQuery().exceptionMessage(EXCEPTION_MESSAGE);
        verifyFailedJob(query, processInstance);
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/mgmt/ManagementServiceTest.testGetJobExceptionStacktrace.bpmn20.xml" })
    public void testQueryByExceptionMessageEmpty() {
        JobQuery query = managementService.createJobQuery().exceptionMessage("");
        verifyQueryResults(query, 0);

        startProcessInstanceWithFailingJob();

        query = managementService.createJobQuery().exceptionMessage("");
        verifyQueryResults(query, 0);
    }

    @Test
    public void testQueryByExceptionMessageNull() {
        assertThatThrownBy(() -> managementService.createJobQuery().exceptionMessage(null))
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class)
                .hasMessage("Provided exception message is null");
    }

    @Test
    public void testJobQueryWithExceptions() throws Throwable {

        createJobWithoutExceptionMsg();

        Job job = managementService.createJobQuery().jobId(jobEntity.getId()).singleResult();

        assertThat(job).isNotNull();

        List<Job> list = managementService.createJobQuery().withException().list();
        assertThat(list).hasSize(1);

        deleteJobInDatabase();

        createJobWithoutExceptionStacktrace();

        job = managementService.createJobQuery().jobId(jobEntity.getId()).singleResult();

        assertThat(job).isNotNull();

        list = managementService.createJobQuery().withException().list();
        assertThat(list).hasSize(1);

        deleteJobInDatabase();

    }

    @Test
    public void testJobQueryByTenantId() {
        createJobWithTenantId("muppets");
        JobQuery query = managementService.createJobQuery().jobTenantId("muppets");
        verifyQueryResults(query, 1);
        deleteJobInDatabase();
    }

    @Test
    public void testTimerJobQueryByTenantId() {
        TimerJobQuery query = managementService.createTimerJobQuery().jobTenantId("muppets");
        verifyQueryResults(query, 0);
    }

    // sorting //////////////////////////////////////////

    @Test
    public void testQuerySorting() {
        // asc
        assertThat(managementService.createJobQuery().orderByJobId().asc().count()).isEqualTo(1);
        assertThat(managementService.createJobQuery().orderByJobDuedate().asc().count()).isEqualTo(1);
        assertThat(managementService.createJobQuery().orderByJobCreateTime().asc().count()).isEqualTo(1);
        assertThat(managementService.createJobQuery().orderByExecutionId().asc().count()).isEqualTo(1);
        assertThat(managementService.createJobQuery().orderByProcessInstanceId().asc().count()).isEqualTo(1);
        assertThat(managementService.createJobQuery().orderByJobRetries().asc().count()).isEqualTo(1);

        assertThat(managementService.createTimerJobQuery().orderByJobId().asc().count()).isEqualTo(3);
        assertThat(managementService.createTimerJobQuery().orderByJobDuedate().asc().count()).isEqualTo(3);
        assertThat(managementService.createTimerJobQuery().orderByJobCreateTime().asc().count()).isEqualTo(3);
        assertThat(managementService.createTimerJobQuery().orderByExecutionId().asc().count()).isEqualTo(3);
        assertThat(managementService.createTimerJobQuery().orderByProcessInstanceId().asc().count()).isEqualTo(3);
        assertThat(managementService.createTimerJobQuery().orderByJobRetries().asc().count()).isEqualTo(3);

        // desc
        assertThat(managementService.createJobQuery().orderByJobId().desc().count()).isEqualTo(1);
        assertThat(managementService.createJobQuery().orderByJobDuedate().desc().count()).isEqualTo(1);
        assertThat(managementService.createJobQuery().orderByJobCreateTime().desc().count()).isEqualTo(1);
        assertThat(managementService.createJobQuery().orderByExecutionId().desc().count()).isEqualTo(1);
        assertThat(managementService.createJobQuery().orderByProcessInstanceId().desc().count()).isEqualTo(1);
        assertThat(managementService.createJobQuery().orderByJobRetries().desc().count()).isEqualTo(1);

        assertThat(managementService.createTimerJobQuery().orderByJobId().desc().count()).isEqualTo(3);
        assertThat(managementService.createTimerJobQuery().orderByJobDuedate().desc().count()).isEqualTo(3);
        assertThat(managementService.createTimerJobQuery().orderByJobCreateTime().desc().count()).isEqualTo(3);
        assertThat(managementService.createTimerJobQuery().orderByExecutionId().desc().count()).isEqualTo(3);
        assertThat(managementService.createTimerJobQuery().orderByProcessInstanceId().desc().count()).isEqualTo(3);
        assertThat(managementService.createTimerJobQuery().orderByJobRetries().desc().count()).isEqualTo(3);

        // sorting on multiple fields
        setRetries(processInstanceIdTwo, 2);
        processEngineConfiguration.getClock().setCurrentTime(new Date(timerThreeFireTime.getTime() + ONE_SECOND)); // make sure all timers can fire

        TimerJobQuery query = managementService.createTimerJobQuery().timers().executable().orderByJobRetries().asc().orderByJobDuedate().desc();

        List<Job> jobs = query.list();
        assertThat(jobs)
                .extracting(Job::getRetries)
                .containsExactly(2, 3, 3);
        assertThat(jobs)
                .extracting(Job::getProcessInstanceId)
                .containsExactly(
                        processInstanceIdTwo,
                        processInstanceIdThree,
                        processInstanceIdOne
                );
    }

    @Test
    public void testQueryInvalidSortingUsage() {
        assertThatThrownBy(() -> managementService.createJobQuery().orderByJobId().list())
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class)
                .hasMessage("Invalid query: call asc() or desc() after using orderByXX()");

        assertThatThrownBy(() -> managementService.createJobQuery().asc())
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class)
                .hasMessage("You should call any of the orderBy methods first before specifying a direction");
    }

    @Test
    public void testQueryWithoutScopeType() {
        JobQuery query = managementService.createJobQuery().withoutScopeType();
        verifyQueryResults(query, 1);
    }

    @Test
    public void testTimerQueryWithoutScopeId() {
        TimerJobQuery query = managementService.createTimerJobQuery().withoutScopeId();
        verifyQueryResults(query, 3);
    }

    @Test
    public void testJobQueryWithoutScopeId() {
        JobQuery query = managementService.createJobQuery().withoutScopeId();
        verifyQueryResults(query, 1);

        assertThat(query.singleResult().getId()).isEqualTo(messageId);
    }

    @Test
    public void testHistoryQueryWithoutScopeType() {
        HistoryJobEntity historyJobEntity = managementService.executeCommand((Command<HistoryJobEntity>) commandContext -> {
            JobServiceConfiguration jobServiceConfiguration = CommandContextUtil.getProcessEngineConfiguration(commandContext).getJobServiceConfiguration();
            HistoryJobService historyJobService = jobServiceConfiguration.getHistoryJobService();
            HistoryJobEntity historyJob = historyJobService.createHistoryJob();
            historyJobService.scheduleHistoryJob(historyJob);
            return historyJob;
        });

        HistoryJobQuery query = managementService.createHistoryJobQuery().jobId(historyJobEntity.getId()).withoutScopeType();
        assertThat(query.singleResult()).isNotNull();

        managementService.executeCommand((Command<Void>) commandContext -> {
            JobServiceConfiguration jobServiceConfiguration = CommandContextUtil.getProcessEngineConfiguration(commandContext).getJobServiceConfiguration();
            HistoryJobService historyJobService = jobServiceConfiguration.getHistoryJobService();
            List<HistoryJob> jobs = historyJobService.findHistoryJobsByQueryCriteria(new HistoryJobQueryImpl(commandContext, jobServiceConfiguration));
            for (HistoryJob historyJob : jobs) {
                historyJobService.deleteHistoryJob((HistoryJobEntity) historyJob);
            }

            return null;
        });
    }

    @Test
    public void testTimerJobQueryWithoutScopeType() {
        TimerJobQuery query = managementService.createTimerJobQuery().withoutScopeType();
        assertThat(query.list().size()).isEqualTo(3);
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/bpmn/oneTask.bpmn20.xml")
    public void testDeadLetterJobQueryWithoutScopeType() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("startToEnd");
        DeadLetterJobEntity deadLetterJob = managementService.executeCommand((Command<DeadLetterJobEntity>) commandContext -> {
            JobServiceConfiguration jobServiceConfiguration = CommandContextUtil.getProcessEngineConfiguration(commandContext).getJobServiceConfiguration();
            DeadLetterJobEntityManager deadLetterJobEntityManager = jobServiceConfiguration.getDeadLetterJobEntityManager();
            DeadLetterJobEntity job = deadLetterJobEntityManager.create();
            job.setJobType(Job.JOB_TYPE_MESSAGE);
            job.setProcessInstanceId(processInstance.getId());
            jobServiceConfiguration.getDeadLetterJobDataManager().insert(job);
            return job;
        });

        DeadLetterJobEntity deadLetterJob2 = managementService.executeCommand((Command<DeadLetterJobEntity>) commandContext -> {
            JobServiceConfiguration jobServiceConfiguration = CommandContextUtil.getProcessEngineConfiguration(commandContext).getJobServiceConfiguration();
            DeadLetterJobEntityManager deadLetterJobEntityManager = jobServiceConfiguration.getDeadLetterJobEntityManager();
            DeadLetterJobEntity job = deadLetterJobEntityManager.create();
            job.setJobType(Job.JOB_TYPE_MESSAGE);
            job.setScopeId("scope1");
            jobServiceConfiguration.getDeadLetterJobDataManager().insert(job);
            return job;
        });

        DeadLetterJobQuery query = managementService.createDeadLetterJobQuery().withoutScopeType();
        assertThat(query.list().size()).isEqualTo(2);

        query = managementService.createDeadLetterJobQuery().withoutProcessInstanceId();
        assertThat(query.count()).isEqualTo(1);
        assertThat(query.singleResult().getId()).isEqualTo(deadLetterJob2.getId());

        query = managementService.createDeadLetterJobQuery().withoutScopeId();
        assertThat(query.count()).isEqualTo(1);
        assertThat(query.singleResult().getId()).isEqualTo(deadLetterJob.getId());

        managementService.executeCommand((Command<Void>) commandContext -> {
            JobServiceConfiguration jobServiceConfiguration = CommandContextUtil.getProcessEngineConfiguration(commandContext).getJobServiceConfiguration();
            DeadLetterJobEntityManager deadLetterJobService = jobServiceConfiguration.getDeadLetterJobEntityManager();
            List<Job> jobs = deadLetterJobService.findJobsByQueryCriteria(new DeadLetterJobQueryImpl(commandContext, jobServiceConfiguration));
            for (Job job : jobs) {
                deadLetterJobService.delete(job.getId());
            }

            return null;
        });
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/bpmn/oneTask.bpmn20.xml")
    public void testSuspendedJobQueryWithoutScopeType() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("startToEnd");
        SuspendedJobEntity suspendedJob = managementService.executeCommand((Command<SuspendedJobEntity>) commandContext -> {
            JobServiceConfiguration jobServiceConfiguration = CommandContextUtil.getProcessEngineConfiguration(commandContext).getJobServiceConfiguration();
            SuspendedJobEntityManager suspendedJobEntityManager = jobServiceConfiguration.getSuspendedJobEntityManager();
            SuspendedJobEntity job = suspendedJobEntityManager.create();
            job.setJobType(Job.JOB_TYPE_MESSAGE);
            job.setProcessInstanceId(processInstance.getId());
            jobServiceConfiguration.getSuspendedJobEntityManager().insert(job);

            return job;
        });

        SuspendedJobEntity suspendedJob2 = managementService.executeCommand((Command<SuspendedJobEntity>) commandContext -> {
            JobServiceConfiguration jobServiceConfiguration = CommandContextUtil.getProcessEngineConfiguration(commandContext).getJobServiceConfiguration();
            SuspendedJobEntityManager suspendedJobEntityManager = jobServiceConfiguration.getSuspendedJobEntityManager();
            SuspendedJobEntity job = suspendedJobEntityManager.create();
            job.setJobType(Job.JOB_TYPE_MESSAGE);
            job.setScopeId("scope1");
            jobServiceConfiguration.getSuspendedJobEntityManager().insert(job);

            return job;
        });

        SuspendedJobQuery query = managementService.createSuspendedJobQuery().withoutScopeType();
        assertThat(query.list().size()).isEqualTo(2);

        query = managementService.createSuspendedJobQuery().withoutProcessInstanceId();
        assertThat(query.count()).isEqualTo(1);
        assertThat(query.singleResult().getId()).isEqualTo(suspendedJob2.getId());

        query = managementService.createSuspendedJobQuery().withoutScopeId();
        assertThat(query.count()).isEqualTo(1);
        assertThat(query.singleResult().getId()).isEqualTo(suspendedJob.getId());

        managementService.executeCommand((Command<Void>) commandContext -> {
            JobServiceConfiguration jobServiceConfiguration = CommandContextUtil.getProcessEngineConfiguration(commandContext).getJobServiceConfiguration();
            SuspendedJobEntityManager suspendedJobEntityManager = jobServiceConfiguration.getSuspendedJobEntityManager();
            List<Job> jobs = suspendedJobEntityManager.findJobsByQueryCriteria(new SuspendedJobQueryImpl(commandContext, jobServiceConfiguration));
            for (Job job : jobs) {
                suspendedJobEntityManager.delete(job.getId());
            }

            return null;
        });
    }

    @Test
    public void testExternalWorkerJobQueryWithoutScopeType() {
        managementService.executeCommand((Command<Void>) commandContext -> {
            JobServiceConfiguration jobServiceConfiguration = CommandContextUtil.getProcessEngineConfiguration(commandContext).getJobServiceConfiguration();
            ExternalWorkerJobEntityManager externalWorkerJobEntityManager = jobServiceConfiguration.getExternalWorkerJobEntityManager();
            ExternalWorkerJobEntity externalWorkerJob = externalWorkerJobEntityManager.create();
            externalWorkerJob.setJobType(JobEntity.JOB_TYPE_EXTERNAL_WORKER);
            jobServiceConfiguration.getExternalWorkerJobEntityManager().insert(externalWorkerJob);
            return null;
        });

        ExternalWorkerJobQuery query = managementService.createExternalWorkerJobQuery().withoutScopeType();
        assertThat(query.list().size()).isEqualTo(1);

        managementService.executeCommand((Command<Void>) commandContext -> {
            JobServiceConfiguration jobServiceConfiguration = CommandContextUtil.getProcessEngineConfiguration(commandContext).getJobServiceConfiguration();
            ExternalWorkerJobEntityManager externalWorkerJobEntityManager = jobServiceConfiguration.getExternalWorkerJobEntityManager();
            List<ExternalWorkerJob> jobs = externalWorkerJobEntityManager.findJobsByQueryCriteria(
                    new ExternalWorkerJobQueryImpl(commandContext, jobServiceConfiguration));
            for (Job externalWorkerJob : jobs) {
                externalWorkerJobEntityManager.delete(externalWorkerJob.getId());
            }

            return null;
        });
    }

    // helper ////////////////////////////////////////////////////////////

    private void setRetries(final String processInstanceId, final int retries) {
        final Job job = managementService.createTimerJobQuery().processInstanceId(processInstanceId).singleResult();
        managementService.setTimerJobRetries(job.getId(), retries);
    }

    private ProcessInstance startProcessInstanceWithFailingJob() {
        // start a process with a failing job
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("exceptionInJobExecution");

        // The execution is waiting in the first usertask. This contains a boundary
        // timer event which we will execute manual for testing purposes.
        Job timerJob = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();

        assertThat(timerJob).as("No job found for process instance").isNotNull();

        assertThatThrownBy(() -> {
            managementService.moveTimerToExecutableJob(timerJob.getId());
            managementService.executeJob(timerJob.getId());
        })
                .isInstanceOf(FlowableException.class)
                .hasMessage(EXCEPTION_MESSAGE);

        return processInstance;
    }

    private void verifyFailedJob(TimerJobQuery query, ProcessInstance processInstance) {
        verifyQueryResults(query, 1);

        Job failedJob = query.singleResult();
        assertThat(failedJob).isNotNull();
        assertThat(failedJob.getProcessInstanceId()).isEqualTo(processInstance.getId());
        assertThat(failedJob.getExceptionMessage()).containsSequence(EXCEPTION_MESSAGE);
    }

    private void verifyQueryResults(JobQuery query, int countExpected) {
        assertThat(query.list()).hasSize(countExpected);
        assertThat(query.count()).isEqualTo(countExpected);

        if (countExpected == 1) {
            assertThat(query.singleResult()).isNotNull();
        } else if (countExpected > 1) {
            verifySingleResultFails(query);
        } else if (countExpected == 0) {
            assertThat(query.singleResult()).isNull();
        }
    }

    private void verifySingleResultFails(JobQuery query) {
        assertThatThrownBy(() -> query.singleResult())
                .isExactlyInstanceOf(FlowableException.class);
    }

    private void verifyQueryResults(TimerJobQuery query, int countExpected) {
        assertThat(query.list()).hasSize(countExpected);
        assertThat(query.count()).isEqualTo(countExpected);

        if (countExpected == 1) {
            assertThat(query.singleResult()).isNotNull();
        } else if (countExpected > 1) {
            verifySingleResultFails(query);
        } else if (countExpected == 0) {
            assertThat(query.singleResult()).isNull();
        }
    }

    private void verifySingleResultFails(TimerJobQuery query) {
        assertThatThrownBy(() -> query.singleResult())
                .isExactlyInstanceOf(FlowableException.class);
    }

    private void createJobWithoutExceptionMsg() {
        CommandExecutor commandExecutor = processEngineConfiguration.getCommandExecutor();
        commandExecutor.execute(new Command<Void>() {

            @Override
            public Void execute(CommandContext commandContext) {
                JobService jobService = CommandContextUtil.getJobService(commandContext);
                jobEntity = jobService.createJob();
                jobEntity.setJobType(Job.JOB_TYPE_MESSAGE);
                jobEntity.setLockOwner(UUID.randomUUID().toString());
                jobEntity.setRetries(0);

                StringWriter stringWriter = new StringWriter();
                NullPointerException exception = new NullPointerException();
                exception.printStackTrace(new PrintWriter(stringWriter));
                jobEntity.setExceptionStacktrace(stringWriter.toString());

                jobService.insertJob(jobEntity);

                assertThat(jobEntity.getId()).isNotNull();

                return null;

            }
        });

    }

    private void createJobWithoutExceptionStacktrace() {
        CommandExecutor commandExecutor = processEngineConfiguration.getCommandExecutor();
        commandExecutor.execute(new Command<Void>() {

            @Override
            public Void execute(CommandContext commandContext) {
                JobService jobService = CommandContextUtil.getJobService(commandContext);
                jobEntity = jobService.createJob();
                jobEntity.setJobType(Job.JOB_TYPE_MESSAGE);
                jobEntity.setLockOwner(UUID.randomUUID().toString());
                jobEntity.setRetries(0);

                jobEntity.setExceptionMessage("I'm supposed to fail");

                jobService.insertJob(jobEntity);

                assertThat(jobEntity.getId()).isNotNull();

                return null;

            }
        });

    }

    private JobEntity createJobWithHandlerType(String handlerType) {
        CommandExecutor commandExecutor = processEngineConfiguration.getCommandExecutor();
        return commandExecutor.execute(new Command<JobEntity>() {

            @Override
            public JobEntity execute(CommandContext commandContext) {
                JobService jobService = CommandContextUtil.getJobService(commandContext);
                JobEntity result = jobService.createJob();
                result.setJobType(Job.JOB_TYPE_MESSAGE);
                result.setRetries(0);
                result.setJobHandlerType(handlerType);
                jobService.insertJob(result);
                assertThat(result.getId()).isNotNull();
                return result;
            }
        });
    }

    private void createDeadLetterJobWithHandlerType(String handlerType) {
        CommandExecutor commandExecutor = processEngineConfiguration.getCommandExecutor();
        commandExecutor.execute(new Command<Void>() {

            @Override
            public Void execute(CommandContext commandContext) {
                JobService jobService = CommandContextUtil.getJobService(commandContext);
                jobService.moveJobToDeadLetterJob(createJobWithHandlerType(handlerType));
                return null;
            }
        });
    }

    private void createSuspendedJobWithHandlerType(String handlerType) {
        CommandExecutor commandExecutor = processEngineConfiguration.getCommandExecutor();
        commandExecutor.execute(new Command<Void>() {

            @Override
            public Void execute(CommandContext commandContext) {
                JobService jobService = CommandContextUtil.getJobService(commandContext);
                jobService.moveJobToSuspendedJob(createJobWithHandlerType(handlerType));
                return null;
            }
        });
    }

    private void createHistoryobWithHandlerType(String handlerType) {
        HistoryJobEntity historyJobEntity = managementService.executeCommand((Command<HistoryJobEntity>) commandContext -> {
            JobServiceConfiguration jobServiceConfiguration = CommandContextUtil.getProcessEngineConfiguration(commandContext).getJobServiceConfiguration();
            HistoryJobService historyJobService = jobServiceConfiguration.getHistoryJobService();
            HistoryJobEntity historyJob = historyJobService.createHistoryJob();
            historyJob.setJobHandlerType(handlerType);
            historyJobService.scheduleHistoryJob(historyJob);
            return historyJob;
        });
    }

    private void createJobWithTenantId(String tenantId) {
        CommandExecutor commandExecutor = processEngineConfiguration.getCommandExecutor();
        commandExecutor.execute(new Command<Void>() {

            @Override
            public Void execute(CommandContext commandContext) {
                JobService jobService = CommandContextUtil.getJobService(commandContext);
                jobEntity = jobService.createJob();
                jobEntity.setJobType(Job.JOB_TYPE_MESSAGE);
                jobEntity.setLockOwner(UUID.randomUUID().toString());
                jobEntity.setRetries(0);
                jobEntity.setTenantId(tenantId);

                StringWriter stringWriter = new StringWriter();
                NullPointerException exception = new NullPointerException();
                exception.printStackTrace(new PrintWriter(stringWriter));
                jobEntity.setExceptionStacktrace(stringWriter.toString());

                jobService.insertJob(jobEntity);

                assertThat(jobEntity.getId()).isNotNull();

                return null;

            }
        });

    }

    private void deleteJobInDatabase() {
        CommandExecutor commandExecutor = processEngineConfiguration.getCommandExecutor();
        commandExecutor.execute(new Command<Void>() {

            @Override
            public Void execute(CommandContext commandContext) {
                JobService jobService = CommandContextUtil.getJobService(commandContext);
                jobService.deleteJob(jobEntity.getId());
                return null;
            }
        });
    }

}
