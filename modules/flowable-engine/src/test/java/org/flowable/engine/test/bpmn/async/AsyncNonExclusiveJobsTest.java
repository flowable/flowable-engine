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
package org.flowable.engine.test.bpmn.async;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.impl.test.HistoryTestHelper;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.test.Deployment;
import org.flowable.variable.api.history.HistoricVariableInstance;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfSystemProperty;

class AsyncNonExclusiveJobsTest extends PluggableFlowableTestCase {

    @Test
    @Deployment
    @DisabledIfSystemProperty(named = "disableWhen", matches = "cockroachdb")
    void testFiveNonExclusiveParallelJobs() {

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {

            // The process has five service tasks in parallel, all non exclusive.
            // They should be executed with at least 3 seconds in between (as they both sleep for 3 seconds)
            runtimeService.startProcessInstanceByKey("testNonExclusiveJobs");
            // There should be 5 jobs
            assertThat(managementService.createJobQuery().count()).isEqualTo(5);
            waitForJobExecutorToProcessAllJobsAndExecutableTimerJobs(20000L, 500L);

            // There should be no jobs
            assertThat(managementService.createJobQuery().count()).isEqualTo(0);
            assertThat(managementService.createTimerJobQuery().count()).isEqualTo(0);
            assertThat(managementService.createDeadLetterJobQuery().count()).isEqualTo(0);

            waitForHistoryJobExecutorToProcessAllJobs(7000, 100);

            HistoricActivityInstance serviceTask1Instance = historyService.createHistoricActivityInstanceQuery().activityId("serviceTaskSleep1").singleResult();
            HistoricActivityInstance serviceTask2Instance = historyService.createHistoricActivityInstanceQuery().activityId("serviceTaskSleep2").singleResult();
            HistoricActivityInstance serviceTask3Instance = historyService.createHistoricActivityInstanceQuery().activityId("serviceTaskSleep3").singleResult();
            HistoricActivityInstance serviceTask4Instance = historyService.createHistoricActivityInstanceQuery().activityId("serviceTaskSleep4").singleResult();
            HistoricActivityInstance serviceTask5Instance = historyService.createHistoricActivityInstanceQuery().activityId("serviceTaskSleep5").singleResult();

            // The end time of all service tasks should be close to each other
            assertThat(serviceTask1Instance.getEndTime()).isCloseTo(serviceTask2Instance.getEndTime(), 100);
            assertThat(serviceTask1Instance.getEndTime()).isCloseTo(serviceTask3Instance.getEndTime(), 100);
            assertThat(serviceTask1Instance.getEndTime()).isCloseTo(serviceTask4Instance.getEndTime(), 100);
            assertThat(serviceTask1Instance.getEndTime()).isCloseTo(serviceTask5Instance.getEndTime(), 100);

            HistoricProcessInstance processInstance = historyService.createHistoricProcessInstanceQuery().singleResult();
            // The entire duration of the process instance should be less than 15 seconds (the combined time of all jobs)
            assertThat(processInstance.getDurationInMillis()).isLessThan(5 * 3000);
        }

    }

    @Test
    @Deployment
    @DisabledIfSystemProperty(named = "disableWhen", matches = "cockroachdb")
    void testFiveNonExclusiveParallelJobsThatCreateVariables() {

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {

            // The process has five service tasks in parallel, all non exclusive.
            // They should be executed with at least 3 seconds in between (as they both sleep for 3 seconds)
            runtimeService.startProcessInstanceByKey("testNonExclusiveJobs");
            // There should be 5 jobs
            assertThat(managementService.createJobQuery().count()).isEqualTo(5);
            waitForJobExecutorToProcessAllJobsAndExecutableTimerJobs(20000L, 500L);

            // There should be no jobs
            assertThat(managementService.createJobQuery().count()).isEqualTo(0);
            assertThat(managementService.createTimerJobQuery().count()).isEqualTo(0);
            assertThat(managementService.createDeadLetterJobQuery().count()).isEqualTo(0);

            waitForHistoryJobExecutorToProcessAllJobs(7000, 100);

            HistoricActivityInstance serviceTask1Instance = historyService.createHistoricActivityInstanceQuery().activityId("serviceTaskSleep1").singleResult();
            HistoricActivityInstance serviceTask2Instance = historyService.createHistoricActivityInstanceQuery().activityId("serviceTaskSleep2").singleResult();
            HistoricActivityInstance serviceTask3Instance = historyService.createHistoricActivityInstanceQuery().activityId("serviceTaskSleep3").singleResult();
            HistoricActivityInstance serviceTask4Instance = historyService.createHistoricActivityInstanceQuery().activityId("serviceTaskSleep4").singleResult();
            HistoricActivityInstance serviceTask5Instance = historyService.createHistoricActivityInstanceQuery().activityId("serviceTaskSleep5").singleResult();

            // The end time of all service tasks should be close to each other
            assertThat(serviceTask1Instance.getEndTime()).isCloseTo(serviceTask2Instance.getEndTime(), 100);
            assertThat(serviceTask1Instance.getEndTime()).isCloseTo(serviceTask3Instance.getEndTime(), 100);
            assertThat(serviceTask1Instance.getEndTime()).isCloseTo(serviceTask4Instance.getEndTime(), 100);
            assertThat(serviceTask1Instance.getEndTime()).isCloseTo(serviceTask5Instance.getEndTime(), 100);

            HistoricProcessInstance processInstance = historyService.createHistoricProcessInstanceQuery().singleResult();
            // The entire duration of the process instance should be less than 15 seconds (the combined time of all jobs)
            assertThat(processInstance.getDurationInMillis()).isLessThan(5 * 3000);

            assertThat(historyService.createHistoricVariableInstanceQuery().list())
                    .extracting(HistoricVariableInstance::getVariableName, HistoricVariableInstance::getValue)
                    .containsExactlyInAnyOrder(
                            tuple("var1", "value1"),
                            tuple("var2", "value2"),
                            tuple("var3", "value3"),
                            tuple("var4", "value4"),
                            tuple("var5", "value5")
                    );
        }

    }

}
