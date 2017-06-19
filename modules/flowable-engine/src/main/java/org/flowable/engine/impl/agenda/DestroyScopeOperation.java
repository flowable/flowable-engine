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
package org.flowable.engine.impl.agenda;

import java.util.Collection;

import org.flowable.engine.common.api.FlowableException;
import org.flowable.engine.impl.interceptor.CommandContext;
import org.flowable.engine.impl.persistence.entity.DeadLetterJobEntity;
import org.flowable.engine.impl.persistence.entity.DeadLetterJobEntityManager;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.persistence.entity.ExecutionEntityManager;
import org.flowable.engine.impl.persistence.entity.JobEntity;
import org.flowable.engine.impl.persistence.entity.JobEntityManager;
import org.flowable.engine.impl.persistence.entity.SuspendedJobEntity;
import org.flowable.engine.impl.persistence.entity.SuspendedJobEntityManager;
import org.flowable.engine.impl.persistence.entity.TaskEntity;
import org.flowable.engine.impl.persistence.entity.TaskEntityManager;
import org.flowable.engine.impl.persistence.entity.TimerJobEntity;
import org.flowable.engine.impl.persistence.entity.TimerJobEntityManager;
import org.flowable.engine.impl.persistence.entity.VariableInstanceEntity;
import org.flowable.engine.impl.persistence.entity.VariableInstanceEntityManager;

/**
 * Destroys a scope (for example a subprocess): this means that all child executions, tasks, jobs, variables, etc within that scope are deleted.
 * 
 * The typical example is an interrupting boundary event that is on the boundary of a subprocess and is triggered. At that point, everything within the subprocess would need to be destroyed.
 * 
 * @author Joram Barrez
 */
public class DestroyScopeOperation extends AbstractOperation {

    public DestroyScopeOperation(CommandContext commandContext, ExecutionEntity execution) {
        super(commandContext, execution);
    }

    @Override
    public void run() {

        // Find the actual scope that needs to be destroyed.
        // This could be the incoming execution, or the first parent execution where isScope = true

        // Find parent scope execution
        ExecutionEntity scopeExecution = execution.isScope() ? execution : findFirstParentScopeExecution(execution);

        if (scopeExecution == null) {
            throw new FlowableException("Programmatic error: no parent scope execution found for boundary event");
        }

        ExecutionEntityManager executionEntityManager = commandContext.getExecutionEntityManager();

        // Delete all child executions
        Collection<ExecutionEntity> childExecutions = executionEntityManager.findChildExecutionsByParentExecutionId(scopeExecution.getId());
        for (ExecutionEntity childExecution : childExecutions) {
            executionEntityManager.deleteExecutionAndRelatedData(childExecution, execution.getDeleteReason(), false);
        }

        // Delete all scope tasks
        TaskEntityManager taskEntityManager = commandContext.getTaskEntityManager();
        Collection<TaskEntity> tasksForExecution = taskEntityManager.findTasksByExecutionId(scopeExecution.getId());
        for (TaskEntity taskEntity : tasksForExecution) {
            taskEntityManager.deleteTask(taskEntity, execution.getDeleteReason(), false, false, true);
        }

        // Delete all scope jobs
        TimerJobEntityManager timerJobEntityManager = commandContext.getTimerJobEntityManager();
        Collection<TimerJobEntity> timerJobsForExecution = timerJobEntityManager.findJobsByExecutionId(scopeExecution.getId());
        for (TimerJobEntity job : timerJobsForExecution) {
            timerJobEntityManager.delete(job);
        }

        JobEntityManager jobEntityManager = commandContext.getJobEntityManager();
        Collection<JobEntity> jobsForExecution = jobEntityManager.findJobsByExecutionId(scopeExecution.getId());
        for (JobEntity job : jobsForExecution) {
            jobEntityManager.delete(job);
        }

        SuspendedJobEntityManager suspendedJobEntityManager = commandContext.getSuspendedJobEntityManager();
        Collection<SuspendedJobEntity> suspendedJobsForExecution = suspendedJobEntityManager.findJobsByExecutionId(scopeExecution.getId());
        for (SuspendedJobEntity job : suspendedJobsForExecution) {
            suspendedJobEntityManager.delete(job);
        }

        DeadLetterJobEntityManager deadLetterJobEntityManager = commandContext.getDeadLetterJobEntityManager();
        Collection<DeadLetterJobEntity> deadLetterJobsForExecution = deadLetterJobEntityManager.findJobsByExecutionId(scopeExecution.getId());
        for (DeadLetterJobEntity job : deadLetterJobsForExecution) {
            deadLetterJobEntityManager.delete(job);
        }

        // Remove variables associated with this scope
        VariableInstanceEntityManager variableInstanceEntityManager = commandContext.getVariableInstanceEntityManager();
        Collection<VariableInstanceEntity> variablesForExecution = variableInstanceEntityManager.findVariableInstancesByExecutionId(scopeExecution.getId());
        for (VariableInstanceEntity variable : variablesForExecution) {
            variableInstanceEntityManager.delete(variable);
        }

        if (scopeExecution.isActive()) {
            commandContext.getHistoryManager().recordActivityEnd(scopeExecution, scopeExecution.getDeleteReason());
        }
        executionEntityManager.delete(scopeExecution);
    }

}
