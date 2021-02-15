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
package flowable;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Date;

import org.flowable.app.api.AppRepositoryService;
import org.flowable.cmmn.api.CmmnRuntimeService;
import org.flowable.cmmn.api.CmmnTaskService;
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.engine.CmmnEngine;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.engine.test.impl.CmmnJobTestHelper;
import org.flowable.cmmn.spring.impl.test.FlowableCmmnSpringExtension;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.impl.test.JobTestHelper;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.spring.impl.test.FlowableSpringExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * @author Filip Hrisafov
 */
@SpringBootTest
@ExtendWith(FlowableCmmnSpringExtension.class)
@ExtendWith(FlowableSpringExtension.class)
public class MixedAsyncExecutorsTest {

    @Autowired
    protected ProcessEngine processEngine;

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    protected TaskService taskService;

    @Autowired
    protected CmmnEngine cmmnEngine;

    @Autowired
    private CmmnRuntimeService cmmnRuntimeService;

    @Autowired
    private CmmnTaskService cmmnTaskService;

    @Autowired
    private AppRepositoryService appRepositoryService;

    @CmmnDeployment(resources = "stageAfterTimer.cmmn")
    @Deployment(resources = "timerAfterStart.bpmn20.xml")
    @Test
    public void mixedAsyncExecutor() {
        Date startTime = new Date();
        setClockTo(startTime);

        // Start the Case instance
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testStageAfterTimerEventListener")
            .start();

        assertThat(cmmnTaskService.createTaskQuery().count()).isZero();

        // Start the Process instance
        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder().processDefinitionKey("testTimerEvent").start();

        assertThat(taskService.createTaskQuery().count()).isZero();

        // Timer fires after 1 day, so setting it to 1 day + 1 second
        setClockTo(new Date(startTime.getTime() + (24 * 60 * 60 * 1000 + 1)));

        CmmnJobTestHelper.waitForJobExecutorToProcessAllJobs(cmmnEngine, 10000L, 200L, true);
        JobTestHelper.waitForJobExecutorToProcessAllJobs(processEngine.getProcessEngineConfiguration(), processEngine.getManagementService(), 10000L, 200L);

        // User task should be active after the timer has triggered
        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).count()).isEqualTo(2);
        assertThat(taskService.createTaskQuery().processInstanceId(processInstance.getId()).count()).isEqualTo(1);
    }
    
    @Test
    public void testAppDefinitions() {
        assertThat(appRepositoryService.createAppDefinitionQuery().count()).isZero();
    }

    protected void setClockTo(Date time) {
        processEngine.getProcessEngineConfiguration().getClock().setCurrentTime(time);
        cmmnEngine.getCmmnEngineConfiguration().getClock().setCurrentTime(time);

    }
}
