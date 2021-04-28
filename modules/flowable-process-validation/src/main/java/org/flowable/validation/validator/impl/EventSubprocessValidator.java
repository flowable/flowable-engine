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
package org.flowable.validation.validator.impl;

import java.util.List;

import org.flowable.bpmn.model.BoundaryEvent;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.ConditionalEventDefinition;
import org.flowable.bpmn.model.ErrorEventDefinition;
import org.flowable.bpmn.model.EscalationEventDefinition;
import org.flowable.bpmn.model.EventDefinition;
import org.flowable.bpmn.model.EventSubProcess;
import org.flowable.bpmn.model.MessageEventDefinition;
import org.flowable.bpmn.model.Process;
import org.flowable.bpmn.model.SignalEventDefinition;
import org.flowable.bpmn.model.StartEvent;
import org.flowable.bpmn.model.TimerEventDefinition;
import org.flowable.bpmn.model.VariableListenerEventDefinition;
import org.flowable.validation.ValidationError;
import org.flowable.validation.validator.Problems;
import org.flowable.validation.validator.ProcessLevelValidator;

/**
 * @author jbarrez
 */
public class EventSubprocessValidator extends ProcessLevelValidator {

    @Override
    protected void executeValidation(BpmnModel bpmnModel, Process process, List<ValidationError> errors) {
        List<EventSubProcess> eventSubprocesses = process.findFlowElementsOfType(EventSubProcess.class);
        for (EventSubProcess eventSubprocess : eventSubprocesses) {

            List<StartEvent> startEvents = process.findFlowElementsInSubProcessOfType(eventSubprocess, StartEvent.class);
            for (StartEvent startEvent : startEvents) {
                if (startEvent.getEventDefinitions() != null && !startEvent.getEventDefinitions().isEmpty()) {
                    EventDefinition eventDefinition = startEvent.getEventDefinitions().get(0);
                    if (!(eventDefinition instanceof ConditionalEventDefinition) &&
                            !(eventDefinition instanceof ErrorEventDefinition) &&
                            !(eventDefinition instanceof EscalationEventDefinition) &&
                            !(eventDefinition instanceof MessageEventDefinition) &&
                            !(eventDefinition instanceof SignalEventDefinition) &&
                            !(eventDefinition instanceof TimerEventDefinition) &&
                            !(eventDefinition instanceof VariableListenerEventDefinition)) {

                        addError(errors, Problems.EVENT_SUBPROCESS_INVALID_START_EVENT_DEFINITION, process, eventSubprocess, eventDefinition,
                                "start event of event subprocess must be of type 'error', 'timer', 'message' or 'signal'");
                    }
                }
            }

            List<BoundaryEvent> boundaryEvents = eventSubprocess.getBoundaryEvents();
            if (boundaryEvents != null && !boundaryEvents.isEmpty()) {
                addWarning(errors, Problems.EVENT_SUBPROCESS_BOUNDARY_EVENT, process, eventSubprocess, "event sub process cannot have attached boundary events");
            }

        }
    }

}
