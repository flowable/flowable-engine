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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.assertj.core.api.SoftAssertions;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.Test;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * @author Filip Hrisafov
 */
public class MultiInstanceJsonCollectionTest extends PluggableFlowableTestCase {

    protected static final List<Object> VALUES = List.of(
            "kermit", "42", " 12 ", "-7", "+5", "1.5", "5000000000", "true", " false ", "TRUE", "   ", "-",
            7, 0, -3, 5000000000L, 2.75, 0.0, -1.5, new BigDecimal("1.25"), new BigInteger("12345678901234567890123"),
            true, false
    );

    // The methods with a default value are not compared,
    // Jackson 3 returns the default value where Jackson 2 (and the element) coerces the value
    protected static final List<String> JACKSON_3_METHODS = List.of(
            "asString()", "stringValue()", "asText()", "textValue()", "asInt()", "asLong()", "asDouble()", "asBoolean()",
            "intValue()", "longValue()", "doubleValue()", "floatValue()", "decimalValue()", "bigIntegerValue()", "numberValue()", "booleanValue()",
            "isString()", "isTextual()", "isNumber()", "isInt()", "isLong()", "isDouble()", "isBigDecimal()", "isBigInteger()", "isBoolean()",
            "isValueNode()", "isNull()", "isMissingNode()", "isContainer()", "isObject()", "isArray()"
    );

    @Test
    @Deployment
    public void parallelUserTasks() {
        ArrayNode assignees = processEngineConfiguration.getObjectMapper().createArrayNode()
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
    @Deployment
    public void elementVariableTypes() {
        ArrayNode items = processEngineConfiguration.getObjectMapper().createArrayNode();
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
    @Deployment
    public void jacksonMethodsOnElement() {
        ArrayNode assignees = processEngineConfiguration.getObjectMapper().createArrayNode()
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
                        tuple("kermit", "kermit kermit true false"),
                        tuple("gonzo", "gonzo gonzo true false")
                );
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/bpmn/multiinstance/MultiInstanceJsonCollectionTest.elementVariableTypes.bpmn20.xml")
    public void jacksonMethodsOnElementBehaveLikeJackson3ValueNode() {
        ObjectMapper objectMapper = processEngineConfiguration.getObjectMapper();
        ArrayNode items = objectMapper.createArrayNode();
        VALUES.forEach(value -> items.add(objectMapper.<JsonNode>valueToTree(value)));

        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("miJsonCollection")
                .variable("items", items)
                .start();

        SoftAssertions softly = new SoftAssertions();
        getExecutionIdsByLoopCounter(processInstance.getId()).forEach((loopCounter, executionId) -> {
            Object node = items.get(loopCounter);
            for (String method : JACKSON_3_METHODS) {
                Object expected;
                try {
                    expected = evaluate("${node." + method + "}", executionId, node);
                } catch (RuntimeException e) {
                    // Jackson 3 is stricter than Jackson 2 and throws for some conversions, the element is lenient in that case
                    continue;
                }

                softly.assertThat(evaluate("${item." + method + "}", executionId))
                        .as("%s.%s", VALUES.get(loopCounter), method)
                        .isEqualTo(expected);
            }
        });
        softly.assertAll();
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/bpmn/multiinstance/MultiInstanceJsonCollectionTest.elementVariableTypes.bpmn20.xml")
    public void nonJacksonMethodsOnElement() {
        ArrayNode items = processEngineConfiguration.getObjectMapper().createArrayNode()
                .add("kermit")
                .add(42)
                .add(true);

        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("miJsonCollection")
                .variable("items", items)
                .start();

        Map<Integer, String> executionIds = getExecutionIdsByLoopCounter(processInstance.getId());
        String textExecutionId = executionIds.get(0);
        String numberExecutionId = executionIds.get(1);
        String booleanExecutionId = executionIds.get(2);

        assertThat(evaluate("${item.length()}", textExecutionId)).isEqualTo(6);
        assertThat(evaluate("${item.toUpperCase()}", textExecutionId)).isEqualTo("KERMIT");
        assertThat(evaluate("${item.substring(1)}", textExecutionId)).isEqualTo("ermit");
        assertThat(evaluate("${item.isEmpty()}", textExecutionId)).isEqualTo(false);
        assertThat(evaluate("${item.startsWith('ker')}", textExecutionId)).isEqualTo(true);
        assertThat(evaluate("${item.toString()}", numberExecutionId)).isEqualTo("42");
        assertThat(evaluate("${item.byteValue()}", numberExecutionId)).isEqualTo((byte) 42);
        assertThat(evaluate("${item.toString()}", booleanExecutionId)).isEqualTo("true");

        assertThatThrownBy(() -> evaluate("${item.unknownMethod()}", textExecutionId))
                .hasMessageContaining("unknownMethod");
        assertThatThrownBy(() -> evaluate("${item.asInt('notANumber')}", textExecutionId))
                .hasMessageContaining("asInt");
        assertThatThrownBy(() -> evaluate("${item.asText(1, 2)}", textExecutionId))
                .hasMessageContaining("asText");
    }

    protected Map<Integer, String> getExecutionIdsByLoopCounter(String processInstanceId) {
        Map<Integer, String> executionIds = new HashMap<>();
        for (Task task : taskService.createTaskQuery().processInstanceId(processInstanceId).list()) {
            executionIds.put((Integer) runtimeService.getVariableLocal(task.getExecutionId(), "loopCounter"), task.getExecutionId());
        }
        return executionIds;
    }

    protected Object evaluate(String expression, String executionId) {
        return evaluate(expression, executionId, null);
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
