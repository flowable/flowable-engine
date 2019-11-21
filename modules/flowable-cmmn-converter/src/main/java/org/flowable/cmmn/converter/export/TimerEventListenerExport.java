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
import org.flowable.cmmn.model.CmmnModel;
import org.flowable.cmmn.model.TimerEventListener;

/**
 * @author Joram Barrez
 */
public class TimerEventListenerExport extends AbstractPlanItemDefinitionExport<TimerEventListener> {

    @Override
    protected Class<TimerEventListener> getExportablePlanItemDefinitionClass() {
        return TimerEventListener.class;
    }

    @Override
    protected String getPlanItemDefinitionXmlElementValue(TimerEventListener timerEventListener) {
        return ELEMENT_TIMER_EVENT_LISTENER;
    }

    @Override
    protected void writePlanItemDefinitionSpecificAttributes(TimerEventListener timerEventListener, XMLStreamWriter xtw) throws Exception {
        super.writePlanItemDefinitionSpecificAttributes(timerEventListener, xtw);

        if (StringUtils.isNotEmpty(timerEventListener.getAvailableConditionExpression())) {
            xtw.writeAttribute(FLOWABLE_EXTENSIONS_NAMESPACE,
                CmmnXmlConstants.ATTRIBUTE_EVENT_LISTENER_AVAILABLE_CONDITION,
                timerEventListener.getAvailableConditionExpression());
        }
    }

    @Override
    protected void writePlanItemDefinitionBody(CmmnModel model, TimerEventListener timerEventListener, XMLStreamWriter xtw) throws Exception {
        if (StringUtils.isNotEmpty(timerEventListener.getTimerExpression())) {
            xtw.writeStartElement(ELEMENT_TIMER_EXPRESSION);
            xtw.writeCData(timerEventListener.getTimerExpression());
            xtw.writeEndElement();
        }

        if (StringUtils.isNotEmpty(timerEventListener.getTimerStartTriggerSourceRef())) {
            xtw.writeStartElement(ELEMENT_PLAN_ITEM_START_TRIGGER);
            xtw.writeAttribute(ATTRIBUTE_PLAN_ITEM_START_TRIGGER_SRC_REF, timerEventListener.getTimerStartTriggerSourceRef());

            xtw.writeStartElement(ELEMENT_STANDARD_EVENT);
            xtw.writeCData(timerEventListener.getTimerStartTriggerStandardEvent());
            xtw.writeEndElement();

            xtw.writeEndElement();
        }
    }
}
