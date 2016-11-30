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
package org.activiti.app.idm.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.activiti.app.idm.model.UserInformation;
import org.activiti.app.service.exception.BadRequestException;
import org.activiti.app.service.exception.ConflictingRequestException;
import org.activiti.app.service.exception.NotFoundException;
import org.activiti.idm.api.Group;
import org.activiti.idm.api.Privilege;
import org.activiti.idm.api.User;
import org.activiti.idm.api.UserQuery;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Joram Barrez
 */
@Service
@Transactional
public class UserServiceImpl extends AbstractIdmService implements UserService {
  
  private static final int MAX_USER_SIZE = 100;
  
  public List<User> getUsers(String filter, String sort, Integer start, String groupId) {
    Integer startValue = start != null ? start.intValue() : 0;
    Integer size = MAX_USER_SIZE; // TODO: pass actual size
    return createUserQuery(filter, sort).listPage(startValue, (size != null && size > 0) ? size : MAX_USER_SIZE);
  }
  
  public long getUserCount(String filter, String sort, Integer start, String groupId) {
    return createUserQuery(filter, sort).count();
  }

  protected UserQuery createUserQuery(String filter, String sort) {
    UserQuery userQuery = identityService.createUserQuery();
    if (StringUtils.isNotEmpty(filter)) {
      userQuery.userFullNameLike("%" + filter + "%");
    }
    
    if (StringUtils.isNotEmpty(sort)) {
      if ("idDesc".equals(sort)) {
        userQuery.orderByUserId().desc();
      } else if ("idAsc".equals(sort)) {
        userQuery.orderByUserId().asc();
      } else if ("emailAsc".equals(sort)) {
        userQuery.orderByUserEmail().asc();
      } else if ("emailDesc".equals(sort)) {
        userQuery.orderByUserEmail().desc();
      }
      
    }
    return userQuery;
  }
  
  public void updateUserDetails(String userId, String firstName, String lastName, String email) {
    User user = identityService.createUserQuery().userId(userId).singleResult();
    if (user != null) {
      user.setFirstName(firstName);
      user.setLastName(lastName);
      user.setEmail(email);
      identityService.saveUser(user);
    }
  }
  
  public void bulkUpdatePassword(List<String> userIds, String newPassword) {
    for (String userId : userIds) {
      User user = identityService.createUserQuery().userId(userId).singleResult();
      if (user != null) {
        user.setPassword(newPassword);
        identityService.saveUser(user);
      }
    }
  }
  
  public void deleteUser(String userId) {
    List<Privilege> privileges = identityService.createPrivilegeQuery().userId(userId).list();
    for (Privilege privilege : privileges) {
      identityService.deleteUserPrivilegeMapping(privilege.getId(), userId);
    }
    
    List<Group> groups = identityService.createGroupQuery().groupMember(userId).list();
    if (groups != null && groups.size() > 0) {
      for (Group group : groups) {
        identityService.deleteMembership(userId, group.getId());
      }
    }
    identityService.deleteUser(userId);
  }

  
  public User createNewUser(String id, String firstName, String lastName, String email, String password) {
    if(StringUtils.isBlank(id) ||
        StringUtils.isBlank(password) || 
        StringUtils.isBlank(firstName)) {
        throw new BadRequestException("Id, password and first name are required");
    }
    
    if (email != null && identityService.createUserQuery().userEmail(email).count() > 0) {
      throw new ConflictingRequestException("User already registered", "ACCOUNT.SIGNUP.ERROR.ALREADY-REGISTERED");
    } 
    
    User user = identityService.newUser(id != null ? id : email);
    user.setFirstName(firstName);
    user.setLastName(lastName);
    user.setEmail(email);
    user.setPassword(password);
    identityService.saveUser(user);
    
    return user;
  }
  
  @Override
  public UserInformation getUserInformation(String userId) {
    User user = identityService.createUserQuery().userId(userId).singleResult();
    if (user == null) {
      throw new NotFoundException();
    }
    
    List<Privilege> userPrivileges = identityService.createPrivilegeQuery().userId(userId).list();
    Set<String> privilegeNames = new HashSet<String>();
    for (Privilege userPrivilege : userPrivileges) {
      privilegeNames.add(userPrivilege.getName());
    }
    
    List<Group> groups = identityService.createGroupQuery().groupMember(userId).list();
    if (groups.size() > 0) {
      List<String> groupIds = new ArrayList<String>();
      for (Group group: groups) {
        groupIds.add(group.getId());
      }
      
      List<Privilege> groupPrivileges = identityService.createPrivilegeQuery().groupIds(groupIds).list();
      for (Privilege groupPrivilege : groupPrivileges) {
        privilegeNames.add(groupPrivilege.getName());
      }
    }
    
    return new UserInformation(user, groups, new ArrayList<String>(privilegeNames));
  }
  
}
