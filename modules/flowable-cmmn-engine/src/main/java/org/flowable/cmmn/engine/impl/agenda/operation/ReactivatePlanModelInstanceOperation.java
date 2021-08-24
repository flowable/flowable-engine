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

import java.util.List;

import org.flowable.cmmn.engine.impl.persistence.entity.CaseInstanceEntity;
import org.flowable.cmmn.engine.impl.repository.CaseDefinitionUtil;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.cmmn.model.Case;
import org.flowable.cmmn.model.PlanItem;
import org.flowable.common.engine.impl.interceptor.CommandContext;

/**
 * This operation is initializing phase two of a case reactivation by reactivating the root plan item model for the case according the case model which means
 * that only plan items marked as eligible for reactivation will be recreated and evaluated for activation.
 *
 * @author Micha Kiener
 */
public class ReactivatePlanModelInstanceOperation extends AbstractCaseInstanceOperation {

    protected List<PlanItem> directlyReactivatedPlanItems;

    public ReactivatePlanModelInstanceOperation(CommandContext commandContext, CaseInstanceEntity caseInstanceEntity, List<PlanItem> directlyReactivatedPlanItems) {
        super(commandContext, null, caseInstanceEntity);
        this.directlyReactivatedPlanItems = directlyReactivatedPlanItems;
    }

    @Override
    public void run() {
        super.run();

        Case caseModel = CaseDefinitionUtil.getCase(caseInstanceEntity.getCaseDefinitionId());
        createPlanItemInstancesForNewOrReactivatedStage(commandContext, caseModel,
                caseModel.getPlanModel().getPlanItems(), directlyReactivatedPlanItems,
                caseInstanceEntity,
                null,
                caseInstanceEntity.getTenantId());

        CommandContextUtil.getAgenda(commandContext).planEvaluateCriteriaOperation(caseInstanceEntity.getId());
    }

    @Override
    public String toString() {
        return "[Reactivate Plan Model] reactivating plan model for case instance " + caseInstanceEntity.getId();
    }

}
