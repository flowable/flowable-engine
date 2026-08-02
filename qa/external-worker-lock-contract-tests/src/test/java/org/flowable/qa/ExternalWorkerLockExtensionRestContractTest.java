package org.flowable.qa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.impl.CmmnManagementServiceImpl;
import org.flowable.common.engine.api.FlowableForbiddenException;
import org.flowable.common.engine.api.FlowableOptimisticLockingException;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.common.engine.impl.interceptor.CommandExecutor;
import org.flowable.common.engine.impl.interceptor.EngineConfigurationConstants;
import org.flowable.common.rest.exception.FlowableConflictException;
import org.flowable.engine.impl.ManagementServiceImpl;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.external.job.rest.service.api.acquire.ExternalWorkerJobLockExtensionRequest;
import org.flowable.external.job.rest.service.api.acquire.ExternalWorkerJobLockExtensionResponse;
import org.flowable.external.job.rest.service.api.acquire.ExternalWorkerJobLockResource;
import org.flowable.job.api.ExternalWorkerJob;
import org.flowable.job.api.ExternalWorkerJobQuery;
import org.flowable.job.service.JobServiceConfiguration;
import org.flowable.job.service.impl.cmd.ExtendExternalWorkerJobLockCmd;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.PostMapping;

class ExternalWorkerLockExtensionRestContractTest {

    private static final String JOB_ID = "job-1";
    private static final String WORKER_ID = "worker-1";
    private static final Date EXPECTED_EXPIRATION = Date.from(Instant.parse("2026-07-16T12:05:00Z"));
    private static final Date RENEWED_EXPIRATION = Date.from(Instant.parse("2026-07-16T12:07:00Z"));
    private static final Duration LOCK_DURATION = Duration.ofMinutes(5);

    @Test
    void exposesTheExpectedRestRoute() throws NoSuchMethodException {
        Method method = ExternalWorkerJobLockResource.class.getMethod(
                "extendLock", String.class, ExternalWorkerJobLockExtensionRequest.class);

        PostMapping mapping = method.getAnnotation(PostMapping.class);

        assertThat(mapping).isNotNull();
        assertThat(mapping.value()).containsExactly("/acquire/jobs/{jobId}/extend-lock");
        assertThat(mapping.produces()).containsExactly("application/json");
    }

    @Test
    void dispatchesBpmnRenewalAndReturnsTheNewExpiration() {
        ManagementServiceImpl managementService = mock(ManagementServiceImpl.class);
        CommandExecutor commandExecutor = configureBpmnService(managementService, bpmnJob());
        doReturn(RENEWED_EXPIRATION).when(commandExecutor).execute(any(ExtendExternalWorkerJobLockCmd.class));

        ExternalWorkerJobLockResource resource = new ExternalWorkerJobLockResource();
        resource.setManagementService(managementService);

        ExternalWorkerJobLockExtensionResponse response = resource.extendLock(JOB_ID, request(WORKER_ID));

        assertThat(response.getJobId()).isEqualTo(JOB_ID);
        assertThat(response.getWorkerId()).isEqualTo(WORKER_ID);
        assertThat(response.getLockExpirationTime()).isEqualTo(RENEWED_EXPIRATION);
        verify(commandExecutor).execute(any(ExtendExternalWorkerJobLockCmd.class));
    }

    @Test
    void dispatchesCmmnRenewal() {
        CmmnManagementServiceImpl managementService = mock(CmmnManagementServiceImpl.class);
        CommandExecutor commandExecutor = configureCmmnService(managementService, cmmnJob());
        doReturn(RENEWED_EXPIRATION).when(commandExecutor).execute(any(ExtendExternalWorkerJobLockCmd.class));

        ExternalWorkerJobLockResource resource = new ExternalWorkerJobLockResource();
        resource.setCmmnManagementService(managementService);

        ExternalWorkerJobLockExtensionResponse response = resource.extendLock(JOB_ID, request(WORKER_ID));

        assertThat(response.getLockExpirationTime()).isEqualTo(RENEWED_EXPIRATION);
        verify(commandExecutor).execute(any(ExtendExternalWorkerJobLockCmd.class));
    }

    @Test
    void rejectsAWorkerThatDoesNotOwnTheJob() {
        ManagementServiceImpl managementService = mock(ManagementServiceImpl.class);
        configureBpmnQuery(managementService, bpmnJob());

        ExternalWorkerJobLockResource resource = new ExternalWorkerJobLockResource();
        resource.setManagementService(managementService);

        assertThatThrownBy(() -> resource.extendLock(JOB_ID, request("worker-2")))
                .isInstanceOf(FlowableForbiddenException.class)
                .hasMessageContaining("does not hold a lock");
    }

    @Test
    void translatesOptimisticLockingIntoAConflict() {
        ManagementServiceImpl managementService = mock(ManagementServiceImpl.class);
        CommandExecutor commandExecutor = configureBpmnService(managementService, bpmnJob());
        doThrow(new FlowableOptimisticLockingException("stale lock"))
                .when(commandExecutor)
                .execute(any(ExtendExternalWorkerJobLockCmd.class));

        ExternalWorkerJobLockResource resource = new ExternalWorkerJobLockResource();
        resource.setManagementService(managementService);

        assertThatThrownBy(() -> resource.extendLock(JOB_ID, request(WORKER_ID)))
                .isInstanceOf(FlowableConflictException.class)
                .hasMessageContaining("stale lock");
    }

    private CommandExecutor configureBpmnService(ManagementServiceImpl managementService, ExternalWorkerJob job) {
        configureBpmnQuery(managementService, job);
        ProcessEngineConfigurationImpl engineConfiguration = mock(ProcessEngineConfigurationImpl.class);
        JobServiceConfiguration jobServiceConfiguration = mock(JobServiceConfiguration.class);
        CommandExecutor commandExecutor = mock(CommandExecutor.class);
        when(engineConfiguration.getServiceConfigurations()).thenReturn(Map.of(
                EngineConfigurationConstants.KEY_JOB_SERVICE_CONFIG,
                jobServiceConfiguration));
        when(managementService.getConfiguration()).thenReturn(engineConfiguration);
        when(managementService.getCommandExecutor()).thenReturn(commandExecutor);
        return commandExecutor;
    }

    private void configureBpmnQuery(ManagementServiceImpl managementService, ExternalWorkerJob job) {
        ExternalWorkerJobQuery query = mock(ExternalWorkerJobQuery.class);
        when(managementService.createExternalWorkerJobQuery()).thenReturn(query);
        when(query.jobId(JOB_ID)).thenReturn(query);
        when(query.singleResult()).thenReturn(job);
    }

    private CommandExecutor configureCmmnService(CmmnManagementServiceImpl managementService, ExternalWorkerJob job) {
        ExternalWorkerJobQuery query = mock(ExternalWorkerJobQuery.class);
        when(managementService.createExternalWorkerJobQuery()).thenReturn(query);
        when(query.jobId(JOB_ID)).thenReturn(query);
        when(query.singleResult()).thenReturn(job);

        CmmnEngineConfiguration engineConfiguration = mock(CmmnEngineConfiguration.class);
        JobServiceConfiguration jobServiceConfiguration = mock(JobServiceConfiguration.class);
        CommandExecutor commandExecutor = mock(CommandExecutor.class);
        when(engineConfiguration.getServiceConfigurations()).thenReturn(Map.of(
                EngineConfigurationConstants.KEY_JOB_SERVICE_CONFIG,
                jobServiceConfiguration));
        when(managementService.getConfiguration()).thenReturn(engineConfiguration);
        when(managementService.getCommandExecutor()).thenReturn(commandExecutor);
        return commandExecutor;
    }

    private ExternalWorkerJob bpmnJob() {
        ExternalWorkerJob job = mock(ExternalWorkerJob.class);
        when(job.getId()).thenReturn(JOB_ID);
        when(job.getLockOwner()).thenReturn(WORKER_ID);
        when(job.getProcessInstanceId()).thenReturn("process-1");
        return job;
    }

    private ExternalWorkerJob cmmnJob() {
        ExternalWorkerJob job = mock(ExternalWorkerJob.class);
        when(job.getId()).thenReturn(JOB_ID);
        when(job.getLockOwner()).thenReturn(WORKER_ID);
        when(job.getScopeType()).thenReturn(ScopeTypes.CMMN);
        return job;
    }

    private ExternalWorkerJobLockExtensionRequest request(String workerId) {
        ExternalWorkerJobLockExtensionRequest request = new ExternalWorkerJobLockExtensionRequest();
        request.setWorkerId(workerId);
        request.setLockDuration(LOCK_DURATION);
        request.setExpectedLockExpirationTime(EXPECTED_EXPIRATION);
        return request;
    }
}
