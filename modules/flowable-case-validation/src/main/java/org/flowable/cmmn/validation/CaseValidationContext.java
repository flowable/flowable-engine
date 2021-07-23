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
package org.flowable.cmmn.validation;

import org.flowable.cmmn.model.BaseElement;
import org.flowable.cmmn.model.Case;
import org.flowable.cmmn.model.CaseElement;
import org.flowable.cmmn.validation.validator.ValidationEntry;

/**
 * @author Filip Hrisafov
 */
public interface CaseValidationContext {

    default ValidationEntry addError(String problem, String description) {
        return addError(problem, null, null, null, description);
    }

    default ValidationEntry addError(String problem, Case caze, CaseElement caseElement, BaseElement baseElement, String description) {
        return addEntry(problem, caze, caseElement, baseElement, description, ValidationEntry.Level.Error);
    }

    default ValidationEntry addWarning(String problem, String description) {
        return addWarning(problem, null, null, null, description);
    }

    default ValidationEntry addWarning(String problem, Case caze, CaseElement caseElement, BaseElement baseElement, String description) {
        return addEntry(problem, caze, caseElement, baseElement, description, ValidationEntry.Level.Warning);
    }

    default ValidationEntry addEntry(String problem, Case caze, CaseElement caseElement, BaseElement baseElement, String description,
            ValidationEntry.Level level) {
        ValidationEntry entry = new ValidationEntry();
        entry.setLevel(level);

        if (caze != null) {
            entry.setCaseDefinitionId(caze.getId());
            entry.setCaseDefinitionName(caze.getName());
        }

        if (baseElement != null) {
            entry.setXmlLineNumber(baseElement.getXmlRowNumber());
            entry.setXmlColumnNumber(baseElement.getXmlColumnNumber());
        }
        entry.setProblem(problem);
        entry.setDefaultDescription(description);

        if (caseElement == null && baseElement instanceof CaseElement) {
            caseElement = (CaseElement) baseElement;
        }

        if (caseElement != null) {
            entry.setItemId(caseElement.getId());
            entry.setItemName(caseElement.getName());
        }

        return addEntry(entry);
    }

    ValidationEntry addEntry(ValidationEntry entry);

}
