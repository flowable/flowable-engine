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
import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.entitylink.api.EntityLink;
import org.flowable.entitylink.api.EntityLinkType;
import org.flowable.task.service.impl.persistence.entity.TaskEntity;

/**
 * @author Javier Casal
 */
public class GetEntityLinkParentsForTaskCmd implements Command<List<EntityLink>>, Serializable {

    private static final long serialVersionUID = 1L;
    protected String taskId;

    public GetEntityLinkParentsForTaskCmd(String taskId) {
        if (taskId == null) {
            throw new FlowableIllegalArgumentException("taskId is required");
        }
        this.taskId = taskId;
    }

    @Override
    public List<EntityLink> execute(CommandContext commandContext) {
        TaskEntity task = CommandContextUtil.getTaskService().getTask(taskId);

        if (task == null) {
            throw new FlowableObjectNotFoundException("Cannot find task with id " + taskId, ExecutionEntity.class);
        }

        return CommandContextUtil.getEntityLinkService().findEntityLinksByReferenceScopeIdAndType(task.getId(), ScopeTypes.TASK, EntityLinkType.CHILD);
    }

}
