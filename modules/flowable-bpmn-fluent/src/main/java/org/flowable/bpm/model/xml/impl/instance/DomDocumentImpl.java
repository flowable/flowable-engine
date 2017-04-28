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
package org.flowable.bpm.model.xml.impl.instance;

import static javax.xml.XMLConstants.XMLNS_ATTRIBUTE_NS_URI;

import java.util.List;

import javax.xml.transform.dom.DOMSource;

import org.flowable.bpm.model.xml.ModelException;
import org.flowable.bpm.model.xml.impl.util.DomUtil;
import org.flowable.bpm.model.xml.impl.util.XmlQName;
import org.flowable.bpm.model.xml.instance.DomDocument;
import org.flowable.bpm.model.xml.instance.DomElement;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class DomDocumentImpl
        implements DomDocument {

    public static final String GENERIC_NS_PREFIX = "ns";

    private final Document document;

    public DomDocumentImpl(Document document) {
        this.document = document;
    }

    @Override
    public DomElement getRootElement() {
        synchronized (document) {
            Element documentElement = document.getDocumentElement();
            return documentElement != null ? new DomElementImpl(documentElement) : null;
        }

    }

    @Override
    public void setRootElement(DomElement rootElement) {
        synchronized (document) {
            Element documentElement = document.getDocumentElement();
            Element newDocumentElement = ((DomElementImpl) rootElement).getElement();
            if (documentElement != null) {
                document.replaceChild(newDocumentElement, documentElement);
            } else {
                document.appendChild(newDocumentElement);
            }
        }
    }

    @Override
    public DomElement createElement(String namespaceUri, String localName) {
        synchronized (document) {
            XmlQName xmlQName = new XmlQName(this, namespaceUri, localName);
            Element element = document.createElementNS(xmlQName.getNamespaceUri(), xmlQName.getPrefixedName());
            return new DomElementImpl(element);
        }
    }

    @Override
    public DomElement getElementById(String id) {
        synchronized (document) {
            Element element = document.getElementById(id);
            return element != null ? new DomElementImpl(element) : null;
        }
    }

    @Override
    public List<DomElement> getElementsByNameNs(String namespaceUri, String localName) {
        synchronized (document) {
            NodeList elementsByTagNameNS = document.getElementsByTagNameNS(namespaceUri, localName);
            return DomUtil.filterNodeListByName(elementsByTagNameNS, namespaceUri, localName);
        }
    }

    @Override
    public DOMSource getDomSource() {
        return new DOMSource(document);
    }

    @Override
    public String registerNamespace(String namespaceUri) {
        synchronized (document) {
            DomElement rootElement = getRootElement();
            if (rootElement != null) {
                return rootElement.registerNamespace(namespaceUri);
            }
            throw new ModelException("Unable to define a new namespace without a root document element");
        }
    }

    @Override
    public void registerNamespace(String prefix, String namespaceUri) {
        synchronized (document) {
            DomElement rootElement = getRootElement();
            if (rootElement != null) {
                rootElement.registerNamespace(prefix, namespaceUri);
            } else {
                throw new ModelException("Unable to define a new namespace without a root document element");
            }
        }
    }

    protected String getUnusedGenericNsPrefix() {
        synchronized (document) {
            Element documentElement = document.getDocumentElement();
            if (documentElement == null) {
                return GENERIC_NS_PREFIX + '0';
            }
            for (int idx = 0; idx < Integer.MAX_VALUE; idx++) {
                if (!documentElement.hasAttributeNS(XMLNS_ATTRIBUTE_NS_URI, GENERIC_NS_PREFIX + idx)) {
                    return GENERIC_NS_PREFIX + idx;
                }
            }
            throw new ModelException("Unable to find an unused namespace prefix");
        }
    }

    @Override
    public DomDocument clone() {
        synchronized (document) {
            return new DomDocumentImpl((Document) document.cloneNode(true));
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        DomDocumentImpl that = (DomDocumentImpl) o;
        return document.equals(that.document);
    }

    @Override
    public int hashCode() {
        return document.hashCode();
    }
}
