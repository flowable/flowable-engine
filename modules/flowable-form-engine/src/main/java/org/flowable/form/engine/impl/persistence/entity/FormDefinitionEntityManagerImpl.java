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

import org.flowable.common.engine.impl.persistence.entity.data.DataManager;
import org.flowable.form.api.FormDefinition;
import org.flowable.form.engine.FormEngineConfiguration;
import org.flowable.form.engine.impl.FormDefinitionQueryImpl;
import org.flowable.form.engine.impl.persistence.entity.data.FormDefinitionDataManager;

/**
 * @author Tijs Rademakers
 * @author Joram Barrez
 */
public class FormDefinitionEntityManagerImpl extends AbstractEntityManager<FormDefinitionEntity> implements FormDefinitionEntityManager {

    protected FormDefinitionDataManager formDefinitionDataManager;

    public FormDefinitionEntityManagerImpl(FormEngineConfiguration formEngineConfiguration, FormDefinitionDataManager formDefinitionDataManager) {
        super(formEngineConfiguration);
        this.formDefinitionDataManager = formDefinitionDataManager;
    }

    @Override
    protected DataManager<FormDefinitionEntity> getDataManager() {
        return formDefinitionDataManager;
    }

    @Override
    public FormDefinitionEntity findLatestFormDefinitionByKey(String formDefinitionKey) {
        return formDefinitionDataManager.findLatestFormDefinitionByKey(formDefinitionKey);
    }

    @Override
    public FormDefinitionEntity findLatestFormDefinitionByKeyAndTenantId(String formDefinitionKey, String tenantId) {
        return formDefinitionDataManager.findLatestFormDefinitionByKeyAndTenantId(formDefinitionKey, tenantId);
    }

    @Override
    public void deleteFormDefinitionsByDeploymentId(String deploymentId) {
        formDefinitionDataManager.deleteFormDefinitionsByDeploymentId(deploymentId);
    }

    @Override
    public List<FormDefinition> findFormDefinitionsByQueryCriteria(FormDefinitionQueryImpl formQuery) {
        return formDefinitionDataManager.findFormDefinitionsByQueryCriteria(formQuery);
    }

    @Override
    public long findFormDefinitionCountByQueryCriteria(FormDefinitionQueryImpl formQuery) {
        return formDefinitionDataManager.findFormDefinitionCountByQueryCriteria(formQuery);
    }

    @Override
    public FormDefinitionEntity findFormDefinitionByDeploymentAndKey(String deploymentId, String formDefinitionKey) {
        return formDefinitionDataManager.findFormDefinitionByDeploymentAndKey(deploymentId, formDefinitionKey);
    }

    @Override
    public FormDefinitionEntity findFormDefinitionByDeploymentAndKeyAndTenantId(String deploymentId, String formDefinitionKey, String tenantId) {
        return formDefinitionDataManager.findFormDefinitionByDeploymentAndKeyAndTenantId(deploymentId, formDefinitionKey, tenantId);
    }

    @Override
    public FormDefinitionEntity findFormDefinitionByKeyAndVersionAndTenantId(String formDefinitionKey, Integer formVersion, String tenantId) {
        if (tenantId == null || FormEngineConfiguration.NO_TENANT_ID.equals(tenantId)) {
            return formDefinitionDataManager.findFormDefinitionByKeyAndVersion(formDefinitionKey, formVersion);
        } else {
            return formDefinitionDataManager.findFormDefinitionByKeyAndVersionAndTenantId(formDefinitionKey, formVersion, tenantId);
        }
    }

    @Override
    public List<FormDefinition> findFormDefinitionsByNativeQuery(Map<String, Object> parameterMap) {
        return formDefinitionDataManager.findFormDefinitionsByNativeQuery(parameterMap);
    }

    @Override
    public long findFormDefinitionCountByNativeQuery(Map<String, Object> parameterMap) {
        return formDefinitionDataManager.findFormDefinitionCountByNativeQuery(parameterMap);
    }

    @Override
    public void updateFormDefinitionTenantIdForDeployment(String deploymentId, String newTenantId) {
        formDefinitionDataManager.updateFormDefinitionTenantIdForDeployment(deploymentId, newTenantId);
    }

    public FormDefinitionDataManager getFormDefinitionDataManager() {
        return formDefinitionDataManager;
    }

    public void setFormDefinitionDataManager(FormDefinitionDataManager formDefinitionDataManager) {
        this.formDefinitionDataManager = formDefinitionDataManager;
    }

}
