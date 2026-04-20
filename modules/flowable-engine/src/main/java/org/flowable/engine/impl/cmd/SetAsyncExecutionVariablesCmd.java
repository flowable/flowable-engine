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

import java.util.Map;

import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.jobexecutor.SetAsyncVariablesJobHandler;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.impl.util.CountingEntityUtil;
import org.flowable.job.service.JobService;
import org.flowable.job.service.JobServiceConfiguration;
import org.flowable.job.service.impl.persistence.entity.JobEntity;
import org.flowable.variable.service.VariableService;
import org.flowable.variable.service.VariableServiceConfiguration;
import org.flowable.variable.service.impl.persistence.entity.VariableInstanceEntity;

public class SetAsyncExecutionVariablesCmd extends NeedsActiveExecutionCmd<Object> {

    private static final long serialVersionUID = 1L;

    protected Map<String, ? extends Object> variables;
    protected boolean isLocal;

    public SetAsyncExecutionVariablesCmd(String executionId, Map<String, ? extends Object> variables, boolean isLocal) {
        super(executionId);
        this.variables = variables;
        this.isLocal = isLocal;
    }

    @Override
    protected Object execute(CommandContext commandContext, ExecutionEntity execution) {
        if (variables != null && !variables.isEmpty()) {
            ProcessEngineConfigurationImpl processEngineConfiguration = CommandContextUtil.getProcessEngineConfiguration(commandContext);
            VariableServiceConfiguration variableServiceConfiguration = processEngineConfiguration.getVariableServiceConfiguration();
            VariableService variableService = variableServiceConfiguration.getVariableService();
            
            for (String variableName : variables.keySet()) {
                addVariable(isLocal, execution.getProcessInstanceId(), execution.getId(), variableName, variables.get(variableName),
                        execution.getTenantId(), variableService);
            }
            
            createSetAsyncVariablesJob(execution, processEngineConfiguration);
        }
        
        return null;
    }
    
    protected void addVariable(boolean isLocal, String scopeId, String subScopeId, String varName, Object varValue, 
            String tenantId, VariableService variableService) {
        
        VariableInstanceEntity variableInstance = variableService.createVariableInstance(varName);
        variableInstance.setScopeId(scopeId);
        variableInstance.setSubScopeId(subScopeId);
        variableInstance.setScopeType(ScopeTypes.BPMN_ASYNC_VARIABLES);
        variableInstance.setMetaInfo(String.valueOf(isLocal));

        variableService.insertVariableInstanceWithValue(variableInstance, varValue, tenantId);

        CountingEntityUtil.handleInsertVariableInstanceEntityCount(variableInstance);
    }
    
    protected void createSetAsyncVariablesJob(ExecutionEntity execution, ProcessEngineConfigurationImpl processEngineConfiguration) {
        JobServiceConfiguration jobServiceConfiguration = processEngineConfiguration.getJobServiceConfiguration();
        JobService jobService = jobServiceConfiguration.getJobService();

        JobEntity job = jobService.createJob();
        job.setExecutionId(execution.getId());
        job.setProcessInstanceId(execution.getProcessInstanceId());
        job.setProcessDefinitionId(execution.getProcessDefinitionId());
        job.setJobHandlerType(SetAsyncVariablesJobHandler.TYPE);

        // Inherit tenant id (if applicable)
        if (execution.getTenantId() != null) {
            job.setTenantId(execution.getTenantId());
        }

        jobService.createAsyncJob(job, true);
        jobService.scheduleAsyncJob(job);
    }

    @Override
    protected String getSuspendedExceptionMessagePrefix() {
        return "Cannot set variables to";
    }

}
