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

import org.flowable.engine.common.api.FlowableException;
import org.flowable.engine.common.api.FlowableIllegalArgumentException;
import org.flowable.engine.common.api.FlowableObjectNotFoundException;
import org.flowable.engine.impl.interceptor.Command;
import org.flowable.engine.impl.interceptor.CommandContext;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.persistence.entity.JobEntity;
import org.flowable.engine.impl.persistence.entity.SuspendedJobEntity;
import org.flowable.engine.impl.persistence.entity.SuspensionState;
import org.flowable.engine.impl.persistence.entity.TaskEntity;
import org.flowable.engine.impl.persistence.entity.TimerJobEntity;
import org.flowable.engine.impl.persistence.entity.SuspensionState.SuspensionStateUtil;
import org.flowable.engine.impl.util.Activiti5Util;
import org.flowable.engine.runtime.Execution;

/**
 * @author Joram Barrez
 * @author Tijs Rademakers
 */
public abstract class AbstractSetProcessInstanceStateCmd implements Command<Void> {

  protected final String processInstanceId;

  public AbstractSetProcessInstanceStateCmd(String processInstanceId) {
    this.processInstanceId = processInstanceId;
  }

  public Void execute(CommandContext commandContext) {

    if (processInstanceId == null) {
      throw new FlowableIllegalArgumentException("ProcessInstanceId cannot be null.");
    }

    ExecutionEntity executionEntity = commandContext.getExecutionEntityManager().findById(processInstanceId);

    if (executionEntity == null) {
      throw new FlowableObjectNotFoundException("Cannot find processInstance for id '" + processInstanceId + "'.", Execution.class);
    }
    if (!executionEntity.isProcessInstanceType()) {
      throw new FlowableException("Cannot set suspension state for execution '" + processInstanceId + "': not a process instance.");
    }
    
    if (Activiti5Util.isActiviti5ProcessDefinitionId(commandContext, executionEntity.getProcessDefinitionId())) {
      if (getNewState() == SuspensionState.ACTIVE) {
        commandContext.getProcessEngineConfiguration().getActiviti5CompatibilityHandler().activateProcessInstance(processInstanceId);
      } else {
        commandContext.getProcessEngineConfiguration().getActiviti5CompatibilityHandler().suspendProcessInstance(processInstanceId);
      }
      return null;
    }

    SuspensionStateUtil.setSuspensionState(executionEntity, getNewState());
    commandContext.getExecutionEntityManager().update(executionEntity, false);

    // All child executions are suspended
    Collection<ExecutionEntity> childExecutions = commandContext.getExecutionEntityManager().findChildExecutionsByProcessInstanceId(processInstanceId);
    for (ExecutionEntity childExecution : childExecutions) {
      if (!childExecution.getId().equals(processInstanceId)) {
        SuspensionStateUtil.setSuspensionState(childExecution, getNewState());
        commandContext.getExecutionEntityManager().update(childExecution, false);
      }
    }

    // All tasks are suspended
    List<TaskEntity> tasks = commandContext.getTaskEntityManager().findTasksByProcessInstanceId(processInstanceId);
    for (TaskEntity taskEntity : tasks) {
      SuspensionStateUtil.setSuspensionState(taskEntity, getNewState());
      commandContext.getTaskEntityManager().update(taskEntity, false);
    }
    
    // All jobs are suspended
    if (getNewState() == SuspensionState.ACTIVE) {
      List<SuspendedJobEntity> suspendedJobs = commandContext.getSuspendedJobEntityManager().findJobsByProcessInstanceId(processInstanceId);
      for (SuspendedJobEntity suspendedJob : suspendedJobs) {
        commandContext.getJobManager().activateSuspendedJob(suspendedJob);
      }
      
    } else {
      List<TimerJobEntity> timerJobs = commandContext.getTimerJobEntityManager().findJobsByProcessInstanceId(processInstanceId);
      for (TimerJobEntity timerJob : timerJobs) {
        commandContext.getJobManager().moveJobToSuspendedJob(timerJob);
      }
      
      List<JobEntity> jobs = commandContext.getJobEntityManager().findJobsByProcessInstanceId(processInstanceId);
      for (JobEntity job : jobs) {
        commandContext.getJobManager().moveJobToSuspendedJob(job);
      }
    }

    return null;
  }

  protected abstract SuspensionState getNewState();

}
