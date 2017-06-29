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
package org.flowable.app.service.idm;

import java.util.List;

import org.flowable.app.model.common.RemoteGroup;
import org.flowable.app.model.common.RemoteToken;
import org.flowable.app.model.common.RemoteUser;

public interface RemoteIdmService {

    RemoteUser authenticateUser(String username, String password);

    RemoteToken getToken(String tokenValue);

    RemoteUser getUser(String userId);

    List<RemoteUser> findUsersByNameFilter(String filter);
    
    List<RemoteUser> findUsersByGroup(String groupId);
    
    RemoteGroup getGroup(String groupId);

    List<RemoteGroup> findGroupsByNameFilter(String filter);

}
