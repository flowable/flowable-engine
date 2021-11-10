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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.flowable.cmmn.api.CallbackTypes;
import org.flowable.cmmn.api.history.HistoricCaseInstance;
import org.flowable.cmmn.api.history.HistoricPlanItemInstance;
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
import org.flowable.cmmn.engine.impl.persistence.entity.HistoricPlanItemInstanceEntityManager;
import org.flowable.cmmn.engine.impl.persistence.entity.PlanItemInstanceEntity;
import org.flowable.cmmn.engine.impl.persistence.entity.PlanItemInstanceEntityManager;
import org.flowable.cmmn.engine.impl.repository.CaseDefinitionUtil;
import org.flowable.cmmn.engine.impl.task.TaskHelper;
import org.flowable.cmmn.engine.impl.util.CmmnLoggingSessionUtil;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.cmmn.engine.impl.util.EntityLinkUtil;
import org.flowable.cmmn.engine.impl.util.EventInstanceCmmnUtil;
import org.flowable.cmmn.engine.impl.util.JobUtil;
import org.flowable.cmmn.engine.interceptor.StartCaseInstanceAfterContext;
import org.flowable.cmmn.engine.interceptor.StartCaseInstanceBeforeContext;
import org.flowable.cmmn.model.Case;
import org.flowable.cmmn.model.CmmnModel;
import org.flowable.cmmn.model.ReactivateEventListener;
import org.flowable.cmmn.model.Stage;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.FlowableIllegalStateException;
import org.flowable.common.engine.api.FlowableObjectNotFoundException;
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
import org.flowable.variable.api.history.HistoricVariableInstance;
import org.flowable.variable.api.types.VariableTypes;
import org.flowable.variable.service.VariableService;
import org.flowable.variable.service.impl.el.NoExecutionVariableScope;
import org.flowable.variable.service.impl.persistence.entity.VariableInstanceEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Joram Barrez
 * @author Micha Kiener
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

    @Override
    public CaseInstanceEntity copyHistoricCaseInstanceToRuntime(HistoricCaseInstance caseInstance) {
        CommandContext commandContext = CommandContextUtil.getCommandContext();
        CaseDefinitionEntityManager caseDefinitionEntityManager = cmmnEngineConfiguration.getCaseDefinitionEntityManager();
        CaseDefinition caseDefinition = caseDefinitionEntityManager.findById(caseInstance.getCaseDefinitionId());
        CaseInstanceEntity caseInstanceEntity = copyHistoricCaseInstanceToRuntime(commandContext, caseDefinition, caseInstance);
        return caseInstanceEntity;
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

        if (!caseModel.isAsync()) {
            // The InitPlanModelOperation will take care of initializing all the child plan items of that stage
            CommandContextUtil.getAgenda(commandContext).planInitPlanModelOperation(caseInstanceEntity);

            CaseInstanceLifeCycleListenerUtil.callLifecycleListeners(commandContext, caseInstanceEntity, "", CaseInstanceState.ACTIVE);

            FlowableEventDispatcher eventDispatcher = cmmnEngineConfiguration.getEventDispatcher();
            if (eventDispatcher != null && eventDispatcher.isEnabled()) {
                eventDispatcher.dispatchEvent(FlowableCmmnEventBuilder.createCaseStartedEvent(caseInstanceEntity), EngineConfigurationConstants.KEY_CMMN_ENGINE_CONFIG);
            }

            if (cmmnEngineConfiguration.isLoggingSessionEnabled()) {
                CmmnLoggingSessionUtil.addLoggingData(CmmnLoggingSessionConstants.TYPE_CASE_STARTED, "Started case instance with id " +
                        caseInstanceEntity.getId(), caseInstanceEntity, cmmnEngineConfiguration.getObjectMapper());
            }
        } else {
            // create a job to execute InitPlanModelOperation, which will take care of initializing all the child plan items of that stage
            JobService jobService = cmmnEngineConfiguration.getJobServiceConfiguration().getJobService();
            createAsyncInitJob(caseInstanceEntity, caseDefinition, caseModel, jobService, commandContext);
        }

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

    /**
     * This is the first part of reactivating a case instance from the history. It copies the historic data back to the runtime which is the case instance,
     * its plan items and the variables. This method does not trigger the reactivation listener, just checks, if it is there, but there is no reactivation
     * of plan items, etc. Just the copy of the historic data back to the runtime.
     *
     * @param commandContext the command context to execute within
     * @param caseDefinition the case definition to get the case model from
     * @param caseInstance the historic case instance to copy back to the runtime
     * @return the copied runtime case instance entity for further processing
     */
    protected CaseInstanceEntity copyHistoricCaseInstanceToRuntime(CommandContext commandContext, CaseDefinition caseDefinition, HistoricCaseInstance caseInstance) {
        CmmnModel cmmnModel = getCmmnModel(commandContext, caseDefinition);
        Case caseModel = getCaseModel(caseDefinition, cmmnModel);

        ReactivateEventListener listener = caseModel.getReactivateEventListener();
        if (listener == null) {
            // the reactivation event listener must be present in order to reactivate the case, there is no generic way as it is always business driven
            // on what happens during reactivation
            throw new FlowableIllegalStateException("The historic case instance " + caseInstance.getId()
                + " cannot be reactivated as there is no reactivation event in its CMMN model. You need to explicitly model the reactivation event in order to support case reactivation.");
        }

        // recreate the case instance in the runtime data table from the historic one
        return createCaseInstanceEntityFromHistoricCaseInstance(commandContext, caseInstance);
    }

    protected void createAsyncInitJob(CaseInstanceEntity caseInstance, CaseDefinition caseDefinition, 
            Case caseModel, JobService jobService, CommandContext commandContext) {
        
        JobEntity job = JobUtil.createJob(caseInstance, caseModel, AsyncInitializePlanModelJobHandler.TYPE, cmmnEngineConfiguration);
        job.setElementId(caseDefinition.getId());
        job.setElementName(caseDefinition.getName());
        job.setJobHandlerConfiguration(caseInstance.getId());
        
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

        StartCaseInstanceBeforeContext instanceBeforeContext = new StartCaseInstanceBeforeContext(caseInstanceBuilder.getBusinessKey(),
                caseInstanceBuilder.getBusinessStatus(), caseInstanceBuilder.getName(), caseInstanceBuilder.getCallbackId(),
                caseInstanceBuilder.getCallbackType(), caseInstanceBuilder.getReferenceId(), caseInstanceBuilder.getReferenceType(),
                caseInstanceBuilder.getParentId(), caseInstanceBuilder.getVariables(), caseInstanceBuilder.getTransientVariables(),
                caseInstanceBuilder.getTenantId(), caseModel.getInitiatorVariableName(), caseModel, caseDefinition, cmmnModel,
                caseInstanceBuilder.getOverrideDefinitionTenantId(), caseInstanceBuilder.getPredefinedCaseInstanceId());

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

        CallbackData callbackData = new CallbackData(caseInstanceEntity.getCallbackId(), caseInstanceEntity.getCallbackType(),
            caseInstanceEntity.getId(), null, CaseInstanceState.ACTIVE);
        callCaseInstanceStateChangeCallbacks(callbackData);
        cmmnEngineConfiguration.getCmmnHistoryManager().recordCaseInstanceStart(caseInstanceEntity);

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
        if (instanceBeforeContext.getBusinessStatus() != null) {
            caseInstanceEntity.setBusinessStatus(instanceBeforeContext.getBusinessStatus());
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

    /**
     * Creates a new runtime case instance based on the given historic one by copying all of its data, but setting its state to active again. Plan items as
     * well as variables are copied to the runtime as well. The historic instance is reflecting the same state as well as it is no longer terminated.
     *
     * @param commandContext the command context to execute within
     * @param historicCaseInstance the historic case instance to be copied back to the runtime
     * @return the newly created runtime case instance, initialized from the historic one
     */
    protected CaseInstanceEntity createCaseInstanceEntityFromHistoricCaseInstance(CommandContext commandContext, HistoricCaseInstance historicCaseInstance) {
        CaseInstanceEntityManager caseInstanceEntityManager = cmmnEngineConfiguration.getCaseInstanceEntityManager();

        // copy the case variables first so we can directly set it on the new case instance so they don't get reloaded later
        Map<String, VariableInstanceEntity> variables = createCaseVariablesFromHistoricCaseInstance(historicCaseInstance);
        CaseInstanceEntity caseInstanceEntity = caseInstanceEntityManager.create(historicCaseInstance, variables);
        caseInstanceEntityManager.insert(caseInstanceEntity);

        // create runtime plan items from the history and set them as the new child plan item list
        caseInstanceEntity.setChildPlanItemInstances(createCasePlanItemsFromHistoricCaseInstance(historicCaseInstance, caseInstanceEntity));

        return caseInstanceEntity;
    }

    /**
     * Creates new plan item instances for the runtime according the historic ones, even though they are all completed, ended or terminated. Later on, they
     * might be reactivated according the case model, but this is not part of this method.
     *
     * @param historicCaseInstance the historic case instance to copy the plan items from
     * @param newCaseInstance the newly created runtime copy of the historic case instance where the new plan items are attached to
     * @return the list of newly copied plan item instances in the runtime
     */
    protected List<PlanItemInstanceEntity> createCasePlanItemsFromHistoricCaseInstance(HistoricCaseInstance historicCaseInstance, CaseInstanceEntity newCaseInstance) {
        HistoricPlanItemInstanceEntityManager planItemInstanceEntityManager = cmmnEngineConfiguration.getHistoricPlanItemInstanceEntityManager();
        PlanItemInstanceEntityManager historicPlanItemInstanceEntityManager = cmmnEngineConfiguration.getPlanItemInstanceEntityManager();

        // move plan items back to runtime data, all of them as we will loop through them later to see which ones need to be reactivated according the
        // reactivation sentry modeling, but this will be done as part of the reactivation operation on the agenda
        List<HistoricPlanItemInstance> historicPlanItemInstances = planItemInstanceEntityManager.createHistoricPlanItemInstanceQuery()
            .planItemInstanceCaseInstanceId(historicCaseInstance.getId())
            .list();

        List<PlanItemInstanceEntity> planItemInstances = new ArrayList<>(historicPlanItemInstances.size());
        for (HistoricPlanItemInstance historicPlanItemInstance : historicPlanItemInstances) {
            // create a new plan item instance in the runtime table with exactly the same data as in the history (even the id, this way we don't even have to
            // rebuild the tree of stages and its child plan items and later on, they get updated from the runtime as well)
            PlanItemInstanceEntity newPlanItemInstance = historicPlanItemInstanceEntityManager.create(historicPlanItemInstance);
            historicPlanItemInstanceEntityManager.insert(newPlanItemInstance);
            planItemInstances.add(newPlanItemInstance);
        }

        return planItemInstances;
    }

    /**
     * Creates new variables in the runtime according the history of the provided case instance.
     *
     * @param historicCaseInstance the historic case instance to copy its variables back to the runtime
     * @return the map of the created variables
     */
    protected Map<String, VariableInstanceEntity> createCaseVariablesFromHistoricCaseInstance(HistoricCaseInstance historicCaseInstance) {
        VariableService variableService = cmmnEngineConfiguration.getVariableServiceConfiguration().getVariableService();
        VariableTypes variableTypes = cmmnEngineConfiguration.getVariableTypes();
        List<HistoricVariableInstance> variables = cmmnEngineConfiguration.getCmmnHistoryService()
            .createHistoricVariableInstanceQuery()
            .caseInstanceId(historicCaseInstance.getId())
            .list();

        if (variables != null) {
            Map<String, VariableInstanceEntity> newVars = new HashMap<>(variables.size());
            for (HistoricVariableInstance variable : variables) {
                // only make a copy, if it is a case variable, we don't copy locally scoped ones (e.g. from stages), as those plan items are practically
                // finished
                if (variable.getSubScopeId() == null) {
                    VariableInstanceEntity newVariable = variableService.createVariableInstance(
                        variable.getVariableName(), variableTypes.getVariableType(variable.getVariableTypeName()), variable.getValue());

                    newVariable.setId(variable.getId());
                    newVariable.setScopeId(historicCaseInstance.getId());
                    newVariable.setScopeType(variable.getScopeType());

                    variableService.insertVariableInstance(newVariable);
                    newVars.put(newVariable.getName(), newVariable);
                }
            }
            return newVars;
        }
        return Collections.emptyMap();
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
