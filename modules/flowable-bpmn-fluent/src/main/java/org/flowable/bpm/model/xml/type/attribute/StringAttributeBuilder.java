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
package org.flowable.bpm.model.xml.type.attribute;

import org.flowable.bpm.model.xml.instance.ModelElementInstance;
import org.flowable.bpm.model.xml.type.reference.AttributeReferenceBuilder;
import org.flowable.bpm.model.xml.type.reference.AttributeReferenceCollection;
import org.flowable.bpm.model.xml.type.reference.AttributeReferenceCollectionBuilder;

public interface StringAttributeBuilder
        extends AttributeBuilder<String> {

    @Override
    StringAttributeBuilder namespace(String namespaceUri);

    @Override
    StringAttributeBuilder defaultValue(String defaultValue);

    @Override
    StringAttributeBuilder required();

    @Override
    StringAttributeBuilder idAttribute();

    <V extends ModelElementInstance> AttributeReferenceBuilder<V> qNameAttributeReference(Class<V> referenceTargetElement);

    <V extends ModelElementInstance> AttributeReferenceBuilder<V> idAttributeReference(Class<V> referenceTargetElement);

    @SuppressWarnings("rawtypes")
    <V extends ModelElementInstance> AttributeReferenceCollectionBuilder<V> idAttributeReferenceCollection(
            Class<V> referenceTargetElement,
            Class<? extends AttributeReferenceCollection> attributeReferenceCollection);

}
