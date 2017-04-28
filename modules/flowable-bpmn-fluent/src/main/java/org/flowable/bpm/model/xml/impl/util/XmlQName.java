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

import static javax.xml.XMLConstants.XMLNS_ATTRIBUTE_NS_URI;

import org.flowable.bpm.model.xml.instance.DomDocument;
import org.flowable.bpm.model.xml.instance.DomElement;

import java.util.HashMap;
import java.util.Map;

public class XmlQName {

    public static final Map<String, String> KNOWN_PREFIXES;
    static {
        KNOWN_PREFIXES = new HashMap<>();
        KNOWN_PREFIXES.put("http://activiti.org/bpmn", "activiti");
        KNOWN_PREFIXES.put("http://flowable.org/bpmn", "flowable");
        KNOWN_PREFIXES.put("http://www.omg.org/spec/BPMN/20100524/MODEL", "bpmn2");
        KNOWN_PREFIXES.put("http://www.omg.org/spec/BPMN/20100524/DI", "bpmndi");
        KNOWN_PREFIXES.put("http://www.omg.org/spec/DD/20100524/DI", "di");
        KNOWN_PREFIXES.put("http://www.omg.org/spec/DD/20100524/DC", "dc");
        KNOWN_PREFIXES.put(XMLNS_ATTRIBUTE_NS_URI, "");
    }

    protected DomElement rootElement;
    protected DomElement element;

    protected String localName;
    protected String namespaceUri;
    protected String prefix;

    public XmlQName(DomDocument document, String namespaceUri, String localName) {
        this(document, null, namespaceUri, localName);
    }

    public XmlQName(DomElement element, String namespaceUri, String localName) {
        this(element.getDocument(), element, namespaceUri, localName);
    }

    public XmlQName(DomDocument document, DomElement element, String namespaceUri, String localName) {
        this.rootElement = document.getRootElement();
        this.element = element;
        this.localName = localName;
        this.namespaceUri = namespaceUri;
        this.prefix = null;
    }

    public String getNamespaceUri() {
        return namespaceUri;
    }

    public String getLocalName() {
        return localName;
    }

    public String getPrefixedName() {
        if (prefix == null) {
            synchronized (this) {
                if (prefix == null) {
                    this.prefix = determinePrefixAndNamespaceUri();
                }
            }
        }
        return QName.combine(prefix, localName);
    }

    public boolean hasLocalNamespace() {
        return element != null && element.getNamespaceURI().equals(namespaceUri);
    }

    private String determinePrefixAndNamespaceUri() {
        if (namespaceUri != null) {
            if (rootElement != null && namespaceUri.equals(rootElement.getNamespaceURI())) {
                // global namespaces do not have a prefix or namespace URI
                return null;
            } else {
                // lookup for prefix
                String lookupPrefix = lookupPrefix();
                if (lookupPrefix == null && rootElement != null) {
                    // if no prefix is found we generate a new one
                    // search for known prefixes
                    String knownPrefix = KNOWN_PREFIXES.get(namespaceUri);
                    if (knownPrefix == null) {
                        // generate namespace
                        return rootElement.registerNamespace(namespaceUri);
                    } else if (knownPrefix.isEmpty()) {
                        // ignored namespace
                        return null;
                    } else {
                        // register known prefix
                        rootElement.registerNamespace(knownPrefix, namespaceUri);
                        return knownPrefix;
                    }
                } else {
                    return lookupPrefix;
                }
            }
        } else {
            // no namespace so no prefix
            return null;
        }
    }

    private String lookupPrefix() {
        if (namespaceUri != null) {
            String lookupPrefix = null;
            if (element != null) {
                lookupPrefix = element.lookupPrefix(namespaceUri);
            } else if (rootElement != null) {
                lookupPrefix = rootElement.lookupPrefix(namespaceUri);
            }
            return lookupPrefix;
        }
        return null;
    }
}
