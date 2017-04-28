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
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.BPMN_ATTRIBUTE_GATEWAY_DIRECTION;
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.BPMN_ELEMENT_GATEWAY;

import org.flowable.bpm.model.bpmn.GatewayDirection;
import org.flowable.bpm.model.bpmn.builder.AbstractGatewayBuilder;
import org.flowable.bpm.model.bpmn.instance.FlowNode;
import org.flowable.bpm.model.bpmn.instance.Gateway;
import org.flowable.bpm.model.bpmn.instance.bpmndi.BpmnShape;
import org.flowable.bpm.model.xml.ModelBuilder;
import org.flowable.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import org.flowable.bpm.model.xml.type.ModelElementTypeBuilder;
import org.flowable.bpm.model.xml.type.attribute.Attribute;

/**
 * The BPMN gateway element.
 */
public abstract class GatewayImpl
        extends FlowNodeImpl
        implements Gateway {

    protected static Attribute<GatewayDirection> gatewayDirectionAttribute;

    public static void registerType(ModelBuilder modelBuilder) {
        ModelElementTypeBuilder typeBuilder = modelBuilder.defineType(Gateway.class, BPMN_ELEMENT_GATEWAY)
                .namespaceUri(BPMN20_NS)
                .extendsType(FlowNode.class)
                .abstractType();

        gatewayDirectionAttribute = typeBuilder.enumAttribute(BPMN_ATTRIBUTE_GATEWAY_DIRECTION, GatewayDirection.class)
                .defaultValue(GatewayDirection.Unspecified)
                .build();

        typeBuilder.build();
    }

    public GatewayImpl(ModelTypeInstanceContext context) {
        super(context);
    }

    @SuppressWarnings("rawtypes")
    public abstract AbstractGatewayBuilder builder();

    public GatewayDirection getGatewayDirection() {
        return gatewayDirectionAttribute.getValue(this);
    }

    public void setGatewayDirection(GatewayDirection gatewayDirection) {
        gatewayDirectionAttribute.setValue(this, gatewayDirection);
    }

    public BpmnShape getDiagramElement() {
        return (BpmnShape) super.getDiagramElement();
    }

}
