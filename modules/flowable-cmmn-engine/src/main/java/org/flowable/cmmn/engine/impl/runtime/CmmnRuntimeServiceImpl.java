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

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.flowable.cmmn.api.CmmnRuntimeService;
import org.flowable.cmmn.api.StageResponse;
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.api.runtime.CaseInstanceBuilder;
import org.flowable.cmmn.api.runtime.CaseInstanceQuery;
import org.flowable.cmmn.api.runtime.ChangePlanItemStateBuilder;
import org.flowable.cmmn.api.runtime.GenericEventListenerInstanceQuery;
import org.flowable.cmmn.api.runtime.MilestoneInstanceQuery;
import org.flowable.cmmn.api.runtime.PlanItemInstanceQuery;
import org.flowable.cmmn.api.runtime.PlanItemInstanceTransitionBuilder;
import org.flowable.cmmn.api.runtime.SignalEventListenerInstanceQuery;
import org.flowable.cmmn.api.runtime.UserEventListenerInstanceQuery;
import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.impl.cmd.AddIdentityLinkForCaseInstanceCmd;
import org.flowable.cmmn.engine.impl.cmd.ChangePlanItemStateCmd;
import org.flowable.cmmn.engine.impl.cmd.CompleteCaseInstanceCmd;
import org.flowable.cmmn.engine.impl.cmd.CompleteStagePlanItemInstanceCmd;
import org.flowable.cmmn.engine.impl.cmd.DeleteCaseInstanceCmd;
import org.flowable.cmmn.engine.impl.cmd.DeleteIdentityLinkForCaseInstanceCmd;
import org.flowable.cmmn.engine.impl.cmd.DisablePlanItemInstanceCmd;
import org.flowable.cmmn.engine.impl.cmd.EnablePlanItemInstanceCmd;
import org.flowable.cmmn.engine.impl.cmd.EvaluateCriteriaCmd;
import org.flowable.cmmn.engine.impl.cmd.GetCaseVariableInstanceCmd;
import org.flowable.cmmn.engine.impl.cmd.GetCaseVariableInstancesCmd;
import org.flowable.cmmn.engine.impl.cmd.GetEntityLinkChildrenForCaseInstanceCmd;
import org.flowable.cmmn.engine.impl.cmd.GetEntityLinkChildrenWithSameRootAsCaseInstanceCmd;
import org.flowable.cmmn.engine.impl.cmd.GetEntityLinkParentsForCaseInstanceCmd;
import org.flowable.cmmn.engine.impl.cmd.GetIdentityLinksForCaseInstanceCmd;
import org.flowable.cmmn.engine.impl.cmd.GetIdentityLinksForPlanItemInstanceCmd;
import org.flowable.cmmn.engine.impl.cmd.GetLocalVariableCmd;
import org.flowable.cmmn.engine.impl.cmd.GetLocalVariablesCmd;
import org.flowable.cmmn.engine.impl.cmd.GetPlanItemVariableInstanceCmd;
import org.flowable.cmmn.engine.impl.cmd.GetPlanItemVariableInstancesCmd;
import org.flowable.cmmn.engine.impl.cmd.GetStageOverviewCmd;
import org.flowable.cmmn.engine.impl.cmd.GetStartFormModelCmd;
import org.flowable.cmmn.engine.impl.cmd.GetVariableCmd;
import org.flowable.cmmn.engine.impl.cmd.GetVariablesCmd;
import org.flowable.cmmn.engine.impl.cmd.HasCaseInstanceVariableCmd;
import org.flowable.cmmn.engine.impl.cmd.RemoveLocalVariableCmd;
import org.flowable.cmmn.engine.impl.cmd.RemoveLocalVariablesCmd;
import org.flowable.cmmn.engine.impl.cmd.RemoveVariableCmd;
import org.flowable.cmmn.engine.impl.cmd.RemoveVariablesCmd;
import org.flowable.cmmn.engine.impl.cmd.SetCaseInstanceBusinessKeyCmd;
import org.flowable.cmmn.engine.impl.cmd.SetCaseInstanceBusinessStatusCmd;
import org.flowable.cmmn.engine.impl.cmd.SetCaseInstanceNameCmd;
import org.flowable.cmmn.engine.impl.cmd.SetLocalVariableCmd;
import org.flowable.cmmn.engine.impl.cmd.SetLocalVariablesCmd;
import org.flowable.cmmn.engine.impl.cmd.SetVariableCmd;
import org.flowable.cmmn.engine.impl.cmd.SetVariablesCmd;
import org.flowable.cmmn.engine.impl.cmd.StartCaseInstanceAsyncCmd;
import org.flowable.cmmn.engine.impl.cmd.StartCaseInstanceCmd;
import org.flowable.cmmn.engine.impl.cmd.StartPlanItemInstanceCmd;
import org.flowable.cmmn.engine.impl.cmd.TerminateCaseInstanceCmd;
import org.flowable.cmmn.engine.impl.cmd.TerminatePlanItemInstanceCmd;
import org.flowable.cmmn.engine.impl.cmd.TriggerPlanItemInstanceCmd;
import org.flowable.common.engine.impl.service.CommonEngineServiceImpl;
import org.flowable.entitylink.api.EntityLink;
import org.flowable.eventsubscription.api.EventSubscriptionQuery;
import org.flowable.eventsubscription.service.impl.EventSubscriptionQueryImpl;
import org.flowable.form.api.FormInfo;
import org.flowable.identitylink.api.IdentityLink;
import org.flowable.variable.api.persistence.entity.VariableInstance;

/**
 * @author Joram Barrez
 */
public class CmmnRuntimeServiceImpl extends CommonEngineServiceImpl<CmmnEngineConfiguration> implements CmmnRuntimeService {

    public CmmnRuntimeServiceImpl(CmmnEngineConfiguration engineConfiguration) {
        super(engineConfiguration);
    }

    @Override
    public CaseInstanceBuilder createCaseInstanceBuilder() {
        return new CaseInstanceBuilderImpl(this);
    }

    @Override
    public PlanItemInstanceTransitionBuilder createPlanItemInstanceTransitionBuilder(String planItemInstanceId) {
        return new PlanItemInstanceTransitionBuilderImpl(commandExecutor, planItemInstanceId);
    }

    public CaseInstance startCaseInstance(CaseInstanceBuilder caseInstanceBuilder) {
        return commandExecutor.execute(new StartCaseInstanceCmd(caseInstanceBuilder));
    }
    
    public CaseInstance startCaseInstanceAsync(CaseInstanceBuilder caseInstanceBuilder) {
        return commandExecutor.execute(new StartCaseInstanceAsyncCmd(caseInstanceBuilder));
    }

    @Override
    public FormInfo getStartFormModel(String caseDefinitionId, String caseInstanceId) {
        return commandExecutor.execute(new GetStartFormModelCmd(caseDefinitionId, caseInstanceId));
    }

    @Override 
    public void triggerPlanItemInstance(String planItemInstanceId) {
        commandExecutor.execute(new TriggerPlanItemInstanceCmd(planItemInstanceId));
    }
    
    @Override
    public void enablePlanItemInstance(String planItemInstanceId) {
        commandExecutor.execute(new EnablePlanItemInstanceCmd(planItemInstanceId));
    }
    
    @Override
    public void disablePlanItemInstance(String planItemInstanceId) {
        commandExecutor.execute(new DisablePlanItemInstanceCmd(planItemInstanceId));
    }
    
    @Override
    public void completeStagePlanItemInstance(String planItemInstanceId) {
        commandExecutor.execute(new CompleteStagePlanItemInstanceCmd(planItemInstanceId));
    }

    @Override
    public void completeStagePlanItemInstance(String planItemInstanceId, boolean force) {
        commandExecutor.execute(new CompleteStagePlanItemInstanceCmd(planItemInstanceId, force));
    }

    @Override
    public void startPlanItemInstance(String planItemInstanceId) {
        commandExecutor.execute(new StartPlanItemInstanceCmd(planItemInstanceId));
    }
    
    @Override
    public void completeCaseInstance(String caseInstanceId) {
        commandExecutor.execute(new CompleteCaseInstanceCmd(caseInstanceId));
    }

    @Override
    public void terminateCaseInstance(String caseInstanceId) {
        commandExecutor.execute(new TerminateCaseInstanceCmd(caseInstanceId));
    }

    @Override
    public void terminatePlanItemInstance(String planItemInstanceId) {
        commandExecutor.execute(new TerminatePlanItemInstanceCmd(planItemInstanceId));
    }
    
    @Override
    public void deleteCaseInstance(String caseInstanceId) {
        commandExecutor.execute(new DeleteCaseInstanceCmd(caseInstanceId));
    }

    @Override
    public void evaluateCriteria(String caseInstanceId) {
        commandExecutor.execute(new EvaluateCriteriaCmd(caseInstanceId));
    }
    
    @Override
    public void completeGenericEventListenerInstance(String genericEventListenerInstanceId) {
        commandExecutor.execute(new TriggerPlanItemInstanceCmd(genericEventListenerInstanceId));
    }

    @Override
    public void completeUserEventListenerInstance(String userEventListenerInstanceId) {
        commandExecutor.execute(new TriggerPlanItemInstanceCmd(userEventListenerInstanceId));
    }

    @Override
    public Map<String, Object> getVariables(String caseInstanceId) {
        return commandExecutor.execute(new GetVariablesCmd(caseInstanceId));
    }
    
    @Override
    public Map<String, VariableInstance> getVariableInstances(String caseInstanceId) {
        return commandExecutor.execute(new GetCaseVariableInstancesCmd(caseInstanceId));
    }

    @Override
    public Map<String, Object> getLocalVariables(String planItemInstanceId) {
        return commandExecutor.execute(new GetLocalVariablesCmd(planItemInstanceId));
    }
    
    @Override
    public Map<String, VariableInstance> getLocalVariableInstances(String planItemInstanceId) {
        return commandExecutor.execute(new GetPlanItemVariableInstancesCmd(planItemInstanceId));
    }

    @Override
    public Object getVariable(String caseInstanceId, String variableName) {
        return commandExecutor.execute(new GetVariableCmd(caseInstanceId, variableName));
    }
    
    @Override
    public VariableInstance getVariableInstance(String caseInstanceId, String variableName) {
        return commandExecutor.execute(new GetCaseVariableInstanceCmd(caseInstanceId, variableName));
    }
    
    @Override
    public Object getLocalVariable(String planItemInstanceId, String variableName) {
        return commandExecutor.execute(new GetLocalVariableCmd(planItemInstanceId, variableName));
    }
    
    @Override
    public VariableInstance getLocalVariableInstance(String planItemInstanceId, String variableName) {
        return commandExecutor.execute(new GetPlanItemVariableInstanceCmd(planItemInstanceId, variableName));
    }
    
    @Override
    public boolean hasVariable(String caseInstanceId, String variableName) {
        return commandExecutor.execute(new HasCaseInstanceVariableCmd(caseInstanceId, variableName, false));
    }
    
    @Override
    public void setVariable(String caseInstanceId, String variableName, Object variableValue) {
        commandExecutor.execute(new SetVariableCmd(caseInstanceId, variableName, variableValue));
    }

    @Override
    public void setVariables(String caseInstanceId, Map<String, Object> variables) {
        commandExecutor.execute(new SetVariablesCmd(caseInstanceId, variables));
    }
    
    @Override
    public void setLocalVariable(String planItemInstanceId, String variableName, Object variableValue) {
        commandExecutor.execute(new SetLocalVariableCmd(planItemInstanceId, variableName, variableValue));
    }
    
    @Override
    public void setLocalVariables(String planItemInstanceId, Map<String, Object> variables) {
        commandExecutor.execute(new SetLocalVariablesCmd(planItemInstanceId, variables));
    }

    @Override
    public void removeVariable(String caseInstanceId, String variableName) {
        commandExecutor.execute(new RemoveVariableCmd(caseInstanceId, variableName));
    }
    
    @Override
    public void removeVariables(String caseInstanceId, Collection<String> variableNames) {
        commandExecutor.execute(new RemoveVariablesCmd(caseInstanceId, variableNames));
    }
    
    @Override
    public void removeLocalVariable(String planItemInstanceId, String variableName) {
        commandExecutor.execute(new RemoveLocalVariableCmd(planItemInstanceId, variableName));
    }
    
    @Override
    public void removeLocalVariables(String planItemInstanceId, Collection<String> variableNames) {
        commandExecutor.execute(new RemoveLocalVariablesCmd(planItemInstanceId, variableNames));
    }

    @Override
    public void setCaseInstanceName(String caseInstanceId, String caseName) {
        commandExecutor.execute(new SetCaseInstanceNameCmd(caseInstanceId, caseName));
    }

    @Override
    public CaseInstanceQuery createCaseInstanceQuery() {
        return configuration.getCaseInstanceEntityManager().createCaseInstanceQuery();
    }

    @Override
    public PlanItemInstanceQuery createPlanItemInstanceQuery() {
        return configuration.getPlanItemInstanceEntityManager().createPlanItemInstanceQuery();
    }

    @Override
    public MilestoneInstanceQuery createMilestoneInstanceQuery() {
        return configuration.getMilestoneInstanceEntityManager().createMilestoneInstanceQuery();
    }
    
    @Override
    public GenericEventListenerInstanceQuery createGenericEventListenerInstanceQuery() {
        return new GenericEventListenerInstanceQueryImpl(configuration.getCommandExecutor(), configuration);
    }
    
    @Override
    public SignalEventListenerInstanceQuery createSignalEventListenerInstanceQuery() {
        return new SignalEventListenerInstanceQueryImpl(configuration.getCommandExecutor(), configuration);
    }

    @Override
    public UserEventListenerInstanceQuery createUserEventListenerInstanceQuery() {
        return new UserEventListenerInstanceQueryImpl(configuration.getCommandExecutor(), configuration);
    }
    
    @Override
    public EventSubscriptionQuery createEventSubscriptionQuery() {
        return new EventSubscriptionQueryImpl(configuration.getCommandExecutor(), configuration.getEventSubscriptionServiceConfiguration());
    }

    @Override
    public List<StageResponse> getStageOverview(String caseInstanceId) {
        return commandExecutor.execute(new GetStageOverviewCmd(caseInstanceId));
    }

    @Override
    public void addUserIdentityLink(String caseInstanceId, String userId, String identityLinkType) {
        commandExecutor.execute(new AddIdentityLinkForCaseInstanceCmd(caseInstanceId, userId, null, identityLinkType));
    }

    @Override
    public void addGroupIdentityLink(String caseInstanceId, String groupId, String identityLinkType) {
        commandExecutor.execute(new AddIdentityLinkForCaseInstanceCmd(caseInstanceId, null, groupId, identityLinkType));
    }

    @Override
    public void deleteUserIdentityLink(String caseInstanceId, String userId, String identityLinkType) {
        commandExecutor.execute(new DeleteIdentityLinkForCaseInstanceCmd(caseInstanceId, userId, null, identityLinkType));
    }

    @Override
    public void deleteGroupIdentityLink(String caseInstanceId, String groupId, String identityLinkType) {
        commandExecutor.execute(new DeleteIdentityLinkForCaseInstanceCmd(caseInstanceId, null, groupId, identityLinkType));
    }

    @Override
    public List<IdentityLink> getIdentityLinksForCaseInstance(String caseInstanceId) {
        return commandExecutor.execute(new GetIdentityLinksForCaseInstanceCmd(caseInstanceId));
    }
    
    @Override
    public List<IdentityLink> getIdentityLinksForPlanItemInstance(String planItemInstanceId) {
        return commandExecutor.execute(new GetIdentityLinksForPlanItemInstanceCmd(planItemInstanceId));
    }
    
    @Override
    public List<EntityLink> getEntityLinkChildrenForCaseInstance(String caseInstanceId) {
        return commandExecutor.execute(new GetEntityLinkChildrenForCaseInstanceCmd(caseInstanceId));
    }

    @Override
    public List<EntityLink> getEntityLinkChildrenWithSameRootAsCaseInstance(String instanceId) {
        return commandExecutor.execute(new GetEntityLinkChildrenWithSameRootAsCaseInstanceCmd(instanceId));
    }

    @Override
    public List<EntityLink> getEntityLinkParentsForCaseInstance(String caseInstanceId) {
        return commandExecutor.execute(new GetEntityLinkParentsForCaseInstanceCmd(caseInstanceId));
    }

    @Override
    public ChangePlanItemStateBuilder createChangePlanItemStateBuilder() {
        return new ChangePlanItemStateBuilderImpl(this);
    }

    @Override
    public void updateBusinessKey(String caseInstanceId, String businessKey) {
        commandExecutor.execute(new SetCaseInstanceBusinessKeyCmd(caseInstanceId, businessKey));
    }
    
    @Override
    public void updateBusinessStatus(String caseInstanceId, String businessStatus) {
        commandExecutor.execute(new SetCaseInstanceBusinessStatusCmd(caseInstanceId, businessStatus));
    }

    public void changePlanItemState(ChangePlanItemStateBuilderImpl changePlanItemStateBuilder) {
        commandExecutor.execute(new ChangePlanItemStateCmd(changePlanItemStateBuilder, configuration));
    }

}
