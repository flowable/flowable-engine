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
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.BPMN_ELEMENT_CONDITION_EXPRESSION;
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.FLOWABLE_ATTRIBUTE_RESOURCE;
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.FLOWABLE_NS;
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.XSI_ATTRIBUTE_TYPE;
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.XSI_NS;
import static org.flowable.bpm.model.xml.type.ModelElementTypeBuilder.ModelTypeInstanceProvider;

import org.flowable.bpm.model.bpmn.instance.ConditionExpression;
import org.flowable.bpm.model.bpmn.instance.FormalExpression;
import org.flowable.bpm.model.xml.ModelBuilder;
import org.flowable.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import org.flowable.bpm.model.xml.type.ModelElementTypeBuilder;
import org.flowable.bpm.model.xml.type.attribute.Attribute;

/**
 * The BPMN conditionExpression element of the BPMN tSequenceFlow type.
 */
public class ConditionExpressionImpl
        extends FormalExpressionImpl
        implements ConditionExpression {

    protected static Attribute<String> typeAttribute;
    protected static Attribute<String> flowableResourceAttribute;

    public static void registerType(ModelBuilder modelBuilder) {
        ModelElementTypeBuilder typeBuilder = modelBuilder.defineType(ConditionExpression.class, BPMN_ELEMENT_CONDITION_EXPRESSION)
                .namespaceUri(BPMN20_NS)
                .extendsType(FormalExpression.class)
                .instanceProvider(new ModelTypeInstanceProvider<ConditionExpression>() {
                    @Override
                    public ConditionExpression newInstance(ModelTypeInstanceContext instanceContext) {
                        return new ConditionExpressionImpl(instanceContext);
                    }
                });

        typeAttribute = typeBuilder.stringAttribute(XSI_ATTRIBUTE_TYPE)
                .namespace(XSI_NS)
                .defaultValue("tFormalExpression")
                .build();

        flowableResourceAttribute = typeBuilder.stringAttribute(FLOWABLE_ATTRIBUTE_RESOURCE)
                .namespace(FLOWABLE_NS)
                .build();

        typeBuilder.build();
    }

    public ConditionExpressionImpl(ModelTypeInstanceContext instanceContext) {
        super(instanceContext);
    }

    @Override
    public String getType() {
        return typeAttribute.getValue(this);
    }

    @Override
    public void setType(String type) {
        typeAttribute.setValue(this, type);
    }

    @Override
    public String getFlowableResource() {
        return flowableResourceAttribute.getValue(this);
    }

    @Override
    public void setFlowableResource(String flowableResource) {
        flowableResourceAttribute.setValue(this, flowableResource);
    }

}
