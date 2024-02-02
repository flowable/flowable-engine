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
package org.flowable.spring.test.jobexecutor;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.flowable.common.engine.impl.test.CleanTest;
import org.flowable.engine.ManagementService;
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.spring.impl.test.SpringFlowableTestCase;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

/**
 * @author Pablo Ganga
 * @author Joram Barrez
 */
@CleanTest
// We need to use per class as the test uses auto deployments. If they are deleted then the other tests will fail
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ContextConfiguration("classpath:org/flowable/spring/test/components/SpringjobExecutorTest-context.xml")
public class SpringAsyncExecutorTest extends SpringFlowableTestCase {

    @Autowired
    protected ManagementService managementService;

    @Autowired
    protected RuntimeService runtimeService;

    @Autowired
    protected TaskService taskService;

    @Autowired
    protected ProcessEngineConfiguration processEngineConfiguration;

    @Test
    public void testHappyJobExecutorPath() throws Exception {
        ProcessInstance instance = runtimeService.startProcessInstanceByKey("process1");
        assertThat(instance).isNotNull();

        processEngineConfiguration.getAsyncExecutor().start();

        waitForTasksToExpire(instance, 0);

        List<Task> activeTasks = taskService.createTaskQuery().processInstanceId(instance.getId()).list();
        assertThat(activeTasks).isEmpty();

        processEngineConfiguration.getAsyncExecutor().shutdown();
    }

    @Test
    public void testAsyncJobExecutorPath() throws Exception {
        ProcessInstance instance = runtimeService.startProcessInstanceByKey("asyncProcess");
        assertThat(instance).isNotNull();

        processEngineConfiguration.getAsyncExecutor().start();

        waitForProcessInstanceToEnd(instance);

        ProcessInstance processInstance = runtimeService.createProcessInstanceQuery()
                .processInstanceId(instance.getId())
                .singleResult();
        assertThat(processInstance).isNull();

        processEngineConfiguration.getAsyncExecutor().shutdown();
    }

    @Test
    public void testRollbackJobExecutorPath() throws Exception {
        ProcessInstance instance = runtimeService.startProcessInstanceByKey("errorProcess1");
        assertThat(instance).isNotNull();

        processEngineConfiguration.getAsyncExecutor().start();

        waitForTasksToExpire(instance, 1);

        List<Task> activeTasks = taskService.createTaskQuery().processInstanceId(instance.getId()).list();
        assertThat(activeTasks).hasSize(1);

        processEngineConfiguration.getAsyncExecutor().shutdown();
    }

    protected void waitForTasksToExpire(ProcessInstance instance, int expectedTaskSize) throws Exception {
        boolean finished = false;
        int nrOfSleeps = 0;
        while (!finished) {
            long jobCount = managementService.createJobQuery().count();
            long timerCount = managementService.createTimerJobQuery().count();
            if (jobCount == 0 && timerCount == 0) {
                List<Task> activeTasks = taskService.createTaskQuery().processInstanceId(instance.getId()).list();
                if (activeTasks.size() == expectedTaskSize) {
                    finished = true;
                } else {
                    Thread.sleep(500L);
                }

            } else if (nrOfSleeps < 20) {
                nrOfSleeps++;
                Thread.sleep(500L);

            } else {
                finished = true;
            }
        }
    }

    protected void waitForProcessInstanceToEnd(ProcessInstance instance) throws Exception {
        boolean finished = false;
        int nrOfSleeps = 0;
        while (!finished) {
            long jobCount = managementService.createJobQuery().count();
            long timerCount = managementService.createTimerJobQuery().count();
            if (jobCount == 0 && timerCount == 0) {
                long instancesFound = runtimeService.createProcessInstanceQuery().processInstanceId(instance.getId()).count();
                if (instancesFound == 0) {
                    finished = true;
                } else {
                    Thread.sleep(500L);
                }

            } else if (nrOfSleeps < 20) {
                nrOfSleeps++;
                Thread.sleep(500L);

            } else {
                finished = true;
            }
        }
    }

}
