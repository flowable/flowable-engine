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
package org.flowable.cmmn.engine.impl.runtime;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.flowable.cmmn.engine.impl.deployer.CmmnDeploymentManager;
import org.flowable.cmmn.engine.impl.persistence.entity.CaseInstanceEntity;
import org.flowable.cmmn.engine.impl.persistence.entity.CaseInstanceEntityManager;
import org.flowable.cmmn.engine.impl.persistence.entity.SentryOnPartInstanceEntity;
import org.flowable.cmmn.engine.impl.repository.CaseDefinitionUtil;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.cmmn.engine.repository.CaseDefinition;
import org.flowable.cmmn.engine.runtime.CaseInstance;
import org.flowable.cmmn.engine.runtime.CaseInstanceState;
import org.flowable.cmmn.model.Stage;
import org.flowable.engine.common.api.FlowableObjectNotFoundException;
import org.flowable.engine.common.impl.callback.CallbackData;
import org.flowable.engine.common.impl.callback.RuntimeInstanceStateChangeCallback;
import org.flowable.engine.common.impl.interceptor.CommandContext;

/**
 * @author Joram Barrez
 */
public class CaseInstanceHelperImpl implements CaseInstanceHelper {
    
    @Override
    public CaseInstanceEntity startCaseInstanceByKey(CommandContext commandContext, String caseDefinitionKey) {
        CmmnDeploymentManager deploymentManager = CommandContextUtil.getCmmnEngineConfiguration(commandContext).getDeploymentManager();
        CaseDefinition caseDefinition = deploymentManager.findDeployedLatestCaseDefinitionByKey(caseDefinitionKey);
        if (caseDefinition == null) {
            throw new FlowableObjectNotFoundException("No case definition found for key " + caseDefinitionKey, CaseDefinition.class);
        }
        return startCaseInstance(commandContext, caseDefinition);
    }
    
    public CaseInstanceEntity startCaseInstanceById(CommandContext commandContext, String caseDefinitionId) {
        CmmnDeploymentManager deploymentManager = CommandContextUtil.getCmmnEngineConfiguration(commandContext).getDeploymentManager();
        CaseDefinition caseDefinition = null;
        if (caseDefinitionId != null) {
            caseDefinition = deploymentManager.findDeployedCaseDefinitionById(caseDefinitionId);
            if (caseDefinition == null) {
                throw new FlowableObjectNotFoundException("No case definition found for id " + caseDefinitionId, CaseDefinition.class);
            }
        }
        return startCaseInstance(commandContext, caseDefinition);
    }

    protected CaseInstanceEntity startCaseInstance(CommandContext commandContext, CaseDefinition caseDefinition) {
        CaseInstanceEntity caseInstanceEntity = createCaseInstanceEntity(commandContext, caseDefinition);
        
        callCaseInstanceStateChangeCallbacks(commandContext, caseInstanceEntity, null, CaseInstanceState.ACTIVE);
        CommandContextUtil.getCmmnHistoryManager().recordCaseInstanceStart(caseInstanceEntity);
        
        // The InitPlanModelOperation will take care of initializing all the child plan items of that stage
        CommandContextUtil.getAgenda(commandContext).planInitPlanModelOperation(caseInstanceEntity);
        
        return caseInstanceEntity;
    }

    protected Stage getPlanModel(CommandContext commandContext, CaseDefinition caseDefinition) {
        return CaseDefinitionUtil.getCmmnModel(caseDefinition.getId()).getPrimaryCase().getPlanModel();
    }
    
    protected CaseInstanceEntity createCaseInstanceEntity(CommandContext commandContext, CaseDefinition caseDefinition) {
        CaseInstanceEntityManager caseInstanceEntityManager = CommandContextUtil.getCaseInstanceEntityManager(commandContext);
        CaseInstanceEntity caseInstanceEntity = caseInstanceEntityManager.create();
        caseInstanceEntity.setCaseDefinitionId(caseDefinition.getId());
        caseInstanceEntity.setStartTime(CommandContextUtil.getCmmnEngineConfiguration(commandContext).getClock().getCurrentTime());
        caseInstanceEntity.setState(CaseInstanceState.ACTIVE);
        caseInstanceEntity.setTenantId(caseDefinition.getTenantId());
        caseInstanceEntityManager.insert(caseInstanceEntity);
        
        caseInstanceEntity.setSatisfiedSentryOnPartInstances(new ArrayList<SentryOnPartInstanceEntity>(1));
        
        return caseInstanceEntity;
    }
    
    public void callCaseInstanceStateChangeCallbacks(CommandContext commandContext, CaseInstance caseInstance, String oldState, String newState) {
        if (caseInstance.getCallbackId() != null && caseInstance.getCallbackType() != null) {
            Map<String, List<RuntimeInstanceStateChangeCallback>> caseInstanceCallbacks = CommandContextUtil
                    .getCmmnEngineConfiguration(commandContext).getCaseInstanceStateChangeCallbacks();
            if (caseInstanceCallbacks != null && caseInstanceCallbacks.containsKey(caseInstance.getCallbackType())) {
                for (RuntimeInstanceStateChangeCallback caseInstanceCallback : caseInstanceCallbacks.get(caseInstance.getCallbackType())) {
                    caseInstanceCallback.stateChanged(new CallbackData(caseInstance.getCallbackId(), 
                                                                       caseInstance.getCallbackType(), 
                                                                       caseInstance.getId(), 
                                                                       oldState, 
                                                                       newState));
                }
            }
        }
    }
    
}
