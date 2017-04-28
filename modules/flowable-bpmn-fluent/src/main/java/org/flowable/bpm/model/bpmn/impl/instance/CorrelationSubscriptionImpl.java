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
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.BPMN_ATTRIBUTE_CORRELATION_KEY_REF;
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.BPMN_ELEMENT_CORRELATION_SUBSCRIPTION;
import static org.flowable.bpm.model.xml.type.ModelElementTypeBuilder.ModelTypeInstanceProvider;

import org.flowable.bpm.model.bpmn.instance.BaseElement;
import org.flowable.bpm.model.bpmn.instance.CorrelationKey;
import org.flowable.bpm.model.bpmn.instance.CorrelationPropertyBinding;
import org.flowable.bpm.model.bpmn.instance.CorrelationSubscription;
import org.flowable.bpm.model.xml.ModelBuilder;
import org.flowable.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import org.flowable.bpm.model.xml.type.ModelElementTypeBuilder;
import org.flowable.bpm.model.xml.type.child.ChildElementCollection;
import org.flowable.bpm.model.xml.type.child.SequenceBuilder;
import org.flowable.bpm.model.xml.type.reference.AttributeReference;

import java.util.Collection;

/**
 * The BPMN correlationSubscription element.
 */
public class CorrelationSubscriptionImpl
        extends BaseElementImpl
        implements CorrelationSubscription {

    protected static AttributeReference<CorrelationKey> correlationKeyAttribute;
    protected static ChildElementCollection<CorrelationPropertyBinding> correlationPropertyBindingCollection;

    public static void registerType(ModelBuilder modelBuilder) {
        ModelElementTypeBuilder typeBuilder = modelBuilder.defineType(CorrelationSubscription.class, BPMN_ELEMENT_CORRELATION_SUBSCRIPTION)
                .namespaceUri(BPMN20_NS)
                .extendsType(BaseElement.class)
                .instanceProvider(new ModelTypeInstanceProvider<CorrelationSubscription>() {
                    public CorrelationSubscription newInstance(ModelTypeInstanceContext instanceContext) {
                        return new CorrelationSubscriptionImpl(instanceContext);
                    }
                });

        correlationKeyAttribute = typeBuilder.stringAttribute(BPMN_ATTRIBUTE_CORRELATION_KEY_REF)
                .required()
                .qNameAttributeReference(CorrelationKey.class)
                .build();

        SequenceBuilder sequenceBuilder = typeBuilder.sequence();

        correlationPropertyBindingCollection = sequenceBuilder.elementCollection(CorrelationPropertyBinding.class)
                .build();

        typeBuilder.build();
    }

    public CorrelationSubscriptionImpl(ModelTypeInstanceContext instanceContext) {
        super(instanceContext);
    }

    public CorrelationKey getCorrelationKey() {
        return correlationKeyAttribute.getReferenceTargetElement(this);
    }

    public void setCorrelationKey(CorrelationKey correlationKey) {
        correlationKeyAttribute.setReferenceTargetElement(this, correlationKey);
    }

    public Collection<CorrelationPropertyBinding> getCorrelationPropertyBindings() {
        return correlationPropertyBindingCollection.get(this);
    }
}
