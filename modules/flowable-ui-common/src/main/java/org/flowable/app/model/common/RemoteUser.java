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
package org.flowable.app.model.common;

import java.util.ArrayList;
import java.util.List;

import org.flowable.idm.api.User;

public class RemoteUser implements User {

    protected String id;
    protected String firstName;
    protected String lastName;
    protected String email;
    protected String fullName;
    protected List<RemoteGroup> groups = new ArrayList<>();
    protected List<String> privileges = new ArrayList<>();

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public List<RemoteGroup> getGroups() {
        return groups;
    }

    public void setGroups(List<RemoteGroup> groups) {
        this.groups = groups;
    }

    public List<String> getPrivileges() {
        return privileges;
    }

    public void setPrivileges(List<String> privileges) {
        this.privileges = privileges;
    }

    public String getPassword() {
        // Not supported
        return null;
    }

    public void setPassword(String string) {
        // Not supported
    }

    public boolean isPictureSet() {
        // Not supported
        return false;
    }

}
