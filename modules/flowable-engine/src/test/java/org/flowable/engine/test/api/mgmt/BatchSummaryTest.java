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

import java.util.List;

import org.flowable.batch.api.Batch;
import org.flowable.batch.api.BatchPart;
import org.flowable.batch.api.BatchSummary;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class BatchSummaryTest extends PluggableFlowableTestCase {

    @AfterEach
    void tearDown() {
        List<Batch> batches = managementService.getAllBatches();
        for (Batch batch : batches) {
            managementService.deleteBatch(batch.getId());
        }
    }

    @Test
    void summaryWithNoBatchParts() {
        Batch batch = managementService.createBatchBuilder()
                .batchType("test")
                .searchKey("key1")
                .status("waiting")
                .tenantId("flowable")
                .create();

        BatchSummary summary = managementService.executeCommand(commandContext ->
                CommandContextUtil.getBatchService(commandContext).calculateBatchSummary(batch.getId()));

        assertThat(summary.getBatchId()).isEqualTo(batch.getId());
        assertThat(summary.getBatchType()).isEqualTo("test");
        assertThat(summary.getStatus()).isEqualTo("waiting");
        assertThat(summary.getCreateTime()).isNotNull();
        assertThat(summary.getCompleteTime()).isNull();
        assertThat(summary.getTenantId()).isEqualTo("flowable");
        assertThat(summary.getTotalBatchParts()).isZero();
        assertThat(summary.getCompletedBatchParts()).isZero();
        assertThat(summary.getSuccessBatchParts()).isZero();
        assertThat(summary.getFailedBatchParts()).isZero();
    }

    @Test
    void summaryWithAllPartsWaiting() {
        Batch batch = managementService.createBatchBuilder()
                .batchType("test")
                .status("waiting")
                .create();

        for (int i = 0; i < 5; i++) {
            managementService.createBatchPartBuilder(batch)
                    .type("testPart")
                    .status("waiting")
                    .scopeId("scope" + i)
                    .scopeType("test")
                    .create();
        }

        BatchSummary summary = managementService.executeCommand(commandContext ->
                CommandContextUtil.getBatchService(commandContext).calculateBatchSummary(batch.getId()));

        assertThat(summary.getTotalBatchParts()).isEqualTo(5);
        assertThat(summary.getCompletedBatchParts()).isZero();
        assertThat(summary.getSuccessBatchParts()).isZero();
        assertThat(summary.getFailedBatchParts()).isZero();
    }

    @Test
    void summaryWithSomePartsCompleted() {
        Batch batch = managementService.createBatchBuilder()
                .batchType("processMigration")
                .status("waiting")
                .create();

        BatchPart part1 = managementService.createBatchPartBuilder(batch)
                .type("testPart")
                .status("waiting")
                .scopeId("scope1")
                .scopeType("test")
                .create();
        BatchPart part2 = managementService.createBatchPartBuilder(batch)
                .type("testPart")
                .status("waiting")
                .scopeId("scope2")
                .scopeType("test")
                .create();
        managementService.createBatchPartBuilder(batch)
                .type("testPart")
                .status("waiting")
                .scopeId("scope3")
                .scopeType("test")
                .create();
        managementService.createBatchPartBuilder(batch)
                .type("testPart")
                .status("waiting")
                .scopeId("scope4")
                .scopeType("test")
                .create();

        managementService.executeCommand(commandContext -> {
            CommandContextUtil.getBatchService(commandContext).completeBatchPart(part1.getId(), "completed", "{}");
            CommandContextUtil.getBatchService(commandContext).completeBatchPart(part2.getId(), "completed", "{}");
            return null;
        });

        BatchSummary summary = managementService.executeCommand(commandContext ->
                CommandContextUtil.getBatchService(commandContext).calculateBatchSummary(batch.getId()));

        assertThat(summary.getTotalBatchParts()).isEqualTo(4);
        assertThat(summary.getCompletedBatchParts()).isEqualTo(2);
        assertThat(summary.getSuccessBatchParts()).isEqualTo(2);
        assertThat(summary.getFailedBatchParts()).isZero();
    }

    @Test
    void summaryWithCompletedAndFailedParts() {
        Batch batch = managementService.createBatchBuilder()
                .batchType("historicProcessDelete")
                .status("waiting")
                .create();

        BatchPart completed1 = managementService.createBatchPartBuilder(batch)
                .type("testPart")
                .status("waiting")
                .scopeId("scope1")
                .scopeType("test")
                .create();
        BatchPart completed2 = managementService.createBatchPartBuilder(batch)
                .type("testPart")
                .status("waiting")
                .scopeId("scope2")
                .scopeType("test")
                .create();
        BatchPart failed1 = managementService.createBatchPartBuilder(batch)
                .type("testPart")
                .status("waiting")
                .scopeId("scope3")
                .scopeType("test")
                .create();
        BatchPart failed2 = managementService.createBatchPartBuilder(batch)
                .type("testPart")
                .status("waiting")
                .scopeId("scope4")
                .scopeType("test")
                .create();
        managementService.createBatchPartBuilder(batch)
                .type("testPart")
                .status("waiting")
                .scopeId("scope5")
                .scopeType("test")
                .create();

        managementService.executeCommand(commandContext -> {
            CommandContextUtil.getBatchService(commandContext).completeBatchPart(completed1.getId(), "completed", "{}");
            CommandContextUtil.getBatchService(commandContext).completeBatchPart(completed2.getId(), "completed", "{}");
            CommandContextUtil.getBatchService(commandContext).completeBatchPart(failed1.getId(), "failed", "{}");
            CommandContextUtil.getBatchService(commandContext).completeBatchPart(failed2.getId(), "failed", "{}");
            return null;
        });

        BatchSummary summary = managementService.executeCommand(commandContext ->
                CommandContextUtil.getBatchService(commandContext).calculateBatchSummary(batch.getId()));

        assertThat(summary.getBatchType()).isEqualTo("historicProcessDelete");
        assertThat(summary.getTotalBatchParts()).isEqualTo(5);
        assertThat(summary.getCompletedBatchParts()).isEqualTo(4);
        assertThat(summary.getSuccessBatchParts()).isEqualTo(2);
        assertThat(summary.getFailedBatchParts()).isEqualTo(2);
    }

    @Test
    void summaryWithFailStatusFromMigration() {
        Batch batch = managementService.createBatchBuilder()
                .batchType("processMigration")
                .status("waiting")
                .create();

        BatchPart success = managementService.createBatchPartBuilder(batch)
                .type("testPart")
                .status("waiting")
                .scopeId("scope1")
                .scopeType("test")
                .create();
        BatchPart fail1 = managementService.createBatchPartBuilder(batch)
                .type("testPart")
                .status("waiting")
                .scopeId("scope2")
                .scopeType("test")
                .create();
        BatchPart fail2 = managementService.createBatchPartBuilder(batch)
                .type("testPart")
                .status("waiting")
                .scopeId("scope3")
                .scopeType("test")
                .create();

        managementService.executeCommand(commandContext -> {
            CommandContextUtil.getBatchService(commandContext).completeBatchPart(success.getId(), "success", "{}");
            CommandContextUtil.getBatchService(commandContext).completeBatchPart(fail1.getId(), "fail", "{}");
            CommandContextUtil.getBatchService(commandContext).completeBatchPart(fail2.getId(), "fail", "{}");
            return null;
        });

        BatchSummary summary = managementService.executeCommand(commandContext ->
                CommandContextUtil.getBatchService(commandContext).calculateBatchSummary(batch.getId()));

        assertThat(summary.getTotalBatchParts()).isEqualTo(3);
        assertThat(summary.getCompletedBatchParts()).isEqualTo(3);
        assertThat(summary.getSuccessBatchParts()).isEqualTo(1);
        assertThat(summary.getFailedBatchParts()).isEqualTo(2);
    }

    @Test
    void summaryWithAllPartsCompleted() {
        Batch batch = managementService.createBatchBuilder()
                .batchType("test")
                .status("waiting")
                .create();

        BatchPart part1 = managementService.createBatchPartBuilder(batch)
                .type("testPart")
                .status("waiting")
                .scopeId("scope1")
                .scopeType("test")
                .create();
        BatchPart part2 = managementService.createBatchPartBuilder(batch)
                .type("testPart")
                .status("waiting")
                .scopeId("scope2")
                .scopeType("test")
                .create();
        BatchPart part3 = managementService.createBatchPartBuilder(batch)
                .type("testPart")
                .status("waiting")
                .scopeId("scope3")
                .scopeType("test")
                .create();

        managementService.executeCommand(commandContext -> {
            CommandContextUtil.getBatchService(commandContext).completeBatchPart(part1.getId(), "completed", "{}");
            CommandContextUtil.getBatchService(commandContext).completeBatchPart(part2.getId(), "completed", "{}");
            CommandContextUtil.getBatchService(commandContext).completeBatchPart(part3.getId(), "completed", "{}");
            CommandContextUtil.getBatchService(commandContext).completeBatch(batch.getId(), "completed");
            return null;
        });

        BatchSummary summary = managementService.executeCommand(commandContext ->
                CommandContextUtil.getBatchService(commandContext).calculateBatchSummary(batch.getId()));

        assertThat(summary.getStatus()).isEqualTo("completed");
        assertThat(summary.getCompleteTime()).isNotNull();
        assertThat(summary.getTotalBatchParts()).isEqualTo(3);
        assertThat(summary.getCompletedBatchParts()).isEqualTo(3);
        assertThat(summary.getSuccessBatchParts()).isEqualTo(3);
        assertThat(summary.getFailedBatchParts()).isZero();
    }

    @Test
    void summaryWithAllPartsFailed() {
        Batch batch = managementService.createBatchBuilder()
                .batchType("test")
                .status("waiting")
                .create();

        BatchPart part1 = managementService.createBatchPartBuilder(batch)
                .type("testPart")
                .status("waiting")
                .scopeId("scope1")
                .scopeType("test")
                .create();
        BatchPart part2 = managementService.createBatchPartBuilder(batch)
                .type("testPart")
                .status("waiting")
                .scopeId("scope2")
                .scopeType("test")
                .create();

        managementService.executeCommand(commandContext -> {
            CommandContextUtil.getBatchService(commandContext).completeBatchPart(part1.getId(), "failed", "{}");
            CommandContextUtil.getBatchService(commandContext).completeBatchPart(part2.getId(), "failed", "{}");
            CommandContextUtil.getBatchService(commandContext).completeBatch(batch.getId(), "failed");
            return null;
        });

        BatchSummary summary = managementService.executeCommand(commandContext ->
                CommandContextUtil.getBatchService(commandContext).calculateBatchSummary(batch.getId()));

        assertThat(summary.getStatus()).isEqualTo("failed");
        assertThat(summary.getCompleteTime()).isNotNull();
        assertThat(summary.getTotalBatchParts()).isEqualTo(2);
        assertThat(summary.getCompletedBatchParts()).isEqualTo(2);
        assertThat(summary.getSuccessBatchParts()).isZero();
        assertThat(summary.getFailedBatchParts()).isEqualTo(2);
    }

    @Test
    void summaryWithMixedFailAndFailedStatuses() {
        Batch batch = managementService.createBatchBuilder()
                .batchType("test")
                .status("waiting")
                .create();

        BatchPart part1 = managementService.createBatchPartBuilder(batch)
                .type("testPart")
                .status("waiting")
                .scopeId("scope1")
                .scopeType("test")
                .create();
        BatchPart part2 = managementService.createBatchPartBuilder(batch)
                .type("testPart")
                .status("waiting")
                .scopeId("scope2")
                .scopeType("test")
                .create();
        BatchPart part3 = managementService.createBatchPartBuilder(batch)
                .type("testPart")
                .status("waiting")
                .scopeId("scope3")
                .scopeType("test")
                .create();
        BatchPart part4 = managementService.createBatchPartBuilder(batch)
                .type("testPart")
                .status("waiting")
                .scopeId("scope4")
                .scopeType("test")
                .create();

        managementService.executeCommand(commandContext -> {
            CommandContextUtil.getBatchService(commandContext).completeBatchPart(part1.getId(), "completed", "{}");
            CommandContextUtil.getBatchService(commandContext).completeBatchPart(part2.getId(), "failed", "{}");
            CommandContextUtil.getBatchService(commandContext).completeBatchPart(part3.getId(), "fail", "{}");
            CommandContextUtil.getBatchService(commandContext).completeBatchPart(part4.getId(), "success", "{}");
            return null;
        });

        BatchSummary summary = managementService.executeCommand(commandContext ->
                CommandContextUtil.getBatchService(commandContext).calculateBatchSummary(batch.getId()));

        assertThat(summary.getTotalBatchParts()).isEqualTo(4);
        assertThat(summary.getCompletedBatchParts()).isEqualTo(4);
        assertThat(summary.getSuccessBatchParts()).isEqualTo(2);
        assertThat(summary.getFailedBatchParts()).isEqualTo(2);
    }
}
