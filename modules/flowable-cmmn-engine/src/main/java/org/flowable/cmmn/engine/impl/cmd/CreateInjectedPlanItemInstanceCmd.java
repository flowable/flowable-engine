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
package org.flowable.cmmn.engine.impl.cmd;

import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.flowable.cmmn.engine.impl.persistence.entity.PlanItemInstanceEntity;
import org.flowable.cmmn.engine.impl.repository.CaseDefinitionUtil;
import org.flowable.cmmn.engine.impl.runtime.InjectedPlanItemInstanceBuilderImpl;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.cmmn.model.Case;
import org.flowable.cmmn.model.CaseElement;
import org.flowable.cmmn.model.PlanItem;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;

/**
 * Creates a new dynamically injected plan item instance and adds it to the agenda for activation.
 *
 * @author Micha Kiener
 */
public class CreateInjectedPlanItemInstanceCmd implements Command<PlanItemInstance> {

    protected final InjectedPlanItemInstanceBuilderImpl planItemInstanceBuilder;

    public CreateInjectedPlanItemInstanceCmd(InjectedPlanItemInstanceBuilderImpl planItemInstanceBuilder) {
        this.planItemInstanceBuilder = planItemInstanceBuilder;
    }

    @Override
    public PlanItemInstance execute(CommandContext commandContext) {

        Case caseModel = CaseDefinitionUtil.getCase(planItemInstanceBuilder.getCaseDefinitionId());
        if (caseModel == null) {
            throw new FlowableIllegalArgumentException("Could not find case model with case definition id " + planItemInstanceBuilder.getCaseDefinitionId());
        }

        CaseElement caseElement = caseModel.getAllCaseElements().get(planItemInstanceBuilder.getElementId());
        if (caseElement == null) {
            throw new FlowableIllegalArgumentException("Could not find case element with id " + planItemInstanceBuilder.getElementId());
        }
        if (!(caseElement instanceof PlanItem)) {
            throw new FlowableIllegalArgumentException("The case element needs to be a plan item, but is a " + caseElement.getClass().getName());
        }

        PlanItemInstanceEntity planItemInstanceEntity = CommandContextUtil.getPlanItemInstanceEntityManager(commandContext)
            .createPlanItemInstanceEntityBuilder()
            .caseDefinitionId(planItemInstanceBuilder.getCaseDefinitionId())
            .planItem((PlanItem) caseElement)
            .name(planItemInstanceBuilder.getName())
            .caseInstanceId(planItemInstanceBuilder.getParentPlanItemInstance().getCaseInstanceId())
            .stagePlanItemInstanceId(planItemInstanceBuilder.getParentPlanItemInstance().getId())
            .tenantId(planItemInstanceBuilder.getParentPlanItemInstance().getTenantId())
            .addToParent(true)
            .create();

        // after adding the plan item to the stage, add it to the agenda for activation processing
        CommandContextUtil.getAgenda(commandContext).planEvaluateToActivatePlanItemInstanceOperation(planItemInstanceEntity);

        return planItemInstanceEntity;
    }
}
