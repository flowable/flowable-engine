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

package org.flowable.idm.engine.impl.persistence.entity;

import java.util.List;
import java.util.Map;

import org.flowable.engine.common.impl.Page;
import org.flowable.engine.common.impl.persistence.entity.data.DataManager;
import org.flowable.idm.api.Picture;
import org.flowable.idm.api.User;
import org.flowable.idm.api.UserQuery;
import org.flowable.idm.engine.IdmEngineConfiguration;
import org.flowable.idm.engine.impl.UserQueryImpl;
import org.flowable.idm.engine.impl.persistence.entity.data.UserDataManager;

/**
 * @author Tijs Rademakers
 * @author Joram Barrez
 */
public class UserEntityManagerImpl extends AbstractEntityManager<UserEntity> implements UserEntityManager {
  
  protected UserDataManager userDataManager;
  
  public UserEntityManagerImpl(IdmEngineConfiguration idmEngineConfiguration, UserDataManager userDataManager) {
    super(idmEngineConfiguration);
    this.userDataManager = userDataManager;
  }
  
  @Override
  protected DataManager<UserEntity> getDataManager() {
    return userDataManager;
  }
  
  @Override
  public UserEntity findById(String entityId) {
    return userDataManager.findById(entityId);
  }

  public User createNewUser(String userId) {
    UserEntity userEntity = create();
    userEntity.setId(userId);
    userEntity.setRevision(0); // needed as users can be transient
    return userEntity;
  }

  public void updateUser(User updatedUser) {
    super.update((UserEntity) updatedUser);
  }

  public void delete(UserEntity userEntity) {
    super.delete(userEntity);
    deletePicture(userEntity);
  }
  
  @Override
  public void deletePicture(User user) {
    UserEntity userEntity = (UserEntity) user;
    if (userEntity.getPictureByteArrayRef() != null) {
      userEntity.getPictureByteArrayRef().delete();
    }
  }

  public void delete(String userId) {
    UserEntity user = findById(userId);
    if (user != null) {
      List<IdentityInfoEntity> identityInfos = getIdentityInfoEntityManager().findIdentityInfoByUserId(userId);
      for (IdentityInfoEntity identityInfo : identityInfos) {
        getIdentityInfoEntityManager().delete(identityInfo);
      }
      getMembershipEntityManager().deleteMembershipByUserId(userId);
      delete(user);
    }
  }

  public List<User> findUserByQueryCriteria(UserQueryImpl query, Page page) {
    return userDataManager.findUserByQueryCriteria(query, page);
  }

  public long findUserCountByQueryCriteria(UserQueryImpl query) {
    return userDataManager.findUserCountByQueryCriteria(query);
  }

  public UserQuery createNewUserQuery() {
    return new UserQueryImpl(getCommandExecutor());
  }

  public Boolean checkPassword(String userId, String password) {
    User user = null;
    
    if (userId != null) {
      user = findById(userId);
    }

    return (user != null) && (password != null) && (password.equals(user.getPassword()));
  }

  public List<User> findUsersByNativeQuery(Map<String, Object> parameterMap, int firstResult, int maxResults) {
    return userDataManager.findUsersByNativeQuery(parameterMap, firstResult, maxResults);
  }

  public long findUserCountByNativeQuery(Map<String, Object> parameterMap) {
    return userDataManager.findUserCountByNativeQuery(parameterMap);
  }

  @Override
  public boolean isNewUser(User user) {
    return ((UserEntity) user).getRevision() == 0;
  }

  @Override
  public Picture getUserPicture(User user) {
    UserEntity userEntity = (UserEntity) user;
    return userEntity.getPicture();
  }

  @Override
  public void setUserPicture(User user, Picture picture) {
    UserEntity userEntity = (UserEntity) user;
    userEntity.setPicture(picture);
    userDataManager.update(userEntity);
  }
  
  @Override
  public List<User> findUsersByPrivilegeId(String name) {
    return userDataManager.findUsersByPrivilegeId(name);
  }

  public UserDataManager getUserDataManager() {
    return userDataManager;
  }

  public void setUserDataManager(UserDataManager userDataManager) {
    this.userDataManager = userDataManager;
  }
  
}
