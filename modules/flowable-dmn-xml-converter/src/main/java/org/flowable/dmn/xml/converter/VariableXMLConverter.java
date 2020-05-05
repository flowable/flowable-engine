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
import org.flowable.dmn.model.InformationItem;

public class VariableXMLConverter extends BaseDmnXMLConverter {

    @Override
    protected String getXMLElementName() {
        return ELEMENT_VARIABLE;
    }

    @Override
    protected DmnElement convertXMLToElement(XMLStreamReader xtr, ConversionHelper conversionHelper) throws Exception {
        InformationItem variable = new InformationItem();
        variable.setId(xtr.getAttributeValue(null, ATTRIBUTE_ID));
        variable.setName(xtr.getAttributeValue(null, ATTRIBUTE_NAME));
        variable.setTypeRef(xtr.getAttributeValue(null, ATTRIBUTE_TYPE_REF));
        return variable;
    }

    @Override
    protected void writeAdditionalAttributes(DmnElement element, DmnDefinition model, XMLStreamWriter xtw) throws Exception {

    }

    @Override
    protected void writeAdditionalChildElements(DmnElement element, DmnDefinition model, XMLStreamWriter xtw) throws Exception {

    }
}
