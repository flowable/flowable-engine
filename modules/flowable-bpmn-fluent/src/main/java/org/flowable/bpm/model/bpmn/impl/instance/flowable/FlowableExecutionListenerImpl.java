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

import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.FLOWABLE_ATTRIBUTE_CLASS;
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.FLOWABLE_ATTRIBUTE_DELEGATE_EXPRESSION;
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.FLOWABLE_ATTRIBUTE_EVENT;
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.FLOWABLE_ATTRIBUTE_EXPRESSION;
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.FLOWABLE_ELEMENT_EXECUTION_LISTENER;
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.FLOWABLE_NS;
import static org.flowable.bpm.model.xml.type.ModelElementTypeBuilder.ModelTypeInstanceProvider;

import org.flowable.bpm.model.bpmn.impl.instance.BpmnModelElementInstanceImpl;
import org.flowable.bpm.model.bpmn.instance.flowable.FlowableExecutionListener;
import org.flowable.bpm.model.bpmn.instance.flowable.FlowableField;
import org.flowable.bpm.model.bpmn.instance.flowable.FlowableScript;
import org.flowable.bpm.model.xml.ModelBuilder;
import org.flowable.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import org.flowable.bpm.model.xml.type.ModelElementTypeBuilder;
import org.flowable.bpm.model.xml.type.attribute.Attribute;
import org.flowable.bpm.model.xml.type.child.ChildElement;
import org.flowable.bpm.model.xml.type.child.ChildElementCollection;
import org.flowable.bpm.model.xml.type.child.SequenceBuilder;

import java.util.Collection;

/**
 * The BPMN executionListener Flowable extension element.
 */
public class FlowableExecutionListenerImpl
        extends BpmnModelElementInstanceImpl
        implements FlowableExecutionListener {

    protected static Attribute<String> flowableEventAttribute;
    protected static Attribute<String> flowableClassAttribute;
    protected static Attribute<String> flowableExpressionAttribute;
    protected static Attribute<String> flowableDelegateExpressionAttribute;
    protected static ChildElementCollection<FlowableField> flowableFieldCollection;
    protected static ChildElement<FlowableScript> flowableScriptChild;

    public static void registerType(ModelBuilder modelBuilder) {
        ModelElementTypeBuilder typeBuilder = modelBuilder.defineType(FlowableExecutionListener.class, FLOWABLE_ELEMENT_EXECUTION_LISTENER)
                .namespaceUri(FLOWABLE_NS)
                .instanceProvider(new ModelTypeInstanceProvider<FlowableExecutionListener>() {
                    @Override
                    public FlowableExecutionListener newInstance(ModelTypeInstanceContext instanceContext) {
                        return new FlowableExecutionListenerImpl(instanceContext);
                    }
                });

        flowableEventAttribute = typeBuilder.stringAttribute(FLOWABLE_ATTRIBUTE_EVENT)
                .namespace(FLOWABLE_NS)
                .build();

        flowableClassAttribute = typeBuilder.stringAttribute(FLOWABLE_ATTRIBUTE_CLASS)
                .namespace(FLOWABLE_NS)
                .build();

        flowableExpressionAttribute = typeBuilder.stringAttribute(FLOWABLE_ATTRIBUTE_EXPRESSION)
                .namespace(FLOWABLE_NS)
                .build();

        flowableDelegateExpressionAttribute = typeBuilder.stringAttribute(FLOWABLE_ATTRIBUTE_DELEGATE_EXPRESSION)
                .namespace(FLOWABLE_NS)
                .build();

        SequenceBuilder sequenceBuilder = typeBuilder.sequence();

        flowableFieldCollection = sequenceBuilder.elementCollection(FlowableField.class)
                .build();

        flowableScriptChild = sequenceBuilder.element(FlowableScript.class)
                .build();

        typeBuilder.build();
    }

    public FlowableExecutionListenerImpl(ModelTypeInstanceContext instanceContext) {
        super(instanceContext);
    }

    @Override
    public String getFlowableEvent() {
        return flowableEventAttribute.getValue(this);
    }

    @Override
    public void setFlowableEvent(String flowableEvent) {
        flowableEventAttribute.setValue(this, flowableEvent);
    }

    @Override
    public String getFlowableClass() {
        return flowableClassAttribute.getValue(this);
    }

    @Override
    public void setFlowableClass(String flowableClass) {
        flowableClassAttribute.setValue(this, flowableClass);
    }

    @Override
    public String getFlowableExpression() {
        return flowableExpressionAttribute.getValue(this);
    }

    @Override
    public void setFlowableExpression(String flowableExpression) {
        flowableExpressionAttribute.setValue(this, flowableExpression);
    }

    @Override
    public String getFlowableDelegateExpression() {
        return flowableDelegateExpressionAttribute.getValue(this);
    }

    @Override
    public void setFlowableDelegateExpression(String flowableDelegateExpression) {
        flowableDelegateExpressionAttribute.setValue(this, flowableDelegateExpression);
    }

    @Override
    public Collection<FlowableField> getFlowableFields() {
        return flowableFieldCollection.get(this);
    }

    @Override
    public FlowableScript getFlowableScript() {
        return flowableScriptChild.getChild(this);
    }

    @Override
    public void setFlowableScript(FlowableScript flowableScript) {
        flowableScriptChild.setChild(this, flowableScript);
    }

}
