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

import javax.xml.stream.XMLStreamReader;

import org.apache.commons.lang3.StringUtils;
import org.flowable.dmn.model.Decision;
import org.flowable.dmn.model.DmnElement;
import org.flowable.dmn.xml.constants.DmnXMLConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Tijs Rademakers
 * @author Yvo Swillens
 */
public abstract class BaseChildElementParser implements DmnXMLConstants {

    protected static final Logger LOGGER = LoggerFactory.getLogger(BaseChildElementParser.class);

    public abstract String getElementName();

    public abstract void parseChildElement(XMLStreamReader xtr, DmnElement parentElement, Decision decision) throws Exception;

    protected void parseChildElements(XMLStreamReader xtr, DmnElement parentElement, Decision decision, BaseChildElementParser parser) throws Exception {
        boolean readyWithChildElements = false;
        while (!readyWithChildElements && xtr.hasNext()) {
            xtr.next();
            if (xtr.isStartElement()) {
                if (parser.getElementName().equals(xtr.getLocalName())) {
                    parser.parseChildElement(xtr, parentElement, decision);
                }

            } else if (xtr.isEndElement() && getElementName().equalsIgnoreCase(xtr.getLocalName())) {
                readyWithChildElements = true;
            }
        }
    }

    public boolean accepts(DmnElement element) {
        return element != null;
    }

    public List<Object> splitAndFormatInputOutputValues(String valuesText) {
        if (StringUtils.isEmpty(valuesText)) {
            return Collections.emptyList();
        }

        List<Object> result = new ArrayList<>();
        int start = 0;
        int subStart, subEnd;
        boolean inQuotes = false;
        for (int current = 0; current < valuesText.length(); current++) {
            if (valuesText.charAt(current) == '\"') {
                inQuotes = !inQuotes;
            } else if (valuesText.charAt(current) == ',' && !inQuotes) {
                subStart = getSubStringStartPos(start, valuesText);
                subEnd = getSubStringEndPos(current, valuesText);

                result.add(valuesText.substring(subStart, subEnd));

                start = current + 1;
                if (valuesText.charAt(start) == ' ') {
                    start++;
                }
            }
        }

        subStart = getSubStringStartPos(start, valuesText);
        subEnd = getSubStringEndPos(valuesText.length(), valuesText);
        result.add(valuesText.substring(subStart, subEnd));

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
}
