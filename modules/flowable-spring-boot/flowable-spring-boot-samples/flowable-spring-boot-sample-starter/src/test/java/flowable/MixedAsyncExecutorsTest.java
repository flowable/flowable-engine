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
import org.flowable.app.engine.test.FlowableAppRule;
import org.flowable.cmmn.api.CmmnRuntimeService;
import org.flowable.cmmn.api.CmmnTaskService;
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.engine.test.FlowableCmmnRule;
import org.flowable.cmmn.engine.test.impl.CmmnJobTestHelper;
import org.flowable.engine.impl.test.JobTestHelper;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.engine.test.FlowableRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * @author Filip Hrisafov
 */
@RunWith(SpringRunner.class)
@SpringBootTest
public class MixedAsyncExecutorsTest {

    @Rule
    @Autowired
    public FlowableRule processRule;

    @Rule
    @Autowired
    public FlowableCmmnRule cmmnRule;
    
    @Rule
    @Autowired
    public FlowableAppRule appRule;
    
    private CmmnRuntimeService cmmnRuntimeService;
    private CmmnTaskService cmmnTaskService;
    private AppRepositoryService appRepositoryService;

    @Before
    public void setUp() {
        cmmnRuntimeService = cmmnRule.getCmmnRuntimeService();
        cmmnTaskService = cmmnRule.getCmmnEngine().getCmmnTaskService();
        appRepositoryService = appRule.getAppRepositoryService();
    }

    @CmmnDeployment(resources = "stageAfterTimer.cmmn")
    @Deployment(resources = "timerAfterStart.bpmn20.xml")
    @Test
    public void mixedAsyncExecutor() {
        Date startTime = new Date();
        setClockTo(startTime);

        // Start the Case instance
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testStageAfterTimerEventListener")
            .start();

        assertThat(cmmnTaskService.createTaskQuery().count()).isEqualTo(0);

        // Start the Process instance
        ProcessInstance processInstance = processRule.getRuntimeService().createProcessInstanceBuilder().processDefinitionKey("testTimerEvent").start();

        assertThat(processRule.getTaskService().createTaskQuery().count()).isEqualTo(0);

        // Timer fires after 1 day, so setting it to 1 day + 1 second
        setClockTo(new Date(startTime.getTime() + (24 * 60 * 60 * 1000 + 1)));

        CmmnJobTestHelper.waitForJobExecutorToProcessAllJobs(cmmnRule.getCmmnEngine(), 5000L, 200L, true);
        JobTestHelper.waitForJobExecutorToProcessAllJobs(processRule, 5000L, 200L);

        // User task should be active after the timer has triggered
        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).count()).isEqualTo(2);
        assertThat(processRule.getTaskService().createTaskQuery().processInstanceId(processInstance.getId()).count()).isEqualTo(1);
    }
    
    @Test
    public void testAppDefinitions() {
        assertThat(appRepositoryService.createAppDefinitionQuery().count()).isEqualTo(0);
    }

    protected void setClockTo(Date time) {
        processRule.setCurrentTime(time);
        cmmnRule.setCurrentTime(time);

    }
}
