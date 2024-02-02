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
package org.flowable.engine.impl.runtime;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.engine.impl.RuntimeServiceImpl;
import org.flowable.engine.runtime.ProcessInstanceStartEventSubscriptionBuilder;
import org.flowable.eventsubscription.api.EventSubscription;

/**
 * A default implementation for the process start event subscription builder.
 *
 * @author Micha Kiener
 */
public class ProcessInstanceStartEventSubscriptionBuilderImpl implements ProcessInstanceStartEventSubscriptionBuilder {
    protected final RuntimeServiceImpl runtimeService;
    protected String processDefinitionKey;
    protected String tenantId;
    protected final Map<String, Object> correlationParameterValues = new HashMap<>();
    protected boolean doNotUpdateToLatestVersionAutomatically;

    public ProcessInstanceStartEventSubscriptionBuilderImpl(RuntimeServiceImpl runtimeService) {
        this.runtimeService = runtimeService;
    }

    @Override
    public ProcessInstanceStartEventSubscriptionBuilder processDefinitionKey(String processDefinitionKey) {
        this.processDefinitionKey = processDefinitionKey;
        return this;
    }

    @Override
    public ProcessInstanceStartEventSubscriptionBuilder doNotUpdateToLatestVersionAutomatically() {
        this.doNotUpdateToLatestVersionAutomatically = true;
        return this;
    }

    @Override
    public ProcessInstanceStartEventSubscriptionBuilder addCorrelationParameterValue(String parameterName, Object parameterValue) {
        correlationParameterValues.put(parameterName, parameterValue);
        return this;
    }

    @Override
    public ProcessInstanceStartEventSubscriptionBuilder addCorrelationParameterValues(Map<String, Object> parameters) {
        correlationParameterValues.putAll(parameters);
        return this;
    }

    @Override
    public ProcessInstanceStartEventSubscriptionBuilder tenantId(String tenantId) {
        this.tenantId = tenantId;
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

    public String getTenantId() {
        return tenantId;
    }

    @Override
    public EventSubscription subscribe() {
        checkValidInformation();
        return runtimeService.registerProcessInstanceStartEventSubscription(this);
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
