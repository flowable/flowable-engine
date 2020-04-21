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
package org.flowable.standalone.validation;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.flowable.bpmn.exceptions.XMLException;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.ProcessEngines;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.impl.cfg.StandaloneInMemProcessEngineConfiguration;
import org.flowable.engine.repository.Deployment;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Joram Barrez
 */
public class DisabledSchemaValidationTest {

    protected ProcessEngine processEngine;

    protected RepositoryService repositoryService;

    @BeforeEach
    public void setup() {
        this.processEngine = new StandaloneInMemProcessEngineConfiguration()
                .setEngineName(this.getClass().getName())
                .setJdbcUrl("jdbc:h2:mem:activiti-process-validation;DB_CLOSE_DELAY=1000")
                .buildProcessEngine();
        this.repositoryService = processEngine.getRepositoryService();
    }

    @AfterEach
    public void tearDown() {
        for (Deployment deployment : repositoryService.createDeploymentQuery().list()) {
            repositoryService.deleteDeployment(deployment.getId());
        }

        ProcessEngines.unregister(processEngine);
        processEngine = null;
        repositoryService = null;
    }

    @Test
    public void testDisableValidation() {

        // Should fail
        assertThatThrownBy(
                () -> repositoryService.createDeployment().addClasspathResource("org/flowable/standalone/validation/invalid_process_xsd_error.bpmn20.xml")
                        .deploy())
                .isExactlyInstanceOf(XMLException.class);

        // Should fail with validation errors
        assertThatThrownBy(
                () -> repositoryService.createDeployment().addClasspathResource("org/flowable/standalone/validation/invalid_process_xsd_error.bpmn20.xml")
                        .disableSchemaValidation().deploy())
                .isExactlyInstanceOf(FlowableException.class);
    }

}
