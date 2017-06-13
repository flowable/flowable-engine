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
package org.flowable.bpm.model.xml.validation;

import org.flowable.bpm.model.xml.ModelInstance;
import org.flowable.bpm.model.xml.instance.ModelElementInstance;

import java.io.StringWriter;
import java.util.List;
import java.util.Map;

/**
 * Object in which the results of a model validation are collected. See: {@link ModelInstance#validate(java.util.Collection)}.
 */
public interface ValidationResults {

    /**
     * @return true if there are {@link ValidationResult} of type {@link ValidationResultType#ERROR}
     */
    boolean hasErrors();

    /**
     * @return the count of {@link ValidationResult} of type {@link ValidationResultType#ERROR}
     */
    int getErrorCount();

    /**
     * @return the count of {@link ValidationResult} of type {@link ValidationResultType#WARNING}
     */
    int getWarningCount();

    /**
     * @return the individual results of the validation grouped by element.
     */
    Map<ModelElementInstance, List<ValidationResult>> getResults();

    /**
     * Utility method to print out a summary of the validation results.
     *
     * @param writer a {@link StringWriter} to which the result should be printed
     * @param printer formatter for printing elements and validation results
     */
    void write(StringWriter writer, ValidationResultFormatter printer);

}
