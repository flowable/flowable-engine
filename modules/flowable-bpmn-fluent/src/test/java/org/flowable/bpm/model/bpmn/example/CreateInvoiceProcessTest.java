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
package org.flowable.bpm.model.bpmn.example;

import static org.assertj.core.api.Assertions.assertThat;

import org.flowable.bpm.model.bpmn.BpmnModelBuilder;
import org.flowable.bpm.model.bpmn.BpmnModelInstance;
import org.flowable.bpm.model.bpmn.GatewayDirection;
import org.flowable.bpm.model.bpmn.instance.ServiceTask;
import org.flowable.bpm.model.bpmn.instance.UserTask;
import org.junit.Test;

/**
 * Create Invoice Process test.
 *
 * See {@code CreateProcessTest.png} in same directory for visual view of the process beging built.
 */
public class CreateInvoiceProcessTest {

    @Test
    public void createInvoiceProcess()
        throws Exception {
        BpmnModelInstance modelInstance = BpmnModelBuilder.createExecutableProcess("invoice")
                .name("BPMN API Invoice Process")
                .startEvent()
                    .name("Invoice received")
                .userTask()
                    .name("Assign Approver")
                    .flowableAssignee("demo")
                .userTask()
                    .id("approveInvoice")
                    .name("Approve Invoice")
                .exclusiveGateway()
                    .name("Invoice approved?")
                    .gatewayDirection(GatewayDirection.Diverging)
                .condition("yes", "${approved}")
                .userTask()
                    .id("Prepare Bank Transfer")
                    .name("Prepare Bank Transfer")
                    .flowableFormKey("embedded:app:forms/prepare-bank-transfer.html")
                    .flowableCandidateGroups("accounting")
                .serviceTask()
                    .id("Archive Invoice")
                    .name("Archive Invoice")
                    .flowableClass("org.flowable.bpm.model.bpmn.example.ArchiveInvoiceService")
                .endEvent()
                    .name("Invoice processed")
                .moveToLastGateway()
                .condition("no", "${!approved}")
                .userTask()
                    .name("Review Invoice")
                    .flowableAssignee("demo")
                .exclusiveGateway()
                    .name("Review successful?")
                    .gatewayDirection(GatewayDirection.Diverging)
                .condition("no", "${!clarified}")
                .endEvent()
                    .name("Invoice not processed")
                .moveToLastGateway()
                .condition("yes", "${clarified}")
                .connectTo("approveInvoice")
                .done();

        /*
         * To see the BPMN 2.0 process model XML on the console log uncomment the following line
         BpmnModelBuilder.writeModelToStream(System.out, modelInstance);
         */

        ServiceTask serviceTask = modelInstance.getModelElementById("Archive Invoice");
        assertThat(serviceTask.getFlowableClass()).isEqualTo("org.flowable.bpm.model.bpmn.example.ArchiveInvoiceService");
        UserTask userTask = modelInstance.getModelElementById("Prepare Bank Transfer");
        assertThat(userTask.getFlowableFormKey()).isEqualTo("embedded:app:forms/prepare-bank-transfer.html");
    }
}
