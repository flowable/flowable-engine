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
package org.flowable.engine.impl;

import java.util.List;

import org.flowable.engine.IdentityService;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.cmd.GetPotentialStarterGroupsCmd;
import org.flowable.engine.impl.cmd.GetPotentialStarterUsersCmd;
import org.flowable.engine.impl.identity.Authentication;
import org.flowable.idm.api.Group;
import org.flowable.idm.api.GroupQuery;
import org.flowable.idm.api.NativeGroupQuery;
import org.flowable.idm.api.NativeUserQuery;
import org.flowable.idm.api.Picture;
import org.flowable.idm.api.User;
import org.flowable.idm.api.UserQuery;

/**
 * @author Tom Baeyens
 */
public class IdentityServiceImpl extends ServiceImpl implements IdentityService {
  
  public IdentityServiceImpl() {

  }

  public IdentityServiceImpl(ProcessEngineConfigurationImpl processEngineConfiguration) {
    super(processEngineConfiguration);
  }

  public Group newGroup(String groupId) {
    return processEngineConfiguration.getIdmIdentityService().newGroup(groupId);
  }

  public User newUser(String userId) {
    return processEngineConfiguration.getIdmIdentityService().newUser(userId);
  }

  public void saveGroup(Group group) {
    processEngineConfiguration.getIdmIdentityService().saveGroup(group);
  }

  public void saveUser(User user) {
    processEngineConfiguration.getIdmIdentityService().saveUser(user);
  }

  public UserQuery createUserQuery() {
    return processEngineConfiguration.getIdmIdentityService().createUserQuery();
  }

  @Override
  public NativeUserQuery createNativeUserQuery() {
    return processEngineConfiguration.getIdmIdentityService().createNativeUserQuery();
  }

  public GroupQuery createGroupQuery() {
    return processEngineConfiguration.getIdmIdentityService().createGroupQuery();
  }

  @Override
  public NativeGroupQuery createNativeGroupQuery() {
    return processEngineConfiguration.getIdmIdentityService().createNativeGroupQuery();
  }
  
  public List<Group> getPotentialStarterGroups(String processDefinitionId) {
    return commandExecutor.execute(new GetPotentialStarterGroupsCmd(processDefinitionId));
  }
  
  public List<User> getPotentialStarterUsers(String processDefinitionId) {
    return commandExecutor.execute(new GetPotentialStarterUsersCmd(processDefinitionId));
  }

  public void createMembership(String userId, String groupId) {
    processEngineConfiguration.getIdmIdentityService().createMembership(userId, groupId);
  }

  public void deleteGroup(String groupId) {
    processEngineConfiguration.getIdmIdentityService().deleteGroup(groupId);
  }

  public void deleteMembership(String userId, String groupId) {
    processEngineConfiguration.getIdmIdentityService().deleteMembership(userId, groupId);
  }

  public boolean checkPassword(String userId, String password) {
    return processEngineConfiguration.getIdmIdentityService().checkPassword(userId, password);
  }

  public void deleteUser(String userId) {
    processEngineConfiguration.getIdmIdentityService().deleteUser(userId);
  }

  public void setUserPicture(String userId, Picture picture) {
    processEngineConfiguration.getIdmIdentityService().setUserPicture(userId, picture);
  }

  public Picture getUserPicture(String userId) {
    return processEngineConfiguration.getIdmIdentityService().getUserPicture(userId);
  }

  public void setAuthenticatedUserId(String authenticatedUserId) {
    Authentication.setAuthenticatedUserId(authenticatedUserId);
  }

  public String getUserInfo(String userId, String key) {
    return processEngineConfiguration.getIdmIdentityService().getUserInfo(userId, key);
  }

  public List<String> getUserInfoKeys(String userId) {
    return processEngineConfiguration.getIdmIdentityService().getUserInfoKeys(userId);
  }

  public void setUserInfo(String userId, String key, String value) {
    processEngineConfiguration.getIdmIdentityService().setUserInfo(userId, key, value);
  }

  public void deleteUserInfo(String userId, String key) {
    processEngineConfiguration.getIdmIdentityService().deleteUserInfo(userId, key);
  }
}
