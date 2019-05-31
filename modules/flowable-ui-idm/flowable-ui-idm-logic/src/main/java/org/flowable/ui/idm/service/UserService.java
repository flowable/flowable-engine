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
package org.flowable.ui.idm.service;

import java.util.List;

import org.flowable.idm.api.User;
import org.flowable.ui.idm.model.UserInformation;

/**
 * @author Joram Barrez
 */
public interface UserService {

    List<User> getUsers(String filter, String sort, Integer start);

    long getUserCount(String filter, String sort, Integer start, String groupId);

    void updateUserDetails(String userId, String firstName, String lastName, String email);
    
    void updateUserDetails(String userId, String firstName, String lastName, String email, String tenantId);

    void bulkUpdatePassword(List<String> userIds, String newPassword);

    void deleteUser(String userId);

    User createNewUser(String id, String firstName, String lastName, String email, String password);
    
    User createNewUser(String id, String firstName, String lastName, String email, String password, String tenantId);

    UserInformation getUserInformation(String userId);

}
