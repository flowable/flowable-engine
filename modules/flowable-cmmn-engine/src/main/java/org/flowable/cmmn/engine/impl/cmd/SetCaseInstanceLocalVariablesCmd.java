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

import java.util.Map;

/**
 * Sets variables locally on the defined case instance. {@link SetLocalVariablesCmd} sets variables on the planItem scope.
 * {@link SetVariablesCmd} sets the variable on the parent scope.
 *
 * @author martin.grofcik
 */
public class SetCaseInstanceLocalVariablesCmd implements Command<Void> {

    protected String caseInstanceId;
    protected Map<String, Object> variables;

    public SetCaseInstanceLocalVariablesCmd(String caseInstanceId, Map<String, Object> variables) {
        this.caseInstanceId = caseInstanceId;
        this.variables = variables;
    }
    
    @Override
    public Void execute(CommandContext commandContext) {
        if (caseInstanceId == null) {
            throw new FlowableIllegalArgumentException("CaseInstanceId is null");
        }
        if (variables == null) {
            throw new FlowableIllegalArgumentException("variables are null");
        }
        if (variables.isEmpty()) {
            throw new FlowableIllegalArgumentException("variables are empty");
        }
     
        CaseInstanceEntity CaseInstanceEntity = CommandContextUtil.getCaseInstanceEntityManager(commandContext).findById(caseInstanceId);
        if (CaseInstanceEntity == null) {
            throw new FlowableObjectNotFoundException("No case  instance found for id " + caseInstanceId, CaseInstanceEntity.class);
        }
        CaseInstanceEntity.setVariablesLocal(variables);
        
        CommandContextUtil.getAgenda(commandContext).planEvaluateCriteriaOperation(caseInstanceId);
        
        return null;
    }

}
