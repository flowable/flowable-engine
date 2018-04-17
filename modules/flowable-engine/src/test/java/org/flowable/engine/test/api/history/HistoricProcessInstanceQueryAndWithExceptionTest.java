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
package org.flowable.engine.test.api.history;

import java.util.List;

import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.engine.history.HistoricProcessInstanceQuery;
import org.flowable.engine.impl.test.HistoryTestHelper;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.job.api.Job;
import org.flowable.job.api.TimerJobQuery;

public class HistoricProcessInstanceQueryAndWithExceptionTest extends PluggableFlowableTestCase {

    private static final String PROCESS_DEFINITION_KEY_NO_EXCEPTION = "oneTaskProcess";
    private static final String PROCESS_DEFINITION_KEY_WITH_EXCEPTION_1 = "JobErrorCheck";
    private static final String PROCESS_DEFINITION_KEY_WITH_EXCEPTION_2 = "JobErrorDoubleCheck";

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        repositoryService.createDeployment()
                .addClasspathResource("org/flowable/engine/test/api/runtime/oneTaskProcess.bpmn20.xml")
                .addClasspathResource("org/flowable/engine/test/api/runtime/JobErrorCheck.bpmn20.xml")
                .addClasspathResource("org/flowable/engine/test/api/runtime/JobErrorDoubleCheck.bpmn20.xml")
                .deploy();
    }

    @Override
    protected void tearDown() throws Exception {
        deleteDeployments();
        super.tearDown();
    }

    public void testQueryWithException() throws InterruptedException {
        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            ProcessInstance processNoException = runtimeService.startProcessInstanceByKey(PROCESS_DEFINITION_KEY_NO_EXCEPTION);
            
            waitForHistoryJobExecutorToProcessAllJobs(5000, 100);

            HistoricProcessInstanceQuery queryNoException = historyService.createHistoricProcessInstanceQuery();
            assertEquals(1, queryNoException.count());
            assertEquals(1, queryNoException.list().size());
            assertEquals(processNoException.getId(), queryNoException.list().get(0).getId());

            HistoricProcessInstanceQuery queryWithException = historyService.createHistoricProcessInstanceQuery();
            assertEquals(0, queryWithException.withJobException().count());
            assertEquals(0, queryWithException.withJobException().list().size());

            ProcessInstance processWithException1 = startProcessInstanceWithFailingJob(PROCESS_DEFINITION_KEY_WITH_EXCEPTION_1);
            TimerJobQuery jobQuery1 = managementService.createTimerJobQuery().processInstanceId(processWithException1.getId());
            assertEquals(1, jobQuery1.withException().count());
            assertEquals(1, jobQuery1.withException().list().size());
            
            waitForHistoryJobExecutorToProcessAllJobs(5000, 100);
            assertEquals(1, queryWithException.withJobException().count());
            assertEquals(1, queryWithException.withJobException().list().size());
            assertEquals(processWithException1.getId(), queryWithException.withJobException().list().get(0).getId());

            ProcessInstance processWithException2 = startProcessInstanceWithFailingJob(PROCESS_DEFINITION_KEY_WITH_EXCEPTION_2);
            TimerJobQuery jobQuery2 = managementService.createTimerJobQuery().processInstanceId(processWithException2.getId());
            assertEquals(2, jobQuery2.withException().count());
            assertEquals(2, jobQuery2.withException().list().size());

            waitForHistoryJobExecutorToProcessAllJobs(5000, 100);
            assertEquals(2, queryWithException.withJobException().count());
            assertEquals(2, queryWithException.withJobException().list().size());
            assertEquals(processWithException1.getId(), queryWithException.withJobException().processDefinitionKey(PROCESS_DEFINITION_KEY_WITH_EXCEPTION_1).list().get(0).getId());
            assertEquals(processWithException2.getId(), queryWithException.withJobException().processDefinitionKey(PROCESS_DEFINITION_KEY_WITH_EXCEPTION_2).list().get(0).getId());
        }
    }

    private ProcessInstance startProcessInstanceWithFailingJob(String processInstanceByKey) {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(processInstanceByKey);

        List<Job> jobList = managementService.createJobQuery()
                .processInstanceId(processInstance.getId())
                .list();

        for (Job job : jobList) {
            try {
                managementService.executeJob(job.getId());
                fail("RuntimeException");
            } catch (RuntimeException re) {
            }
        }
        return processInstance;
    }
}
