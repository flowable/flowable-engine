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
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.BPMN_ATTRIBUTE_ATTACHED_TO_REF;
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.BPMN_ATTRIBUTE_CANCEL_ACTIVITY;
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.BPMN_ELEMENT_BOUNDARY_EVENT;
import static org.flowable.bpm.model.xml.type.ModelElementTypeBuilder.ModelTypeInstanceProvider;

import org.flowable.bpm.model.bpmn.BpmnModelInstance;
import org.flowable.bpm.model.bpmn.builder.BoundaryEventBuilder;
import org.flowable.bpm.model.bpmn.instance.Activity;
import org.flowable.bpm.model.bpmn.instance.BoundaryEvent;
import org.flowable.bpm.model.bpmn.instance.CatchEvent;
import org.flowable.bpm.model.xml.ModelBuilder;
import org.flowable.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import org.flowable.bpm.model.xml.type.ModelElementTypeBuilder;
import org.flowable.bpm.model.xml.type.attribute.Attribute;
import org.flowable.bpm.model.xml.type.reference.AttributeReference;

/**
 * The BPMN boundaryEvent element.
 */
public class BoundaryEventImpl
        extends CatchEventImpl
        implements BoundaryEvent {

    protected static Attribute<Boolean> cancelActivityAttribute;
    protected static AttributeReference<Activity> attachedToRefAttribute;

    public static void registerType(ModelBuilder modelBuilder) {
        ModelElementTypeBuilder typeBuilder = modelBuilder.defineType(BoundaryEvent.class, BPMN_ELEMENT_BOUNDARY_EVENT)
                .namespaceUri(BPMN20_NS)
                .extendsType(CatchEvent.class)
                .instanceProvider(new ModelTypeInstanceProvider<BoundaryEvent>() {
                    @Override
                    public BoundaryEvent newInstance(ModelTypeInstanceContext instanceContext) {
                        return new BoundaryEventImpl(instanceContext);
                    }
                });

        cancelActivityAttribute = typeBuilder.booleanAttribute(BPMN_ATTRIBUTE_CANCEL_ACTIVITY)
                .defaultValue(true)
                .build();

        attachedToRefAttribute = typeBuilder.stringAttribute(BPMN_ATTRIBUTE_ATTACHED_TO_REF)
                .required()
                .qNameAttributeReference(Activity.class)
                .build();

        typeBuilder.build();
    }

    public BoundaryEventImpl(ModelTypeInstanceContext context) {
        super(context);
    }

    @Override
    public BoundaryEventBuilder builder() {
        return new BoundaryEventBuilder((BpmnModelInstance) modelInstance, this);
    }

    @Override
    public boolean cancelActivity() {
        return cancelActivityAttribute.getValue(this);
    }

    @Override
    public void setCancelActivity(boolean cancelActivity) {
        cancelActivityAttribute.setValue(this, cancelActivity);
    }

    @Override
    public Activity getAttachedTo() {
        return attachedToRefAttribute.getReferenceTargetElement(this);
    }

    @Override
    public void setAttachedTo(Activity attachedTo) {
        attachedToRefAttribute.setReferenceTargetElement(this, attachedTo);
    }

    /** Flowable Attributes */

    @Override
    public boolean isFlowableAsync() {
        throw new UnsupportedOperationException("'async' is not supported for 'Boundary Events' right now.");
    }

    @Override
    public void setFlowableAsync(boolean isFlowableAsync) {
        throw new UnsupportedOperationException("'async' is not supported for 'Boundary Events' right now.");
    }

    @Override
    public boolean isFlowableExclusive() {
        throw new UnsupportedOperationException("'exclusive' is not supported for 'Boundary Events' right now.");
    }

    @Override
    public void setFlowableExclusive(boolean isFlowableExclusive) {
        throw new UnsupportedOperationException("'exclusive' is not supported for 'Boundary Events' right now.");
    }
}
