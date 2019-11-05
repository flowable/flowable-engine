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

import static org.flowable.cmmn.model.Criterion.EXIT_EVENT_TYPE_COMPLETE;
import static org.flowable.cmmn.model.Criterion.EXIT_EVENT_TYPE_FORCE_COMPLETE;

import org.flowable.cmmn.api.runtime.PlanItemInstanceState;
import org.flowable.cmmn.engine.impl.persistence.entity.PlanItemInstanceEntity;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.cmmn.engine.impl.util.PlanItemInstanceContainerUtil;
import org.flowable.cmmn.model.Criterion;
import org.flowable.cmmn.model.PlanItemTransition;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.impl.interceptor.CommandContext;

/**
 * @author Joram Barrez
 * @author Micha Kiener
 */
public class ExitPlanItemInstanceOperation extends AbstractMovePlanItemInstanceToTerminalStateOperation {

    protected String exitCriterionId;
    protected String exitType;
    protected String exitEventType;
    protected Boolean isStage = null;
    
    public ExitPlanItemInstanceOperation(CommandContext commandContext, PlanItemInstanceEntity planItemInstanceEntity, String exitCriterionId, String exitType, String exitEventType) {
        super(commandContext, planItemInstanceEntity);
        this.exitCriterionId = exitCriterionId;
        this.exitType = exitType;
        this.exitEventType = exitEventType;
    }
    
    @Override
    protected String getNewState() {
        // depending on the exit event type, we want to leave the stage in completed state, not terminated
        if (isStage() && (EXIT_EVENT_TYPE_COMPLETE.equals(exitEventType) || EXIT_EVENT_TYPE_FORCE_COMPLETE.equals(exitEventType))) {
            return PlanItemInstanceState.COMPLETED;
        }
        return PlanItemInstanceState.TERMINATED;
    }
    
    @Override
    protected String getLifeCycleTransition() {
        // depending on the exit event type, we want to use the complete transition, not the exit one, so depending on-parts get triggered waiting for the
        // complete transition
        if (isStage() && (EXIT_EVENT_TYPE_COMPLETE.equals(exitEventType) || EXIT_EVENT_TYPE_FORCE_COMPLETE.equals(exitEventType))) {
            return PlanItemTransition.COMPLETE;
        }
        return PlanItemTransition.EXIT;
    }
    
    @Override
    protected void internalExecute() {
        if (isStage()) {
            if (EXIT_EVENT_TYPE_COMPLETE.equals(exitEventType)) {
                // if the stage should exit with a complete event instead of exit, we need to make sure it is completable
                if (!PlanItemInstanceContainerUtil.shouldPlanItemContainerComplete(commandContext, planItemInstanceEntity, true).isCompletable()) {
                    // we can't complete the stage as it is currently not completable, so we need to throw an exception
                    throw new FlowableIllegalArgumentException("Cannot exit stage with 'complete' event type as the stage '" + planItemInstanceEntity.getId() + "' is not yet completable.");
                }
            }

            // regardless of the exit event type, we need to exit the child plan items as well (we don't propagate the exit event type though, children are
            // always exited, not completed)
            exitChildPlanItemInstances(exitCriterionId);
        }

        planItemInstanceEntity.setExitCriterionId(exitCriterionId);
        planItemInstanceEntity.setEndedTime(getCurrentTime(commandContext));

        if (isStage() && (EXIT_EVENT_TYPE_COMPLETE.equals(exitEventType) || EXIT_EVENT_TYPE_FORCE_COMPLETE.equals(exitEventType))) {
            // if the stage should exit with event type complete or even force-complete, we end the stage differently than with a regular exit
            planItemInstanceEntity.setCompletedTime(planItemInstanceEntity.getEndedTime());
            CommandContextUtil.getCmmnHistoryManager(commandContext).recordPlanItemInstanceCompleted(planItemInstanceEntity);
        } else {
            // regular exit
            planItemInstanceEntity.setExitTime(planItemInstanceEntity.getEndedTime());
            CommandContextUtil.getCmmnHistoryManager(commandContext).recordPlanItemInstanceExit(planItemInstanceEntity);
        }
    }

    @Override
    protected boolean isEvaluateRepetitionRule() {
        return false;
    }

    protected boolean isStage() {
        if (isStage == null) {
            isStage = isStage(planItemInstanceEntity);
        }
        return isStage;
    }
}
