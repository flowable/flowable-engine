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
package org.activiti.app.rest.idm;

import javax.servlet.http.HttpServletRequest;

import org.activiti.app.idm.model.UserInformation;
import org.activiti.app.idm.service.UserService;
import org.activiti.app.model.common.GroupRepresentation;
import org.activiti.app.model.common.UserRepresentation;
import org.activiti.app.security.SecurityUtils;
import org.activiti.app.service.exception.NotFoundException;
import org.activiti.app.service.exception.UnauthorizedException;
import org.activiti.idm.api.Group;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * REST controller for managing the current user's account.
 * 
 * @author Joram Barrez
 */
@RestController
public class AccountResource {
	
	@Autowired
	private ObjectMapper objectMapper;
	
	@Autowired
	private UserService userService;

  /**
   * GET  /rest/authenticate -> check if the user is authenticated, and return its full name.
   */
  @RequestMapping(value = "/rest/authenticate", method = RequestMethod.GET, produces = {"application/json"})
  public ObjectNode isAuthenticated(HttpServletRequest request) {
    String user = request.getRemoteUser();
    
    if(user == null) {
        throw new UnauthorizedException("Request did not contain valid authorization");
    }
    
    ObjectNode result = objectMapper.createObjectNode();
    result.put("login", user);
    return result;
  }

  /**
   * GET  /rest/account -> get the current user.
   */
  @RequestMapping(value = "/rest/account", method = RequestMethod.GET, produces = "application/json")
  public UserRepresentation getAccount() {
    String userId = SecurityUtils.getCurrentActivitiAppUser().getUserObject().getId();
    UserInformation userInformation = userService.getUserInformation(userId);
    if (userInformation != null) {
      UserRepresentation userRepresentation = new UserRepresentation(userInformation.getUser());
      if (userInformation.getGroups() != null) {
        for (Group group : userInformation.getGroups()) {
          userRepresentation.getGroups().add(new GroupRepresentation(group));
        }
      }
      if (userInformation.getPrivileges() != null) {
        for (String privilege : userInformation.getPrivileges()) {
          userRepresentation.getPrivileges().add(privilege);
        }
      }
      return userRepresentation;
    } else {
      throw new NotFoundException();
    }
  }
}
