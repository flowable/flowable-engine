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
import org.flowable.cmmn.converter.util.CmmnXmlUtil;
import org.flowable.cmmn.model.CmmnModel;
import org.flowable.cmmn.model.Sentry;
import org.flowable.cmmn.model.SentryIfPart;
import org.flowable.cmmn.model.SentryOnPart;

public class SentryExport implements CmmnXmlConstants {
    
    public static void writeSentry(CmmnModel model, Sentry sentry, XMLStreamWriter xtw) throws Exception {
        // start sentry element
        xtw.writeStartElement(ELEMENT_SENTRY);
        xtw.writeAttribute(ATTRIBUTE_ID, sentry.getId());

        if (StringUtils.isNotEmpty(sentry.getName())) {
            xtw.writeAttribute(ATTRIBUTE_NAME, sentry.getName());
        }

        if (StringUtils.isNotEmpty(sentry.getTriggerMode())
                && !Sentry.TRIGGER_MODE_DEFAULT.equals(sentry.getTriggerMode())) { // default is not exported. If missing, default is assumed
            xtw.writeAttribute(FLOWABLE_EXTENSIONS_NAMESPACE, ATTRIBUTE_TRIGGER_MODE, sentry.getTriggerMode());
        }

        if (StringUtils.isNotEmpty(sentry.getDocumentation())) {
            xtw.writeStartElement(ELEMENT_DOCUMENTATION);
            xtw.writeCharacters(sentry.getDocumentation());
            xtw.writeEndElement();
        }

        boolean didWriteExtensionElement = CmmnXmlUtil.writeExtensionElements(sentry, false, model.getNamespaces(), xtw);
        if (didWriteExtensionElement) {
            xtw.writeEndElement();
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
        SentryIfPart sentryIfPart = sentry.getSentryIfPart();
        if (sentryIfPart != null) {
            xtw.writeStartElement(ELEMENT_IF_PART);
            if (StringUtils.isNotEmpty(sentryIfPart.getId())) {
                xtw.writeAttribute(ATTRIBUTE_ID, sentryIfPart.getId());
            }
            xtw.writeStartElement(ELEMENT_CONDITION);
            xtw.writeCData(sentryIfPart.getCondition());
            xtw.writeEndElement();
            xtw.writeEndElement();
        }

        // end plan item element
        xtw.writeEndElement();
    }
}
