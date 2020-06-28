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

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flowable.form.api.FormDefinition;
import org.flowable.form.api.FormDeployment;
import org.flowable.form.api.FormInfo;
import org.flowable.form.api.FormInstance;
import org.flowable.form.model.FormField;
import org.flowable.form.model.SimpleFormModel;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;

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
        assertThat(formModel.getFields())
            .extracting(FormField::getId, FormField::getName)
            .containsExactly(tuple("input1", "Input1"));

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
        assertThat(formModel.getFields())
            .extracting(FormField::getId, FormField::getName)
            .containsExactly(tuple("input2", "Input2"));

        repositoryService.deleteDeployment(redeployment.getId(), true);
    }

    @Test
    @FormDeploymentAnnotation(resources = { "org/flowable/form/engine/test/deployment/simple.form",
            "org/flowable/form/engine/test/deployment/form_with_dates.form" })
    public void deploy2Forms() {
        List<FormDefinition> formDefinitions = repositoryService.createFormDefinitionQuery().orderByFormName().asc().list();
        assertThat(formDefinitions)
            .extracting(FormDefinition::getName)
            .containsExactly("My date form", "My first form");
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
            repositoryService.deleteDeployment(deployment.getId(), true);
            repositoryService.deleteDeployment(newDeployment.getId(), true);
        }
    }

    @Test
    public void deleteDeploymentWithCascadeShouldDeleteFormInstances() throws Exception {
        FormDeployment deployment = repositoryService.createDeployment()
                .addClasspathResource("org/flowable/form/engine/test/deployment/simple.form")
                .deploy();
        FormInfo formInfo = repositoryService.getFormModelByKey("form1");

        Map<String, Object> formValues = new HashMap<>();
        formValues.put("input1", "test");

        FormInstance formInstance = formService.createFormInstance(formValues, formInfo, null, null, null, null, "default");
        assertThat(formInstance.getFormDefinitionId()).isEqualTo(formInfo.getId());
        JsonNode formNode = formEngineConfiguration.getObjectMapper().readTree(formInstance.getFormValueBytes());
        assertThatJson(formNode)
                .isEqualTo("{"
                        + "   values: {"
                        + "     input1: 'test'"
                        + " },"
                        + " flowable_form_outcome: 'default'"
                        + "}");

        assertThat(formService.createFormInstanceQuery().count()).isOne();
        repositoryService.deleteDeployment(deployment.getId(), true);
        assertThat(formService.createFormInstanceQuery().id(formInstance.getId()).count()).isZero();
    }

    @Test
    public void deleteDeploymentWithoutCascadeShouldNotDeleteFormInstances() throws Exception {
        FormDeployment deployment = repositoryService.createDeployment()
                .addClasspathResource("org/flowable/form/engine/test/deployment/simple.form")
                .deploy();
        FormInfo formInfo = repositoryService.getFormModelByKey("form1");

        Map<String, Object> formValues = new HashMap<>();
        formValues.put("input1", "test");

        FormInstance formInstance = formService.createFormInstance(formValues, formInfo, null, null, null, null, "default");
        assertThat(formInstance.getFormDefinitionId()).isEqualTo(formInfo.getId());
        JsonNode formNode = formEngineConfiguration.getObjectMapper().readTree(formInstance.getFormValueBytes());
        assertThatJson(formNode)
                .isEqualTo("{"
                        + "   values: {"
                        + "     input1: 'test'"
                        + " },"
                        + " flowable_form_outcome: 'default'"
                        + "}");

        assertThat(formService.createFormInstanceQuery().count()).isOne();

        repositoryService.deleteDeployment(deployment.getId());
        assertThat(formService.createFormInstanceQuery().count()).isOne();

        formService.deleteFormInstance(formInstance.getId());
        assertThat(formService.createFormInstanceQuery().id(formInstance.getId()).count()).isZero();
    }
}
