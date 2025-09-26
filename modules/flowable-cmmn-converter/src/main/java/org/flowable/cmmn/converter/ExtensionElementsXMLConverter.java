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
import static org.flowable.cmmn.converter.CmmnXmlConstants.ATTRIBUTE_ELEMENT_NAME;
import static org.flowable.cmmn.model.ImplementationType.IMPLEMENTATION_TYPE_CLASS;
import static org.flowable.cmmn.model.ImplementationType.IMPLEMENTATION_TYPE_DELEGATEEXPRESSION;

import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamReader;

import org.apache.commons.lang3.StringUtils;
import org.flowable.cmmn.converter.exception.XMLException;
import org.flowable.cmmn.converter.util.CmmnXmlUtil;
import org.flowable.cmmn.converter.util.ListenerXmlConverterUtil;
import org.flowable.cmmn.model.AbstractFlowableHttpHandler;
import org.flowable.cmmn.model.BaseElement;
import org.flowable.cmmn.model.Case;
import org.flowable.cmmn.model.CaseElement;
import org.flowable.cmmn.model.CmmnElement;
import org.flowable.cmmn.model.CompletionNeutralRule;
import org.flowable.cmmn.model.DecisionTask;
import org.flowable.cmmn.model.ExtensionElement;
import org.flowable.cmmn.model.FieldExtension;
import org.flowable.cmmn.model.FlowableHttpRequestHandler;
import org.flowable.cmmn.model.FlowableHttpResponseHandler;
import org.flowable.cmmn.model.FlowableListener;
import org.flowable.cmmn.model.GenericEventListener;
import org.flowable.cmmn.model.HasInParameters;
import org.flowable.cmmn.model.HasLifecycleListeners;
import org.flowable.cmmn.model.HasOutParameters;
import org.flowable.cmmn.model.HttpServiceTask;
import org.flowable.cmmn.model.HumanTask;
import org.flowable.cmmn.model.IOParameter;
import org.flowable.cmmn.model.ImplementationType;
import org.flowable.cmmn.model.ParentCompletionRule;
import org.flowable.cmmn.model.PlanItemControl;
import org.flowable.cmmn.model.ReactivateEventListener;
import org.flowable.cmmn.model.ReactivationRule;
import org.flowable.cmmn.model.RepetitionRule;
import org.flowable.cmmn.model.SendEventServiceTask;
import org.flowable.cmmn.model.ServiceTask;
import org.flowable.cmmn.model.VariableAggregationDefinition;
import org.flowable.common.engine.api.FlowableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Tijs Rademakers
 * @author Joram Barrez
 * @author Filip Hrisafov
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
                        
                    } else if (CmmnXmlConstants.ELEMENT_PARENT_COMPLETION_RULE.equals(xtr.getLocalName())) {
                        readParentCompletionRule(xtr, conversionHelper);

                    } else if (CmmnXmlConstants.ELEMENT_REACTIVATION_RULE.equals(xtr.getLocalName())) {
                        readReactivationRule(xtr, conversionHelper);

                    } else if (CmmnXmlConstants.ELEMENT_DEFAULT_REACTIVATION_RULE.equals(xtr.getLocalName())) {
                        readDefaultReactivationRule(xtr, conversionHelper);

                    } else if (CmmnXmlConstants.ELEMENT_FIELD.equals(xtr.getLocalName())) {
                        readFieldExtension(xtr, conversionHelper);

                    } else if (CmmnXmlConstants.ELEMENT_HTTP_REQUEST_HANDLER.equals(xtr.getLocalName())) {
                        readHttpRequestHandler(xtr, conversionHelper);

                    } else if (CmmnXmlConstants.ELEMENT_HTTP_RESPONSE_HANDLER.equals(xtr.getLocalName())) {
                        readHttpResponseHandler(xtr, conversionHelper);

                    } else if (CmmnXmlConstants.ELEMENT_CHILD_TASK_IN_PARAMETERS.equals(xtr.getLocalName())
                            || CmmnXmlConstants.ELEMENT_EXTERNAL_WORKER_IN_PARAMETER.equals(xtr.getLocalName())) {
                        readInputParameter(xtr, conversionHelper);

                    } else if (CmmnXmlConstants.ELEMENT_CHILD_TASK_OUT_PARAMETERS.equals(xtr.getLocalName())
                            || CmmnXmlConstants.ELEMENT_EXTERNAL_WORKER_OUT_PARAMETER.equals(xtr.getLocalName())) {
                        readOutputParameter(xtr, conversionHelper);

                    } else if (CmmnXmlConstants.ELEMENT_TASK_LISTENER.equals(xtr.getLocalName())) {
                        readTaskListener(xtr, conversionHelper);

                    } else if (CmmnXmlConstants.ELEMENT_PLAN_ITEM_LIFECYCLE_LISTENER.equals(xtr.getLocalName()) ||
                                    CmmnXmlConstants.ELEMENT_CASE_LIFECYCLE_LISTENER.equals(xtr.getLocalName())) {
                        
                        readLifecycleListener(xtr, conversionHelper);

                    } else if (CmmnXmlConstants.ELEMENT_EVENT_TYPE.equals(xtr.getLocalName())) {
                        readEventType(xtr, conversionHelper);

                    } else if (CmmnXmlConstants.ELEMENT_VARIABLE_AGGREGATION.equals(xtr.getLocalName())) {
                        readVariableAggregationDefinition(xtr, conversionHelper);

                    } else if (CmmnXmlConstants.ATTRIBUTE_ELEMENT_NAME.equals(xtr.getLocalName())) {
                        readElementName(xtr, conversionHelper);
                    } else {
                        ExtensionElement extensionElement = CmmnXmlUtil.parseExtensionElement(xtr);
                        conversionHelper.getCurrentCmmnElement().addExtensionElement(extensionElement);

                    }

                } else if (xtr.isEndElement()) {
                    if (CmmnXmlConstants.ELEMENT_TASK_LISTENER.equalsIgnoreCase(xtr.getLocalName())
                                    || CmmnXmlConstants.ELEMENT_PLAN_ITEM_LIFECYCLE_LISTENER.equalsIgnoreCase(xtr.getLocalName())
                                    || CmmnXmlConstants.ELEMENT_CASE_LIFECYCLE_LISTENER.equalsIgnoreCase(xtr.getLocalName())) {
                        
                        conversionHelper.removeCurrentCmmnElement();
                        
                    } else if (CmmnXmlConstants.ELEMENT_EXTENSION_ELEMENTS.equalsIgnoreCase(xtr.getLocalName())) {
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
        if (conversionHelper.getCurrentCmmnElement() instanceof PlanItemControl planItemControl) {
            CompletionNeutralRule completionNeutralRule = new CompletionNeutralRule();
            completionNeutralRule.setName(xtr.getAttributeValue(null, CmmnXmlConstants.ATTRIBUTE_NAME));

            planItemControl.setCompletionNeutralRule(completionNeutralRule);

            readCommonXmlInfo(completionNeutralRule, xtr);

            boolean readyWithChildElements = false;
            try {

                while (!readyWithChildElements && xtr.hasNext()) {
                    xtr.next();
                    if (xtr.isStartElement()) {
                        if (CmmnXmlConstants.ELEMENT_EXTENSION_ELEMENTS.equals(xtr.getLocalName())) {
                            boolean readyWithChildExtensionElements = false;
                            while (!readyWithChildExtensionElements && xtr.hasNext()) {
                                xtr.next();
                                if (xtr.isStartElement()) {
                                    ExtensionElement extensionElement = CmmnXmlUtil.parseExtensionElement(xtr);
                                    completionNeutralRule.addExtensionElement(extensionElement);
                                } else if (xtr.isEndElement() && CmmnXmlConstants.ELEMENT_EXTENSION_ELEMENTS.equals(xtr.getLocalName())) {
                                    readyWithChildExtensionElements = true;
                                }
                            }
                        } else if (CmmnXmlConstants.ELEMENT_CONDITION.equals(xtr.getLocalName())) {
                            completionNeutralRule.setCondition(xtr.getElementText());
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
    
    protected void readParentCompletionRule(XMLStreamReader xtr, ConversionHelper conversionHelper) {
        if (conversionHelper.getCurrentCmmnElement() instanceof PlanItemControl planItemControl) {
            ParentCompletionRule parentCompletionRule = new ParentCompletionRule();
            parentCompletionRule.setName(xtr.getAttributeValue(null, CmmnXmlConstants.ATTRIBUTE_NAME));
            parentCompletionRule.setType(xtr.getAttributeValue(null, CmmnXmlConstants.ATTRIBUTE_TYPE));

            planItemControl.setParentCompletionRule(parentCompletionRule);

            readCommonXmlInfo(parentCompletionRule, xtr);
        }
    }

    protected void readReactivationRule(XMLStreamReader xtr, ConversionHelper conversionHelper) {
        if (conversionHelper.getCurrentCmmnElement() instanceof PlanItemControl planItemControl) {
            planItemControl.setReactivationRule(readReactivationRule(xtr));
        }
    }

    protected void readDefaultReactivationRule(XMLStreamReader xtr, ConversionHelper conversionHelper) {
        if (conversionHelper.getCurrentCmmnElement() instanceof ReactivateEventListener reactivateEventListener) {
            reactivateEventListener.setDefaultReactivationRule(readReactivationRule(xtr));
        }
    }

    protected ReactivationRule readReactivationRule(XMLStreamReader xtr) {
        ReactivationRule reactivationRule = new ReactivationRule();
        reactivationRule.setName(xtr.getAttributeValue(null, CmmnXmlConstants.ATTRIBUTE_NAME));
        reactivationRule.setActivateCondition(xtr.getAttributeValue(null, CmmnXmlConstants.ATTRIBUTE_ACTIVATE_CONDITION));
        reactivationRule.setIgnoreCondition(xtr.getAttributeValue(null, CmmnXmlConstants.ATTRIBUTE_IGNORE_CONDITION));
        reactivationRule.setDefaultCondition(xtr.getAttributeValue(null, CmmnXmlConstants.ATTRIBUTE_DEFAULT_CONDITION));
        readCommonXmlInfo(reactivationRule, xtr);
        return reactivationRule;
    }

    protected void readFieldExtension(XMLStreamReader xtr, ConversionHelper conversionHelper) {
        BaseElement cmmnElement = conversionHelper.getCurrentCmmnElement();

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

        CmmnElement currentCmmnElement = conversionHelper.getCurrentCmmnElement();
        if (currentCmmnElement instanceof ServiceTask) {
            ((ServiceTask) currentCmmnElement).getFieldExtensions().add(extension);

        } else if (currentCmmnElement instanceof DecisionTask) {
            ((DecisionTask) currentCmmnElement).getFieldExtensions().add(extension);
        } else if (currentCmmnElement instanceof FlowableListener) {
            ((FlowableListener) currentCmmnElement).getFieldExtensions().add(extension);

        } else {
            throw new FlowableException("Programmatic error: field added to unknown element '" + currentCmmnElement + "'");

        }
    }

    protected void readHttpRequestHandler(XMLStreamReader xtr, ConversionHelper conversionHelper) {
        BaseElement cmmnElement = conversionHelper.getCurrentCmmnElement();
        if (!(cmmnElement instanceof HttpServiceTask)) {
            return;
        }

        FlowableHttpRequestHandler requestHandler = new FlowableHttpRequestHandler();
        setImplementation(xtr, requestHandler);

        ((HttpServiceTask) cmmnElement).setHttpRequestHandler(requestHandler);
    }

    protected void readHttpResponseHandler(XMLStreamReader xtr, ConversionHelper conversionHelper) {
        BaseElement cmmnElement = conversionHelper.getCurrentCmmnElement();
        if (!(cmmnElement instanceof HttpServiceTask)) {
            return;
        }

        FlowableHttpResponseHandler responseHandler = new FlowableHttpResponseHandler();
        setImplementation(xtr, responseHandler);

        ((HttpServiceTask) cmmnElement).setHttpResponseHandler(responseHandler);
    }

    protected void readInputParameter(XMLStreamReader xtr, ConversionHelper conversionHelper) {
        CmmnElement currentCmmnElement = conversionHelper.getCurrentCmmnElement();
        if (currentCmmnElement instanceof HasInParameters hasInParameters) {
            hasInParameters.addInParameter(readIoParameter(xtr));
        }
    }

    protected void readOutputParameter(XMLStreamReader xtr, ConversionHelper conversionHelper) {
        CmmnElement currentCmmnElement = conversionHelper.getCurrentCmmnElement();
        if (currentCmmnElement instanceof HasOutParameters hasOutParameters) {
            hasOutParameters.addOutParameter(readIoParameter(xtr));
        }
    }

    protected IOParameter readIoParameter(XMLStreamReader xtr) {
        String source = xtr.getAttributeValue(null, CmmnXmlConstants.ATTRIBUTE_IOPARAMETER_SOURCE);
        String sourceExpression = xtr.getAttributeValue(null, CmmnXmlConstants.ATTRIBUTE_IOPARAMETER_SOURCE_EXPRESSION);
        String target = xtr.getAttributeValue(null, CmmnXmlConstants.ATTRIBUTE_IOPARAMETER_TARGET);
        String targetExpression = xtr.getAttributeValue(null, CmmnXmlConstants.ATTRIBUTE_IOPARAMETER_TARGET_EXPRESSION);
        String isTransient = xtr.getAttributeValue(null, CmmnXmlConstants.ATTRIBUTE_IOPARAMETER_TRANSIENT);

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

        if (StringUtils.isNotEmpty(isTransient)) {
            parameter.setTransient(Boolean.parseBoolean(isTransient));
        }
        return parameter;
    }

    protected void readTaskListener(XMLStreamReader xtr, ConversionHelper conversionHelper) throws Exception {
        BaseElement currentCmmnElement = conversionHelper.getCurrentCmmnElement(); // needs to be captured before setting the flowable listeners as this will change the current element

        FlowableListener flowableListener = ListenerXmlConverterUtil.convertToListener(xtr);
        if (flowableListener != null) {
            if (currentCmmnElement instanceof HumanTask humanTask) {
                humanTask.getTaskListeners().add(flowableListener);
            } else {
                throw new FlowableException("Programmatic error: task listener added to an element that is not a human task, but a " + currentCmmnElement.getClass());
            }
        }

        conversionHelper.setCurrentCmmnElement(flowableListener);
    }

    protected void readLifecycleListener(XMLStreamReader xtr, ConversionHelper conversionHelper) throws Exception {
        BaseElement currentCmmnElement = conversionHelper.getCurrentCmmnElement(); // needs to be captured before setting the flowable listeners as this will change the current element

        FlowableListener flowableListener = ListenerXmlConverterUtil.convertToListener(xtr);
        if (flowableListener != null) {
            if (currentCmmnElement instanceof HasLifecycleListeners lifecycleListenersElement) {
                lifecycleListenersElement.getLifecycleListeners().add(flowableListener);
            } else {
                throw new FlowableException("Programmatic error: lifecycle listener added to an element that is not a plan item definition, but a " + currentCmmnElement.getClass());
            }
        }

        conversionHelper.setCurrentCmmnElement(flowableListener);

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

    protected void readEventType(XMLStreamReader xtr, ConversionHelper conversionHelper) {
        CmmnElement currentCmmnElement = conversionHelper.getCurrentCmmnElement();

        String eventType = null;
        try {

            // Parsing and storing as an extension element, which means the export will work automatically
            ExtensionElement extensionElement = CmmnXmlUtil.parseExtensionElement(xtr);
            eventType = extensionElement.getElementText();
            
        } catch (Exception e) {
            throw new FlowableException("Error while reading eventType element", e);
        }

        if (currentCmmnElement instanceof Case caze) {
            caze.setStartEventType(eventType);

        } else if (currentCmmnElement instanceof SendEventServiceTask sendEventServiceTask) {
            sendEventServiceTask.setEventType(eventType);

        } else if (currentCmmnElement instanceof GenericEventListener genericEventListener) {
            genericEventListener.setEventType(eventType);

        } else {
            LOGGER.warn("Unsupported eventType detected for element {}", currentCmmnElement);
        }
    }

    protected void readVariableAggregationDefinition(XMLStreamReader xtr, ConversionHelper conversionHelper) {
        CmmnElement currentCmmnElement = conversionHelper.getCurrentCmmnElement();

        if (currentCmmnElement instanceof RepetitionRule repetitionRule) {

            VariableAggregationDefinition aggregationDefinition = new VariableAggregationDefinition();

            if (StringUtils.isNotEmpty(xtr.getAttributeValue(null, ATTRIBUTE_CLASS))) {
                aggregationDefinition.setImplementation(xtr.getAttributeValue(null, ATTRIBUTE_CLASS));
                aggregationDefinition.setImplementationType(ImplementationType.IMPLEMENTATION_TYPE_CLASS);

            } else if (StringUtils.isNotEmpty(xtr.getAttributeValue(null, ATTRIBUTE_DELEGATE_EXPRESSION))) {
                aggregationDefinition.setImplementation(xtr.getAttributeValue(null, ATTRIBUTE_DELEGATE_EXPRESSION));
                aggregationDefinition.setImplementationType(ImplementationType.IMPLEMENTATION_TYPE_DELEGATEEXPRESSION);
            }

            aggregationDefinition.setTarget(xtr.getAttributeValue(null, CmmnXmlConstants.ATTRIBUTE_IOPARAMETER_TARGET));
            aggregationDefinition.setTargetExpression(xtr.getAttributeValue(null, CmmnXmlConstants.ATTRIBUTE_IOPARAMETER_TARGET_EXPRESSION));
            aggregationDefinition.setStoreAsTransientVariable(Boolean.parseBoolean(xtr.getAttributeValue(null, CmmnXmlConstants.ATTRIBUTE_VARIABLE_AGGREGATION_STORE_AS_TRANSIENT_VARIABLE)));
            aggregationDefinition.setCreateOverviewVariable(Boolean.parseBoolean(xtr.getAttributeValue(null, CmmnXmlConstants.ATTRIBUTE_VARIABLE_AGGREGATION_CREATE_OVERVIEW)));

            repetitionRule.addAggregation(aggregationDefinition);

            boolean readyWithAggregation = false;
            try {
                while (!readyWithAggregation && xtr.hasNext()) {
                    xtr.next();
                    if (xtr.isStartElement() && CmmnXmlConstants.ATTRIBUTE_VARIABLE_AGGREGATION_VARIABLE.equalsIgnoreCase(xtr.getLocalName())) {
                        VariableAggregationDefinition.Variable definition = new VariableAggregationDefinition.Variable();

                        definition.setSource(xtr.getAttributeValue(null, CmmnXmlConstants.ATTRIBUTE_IOPARAMETER_SOURCE));
                        definition.setSourceExpression(xtr.getAttributeValue(null, CmmnXmlConstants.ATTRIBUTE_IOPARAMETER_SOURCE_EXPRESSION));
                        definition.setTarget(xtr.getAttributeValue(null, CmmnXmlConstants.ATTRIBUTE_IOPARAMETER_TARGET));
                        definition.setTargetExpression(xtr.getAttributeValue(null, CmmnXmlConstants.ATTRIBUTE_IOPARAMETER_TARGET_EXPRESSION));

                        aggregationDefinition.addDefinition(definition);
                    } else if (xtr.isEndElement() && CmmnXmlConstants.ELEMENT_VARIABLE_AGGREGATION.equalsIgnoreCase(xtr.getLocalName())) {
                        readyWithAggregation = true;
                    }
                }
            } catch (Exception e) {
                LOGGER.warn("Error parsing collection child elements", e);
            }
        }

    }

    protected void readElementName(XMLStreamReader xtr, ConversionHelper conversionHelper) {
        CmmnElement currentCmmnElement = conversionHelper.getCurrentCmmnElement();

        if (currentCmmnElement instanceof CaseElement) {
            try {
                String elementName = xtr.getElementText();

                if (StringUtils.isNotEmpty(elementName)) {
                    ((CaseElement) currentCmmnElement).setName(elementName.trim());
                }
            } catch (Exception e) {
                throw new FlowableException("Error while reading " + ATTRIBUTE_ELEMENT_NAME + " extension element", e);
            }

        }

    }

    protected void readCommonXmlInfo(BaseElement baseElement, XMLStreamReader xtr) {
        baseElement.setId(xtr.getAttributeValue(null, CmmnXmlConstants.ATTRIBUTE_ID));
        Location location = xtr.getLocation();
        baseElement.setXmlRowNumber(location.getLineNumber());
        baseElement.setXmlRowNumber(location.getColumnNumber());
    }

}
