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

import java.util.Objects;

import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.impl.behavior.PlanItemActivityBehavior;
import org.flowable.cmmn.engine.impl.criteria.PlanItemLifeCycleEvent;
import org.flowable.cmmn.engine.impl.persistence.entity.PlanItemInstanceEntity;
import org.flowable.cmmn.engine.impl.runtime.StateTransition;
import org.flowable.cmmn.engine.impl.util.CmmnLoggingSessionUtil;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.cmmn.model.PlanItem;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.logging.CmmnLoggingSessionConstants;

/**
 * @author Joram Barrez
 */
public abstract class AbstractChangePlanItemInstanceStateOperation extends AbstractPlanItemInstanceOperation {

    public AbstractChangePlanItemInstanceStateOperation(CommandContext commandContext, PlanItemInstanceEntity planItemInstanceEntity) {
        super(commandContext, planItemInstanceEntity);
    }

    @Override
    public void run() {
        String oldState = planItemInstanceEntity.getState();
        String newState = getNewState();

        if (isStateNotChanged(oldState, newState)) {
            markAsNoop();
            return;
        }

        if (planItemInstanceEntity.getPlanItem() != null) { // can be null for the plan model
            Object behavior = planItemInstanceEntity.getPlanItem().getBehavior();
            if (behavior instanceof PlanItemActivityBehavior
                    && StateTransition.isPossible(planItemInstanceEntity, getLifeCycleTransition())) {
                ((PlanItemActivityBehavior) behavior).onStateTransition(commandContext, planItemInstanceEntity, getLifeCycleTransition());
            }
        }

        planItemInstanceEntity.setState(newState);
        CmmnEngineConfiguration cmmnEngineConfiguration =CommandContextUtil.getCmmnEngineConfiguration(commandContext);
        cmmnEngineConfiguration.getListenerNotificationHelper().executeLifecycleListeners(
                commandContext, planItemInstanceEntity, oldState, getNewState());

        CommandContextUtil.getAgenda(commandContext).planEvaluateCriteriaOperation(planItemInstanceEntity.getCaseInstanceId(), createPlanItemLifeCycleEvent());
        internalExecute();
        
        if (CommandContextUtil.getCmmnEngineConfiguration(commandContext).isLoggingSessionEnabled()) {
            String loggingType = null;
            String message = null;
            if (oldState == null) {
                loggingType = CmmnLoggingSessionConstants.TYPE_PLAN_ITEM_CREATED;
                message = "Plan item instance created with type " + planItemInstanceEntity.getPlanItemDefinitionType() + 
                                ", new state " + planItemInstanceEntity.getState();
                
            } else {
                loggingType = CmmnLoggingSessionConstants.TYPE_PLAN_ITEM_NEW_STATE;
                message = "Plan item instance state change with type " + planItemInstanceEntity.getPlanItemDefinitionType() + 
                                ", old state " + oldState + ", new state " + newState;
            }
            
            CmmnLoggingSessionUtil.addLoggingData(loggingType, message, oldState, newState, planItemInstanceEntity, cmmnEngineConfiguration.getObjectMapper());
        }
    }

    public boolean isStateNotChanged(String oldState, String newState) {
        // if the old and new state are the same, leave the operation as we don't execute any transition
        return oldState != null && oldState.equals(newState) && abortOperationIfNewStateEqualsOldState();
    }

    protected abstract void internalExecute();

    protected PlanItemLifeCycleEvent createPlanItemLifeCycleEvent() {
        return new PlanItemLifeCycleEvent(planItemInstanceEntity.getPlanItem(), getLifeCycleTransition());
    }

    public abstract String getNewState();

    public abstract String getLifeCycleTransition();

    /**
     * Overwrite this default implemented hook, if the operation should be aborted on a void transition which might be the case, if the old and new state
     * will be the same.
     *
     * @return true, if this operation should be aborted, if the new plan item state is the same as the old one, false, if the operation is to be executed in any case
     */
    public boolean abortOperationIfNewStateEqualsOldState() {
        return false;
    }

    public abstract String getOperationName();

    @Override
    public String toString() {
        PlanItem planItem = planItemInstanceEntity.getPlanItem();

        StringBuilder stringBuilder = new StringBuilder();

        String operationName = getOperationName();
        stringBuilder.append(operationName != null ? operationName : "[Change plan item state]").append(" ");

        if (planItem != null) {
            stringBuilder.append(planItem);
        } else {
            stringBuilder.append(planItemInstanceEntity);
        }

        stringBuilder.append(", ");

        String currentState = planItemInstanceEntity.getState();
        String newState = getNewState();

        if (!Objects.equals(currentState, newState)) {

            stringBuilder.append("new state: [").append(getNewState()).append("]");
            stringBuilder.append(" with transition [");
            stringBuilder.append(getLifeCycleTransition());
            stringBuilder.append("]");

        } else {
            stringBuilder.append("will remain in state [").append(newState).append("]");

        }


        return stringBuilder.toString();
    }

}
