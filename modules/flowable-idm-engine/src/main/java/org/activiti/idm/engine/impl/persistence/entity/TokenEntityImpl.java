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
package org.activiti.idm.engine.impl.persistence.entity;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.activiti.idm.engine.impl.db.HasRevision;

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

  public String getTokenValue() {
    return tokenValue;
  }

  public void setTokenValue(String tokenValue) {
    this.tokenValue = tokenValue;
  }

  public Date getTokenDate() {
    return tokenDate;
  }

  public void setTokenDate(Date tokenDate) {
    this.tokenDate = tokenDate;
  }

  public String getIpAddress() {
    return ipAddress;
  }

  public void setIpAddress(String ipAddress) {
    this.ipAddress = ipAddress;
  }

  public String getUserAgent() {
    return userAgent;
  }

  public void setUserAgent(String userAgent) {
    this.userAgent = userAgent;
  }

  public String getUserId() {
    return userId;
  }

  public void setUserId(String userId) {
    this.userId = userId;
  }

  public String getTokenData() {
    return tokenData;
  }

  public void setTokenData(String tokenData) {
    this.tokenData = tokenData;
  }

  public Object getPersistentState() {
    Map<String, Object> persistentState = new HashMap<String, Object>();
    persistentState.put("tokenValue", tokenValue);
    persistentState.put("tokenDate", tokenDate);
    persistentState.put("ipAddress", ipAddress);
    persistentState.put("userAgent", userAgent);
    persistentState.put("userId", userId);
    persistentState.put("tokenData", tokenData);
    
    return persistentState;
  }
  
  public int getRevisionNext() {
    return revision + 1;
  }

  // common methods //////////////////////////////////////////////////////////

  @Override
  public String toString() {
    return "TokenEntity[tokenValue=" + tokenValue + ", userId=" + userId + "]";
  }

}
