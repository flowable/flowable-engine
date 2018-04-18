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

package org.flowable.rest.service.api.repository;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Authorization;

import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.engine.repository.Model;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author Frederik Heremans
 */
@RestController
@Api(tags = { "Models" }, description = "Manage Models", authorizations = { @Authorization(value = "basicAuth") })
public class ModelSourceResource extends BaseModelSourceResource {

    @ApiOperation(value = "Get the editor source for a model", tags = { "Models" },
            notes = "Response body contains the model’s raw editor source. The response’s content-type is set to application/octet-stream, regardless of the content of the source.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Indicates the model was found and source is returned."),
            @ApiResponse(code = 404, message = "Indicates the requested model was not found.")
    })
    @GetMapping("/repository/models/{modelId}/source")
    @ResponseBody
    public byte[] getModelBytes(@ApiParam(name = "modelId") @PathVariable String modelId, HttpServletResponse response) {
        byte[] editorSource = repositoryService.getModelEditorSource(modelId);
        if (editorSource == null) {
            throw new FlowableObjectNotFoundException("Model with id '" + modelId + "' does not have source available.", String.class);
        }
        response.setContentType("application/octet-stream");
        return editorSource;
    }

    @ApiOperation(value = "Set the editor source for a model", tags = { "Models" }, consumes = "multipart/form-data",
            notes = "Response body contains the model’s raw editor source. The response’s content-type is set to application/octet-stream, regardless of the content of the source.")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "file", dataType = "file", paramType = "form", required = true)
    })
    @ApiResponses(value = {
            @ApiResponse(code = 204, message = "Indicates the model was found and the source has been updated."),
            @ApiResponse(code = 404, message = "Indicates the requested model was not found.")
    })
    @PutMapping(value = "/repository/models/{modelId}/source", consumes = "multipart/form-data")
    public void setModelSource(@ApiParam(name = "modelId") @PathVariable String modelId, HttpServletRequest request, HttpServletResponse response) {
        Model model = getModelFromRequest(modelId);
        if (model != null) {

            if (!(request instanceof MultipartHttpServletRequest)) {
                throw new FlowableIllegalArgumentException("Multipart request is required");
            }

            MultipartHttpServletRequest multipartRequest = (MultipartHttpServletRequest) request;

            if (multipartRequest.getFileMap().size() == 0) {
                throw new FlowableIllegalArgumentException("Multipart request with file content is required");
            }

            MultipartFile file = multipartRequest.getFileMap().values().iterator().next();

            try {
                repositoryService.addModelEditorSource(modelId, file.getBytes());
                response.setStatus(HttpStatus.NO_CONTENT.value());
            } catch (Exception e) {
                throw new FlowableException("Error adding model editor source extra", e);
            }
        }
    }
}
