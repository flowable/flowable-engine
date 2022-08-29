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
package org.flowable.examples.bpmn.error;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

import org.flowable.common.engine.impl.util.CollectionUtil;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.junit.jupiter.api.Test;

/**
 * Tests throwing and handling for BPMN Errors under different scenarios
 *
 * @author Arthur Hupka-Merle
 */
public class BpmnErrorTest extends PluggableFlowableTestCase {

    /**
     * Tests that BpmnError thrown in <i>script task</i> is correctly propagated in the engine and
     * handled by the error boundary event.
     */
    @Test
    @Deployment
    public void testScriptTaskThrowsBpmnErrorHandledByErrorBoundaryEvent() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("simpleErrorProcess", CollectionUtil.singletonMap("throwError", "true"));
        assertThat(processInstance.getProcessVariables()).contains(entry("error", "error_handled"));
    }

    /**
     * Tests that BpmnError thrown in <i>execution end listener</i>  is correctly propagated in the engine and
     * handled by the error boundary event.
     */
    @Test
    @Deployment
    public void testScriptTaskExecutionEndListenerThrowsBpmnErrorHandledByErrorBoundaryEvent() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("simpleErrorProcess");
        assertThat(processInstance.getProcessVariables()).contains(entry("error", "error_handled"));
        assertThat(processInstance.getProcessVariables())
                .describedAs("We are in 'end' event. The actual activity should have been called.").contains(entry("scriptResult", "success"));
    }

    /**
     * Tests that BpmnError thrown in <i>execution start listener</i> is correctly propagated in the engine and
     * handled by the error boundary event.
     */
    @Test
    @Deployment
    public void testScriptTaskExecutionStartListenerThrowsBpmnErrorHandledByErrorBoundaryEvent() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("simpleErrorProcess");
        assertThat(processInstance.getProcessVariables()).contains(entry("error", "error_handled"));
        assertThat(processInstance.getProcessVariables())
                .describedAs("We are in 'start' event. The actual activity is not called.").doesNotContainKey("scriptResult");
    }

    /**
     * Tests that an event subprocess is triggered in case a task throws an BPMN Error.
     * The event subprocess is triggered by an error start event, with a <code>flowable:errorVariableName="error_code"</code>
     * custom attribute, allowing to store the BPMN error code as variable to use it as
     * condition expression in gateways to handle specific errors.
     */
    @Test
    @Deployment
    public void testErrorHandlingSubProcess() {
        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder().processDefinitionKey("errorHandlingSubProcess")
                .variable("throwErrorCode", "ERROR_A").start();
        assertThat(processInstance.getProcessVariables()).contains(entry("handled_error", "ERROR_A_special"));

        processInstance = runtimeService.createProcessInstanceBuilder().processDefinitionKey("errorHandlingSubProcess")
                .variable("throwErrorCode", "ERROR_B").start();
        assertThat(processInstance.getProcessVariables()).contains(entry("handled_error", "ERROR_B_special"));

        processInstance = runtimeService.createProcessInstanceBuilder().processDefinitionKey("errorHandlingSubProcess")
                .variable("throwErrorCode", "ERROR_UNKNOWN").start();
        assertThat(processInstance.getProcessVariables()).contains(entry("handled_error", "ERROR_UNKNOWN"));

        processInstance = runtimeService.createProcessInstanceBuilder().processDefinitionKey("errorHandlingSubProcess").start();
        assertThat(processInstance.getProcessVariables()).doesNotContainKey("handled_error");
    }

}
