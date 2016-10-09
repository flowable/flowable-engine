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
package org.activiti.engine.impl;

import java.util.List;

import org.activiti.engine.IdentityService;
import org.activiti.engine.impl.identity.Authentication;
import org.activiti.idm.api.Group;
import org.activiti.idm.api.GroupQuery;
import org.activiti.idm.api.NativeGroupQuery;
import org.activiti.idm.api.NativeUserQuery;
import org.activiti.idm.api.Picture;
import org.activiti.idm.api.User;
import org.activiti.idm.api.UserQuery;

/**
 * @author Tom Baeyens
 */
public class IdentityServiceImpl extends ServiceImpl implements IdentityService {

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
