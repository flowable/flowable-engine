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
package org.flowable.bpm.model.bpmn.instance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.FLOWABLE_NS;

import org.flowable.bpm.model.bpmn.BpmnModelBuilder;
import org.flowable.bpm.model.bpmn.BpmnModelInstance;
import org.flowable.bpm.model.bpmn.impl.instance.Incoming;
import org.flowable.bpm.model.bpmn.impl.instance.Outgoing;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collection;


public class FlowNodeTest
        extends BpmnModelElementInstanceTest {

    public TypeAssumption getTypeAssumption() {
        return new TypeAssumption(FlowElement.class, true);
    }

    public Collection<ChildElementAssumption> getChildElementAssumptions() {
        return Arrays.asList(
                new ChildElementAssumption(Incoming.class),
                new ChildElementAssumption(Outgoing.class));
    }

    public Collection<AttributeAssumption> getAttributesAssumptions() {
        return Arrays.asList(
                new AttributeAssumption(FLOWABLE_NS, "async", false, false, false),
                new AttributeAssumption(FLOWABLE_NS, "exclusive", false, false, true));
    }

    @Test
    public void updateIncomingOutgoingChildElements() {
        BpmnModelInstance modelInstance = BpmnModelBuilder.createProcess()
                .startEvent()
                .userTask("test")
                .endEvent()
                .done();

        // save current incoming and outgoing sequence flows
        UserTask userTask = modelInstance.getModelElementById("test");
        Collection<SequenceFlow> incoming = userTask.getIncoming();
        Collection<SequenceFlow> outgoing = userTask.getOutgoing();

        // create a new service task
        ServiceTask serviceTask = modelInstance.newInstance(ServiceTask.class);
        serviceTask.setId("new");

        // replace the user task with the new service task
        userTask.replaceWithElement(serviceTask);

        // assert that the new service task has the same incoming and outgoing sequence flows
        assertThat(serviceTask.getIncoming()).containsExactlyElementsOf(incoming);
        assertThat(serviceTask.getOutgoing()).containsExactlyElementsOf(outgoing);
    }

    @Test
    public void flowableAsync() {
        Task task = modelInstance.newInstance(Task.class);
        assertThat(task.isFlowableAsync()).isFalse();

        task.setFlowableAsync(true);
        assertThat(task.isFlowableAsync()).isTrue();
    }

    @Test
    public void flowableExclusive() {
        Task task = modelInstance.newInstance(Task.class);

        assertThat(task.isFlowableExclusive()).isTrue();

        task.setFlowableExclusive(false);

        assertThat(task.isFlowableExclusive()).isFalse();
    }
}
