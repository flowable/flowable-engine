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

import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.variable.service.impl.persistence.entity.VariableInstanceEntity;

/**
 * @author Joram Barrez
 */
public class GetVariableCmd implements Command<Object> {
    
    protected String caseInstanceId;
    protected String variableName;
    
    public GetVariableCmd(String caseInstanceId, String variableName) {
        this.caseInstanceId = caseInstanceId;
        this.variableName = variableName;
    }
    
    @Override
    public Object execute(CommandContext commandContext) {
        if (caseInstanceId == null) {
            throw new FlowableIllegalArgumentException("caseInstanceId is null");
        }
        
        VariableInstanceEntity variableInstanceEntity = CommandContextUtil.getVariableService(commandContext)
                .findVariableInstanceByScopeIdAndScopeTypeAndName(caseInstanceId, ScopeTypes.CMMN, variableName);
        if (variableInstanceEntity != null) {
            return variableInstanceEntity.getValue();
        } 
        return null;
    }

}
