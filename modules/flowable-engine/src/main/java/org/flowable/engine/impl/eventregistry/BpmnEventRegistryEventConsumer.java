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
import org.flowable.common.engine.impl.cfg.TransactionPropagation;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandConfig;
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.util.CommandContextUtil;
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

                    ProcessDefinition processDefinition = processEngineConfiguration.getRepositoryService()
                            .getProcessDefinition(eventSubscription.getProcessDefinitionId());

                    long processInstanceCount = countProcessInstances(runtimeService, eventInstance, correlationKeyWithAllParameters, processDefinition);
                    if (processInstanceCount > 0) {
                        // Returning, no new instance should be started
                        eventConsumerInfo.setHasExistingInstancesForUniqueCorrelation(true);
                        LOGGER.debug("Event received to start a new process instance, but a unique instance already exists.");
                        return;

                    } else {

                        /*
                         * When multiple threads/transactions are querying concurrently, it could happen
                         * that multiple times zero is returned as result of the count.
                         *
                         * To make sure only one unique instance is created, the event subscription
                         * is locked first, which means that the current logic can now act on it when that's succesful.
                         *
                         * Once the lock is acquired, the query is repeated (similar reasoning as when using synchronized methods).
                         * If the result is again zero, the process instance can be started.
                         *
                         * Transactionally, there are 4 transactions at play here:
                         * - tx 1 for locking the event subscription
                         * - tx 2 for doing the process instance count
                         * - tx 3 for starting the process instance (if tx 1 was successful and tx 2 returned 0)
                         * - tx 4 for unlocking (if tx 1 was successful)
                         *
                         * The counting + process instance starting happens exclusively for a given event subscription
                         * and due to using separate transactions for the count and the start, it's guaranteed
                         * other engine nodes or other threads will always see any other instance started.
                         */

                        boolean eventLocked = processEngineConfiguration.getManagementService().executeCommand(
                                new CommandConfig(false, TransactionPropagation.REQUIRES_NEW),
                                commandContext -> CommandContextUtil.getEventSubscriptionService(commandContext)
                                        .lockEventSubscription(eventSubscription.getId()));

                        if (eventLocked) {

                            try {

                                processInstanceCount = countProcessInstances(runtimeService, eventInstance, correlationKeyWithAllParameters, processDefinition);
                                if (processInstanceCount > 0) {
                                    // Returning, no new instance should be started
                                    eventConsumerInfo.setHasExistingInstancesForUniqueCorrelation(true);
                                    LOGGER.debug("Event received to start a new process instance, but a unique instance already exists.");
                                    return;
                                }

                                startProcessInstance(processInstanceBuilder, correlationKeyWithAllParameters.getValue(), ReferenceTypes.EVENT_PROCESS);
                                return;

                            } finally {
                                processEngineConfiguration.getManagementService().executeCommand(
                                        new CommandConfig(false, TransactionPropagation.REQUIRES_NEW), (Command<Void>) commandContext -> {
                                            CommandContextUtil.getEventSubscriptionService(commandContext)
                                                    .unlockEventSubscription(eventSubscription.getId());
                                            return null;
                                        });

                            }

                        } else {
                            return;

                        }

                    }

                }

            }

            startProcessInstance(processInstanceBuilder, null, null);

        }

    }

    protected long countProcessInstances(RuntimeService runtimeService, EventInstance eventInstance,
            CorrelationKey correlationKey, ProcessDefinition processDefinition) {

        ProcessInstanceQuery processInstanceQuery = runtimeService.createProcessInstanceQuery()
                .processDefinitionKey(processDefinition.getKey())
                .processInstanceReferenceId(correlationKey.getValue())
                .processInstanceReferenceType(ReferenceTypes.EVENT_PROCESS);

        if (eventInstance.getTenantId() != null && !Objects.equals(ProcessEngineConfiguration.NO_TENANT_ID, eventInstance.getTenantId())) {
            processInstanceQuery.processInstanceTenantId(eventInstance.getTenantId());
        }

        return processInstanceQuery.count();
    }

    protected void startProcessInstance(ProcessInstanceBuilder processInstanceBuilder, String referenceId, String referenceType) {

        if (referenceId != null) {
            processInstanceBuilder.referenceId(referenceId);
        }
        if (referenceType != null) {
            processInstanceBuilder.referenceType(referenceType);
        }

        if (processEngineConfiguration.isEventRegistryStartProcessInstanceAsync()) {
            processInstanceBuilder.startAsync();
        } else {
            processInstanceBuilder.start();
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
