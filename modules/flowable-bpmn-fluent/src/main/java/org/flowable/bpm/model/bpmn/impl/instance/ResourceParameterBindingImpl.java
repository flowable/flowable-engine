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
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.BPMN_ATTRIBUTE_PARAMETER_REF;
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.BPMN_ELEMENT_RESOURCE_PARAMETER_BINDING;
import static org.flowable.bpm.model.xml.type.ModelElementTypeBuilder.ModelTypeInstanceProvider;

import org.flowable.bpm.model.bpmn.instance.BaseElement;
import org.flowable.bpm.model.bpmn.instance.Expression;
import org.flowable.bpm.model.bpmn.instance.ResourceParameter;
import org.flowable.bpm.model.bpmn.instance.ResourceParameterBinding;
import org.flowable.bpm.model.xml.ModelBuilder;
import org.flowable.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import org.flowable.bpm.model.xml.type.ModelElementTypeBuilder;
import org.flowable.bpm.model.xml.type.child.ChildElement;
import org.flowable.bpm.model.xml.type.child.SequenceBuilder;
import org.flowable.bpm.model.xml.type.reference.AttributeReference;

/**
 * The BPMN resourceParameterBinding element.
 */
public class ResourceParameterBindingImpl
        extends BaseElementImpl
        implements ResourceParameterBinding {

    protected static AttributeReference<ResourceParameter> parameterRefAttribute;
    protected static ChildElement<Expression> expressionChild;

    public static void registerType(ModelBuilder modelBuilder) {
        ModelElementTypeBuilder typeBuilder = modelBuilder.defineType(ResourceParameterBinding.class, BPMN_ELEMENT_RESOURCE_PARAMETER_BINDING)
                .namespaceUri(BPMN20_NS)
                .extendsType(BaseElement.class)
                .instanceProvider(new ModelTypeInstanceProvider<ResourceParameterBinding>() {
                    @Override
                    public ResourceParameterBinding newInstance(ModelTypeInstanceContext instanceContext) {
                        return new ResourceParameterBindingImpl(instanceContext);
                    }
                });

        parameterRefAttribute = typeBuilder.stringAttribute(BPMN_ATTRIBUTE_PARAMETER_REF)
                .required()
                .qNameAttributeReference(ResourceParameter.class)
                .build();

        SequenceBuilder sequenceBuilder = typeBuilder.sequence();

        expressionChild = sequenceBuilder.element(Expression.class)
                .required()
                .build();

        typeBuilder.build();
    }

    public ResourceParameterBindingImpl(ModelTypeInstanceContext instanceContext) {
        super(instanceContext);
    }

    @Override
    public ResourceParameter getParameter() {
        return parameterRefAttribute.getReferenceTargetElement(this);
    }

    @Override
    public void setParameter(ResourceParameter parameter) {
        parameterRefAttribute.setReferenceTargetElement(this, parameter);
    }

    @Override
    public Expression getExpression() {
        return expressionChild.getChild(this);
    }

    @Override
    public void setExpression(Expression expression) {
        expressionChild.setChild(this, expression);
    }
}
