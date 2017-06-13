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
package org.flowable.bpm.model.bpmn;

import static org.assertj.core.api.Assertions.assertThat;

import org.flowable.bpm.model.bpmn.instance.Definitions;
import org.flowable.bpm.model.bpmn.instance.EventDefinition;
import org.flowable.bpm.model.bpmn.instance.Message;
import org.flowable.bpm.model.bpmn.instance.MessageEventDefinition;
import org.flowable.bpm.model.bpmn.instance.Process;
import org.flowable.bpm.model.bpmn.instance.StartEvent;
import org.flowable.bpm.model.bpmn.util.BpmnModelResource;
import org.junit.Before;
import org.junit.Test;

import java.util.Collection;

public class ReferenceTest
        extends BpmnModelTest {

    private BpmnModelInstance testBpmnModelInstance;
    private Message message;
    private MessageEventDefinition messageEventDefinition;
    private StartEvent startEvent;

    @Before
    public void createModel() {
        testBpmnModelInstance = BpmnModelBuilder.createEmptyModel();
        Definitions definitions = testBpmnModelInstance.newInstance(Definitions.class);
        testBpmnModelInstance.setDefinitions(definitions);

        message = testBpmnModelInstance.newInstance(Message.class);
        message.setId("message-id");
        definitions.getRootElements().add(message);

        Process process = testBpmnModelInstance.newInstance(Process.class);
        process.setId("process-id");
        definitions.getRootElements().add(process);

        startEvent = testBpmnModelInstance.newInstance(StartEvent.class);
        startEvent.setId("start-event-id");
        process.getFlowElements().add(startEvent);

        messageEventDefinition = testBpmnModelInstance.newInstance(MessageEventDefinition.class);
        messageEventDefinition.setId("msg-def-id");
        messageEventDefinition.setMessage(message);
        startEvent.getEventDefinitions().add(messageEventDefinition);

        startEvent.getEventDefinitionRefs().add(messageEventDefinition);
    }

    @Test
    public void shouldUpdateReferenceOnIdChange() {
        assertThat(messageEventDefinition.getMessage()).isEqualTo(message);
        message.setId("changed-message-id");
        assertThat(message.getId()).isEqualTo("changed-message-id");
        assertThat(messageEventDefinition.getMessage()).isEqualTo(message);

        message.setAttributeValue("id", "another-message-id", true);
        assertThat(message.getId()).isEqualTo("another-message-id");
        assertThat(messageEventDefinition.getMessage()).isEqualTo(message);
    }

    @Test
    public void shouldRemoveReferenceIfReferencingElementIsRemoved() {
        assertThat(messageEventDefinition.getMessage()).isEqualTo(message);

        Definitions definitions = testBpmnModelInstance.getDefinitions();
        definitions.getRootElements().remove(message);

        assertThat(messageEventDefinition.getId()).isEqualTo("msg-def-id");
        assertThat(messageEventDefinition.getMessage()).isNull();
    }

    @Test
    public void shouldRemoveReferenceIfReferencingAttributeIsRemoved() {
        assertThat(messageEventDefinition.getMessage()).isEqualTo(message);

        message.removeAttribute("id");

        assertThat(messageEventDefinition.getId()).isEqualTo("msg-def-id");
        assertThat(messageEventDefinition.getMessage()).isNull();
    }

    @Test
    public void shouldUpdateReferenceIfReferencingElementIsReplaced() {
        assertThat(messageEventDefinition.getMessage()).isEqualTo(message);
        Message newMessage = testBpmnModelInstance.newInstance(Message.class);
        newMessage.setId("new-message-id");

        message.replaceWithElement(newMessage);

        assertThat(messageEventDefinition.getMessage()).isEqualTo(newMessage);
    }

    @Test
    public void shouldAddMessageEventDefinitionRef() {
        Collection<EventDefinition> eventDefinitionRefs = startEvent.getEventDefinitionRefs();
        assertThat(eventDefinitionRefs).isNotEmpty();
        assertThat(eventDefinitionRefs).contains(messageEventDefinition);
    }

    @Test
    public void shouldUpdateMessageEventDefinitionRefOnIdChange() {
        Collection<EventDefinition> eventDefinitionRefs = startEvent.getEventDefinitionRefs();
        assertThat(eventDefinitionRefs).contains(messageEventDefinition);
        messageEventDefinition.setId("changed-message-event-definition-id");
        assertThat(eventDefinitionRefs).contains(messageEventDefinition);
        messageEventDefinition.setAttributeValue("id", "another-message-event-definition-id", true);
    }

    @Test
    public void shouldRemoveMessageEventDefinitionRefIfMessageEventDefinitionIsRemoved() {
        startEvent.getEventDefinitions().remove(messageEventDefinition);
        Collection<EventDefinition> eventDefinitionRefs = startEvent.getEventDefinitionRefs();
        assertThat(eventDefinitionRefs).doesNotContain(messageEventDefinition);
        assertThat(eventDefinitionRefs).isEmpty();
    }

    @Test
    public void shouldReplaceMessageEventDefinitionRefIfMessageEventDefinitionIsReplaced() {
        MessageEventDefinition otherMessageEventDefinition = testBpmnModelInstance.newInstance(MessageEventDefinition.class);
        otherMessageEventDefinition.setId("other-message-event-definition-id");
        Collection<EventDefinition> eventDefinitionRefs = startEvent.getEventDefinitionRefs();
        assertThat(eventDefinitionRefs).contains(messageEventDefinition);
        messageEventDefinition.replaceWithElement(otherMessageEventDefinition);
        assertThat(eventDefinitionRefs).doesNotContain(messageEventDefinition);
        assertThat(eventDefinitionRefs).contains(otherMessageEventDefinition);
    }

    @Test
    public void shouldRemoveMessageEventDefinitionRefIfIdIsRemovedOfMessageEventDefinition() {
        Collection<EventDefinition> eventDefinitionRefs = startEvent.getEventDefinitionRefs();
        assertThat(eventDefinitionRefs).contains(messageEventDefinition);
        messageEventDefinition.removeAttribute("id");
        assertThat(eventDefinitionRefs).doesNotContain(messageEventDefinition);
        assertThat(eventDefinitionRefs).isEmpty();
    }

    @Test
    @BpmnModelResource
    public void shouldFindReferenceWithNamespace() {
        MessageEventDefinition messageEventDefinition = bpmnModelInstance.getModelElementById("message-event-definition");
        Message message = bpmnModelInstance.getModelElementById("message-id");
        assertThat(messageEventDefinition.getMessage()).isNotNull();
        assertThat(messageEventDefinition.getMessage()).isEqualTo(message);
        message.setId("changed-message");
        assertThat(messageEventDefinition.getMessage()).isNotNull();
        assertThat(messageEventDefinition.getMessage()).isEqualTo(message);
        message.setAttributeValue("id", "again-changed-message", true);
        assertThat(messageEventDefinition.getMessage()).isNotNull();
        assertThat(messageEventDefinition.getMessage()).isEqualTo(message);

        StartEvent startEvent = bpmnModelInstance.getModelElementById("start-event");
        Collection<EventDefinition> eventDefinitionRefs = startEvent.getEventDefinitionRefs();
        assertThat(eventDefinitionRefs).isNotEmpty();
        assertThat(eventDefinitionRefs).contains(messageEventDefinition);
        messageEventDefinition.setId("changed-message-event");
        assertThat(eventDefinitionRefs).isNotEmpty();
        assertThat(eventDefinitionRefs).contains(messageEventDefinition);
        messageEventDefinition.setAttributeValue("id", "again-changed-message-event", true);
        assertThat(eventDefinitionRefs).isNotEmpty();
        assertThat(eventDefinitionRefs).contains(messageEventDefinition);

        message.removeAttribute("id");
        assertThat(messageEventDefinition.getMessage()).isNull();
        messageEventDefinition.removeAttribute("id");
        assertThat(eventDefinitionRefs).doesNotContain(messageEventDefinition);
        assertThat(eventDefinitionRefs).isEmpty();
    }
}
