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
import org.flowable.common.engine.impl.identity.Authentication;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.variable.service.impl.el.NoExecutionVariableScope;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * @author Frederik Heremans
 */
public class ExpressionManagerTest extends PluggableFlowableTestCase {

    @Test
    public void testExpressionEvaluationWithoutProcessContext() {
        Expression expression = this.processEngineConfiguration.getExpressionManager().createExpression("#{1 == 1}");
        Object value = expression.getValue(new NoExecutionVariableScope());
        assertThat(value).isEqualTo(true);
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/api/runtime/oneTaskProcess.bpmn20.xml")
    public void testIntJsonVariableSerialization() {
        Map<String, Object> vars = new HashMap<>();
        vars.put("mapVariable", processEngineConfiguration.getObjectMapper().createObjectNode().put("minIntVar", Integer.MIN_VALUE));
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);

        Expression expression = this.processEngineConfiguration.getExpressionManager().createExpression("#{mapVariable.minIntVar}");
        Object value = managementService.executeCommand(commandContext ->
            expression.getValue((ExecutionEntity) runtimeService.createProcessInstanceQuery().processInstanceId(processInstance.getId()).includeProcessVariables().singleResult()));

        assertThat(value).isEqualTo(Integer.MIN_VALUE);
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/api/runtime/oneTaskProcess.bpmn20.xml")
    public void testShortJsonVariableSerialization() {
        Map<String, Object> vars = new HashMap<>();
        vars.put("mapVariable", processEngineConfiguration.getObjectMapper().createObjectNode().put("minShortVar", Short.MIN_VALUE));
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);

        Expression expression = this.processEngineConfiguration.getExpressionManager().createExpression("#{mapVariable.minShortVar}");
        Object value = managementService.executeCommand(commandContext ->
            expression.getValue((ExecutionEntity) runtimeService.createProcessInstanceQuery().processInstanceId(processInstance.getId()).includeProcessVariables().singleResult()));

        assertThat(value).isEqualTo((int) Short.MIN_VALUE);
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/api/runtime/oneTaskProcess.bpmn20.xml")
    public void testFloatJsonVariableSerialization() {
        Map<String, Object> vars = new HashMap<>();
        vars.put("mapVariable", processEngineConfiguration.getObjectMapper().createObjectNode().put("minFloatVar", new Float(-1.5)));
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);

        Expression expression = this.processEngineConfiguration.getExpressionManager().createExpression("#{mapVariable.minFloatVar}");
        Object value = managementService.executeCommand(commandContext ->
            expression.getValue((ExecutionEntity) runtimeService.createProcessInstanceQuery().processInstanceId(processInstance.getId()).includeProcessVariables().singleResult()));

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
            expression.getValue((ExecutionEntity) runtimeService.createProcessInstanceQuery().processInstanceId(processInstance.getId()).includeProcessVariables().singleResult()));

        assertThat(value).isNull();
    }

    @Test
    @Deployment
    public void testMethodExpressions() {
        // Process contains 2 service tasks. One containing a method with no params, the other
        // contains a method with 2 params. When the process completes without exception, test passed.
        Map<String, Object> vars = new HashMap<>();
        vars.put("aString", "abcdefgh");
        runtimeService.startProcessInstanceByKey("methodExpressionProcess", vars);

        assertThat(runtimeService.createProcessInstanceQuery().processDefinitionKey("methodExpressionProcess").count()).isZero();
    }

    @Test
    @Deployment
    public void testExecutionAvailable() {
        Map<String, Object> vars = new HashMap<>();

        vars.put("myVar", new ExecutionTestVariable());
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("testExecutionAvailableProcess", vars);

        // Check of the testMethod has been called with the current execution
        String value = (String) runtimeService.getVariable(processInstance.getId(), "testVar");
        assertThat(value).isEqualTo("myValue");
    }

    @Test
    @Deployment
    public void testAuthenticatedUserIdAvailable() {
        try {
            // Setup authentication
            Authentication.setAuthenticatedUserId("frederik");
            ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("testAuthenticatedUserIdAvailableProcess");

            // Check if the variable that has been set in service-task is the
            // authenticated user
            String value = (String) runtimeService.getVariable(processInstance.getId(), "theUser");
            assertThat(value).isEqualTo("frederik");
        } finally {
            // Cleanup
            Authentication.setAuthenticatedUserId(null);
        }
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/api/runtime/oneTaskProcess.bpmn20.xml")
    public void testOverloadedMethodUsage() {
        Map<String, Object> vars = new HashMap<>();
        vars.put("nodeVariable", processEngineConfiguration.getObjectMapper().createObjectNode());
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);

        Expression expression = this.processEngineConfiguration.getExpressionManager().createExpression("#{nodeVariable.put('stringVar', 'String value').put('intVar', 10)}");
        Object value = managementService.executeCommand(commandContext ->
                expression.getValue((ExecutionEntity) runtimeService.createProcessInstanceQuery().processInstanceId(processInstance.getId()).includeProcessVariables().singleResult()));

        assertThat(value).isInstanceOf(ObjectNode.class);
        assertThatJson(value)
                .isEqualTo("{"
                        + "  stringVar: 'String value',"
                        + "  intVar: 10"
                        + "}");
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/api/runtime/oneTaskProcess.bpmn20.xml")
    public void testAmbiguousMethodSingleNumberParameterUsage() {
        Map<String, Object> vars = new HashMap<>();
        vars.put("intVar", 10);
        vars.put("doubleVar", 25.5d);
        vars.put("longVar", 350L);
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);

        Expression intExpression = processEngineConfiguration.getExpressionManager().createExpression("#{bean.number(intVar)}");
        Object value = managementService.executeCommand(commandContext -> {
            ExecutionEntity executionEntity = (ExecutionEntity) runtimeService.createProcessInstanceQuery()
                    .processInstanceId(processInstance.getId())
                    .includeProcessVariables().singleResult();
            executionEntity.setTransientVariable("bean", new TestAmbiguousMethodSingleNumberParameterBean());
            return intExpression.getValue(executionEntity);
        });

        assertThat(value).isEqualTo("number");

        Expression doubleExpression = processEngineConfiguration.getExpressionManager().createExpression("#{bean.number(doubleVar)}");
        value = managementService.executeCommand(commandContext -> {
            ExecutionEntity executionEntity = (ExecutionEntity) runtimeService.createProcessInstanceQuery()
                    .processInstanceId(processInstance.getId())
                    .includeProcessVariables().singleResult();
            executionEntity.setTransientVariable("bean", new TestAmbiguousMethodSingleNumberParameterBean());
            return doubleExpression.getValue(executionEntity);
        });

        assertThat(value).isEqualTo("double");

        Expression longExpression = processEngineConfiguration.getExpressionManager().createExpression("#{bean.number(longVar)}");
        value = managementService.executeCommand(commandContext -> {
            ExecutionEntity executionEntity = (ExecutionEntity) runtimeService.createProcessInstanceQuery()
                    .processInstanceId(processInstance.getId())
                    .includeProcessVariables().singleResult();
            executionEntity.setTransientVariable("bean", new TestAmbiguousMethodSingleNumberParameterBean());
            return longExpression.getValue(executionEntity);
        });

        assertThat(value).isEqualTo("long");
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/api/runtime/oneTaskProcess.bpmn20.xml")
    public void testAmbiguousMethodSingleExecutionParameterUsage() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");

        Expression expression = processEngineConfiguration.getExpressionManager().createExpression("#{bean.run(execution)}");
        Object value = managementService.executeCommand(commandContext -> {
            ExecutionEntity executionEntity = (ExecutionEntity) runtimeService.createProcessInstanceQuery()
                    .processInstanceId(processInstance.getId())
                    .includeProcessVariables().singleResult();
            executionEntity.setTransientVariable("bean", new TestAmbiguousMethodSingleExecutionParameterBean());
            return expression.getValue(executionEntity);
        });

        assertThat(value).isEqualTo("executionEntity");
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/api/runtime/oneTaskProcess.bpmn20.xml")
    public void testInvokeStringMethodWithNullParameter() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");

        Expression expression = processEngineConfiguration.getExpressionManager().createExpression("#{bean.string(null)}");
        Object value = managementService.executeCommand(commandContext -> {
            ExecutionEntity executionEntity = (ExecutionEntity) runtimeService.createProcessInstanceQuery()
                    .processInstanceId(processInstance.getId())
                    .includeProcessVariables().singleResult();
            executionEntity.setTransientVariable("bean", new TestCoerceBean());
            return expression.getValue(executionEntity);
        });

        assertThat(value).isNull();
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/api/runtime/oneTaskProcess.bpmn20.xml")
    public void testInvokePrimitiveIntegerMethodWithNullParameter() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");

        Expression expression = processEngineConfiguration.getExpressionManager().createExpression("#{bean.primitiveInteger(null)}");
        Object value = managementService.executeCommand(commandContext -> {
            ExecutionEntity executionEntity = (ExecutionEntity) runtimeService.createProcessInstanceQuery()
                    .processInstanceId(processInstance.getId())
                    .includeProcessVariables().singleResult();
            executionEntity.setTransientVariable("bean", new TestCoerceBean());
            return expression.getValue(executionEntity);
        });

        assertThat(value).isEqualTo("0");
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/api/runtime/oneTaskProcess.bpmn20.xml")
    public void testInvokeIntegerMethodWithNullParameter() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");

        Expression expression = processEngineConfiguration.getExpressionManager().createExpression("#{bean.nonPrimitiveInteger(null)}");
        Object value = managementService.executeCommand(commandContext -> {
            ExecutionEntity executionEntity = (ExecutionEntity) runtimeService.createProcessInstanceQuery()
                    .processInstanceId(processInstance.getId())
                    .includeProcessVariables().singleResult();
            executionEntity.setTransientVariable("bean", new TestCoerceBean());
            return expression.getValue(executionEntity);
        });

        assertThat(value).isNull();
    }

    static class TestAmbiguousMethodSingleNumberParameterBean {

        public String number(Number number) {
            return "number";
        }

        public String number(Double d) {
            return "double";
        }

        public String number(Long l) {
            return "long";
        }
    }

    static class TestAmbiguousMethodSingleExecutionParameterBean {

        public String run(DelegateExecution execution) {
            return "delegateExecution";
        }

        public String run(ExecutionEntity execution) {
            return "executionEntity";
        }
    }

    static class TestCoerceBean {

        public String string(String value) {
            return value;
        }

        public String primitiveInteger(int value) {
            return String.valueOf(value);
        }

        public String nonPrimitiveInteger(Integer value) {
            return value == null ? null : value.toString();
        }
    }
}
