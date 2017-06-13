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
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.BPMN_ATTRIBUTE_EVENT_GATEWAY_TYPE;
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.BPMN_ATTRIBUTE_INSTANTIATE;
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.BPMN_ELEMENT_EVENT_BASED_GATEWAY;
import static org.flowable.bpm.model.xml.type.ModelElementTypeBuilder.ModelTypeInstanceProvider;

import org.flowable.bpm.model.bpmn.BpmnModelInstance;
import org.flowable.bpm.model.bpmn.EventBasedGatewayType;
import org.flowable.bpm.model.bpmn.builder.EventBasedGatewayBuilder;
import org.flowable.bpm.model.bpmn.instance.EventBasedGateway;
import org.flowable.bpm.model.bpmn.instance.Gateway;
import org.flowable.bpm.model.xml.ModelBuilder;
import org.flowable.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import org.flowable.bpm.model.xml.type.ModelElementTypeBuilder;
import org.flowable.bpm.model.xml.type.attribute.Attribute;

/**
 * The BPMN eventBasedGateway element.
 */
public class EventBasedGatewayImpl
        extends GatewayImpl
        implements EventBasedGateway {

    protected static Attribute<Boolean> instantiateAttribute;
    protected static Attribute<EventBasedGatewayType> eventGatewayTypeAttribute;

    public static void registerType(ModelBuilder modelBuilder) {
        ModelElementTypeBuilder typeBuilder = modelBuilder.defineType(EventBasedGateway.class, BPMN_ELEMENT_EVENT_BASED_GATEWAY)
                .namespaceUri(BPMN20_NS)
                .extendsType(Gateway.class)
                .instanceProvider(new ModelTypeInstanceProvider<EventBasedGateway>() {
                    @Override
                    public EventBasedGateway newInstance(ModelTypeInstanceContext instanceContext) {
                        return new EventBasedGatewayImpl(instanceContext);
                    }
                });

        instantiateAttribute = typeBuilder.booleanAttribute(BPMN_ATTRIBUTE_INSTANTIATE)
                .defaultValue(false)
                .build();

        eventGatewayTypeAttribute = typeBuilder.enumAttribute(BPMN_ATTRIBUTE_EVENT_GATEWAY_TYPE, EventBasedGatewayType.class)
                .defaultValue(EventBasedGatewayType.Exclusive)
                .build();

        typeBuilder.build();
    }

    public EventBasedGatewayImpl(ModelTypeInstanceContext context) {
        super(context);
    }

    @Override
    public EventBasedGatewayBuilder builder() {
        return new EventBasedGatewayBuilder((BpmnModelInstance) modelInstance, this);
    }

    @Override
    public boolean isInstantiate() {
        return instantiateAttribute.getValue(this);
    }

    @Override
    public void setInstantiate(boolean isInstantiate) {
        instantiateAttribute.setValue(this, isInstantiate);
    }

    @Override
    public EventBasedGatewayType getEventGatewayType() {
        return eventGatewayTypeAttribute.getValue(this);
    }

    @Override
    public void setEventGatewayType(EventBasedGatewayType eventGatewayType) {
        eventGatewayTypeAttribute.setValue(this, eventGatewayType);
    }

    @Override
    public boolean isFlowableAsync() {
        throw new UnsupportedOperationException("'async' is not supported for 'Event Based Gateway'");
    }

    @Override
    public void setFlowableAsync(boolean isFlowableAsync) {
        throw new UnsupportedOperationException("'async' is not supported for 'Event Based Gateway'");
    }

}
