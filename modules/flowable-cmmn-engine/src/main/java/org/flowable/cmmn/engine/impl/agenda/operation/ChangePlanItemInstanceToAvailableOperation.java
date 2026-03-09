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

import java.util.List;

import org.flowable.cmmn.api.runtime.PlanItemInstanceState;
import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.impl.persistence.entity.PlanItemInstanceEntity;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.cmmn.model.PlanItemDefinition;
import org.flowable.cmmn.model.PlanItemTransition;
import org.flowable.cmmn.model.TimerEventListener;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.job.api.Job;
import org.flowable.job.service.impl.SuspendedJobQueryImpl;
import org.flowable.job.service.impl.persistence.entity.SuspendedJobEntity;

/**
 * @author Tijs Rademakers
 */
public class ChangePlanItemInstanceToAvailableOperation extends AbstractChangePlanItemInstanceStateOperation {
    
    protected boolean enableSuspendedJobs;
    
    public ChangePlanItemInstanceToAvailableOperation(CommandContext commandContext, PlanItemInstanceEntity planItemInstanceEntity) {
        super(commandContext, planItemInstanceEntity);
    }
    
    public ChangePlanItemInstanceToAvailableOperation(CommandContext commandContext, PlanItemInstanceEntity planItemInstanceEntity, boolean enableSuspendedJobs) {
        super(commandContext, planItemInstanceEntity);
        
        this.enableSuspendedJobs = enableSuspendedJobs;
    }
    
    @Override
    public String getLifeCycleTransition() {
        return PlanItemTransition.CREATE;
    }
    
    @Override
    public String getNewState() {
        return PlanItemInstanceState.AVAILABLE;
    }
    
    @Override
    protected void internalExecute() {
        planItemInstanceEntity.setLastAvailableTime(getCurrentTime(commandContext));
        
        if (enableSuspendedJobs) {
            PlanItemDefinition planItemDefinition = planItemInstanceEntity.getPlanItem().getPlanItemDefinition();
            if (planItemDefinition instanceof TimerEventListener) {
                CmmnEngineConfiguration cmmnEngineConfiguration = CommandContextUtil.getCmmnEngineConfiguration(commandContext);
                SuspendedJobQueryImpl suspendedJobQuery = new SuspendedJobQueryImpl(commandContext, cmmnEngineConfiguration.getJobServiceConfiguration());
                suspendedJobQuery.subScopeId(planItemInstanceEntity.getId());
                List<Job> suspendedJobs = cmmnEngineConfiguration.getJobServiceConfiguration().getSuspendedJobEntityManager().findJobsByQueryCriteria(suspendedJobQuery);
                if (suspendedJobs != null && !suspendedJobs.isEmpty()) {
                    cmmnEngineConfiguration.getJobServiceConfiguration().getJobService().activateSuspendedJob((SuspendedJobEntity) suspendedJobs.get(0));
                }
            }
        }
        
        CommandContextUtil.getCmmnHistoryManager(commandContext).recordPlanItemInstanceAvailable(planItemInstanceEntity);
    }

    @Override
    public String getOperationName() {
        return null; // Default one is ok.
    }
    
}
