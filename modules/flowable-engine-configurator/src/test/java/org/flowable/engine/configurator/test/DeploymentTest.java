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
package org.flowable.engine.configurator.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.flowable.app.api.repository.AppDefinition;
import org.flowable.app.api.repository.AppDeployment;
import org.flowable.app.engine.test.FlowableAppTestCase;
import org.flowable.cmmn.api.CmmnRepositoryService;
import org.flowable.cmmn.api.repository.CaseDefinition;
import org.flowable.cmmn.api.repository.CmmnDeployment;
import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.common.engine.impl.interceptor.EngineConfigurationConstants;
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.ProcessDefinition;
import org.junit.Test;

/**
 * @author Tijs Rademakers
 */
public class DeploymentTest extends FlowableAppTestCase {
    
    @Test
    public void testAppDefinitionDeployed() throws Exception {
        String baseResourcePath = "org/flowable/engine/configurator/test/";
        AppDeployment appDeployment = appRepositoryService.createDeployment()
            .addClasspathResource(baseResourcePath + "test.app")
            .addClasspathResource(baseResourcePath + "oneTaskProcess.bpmn20.xml")
            .addClasspathResource(baseResourcePath + "one-human-task-model.cmmn").deploy();
        
        ProcessEngineConfiguration processEngineConfiguration = null;
        Deployment deployment = null;
        CmmnEngineConfiguration cmmnEngineConfiguration = null;
        CmmnDeployment cmmnDeployment = null;
        
        try {
            AppDeployment queryAppDeployment = appRepositoryService.createDeploymentQuery().singleResult();
            assertNotNull(queryAppDeployment);
            assertEquals(appDeployment.getId(), queryAppDeployment.getId());
            
            AppDefinition appDefinition = appRepositoryService.createAppDefinitionQuery().deploymentId(appDeployment.getId()).singleResult();
            assertNotNull(appDefinition.getId());
            assertNotNull(appDeployment.getId(), appDefinition.getDeploymentId());
            assertEquals("testApp", appDefinition.getKey());
            assertEquals("Test app", appDefinition.getName());
            assertEquals(1, appDefinition.getVersion());
            
            processEngineConfiguration = (ProcessEngineConfiguration) appEngineConfiguration.getEngineConfigurations()
                            .get(EngineConfigurationConstants.KEY_PROCESS_ENGINE_CONFIG);
            RepositoryService repositoryService = processEngineConfiguration.getRepositoryService();
            deployment = repositoryService.createDeploymentQuery().parentDeploymentId(appDeployment.getId()).singleResult();
            assertNotNull(deployment);
            assertEquals(appDeployment.getId(), deployment.getParentDeploymentId());
            ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().deploymentId(deployment.getId()).singleResult();
            assertNotNull(processDefinition);
            assertEquals("oneTask", processDefinition.getKey());
            
            cmmnEngineConfiguration = (CmmnEngineConfiguration) appEngineConfiguration.getEngineConfigurations()
                            .get(EngineConfigurationConstants.KEY_CMMN_ENGINE_CONFIG);
            CmmnRepositoryService cmmnRepositoryService = cmmnEngineConfiguration.getCmmnRepositoryService();
            cmmnDeployment = cmmnRepositoryService.createDeploymentQuery().parentDeploymentId(appDeployment.getId()).singleResult();
            assertNotNull(cmmnDeployment);
            assertEquals(appDeployment.getId(), cmmnDeployment.getParentDeploymentId());
            CaseDefinition caseDefinition = cmmnRepositoryService.createCaseDefinitionQuery().deploymentId(cmmnDeployment.getId()).singleResult();
            assertNotNull(caseDefinition);
            assertEquals("oneTaskCase", caseDefinition.getKey());
            
            
        } finally {
            appRepositoryService.deleteDeployment(appDeployment.getId(), true);
            if (deployment != null) {
                processEngineConfiguration.getRepositoryService().deleteDeployment(deployment.getId());
            }
            
            if (cmmnDeployment != null) {
                cmmnEngineConfiguration.getCmmnRepositoryService().deleteDeployment(cmmnDeployment.getId(), true);
            }
        }
    }
}
