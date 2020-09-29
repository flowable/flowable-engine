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

package org.flowable.rest.service.api.identity;

import java.io.ByteArrayOutputStream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.idm.api.Picture;
import org.flowable.idm.api.User;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Authorization;

/**
 * @author Frederik Heremans
 */
@RestController
@Api(tags = { "Users" }, description = "Manage Users", authorizations = { @Authorization(value = "basicAuth") })
public class UserPictureResource extends BaseUserResource {


    @ApiOperation(value = "Get a user’s picture", produces = "application/octet-stream", tags = {
            "Users" }, notes = "The response body contains the raw picture data, representing the user’s picture. The Content-type of the response corresponds to the mimeType that was set when creating the picture.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Indicates the user was found and has a picture, which is returned in the body."),
            @ApiResponse(code = 404, message = "Indicates the requested user was not found or the user does not have a profile picture. Status-description contains additional information about the error.")
    })
    @GetMapping(value = "/identity/users/{userId}/picture")
    public ResponseEntity<byte[]> getUserPicture(@ApiParam(name = "userId") @PathVariable String userId, HttpServletRequest request, HttpServletResponse response) {
        User user = getUserFromRequest(userId);
        Picture userPicture = identityService.getUserPicture(user.getId());

        if (userPicture == null) {
            throw new FlowableObjectNotFoundException("The user with id '" + user.getId() + "' does not have a picture.", Picture.class);
        }

        HttpHeaders responseHeaders = new HttpHeaders();
        if (userPicture.getMimeType() != null) {
            responseHeaders.set("Content-Type", userPicture.getMimeType());
        } else {
            responseHeaders.set("Content-Type", "image/jpeg");
        }

        try {
            return new ResponseEntity<>(IOUtils.toByteArray(userPicture.getInputStream()), responseHeaders, HttpStatus.OK);
        } catch (Exception e) {
            throw new FlowableException("Error exporting picture: " + e.getMessage(), e);
        }
    }

    @ApiOperation(consumes = "multipart/form-data", value = "Updating a user’s picture", tags = {
            "Users" },  notes = "The request should be of type multipart/form-data. There should be a single file-part included with the binary value of the picture. On top of that, the following additional form-fields can be present:\n"
                    + "\n"
                    + "mimeType: Optional mime-type for the uploaded picture. If omitted, the default of image/jpeg is used as a mime-type for the picture.")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "file", dataType = "file", value = "Picture to update", paramType = "form", required = true)
    })
    @ApiResponses(value = {
            @ApiResponse(code = 204, message = "Indicates the user was found and the picture has been updated. The response-body is left empty intentionally."),
            @ApiResponse(code = 404, message = "Indicates the requested user was not found.")
    })
    @PutMapping(value = "/identity/users/{userId}/picture", consumes = "multipart/form-data")
    public void updateUserPicture(@ApiParam(name = "userId") @PathVariable String userId, HttpServletRequest request, HttpServletResponse response) {
        User user = getUserFromRequest(userId);

        if (!(request instanceof MultipartHttpServletRequest)) {
            throw new FlowableIllegalArgumentException("Multipart request is required");
        }

        MultipartHttpServletRequest multipartRequest = (MultipartHttpServletRequest) request;

        if (multipartRequest.getFileMap().size() == 0) {
            throw new FlowableIllegalArgumentException("Multipart request with file content is required");
        }

        MultipartFile file = multipartRequest.getFileMap().values().iterator().next();

        try {
            String mimeType = file.getContentType();
            int size = ((Long) file.getSize()).intValue();

            // Copy file-body in a bytearray as the engine requires this
            ByteArrayOutputStream bytesOutput = new ByteArrayOutputStream(size);
            IOUtils.copy(file.getInputStream(), bytesOutput);

            Picture newPicture = new Picture(bytesOutput.toByteArray(), mimeType);
            identityService.setUserPicture(user.getId(), newPicture);

            response.setStatus(HttpStatus.NO_CONTENT.value());

        } catch (Exception e) {
            throw new FlowableException("Error while reading uploaded file: " + e.getMessage(), e);
        }
    }
}
