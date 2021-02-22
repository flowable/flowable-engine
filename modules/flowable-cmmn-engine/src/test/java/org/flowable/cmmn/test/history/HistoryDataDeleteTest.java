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
package org.flowable.cmmn.test.history;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

import org.flowable.cmmn.api.history.HistoricCaseInstanceQuery;
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.engine.test.FlowableCmmnTestCase;
import org.flowable.cmmn.engine.test.impl.CmmnHistoryTestHelper;
import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.task.api.Task;
import org.junit.Test;

/**
 * @author Tijs Rademakers
 */
public class HistoryDataDeleteTest extends FlowableCmmnTestCase {

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/one-human-task-model.cmmn")
    public void testDeleteSingleHistoricCaseInstance() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("oneTaskCase").start();
        cmmnRuntimeService.setVariable(caseInstance.getId(), "testVar", "testValue");
        cmmnRuntimeService.setVariable(caseInstance.getId(), "numVar", 43);

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.NONE, cmmnEngineConfiguration)) {
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceId(caseInstance.getId()).count()).isEqualTo(1);
        }

        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        cmmnTaskService.setVariableLocal(task.getId(), "taskVar", "taskValue");
        cmmnTaskService.complete(task.getId());

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.NONE, cmmnEngineConfiguration)) {
            HistoricCaseInstanceQuery query = cmmnHistoryService.createHistoricCaseInstanceQuery();
            Calendar cal = new GregorianCalendar();
            cal.set(Calendar.YEAR, cal.get(Calendar.YEAR) + 1);
            query.finishedBefore(cal.getTime());

            query.deleteWithRelatedData();

            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceId(caseInstance.getId()).count()).isZero();
            assertThat(cmmnHistoryService.createHistoricPlanItemInstanceQuery().planItemInstanceCaseInstanceId(caseInstance.getId()).count()).isZero();
            assertThat(cmmnHistoryService.createHistoricTaskInstanceQuery().caseInstanceId(caseInstance.getId()).count()).isZero();
            assertThat(cmmnHistoryService.getHistoricIdentityLinksForCaseInstance(caseInstance.getId())).isEmpty();
            assertThat(cmmnHistoryService.getHistoricEntityLinkChildrenForCaseInstance(caseInstance.getId())).isEmpty();
            assertThat(cmmnHistoryService.createHistoricTaskLogEntryQuery().caseInstanceId(caseInstance.getId()).count()).isZero();
            assertThat(cmmnHistoryService.createHistoricVariableInstanceQuery().caseInstanceId(caseInstance.getId()).count()).isZero();
        }
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/human-task-milestone-model.cmmn")
    public void testDeleteHistoricInstancesWithFinishedBefore() {
        List<String> caseInstanceIds = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("oneTaskCase").start();
            caseInstanceIds.add(caseInstance.getId());
            cmmnRuntimeService.setVariable(caseInstance.getId(), "testVar", "testValue" + (i + 1));
            cmmnRuntimeService.setVariable(caseInstance.getId(), "numVar", (i + 1));
        }

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.NONE, cmmnEngineConfiguration)) {

            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().count()).isEqualTo(20);

        }

        for (int i = 0; i < 10; i++) {
            Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstanceIds.get(i)).singleResult();
            cmmnTaskService.setVariableLocal(task.getId(), "taskVar", "taskValue" + (i + 1));
            cmmnTaskService.complete(task.getId());
        }

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.NONE, cmmnEngineConfiguration)) {
            HistoricCaseInstanceQuery query = cmmnHistoryService.createHistoricCaseInstanceQuery();
            Calendar cal = new GregorianCalendar();
            cal.set(Calendar.YEAR, cal.get(Calendar.YEAR) + 1);
            query.finishedBefore(cal.getTime());
            query.deleteWithRelatedData();

            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().count()).isEqualTo(10);
            assertThat(cmmnHistoryService.createHistoricPlanItemInstanceQuery().count()).isEqualTo(20);
            assertThat(cmmnHistoryService.createHistoricTaskInstanceQuery().count()).isEqualTo(10);

            for (int i = 0; i < 20; i++) {
                if (i < 10) {
                    assertThat(cmmnHistoryService.getHistoricIdentityLinksForCaseInstance(caseInstanceIds.get(i))).isEmpty();
                    assertThat(cmmnHistoryService.createHistoricTaskLogEntryQuery().caseInstanceId(caseInstanceIds.get(i)).count()).isZero();
                    assertThat(cmmnHistoryService.createHistoricVariableInstanceQuery().caseInstanceId(caseInstanceIds.get(i)).count()).isZero();
                    assertThat(cmmnHistoryService.createHistoricMilestoneInstanceQuery().milestoneInstanceCaseInstanceId(caseInstanceIds.get(i)).count())
                            .isZero();
                } else {
                    assertThat(cmmnHistoryService.getHistoricIdentityLinksForCaseInstance(caseInstanceIds.get(i))).hasSize(1);
                    assertThat(cmmnHistoryService.createHistoricTaskLogEntryQuery().caseInstanceId(caseInstanceIds.get(i)).count()).isEqualTo(1);
                    assertThat(cmmnHistoryService.createHistoricVariableInstanceQuery().caseInstanceId(caseInstanceIds.get(i)).count()).isEqualTo(2);
                    assertThat(cmmnHistoryService.createHistoricMilestoneInstanceQuery().milestoneInstanceCaseInstanceId(caseInstanceIds.get(i)).count())
                            .isEqualTo(1);
                }
            }
        }
    }
    
    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/human-task-milestone-model.cmmn")
    public void testDeleteHistoricInstances() {
        List<String> caseInstanceIds = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("oneTaskCase").start();
            caseInstanceIds.add(caseInstance.getId());
            cmmnRuntimeService.setVariable(caseInstance.getId(), "testVar", "testValue" + (i + 1));
            cmmnRuntimeService.setVariable(caseInstance.getId(), "numVar", (i + 1));
        }

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.NONE, cmmnEngineConfiguration)) {

            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().count()).isEqualTo(20);

        }

        for (int i = 0; i < 10; i++) {
            Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstanceIds.get(i)).singleResult();
            cmmnTaskService.setVariableLocal(task.getId(), "taskVar", "taskValue" + (i + 1));
            cmmnTaskService.complete(task.getId());
        }

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.NONE, cmmnEngineConfiguration)) {
            HistoricCaseInstanceQuery query = cmmnHistoryService.createHistoricCaseInstanceQuery();
            query.activePlanItemDefinitionId("noneexisting");
            query.deleteWithRelatedData();

            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().count()).isEqualTo(20);
            assertThat(cmmnHistoryService.createHistoricPlanItemInstanceQuery().count()).isEqualTo(40);
            assertThat(cmmnHistoryService.createHistoricTaskInstanceQuery().count()).isEqualTo(20);
        }
    }
}
