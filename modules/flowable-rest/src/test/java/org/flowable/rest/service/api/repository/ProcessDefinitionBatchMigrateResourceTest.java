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
package org.flowable.rest.service.api.repository;

import static org.assertj.core.api.Assertions.assertThat;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.flowable.batch.api.Batch;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.job.api.Job;
import org.flowable.rest.service.BaseSpringRestTestCase;
import org.flowable.rest.service.api.RestUrls;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

public class ProcessDefinitionBatchMigrateResourceTest extends BaseSpringRestTestCase {

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
        for (Deployment deployment : repositoryService.createDeploymentQuery().list()) {
            repositoryService.deleteDeployment(deployment.getId(), true);
        }
    }

    @Test
    public void testBatchMigrateReturnsBatchResponse() throws Exception {
        Deployment deploymentV1 = repositoryService.createDeployment()
                .name("v1")
                .addClasspathResource("org/flowable/rest/service/api/runtime/one-task-start.bpmn20.xml")
                .deploy();
        ProcessDefinition v1 = repositoryService.createProcessDefinitionQuery()
                .deploymentId(deploymentV1.getId()).singleResult();

        runtimeService.createProcessInstanceBuilder().processDefinitionKey("one-task").start();
        runtimeService.createProcessInstanceBuilder().processDefinitionKey("one-task").start();

        Deployment deploymentV2 = repositoryService.createDeployment()
                .name("v2")
                .addClasspathResource("org/flowable/rest/service/api/runtime/one-task-rename-task.bpmn20.xml")
                .deploy();
        ProcessDefinition v2 = repositoryService.createProcessDefinitionQuery()
                .deploymentId(deploymentV2.getId()).singleResult();

        ObjectNode requestNode = objectMapper.createObjectNode();
        requestNode.put("toProcessDefinitionId", v2.getId());

        HttpPost httpPost = new HttpPost(SERVER_URL_PREFIX +
                RestUrls.createRelativeResourceUrl(RestUrls.URL_PROCESS_DEFINITION, v1.getId()) + "/batch-migrate");
        httpPost.setEntity(new StringEntity(requestNode.toString()));

        CloseableHttpResponse response = executeRequest(httpPost, HttpStatus.SC_OK);
        JsonNode responseNode = readContent(response);

        assertThat(responseNode.get("id").asString()).isNotEmpty();
        assertThat(responseNode.get("batchType").asString()).isEqualTo(Batch.PROCESS_MIGRATION_TYPE);
        assertThat(responseNode.get("searchKey").asString()).isEqualTo(v1.getId());
        assertThat(responseNode.get("searchKey2").asString()).isEqualTo(v2.getId());
        assertThat(responseNode.get("createTime").asString()).isNotEmpty();
        assertThat(responseNode.get("status").asString()).isNotEmpty();
        assertThat(responseNode.get("url").asString()).contains("management/batches/" + responseNode.get("id").asString());
    }
}
