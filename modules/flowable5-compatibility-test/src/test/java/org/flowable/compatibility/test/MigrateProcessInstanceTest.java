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
package org.flowable.compatibility.test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.engine.impl.cmd.SetProcessDefinitionVersionCmd;
import org.flowable.engine.repository.DeploymentProperties;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ProcessInstance;
import org.junit.jupiter.api.Test;

public class MigrateProcessInstanceTest extends AbstractFlowable6CompatibilityTest {

    @Test
    public void testSetProcessDefinitionVersion() {
        assertThat(runtimeService.createProcessInstanceQuery().processDefinitionKey("receiveTask").count())
            .isEqualTo(1);
        ProcessDefinition v5ProcessDefinition = repositoryService.createProcessDefinitionQuery().processDefinitionKey("receiveTask").singleResult();
        assertThat(repositoryService.createProcessDefinitionQuery().processDefinitionKey("receiveTask").count())
            .isEqualTo(1);

        // deploy second v5 process definition
        repositoryService.createDeployment()
            .addClasspathResource("migrationProcess.bpmn20.xml")
            .deploymentProperty(DeploymentProperties.DEPLOY_AS_FLOWABLE5_PROCESS_DEFINITION, true)
            .deploy();

        ProcessDefinition v5SecondProcessDefinition = repositoryService.createProcessDefinitionQuery()
            .processDefinitionKey("receiveTask")
            .latestVersion()
            .singleResult();

        // deploy v6 process definition
        repositoryService.createDeployment()
            .addClasspathResource("migrationProcess.bpmn20.xml")
            .deploy();

        assertThat(repositoryService.createProcessDefinitionQuery().processDefinitionKey("receiveTask").list())
            .extracting(ProcessDefinition::getKey, ProcessDefinition::getVersion, ProcessDefinition::getEngineVersion)
            .as("key, version, engineVersion")
            .containsExactlyInAnyOrder(
                tuple("receiveTask", 1, "v5"),
                tuple("receiveTask", 2, "v5"),
                tuple("receiveTask", 3, null)
            );

        ProcessDefinition v6ProcessDefinition = repositoryService.createProcessDefinitionQuery()
            .processDefinitionKey("receiveTask")
            .latestVersion()
            .singleResult();

        ProcessInstance pi = runtimeService.createProcessInstanceQuery().processDefinitionKey("receiveTask").singleResult();
        assertThat(pi.getProcessDefinitionId()).isEqualTo(v5ProcessDefinition.getId());

        // migrate process instance v5 to v5
        managementService.executeCommand(new SetProcessDefinitionVersionCmd(pi.getId(), 2));

        pi = runtimeService.createProcessInstanceQuery().processInstanceId(pi.getId()).singleResult();
        assertThat(pi.getProcessDefinitionId()).isEqualTo(v5SecondProcessDefinition.getId());

        // migrate process instance v5 to v6 should not work
        SetProcessDefinitionVersionCmd command = new SetProcessDefinitionVersionCmd(pi.getId(), 3);
        assertThatThrownBy(() -> managementService.executeCommand(command))
            .isInstanceOf(FlowableIllegalArgumentException.class)
            .hasMessage("The current process definition (id = '" + v5SecondProcessDefinition.getId() + "') is a v5 definition."
                + " However the new process definition (id = '" + v6ProcessDefinition.getId() + "') is not a v5 definition.");

    }
}
