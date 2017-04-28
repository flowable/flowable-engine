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
package org.flowable.bpm.model.xml.impl.type.reference;

import org.flowable.bpm.model.xml.impl.type.attribute.AttributeImpl;
import org.flowable.bpm.model.xml.instance.ModelElementInstance;
import org.flowable.bpm.model.xml.type.ModelElementType;
import org.flowable.bpm.model.xml.type.attribute.Attribute;
import org.flowable.bpm.model.xml.type.reference.AttributeReference;

public class AttributeReferenceImpl<T extends ModelElementInstance>
        extends ReferenceImpl<T>
        implements AttributeReference<T> {

    protected final AttributeImpl<String> referenceSourceAttribute;

    public AttributeReferenceImpl(AttributeImpl<String> referenceSourceAttribute) {
        this.referenceSourceAttribute = referenceSourceAttribute;
    }

    @Override
    public String getReferenceIdentifier(ModelElementInstance referenceSourceElement) {
        return referenceSourceAttribute.getValue(referenceSourceElement);
    }

    @Override
    protected void setReferenceIdentifier(ModelElementInstance referenceSourceElement, String referenceIdentifier) {
        referenceSourceAttribute.setValue(referenceSourceElement, referenceIdentifier);
    }

    /**
     * Get the reference source attribute
     *
     * @return the reference source attribute
     */
    @Override
    public Attribute<String> getReferenceSourceAttribute() {
        return referenceSourceAttribute;
    }

    @Override
    public ModelElementType getReferenceSourceElementType() {
        return referenceSourceAttribute.getOwningElementType();
    }

    @Override
    protected void updateReference(ModelElementInstance referenceSourceElement, String oldIdentifier, String newIdentifier) {
        String referencingAttributeValue = getReferenceIdentifier(referenceSourceElement);
        if (oldIdentifier != null && oldIdentifier.equals(referencingAttributeValue)) {
            setReferenceIdentifier(referenceSourceElement, newIdentifier);
        }
    }

    @Override
    protected void removeReference(ModelElementInstance referenceSourceElement, ModelElementInstance referenceTargetElement) {
        referenceSourceAttribute.removeAttribute(referenceSourceElement);
    }

}
