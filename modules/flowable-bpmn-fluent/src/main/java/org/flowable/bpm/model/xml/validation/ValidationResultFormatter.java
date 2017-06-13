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

import org.flowable.bpm.model.xml.instance.ModelElementInstance;

import java.io.StringWriter;

/**
 * Service Provider Interface which can be implemented to print out a summary of a validation result. See
 * {@link ValidationResults#write(StringWriter, ValidationResultFormatter)}
 */
public interface ValidationResultFormatter {

    /**
     * Formats an element in the summary
     *
     * @param writer the writer
     * @param element the element to write
     */
    void formatElement(StringWriter writer, ModelElementInstance element);

    /**
     * Formats a validation result
     *
     * @param writer the writer
     * @param result the result to format
     */
    void formatResult(StringWriter writer, ValidationResult result);

}
