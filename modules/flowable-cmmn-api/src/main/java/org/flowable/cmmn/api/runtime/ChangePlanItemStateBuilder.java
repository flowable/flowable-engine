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

import java.util.List;
import java.util.Map;

import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.FlowableObjectNotFoundException;

/**
 * Helper for changing the state of a case instance.
 *
 * An instance can be obtained through {@link org.flowable.cmmn.api.CmmnRuntimeService#createChangePlanItemStateBuilder()}.
 *
 * @author Tijs Rademakers
 */
public interface ChangePlanItemStateBuilder {

    /**
     * Set the id of the case instance
     **/
    ChangePlanItemStateBuilder caseInstanceId(String caseInstanceId);

    /**
     * Set the id of the plan item instance for which the plan item state should be changed
     **/
    ChangePlanItemStateBuilder movePlanItemInstanceToPlanItemDefinitionId(String planItemInstanceId, String planItemDefinitionId);

    /**
     * Set the ids of the plan item instances which should be changed to a single plan item instance with the provided plan item definition id.
     **/
    ChangePlanItemStateBuilder movePlanItemInstancesToSinglePlanItemDefinitionId(List<String> planItemInstanceIds, String planItemDefinitionId);

    /**
     * Set the id of a plan item instance which should be changed to multiple plan item instances with the provided plan item definition ids.
     **/
    ChangePlanItemStateBuilder moveSinglePlanItemInstanceToPlanItemDefinitionIds(String planItemInstanceId, List<String> planItemDefinitionIds);

    /**
     * Moves the plan item instance with the current plan item definition id to the provided new plan item definition id
     */
    ChangePlanItemStateBuilder movePlanItemDefinitionIdTo(String currentPlanItemDefinitionId, String newPlanItemDefinitionId);

    /**
     * Set the plan item definition ids that should be changed to a single plan item definition id.
     */
    ChangePlanItemStateBuilder movePlanItemDefinitionIdsToSinglePlanItemDefinitionId(List<String> currentPlanItemDefinitionIds, String newPlanItemDefinitionId);

    /**
     * Set the plan item definition id that should be changed to multiple plan item definition ids.
     */
    ChangePlanItemStateBuilder moveSinglePlanItemDefinitionIdToPlanItemDefinitionIds(String currentPlanItemDefinitionId, List<String> newPlanItemDefinitionIds);

    /**
     * Set the case variable that should be set as part of the change plan item state action.
     */
    ChangePlanItemStateBuilder caseVariable(String caseVariableName, Object caseVariableValue);

    /**
     * Set the case variable that should be set as part of the change plan item state action.
     */
    ChangePlanItemStateBuilder caseVariables(Map<String, Object> caseVariables);
    
    /**
     * Changes the case instance state
     *
     * @throws FlowableObjectNotFoundException
     *             when no case instance is found
     * @throws FlowableException
     *             plan item instance could not be canceled or started
     **/
    void changeState();

}
