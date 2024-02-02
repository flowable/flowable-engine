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

package org.flowable.engine.test.json;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.RandomStringUtils;
import org.flowable.common.engine.api.delegate.Expression;
import org.flowable.common.engine.impl.el.ExpressionManager;
import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.common.engine.impl.javax.el.PropertyNotFoundException;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.flowable.engine.impl.test.HistoryTestHelper;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.task.api.Task;
import org.flowable.variable.api.history.HistoricVariableInstance;
import org.flowable.variable.api.persistence.entity.VariableInstance;
import org.flowable.variable.service.impl.persistence.entity.HistoricVariableInstanceEntity;
import org.flowable.variable.service.impl.persistence.entity.VariableInstanceEntity;
import org.flowable.variable.service.impl.types.JsonType;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * @author Tijs Rademakers
 * @author Tim Stephenson
 * @author Filip Hrisafov
 */
public class JsonTest extends PluggableFlowableTestCase {

    public static final String MY_JSON_OBJ = "myJsonObj";
    public static final String BIG_JSON_OBJ = "bigJsonObj";

    protected ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @Deployment(resources = "org/flowable/engine/test/json/JsonTest.testUpdateJsonValueDuringExecution.bpmn20.xml")
    public void testCreateAndUpdateJsonValueDuringExecution() {
        JavaDelegate javaDelegate = new JavaDelegate() {

            @Override
            public void execute(DelegateExecution execution) {

                ExpressionManager expressionManager = CommandContextUtil.getProcessEngineConfiguration().getExpressionManager();
                execution.setVariable("customer", objectMapper.createObjectNode());

                Expression expression = expressionManager.createExpression("${customer.name}");
                expression.setValue("Kermit", execution);

                expression = expressionManager.createExpression("${customer.address}");
                expression.setValue(objectMapper.createObjectNode(), execution);

                expression = expressionManager.createExpression("${customer.address.street}");
                expression.setValue("Sesame Street", execution);
            }
        };
        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("updateJsonValue")
                .transientVariable("jsonBean", javaDelegate)
                .start();

        VariableInstance customerVarInstance = runtimeService.getVariableInstance(processInstance.getId(), "customer");
        assertThat(customerVarInstance.getTypeName()).isEqualTo(JsonType.TYPE_NAME);

        JsonNode customerVar = (JsonNode) customerVarInstance.getValue();
        assertThatJson(customerVar)
                .isEqualTo("{"
                        + "  name: 'Kermit',"
                        + "  address: {"
                        + "    street: 'Sesame Street'"
                        + "  }"
                        + "}");

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            HistoricVariableInstance customerHistoricVarInstance = historyService.createHistoricVariableInstanceQuery()
                    .processInstanceId(processInstance.getId())
                    .variableName("customer")
                    .singleResult();
            assertThat(customerHistoricVarInstance.getVariableTypeName()).isEqualTo(JsonType.TYPE_NAME);

            customerVar = (JsonNode) customerHistoricVarInstance.getValue();
            assertThatJson(customerVar)
                    .isEqualTo("{"
                            + "  name: 'Kermit',"
                            + "  address: {"
                            + "    street: 'Sesame Street'"
                            + "  }"
                            + "}");
        }
    }

    @Test
    @Deployment
    public void testCreateJsonArrayDuringExecution() {
        JavaDelegate javaDelegate = new JavaDelegate() {

            @Override
            public void execute(DelegateExecution execution) {
                ArrayNode testArrayNode = objectMapper.createArrayNode();
                testArrayNode.addObject();
                execution.setVariable("jsonArrayTest", testArrayNode);
                execution.setVariable("${jsonArrayTest[0].name}", "test");

                execution.setVariable("jsonObjectTest", objectMapper.createObjectNode());
                execution.setVariable("${jsonObjectTest.name}", "anotherTest");
            }
        };

        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("createJsonArray")
                .transientVariable("jsonBean", javaDelegate)
                .start();

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            List<HistoricVariableInstance> varInstances = historyService.createHistoricVariableInstanceQuery()
                    .processInstanceId(processInstance.getId())
                    .orderByVariableName()
                    .asc()
                    .list();
            assertThat(varInstances).hasSize(2);
            HistoricVariableInstance varInstance = varInstances.get(0);
            assertThatJson(varInstance.getValue())
                    .isEqualTo("[{"
                            + "  name: 'test'"
                            + "}]");

            varInstance = varInstances.get(1);
            assertThatJson(varInstance.getValue())
                    .isEqualTo("{"
                            + "  name: 'anotherTest'"
                            + "}");
        }
    }

    @Test
    @Deployment
    public void testCreateAndUpdateJsonValueDuringExecutionWithoutWaitState() {
        JavaDelegate javaDelegate = new JavaDelegate() {

            @Override
            public void execute(DelegateExecution execution) {

                ExpressionManager expressionManager = CommandContextUtil.getProcessEngineConfiguration().getExpressionManager();
                execution.setVariable("customer", objectMapper.createObjectNode());

                Expression expression = expressionManager.createExpression("${customer.name}");
                expression.setValue("Kermit", execution);

                expression = expressionManager.createExpression("${customer.address}");
                expression.setValue(objectMapper.createObjectNode(), execution);

                expression = expressionManager.createExpression("${customer.address.street}");
                expression.setValue("Sesame Street", execution);
            }
        };
        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("updateJsonValue")
                .transientVariable("jsonBean", javaDelegate)
                .start();

        JsonNode customerVar = (JsonNode) processInstance.getProcessVariables().get("customer");
        assertThatJson(customerVar)
                .isEqualTo("{"
                        + "  name: 'Kermit',"
                        + "  address: {"
                        + "    street: 'Sesame Street'"
                        + "  }"
                        + "}");

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            HistoricVariableInstance customerHistoricVarInstance = historyService.createHistoricVariableInstanceQuery()
                    .processInstanceId(processInstance.getId())
                    .variableName("customer")
                    .singleResult();
            assertThat(customerHistoricVarInstance.getVariableTypeName()).isEqualTo(JsonType.TYPE_NAME);

            customerVar = (JsonNode) customerHistoricVarInstance.getValue();
            assertThatJson(customerVar)
                    .isEqualTo("{"
                            + "  name: 'Kermit',"
                            + "  address: {"
                            + "    street: 'Sesame Street'"
                            + "  }"
                            + "}");
        }
    }

    @Test
    @Deployment
    public void testUpdateJsonValueDuringExecution() {
        ObjectNode customer = objectMapper.createObjectNode();
        customer.put("name", "Kermit");
        JavaDelegate javaDelegate = new JavaDelegate() {

            @Override
            public void execute(DelegateExecution execution) {
                execution.getVariable("customer", ObjectNode.class)
                        .putObject("address")
                        .put("street", "Sesame Street");
            }
        };
        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("updateJsonValue")
                .variable("customer", customer)
                .transientVariable("jsonBean", javaDelegate)
                .start();

        assertThatJson(customer)
                .isEqualTo("{"
                        + "  name: 'Kermit',"
                        + "  address: {"
                        + "    street: 'Sesame Street'"
                        + "  }"
                        + "}");

        VariableInstance customerVarInstance = runtimeService.getVariableInstance(processInstance.getId(), "customer");
        assertThat(customerVarInstance.getTypeName()).isEqualTo(JsonType.TYPE_NAME);

        JsonNode customerVar = (JsonNode) customerVarInstance.getValue();
        assertThatJson(customerVar)
                .isEqualTo("{"
                        + "  name: 'Kermit',"
                        + "  address: {"
                        + "    street: 'Sesame Street'"
                        + "  }"
                        + "}");

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            HistoricVariableInstance customerHistoricVarInstance = historyService.createHistoricVariableInstanceQuery()
                    .processInstanceId(processInstance.getId())
                    .variableName("customer")
                    .singleResult();
            assertThat(customerHistoricVarInstance.getVariableTypeName()).isEqualTo(JsonType.TYPE_NAME);

            customerVar = (JsonNode) customerHistoricVarInstance.getValue();
            assertThatJson(customerVar)
                    .isEqualTo("{"
                            + "  name: 'Kermit',"
                            + "  address: {"
                            + "    street: 'Sesame Street'"
                            + "  }"
                            + "}");
        }
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/json/JsonTest.testUpdateJsonValueDuringExecution.bpmn20.xml")
    public void testUpdateFromSmallToLongJsonValue() {
        // Set customer.street to 'Sesame Street'
        ObjectNode customer = objectMapper.createObjectNode();
        customer.put("street", "Sesame Street");
        JavaDelegate javaDelegate = execution -> {
        };
        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("updateJsonValue")
                .variable("customer", customer)
                .transientVariable("jsonBean", javaDelegate)
                .start();

        VariableInstance customerVariableInstance = runtimeService.getVariableInstance(processInstance.getId(), "customer");
        assertThat(((VariableInstanceEntity) customerVariableInstance).getByteArrayRef()).isNull();

        Object customerActual = customerVariableInstance.getValue();
        assertThat(customerActual).isInstanceOf(ObjectNode.class);
        assertThatJson(customerActual)
                .inPath("street")
                .isEqualTo("Sesame Street");

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            HistoricVariableInstance customerHistoricVariableInstance = historyService.createHistoricVariableInstanceQuery()
                    .processInstanceId(processInstance.getId())
                    .variableName("customer")
                    .singleResult();
            assertThatJson(customerHistoricVariableInstance.getValue())
                    .inPath("street")
                    .isEqualTo("Sesame Street");
            assertThat(((HistoricVariableInstanceEntity) customerHistoricVariableInstance).getByteArrayRef()).isNull();
        }

        // Set customer.street to long value
        String randomLongValue = RandomStringUtils.randomAlphanumeric(processEngineConfiguration.getMaxLengthString() + 1);
        customer.put("street", randomLongValue);
        runtimeService.setVariable(processInstance.getId(), "customer", customer);

        customerVariableInstance = runtimeService.getVariableInstance(processInstance.getId(), "customer");
        customerActual = customerVariableInstance.getValue();
        assertThat(customerActual).isInstanceOf(ObjectNode.class);
        assertThatJson(customerActual)
                .inPath("street")
                .isEqualTo(randomLongValue);
        assertThat(((VariableInstanceEntity) customerVariableInstance).getByteArrayRef()).isNotNull();

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            HistoricVariableInstance customerHistoricVariableInstance = historyService.createHistoricVariableInstanceQuery()
                    .processInstanceId(processInstance.getId())
                    .variableName("customer")
                    .singleResult();
            assertThatJson(customerHistoricVariableInstance.getValue())
                    .inPath("street")
                    .isEqualTo(randomLongValue);
            assertThat(((HistoricVariableInstanceEntity) customerHistoricVariableInstance).getByteArrayRef()).isNotNull();
        }

        // Set customer.street back to a small value
        customer.put("street", "Sesame Street 2");
        runtimeService.setVariable(processInstance.getId(), "customer", customer);

        customerVariableInstance = runtimeService.getVariableInstance(processInstance.getId(), "customer");
        customerActual = customerVariableInstance.getValue();
        assertThat(customerActual).isInstanceOf(ObjectNode.class);
        assertThatJson(customerActual)
                .inPath("street")
                .isEqualTo("Sesame Street 2");
        assertThat(((VariableInstanceEntity) customerVariableInstance).getByteArrayRef()).isNull();

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            HistoricVariableInstance customerHistoricVariableInstance = historyService.createHistoricVariableInstanceQuery()
                    .processInstanceId(processInstance.getId())
                    .variableName("customer").singleResult();
            assertThatJson(customerHistoricVariableInstance.getValue())
                    .inPath("street")
                    .isEqualTo("Sesame Street 2");
            assertThat(((HistoricVariableInstanceEntity) customerHistoricVariableInstance).getByteArrayRef()).isNull();
        }
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/json/JsonTest.testUpdateJsonValueDuringExecution.bpmn20.xml")
    public void testUpdateJsonValueToLongValueDuringExecution() {
        ObjectNode customer = objectMapper.createObjectNode();
        customer.put("name", "Kermit");
        String randomLongStreetName = RandomStringUtils.randomAlphanumeric(processEngineConfiguration.getMaxLengthString() + 1);
        JavaDelegate javaDelegate = new JavaDelegate() {

            @Override
            public void execute(DelegateExecution execution) {
                execution.getVariable("customer", ObjectNode.class)
                        .putObject("address")
                        .put("street", randomLongStreetName);
            }
        };
        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("updateJsonValue")
                .variable("customer", customer)
                .transientVariable("jsonBean", javaDelegate)
                .start();

        assertThatJson(customer)
                .isEqualTo("{"
                        + "  name: 'Kermit',"
                        + "  address: {"
                        + "    street: '" + randomLongStreetName + "'"
                        + "  }"
                        + "}");

        VariableInstance customerVarInstance = runtimeService.getVariableInstance(processInstance.getId(), "customer");
        assertThat(customerVarInstance.getTypeName()).isEqualTo(JsonType.TYPE_NAME);

        JsonNode customerVar = (JsonNode) managementService.executeCommand(commandContext -> customerVarInstance.getValue());
        assertThatJson(customerVar)
                .isEqualTo("{"
                        + "  name: 'Kermit',"
                        + "  address: {"
                        + "    street: '" + randomLongStreetName + "'"
                        + "  }"
                        + "}");

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            HistoricVariableInstance customerHistoricVarInstance = historyService.createHistoricVariableInstanceQuery()
                    .processInstanceId(processInstance.getId())
                    .variableName("customer")
                    .singleResult();
            assertThat(customerHistoricVarInstance.getVariableTypeName()).isEqualTo(JsonType.TYPE_NAME);

            customerVar = (JsonNode) managementService.executeCommand(commandContext -> customerHistoricVarInstance.getValue());
            assertThatJson(customerVar)
                    .isEqualTo("{"
                            + "  name: 'Kermit',"
                            + "  address: {"
                            + "    street: '" + randomLongStreetName + "'"
                            + "  }"
                            + "}");
        }
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/json/JsonTest.testUpdateJsonValueDuringExecution.bpmn20.xml")
    public void testUpdateLongJsonValueDuringExecution() {
        ObjectNode customer = objectMapper.createObjectNode();

        String randomLongStreetName = RandomStringUtils.randomAlphanumeric(processEngineConfiguration.getMaxLengthString() + 1);
        customer.put("name", "Kermit");
        customer.putObject("address")
                .put("address", randomLongStreetName);
        JavaDelegate javaDelegate = new JavaDelegate() {

            @Override
            public void execute(DelegateExecution execution) {
                execution.getVariable("customer", ObjectNode.class)
                        .remove("address");
            }
        };
        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("updateJsonValue")
                .variable("customer", customer)
                .transientVariable("jsonBean", javaDelegate)
                .start();

        assertThatJson(customer)
                .isEqualTo("{"
                        + "  name: 'Kermit'"
                        + "}");

        VariableInstance customerVarInstance = runtimeService.getVariableInstance(processInstance.getId(), "customer");
        assertThat(customerVarInstance.getTypeName()).isEqualTo(JsonType.TYPE_NAME);

        JsonNode customerVar = (JsonNode) managementService.executeCommand(commandContext -> customerVarInstance.getValue());
        assertThatJson(customerVar)
                .isEqualTo("{"
                        + "  name: 'Kermit'"
                        + "}");

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            HistoricVariableInstance customerHistoricVarInstance = historyService.createHistoricVariableInstanceQuery()
                    .processInstanceId(processInstance.getId())
                    .variableName("customer")
                    .singleResult();
            assertThat(customerHistoricVarInstance.getVariableTypeName()).isEqualTo(JsonType.TYPE_NAME);

            customerVar = (JsonNode) managementService.executeCommand(commandContext -> customerHistoricVarInstance.getValue());
            assertThatJson(customerVar)
                    .isEqualTo("{"
                            + "  name: 'Kermit'"
                            + "}");
        }
    }

    @Test
    @Deployment
    public void testJsonObjectAvailable() {
        Map<String, Object> vars = new HashMap<>();

        ObjectNode varNode = objectMapper.createObjectNode();
        varNode.put("var", "myValue");
        vars.put(MY_JSON_OBJ, varNode);
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("testJsonAvailableProcess", vars);

        // Check JSON has been parsed as expected
        ObjectNode value = (ObjectNode) runtimeService.getVariable(processInstance.getId(), MY_JSON_OBJ);
        assertThat(value).isNotNull();
        assertThatJson(value)
                .isEqualTo("{ var: 'myValue' }");

        ObjectNode var2Node = objectMapper.createObjectNode();
        var2Node.put("var", "myValue");
        var2Node.put("var2", "myOtherValue");
        runtimeService.setVariable(processInstance.getId(), MY_JSON_OBJ, var2Node);

        // Check JSON has been updated as expected
        value = (ObjectNode) runtimeService.getVariable(processInstance.getId(), MY_JSON_OBJ);
        assertThat(value).isNotNull();
        assertThatJson(value)
                .isEqualTo("{ "
                        + "   var: 'myValue',"
                        + "   var2: 'myOtherValue'"
                        + "}");

        org.flowable.task.api.Task task = taskService.createTaskQuery().active().singleResult();
        assertThat(task).isNotNull();
        ObjectNode var3Node = objectMapper.createObjectNode();
        var3Node.put("var", "myValue");
        var3Node.put("var2", "myOtherValue");
        var3Node.put("var3", "myThirdValue");

        vars = new HashMap<>();
        vars.put(MY_JSON_OBJ, var3Node);
        vars.put(BIG_JSON_OBJ, createBigJsonObject());
        taskService.complete(task.getId(), vars);
        value = (ObjectNode) runtimeService.getVariable(processInstance.getId(), MY_JSON_OBJ);
        assertThat(value).isNotNull();
        assertThatJson(value)
                .isEqualTo("{ "
                        + "   var: 'myValue',"
                        + "   var2: 'myOtherValue',"
                        + "   var3: 'myThirdValue'"
                        + "}");

        value = (ObjectNode) runtimeService.getVariable(processInstance.getId(), BIG_JSON_OBJ);
        assertThat(value).hasToString(createBigJsonObject().toString());

        VariableInstance variableInstance = runtimeService.getVariableInstance(processInstance.getId(), BIG_JSON_OBJ);
        assertThat(variableInstance).isNotNull();
        assertThat(variableInstance.getValue()).hasToString(createBigJsonObject().toString());

        task = taskService.createTaskQuery().active().singleResult();
        assertThat(task).isNotNull();
        assertThat(task.getTaskDefinitionKey()).isEqualTo("userTaskSuccess");

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
            List<HistoricVariableInstance> historicVariableInstances = historyService.createHistoricVariableInstanceQuery()
                    .processInstanceId(processInstance.getProcessInstanceId()).orderByVariableName().asc().list();
            assertThat(historicVariableInstances).hasSize(2);

            assertThat(historicVariableInstances.get(0).getVariableName()).isEqualTo(BIG_JSON_OBJ);
            value = (ObjectNode) historicVariableInstances.get(0).getValue();
            assertThat(value).hasToString(createBigJsonObject().toString());

            assertThat(historicVariableInstances.get(1).getVariableName()).isEqualTo(MY_JSON_OBJ);
            value = (ObjectNode) historicVariableInstances.get(1).getValue();
            assertThat(value).isNotNull();
            assertThatJson(value)
                    .isEqualTo("{ "
                            + "   var: 'myValue',"
                            + "   var2: 'myOtherValue',"
                            + "   var3: 'myThirdValue'"
                            + "}");

            HistoricVariableInstance historicVariableInstance = historyService.createHistoricVariableInstanceQuery()
                    .processInstanceId(processInstance.getId())
                    .variableName(BIG_JSON_OBJ)
                    .singleResult();

            assertThat(historicVariableInstance).isNotNull();
            assertThat(historicVariableInstance.getValue()).hasToString(createBigJsonObject().toString());
        }

        // It should be possible do remove a json variable
        runtimeService.removeVariable(processInstance.getId(), MY_JSON_OBJ);
        assertThat(runtimeService.getVariable(processInstance.getId(), MY_JSON_OBJ)).isNull();

        // It should be possible do remove a longJson variable
        runtimeService.removeVariable(processInstance.getId(), BIG_JSON_OBJ);
        assertThat(runtimeService.getVariable(processInstance.getId(), BIG_JSON_OBJ)).isNull();
    }

    @Test
    @Deployment
    public void testDirectJsonPropertyAccess() {
        Map<String, Object> vars = new HashMap<>();

        ObjectNode varNode = objectMapper.createObjectNode();
        varNode.put("var", "myValue");
        vars.put("myJsonObj", varNode);
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("testJsonAvailableProcess", vars);

        // Check JSON has been parsed as expected
        ObjectNode value = (ObjectNode) runtimeService.getVariable(processInstance.getId(), "myJsonObj");
        assertThat(value).isNotNull();
        assertThatJson(value)
                .isEqualTo("{ "
                        + "   var: 'myValue'"
                        + "}");

        org.flowable.task.api.Task task = taskService.createTaskQuery().active().singleResult();
        assertThat(task).isNotNull();
        ObjectNode var3Node = objectMapper.createObjectNode();
        var3Node.put("var", "myValue");
        var3Node.put("var2", "myOtherValue");
        var3Node.put("var3", "myThirdValue");

        vars.put("myJsonObj", var3Node);
        taskService.complete(task.getId(), vars);

        value = (ObjectNode) runtimeService.getVariable(processInstance.getId(), "myJsonObj");
        assertThat(value).isNotNull();
        assertThatJson(value)
                .isEqualTo("{ "
                        + "   var: 'myValue',"
                        + "   var2: 'myOtherValue',"
                        + "   var3: 'myThirdValue'"
                        + "}");

        task = taskService.createTaskQuery().active().singleResult();
        assertThat(task).isNotNull();
        assertThat(task.getTaskDefinitionKey()).isEqualTo("userTaskSuccess");
    }

    @Test
    @Deployment
    public void testJsonArrayAvailable() {
        Map<String, Object> vars = new HashMap<>();

        ArrayNode varArray = objectMapper.createArrayNode();
        ObjectNode varNode = objectMapper.createObjectNode();
        varNode.put("var", "myValue");
        varArray.add(varNode);
        vars.put("myJsonArr", varArray);
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("testJsonAvailableProcess", vars);

        // Check JSON has been parsed as expected
        ArrayNode value = (ArrayNode) runtimeService.getVariable(processInstance.getId(), "myJsonArr");
        assertThat(value).isNotNull();
        assertThatJson(value)
                .isEqualTo("[ { "
                        + "   var: 'myValue'"
                        + "} ]");

        ArrayNode varArray2 = objectMapper.createArrayNode();
        varNode = objectMapper.createObjectNode();
        varNode.put("var", "myValue");
        varArray2.add(varNode);
        varNode = objectMapper.createObjectNode();
        varNode.put("var", "myOtherValue");
        varArray2.add(varNode);
        runtimeService.setVariable(processInstance.getId(), "myJsonArr", varArray2);

        // Check JSON has been updated as expected
        value = (ArrayNode) runtimeService.getVariable(processInstance.getId(), "myJsonArr");
        assertThat(value).isNotNull();
        assertThatJson(value)
                .isEqualTo("[ { "
                        + "   var: 'myValue'"
                        + "}, {"
                        + "   var: 'myOtherValue'"
                        + "} ]");

        org.flowable.task.api.Task task = taskService.createTaskQuery().active().singleResult();
        assertThat(task).isNotNull();
        ArrayNode varArray3 = objectMapper.createArrayNode();
        varNode = objectMapper.createObjectNode();
        varNode.put("var", "myValue");
        varArray3.add(varNode);
        varNode = objectMapper.createObjectNode();
        varNode.put("var", "myOtherValue");
        varArray3.add(varNode);
        varNode = objectMapper.createObjectNode();
        varNode.put("var", "myThirdValue");
        varArray3.add(varNode);
        vars = new HashMap<>();
        vars.put("myJsonArr", varArray3);
        taskService.complete(task.getId(), vars);
        value = (ArrayNode) runtimeService.getVariable(processInstance.getId(), "myJsonArr");
        assertThat(value).isNotNull();
        assertThatJson(value)
                .isEqualTo("[ { "
                        + "   var: 'myValue'"
                        + "}, {"
                        + "   var: 'myOtherValue'"
                        + "}, {"
                        + "   var: 'myThirdValue'"
                        + "} ]");

        task = taskService.createTaskQuery().active().singleResult();
        assertThat(task).isNotNull();
        assertThat(task.getTaskDefinitionKey()).isEqualTo("userTaskSuccess");

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
            HistoricVariableInstance historicVariableInstance = historyService.createHistoricVariableInstanceQuery()
                    .processInstanceId(processInstance.getProcessInstanceId()).singleResult();
            value = (ArrayNode) historicVariableInstance.getValue();
            assertThat(value).isNotNull();
            assertThatJson(value)
                    .isEqualTo("[ { "
                            + "   var: 'myValue'"
                            + "}, {"
                            + "   var: 'myOtherValue'"
                            + "}, {"
                            + "   var: 'myThirdValue'"
                            + "} ]");
        }
    }

    @Test
    @Deployment
    public void testJsonArrayAccessByIndex() {
        Map<String, Object> vars = new HashMap<>();

        ArrayNode varArray = objectMapper.createArrayNode();
        ObjectNode varNode = objectMapper.createObjectNode();
        varNode.put("var", "myValue");
        varArray.add(varNode);
        vars.put("myJsonArr", varArray);
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("testJsonAvailableProcess", vars);

        // Check JSON has been parsed as expected
        ArrayNode value = (ArrayNode) runtimeService.getVariable(processInstance.getId(), "myJsonArr");
        assertThat(value).isNotNull();
        assertThatJson(value)
                .isEqualTo("[ { "
                        + "   var: 'myValue'"
                        + "} ]");

        org.flowable.task.api.Task task = taskService.createTaskQuery().active().singleResult();
        assertThat(task).isNotNull();
        ArrayNode taskVarArray = objectMapper.createArrayNode();
        taskVarArray.addObject().put("var", "firstValue");
        taskVarArray.addObject().put("var", "secondValue");
        taskVarArray.addObject().put("var", "thirdValue");
        vars = new HashMap<>();
        vars.put("myJsonArr", taskVarArray);
        taskService.complete(task.getId(), vars);

        task = taskService.createTaskQuery().active().singleResult();
        assertThat(task).isNotNull();
        assertThat(task.getTaskDefinitionKey()).isEqualTo("userTaskSuccess");
    }

    @Test
    @Deployment
    public void testJsonArrayAccessByNegativeIndex() {
        Map<String, Object> vars = new HashMap<>();

        ArrayNode varArray = objectMapper.createArrayNode();
        ObjectNode varNode = objectMapper.createObjectNode();
        varNode.put("var", "myValue");
        varArray.add(varNode);
        vars.put("myJsonArr", varArray);
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("testJsonAvailableProcess", vars);

        // Check JSON has been parsed as expected
        ArrayNode value = (ArrayNode) runtimeService.getVariable(processInstance.getId(), "myJsonArr");
        assertThat(value).isNotNull();
        assertThatJson(value)
                .isEqualTo("[ { "
                        + "   var: 'myValue'"
                        + "} ]");

        org.flowable.task.api.Task task = taskService.createTaskQuery().active().singleResult();
        assertThat(task).isNotNull();
        ArrayNode taskVarArray = objectMapper.createArrayNode();
        taskVarArray.addObject().put("var", "firstValue");
        taskVarArray.addObject().put("var", "secondValue");
        taskVarArray.addObject().put("var", "thirdValue");
        vars = new HashMap<>();
        vars.put("myJsonArr", taskVarArray);
        taskService.complete(task.getId(), vars);

        task = taskService.createTaskQuery().active().singleResult();
        assertThat(task).isNotNull();
        assertThat(task.getTaskDefinitionKey()).isEqualTo("userTaskSuccess");
    }

    @Test
    @Deployment
    public void testJsonNumber() {
        Map<String, Object> vars = new HashMap<>();

        ObjectNode varNode = objectMapper.createObjectNode();
        varNode.put("numVar", 10);
        vars.put(MY_JSON_OBJ, varNode);
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("testJsonAvailableProcess", vars);

        // Check JSON has been parsed as expected
        ObjectNode value = (ObjectNode) runtimeService.getVariable(processInstance.getId(), MY_JSON_OBJ);
        assertThat(value.get("numVar").asInt()).isEqualTo(10);

        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        taskService.complete(task.getId(), vars);

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task.getTaskDefinitionKey()).isEqualTo("userTaskSuccess");

        vars = new HashMap<>();

        varNode = objectMapper.createObjectNode();
        varNode.put("numVar", 40);
        vars.put(MY_JSON_OBJ, varNode);
        processInstance = runtimeService.startProcessInstanceByKey("testJsonAvailableProcess", vars);

        // Check JSON has been parsed as expected
        value = (ObjectNode) runtimeService.getVariable(processInstance.getId(), MY_JSON_OBJ);
        assertThat(value.get("numVar").asInt()).isEqualTo(40);

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        taskService.complete(task.getId(), vars);

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task.getTaskDefinitionKey()).isEqualTo("userTaskFailure");
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml")
    void testSetNestedJsonNodeValue() {
        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("oneTaskProcess")
                .variable("customer", objectMapper.createObjectNode())
                .start();

        assertThatJson(runtimeService.getVariable(processInstance.getId(), "customer"))
                .isEqualTo("{}");
        managementService.executeCommand(commandContext -> {
            Expression expression = processEngineConfiguration.getExpressionManager().createExpression("${customer.address.street}");
            expression.setValue("Sesame Street", CommandContextUtil.getExecutionEntityManager(commandContext).findById(processInstance.getId()));
            return null;
        });

        assertThatJson(runtimeService.getVariable(processInstance.getId(), "customer"))
                .isEqualTo("{"
                        + "  address: {"
                        + "    street: 'Sesame Street'"
                        + "  }"
                        + "}");

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            assertThatJson(historyService.createHistoricVariableInstanceQuery().variableName("customer").singleResult().getValue())
                    .isEqualTo("{"
                            + "  address: {"
                            + "    street: 'Sesame Street'"
                            + "  }"
                            + "}");
        }
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml")
    void testSetIntegerInJsonNode() {
        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("oneTaskProcess")
                .variable("customer", objectMapper.createObjectNode())
                .start();

        assertThatJson(runtimeService.getVariable(processInstance.getId(), "customer"))
                .isEqualTo("{}");
        managementService.executeCommand(commandContext -> {
            Expression expression = processEngineConfiguration.getExpressionManager().createExpression("${customer.pin}");
            expression.setValue(10, CommandContextUtil.getExecutionEntityManager(commandContext).findById(processInstance.getId()));
            return null;
        });

        assertThatJson(runtimeService.getVariable(processInstance.getId(), "customer"))
                .isEqualTo("{"
                        + "  pin: 10"
                        + "}");

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            assertThatJson(historyService.createHistoricVariableInstanceQuery().variableName("customer").singleResult().getValue())
                    .isEqualTo("{"
                            + "  pin: 10"
                            + "}");
        }
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml")
    void testSetDateInJsonNode() {
        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("oneTaskProcess")
                .variable("customer", objectMapper.createObjectNode())
                .start();

        assertThatJson(runtimeService.getVariable(processInstance.getId(), "customer"))
                .isEqualTo("{}");
        managementService.executeCommand(commandContext -> {
            Expression expression = processEngineConfiguration.getExpressionManager().createExpression("${customer.creationDate}");
            expression.setValue(Date.from(Instant.parse("2020-02-16T14:24:45.583Z")),
                    CommandContextUtil.getExecutionEntityManager(commandContext).findById(processInstance.getId()));
            return null;
        });

        assertThatJson(runtimeService.getVariable(processInstance.getId(), "customer"))
                .isEqualTo("{ creationDate: '2020-02-16T14:24:45.583Z' }");

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            assertThatJson(historyService.createHistoricVariableInstanceQuery().variableName("customer").singleResult().getValue())
                    .isEqualTo("{ creationDate: '2020-02-16T14:24:45.583Z' }");
        }
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml")
    void testSetInstantInJsonNode() {
        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("oneTaskProcess")
                .variable("customer", objectMapper.createObjectNode())
                .start();

        assertThatJson(runtimeService.getVariable(processInstance.getId(), "customer"))
                .isEqualTo("{}");
        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            assertThatJson(historyService.createHistoricVariableInstanceQuery().variableName("customer").singleResult().getValue())
                    .isEqualTo("{}");
        }

        managementService.executeCommand(commandContext -> {
            Expression expression = processEngineConfiguration.getExpressionManager().createExpression("${customer.creationDate}");
            expression.setValue(Instant.parse("2020-02-16T14:24:45.583Z"),
                    CommandContextUtil.getExecutionEntityManager(commandContext).findById(processInstance.getId()));
            return null;
        });

        assertThatJson(runtimeService.getVariable(processInstance.getId(), "customer"))
                .isEqualTo("{ creationDate: '2020-02-16T14:24:45.583Z' }");

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            assertThatJson(historyService.createHistoricVariableInstanceQuery().variableName("customer").singleResult().getValue())
                    .isEqualTo("{ creationDate: '2020-02-16T14:24:45.583Z' }");
        }
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml")
    void testGetNestedJsonNodeValue() {
        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("oneTaskProcess")
                .variable("customer", objectMapper.createObjectNode())
                .start();

        assertThatJson(runtimeService.getVariable(processInstance.getId(), "customer"))
                .isEqualTo("{}");

        Object value = managementService.executeCommand(commandContext -> {
            Expression expression = processEngineConfiguration.getExpressionManager().createExpression("${customer.address.street}");
            return expression.getValue(CommandContextUtil.getExecutionEntityManager(commandContext).findById(processInstance.getId()));
        });

        assertThat(value).isNull();

        assertThatJson(runtimeService.getVariable(processInstance.getId(), "customer"))
                .isEqualTo("{}");
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml")
    void testSetArrayNestedJsonNodeValue() {
        ArrayNode customers = objectMapper.createArrayNode();
        customers.addObject();

        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("oneTaskProcess")
                .variable("customers", customers)
                .start();

        assertThatJson(runtimeService.getVariable(processInstance.getId(), "customers"))
                .isEqualTo("[{}]");

        managementService.executeCommand(commandContext -> {
            Expression expression = processEngineConfiguration.getExpressionManager().createExpression("${customers[0].address.street}");
            expression.setValue("Sesame Street", CommandContextUtil.getExecutionEntityManager(commandContext).findById(processInstance.getId()));
            return null;
        });

        assertThatJson(runtimeService.getVariable(processInstance.getId(), "customers"))
                .isEqualTo("["
                        + "  {"
                        + "    address: {"
                        + "      street: 'Sesame Street'"
                        + "    }"
                        + "  }"
                        + "]");
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml")
    void testGetArrayNestedJsonNodeValue() {
        ArrayNode customers = objectMapper.createArrayNode();
        customers.addObject()
                .put("name", "Kermit the Frog");

        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("oneTaskProcess")
                .variable("customers", customers)
                .start();

        assertThatJson(runtimeService.getVariable(processInstance.getId(), "customers"))
                .isEqualTo("["
                        + "  {"
                        + "    name: 'Kermit the Frog'"
                        + "  }"
                        + "]");

        Object value = managementService.executeCommand(commandContext -> {
            Expression expression = processEngineConfiguration.getExpressionManager().createExpression("${customers[0].address.street}");
            return expression.getValue(CommandContextUtil.getExecutionEntityManager(commandContext).findById(processInstance.getId()));
        });

        assertThat(value).isNull();

        assertThatJson(runtimeService.getVariable(processInstance.getId(), "customers"))
                .isEqualTo("["
                        + "  {"
                        + "    name: 'Kermit the Frog'"
                        + "  }"
                        + "]");
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml")
    void testSetNestedArrayNestedJsonNodeValue() {
        ArrayNode customers = objectMapper.createArrayNode();
        customers.addObject()
                .putArray("addresses")
                .addObject();

        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("oneTaskProcess")
                .variable("customers", customers)
                .start();
        assertThatJson(runtimeService.getVariable(processInstance.getId(), "customers"))
                .isEqualTo("[{"
                        + "  addresses: [{}]"
                        + "}]");

        managementService.executeCommand(commandContext -> {
            Expression expression = processEngineConfiguration.getExpressionManager().createExpression("${customers[0].addresses[0].street}");
            expression.setValue("Sesame Street", CommandContextUtil.getExecutionEntityManager(commandContext).findById(processInstance.getId()));
            return null;
        });

        assertThatJson(runtimeService.getVariable(processInstance.getId(), "customers"))
                .isEqualTo("[{"
                        + "  addresses: [{"
                        + "    street: 'Sesame Street'"
                        + "  }]"
                        + "}]");
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml")
    void testSetNestedArrayNestedJsonNodeValueWhenNestedArrayIsMissing() {
        ArrayNode customers = objectMapper.createArrayNode();
        customers.addObject();

        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("oneTaskProcess")
                .variable("customers", customers)
                .start();
        assertThatJson(runtimeService.getVariable(processInstance.getId(), "customers"))
                .isEqualTo("[{}]");

        assertThatThrownBy(() -> managementService.executeCommand(commandContext -> {
            Expression expression = processEngineConfiguration.getExpressionManager().createExpression("${customers[0].addresses[0].street}");
            expression.setValue("Sesame Street", CommandContextUtil.getExecutionEntityManager(commandContext).findById(processInstance.getId()));
            return null;
        }))
                .hasCauseInstanceOf(PropertyNotFoundException.class);

        assertThatJson(runtimeService.getVariable(processInstance.getId(), "customers"))
                .isEqualTo("[{}]");
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml")
    void testArrayNodeJsonNodeValue() {
        ArrayNode customers = objectMapper.createArrayNode();
        customers.add("Initial value");

        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("oneTaskProcess")
                .variable("customers", customers)
                .start();

        assertThatJson(runtimeService.getVariable(processInstance.getId(), "customers"))
                .isEqualTo("['Initial value']");
        managementService.executeCommand(commandContext -> {
            Expression expression = processEngineConfiguration.getExpressionManager().createExpression("${customers[0]}");
            expression.setValue("Flowable", CommandContextUtil.getExecutionEntityManager(commandContext).findById(processInstance.getId()));
            return customers;
        });

        assertThatJson(runtimeService.getVariable(processInstance.getId(), "customers"))
                .isEqualTo("['Flowable']");

        managementService.executeCommand(commandContext -> {
            Expression expression = processEngineConfiguration.getExpressionManager().createExpression("${customers[0]}");
            expression.setValue(10L, CommandContextUtil.getExecutionEntityManager(commandContext).findById(processInstance.getId()));
            return customers;
        });
        assertThatJson(runtimeService.getVariable(processInstance.getId(), "customers"))
                .isEqualTo("[10]");

        managementService.executeCommand(commandContext -> {
            Expression expression = processEngineConfiguration.getExpressionManager().createExpression("${customers[0]}");
            ObjectNode value = objectMapper.createObjectNode();
            value.putObject("address")
                    .put("street", "Sesame Street");
            expression.setValue(value, CommandContextUtil.getExecutionEntityManager(commandContext).findById(processInstance.getId()));
            return customers;
        });
        assertThatJson(runtimeService.getVariable(processInstance.getId(), "customers"))
                .isEqualTo("[{"
                        + "  address: {"
                        + "    street: 'Sesame Street'"
                        + "  }"
                        + "}]");
    }

    protected ObjectNode createBigJsonObject() {
        ObjectNode valueNode = objectMapper.createObjectNode();
        for (int i = 0; i < 1000; i++) {
            ObjectNode childNode = objectMapper.createObjectNode();
            childNode.put("test", "this is a simple test text");
            childNode.put("test2", "this is a simple test2 text");
            childNode.put("test3", "this is a simple test3 text");
            childNode.put("test4", "this is a simple test4 text");
            valueNode.set("var" + i, childNode);
        }
        return valueNode;
    }

}
