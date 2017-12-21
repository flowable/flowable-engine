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

import org.flowable.dmn.model.DecisionRule;
import org.flowable.dmn.model.DecisionTable;
import org.flowable.dmn.model.DmnElement;
import org.flowable.dmn.model.LiteralExpression;
import org.flowable.dmn.model.OutputClause;
import org.flowable.dmn.model.RuleOutputClauseContainer;

/**
 * @author Tijs Rademakers
 * @author Yvo Swillens
 */
public class OutputEntryParser extends BaseChildElementParser {

    @Override
    public String getElementName() {
        return ELEMENT_OUTPUT_ENTRY;
    }

    @Override
    public void parseChildElement(XMLStreamReader xtr, DmnElement parentElement, DecisionTable decisionTable) throws Exception {
        if (!(parentElement instanceof DecisionRule))
            return;

        DecisionRule rule = (DecisionRule) parentElement;
        LiteralExpression outputEntry = new LiteralExpression();
        outputEntry.setId(xtr.getAttributeValue(null, ATTRIBUTE_ID));

        boolean readyWithOutputEntry = false;
        try {
            while (!readyWithOutputEntry && xtr.hasNext()) {
                xtr.next();
                if (xtr.isStartElement() && ELEMENT_TEXT.equalsIgnoreCase(xtr.getLocalName())) {
                    outputEntry.setText(xtr.getElementText());

                } else if (xtr.isEndElement() && getElementName().equalsIgnoreCase(xtr.getLocalName())) {
                    readyWithOutputEntry = true;
                }
            }
        } catch (Exception e) {
            LOGGER.warn("Error parsing output entry", e);
        }

        // determine corresponding output clause based on position
        OutputClause outputClause = null;
        if (decisionTable.getOutputs() != null) {
            if (decisionTable.getOutputs().get(rule.getOutputEntries().size()) != null) {
                outputClause = decisionTable.getOutputs().get(rule.getOutputEntries().size());
            }
        }

        if (outputClause == null) {
            LOGGER.warn("Error determine output clause for position: {}", decisionTable.getOutputs());
        }

        RuleOutputClauseContainer outputContainer = new RuleOutputClauseContainer();
        outputContainer.setOutputClause(outputClause);
        outputContainer.setOutputEntry(outputEntry);

        if (outputEntry.getText() != null) {
            rule.addOutputEntry(outputContainer);
        }
    }
}
