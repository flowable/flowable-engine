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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.variable.service.impl.persistence.entity.VariableInstanceEntity;

/**
 * @author Tijs Rademakers
 */
public class GetLocalVariablesCmd implements Command<Map<String, Object>> {
    
    protected String planItemInstanceId;
    protected Collection<String> variableNames;

    public GetLocalVariablesCmd(String planItemInstanceId, Collection<String> variableNames) {
        this.planItemInstanceId = planItemInstanceId;
        this.variableNames = variableNames;
    }
    
    @Override
    public Map<String, Object> execute(CommandContext commandContext) {
        if (planItemInstanceId == null) {
            throw new FlowableIllegalArgumentException("planItemInstanceId is null");
        }
        
        CmmnEngineConfiguration cmmnEngineConfiguration = CommandContextUtil.getCmmnEngineConfiguration(commandContext);
        List<VariableInstanceEntity> variableInstanceEntities;
        if (variableNames == null || variableNames.isEmpty()) {
            variableInstanceEntities = cmmnEngineConfiguration.getVariableServiceConfiguration().getVariableService()
                    .findVariableInstanceBySubScopeIdAndScopeType(planItemInstanceId, ScopeTypes.CMMN);
        } else {
            variableInstanceEntities = cmmnEngineConfiguration.getVariableServiceConfiguration().getVariableService()
                    .createInternalVariableInstanceQuery()
                    .subScopeId(planItemInstanceId)
                    .scopeType(ScopeTypes.CMMN)
                    .names(variableNames)
                    .list();
        }
        Map<String, Object> variables = new HashMap<>(variableInstanceEntities.size());
        for (VariableInstanceEntity variableInstanceEntity : variableInstanceEntities) {
            variables.put(variableInstanceEntity.getName(), variableInstanceEntity.getValue());
        }
        return variables;
    }

}
