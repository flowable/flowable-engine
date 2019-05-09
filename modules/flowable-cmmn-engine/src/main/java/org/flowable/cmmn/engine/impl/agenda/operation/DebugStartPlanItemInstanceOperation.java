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
package org.flowable.cmmn.engine.impl.agenda.operation;

import org.flowable.cmmn.api.runtime.CmmnDebugger;
import org.flowable.cmmn.engine.impl.job.ActivateCmmnBreakpointJobHandler;
import org.flowable.cmmn.engine.impl.persistence.entity.PlanItemInstanceEntity;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.cmmn.model.Task;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.job.service.JobService;
import org.flowable.job.service.impl.persistence.entity.JobEntity;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Start plan item instance in the debug mode. Creates debug async job to to allow case execution triggering
 *
 * @author martin.grofcik
 */
public class DebugStartPlanItemInstanceOperation extends StartPlanItemInstanceOperation {

    public static final String START_PLAN_ITEM_INSTANCE_OPERATION = "StartPlanItemInstanceOperation";

    protected CmmnDebugger debugger;

    public DebugStartPlanItemInstanceOperation(CommandContext commandContext,
        CmmnDebugger cmmnDebugger, PlanItemInstanceEntity planItemInstanceEntity, String entryCriterionId) {
        super(commandContext, planItemInstanceEntity, entryCriterionId);
        this.debugger = cmmnDebugger;
    }

    @Override
    protected void internalExecute() {
        if (debugger.isBreakPoint(entryCriterionId, planItemInstanceEntity)) {
            ObjectMapper objectMapper = CommandContextUtil.getCmmnEngineConfiguration(commandContext).getObjectMapper();
            ObjectNode configuration = objectMapper.createObjectNode();
            configuration.
                put("operation", START_PLAN_ITEM_INSTANCE_OPERATION).
                put("entryCriterionId", entryCriterionId);
            createAsyncJob(configuration.toString(), (Task) planItemInstanceEntity.getPlanItem().getPlanItemDefinition());
        } else {
            super.internalExecute();
        }
    }

    protected void createAsyncJob(String configuration, Task task) {
        JobService jobService = CommandContextUtil.getCmmnEngineConfiguration(commandContext).getJobServiceConfiguration().getJobService();
        JobEntity job = jobService.createJob();
        job.setJobHandlerType(ActivateCmmnBreakpointJobHandler.CMMN_BREAKPOINT);
        job.setScopeId(planItemInstanceEntity.getCaseInstanceId());
        job.setSubScopeId(planItemInstanceEntity.getId());
        job.setScopeDefinitionId(planItemInstanceEntity.getCaseDefinitionId());
        job.setScopeType(ScopeTypes.CMMN);
        job.setJobHandlerConfiguration(configuration);
        job.setTenantId(planItemInstanceEntity.getTenantId());
        jobService.createAsyncJob(job, task.isExclusive());
        jobService.moveJobToSuspendedJob(job);
    }

}
