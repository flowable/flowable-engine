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
package org.flowable.cmmn.test.runtime;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import org.flowable.cmmn.api.CallbackTypes;
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.engine.impl.runtime.CmmnRuntimeServiceImpl;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.engine.test.FlowableCmmnTestCase;
import org.flowable.cmmn.engine.test.impl.CmmnJobTestHelper;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * This class tests {@link CmmnRuntimeServiceImpl} implementation
 */
public class CmmnRuntimeServiceTest extends FlowableCmmnTestCase {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/runtime/oneTaskCase.cmmn")
    public void createCaseInstanceWithCallBacks() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().
            caseDefinitionKey("oneTaskCase").
            callbackId("testCallBackId").
            callbackType(CallbackTypes.CASE_ADHOC_CHILD).
            start();

        // in fact it must be possible to set any callbackType and Id
        assertThat(caseInstance.getCallbackType(), is(CallbackTypes.CASE_ADHOC_CHILD));
        assertThat(caseInstance.getCallbackId(), is("testCallBackId"));
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/runtime/oneTaskCase.cmmn")
    public void createCaseInstanceWithoutCallBacks() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().
            caseDefinitionKey("oneTaskCase").
            start();

        // default values for callbacks are null
        assertThat(caseInstance.getCallbackType(), nullValue());
        assertThat(caseInstance.getCallbackId(), nullValue());
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/runtime/oneTaskCase.cmmn")
    public void updateCaseName() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().
            caseDefinitionKey("oneTaskCase").
            start();

        // default name is empty
        assertThat(caseInstance.getName(), nullValue());

        cmmnRuntimeService.setCaseInstanceName(caseInstance.getId(), "My case name");

        caseInstance = cmmnRuntimeService.createCaseInstanceQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertEquals("My case name", caseInstance.getName());
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/runtime/oneTaskCase.cmmn")
    public void updateBusinessKey() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().
            caseDefinitionKey("oneTaskCase").
            start();

        // default business key is empty
        assertThat(caseInstance.getName(), nullValue());

        cmmnRuntimeService.updateBusinessKey(caseInstance.getId(), "bzKey");

        caseInstance = cmmnRuntimeService.createCaseInstanceQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertEquals("bzKey", caseInstance.getBusinessKey());
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/runtime/oneTaskCase.cmmn")
    public void updateCaseNameSetEmpty() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().
            caseDefinitionKey("oneTaskCase").
            start();

        // default name is empty
        assertThat(caseInstance.getName(), nullValue());

        cmmnRuntimeService.setCaseInstanceName(caseInstance.getId(), "My case name");

        caseInstance = cmmnRuntimeService.createCaseInstanceQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertEquals("My case name", caseInstance.getName());

        cmmnRuntimeService.setCaseInstanceName(caseInstance.getId(), null);

        caseInstance = cmmnRuntimeService.createCaseInstanceQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertThat(caseInstance.getName(), nullValue());
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/runtime/oneTaskCase.cmmn")
    public void createCaseInstanceAsync() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().
            caseDefinitionKey("oneTaskCase").
            startAsync();

        assertThat(caseInstance, is(notNullValue()));
        assertThat("Plan items are created asynchronously", this.cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).count(), is(0l));

        CmmnJobTestHelper.waitForJobExecutorToProcessAllJobs(cmmnEngineConfiguration, 1000, 100, true);
        assertThat(this.cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).count(), is(1l));
    }

    @Test
    public void createCaseInstanceAsyncWithoutDef() {
        expectedException.expect(FlowableIllegalArgumentException.class);
        expectedException.expectMessage("caseDefinitionKey and caseDefinitionId are null");

        cmmnRuntimeService.createCaseInstanceBuilder().
            startAsync();
    }

    @Test
    public void createCaseInstanceAsyncWithNonExistingDefKey() {
        expectedException.expect(FlowableObjectNotFoundException.class);
        expectedException.expectMessage("No case definition found for key nonExistingDefinition");

        cmmnRuntimeService.createCaseInstanceBuilder().
            caseDefinitionKey("nonExistingDefinition").
            startAsync();
    }

    @Test
    public void createCaseInstanceAsyncWithNonExistingDefId() {
        expectedException.expect(FlowableObjectNotFoundException.class);
        expectedException.expectMessage("No case definition found for id nonExistingDefinition");

        cmmnRuntimeService.createCaseInstanceBuilder().
            caseDefinitionId("nonExistingDefinition").
            startAsync();
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/runtime/oneTaskCase.cmmn")
    public void createCaseInstanceWithFallback() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().
            caseDefinitionKey("oneTaskCase").
            tenantId("flowable").
            overrideCaseDefinitionTenantId("flowable").
            fallbackToDefaultTenant().
            start();

        assertThat(caseInstance, is(notNullValue()));
        assertThat(caseInstance.getTenantId(), is("flowable"));
    }
    
    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/runtime/oneTaskCase.cmmn", tenantId="defaultFlowable")
    public void createCaseInstanceWithFallbackAndOverrideTenantId() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().
            caseDefinitionKey("oneTaskCase").
            tenantId("defaultFlowable").
            overrideCaseDefinitionTenantId("someTenant").
            start();

        assertThat(caseInstance, is(notNullValue()));
        assertThat(caseInstance.getTenantId(), is("someTenant"));
    }
    
    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/runtime/oneTaskCase.cmmn", tenantId="defaultFlowable")
    public void createCaseInstanceWithGlobalFallbackAndDefaultTenantValue() {
        String originalDefaultTenantValue = cmmnEngineConfiguration.getDefaultTenantValue();
        cmmnEngineConfiguration.setFallbackToDefaultTenant(true);
        cmmnEngineConfiguration.setDefaultTenantValue("defaultFlowable");
        try {
            CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().
                caseDefinitionKey("oneTaskCase").
                tenantId("someTenant").
                start();
    
            assertThat(caseInstance, is(notNullValue()));
            assertThat(caseInstance.getTenantId(), is("someTenant"));
            
        } finally {
            cmmnEngineConfiguration.setFallbackToDefaultTenant(false);
            cmmnEngineConfiguration.setDefaultTenantValue(originalDefaultTenantValue);
        }
    }

    @Test
    public void createCaseInstanceWithFallbackDefinitionNotFound() {
        this.expectedException.expect(FlowableObjectNotFoundException.class);
        this.expectedException.expectMessage("Case definition was not found by key 'oneTaskCase'. Fallback to default tenant was also used.");

        cmmnRuntimeService.createCaseInstanceBuilder().
            caseDefinitionKey("oneTaskCase").
            tenantId("flowable").
            fallbackToDefaultTenant().
            start();
    }
    
    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/runtime/oneTaskCase.cmmn", tenantId="tenant1")
    public void createCaseInstanceWithGlobalFallbackDefinitionNotFound() {
        String originalDefaultTenantValue = cmmnEngineConfiguration.getDefaultTenantValue();
        cmmnEngineConfiguration.setFallbackToDefaultTenant(true);
        cmmnEngineConfiguration.setDefaultTenantValue("defaultFlowable");
        
        this.expectedException.expect(FlowableObjectNotFoundException.class);
        
        try {
            cmmnRuntimeService.createCaseInstanceBuilder().
                caseDefinitionKey("oneTaskCase").
                tenantId("someTenant").
                start();
            
        } finally {
            cmmnEngineConfiguration.setFallbackToDefaultTenant(false);
            cmmnEngineConfiguration.setDefaultTenantValue(originalDefaultTenantValue);
        }
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/runtime/oneTaskCase.cmmn")
    public void createCaseInstanceAsyncWithFallback() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().
            caseDefinitionKey("oneTaskCase").
            tenantId("flowable").
            fallbackToDefaultTenant().
            startAsync();

        assertThat(caseInstance, is(notNullValue()));
    }

    @Test
    public void createCaseInstanceAsyncWithFallbackDefinitionNotFound() {
        this.expectedException.expect(FlowableObjectNotFoundException.class);
        this.expectedException.expectMessage("Case definition was not found by key 'oneTaskCase'. Fallback to default tenant was also used.");

        cmmnRuntimeService.createCaseInstanceBuilder().
            caseDefinitionKey("oneTaskCase").
            tenantId("flowable").
            fallbackToDefaultTenant().
            startAsync();
    }

}
