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
package org.flowable.form.engine.impl.cmd;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flowable.form.api.FormDeployment;
import org.flowable.form.engine.FormEngineConfiguration;
import org.flowable.form.engine.impl.FormDeploymentQueryImpl;
import org.flowable.form.engine.impl.interceptor.Command;
import org.flowable.form.engine.impl.interceptor.CommandContext;
import org.flowable.form.engine.impl.persistence.entity.FormDeploymentEntity;
import org.flowable.form.engine.impl.persistence.entity.ResourceEntity;
import org.flowable.form.engine.impl.repository.FormDeploymentBuilderImpl;

/**
 * @author Tijs Rademakers
 * @author Joram Barrez
 */
public class DeployCmd<T> implements Command<FormDeployment>, Serializable {

    private static final long serialVersionUID = 1L;
    protected FormDeploymentBuilderImpl deploymentBuilder;

    public DeployCmd(FormDeploymentBuilderImpl deploymentBuilder) {
        this.deploymentBuilder = deploymentBuilder;
    }

    public FormDeployment execute(CommandContext commandContext) {

        FormDeploymentEntity deployment = deploymentBuilder.getDeployment();

        deployment.setDeploymentTime(commandContext.getFormEngineConfiguration().getClock().getCurrentTime());

        if (deploymentBuilder.isDuplicateFilterEnabled()) {

            List<FormDeployment> existingDeployments = new ArrayList<FormDeployment>();
            if (deployment.getTenantId() == null || FormEngineConfiguration.NO_TENANT_ID.equals(deployment.getTenantId())) {
                List<FormDeployment> deploymentEntities = new FormDeploymentQueryImpl(commandContext.getFormEngineConfiguration().getCommandExecutor()).deploymentName(deployment.getName()).listPage(0, 1);
                if (!deploymentEntities.isEmpty()) {
                    existingDeployments.add(deploymentEntities.get(0));
                }
            } else {
                List<FormDeployment> deploymentList = commandContext.getFormEngineConfiguration().getFormRepositoryService().createDeploymentQuery().deploymentName(deployment.getName())
                        .deploymentTenantId(deployment.getTenantId()).orderByDeploymentId().desc().list();

                if (!deploymentList.isEmpty()) {
                    existingDeployments.addAll(deploymentList);
                }
            }

            FormDeploymentEntity existingDeployment = null;
            if (!existingDeployments.isEmpty()) {
                existingDeployment = (FormDeploymentEntity) existingDeployments.get(0);

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

        // Actually deploy
        commandContext.getFormEngineConfiguration().getDeploymentManager().deploy(deployment);

        return deployment;
    }

    protected boolean deploymentsDiffer(FormDeploymentEntity deployment, FormDeploymentEntity saved) {

        if (deployment.getResources() == null || saved.getResources() == null) {
            return true;
        }

        Map<String, ResourceEntity> resources = deployment.getResources();
        Map<String, ResourceEntity> savedResources = saved.getResources();

        for (String resourceName : resources.keySet()) {
            ResourceEntity savedResource = savedResources.get(resourceName);

            if (savedResource == null) {
                return true;
            }

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
