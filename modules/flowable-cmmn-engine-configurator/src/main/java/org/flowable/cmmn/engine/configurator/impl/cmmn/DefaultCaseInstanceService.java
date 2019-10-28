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
package org.flowable.cmmn.engine.configurator.impl.cmmn;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.flowable.cmmn.api.CallbackTypes;
import org.flowable.cmmn.api.CmmnRuntimeService;
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.api.runtime.CaseInstanceBuilder;
import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.impl.persistence.entity.CmmnEngineEntityConstants;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.engine.impl.cmmn.CaseInstanceService;
import org.flowable.eventsubscription.service.impl.persistence.entity.EventSubscriptionEntity;

/**
 * @author Tijs Rademakers
 * @author Valentin Zickner
 */
public class DefaultCaseInstanceService implements CaseInstanceService {
    
    protected CmmnEngineConfiguration cmmnEngineConfiguration;

    public DefaultCaseInstanceService(CmmnEngineConfiguration cmmnEngineConfiguration) {
        this.cmmnEngineConfiguration = cmmnEngineConfiguration;
    }
    
    @Override
    public String generateNewCaseInstanceId() {
        if (cmmnEngineConfiguration.isUsePrefixId()) {
            return CmmnEngineEntityConstants.CMMN_ENGINE_ID_PREFIX + cmmnEngineConfiguration.getIdGenerator().getNextId();
        } else {
            return cmmnEngineConfiguration.getIdGenerator().getNextId();
        }
    }

    @Override
    public String startCaseInstanceByKey(String caseDefinitionKey, String predefinedCaseInstanceId, String caseInstanceName, String businessKey, String executionId, 
                    String tenantId, boolean fallbackToDefaultTenant, Map<String, Object> inParametersMap) {
        
        CaseInstanceBuilder caseInstanceBuilder = cmmnEngineConfiguration.getCmmnRuntimeService().createCaseInstanceBuilder();
        caseInstanceBuilder.caseDefinitionKey(caseDefinitionKey);
        
        if (predefinedCaseInstanceId != null) {
            caseInstanceBuilder.predefinedCaseInstanceId(predefinedCaseInstanceId);
        }
        
        if (tenantId != null) {
            caseInstanceBuilder.tenantId(tenantId);
        }
        
        if (executionId != null) {
            caseInstanceBuilder.callbackId(executionId);
            caseInstanceBuilder.callbackType(CallbackTypes.EXECUTION_CHILD_CASE);
        }

        for (String target : inParametersMap.keySet()) {
            caseInstanceBuilder.variable(target, inParametersMap.get(target));
        }

        if (fallbackToDefaultTenant) {
            caseInstanceBuilder.fallbackToDefaultTenant();
        }

        if (businessKey != null) {
            caseInstanceBuilder.businessKey(businessKey);
        }

        if (caseInstanceName != null) {
            caseInstanceBuilder.name(caseInstanceName);
        }
        
        CaseInstance caseInstance = caseInstanceBuilder.start();
        return caseInstance.getId();
    }

    @Override
    public void handleSignalEvent(EventSubscriptionEntity eventSubscription) {
        if (StringUtils.isEmpty(eventSubscription.getSubScopeId())) {
            throw new FlowableException("Plan item instance for event subscription can not be found with empty sub scope id value");
        }
        
        CmmnRuntimeService cmmnRuntimeService = cmmnEngineConfiguration.getCmmnRuntimeService();
        PlanItemInstance planItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery()
                        .planItemInstanceId(eventSubscription.getSubScopeId())
                        .singleResult();
        
        if (planItemInstance == null) {
            throw new FlowableException("Plan item instance for event subscription can not be found with sub scope id " + eventSubscription.getSubScopeId());
        }
        
        cmmnRuntimeService.triggerPlanItemInstance(planItemInstance.getId());
    }

    @Override
    public void deleteCaseInstance(String caseInstanceId) {
        cmmnEngineConfiguration.getCmmnRuntimeService().terminateCaseInstance(caseInstanceId);
    }

    @Override
    public Set<String> findChildCaseIdsForExecutionId(String executionId) {
        CmmnRuntimeService cmmnRuntimeService = cmmnEngineConfiguration.getCmmnRuntimeService();
        return cmmnRuntimeService.createCaseInstanceQuery()
                .caseInstanceCallbackType(CallbackTypes.EXECUTION_CHILD_CASE)
                .caseInstanceCallbackId(executionId)
                .list()
                .stream()
                .map(CaseInstance::getId)
                .collect(Collectors.toSet());
    }
}
