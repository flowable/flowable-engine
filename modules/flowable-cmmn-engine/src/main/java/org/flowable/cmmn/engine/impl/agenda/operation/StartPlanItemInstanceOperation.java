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

import org.flowable.cmmn.api.runtime.PlanItemInstanceState;
import org.flowable.cmmn.engine.impl.behavior.CmmnActivityBehavior;
import org.flowable.cmmn.engine.impl.behavior.CmmnActivityWithMigrationContextBehavior;
import org.flowable.cmmn.engine.impl.behavior.CoreCmmnActivityBehavior;
import org.flowable.cmmn.engine.impl.behavior.impl.ChildTaskActivityBehavior;
import org.flowable.cmmn.engine.impl.persistence.entity.PlanItemInstanceEntity;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.cmmn.engine.interceptor.MigrationContext;
import org.flowable.cmmn.model.PlanItemTransition;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.impl.interceptor.CommandContext;

/**
 * @author Joram Barrez
 */
public class StartPlanItemInstanceOperation extends AbstractChangePlanItemInstanceStateOperation {

    protected String entryCriterionId;
    protected ChildTaskActivityBehavior.VariableInfo childTaskVariableInfo;
    protected MigrationContext migrationContext;
    
    public StartPlanItemInstanceOperation(CommandContext commandContext, PlanItemInstanceEntity planItemInstanceEntity, String entryCriterionId) {
        super(commandContext, planItemInstanceEntity);
        this.entryCriterionId = entryCriterionId;
    }
    
    public StartPlanItemInstanceOperation(CommandContext commandContext, PlanItemInstanceEntity planItemInstanceEntity, 
                    String entryCriterionId, ChildTaskActivityBehavior.VariableInfo childTaskVariableInfo) {
        
        this(commandContext, planItemInstanceEntity, entryCriterionId);
        this.childTaskVariableInfo = childTaskVariableInfo;
    }
    
    public StartPlanItemInstanceOperation(CommandContext commandContext, PlanItemInstanceEntity planItemInstanceEntity, 
            String entryCriterionId, MigrationContext migrationContext) {
        
        this(commandContext, planItemInstanceEntity, entryCriterionId);
        this.migrationContext = migrationContext;
    }
    
    @Override
    public String getNewState() {
        return PlanItemInstanceState.ACTIVE;
    }
    
    @Override
    public String getLifeCycleTransition() {
        return PlanItemTransition.START;
    }
    
    @Override
    protected void internalExecute() {
        // Sentries are not needed to be kept around, as the plan item is being started
        removeSentryRelatedData();

        planItemInstanceEntity.setEntryCriterionId(entryCriterionId);
        planItemInstanceEntity.setLastStartedTime(getCurrentTime(commandContext));
        executeActivityBehavior();
        CommandContextUtil.getCmmnHistoryManager(commandContext).recordPlanItemInstanceStarted(planItemInstanceEntity);
    }

    protected void executeActivityBehavior() {
        CmmnActivityBehavior activityBehavior = (CmmnActivityBehavior) planItemInstanceEntity.getPlanItem().getBehavior();
        if (migrationContext != null && activityBehavior instanceof CmmnActivityWithMigrationContextBehavior) {
            ((CmmnActivityWithMigrationContextBehavior) activityBehavior).execute(commandContext, planItemInstanceEntity, migrationContext);
            
        } else if (activityBehavior instanceof ChildTaskActivityBehavior) {
            ((ChildTaskActivityBehavior) activityBehavior).execute(commandContext, planItemInstanceEntity, childTaskVariableInfo);
            
        } else if (activityBehavior instanceof CoreCmmnActivityBehavior) {
            ((CoreCmmnActivityBehavior) activityBehavior).execute(commandContext, planItemInstanceEntity);
            
        } else if (activityBehavior != null) {
            activityBehavior.execute(planItemInstanceEntity);

        } else {
            throw new FlowableException("PlanItemInstance " + planItemInstanceEntity + " does not have a behavior");

        }
    }

    @Override
    public String getOperationName() {
        return "[Start plan item]";
    }
    
}
