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

import javax.xml.stream.XMLStreamReader;

import org.flowable.cmmn.model.BaseElement;
import org.flowable.cmmn.model.CaseElement;
import org.flowable.cmmn.model.Stage;

public abstract class CaseElementXmlConverter extends BaseCmmnXmlConverter {
    
    public abstract String getXMLElementName();
    
    public BaseElement convertToCmmnModel(XMLStreamReader xtr, ConversionHelper conversionHelper) {
        CaseElement caseElement = (CaseElement) super.convertToCmmnModel(xtr, conversionHelper);
        if (caseElement != null) {
            conversionHelper.addCaseElement(caseElement);
            if (!(caseElement instanceof Stage)) {
                caseElement.setParent(conversionHelper.getCurrentPlanFragment());
            } 
        }
        return caseElement;
    }
    
}
