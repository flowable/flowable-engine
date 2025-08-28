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

import java.util.List;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.flowable.common.engine.impl.identity.Authentication;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.rest.service.BaseSpringRestTestCase;
import org.flowable.rest.service.api.RestUrls;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class ProcessInstanceChangeActivityStateResourceTest extends BaseSpringRestTestCase {

    @Test
    @Deployment(resources = { "org/flowable/rest/service/api/runtime/parallelTask.bpmn20.xml" })
    public void testChangeActivityStateManyToOne() throws Exception {
        Authentication.setAuthenticatedUserId("testUser");
        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("startParallelProcess")
                .start();
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task.getTaskDefinitionKey()).isEqualTo("taskBefore");
        
        taskService.complete(task.getId());
        
        assertThat(taskService.createTaskQuery().processInstanceId(processInstance.getId()).count()).isEqualTo(2);
        
        assertThat(runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).count()).isEqualTo(3);
        
        Authentication.setAuthenticatedUserId(null);
        
        ObjectNode requestNode = objectMapper.createObjectNode();
        ArrayNode cancelActivityArray = requestNode.putArray("cancelActivityIds");
        cancelActivityArray.add("task1");
        cancelActivityArray.add("task2");
        
        ArrayNode startActivityArray = requestNode.putArray("startActivityIds");
        startActivityArray.add("taskBefore");

        HttpPost httpPost = new HttpPost(buildUrl(RestUrls.URL_PROCESS_INSTANCE_CHANGE_STATE, processInstance.getId()));
        httpPost.setEntity(new StringEntity(requestNode.toString()));
        CloseableHttpResponse response = executeRequest(httpPost, HttpStatus.SC_OK);
        closeResponse(response);
        
        assertThat(taskService.createTaskQuery().processInstanceId(processInstance.getId()).count()).isEqualTo(1);
        
        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task.getTaskDefinitionKey()).isEqualTo("taskBefore");
        
        assertThat(runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).count()).isEqualTo(2);
        
        taskService.complete(task.getId());
        
        assertThat(taskService.createTaskQuery().processInstanceId(processInstance.getId()).count()).isEqualTo(2);
        
        List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        for (Task parallelTask : tasks) {
            taskService.complete(parallelTask.getId());
        }
        
        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task.getTaskDefinitionKey()).isEqualTo("taskAfter");
        
        taskService.complete(task.getId());
        
        assertProcessEnded(processInstance.getId());
    }
    
    @Test
    @Deployment(resources = { "org/flowable/rest/service/api/runtime/parallelTaskWithMI.bpmn20.xml" })
    public void testChangeActivityStateManyToOneWithMI() throws Exception {
        Authentication.setAuthenticatedUserId("testUser");
        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("startParallelProcess")
                .start();
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task.getTaskDefinitionKey()).isEqualTo("taskBefore");
        
        taskService.complete(task.getId());
        
        assertThat(taskService.createTaskQuery().processInstanceId(processInstance.getId()).count()).isEqualTo(4);
        
        assertThat(runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).count()).isEqualTo(7);
        
        Authentication.setAuthenticatedUserId(null);
        
        ObjectNode requestNode = objectMapper.createObjectNode();
        ArrayNode cancelActivityArray = requestNode.putArray("cancelActivityIds");
        cancelActivityArray.add("task1");
        cancelActivityArray.add("task2");
        
        ArrayNode startActivityArray = requestNode.putArray("startActivityIds");
        startActivityArray.add("taskBefore");

        HttpPost httpPost = new HttpPost(buildUrl(RestUrls.URL_PROCESS_INSTANCE_CHANGE_STATE, processInstance.getId()));
        httpPost.setEntity(new StringEntity(requestNode.toString()));
        CloseableHttpResponse response = executeRequest(httpPost, HttpStatus.SC_OK);
        closeResponse(response);
        
        assertThat(taskService.createTaskQuery().processInstanceId(processInstance.getId()).count()).isEqualTo(1);
        
        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task.getTaskDefinitionKey()).isEqualTo("taskBefore");
        
        assertThat(runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).count()).isEqualTo(2);
        
        taskService.complete(task.getId());
        
        assertThat(taskService.createTaskQuery().processInstanceId(processInstance.getId()).count()).isEqualTo(4);
        
        List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        for (Task parallelTask : tasks) {
            taskService.complete(parallelTask.getId());
        }
        
        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task.getTaskDefinitionKey()).isEqualTo("taskAfter");
        
        taskService.complete(task.getId());
        
        assertProcessEnded(processInstance.getId());
    }
}
