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

/**
 * Object passed to the {@link ModelElementValidator} to collect validation results.
 */
public interface ValidationResultCollector {

    /**
     * Adds an error.
     *
     * @param code a reference code for the error
     * @param message a human consumable error message
     */
    void addError(int code, String message);

    /**
     * Adds a warning.
     *
     * @param code a reference code for the error
     * @param message a human consumable error message
     */
    void addWarning(int code, String message);

}
