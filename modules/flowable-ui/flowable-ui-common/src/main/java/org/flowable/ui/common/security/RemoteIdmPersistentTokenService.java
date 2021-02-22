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

import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.idm.api.Token;
import org.flowable.ui.common.model.RemoteToken;
import org.flowable.ui.common.service.idm.RemoteIdmService;
import org.springframework.security.web.authentication.rememberme.RememberMeAuthenticationException;

/**
 * @author Filip Hrisafov
 */
public class RemoteIdmPersistentTokenService extends BasePersistentTokenService {

    protected final RemoteIdmService remoteIdmService;

    public RemoteIdmPersistentTokenService(RemoteIdmService remoteIdmService) {
        this.remoteIdmService = remoteIdmService;
    }

    @Override
    protected Token loadToken(String tokenId) {
        RemoteToken token = remoteIdmService.getToken(tokenId);
        if (token != null) {
            return token;
        } else {
            throw new FlowableObjectNotFoundException("Token with id '" + tokenId + "' not found.");
        }
    }

    @Override
    public Token createToken(String userId, String remoteAddress, String userAgent) {
        // Throwing an exception will cause a redirect for the login
        throw new RememberMeAuthenticationException("Creating remote tokens is not allowed");
    }
}
