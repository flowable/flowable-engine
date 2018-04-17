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

import org.apache.commons.lang3.StringUtils;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.content.engine.impl.util.CommandContextUtil;

/**
 * @author Tijs Rademakers
 */
public class DeleteContentItemsCmd implements Command<Void>, Serializable {

    private static final long serialVersionUID = 1L;

    protected String processInstanceId;
    protected String taskId;
    protected String caseId;

    public DeleteContentItemsCmd(String processInstanceId, String taskId, String caseId) {
        this.processInstanceId = processInstanceId;
        this.taskId = taskId;
        this.caseId = caseId;
    }

    @Override
    public Void execute(CommandContext commandContext) {
        if (processInstanceId == null && taskId == null && caseId == null) {
            throw new FlowableIllegalArgumentException("taskId, processInstanceId and caseId are null");
        }

        if (processInstanceId != null) {
            CommandContextUtil.getContentItemEntityManager().deleteContentItemsByProcessInstanceId(processInstanceId);

        } else if (StringUtils.isNotEmpty(caseId)) {
            CommandContextUtil.getContentItemEntityManager().deleteContentItemsByScopeIdAndScopeType(caseId, "cmmn");
        } else {
            CommandContextUtil.getContentItemEntityManager().deleteContentItemsByTaskId(taskId);
        }

        return null;
    }

}
