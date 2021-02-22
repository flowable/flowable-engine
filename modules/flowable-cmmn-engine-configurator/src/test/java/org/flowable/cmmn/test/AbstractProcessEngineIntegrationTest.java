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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.flowable.cmmn.api.CmmnHistoryService;
import org.flowable.cmmn.api.CmmnManagementService;
import org.flowable.cmmn.api.CmmnRepositoryService;
import org.flowable.cmmn.api.CmmnRuntimeService;
import org.flowable.cmmn.api.CmmnTaskService;
import org.flowable.cmmn.api.repository.CmmnDeployment;
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.test.impl.CmmnTestRunner;
import org.flowable.common.engine.impl.interceptor.EngineConfigurationConstants;
import org.flowable.engine.DynamicBpmnService;
import org.flowable.engine.HistoryService;
import org.flowable.engine.ManagementService;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.repository.Deployment;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

/**
 * @author Joram Barrez
 */
@RunWith(CmmnTestRunner.class)
public abstract class AbstractProcessEngineIntegrationTest {

    protected static CmmnEngineConfiguration cmmnEngineConfiguration;
    protected static ProcessEngine processEngine;

    protected CmmnRepositoryService cmmnRepositoryService;
    protected CmmnRuntimeService cmmnRuntimeService;
    protected CmmnTaskService cmmnTaskService;
    protected CmmnHistoryService cmmnHistoryService;
    protected CmmnManagementService cmmnManagementService;

    protected ManagementService processEngineManagementService;
    protected RepositoryService processEngineRepositoryService;
    protected RuntimeService processEngineRuntimeService;
    protected HistoryService processEngineHistoryService;
    protected TaskService processEngineTaskService;
    protected ProcessEngineConfiguration processEngineConfiguration;
    protected DynamicBpmnService processEngineDynamicBpmnService;

    @BeforeClass
    public static void bootProcessEngine() {
        if (processEngine == null) {
            processEngine = ProcessEngineConfiguration.createProcessEngineConfigurationFromResource("flowable.cfg.xml").buildProcessEngine();
            cmmnEngineConfiguration = (CmmnEngineConfiguration) processEngine.getProcessEngineConfiguration()
                    .getEngineConfigurations().get(EngineConfigurationConstants.KEY_CMMN_ENGINE_CONFIG);
            CmmnTestRunner.setCmmnEngineConfiguration(cmmnEngineConfiguration);
        }
    }

    @Before
    public void setupServices() {
        this.cmmnRepositoryService = cmmnEngineConfiguration.getCmmnRepositoryService();
        this.cmmnRuntimeService = cmmnEngineConfiguration.getCmmnRuntimeService();
        this.cmmnTaskService = cmmnEngineConfiguration.getCmmnTaskService();
        this.cmmnHistoryService = cmmnEngineConfiguration.getCmmnHistoryService();
        this.cmmnManagementService = cmmnEngineConfiguration.getCmmnManagementService();

        this.processEngineManagementService = processEngine.getManagementService();
        this.processEngineRepositoryService = processEngine.getRepositoryService();
        this.processEngineRuntimeService = processEngine.getRuntimeService();
        this.processEngineTaskService = processEngine.getTaskService();
        this.processEngineHistoryService = processEngine.getHistoryService();
        this.processEngineConfiguration = processEngine.getProcessEngineConfiguration();
        this.processEngineDynamicBpmnService = processEngine.getDynamicBpmnService();
    }

    @After
    public void cleanup() {
        for (Deployment deployment : processEngineRepositoryService.createDeploymentQuery().list()) {
            processEngineRepositoryService.deleteDeployment(deployment.getId(), true);
        }

        for (CmmnDeployment deployment : cmmnRepositoryService.createDeploymentQuery().list()) {
            cmmnRepositoryService.deleteDeployment(deployment.getId(), true);
        }
    }

    protected Date setCmmnClockFixedToCurrentTime() {
        Date date = new Date();
        cmmnEngineConfiguration.getClock().setCurrentTime(date);
        return date;
    }

    protected void assertCaseInstanceEnded(CaseInstance caseInstance) {
        long count = cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).count();
        assertThat(count).as(createCaseInstanceEndedErrorMessage(caseInstance, count)).isZero();
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceId(caseInstance.getId()).count()).as("Runtime case instance found").isZero();
        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceId(caseInstance.getId()).finished().count()).isEqualTo(1);
    }

    protected String createCaseInstanceEndedErrorMessage(CaseInstance caseInstance, long count) {
        String errorMessage = "Plan item instances found for case instance: ";
        if (count != 0) {
            List<PlanItemInstance> planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).list();
            String names = planItemInstances.stream()
                    .map(planItemInstance -> planItemInstance.getName() + "(" + planItemInstance.getPlanItemDefinitionType() + ")")
                    .collect(Collectors.joining(", "));
            errorMessage += names;
        }
        return errorMessage;
    }
}
