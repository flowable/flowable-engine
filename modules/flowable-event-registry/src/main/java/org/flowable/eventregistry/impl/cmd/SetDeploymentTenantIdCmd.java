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
import java.util.List;

import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.eventregistry.api.EventDefinition;
import org.flowable.eventregistry.impl.EventDefinitionQueryImpl;
import org.flowable.eventregistry.impl.persistence.entity.EventDeploymentEntity;
import org.flowable.eventregistry.impl.util.CommandContextUtil;

/**
 * @author Tijs Rademakers
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

        EventDeploymentEntity deployment = CommandContextUtil.getDeploymentEntityManager(commandContext).findById(deploymentId);
        if (deployment == null) {
            throw new FlowableObjectNotFoundException("Could not find deployment with id " + deploymentId);
        }

        deployment.setTenantId(newTenantId);

        CommandContextUtil.getEventDefinitionEntityManager(commandContext).updateEventDefinitionTenantIdForDeployment(deploymentId, newTenantId);

        // Doing event definitions in memory, cause we need to clear the event definition cache
        List<EventDefinition> eventDefinitions = new EventDefinitionQueryImpl().deploymentId(deploymentId).list();
        for (EventDefinition eventDefinition : eventDefinitions) {
            CommandContextUtil.getEventRegistryConfiguration().getEventDefinitionCache().remove(eventDefinition.getId());
        }

        CommandContextUtil.getDeploymentEntityManager(commandContext).update(deployment);

        return null;

    }

}
