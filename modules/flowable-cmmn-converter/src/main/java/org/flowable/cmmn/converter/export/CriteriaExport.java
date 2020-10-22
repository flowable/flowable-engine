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

import javax.xml.stream.XMLStreamWriter;

import org.apache.commons.lang3.StringUtils;
import org.flowable.cmmn.converter.CmmnXmlConstants;
import org.flowable.cmmn.model.Criterion;
import org.flowable.cmmn.model.PlanItem;
import org.flowable.cmmn.model.TimerEventListener;

public class CriteriaExport implements CmmnXmlConstants {

    public static void writeCriteriaElements(PlanItem planItem, XMLStreamWriter xtw) throws Exception {
        if (!(planItem.getPlanItemDefinition() instanceof TimerEventListener)) {
            // Timer event listeners are not allowed to have criteria.
            // However the planItemStartTrigger is implemented as a fake criterion.
            // Therefore we ignore writing the entry criteria elements for Timer Event listeners
            writeEntryCriteriaElements(planItem.getEntryCriteria(), xtw);
        }
        writeExitCriteriaElements(planItem.getExitCriteria(), xtw);
    }

    public static void writeEntryCriteriaElements(List<Criterion> criterionList, XMLStreamWriter xtw) throws Exception {
        writeCriteriaElements(ELEMENT_ENTRY_CRITERION, criterionList, xtw);
    }

    public static void writeExitCriteriaElements(List<Criterion> criterionList, XMLStreamWriter xtw) throws Exception {
        writeCriteriaElements(ELEMENT_EXIT_CRITERION, criterionList, xtw);

    }

    public static void writeCriteriaElements(String criteriaElementLabel, List<Criterion> criterionList, XMLStreamWriter xtw) throws Exception {
        for (Criterion criterion : criterionList) {
            // start entry criterion element
            xtw.writeStartElement(criteriaElementLabel);
            xtw.writeAttribute(ATTRIBUTE_ID, criterion.getId());

            if (StringUtils.isNotEmpty(criterion.getName())) {
                xtw.writeAttribute(ATTRIBUTE_NAME, criterion.getName());
            }

            if (StringUtils.isNotEmpty(criterion.getSentryRef())) {
                xtw.writeAttribute(ATTRIBUTE_SENTRY_REF, criterion.getSentryRef());
            }

            if (StringUtils.isNotEmpty(criterion.getExitType())) {
                xtw.writeAttribute(FLOWABLE_EXTENSIONS_NAMESPACE, ATTRIBUTE_EXIT_TYPE, criterion.getExitType());
            }

            if (StringUtils.isNotEmpty(criterion.getExitEventType())) {
                xtw.writeAttribute(FLOWABLE_EXTENSIONS_NAMESPACE, ATTRIBUTE_EXIT_EVENT_TYPE, criterion.getExitEventType());
            }

            // end entry criterion element
            xtw.writeEndElement();
        }
    }
}
