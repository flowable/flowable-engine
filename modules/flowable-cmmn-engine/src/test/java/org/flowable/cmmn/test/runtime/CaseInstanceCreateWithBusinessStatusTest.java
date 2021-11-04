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

import static org.assertj.core.api.Assertions.assertThat;

import org.flowable.cmmn.api.delegate.DelegatePlanItemInstance;
import org.flowable.cmmn.api.history.HistoricCaseInstance;
import org.flowable.cmmn.api.listener.PlanItemInstanceLifecycleListener;
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.engine.impl.persistence.entity.CaseInstanceEntity;
import org.flowable.cmmn.engine.impl.runtime.CaseInstanceBuilderImpl;
import org.flowable.cmmn.engine.impl.runtime.CaseInstanceHelper;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.engine.test.FlowableCmmnTestCase;
import org.flowable.cmmn.engine.test.impl.CmmnHistoryTestHelper;
import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.task.api.Task;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class CaseInstanceCreateWithBusinessStatusTest extends FlowableCmmnTestCase {

    private String deplId;

    @Before
    public void createCase() {
        org.flowable.cmmn.api.repository.CmmnDeployment deployment = cmmnRepositoryService.createDeployment()
                .addClasspathResource("org/flowable/cmmn/test/runtime/CaseInstanceCreateWithBusinessStatusTest.testCaseCreateWithBusinessStatus.cmmn")
                .addClasspathResource("org/flowable/cmmn/test/runtime/CaseInstanceCreateWithBusinessStatusTest.testCaseCreateWithoutBusinessStatus.cmmn")
                .deploy();

        deplId = deployment.getId();
    }

    @After
    public void deleteCase() {
        cmmnRepositoryService.deleteDeployment(deplId, true);
    }

    @Test
    @CmmnDeployment
    public void testCaseCreateWithBusinessStatus() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("businessKeyCase").businessStatus("businessStatus").start();
        assertThat(caseInstance.getBusinessStatus()).isEqualTo("businessStatus");
        Task task = cmmnTaskService.createTaskQuery().singleResult();
        cmmnTaskService.complete(task.getId());

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            HistoricCaseInstance historicCaseInstance = cmmnHistoryService.createHistoricCaseInstanceQuery().singleResult();
            assertThat(historicCaseInstance.getBusinessStatus()).isEqualTo("businessStatus");
        }
    }

    @Test
    @CmmnDeployment
    public void testCaseCreateWithoutBusinessStatus() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("businessKeyCaseWithout").start();
        assertThat(caseInstance.getBusinessStatus()).isNull();
        Task task = cmmnTaskService.createTaskQuery().singleResult();
        cmmnTaskService.complete(task.getId());

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            HistoricCaseInstance historicCaseInstance = cmmnHistoryService.createHistoricCaseInstanceQuery().singleResult();
            assertThat(historicCaseInstance.getBusinessStatus()).isNull();
        }

    }



    public static class UpdateBusinessKeyPlanItemJavaDelegate implements PlanItemInstanceLifecycleListener {

        private static final long serialVersionUID = 1L;

        @Override
        public String getSourceState() {
            return null;
        }

        @Override
        public String getTargetState() {
            return null;
        }

        @Override
        public void stateChanged(DelegatePlanItemInstance planItemInstance, String oldState, String newState) {
            CaseInstanceEntity caseInstanceEntity = CommandContextUtil.getCaseInstanceEntityManager().findById(planItemInstance.getCaseInstanceId());
            CommandContextUtil.getCaseInstanceEntityManager().updateCaseInstanceBusinessKey(caseInstanceEntity, "bzKey");
        }
    }

}
