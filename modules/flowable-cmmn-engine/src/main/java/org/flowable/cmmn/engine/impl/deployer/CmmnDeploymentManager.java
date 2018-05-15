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

package org.flowable.cmmn.engine.impl.deployer;

import java.util.List;
import java.util.Map;

import org.flowable.cmmn.api.repository.CaseDefinition;
import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.impl.persistence.entity.CaseDefinitionEntity;
import org.flowable.cmmn.engine.impl.persistence.entity.CaseDefinitionEntityManager;
import org.flowable.cmmn.engine.impl.persistence.entity.CmmnDeploymentEntity;
import org.flowable.cmmn.engine.impl.persistence.entity.CmmnDeploymentEntityManager;
import org.flowable.cmmn.engine.impl.persistence.entity.deploy.CaseDefinitionCacheEntry;
import org.flowable.cmmn.engine.impl.repository.CaseDefinitionQueryImpl;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.common.engine.api.repository.EngineDeployment;
import org.flowable.common.engine.impl.EngineDeployer;
import org.flowable.common.engine.impl.persistence.deploy.DeploymentCache;

public class CmmnDeploymentManager {

    protected DeploymentCache<CaseDefinitionCacheEntry> caseDefinitionCache;
    protected List<EngineDeployer> deployers;
    protected CmmnEngineConfiguration cmmnEngineConfiguration;
    protected CmmnDeploymentEntityManager deploymentEntityManager;
    protected CaseDefinitionEntityManager caseDefinitionEntityManager;

    public void deploy(EngineDeployment deployment) {
        deploy(deployment, null);
    }

    public void deploy(EngineDeployment deployment, Map<String, Object> deploymentSettings) {
        for (EngineDeployer deployer : deployers) {
            deployer.deploy(deployment, deploymentSettings);
        }
    }

    public CaseDefinition findDeployedCaseDefinitionById(String caseDefinitionId) {
        if (caseDefinitionId == null) {
            throw new FlowableIllegalArgumentException("Invalid case definition id : null");
        }

        CaseDefinitionCacheEntry cacheEntry = caseDefinitionCache.get(caseDefinitionId);
        CaseDefinition caseDefinition = cacheEntry != null ? cacheEntry.getCaseDefinition() : null;

        if (caseDefinition == null) {
            caseDefinition = caseDefinitionEntityManager.findById(caseDefinitionId);
            if (caseDefinition == null) {
                throw new FlowableObjectNotFoundException("no deployed case definition found with id '" + caseDefinitionId + "'", CaseDefinition.class);
            }
            caseDefinition = resolveCaseDefinition(caseDefinition).getCaseDefinition();
        }
        return caseDefinition;
    }

    public CaseDefinition findDeployedLatestCaseDefinitionByKey(String caseDefinitionKey) {
        CaseDefinition caseDefinition = caseDefinitionEntityManager.findLatestCaseDefinitionByKey(caseDefinitionKey);

        if (caseDefinition == null) {
            throw new FlowableObjectNotFoundException("no cases deployed with key '" + caseDefinitionKey + "'", CaseDefinition.class);
        }
        caseDefinition = resolveCaseDefinition(caseDefinition).getCaseDefinition();
        return caseDefinition;
    }

    public CaseDefinition findDeployedLatestCaseDefinitionByKeyAndTenantId(String caseDefinitionKey, String tenantId) {
        CaseDefinition caseDefinition = caseDefinitionEntityManager.findLatestCaseDefinitionByKeyAndTenantId(caseDefinitionKey, tenantId);
        if (caseDefinition == null) {
            throw new FlowableObjectNotFoundException("no cases deployed with key '" + caseDefinitionKey + "' for tenant identifier '" + tenantId + "'", CaseDefinition.class);
        }
        caseDefinition = resolveCaseDefinition(caseDefinition).getCaseDefinition();
        return caseDefinition;
    }

    public CaseDefinition findDeployedCaseDefinitionByKeyAndVersionAndTenantId(String caseDefinitionKey, Integer caseDefinitionVersion, String tenantId) {
        CaseDefinition caseDefinition = (CaseDefinitionEntity) caseDefinitionEntityManager
                .findCaseDefinitionByKeyAndVersionAndTenantId(caseDefinitionKey, caseDefinitionVersion, tenantId);
        if (caseDefinition == null) {
            throw new FlowableObjectNotFoundException("no cases deployed with key = '" + caseDefinitionKey + "' and version = '" + caseDefinitionVersion + "'", CaseDefinition.class);
        }
        caseDefinition = resolveCaseDefinition(caseDefinition).getCaseDefinition();
        return caseDefinition;
    }

    public CaseDefinitionCacheEntry resolveCaseDefinition(CaseDefinition caseDefinition) {
        String caseDefinitionId = caseDefinition.getId();
        String deploymentId = caseDefinition.getDeploymentId();

        CaseDefinitionCacheEntry cachedCaseDefinition = caseDefinitionCache.get(caseDefinitionId);

        if (cachedCaseDefinition == null) {
            CmmnDeploymentEntity deployment = deploymentEntityManager.findById(deploymentId);
            deployment.setNew(false);
            deploy(deployment, null);
            cachedCaseDefinition = caseDefinitionCache.get(caseDefinitionId);

            if (cachedCaseDefinition == null) {
                throw new FlowableException("deployment '" + deploymentId + "' didn't put case definition '" + caseDefinitionId + "' in the cache");
            }
        }
        return cachedCaseDefinition;
    }
    
    public void removeDeployment(String deploymentId) {
        removeDeployment(deploymentId, true);
    }
    
    public void removeDeployment(String deploymentId, boolean cascade) {
        CmmnDeploymentEntity deployment = deploymentEntityManager.findById(deploymentId);
        if (deployment == null) {
            throw new FlowableObjectNotFoundException("Could not find a deployment with id '" + deploymentId + "'.", CmmnDeploymentEntity.class);
        }
        
        for (CaseDefinition caseDefinition : new CaseDefinitionQueryImpl().deploymentId(deploymentId).list()) {
            caseDefinitionCache.remove(caseDefinition.getId());
        }
        
        deploymentEntityManager.deleteDeploymentAndRelatedData(deploymentId, cascade);
    }

    public List<EngineDeployer> getDeployers() {
        return deployers;
    }

    public void setDeployers(List<EngineDeployer> deployers) {
        this.deployers = deployers;
    }

    public DeploymentCache<CaseDefinitionCacheEntry> getCaseDefinitionCache() {
        return caseDefinitionCache;
    }

    public void setCaseDefinitionCache(DeploymentCache<CaseDefinitionCacheEntry> caseDefinitionCache) {
        this.caseDefinitionCache = caseDefinitionCache;
    }

    public CmmnEngineConfiguration getCaseEngineConfiguration() {
        return cmmnEngineConfiguration;
    }

    public void setCmmnEngineConfiguration(CmmnEngineConfiguration cmmnEngineConfiguration) {
        this.cmmnEngineConfiguration = cmmnEngineConfiguration;
    }

    public CaseDefinitionEntityManager getCaseDefinitionEntityManager() {
        return caseDefinitionEntityManager;
    }

    public void setCaseDefinitionEntityManager(CaseDefinitionEntityManager caseDefinitionEntityManager) {
        this.caseDefinitionEntityManager = caseDefinitionEntityManager;
    }

    public CmmnDeploymentEntityManager getDeploymentEntityManager() {
        return deploymentEntityManager;
    }

    public void setDeploymentEntityManager(CmmnDeploymentEntityManager deploymentEntityManager) {
        this.deploymentEntityManager = deploymentEntityManager;
    }
    
}
