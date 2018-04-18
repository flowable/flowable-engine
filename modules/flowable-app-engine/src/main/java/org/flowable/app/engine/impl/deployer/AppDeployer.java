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

import java.util.Map;

import org.flowable.app.api.repository.AppModel;
import org.flowable.app.engine.AppEngineConfiguration;
import org.flowable.app.engine.impl.persistence.entity.AppDefinitionEntity;
import org.flowable.app.engine.impl.persistence.entity.AppDefinitionEntityManager;
import org.flowable.app.engine.impl.persistence.entity.AppDeploymentEntity;
import org.flowable.app.engine.impl.persistence.entity.deploy.AppDefinitionCacheEntry;
import org.flowable.app.engine.impl.util.CommandContextUtil;
import org.flowable.common.engine.api.repository.EngineDeployment;
import org.flowable.common.engine.api.repository.EngineResource;
import org.flowable.common.engine.impl.EngineDeployer;
import org.flowable.common.engine.impl.persistence.deploy.DeploymentCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Tijs Rademakers
 */
public class AppDeployer implements EngineDeployer {

    private static final Logger LOGGER = LoggerFactory.getLogger(AppDeployer.class);

    @Override
    public void deploy(EngineDeployment deployment, Map<String, Object> deploymentSettings) {
        LOGGER.debug("Processing app deployment {}", deployment.getName());

        AppEngineConfiguration appEngineConfiguration = CommandContextUtil.getAppEngineConfiguration();
        
        AppModel appResourceModel = null;
        AppDeploymentEntity deploymentEntity = (AppDeploymentEntity) deployment;
        Map<String, EngineResource> resources = deploymentEntity.getResources();
        
        for (String resourceName : resources.keySet()) {
            if (resourceName.endsWith(".app")) {
                LOGGER.info("Processing app resource {}", resourceName);

                EngineResource resourceEntity = resources.get(resourceName);
                byte[] resourceBytes = resourceEntity.getBytes();
                appResourceModel = appEngineConfiguration.getAppResourceConverter().convertAppResourceToModel(resourceBytes);
                
                if (deployment.isNew()) {
                    AppDefinitionEntity latestAppDefinition = getMostRecentVersionOfAppDefinition(appResourceModel, deployment.getTenantId());
                    int version = 1;
                    if (latestAppDefinition != null) {
                        version = latestAppDefinition.getVersion() + 1;
                    }
                    
                    AppDefinitionEntityManager appDefinitionEntityManager = appEngineConfiguration.getAppDefinitionEntityManager();
                    AppDefinitionEntity newAppDefinition = appEngineConfiguration.getAppDefinitionEntityManager().create();
                    newAppDefinition.setVersion(version);
                    newAppDefinition.setId(appEngineConfiguration.getIdGenerator().getNextId());
                    newAppDefinition.setKey(appResourceModel.getKey());
                    newAppDefinition.setName(appResourceModel.getName());
                    newAppDefinition.setDescription(appResourceModel.getDescription());
                    newAppDefinition.setTenantId(deployment.getTenantId());
                    newAppDefinition.setDeploymentId(deployment.getId());
                    newAppDefinition.setResourceName(resourceName);
                    appDefinitionEntityManager.insert(newAppDefinition, false);
                    updateCachingAndArtifacts(newAppDefinition, appResourceModel, deploymentEntity);
                    
                } else {
                    AppDefinitionEntity appDefinitionEntity = getPersistedInstanceOfAppDefinition(appResourceModel.getKey(), deployment.getId(), deployment.getTenantId());
                    updateCachingAndArtifacts(appDefinitionEntity, appResourceModel, deploymentEntity);
                }
                
                // there can only be one app definition per deployment
                break;
            }
        }
    }
    
    protected AppDefinitionEntity getMostRecentVersionOfAppDefinition(AppModel appModel, String tenantId) {
        AppDefinitionEntityManager appDefinitionEntityManager = CommandContextUtil.getAppDefinitionEntityManager();
        AppDefinitionEntity existingAppDefinition = null;
        if (tenantId != null && !tenantId.equals(AppEngineConfiguration.NO_TENANT_ID)) {
            existingAppDefinition = appDefinitionEntityManager.findLatestAppDefinitionByKeyAndTenantId(appModel.getKey(), tenantId);
        } else {
            existingAppDefinition = appDefinitionEntityManager.findLatestAppDefinitionByKey(appModel.getKey());
        }
        
        return existingAppDefinition;
    }
    
    protected AppDefinitionEntity getPersistedInstanceOfAppDefinition(String key, String deploymentId, String tenantId) {
        AppDefinitionEntityManager appDefinitionEntityManager = CommandContextUtil.getAppDefinitionEntityManager();
        AppDefinitionEntity persistedAppDefinitionEntity = null;
        if (tenantId == null || AppEngineConfiguration.NO_TENANT_ID.equals(tenantId)) {
            persistedAppDefinitionEntity = appDefinitionEntityManager.findAppDefinitionByDeploymentAndKey(deploymentId, key);
        } else {
            persistedAppDefinitionEntity = appDefinitionEntityManager.findAppDefinitionByDeploymentAndKeyAndTenantId(deploymentId, key, tenantId);
        }
        return persistedAppDefinitionEntity;
    }
    
    protected void updateCachingAndArtifacts(AppDefinitionEntity appDefinition, AppModel appResourceModel, AppDeploymentEntity deployment) {
        AppEngineConfiguration appEngineConfiguration = CommandContextUtil.getAppEngineConfiguration();
        DeploymentCache<AppDefinitionCacheEntry> appDefinitionCache = appEngineConfiguration.getAppDefinitionCache();
        
        AppDefinitionCacheEntry cacheEntry = new AppDefinitionCacheEntry(appDefinition, appResourceModel);
        appDefinitionCache.add(appDefinition.getId(), cacheEntry);

        deployment.addDeployedArtifact(appDefinition);
    }
}
