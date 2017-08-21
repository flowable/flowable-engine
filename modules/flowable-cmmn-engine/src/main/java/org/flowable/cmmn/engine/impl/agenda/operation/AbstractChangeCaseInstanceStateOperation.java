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
import org.flowable.cmmn.engine.impl.persistence.entity.MilestoneInstanceEntity;
import org.flowable.cmmn.engine.impl.persistence.entity.MilestoneInstanceEntityManager;
import org.flowable.cmmn.engine.impl.persistence.entity.PlanItemInstanceEntity;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.engine.common.impl.interceptor.CommandContext;

/**
 * @author Joram Barrez
 */
public abstract class AbstractChangeCaseInstanceStateOperation extends AbstractCaseInstanceOperation {

    public AbstractChangeCaseInstanceStateOperation(CommandContext commandContext, String caseInstanceId) {
        super(commandContext, caseInstanceId, null);
    }

    public AbstractChangeCaseInstanceStateOperation(CommandContext commandContext, CaseInstanceEntity caseInstanceEntity) {
        super(commandContext, null, caseInstanceEntity);
    }

    @Override
    public void run() {
        super.run();
        
        String previousState = caseInstanceEntity.getState();
        String newState = getNewState();
        caseInstanceEntity.setState(newState);
        
        deleteCaseInstanceRuntimeData();
        CommandContextUtil.getCaseInstanceHelper(commandContext).callCaseInstanceCallbacks(commandContext, caseInstanceEntity, previousState, newState);
        CommandContextUtil.getCmmnHistoryManager(commandContext).recordCaseInstanceEnd(caseInstanceEntityId);
    }
    
    protected void deleteCaseInstanceRuntimeData() {
        PlanItemInstanceEntity planModelPlanItemInstanceEntity = caseInstanceEntity.getPlanModelInstance();
        
        MilestoneInstanceEntityManager milestoneInstanceEntityManager = CommandContextUtil.getMilestoneInstanceEntityManager(commandContext);
        List<MilestoneInstanceEntity> milestoneInstanceEntities = milestoneInstanceEntityManager
                .findMilestoneInstancesByCaseInstanceId(caseInstanceEntityId);
        if (milestoneInstanceEntities != null) {
            for (MilestoneInstanceEntity milestoneInstanceEntity : milestoneInstanceEntities) {
                milestoneInstanceEntityManager.delete(milestoneInstanceEntity);
            }
        }
        
        CommandContextUtil.getPlanItemInstanceEntityManager(commandContext).deleteCascade(planModelPlanItemInstanceEntity);
        CommandContextUtil.getCaseInstanceEntityManager(commandContext).delete(caseInstanceEntityId);
    }
    
    protected abstract String getNewState();

}
