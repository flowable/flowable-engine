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

package org.flowable.idm.engine.impl;

import java.util.Date;
import java.util.List;

import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.impl.AbstractQuery;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.interceptor.CommandExecutor;
import org.flowable.idm.api.Token;
import org.flowable.idm.api.TokenQuery;
import org.flowable.idm.api.TokenQueryProperty;
import org.flowable.idm.engine.impl.util.CommandContextUtil;

/**
 * @author Tijs Rademakers
 */
public class TokenQueryImpl extends AbstractQuery<TokenQuery, Token> implements TokenQuery {

    private static final long serialVersionUID = 1L;
    protected String id;
    protected List<String> ids;
    protected String tokenValue;
    protected Date tokenDate;
    protected Date tokenDateBefore;
    protected Date tokenDateAfter;
    protected String ipAddress;
    protected String ipAddressLike;
    protected String userAgent;
    protected String userAgentLike;
    protected String userId;
    protected String userIdLike;
    protected String tokenData;
    protected String tokenDataLike;

    public TokenQueryImpl() {
    }

    public TokenQueryImpl(CommandContext commandContext) {
        super(commandContext);
    }

    public TokenQueryImpl(CommandExecutor commandExecutor) {
        super(commandExecutor);
    }

    @Override
    public TokenQuery tokenId(String id) {
        if (id == null) {
            throw new FlowableIllegalArgumentException("Provided id is null");
        }
        this.id = id;
        return this;
    }

    @Override
    public TokenQuery tokenIds(List<String> ids) {
        if (ids == null) {
            throw new FlowableIllegalArgumentException("Provided ids is null");
        }
        this.ids = ids;
        return this;
    }

    @Override
    public TokenQuery tokenValue(String tokenValue) {
        if (tokenValue == null) {
            throw new FlowableIllegalArgumentException("Provided token value is null");
        }
        this.tokenValue = tokenValue;
        return this;
    }

    @Override
    public TokenQuery tokenDate(Date tokenDate) {
        if (tokenDate == null) {
            throw new FlowableIllegalArgumentException("Provided token date is null");
        }
        this.tokenDate = tokenDate;
        return this;
    }

    @Override
    public TokenQuery tokenDateBefore(Date tokenDateBefore) {
        if (tokenDateBefore == null) {
            throw new FlowableIllegalArgumentException("Provided tokenDateBefore is null");
        }
        this.tokenDateBefore = tokenDateBefore;
        return this;
    }

    @Override
    public TokenQuery tokenDateAfter(Date tokenDateAfter) {
        if (tokenDateAfter == null) {
            throw new FlowableIllegalArgumentException("Provided tokenDateAfter is null");
        }
        this.tokenDateAfter = tokenDateAfter;
        return this;
    }

    @Override
    public TokenQuery ipAddress(String ipAddress) {
        if (ipAddress == null) {
            throw new FlowableIllegalArgumentException("Provided ip address is null");
        }
        this.ipAddress = ipAddress;
        return this;
    }

    @Override
    public TokenQuery ipAddressLike(String ipAddressLike) {
        if (ipAddressLike == null) {
            throw new FlowableIllegalArgumentException("Provided ipAddressLike is null");
        }
        this.ipAddressLike = ipAddressLike;
        return this;
    }

    @Override
    public TokenQuery userAgent(String userAgent) {
        if (userAgent == null) {
            throw new FlowableIllegalArgumentException("Provided user agent is null");
        }
        this.userAgent = userAgent;
        return this;
    }

    @Override
    public TokenQuery userAgentLike(String userAgentLike) {
        if (userAgentLike == null) {
            throw new FlowableIllegalArgumentException("Provided userAgentLike is null");
        }
        this.userAgentLike = userAgentLike;
        return this;
    }

    @Override
    public TokenQuery userId(String userId) {
        if (userId == null) {
            throw new FlowableIllegalArgumentException("Provided user id is null");
        }
        this.userId = userId;
        return this;
    }

    @Override
    public TokenQuery userIdLike(String userIdLike) {
        if (userIdLike == null) {
            throw new FlowableIllegalArgumentException("Provided userIdLike is null");
        }
        this.userIdLike = userIdLike;
        return this;
    }

    @Override
    public TokenQuery tokenData(String tokenData) {
        if (tokenData == null) {
            throw new FlowableIllegalArgumentException("Provided token data is null");
        }
        this.tokenData = tokenData;
        return this;
    }

    @Override
    public TokenQuery tokenDataLike(String tokenDataLike) {
        if (tokenDataLike == null) {
            throw new FlowableIllegalArgumentException("Provided tokenDataLike is null");
        }
        this.tokenDataLike = tokenDataLike;
        return this;
    }

    // sorting //////////////////////////////////////////////////////////

    @Override
    public TokenQuery orderByTokenId() {
        return orderBy(TokenQueryProperty.TOKEN_ID);
    }

    @Override
    public TokenQuery orderByTokenDate() {
        return orderBy(TokenQueryProperty.TOKEN_DATE);
    }

    // results //////////////////////////////////////////////////////////

    @Override
    public long executeCount(CommandContext commandContext) {
        checkQueryOk();
        return CommandContextUtil.getTokenEntityManager(commandContext).findTokenCountByQueryCriteria(this);
    }

    @Override
    public List<Token> executeList(CommandContext commandContext) {
        checkQueryOk();
        return CommandContextUtil.getTokenEntityManager(commandContext).findTokenByQueryCriteria(this);
    }

    // getters //////////////////////////////////////////////////////////

    public String getId() {
        return id;
    }

    public List<String> getIds() {
        return ids;
    }

    public String getTokenValue() {
        return tokenValue;
    }

    public Date getTokenDate() {
        return tokenDate;
    }

    public Date getTokenDateBefore() {
        return tokenDateBefore;
    }

    public Date getTokenDateAfter() {
        return tokenDateAfter;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public String getIpAddressLike() {
        return ipAddressLike;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public String getUserAgentLike() {
        return userAgentLike;
    }

    public String getUserId() {
        return userId;
    }

    public String getUserIdLike() {
        return userIdLike;
    }

    public String getTokenData() {
        return tokenData;
    }

    public String getTokenDataLike() {
        return tokenDataLike;
    }
}
