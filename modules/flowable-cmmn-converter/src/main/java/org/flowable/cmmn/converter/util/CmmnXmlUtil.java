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
package org.flowable.cmmn.converter.util;

import java.text.StringCharacterIterator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import org.apache.commons.lang3.StringUtils;
import org.flowable.cmmn.converter.CmmnXmlConstants;
import org.flowable.cmmn.model.BaseElement;
import org.flowable.cmmn.model.CaseElement;
import org.flowable.cmmn.model.ExtensionAttribute;
import org.flowable.cmmn.model.ExtensionElement;
import org.flowable.cmmn.model.GraphicInfo;
import org.flowable.cmmn.model.IOParameter;

public class CmmnXmlUtil implements CmmnXmlConstants {

    public static void addXMLLocation(BaseElement element, XMLStreamReader xtr) {
        Location location = xtr.getLocation();
        element.setXmlRowNumber(location.getLineNumber());
        element.setXmlColumnNumber(location.getColumnNumber());
    }

    public static void addXMLLocation(GraphicInfo graphicInfo, XMLStreamReader xtr) {
        Location location = xtr.getLocation();
        graphicInfo.setXmlRowNumber(location.getLineNumber());
        graphicInfo.setXmlColumnNumber(location.getColumnNumber());
    }
    
    public static ExtensionElement parseExtensionElement(XMLStreamReader xtr) throws Exception {
        ExtensionElement extensionElement = new ExtensionElement();
        extensionElement.setName(xtr.getLocalName());
        addXMLLocation(extensionElement, xtr);
        if (StringUtils.isNotEmpty(xtr.getNamespaceURI())) {
            extensionElement.setNamespace(xtr.getNamespaceURI());
        }
        if (StringUtils.isNotEmpty(xtr.getPrefix())) {
            extensionElement.setNamespacePrefix(xtr.getPrefix());
        }

        for (int i = 0; i < xtr.getAttributeCount(); i++) {
            ExtensionAttribute extensionAttribute = new ExtensionAttribute();
            extensionAttribute.setName(xtr.getAttributeLocalName(i));
            extensionAttribute.setValue(xtr.getAttributeValue(i));
            if (StringUtils.isNotEmpty(xtr.getAttributeNamespace(i))) {
                extensionAttribute.setNamespace(xtr.getAttributeNamespace(i));
            }
            if (StringUtils.isNotEmpty(xtr.getAttributePrefix(i))) {
                extensionAttribute.setNamespacePrefix(xtr.getAttributePrefix(i));
            }
            extensionElement.addAttribute(extensionAttribute);
        }

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
                ExtensionElement childExtensionElement = parseExtensionElement(xtr);
                extensionElement.addChildElement(childExtensionElement);
                
            } else if (xtr.isEndElement() && extensionElement.getName().equalsIgnoreCase(xtr.getLocalName())) {
                readyWithExtensionElement = true;
            }
        }
        return extensionElement;
    }
    
    public static String getAttributeValue(String attributeName, XMLStreamReader xtr) {
        String attributeValue = xtr.getAttributeValue(FLOWABLE_EXTENSIONS_NAMESPACE, attributeName);

        return attributeValue;
    }
    
    public static List<String> parseDelimitedList(String s) {
        List<String> result = new ArrayList<>();
        if (StringUtils.isNotEmpty(s)) {

            StringCharacterIterator iterator = new StringCharacterIterator(s);
            char c = iterator.first();

            StringBuilder strb = new StringBuilder();
            boolean insideExpression = false;

            while (c != StringCharacterIterator.DONE) {
                if (c == '{' || c == '$') {
                    insideExpression = true;
                } else if (c == '}') {
                    insideExpression = false;
                } else if (c == ',' && !insideExpression) {
                    result.add(strb.toString().trim());
                    strb.delete(0, strb.length());
                }

                if (c != ',' || insideExpression) {
                    strb.append(c);
                }

                c = iterator.next();
            }

            if (strb.length() > 0) {
                result.add(strb.toString().trim());
            }

        }
        return result;
    }

    public static void writeDefaultAttribute(String attributeName, String value, XMLStreamWriter xtw) throws XMLStreamException {
        if (StringUtils.isNotEmpty(value) && !"null".equalsIgnoreCase(value)) {
            xtw.writeAttribute(attributeName, value);
        }
    }

    public static void writeQualifiedAttribute(String attributeName, String value, XMLStreamWriter xtw) throws Exception {
        if (StringUtils.isNotEmpty(value)) {
            xtw.writeAttribute(FLOWABLE_EXTENSIONS_PREFIX, FLOWABLE_EXTENSIONS_NAMESPACE, attributeName, value);
        }
    }

    public static boolean writeExtensionElements(BaseElement baseElement, boolean didWriteExtensionStartElement, XMLStreamWriter xtw) throws Exception {
        return writeExtensionElements(baseElement, didWriteExtensionStartElement, null, xtw);
    }

    public static boolean writeExtensionElements(BaseElement baseElement, boolean didWriteExtensionStartElement, Map<String, String> namespaceMap, XMLStreamWriter xtw) throws Exception {
        if (!baseElement.getExtensionElements().isEmpty()) {
            if (!didWriteExtensionStartElement) {
                xtw.writeStartElement(ELEMENT_EXTENSION_ELEMENTS);
                didWriteExtensionStartElement = true;
            }

            if (namespaceMap == null) {
                namespaceMap = new HashMap<>();
            }

            for (List<ExtensionElement> extensionElements : baseElement.getExtensionElements().values()) {
                for (ExtensionElement extensionElement : extensionElements) {
                    writeExtensionElement(extensionElement, namespaceMap, xtw);
                }
            }
        }
        return didWriteExtensionStartElement;
    }

    protected static void writeExtensionElement(ExtensionElement extensionElement, Map<String, String> namespaceMap, XMLStreamWriter xtw) throws Exception {
        if (StringUtils.isNotEmpty(extensionElement.getName())) {
            Map<String, String> localNamespaceMap = new HashMap<>();
            if (StringUtils.isNotEmpty(extensionElement.getNamespace())) {
                if (StringUtils.isNotEmpty(extensionElement.getNamespacePrefix())) {
                    xtw.writeStartElement(extensionElement.getNamespacePrefix(), extensionElement.getName(), extensionElement.getNamespace());

                    if (!namespaceMap.containsKey(extensionElement.getNamespacePrefix()) || !namespaceMap.get(extensionElement.getNamespacePrefix()).equals(extensionElement.getNamespace())) {

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

            for (List<ExtensionAttribute> attributes : extensionElement.getAttributes().values()) {
                for (ExtensionAttribute attribute : attributes) {
                    if (StringUtils.isNotEmpty(attribute.getName()) && attribute.getValue() != null) {
                        if (StringUtils.isNotEmpty(attribute.getNamespace())) {
                            if (StringUtils.isNotEmpty(attribute.getNamespacePrefix())) {

                                if (!namespaceMap.containsKey(attribute.getNamespacePrefix()) || !namespaceMap.get(attribute.getNamespacePrefix()).equals(attribute.getNamespace())) {

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

            if (extensionElement.getElementText() != null) {
                xtw.writeCData(extensionElement.getElementText());
            } else {
                for (List<ExtensionElement> childElements : extensionElement.getChildElements().values()) {
                    for (ExtensionElement childElement : childElements) {
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

    public static boolean writeElementNameExtensionElement(CaseElement element, boolean didWriteExtensionStartElement, XMLStreamWriter xtw) throws Exception {
        String name = element.getName();
        if (containsNewLine(name)) {
            if (!didWriteExtensionStartElement) {
                xtw.writeStartElement(ELEMENT_EXTENSION_ELEMENTS);
                didWriteExtensionStartElement = true;
            }

            xtw.writeStartElement(FLOWABLE_EXTENSIONS_PREFIX, ATTRIBUTE_ELEMENT_NAME, FLOWABLE_EXTENSIONS_NAMESPACE);
            xtw.writeCharacters(element.getName());
            xtw.writeEndElement();
        }

        return didWriteExtensionStartElement;
    }

    public static void writeCustomAttributes(Collection<List<ExtensionAttribute>> attributes, XMLStreamWriter xtw, List<ExtensionAttribute>... blackLists) throws XMLStreamException {
        writeCustomAttributes(attributes, xtw, new LinkedHashMap<>(), blackLists);
    }

    /**
     * write attributes to xtw (except blacklisted)
     *
     * @param attributes
     * @param xtw
     * @param namespaceMap
     * @param blackLists
     */
    public static void writeCustomAttributes(Collection<List<ExtensionAttribute>> attributes, XMLStreamWriter xtw, Map<String, String> namespaceMap, List<ExtensionAttribute>... blackLists)
            throws XMLStreamException {

        for (List<ExtensionAttribute> attributeList : attributes) {
            if (attributeList != null && !attributeList.isEmpty()) {
                for (ExtensionAttribute attribute : attributeList) {
                    if (!isBlacklisted(attribute, blackLists)) {
                        if (attribute.getNamespacePrefix() == null) {
                            if (attribute.getNamespace() == null) {
                                xtw.writeAttribute(attribute.getName(), attribute.getValue());
                            } else {
                                xtw.writeAttribute(attribute.getNamespace(), attribute.getName(), attribute.getValue());
                            }
                        } else {
                            if (!namespaceMap.containsKey(attribute.getNamespacePrefix())) {
                                namespaceMap.put(attribute.getNamespacePrefix(), attribute.getNamespace());
                                xtw.writeNamespace(attribute.getNamespacePrefix(), attribute.getNamespace());
                            }
                            xtw.writeAttribute(attribute.getNamespacePrefix(), attribute.getNamespace(), attribute.getName(), attribute.getValue());
                        }
                    }
                }
            }
        }
    }
    
    public static String convertToDelimitedString(List<String> stringList) {
        StringBuilder resultString = new StringBuilder();

        if (stringList != null) {
            for (String result : stringList) {
                if (resultString.length() > 0) {
                    resultString.append(",");
                }
                resultString.append(result);
            }
        }
        return resultString.toString();
    }

    public static boolean isBlacklisted(ExtensionAttribute attribute, List<ExtensionAttribute>... blackLists) {
        if (blackLists != null) {
            for (List<ExtensionAttribute> blackList : blackLists) {
                for (ExtensionAttribute blackAttribute : blackList) {
                    if (blackAttribute.getName().equals(attribute.getName())) {
                        if (attribute.getNamespace() != null && FLOWABLE_EXTENSIONS_NAMESPACE.equals(attribute.getNamespace())) {

                            return true;
                        }

                        if (blackAttribute.getNamespace() == null && attribute.getNamespace() == null) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    public static boolean containsNewLine(String str) {
        return str != null && str.contains("\n");
    }

    public static boolean writeIOParameters(String elementName, List<IOParameter> parameterList, boolean didWriteParameterStartElement, XMLStreamWriter xtw) throws Exception {

        if (parameterList == null || parameterList.isEmpty()) {
            return didWriteParameterStartElement;
        }

        for (IOParameter ioParameter : parameterList) {
            if (!didWriteParameterStartElement) {
                xtw.writeStartElement(ELEMENT_EXTENSION_ELEMENTS);
                didWriteParameterStartElement = true;
            }

            xtw.writeStartElement(FLOWABLE_EXTENSIONS_PREFIX, elementName, FLOWABLE_EXTENSIONS_NAMESPACE);
            if (StringUtils.isNotEmpty(ioParameter.getSource())) {
                xtw.writeAttribute(ATTRIBUTE_IOPARAMETER_SOURCE, ioParameter.getSource());
            }
            if (StringUtils.isNotEmpty(ioParameter.getSourceExpression())) {
                xtw.writeAttribute(ATTRIBUTE_IOPARAMETER_SOURCE_EXPRESSION, ioParameter.getSourceExpression());
            }
            if (StringUtils.isNotEmpty(ioParameter.getTarget())) {
                xtw.writeAttribute(ATTRIBUTE_IOPARAMETER_TARGET, ioParameter.getTarget());
            }
            if (StringUtils.isNotEmpty(ioParameter.getTargetExpression())) {
                xtw.writeAttribute(ATTRIBUTE_IOPARAMETER_TARGET_EXPRESSION, ioParameter.getTargetExpression());
            }

            xtw.writeEndElement();
        }

        return didWriteParameterStartElement;
    }
}
