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
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.BPMN_ATTRIBUTE_DIRECTION;
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.BPMN_ATTRIBUTE_TYPE;
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.BPMN_ELEMENT_RELATIONSHIP;
import static org.flowable.bpm.model.xml.type.ModelElementTypeBuilder.ModelTypeInstanceProvider;

import org.flowable.bpm.model.bpmn.RelationshipDirection;
import org.flowable.bpm.model.bpmn.instance.BaseElement;
import org.flowable.bpm.model.bpmn.instance.Relationship;
import org.flowable.bpm.model.xml.ModelBuilder;
import org.flowable.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import org.flowable.bpm.model.xml.type.ModelElementTypeBuilder;
import org.flowable.bpm.model.xml.type.attribute.Attribute;
import org.flowable.bpm.model.xml.type.child.ChildElementCollection;
import org.flowable.bpm.model.xml.type.child.SequenceBuilder;

import java.util.Collection;

/**
 * The BPMN relationship element.
 */
public class RelationshipImpl
        extends BaseElementImpl
        implements Relationship {

    protected static Attribute<String> typeAttribute;
    protected static Attribute<RelationshipDirection> directionAttribute;
    protected static ChildElementCollection<Source> sourceCollection;
    protected static ChildElementCollection<Target> targetCollection;

    public static void registerType(ModelBuilder modelBuilder) {
        ModelElementTypeBuilder typeBuilder = modelBuilder.defineType(Relationship.class, BPMN_ELEMENT_RELATIONSHIP)
                .namespaceUri(BPMN20_NS)
                .extendsType(BaseElement.class)
                .instanceProvider(new ModelTypeInstanceProvider<Relationship>() {
                    @Override
                    public Relationship newInstance(ModelTypeInstanceContext instanceContext) {
                        return new RelationshipImpl(instanceContext);
                    }
                });

        typeAttribute = typeBuilder.stringAttribute(BPMN_ATTRIBUTE_TYPE)
                .required()
                .build();

        directionAttribute = typeBuilder.enumAttribute(BPMN_ATTRIBUTE_DIRECTION, RelationshipDirection.class)
                .build();

        SequenceBuilder sequenceBuilder = typeBuilder.sequence();

        sourceCollection = sequenceBuilder.elementCollection(Source.class)
                .minOccurs(1)
                .build();

        targetCollection = sequenceBuilder.elementCollection(Target.class)
                .minOccurs(1)
                .build();

        typeBuilder.build();
    }

    public RelationshipImpl(ModelTypeInstanceContext instanceContext) {
        super(instanceContext);
    }

    @Override
    public String getType() {
        return typeAttribute.getValue(this);
    }

    @Override
    public void setType(String type) {
        typeAttribute.setValue(this, type);
    }

    @Override
    public RelationshipDirection getDirection() {
        return directionAttribute.getValue(this);
    }

    @Override
    public void setDirection(RelationshipDirection direction) {
        directionAttribute.setValue(this, direction);
    }

    @Override
    public Collection<Source> getSources() {
        return sourceCollection.get(this);
    }

    @Override
    public Collection<Target> getTargets() {
        return targetCollection.get(this);
    }
}
