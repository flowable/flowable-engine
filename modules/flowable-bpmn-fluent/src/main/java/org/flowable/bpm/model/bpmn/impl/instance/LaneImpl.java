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
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.BPMN_ATTRIBUTE_PARTITION_ELEMENT_REF;
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.BPMN_ELEMENT_LANE;
import static org.flowable.bpm.model.xml.type.ModelElementTypeBuilder.ModelTypeInstanceProvider;

import org.flowable.bpm.model.bpmn.instance.BaseElement;
import org.flowable.bpm.model.bpmn.instance.FlowNode;
import org.flowable.bpm.model.bpmn.instance.Lane;
import org.flowable.bpm.model.xml.ModelBuilder;
import org.flowable.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import org.flowable.bpm.model.xml.type.ModelElementTypeBuilder;
import org.flowable.bpm.model.xml.type.attribute.Attribute;
import org.flowable.bpm.model.xml.type.child.ChildElement;
import org.flowable.bpm.model.xml.type.child.SequenceBuilder;
import org.flowable.bpm.model.xml.type.reference.AttributeReference;
import org.flowable.bpm.model.xml.type.reference.ElementReferenceCollection;

import java.util.Collection;

/**
 * The BPMN lane element.
 */
public class LaneImpl
        extends BaseElementImpl
        implements Lane {

    protected static Attribute<String> nameAttribute;
    protected static AttributeReference<PartitionElement> partitionElementRefAttribute;
    protected static ChildElement<PartitionElement> partitionElementChild;
    protected static ElementReferenceCollection<FlowNode, FlowNodeRef> flowNodeRefCollection;
    protected static ChildElement<ChildLaneSet> childLaneSetChild;

    public static void registerType(ModelBuilder modelBuilder) {
        ModelElementTypeBuilder typeBuilder = modelBuilder.defineType(Lane.class, BPMN_ELEMENT_LANE)
                .namespaceUri(BPMN20_NS)
                .extendsType(BaseElement.class)
                .instanceProvider(new ModelTypeInstanceProvider<Lane>() {
                    public Lane newInstance(ModelTypeInstanceContext instanceContext) {
                        return new LaneImpl(instanceContext);
                    }
                });

        nameAttribute = typeBuilder.stringAttribute(BPMN_ATTRIBUTE_NAME)
                .build();

        partitionElementRefAttribute = typeBuilder.stringAttribute(BPMN_ATTRIBUTE_PARTITION_ELEMENT_REF)
                .qNameAttributeReference(PartitionElement.class)
                .build();

        SequenceBuilder sequenceBuilder = typeBuilder.sequence();

        partitionElementChild = sequenceBuilder.element(PartitionElement.class)
                .build();

        flowNodeRefCollection = sequenceBuilder.elementCollection(FlowNodeRef.class)
                .idElementReferenceCollection(FlowNode.class)
                .build();

        childLaneSetChild = sequenceBuilder.element(ChildLaneSet.class)
                .build();


        typeBuilder.build();
    }

    public LaneImpl(ModelTypeInstanceContext instanceContext) {
        super(instanceContext);
    }

    public String getName() {
        return nameAttribute.getValue(this);
    }

    public void setName(String name) {
        nameAttribute.setValue(this, name);
    }

    public PartitionElement getPartitionElement() {
        return partitionElementRefAttribute.getReferenceTargetElement(this);
    }

    public void setPartitionElement(PartitionElement partitionElement) {
        partitionElementRefAttribute.setReferenceTargetElement(this, partitionElement);
    }

    public PartitionElement getPartitionElementChild() {
        return partitionElementChild.getChild(this);
    }

    public void setPartitionElementChild(PartitionElement partitionElement) {
        partitionElementChild.setChild(this, partitionElement);
    }

    public Collection<FlowNode> getFlowNodeRefs() {
        return flowNodeRefCollection.getReferenceTargetElements(this);
    }

    public ChildLaneSet getChildLaneSet() {
        return childLaneSetChild.getChild(this);
    }

    public void setChildLaneSet(ChildLaneSet childLaneSet) {
        childLaneSetChild.setChild(this, childLaneSet);
    }
}
