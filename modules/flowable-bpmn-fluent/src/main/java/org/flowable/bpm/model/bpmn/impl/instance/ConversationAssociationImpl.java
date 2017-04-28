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
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.BPMN_ATTRIBUTE_INNER_CONVERSATION_NODE_REF;
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.BPMN_ATTRIBUTE_OUTER_CONVERSATION_NODE_REF;
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.BPMN_ELEMENT_CONVERSATION_ASSOCIATION;
import static org.flowable.bpm.model.xml.type.ModelElementTypeBuilder.ModelTypeInstanceProvider;

import org.flowable.bpm.model.bpmn.instance.BaseElement;
import org.flowable.bpm.model.bpmn.instance.ConversationAssociation;
import org.flowable.bpm.model.bpmn.instance.ConversationNode;
import org.flowable.bpm.model.xml.ModelBuilder;
import org.flowable.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import org.flowable.bpm.model.xml.type.ModelElementTypeBuilder;
import org.flowable.bpm.model.xml.type.reference.AttributeReference;

/**
 * The BPMN conversationAssociation element.
 */
public class ConversationAssociationImpl
        extends BaseElementImpl
        implements ConversationAssociation {

    protected static AttributeReference<ConversationNode> innerConversationNodeRefAttribute;
    protected static AttributeReference<ConversationNode> outerConversationNodeRefAttribute;

    public static void registerType(ModelBuilder modelBuilder) {
        ModelElementTypeBuilder typeBuilder = modelBuilder.defineType(ConversationAssociation.class, BPMN_ELEMENT_CONVERSATION_ASSOCIATION)
                .namespaceUri(BPMN20_NS)
                .extendsType(BaseElement.class)
                .instanceProvider(new ModelTypeInstanceProvider<ConversationAssociation>() {
                    public ConversationAssociation newInstance(ModelTypeInstanceContext instanceContext) {
                        return new ConversationAssociationImpl(instanceContext);
                    }
                });

        innerConversationNodeRefAttribute = typeBuilder.stringAttribute(BPMN_ATTRIBUTE_INNER_CONVERSATION_NODE_REF)
                .required()
                .qNameAttributeReference(ConversationNode.class)
                .build();

        outerConversationNodeRefAttribute = typeBuilder.stringAttribute(BPMN_ATTRIBUTE_OUTER_CONVERSATION_NODE_REF)
                .required()
                .qNameAttributeReference(ConversationNode.class)
                .build();

        typeBuilder.build();
    }

    public ConversationAssociationImpl(ModelTypeInstanceContext instanceContext) {
        super(instanceContext);
    }

    public ConversationNode getInnerConversationNode() {
        return innerConversationNodeRefAttribute.getReferenceTargetElement(this);
    }

    public void setInnerConversationNode(ConversationNode innerConversationNode) {
        innerConversationNodeRefAttribute.setReferenceTargetElement(this, innerConversationNode);
    }

    public ConversationNode getOuterConversationNode() {
        return outerConversationNodeRefAttribute.getReferenceTargetElement(this);
    }

    public void setOuterConversationNode(ConversationNode outerConversationNode) {
        outerConversationNodeRefAttribute.setReferenceTargetElement(this, outerConversationNode);
    }
}
