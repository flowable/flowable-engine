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

import java.util.Date;

import org.flowable.cmmn.engine.impl.persistence.entity.PlanItemInstanceEntity;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
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

    @Override
    public String getCaseInstanceId() {
        return planItemInstanceEntity.getCaseInstanceId();
    }

    protected void removeSentryRelatedData() {
        CommandContextUtil.getPlanItemInstanceEntityManager(commandContext).deleteSentryRelatedData(planItemInstanceEntity.getId());
    }

    protected Date getCurrentTime(CommandContext commandContext) {
        return CommandContextUtil.getCmmnEngineConfiguration(commandContext).getClock().getCurrentTime();
    }
}
