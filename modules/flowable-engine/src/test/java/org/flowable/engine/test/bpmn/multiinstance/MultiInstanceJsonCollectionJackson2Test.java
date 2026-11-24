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
package org.flowable.engine.test.bpmn.multiinstance;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.assertj.core.api.SoftAssertions;
import org.flowable.common.engine.impl.json.jackson2.Jackson2VariableJsonMapper;
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.test.ResourceFlowableTestCase;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * @author Filip Hrisafov
 */
public class MultiInstanceJsonCollectionJackson2Test extends ResourceFlowableTestCase {

    protected static final List<String> JACKSON_2_METHODS = List.of(
            "asText()", "textValue()", "asInt()", "asLong()", "asDouble()", "asBoolean()",
            "intValue()", "longValue()", "doubleValue()", "floatValue()", "decimalValue()", "bigIntegerValue()", "numberValue()", "booleanValue()",
            "isTextual()", "isNumber()", "isInt()", "isLong()", "isDouble()", "isBigDecimal()", "isBigInteger()", "isBoolean()",
            "isValueNode()", "isNull()", "isMissingNode()", "isContainerNode()", "isObject()", "isArray()",
            "asText('default')", "asInt(9)", "asLong(9)", "asDouble(9.5)", "asBoolean(true)"
    );

    // Jackson 3 names that do not exist in Jackson 2, mapped to their Jackson 2 counterpart
    protected static final Map<String, String> JACKSON_3_ONLY_METHODS = Map.of(
            "asString()", "asText()",
            "asString('default')", "asText('default')",
            "stringValue()", "textValue()",
            "isString()", "isTextual()",
            "isContainer()", "isContainerNode()"
    );

    protected final ObjectMapper objectMapper = new ObjectMapper();

    public MultiInstanceJsonCollectionJackson2Test() {
        super("flowable.cfg.xml", "multiInstanceJsonCollectionJackson2");
    }

    @Override
    protected void additionalConfiguration(ProcessEngineConfiguration processEngineConfiguration) {
        ((ProcessEngineConfigurationImpl) processEngineConfiguration).setVariableJsonMapper(new Jackson2VariableJsonMapper(objectMapper));
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/bpmn/multiinstance/MultiInstanceJsonCollectionTest.parallelUserTasks.bpmn20.xml")
    public void parallelUserTasks() {
        ArrayNode assignees = objectMapper.createArrayNode()
                .add("kermit")
                .add("gonzo");

        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("miJsonCollection")
                .variable("assignees", assignees)
                .start();

        List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertThat(tasks)
                .extracting(Task::getAssignee, Task::getName)
                .containsExactlyInAnyOrder(
                        tuple("kermit", "Task kermit"),
                        tuple("gonzo", "Task gonzo")
                );

        for (Task task : tasks) {
            assertThat(runtimeService.getVariableLocal(task.getExecutionId(), "assignee"))
                    .isEqualTo(task.getAssignee());
        }

        tasks.forEach(task -> taskService.complete(task.getId()));
        assertProcessEnded(processInstance.getId());
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/bpmn/multiinstance/MultiInstanceJsonCollectionTest.elementVariableTypes.bpmn20.xml")
    public void elementVariableTypes() {
        ArrayNode items = objectMapper.createArrayNode();
        items.add(42);
        items.add((short) 7);
        items.add(5000000000L);
        items.add(2.5);
        items.add(1.5f);
        items.add(new BigDecimal("1.25"));
        items.add(new BigInteger("12345678901234567890123"));
        items.add(true);
        items.add("text");
        items.addNull();
        items.addObject().put("name", "gonzo");
        items.addArray().add(1).add(2);

        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("miJsonCollection")
                .variable("items", items)
                .start();

        Map<Integer, Object> itemsByLoopCounter = new HashMap<>();
        List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertThat(tasks).hasSize(12);
        for (Task task : tasks) {
            Map<String, Object> localVariables = runtimeService.getVariablesLocal(task.getExecutionId());
            assertThat(localVariables).containsKey("item");
            itemsByLoopCounter.put((Integer) localVariables.get("loopCounter"), localVariables.get("item"));
        }

        assertThat(itemsByLoopCounter.get(0)).isEqualTo(42);
        assertThat(itemsByLoopCounter.get(1)).isEqualTo(7);
        assertThat(itemsByLoopCounter.get(2)).isEqualTo(5000000000L);
        assertThat(itemsByLoopCounter.get(3)).isEqualTo(2.5);
        assertThat(itemsByLoopCounter.get(4)).isEqualTo(1.5);
        assertThat(itemsByLoopCounter.get(5)).isEqualTo(new BigDecimal("1.25"));
        assertThat(itemsByLoopCounter.get(6)).isEqualTo(new BigInteger("12345678901234567890123"));
        assertThat(itemsByLoopCounter.get(7)).isEqualTo(true);
        assertThat(itemsByLoopCounter.get(8)).isEqualTo("text");
        assertThat(itemsByLoopCounter.get(9)).isNull();
        assertThat(itemsByLoopCounter.get(10)).isInstanceOf(ObjectNode.class);
        assertThatJson(itemsByLoopCounter.get(10)).isEqualTo("{ name: 'gonzo' }");
        assertThat(itemsByLoopCounter.get(11)).isInstanceOf(ArrayNode.class);
        assertThatJson(itemsByLoopCounter.get(11)).isEqualTo("[1, 2]");
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/bpmn/multiinstance/MultiInstanceJsonCollectionTest.elementVariableTypes.bpmn20.xml")
    public void jacksonMethodsOnElementBehaveLikeJackson2ValueNode() {
        List<Object> values = MultiInstanceJsonCollectionTest.VALUES;
        ArrayNode items = objectMapper.createArrayNode();
        values.forEach(value -> items.add(objectMapper.<JsonNode>valueToTree(value)));

        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("miJsonCollection")
                .variable("items", items)
                .start();

        // The method invoked on the element, mapped to the method invoked on the Jackson 2 node
        Map<String, String> methods = new LinkedHashMap<>();
        JACKSON_2_METHODS.forEach(method -> methods.put(method, method));
        methods.putAll(JACKSON_3_ONLY_METHODS);

        SoftAssertions softly = new SoftAssertions();
        getExecutionIdsByLoopCounter(processInstance.getId()).forEach((loopCounter, executionId) -> {
            Object node = items.get(loopCounter);
            methods.forEach((method, jackson2Method) -> softly.assertThat(evaluate("${item." + method + "}", executionId, null))
                    .as("%s.%s", values.get(loopCounter), method)
                    .isEqualTo(evaluate("${node." + jackson2Method + "}", executionId, node)));
        });
        softly.assertAll();
    }

    protected Map<Integer, String> getExecutionIdsByLoopCounter(String processInstanceId) {
        Map<Integer, String> executionIds = new HashMap<>();
        for (Task task : taskService.createTaskQuery().processInstanceId(processInstanceId).list()) {
            executionIds.put((Integer) runtimeService.getVariableLocal(task.getExecutionId(), "loopCounter"), task.getExecutionId());
        }
        return executionIds;
    }

    protected Object evaluate(String expression, String executionId, Object node) {
        return managementService.executeCommand(commandContext -> {
            ExecutionEntity execution = (ExecutionEntity) runtimeService.createExecutionQuery().executionId(executionId).singleResult();
            if (node != null) {
                execution.setTransientVariableLocal("node", node);
            }
            return processEngineConfiguration.getExpressionManager().createExpression(expression).getValue(execution);
        });
    }
}
