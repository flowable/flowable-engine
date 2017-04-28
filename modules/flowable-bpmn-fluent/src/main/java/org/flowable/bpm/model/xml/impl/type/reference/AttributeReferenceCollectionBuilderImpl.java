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

import org.flowable.bpm.model.xml.Model;
import org.flowable.bpm.model.xml.ModelException;
import org.flowable.bpm.model.xml.impl.ModelBuildOperation;
import org.flowable.bpm.model.xml.impl.type.ModelElementTypeImpl;
import org.flowable.bpm.model.xml.impl.type.attribute.AttributeImpl;
import org.flowable.bpm.model.xml.instance.ModelElementInstance;
import org.flowable.bpm.model.xml.type.reference.AttributeReferenceCollection;
import org.flowable.bpm.model.xml.type.reference.AttributeReferenceCollectionBuilder;

public class AttributeReferenceCollectionBuilderImpl<T extends ModelElementInstance>
        implements AttributeReferenceCollectionBuilder<T>, ModelBuildOperation {

    private final AttributeImpl<String> referenceSourceAttribute;
    protected AttributeReferenceCollection<T> attributeReferenceCollection;
    private final Class<T> referenceTargetElement;

    @SuppressWarnings({"unchecked", "rawtypes"})
    public AttributeReferenceCollectionBuilderImpl(AttributeImpl<String> attribute, Class<T> referenceTargetElement,
            Class<? extends AttributeReferenceCollection> attributeReferenceCollection) {
        this.referenceSourceAttribute = attribute;
        this.referenceTargetElement = referenceTargetElement;
        try {
            this.attributeReferenceCollection = (AttributeReferenceCollection<T>) attributeReferenceCollection.getConstructor(AttributeImpl.class)
                    .newInstance(referenceSourceAttribute);
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public AttributeReferenceCollection<T> build() {
        referenceSourceAttribute.registerOutgoingReference(attributeReferenceCollection);
        return attributeReferenceCollection;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void performModelBuild(Model model) {
        // register declaring type as a referencing type of referenced type
        ModelElementTypeImpl referenceTargetType = (ModelElementTypeImpl) model.getType(referenceTargetElement);

        // the actual referenced type
        attributeReferenceCollection.setReferenceTargetElementType(referenceTargetType);

        // the referenced attribute may be declared on a base type of the referenced type.
        AttributeImpl<String> idAttribute = (AttributeImpl<String>) referenceTargetType.getAttribute("id");
        if (idAttribute != null) {
            idAttribute.registerIncoming(attributeReferenceCollection);
            attributeReferenceCollection.setReferenceTargetAttribute(idAttribute);
        } else {
            throw new ModelException(
                    "Element type " + referenceTargetType.getTypeNamespace() + ':' + referenceTargetType.getTypeName() + " has no id attribute");
        }
    }

}
