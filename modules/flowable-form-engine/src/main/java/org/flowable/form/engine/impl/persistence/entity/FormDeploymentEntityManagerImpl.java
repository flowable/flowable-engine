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
import org.flowable.form.api.FormDeployment;
import org.flowable.form.engine.FormEngineConfiguration;
import org.flowable.form.engine.impl.FormDeploymentQueryImpl;
import org.flowable.form.engine.impl.persistence.entity.data.FormDeploymentDataManager;

/**
 * @author Tijs Rademakers
 * @author Joram Barrez
 */
public class FormDeploymentEntityManagerImpl extends AbstractEntityManager<FormDeploymentEntity> implements FormDeploymentEntityManager {

    protected FormDeploymentDataManager deploymentDataManager;

    public FormDeploymentEntityManagerImpl(FormEngineConfiguration formEngineConfiguration, FormDeploymentDataManager deploymentDataManager) {
        super(formEngineConfiguration);
        this.deploymentDataManager = deploymentDataManager;
    }

    @Override
    protected DataManager<FormDeploymentEntity> getDataManager() {
        return deploymentDataManager;
    }

    @Override
    public void insert(FormDeploymentEntity deployment) {
        insert(deployment, true);
    }

    @Override
    public void insert(FormDeploymentEntity deployment, boolean fireEvent) {
        super.insert(deployment, fireEvent);

        for (FormResourceEntity resource : deployment.getResources().values()) {
            resource.setDeploymentId(deployment.getId());
            getResourceEntityManager().insert(resource);
        }
    }

    @Override
    public void deleteDeployment(String deploymentId) {
        deleteDecisionTablesForDeployment(deploymentId);
        getResourceEntityManager().deleteResourcesByDeploymentId(deploymentId);
        delete(findById(deploymentId));
    }

    protected void deleteDecisionTablesForDeployment(String deploymentId) {
        getFormDefinitionEntityManager().deleteFormDefinitionsByDeploymentId(deploymentId);
    }

    protected FormDefinitionEntity findLatestFormDefinition(FormDefinition formDefinition) {
        FormDefinitionEntity latestForm = null;
        if (formDefinition.getTenantId() != null && !FormEngineConfiguration.NO_TENANT_ID.equals(formDefinition.getTenantId())) {
            latestForm = getFormDefinitionEntityManager().findLatestFormDefinitionByKeyAndTenantId(formDefinition.getKey(), formDefinition.getTenantId());
        } else {
            latestForm = getFormDefinitionEntityManager().findLatestFormDefinitionByKey(formDefinition.getKey());
        }
        return latestForm;
    }

    @Override
    public long findDeploymentCountByQueryCriteria(FormDeploymentQueryImpl deploymentQuery) {
        return deploymentDataManager.findDeploymentCountByQueryCriteria(deploymentQuery);
    }

    @Override
    public List<FormDeployment> findDeploymentsByQueryCriteria(FormDeploymentQueryImpl deploymentQuery) {
        return deploymentDataManager.findDeploymentsByQueryCriteria(deploymentQuery);
    }

    @Override
    public List<String> getDeploymentResourceNames(String deploymentId) {
        return deploymentDataManager.getDeploymentResourceNames(deploymentId);
    }

    @Override
    public List<FormDeployment> findDeploymentsByNativeQuery(Map<String, Object> parameterMap) {
        return deploymentDataManager.findDeploymentsByNativeQuery(parameterMap);
    }

    @Override
    public long findDeploymentCountByNativeQuery(Map<String, Object> parameterMap) {
        return deploymentDataManager.findDeploymentCountByNativeQuery(parameterMap);
    }

    public FormDeploymentDataManager getDeploymentDataManager() {
        return deploymentDataManager;
    }

    public void setDeploymentDataManager(FormDeploymentDataManager deploymentDataManager) {
        this.deploymentDataManager = deploymentDataManager;
    }

}
