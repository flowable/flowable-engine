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

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.flowable.common.engine.impl.persistence.entity.ByteArrayEntity;
import org.flowable.engine.history.HistoricDetail;
import org.flowable.engine.impl.persistence.entity.HistoricDetailVariableInstanceUpdateEntity;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.rest.service.BaseSpringRestTestCase;
import org.flowable.rest.service.api.RestUrls;
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
    @Deployment(resources = { "org/flowable/rest/service/api/runtime/ProcessInstanceVariablesCollectionResourceTest.testProcess.bpmn20.xml" })
    public void testVariableResources() throws Exception {
        ProcessInstance processInstance = startProcessWithUnresolvableOrderVariable();

        JsonNode responseNode = getJson(RestUrls.createRelativeResourceUrl(RestUrls.URL_PROCESS_INSTANCE_VARIABLE_COLLECTION, processInstance.getId()));
        assertUnresolvableOrderVariable(responseNode);

        responseNode = getJson(RestUrls.createRelativeResourceUrl(RestUrls.URL_PROCESS_INSTANCE_VARIABLE, processInstance.getId(), "order"));
        assertThat(responseNode.path("name").asString()).isEqualTo("order");
        assertThat(responseNode.hasNonNull("value")).isFalse();
        assertThat(responseNode.path("valueUnresolvable").asBoolean()).isTrue();
        assertThat(responseNode.path("type").asString()).isEqualTo("serializable");

        responseNode = getJson(RestUrls.createRelativeResourceUrl(RestUrls.URL_EXECUTION_VARIABLE_COLLECTION, processInstance.getId()));
        assertUnresolvableOrderVariable(responseNode);

        responseNode = getJson(RestUrls.createRelativeResourceUrl(RestUrls.URL_EXECUTION_VARIABLE, processInstance.getId(), "order"));
        assertThat(responseNode.path("name").asString()).isEqualTo("order");
        assertThat(responseNode.hasNonNull("value")).isFalse();
        assertThat(responseNode.path("valueUnresolvable").asBoolean()).isTrue();
        assertThat(responseNode.path("type").asString()).isEqualTo("serializable");

        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        responseNode = getJson(RestUrls.createRelativeResourceUrl(RestUrls.URL_TASK_VARIABLES_COLLECTION, task.getId()));
        assertUnresolvableOrderVariable(responseNode);

        responseNode = getJson(RestUrls.createRelativeResourceUrl(RestUrls.URL_TASK_VARIABLE, task.getId(), "order"));
        assertThat(responseNode.path("name").asString()).isEqualTo("order");
        assertThat(responseNode.hasNonNull("value")).isFalse();
        assertThat(responseNode.path("valueUnresolvable").asBoolean()).isTrue();
        assertThat(responseNode.path("type").asString()).isEqualTo("serializable");
    }

    @Test
    @Deployment(resources = { "org/flowable/rest/service/api/runtime/ProcessInstanceVariablesCollectionResourceTest.testProcess.bpmn20.xml" })
    public void testQueryResources() throws Exception {
        ProcessInstance processInstance = startProcessWithUnresolvableOrderVariable();
        String processInstanceId = processInstance.getId();

        JsonNode responseNode = getJson(RestUrls.createRelativeResourceUrl(RestUrls.URL_PROCESS_INSTANCE_COLLECTION)
                + "?id=" + processInstanceId + "&includeProcessVariables=true");
        assertUnresolvableOrderVariable(responseNode.path("data").path(0).path("variables"));

        responseNode = getJson(RestUrls.createRelativeResourceUrl(RestUrls.URL_TASK_COLLECTION)
                + "?processInstanceId=" + processInstanceId + "&includeProcessVariables=true");
        assertUnresolvableOrderVariable(responseNode.path("data").path(0).path("variables"));

        responseNode = getJson(RestUrls.createRelativeResourceUrl(RestUrls.URL_VARIABLE_INSTANCES) + "?processInstanceId=" + processInstanceId);
        assertUnresolvableOrderVariableInstance(responseNode);

        responseNode = getJson(RestUrls.createRelativeResourceUrl(RestUrls.URL_HISTORIC_PROCESS_INSTANCES)
                + "?processInstanceId=" + processInstanceId + "&includeProcessVariables=true");
        assertUnresolvableOrderVariable(responseNode.path("data").path(0).path("variables"));

        responseNode = getJson(RestUrls.createRelativeResourceUrl(RestUrls.URL_HISTORIC_TASK_INSTANCES)
                + "?processInstanceId=" + processInstanceId + "&includeProcessVariables=true");
        assertUnresolvableOrderVariable(responseNode.path("data").path(0).path("variables"));

        responseNode = getJson(RestUrls.createRelativeResourceUrl(RestUrls.URL_HISTORIC_VARIABLE_INSTANCES) + "?processInstanceId=" + processInstanceId);
        assertUnresolvableOrderVariableInstance(responseNode);
    }

    @Test
    @Deployment(resources = { "org/flowable/rest/service/api/runtime/ProcessInstanceVariablesCollectionResourceTest.testProcess.bpmn20.xml" })
    public void testDataResources() throws Exception {
        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("oneTaskProcess")
                .variable("order", new OrderVariable(1))
                .variable("document", "Kermit".getBytes(StandardCharsets.UTF_8))
                .start();
        String processInstanceId = processInstance.getId();
        corruptStoredValue(processInstanceId, "order");
        String historicDetailId = corruptHistoricDetailValue(processInstanceId, "order");
        String taskId = taskService.createTaskQuery().processInstanceId(processInstanceId).singleResult().getId();
        String variableInstanceId = runtimeService.createVariableInstanceQuery().processInstanceId(processInstanceId).variableName("order")
                .excludeVariableInitialization().singleResult().getId();
        String historicVariableInstanceId = historyService.createHistoricVariableInstanceQuery().processInstanceId(processInstanceId).variableName("order")
                .excludeVariableInitialization().singleResult().getId();

        // The binary value of a variable whose value cannot be resolved cannot be returned
        assertDataStatus(RestUrls.createRelativeResourceUrl(RestUrls.URL_PROCESS_INSTANCE_VARIABLE_DATA, processInstanceId, "order"),
                HttpStatus.SC_INTERNAL_SERVER_ERROR);
        assertDataStatus(RestUrls.createRelativeResourceUrl(RestUrls.URL_EXECUTION_VARIABLE_DATA, processInstanceId, "order"),
                HttpStatus.SC_INTERNAL_SERVER_ERROR);
        assertDataStatus(RestUrls.createRelativeResourceUrl(RestUrls.URL_TASK_VARIABLE_DATA, taskId, "order"), HttpStatus.SC_INTERNAL_SERVER_ERROR);
        assertDataStatus(RestUrls.createRelativeResourceUrl(RestUrls.URL_VARIABLE_INSTANCE_DATA, variableInstanceId), HttpStatus.SC_INTERNAL_SERVER_ERROR);
        assertDataStatus(RestUrls.createRelativeResourceUrl(RestUrls.URL_HISTORIC_VARIABLE_INSTANCE_DATA, historicVariableInstanceId),
                HttpStatus.SC_INTERNAL_SERVER_ERROR);
        assertDataStatus(RestUrls.createRelativeResourceUrl(RestUrls.URL_HISTORIC_PROCESS_INSTANCE_VARIABLE_DATA, processInstanceId, "order"),
                HttpStatus.SC_INTERNAL_SERVER_ERROR);
        assertDataStatus(RestUrls.createRelativeResourceUrl(RestUrls.URL_HISTORIC_TASK_INSTANCE_VARIABLE_DATA, taskId, "order"),
                HttpStatus.SC_INTERNAL_SERVER_ERROR);
        assertDataStatus(RestUrls.createRelativeResourceUrl(RestUrls.URL_HISTORIC_DETAIL_VARIABLE_DATA, historicDetailId), HttpStatus.SC_INTERNAL_SERVER_ERROR);

        // The binary value of another variable is still returned
        assertDataStatus(RestUrls.createRelativeResourceUrl(RestUrls.URL_PROCESS_INSTANCE_VARIABLE_DATA, processInstanceId, "document"), HttpStatus.SC_OK);
        assertDataStatus(RestUrls.createRelativeResourceUrl(RestUrls.URL_EXECUTION_VARIABLE_DATA, processInstanceId, "document"), HttpStatus.SC_OK);
        assertDataStatus(RestUrls.createRelativeResourceUrl(RestUrls.URL_TASK_VARIABLE_DATA, taskId, "document"), HttpStatus.SC_OK);
        assertDataStatus(RestUrls.createRelativeResourceUrl(RestUrls.URL_HISTORIC_PROCESS_INSTANCE_VARIABLE_DATA, processInstanceId, "document"),
                HttpStatus.SC_OK);
        assertDataStatus(RestUrls.createRelativeResourceUrl(RestUrls.URL_HISTORIC_TASK_INSTANCE_VARIABLE_DATA, taskId, "document"), HttpStatus.SC_OK);
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

    protected void assertUnresolvableOrderVariableInstance(JsonNode variableInstancesNode) {
        Map<String, JsonNode> variables = new HashMap<>();
        variableInstancesNode.path("data").forEach(variableInstanceNode -> variables.put(variableInstanceNode.path("variable").path("name").asString(),
                variableInstanceNode.path("variable")));
        assertUnresolvableOrderVariable(variables);
    }

    protected void assertUnresolvableOrderVariable(JsonNode variablesNode) {
        Map<String, JsonNode> variables = new HashMap<>();
        variablesNode.forEach(variableNode -> variables.put(variableNode.path("name").asString(), variableNode));
        assertUnresolvableOrderVariable(variables);
    }

    protected void assertUnresolvableOrderVariable(Map<String, JsonNode> variables) {
        assertThat(variables).containsOnlyKeys("customer", "order");
        assertThat(variables.get("customer").path("value").asString()).isEqualTo("Kermit");
        assertThat(variables.get("customer").has("valueUnresolvable")).isFalse();
        assertThat(variables.get("order").hasNonNull("value")).isFalse();
        assertThat(variables.get("order").path("valueUnresolvable").asBoolean()).isTrue();
        assertThat(variables.get("order").path("type").asString()).isEqualTo("serializable");
    }

    protected ProcessInstance startProcessWithUnresolvableOrderVariable() {
        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("oneTaskProcess")
                .variable("customer", "Kermit")
                .variable("order", new OrderVariable(1))
                .start();

        corruptStoredValue(processInstance.getId(), "order");
        return processInstance;
    }

    protected void corruptStoredValue(String processInstanceId, String variableName) {
        managementService.executeCommand(commandContext -> {
            VariableInstanceEntity variableInstance = (VariableInstanceEntity) runtimeService.createVariableInstanceQuery()
                    .processInstanceId(processInstanceId)
                    .variableName(variableName)
                    .excludeVariableInitialization()
                    .singleResult();
            corruptByteArray(variableInstance.getByteArrayRef().getId());

            HistoricVariableInstanceEntity historicVariableInstance = (HistoricVariableInstanceEntity) historyService.createHistoricVariableInstanceQuery()
                    .processInstanceId(processInstanceId)
                    .variableName(variableName)
                    .excludeVariableInitialization()
                    .singleResult();
            corruptByteArray(historicVariableInstance.getByteArrayRef().getId());
            return null;
        });
    }

    protected String corruptHistoricDetailValue(String processInstanceId, String variableName) {
        return managementService.executeCommand(commandContext -> {
            for (HistoricDetail historicDetail : historyService.createHistoricDetailQuery().processInstanceId(processInstanceId).variableUpdates().list()) {
                HistoricDetailVariableInstanceUpdateEntity variableUpdate = (HistoricDetailVariableInstanceUpdateEntity) historicDetail;
                if (variableName.equals(variableUpdate.getVariableName())) {
                    corruptByteArray(variableUpdate.getByteArrayRef().getId());
                    return variableUpdate.getId();
                }
            }
            throw new IllegalStateException("No historic detail found for variable " + variableName);
        });
    }

    protected void corruptByteArray(String byteArrayId) {
        ByteArrayEntity byteArray = processEngineConfiguration.getByteArrayEntityManager().findById(byteArrayId);
        byteArray.setBytes("not a serialized object".getBytes(StandardCharsets.UTF_8));
    }

    public static class OrderVariable implements Serializable {

        private static final long serialVersionUID = 1L;

        protected int number;

        public OrderVariable(int number) {
            this.number = number;
        }

        public int getNumber() {
            return number;
        }
    }
}
