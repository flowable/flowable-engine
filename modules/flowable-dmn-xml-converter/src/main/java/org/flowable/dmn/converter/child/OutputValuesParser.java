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

import org.flowable.dmn.model.Decision;
import org.flowable.dmn.model.DmnElement;
import org.flowable.dmn.model.OutputClause;
import org.flowable.dmn.model.UnaryTests;

/**
 * @author Yvo Swillens
 */
public class OutputValuesParser extends BaseChildElementParser {

    @Override
    public String getElementName() {
        return ELEMENT_OUTPUT_VALUES;
    }

    @Override
    public void parseChildElement(XMLStreamReader xtr, DmnElement parentElement, Decision decision) throws Exception {
        if (!(parentElement instanceof OutputClause clause)) {
            return;
        }

        UnaryTests outputValues = new UnaryTests();

        boolean readyWithOutputValues = false;
        try {
            while (!readyWithOutputValues && xtr.hasNext()) {
                xtr.next();
                if (xtr.isStartElement() && ELEMENT_TEXT.equalsIgnoreCase(xtr.getLocalName())) {
                    String outputValuesText = xtr.getElementText();
                    outputValues.setText(outputValuesText);

                    outputValues.setTextValues(splitAndFormatInputOutputValues(outputValuesText));
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
