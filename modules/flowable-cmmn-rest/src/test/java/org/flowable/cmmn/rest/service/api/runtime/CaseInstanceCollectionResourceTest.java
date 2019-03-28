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

package org.flowable.cmmn.rest.service.api.runtime;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.flowable.cmmn.api.history.HistoricCaseInstance;
import org.flowable.cmmn.api.repository.CaseDefinition;
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.rest.service.BaseSpringRestTestCase;
import org.flowable.cmmn.rest.service.api.CmmnRestUrls;
import org.flowable.form.api.FormDefinition;
import org.flowable.form.api.FormDeployment;
import org.flowable.form.api.FormInstance;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Test for all REST-operations related to a single Case instance resource.
 * 
 * @author Tijs Rademakers
 * @author Filip Hrisafov
 * @author Jose Antonio Alvarez
 */
public class CaseInstanceCollectionResourceTest extends BaseSpringRestTestCase {

    /**
     * Test getting a list of case instance, using all possible filters.
     */
    @CmmnDeployment(resources = { "org/flowable/cmmn/rest/service/api/repository/oneHumanTaskCase.cmmn" })
    public void testGetCaseInstances() throws Exception {
        CaseInstance caseInstance = runtimeService.createCaseInstanceBuilder().caseDefinitionKey("oneHumanTaskCase").businessKey("myBusinessKey").start();
        String id = caseInstance.getId();

        // Test without any parameters
        String url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_CASE_INSTANCE_COLLECTION);
        assertResultsPresentInDataResponse(url, id);

        // Process instance id
        url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_CASE_INSTANCE_COLLECTION) + "?id=" + id;
        assertResultsPresentInDataResponse(url, id);

        url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_CASE_INSTANCE_COLLECTION) + "?id=anotherId";
        assertResultsPresentInDataResponse(url);

        // Process instance business key
        url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_CASE_INSTANCE_COLLECTION) + "?businessKey=myBusinessKey";
        assertResultsPresentInDataResponse(url, id);

        url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_CASE_INSTANCE_COLLECTION) + "?businessKey=anotherBusinessKey";
        assertResultsPresentInDataResponse(url);

        // Process definition key
        url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_CASE_INSTANCE_COLLECTION) + "?caseDefinitionKey=oneHumanTaskCase";
        assertResultsPresentInDataResponse(url, id);

        url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_CASE_INSTANCE_COLLECTION) + "?caseDefinitionKey=caseTwo";
        assertResultsPresentInDataResponse(url);

        // Process definition id
        url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_CASE_INSTANCE_COLLECTION) + "?caseDefinitionId=" + caseInstance.getCaseDefinitionId();
        assertResultsPresentInDataResponse(url, id);

        url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_CASE_INSTANCE_COLLECTION) + "?caseDefinitionId=anotherId";
        assertResultsPresentInDataResponse(url);
    }

    /**
     * Test getting a list of sorted case instance
     */
    @CmmnDeployment(resources = { "org/flowable/cmmn/rest/service/api/repository/oneHumanTaskCase.cmmn" })
    public void testGetCaseInstancesSorted() throws Exception {
        Instant initialTime = Instant.now();
        cmmnEngineConfiguration.getClock().setCurrentTime(Date.from(initialTime));
        String nowInstanceId = runtimeService.createCaseInstanceBuilder().caseDefinitionKey("oneHumanTaskCase").businessKey("now").start().getId();

        cmmnEngineConfiguration.getClock().setCurrentTime(Date.from(initialTime.plus(1, ChronoUnit.HOURS)));
        String nowPlus1InstanceId = runtimeService.createCaseInstanceBuilder().caseDefinitionKey("oneHumanTaskCase").businessKey("nowPlus1").start().getId();

        cmmnEngineConfiguration.getClock().setCurrentTime(Date.from(initialTime.minus(1, ChronoUnit.HOURS)));
        String nowMinus1InstanceId = runtimeService.createCaseInstanceBuilder().caseDefinitionKey("oneHumanTaskCase").businessKey("nowMinus1").start().getId();

        List<String> sortedIds = new ArrayList<>();
        sortedIds.add(nowInstanceId);
        sortedIds.add(nowPlus1InstanceId);
        sortedIds.add(nowMinus1InstanceId);
        Collections.sort(sortedIds);

        // Test without any parameters
        String url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_CASE_INSTANCE_COLLECTION);
        assertResultsExactlyPresentInDataResponse(url, sortedIds.toArray(new String[0]));

        // Sort by start time
        url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_CASE_INSTANCE_COLLECTION) + "?sort=startTime";
        assertResultsExactlyPresentInDataResponse(url, nowMinus1InstanceId, nowInstanceId, nowPlus1InstanceId);

        // Sort by start time desc
        url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_CASE_INSTANCE_COLLECTION) + "?sort=startTime&order=desc";
        assertResultsExactlyPresentInDataResponse(url, nowPlus1InstanceId, nowInstanceId, nowMinus1InstanceId);
    }

    /**
     * Test getting a list of case instance, using all tenant filters.
     */
    @CmmnDeployment(resources = { "org/flowable/cmmn/rest/service/api/repository/oneHumanTaskCase.cmmn" })
    public void testGetCaseInstancesTenant() throws Exception {
        CaseInstance caseInstance = runtimeService.createCaseInstanceBuilder().caseDefinitionKey("oneHumanTaskCase").businessKey("myBusinessKey").start();
        String id = caseInstance.getId();

        // Test without tenant id
        String url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_CASE_INSTANCE_COLLECTION) + "?withoutTenantId=true";
        assertResultsPresentInDataResponse(url, id);

        org.flowable.cmmn.api.repository.CmmnDeployment deployment = repositoryService.createDeployment().addClasspathResource(
                        "org/flowable/cmmn/rest/service/api/repository/oneHumanTaskCase.cmmn").tenantId("myTenant").deploy();
        
        try {
            caseInstance = runtimeService.createCaseInstanceBuilder().caseDefinitionKey("oneHumanTaskCase").businessKey("myBusinessKey").tenantId("myTenant").start();
            String idWithTenant = caseInstance.getId();
    
            // Test tenant id
            url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_CASE_INSTANCE_COLLECTION) + "?tenantId=myTenant";
            assertResultsPresentInDataResponse(url, idWithTenant);
    
            url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_CASE_INSTANCE_COLLECTION) + "?tenantId=anotherTenant";
            assertResultsPresentInDataResponse(url);
    
            // Test tenant id like
            url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_CASE_INSTANCE_COLLECTION) + "?tenantIdLike=" + encode("%enant");
            assertResultsPresentInDataResponse(url, idWithTenant);
    
            url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_CASE_INSTANCE_COLLECTION) + "?tenantIdLike=" + encode("%what");
            assertResultsPresentInDataResponse(url);
    
            // Test without tenant id
            url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_CASE_INSTANCE_COLLECTION) + "?withoutTenantId=true";
            assertResultsPresentInDataResponse(url, id);
        } finally {
            repositoryService.deleteDeployment(deployment.getId(), true);
        }
    }

    /**
     * Test getting a list of case instance, using the variable selector
     */
    @CmmnDeployment(resources = { "org/flowable/cmmn/rest/service/api/repository/oneHumanTaskCase.cmmn" })
    public void testGetCaseInstancesWithVariables() throws Exception {
        runtimeService.createCaseInstanceBuilder().caseDefinitionKey("oneHumanTaskCase").businessKey("myBusinessKey").variable("someVar", "someValue").start();

        // Test without any parameters, no variables included by default
        String url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_CASE_INSTANCE_COLLECTION);

        CloseableHttpResponse response = executeRequest(new HttpGet(SERVER_URL_PREFIX + url), HttpStatus.SC_OK);

        JsonNode rootNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        assertTrue(rootNode.size() > 0);
        assertEquals(1, rootNode.get("data").size());
        JsonNode dataNode = rootNode.get("data").get(0);
        JsonNode variableNodes = dataNode.get("variables");
        assertEquals(0, variableNodes.size());

        // Test excluding variables
        url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_CASE_INSTANCE_COLLECTION) + "?includeCaseVariables=false";

        response = executeRequest(new HttpGet(SERVER_URL_PREFIX + url), HttpStatus.SC_OK);

        rootNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        assertTrue(rootNode.size() > 0);
        assertEquals(1, rootNode.get("data").size());
        dataNode = rootNode.get("data").get(0);
        variableNodes = dataNode.get("variables");
        assertEquals(0, variableNodes.size());

        // Test including variables
        url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_CASE_INSTANCE_COLLECTION) + "?includeCaseVariables=true";

        response = executeRequest(new HttpGet(SERVER_URL_PREFIX + url), HttpStatus.SC_OK);

        rootNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        assertTrue(rootNode.size() > 0);
        assertEquals(1, rootNode.get("data").size());
        dataNode = rootNode.get("data").get(0);
        variableNodes = dataNode.get("variables");
        assertEquals(1, variableNodes.size());

        variableNodes = dataNode.get("variables");
        assertEquals(1, variableNodes.size());
        assertNotNull(variableNodes.get(0).get("name"));
        assertNotNull(variableNodes.get(0).get("value"));

        assertEquals("someVar", variableNodes.get(0).get("name").asText());
        assertEquals("someValue", variableNodes.get(0).get("value").asText());
    }


    /**
     * Test starting a case instance using caseDefinitionId, key caseDefinitionKey business-key.
     */
    @CmmnDeployment(resources = { "org/flowable/cmmn/rest/service/api/repository/oneHumanTaskCase.cmmn" })
    public void testStartCase() throws Exception {
        ObjectNode requestNode = objectMapper.createObjectNode();

        // Start using case definition key
        requestNode.put("caseDefinitionKey", "oneHumanTaskCase");

        HttpPost httpPost = new HttpPost(SERVER_URL_PREFIX + CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_CASE_INSTANCE_COLLECTION));
        httpPost.setEntity(new StringEntity(requestNode.toString()));
        CloseableHttpResponse response = executeRequest(httpPost, HttpStatus.SC_CREATED);

        CaseInstance caseInstance = runtimeService.createCaseInstanceQuery().singleResult();
        assertNotNull(caseInstance);

        HistoricCaseInstance historicCaseInstance = historyService.createHistoricCaseInstanceQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertNotNull(historicCaseInstance);
        assertEquals("kermit", historicCaseInstance.getStartUserId());

        JsonNode responseNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        assertNotNull(responseNode);
        assertEquals(caseInstance.getId(), responseNode.get("id").textValue());
        assertTrue(responseNode.get("businessKey").isNull());

        assertTrue(responseNode.get("url").asText().endsWith(CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_CASE_INSTANCE, caseInstance.getId())));
        assertTrue(responseNode.get("caseDefinitionUrl").asText().endsWith(CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_CASE_DEFINITION, caseInstance.getCaseDefinitionId())));
        runtimeService.terminateCaseInstance(caseInstance.getId());

        // Start using case definition id
        requestNode = objectMapper.createObjectNode();
        requestNode.put("caseDefinitionId", repositoryService.createCaseDefinitionQuery().caseDefinitionKey("oneHumanTaskCase").singleResult().getId());
        httpPost.setEntity(new StringEntity(requestNode.toString()));
        response = executeRequest(httpPost, HttpStatus.SC_CREATED);

        caseInstance = runtimeService.createCaseInstanceQuery().singleResult();
        assertNotNull(caseInstance);

        responseNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        assertNotNull(responseNode);
        assertEquals(caseInstance.getId(), responseNode.get("id").textValue());
        assertTrue(responseNode.get("businessKey").isNull());

        assertTrue(responseNode.get("url").asText().endsWith(CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_CASE_INSTANCE, caseInstance.getId())));
        assertTrue(responseNode.get("caseDefinitionUrl").asText().endsWith(CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_CASE_DEFINITION, caseInstance.getCaseDefinitionId())));
        runtimeService.terminateCaseInstance(caseInstance.getId());

        // Start using process definition id and business key
        requestNode = objectMapper.createObjectNode();
        requestNode.put("caseDefinitionId", repositoryService.createCaseDefinitionQuery().caseDefinitionKey("oneHumanTaskCase").singleResult().getId());
        requestNode.put("businessKey", "myBusinessKey");
        httpPost.setEntity(new StringEntity(requestNode.toString()));
        response = executeRequest(httpPost, HttpStatus.SC_CREATED);

        responseNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        assertNotNull(responseNode);
        assertEquals("myBusinessKey", responseNode.get("businessKey").textValue());
    }

    /**
     * Test starting a case instance passing in variables to set.
     */
    @CmmnDeployment(resources = { "org/flowable/cmmn/rest/service/api/repository/oneHumanTaskCase.cmmn" })
    public void testStartCaseWithVariables() throws Exception {
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

        // Start using case definition key, passing in variables
        requestNode.put("caseDefinitionKey", "oneHumanTaskCase");
        requestNode.set("variables", variablesNode);

        HttpPost httpPost = new HttpPost(SERVER_URL_PREFIX + CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_CASE_INSTANCE_COLLECTION));
        httpPost.setEntity(new StringEntity(requestNode.toString()));
        CloseableHttpResponse response = executeRequest(httpPost, HttpStatus.SC_CREATED);
        closeResponse(response);

        CaseInstance caseInstance = runtimeService.createCaseInstanceQuery().singleResult();
        assertNotNull(caseInstance);

        // Check if engine has correct variables set
        Map<String, Object> caseVariables = runtimeService.getVariables(caseInstance.getId());
        assertEquals(7, caseVariables.size());

        assertEquals("simple string value", caseVariables.get("stringVariable"));
        assertEquals(1234, caseVariables.get("integerVariable"));
        assertEquals((short) 123, caseVariables.get("shortVariable"));
        assertEquals(4567890L, caseVariables.get("longVariable"));
        assertEquals(123.456, caseVariables.get("doubleVariable"));
        assertEquals(Boolean.TRUE, caseVariables.get("booleanVariable"));
        assertEquals(longDateFormat.parse(isoString), caseVariables.get("dateVariable"));
    }

    @CmmnDeployment(resources = { "org/flowable/cmmn/rest/service/api/repository/oneHumanTaskCase.cmmn" })
    public void testStartCaseUsingKeyAndTenantId() throws Exception {
        org.flowable.cmmn.api.repository.CmmnDeployment tenantDeployment = null;

        try {
            // Deploy the same process, in another tenant
            tenantDeployment = repositoryService.createDeployment().addClasspathResource("org/flowable/cmmn/rest/service/api/repository/oneHumanTaskCase.cmmn").tenantId("tenant1").deploy();

            ObjectNode requestNode = objectMapper.createObjectNode();

            // Start using process definition key, in tenant 1
            requestNode.put("caseDefinitionKey", "oneHumanTaskCase");
            requestNode.put("tenantId", "tenant1");

            HttpPost httpPost = new HttpPost(SERVER_URL_PREFIX + CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_CASE_INSTANCE_COLLECTION));
            httpPost.setEntity(new StringEntity(requestNode.toString()));
            CloseableHttpResponse response = executeRequest(httpPost, HttpStatus.SC_CREATED);
            closeResponse(response);

            // Only one case should have been started
            CaseInstance caseInstance = runtimeService.createCaseInstanceQuery().singleResult();
            assertNotNull(caseInstance);
            assertEquals("tenant1", caseInstance.getTenantId());

        } finally {
            // Cleanup deployment in tenant
            if (tenantDeployment != null) {
                repositoryService.deleteDeployment(tenantDeployment.getId(), true);
            }
        }
    }
    
    @CmmnDeployment(resources = { "org/flowable/cmmn/rest/service/api/runtime/oneHumanTaskCaseWithStartForm.cmmn",
                    "org/flowable/cmmn/rest/service/api/runtime/simple.form"})
    public void testStartCaseWithForm() throws Exception {
        CaseDefinition caseDefinition = repositoryService.createCaseDefinitionQuery().caseDefinitionKey("oneHumanTaskCase").singleResult();
        try {
            FormDefinition formDefinition = formRepositoryService.createFormDefinitionQuery().formDefinitionKey("form1").singleResult();
            assertNotNull(formDefinition);
            
            FormInstance formInstance = formEngineFormService.createFormInstanceQuery().formDefinitionId(formDefinition.getId()).singleResult();
            assertNull(formInstance);
            
            String url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_CASE_DEFINITION_START_FORM, caseDefinition.getId());
            CloseableHttpResponse response = executeRequest(new HttpGet(SERVER_URL_PREFIX + url), HttpStatus.SC_OK);
            JsonNode responseNode = objectMapper.readTree(response.getEntity().getContent());
            closeResponse(response);
            assertEquals(formDefinition.getId(), responseNode.get("id").asText());
            assertEquals(formDefinition.getKey(), responseNode.get("key").asText());
            assertEquals(formDefinition.getName(), responseNode.get("name").asText());
            assertEquals(2, responseNode.get("fields").size());
            
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
            
            // Start using case definition key
            requestNode.put("caseDefinitionKey", "oneHumanTaskCase");
            requestNode.set("startFormVariables", formVariablesNode);
    
            HttpPost httpPost = new HttpPost(SERVER_URL_PREFIX + CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_CASE_INSTANCE_COLLECTION));
            httpPost.setEntity(new StringEntity(requestNode.toString()));
            response = executeRequest(httpPost, HttpStatus.SC_CREATED);
    
            CaseInstance caseInstance = runtimeService.createCaseInstanceQuery().singleResult();
            assertNotNull(caseInstance);
    
            assertEquals("simple string value", runtimeService.getVariable(caseInstance.getId(), "user"));
            assertEquals(1234, runtimeService.getVariable(caseInstance.getId(), "number"));
            
            formInstance = formEngineFormService.createFormInstanceQuery().formDefinitionId(formDefinition.getId()).singleResult();
            assertNotNull(formInstance);
            byte[] valuesBytes = formEngineFormService.getFormInstanceValues(formInstance.getId());
            assertNotNull(valuesBytes);
            JsonNode instanceNode = objectMapper.readTree(valuesBytes);
            JsonNode valuesNode = instanceNode.get("values");
            assertEquals("simple string value", valuesNode.get("user").asText());
            assertEquals(1234, valuesNode.get("number").asInt());
            
        } finally {
            formEngineFormService.deleteFormInstancesByScopeDefinition(caseDefinition.getId());
            
            List<FormDeployment> formDeployments = formRepositoryService.createDeploymentQuery().list();
            for (FormDeployment formDeployment : formDeployments) {
                formRepositoryService.deleteDeployment(formDeployment.getId());
            }
        }
    }

    /**
     * Test starting a process instance, covering all edge-cases.
     */
    public void testStartCaseExceptions() throws Exception {

        ObjectNode requestNode = objectMapper.createObjectNode();

        // Try starting without id and key
        HttpPost httpPost = new HttpPost(SERVER_URL_PREFIX + CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_CASE_INSTANCE_COLLECTION));
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
}
