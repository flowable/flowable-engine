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

package org.flowable.cmmn.converter;

import static org.flowable.cmmn.converter.CmmnXmlConstants.ATTRIBUTE_CLASS;
import static org.flowable.cmmn.converter.CmmnXmlConstants.ATTRIBUTE_DELEGATE_EXPRESSION;
import static org.flowable.cmmn.model.ImplementationType.IMPLEMENTATION_TYPE_CLASS;
import static org.flowable.cmmn.model.ImplementationType.IMPLEMENTATION_TYPE_DELEGATEEXPRESSION;

import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamReader;

import org.apache.commons.lang3.StringUtils;
import org.flowable.cmmn.converter.exception.XMLException;
import org.flowable.cmmn.converter.util.CmmnXmlUtil;
import org.flowable.cmmn.model.AbstractFlowableHttpHandler;
import org.flowable.cmmn.model.BaseElement;
import org.flowable.cmmn.model.CmmnElement;
import org.flowable.cmmn.model.CompletionNeutralRule;
import org.flowable.cmmn.model.DecisionTask;
import org.flowable.cmmn.model.ExtensionElement;
import org.flowable.cmmn.model.FieldExtension;
import org.flowable.cmmn.model.FlowableHttpRequestHandler;
import org.flowable.cmmn.model.FlowableHttpResponseHandler;
import org.flowable.cmmn.model.HttpServiceTask;
import org.flowable.cmmn.model.IOParameter;
import org.flowable.cmmn.model.PlanItemControl;
import org.flowable.cmmn.model.ProcessTask;
import org.flowable.cmmn.model.ServiceTask;
import org.flowable.cmmn.model.TaskWithFieldExtensions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @auther Tijs Rademakers
 */
public class ExtensionElementsXMLConverter extends CaseElementXmlConverter {

    protected static final Logger LOGGER = LoggerFactory.getLogger(ExtensionElementsXMLConverter.class);

    @Override
    public String getXMLElementName() {
        return CmmnXmlConstants.ELEMENT_EXTENSION_ELEMENTS;
    }

    @Override
    public boolean hasChildElements() {
        return false;
    }

    @Override
    protected CmmnElement convert(XMLStreamReader xtr, ConversionHelper conversionHelper) {

        boolean readyWithChildElements = false;
        try {

            while (!readyWithChildElements && xtr.hasNext()) {
                xtr.next();
                if (xtr.isStartElement()) {
                    if (CmmnXmlConstants.ELEMENT_COMPLETION_NEUTRAL_RULE.equals(xtr.getLocalName())) {
                        readCompletionNeutralRule(xtr, conversionHelper);
                        
                    } else if (CmmnXmlConstants.ELEMENT_FIELD.equals(xtr.getLocalName())) {
                        readFieldExtension(xtr, conversionHelper);
                        
                    } else if (CmmnXmlConstants.ELEMENT_HTTP_REQUEST_HANDLER.equals(xtr.getLocalName())) {
                        readHttpRequestHandler(xtr, conversionHelper);
                        
                    } else if (CmmnXmlConstants.ELEMENT_HTTP_RESPONSE_HANDLER.equals(xtr.getLocalName())) {
                        readHttpResponseHandler(xtr, conversionHelper);
                        
                    } else if (CmmnXmlConstants.ELEMENT_PROCESS_TASK_IN_PARAMETERS.equals(xtr.getLocalName())) {
                        readIOParameter(xtr, true, conversionHelper);
                        
                    } else if (CmmnXmlConstants.ELEMENT_PROCESS_TASK_OUT_PARAMETERS.equals(xtr.getLocalName())) {
                        readIOParameter(xtr, false, conversionHelper);
                        
                    } else {
                        ExtensionElement extensionElement = CmmnXmlUtil.parseExtensionElement(xtr);
                        conversionHelper.getCurrentCmmnElement().addExtensionElement(extensionElement);
                    }
                    
                } else if (xtr.isEndElement()) {
                    if (CmmnXmlConstants.ELEMENT_EXTENSION_ELEMENTS.equalsIgnoreCase(xtr.getLocalName())) {
                        readyWithChildElements = true;
                    }
                }

            }
        } catch (Exception ex) {
            LOGGER.error("Error processing CMMN document", ex);
            throw new XMLException("Error processing CMMN document", ex);
        }
   
        return null;
    }

    protected void readCompletionNeutralRule(XMLStreamReader xtr, ConversionHelper conversionHelper) {
        if (conversionHelper.getCurrentCmmnElement() instanceof PlanItemControl) {
            CompletionNeutralRule completionNeutralRule = new CompletionNeutralRule();
            completionNeutralRule.setName(xtr.getAttributeValue(null, CmmnXmlConstants.ATTRIBUTE_NAME));

            PlanItemControl planItemControl = (PlanItemControl) conversionHelper.getCurrentCmmnElement();
            planItemControl.setCompletionNeutralRule(completionNeutralRule);
            
            readCommonXmlInfo(completionNeutralRule, xtr);
            
            boolean readyWithChildElements = false;
            try {

                while (!readyWithChildElements && xtr.hasNext()) {
                    xtr.next();
                    if (xtr.isStartElement()) {
                        if (CmmnXmlConstants.ELEMENT_CONDITION.equals(xtr.getLocalName())) {
                            xtr.next();
                            if (xtr.isCharacters()) {
                                completionNeutralRule.setCondition(xtr.getText());
                            }
                            break;
                        } 
                        
                    } else if (xtr.isEndElement()) {
                        if (CmmnXmlConstants.ELEMENT_COMPLETION_NEUTRAL_RULE.equalsIgnoreCase(xtr.getLocalName())) {
                            readyWithChildElements = true;
                        }
                    }

                }
            } catch (Exception ex) {
                LOGGER.error("Error processing CMMN document", ex);
                throw new XMLException("Error processing CMMN document", ex);
            }
        }
    }
    
    protected void readFieldExtension(XMLStreamReader xtr, ConversionHelper conversionHelper) {
        CmmnElement cmmnElement = conversionHelper.getCurrentCmmnElement();
        if (!(cmmnElement instanceof ServiceTask || cmmnElement instanceof DecisionTask)) {
            return;
        }

        TaskWithFieldExtensions serviceTask = (TaskWithFieldExtensions) cmmnElement;

        FieldExtension extension = new FieldExtension();
        extension.setFieldName(xtr.getAttributeValue(null, CmmnXmlConstants.ATTRIBUTE_NAME));

        String stringValueAttribute = xtr.getAttributeValue(null, CmmnXmlConstants.ATTRIBUTE_FIELD_STRING);
        String expressionAttribute = xtr.getAttributeValue(null, CmmnXmlConstants.ATTRIBUTE_FIELD_EXPRESSION);
        if (StringUtils.isNotEmpty(stringValueAttribute)) {
            extension.setStringValue(stringValueAttribute);

        } else if (StringUtils.isNotEmpty(expressionAttribute)) {
            extension.setExpression(expressionAttribute);

        } else {
            boolean readyWithFieldExtension = false;
            try {
                while (!readyWithFieldExtension && xtr.hasNext()) {
                    xtr.next();
                    if (xtr.isStartElement() && CmmnXmlConstants.ELEMENT_FIELD_STRING.equalsIgnoreCase(xtr.getLocalName())) {
                        extension.setStringValue(xtr.getElementText().trim());

                    } else if (xtr.isStartElement() && CmmnXmlConstants.ELEMENT_FIELD_EXPRESSION.equalsIgnoreCase(xtr.getLocalName())) {
                        extension.setExpression(xtr.getElementText().trim());

                    } else if (xtr.isEndElement() && CmmnXmlConstants.ELEMENT_FIELD.equalsIgnoreCase(xtr.getLocalName())) {
                        readyWithFieldExtension = true;
                    }
                }
            } catch (Exception e) {
                LOGGER.warn("Error parsing field extension child elements", e);
            }
        }

        serviceTask.getFieldExtensions().add(extension);
    }
    
    protected void readHttpRequestHandler(XMLStreamReader xtr, ConversionHelper conversionHelper) {
        CmmnElement cmmnElement = conversionHelper.getCurrentCmmnElement();
        if (!(cmmnElement instanceof HttpServiceTask)) {
            return;
        }

        FlowableHttpRequestHandler requestHandler = new FlowableHttpRequestHandler();
        setImplementation(xtr, requestHandler);

        ((HttpServiceTask) cmmnElement).setHttpRequestHandler(requestHandler);
    }
    
    protected void readHttpResponseHandler(XMLStreamReader xtr, ConversionHelper conversionHelper) {
        CmmnElement cmmnElement = conversionHelper.getCurrentCmmnElement();
        if (!(cmmnElement instanceof HttpServiceTask)) {
            return;
        }

        FlowableHttpResponseHandler responseHandler = new FlowableHttpResponseHandler();
        setImplementation(xtr, responseHandler);

        ((HttpServiceTask) cmmnElement).setHttpResponseHandler(responseHandler);
    }
    
    protected void readIOParameter(XMLStreamReader xtr, boolean isInParameter, ConversionHelper conversionHelper) {
        if (!(conversionHelper.getCurrentCmmnElement() instanceof ProcessTask)) {
            return;
        }
        
        ProcessTask processTask = (ProcessTask) conversionHelper.getCurrentCmmnElement();
        String source = xtr.getAttributeValue(null, CmmnXmlConstants.ATTRIBUTE_IOPARAMETER_SOURCE);
        String sourceExpression = xtr.getAttributeValue(null, CmmnXmlConstants.ATTRIBUTE_IOPARAMETER_SOURCE_EXPRESSION);
        String target = xtr.getAttributeValue(null, CmmnXmlConstants.ATTRIBUTE_IOPARAMETER_TARGET);
        String targetExpression = xtr.getAttributeValue(null, CmmnXmlConstants.ATTRIBUTE_IOPARAMETER_TARGET_EXPRESSION);

        IOParameter parameter = new IOParameter();

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

        if (isInParameter) {
            processTask.getInParameters().add(parameter);
        } else {
            processTask.getOutParameters().add(parameter);
        }
    }
    
    protected void setImplementation(XMLStreamReader xtr, AbstractFlowableHttpHandler handler) {
        if (StringUtils.isNotEmpty(xtr.getAttributeValue(null, ATTRIBUTE_CLASS))) {
            handler.setImplementation(xtr.getAttributeValue(null, ATTRIBUTE_CLASS));
            handler.setImplementationType(IMPLEMENTATION_TYPE_CLASS);

        } else if (StringUtils.isNotEmpty(xtr.getAttributeValue(null, ATTRIBUTE_DELEGATE_EXPRESSION))) {
            handler.setImplementation(xtr.getAttributeValue(null, ATTRIBUTE_DELEGATE_EXPRESSION));
            handler.setImplementationType(IMPLEMENTATION_TYPE_DELEGATEEXPRESSION);
        }
    }
    
    protected void readCommonXmlInfo(BaseElement baseElement, XMLStreamReader xtr) {
        baseElement.setId(xtr.getAttributeValue(null, CmmnXmlConstants.ATTRIBUTE_ID));
        Location location = xtr.getLocation();
        baseElement.setXmlRowNumber(location.getLineNumber());
        baseElement.setXmlRowNumber(location.getColumnNumber());
    }

}
