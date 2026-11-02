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

import org.flowable.cmmn.api.migration.ActivatePlanItemDefinitionMapping;
import org.flowable.cmmn.api.migration.MoveToAvailablePlanItemDefinitionMapping;
import org.flowable.cmmn.api.migration.RemoveWaitingForRepetitionPlanItemDefinitionMapping;
import org.flowable.cmmn.api.migration.TerminatePlanItemDefinitionMapping;
import org.flowable.cmmn.api.migration.WaitingForRepetitionPlanItemDefinitionMapping;
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
     * Activate a plan item by definition id.
     */
    ChangePlanItemStateBuilder activatePlanItemDefinitionId(String planItemDefinitionId);
    
    /**
     * Activate multiple plan items by definition id.
     */
    ChangePlanItemStateBuilder activatePlanItemDefinitionIds(List<String> planItemDefinitionIds);
    
    /**
     * Activate a plan item by definition mapping.
     */
    ChangePlanItemStateBuilder activatePlanItemDefinition(ActivatePlanItemDefinitionMapping planItemDefinitionMapping);
    
    /**
     * Activate multiple plan items by definition mapping.
     */
    ChangePlanItemStateBuilder activatePlanItemDefinitions(List<ActivatePlanItemDefinitionMapping> planItemDefinitionMappings);
    
    /**
     * Set a plan item to available state by definition id.
     */
    ChangePlanItemStateBuilder changeToAvailableStateByPlanItemDefinitionId(String planItemDefinitionId);
    
    /**
     * Set multiple plan items to available state by definition id.
     */
    ChangePlanItemStateBuilder changeToAvailableStateByPlanItemDefinitionIds(List<String> planItemDefinitionIds);
    
    /**
     * Set a plan item to available state by definition mapping.
     */
    ChangePlanItemStateBuilder changeToAvailableStateByPlanItemDefinition(MoveToAvailablePlanItemDefinitionMapping planItemDefinitionMapping);
    
    /**
     * Terminate a plan item by definition id without terminating another plan item instance.
     */
    ChangePlanItemStateBuilder terminatePlanItemDefinitionId(String planItemDefinitionId);

    /**
     * Terminate a plan item by definition mapping without terminating another plan item instance.
     */
    ChangePlanItemStateBuilder terminatePlanItemDefinition(TerminatePlanItemDefinitionMapping planItemDefinition);

    /**
     * Terminate multiple plan items by definition id without terminating another plan item instance.
     */
    ChangePlanItemStateBuilder terminatePlanItemDefinitionIds(List<String> planItemDefinitionIds);
    
    /**
     * Add waiting for repetition to a plan item by definition id.
     */
    ChangePlanItemStateBuilder addWaitingForRepetitionPlanItemDefinitionId(String planItemDefinitionId);

    /**
     * Add waiting for repetition to a plan item by definition mapping.
     */
    ChangePlanItemStateBuilder addWaitingForRepetitionPlanItemDefinition(WaitingForRepetitionPlanItemDefinitionMapping planItemDefinitionMapping);

    /**
     * Add multiple waiting for repetitions to a plan item by definition id.
     */
    ChangePlanItemStateBuilder addWaitingForRepetitionPlanItemDefinitionIds(List<String> planItemDefinitionIds);
    
    /**
     * Remove waiting for repetition from a plan item by definition id.
     */
    ChangePlanItemStateBuilder removeWaitingForRepetitionPlanItemDefinitionId(String planItemDefinitionId);

    /**
     * Remove waiting for repetition from a plan item by definition.
     */
    ChangePlanItemStateBuilder removeWaitingForRepetitionPlanItemDefinition(RemoveWaitingForRepetitionPlanItemDefinitionMapping planItemDefinitionId);

    /**
     * Remove multiple waiting for repetitions from a plan item by definition id.
     */
    ChangePlanItemStateBuilder removeWaitingForRepetitionPlanItemDefinitionIds(List<String> planItemDefinitionIds);
    
    /**
     * Add plan item id mapping. This should not be needed in general, but there are cases where the existing plan item id 
     * is different from the new plan item id, and for this reason this option is provided.
     */
    ChangePlanItemStateBuilder changePlanItemId(String existingPlanItemId, String newPlanItemId);
    
    /**
     * Add plan item id mapping. This should not be needed in general, but there are cases where the existing plan item id 
     * is different from the new plan item id, and for this reason this option is provided.
     */
    ChangePlanItemStateBuilder changePlanItemIds(Map<String, String> changePlanItemIdMap);
    
    /**
     * Add plan item id mapping with definition id. This should not be needed in general, but there are cases where the existing plan item id 
     * is different from the new plan item id, and for this reason this option is provided.
     */
    ChangePlanItemStateBuilder changePlanItemIdWithDefinitionId(String existingPlanItemDefinitionId, String newPlanItemDefinitionId);
    
    /**
     * Add plan item id mapping with definition id. This should not be needed in general, but there are cases where the existing plan item id 
     * is different from the new plan item id, and for this reason this option is provided.
     */
    ChangePlanItemStateBuilder changePlanItemIdsWithDefinitionId(Map<String, String> changePlanItemIdWithDefinitionIdMap);
    
    /**
     * Add plan item id mapping with definition id. This should not be needed in general, but there are cases where the existing plan item id 
     * is different from the new plan item id, and for this reason this option is provided.
     */
    ChangePlanItemStateBuilder changePlanItemDefinitionWithNewTargetIds(String existingPlanItemDefinitionId, String newPlanItemId, String newPlanItemDefinitionId);
    
    /**
     * Set the case variable that should be set as part of the change plan item state action.
     */
    ChangePlanItemStateBuilder caseVariable(String caseVariableName, Object caseVariableValue);

    /**
     * Set the case variable that should be set as part of the change plan item state action.
     */
    ChangePlanItemStateBuilder caseVariables(Map<String, Object> caseVariables);
    
    /**
     * Set the case variable that should be set as part of the change process or case task state action.
     */
    ChangePlanItemStateBuilder childInstanceTaskVariable(String planItemDefinitionId, String name, Object value);
    
    /**
     * Set the case variable that should be set as part of the change process or case task state action.
     */
    ChangePlanItemStateBuilder childInstanceTaskVariables(String planItemDefinitionId, Map<String, Object> variables);
    
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
