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
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.BPMN_ATTRIBUTE_NAME;
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.BPMN_ATTRIBUTE_SOURCE_REF;
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.BPMN_ATTRIBUTE_TARGET_REF;
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.BPMN_ELEMENT_CONVERSATION_LINK;
import static org.flowable.bpm.model.xml.type.ModelElementTypeBuilder.ModelTypeInstanceProvider;

import org.flowable.bpm.model.bpmn.instance.BaseElement;
import org.flowable.bpm.model.bpmn.instance.ConversationLink;
import org.flowable.bpm.model.bpmn.instance.InteractionNode;
import org.flowable.bpm.model.xml.ModelBuilder;
import org.flowable.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import org.flowable.bpm.model.xml.type.ModelElementTypeBuilder;
import org.flowable.bpm.model.xml.type.attribute.Attribute;
import org.flowable.bpm.model.xml.type.reference.AttributeReference;

/**
 * The BPMN conversationLink element.
 */
public class ConversationLinkImpl
        extends BaseElementImpl
        implements ConversationLink {

    protected static Attribute<String> nameAttribute;
    protected static AttributeReference<InteractionNode> sourceRefAttribute;
    protected static AttributeReference<InteractionNode> targetRefAttribute;

    public static void registerType(ModelBuilder modelBuilder) {
        ModelElementTypeBuilder typeBuilder = modelBuilder.defineType(ConversationLink.class, BPMN_ELEMENT_CONVERSATION_LINK)
                .namespaceUri(BPMN20_NS)
                .extendsType(BaseElement.class)
                .instanceProvider(new ModelTypeInstanceProvider<ConversationLink>() {
                    public ConversationLink newInstance(ModelTypeInstanceContext instanceContext) {
                        return new ConversationLinkImpl(instanceContext);
                    }
                });

        nameAttribute = typeBuilder.stringAttribute(BPMN_ATTRIBUTE_NAME)
                .build();

        sourceRefAttribute = typeBuilder.stringAttribute(BPMN_ATTRIBUTE_SOURCE_REF)
                .required()
                .qNameAttributeReference(InteractionNode.class)
                .build();

        targetRefAttribute = typeBuilder.stringAttribute(BPMN_ATTRIBUTE_TARGET_REF)
                .required()
                .qNameAttributeReference(InteractionNode.class)
                .build();

        typeBuilder.build();
    }

    public ConversationLinkImpl(ModelTypeInstanceContext instanceContext) {
        super(instanceContext);
    }

    public String getName() {
        return nameAttribute.getValue(this);
    }

    public void setName(String name) {
        nameAttribute.setValue(this, name);
    }

    public InteractionNode getSource() {
        return sourceRefAttribute.getReferenceTargetElement(this);
    }

    public void setSource(InteractionNode source) {
        sourceRefAttribute.setReferenceTargetElement(this, source);
    }

    public InteractionNode getTarget() {
        return targetRefAttribute.getReferenceTargetElement(this);
    }

    public void setTarget(InteractionNode target) {
        targetRefAttribute.setReferenceTargetElement(this, target);
    }
}
