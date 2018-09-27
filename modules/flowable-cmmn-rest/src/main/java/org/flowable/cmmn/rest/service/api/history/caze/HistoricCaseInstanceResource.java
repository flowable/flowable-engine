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

package org.flowable.cmmn.rest.service.api.history.caze;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import javax.servlet.http.HttpServletResponse;

import org.flowable.cmmn.api.CmmnHistoryService;
import org.flowable.cmmn.api.CmmnRepositoryService;
import org.flowable.cmmn.api.history.HistoricCaseInstance;
import org.flowable.cmmn.api.history.HistoricPlanItemInstance;
import org.flowable.cmmn.api.runtime.PlanItemDefinitionType;
import org.flowable.cmmn.api.runtime.PlanItemInstanceState;
import org.flowable.cmmn.model.CmmnModel;
import org.flowable.cmmn.model.Stage;
import org.flowable.cmmn.rest.service.api.CmmnRestResponseFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Authorization;

/**
 * @author Tijs Rademakers
 * @author Joram Barrez
 */
@RestController
@Api(tags = { "History Case" }, description = "Manage History Case Instances", authorizations = { @Authorization(value = "basicAuth") })
public class HistoricCaseInstanceResource extends HistoricCaseInstanceBaseResource {

    @Autowired
    protected CmmnRestResponseFactory restResponseFactory;

    @Autowired
    protected CmmnRepositoryService cmmnRepositoryService;

    @Autowired
    protected CmmnHistoryService cmmnhistoryService;

    @ApiOperation(value = "Get a historic case instance", tags = { "History Case" }, nickname = "getHistoricCaseInstance")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Indicates that the historic process instances could be found."),
            @ApiResponse(code = 404, message = "Indicates that the historic process instances could not be found.") })
    @GetMapping(value = "/cmmn-history/historic-case-instances/{caseInstanceId}", produces = "application/json")
    public HistoricCaseInstanceResponse getCaseInstance(@ApiParam(name = "caseInstanceId") @PathVariable String caseInstanceId) {
        return restResponseFactory.createHistoricCaseInstanceResponse(getHistoricCaseInstanceFromRequest(caseInstanceId));
    }

    @ApiOperation(value = " Delete a historic case instance", tags = { "History Case" }, nickname = "deleteHistoricCaseInstance")
    @ApiResponses(value = {
            @ApiResponse(code = 204, message = "Indicates that the historic process instance was deleted."),
            @ApiResponse(code = 404, message = "Indicates that the historic process instance could not be found.") })
    @DeleteMapping(value = "/cmmn-history/historic-case-instances/{caseInstanceId}")
    public void deleteCaseInstance(@ApiParam(name = "caseInstanceId") @PathVariable String caseInstanceId, HttpServletResponse response) {
        HistoricCaseInstance caseInstance = getHistoricCaseInstanceFromRequest(caseInstanceId);
        if (restApiInterceptor != null) {
            restApiInterceptor.deleteHistoricCase(caseInstance);
        }
        
        cmmnhistoryService.deleteHistoricCaseInstance(caseInstance.getId());
        response.setStatus(HttpStatus.NO_CONTENT.value());
    }

    @GetMapping(value = "/cmmn-history/historic-case-instances/{caseInstanceId}/stage-overview", produces = "application/json")
    public List<StageResponse> getStageOverview(@ApiParam(name = "caseInstanceId") @PathVariable String caseInstanceId) {
        HistoricCaseInstance historicCaseInstance = getHistoricCaseInstanceFromRequest(caseInstanceId);
        if (restApiInterceptor != null) {
            restApiInterceptor.accessStageOverview(historicCaseInstance);
        }

        List<HistoricPlanItemInstance> stagePlanItemInstances = cmmnhistoryService.createHistoricPlanItemInstanceQuery()
            .planItemInstanceDefinitionType(PlanItemDefinitionType.STAGE)
            .orderByEndedTime().asc()
            .list();

        CmmnModel cmmnModel = cmmnRepositoryService.getCmmnModel(historicCaseInstance.getCaseDefinitionId());
        List<Stage> stages = cmmnModel.getPrimaryCase().getPlanModel().findPlanItemDefinitionsOfType(Stage.class, true);

        // If one stage has a display order, they are ordered by that.
        // Otherwise, the order as it comes back from the query is used.
        stages.sort(Comparator.comparing(Stage::getDisplayOrder, Comparator.nullsFirst(Comparator.naturalOrder()))
            .thenComparing(stage -> getHistoricPlanItemInstanceEndTime(stagePlanItemInstances, stage), Comparator.nullsLast(Comparator.naturalOrder()))
        );
        List<StageResponse> stageResponses = new ArrayList<>(stages.size());
        for (Stage stage : stages) {
            StageResponse stageResponse = new StageResponse(stage.getId(), stage.getName());
            Optional<HistoricPlanItemInstance> historicPlanItemInstance = getHistoricPlanItemInstance(stagePlanItemInstances, stage);

            // If not ended or current, it's implicitly a future one
            if (historicPlanItemInstance.isPresent()) {
                stageResponse.setEnded(historicPlanItemInstance.get().getEndedTime() != null);
                stageResponse.setCurrent(PlanItemInstanceState.ACTIVE.equals(historicPlanItemInstance.get().getState()));
            }

            stageResponses.add(stageResponse);
        }

        return stageResponses;
    }

    protected Date getHistoricPlanItemInstanceEndTime(List<HistoricPlanItemInstance> stagePlanItemInstances, Stage stage) {
        return getHistoricPlanItemInstance(stagePlanItemInstances, stage)
            .map(HistoricPlanItemInstance::getEndedTime)
            .orElse(null);
    }

    private Optional<HistoricPlanItemInstance> getHistoricPlanItemInstance(List<HistoricPlanItemInstance> stagePlanItemInstances, Stage stage) {
        return stagePlanItemInstances.stream()
            .filter(s -> s.getPlanItemDefinitionId().equals(stage.getId()))
            .findFirst();
    }

}
