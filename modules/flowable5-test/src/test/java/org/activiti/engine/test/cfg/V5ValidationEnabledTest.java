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

import org.flowable.common.engine.api.FlowableException;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.engine.ProcessEngines;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.cfg.StandaloneInMemProcessEngineConfiguration;
import org.flowable.engine.impl.cfg.StandaloneProcessEngineConfiguration;
import org.flowable.engine.impl.test.AbstractTestCase;
import org.flowable.engine.repository.DeploymentProperties;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ProcessInstance;

/**
 * Test cases for testing v5 entity validation functionality when the process engine is booted.
 * 
 * @author Tijs Rademakers
 */
public class V5ValidationEnabledTest extends AbstractTestCase {

    public void testDeployedV5ProcessDefinitionAfterReboot() {

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
                .setValidateFlowable5EntitiesEnabled(false)
                .setFlowable5CompatibilityEnabled(true)
                .buildProcessEngine();

        processEngine.getRepositoryService().createDeployment()
                .addClasspathResource("org/activiti/engine/test/api/oneTaskProcess.bpmn20.xml")
                .deploymentProperty(DeploymentProperties.DEPLOY_AS_FLOWABLE5_PROCESS_DEFINITION, true)
                .deploy();

        // verify existence of process definition
        List<ProcessDefinition> processDefinitions = processEngine.getRepositoryService().createProcessDefinitionQuery().list();
        assertEquals(1, processDefinitions.size());

        // Close the process engine
        processEngine.close();
        assertNotNull(processEngine.getRuntimeService());

        // Reboot the process engine
        try {
            processEngine = new StandaloneProcessEngineConfiguration()
                    .setEngineName("reboot-test")
                    .setDatabaseSchemaUpdate(ProcessEngineConfiguration.DB_SCHEMA_UPDATE_FALSE)
                    .setJdbcUrl("jdbc:h2:mem:flowable-reboot-test;DB_CLOSE_DELAY=1000")
                    .setAsyncExecutorActivate(false)
                    .buildProcessEngine();
            fail("Expected v5 validation error while booting the engine");

        } catch (FlowableException e) {
            assertTrue(e.getMessage().contains("Found v5 process definitions that are the latest version"));
        }

        processEngine = new StandaloneProcessEngineConfiguration()
                .setEngineName("reboot-test")
                .setDatabaseSchemaUpdate(ProcessEngineConfiguration.DB_SCHEMA_UPDATE_FALSE)
                .setJdbcUrl("jdbc:h2:mem:flowable-reboot-test;DB_CLOSE_DELAY=1000")
                .setAsyncExecutorActivate(false)
                .setValidateFlowable5EntitiesEnabled(false)
                .setFlowable5CompatibilityEnabled(true)
                .buildProcessEngine();

        processDefinitions = processEngine.getRepositoryService().createProcessDefinitionQuery().list();
        assertEquals(1, processDefinitions.size());

        // close the process engine
        processEngine.close();

        // Cleanup schema
        schemaProcessEngine.close();
    }

    public void testRunningV5ProcessInstancesAfterReboot() {

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
                .setValidateFlowable5EntitiesEnabled(false)
                .setFlowable5CompatibilityEnabled(true)
                .buildProcessEngine();

        processEngine.getRepositoryService().createDeployment()
                .addClasspathResource("org/activiti/engine/test/api/oneTaskProcess.bpmn20.xml")
                .deploymentProperty(DeploymentProperties.DEPLOY_AS_FLOWABLE5_PROCESS_DEFINITION, true)
                .deploy();

        // verify existence of process definition
        List<ProcessDefinition> processDefinitions = processEngine.getRepositoryService().createProcessDefinitionQuery().list();
        assertEquals(1, processDefinitions.size());

        ProcessInstance processInstance = processEngine.getRuntimeService().startProcessInstanceByKey("oneTaskProcess");

        // deploy new version of one task process definition on v6 engine
        processEngine.getRepositoryService().createDeployment()
                .addClasspathResource("org/activiti/engine/test/api/oneTaskProcess.bpmn20.xml")
                .deploy();

        // Close the process engine
        processEngine.close();
        assertNotNull(processEngine.getRuntimeService());

        // Reboot the process engine
        try {
            processEngine = new StandaloneProcessEngineConfiguration()
                    .setEngineName("reboot-test")
                    .setDatabaseSchemaUpdate(ProcessEngineConfiguration.DB_SCHEMA_UPDATE_FALSE)
                    .setJdbcUrl("jdbc:h2:mem:flowable-reboot-test;DB_CLOSE_DELAY=1000")
                    .setAsyncExecutorActivate(false)
                    .buildProcessEngine();
            fail("Expected v5 validation error while booting the engine");

        } catch (FlowableException e) {
            assertTrue(e.getMessage().contains("Found at least one running v5 process instance"));
        }

        processEngine = new StandaloneProcessEngineConfiguration()
                .setEngineName("reboot-test")
                .setDatabaseSchemaUpdate(ProcessEngineConfiguration.DB_SCHEMA_UPDATE_FALSE)
                .setJdbcUrl("jdbc:h2:mem:flowable-reboot-test;DB_CLOSE_DELAY=1000")
                .setAsyncExecutorActivate(false)
                .setValidateFlowable5EntitiesEnabled(false)
                .setFlowable5CompatibilityEnabled(true)
                .buildProcessEngine();

        processInstance = processEngine.getRuntimeService().createProcessInstanceQuery().singleResult();
        assertNotNull(processInstance);

        processEngine.getTaskService().complete(processEngine.getTaskService().createTaskQuery().singleResult().getId());

        assertEquals(0, processEngine.getRuntimeService().createProcessInstanceQuery().count());

        // close the process engine
        processEngine.close();

        // starting process engine without running process instances
        processEngine = new StandaloneProcessEngineConfiguration()
                .setEngineName("reboot-test")
                .setDatabaseSchemaUpdate(ProcessEngineConfiguration.DB_SCHEMA_UPDATE_FALSE)
                .setJdbcUrl("jdbc:h2:mem:flowable-reboot-test;DB_CLOSE_DELAY=1000")
                .setAsyncExecutorActivate(false)
                .buildProcessEngine();

        // close the process engine
        processEngine.close();

        // Cleanup schema
        schemaProcessEngine.close();
    }
}
