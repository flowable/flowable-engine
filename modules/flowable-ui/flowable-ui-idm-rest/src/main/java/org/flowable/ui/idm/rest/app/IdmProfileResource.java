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
package org.flowable.ui.idm.rest.app;

import org.apache.commons.lang3.tuple.Pair;
import org.flowable.idm.api.Group;
import org.flowable.idm.api.User;
import org.flowable.ui.common.model.GroupRepresentation;
import org.flowable.ui.common.model.UserRepresentation;
import org.flowable.ui.common.security.SecurityUtils;
import org.flowable.ui.common.service.exception.InternalServerErrorException;
import org.flowable.ui.common.service.exception.NotFoundException;
import org.flowable.ui.idm.model.ChangePasswordRepresentation;
import org.flowable.ui.idm.service.GroupService;
import org.flowable.ui.idm.service.ProfileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
/**
 *
 * @author Joram Barrez
 * @author Tijs Rademakers
 */
@RestController
@RequestMapping(value = "/app/rest/admin")
public class IdmProfileResource {

    @Autowired
    protected ProfileService profileService;

    @Autowired
    protected GroupService groupService;

    @GetMapping(value = "/profile", produces = "application/json")
    public UserRepresentation getProfile() {
        User user = SecurityUtils.getCurrentFlowableAppUser().getUserObject();
        UserRepresentation userRepresentation = new UserRepresentation(user);
        for (Group group : groupService.getGroupsForUser(user.getId())) {
            userRepresentation.getGroups().add(new GroupRepresentation(group));
        }
        return userRepresentation;
    }

    @PostMapping(value = "/profile", produces = "application/json")
    public UserRepresentation updateProfile(@RequestBody UserRepresentation userRepresentation) {
        return new UserRepresentation(profileService.updateProfile(userRepresentation.getFirstName(),
                userRepresentation.getLastName(),
                userRepresentation.getEmail()));
    }

    @ResponseStatus(value = HttpStatus.OK)
    @PostMapping(value = "/profile-password", produces = "application/json")
    public void changePassword(@RequestBody ChangePasswordRepresentation changePasswordRepresentation) {
        profileService.changePassword(changePasswordRepresentation.getOriginalPassword(), changePasswordRepresentation.getNewPassword());
    }

    @GetMapping(value = "/profile-picture")
    public void getProfilePicture(HttpServletResponse response) {
        try {
            Pair<String, InputStream> picture = profileService.getProfilePicture();
            if (picture == null) {
                throw new NotFoundException();
            }
            response.setContentType(picture.getLeft());
            ServletOutputStream servletOutputStream = response.getOutputStream();

            byte[] buffer = new byte[32384];
            while (true) {
                int count = picture.getRight().read(buffer);
                if (count == -1)
                    break;
                servletOutputStream.write(buffer, 0, count);
            }

            // Flush and close stream
            servletOutputStream.flush();
            servletOutputStream.close();
        } catch (Exception e) {
            throw new InternalServerErrorException("Could not get profile picture", e);
        }
    }

    @ResponseStatus(value = HttpStatus.OK)
    @PostMapping(value = "/profile-picture", produces = "application/json")
    public void uploadProfilePicture(@RequestParam("file") MultipartFile file) {
        try {
            profileService.uploadProfilePicture(file.getContentType(), file.getBytes());
        } catch (IOException e) {
            throw new InternalServerErrorException(e.getMessage(), e);
        }
    }

}
