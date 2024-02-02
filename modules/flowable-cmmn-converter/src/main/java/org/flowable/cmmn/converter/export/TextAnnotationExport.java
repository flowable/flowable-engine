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

import javax.xml.stream.XMLStreamWriter;

import org.apache.commons.lang3.StringUtils;
import org.flowable.cmmn.converter.CmmnXmlConstants;
import org.flowable.cmmn.model.TextAnnotation;

/**
 * @author Joram Barrez
 */
public class TextAnnotationExport implements CmmnXmlConstants {
    
    public static void writeTextAnnotations(TextAnnotation textAnnotation, XMLStreamWriter xtw) throws Exception {
        xtw.writeStartElement(ELEMENT_TEXT_ANNOTATION);

        String id = textAnnotation.getId();
        if (StringUtils.isNotEmpty(id)) {
            xtw.writeAttribute(ATTRIBUTE_ID, textAnnotation.getId());
        }

        String textFormat = textAnnotation.getTextFormat();
        if (StringUtils.isNotEmpty(textFormat)) {
            xtw.writeAttribute(ATTRIBUTE_TEXT_FORMAT, textFormat);
        }

        xtw.writeStartElement(ELEMENT_TEXT);
        String text = textAnnotation.getText();
        if (StringUtils.isNotEmpty(text)) {
            xtw.writeCData(text);
        } else {
            xtw.writeCData("");
        }
        xtw.writeEndElement();

        xtw.writeEndElement();
    }
}
