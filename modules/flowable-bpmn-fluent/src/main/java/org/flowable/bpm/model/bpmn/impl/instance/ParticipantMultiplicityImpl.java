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
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.BPMN_ATTRIBUTE_MAXIMUM;
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.BPMN_ATTRIBUTE_MINIMUM;
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.BPMN_ELEMENT_PARTICIPANT_MULTIPLICITY;
import static org.flowable.bpm.model.xml.type.ModelElementTypeBuilder.ModelTypeInstanceProvider;

import org.flowable.bpm.model.bpmn.instance.BaseElement;
import org.flowable.bpm.model.bpmn.instance.ParticipantMultiplicity;
import org.flowable.bpm.model.xml.ModelBuilder;
import org.flowable.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import org.flowable.bpm.model.xml.type.ModelElementTypeBuilder;
import org.flowable.bpm.model.xml.type.attribute.Attribute;

/**
 * The BPMN participantMultiplicity element.
 */
public class ParticipantMultiplicityImpl
        extends BaseElementImpl
        implements ParticipantMultiplicity {

    protected static Attribute<Integer> minimumAttribute;
    protected static Attribute<Integer> maximumAttribute;

    public static void registerType(ModelBuilder modelBuilder) {
        ModelElementTypeBuilder typeBuilder = modelBuilder.defineType(ParticipantMultiplicity.class, BPMN_ELEMENT_PARTICIPANT_MULTIPLICITY)
                .namespaceUri(BPMN20_NS)
                .extendsType(BaseElement.class)
                .instanceProvider(new ModelTypeInstanceProvider<ParticipantMultiplicity>() {
                    @Override
                    public ParticipantMultiplicity newInstance(ModelTypeInstanceContext instanceContext) {
                        return new ParticipantMultiplicityImpl(instanceContext);
                    }
                });

        minimumAttribute = typeBuilder.integerAttribute(BPMN_ATTRIBUTE_MINIMUM)
                .defaultValue(0)
                .build();

        maximumAttribute = typeBuilder.integerAttribute(BPMN_ATTRIBUTE_MAXIMUM)
                .defaultValue(1)
                .build();

        typeBuilder.build();
    }

    public ParticipantMultiplicityImpl(ModelTypeInstanceContext instanceContext) {
        super(instanceContext);
    }

    @Override
    public int getMinimum() {
        return minimumAttribute.getValue(this);
    }

    @Override
    public void setMinimum(int minimum) {
        minimumAttribute.setValue(this, minimum);
    }

    @Override
    public int getMaximum() {
        return maximumAttribute.getValue(this);
    }

    @Override
    public void setMaximum(int maximum) {
        maximumAttribute.setValue(this, maximum);
    }
}
