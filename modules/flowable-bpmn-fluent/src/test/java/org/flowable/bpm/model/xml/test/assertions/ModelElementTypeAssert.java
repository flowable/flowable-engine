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
package org.flowable.bpm.model.xml.test.assertions;

import org.assertj.core.api.AbstractAssert;
import org.flowable.bpm.model.xml.Model;
import org.flowable.bpm.model.xml.impl.util.QName;
import org.flowable.bpm.model.xml.instance.ModelElementInstance;
import org.flowable.bpm.model.xml.type.ModelElementType;
import org.flowable.bpm.model.xml.type.attribute.Attribute;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class ModelElementTypeAssert
        extends AbstractAssert<ModelElementTypeAssert, ModelElementType> {

    private final String typeName;

    protected ModelElementTypeAssert(ModelElementType actual) {
        super(actual, ModelElementTypeAssert.class);
        typeName = actual.getTypeName();
    }

    private List<String> getActualAttributeNames() {
        List<String> actualAttributeNames = new ArrayList<>();
        for (Attribute<?> attribute : actual.getAttributes()) {
            actualAttributeNames.add(attribute.getAttributeName());
        }
        return actualAttributeNames;
    }

    private Collection<String> getTypeNames(Collection<ModelElementType> elementTypes) {
        List<String> typeNames = new ArrayList<>();
        QName qName;
        for (ModelElementType elementType : elementTypes) {
            qName = new QName(elementType.getTypeNamespace(), elementType.getTypeName());
            typeNames.add(qName.toString());
        }
        return typeNames;
    }

    public ModelElementTypeAssert isAbstract() {
        isNotNull();

        if (!actual.isAbstract()) {
            failWithMessage("Expected element type <%s> to be abstract but was not", typeName);
        }

        return this;
    }

    public ModelElementTypeAssert isNotAbstract() {
        isNotNull();

        if (actual.isAbstract()) {
            failWithMessage("Expected element type <%s> not to be abstract but was", typeName);
        }

        return this;
    }

    public ModelElementTypeAssert extendsType(ModelElementType baseType) {
        isNotNull();

        ModelElementType actualBaseType = actual.getBaseType();

        if (!actualBaseType.equals(baseType)) {
            failWithMessage("Expected element type <%s> to extend type <%s> but extends <%s>", typeName, actualBaseType.getTypeName(), baseType.getTypeName());
        }

        return this;
    }

    public ModelElementTypeAssert extendsNoType() {
        isNotNull();

        ModelElementType actualBaseType = actual.getBaseType();

        if (actualBaseType != null) {
            failWithMessage("Expected element type <%s> to not extend any type but extends <%s>", typeName, actualBaseType.getTypeName());
        }

        return this;
    }

    public ModelElementTypeAssert hasAttributes() {
        isNotNull();

        List<Attribute<?>> actualAttributes = actual.getAttributes();

        if (actualAttributes.isEmpty()) {
            failWithMessage("Expected element type <%s> to have attributes but has none", typeName);
        }

        return this;
    }

    public ModelElementTypeAssert hasAttributes(String... attributeNames) {
        isNotNull();

        List<String> actualAttributeNames = getActualAttributeNames();

        if (!actualAttributeNames.containsAll(Arrays.asList(attributeNames))) {
            failWithMessage("Expected element type <%s> to have attributes <%s> but has <%s>", typeName, attributeNames, actualAttributeNames);
        }

        return this;
    }

    public ModelElementTypeAssert hasNoAttributes() {
        isNotNull();

        List<String> actualAttributeNames = getActualAttributeNames();

        if (!actualAttributeNames.isEmpty()) {
            failWithMessage("Expected element type <%s> to have no attributes but has <%s>", typeName, actualAttributeNames);
        }

        return this;
    }

    public ModelElementTypeAssert hasChildElements() {
        isNotNull();

        List<ModelElementType> childElementTypes = actual.getChildElementTypes();

        if (childElementTypes.isEmpty()) {
            failWithMessage("Expected element type <%s> to have child elements but has non", typeName);
        }

        return this;
    }

    public ModelElementTypeAssert hasChildElements(ModelElementType... types) {
        isNotNull();

        List<ModelElementType> childElementTypes = Arrays.asList(types);
        List<ModelElementType> actualChildElementTypes = actual.getChildElementTypes();

        if (!actualChildElementTypes.containsAll(childElementTypes)) {
            Collection<String> typeNames = getTypeNames(childElementTypes);
            Collection<String> actualTypeNames = getTypeNames(actualChildElementTypes);
            failWithMessage("Expected element type <%s> to have child elements <%s> but has <%s>", typeName, typeNames, actualTypeNames);
        }

        return this;
    }

    public ModelElementTypeAssert hasNoChildElements() {
        isNotNull();

        Collection<String> actualChildElementTypeNames = getTypeNames(actual.getChildElementTypes());

        if (!actualChildElementTypeNames.isEmpty()) {
            failWithMessage("Expected element type <%s> to have no child elements but has <%s>", typeName, actualChildElementTypeNames);
        }

        return this;
    }

    public ModelElementTypeAssert hasTypeName(String typeName) {
        isNotNull();

        if (!typeName.equals(this.typeName)) {
            failWithMessage("Expected element type to have name <%s> but was <%s>", typeName, this.typeName);
        }

        return this;
    }

    public ModelElementTypeAssert hasTypeNamespace(String typeNamespace) {
        isNotNull();

        String actualTypeNamespace = actual.getTypeNamespace();

        if (!typeNamespace.equals(actualTypeNamespace)) {
            failWithMessage("Expected element type <%s> has type namespace <%s> but was <%s>", typeName, typeNamespace, actualTypeNamespace);
        }

        return this;
    }

    public ModelElementTypeAssert hasInstanceType(Class<? extends ModelElementInstance> instanceType) {
        isNotNull();

        Class<? extends ModelElementInstance> actualInstanceType = actual.getInstanceType();

        if (!instanceType.equals(actualInstanceType)) {
            failWithMessage("Expected element type <%s> has instance type <%s> but was <%s>", typeName, instanceType, actualInstanceType);
        }

        return this;
    }

    public ModelElementTypeAssert isExtended() {
        isNotNull();

        Collection<ModelElementType> actualExtendingTypes = actual.getExtendingTypes();

        if (actualExtendingTypes.isEmpty()) {
            failWithMessage("Expected element type <%s> to be extended by types but was not", typeName);
        }

        return this;
    }

    public ModelElementTypeAssert isExtendedBy(ModelElementType... types) {
        isNotNull();

        List<ModelElementType> extendingTypes = Arrays.asList(types);
        Collection<ModelElementType> actualExtendingTypes = actual.getExtendingTypes();

        if (!actualExtendingTypes.containsAll(extendingTypes)) {
            Collection<String> typeNames = getTypeNames(extendingTypes);
            Collection<String> actualTypeNames = getTypeNames(actualExtendingTypes);
            failWithMessage("Expected element type <%s> to be extended by types <%s> but is extended by <%s>", typeName, typeNames, actualTypeNames);
        }

        return this;
    }

    public ModelElementTypeAssert isNotExtended() {
        isNotNull();

        Collection<String> actualExtendingTypeNames = getTypeNames(actual.getExtendingTypes());

        if (!actualExtendingTypeNames.isEmpty()) {
            failWithMessage("Expected element type <%s> to be not extend but is extended by <%s>", typeName, actualExtendingTypeNames);
        }

        return this;
    }

    public ModelElementTypeAssert isNotExtendedBy(ModelElementType... types) {
        isNotNull();

        List<ModelElementType> notExtendingTypes = Arrays.asList(types);
        Collection<ModelElementType> actualExtendingTypes = actual.getExtendingTypes();

        List<ModelElementType> errorTypes = new ArrayList<>();

        for (ModelElementType notExtendingType : notExtendingTypes) {
            if (actualExtendingTypes.contains(notExtendingType)) {
                errorTypes.add(notExtendingType);
            }
        }

        if (!errorTypes.isEmpty()) {
            Collection<String> errorTypeNames = getTypeNames(errorTypes);
            Collection<String> notExtendingTypeNames = getTypeNames(notExtendingTypes);
            failWithMessage("Expected element type <%s> to be not extended by types <%s> but is extended by <%s>", typeName, notExtendingTypeNames,
                    errorTypeNames);
        }

        return this;
    }

    public ModelElementTypeAssert isPartOfModel(Model model) {
        isNotNull();

        Model actualModel = actual.getModel();

        if (!model.equals(actualModel)) {
            failWithMessage("Expected element type <%s> to be part of model <%s> but was part of <%s>", typeName, model.getModelName(),
                    actualModel.getModelName());
        }

        return this;
    }
}
