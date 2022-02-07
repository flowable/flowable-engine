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
package org.flowable.engine.impl.eventregistry;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.flowable.bpmn.constants.BpmnXMLConstants;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.ExtensionElement;
import org.flowable.bpmn.model.StartEvent;
import org.flowable.common.engine.api.constant.ReferenceTypes;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ProcessInstanceBuilder;
import org.flowable.engine.runtime.ProcessInstanceQuery;
import org.flowable.eventregistry.api.EventConsumerInfo;
import org.flowable.eventregistry.api.EventRegistryProcessingInfo;
import org.flowable.eventregistry.api.runtime.EventInstance;
import org.flowable.eventregistry.impl.constant.EventConstants;
import org.flowable.eventregistry.impl.consumer.BaseEventRegistryEventConsumer;
import org.flowable.eventregistry.impl.consumer.CorrelationKey;
import org.flowable.eventsubscription.api.EventSubscription;
import org.flowable.eventsubscription.api.EventSubscriptionQuery;
import org.flowable.eventsubscription.service.impl.EventSubscriptionQueryImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BpmnEventRegistryEventConsumer extends BaseEventRegistryEventConsumer {

    private static final Logger LOGGER = LoggerFactory.getLogger(BpmnEventRegistryEventConsumer.class);

    protected ProcessEngineConfigurationImpl processEngineConfiguration;

    public BpmnEventRegistryEventConsumer(ProcessEngineConfigurationImpl processEngineConfiguration) {
        super(processEngineConfiguration);
        
        this.processEngineConfiguration = processEngineConfiguration;
    }

    @Override
    public String getConsumerKey() {
        return "bpmnEventConsumer";
    }
    
    @Override
    protected EventRegistryProcessingInfo eventReceived(EventInstance eventInstance) {

        // Fetching the event subscriptions happens in one transaction,
        // executing them one per subscription. There is no overarching transaction.
        // The reason for this is that the handling of one event subscription
        // should not influence (i.e. roll back) the handling of another.

        EventRegistryProcessingInfo eventRegistryProcessingInfo = new EventRegistryProcessingInfo();
        
        Collection<CorrelationKey> correlationKeys = generateCorrelationKeys(eventInstance.getCorrelationParameterInstances());
        List<EventSubscription> eventSubscriptions = findEventSubscriptions(ScopeTypes.BPMN, eventInstance, correlationKeys);
        RuntimeService runtimeService = processEngineConfiguration.getRuntimeService();
        for (EventSubscription eventSubscription : eventSubscriptions) {
            EventConsumerInfo eventConsumerInfo = new EventConsumerInfo(eventSubscription.getId(), eventSubscription.getExecutionId(), 
                    eventSubscription.getProcessDefinitionId(), ScopeTypes.BPMN);
            handleEventSubscription(runtimeService, eventSubscription, eventInstance, correlationKeys, eventConsumerInfo);
            eventRegistryProcessingInfo.addEventConsumerInfo(eventConsumerInfo);
        }
        
        return eventRegistryProcessingInfo;
    }

    protected void handleEventSubscription(RuntimeService runtimeService, EventSubscription eventSubscription,
            EventInstance eventInstance, Collection<CorrelationKey> correlationKeys, EventConsumerInfo eventConsumerInfo) {

        if (eventSubscription.getExecutionId() != null) {

            // When an executionId is set, this means that the process instance is waiting at that step for an event

            Map<String, Object> transientVariableMap = new HashMap<>();
            transientVariableMap.put(EventConstants.EVENT_INSTANCE, eventInstance);
            runtimeService.trigger(eventSubscription.getExecutionId(), null, transientVariableMap);

        } else if (eventSubscription.getProcessDefinitionId() != null
                        && eventSubscription.getProcessInstanceId() == null && eventSubscription.getExecutionId() == null) {

            // If there is no execution/process instance set, but a definition id is set, this means that it's a start event

            ProcessInstanceBuilder processInstanceBuilder = runtimeService.createProcessInstanceBuilder()
                .processDefinitionId(eventSubscription.getProcessDefinitionId())
                .transientVariable(EventConstants.EVENT_INSTANCE, eventInstance);

            if (eventInstance.getTenantId() != null && !Objects.equals(ProcessEngineConfiguration.NO_TENANT_ID, eventInstance.getTenantId())) {
                processInstanceBuilder.overrideProcessDefinitionTenantId(eventInstance.getTenantId());
            }

            if (correlationKeys != null) {
                String startCorrelationConfiguration = getStartCorrelationConfiguration(eventSubscription);

                if (Objects.equals(startCorrelationConfiguration, BpmnXMLConstants.START_EVENT_CORRELATION_STORE_AS_UNIQUE_REFERENCE_ID)) {

                    CorrelationKey correlationKeyWithAllParameters = getCorrelationKeyWithAllParameters(correlationKeys);
                    
                    ProcessDefinition processDefinition = processEngineConfiguration.getRepositoryService().getProcessDefinition(eventSubscription.getProcessDefinitionId());

                    ProcessInstanceQuery processInstanceQuery = runtimeService.createProcessInstanceQuery()
                        .processDefinitionKey(processDefinition.getKey())
                        .processInstanceReferenceId(correlationKeyWithAllParameters.getValue())
                        .processInstanceReferenceType(ReferenceTypes.EVENT_PROCESS);
                    
                    if (eventInstance.getTenantId() != null && !Objects.equals(ProcessEngineConfiguration.NO_TENANT_ID, eventInstance.getTenantId())) {
                        processInstanceQuery.processInstanceTenantId(eventInstance.getTenantId());
                    }
                    
                    long processInstanceCount = processInstanceQuery.count();

                    if (processInstanceCount > 0) {
                        // Returning, no new instance should be started
                        eventConsumerInfo.setHasExistingInstancesForUniqueCorrelation(true);
                        LOGGER.debug("Event received to start a new process instance, but a unique instance already exists.");
                        return;
                    }

                    processInstanceBuilder.referenceId(correlationKeyWithAllParameters.getValue());
                    processInstanceBuilder.referenceType(ReferenceTypes.EVENT_PROCESS);

                }
            }

            if (processEngineConfiguration.isEventRegistryStartProcessInstanceAsync()) {
                processInstanceBuilder.startAsync();
            } else {
                processInstanceBuilder.start();
            }
        }

    }

    protected String getStartCorrelationConfiguration(EventSubscription eventSubscription) {
        BpmnModel bpmnModel = processEngineConfiguration.getRepositoryService().getBpmnModel(eventSubscription.getProcessDefinitionId());
        if (bpmnModel != null) {

            // There are potentially multiple start events, with different configurations.
            // The one that has the matching eventType needs to be used

            List<StartEvent> startEvents = bpmnModel.getMainProcess().findFlowElementsOfType(StartEvent.class);
            for (StartEvent startEvent : startEvents) {
                List<ExtensionElement> eventTypes = startEvent.getExtensionElements().get(BpmnXMLConstants.ELEMENT_EVENT_TYPE);
                if (eventTypes != null && !eventTypes.isEmpty()
                        && Objects.equals(eventSubscription.getEventType(), eventTypes.get(0).getElementText())) {

                    List<ExtensionElement> correlationCfgExtensions = startEvent.getExtensionElements()
                        .getOrDefault(BpmnXMLConstants.START_EVENT_CORRELATION_CONFIGURATION, Collections.emptyList());
                    if (!correlationCfgExtensions.isEmpty()) {
                        return correlationCfgExtensions.get(0).getElementText();
                    }
                }
            }

        }

        return null;
    }

    @Override
    protected EventSubscriptionQuery createEventSubscriptionQuery() {
        return new EventSubscriptionQueryImpl(commandExecutor, processEngineConfiguration.getEventSubscriptionServiceConfiguration());
    }

}
