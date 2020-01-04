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
package org.flowable.cmmn.engine.impl.runtime;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.flowable.cmmn.api.CallbackTypes;
import org.flowable.cmmn.api.repository.CaseDefinition;
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.api.runtime.CaseInstanceBuilder;
import org.flowable.cmmn.api.runtime.CaseInstanceState;
import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.impl.deployer.CmmnDeploymentManager;
import org.flowable.cmmn.engine.impl.job.AsyncInitializePlanModelJobHandler;
import org.flowable.cmmn.engine.impl.listener.CaseLifeCycleListenerUtil;
import org.flowable.cmmn.engine.impl.persistence.entity.CaseDefinitionEntityManager;
import org.flowable.cmmn.engine.impl.persistence.entity.CaseInstanceEntity;
import org.flowable.cmmn.engine.impl.persistence.entity.CaseInstanceEntityManager;
import org.flowable.cmmn.engine.impl.persistence.entity.PlanItemInstanceEntity;
import org.flowable.cmmn.engine.impl.repository.CaseDefinitionUtil;
import org.flowable.cmmn.engine.impl.task.TaskHelper;
import org.flowable.cmmn.engine.impl.util.CmmnLoggingSessionUtil;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.cmmn.engine.impl.util.EntityLinkUtil;
import org.flowable.cmmn.engine.interceptor.StartCaseInstanceAfterContext;
import org.flowable.cmmn.engine.interceptor.StartCaseInstanceBeforeContext;
import org.flowable.cmmn.model.Case;
import org.flowable.cmmn.model.CmmnModel;
import org.flowable.cmmn.model.Stage;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.common.engine.impl.callback.CallbackData;
import org.flowable.common.engine.impl.callback.RuntimeInstanceStateChangeCallback;
import org.flowable.common.engine.impl.identity.Authentication;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.logging.CmmnLoggingSessionConstants;
import org.flowable.form.api.FormFieldHandler;
import org.flowable.form.api.FormInfo;
import org.flowable.form.api.FormRepositoryService;
import org.flowable.form.api.FormService;
import org.flowable.job.service.JobService;
import org.flowable.job.service.impl.persistence.entity.JobEntity;
import org.flowable.variable.service.impl.el.NoExecutionVariableScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Joram Barrez
 */
public class CaseInstanceHelperImpl implements CaseInstanceHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(CaseInstanceHelperImpl.class);

    @Override
    public CaseInstanceEntity startCaseInstance(CaseInstanceBuilder caseInstanceBuilder) {
        CommandContext commandContext = CommandContextUtil.getCommandContext();
        return startCaseInstance(commandContext, getCaseDefinition(caseInstanceBuilder, commandContext), caseInstanceBuilder);
    }

    @Override
    public CaseInstanceEntity startCaseInstanceAsync(CaseInstanceBuilder caseInstanceBuilder) {
        CommandContext commandContext = CommandContextUtil.getCommandContext();
        return startCaseInstanceAsync(commandContext, getCaseDefinition(caseInstanceBuilder, commandContext), caseInstanceBuilder);
    }

    protected CaseDefinition getCaseDefinition(CaseInstanceBuilder caseInstanceBuilder, CommandContext commandContext) {
        CaseDefinition caseDefinition = null;
        CmmnEngineConfiguration cmmnEngineConfiguration = CommandContextUtil.getCmmnEngineConfiguration(commandContext);
        if (caseInstanceBuilder.getCaseDefinitionId() != null) {
            String caseDefinitionId = caseInstanceBuilder.getCaseDefinitionId();
            CaseDefinitionEntityManager definitionEntityManager = cmmnEngineConfiguration.getCaseDefinitionEntityManager();
            if (caseDefinitionId != null) {
                caseDefinition = definitionEntityManager.findById(caseDefinitionId);
                if (caseDefinition == null) {
                    throw new FlowableObjectNotFoundException("No case definition found for id " + caseDefinitionId, CaseDefinition.class);
                }
            }

        } else if (caseInstanceBuilder.getCaseDefinitionKey() != null) {
            String caseDefinitionKey = caseInstanceBuilder.getCaseDefinitionKey();
            CaseDefinitionEntityManager caseDefinitionEntityManager = cmmnEngineConfiguration.getCaseDefinitionEntityManager();
            String tenantId = caseInstanceBuilder.getTenantId();
            if (tenantId == null || CmmnEngineConfiguration.NO_TENANT_ID.equals(tenantId)) {
                caseDefinition = caseDefinitionEntityManager.findLatestCaseDefinitionByKey(caseDefinitionKey);
                if (caseDefinition == null) {
                    throw new FlowableObjectNotFoundException("No case definition found for key " + caseDefinitionKey, CaseDefinition.class);
                }
                
            } else if (!CmmnEngineConfiguration.NO_TENANT_ID.equals(tenantId)) {
                caseDefinition = caseDefinitionEntityManager.findLatestCaseDefinitionByKeyAndTenantId(caseDefinitionKey, tenantId);

                if (caseDefinition == null) {
                    if (caseInstanceBuilder.isFallbackToDefaultTenant() || cmmnEngineConfiguration.isFallbackToDefaultTenant()) {
                        String defaultTenant = cmmnEngineConfiguration.getDefaultTenantProvider().getDefaultTenant(tenantId, ScopeTypes.CMMN, caseDefinitionKey);
                        if (StringUtils.isNotEmpty(defaultTenant)) {
                            caseDefinition = caseDefinitionEntityManager.findLatestCaseDefinitionByKeyAndTenantId(caseDefinitionKey, defaultTenant);
                            caseInstanceBuilder.overrideCaseDefinitionTenantId(tenantId);
                            
                        } else {
                            caseDefinition = caseDefinitionEntityManager.findLatestCaseDefinitionByKey(caseDefinitionKey);
                        }
                        
                        if (caseDefinition == null) {
                            throw new FlowableObjectNotFoundException(
                                "Case definition was not found by key '" + caseDefinitionKey + "'. Fallback to default tenant was also used.");
                        }
                    } else {
                        throw new FlowableObjectNotFoundException(
                            "Case definition was not found by key '" + caseDefinitionKey + "' and tenant '" + tenantId + "'");
                    }
                }
            }
        } else {
            throw new FlowableIllegalArgumentException("caseDefinitionKey and caseDefinitionId are null");
        }
        return caseDefinition;
    }

    protected CaseInstanceEntity startCaseInstance(CommandContext commandContext, CaseDefinition caseDefinition, CaseInstanceBuilder caseInstanceBuilder) {
        CaseInstanceEntity caseInstanceEntity = initializeCaseInstanceEntity(commandContext, caseDefinition, caseInstanceBuilder);

        // The InitPlanModelOperation will take care of initializing all the child plan items of that stage
        CommandContextUtil.getAgenda(commandContext).planInitPlanModelOperation(caseInstanceEntity);

        return caseInstanceEntity;
    }

    protected CaseInstanceEntity startCaseInstanceAsync(CommandContext commandContext, CaseDefinition caseDefinition, CaseInstanceBuilder caseInstanceBuilder) {
        CaseInstanceEntity caseInstanceEntity = initializeCaseInstanceEntity(commandContext, caseDefinition, caseInstanceBuilder);

        // create a job to execute InitPlanModelOperation, which will take care of initializing all the child plan items of that stage
        JobService jobService = CommandContextUtil.getCmmnEngineConfiguration(commandContext).getJobServiceConfiguration().getJobService();
        createAsyncInitJob(caseInstanceEntity, caseDefinition, jobService);

        return caseInstanceEntity;
    }

    protected void createAsyncInitJob(CaseInstance caseInstance, CaseDefinition caseDefinition, JobService jobService) {
        JobEntity job = jobService.createJob();
        job.setJobHandlerType(AsyncInitializePlanModelJobHandler.TYPE);
        job.setScopeId(caseInstance.getId());
        job.setScopeDefinitionId(caseInstance.getCaseDefinitionId());
        job.setScopeType(ScopeTypes.CMMN);
        job.setElementId(caseDefinition.getId());
        job.setElementName(caseDefinition.getName());
        job.setJobHandlerConfiguration(caseInstance.getId());
        job.setTenantId(caseInstance.getTenantId());
        jobService.createAsyncJob(job, true);
        jobService.scheduleAsyncJob(job);
    }

    protected CaseInstanceEntity initializeCaseInstanceEntity(CommandContext commandContext, CaseDefinition caseDefinition, 
                    CaseInstanceBuilder caseInstanceBuilder) {
        
        CmmnEngineConfiguration cmmnEngineConfiguration = CommandContextUtil.getCmmnEngineConfiguration(commandContext);
        CmmnDeploymentManager deploymentManager = cmmnEngineConfiguration.getDeploymentManager();
        CmmnModel cmmnModel = deploymentManager.resolveCaseDefinition(caseDefinition).getCmmnModel();
        Case caseModel = cmmnModel.getCaseById(caseDefinition.getKey());

        StartCaseInstanceBeforeContext instanceBeforeContext = new StartCaseInstanceBeforeContext(caseInstanceBuilder.getBusinessKey(), caseInstanceBuilder.getName(),
                        caseInstanceBuilder.getCallbackId(), caseInstanceBuilder.getCallbackType(),
                        caseInstanceBuilder.getReferenceId(), caseInstanceBuilder.getReferenceType(),
                        caseInstanceBuilder.getParentId(), caseInstanceBuilder.getVariables(),
                        caseInstanceBuilder.getTransientVariables(), caseInstanceBuilder.getTenantId(), caseModel.getInitiatorVariableName(),
                        caseModel, caseDefinition, cmmnModel, caseInstanceBuilder.getOverrideDefinitionTenantId(), caseInstanceBuilder.getPredefinedCaseInstanceId());
        
        if (cmmnEngineConfiguration.getStartCaseInstanceInterceptor() != null) {
            cmmnEngineConfiguration.getStartCaseInstanceInterceptor().beforeStartCaseInstance(instanceBeforeContext);
        }
        
        CaseInstanceEntity caseInstanceEntity = createCaseInstanceEntityFromDefinition(commandContext, caseDefinition, instanceBeforeContext);
        applyCaseInstanceBuilder(cmmnEngineConfiguration, caseInstanceBuilder, caseInstanceEntity, caseDefinition, instanceBeforeContext, commandContext);

        if (cmmnEngineConfiguration.isEnableEntityLinks()) {
            if (CallbackTypes.PLAN_ITEM_CHILD_CASE.equals(caseInstanceEntity.getCallbackType())) {
                PlanItemInstanceEntity planItemInstanceEntity = CommandContextUtil
                    .getPlanItemInstanceEntityManager(commandContext).findById(caseInstanceEntity.getCallbackId());
                EntityLinkUtil.copyExistingEntityLinks(planItemInstanceEntity.getCaseInstanceId(), caseInstanceEntity.getId(), ScopeTypes.CMMN);
                EntityLinkUtil.createNewEntityLink(planItemInstanceEntity.getCaseInstanceId(), caseInstanceEntity.getId(), ScopeTypes.CMMN);
            }
        }

        if (cmmnEngineConfiguration.getStartCaseInstanceInterceptor() != null) {
            StartCaseInstanceAfterContext instanceAfterContext = new StartCaseInstanceAfterContext(caseInstanceEntity,
                caseInstanceBuilder.getVariables(), caseInstanceBuilder.getTransientVariables(), caseModel, caseDefinition, cmmnModel);

            cmmnEngineConfiguration.getStartCaseInstanceInterceptor().afterStartCaseInstance(instanceAfterContext);
        }

        CaseLifeCycleListenerUtil.callLifecycleListeners(commandContext, caseInstanceEntity, "", CaseInstanceState.ACTIVE);

        callCaseInstanceStateChangeCallbacks(commandContext, caseInstanceEntity, null, CaseInstanceState.ACTIVE);
        CommandContextUtil.getCmmnHistoryManager(commandContext).recordCaseInstanceStart(caseInstanceEntity);
        
        if (cmmnEngineConfiguration.isLoggingSessionEnabled()) {
            CmmnLoggingSessionUtil.addLoggingData(CmmnLoggingSessionConstants.TYPE_CASE_STARTED, "Started case instance with id " + 
                            caseInstanceEntity.getId(), caseInstanceEntity);
        }

        return caseInstanceEntity;
    }

    protected void applyCaseInstanceBuilder(CmmnEngineConfiguration cmmnEngineConfiguration, CaseInstanceBuilder caseInstanceBuilder, CaseInstanceEntity caseInstanceEntity,
                    CaseDefinition caseDefinition, StartCaseInstanceBeforeContext instanceBeforeContext, CommandContext commandContext) {
        
        if (instanceBeforeContext.getCaseInstanceName() != null) {
            caseInstanceEntity.setName(instanceBeforeContext.getCaseInstanceName());
        }

        if (instanceBeforeContext.getBusinessKey() != null) {
            caseInstanceEntity.setBusinessKey(instanceBeforeContext.getBusinessKey());
        }

        if (instanceBeforeContext.getOverrideDefinitionTenantId() != null) {
            caseInstanceEntity.setTenantId(instanceBeforeContext.getOverrideDefinitionTenantId());
        }

        if (instanceBeforeContext.getParentId() != null) {
            caseInstanceEntity.setParentId(instanceBeforeContext.getParentId());
        }

        if (instanceBeforeContext.getCallbackId() != null) {
            caseInstanceEntity.setCallbackId(instanceBeforeContext.getCallbackId());
        }

        if (instanceBeforeContext.getCallbackType() != null) {
            caseInstanceEntity.setCallbackType(instanceBeforeContext.getCallbackType());
        }

        if (instanceBeforeContext.getReferenceId() != null) {
            caseInstanceEntity.setReferenceId(instanceBeforeContext.getReferenceId());
        }

        if (instanceBeforeContext.getReferenceType() != null) {
            caseInstanceEntity.setReferenceType(instanceBeforeContext.getReferenceType());
        }

        if (cmmnEngineConfiguration.getIdentityLinkInterceptor() != null) {
            cmmnEngineConfiguration.getIdentityLinkInterceptor().handleCreateCaseInstance(caseInstanceEntity);
        }
        if (instanceBeforeContext.getInitiatorVariableName() != null) {
            caseInstanceEntity.setVariable(instanceBeforeContext.getInitiatorVariableName(), Authentication.getAuthenticatedUserId());
        }

        Map<String, Object> variables = instanceBeforeContext.getVariables();
        if (variables != null) {
            for (String variableName : variables.keySet()) {
                caseInstanceEntity.setVariable(variableName, variables.get(variableName));
            }
        }

        Map<String, Object> transientVariables = instanceBeforeContext.getTransientVariables();
        if (transientVariables != null) {
            for (String variableName : transientVariables.keySet()) {
                caseInstanceEntity.setTransientVariable(variableName, transientVariables.get(variableName));
            }
        }

        if (caseInstanceBuilder.isStartWithForm() || caseInstanceBuilder.getOutcome() != null) {
            Map<String, Object> startFormVariables = caseInstanceBuilder.getStartFormVariables();

            FormService formService = CommandContextUtil.getFormService(commandContext);

            CmmnModel cmmnModel = CaseDefinitionUtil.getCmmnModel(caseDefinition.getId());
            Case caze = cmmnModel.getCaseById(caseDefinition.getKey());
            Stage planModel = caze.getPlanModel();
            if (planModel != null && StringUtils.isNotEmpty(planModel.getFormKey())) {
                FormRepositoryService formRepositoryService = CommandContextUtil.getFormRepositoryService(commandContext);
                if (formRepositoryService != null) {

                    FormInfo formInfo = null;
                    if (caseInstanceEntity.getTenantId() == null || CmmnEngineConfiguration.NO_TENANT_ID.equals(caseInstanceEntity.getTenantId())) {
                        formInfo = formRepositoryService.getFormModelByKey(planModel.getFormKey());
                    } else {
                        formInfo = formRepositoryService.getFormModelByKey(planModel.getFormKey(), caseInstanceEntity.getTenantId(),
                                        cmmnEngineConfiguration.isFallbackToDefaultTenant());
                    }

                    if (formInfo != null) {
                        FormFieldHandler formFieldHandler = CommandContextUtil.getCmmnEngineConfiguration().getFormFieldHandler();
                        // validate input before anything else
                        if (isFormFieldValidationEnabled(cmmnEngineConfiguration, planModel)) {
                            formService.validateFormFields(formInfo, startFormVariables);
                        }
                        // Extract the caseVariables from the form submission variables and pass them to the case
                        Map<String, Object> caseVariables = formService.getVariablesFromFormSubmission(formInfo,
                            startFormVariables, caseInstanceBuilder.getOutcome());

                        if (caseVariables != null) {
	                        for (String variableName : caseVariables.keySet()) {
	                            caseInstanceEntity.setVariable(variableName, caseVariables.get(variableName));
	                        }
                        }

                        // The caseVariables are the variables that should be used when starting the case
                        // the actual variables should instead be used when creating the form instances
                        formService.createFormInstanceWithScopeId(startFormVariables, formInfo, null, caseInstanceEntity.getId(),
                            ScopeTypes.CMMN, caseInstanceEntity.getCaseDefinitionId(), caseInstanceEntity.getTenantId(), caseInstanceBuilder.getOutcome());
                        formFieldHandler.handleFormFieldsOnSubmit(formInfo, null, null,
                            caseInstanceEntity.getId(), ScopeTypes.CMMN, caseVariables, caseInstanceEntity.getTenantId());
                    }

                } else {
                    LOGGER.warn("Requesting form model {} without configured formRepositoryService", planModel.getFormKey());
                }
            }
        }

    }

    protected boolean isFormFieldValidationEnabled(CmmnEngineConfiguration cmmnEngineConfiguration, Stage stage) {
        if (cmmnEngineConfiguration.isFormFieldValidationEnabled()) {
            return TaskHelper.isFormFieldValidationEnabled(NoExecutionVariableScope.getSharedInstance(), // case instance does not exist yet
                cmmnEngineConfiguration, stage.getValidateFormFields()
            );
        }
        return false;
    }

    protected CaseInstanceEntity createCaseInstanceEntityFromDefinition(CommandContext commandContext, 
                    CaseDefinition caseDefinition, StartCaseInstanceBeforeContext instanceBeforeContext) {
        
        CaseInstanceEntityManager caseInstanceEntityManager = CommandContextUtil.getCaseInstanceEntityManager(commandContext);
        CaseInstanceEntity caseInstanceEntity = caseInstanceEntityManager.create();
        
        if (instanceBeforeContext.getPredefinedCaseInstanceId() != null) {
            caseInstanceEntity.setId(instanceBeforeContext.getPredefinedCaseInstanceId());
        }
        
        CmmnEngineConfiguration cmmnEngineConfiguration = CommandContextUtil.getCmmnEngineConfiguration(commandContext);
        caseInstanceEntity.setCaseDefinitionId(caseDefinition.getId());
        caseInstanceEntity.setStartTime(cmmnEngineConfiguration.getClock().getCurrentTime());
        caseInstanceEntity.setState(CaseInstanceState.ACTIVE);
        caseInstanceEntity.setTenantId(caseDefinition.getTenantId());

        String authenticatedUserId = Authentication.getAuthenticatedUserId();
        caseInstanceEntity.setStartUserId(authenticatedUserId);
        
        caseInstanceEntityManager.insert(caseInstanceEntity);
        caseInstanceEntity.setSatisfiedSentryPartInstances(new ArrayList<>(1));

        return caseInstanceEntity;
    }

    @Override
    public void callCaseInstanceStateChangeCallbacks(CommandContext commandContext, CaseInstance caseInstance, String oldState, String newState) {
        if (caseInstance.getCallbackId() != null && caseInstance.getCallbackType() != null) {
            Map<String, List<RuntimeInstanceStateChangeCallback>> caseInstanceCallbacks = CommandContextUtil
                    .getCmmnEngineConfiguration(commandContext).getCaseInstanceStateChangeCallbacks();
            if (caseInstanceCallbacks != null && caseInstanceCallbacks.containsKey(caseInstance.getCallbackType())) {
                for (RuntimeInstanceStateChangeCallback caseInstanceCallback : caseInstanceCallbacks.get(caseInstance.getCallbackType())) {
                    CallbackData callBackData = new CallbackData(caseInstance.getCallbackId(), caseInstance.getCallbackType(), caseInstance.getId(), oldState, newState);
                    caseInstanceCallback.stateChanged(callBackData);
                }
            }
        }
    }

}
