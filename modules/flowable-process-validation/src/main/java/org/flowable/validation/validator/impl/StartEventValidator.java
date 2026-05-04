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

import java.util.ArrayList;
import java.util.List;

import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.EventDefinition;
import org.flowable.bpmn.model.EventDefinitionLocation;
import org.flowable.bpmn.model.Process;
import org.flowable.bpmn.model.StartEvent;
import org.flowable.validation.ProcessValidationContext;
import org.flowable.validation.validator.Problems;
import org.flowable.validation.validator.ProcessLevelValidator;

/**
 * @author Joram Barrez
 */
public class StartEventValidator extends ProcessLevelValidator {

    @Override
    protected void executeValidation(BpmnModel bpmnModel, Process process, ProcessValidationContext validationContext) {
        List<StartEvent> startEvents = process.findFlowElementsOfType(StartEvent.class, false);
        validateEventDefinitionTypes(startEvents, process, validationContext);
        validateMultipleStartEvents(startEvents, process, validationContext);
    }

    protected void validateEventDefinitionTypes(List<StartEvent> startEvents, Process process, ProcessValidationContext validationContext) {
        for (StartEvent startEvent : startEvents) {
            if (startEvent.getEventDefinitions() != null && !startEvent.getEventDefinitions().isEmpty()) {
                EventDefinition eventDefinition = startEvent.getEventDefinitions().get(0);
                if (!eventDefinition.getSupportedLocations().contains(EventDefinitionLocation.START_EVENT)) {
                    validationContext.addError(Problems.START_EVENT_INVALID_EVENT_DEFINITION,
                            process, startEvent,
                            eventDefinition,
                            "Unsupported event definition on start event");
                }
            }

        }
    }

    protected void validateMultipleStartEvents(List<StartEvent> startEvents, Process process, ProcessValidationContext validationContext) {

        // Multiple none events are not supported
        List<StartEvent> noneStartEvents = new ArrayList<>();
        for (StartEvent startEvent : startEvents) {
            if (startEvent.getEventDefinitions() == null || startEvent.getEventDefinitions().isEmpty()) {
                noneStartEvents.add(startEvent);
            }
        }

        if (noneStartEvents.size() > 1) {
            for (StartEvent startEvent : noneStartEvents) {
                validationContext.addError(
                        Problems.START_EVENT_MULTIPLE_FOUND,
                        process,
                        startEvent,
                        "Multiple none start events are not supported");
            }
        }

    }

}
