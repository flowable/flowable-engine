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

import org.flowable.dmn.api.DmnDecisionTable;
import org.flowable.dmn.api.DmnDecisionTableQuery;
import org.flowable.dmn.api.DmnDeployment;
import org.flowable.dmn.api.DmnDeploymentBuilder;
import org.flowable.dmn.api.DmnDeploymentQuery;
import org.flowable.dmn.api.DmnRepositoryService;
import org.flowable.dmn.api.NativeDecisionTableQuery;
import org.flowable.dmn.api.NativeDmnDeploymentQuery;
import org.flowable.dmn.engine.impl.cmd.DeleteDeploymentCmd;
import org.flowable.dmn.engine.impl.cmd.DeployCmd;
import org.flowable.dmn.engine.impl.cmd.GetDeploymentDecisionTableCmd;
import org.flowable.dmn.engine.impl.cmd.GetDeploymentDmnResourceCmd;
import org.flowable.dmn.engine.impl.cmd.GetDeploymentResourceCmd;
import org.flowable.dmn.engine.impl.cmd.GetDeploymentResourceNamesCmd;
import org.flowable.dmn.engine.impl.cmd.GetDmnDefinitionCmd;
import org.flowable.dmn.engine.impl.cmd.SetDecisionTableCategoryCmd;
import org.flowable.dmn.engine.impl.cmd.SetDeploymentCategoryCmd;
import org.flowable.dmn.engine.impl.cmd.SetDeploymentTenantIdCmd;
import org.flowable.dmn.engine.impl.repository.DmnDeploymentBuilderImpl;
import org.flowable.dmn.model.DmnDefinition;
import org.flowable.engine.common.impl.interceptor.Command;
import org.flowable.engine.common.impl.interceptor.CommandContext;

/**
 * @author Tijs Rademakers
 */
public class DmnRepositoryServiceImpl extends ServiceImpl implements DmnRepositoryService {

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

    public void deleteDeployment(String deploymentId) {
        commandExecutor.execute(new DeleteDeploymentCmd(deploymentId));
    }

    public DmnDecisionTableQuery createDecisionTableQuery() {
        return new DecisionTableQueryImpl(commandExecutor);
    }

    public NativeDecisionTableQuery createNativeDecisionTableQuery() {
        return new NativeDecisionTableQueryImpl(commandExecutor);
    }

    public List<String> getDeploymentResourceNames(String deploymentId) {
        return commandExecutor.execute(new GetDeploymentResourceNamesCmd(deploymentId));
    }

    public InputStream getResourceAsStream(String deploymentId, String resourceName) {
        return commandExecutor.execute(new GetDeploymentResourceCmd(deploymentId, resourceName));
    }

    public void setDeploymentCategory(String deploymentId, String category) {
        commandExecutor.execute(new SetDeploymentCategoryCmd(deploymentId, category));
    }

    public void setDeploymentTenantId(String deploymentId, String newTenantId) {
        commandExecutor.execute(new SetDeploymentTenantIdCmd(deploymentId, newTenantId));
    }

    public DmnDeploymentQuery createDeploymentQuery() {
        return new DmnDeploymentQueryImpl(commandExecutor);
    }

    public NativeDmnDeploymentQuery createNativeDeploymentQuery() {
        return new NativeDmnDeploymentQueryImpl(commandExecutor);
    }

    public DmnDecisionTable getDecisionTable(String decisionTableId) {
        return commandExecutor.execute(new GetDeploymentDecisionTableCmd(decisionTableId));
    }

    public DmnDefinition getDmnDefinition(String decisionTableId) {
        return commandExecutor.execute(new GetDmnDefinitionCmd(decisionTableId));
    }

    public InputStream getDmnResource(String decisionTableId) {
        return commandExecutor.execute(new GetDeploymentDmnResourceCmd(decisionTableId));
    }

    public void setDecisionTableCategory(String decisionTableId, String category) {
        commandExecutor.execute(new SetDecisionTableCategoryCmd(decisionTableId, category));
    }
}
