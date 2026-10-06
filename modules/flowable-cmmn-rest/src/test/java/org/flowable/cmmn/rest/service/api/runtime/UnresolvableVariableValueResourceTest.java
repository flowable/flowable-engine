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

import static org.assertj.core.api.Assertions.assertThat;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.rest.service.BaseSpringRestTestCase;
import org.flowable.cmmn.rest.service.api.CmmnRestUrls;
import org.flowable.common.engine.impl.persistence.entity.ByteArrayEntity;
import org.flowable.task.api.Task;
import org.flowable.variable.service.impl.persistence.entity.HistoricVariableInstanceEntity;
import org.flowable.variable.service.impl.persistence.entity.VariableInstanceEntity;
import org.junit.jupiter.api.Test;

import tools.jackson.databind.JsonNode;

/**
 * A variable whose value cannot be resolved is returned without a value and with a valueUnresolvable marker, instead of failing the whole response.
 */
public class UnresolvableVariableValueResourceTest extends BaseSpringRestTestCase {

    @Test
    @CmmnDeployment(resources = { "org/flowable/cmmn/rest/service/api/repository/oneHumanTaskCase.cmmn" })
    public void testVariableResources() throws Exception {
        CaseInstance caseInstance = startCaseWithUnresolvableOrderVariable();

        JsonNode responseNode = getJson(CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_CASE_INSTANCE_VARIABLE_COLLECTION, caseInstance.getId()));
        assertUnresolvableVariable(responseNode, "customer", "order");

        responseNode = getJson(CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_CASE_INSTANCE_VARIABLE, caseInstance.getId(), "order"));
        assertThat(responseNode.path("name").asString()).isEqualTo("order");
        assertThat(responseNode.hasNonNull("value")).isFalse();
        assertThat(responseNode.path("valueUnresolvable").asBoolean()).isTrue();
        assertThat(responseNode.path("type").asString()).isEqualTo("serializable");

        Task task = taskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        responseNode = getJson(CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_TASK_VARIABLES_COLLECTION, task.getId()));
        assertUnresolvableVariable(responseNode, "customer", "order");

        responseNode = getJson(CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_TASK_VARIABLE, task.getId(), "order"));
        assertThat(responseNode.path("name").asString()).isEqualTo("order");
        assertThat(responseNode.hasNonNull("value")).isFalse();
        assertThat(responseNode.path("valueUnresolvable").asBoolean()).isTrue();
        assertThat(responseNode.path("type").asString()).isEqualTo("serializable");
    }

    @Test
    @CmmnDeployment(resources = { "org/flowable/cmmn/rest/service/api/repository/oneHumanTaskCase.cmmn" })
    public void testQueryResources() throws Exception {
        CaseInstance caseInstance = startCaseWithUnresolvableOrderVariable();
        String caseInstanceId = caseInstance.getId();

        JsonNode responseNode = getJson(CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_CASE_INSTANCE_COLLECTION)
                + "?id=" + caseInstanceId + "&includeCaseVariables=true");
        assertUnresolvableVariable(responseNode.path("data").path(0).path("variables"), "customer", "order");

        responseNode = getJson(CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_HISTORIC_CASE_INSTANCES)
                + "?caseInstanceId=" + caseInstanceId + "&includeCaseVariables=true");
        assertUnresolvableVariable(responseNode.path("data").path(0).path("variables"), "customer", "order");

        responseNode = getJson(CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_VARIABLE_INSTANCES) + "?caseInstanceId=" + caseInstanceId);
        assertUnresolvableVariableInstance(responseNode);

        responseNode = getJson(CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_HISTORIC_VARIABLE_INSTANCES) + "?caseInstanceId=" + caseInstanceId);
        assertUnresolvableVariableInstance(responseNode);
    }

    @Test
    @CmmnDeployment(resources = { "org/flowable/cmmn/rest/service/api/repository/oneHumanTaskCase.cmmn" })
    public void testPlanItemInstanceQueryIncludingLocalVariables() throws Exception {
        CaseInstance caseInstance = runtimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneHumanTaskCase")
                .variable("caseData", "case data".getBytes(StandardCharsets.UTF_8))
                .start();
        String planItemInstanceId = runtimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).singleResult().getId();
        Map<String, Object> variables = new HashMap<>();
        variables.put("localCustomer", "Kermit");
        variables.put("localOrder", new TestSerializableVariable(1));
        runtimeService.setLocalVariables(planItemInstanceId, variables);
        corruptSerializedValue(caseInstance.getId(), "localOrder");

        JsonNode responseNode = getJson(CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_PLAN_ITEM_INSTANCE_COLLECTION)
                + "?caseInstanceId=" + caseInstance.getId() + "&includeLocalVariables=true");
        assertUnresolvableVariable(responseNode.path("data").path(0).path("localVariables"), "localCustomer", "localOrder");


        // The binary data of a case variable is not returned as a local variable of the plan item instance
        closeResponse(executeRequest(new HttpGet(SERVER_URL_PREFIX + CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_PLAN_ITEM_INSTANCE_VARIABLE_DATA,
                planItemInstanceId, "caseData")), HttpStatus.SC_NOT_FOUND));
    }

    @Test
    @CmmnDeployment(resources = { "org/flowable/cmmn/rest/service/api/repository/oneHumanTaskCase.cmmn" })
    public void testTaskQueriesIncludingLocalVariables() throws Exception {
        CaseInstance caseInstance = runtimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneHumanTaskCase")
                .start();
        Task task = taskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        Map<String, Object> variables = new HashMap<>();
        variables.put("taskCustomer", "Kermit");
        variables.put("taskOrder", new TestSerializableVariable(1));
        taskService.setVariablesLocal(task.getId(), variables);
        corruptSerializedValue(caseInstance.getId(), "taskOrder");

        JsonNode responseNode = getJson(CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_TASK_COLLECTION)
                + "?caseInstanceId=" + caseInstance.getId() + "&includeTaskLocalVariables=true");
        assertUnresolvableVariable(responseNode.path("data").path(0).path("variables"), "taskCustomer", "taskOrder");

        responseNode = getJson(CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_HISTORIC_TASK_INSTANCES)
                + "?caseInstanceId=" + caseInstance.getId() + "&includeTaskLocalVariables=true");
        assertUnresolvableVariable(responseNode.path("data").path(0).path("variables"), "taskCustomer", "taskOrder");
    }

    @Test
    @CmmnDeployment(resources = { "org/flowable/cmmn/rest/service/api/repository/oneHumanTaskCase.cmmn" })
    public void testDataResources() throws Exception {
        CaseInstance caseInstance = runtimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneHumanTaskCase")
                .variable("order", new TestSerializableVariable(1))
                .variable("document", "Kermit".getBytes(StandardCharsets.UTF_8))
                .start();
        String caseInstanceId = caseInstance.getId();
        String planItemInstanceId = runtimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstanceId).singleResult().getId();
        String taskId = taskService.createTaskQuery().caseInstanceId(caseInstanceId).singleResult().getId();

        Map<String, Object> variables = new HashMap<>();
        variables.put("localOrder", new TestSerializableVariable(1));
        variables.put("localDocument", "Kermit".getBytes(StandardCharsets.UTF_8));
        runtimeService.setLocalVariables(planItemInstanceId, variables);

        variables.clear();
        variables.put("taskOrder", new TestSerializableVariable(1));
        variables.put("taskDocument", "Kermit".getBytes(StandardCharsets.UTF_8));
        taskService.setVariablesLocal(taskId, variables);

        corruptSerializedValue(caseInstanceId, "order");
        corruptSerializedValue(caseInstanceId, "localOrder");
        corruptSerializedValue(caseInstanceId, "taskOrder");
        String variableInstanceId = runtimeService.createVariableInstanceQuery().caseInstanceId(caseInstanceId).variableName("order")
                .excludeVariableInitialization().singleResult().getId();
        String historicVariableInstanceId = historyService.createHistoricVariableInstanceQuery().caseInstanceId(caseInstanceId).variableName("order")
                .excludeVariableInitialization().singleResult().getId();

        // The binary value of a variable whose value cannot be resolved cannot be returned
        assertDataStatus(CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_CASE_INSTANCE_VARIABLE_DATA, caseInstanceId, "order"),
                HttpStatus.SC_INTERNAL_SERVER_ERROR);
        assertDataStatus(CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_PLAN_ITEM_INSTANCE_VARIABLE_DATA, planItemInstanceId, "localOrder"),
                HttpStatus.SC_INTERNAL_SERVER_ERROR);
        assertDataStatus(CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_TASK_VARIABLE_DATA, taskId, "order"), HttpStatus.SC_INTERNAL_SERVER_ERROR);
        assertDataStatus(CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_TASK_VARIABLE_DATA, taskId, "taskOrder"), HttpStatus.SC_INTERNAL_SERVER_ERROR);
        assertDataStatus(CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_VARIABLE_INSTANCE_DATA, variableInstanceId),
                HttpStatus.SC_INTERNAL_SERVER_ERROR);
        assertDataStatus(CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_HISTORIC_VARIABLE_INSTANCE_DATA, historicVariableInstanceId),
                HttpStatus.SC_INTERNAL_SERVER_ERROR);
        assertDataStatus(CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_HISTORIC_CASE_INSTANCE_VARIABLE_DATA, caseInstanceId, "order"),
                HttpStatus.SC_INTERNAL_SERVER_ERROR);
        assertDataStatus(CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_HISTORIC_TASK_INSTANCE_VARIABLE_DATA, taskId, "taskOrder"),
                HttpStatus.SC_INTERNAL_SERVER_ERROR);

        // The binary value of another variable is still returned
        assertDataStatus(CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_CASE_INSTANCE_VARIABLE_DATA, caseInstanceId, "document"), HttpStatus.SC_OK);
        assertDataStatus(CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_PLAN_ITEM_INSTANCE_VARIABLE_DATA, planItemInstanceId, "localDocument"),
                HttpStatus.SC_OK);
        assertDataStatus(CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_TASK_VARIABLE_DATA, taskId, "taskDocument"), HttpStatus.SC_OK);
        assertDataStatus(CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_HISTORIC_CASE_INSTANCE_VARIABLE_DATA, caseInstanceId, "document"),
                HttpStatus.SC_OK);
        assertDataStatus(CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_HISTORIC_TASK_INSTANCE_VARIABLE_DATA, taskId, "taskDocument"),
                HttpStatus.SC_OK);
    }

    protected void assertDataStatus(String url, int expectedStatus) {
        closeResponse(executeRequest(new HttpGet(SERVER_URL_PREFIX + url), expectedStatus));
    }

    protected JsonNode getJson(String url) throws Exception {
        CloseableHttpResponse response = executeRequest(new HttpGet(SERVER_URL_PREFIX + url), HttpStatus.SC_OK);
        JsonNode responseNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        return responseNode;
    }

    protected void assertUnresolvableVariableInstance(JsonNode variableInstancesNode) {
        Map<String, JsonNode> variables = new HashMap<>();
        variableInstancesNode.path("data").forEach(variableInstanceNode -> variables.put(variableInstanceNode.path("variable").path("name").asString(),
                variableInstanceNode.path("variable")));
        assertUnresolvableVariable(variables, "customer", "order");
    }

    protected void assertUnresolvableVariable(JsonNode variablesNode, String resolvableName, String unresolvableName) {
        Map<String, JsonNode> variables = new HashMap<>();
        variablesNode.forEach(variableNode -> variables.put(variableNode.path("name").asString(), variableNode));
        assertUnresolvableVariable(variables, resolvableName, unresolvableName);
    }

    protected void assertUnresolvableVariable(Map<String, JsonNode> variables, String resolvableName, String unresolvableName) {
        assertThat(variables).containsOnlyKeys(resolvableName, unresolvableName);
        assertThat(variables.get(resolvableName).path("value").asString()).isEqualTo("Kermit");
        assertThat(variables.get(resolvableName).has("valueUnresolvable")).isFalse();
        assertThat(variables.get(unresolvableName).hasNonNull("value")).isFalse();
        assertThat(variables.get(unresolvableName).path("valueUnresolvable").asBoolean()).isTrue();
        assertThat(variables.get(unresolvableName).path("type").asString()).isEqualTo("serializable");
    }

    protected CaseInstance startCaseWithUnresolvableOrderVariable() {
        CaseInstance caseInstance = runtimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneHumanTaskCase")
                .variable("customer", "Kermit")
                .variable("order", new TestSerializableVariable(1))
                .start();
        corruptSerializedValue(caseInstance.getId(), "order");
        return caseInstance;
    }

    protected void corruptSerializedValue(String caseInstanceId, String variableName) {
        cmmnEngineConfiguration.getCommandExecutor().execute(commandContext -> {
            VariableInstanceEntity variableInstance = (VariableInstanceEntity) runtimeService.createVariableInstanceQuery()
                    .caseInstanceId(caseInstanceId)
                    .variableName(variableName)
                    .excludeVariableInitialization()
                    .singleResult();
            corruptByteArray(variableInstance.getByteArrayRef().getId());

            HistoricVariableInstanceEntity historicVariableInstance = (HistoricVariableInstanceEntity) historyService.createHistoricVariableInstanceQuery()
                    .caseInstanceId(caseInstanceId)
                    .variableName(variableName)
                    .excludeVariableInitialization()
                    .singleResult();
            corruptByteArray(historicVariableInstance.getByteArrayRef().getId());
            return null;
        });
    }

    protected void corruptByteArray(String byteArrayId) {
        ByteArrayEntity byteArray = cmmnEngineConfiguration.getByteArrayEntityManager().findById(byteArrayId);
        byteArray.setBytes("not a serialized object".getBytes(StandardCharsets.UTF_8));
    }

    public static class TestSerializableVariable implements Serializable {

        private static final long serialVersionUID = 1L;

        protected int number;

        public TestSerializableVariable(int number) {
            this.number = number;
        }

        public int getNumber() {
            return number;
        }
    }
}
