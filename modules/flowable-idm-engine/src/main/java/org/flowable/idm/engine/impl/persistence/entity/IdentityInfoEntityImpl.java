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

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.flowable.common.engine.impl.persistence.entity.AbstractEntity;

/**
 * @author Tom Baeyens
 */
public class IdentityInfoEntityImpl extends AbstractEntity implements IdentityInfoEntity, Serializable {

    private static final long serialVersionUID = 1L;

    protected String type;
    protected String userId;
    protected String key;
    protected String value;
    protected String password;
    protected byte[] passwordBytes;
    protected String parentId;
    protected Map<String, String> details;

    public IdentityInfoEntityImpl() {

    }

    @Override
    public Object getPersistentState() {
        Map<String, Object> persistentState = new HashMap<>();
        persistentState.put("value", value);
        persistentState.put("password", passwordBytes);
        return persistentState;
    }

    @Override
    public String getType() {
        return type;
    }

    @Override
    public void setType(String type) {
        this.type = type;
    }

    @Override
    public String getUserId() {
        return userId;
    }

    @Override
    public void setUserId(String userId) {
        this.userId = userId;
    }

    @Override
    public String getKey() {
        return key;
    }

    @Override
    public void setKey(String key) {
        this.key = key;
    }

    @Override
    public String getValue() {
        return value;
    }

    @Override
    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public byte[] getPasswordBytes() {
        return passwordBytes;
    }

    @Override
    public void setPasswordBytes(byte[] passwordBytes) {
        this.passwordBytes = passwordBytes;
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
    public String getName() {
        return key;
    }

    @Override
    public String getUsername() {
        return value;
    }

    @Override
    public String getParentId() {
        return parentId;
    }

    @Override
    public void setParentId(String parentId) {
        this.parentId = parentId;
    }

    @Override
    public Map<String, String> getDetails() {
        return details;
    }

    @Override
    public void setDetails(Map<String, String> details) {
        this.details = details;
    }
}
