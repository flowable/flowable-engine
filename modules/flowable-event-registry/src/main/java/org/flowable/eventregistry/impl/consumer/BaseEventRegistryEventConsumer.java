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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.flowable.common.engine.impl.AbstractEngineConfiguration;
import org.flowable.common.engine.impl.interceptor.EngineConfigurationConstants;
import org.flowable.eventregistry.api.EventRegistry;
import org.flowable.eventregistry.api.EventRegistryEvent;
import org.flowable.eventregistry.api.EventRegistryEventConsumer;
import org.flowable.eventregistry.api.runtime.EventCorrelationParameterInstance;
import org.flowable.eventregistry.api.runtime.EventInstance;
import org.flowable.eventregistry.impl.EventRegistryEngineConfiguration;

/**
 * @author Joram Barrez
 * @author Filip Hrisafov
 */
public abstract class BaseEventRegistryEventConsumer implements EventRegistryEventConsumer {

    protected AbstractEngineConfiguration engingeConfiguration;

    public BaseEventRegistryEventConsumer(AbstractEngineConfiguration engingeConfiguration) {
        this.engingeConfiguration = engingeConfiguration;
    }

    @Override
    public void eventReceived(EventRegistryEvent event) {
        if (event.getEventObject() != null && event.getEventObject() instanceof EventInstance) {
            eventReceived((EventInstance) event.getEventObject());
        } else {
            // TODO: what should happen in this case?
        }
    }

    protected abstract void eventReceived(EventInstance eventInstance);

    /**
     * Generates all possible correlation keys for the given correlation parameters.
     * The first element in the list will only have used one parameter. The last element in the list has included all parameters.
     */
    protected Collection<CorrelationKey> generateCorrelationKeys(Collection<EventCorrelationParameterInstance> correlationParameterInstances) {

        if (correlationParameterInstances.isEmpty()) {
            return Collections.emptySet();
        }

        List<EventCorrelationParameterInstance> list = new ArrayList<>(correlationParameterInstances);
        Collection<CorrelationKey> correlationKeys = new ArrayList<>();
        for (int i = 1; i <= list.size(); i++) {
            for (int j = 0; j <= list.size() - i; j++) {
                List<EventCorrelationParameterInstance> parameterSubList = list.subList(j, j + i);
                String correlationKey = generateCorrelationKey(parameterSubList);

                if (correlationKeys.stream().noneMatch(c -> Objects.equals(c.getValue(), correlationKey))) {
                    correlationKeys.add(new CorrelationKey(correlationKey, parameterSubList));
                }
            }
        }

        return correlationKeys;
    }

    protected String generateCorrelationKey(Collection<EventCorrelationParameterInstance> correlationParameterInstances) {
        Map<String, Object> data = new HashMap<>();
        for (EventCorrelationParameterInstance correlationParameterInstance : correlationParameterInstances) {
            data.put(correlationParameterInstance.getDefinitionName(), correlationParameterInstance.getValue());
        }

        return getEventRegistry().generateKey(data);
    }

    protected EventRegistry getEventRegistry() {
        EventRegistryEngineConfiguration eventRegistryEngineConfiguration = (EventRegistryEngineConfiguration) 
                        engingeConfiguration.getEngineConfigurations().get(EngineConfigurationConstants.KEY_EVENT_REGISTRY_CONFIG);
        return eventRegistryEngineConfiguration.getEventRegistry();
    }
}
