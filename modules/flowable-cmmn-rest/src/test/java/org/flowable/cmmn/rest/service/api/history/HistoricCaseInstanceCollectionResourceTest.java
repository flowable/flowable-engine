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

package org.flowable.cmmn.rest.service.api.history;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
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
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.rest.service.BaseSpringRestTestCase;
import org.flowable.cmmn.rest.service.api.CmmnRestUrls;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import net.javacrumbs.jsonunit.core.Option;

/**
 * Test for REST-operation related to the historic case instance query resource.
 *
 * @author Tijs Rademakers
 */
public class HistoricCaseInstanceCollectionResourceTest extends BaseSpringRestTestCase {

    /**
     * Test querying historic case instance based on variables. GET history/historic-process-instances
     */
    @Test
    @CmmnDeployment(resources = { "org/flowable/cmmn/rest/service/api/repository/oneHumanTaskCase.cmmn" })
    public void testQueryCaseInstances() throws Exception {
        Calendar startTime = Calendar.getInstance();
        cmmnEngineConfiguration.getClock().setCurrentTime(startTime.getTime());

        HashMap<String, Object> caseVariables = new HashMap<>();
        caseVariables.put("stringVar", "Azerty");
        caseVariables.put("intVar", 67890);
        caseVariables.put("booleanVar", false);

        identityService.setAuthenticatedUserId("kermit");
        CaseInstance caseInstance = runtimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneHumanTaskCase")
                .name("myCaseInstanceName")
                .businessKey("myBusinessKey")
                .businessStatus("myBusinessStatus")
                .callbackId("someCallbackId")
                .callbackType("someCallbackType")
                .variables(caseVariables).start();
        Task task = taskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        taskService.complete(task.getId());

        identityService.setAuthenticatedUserId("fozzie");
        startTime.add(Calendar.DAY_OF_YEAR, 1);
        cmmnEngineConfiguration.getClock().setCurrentTime(startTime.getTime());
        CaseInstance caseInstance2 = runtimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneHumanTaskCase")
                .name("anotherCaseInstanceName")
                .businessKey("anotherBusinessKey")
                .businessStatus("anotherBusinessStatus")
                .callbackId("someOtherCallbackId")
                .callbackType("someOtherCallbackType")
                .start();
        
        identityService.setAuthenticatedUserId(null);

        String url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_HISTORIC_CASE_INSTANCES);

        assertResultsPresentInDataResponse(url + "?caseInstanceId=" + caseInstance.getId(), caseInstance.getId());

        assertResultsPresentInDataResponse(url + "?caseInstanceIds=someId," + caseInstance.getId(), caseInstance.getId());
        assertResultsPresentInDataResponse(url + "?caseInstanceIds=someId," + caseInstance.getId() + "," + caseInstance2.getId(), caseInstance.getId(), caseInstance2.getId());

        assertResultsPresentInDataResponse(url + "?finished=true", caseInstance.getId());

        assertResultsPresentInDataResponse(url + "?finished=false", caseInstance2.getId());

        assertResultsPresentInDataResponse(url + "?caseDefinitionId=" + caseInstance.getCaseDefinitionId(), caseInstance.getId(), caseInstance2.getId());

        assertResultsPresentInDataResponse(url + "?caseDefinitionId=" + caseInstance.getCaseDefinitionId() + "&finished=true", caseInstance.getId());

        assertResultsPresentInDataResponse(url + "?caseDefinitionKey=oneHumanTaskCase", caseInstance.getId(), caseInstance2.getId());

        assertResultsPresentInDataResponse(url + "?caseDefinitionName=" + encode("One Human Task Case"), caseInstance.getId(), caseInstance2.getId());
        
        assertResultsPresentInDataResponse(url + "?name=myCaseInstanceName", caseInstance.getId());
        assertResultsPresentInDataResponse(url + "?name=none");
        
        assertResultsPresentInDataResponse(url + "?nameLike=" + encode("%CaseInstanceName"), caseInstance.getId(), caseInstance2.getId());
        assertResultsPresentInDataResponse(url + "?nameLike=" + encode("my%InstanceName"), caseInstance.getId());
        assertResultsPresentInDataResponse(url + "?nameLike=none");
        
        assertResultsPresentInDataResponse(url + "?nameLikeIgnoreCase=" + encode("%CASEInstanceName"), caseInstance.getId(), caseInstance2.getId());
        assertResultsPresentInDataResponse(url + "?nameLikeIgnoreCase=" + encode("my%INSTANCEName"), caseInstance.getId());
        assertResultsPresentInDataResponse(url + "?nameLikeIgnoreCase=NONE");
        
        assertResultsPresentInDataResponse(url + "?businessKey=myBusinessKey", caseInstance.getId());
        assertResultsPresentInDataResponse(url + "?businessKey=none");
        
        assertResultsPresentInDataResponse(url + "?businessStatus=anotherBusinessStatus", caseInstance2.getId());
        assertResultsPresentInDataResponse(url + "?businessStatus=none");
        
        assertResultsPresentInDataResponse(url + "?callbackId=someCallbackId", caseInstance.getId());
        assertResultsPresentInDataResponse(url + "?callbackIds=noneExistingId,someCallbackId,someOtherCallbackId", caseInstance.getId(),caseInstance2.getId());
        assertResultsPresentInDataResponse(url + "?callbackType=someCallbackType", caseInstance.getId());

        assertResultsPresentInDataResponse(url + "?state=active", caseInstance2.getId());
        assertResultsPresentInDataResponse(url + "?state=none");
        
        assertResultsPresentInDataResponse(url + "?startedBy=kermit", caseInstance.getId());
        assertResultsPresentInDataResponse(url + "?startedBy=none");
        
        Calendar todayCal = new GregorianCalendar();
        Calendar futureCal = new GregorianCalendar(todayCal.get(Calendar.YEAR) + 2, todayCal.get(Calendar.MONTH), todayCal.get(Calendar.DAY_OF_MONTH));
        Calendar historicCal = new GregorianCalendar(todayCal.get(Calendar.YEAR) - 2, todayCal.get(Calendar.MONTH), todayCal.get(Calendar.DAY_OF_MONTH));
        
        assertResultsPresentInDataResponse(url + "?startedBefore=" + getISODateString(futureCal.getTime()), caseInstance.getId(), caseInstance2.getId());
        assertResultsPresentInDataResponse(url + "?startedBefore=" + getIsoDateStringWithoutSeconds(futureCal.getTime()), caseInstance.getId(), caseInstance2.getId());
        assertResultsPresentInDataResponse(url + "?startedBefore=" + getISODateString(futureCal.getTime()), caseInstance.getId(), caseInstance2.getId());
        assertResultsPresentInDataResponse(url + "?startedBefore=" + getISODateString(historicCal.getTime()));
        
        assertResultsPresentInDataResponse(url + "?startedAfter=" + getISODateString(historicCal.getTime()), caseInstance.getId(), caseInstance2.getId());
        assertResultsPresentInDataResponse(url + "?startedAfter=" + getIsoDateStringWithoutSeconds(historicCal.getTime()), caseInstance.getId(), caseInstance2.getId());
        assertResultsPresentInDataResponse(url + "?startedAfter=" + getIsoDateStringWithoutMS(historicCal.getTime()), caseInstance.getId(), caseInstance2.getId());

        assertResultsPresentInDataResponse(url + "?startedAfter=" + getISODateString(futureCal.getTime()));
        
        assertVariablesPresentInPostDataResponse(url, "?includeCaseVariables=false&caseInstanceId=" + caseInstance.getId(), caseInstance.getId(),
                new HashMap<>());
        assertVariablesPresentInPostDataResponse(url, "?includeCaseVariables=true&caseInstanceId=" + caseInstance.getId(), caseInstance.getId(), caseVariables);

        assertVariablesPresentInPostDataResponse(url, "?includeCaseVariablesNames=stringVar,dummy&caseInstanceId=" + caseInstance.getId(), caseInstance.getId(),
                Map.of("stringVar", "Azerty"));

        // Without tenant ID, before setting tenant
        assertResultsPresentInDataResponse(url + "?withoutTenantId=true", caseInstance.getId(), caseInstance2.getId());

        // Set tenant on deployment
        org.flowable.cmmn.api.repository.CmmnDeployment deployment = repositoryService.createDeployment().addClasspathResource(
                "org/flowable/cmmn/rest/service/api/repository/oneHumanTaskCase.cmmn").tenantId("myTenant").deploy();

        try {
            startTime.add(Calendar.DAY_OF_YEAR, 1);
            cmmnEngineConfiguration.getClock().setCurrentTime(startTime.getTime());
            CaseInstance caseInstance3 = runtimeService.createCaseInstanceBuilder().caseDefinitionKey("oneHumanTaskCase").tenantId("myTenant").start();

            // Without tenant ID, after setting tenant
            assertResultsPresentInDataResponse(url + "?withoutTenantId=true", caseInstance.getId(), caseInstance2.getId());

            // Tenant id
            assertResultsPresentInDataResponse(url + "?tenantId=myTenant", caseInstance3.getId());
            assertResultsPresentInDataResponse(url + "?tenantId=anotherTenant");

            CloseableHttpResponse response = executeRequest(new HttpGet(SERVER_URL_PREFIX + url + "?caseDefinitionKey=oneHumanTaskCase&sort=startTime"), 200);

            // Check status and size
            assertThat(response.getStatusLine().getStatusCode()).isEqualTo(HttpStatus.SC_OK);
            JsonNode dataNode = objectMapper.readTree(response.getEntity().getContent()).get("data");
            closeResponse(response);
            assertThatJson(dataNode)
                    .when(Option.IGNORING_EXTRA_FIELDS)
                    .isEqualTo("["
                            + "{"
                            + "   id: '" + caseInstance.getId() + "'"
                            + "}, {"
                            + "   id: '" + caseInstance2.getId() + "'"
                            + "}, {"
                            + "    id: '" + caseInstance3.getId() + "'"
                            + "} ]");

        } finally {
            repositoryService.deleteDeployment(deployment.getId(), true);
        }
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
            JsonNode jsonNodeEntry = it.next();
            String id = jsonNodeEntry.get("id").textValue();
            String state = jsonNodeEntry.get("state").textValue();
            assertThat(state).as("state is missing on the historic case instance").isNotEmpty();
            toBeFound.remove(id);
        }
        assertThat(toBeFound).as("Not all process instances have been found in result, missing: " + StringUtils.join(toBeFound, ", ").isEmpty());
    }

    @Test
    @CmmnDeployment(resources = { "org/flowable/cmmn/rest/service/api/repository/twoHumanTaskCase.cmmn" })
    public void testGetCaseInstancesByActivePlanItemDefinitionId() throws Exception {
        CaseInstance caseInstance = runtimeService.createCaseInstanceBuilder().caseDefinitionKey("myCase").start();
        String id = caseInstance.getId();

        // Test without any parameters
        String url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_HISTORIC_CASE_INSTANCES);
        assertResultsPresentInDataResponse(url, id);

        url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_HISTORIC_CASE_INSTANCES) + "?activePlanItemDefinitionId=task1";
        assertResultsPresentInDataResponse(url, id);

        url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_HISTORIC_CASE_INSTANCES) + "?activePlanItemDefinitionId=task2";
        assertResultsPresentInDataResponse(url);

        Task task = taskService.createTaskQuery().caseInstanceId(id).singleResult();
        taskService.complete(task.getId());
        
        url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_HISTORIC_CASE_INSTANCES) + "?activePlanItemDefinitionId=task2";
        assertResultsPresentInDataResponse(url, id);
        
        url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_HISTORIC_CASE_INSTANCES) + "?activePlanItemDefinitionId=task1";
        assertResultsPresentInDataResponse(url);
    }

    @Test
    @CmmnDeployment(resources = { "org/flowable/cmmn/rest/service/api/repository/oneHumanTaskCase.cmmn" })
    public void testBulkDeleteHistoricCaseInstances() throws Exception {
        CaseInstance caseInstance1 = runtimeService.createCaseInstanceBuilder().caseDefinitionKey("oneHumanTaskCase").start();
        CaseInstance caseInstance2 = runtimeService.createCaseInstanceBuilder().caseDefinitionKey("oneHumanTaskCase").start();
        CaseInstance caseInstance3 = runtimeService.createCaseInstanceBuilder().caseDefinitionKey("oneHumanTaskCase").start();

        // Test without any parameters
        String url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_HISTORIC_CASE_INSTANCES) + "/delete";

        Task task = taskService.createTaskQuery().caseInstanceId(caseInstance1.getId()).singleResult();
        taskService.complete(task.getId());

        task = taskService.createTaskQuery().caseInstanceId(caseInstance2.getId()).singleResult();
        taskService.complete(task.getId());

        task = taskService.createTaskQuery().caseInstanceId(caseInstance3.getId()).singleResult();
        taskService.complete(task.getId());

        assertThat(historyService.createHistoricTaskInstanceQuery().caseInstanceId(caseInstance1.getId()).count()).isEqualTo(1);
        assertThat(historyService.createHistoricTaskInstanceQuery().caseInstanceId(caseInstance2.getId()).count()).isEqualTo(1);
        assertThat(historyService.createHistoricTaskInstanceQuery().caseInstanceId(caseInstance3.getId()).count()).isEqualTo(1);

        ObjectNode body = objectMapper.createObjectNode();
        body.put("action", "delete");
        body.putArray("instanceIds").add(caseInstance1.getId()).add(caseInstance2.getId());
        HttpPost httpPost = new HttpPost(SERVER_URL_PREFIX + url);
        httpPost.setEntity(new StringEntity(body.toString()));
        closeResponse(executeRequest(httpPost, HttpStatus.SC_NO_CONTENT));

        assertThat(historyService.createHistoricTaskInstanceQuery().caseInstanceId(caseInstance1.getId()).count()).isZero();
        assertThat(historyService.createHistoricTaskInstanceQuery().caseInstanceId(caseInstance2.getId()).count()).isZero();
        assertThat(historyService.createHistoricTaskInstanceQuery().caseInstanceId(caseInstance3.getId()).count()).isEqualTo(1);

    }

    @Test
    @CmmnDeployment(resources = { "org/flowable/cmmn/rest/service/api/repository/oneHumanTaskCase.cmmn" })
    public void testInvalidBulkDeleteHistoricCaseInstances() throws Exception {
        CaseInstance caseInstance1 = runtimeService.createCaseInstanceBuilder().caseDefinitionKey("oneHumanTaskCase").start();
        CaseInstance caseInstance2 = runtimeService.createCaseInstanceBuilder().caseDefinitionKey("oneHumanTaskCase").start();
        CaseInstance caseInstance3 = runtimeService.createCaseInstanceBuilder().caseDefinitionKey("oneHumanTaskCase").start();

        Task task = taskService.createTaskQuery().caseInstanceId(caseInstance1.getId()).singleResult();
        taskService.complete(task.getId());

        task = taskService.createTaskQuery().caseInstanceId(caseInstance2.getId()).singleResult();
        taskService.complete(task.getId());

        task = taskService.createTaskQuery().caseInstanceId(caseInstance3.getId()).singleResult();
        taskService.complete(task.getId());

        String url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_HISTORIC_CASE_INSTANCES) + "/delete";
        ObjectNode body = objectMapper.createObjectNode();
        body.put("action", "delete");
        body.putArray("instanceIds").add(caseInstance1.getId()).add(caseInstance2.getId()).add("notValidID");
        HttpPost httpPost = new HttpPost(SERVER_URL_PREFIX + url);
        httpPost.setEntity(new StringEntity(body.toString()));
        closeResponse(executeRequest(httpPost, HttpStatus.SC_NO_CONTENT));

        assertThat(historyService.createHistoricTaskInstanceQuery().caseInstanceId(caseInstance1.getId()).count()).isEqualTo(0);
        assertThat(historyService.createHistoricTaskInstanceQuery().caseInstanceId(caseInstance2.getId()).count()).isEqualTo(0);
        assertThat(historyService.createHistoricTaskInstanceQuery().caseInstanceId(caseInstance3.getId()).count()).isEqualTo(1);


        body = objectMapper.createObjectNode();
        body.put("action", "delete");
        httpPost = new HttpPost(SERVER_URL_PREFIX + url);
        httpPost.setEntity(new StringEntity(body.toString()));
        closeResponse(executeRequest(httpPost, HttpStatus.SC_BAD_REQUEST));

        body = objectMapper.createObjectNode();
        body.put("action", "invalidAction");
        body.putArray("instanceIds").add(caseInstance1.getId()).add(caseInstance2.getId());
        httpPost = new HttpPost(SERVER_URL_PREFIX + url);
        httpPost.setEntity(new StringEntity(body.toString()));
        closeResponse(executeRequest(httpPost, HttpStatus.SC_BAD_REQUEST));
    }

    @Test
    @CmmnDeployment(resources = { "org/flowable/cmmn/rest/service/api/repository/oneHumanTaskCase.cmmn" })
    public void testOrderByStartTime() throws IOException {
        Instant startTime = Instant.now().minus(2, ChronoUnit.DAYS);
        cmmnEngineConfiguration.getClock().setCurrentTime(Date.from(startTime));

        runtimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneHumanTaskCase")
                .name("2 days ago case")
                .start();
        cmmnEngineConfiguration.getClock().setCurrentTime(Date.from(startTime.plus(1, ChronoUnit.DAYS)));

        runtimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneHumanTaskCase")
                .name("1 day ago case")
                .start();

        cmmnEngineConfiguration.getClock().setCurrentTime(Date.from(startTime.plus(12, ChronoUnit.HOURS)));

        runtimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneHumanTaskCase")
                .name("1 and a half day ago case")
                .start();

        // Do the actual call
        CloseableHttpResponse response = executeRequest(new HttpGet(SERVER_URL_PREFIX + "cmmn-history/historic-case-instances?sort=startTime"), HttpStatus.SC_OK);

        // Check status and size
        JsonNode responseNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);

        assertThatJson(responseNode)
                .when(Option.IGNORING_EXTRA_FIELDS)
                .isEqualTo("{"
                        + "  sort: 'startTime',"
                        + "  order: 'asc',"
                        + "  data: ["
                        + "    { name: '2 days ago case' },"
                        + "    { name: '1 and a half day ago case' },"
                        + "    { name: '1 day ago case' }"
                        + "  ]"
                        + "}");

        response = executeRequest(new HttpGet(SERVER_URL_PREFIX + "cmmn-history/historic-case-instances?sort=startTime&order=desc"), HttpStatus.SC_OK);

        // Check status and size
        responseNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);

        assertThatJson(responseNode)
                .when(Option.IGNORING_EXTRA_FIELDS)
                .isEqualTo("{"
                        + "  sort: 'startTime',"
                        + "  order: 'desc',"
                        + "  data: ["
                        + "    { name: '1 day ago case' },"
                        + "    { name: '1 and a half day ago case' },"
                        + "    { name: '2 days ago case' }"
                        + "  ]"
                        + "}");
    }

    @Test
    @CmmnDeployment(resources = {
            "org/flowable/cmmn/rest/service/api/repository/oneHumanTaskCase.cmmn",
            "org/flowable/cmmn/rest/service/api/repository/twoHumanTaskCase.cmmn"
    })
    public void testOrderByCaseDefinitionId() throws IOException {
        CaseInstance case1 = runtimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneHumanTaskCase")
                .name("One Human Task Case")
                .start();

        CaseInstance case2 = runtimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("myCase")
                .name("Two Human Task Case")
                .start();

        String caseNameWithHigherCaseDefId;
        String caseNameWithLowerCaseDefId;

        if (case1.getCaseDefinitionId().compareTo(case2.getCaseDefinitionId()) > 0) {
            // Case Definition Id 1 has higher id
            caseNameWithHigherCaseDefId = case1.getName();
            caseNameWithLowerCaseDefId = case2.getName();
        } else {
            caseNameWithHigherCaseDefId = case2.getName();
            caseNameWithLowerCaseDefId = case1.getName();
        }

        // Do the actual call
        CloseableHttpResponse response = executeRequest(new HttpGet(SERVER_URL_PREFIX + "cmmn-history/historic-case-instances?sort=caseDefinitionId"), HttpStatus.SC_OK);

        // Check status and size
        JsonNode responseNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);

        assertThatJson(responseNode)
                .when(Option.IGNORING_EXTRA_FIELDS)
                .isEqualTo("{"
                        + "  sort: 'caseDefinitionId',"
                        + "  order: 'asc',"
                        + "  data: ["
                        + "    { name: '" + caseNameWithLowerCaseDefId + "' },"
                        + "    { name: '" + caseNameWithHigherCaseDefId + "' }"
                        + "  ]"
                        + "}");

        response = executeRequest(new HttpGet(SERVER_URL_PREFIX + "cmmn-history/historic-case-instances?sort=caseDefinitionId&order=desc"), HttpStatus.SC_OK);

        // Check status and size
        responseNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);

        assertThatJson(responseNode)
                .when(Option.IGNORING_EXTRA_FIELDS)
                .isEqualTo("{"
                        + "  sort: 'caseDefinitionId',"
                        + "  order: 'desc',"
                        + "  data: ["
                        + "    { name: '" + caseNameWithHigherCaseDefId + "' },"
                        + "    { name: '" + caseNameWithLowerCaseDefId + "' }"
                        + "  ]"
                        + "}");
    }

    @Test
    @CmmnDeployment(resources = { "org/flowable/cmmn/rest/service/api/repository/oneHumanTaskCase.cmmn" })
    public void testWithoutParentIDWithoutCallbackId() throws IOException {
        CaseInstance parentInstance = runtimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneHumanTaskCase")
                .name("withoutBoth")
                .start();

        runtimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneHumanTaskCase")
                .name("withParentId").parentId(parentInstance.getId())
                .start();

        runtimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneHumanTaskCase")
                .name("withCallBackId").callbackId("testID")
                .start();

        // Do the actual call
        CloseableHttpResponse response = executeRequest(
                new HttpGet(SERVER_URL_PREFIX + "cmmn-history/historic-case-instances?withoutCaseInstanceParentId=true"), HttpStatus.SC_OK);

        JsonNode responseNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        assertThatJson(responseNode)
                .when(Option.IGNORING_EXTRA_FIELDS)
                .isEqualTo("{"
                        + "  data: ["
                        + "    { name: 'withoutBoth' },"
                        + "    { name: 'withCallBackId' }"
                        + "  ]"
                        + "}");

        response = executeRequest(new HttpGet(SERVER_URL_PREFIX + "cmmn-history/historic-case-instances?withoutCaseInstanceCallbackId=true"),
                HttpStatus.SC_OK);

        responseNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        assertThatJson(responseNode)
                .when(Option.IGNORING_EXTRA_FIELDS)
                .isEqualTo("{"
                        + "  data: ["
                        + "    { name: 'withoutBoth' },"
                        + "    { name: 'withParentId' }"
                        + "  ]"
                        + "}");

        response = executeRequest(
                new HttpGet(SERVER_URL_PREFIX + "cmmn-history/historic-case-instances?withoutCaseInstanceParentId=false&withoutCaseInstanceCallbackId=false"),
                HttpStatus.SC_OK);
        
        responseNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        assertThatJson(responseNode)
                .when(Option.IGNORING_EXTRA_FIELDS)
                .isEqualTo("{"
                        + "  data: ["
                        + "    { name: 'withoutBoth' },"
                        + "    { name: 'withParentId' },"
                        + "    { name: 'withCallBackId' }"
                        + "  ]"
                        + "}");

    }

    @Test
    @CmmnDeployment(resources = {
            "org/flowable/cmmn/rest/service/api/runtime/simpleCaseWithCaseTasks.cmmn",
            "org/flowable/cmmn/rest/service/api/runtime/simpleInnerCaseWithCaseTasks.cmmn",
            "org/flowable/cmmn/rest/service/api/runtime/simpleInnerCaseWithHumanTasksAndCaseTask.cmmn",
            "org/flowable/cmmn/rest/service/api/runtime/oneTaskCase.cmmn"
    })
    public void testQueryByRootScopeId() throws IOException {
        runtimeService.createCaseInstanceBuilder().caseDefinitionKey("simpleTestCaseWithCaseTasks").start();
        CaseInstance caseInstance = runtimeService.createCaseInstanceBuilder().caseDefinitionKey("simpleTestCaseWithCaseTasks").start();

        PlanItemInstance oneTaskCasePlanItemInstance = runtimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId())
                .planItemDefinitionId("caseTaskOneTaskCase").singleResult();

        PlanItemInstance caseTaskSimpleCaseWithCaseTasksPlanItemInstance = runtimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId())
                .planItemDefinitionId("caseTaskSimpleCaseWithCaseTasks").singleResult();

        PlanItemInstance caseTaskWithHumanTasksPlanItemInstance = runtimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseTaskSimpleCaseWithCaseTasksPlanItemInstance.getReferenceId())
                .planItemDefinitionId("caseTaskCaseWithHumanTasks").singleResult();

        PlanItemInstance oneTaskCase2PlanItemInstance = runtimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseTaskWithHumanTasksPlanItemInstance.getReferenceId())
                .planItemDefinitionId("caseTaskOneTaskCase").singleResult();

        taskService.createTaskQuery().list().forEach(task -> taskService.complete(task.getId()));

        String url =
                SERVER_URL_PREFIX + CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_HISTORIC_CASE_INSTANCES) + "?rootScopeId=" + caseInstance.getId();
        CloseableHttpResponse response = executeRequest(new HttpGet(url), HttpStatus.SC_OK);

        JsonNode responseNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        assertThatJson(responseNode)
                .when(Option.IGNORING_EXTRA_FIELDS, Option.IGNORING_ARRAY_ORDER)
                .isEqualTo("{"
                        + "  data: ["
                        + "    { id: '" + oneTaskCasePlanItemInstance.getReferenceId() + "' },"
                        + "    { id: '" + caseTaskWithHumanTasksPlanItemInstance.getReferenceId() + "' },"
                        + "    { id: '" + caseTaskSimpleCaseWithCaseTasksPlanItemInstance.getReferenceId() + "' },"
                        + "    { id: '" + oneTaskCase2PlanItemInstance.getReferenceId() + "' }"
                        + "  ]"
                        + "}");
    }

    @Test
    @CmmnDeployment(resources = {
            "org/flowable/cmmn/rest/service/api/runtime/simpleCaseWithCaseTasks.cmmn",
            "org/flowable/cmmn/rest/service/api/runtime/simpleInnerCaseWithCaseTasks.cmmn",
            "org/flowable/cmmn/rest/service/api/runtime/simpleInnerCaseWithHumanTasksAndCaseTask.cmmn",
            "org/flowable/cmmn/rest/service/api/runtime/oneTaskCase.cmmn"
    })
    public void testQueryByParentScopeId() throws IOException {
        runtimeService.createCaseInstanceBuilder().caseDefinitionKey("simpleTestCaseWithCaseTasks").start();
        CaseInstance caseInstance = runtimeService.createCaseInstanceBuilder().caseDefinitionKey("simpleTestCaseWithCaseTasks").start();

        PlanItemInstance caseTaskSimpleCaseWithCaseTasksPlanItemInstance = runtimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId())
                .planItemDefinitionId("caseTaskSimpleCaseWithCaseTasks").singleResult();

        PlanItemInstance caseTaskWithHumanTasksPlanItemInstance = runtimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseTaskSimpleCaseWithCaseTasksPlanItemInstance.getReferenceId())
                .planItemDefinitionId("caseTaskCaseWithHumanTasks").singleResult();

        PlanItemInstance oneTaskCase2PlanItemInstance = runtimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseTaskWithHumanTasksPlanItemInstance.getReferenceId())
                .planItemDefinitionId("caseTaskOneTaskCase").singleResult();

        taskService.createTaskQuery().list().forEach(task -> taskService.complete(task.getId()));

        String url = SERVER_URL_PREFIX + CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_HISTORIC_CASE_INSTANCES) + "?parentScopeId="
                + caseTaskWithHumanTasksPlanItemInstance.getReferenceId();
        CloseableHttpResponse response = executeRequest(new HttpGet(url), HttpStatus.SC_OK);

        JsonNode responseNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        assertThatJson(responseNode)
                .when(Option.IGNORING_EXTRA_FIELDS, Option.IGNORING_ARRAY_ORDER)
                .isEqualTo("{"
                        + "  data: ["
                        + "    { id: '" + oneTaskCase2PlanItemInstance.getReferenceId() + "' }"
                        + "  ]"
                        + "}");

    }

    @Test
    @CmmnDeployment(resources = {
            "org/flowable/cmmn/rest/service/api/runtime/simpleCaseWithCaseTasks.cmmn",
            "org/flowable/cmmn/rest/service/api/runtime/simpleInnerCaseWithCaseTasks.cmmn",
            "org/flowable/cmmn/rest/service/api/runtime/simpleInnerCaseWithHumanTasksAndCaseTask.cmmn",
            "org/flowable/cmmn/rest/service/api/runtime/oneTaskCase.cmmn"
    })
    public void testQueryByParentCaseInstanceId() throws IOException {
        runtimeService.createCaseInstanceBuilder().caseDefinitionKey("simpleTestCaseWithCaseTasks").start();
        CaseInstance caseInstance = runtimeService.createCaseInstanceBuilder().caseDefinitionKey("simpleTestCaseWithCaseTasks").start();

        PlanItemInstance oneTaskCasePlanItemInstance = runtimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId())
                .planItemDefinitionId("caseTaskOneTaskCase").singleResult();

        PlanItemInstance caseTaskSimpleCaseWithCaseTasksPlanItemInstance = runtimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId())
                .planItemDefinitionId("caseTaskSimpleCaseWithCaseTasks").singleResult();

        PlanItemInstance caseTaskWithHumanTasksPlanItemInstance = runtimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseTaskSimpleCaseWithCaseTasksPlanItemInstance.getReferenceId())
                .planItemDefinitionId("caseTaskCaseWithHumanTasks").singleResult();

        PlanItemInstance oneTaskCase2PlanItemInstance = runtimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseTaskWithHumanTasksPlanItemInstance.getReferenceId())
                .planItemDefinitionId("caseTaskOneTaskCase").singleResult();

        taskService.createTaskQuery().list().forEach(task -> taskService.complete(task.getId()));

        String url = SERVER_URL_PREFIX + CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_HISTORIC_CASE_INSTANCES) + "?parentCaseInstanceId=" + caseInstance.getId();
        CloseableHttpResponse response = executeRequest(new HttpGet(url), HttpStatus.SC_OK);

        JsonNode responseNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        assertThatJson(responseNode)
                .when(Option.IGNORING_EXTRA_FIELDS, Option.IGNORING_ARRAY_ORDER)
                .isEqualTo("{"
                        + "  data: ["
                        + "    { id: '" + oneTaskCasePlanItemInstance.getReferenceId() + "' },"
                        + "    { id: '" + caseTaskSimpleCaseWithCaseTasksPlanItemInstance.getReferenceId() + "' }"
                        + "  ]"
                        + "}");
        
        url = SERVER_URL_PREFIX + CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_HISTORIC_CASE_INSTANCES) + "?parentCaseInstanceId=" + oneTaskCasePlanItemInstance.getReferenceId();
        response = executeRequest(new HttpGet(url), HttpStatus.SC_OK);

        responseNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        assertThatJson(responseNode)
                .when(Option.IGNORING_EXTRA_FIELDS, Option.IGNORING_ARRAY_ORDER)
                .isEqualTo("{"
                        + "  data: []"
                        + "}");
        
        url = SERVER_URL_PREFIX + CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_HISTORIC_CASE_INSTANCES) + "?parentCaseInstanceId=" + caseTaskSimpleCaseWithCaseTasksPlanItemInstance.getReferenceId();
        response = executeRequest(new HttpGet(url), HttpStatus.SC_OK);

        responseNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        assertThatJson(responseNode)
                .when(Option.IGNORING_EXTRA_FIELDS, Option.IGNORING_ARRAY_ORDER)
                .isEqualTo("{"
                        + "  data: ["
                        + "    { id: '" + oneTaskCase2PlanItemInstance.getCaseInstanceId() + "' }"
                        + "  ]"
                        + "}");
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/rest/service/api/runtime/oneTaskCase.cmmn")
    public void testQueryByCaseInstanceIds() throws IOException {
        CaseInstance caseInstance1 = runtimeService.createCaseInstanceBuilder().caseDefinitionKey("oneTaskCase").start();
        CaseInstance caseInstance2 = runtimeService.createCaseInstanceBuilder().caseDefinitionKey("oneTaskCase").start();
        CaseInstance caseInstance3 = runtimeService.createCaseInstanceBuilder().caseDefinitionKey("oneTaskCase").start();

        String url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_HISTORIC_CASE_INSTANCES);
        assertResultsPresentInDataResponse(url, caseInstance1.getId(), caseInstance2.getId(), caseInstance3.getId());

        url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_HISTORIC_CASE_INSTANCES) + "?caseInstanceIds=" + caseInstance1.getId() + "," + caseInstance3.getId();
        assertResultsPresentInDataResponse(url, caseInstance1.getId(), caseInstance3.getId());

        url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_HISTORIC_CASE_INSTANCES) + "?caseInstanceIds=dummy1,dummy2";
        assertEmptyResultsPresentInDataResponse(url);
    }

    private void assertVariablesPresentInPostDataResponse(String url, String queryParameters, String caseInstanceId, Map<String, Object> expectedVariables)
            throws IOException {

        HttpGet httpPost = new HttpGet(SERVER_URL_PREFIX + url + queryParameters);
        CloseableHttpResponse response = executeRequest(httpPost, HttpStatus.SC_OK);

        // Check status and size
        JsonNode dataNode = objectMapper.readTree(response.getEntity().getContent()).get("data");
        closeResponse(response);
        assertThat(dataNode).hasSize(1);
        JsonNode valueNode = dataNode.get(0);
        assertThat(valueNode.get("id").asText()).isEqualTo(caseInstanceId);

        // Check expected variables
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
}
