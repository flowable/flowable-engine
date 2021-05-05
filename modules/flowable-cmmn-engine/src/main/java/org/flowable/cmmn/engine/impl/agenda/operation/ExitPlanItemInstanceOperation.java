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

import static org.flowable.cmmn.api.runtime.PlanItemInstanceState.ENABLED;
import static org.flowable.cmmn.api.runtime.PlanItemInstanceState.EVALUATE_STATES;
import static org.flowable.cmmn.api.runtime.PlanItemInstanceState.TERMINATED;
import static org.flowable.cmmn.model.Criterion.EXIT_EVENT_TYPE_COMPLETE;
import static org.flowable.cmmn.model.Criterion.EXIT_EVENT_TYPE_FORCE_COMPLETE;
import static org.flowable.cmmn.model.Criterion.EXIT_TYPE_ACTIVE_AND_ENABLED_INSTANCES;
import static org.flowable.cmmn.model.Criterion.EXIT_TYPE_ACTIVE_INSTANCES;

import org.flowable.cmmn.api.runtime.PlanItemInstanceState;
import org.flowable.cmmn.engine.impl.persistence.entity.PlanItemInstanceEntity;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.cmmn.engine.impl.util.CompletionEvaluationResult;
import org.flowable.cmmn.engine.impl.util.PlanItemInstanceContainerUtil;
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
    public String getNewState() {
        // depending on the exit event type, we want to leave the stage in completed state, not terminated
        if (shouldStageGoIntoCompletedState()) {
            return PlanItemInstanceState.COMPLETED;
        }

        // check the exit type for a regular plan item, not a stage to be something else than default
        if (shouldPlanItemStayInCurrentState()) {
            // if there is an exit type set to only terminate active instances and this one is only available or enabled, don't change its state
            return planItemInstanceEntity.getState();
        }

        return PlanItemInstanceState.TERMINATED;
    }

    @Override
    public boolean abortOperationIfNewStateEqualsOldState() {
        // on an exit operation, we abort the operation, if we don't go into terminated state, but remain in the current state
        return true;
    }

    /**
     * @return true, if this plan item is a stage and according the exit sentry exit event type needs to go in complete state instead of terminated
     */
    public boolean shouldStageGoIntoCompletedState() {
        return isStage() && (EXIT_EVENT_TYPE_COMPLETE.equals(exitEventType) || EXIT_EVENT_TYPE_FORCE_COMPLETE.equals(exitEventType));
    }

    public boolean shouldPlanItemStayInCurrentState() {
        return !isStage() && (
            (EXIT_TYPE_ACTIVE_INSTANCES.equals(exitType) &&
                (ENABLED.equals(planItemInstanceEntity.getState()) || EVALUATE_STATES.contains(planItemInstanceEntity.getState())))
                ||
            (EXIT_TYPE_ACTIVE_AND_ENABLED_INSTANCES.equals(exitType) &&
                EVALUATE_STATES.contains(planItemInstanceEntity.getState()))
        );
    }
    
    @Override
    public String getLifeCycleTransition() {
        // depending on the exit event type, we want to use the complete transition, not the exit one, so depending on-parts get triggered waiting for the
        // complete transition
        if (shouldStageGoIntoCompletedState()) {
            return PlanItemTransition.COMPLETE;
        }
        return PlanItemTransition.EXIT;
    }
    
    @Override
    protected void internalExecute() {
        if (isStage()) {
            if (EXIT_EVENT_TYPE_COMPLETE.equals(exitEventType)) {
                // if the stage should exit with a complete event instead of exit, we need to make sure it is completable
                // we don't use the completion flag directly on the entity as it gets evaluated only at the end of an evaluation cycle which we didn't hit yet
                // at this point, so we need a proper evaluation of the completion
                CompletionEvaluationResult completionEvaluationResult = PlanItemInstanceContainerUtil
                    .shouldPlanItemContainerComplete(commandContext, planItemInstanceEntity, true);

                if (!completionEvaluationResult.isCompletable()) {
                    // we can't complete the stage as it is currently not completable, so we need to throw an exception
                    throw new FlowableIllegalArgumentException(
                        "Cannot exit stage with 'complete' event type as the stage '" + planItemInstanceEntity.getId() + "' is not yet completable. The plan item '" +
                            completionEvaluationResult.getPlanItemInstance().getName() + " (" +
                            completionEvaluationResult.getPlanItemInstance().getPlanItemDefinitionId() + ")' prevented it from completion.");
                }
            }

            // regardless of the exit event type, we need to exit the child plan items as well (we don't propagate the exit event type though, children are
            // always exited, not completed)
            exitChildPlanItemInstances(PlanItemTransition.EXIT, exitCriterionId, exitEventType);
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
    public boolean isEvaluateRepetitionRule() {
        // by default, we don't create new instances for repeatable plan items being terminated, however, if the exit type is set to only terminate active or
        // enabled instances, we might want to immediately create a new instance for repetition, but only, if the current one was terminated, of course
        return (EXIT_TYPE_ACTIVE_INSTANCES.equals(exitType) || EXIT_TYPE_ACTIVE_AND_ENABLED_INSTANCES.equals(exitType)) && TERMINATED.equals(getNewState());
    }

    @Override
    protected boolean shouldAggregateForSingleInstance() {
        return false;
    }

    @Override
    protected boolean shouldAggregateForMultipleInstances() {
        return false;
    }

    public boolean isStage() {
        if (isStage == null) {
            isStage = isStage(planItemInstanceEntity);
        }
        return isStage;
    }

    @Override
    public String getOperationName() {
        return "[Exit plan item]";
    }

    public String getExitCriterionId() {
        return exitCriterionId;
    }
    public void setExitCriterionId(String exitCriterionId) {
        this.exitCriterionId = exitCriterionId;
    }
    public String getExitType() {
        return exitType;
    }
    public void setExitType(String exitType) {
        this.exitType = exitType;
    }
    public String getExitEventType() {
        return exitEventType;
    }
    public void setExitEventType(String exitEventType) {
        this.exitEventType = exitEventType;
    }
    public Boolean getStage() {
        return isStage;
    }
    public void setStage(Boolean stage) {
        isStage = stage;
    }
}
