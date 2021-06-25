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

import org.flowable.cmmn.api.runtime.CaseInstanceState;
import org.flowable.cmmn.engine.impl.behavior.OnParentEndDependantActivityBehavior;
import org.flowable.cmmn.engine.impl.persistence.entity.CaseInstanceEntity;
import org.flowable.cmmn.engine.impl.persistence.entity.PlanItemInstanceEntity;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.cmmn.model.PlanItemTransition;
import org.flowable.common.engine.impl.interceptor.CommandContext;

/**
 * @author Joram Barrez
 * @author Micha Kiener
 */
public class CompleteCaseInstanceOperation extends AbstractDeleteCaseInstanceOperation {

    public CompleteCaseInstanceOperation(CommandContext commandContext, String caseInstanceId) {
        super(commandContext, caseInstanceId);
    }

    public CompleteCaseInstanceOperation(CommandContext commandContext, CaseInstanceEntity caseInstanceEntity) {
        super(commandContext, caseInstanceEntity);
    }

    @Override
    public String getNewState() {
        return CaseInstanceState.COMPLETED;
    }
    
    @Override
    public void changeStateForChildPlanItemInstance(PlanItemInstanceEntity planItemInstanceEntity) {
        // terminate all child plan items not yet in an end state of the case itself (same way as with a stage for instance)
        // if they would be completed, the history will contain completed plan item instances although they never "truly" completed
        // specially important for cases supporting reactivation

        // if the plan item implements the specific behavior interface for ending, invoke it, otherwise use the default one which is terminate, regardless,
        // if the case got completed or terminated
        Object behavior = planItemInstanceEntity.getPlanItem().getBehavior();
        if (behavior instanceof OnParentEndDependantActivityBehavior) {
            // if the specific behavior is implemented, invoke it
            ((OnParentEndDependantActivityBehavior) behavior).onParentEnd(commandContext, planItemInstanceEntity, PlanItemTransition.COMPLETE, null);
        } else {
            // use default behavior, if the interface is not implemented
            CommandContextUtil.getAgenda(commandContext).planTerminatePlanItemInstanceOperation(planItemInstanceEntity, null, null);
        }
    }
    
    @Override
    public String getDeleteReason() {
        return "cmmn-state-transition-complete-case";
    }

    @Override
    public String toString() {
        StringBuilder strb = new StringBuilder();
        strb.append("[Complete case instance] case instance ");
        strb.append(caseInstanceEntity != null ? caseInstanceEntity.getId() : caseInstanceEntityId);
        return strb.toString();
    }

}
