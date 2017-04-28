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
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.BPMN_ELEMENT_LINK_EVENT_DEFINITION;
import static org.flowable.bpm.model.xml.type.ModelElementTypeBuilder.ModelTypeInstanceProvider;

import org.flowable.bpm.model.bpmn.instance.EventDefinition;
import org.flowable.bpm.model.bpmn.instance.LinkEventDefinition;
import org.flowable.bpm.model.xml.ModelBuilder;
import org.flowable.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import org.flowable.bpm.model.xml.type.ModelElementTypeBuilder;
import org.flowable.bpm.model.xml.type.attribute.Attribute;
import org.flowable.bpm.model.xml.type.child.SequenceBuilder;
import org.flowable.bpm.model.xml.type.reference.ElementReference;
import org.flowable.bpm.model.xml.type.reference.ElementReferenceCollection;

import java.util.Collection;

/**
 * The BPMN linkEventDefinition element.
 */
public class LinkEventDefinitionImpl
        extends EventDefinitionImpl
        implements LinkEventDefinition {

    protected static Attribute<String> nameAttribute;
    protected static ElementReferenceCollection<LinkEventDefinition, Source> sourceCollection;
    protected static ElementReference<LinkEventDefinition, Target> targetChild;

    public static void registerType(ModelBuilder modelBuilder) {
        ModelElementTypeBuilder typeBuilder = modelBuilder.defineType(LinkEventDefinition.class, BPMN_ELEMENT_LINK_EVENT_DEFINITION)
                .namespaceUri(BPMN20_NS)
                .extendsType(EventDefinition.class)
                .instanceProvider(new ModelTypeInstanceProvider<LinkEventDefinition>() {
                    public LinkEventDefinition newInstance(ModelTypeInstanceContext instanceContext) {
                        return new LinkEventDefinitionImpl(instanceContext);
                    }
                });

        nameAttribute = typeBuilder.stringAttribute(BPMN_ATTRIBUTE_NAME)
                .required()
                .build();

        SequenceBuilder sequenceBuilder = typeBuilder.sequence();

        sourceCollection = sequenceBuilder.elementCollection(Source.class)
                .qNameElementReferenceCollection(LinkEventDefinition.class)
                .build();

        targetChild = sequenceBuilder.element(Target.class)
                .qNameElementReference(LinkEventDefinition.class)
                .build();

        typeBuilder.build();
    }

    public LinkEventDefinitionImpl(ModelTypeInstanceContext context) {
        super(context);
    }

    public String getName() {
        return nameAttribute.getValue(this);
    }

    public void setName(String name) {
        nameAttribute.setValue(this, name);
    }

    public Collection<LinkEventDefinition> getSources() {
        return sourceCollection.getReferenceTargetElements(this);
    }

    public LinkEventDefinition getTarget() {
        return targetChild.getReferenceTargetElement(this);
    }

    public void setTarget(LinkEventDefinition target) {
        targetChild.setReferenceTargetElement(this, target);
    }

}
