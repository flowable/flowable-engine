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
package org.flowable.engine.test.bpmn.event.error.mapError;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashMap;
import java.util.Map;

import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.test.Deployment;
import org.flowable.standalone.testing.helpers.ServiceTaskTestMock;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * @author Saeid Mirzaei
 */
public class BoundaryErrorMapTest extends PluggableFlowableTestCase {

    // exception matches the only mapping, directly
    @Test
    @Deployment
    public void testClassDelegateSingleDirectMap() {
        FlagDelegate.reset();
        Map<String, Object> vars = new HashMap<>();
        vars.put("exceptionClass", BoundaryErrorParentException.class.getName());

        runtimeService.startProcessInstanceByKey("processWithSingleExceptionMap", vars);
        assertThat(FlagDelegate.isVisited()).isTrue();
    }

    @ParameterizedTest(name = "JavaFutureDelegate via class throws error in {0}")
    @ValueSource(strings = { "beforeExecution", "execute", "afterExecution" })
    @Deployment
    public void testClassFutureDelegateSingleDirectMap(String throwErrorIn) {
        FlagDelegate.reset();
        Map<String, Object> vars = new HashMap<>();
        vars.put("exceptionClass", BoundaryErrorParentException.class.getName());
        vars.put("throwErrorIn", throwErrorIn);

        runtimeService.startProcessInstanceByKey("processWithSingleExceptionMap", vars);
        assertThat(FlagDelegate.isVisited()).isTrue();
    }

    @Test
    @Deployment
    public void testExpressionSingleDirectMap() {
        FlagDelegate.reset();
        Map<String, Object> vars = new HashMap<>();
        vars.put("exceptionClass", BoundaryErrorParentException.class.getName());

        runtimeService.startProcessInstanceByKey("processWithSingleExceptionMap", vars);
        assertThat(FlagDelegate.isVisited()).isTrue();
    }
    
    @Test
    @Deployment
    public void testExpressionNonRuntimeException() {
        FlagDelegate.reset();
        Map<String, Object> vars = new HashMap<>();
        vars.put("exceptionClass", BoundaryErrorNonRuntimeException.class.getName());

        runtimeService.startProcessInstanceByKey("processWithSingleExceptionMap", vars);
        assertThat(FlagDelegate.isVisited()).isTrue();
    }

    @Test
    @Deployment
    public void testDelegateExpressionSingleDirectMap() {
        FlagDelegate.reset();
        Map<String, Object> vars = new HashMap<>();
        vars.put("exceptionClass", BoundaryErrorParentException.class.getName());

        runtimeService.startProcessInstanceByKey("processWithSingleExceptionMap", vars);
        assertThat(FlagDelegate.isVisited()).isTrue();
    }

    @ParameterizedTest(name = "JavaFutureDelegate via delegate expression throws error in {0}")
    @ValueSource(strings = { "beforeExecution", "execute", "afterExecution" })
    @Deployment
    public void testFutureDelegateExpressionSingleDirectMap(String throwErrorIn) {
        FlagDelegate.reset();

        runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("processWithSingleExceptionMap")
                .variable("exceptionClass", BoundaryErrorParentException.class.getName())
                .variable("throwErrorIn", throwErrorIn)
                .transientVariable("throwCustomExceptionFutureDelegate", new ThrowCustomExceptionFutureDelegate())
                .start();
        assertThat(FlagDelegate.isVisited()).isTrue();
    }

    @Test
    @Deployment
    public void testRootCauseSingleDirectMap() {
        FlagDelegate.reset();
        Map<String, Object> vars = new HashMap<>();
        vars.put("exceptionClass", BoundaryErrorParentException.class.getName());
        vars.put("nestedExceptionClass", IllegalArgumentException.class.getName());

        runtimeService.startProcessInstanceByKey("processWithSingleExceptionMap", vars);
        assertThat(FlagDelegate.isVisited()).isTrue();
    }

    // exception does not match the single mapping
    @Test
    @Deployment(resources = "org/flowable/engine/test/bpmn/event/error/mapError/BoundaryErrorMapTest.testClassDelegateSingleDirectMap.bpmn20.xml")
    public void testClassDelegateSingleDirectMapNotMatchingException() {
        FlagDelegate.reset();

        Map<String, Object> vars = new HashMap<>();
        vars.put("exceptionClass", IllegalStateException.class.getName());
        assertThat(ServiceTaskTestMock.CALL_COUNT.get()).isZero();

        assertThatThrownBy(() -> runtimeService.startProcessInstanceByKey("processWithSingleExceptionMap", vars))
                .as("exception expected, as there is no matching exception map")
                .isInstanceOf(Exception.class);
        assertThat(FlagDelegate.isVisited()).isFalse();
    }


    @ParameterizedTest(name = "JavaFutureDelegate via class throws error in {0}")
    @ValueSource(strings = { "beforeExecution", "execute", "afterExecution" })
    @Deployment(resources = "org/flowable/engine/test/bpmn/event/error/mapError/BoundaryErrorMapTest.testClassFutureDelegateSingleDirectMap.bpmn20.xml")
    public void testClassFutureDelegateSingleDirectMapNotMatchingException(String throwErrorIn) {
        FlagDelegate.reset();

        Map<String, Object> vars = new HashMap<>();
        vars.put("exceptionClass", IllegalStateException.class.getName());
        vars.put("throwErrorIn", throwErrorIn);
        assertThat(ServiceTaskTestMock.CALL_COUNT.get()).isZero();

        assertThatThrownBy(() -> runtimeService.startProcessInstanceByKey("processWithSingleExceptionMap", vars))
                .as("exception expected, as there is no matching exception map")
                .isInstanceOf(Exception.class);
        assertThat(FlagDelegate.isVisited()).isFalse();
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/bpmn/event/error/mapError/BoundaryErrorMapTest.testExpressionSingleDirectMap.bpmn20.xml")
    public void testExpressionSingleDirectMapNotMatchingException() {
        FlagDelegate.reset();

        Map<String, Object> vars = new HashMap<>();
        vars.put("exceptionClass", IllegalArgumentException.class.getName());
        assertThat(ServiceTaskTestMock.CALL_COUNT.get()).isZero();

        assertThatThrownBy(() -> runtimeService.startProcessInstanceByKey("processWithSingleExceptionMap", vars))
                .as("exception expected, as there is no matching exception map")
                .isInstanceOf(Exception.class);
        assertThat(FlagDelegate.isVisited()).isFalse();
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/bpmn/event/error/mapError/BoundaryErrorMapTest.testDelegateExpressionSingleDirectMap.bpmn20.xml")
    public void testDelegateExpressionSingleDirectMapNotMatchingException() {
        FlagDelegate.reset();

        Map<String, Object> vars = new HashMap<>();
        vars.put("exceptionClass", IllegalArgumentException.class.getName());
        assertThat(ServiceTaskTestMock.CALL_COUNT.get()).isZero();

        assertThatThrownBy(() -> runtimeService.startProcessInstanceByKey("processWithSingleExceptionMap", vars))
                .as("exception expected, as there is no matching exception map")
                .isInstanceOf(Exception.class);
        assertThat(FlagDelegate.isVisited()).isFalse();
    }

    @ParameterizedTest(name = "JavaFutureDelegate via delegate expression throws error in {0}")
    @ValueSource(strings = { "beforeExecution", "execute", "afterExecution" })
    @Deployment(resources = "org/flowable/engine/test/bpmn/event/error/mapError/BoundaryErrorMapTest.testFutureDelegateExpressionSingleDirectMap.bpmn20.xml")
    public void testFutureDelegateExpressionSingleDirectMapNotMatchingException(String throwErrorIn) {
        FlagDelegate.reset();

        assertThat(ServiceTaskTestMock.CALL_COUNT.get()).isZero();

        assertThatThrownBy(() -> runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("processWithSingleExceptionMap")
                .variable("exceptionClass", IllegalArgumentException.class.getName())
                .variable("throwErrorIn", throwErrorIn)
                .transientVariable("throwCustomExceptionFutureDelegate", new ThrowCustomExceptionFutureDelegate())
                .start())
                .as("exception expected, as there is no matching exception map")
                .isInstanceOf(Exception.class);
        assertThat(FlagDelegate.isVisited()).isFalse();
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/bpmn/event/error/mapError/BoundaryErrorMapTest.testRootCauseSingleDirectMap.bpmn20.xml")
    public void testRootCauseSingleDirectMapNotMatchingException() {
        FlagDelegate.reset();

        Map<String, Object> vars = new HashMap<>();
        vars.put("exceptionClass", BoundaryErrorParentException.class.getName());
        vars.put("nestedExceptionClass", IllegalStateException.class.getName());
        assertThat(ServiceTaskTestMock.CALL_COUNT.get()).isZero();

        assertThatThrownBy(() -> runtimeService.startProcessInstanceByKey("processWithSingleExceptionMap", vars))
                .as("exception expected, as there is no matching exception map")
                .isInstanceOf(Exception.class);
        assertThat(FlagDelegate.isVisited()).isFalse();
    }

    // exception matches by inheritance
    @Test
    @Deployment(resources = "org/flowable/engine/test/bpmn/event/error/mapError/BoundaryErrorMapTest.testClassDelegateSingleInheritedMapWithRootCause.bpmn20.xml")
    public void testClassDelegateSingleInheritedMapWithRootCauseNotMatchingException() {
        FlagDelegate.reset();

        Map<String, Object> vars = new HashMap<>();
        vars.put("exceptionClass", BoundaryEventChildException.class.getName());
        vars.put("nestedExceptionClass", IllegalStateException.class.getName());
        assertThat(ServiceTaskTestMock.CALL_COUNT.get()).isZero();

        assertThatThrownBy(() -> runtimeService.startProcessInstanceByKey("processWithSingleExceptionMap", vars))
                .as("exception expected, as there is no matching exception map")
                .isInstanceOf(Exception.class);
        assertThat(FlagDelegate.isVisited()).isFalse();
    }

    // exception matches by inheritance
    @Test
    @Deployment
    public void testClassDelegateSingleInheritedMap() {
        Map<String, Object> vars = new HashMap<>();
        vars.put("exceptionClass", BoundaryEventChildException.class.getName());
        FlagDelegate.reset();

        runtimeService.startProcessInstanceByKey("processWithSingleExceptionMap", vars);
        assertThat(FlagDelegate.isVisited()).isTrue();
    }

    // exception matches by inheritance
    @Test
    @Deployment
    public void testClassDelegateSingleInheritedMapWithRootCause() {
        Map<String, Object> vars = new HashMap<>();
        vars.put("exceptionClass", BoundaryEventChildException.class.getName());
        vars.put("nestedExceptionClass", IllegalArgumentException.class.getName());
        FlagDelegate.reset();

        runtimeService.startProcessInstanceByKey("processWithSingleExceptionMap", vars);
        assertThat(FlagDelegate.isVisited()).isTrue();
    }

    // check the default map
    @Test
    @Deployment
    public void testClassDelegateDefaultMap() {
        Map<String, Object> vars = new HashMap<>();
        vars.put("exceptionClass", Exception.class.getName());
        FlagDelegate.reset();

        runtimeService.startProcessInstanceByKey("processWithSingleExceptionMap", vars);
        assertThat(FlagDelegate.isVisited()).isTrue();
    }

    @Test
    @Deployment
    public void testExpressionDefaultMap() {
        Map<String, Object> vars = new HashMap<>();
        vars.put("exceptionClass", Exception.class.getName());
        FlagDelegate.reset();

        runtimeService.startProcessInstanceByKey("processWithSingleExceptionMap", vars);
        assertThat(FlagDelegate.isVisited()).isTrue();
    }

    @Test
    @Deployment
    public void testDelegateExpressionDefaultMap() {
        Map<String, Object> vars = new HashMap<>();
        vars.put("exceptionClass", Exception.class.getName());
        FlagDelegate.reset();

        runtimeService.startProcessInstanceByKey("processWithSingleExceptionMap", vars);
        assertThat(FlagDelegate.isVisited()).isTrue();
    }

    @Test
    @Deployment
    public void testSeqMultInstanceSingleDirectMap() {
        FlagDelegate.reset();
        Map<String, Object> vars = new HashMap<>();
        vars.put("exceptionClass", BoundaryErrorParentException.class.getName());

        runtimeService.startProcessInstanceByKey("processWithSingleExceptionMap", vars);
        assertThat(FlagDelegate.isVisited()).isTrue();
    }

    @Test
    @Deployment
    public void testSubProcessSingleDirectMap() {
        FlagDelegate.reset();
        Map<String, Object> vars = new HashMap<>();
        vars.put("exceptionClass", BoundaryErrorParentException.class.getName());

        runtimeService.startProcessInstanceByKey("subprocssWithSingleExceptionMap", vars);
        assertThat(FlagDelegate.isVisited()).isTrue();
    }

    @Test
    @Deployment(resources = {
            "org/flowable/engine/test/bpmn/event/error/mapError/BoundaryErrorMapTest.testCallProcessSingleDirectMap.bpmn20.xml",
            "org/flowable/engine/test/bpmn/event/error/mapError/BoundaryErrorMapTest.testCallProcessCalee.bpmn20.xml" })
    public void testCallProcessSingleDirectMap() {
        FlagDelegate.reset();
        Map<String, Object> vars = new HashMap<>();
        vars.put("exceptionClass", BoundaryErrorParentException.class.getName());

        runtimeService.startProcessInstanceByKey("callProcssWithSingleExceptionMap", vars);
        assertThat(FlagDelegate.isVisited()).isTrue();
    }

    @Test
    @Deployment(resources = {
            "org/flowable/engine/test/bpmn/event/error/mapError/BoundaryErrorMapTest.testExpressionCallProcessSingleDirectMap.bpmn20.xml",
            "org/flowable/engine/test/bpmn/event/error/mapError/BoundaryErrorMapTest.testCallProcessExpressionSubProcess.bpmn20.xml" })
    public void testCallProcessExpressionSingleDirectMap() {
        FlagDelegate.reset();
        Map<String, Object> vars = new HashMap<>();
        vars.put("exceptionClass", BoundaryErrorParentException.class.getName());

        runtimeService.startProcessInstanceByKey("callProcssWithSingleExceptionMap", vars);
        assertThat(FlagDelegate.isVisited()).isTrue();
    }

    @Test
    @Deployment(resources = {
            "org/flowable/engine/test/bpmn/event/error/mapError/BoundaryErrorMapTest.testDelegateExpressionCallProcessSingleDirectMap.bpmn20.xml",
            "org/flowable/engine/test/bpmn/event/error/mapError/BoundaryErrorMapTest.testCallProcessDelegateExpressionSubProcess.bpmn20.xml" })
    public void testCallProcessDelegateExpressionSingleDirectMap() {
        FlagDelegate.reset();
        Map<String, Object> vars = new HashMap<>();
        vars.put("exceptionClass", BoundaryErrorParentException.class.getName());

        runtimeService.startProcessInstanceByKey("callProcssWithSingleExceptionMap", vars);
        assertThat(FlagDelegate.isVisited()).isTrue();
    }

}
