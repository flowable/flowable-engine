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
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.BPMN_ATTRIBUTE_SOURCE_REF;
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.BPMN_ATTRIBUTE_TARGET_REF;
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.BPMN_ELEMENT_FLOW_NODE;
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.FLOWABLE_ATTRIBUTE_ASYNC;
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.FLOWABLE_ATTRIBUTE_EXCLUSIVE;
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.FLOWABLE_NS;

import org.flowable.bpm.model.bpmn.BpmnModelException;
import org.flowable.bpm.model.bpmn.Query;
import org.flowable.bpm.model.bpmn.builder.AbstractFlowNodeBuilder;
import org.flowable.bpm.model.bpmn.impl.QueryImpl;
import org.flowable.bpm.model.bpmn.instance.FlowElement;
import org.flowable.bpm.model.bpmn.instance.FlowNode;
import org.flowable.bpm.model.bpmn.instance.SequenceFlow;
import org.flowable.bpm.model.xml.ModelBuilder;
import org.flowable.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import org.flowable.bpm.model.xml.instance.ModelElementInstance;
import org.flowable.bpm.model.xml.type.ModelElementTypeBuilder;
import org.flowable.bpm.model.xml.type.attribute.Attribute;
import org.flowable.bpm.model.xml.type.child.SequenceBuilder;
import org.flowable.bpm.model.xml.type.reference.AttributeReference;
import org.flowable.bpm.model.xml.type.reference.ElementReferenceCollection;
import org.flowable.bpm.model.xml.type.reference.Reference;

import java.util.Collection;
import java.util.HashSet;

/**
 * The BPMN flowNode element.
 */
public abstract class FlowNodeImpl
        extends FlowElementImpl
        implements FlowNode {

    protected static ElementReferenceCollection<SequenceFlow, Incoming> incomingCollection;
    protected static ElementReferenceCollection<SequenceFlow, Outgoing> outgoingCollection;

    /** Flowable Attributes */
    protected static Attribute<Boolean> flowableAsync;
    protected static Attribute<Boolean> flowableExclusive;

    public static void registerType(ModelBuilder modelBuilder) {
        ModelElementTypeBuilder typeBuilder = modelBuilder.defineType(FlowNode.class, BPMN_ELEMENT_FLOW_NODE)
                .namespaceUri(BPMN20_NS)
                .extendsType(FlowElement.class)
                .abstractType();

        SequenceBuilder sequenceBuilder = typeBuilder.sequence();

        incomingCollection = sequenceBuilder.elementCollection(Incoming.class)
                .qNameElementReferenceCollection(SequenceFlow.class)
                .build();

        outgoingCollection = sequenceBuilder.elementCollection(Outgoing.class)
                .qNameElementReferenceCollection(SequenceFlow.class)
                .build();

        /* Flowable Attributes */

        flowableAsync = typeBuilder.booleanAttribute(FLOWABLE_ATTRIBUTE_ASYNC)
                .namespace(FLOWABLE_NS)
                .defaultValue(false)
                .build();

        flowableExclusive = typeBuilder.booleanAttribute(FLOWABLE_ATTRIBUTE_EXCLUSIVE)
                .namespace(FLOWABLE_NS)
                .defaultValue(true)
                .build();

        typeBuilder.build();
    }

    public FlowNodeImpl(ModelTypeInstanceContext context) {
        super(context);
    }

    @SuppressWarnings("rawtypes")
    public AbstractFlowNodeBuilder builder() {
        throw new BpmnModelException("No builder implemented for type " + getElementType().getTypeNamespace() + ':' + getElementType().getTypeName());
    }

    @SuppressWarnings("rawtypes")
    public void updateAfterReplacement() {
        super.updateAfterReplacement();
        Collection<Reference> incomingReferences = getIncomingReferencesByType(SequenceFlow.class);
        for (Reference<?> reference : incomingReferences) {
            for (ModelElementInstance sourceElement : reference.findReferenceSourceElements(this)) {
                String referenceIdentifier = reference.getReferenceIdentifier(sourceElement);

                if (referenceIdentifier != null && referenceIdentifier.equals(getId()) && reference instanceof AttributeReference) {
                    String attributeName = ((AttributeReference) reference).getReferenceSourceAttribute().getAttributeName();
                    if (attributeName.equals(BPMN_ATTRIBUTE_SOURCE_REF)) {
                        getOutgoing().add((SequenceFlow) sourceElement);
                    } else if (attributeName.equals(BPMN_ATTRIBUTE_TARGET_REF)) {
                        getIncoming().add((SequenceFlow) sourceElement);
                    }
                }
            }

        }
    }

    public Collection<SequenceFlow> getIncoming() {
        return incomingCollection.getReferenceTargetElements(this);
    }

    public Collection<SequenceFlow> getOutgoing() {
        return outgoingCollection.getReferenceTargetElements(this);
    }

    public Query<FlowNode> getPreviousNodes() {
        Collection<FlowNode> previousNodes = new HashSet<>();
        for (SequenceFlow sequenceFlow : getIncoming()) {
            previousNodes.add(sequenceFlow.getSource());
        }
        return new QueryImpl<>(previousNodes);
    }

    public Query<FlowNode> getSucceedingNodes() {
        Collection<FlowNode> succeedingNodes = new HashSet<>();
        for (SequenceFlow sequenceFlow : getOutgoing()) {
            succeedingNodes.add(sequenceFlow.getTarget());
        }
        return new QueryImpl<>(succeedingNodes);
    }

    /** Flowable Attributes */

    public boolean isFlowableAsync() {
        return flowableAsync.getValue(this);
    }

    public void setFlowableAsync(boolean isFlowableAsync) {
        flowableAsync.setValue(this, isFlowableAsync);
    }

    public boolean isFlowableExclusive() {
        return flowableExclusive.getValue(this);
    }

    public void setFlowableExclusive(boolean isFlowableExclusive) {
        flowableExclusive.setValue(this, isFlowableExclusive);
    }
}
