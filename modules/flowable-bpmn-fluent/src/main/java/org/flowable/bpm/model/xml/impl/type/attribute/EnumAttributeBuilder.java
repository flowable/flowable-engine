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

public class EnumAttributeBuilder<T extends Enum<T>>
        extends AttributeBuilderImpl<T> {

    public EnumAttributeBuilder(String attributeName, ModelElementTypeImpl modelType, Class<T> type) {
        super(attributeName, modelType, new EnumAttribute<>(modelType, type));
    }

    @Override
    public EnumAttributeBuilder<T> namespace(String namespaceUri) {
        return (EnumAttributeBuilder<T>) super.namespace(namespaceUri);
    }

    @Override
    public EnumAttributeBuilder<T> defaultValue(T defaultValue) {
        return (EnumAttributeBuilder<T>) super.defaultValue(defaultValue);
    }

    @Override
    public EnumAttributeBuilder<T> required() {
        return (EnumAttributeBuilder<T>) super.required();
    }

    @Override
    public EnumAttributeBuilder<T> idAttribute() {
        return (EnumAttributeBuilder<T>) super.idAttribute();
    }
}
