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
package org.flowable.cmmn.api.runtime;

/**
 * A builder API to create new, dynamically injected plan items into an existing, running stage instance.
 *
 * @author Micha Kiener
 */
public interface InjectedPlanItemInstanceBuilder {

    /**
     * The explicit name for the new plan item to be created, if this is not set, the name of the referenced element is taken instead.
     *
     * @param name the explicit name to be used for the new plan item, which supersedes the one from the referenced plan item model
     * @return the builder reference for method chaining
     */
    InjectedPlanItemInstanceBuilder name(String name);

    /**
     * The id of the case definition from which the referenced plan item model should be taken as the model for the new plan item to be created dynamically.
     *
     * @param caseDefinitionId the id of the case definition where the referenced plan item model is taken from
     * @return the builder reference for method chaining
     */
    InjectedPlanItemInstanceBuilder caseDefinitionId(String caseDefinitionId);

    /**
     * The id of the referenced element within the case model to be used as the base line for the new dynamic plan item to be created.
     *
     * @param elementId the id of the referenced plan item element within the case model
     * @return the builder reference for method chaining
     */
    InjectedPlanItemInstanceBuilder elementId(String elementId);

    /**
     * Create the newly setup plan item, add it to the parent running stage and plan it for activation and further processing in the case engine.
     *
     * @param stagePlanItemInstanceId the id of the running stage plan item instance to inject a new plan item into
     * @return the plan item instance dynamically be created
     */
    PlanItemInstance createInStage(String stagePlanItemInstanceId);

    /**
     * Create the newly setup plan item, add it to the parent running case instance and plan it for activation and further processing in the case engine.
     *
     * @param caseInstanceId the id of the running case instance to inject a new plan item into
     * @return the plan item instance dynamically be created
     */
    PlanItemInstance createInCase(String caseInstanceId);
}
