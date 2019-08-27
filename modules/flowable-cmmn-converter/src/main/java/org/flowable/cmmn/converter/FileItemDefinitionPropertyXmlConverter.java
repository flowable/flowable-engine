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

import org.apache.commons.lang3.StringUtils;
import org.flowable.cmmn.model.CaseFileItemPropertyDefinition;
import org.flowable.cmmn.model.CmmnElement;

/**
 * @author Joram Barrez
 */
public class FileItemDefinitionPropertyXmlConverter extends BaseCmmnXmlConverter {

    @Override
    public String getXMLElementName() {
        return CmmnXmlConstants.ELEMENT_FILE_DEFINITION_PROPERTY;
    }

    @Override
    public boolean hasChildElements() {
        return false;
    }

    @Override
    protected CmmnElement convert(XMLStreamReader xtr, ConversionHelper conversionHelper) {
        CaseFileItemPropertyDefinition propertyDefinition = new CaseFileItemPropertyDefinition();

        String name = xtr.getAttributeValue(null, CmmnXmlConstants.ATTRIBUTE_NAME);
        if (StringUtils.isNotEmpty(name)) {
            propertyDefinition.setName(name);
        }

        String type = xtr.getAttributeValue(null, CmmnXmlConstants.ATTRIBUTE_TYPE);
        if (StringUtils.isNotEmpty(type)) {
            propertyDefinition.setType(type);
        }

        conversionHelper.getCurrentFileItemDefinition().addPropertyDefinition(propertyDefinition);

        return propertyDefinition;
    }

}
