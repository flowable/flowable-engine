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
package org.flowable.engine.test.jobexecutor;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Date;
import java.util.concurrent.Callable;

import org.flowable.engine.impl.test.JobTestHelper;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.job.api.TimerJobQuery;
import org.junit.jupiter.api.Test;

/**
 * @author Joram Barrez
 */
public class JobExecutorExceptionsTest extends PluggableFlowableTestCase {

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/mgmt/ManagementServiceTest.testGetJobExceptionStacktrace.bpmn20.xml" })
    public void testQueryByExceptionWithRealJobExecutor() {
        TimerJobQuery query = managementService.createTimerJobQuery().withException();
        assertThat(query.count()).isZero();

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("exceptionInJobExecution");

        // Timer is set for 4 hours, so move clock 5 hours
        processEngineConfiguration.getClock().setCurrentTime(new Date(new Date().getTime() + 5 * 60 * 60 * 1000));

        // The execution is waiting in the first usertask. This contains a
        // boundary timer event which we will execute manual for testing purposes.
        JobTestHelper.waitForJobExecutorOnCondition(processEngineConfiguration, 7000L, 100L, new Callable<>() {
            @Override
            public Boolean call() throws Exception {
                return managementService.createTimerJobQuery().withException().count() == 1;
            }
        });

        query = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).withException();
        assertThat(query.count()).isEqualTo(1);
    }

}
