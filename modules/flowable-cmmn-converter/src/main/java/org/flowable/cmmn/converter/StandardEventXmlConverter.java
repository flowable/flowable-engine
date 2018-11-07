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

import org.flowable.cmmn.model.BaseElement;
import org.flowable.cmmn.model.TimerEventListener;

/**
 * @author Joram Barrez
 */
public class StandardEventXmlConverter extends CaseElementXmlConverter {
    
    @Override
    public String getXMLElementName() {
        return CmmnXmlConstants.ELEMENT_STANDARD_EVENT;
    }
    
    @Override
    public boolean hasChildElements() {
        return false;
    }

    @Override
    protected BaseElement convert(XMLStreamReader xtr, ConversionHelper conversionHelper) {
        String event = xtr.getText();
        if (conversionHelper.getCurrentCmmnElement() instanceof TimerEventListener) {
            TimerEventListener timerEventListener = (TimerEventListener) conversionHelper.getCurrentCmmnElement();
            timerEventListener.setTimerStartTriggerStandardEvent(event);
        } else {
            conversionHelper.getCurrentSentryOnPart().setStandardEvent(xtr.getText());
        }
        return null;
    }
    
}