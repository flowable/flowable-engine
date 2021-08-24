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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.flowable.cmmn.api.CallbackTypes;
import org.flowable.cmmn.api.event.FlowableCaseStartedEvent;
import org.flowable.cmmn.api.repository.CaseDefinition;
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.engine.impl.CmmnManagementServiceImpl;
import org.flowable.cmmn.engine.impl.persistence.entity.CaseInstanceEntity;
import org.flowable.cmmn.engine.impl.persistence.entity.PlanItemInstanceEntity;
import org.flowable.cmmn.engine.impl.runtime.CmmnRuntimeServiceImpl;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.engine.test.FlowableCmmnTestCase;
import org.flowable.cmmn.engine.test.impl.CmmnJobTestHelper;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.common.engine.api.delegate.event.AbstractFlowableEventListener;
import org.flowable.common.engine.api.delegate.event.FlowableEntityEvent;
import org.flowable.common.engine.api.delegate.event.FlowableEvent;
import org.flowable.common.engine.api.delegate.event.FlowableEventListener;
import org.flowable.common.engine.impl.DefaultTenantProvider;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.persistence.cache.EntityCache;
import org.flowable.job.api.Job;
import org.junit.Test;

/**
 * This class tests {@link CmmnRuntimeServiceImpl} implementation
 */
public class CmmnRuntimeServiceTest extends FlowableCmmnTestCase {

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/runtime/oneTaskCase.cmmn")
    public void createCaseInstanceWithCallBacks() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCase")
                .callbackId("testCallBackId")
                .callbackType(CallbackTypes.CASE_ADHOC_CHILD)
                .start();

        // in fact it must be possible to set any callbackType and Id
        assertThat(caseInstance.getCallbackType()).isEqualTo(CallbackTypes.CASE_ADHOC_CHILD);
        assertThat(caseInstance.getCallbackId()).isEqualTo("testCallBackId");
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/runtime/oneTaskCase.cmmn")
    public void createCaseInstanceWithoutCallBacks() {
        CmmnManagementServiceImpl cmmnManagementServiceImpl = (CmmnManagementServiceImpl) cmmnManagementService;
        List<PlanItemInstanceEntity> planItemInstances = cmmnManagementServiceImpl.executeCommand(new Command<List<PlanItemInstanceEntity>>() {
            
            @Override
            public List<PlanItemInstanceEntity> execute(CommandContext commandContext) {
                CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                        .caseDefinitionKey("oneTaskCase")
                        .start();
                
                EntityCache entityCache = CommandContextUtil.getEntityCache();
                List<PlanItemInstanceEntity> cachedPlanItemInstances = entityCache.findInCache(PlanItemInstanceEntity.class);
                List<PlanItemInstanceEntity> scopedPlanItemInstances = null;
                if (cachedPlanItemInstances != null && !cachedPlanItemInstances.isEmpty()) {
                    scopedPlanItemInstances = cachedPlanItemInstances.stream().filter(planItemInstance ->
                            Objects.equals(caseInstance.getId(), planItemInstance.getCaseInstanceId())).collect(Collectors.toList());
                }
                
                return scopedPlanItemInstances;
            }
        });
        
        assertThat(planItemInstances).hasSize(1);
        assertThat(planItemInstances.get(0).getPlanItemDefinitionId()).isEqualTo("theTask");

        // default values for callbacks are null
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceQuery().caseInstanceId(planItemInstances.get(0).getCaseInstanceId()).singleResult();
        assertThat(caseInstance.getCallbackType()).isNull();
        assertThat(caseInstance.getCallbackId()).isNull();
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/runtime/oneTaskCase.cmmn")
    public void updateCaseName() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCase")
                .start();

        // default name is empty
        assertThat(caseInstance.getName()).isNull();

        cmmnRuntimeService.setCaseInstanceName(caseInstance.getId(), "My case name");

        caseInstance = cmmnRuntimeService.createCaseInstanceQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertThat(caseInstance.getName()).isEqualTo("My case name");
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/runtime/oneTaskCase.cmmn")
    public void updateBusinessKey() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCase")
                .start();

        // default business key is empty
        assertThat(caseInstance.getName()).isNull();

        cmmnRuntimeService.updateBusinessKey(caseInstance.getId(), "bzKey");

        caseInstance = cmmnRuntimeService.createCaseInstanceQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertThat(caseInstance.getBusinessKey()).isEqualTo("bzKey");
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/runtime/oneTaskCase.cmmn")
    public void updateCaseNameSetEmpty() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCase")
                .start();

        // default name is empty
        assertThat(caseInstance.getName()).isNull();

        cmmnRuntimeService.setCaseInstanceName(caseInstance.getId(), "My case name");

        caseInstance = cmmnRuntimeService.createCaseInstanceQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertThat(caseInstance.getName()).isEqualTo("My case name");

        cmmnRuntimeService.setCaseInstanceName(caseInstance.getId(), null);

        caseInstance = cmmnRuntimeService.createCaseInstanceQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertThat(caseInstance.getName()).isNull();
    }

    @Test
    @CmmnDeployment
    public void createCaseInstanceAsyncDefinedInModel() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCase")
                .start();

        assertThat(caseInstance).isNotNull();
        assertThat(this.cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).count())
                .as("Plan items are created asynchronously").isZero();

        Job job = this.cmmnManagementService.createJobQuery().singleResult();
        assertThat(job).isNotNull();
        assertThat(job.isExclusive()).isFalse();

        CmmnJobTestHelper.waitForJobExecutorToProcessAllJobs(cmmnEngineConfiguration, 7000L, 200, true);
        assertThat(this.cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).count()).isEqualTo(1);
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/runtime/oneTaskCase.cmmn")
    public void createCaseInstanceAsync() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCase")
                .startAsync();

        assertThat(caseInstance).isNotNull();
        assertThat(this.cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).count())
                .as("Plan items are created asynchronously").isZero();

        Job job = this.cmmnManagementService.createJobQuery().singleResult();
        assertThat(job).isNotNull();
        assertThat(job.isExclusive()).isFalse();

        CmmnJobTestHelper.waitForJobExecutorToProcessAllJobs(cmmnEngineConfiguration, 7000L, 200, true);
        assertThat(this.cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).count()).isEqualTo(1);
    }

    @Test
    public void createCaseInstanceAsyncWithoutDef() {
        assertThatThrownBy(() -> cmmnRuntimeService.createCaseInstanceBuilder().startAsync())
                .isInstanceOf(FlowableIllegalArgumentException.class)
                .hasMessage("caseDefinitionKey and caseDefinitionId are null");
    }

    @Test
    public void createCaseInstanceAsyncWithNonExistingDefKey() {
        assertThatThrownBy(() -> cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("nonExistingDefinition").startAsync())
                .isInstanceOf(FlowableObjectNotFoundException.class)
                .hasMessage("No case definition found for key nonExistingDefinition");
    }

    @Test
    public void createCaseInstanceAsyncWithNonExistingDefId() {
        assertThatThrownBy(() -> cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionId("nonExistingDefinition").startAsync())
                .isInstanceOf(FlowableObjectNotFoundException.class)
                .hasMessage("no deployed case definition found with id 'nonExistingDefinition'");
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/runtime/oneTaskCase.cmmn")
    public void createCaseInstanceWithFallback() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCase")
                .tenantId("flowable")
                .overrideCaseDefinitionTenantId("flowable")
                .fallbackToDefaultTenant()
                .start();

        assertThat(caseInstance).isNotNull();
        assertThat(caseInstance.getTenantId()).isEqualTo("flowable");
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/runtime/oneTaskCase.cmmn", tenantId = "defaultFlowable")
    public void createCaseInstanceWithFallbackAndOverrideTenantId() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCase")
                .tenantId("defaultFlowable")
                .overrideCaseDefinitionTenantId("someTenant")
                .start();

        assertThat(caseInstance).isNotNull();
        assertThat(caseInstance.getTenantId()).isEqualTo("someTenant");
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/runtime/oneTaskCase.cmmn", tenantId = "defaultFlowable")
    public void createCaseInstanceWithGlobalFallbackAndDefaultTenantValue() {
        DefaultTenantProvider originalDefaultTenantProvider = cmmnEngineConfiguration.getDefaultTenantProvider();
        cmmnEngineConfiguration.setFallbackToDefaultTenant(true);
        cmmnEngineConfiguration.setDefaultTenantValue("defaultFlowable");
        try {
            CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                    .caseDefinitionKey("oneTaskCase")
                    .tenantId("someTenant")
                    .start();

            assertThat(caseInstance).isNotNull();
            assertThat(caseInstance.getTenantId()).isEqualTo("someTenant");

        } finally {
            cmmnEngineConfiguration.setFallbackToDefaultTenant(false);
            cmmnEngineConfiguration.setDefaultTenantProvider(originalDefaultTenantProvider);
        }
    }

    @Test
    public void createCaseInstanceWithFallbackDefinitionNotFound() {
        assertThatThrownBy(() -> cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCase")
                .tenantId("flowable")
                .fallbackToDefaultTenant()
                .start())
                .isInstanceOf(FlowableObjectNotFoundException.class)
                .hasMessage("Case definition was not found by key 'oneTaskCase'. Fallback to default tenant was also used.");
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/runtime/oneTaskCase.cmmn", tenantId = "tenant1")
    public void createCaseInstanceWithGlobalFallbackDefinitionNotFound() {
        DefaultTenantProvider originalDefaultTenantProvider = cmmnEngineConfiguration.getDefaultTenantProvider();
        cmmnEngineConfiguration.setFallbackToDefaultTenant(true);
        cmmnEngineConfiguration.setDefaultTenantValue("defaultFlowable");

        assertThatThrownBy(() -> cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCase")
                .tenantId("someTenant")
                .start())
                .isInstanceOf(FlowableObjectNotFoundException.class)
                .hasMessage("Case definition was not found by key 'oneTaskCase'. Fallback to default tenant was also used.");

        cmmnEngineConfiguration.setFallbackToDefaultTenant(false);
        cmmnEngineConfiguration.setDefaultTenantProvider(originalDefaultTenantProvider);
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/runtime/oneTaskCase.cmmn")
    public void createCaseInstanceAsyncWithFallback() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCase")
                .tenantId("flowable")
                .fallbackToDefaultTenant()
                .startAsync();

        assertThat(caseInstance).isNotNull();
    }

    @Test
    public void createCaseInstanceAsyncWithFallbackDefinitionNotFound() {
        assertThatThrownBy(() -> cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCase")
                .tenantId("flowable")
                .fallbackToDefaultTenant()
                .startAsync())
                .isInstanceOf(FlowableObjectNotFoundException.class)
                .hasMessage("Case definition was not found by key 'oneTaskCase'. Fallback to default tenant was also used.");
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/runtime/oneTaskCase.cmmn")
    public void createCaseInstanceHasCaseDefinitionInfo() {
        CaseDefinition caseDefinition = cmmnRepositoryService.createCaseDefinitionQuery().singleResult();
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCase")
                .start();

        assertThat(caseInstance.getCaseDefinitionKey()).isEqualTo("oneTaskCase");
        assertThat(caseInstance.getCaseDefinitionName()).isEqualTo("oneTaskCaseName");
        assertThat(caseInstance.getCaseDefinitionVersion()).isEqualTo(1);
        assertThat(caseInstance.getCaseDefinitionId()).isEqualTo(caseDefinition.getId());
        assertThat(caseInstance.getCaseDefinitionDeploymentId()).isEqualTo(caseDefinition.getDeploymentId());
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/runtime/oneTaskCase.cmmn")
    public void testNestedRuntimeServiceInvocation() {

        // The 'nested' here means that the runtime service will be called in a nested service call (with command context reuse)

        Boolean originalIsEnableEventDispatcher = null;
        FlowableEventListener eventListener = null;

        CaseInstance caseInstance = null;
        try {

            eventListener = new AbstractFlowableEventListener() {

                @Override
                public void onEvent(FlowableEvent event) {
                    Object entity = ((FlowableEntityEvent) event).getEntity();
                    if (event instanceof FlowableCaseStartedEvent) {
                        CaseInstanceEntity caseInstanceEntity = (CaseInstanceEntity) entity;
                        cmmnRuntimeService.setVariable(caseInstanceEntity.getId(), "testVariable", 123);
                    }
                }

                @Override
                public boolean isFailOnException() {
                    return false;
                }

            };

            originalIsEnableEventDispatcher = cmmnEngineConfiguration.isEnableEventDispatcher();

            cmmnEngineConfiguration.setEnableEventDispatcher(true);
            cmmnEngineConfiguration.getEventDispatcher().addEventListener(eventListener);

            caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("oneTaskCase").start();

        } finally {
            if (originalIsEnableEventDispatcher != null) {
                cmmnEngineConfiguration.setEnableEventDispatcher(originalIsEnableEventDispatcher);
            }
            if (eventListener != null) {
                cmmnEngineConfiguration.getEventDispatcher().removeEventListener(eventListener);
            }
        }

        assertThat(cmmnRuntimeService.getVariable(caseInstance.getId(), "testVariable")).isEqualTo(123);

    }

}
