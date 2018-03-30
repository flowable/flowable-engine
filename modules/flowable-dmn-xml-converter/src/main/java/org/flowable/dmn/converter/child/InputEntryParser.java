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
package org.flowable.dmn.converter.child;

import javax.xml.stream.XMLStreamReader;

import org.flowable.dmn.converter.util.DmnXMLUtil;
import org.flowable.dmn.model.DecisionRule;
import org.flowable.dmn.model.DecisionTable;
import org.flowable.dmn.model.DmnElement;
import org.flowable.dmn.model.DmnExtensionElement;
import org.flowable.dmn.model.InputClause;
import org.flowable.dmn.model.RuleInputClauseContainer;
import org.flowable.dmn.model.UnaryTests;

/**
 * @author Tijs Rademakers
 * @author Yvo Swillens
 */
public class InputEntryParser extends BaseChildElementParser {

    @Override
    public String getElementName() {
        return ELEMENT_INPUT_ENTRY;
    }

    @Override
    public void parseChildElement(XMLStreamReader xtr, DmnElement parentElement, DecisionTable decisionTable) throws Exception {
        if (!(parentElement instanceof DecisionRule))
            return;

        DecisionRule rule = (DecisionRule) parentElement;
        UnaryTests inputEntry = new UnaryTests();

        inputEntry.setId(xtr.getAttributeValue(null, ATTRIBUTE_ID));

        boolean readyWithInputEntry = false;
        try {
            while (!readyWithInputEntry && xtr.hasNext()) {
                xtr.next();
                if (xtr.isStartElement() && ELEMENT_TEXT.equalsIgnoreCase(xtr.getLocalName())) {
                    inputEntry.setText(xtr.getElementText());
                } else if (xtr.isStartElement() && ELEMENT_EXTENSIONS.equals(xtr.getLocalName())) {
                    while (xtr.hasNext()) {
                        xtr.next();
                        if (xtr.isStartElement()) {
                            DmnExtensionElement extensionElement = DmnXMLUtil.parseExtensionElement(xtr);
                            inputEntry.addExtensionElement(extensionElement);
                        } else if (xtr.isEndElement()) {
                            if (ELEMENT_EXTENSIONS.equals(xtr.getLocalName())) {
                                break;
                            }
                        }
                    }
                } else if (xtr.isEndElement() && getElementName().equalsIgnoreCase(xtr.getLocalName())) {
                    readyWithInputEntry = true;
                }
            }
        } catch (Exception e) {
            LOGGER.warn("Error parsing input entry", e);
        }

        // determine corresponding input clause based on position
        InputClause inputClause = null;
        if (decisionTable.getInputs() != null) {
            if (decisionTable.getInputs().get(rule.getInputEntries().size()) != null) {
                inputClause = decisionTable.getInputs().get(rule.getInputEntries().size());
            }
        }

        if (inputClause == null) {
            LOGGER.warn("Error determine output clause for position: {}", decisionTable.getInputs());
        }

        RuleInputClauseContainer ruleInputClauseContainer = new RuleInputClauseContainer();
        ruleInputClauseContainer.setInputClause(inputClause);
        ruleInputClauseContainer.setInputEntry(inputEntry);

        rule.addInputEntry(ruleInputClauseContainer);
    }
}
