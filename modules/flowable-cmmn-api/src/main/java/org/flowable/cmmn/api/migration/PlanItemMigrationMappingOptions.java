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
public interface PlanItemMigrationMappingOptions<T extends PlanItemMigrationMapping> {

    interface SingleToPlanItemOptions<T extends PlanItemMigrationMapping> extends PlanItemMigrationMappingOptions<T> {

        T withNewAssignee(String newAssigneeId);

        String getWithNewAssignee();

        T withLocalVariable(String variableName, Object variableValue);

        T withLocalVariables(Map<String, Object> variables);

        Map<String, Object> getPlanItemLocalVariables();

    }

    interface MultipleToPlanItemOptions<T extends PlanItemMigrationMapping> extends PlanItemMigrationMappingOptions<T> {

        T withLocalVariableForPlanItem(String toPlanItem, String variableName, Object variableValue);

        T withLocalVariablesForPlanItem(String toPlanItem, Map<String, Object> variables);

        T withLocalVariableForAllPlanItems(String variableName, Object variableValue);

        T withLocalVariablesForAllPlanItems(Map<String, Object> variables);

        T withLocalVariables(Map<String, Map<String, Object>> mappingVariables);

        Map<String, Map<String, Object>> getPlanItemsLocalVariables();
    }

}
