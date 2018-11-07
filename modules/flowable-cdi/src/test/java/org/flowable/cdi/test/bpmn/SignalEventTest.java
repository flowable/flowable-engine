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
package org.flowable.cdi.test.bpmn;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;

import javax.inject.Inject;
import javax.inject.Named;

import org.flowable.cdi.BusinessProcess;
import org.flowable.cdi.test.CdiFlowableTestCase;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.junit.Test;

public class SignalEventTest extends CdiFlowableTestCase {

    @Named
    public static class SignalReceivedDelegate implements JavaDelegate {

        @Inject
        private BusinessProcess businessProcess;

        @Override
        public void execute(DelegateExecution execution) {
            businessProcess.setVariable("processName", "catchSignal-visited (was " + businessProcess.getVariable("processName") + ")");
        }
    }

    @Named
    public static class SendSignalDelegate implements JavaDelegate {

        @Inject
        private RuntimeService runtimeService;

        @Inject
        private BusinessProcess businessProcess;

        @Override
        public void execute(DelegateExecution execution) {
            businessProcess.setVariable("processName", "throwSignal-visited (was " + businessProcess.getVariable("processName") + ")");

            String signalProcessInstanceId = (String) execution.getVariable("signalProcessInstanceId");
            String executionId = runtimeService.createExecutionQuery().processInstanceId(signalProcessInstanceId).signalEventSubscriptionName("alert").singleResult().getId();

            runtimeService.signalEventReceived("alert", executionId);
        }

    }

    @Test
    @Deployment(resources = {"org/flowable/cdi/test/bpmn/SignalEventTests.catchAlertSignalBoundaryWithReceiveTask.bpmn20.xml",
            "org/flowable/cdi/test/bpmn/SignalEventTests.throwAlertSignalWithDelegate.bpmn20.xml"})
    public void testSignalCatchBoundaryWithVariables() {
        HashMap<String, Object> variables1 = new HashMap<>();
        variables1.put("processName", "catchSignal");
        ProcessInstance piCatchSignal = runtimeService.startProcessInstanceByKey("catchSignal", variables1);

        HashMap<String, Object> variables2 = new HashMap<>();
        variables2.put("processName", "throwSignal");
        variables2.put("signalProcessInstanceId", piCatchSignal.getProcessInstanceId());
        ProcessInstance piThrowSignal = runtimeService.startProcessInstanceByKey("throwSignal", variables2);

        assertEquals(1, runtimeService.createExecutionQuery().processInstanceId(piCatchSignal.getProcessInstanceId()).activityId("receiveTask").count());
        assertEquals(1, runtimeService.createExecutionQuery().processInstanceId(piThrowSignal.getProcessInstanceId()).activityId("receiveTask").count());

        assertEquals("catchSignal-visited (was catchSignal)", runtimeService.getVariable(piCatchSignal.getId(), "processName"));
        assertEquals("throwSignal-visited (was throwSignal)", runtimeService.getVariable(piThrowSignal.getId(), "processName"));

    }

}
