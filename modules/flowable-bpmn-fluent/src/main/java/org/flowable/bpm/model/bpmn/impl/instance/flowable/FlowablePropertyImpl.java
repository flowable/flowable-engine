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
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.FLOWABLE_ATTRIBUTE_NAME;
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.FLOWABLE_ATTRIBUTE_VALUE;
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.FLOWABLE_ELEMENT_PROPERTY;
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.FLOWABLE_NS;
import static org.flowable.bpm.model.xml.type.ModelElementTypeBuilder.ModelTypeInstanceProvider;

import org.flowable.bpm.model.bpmn.impl.instance.BpmnModelElementInstanceImpl;
import org.flowable.bpm.model.bpmn.instance.flowable.FlowableProperty;
import org.flowable.bpm.model.xml.ModelBuilder;
import org.flowable.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import org.flowable.bpm.model.xml.type.ModelElementTypeBuilder;
import org.flowable.bpm.model.xml.type.attribute.Attribute;

/**
 * The BPMN property Flowable extension element.
 */
public class FlowablePropertyImpl
        extends BpmnModelElementInstanceImpl
        implements FlowableProperty {

    protected static Attribute<String> flowableIdAttribute;
    protected static Attribute<String> flowableNameAttribute;
    protected static Attribute<String> flowableValueAttribute;

    public static void registerType(ModelBuilder modelBuilder) {
        ModelElementTypeBuilder typeBuilder = modelBuilder.defineType(FlowableProperty.class, FLOWABLE_ELEMENT_PROPERTY)
                .namespaceUri(FLOWABLE_NS)
                .instanceProvider(new ModelTypeInstanceProvider<FlowableProperty>() {
                    @Override
                    public FlowableProperty newInstance(ModelTypeInstanceContext instanceContext) {
                        return new FlowablePropertyImpl(instanceContext);
                    }
                });

        flowableIdAttribute = typeBuilder.stringAttribute(FLOWABLE_ATTRIBUTE_ID)
                .namespace(FLOWABLE_NS)
                .build();

        flowableNameAttribute = typeBuilder.stringAttribute(FLOWABLE_ATTRIBUTE_NAME)
                .namespace(FLOWABLE_NS)
                .build();

        flowableValueAttribute = typeBuilder.stringAttribute(FLOWABLE_ATTRIBUTE_VALUE)
                .namespace(FLOWABLE_NS)
                .build();

        typeBuilder.build();
    }

    public FlowablePropertyImpl(ModelTypeInstanceContext instanceContext) {
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
    public String getFlowableName() {
        return flowableNameAttribute.getValue(this);
    }

    @Override
    public void setFlowableName(String flowableName) {
        flowableNameAttribute.setValue(this, flowableName);
    }

    @Override
    public String getFlowableValue() {
        return flowableValueAttribute.getValue(this);
    }

    @Override
    public void setFlowableValue(String flowableValue) {
        flowableValueAttribute.setValue(this, flowableValue);
    }
}
