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
package org.flowable.form.api;

import java.util.Map;

/**
 * Defines a form field handler.
 * 
 * @author Tijs Rademakers
 */
public interface FormFieldHandler {

    void handleFormFieldsOnSubmit(FormInfo formInfo, String taskId, String processInstanceId, String scopeId, 
                    String scopeType, Map<String, Object> variables, String tenantId);
    
    void enrichFormFields(FormInfo formInfo);

}
