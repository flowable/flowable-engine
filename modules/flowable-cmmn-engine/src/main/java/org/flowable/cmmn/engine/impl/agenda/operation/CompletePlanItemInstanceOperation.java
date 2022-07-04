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
import java.util.Map;

import org.flowable.cmmn.api.event.FlowableCaseStageEndedEvent;
import org.flowable.cmmn.api.runtime.PlanItemInstanceState;
import org.flowable.cmmn.engine.impl.event.FlowableCmmnEventBuilder;
import org.flowable.cmmn.engine.impl.persistence.entity.PlanItemInstanceEntity;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.cmmn.model.PlanItemTransition;
import org.flowable.common.engine.api.delegate.event.FlowableEventDispatcher;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.interceptor.EngineConfigurationConstants;

/**
 * @author Joram Barrez
 */
public class CompletePlanItemInstanceOperation extends AbstractMovePlanItemInstanceToTerminalStateOperation {
    
    public CompletePlanItemInstanceOperation(CommandContext commandContext, PlanItemInstanceEntity planItemInstanceEntity) {
        super(commandContext, planItemInstanceEntity);
    }

    @Override
    public String getNewState() {
        return PlanItemInstanceState.COMPLETED;
    }
    
    @Override
    public String getLifeCycleTransition() {
        return PlanItemTransition.COMPLETE;
    }
    
    @Override
    public boolean isEvaluateRepetitionRule() {
        return true;
    }

    @Override
    protected boolean shouldAggregateForSingleInstance() {
        return true;
    }

    @Override
    protected boolean shouldAggregateForMultipleInstances() {
        return true;
    }

    @Override
    protected void internalExecute() {
        if (isStage(planItemInstanceEntity)) {
            // terminate any remaining child plan items (e.g. in enabled / available state), but don't complete them as it might lead
            // into wrong behavior resulting from it (e.g. triggering some follow-up actions on that completion event) and it will leave
            // such implicitly completed plan items in complete state, although they were never explicitly completed
            exitChildPlanItemInstances(PlanItemTransition.COMPLETE, null, null);

            // create stage ended with completion state event
            FlowableEventDispatcher eventDispatcher = CommandContextUtil.getCmmnEngineConfiguration(commandContext).getEventDispatcher();
            if (eventDispatcher != null && eventDispatcher.isEnabled()) {
                eventDispatcher.dispatchEvent(FlowableCmmnEventBuilder.createStageEndedEvent(getCaseInstance(), planItemInstanceEntity,
                        FlowableCaseStageEndedEvent.ENDING_STATE_COMPLETED), EngineConfigurationConstants.KEY_CMMN_ENGINE_CONFIG);
            }
        }

        planItemInstanceEntity.setEndedTime(getCurrentTime(commandContext));
        planItemInstanceEntity.setCompletedTime(planItemInstanceEntity.getEndedTime());
        CommandContextUtil.getCmmnHistoryManager(commandContext).recordPlanItemInstanceCompleted(planItemInstanceEntity);
    }

    @Override
    protected Map<String, String> getAsyncLeaveTransitionMetadata() {
        Map<String, String> metadata = new HashMap<>();
        metadata.put(OperationSerializationMetadata.FIELD_PLAN_ITEM_INSTANCE_ID, planItemInstanceEntity.getId());
        return metadata;
    }

    @Override
    public String getOperationName() {
        return "[Complete plan item]";
    }

}
