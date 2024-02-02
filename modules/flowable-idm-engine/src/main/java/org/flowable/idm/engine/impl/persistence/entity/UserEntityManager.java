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

import org.flowable.common.engine.impl.persistence.entity.EntityManager;
import org.flowable.idm.api.PasswordEncoder;
import org.flowable.idm.api.PasswordSalt;
import org.flowable.idm.api.Picture;
import org.flowable.idm.api.User;
import org.flowable.idm.api.UserQuery;
import org.flowable.idm.engine.impl.UserQueryImpl;

/**
 * @author Joram Barrez
 */
public interface UserEntityManager extends EntityManager<UserEntity> {

    User createNewUser(String userId);

    void updateUser(User updatedUser);

    List<User> findUserByQueryCriteria(UserQueryImpl query);

    long findUserCountByQueryCriteria(UserQueryImpl query);

    UserQuery createNewUserQuery();

    Boolean checkPassword(String userId, String password, PasswordEncoder passwordEncoder, PasswordSalt passwordSalt);

    List<User> findUsersByNativeQuery(Map<String, Object> parameterMap);

    long findUserCountByNativeQuery(Map<String, Object> parameterMap);

    boolean isNewUser(User user);

    Picture getUserPicture(User user);

    void setUserPicture(User user, Picture picture);

    void deletePicture(User user);

    List<User> findUsersByPrivilegeId(String privilegeId);

}
