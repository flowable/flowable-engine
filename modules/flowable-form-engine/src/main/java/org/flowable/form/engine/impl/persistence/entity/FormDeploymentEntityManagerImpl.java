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

import org.flowable.common.engine.impl.persistence.entity.AbstractEngineEntityManager;
import org.flowable.form.api.FormDefinition;
import org.flowable.form.api.FormDeployment;
import org.flowable.form.engine.FormEngineConfiguration;
import org.flowable.form.engine.impl.FormDefinitionQueryImpl;
import org.flowable.form.engine.impl.FormDeploymentQueryImpl;
import org.flowable.form.engine.impl.persistence.entity.data.FormDeploymentDataManager;

/**
 * @author Tijs Rademakers
 * @author Joram Barrez
 */
public class FormDeploymentEntityManagerImpl
    extends AbstractEngineEntityManager<FormEngineConfiguration, FormDeploymentEntity, FormDeploymentDataManager>
    implements FormDeploymentEntityManager {

    public FormDeploymentEntityManagerImpl(FormEngineConfiguration formEngineConfiguration, FormDeploymentDataManager deploymentDataManager) {
        super(formEngineConfiguration, deploymentDataManager);
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
    public void deleteDeployment(String deploymentId, boolean cascade) {
        if (cascade) {
            List<FormDefinition> formDefinitions = new FormDefinitionQueryImpl().deploymentId(deploymentId).list();
            deleteFormInstancesForDefinitions(formDefinitions);
        }
        deleteFormDefinitionsForDeployment(deploymentId);
        getResourceEntityManager().deleteResourcesByDeploymentId(deploymentId);
        delete(findById(deploymentId));
    }

    protected void deleteFormInstancesForDefinitions(List<FormDefinition> formDefinitions) {
        for (FormDefinition formDefinition : formDefinitions) {
            getFormInstanceEntityManager().deleteFormInstancesByFormDefinitionId(formDefinition.getId());
        }
    }

    protected void deleteFormDefinitionsForDeployment(String deploymentId) {
        getFormDefinitionEntityManager().deleteFormDefinitionsByDeploymentId(deploymentId);
    }

    @Override
    public long findDeploymentCountByQueryCriteria(FormDeploymentQueryImpl deploymentQuery) {
        return dataManager.findDeploymentCountByQueryCriteria(deploymentQuery);
    }

    @Override
    public List<FormDeployment> findDeploymentsByQueryCriteria(FormDeploymentQueryImpl deploymentQuery) {
        return dataManager.findDeploymentsByQueryCriteria(deploymentQuery);
    }

    @Override
    public List<String> getDeploymentResourceNames(String deploymentId) {
        return dataManager.getDeploymentResourceNames(deploymentId);
    }

    @Override
    public List<FormDeployment> findDeploymentsByNativeQuery(Map<String, Object> parameterMap) {
        return dataManager.findDeploymentsByNativeQuery(parameterMap);
    }

    @Override
    public long findDeploymentCountByNativeQuery(Map<String, Object> parameterMap) {
        return dataManager.findDeploymentCountByNativeQuery(parameterMap);
    }

    protected FormResourceEntityManager getResourceEntityManager() {
        return engineConfiguration.getResourceEntityManager();
    }

    protected FormDefinitionEntityManager getFormDefinitionEntityManager() {
        return engineConfiguration.getFormDefinitionEntityManager();
    }

    protected FormInstanceEntityManager getFormInstanceEntityManager() {
        return engineConfiguration.getFormInstanceEntityManager();
    }
}
