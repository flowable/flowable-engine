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
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.FLOWABLE_ATTRIBUTE_NAME;
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.FLOWABLE_ATTRIBUTE_STRING_VALUE;
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.FLOWABLE_ELEMENT_FIELD;
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.FLOWABLE_NS;
import static org.flowable.bpm.model.xml.type.ModelElementTypeBuilder.ModelTypeInstanceProvider;

import org.flowable.bpm.model.bpmn.impl.instance.BpmnModelElementInstanceImpl;
import org.flowable.bpm.model.bpmn.instance.flowable.FlowableExpression;
import org.flowable.bpm.model.bpmn.instance.flowable.FlowableField;
import org.flowable.bpm.model.bpmn.instance.flowable.FlowableString;
import org.flowable.bpm.model.xml.ModelBuilder;
import org.flowable.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import org.flowable.bpm.model.xml.type.ModelElementTypeBuilder;
import org.flowable.bpm.model.xml.type.attribute.Attribute;
import org.flowable.bpm.model.xml.type.child.ChildElement;
import org.flowable.bpm.model.xml.type.child.SequenceBuilder;

/**
 * The BPMN field Flowable extension element.
 */
public class FlowableFieldImpl
        extends BpmnModelElementInstanceImpl
        implements FlowableField {

    protected static Attribute<String> flowableNameAttribute;
    protected static Attribute<String> flowableExpressionAttribute;
    protected static Attribute<String> flowableStringValueAttribute;
    protected static ChildElement<FlowableExpression> flowableExpressionChild;
    protected static ChildElement<FlowableString> flowableStringChild;

    public static void registerType(ModelBuilder modelBuilder) {
        ModelElementTypeBuilder typeBuilder = modelBuilder.defineType(FlowableField.class, FLOWABLE_ELEMENT_FIELD)
                .namespaceUri(FLOWABLE_NS)
                .instanceProvider(new ModelTypeInstanceProvider<FlowableField>() {
                    public FlowableField newInstance(ModelTypeInstanceContext instanceContext) {
                        return new FlowableFieldImpl(instanceContext);
                    }
                });

        flowableNameAttribute = typeBuilder.stringAttribute(FLOWABLE_ATTRIBUTE_NAME)
                .namespace(FLOWABLE_NS)
                .build();

        flowableExpressionAttribute = typeBuilder.stringAttribute(FLOWABLE_ATTRIBUTE_EXPRESSION)
                .namespace(FLOWABLE_NS)
                .build();

        flowableStringValueAttribute = typeBuilder.stringAttribute(FLOWABLE_ATTRIBUTE_STRING_VALUE)
                .namespace(FLOWABLE_NS)
                .build();

        SequenceBuilder sequenceBuilder = typeBuilder.sequence();

        flowableExpressionChild = sequenceBuilder.element(FlowableExpression.class)
                .build();

        flowableStringChild = sequenceBuilder.element(FlowableString.class)
                .build();

        typeBuilder.build();
    }

    public FlowableFieldImpl(ModelTypeInstanceContext instanceContext) {
        super(instanceContext);
    }

    public String getFlowableName() {
        return flowableNameAttribute.getValue(this);
    }

    public void setFlowableName(String flowableName) {
        flowableNameAttribute.setValue(this, flowableName);
    }

    public String getFlowableExpression() {
        return flowableExpressionAttribute.getValue(this);
    }

    public void setFlowableExpression(String flowableExpression) {
        flowableExpressionAttribute.setValue(this, flowableExpression);
    }

    public String getFlowableStringValue() {
        return flowableStringValueAttribute.getValue(this);
    }

    public void setFlowableStringValue(String flowableStringValue) {
        flowableStringValueAttribute.setValue(this, flowableStringValue);
    }

    public FlowableString getFlowableString() {
        return flowableStringChild.getChild(this);
    }

    public void setFlowableString(FlowableString flowableString) {
        flowableStringChild.setChild(this, flowableString);
    }

    public FlowableExpression getFlowableExpressionChild() {
        return flowableExpressionChild.getChild(this);
    }

    public void setFlowableExpressionChild(FlowableExpression flowableExpression) {
        flowableExpressionChild.setChild(this, flowableExpression);
    }
}
