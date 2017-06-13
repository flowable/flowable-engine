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
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.BPMN_ATTRIBUTE_PARALLEL_MULTIPLE;
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.BPMN_ELEMENT_CATCH_EVENT;

import org.flowable.bpm.model.bpmn.instance.CatchEvent;
import org.flowable.bpm.model.bpmn.instance.DataOutput;
import org.flowable.bpm.model.bpmn.instance.DataOutputAssociation;
import org.flowable.bpm.model.bpmn.instance.Event;
import org.flowable.bpm.model.bpmn.instance.EventDefinition;
import org.flowable.bpm.model.bpmn.instance.OutputSet;
import org.flowable.bpm.model.xml.ModelBuilder;
import org.flowable.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import org.flowable.bpm.model.xml.type.ModelElementTypeBuilder;
import org.flowable.bpm.model.xml.type.attribute.Attribute;
import org.flowable.bpm.model.xml.type.child.ChildElement;
import org.flowable.bpm.model.xml.type.child.ChildElementCollection;
import org.flowable.bpm.model.xml.type.child.SequenceBuilder;
import org.flowable.bpm.model.xml.type.reference.ElementReferenceCollection;

import java.util.Collection;

/**
 * The BPMN catchEvent element.
 */
public abstract class CatchEventImpl
        extends EventImpl
        implements CatchEvent {

    protected static Attribute<Boolean> parallelMultipleAttribute;
    protected static ChildElementCollection<DataOutput> dataOutputCollection;
    protected static ChildElementCollection<DataOutputAssociation> dataOutputAssociationCollection;
    protected static ChildElement<OutputSet> outputSetChild;
    protected static ChildElementCollection<EventDefinition> eventDefinitionCollection;
    protected static ElementReferenceCollection<EventDefinition, EventDefinitionRef> eventDefinitionRefCollection;

    public static void registerType(ModelBuilder modelBuilder) {
        ModelElementTypeBuilder typeBuilder = modelBuilder.defineType(CatchEvent.class, BPMN_ELEMENT_CATCH_EVENT)
                .namespaceUri(BPMN20_NS)
                .extendsType(Event.class)
                .abstractType();

        parallelMultipleAttribute = typeBuilder.booleanAttribute(BPMN_ATTRIBUTE_PARALLEL_MULTIPLE)
                .defaultValue(false)
                .build();

        SequenceBuilder sequenceBuilder = typeBuilder.sequence();

        dataOutputCollection = sequenceBuilder.elementCollection(DataOutput.class)
                .build();

        dataOutputAssociationCollection = sequenceBuilder.elementCollection(DataOutputAssociation.class)
                .build();

        outputSetChild = sequenceBuilder.element(OutputSet.class)
                .build();

        eventDefinitionCollection = sequenceBuilder.elementCollection(EventDefinition.class)
                .build();

        eventDefinitionRefCollection = sequenceBuilder.elementCollection(EventDefinitionRef.class)
                .qNameElementReferenceCollection(EventDefinition.class)
                .build();

        typeBuilder.build();
    }


    public CatchEventImpl(ModelTypeInstanceContext context) {
        super(context);
    }

    @Override
    public boolean isParallelMultiple() {
        return parallelMultipleAttribute.getValue(this);
    }

    @Override
    public void setParallelMultiple(boolean parallelMultiple) {
        parallelMultipleAttribute.setValue(this, parallelMultiple);
    }

    @Override
    public Collection<DataOutput> getDataOutputs() {
        return dataOutputCollection.get(this);
    }

    @Override
    public Collection<DataOutputAssociation> getDataOutputAssociations() {
        return dataOutputAssociationCollection.get(this);
    }

    @Override
    public OutputSet getOutputSet() {
        return outputSetChild.getChild(this);
    }

    @Override
    public void setOutputSet(OutputSet outputSet) {
        outputSetChild.setChild(this, outputSet);
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
