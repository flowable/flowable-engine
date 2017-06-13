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
package org.flowable.bpm.model.bpmn.validation;

import org.flowable.bpm.model.bpmn.instance.Process;
import org.flowable.bpm.model.bpmn.instance.StartEvent;
import org.flowable.bpm.model.xml.validation.ModelElementValidator;
import org.flowable.bpm.model.xml.validation.ValidationResultCollector;

import java.util.Collection;

public class ProcessStartEventValidator
        implements ModelElementValidator<Process> {

    @Override
    public Class<Process> getElementType() {
        return Process.class;
    }

    @Override
    public void validate(Process process, ValidationResultCollector validationResultCollector) {
        Collection<StartEvent> startEvents = process.getChildElementsByType(StartEvent.class);
        int startEventCount = startEvents.size();

        if (startEventCount != 1) {
            validationResultCollector.addError(10, String.format("Process does not have exactly one start event. Got %d.", startEventCount));
        }
    }

}
