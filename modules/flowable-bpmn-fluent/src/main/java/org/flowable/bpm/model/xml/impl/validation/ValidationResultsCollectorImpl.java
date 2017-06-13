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
package org.flowable.bpm.model.xml.impl.validation;

import org.flowable.bpm.model.xml.instance.ModelElementInstance;
import org.flowable.bpm.model.xml.validation.ValidationResult;
import org.flowable.bpm.model.xml.validation.ValidationResultCollector;
import org.flowable.bpm.model.xml.validation.ValidationResultType;
import org.flowable.bpm.model.xml.validation.ValidationResults;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ValidationResultsCollectorImpl
        implements ValidationResultCollector {

    protected ModelElementInstance currentElement;

    protected Map<ModelElementInstance, List<ValidationResult>> collectedResults = new HashMap<>();

    protected int errorCount;
    protected int warningCount;

    @Override
    public void addError(int code, String message) {
        resultsForCurrentElement()
                .add(new ModelValidationResultImpl(currentElement, ValidationResultType.ERROR, code, message));

        ++errorCount;
    }

    @Override
    public void addWarning(int code, String message) {
        resultsForCurrentElement()
                .add(new ModelValidationResultImpl(currentElement, ValidationResultType.WARNING, code, message));

        ++warningCount;
    }

    public void setCurrentElement(ModelElementInstance currentElement) {
        this.currentElement = currentElement;
    }

    public ValidationResults getResults() {
        return new ModelValidationResultsImpl(collectedResults, errorCount, warningCount);
    }

    protected List<ValidationResult> resultsForCurrentElement() {
        List<ValidationResult> resultsByElement = collectedResults.get(currentElement);

        if (resultsByElement == null) {
            resultsByElement = new ArrayList<>();
            collectedResults.put(currentElement, resultsByElement);
        }
        return resultsByElement;
    }

}
