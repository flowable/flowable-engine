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

import org.flowable.form.api.FormDefinition;
import org.flowable.form.api.FormDeployment;
import org.flowable.form.api.FormInfo;
import org.flowable.form.model.SimpleFormModel;
import org.junit.jupiter.api.Test;

public class DeploymentTest extends AbstractFlowableFormTest {

    @Test
    @FormDeploymentAnnotation(resources = "org/flowable/form/engine/test/deployment/simple.form")
    public void deploySingleForm() {
        FormDefinition formDefinition = repositoryService.createFormDefinitionQuery()
                .latestVersion()
                .formDefinitionKey("form1")
                .singleResult();
        assertThat(formDefinition).isNotNull();
        assertThat(formDefinition.getKey()).isEqualTo("form1");
    }

    @Test
    @FormDeploymentAnnotation(resources = "org/flowable/form/engine/test/deployment/simple.form")
    public void redeploySingleForm() {
        FormDefinition formDefinition = repositoryService.createFormDefinitionQuery()
                .latestVersion()
                .formDefinitionKey("form1")
                .singleResult();
        assertThat(formDefinition).isNotNull();
        assertThat(formDefinition.getKey()).isEqualTo("form1");
        assertThat(formDefinition.getVersion()).isOne();

        FormInfo formInfo = repositoryService.getFormModelByKey("form1");
        SimpleFormModel formModel = (SimpleFormModel) formInfo.getFormModel();
        assertThat(formModel.getFields().size()).isOne();
        assertThat(formModel.getFields().get(0).getId()).isEqualTo("input1");
        assertThat(formModel.getFields().get(0).getName()).isEqualTo("Input1");

        FormDeployment redeployment = repositoryService.createDeployment()
                .addClasspathResource("org/flowable/form/engine/test/deployment/simple2.form")
                .deploy();

        formDefinition = repositoryService.createFormDefinitionQuery()
                .latestVersion()
                .formDefinitionKey("form1")
                .singleResult();
        assertThat(formDefinition).isNotNull();
        assertThat(formDefinition.getKey()).isEqualTo("form1");
        assertThat(formDefinition.getVersion()).isEqualTo(2);

        formInfo = repositoryService.getFormModelByKey("form1");
        formModel = (SimpleFormModel) formInfo.getFormModel();
        assertThat(formModel.getFields().size()).isOne();
        assertThat(formModel.getFields().get(0).getId()).isEqualTo("input2");
        assertThat(formModel.getFields().get(0).getName()).isEqualTo("Input2");

        repositoryService.deleteDeployment(redeployment.getId());
    }

    @Test
    @FormDeploymentAnnotation(resources = { "org/flowable/form/engine/test/deployment/simple.form",
            "org/flowable/form/engine/test/deployment/form_with_dates.form" })
    public void deploy2Forms() {
        List<FormDefinition> formDefinitions = repositoryService.createFormDefinitionQuery().orderByFormName().asc().list();
        assertThat(formDefinitions.size()).isEqualTo(2);

        assertThat(formDefinitions.get(0).getName()).isEqualTo("My date form");
        assertThat(formDefinitions.get(1).getName()).isEqualTo("My first form");
    }
    
    @Test
    public void deploySingleFormWithParentDeploymentId() {
        FormDeployment deployment = repositoryService.createDeployment()
                .addClasspathResource("org/flowable/form/engine/test/deployment/simple.form")
                .parentDeploymentId("someDeploymentId")
                .deploy();
        
        FormDeployment newDeployment = repositoryService.createDeployment()
                .addClasspathResource("org/flowable/form/engine/test/deployment/simple.form")
                .deploy();
        
        try {
            FormDefinition definition = repositoryService.createFormDefinitionQuery().deploymentId(deployment.getId()).singleResult();
            assertThat(definition).isNotNull();
            assertThat(definition.getKey()).isEqualTo("form1");
            assertThat(definition.getVersion()).isOne();
            
            FormDefinition newDefinition = repositoryService.createFormDefinitionQuery().deploymentId(newDeployment.getId()).singleResult();
            assertThat(newDefinition).isNotNull();
            assertThat(newDefinition.getKey()).isEqualTo("form1");
            assertThat(newDefinition.getVersion()).isEqualTo(2);
            
            FormInfo formInfo = repositoryService.getFormModelByKeyAndParentDeploymentId("form1", "someDeploymentId");
            assertThat(formInfo.getKey()).isEqualTo("form1");
            assertThat(formInfo.getVersion()).isOne();
            
            formEngineConfiguration.setAlwaysLookupLatestDefinitionVersion(true);
            formInfo = repositoryService.getFormModelByKeyAndParentDeploymentId("form1", "someDeploymentId");
            assertThat(formInfo.getKey()).isEqualTo("form1");
            assertThat(formInfo.getVersion()).isEqualTo(2);
        
        } finally {
            formEngineConfiguration.setAlwaysLookupLatestDefinitionVersion(false);
            repositoryService.deleteDeployment(deployment.getId());
            repositoryService.deleteDeployment(newDeployment.getId());
        }
    }
}
