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

import java.util.Date;

import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.idm.api.IdmIdentityService;
import org.flowable.idm.api.Token;
import org.springframework.dao.DataAccessException;

/**
 * @author Filip Hrisafov
 */
public class IdmEnginePersistentTokenService extends BasePersistentTokenService {

    protected final IdmIdentityService idmIdentityService;

    public IdmEnginePersistentTokenService(IdmIdentityService idmIdentityService) {
        this.idmIdentityService = idmIdentityService;
    }

    @Override
    protected Token loadToken(String tokenId) {
        Token token = idmIdentityService.createTokenQuery().tokenId(tokenId).singleResult();
        if (token != null) {
            return token;
        } else {
            throw new FlowableObjectNotFoundException("Token with id '" + tokenId + "' not found.");
        }
    }

    @Override
    public void delete(Token token) {
        super.delete(token);
        idmIdentityService.deleteToken(token.getId());
    }

    @Override
    public Token createToken(String userId, String remoteAddress, String userAgent) {

        Token token = idmIdentityService.newToken(generateSeriesData());
        token.setTokenValue(generateTokenData());
        token.setTokenDate(new Date());
        token.setIpAddress(remoteAddress);
        token.setUserAgent(userAgent);
        token.setUserId(userId);

        try {
            idmIdentityService.saveToken(token);
            return token;
        } catch (DataAccessException e) {
            logger.error("Failed to save persistent token ", e);
            return token;
        }
    }
}
