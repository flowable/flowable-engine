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
package org.flowable.spring.security;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.flowable.idm.api.Group;
import org.flowable.idm.api.User;
import org.springframework.security.core.GrantedAuthority;

/**
 * A flowable implementation of {@link org.springframework.security.core.userdetails.UserDetails}.
 *
 * @author Filip Hrisafov
 */
public class FlowableUser extends org.springframework.security.core.userdetails.User implements FlowableUserDetails {

    private static final long serialVersionUID = 1L;

    protected final User user;

    protected final List<Group> groups;

    public FlowableUser(User user, boolean active, List<? extends Group> groups, Collection<? extends GrantedAuthority> authorities) {
        super(user.getId(), user.getPassword() == null ? "" : user.getPassword(), active, active, active, active, authorities);
        this.user = user;
        this.groups = Collections.unmodifiableList(groups);
    }

    public FlowableUser(User user, String username, boolean enabled,
        List<? extends Group> groups, Collection<? extends GrantedAuthority> authorities) {
        super(username, user.getPassword() == null ? "" : user.getPassword(), enabled, true, true, true, authorities);
        this.user = user;
        this.groups = Collections.unmodifiableList(groups);
    }

    @Override
    public User getUser() {
        return user;
    }

    @Override
    public List<Group> getGroups() {
        return groups;
    }

    @Override
    public void eraseCredentials() {
        super.eraseCredentials();
        user.setPassword(null);
    }
}
