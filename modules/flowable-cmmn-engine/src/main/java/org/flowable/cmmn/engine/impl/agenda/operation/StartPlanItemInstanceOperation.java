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

import java.util.Map;

import org.flowable.cmmn.api.runtime.PlanItemInstanceState;
import org.flowable.cmmn.engine.impl.behavior.CmmnActivityBehavior;
import org.flowable.cmmn.engine.impl.behavior.CoreCmmnActivityBehavior;
import org.flowable.cmmn.engine.impl.behavior.impl.ChildTaskActivityBehavior;
import org.flowable.cmmn.engine.impl.persistence.entity.PlanItemInstanceEntity;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.cmmn.model.PlanItemTransition;
import org.flowable.common.engine.impl.interceptor.CommandContext;

/**
 * @author Joram Barrez
 */
public class StartPlanItemInstanceOperation extends AbstractChangePlanItemInstanceStateOperation {

    protected String entryCriterionId;
    protected Map<String, Object> variables;
    
    public StartPlanItemInstanceOperation(CommandContext commandContext, PlanItemInstanceEntity planItemInstanceEntity, String entryCriterionId) {
        super(commandContext, planItemInstanceEntity);
        this.entryCriterionId = entryCriterionId;
    }
    
    public StartPlanItemInstanceOperation(CommandContext commandContext, PlanItemInstanceEntity planItemInstanceEntity, 
                    String entryCriterionId, Map<String, Object> variables) {
        
        this(commandContext, planItemInstanceEntity, entryCriterionId);
        this.variables = variables;
    }
    
    @Override
    protected String getNewState() {
        return PlanItemInstanceState.ACTIVE;
    }
    
    @Override
    protected String getLifeCycleTransition() {
        return PlanItemTransition.START;
    }
    
    @Override
    protected void internalExecute() {
        // Sentries are not needed to be kept around, as the plan item is being started
        removeSentryRelatedData();

        planItemInstanceEntity.setEntryCriterionId(entryCriterionId);
        planItemInstanceEntity.setLastStartedTime(getCurrentTime(commandContext));
        CommandContextUtil.getCmmnHistoryManager(commandContext).recordPlanItemInstanceStarted(planItemInstanceEntity);
        executeActivityBehavior();
    }

    protected void executeActivityBehavior() {
        CmmnActivityBehavior activityBehavior = (CmmnActivityBehavior) planItemInstanceEntity.getPlanItem().getBehavior();
        if (activityBehavior instanceof ChildTaskActivityBehavior) {
            ((ChildTaskActivityBehavior) activityBehavior).execute(commandContext, planItemInstanceEntity, variables);
            
        } else if (activityBehavior instanceof CoreCmmnActivityBehavior) {
            ((CoreCmmnActivityBehavior) activityBehavior).execute(commandContext, planItemInstanceEntity);
            
        } else {
            activityBehavior.execute(planItemInstanceEntity);
        }
    }
    
}
