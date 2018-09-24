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

package org.flowable.cmmn.rest.service.api.runtime.caze;

import java.io.InputStream;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.flowable.cmmn.api.CmmnRepositoryService;
import org.flowable.cmmn.api.repository.CaseDefinition;
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.image.CaseDiagramGenerator;
import org.flowable.cmmn.model.CmmnModel;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
 * @author Frederik Heremans
 */
@RestController
@Api(tags = { "Case Instances" }, description = "Manage Case Instances", authorizations = { @Authorization(value = "basicAuth") })
public class CaseInstanceDiagramResource extends BaseCaseInstanceResource {

    @Autowired
    protected CmmnRepositoryService repositoryService;

    @Autowired
    protected CmmnEngineConfiguration cmmnEngineConfiguration;

    @ApiOperation(value = "Get diagram for a case instance", tags = { "Case Instances" })
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Indicates the case instance was found and the diagram was returned."),
            @ApiResponse(code = 400, message = "Indicates the requested case instance was not found but the process doesn’t contain any graphical information (CMMN DI) and no diagram can be created."),
            @ApiResponse(code = 404, message = "Indicates the requested case instance was not found.")
    })
    @GetMapping(value = "/cmmn-runtime/case-instances/{caseInstanceId}/diagram")
    public ResponseEntity<byte[]> getCaseInstanceDiagram(@ApiParam(name = "caseInstanceId") @PathVariable String caseInstanceId, HttpServletResponse response) {
        CaseInstance caseInstance = getCaseInstanceFromRequest(caseInstanceId);

        CaseDefinition caseDef = repositoryService.getCaseDefinition(caseInstance.getCaseDefinitionId());

        if (caseDef != null && caseDef.hasGraphicalNotation()) {
            CmmnModel cmmnModel = repositoryService.getCmmnModel(caseDef.getId());
            CaseDiagramGenerator diagramGenerator = cmmnEngineConfiguration.getCaseDiagramGenerator();
            InputStream resource = diagramGenerator.generateDiagram(cmmnModel, "png", cmmnEngineConfiguration.getActivityFontName(), cmmnEngineConfiguration.getLabelFontName(),
                            cmmnEngineConfiguration.getAnnotationFontName(), cmmnEngineConfiguration.getClassLoader(), 1.0);

            HttpHeaders responseHeaders = new HttpHeaders();
            responseHeaders.set("Content-Type", "image/png");
            try {
                return new ResponseEntity<>(IOUtils.toByteArray(resource), responseHeaders, HttpStatus.OK);
            } catch (Exception e) {
                throw new FlowableIllegalArgumentException("Error exporting diagram", e);
            }

        } else {
            throw new FlowableIllegalArgumentException("Case instance with id '" + caseInstance.getId() + "' has no graphical notation defined.");
        }
    }
}
