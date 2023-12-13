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
package org.flowable.engine.impl.event;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.engine.event.ProcessStartEventSubscriptionBuilder;
import org.flowable.engine.impl.RuntimeServiceImpl;
import org.flowable.eventsubscription.api.EventSubscription;

/**
 * A default implementation for the process start event subscription builder.
 *
 * @author Micha Kiener
 */
public class ProcessStartEventSubscriptionBuilderImpl implements ProcessStartEventSubscriptionBuilder {
    protected final RuntimeServiceImpl runtimeService;
    protected String processDefinitionKey;
    protected final Map<String, Object> correlationParameterValues = new HashMap<>();
    protected boolean doNotUpdateToLatestVersionAutomatically;

    public ProcessStartEventSubscriptionBuilderImpl(RuntimeServiceImpl runtimeService) {
        this.runtimeService = runtimeService;
    }

    @Override
    public ProcessStartEventSubscriptionBuilder processDefinitionKey(String processDefinitionKey) {
        this.processDefinitionKey = processDefinitionKey;
        return this;
    }

    @Override
    public ProcessStartEventSubscriptionBuilder doNotUpdateToLatestVersionAutomatically() {
        this.doNotUpdateToLatestVersionAutomatically = true;
        return this;
    }

    @Override
    public ProcessStartEventSubscriptionBuilder addCorrelationParameterValue(String parameterName, Object parameterValue) {
        correlationParameterValues.put(parameterName, parameterValue);
        return this;
    }

    @Override
    public ProcessStartEventSubscriptionBuilder addCorrelationParameterValues(Map<String, Object> parameters) {
        correlationParameterValues.putAll(parameters);
        return this;
    }

    public String getProcessDefinitionKey() {
        return processDefinitionKey;
    }

    public Map<String, Object> getCorrelationParameterValues() {
        return correlationParameterValues;
    }

    public boolean isDoNotUpdateToLatestVersionAutomatically() {
        return doNotUpdateToLatestVersionAutomatically;
    }

    @Override
    public EventSubscription registerProcessStartEventSubscription() {
        checkValidInformation();
        return runtimeService.registerProcessStartEventSubscription(this);
    }

    protected void checkValidInformation() {
        if (StringUtils.isEmpty(processDefinitionKey)) {
            throw new FlowableIllegalArgumentException("The process definition must be provided using the key for the subscription to be registered.");
        }

        if (correlationParameterValues.isEmpty()) {
            throw new FlowableIllegalArgumentException(
                "At least one correlation parameter value must be provided for a dynamic process start event subscription, "
                    + "otherwise the process would get started on all events, regardless their correlation parameter values.");
        }
    }
}
