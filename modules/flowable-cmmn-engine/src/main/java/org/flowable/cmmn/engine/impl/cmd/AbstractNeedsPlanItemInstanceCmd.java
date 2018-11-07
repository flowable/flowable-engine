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

import java.io.Serializable;

import org.flowable.cmmn.engine.impl.persistence.entity.PlanItemInstanceEntity;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;

/**
 * @author Joram Barrez
 */
public abstract class AbstractNeedsPlanItemInstanceCmd implements Command<Void>, Serializable {

    protected String planItemInstanceId;

    public AbstractNeedsPlanItemInstanceCmd(String planItemInstanceId) {
        this.planItemInstanceId = planItemInstanceId;
    }

    @Override
    public Void execute(CommandContext commandContext) {
        if (planItemInstanceId == null) {
            throw new FlowableIllegalArgumentException("Plan item instance id is null");
        }
        PlanItemInstanceEntity planItemInstanceEntity = CommandContextUtil.getPlanItemInstanceEntityManager(commandContext).findById(planItemInstanceId);
        if (planItemInstanceEntity == null) {
            throw new FlowableObjectNotFoundException("Cannot find plan item instance for id " + planItemInstanceId, PlanItemInstanceEntity.class);
        }
        
        internalExecute(commandContext, planItemInstanceEntity);
        return null;
    }
    
    protected abstract void internalExecute(CommandContext commandContext, PlanItemInstanceEntity planItemInstanceEntity);
   
}
