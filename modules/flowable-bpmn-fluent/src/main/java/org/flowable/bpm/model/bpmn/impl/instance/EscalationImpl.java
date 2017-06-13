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
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.BPMN_ATTRIBUTE_ESCALATION_CODE;
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.BPMN_ATTRIBUTE_NAME;
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.BPMN_ATTRIBUTE_STRUCTURE_REF;
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.BPMN_ELEMENT_ESCALATION;
import static org.flowable.bpm.model.xml.type.ModelElementTypeBuilder.ModelTypeInstanceProvider;

import org.flowable.bpm.model.bpmn.instance.Escalation;
import org.flowable.bpm.model.bpmn.instance.ItemDefinition;
import org.flowable.bpm.model.bpmn.instance.RootElement;
import org.flowable.bpm.model.xml.ModelBuilder;
import org.flowable.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import org.flowable.bpm.model.xml.type.ModelElementTypeBuilder;
import org.flowable.bpm.model.xml.type.attribute.Attribute;
import org.flowable.bpm.model.xml.type.reference.AttributeReference;

/**
 * The BPMN escalation element.
 */
public class EscalationImpl
        extends RootElementImpl
        implements Escalation {

    protected static Attribute<String> nameAttribute;
    protected static Attribute<String> escalationCodeAttribute;
    protected static AttributeReference<ItemDefinition> structureRefAttribute;

    public static void registerType(ModelBuilder modelBuilder) {
        ModelElementTypeBuilder typeBuilder = modelBuilder.defineType(Escalation.class, BPMN_ELEMENT_ESCALATION)
                .namespaceUri(BPMN20_NS)
                .extendsType(RootElement.class)
                .instanceProvider(new ModelTypeInstanceProvider<Escalation>() {
                    @Override
                    public Escalation newInstance(ModelTypeInstanceContext instanceContext) {
                        return new EscalationImpl(instanceContext);
                    }
                });

        nameAttribute = typeBuilder.stringAttribute(BPMN_ATTRIBUTE_NAME)
                .build();

        escalationCodeAttribute = typeBuilder.stringAttribute(BPMN_ATTRIBUTE_ESCALATION_CODE)
                .build();

        structureRefAttribute = typeBuilder.stringAttribute(BPMN_ATTRIBUTE_STRUCTURE_REF)
                .qNameAttributeReference(ItemDefinition.class)
                .build();

        typeBuilder.build();
    }

    public EscalationImpl(ModelTypeInstanceContext context) {
        super(context);
    }

    @Override
    public String getName() {
        return nameAttribute.getValue(this);
    }

    @Override
    public void setName(String name) {
        nameAttribute.setValue(this, name);
    }

    @Override
    public String getEscalationCode() {
        return escalationCodeAttribute.getValue(this);
    }

    @Override
    public void setEscalationCode(String escalationCode) {
        escalationCodeAttribute.setValue(this, escalationCode);
    }

    @Override
    public ItemDefinition getStructure() {
        return structureRefAttribute.getReferenceTargetElement(this);
    }

    @Override
    public void setStructure(ItemDefinition structure) {
        structureRefAttribute.setReferenceTargetElement(this, structure);
    }

}
