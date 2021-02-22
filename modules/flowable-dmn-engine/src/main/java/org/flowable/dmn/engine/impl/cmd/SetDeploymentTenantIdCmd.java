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
import java.util.List;

import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.dmn.api.DmnDecision;
import org.flowable.dmn.engine.impl.DecisionQueryImpl;
import org.flowable.dmn.engine.impl.persistence.entity.DmnDeploymentEntity;
import org.flowable.dmn.engine.impl.util.CommandContextUtil;

/**
 * @author Joram Barrez
 */
public class SetDeploymentTenantIdCmd implements Command<Void>, Serializable {

    private static final long serialVersionUID = 1L;

    protected String deploymentId;
    protected String newTenantId;

    public SetDeploymentTenantIdCmd(String deploymentId, String newTenantId) {
        this.deploymentId = deploymentId;
        this.newTenantId = newTenantId;
    }

    @Override
    public Void execute(CommandContext commandContext) {
        if (deploymentId == null) {
            throw new FlowableIllegalArgumentException("deploymentId is null");
        }

        // Update all entities

        DmnDeploymentEntity deployment = CommandContextUtil.getDeploymentEntityManager(commandContext).findById(deploymentId);
        if (deployment == null) {
            throw new FlowableObjectNotFoundException("Could not find deployment with id " + deploymentId);
        }

        deployment.setTenantId(newTenantId);

        // Doing process instances, executions and tasks with direct SQL updates
        // (otherwise would not be performant)
        CommandContextUtil.getDecisionEntityManager(commandContext).updateDecisionTenantIdForDeployment(deploymentId, newTenantId);

        // Doing decision tables in memory, cause we need to clear the decision table cache
        List<DmnDecision> decisionTables = new DecisionQueryImpl().deploymentId(deploymentId).list();
        for (DmnDecision decisionTable : decisionTables) {
            CommandContextUtil.getDmnEngineConfiguration().getDefinitionCache().remove(decisionTable.getId());
        }

        CommandContextUtil.getDeploymentEntityManager(commandContext).update(deployment);

        return null;

    }

}
