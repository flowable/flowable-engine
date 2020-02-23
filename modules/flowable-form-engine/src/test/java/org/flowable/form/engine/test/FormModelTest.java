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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashMap;
import java.util.Map;

import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.common.engine.impl.DefaultTenantProvider;
import org.flowable.form.api.FormInfo;
import org.flowable.form.model.FormField;
import org.flowable.form.model.SimpleFormModel;
import org.junit.jupiter.api.Test;

public class FormModelTest extends AbstractFlowableFormTest {

    @Test
    @FormDeploymentAnnotation(resources = "org/flowable/form/engine/test/deployment/simple.form")
    public void getSimpleFormModelWithVariables() throws Exception {
        String formDefinitionId = repositoryService.getFormModelByKey("form1").getId();

        Map<String, Object> variables = new HashMap<>();
        variables.put("input1", "test");

        FormInfo formInfo = formService.getFormModelWithVariablesById(formDefinitionId, null, variables, null, false);

        assertThat(formInfo.getId()).isEqualTo(formDefinitionId);
        assertFormModel(formInfo);
    }
    
    @Test
    @FormDeploymentAnnotation(resources = "org/flowable/form/engine/test/deployment/simple.form", tenantId="flowable")
    public void getSimpleFormModelInTenantWithVariables() throws Exception {
        String formDefinitionId = repositoryService.getFormModelByKey("form1", "flowable", false).getId();

        Map<String, Object> variables = new HashMap<>();
        variables.put("input1", "test");

        FormInfo formInfo = formService.getFormModelWithVariablesByKey("form1", null, variables, "flowable", false);

        assertThat(formInfo.getId()).isEqualTo(formDefinitionId);
        assertFormModel(formInfo);
    }
    
    @Test
    @FormDeploymentAnnotation(resources = "org/flowable/form/engine/test/deployment/simple.form", tenantId="flowable")
    public void getSimpleFormModelInAnotherTenantWithVariables() throws Exception {
        assertThatThrownBy(() -> repositoryService.getFormModelByKey("form1", "someTenant", false).getId())
            .isInstanceOf(FlowableObjectNotFoundException.class);

        Map<String, Object> variables = new HashMap<>();
        variables.put("input1", "test");

        assertThatThrownBy(() -> formService.getFormModelWithVariablesByKey("form1", null, variables, "someTenant", false))
            .isInstanceOf(FlowableObjectNotFoundException.class);
    }
    
    @Test
    @FormDeploymentAnnotation(resources = "org/flowable/form/engine/test/deployment/simple.form", tenantId="")
    public void getSimpleFormModelWithFallbackDefaultTenantWithVariables() throws Exception {
        String formDefinitionId = repositoryService.getFormModelByKey("form1", "flowable", true).getId();

        Map<String, Object> variables = new HashMap<>();
        variables.put("input1", "test");

        FormInfo formInfo = formService.getFormModelWithVariablesByKey("form1", null, variables, "flowable", true);

        assertThat(formInfo.getId()).isEqualTo(formDefinitionId);
        assertFormModel(formInfo);
    }
    
    @Test
    @FormDeploymentAnnotation(resources = "org/flowable/form/engine/test/deployment/simple.form", tenantId="defaultFlowable")
    public void getSimpleFormModelWithFallbackCustomTenantWithVariables() throws Exception {
        DefaultTenantProvider originalDefaultTenantValue = formEngineConfiguration.getDefaultTenantProvider();
        formEngineConfiguration.setDefaultTenantValue("defaultFlowable");
        try {
            String formDefinitionId = repositoryService.getFormModelByKey("form1", "flowable", true).getId();
    
            Map<String, Object> variables = new HashMap<>();
            variables.put("input1", "test");
    
            FormInfo formInfo = formService.getFormModelWithVariablesByKey("form1", null, variables, "flowable", true);
    
            assertThat(formInfo.getId()).isEqualTo(formDefinitionId);
            assertFormModel(formInfo);
            
        } finally {
            formEngineConfiguration.setDefaultTenantProvider(originalDefaultTenantValue);
        }
    }
    
    @Test
    @FormDeploymentAnnotation(resources = "org/flowable/form/engine/test/deployment/simple.form", tenantId="flowable")
    public void getSimpleFormModelWithFallbackCustomTenantNotExistingWithVariables() throws Exception {
        DefaultTenantProvider originalDefaultTenantProvider = formEngineConfiguration.getDefaultTenantProvider();
        formEngineConfiguration.setDefaultTenantValue("defaultFlowable");
        try {
            assertThatThrownBy(() -> repositoryService.getFormModelByKey("form1", "someTenant", true).getId())
                .isInstanceOf(FlowableObjectNotFoundException.class);

            Map<String, Object> variables = new HashMap<>();
            variables.put("input1", "test");

            assertThatThrownBy(() -> formService.getFormModelWithVariablesByKey("form1", null, variables, "someTenant", true))
                .isInstanceOf(FlowableObjectNotFoundException.class);

        } finally {
            formEngineConfiguration.setDefaultTenantProvider(originalDefaultTenantProvider);
        }
    }
    
    @Test
    @FormDeploymentAnnotation(resources = "org/flowable/form/engine/test/deployment/simple.form", tenantId="defaultFlowable")
    public void getSimpleFormModelWithGlobalFallbackCustomTenantWithVariables() throws Exception {
        DefaultTenantProvider originalDefaultTenantProvider = formEngineConfiguration.getDefaultTenantProvider();
        formEngineConfiguration.setFallbackToDefaultTenant(true);
        formEngineConfiguration.setDefaultTenantValue("defaultFlowable");
        try {
            String formDefinitionId = repositoryService.getFormModelByKey("form1", "flowable", false).getId();
    
            Map<String, Object> variables = new HashMap<>();
            variables.put("input1", "test");
    
            FormInfo formInfo = formService.getFormModelWithVariablesByKey("form1", null, variables, "flowable", false);
    
            assertThat(formInfo.getId()).isEqualTo(formDefinitionId);
            assertFormModel(formInfo);
            
        } finally {
            formEngineConfiguration.setFallbackToDefaultTenant(false);
            formEngineConfiguration.setDefaultTenantProvider(originalDefaultTenantProvider);
        }
    }
    
    @Test
    @FormDeploymentAnnotation(resources = "org/flowable/form/engine/test/deployment/simple.form", tenantId="flowable")
    public void getSimpleFormModelWithGlobalFallbackCustomTenantNotExistingWithVariables() throws Exception {
        DefaultTenantProvider originalDefaultTenantProvider = formEngineConfiguration.getDefaultTenantProvider();
        formEngineConfiguration.setFallbackToDefaultTenant(true);
        formEngineConfiguration.setDefaultTenantValue("defaultFlowable");
        try {
            assertThatThrownBy(() -> repositoryService.getFormModelByKey("form1", "someTenant", false).getId())
                .isInstanceOf(FlowableObjectNotFoundException.class);

            Map<String, Object> variables = new HashMap<>();
            variables.put("input1", "test");

            assertThatThrownBy(() -> formService.getFormModelWithVariablesByKey("form1", null, variables, "someTenant", false))
                .isInstanceOf(FlowableObjectNotFoundException.class);
            
        } finally {
            formEngineConfiguration.setFallbackToDefaultTenant(false);
            formEngineConfiguration.setDefaultTenantProvider(originalDefaultTenantProvider);
        }
    }
    
    protected void assertFormModel(FormInfo formInfo) {
        SimpleFormModel formModel = (SimpleFormModel) formInfo.getFormModel();
        assertThat(formModel.getFields()).hasSize(1);
        FormField formField = formModel.getFields().get(0);
        assertThat(formField.getId()).isEqualTo("input1");
        assertThat(formField.getValue()).isEqualTo("test");
    }

}
