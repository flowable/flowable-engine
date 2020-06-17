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

package org.flowable.cmmn.api.migration;

import java.util.Map;

/**
 * @author Valentin Zickner
 */
public class PlanItemDefinitionMappingBuilder {

    public static ActivatePlanItemDefinitionMapping createActivatePlanItemDefinitionMappingFor(String planItemDefinitionId) {
        return new ActivatePlanItemDefinitionMapping(planItemDefinitionId);
    }
    
    public static ActivatePlanItemDefinitionMapping createActivatePlanItemDefinitionMappingFor(String planItemDefinitionId,
            String newAssignee, Map<String, Object> withLocalVariables) {
        
        return new ActivatePlanItemDefinitionMapping(planItemDefinitionId, newAssignee, withLocalVariables);
    }
    
    public static TerminatePlanItemDefinitionMapping createTerminatePlanItemDefinitionMappingFor(String planItemDefinitionId) {
        return new TerminatePlanItemDefinitionMapping(planItemDefinitionId);
    }
    
    public static MoveToAvailablePlanItemDefinitionMapping createMoveToAvailablePlanItemDefinitionMappingFor(String planItemDefinitionId) {
        return new MoveToAvailablePlanItemDefinitionMapping(planItemDefinitionId);
    }
}
