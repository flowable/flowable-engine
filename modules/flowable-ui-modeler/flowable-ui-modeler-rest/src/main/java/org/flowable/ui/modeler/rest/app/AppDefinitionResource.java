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
package org.flowable.ui.modeler.rest.app;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.flowable.ui.common.service.exception.InternalServerErrorException;
import org.flowable.ui.modeler.model.AppDefinitionPublishRepresentation;
import org.flowable.ui.modeler.model.AppDefinitionRepresentation;
import org.flowable.ui.modeler.model.AppDefinitionSaveRepresentation;
import org.flowable.ui.modeler.model.AppDefinitionUpdateResultRepresentation;
import org.flowable.ui.modeler.service.AppDefinitionExportService;
import org.flowable.ui.modeler.service.AppDefinitionImportService;
import org.flowable.ui.modeler.serviceapi.AppDefinitionService;
import org.flowable.ui.modeler.serviceapi.ModelService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@RestController
@RequestMapping("/app")
public class AppDefinitionResource {

    @Autowired
    protected AppDefinitionService appDefinitionService;

    @Autowired
    protected AppDefinitionExportService appDefinitionExportService;

    @Autowired
    protected AppDefinitionImportService appDefinitionImportService;

    @Autowired
    protected ModelService modelService;

    @Autowired
    protected ObjectMapper objectMapper;

    private static final Logger LOGGER = LoggerFactory.getLogger(AppDefinitionResource.class);

    @GetMapping(value = "/rest/app-definitions/{modelId}", produces = "application/json")
    public AppDefinitionRepresentation getAppDefinition(@PathVariable("modelId") String modelId) {
        return appDefinitionService.getAppDefinition(modelId);
    }

    @GetMapping(value = "/rest/app-definitions/{modelId}/history/{modelHistoryId}", produces = "application/json")
    public AppDefinitionRepresentation getAppDefinitionHistory(@PathVariable String modelId, @PathVariable String modelHistoryId) {
        return appDefinitionService.getAppDefinitionHistory(modelId, modelHistoryId);
    }

    @PutMapping(value = "/rest/app-definitions/{modelId}", produces = "application/json")
    public AppDefinitionUpdateResultRepresentation updateAppDefinition(@PathVariable("modelId") String modelId, @RequestBody AppDefinitionSaveRepresentation updatedModel) {
        AppDefinitionUpdateResultRepresentation resultRepresentation = null;
        try {
            resultRepresentation = appDefinitionService.updateAppDefinition(modelId, updatedModel);
        } catch (Exception ex) {
            resultRepresentation = new AppDefinitionUpdateResultRepresentation();
            resultRepresentation.setError(true);
            resultRepresentation.setErrorDescription(ex.getMessage());
        }
        return resultRepresentation;
    }

    @PostMapping(value = "/rest/app-definitions/{modelId}/publish", produces = "application/json")
    public AppDefinitionUpdateResultRepresentation publishAppDefinition(@PathVariable("modelId") String modelId, @RequestBody AppDefinitionPublishRepresentation publishModel) {
        AppDefinitionUpdateResultRepresentation resultRepresentation = null;
        try {
            resultRepresentation = appDefinitionImportService.publishAppDefinition(modelId, publishModel);
        } catch (Exception ex) {
            resultRepresentation = new AppDefinitionUpdateResultRepresentation();
            resultRepresentation.setError(true);
            resultRepresentation.setErrorDescription(ex.getMessage());
        }
        return resultRepresentation;
    }

    @GetMapping(value = "/rest/app-definitions/{modelId}/export")
    public void exportAppDefinition(HttpServletResponse response, @PathVariable String modelId) throws IOException {
        appDefinitionExportService.exportAppDefinition(response, modelId);
    }

    @GetMapping(value = "/rest/app-definitions/{modelId}/export-bar")
    public void exportDeployableAppDefinition(HttpServletResponse response, @PathVariable String modelId) throws IOException {
        appDefinitionExportService.exportDeployableAppDefinition(response, modelId);
    }

    @Transactional
    @PostMapping(value = "/rest/app-definitions/{modelId}/import", produces = "application/json")
    public AppDefinitionRepresentation importAppDefinition(HttpServletRequest request, @PathVariable String modelId, @RequestParam("file") MultipartFile file) {
        return appDefinitionImportService.importAppDefinitionNewVersion(request, file, modelId);
    }

    @Transactional
    @PostMapping(value = "/rest/app-definitions/{modelId}/text/import")
    public String importAppDefinitionText(HttpServletRequest request, @PathVariable String modelId, @RequestParam("file") MultipartFile file) {

        AppDefinitionRepresentation appDefinitionRepresentation = appDefinitionImportService.importAppDefinitionNewVersion(request, file, modelId);
        String appDefinitionRepresentationJson = null;
        try {
            appDefinitionRepresentationJson = objectMapper.writeValueAsString(appDefinitionRepresentation);
        } catch (Exception e) {
            LOGGER.error("Error while App Definition representation json", e);
            throw new InternalServerErrorException("App definition could not be saved");
        }

        return appDefinitionRepresentationJson;
    }

    @Transactional
    @PostMapping(value = "/rest/app-definitions/import", produces = "application/json")
    public AppDefinitionRepresentation importAppDefinition(HttpServletRequest request, @RequestParam("file") MultipartFile file) {
        return appDefinitionImportService.importAppDefinition(request, file);
    }

    @Transactional
    @PostMapping(value = "/rest/app-definitions/text/import")
    public String importAppDefinitionText(HttpServletRequest request, @RequestParam("file") MultipartFile file) {
        AppDefinitionRepresentation appDefinitionRepresentation = appDefinitionImportService.importAppDefinition(request, file);
        String appDefinitionRepresentationJson = null;
        try {
            appDefinitionRepresentationJson = objectMapper.writeValueAsString(appDefinitionRepresentation);
        } catch (Exception e) {
            LOGGER.error("Error while App Definition representation json", e);
            throw new InternalServerErrorException("App definition could not be saved");
        }

        return appDefinitionRepresentationJson;
    }
}
