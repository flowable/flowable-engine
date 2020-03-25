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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.xml.stream.XMLStreamReader;

import org.apache.commons.lang3.StringUtils;
import org.flowable.cmmn.model.CmmnElement;
import org.flowable.cmmn.model.CmmnModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Joram Barrez
 */
public class DefinitionsXmlConverter extends BaseCmmnXmlConverter {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(DefinitionsXmlConverter.class);
    
    private static final String XSD_DATETIME_FORMAT = "yyyy-MM-dd'T'HH':'mm':'ss";
    
    @Override
    public String getXMLElementName() {
        return CmmnXmlConstants.ELEMENT_DEFINITIONS;
    }
    
    @Override
    public boolean hasChildElements() {
        return false;
    }
    
    @Override
    protected CmmnElement convert(XMLStreamReader xtr, ConversionHelper conversionHelper) {
        CmmnModel model = conversionHelper.getCmmnModel();
        model.setId(xtr.getAttributeValue(null, CmmnXmlConstants.ATTRIBUTE_ID));
        model.setName(xtr.getAttributeValue(null, CmmnXmlConstants.ATTRIBUTE_NAME));
        model.setExpressionLanguage(xtr.getAttributeValue(null, CmmnXmlConstants.ATTRIBUTE_EXPRESSION_LANGUAGE));
        model.setExporter(xtr.getAttributeValue(null, CmmnXmlConstants.ATTRIBUTE_EXPORTER));
        model.setExporterVersion(xtr.getAttributeValue(null, CmmnXmlConstants.ATTRIBUTE_EXPORTER_VERSION));
        model.setAuthor(xtr.getAttributeValue(null, CmmnXmlConstants.ATTRIBUTE_AUTHOR));
        model.setTargetNamespace(xtr.getAttributeValue(null, CmmnXmlConstants.ATTRIBUTE_TARGET_NAMESPACE));
        
        String creationDateString = xtr.getAttributeValue(null, CmmnXmlConstants.ATTRIBUTE_CREATION_DATE);
        if (StringUtils.isNotEmpty(creationDateString)) {
            try {
                Date creationDate = new SimpleDateFormat(XSD_DATETIME_FORMAT).parse(creationDateString);
                model.setCreationDate(creationDate);
            } catch (ParseException e) {
                LOGGER.warn("Ignoring creationDate attribute: invalid date format", e);
            }
        }

        for (int i = 0; i < xtr.getNamespaceCount(); i++) {
            String prefix = xtr.getNamespacePrefix(i);
            if (StringUtils.isNotEmpty(prefix)) {
                model.addNamespace(prefix, xtr.getNamespaceURI(i));
            }
        }
        
        return null;
    }
    
    
    @Override
    protected void elementEnd(XMLStreamReader xtr, ConversionHelper conversionHelper) {
        
    }
}