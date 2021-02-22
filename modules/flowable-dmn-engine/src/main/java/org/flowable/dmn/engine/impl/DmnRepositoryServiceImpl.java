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
package org.flowable.dmn.engine.impl;

import java.io.InputStream;
import java.util.List;

import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.service.CommonEngineServiceImpl;
import org.flowable.dmn.api.DmnDecision;
import org.flowable.dmn.api.DmnDecisionQuery;
import org.flowable.dmn.api.DmnDeployment;
import org.flowable.dmn.api.DmnDeploymentBuilder;
import org.flowable.dmn.api.DmnDeploymentQuery;
import org.flowable.dmn.api.DmnRepositoryService;
import org.flowable.dmn.api.NativeDecisionQuery;
import org.flowable.dmn.api.NativeDmnDeploymentQuery;
import org.flowable.dmn.engine.DmnEngineConfiguration;
import org.flowable.dmn.engine.impl.cmd.DeleteDeploymentCmd;
import org.flowable.dmn.engine.impl.cmd.DeployCmd;
import org.flowable.dmn.engine.impl.cmd.GetDeploymentDecisionCmd;
import org.flowable.dmn.engine.impl.cmd.GetDeploymentDecisionRequirementsDiagramCmd;
import org.flowable.dmn.engine.impl.cmd.GetDeploymentDmnResourceCmd;
import org.flowable.dmn.engine.impl.cmd.GetDeploymentResourceCmd;
import org.flowable.dmn.engine.impl.cmd.GetDeploymentResourceNamesCmd;
import org.flowable.dmn.engine.impl.cmd.GetDmnDefinitionCmd;
import org.flowable.dmn.engine.impl.cmd.SetDecisionTableCategoryCmd;
import org.flowable.dmn.engine.impl.cmd.SetDeploymentCategoryCmd;
import org.flowable.dmn.engine.impl.cmd.SetDeploymentParentDeploymentIdCmd;
import org.flowable.dmn.engine.impl.cmd.SetDeploymentTenantIdCmd;
import org.flowable.dmn.engine.impl.repository.DmnDeploymentBuilderImpl;
import org.flowable.dmn.model.DmnDefinition;

/**
 * @author Tijs Rademakers
 */
public class DmnRepositoryServiceImpl extends CommonEngineServiceImpl<DmnEngineConfiguration> implements DmnRepositoryService {

    @Override
    public DmnDeploymentBuilder createDeployment() {
        return commandExecutor.execute(new Command<DmnDeploymentBuilder>() {
            @Override
            public DmnDeploymentBuilder execute(CommandContext commandContext) {
                return new DmnDeploymentBuilderImpl();
            }
        });
    }

    public DmnDeployment deploy(DmnDeploymentBuilderImpl deploymentBuilder) {
        return commandExecutor.execute(new DeployCmd<DmnDeployment>(deploymentBuilder));
    }

    @Override
    public void deleteDeployment(String deploymentId) {
        commandExecutor.execute(new DeleteDeploymentCmd(deploymentId));
    }

    @Override
    public DmnDecisionQuery createDecisionQuery() {
        return new DecisionQueryImpl(commandExecutor);
    }

    @Override
    public NativeDecisionQuery createNativeDecisionQuery() {
        return new NativeDecisionTableQueryImpl(commandExecutor);
    }

    @Override
    public List<String> getDeploymentResourceNames(String deploymentId) {
        return commandExecutor.execute(new GetDeploymentResourceNamesCmd(deploymentId));
    }

    @Override
    public InputStream getResourceAsStream(String deploymentId, String resourceName) {
        return commandExecutor.execute(new GetDeploymentResourceCmd(deploymentId, resourceName));
    }

    @Override
    public void setDeploymentCategory(String deploymentId, String category) {
        commandExecutor.execute(new SetDeploymentCategoryCmd(deploymentId, category));
    }

    @Override
    public void setDeploymentTenantId(String deploymentId, String newTenantId) {
        commandExecutor.execute(new SetDeploymentTenantIdCmd(deploymentId, newTenantId));
    }
    
    @Override
    public void changeDeploymentParentDeploymentId(String deploymentId, String newParentDeploymentId) {
        commandExecutor.execute(new SetDeploymentParentDeploymentIdCmd(deploymentId, newParentDeploymentId));
    }

    @Override
    public DmnDeploymentQuery createDeploymentQuery() {
        return new DmnDeploymentQueryImpl(commandExecutor);
    }

    @Override
    public NativeDmnDeploymentQuery createNativeDeploymentQuery() {
        return new NativeDmnDeploymentQueryImpl(commandExecutor);
    }

    @Override
    public DmnDecision getDecision(String decisionId) {
        return commandExecutor.execute(new GetDeploymentDecisionCmd(decisionId));
    }

    @Override
    public DmnDefinition getDmnDefinition(String decisionId) {
        return commandExecutor.execute(new GetDmnDefinitionCmd(decisionId));
    }

    @Override
    public InputStream getDmnResource(String decisionId) {
        return commandExecutor.execute(new GetDeploymentDmnResourceCmd(decisionId));
    }

    @Override
    public void setDecisionCategory(String decisionId, String category) {
        commandExecutor.execute(new SetDecisionTableCategoryCmd(decisionId, category));
    }

    @Override
    public InputStream getDecisionRequirementsDiagram(String decisionId) {
        return commandExecutor.execute(new GetDeploymentDecisionRequirementsDiagramCmd(decisionId));
    }

}
