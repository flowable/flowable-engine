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
package org.flowable.cmmn.rest.service.api.repository;

import static org.assertj.core.api.Assertions.assertThat;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.flowable.batch.api.Batch;
import org.flowable.cmmn.api.repository.CaseDefinition;
import org.flowable.cmmn.api.repository.CmmnDeployment;
import org.flowable.cmmn.rest.service.BaseSpringRestTestCase;
import org.flowable.cmmn.rest.service.api.CmmnRestUrls;
import org.flowable.job.api.Job;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

public class CaseDefinitionBatchMigrateResourceTest extends BaseSpringRestTestCase {

    @AfterEach
    public void tearDown() {
        for (Job job : managementService.createJobQuery().list()) {
            managementService.deleteJob(job.getId());
        }
        for (Job job : managementService.createTimerJobQuery().list()) {
            managementService.deleteTimerJob(job.getId());
        }
        for (Batch batch : managementService.createBatchQuery().list()) {
            managementService.deleteBatch(batch.getId());
        }
        for (CmmnDeployment deployment : repositoryService.createDeploymentQuery().list()) {
            repositoryService.deleteDeployment(deployment.getId(), true);
        }
    }

    @Test
    public void testBatchMigrateReturnsBatchResponse() throws Exception {
        CaseDefinition v1 = deployCaseDefinition("v1", "org/flowable/cmmn/rest/service/api/runtime/task-and-stage-start.cmmn.xml");

        runtimeService.createCaseInstanceBuilder().caseDefinitionKey("testCase").start();
        runtimeService.createCaseInstanceBuilder().caseDefinitionKey("testCase").start();

        CaseDefinition v2 = deployCaseDefinition("v2", "org/flowable/cmmn/rest/service/api/runtime/task-and-stage-add-task-listener.cmmn.xml");

        ObjectNode requestNode = objectMapper.createObjectNode();
        requestNode.put("toCaseDefinitionId", v2.getId());

        HttpPost httpPost = new HttpPost(SERVER_URL_PREFIX +
                CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_CASE_DEFINITION, v1.getId()) + "/batch-migrate");
        httpPost.setEntity(new StringEntity(requestNode.toString()));

        CloseableHttpResponse response = executeRequest(httpPost, HttpStatus.SC_OK);
        JsonNode responseNode = objectMapper.readTree(response.getEntity().getContent());

        assertThat(responseNode.get("id").asString()).isNotEmpty();
        assertThat(responseNode.get("batchType").asString()).isEqualTo(Batch.CASE_MIGRATION_TYPE);
        assertThat(responseNode.get("searchKey").asString()).isEqualTo(v1.getId());
        assertThat(responseNode.get("searchKey2").asString()).isEqualTo(v2.getId());
        assertThat(responseNode.get("createTime").asString()).isNotEmpty();
        assertThat(responseNode.get("status").asString()).isNotEmpty();
        assertThat(responseNode.get("url").asString()).contains("cmmn-management/batches/" + responseNode.get("id").asString());
    }

    @Test
    public void testBatchMigrateHistoricReturnsBatchResponse() throws Exception {
        CaseDefinition v1 = deployCaseDefinition("v1", "org/flowable/cmmn/rest/service/api/runtime/task-and-stage-start.cmmn.xml");

        runtimeService.createCaseInstanceBuilder().caseDefinitionKey("testCase").start();

        CaseDefinition v2 = deployCaseDefinition("v2", "org/flowable/cmmn/rest/service/api/runtime/task-and-stage-add-task-listener.cmmn.xml");

        ObjectNode requestNode = objectMapper.createObjectNode();
        requestNode.put("toCaseDefinitionId", v2.getId());

        HttpPost httpPost = new HttpPost(SERVER_URL_PREFIX +
                CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_CASE_DEFINITION, v1.getId()) + "/batch-migrate-historic-instances");
        httpPost.setEntity(new StringEntity(requestNode.toString()));

        CloseableHttpResponse response = executeRequest(httpPost, HttpStatus.SC_OK);
        JsonNode responseNode = objectMapper.readTree(response.getEntity().getContent());

        assertThat(responseNode.get("id").asString()).isNotEmpty();
        assertThat(responseNode.get("batchType").asString()).isEqualTo(Batch.CASE_MIGRATION_TYPE);
        assertThat(responseNode.get("searchKey").asString()).isEqualTo(v1.getId());
        assertThat(responseNode.get("searchKey2").asString()).isEqualTo(v2.getId());
        assertThat(responseNode.get("createTime").asString()).isNotEmpty();
        assertThat(responseNode.get("status").asString()).isNotEmpty();
        assertThat(responseNode.get("url").asString()).contains("cmmn-management/batches/" + responseNode.get("id").asString());
    }

    protected CaseDefinition deployCaseDefinition(String name, String path) {
        CmmnDeployment deployment = repositoryService.createDeployment()
                .name(name)
                .addClasspathResource(path)
                .deploy();
        return repositoryService.createCaseDefinitionQuery()
                .deploymentId(deployment.getId()).singleResult();
    }
}
