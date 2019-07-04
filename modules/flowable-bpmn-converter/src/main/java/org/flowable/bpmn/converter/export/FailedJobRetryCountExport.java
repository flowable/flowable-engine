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
package org.flowable.bpmn.converter.export;

import javax.xml.stream.XMLStreamWriter;

import org.apache.commons.lang3.StringUtils;
import org.flowable.bpmn.constants.BpmnXMLConstants;
import org.flowable.bpmn.model.Activity;

public class FailedJobRetryCountExport implements BpmnXMLConstants {

    public static boolean writeFailedJobRetryCount(Activity activity, boolean didWriteExtensionStartElement, XMLStreamWriter xtw) throws Exception {
        String failedJobRetryCycle = activity.getFailedJobRetryTimeCycleValue();
        if (failedJobRetryCycle != null) {

            if (StringUtils.isNotEmpty(failedJobRetryCycle)) {
                if (!didWriteExtensionStartElement) {
                    xtw.writeStartElement(ELEMENT_EXTENSIONS);
                    didWriteExtensionStartElement = true;
                }
                xtw.writeStartElement(FLOWABLE_EXTENSIONS_PREFIX, FAILED_JOB_RETRY_TIME_CYCLE, FLOWABLE_EXTENSIONS_NAMESPACE);
                xtw.writeCharacters(failedJobRetryCycle);
                xtw.writeEndElement();
            }
        }
        return didWriteExtensionStartElement;
    }
}
