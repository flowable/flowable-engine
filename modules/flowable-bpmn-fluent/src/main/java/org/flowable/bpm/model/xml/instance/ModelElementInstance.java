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
package org.flowable.bpm.model.xml.instance;

import org.flowable.bpm.model.xml.ModelException;
import org.flowable.bpm.model.xml.ModelInstance;
import org.flowable.bpm.model.xml.impl.instance.ModelElementInstanceImpl;
import org.flowable.bpm.model.xml.type.ModelElementType;

import java.util.Collection;

/**
 * An instance of a {@link ModelElementType}.
 */
public interface ModelElementInstance {

    /**
     * Returns the represented DOM {@link DomElement}.
     *
     * @return the DOM element
     */
    DomElement getDomElement();

    /**
     * Returns the model instance which contains this type instance.
     *
     * @return the model instance
     */

    ModelInstance getModelInstance();

    /**
     * Returns the parent element of this.
     *
     * @return the parent element
     */
    ModelElementInstance getParentElement();

    /**
     * Returns the element type of this.
     *
     * @return the element type
     */
    ModelElementType getElementType();

    /**
     * Returns the attribute value for the attribute name.
     *
     * @param attributeName the name of the attribute
     * @return the value of the attribute
     */
    String getAttributeValue(String attributeName);

    /**
     * Sets the value by name of a non-ID attribute.
     *
     * @param attributeName the name of the attribute
     * @param xmlValue the value to set
     */
    void setAttributeValue(String attributeName, String xmlValue);

    /**
     * Sets attribute value by name.
     *
     * @param attributeName the name of the attribute
     * @param xmlValue the value to set
     * @param isIdAttribute true if the attribute is an ID attribute, false otherwise
     */
    void setAttributeValue(String attributeName, String xmlValue, boolean isIdAttribute);

    /**
     * Removes attribute by name.
     *
     * @param attributeName the name of the attribute
     */
    void removeAttribute(String attributeName);

    /**
     * Returns the attribute value for the given attribute name and namespace URI.
     *
     * @param namespaceUri the namespace URI of the attribute
     * @param attributeName the attribute name of the attribute
     * @return the value of the attribute
     */
    String getAttributeValueNs(String namespaceUri, String attributeName);

    /**
     * Sets the value by name and namespace of a non-ID attribute.
     *
     * @param namespaceUri the namespace URI of the attribute
     * @param attributeName the name of the attribute
     * @param xmlValue the XML value to set
     */
    void setAttributeValueNs(String namespaceUri, String attributeName, String xmlValue);

    /**
     * Sets the attribute value by name and namespace.
     *
     * @param namespaceUri the namespace URI of the attribute
     * @param attributeName the name of the attribute
     * @param xmlValue the XML value to set
     * @param isIdAttribute true if the attribute is an ID attribute, false otherwise
     */
    void setAttributeValueNs(String namespaceUri, String attributeName, String xmlValue, boolean isIdAttribute);

    /**
     * Removes the attribute by name and namespace.
     *
     * @param namespaceUri the namespace URI of the attribute
     * @param attributeName the name of the attribute
     */
    void removeAttributeNs(String namespaceUri, String attributeName);

    /**
     * Returns the text content of the DOM element without leading and trailing spaces. For raw text content see
     * {@link ModelElementInstanceImpl#getRawTextContent()}.
     *
     * @return text content of underlying DOM element with leading and trailing whitespace trimmed
     */
    String getTextContent();

    /**
     * Returns the raw text content of the DOM element including all whitespaces.
     *
     * @return raw text content of underlying DOM element
     */
    String getRawTextContent();

    /**
     * Sets the text content of the DOM element
     *
     * @param textContent the new text content
     */
    void setTextContent(String textContent);

    /**
     * Replaces this element with a new element and updates references.
     *
     * @param newElement the new element to replace with
     */
    void replaceWithElement(ModelElementInstance newElement);

    /**
     * Returns a child element with the given name or 'null' if no such element exists
     *
     * @param namespaceUri the local name of the element
     * @param elementName the namespace of the element
     * @return the child element or null.
     */
    ModelElementInstance getUniqueChildElementByNameNs(String namespaceUri, String elementName);

    /**
     * Returns a child element with the given type
     *
     * @param elementType the type of the element
     * @return the child element or null
     */
    ModelElementInstance getUniqueChildElementByType(Class<? extends ModelElementInstance> elementType);

    /**
     * Adds or replaces a child element by name. Replaces an existing Child Element with the same name or adds a new child if no such element exists.
     *
     * @param newChild the child to add
     */
    void setUniqueChildElementByNameNs(ModelElementInstance newChild);

    /**
     * Replace an existing child element with a new child element. Changes the underlying DOM element tree.
     *
     * @param existingChild the child element to replace
     * @param newChild the new child element
     */
    void replaceChildElement(ModelElementInstance existingChild, ModelElementInstance newChild);

    /**
     * Adds a new child element to the children of this element. The child is inserted at the correct position of the allowed child types. Updates the
     * underlying DOM element tree.
     *
     * @param newChild the new child element
     * @throws ModelException if the new child type is not an allowed child type
     */
    void addChildElement(ModelElementInstance newChild);

    /**
     * Removes the child element from this.
     *
     * @param child the child element to remove
     * @return true if the child element could be removed
     */
    boolean removeChildElement(ModelElementInstance child);

    /**
     * Return all child elements of a given type
     *
     * @param childElementType the child element type to search for
     * @return a collection of elements of the given type
     */
    Collection<ModelElementInstance> getChildElementsByType(ModelElementType childElementType);

    /**
     * Return all child elements of a given type
     *
     * @param childElementClass the class of the child element type to search for
     * @return a collection of elements to the given type
     */
    <T extends ModelElementInstance> Collection<T> getChildElementsByType(Class<T> childElementClass);

    /**
     * Inserts the new element after the given element or at the beginning if the given element is null.
     *
     * @param elementToInsert the new element to insert
     * @param insertAfterElement the element to insert after or null to insert at first position
     */
    void insertElementAfter(ModelElementInstance elementToInsert, ModelElementInstance insertAfterElement);

    /**
     * Execute updates after the element was inserted as a replacement of another element.
     */
    void updateAfterReplacement();
}
