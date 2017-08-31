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
package org.flowable.cmmn.engine.impl.behavior.impl;

import org.flowable.cmmn.engine.impl.behavior.CmmnActivityBehavior;
import org.flowable.cmmn.engine.impl.persistence.entity.MilestoneInstanceEntity;
import org.flowable.cmmn.engine.impl.persistence.entity.MilestoneInstanceEntityManager;
import org.flowable.cmmn.engine.impl.persistence.entity.PlanItemInstanceEntity;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.cmmn.engine.runtime.DelegatePlanItemInstance;
import org.flowable.engine.common.impl.interceptor.CommandContext;

/**
 * @author Joram Barrez
 */
public class MilestoneActivityBehavior implements CmmnActivityBehavior {
    
    // TODO: should be expression
    protected String milestoneName;
    
    public MilestoneActivityBehavior(String milestoneName) {
        this.milestoneName = milestoneName;
    }

    @Override
    public void execute(DelegatePlanItemInstance delegatePlanItemInstance) {
        CommandContext commandContext = CommandContextUtil.getCommandContext();
        MilestoneInstanceEntity milestoneInstanceEntity = createMilestoneInstance(delegatePlanItemInstance, commandContext);
        CommandContextUtil.getCmmnHistoryManager(commandContext).recordMilestoneReached(milestoneInstanceEntity);
        CommandContextUtil.getAgenda(commandContext).planOccurPlanItem((PlanItemInstanceEntity) delegatePlanItemInstance);
    }

    protected MilestoneInstanceEntity createMilestoneInstance(DelegatePlanItemInstance delegatePlanItemInstance, CommandContext commandContext) {
        MilestoneInstanceEntityManager milestoneInstanceEntityManager = CommandContextUtil.getMilestoneInstanceEntityManager(commandContext);
        MilestoneInstanceEntity milestoneInstanceEntity = milestoneInstanceEntityManager.create();
        milestoneInstanceEntity.setName(milestoneName);
        milestoneInstanceEntity.setTimeStamp(CommandContextUtil.getCmmnEngineConfiguration(commandContext).getClock().getCurrentTime());
        milestoneInstanceEntity.setCaseInstanceId(delegatePlanItemInstance.getCaseInstanceId());
        milestoneInstanceEntity.setCaseDefinitionId(delegatePlanItemInstance.getCaseDefinitionId());
        milestoneInstanceEntity.setElementId(delegatePlanItemInstance.getElementId());
        milestoneInstanceEntityManager.insert(milestoneInstanceEntity);
        return milestoneInstanceEntity;
    }
    
}
