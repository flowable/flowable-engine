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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.common.engine.impl.db.SuspensionState;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.engine.compatibility.Flowable5CompatibilityHandler;
import org.flowable.engine.impl.ProcessDefinitionQueryImpl;
import org.flowable.engine.impl.ProcessInstanceQueryImpl;
import org.flowable.engine.impl.jobexecutor.TimerChangeProcessDefinitionSuspensionStateJobHandler;
import org.flowable.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.flowable.engine.impl.persistence.entity.ProcessDefinitionEntityManager;
import org.flowable.engine.impl.persistence.entity.SuspensionStateUtil;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.impl.util.Flowable5Util;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.job.service.JobHandler;
import org.flowable.job.service.TimerJobService;
import org.flowable.job.service.impl.persistence.entity.JobEntity;
import org.flowable.job.service.impl.persistence.entity.TimerJobEntity;

/**
 * @author Daniel Meyer
 * @author Joram Barrez
 */
public abstract class AbstractSetProcessDefinitionStateCmd implements Command<Void> {

    protected String processDefinitionId;
    protected String processDefinitionKey;
    protected ProcessDefinitionEntity processDefinitionEntity;
    protected boolean includeProcessInstances;
    protected Date executionDate;
    protected String tenantId;

    public AbstractSetProcessDefinitionStateCmd(ProcessDefinitionEntity processDefinitionEntity, boolean includeProcessInstances, Date executionDate, String tenantId) {
        this.processDefinitionEntity = processDefinitionEntity;
        this.includeProcessInstances = includeProcessInstances;
        this.executionDate = executionDate;
        this.tenantId = tenantId;
    }

    public AbstractSetProcessDefinitionStateCmd(String processDefinitionId, String processDefinitionKey, boolean includeProcessInstances, Date executionDate, String tenantId) {
        this.processDefinitionId = processDefinitionId;
        this.processDefinitionKey = processDefinitionKey;
        this.includeProcessInstances = includeProcessInstances;
        this.executionDate = executionDate;
        this.tenantId = tenantId;
    }

    @Override
    public Void execute(CommandContext commandContext) {

        List<ProcessDefinitionEntity> processDefinitions = findProcessDefinition(commandContext);
        boolean hasV5ProcessDefinitions = false;
        for (ProcessDefinitionEntity processDefinitionEntity : processDefinitions) {
            if (Flowable5Util.isFlowable5ProcessDefinition(processDefinitionEntity, commandContext)) {
                hasV5ProcessDefinitions = true;
                break;
            }
        }

        if (hasV5ProcessDefinitions) {
            Flowable5CompatibilityHandler compatibilityHandler = Flowable5Util.getFlowable5CompatibilityHandler();
            if (getProcessDefinitionSuspensionState() == SuspensionState.ACTIVE) {
                compatibilityHandler.activateProcessDefinition(processDefinitionId, processDefinitionKey, includeProcessInstances, executionDate, tenantId);
            } else if (getProcessDefinitionSuspensionState() == SuspensionState.SUSPENDED) {
                compatibilityHandler.suspendProcessDefinition(processDefinitionId, processDefinitionKey, includeProcessInstances, executionDate, tenantId);
            }
            return null;
        }

        if (executionDate != null) { // Process definition state change is delayed
            createTimerForDelayedExecution(commandContext, processDefinitions);
        } else { // Process definition state is changed now
            changeProcessDefinitionState(commandContext, processDefinitions);
        }

        return null;
    }

    protected List<ProcessDefinitionEntity> findProcessDefinition(CommandContext commandContext) {

        // If process definition is already provided (eg. when command is called through the DeployCmd)
        // we don't need to do an extra database fetch and we can simply return it, wrapped in a list
        if (processDefinitionEntity != null) {
            return Collections.singletonList(processDefinitionEntity);
        }

        // Validation of input parameters
        if (processDefinitionId == null && processDefinitionKey == null) {
            throw new FlowableIllegalArgumentException("Process definition id or key cannot be null");
        }

        List<ProcessDefinitionEntity> processDefinitionEntities = new ArrayList<>();
        ProcessDefinitionEntityManager processDefinitionManager = CommandContextUtil.getProcessDefinitionEntityManager(commandContext);

        if (processDefinitionId != null) {

            ProcessDefinitionEntity processDefinitionEntity = processDefinitionManager.findById(processDefinitionId);
            if (processDefinitionEntity == null) {
                throw new FlowableObjectNotFoundException("Cannot find process definition for id '" + processDefinitionId + "'", ProcessDefinition.class);
            }
            processDefinitionEntities.add(processDefinitionEntity);

        } else {

            ProcessDefinitionQueryImpl query = new ProcessDefinitionQueryImpl(commandContext).processDefinitionKey(processDefinitionKey);

            if (tenantId == null || ProcessEngineConfiguration.NO_TENANT_ID.equals(tenantId)) {
                query.processDefinitionWithoutTenantId();
            } else {
                query.processDefinitionTenantId(tenantId);
            }

            List<ProcessDefinition> processDefinitions = query.list();
            if (processDefinitions.isEmpty()) {
                throw new FlowableException("Cannot find process definition for key '" + processDefinitionKey + "'");
            }

            for (ProcessDefinition processDefinition : processDefinitions) {
                processDefinitionEntities.add((ProcessDefinitionEntity) processDefinition);
            }

        }
        return processDefinitionEntities;
    }

    protected void createTimerForDelayedExecution(CommandContext commandContext, List<ProcessDefinitionEntity> processDefinitions) {
        for (ProcessDefinitionEntity processDefinition : processDefinitions) {

            if (Flowable5Util.isFlowable5ProcessDefinition(processDefinition, commandContext))
                continue;

            TimerJobService timerJobService = CommandContextUtil.getTimerJobService(commandContext);
            TimerJobEntity timer = timerJobService.createTimerJob();
            timer.setJobType(JobEntity.JOB_TYPE_TIMER);
            timer.setProcessDefinitionId(processDefinition.getId());

            // Inherit tenant identifier (if applicable)
            if (processDefinition.getTenantId() != null) {
                timer.setTenantId(processDefinition.getTenantId());
            }

            timer.setDuedate(executionDate);
            timer.setJobHandlerType(getDelayedExecutionJobHandlerType());
            timer.setJobHandlerConfiguration(TimerChangeProcessDefinitionSuspensionStateJobHandler.createJobHandlerConfiguration(includeProcessInstances));
            timerJobService.scheduleTimerJob(timer);
        }
    }

    protected void changeProcessDefinitionState(CommandContext commandContext, List<ProcessDefinitionEntity> processDefinitions) {
        for (ProcessDefinitionEntity processDefinition : processDefinitions) {

            if (Flowable5Util.isFlowable5ProcessDefinition(processDefinition, commandContext))
                continue;

            SuspensionStateUtil.setSuspensionState(processDefinition, getProcessDefinitionSuspensionState());

            // Evict cache
            CommandContextUtil.getProcessEngineConfiguration(commandContext).getDeploymentManager().getProcessDefinitionCache().remove(processDefinition.getId());

            // Suspend process instances (if needed)
            if (includeProcessInstances) {

                int currentStartIndex = 0;
                List<ProcessInstance> processInstances = fetchProcessInstancesPage(commandContext, processDefinition, currentStartIndex);
                while (!processInstances.isEmpty()) {

                    for (ProcessInstance processInstance : processInstances) {
                        AbstractSetProcessInstanceStateCmd processInstanceCmd = getProcessInstanceChangeStateCmd(processInstance);
                        processInstanceCmd.execute(commandContext);
                    }

                    // Fetch new batch of process instances
                    currentStartIndex += processInstances.size();
                    processInstances = fetchProcessInstancesPage(commandContext, processDefinition, currentStartIndex);
                }
            }
        }
    }

    protected List<ProcessInstance> fetchProcessInstancesPage(CommandContext commandContext, ProcessDefinition processDefinition, int currentPageStartIndex) {

        if (SuspensionState.ACTIVE.equals(getProcessDefinitionSuspensionState())) {
            return new ProcessInstanceQueryImpl(commandContext).processDefinitionId(processDefinition.getId()).suspended()
                    .listPage(currentPageStartIndex, CommandContextUtil.getProcessEngineConfiguration(commandContext).getBatchSizeProcessInstances());
        } else {
            return new ProcessInstanceQueryImpl(commandContext).processDefinitionId(processDefinition.getId()).active()
                    .listPage(currentPageStartIndex, CommandContextUtil.getProcessEngineConfiguration(commandContext).getBatchSizeProcessInstances());
        }
    }

    // ABSTRACT METHODS
    // ////////////////////////////////////////////////////////////////////

    /**
     * Subclasses should return the wanted {@link SuspensionState} here.
     */
    protected abstract SuspensionState getProcessDefinitionSuspensionState();

    /**
     * Subclasses should return the type of the {@link JobHandler} here. it will be used when the user provides an execution date on which the actual state change will happen.
     */
    protected abstract String getDelayedExecutionJobHandlerType();

    /**
     * Subclasses should return a {@link Command} implementation that matches the process definition state change.
     */
    protected abstract AbstractSetProcessInstanceStateCmd getProcessInstanceChangeStateCmd(ProcessInstance processInstance);

}
