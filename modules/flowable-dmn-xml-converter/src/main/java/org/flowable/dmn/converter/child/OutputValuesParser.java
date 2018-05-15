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

import org.apache.commons.lang3.StringUtils;
import org.flowable.dmn.model.DecisionTable;
import org.flowable.dmn.model.DmnElement;
import org.flowable.dmn.model.OutputClause;
import org.flowable.dmn.model.UnaryTests;

import javax.xml.stream.XMLStreamReader;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * @author Yvo Swillens
 */
public class OutputValuesParser extends BaseChildElementParser {

    @Override
    public String getElementName() {
        return ELEMENT_OUTPUT_VALUES;
    }

    @Override
    public void parseChildElement(XMLStreamReader xtr, DmnElement parentElement, DecisionTable decisionTable) throws Exception {
        if (!(parentElement instanceof OutputClause))
            return;

        OutputClause clause = (OutputClause) parentElement;
        UnaryTests outputValues = new UnaryTests();

        boolean readyWithOutputValues = false;
        try {
            while (!readyWithOutputValues && xtr.hasNext()) {
                xtr.next();
                if (xtr.isStartElement() && ELEMENT_TEXT.equalsIgnoreCase(xtr.getLocalName())) {
                    String outputValuesText = xtr.getElementText();

                    if (StringUtils.isNotEmpty(outputValuesText)) {
                        String[] outputValuesSplit = outputValuesText.replaceAll("^\"", "").split("\"?(,|$)(?=(([^\"]*\"){2})*[^\"]*$) *\"?");
                        outputValues.setTextValues(new ArrayList<>(Arrays.asList(outputValuesSplit)));
                    }


                } else if (xtr.isEndElement() && getElementName().equalsIgnoreCase(xtr.getLocalName())) {
                    readyWithOutputValues = true;
                }
            }
        } catch (Exception e) {
            LOGGER.warn("Error parsing output values", e);
        }

        clause.setOutputValues(outputValues);
    }
}
