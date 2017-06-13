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
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.BPMN_ELEMENT_INCLUSIVE_GATEWAY;
import static org.flowable.bpm.model.xml.type.ModelElementTypeBuilder.ModelTypeInstanceProvider;

import org.flowable.bpm.model.bpmn.BpmnModelInstance;
import org.flowable.bpm.model.bpmn.builder.InclusiveGatewayBuilder;
import org.flowable.bpm.model.bpmn.instance.Gateway;
import org.flowable.bpm.model.bpmn.instance.InclusiveGateway;
import org.flowable.bpm.model.bpmn.instance.SequenceFlow;
import org.flowable.bpm.model.xml.ModelBuilder;
import org.flowable.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import org.flowable.bpm.model.xml.type.ModelElementTypeBuilder;
import org.flowable.bpm.model.xml.type.reference.AttributeReference;

/**
 * The BPMN inclusiveGateway element.
 */
public class InclusiveGatewayImpl
        extends GatewayImpl
        implements InclusiveGateway {

    protected static AttributeReference<SequenceFlow> defaultAttribute;

    public static void registerType(ModelBuilder modelBuilder) {
        ModelElementTypeBuilder typeBuilder = modelBuilder.defineType(InclusiveGateway.class, BPMN_ELEMENT_INCLUSIVE_GATEWAY)
                .namespaceUri(BPMN20_NS)
                .extendsType(Gateway.class)
                .instanceProvider(new ModelTypeInstanceProvider<InclusiveGateway>() {
                    @Override
                    public InclusiveGateway newInstance(ModelTypeInstanceContext instanceContext) {
                        return new InclusiveGatewayImpl(instanceContext);
                    }
                });

        defaultAttribute = typeBuilder.stringAttribute(BPMN_ATTRIBUTE_DEFAULT)
                .idAttributeReference(SequenceFlow.class)
                .build();

        typeBuilder.build();
    }

    public InclusiveGatewayImpl(ModelTypeInstanceContext context) {
        super(context);
    }

    @Override
    public InclusiveGatewayBuilder builder() {
        return new InclusiveGatewayBuilder((BpmnModelInstance) modelInstance, this);
    }

    @Override
    public SequenceFlow getDefault() {
        return defaultAttribute.getReferenceTargetElement(this);
    }

    @Override
    public void setDefault(SequenceFlow defaultFlow) {
        defaultAttribute.setReferenceTargetElement(this, defaultFlow);
    }
}
