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

import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import org.flowable.dmn.converter.child.BaseChildElementParser;
import org.flowable.dmn.converter.util.DmnXMLUtil;
import org.flowable.dmn.model.DecisionRule;
import org.flowable.dmn.model.DecisionTable;
import org.flowable.dmn.model.DmnDefinition;
import org.flowable.dmn.model.DmnElement;
import org.flowable.dmn.model.InputClause;
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

    public void convertToDmnModel(XMLStreamReader xtr, DmnDefinition model, DecisionTable decisionTable) throws Exception {

        //String elementId = xtr.getAttributeValue(null, ATTRIBUTE_ID);
        //String elementName = xtr.getAttributeValue(null, ATTRIBUTE_NAME);

        DmnElement parsedElement = convertXMLToElement(xtr, model, decisionTable);
        // parsedElement.setId(elementId);
        // parsedElement.setName(elementName);

        if (parsedElement instanceof InputClause) {
            InputClause inputClause = (InputClause) parsedElement;
            inputClause.setInputNumber(elementCounter);
            decisionTable.addInput(inputClause);

            elementCounter++;
        } else if (parsedElement instanceof OutputClause) {
            OutputClause outputClause = (OutputClause) parsedElement;
            outputClause.setOutputNumber(elementCounter);
            decisionTable.addOutput(outputClause);

            elementCounter++;
        } else if (parsedElement instanceof DecisionRule) {
            DecisionRule decisionRule = (DecisionRule) parsedElement;
            decisionRule.setRuleNumber(elementCounter);
            decisionTable.addRule(decisionRule);

            elementCounter++;
        } else if (parsedElement instanceof ItemDefinition) {
            model.addItemDefinition((ItemDefinition) parsedElement);
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

    protected abstract DmnElement convertXMLToElement(XMLStreamReader xtr, DmnDefinition model, DecisionTable decisionTable) throws Exception;

    protected abstract String getXMLElementName();

    protected abstract void writeAdditionalAttributes(DmnElement element, DmnDefinition model, XMLStreamWriter xtw) throws Exception;

    protected abstract void writeAdditionalChildElements(DmnElement element, DmnDefinition model, XMLStreamWriter xtw) throws Exception;

    // To BpmnModel converter convenience methods

    protected void parseChildElements(String elementName, DmnElement parentElement, DecisionTable decisionTable, XMLStreamReader xtr) throws Exception {
        parseChildElements(elementName, parentElement, null, decisionTable, xtr);
    }

    protected void parseChildElements(String elementName, DmnElement parentElement, Map<String, BaseChildElementParser> additionalParsers, DecisionTable decisionTable, XMLStreamReader xtr) throws Exception {

        Map<String, BaseChildElementParser> childParsers = new HashMap<>();
        if (additionalParsers != null) {
            childParsers.putAll(additionalParsers);
        }
        DmnXMLUtil.parseChildElements(elementName, parentElement, xtr, childParsers, decisionTable);
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
