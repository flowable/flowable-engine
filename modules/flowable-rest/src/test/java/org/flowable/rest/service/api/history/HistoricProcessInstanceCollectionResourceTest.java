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

package org.flowable.rest.service.api.history;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.flowable.engine.impl.cmd.ChangeDeploymentTenantIdCmd;
import org.flowable.engine.runtime.ActivityInstance;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.rest.service.BaseSpringRestTestCase;
import org.flowable.rest.service.api.RestUrls;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import net.javacrumbs.jsonunit.core.Option;

/**
 * Test for REST-operation related to the historic process instance query resource.
 * 
 * @author Tijs Rademakers
 */
public class HistoricProcessInstanceCollectionResourceTest extends BaseSpringRestTestCase {

    /**
     * Test querying historic process instance based on variables. GET history/historic-process-instances
     */
    @Test
    @Deployment
    public void testQueryProcessInstances() throws Exception {
        Calendar startTime = Calendar.getInstance();
        processEngineConfiguration.getClock().setCurrentTime(startTime.getTime());

        HashMap<String, Object> processVariables = new HashMap<>();
        processVariables.put("stringVar", "Azerty");
        processVariables.put("intVar", 67890);
        processVariables.put("booleanVar", false);

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess", "businessKey", processVariables);

        runtimeService.setProcessInstanceName(processInstance.getId(), "myProcessInstance");
        
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        taskService.complete(task.getId());

        startTime.add(Calendar.DAY_OF_YEAR, 1);
        processEngineConfiguration.getClock().setCurrentTime(startTime.getTime());
        ProcessInstance processInstance2 = runtimeService.startProcessInstanceByKey("oneTaskProcess", "businessKey2");
        
        runtimeService.setProcessInstanceName(processInstance2.getId(), "otherProcessInstance");

        String url = RestUrls.createRelativeResourceUrl(RestUrls.URL_HISTORIC_PROCESS_INSTANCES);

        assertResultsPresentInDataResponse(url + "?finished=true", processInstance.getId());

        assertResultsPresentInDataResponse(url + "?finished=false", processInstance2.getId());

        assertResultsPresentInDataResponse(url + "?processDefinitionId=" + processInstance.getProcessDefinitionId(), processInstance.getId(), processInstance2.getId());

        assertResultsPresentInDataResponse(url + "?processDefinitionId=" + processInstance.getProcessDefinitionId() + "&finished=true", processInstance.getId());

        assertResultsPresentInDataResponse(url + "?processDefinitionKey=oneTaskProcess", processInstance.getId(), processInstance2.getId());
        
        assertResultsPresentInDataResponse(url + "?processInstanceName=myProcessInstance", processInstance.getId());
        assertResultsPresentInDataResponse(url + "?processInstanceName=otherProcessInstance", processInstance2.getId());
        
        assertResultsPresentInDataResponse(url + "?processInstanceNameLike=" + encode("%ProcessInstance"), processInstance.getId(), processInstance2.getId());
        assertResultsPresentInDataResponse(url + "?processInstanceNameLike=" + encode("other%Instance"), processInstance2.getId());
        
        assertResultsPresentInDataResponse(url + "?processInstanceNameLikeIgnoreCase=" + encode("%proceSSinstance"), processInstance.getId(), processInstance2.getId());
        assertResultsPresentInDataResponse(url + "?processInstanceNameLikeIgnoreCase=" + encode("OTHER%Instance"), processInstance2.getId());
        
        assertResultsPresentInDataResponse(url + "?businessKey=businessKey", processInstance.getId());
        assertResultsPresentInDataResponse(url + "?businessKey=businessKey2", processInstance2.getId());
        
        assertResultsPresentInDataResponse(url + "?businessKeyLike=" + encode("business%"), processInstance.getId(), processInstance2.getId());

        
        // includeProcessVariables
        assertVariablesPresentInPostDataResponse(url, "?includeProcessVariables=false&processInstanceId=" + processInstance.getId(), processInstance.getId(), new HashMap<>());
        assertVariablesPresentInPostDataResponse(url, "?includeProcessVariables=true&processInstanceId=" + processInstance.getId(), processInstance.getId(), processVariables);

        assertVariablesPresentInPostDataResponse(url, "?includeProcessVariablesNames=stringVar,dummy&processInstanceId=" + processInstance.getId(),
                processInstance.getId(), Map.of("stringVar", "Azerty"));

        // Without tenant ID, before setting tenant
        assertResultsPresentInDataResponse(url + "?withoutTenantId=true", processInstance.getId(), processInstance2.getId());

        // Set tenant on deployment
        managementService.executeCommand(new ChangeDeploymentTenantIdCmd(deploymentId, "myTenant"));
        startTime.add(Calendar.DAY_OF_YEAR, 1);
        processEngineConfiguration.getClock().setCurrentTime(startTime.getTime());
        ProcessInstance processInstance3 = runtimeService.startProcessInstanceByKeyAndTenantId("oneTaskProcess", "myTenant");

        // Without tenant ID, after setting tenant
        assertResultsPresentInDataResponse(url + "?withoutTenantId=true", processInstance.getId(), processInstance2.getId());

        // Tenant id
        assertResultsPresentInDataResponse(url + "?tenantId=myTenant", processInstance3.getId());
        assertResultsPresentInDataResponse(url + "?tenantId=anotherTenant");

        // Tenant id like
        assertResultsPresentInDataResponse(url + "?tenantIdLike=" + encode("%enant"), processInstance3.getId());
        assertResultsPresentInDataResponse(url + "?tenantIdLike=anotherTenant");

        CloseableHttpResponse response = executeRequest(new HttpGet(SERVER_URL_PREFIX + url + "?processDefinitionKey=oneTaskProcess&sort=startTime"), 200);

        // Check status and size
        assertThat(response.getStatusLine().getStatusCode()).isEqualTo(HttpStatus.SC_OK);
        JsonNode dataNode = objectMapper.readTree(response.getEntity().getContent()).get("data");
        closeResponse(response);
        assertThatJson(dataNode)
                .when(Option.IGNORING_EXTRA_FIELDS, Option.IGNORING_ARRAY_ORDER)
                .isEqualTo("[ {"
                        + "     id:'" + processInstance.getId() + "'"
                        + "   },"
                        + "   {"
                        + "     id:'" + processInstance2.getId() + "'"
                        + "   },"
                        + "   {"
                        + "     id:'" + processInstance3.getId() + "'"
                        + "   }"
                        + "]");
    }
    
    @Test
    @Deployment(resources = { "org/flowable/rest/service/api/twoTaskProcess.bpmn20.xml" })
    public void testGetProcessInstancesByActiveActivityId() throws Exception {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
        
        // check that the right process is returned with no variables
        String url = RestUrls.createRelativeResourceUrl(RestUrls.URL_HISTORIC_PROCESS_INSTANCES) + "?activeActivityId=processTask";

        CloseableHttpResponse response = executeRequest(new HttpGet(SERVER_URL_PREFIX + url), HttpStatus.SC_OK);

        JsonNode rootNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        assertThatJson(rootNode)
                .when(Option.IGNORING_EXTRA_FIELDS)
                .isEqualTo("{"
                        + "data: [ {"
                        + "   id: '" + processInstance.getId() + "',"
                        + "   processDefinitionId: '" + processInstance.getProcessDefinitionId() + "'"
                        + "} ]"
                        + "}");

        url = RestUrls.createRelativeResourceUrl(RestUrls.URL_HISTORIC_PROCESS_INSTANCES) + "?activeActivityId=processTask2";

        response = executeRequest(new HttpGet(SERVER_URL_PREFIX + url), HttpStatus.SC_OK);

        rootNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        assertThatJson(rootNode)
                .when(Option.IGNORING_EXTRA_FIELDS)
                .isEqualTo("{"
                        + "data: []"
                        + "}");
        
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        taskService.complete(task.getId());
        
        url = RestUrls.createRelativeResourceUrl(RestUrls.URL_HISTORIC_PROCESS_INSTANCES) + "?activeActivityId=processTask2";

        response = executeRequest(new HttpGet(SERVER_URL_PREFIX + url), HttpStatus.SC_OK);

        rootNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        assertThatJson(rootNode)
        .when(Option.IGNORING_EXTRA_FIELDS)
        .isEqualTo("{"
                + "data: [ {"
                + "   id: '" + processInstance.getId() + "',"
                + "   processDefinitionId: '" + processInstance.getProcessDefinitionId() + "'"
                + "} ]"
                + "}");
    }

    @Test
    @Deployment(resources = { "org/flowable/rest/service/api/oneTaskProcess.bpmn20.xml" })
    public void testBulkDeleteHistoricProcessInstances() throws Exception {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
        ProcessInstance processInstance2 = runtimeService.startProcessInstanceByKey("oneTaskProcess");
        ProcessInstance processInstance3 = runtimeService.startProcessInstanceByKey("oneTaskProcess");

        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        taskService.complete(task.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance2.getId()).singleResult();
        taskService.complete(task.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance3.getId()).singleResult();
        taskService.complete(task.getId());

        assertThat(historyService.createHistoricProcessInstanceQuery().processInstanceId(processInstance.getId()).count()).isEqualTo(1);
        assertThat(historyService.createHistoricProcessInstanceQuery().processInstanceId(processInstance2.getId()).count()).isEqualTo(1);
        assertThat(historyService.createHistoricProcessInstanceQuery().processInstanceId(processInstance3.getId()).count()).isEqualTo(1);

        String url = RestUrls.createRelativeResourceUrl(RestUrls.URL_HISTORIC_PROCESS_INSTANCES) + "/delete";

        ObjectNode body = objectMapper.createObjectNode();
        body.put("action", "delete");
        body.putArray("instanceIds").add(processInstance.getId()).add(processInstance2.getId());
        HttpPost httpPost = new HttpPost(SERVER_URL_PREFIX + url);
        httpPost.setEntity(new StringEntity(body.toString()));
        closeResponse(executeRequest(httpPost, HttpStatus.SC_NO_CONTENT));

        assertThat(historyService.createHistoricProcessInstanceQuery().processInstanceId(processInstance.getId()).count()).isZero();
        assertThat(historyService.createHistoricProcessInstanceQuery().processInstanceId(processInstance2.getId()).count()).isZero();
        assertThat(historyService.createHistoricProcessInstanceQuery().processInstanceId(processInstance3.getId()).count()).isEqualTo(1);

    }

    @Test
    @Deployment(resources = { "org/flowable/rest/service/api/oneTaskProcess.bpmn20.xml" })
    public void testInvalidBulkDeleteHistoricProcessInstances() throws Exception {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
        ProcessInstance processInstance2 = runtimeService.startProcessInstanceByKey("oneTaskProcess");
        ProcessInstance processInstance3 = runtimeService.startProcessInstanceByKey("oneTaskProcess");

        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        taskService.complete(task.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance2.getId()).singleResult();
        taskService.complete(task.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance3.getId()).singleResult();
        taskService.complete(task.getId());

        String url = RestUrls.createRelativeResourceUrl(RestUrls.URL_HISTORIC_PROCESS_INSTANCES) + "/delete";

        ObjectNode body = objectMapper.createObjectNode();
        body.put("action", "delete");
        body.putArray("instanceIds").add(processInstance.getId()).add(processInstance2.getId()).add("notValidID");
        HttpPost httpPost = new HttpPost(SERVER_URL_PREFIX + url);
        httpPost.setEntity(new StringEntity(body.toString()));
        closeResponse(executeRequest(httpPost, HttpStatus.SC_NO_CONTENT));

        assertThat(historyService.createHistoricProcessInstanceQuery().processInstanceId(processInstance.getId()).count()).isEqualTo(0);
        assertThat(historyService.createHistoricProcessInstanceQuery().processInstanceId(processInstance2.getId()).count()).isEqualTo(0);
        assertThat(historyService.createHistoricProcessInstanceQuery().processInstanceId(processInstance3.getId()).count()).isEqualTo(1);

        body = objectMapper.createObjectNode();
        body.put("action", "delete");
        httpPost = new HttpPost(SERVER_URL_PREFIX + url);
        httpPost.setEntity(new StringEntity(body.toString()));
        closeResponse(executeRequest(httpPost, HttpStatus.SC_BAD_REQUEST));

        body.put("action", "invalidAction");
        httpPost = new HttpPost(SERVER_URL_PREFIX + url);
        httpPost.setEntity(new StringEntity(body.toString()));
        closeResponse(executeRequest(httpPost, HttpStatus.SC_BAD_REQUEST));
    }

    /**
     * Test getting a list of process instance by callback id
     */
    @Test
    @Deployment(resources = { "org/flowable/rest/service/api/oneTaskProcess.bpmn20.xml" })
    public void testGetProcessInstancesByCallbackId() throws Exception {
        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder().callbackId("callBackId1").processDefinitionKey("oneTaskProcess").start();
        runtimeService.createProcessInstanceBuilder().callbackId("callBackId2").processDefinitionKey("oneTaskProcess").start();

        taskService.createTaskQuery().list().forEach(task -> taskService.complete(task.getId()));

        assertThat(runtimeService.createProcessInstanceQuery().count()).isEqualTo(0);
        String id = processInstance.getId();
        // Process instance id

        String url = RestUrls.createRelativeResourceUrl(RestUrls.URL_HISTORIC_PROCESS_INSTANCES) + "?callbackId=callBackId1";
        assertResultsPresentInDataResponse(url, id);

        url = RestUrls.createRelativeResourceUrl(RestUrls.URL_HISTORIC_PROCESS_INSTANCES) + "?callbackIds=someOtherId,callBackId1";
        assertResultsPresentInDataResponse(url, id);

    }
    
    @Override
    protected void assertResultsPresentInDataResponse(String url, String... expectedResourceIds) throws JsonProcessingException, IOException {
        int numberOfResultsExpected = expectedResourceIds.length;

        // Do the actual call
        CloseableHttpResponse response = executeRequest(new HttpGet(SERVER_URL_PREFIX + url), 200);

        // Check status and size
        assertThat(response.getStatusLine().getStatusCode()).isEqualTo(HttpStatus.SC_OK);
        JsonNode dataNode = objectMapper.readTree(response.getEntity().getContent()).get("data");
        closeResponse(response);
        assertThat(dataNode).hasSize(numberOfResultsExpected);

        // Check presence of ID's
        List<String> toBeFound = new ArrayList<>(Arrays.asList(expectedResourceIds));
        Iterator<JsonNode> it = dataNode.iterator();
        while (it.hasNext()) {
            String id = it.next().get("id").textValue();
            toBeFound.remove(id);
        }
        assertThat(toBeFound).as("Not all process instances have been found in result, missing: " + StringUtils.join(toBeFound, ", ")).isEmpty();
    }

    protected void assertVariablesPresentInPostDataResponse(String url, String queryParameters, String processInstanceId, Map<String, Object> expectedVariables) throws IOException {

        HttpGet httpPost = new HttpGet(SERVER_URL_PREFIX + url + queryParameters);
        CloseableHttpResponse response = executeRequest(httpPost, HttpStatus.SC_OK);

        // Check status and size
        JsonNode dataNode = objectMapper.readTree(response.getEntity().getContent()).get("data");
        closeResponse(response);
        assertThatJson(dataNode)
                .when(Option.IGNORING_EXTRA_FIELDS)
                .isEqualTo("[{"
                        + "  id: '" + processInstanceId + "'"
                        + "}]");

        // Check expected variables
        JsonNode valueNode = dataNode.get(0);
        assertThat(valueNode.get("variables")).hasSize(expectedVariables.size());

        for (JsonNode node : valueNode.get("variables")) {
            ObjectNode variableNode = (ObjectNode) node;
            String variableName = variableNode.get("name").textValue();
            Object variableValue = objectMapper.convertValue(variableNode.get("value"), Object.class);

            assertThat(expectedVariables).containsKey(variableName);
            assertThat(variableValue).isEqualTo(expectedVariables.get(variableName));
            assertThat(variableNode.get("type").textValue()).isEqualTo(expectedVariables.get(variableName).getClass().getSimpleName().toLowerCase());
            assertThat(variableNode.get("scope").textValue()).isEqualTo("local");
        }

    }


    @Test
    @Deployment(resources = {
            "org/flowable/rest/service/api/oneTaskProcess.bpmn20.xml",
            "org/flowable/rest/service/api/runtime/simpleParallelCallActivity.bpmn20.xml",
            "org/flowable/rest/service/api/runtime/simpleInnerCallActivity.bpmn20.xml",
            "org/flowable/rest/service/api/runtime/simpleProcessWithUserTasks.bpmn20.xml"

    })
    public void testQueryByRootScopeId() throws IOException {
        runtimeService.startProcessInstanceByKey("simpleParallelCallActivity");
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("simpleParallelCallActivity");

        ActivityInstance firstLevelCallActivity1 = runtimeService.createActivityInstanceQuery()
                .processInstanceId(processInstance.getId())
                .activityId("callActivity1").singleResult();

        ActivityInstance secondLevelCallActivity1_1 = runtimeService.createActivityInstanceQuery()
                .processInstanceId(firstLevelCallActivity1.getCalledProcessInstanceId())
                .activityId("callActivity1").singleResult();

        ActivityInstance thirdLevelCallActivity1_1_1 = runtimeService.createActivityInstanceQuery()
                .processInstanceId(secondLevelCallActivity1_1.getCalledProcessInstanceId())
                .activityId("callActivity1").singleResult();

        ActivityInstance secondLevelCallActivity1_2 = runtimeService.createActivityInstanceQuery()
                .processInstanceId(firstLevelCallActivity1.getCalledProcessInstanceId())
                .activityId("callActivity2").singleResult();

        ActivityInstance firstLevelCallActivity2 = runtimeService.createActivityInstanceQuery().processInstanceId(processInstance.getId())
                .activityId("callActivity2").singleResult();

        taskService.createTaskQuery().list().forEach(task -> taskService.complete(task.getId()));

        String url = SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_HISTORIC_PROCESS_INSTANCES)
                + "?rootScopeId=" + processInstance.getId();
        CloseableHttpResponse response = executeRequest(new HttpGet(url), HttpStatus.SC_OK);

        JsonNode responseNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        assertThatJson(responseNode)
                .when(Option.IGNORING_EXTRA_FIELDS, Option.IGNORING_ARRAY_ORDER)
                .isEqualTo("{"
                        + "  data: ["
                        + "    { id: '" + firstLevelCallActivity1.getCalledProcessInstanceId() + "' },"
                        + "    { id: '" + secondLevelCallActivity1_1.getCalledProcessInstanceId() + "' },"
                        + "    { id: '" + thirdLevelCallActivity1_1_1.getCalledProcessInstanceId() + "' },"
                        + "    { id: '" + secondLevelCallActivity1_2.getCalledProcessInstanceId() + "' },"
                        + "    { id: '" + firstLevelCallActivity2.getCalledProcessInstanceId() + "' }"
                        + "  ]"
                        + "}");

    }

    @Test
    @Deployment(resources = {
            "org/flowable/rest/service/api/oneTaskProcess.bpmn20.xml",
            "org/flowable/rest/service/api/runtime/simpleParallelCallActivity.bpmn20.xml",
            "org/flowable/rest/service/api/runtime/simpleInnerCallActivity.bpmn20.xml",
            "org/flowable/rest/service/api/runtime/simpleProcessWithUserTasks.bpmn20.xml"
    })
    public void testQueryByParentScopeId() throws IOException {
        runtimeService.startProcessInstanceByKey("oneTaskProcess");
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("simpleParallelCallActivity");

        ActivityInstance firstLevelCallActivity1 = runtimeService.createActivityInstanceQuery().processInstanceId(processInstance.getId())
                .activityId("callActivity1").singleResult();

        ActivityInstance secondLevelCallActivity1 = runtimeService.createActivityInstanceQuery()
                .processInstanceId(firstLevelCallActivity1.getCalledProcessInstanceId())
                .activityId("callActivity1").singleResult();
        ActivityInstance secondLevelCallActivity2 = runtimeService.createActivityInstanceQuery()
                .processInstanceId(firstLevelCallActivity1.getCalledProcessInstanceId())
                .activityId("callActivity2").singleResult();

        taskService.createTaskQuery().list().forEach(task -> taskService.complete(task.getId()));

        String url = SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_HISTORIC_PROCESS_INSTANCES) + "?parentScopeId="
                + firstLevelCallActivity1.getCalledProcessInstanceId();
        CloseableHttpResponse response = executeRequest(new HttpGet(url), HttpStatus.SC_OK);

        JsonNode responseNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        assertThatJson(responseNode)
                .when(Option.IGNORING_EXTRA_FIELDS, Option.IGNORING_ARRAY_ORDER)
                .isEqualTo("{"
                        + "  data: ["
                        + "    { id: '" + secondLevelCallActivity1.getCalledProcessInstanceId() + "' },"
                        + "    { id: '" + secondLevelCallActivity2.getCalledProcessInstanceId() + "' }"
                        + "  ]"
                        + "}");
    }
}
