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
package org.flowable.cmmn.engine.impl.job;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.flowable.cmmn.api.repository.CaseDefinition;
import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.impl.agenda.CmmnEngineAgenda;
import org.flowable.cmmn.engine.impl.deployer.CmmnDeploymentManager;
import org.flowable.cmmn.engine.impl.persistence.entity.CaseInstanceEntity;
import org.flowable.cmmn.engine.impl.persistence.entity.CountingPlanItemInstanceEntity;
import org.flowable.cmmn.engine.impl.persistence.entity.PlanItemInstanceEntity;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.cmmn.model.Case;
import org.flowable.cmmn.model.CmmnModel;
import org.flowable.cmmn.model.VariableEventListener;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.job.service.JobHandler;
import org.flowable.job.service.impl.persistence.entity.JobEntity;
import org.flowable.variable.api.delegate.VariableScope;
import org.flowable.variable.service.VariableService;
import org.flowable.variable.service.impl.persistence.entity.VariableInstanceEntity;

public class SetAsyncVariablesJobHandler implements JobHandler {

    public static final String TYPE = "cmmn-set-async-variables";
    
    protected CmmnEngineConfiguration cmmnEngineConfiguration;
    
    public SetAsyncVariablesJobHandler(CmmnEngineConfiguration cmmnEngineConfiguration) {
        this.cmmnEngineConfiguration = cmmnEngineConfiguration;
    }

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public void execute(JobEntity job, String configuration, VariableScope variableScope, CommandContext commandContext) {
        VariableService variableService = cmmnEngineConfiguration.getVariableServiceConfiguration().getVariableService();
        
        List<VariableInstanceEntity> jobVariables = null;
        PlanItemInstanceEntity planItemInstanceEntity = null;
        CaseInstanceEntity caseInstanceEntity = null;
        String caseDefinitionId = null;
        String caseInstanceId = null;
        if (StringUtils.isNotEmpty(job.getSubScopeId())) {
            planItemInstanceEntity = cmmnEngineConfiguration.getPlanItemInstanceEntityManager().findById(job.getSubScopeId());
            jobVariables = variableService.findVariableInstanceBySubScopeIdAndScopeType(job.getSubScopeId(), ScopeTypes.CMMN_ASYNC_VARIABLES);
            caseDefinitionId = planItemInstanceEntity.getCaseDefinitionId();
            caseInstanceId = planItemInstanceEntity.getCaseInstanceId();
            
        } else {
            caseInstanceEntity = cmmnEngineConfiguration.getCaseInstanceEntityManager().findById(job.getScopeId());
            jobVariables = variableService.findVariableInstanceByScopeIdAndScopeType(job.getScopeId(), ScopeTypes.CMMN_ASYNC_VARIABLES);
            caseDefinitionId = caseInstanceEntity.getCaseDefinitionId();
            caseInstanceId = caseInstanceEntity.getId();
        }

        if (!jobVariables.isEmpty()) {
            Set<String> variableNames = new HashSet<>();
            for (VariableInstanceEntity jobVariable : jobVariables) {
                variableNames.add(jobVariable.getName());
                if (caseInstanceEntity != null) {
                    caseInstanceEntity.setVariable(jobVariable.getName(), jobVariable.getValue());
                } else {
                    planItemInstanceEntity.setVariableLocal(jobVariable.getName(), jobVariable.getValue());
                }
                
                variableService.deleteVariableInstance(jobVariable);
            }

            if (planItemInstanceEntity != null && planItemInstanceEntity instanceof CountingPlanItemInstanceEntity) {
                ((CountingPlanItemInstanceEntity) planItemInstanceEntity)
                        .setVariableCount(((CountingPlanItemInstanceEntity) planItemInstanceEntity).getVariableCount() - jobVariables.size());
            }
            
            CmmnEngineAgenda agenda = CommandContextUtil.getAgenda(commandContext);
            
            if (planItemInstanceEntity == null) {
                CmmnDeploymentManager deploymentManager = cmmnEngineConfiguration.getDeploymentManager();
                CaseDefinition caseDefinition = deploymentManager.findDeployedCaseDefinitionById(caseDefinitionId);
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
            }
            
            agenda.planEvaluateCriteriaOperation(caseInstanceId);
        }
    }
}
