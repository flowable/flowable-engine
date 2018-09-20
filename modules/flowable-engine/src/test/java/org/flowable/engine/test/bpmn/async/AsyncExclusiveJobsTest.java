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

import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.impl.test.HistoryTestHelper;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.test.Deployment;
import org.junit.jupiter.api.Test;

public class AsyncExclusiveJobsTest extends PluggableFlowableTestCase {

    /**
     * Test for https://activiti.atlassian.net/browse/ACT-4035.
     */
    @Test
    @Deployment
    public void testExclusiveJobs() {

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {

            // The process has two script tasks in parallel, both exclusive.
            // They should be executed with at least 6 seconds in between (as they both sleep for 6 seconds)
            runtimeService.startProcessInstanceByKey("testExclusiveJobs");
            waitForJobExecutorToProcessAllJobs(20000L, 500L);
            
            waitForHistoryJobExecutorToProcessAllJobs(7000, 100);

            HistoricActivityInstance scriptTaskAInstance = historyService.createHistoricActivityInstanceQuery().activityId("scriptTaskA").singleResult();
            HistoricActivityInstance scriptTaskBInstance = historyService.createHistoricActivityInstanceQuery().activityId("scriptTaskB").singleResult();

            long endTimeA = scriptTaskAInstance.getEndTime().getTime();
            long endTimeB = scriptTaskBInstance.getEndTime().getTime();
            long endTimeDifference = 0;
            if (endTimeB > endTimeA) {
                endTimeDifference = endTimeB - endTimeA;
            } else {
                endTimeDifference = endTimeA - endTimeB;
            }
            assertTrue(endTimeDifference > 6000); // > 6000 -> jobs were executed in parallel
        }

    }

}
