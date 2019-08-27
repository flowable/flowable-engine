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

import org.apache.commons.lang3.StringUtils;
import org.flowable.cmmn.api.StageResponse;
import org.flowable.cmmn.api.repository.CaseDefinition;
import org.flowable.cmmn.api.runtime.PlanItemDefinitionType;
import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstanceState;
import org.flowable.cmmn.engine.impl.deployer.CmmnDeploymentManager;
import org.flowable.cmmn.engine.impl.persistence.entity.CaseInstanceEntity;
import org.flowable.cmmn.engine.impl.persistence.entity.PlanItemInstanceEntityManager;
import org.flowable.cmmn.engine.impl.runtime.PlanItemInstanceQueryImpl;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.cmmn.model.CmmnModel;
import org.flowable.cmmn.model.Stage;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.common.engine.api.delegate.Expression;
import org.flowable.common.engine.api.variable.VariableContainer;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;

/**
 * @author Tijs Rademakers
 */
public class GetStageOverviewCmd implements Command<List<StageResponse>>, Serializable {

    private static final long serialVersionUID = 1L;
    protected String caseInstanceId;

    public GetStageOverviewCmd(String caseInstanceId) {
        this.caseInstanceId = caseInstanceId;
    }

    @Override
    public List<StageResponse> execute(CommandContext commandContext) {
        CaseInstanceEntity caseInstance = CommandContextUtil.getCaseInstanceEntityManager(commandContext).findById(caseInstanceId);
        if (caseInstance == null) {
            throw new FlowableObjectNotFoundException("No case instance found for id " + caseInstanceId, CaseInstanceEntity.class);
        }

        PlanItemInstanceEntityManager planItemInstanceEntityManager = CommandContextUtil.getPlanItemInstanceEntityManager(commandContext);
        List<PlanItemInstance> stagePlanItemInstances = planItemInstanceEntityManager.findByCriteria(new PlanItemInstanceQueryImpl(commandContext)
            .caseInstanceId(caseInstanceId)
            .planItemDefinitionType(PlanItemDefinitionType.STAGE)
            .includeEnded()
            .orderByEndTime().asc());

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
            boolean includeInStageOverview = false;
            if ("true".equalsIgnoreCase(stage.getIncludeInStageOverview())) {
                includeInStageOverview = true;
            
            } else if (StringUtils.isNotEmpty(stage.getIncludeInStageOverview()) && !"false".equalsIgnoreCase(stage.getIncludeInStageOverview())) {
                Expression stageExpression = CommandContextUtil.getExpressionManager(commandContext).createExpression(stage.getIncludeInStageOverview());
                Optional<PlanItemInstance> planItemInstance = getPlanItemInstance(stagePlanItemInstances, stage);
                if (planItemInstance.isPresent()) {
                    includeInStageOverview = evaluateIncludeInStageOverviewExpression(stageExpression, stage.getIncludeInStageOverview(), 
                                    (VariableContainer) planItemInstance.get());
                
                } else {
                    includeInStageOverview = evaluateIncludeInStageOverviewExpression(stageExpression, stage.getIncludeInStageOverview(), 
                                    (VariableContainer) caseInstance);
                }
            }
            
            if (includeInStageOverview) {
                StageResponse stageResponse = new StageResponse(stage.getId(), stage.getName());
                Optional<PlanItemInstance> planItemInstance = getPlanItemInstance(stagePlanItemInstances, stage);

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
    
    protected boolean evaluateIncludeInStageOverviewExpression(Expression stageExpression, String includeInStageOverview, VariableContainer variableContainer) {
        Object stageValueObject = stageExpression.getValue(variableContainer);
        if (!(stageValueObject instanceof Boolean)) {
            throw new FlowableException("Include in stage overview expression does not resolve to a boolean value " + 
                            includeInStageOverview + ": " + stageValueObject);
        }
        
        return (Boolean) stageValueObject;
    }
    
    protected Date getPlanItemInstanceEndTime(List<PlanItemInstance> stagePlanItemInstances, Stage stage) {
        return getPlanItemInstance(stagePlanItemInstances, stage)
            .map(PlanItemInstance::getEndedTime)
            .orElse(null);
    }

    protected Optional<PlanItemInstance> getPlanItemInstance(List<PlanItemInstance> stagePlanItemInstances, Stage stage) {
        PlanItemInstance planItemInstance = null;
        for (PlanItemInstance p : stagePlanItemInstances) {
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
