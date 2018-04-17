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
package org.flowable.form.engine.impl.persistence.deploy;

import java.util.List;

import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.common.engine.impl.persistence.deploy.DeploymentCache;
import org.flowable.form.api.FormDefinition;
import org.flowable.form.engine.FormEngineConfiguration;
import org.flowable.form.engine.impl.FormDefinitionQueryImpl;
import org.flowable.form.engine.impl.persistence.entity.FormDefinitionEntity;
import org.flowable.form.engine.impl.persistence.entity.FormDefinitionEntityManager;
import org.flowable.form.engine.impl.persistence.entity.FormDeploymentEntity;
import org.flowable.form.engine.impl.persistence.entity.FormDeploymentEntityManager;
import org.flowable.form.engine.impl.persistence.entity.FormResourceEntity;

/**
 * @author Tijs Rademakers
 * @author Joram Barrez
 */
public class DeploymentManager {

    protected FormEngineConfiguration engineConfig;
    protected DeploymentCache<FormDefinitionCacheEntry> formCache;

    protected List<Deployer> deployers;
    protected FormDefinitionEntityManager formDefinitionEntityManager;
    protected FormDeploymentEntityManager deploymentEntityManager;

    public DeploymentManager(DeploymentCache<FormDefinitionCacheEntry> formCache, FormEngineConfiguration engineConfig) {
        this.formCache = formCache;
        this.engineConfig = engineConfig;
    }

    public void deploy(FormDeploymentEntity deployment) {
        for (Deployer deployer : deployers) {
            deployer.deploy(deployment);
        }
    }

    public FormDefinitionEntity findDeployedFormDefinitionById(String formDefinitionId) {
        if (formDefinitionId == null) {
            throw new FlowableException("Invalid form definition id : null");
        }

        // first try the cache
        FormDefinitionCacheEntry cacheEntry = formCache.get(formDefinitionId);
        FormDefinitionEntity formDefinition = cacheEntry != null ? cacheEntry.getFormDefinitionEntity() : null;

        if (formDefinition == null) {
            formDefinition = engineConfig.getFormDefinitionEntityManager().findById(formDefinitionId);
            if (formDefinition == null) {
                throw new FlowableObjectNotFoundException("no deployed form definition found with id '" + formDefinitionId + "'");
            }
            formDefinition = resolveFormDefinition(formDefinition).getFormDefinitionEntity();
        }
        return formDefinition;
    }

    public FormDefinitionEntity findDeployedLatestFormDefinitionByKey(String formDefinitionKey) {
        FormDefinitionEntity formDefinition = formDefinitionEntityManager.findLatestFormDefinitionByKey(formDefinitionKey);

        if (formDefinition == null) {
            throw new FlowableObjectNotFoundException("no form definitions deployed with key '" + formDefinitionKey + "'");
        }
        formDefinition = resolveFormDefinition(formDefinition).getFormDefinitionEntity();
        return formDefinition;
    }

    public FormDefinitionEntity findDeployedLatestFormDefinitionByKeyAndTenantId(String formDefinitionKey, String tenantId) {
        FormDefinitionEntity formDefinition = formDefinitionEntityManager.findLatestFormDefinitionByKeyAndTenantId(formDefinitionKey, tenantId);

        if (formDefinition == null) {
            throw new FlowableObjectNotFoundException("no form definitions deployed with key '" + formDefinitionKey + "' for tenant identifier '" + tenantId + "'");
        }
        formDefinition = resolveFormDefinition(formDefinition).getFormDefinitionEntity();
        return formDefinition;
    }

    public FormDefinitionEntity findDeployedLatestFormDefinitionByKeyAndParentDeploymentId(String formDefinitionKey, String parentDeploymentId) {
        FormDefinitionEntity formDefinition = formDefinitionEntityManager.findLatestFormDefinitionByKeyAndParentDeploymentId(formDefinitionKey, parentDeploymentId);

        if (formDefinition == null) {
            throw new FlowableObjectNotFoundException("no form definitions deployed with key '" + formDefinitionKey +
                    "' for parent deployment id '" + parentDeploymentId + "'");
        }
        formDefinition = resolveFormDefinition(formDefinition).getFormDefinitionEntity();
        return formDefinition;
    }

    public FormDefinitionEntity findDeployedLatestFormDefinitionByKeyParentDeploymentIdAndTenantId(String formDefinitionKey, String parentDeploymentId, String tenantId) {
        FormDefinitionEntity formDefinition = formDefinitionEntityManager.findLatestFormDefinitionByKeyParentDeploymentIdAndTenantId(formDefinitionKey, parentDeploymentId, tenantId);

        if (formDefinition == null) {
            throw new FlowableObjectNotFoundException("no form definitions deployed with key '" + formDefinitionKey +
                    "' for parent deployment id '" + parentDeploymentId + "' and tenant identifier '" + tenantId + "'");
        }
        formDefinition = resolveFormDefinition(formDefinition).getFormDefinitionEntity();
        return formDefinition;
    }

    public FormDefinitionEntity findDeployedFormDefinitionByKeyAndVersionAndTenantId(String formDefinitionKey, int formVersion, String tenantId) {
        FormDefinitionEntity formDefinition = formDefinitionEntityManager.findFormDefinitionByKeyAndVersionAndTenantId(formDefinitionKey, formVersion, tenantId);

        if (formDefinition == null) {
            throw new FlowableObjectNotFoundException("no form definitions deployed with key = '" + formDefinitionKey + "' and version = '" + formVersion + "'");
        }

        formDefinition = resolveFormDefinition(formDefinition).getFormDefinitionEntity();
        return formDefinition;
    }

    /**
     * Resolving the decision will fetch the DMN, parse it and store the {@link FormDefinition} in memory.
     */
    public FormDefinitionCacheEntry resolveFormDefinition(FormDefinition formDefinition) {
        String formDefinitionId = formDefinition.getId();
        String deploymentId = formDefinition.getDeploymentId();

        FormDefinitionCacheEntry cachedForm = formCache.get(formDefinitionId);

        if (cachedForm == null) {
            FormDeploymentEntity deployment = engineConfig.getDeploymentEntityManager().findById(deploymentId);
            List<FormResourceEntity> resources = engineConfig.getResourceEntityManager().findResourcesByDeploymentId(deploymentId);
            for (FormResourceEntity resource : resources) {
                deployment.addResource(resource);
            }

            deployment.setNew(false);
            deploy(deployment);
            cachedForm = formCache.get(formDefinitionId);

            if (cachedForm == null) {
                throw new FlowableException("deployment '" + deploymentId + "' didn't put form definition '" + formDefinitionId + "' in the cache");
            }
        }
        return cachedForm;
    }

    public void removeDeployment(String deploymentId) {

        FormDeploymentEntity deployment = deploymentEntityManager.findById(deploymentId);
        if (deployment == null) {
            throw new FlowableObjectNotFoundException("Could not find a deployment with id '" + deploymentId + "'.");
        }

        // Remove any form definition from the cache
        List<FormDefinition> forms = new FormDefinitionQueryImpl().deploymentId(deploymentId).list();

        // Delete data
        deploymentEntityManager.deleteDeployment(deploymentId);

        for (FormDefinition form : forms) {
            formCache.remove(form.getId());
        }
    }

    public List<Deployer> getDeployers() {
        return deployers;
    }

    public void setDeployers(List<Deployer> deployers) {
        this.deployers = deployers;
    }

    public DeploymentCache<FormDefinitionCacheEntry> getFormCache() {
        return formCache;
    }

    public void setFormCache(DeploymentCache<FormDefinitionCacheEntry> formCache) {
        this.formCache = formCache;
    }

    public FormDefinitionEntityManager getFormDefinitionEntityManager() {
        return formDefinitionEntityManager;
    }

    public void setFormDefinitionEntityManager(FormDefinitionEntityManager formDefinitionEntityManager) {
        this.formDefinitionEntityManager = formDefinitionEntityManager;
    }

    public FormDeploymentEntityManager getDeploymentEntityManager() {
        return deploymentEntityManager;
    }

    public void setDeploymentEntityManager(FormDeploymentEntityManager deploymentEntityManager) {
        this.deploymentEntityManager = deploymentEntityManager;
    }
}
