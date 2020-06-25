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
package org.flowable.form.engine.test;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.test.Deployment;
import org.flowable.form.api.FormDefinition;
import org.junit.Test;

/**
 * @author Yvo Swillens
 */
public class MixedDeploymentTest extends AbstractFlowableFormEngineConfiguratorTest {

    @Test
    @Deployment(resources = { "org/flowable/form/engine/test/deployment/oneTaskWithFormKeyProcess.bpmn20.xml",
            "org/flowable/form/engine/test/deployment/simple.form" })
    public void deploySingleProcessAndForm() {
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery()
                .latestVersion()
                .processDefinitionKey("oneTaskWithFormProcess")
                .singleResult();

        assertThat(processDefinition).isNotNull();
        assertThat(processDefinition.getKey()).isEqualTo("oneTaskWithFormProcess");

        FormDefinition formDefinition = formRepositoryService.createFormDefinitionQuery()
                .latestVersion()
                .formDefinitionKey("form1")
                .singleResult();
        assertThat(formDefinition).isNotNull();
        assertThat(formDefinition.getKey()).isEqualTo("form1");

        List<FormDefinition> formDefinitionList = repositoryService.getFormDefinitionsForProcessDefinition(processDefinition.getId());
        assertThat(formDefinitionList)
                .extracting(FormDefinition::getKey)
                .containsExactly("form1");
    }
}
