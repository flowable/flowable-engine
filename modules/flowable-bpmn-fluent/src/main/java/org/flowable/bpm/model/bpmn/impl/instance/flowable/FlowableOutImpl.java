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

import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.FLOWABLE_ATTRIBUTE_SOURCE;
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.FLOWABLE_ATTRIBUTE_SOURCE_EXPRESSION;
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.FLOWABLE_ATTRIBUTE_TARGET;
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.FLOWABLE_ELEMENT_OUT;
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.FLOWABLE_NS;
import static org.flowable.bpm.model.xml.type.ModelElementTypeBuilder.ModelTypeInstanceProvider;

import org.flowable.bpm.model.bpmn.impl.instance.BpmnModelElementInstanceImpl;
import org.flowable.bpm.model.bpmn.instance.flowable.FlowableOut;
import org.flowable.bpm.model.xml.ModelBuilder;
import org.flowable.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import org.flowable.bpm.model.xml.type.ModelElementTypeBuilder;
import org.flowable.bpm.model.xml.type.attribute.Attribute;

/**
 * The BPMN out Flowable extension element.
 */
public class FlowableOutImpl
        extends BpmnModelElementInstanceImpl
        implements FlowableOut {

    protected static Attribute<String> flowableSourceAttribute;
    protected static Attribute<String> flowableSourceExpressionAttribute;
    protected static Attribute<String> flowableTargetAttribute;

    public static void registerType(ModelBuilder modelBuilder) {
        ModelElementTypeBuilder typeBuilder = modelBuilder.defineType(FlowableOut.class, FLOWABLE_ELEMENT_OUT)
                .namespaceUri(FLOWABLE_NS)
                .instanceProvider(new ModelTypeInstanceProvider<FlowableOut>() {
                    public FlowableOut newInstance(ModelTypeInstanceContext instanceContext) {
                        return new FlowableOutImpl(instanceContext);
                    }
                });

        flowableSourceAttribute = typeBuilder.stringAttribute(FLOWABLE_ATTRIBUTE_SOURCE)
                .namespace(FLOWABLE_NS)
                .build();

        flowableSourceExpressionAttribute = typeBuilder.stringAttribute(FLOWABLE_ATTRIBUTE_SOURCE_EXPRESSION)
                .namespace(FLOWABLE_NS)
                .build();

        flowableTargetAttribute = typeBuilder.stringAttribute(FLOWABLE_ATTRIBUTE_TARGET)
                .namespace(FLOWABLE_NS)
                .build();

        typeBuilder.build();
    }

    public FlowableOutImpl(ModelTypeInstanceContext instanceContext) {
        super(instanceContext);
    }

    public String getFlowableSource() {
        return flowableSourceAttribute.getValue(this);
    }

    public void setFlowableSource(String flowableSource) {
        flowableSourceAttribute.setValue(this, flowableSource);
    }

    public String getFlowableSourceExpression() {
        return flowableSourceExpressionAttribute.getValue(this);
    }

    public void setFlowableSourceExpression(String flowableSourceExpression) {
        flowableSourceExpressionAttribute.setValue(this, flowableSourceExpression);
    }

    public String getFlowableTarget() {
        return flowableTargetAttribute.getValue(this);
    }

    public void setFlowableTarget(String flowableTarget) {
        flowableTargetAttribute.setValue(this, flowableTarget);
    }
}
