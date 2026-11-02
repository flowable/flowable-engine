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

import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.CancelEventDefinition;
import org.flowable.bpmn.model.EndEvent;
import org.flowable.bpmn.model.EventDefinition;
import org.flowable.bpmn.model.EventDefinitionLocation;
import org.flowable.bpmn.model.FlowElementsContainer;
import org.flowable.bpmn.model.Process;
import org.flowable.bpmn.model.Transaction;
import org.flowable.validation.ProcessValidationContext;
import org.flowable.validation.validator.Problems;
import org.flowable.validation.validator.ProcessLevelValidator;

/**
 * @author jbarrez
 */
public class EndEventValidator extends ProcessLevelValidator {

    @Override
    protected void executeValidation(BpmnModel bpmnModel, Process process, ProcessValidationContext validationContext) {
        List<EndEvent> endEvents = process.findFlowElementsOfType(EndEvent.class);
        for (EndEvent endEvent : endEvents) {
            if (endEvent.getEventDefinitions() == null || endEvent.getEventDefinitions().isEmpty()) {
                continue;
            }
            EventDefinition eventDefinition = endEvent.getEventDefinitions().get(0);

            if (!eventDefinition.getSupportedLocations().contains(EventDefinitionLocation.END_EVENT)) {
                validationContext.addError(Problems.END_EVENT_INVALID_EVENT_DEFINITION, process, endEvent, eventDefinition, "Invalid or unsupported event definition");
            }

            if (eventDefinition instanceof CancelEventDefinition) {
                FlowElementsContainer parent = process.findParent(endEvent);
                if (!(parent instanceof Transaction)) {
                    validationContext.addError(Problems.END_EVENT_CANCEL_ONLY_INSIDE_TRANSACTION, process, endEvent, "end event with cancelEventDefinition only supported inside transaction subprocess");
                }
            }
        }
    }

}
