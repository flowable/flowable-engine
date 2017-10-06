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
package org.flowable.cmmn.engine.configurator.impl.deployer;

import java.util.Map;

import org.flowable.cmmn.engine.CmmnRepositoryService;
import org.flowable.cmmn.engine.configurator.CmmnEngineConfigurator;
import org.flowable.cmmn.engine.repository.CmmnDeploymentBuilder;
import org.flowable.engine.impl.persistence.deploy.Deployer;
import org.flowable.engine.impl.persistence.entity.DeploymentEntity;
import org.flowable.engine.impl.persistence.entity.ResourceEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Joram Barrez
 */
public class CmmnDeployer implements Deployer {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(CmmnDeployer.class);
    
    protected CmmnEngineConfigurator cmmnEngineConfigurator;
    
    public CmmnDeployer(CmmnEngineConfigurator cmmnEngineConfigurator) {
        this.cmmnEngineConfigurator = cmmnEngineConfigurator;
    }

    @Override
    public void deploy(DeploymentEntity deployment, Map<String, Object> deploymentSettings) {
        if (!deployment.isNew()) {
            return;
        }

        LOGGER.debug("CmmnDeployer: processing deployment {}", deployment.getName());

        CmmnDeploymentBuilder cmmnDeploymentBuilder = null;
        Map<String, ResourceEntity> resources = deployment.getResources();
        for (String resourceName : resources.keySet()) {
            if (org.flowable.cmmn.engine.impl.deployer.CmmnDeployer.isCmmnResource(resourceName)) {
                LOGGER.info("CmmnDeployer: processing resource {}", resourceName);
                if (cmmnDeploymentBuilder == null) {
                    CmmnRepositoryService cmmnRepositoryService = cmmnEngineConfigurator.getCmmnEngine().getCmmnRepositoryService();
                    cmmnDeploymentBuilder = cmmnRepositoryService.createDeployment();
                }
                cmmnDeploymentBuilder.addBytes(resourceName, resources.get(resourceName).getBytes());
            }
        }

        if (cmmnDeploymentBuilder != null) {
            cmmnDeploymentBuilder.parentDeploymentId(deployment.getId());
            if (deployment.getTenantId() != null && deployment.getTenantId().length() > 0) {
                cmmnDeploymentBuilder.tenantId(deployment.getTenantId());
            }
            cmmnDeploymentBuilder.deploy();
        }
    }

}
