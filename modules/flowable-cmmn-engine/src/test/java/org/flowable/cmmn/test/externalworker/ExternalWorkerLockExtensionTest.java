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
package org.flowable.cmmn.test.externalworker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;

import org.flowable.cmmn.engine.impl.persistence.entity.CaseInstanceEntity;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.test.FlowableCmmnTestCase;
import org.flowable.common.engine.api.FlowableOptimisticLockingException;
import org.flowable.job.api.AcquiredExternalWorkerJob;
import org.flowable.job.api.ExternalWorkerJob;
import org.flowable.job.service.impl.cmd.ExtendExternalWorkerJobLockCmd;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ExternalWorkerLockExtensionTest extends FlowableCmmnTestCase {

    private boolean asyncExecutorActivated;

    @BeforeEach
    void disableAsyncExecutorIfNeeded() {
        asyncExecutorActivated = cmmnEngineConfiguration.getAsyncExecutor().isActive();
        if (asyncExecutorActivated) {
            cmmnEngineConfiguration.getAsyncExecutor().shutdown();
        }
    }

    @AfterEach
    void enableAsyncExecutorIfNeeded() {
        if (asyncExecutorActivated) {
            cmmnEngineConfiguration.getAsyncExecutor().start();
        }
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/externalworker/ExternalWorkerServiceTaskTest.testSimpleExclusive.cmmn")
    void extendsJobAndExclusiveCaseScopeLock() {
        AcquiredExternalWorkerJob acquiredJob = startAndAcquireAt(Instant.parse("2026-07-16T12:00:00Z"));
        Date expectedExpiration = acquiredJob.getLockExpirationTime();

        cmmnEngineConfiguration.getClock().setCurrentTime(Date.from(Instant.parse("2026-07-16T12:02:00Z")));
        Date newExpiration = extend(acquiredJob, Duration.ofMinutes(5), expectedExpiration);

        assertThat(newExpiration).isEqualTo(Date.from(Instant.parse("2026-07-16T12:07:00Z")));
        ExternalWorkerJob renewedJob = cmmnManagementService.createExternalWorkerJobQuery().singleResult();
        assertThat(renewedJob.getLockOwner()).isEqualTo("worker-1");
        assertThat(renewedJob.getLockExpirationTime()).isEqualTo(newExpiration);

        CaseInstanceEntity caseInstance = (CaseInstanceEntity) cmmnRuntimeService.createCaseInstanceQuery()
                .caseInstanceId(acquiredJob.getScopeId())
                .singleResult();
        assertThat(caseInstance.getLockOwner()).isEqualTo("worker-1");
        assertThat(caseInstance.getLockTime()).isEqualTo(newExpiration);
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/externalworker/ExternalWorkerServiceTaskTest.testSimpleExclusive.cmmn")
    void rejectsStaleExpectedExpiration() {
        AcquiredExternalWorkerJob acquiredJob = startAndAcquireAt(Instant.parse("2026-07-16T12:00:00Z"));
        Date originalExpiration = acquiredJob.getLockExpirationTime();

        cmmnEngineConfiguration.getClock().setCurrentTime(Date.from(Instant.parse("2026-07-16T12:01:00Z")));
        extend(acquiredJob, Duration.ofMinutes(5), originalExpiration);

        assertThatThrownBy(() -> extend(acquiredJob, Duration.ofMinutes(5), originalExpiration))
                .isInstanceOf(FlowableOptimisticLockingException.class)
                .hasMessageContaining("changed by another request");
    }

    private Date extend(AcquiredExternalWorkerJob job, Duration lockDuration, Date expectedExpiration) {
        return cmmnEngineConfiguration.getCommandExecutor().execute(new ExtendExternalWorkerJobLockCmd(
                job.getId(),
                "worker-1",
                lockDuration,
                expectedExpiration,
                cmmnEngineConfiguration.getJobServiceConfiguration()));
    }

    private AcquiredExternalWorkerJob startAndAcquireAt(Instant acquireTime) {
        cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("simpleExternalWorker")
                .start();
        cmmnEngineConfiguration.getClock().setCurrentTime(Date.from(acquireTime));
        return cmmnManagementService.createExternalWorkerJobAcquireBuilder()
                .topic("simple", Duration.ofMinutes(5))
                .acquireAndLock(1, "worker-1")
                .getFirst();
    }
}
