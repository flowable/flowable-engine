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

import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import org.flowable.dmn.model.DmnDefinition;
import org.flowable.dmn.model.DmnElement;
import org.flowable.dmn.model.OutputClause;

/**
 * @author Yvo Swillens
 */
public class OutputClauseXMLConverter extends BaseDmnXMLConverter {

    @Override
    protected String getXMLElementName() {
        return ELEMENT_OUTPUT_CLAUSE;
    }

    @Override
    protected DmnElement convertXMLToElement(XMLStreamReader xtr, ConversionHelper conversionHelper) throws Exception {
        OutputClause clause = new OutputClause();
        clause.setId(xtr.getAttributeValue(null, ATTRIBUTE_ID));
        clause.setLabel(xtr.getAttributeValue(null, ATTRIBUTE_LABEL));
        clause.setName(xtr.getAttributeValue(null, ATTRIBUTE_NAME));
        clause.setTypeRef(xtr.getAttributeValue(null, ATTRIBUTE_TYPE_REF));
        parseChildElements(getXMLElementName(), clause, conversionHelper.getCurrentDecision(), xtr);
        return clause;
    }

    @Override
    protected void writeAdditionalAttributes(DmnElement element, DmnDefinition model, XMLStreamWriter xtw) throws Exception {

    }

    @Override
    protected void writeAdditionalChildElements(DmnElement element, DmnDefinition model, XMLStreamWriter xtw) throws Exception {

    }

}
