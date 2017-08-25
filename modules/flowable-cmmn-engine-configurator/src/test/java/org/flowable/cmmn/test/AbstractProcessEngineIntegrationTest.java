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

import java.util.List;

import org.flowable.cmmn.engine.CmmnEngine;
import org.flowable.cmmn.engine.CmmnHistoryService;
import org.flowable.cmmn.engine.CmmnManagementService;
import org.flowable.cmmn.engine.CmmnRepositoryService;
import org.flowable.cmmn.engine.CmmnRuntimeService;
import org.flowable.cmmn.engine.configurator.CmmnEngineConfigurator;
import org.flowable.cmmn.engine.repository.CmmnDeployment;
import org.flowable.cmmn.engine.test.impl.CmmnTestRunner;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.cfg.ProcessEngineConfigurator;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
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

    protected static CmmnEngine cmmnEngine;
    protected static ProcessEngine processEngine;
    
    protected CmmnRepositoryService cmmnRepositoryService;
    protected CmmnRuntimeService cmmnRuntimeService;
    protected CmmnHistoryService cmmnHistoryService;
    protected CmmnManagementService cmmnManagementService;
    
    protected RepositoryService processEngineRepositoryService;
    protected RuntimeService processEngineRuntimeService;
    protected TaskService processEngineTaskService;
    
    @BeforeClass
    public static void bootProcessEngine() {
        if (processEngine == null) {
            processEngine = ProcessEngineConfiguration.createProcessEngineConfigurationFromResource("flowable.cfg.xml").buildProcessEngine();
            List<ProcessEngineConfigurator> configurators = ((ProcessEngineConfigurationImpl) processEngine.getProcessEngineConfiguration()).getAllConfigurators();
            for (ProcessEngineConfigurator configurator : configurators) {
                if (configurator instanceof CmmnEngineConfigurator) {
                    cmmnEngine = ((CmmnEngineConfigurator) configurator).getCmmnEngine();
                    CmmnTestRunner.setCmmnEngine(cmmnEngine); // TODO: better solution
                }
            }
        }
    }

    @Before
    public void setupServices() {
        this.cmmnRepositoryService = cmmnEngine.getCmmnRepositoryService();
        this.cmmnRuntimeService = cmmnEngine.getCmmnRuntimeService();
        this.cmmnHistoryService = cmmnEngine.getCmmnHistoryService();
        this.cmmnManagementService = cmmnEngine.getCmmnManagementService();
        
        this.processEngineRepositoryService = processEngine.getRepositoryService();
        this.processEngineRuntimeService = processEngine.getRuntimeService();
        this.processEngineTaskService = processEngine.getTaskService();
    }
    
    @After
    public void cleanup() {
        for (Deployment deployment : processEngineRepositoryService.createDeploymentQuery().list()) {
            processEngineRepositoryService.deleteDeployment(deployment.getId(), true);
        }
    }

}
