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

package org.flowable.app.engine.impl.deployer;

import java.util.List;
import java.util.Map;

import org.flowable.app.api.repository.AppDefinition;
import org.flowable.app.engine.AppEngineConfiguration;
import org.flowable.app.engine.impl.persistence.entity.AppDefinitionEntity;
import org.flowable.app.engine.impl.persistence.entity.AppDefinitionEntityManager;
import org.flowable.app.engine.impl.persistence.entity.AppDeploymentEntity;
import org.flowable.app.engine.impl.persistence.entity.AppDeploymentEntityManager;
import org.flowable.app.engine.impl.persistence.entity.deploy.AppDefinitionCacheEntry;
import org.flowable.app.engine.impl.repository.AppDefinitionQueryImpl;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.common.engine.api.repository.EngineDeployment;
import org.flowable.common.engine.impl.EngineDeployer;
import org.flowable.common.engine.impl.persistence.deploy.DeploymentCache;

public class AppDeploymentManager {

    protected DeploymentCache<AppDefinitionCacheEntry> appDefinitionCache;
    protected List<EngineDeployer> deployers;
    protected AppEngineConfiguration appEngineConfiguration;
    protected AppDeploymentEntityManager deploymentEntityManager;
    protected AppDefinitionEntityManager appDefinitionEntityManager;

    public void deploy(EngineDeployment deployment) {
        deploy(deployment, null);
    }

    public void deploy(EngineDeployment deployment, Map<String, Object> deploymentSettings) {
        for (EngineDeployer deployer : deployers) {
            deployer.deploy(deployment, deploymentSettings);
        }
    }

    public AppDefinition findDeployedAppDefinitionById(String appDefinitionId) {
        if (appDefinitionId == null) {
            throw new FlowableIllegalArgumentException("Invalid app definition id : null");
        }

        AppDefinitionCacheEntry cacheEntry = appDefinitionCache.get(appDefinitionId);
        AppDefinition appDefinition = cacheEntry != null ? cacheEntry.getAppDefinition() : null;

        if (appDefinition == null) {
            appDefinition = appDefinitionEntityManager.findById(appDefinitionId);
            if (appDefinition == null) {
                throw new FlowableObjectNotFoundException("no deployed app definition found with id '" + appDefinitionId + "'", AppDefinition.class);
            }
            appDefinition = resolveAppDefinition(appDefinition).getAppDefinition();
        }
        return appDefinition;
    }

    public AppDefinition findDeployedLatestAppDefinitionByKey(String appDefinitionKey) {
        AppDefinition appDefinition = appDefinitionEntityManager.findLatestAppDefinitionByKey(appDefinitionKey);

        if (appDefinition == null) {
            throw new FlowableObjectNotFoundException("no apps deployed with key '" + appDefinitionKey + "'", AppDefinition.class);
        }
        appDefinition = resolveAppDefinition(appDefinition).getAppDefinition();
        return appDefinition;
    }

    public AppDefinition findDeployedLatestAppDefinitionByKeyAndTenantId(String appDefinitionKey, String tenantId) {
        AppDefinition appDefinition = appDefinitionEntityManager.findLatestAppDefinitionByKeyAndTenantId(appDefinitionKey, tenantId);
        if (appDefinition == null) {
            throw new FlowableObjectNotFoundException("no apps deployed with key '" + appDefinitionKey + "' for tenant identifier '" + tenantId + "'", AppDefinition.class);
        }
        appDefinition = resolveAppDefinition(appDefinition).getAppDefinition();
        return appDefinition;
    }

    public AppDefinition findDeployedAppDefinitionByKeyAndVersionAndTenantId(String caseDefinitionKey, Integer caseDefinitionVersion, String tenantId) {
        AppDefinition appDefinition = (AppDefinitionEntity) appDefinitionEntityManager
                .findAppDefinitionByKeyAndVersionAndTenantId(caseDefinitionKey, caseDefinitionVersion, tenantId);
        if (appDefinition == null) {
            throw new FlowableObjectNotFoundException("no casees deployed with key = '" + caseDefinitionKey + "' and version = '" + caseDefinitionVersion + "'", AppDefinition.class);
        }
        appDefinition = resolveAppDefinition(appDefinition).getAppDefinition();
        return appDefinition;
    }

    public AppDefinitionCacheEntry resolveAppDefinition(AppDefinition appDefinition) {
        String appDefinitionId = appDefinition.getId();
        String deploymentId = appDefinition.getDeploymentId();

        AppDefinitionCacheEntry cachedAppDefinition = appDefinitionCache.get(appDefinitionId);

        if (cachedAppDefinition == null) {
            AppDeploymentEntity deployment = deploymentEntityManager.findById(deploymentId);
            deployment.setNew(false);
            deploy(deployment, null);
            cachedAppDefinition = appDefinitionCache.get(appDefinitionId);

            if (cachedAppDefinition == null) {
                throw new FlowableException("deployment '" + deploymentId + "' didn't put app definition '" + appDefinitionId + "' in the cache");
            }
        }
        return cachedAppDefinition;
    }
    
    public void removeDeployment(String deploymentId) {
        removeDeployment(deploymentId, true);
    }
    
    public void removeDeployment(String deploymentId, boolean cascade) {
        AppDeploymentEntity deployment = deploymentEntityManager.findById(deploymentId);
        if (deployment == null) {
            throw new FlowableObjectNotFoundException("Could not find a deployment with id '" + deploymentId + "'.", AppDeploymentEntity.class);
        }
        
        for (AppDefinition appDefinition : new AppDefinitionQueryImpl().deploymentId(deploymentId).list()) {
            appDefinitionCache.remove(appDefinition.getId());
        }
        
        deploymentEntityManager.deleteDeploymentAndRelatedData(deploymentId, cascade);
    }

    public List<EngineDeployer> getDeployers() {
        return deployers;
    }

    public void setDeployers(List<EngineDeployer> deployers) {
        this.deployers = deployers;
    }

    public DeploymentCache<AppDefinitionCacheEntry> getAppDefinitionCache() {
        return appDefinitionCache;
    }

    public void setAppDefinitionCache(DeploymentCache<AppDefinitionCacheEntry> appDefinitionCache) {
        this.appDefinitionCache = appDefinitionCache;
    }

    public AppEngineConfiguration getAppEngineConfiguration() {
        return appEngineConfiguration;
    }

    public void setAppEngineConfiguration(AppEngineConfiguration appEngineConfiguration) {
        this.appEngineConfiguration = appEngineConfiguration;
    }

    public AppDefinitionEntityManager getAppDefinitionEntityManager() {
        return appDefinitionEntityManager;
    }

    public void setAppDefinitionEntityManager(AppDefinitionEntityManager appDefinitionEntityManager) {
        this.appDefinitionEntityManager = appDefinitionEntityManager;
    }

    public AppDeploymentEntityManager getDeploymentEntityManager() {
        return deploymentEntityManager;
    }

    public void setDeploymentEntityManager(AppDeploymentEntityManager deploymentEntityManager) {
        this.deploymentEntityManager = deploymentEntityManager;
    }
    
}
