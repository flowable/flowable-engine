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

import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.BPMN_ATTRIBUTE_COMPLETION_QUANTITY;
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.BPMN_ATTRIBUTE_IS_FOR_COMPENSATION;
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.BPMN_ATTRIBUTE_START_QUANTITY;
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.BPMN_ELEMENT_ACTIVITY;

import java.util.Collection;

import org.flowable.bpm.model.bpmn.impl.BpmnModelConstants;
import org.flowable.bpm.model.bpmn.instance.Activity;
import org.flowable.bpm.model.bpmn.instance.DataInputAssociation;
import org.flowable.bpm.model.bpmn.instance.DataOutputAssociation;
import org.flowable.bpm.model.bpmn.instance.FlowNode;
import org.flowable.bpm.model.bpmn.instance.IoSpecification;
import org.flowable.bpm.model.bpmn.instance.LoopCharacteristics;
import org.flowable.bpm.model.bpmn.instance.Property;
import org.flowable.bpm.model.bpmn.instance.ResourceRole;
import org.flowable.bpm.model.xml.ModelBuilder;
import org.flowable.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import org.flowable.bpm.model.xml.type.ModelElementTypeBuilder;
import org.flowable.bpm.model.xml.type.attribute.Attribute;
import org.flowable.bpm.model.xml.type.child.ChildElement;
import org.flowable.bpm.model.xml.type.child.ChildElementCollection;
import org.flowable.bpm.model.xml.type.child.SequenceBuilder;

/**
 * The BPMN activity element.
 */
public abstract class ActivityImpl
        extends FlowNodeImpl
        implements Activity {

    protected static Attribute<Boolean> isForCompensationAttribute;
    protected static Attribute<Integer> startQuantityAttribute;
    protected static Attribute<Integer> completionQuantityAttribute;
    protected static ChildElement<IoSpecification> ioSpecificationChild;
    protected static ChildElementCollection<Property> propertyCollection;
    protected static ChildElementCollection<DataInputAssociation> dataInputAssociationCollection;
    protected static ChildElementCollection<DataOutputAssociation> dataOutputAssociationCollection;
    protected static ChildElementCollection<ResourceRole> resourceRoleCollection;
    protected static ChildElement<LoopCharacteristics> loopCharacteristicsChild;

    public static void registerType(ModelBuilder modelBuilder) {
        ModelElementTypeBuilder typeBuilder = modelBuilder.defineType(Activity.class, BPMN_ELEMENT_ACTIVITY)
                .namespaceUri(BpmnModelConstants.BPMN20_NS)
                .extendsType(FlowNode.class)
                .abstractType();

        isForCompensationAttribute = typeBuilder.booleanAttribute(BPMN_ATTRIBUTE_IS_FOR_COMPENSATION)
                .defaultValue(false)
                .build();

        startQuantityAttribute = typeBuilder.integerAttribute(BPMN_ATTRIBUTE_START_QUANTITY)
                .defaultValue(1)
                .build();

        completionQuantityAttribute = typeBuilder.integerAttribute(BPMN_ATTRIBUTE_COMPLETION_QUANTITY)
                .defaultValue(1)
                .build();

        SequenceBuilder sequenceBuilder = typeBuilder.sequence();

        ioSpecificationChild = sequenceBuilder.element(IoSpecification.class)
                .build();

        propertyCollection = sequenceBuilder.elementCollection(Property.class)
                .build();

        dataInputAssociationCollection = sequenceBuilder.elementCollection(DataInputAssociation.class)
                .build();

        dataOutputAssociationCollection = sequenceBuilder.elementCollection(DataOutputAssociation.class)
                .build();

        resourceRoleCollection = sequenceBuilder.elementCollection(ResourceRole.class)
                .build();

        loopCharacteristicsChild = sequenceBuilder.element(LoopCharacteristics.class)
                .build();

        typeBuilder.build();
    }

    public ActivityImpl(ModelTypeInstanceContext context) {
        super(context);
    }

    public boolean isForCompensation() {
        return isForCompensationAttribute.getValue(this);
    }

    public void setForCompensation(boolean isForCompensation) {
        isForCompensationAttribute.setValue(this, isForCompensation);
    }

    public int getStartQuantity() {
        return startQuantityAttribute.getValue(this);
    }

    public void setStartQuantity(int startQuantity) {
        startQuantityAttribute.setValue(this, startQuantity);
    }

    public int getCompletionQuantity() {
        return completionQuantityAttribute.getValue(this);
    }

    public void setCompletionQuantity(int completionQuantity) {
        completionQuantityAttribute.setValue(this, completionQuantity);
    }

    public IoSpecification getIoSpecification() {
        return ioSpecificationChild.getChild(this);
    }

    public void setIoSpecification(IoSpecification ioSpecification) {
        ioSpecificationChild.setChild(this, ioSpecification);
    }

    public Collection<Property> getProperties() {
        return propertyCollection.get(this);
    }

    public Collection<DataInputAssociation> getDataInputAssociations() {
        return dataInputAssociationCollection.get(this);
    }

    public Collection<DataOutputAssociation> getDataOutputAssociations() {
        return dataOutputAssociationCollection.get(this);
    }

    public Collection<ResourceRole> getResourceRoles() {
        return resourceRoleCollection.get(this);
    }

    public LoopCharacteristics getLoopCharacteristics() {
        return loopCharacteristicsChild.getChild(this);
    }

    public void setLoopCharacteristics(LoopCharacteristics loopCharacteristics) {
        loopCharacteristicsChild.setChild(this, loopCharacteristics);
    }
}
