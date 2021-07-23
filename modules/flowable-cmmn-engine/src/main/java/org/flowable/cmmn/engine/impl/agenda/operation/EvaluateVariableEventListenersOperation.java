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

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.flowable.bpmn.model.VariableListenerEventDefinition;
import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.impl.persistence.entity.PlanItemInstanceEntity;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.variablelistener.VariableListenerSession;
import org.flowable.common.engine.impl.variablelistener.VariableListenerSessionData;
import org.flowable.eventsubscription.service.impl.persistence.entity.EventSubscriptionEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;

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
        
        CmmnEngineConfiguration cmmnEngineConfiguration = CommandContextUtil.getCmmnEngineConfiguration(commandContext);
        List<EventSubscriptionEntity> eventSubscriptionEntities = cmmnEngineConfiguration.getEventSubscriptionServiceConfiguration().getEventSubscriptionEntityManager()
                .findEventSubscriptionsByScopeIdAndType(caseInstanceEntity.getId(), "variable");
        
        boolean triggeredPlanItemInstance = false;
        for (EventSubscriptionEntity eventSubscription : eventSubscriptionEntities) {
            
            if (eventSubscription.isDeleted() || !variableSessionData.containsKey(eventSubscription.getEventName())) {
                continue;
            }
            
            List<VariableListenerSessionData> variableListenerDataList = variableSessionData.get(eventSubscription.getEventName());
            Iterator<VariableListenerSessionData> itVariableListener = variableListenerDataList.iterator();
            while (itVariableListener.hasNext()) {
                VariableListenerSessionData variableListenerData = itVariableListener.next();
               
                if (!caseInstanceEntity.getId().equals(variableListenerData.getScopeId())) {
                    continue;
                }
                
                String subScopeId = eventSubscription.getSubScopeId();
                PlanItemInstanceEntity planItemInstance = cmmnEngineConfiguration.getPlanItemInstanceEntityManager().findById(subScopeId);
                
                String configuration = eventSubscription.getConfiguration();
                String changeTypeValue = VariableListenerEventDefinition.CHANGE_TYPE_ALL;
                if (StringUtils.isNotEmpty(configuration)) {
                    try {
                        JsonNode configNode = cmmnEngineConfiguration.getObjectMapper().readTree(configuration);
                        if (configNode.has(VariableListenerEventDefinition.CHANGE_TYPE_PROPERTY) && !configNode.get(VariableListenerEventDefinition.CHANGE_TYPE_PROPERTY).isNull()) {
                            changeTypeValue = configNode.get(VariableListenerEventDefinition.CHANGE_TYPE_PROPERTY).asText();
                        }
                    } catch (Exception e) {
                        LOGGER.error("Error reading variable listener configuration value for {}", eventSubscription.getActivityId(), e);
                    }
                }
            
                if (changeTypeValue.equals(variableListenerData.getChangeType()) || VariableListenerEventDefinition.CHANGE_TYPE_ALL.equals(changeTypeValue) ||
                            (VariableListenerEventDefinition.CHANGE_TYPE_UPDATE_CREATE.equals(changeTypeValue) &&
                                    (VariableListenerEventDefinition.CHANGE_TYPE_CREATE.equals(variableListenerData.getChangeType()) || VariableListenerEventDefinition.CHANGE_TYPE_UPDATE.equals(variableListenerData.getChangeType())))) {
                        
                    itVariableListener.remove();
                    CommandContextUtil.getAgenda().planTriggerPlanItemInstanceOperation(planItemInstance);
                    triggeredPlanItemInstance = true;
                }         
            }
        }
        
        if (!triggeredPlanItemInstance) { 
            markAsNoop();
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
