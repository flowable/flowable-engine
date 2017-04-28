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
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.BPMN_ELEMENT_DATA_ASSOCIATION;
import static org.flowable.bpm.model.xml.type.ModelElementTypeBuilder.ModelTypeInstanceProvider;

import org.flowable.bpm.model.bpmn.instance.Assignment;
import org.flowable.bpm.model.bpmn.instance.BaseElement;
import org.flowable.bpm.model.bpmn.instance.DataAssociation;
import org.flowable.bpm.model.bpmn.instance.FormalExpression;
import org.flowable.bpm.model.bpmn.instance.ItemAwareElement;
import org.flowable.bpm.model.bpmn.instance.bpmndi.BpmnEdge;
import org.flowable.bpm.model.xml.ModelBuilder;
import org.flowable.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import org.flowable.bpm.model.xml.type.ModelElementTypeBuilder;
import org.flowable.bpm.model.xml.type.child.ChildElement;
import org.flowable.bpm.model.xml.type.child.ChildElementCollection;
import org.flowable.bpm.model.xml.type.child.SequenceBuilder;
import org.flowable.bpm.model.xml.type.reference.ElementReference;
import org.flowable.bpm.model.xml.type.reference.ElementReferenceCollection;

import java.util.Collection;

/**
 * The BPMN dataAssociation element.
 */
public class DataAssociationImpl
        extends BaseElementImpl
        implements DataAssociation {

    protected static ElementReferenceCollection<ItemAwareElement, SourceRef> sourceRefCollection;
    protected static ElementReference<ItemAwareElement, TargetRef> targetRefChild;
    protected static ChildElement<Transformation> transformationChild;
    protected static ChildElementCollection<Assignment> assignmentCollection;

    public static void registerType(ModelBuilder modelBuilder) {
        ModelElementTypeBuilder typeBuilder = modelBuilder.defineType(DataAssociation.class, BPMN_ELEMENT_DATA_ASSOCIATION)
                .namespaceUri(BPMN20_NS)
                .extendsType(BaseElement.class)
                .instanceProvider(new ModelTypeInstanceProvider<DataAssociation>() {
                    public DataAssociation newInstance(ModelTypeInstanceContext instanceContext) {
                        return new DataAssociationImpl(instanceContext);
                    }
                });

        SequenceBuilder sequenceBuilder = typeBuilder.sequence();

        sourceRefCollection = sequenceBuilder.elementCollection(SourceRef.class)
                .idElementReferenceCollection(ItemAwareElement.class)
                .build();

        targetRefChild = sequenceBuilder.element(TargetRef.class)
                .required()
                .idElementReference(ItemAwareElement.class)
                .build();

        transformationChild = sequenceBuilder.element(Transformation.class)
                .build();

        assignmentCollection = sequenceBuilder.elementCollection(Assignment.class)
                .build();

        typeBuilder.build();
    }

    public DataAssociationImpl(ModelTypeInstanceContext instanceContext) {
        super(instanceContext);
    }

    public Collection<ItemAwareElement> getSources() {
        return sourceRefCollection.getReferenceTargetElements(this);
    }

    public ItemAwareElement getTarget() {
        return targetRefChild.getReferenceTargetElement(this);
    }

    public void setTarget(ItemAwareElement target) {
        targetRefChild.setReferenceTargetElement(this, target);
    }

    public FormalExpression getTransformation() {
        return transformationChild.getChild(this);
    }

    public void setTransformation(Transformation transformation) {
        transformationChild.setChild(this, transformation);
    }

    public Collection<Assignment> getAssignments() {
        return assignmentCollection.get(this);
    }

    public BpmnEdge getDiagramElement() {
        return (BpmnEdge) super.getDiagramElement();
    }
}
