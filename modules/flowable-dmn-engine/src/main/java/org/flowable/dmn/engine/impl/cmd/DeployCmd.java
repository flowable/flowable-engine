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
package org.flowable.dmn.engine.impl.cmd;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flowable.dmn.api.DmnDeployment;
import org.flowable.dmn.engine.DmnEngineConfiguration;
import org.flowable.dmn.engine.impl.DeploymentSettings;
import org.flowable.dmn.engine.impl.DmnDeploymentQueryImpl;
import org.flowable.dmn.engine.impl.interceptor.Command;
import org.flowable.dmn.engine.impl.interceptor.CommandContext;
import org.flowable.dmn.engine.impl.persistence.entity.DmnDeploymentEntity;
import org.flowable.dmn.engine.impl.persistence.entity.ResourceEntity;
import org.flowable.dmn.engine.impl.repository.DmnDeploymentBuilderImpl;

/**
 * @author Tijs Rademakers
 * @author Joram Barrez
 */
public class DeployCmd<T> implements Command<DmnDeployment>, Serializable {

    private static final long serialVersionUID = 1L;
    protected DmnDeploymentBuilderImpl deploymentBuilder;

    public DeployCmd(DmnDeploymentBuilderImpl deploymentBuilder) {
        this.deploymentBuilder = deploymentBuilder;
    }

    public DmnDeployment execute(CommandContext commandContext) {

        DmnDeploymentEntity deployment = deploymentBuilder.getDeployment();

        deployment.setDeploymentTime(commandContext.getDmnEngineConfiguration().getClock().getCurrentTime());

        if (deploymentBuilder.isDuplicateFilterEnabled()) {

            List<DmnDeployment> existingDeployments = new ArrayList<DmnDeployment>();
            if (deployment.getTenantId() == null || DmnEngineConfiguration.NO_TENANT_ID.equals(deployment.getTenantId())) {
                List<DmnDeployment> deploymentEntities = new DmnDeploymentQueryImpl(commandContext.getDmnEngineConfiguration().getCommandExecutor()).deploymentName(deployment.getName()).listPage(0, 1);
                if (!deploymentEntities.isEmpty()) {
                    existingDeployments.add(deploymentEntities.get(0));
                }
            } else {
                List<DmnDeployment> deploymentList = commandContext.getDmnEngineConfiguration().getDmnRepositoryService().createDeploymentQuery()
                        .deploymentName(deployment.getName())
                        .deploymentTenantId(deployment.getTenantId())
                        .orderByDeploymentId()
                        .desc()
                        .list();

                if (!deploymentList.isEmpty()) {
                    existingDeployments.addAll(deploymentList);
                }
            }

            DmnDeploymentEntity existingDeployment = null;
            if (!existingDeployments.isEmpty()) {
                existingDeployment = (DmnDeploymentEntity) existingDeployments.get(0);

                Map<String, ResourceEntity> resourceMap = new HashMap<String, ResourceEntity>();
                List<ResourceEntity> resourceList = commandContext.getResourceEntityManager().findResourcesByDeploymentId(existingDeployment.getId());
                for (ResourceEntity resourceEntity : resourceList) {
                    resourceMap.put(resourceEntity.getName(), resourceEntity);
                }
                existingDeployment.setResources(resourceMap);
            }

            if ((existingDeployment != null) && !deploymentsDiffer(deployment, existingDeployment)) {
                return existingDeployment;
            }
        }

        deployment.setNew(true);

        // Save the data
        commandContext.getDeploymentEntityManager().insert(deployment);

        // Deployment settings
        Map<String, Object> deploymentSettings = new HashMap<String, Object>();
        deploymentSettings.put(DeploymentSettings.IS_DMN_XSD_VALIDATION_ENABLED, deploymentBuilder.isDmnXsdValidationEnabled());

        // Actually deploy
        commandContext.getDmnEngineConfiguration().getDeploymentManager().deploy(deployment, deploymentSettings);

        return deployment;
    }

    protected boolean deploymentsDiffer(DmnDeploymentEntity deployment, DmnDeploymentEntity saved) {

        if (deployment.getResources() == null || saved.getResources() == null) {
            return true;
        }

        Map<String, ResourceEntity> resources = deployment.getResources();
        Map<String, ResourceEntity> savedResources = saved.getResources();

        for (String resourceName : resources.keySet()) {
            ResourceEntity savedResource = savedResources.get(resourceName);

            if (savedResource == null)
                return true;

            ResourceEntity resource = resources.get(resourceName);

            byte[] bytes = resource.getBytes();
            byte[] savedBytes = savedResource.getBytes();
            if (!Arrays.equals(bytes, savedBytes)) {
                return true;
            }
        }
        return false;
    }
}
