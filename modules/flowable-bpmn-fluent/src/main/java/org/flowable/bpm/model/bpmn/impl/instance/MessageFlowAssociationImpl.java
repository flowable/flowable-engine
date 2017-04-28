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
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.BPMN_ATTRIBUTE_INNER_MESSAGE_FLOW_REF;
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.BPMN_ATTRIBUTE_OUTER_MESSAGE_FLOW_REF;
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.BPMN_ELEMENT_MESSAGE_FLOW_ASSOCIATION;
import static org.flowable.bpm.model.xml.type.ModelElementTypeBuilder.ModelTypeInstanceProvider;

import org.flowable.bpm.model.bpmn.instance.BaseElement;
import org.flowable.bpm.model.bpmn.instance.MessageFlow;
import org.flowable.bpm.model.bpmn.instance.MessageFlowAssociation;
import org.flowable.bpm.model.xml.ModelBuilder;
import org.flowable.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import org.flowable.bpm.model.xml.type.ModelElementTypeBuilder;
import org.flowable.bpm.model.xml.type.reference.AttributeReference;

/**
 * The BPMN messageFlowAssociation element.
 */
public class MessageFlowAssociationImpl
        extends BaseElementImpl
        implements MessageFlowAssociation {

    protected static AttributeReference<MessageFlow> innerMessageFlowRefAttribute;
    protected static AttributeReference<MessageFlow> outerMessageFlowRefAttribute;

    public static void registerType(ModelBuilder modelBuilder) {
        ModelElementTypeBuilder typeBuilder = modelBuilder.defineType(MessageFlowAssociation.class, BPMN_ELEMENT_MESSAGE_FLOW_ASSOCIATION)
                .namespaceUri(BPMN20_NS)
                .extendsType(BaseElement.class)
                .instanceProvider(new ModelTypeInstanceProvider<MessageFlowAssociation>() {
                    public MessageFlowAssociation newInstance(ModelTypeInstanceContext instanceContext) {
                        return new MessageFlowAssociationImpl(instanceContext);
                    }
                });

        innerMessageFlowRefAttribute = typeBuilder.stringAttribute(BPMN_ATTRIBUTE_INNER_MESSAGE_FLOW_REF)
                .required()
                .qNameAttributeReference(MessageFlow.class)
                .build();

        outerMessageFlowRefAttribute = typeBuilder.stringAttribute(BPMN_ATTRIBUTE_OUTER_MESSAGE_FLOW_REF)
                .required()
                .qNameAttributeReference(MessageFlow.class)
                .build();

        typeBuilder.build();
    }

    public MessageFlowAssociationImpl(ModelTypeInstanceContext instanceContext) {
        super(instanceContext);
    }

    public MessageFlow getInnerMessageFlow() {
        return innerMessageFlowRefAttribute.getReferenceTargetElement(this);
    }

    public void setInnerMessageFlow(MessageFlow innerMessageFlow) {
        innerMessageFlowRefAttribute.setReferenceTargetElement(this, innerMessageFlow);
    }

    public MessageFlow getOuterMessageFlow() {
        return outerMessageFlowRefAttribute.getReferenceTargetElement(this);
    }

    public void setOuterMessageFlow(MessageFlow outerMessageFlow) {
        outerMessageFlowRefAttribute.setReferenceTargetElement(this, outerMessageFlow);
    }
}
