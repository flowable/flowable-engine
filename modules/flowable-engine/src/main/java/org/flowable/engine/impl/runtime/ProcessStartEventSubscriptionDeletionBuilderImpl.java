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
import org.flowable.engine.runtime.ProcessStartEventSubscriptionDeletionBuilder;

/**
 * The implementation for a process start event subscription deletion builder.
 *
 * @author Micha Kiener
 */
public class ProcessStartEventSubscriptionDeletionBuilderImpl implements ProcessStartEventSubscriptionDeletionBuilder {
protected final RuntimeServiceImpl runtimeService;
    protected String processDefinitionId;
    protected final Map<String, Object> correlationParameterValues = new HashMap<>();

    public ProcessStartEventSubscriptionDeletionBuilderImpl(RuntimeServiceImpl runtimeService) {
        this.runtimeService = runtimeService;
    }

    @Override
    public ProcessStartEventSubscriptionDeletionBuilder processDefinitionId(String processDefinitionId) {
        this.processDefinitionId = processDefinitionId;
        return this;
    }

    @Override
    public ProcessStartEventSubscriptionDeletionBuilder addCorrelationParameterValue(String parameterName, Object parameterValue) {
        correlationParameterValues.put(parameterName, parameterValue);
        return this;
    }

    @Override
    public ProcessStartEventSubscriptionDeletionBuilder addCorrelationParameterValues(Map<String, Object> parameters) {
        correlationParameterValues.putAll(parameters);
        return this;
    }

    public String getProcessDefinitionId() {
        return processDefinitionId;
    }

    public boolean hasCorrelationParameterValues() {
        return correlationParameterValues.size() > 0;
    }

    public Map<String, Object> getCorrelationParameterValues() {
        return correlationParameterValues;
    }

    @Override
    public void deleteSubscriptions() {
        checkValidInformation();
        runtimeService.deleteProcessStartEventSubscriptions(this);
    }

    protected void checkValidInformation() {
        if (StringUtils.isEmpty(processDefinitionId)) {
            throw new FlowableIllegalArgumentException("The process definition must be provided using the exact id of the version the subscription was registered for.");
        }
    }
}
