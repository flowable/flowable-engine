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
package org.flowable.engine.test.api.runtime;

import java.util.List;

import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.runtime.ProcessInstanceQuery;
import org.flowable.job.api.Job;
import org.flowable.job.api.TimerJobQuery;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ProcessInstanceQueryAndWithExceptionTest extends PluggableFlowableTestCase {

    private static final String PROCESS_DEFINITION_KEY_NO_EXCEPTION = "oneTaskProcess";
    private static final String PROCESS_DEFINITION_KEY_WITH_EXCEPTION_1 = "JobErrorCheck";
    private static final String PROCESS_DEFINITION_KEY_WITH_EXCEPTION_2 = "JobErrorDoubleCheck";

    private org.flowable.engine.repository.Deployment deployment;

    @BeforeEach
    protected void setUp() throws Exception {
        deployment = repositoryService.createDeployment()
                .addClasspathResource("org/flowable/engine/test/api/runtime/oneTaskProcess.bpmn20.xml")
                .addClasspathResource("org/flowable/engine/test/api/runtime/JobErrorCheck.bpmn20.xml")
                .addClasspathResource("org/flowable/engine/test/api/runtime/JobErrorDoubleCheck.bpmn20.xml")
                .deploy();
    }

    @AfterEach
    protected void tearDown() throws Exception {
        repositoryService.deleteDeployment(deployment.getId(), true);
    }

    @Test
    public void testQueryWithException() throws InterruptedException {
        ProcessInstance processNoException = runtimeService.startProcessInstanceByKey(PROCESS_DEFINITION_KEY_NO_EXCEPTION);

        ProcessInstanceQuery queryNoException = runtimeService.createProcessInstanceQuery();
        assertEquals(1, queryNoException.count());
        assertEquals(1, queryNoException.list().size());
        assertEquals(processNoException.getId(), queryNoException.list().get(0).getId());

        ProcessInstanceQuery queryWithException = runtimeService.createProcessInstanceQuery();
        assertEquals(0, queryWithException.withJobException().count());
        assertEquals(0, queryWithException.withJobException().list().size());

        ProcessInstance processWithException1 = startProcessInstanceWithFailingJob(PROCESS_DEFINITION_KEY_WITH_EXCEPTION_1);
        TimerJobQuery jobQuery1 = managementService.createTimerJobQuery().processInstanceId(processWithException1.getId());
        assertEquals(1, jobQuery1.withException().count());
        assertEquals(1, jobQuery1.withException().list().size());
        assertEquals(1, queryWithException.withJobException().count());
        assertEquals(1, queryWithException.withJobException().list().size());
        assertEquals(processWithException1.getId(), queryWithException.withJobException().list().get(0).getId());

        ProcessInstance processWithException2 = startProcessInstanceWithFailingJob(PROCESS_DEFINITION_KEY_WITH_EXCEPTION_2);
        TimerJobQuery jobQuery2 = managementService.createTimerJobQuery().processInstanceId(processWithException2.getId());
        assertEquals(2, jobQuery2.withException().count());
        assertEquals(2, jobQuery2.withException().list().size());

        assertEquals(2, queryWithException.withJobException().count());
        assertEquals(2, queryWithException.withJobException().list().size());
        assertEquals(processWithException1.getId(), queryWithException.withJobException().processDefinitionKey(PROCESS_DEFINITION_KEY_WITH_EXCEPTION_1).list().get(0).getId());
        assertEquals(processWithException2.getId(), queryWithException.withJobException().processDefinitionKey(PROCESS_DEFINITION_KEY_WITH_EXCEPTION_2).list().get(0).getId());
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

    // Test delegate
    public static class TestJavaDelegate implements JavaDelegate {
        @Override
        public void execute(DelegateExecution execution) {
            throw new RuntimeException();
        }
    }
}
