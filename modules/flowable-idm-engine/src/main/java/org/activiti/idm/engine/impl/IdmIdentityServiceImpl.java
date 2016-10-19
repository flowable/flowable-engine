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
package org.activiti.idm.engine.impl;

import java.util.List;

import org.activiti.idm.api.Group;
import org.activiti.idm.api.GroupQuery;
import org.activiti.idm.api.IdmIdentityService;
import org.activiti.idm.api.NativeGroupQuery;
import org.activiti.idm.api.NativeTokenQuery;
import org.activiti.idm.api.NativeUserQuery;
import org.activiti.idm.api.Picture;
import org.activiti.idm.api.Token;
import org.activiti.idm.api.TokenQuery;
import org.activiti.idm.api.User;
import org.activiti.idm.api.UserQuery;
import org.activiti.idm.engine.impl.cmd.CheckPassword;
import org.activiti.idm.engine.impl.cmd.CreateGroupCmd;
import org.activiti.idm.engine.impl.cmd.CreateGroupQueryCmd;
import org.activiti.idm.engine.impl.cmd.CreateMembershipCmd;
import org.activiti.idm.engine.impl.cmd.CreateTokenCmd;
import org.activiti.idm.engine.impl.cmd.CreateTokenQueryCmd;
import org.activiti.idm.engine.impl.cmd.CreateUserCmd;
import org.activiti.idm.engine.impl.cmd.CreateUserQueryCmd;
import org.activiti.idm.engine.impl.cmd.DeleteGroupCmd;
import org.activiti.idm.engine.impl.cmd.DeleteMembershipCmd;
import org.activiti.idm.engine.impl.cmd.DeleteTokenCmd;
import org.activiti.idm.engine.impl.cmd.DeleteUserCmd;
import org.activiti.idm.engine.impl.cmd.DeleteUserInfoCmd;
import org.activiti.idm.engine.impl.cmd.GetUserInfoCmd;
import org.activiti.idm.engine.impl.cmd.GetUserInfoKeysCmd;
import org.activiti.idm.engine.impl.cmd.GetUserPictureCmd;
import org.activiti.idm.engine.impl.cmd.SaveGroupCmd;
import org.activiti.idm.engine.impl.cmd.SaveTokenCmd;
import org.activiti.idm.engine.impl.cmd.SaveUserCmd;
import org.activiti.idm.engine.impl.cmd.SetUserInfoCmd;
import org.activiti.idm.engine.impl.cmd.SetUserPictureCmd;
import org.activiti.idm.engine.impl.persistence.entity.IdentityInfoEntity;

/**
 * @author Tijs Rademakers
 */
public class IdmIdentityServiceImpl extends ServiceImpl implements IdmIdentityService {

  public Group newGroup(String groupId) {
    return commandExecutor.execute(new CreateGroupCmd(groupId));
  }

  public User newUser(String userId) {
    return commandExecutor.execute(new CreateUserCmd(userId));
  }

  public void saveGroup(Group group) {
    commandExecutor.execute(new SaveGroupCmd(group));
  }

  public void saveUser(User user) {
    commandExecutor.execute(new SaveUserCmd(user));
  }

  public UserQuery createUserQuery() {
    return commandExecutor.execute(new CreateUserQueryCmd());
  }

  @Override
  public NativeUserQuery createNativeUserQuery() {
    return new NativeUserQueryImpl(commandExecutor);
  }

  public GroupQuery createGroupQuery() {
    return commandExecutor.execute(new CreateGroupQueryCmd());
  }

  @Override
  public NativeGroupQuery createNativeGroupQuery() {
    return new NativeGroupQueryImpl(commandExecutor);
  }

  public void createMembership(String userId, String groupId) {
    commandExecutor.execute(new CreateMembershipCmd(userId, groupId));
  }

  public void deleteGroup(String groupId) {
    commandExecutor.execute(new DeleteGroupCmd(groupId));
  }

  public void deleteMembership(String userId, String groupId) {
    commandExecutor.execute(new DeleteMembershipCmd(userId, groupId));
  }

  public boolean checkPassword(String userId, String password) {
    return commandExecutor.execute(new CheckPassword(userId, password));
  }

  public void deleteUser(String userId) {
    commandExecutor.execute(new DeleteUserCmd(userId));
  }
  
  public Token newToken(String tokenId) {
    return commandExecutor.execute(new CreateTokenCmd(tokenId));
  }
  
  public void saveToken(Token token) {
    commandExecutor.execute(new SaveTokenCmd(token));
  }
  
  public void deleteToken(String tokenId) {
    commandExecutor.execute(new DeleteTokenCmd(tokenId));
  }
  
  public TokenQuery createTokenQuery() {
    return commandExecutor.execute(new CreateTokenQueryCmd());
  }

  public NativeTokenQuery createNativeTokenQuery() {
    return new NativeTokenQueryImpl(commandExecutor);
  }

  public void setUserPicture(String userId, Picture picture) {
    commandExecutor.execute(new SetUserPictureCmd(userId, picture));
  }

  public Picture getUserPicture(String userId) {
    return commandExecutor.execute(new GetUserPictureCmd(userId));
  }

  public String getUserInfo(String userId, String key) {
    return commandExecutor.execute(new GetUserInfoCmd(userId, key));
  }

  public List<String> getUserInfoKeys(String userId) {
    return commandExecutor.execute(new GetUserInfoKeysCmd(userId, IdentityInfoEntity.TYPE_USERINFO));
  }

  public void setUserInfo(String userId, String key, String value) {
    commandExecutor.execute(new SetUserInfoCmd(userId, key, value));
  }

  public void deleteUserInfo(String userId, String key) {
    commandExecutor.execute(new DeleteUserInfoCmd(userId, key));
  }
}
