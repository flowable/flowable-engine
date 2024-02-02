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
public interface ProcessInstanceMigrationDocument {

    String getMigrateToProcessDefinitionId();

    String getMigrateToProcessDefinitionKey();

    Integer getMigrateToProcessDefinitionVersion();

    String getMigrateToProcessDefinitionTenantId();

    Script getPreUpgradeScript();

    String getPreUpgradeJavaDelegate();

    String getPreUpgradeJavaDelegateExpression();

    Script getPostUpgradeScript();

    String getPostUpgradeJavaDelegate();

    String getPostUpgradeJavaDelegateExpression();

    List<ActivityMigrationMapping> getActivityMigrationMappings();

    Map<String, Map<String, Object>> getActivitiesLocalVariables();

    Map<String, Object> getProcessInstanceVariables();

    String asJsonString();

}
