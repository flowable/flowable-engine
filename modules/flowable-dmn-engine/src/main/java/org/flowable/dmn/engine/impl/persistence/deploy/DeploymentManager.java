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
package org.flowable.dmn.engine.impl.persistence.deploy;

import java.util.List;
import java.util.Map;

import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.common.engine.impl.persistence.deploy.DeploymentCache;
import org.flowable.dmn.api.DmnDecision;
import org.flowable.dmn.engine.DmnEngineConfiguration;
import org.flowable.dmn.engine.impl.DecisionQueryImpl;
import org.flowable.dmn.engine.impl.persistence.entity.DecisionEntity;
import org.flowable.dmn.engine.impl.persistence.entity.DecisionEntityManager;
import org.flowable.dmn.engine.impl.persistence.entity.DmnDeploymentEntity;
import org.flowable.dmn.engine.impl.persistence.entity.DmnDeploymentEntityManager;
import org.flowable.dmn.engine.impl.persistence.entity.DmnResourceEntity;

/**
 * @author Tijs Rademakers
 * @author Joram Barrez
 * @author Yvo Swillens
 */
public class DeploymentManager {

    protected DmnEngineConfiguration engineConfig;
    protected DeploymentCache<DecisionCacheEntry> decisionCache;

    protected List<Deployer> deployers;
    protected DecisionEntityManager decisionEntityManager;
    protected DmnDeploymentEntityManager deploymentEntityManager;

    public DeploymentManager(DeploymentCache<DecisionCacheEntry> decisionCache, DmnEngineConfiguration engineConfig) {
        this.decisionCache = decisionCache;
        this.engineConfig = engineConfig;
    }

    public void deploy(DmnDeploymentEntity deployment) {
        deploy(deployment, null);
    }

    public void deploy(DmnDeploymentEntity deployment, Map<String, Object> deploymentSettings) {
        for (Deployer deployer : deployers) {
            deployer.deploy(deployment, deploymentSettings);
        }
    }

    public DecisionEntity findDeployedDecisionById(String decisionId) {
        if (decisionId == null) {
            throw new FlowableException("Invalid decision id : null");
        }

        // first try the cache
        DecisionCacheEntry cacheEntry = decisionCache.get(decisionId);
        DecisionEntity decision = cacheEntry != null ? cacheEntry.getDecisionEntity() : null;

        if (decision == null) {
            decision = engineConfig.getDecisionEntityManager().findById(decisionId);
            if (decision == null) {
                throw new FlowableObjectNotFoundException("no decision found with id '" + decisionId + "'");
            }
            decision = resolveDecision(decision).getDecisionEntity();
        }
        return decision;
    }

    public DecisionEntity findDeployedLatestDefinitionByKey(String definitionKey) {
        DecisionEntity definition = decisionEntityManager.findLatestDecisionByKey(definitionKey);

        if (definition == null) {
            throw new FlowableObjectNotFoundException("no decisions deployed with key '" + definitionKey + "'");
        }
        definition = resolveDecision(definition).getDecisionEntity();
        return definition;
    }

    public DecisionEntity findDeployedLatestDefinitionByKeyAndTenantId(String definitionKey, String tenantId) {
        DecisionEntity definition = decisionEntityManager.findLatestDecisionByKeyAndTenantId(definitionKey, tenantId);

        if (definition == null) {
            throw new FlowableObjectNotFoundException("no decisions deployed with key '" + definitionKey + "' for tenant identifier '" + tenantId + "'");
        }
        definition = resolveDecision(definition).getDecisionEntity();
        return definition;
    }

    public DecisionEntity findDeployedLatestDecisionByKeyAndDeploymentId(String definitionKey, String deploymentId) {
        DecisionEntity definition = decisionEntityManager.findDecisionByDeploymentAndKey(deploymentId, definitionKey);

        if (definition == null) {
            throw new FlowableObjectNotFoundException("no decisions deployed with key '" + definitionKey +
                            "' for deployment id '" + deploymentId + "'");
        }
        definition = resolveDecision(definition).getDecisionEntity();
        return definition;
    }

    public DecisionEntity findDeployedLatestDecisionByKeyDeploymentIdAndTenantId(String definitionKey,
            String deploymentId, String tenantId) {
        DecisionEntity definition = decisionEntityManager.findDecisionByDeploymentAndKeyAndTenantId(deploymentId, definitionKey, tenantId);

        if (definition == null) {
            throw new FlowableObjectNotFoundException("no decisions deployed with key '" + definitionKey +
                            "' for deployment id '" + deploymentId + "' and tenant identifier " + tenantId);
        }
        definition = resolveDecision(definition).getDecisionEntity();
        return definition;
    }

    public DecisionEntity findDeployedDefinitionByKeyAndVersionAndTenantId(String definitionKey, int definitionVersion, String tenantId) {
        DecisionEntity definition = decisionEntityManager.findDecisionByKeyAndVersionAndTenantId(definitionKey, definitionVersion, tenantId);

        if (definition == null) {
            throw new FlowableObjectNotFoundException("no decision deployed with key = '" + definitionKey + "' and version = '" + definitionVersion + "'");
        }

        definition = resolveDecision(definition).getDecisionEntity();
        return definition;
    }

    /**
     * Resolving the decision will fetch the DMN, parse it and store the {@link org.flowable.dmn.model.DmnDefinition} in memory.
     */
    public DecisionCacheEntry resolveDecision(DmnDecision decision) {
        String decisionId = decision.getId();
        String deploymentId = decision.getDeploymentId();

        DecisionCacheEntry cachedDecision = decisionCache.get(decisionId);

        if (cachedDecision == null) {
            DmnDeploymentEntity deployment = engineConfig.getDeploymentEntityManager().findById(deploymentId);
            List<DmnResourceEntity> resources = engineConfig.getResourceEntityManager().findResourcesByDeploymentId(deploymentId);
            for (DmnResourceEntity resource : resources) {
                deployment.addResource(resource);
            }

            deployment.setNew(false);
            deploy(deployment, null);
            cachedDecision = decisionCache.get(decisionId);

            if (cachedDecision == null) {
                throw new FlowableException("deployment '" + deploymentId + "' didn't put decision '" + decisionId + "' in the cache");
            }
        }
        return cachedDecision;
    }

    public void removeDeployment(String deploymentId) {

        DmnDeploymentEntity deployment = deploymentEntityManager.findById(deploymentId);
        if (deployment == null) {
            throw new FlowableObjectNotFoundException("Could not find a deployment with id '" + deploymentId + "'.");
        }

        // Remove any dmn definition from the cache
        List<DmnDecision> definitions = new DecisionQueryImpl().deploymentId(deploymentId).list();

        // Delete data
        deploymentEntityManager.deleteDeployment(deploymentId);

        for (DmnDecision definition : definitions) {
            decisionCache.remove(definition.getId());
        }
    }

    public List<Deployer> getDeployers() {
        return deployers;
    }

    public void setDeployers(List<Deployer> deployers) {
        this.deployers = deployers;
    }

    public DeploymentCache<DecisionCacheEntry> getDecisionCache() {
        return decisionCache;
    }

    public void setDecisionCache(DeploymentCache<DecisionCacheEntry> decisionCache) {
        this.decisionCache = decisionCache;
    }

    public DecisionEntityManager getDecisionEntityManager() {
        return decisionEntityManager;
    }

    public void setDecisionEntityManager(DecisionEntityManager decisionEntityManager) {
        this.decisionEntityManager = decisionEntityManager;
    }

    public DmnDeploymentEntityManager getDeploymentEntityManager() {
        return deploymentEntityManager;
    }

    public void setDeploymentEntityManager(DmnDeploymentEntityManager deploymentEntityManager) {
        this.deploymentEntityManager = deploymentEntityManager;
    }
}
