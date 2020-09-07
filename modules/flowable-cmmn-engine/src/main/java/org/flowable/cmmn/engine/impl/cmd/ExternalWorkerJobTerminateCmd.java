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
package org.flowable.cmmn.engine.impl.cmd;

import java.util.Map;

import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.impl.persistence.entity.CountingPlanItemInstanceEntity;
import org.flowable.cmmn.engine.impl.persistence.entity.PlanItemInstanceEntity;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.job.service.impl.persistence.entity.ExternalWorkerJobEntity;
import org.flowable.variable.api.types.VariableType;
import org.flowable.variable.api.types.VariableTypes;
import org.flowable.variable.service.VariableService;
import org.flowable.variable.service.VariableServiceConfiguration;
import org.flowable.variable.service.impl.persistence.entity.VariableInstanceEntity;

/**
 * @author Filip Hrisafov
 */
public class ExternalWorkerJobTerminateCmd extends AbstractExternalWorkerJobCmd implements Command<Void> {

    protected Map<String, Object> variables;

    public ExternalWorkerJobTerminateCmd(String externalJobId, String workerId, Map<String, Object> variables) {
        super(externalJobId, workerId);
        this.variables = variables;
    }

    @Override
    protected void runJobLogic(ExternalWorkerJobEntity externalWorkerJob, CommandContext commandContext) {
        externalWorkerJob.setJobHandlerConfiguration("terminate:");

        if (variables != null && !variables.isEmpty()) {
            CmmnEngineConfiguration cmmnEngineConfiguration = CommandContextUtil.getCmmnEngineConfiguration(commandContext);
            VariableServiceConfiguration variableServiceConfiguration = cmmnEngineConfiguration.getVariableServiceConfiguration();
            VariableService variableService = variableServiceConfiguration.getVariableService();
            VariableTypes variableTypes = variableServiceConfiguration.getVariableTypes();
            for (Map.Entry<String, Object> variableEntry : variables.entrySet()) {
                String varName = variableEntry.getKey();
                Object varValue = variableEntry.getValue();

                VariableType variableType = variableTypes.findVariableType(varValue);
                VariableInstanceEntity variableInstance = variableService.createVariableInstance(varName, variableType, varValue);
                variableInstance.setScopeId(externalWorkerJob.getScopeId());
                variableInstance.setSubScopeId(externalWorkerJob.getSubScopeId());
                variableInstance.setScopeType(ScopeTypes.CMMN_EXTERNAL_WORKER);

                variableService.insertVariableInstance(variableInstance);
            }

            PlanItemInstanceEntity planItemInstanceEntity = cmmnEngineConfiguration.getPlanItemInstanceEntityManager()
                    .findById(externalWorkerJob.getSubScopeId());

            if (planItemInstanceEntity instanceof CountingPlanItemInstanceEntity) {
                ((CountingPlanItemInstanceEntity) planItemInstanceEntity)
                        .setVariableCount(((CountingPlanItemInstanceEntity) planItemInstanceEntity).getVariableCount() + variables.size());
            }
        }

        moveExternalWorkerJobToExecutableJob(externalWorkerJob, commandContext);
    }
}
