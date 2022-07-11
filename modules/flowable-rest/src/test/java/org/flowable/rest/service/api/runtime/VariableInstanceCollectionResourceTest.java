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

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.rest.service.BaseSpringRestTestCase;
import org.flowable.rest.service.api.RestUrls;
import org.flowable.task.api.Task;
import org.flowable.variable.api.persistence.entity.VariableInstance;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Test for REST-operation related to the variable instance query resource.
 * 
 * @author Tijs Rademakers
 */
public class VariableInstanceCollectionResourceTest extends BaseSpringRestTestCase {

    /**
     * Test querying historic variable instance. GET history/historic-variable-instances
     */
    @Test
    @Deployment
    public void testQueryVariableInstances() throws Exception {
        HashMap<String, Object> processVariables = new HashMap<>();
        processVariables.put("stringVar", "Azerty");
        processVariables.put("intVar", 67890);
        processVariables.put("booleanVar", false);

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess", processVariables);
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        taskService.complete(task.getId());
        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        taskService.setVariableLocal(task.getId(), "taskVariable", "test");

        ProcessInstance processInstance2 = runtimeService.startProcessInstanceByKey("oneTaskProcess", processVariables);

        String url = RestUrls.createRelativeResourceUrl(RestUrls.URL_VARIABLE_INSTANCES);

        assertResultsPresentInDataResponse(url + "?variableName=stringVar", 2, "stringVar", "Azerty");

        assertResultsPresentInDataResponse(url + "?variableName=booleanVar", 2, "booleanVar", false);

        assertResultsPresentInDataResponse(url + "?variableName=booleanVar2", 0, null, null);

        assertResultsPresentInDataResponse(url + "?processInstanceId=" + processInstance.getId(), 4, "taskVariable", "test");

        assertResultsPresentInDataResponse(url + "?processInstanceId=" + processInstance.getId() + "&excludeTaskVariables=true", 3, "intVar", 67890);

        assertResultsPresentInDataResponse(url + "?processInstanceId=" + processInstance2.getId(), 3, "stringVar", "Azerty");

        assertResultsPresentInDataResponse(url + "?taskId=" + task.getId(), 1, "taskVariable", "test");

        assertResultsPresentInDataResponse(url + "?taskId=" + task.getId() + "&variableName=booleanVar", 0, null, null);

        assertResultsPresentInDataResponse(url + "?variableNameLike=" + encode("%Var"), 6, "stringVar", "Azerty");

        assertResultsPresentInDataResponse(url + "?variableNameLike=" + encode("%Var2"), 0, null, null);
    }

    /**
     * Test querying variable instance without local variables.
     */
    @Test
    @Deployment(resources = "org/flowable/rest/service/api/runtime/VariableInstanceCollectionResourceTest.testQueryVariableInstances.bpmn20.xml")
    public void testQueryExcludeLocalVariable() throws Exception {

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");

        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        taskService.setVariable(task.getId(), "var1", "test1");
        taskService.setVariableLocal(task.getId(), "varLocal1", "test2");

        waitForJobExecutorToProcessAllJobs(7000, 100);

        Execution execution = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().singleResult();
        runtimeService.setVariableLocal(execution.getId(), "varLocal2", "test3");

        List<VariableInstance> vars = runtimeService.createVariableInstanceQuery().processInstanceId(processInstance.getId()).excludeLocalVariables().list();

        assertThat(vars.size()).isEqualTo(1);
        assertThat(vars.get(0).getValue()).isEqualTo("test1");

        taskService.complete(task.getId());

        waitForJobExecutorToProcessAllJobs(7000, 100);

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        taskService.setVariable(task.getId(), "var3", "test4");
        taskService.setVariableLocal(task.getId(), "varLocal3", "test5");

        waitForJobExecutorToProcessAllJobs(7000, 100);

        String url = RestUrls.createRelativeResourceUrl(RestUrls.URL_VARIABLE_INSTANCES);

        assertResultsPresentInDataResponse(url + "?processInstanceId=" + processInstance.getId(), 4, "varLocal3", "test5");
        assertResultsPresentInDataResponse(url + "?processInstanceId=" + processInstance.getId() + "&excludeLocalVariables=true", 2, "var3", "test4");

    }

    protected void assertResultsPresentInDataResponse(String url, int numberOfResultsExpected, String variableName, Object variableValue) throws JsonProcessingException, IOException {

        // Do the actual call
        CloseableHttpResponse response = executeRequest(new HttpGet(SERVER_URL_PREFIX + url), HttpStatus.SC_OK);

        // Check status and size
        JsonNode dataNode = objectMapper.readTree(response.getEntity().getContent()).get("data");
        closeResponse(response);
        assertThat(dataNode).hasSize(numberOfResultsExpected);

        // Check presence of ID's
        if (variableName != null) {
            boolean variableFound = false;
            Iterator<JsonNode> it = dataNode.iterator();
            while (it.hasNext()) {
                JsonNode dataElementNode = it.next();
                JsonNode variableNode = dataElementNode.get("variable");
                String name = variableNode.get("name").textValue();
                if (variableName.equals(name)) {
                    variableFound = true;
                    if (variableValue instanceof Boolean) {
                        assertThat((boolean) (Boolean) variableValue).as("Variable value is not equal").isEqualTo(variableNode.get("value").asBoolean());
                    } else if (variableValue instanceof Integer) {
                        assertThat((int) (Integer) variableValue).as("Variable value is not equal").isEqualTo(variableNode.get("value").asInt());
                    } else {
                        assertThat((String) variableValue).as("Variable value is not equal").isEqualTo(variableNode.get("value").asText());
                    }
                }
            }
            assertThat(variableFound).as("Variable " + variableName + " is missing").isTrue();
        }
    }
}
