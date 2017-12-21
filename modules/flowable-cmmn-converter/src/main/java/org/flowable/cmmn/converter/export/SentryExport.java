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
import org.flowable.cmmn.model.Sentry;
import org.flowable.cmmn.model.SentryOnPart;

public class SentryExport implements CmmnXmlConstants {
    
    public static void writeSentry(Sentry sentry, XMLStreamWriter xtw) throws Exception {
        // start sentry element
        xtw.writeStartElement(ELEMENT_SENTRY);
        xtw.writeAttribute(ATTRIBUTE_ID, sentry.getId());

        if (StringUtils.isNotEmpty(sentry.getName())) {
            xtw.writeAttribute(ATTRIBUTE_NAME, sentry.getName());
        }

        for (SentryOnPart sentryOnPart : sentry.getOnParts()) {
            // start sentry on part element
            xtw.writeStartElement(ELEMENT_PLAN_ITEM_ON_PART);
            
            xtw.writeAttribute(ATTRIBUTE_ID, sentryOnPart.getId());
            xtw.writeAttribute(ATTRIBUTE_SOURCE_REF, sentryOnPart.getSourceRef());
            
            // start standard event element
            xtw.writeStartElement(ELEMENT_STANDARD_EVENT);
            xtw.writeCharacters(sentryOnPart.getStandardEvent());
            xtw.writeEndElement();
            
            // end sentry on part element
            xtw.writeEndElement();
        }
        
        // If part
        if (sentry.getSentryIfPart() != null) {
            xtw.writeStartElement(ELEMENT_IF_PART);
            xtw.writeStartElement(ELEMENT_CONDITION);
            xtw.writeCData(sentry.getSentryIfPart().getCondition());
            xtw.writeEndElement();
            xtw.writeEndElement();
        }
        
        // end plan item element
        xtw.writeEndElement();
    }
}
