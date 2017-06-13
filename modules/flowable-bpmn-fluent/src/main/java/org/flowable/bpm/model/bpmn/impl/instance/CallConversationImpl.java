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
package org.flowable.bpm.model.bpmn.impl.instance;

import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.BPMN20_NS;
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.BPMN_ATTRIBUTE_CALLED_COLLABORATION_REF;
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.BPMN_ELEMENT_CALL_CONVERSATION;
import static org.flowable.bpm.model.xml.type.ModelElementTypeBuilder.ModelTypeInstanceProvider;

import org.flowable.bpm.model.bpmn.instance.CallConversation;
import org.flowable.bpm.model.bpmn.instance.ConversationNode;
import org.flowable.bpm.model.bpmn.instance.GlobalConversation;
import org.flowable.bpm.model.bpmn.instance.ParticipantAssociation;
import org.flowable.bpm.model.xml.ModelBuilder;
import org.flowable.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import org.flowable.bpm.model.xml.type.ModelElementTypeBuilder;
import org.flowable.bpm.model.xml.type.child.ChildElementCollection;
import org.flowable.bpm.model.xml.type.child.SequenceBuilder;
import org.flowable.bpm.model.xml.type.reference.AttributeReference;

import java.util.Collection;

/**
 * The BPMN callConversation element.
 */
public class CallConversationImpl
        extends ConversationNodeImpl
        implements CallConversation {

    protected static AttributeReference<GlobalConversation> calledCollaborationRefAttribute;
    protected static ChildElementCollection<ParticipantAssociation> participantAssociationCollection;

    public static void registerType(ModelBuilder modelBuilder) {
        ModelElementTypeBuilder typeBuilder = modelBuilder.defineType(CallConversation.class, BPMN_ELEMENT_CALL_CONVERSATION)
                .namespaceUri(BPMN20_NS)
                .extendsType(ConversationNode.class)
                .instanceProvider(new ModelTypeInstanceProvider<CallConversation>() {
                    @Override
                    public CallConversation newInstance(ModelTypeInstanceContext instanceContext) {
                        return new CallConversationImpl(instanceContext);
                    }
                });

        calledCollaborationRefAttribute = typeBuilder.stringAttribute(BPMN_ATTRIBUTE_CALLED_COLLABORATION_REF)
                .qNameAttributeReference(GlobalConversation.class)
                .build();

        SequenceBuilder sequenceBuilder = typeBuilder.sequence();

        participantAssociationCollection = sequenceBuilder.elementCollection(ParticipantAssociation.class)
                .build();

        typeBuilder.build();
    }

    public CallConversationImpl(ModelTypeInstanceContext instanceContext) {
        super(instanceContext);
    }

    @Override
    public GlobalConversation getCalledCollaboration() {
        return calledCollaborationRefAttribute.getReferenceTargetElement(this);
    }

    @Override
    public void setCalledCollaboration(GlobalConversation calledCollaboration) {
        calledCollaborationRefAttribute.setReferenceTargetElement(this, calledCollaboration);
    }

    @Override
    public Collection<ParticipantAssociation> getParticipantAssociations() {
        return participantAssociationCollection.get(this);
    }
}
