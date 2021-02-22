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

package org.flowable.app.engine.impl.persistence.entity;

import java.util.List;

import org.flowable.app.api.repository.AppDefinition;
import org.flowable.app.api.repository.AppDefinitionQuery;
import org.flowable.app.engine.AppEngineConfiguration;
import org.flowable.app.engine.impl.persistence.entity.data.AppDefinitionDataManager;
import org.flowable.app.engine.impl.repository.AppDefinitionQueryImpl;
import org.flowable.common.engine.impl.persistence.entity.AbstractEngineEntityManager;


/**
 * @author Tijs Rademakers
 */
public class AppDefinitionEntityManagerImpl
    extends AbstractEngineEntityManager<AppEngineConfiguration, AppDefinitionEntity, AppDefinitionDataManager>
    implements AppDefinitionEntityManager {

    public AppDefinitionEntityManagerImpl(AppEngineConfiguration appEngineConfiguration, AppDefinitionDataManager appDefinitionDataManager) {
        super(appEngineConfiguration, appDefinitionDataManager);
    }

    @Override
    public AppDefinitionEntity findLatestAppDefinitionByKey(String appDefinitionKey) {
        return dataManager.findLatestAppDefinitionByKey(appDefinitionKey);
    }

    @Override
    public AppDefinitionEntity findLatestAppDefinitionByKeyAndTenantId(String appDefinitionKey, String tenantId) {
        return dataManager.findLatestAppDefinitionByKeyAndTenantId(appDefinitionKey, tenantId);
    }

    @Override
    public AppDefinitionEntity findAppDefinitionByDeploymentAndKey(String deploymentId, String appDefinitionKey) {
        return dataManager.findAppDefinitionByDeploymentAndKey(deploymentId, appDefinitionKey);
    }

    @Override
    public AppDefinitionEntity findAppDefinitionByDeploymentAndKeyAndTenantId(String deploymentId, String appDefinitionKey, String tenantId) {
        return dataManager.findAppDefinitionByDeploymentAndKeyAndTenantId(deploymentId, appDefinitionKey, tenantId);
    }

    @Override
    public AppDefinition findAppDefinitionByKeyAndVersionAndTenantId(String appDefinitionKey, Integer appDefinitionVersion, String tenantId) {
        if (tenantId == null || AppEngineConfiguration.NO_TENANT_ID.equals(tenantId)) {
            return dataManager.findAppDefinitionByKeyAndVersion(appDefinitionKey, appDefinitionVersion);
        } else {
            return dataManager.findAppDefinitionByKeyAndVersionAndTenantId(appDefinitionKey, appDefinitionVersion, tenantId);
        }
    }
    
    @Override
    public void deleteAppDefinitionAndRelatedData(String appDefinitionId) {
        AppDefinitionEntity appDefinitionEntity = findById(appDefinitionId);
        delete(appDefinitionEntity);
    }
    
    @Override
    public AppDefinitionQuery createAppDefinitionQuery() {
        return new AppDefinitionQueryImpl(engineConfiguration.getCommandExecutor());
    }

    @Override
    public List<AppDefinition> findAppDefinitionsByQueryCriteria(AppDefinitionQuery appDefinitionQuery) {
        return dataManager.findAppDefinitionsByQueryCriteria((AppDefinitionQueryImpl) appDefinitionQuery);
    }

    @Override
    public long findAppDefinitionCountByQueryCriteria(AppDefinitionQuery appDefinitionQuery) {
        return dataManager.findAppDefinitionCountByQueryCriteria((AppDefinitionQueryImpl) appDefinitionQuery);
    }

}
