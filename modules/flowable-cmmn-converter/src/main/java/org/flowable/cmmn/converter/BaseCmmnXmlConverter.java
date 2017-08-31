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

import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamReader;

import org.flowable.cmmn.model.BaseElement;
import org.flowable.cmmn.model.CmmnElement;

/**
 * @author Joram Barrez
 */
public abstract class BaseCmmnXmlConverter {
    
    public abstract String getXMLElementName();
    
    public abstract boolean isCmmnElement();
    
    public BaseElement convertToCmmnModel(XMLStreamReader xtr, ConversionHelper conversionHelper) {
        BaseElement baseElement = convert(xtr, conversionHelper);
        if (baseElement != null) {

            baseElement.setId(xtr.getAttributeValue(null, CmmnXmlConstants.ATTRIBUTE_ID));
            Location location = xtr.getLocation();
            baseElement.setXmlRowNumber(location.getLineNumber());
            baseElement.setXmlRowNumber(location.getColumnNumber());
            
            if (baseElement instanceof CmmnElement) {
                CmmnElement cmmnElement = (CmmnElement) baseElement;
                conversionHelper.setCurrentCmmnElement(cmmnElement);
            }
            
        }
        return baseElement;
    }
    
    protected abstract BaseElement convert(XMLStreamReader xtr, ConversionHelper conversionHelper);
    
    protected void elementEnd(XMLStreamReader xtr, ConversionHelper conversionHelper) {
        if (isCmmnElement()) {
            conversionHelper.removeCurrentCmmnElement();
        }
    }
    
}
