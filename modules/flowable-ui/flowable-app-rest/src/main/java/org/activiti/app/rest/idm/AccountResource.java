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

import java.util.List;

import org.activiti.app.model.idm.GroupRepresentation;
import org.activiti.app.model.idm.UserRepresentation;
import org.activiti.engine.IdentityService;
import org.activiti.idm.api.Group;
import org.activiti.idm.api.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * REST controller for managing the current user's account.
 * 
 * @author Joram Barrez
 * @author Tijs Rademakers
 */
@RestController
public class AccountResource {
	
	@Autowired
	private ObjectMapper objectMapper;
	
	@Autowired
	private IdentityService identityService;

  /**
   * GET  /rest/account -> get the current user.
   */
  @RequestMapping(value = "/rest/account", method = RequestMethod.GET, produces = "application/json")
  public UserRepresentation getAccount() {
    User user = identityService.createUserQuery().list().get(0);
    //User user = SecurityUtils.getCurrentActivitiAppUser().getUserObject();
    
    UserRepresentation userRepresentation = new UserRepresentation(user);
    
    List<Group> groups = identityService.createGroupQuery().groupMember(user.getId()).list();
    for (Group group : groups) {
      userRepresentation.getGroups().add(new GroupRepresentation(group));
    }
    
    return userRepresentation;
  }
}
