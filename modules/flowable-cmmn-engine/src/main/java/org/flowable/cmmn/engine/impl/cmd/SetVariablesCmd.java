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

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.flowable.cmmn.api.repository.CaseDefinition;
import org.flowable.cmmn.engine.impl.agenda.CmmnEngineAgenda;
import org.flowable.cmmn.engine.impl.deployer.CmmnDeploymentManager;
import org.flowable.cmmn.engine.impl.persistence.entity.CaseInstanceEntity;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.cmmn.model.Case;
import org.flowable.cmmn.model.CmmnModel;
import org.flowable.cmmn.model.VariableEventListener;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;

/**
 * @author Joram Barrez
 */
public class SetVariablesCmd implements Command<Void> {
    
    protected String caseInstanceId;
    protected Map<String, Object> variables;
    
    public SetVariablesCmd(String caseInstanceId, Map<String, Object> variables) {
        this.caseInstanceId = caseInstanceId;
        this.variables = variables;
    }
    
    @Override
    public Void execute(CommandContext commandContext) {
        if (caseInstanceId == null) {
            throw new FlowableIllegalArgumentException("caseInstanceId is null");
        }
        if (variables == null) {
            throw new FlowableIllegalArgumentException("variables is null");
        }
        if (variables.isEmpty()) {
            throw new FlowableIllegalArgumentException("variables is empty");
        }
     
        CaseInstanceEntity caseInstanceEntity = CommandContextUtil.getCaseInstanceEntityManager(commandContext).findById(caseInstanceId);
        if (caseInstanceEntity == null) {
            throw new FlowableObjectNotFoundException("No case instance found for id " + caseInstanceId, CaseInstanceEntity.class);
        }
        caseInstanceEntity.setVariables(variables);
        
        Set<String> variableNames = variables.keySet();
        
        CmmnEngineAgenda agenda = CommandContextUtil.getAgenda(commandContext);
        
        CmmnDeploymentManager deploymentManager = CommandContextUtil.getCmmnEngineConfiguration(commandContext).getDeploymentManager();
        CaseDefinition caseDefinition = deploymentManager.findDeployedCaseDefinitionById(caseInstanceEntity.getCaseDefinitionId());
        boolean evaluateVariableEventListener = false;
        if (caseDefinition != null) {
            CmmnModel cmmnModel = deploymentManager.resolveCaseDefinition(caseDefinition).getCmmnModel();
            for (Case caze : cmmnModel.getCases()) {
                List<VariableEventListener> variableEventListeners = caze.findPlanItemDefinitionsOfType(VariableEventListener.class);
                for (VariableEventListener variableEventListener : variableEventListeners) {
                    if (variableNames.contains(variableEventListener.getVariableName())) {
                        evaluateVariableEventListener = true;
                        break;
                    }
                }
                
                if (evaluateVariableEventListener) {
                    break;
                }
            }
        }
        
        if (evaluateVariableEventListener) {
            agenda.planEvaluateVariableEventListenersOperation(caseInstanceId);
        }
        
        agenda.planEvaluateCriteriaOperation(caseInstanceId);
        
        return null;
    }

}
