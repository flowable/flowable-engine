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
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.BPMN_ATTRIBUTE_DEFAULT;
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.BPMN_ELEMENT_COMPLEX_GATEWAY;
import static org.flowable.bpm.model.xml.type.ModelElementTypeBuilder.ModelTypeInstanceProvider;

import org.flowable.bpm.model.bpmn.BpmnModelInstance;
import org.flowable.bpm.model.bpmn.builder.ComplexGatewayBuilder;
import org.flowable.bpm.model.bpmn.instance.ActivationCondition;
import org.flowable.bpm.model.bpmn.instance.ComplexGateway;
import org.flowable.bpm.model.bpmn.instance.Gateway;
import org.flowable.bpm.model.bpmn.instance.SequenceFlow;
import org.flowable.bpm.model.xml.ModelBuilder;
import org.flowable.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import org.flowable.bpm.model.xml.type.ModelElementTypeBuilder;
import org.flowable.bpm.model.xml.type.child.ChildElement;
import org.flowable.bpm.model.xml.type.child.SequenceBuilder;
import org.flowable.bpm.model.xml.type.reference.AttributeReference;

/**
 * The BPMN complexGateway element.
 */
public class ComplexGatewayImpl
        extends GatewayImpl
        implements ComplexGateway {

    protected static AttributeReference<SequenceFlow> defaultAttribute;
    protected static ChildElement<ActivationCondition> activationConditionChild;

    public static void registerType(ModelBuilder modelBuilder) {
        ModelElementTypeBuilder typeBuilder = modelBuilder.defineType(ComplexGateway.class, BPMN_ELEMENT_COMPLEX_GATEWAY)
                .namespaceUri(BPMN20_NS)
                .extendsType(Gateway.class)
                .instanceProvider(new ModelTypeInstanceProvider<ComplexGateway>() {
                    @Override
                    public ComplexGateway newInstance(ModelTypeInstanceContext instanceContext) {
                        return new ComplexGatewayImpl(instanceContext);
                    }
                });

        defaultAttribute = typeBuilder.stringAttribute(BPMN_ATTRIBUTE_DEFAULT)
                .idAttributeReference(SequenceFlow.class)
                .build();

        SequenceBuilder sequenceBuilder = typeBuilder.sequence();

        activationConditionChild = sequenceBuilder.element(ActivationCondition.class)
                .build();

        typeBuilder.build();
    }

    public ComplexGatewayImpl(ModelTypeInstanceContext context) {
        super(context);
    }

    @Override
    public ComplexGatewayBuilder builder() {
        return new ComplexGatewayBuilder((BpmnModelInstance) modelInstance, this);
    }

    @Override
    public SequenceFlow getDefault() {
        return defaultAttribute.getReferenceTargetElement(this);
    }

    @Override
    public void setDefault(SequenceFlow defaultFlow) {
        defaultAttribute.setReferenceTargetElement(this, defaultFlow);
    }

    @Override
    public ActivationCondition getActivationCondition() {
        return activationConditionChild.getChild(this);
    }

    @Override
    public void setActivationCondition(ActivationCondition activationCondition) {
        activationConditionChild.setChild(this, activationCondition);
    }

}
