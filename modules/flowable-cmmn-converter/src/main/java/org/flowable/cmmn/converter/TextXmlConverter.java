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

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.flowable.cmmn.model.CmmnElement;
import org.flowable.cmmn.model.TextAnnotation;
import org.flowable.common.engine.api.FlowableException;

/**
 * @author Joram Barrez
 */
public class TextXmlConverter extends BaseCmmnXmlConverter {
    
    @Override
    public String getXMLElementName() {
        return CmmnXmlConstants.ELEMENT_TEXT;
    }
    
    @Override
    public boolean hasChildElements() {
        return false;
    }

    @Override
    protected CmmnElement convert(XMLStreamReader xtr, ConversionHelper conversionHelper) {
        CmmnElement currentCmmnElement = conversionHelper.getCurrentCmmnElement();
        try {
            if (currentCmmnElement instanceof TextAnnotation textAnnotation) {
                textAnnotation.setText(xtr.getElementText());
            }
        } catch (XMLStreamException e) {
            throw new FlowableException("Error converting text annotation", e);
        }

        return null;
    }
    
}
