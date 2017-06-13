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
package org.flowable.bpm.model.xml.type.child;

import org.flowable.bpm.model.xml.instance.ModelElementInstance;
import org.flowable.bpm.model.xml.type.reference.ElementReferenceBuilder;

public interface ChildElementBuilder<T extends ModelElementInstance>
        extends ChildElementCollectionBuilder<T> {

    @Override
    ChildElementBuilder<T> immutable();

    @Override
    ChildElementBuilder<T> required();

    @Override
    ChildElementBuilder<T> minOccurs(int i);

    @Override
    ChildElementBuilder<T> maxOccurs(int i);

    @Override
    ChildElement<T> build();

    <V extends ModelElementInstance> ElementReferenceBuilder<V, T> qNameElementReference(Class<V> referenceTargetType);

    <V extends ModelElementInstance> ElementReferenceBuilder<V, T> idElementReference(Class<V> referenceTargetType);

    <V extends ModelElementInstance> ElementReferenceBuilder<V, T> uriElementReference(Class<V> referenceTargetType);

}
