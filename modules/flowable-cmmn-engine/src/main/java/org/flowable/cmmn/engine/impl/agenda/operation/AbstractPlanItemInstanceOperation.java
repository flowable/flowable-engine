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

import org.flowable.cmmn.engine.impl.persistence.entity.PlanItemInstanceEntity;
import org.flowable.cmmn.engine.impl.persistence.entity.SentryPartInstanceEntity;
import org.flowable.cmmn.engine.impl.persistence.entity.SentryPartInstanceEntityManager;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.cmmn.model.PlanItem;
import org.flowable.common.engine.impl.interceptor.CommandContext;

/**
 * @author Joram Barrez
 */
public abstract class AbstractPlanItemInstanceOperation extends CmmnOperation {
    
    protected PlanItemInstanceEntity planItemInstanceEntity;

    public AbstractPlanItemInstanceOperation(CommandContext commandContext, PlanItemInstanceEntity planItemInstanceEntity) {
        super(commandContext);
        this.planItemInstanceEntity = planItemInstanceEntity;
    }
    
    public PlanItemInstanceEntity getPlanItemInstanceEntity() {
        return planItemInstanceEntity;
    }

    public void setPlanItemInstanceEntity(PlanItemInstanceEntity planItemInstanceEntity) {
        this.planItemInstanceEntity = planItemInstanceEntity;
    }
    
    protected void deleteSentryPartInstances() {
        SentryPartInstanceEntityManager sentryPartInstanceEntityManager = CommandContextUtil.getSentryPartInstanceEntityManager(commandContext);
        if (planItemInstanceEntity.getPlanItem() != null 
                && (!planItemInstanceEntity.getPlanItem().getEntryCriteria().isEmpty()
                        || !planItemInstanceEntity.getPlanItem().getExitCriteria().isEmpty())) {
            if (planItemInstanceEntity.getSatisfiedSentryPartInstances() != null) {
                for (SentryPartInstanceEntity sentryPartInstanceEntity : planItemInstanceEntity.getSatisfiedSentryPartInstances()) {
                    sentryPartInstanceEntityManager.delete(sentryPartInstanceEntity);
                }
            }
        }
    }
    
    protected boolean isPlanItemRepeatableOnComplete(PlanItem planItem) {
        return  planItem != null
                && planItem.getEntryCriteria().isEmpty()
                && planItem.getItemControl() != null
                && planItem.getItemControl().getRepetitionRule() != null;
    }
    
}
