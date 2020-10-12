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
package org.flowable.test.spring.executor.jms;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.awaitility.Awaitility;
import org.flowable.engine.ProcessEngine;
import org.flowable.job.service.impl.asyncexecutor.DefaultAsyncJobExecutor;
import org.flowable.spring.impl.test.CleanTestExecutionListener;
import org.flowable.test.spring.executor.jms.config.SpringJmsConfig;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.TestExecutionListeners.MergeMode;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@TestExecutionListeners(value = CleanTestExecutionListener.class, mergeMode = MergeMode.MERGE_WITH_DEFAULTS)
@ContextConfiguration(classes = SpringJmsConfig.class)
@RunWith(SpringJUnit4ClassRunner.class)
public class SpringJmsTest {

    @Autowired
    private ProcessEngine processEngine;

    @Test
    public void testMessageQueueAsyncExecutor() {
        processEngine.getRepositoryService().createDeployment()
                .addClasspathResource("org/flowable/test/spring/executor/jms/SpringJmsTest.testMessageQueueAsyncExecutor.bpmn20.xml")
                .deploy();

        Map<String, Object> vars = new HashMap<>();
        vars.put("input1", 123);
        vars.put("input2", 456);
        processEngine.getRuntimeService().startProcessInstanceByKey("AsyncProcess", vars);

        // Wait until the process is completely finished
        Awaitility.await().atMost(Duration.ofMinutes(1)).pollInterval(Duration.ofMillis(500)).untilAsserted(
            () -> assertThat(processEngine.getRuntimeService().createProcessInstanceQuery().count()).isZero());

        for (String activityName : Arrays.asList("A", "B", "C", "D", "E", "F", "After boundary", "The user task", "G", "G1", "G2", "G3", "H", "I", "J", "K", "L")) {
            assertThat(processEngine.getHistoryService().createHistoricActivityInstanceQuery().activityName(activityName).singleResult()).isNotNull();
        }

        assertThat(((DefaultAsyncJobExecutor) processEngine.getProcessEngineConfiguration().getAsyncExecutor()).getAsyncJobAcquisitionThread()).isNull();
    }

    @Test
    public void testMessageQueueAsyncExecutorException() {
        processEngine.getRepositoryService().createDeployment()
            .addClasspathResource("org/flowable/test/spring/executor/jms/SpringJmsTest.testMessageQueueAsyncExecutorException.bpmn20.xml")
            .deploy();

        Map<String, Object> vars = new HashMap<>();
        vars.put("input1", 123);
        vars.put("input2", 456);
        processEngine.getRuntimeService().startProcessInstanceByKey("AsyncProcess", vars);

        assertThat(processEngine.getManagementService().createDeadLetterJobQuery().count()).isEqualTo(0);

        for (int i = 0; i < 2; i++) {
            // Wait until the job has used up all retries
            Awaitility.await().atMost(Duration.ofMinutes(1)).pollInterval(Duration.ofMillis(500)).untilAsserted(
                () -> assertThat(processEngine.getManagementService().createTimerJobQuery().count()).isEqualTo(1));

            // Async service task expression fails -> is moved to timer
            assertThat(processEngine.getManagementService().createTimerJobQuery().singleResult().getRetries()).isEqualTo(3 - (i+1));
            processEngine.getManagementService().moveTimerToExecutableJob(processEngine.getManagementService().createTimerJobQuery().singleResult().getId());
        }

        assertThat(processEngine.getManagementService().createDeadLetterJobQuery().count()).isEqualTo(0);

    }

}