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
import org.flowable.cmmn.engine.impl.persistence.entity.PlanItemInstanceEntity;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.common.engine.impl.interceptor.CommandContext;

/**
 * @author Joram Barrez
 */
public class TerminateCaseInstanceOperation extends AbstractDeleteCaseInstanceOperation {
    
    protected boolean manualTermination;

    public TerminateCaseInstanceOperation(CommandContext commandContext, String caseInstanceId, boolean manualTermination) {
        super(commandContext, caseInstanceId);
        this.manualTermination = manualTermination;
    }

    @Override
    protected String getNewState() {
        return CaseInstanceState.TERMINATED;
    }
    
    @Override
    protected void changeStateForChildPlanItemInstance(PlanItemInstanceEntity planItemInstanceEntity) {
        if (manualTermination) {
            CommandContextUtil.getAgenda(commandContext).planTerminatePlanItemInstanceOperation(planItemInstanceEntity);
        } else {
            CommandContextUtil.getAgenda(commandContext).planExitPlanItemInstanceOperation(planItemInstanceEntity);
        }
    }
    
    @Override
    protected String getDeleteReason() {
        return "cmmn-state-transition-terminate-case";
    }

}
