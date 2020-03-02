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
package org.flowable.dmn.xml.converter;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import org.flowable.dmn.converter.child.BaseChildElementParser;
import org.flowable.dmn.converter.util.DmnXMLUtil;
import org.flowable.dmn.model.AuthorityRequirement;
import org.flowable.dmn.model.DecisionRule;
import org.flowable.dmn.model.Decision;
import org.flowable.dmn.model.DecisionService;
import org.flowable.dmn.model.DecisionTable;
import org.flowable.dmn.model.DmnDefinition;
import org.flowable.dmn.model.DmnElement;
import org.flowable.dmn.model.InformationItem;
import org.flowable.dmn.model.InformationRequirement;
import org.flowable.dmn.model.InputClause;
import org.flowable.dmn.model.InputData;
import org.flowable.dmn.model.ItemDefinition;
import org.flowable.dmn.model.OutputClause;
import org.flowable.dmn.xml.constants.DmnXMLConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Tijs Rademakers
 * @author Yvo Swillens
 */
public abstract class BaseDmnXMLConverter implements DmnXMLConstants {

    protected static final Logger LOGGER = LoggerFactory.getLogger(BaseDmnXMLConverter.class);

    private int elementCounter;

    public void convertToDmnModel(XMLStreamReader xtr, DmnDefinition model, Decision decision) throws Exception {
        DmnElement parsedElement = convertXMLToElement(xtr, model, decision);

        //TODO: this needs to change when we support more expression types
        Optional<DecisionTable> decisionTable = null;
        if (decision != null && decision.getExpression() != null) {
            DecisionTable decisionTableExpression = (DecisionTable) decision.getExpression();
            decisionTable = Optional.of(decisionTableExpression);
        }

        if (parsedElement instanceof InputClause) {
            decisionTable.ifPresent(dt -> {
                InputClause inputClause = (InputClause) parsedElement;
                inputClause.setInputNumber(elementCounter);
                dt.addInput(inputClause);
                elementCounter++;
            });
        } else if (parsedElement instanceof OutputClause) {
            decisionTable.ifPresent(dt -> {
                OutputClause outputClause = (OutputClause) parsedElement;
                outputClause.setOutputNumber(elementCounter);
                dt.addOutput(outputClause);
                elementCounter++;
            });

        } else if (parsedElement instanceof DecisionRule) {
            decisionTable.ifPresent(dt -> {
                DecisionRule decisionRule = (DecisionRule) parsedElement;
                decisionRule.setRuleNumber(elementCounter);
                dt.addRule(decisionRule);
                elementCounter++;
            });
        } else if (parsedElement instanceof ItemDefinition) {
            model.addItemDefinition((ItemDefinition) parsedElement);
        } else if (parsedElement instanceof InputData) {
            InputData inputData = (InputData) parsedElement;
            // TODO: handle inputData as href in DecisionService (same tag)
            if (inputData.getVariable() != null) {
                model.addInputData(inputData);
            }
        } else if (parsedElement instanceof InformationRequirement) {
            InformationRequirement informationRequirement = (InformationRequirement) parsedElement;
            if (informationRequirement.getRequiredDecision() != null) {
                decision.addRequiredDecision(informationRequirement);
            } else if (informationRequirement.getRequiredInput() != null) {
                decision.addRequiredInput(informationRequirement);
            }
        } else if (parsedElement instanceof AuthorityRequirement) {
            AuthorityRequirement authorityRequirement = (AuthorityRequirement) parsedElement;
            decision.addAuthorityRequirement(authorityRequirement);
        } else if (parsedElement instanceof InformationItem) {
            if (decision.getVariable() == null) {
                decision.setVariable((InformationItem) parsedElement);
            }
        }  else if (parsedElement instanceof DecisionService) {
            DecisionService decisionService = (DecisionService) parsedElement;
            decisionService.setDmnDefinition(model);
            model.addDecisionService(decisionService);
        }

    }

    public void convertToXML(XMLStreamWriter xtw, DmnElement baseElement, DmnDefinition model) throws Exception {
        xtw.writeStartElement(getXMLElementName());
        writeDefaultAttribute(ATTRIBUTE_ID, baseElement.getId(), xtw);

        writeAdditionalAttributes(baseElement, model, xtw);

        writeAdditionalChildElements(baseElement, model, xtw);

        xtw.writeEndElement();
    }

    protected abstract Class<? extends DmnElement> getDmnElementType();

    protected abstract DmnElement convertXMLToElement(XMLStreamReader xtr, DmnDefinition model, Decision decision) throws Exception;

    protected abstract String getXMLElementName();

    protected abstract void writeAdditionalAttributes(DmnElement element, DmnDefinition model, XMLStreamWriter xtw) throws Exception;

    protected abstract void writeAdditionalChildElements(DmnElement element, DmnDefinition model, XMLStreamWriter xtw) throws Exception;

    // To BpmnModel converter convenience methods

    protected void parseChildElements(String elementName, DmnElement parentElement, Decision decision, XMLStreamReader xtr) throws Exception {
        parseChildElements(elementName, parentElement, null, decision, xtr);
    }

    protected void parseChildElements(String elementName, DmnElement parentElement, Map<String, BaseChildElementParser> additionalParsers, Decision decision,
        XMLStreamReader xtr) throws Exception {

        Map<String, BaseChildElementParser> childParsers = new HashMap<>();
        if (additionalParsers != null) {
            childParsers.putAll(additionalParsers);
        }
        DmnXMLUtil.parseChildElements(elementName, parentElement, xtr, childParsers, decision);
    }

    // To XML converter convenience methods

    protected void writeDefaultAttribute(String attributeName, String value, XMLStreamWriter xtw) throws Exception {
        DmnXMLUtil.writeDefaultAttribute(attributeName, value, xtw);
    }

    protected void writeQualifiedAttribute(String attributeName, String value, XMLStreamWriter xtw) throws Exception {
        DmnXMLUtil.writeQualifiedAttribute(attributeName, value, xtw);
    }

    protected void initializeElementCounter() {
        elementCounter = 1;
    }
}
