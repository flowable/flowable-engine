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

import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.FLOWABLE_ATTRIBUTE_NAME;
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.FLOWABLE_ELEMENT_OUTPUT_PARAMETER;
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.FLOWABLE_NS;

import org.flowable.bpm.model.bpmn.instance.flowable.FlowableOutputParameter;
import org.flowable.bpm.model.xml.ModelBuilder;
import org.flowable.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import org.flowable.bpm.model.xml.type.ModelElementTypeBuilder;
import org.flowable.bpm.model.xml.type.ModelElementTypeBuilder.ModelTypeInstanceProvider;
import org.flowable.bpm.model.xml.type.attribute.Attribute;

/**
 * The BPMN outputParameter Flowable extension element.
 */
public class FlowableOutputParameterImpl
        extends FlowableGenericValueElementImpl
        implements FlowableOutputParameter {

    protected static Attribute<String> flowableNameAttribute;

    public static void registerType(ModelBuilder modelBuilder) {
        ModelElementTypeBuilder typeBuilder = modelBuilder.defineType(FlowableOutputParameter.class, FLOWABLE_ELEMENT_OUTPUT_PARAMETER)
                .namespaceUri(FLOWABLE_NS)
                .instanceProvider(new ModelTypeInstanceProvider<FlowableOutputParameter>() {
                    @Override
                    public FlowableOutputParameter newInstance(ModelTypeInstanceContext instanceContext) {
                        return new FlowableOutputParameterImpl(instanceContext);
                    }
                });

        flowableNameAttribute = typeBuilder.stringAttribute(FLOWABLE_ATTRIBUTE_NAME)
                .namespace(FLOWABLE_NS)
                .required()
                .build();

        typeBuilder.build();
    }

    public FlowableOutputParameterImpl(ModelTypeInstanceContext instanceContext) {
        super(instanceContext);
    }

    @Override
    public String getFlowableName() {
        return flowableNameAttribute.getValue(this);
    }

    @Override
    public void setFlowableName(String flowableName) {
        flowableNameAttribute.setValue(this, flowableName);
    }
}
