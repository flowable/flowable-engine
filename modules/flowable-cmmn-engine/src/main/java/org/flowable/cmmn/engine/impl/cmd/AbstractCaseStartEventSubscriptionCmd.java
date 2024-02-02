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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.flowable.cmmn.api.CmmnRepositoryService;
import org.flowable.cmmn.api.repository.CaseDefinition;
import org.flowable.cmmn.converter.CmmnXmlConstants;
import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.impl.persistence.entity.CaseDefinitionEntityManager;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.cmmn.model.Case;
import org.flowable.cmmn.model.CmmnModel;
import org.flowable.cmmn.model.ExtensionElement;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.eventregistry.model.EventModel;
import org.flowable.eventregistry.model.EventPayload;
import org.flowable.eventsubscription.service.EventSubscriptionService;

/**
 * An abstract command with various common methods for the creation and modification of case start event subscriptions.
 *
 * @author Micha Kiener
 */
public class AbstractCaseStartEventSubscriptionCmd {

    protected String generateCorrelationConfiguration(String eventDefinitionKey, String tenantId, Map<String, Object> correlationParameterValues, CommandContext commandContext) {
        EventModel eventModel = getEventModel(eventDefinitionKey, tenantId, commandContext);
        Map<String, Object> correlationParameters = new HashMap<>();
        for (Map.Entry<String, Object> correlationValue : correlationParameterValues.entrySet()) {
            // make sure the correlation parameter value is based on a valid, defined correlation parameter within the event model
            checkEventModelCorrelationParameter(eventModel, correlationValue.getKey());
            correlationParameters.put(correlationValue.getKey(), correlationValue.getValue());
        }

        return CommandContextUtil.getEventRegistry().generateKey(correlationParameters);
    }

    protected void checkEventModelCorrelationParameter(EventModel eventModel, String correlationParameterName) {
        Collection<EventPayload> correlationParameters = eventModel.getCorrelationParameters();
        for (EventPayload correlationParameter : correlationParameters) {
            if (correlationParameter.getName().equals(correlationParameterName)) {
                return;
            }
        }
        throw new FlowableIllegalArgumentException("There is no correlation parameter with name '" + correlationParameterName + "' defined in event model "
            + "with key '" + eventModel.getKey() + "'. You can only subscribe for an event with a combination of valid correlation parameters.");
    }

    protected CaseDefinition getLatestCaseDefinitionByKey(String caseDefinitionKey, String tenantId, CommandContext commandContext) {
        CaseDefinitionEntityManager caseDefinitionEntityManager = CommandContextUtil.getCaseDefinitionEntityManager(commandContext);
        CaseDefinition caseDefinition = null;
        if (caseDefinitionKey != null && (tenantId == null || CmmnEngineConfiguration.NO_TENANT_ID.equals(tenantId))) {
            caseDefinition = caseDefinitionEntityManager.findLatestCaseDefinitionByKey(caseDefinitionKey);

            if (caseDefinition == null) {
                throw new FlowableObjectNotFoundException("No case definition found for key '" + caseDefinitionKey + "'", CaseDefinition.class);
            }

        } else if (caseDefinitionKey != null && tenantId != null && !CmmnEngineConfiguration.NO_TENANT_ID.equals(tenantId)) {

            caseDefinition = caseDefinitionEntityManager.findLatestCaseDefinitionByKeyAndTenantId(caseDefinitionKey, tenantId);

            if (caseDefinition == null) {
                CmmnEngineConfiguration caseEngineConfiguration = CommandContextUtil.getCmmnEngineConfiguration(commandContext);
                if (caseEngineConfiguration.isFallbackToDefaultTenant()) {
                    String defaultTenant = caseEngineConfiguration.getDefaultTenantProvider().getDefaultTenant(tenantId, ScopeTypes.BPMN, caseDefinitionKey);
                    if (StringUtils.isNotEmpty(defaultTenant)) {
                        caseDefinition = caseDefinitionEntityManager.findLatestCaseDefinitionByKeyAndTenantId(caseDefinitionKey, defaultTenant);
                        
                    } else {
                        caseDefinition = caseDefinitionEntityManager.findLatestCaseDefinitionByKey(caseDefinitionKey);
                    }
                    
                    if (caseDefinition == null) {
                        throw new FlowableObjectNotFoundException("No case definition found for key '" + caseDefinitionKey +
                            "'. Fallback to default tenant was also applied.", CaseDefinition.class);
                    }
                    
                } else {
                    throw new FlowableObjectNotFoundException("Case definition with key '" + caseDefinitionKey +
                        "' and tenantId '" + tenantId + "' was not found.", CaseDefinition.class);
                }
            }
        }
        
        if (caseDefinition == null) {
            throw new FlowableIllegalArgumentException("No deployed case definition found for key '" + caseDefinitionKey + "'.");
        }
        
        return caseDefinition;
    }

    protected CaseDefinition getCaseDefinitionById(String caseDefinitionId, CommandContext commandContext) {
        CmmnRepositoryService repositoryService = CommandContextUtil.getCmmnEngineConfiguration(commandContext).getCmmnRepositoryService();

        CaseDefinition caseDefinition = repositoryService.createCaseDefinitionQuery()
            .caseDefinitionId(caseDefinitionId)
            .singleResult();

        if (caseDefinition == null) {
            throw new FlowableIllegalArgumentException("No deployed case definition found for id '" + caseDefinitionId + "'.");
        }
        return caseDefinition;
    }

    protected Case getCase(String caseDefinitionId, CommandContext commandContext) {
        CmmnRepositoryService repositoryService = CommandContextUtil.getCmmnEngineConfiguration(commandContext).getCmmnRepositoryService();
        CmmnModel cmmnModel = repositoryService.getCmmnModel(caseDefinitionId);
        return cmmnModel.getPrimaryCase();
    }

    protected EventModel getEventModel(String eventDefinitionKey, String tenantId, CommandContext commandContext) {
        EventModel eventModel = CommandContextUtil.getEventRepositoryService(commandContext).getEventModelByKey(eventDefinitionKey, tenantId);
        if (eventModel == null) {
            throw new FlowableIllegalArgumentException("Could not find event model with key '" + eventDefinitionKey + "'.");
        }
        return eventModel;
    }

    protected String getStartCorrelationConfiguration(String caseDefinitionId, CommandContext commandContext) {
        CmmnModel cmmnModel = CommandContextUtil.getCmmnEngineConfiguration(commandContext).getCmmnRepositoryService().getCmmnModel(caseDefinitionId);
        if (cmmnModel != null) {
            List<ExtensionElement> correlationCfgExtensions = cmmnModel.getPrimaryCase().getExtensionElements()
                .getOrDefault(CmmnXmlConstants.START_EVENT_CORRELATION_CONFIGURATION, Collections.emptyList());
            if (!correlationCfgExtensions.isEmpty()) {
                return correlationCfgExtensions.get(0).getElementText();
            }
        }
        return null;
    }

    protected EventSubscriptionService getEventSubscriptionService(CommandContext commandContext) {
        return CommandContextUtil.getCmmnEngineConfiguration(commandContext).getEventSubscriptionServiceConfiguration().getEventSubscriptionService();
    }
}
