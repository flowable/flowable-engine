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

import java.util.Collection;
import java.util.List;

import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.common.engine.impl.db.SuspensionState;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.persistence.entity.ExecutionEntityManager;
import org.flowable.engine.impl.persistence.entity.SuspensionStateUtil;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.impl.util.Flowable5Util;
import org.flowable.engine.runtime.Execution;
import org.flowable.job.service.JobService;
import org.flowable.job.service.TimerJobService;
import org.flowable.job.service.impl.persistence.entity.JobEntity;
import org.flowable.job.service.impl.persistence.entity.SuspendedJobEntity;
import org.flowable.job.service.impl.persistence.entity.TimerJobEntity;
import org.flowable.task.service.impl.persistence.entity.TaskEntity;

/**
 * @author Joram Barrez
 * @author Tijs Rademakers
 */
public abstract class AbstractSetProcessInstanceStateCmd implements Command<Void> {

    protected final String processInstanceId;

    public AbstractSetProcessInstanceStateCmd(String processInstanceId) {
        this.processInstanceId = processInstanceId;
    }

    @Override
    public Void execute(CommandContext commandContext) {

        if (processInstanceId == null) {
            throw new FlowableIllegalArgumentException("ProcessInstanceId cannot be null.");
        }

        ExecutionEntityManager executionEntityManager = CommandContextUtil.getExecutionEntityManager(commandContext);
        ExecutionEntity executionEntity = executionEntityManager.findById(processInstanceId);

        if (executionEntity == null) {
            throw new FlowableObjectNotFoundException("Cannot find processInstance for id '" + processInstanceId + "'.", Execution.class);
        }
        if (!executionEntity.isProcessInstanceType()) {
            throw new FlowableException("Cannot set suspension state for execution '" + processInstanceId + "': not a process instance.");
        }

        if (Flowable5Util.isFlowable5ProcessDefinitionId(commandContext, executionEntity.getProcessDefinitionId())) {
            if (getNewState() == SuspensionState.ACTIVE) {
                CommandContextUtil.getProcessEngineConfiguration().getFlowable5CompatibilityHandler().activateProcessInstance(processInstanceId);
            } else {
                CommandContextUtil.getProcessEngineConfiguration().getFlowable5CompatibilityHandler().suspendProcessInstance(processInstanceId);
            }
            return null;
        }

        SuspensionStateUtil.setSuspensionState(executionEntity, getNewState());
        executionEntityManager.update(executionEntity, false);

        // All child executions are suspended
        Collection<ExecutionEntity> childExecutions = executionEntityManager.findChildExecutionsByProcessInstanceId(processInstanceId);
        for (ExecutionEntity childExecution : childExecutions) {
            if (!childExecution.getId().equals(processInstanceId)) {
                SuspensionStateUtil.setSuspensionState(childExecution, getNewState());
                executionEntityManager.update(childExecution, false);
            }
        }

        // All tasks are suspended
        List<TaskEntity> tasks = CommandContextUtil.getTaskService().findTasksByProcessInstanceId(processInstanceId);
        for (TaskEntity taskEntity : tasks) {
            SuspensionStateUtil.setSuspensionState(taskEntity, getNewState());
            CommandContextUtil.getTaskService().updateTask(taskEntity, false);
        }

        // All jobs are suspended
        JobService jobService = CommandContextUtil.getJobService(commandContext);
        if (getNewState() == SuspensionState.ACTIVE) {
            List<SuspendedJobEntity> suspendedJobs = jobService.findSuspendedJobsByProcessInstanceId(processInstanceId);
            for (SuspendedJobEntity suspendedJob : suspendedJobs) {
                jobService.activateSuspendedJob(suspendedJob);
            }

        } else {
            TimerJobService timerJobService = CommandContextUtil.getTimerJobService(commandContext);
            List<TimerJobEntity> timerJobs = timerJobService.findTimerJobsByProcessInstanceId(processInstanceId);
            for (TimerJobEntity timerJob : timerJobs) {
                jobService.moveJobToSuspendedJob(timerJob);
            }

            List<JobEntity> jobs = jobService.findJobsByProcessInstanceId(processInstanceId);
            for (JobEntity job : jobs) {
                jobService.moveJobToSuspendedJob(job);
            }
        }

        return null;
    }

    protected abstract SuspensionState getNewState();

}
