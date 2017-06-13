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
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.BPMN_ELEMENT_THROW_EVENT;

import org.flowable.bpm.model.bpmn.instance.DataInput;
import org.flowable.bpm.model.bpmn.instance.DataInputAssociation;
import org.flowable.bpm.model.bpmn.instance.Event;
import org.flowable.bpm.model.bpmn.instance.EventDefinition;
import org.flowable.bpm.model.bpmn.instance.InputSet;
import org.flowable.bpm.model.bpmn.instance.ThrowEvent;
import org.flowable.bpm.model.xml.ModelBuilder;
import org.flowable.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import org.flowable.bpm.model.xml.type.ModelElementTypeBuilder;
import org.flowable.bpm.model.xml.type.child.ChildElement;
import org.flowable.bpm.model.xml.type.child.ChildElementCollection;
import org.flowable.bpm.model.xml.type.child.SequenceBuilder;
import org.flowable.bpm.model.xml.type.reference.ElementReferenceCollection;

import java.util.Collection;

/**
 * The BPMN throwEvent element.
 */
public abstract class ThrowEventImpl
        extends EventImpl
        implements ThrowEvent {

    protected static ChildElementCollection<DataInput> dataInputCollection;
    protected static ChildElementCollection<DataInputAssociation> dataInputAssociationCollection;
    protected static ChildElement<InputSet> inputSetChild;
    protected static ChildElementCollection<EventDefinition> eventDefinitionCollection;
    protected static ElementReferenceCollection<EventDefinition, EventDefinitionRef> eventDefinitionRefCollection;

    public static void registerType(ModelBuilder modelBuilder) {
        ModelElementTypeBuilder typeBuilder = modelBuilder.defineType(ThrowEvent.class, BPMN_ELEMENT_THROW_EVENT)
                .namespaceUri(BPMN20_NS)
                .extendsType(Event.class)
                .abstractType();

        SequenceBuilder sequenceBuilder = typeBuilder.sequence();

        dataInputCollection = sequenceBuilder.elementCollection(DataInput.class)
                .build();

        dataInputAssociationCollection = sequenceBuilder.elementCollection(DataInputAssociation.class)
                .build();

        inputSetChild = sequenceBuilder.element(InputSet.class)
                .build();

        eventDefinitionCollection = sequenceBuilder.elementCollection(EventDefinition.class)
                .build();

        eventDefinitionRefCollection = sequenceBuilder.elementCollection(EventDefinitionRef.class)
                .qNameElementReferenceCollection(EventDefinition.class)
                .build();

        typeBuilder.build();
    }


    public ThrowEventImpl(ModelTypeInstanceContext context) {
        super(context);
    }

    @Override
    public Collection<DataInput> getDataInputs() {
        return dataInputCollection.get(this);
    }

    @Override
    public Collection<DataInputAssociation> getDataInputAssociations() {
        return dataInputAssociationCollection.get(this);
    }

    @Override
    public InputSet getInputSet() {
        return inputSetChild.getChild(this);
    }

    @Override
    public void setInputSet(InputSet inputSet) {
        inputSetChild.setChild(this, inputSet);
    }

    @Override
    public Collection<EventDefinition> getEventDefinitions() {
        return eventDefinitionCollection.get(this);
    }

    @Override
    public Collection<EventDefinition> getEventDefinitionRefs() {
        return eventDefinitionRefCollection.getReferenceTargetElements(this);
    }
}
