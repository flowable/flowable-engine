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
package org.flowable.app.engine.impl.cmd;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.flowable.app.api.repository.AppDeployment;
import org.flowable.app.engine.AppEngineConfiguration;
import org.flowable.app.engine.impl.persistence.entity.AppDeploymentEntity;
import org.flowable.app.engine.impl.repository.AppDeploymentBuilderImpl;
import org.flowable.app.engine.impl.repository.AppDeploymentQueryImpl;
import org.flowable.app.engine.impl.util.CommandContextUtil;
import org.flowable.common.engine.api.repository.EngineResource;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;

/**
 * @author Tijs Rademakers
 */
public class DeployCmd implements Command<AppDeployment> {

    protected AppDeploymentBuilderImpl deploymentBuilder;

    public DeployCmd(AppDeploymentBuilderImpl deploymentBuilder) {
        this.deploymentBuilder = deploymentBuilder;
    }

    public AppDeployment execute(CommandContext commandContext) {
        AppEngineConfiguration appEngineConfiguration = CommandContextUtil.getAppEngineConfiguration(commandContext);
        AppDeploymentEntity deployment = deploymentBuilder.getDeployment();
        
        if (deploymentBuilder.isDuplicateFilterEnabled()) {

            List<AppDeployment> existingDeployments = new ArrayList<>();
            if (deployment.getTenantId() == null || AppEngineConfiguration.NO_TENANT_ID.equals(deployment.getTenantId())) {
                List<AppDeployment> deploymentEntities = new AppDeploymentQueryImpl(appEngineConfiguration.getCommandExecutor())
                        .deploymentName(deployment.getName())
                        .orderByDeploymenTime().desc()
                        .listPage(0, 1);
                if (!deploymentEntities.isEmpty()) {
                    existingDeployments.add(deploymentEntities.get(0));
                }
                
            } else {
                List<AppDeployment> deploymentList = appEngineConfiguration.getAppRepositoryService().createDeploymentQuery().deploymentName(deployment.getName())
                        .deploymentTenantId(deployment.getTenantId()).orderByDeploymentId().desc().list();

                if (!deploymentList.isEmpty()) {
                    existingDeployments.addAll(deploymentList);
                }
            }

            AppDeploymentEntity existingDeployment = null;
            if (!existingDeployments.isEmpty()) {
                existingDeployment = (AppDeploymentEntity) existingDeployments.get(0);
            }

            if ((existingDeployment != null) && !deploymentsDiffer(deployment, existingDeployment)) {
                return existingDeployment;
            }
        }
        
        deployment.setDeploymentTime(appEngineConfiguration.getClock().getCurrentTime());
        deployment.setNew(true);
        CommandContextUtil.getAppDeploymentEntityManager(commandContext).insert(deployment);
        appEngineConfiguration.getDeploymentManager().deploy(deployment, null);
        return deployment;
    }
    
    protected boolean deploymentsDiffer(AppDeploymentEntity deployment, AppDeploymentEntity saved) {

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

            EngineResource resource = resources.get(resourceName);

            byte[] bytes = resource.getBytes();
            byte[] savedBytes = savedResource.getBytes();
            if (!Arrays.equals(bytes, savedBytes)) {
                return true;
            }
        }
        return false;
    }

}
