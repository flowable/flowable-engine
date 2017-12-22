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
package org.flowable.content.engine.impl.cmd;

import java.io.Serializable;

import org.flowable.content.engine.impl.util.CommandContextUtil;
import org.flowable.engine.common.api.FlowableIllegalArgumentException;
import org.flowable.engine.common.impl.interceptor.Command;
import org.flowable.engine.common.impl.interceptor.CommandContext;

/**
 * @author Tijs Rademakers
 */
public class DeleteContentItemsCmd implements Command<Void>, Serializable {

    private static final long serialVersionUID = 1L;

    protected String processInstanceId;
    protected String taskId;
    protected String scopeId;
    protected String scopeType;

    public DeleteContentItemsCmd(String processInstanceId, String taskId, String scopeId, String scopeType) {
        this.processInstanceId = processInstanceId;
        this.taskId = taskId;
        this.scopeId = scopeId;
        this.scopeType = scopeType;
    }

    @Override
    public Void execute(CommandContext commandContext) {
        if (processInstanceId == null && taskId == null && (scopeType == null || scopeType == null)) {
            throw new FlowableIllegalArgumentException("(scopeType or scopeId) and taskId and processInstanceId are null");
        }

        if (processInstanceId != null) {
            CommandContextUtil.getContentItemEntityManager().deleteContentItemsByProcessInstanceId(processInstanceId);

        } else if (taskId != null){
            CommandContextUtil.getContentItemEntityManager().deleteContentItemsByTaskId(taskId);
        } else {
            CommandContextUtil.getContentItemEntityManager().deleteContentItemsByScopeIdAndScopeType(scopeId, scopeType);
        }

        return null;
    }

}
