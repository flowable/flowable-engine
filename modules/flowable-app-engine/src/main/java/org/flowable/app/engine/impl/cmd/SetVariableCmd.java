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
public class SetVariableCmd implements Command<Void> {
    
    protected String appDefinitionId;
    protected String variableName;
    protected Object variableValue;
    
    public SetVariableCmd(String appDefinitionId, String variableName, Object variableValue) {
        this.appDefinitionId = appDefinitionId;
        this.variableName = variableName;
        this.variableValue = variableValue;
    }
    
    @Override
    public Void execute(CommandContext commandContext) {
        if (appDefinitionId == null) {
            throw new FlowableIllegalArgumentException("appDefinitionId is null");
        }
        if (variableName == null) {
            throw new FlowableIllegalArgumentException("variable name is null");
        }
        
        VariableTypes variableTypes = CommandContextUtil.getAppEngineConfiguration().getVariableTypes();
        VariableType type = variableTypes.findVariableType(variableValue);
     
        VariableService variableService = CommandContextUtil.getVariableService(commandContext);
        VariableInstanceEntity variableInstance = variableService.createVariableInstance(variableName, type, variableValue);
        variableInstance.setScopeId(appDefinitionId);
        variableInstance.setScopeType(ScopeTypes.APP);
        variableService.updateVariableInstance(variableInstance);
        
        return null;
    }

}
