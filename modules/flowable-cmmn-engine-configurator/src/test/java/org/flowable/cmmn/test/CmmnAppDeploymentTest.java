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

import org.flowable.cmmn.api.repository.CaseDefinition;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.ProcessDefinition;
import org.junit.jupiter.api.Test;

/**
 * @author Filip Hrisafov
 */
public class CmmnAppDeploymentTest extends AbstractProcessEngineIntegrationTest {

    @Test
    public void deletingProcessDeploymentShouldRemoveChildCaseDeployment() {
        Deployment deployment = processEngineRepositoryService.createDeployment()
                .addClasspathResource("org/flowable/cmmn/test/processTaskSameDeploymentTrue.cmmn")
                .addClasspathResource("org/flowable/cmmn/test/oneTaskProcess.bpmn20.xml")
                .deploy();

        assertThat(cmmnRepositoryService.createCaseDefinitionQuery().list())
                .extracting(CaseDefinition::getKey)
                .containsExactlyInAnyOrder("oneProcessTaskCase");

        assertThat(processEngineRepositoryService.createProcessDefinitionQuery().list())
                .extracting(ProcessDefinition::getKey)
                .containsExactlyInAnyOrder("oneTask");

        processEngineRepositoryService.deleteDeployment(deployment.getId(), false);

        assertThat(cmmnRepositoryService.createCaseDefinitionQuery().list())
                .extracting(CaseDefinition::getKey)
                .isEmpty();

        assertThat(processEngineRepositoryService.createProcessDefinitionQuery().list())
                .extracting(ProcessDefinition::getKey)
                .isEmpty();
    }

    @Test
    public void deletingProcessDeploymentShouldRemoveChildCaseDeploymentWithCascade() {
        Deployment deployment = processEngineRepositoryService.createDeployment()
                .addClasspathResource("org/flowable/cmmn/test/processTaskSameDeploymentTrue.cmmn")
                .addClasspathResource("org/flowable/cmmn/test/oneTaskProcess.bpmn20.xml")
                .deploy();

        assertThat(cmmnRepositoryService.createCaseDefinitionQuery().list())
                .extracting(CaseDefinition::getKey)
                .containsExactlyInAnyOrder("oneProcessTaskCase");

        assertThat(processEngineRepositoryService.createProcessDefinitionQuery().list())
                .extracting(ProcessDefinition::getKey)
                .containsExactlyInAnyOrder("oneTask");

        cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneProcessTaskCase")
                .start();

        assertThat(cmmnRuntimeService.createCaseInstanceQuery().list())
                .hasSize(1);
        assertThat(processEngineRuntimeService.createProcessInstanceQuery().list())
                .hasSize(1);

        processEngineRepositoryService.deleteDeployment(deployment.getId(), true);

        assertThat(cmmnRepositoryService.createCaseDefinitionQuery().list())
                .extracting(CaseDefinition::getKey)
                .isEmpty();

        assertThat(processEngineRepositoryService.createProcessDefinitionQuery().list())
                .extracting(ProcessDefinition::getKey)
                .isEmpty();

        assertThat(cmmnRuntimeService.createCaseInstanceQuery().list())
                .isEmpty();
        assertThat(processEngineRuntimeService.createProcessInstanceQuery().list())
                .isEmpty();
    }
}
