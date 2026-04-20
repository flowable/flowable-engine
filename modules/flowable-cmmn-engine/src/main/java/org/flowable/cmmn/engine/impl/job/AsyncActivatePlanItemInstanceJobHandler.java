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

import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.impl.persistence.entity.PlanItemInstanceEntity;
import org.flowable.cmmn.engine.impl.util.CmmnLoggingSessionUtil;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.logging.CmmnLoggingSessionConstants;
import org.flowable.job.service.JobHandler;
import org.flowable.job.service.impl.persistence.entity.JobEntity;
import org.flowable.variable.api.delegate.VariableScope;

/**
 * @author Joram Barrez
 */
public class AsyncActivatePlanItemInstanceJobHandler implements JobHandler {
    
    public static final String TYPE = "cmmn-async-activate-plan-item-instance";

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public void execute(JobEntity job, String configuration, VariableScope variableScope, CommandContext commandContext) {
        if (variableScope instanceof PlanItemInstanceEntity planItemInstanceEntity) {
            CmmnEngineConfiguration cmmnEngineConfiguration = CommandContextUtil.getCmmnEngineConfiguration(commandContext);
            if (cmmnEngineConfiguration.isLoggingSessionEnabled()) {
                CmmnLoggingSessionUtil.addAsyncActivityLoggingData("Executing async job for " + planItemInstanceEntity.getPlanItemDefinitionId() + ", with job id " + job.getId(),
                        CmmnLoggingSessionConstants.TYPE_SERVICE_TASK_EXECUTE_ASYNC_JOB, job, planItemInstanceEntity.getPlanItemDefinition(), 
                        planItemInstanceEntity, cmmnEngineConfiguration.getObjectMapper());
            }
            
            CommandContextUtil.getAgenda(commandContext).planActivatePlanItemInstanceOperation(planItemInstanceEntity, configuration); // configuration == entryCriterionId
            
        } else {
            throw new FlowableException("Invalid usage of " + TYPE + " job handler, variable scope is of type " + variableScope.getClass());
        }
    }

}
