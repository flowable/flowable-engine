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
package org.flowable.engine.test.api.form;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flowable.engine.form.FormProperty;
import org.flowable.engine.form.StartFormData;
import org.flowable.engine.form.TaskFormData;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.junit.jupiter.api.Test;

public class FormPropertyDefaultValueTest extends PluggableFlowableTestCase {

    @Test
    @Deployment
    public void testDefaultValue() throws Exception {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("FormPropertyDefaultValueTest.testDefaultValue");
        org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();

        TaskFormData formData = formService.getTaskFormData(task.getId());
        List<FormProperty> formProperties = formData.getFormProperties();
        assertThat(formProperties)
                .extracting(FormProperty::getId, FormProperty::getValue)
                .containsExactlyInAnyOrder(
                        tuple("booleanProperty", "true"),
                        tuple("stringProperty", "someString"),
                        tuple("longProperty", "42"),
                        tuple("longExpressionProperty", "23")
                );

        Map<String, String> formDataUpdate = new HashMap<>();
        formDataUpdate.put("longExpressionProperty", "1");
        formDataUpdate.put("booleanProperty", "false");
        formService.submitTaskFormData(task.getId(), formDataUpdate);

        assertThat(runtimeService.getVariable(processInstance.getId(), "booleanProperty")).isEqualTo(false);
        assertThat(runtimeService.getVariable(processInstance.getId(), "stringProperty")).isEqualTo("someString");
        assertThat(runtimeService.getVariable(processInstance.getId(), "longProperty")).isEqualTo(42L);
        assertThat(runtimeService.getVariable(processInstance.getId(), "longExpressionProperty")).isEqualTo(1L);
    }

    @Test
    @Deployment
    public void testStartFormDefaultValue() throws Exception {
        String processDefinitionId = repositoryService.createProcessDefinitionQuery().processDefinitionKey("FormPropertyDefaultValueTest.testDefaultValue").latestVersion().singleResult().getId();

        StartFormData startForm = formService.getStartFormData(processDefinitionId);

        List<FormProperty> formProperties = startForm.getFormProperties();
        assertThat(formProperties)
                .extracting(FormProperty::getId, FormProperty::getValue)
                .containsExactlyInAnyOrder(
                        tuple("booleanProperty", "true"),
                        tuple("stringProperty", "someString"),
                        tuple("longProperty", "42"),
                        tuple("longExpressionProperty", "23")
                );

        // Override 2 properties. The others should pe posted as the default-value
        Map<String, String> formDataUpdate = new HashMap<>();
        formDataUpdate.put("longExpressionProperty", "1");
        formDataUpdate.put("booleanProperty", "false");
        ProcessInstance processInstance = formService.submitStartFormData(processDefinitionId, formDataUpdate);

        assertThat(runtimeService.getVariable(processInstance.getId(), "booleanProperty")).isEqualTo(false);
        assertThat(runtimeService.getVariable(processInstance.getId(), "stringProperty")).isEqualTo("someString");
        assertThat(runtimeService.getVariable(processInstance.getId(), "longProperty")).isEqualTo(42L);
        assertThat(runtimeService.getVariable(processInstance.getId(), "longExpressionProperty")).isEqualTo(1L);
    }
}
