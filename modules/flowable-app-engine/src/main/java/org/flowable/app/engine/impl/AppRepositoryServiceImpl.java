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
package org.flowable.app.engine.impl;

import java.io.InputStream;
import java.util.List;

import org.flowable.app.api.AppRepositoryService;
import org.flowable.app.api.repository.AppDefinition;
import org.flowable.app.api.repository.AppDefinitionQuery;
import org.flowable.app.api.repository.AppDeployment;
import org.flowable.app.api.repository.AppDeploymentBuilder;
import org.flowable.app.api.repository.AppDeploymentQuery;
import org.flowable.app.api.repository.AppModel;
import org.flowable.app.engine.AppEngineConfiguration;
import org.flowable.app.engine.impl.cmd.DeleteDeploymentCmd;
import org.flowable.app.engine.impl.cmd.DeployCmd;
import org.flowable.app.engine.impl.cmd.GetAppModelCmd;
import org.flowable.app.engine.impl.cmd.GetDeploymentAppDefinitionCmd;
import org.flowable.app.engine.impl.cmd.GetDeploymentResourceCmd;
import org.flowable.app.engine.impl.cmd.GetDeploymentResourceNamesCmd;
import org.flowable.app.engine.impl.cmd.SetAppDefinitionCategoryCmd;
import org.flowable.app.engine.impl.repository.AppDeploymentBuilderImpl;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.service.CommonEngineServiceImpl;

/**
 * @author Joram Barrez
 * @author Tijs Rademakers
 */
public class AppRepositoryServiceImpl extends CommonEngineServiceImpl<AppEngineConfiguration> implements AppRepositoryService {
    
    public AppRepositoryServiceImpl(AppEngineConfiguration engineConfiguration) {
        super(engineConfiguration);
    }

    @Override
    public AppDeploymentBuilder createDeployment() {
        return commandExecutor.execute(new Command<AppDeploymentBuilder>() {
            @Override
            public AppDeploymentBuilder execute(CommandContext commandContext) {
                return new AppDeploymentBuilderImpl();
            }
        });
    }
    
    @Override
    public List<String> getDeploymentResourceNames(String deploymentId) {
       return commandExecutor.execute(new GetDeploymentResourceNamesCmd(deploymentId));
    }

    @Override
    public InputStream getResourceAsStream(String deploymentId, String resourceName) {
        return commandExecutor.execute(new GetDeploymentResourceCmd(deploymentId, resourceName));
    }
    
    public AppDeployment deploy(AppDeploymentBuilderImpl deploymentBuilder) {
        return commandExecutor.execute(new DeployCmd(deploymentBuilder));
    }
    
    @Override
    public AppDefinition getAppDefinition(String appDefinitionId) {
        return commandExecutor.execute(new GetDeploymentAppDefinitionCmd(appDefinitionId));
    }
    
    @Override
    public AppModel getAppModel(String appDefinitionId) {
        return commandExecutor.execute(new GetAppModelCmd(appDefinitionId));
    }
    
    @Override
    public void deleteDeployment(String deploymentId, boolean cascade) {
        commandExecutor.execute(new DeleteDeploymentCmd(deploymentId, cascade));
    }
    
    @Override
    public AppDeploymentQuery createDeploymentQuery() {
        return configuration.getAppDeploymentEntityManager().createDeploymentQuery();
    }
    
    @Override
    public AppDefinitionQuery createAppDefinitionQuery() {
        return configuration.getAppDefinitionEntityManager().createAppDefinitionQuery();
    }

    @Override
    public void setAppDefinitionCategory(String appDefinitionId, String category) {
        commandExecutor.execute(new SetAppDefinitionCategoryCmd(appDefinitionId, category));
    }
}
