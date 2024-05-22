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
import org.flowable.rest.service.BaseSpringRestTestCase;
import org.flowable.rest.service.api.RestUrls;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Test getting a BatchPart with the BatchPartCollectionResource.
 */
public class BatchPartCollectionResourceTest extends BaseSpringRestTestCase {

    @Test
    public void testGetBatchParts() throws Exception {
        ObjectNode docNode = objectMapper.createObjectNode();
        docNode.put("test", "value");

        Batch batch = managementService.createBatchBuilder()
                .batchType(Batch.PROCESS_MIGRATION_TYPE)
                .searchKey("test")
                .searchKey2("anotherTest")
                .batchDocumentJson(docNode.toString())
                .create();

        BatchPart batchPart = managementService.createBatchPartBuilder(batch)
                .type("deleteProcess")
                .status("waiting")
                .searchKey("testPart")
                .searchKey2("anotherTestPart")
                .type(Batch.PROCESS_MIGRATION_TYPE)
                .create();

        String url = RestUrls.createRelativeResourceUrl(RestUrls.URL_BATCH_PART_COLLECTION, batch.getId());
        CloseableHttpResponse response = executeRequest(new HttpGet(SERVER_URL_PREFIX + url), HttpStatus.SC_OK);
        JsonNode responseNode = objectMapper.readTree(response.getEntity().getContent());
        JsonNode batchPartNode = responseNode.get(0);
        assertThat(batchPartNode).isNotNull();
        assertThat(batchPartNode.get("id").asText()).isEqualTo(batchPart.getId());
        assertThat(batchPartNode.get("batchId").asText()).isEqualTo(batchPart.getBatchId());
        assertThat(batchPartNode.get("searchKey").asText()).isEqualTo(batchPart.getSearchKey());
        assertThat(batchPartNode.get("searchKey2").asText()).isEqualTo(batchPart.getSearchKey2());
        assertThat(batchPartNode.get("batchType").asText()).isEqualTo(batchPart.getBatchType());
        assertThat(batchPartNode.get("status").asText()).isEqualTo(batchPart.getStatus());

        managementService.deleteBatch(batch.getId());
    }
}
