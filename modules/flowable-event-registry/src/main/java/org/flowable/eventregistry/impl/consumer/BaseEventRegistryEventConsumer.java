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
package org.flowable.eventregistry.impl.consumer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.FlowableIllegalStateException;
import org.flowable.common.engine.impl.AbstractEngineConfiguration;
import org.flowable.common.engine.impl.interceptor.CommandExecutor;
import org.flowable.common.engine.impl.interceptor.EngineConfigurationConstants;
import org.flowable.eventregistry.api.EventRegistry;
import org.flowable.eventregistry.api.EventRegistryEvent;
import org.flowable.eventregistry.api.EventRegistryEventConsumer;
import org.flowable.eventregistry.api.EventRegistryProcessingInfo;
import org.flowable.eventregistry.api.runtime.EventInstance;
import org.flowable.eventregistry.api.runtime.EventPayloadInstance;
import org.flowable.eventregistry.impl.EventRegistryEngineConfiguration;
import org.flowable.eventregistry.impl.util.CommandContextUtil;
import org.flowable.eventsubscription.api.EventSubscription;
import org.flowable.eventsubscription.api.EventSubscriptionQuery;

/**
 * @author Joram Barrez
 * @author Filip Hrisafov
 */
public abstract class BaseEventRegistryEventConsumer implements EventRegistryEventConsumer {

    protected AbstractEngineConfiguration engingeConfiguration;
    protected CommandExecutor commandExecutor;

    public BaseEventRegistryEventConsumer(AbstractEngineConfiguration engingeConfiguration) {
        this.engingeConfiguration = engingeConfiguration;
        this.commandExecutor = engingeConfiguration.getCommandExecutor();
    }

    @Override
    public EventRegistryProcessingInfo eventReceived(EventRegistryEvent event) {
        if (event.getEventObject() != null && event.getEventObject() instanceof EventInstance) {
            return eventReceived((EventInstance) event.getEventObject());
        } else {
            if (event.getEventObject() == null) {
                throw new FlowableIllegalArgumentException("No event object was passed to the consumer");
            } else {
                throw new FlowableIllegalArgumentException("Unsupported event object type: " + event.getEventObject().getClass());
            }
        }
    }

    protected abstract EventRegistryProcessingInfo eventReceived(EventInstance eventInstance);

    /**
     * Generates all possible correlation keys for the given correlation parameters.
     * The first element in the list will only have used one parameter. The last element in the list has included all parameters.
     */
    protected Collection<CorrelationKey> generateCorrelationKeys(Collection<EventPayloadInstance> correlationParameterInstances) {

        if (correlationParameterInstances.isEmpty()) {
            return Collections.emptySet();
        }

        int numberOfCorrelationParameters = correlationParameterInstances.size();
        if (numberOfCorrelationParameters == 1) {
            // size of 1 is handled in a special way in order to avoid creation of lists
            String correlationKey = generateCorrelationKey(correlationParameterInstances);
            return Collections.singleton(new CorrelationKey(correlationKey, correlationParameterInstances));
        }

        if (numberOfCorrelationParameters == 2) {
            // size of 2 is handled in a special way in order to avoid creation of additional lists
            Set<CorrelationKey> correlationKeys = new HashSet<>();

            String allParametersCorrelationKey = generateCorrelationKey(correlationParameterInstances);
            correlationKeys.add(new CorrelationKey(allParametersCorrelationKey, correlationParameterInstances));

            for (EventPayloadInstance correlationParameterInstance : correlationParameterInstances) {
                Set<EventPayloadInstance> singleParameterInstance = Collections.singleton(correlationParameterInstance);
                String correlationKey = generateCorrelationKey(singleParameterInstance);
                correlationKeys.add(new CorrelationKey(correlationKey, singleParameterInstance));
            }
            return correlationKeys;
        }

        // The correlation keys are the power set of the correlation parameter instances minus the empty set.
        // A power set is a set of all subsets including the empty set and the set itself.
        // A power set has a size of 2^n where n is the size of the set.
        // e.g. power set for [A, B] is [ [], [A], [B], [A, B] ]
        // We are going to compute the correlation keys by iterating from 1 to 2^n and the binary representation of every iteration index
        // will be used to determine if the index should be included in the set or not
        // We will start from 1 because we want to skip the empty set
        // This is an adaptation of https://simonhessner.de/calculate-power-set-set-of-all-subsets-in-python-without-recursion/
        List<EventPayloadInstance> list = new ArrayList<>(correlationParameterInstances);
        
        int correlationKeysSize = Math.toIntExact((long) Math.pow(2, list.size()));
        Collection<CorrelationKey> correlationKeys = new HashSet<>();

        for (int counter = 1; counter < correlationKeysSize; counter++) {
            Collection<EventPayloadInstance> subset = new ArrayList<>(list.size());
            for (int i = 0; i < list.size(); i++) {
                if ((counter & (1 << i)) > 0) {
                    // Here we check if the index should be included in the combination
                    subset.add(list.get(i));
                }
            }

            String correlationKey = generateCorrelationKey(subset);
            correlationKeys.add(new CorrelationKey(correlationKey, subset));
        }

        return correlationKeys;
    }

    protected String generateCorrelationKey(Collection<EventPayloadInstance> correlationParameterInstances) {
        Map<String, Object> data = new HashMap<>();
        for (EventPayloadInstance correlationParameterInstance : correlationParameterInstances) {
            data.put(correlationParameterInstance.getDefinitionName(), correlationParameterInstance.getValue());
        }

        return getEventRegistry().generateKey(data);
    }

    protected EventRegistry getEventRegistry() {
        EventRegistryEngineConfiguration eventRegistryEngineConfiguration = (EventRegistryEngineConfiguration) 
                        engingeConfiguration.getEngineConfigurations().get(EngineConfigurationConstants.KEY_EVENT_REGISTRY_CONFIG);
        return eventRegistryEngineConfiguration.getEventRegistry();
    }

    protected CorrelationKey getCorrelationKeyWithAllParameters(Collection<CorrelationKey> correlationKeys, EventInstance eventInstance) {
        CorrelationKey result = null;
        for (CorrelationKey correlationKey : correlationKeys) {
            if (result == null || (correlationKey.getParameterInstances().size() >= result.getParameterInstances().size()) ) {
                result = correlationKey;
            }
        }
        if (result == null) {
            throw new FlowableIllegalStateException(String.format("Event definition %s does not contain correlation parameters. Cannot verify if instance already exists.", eventInstance.getEventKey()));
        }
        return result;
    }

    protected List<EventSubscription> findEventSubscriptions(String scopeType, EventInstance eventInstance,  Collection<CorrelationKey> correlationKeys) {
        return commandExecutor.execute(commandContext -> {

            EventSubscriptionQuery eventSubscriptionQuery = createEventSubscriptionQuery()
                .eventType(eventInstance.getEventKey())
                .scopeType(scopeType);

            if (!correlationKeys.isEmpty()) {

                Set<String> allCorrelationKeyValues = correlationKeys.stream().map(CorrelationKey::getValue).collect(Collectors.toSet());

                eventSubscriptionQuery.or()
                    .withoutConfiguration()
                    .configurations(allCorrelationKeyValues)
                    .endOr();

            } else {
                eventSubscriptionQuery.withoutConfiguration();

            }

            String eventInstanceTenantId = eventInstance.getTenantId();
            if (eventInstanceTenantId != null && !AbstractEngineConfiguration.NO_TENANT_ID.equals(eventInstanceTenantId)) {

                EventRegistryEngineConfiguration eventRegistryConfiguration = CommandContextUtil.getEventRegistryConfiguration();

                if (eventRegistryConfiguration.isFallbackToDefaultTenant()) {
                    String defaultTenant = eventRegistryConfiguration.getDefaultTenantProvider()
                        .getDefaultTenant(eventInstance.getTenantId(), scopeType, eventInstance.getEventKey());

                    if (AbstractEngineConfiguration.NO_TENANT_ID.equals(defaultTenant)) {
                        eventSubscriptionQuery.or()
                            .tenantId(eventInstance.getTenantId())
                            .withoutTenantId()
                        .endOr();

                    } else {
                        eventSubscriptionQuery.tenantIds(Arrays.asList(eventInstanceTenantId, defaultTenant));

                    }

                } else {
                    eventSubscriptionQuery.tenantId(eventInstanceTenantId);

                }

            }

            return eventSubscriptionQuery.list();

        });
    }

    protected abstract EventSubscriptionQuery createEventSubscriptionQuery();

}
