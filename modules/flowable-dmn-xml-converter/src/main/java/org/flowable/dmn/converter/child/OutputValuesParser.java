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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.StringTokenizer;
import java.util.stream.Collectors;

import javax.xml.stream.XMLStreamReader;

import org.apache.commons.lang3.StringUtils;
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

                    outputValues.setTextValues(splitAndFormatOutputValues(outputValuesText));
                } else if (xtr.isEndElement() && getElementName().equalsIgnoreCase(xtr.getLocalName())) {
                    readyWithOutputValues = true;
                }
            }
        } catch (Exception e) {
            LOGGER.warn("Error parsing output values", e);
        }

        clause.setOutputValues(outputValues);
    }

    public List<Object> splitAndFormatOutputValues(String outputValuesText) {
        if (StringUtils.isEmpty(outputValuesText)) {
            return Collections.emptyList();
        }

        List<Object> result = new ArrayList<>();
        int start = 0;
        int subStart, subEnd;
        boolean inQuotes = false;
        for (int current = 0; current < outputValuesText.length(); current++) {
            if (outputValuesText.charAt(current) == '\"') {
                inQuotes = !inQuotes;
            } else if (outputValuesText.charAt(current) == ',' && !inQuotes) {
                subStart = getSubStringStartPos(start, outputValuesText);
                subEnd = getSubStringEndPos(current, outputValuesText);

                result.add(outputValuesText.substring(subStart, subEnd));

                start = current + 1;
                if (outputValuesText.charAt(start) == ' ') {
                    start++;
                }
            }
        }

        subStart = getSubStringStartPos(start, outputValuesText);
        subEnd = getSubStringEndPos(outputValuesText.length(), outputValuesText);
        result.add(outputValuesText.substring(subStart, subEnd));

        return result;
    }

    protected int getSubStringStartPos(int initialStart, String searchString) {
        if (searchString.charAt(initialStart) == '\"') {
            return initialStart + 1;
        }
        return initialStart;
    }

    protected int getSubStringEndPos(int initialEnd, String searchString) {
        if (searchString.charAt(initialEnd - 1) == '\"') {
            return initialEnd - 1;
        }
        return initialEnd;
    }

    protected List<Object> formatTokens(List<String> splitTokens) {
        // remove start quote
        // remove end quote
        // remove space start quote
        return splitTokens.stream()
                .map(elem -> elem.replaceAll("^\"|^ \"|\"$", ""))
                .collect(Collectors.toList());
    }
}
