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
package org.flowable.common.engine.impl;

import org.flowable.common.engine.impl.json.VariableJsonMapper;

/**
 * Handler for converting variable values between types.
 * Used by IO parameter processing (e.g. targetType on in parameters, sourceType on out parameters)
 * in both the BPMN and CMMN engines.
 *
 * <p>Can be plugged in on the process engine and CMMN engine configuration to customize
 * the type conversion behavior.</p>
 *
 * @author Tijs Rademakers
 */
public interface VariableValueConversionHandler {

    /**
     * Convert a value to the specified type.
     *
     * @param value the value to convert (must not be null)
     * @param type the target type name (e.g. "string", "integer", "date", "json")
     * @param variableJsonMapper the mapper to use for JSON/array conversions
     * @return the converted value
     */
    Object convertValue(Object value, String type, VariableJsonMapper variableJsonMapper);
}
