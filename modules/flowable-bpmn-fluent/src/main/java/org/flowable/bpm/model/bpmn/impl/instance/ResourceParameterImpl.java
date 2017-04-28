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
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.BPMN_ATTRIBUTE_IS_REQUIRED;
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.BPMN_ATTRIBUTE_NAME;
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.BPMN_ATTRIBUTE_TYPE;
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.BPMN_ELEMENT_RESOURCE_PARAMETER;
import static org.flowable.bpm.model.xml.type.ModelElementTypeBuilder.ModelTypeInstanceProvider;

import org.flowable.bpm.model.bpmn.instance.BaseElement;
import org.flowable.bpm.model.bpmn.instance.ItemDefinition;
import org.flowable.bpm.model.bpmn.instance.ResourceParameter;
import org.flowable.bpm.model.xml.ModelBuilder;
import org.flowable.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import org.flowable.bpm.model.xml.type.ModelElementTypeBuilder;
import org.flowable.bpm.model.xml.type.attribute.Attribute;
import org.flowable.bpm.model.xml.type.reference.AttributeReference;

/**
 * The BPMN resourceParameter element.
 */
public class ResourceParameterImpl
        extends BaseElementImpl
        implements ResourceParameter {

    protected static Attribute<String> nameAttribute;
    protected static AttributeReference<ItemDefinition> typeAttribute;
    protected static Attribute<Boolean> isRequiredAttribute;

    public static void registerType(ModelBuilder modelBuilder) {
        ModelElementTypeBuilder typeBuilder = modelBuilder.defineType(ResourceParameter.class, BPMN_ELEMENT_RESOURCE_PARAMETER)
                .namespaceUri(BPMN20_NS)
                .extendsType(BaseElement.class)
                .instanceProvider(new ModelTypeInstanceProvider<ResourceParameter>() {
                    @Override
                    public ResourceParameter newInstance(ModelTypeInstanceContext instanceContext) {
                        return new ResourceParameterImpl(instanceContext);
                    }
                });

        nameAttribute = typeBuilder.stringAttribute(BPMN_ATTRIBUTE_NAME)
                .build();

        typeAttribute = typeBuilder.stringAttribute(BPMN_ATTRIBUTE_TYPE)
                .qNameAttributeReference(ItemDefinition.class)
                .build();

        isRequiredAttribute = typeBuilder.booleanAttribute(BPMN_ATTRIBUTE_IS_REQUIRED)
                .build();

        typeBuilder.build();
    }

    public ResourceParameterImpl(ModelTypeInstanceContext instanceContext) {
        super(instanceContext);
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
    public ItemDefinition getType() {
        return typeAttribute.getReferenceTargetElement(this);
    }

    @Override
    public void setType(ItemDefinition type) {
        typeAttribute.setReferenceTargetElement(this, type);
    }

    @Override
    public boolean isRequired() {
        return isRequiredAttribute.getValue(this);
    }

    @Override
    public void setRequired(boolean isRequired) {
        isRequiredAttribute.setValue(this, isRequired);
    }
}
