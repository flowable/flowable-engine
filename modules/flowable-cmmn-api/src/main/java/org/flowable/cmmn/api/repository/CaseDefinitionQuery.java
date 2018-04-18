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

package org.flowable.cmmn.api.repository;

import java.util.Set;

import org.flowable.common.engine.api.query.Query;

/**
 * @author Joram Barrez
 */
public interface CaseDefinitionQuery extends Query<CaseDefinitionQuery, CaseDefinition> {

    CaseDefinitionQuery caseDefinitionId(String caseDefinitionId);

    CaseDefinitionQuery caseDefinitionIds(Set<String> caseDefinitionIds);

    CaseDefinitionQuery caseDefinitionCategory(String caseDefinitionCategory);

    CaseDefinitionQuery caseDefinitionCategoryLike(String caseDefinitionCategoryLike);

    CaseDefinitionQuery caseDefinitionCategoryNotEquals(String categoryNotEquals);

    CaseDefinitionQuery caseDefinitionName(String caseDefinitionName);

    CaseDefinitionQuery caseDefinitionNameLike(String caseDefinitionNameLike);

    CaseDefinitionQuery deploymentId(String deploymentId);

    CaseDefinitionQuery deploymentIds(Set<String> deploymentIds);

    CaseDefinitionQuery caseDefinitionKey(String caseDefinitionKey);

    CaseDefinitionQuery caseDefinitionKeyLike(String caseDefinitionKeyLike);

    CaseDefinitionQuery caseDefinitionVersion(Integer caseDefinitionVersion);

    CaseDefinitionQuery caseDefinitionVersionGreaterThan(Integer caseDefinitionVersion);

    CaseDefinitionQuery caseDefinitionVersionGreaterThanOrEquals(Integer caseDefinitionVersion);

    CaseDefinitionQuery caseDefinitionVersionLowerThan(Integer caseDefinitionVersion);

    CaseDefinitionQuery caseDefinitionVersionLowerThanOrEquals(Integer caseDefinitionVersion);

    CaseDefinitionQuery latestVersion();

    CaseDefinitionQuery caseDefinitionResourceName(String resourceName);

    CaseDefinitionQuery caseDefinitionResourceNameLike(String resourceNameLike);

    CaseDefinitionQuery caseDefinitionTenantId(String tenantId);

    CaseDefinitionQuery caseDefinitionTenantIdLike(String tenantIdLike);

    CaseDefinitionQuery caseDefinitionWithoutTenantId();

    CaseDefinitionQuery orderByCaseDefinitionCategory();

    CaseDefinitionQuery orderByCaseDefinitionKey();

    CaseDefinitionQuery orderByCaseDefinitionId();

    CaseDefinitionQuery orderByCaseDefinitionVersion();

    CaseDefinitionQuery orderByCaseDefinitionName();

    CaseDefinitionQuery orderByDeploymentId();

    CaseDefinitionQuery orderByTenantId();

}
