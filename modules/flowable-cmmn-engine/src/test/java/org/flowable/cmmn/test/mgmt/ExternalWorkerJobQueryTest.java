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

package org.flowable.cmmn.test.mgmt;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.List;

import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.flowable.cmmn.engine.impl.job.ExternalWorkerTaskCompleteJobHandler;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.engine.test.FlowableCmmnTestCase;
import org.flowable.job.api.AcquiredExternalWorkerJob;
import org.flowable.job.api.ExternalWorkerJobQuery;
import org.flowable.job.api.ExternalWorkerJob;
import org.junit.Test;

/**
 * @author Filip Hrisafov
 */
public class ExternalWorkerJobQueryTest extends FlowableCmmnTestCase {

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/mgmt/ExternalWorkerJobQueryTest.cmmn")
    public void testQueryByNoCriteria() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("externalWorkerJobQueryTest")
                .start();

        ExternalWorkerJobQuery query = cmmnManagementService.createExternalWorkerJobQuery();
        assertThat(query.count()).isEqualTo(2);
        assertThat(query.list())
                .extracting(ExternalWorkerJob::getElementId)
                .containsExactlyInAnyOrder("externalOrder", "externalCustomer1");

        assertThat(query.list())
                .extracting(ExternalWorkerJob::getScopeId)
                .containsOnly(caseInstance.getId());
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/mgmt/ExternalWorkerJobQueryTest.cmmn")
    public void testQueryByCaseInstanceId() {
        CaseInstance caseInstance1 = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("externalWorkerJobQueryTest")
                .start();
        CaseInstance caseInstance2 = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("externalWorkerJobQueryTest")
                .start();

        ExternalWorkerJobQuery query = cmmnManagementService.createExternalWorkerJobQuery().caseInstanceId(caseInstance1.getId());
        assertThat(query.count()).isEqualTo(2);
        assertThat(query.list())
                .extracting(ExternalWorkerJob::getScopeId)
                .containsOnly(caseInstance1.getId());

        query = cmmnManagementService.createExternalWorkerJobQuery().caseInstanceId(caseInstance2.getId());
        assertThat(query.count()).isEqualTo(2);
        assertThat(query.list())
                .extracting(ExternalWorkerJob::getScopeId)
                .containsOnly(caseInstance2.getId());

        query = cmmnManagementService.createExternalWorkerJobQuery().caseInstanceId("invalid");
        assertThat(query.count()).isEqualTo(0);
        assertThat(query.list()).isEmpty();
        assertThat(query.singleResult()).isNull();
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/mgmt/ExternalWorkerJobQueryTest.cmmn")
    public void testQueryByCaseDefinitionId() {
        CaseInstance caseInstance1 = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("externalWorkerJobQueryTest")
                .start();
        CaseInstance caseInstance2 = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("externalWorkerJobQueryTest")
                .start();
        ExternalWorkerJobQuery query = cmmnManagementService.createExternalWorkerJobQuery().caseDefinitionId(caseInstance1.getCaseDefinitionId());
        assertThat(query.count()).isEqualTo(4);
        assertThat(query.list())
                .extracting(ExternalWorkerJob::getScopeId)
                .containsOnly(caseInstance1.getId(), caseInstance2.getId());

        query = cmmnManagementService.createExternalWorkerJobQuery().caseDefinitionId("invalid");
        assertThat(query.count()).isEqualTo(0);
        assertThat(query.list()).isEmpty();
        assertThat(query.singleResult()).isNull();
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/mgmt/ExternalWorkerJobQueryTest.cmmn")
    public void testQueryByPlanItemInstanceId() {
        cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("externalWorkerJobQueryTest")
                .start();

        PlanItemInstance orderPlanItem = cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionId("externalOrder").singleResult();
        assertThat(orderPlanItem).isNotNull();
        PlanItemInstance customerPlanItem = cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionId("externalCustomer1").singleResult();
        assertThat(customerPlanItem).isNotNull();

        ExternalWorkerJobQuery query = cmmnManagementService.createExternalWorkerJobQuery().planItemInstanceId(orderPlanItem.getId());
        assertThat(query.count()).isEqualTo(1);
        assertThat(query.list())
                .extracting(ExternalWorkerJob::getElementId)
                .containsExactlyInAnyOrder("externalOrder");
        assertThat(query.singleResult()).isNotNull();

        query = cmmnManagementService.createExternalWorkerJobQuery().planItemInstanceId(customerPlanItem.getId());
        assertThat(query.count()).isEqualTo(1);
        assertThat(query.list())
                .extracting(ExternalWorkerJob::getElementId)
                .containsExactlyInAnyOrder("externalCustomer1");
        assertThat(query.singleResult()).isNotNull();

        query = cmmnManagementService.createExternalWorkerJobQuery().planItemInstanceId("invalid");
        assertThat(query.count()).isEqualTo(0);
        assertThat(query.list()).isEmpty();
        assertThat(query.singleResult()).isNull();
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/mgmt/ExternalWorkerJobQueryTest.cmmn")
    public void testQueryByElementId() {
        CaseInstance caseInstance1 = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("externalWorkerJobQueryTest")
                .start();
        CaseInstance caseInstance2 = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("externalWorkerJobQueryTest")
                .start();

        ExternalWorkerJobQuery query = cmmnManagementService.createExternalWorkerJobQuery().elementId("externalOrder");
        assertThat(query.count()).isEqualTo(2);
        assertThat(query.list())
                .extracting(ExternalWorkerJob::getScopeId)
                .containsExactlyInAnyOrder(caseInstance1.getId(), caseInstance2.getId());

        query = cmmnManagementService.createExternalWorkerJobQuery().elementId("invalid");
        assertThat(query.count()).isEqualTo(0);
        assertThat(query.list()).isEmpty();

    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/mgmt/ExternalWorkerJobQueryTest.cmmn")
    public void testQueryByElementName() {
        CaseInstance caseInstance1 = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("externalWorkerJobQueryTest")
                .start();
        CaseInstance caseInstance2 = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("externalWorkerJobQueryTest")
                .start();

        ExternalWorkerJobQuery query = cmmnManagementService.createExternalWorkerJobQuery().elementName("Customer Service");
        assertThat(query.count()).isEqualTo(2);
        assertThat(query.list())
                .extracting(ExternalWorkerJob::getScopeId)
                .containsExactlyInAnyOrder(caseInstance1.getId(), caseInstance2.getId());

        query = cmmnManagementService.createExternalWorkerJobQuery().elementName("invalid");
        assertThat(query.count()).isEqualTo(0);
        assertThat(query.list()).isEmpty();
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/mgmt/ExternalWorkerJobQueryTest.cmmn")
    public void testQueryByHandlerType() {
        cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("externalWorkerJobQueryTest")
                .start();

        ExternalWorkerJobQuery query = cmmnManagementService.createExternalWorkerJobQuery().handlerType(ExternalWorkerTaskCompleteJobHandler.TYPE);
        assertThat(query.count()).isEqualTo(2);
        assertThat(query.list()).hasSize(2);

        query = cmmnManagementService.createExternalWorkerJobQuery().handlerType("invalid");
        assertThat(query.count()).isEqualTo(0);
        assertThat(query.list()).isEmpty();
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/mgmt/ExternalWorkerJobQueryTest.cmmn")
    public void testQueryByException() {
        cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("externalWorkerJobQueryTest")
                .start();

        List<AcquiredExternalWorkerJob> acquiredJobs = cmmnManagementService.createExternalWorkerJobAcquireBuilder()
                .topic("orderService", Duration.ofMinutes(10))
                .acquireAndLock(1, "testWorker");

        cmmnManagementService.createExternalWorkerJobFailureBuilder(acquiredJobs.get(0).getId(), "testWorker")
                .errorMessage("Error message")
                .errorDetails("Error details")
                .fail();

        ExternalWorkerJobQuery query = cmmnManagementService.createExternalWorkerJobQuery().withException();
        assertThat(query.count()).isEqualTo(1);
        assertThat(query.list())
                .extracting(ExternalWorkerJob::getElementId)
                .containsExactlyInAnyOrder("externalOrder");

        ExternalWorkerJob job = query.singleResult();

        assertThat(job).isNotNull();
        assertThat(job.getExceptionMessage()).isEqualTo("Error message");
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/mgmt/ExternalWorkerJobQueryTest.cmmn")
    public void testQueryByExceptionMessage() {
        cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("externalWorkerJobQueryTest")
                .start();

        List<AcquiredExternalWorkerJob> acquiredJobs = cmmnManagementService.createExternalWorkerJobAcquireBuilder()
                .topic("orderService", Duration.ofMinutes(10))
                .acquireAndLock(1, "testWorker");

        cmmnManagementService.createExternalWorkerJobFailureBuilder(acquiredJobs.get(0).getId(), "testWorker")
                .errorMessage("Error message")
                .errorDetails("Error details")
                .fail();

        ExternalWorkerJobQuery query = cmmnManagementService.createExternalWorkerJobQuery().exceptionMessage("Error message");
        assertThat(query.count()).isEqualTo(1);
        assertThat(query.list())
                .extracting(ExternalWorkerJob::getElementId)
                .containsExactlyInAnyOrder("externalOrder");

        ExternalWorkerJob job = query.singleResult();

        assertThat(job).isNotNull();
        assertThat(job.getExceptionMessage()).isEqualTo("Error message");

        query = cmmnManagementService.createExternalWorkerJobQuery().exceptionMessage("Error");
        assertThat(query.count()).isEqualTo(0);
        assertThat(query.list()).isEmpty();
        assertThat(query.singleResult()).isNull();
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/mgmt/ExternalWorkerJobQueryTest.cmmn")
    public void testQueryByLockedAndUnlocked() {
        cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("externalWorkerJobQueryTest")
                .start();

        cmmnManagementService.createExternalWorkerJobAcquireBuilder()
                .topic("orderService", Duration.ofMinutes(10))
                .acquireAndLock(1, "testWorker");

        ExternalWorkerJobQuery query = cmmnManagementService.createExternalWorkerJobQuery().locked();
        assertThat(query.count()).isEqualTo(1);
        assertThat(query.list())
                .extracting(ExternalWorkerJob::getElementId)
                .containsExactlyInAnyOrder("externalOrder");

        ExternalWorkerJob job = query.singleResult();

        assertThat(job).isNotNull();
        assertThat(job.getLockOwner()).isEqualTo("testWorker");
        assertThat(job.getLockExpirationTime()).isNotNull();

        query = cmmnManagementService.createExternalWorkerJobQuery().unlocked();
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
    @CmmnDeployment(resources = "org/flowable/cmmn/test/mgmt/ExternalWorkerJobQueryTest.cmmn")
    public void testQueryByLockOwner() {
        cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("externalWorkerJobQueryTest")
                .start();

        cmmnManagementService.createExternalWorkerJobAcquireBuilder()
                .topic("orderService", Duration.ofMinutes(10))
                .acquireAndLock(1, "testWorker1");

        cmmnManagementService.createExternalWorkerJobAcquireBuilder()
                .topic("customerService", Duration.ofMinutes(10))
                .acquireAndLock(1, "testWorker2");

        ExternalWorkerJobQuery query = cmmnManagementService.createExternalWorkerJobQuery().lockOwner("testWorker1");
        assertThat(query.count()).isEqualTo(1);
        assertThat(query.list())
                .extracting(ExternalWorkerJob::getElementId)
                .containsExactlyInAnyOrder("externalOrder");

        ExternalWorkerJob job = query.singleResult();

        assertThat(job).isNotNull();
        assertThat(job.getLockOwner()).isEqualTo("testWorker1");
        assertThat(job.getLockExpirationTime()).isNotNull();

        query = cmmnManagementService.createExternalWorkerJobQuery().lockOwner("testWorker2");
        assertThat(query.count()).isEqualTo(1);
        assertThat(query.list())
                .extracting(ExternalWorkerJob::getElementId)
                .containsExactlyInAnyOrder("externalCustomer1");

        job = query.singleResult();

        assertThat(job).isNotNull();
        assertThat(job.getLockOwner()).isEqualTo("testWorker2");
        assertThat(job.getLockExpirationTime()).isNotNull();

        query = cmmnManagementService.createExternalWorkerJobQuery().lockOwner("invalid");
        assertThat(query.count()).isEqualTo(0);
        assertThat(query.list()).isEmpty();
        assertThat(query.singleResult()).isNull();
    }

}
