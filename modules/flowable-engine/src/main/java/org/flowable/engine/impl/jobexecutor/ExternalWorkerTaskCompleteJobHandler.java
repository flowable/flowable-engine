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
package org.flowable.engine.impl.jobexecutor;

import java.util.List;

import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.impl.bpmn.helper.ErrorPropagation;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.impl.util.CountingEntityUtil;
import org.flowable.job.service.JobHandler;
import org.flowable.job.service.impl.persistence.entity.JobEntity;
import org.flowable.variable.api.delegate.VariableScope;
import org.flowable.variable.service.VariableService;
import org.flowable.variable.service.impl.persistence.entity.VariableInstanceEntity;

/**
 * @author Filip Hrisafov
 */
public class ExternalWorkerTaskCompleteJobHandler implements JobHandler {

    public static final String TYPE = "external-worker-complete";

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public void execute(JobEntity job, String configuration, VariableScope variableScope, CommandContext commandContext) {
        ExecutionEntity executionEntity = (ExecutionEntity) variableScope;

        ProcessEngineConfigurationImpl processEngineConfiguration = CommandContextUtil.getProcessEngineConfiguration(commandContext);
        VariableService variableService = processEngineConfiguration.getVariableServiceConfiguration().getVariableService();
        List<VariableInstanceEntity> jobVariables = variableService.findVariableInstanceBySubScopeIdAndScopeType(executionEntity.getId(), ScopeTypes.BPMN_EXTERNAL_WORKER);
        for (VariableInstanceEntity jobVariable : jobVariables) {
            executionEntity.setVariable(jobVariable.getName(), jobVariable.getValue());
            CountingEntityUtil.handleDeleteVariableInstanceEntityCount(jobVariable, false);
            variableService.deleteVariableInstance(jobVariable);
        }

        if (configuration != null && configuration.startsWith("error:")) {
            String errorCode;
            if (configuration.length() > 6) {
                errorCode = configuration.substring(6);
            } else {
                errorCode = null;
            }
            ErrorPropagation.propagateError(errorCode, executionEntity);
        } else {
            CommandContextUtil.getAgenda(commandContext).planTriggerExecutionOperation(executionEntity);
        }
    }

}
