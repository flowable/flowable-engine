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

package org.flowable.engine.test.el;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;

import org.flowable.common.engine.api.delegate.Expression;
import org.flowable.common.engine.impl.json.jackson2.Jackson2VariableJsonMapper;
import org.flowable.common.engine.impl.tenant.CurrentTenant;
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.test.ResourceFlowableTestCase;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * @author Filip Hrisafov
 */
public class ExpressionManagerJackson2Test extends ResourceFlowableTestCase {

    public ExpressionManagerJackson2Test() {
        super("flowable.cfg.xml", "expressionManagerJackson2Test");
    }

    @Override
    protected void additionalConfiguration(ProcessEngineConfiguration processEngineConfiguration) {
        ((ProcessEngineConfigurationImpl) processEngineConfiguration).setVariableJsonMapper(new Jackson2VariableJsonMapper(new ObjectMapper()));
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/api/runtime/oneTaskProcess.bpmn20.xml")
    public void testIntJsonVariableSerialization() {
        Map<String, Object> vars = new HashMap<>();
        vars.put("mapVariable", processEngineConfiguration.getObjectMapper().createObjectNode().put("minIntVar", Integer.MIN_VALUE));
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);

        Expression expression = this.processEngineConfiguration.getExpressionManager().createExpression("#{mapVariable.minIntVar}");
        Object value = managementService.executeCommand(commandContext ->
                expression.getValue(
                        (ExecutionEntity) runtimeService.createProcessInstanceQuery().processInstanceId(processInstance.getId()).includeProcessVariables()
                                .singleResult()));

        assertThat(value).isEqualTo(Integer.MIN_VALUE);
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/api/runtime/oneTaskProcess.bpmn20.xml")
    public void testFloatJsonVariableSerialization() {
        Map<String, Object> vars = new HashMap<>();
        vars.put("mapVariable", processEngineConfiguration.getObjectMapper().createObjectNode().put("minFloatVar", Float.valueOf((float) -1.5)));
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);

        Expression expression = this.processEngineConfiguration.getExpressionManager().createExpression("#{mapVariable.minFloatVar}");
        Object value = managementService.executeCommand(commandContext ->
                expression.getValue(
                        (ExecutionEntity) runtimeService.createProcessInstanceQuery().processInstanceId(processInstance.getId()).includeProcessVariables()
                                .singleResult()));

        assertThat(value).isEqualTo(-1.5d);
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/api/runtime/oneTaskProcess.bpmn20.xml")
    public void testNullJsonVariableSerialization() {
        Map<String, Object> vars = new HashMap<>();
        vars.put("mapVariable", processEngineConfiguration.getObjectMapper().createObjectNode().putNull("nullVar"));
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);

        Expression expression = this.processEngineConfiguration.getExpressionManager().createExpression("#{mapVariable.nullVar}");
        Object value = managementService.executeCommand(commandContext ->
                expression.getValue(
                        (ExecutionEntity) runtimeService.createProcessInstanceQuery().processInstanceId(processInstance.getId()).includeProcessVariables()
                                .singleResult()));

        assertThat(value).isNull();
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/api/runtime/oneTaskProcess.bpmn20.xml")
    public void testOverloadedMethodUsage() {
        Map<String, Object> vars = new HashMap<>();
        vars.put("nodeVariable", processEngineConfiguration.getObjectMapper().createObjectNode());
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);

        Expression expression = this.processEngineConfiguration.getExpressionManager()
                .createExpression("#{nodeVariable.put('stringVar', 'String value').put('intVar', 10)}");
        Object value = managementService.executeCommand(commandContext ->
                expression.getValue(
                        (ExecutionEntity) runtimeService.createProcessInstanceQuery().processInstanceId(processInstance.getId()).includeProcessVariables()
                                .singleResult()));

        assertThat(value).isInstanceOf(com.fasterxml.jackson.databind.node.ObjectNode.class);
        assertThatJson(value)
                .isEqualTo("{"
                        + "  stringVar: 'String value',"
                        + "  intVar: 10"
                        + "}");
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/api/runtime/oneTaskProcess.bpmn20.xml")
    public void testInvokeOnArrayNode() {
        Map<String, Object> vars = new HashMap<>();
        ArrayNode arrayNode = processEngineConfiguration.getObjectMapper().createArrayNode();
        arrayNode.add("firstValue");
        arrayNode.add("secondValue");
        arrayNode.add(42);

        vars.put("array", arrayNode);
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);

        assertThat(getExpressionValue("${array.get(0).isTextual()}", processInstance)).isEqualTo(true);
        assertThat(getExpressionValue("${array.get(0).textValue()}", processInstance)).isEqualTo("firstValue");
        assertThat(getExpressionValue("${array.get(0).isNumber()}", processInstance)).isEqualTo(false);

        assertThat(getExpressionValue("${array.get(2).isNumber()}", processInstance)).isEqualTo(true);
        assertThat(getExpressionValue("${array.get(2).asInt()}", processInstance)).isEqualTo(42);
        assertThat(getExpressionValue("${array.get(2).asLong()}", processInstance)).isEqualTo(42L);

        assertThat(getExpressionValue("${array.get(1).textValue()}", processInstance)).isEqualTo("secondValue");
        assertThat(getExpressionValue("${array.get(1).asLong(123)}", processInstance)).isEqualTo(123L);

        assertThat(getExpressionValue("${array.get(3)}", processInstance)).isNull();
        assertThat(getExpressionValue("${array.path(3).isMissingNode()}", processInstance)).isEqualTo(true);
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/api/runtime/oneTaskProcess.bpmn20.xml")
    public void testInvokeOnObjectNode() {
        Map<String, Object> vars = new HashMap<>();
        ObjectNode objectNode = processEngineConfiguration.getObjectMapper().createObjectNode();
        objectNode.put("firstAttribute", "foo");
        objectNode.put("secondAttribute", "bar");
        objectNode.put("thirdAttribute", 42);
        objectNode.putNull("nullAttribute");

        vars.put("object", objectNode);
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);

        assertThat(getExpressionValue("${object.get(\"firstAttribute\").isTextual()}", processInstance)).isEqualTo(true);
        assertThat(getExpressionValue("${object.get(\"firstAttribute\").textValue()}", processInstance)).isEqualTo("foo");
        assertThat(getExpressionValue("${object.get(\"firstAttribute\").isNumber()}", processInstance)).isEqualTo(false);

        assertThat(getExpressionValue("${object.get(\"thirdAttribute\").isNumber()}", processInstance)).isEqualTo(true);
        assertThat(getExpressionValue("${object.get(\"thirdAttribute\").asInt()}", processInstance)).isEqualTo(42);
        assertThat(getExpressionValue("${object.get(\"thirdAttribute\").asLong()}", processInstance)).isEqualTo(42L);

        assertThat(getExpressionValue("${object.get(\"secondAttribute\").textValue()}", processInstance)).isEqualTo("bar");
        assertThat(getExpressionValue("${object.get(\"secondAttribute\").asLong(123)}", processInstance)).isEqualTo(123L);

        assertThat(getExpressionValue("${object.get(\"dummyAttribute\")}", processInstance)).isNull();
        assertThat(getExpressionValue("${object.path(\"dummyAttribute\").isMissingNode()}", processInstance)).isEqualTo(true);
        assertThat(getExpressionValue("${object.path(\"dummyAttribute\").asString()}", processInstance)).isEqualTo("");
        assertThat(getExpressionValue("${object.path(\"dummyAttribute\").asText()}", processInstance)).isEqualTo("");

        assertThat(getExpressionValue("${object.path(\"nullAttribute\").isNull()}", processInstance)).isEqualTo(true);
        assertThat(getExpressionValue("${object.path(\"nullAttribute\").asString()}", processInstance)).isEqualTo("null");
        assertThat(getExpressionValue("${object.path(\"nullAttribute\").asText()}", processInstance)).isEqualTo("null");
    }

    private Object getExpressionValue(String expressionStr, ProcessInstance processInstance) {
        Expression expression = this.processEngineConfiguration.getExpressionManager().createExpression(expressionStr);
        return managementService.executeCommand(commandContext ->
                expression.getValue(
                        (ExecutionEntity) runtimeService.createProcessInstanceQuery().processInstanceId(processInstance.getId()).includeProcessVariables()
                                .singleResult()));
    }

}
