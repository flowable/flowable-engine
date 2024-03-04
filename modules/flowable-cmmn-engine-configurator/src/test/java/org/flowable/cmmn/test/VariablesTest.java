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
package org.flowable.cmmn.test;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.engine.test.impl.CmmnHistoryTestHelper;
import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.engine.test.Deployment;
import org.flowable.variable.api.history.HistoricVariableInstance;
import org.junit.Test;

/**
 * @author Joram Barrez
 */
public class VariablesTest extends AbstractProcessEngineIntegrationTest {

    @Test
    @Deployment
    @CmmnDeployment
    public void testSettingAndRemovingVariableThroughCmmnRuntimeService() {
        processEngineRepositoryService.createDeployment()
                .addClasspathResource("org/flowable/cmmn/test/VariablesTest.testSettingAndRemovingVariableThroughCmmnRuntimeService.bpmn20.xml")
                .deploy();

        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("varSyncTestCase")
                .variable("loopNum", 100)
                .variable("round", 5)
                .start();

        // The case instance ending means all variable lookups succeeded
        assertCaseInstanceEnded(caseInstance);
        assertThat(cmmnRuntimeService.getVariables(caseInstance.getId())).isEmpty();

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            List<HistoricVariableInstance> historicVariables = cmmnHistoryService.createHistoricVariableInstanceQuery()
                    .caseInstanceId(caseInstance.getId()).list();

            // The variables are recreated each loop with a non-null value
            assertThat(historicVariables).hasSize(102); // 100 from the variables and 2 for round and loopNum
            for (HistoricVariableInstance historicVariable : historicVariables) {
                assertThat(historicVariable.getValue()).isNotNull();
            }
        }
    }

}
