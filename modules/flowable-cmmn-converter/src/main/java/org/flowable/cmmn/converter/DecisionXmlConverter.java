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
package org.flowable.cmmn.converter;

import org.flowable.cmmn.model.CmmnElement;
import org.flowable.cmmn.model.Decision;

import javax.xml.stream.XMLStreamReader;

/**
 * @author martin.grofcik
 */
public class DecisionXmlConverter extends BaseCmmnXmlConverter {

    @Override
    public String getXMLElementName() {
        return CmmnXmlConstants.ELEMENT_DECISION;
    }

    @Override
    public boolean hasChildElements() {
        return true;
    }

    @Override
    protected CmmnElement convert(XMLStreamReader xtr, ConversionHelper conversionHelper) {
        Decision decision = new Decision();
        decision.setName(xtr.getAttributeValue(null, CmmnXmlConstants.ATTRIBUTE_NAME));
        decision.setImplementationType(xtr.getAttributeValue(null, CmmnXmlConstants.ATTRIBUTE_IMPLEMENTATION_TYPE));
        decision.setExternalRef(xtr.getAttributeValue(null, CmmnXmlConstants.ATTRIBUTE_EXTERNAL_REF));
        conversionHelper.getCmmnModel().addDecision(decision);
        return decision;
    }

}
