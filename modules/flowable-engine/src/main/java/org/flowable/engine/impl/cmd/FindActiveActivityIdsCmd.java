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
import java.util.ArrayList;
import java.util.List;

import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.persistence.entity.ExecutionEntityManager;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.runtime.Execution;

/**
 * @author Tom Baeyens
 * @author Joram Barrez
 */
public class FindActiveActivityIdsCmd implements Command<List<String>>, Serializable {

    private static final long serialVersionUID = 1L;
    protected String executionId;

    public FindActiveActivityIdsCmd(String executionId) {
        this.executionId = executionId;
    }

    @Override
    public List<String> execute(CommandContext commandContext) {
        if (executionId == null) {
            throw new FlowableIllegalArgumentException("executionId is null");
        }

        ExecutionEntityManager executionEntityManager = CommandContextUtil.getExecutionEntityManager(commandContext);
        ExecutionEntity execution = executionEntityManager.findById(executionId);

        if (execution == null) {
            throw new FlowableObjectNotFoundException("execution " + executionId + " doesn't exist", Execution.class);
        }

        return findActiveActivityIds(execution);
    }

    public List<String> findActiveActivityIds(ExecutionEntity executionEntity) {
        List<String> activeActivityIds = new ArrayList<>();
        collectActiveActivityIds(executionEntity, activeActivityIds);
        return activeActivityIds;
    }

    protected void collectActiveActivityIds(ExecutionEntity executionEntity, List<String> activeActivityIds) {
        if (executionEntity.isActive() && executionEntity.getActivityId() != null) {
            activeActivityIds.add(executionEntity.getActivityId());
        }

        for (ExecutionEntity childExecution : executionEntity.getExecutions()) {
            collectActiveActivityIds(childExecution, activeActivityIds);
        }
    }

}
