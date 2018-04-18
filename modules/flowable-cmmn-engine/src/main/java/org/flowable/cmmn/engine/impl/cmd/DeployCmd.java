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
package org.flowable.cmmn.engine.impl.cmd;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.flowable.cmmn.api.repository.CmmnDeployment;
import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.impl.persistence.entity.CmmnDeploymentEntity;
import org.flowable.cmmn.engine.impl.repository.CmmnDeploymentBuilderImpl;
import org.flowable.cmmn.engine.impl.repository.CmmnDeploymentQueryImpl;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.common.engine.api.repository.EngineResource;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;

/**
 * @author Joram Barrez
 */
public class DeployCmd implements Command<CmmnDeployment> {

    protected CmmnDeploymentBuilderImpl deploymentBuilder;

    public DeployCmd(CmmnDeploymentBuilderImpl deploymentBuilder) {
        this.deploymentBuilder = deploymentBuilder;
    }

    public CmmnDeployment execute(CommandContext commandContext) {
        CmmnEngineConfiguration cmmnEngineConfiguration = CommandContextUtil.getCmmnEngineConfiguration(commandContext);
        CmmnDeploymentEntity deployment = deploymentBuilder.getDeployment();
        
        if (deploymentBuilder.isDuplicateFilterEnabled()) {

            List<CmmnDeployment> existingDeployments = new ArrayList<>();
            if (deployment.getTenantId() == null || CmmnEngineConfiguration.NO_TENANT_ID.equals(deployment.getTenantId())) {
                List<CmmnDeployment> deploymentEntities = new CmmnDeploymentQueryImpl(cmmnEngineConfiguration.getCommandExecutor())
                        .deploymentName(deployment.getName())
                        .orderByDeploymenTime().desc()
                        .listPage(0, 1);
                if (!deploymentEntities.isEmpty()) {
                    existingDeployments.add(deploymentEntities.get(0));
                }
                
            } else {
                List<CmmnDeployment> deploymentList = cmmnEngineConfiguration.getCmmnRepositoryService().createDeploymentQuery().deploymentName(deployment.getName())
                        .deploymentTenantId(deployment.getTenantId()).orderByDeploymentId().desc().list();

                if (!deploymentList.isEmpty()) {
                    existingDeployments.addAll(deploymentList);
                }
            }

            CmmnDeploymentEntity existingDeployment = null;
            if (!existingDeployments.isEmpty()) {
                existingDeployment = (CmmnDeploymentEntity) existingDeployments.get(0);
            }

            if ((existingDeployment != null) && !deploymentsDiffer(deployment, existingDeployment)) {
                return existingDeployment;
            }
        }
        
        deployment.setDeploymentTime(cmmnEngineConfiguration.getClock().getCurrentTime());
        deployment.setNew(true);
        CommandContextUtil.getCmmnDeploymentEntityManager(commandContext).insert(deployment);
        cmmnEngineConfiguration.getDeploymentManager().deploy(deployment, null);
        return deployment;
    }
    
    protected boolean deploymentsDiffer(CmmnDeploymentEntity deployment, CmmnDeploymentEntity saved) {

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
