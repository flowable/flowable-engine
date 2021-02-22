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

import org.flowable.idm.api.PasswordEncoder;
import org.flowable.idm.api.PasswordSalt;
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
public class UserEntityManagerImpl
    extends AbstractIdmEngineEntityManager<UserEntity, UserDataManager>
    implements UserEntityManager {

    public UserEntityManagerImpl(IdmEngineConfiguration idmEngineConfiguration, UserDataManager userDataManager) {
        super(idmEngineConfiguration, userDataManager);
    }

    @Override
    public UserEntity findById(String entityId) {
        return dataManager.findById(entityId);
    }

    @Override
    public User createNewUser(String userId) {
        UserEntity userEntity = create();
        userEntity.setId(userId);
        userEntity.setRevision(0); // needed as users can be transient
        return userEntity;
    }

    @Override
    public void updateUser(User updatedUser) {
        super.update((UserEntity) updatedUser);
    }

    @Override
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

    @Override
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

    @Override
    public List<User> findUserByQueryCriteria(UserQueryImpl query) {
        return dataManager.findUserByQueryCriteria(query);
    }

    @Override
    public long findUserCountByQueryCriteria(UserQueryImpl query) {
        return dataManager.findUserCountByQueryCriteria(query);
    }

    @Override
    public UserQuery createNewUserQuery() {
        return new UserQueryImpl(getCommandExecutor());
    }

    @Override
    public Boolean checkPassword(String userId, String password, PasswordEncoder passwordEncoder, PasswordSalt salt) {
        User user = null;

        if (userId != null) {
            user = findById(userId);
        }

        return (user != null) && (password != null) && passwordEncoder.isMatches(password, user.getPassword(), salt);
    }

    @Override
    public List<User> findUsersByNativeQuery(Map<String, Object> parameterMap) {
        return dataManager.findUsersByNativeQuery(parameterMap);
    }

    @Override
    public long findUserCountByNativeQuery(Map<String, Object> parameterMap) {
        return dataManager.findUserCountByNativeQuery(parameterMap);
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
        dataManager.update(userEntity);
    }

    @Override
    public List<User> findUsersByPrivilegeId(String name) {
        return dataManager.findUsersByPrivilegeId(name);
    }

    public UserDataManager getUserDataManager() {
        return dataManager;
    }

    public void setUserDataManager(UserDataManager userDataManager) {
        this.dataManager = userDataManager;
    }

    protected IdentityInfoEntityManager getIdentityInfoEntityManager() {
        return engineConfiguration.getIdentityInfoEntityManager();
    }

    protected MembershipEntityManager getMembershipEntityManager() {
        return engineConfiguration.getMembershipEntityManager();
    }

}
