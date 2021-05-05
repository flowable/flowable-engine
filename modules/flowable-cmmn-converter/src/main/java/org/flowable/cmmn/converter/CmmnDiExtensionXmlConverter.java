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

import org.flowable.cmmn.converter.exception.XMLException;
import org.flowable.cmmn.model.BaseElement;
import org.flowable.cmmn.model.CmmnDiEdge;
import org.flowable.cmmn.model.GraphicInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Tijs Rademakers
 */
public class CmmnDiExtensionXmlConverter extends BaseCmmnXmlConverter {
    
    protected static final Logger LOGGER = LoggerFactory.getLogger(CmmnDiExtensionXmlConverter.class);
    
    @Override
    public String getXMLElementName() {
        return CmmnXmlConstants.ELEMENT_DI_EXTENSION;
    }
    
    @Override
    public boolean hasChildElements() {
        return false;
    }

    @Override
    protected BaseElement convert(XMLStreamReader xtr, ConversionHelper conversionHelper) {
        CmmnDiEdge edgeInfo = conversionHelper.getCurrentDiEdge();
        if (edgeInfo == null) {
            return null;
        }
        
        boolean readyWithChildElements = false;
        try {

            while (!readyWithChildElements && xtr.hasNext()) {
                xtr.next();
                if (xtr.isStartElement()) {
                    if (CmmnXmlConstants.ELEMENT_DI_DOCKER.equals(xtr.getLocalName())) {
                        String type = xtr.getAttributeValue(null, CmmnXmlConstants.ATTRIBUTE_TYPE);
                        if ("source".equals(type) || "target".equals(type)) {
                            GraphicInfo graphicInfo = new GraphicInfo();
                            graphicInfo.setX(Double.valueOf(xtr.getAttributeValue(null, CmmnXmlConstants.ATTRIBUTE_DI_X)));
                            graphicInfo.setY(Double.valueOf(xtr.getAttributeValue(null, CmmnXmlConstants.ATTRIBUTE_DI_Y)));
                            if ("source".equals(type)) {
                                edgeInfo.setSourceDockerInfo(graphicInfo);
                            } else {
                                edgeInfo.setTargetDockerInfo(graphicInfo);
                            }
                        }
                    }

                } else if (xtr.isEndElement()) {
                    if (CmmnXmlConstants.ELEMENT_DI_EXTENSION.equalsIgnoreCase(xtr.getLocalName())) {
                        readyWithChildElements = true;
                    }
                }

            }
        } catch (Exception ex) {
            LOGGER.error("Error processing CMMN document", ex);
            throw new XMLException("Error processing CMMN document", ex);
        }

        return null;
    }
    
}