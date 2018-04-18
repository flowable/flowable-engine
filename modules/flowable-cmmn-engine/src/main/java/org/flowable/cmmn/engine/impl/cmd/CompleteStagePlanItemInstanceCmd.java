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

import org.flowable.cmmn.api.runtime.PlanItemDefinitionType;
import org.flowable.cmmn.engine.impl.persistence.entity.PlanItemInstanceEntity;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.impl.interceptor.CommandContext;

/**
 * @author Joram Barrez
 */
public class CompleteStagePlanItemInstanceCmd extends AbstractNeedsPlanItemInstanceCmd {

    public CompleteStagePlanItemInstanceCmd(String planItemInstanceId) {
        super(planItemInstanceId);
    }
    
    @Override
    protected void internalExecute(CommandContext commandContext, PlanItemInstanceEntity planItemInstanceEntity) {
        if (!PlanItemDefinitionType.STAGE.equals(planItemInstanceEntity.getPlanItemDefinitionType())) {
            throw new FlowableIllegalArgumentException("Can only complete plan item instances of type stage. Type is " + planItemInstanceEntity.getPlanItemDefinitionType());
        }
        if (!planItemInstanceEntity.isCompleteable()) {
            throw new FlowableIllegalArgumentException("Can only complete a stage plan item instance that is marked as completeable (there might still be active plan item instance).");
        }
        CommandContextUtil.getAgenda(commandContext).planCompletePlanItemInstanceOperation(planItemInstanceEntity);
    }
    
}
