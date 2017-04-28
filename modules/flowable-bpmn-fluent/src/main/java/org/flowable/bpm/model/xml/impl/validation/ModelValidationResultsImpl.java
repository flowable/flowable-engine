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
import org.flowable.bpm.model.xml.validation.ValidationResultFormatter;
import org.flowable.bpm.model.xml.validation.ValidationResults;

import java.io.StringWriter;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class ModelValidationResultsImpl
        implements ValidationResults {

    protected Map<ModelElementInstance, List<ValidationResult>> collectedResults;

    protected int errorCount;
    protected int warningCount;

    public ModelValidationResultsImpl(Map<ModelElementInstance, List<ValidationResult>> collectedResults, int errorCount, int warningCount) {
        this.collectedResults = collectedResults;
        this.errorCount = errorCount;
        this.warningCount = warningCount;
    }

    @Override
    public boolean hasErrors() {
        return errorCount > 0;
    }

    @Override
    public int getErrorCount() {
        return errorCount;
    }

    @Override
    public int getWarningCount() {
        return warningCount;
    }

    @Override
    public void write(StringWriter writer, ValidationResultFormatter formatter) {
        for (Entry<ModelElementInstance, List<ValidationResult>> entry : collectedResults.entrySet()) {

            ModelElementInstance element = entry.getKey();
            List<ValidationResult> results = entry.getValue();

            formatter.formatElement(writer, element);

            for (ValidationResult result : results) {
                formatter.formatResult(writer, result);
            }
        }
    }

    @Override
    public Map<ModelElementInstance, List<ValidationResult>> getResults() {
        return collectedResults;
    }

}
