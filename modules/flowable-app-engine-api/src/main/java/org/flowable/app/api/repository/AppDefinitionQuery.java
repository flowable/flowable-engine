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

package org.flowable.app.api.repository;

import java.util.Set;

import org.flowable.common.engine.api.query.Query;

/**
 * @author Tijs Rademakers
 */
public interface AppDefinitionQuery extends Query<AppDefinitionQuery, AppDefinition> {

    AppDefinitionQuery appDefinitionId(String caseDefinitionId);

    AppDefinitionQuery appDefinitionIds(Set<String> caseDefinitionIds);

    AppDefinitionQuery appDefinitionCategory(String caseDefinitionCategory);

    AppDefinitionQuery appDefinitionCategoryLike(String caseDefinitionCategoryLike);

    AppDefinitionQuery appDefinitionCategoryNotEquals(String categoryNotEquals);

    AppDefinitionQuery appDefinitionName(String caseDefinitionName);

    AppDefinitionQuery appDefinitionNameLike(String caseDefinitionNameLike);

    AppDefinitionQuery deploymentId(String deploymentId);

    AppDefinitionQuery deploymentIds(Set<String> deploymentIds);

    AppDefinitionQuery appDefinitionKey(String caseDefinitionKey);

    AppDefinitionQuery appDefinitionKeyLike(String caseDefinitionKeyLike);

    AppDefinitionQuery appDefinitionVersion(Integer caseDefinitionVersion);

    AppDefinitionQuery appDefinitionVersionGreaterThan(Integer caseDefinitionVersion);

    AppDefinitionQuery appDefinitionVersionGreaterThanOrEquals(Integer caseDefinitionVersion);

    AppDefinitionQuery appDefinitionVersionLowerThan(Integer caseDefinitionVersion);

    AppDefinitionQuery appDefinitionVersionLowerThanOrEquals(Integer caseDefinitionVersion);

    AppDefinitionQuery latestVersion();

    AppDefinitionQuery appDefinitionResourceName(String resourceName);

    AppDefinitionQuery appDefinitionResourceNameLike(String resourceNameLike);

    AppDefinitionQuery appDefinitionTenantId(String tenantId);

    AppDefinitionQuery appDefinitionTenantIdLike(String tenantIdLike);

    AppDefinitionQuery appDefinitionWithoutTenantId();

    AppDefinitionQuery orderByAppDefinitionCategory();

    AppDefinitionQuery orderByAppDefinitionKey();

    AppDefinitionQuery orderByAppDefinitionId();

    AppDefinitionQuery orderByAppDefinitionVersion();

    AppDefinitionQuery orderByAppDefinitionName();

    AppDefinitionQuery orderByDeploymentId();

    AppDefinitionQuery orderByTenantId();

}
