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
public interface AppDefinitionBaseQuery <T extends AppDefinitionBaseQuery<T, C>, C extends AppDefinition> extends Query<T, C> {

    T appDefinitionId(String caseDefinitionId);

    T appDefinitionIds(Set<String> caseDefinitionIds);

    T appDefinitionCategory(String caseDefinitionCategory);

    T appDefinitionCategoryLike(String caseDefinitionCategoryLike);

    T appDefinitionCategoryNotEquals(String categoryNotEquals);

    T appDefinitionName(String caseDefinitionName);

    T appDefinitionNameLike(String caseDefinitionNameLike);

    T deploymentId(String deploymentId);

    T deploymentIds(Set<String> deploymentIds);

    T appDefinitionKey(String caseDefinitionKey);

    T appDefinitionKeyLike(String caseDefinitionKeyLike);

    T appDefinitionVersion(Integer caseDefinitionVersion);

    T appDefinitionVersionGreaterThan(Integer caseDefinitionVersion);

    T appDefinitionVersionGreaterThanOrEquals(Integer caseDefinitionVersion);

    T appDefinitionVersionLowerThan(Integer caseDefinitionVersion);

    T appDefinitionVersionLowerThanOrEquals(Integer caseDefinitionVersion);

    T latestVersion();

    T appDefinitionResourceName(String resourceName);

    T appDefinitionResourceNameLike(String resourceNameLike);

    T appDefinitionTenantId(String tenantId);

    T appDefinitionTenantIdLike(String tenantIdLike);

    T appDefinitionWithoutTenantId();

    T orderByAppDefinitionCategory();

    T orderByAppDefinitionKey();

    T orderByAppDefinitionId();

    T orderByAppDefinitionVersion();

    T orderByAppDefinitionName();

    T orderByDeploymentId();

    T orderByTenantId();

}
