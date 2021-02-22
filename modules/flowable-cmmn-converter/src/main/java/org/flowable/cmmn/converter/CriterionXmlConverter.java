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

import org.flowable.cmmn.model.CmmnElement;
import org.flowable.cmmn.model.Criterion;

/**
 * @author Joram Barrez
 */
public abstract class CriterionXmlConverter extends CaseElementXmlConverter {
    
    @Override
    protected CmmnElement convert(XMLStreamReader xtr, ConversionHelper conversionHelper) {
        Criterion criterion = new Criterion();
        // Even though the id is set in the CaseElementXmlConverter it is used in the implementations
        // of this converter. Therefore we set it here as well.
        criterion.setId(xtr.getAttributeValue(null, CmmnXmlConstants.ATTRIBUTE_ID));
        criterion.setName(xtr.getAttributeValue(null, CmmnXmlConstants.ATTRIBUTE_NAME));
        criterion.setSentryRef(xtr.getAttributeValue(null, CmmnXmlConstants.ATTRIBUTE_SENTRY_REF));
        return criterion;
    }
    
}