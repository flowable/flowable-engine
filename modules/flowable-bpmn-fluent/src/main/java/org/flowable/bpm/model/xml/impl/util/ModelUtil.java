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
package org.flowable.bpm.model.xml.impl.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.flowable.bpm.model.xml.Model;
import org.flowable.bpm.model.xml.ModelException;
import org.flowable.bpm.model.xml.impl.ModelInstanceImpl;
import org.flowable.bpm.model.xml.impl.instance.ModelElementInstanceImpl;
import org.flowable.bpm.model.xml.impl.type.ModelElementTypeImpl;
import org.flowable.bpm.model.xml.impl.type.attribute.StringAttribute;
import org.flowable.bpm.model.xml.instance.DomElement;
import org.flowable.bpm.model.xml.instance.ModelElementInstance;
import org.flowable.bpm.model.xml.type.ModelElementType;
import org.flowable.bpm.model.xml.type.attribute.Attribute;

/**
 * Some Helpers useful when handling model elements.
 */
public final class ModelUtil {

    private ModelUtil() {}

    /**
     * Returns the {@link ModelElementInstanceImpl ModelElement} for a DOM element. If the model element does not yet exist, it is created and linked
     * to the DOM.
     *
     * @param domElement the child element to create a new {@link ModelElementInstanceImpl ModelElement} for
     * @return the child model element
     */
    public static ModelElementInstance getModelElement(DomElement domElement, ModelInstanceImpl modelInstance) {
        ModelElementInstance modelElement = domElement.getModelElementInstance();
        if (modelElement == null) {
            ModelElementTypeImpl modelType = getModelElement(domElement, modelInstance, domElement.getNamespaceURI());
            modelElement = modelType.newInstance(modelInstance, domElement);
            domElement.setModelElementInstance(modelElement);
        }
        return modelElement;
    }

    protected static ModelElementTypeImpl getModelElement(DomElement domElement, ModelInstanceImpl modelInstance, String namespaceUri) {
        String localName = domElement.getLocalName();
        ModelElementTypeImpl modelType = (ModelElementTypeImpl) modelInstance.getModel().getTypeForName(namespaceUri, localName);

        if (modelType == null) {

            Model model = modelInstance.getModel();
            String actualNamespaceUri = model.getActualNamespace(namespaceUri);

            modelType = actualNamespaceUri != null ? getModelElement(domElement, modelInstance, actualNamespaceUri)
                    : (ModelElementTypeImpl) modelInstance.registerGenericType(namespaceUri, localName);
        }
        return modelType;
    }

    public static QName getQName(String namespaceUri, String localName) {
        return new QName(namespaceUri, localName);
    }

    public static void ensureInstanceOf(Object instance, Class<?> type) {
        if (!type.isAssignableFrom(instance.getClass())) {
            throw new ModelException("Object is not instance of type " + type.getName());
        }
    }

    public static boolean valueAsBoolean(String rawValue) {
        return Boolean.parseBoolean(rawValue);
    }

    public static String valueAsString(boolean booleanValue) {
        return Boolean.toString(booleanValue);
    }

    /**
     * Get a collection of all model element instances in a view
     *
     * @param view the collection of DOM elements to find the model element instances for
     * @param model the model of the elements
     * @return the collection of model element instances of the view
     */
    @SuppressWarnings("unchecked")
    public static <T extends ModelElementInstance> Collection<T> getModelElementCollection(Collection<DomElement> view, ModelInstanceImpl model) {
        List<ModelElementInstance> resultList = new ArrayList<>();
        for (DomElement element : view) {
            resultList.add(getModelElement(element, model));
        }
        return (Collection<T>) resultList;
    }

    /**
     * Find the index of the type of a model element in a list of element types
     *
     * @param modelElement the model element which type is searched for
     * @param childElementTypes the list to search the type
     * @return the index of the model element type in the list or -1 if it is not found
     */
    public static int getIndexOfElementType(ModelElementInstance modelElement, List<ModelElementType> childElementTypes) {
        for (int index = 0; index < childElementTypes.size(); index++) {
            ModelElementType childElementType = childElementTypes.get(index);
            Class<? extends ModelElementInstance> instanceType = childElementType.getInstanceType();
            if (instanceType.isAssignableFrom(modelElement.getClass())) {
                return index;
            }
        }
        Collection<String> childElementTypeNames = new ArrayList<>();
        for (ModelElementType childElementType : childElementTypes) {
            childElementTypeNames.add(childElementType.getTypeName());
        }
        throw new ModelException(
                "New child is not a valid child element type: " + modelElement.getElementType().getTypeName() + "; valid types are: " + childElementTypeNames);
    }

    /**
     * Calculate a collection of all extending types for the given base types
     *
     * @param baseTypes the collection of types to calculate the union of all extending types
     */
    public static Collection<ModelElementType> calculateAllExtendingTypes(Model model, Collection<ModelElementType> baseTypes) {
        Set<ModelElementType> allExtendingTypes = new HashSet<>();
        for (ModelElementType baseType : baseTypes) {
            ModelElementTypeImpl modelElementTypeImpl = (ModelElementTypeImpl) model.getType(baseType.getInstanceType());
            modelElementTypeImpl.resolveExtendingTypes(allExtendingTypes);
        }
        return allExtendingTypes;
    }

    /**
     * Calculate a collection of all base types for the given type
     */
    public static Collection<ModelElementType> calculateAllBaseTypes(ModelElementType type) {
        List<ModelElementType> baseTypes = new ArrayList<>();
        ModelElementTypeImpl typeImpl = (ModelElementTypeImpl) type;
        typeImpl.resolveBaseTypes(baseTypes);
        return baseTypes;
    }

    /**
     * Set unique identifier if the type has a String id attribute
     *
     * @param type the type of the model element
     * @param modelElementInstance the model element instance to set the id
     */
    public static void setGeneratedUniqueIdentifier(ModelElementType type, ModelElementInstance modelElementInstance) {
        Attribute<?> id = type.getAttribute("id");
        if (id instanceof StringAttribute && id.isIdAttribute()) {
            ((StringAttribute) id).setValue(modelElementInstance, getUniqueIdentifier(type));
        }
    }

    public static String getUniqueIdentifier(ModelElementType type) {
        return type.getTypeName() + '_' + UUID.randomUUID();
    }
}
