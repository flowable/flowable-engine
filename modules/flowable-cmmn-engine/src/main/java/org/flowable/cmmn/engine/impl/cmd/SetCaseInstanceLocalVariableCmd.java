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

import org.flowable.cmmn.engine.impl.persistence.entity.CaseInstanceEntity;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;

/**
 * Sets variable locally on the defined case instance. {@link SetLocalVariableCmd} sets variable on the planItem scope.
 * {@link SetVariableCmd} sets the variable on the parent scope.
 *
 * @author martin.grofcik
 */
public class SetCaseInstanceLocalVariableCmd implements Command<Void> {

    protected String caseInstanceId;
    protected String variableName;
    protected Object variableValue;

    public SetCaseInstanceLocalVariableCmd(String caseInstanceId, String variableName, Object variableValue) {
        this.caseInstanceId = caseInstanceId;
        this.variableName = variableName;
        this.variableValue = variableValue;
    }
    
    @Override
    public Void execute(CommandContext commandContext) {
        if (caseInstanceId == null) {
            throw new FlowableIllegalArgumentException("caseInstanceId is null");
        }
        if (variableName == null) {
            throw new FlowableIllegalArgumentException("variable name is null");
        }
     
        CaseInstanceEntity caseInstanceEntity = CommandContextUtil.getCaseInstanceEntityManager(commandContext).findById(caseInstanceId);
        if (caseInstanceEntity == null) {
            throw new FlowableObjectNotFoundException("No case instance found for id " + caseInstanceId, CaseInstanceEntity.class);
        }
        caseInstanceEntity.setVariableLocal(variableName, variableValue);
        
        CommandContextUtil.getAgenda(commandContext).planEvaluateCriteriaOperation(caseInstanceId);
        
        return null;
    }

}
