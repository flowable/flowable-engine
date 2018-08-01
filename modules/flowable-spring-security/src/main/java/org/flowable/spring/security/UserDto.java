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

import java.io.Serializable;

import org.flowable.idm.api.User;

/**
 * An immutable serializable implementation of {@link User}. This implementation allows mutation only for the password,
 * in order for it to be removed by Spring Security when the credentials are erased.
 * @author Filip Hrisafov
 */
public class UserDto implements User, Serializable {

    private static final long serialVersionUID = 1L;

    protected final String id;
    protected String password;
    protected final String firstName;
    protected final String lastName;
    protected final String displayName;
    protected final String email;
    protected final String tenantId;

    protected UserDto(String id, String password, String firstName, String lastName, String displayName, String email, String tenantId) {
        this.id = id;
        this.password = password;
        this.firstName = firstName;
        this.lastName = lastName;
        this.displayName = displayName;
        this.email = email;
        this.tenantId = tenantId;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        // Not supported
    }

    @Override
    public String getFirstName() {
        return firstName;
    }

    @Override
    public void setFirstName(String firstName) {
        // Not supported
    }

    @Override
    public String getLastName() {
        return lastName;
    }

    @Override
    public void setLastName(String lastName) {
        // Not supported
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }

    @Override
    public void setDisplayName(String displayName) {
        // Not supported
    }

    @Override
    public String getEmail() {
        return email;
    }

    @Override
    public void setEmail(String email) {
        // Not supported
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public String getTenantId() {
        return tenantId;
    }

    @Override
    public void setTenantId(String tenantId) {
        // Not supported
    }

    @Override
    public boolean isPictureSet() {
        return false;
    }

    public static UserDto create(User user) {
        return new UserDto(user.getId(), user.getPassword(), user.getFirstName(), user.getLastName(), user.getDisplayName(), user.getEmail(),
            user.getTenantId());
    }
}
