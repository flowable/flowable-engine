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
package org.flowable.cmmn.engine.impl.agenda.operation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.flowable.bpmn.model.VariableListenerEventDefinition;
import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstanceState;
import org.flowable.cmmn.engine.impl.persistence.entity.PlanItemInstanceContainer;
import org.flowable.cmmn.engine.impl.persistence.entity.PlanItemInstanceEntity;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.cmmn.model.PlanItemDefinition;
import org.flowable.cmmn.model.Stage;
import org.flowable.cmmn.model.VariableEventListener;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.variablelistener.VariableListenerSession;
import org.flowable.common.engine.impl.variablelistener.VariableListenerSessionData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EvaluateVariableEventListenersOperation extends AbstractEvaluationCriteriaOperation {

    private static final Logger LOGGER = LoggerFactory.getLogger(EvaluateVariableEventListenersOperation.class);

    public EvaluateVariableEventListenersOperation(CommandContext commandContext, String caseInstanceEntityId) {
        super(commandContext, caseInstanceEntityId, null, null);
    }

    @Override
    public void run() {
        super.run();

        if (caseInstanceEntity.isDeleted()) {
            markAsNoop();
            return;
        }
        
        VariableListenerSession variableListenerSession = commandContext.getSession(VariableListenerSession.class);
        Map<String, List<VariableListenerSessionData>> variableSessionData = variableListenerSession.getVariableData();
        if (variableSessionData == null || variableSessionData.isEmpty()) {
            markAsNoop();
            return;
        }
        
        Map<String, List<PlanItemInstanceEntity>> variableListenerMap = new HashMap<>();
        collectVariableListenerPlanItemInstances(caseInstanceEntity, variableListenerMap);
        
        boolean triggeredPlanItemInstance = false;
        for (String variableName : variableSessionData.keySet()) {
            
            if (!variableListenerMap.containsKey(variableName)) {
                continue;
            }
            
            List<VariableListenerSessionData> variableListenerDataList = variableSessionData.get(variableName);
            Iterator<VariableListenerSessionData> itVariableListener = variableListenerDataList.iterator();
            while (itVariableListener.hasNext()) {
                VariableListenerSessionData variableListenerData = itVariableListener.next();
               
                for (PlanItemInstanceEntity planItemInstanceEntity : variableListenerMap.get(variableName)) {
                    if (!planItemInstanceEntity.getCaseInstanceId().equals(variableListenerData.getScopeId())) {
                        continue;
                    }
                    
                    PlanItemDefinition planItemDefinition = planItemInstanceEntity.getPlanItemDefinition();
                    VariableEventListener variableEventListener = (VariableEventListener) planItemDefinition;
                    if (StringUtils.isEmpty(variableEventListener.getVariableChangeType()) || variableEventListener.getVariableChangeType().equals(variableListenerData.getChangeType()) ||
                            VariableListenerEventDefinition.CHANGE_TYPE_ALL.equals(variableEventListener.getVariableChangeType()) ||
                            (VariableListenerEventDefinition.CHANGE_TYPE_UPDATE_CREATE.equals(variableEventListener.getVariableChangeType()) &&
                                    (VariableListenerEventDefinition.CHANGE_TYPE_CREATE.equals(variableListenerData.getChangeType()) || VariableListenerEventDefinition.CHANGE_TYPE_UPDATE.equals(variableListenerData.getChangeType())))) {
                        
                        itVariableListener.remove();
                        CommandContextUtil.getAgenda().planTriggerPlanItemInstanceOperation(planItemInstanceEntity);
                        triggeredPlanItemInstance = true;
                    }
                }
            }               
        }
        
        if (!triggeredPlanItemInstance) { 
            markAsNoop();
        }
    }
    
    protected void collectVariableListenerPlanItemInstances(PlanItemInstanceContainer planItemInstanceContainer,
            Map<String, List<PlanItemInstanceEntity>> variableListenerMap) {
        
        for (PlanItemInstance planItemInstance : planItemInstanceContainer.getChildPlanItemInstances()) {
            PlanItemInstanceEntity planItemInstanceEntity = (PlanItemInstanceEntity) planItemInstance;
            PlanItemDefinition planItemDefinition = planItemInstanceEntity.getPlanItemDefinition();
            if (planItemDefinition instanceof VariableEventListener && PlanItemInstanceState.AVAILABLE.equals(planItemInstanceEntity.getState())) {
                VariableEventListener variableEventListener = (VariableEventListener) planItemDefinition;
                if (!variableListenerMap.containsKey(variableEventListener.getVariableName())) {
                    variableListenerMap.put(variableEventListener.getVariableName(), new ArrayList<>());
                }
                
                variableListenerMap.get(variableEventListener.getVariableName()).add(planItemInstanceEntity);
            
            } else if (planItemDefinition instanceof Stage) {
                collectVariableListenerPlanItemInstances(planItemInstanceEntity, variableListenerMap);
            }
        }
    }

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("[Evaluate Variable Event Listeners] case instance ");
        if (caseInstanceEntity != null) {
            stringBuilder.append(caseInstanceEntity.getId());
        } else {
            stringBuilder.append(caseInstanceEntityId);
        }

        return stringBuilder.toString();
    }
    
}
