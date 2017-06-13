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
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.BPMN_ATTRIBUTE_TYPE;
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.BPMN_ELEMENT_CORRELATION_PROPERTY;
import static org.flowable.bpm.model.xml.type.ModelElementTypeBuilder.ModelTypeInstanceProvider;

import org.flowable.bpm.model.bpmn.instance.CorrelationProperty;
import org.flowable.bpm.model.bpmn.instance.CorrelationPropertyRetrievalExpression;
import org.flowable.bpm.model.bpmn.instance.ItemDefinition;
import org.flowable.bpm.model.bpmn.instance.RootElement;
import org.flowable.bpm.model.xml.ModelBuilder;
import org.flowable.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import org.flowable.bpm.model.xml.type.ModelElementTypeBuilder;
import org.flowable.bpm.model.xml.type.attribute.Attribute;
import org.flowable.bpm.model.xml.type.child.ChildElementCollection;
import org.flowable.bpm.model.xml.type.child.SequenceBuilder;
import org.flowable.bpm.model.xml.type.reference.AttributeReference;

import java.util.Collection;

/**
 * The BPMN correlationProperty element.
 */
public class CorrelationPropertyImpl
        extends RootElementImpl
        implements CorrelationProperty {

    protected static Attribute<String> nameAttribute;
    protected static AttributeReference<ItemDefinition> typeAttribute;
    protected static ChildElementCollection<CorrelationPropertyRetrievalExpression> correlationPropertyRetrievalExpressionCollection;

    public static void registerType(ModelBuilder modelBuilder) {
        ModelElementTypeBuilder typeBuilder;
        typeBuilder = modelBuilder.defineType(CorrelationProperty.class, BPMN_ELEMENT_CORRELATION_PROPERTY)
                .namespaceUri(BPMN20_NS)
                .extendsType(RootElement.class)
                .instanceProvider(new ModelTypeInstanceProvider<CorrelationProperty>() {
                    @Override
                    public CorrelationProperty newInstance(ModelTypeInstanceContext instanceContext) {
                        return new CorrelationPropertyImpl(instanceContext);
                    }
                });

        nameAttribute = typeBuilder.stringAttribute(BPMN_ATTRIBUTE_NAME)
                .build();

        typeAttribute = typeBuilder.stringAttribute(BPMN_ATTRIBUTE_TYPE)
                .qNameAttributeReference(ItemDefinition.class)
                .build();

        SequenceBuilder sequenceBuilder = typeBuilder.sequence();

        correlationPropertyRetrievalExpressionCollection = sequenceBuilder
                .elementCollection(CorrelationPropertyRetrievalExpression.class)
                .required()
                .build();

        typeBuilder.build();
    }

    public CorrelationPropertyImpl(ModelTypeInstanceContext context) {
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
    public ItemDefinition getType() {
        return typeAttribute.getReferenceTargetElement(this);
    }

    @Override
    public void setType(ItemDefinition type) {
        typeAttribute.setReferenceTargetElement(this, type);
    }

    @Override
    public Collection<CorrelationPropertyRetrievalExpression> getCorrelationPropertyRetrievalExpressions() {
        return correlationPropertyRetrievalExpressionCollection.get(this);
    }
}
