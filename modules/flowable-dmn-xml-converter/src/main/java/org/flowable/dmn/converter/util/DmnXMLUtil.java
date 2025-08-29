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
package org.flowable.dmn.converter.util;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import org.apache.commons.lang3.StringUtils;
import org.flowable.dmn.converter.child.AllowedValuesParser;
import org.flowable.dmn.converter.child.BaseChildElementParser;
import org.flowable.dmn.converter.child.InputClauseParser;
import org.flowable.dmn.converter.child.InputEntryParser;
import org.flowable.dmn.converter.child.InputExpressionParser;
import org.flowable.dmn.converter.child.InputValuesParser;
import org.flowable.dmn.converter.child.ItemComponentParser;
import org.flowable.dmn.converter.child.OutputClauseParser;
import org.flowable.dmn.converter.child.OutputEntryParser;
import org.flowable.dmn.converter.child.OutputValuesParser;
import org.flowable.dmn.converter.child.RequiredAuthorityParser;
import org.flowable.dmn.converter.child.RequiredDecisionParser;
import org.flowable.dmn.converter.child.RequiredInputParser;
import org.flowable.dmn.converter.child.TypeRefParser;
import org.flowable.dmn.converter.child.VariableParser;
import org.flowable.dmn.model.Decision;
import org.flowable.dmn.model.DmnElement;
import org.flowable.dmn.model.DmnExtensionAttribute;
import org.flowable.dmn.model.DmnExtensionElement;
import org.flowable.dmn.xml.constants.DmnXMLConstants;

/**
 * @author Tijs Rademakers
 * @author Yvo Swillens
 * @author Bassam Al-Sarori
 */
public class DmnXMLUtil implements DmnXMLConstants {

    private static Map<String, BaseChildElementParser> genericChildParserMap = new HashMap<>();

    static {
        addGenericParser(new InputClauseParser());
        addGenericParser(new OutputClauseParser());
        addGenericParser(new InputEntryParser());
        addGenericParser(new OutputEntryParser());
        addGenericParser(new InputExpressionParser());
        addGenericParser(new InputValuesParser());
        addGenericParser(new OutputValuesParser());
        addGenericParser(new VariableParser());
        addGenericParser(new RequiredAuthorityParser());
        addGenericParser(new RequiredDecisionParser());
        addGenericParser(new RequiredInputParser());
        addGenericParser(new AllowedValuesParser());
        addGenericParser(new ItemComponentParser());
        addGenericParser(new TypeRefParser());
    }

    private static void addGenericParser(BaseChildElementParser parser) {
        genericChildParserMap.put(parser.getElementName(), parser);
    }

    public static void parseChildElements(String elementName, DmnElement parentElement, XMLStreamReader xtr,
            Map<String, BaseChildElementParser> childParsers, Decision decision) throws Exception {

        Map<String, BaseChildElementParser> localParserMap = new HashMap<>(genericChildParserMap);
        if (childParsers != null) {
            localParserMap.putAll(childParsers);
        }

        boolean inExtensionElements = false;
        boolean readyWithChildElements = false;
        while (!readyWithChildElements && xtr.hasNext()) {
            xtr.next();

            if (xtr.isStartElement()) {
                if (ELEMENT_EXTENSIONS.equals(xtr.getLocalName())) {
                    inExtensionElements = true;
                } else if (localParserMap.containsKey(xtr.getLocalName())) {
                    BaseChildElementParser childParser = localParserMap.get(xtr.getLocalName());
                    // if we're into an extension element but the current element is not accepted by this parentElement then is read as a custom extension element
                    if (inExtensionElements && !childParser.accepts(parentElement)) {
                        DmnExtensionElement extensionElement = parseExtensionElement(xtr);
                        parentElement.addExtensionElement(extensionElement);
                        continue;
                    }
                    localParserMap.get(xtr.getLocalName()).parseChildElement(xtr, parentElement, decision);
                } else if (inExtensionElements) {
                    DmnExtensionElement extensionElement = parseExtensionElement(xtr);
                    parentElement.addExtensionElement(extensionElement);
                }

            } else if (xtr.isEndElement()) {
                if (ELEMENT_EXTENSIONS.equals(xtr.getLocalName())) {
                    inExtensionElements = false;
                } else if (elementName.equalsIgnoreCase(xtr.getLocalName())) {
                    readyWithChildElements = true;
                }
            }
        }
    }

    public static void writeDefaultAttribute(String attributeName, String value, XMLStreamWriter xtw) throws Exception {
        if (StringUtils.isNotEmpty(value) && !"null".equalsIgnoreCase(value)) {
            xtw.writeAttribute(attributeName, value);
        }
    }

    public static void writeQualifiedAttribute(String attributeName, String value, XMLStreamWriter xtw) throws Exception {
        if (StringUtils.isNotEmpty(value)) {
            xtw.writeAttribute(FLOWABLE_EXTENSIONS_PREFIX, FLOWABLE_EXTENSIONS_NAMESPACE, attributeName, value);
        }
    }

    public static DmnExtensionElement parseExtensionElement(XMLStreamReader xtr) throws Exception {
        DmnExtensionElement extensionElement = new DmnExtensionElement();
        extensionElement.setName(xtr.getLocalName());
        if (StringUtils.isNotEmpty(xtr.getNamespaceURI())) {
            extensionElement.setNamespace(xtr.getNamespaceURI());
        }
        if (StringUtils.isNotEmpty(xtr.getPrefix())) {
            extensionElement.setNamespacePrefix(xtr.getPrefix());
        }

        parseAttributes(extensionElement, xtr);

        boolean readyWithExtensionElement = false;
        while (!readyWithExtensionElement && xtr.hasNext()) {
            xtr.next();
            if (xtr.isCharacters() || XMLStreamReader.CDATA == xtr.getEventType()) {
                if (StringUtils.isNotEmpty(xtr.getText().trim())) {
                    if (extensionElement.getElementText() != null) {
                        extensionElement.setElementText(extensionElement.getElementText() + xtr.getText().trim());
                        
                    } else {
                        extensionElement.setElementText(xtr.getText().trim());
                    }
                }
                
            } else if (xtr.isStartElement()) {
                DmnExtensionElement childExtensionElement = parseExtensionElement(xtr);
                extensionElement.addChildElement(childExtensionElement);
                
            } else if (xtr.isEndElement() && extensionElement.getName().equalsIgnoreCase(xtr.getLocalName())) {
                readyWithExtensionElement = true;
            }
        }
        return extensionElement;
    }

    public static void parseAttributes(DmnElement dmnElement, XMLStreamReader xtr) {
        parseAttributes(dmnElement, xtr, Collections.emptyList());
    }

    public static void parseAttributes(DmnElement dmnElement, XMLStreamReader xtr, Collection<DmnExtensionAttribute> attributesToIgnore) {
        for (int i = 0; i < xtr.getAttributeCount(); i++) {
            DmnExtensionAttribute extensionAttribute = new DmnExtensionAttribute();
            extensionAttribute.setName(xtr.getAttributeLocalName(i));
            extensionAttribute.setValue(xtr.getAttributeValue(i));
            if (StringUtils.isNotEmpty(xtr.getAttributeNamespace(i))) {
                extensionAttribute.setNamespace(xtr.getAttributeNamespace(i));
            }
            if (StringUtils.isNotEmpty(xtr.getAttributePrefix(i))) {
                extensionAttribute.setNamespacePrefix(xtr.getAttributePrefix(i));
            }

            if (isAttributedIncluded(extensionAttribute, attributesToIgnore)) {
                dmnElement.addAttribute(extensionAttribute);
            }
        }
    }

    protected static boolean isAttributedIncluded(DmnExtensionAttribute attribute, Collection<DmnExtensionAttribute> attributesToIgnore) {
        if (attributesToIgnore.isEmpty()) {
            return true;
        }

        for (DmnExtensionAttribute attributeToIgnore : attributesToIgnore) {
            if (Objects.equals(attributeToIgnore.getName(), attribute.getName())) {
                if (Objects.equals(attributeToIgnore.getNamespace(), attribute.getNamespace())) {
                    return false;
                }
            }
        }

        return true;

    }

    public static void writeElementDescription(DmnElement dmnElement, XMLStreamWriter xtw) throws Exception {
        if (StringUtils.isNotEmpty(dmnElement.getDescription()) && !"null".equalsIgnoreCase(dmnElement.getDescription())) {
            xtw.writeStartElement(ELEMENT_DESCRIPTION);
            xtw.writeCharacters(dmnElement.getDescription());
            xtw.writeEndElement();
        }
    }

    public static void writeExtensionElements(DmnElement dmnElement, XMLStreamWriter xtw) throws Exception {
        writeExtensionElements(dmnElement, null, xtw);
    }

    public static void writeExtensionElements(DmnElement dmnElement, Map<String, String> namespaceMap, XMLStreamWriter xtw) throws Exception {
        if (writeExtensionElements(dmnElement, false, xtw)) {
            xtw.writeEndElement();
        }
    }

    public static boolean writeExtensionElements(DmnElement dmnElement, boolean didWriteExtensionStartElement, XMLStreamWriter xtw) throws Exception {
        return writeExtensionElements(dmnElement, didWriteExtensionStartElement, null, xtw);
    }

    public static boolean writeExtensionElements(DmnElement dmnElement, boolean didWriteExtensionStartElement, Map<String, String> namespaceMap, XMLStreamWriter xtw) throws Exception {
        if (!dmnElement.getExtensionElements().isEmpty()) {
            if (!didWriteExtensionStartElement) {
                xtw.writeStartElement(ELEMENT_EXTENSIONS);
                didWriteExtensionStartElement = true;
            }

            if (namespaceMap == null) {
                namespaceMap = new HashMap<>();
            }

            for (List<DmnExtensionElement> extensionElements : dmnElement.getExtensionElements().values()) {
                for (DmnExtensionElement extensionElement : extensionElements) {
                    writeExtensionElement(extensionElement, namespaceMap, xtw);
                }
            }
        }
        return didWriteExtensionStartElement;
    }

    protected static void writeExtensionElement(DmnExtensionElement extensionElement, Map<String, String> namespaceMap, XMLStreamWriter xtw) throws Exception {
        if (StringUtils.isNotEmpty(extensionElement.getName())) {
            Map<String, String> localNamespaceMap = new HashMap<>();
            if (StringUtils.isNotEmpty(extensionElement.getNamespace())) {
                if (StringUtils.isNotEmpty(extensionElement.getNamespacePrefix())) {
                    xtw.writeStartElement(extensionElement.getNamespacePrefix(), extensionElement.getName(), extensionElement.getNamespace());

                    if (!namespaceMap.containsKey(extensionElement.getNamespacePrefix()) ||
                            !namespaceMap.get(extensionElement.getNamespacePrefix()).equals(extensionElement.getNamespace())) {

                        xtw.writeNamespace(extensionElement.getNamespacePrefix(), extensionElement.getNamespace());
                        namespaceMap.put(extensionElement.getNamespacePrefix(), extensionElement.getNamespace());
                        localNamespaceMap.put(extensionElement.getNamespacePrefix(), extensionElement.getNamespace());
                    }
                } else {
                    xtw.writeStartElement(extensionElement.getNamespace(), extensionElement.getName());
                }
            } else {
                xtw.writeStartElement(extensionElement.getName());
            }

            writeAttributes(extensionElement, namespaceMap, xtw);

            if (extensionElement.getElementText() != null) {
                xtw.writeCharacters(extensionElement.getElementText());
            } else {
                for (List<DmnExtensionElement> childElements : extensionElement.getChildElements().values()) {
                    for (DmnExtensionElement childElement : childElements) {
                        writeExtensionElement(childElement, namespaceMap, xtw);
                    }
                }
            }

            for (String prefix : localNamespaceMap.keySet()) {
                namespaceMap.remove(prefix);
            }

            xtw.writeEndElement();
        }
    }

    public static void writeAttributes(DmnElement dmnElement, Map<String, String> namespaceMap, XMLStreamWriter xtw) throws Exception {
        if (!dmnElement.getAttributes().isEmpty()) {
            if (namespaceMap == null) {
                namespaceMap = new HashMap<>();
            }
            for (List<DmnExtensionAttribute> attributes : dmnElement.getAttributes().values()) {
                for (DmnExtensionAttribute attribute : attributes) {
                    if (StringUtils.isNotEmpty(attribute.getName()) && attribute.getValue() != null) {
                        if (StringUtils.isNotEmpty(attribute.getNamespace())) {
                            if (StringUtils.isNotEmpty(attribute.getNamespacePrefix())) {

                                if (!namespaceMap.containsKey(attribute.getNamespacePrefix()) ||
                                        !namespaceMap.get(attribute.getNamespacePrefix()).equals(attribute.getNamespace())) {

                                    xtw.writeNamespace(attribute.getNamespacePrefix(), attribute.getNamespace());
                                    namespaceMap.put(attribute.getNamespacePrefix(), attribute.getNamespace());
                                }

                                xtw.writeAttribute(attribute.getNamespacePrefix(), attribute.getNamespace(), attribute.getName(), attribute.getValue());
                            } else {
                                xtw.writeAttribute(attribute.getNamespace(), attribute.getName(), attribute.getValue());
                            }
                        } else {
                            xtw.writeAttribute(attribute.getName(), attribute.getValue());
                        }
                    }
                }
            }
        }
    }

    public static String getUniqueElementId() {
        return getUniqueElementId(null);
    }

    public static String getUniqueElementId(String prefix) {
        UUID uuid = UUID.randomUUID();
        if (StringUtils.isEmpty(prefix)) {
            return uuid.toString();
        } else {
            return String.format("%s_%s", prefix, uuid);
        }
    }
}
