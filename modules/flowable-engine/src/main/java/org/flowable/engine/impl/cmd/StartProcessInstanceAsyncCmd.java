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

import org.flowable.bpmn.model.Process;
import org.flowable.common.engine.api.delegate.event.FlowableEventDispatcher;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.delegate.event.impl.FlowableEventBuilder;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.jobexecutor.AsyncContinuationJobHandler;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.runtime.ProcessInstanceBuilderImpl;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.impl.util.ProcessDefinitionUtil;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.job.service.JobService;
import org.flowable.job.service.impl.persistence.entity.JobEntity;

/**
 * author martin.grofcik
 */
public class StartProcessInstanceAsyncCmd extends StartProcessInstanceCmd {

    public StartProcessInstanceAsyncCmd(ProcessInstanceBuilderImpl processInstanceBuilder) {
        super(processInstanceBuilder);
    }

    @Override
    public ProcessInstance execute(CommandContext commandContext) {
        ProcessEngineConfigurationImpl processEngineConfiguration = CommandContextUtil.getProcessEngineConfiguration(commandContext);
        ProcessDefinition processDefinition = getProcessDefinition(processEngineConfiguration, commandContext);
        processInstanceHelper = CommandContextUtil.getProcessEngineConfiguration(commandContext).getProcessInstanceHelper();
        ExecutionEntity processInstance = (ExecutionEntity) processInstanceHelper.createProcessInstance(processDefinition, businessKey, processInstanceName,
            overrideDefinitionTenantId, predefinedProcessInstanceId, variables, transientVariables,
            callbackId, callbackType, referenceId, referenceType, stageInstanceId, false);
        ExecutionEntity execution = processInstance.getExecutions().get(0);
        Process process = ProcessDefinitionUtil.getProcess(processInstance.getProcessDefinitionId());

        processInstanceHelper.processAvailableEventSubProcesses(processInstance, process, commandContext);

        FlowableEventDispatcher eventDispatcher = CommandContextUtil.getProcessEngineConfiguration().getEventDispatcher();
        if (eventDispatcher != null && eventDispatcher.isEnabled()) {
            eventDispatcher.dispatchEvent(FlowableEventBuilder.createProcessStartedEvent(execution, variables, false));
        }

        executeAsynchronous(execution, process);

        return processInstance;
    }

    protected void executeAsynchronous(ExecutionEntity execution, Process process) {
        JobService jobService = CommandContextUtil.getJobService();

        JobEntity job = jobService.createJob();
        job.setExecutionId(execution.getId());
        job.setProcessInstanceId(execution.getProcessInstanceId());
        job.setProcessDefinitionId(execution.getProcessDefinitionId());
        job.setElementId(process.getId());
        job.setElementName(process.getName());
        job.setJobHandlerType(AsyncContinuationJobHandler.TYPE);

        // Inherit tenant id (if applicable)
        if (execution.getTenantId() != null) {
            job.setTenantId(execution.getTenantId());
        }

        execution.getJobs().add(job);

        jobService.createAsyncJob(job, false);
        jobService.scheduleAsyncJob(job);
    }

}
