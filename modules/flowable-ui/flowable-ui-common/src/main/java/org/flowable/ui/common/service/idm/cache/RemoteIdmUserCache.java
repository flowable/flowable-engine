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
package org.flowable.ui.common.service.idm.cache;

import java.util.ArrayList;
import java.util.Collection;

import org.flowable.idm.api.User;
import org.flowable.ui.common.properties.FlowableCommonAppProperties;
import org.flowable.ui.common.service.idm.RemoteIdmService;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

/**
 * Cache containing User objects to prevent too much DB-traffic (users exist separately from the Flowable tables, they need to be fetched afterward one by one to join with those entities).
 * <p>
 * TODO: This could probably be made more efficient with bulk getting. The Google cache impl allows this: override loadAll and use getAll() to fetch multiple entities.
 *
 * @author Frederik Heremans
 * @author Joram Barrez
 * @author Filip Hrisafov
 */
public class RemoteIdmUserCache extends BaseUserCache {

    protected final RemoteIdmService remoteIdmService;

    public RemoteIdmUserCache(FlowableCommonAppProperties properties, RemoteIdmService remoteIdmService) {
        super(properties);
        this.remoteIdmService = remoteIdmService;
    }

    @Override
    protected CachedUser loadUser(String userId) {
        User user = remoteIdmService.getUser(userId);
        if (user == null) {
            throw new UsernameNotFoundException("User " + userId + " was not found in the database");
        }

        Collection<GrantedAuthority> grantedAuthorities = new ArrayList<>();

        return new CachedUser(user, grantedAuthorities);
    }
}
