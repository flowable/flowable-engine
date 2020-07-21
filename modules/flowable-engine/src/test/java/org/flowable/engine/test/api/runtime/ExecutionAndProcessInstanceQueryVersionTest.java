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
package org.flowable.engine.test.api.runtime;

import static org.assertj.core.api.Assertions.assertThat;

import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ExecutionAndProcessInstanceQueryVersionTest extends PluggableFlowableTestCase {

    private static final String PROCESS_DEFINITION_KEY = "oneTaskProcess";
    private static final String DEPLOYMENT_FILE_PATH = "org/flowable/engine/test/api/runtime/oneTaskProcess.bpmn20.xml";

    private org.flowable.engine.repository.Deployment oldDeployment;
    private org.flowable.engine.repository.Deployment newDeployment;

    @BeforeEach
    protected void setUp() throws Exception {
        oldDeployment = repositoryService.createDeployment()
                .addClasspathResource(DEPLOYMENT_FILE_PATH)
                .deploy();

        runtimeService.startProcessInstanceByKey(PROCESS_DEFINITION_KEY).getId();

        newDeployment = repositoryService.createDeployment()
                .addClasspathResource(DEPLOYMENT_FILE_PATH)
                .deploy();

        runtimeService.startProcessInstanceByKey(PROCESS_DEFINITION_KEY).getId();
    }

    @AfterEach
    protected void tearDown() throws Exception {
        repositoryService.deleteDeployment(oldDeployment.getId(), true);
        repositoryService.deleteDeployment(newDeployment.getId(), true);
    }

    @Test
    public void testProcessInstanceQueryByProcessDefinitionVersion() {
        assertThat(runtimeService.createProcessInstanceQuery().processDefinitionVersion(1).count()).isEqualTo(1);
        assertThat(runtimeService.createProcessInstanceQuery().processDefinitionVersion(2).count()).isEqualTo(1);
        assertThat(runtimeService.createProcessInstanceQuery().processDefinitionVersion(3).count()).isZero();
        assertThat(runtimeService.createProcessInstanceQuery().processDefinitionVersion(1).count()).isEqualTo(1);
        assertThat(runtimeService.createProcessInstanceQuery().processDefinitionVersion(2).list()).hasSize(1);
        assertThat(runtimeService.createProcessInstanceQuery().processDefinitionVersion(3).list()).isEmpty();
    }

    @Test
    public void testProcessInstanceQueryByProcessDefinitionVersionAndKey() {
        assertThat(runtimeService.createProcessInstanceQuery().processDefinitionKey(PROCESS_DEFINITION_KEY).processDefinitionVersion(1).count()).isEqualTo(1);
        assertThat(runtimeService.createProcessInstanceQuery().processDefinitionKey(PROCESS_DEFINITION_KEY).processDefinitionVersion(2).count()).isEqualTo(1);
        assertThat(runtimeService.createProcessInstanceQuery().processDefinitionKey("undefined").processDefinitionVersion(1).count()).isZero();
        assertThat(runtimeService.createProcessInstanceQuery().processDefinitionKey("undefined").processDefinitionVersion(2).count()).isZero();
        assertThat(runtimeService.createProcessInstanceQuery().processDefinitionKey(PROCESS_DEFINITION_KEY).processDefinitionVersion(1).list()).hasSize(1);
        assertThat(runtimeService.createProcessInstanceQuery().processDefinitionKey(PROCESS_DEFINITION_KEY).processDefinitionVersion(2).list()).hasSize(1);
        assertThat(runtimeService.createProcessInstanceQuery().processDefinitionKey("undefined").processDefinitionVersion(1).list()).isEmpty();
        assertThat(runtimeService.createProcessInstanceQuery().processDefinitionKey("undefined").processDefinitionVersion(2).list()).isEmpty();
    }

    @Test
    public void testProcessInstanceOrQueryByProcessDefinitionVersion() {
        assertThat(runtimeService.createProcessInstanceQuery().or().processDefinitionVersion(1).processDefinitionId("undefined").endOr().count()).isEqualTo(1);
        assertThat(runtimeService.createProcessInstanceQuery().or().processDefinitionVersion(2).processDefinitionId("undefined").endOr().count()).isEqualTo(1);
        assertThat(runtimeService.createProcessInstanceQuery().or().processDefinitionVersion(3).processDefinitionId("undefined").endOr().count()).isZero();
        assertThat(runtimeService.createProcessInstanceQuery().or().processDefinitionVersion(1).processDefinitionId("undefined").endOr().list()).hasSize(1);
        assertThat(runtimeService.createProcessInstanceQuery().or().processDefinitionVersion(2).processDefinitionId("undefined").endOr().list()).hasSize(1);
        assertThat(runtimeService.createProcessInstanceQuery().or().processDefinitionVersion(3).processDefinitionId("undefined").endOr().list()).isEmpty();
    }

    @Test
    public void testExecutionQueryByProcessDefinitionVersion() {
        assertThat(runtimeService.createExecutionQuery().processDefinitionVersion(1).count()).isEqualTo(2);
        assertThat(runtimeService.createExecutionQuery().processDefinitionVersion(2).count()).isEqualTo(2);
        assertThat(runtimeService.createExecutionQuery().processDefinitionVersion(3).count()).isZero();
        assertThat(runtimeService.createExecutionQuery().processDefinitionVersion(1).list()).hasSize(2);
        assertThat(runtimeService.createExecutionQuery().processDefinitionVersion(2).list()).hasSize(2);
        assertThat(runtimeService.createExecutionQuery().processDefinitionVersion(3).list()).isEmpty();
    }

    @Test
    public void testExecutionQueryByProcessDefinitionVersionAndKey() {
        assertThat(runtimeService.createExecutionQuery().processDefinitionKey(PROCESS_DEFINITION_KEY).processDefinitionVersion(1).count()).isEqualTo(2);
        assertThat(runtimeService.createExecutionQuery().processDefinitionKey(PROCESS_DEFINITION_KEY).processDefinitionVersion(2).count()).isEqualTo(2);
        assertThat(runtimeService.createExecutionQuery().processDefinitionKey("undefined").processDefinitionVersion(1).count()).isZero();
        assertThat(runtimeService.createExecutionQuery().processDefinitionKey("undefined").processDefinitionVersion(2).count()).isZero();
        assertThat(runtimeService.createExecutionQuery().processDefinitionKey(PROCESS_DEFINITION_KEY).processDefinitionVersion(1).list()).hasSize(2);
        assertThat(runtimeService.createExecutionQuery().processDefinitionKey(PROCESS_DEFINITION_KEY).processDefinitionVersion(2).list()).hasSize(2);
        assertThat(runtimeService.createExecutionQuery().processDefinitionKey("undefined").processDefinitionVersion(1).list()).isEmpty();
        assertThat(runtimeService.createExecutionQuery().processDefinitionKey("undefined").processDefinitionVersion(2).list()).isEmpty();
    }
}
