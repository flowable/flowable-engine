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

package org.flowable.cmmn.engine.impl.runtime;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.flowable.cmmn.engine.impl.persistence.entity.PlanItemInstanceEntity;
import org.flowable.cmmn.engine.impl.persistence.entity.PlanItemInstanceEntityManager;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.cmmn.model.PlanItemDefinition;
import org.flowable.common.engine.impl.interceptor.CommandContext;

/**
 * @author Tijs Rademakers
 */
public class DefaultCmmnDynamicStateManager extends AbstractCmmnDynamicStateManager implements CmmnDynamicStateManager {

    @Override
    public void movePlanItemInstanceState(ChangePlanItemStateBuilderImpl changePlanItemStateBuilder, CommandContext commandContext) {
        List<MovePlanItemInstanceEntityContainer> movePlanItemInstanceEntityContainerList = resolveMovePlanItemInstanceEntityContainers(changePlanItemStateBuilder, null, changePlanItemStateBuilder.getCaseVariables(), commandContext);
        List<PlanItemInstanceEntity> planItemInstances = movePlanItemInstanceEntityContainerList.iterator().next().getPlanItemInstances();
        String caseInstanceId = planItemInstances.iterator().next().getCaseInstanceId();
        
        CaseInstanceChangeState caseInstanceChangeState = new CaseInstanceChangeState()
            .setCaseInstanceId(caseInstanceId)
            .setMovePlanItemInstanceEntityContainers(movePlanItemInstanceEntityContainerList)
            .setCaseVariables(changePlanItemStateBuilder.getCaseVariables());
        
        doMovePlanItemState(caseInstanceChangeState, commandContext);
    }

    @Override
    protected Map<String, List<PlanItemInstance>> resolveStagePlanItemInstances(String caseInstanceId, CommandContext commandContext) {
        PlanItemInstanceEntityManager planItemInstanceEntityManager = CommandContextUtil.getPlanItemInstanceEntityManager(commandContext);
        PlanItemInstanceQueryImpl planItemInstanceQuery = new PlanItemInstanceQueryImpl(commandContext);
        planItemInstanceQuery.caseInstanceId(caseInstanceId).onlyStages();
        List<PlanItemInstance> planItemInstances = planItemInstanceEntityManager.findByCriteria(planItemInstanceQuery);

        Map<String, List<PlanItemInstance>> stagesByPlanItemDefinitionId = planItemInstances.stream()
            .collect(Collectors.groupingBy(PlanItemInstance::getPlanItemDefinitionId));
        return stagesByPlanItemDefinitionId;
    }

    @Override
    protected boolean isDirectPlanItemDefinitionMigration(PlanItemDefinition currentPlanItemDefinition, PlanItemDefinition newPlanItemDefinition) {
        return false;
    }

}
