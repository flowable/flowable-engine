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
package org.flowable.cmmn.test.runtime;

import org.flowable.cmmn.api.CmmnHistoryService;
import org.flowable.cmmn.api.CmmnRuntimeService;
import org.flowable.cmmn.api.delegate.DelegatePlanItemInstance;
import org.flowable.cmmn.api.delegate.PlanItemJavaDelegate;
import org.flowable.cmmn.api.history.HistoricCaseInstance;
import org.flowable.cmmn.api.history.HistoricPlanItemInstance;
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;

public class ServiceCacheTask implements PlanItemJavaDelegate {
    
    public static String caseInstanceId;
    public static String planItemInstanceId;
    public static String historicCaseInstanceId;
    public static String historicPlanItemInstanceId;

    public static void reset() {
        caseInstanceId = null;
        planItemInstanceId = null;
        historicCaseInstanceId = null;
        historicPlanItemInstanceId = null;
    }

    @Override
    public void execute(DelegatePlanItemInstance planItemInstance) {
        CmmnEngineConfiguration cmmnEngineConfiguration = CommandContextUtil.getCmmnEngineConfiguration();
        CmmnRuntimeService runtimeService = cmmnEngineConfiguration.getCmmnRuntimeService();
        CaseInstance caseInstance = runtimeService.createCaseInstanceQuery().caseInstanceId(planItemInstance.getCaseInstanceId()).singleResult();
        if (caseInstance != null && caseInstance.getId().equals(planItemInstance.getCaseInstanceId())) {
            caseInstanceId = caseInstance.getId();
        }
        
        PlanItemInstance queryPlanItemInstance = runtimeService.createPlanItemInstanceQuery().planItemInstanceId(planItemInstance.getId()).singleResult();
        if (queryPlanItemInstance != null && planItemInstance.getId().equals(queryPlanItemInstance.getId())) {
            planItemInstanceId = queryPlanItemInstance.getId();
        }
        
        CmmnHistoryService historyService = cmmnEngineConfiguration.getCmmnHistoryService();
        HistoricCaseInstance historicCaseInstance = historyService.createHistoricCaseInstanceQuery().caseInstanceId(planItemInstance.getCaseInstanceId()).singleResult();
        if (historicCaseInstance != null && historicCaseInstance.getId().equals(planItemInstance.getCaseInstanceId())) {
            historicCaseInstanceId = historicCaseInstance.getId();
        }
        
        HistoricPlanItemInstance historicPlanItemInstance = historyService.createHistoricPlanItemInstanceQuery().planItemInstanceId(planItemInstance.getId()).singleResult();
        if (historicPlanItemInstance != null && planItemInstance.getId().equals(historicPlanItemInstance.getId())) {
            historicPlanItemInstanceId = historicPlanItemInstance.getId();
        }
    }

}
