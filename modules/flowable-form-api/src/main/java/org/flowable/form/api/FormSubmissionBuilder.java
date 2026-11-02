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
 * Helper for validating a form submission and extracting the variables from it.
 *
 * An instance can be obtained through {@link FormService#createFormSubmissionBuilder()}.
 *
 * @author Filip Hrisafov
 */
public interface FormSubmissionBuilder {

    /**
     * Set the id of the element to which the form is attached (e.g. the task definition key).
     */
    FormSubmissionBuilder elementId(String elementId);

    /**
     * Set the type of the element to which the form is attached (e.g. userTask, humanTask, startEvent).
     */
    FormSubmissionBuilder elementType(String elementType);

    /**
     * Set the id of the scope (e.g. process or case instance) for which the form is submitted.
     */
    FormSubmissionBuilder scopeId(String scopeId);

    /**
     * Set the id of the definition of the scope (e.g. process or case definition) for which the form is submitted.
     */
    FormSubmissionBuilder scopeDefinitionId(String scopeDefinitionId);

    /**
     * Set the id of the sub scope (e.g. plan item instance) for which the form is submitted.
     */
    FormSubmissionBuilder subScopeId(String subScopeId);

    /**
     * Set the type of the scope for which the form is submitted.
     */
    FormSubmissionBuilder scopeType(String scopeType);

    /**
     * Set the id of the task for which the form is submitted.
     */
    FormSubmissionBuilder taskId(String taskId);

    /**
     * Set the form definition to use for the type conversion and the validation.
     */
    FormSubmissionBuilder formInfo(FormInfo formInfo);

    /**
     * Set the values submitted by the user.
     */
    FormSubmissionBuilder values(Map<String, Object> values);

    /**
     * Set the outcome selected by the user.
     * If not set, no outcome is used and any outcome definitions are ignored.
     */
    FormSubmissionBuilder outcome(String outcome);

    /**
     * Apply the validation restrictions on the submitted values.
     *
     * @throws org.flowable.common.engine.api.FlowableException in the case when the validation failed
     */
    void validate();

    /**
     * @return raw variables that can be used in the engines, based on the submitted values and the selected outcome.
     */
    Map<String, Object> extractVariables();

}
