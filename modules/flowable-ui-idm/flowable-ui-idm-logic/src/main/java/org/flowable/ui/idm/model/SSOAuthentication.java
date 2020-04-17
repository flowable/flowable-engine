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
package org.flowable.ui.idm.model;

import java.util.Collection;
import java.util.Collections;

import org.flowable.idm.api.User;
import org.flowable.ui.common.security.FlowableAppUser;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

public class SSOAuthentication implements Authentication {

    private FlowableAppUser user;

    public SSOAuthentication(User user) {
        this.user = new FlowableAppUser(user, user.getId(), Collections.emptyList());
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.emptyList();
    }
    @Override
    public String getCredentials() {
        return user.getPassword();
    }
    @Override
    public User getDetails() {
        return user.getUserObject();
    }
    @Override
    public FlowableAppUser getPrincipal() {
        return user;
    }
    @Override
    public boolean isAuthenticated() {
        return true;
    }
    @Override
    public void setAuthenticated(boolean b) throws IllegalArgumentException {

    }
    @Override
    public boolean equals(Object o) {
        return o instanceof SSOAuthentication && user.getUserObject().getId().equals(((SSOAuthentication) o).user.getUserObject().getId());
    }
    @Override
    public String toString() {
        return user.toString();
    }
    @Override
    public int hashCode() {
        return user.getUserObject().hashCode();
    }
    @Override
    public String getName() {
        return user.getUsername();
    }
}
