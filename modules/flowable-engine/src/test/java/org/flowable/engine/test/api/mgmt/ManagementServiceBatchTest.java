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
import static org.assertj.core.api.Assertions.tuple;

import java.util.List;

import org.flowable.batch.api.Batch;
import org.flowable.batch.api.BatchPart;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * @author Filip Hrisafov
 */
class ManagementServiceBatchTest extends PluggableFlowableTestCase {

    @AfterEach
    void tearDown() {
        List<Batch> batches = managementService.getAllBatches();
        for (Batch batch : batches) {
            managementService.deleteBatch(batch.getId());
        }
    }

    @Test
    void createBatchPart() {
        Batch batch = managementService.createBatchBuilder()
                .batchType("test")
                .searchKey("search 1")
                .searchKey2("search 2")
                .status("start")
                .tenantId("flowable")
                .create();

        assertThat(batch.getBatchType()).isEqualTo("test");
        assertThat(batch.getBatchSearchKey()).isEqualTo("search 1");
        assertThat(batch.getBatchSearchKey2()).isEqualTo("search 2");
        assertThat(batch.getStatus()).isEqualTo("start");
        assertThat(batch.getTenantId()).isEqualTo("flowable");

        BatchPart part = managementService.createBatchPartBuilder(batch)
                .type("testPart")
                .searchKey("part search 1")
                .searchKey2("part search 2")
                .status("startPart")
                .scopeId("scope1")
                .subScopeId("subScope1")
                .scopeType("test")
                .create();

        assertThat(part).isNotNull();
        assertThat(part.getType()).isEqualTo("testPart");
        assertThat(part.getBatchType()).isEqualTo("test");
        assertThat(part.getBatchId()).isEqualTo(batch.getId());
        assertThat(part.getCreateTime()).isNotNull();
        assertThat(part.getCompleteTime()).isNull();
        assertThat(part.isCompleted()).isFalse();
        assertThat(part.getSearchKey()).isEqualTo("part search 1");
        assertThat(part.getSearchKey2()).isEqualTo("part search 2");
        assertThat(part.getBatchSearchKey()).isEqualTo("search 1");
        assertThat(part.getBatchSearchKey2()).isEqualTo("search 2");
        assertThat(part.getStatus()).isEqualTo("startPart");
        assertThat(part.getScopeId()).isEqualTo("scope1");
        assertThat(part.getSubScopeId()).isEqualTo("subScope1");
        assertThat(part.getScopeType()).isEqualTo("test");
        assertThat(part.getTenantId()).isEqualTo("flowable");

        part = managementService.createBatchPartQuery().id(part.getId()).singleResult();
        assertThat(part).isNotNull();
        assertThat(part.getType()).isEqualTo("testPart");
        assertThat(part.getBatchType()).isEqualTo("test");
        assertThat(part.getBatchId()).isEqualTo(batch.getId());
        assertThat(part.getCreateTime()).isNotNull();
        assertThat(part.getCompleteTime()).isNull();
        assertThat(part.isCompleted()).isFalse();
        assertThat(part.getSearchKey()).isEqualTo("part search 1");
        assertThat(part.getSearchKey2()).isEqualTo("part search 2");
        assertThat(part.getBatchSearchKey()).isEqualTo("search 1");
        assertThat(part.getBatchSearchKey2()).isEqualTo("search 2");
        assertThat(part.getStatus()).isEqualTo("startPart");
        assertThat(part.getScopeId()).isEqualTo("scope1");
        assertThat(part.getSubScopeId()).isEqualTo("subScope1");
        assertThat(part.getScopeType()).isEqualTo("test");
        assertThat(part.getTenantId()).isEqualTo("flowable");
    }

    @Test
    void queryBatchParts() {
        Batch flowableBatch = managementService.createBatchBuilder()
                .batchType("testFlowable")
                .searchKey("search 1")
                .searchKey2("flowable search 1")
                .status("start")
                .tenantId("flowable")
                .create();
        Batch acmeBatch = managementService.createBatchBuilder()
                .batchType("testAcme")
                .searchKey("search 1")
                .searchKey2("acme search 1")
                .status("start")
                .tenantId("acme")
                .create();

        managementService.createBatchPartBuilder(flowableBatch)
                .type("partStart")
                .status("start")
                .searchKey("part 1")
                .searchKey2("flowable part 1")
                .scopeId("flowable1")
                .subScopeId("subFlowable1")
                .scopeType("test")
                .create();
        managementService.createBatchPartBuilder(flowableBatch)
                .type("partWait")
                .status("wait")
                .searchKey("part 2")
                .searchKey2("flowable part 2")
                .scopeId("flowable2")
                .subScopeId("subFlowable2")
                .scopeType("test")
                .create();
        managementService.createBatchPartBuilder(flowableBatch)
                .type("partRun")
                .status("run")
                .searchKey("part 3")
                .searchKey2("flowable part 3")
                .scopeId("flowable3")
                .subScopeId("subFlowable3")
                .scopeType(ScopeTypes.BPMN)
                .create();

        managementService.createBatchPartBuilder(acmeBatch)
                .type("partStart")
                .status("start")
                .searchKey("part 1")
                .searchKey2("acme part 1")
                .scopeId("acme1")
                .subScopeId("subAcme1")
                .scopeType("test")
                .create();
        managementService.createBatchPartBuilder(acmeBatch)
                .type("partWait")
                .status("wait")
                .searchKey("part 2")
                .searchKey2("acme part 2")
                .scopeId("acme2")
                .subScopeId("subAcme2")
                .scopeType("test")
                .create();
        managementService.createBatchPartBuilder(acmeBatch)
                .type("partRun")
                .status("run")
                .searchKey("part 3")
                .searchKey2("acme part 3")
                .scopeId("acme3")
                .subScopeId("subAcme3")
                .scopeType(ScopeTypes.BPMN)
                .create();

        assertThat(managementService.createBatchPartQuery().list())
                .extracting(BatchPart::getScopeId, BatchPart::getSubScopeId)
                .containsExactlyInAnyOrder(
                        tuple("flowable1", "subFlowable1"),
                        tuple("flowable2", "subFlowable2"),
                        tuple("flowable3", "subFlowable3"),
                        tuple("acme1", "subAcme1"),
                        tuple("acme2", "subAcme2"),
                        tuple("acme3", "subAcme3")
                );
        assertThat(managementService.createBatchPartQuery().count()).isEqualTo(6);

        assertThat(managementService.createBatchPartQuery().batchId(acmeBatch.getId()).list())
                .extracting(BatchPart::getScopeId, BatchPart::getSubScopeId)
                .containsExactlyInAnyOrder(
                        tuple("acme1", "subAcme1"),
                        tuple("acme2", "subAcme2"),
                        tuple("acme3", "subAcme3")
                );
        assertThat(managementService.createBatchPartQuery().batchId(acmeBatch.getId()).count()).isEqualTo(3);

        assertThat(managementService.createBatchPartQuery().type("partStart").list())
                .extracting(BatchPart::getScopeId, BatchPart::getSubScopeId)
                .containsExactlyInAnyOrder(
                        tuple("flowable1", "subFlowable1"),
                        tuple("acme1", "subAcme1")
                );
        assertThat(managementService.createBatchPartQuery().type("partStart").count()).isEqualTo(2);

        assertThat(managementService.createBatchPartQuery().type("partRun").list())
                .extracting(BatchPart::getScopeId, BatchPart::getSubScopeId)
                .containsExactlyInAnyOrder(
                        tuple("flowable3", "subFlowable3"),
                        tuple("acme3", "subAcme3")
                );
        assertThat(managementService.createBatchPartQuery().type("partRun").count()).isEqualTo(2);

        assertThat(managementService.createBatchPartQuery().type("unknown").list()).isEmpty();
        assertThat(managementService.createBatchPartQuery().type("unknown").count()).isZero();

        assertThat(managementService.createBatchPartQuery().searchKey("part 1").list())
                .extracting(BatchPart::getScopeId, BatchPart::getSubScopeId)
                .containsExactlyInAnyOrder(
                        tuple("flowable1", "subFlowable1"),
                        tuple("acme1", "subAcme1")
                );
        assertThat(managementService.createBatchPartQuery().searchKey("part 1").count()).isEqualTo(2);

        assertThat(managementService.createBatchPartQuery().searchKey2("part 1").list()).isEmpty();
        assertThat(managementService.createBatchPartQuery().searchKey2("part 1").count()).isZero();

        assertThat(managementService.createBatchPartQuery().searchKey2("flowable part 1").list())
                .extracting(BatchPart::getScopeId, BatchPart::getSubScopeId)
                .containsExactlyInAnyOrder(
                        tuple("flowable1", "subFlowable1")
                );
        assertThat(managementService.createBatchPartQuery().searchKey2("flowable part 1").count()).isEqualTo(1);

        assertThat(managementService.createBatchPartQuery().batchType("testFlowable").list())
                .extracting(BatchPart::getScopeId, BatchPart::getSubScopeId)
                .containsExactlyInAnyOrder(
                        tuple("flowable1", "subFlowable1"),
                        tuple("flowable2", "subFlowable2"),
                        tuple("flowable3", "subFlowable3")
                );
        assertThat(managementService.createBatchPartQuery().batchType("testFlowable").count()).isEqualTo(3);

        assertThat(managementService.createBatchPartQuery().batchType("unknown").list()).isEmpty();
        assertThat(managementService.createBatchPartQuery().batchType("unknown").count()).isZero();

        assertThat(managementService.createBatchPartQuery().batchSearchKey("search 1").list())
                .extracting(BatchPart::getScopeId, BatchPart::getSubScopeId)
                .containsExactlyInAnyOrder(
                        tuple("flowable1", "subFlowable1"),
                        tuple("flowable2", "subFlowable2"),
                        tuple("flowable3", "subFlowable3"),
                        tuple("acme1", "subAcme1"),
                        tuple("acme2", "subAcme2"),
                        tuple("acme3", "subAcme3")
                );
        assertThat(managementService.createBatchPartQuery().batchSearchKey("search 1").count()).isEqualTo(6);

        assertThat(managementService.createBatchPartQuery().batchSearchKey2("search 1").list()).isEmpty();
        assertThat(managementService.createBatchPartQuery().batchSearchKey2("search 1").count()).isZero();

        assertThat(managementService.createBatchPartQuery().batchSearchKey2("flowable search 1").list())
                .extracting(BatchPart::getScopeId, BatchPart::getSubScopeId)
                .containsExactlyInAnyOrder(
                        tuple("flowable1", "subFlowable1"),
                        tuple("flowable2", "subFlowable2"),
                        tuple("flowable3", "subFlowable3")
                );
        assertThat(managementService.createBatchPartQuery().batchSearchKey2("flowable search 1").count()).isEqualTo(3);

        assertThat(managementService.createBatchPartQuery().status("wait").list())
                .extracting(BatchPart::getScopeId, BatchPart::getSubScopeId)
                .containsExactlyInAnyOrder(
                        tuple("flowable2", "subFlowable2"),
                        tuple("acme2", "subAcme2")
                );
        assertThat(managementService.createBatchPartQuery().status("wait").count()).isEqualTo(2);

        assertThat(managementService.createBatchPartQuery().scopeId("flowable2").list())
                .extracting(BatchPart::getScopeId, BatchPart::getSubScopeId)
                .containsExactlyInAnyOrder(
                        tuple("flowable2", "subFlowable2")
                );
        assertThat(managementService.createBatchPartQuery().scopeId("flowable2").count()).isEqualTo(1);

        assertThat(managementService.createBatchPartQuery().scopeId("unknown").list()).isEmpty();
        assertThat(managementService.createBatchPartQuery().scopeId("unknown").count()).isZero();

        assertThat(managementService.createBatchPartQuery().subScopeId("subFlowable3").list())
                .extracting(BatchPart::getScopeId, BatchPart::getSubScopeId)
                .containsExactlyInAnyOrder(
                        tuple("flowable3", "subFlowable3")
                );
        assertThat(managementService.createBatchPartQuery().subScopeId("subFlowable3").count()).isEqualTo(1);

        assertThat(managementService.createBatchPartQuery().subScopeId("unknown").list()).isEmpty();
        assertThat(managementService.createBatchPartQuery().subScopeId("unknown").count()).isZero();

        assertThat(managementService.createBatchPartQuery().scopeType("test").list())
                .extracting(BatchPart::getScopeId, BatchPart::getSubScopeId)
                .containsExactlyInAnyOrder(
                        tuple("flowable1", "subFlowable1"),
                        tuple("flowable2", "subFlowable2"),
                        tuple("acme1", "subAcme1"),
                        tuple("acme2", "subAcme2")
                );
        assertThat(managementService.createBatchPartQuery().scopeType("test").count()).isEqualTo(4);

        assertThat(managementService.createBatchPartQuery().scopeType("unknown").list()).isEmpty();
        assertThat(managementService.createBatchPartQuery().scopeType("unknown").count()).isZero();

        assertThat(managementService.createBatchPartQuery().tenantId("flowable").list())
                .extracting(BatchPart::getScopeId, BatchPart::getSubScopeId)
                .containsExactlyInAnyOrder(
                        tuple("flowable1", "subFlowable1"),
                        tuple("flowable2", "subFlowable2"),
                        tuple("flowable3", "subFlowable3")
                );
        assertThat(managementService.createBatchPartQuery().tenantId("flowable").count()).isEqualTo(3);

        assertThat(managementService.createBatchPartQuery().tenantId("unknown").list()).isEmpty();
        assertThat(managementService.createBatchPartQuery().tenantId("unknown").count()).isZero();

        assertThat(managementService.createBatchPartQuery().tenantIdLike("%a%").list())
                .extracting(BatchPart::getScopeId, BatchPart::getSubScopeId)
                .containsExactlyInAnyOrder(
                        tuple("flowable1", "subFlowable1"),
                        tuple("flowable2", "subFlowable2"),
                        tuple("flowable3", "subFlowable3"),
                        tuple("acme1", "subAcme1"),
                        tuple("acme2", "subAcme2"),
                        tuple("acme3", "subAcme3")
                );
        assertThat(managementService.createBatchPartQuery().tenantIdLike("%a%").count()).isEqualTo(6);

        assertThat(managementService.createBatchPartQuery().tenantIdLike("%acm%").list())
                .extracting(BatchPart::getScopeId, BatchPart::getSubScopeId)
                .containsExactlyInAnyOrder(
                        tuple("acme1", "subAcme1"),
                        tuple("acme2", "subAcme2"),
                        tuple("acme3", "subAcme3")
                );
        assertThat(managementService.createBatchPartQuery().tenantIdLike("%acm%").count()).isEqualTo(3);
    }
}
