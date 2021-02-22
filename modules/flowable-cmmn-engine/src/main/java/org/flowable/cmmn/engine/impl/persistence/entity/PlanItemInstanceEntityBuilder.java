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
package org.flowable.cmmn.engine.impl.persistence.entity;

import java.util.Map;

import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.flowable.cmmn.model.PlanItem;

/**
 * A plan item instance builder used to create new plan item instances with different options.
 *
 * @author Micha Kiener
 */
public interface PlanItemInstanceEntityBuilder {

    /**
     * Set the plan item for the new instance to be based on. This is a mandatory information to be set.
     *
     * @param planItem the plan item to base the new instance on
     * @return the builder instance for method chaining
     */
    PlanItemInstanceEntityBuilder planItem(PlanItem planItem);

    /**
     * Optionally override the name for this plan item instance and don't create it based on its plan item model. If set, it has priority over the
     * default given by its plan item model.
     *
     * @param name the optional name to be used (overridden) for this plan item instance
     * @return the builder instance for method chaining
     */
    PlanItemInstanceEntityBuilder name(String name);

    /**
     * Set the id of the case definition this plan item instance is part of. This is a mandatory information to be set.
     *
     * @param caseDefinitionId the id of the case definition the plan item is a part of
     * @return the builder instance for method chaining
     */
    PlanItemInstanceEntityBuilder caseDefinitionId(String caseDefinitionId);

    /**
     * If this plan item is derived from another case definition than it is used in, set the case definition it is taken from using this method.
     *
     * @param derivedCaseDefinitionId the case definition id from which this plan item is derived from
     * @return the builder instance for method chaining
     */
    PlanItemInstanceEntityBuilder derivedCaseDefinitionId(String derivedCaseDefinitionId);

    /**
     * Set the id of the case instance the plan item instance is a direct or indirect child of. This is a mandatory information to be set.
     * @param caseInstanceId the id of the case instance the new plan item instance is a part of
     * @return the builder instance for method chaining
     */
    PlanItemInstanceEntityBuilder caseInstanceId(String caseInstanceId);

    /**
     * Set the id of the stage plan item instance the new plan item instance is a direct child of.
     * @param stagePlanItemInstance the parent stage instance for the new plan item instance
     * @return the builder instance for method chaining
     */
    PlanItemInstanceEntityBuilder stagePlanItemInstance(PlanItemInstance stagePlanItemInstance);

    /**
     * Set the id of the tenant for the new plan item instance.
     * @param tenantId the id of the tenant the new plan item instance belongs to
     * @return the builder instance for method chaining
     */
    PlanItemInstanceEntityBuilder tenantId(String tenantId);

    /**
     * Optionally add any variables to be set on the new plan item instance as local variables.
     * @param localVariables an optional map of variables to set as local values for the new plan item instance
     * @return the builder instance for method chaining
     */
    PlanItemInstanceEntityBuilder localVariables(Map<String, Object> localVariables);

    /**
     * Set true, if the new plan item instance to be created should be added to its parent, false otherwise.
     * @param addToParent true, if the plan item instance should be added to its parent
     * @return the builder instance for method chaining
     */
    PlanItemInstanceEntityBuilder addToParent(boolean addToParent);

    /**
     * Invoke this method to suppress any exceptions thrown when evaluating the plan item name expression. This might be necessary, if not all of the necessary
     * values are already available when creating the plan item instance and evaluating its name expression.
     * By default, this is NOT set and an exception while evaluating the name expression will lead into an exception.
     * @param silentNameExpressionEvaluation true, if the name expression evaluation should ignore any exception thrown
     * @return the builder instance for method chaining
     */
    PlanItemInstanceEntityBuilder silentNameExpressionEvaluation(boolean silentNameExpressionEvaluation);

    /**
     * Checks for all necessary values to be present within the builder, creates a new plan item instance and returns it.
     * @return the newly created plan item instance
     */
    PlanItemInstanceEntity create();
}
