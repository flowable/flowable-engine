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

import org.flowable.cmmn.api.CallbackTypes;
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.api.runtime.CaseInstanceBuilder;
import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.engine.impl.cmmn.CaseInstanceService;

/**
 * @author Tijs Rademakers
 */
public class DefaultCaseInstanceService implements CaseInstanceService {
    
    protected CmmnEngineConfiguration cmmnEngineConfiguration;

    public DefaultCaseInstanceService(CmmnEngineConfiguration cmmnEngineConfiguration) {
        this.cmmnEngineConfiguration = cmmnEngineConfiguration;
    }
    

    @Override
    public String startCaseInstanceByKey(String caseDefinitionKey, String caseInstanceName, String businessKey, String executionId, 
                    String tenantId, boolean fallbackToDefaultTenant, Map<String, Object> inParametersMap) {
        
        CaseInstanceBuilder caseInstanceBuilder = cmmnEngineConfiguration.getCmmnRuntimeService().createCaseInstanceBuilder();
        caseInstanceBuilder.caseDefinitionKey(caseDefinitionKey);
        
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
        
        CaseInstance caseInstance = caseInstanceBuilder.start();
        return caseInstance.getId();
    }


    @Override
    public void deleteCaseInstance(String caseInstanceId) {
        cmmnEngineConfiguration.getCmmnRuntimeService().terminateCaseInstance(caseInstanceId);
    }

}
