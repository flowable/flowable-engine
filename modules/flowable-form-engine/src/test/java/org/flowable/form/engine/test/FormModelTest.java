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

import org.flowable.form.api.FormInfo;
import org.flowable.form.model.FormField;
import org.flowable.form.model.SimpleFormModel;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class FormModelTest extends AbstractFlowableFormTest {

    @Test
    @FormDeploymentAnnotation(resources = "org/flowable/form/engine/test/deployment/simple.form")
    public void getSimpleFormModelWithVariables() throws Exception {
        String formDefinitionId = repositoryService.getFormModelByKey("form1").getId();

        Map<String, Object> variables = new HashMap<>();
        variables.put("input1", "test");

        FormInfo formInfo = formService.getFormModelWithVariablesById(formDefinitionId, null, variables, null);

        assertEquals(formDefinitionId, formInfo.getId());

        SimpleFormModel formModel = (SimpleFormModel) formInfo.getFormModel();
        assertEquals(1, formModel.getFields().size());
        FormField formField = formModel.getFields().get(0);
        assertEquals("input1", formField.getId());
        assertEquals("test", formField.getValue());
    }

}
