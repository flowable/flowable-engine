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
package org.flowable.cmmn.test;

import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstanceState;
import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.engine.test.impl.CmmnTestRunner;
import org.flowable.common.engine.impl.interceptor.EngineConfigurationConstants;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.List;

import static junit.framework.TestCase.assertTrue;
import static org.hamcrest.core.StringStartsWith.startsWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

public class TaskIdGeneratorsConfigurationTest extends AbstractProcessEngineIntegrationTest {

    private static ProcessEngine previousProcessEngine;

    @BeforeClass
    public static void bootProcessEngine() {
        previousProcessEngine = processEngine;
        processEngine = ProcessEngineConfiguration.createProcessEngineConfigurationFromResource("taskidgenerators.flowable.cfg.xml").buildProcessEngine();
        cmmnEngineConfiguration = (CmmnEngineConfiguration) processEngine.getProcessEngineConfiguration()
                .getEngineConfigurations().get(EngineConfigurationConstants.KEY_CMMN_ENGINE_CONFIG);
        CmmnTestRunner.setCmmnEngineConfiguration(cmmnEngineConfiguration);
    }
    
    @AfterClass
    public static void resetProcessEngine() {
        processEngine.close();
        if (previousProcessEngine != null) {
            processEngine = previousProcessEngine;
            CmmnTestRunner.setCmmnEngineConfiguration(
                    (CmmnEngineConfiguration) previousProcessEngine.getProcessEngineConfiguration()
                            .getEngineConfigurations().get(EngineConfigurationConstants.KEY_CMMN_ENGINE_CONFIG)
            );
        } else {
            CmmnTestRunner.setCmmnEngineConfiguration(null);
        }
    }

    @Before
    public void deployOneTaskProcess() {
        if (processEngineRepositoryService.createDeploymentQuery().count() == 0) {
            processEngineRepositoryService.createDeployment().addClasspathResource("org/flowable/cmmn/test/oneTaskProcess.bpmn20.xml").deploy();
        }
    }
    
    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/ProcessTaskTest.testOneTaskProcessNonBlocking.cmmn")
    public void testOneTaskProcessNonBlocking() {
        startCaseInstanceWithOneTaskProcess();
        
        Task processTask = processEngine.getTaskService().createTaskQuery().singleResult();
        assertThat(processTask.getId(), startsWith("TASK-"));
    }
    
    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/ProcessTaskTest.testOneTaskProcessNonBlocking.cmmn")
    public void testOneTaskProcessNonBlockingWithVariables() {
        startCaseInstanceWithOneTaskProcess();
        ProcessInstance processInstance = processEngine.getRuntimeService().createProcessInstanceQuery().singleResult();
        processEngine.getRuntimeService().setVariableLocal(processInstance.getId(), "processVariable", "processVariableValue");
        Task processTask = processEngine.getTaskService().createTaskQuery().includeProcessVariables().singleResult();
        assertTrue(processTask.getProcessVariables().containsKey("processVariable"));
        assertThat(processTask.getId(), startsWith("TASK-"));
        Task task = cmmnTaskService.createTaskQuery().taskId(processTask.getId()).includeProcessVariables().singleResult();
        assertTrue(task.getProcessVariables().containsKey("processVariable"));
        assertThat(task.getId(), startsWith("TASK-"));
    }

    @Test
    public void testTaskBuilder() {
        Task processEngineTask = null;
        Task cmmnEngineTask = null;
        try {
            processEngineTask = this.processEngineTaskService.createTaskBuilder().name("process engine task").create();
            assertThat(processEngineTask.getId(), startsWith("TASK-"));

            cmmnEngineTask = this.cmmnTaskService.createTaskBuilder().name("cmmn engine task").create();
            assertThat(cmmnEngineTask.getId(), startsWith("CMMN-TASK-"));
        } finally {
            if (processEngineTask != null) {
                processEngineTaskService.deleteTask(processEngineTask.getId(), true);
            }
            if (cmmnEngineTask != null) {
                cmmnTaskService.deleteTask(cmmnEngineTask.getId(), true);
            }
        }
    }

    @Test
    public void testDeleteTasks() {
        Task processEngineTask = null;
        Task cmmnEngineTask = null;
        try {
            processEngineTask = this.processEngineTaskService.createTaskBuilder().name("process engine task").create();
            assertNotNull(processEngineTask);

            cmmnEngineTask = this.cmmnTaskService.createTaskBuilder().name("cmmn engine task").create();
            assertNotNull(cmmnEngineTask);
        } finally {
            //processTaskService can delete cmmn task and cmmnTaskService can delete process task
            if (processEngineTask != null) {
                cmmnTaskService.deleteTask(processEngineTask.getId(), true);
            }
            if (cmmnEngineTask != null) {
                processEngineTaskService.deleteTask(cmmnEngineTask.getId(), true);
            }
        }
    }

    @Test
    public void testOneTaskProcess() {
        this.processEngineRuntimeService.startProcessInstanceByKey("oneTask");

        Task processTask = processEngine.getTaskService().createTaskQuery().singleResult();
        assertThat(processTask.getId(), startsWith("TASK-"));
    }

    protected CaseInstance startCaseInstanceWithOneTaskProcess() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionId(cmmnRepositoryService.createCaseDefinitionQuery().singleResult().getId()).start();
        List<PlanItemInstance> planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .planItemInstanceState(PlanItemInstanceState.ACTIVE)
                .list();
        assertEquals(1, planItemInstances.size());
        cmmnRuntimeService.triggerPlanItemInstance(planItemInstances.get(0).getId());
        return caseInstance;
    }

}
