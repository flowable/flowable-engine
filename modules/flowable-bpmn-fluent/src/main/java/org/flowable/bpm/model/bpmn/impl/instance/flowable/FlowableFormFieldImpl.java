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
package org.flowable.bpm.model.bpmn.impl.instance.flowable;

import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.FLOWABLE_ATTRIBUTE_ID;
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.FLOWABLE_ATTRIBUTE_TYPE;
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.FLOWABLE_ELEMENT_FORM_FIELD;
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.FLOWABLE_NS;
import static org.flowable.bpm.model.xml.type.ModelElementTypeBuilder.ModelTypeInstanceProvider;

import org.flowable.bpm.model.bpmn.impl.instance.BpmnModelElementInstanceImpl;
import org.flowable.bpm.model.bpmn.instance.flowable.FlowableFormField;
import org.flowable.bpm.model.bpmn.instance.flowable.FlowableProperties;
import org.flowable.bpm.model.bpmn.instance.flowable.FlowableValidation;
import org.flowable.bpm.model.bpmn.instance.flowable.FlowableValue;
import org.flowable.bpm.model.xml.ModelBuilder;
import org.flowable.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import org.flowable.bpm.model.xml.type.ModelElementTypeBuilder;
import org.flowable.bpm.model.xml.type.attribute.Attribute;
import org.flowable.bpm.model.xml.type.child.ChildElement;
import org.flowable.bpm.model.xml.type.child.ChildElementCollection;
import org.flowable.bpm.model.xml.type.child.SequenceBuilder;

import java.util.Collection;

/**
 * The BPMN formField Flowable extension element.
 */
public class FlowableFormFieldImpl
        extends BpmnModelElementInstanceImpl
        implements FlowableFormField {

    protected static Attribute<String> flowableIdAttribute;
    protected static Attribute<String> flowableTypeAttribute;
    protected static ChildElement<FlowableProperties> flowablePropertiesChild;
    protected static ChildElement<FlowableValidation> flowableValidationChild;
    protected static ChildElementCollection<FlowableValue> flowableValueCollection;

    public static void registerType(ModelBuilder modelBuilder) {
        ModelElementTypeBuilder typeBuilder = modelBuilder.defineType(FlowableFormField.class, FLOWABLE_ELEMENT_FORM_FIELD)
                .namespaceUri(FLOWABLE_NS)
                .instanceProvider(new ModelTypeInstanceProvider<FlowableFormField>() {
                    @Override
                    public FlowableFormField newInstance(ModelTypeInstanceContext instanceContext) {
                        return new FlowableFormFieldImpl(instanceContext);
                    }
                });

        flowableIdAttribute = typeBuilder.stringAttribute(FLOWABLE_ATTRIBUTE_ID)
                .namespace(FLOWABLE_NS)
                .build();

        flowableTypeAttribute = typeBuilder.stringAttribute(FLOWABLE_ATTRIBUTE_TYPE)
                .namespace(FLOWABLE_NS)
                .build();

        SequenceBuilder sequenceBuilder = typeBuilder.sequence();

        flowablePropertiesChild = sequenceBuilder.element(FlowableProperties.class)
                .build();

        flowableValidationChild = sequenceBuilder.element(FlowableValidation.class)
                .build();

        flowableValueCollection = sequenceBuilder.elementCollection(FlowableValue.class)
                .build();

        typeBuilder.build();
    }

    public FlowableFormFieldImpl(ModelTypeInstanceContext instanceContext) {
        super(instanceContext);
    }

    @Override
    public String getFlowableId() {
        return flowableIdAttribute.getValue(this);
    }

    @Override
    public void setFlowableId(String flowableId) {
        flowableIdAttribute.setValue(this, flowableId);
    }

    @Override
    public String getFlowableType() {
        return flowableTypeAttribute.getValue(this);
    }

    @Override
    public void setFlowableType(String flowableType) {
        flowableTypeAttribute.setValue(this, flowableType);
    }

    @Override
    public FlowableProperties getFlowableProperties() {
        return flowablePropertiesChild.getChild(this);
    }

    @Override
    public void setFlowableProperties(FlowableProperties flowableProperties) {
        flowablePropertiesChild.setChild(this, flowableProperties);
    }

    @Override
    public FlowableValidation getFlowableValidation() {
        return flowableValidationChild.getChild(this);
    }

    @Override
    public void setFlowableValidation(FlowableValidation flowableValidation) {
        flowableValidationChild.setChild(this, flowableValidation);
    }

    @Override
    public Collection<FlowableValue> getFlowableValues() {
        return flowableValueCollection.get(this);
    }
}
