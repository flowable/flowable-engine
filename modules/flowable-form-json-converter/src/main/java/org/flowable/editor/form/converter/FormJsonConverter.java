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
package org.flowable.editor.form.converter;

import org.flowable.form.model.SimpleFormModel;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author Tijs Rademakers
 */
public class FormJsonConverter {

    protected ObjectMapper objectMapper = new ObjectMapper();

    public SimpleFormModel convertToFormModel(String modelJson) {
        try {
            SimpleFormModel definition = objectMapper.readValue(modelJson, SimpleFormModel.class);

            return definition;
        } catch (Exception e) {
            throw new FlowableFormJsonException("Error reading form json", e);
        }
    }

    public String convertToJson(SimpleFormModel definition) {
        try {
            return objectMapper.writeValueAsString(definition);
        } catch (Exception e) {
            throw new FlowableFormJsonException("Error writing form json", e);
        }
    }
}