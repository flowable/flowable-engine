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
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.BPMN_ATTRIBUTE_CORRELATION_PROPERTY_REF;
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.BPMN_ELEMENT_CORRELATION_PROPERTY_BINDING;
import static org.flowable.bpm.model.xml.type.ModelElementTypeBuilder.ModelTypeInstanceProvider;

import org.flowable.bpm.model.bpmn.instance.BaseElement;
import org.flowable.bpm.model.bpmn.instance.CorrelationProperty;
import org.flowable.bpm.model.bpmn.instance.CorrelationPropertyBinding;
import org.flowable.bpm.model.xml.ModelBuilder;
import org.flowable.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import org.flowable.bpm.model.xml.type.ModelElementTypeBuilder;
import org.flowable.bpm.model.xml.type.child.ChildElement;
import org.flowable.bpm.model.xml.type.child.SequenceBuilder;
import org.flowable.bpm.model.xml.type.reference.AttributeReference;

/**
 * The BPMN correlationPropertyBinding element.
 */
public class CorrelationPropertyBindingImpl
        extends BaseElementImpl
        implements CorrelationPropertyBinding {

    protected static AttributeReference<CorrelationProperty> correlationPropertyRefAttribute;
    protected static ChildElement<DataPath> dataPathChild;

    public static void registerType(ModelBuilder modelBuilder) {
        ModelElementTypeBuilder typeBuilder = modelBuilder.defineType(CorrelationPropertyBinding.class, BPMN_ELEMENT_CORRELATION_PROPERTY_BINDING)
                .namespaceUri(BPMN20_NS)
                .extendsType(BaseElement.class)
                .instanceProvider(new ModelTypeInstanceProvider<CorrelationPropertyBinding>() {
                    @Override
                    public CorrelationPropertyBinding newInstance(ModelTypeInstanceContext instanceContext) {
                        return new CorrelationPropertyBindingImpl(instanceContext);
                    }
                });

        correlationPropertyRefAttribute = typeBuilder.stringAttribute(BPMN_ATTRIBUTE_CORRELATION_PROPERTY_REF)
                .required()
                .qNameAttributeReference(CorrelationProperty.class)
                .build();

        SequenceBuilder sequenceBuilder = typeBuilder.sequence();

        dataPathChild = sequenceBuilder.element(DataPath.class)
                .required()
                .build();

        typeBuilder.build();
    }

    public CorrelationPropertyBindingImpl(ModelTypeInstanceContext instanceContext) {
        super(instanceContext);
    }

    @Override
    public CorrelationProperty getCorrelationProperty() {
        return correlationPropertyRefAttribute.getReferenceTargetElement(this);
    }

    @Override
    public void setCorrelationProperty(CorrelationProperty correlationProperty) {
        correlationPropertyRefAttribute.setReferenceTargetElement(this, correlationProperty);
    }

    @Override
    public DataPath getDataPath() {
        return dataPathChild.getChild(this);
    }

    @Override
    public void setDataPath(DataPath dataPath) {
        dataPathChild.setChild(this, dataPath);
    }
}
