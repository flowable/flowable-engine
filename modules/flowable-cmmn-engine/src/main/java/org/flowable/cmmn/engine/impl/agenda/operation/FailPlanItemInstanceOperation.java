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

import org.flowable.cmmn.api.runtime.PlanItemInstanceState;
import org.flowable.cmmn.engine.impl.persistence.entity.PlanItemInstanceEntity;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.cmmn.model.PlanItemTransition;
import org.flowable.common.engine.impl.interceptor.CommandContext;

/**
 * Operation that moves a plan item instance to the {@link PlanItemInstanceState#FAILED} state
 * via the {@link PlanItemTransition#FAULT} transition.
 *
 * For tasks: children are NOT affected (non-propagating per CMMN 1.1 spec).
 * For stages (when the fault propagates to a stage with a catching sentry): children are
 * terminated via exitChildPlanItemInstances, since the stage is ending as a scope —
 * consistent with BPMN where a subprocess is cleaned up when a boundary error event catches an error.
 *
 * @author Joram Barrez
 */
public class FailPlanItemInstanceOperation extends AbstractMovePlanItemInstanceToTerminalStateOperation {

    public FailPlanItemInstanceOperation(CommandContext commandContext, PlanItemInstanceEntity planItemInstanceEntity) {
        super(commandContext, planItemInstanceEntity);
    }

    @Override
    public String getNewState() {
        return PlanItemInstanceState.FAILED;
    }

    @Override
    public String getLifeCycleTransition() {
        return PlanItemTransition.FAULT;
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
        // When a stage faults (because a fault propagated through it and was caught at the parent level),
        // its children must be terminated — the scope is ending, consistent with BPMN subprocess cleanup.
        if (isStage(planItemInstanceEntity)) {
            exitChildPlanItemInstances(PlanItemTransition.FAULT, null, null);
        }

        planItemInstanceEntity.setEndedTime(getCurrentTime(commandContext));
        planItemInstanceEntity.setFailedTime(planItemInstanceEntity.getEndedTime());
        CommandContextUtil.getCmmnHistoryManager(commandContext).recordPlanItemInstanceFailed(planItemInstanceEntity);
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
        return "[Fail plan item]";
    }
}
