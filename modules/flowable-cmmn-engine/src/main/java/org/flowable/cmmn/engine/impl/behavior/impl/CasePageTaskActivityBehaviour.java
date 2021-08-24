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
package org.flowable.cmmn.engine.impl.behavior.impl;

import static org.flowable.cmmn.model.Criterion.EXIT_EVENT_TYPE_COMPLETE;
import static org.flowable.cmmn.model.Criterion.EXIT_EVENT_TYPE_FORCE_COMPLETE;

import org.apache.commons.lang3.StringUtils;
import org.flowable.cmmn.api.delegate.DelegatePlanItemInstance;
import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.impl.behavior.OnParentEndDependantActivityBehavior;
import org.flowable.cmmn.engine.impl.behavior.PlanItemActivityBehavior;
import org.flowable.cmmn.engine.impl.persistence.entity.PlanItemInstanceEntity;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.cmmn.engine.impl.util.IdentityLinkUtil;
import org.flowable.cmmn.engine.interceptor.CreateCasePageTaskAfterContext;
import org.flowable.cmmn.engine.interceptor.CreateCasePageTaskBeforeContext;
import org.flowable.cmmn.model.CasePageTask;
import org.flowable.cmmn.model.PlanItemTransition;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.impl.el.ExpressionManager;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.identitylink.api.IdentityLinkType;

public class CasePageTaskActivityBehaviour extends TaskActivityBehavior implements PlanItemActivityBehavior, OnParentEndDependantActivityBehavior {

    protected CasePageTask casePageTask;

    public CasePageTaskActivityBehaviour(CasePageTask casePageTask) {
        super(true, null);
        this.casePageTask = casePageTask;
    }
    
    @Override
    public void execute(CommandContext commandContext, PlanItemInstanceEntity planItemInstanceEntity) {
        CreateCasePageTaskBeforeContext beforeContext = new CreateCasePageTaskBeforeContext(casePageTask, planItemInstanceEntity, 
                        casePageTask.getFormKey(), casePageTask.getAssignee(), casePageTask.getOwner(), 
                        casePageTask.getCandidateUsers(), casePageTask.getCandidateGroups());
        
        CmmnEngineConfiguration cmmnEngineConfiguration = CommandContextUtil.getCmmnEngineConfiguration(commandContext);
        if (cmmnEngineConfiguration.getCreateCasePageTaskInterceptor() != null) {
            cmmnEngineConfiguration.getCreateCasePageTaskInterceptor().beforeCreateCasePageTask(beforeContext);
        }
        
        ExpressionManager expressionManager = cmmnEngineConfiguration.getExpressionManager();
        if (StringUtils.isNotEmpty(beforeContext.getFormKey())) {
            planItemInstanceEntity.setFormKey(getExpressionValue(beforeContext.getFormKey(), planItemInstanceEntity, expressionManager));
        }
        
        if (StringUtils.isNotEmpty(beforeContext.getAssignee())) {
            IdentityLinkUtil.createPlanItemInstanceIdentityLink(planItemInstanceEntity, 
                            getExpressionValue(beforeContext.getAssignee(), planItemInstanceEntity, expressionManager), 
                            null, IdentityLinkType.ASSIGNEE, cmmnEngineConfiguration);
        }
        
        if (StringUtils.isNotEmpty(beforeContext.getOwner())) {
            IdentityLinkUtil.createPlanItemInstanceIdentityLink(planItemInstanceEntity, 
                            getExpressionValue(beforeContext.getOwner(), planItemInstanceEntity, expressionManager), 
                            null, IdentityLinkType.OWNER, cmmnEngineConfiguration);
        }
        
        if (beforeContext.getCandidateUsers() != null && !beforeContext.getCandidateUsers().isEmpty()) {
            for (String candidateUser : beforeContext.getCandidateUsers()) {
                IdentityLinkUtil.createPlanItemInstanceIdentityLink(planItemInstanceEntity, 
                                getExpressionValue(candidateUser, planItemInstanceEntity, expressionManager), 
                                null, IdentityLinkType.CANDIDATE, cmmnEngineConfiguration);
            }
        }
        
        if (beforeContext.getCandidateGroups() != null && !beforeContext.getCandidateGroups().isEmpty()) {
            for (String candidateGroup : beforeContext.getCandidateGroups()) {
                IdentityLinkUtil.createPlanItemInstanceIdentityLink(planItemInstanceEntity, null,
                                getExpressionValue(candidateGroup, planItemInstanceEntity, expressionManager), 
                                IdentityLinkType.CANDIDATE, cmmnEngineConfiguration);
            }
        }

        if (cmmnEngineConfiguration.getCreateCasePageTaskInterceptor() != null) {
            CreateCasePageTaskAfterContext afterContext = new CreateCasePageTaskAfterContext(casePageTask, planItemInstanceEntity);
            cmmnEngineConfiguration.getCreateCasePageTaskInterceptor().afterCreateCasePageTask(afterContext);
        }
        
        cmmnEngineConfiguration.getCmmnHistoryManager().recordPlanItemInstanceUpdated(planItemInstanceEntity);
    }

    @Override
    public void onStateTransition(CommandContext commandContext, DelegatePlanItemInstance planItemInstance, String transition) {
        
    }

    @Override
    public void onParentEnd(CommandContext commandContext, PlanItemInstanceEntity planItemInstanceEntity, String parentEndTransition, String exitEventType) {
        // a case page is working differently from default plan items as it gets completed, if its parent is completed and is terminated otherwise
        // delegate the completion on parent complete or an exit sentry having exit event type complete or force complete
        if (PlanItemTransition.COMPLETE.equals(parentEndTransition) || EXIT_EVENT_TYPE_COMPLETE.equals(exitEventType) || EXIT_EVENT_TYPE_FORCE_COMPLETE.equals(exitEventType)) {
            CommandContextUtil.getAgenda(commandContext).planCompletePlanItemInstanceOperation(planItemInstanceEntity);
        } else {
            CommandContextUtil.getAgenda(commandContext).planTerminatePlanItemInstanceOperation(planItemInstanceEntity, null, null);
        }
    }

    protected String getExpressionValue(String value, PlanItemInstanceEntity planItemInstanceEntity, ExpressionManager expressionManager) {
        Object expressionValue = expressionManager.createExpression(value).getValue(planItemInstanceEntity);
        if (expressionValue != null) {
            return expressionValue.toString();
        }
        
        throw new FlowableException("Unable to resolve expression value for " + value);
    }
}
