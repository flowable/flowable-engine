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

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.api.runtime.PlanItemDefinitionType;
import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.rest.service.BaseSpringRestTestCase;
import org.flowable.cmmn.rest.service.api.CmmnRestUrls;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import net.javacrumbs.jsonunit.core.Option;

/**
 * Test for all REST-operations related to the plan item instance query resource.
 *
 * @author Tijs Rademakers
 * @author Joram Barrez
 */
public class PlanItemInstanceQueryResourceTest extends BaseSpringRestTestCase {

    @Test
    @CmmnDeployment(resources = { "org/flowable/cmmn/rest/service/api/runtime/PlanItemInstanceQueryResourceTest.testQueryPlanItemInstances.cmmn" })
    public void testQueryPlanItemInstances() throws Exception {
        CaseInstance caseInstance = runtimeService.createCaseInstanceBuilder().caseDefinitionKey("testPlanItemInstanceQuery").start();

        // Type
        List<PlanItemInstance> planItemInstanceList = runtimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .planItemDefinitionType(PlanItemDefinitionType.HUMAN_TASK)
                .list();
        assertThat(planItemInstanceList).hasSize(2);

        String url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_PLAN_ITEM_INSTANCE_QUERY);
        ObjectNode requestNode = objectMapper.createObjectNode();
        requestNode.put("caseInstanceId", caseInstance.getId());
        requestNode.put("planItemDefinitionType", "humantask");
        assertResultsPresentInPostDataResponse(url, requestNode, planItemInstanceList.get(0).getId(), planItemInstanceList.get(1).getId());

        // Multiple types
        planItemInstanceList = runtimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .planItemDefinitionTypes(Arrays.asList(PlanItemDefinitionType.HUMAN_TASK, PlanItemDefinitionType.STAGE))
                .list();
        assertThat(planItemInstanceList).hasSize(4);

        url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_PLAN_ITEM_INSTANCE_QUERY);
        requestNode = objectMapper.createObjectNode();
        requestNode.put("caseInstanceId", caseInstance.getId());
        requestNode.putArray("planItemDefinitionTypes").add("humantask").add("stage");
        assertResultsPresentInPostDataResponse(url, requestNode,
                planItemInstanceList.get(0).getId(),
                planItemInstanceList.get(1).getId(),
                planItemInstanceList.get(2).getId(),
                planItemInstanceList.get(3).getId());

        requestNode = objectMapper.createObjectNode();
        requestNode.putArray("caseInstanceIds").add(caseInstance.getId()).add("someOtherIds");
        assertResultsPresentInPostDataResponse(url, requestNode,
                planItemInstanceList.get(0).getId(),
                planItemInstanceList.get(1).getId(),
                planItemInstanceList.get(2).getId(),
                planItemInstanceList.get(3).getId());
    }

    /**
     * Test querying plan item instance based on variables. POST query/planitem-instances
     */
    @Test
    @CmmnDeployment(resources = { "org/flowable/cmmn/rest/service/api/repository/oneHumanTaskCase.cmmn" })
    public void testQueryPlanItemInstancesWithVariables() throws Exception {
        HashMap<String, Object> caseVariables = new HashMap<>();
        caseVariables.put("stringVar", "Azerty");
        caseVariables.put("intVar", 67890);
        caseVariables.put("booleanVar", false);

        CaseInstance caseInstance = runtimeService.createCaseInstanceBuilder().caseDefinitionKey("oneHumanTaskCase").variables(caseVariables).start();

        PlanItemInstance planItem = runtimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).singleResult();

        String url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_PLAN_ITEM_INSTANCE_QUERY);

        // Process variables
        ObjectNode requestNode = objectMapper.createObjectNode();
        ArrayNode variableArray = objectMapper.createArrayNode();
        ObjectNode variableNode = objectMapper.createObjectNode();
        variableArray.add(variableNode);
        requestNode.set("caseInstanceVariables", variableArray);

        // String equals
        variableNode.put("name", "stringVar");
        variableNode.put("value", "Azerty");
        variableNode.put("operation", "equals");
        assertResultsPresentInPostDataResponse(url, requestNode, planItem.getId());

        // Integer equals
        variableNode.removeAll();
        variableNode.put("name", "intVar");
        variableNode.put("value", 67890);
        variableNode.put("operation", "equals");
        assertResultsPresentInPostDataResponse(url, requestNode, planItem.getId());

        // Boolean equals
        variableNode.removeAll();
        variableNode.put("name", "booleanVar");
        variableNode.put("value", false);
        variableNode.put("operation", "equals");
        assertResultsPresentInPostDataResponse(url, requestNode, planItem.getId());

        // String not equals
        variableNode.removeAll();
        variableNode.put("name", "stringVar");
        variableNode.put("value", "ghijkl");
        variableNode.put("operation", "notEquals");
        assertResultsPresentInPostDataResponse(url, requestNode, planItem.getId());

        // Integer not equals
        variableNode.removeAll();
        variableNode.put("name", "intVar");
        variableNode.put("value", 45678);
        variableNode.put("operation", "notEquals");
        assertResultsPresentInPostDataResponse(url, requestNode, planItem.getId());

        // Boolean not equals
        variableNode.removeAll();
        variableNode.put("name", "booleanVar");
        variableNode.put("value", true);
        variableNode.put("operation", "notEquals");
        assertResultsPresentInPostDataResponse(url, requestNode, planItem.getId());

        // String equals ignore case
        variableNode.removeAll();
        variableNode.put("name", "stringVar");
        variableNode.put("value", "azeRTY");
        variableNode.put("operation", "equalsIgnoreCase");
        assertResultsPresentInPostDataResponse(url, requestNode, planItem.getId());

        // String not equals ignore case
        variableNode.removeAll();
        variableNode.put("name", "stringVar");
        variableNode.put("value", "HIJKLm");
        variableNode.put("operation", "notEqualsIgnoreCase");
        assertResultsPresentInPostDataResponse(url, requestNode, planItem.getId());

        // String equals without value
        variableNode.removeAll();
        variableNode.put("value", "Azerty");
        variableNode.put("operation", "equals");
        assertResultsPresentInPostDataResponse(url, requestNode, planItem.getId());

        // String equals with non existing value
        variableNode.removeAll();
        variableNode.put("value", "Azerty2");
        variableNode.put("operation", "equals");
        assertResultsPresentInPostDataResponse(url, requestNode);
    }

    /**
     * Test querying plan item instance and return local variables. POST query/planitem-instances
     */
    @Test
    @CmmnDeployment(resources = { "org/flowable/cmmn/rest/service/api/repository/oneHumanTaskCase.cmmn" })
    public void testQueryPlanItemInstancesWithLocalVariables() throws Exception {
        HashMap<String, Object> caseVariables = new HashMap<>();
        caseVariables.put("stringVar", "Azerty");
        caseVariables.put("intVar", 67890);
        caseVariables.put("booleanVar", false);

        CaseInstance caseInstance = runtimeService.createCaseInstanceBuilder().caseDefinitionKey("oneHumanTaskCase").variables(caseVariables).start();

        PlanItemInstance planItem = runtimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).singleResult();
        runtimeService.setLocalVariable(planItem.getId(), "someLocalVariable", "someLocalValue");

        String url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_PLAN_ITEM_INSTANCE_QUERY);

        ObjectNode requestNode = objectMapper.createObjectNode();
        requestNode.put("id", planItem.getId());
        HttpPost post = new HttpPost(SERVER_URL_PREFIX + url);
        post.setEntity(new StringEntity(requestNode.toString()));
        CloseableHttpResponse response = executeRequest(post, 200);
        JsonNode dataNode = objectMapper.readTree(response.getEntity().getContent()).get("data");
        closeResponse(response);

        assertThatJson(dataNode).when(Option.IGNORING_EXTRA_FIELDS, Option.IGNORING_ARRAY_ORDER).isEqualTo("["
                + "     {"
                + "         id : '" + planItem.getId() + "',"
                + "         caseInstanceId : '" + caseInstance.getId() + "',"
                + "         localVariables:["
                + "         ]"
                + "     }"
                + "]");

        requestNode.put("includeLocalVariables", true);
        post = new HttpPost(SERVER_URL_PREFIX + url);
        post.setEntity(new StringEntity(requestNode.toString()));
        response = executeRequest(post, 200);
        dataNode = objectMapper.readTree(response.getEntity().getContent()).get("data");
        closeResponse(response);

        assertThatJson(dataNode).when(Option.IGNORING_EXTRA_FIELDS, Option.IGNORING_ARRAY_ORDER).isEqualTo("["
                + "     {"
                + "         id : '" + planItem.getId() + "',"
                + "         caseInstanceId : '" + caseInstance.getId() + "',"
                + "         localVariables:[{"
                + "             name:'someLocalVariable',"
                + "             value:'someLocalValue',"
                + "             scope:'local'"
                + "         }]"
                + "     }"
                + "]");
    }
}
