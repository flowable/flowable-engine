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

import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.form.api.FormDeployment;
import org.flowable.form.engine.FormEngineConfiguration;
import org.flowable.form.engine.impl.FormDeploymentQueryImpl;
import org.flowable.form.engine.impl.persistence.entity.FormDeploymentEntity;
import org.flowable.form.engine.impl.persistence.entity.FormResourceEntity;
import org.flowable.form.engine.impl.repository.FormDeploymentBuilderImpl;
import org.flowable.form.engine.impl.util.CommandContextUtil;

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

    @Override
    public FormDeployment execute(CommandContext commandContext) {

        FormDeploymentEntity deployment = deploymentBuilder.getDeployment();

        deployment.setDeploymentTime(CommandContextUtil.getFormEngineConfiguration().getClock().getCurrentTime());

        if (deploymentBuilder.isDuplicateFilterEnabled()) {

            List<FormDeployment> existingDeployments = new ArrayList<>();
            if (deployment.getTenantId() == null || FormEngineConfiguration.NO_TENANT_ID.equals(deployment.getTenantId())) {
                List<FormDeployment> deploymentEntities = new FormDeploymentQueryImpl(CommandContextUtil.getFormEngineConfiguration().getCommandExecutor()).deploymentName(deployment.getName()).listPage(0, 1);
                if (!deploymentEntities.isEmpty()) {
                    existingDeployments.add(deploymentEntities.get(0));
                }
            } else {
                List<FormDeployment> deploymentList = CommandContextUtil.getFormEngineConfiguration().getFormRepositoryService().createDeploymentQuery().deploymentName(deployment.getName())
                        .deploymentTenantId(deployment.getTenantId()).orderByDeploymentId().desc().list();

                if (!deploymentList.isEmpty()) {
                    existingDeployments.addAll(deploymentList);
                }
            }

            FormDeploymentEntity existingDeployment = null;
            if (!existingDeployments.isEmpty()) {
                existingDeployment = (FormDeploymentEntity) existingDeployments.get(0);

                Map<String, FormResourceEntity> resourceMap = new HashMap<>();
                List<FormResourceEntity> resourceList = CommandContextUtil.getResourceEntityManager().findResourcesByDeploymentId(existingDeployment.getId());
                for (FormResourceEntity resourceEntity : resourceList) {
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
        CommandContextUtil.getDeploymentEntityManager(commandContext).insert(deployment);

        // Actually deploy
        CommandContextUtil.getFormEngineConfiguration().getDeploymentManager().deploy(deployment);

        return deployment;
    }

    protected boolean deploymentsDiffer(FormDeploymentEntity deployment, FormDeploymentEntity saved) {

        if (deployment.getResources() == null || saved.getResources() == null) {
            return true;
        }

        Map<String, FormResourceEntity> resources = deployment.getResources();
        Map<String, FormResourceEntity> savedResources = saved.getResources();

        for (String resourceName : resources.keySet()) {
            FormResourceEntity savedResource = savedResources.get(resourceName);

            if (savedResource == null) {
                return true;
            }

            FormResourceEntity resource = resources.get(resourceName);

            byte[] bytes = resource.getBytes();
            byte[] savedBytes = savedResource.getBytes();
            if (!Arrays.equals(bytes, savedBytes)) {
                return true;
            }
        }
        return false;
    }
}
