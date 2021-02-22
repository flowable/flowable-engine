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
package org.flowable.eventregistry.impl.cmd;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.eventregistry.api.EventDeployment;
import org.flowable.eventregistry.impl.EventDeploymentQueryImpl;
import org.flowable.eventregistry.impl.EventRegistryEngineConfiguration;
import org.flowable.eventregistry.impl.persistence.entity.EventDeploymentEntity;
import org.flowable.eventregistry.impl.persistence.entity.EventResourceEntity;
import org.flowable.eventregistry.impl.repository.EventDeploymentBuilderImpl;
import org.flowable.eventregistry.impl.util.CommandContextUtil;

/**
 * @author Tijs Rademakers
 * @author Joram Barrez
 */
public class DeployCmd<T> implements Command<EventDeployment>, Serializable {

    private static final long serialVersionUID = 1L;
    protected EventDeploymentBuilderImpl deploymentBuilder;

    public DeployCmd(EventDeploymentBuilderImpl deploymentBuilder) {
        this.deploymentBuilder = deploymentBuilder;
    }

    @Override
    public EventDeployment execute(CommandContext commandContext) {

        EventDeploymentEntity deployment = deploymentBuilder.getDeployment();

        EventRegistryEngineConfiguration eventRegistryConfiguration = CommandContextUtil.getEventRegistryConfiguration(commandContext);
        deployment.setDeploymentTime(eventRegistryConfiguration.getClock().getCurrentTime());

        if (deploymentBuilder.isDuplicateFilterEnabled()) {

            List<EventDeployment> existingDeployments = new ArrayList<>();
            if (deployment.getTenantId() == null || EventRegistryEngineConfiguration.NO_TENANT_ID.equals(deployment.getTenantId())) {
                List<EventDeployment> deploymentEntities = new EventDeploymentQueryImpl(eventRegistryConfiguration.getCommandExecutor())
                        .deploymentName(deployment.getName())
                        .orderByDeploymentTime().desc()
                        .listPage(0, 1);
                if (!deploymentEntities.isEmpty()) {
                    existingDeployments.add(deploymentEntities.get(0));
                }
                
            } else {
                List<EventDeployment> deploymentList = eventRegistryConfiguration.getEventRepositoryService().createDeploymentQuery()
                        .deploymentName(deployment.getName())
                        .deploymentTenantId(deployment.getTenantId())
                        .orderByDeploymentTime().desc()
                        .listPage(0, 1);

                if (!deploymentList.isEmpty()) {
                    existingDeployments.addAll(deploymentList);
                }
            }

            if (!existingDeployments.isEmpty()) {
                EventDeploymentEntity existingDeployment = (EventDeploymentEntity) existingDeployments.get(0);

                Map<String, EventResourceEntity> resourceMap = new HashMap<>();
                List<EventResourceEntity> resourceList = eventRegistryConfiguration.getResourceEntityManager().findResourcesByDeploymentId(existingDeployment.getId());
                for (EventResourceEntity resourceEntity : resourceList) {
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
        eventRegistryConfiguration.getDeploymentEntityManager().insert(deployment);

        if (StringUtils.isEmpty(deployment.getParentDeploymentId())) {
            // If no parent deployment id is set then set the current ID as the parent
            // If something was deployed via this command than this deployment would
            // be a parent deployment to other potential child deployments
            deployment.setParentDeploymentId(deployment.getId());
        }

        // Actually deploy
        eventRegistryConfiguration.getDeploymentManager().deploy(deployment);

        return deployment;
    }

    protected boolean deploymentsDiffer(EventDeploymentEntity deployment, EventDeploymentEntity saved) {

        if (deployment.getResources() == null || saved.getResources() == null) {
            return true;
        }

        Map<String, EventResourceEntity> resources = deployment.getResources();
        Map<String, EventResourceEntity> savedResources = saved.getResources();

        for (String resourceName : resources.keySet()) {
            EventResourceEntity savedResource = savedResources.get(resourceName);

            if (savedResource == null) {
                return true;
            }

            EventResourceEntity resource = resources.get(resourceName);

            byte[] bytes = resource.getBytes();
            byte[] savedBytes = savedResource.getBytes();
            if (!Arrays.equals(bytes, savedBytes)) {
                return true;
            }
        }
        return false;
    }
}
