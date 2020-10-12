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

import java.util.Map;

import org.flowable.cmmn.api.delegate.DelegatePlanItemInstance;
import org.flowable.cmmn.api.delegate.PlanItemJavaDelegate;
import org.flowable.cmmn.api.history.HistoricCaseInstance;
import org.flowable.cmmn.api.runtime.CaseInstance;
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
        planItemInstance.setVariable("varFromTheServiceTask", "valueFromTheServiceTask");

        CmmnEngineConfiguration cmmnEngineConfiguration = CommandContextUtil.getCmmnEngineConfiguration();

        CaseInstance caseInstance = cmmnEngineConfiguration.getCmmnRuntimeService().createCaseInstanceQuery()
            .caseInstanceId(planItemInstance.getCaseInstanceId())
            .includeCaseVariables()
            .singleResult();
        VARIABLES = caseInstance.getCaseVariables();

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            HistoricCaseInstance historicCaseInstance = cmmnEngineConfiguration.getCmmnHistoryService().createHistoricCaseInstanceQuery()
                .caseInstanceId(planItemInstance.getCaseInstanceId())
                .includeCaseVariables()
                .singleResult();
            HISTORIC_VARIABLES = historicCaseInstance.getCaseVariables();
        }

    }

}
