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
package org.flowable.ui.task.service.debugger;

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.flowable.cmmn.engine.impl.job.ActivateCmmnBreakpointJobHandler.CMMN_BREAKPOINT;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.flowable.cmmn.api.CmmnHistoryService;
import org.flowable.cmmn.api.CmmnManagementService;
import org.flowable.cmmn.api.CmmnRuntimeService;
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.api.runtime.CmmnDebugger;
import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.impl.persistence.entity.CaseInstanceEntity;
import org.flowable.cmmn.engine.impl.persistence.entity.PlanItemInstanceEntity;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.delegate.Expression;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.common.engine.impl.el.ExpressionManager;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandExecutor;
import org.flowable.common.engine.impl.scripting.ScriptingEngines;
import org.flowable.engine.impl.context.Context;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.persistence.entity.ExecutionEntityImpl;
import org.flowable.engine.runtime.Execution;
import org.flowable.job.api.Job;
import org.flowable.ui.task.model.debugger.BreakpointRepresentation;
import org.flowable.ui.task.model.debugger.ExecutionRepresentation;
import org.flowable.ui.task.model.debugger.PlanItemRepresentation;
import org.flowable.variable.api.delegate.VariableScope;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Service;

/**
 * This class implements basic methods for managing breakpoints
 *
 * @author martin.grofcik
 */
@Service
public class CmmnDebuggerService implements CmmnDebugger, ApplicationContextAware {

    protected List<BreakpointRepresentation> breakpoints = new ArrayList<>();
    protected ApplicationContext applicationContext;

    public void addBreakpoint(BreakpointRepresentation breakpointRepresentation) {
        assert breakpointRepresentation != null && isNotBlank(breakpointRepresentation.getElementId());
        this.breakpoints.add(breakpointRepresentation);
    }

    public void removeBreakpoint(BreakpointRepresentation breakpointRepresentation) {
        assert breakpointRepresentation != null && isNotBlank(breakpointRepresentation.getElementId());
        if (!this.breakpoints.remove(breakpointRepresentation)) {
            throw new FlowableException("Breakpoint is not set on the elementId");
        }
    }

    public List<BreakpointRepresentation> getBreakpoints() {
        return breakpoints;
    }

    public Collection<String> getBrokenPlanItems(String caseInstanceId) {
        List<Job> brokenJobs = getCmmnManagementService().createSuspendedJobQuery().
            scopeType(ScopeTypes.CMMN).scopeId(caseInstanceId).
            handlerType(CMMN_BREAKPOINT).
            list();

        ArrayList<String> planItemIds = new ArrayList<>();
        for (Job brokenJob : brokenJobs) {
            planItemIds.add(brokenJob.getSubScopeId());
        }
        return planItemIds;
    }

    public String getBrokenPlanItemsForModelPlanItem(String modelPlanItemId) {
        List<Job> brokenJobs = getCmmnManagementService().createSuspendedJobQuery().
            scopeType(ScopeTypes.CMMN).
            handlerType(CMMN_BREAKPOINT).
            list();

        for (Job brokenJob : brokenJobs) {
            String subScopeId = brokenJob.getSubScopeId();
            PlanItemInstance planItemInstance = getCmmnRuntimeService().createPlanItemInstanceQuery().planItemInstanceId(subScopeId).singleResult();
            if (planItemInstance.getElementId().equals(modelPlanItemId)) {
                return planItemInstance.getId();
            }
        }
        return null;
    }


    // todo
//    public List<EventLogEntry> getCaseInstanceEventLog(String caseInstanceId) {
//        return getCmmnManagementService().getEventLogEntriesByProcessInstanceId(caseInstanceId);
//    }

    public void continuePlanItem(String planItemId) {
        PlanItemInstance planItemInstance = getCmmnRuntimeService().createPlanItemInstanceQuery().planItemInstanceId(planItemId).singleResult();
        if (planItemInstance == null) {
            throw new FlowableException("No planIntemInstance found '"+planItemId+"'");
        }

        Job job = getCmmnManagementService().createSuspendedJobQuery().handlerType(CMMN_BREAKPOINT).
            scopeType(ScopeTypes.CMMN).
            scopeId(planItemInstance.getCaseInstanceId()).
            subScopeId(planItemId).
            singleResult();
        if (job == null) {
            throw new FlowableException("No broken job found for planItem '" + planItemId+ "'");
        }

        getCmmnManagementService().moveSuspendedJobToExecutableJob(job.getId());
        try {
            // wait until job is processed
            while (getCmmnManagementService().createJobQuery().jobId(job.getId()).count() > 0) {
                Thread.sleep(100);
            }
        } catch (InterruptedException e) {
        }
    }

    @Override
    public boolean isBreakPoint(String entryCriterionId, PlanItemInstance planItemInstance) {
        for (BreakpointRepresentation breakpoint : breakpoints) {
            if (breakpoint.getElementId().equals(planItemInstance.getPlanItemDefinitionId()) ||
                breakpoint.getElementId().equals(planItemInstance.getElementId())) {
                if (StringUtils.isEmpty(breakpoint.getDefinitionId())) {
                    return true;
                }

                if (Objects.equals(breakpoint.getDefinitionId(), planItemInstance.getCaseDefinitionId())) {
                    return true;
                }
            }
            if (breakpoint.getElementId().equals(entryCriterionId)) {
                return true;
            }
        }
        return false;
    }

    protected CmmnManagementService getCmmnManagementService() {
        return this.applicationContext.getBean(CmmnManagementService.class);
    }

    protected CommandExecutor getCmmnCommandExecutor() {
        return this.applicationContext.getBean(CmmnEngineConfiguration.class).getCommandExecutor();
    }

    protected CmmnRuntimeService getCmmnRuntimeService() {
        return this.applicationContext.getBean(CmmnRuntimeService.class);
    }

    protected CmmnHistoryService getCmmnHistoricService() {
        return this.applicationContext.getBean(CmmnHistoryService.class);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    public List<DebuggerRestVariable> getCaseAndPlanItemVariables(String caseInstanceId) {
        List<PlanItemInstance> planItemInstances = getCmmnRuntimeService().createPlanItemInstanceQuery().caseInstanceId(caseInstanceId).list();
        if (planItemInstances.isEmpty()) {//if case is completed (i.e. no active plan item left)
            return getCmmnHistoricService().createHistoricVariableInstanceQuery().
                    caseInstanceId(caseInstanceId).
                    list().stream().
                    map(DebuggerRestVariable::new).
                    collect(Collectors.toList());
        }
        return getCmmnCommandExecutor().execute(commandContext ->
            ((CaseInstanceEntity)getCmmnRuntimeService().createCaseInstanceQuery()
                    .caseInstanceId(caseInstanceId).singleResult()).
                    getVariableInstances().
                    values().stream().
                    map(DebuggerRestVariable::new).
                    collect(Collectors.toList())
        );
    }
    
    public List<PlanItemRepresentation> getPlanItemInstances(String caseInstanceId) {
        List<PlanItemInstance> planItemInstances = getCmmnRuntimeService().createPlanItemInstanceQuery().caseInstanceId(caseInstanceId).list();
        List<PlanItemRepresentation> planItemRepresentations = new ArrayList<>(planItemInstances.size());
        for (PlanItemInstance planItemInstance : planItemInstances) {
            planItemRepresentations.add(new PlanItemRepresentation(planItemInstance.getId(),
                    planItemInstance.getCaseInstanceId(), planItemInstance.getStageInstanceId(), planItemInstance.getElementId(),
                    planItemInstance.isCompleteable(), planItemInstance.getTenantId()));
        }
        return planItemRepresentations;
    }

    public List<String> getPlanItemInstancesPlanItemsIds() {
        List<PlanItemInstance> planItemInstances = getCmmnRuntimeService().createPlanItemInstanceQuery().list();
        List<String> planItemIds= new ArrayList<>(planItemInstances.size());
        for (PlanItemInstance planItemInstance : planItemInstances) {
            planItemIds.add(planItemInstance.getPlanItemDefinitionId());
        }
        return planItemIds;
    }

    public Object evaluateExpression(final String planItemInstanceIdOrCaseInstanceId, final String expressionString) {
        final CmmnEngineConfiguration engineConfiguration = this.applicationContext.getBean(CmmnEngineConfiguration.class);
        return engineConfiguration.getCommandExecutor().execute(commandContext -> {
            ExpressionManager expressionManager = engineConfiguration.getExpressionManager();
            Expression expression = expressionManager.createExpression(expressionString);

            PlanItemInstance planItemInstance = engineConfiguration.getPlanItemInstanceEntityManager().findById(planItemInstanceIdOrCaseInstanceId);
            if(planItemInstance != null) {
                return expression.getValue((VariableScope) planItemInstance);
            } else {
                CaseInstance caseInstance = engineConfiguration.getCaseInstanceEntityManager().findById(planItemInstanceIdOrCaseInstanceId);
                return expression.getValue((VariableScope) caseInstance);
            }
        });
    }

    public void evaluateScript(final String planItemInstanceIdOrCaseInstanceId, final String scriptLanguage, final String script) {
        getCmmnCommandExecutor().execute(
                (Command<Void>) commandContext -> {
                    CmmnEngineConfiguration cmmnEngineConfiguration = this.applicationContext.getBean(CmmnEngineConfiguration.class);
                    ScriptingEngines scriptingEngines = cmmnEngineConfiguration.getScriptingEngines();
                    PlanItemInstanceEntity planItemInstance = cmmnEngineConfiguration .getPlanItemInstanceEntityManager().findById(planItemInstanceIdOrCaseInstanceId);
                    if(planItemInstance != null) {
                        scriptingEngines.evaluate(script, scriptLanguage, planItemInstance, false);
                    } else {
                        CaseInstanceEntity caseInstance = cmmnEngineConfiguration.getCaseInstanceEntityManager().findById(planItemInstanceIdOrCaseInstanceId);
                        scriptingEngines.evaluate(script, scriptLanguage, caseInstance, false);
                    }
                    return null;
                }
        );
    }
}
