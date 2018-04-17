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
import org.flowable.cmmn.engine.impl.persistence.entity.CaseInstanceEntity;
import org.flowable.cmmn.engine.impl.persistence.entity.PlanItemInstanceEntity;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.common.engine.impl.interceptor.CommandContext;

/**
 * @author Joram Barrez
 */
public class CompleteCaseInstanceOperation extends AbstractDeleteCaseInstanceOperation {

    public CompleteCaseInstanceOperation(CommandContext commandContext, String caseInstanceId) {
        super(commandContext, caseInstanceId);
    }

    public CompleteCaseInstanceOperation(CommandContext commandContext, CaseInstanceEntity caseInstanceEntity) {
        super(commandContext, caseInstanceEntity);
    }

    @Override
    protected String getNewState() {
        return CaseInstanceState.COMPLETED;
    }
    
    @Override
    protected void changeStateForChildPlanItemInstance(PlanItemInstanceEntity planItemInstanceEntity) {
        CommandContextUtil.getAgenda(commandContext).planCompletePlanItemInstanceOperation(planItemInstanceEntity);
    }
    
    @Override
    protected String getDeleteReason() {
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
