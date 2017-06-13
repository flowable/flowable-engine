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
package org.flowable.bpm.model.xml.impl.type;

import org.flowable.bpm.model.xml.Model;
import org.flowable.bpm.model.xml.ModelException;
import org.flowable.bpm.model.xml.impl.ModelBuildOperation;
import org.flowable.bpm.model.xml.impl.ModelImpl;
import org.flowable.bpm.model.xml.impl.type.attribute.BooleanAttributeBuilder;
import org.flowable.bpm.model.xml.impl.type.attribute.DoubleAttributeBuilder;
import org.flowable.bpm.model.xml.impl.type.attribute.EnumAttributeBuilder;
import org.flowable.bpm.model.xml.impl.type.attribute.IntegerAttributeBuilder;
import org.flowable.bpm.model.xml.impl.type.attribute.NamedEnumAttributeBuilder;
import org.flowable.bpm.model.xml.impl.type.attribute.StringAttributeBuilderImpl;
import org.flowable.bpm.model.xml.impl.type.child.SequenceBuilderImpl;
import org.flowable.bpm.model.xml.instance.ModelElementInstance;
import org.flowable.bpm.model.xml.type.ModelElementType;
import org.flowable.bpm.model.xml.type.ModelElementTypeBuilder;
import org.flowable.bpm.model.xml.type.attribute.AttributeBuilder;
import org.flowable.bpm.model.xml.type.attribute.StringAttributeBuilder;
import org.flowable.bpm.model.xml.type.child.SequenceBuilder;

import java.util.ArrayList;
import java.util.List;

public class ModelElementTypeBuilderImpl
        implements ModelElementTypeBuilder, ModelBuildOperation {

    private final ModelElementTypeImpl modelType;
    private final ModelImpl model;
    private final Class<? extends ModelElementInstance> instanceType;

    private final List<ModelBuildOperation> modelBuildOperations = new ArrayList<>();
    private Class<? extends ModelElementInstance> extendedType;

    public ModelElementTypeBuilderImpl(Class<? extends ModelElementInstance> instanceType, String name, ModelImpl model) {
        this.instanceType = instanceType;
        this.model = model;
        modelType = new ModelElementTypeImpl(model, name, instanceType);
    }

    @Override
    public ModelElementTypeBuilder extendsType(Class<? extends ModelElementInstance> extendedType) {
        this.extendedType = extendedType;
        return this;
    }

    @Override
    public <T extends ModelElementInstance> ModelElementTypeBuilder instanceProvider(ModelTypeInstanceProvider<T> instanceProvider) {
        modelType.setInstanceProvider(instanceProvider);
        return this;
    }

    @Override
    public ModelElementTypeBuilder namespaceUri(String namespaceUri) {
        modelType.setTypeNamespace(namespaceUri);
        return this;
    }

    @Override
    public AttributeBuilder<Boolean> booleanAttribute(String attributeName) {
        BooleanAttributeBuilder builder = new BooleanAttributeBuilder(attributeName, modelType);
        modelBuildOperations.add(builder);
        return builder;
    }

    @Override
    public StringAttributeBuilder stringAttribute(String attributeName) {
        StringAttributeBuilderImpl builder = new StringAttributeBuilderImpl(attributeName, modelType);
        modelBuildOperations.add(builder);
        return builder;
    }

    @Override
    public AttributeBuilder<Integer> integerAttribute(String attributeName) {
        IntegerAttributeBuilder builder = new IntegerAttributeBuilder(attributeName, modelType);
        modelBuildOperations.add(builder);
        return builder;
    }

    @Override
    public AttributeBuilder<Double> doubleAttribute(String attributeName) {
        DoubleAttributeBuilder builder = new DoubleAttributeBuilder(attributeName, modelType);
        modelBuildOperations.add(builder);
        return builder;
    }

    @Override
    public <V extends Enum<V>> AttributeBuilder<V> enumAttribute(String attributeName, Class<V> enumType) {
        EnumAttributeBuilder<V> builder = new EnumAttributeBuilder<>(attributeName, modelType, enumType);
        modelBuildOperations.add(builder);
        return builder;
    }

    @Override
    public <V extends Enum<V>> AttributeBuilder<V> namedEnumAttribute(String attributeName, Class<V> enumType) {
        NamedEnumAttributeBuilder<V> builder = new NamedEnumAttributeBuilder<>(attributeName, modelType, enumType);
        modelBuildOperations.add(builder);
        return builder;
    }

    @Override
    public ModelElementType build() {
        model.registerType(modelType, instanceType);
        return modelType;
    }

    @Override
    public ModelElementTypeBuilder abstractType() {
        modelType.setAbstract(true);
        return this;
    }

    @Override
    public SequenceBuilder sequence() {
        SequenceBuilderImpl builder = new SequenceBuilderImpl(modelType);
        modelBuildOperations.add(builder);
        return builder;
    }

    public void buildTypeHierarchy(Model model) {

        // build type hierarchy
        if (extendedType != null) {
            ModelElementTypeImpl extendedModelElementType = (ModelElementTypeImpl) model.getType(extendedType);
            if (extendedModelElementType == null) {
                throw new ModelException("Type " + modelType + " is defined to extend " + extendedType + " but no such type is defined.");

            } else {
                modelType.setBaseType(extendedModelElementType);
                extendedModelElementType.registerExtendingType(modelType);
            }
        }
    }

    @Override
    public void performModelBuild(Model model) {
        for (ModelBuildOperation operation : modelBuildOperations) {
            operation.performModelBuild(model);
        }
    }
}
