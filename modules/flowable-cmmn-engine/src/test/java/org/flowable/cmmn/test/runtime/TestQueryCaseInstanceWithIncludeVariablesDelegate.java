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

import java.util.Collection;
import java.util.Map;

import org.flowable.cmmn.api.delegate.DelegatePlanItemInstance;
import org.flowable.cmmn.api.delegate.PlanItemJavaDelegate;
import org.flowable.cmmn.api.history.HistoricCaseInstance;
import org.flowable.cmmn.api.history.HistoricCaseInstanceQuery;
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.api.runtime.CaseInstanceQuery;
import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.cmmn.engine.test.impl.CmmnHistoryTestHelper;
import org.flowable.common.engine.impl.history.HistoryLevel;

public class TestQueryCaseInstanceWithIncludeVariablesDelegate implements PlanItemJavaDelegate {

    public static Map<String, Object> VARIABLES;

    public static Map<String, Object> HISTORIC_VARIABLES;

    public static void reset() {
        VARIABLES = null;
        HISTORIC_VARIABLES = null;
    }

    @Override
    public void execute(DelegatePlanItemInstance planItemInstance) {
        Object doNotSetVariable = planItemInstance.getTransientVariable("doNotSetVariable");
        if (doNotSetVariable == null || Boolean.FALSE.equals(doNotSetVariable)) {
            planItemInstance.setVariable("varFromTheServiceTask", "valueFromTheServiceTask");
        }

        CmmnEngineConfiguration cmmnEngineConfiguration = CommandContextUtil.getCmmnEngineConfiguration();

        Collection<String> queryVariableNames = (Collection<String>) planItemInstance.getTransientVariable("queryVariableNames");

        CaseInstanceQuery runtimeQuery = cmmnEngineConfiguration.getCmmnRuntimeService().createCaseInstanceQuery()
                .caseInstanceId(planItemInstance.getCaseInstanceId());
        if (queryVariableNames != null && !queryVariableNames.isEmpty()) {
            runtimeQuery.includeCaseVariables(queryVariableNames);
        } else {
            runtimeQuery.includeCaseVariables();
        }
        CaseInstance caseInstance = runtimeQuery
            .singleResult();
        VARIABLES = caseInstance.getCaseVariables();

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            HistoricCaseInstanceQuery historicQuery = cmmnEngineConfiguration.getCmmnHistoryService().createHistoricCaseInstanceQuery()
                    .caseInstanceId(planItemInstance.getCaseInstanceId());
            if (queryVariableNames != null && !queryVariableNames.isEmpty()) {
                historicQuery.includeCaseVariables(queryVariableNames);
            } else {
                historicQuery.includeCaseVariables();
            }
            HistoricCaseInstance historicCaseInstance = historicQuery
                .singleResult();
            HISTORIC_VARIABLES = historicCaseInstance.getCaseVariables();
        }

    }

}
