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

import org.flowable.cmmn.api.runtime.CaseInstanceState;
import org.flowable.cmmn.engine.impl.persistence.entity.PlanItemInstanceEntity;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.impl.interceptor.CommandContext;

/**
 * @author Joram Barrez
 * @author Micha Kiener
 */
public class TerminateCaseInstanceOperation extends AbstractDeleteCaseInstanceOperation {
    
    protected boolean manualTermination;
    protected String exitCriterionId;
    protected String exitType;
    protected String exitEventType;

    public TerminateCaseInstanceOperation(CommandContext commandContext, String caseInstanceId, boolean manualTermination, String exitCriterionId,
        String exitType, String exitEventType) {
        super(commandContext, caseInstanceId);
        this.manualTermination = manualTermination;
        this.exitCriterionId = exitCriterionId;
        this.exitType = exitType;
        this.exitEventType = exitEventType;
    }

    /**
     * Overriden to check, if the optional exit event type is set to 'complete' and if so, throw an exception, if the case is not yet completable.
     */
    @Override
    public void run() {
        if (EXIT_EVENT_TYPE_COMPLETE.equals(exitEventType)) {
            checkCaseToBeCompletable();
        }
        super.run();
    }

    /**
     * Checks, if the case is completable and if not, raises an exception.
     */
    protected void checkCaseToBeCompletable() {
        // if the case should exit with a complete event instead of exit, we need to make sure it is completable
        if (!getCaseInstanceEntity().isCompletable()) {
            // we can't complete the case as it is currently not completable, so we need to throw an exception
            throw new FlowableIllegalArgumentException(
                "Cannot exit case with 'complete' event type as the case '" + getCaseInstanceEntityId() + "' is not yet completable.");
        }
    }

    @Override
    protected String getNewState() {
        // depending on the exit event type, we will end up in the complete state, even though the case was actually terminated / exited
        if (EXIT_EVENT_TYPE_COMPLETE.equals(exitEventType) || EXIT_EVENT_TYPE_FORCE_COMPLETE.equals(exitEventType)) {
            return CaseInstanceState.COMPLETED;
        }
        return CaseInstanceState.TERMINATED;
    }
    
    @Override
    protected void changeStateForChildPlanItemInstance(PlanItemInstanceEntity planItemInstanceEntity) {
        // we don't propagate the exit and exit event type to the children, they will always be terminated / exited the same way, regardless of its case being
        // completed or terminated
        if (manualTermination) {
            CommandContextUtil.getAgenda(commandContext).planTerminatePlanItemInstanceOperation(planItemInstanceEntity, null, null);
        } else {
            CommandContextUtil.getAgenda(commandContext).planExitPlanItemInstanceOperation(planItemInstanceEntity, exitCriterionId, null, null);
        }
    }
    
    @Override
    protected String getDeleteReason() {
        return "cmmn-state-transition-terminate-case";
    }

}
