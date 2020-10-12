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

import java.time.Duration;
import java.util.Collections;
import java.util.List;

import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.engine.impl.jobexecutor.ExternalWorkerTaskCompleteJobHandler;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.identitylink.api.IdentityLinkType;
import org.flowable.job.api.AcquiredExternalWorkerJob;
import org.flowable.job.api.ExternalWorkerJob;
import org.flowable.job.api.ExternalWorkerJobQuery;
import org.flowable.job.api.Job;
import org.junit.jupiter.api.Test;

/**
 * @author Filip Hrisafov
 */
public class ExternalWorkerJobQueryTest extends PluggableFlowableTestCase {

    @Test
    @Deployment(resources = "org/flowable/engine/test/api/mgmt/ExternalWorkerJobQueryTest.bpmn20.xml")
    public void testQueryByNoCriteria() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("externalWorkerJobQueryTest");

        ExternalWorkerJobQuery query = managementService.createExternalWorkerJobQuery();
        assertThat(query.count()).isEqualTo(2);
        assertThat(query.list())
                .extracting(ExternalWorkerJob::getElementId)
                .containsExactlyInAnyOrder("externalOrder", "externalCustomer1");

        assertThat(query.list())
                .extracting(ExternalWorkerJob::getProcessInstanceId)
                .containsOnly(processInstance.getId());
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/api/mgmt/ExternalWorkerJobQueryTest.bpmn20.xml")
    public void testQueryByProcessInstanceId() {
        ProcessInstance processInstance1 = runtimeService.startProcessInstanceByKey("externalWorkerJobQueryTest");
        ProcessInstance processInstance2 = runtimeService.startProcessInstanceByKey("externalWorkerJobQueryTest");
        ExternalWorkerJobQuery query = managementService.createExternalWorkerJobQuery().processInstanceId(processInstance1.getId());
        assertThat(query.count()).isEqualTo(2);
        assertThat(query.list())
                .extracting(ExternalWorkerJob::getProcessInstanceId)
                .containsOnly(processInstance1.getId());

        query = managementService.createExternalWorkerJobQuery().processInstanceId(processInstance2.getId());
        assertThat(query.count()).isEqualTo(2);
        assertThat(query.list())
                .extracting(ExternalWorkerJob::getProcessInstanceId)
                .containsOnly(processInstance2.getId());

        query = managementService.createExternalWorkerJobQuery().processInstanceId("invalid");
        assertThat(query.count()).isZero();
        assertThat(query.list()).isEmpty();
        assertThat(query.singleResult()).isNull();
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/api/mgmt/ExternalWorkerJobQueryTest.bpmn20.xml")
    public void testQueryByProcessDefinitionId() {
        ProcessInstance processInstance1 = runtimeService.startProcessInstanceByKey("externalWorkerJobQueryTest");
        ProcessInstance processInstance2 = runtimeService.startProcessInstanceByKey("externalWorkerJobQueryTest");
        ExternalWorkerJobQuery query = managementService.createExternalWorkerJobQuery().processDefinitionId(processInstance1.getProcessDefinitionId());
        assertThat(query.count()).isEqualTo(4);
        assertThat(query.list())
                .extracting(ExternalWorkerJob::getProcessInstanceId)
                .containsOnly(processInstance1.getId(), processInstance2.getId());

        query = managementService.createExternalWorkerJobQuery().processDefinitionId("invalid");
        assertThat(query.count()).isZero();
        assertThat(query.list()).isEmpty();
        assertThat(query.singleResult()).isNull();
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/api/mgmt/ExternalWorkerJobQueryTest.bpmn20.xml")
    public void testQueryByExecutionId() {
        runtimeService.startProcessInstanceByKey("externalWorkerJobQueryTest");

        Execution orderExecution = runtimeService.createExecutionQuery().activityId("externalOrder").singleResult();
        assertThat(orderExecution).isNotNull();
        Execution customerExecution = runtimeService.createExecutionQuery().activityId("externalCustomer1").singleResult();
        assertThat(customerExecution).isNotNull();

        ExternalWorkerJobQuery query = managementService.createExternalWorkerJobQuery().executionId(orderExecution.getId());
        assertThat(query.count()).isEqualTo(1);
        assertThat(query.list())
                .extracting(ExternalWorkerJob::getElementId)
                .containsExactlyInAnyOrder("externalOrder");
        assertThat(query.singleResult()).isNotNull();

        query = managementService.createExternalWorkerJobQuery().executionId(customerExecution.getId());
        assertThat(query.count()).isEqualTo(1);
        assertThat(query.list())
                .extracting(ExternalWorkerJob::getElementId)
                .containsExactlyInAnyOrder("externalCustomer1");
        assertThat(query.singleResult()).isNotNull();

        query = managementService.createExternalWorkerJobQuery().executionId("invalid");
        assertThat(query.count()).isZero();
        assertThat(query.list()).isEmpty();
        assertThat(query.singleResult()).isNull();
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/api/mgmt/ExternalWorkerJobQueryTest.bpmn20.xml")
    public void testQueryByElementId() {
        ProcessInstance processInstance1 = runtimeService.startProcessInstanceByKey("externalWorkerJobQueryTest");
        ProcessInstance processInstance2 = runtimeService.startProcessInstanceByKey("externalWorkerJobQueryTest");

        ExternalWorkerJobQuery query = managementService.createExternalWorkerJobQuery().elementId("externalOrder");
        assertThat(query.count()).isEqualTo(2);
        assertThat(query.list())
                .extracting(ExternalWorkerJob::getProcessInstanceId)
                .containsExactlyInAnyOrder(processInstance1.getId(), processInstance2.getId());

        query = managementService.createExternalWorkerJobQuery().elementId("invalid");
        assertThat(query.count()).isZero();
        assertThat(query.list()).isEmpty();

    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/api/mgmt/ExternalWorkerJobQueryTest.bpmn20.xml")
    public void testQueryByElementName() {
        ProcessInstance processInstance1 = runtimeService.startProcessInstanceByKey("externalWorkerJobQueryTest");
        ProcessInstance processInstance2 = runtimeService.startProcessInstanceByKey("externalWorkerJobQueryTest");

        ExternalWorkerJobQuery query = managementService.createExternalWorkerJobQuery().elementName("Customer Service");
        assertThat(query.count()).isEqualTo(2);
        assertThat(query.list())
                .extracting(ExternalWorkerJob::getProcessInstanceId)
                .containsExactlyInAnyOrder(processInstance1.getId(), processInstance2.getId());

        query = managementService.createExternalWorkerJobQuery().elementName("invalid");
        assertThat(query.count()).isZero();
        assertThat(query.list()).isEmpty();
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/api/mgmt/ExternalWorkerJobQueryTest.bpmn20.xml")
    public void testQueryByHandlerType() {
        runtimeService.startProcessInstanceByKey("externalWorkerJobQueryTest");

        ExternalWorkerJobQuery query = managementService.createExternalWorkerJobQuery().handlerType(ExternalWorkerTaskCompleteJobHandler.TYPE);
        assertThat(query.count()).isEqualTo(2);
        assertThat(query.list()).hasSize(2);

        query = managementService.createExternalWorkerJobQuery().handlerType("invalid");
        assertThat(query.count()).isZero();
        assertThat(query.list()).isEmpty();
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/api/mgmt/ExternalWorkerJobQueryTest.bpmn20.xml")
    public void testQueryByException() {
        runtimeService.startProcessInstanceByKey("externalWorkerJobQueryTest");

        List<AcquiredExternalWorkerJob> acquiredJobs = managementService.createExternalWorkerJobAcquireBuilder()
                .topic("orderService", Duration.ofMinutes(10))
                .acquireAndLock(1, "testWorker");

        managementService.createExternalWorkerJobFailureBuilder(acquiredJobs.get(0).getId(), "testWorker")
                .errorMessage("Error message")
                .errorDetails("Error details")
                .fail();

        ExternalWorkerJobQuery query = managementService.createExternalWorkerJobQuery().withException();
        assertThat(query.count()).isEqualTo(1);
        assertThat(query.list())
                .extracting(ExternalWorkerJob::getElementId)
                .containsExactlyInAnyOrder("externalOrder");

        ExternalWorkerJob job = query.singleResult();

        assertThat(job).isNotNull();
        assertThat(job.getExceptionMessage()).isEqualTo("Error message");
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/api/mgmt/ExternalWorkerJobQueryTest.bpmn20.xml")
    public void testQueryByExceptionMessage() {
        runtimeService.startProcessInstanceByKey("externalWorkerJobQueryTest");

        List<AcquiredExternalWorkerJob> acquiredJobs = managementService.createExternalWorkerJobAcquireBuilder()
                .topic("orderService", Duration.ofMinutes(10))
                .acquireAndLock(1, "testWorker");

        managementService.createExternalWorkerJobFailureBuilder(acquiredJobs.get(0).getId(), "testWorker")
                .errorMessage("Error message")
                .errorDetails("Error details")
                .fail();

        ExternalWorkerJobQuery query = managementService.createExternalWorkerJobQuery().exceptionMessage("Error message");
        assertThat(query.count()).isEqualTo(1);
        assertThat(query.list())
                .extracting(ExternalWorkerJob::getElementId)
                .containsExactlyInAnyOrder("externalOrder");

        ExternalWorkerJob job = query.singleResult();

        assertThat(job).isNotNull();
        assertThat(job.getExceptionMessage()).isEqualTo("Error message");

        query = managementService.createExternalWorkerJobQuery().exceptionMessage("Error");
        assertThat(query.count()).isZero();
        assertThat(query.list()).isEmpty();
        assertThat(query.singleResult()).isNull();
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/api/mgmt/ExternalWorkerJobQueryTest.bpmn20.xml")
    public void testQueryByLockedAndUnlocked() {
        runtimeService.startProcessInstanceByKey("externalWorkerJobQueryTest");

        managementService.createExternalWorkerJobAcquireBuilder()
                .topic("orderService", Duration.ofMinutes(10))
                .acquireAndLock(1, "testWorker");

        ExternalWorkerJobQuery query = managementService.createExternalWorkerJobQuery().locked();
        assertThat(query.count()).isEqualTo(1);
        assertThat(query.list())
                .extracting(ExternalWorkerJob::getElementId)
                .containsExactlyInAnyOrder("externalOrder");

        ExternalWorkerJob job = query.singleResult();

        assertThat(job).isNotNull();
        assertThat(job.getLockOwner()).isEqualTo("testWorker");
        assertThat(job.getLockExpirationTime()).isNotNull();

        query = managementService.createExternalWorkerJobQuery().unlocked();
        assertThat(query.count()).isEqualTo(1);
        assertThat(query.list())
                .extracting(ExternalWorkerJob::getElementId)
                .containsExactlyInAnyOrder("externalCustomer1");

        job = query.singleResult();

        assertThat(job).isNotNull();
        assertThat(job.getLockOwner()).isNull();
        assertThat(job.getLockExpirationTime()).isNull();
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/api/mgmt/ExternalWorkerJobQueryTest.bpmn20.xml")
    public void testQueryByLockOwner() {
        runtimeService.startProcessInstanceByKey("externalWorkerJobQueryTest");

        managementService.createExternalWorkerJobAcquireBuilder()
                .topic("orderService", Duration.ofMinutes(10))
                .acquireAndLock(1, "testWorker1");

        managementService.createExternalWorkerJobAcquireBuilder()
                .topic("customerService", Duration.ofMinutes(10))
                .acquireAndLock(1, "testWorker2");

        ExternalWorkerJobQuery query = managementService.createExternalWorkerJobQuery().lockOwner("testWorker1");
        assertThat(query.count()).isEqualTo(1);
        assertThat(query.list())
                .extracting(ExternalWorkerJob::getElementId)
                .containsExactlyInAnyOrder("externalOrder");

        ExternalWorkerJob job = query.singleResult();

        assertThat(job).isNotNull();
        assertThat(job.getLockOwner()).isEqualTo("testWorker1");
        assertThat(job.getLockExpirationTime()).isNotNull();

        query = managementService.createExternalWorkerJobQuery().lockOwner("testWorker2");
        assertThat(query.count()).isEqualTo(1);
        assertThat(query.list())
                .extracting(ExternalWorkerJob::getElementId)
                .containsExactlyInAnyOrder("externalCustomer1");

        job = query.singleResult();

        assertThat(job).isNotNull();
        assertThat(job.getLockOwner()).isEqualTo("testWorker2");
        assertThat(job.getLockExpirationTime()).isNotNull();

        query = managementService.createExternalWorkerJobQuery().lockOwner("invalid");
        assertThat(query.count()).isZero();
        assertThat(query.list()).isEmpty();
        assertThat(query.singleResult()).isNull();
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/api/mgmt/ExternalWorkerJobQueryTest.bpmn20.xml")
    public void testQueryByCorrelationId() {
        runtimeService.startProcessInstanceByKey("externalWorkerJobQueryTest");
        ExternalWorkerJob workerJob = managementService.createExternalWorkerJobQuery().elementId("externalCustomer1").singleResult();
        assertThat(workerJob).isNotNull();
        assertThat(workerJob.getCorrelationId()).isNotNull();

        ExternalWorkerJob job = managementService.createExternalWorkerJobQuery().correlationId(workerJob.getCorrelationId()).singleResult();
        assertThat(job).isNotNull();
        assertThat(job.getId()).isEqualTo(workerJob.getId());
        assertThat(job.getCorrelationId()).isEqualTo(workerJob.getCorrelationId());

        assertThat(managementService.createExternalWorkerJobQuery().correlationId("invalid").singleResult()).isNull();
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/api/mgmt/ExternalWorkerJobQueryTest.bpmn20.xml")
    public void testAcquireForUserOrGroups() {
        runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("externalWorkerJobQueryTest")
                .start();

        runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("externalWorkerJobQueryTest")
                .start();

        List<ExternalWorkerJob> jobs = managementService.createExternalWorkerJobQuery().list();
        assertThat(jobs).hasSize(4);

        ExternalWorkerJob onlyUserJob = jobs.get(0);
        ExternalWorkerJob onlyGroupJob = jobs.get(1);
        ExternalWorkerJob userAndGroupJob = jobs.get(2);

        addUserIdentityLinkToJob(onlyUserJob, "gonzo");
        addGroupIdentityLinkToJob(onlyGroupJob, "bears");
        addGroupIdentityLinkToJob(userAndGroupJob, "frogs");
        addUserIdentityLinkToJob(userAndGroupJob, "fozzie");

        jobs = managementService.createExternalWorkerJobQuery()
                .forUserOrGroups("kermit", Collections.singleton("muppets"))
                .list();

        assertThat(jobs).isEmpty();

        jobs = managementService.createExternalWorkerJobQuery()
                .forUserOrGroups("gonzo", Collections.singleton("muppets"))
                .list();

        assertThat(jobs)
                .extracting(ExternalWorkerJob::getId)
                .containsExactlyInAnyOrder(onlyUserJob.getId());

        jobs = managementService.createExternalWorkerJobQuery()
                .forUserOrGroups("fozzie", Collections.singleton("bears"))
                .list();

        assertThat(jobs)
                .extracting(ExternalWorkerJob::getId)
                .containsExactlyInAnyOrder(onlyGroupJob.getId(), userAndGroupJob.getId());

        jobs = managementService.createExternalWorkerJobQuery()
                .forUserOrGroups(null, Collections.singleton("bears"))
                .list();

        assertThat(jobs)
                .extracting(ExternalWorkerJob::getId)
                .containsExactlyInAnyOrder(onlyGroupJob.getId());

        jobs = managementService.createExternalWorkerJobQuery()
                .forUserOrGroups("fozzie", Collections.emptyList())
                .list();

        assertThat(jobs)
                .extracting(ExternalWorkerJob::getId)
                .containsExactlyInAnyOrder(userAndGroupJob.getId());
    }

    protected void addUserIdentityLinkToJob(Job job, String userId) {
        managementService.executeCommand(commandContext -> {
            processEngineConfiguration.getIdentityLinkServiceConfiguration().getIdentityLinkService()
                    .createScopeIdentityLink(null, job.getCorrelationId(), ScopeTypes.EXTERNAL_WORKER, userId, null, IdentityLinkType.PARTICIPANT);

            return null;
        });
    }

    protected void addGroupIdentityLinkToJob(Job job, String groupId) {
        managementService.executeCommand(commandContext -> {
            processEngineConfiguration.getIdentityLinkServiceConfiguration().getIdentityLinkService()
                    .createScopeIdentityLink(null, job.getCorrelationId(), ScopeTypes.EXTERNAL_WORKER, null, groupId, IdentityLinkType.PARTICIPANT);
            return null;
        });
    }


}
