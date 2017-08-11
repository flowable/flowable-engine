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

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.flowable.cmmn.engine.impl.criteria.PlanItemLifeCycleEvent;
import org.flowable.cmmn.engine.impl.persistence.entity.CaseInstanceEntity;
import org.flowable.cmmn.engine.impl.persistence.entity.MilestoneInstanceEntityImpl;
import org.flowable.cmmn.engine.impl.persistence.entity.MilestoneInstanceEntityManager;
import org.flowable.cmmn.engine.impl.persistence.entity.PlanItemInstanceEntity;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.cmmn.engine.runtime.CaseInstanceState;
import org.flowable.cmmn.engine.runtime.MilestoneInstance;
import org.flowable.cmmn.model.PlanItem;
import org.flowable.cmmn.model.Stage;
import org.flowable.engine.common.impl.interceptor.CommandContext;
import org.flowable.engine.common.impl.persistence.cache.CachedEntity;

/**
 * @author Joram Barrez
 */
public abstract class AbstractChangePlanItemStateOperation extends AbstractPlanItemInstanceOperation {
    
    public AbstractChangePlanItemStateOperation(CommandContext commandContext, PlanItemInstanceEntity planItemInstanceEntity) {
        super(commandContext, planItemInstanceEntity);
    }
    
    @Override
    public void run() {
        planItemInstanceEntity.setState(getNewState());
        if (isPlanModel(planItemInstanceEntity)) {
            changeCaseInstanceState();
        }
        CommandContextUtil.getAgenda(commandContext).planEvaluateCriteria(planItemInstanceEntity.getCaseInstanceId(), createPlanItemLifeCycleEvent());
    }
    
    protected boolean isPlanModel(PlanItemInstanceEntity stagePlanItemInstanceEntity) {
        Stage stage = getStage(stagePlanItemInstanceEntity.getCaseDefinitionId(), stagePlanItemInstanceEntity.getElementId());
        if (stage != null) {
            return stage.isPlanModel();
        }
        return false;
    }
    
    protected PlanItemLifeCycleEvent createPlanItemLifeCycleEvent() {
        return new PlanItemLifeCycleEvent(planItemInstanceEntity.getPlanItem(), getLifeCycleEventType());
    }
    
    protected void changeCaseInstanceState() {
        CaseInstanceEntity caseInstanceEntity = CommandContextUtil.getCaseInstanceEntityManager(commandContext)
                .findById(planItemInstanceEntity.getCaseInstanceId());
        if (CaseInstanceState.ACTIVE.equals(caseInstanceEntity.getState())) {
            caseInstanceEntity.setState(getNewState());
            
            if (CaseInstanceState.COMPLETED.equals(caseInstanceEntity.getState())) {
                
                // TODO: replace with CachedEntityMatcher once changes from Tijs on master are merged in
                
                Set<String> milestoneIds = new HashSet<>();

                // DB
                MilestoneInstanceEntityManager milestoneInstanceEntityManager = CommandContextUtil.getMilestoneInstanceEntityManager(commandContext);
                List<MilestoneInstance> milestones = milestoneInstanceEntityManager.createMilestoneInstanceQuery()
                        .milestoneInstanceCaseInstanceId(caseInstanceEntity.getId()).list();
                for (MilestoneInstance milestoneInstance : milestones) {
                    milestoneInstanceEntityManager.delete(milestoneInstance.getId());
                }
                
                // Cache
                Map<String, CachedEntity> cachedMilestoneInstanceEntities = CommandContextUtil.getEntityCache()
                        .getAllCachedEntities().get(MilestoneInstanceEntityImpl.class);
                for (CachedEntity cachedEntity : cachedMilestoneInstanceEntities.values()) {
                    MilestoneInstanceEntityImpl milestoneInstanceEntity = (MilestoneInstanceEntityImpl) cachedEntity.getEntity();
                    if (milestoneInstanceEntity.getCaseInstanceId().equals(caseInstanceEntity.getId())) {
                        milestoneIds.add(milestoneInstanceEntity.getId());
                    }
                }
                
                for (String milestoneId : milestoneIds) {
                    milestoneInstanceEntityManager.delete(milestoneId);
                }
                
                PlanItemInstanceEntity planModelPlanItemInstanceEntity = caseInstanceEntity.getPlanModelInstance();
                CommandContextUtil.getPlanItemInstanceEntityManager(commandContext).deleteCascade(planModelPlanItemInstanceEntity);
                CommandContextUtil.getCaseInstanceEntityManager(commandContext).delete(planModelPlanItemInstanceEntity.getCaseInstanceId());
            }
        }
    }
    
    protected abstract String getNewState();
    
    protected abstract String getLifeCycleEventType(); 
    
    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        PlanItem planItem = planItemInstanceEntity.getPlanItem();
        stringBuilder.append("[Change Planitem state] ");
        if (planItem != null) {
            if (planItem.getName() != null) {
            stringBuilder.append(planItem.getName());
            stringBuilder.append("(");
            stringBuilder.append(planItem.getId());
            stringBuilder.append(")");
            } else {
                stringBuilder.append(planItem.getId());
            }
        } else {
            stringBuilder.append("(plan item instance with id " + planItemInstanceEntity.getId() + ")");
        }
        stringBuilder.append(": ");
        stringBuilder.append("new state: [" + getNewState() + "]");
        return stringBuilder.toString();
    }
    
}
