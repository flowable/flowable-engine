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

package org.flowable.cmmn.engine.impl.cmd;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.flowable.cmmn.api.StageResponse;
import org.flowable.cmmn.api.history.HistoricPlanItemInstance;
import org.flowable.cmmn.api.repository.CaseDefinition;
import org.flowable.cmmn.api.runtime.PlanItemDefinitionType;
import org.flowable.cmmn.api.runtime.PlanItemInstanceState;
import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.impl.deployer.CmmnDeploymentManager;
import org.flowable.cmmn.engine.impl.history.HistoricPlanItemInstanceQueryImpl;
import org.flowable.cmmn.engine.impl.persistence.entity.HistoricCaseInstanceEntity;
import org.flowable.cmmn.engine.impl.persistence.entity.HistoricPlanItemInstanceEntityManager;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.cmmn.model.CmmnModel;
import org.flowable.cmmn.model.Milestone;
import org.flowable.cmmn.model.PlanItemDefinition;
import org.flowable.cmmn.model.Stage;
import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;

/**
 * @author Tijs Rademakers
 */
public class GetHistoricStageOverviewCmd implements Command<List<StageResponse>>, Serializable {

    private static final long serialVersionUID = 1L;
    
    protected String caseInstanceId;

    public GetHistoricStageOverviewCmd(String caseInstanceId) {
        this.caseInstanceId = caseInstanceId;
    }

    @Override
    public List<StageResponse> execute(CommandContext commandContext) {
        CmmnEngineConfiguration cmmnEngineConfiguration = CommandContextUtil.getCmmnEngineConfiguration(commandContext);
        HistoricCaseInstanceEntity caseInstance = cmmnEngineConfiguration.getHistoricCaseInstanceEntityManager().findById(caseInstanceId);
        if (caseInstance == null) {
            throw new FlowableObjectNotFoundException("No case instance found for id " + caseInstanceId, HistoricCaseInstanceEntity.class);
        }

        HistoricPlanItemInstanceEntityManager planItemInstanceEntityManager = cmmnEngineConfiguration.getHistoricPlanItemInstanceEntityManager();
        List<HistoricPlanItemInstance> planItemInstances = planItemInstanceEntityManager.findByCriteria(new HistoricPlanItemInstanceQueryImpl()
            .planItemInstanceCaseInstanceId(caseInstanceId)
            .planItemInstanceDefinitionTypes(Arrays.asList(PlanItemDefinitionType.STAGE, PlanItemDefinitionType.MILESTONE))
            .orderByEndedTime().asc());

        // Filter out the states that shouldn't be returned in the overview
        planItemInstances.removeIf(planItemInstance -> {
            return Objects.equals(PlanItemInstanceState.WAITING_FOR_REPETITION, planItemInstance.getState())
                || Objects.equals(PlanItemInstanceState.ASYNC_ACTIVE, planItemInstance.getState());
        });

        CmmnDeploymentManager deploymentManager = cmmnEngineConfiguration.getDeploymentManager();
        CaseDefinition caseDefinition = deploymentManager.findDeployedCaseDefinitionById(caseInstance.getCaseDefinitionId());
        CmmnModel cmmnModel = deploymentManager.resolveCaseDefinition(caseDefinition).getCmmnModel();
        List<Stage> stages = cmmnModel.getPrimaryCase().getPlanModel().findPlanItemDefinitionsOfType(Stage.class, true);
        List<Milestone> milestones = cmmnModel.getPrimaryCase().getPlanModel().findPlanItemDefinitionsOfType(Milestone.class, true);
        
        List<OverviewElement> overviewElements = new ArrayList<>();
        for (Stage stage : stages) {
            overviewElements.add(new OverviewElement(stage.getId(), stage.getName(), stage.getDisplayOrder(), stage.getIncludeInStageOverview(), stage));
        }
        for (Milestone milestone : milestones) {
            overviewElements.add(new OverviewElement(milestone.getId(), milestone.getName(), milestone.getDisplayOrder(), milestone.getIncludeInStageOverview(), milestone));
        }

        // If one stage has a display order, they are ordered by that.
        // Otherwise, the order as it comes back from the query is used.
        overviewElements.sort(Comparator.comparing(OverviewElement::getDisplayOrder, Comparator.nullsFirst(Comparator.naturalOrder()))
            .thenComparing(overviewElement -> getPlanItemInstanceEndTime(planItemInstances, overviewElement.getPlanItemDefinition()), Comparator.nullsLast(Comparator.naturalOrder()))
        );
        
        List<StageResponse> stageResponses = new ArrayList<>(stages.size());
        for (OverviewElement overviewElement : overviewElements) {
            Optional<HistoricPlanItemInstance> planItemInstance = getPlanItemInstance(planItemInstances, overviewElement.getPlanItemDefinition());
            boolean showInOverview = false;
            
            // If not ended or current, it's implicitly a future one
            if (planItemInstance.isPresent()) {
                if (planItemInstance.get().isShowInOverview()) {
                    showInOverview = true;
                }
            
            } else if (caseInstance.getEndTime() == null) {
                // It can be a future one only if the case instance is not completed
                if (!"false".equalsIgnoreCase(overviewElement.getIncludeInStageOverview())) {
                    showInOverview = true;
                }
            }
            
            if (showInOverview) {
                StageResponse stageResponse = new StageResponse(overviewElement.getId(), overviewElement.getName());
                
                // If not ended or current, it's implicitly a future one
                if (planItemInstance.isPresent()) {
                    stageResponse.setEndTime(planItemInstance.get().getEndedTime());
                    stageResponse.setEnded(stageResponse.getEndTime() != null);
                    stageResponse.setCurrent(PlanItemInstanceState.ACTIVE.equals(planItemInstance.get().getState()));
                }

                stageResponses.add(stageResponse);
            }
        }

        return stageResponses;
    }
    
    protected Date getPlanItemInstanceEndTime(List<HistoricPlanItemInstance> planItemInstances, PlanItemDefinition planItemDefinition) {
        return getPlanItemInstance(planItemInstances, planItemDefinition)
            .map(HistoricPlanItemInstance::getEndedTime)
            .orElse(null);
    }

    protected Optional<HistoricPlanItemInstance> getPlanItemInstance(List<HistoricPlanItemInstance> planItemInstances, PlanItemDefinition planItemDefinition) {
        HistoricPlanItemInstance planItemInstance = null;
        for (HistoricPlanItemInstance p : planItemInstances) {
            if (p.getPlanItemDefinitionId().equals(planItemDefinition.getId())) {
                
                if (p.getEndedTime() == null) {
                    planItemInstance = p; // one that's not ended yet has precedence
                } else if (planItemInstance == null) {
                    planItemInstance = p;
                }
            }
        }
        return Optional.ofNullable(planItemInstance);
    }
    
    protected class OverviewElement {
        
        protected String id;
        protected String name;
        protected Integer displayOrder;
        protected String includeInStageOverview;
        protected PlanItemDefinition planItemDefinition;
        
        public OverviewElement(String id, String name, Integer displayOrder, String includeInStageOverview, PlanItemDefinition planItemDefinition) {
            this.id = id;
            this.name = name;
            this.displayOrder = displayOrder;
            this.includeInStageOverview = includeInStageOverview;
            this.planItemDefinition = planItemDefinition;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Integer getDisplayOrder() {
            return displayOrder;
        }

        public void setDisplayOrder(Integer displayOrder) {
            this.displayOrder = displayOrder;
        }

        public String getIncludeInStageOverview() {
            return includeInStageOverview;
        }

        public void setIncludeInStageOverview(String includeInStageOverview) {
            this.includeInStageOverview = includeInStageOverview;
        }

        public PlanItemDefinition getPlanItemDefinition() {
            return planItemDefinition;
        }

        public void setPlanItemDefinition(PlanItemDefinition planItemDefinition) {
            this.planItemDefinition = planItemDefinition;
        }
    }
}
