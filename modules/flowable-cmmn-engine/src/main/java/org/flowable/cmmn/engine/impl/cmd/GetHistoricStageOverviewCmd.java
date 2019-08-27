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
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.flowable.cmmn.api.StageResponse;
import org.flowable.cmmn.api.history.HistoricPlanItemInstance;
import org.flowable.cmmn.api.repository.CaseDefinition;
import org.flowable.cmmn.api.runtime.PlanItemDefinitionType;
import org.flowable.cmmn.api.runtime.PlanItemInstanceState;
import org.flowable.cmmn.engine.impl.deployer.CmmnDeploymentManager;
import org.flowable.cmmn.engine.impl.history.HistoricPlanItemInstanceQueryImpl;
import org.flowable.cmmn.engine.impl.persistence.entity.HistoricCaseInstanceEntity;
import org.flowable.cmmn.engine.impl.persistence.entity.HistoricPlanItemInstanceEntityManager;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.cmmn.model.CmmnModel;
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
        HistoricCaseInstanceEntity caseInstance = CommandContextUtil.getHistoricCaseInstanceEntityManager(commandContext).findById(caseInstanceId);
        if (caseInstance == null) {
            throw new FlowableObjectNotFoundException("No case instance found for id " + caseInstanceId, HistoricCaseInstanceEntity.class);
        }

        HistoricPlanItemInstanceEntityManager planItemInstanceEntityManager = CommandContextUtil.getHistoricPlanItemInstanceEntityManager(commandContext);
        List<HistoricPlanItemInstance> stagePlanItemInstances = planItemInstanceEntityManager.findByCriteria(new HistoricPlanItemInstanceQueryImpl()
            .planItemInstanceCaseInstanceId(caseInstanceId)
            .planItemInstanceDefinitionType(PlanItemDefinitionType.STAGE)
            .orderByEndedTime().asc());

        CmmnDeploymentManager deploymentManager = CommandContextUtil.getCmmnEngineConfiguration(commandContext).getDeploymentManager();
        CaseDefinition caseDefinition = deploymentManager.findDeployedCaseDefinitionById(caseInstance.getCaseDefinitionId());
        CmmnModel cmmnModel = deploymentManager.resolveCaseDefinition(caseDefinition).getCmmnModel();
        List<Stage> stages = cmmnModel.getPrimaryCase().getPlanModel().findPlanItemDefinitionsOfType(Stage.class, true);

        // If one stage has a display order, they are ordered by that.
        // Otherwise, the order as it comes back from the query is used.
        stages.sort(Comparator.comparing(Stage::getDisplayOrder, Comparator.nullsFirst(Comparator.naturalOrder()))
            .thenComparing(stage -> getPlanItemInstanceEndTime(stagePlanItemInstances, stage), Comparator.nullsLast(Comparator.naturalOrder()))
        );
        
        List<StageResponse> stageResponses = new ArrayList<>(stages.size());
        for (Stage stage : stages) {
            Optional<HistoricPlanItemInstance> planItemInstance = getPlanItemInstance(stagePlanItemInstances, stage);
            boolean showInOverview = false;
            
            // If not ended or current, it's implicitly a future one
            if (planItemInstance.isPresent()) {
                if (planItemInstance.get().isShowInOverview()) {
                    showInOverview = true;
                }
            
            } else {
                showInOverview = true;
            }
            
            if (showInOverview) {
                StageResponse stageResponse = new StageResponse(stage.getId(), stage.getName());
                
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
    
    protected Date getPlanItemInstanceEndTime(List<HistoricPlanItemInstance> stagePlanItemInstances, Stage stage) {
        return getPlanItemInstance(stagePlanItemInstances, stage)
            .map(HistoricPlanItemInstance::getEndedTime)
            .orElse(null);
    }

    protected Optional<HistoricPlanItemInstance> getPlanItemInstance(List<HistoricPlanItemInstance> stagePlanItemInstances, Stage stage) {
        HistoricPlanItemInstance planItemInstance = null;
        for (HistoricPlanItemInstance p : stagePlanItemInstances) {
            if (p.getPlanItemDefinitionId().equals(stage.getId())) {
                if (p.getEndedTime() == null) {
                    planItemInstance = p; // one that's not ended yet has precedence
                } else {
                    if (planItemInstance == null) {
                        planItemInstance = p;
                    }
                }

            }
        }
        return Optional.ofNullable(planItemInstance);
    }
}
