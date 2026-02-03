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

package org.flowable.rest.service.api.runtime;

import static org.assertj.core.api.Assertions.assertThat;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.flowable.common.engine.impl.identity.Authentication;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.rest.service.BaseSpringRestTestCase;
import org.flowable.rest.service.api.RestUrls;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

public class ProcessInstanceMigrationResourceTest extends BaseSpringRestTestCase {
    
    @AfterEach
    public void tearDown() {
        for (org.flowable.engine.repository.Deployment deployment : repositoryService.createDeploymentQuery().list()) {
            repositoryService.deleteDeployment(deployment.getId(), true);
        }
    }

    @Test
    public void testDirectMigration() throws Exception {
        deployProcessDefinition("my deploy", "org/flowable/rest/service/api/runtime/one-task-start.bpmn20.xml");
        Authentication.setAuthenticatedUserId("testUser");
        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("one-task")
                .start();
        
        ProcessDefinition toProcessDefinition = deployProcessDefinition("my deploy", "org/flowable/rest/service/api/runtime/one-task-rename-task.bpmn20.xml");
        
        ObjectNode requestNode = objectMapper.createObjectNode();
        requestNode.put("toProcessDefinitionId", toProcessDefinition.getId());

        HttpPost httpPost = new HttpPost(buildUrl(RestUrls.URL_PROCESS_INSTANCE_VALIDATE_MIGRATION, processInstance.getId()));
        httpPost.setEntity(new StringEntity(requestNode.toString()));
        CloseableHttpResponse response = executeRequest(httpPost, HttpStatus.SC_OK);
        JsonNode validationNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        
        assertThat(validationNode.path("validationMessages")).hasSize(0);
        
        httpPost = new HttpPost(buildUrl(RestUrls.URL_PROCESS_INSTANCE_MIGRATE, processInstance.getId()));
        httpPost.setEntity(new StringEntity(requestNode.toString()));
        response = executeRequest(httpPost, HttpStatus.SC_OK);
        closeResponse(response);
        
        ProcessInstance migratedInstance = runtimeService.createProcessInstanceQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(migratedInstance.getProcessDefinitionId()).isEqualTo(toProcessDefinition.getId());
        
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task.getName()).isEqualTo("Task 1 updated");
        taskService.complete(task.getId());
        
        assertProcessEnded(processInstance.getId());
    }
    
    @Test
    public void testRenameTaskIdMigration() throws Exception {
        ProcessDefinition fromProcessDefinition = deployProcessDefinition("my deploy", "org/flowable/rest/service/api/runtime/one-task-start.bpmn20.xml");
        Authentication.setAuthenticatedUserId("testUser");
        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("one-task")
                .start();
        
        ProcessDefinition toProcessDefinition = deployProcessDefinition("my deploy", "org/flowable/rest/service/api/runtime/one-task-rename-task-id.bpmn20.xml");
        
        ObjectNode requestNode = objectMapper.createObjectNode();
        requestNode.put("toProcessDefinitionId", toProcessDefinition.getId());

        HttpPost httpPost = new HttpPost(buildUrl(RestUrls.URL_PROCESS_INSTANCE_VALIDATE_MIGRATION, processInstance.getId()));
        httpPost.setEntity(new StringEntity(requestNode.toString()));
        CloseableHttpResponse response = executeRequest(httpPost, HttpStatus.SC_OK);
        JsonNode validationNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        
        assertThat(validationNode.path("validationMessages")).hasSize(1);
        assertThat(validationNode.path("validationMessages").get(0).asText()).contains("has a running Activity (id:'userTask1Id') that is not mapped");
        
        httpPost = new HttpPost(buildUrl(RestUrls.URL_PROCESS_INSTANCE_MIGRATE, processInstance.getId()));
        httpPost.setEntity(new StringEntity(requestNode.toString()));
        response = executeRequest(httpPost, HttpStatus.SC_INTERNAL_SERVER_ERROR);
        closeResponse(response);
        
        ProcessInstance nonMigratedInstance = runtimeService.createProcessInstanceQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(nonMigratedInstance.getProcessDefinitionId()).isEqualTo(fromProcessDefinition.getId());
        
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task.getName()).isEqualTo("Task 1");
        taskService.complete(task.getId());
        
        assertProcessEnded(processInstance.getId());
    }
    
    protected ProcessDefinition deployProcessDefinition(String name, String path) {
        Deployment deployment = repositoryService.createDeployment()
            .name(name)
            .addClasspathResource(path)
            .deploy();
        
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery()
            .deploymentId(deployment.getId()).singleResult();

        return processDefinition;
    }
}
