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
import org.flowable.bpm.model.xml.impl.ModelInstanceImpl;
import org.w3c.dom.Element;

import java.util.List;
import java.util.Set;

/**
 * Encapsulates {@link Element}. Implementations of this interface must be thread-safe.
 */
public interface DomElement {

    /**
     * Returns the namespace URI for this element.
     *
     * @return the namespace URI
     */
    String getNamespaceURI();

    /**
     * Returns the local name of this element.
     *
     * @return the local name
     */
    String getLocalName();

    /**
     * Returns the prefix of this element.
     *
     * @return the prefix
     */
    String getPrefix();

    /**
     * Returns the DOM document which contains this element.
     *
     * @return the DOM document or null if the element itself is a document
     */
    DomDocument getDocument();

    /**
     * Returns the root element of the document which contains this element.
     *
     * @return the root element of the document or null if non exists
     */
    DomElement getRootElement();

    /**
     * Returns the parent element of this element.
     *
     * @return the parent element or null if not part of a tree
     */
    DomElement getParentElement();

    /**
     * Returns all child elements of this element.
     *
     * @return the list of child elements
     */
    List<DomElement> getChildElements();

    /**
     * Returns all child elements of this element with a specific namespace + name
     *
     * @return the list of child elements
     */
    List<DomElement> getChildElementsByNameNs(String namespaceUris, String elementName);

    /**
     * Returns all child elements of this element with specific namespaces + name.
     *
     * @return the list of child elements
     */
    List<DomElement> getChildElementsByNameNs(Set<String> namespaceUris, String elementName);

    /**
     * Returns all child elements of this element with a specific type.
     *
     * @return the list of child elements matching the type
     */
    List<DomElement> getChildElementsByType(ModelInstanceImpl modelInstance, Class<? extends ModelElementInstance> elementType);

    /**
     * Replaces a child element with a new element.
     *
     * @param newChildDomElement the new child element
     * @param existingChildDomElement the existing child element
     * @throws ModelException if the child cannot be replaced
     */
    void replaceChild(DomElement newChildDomElement, DomElement existingChildDomElement);

    /**
     * Removes a child element of this element.
     *
     * @param domElement the child element to remove
     * @return true if the child element was removed otherwise false
     */
    boolean removeChild(DomElement domElement);

    /**
     * Appends the element to the child elements of this element.
     *
     * @param childElement the element to append
     */
    void appendChild(DomElement childElement);

    /**
     * Inserts the new child element after another child element. If the child element to insert after is null the new child element will be inserted
     * at the beginning.
     *
     * @param elementToInsert the new element to insert
     * @param insertAfter the existing child element to insert after or null
     */
    void insertChildElementAfter(DomElement elementToInsert, DomElement insertAfter);

    /**
     * Checks if this element has a attribute under the namespace of this element.
     *
     * @param localName the name of the attribute
     * @return true if the attribute exists otherwise false
     */
    boolean hasAttribute(String localName);

    /**
     * Checks if this element has a attribute with the given namespace.
     *
     * @param namespaceUri the namespaceUri of the namespace
     * @param localName the name of the attribute
     * @return true if the attribute exists otherwise false
     */
    boolean hasAttribute(String namespaceUri, String localName);

    /**
     * Returns the attribute value for the namespace of this element.
     *
     * @param attributeName the name of the attribute
     * @return the value of the attribute or the empty string
     */
    String getAttribute(String attributeName);

    /**
     * Returns the attribute value for the given namespace.
     *
     * @param namespaceUri the namespaceUri of the namespace
     * @param localName the name of the attribute
     * @return the value of the attribute or the empty string
     */
    String getAttribute(String namespaceUri, String localName);

    /**
     * Sets the attribute value for the namespace of this element.
     *
     * @param localName the name of the attribute
     * @param value the value to set
     */
    void setAttribute(String localName, String value);

    /**
     * Sets the attribute value for the given namespace.
     *
     * @param namespaceUri the namespaceUri of the namespace
     * @param localName the name of the attribute
     * @param value the value to set
     */
    void setAttribute(String namespaceUri, String localName, String value);

    /**
     * Sets the value of a id attribute for the namespace of this element.
     *
     * @param localName the name of the attribute
     * @param value the value to set
     */
    void setIdAttribute(String localName, String value);

    /**
     * Sets the value of a id attribute for the given namespace.
     *
     * @param namespaceUri the namespaceUri of the namespace
     * @param localName the name of the attribute
     * @param value the value to set
     */
    void setIdAttribute(String namespaceUri, String localName, String value);

    /**
     * Removes the attribute for the namespace of this element.
     *
     * @param localName the name of the attribute
     */
    void removeAttribute(String localName);

    /**
     * Removes the attribute for the given namespace.
     *
     * @param namespaceUri the namespaceUri of the namespace
     * @param localName the name of the attribute
     */
    void removeAttribute(String namespaceUri, String localName);

    /**
     * Gets the text content of this element all its descendants.
     *
     * @return the text content
     */
    String getTextContent();

    /**
     * Sets the text content of this element.
     *
     * @param textContent the text content to set
     */
    void setTextContent(String textContent);

    /**
     * Adds a CDATA section to this element.
     *
     * @param data the CDATA content to set
     */
    void addCDataSection(String data);

    /**
     * Returns the {@link ModelElementInstance} which is associated with this element.
     *
     * @return the {@link ModelElementInstance} or null if non is associated
     */
    ModelElementInstance getModelElementInstance();

    /**
     * Sets the {@link ModelElementInstance} which should be associated with this element.
     *
     * @param modelElementInstance the {@link ModelElementInstance} to associate
     */
    void setModelElementInstance(ModelElementInstance modelElementInstance);

    /**
     * Adds a new namespace with a generated prefix to this element.
     *
     * @param namespaceUri the namespaceUri of the namespace
     * @return the generated prefix for the new namespace
     */
    String registerNamespace(String namespaceUri);

    /**
     * Adds a new namespace with prefix to this element.
     *
     * @param prefix the prefix of the namespace
     * @param namespaceUri the namespaceUri of the namespace
     */
    void registerNamespace(String prefix, String namespaceUri);

    /**
     * Returns the prefix of the namespace starting from this node upwards. The default namespace has the prefix {@code null}.
     *
     * @param namespaceUri the namespaceUri of the namespace
     * @return the prefix or null if non is defined
     */
    String lookupPrefix(String namespaceUri);
}
