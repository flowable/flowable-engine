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
import org.flowable.engine.runtime.ProcessInstanceStartEventSubscriptionModificationBuilder;

/**
 * The implementation for a process start event subscription modification builder.
 *
 * @author Micha Kiener
 */
public class ProcessInstanceStartEventSubscriptionModificationBuilderImpl implements ProcessInstanceStartEventSubscriptionModificationBuilder {
    protected final RuntimeServiceImpl runtimeService;
    protected String processDefinitionId;
    protected String newProcessDefinitionId;
    protected String tenantId;
    protected final Map<String, Object> correlationParameterValues = new HashMap<>();

    public ProcessInstanceStartEventSubscriptionModificationBuilderImpl(RuntimeServiceImpl runtimeService) {
        this.runtimeService = runtimeService;
    }

    @Override
    public ProcessInstanceStartEventSubscriptionModificationBuilder processDefinitionId(String processDefinitionId) {
        this.processDefinitionId = processDefinitionId;
        return this;
    }
    
    @Override
    public ProcessInstanceStartEventSubscriptionModificationBuilder tenantId(String tenantId) {
        this.tenantId = tenantId;
        return this;
    }

    @Override
    public ProcessInstanceStartEventSubscriptionModificationBuilder addCorrelationParameterValue(String parameterName, Object parameterValue) {
        correlationParameterValues.put(parameterName, parameterValue);
        return this;
    }

    @Override
    public ProcessInstanceStartEventSubscriptionModificationBuilder addCorrelationParameterValues(Map<String, Object> parameters) {
        correlationParameterValues.putAll(parameters);
        return this;
    }

    public String getProcessDefinitionId() {
        return processDefinitionId;
    }

    public boolean hasNewProcessDefinitionId() {
        return StringUtils.isNotBlank(newProcessDefinitionId);
    }

    public String getNewProcessDefinitionId() {
        return newProcessDefinitionId;
    }
    
    public String getTenantId() {
        return tenantId;
    }

    public boolean hasCorrelationParameterValues() {
        return correlationParameterValues.size() > 0;
    }

    public Map<String, Object> getCorrelationParameterValues() {
        return correlationParameterValues;
    }

    @Override
    public void migrateToLatestProcessDefinition() {
        checkValidInformation();
        runtimeService.migrateProcessInstanceStartEventSubscriptionsToProcessDefinitionVersion(this);
    }

    @Override
    public void migrateToProcessDefinition(String processDefinitionId) {
        this.newProcessDefinitionId = processDefinitionId;
        checkValidInformation();
        runtimeService.migrateProcessInstanceStartEventSubscriptionsToProcessDefinitionVersion(this);
    }

    protected void checkValidInformation() {
        if (StringUtils.isEmpty(processDefinitionId)) {
            throw new FlowableIllegalArgumentException("The process definition must be provided using the exact id of the version the subscription was registered for.");
        }
    }
}
