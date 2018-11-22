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
package org.flowable.cmmn.converter.export;

import java.util.List;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.apache.commons.lang3.StringUtils;
import org.flowable.cmmn.converter.CmmnXmlConstants;
import org.flowable.cmmn.model.FieldExtension;

/**
 * @author Joram Barrez
 */
public class FieldExport {

    public static boolean writeFieldExtensions(List<FieldExtension> fieldExtensions, boolean didWriteExtensionElement, XMLStreamWriter xtw) throws
        XMLStreamException {
        if (fieldExtensions.size() > 0) {
            if (!didWriteExtensionElement) {
                xtw.writeStartElement(CmmnXmlConstants.ELEMENT_EXTENSION_ELEMENTS);
                didWriteExtensionElement = true;
            }

            for (FieldExtension fieldExtension : fieldExtensions) {
                xtw.writeStartElement(CmmnXmlConstants.FLOWABLE_EXTENSIONS_PREFIX, CmmnXmlConstants.ELEMENT_FIELD, CmmnXmlConstants.FLOWABLE_EXTENSIONS_NAMESPACE);
                xtw.writeAttribute(CmmnXmlConstants.ATTRIBUTE_NAME, fieldExtension.getFieldName());

                if (StringUtils.isNotEmpty(fieldExtension.getStringValue())) {
                    xtw.writeStartElement(CmmnXmlConstants.FLOWABLE_EXTENSIONS_PREFIX, CmmnXmlConstants.ELEMENT_FIELD_STRING, CmmnXmlConstants.FLOWABLE_EXTENSIONS_NAMESPACE);
                    xtw.writeCData(fieldExtension.getStringValue());
                } else {
                    xtw.writeStartElement(CmmnXmlConstants.FLOWABLE_EXTENSIONS_PREFIX, CmmnXmlConstants.ATTRIBUTE_FIELD_EXPRESSION, CmmnXmlConstants.FLOWABLE_EXTENSIONS_NAMESPACE);
                    xtw.writeCData(fieldExtension.getExpression());
                }
                xtw.writeEndElement();
                xtw.writeEndElement();
            }
        }

        return didWriteExtensionElement;
    }

}
