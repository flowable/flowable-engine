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
package org.flowable.engine.migration;

import java.util.Map;

/**
 * @author Dennis
 */
public interface ProcessInstanceActivityMigrationMappingOptions <T extends ProcessInstanceActivityMigrationMapping> {

    T withNewAssignee(String newAssigneeId);

    String getWithNewAssignee();

    interface SingleToActivityOptions <T extends ProcessInstanceActivityMigrationMapping> extends ProcessInstanceActivityMigrationMappingOptions <T> {

        T withLocalVariable(String variableName, Object variableValue);

        T withLocalVariables(Map<String, Object> variables);

        Map<String,Object> getActivityLocalVariables();
    }

    interface MultipleToActivityOptions<T extends ProcessInstanceActivityMigrationMapping> extends ProcessInstanceActivityMigrationMappingOptions <T> {

        T withLocalVariableForActivity(String toActivity, String variableName, Object variableValue);

        T withLocalVariablesForActivity(String toActivity, Map<String, Object> variables);

        T withLocalVariableForAllActivities(String variableName, Object variableValue);

        T withLocalVariablesForAllActivities(Map<String, Object> variables);

        Map<String, Map<String, Object>> getActivitiesLocalVariables();
    }

}
