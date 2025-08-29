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
package org.flowable.bpmn.converter.util;

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
import org.flowable.bpmn.constants.BpmnXMLConstants;
import org.flowable.bpmn.converter.child.BaseChildElementParser;
import org.flowable.bpmn.converter.child.CancelEventDefinitionParser;
import org.flowable.bpmn.converter.child.CompensateEventDefinitionParser;
import org.flowable.bpmn.converter.child.ConditionExpressionParser;
import org.flowable.bpmn.converter.child.ConditionParser;
import org.flowable.bpmn.converter.child.ConditionalEventDefinitionParser;
import org.flowable.bpmn.converter.child.DataInputAssociationParser;
import org.flowable.bpmn.converter.child.DataOutputAssociationParser;
import org.flowable.bpmn.converter.child.DataStateParser;
import org.flowable.bpmn.converter.child.DocumentationParser;
import org.flowable.bpmn.converter.child.ElementNameParser;
import org.flowable.bpmn.converter.child.ErrorEventDefinitionParser;
import org.flowable.bpmn.converter.child.EscalationEventDefinitionParser;
import org.flowable.bpmn.converter.child.ExecutionListenerParser;
import org.flowable.bpmn.converter.child.FieldExtensionParser;
import org.flowable.bpmn.converter.child.FlowNodeRefParser;
import org.flowable.bpmn.converter.child.FlowableEventListenerParser;
import org.flowable.bpmn.converter.child.FlowableFailedjobRetryParser;
import org.flowable.bpmn.converter.child.FlowableHttpRequestHandlerParser;
import org.flowable.bpmn.converter.child.FlowableHttpResponseHandlerParser;
import org.flowable.bpmn.converter.child.FlowableMapExceptionParser;
import org.flowable.bpmn.converter.child.FormPropertyParser;
import org.flowable.bpmn.converter.child.IOSpecificationParser;
import org.flowable.bpmn.converter.child.InParameterParser;
import org.flowable.bpmn.converter.child.MessageEventDefinitionParser;
import org.flowable.bpmn.converter.child.MultiInstanceParser;
import org.flowable.bpmn.converter.child.OutParameterParser;
import org.flowable.bpmn.converter.child.ScriptInfoParser;
import org.flowable.bpmn.converter.child.SignalEventDefinitionParser;
import org.flowable.bpmn.converter.child.TaskListenerParser;
import org.flowable.bpmn.converter.child.TerminateEventDefinitionParser;
import org.flowable.bpmn.converter.child.TimeCycleParser;
import org.flowable.bpmn.converter.child.TimeDateParser;
import org.flowable.bpmn.converter.child.TimeDurationParser;
import org.flowable.bpmn.converter.child.TimerEventDefinitionParser;
import org.flowable.bpmn.model.BaseElement;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.ExtensionAttribute;
import org.flowable.bpmn.model.ExtensionElement;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.GraphicInfo;
import org.flowable.bpmn.model.IOParameter;

public class BpmnXMLUtil implements BpmnXMLConstants {

    private static Map<String, BaseChildElementParser> genericChildParserMap = new HashMap<>();

    static {
        addGenericParser(new CancelEventDefinitionParser());
        addGenericParser(new CompensateEventDefinitionParser());
        addGenericParser(new ConditionalEventDefinitionParser());
        addGenericParser(new ConditionParser());
        addGenericParser(new ConditionExpressionParser());
        addGenericParser(new DataInputAssociationParser());
        addGenericParser(new DataOutputAssociationParser());
        addGenericParser(new DataStateParser());
        addGenericParser(new DocumentationParser());
        addGenericParser(new ErrorEventDefinitionParser());
        addGenericParser(new EscalationEventDefinitionParser());
        addGenericParser(new ExecutionListenerParser());
        addGenericParser(new FieldExtensionParser());
        addGenericParser(new ScriptInfoParser());
        addGenericParser(new FlowableEventListenerParser());
        addGenericParser(new FlowableHttpRequestHandlerParser());
        addGenericParser(new FlowableHttpResponseHandlerParser());
        addGenericParser(new FormPropertyParser());
        addGenericParser(new IOSpecificationParser());
        addGenericParser(new MessageEventDefinitionParser());
        addGenericParser(new MultiInstanceParser());
        addGenericParser(new SignalEventDefinitionParser());
        addGenericParser(new TaskListenerParser());
        addGenericParser(new TerminateEventDefinitionParser());
        addGenericParser(new TimerEventDefinitionParser());
        addGenericParser(new TimeDateParser());
        addGenericParser(new TimeCycleParser());
        addGenericParser(new TimeDurationParser());
        addGenericParser(new FlowNodeRefParser());
        addGenericParser(new FlowableFailedjobRetryParser());
        addGenericParser(new FlowableMapExceptionParser());
        addGenericParser(new ElementNameParser());
    }

    private static void addGenericParser(BaseChildElementParser parser) {
        genericChildParserMap.put(parser.getElementName(), parser);
    }

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

    public static void parseChildElements(String elementName, BaseElement parentElement, XMLStreamReader xtr, BpmnModel model) throws Exception {
        parseChildElements(elementName, parentElement, xtr, null, model);
    }

    public static void parseChildElements(String elementName, BaseElement parentElement, XMLStreamReader xtr,
            Map<String, BaseChildElementParser> childParsers, BpmnModel model) throws Exception {

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
                        ExtensionElement extensionElement = BpmnXMLUtil.parseExtensionElement(xtr);
                        parentElement.addExtensionElement(extensionElement);
                        continue;
                    }
                    localParserMap.get(xtr.getLocalName()).parseChildElement(xtr, parentElement, model);
                    
                } else if (inExtensionElements) {
                    ExtensionElement extensionElement = BpmnXMLUtil.parseExtensionElement(xtr);
                    parentElement.addExtensionElement(extensionElement);
                }

            } else if (xtr.isEndElement()) {
                if (ELEMENT_EXTENSIONS.equals(xtr.getLocalName())) {
                    inExtensionElements = false;
                }
                
                if (elementName.equalsIgnoreCase(xtr.getLocalName())) {
                    readyWithChildElements = true;
                }
            }
        }
    }

    public static ExtensionElement parseExtensionElement(XMLStreamReader xtr) throws Exception {
        ExtensionElement extensionElement = new ExtensionElement();
        BpmnXMLUtil.addXMLLocation(extensionElement, xtr);
        extensionElement.setName(xtr.getLocalName());
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
        if (attributeValue == null) {
            attributeValue = xtr.getAttributeValue(ACTIVITI_EXTENSIONS_NAMESPACE, attributeName);
            if (attributeValue == null) {
                attributeValue = xtr.getAttributeValue(CAMUNDA_EXTENSIONS_NAMESPACE, attributeName);
            }
        }

        return attributeValue;
    }
    
    public static IOParameter parseInIOParameter(XMLStreamReader xtr) {
        IOParameter parameter = null;
        String source = xtr.getAttributeValue(null, ATTRIBUTE_IOPARAMETER_SOURCE);
        String sourceExpression = xtr.getAttributeValue(null, ATTRIBUTE_IOPARAMETER_SOURCE_EXPRESSION);
        String target = xtr.getAttributeValue(null, ATTRIBUTE_IOPARAMETER_TARGET);
        if ((StringUtils.isNotEmpty(source) || StringUtils.isNotEmpty(sourceExpression)) && StringUtils.isNotEmpty(target)) {

            parameter = new IOParameter();
            if (StringUtils.isNotEmpty(sourceExpression)) {
                parameter.setSourceExpression(sourceExpression);
            } else {
                parameter.setSource(source);
            }

            parameter.setTarget(target);
            
            for (int i = 0; i < xtr.getAttributeCount(); i++) {
                String attributeName = xtr.getAttributeLocalName(i);
                if (ATTRIBUTE_IOPARAMETER_SOURCE.equals(attributeName) || ATTRIBUTE_IOPARAMETER_SOURCE_EXPRESSION.equals(attributeName) ||
                                ATTRIBUTE_IOPARAMETER_TARGET.equals(attributeName)) {
                    
                    continue;
                }
                
                ExtensionAttribute extensionAttribute = new ExtensionAttribute();
                extensionAttribute.setName(attributeName);
                extensionAttribute.setValue(xtr.getAttributeValue(i));
                if (StringUtils.isNotEmpty(xtr.getAttributeNamespace(i))) {
                    extensionAttribute.setNamespace(xtr.getAttributeNamespace(i));
                }
                if (StringUtils.isNotEmpty(xtr.getAttributePrefix(i))) {
                    extensionAttribute.setNamespacePrefix(xtr.getAttributePrefix(i));
                }
                parameter.addAttribute(extensionAttribute);
            }
        }
        
        return parameter;
    }
    
    public static IOParameter parseOutIOParameter(XMLStreamReader xtr) {
        IOParameter parameter = null;
        String source = xtr.getAttributeValue(null, ATTRIBUTE_IOPARAMETER_SOURCE);
        String sourceExpression = xtr.getAttributeValue(null, ATTRIBUTE_IOPARAMETER_SOURCE_EXPRESSION);
        String target = xtr.getAttributeValue(null, ATTRIBUTE_IOPARAMETER_TARGET);
        String targetExpression = xtr.getAttributeValue(null, ATTRIBUTE_IOPARAMETER_TARGET_EXPRESSION);
        if ((StringUtils.isNotEmpty(source) || StringUtils.isNotEmpty(sourceExpression)) && 
                        (StringUtils.isNotEmpty(target) || StringUtils.isNotEmpty(targetExpression))) {

            parameter = new IOParameter();
            if (StringUtils.isNotEmpty(sourceExpression)) {
                parameter.setSourceExpression(sourceExpression);
            } else {
                parameter.setSource(source);
            }

            if (StringUtils.isNotEmpty(targetExpression)) {
                parameter.setTargetExpression(targetExpression);
            } else {
                parameter.setTarget(target);
            }
            
            for (int i = 0; i < xtr.getAttributeCount(); i++) {
                String attributeName = xtr.getAttributeLocalName(i);
                if (ATTRIBUTE_IOPARAMETER_SOURCE.equals(attributeName) || ATTRIBUTE_IOPARAMETER_SOURCE_EXPRESSION.equals(attributeName) ||
                                ATTRIBUTE_IOPARAMETER_TARGET.equals(attributeName) || ATTRIBUTE_IOPARAMETER_TARGET_EXPRESSION.equals(attributeName)) {
                    
                    continue;
                }
                
                ExtensionAttribute extensionAttribute = new ExtensionAttribute();
                extensionAttribute.setName(attributeName);
                extensionAttribute.setValue(xtr.getAttributeValue(i));
                if (StringUtils.isNotEmpty(xtr.getAttributeNamespace(i))) {
                    extensionAttribute.setNamespace(xtr.getAttributeNamespace(i));
                }
                if (StringUtils.isNotEmpty(xtr.getAttributePrefix(i))) {
                    extensionAttribute.setNamespacePrefix(xtr.getAttributePrefix(i));
                }
                parameter.addAttribute(extensionAttribute);
            }

            String transientString = xtr.getAttributeValue(null, ATTRIBUTE_IOPARAMETER_TRANSIENT);
            if ("true".equalsIgnoreCase(transientString)) {
                parameter.setTransient(true);
            }
        }
        
        return parameter;
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

    public static boolean writeExtensionElements(BaseElement baseElement, boolean didWriteExtensionStartElement, XMLStreamWriter xtw) throws Exception {
        return writeExtensionElements(baseElement, didWriteExtensionStartElement, null, xtw);
    }

    public static boolean writeExtensionElements(BaseElement baseElement, boolean didWriteExtensionStartElement, Map<String, String> namespaceMap, XMLStreamWriter xtw) throws Exception {
        if (!baseElement.getExtensionElements().isEmpty()) {
            if (!didWriteExtensionStartElement) {
                xtw.writeStartElement(ELEMENT_EXTENSIONS);
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
                                    localNamespaceMap.put(attribute.getNamespacePrefix(), attribute.getNamespace());
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

    public static boolean writeElementNameExtensionElement(FlowElement element, boolean didWriteExtensionStartElement, XMLStreamWriter xtw) throws Exception {
        String name = element.getName();
        if (BpmnXMLUtil.containsNewLine(name)) {
            if (!didWriteExtensionStartElement) {
                xtw.writeStartElement(ELEMENT_EXTENSIONS);
                didWriteExtensionStartElement = true;
            }

            xtw.writeStartElement(FLOWABLE_EXTENSIONS_PREFIX, ATTRIBUTE_ELEMENT_NAME, FLOWABLE_EXTENSIONS_NAMESPACE);
            xtw.writeCharacters(element.getName());
            xtw.writeEndElement();
        }

        return didWriteExtensionStartElement;
    }
    
    public static boolean writeIOParameters(String elementName, List<IOParameter> parameterList, boolean didWriteExtensionStartElement, XMLStreamWriter xtw) throws Exception {

        if (parameterList == null || parameterList.isEmpty()) {
            return didWriteExtensionStartElement;
        }

        for (IOParameter ioParameter : parameterList) {
            if (!didWriteExtensionStartElement) {
                xtw.writeStartElement(ELEMENT_EXTENSIONS);
                didWriteExtensionStartElement = true;
            }

            xtw.writeStartElement(FLOWABLE_EXTENSIONS_PREFIX, elementName, FLOWABLE_EXTENSIONS_NAMESPACE);
            if (StringUtils.isNotEmpty(ioParameter.getSourceExpression())) {
                writeDefaultAttribute(ATTRIBUTE_IOPARAMETER_SOURCE_EXPRESSION, ioParameter.getSourceExpression(), xtw);
                
            } else if (StringUtils.isNotEmpty(ioParameter.getSource())) {
                writeDefaultAttribute(ATTRIBUTE_IOPARAMETER_SOURCE, ioParameter.getSource(), xtw);
            }
            
            if (StringUtils.isNotEmpty(ioParameter.getAttributeValue(null, ATTRIBUTE_IOPARAMETER_SOURCE_TYPE))) {
                writeDefaultAttribute(ATTRIBUTE_IOPARAMETER_SOURCE_TYPE, ioParameter.getAttributeValue(null, ATTRIBUTE_IOPARAMETER_SOURCE_TYPE), xtw);
            }
            
            if (StringUtils.isNotEmpty(ioParameter.getTargetExpression())) {
                writeDefaultAttribute(ATTRIBUTE_IOPARAMETER_TARGET_EXPRESSION, ioParameter.getTargetExpression(), xtw);
                
            } else if (StringUtils.isNotEmpty(ioParameter.getTarget())) {
                writeDefaultAttribute(ATTRIBUTE_IOPARAMETER_TARGET, ioParameter.getTarget(), xtw);
            }
            
            if (StringUtils.isNotEmpty(ioParameter.getAttributeValue(null, ATTRIBUTE_IOPARAMETER_TARGET_TYPE))) {
                writeDefaultAttribute(ATTRIBUTE_IOPARAMETER_TARGET_TYPE, ioParameter.getAttributeValue(null, ATTRIBUTE_IOPARAMETER_TARGET_TYPE), xtw);
            }
            
            if (ioParameter.isTransient()) {
                writeDefaultAttribute(ATTRIBUTE_IOPARAMETER_TRANSIENT, "true", xtw);
            }

            writeCustomAttributes(ioParameter.getAttributes().values(), xtw,
                    InParameterParser.defaultInParameterAttributes, OutParameterParser.defaultOutParameterAttributes);

            xtw.writeEndElement();
        }

        return didWriteExtensionStartElement;
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

    /**
     * add all attributes from XML to element extensionAttributes (except blackListed).
     * 
     * @param xtr
     * @param element
     * @param blackLists
     */
    public static void addCustomAttributes(XMLStreamReader xtr, BaseElement element, List<ExtensionAttribute>... blackLists) {
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
            if (!isBlacklisted(extensionAttribute, blackLists)) {
                element.addAttribute(extensionAttribute);
            }
        }
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

    public static boolean isBlacklisted(ExtensionAttribute attribute, List<ExtensionAttribute>... blackLists) {
        if (blackLists != null) {
            for (List<ExtensionAttribute> blackList : blackLists) {
                for (ExtensionAttribute blackAttribute : blackList) {
                    if (blackAttribute.getName().equals(attribute.getName())) {
                        if (attribute.getNamespace() != null && (FLOWABLE_EXTENSIONS_NAMESPACE.equals(attribute.getNamespace()) ||
                                ACTIVITI_EXTENSIONS_NAMESPACE.equals(attribute.getNamespace()) ||
                                CAMUNDA_EXTENSIONS_NAMESPACE.equals(attribute.getNamespace()))) {

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

    public static void parseLabelElement(XMLStreamReader xtr, BpmnModel model, String BpmnElementId) throws Exception {
        GraphicInfo labelGraphicInfo = new GraphicInfo();
        BpmnXMLUtil.addXMLLocation(labelGraphicInfo, xtr);

        if (xtr.getAttributeValue(null, ATTRIBUTE_DI_ROTATION) != null
                && !xtr.getAttributeValue(null, ATTRIBUTE_DI_ROTATION).isEmpty()) {
            labelGraphicInfo.setRotation(Double.valueOf(xtr.getAttributeValue(null, ATTRIBUTE_DI_ROTATION)).intValue());
        }

        while (xtr.hasNext()) {
            xtr.next();
            if (xtr.isStartElement() && ELEMENT_DI_BOUNDS.equalsIgnoreCase(xtr.getLocalName())) {

                labelGraphicInfo.setX(Double.valueOf(xtr.getAttributeValue(null, ATTRIBUTE_DI_X)).intValue());
                labelGraphicInfo.setY(Double.valueOf(xtr.getAttributeValue(null, ATTRIBUTE_DI_Y)).intValue());
                labelGraphicInfo.setWidth(Double.valueOf(xtr.getAttributeValue(null, ATTRIBUTE_DI_WIDTH)).intValue());
                labelGraphicInfo.setHeight(Double.valueOf(xtr.getAttributeValue(null, ATTRIBUTE_DI_HEIGHT)).intValue());
                model.addLabelGraphicInfo(BpmnElementId, labelGraphicInfo);
                break;
            } else if (xtr.isEndElement() && ELEMENT_DI_LABEL.equalsIgnoreCase(xtr.getLocalName())) {
                break;
            }
        }
    }

    public static boolean containsNewLine(String str) {
        return str != null && str.contains("\n");
    }
}
