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

import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.entitylink.api.EntityLinkType;
import org.flowable.entitylink.api.history.HistoricEntityLink;

/**
 * @author Javier Casal
 */
public class GetHistoricEntityLinkParentsForTaskCmd implements Command<List<HistoricEntityLink>>, Serializable {

    private static final long serialVersionUID = 1L;
    protected String taskId;

    public GetHistoricEntityLinkParentsForTaskCmd(String taskId) {
        if (taskId == null) {
            throw new FlowableIllegalArgumentException("taskId is required");
        }
        this.taskId = taskId;
    }

    @Override
    public List<HistoricEntityLink> execute(CommandContext commandContext) {
        ProcessEngineConfigurationImpl processEngineConfiguration = CommandContextUtil.getProcessEngineConfiguration(commandContext);
        return processEngineConfiguration.getEntityLinkServiceConfiguration().getHistoricEntityLinkService()
                .findHistoricEntityLinksByReferenceScopeIdAndType(taskId, ScopeTypes.TASK, EntityLinkType.CHILD);
    }

}
