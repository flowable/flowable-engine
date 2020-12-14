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
import org.flowable.cmmn.engine.impl.persistence.entity.PlanItemInstanceEntity;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.cmmn.model.PlanItemTransition;
import org.flowable.common.engine.impl.interceptor.CommandContext;

/**
 * @author Joram Barrez
 */
public class TerminatePlanItemInstanceOperation extends AbstractMovePlanItemInstanceToTerminalStateOperation {

    protected String exitType;
    protected String exitEventType;

    public TerminatePlanItemInstanceOperation(CommandContext commandContext, PlanItemInstanceEntity planItemInstanceEntity, String exitType, String exitEventType) {
        super(commandContext, planItemInstanceEntity);
        this.exitType = exitType;
        this.exitEventType = exitEventType;
    }

    @Override
    public String getNewState() {
        return PlanItemInstanceState.TERMINATED;
    }

    @Override
    public String getLifeCycleTransition() {
        return PlanItemTransition.TERMINATE;
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
        planItemInstanceEntity.setEndedTime(getCurrentTime(commandContext));
        planItemInstanceEntity.setTerminatedTime(planItemInstanceEntity.getEndedTime());
        CommandContextUtil.getCmmnHistoryManager(commandContext).recordPlanItemInstanceTerminated(planItemInstanceEntity);
    }

    @Override
    public String getOperationName() {
        return "[Terminate plan item]";
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
}
