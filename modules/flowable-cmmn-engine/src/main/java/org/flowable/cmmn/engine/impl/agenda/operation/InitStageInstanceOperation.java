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
import org.flowable.cmmn.engine.impl.persistence.entity.CaseInstanceEntity;
import org.flowable.cmmn.engine.impl.persistence.entity.PlanItemInstanceEntity;
import org.flowable.cmmn.engine.impl.repository.CaseDefinitionUtil;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.cmmn.model.Case;
import org.flowable.cmmn.model.Stage;
import org.flowable.common.engine.impl.interceptor.CommandContext;

/**
 * @author Joram Barrez
 */
public class InitStageInstanceOperation extends AbstractPlanItemInstanceOperation {
    
    public InitStageInstanceOperation(CommandContext commandContext, PlanItemInstanceEntity planItemInstanceEntity) {
        super(commandContext, planItemInstanceEntity);
    }
    
    @Override
    public void run() {
        Stage stage = getStage(planItemInstanceEntity);

        String oldState = planItemInstanceEntity.getState();
        String newState = PlanItemInstanceState.ACTIVE;
        planItemInstanceEntity.setState(newState);
        CommandContextUtil.getCmmnEngineConfiguration(commandContext).getListenerNotificationHelper()
            .executeLifecycleListeners(commandContext, planItemInstanceEntity, oldState, newState);

        planItemInstanceEntity.setStage(true);

        Case caseModel = CaseDefinitionUtil.getCase(planItemInstanceEntity.getCaseDefinitionId());
        CaseInstanceEntity caseInstanceEntity = CommandContextUtil.getCmmnEngineConfiguration(commandContext).getCaseInstanceEntityManager()
            .findById(planItemInstanceEntity.getCaseInstanceId());

        createPlanItemInstancesForNewOrReactivatedStage(commandContext, caseModel,
                stage.getPlanItems(), null,
                caseInstanceEntity,
                planItemInstanceEntity,
                planItemInstanceEntity.getTenantId());

        planItemInstanceEntity.setLastStartedTime(getCurrentTime(commandContext));
        CommandContextUtil.getCmmnHistoryManager(commandContext).recordPlanItemInstanceStarted(planItemInstanceEntity);
    }

    @Override
    public String toString() {
        return "[Init Stage] Using plan item " + planItemInstanceEntity.getName() + " (" + planItemInstanceEntity.getId() + ")";
    }

}
