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
import org.flowable.cmmn.model.CmmnElement;
import org.flowable.cmmn.model.DecisionTask;
import org.flowable.cmmn.model.FieldExtension;
import org.flowable.cmmn.model.ServiceTask;
import org.flowable.cmmn.model.TaskWithFieldExtensions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Joram Barrez
 */
public class FieldExtensionXmlConverter extends BaseCmmnXmlConverter {

    private static final Logger LOGGER = LoggerFactory.getLogger(FieldExtensionXmlConverter.class);

    @Override
    public String getXMLElementName() {
        return CmmnXmlConstants.ELEMENT_FIELD;
    }

    @Override
    public boolean hasChildElements() {
        return false;
    }

    @Override
    protected CmmnElement convert(XMLStreamReader xtr, ConversionHelper conversionHelper) {
        CmmnElement cmmnElement = conversionHelper.getCurrentCmmnElement();
        if (!(cmmnElement instanceof ServiceTask || cmmnElement instanceof DecisionTask)) {
            return null;
        }

        TaskWithFieldExtensions serviceTask = (TaskWithFieldExtensions) cmmnElement;

        FieldExtension extension = new FieldExtension();
        extension.setFieldName(xtr.getAttributeValue(null, CmmnXmlConstants.ATTRIBUTE_NAME));

        String stringValueAttribute = xtr.getAttributeValue(null, CmmnXmlConstants.ATTRIBUTE_FIELD_STRING);
        String expressionAttribute = xtr.getAttributeValue(null, CmmnXmlConstants.ATTRIBUTE_FIELD_EXPRESSION);
        if (StringUtils.isNotEmpty(stringValueAttribute)) {
            extension.setStringValue(stringValueAttribute);

        } else if (StringUtils.isNotEmpty(expressionAttribute)) {
            extension.setExpression(expressionAttribute);

        } else {
            boolean readyWithFieldExtension = false;
            try {
                while (!readyWithFieldExtension && xtr.hasNext()) {
                    xtr.next();
                    if (xtr.isStartElement() && CmmnXmlConstants.ELEMENT_FIELD_STRING.equalsIgnoreCase(xtr.getLocalName())) {
                        extension.setStringValue(xtr.getElementText().trim());

                    } else if (xtr.isStartElement() && CmmnXmlConstants.ELEMENT_FIELD_EXPRESSION.equalsIgnoreCase(xtr.getLocalName())) {
                        extension.setExpression(xtr.getElementText().trim());

                    } else if (xtr.isEndElement() && getXMLElementName().equalsIgnoreCase(xtr.getLocalName())) {
                        readyWithFieldExtension = true;
                    }
                }
            } catch (Exception e) {
                LOGGER.warn("Error parsing field extension child elements", e);
            }
        }

        serviceTask.getFieldExtensions().add(extension);

        return null;
    }

}
