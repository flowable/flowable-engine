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

package org.activiti.engine.test.cfg;

import java.util.List;

import org.activiti.engine.impl.test.AbstractTestCase;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.engine.ProcessEngines;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.cfg.StandaloneInMemProcessEngineConfiguration;
import org.flowable.engine.impl.cfg.StandaloneProcessEngineConfiguration;
import org.flowable.engine.repository.DeploymentProperties;
import org.flowable.engine.repository.ProcessDefinition;

/**
 * Test cases for testing v5 entity validation functionality when the process engine is booted.
 * 
 * @author Tijs Rademakers
 */
public class V5RedeployTest extends AbstractTestCase {

    public void testRedeployV5ProcessDefinitionsAfterReboot() {

        // In case this test is run in a test suite, previous engines might have been initialized and cached. First we close the
        // existing process engines to make sure that the db is clean and that there are no existing process engines involved.
        ProcessEngines.destroy();

        // Creating the DB schema (without building a process engine)
        ProcessEngineConfigurationImpl processEngineConfiguration = new StandaloneInMemProcessEngineConfiguration();
        processEngineConfiguration.setEngineName("reboot-test-schema");
        processEngineConfiguration.setJdbcUrl("jdbc:h2:mem:flowable-reboot-test;DB_CLOSE_DELAY=1000");
        ProcessEngine schemaProcessEngine = processEngineConfiguration.buildProcessEngine();

        // Create process engine and deploy test process
        ProcessEngine processEngine = new StandaloneProcessEngineConfiguration()
                .setEngineName("reboot-test")
                .setDatabaseSchemaUpdate(ProcessEngineConfiguration.DB_SCHEMA_UPDATE_FALSE)
                .setJdbcUrl("jdbc:h2:mem:flowable-reboot-test;DB_CLOSE_DELAY=1000")
                .setAsyncExecutorActivate(false)
                .setFlowable5CompatibilityEnabled(true)
                .buildProcessEngine();

        processEngine.getRepositoryService().createDeployment()
                .addClasspathResource("org/activiti/engine/test/api/oneTaskProcess.bpmn20.xml")
                .addClasspathResource("org/activiti/engine/test/api/oneSubProcess.bpmn20.xml")
                .deploymentProperty(DeploymentProperties.DEPLOY_AS_FLOWABLE5_PROCESS_DEFINITION, true)
                .deploy();

        // verify existence of process definition
        List<ProcessDefinition> processDefinitions = processEngine.getRepositoryService().createProcessDefinitionQuery().list();
        assertEquals(2, processDefinitions.size());

        // Close the process engine
        processEngine.close();

        // Reboot the process engine
        processEngine = new StandaloneProcessEngineConfiguration()
                .setEngineName("reboot-test")
                .setDatabaseSchemaUpdate(ProcessEngineConfiguration.DB_SCHEMA_UPDATE_FALSE)
                .setJdbcUrl("jdbc:h2:mem:flowable-reboot-test;DB_CLOSE_DELAY=1000")
                .setAsyncExecutorActivate(false)
                .setFlowable5CompatibilityEnabled(true)
                .setRedeployFlowable5ProcessDefinitions(true)
                .buildProcessEngine();

        processDefinitions = processEngine.getRepositoryService().createProcessDefinitionQuery().list();
        assertEquals(4, processDefinitions.size());

        processDefinitions = processEngine.getRepositoryService().createProcessDefinitionQuery().latestVersion().list();
        assertEquals(2, processDefinitions.size());

        for (ProcessDefinition processDefinition : processDefinitions) {
            assertNull(processDefinition.getEngineVersion());
        }

        assertEquals(processDefinitions.get(0).getDeploymentId(), processDefinitions.get(1).getDeploymentId());

        // close the process engine
        processEngine.close();

        // Cleanup schema
        schemaProcessEngine.close();
    }

    public void testOnlyOneRedeployNecessaryAfterReboot() {

        // In case this test is run in a test suite, previous engines might have been initialized and cached. First we close the
        // existing process engines to make sure that the db is clean and that there are no existing process engines involved.
        ProcessEngines.destroy();

        // Creating the DB schema (without building a process engine)
        ProcessEngineConfigurationImpl processEngineConfiguration = new StandaloneInMemProcessEngineConfiguration();
        processEngineConfiguration.setEngineName("reboot-test-schema");
        processEngineConfiguration.setJdbcUrl("jdbc:h2:mem:flowable-reboot-test;DB_CLOSE_DELAY=1000");
        ProcessEngine schemaProcessEngine = processEngineConfiguration.buildProcessEngine();

        // Create process engine and deploy test process
        ProcessEngine processEngine = new StandaloneProcessEngineConfiguration()
                .setEngineName("reboot-test")
                .setDatabaseSchemaUpdate(ProcessEngineConfiguration.DB_SCHEMA_UPDATE_FALSE)
                .setJdbcUrl("jdbc:h2:mem:flowable-reboot-test;DB_CLOSE_DELAY=1000")
                .setAsyncExecutorActivate(false)
                .setFlowable5CompatibilityEnabled(true)
                .buildProcessEngine();

        processEngine.getRepositoryService().createDeployment()
                .addClasspathResource("org/activiti/engine/test/api/oneTaskProcess.bpmn20.xml")
                .addClasspathResource("org/activiti/engine/test/api/oneSubProcess.bpmn20.xml")
                .deploymentProperty(DeploymentProperties.DEPLOY_AS_FLOWABLE5_PROCESS_DEFINITION, true)
                .deploy();

        processEngine.getRepositoryService().createDeployment()
                .addClasspathResource("org/activiti/engine/test/api/oneSubProcess.bpmn20.xml")
                .deploy();

        // verify existence of process definition
        List<ProcessDefinition> processDefinitions = processEngine.getRepositoryService().createProcessDefinitionQuery().list();
        assertEquals(3, processDefinitions.size());

        // Close the process engine
        processEngine.close();

        // Reboot the process engine
        processEngine = new StandaloneProcessEngineConfiguration()
                .setEngineName("reboot-test")
                .setDatabaseSchemaUpdate(ProcessEngineConfiguration.DB_SCHEMA_UPDATE_FALSE)
                .setJdbcUrl("jdbc:h2:mem:flowable-reboot-test;DB_CLOSE_DELAY=1000")
                .setAsyncExecutorActivate(false)
                .setFlowable5CompatibilityEnabled(true)
                .setRedeployFlowable5ProcessDefinitions(true)
                .buildProcessEngine();

        processDefinitions = processEngine.getRepositoryService().createProcessDefinitionQuery().list();
        assertEquals(4, processDefinitions.size());

        processDefinitions = processEngine.getRepositoryService().createProcessDefinitionQuery().latestVersion().list();
        assertEquals(2, processDefinitions.size());

        for (ProcessDefinition processDefinition : processDefinitions) {
            assertNull(processDefinition.getEngineVersion());
        }

        assertFalse(processDefinitions.get(0).getDeploymentId().equals(processDefinitions.get(1).getDeploymentId()));

        // close the process engine
        processEngine.close();

        // Cleanup schema
        schemaProcessEngine.close();
    }

    public void testNoRedeployNecessaryAfterReboot() {

        // In case this test is run in a test suite, previous engines might have been initialized and cached. First we close the
        // existing process engines to make sure that the db is clean and that there are no existing process engines involved.
        ProcessEngines.destroy();

        // Creating the DB schema (without building a process engine)
        ProcessEngineConfigurationImpl processEngineConfiguration = new StandaloneInMemProcessEngineConfiguration();
        processEngineConfiguration.setEngineName("reboot-test-schema");
        processEngineConfiguration.setJdbcUrl("jdbc:h2:mem:flowable-reboot-test;DB_CLOSE_DELAY=1000");
        ProcessEngine schemaProcessEngine = processEngineConfiguration.buildProcessEngine();

        // Create process engine and deploy test process
        ProcessEngine processEngine = new StandaloneProcessEngineConfiguration()
                .setEngineName("reboot-test")
                .setDatabaseSchemaUpdate(ProcessEngineConfiguration.DB_SCHEMA_UPDATE_FALSE)
                .setJdbcUrl("jdbc:h2:mem:flowable-reboot-test;DB_CLOSE_DELAY=1000")
                .setAsyncExecutorActivate(false)
                .setFlowable5CompatibilityEnabled(true)
                .buildProcessEngine();

        processEngine.getRepositoryService().createDeployment()
                .addClasspathResource("org/activiti/engine/test/api/oneTaskProcess.bpmn20.xml")
                .addClasspathResource("org/activiti/engine/test/api/oneSubProcess.bpmn20.xml")
                .deploymentProperty(DeploymentProperties.DEPLOY_AS_FLOWABLE5_PROCESS_DEFINITION, true)
                .deploy();

        processEngine.getRepositoryService().createDeployment()
                .addClasspathResource("org/activiti/engine/test/api/oneTaskProcess.bpmn20.xml")
                .addClasspathResource("org/activiti/engine/test/api/oneSubProcess.bpmn20.xml")
                .deploy();

        // verify existence of process definition
        List<ProcessDefinition> processDefinitions = processEngine.getRepositoryService().createProcessDefinitionQuery().list();
        assertEquals(4, processDefinitions.size());

        // Close the process engine
        processEngine.close();

        // Reboot the process engine
        processEngine = new StandaloneProcessEngineConfiguration()
                .setEngineName("reboot-test")
                .setDatabaseSchemaUpdate(ProcessEngineConfiguration.DB_SCHEMA_UPDATE_FALSE)
                .setJdbcUrl("jdbc:h2:mem:flowable-reboot-test;DB_CLOSE_DELAY=1000")
                .setAsyncExecutorActivate(false)
                .setFlowable5CompatibilityEnabled(true)
                .setRedeployFlowable5ProcessDefinitions(true)
                .buildProcessEngine();

        processDefinitions = processEngine.getRepositoryService().createProcessDefinitionQuery().list();
        assertEquals(4, processDefinitions.size());

        processDefinitions = processEngine.getRepositoryService().createProcessDefinitionQuery().latestVersion().list();
        assertEquals(2, processDefinitions.size());

        for (ProcessDefinition processDefinition : processDefinitions) {
            assertNull(processDefinition.getEngineVersion());
        }

        assertEquals(processDefinitions.get(0).getDeploymentId(), processDefinitions.get(1).getDeploymentId());

        // close the process engine
        processEngine.close();

        // Cleanup schema
        schemaProcessEngine.close();
    }
}
