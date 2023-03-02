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
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;

/**
 * @author Christopher Welsch
 */
public class DeleteProcessInstancesByIdCmd implements Command<Void>, Serializable {

    private static final long serialVersionUID = 1L;
    protected Collection<String> processInstanceIds;
    protected String deleteReason;

    public DeleteProcessInstancesByIdCmd(Collection<String> processInstanceIds, String deleteReason) {
        this.processInstanceIds = processInstanceIds;
        this.deleteReason = deleteReason;
    }

    @Override
    public Void execute(CommandContext commandContext) {
        if (processInstanceIds == null) {
            throw new FlowableIllegalArgumentException("processInstanceIds are null");
        }

        Set<String> processInstanceIdSet = new HashSet<>(processInstanceIds);
        for (String processInstanceId : processInstanceIdSet) {
            executeSingleDelete(commandContext, processInstanceId);
        }
        return null;
    }

    protected Void executeSingleDelete(CommandContext commandContext, String processInstanceId) {
        DeleteProcessInstanceCmd command = new DeleteProcessInstanceCmd(processInstanceId, deleteReason);
        return command.execute(commandContext);
    }
}
