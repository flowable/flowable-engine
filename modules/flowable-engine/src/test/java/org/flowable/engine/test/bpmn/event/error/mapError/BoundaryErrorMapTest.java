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

import java.util.HashMap;
import java.util.Map;

import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.test.Deployment;
import org.flowable.standalone.testing.helpers.ServiceTaskTestMock;

/**
 * @author Saeid Mirzaei
 */
public class BoundaryErrorMapTest extends PluggableFlowableTestCase {

    // exception matches the only mapping, directly
    @Deployment
    public void testClassDelegateSingleDirectMap() {
        FlagDelegate.reset();
        Map<String, Object> vars = new HashMap<>();
        vars.put("exceptionClass", BoundaryErrorParentException.class.getName());

        runtimeService.startProcessInstanceByKey("processWithSingleExceptionMap", vars);
        assertTrue(FlagDelegate.isVisited());
    }

    @Deployment
    public void testExpressionSingleDirectMap() {
        FlagDelegate.reset();
        Map<String, Object> vars = new HashMap<>();
        vars.put("exceptionClass", BoundaryErrorParentException.class.getName());

        runtimeService.startProcessInstanceByKey("processWithSingleExceptionMap", vars);
        assertTrue(FlagDelegate.isVisited());
    }

    @Deployment
    public void testDelegateExpressionSingleDirectMap() {
        FlagDelegate.reset();
        Map<String, Object> vars = new HashMap<>();
        vars.put("exceptionClass", BoundaryErrorParentException.class.getName());

        runtimeService.startProcessInstanceByKey("processWithSingleExceptionMap", vars);
        assertTrue(FlagDelegate.isVisited());
    }

    // exception does not match the single mapping
    @Deployment(resources = "org/flowable/engine/test/bpmn/event/error/mapError/BoundaryErrorMapTest.testClassDelegateSingleDirectMap.bpmn20.xml")
    public void testClassDelegateSingleDirectMapNotMatchingException() {
        FlagDelegate.reset();

        Map<String, Object> vars = new HashMap<>();
        vars.put("exceptionClass", IllegalStateException.class.getName());
        assertEquals(0, ServiceTaskTestMock.CALL_COUNT.get());

        try {
            runtimeService.startProcessInstanceByKey("processWithSingleExceptionMap", vars);
            fail("exception expected, as there is no matching exception map");
        } catch (Exception e) {
            assertFalse(FlagDelegate.isVisited());
        }
    }

    @Deployment(resources = "org/flowable/engine/test/bpmn/event/error/mapError/BoundaryErrorMapTest.testExpressionSingleDirectMap.bpmn20.xml")
    public void testExpressionSingleDirectMapNotMatchingException() {
        FlagDelegate.reset();

        Map<String, Object> vars = new HashMap<>();
        vars.put("exceptionClass", IllegalArgumentException.class.getName());
        assertEquals(0, ServiceTaskTestMock.CALL_COUNT.get());

        try {
            runtimeService.startProcessInstanceByKey("processWithSingleExceptionMap", vars);
            fail("exception expected, as there is no matching exception map");
        } catch (Exception e) {
            assertFalse(FlagDelegate.isVisited());
        }
    }

    @Deployment(resources = "org/flowable/engine/test/bpmn/event/error/mapError/BoundaryErrorMapTest.testDelegateExpressionSingleDirectMap.bpmn20.xml")
    public void testDelegateExpressionSingleDirectMapNotMatchingException() {
        FlagDelegate.reset();

        Map<String, Object> vars = new HashMap<>();
        vars.put("exceptionClass", IllegalArgumentException.class.getName());
        assertEquals(0, ServiceTaskTestMock.CALL_COUNT.get());

        try {
            runtimeService.startProcessInstanceByKey("processWithSingleExceptionMap", vars);
            fail("exception expected, as there is no matching exception map");
        } catch (Exception e) {
            assertFalse(FlagDelegate.isVisited());
        }
    }

    // exception matches by inheritance
    @Deployment
    public void testClassDelegateSingleInheritedMap() {
        Map<String, Object> vars = new HashMap<>();
        vars.put("exceptionClass", BoundaryEventChildException.class.getName());
        FlagDelegate.reset();

        runtimeService.startProcessInstanceByKey("processWithSingleExceptionMap", vars);
        assertTrue(FlagDelegate.isVisited());
    }

    // check the default map
    @Deployment
    public void testClassDelegateDefaultMap() {
        Map<String, Object> vars = new HashMap<>();
        vars.put("exceptionClass", Exception.class.getName());
        FlagDelegate.reset();

        runtimeService.startProcessInstanceByKey("processWithSingleExceptionMap", vars);
        assertTrue(FlagDelegate.isVisited());
    }

    @Deployment
    public void testExpressionDefaultMap() {
        Map<String, Object> vars = new HashMap<>();
        vars.put("exceptionClass", Exception.class.getName());
        FlagDelegate.reset();

        runtimeService.startProcessInstanceByKey("processWithSingleExceptionMap", vars);
        assertTrue(FlagDelegate.isVisited());
    }

    @Deployment
    public void testDelegateExpressionDefaultMap() {
        Map<String, Object> vars = new HashMap<>();
        vars.put("exceptionClass", Exception.class.getName());
        FlagDelegate.reset();

        runtimeService.startProcessInstanceByKey("processWithSingleExceptionMap", vars);
        assertTrue(FlagDelegate.isVisited());
    }

    @Deployment
    public void testSeqMultInstanceSingleDirectMap() {
        FlagDelegate.reset();
        Map<String, Object> vars = new HashMap<>();
        vars.put("exceptionClass", BoundaryErrorParentException.class.getName());

        runtimeService.startProcessInstanceByKey("processWithSingleExceptionMap", vars);
        assertTrue(FlagDelegate.isVisited());
    }

    @Deployment
    public void testSubProcessSingleDirectMap() {
        FlagDelegate.reset();
        Map<String, Object> vars = new HashMap<>();
        vars.put("exceptionClass", BoundaryErrorParentException.class.getName());

        runtimeService.startProcessInstanceByKey("subprocssWithSingleExceptionMap", vars);
        assertTrue(FlagDelegate.isVisited());
    }

    @Deployment(resources = {
            "org/flowable/engine/test/bpmn/event/error/mapError/BoundaryErrorMapTest.testCallProcessSingleDirectMap.bpmn20.xml",
            "org/flowable/engine/test/bpmn/event/error/mapError/BoundaryErrorMapTest.testCallProcessCalee.bpmn20.xml" })
    public void testCallProcessSingleDirectMap() {
        FlagDelegate.reset();
        Map<String, Object> vars = new HashMap<>();
        vars.put("exceptionClass", BoundaryErrorParentException.class.getName());

        runtimeService.startProcessInstanceByKey("callProcssWithSingleExceptionMap", vars);
        assertTrue(FlagDelegate.isVisited());
    }

    @Deployment(resources = {
            "org/flowable/engine/test/bpmn/event/error/mapError/BoundaryErrorMapTest.testExpressionCallProcessSingleDirectMap.bpmn20.xml",
            "org/flowable/engine/test/bpmn/event/error/mapError/BoundaryErrorMapTest.testCallProcessExpressionSubProcess.bpmn20.xml" })
    public void testCallProcessExpressionSingleDirectMap() {
        FlagDelegate.reset();
        Map<String, Object> vars = new HashMap<>();
        vars.put("exceptionClass", BoundaryErrorParentException.class.getName());

        runtimeService.startProcessInstanceByKey("callProcssWithSingleExceptionMap", vars);
        assertTrue(FlagDelegate.isVisited());
    }

    @Deployment(resources = {
            "org/flowable/engine/test/bpmn/event/error/mapError/BoundaryErrorMapTest.testDelegateExpressionCallProcessSingleDirectMap.bpmn20.xml",
            "org/flowable/engine/test/bpmn/event/error/mapError/BoundaryErrorMapTest.testCallProcessDelegateExpressionSubProcess.bpmn20.xml" })
    public void testCallProcessDelegateExpressionSingleDirectMap() {
        FlagDelegate.reset();
        Map<String, Object> vars = new HashMap<>();
        vars.put("exceptionClass", BoundaryErrorParentException.class.getName());

        runtimeService.startProcessInstanceByKey("callProcssWithSingleExceptionMap", vars);
        assertTrue(FlagDelegate.isVisited());
    }

}
