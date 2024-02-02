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
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.engine.test.FlowableCmmnTestCase;
import org.flowable.cmmn.engine.test.impl.CmmnHistoryTestHelper;
import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.task.api.Task;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class CaseInstanceUpdateBusinessKeyTest extends FlowableCmmnTestCase {

    private String deplId;

    @Before
    public void createCase() {
        org.flowable.cmmn.api.repository.CmmnDeployment deployment = cmmnRepositoryService.createDeployment()
                .addClasspathResource("org/flowable/cmmn/test/runtime/CaseInstanceUpdateBusinessKeyTest.testUpdateExistingCaseBusinessKey.cmmn")
                .deploy();

        deplId = deployment.getId();
    }

    @After
    public void deleteCase() {
        cmmnRepositoryService.deleteDeployment(deplId, true);
    }

    @Test
    @CmmnDeployment
    public void testCaseInstanceUpdateBusinessKey() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("businessKeyCase").start();
        assertThat(caseInstance.getBusinessKey()).isEqualTo("bzKey");

        Task task = cmmnTaskService.createTaskQuery().singleResult();
        cmmnTaskService.complete(task.getId());

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            HistoricCaseInstance historicCaseInstance = cmmnHistoryService.createHistoricCaseInstanceQuery().singleResult();
            assertThat(historicCaseInstance.getBusinessKey()).isEqualTo("bzKey");
        }
    }

    @Test
    @CmmnDeployment
    public void testUpdateExistingCaseBusinessKey() {
        cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("businessKeyCase").businessKey("bzKey").start();

        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceQuery().singleResult();
        assertThat(caseInstance.getBusinessKey()).isEqualTo("bzKey");

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            HistoricCaseInstance historicCaseInstance = cmmnHistoryService.createHistoricCaseInstanceQuery().singleResult();
            assertThat(historicCaseInstance.getBusinessKey()).isEqualTo("bzKey");
        }

        cmmnRuntimeService.updateBusinessKey(caseInstance.getId(), "newKey");

        caseInstance = cmmnRuntimeService.createCaseInstanceQuery().singleResult();
        assertThat(caseInstance.getBusinessKey()).isEqualTo("newKey");

        Task task = cmmnTaskService.createTaskQuery().singleResult();
        cmmnTaskService.complete(task.getId());
        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            HistoricCaseInstance historicCaseInstance2 = cmmnHistoryService.createHistoricCaseInstanceQuery().singleResult();
            assertThat(historicCaseInstance2.getBusinessKey()).isEqualTo("newKey");
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
