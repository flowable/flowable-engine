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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flowable.cmmn.api.runtime.PlanItemInstanceState;
import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.impl.persistence.entity.PlanItemInstanceEntity;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.cmmn.model.PlanItemDefinition;
import org.flowable.cmmn.model.PlanItemTransition;
import org.flowable.cmmn.model.TimerEventListener;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.job.api.Job;
import org.flowable.job.service.impl.TimerJobQueryImpl;
import org.flowable.job.service.impl.persistence.entity.AbstractRuntimeJobEntity;

public class SuspendPlanItemInstanceOperation extends AbstractMovePlanItemInstanceToTerminalStateOperation {

    public SuspendPlanItemInstanceOperation(CommandContext commandContext, PlanItemInstanceEntity planItemInstanceEntity) {
        super(commandContext, planItemInstanceEntity);
    }

    @Override
    public String getNewState() {
        return PlanItemInstanceState.SUSPENDED;
    }

    @Override
    public String getLifeCycleTransition() {
        return PlanItemTransition.SUSPEND;
    }
    
    @Override
    public boolean isEvaluateRepetitionRule() {
        return false;
    }
    
    @Override
    protected boolean shouldAggregateForSingleInstance() {
        return false;
    }

    @Override
    protected boolean shouldAggregateForMultipleInstances() {
        return false;
    }

    @Override
    protected void internalExecute() {
        planItemInstanceEntity.setLastSuspendedTime(getCurrentTime(commandContext));
        
        PlanItemDefinition planItemDefinition = planItemInstanceEntity.getPlanItem().getPlanItemDefinition();
        if (planItemDefinition instanceof TimerEventListener) {
            CmmnEngineConfiguration cmmnEngineConfiguration = CommandContextUtil.getCmmnEngineConfiguration(commandContext);
            TimerJobQueryImpl timerJobQuery = new TimerJobQueryImpl(commandContext, cmmnEngineConfiguration.getJobServiceConfiguration());
            timerJobQuery.subScopeId(planItemInstanceEntity.getId());
            List<Job> timerJobs = cmmnEngineConfiguration.getJobServiceConfiguration().getTimerJobEntityManager().findJobsByQueryCriteria(timerJobQuery);
            if (timerJobs != null && !timerJobs.isEmpty()) {
                cmmnEngineConfiguration.getJobServiceConfiguration().getJobService().moveJobToSuspendedJob((AbstractRuntimeJobEntity) timerJobs.get(0));
            }
        }
        
        CommandContextUtil.getCmmnHistoryManager(commandContext).recordPlanItemInstanceSuspended(planItemInstanceEntity);
    }

    @Override
    protected Map<String, String> getAsyncLeaveTransitionMetadata() {
        Map<String, String> metadata = new HashMap<>();
        metadata.put(OperationSerializationMetadata.FIELD_PLAN_ITEM_INSTANCE_ID, planItemInstanceEntity.getId());
        return metadata;
    }

    @Override
    public boolean abortOperationIfNewStateEqualsOldState() {
        return true;
    }

    @Override
    public String getOperationName() {
        return "[Suspend plan item]";
    }
}
