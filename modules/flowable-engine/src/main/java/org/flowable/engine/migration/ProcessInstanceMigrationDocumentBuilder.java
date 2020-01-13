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

import java.util.List;
import java.util.Map;

/**
 * @author Dennis Federico
 * @author martin.grofcik
 */
public interface ProcessInstanceMigrationDocumentBuilder {

    ProcessInstanceMigrationDocumentBuilder setProcessDefinitionToMigrateTo(String processDefinitionId);

    ProcessInstanceMigrationDocumentBuilder setProcessDefinitionToMigrateTo(String processDefinitionKey, Integer processDefinitionVersion);

    ProcessInstanceMigrationDocumentBuilder setTenantId(String processDefinitionTenantId);

    ProcessInstanceMigrationDocumentBuilder setPreUpgradeScript(Script script);

    ProcessInstanceMigrationDocumentBuilder setPreUpgradeJavaDelegate(String javaDelegateClassName);

    ProcessInstanceMigrationDocumentBuilder setPreUpgradeJavaDelegateExpression(String expression);

    ProcessInstanceMigrationDocumentBuilder setPostUpgradeScript(Script script);

    ProcessInstanceMigrationDocumentBuilder setPostUpgradeJavaDelegate(String javaDelegateClassName);

    ProcessInstanceMigrationDocumentBuilder setPostUpgradeJavaDelegateExpression(String expression);

    ProcessInstanceMigrationDocumentBuilder addActivityMigrationMappings(List<ActivityMigrationMapping> activityMigrationMappings);

    ProcessInstanceMigrationDocumentBuilder addActivityMigrationMapping(ActivityMigrationMapping activityMigrationMapping);

    ProcessInstanceMigrationDocumentBuilder addProcessInstanceVariable(String variableName, Object variableValue);

    ProcessInstanceMigrationDocumentBuilder addProcessInstanceVariables(Map<String, Object> processInstanceVariables);

    ProcessInstanceMigrationDocument build();

}
