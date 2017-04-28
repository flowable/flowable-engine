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
package org.flowable.bpm.model.xml.impl.type.attribute;

import org.flowable.bpm.model.xml.impl.type.ModelElementTypeImpl;

public class NamedEnumAttributeBuilder<T extends Enum<T>>
        extends AttributeBuilderImpl<T> {

    public NamedEnumAttributeBuilder(String attributeName, ModelElementTypeImpl modelType, Class<T> type) {
        super(attributeName, modelType, new NamedEnumAttribute<>(modelType, type));
    }

    @Override
    public NamedEnumAttributeBuilder<T> namespace(String namespaceUri) {
        return (NamedEnumAttributeBuilder<T>) super.namespace(namespaceUri);
    }

    @Override
    public NamedEnumAttributeBuilder<T> defaultValue(T defaultValue) {
        return (NamedEnumAttributeBuilder<T>) super.defaultValue(defaultValue);
    }

    @Override
    public NamedEnumAttributeBuilder<T> required() {
        return (NamedEnumAttributeBuilder<T>) super.required();
    }

    @Override
    public NamedEnumAttributeBuilder<T> idAttribute() {
        return (NamedEnumAttributeBuilder<T>) super.idAttribute();
    }

}
