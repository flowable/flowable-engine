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

import java.util.Collection;

import org.flowable.cmmn.engine.impl.persistence.entity.PlanItemInstanceEntity;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.engine.common.api.FlowableIllegalArgumentException;
import org.flowable.engine.common.api.FlowableObjectNotFoundException;
import org.flowable.engine.common.impl.interceptor.Command;
import org.flowable.engine.common.impl.interceptor.CommandContext;

/**
 * @author Tijs Rademakers
 */
public class RemoveLocalVariablesCmd implements Command<Void> {
    
    protected String planItemInstanceId;
    protected Collection<String> variableNames;
    
    public RemoveLocalVariablesCmd(String planItemInstanceId, Collection<String> variableNames) {
        this.planItemInstanceId = planItemInstanceId;
        this.variableNames = variableNames;
    }
    
    @Override
    public Void execute(CommandContext commandContext) {
        if (planItemInstanceId == null) {
            throw new FlowableIllegalArgumentException("planItemInstanceId is null");
        }
        if (variableNames == null) {
            throw new FlowableIllegalArgumentException("variableNames is null");
        }
     
        PlanItemInstanceEntity planItemInstanceEntity = CommandContextUtil.getPlanItemInstanceEntityManager(commandContext).findById(planItemInstanceId);
        if (planItemInstanceEntity == null) {
            throw new FlowableObjectNotFoundException("No plan item instance found for id " + planItemInstanceId, PlanItemInstanceEntity.class);
        }
        planItemInstanceEntity.removeVariables(variableNames);
        
        return null;
    }

}
