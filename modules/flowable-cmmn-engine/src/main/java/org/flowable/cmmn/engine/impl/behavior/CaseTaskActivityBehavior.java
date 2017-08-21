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
package org.flowable.cmmn.engine.impl.behavior;

import org.flowable.cmmn.engine.CaseInstanceCallbackType;
import org.flowable.cmmn.engine.impl.persistence.entity.CaseInstanceEntity;
import org.flowable.cmmn.engine.impl.persistence.entity.PlanItemInstanceEntity;
import org.flowable.cmmn.engine.impl.runtime.CaseInstanceHelper;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.cmmn.engine.runtime.DelegatePlanItemInstance;
import org.flowable.engine.common.impl.interceptor.CommandContext;

/**
 * @author Joram Barrez
 */
public class CaseTaskActivityBehavior extends TaskActivityBehavior {
    
    protected String caseRef;
    
    public CaseTaskActivityBehavior(String caseRef, boolean isBlocking) {
        super(isBlocking);
        this.caseRef = caseRef;
    }

    @Override
    public void execute(DelegatePlanItemInstance planItemInstance) {
        CommandContext commandContext = CommandContextUtil.getCommandContext();
        
        CaseInstanceHelper caseInstanceHelper = CommandContextUtil.getCaseInstanceHelper(commandContext);
        CaseInstanceEntity caseInstanceEntity = caseInstanceHelper.startCaseInstanceByKey(commandContext, caseRef);
        caseInstanceEntity.setParentId(planItemInstance.getCaseInstanceId());
        caseInstanceEntity.setCallbackId(planItemInstance.getId());
        caseInstanceEntity.setCallbackType(CaseInstanceCallbackType.CHILD_CASE);
        
        if (!isBlocking) {
            CommandContextUtil.getAgenda(commandContext).planCompletePlanItem((PlanItemInstanceEntity) planItemInstance);
        }
    }
    
}
