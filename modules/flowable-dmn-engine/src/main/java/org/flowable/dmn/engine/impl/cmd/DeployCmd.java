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

import org.apache.commons.lang3.StringUtils;
import org.flowable.common.engine.api.repository.EngineResource;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.dmn.api.DmnDeployment;
import org.flowable.dmn.engine.DmnEngineConfiguration;
import org.flowable.dmn.engine.impl.DeploymentSettings;
import org.flowable.dmn.engine.impl.DmnDeploymentQueryImpl;
import org.flowable.dmn.engine.impl.persistence.entity.DmnDeploymentEntity;
import org.flowable.dmn.engine.impl.persistence.entity.DmnResourceEntity;
import org.flowable.dmn.engine.impl.repository.DmnDeploymentBuilderImpl;
import org.flowable.dmn.engine.impl.util.CommandContextUtil;

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

    @Override
    public DmnDeployment execute(CommandContext commandContext) {

        DmnDeploymentEntity deployment = deploymentBuilder.getDeployment();

        DmnEngineConfiguration dmnEngineConfiguration = CommandContextUtil.getDmnEngineConfiguration(commandContext);
        deployment.setDeploymentTime(dmnEngineConfiguration.getClock().getCurrentTime());

        if (deploymentBuilder.isDuplicateFilterEnabled()) {

            List<DmnDeployment> existingDeployments = new ArrayList<>();
            if (deployment.getTenantId() == null || DmnEngineConfiguration.NO_TENANT_ID.equals(deployment.getTenantId())) {
                List<DmnDeployment> deploymentEntities = new DmnDeploymentQueryImpl(dmnEngineConfiguration.getCommandExecutor())
                        .deploymentName(deployment.getName())
                        .orderByDeploymentTime().desc()
                        .listPage(0, 1);
                if (!deploymentEntities.isEmpty()) {
                    existingDeployments.add(deploymentEntities.get(0));
                }
            } else {
                List<DmnDeployment> deploymentList = dmnEngineConfiguration.getDmnRepositoryService().createDeploymentQuery()
                        .deploymentName(deployment.getName())
                        .deploymentTenantId(deployment.getTenantId())
                        .orderByDeploymentTime().desc()
                        .listPage(0, 1);

                if (!deploymentList.isEmpty()) {
                    existingDeployments.addAll(deploymentList);
                }
            }

            if (!existingDeployments.isEmpty()) {
                DmnDeploymentEntity existingDeployment = (DmnDeploymentEntity) existingDeployments.get(0);

                Map<String, EngineResource> resourceMap = new HashMap<>();
                List<DmnResourceEntity> resourceList = dmnEngineConfiguration.getResourceEntityManager().findResourcesByDeploymentId(existingDeployment.getId());
                for (DmnResourceEntity resourceEntity : resourceList) {
                    resourceMap.put(resourceEntity.getName(), resourceEntity);
                }
                existingDeployment.setResources(resourceMap);
                
                if (!deploymentsDiffer(deployment, existingDeployment)) {
                    return existingDeployment;
                }
            }
        }

        deployment.setNew(true);

        // Save the data
        dmnEngineConfiguration.getDeploymentEntityManager().insert(deployment);

        if (StringUtils.isEmpty(deployment.getParentDeploymentId())) {
            // If no parent deployment id is set then set the current ID as the parent
            // If something was deployed via this command than this deployment would
            // be a parent deployment to other potential child deployments
            deployment.setParentDeploymentId(deployment.getId());
        }

        // Deployment settings
        Map<String, Object> deploymentSettings = new HashMap<>();
        deploymentSettings.put(DeploymentSettings.IS_DMN_XSD_VALIDATION_ENABLED, deploymentBuilder.isDmnXsdValidationEnabled());

        // Actually deploy
        dmnEngineConfiguration.getDeploymentManager().deploy(deployment, deploymentSettings);

        return deployment;
    }

    protected boolean deploymentsDiffer(DmnDeploymentEntity deployment, DmnDeploymentEntity saved) {

        if (deployment.getResources() == null || saved.getResources() == null) {
            return true;
        }

        Map<String, EngineResource> resources = deployment.getResources();
        Map<String, EngineResource> savedResources = saved.getResources();

        for (String resourceName : resources.keySet()) {
            EngineResource savedResource = savedResources.get(resourceName);

            if (savedResource == null) {
                return true;
            }

            if (!savedResource.isGenerated()) {
                EngineResource resource = resources.get(resourceName);

                byte[] bytes = resource.getBytes();
                byte[] savedBytes = savedResource.getBytes();
                if (!Arrays.equals(bytes, savedBytes)) {
                    return true;
                }
            }
        }
        return false;
    }
}
