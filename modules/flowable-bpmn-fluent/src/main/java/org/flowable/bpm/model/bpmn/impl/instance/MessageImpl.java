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
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.BPMN_ATTRIBUTE_ITEM_REF;
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.BPMN_ATTRIBUTE_NAME;
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.BPMN_ELEMENT_MESSAGE;

import org.flowable.bpm.model.bpmn.instance.ItemDefinition;
import org.flowable.bpm.model.bpmn.instance.Message;
import org.flowable.bpm.model.bpmn.instance.RootElement;
import org.flowable.bpm.model.xml.ModelBuilder;
import org.flowable.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import org.flowable.bpm.model.xml.type.ModelElementTypeBuilder;
import org.flowable.bpm.model.xml.type.attribute.Attribute;
import org.flowable.bpm.model.xml.type.reference.AttributeReference;

/**
 * The BPMN message event.
 */
public class MessageImpl
        extends RootElementImpl
        implements Message {

    protected static Attribute<String> nameAttribute;
    protected static AttributeReference<ItemDefinition> itemRefAttribute;

    public static void registerType(ModelBuilder modelBuilder) {
        ModelElementTypeBuilder typeBuilder = modelBuilder.defineType(Message.class, BPMN_ELEMENT_MESSAGE)
                .namespaceUri(BPMN20_NS)
                .extendsType(RootElement.class)
                .instanceProvider(new ModelElementTypeBuilder.ModelTypeInstanceProvider<Message>() {
                    public Message newInstance(ModelTypeInstanceContext instanceContext) {
                        return new MessageImpl(instanceContext);
                    }
                });

        nameAttribute = typeBuilder.stringAttribute(BPMN_ATTRIBUTE_NAME)
                .build();

        itemRefAttribute = typeBuilder.stringAttribute(BPMN_ATTRIBUTE_ITEM_REF)
                .qNameAttributeReference(ItemDefinition.class)
                .build();

        typeBuilder.build();
    }

    public MessageImpl(ModelTypeInstanceContext context) {
        super(context);
    }

    public String getName() {
        return nameAttribute.getValue(this);
    }

    public void setName(String name) {
        nameAttribute.setValue(this, name);
    }

    public ItemDefinition getItem() {
        return itemRefAttribute.getReferenceTargetElement(this);
    }

    public void setItem(ItemDefinition item) {
        itemRefAttribute.setReferenceTargetElement(this, item);
    }

}
