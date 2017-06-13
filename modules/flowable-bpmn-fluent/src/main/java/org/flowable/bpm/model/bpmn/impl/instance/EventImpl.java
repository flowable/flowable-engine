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
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.BPMN_ELEMENT_EVENT;

import org.flowable.bpm.model.bpmn.instance.Event;
import org.flowable.bpm.model.bpmn.instance.FlowNode;
import org.flowable.bpm.model.bpmn.instance.Property;
import org.flowable.bpm.model.xml.ModelBuilder;
import org.flowable.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import org.flowable.bpm.model.xml.type.ModelElementTypeBuilder;
import org.flowable.bpm.model.xml.type.child.ChildElementCollection;
import org.flowable.bpm.model.xml.type.child.SequenceBuilder;

import java.util.Collection;

/**
 * The BPMN event element.
 */
public abstract class EventImpl
        extends FlowNodeImpl
        implements Event {

    protected static ChildElementCollection<Property> propertyCollection;

    public static void registerType(ModelBuilder modelBuilder) {
        ModelElementTypeBuilder typeBuilder = modelBuilder.defineType(Event.class, BPMN_ELEMENT_EVENT)
                .namespaceUri(BPMN20_NS)
                .extendsType(FlowNode.class)
                .abstractType();

        SequenceBuilder sequence = typeBuilder.sequence();

        propertyCollection = sequence.elementCollection(Property.class)
                .build();

        typeBuilder.build();
    }

    public EventImpl(ModelTypeInstanceContext context) {
        super(context);
    }

    @Override
    public Collection<Property> getProperties() {
        return propertyCollection.get(this);
    }
}
