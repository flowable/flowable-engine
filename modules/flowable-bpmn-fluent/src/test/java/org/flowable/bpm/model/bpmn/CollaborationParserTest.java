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

import org.flowable.bpm.model.bpmn.instance.Collaboration;
import org.flowable.bpm.model.bpmn.instance.Conversation;
import org.flowable.bpm.model.bpmn.instance.ConversationLink;
import org.flowable.bpm.model.bpmn.instance.ConversationNode;
import org.flowable.bpm.model.bpmn.instance.Event;
import org.flowable.bpm.model.bpmn.instance.MessageFlow;
import org.flowable.bpm.model.bpmn.instance.Participant;
import org.flowable.bpm.model.bpmn.instance.ServiceTask;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Collection;

public class CollaborationParserTest {

    private static BpmnModelInstance modelInstance;
    private static Collaboration collaboration;

    @BeforeClass
    public static void parseModel() {
        modelInstance = BpmnModelBuilder.readModelFromStream(CollaborationParserTest.class.getResourceAsStream("CollaborationParserTest.bpmn20.xml"));
        collaboration = modelInstance.getModelElementById("collaboration1");
    }

    @Test
    public void conversations() {
        assertThat(collaboration.getConversationNodes()).hasSize(1);

        ConversationNode conversationNode = collaboration.getConversationNodes().iterator().next();
        assertThat(conversationNode).isInstanceOf(Conversation.class);
        assertThat(conversationNode.getParticipants()).isEmpty();
        assertThat(conversationNode.getCorrelationKeys()).isEmpty();
        assertThat(conversationNode.getMessageFlows()).isEmpty();
    }

    @Test
    public void conversationLink() {
        Collection<ConversationLink> conversationLinks = collaboration.getConversationLinks();
        for (ConversationLink conversationLink : conversationLinks) {
            assertThat(conversationLink.getId()).startsWith("conversationLink");
            assertThat(conversationLink.getSource()).isInstanceOf(Participant.class);
            Participant source = (Participant) conversationLink.getSource();
            assertThat(source.getName()).isEqualTo("Pool");
            assertThat(source.getId()).startsWith("participant");

            assertThat(conversationLink.getTarget()).isInstanceOf(Conversation.class);
            Conversation target = (Conversation) conversationLink.getTarget();
            assertThat(target.getId()).isEqualTo("conversation1");
        }
    }

    @Test
    public void messageFlow() {
        Collection<MessageFlow> messageFlows = collaboration.getMessageFlows();
        for (MessageFlow messageFlow : messageFlows) {
            assertThat(messageFlow.getId()).startsWith("messageFlow");
            assertThat(messageFlow.getSource()).isInstanceOf(ServiceTask.class);
            assertThat(messageFlow.getTarget()).isInstanceOf(Event.class);
        }
    }

    @Test
    public void participant() {
        Collection<Participant> participants = collaboration.getParticipants();
        for (Participant participant : participants) {
            assertThat(participant.getProcess().getId()).startsWith("process");
        }
    }

    @Test
    public void unused() {
        assertThat(collaboration.getCorrelationKeys()).isEmpty();
        assertThat(collaboration.getArtifacts()).isEmpty();
        assertThat(collaboration.getConversationAssociations()).isEmpty();
        assertThat(collaboration.getMessageFlowAssociations()).isEmpty();
        assertThat(collaboration.getParticipantAssociations()).isEmpty();
    }


    @AfterClass
    public static void validateModel() {
        BpmnModelBuilder.validateModel(modelInstance);
    }

}
