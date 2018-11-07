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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

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
        assertNotNull(formDefinition);
        assertEquals("form1", formDefinition.getKey());
    }

    @Test
    @FormDeploymentAnnotation(resources = "org/flowable/form/engine/test/deployment/simple.form")
    public void redeploySingleForm() {
        FormDefinition formDefinition = repositoryService.createFormDefinitionQuery()
                .latestVersion()
                .formDefinitionKey("form1")
                .singleResult();
        assertNotNull(formDefinition);
        assertEquals("form1", formDefinition.getKey());
        assertEquals(1, formDefinition.getVersion());

        FormInfo formInfo = repositoryService.getFormModelByKey("form1");
        SimpleFormModel formModel = (SimpleFormModel) formInfo.getFormModel();
        assertEquals(1, formModel.getFields().size());
        assertEquals("input1", formModel.getFields().get(0).getId());
        assertEquals("Input1", formModel.getFields().get(0).getName());

        FormDeployment redeployment = repositoryService.createDeployment()
                .addClasspathResource("org/flowable/form/engine/test/deployment/simple2.form")
                .deploy();

        formDefinition = repositoryService.createFormDefinitionQuery()
                .latestVersion()
                .formDefinitionKey("form1")
                .singleResult();
        assertNotNull(formDefinition);
        assertEquals("form1", formDefinition.getKey());
        assertEquals(2, formDefinition.getVersion());

        formInfo = repositoryService.getFormModelByKey("form1");
        formModel = (SimpleFormModel) formInfo.getFormModel();
        assertEquals(1, formModel.getFields().size());
        assertEquals("input2", formModel.getFields().get(0).getId());
        assertEquals("Input2", formModel.getFields().get(0).getName());

        repositoryService.deleteDeployment(redeployment.getId());
    }

    @Test
    @FormDeploymentAnnotation(resources = { "org/flowable/form/engine/test/deployment/simple.form",
            "org/flowable/form/engine/test/deployment/form_with_dates.form" })
    public void deploy2Forms() {
        List<FormDefinition> formDefinitions = repositoryService.createFormDefinitionQuery().orderByFormName().asc().list();
        assertEquals(2, formDefinitions.size());

        assertEquals("My date form", formDefinitions.get(0).getName());
        assertEquals("My first form", formDefinitions.get(1).getName());
    }
}
