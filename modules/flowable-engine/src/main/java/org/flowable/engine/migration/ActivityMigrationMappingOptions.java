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
public interface ActivityMigrationMappingOptions<T extends ActivityMigrationMapping> {

    T inParentProcessOfCallActivityId(String callActivityId);

    String getFromCallActivityId();

    boolean isToParentProcess();

    T inSubProcessOfCallActivityId(String callActivityId);

    T inSubProcessOfCallActivityId(String callActivityId, int subProcessDefVersion);

    String getToCallActivityId();

    Integer getCallActivityProcessDefinitionVersion();

    boolean isToCallActivity();

    interface SingleToActivityOptions <T extends ActivityMigrationMapping> extends ActivityMigrationMappingOptions<T> {

        T withNewAssignee(String newAssigneeId);

        String getWithNewAssignee();

        T withLocalVariable(String variableName, Object variableValue);

        T withLocalVariables(Map<String, Object> variables);

        Map<String,Object> getActivityLocalVariables();
    }

    interface MultipleToActivityOptions<T extends ActivityMigrationMapping> extends ActivityMigrationMappingOptions<T> {

        T withLocalVariableForActivity(String toActivity, String variableName, Object variableValue);

        T withLocalVariablesForActivity(String toActivity, Map<String, Object> variables);

        T withLocalVariableForAllActivities(String variableName, Object variableValue);

        T withLocalVariablesForAllActivities(Map<String, Object> variables);

        T withLocalVariables(Map<String, Map<String, Object>> mappingVariables);

        Map<String, Map<String, Object>> getActivitiesLocalVariables();
    }

}
