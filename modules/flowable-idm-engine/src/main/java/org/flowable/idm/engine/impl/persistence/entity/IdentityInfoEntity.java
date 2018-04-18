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

import java.util.Map;

import org.flowable.common.engine.impl.db.HasRevision;
import org.flowable.common.engine.impl.persistence.entity.Entity;

/**
 * @author Tom Baeyens
 */
public interface IdentityInfoEntity extends Entity, HasRevision {

    String TYPE_USERINFO = "userinfo";

    String getType();

    void setType(String type);

    String getUserId();

    void setUserId(String userId);

    String getKey();

    void setKey(String key);

    String getValue();

    void setValue(String value);

    byte[] getPasswordBytes();

    void setPasswordBytes(byte[] passwordBytes);

    String getPassword();

    void setPassword(String password);

    String getName();

    String getUsername();

    String getParentId();

    void setParentId(String parentId);

    Map<String, String> getDetails();

    void setDetails(Map<String, String> details);

}
