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
import org.flowable.cmmn.api.runtime.CaseInstanceStartEventSubscriptionDeletionBuilder;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;

/**
 * The implementation for a case start event subscription deletion builder.
 *
 * @author Micha Kiener
 */
public class CaseInstanceStartEventSubscriptionDeletionBuilderImpl implements CaseInstanceStartEventSubscriptionDeletionBuilder {
    
    protected final CmmnRuntimeServiceImpl cmmnRuntimeService;
    protected String caseDefinitionId;
    protected String tenantId;
    protected final Map<String, Object> correlationParameterValues = new HashMap<>();

    public CaseInstanceStartEventSubscriptionDeletionBuilderImpl(CmmnRuntimeServiceImpl runtimeService) {
        this.cmmnRuntimeService = runtimeService;
    }

    @Override
    public CaseInstanceStartEventSubscriptionDeletionBuilder caseDefinitionId(String caseDefinitionId) {
        this.caseDefinitionId = caseDefinitionId;
        return this;
    }
    
    @Override
    public CaseInstanceStartEventSubscriptionDeletionBuilder tenantId(String tenantId) {
        this.tenantId = tenantId;
        return this;
    }

    @Override
    public CaseInstanceStartEventSubscriptionDeletionBuilder addCorrelationParameterValue(String parameterName, Object parameterValue) {
        correlationParameterValues.put(parameterName, parameterValue);
        return this;
    }

    @Override
    public CaseInstanceStartEventSubscriptionDeletionBuilder addCorrelationParameterValues(Map<String, Object> parameters) {
        correlationParameterValues.putAll(parameters);
        return this;
    }

    public String getCaseDefinitionId() {
        return caseDefinitionId;
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
    public void deleteSubscriptions() {
        checkValidInformation();
        cmmnRuntimeService.deleteCaseInstanceStartEventSubscriptions(this);
    }

    protected void checkValidInformation() {
        if (StringUtils.isEmpty(caseDefinitionId)) {
            throw new FlowableIllegalArgumentException("The case definition must be provided using the exact id of the version the subscription was registered for.");
        }
    }
}
