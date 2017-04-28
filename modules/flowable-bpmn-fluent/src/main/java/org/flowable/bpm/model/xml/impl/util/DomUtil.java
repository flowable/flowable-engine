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

import org.flowable.bpm.model.xml.ModelParseException;
import org.flowable.bpm.model.xml.impl.ModelInstanceImpl;
import org.flowable.bpm.model.xml.impl.instance.DomDocumentImpl;
import org.flowable.bpm.model.xml.impl.instance.DomElementImpl;
import org.flowable.bpm.model.xml.instance.DomDocument;
import org.flowable.bpm.model.xml.instance.DomElement;
import org.flowable.bpm.model.xml.instance.ModelElementInstance;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Helper methods which abstract some gruesome DOM specifics. It does not provide synchronization when invoked in parallel with the same objects.
 */
public final class DomUtil {

    private DomUtil() {}

    /**
     * A {@link NodeListFilter} allows to filter a {@link NodeList}, retaining only elements in the list which match the filter.
     *
     * @see DomUtil#filterNodeList(NodeList, NodeListFilter)
     */
    public interface NodeListFilter {

        /**
         * Test if node matches the filter
         *
         * @param node the node to match
         * @return true if the filter does match the node, false otherwise
         */
        boolean matches(Node node);

    }

    /**
     * Filter retaining only Nodes of type {@link Node#ELEMENT_NODE}
     *
     */
    public static class ElementNodeListFilter
            implements NodeListFilter {

        @Override
        public boolean matches(Node node) {
            return node.getNodeType() == Node.ELEMENT_NODE;
        }

    }

    /**
     * Filters {@link Element Elements} by their nodeName + namespaceUri
     *
     */
    public static class ElementByNameListFilter
            extends ElementNodeListFilter {

        private final String localName;
        private final String namespaceUri;

        /**
         * @param localName the local name to filter for
         * @param namespaceUri the namespaceUri to filter for
         */
        public ElementByNameListFilter(String localName, String namespaceUri) {
            this.localName = localName;
            this.namespaceUri = namespaceUri;
        }

        @Override
        public boolean matches(Node node) {
            return super.matches(node)
                    && localName.equals(node.getLocalName())
                    && namespaceUri.equals(node.getNamespaceURI());
        }

    }

    public static class ElementByTypeListFilter
            extends ElementNodeListFilter {

        private final Class<?> type;
        private final ModelInstanceImpl model;

        public ElementByTypeListFilter(Class<?> type, ModelInstanceImpl modelInstance) {
            this.type = type;
            this.model = modelInstance;
        }

        @Override
        public boolean matches(Node node) {
            if (!super.matches(node)) {
                return false;
            }
            ModelElementInstance modelElement = ModelUtil.getModelElement(new DomElementImpl((Element) node), model);
            return type.isAssignableFrom(modelElement.getClass());
        }
    }

    /**
     * Allows to apply a {@link NodeListFilter} to a {@link NodeList}. This allows to remove all elements from a node list which do not match the
     * Filter.
     *
     * @param nodeList the {@link NodeList} to filter
     * @param filter the {@link NodeListFilter} to apply to the {@link NodeList}
     * @return the List of all Nodes which match the filter
     */
    public static List<DomElement> filterNodeList(NodeList nodeList, NodeListFilter filter) {

        List<DomElement> filteredList = new ArrayList<>();
        for (int idx = 0; idx < nodeList.getLength(); idx++) {
            Node node = nodeList.item(idx);
            if (filter.matches(node)) {
                filteredList.add(new DomElementImpl((Element) node));
            }
        }

        return filteredList;

    }

    /**
     * Filters a {@link NodeList} retaining all elements
     *
     * @param nodeList the the {@link NodeList} to filter
     * @return the list of all elements
     */
    public static List<DomElement> filterNodeListForElements(NodeList nodeList) {
        return filterNodeList(nodeList, new ElementNodeListFilter());
    }

    /**
     * Filter a {@link NodeList} retaining all elements with a specific name
     *
     *
     * @param nodeList the {@link NodeList} to filter
     * @param namespaceUri the namespace for the elements
     * @param localName the local element name to filter for
     * @return the List of all Elements which match the filter
     */
    public static List<DomElement> filterNodeListByName(NodeList nodeList, String namespaceUri, String localName) {
        return filterNodeList(nodeList, new ElementByNameListFilter(localName, namespaceUri));
    }

    /**
     * Filter a {@link NodeList} retaining all elements with a specific type
     *
     *
     * @param nodeList the {@link NodeList} to filter
     * @param modelInstance the model instance
     * @param type the type class to filter for
     * @return the list of all Elements which match the filter
     */
    public static List<DomElement> filterNodeListByType(NodeList nodeList, ModelInstanceImpl modelInstance, Class<?> type) {
        return filterNodeList(nodeList, new ElementByTypeListFilter(type, modelInstance));
    }

    public static class DomErrorHandler
            implements ErrorHandler {

        private static final Logger LOGGER = Logger.getLogger(DomErrorHandler.class.getName());

        private String getParseExceptionInfo(SAXParseException spe) {
            return "URI=" + spe.getSystemId() + " Line="
                    + spe.getLineNumber() + ": " + spe.getMessage();
        }

        @Override
        public void warning(SAXParseException spe) {
            LOGGER.warning(getParseExceptionInfo(spe));
        }

        @Override
        public void error(SAXParseException spe)
            throws SAXException {
            String message = "Error: " + getParseExceptionInfo(spe);
            throw new SAXException(message);
        }

        @Override
        public void fatalError(SAXParseException spe)
            throws SAXException {
            String message = "Fatal Error: " + getParseExceptionInfo(spe);
            throw new SAXException(message);
        }
    }

    /**
     * Get an empty DOM document
     *
     * @param documentBuilderFactory the factory to build to DOM document
     * @return the new empty document
     * @throws ModelParseException if unable to create a new document
     */
    public static DomDocument getEmptyDocument(DocumentBuilderFactory documentBuilderFactory) {
        try {
            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
            return new DomDocumentImpl(documentBuilder.newDocument());
        }
        catch (ParserConfigurationException e) {
            throw new ModelParseException("Unable to create a new document", e);
        }
    }

    /**
     * Create a new DOM document from the input stream.
     *
     * @param documentBuilderFactory the factory to build to DOM document
     * @param inputStream the input stream to parse
     * @return the new DOM document
     * @throws ModelParseException if a parsing or IO error is triggered
     */
    public static DomDocument parseInputStream(DocumentBuilderFactory documentBuilderFactory, InputStream inputStream) {

        try {
            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
            documentBuilder.setErrorHandler(new DomErrorHandler());
            return new DomDocumentImpl(documentBuilder.parse(inputStream));
        }
        catch (ParserConfigurationException e) {
            throw new ModelParseException("ParserConfigurationException while parsing input stream", e);
        }
        catch (SAXException e) {
            throw new ModelParseException("SAXException while parsing input stream", e);
        }
        catch (IOException e) {
            throw new ModelParseException("IOException while parsing input stream", e);
        }
    }
}
