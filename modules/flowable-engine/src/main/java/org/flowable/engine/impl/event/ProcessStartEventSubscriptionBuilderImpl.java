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

import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.engine.event.ProcessStartEventSubscriptionBuilder;
import org.flowable.engine.impl.RuntimeServiceImpl;

/**
 * A default implementation for the process start event subscription builder.
 *
 * @author Micha Kiener
 */
public class ProcessStartEventSubscriptionBuilderImpl implements ProcessStartEventSubscriptionBuilder {

    protected final RuntimeServiceImpl runtimeService;
    protected final String processDefinitionKey;
    protected final Map<String, Object> correlationParameterValues = new HashMap<>();

    public ProcessStartEventSubscriptionBuilderImpl(RuntimeServiceImpl runtimeService, String processDefinitionKey) {
        this.runtimeService = runtimeService;
        this.processDefinitionKey = processDefinitionKey;
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

    @Override
    public void registerProcessStartEventSubscription() {
        if (correlationParameterValues.isEmpty()) {
            throw new FlowableIllegalArgumentException(
                "At least one correlation parameter value must be provided for a dynamic process start event subscription, "
                    + "otherwise the process would get started on all events, regardless their correlation parameter values.");
        }

        runtimeService.registerProcessStartEventSubscription(this);
    }
}
