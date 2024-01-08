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
package org.flowable.cmmn.engine.impl.runtime;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.flowable.cmmn.api.runtime.CaseInstanceStartEventSubscriptionBuilder;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.eventsubscription.api.EventSubscription;

/**
 * A default implementation for the case start event subscription builder.
 *
 * @author Micha Kiener
 */
public class CaseInstanceStartEventSubscriptionBuilderImpl implements CaseInstanceStartEventSubscriptionBuilder {
    protected final CmmnRuntimeServiceImpl cmmnRuntimeService;
    protected String caseDefinitionKey;
    protected String tenantId;
    protected final Map<String, Object> correlationParameterValues = new HashMap<>();
    protected boolean doNotUpdateToLatestVersionAutomatically;

    public CaseInstanceStartEventSubscriptionBuilderImpl(CmmnRuntimeServiceImpl cmmnRuntimeService) {
        this.cmmnRuntimeService = cmmnRuntimeService;
    }

    @Override
    public CaseInstanceStartEventSubscriptionBuilder caseDefinitionKey(String caseDefinitionKey) {
        this.caseDefinitionKey = caseDefinitionKey;
        return this;
    }

    @Override
    public CaseInstanceStartEventSubscriptionBuilder doNotUpdateToLatestVersionAutomatically() {
        this.doNotUpdateToLatestVersionAutomatically = true;
        return this;
    }

    @Override
    public CaseInstanceStartEventSubscriptionBuilder addCorrelationParameterValue(String parameterName, Object parameterValue) {
        correlationParameterValues.put(parameterName, parameterValue);
        return this;
    }

    @Override
    public CaseInstanceStartEventSubscriptionBuilder addCorrelationParameterValues(Map<String, Object> parameters) {
        correlationParameterValues.putAll(parameters);
        return this;
    }

    @Override
    public CaseInstanceStartEventSubscriptionBuilder tenantId(String tenantId) {
        this.tenantId = tenantId;
        return this;
    }

    public String getCaseDefinitionKey() {
        return caseDefinitionKey;
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
        return cmmnRuntimeService.registerCaseInstanceStartEventSubscription(this);
    }

    protected void checkValidInformation() {
        if (StringUtils.isEmpty(caseDefinitionKey)) {
            throw new FlowableIllegalArgumentException("The case definition must be provided using the key for the subscription to be registered.");
        }

        if (correlationParameterValues.isEmpty()) {
            throw new FlowableIllegalArgumentException(
                "At least one correlation parameter value must be provided for a dynamic case start event subscription, "
                    + "otherwise the case would get started on all events, regardless their correlation parameter values.");
        }
    }
}
