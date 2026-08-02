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
package org.flowable.engine.test.externalworker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;

import org.flowable.common.engine.api.FlowableForbiddenException;
import org.flowable.common.engine.api.FlowableOptimisticLockingException;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.test.Deployment;
import org.flowable.job.api.AcquiredExternalWorkerJob;
import org.flowable.job.api.ExternalWorkerJob;
import org.flowable.job.service.impl.cmd.ExtendExternalWorkerJobLockCmd;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ExternalWorkerLockExtensionTest extends PluggableFlowableTestCase {

    private boolean asyncExecutorActivated;

    @BeforeEach
    void disableAsyncExecutorIfNeeded() {
        asyncExecutorActivated = processEngineConfiguration.getAsyncExecutor().isActive();
        if (asyncExecutorActivated) {
            processEngineConfiguration.getAsyncExecutor().shutdown();
        }
    }

    @AfterEach
    void enableAsyncExecutorIfNeeded() {
        if (asyncExecutorActivated) {
            processEngineConfiguration.getAsyncExecutor().start();
        }
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/externalworker/ExternalWorkerServiceTaskTest.testSimpleExclusive.bpmn20.xml")
    void extendsJobAndExclusiveProcessScopeLock() {
        AcquiredExternalWorkerJob acquiredJob = startAndAcquireAt(Instant.parse("2026-07-16T12:00:00Z"));
        Date expectedExpiration = acquiredJob.getLockExpirationTime();

        processEngineConfiguration.getClock().setCurrentTime(Date.from(Instant.parse("2026-07-16T12:02:00Z")));
        Date newExpiration = extend(acquiredJob, "worker-1", Duration.ofMinutes(5), expectedExpiration);

        assertThat(newExpiration).isEqualTo(Date.from(Instant.parse("2026-07-16T12:07:00Z")));
        ExternalWorkerJob renewedJob = managementService.createExternalWorkerJobQuery().jobId(acquiredJob.getId()).singleResult();
        assertThat(renewedJob.getLockOwner()).isEqualTo("worker-1");
        assertThat(renewedJob.getLockExpirationTime()).isEqualTo(newExpiration);

        ExecutionEntity processInstance = (ExecutionEntity) runtimeService.createProcessInstanceQuery()
                .processInstanceId(acquiredJob.getProcessInstanceId())
                .singleResult();
        assertThat(processInstance.getLockOwner()).isEqualTo("worker-1");
        assertThat(processInstance.getLockTime()).isEqualTo(newExpiration);
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/externalworker/ExternalWorkerServiceTaskTest.testSimpleExclusive.bpmn20.xml")
    void rejectsWorkerThatDoesNotOwnTheLock() {
        AcquiredExternalWorkerJob acquiredJob = startAndAcquireAt(Instant.parse("2026-07-16T12:00:00Z"));

        assertThatThrownBy(() -> extend(
                acquiredJob, "worker-2", Duration.ofMinutes(5), acquiredJob.getLockExpirationTime()))
                .isInstanceOf(FlowableForbiddenException.class)
                .hasMessageContaining("does not hold a lock");
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/externalworker/ExternalWorkerServiceTaskTest.testSimpleExclusive.bpmn20.xml")
    void rejectsStaleExpectedExpiration() {
        AcquiredExternalWorkerJob acquiredJob = startAndAcquireAt(Instant.parse("2026-07-16T12:00:00Z"));
        Date originalExpiration = acquiredJob.getLockExpirationTime();

        processEngineConfiguration.getClock().setCurrentTime(Date.from(Instant.parse("2026-07-16T12:01:00Z")));
        extend(acquiredJob, "worker-1", Duration.ofMinutes(5), originalExpiration);

        assertThatThrownBy(() -> extend(acquiredJob, "worker-1", Duration.ofMinutes(5), originalExpiration))
                .isInstanceOf(FlowableOptimisticLockingException.class)
                .hasMessageContaining("changed by another request");
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/externalworker/ExternalWorkerServiceTaskTest.testSimpleExclusive.bpmn20.xml")
    void rejectsExpiredLock() {
        AcquiredExternalWorkerJob acquiredJob = startAndAcquireAt(Instant.parse("2026-07-16T12:00:00Z"));
        processEngineConfiguration.getClock().setCurrentTime(acquiredJob.getLockExpirationTime());

        assertThatThrownBy(() -> extend(
                acquiredJob, "worker-1", Duration.ofMinutes(5), acquiredJob.getLockExpirationTime()))
                .isInstanceOf(FlowableOptimisticLockingException.class)
                .hasMessageContaining("has expired");
    }

    private Date extend(
            AcquiredExternalWorkerJob job,
            String workerId,
            Duration lockDuration,
            Date expectedExpiration) {

        return processEngineConfiguration.getCommandExecutor().execute(new ExtendExternalWorkerJobLockCmd(
                job.getId(),
                workerId,
                lockDuration,
                expectedExpiration,
                processEngineConfiguration.getJobServiceConfiguration()));
    }

    private AcquiredExternalWorkerJob startAndAcquireAt(Instant acquireTime) {
        runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("simpleExternalWorker")
                .start();
        processEngineConfiguration.getClock().setCurrentTime(Date.from(acquireTime));
        return managementService.createExternalWorkerJobAcquireBuilder()
                .topic("simple", Duration.ofMinutes(5))
                .acquireAndLock(1, "worker-1")
                .getFirst();
    }
}
