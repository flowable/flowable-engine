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
import static org.junit.Assert.fail;

import java.util.HashMap;
import java.util.Map;

import org.flowable.common.engine.api.FlowableObjectNotFoundException;
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

        assertEquals(formDefinitionId, formInfo.getId());
        assertFormModel(formInfo);
    }
    
    @Test
    @FormDeploymentAnnotation(resources = "org/flowable/form/engine/test/deployment/simple.form", tenantId="flowable")
    public void getSimpleFormModelInTenantWithVariables() throws Exception {
        String formDefinitionId = repositoryService.getFormModelByKey("form1", "flowable", false).getId();

        Map<String, Object> variables = new HashMap<>();
        variables.put("input1", "test");

        FormInfo formInfo = formService.getFormModelWithVariablesByKey("form1", null, variables, "flowable", false);

        assertEquals(formDefinitionId, formInfo.getId());
        assertFormModel(formInfo);
    }
    
    @Test
    @FormDeploymentAnnotation(resources = "org/flowable/form/engine/test/deployment/simple.form", tenantId="flowable")
    public void getSimpleFormModelInAnotherTenantWithVariables() throws Exception {
        try {
            repositoryService.getFormModelByKey("form1", "someTenant", false).getId();
            fail("expected exception");
            
        } catch (FlowableObjectNotFoundException e) {
            // expected
        }

        Map<String, Object> variables = new HashMap<>();
        variables.put("input1", "test");

        try {
            formService.getFormModelWithVariablesByKey("form1", null, variables, "someTenant", false);
            fail("expected exception");
            
        } catch (FlowableObjectNotFoundException e) {
            // expected
        }
    }
    
    @Test
    @FormDeploymentAnnotation(resources = "org/flowable/form/engine/test/deployment/simple.form", tenantId="")
    public void getSimpleFormModelWithFallbackDefaultTenantWithVariables() throws Exception {
        String formDefinitionId = repositoryService.getFormModelByKey("form1", "flowable", true).getId();

        Map<String, Object> variables = new HashMap<>();
        variables.put("input1", "test");

        FormInfo formInfo = formService.getFormModelWithVariablesByKey("form1", null, variables, "flowable", true);

        assertEquals(formDefinitionId, formInfo.getId());
        assertFormModel(formInfo);
    }
    
    @Test
    @FormDeploymentAnnotation(resources = "org/flowable/form/engine/test/deployment/simple.form", tenantId="defaultFlowable")
    public void getSimpleFormModelWithFallbackCustomTenantWithVariables() throws Exception {
        String originalDefaultTenantValue = formEngineConfiguration.getDefaultTenantValue();
        formEngineConfiguration.setDefaultTenantValue("defaultFlowable");
        try {
            String formDefinitionId = repositoryService.getFormModelByKey("form1", "flowable", true).getId();
    
            Map<String, Object> variables = new HashMap<>();
            variables.put("input1", "test");
    
            FormInfo formInfo = formService.getFormModelWithVariablesByKey("form1", null, variables, "flowable", true);
    
            assertEquals(formDefinitionId, formInfo.getId());
            assertFormModel(formInfo);
            
        } finally {
            formEngineConfiguration.setDefaultTenantValue(originalDefaultTenantValue);
        }
    }
    
    @Test
    @FormDeploymentAnnotation(resources = "org/flowable/form/engine/test/deployment/simple.form", tenantId="flowable")
    public void getSimpleFormModelWithFallbackCustomTenantNotExistingWithVariables() throws Exception {
        String originalDefaultTenantValue = formEngineConfiguration.getDefaultTenantValue();
        formEngineConfiguration.setDefaultTenantValue("defaultFlowable");
        try {
            try {
                repositoryService.getFormModelByKey("form1", "someTenant", true).getId();
                fail("expected exception");
                
            } catch (FlowableObjectNotFoundException e) {
                // expected
            }
    
            Map<String, Object> variables = new HashMap<>();
            variables.put("input1", "test");
    
            try {
                formService.getFormModelWithVariablesByKey("form1", null, variables, "someTenant", true);
                fail("expected exception");
                
            } catch (FlowableObjectNotFoundException e) {
                // expected
            }
            
        } finally {
            formEngineConfiguration.setDefaultTenantValue(originalDefaultTenantValue);
        }
    }
    
    @Test
    @FormDeploymentAnnotation(resources = "org/flowable/form/engine/test/deployment/simple.form", tenantId="defaultFlowable")
    public void getSimpleFormModelWithGlobalFallbackCustomTenantWithVariables() throws Exception {
        String originalDefaultTenantValue = formEngineConfiguration.getDefaultTenantValue();
        formEngineConfiguration.setFallbackToDefaultTenant(true);
        formEngineConfiguration.setDefaultTenantValue("defaultFlowable");
        try {
            String formDefinitionId = repositoryService.getFormModelByKey("form1", "flowable", false).getId();
    
            Map<String, Object> variables = new HashMap<>();
            variables.put("input1", "test");
    
            FormInfo formInfo = formService.getFormModelWithVariablesByKey("form1", null, variables, "flowable", false);
    
            assertEquals(formDefinitionId, formInfo.getId());
            assertFormModel(formInfo);
            
        } finally {
            formEngineConfiguration.setFallbackToDefaultTenant(false);
            formEngineConfiguration.setDefaultTenantValue(originalDefaultTenantValue);
        }
    }
    
    @Test
    @FormDeploymentAnnotation(resources = "org/flowable/form/engine/test/deployment/simple.form", tenantId="flowable")
    public void getSimpleFormModelWithGlobalFallbackCustomTenantNotExistingWithVariables() throws Exception {
        String originalDefaultTenantValue = formEngineConfiguration.getDefaultTenantValue();
        formEngineConfiguration.setFallbackToDefaultTenant(true);
        formEngineConfiguration.setDefaultTenantValue("defaultFlowable");
        try {
            try {
                repositoryService.getFormModelByKey("form1", "someTenant", false).getId();
                fail("expected exception");
                
            } catch (FlowableObjectNotFoundException e) {
                // expected
            }
    
            Map<String, Object> variables = new HashMap<>();
            variables.put("input1", "test");
    
            try {
                formService.getFormModelWithVariablesByKey("form1", null, variables, "someTenant", false);
                fail("expected exception");
                
            } catch (FlowableObjectNotFoundException e) {
                // expected
            }
            
        } finally {
            formEngineConfiguration.setFallbackToDefaultTenant(false);
            formEngineConfiguration.setDefaultTenantValue(originalDefaultTenantValue);
        }
    }
    
    protected void assertFormModel(FormInfo formInfo) {
        SimpleFormModel formModel = (SimpleFormModel) formInfo.getFormModel();
        assertEquals(1, formModel.getFields().size());
        FormField formField = formModel.getFields().get(0);
        assertEquals("input1", formField.getId());
        assertEquals("test", formField.getValue());
    }

}
