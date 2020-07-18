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
package org.flowable.ui.common.security;

import java.util.Collection;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * A {@link UserDetails} implementation that exposes the {@link org.flowable.idm.api.User} object the logged in user represents.
 * 
 * @author Frederik Heremans
 * @author Joram Barrez
 */
public class FlowableAppUser extends User {

    private static final long serialVersionUID = 1L;

    protected org.flowable.idm.api.User userObject;

    /**
     * The userId needs to be passed explicitly. It can be the email, but also the external id when eg LDAP is being used.
     */
    public FlowableAppUser(org.flowable.idm.api.User user, String userId, Collection<? extends GrantedAuthority> authorities) {
        super(userId, user.getPassword() != null ? user.getPassword() : "", authorities); // password needs to be non null
        this.userObject = user;
    }

    public org.flowable.idm.api.User getUserObject() {
        return userObject;
    }
}
