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
package org.flowable.form.engine.impl.persistence.entity;

import java.util.List;
import java.util.Map;

import org.flowable.common.engine.impl.persistence.entity.EntityManager;
import org.flowable.form.api.FormDefinition;
import org.flowable.form.engine.impl.FormDefinitionQueryImpl;

/**
 * @author Joram Barrez
 */
public interface FormDefinitionEntityManager extends EntityManager<FormDefinitionEntity> {

    FormDefinitionEntity findLatestFormDefinitionByKey(String formDefinitionKey);

    FormDefinitionEntity findLatestFormDefinitionByKeyAndTenantId(String formDefinitionKey, String tenantId);

    List<FormDefinition> findFormDefinitionsByQueryCriteria(FormDefinitionQueryImpl formQuery);

    long findFormDefinitionCountByQueryCriteria(FormDefinitionQueryImpl formQuery);

    FormDefinitionEntity findFormDefinitionByDeploymentAndKey(String deploymentId, String formDefinitionKey);

    FormDefinitionEntity findFormDefinitionByDeploymentAndKeyAndTenantId(String deploymentId, String formDefinitionKey, String tenantId);

    FormDefinitionEntity findFormDefinitionByKeyAndVersionAndTenantId(String formDefinitionKey, Integer formVersion, String tenantId);

    List<FormDefinition> findFormDefinitionsByNativeQuery(Map<String, Object> parameterMap);

    long findFormDefinitionCountByNativeQuery(Map<String, Object> parameterMap);

    void updateFormDefinitionTenantIdForDeployment(String deploymentId, String newTenantId);

    void deleteFormDefinitionsByDeploymentId(String deploymentId);

}