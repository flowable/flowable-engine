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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.flowable.cmmn.api.CmmnRuntimeService;
import org.flowable.cmmn.api.CmmnTaskService;
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.engine.test.impl.CmmnTestRunner;
import org.flowable.common.engine.impl.interceptor.EngineConfigurationConstants;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.form.api.FormRepositoryService;
import org.flowable.form.engine.FormEngineConfiguration;
import org.flowable.task.api.Task;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author martin.grofcik
 */
@RunWith(CmmnTestRunner.class)
public class HumanTaskTest {

    protected static ProcessEngine processEngine;
    protected static CmmnEngineConfiguration cmmnEngineConfiguration;

    protected CmmnRuntimeService cmmnRuntimeService;
    protected CmmnTaskService cmmnTaskService;
    protected FormRepositoryService formRepositoryService;

    @BeforeClass
    public static void bootProcessEngine() {
        if (processEngine == null) {
            processEngine = ProcessEngineConfiguration.createProcessEngineConfigurationFromResource("flowable-process-cmmn-form.cfg.xml").buildProcessEngine();
            cmmnEngineConfiguration = (CmmnEngineConfiguration) processEngine.getProcessEngineConfiguration()
                .getEngineConfigurations().get(EngineConfigurationConstants.KEY_CMMN_ENGINE_CONFIG);
            CmmnTestRunner.setCmmnEngineConfiguration(cmmnEngineConfiguration);
        }
    }

    @Before
    public void setup() {
        this.cmmnRuntimeService = cmmnEngineConfiguration.getCmmnRuntimeService();
        this.cmmnTaskService = cmmnEngineConfiguration.getCmmnTaskService();
        FormEngineConfiguration formEngineConfiguration = (FormEngineConfiguration) processEngine.getProcessEngineConfiguration()
            .getEngineConfigurations().get(EngineConfigurationConstants.KEY_FORM_ENGINE_CONFIG);
        this.formRepositoryService = formEngineConfiguration.getFormRepositoryService();

        formRepositoryService.createDeployment().addClasspathResource("org/flowable/cmmn/test/simple.form").deploy();
    }

    @After
    public void deleteFormDeployment() {
        this.formRepositoryService.createDeploymentQuery().list().forEach(
            formDeployment -> formRepositoryService.deleteDeployment(formDeployment.getId())
        );
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/CaseTaskTest.testCaseTask.cmmn")
    public void completeHumanTaskWithoutVariables() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().
            caseDefinitionKey("myCase").
            start();
        assertNotNull(caseInstance);

        Task caseTask = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertNotNull(caseTask);

        cmmnTaskService.completeTaskWithForm(caseTask.getId(), formRepositoryService.createFormDefinitionQuery().formDefinitionKey("form1").singleResult().getId(),
            "__COMPLETE", null);

        CaseInstance dbCaseInstance = cmmnRuntimeService.createCaseInstanceQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertNull(dbCaseInstance);
    }

}
