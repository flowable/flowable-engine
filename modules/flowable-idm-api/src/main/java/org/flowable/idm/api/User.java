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
package org.flowable.idm.api;

import java.io.Serializable;

/**
 * Represents a user, used in {@link IdmIdentityService}.
 *
 * @author Tom Baeyens
 */
public interface User extends Serializable {

    String getId();

    void setId(String id);

    String getFirstName();

    void setFirstName(String firstName);

    void setLastName(String lastName);

    String getLastName();
    
    void setDisplayName(String displayName);

    String getDisplayName();

    void setEmail(String email);

    String getEmail();

    String getPassword();

    void setPassword(String string);

    String getTenantId();

    void setTenantId(String tenantId);

    boolean isPictureSet();
}
