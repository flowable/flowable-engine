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
package org.flowable.dmn.engine.deployer;

import java.util.Map;

import org.flowable.dmn.api.DmnDeploymentBuilder;
import org.flowable.dmn.api.DmnRepositoryService;
import org.flowable.engine.impl.persistence.deploy.Deployer;
import org.flowable.engine.impl.persistence.entity.DeploymentEntity;
import org.flowable.engine.impl.persistence.entity.ResourceEntity;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Tijs Rademakers
 */
public class DmnDeployer implements Deployer {

    private static final Logger LOGGER = LoggerFactory.getLogger(DmnDeployer.class);

    @Override
    public void deploy(DeploymentEntity deployment, Map<String, Object> deploymentSettings) {
        if (!deployment.isNew())
            return;

        LOGGER.debug("DmnDeployer: processing deployment {}", deployment.getName());

        DmnDeploymentBuilder dmnDeploymentBuilder = null;

        Map<String, ResourceEntity> resources = deployment.getResources();
        for (String resourceName : resources.keySet()) {
            if (resourceName.endsWith(".dmn")) {
                LOGGER.info("DmnDeployer: processing resource {}", resourceName);
                if (dmnDeploymentBuilder == null) {
                    DmnRepositoryService dmnRepositoryService = CommandContextUtil.getDmnRepositoryService();
                    dmnDeploymentBuilder = dmnRepositoryService.createDeployment();
                }

                dmnDeploymentBuilder.addDmnBytes(resourceName, resources.get(resourceName).getBytes());
            }
        }

        if (dmnDeploymentBuilder != null) {
            dmnDeploymentBuilder.parentDeploymentId(deployment.getId());
            if (deployment.getTenantId() != null && deployment.getTenantId().length() > 0) {
                dmnDeploymentBuilder.tenantId(deployment.getTenantId());
            }

            dmnDeploymentBuilder.deploy();
        }
    }
}
