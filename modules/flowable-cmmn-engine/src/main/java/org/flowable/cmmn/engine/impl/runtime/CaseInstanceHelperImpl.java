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
import org.flowable.cmmn.api.runtime.CaseInstanceBuilder;
import org.flowable.cmmn.api.runtime.CaseInstanceState;
import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.impl.deployer.CmmnDeploymentManager;
import org.flowable.cmmn.engine.impl.event.FlowableCmmnEventBuilder;
import org.flowable.cmmn.engine.impl.job.AsyncInitializePlanModelJobHandler;
import org.flowable.cmmn.engine.impl.listener.CaseInstanceLifeCycleListenerUtil;
import org.flowable.cmmn.engine.impl.persistence.entity.CaseDefinitionEntityManager;
import org.flowable.cmmn.engine.impl.persistence.entity.CaseInstanceEntity;
import org.flowable.cmmn.engine.impl.persistence.entity.CaseInstanceEntityManager;
import org.flowable.cmmn.engine.impl.persistence.entity.PlanItemInstanceEntity;
import org.flowable.cmmn.engine.impl.repository.CaseDefinitionUtil;
import org.flowable.cmmn.engine.impl.task.TaskHelper;
import org.flowable.cmmn.engine.impl.util.CmmnLoggingSessionUtil;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.cmmn.engine.impl.util.EntityLinkUtil;
import org.flowable.cmmn.engine.impl.util.EventInstanceCmmnUtil;
import org.flowable.cmmn.engine.interceptor.StartCaseInstanceAfterContext;
import org.flowable.cmmn.engine.interceptor.StartCaseInstanceBeforeContext;
import org.flowable.cmmn.model.Case;
import org.flowable.cmmn.model.CmmnModel;
import org.flowable.cmmn.model.ExtensionElement;
import org.flowable.cmmn.model.Stage;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.common.engine.api.delegate.Expression;
import org.flowable.common.engine.api.delegate.event.FlowableEventDispatcher;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.common.engine.impl.callback.CallbackData;
import org.flowable.common.engine.impl.callback.RuntimeInstanceStateChangeCallback;
import org.flowable.common.engine.impl.identity.Authentication;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.interceptor.EngineConfigurationConstants;
import org.flowable.common.engine.impl.logging.CmmnLoggingSessionConstants;
import org.flowable.eventregistry.api.runtime.EventInstance;
import org.flowable.eventregistry.impl.constant.EventConstants;
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
    
    protected CmmnEngineConfiguration cmmnEngineConfiguration;
    
    public CaseInstanceHelperImpl(CmmnEngineConfiguration cmmnEngineConfiguration) {
        this.cmmnEngineConfiguration = cmmnEngineConfiguration;
    }

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
        if (caseInstanceBuilder.getCaseDefinitionId() != null) {
            String caseDefinitionId = caseInstanceBuilder.getCaseDefinitionId();
            CmmnDeploymentManager deploymentManager = cmmnEngineConfiguration.getDeploymentManager();
            caseDefinition = deploymentManager.findDeployedCaseDefinitionById(caseDefinitionId);

        } else if (caseInstanceBuilder.getCaseDefinitionKey() != null) {
            String caseDefinitionKey = caseInstanceBuilder.getCaseDefinitionKey();
            CaseDefinitionEntityManager caseDefinitionEntityManager = cmmnEngineConfiguration.getCaseDefinitionEntityManager();
            String tenantId = caseInstanceBuilder.getTenantId();
            String parentDeploymentId = caseInstanceBuilder.getCaseDefinitionParentDeploymentId();
            if (tenantId == null || CmmnEngineConfiguration.NO_TENANT_ID.equals(tenantId)) {

                if (parentDeploymentId != null) {
                    caseDefinition = caseDefinitionEntityManager.findCaseDefinitionByParentDeploymentAndKey(parentDeploymentId, caseDefinitionKey);
                }

                if (caseDefinition == null) {
                    caseDefinition = caseDefinitionEntityManager.findLatestCaseDefinitionByKey(caseDefinitionKey);
                }

                if (caseDefinition == null) {
                    throw new FlowableObjectNotFoundException("No case definition found for key " + caseDefinitionKey, CaseDefinition.class);
                }
                
            } else if (!CmmnEngineConfiguration.NO_TENANT_ID.equals(tenantId)) {
                if (parentDeploymentId != null) {
                    caseDefinition = caseDefinitionEntityManager
                            .findCaseDefinitionByParentDeploymentAndKeyAndTenantId(parentDeploymentId, caseDefinitionKey, tenantId);
                }

                if (caseDefinition == null) {
                    caseDefinition = caseDefinitionEntityManager.findLatestCaseDefinitionByKeyAndTenantId(caseDefinitionKey, tenantId);
                }

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
        CmmnModel cmmnModel = getCmmnModel(commandContext, caseDefinition);
        Case caseModel = getCaseModel(caseDefinition, cmmnModel);
        CaseInstanceEntity caseInstanceEntity = initializeCaseInstanceEntity(commandContext, caseDefinition, 
                cmmnModel, caseModel, caseInstanceBuilder);

        // The InitPlanModelOperation will take care of initializing all the child plan items of that stage
        CommandContextUtil.getAgenda(commandContext).planInitPlanModelOperation(caseInstanceEntity);

        return caseInstanceEntity;
    }

    protected CaseInstanceEntity startCaseInstanceAsync(CommandContext commandContext, CaseDefinition caseDefinition, CaseInstanceBuilder caseInstanceBuilder) {
        CmmnModel cmmnModel = getCmmnModel(commandContext, caseDefinition);
        Case caseModel = getCaseModel(caseDefinition, cmmnModel);
        CaseInstanceEntity caseInstanceEntity = initializeCaseInstanceEntity(commandContext, caseDefinition, 
                cmmnModel, caseModel, caseInstanceBuilder);

        // create a job to execute InitPlanModelOperation, which will take care of initializing all the child plan items of that stage
        JobService jobService = cmmnEngineConfiguration.getJobServiceConfiguration().getJobService();
        createAsyncInitJob(caseInstanceEntity, caseDefinition, caseModel, jobService, commandContext);

        return caseInstanceEntity;
    }

    protected void createAsyncInitJob(CaseInstanceEntity caseInstance, CaseDefinition caseDefinition, 
            Case caseModel, JobService jobService, CommandContext commandContext) {
        
        JobEntity job = jobService.createJob();
        job.setJobHandlerType(AsyncInitializePlanModelJobHandler.TYPE);
        job.setScopeId(caseInstance.getId());
        job.setScopeDefinitionId(caseInstance.getCaseDefinitionId());
        job.setScopeType(ScopeTypes.CMMN);
        job.setElementId(caseDefinition.getId());
        job.setElementName(caseDefinition.getName());
        job.setJobHandlerConfiguration(caseInstance.getId());
        
        List<ExtensionElement> jobCategoryElements = caseModel.getExtensionElements().get("jobCategory");
        if (jobCategoryElements != null && jobCategoryElements.size() > 0) {
            ExtensionElement jobCategoryElement = jobCategoryElements.get(0);
            if (StringUtils.isNotEmpty(jobCategoryElement.getElementText())) {
                Expression categoryExpression = cmmnEngineConfiguration.getExpressionManager().createExpression(jobCategoryElement.getElementText());
                Object categoryValue = categoryExpression.getValue(caseInstance);
                if (categoryValue != null) {
                    job.setCategory(categoryValue.toString());
                }
            }
        }
        
        job.setTenantId(caseInstance.getTenantId());
        jobService.createAsyncJob(job, false);
        jobService.scheduleAsyncJob(job);
    }
    
    protected CmmnModel getCmmnModel(CommandContext commandContext, CaseDefinition caseDefinition) {
        CmmnEngineConfiguration cmmnEngineConfiguration = CommandContextUtil.getCmmnEngineConfiguration(commandContext);
        CmmnDeploymentManager deploymentManager = cmmnEngineConfiguration.getDeploymentManager();
        return deploymentManager.resolveCaseDefinition(caseDefinition).getCmmnModel();
    }
    
    protected Case getCaseModel(CaseDefinition caseDefinition, CmmnModel cmmnModel) {
        return cmmnModel.getCaseById(caseDefinition.getKey());
    }

    protected CaseInstanceEntity initializeCaseInstanceEntity(CommandContext commandContext, CaseDefinition caseDefinition, 
            CmmnModel cmmnModel, Case caseModel, CaseInstanceBuilder caseInstanceBuilder) {
        
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
        applyCaseInstanceBuilder(cmmnEngineConfiguration, caseInstanceBuilder, caseModel, caseInstanceEntity, caseDefinition, instanceBeforeContext, commandContext);

        if (cmmnEngineConfiguration.isEnableEntityLinks()) {
            if (CallbackTypes.PLAN_ITEM_CHILD_CASE.equals(caseInstanceEntity.getCallbackType())) {
                PlanItemInstanceEntity planItemInstanceEntity = cmmnEngineConfiguration.getPlanItemInstanceEntityManager()
                        .findById(caseInstanceEntity.getCallbackId());
                EntityLinkUtil.createEntityLinks(planItemInstanceEntity.getCaseInstanceId(), planItemInstanceEntity.getId(),
                        planItemInstanceEntity.getPlanItemDefinitionId(), caseInstanceEntity.getId(), ScopeTypes.CMMN, cmmnEngineConfiguration);
            }
        }

        if (cmmnEngineConfiguration.getStartCaseInstanceInterceptor() != null) {
            StartCaseInstanceAfterContext instanceAfterContext = new StartCaseInstanceAfterContext(caseInstanceEntity,
                caseInstanceBuilder.getVariables(), caseInstanceBuilder.getTransientVariables(), caseModel, caseDefinition, cmmnModel);

            cmmnEngineConfiguration.getStartCaseInstanceInterceptor().afterStartCaseInstance(instanceAfterContext);
        }

        CaseInstanceLifeCycleListenerUtil.callLifecycleListeners(commandContext, caseInstanceEntity, "", CaseInstanceState.ACTIVE);

        CallbackData callbackData = new CallbackData(caseInstanceEntity.getCallbackId(), caseInstanceEntity.getCallbackType(),
            caseInstanceEntity.getId(), null, CaseInstanceState.ACTIVE);
        callCaseInstanceStateChangeCallbacks(callbackData);
        cmmnEngineConfiguration.getCmmnHistoryManager().recordCaseInstanceStart(caseInstanceEntity);
        
        FlowableEventDispatcher eventDispatcher = cmmnEngineConfiguration.getEventDispatcher();
        if (eventDispatcher != null && eventDispatcher.isEnabled()) {
            eventDispatcher.dispatchEvent(FlowableCmmnEventBuilder.createCaseStartedEvent(caseInstanceEntity), EngineConfigurationConstants.KEY_CMMN_ENGINE_CONFIG);
        }

        if (cmmnEngineConfiguration.isLoggingSessionEnabled()) {
            CmmnLoggingSessionUtil.addLoggingData(CmmnLoggingSessionConstants.TYPE_CASE_STARTED, "Started case instance with id " + 
                    caseInstanceEntity.getId(), caseInstanceEntity, cmmnEngineConfiguration.getObjectMapper());
        }

        return caseInstanceEntity;
    }

    protected void applyCaseInstanceBuilder(CmmnEngineConfiguration cmmnEngineConfiguration, CaseInstanceBuilder caseInstanceBuilder, Case caseModel,
            CaseInstanceEntity caseInstanceEntity, CaseDefinition caseDefinition, StartCaseInstanceBeforeContext instanceBeforeContext, CommandContext commandContext) {
        
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

            Object eventInstance = caseInstanceEntity.getTransientVariable(EventConstants.EVENT_INSTANCE);
            if (eventInstance instanceof EventInstance) {
                EventInstanceCmmnUtil.handleEventInstanceOutParameters(caseInstanceEntity, caseModel, (EventInstance) eventInstance);
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

                    FormInfo formInfo = resolveFormInfo(planModel, caseDefinition, caseInstanceEntity.getTenantId(),
                            formRepositoryService, cmmnEngineConfiguration);

                    if (formInfo != null) {
                        FormFieldHandler formFieldHandler = cmmnEngineConfiguration.getFormFieldHandler();
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

    protected FormInfo resolveFormInfo(Stage planModel, CaseDefinition caseDefinition, String tenantId, FormRepositoryService formRepositoryService,
            CmmnEngineConfiguration cmmnEngineConfiguration) {
        String formKey = planModel.getFormKey();
        FormInfo formInfo;
        if (tenantId == null || CmmnEngineConfiguration.NO_TENANT_ID.equals(tenantId)) {
            if (planModel.isSameDeployment()) {
                String parentDeploymentId = CaseDefinitionUtil.getDefinitionDeploymentId(caseDefinition, cmmnEngineConfiguration);
                formInfo = formRepositoryService.getFormModelByKeyAndParentDeploymentId(formKey, parentDeploymentId);
            } else {
                formInfo = formRepositoryService.getFormModelByKey(formKey);
            }
        } else {
            if (planModel.isSameDeployment()) {
                String parentDeploymentId = CaseDefinitionUtil.getDefinitionDeploymentId(caseDefinition, cmmnEngineConfiguration);
                formInfo = formRepositoryService.getFormModelByKeyAndParentDeploymentId(formKey, parentDeploymentId, tenantId,
                        cmmnEngineConfiguration.isFallbackToDefaultTenant());
            } else {
                formInfo = formRepositoryService.getFormModelByKey(formKey, tenantId, cmmnEngineConfiguration.isFallbackToDefaultTenant());
            }
        }

        return formInfo;
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
        
        CaseInstanceEntityManager caseInstanceEntityManager = cmmnEngineConfiguration.getCaseInstanceEntityManager();
        CaseInstanceEntity caseInstanceEntity = caseInstanceEntityManager.create();
        
        if (instanceBeforeContext.getPredefinedCaseInstanceId() != null) {
            caseInstanceEntity.setId(instanceBeforeContext.getPredefinedCaseInstanceId());
        }
        
        caseInstanceEntity.setCaseDefinitionId(caseDefinition.getId());
        caseInstanceEntity.setCaseDefinitionKey(caseDefinition.getKey());
        caseInstanceEntity.setCaseDefinitionName(caseDefinition.getName());
        caseInstanceEntity.setCaseDefinitionVersion(caseDefinition.getVersion());
        caseInstanceEntity.setCaseDefinitionDeploymentId(caseDefinition.getDeploymentId());
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
    public void callCaseInstanceStateChangeCallbacks(CallbackData callbackData) {
        String callbackId = callbackData.getCallbackId();
        String callbackType = callbackData.getCallbackType();
        if (callbackId != null && callbackType != null) {
            Map<String, List<RuntimeInstanceStateChangeCallback>> caseInstanceCallbacks = cmmnEngineConfiguration.getCaseInstanceStateChangeCallbacks();
            if (caseInstanceCallbacks != null && caseInstanceCallbacks.containsKey(callbackType)) {
                for (RuntimeInstanceStateChangeCallback caseInstanceCallback : caseInstanceCallbacks.get(callbackType)) {
                    caseInstanceCallback.stateChanged(callbackData);
                }
            }
        }
    }

}
