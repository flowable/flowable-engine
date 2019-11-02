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

import static org.flowable.cmmn.converter.CmmnXmlConstants.FLOWABLE_EXTENSIONS_NAMESPACE;

import javax.xml.stream.XMLStreamReader;

import org.flowable.cmmn.model.CmmnElement;
import org.flowable.cmmn.model.Criterion;

/**
 * @author Joram Barrez
 */
public class ExitCriterionXmlConverter extends CriterionXmlConverter {
    
    @Override
    public String getXMLElementName() {
        return CmmnXmlConstants.ELEMENT_EXIT_CRITERION;
    }
    
    @Override
    public boolean hasChildElements() {
        return true;
    }

    @Override
    protected CmmnElement convert(XMLStreamReader xtr, ConversionHelper conversionHelper) {
        Criterion exitCriterion = (Criterion) super.convert(xtr, conversionHelper);
        exitCriterion.setExitCriterion(true);

        // parse exit-sentry specific additional properties
        exitCriterion.setExitType(xtr.getAttributeValue(FLOWABLE_EXTENSIONS_NAMESPACE, CmmnXmlConstants.ATTRIBUTE_EXIT_TYPE));
        exitCriterion.setExitEventType(xtr.getAttributeValue(FLOWABLE_EXTENSIONS_NAMESPACE, CmmnXmlConstants.ATTRIBUTE_EXIT_EVENT_TYPE));

        conversionHelper.addExitCriteriaToCurrentElement(exitCriterion);
        return exitCriterion;
    }
    
}