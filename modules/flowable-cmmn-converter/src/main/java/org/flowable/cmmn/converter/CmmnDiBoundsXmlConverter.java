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
import org.flowable.cmmn.model.GraphicInfo;

/**
 * @author Tijs Rademakers
 */
public class CmmnDiBoundsXmlConverter extends BaseCmmnXmlConverter {
    
    @Override
    public String getXMLElementName() {
        return CmmnXmlConstants.ELEMENT_DI_BOUNDS;
    }
    
    @Override
    public boolean hasChildElements() {
        return false;
    }

    @Override
    protected BaseElement convert(XMLStreamReader xtr, ConversionHelper conversionHelper) {
        // If this Bounds element is in a Label element, there will be a currentLabelGraphicInfo available
        GraphicInfo graphicInfo = null;
        if (conversionHelper.getCurrentLabelGraphicInfo() != null) {
            graphicInfo = conversionHelper.getCurrentLabelGraphicInfo();

            if (conversionHelper.getCurrentDiEdge() != null) {
                conversionHelper.getCurrentDiEdge().setLabelGraphicInfo(graphicInfo);
            } else if (conversionHelper.getCurrentDiShape() != null) {
                conversionHelper.getCurrentDiShape().setLabelGraphicInfo(graphicInfo);
            }
        } else {
            graphicInfo = new GraphicInfo();
            conversionHelper.getCurrentDiShape().setGraphicInfo(graphicInfo);
        }

        graphicInfo.setX(Double.valueOf(xtr.getAttributeValue(null, CmmnXmlConstants.ATTRIBUTE_DI_X)));
        graphicInfo.setY(Double.valueOf(xtr.getAttributeValue(null, CmmnXmlConstants.ATTRIBUTE_DI_Y)));
        graphicInfo.setWidth(Double.valueOf(xtr.getAttributeValue(null, CmmnXmlConstants.ATTRIBUTE_DI_WIDTH)));
        graphicInfo.setHeight(Double.valueOf(xtr.getAttributeValue(null, CmmnXmlConstants.ATTRIBUTE_DI_HEIGHT)));
        
        return graphicInfo;
    }
    
}