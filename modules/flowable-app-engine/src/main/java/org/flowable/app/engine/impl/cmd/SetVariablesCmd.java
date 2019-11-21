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
package org.flowable.app.engine.impl.cmd;

import java.util.Map;

import org.flowable.app.engine.impl.util.CommandContextUtil;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.variable.api.types.VariableType;
import org.flowable.variable.api.types.VariableTypes;
import org.flowable.variable.service.VariableService;
import org.flowable.variable.service.impl.persistence.entity.VariableInstanceEntity;

/**
 * @author Tijs Rademakers
 */
public class SetVariablesCmd implements Command<Void> {
    
    protected String appDefinitionId;
    protected Map<String, Object> variables;
    
    public SetVariablesCmd(String appDefinitionId, Map<String, Object> variables) {
        this.appDefinitionId = appDefinitionId;
        this.variables = variables;
    }
    
    @Override
    public Void execute(CommandContext commandContext) {
        if (appDefinitionId == null) {
            throw new FlowableIllegalArgumentException("appDefinitionId is null");
        }
        if (variables == null) {
            throw new FlowableIllegalArgumentException("variables is null");
        }
        if (variables.isEmpty()) {
            throw new FlowableIllegalArgumentException("variables is empty");
        }
     
        VariableTypes variableTypes = CommandContextUtil.getAppEngineConfiguration().getVariableTypes();
        VariableService variableService = CommandContextUtil.getVariableService(commandContext);
        
        for (String variableName : variables.keySet()) {
            Object variableValue = variables.get(variableName);
            VariableType type = variableTypes.findVariableType(variableValue);
         
            VariableInstanceEntity variableInstance = variableService.createVariableInstance(variableName, type);
            variableInstance.setScopeId(appDefinitionId);
            variableInstance.setScopeType(ScopeTypes.APP);
            variableInstance.setValue(variableValue);
            variableService.updateVariableInstance(variableInstance);
        }
        
        return null;
    }

}
