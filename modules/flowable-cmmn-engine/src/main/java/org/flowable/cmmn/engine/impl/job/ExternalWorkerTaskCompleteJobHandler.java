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
package org.flowable.cmmn.engine.impl.job;

import java.util.List;

import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.impl.persistence.entity.CountingPlanItemInstanceEntity;
import org.flowable.cmmn.engine.impl.persistence.entity.PlanItemInstanceEntity;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.job.service.JobHandler;
import org.flowable.job.service.impl.persistence.entity.JobEntity;
import org.flowable.variable.api.delegate.VariableScope;
import org.flowable.variable.service.VariableService;
import org.flowable.variable.service.impl.persistence.entity.VariableInstanceEntity;

/**
 * @author Filip Hrisafov
 */
public class ExternalWorkerTaskCompleteJobHandler implements JobHandler {

    public static final String TYPE = "cmmn-external-worker-complete";
    
    protected CmmnEngineConfiguration cmmnEngineConfiguration;
    
    public ExternalWorkerTaskCompleteJobHandler(CmmnEngineConfiguration cmmnEngineConfiguration) {
        this.cmmnEngineConfiguration = cmmnEngineConfiguration;
    }

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public void execute(JobEntity job, String configuration, VariableScope variableScope, CommandContext commandContext) {
        PlanItemInstanceEntity planItemInstanceEntity = (PlanItemInstanceEntity) variableScope;

        VariableService variableService = cmmnEngineConfiguration.getVariableServiceConfiguration().getVariableService();
        List<VariableInstanceEntity> jobVariables = variableService.findVariableInstanceBySubScopeIdAndScopeType(planItemInstanceEntity.getId(), ScopeTypes.CMMN_EXTERNAL_WORKER);

        if (!jobVariables.isEmpty()) {
            for (VariableInstanceEntity jobVariable : jobVariables) {
                planItemInstanceEntity.setVariable(jobVariable.getName(), jobVariable.getValue());
                variableService.deleteVariableInstance(jobVariable);
            }

            if (planItemInstanceEntity instanceof CountingPlanItemInstanceEntity) {
                ((CountingPlanItemInstanceEntity) planItemInstanceEntity)
                        .setVariableCount(((CountingPlanItemInstanceEntity) planItemInstanceEntity).getVariableCount() - jobVariables.size());
            }
        }

        if (configuration != null && configuration.startsWith("terminate:")) {
            //TODO maybe pass exitType and exitEventType
            CommandContextUtil.getAgenda(commandContext).planTerminatePlanItemInstanceOperation(planItemInstanceEntity, null, null);
        } else {
            CommandContextUtil.getAgenda(commandContext).planCompletePlanItemInstanceOperation(planItemInstanceEntity);
        }
    }

}
