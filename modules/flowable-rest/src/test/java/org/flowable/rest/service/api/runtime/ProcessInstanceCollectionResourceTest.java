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

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.impl.cmd.ChangeDeploymentTenantIdCmd;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.form.api.FormDefinition;
import org.flowable.form.api.FormDeployment;
import org.flowable.form.api.FormInstance;
import org.flowable.rest.service.BaseSpringRestTestCase;
import org.flowable.rest.service.api.RestUrls;
import org.flowable.task.api.Task;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import net.javacrumbs.jsonunit.core.Option;

/**
 * Test for all REST-operations related to a single Process instance resource.
 *
 * @author Frederik Heremans
 * @author Saeid Mirzaei
 * @author Filip Hrisafov
 */
public class ProcessInstanceCollectionResourceTest extends BaseSpringRestTestCase {

    // check if process instance query with business key with and without includeProcess Variables
    // related to https://activiti.atlassian.net/browse/ACT-1992
    @Test
    @Deployment(resources = { "org/flowable/rest/service/api/runtime/ProcessInstanceResourceTest.process-one.bpmn20.xml" })
    public void testGetProcessInstancesByBusinessKeyAndIncludeVariables() throws Exception {
        HashMap<String, Object> variables = new HashMap<>();
        variables.put("myVar1", "myVar1");
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("processOne", "myBusinessKey", variables);
        String processId = processInstance.getId();

        // check that the right process is returned with no variables
        String url = RestUrls.createRelativeResourceUrl(RestUrls.URL_PROCESS_INSTANCE_COLLECTION) + "?businessKey=myBusinessKey";

        CloseableHttpResponse response = executeRequest(new HttpGet(SERVER_URL_PREFIX + url), HttpStatus.SC_OK);

        JsonNode rootNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        assertThatJson(rootNode)
                .when(Option.IGNORING_EXTRA_FIELDS)
                .isEqualTo("{"
                        + "data: [ {"
                        + "   id: '" + processId + "',"
                        + "   processDefinitionId: '" + processInstance.getProcessDefinitionId() + "',"
                        + "   processDefinitionUrl: '" + SERVER_URL_PREFIX + RestUrls
                        .createRelativeResourceUrl(RestUrls.URL_PROCESS_DEFINITION, processInstance.getProcessDefinitionId()) + "',"
                        + "   variables: [ ]"
                        + "} ]"
                        + "}");

        // check that the right process is returned along with the variables
        // when includeProcessvariable is set
        url = RestUrls.createRelativeResourceUrl(RestUrls.URL_PROCESS_INSTANCE_COLLECTION) + "?businessKey=myBusinessKey&includeProcessVariables=true";

        response = executeRequest(new HttpGet(SERVER_URL_PREFIX + url), HttpStatus.SC_OK);

        rootNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        assertThatJson(rootNode)
                .when(Option.IGNORING_EXTRA_FIELDS)
                .isEqualTo("{"
                        + "data: [ {"
                        + "   id: '" + processId + "',"
                        + "   processDefinitionId: '" + processInstance.getProcessDefinitionId() + "',"
                        + "   processDefinitionUrl: '" + SERVER_URL_PREFIX + RestUrls
                        .createRelativeResourceUrl(RestUrls.URL_PROCESS_DEFINITION, processInstance.getProcessDefinitionId()) + "',"
                        + "   variables: [ {"
                        + "                name: 'myVar1',"
                        + "                value: 'myVar1'"
                        + "   } ]"
                        + "} ]"
                        + "}");
    }

    /**
     * Test getting a list of process instance, using all possible filters.
     */
    @Test
    @Deployment(resources = { "org/flowable/rest/service/api/runtime/ProcessInstanceResourceTest.process-one.bpmn20.xml" })
    public void testGetProcessInstances() throws Exception {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("processOne", "myBusinessKey");
        String id = processInstance.getId();
        runtimeService.addUserIdentityLink(id, "kermit", "whatever");

        // Test without any parameters
        String url = RestUrls.createRelativeResourceUrl(RestUrls.URL_PROCESS_INSTANCE_COLLECTION);
        assertResultsPresentInDataResponse(url, id);

        // Process instance id
        url = RestUrls.createRelativeResourceUrl(RestUrls.URL_PROCESS_INSTANCE_COLLECTION) + "?id=" + id;
        assertResultsPresentInDataResponse(url, id);

        url = RestUrls.createRelativeResourceUrl(RestUrls.URL_PROCESS_INSTANCE_COLLECTION) + "?id=anotherId";
        assertResultsPresentInDataResponse(url);

        // Process instance business key
        url = RestUrls.createRelativeResourceUrl(RestUrls.URL_PROCESS_INSTANCE_COLLECTION) + "?businessKey=myBusinessKey";
        assertResultsPresentInDataResponse(url, id);

        url = RestUrls.createRelativeResourceUrl(RestUrls.URL_PROCESS_INSTANCE_COLLECTION) + "?businessKey=anotherBusinessKey";
        assertResultsPresentInDataResponse(url);

        url = RestUrls.createRelativeResourceUrl(RestUrls.URL_PROCESS_INSTANCE_COLLECTION) + "?businessKeyLike=" + encode("%BusinessKey");
        assertResultsPresentInDataResponse(url, id);

        // Process definition key
        url = RestUrls.createRelativeResourceUrl(RestUrls.URL_PROCESS_INSTANCE_COLLECTION) + "?processDefinitionKey=processOne";
        assertResultsPresentInDataResponse(url, id);

        url = RestUrls.createRelativeResourceUrl(RestUrls.URL_PROCESS_INSTANCE_COLLECTION) + "?processDefinitionKey=processTwo";
        assertResultsPresentInDataResponse(url);

        // Process definition id
        url = RestUrls.createRelativeResourceUrl(RestUrls.URL_PROCESS_INSTANCE_COLLECTION) + "?processDefinitionId=" + processInstance.getProcessDefinitionId();
        assertResultsPresentInDataResponse(url, id);

        url = RestUrls.createRelativeResourceUrl(RestUrls.URL_PROCESS_INSTANCE_COLLECTION) + "?processDefinitionId=anotherId";
        assertResultsPresentInDataResponse(url);

        // Involved user
        url = RestUrls.createRelativeResourceUrl(RestUrls.URL_PROCESS_INSTANCE_COLLECTION) + "?involvedUser=kermit";
        assertResultsPresentInDataResponse(url, id);

        url = RestUrls.createRelativeResourceUrl(RestUrls.URL_PROCESS_INSTANCE_COLLECTION) + "?involvedUser=gonzo";
        assertResultsPresentInDataResponse(url);

        // Active process
        url = RestUrls.createRelativeResourceUrl(RestUrls.URL_PROCESS_INSTANCE_COLLECTION) + "?suspended=false";
        assertResultsPresentInDataResponse(url, id);

        url = RestUrls.createRelativeResourceUrl(RestUrls.URL_PROCESS_INSTANCE_COLLECTION) + "?suspended=true";
        assertResultsPresentInDataResponse(url);

        // Suspended process
        runtimeService.suspendProcessInstanceById(id);
        url = RestUrls.createRelativeResourceUrl(RestUrls.URL_PROCESS_INSTANCE_COLLECTION) + "?suspended=true";
        assertResultsPresentInDataResponse(url, id);

        url = RestUrls.createRelativeResourceUrl(RestUrls.URL_PROCESS_INSTANCE_COLLECTION) + "?suspended=false";
        assertResultsPresentInDataResponse(url);
        runtimeService.activateProcessInstanceById(id);

        // Complete first task in the process to have a subprocess created
        taskService.complete(taskService.createTaskQuery().processInstanceId(id).singleResult().getId());

        ProcessInstance subProcess = runtimeService.createProcessInstanceQuery().superProcessInstanceId(id).singleResult();
        assertThat(subProcess).isNotNull();

        // Super-process instance id
        url = RestUrls.createRelativeResourceUrl(RestUrls.URL_PROCESS_INSTANCE_COLLECTION) + "?superProcessInstanceId=" + id;
        assertResultsPresentInDataResponse(url, subProcess.getId());

        url = RestUrls.createRelativeResourceUrl(RestUrls.URL_PROCESS_INSTANCE_COLLECTION) + "?superProcessInstanceId=anotherId";
        assertResultsPresentInDataResponse(url);

        // Sub-process instance id
        url = RestUrls.createRelativeResourceUrl(RestUrls.URL_PROCESS_INSTANCE_COLLECTION) + "?subProcessInstanceId=" + subProcess.getId();
        assertResultsPresentInDataResponse(url, id);

        url = RestUrls.createRelativeResourceUrl(RestUrls.URL_PROCESS_INSTANCE_COLLECTION) + "?subProcessInstanceId=anotherId";
        assertResultsPresentInDataResponse(url);
    }

    /**
     * Test getting a list of sorted process instance
     */
    @Test
    @Deployment(resources = { "org/flowable/rest/service/api/runtime/ProcessInstanceResourceTest.process-one.bpmn20.xml" })
    public void testGetProcessInstancesSorted() throws Exception {
        Instant initialTime = Instant.now();
        processEngineConfiguration.getClock().setCurrentTime(Date.from(initialTime));
        String nowInstanceId = runtimeService.startProcessInstanceByKey("processOne", "now").getId();

        processEngineConfiguration.getClock().setCurrentTime(Date.from(initialTime.plus(1, ChronoUnit.HOURS)));
        String nowPlus1InstanceId = runtimeService.startProcessInstanceByKey("processOne", "nowPlus1").getId();

        processEngineConfiguration.getClock().setCurrentTime(Date.from(initialTime.minus(1, ChronoUnit.HOURS)));
        String nowMinus1InstanceId = runtimeService.startProcessInstanceByKey("processOne", "nowMinus1").getId();

        List<String> sortedIds = new ArrayList<>();
        sortedIds.add(nowInstanceId);
        sortedIds.add(nowPlus1InstanceId);
        sortedIds.add(nowMinus1InstanceId);
        Collections.sort(sortedIds);

        // Test without any parameters
        String url = RestUrls.createRelativeResourceUrl(RestUrls.URL_PROCESS_INSTANCE_COLLECTION);
        assertResultsExactlyPresentInDataResponse(url, sortedIds.toArray(new String[0]));

        // Sort by start time
        url = RestUrls.createRelativeResourceUrl(RestUrls.URL_PROCESS_INSTANCE_COLLECTION) + "?sort=startTime";
        assertResultsExactlyPresentInDataResponse(url, nowMinus1InstanceId, nowInstanceId, nowPlus1InstanceId);

        // Sort by start time desc
        url = RestUrls.createRelativeResourceUrl(RestUrls.URL_PROCESS_INSTANCE_COLLECTION) + "?sort=startTime&order=desc";
        assertResultsExactlyPresentInDataResponse(url, nowPlus1InstanceId, nowInstanceId, nowMinus1InstanceId);
    }

    /**
     * Test getting a list of process instance, using all tenant filters.
     */
    @Test
    @Deployment(resources = { "org/flowable/rest/service/api/runtime/ProcessInstanceResourceTest.process-one.bpmn20.xml" })
    public void testGetProcessInstancesTenant() throws Exception {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("processOne", "myBusinessKey");
        String id = processInstance.getId();

        // Test without tenant id
        String url = RestUrls.createRelativeResourceUrl(RestUrls.URL_PROCESS_INSTANCE_COLLECTION) + "?withoutTenantId=true";
        assertResultsPresentInDataResponse(url, id);

        // Update the tenant for the deployment
        managementService.executeCommand(new ChangeDeploymentTenantIdCmd(deploymentId, "myTenant"));

        // Test tenant id
        url = RestUrls.createRelativeResourceUrl(RestUrls.URL_PROCESS_INSTANCE_COLLECTION) + "?tenantId=myTenant";
        assertResultsPresentInDataResponse(url, id);

        url = RestUrls.createRelativeResourceUrl(RestUrls.URL_PROCESS_INSTANCE_COLLECTION) + "?tenantId=anotherTenant";
        assertResultsPresentInDataResponse(url);

        // Test tenant id like
        url = RestUrls.createRelativeResourceUrl(RestUrls.URL_PROCESS_INSTANCE_COLLECTION) + "?tenantIdLike=" + encode("%enant");
        assertResultsPresentInDataResponse(url, id);

        url = RestUrls.createRelativeResourceUrl(RestUrls.URL_PROCESS_INSTANCE_COLLECTION) + "?tenantIdLike=" + encode("%what");
        assertResultsPresentInDataResponse(url);

        // Test without tenant id
        url = RestUrls.createRelativeResourceUrl(RestUrls.URL_PROCESS_INSTANCE_COLLECTION) + "?withoutTenantId=true";
        assertResultsPresentInDataResponse(url);
    }

    /**
     * Test starting a process instance using procDefinitionId, key procDefinitionKey business-key.
     */
    @Test
    @Deployment(resources = { "org/flowable/rest/service/api/runtime/ProcessInstanceResourceTest.process-one.bpmn20.xml" })
    public void testStartProcess() throws Exception {
        ObjectNode requestNode = objectMapper.createObjectNode();

        // Start using process definition key
        requestNode.put("processDefinitionKey", "processOne");

        HttpPost httpPost = new HttpPost(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_PROCESS_INSTANCE_COLLECTION));
        httpPost.setEntity(new StringEntity(requestNode.toString()));
        CloseableHttpResponse response = executeRequest(httpPost, HttpStatus.SC_CREATED);

        ProcessInstance processInstance = runtimeService.createProcessInstanceQuery().singleResult();
        assertThat(processInstance).isNotNull();

        HistoricProcessInstance historicProcessInstance = historyService.createHistoricProcessInstanceQuery().processInstanceId(processInstance.getId())
                .singleResult();
        assertThat(historicProcessInstance).isNotNull();
        assertThat(historicProcessInstance.getStartUserId()).isEqualTo("kermit");

        JsonNode responseNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        assertThatJson(responseNode)
                .when(Option.IGNORING_EXTRA_FIELDS)
                .isEqualTo("{"
                        + "id: '" + processInstance.getId() + "',"
                        + "businessKey: null,"
                        + "suspended: false,"
                        + "url: '" + SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_PROCESS_INSTANCE, processInstance.getId()) + "',"
                        + "processDefinitionUrl: '" + SERVER_URL_PREFIX + RestUrls
                        .createRelativeResourceUrl(RestUrls.URL_PROCESS_DEFINITION, processInstance.getProcessDefinitionId()) + "'"
                        + "}");

        runtimeService.deleteProcessInstance(processInstance.getId(), "testing");

        // Start using process definition id
        requestNode = objectMapper.createObjectNode();
        requestNode.put("processDefinitionId", repositoryService.createProcessDefinitionQuery().processDefinitionKey("processOne").singleResult().getId());
        httpPost.setEntity(new StringEntity(requestNode.toString()));
        response = executeRequest(httpPost, HttpStatus.SC_CREATED);

        processInstance = runtimeService.createProcessInstanceQuery().singleResult();
        assertThat(processInstance).isNotNull();

        responseNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        assertThat(responseNode).isNotNull();
        assertThatJson(responseNode)
                .when(Option.IGNORING_EXTRA_FIELDS)
                .isEqualTo("{"
                        + "id: '" + processInstance.getId() + "',"
                        + "businessKey: null,"
                        + "suspended: false,"
                        + "url: '" + SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_PROCESS_INSTANCE, processInstance.getId()) + "',"
                        + "processDefinitionUrl: '" + SERVER_URL_PREFIX + RestUrls
                        .createRelativeResourceUrl(RestUrls.URL_PROCESS_DEFINITION, processInstance.getProcessDefinitionId()) + "'"
                        + "}");

        runtimeService.deleteProcessInstance(processInstance.getId(), "testing");

        // Start using message
        requestNode = objectMapper.createObjectNode();
        requestNode.put("message", "newInvoiceMessage");
        httpPost.setEntity(new StringEntity(requestNode.toString()));
        response = executeRequest(httpPost, HttpStatus.SC_CREATED);

        processInstance = runtimeService.createProcessInstanceQuery().singleResult();
        assertThat(processInstance).isNotNull();

        responseNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        assertThat(responseNode).isNotNull();
        assertThatJson(responseNode)
                .when(Option.IGNORING_EXTRA_FIELDS)
                .isEqualTo("{"
                        + "id: '" + processInstance.getId() + "',"
                        + "businessKey: null,"
                        + "suspended: false,"
                        + "url: '" + SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_PROCESS_INSTANCE, processInstance.getId()) + "',"
                        + "processDefinitionUrl: '" + SERVER_URL_PREFIX + RestUrls
                        .createRelativeResourceUrl(RestUrls.URL_PROCESS_DEFINITION, processInstance.getProcessDefinitionId()) + "'"
                        + "}");

        // Start using process definition id and business key
        requestNode = objectMapper.createObjectNode();
        requestNode.put("processDefinitionId", repositoryService.createProcessDefinitionQuery().processDefinitionKey("processOne").singleResult().getId());
        requestNode.put("businessKey", "myBusinessKey");
        httpPost.setEntity(new StringEntity(requestNode.toString()));
        response = executeRequest(httpPost, HttpStatus.SC_CREATED);

        responseNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        assertThat(responseNode).isNotNull();
        assertThat(responseNode.get("businessKey").textValue()).isEqualTo("myBusinessKey");
    }

    /**
     * Test starting a process instance passing in variables to set.
     */
    @Test
    @Deployment(resources = { "org/flowable/rest/service/api/runtime/ProcessInstanceResourceTest.process-one.bpmn20.xml" })
    public void testStartProcessWithVariables() throws Exception {
        ArrayNode variablesNode = objectMapper.createArrayNode();

        // String variable
        ObjectNode stringVarNode = variablesNode.addObject();
        stringVarNode.put("name", "stringVariable");
        stringVarNode.put("value", "simple string value");
        stringVarNode.put("type", "string");

        ObjectNode integerVarNode = variablesNode.addObject();
        integerVarNode.put("name", "integerVariable");
        integerVarNode.put("value", 1234);
        integerVarNode.put("type", "integer");

        ObjectNode shortVarNode = variablesNode.addObject();
        shortVarNode.put("name", "shortVariable");
        shortVarNode.put("value", 123);
        shortVarNode.put("type", "short");

        ObjectNode longVarNode = variablesNode.addObject();
        longVarNode.put("name", "longVariable");
        longVarNode.put("value", 4567890L);
        longVarNode.put("type", "long");

        ObjectNode doubleVarNode = variablesNode.addObject();
        doubleVarNode.put("name", "doubleVariable");
        doubleVarNode.put("value", 123.456);
        doubleVarNode.put("type", "double");

        ObjectNode booleanVarNode = variablesNode.addObject();
        booleanVarNode.put("name", "booleanVariable");
        booleanVarNode.put("value", Boolean.TRUE);
        booleanVarNode.put("type", "boolean");

        // Date
        Calendar varCal = Calendar.getInstance();
        String isoString = getISODateString(varCal.getTime());
        ObjectNode dateVarNode = variablesNode.addObject();
        dateVarNode.put("name", "dateVariable");
        dateVarNode.put("value", isoString);
        dateVarNode.put("type", "date");

        ObjectNode requestNode = objectMapper.createObjectNode();

        // Start using process definition key, passing in variables
        requestNode.put("processDefinitionKey", "processOne");
        requestNode.set("variables", variablesNode);

        HttpPost httpPost = new HttpPost(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_PROCESS_INSTANCE_COLLECTION));
        httpPost.setEntity(new StringEntity(requestNode.toString()));
        CloseableHttpResponse response = executeRequest(httpPost, HttpStatus.SC_CREATED);

        JsonNode responseNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        assertThatJson(responseNode)
                .when(Option.IGNORING_EXTRA_FIELDS)
                .isEqualTo("{"
                        + "   ended: false"
                        + "}");
        JsonNode variablesArrayNode = responseNode.get("variables");
        assertThat(variablesArrayNode).hasSize(7);

        ProcessInstance processInstance = runtimeService.createProcessInstanceQuery().singleResult();
        assertThat(processInstance).isNotNull();

        // Check if engine has correct variables set
        Map<String, Object> processVariables = runtimeService.getVariables(processInstance.getId());
        assertThat(processVariables)
                .containsOnly(
                        entry("stringVariable", "simple string value"),
                        entry("integerVariable", 1234),
                        entry("shortVariable", (short) 123),
                        entry("longVariable", 4567890L),
                        entry("doubleVariable", 123.456),
                        entry("booleanVariable", Boolean.TRUE),
                        entry("dateVariable", dateFormat.parse(isoString))
                );
    }

    /**
     * Test starting a process instance passing in variables to set.
     */
    @Test
    @Deployment(resources = { "org/flowable/rest/service/api/runtime/ProcessInstanceResourceTest.process-one.bpmn20.xml" })
    public void testStartProcessWithVariablesAndReturnVariables() throws Exception {
        ArrayNode variablesNode = objectMapper.createArrayNode();

        // String variable
        ObjectNode stringVarNode = variablesNode.addObject();
        stringVarNode.put("name", "stringVariable");
        stringVarNode.put("value", "simple string value");
        stringVarNode.put("type", "string");

        ObjectNode integerVarNode = variablesNode.addObject();
        integerVarNode.put("name", "integerVariable");
        integerVarNode.put("value", 1234);
        integerVarNode.put("type", "integer");

        ObjectNode requestNode = objectMapper.createObjectNode();

        // Start using process definition key, passing in variables
        requestNode.put("processDefinitionKey", "processOne");
        requestNode.put("returnVariables", true);
        requestNode.set("variables", variablesNode);

        HttpPost httpPost = new HttpPost(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_PROCESS_INSTANCE_COLLECTION));
        httpPost.setEntity(new StringEntity(requestNode.toString()));
        CloseableHttpResponse response = executeRequest(httpPost, HttpStatus.SC_CREATED);
        JsonNode responseNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        assertThatJson(responseNode)
                .when(Option.IGNORING_EXTRA_FIELDS, Option.IGNORING_ARRAY_ORDER)
                .isEqualTo("{"
                        + "  ended: false,"
                        + "  variables: [ {"
                        + "               name: 'stringVariable',"
                        + "               value: 'simple string value',"
                        + "               type: 'string'"
                        + "              }, {"
                        + "               name: 'integerVariable',"
                        + "               value: 1234,"
                        + "               type: 'integer'"
                        + "              } ]"
                        + "}");

        ProcessInstance processInstance = runtimeService.createProcessInstanceQuery().singleResult();
        assertThat(processInstance).isNotNull();

        // Check if engine has correct variables set
        Map<String, Object> processVariables = runtimeService.getVariables(processInstance.getId());

        assertThat(processVariables)
                .containsOnly(
                        entry("stringVariable", "simple string value"),
                        entry("integerVariable", 1234)
                );
    }

    @Test
    @Deployment(resources = { "org/flowable/rest/service/api/runtime/ProcessInstanceResourceTest.process-with-form.bpmn20.xml",
            "org/flowable/rest/service/api/runtime/simple.form" })
    public void testStartProcessWithForm() throws Exception {
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().processDefinitionKey("processOne").singleResult();
        try {
            FormDefinition formDefinition = formRepositoryService.createFormDefinitionQuery().formDefinitionKey("form1").singleResult();
            assertThat(formDefinition).isNotNull();

            FormInstance formInstance = formEngineFormService.createFormInstanceQuery().formDefinitionId(formDefinition.getId()).singleResult();
            assertThat(formInstance).isNull();

            String url = RestUrls.createRelativeResourceUrl(RestUrls.URL_PROCESS_DEFINITION_START_FORM, processDefinition.getId());
            CloseableHttpResponse response = executeRequest(new HttpGet(SERVER_URL_PREFIX + url), HttpStatus.SC_OK);
            JsonNode responseNode = objectMapper.readTree(response.getEntity().getContent());
            closeResponse(response);
            assertThatJson(responseNode)
                    .when(Option.IGNORING_EXTRA_FIELDS, Option.IGNORING_ARRAY_ORDER)
                    .isEqualTo("{"
                            + " id: '" + formDefinition.getId() + "',"
                            + " key: '" + formDefinition.getKey() + "',"
                            + " name: '" + formDefinition.getName() + "',"
                            + " fields : [ {"
                            + "               id: 'user'"
                            + "          }, {"
                            + "               id: 'number'"
                            + "          } ]"
                            + "}");

            ArrayNode formVariablesNode = objectMapper.createArrayNode();

            // String variable
            ObjectNode stringVarNode = formVariablesNode.addObject();
            stringVarNode.put("name", "user");
            stringVarNode.put("value", "simple string value");
            stringVarNode.put("type", "string");

            ObjectNode integerVarNode = formVariablesNode.addObject();
            integerVarNode.put("name", "number");
            integerVarNode.put("value", 1234);
            integerVarNode.put("type", "integer");

            ObjectNode requestNode = objectMapper.createObjectNode();

            // Start using process definition key, passing in variables
            requestNode.put("processDefinitionKey", "processOne");
            requestNode.set("startFormVariables", formVariablesNode);

            HttpPost httpPost = new HttpPost(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_PROCESS_INSTANCE_COLLECTION));
            httpPost.setEntity(new StringEntity(requestNode.toString()));
            response = executeRequest(httpPost, HttpStatus.SC_CREATED);

            responseNode = objectMapper.readTree(response.getEntity().getContent());
            closeResponse(response);
            assertThatJson(responseNode)
                    .when(Option.IGNORING_EXTRA_FIELDS)
                    .isEqualTo("{"
                            + "   ended: false"
                            + "}");

            String processInstanceId = responseNode.get("id").asText();
            assertThat(runtimeService.getVariable(processInstanceId, "user")).isEqualTo("simple string value");
            assertThat(runtimeService.getVariable(processInstanceId, "number")).isEqualTo(1234);

            formInstance = formEngineFormService.createFormInstanceQuery().formDefinitionId(formDefinition.getId()).singleResult();
            assertThat(formInstance).isNotNull();
            byte[] valuesBytes = formEngineFormService.getFormInstanceValues(formInstance.getId());
            assertThat(valuesBytes).isNotNull();
            JsonNode instanceNode = objectMapper.readTree(valuesBytes);
            assertThatJson(instanceNode)
                    .isEqualTo("{"
                            + "values: {"
                            + "        number: '1234',"
                            + "        user: 'simple string value'"
                            + "        }"
                            + "}");

        } finally {
            formEngineFormService.deleteFormInstancesByProcessDefinition(processDefinition.getId());

            List<FormDeployment> formDeployments = formRepositoryService.createDeploymentQuery().list();
            for (FormDeployment formDeployment : formDeployments) {
                formRepositoryService.deleteDeployment(formDeployment.getId(), true);
            }
        }
    }

    @Test
    @Deployment(resources = { "org/flowable/rest/service/api/oneTaskProcess.bpmn20.xml" })
    public void testStartProcessUsingKeyAndTenantId() throws Exception {
        org.flowable.engine.repository.Deployment tenantDeployment = null;

        try {
            // Deploy the same process, in another tenant
            tenantDeployment = repositoryService.createDeployment().addClasspathResource("org/flowable/rest/service/api/oneTaskProcess.bpmn20.xml").tenantId("tenant1").deploy();

            ObjectNode requestNode = objectMapper.createObjectNode();

            // Start using process definition key, in tenant 1
            requestNode.put("processDefinitionKey", "oneTaskProcess");
            requestNode.put("tenantId", "tenant1");

            HttpPost httpPost = new HttpPost(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_PROCESS_INSTANCE_COLLECTION));
            httpPost.setEntity(new StringEntity(requestNode.toString()));
            CloseableHttpResponse response = executeRequest(httpPost, HttpStatus.SC_CREATED);
            closeResponse(response);

            // Only one process should have been started
            ProcessInstance processInstance = runtimeService.createProcessInstanceQuery().singleResult();
            assertThat(processInstance).isNotNull();
            assertThat(processInstance.getTenantId()).isEqualTo("tenant1");

            // Start using an unexisting tenant
            requestNode.put("processDefinitionKey", "oneTaskProcess");
            requestNode.put("tenantId", "tenantThatDoesntExist");

            httpPost.setEntity(new StringEntity(requestNode.toString()));
            response = executeRequest(httpPost, HttpStatus.SC_BAD_REQUEST);
            closeResponse(response);

        } finally {
            // Cleanup deployment in tenant
            if (tenantDeployment != null) {
                repositoryService.deleteDeployment(tenantDeployment.getId(), true);
            }
        }
    }

    /**
     * Test starting a process instance, covering all edge-cases.
     */
    @Test
    public void testStartProcessExceptions() throws Exception {

        ObjectNode requestNode = objectMapper.createObjectNode();

        // Try starting without id and key
        HttpPost httpPost = new HttpPost(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_PROCESS_INSTANCE_COLLECTION));
        httpPost.setEntity(new StringEntity(requestNode.toString()));
        closeResponse(executeRequest(httpPost, HttpStatus.SC_BAD_REQUEST));

        // Try starting with both id and key
        requestNode = objectMapper.createObjectNode();
        requestNode.put("processDefinitionId", "123");
        requestNode.put("processDefinitionKey", "456");

        httpPost.setEntity(new StringEntity(requestNode.toString()));
        closeResponse(executeRequest(httpPost, HttpStatus.SC_BAD_REQUEST));

        // Try starting with both message and key
        requestNode = objectMapper.createObjectNode();
        requestNode.put("processDefinitionId", "123");
        requestNode.put("message", "456");

        httpPost.setEntity(new StringEntity(requestNode.toString()));
        closeResponse(executeRequest(httpPost, HttpStatus.SC_BAD_REQUEST));

        // Try starting with unexisting process definition key
        requestNode = objectMapper.createObjectNode();
        requestNode.put("processDefinitionKey", "123");

        httpPost.setEntity(new StringEntity(requestNode.toString()));
        closeResponse(executeRequest(httpPost, HttpStatus.SC_BAD_REQUEST));

        // Try starting with unexisting process definition id
        requestNode = objectMapper.createObjectNode();
        requestNode.put("processDefinitionId", "123");

        httpPost.setEntity(new StringEntity(requestNode.toString()));
        closeResponse(executeRequest(httpPost, HttpStatus.SC_BAD_REQUEST));

        // Try starting with unexisting message
        requestNode = objectMapper.createObjectNode();
        requestNode.put("message", "unexistingmessage");

        httpPost.setEntity(new StringEntity(requestNode.toString()));
        closeResponse(executeRequest(httpPost, HttpStatus.SC_BAD_REQUEST));
    }

    /**
     * Explicitly testing the statelessness of the Rest API.
     */
    @Test
    @Deployment(resources = { "org/flowable/rest/service/api/runtime/ProcessInstanceResourceTest.process-one.bpmn20.xml" })
    public void testStartProcessWithSameHttpClient() throws Exception {
        ObjectNode requestNode = objectMapper.createObjectNode();

        // Start using process definition key
        requestNode.put("processDefinitionKey", "processOne");

        HttpPost httpPost = new HttpPost(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_PROCESS_INSTANCE_COLLECTION));
        httpPost.setEntity(new StringEntity(requestNode.toString()));

        // First call
        closeResponse(executeRequest(httpPost, HttpStatus.SC_CREATED));

        // Second call
        closeResponse(executeRequest(httpPost, HttpStatus.SC_CREATED));

        List<ProcessInstance> processInstances = runtimeService.createProcessInstanceQuery().list();
        assertThat(processInstances).hasSize(2);
        for (ProcessInstance processInstance : processInstances) {
            HistoricProcessInstance historicProcessInstance = historyService.createHistoricProcessInstanceQuery().processInstanceId(processInstance.getId())
                    .singleResult();
            assertThat(historicProcessInstance).isNotNull();
            assertThat(historicProcessInstance.getStartUserId()).isEqualTo("kermit");
        }

    }
    
    @Test
    @Deployment(resources = { "org/flowable/rest/service/api/twoTaskProcess.bpmn20.xml" })
    public void testGetProcessInstancesByActiveActivityId() throws Exception {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
        
        // check that the right process is returned with no variables
        String url = RestUrls.createRelativeResourceUrl(RestUrls.URL_PROCESS_INSTANCE_COLLECTION) + "?activeActivityId=processTask";

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

        url = RestUrls.createRelativeResourceUrl(RestUrls.URL_PROCESS_INSTANCE_COLLECTION) + "?activeActivityId=processTask2";

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
        
        url = RestUrls.createRelativeResourceUrl(RestUrls.URL_PROCESS_INSTANCE_COLLECTION) + "?activeActivityId=processTask2";

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
}
