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

import org.flowable.cmmn.api.repository.CaseDefinition;
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.api.runtime.CaseInstanceBuilder;
import org.flowable.cmmn.api.runtime.CaseInstanceState;
import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.impl.deployer.CmmnDeploymentManager;
import org.flowable.cmmn.engine.impl.job.AsyncInitializePlanModelJobHandler;
import org.flowable.cmmn.engine.impl.persistence.entity.CaseInstanceEntity;
import org.flowable.cmmn.engine.impl.persistence.entity.CaseInstanceEntityManager;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.cmmn.engine.impl.util.IdentityLinkUtil;
import org.flowable.cmmn.model.Case;
import org.flowable.cmmn.model.CmmnModel;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.common.engine.impl.callback.CallbackData;
import org.flowable.common.engine.impl.callback.RuntimeInstanceStateChangeCallback;
import org.flowable.common.engine.impl.identity.Authentication;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.identitylink.api.IdentityLinkType;
import org.flowable.job.service.JobService;
import org.flowable.job.service.impl.persistence.entity.JobEntity;

/**
 * @author Joram Barrez
 */
public class CaseInstanceHelperImpl implements CaseInstanceHelper {

    @Override
    public CaseInstanceEntity startCaseInstance(CaseInstanceBuilder caseInstanceBuilder) {
        CommandContext commandContext = CommandContextUtil.getCommandContext();

        return startCaseInstance(
            commandContext,
            getCaseDefinition(caseInstanceBuilder, commandContext),
            caseInstanceBuilder
        );
    }

    @Override
    public CaseInstanceEntity startCaseInstanceAsync(CaseInstanceBuilder caseInstanceBuilder) {
        CommandContext commandContext = CommandContextUtil.getCommandContext();

        return startCaseInstanceAsync(
            commandContext,
            getCaseDefinition(caseInstanceBuilder, commandContext),
            caseInstanceBuilder
        );
    }

    protected CaseDefinition getCaseDefinition(CaseInstanceBuilder caseInstanceBuilder, CommandContext commandContext) {
        CaseDefinition caseDefinition = null;
        if (caseInstanceBuilder.getCaseDefinitionId() != null) {
            String caseDefinitionId = caseInstanceBuilder.getCaseDefinitionId();
            CmmnDeploymentManager deploymentManager = CommandContextUtil.getCmmnEngineConfiguration(commandContext).getDeploymentManager();
            if (caseDefinitionId != null) {
                caseDefinition = deploymentManager.findDeployedCaseDefinitionById(caseDefinitionId);
                if (caseDefinition == null) {
                    throw new FlowableObjectNotFoundException("No case definition found for id " + caseDefinitionId, CaseDefinition.class);
                }
            }

        } else if (caseInstanceBuilder.getCaseDefinitionKey() != null) {
            String caseDefinitionKey = caseInstanceBuilder.getCaseDefinitionKey();
            CmmnDeploymentManager deploymentManager = CommandContextUtil.getCmmnEngineConfiguration(commandContext).getDeploymentManager();
            String tenantId = caseInstanceBuilder.getTenantId();
            if (tenantId == null || CmmnEngineConfiguration.NO_TENANT_ID.equals(tenantId)) {
                caseDefinition = deploymentManager.findDeployedLatestCaseDefinitionByKey(caseDefinitionKey);
                if (caseDefinition == null) {
                    throw new FlowableObjectNotFoundException("No case definition found for key " + caseDefinitionKey, CaseDefinition.class);
                }
            } else if (!CmmnEngineConfiguration.NO_TENANT_ID.equals(tenantId)) {
                try {
                    caseDefinition = deploymentManager.findDeployedLatestCaseDefinitionByKeyAndTenantId(caseDefinitionKey, caseInstanceBuilder.getTenantId());
                } catch (FlowableObjectNotFoundException e) {
                    if (caseInstanceBuilder.isFallbackToDefaultTenant()) {
                        try {
                            caseDefinition = deploymentManager.findDeployedLatestCaseDefinitionByKey(caseDefinitionKey);
                        } catch(FlowableObjectNotFoundException e1) {
                            throw new FlowableObjectNotFoundException("Case definition was not found by key '"+caseDefinitionKey+"'. Fallback to default tenant was also used.");
                        }
                    } else {
                        throw e;
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
        createAsyncInitJob(
            caseInstanceEntity,
            CommandContextUtil.getCmmnEngineConfiguration(commandContext).getJobServiceConfiguration().getJobService()
        );

        return caseInstanceEntity;
    }

    protected void createAsyncInitJob(CaseInstance caseInstance, JobService jobService) {
        JobEntity job = jobService.createJob();
        job.setJobHandlerType(AsyncInitializePlanModelJobHandler.TYPE);
        job.setScopeId(caseInstance.getId());
        job.setScopeDefinitionId(caseInstance.getCaseDefinitionId());
        job.setScopeType(ScopeTypes.CMMN);
        job.setJobHandlerConfiguration(caseInstance.getId());
        job.setTenantId(caseInstance.getTenantId());
        jobService.createAsyncJob(job, true);
        jobService.scheduleAsyncJob(job);
    }


    protected CaseInstanceEntity initializeCaseInstanceEntity(CommandContext commandContext, CaseDefinition caseDefinition,
        CaseInstanceBuilder caseInstanceBuilder) {
        CaseInstanceEntity caseInstanceEntity = createCaseInstanceEntityFromDefinition(commandContext, caseDefinition);

        applyCaseInstanceBuilder(caseInstanceBuilder, caseInstanceEntity);

        callCaseInstanceStateChangeCallbacks(commandContext, caseInstanceEntity, null, CaseInstanceState.ACTIVE);
        CommandContextUtil.getCmmnHistoryManager().recordCaseInstanceStart(caseInstanceEntity);
        return caseInstanceEntity;
    }

    protected void applyCaseInstanceBuilder(CaseInstanceBuilder caseInstanceBuilder, CaseInstanceEntity caseInstanceEntity) {
        if (caseInstanceBuilder.getName() != null) {
            caseInstanceEntity.setName(caseInstanceBuilder.getName());
        }

        if (caseInstanceBuilder.getBusinessKey() != null) {
            caseInstanceEntity.setBusinessKey(caseInstanceBuilder.getBusinessKey());
        }

        if (caseInstanceBuilder.getTenantId() != null) {
            caseInstanceEntity.setTenantId(caseInstanceBuilder.getTenantId());
        }

        if (caseInstanceBuilder.getParentId() != null) {
            caseInstanceEntity.setParentId(caseInstanceBuilder.getParentId());
        }

        if (caseInstanceBuilder.getCallbackId() != null) {
            caseInstanceEntity.setCallbackId(caseInstanceBuilder.getCallbackId());
        }

        if (caseInstanceBuilder.getCallbackType() != null) {
            caseInstanceEntity.setCallbackType(caseInstanceBuilder.getCallbackType());
        }

        Map<String, Object> variables = caseInstanceBuilder.getVariables();
        if (variables != null) {
            for (String variableName : variables.keySet()) {
                caseInstanceEntity.setVariable(variableName, variables.get(variableName));
            }
        }

        Map<String, Object> transientVariables = caseInstanceBuilder.getTransientVariables();
        if (transientVariables != null) {
            for (String variableName : transientVariables.keySet()) {
                caseInstanceEntity.setTransientVariable(variableName, transientVariables.get(variableName));
            }
        }

    }

    protected CaseInstanceEntity createCaseInstanceEntityFromDefinition(CommandContext commandContext, CaseDefinition caseDefinition) {
        CaseInstanceEntityManager caseInstanceEntityManager = CommandContextUtil.getCaseInstanceEntityManager(commandContext);
        CaseInstanceEntity caseInstanceEntity = caseInstanceEntityManager.create();
        caseInstanceEntity.setCaseDefinitionId(caseDefinition.getId());
        caseInstanceEntity.setStartTime(CommandContextUtil.getCmmnEngineConfiguration(commandContext).getClock().getCurrentTime());
        caseInstanceEntity.setState(CaseInstanceState.ACTIVE);
        caseInstanceEntity.setTenantId(caseDefinition.getTenantId());

        String authenticatedUserId = Authentication.getAuthenticatedUserId();
        caseInstanceEntity.setStartUserId(authenticatedUserId);
        
        caseInstanceEntityManager.insert(caseInstanceEntity);
        
        if (authenticatedUserId != null) {
            IdentityLinkUtil.createCaseInstanceIdentityLink(caseInstanceEntity, authenticatedUserId, null, IdentityLinkType.STARTER);
        }

        caseInstanceEntity.setSatisfiedSentryPartInstances(new ArrayList<>(1));

        CmmnDeploymentManager deploymentManager = CommandContextUtil.getCmmnEngineConfiguration(commandContext).getDeploymentManager();
        CmmnModel cmmnModel = deploymentManager.resolveCaseDefinition(caseDefinition).getCmmnModel();
        Case caseModel = cmmnModel.getCaseById(caseDefinition.getKey());

        if (caseModel.getInitiatorVariableName() != null) {
            caseInstanceEntity.setVariable(caseModel.getInitiatorVariableName(), Authentication.getAuthenticatedUserId());
        }

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
