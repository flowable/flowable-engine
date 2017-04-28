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
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.BPMN_ATTRIBUTE_NAME;
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.BPMN_ELEMENT_OUTPUT_SET;
import static org.flowable.bpm.model.xml.type.ModelElementTypeBuilder.ModelTypeInstanceProvider;

import org.flowable.bpm.model.bpmn.instance.BaseElement;
import org.flowable.bpm.model.bpmn.instance.DataOutput;
import org.flowable.bpm.model.bpmn.instance.InputSet;
import org.flowable.bpm.model.bpmn.instance.OutputSet;
import org.flowable.bpm.model.xml.ModelBuilder;
import org.flowable.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import org.flowable.bpm.model.xml.type.ModelElementTypeBuilder;
import org.flowable.bpm.model.xml.type.attribute.Attribute;
import org.flowable.bpm.model.xml.type.child.SequenceBuilder;
import org.flowable.bpm.model.xml.type.reference.ElementReferenceCollection;

import java.util.Collection;

/**
 * The BPMN outputSet element.
 */
public class OutputSetImpl
        extends BaseElementImpl
        implements OutputSet {

    protected static Attribute<String> nameAttribute;
    protected static ElementReferenceCollection<DataOutput, DataOutputRefs> dataOutputRefsCollection;
    protected static ElementReferenceCollection<DataOutput, OptionalOutputRefs> optionalOutputRefsCollection;
    protected static ElementReferenceCollection<DataOutput, WhileExecutingOutputRefs> whileExecutingOutputRefsCollection;
    protected static ElementReferenceCollection<InputSet, InputSetRefs> inputSetInputSetRefsCollection;

    public static void registerType(ModelBuilder modelBuilder) {
        ModelElementTypeBuilder typeBuilder = modelBuilder.defineType(OutputSet.class, BPMN_ELEMENT_OUTPUT_SET)
                .namespaceUri(BPMN20_NS)
                .extendsType(BaseElement.class)
                .instanceProvider(new ModelTypeInstanceProvider<OutputSet>() {
                    public OutputSet newInstance(ModelTypeInstanceContext instanceContext) {
                        return new OutputSetImpl(instanceContext);
                    }
                });

        nameAttribute = typeBuilder.stringAttribute(BPMN_ATTRIBUTE_NAME)
                .build();

        SequenceBuilder sequenceBuilder = typeBuilder.sequence();

        dataOutputRefsCollection = sequenceBuilder.elementCollection(DataOutputRefs.class)
                .idElementReferenceCollection(DataOutput.class)
                .build();

        optionalOutputRefsCollection = sequenceBuilder.elementCollection(OptionalOutputRefs.class)
                .idElementReferenceCollection(DataOutput.class)
                .build();

        whileExecutingOutputRefsCollection = sequenceBuilder.elementCollection(WhileExecutingOutputRefs.class)
                .idElementReferenceCollection(DataOutput.class)
                .build();

        inputSetInputSetRefsCollection = sequenceBuilder.elementCollection(InputSetRefs.class)
                .idElementReferenceCollection(InputSet.class)
                .build();

        typeBuilder.build();
    }

    public OutputSetImpl(ModelTypeInstanceContext instanceContext) {
        super(instanceContext);
    }

    public String getName() {
        return nameAttribute.getValue(this);
    }

    public void setName(String name) {
        nameAttribute.setValue(this, name);
    }

    public Collection<DataOutput> getDataOutputRefs() {
        return dataOutputRefsCollection.getReferenceTargetElements(this);
    }

    public Collection<DataOutput> getOptionalOutputRefs() {
        return optionalOutputRefsCollection.getReferenceTargetElements(this);
    }

    public Collection<DataOutput> getWhileExecutingOutputRefs() {
        return whileExecutingOutputRefsCollection.getReferenceTargetElements(this);
    }

    public Collection<InputSet> getInputSetRefs() {
        return inputSetInputSetRefsCollection.getReferenceTargetElements(this);
    }
}
