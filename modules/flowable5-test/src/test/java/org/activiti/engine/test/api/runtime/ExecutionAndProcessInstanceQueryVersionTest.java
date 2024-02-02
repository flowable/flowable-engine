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
package org.activiti.engine.test.api.runtime;

import org.activiti.engine.impl.test.PluggableFlowableTestCase;

public class ExecutionAndProcessInstanceQueryVersionTest extends PluggableFlowableTestCase {

    private static final String PROCESS_DEFINITION_KEY = "oneTaskProcess";
    private static final String DEPLOYMENT_FILE_PATH = "org/activiti/engine/test/api/runtime/oneTaskProcess.bpmn20.xml";

    private org.flowable.engine.repository.Deployment oldDeployment;
    private org.flowable.engine.repository.Deployment newDeployment;

    protected void setUp() throws Exception {
        super.setUp();
        oldDeployment = repositoryService.createDeployment()
                .addClasspathResource(DEPLOYMENT_FILE_PATH)
                .deploy();

        runtimeService.startProcessInstanceByKey(PROCESS_DEFINITION_KEY).getId();

        newDeployment = repositoryService.createDeployment()
                .addClasspathResource(DEPLOYMENT_FILE_PATH)
                .deploy();

        runtimeService.startProcessInstanceByKey(PROCESS_DEFINITION_KEY).getId();
    }

    protected void tearDown() throws Exception {
        repositoryService.deleteDeployment(oldDeployment.getId(), true);
        repositoryService.deleteDeployment(newDeployment.getId(), true);
    }

    public void testProcessInstanceQueryByProcessDefinitionVersion() {
        assertEquals(1, runtimeService.createProcessInstanceQuery().processDefinitionVersion(1).count());
        assertEquals(1, runtimeService.createProcessInstanceQuery().processDefinitionVersion(2).count());
        assertEquals(0, runtimeService.createProcessInstanceQuery().processDefinitionVersion(3).count());
        assertEquals(1, runtimeService.createProcessInstanceQuery().processDefinitionVersion(1).count());
        assertEquals(1, runtimeService.createProcessInstanceQuery().processDefinitionVersion(2).list().size());
        assertEquals(0, runtimeService.createProcessInstanceQuery().processDefinitionVersion(3).list().size());
    }

    public void testProcessInstanceQueryByProcessDefinitionVersionAndKey() {
        assertEquals(1, runtimeService.createProcessInstanceQuery().processDefinitionKey(PROCESS_DEFINITION_KEY).processDefinitionVersion(1).count());
        assertEquals(1, runtimeService.createProcessInstanceQuery().processDefinitionKey(PROCESS_DEFINITION_KEY).processDefinitionVersion(2).count());
        assertEquals(0, runtimeService.createProcessInstanceQuery().processDefinitionKey("undefined").processDefinitionVersion(1).count());
        assertEquals(0, runtimeService.createProcessInstanceQuery().processDefinitionKey("undefined").processDefinitionVersion(2).count());
        assertEquals(1, runtimeService.createProcessInstanceQuery().processDefinitionKey(PROCESS_DEFINITION_KEY).processDefinitionVersion(1).list().size());
        assertEquals(1, runtimeService.createProcessInstanceQuery().processDefinitionKey(PROCESS_DEFINITION_KEY).processDefinitionVersion(2).list().size());
        assertEquals(0, runtimeService.createProcessInstanceQuery().processDefinitionKey("undefined").processDefinitionVersion(1).list().size());
        assertEquals(0, runtimeService.createProcessInstanceQuery().processDefinitionKey("undefined").processDefinitionVersion(2).list().size());
    }

    public void testProcessInstanceOrQueryByProcessDefinitionVersion() {
        assertEquals(1, runtimeService.createProcessInstanceQuery().or().processDefinitionVersion(1).processDefinitionId("undefined").endOr().count());
        assertEquals(1, runtimeService.createProcessInstanceQuery().or().processDefinitionVersion(2).processDefinitionId("undefined").endOr().count());
        assertEquals(0, runtimeService.createProcessInstanceQuery().or().processDefinitionVersion(3).processDefinitionId("undefined").endOr().count());
        assertEquals(1, runtimeService.createProcessInstanceQuery().or().processDefinitionVersion(1).processDefinitionId("undefined").endOr().list().size());
        assertEquals(1, runtimeService.createProcessInstanceQuery().or().processDefinitionVersion(2).processDefinitionId("undefined").endOr().list().size());
        assertEquals(0, runtimeService.createProcessInstanceQuery().or().processDefinitionVersion(3).processDefinitionId("undefined").endOr().list().size());
    }

    public void testExecutionQueryByProcessDefinitionVersion() {
        assertEquals(2, runtimeService.createExecutionQuery().processDefinitionVersion(1).count());
        assertEquals(2, runtimeService.createExecutionQuery().processDefinitionVersion(2).count());
        assertEquals(0, runtimeService.createExecutionQuery().processDefinitionVersion(3).count());
        assertEquals(2, runtimeService.createExecutionQuery().processDefinitionVersion(1).list().size());
        assertEquals(2, runtimeService.createExecutionQuery().processDefinitionVersion(2).list().size());
        assertEquals(0, runtimeService.createExecutionQuery().processDefinitionVersion(3).list().size());
    }

    public void testExecutionQueryByProcessDefinitionVersionAndKey() {
        assertEquals(2, runtimeService.createExecutionQuery().processDefinitionKey(PROCESS_DEFINITION_KEY).processDefinitionVersion(1).count());
        assertEquals(2, runtimeService.createExecutionQuery().processDefinitionKey(PROCESS_DEFINITION_KEY).processDefinitionVersion(2).count());
        assertEquals(0, runtimeService.createExecutionQuery().processDefinitionKey("undefined").processDefinitionVersion(1).count());
        assertEquals(0, runtimeService.createExecutionQuery().processDefinitionKey("undefined").processDefinitionVersion(2).count());
        assertEquals(2, runtimeService.createExecutionQuery().processDefinitionKey(PROCESS_DEFINITION_KEY).processDefinitionVersion(1).list().size());
        assertEquals(2, runtimeService.createExecutionQuery().processDefinitionKey(PROCESS_DEFINITION_KEY).processDefinitionVersion(2).list().size());
        assertEquals(0, runtimeService.createExecutionQuery().processDefinitionKey("undefined").processDefinitionVersion(1).list().size());
        assertEquals(0, runtimeService.createExecutionQuery().processDefinitionKey("undefined").processDefinitionVersion(2).list().size());
    }
}
