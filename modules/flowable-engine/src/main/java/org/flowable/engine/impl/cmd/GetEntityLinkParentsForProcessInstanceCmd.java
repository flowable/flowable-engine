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
package org.flowable.engine.impl.cmd;

import java.io.Serializable;
import java.util.List;

import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.entitylink.api.EntityLink;
import org.flowable.entitylink.api.EntityLinkType;

/**
 * @author Javier Casal
 */
public class GetEntityLinkParentsForProcessInstanceCmd implements Command<List<EntityLink>>, Serializable {

    private static final long serialVersionUID = 1L;

    protected String processInstanceId;

    public GetEntityLinkParentsForProcessInstanceCmd(String processInstanceId) {
        this.processInstanceId = processInstanceId;
    }

    @Override
    public List<EntityLink> execute(CommandContext commandContext) {
        ProcessEngineConfigurationImpl processEngineConfiguration = CommandContextUtil.getProcessEngineConfiguration(commandContext);
        ExecutionEntity processInstance = processEngineConfiguration.getExecutionEntityManager().findById(processInstanceId);

        if (processInstance == null) {
            throw new FlowableObjectNotFoundException("Cannot find process instance with id " + processInstanceId, ExecutionEntity.class);
        }

        return processEngineConfiguration.getEntityLinkServiceConfiguration().getEntityLinkService()
                .findEntityLinksByReferenceScopeIdAndType(processInstanceId, ScopeTypes.BPMN, EntityLinkType.CHILD);
    }

}
