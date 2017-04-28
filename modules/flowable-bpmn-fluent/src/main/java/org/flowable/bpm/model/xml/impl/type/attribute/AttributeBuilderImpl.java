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

import org.flowable.bpm.model.xml.Model;
import org.flowable.bpm.model.xml.impl.ModelBuildOperation;
import org.flowable.bpm.model.xml.impl.type.ModelElementTypeImpl;
import org.flowable.bpm.model.xml.type.attribute.Attribute;
import org.flowable.bpm.model.xml.type.attribute.AttributeBuilder;

public abstract class AttributeBuilderImpl<T>
        implements AttributeBuilder<T>, ModelBuildOperation {

    private final AttributeImpl<T> attribute;
    private final ModelElementTypeImpl modelType;

    AttributeBuilderImpl(String attributeName, ModelElementTypeImpl modelType, AttributeImpl<T> attribute) {
        this.modelType = modelType;
        this.attribute = attribute;
        attribute.setAttributeName(attributeName);
    }

    @Override
    public AttributeBuilder<T> namespace(String namespaceUri) {
        attribute.setNamespaceUri(namespaceUri);
        return this;
    }

    @Override
    public AttributeBuilder<T> idAttribute() {
        attribute.setId();
        return this;
    }


    @Override
    public AttributeBuilder<T> defaultValue(T defaultValue) {
        attribute.setDefaultValue(defaultValue);
        return this;
    }

    @Override
    public AttributeBuilder<T> required() {
        attribute.setRequired(true);
        return this;
    }

    @Override
    public Attribute<T> build() {
        modelType.registerAttribute(attribute);
        return attribute;
    }

    @Override
    public void performModelBuild(Model model) {
        // do nothing
    }

}
