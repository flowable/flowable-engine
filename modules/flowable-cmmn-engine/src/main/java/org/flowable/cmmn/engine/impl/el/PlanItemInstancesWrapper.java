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
package org.flowable.cmmn.engine.impl.el;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstanceState;
import org.flowable.cmmn.engine.impl.persistence.entity.CaseInstanceEntity;
import org.flowable.cmmn.engine.impl.persistence.entity.PlanItemInstanceContainer;
import org.flowable.cmmn.engine.impl.persistence.entity.PlanItemInstanceEntity;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.cmmn.model.Stage;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.variable.VariableContainer;
import org.flowable.common.engine.impl.interceptor.CommandContext;

/**
 * @author Joram Barrez
 */
public class PlanItemInstancesWrapper {

    protected VariableContainer variableContainer;
    protected CaseInstanceEntity caseInstanceEntity;

    protected List<PlanItemInstanceEntity> planItemInstances;

    public PlanItemInstancesWrapper(VariableContainer variableContainer) {
        this.variableContainer = variableContainer;

        if (variableContainer instanceof PlanItemInstanceEntity planItemInstanceEntity) {
            caseInstanceEntity = CommandContextUtil.getCaseInstanceEntityManager().findById(planItemInstanceEntity.getCaseInstanceId());

        } else if (variableContainer instanceof CaseInstanceEntity) {
            caseInstanceEntity = (CaseInstanceEntity) variableContainer;

        }
    }

    public PlanItemInstancesWrapper(VariableContainer variableContainer, CaseInstanceEntity caseInstanceEntity, List<PlanItemInstanceEntity> planItemInstances) {
        this.variableContainer = variableContainer;
        this.caseInstanceEntity = caseInstanceEntity;
        this.planItemInstances = planItemInstances;
    }

    public int count() {
        ensurePlanItemInstanceInitialized();
        return planItemInstances.size();
    }

    public boolean exists() {
        return count() > 0;
    }

    protected void ensurePlanItemInstanceInitialized() {
        if (planItemInstances == null) {
            planItemInstances = collectAllChildPlanItemInstances(caseInstanceEntity);
        }
    }

    public PlanItemInstancesWrapper definitionId(String...ids) {
        return definitionIds(ids);
    }

    public PlanItemInstancesWrapper definitionIds(String...ids) {
        ensurePlanItemInstanceInitialized();

        List<String> list = Arrays.asList(ids);
        List<PlanItemInstanceEntity> filteredPlanItemInstances = planItemInstances.stream()
            .filter(planItemInstanceEntity -> planItemInstanceEntity.getPlanItem() != null
                && planItemInstanceEntity.getPlanItem().getPlanItemDefinition() != null
                && list.contains(planItemInstanceEntity.getPlanItem().getPlanItemDefinition().getId()))
            .collect(Collectors.toList());

        return new PlanItemInstancesWrapper(variableContainer, caseInstanceEntity, filteredPlanItemInstances);
    }

    public List<String> getDefinitionId() {
       return getDefinitionIds();
    }

    public List<String> getDefinitionIds() {
        ensurePlanItemInstanceInitialized();
        return planItemInstances.stream().map(planItemInstanceEntity -> planItemInstanceEntity.getPlanItem().getPlanItemDefinition().getId()).collect(Collectors.toList());
    }

    public PlanItemInstancesWrapper name(String...names) {
        return names(names);
    }

    public PlanItemInstancesWrapper names(String...names) {
        ensurePlanItemInstanceInitialized();

        List<String> list = Arrays.asList(names);
        List<PlanItemInstanceEntity> filteredPlanItemInstances = planItemInstances.stream()
            .filter(planItemInstanceEntity -> planItemInstanceEntity.getPlanItem() != null
                && planItemInstanceEntity.getPlanItem().getPlanItemDefinition() != null
                && list.contains(planItemInstanceEntity.getPlanItem().getPlanItemDefinition().getName()))
            .collect(Collectors.toList());

        return new PlanItemInstancesWrapper(variableContainer, caseInstanceEntity, filteredPlanItemInstances);
    }

    public List<String> getDefinitionName() {
        return getDefinitionNames();
    }

    public List<String> getDefinitionNames() {
        ensurePlanItemInstanceInitialized();
        return planItemInstances.stream().map(planItemInstanceEntity -> planItemInstanceEntity.getPlanItem().getPlanItemDefinition().getName()).collect(Collectors.toList());
    }

    public PlanItemInstancesWrapper currentStage() {
        PlanItemInstanceContainer stageContainer = null;
        if (variableContainer instanceof PlanItemInstanceEntity planItemInstanceEntity) {
            PlanItemInstanceEntity stagePlanItemInstanceEntity = planItemInstanceEntity.getStagePlanItemInstanceEntity();
            if (stagePlanItemInstanceEntity != null) {
                stageContainer = stagePlanItemInstanceEntity;
            } else {
                stageContainer = CommandContextUtil.getCaseInstanceEntityManager().findById(planItemInstanceEntity.getCaseInstanceId());
            }

        } else if (variableContainer instanceof CaseInstanceEntity) {
            stageContainer = (CaseInstanceEntity) variableContainer;

        }

        List<PlanItemInstanceEntity> childPlanItemInstances = collectAllChildPlanItemInstances(stageContainer);
        return new PlanItemInstancesWrapper(variableContainer, caseInstanceEntity, childPlanItemInstances);
    }

    public PlanItemInstancesWrapper active() {
        return getPlanItemInstancesWithState(PlanItemInstanceState.ACTIVE);
    }

    public PlanItemInstancesWrapper available() {
        return getPlanItemInstancesWithState(PlanItemInstanceState.AVAILABLE);
    }

    public PlanItemInstancesWrapper enabled() {
        return getPlanItemInstancesWithState(PlanItemInstanceState.ENABLED);
    }

    public PlanItemInstancesWrapper disabled() {
        return getPlanItemInstancesWithState(PlanItemInstanceState.DISABLED);
    }

    public PlanItemInstancesWrapper completed() {
        return getPlanItemInstancesWithState(PlanItemInstanceState.COMPLETED);
    }

    public PlanItemInstancesWrapper failed() {
        return getPlanItemInstancesWithState(PlanItemInstanceState.FAILED);
    }

    public PlanItemInstancesWrapper suspended() {
        return getPlanItemInstancesWithState(PlanItemInstanceState.SUSPENDED);
    }

    public PlanItemInstancesWrapper terminated() {
        return getPlanItemInstancesWithState(PlanItemInstanceState.TERMINATED);
    }

    public PlanItemInstancesWrapper unavailable() {
        return getPlanItemInstancesWithState(PlanItemInstanceState.UNAVAILABLE);
    }

    public PlanItemInstancesWrapper waitingForRepetition() {
        return getPlanItemInstancesWithState(PlanItemInstanceState.WAITING_FOR_REPETITION);
    }

    public PlanItemInstancesWrapper asyncActive() {
        return getPlanItemInstancesWithState(PlanItemInstanceState.ASYNC_ACTIVE);
    }

    public PlanItemInstancesWrapper asyncActiveLeave() {
        return getPlanItemInstancesWithState(PlanItemInstanceState.ASYNC_ACTIVE_LEAVE);
    }

    public PlanItemInstancesWrapper onlyTerminal() {
        ensurePlanItemInstanceInitialized();

        List<PlanItemInstanceEntity> filteredPlanItemInstances = planItemInstances.stream()
            .filter(planItemInstanceEntity -> planItemInstanceEntity.getPlanItem() != null
                && planItemInstanceEntity.getPlanItem().getPlanItemDefinition() != null
                && PlanItemInstanceState.isInTerminalState(planItemInstanceEntity))
            .collect(Collectors.toList());

        return new PlanItemInstancesWrapper(variableContainer, caseInstanceEntity, filteredPlanItemInstances);
    }

    public PlanItemInstancesWrapper onlyNonTerminal() {
        ensurePlanItemInstanceInitialized();

        List<PlanItemInstanceEntity> filteredPlanItemInstances = planItemInstances.stream()
            .filter(planItemInstanceEntity -> planItemInstanceEntity.getPlanItem() != null
                && planItemInstanceEntity.getPlanItem().getPlanItemDefinition() != null
                && !PlanItemInstanceState.isInTerminalState(planItemInstanceEntity))
            .collect(Collectors.toList());

        return new PlanItemInstancesWrapper(variableContainer, caseInstanceEntity, filteredPlanItemInstances);
    }

    public List<PlanItemInstanceEntity> getList() {
        ensurePlanItemInstanceInitialized();
        return planItemInstances;
    }

    // Helper methods

    protected PlanItemInstancesWrapper getPlanItemInstancesWithState(String state) {
        ensurePlanItemInstanceInitialized();

        List<PlanItemInstanceEntity> filteredPlanItemInstances = planItemInstances.stream()
            .filter(planItemInstanceEntity -> Objects.equals(state, planItemInstanceEntity.getState()))
            .collect(Collectors.toList());

        return new PlanItemInstancesWrapper(variableContainer, caseInstanceEntity, filteredPlanItemInstances);
    }

    protected List<PlanItemInstanceEntity> collectAllChildPlanItemInstances(PlanItemInstanceContainer planItemInstanceContainer) {
        List<PlanItemInstanceEntity> childPlanItemInstances = new ArrayList<>();
        collectAllChildPlanItemInstances(CommandContextUtil.getCommandContext(), planItemInstanceContainer, childPlanItemInstances);
        return childPlanItemInstances;
    }

    protected void collectAllChildPlanItemInstances(CommandContext commandContext,
            PlanItemInstanceContainer planItemInstanceContainer, List<PlanItemInstanceEntity> childPlanItemInstances) {

        List<PlanItemInstanceEntity> planItemInstances = null;
        if (planItemInstanceContainer instanceof CaseInstance) {
            planItemInstances = CommandContextUtil.getPlanItemInstanceEntityManager(commandContext).findByCaseInstanceId(((CaseInstance) planItemInstanceContainer).getId());
        } else if (planItemInstanceContainer instanceof PlanItemInstance){
            planItemInstances = CommandContextUtil.getPlanItemInstanceEntityManager(commandContext).findByStagePlanItemInstanceId(((PlanItemInstance) planItemInstanceContainer).getId());
        } else {
            throw new FlowableException("Programmatic error: unknown variable container type: " + variableContainer);
        }

        for (PlanItemInstanceEntity childPlanItemInstance : planItemInstances) {
            if (!contains(childPlanItemInstances, childPlanItemInstance)) {
                childPlanItemInstances.add(childPlanItemInstance);
            }

            if (childPlanItemInstance.getPlanItem() != null && childPlanItemInstance.getPlanItem().getPlanItemDefinition() instanceof Stage) {
                collectAllChildPlanItemInstances(commandContext, childPlanItemInstance, childPlanItemInstances);
            }
        }

    }

    protected boolean contains(List<PlanItemInstanceEntity> planItemInstances, PlanItemInstanceEntity planItemInstanceEntity) {
        return planItemInstances.stream()
            .anyMatch(p -> Objects.equals(p.getId(), planItemInstanceEntity.getId()));
    }

}
