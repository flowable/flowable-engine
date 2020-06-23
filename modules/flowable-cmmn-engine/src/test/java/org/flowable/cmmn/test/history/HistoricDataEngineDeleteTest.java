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
import java.util.List;

import org.flowable.cmmn.api.CmmnHistoryService;
import org.flowable.cmmn.api.CmmnManagementService;
import org.flowable.cmmn.api.CmmnRuntimeService;
import org.flowable.cmmn.api.CmmnTaskService;
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.impl.job.CmmnHistoryCleanupJobHandler;
import org.flowable.cmmn.engine.test.CmmnConfigurationResource;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.engine.test.FlowableCmmnTest;
import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.common.engine.impl.runtime.Clock;
import org.flowable.job.api.Job;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.Test;

@FlowableCmmnTest
@CmmnConfigurationResource("flowable.historyclean.cmmn.cfg.xml")
public class HistoricDataEngineDeleteTest {

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/human-task-milestone-model.cmmn")
    public void testHistoryCleanupTimerJob(CmmnEngineConfiguration cmmnEngineConfiguration, CmmnRuntimeService cmmnRuntimeService,
            CmmnHistoryService cmmnHistoryService, CmmnTaskService cmmnTaskService, CmmnManagementService cmmnManagementService) {

        try {
            Clock clock = cmmnEngineConfiguration.getClock();
            Calendar cal = clock.getCurrentCalendar();
            cal.add(Calendar.DAY_OF_YEAR, -400);
            clock.setCurrentCalendar(cal);

            List<String> caseInstanceIds = new ArrayList<>();
            for (int i = 0; i < 20; i++) {
                CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("oneTaskCase").start();
                caseInstanceIds.add(caseInstance.getId());
                cmmnRuntimeService.setVariable(caseInstance.getId(), "testVar", "testValue" + (i + 1));
                cmmnRuntimeService.setVariable(caseInstance.getId(), "numVar", (i + 1));
            }

            if (cmmnEngineConfiguration.getHistoryLevel() != HistoryLevel.NONE) {

                assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().count()).isEqualTo(20);

                for (int i = 0; i < 10; i++) {
                    Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstanceIds.get(i)).singleResult();
                    cmmnTaskService.setVariableLocal(task.getId(), "taskVar", "taskValue" + (i + 1));
                    cmmnTaskService.complete(task.getId());
                }

                assertThat(cmmnManagementService.createTimerJobQuery().handlerType(CmmnHistoryCleanupJobHandler.TYPE).count()).isEqualTo(1);

                Job executableJob = cmmnManagementService.moveTimerToExecutableJob(
                        cmmnManagementService.createTimerJobQuery().handlerType(CmmnHistoryCleanupJobHandler.TYPE).singleResult().getId());
                cmmnManagementService.executeJob(executableJob.getId());

                assertThat(cmmnManagementService.createTimerJobQuery().handlerType(CmmnHistoryCleanupJobHandler.TYPE).count()).isEqualTo(1);

                assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().count()).isEqualTo(10);
                assertThat(cmmnHistoryService.createHistoricPlanItemInstanceQuery().count()).isEqualTo(20);
                assertThat(cmmnHistoryService.createHistoricTaskInstanceQuery().count()).isEqualTo(10);

                for (int i = 0; i < 20; i++) {
                    if (i < 10) {
                        assertThat(cmmnHistoryService.getHistoricIdentityLinksForCaseInstance(caseInstanceIds.get(i))).isEmpty();
                        assertThat(cmmnHistoryService.createHistoricTaskLogEntryQuery().caseInstanceId(caseInstanceIds.get(i)).count()).isZero();
                        assertThat(cmmnHistoryService.createHistoricVariableInstanceQuery().caseInstanceId(caseInstanceIds.get(i)).count()).isZero();
                        assertThat(cmmnHistoryService.createHistoricMilestoneInstanceQuery().milestoneInstanceCaseInstanceId(caseInstanceIds.get(i)).count()).isZero();

                    } else {
                        assertThat(cmmnHistoryService.getHistoricIdentityLinksForCaseInstance(caseInstanceIds.get(i))).hasSize(1);
                        assertThat(cmmnHistoryService.createHistoricTaskLogEntryQuery().caseInstanceId(caseInstanceIds.get(i)).count()).isEqualTo(1);
                        assertThat(cmmnHistoryService.createHistoricVariableInstanceQuery().caseInstanceId(caseInstanceIds.get(i)).count()).isEqualTo(2);
                        assertThat(cmmnHistoryService.createHistoricMilestoneInstanceQuery().milestoneInstanceCaseInstanceId(caseInstanceIds.get(i)).count())
                                .isEqualTo(1);
                    }
                }

                cmmnManagementService
                        .deleteTimerJob(cmmnManagementService.createTimerJobQuery().handlerType(CmmnHistoryCleanupJobHandler.TYPE).singleResult().getId());
            }

        } finally {
            cmmnEngineConfiguration.resetClock();
        }
    }
}