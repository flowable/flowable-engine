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

import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.flowable.cmmn.api.CmmnHistoryService;
import org.flowable.cmmn.api.CmmnMigrationService;
import org.flowable.cmmn.api.CmmnRepositoryService;
import org.flowable.cmmn.api.CmmnRuntimeService;
import org.flowable.cmmn.api.StageResponse;
import org.flowable.cmmn.api.history.HistoricCaseInstance;
import org.flowable.cmmn.api.history.HistoricPlanItemInstance;
import org.flowable.cmmn.api.migration.HistoricCaseInstanceMigrationDocument;
import org.flowable.cmmn.api.repository.CaseDefinition;
import org.flowable.cmmn.engine.impl.migration.HistoricCaseInstanceMigrationDocumentConverter;
import org.flowable.cmmn.model.Stage;
import org.flowable.cmmn.rest.service.api.CmmnRestResponseFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
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
@Api(tags = { "History Case" }, authorizations = { @Authorization(value = "basicAuth") })
public class HistoricCaseInstanceResource extends HistoricCaseInstanceBaseResource {

    @Autowired
    protected CmmnRestResponseFactory restResponseFactory;

    @Autowired
    protected CmmnRepositoryService cmmnRepositoryService;

    @Autowired
    protected CmmnHistoryService cmmnhistoryService;
    
    @Autowired
    protected CmmnRuntimeService cmmnRuntimeService;
    
    @Autowired
    protected CmmnMigrationService cmmnMigrationService;

    @ApiOperation(value = "Get a historic case instance", tags = { "History Case" }, nickname = "getHistoricCaseInstance")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Indicates that the historic process instances could be found."),
            @ApiResponse(code = 404, message = "Indicates that the historic process instances could not be found.") })
    @GetMapping(value = "/cmmn-history/historic-case-instances/{caseInstanceId}", produces = "application/json")
    public HistoricCaseInstanceResponse getCaseInstance(@ApiParam(name = "caseInstanceId") @PathVariable String caseInstanceId) {
        HistoricCaseInstanceResponse caseInstanceResponse = restResponseFactory.createHistoricCaseInstanceResponse(getHistoricCaseInstanceFromRequest(caseInstanceId));
        
        CaseDefinition caseDefinition = cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionId(caseInstanceResponse.getCaseDefinitionId()).singleResult();
        if (caseDefinition != null) {
            caseInstanceResponse.setCaseDefinitionName(caseDefinition.getName());
            caseInstanceResponse.setCaseDefinitionDescription(caseDefinition.getDescription());
        }
        
        return caseInstanceResponse;
    }

    @ApiOperation(value = " Delete a historic case instance", tags = { "History Case" }, nickname = "deleteHistoricCaseInstance", code = 204)
    @ApiResponses(value = {
            @ApiResponse(code = 204, message = "Indicates that the historic process instance was deleted."),
            @ApiResponse(code = 404, message = "Indicates that the historic process instance could not be found.") })
    @DeleteMapping(value = "/cmmn-history/historic-case-instances/{caseInstanceId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCaseInstance(@ApiParam(name = "caseInstanceId") @PathVariable String caseInstanceId) {
        HistoricCaseInstance caseInstance = getHistoricCaseInstanceFromRequestWithoutAccessCheck(caseInstanceId);
        if (restApiInterceptor != null) {
            restApiInterceptor.deleteHistoricCase(caseInstance);
        }
        
        cmmnhistoryService.deleteHistoricCaseInstance(caseInstance.getId());
    }
    
    @ApiOperation(value = "Get a stage overview of historic case instance", tags = { "History Case" }, nickname = "getHistoricStageOverview", code = 204)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Indicates that the historic case instance was found."),
            @ApiResponse(code = 404, message = "Indicates that the historic case instance could not be found.") })
    @GetMapping(value = "/cmmn-history/historic-case-instances/{caseInstanceId}/stage-overview", produces = "application/json")
    public List<StageResponse> getStageOverview(@ApiParam(name = "caseInstanceId") @PathVariable String caseInstanceId) {
        HistoricCaseInstance caseInstance = getHistoricCaseInstanceFromRequest(caseInstanceId);

        return cmmnhistoryService.getStageOverview(caseInstance.getId());
    }
    
    @ApiOperation(value = "Migrate historic case instance", tags = { "History Case" }, nickname = "migrateHistoricCaseInstance")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Indicates the historiccase instance was found and migration was executed."),
            @ApiResponse(code = 404, message = "Indicates the requested historic case instance was not found.")
    })
    @PostMapping(value = "/cmmn-history/historic-case-instances/{caseInstanceId}/migrate", produces = "application/json")
    public void migrateHistoricCaseInstance(@ApiParam(name = "caseInstanceId") @PathVariable String caseInstanceId,
                                       @RequestBody String migrationDocumentJson) {

        HistoricCaseInstance caseInstance = getHistoricCaseInstanceFromRequestWithoutAccessCheck(caseInstanceId);
        
        if (restApiInterceptor != null) {
            restApiInterceptor.migrateHistoricCaseInstance(caseInstance, migrationDocumentJson);
        }

        HistoricCaseInstanceMigrationDocument migrationDocument = HistoricCaseInstanceMigrationDocumentConverter.convertFromJson(migrationDocumentJson);
        cmmnMigrationService.migrateHistoricCaseInstance(caseInstanceId, migrationDocument);
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
