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
package org.flowable.engine.test.regression;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashMap;
import java.util.Map;

import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.impl.test.HistoryTestHelper;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.job.api.Job;
import org.junit.jupiter.api.Test;
//SLF4J
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * From http://forums.activiti.org/content/inability-completely-delete-process- instance-when
 */
public class DeleteProcessInstanceTest extends PluggableFlowableTestCase {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeleteProcessInstanceTest.class);

    @Test
    @Deployment
    public void testNoEndTimeSet() {

        // Note that the instance with a org.flowable.task.service.Task Type of "user" is being started.
        LOGGER.info("Starting an instance of \"Demo Partial Deletion\" with a org.flowable.task.service.Task Type of \"user\".");

        // Set the inputs for the first process instance, which we will be able
        // to completely delete.
        Map<String, Object> inputParamsUser = new HashMap<>();
        inputParamsUser.put("taskType", "user");

        // Start the process instance & ensure it's started.
        ProcessInstance instanceUser = runtimeService.startProcessInstanceByKey("DemoPartialDeletion", inputParamsUser);
        assertThat(instanceUser).isNotNull();
        LOGGER.info("Process instance (of process model {}) started with id: {}.", instanceUser.getProcessDefinitionId(), instanceUser.getId());

        // Assert that the process instance is active.
        Execution executionUser = runtimeService.createExecutionQuery().processInstanceId(instanceUser.getProcessInstanceId()).onlyChildExecutions().singleResult();
        assertThat(executionUser.isEnded()).isFalse();

        // Assert that a user task is available for claiming.
        org.flowable.task.api.Task taskUser = taskService.createTaskQuery().processInstanceId(instanceUser.getProcessInstanceId()).singleResult();
        assertThat(taskUser).isNotNull();

        // Delete the process instance.
        runtimeService.deleteProcessInstance(instanceUser.getId(), null);

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            // Retrieve the HistoricProcessInstance and assert that there is an
            // end time.
            HistoricProcessInstance hInstanceUser = historyService.createHistoricProcessInstanceQuery().processInstanceId(instanceUser.getId()).singleResult();
            assertThat(hInstanceUser.getEndTime()).isNotNull();
            LOGGER.info("End time for the deleted instance of \"Demo Partial Deletion\" that was started with a org.flowable.task.service.Task Type of \"user\": {}.", hInstanceUser.getEndTime());
            LOGGER.info("Successfully deleted the instance of \"Demo Partial Deletion\" that was started with a org.flowable.task.service.Task Type of \"user\".");
        }

        // Note that the instance with a org.flowable.task.service.Task Type of "java" is being started.
        LOGGER.info("Starting an instance of \"Demo Partial Deletion\" with a org.flowable.task.service.Task Type of \"java\".");

        // Set the inputs for the second process instance, which we will NOT be
        // able to completely delete.
        Map<String, Object> inputParamsJava = new HashMap<>();
        inputParamsJava.put("taskType", "java");

        // Start the process instance & ensure it's started.
        ProcessInstance instanceJava = runtimeService.startProcessInstanceByKey("DemoPartialDeletion", inputParamsJava);
        assertThat(instanceJava).isNotNull();
        LOGGER.info("Process instance (of process model {}) started with id: {}.", instanceJava.getProcessDefinitionId(), instanceJava.getId());

        // Assert that the process instance is active.
        Execution executionJava = runtimeService.createExecutionQuery().processInstanceId(instanceJava.getProcessInstanceId()).onlyChildExecutions().singleResult();
        assertThat(executionJava.isEnded()).isFalse();

        // Try to execute job 3 times
        Job jobJava = managementService.createJobQuery().processInstanceId(instanceJava.getId()).singleResult();
        assertThat(jobJava).isNotNull();
        String jobId = jobJava.getId();

        assertThatThrownBy(() -> managementService.executeJob(jobId))
                .isInstanceOf(Exception.class);

        assertThatThrownBy(() -> {
            managementService.moveTimerToExecutableJob(jobId);
            managementService.executeJob(jobId);
        })
                .isInstanceOf(Exception.class);

        assertThatThrownBy(() -> {
            managementService.moveTimerToExecutableJob(jobId);
            managementService.executeJob(jobId);
        })
                .isInstanceOf(Exception.class);

        // Assert that there is a failed job.
        assertThat(managementService.createTimerJobQuery().processInstanceId(instanceJava.getId()).count()).isZero();
        jobJava = managementService.createDeadLetterJobQuery().processInstanceId(instanceJava.getId()).singleResult();
        assertThat(jobJava).isNotNull();

        // Delete the process instance.
        runtimeService.deleteProcessInstance(instanceJava.getId(), null);

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            // Retrieve the HistoricProcessInstance and assert that there is no
            // end time.
            HistoricProcessInstance hInstanceJava = historyService.createHistoricProcessInstanceQuery().processInstanceId(instanceJava.getId()).singleResult();
            assertThat(hInstanceJava.getEndTime()).isNotNull();
        }
    }

}
