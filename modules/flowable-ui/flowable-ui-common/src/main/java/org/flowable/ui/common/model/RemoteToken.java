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
package org.flowable.ui.common.model;

import java.util.Date;

import org.flowable.idm.api.Token;

public class RemoteToken implements Token {

    protected String id;
    protected String value;
    protected String userId;
    protected Date date;


    @Override
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
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
    public String getTokenValue() {
        return getValue();
    }

    @Override
    public void setTokenValue(String tokenValue) {
        setValue(tokenValue);
    }

    @Override
    public Date getTokenDate() {
        return date;
    }

    @Override
    public void setTokenDate(Date tokenDate) {
        this.date = tokenDate;
    }

    @Override
    public String getIpAddress() {
        return null;
    }

    @Override
    public void setIpAddress(String ipAddress) {
        throw new UnsupportedOperationException("Setting IP address is not supported on a remote token");
    }

    @Override
    public String getUserAgent() {
        return null;
    }

    @Override
    public void setUserAgent(String userAgent) {
        throw new UnsupportedOperationException("Setting User Agent is not supported on a remote token");
    }

    @Override
    public String getTokenData() {
        return null;
    }

    @Override
    public void setTokenData(String tokenData) {
        throw new UnsupportedOperationException("Setting token data is not supported on a remote token");
    }
}
