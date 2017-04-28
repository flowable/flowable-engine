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

import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.FLOWABLE_ATTRIBUTE_EXPRESSION;
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.FLOWABLE_ATTRIBUTE_ID;
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.FLOWABLE_ATTRIBUTE_NAME;
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.FLOWABLE_ATTRIBUTE_READABLE;
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.FLOWABLE_ATTRIBUTE_REQUIRED;
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.FLOWABLE_ATTRIBUTE_TYPE;
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.FLOWABLE_ATTRIBUTE_VARIABLE;
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.FLOWABLE_ATTRIBUTE_WRITEABLE;
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.FLOWABLE_ELEMENT_FORM_PROPERTY;
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.FLOWABLE_NS;
import static org.flowable.bpm.model.xml.type.ModelElementTypeBuilder.ModelTypeInstanceProvider;

import org.flowable.bpm.model.bpmn.impl.instance.BpmnModelElementInstanceImpl;
import org.flowable.bpm.model.bpmn.instance.flowable.FlowableFormProperty;
import org.flowable.bpm.model.bpmn.instance.flowable.FlowableValue;
import org.flowable.bpm.model.xml.ModelBuilder;
import org.flowable.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import org.flowable.bpm.model.xml.type.ModelElementTypeBuilder;
import org.flowable.bpm.model.xml.type.attribute.Attribute;
import org.flowable.bpm.model.xml.type.child.ChildElementCollection;
import org.flowable.bpm.model.xml.type.child.SequenceBuilder;

import java.util.Collection;

/**
 * The BPMN formProperty Flowable extension element.
 */
public class FlowableFormPropertyImpl
        extends BpmnModelElementInstanceImpl
        implements FlowableFormProperty {

    protected static Attribute<String> flowableIdAttribute;
    protected static Attribute<String> flowableNameAttribute;
    protected static Attribute<String> flowableTypeAttribute;
    protected static Attribute<Boolean> flowableRequiredAttribute;
    protected static Attribute<Boolean> flowableReadableAttribute;
    protected static Attribute<Boolean> flowableWriteableAttribute;
    protected static Attribute<String> flowableVariableAttribute;
    protected static Attribute<String> flowableExpressionAttribute;
    protected static ChildElementCollection<FlowableValue> flowableValueCollection;

    public static void registerType(ModelBuilder modelBuilder) {
        ModelElementTypeBuilder typeBuilder = modelBuilder.defineType(FlowableFormProperty.class, FLOWABLE_ELEMENT_FORM_PROPERTY)
                .namespaceUri(FLOWABLE_NS)
                .instanceProvider(new ModelTypeInstanceProvider<FlowableFormProperty>() {
                    public FlowableFormProperty newInstance(ModelTypeInstanceContext instanceContext) {
                        return new FlowableFormPropertyImpl(instanceContext);
                    }
                });

        flowableIdAttribute = typeBuilder.stringAttribute(FLOWABLE_ATTRIBUTE_ID)
                .namespace(FLOWABLE_NS)
                .build();

        flowableNameAttribute = typeBuilder.stringAttribute(FLOWABLE_ATTRIBUTE_NAME)
                .namespace(FLOWABLE_NS)
                .build();

        flowableTypeAttribute = typeBuilder.stringAttribute(FLOWABLE_ATTRIBUTE_TYPE)
                .namespace(FLOWABLE_NS)
                .build();

        flowableRequiredAttribute = typeBuilder.booleanAttribute(FLOWABLE_ATTRIBUTE_REQUIRED)
                .namespace(FLOWABLE_NS)
                .defaultValue(false)
                .build();

        flowableReadableAttribute = typeBuilder.booleanAttribute(FLOWABLE_ATTRIBUTE_READABLE)
                .namespace(FLOWABLE_NS)
                .defaultValue(true)
                .build();

        flowableWriteableAttribute = typeBuilder.booleanAttribute(FLOWABLE_ATTRIBUTE_WRITEABLE)
                .namespace(FLOWABLE_NS)
                .defaultValue(true)
                .build();

        flowableVariableAttribute = typeBuilder.stringAttribute(FLOWABLE_ATTRIBUTE_VARIABLE)
                .namespace(FLOWABLE_NS)
                .build();

        flowableExpressionAttribute = typeBuilder.stringAttribute(FLOWABLE_ATTRIBUTE_EXPRESSION)
                .namespace(FLOWABLE_NS)
                .build();

        SequenceBuilder sequenceBuilder = typeBuilder.sequence();

        flowableValueCollection = sequenceBuilder.elementCollection(FlowableValue.class)
                .build();

        typeBuilder.build();
    }

    public FlowableFormPropertyImpl(ModelTypeInstanceContext instanceContext) {
        super(instanceContext);
    }

    public String getFlowableId() {
        return flowableIdAttribute.getValue(this);
    }

    public void setFlowableId(String flowableId) {
        flowableIdAttribute.setValue(this, flowableId);
    }

    public String getFlowableName() {
        return flowableNameAttribute.getValue(this);
    }

    public void setFlowableName(String flowableName) {
        flowableNameAttribute.setValue(this, flowableName);
    }

    public String getFlowableType() {
        return flowableTypeAttribute.getValue(this);
    }

    public void setFlowableType(String flowableType) {
        flowableTypeAttribute.setValue(this, flowableType);
    }

    public boolean isFlowableRequired() {
        return flowableRequiredAttribute.getValue(this);
    }

    public void setFlowableRequired(boolean isFlowableRequired) {
        flowableRequiredAttribute.setValue(this, isFlowableRequired);
    }

    public boolean isFlowableReadable() {
        return flowableReadableAttribute.getValue(this);
    }

    public void setFlowableReadable(boolean isFlowableReadable) {
        flowableReadableAttribute.setValue(this, isFlowableReadable);
    }

    public boolean isFlowableWriteable() {
        return flowableWriteableAttribute.getValue(this);
    }

    public void setFlowableWriteable(boolean isFlowableWriteable) {
        flowableWriteableAttribute.setValue(this, isFlowableWriteable);
    }

    public String getFlowableVariable() {
        return flowableVariableAttribute.getValue(this);
    }

    public void setFlowableVariable(String flowableVariable) {
        flowableVariableAttribute.setValue(this, flowableVariable);
    }

    public String getFlowableExpression() {
        return flowableExpressionAttribute.getValue(this);
    }

    public void setFlowableExpression(String flowableExpression) {
        flowableExpressionAttribute.setValue(this, flowableExpression);
    }

    public Collection<FlowableValue> getFlowableValues() {
        return flowableValueCollection.get(this);
    }
}
