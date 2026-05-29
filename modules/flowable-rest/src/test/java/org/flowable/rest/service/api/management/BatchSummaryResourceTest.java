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
package org.flowable.rest.service.api.management;

import static org.assertj.core.api.Assertions.assertThat;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.flowable.batch.api.Batch;
import org.flowable.batch.api.BatchPart;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.rest.service.BaseSpringRestTestCase;
import org.flowable.rest.service.api.RestUrls;
import org.junit.jupiter.api.Test;

import tools.jackson.databind.JsonNode;

public class BatchSummaryResourceTest extends BaseSpringRestTestCase {

    @Test
    public void testGetBatchSummary() throws Exception {
        Batch batch = managementService.createBatchBuilder()
                .batchType(Batch.PROCESS_MIGRATION_TYPE)
                .searchKey("test")
                .searchKey2("anotherTest")
                .status("waiting")
                .tenantId("flowable")
                .create();

        try {
            BatchPart part1 = managementService.createBatchPartBuilder(batch)
                    .type(Batch.PROCESS_MIGRATION_TYPE)
                    .status("waiting")
                    .scopeId("scope1")
                    .scopeType("bpmn")
                    .create();
            BatchPart part2 = managementService.createBatchPartBuilder(batch)
                    .type(Batch.PROCESS_MIGRATION_TYPE)
                    .status("waiting")
                    .scopeId("scope2")
                    .scopeType("bpmn")
                    .create();
            BatchPart part3 = managementService.createBatchPartBuilder(batch)
                    .type(Batch.PROCESS_MIGRATION_TYPE)
                    .status("waiting")
                    .scopeId("scope3")
                    .scopeType("bpmn")
                    .create();
            managementService.createBatchPartBuilder(batch)
                    .type(Batch.PROCESS_MIGRATION_TYPE)
                    .status("waiting")
                    .scopeId("scope4")
                    .scopeType("bpmn")
                    .create();
            managementService.createBatchPartBuilder(batch)
                    .type(Batch.PROCESS_MIGRATION_TYPE)
                    .status("waiting")
                    .scopeId("scope5")
                    .scopeType("bpmn")
                    .create();

            managementService.executeCommand(commandContext -> {
                CommandContextUtil.getBatchService(commandContext).completeBatchPart(part1.getId(), "completed", "{}");
                CommandContextUtil.getBatchService(commandContext).completeBatchPart(part2.getId(), "failed", "{}");
                CommandContextUtil.getBatchService(commandContext).completeBatchPart(part3.getId(), "fail", "{}");
                return null;
            });

            String url = RestUrls.createRelativeResourceUrl(RestUrls.URL_BATCH_SUMMARY, batch.getId());
            CloseableHttpResponse response = executeRequest(new HttpGet(SERVER_URL_PREFIX + url), HttpStatus.SC_OK);
            JsonNode responseNode = readContent(response);

            assertThat(responseNode.get("id").asString()).isEqualTo(batch.getId());
            assertThat(responseNode.get("batchType").asString()).isEqualTo(Batch.PROCESS_MIGRATION_TYPE);
            assertThat(responseNode.get("status").asString()).isEqualTo("waiting");
            assertThat(responseNode.get("createTime").asString()).isNotEmpty();
            assertThat(responseNode.get("completeTime").isNull()).isTrue();
            assertThat(responseNode.get("tenantId").asString()).isEqualTo("flowable");
            assertThat(responseNode.get("totalBatchParts").asLong()).isEqualTo(5);
            assertThat(responseNode.get("completedBatchParts").asLong()).isEqualTo(3);
            assertThat(responseNode.get("successBatchParts").asLong()).isEqualTo(1);
            assertThat(responseNode.get("failedBatchParts").asLong()).isEqualTo(2);
            assertThat(responseNode.get("url").asString()).endsWith(RestUrls.createRelativeResourceUrl(RestUrls.URL_BATCH, batch.getId()));

        } finally {
            managementService.deleteBatch(batch.getId());
        }
    }

    @Test
    public void testGetBatchSummaryWithNoParts() throws Exception {
        Batch batch = managementService.createBatchBuilder()
                .batchType("test")
                .status("waiting")
                .create();

        try {
            String url = RestUrls.createRelativeResourceUrl(RestUrls.URL_BATCH_SUMMARY, batch.getId());
            CloseableHttpResponse response = executeRequest(new HttpGet(SERVER_URL_PREFIX + url), HttpStatus.SC_OK);
            JsonNode responseNode = readContent(response);

            assertThat(responseNode.get("id").asString()).isEqualTo(batch.getId());
            assertThat(responseNode.get("totalBatchParts").asLong()).isZero();
            assertThat(responseNode.get("completedBatchParts").asLong()).isZero();
            assertThat(responseNode.get("successBatchParts").asLong()).isZero();
            assertThat(responseNode.get("failedBatchParts").asLong()).isZero();

        } finally {
            managementService.deleteBatch(batch.getId());
        }
    }

    @Test
    public void testGetBatchSummaryWithCompletedBatch() throws Exception {
        Batch batch = managementService.createBatchBuilder()
                .batchType("test")
                .status("waiting")
                .create();

        try {
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
                CommandContextUtil.getBatchService(commandContext).completeBatchPart(part1.getId(), "completed", "{}");
                CommandContextUtil.getBatchService(commandContext).completeBatchPart(part2.getId(), "completed", "{}");
                CommandContextUtil.getBatchService(commandContext).completeBatch(batch.getId(), "completed");
                return null;
            });

            String url = RestUrls.createRelativeResourceUrl(RestUrls.URL_BATCH_SUMMARY, batch.getId());
            CloseableHttpResponse response = executeRequest(new HttpGet(SERVER_URL_PREFIX + url), HttpStatus.SC_OK);
            JsonNode responseNode = readContent(response);

            assertThat(responseNode.get("status").asString()).isEqualTo("completed");
            assertThat(responseNode.get("completeTime").isNull()).isFalse();
            assertThat(responseNode.get("totalBatchParts").asLong()).isEqualTo(2);
            assertThat(responseNode.get("completedBatchParts").asLong()).isEqualTo(2);
            assertThat(responseNode.get("successBatchParts").asLong()).isEqualTo(2);
            assertThat(responseNode.get("failedBatchParts").asLong()).isZero();

        } finally {
            managementService.deleteBatch(batch.getId());
        }
    }

    @Test
    public void testGetBatchSummaryNotFound() throws Exception {
        executeRequest(new HttpGet(SERVER_URL_PREFIX +
                RestUrls.createRelativeResourceUrl(RestUrls.URL_BATCH_SUMMARY, "nonexistent")), HttpStatus.SC_NOT_FOUND);
    }
}
