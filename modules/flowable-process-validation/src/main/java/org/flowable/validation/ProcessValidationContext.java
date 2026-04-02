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
package org.flowable.validation;

import org.flowable.bpmn.model.BaseElement;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.Process;

/**
 * @author Tijs Rademakers
 */
public interface ProcessValidationContext {

    default ValidationError addError(String problem, String description) {
        return addError(problem, null, null, null, description, false);
    }

    default ValidationError addError(String problem, BaseElement baseElement, String description) {
        return addError(problem, null, null, baseElement, description, false);
    }

    default ValidationError addError(String problem, FlowElement flowElement, BaseElement baseElement, String description) {
        return addError(problem, null, flowElement, baseElement, description, false);
    }

    default ValidationError addError(String problem, Process process, BaseElement baseElement, String description) {
        return addError(problem, process, null, baseElement, description, false);
    }

    default ValidationError addError(String problem, Process process, FlowElement flowElement, BaseElement baseElement, String description) {
        return addError(problem, process, flowElement, baseElement, description, false);
    }

    default ValidationError addWarning(String problem, Process process, BaseElement baseElement, String description) {
        return addWarning(problem, process, null, baseElement, description);
    }

    default ValidationError addWarning(String problem, Process process, FlowElement flowElement, BaseElement baseElement, String description) {
        return addError(problem, process, flowElement, baseElement, description, true);
    }

    default ValidationError addError(String problem, Process process, BaseElement baseElement, String description, boolean isWarning) {
        return addError(problem, process, null, baseElement, description, isWarning);
    }

    default ValidationError addError(String problem, Process process, FlowElement flowElement, BaseElement baseElement, String description,
            boolean isWarning) {
        ValidationError error = new ValidationError();
        error.setWarning(isWarning);

        if (process != null) {
            error.setProcessDefinitionId(process.getId());
            error.setProcessDefinitionName(process.getName());
        }

        if (baseElement != null) {
            error.setXmlLineNumber(baseElement.getXmlRowNumber());
            error.setXmlColumnNumber(baseElement.getXmlColumnNumber());
        }
        error.setProblem(problem);
        error.setDefaultDescription(description);

        if (flowElement == null && baseElement instanceof FlowElement) {
            flowElement = (FlowElement) baseElement;
        }

        if (flowElement != null) {
            error.setActivityId(flowElement.getId());
            error.setActivityName(flowElement.getName());
        }

        return addEntry(error);
    }

    default ValidationError addError(String problem, Process process, String id, String description) {
        ValidationError error = new ValidationError();

        if (process != null) {
            error.setProcessDefinitionId(process.getId());
            error.setProcessDefinitionName(process.getName());
        }

        error.setProblem(problem);
        error.setDefaultDescription(description);
        error.setActivityId(id);

        return addEntry(error);
    }

    ValidationError addEntry(ValidationError entry);

}
