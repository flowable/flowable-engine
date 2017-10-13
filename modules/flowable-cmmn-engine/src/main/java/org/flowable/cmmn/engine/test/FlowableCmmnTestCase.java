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
package org.flowable.cmmn.engine.test;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.InputStream;

import org.flowable.cmmn.api.CmmnHistoryService;
import org.flowable.cmmn.api.CmmnManagementService;
import org.flowable.cmmn.api.CmmnRepositoryService;
import org.flowable.cmmn.api.CmmnRuntimeService;
import org.flowable.cmmn.api.CmmnTaskService;
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.engine.CmmnEngine;
import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.test.impl.CmmnTestRunner;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Joram Barrez
 */
@RunWith(CmmnTestRunner.class)
public class FlowableCmmnTestCase {
    
    private static final Logger logger = LoggerFactory.getLogger(FlowableCmmnTestCase.class);
    
    protected CmmnEngineConfiguration cmmnEngineConfiguration;
    protected CmmnManagementService cmmnManagementService;
    protected CmmnRepositoryService cmmnRepositoryService;
    protected CmmnRuntimeService cmmnRuntimeService;
    protected CmmnTaskService cmmnTaskService;
    protected CmmnHistoryService cmmnHistoryService;
    
    protected String deploymentId;
    
    @BeforeClass
    public static void setupEngine() {
        if (CmmnTestRunner.getCmmnEngineConfiguration() == null) {
            initCmmnEngine();
        }
    }

    protected static void initCmmnEngine() {
        try (InputStream inputStream = FlowableCmmnTestCase.class.getClassLoader().getResourceAsStream("flowable.cmmn.cfg.xml")) {
            CmmnEngine cmmnEngine = CmmnEngineConfiguration.createCmmnEngineConfigurationFromInputStream(inputStream).buildCmmnEngine();
            CmmnTestRunner.setCmmnEngineConfiguration(cmmnEngine.getCmmnEngineConfiguration());
        } catch (IOException e) {
            logger.error("Could not create CMMN engine", e);
        }
    }
    
    @Before
    public void setupServices() {
        CmmnEngineConfiguration cmmnEngineConfiguration = CmmnTestRunner.getCmmnEngineConfiguration();
        this.cmmnEngineConfiguration = cmmnEngineConfiguration;
        this.cmmnRepositoryService = cmmnEngineConfiguration.getCmmnRepositoryService();
        this.cmmnManagementService = cmmnEngineConfiguration.getCmmnManagementService();
        this.cmmnRuntimeService = cmmnEngineConfiguration.getCmmnRuntimeService();
        this.cmmnTaskService = cmmnEngineConfiguration.getCmmnTaskService();
        this.cmmnHistoryService = cmmnEngineConfiguration.getCmmnHistoryService();
    }
    
    @After
    public void cleanupDeployment() {
        if (deploymentId != null) {
           cmmnRepositoryService.deleteDeployment(deploymentId, true);
        }
    }
    
    protected void deployOneHumanTaskCaseModel() {
        deploymentId = cmmnRepositoryService.createDeployment()
                .addClasspathResource("org/flowable/cmmn/test/one-human-task-model.cmmn")
                .deploy()
                .getId();
    }
    
    protected void deployOneTaskCaseModel() {
        deploymentId = cmmnRepositoryService.createDeployment()
                .addClasspathResource("org/flowable/cmmn/test/one-task-model.cmmn")
                .deploy()
                .getId();
    }
    
    protected void assertCaseInstanceEnded(CaseInstance caseInstance) {
        assertEquals(0, cmmnRuntimeService.createCaseInstanceQuery().caseInstanceId(caseInstance.getId()).count());
        assertEquals(0, cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).count());
        assertEquals(1, cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceId(caseInstance.getId()).finished().count());
    }
    
    protected void assertCaseInstanceEnded(CaseInstance caseInstance, int nrOfExpectedMilestones) {
        assertCaseInstanceEnded(caseInstance);
        assertEquals(0, cmmnRuntimeService.createMilestoneInstanceQuery().milestoneInstanceCaseInstanceId(caseInstance.getId()).count());
        assertEquals(nrOfExpectedMilestones, cmmnHistoryService.createHistoricMilestoneInstanceQuery().milestoneInstanceCaseInstanceId(caseInstance.getId()).count());
    }
    
}
