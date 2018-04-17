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
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.flowable.common.engine.impl.db.HasRevision;
import org.flowable.common.engine.impl.persistence.entity.AbstractEntity;

/**
 * @author Tijs Rademakers
 */
public class TokenEntityImpl extends AbstractEntity implements TokenEntity, Serializable, HasRevision {

    private static final long serialVersionUID = 1L;

    protected String tokenValue;
    protected Date tokenDate;
    protected String ipAddress;
    protected String userAgent;
    protected String userId;
    protected String tokenData;

    @Override
    public String getTokenValue() {
        return tokenValue;
    }

    @Override
    public void setTokenValue(String tokenValue) {
        this.tokenValue = tokenValue;
    }

    @Override
    public Date getTokenDate() {
        return tokenDate;
    }

    @Override
    public void setTokenDate(Date tokenDate) {
        this.tokenDate = tokenDate;
    }

    @Override
    public String getIpAddress() {
        return ipAddress;
    }

    @Override
    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    @Override
    public String getUserAgent() {
        return userAgent;
    }

    @Override
    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
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
    public String getTokenData() {
        return tokenData;
    }

    @Override
    public void setTokenData(String tokenData) {
        this.tokenData = tokenData;
    }

    @Override
    public Object getPersistentState() {
        Map<String, Object> persistentState = new HashMap<>();
        persistentState.put("tokenValue", tokenValue);
        persistentState.put("tokenDate", tokenDate);
        persistentState.put("ipAddress", ipAddress);
        persistentState.put("userAgent", userAgent);
        persistentState.put("userId", userId);
        persistentState.put("tokenData", tokenData);

        return persistentState;
    }

    // common methods //////////////////////////////////////////////////////////

    @Override
    public String toString() {
        return "TokenEntity[tokenValue=" + tokenValue + ", userId=" + userId + "]";
    }

}
