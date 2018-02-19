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

package org.flowable.rest.idm.service.api.privilege;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.flowable.engine.common.api.FlowableObjectNotFoundException;
import org.flowable.idm.api.Group;
import org.flowable.idm.api.IdmIdentityService;
import org.flowable.idm.api.Privilege;
import org.flowable.idm.api.User;
import org.flowable.rest.idm.service.api.IdmRestResponseFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
 * @author Joram Barrez
 */
@RestController
@Api(tags = { "Privileges" }, description = "Manage Privileges", authorizations = { @Authorization(value = "basicAuth") })
public class PrivilegeResource {
    
    @Autowired
    protected IdmRestResponseFactory restResponseFactory;

    @Autowired
    protected IdmIdentityService identityService;

    @ApiOperation(value = "Get a single privilege", tags = { "Privileges" })
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Indicates the privilege exists and is returned."),
            @ApiResponse(code = 404, message = "Indicates the requested privilege does not exist.")
    })
    @GetMapping(value = "/privileges/{privilegeId}", produces = "application/json")
    public PrivilegeResponse getUser(@ApiParam(name = "privilegeId") @PathVariable String privilegeId, HttpServletRequest request) {
        Privilege privilege = identityService.createPrivilegeQuery().privilegeId(privilegeId).singleResult();
        
        if (privilege == null) {
            throw new FlowableObjectNotFoundException("Could not find privilege with id " + privilegeId, Privilege.class);
        }
        
        List<User> users = identityService.getUsersWithPrivilege(privilege.getId());
        List<Group> groups = identityService.getGroupsWithPrivilege(privilege.getId());
        
        return restResponseFactory.createPrivilegeResponse(privilege, users, groups); 
    }

}
