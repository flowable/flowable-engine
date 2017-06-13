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

public class DoubleAttributeBuilder
        extends AttributeBuilderImpl<Double> {

    public DoubleAttributeBuilder(String attributeName, ModelElementTypeImpl modelType) {
        super(attributeName, modelType, new DoubleAttribute(modelType));
    }

    @Override
    public DoubleAttributeBuilder namespace(String namespaceUri) {
        return (DoubleAttributeBuilder) super.namespace(namespaceUri);
    }

    @Override
    public DoubleAttributeBuilder defaultValue(Double defaultValue) {
        return (DoubleAttributeBuilder) super.defaultValue(defaultValue);
    }

    @Override
    public DoubleAttributeBuilder required() {
        return (DoubleAttributeBuilder) super.required();
    }

    @Override
    public DoubleAttributeBuilder idAttribute() {
        return (DoubleAttributeBuilder) super.idAttribute();
    }
}
