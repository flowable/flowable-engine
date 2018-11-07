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

import java.util.List;
import java.util.Map;

import org.flowable.common.engine.impl.persistence.entity.data.DataManager;
import org.flowable.idm.api.Token;
import org.flowable.idm.api.TokenQuery;
import org.flowable.idm.engine.IdmEngineConfiguration;
import org.flowable.idm.engine.impl.TokenQueryImpl;
import org.flowable.idm.engine.impl.persistence.entity.data.TokenDataManager;

/**
 * @author Tijs Rademakers
 */
public class TokenEntityManagerImpl extends AbstractEntityManager<TokenEntity> implements TokenEntityManager {

    protected TokenDataManager tokenDataManager;

    public TokenEntityManagerImpl(IdmEngineConfiguration idmEngineConfiguration, TokenDataManager tokenDataManager) {
        super(idmEngineConfiguration);
        this.tokenDataManager = tokenDataManager;
    }

    @Override
    protected DataManager<TokenEntity> getDataManager() {
        return tokenDataManager;
    }

    @Override
    public Token createNewToken(String tokenId) {
        TokenEntity tokenEntity = create();
        tokenEntity.setId(tokenId);
        tokenEntity.setRevision(0); // needed as tokens can be transient
        return tokenEntity;
    }

    @Override
    public void updateToken(Token updatedToken) {
        super.update((TokenEntity) updatedToken);
    }

    @Override
    public boolean isNewToken(Token token) {
        return ((TokenEntity) token).getRevision() == 0;
    }

    @Override
    public List<Token> findTokenByQueryCriteria(TokenQueryImpl query) {
        return tokenDataManager.findTokenByQueryCriteria(query);
    }

    @Override
    public long findTokenCountByQueryCriteria(TokenQueryImpl query) {
        return tokenDataManager.findTokenCountByQueryCriteria(query);
    }

    @Override
    public TokenQuery createNewTokenQuery() {
        return new TokenQueryImpl(getCommandExecutor());
    }

    @Override
    public List<Token> findTokensByNativeQuery(Map<String, Object> parameterMap) {
        return tokenDataManager.findTokensByNativeQuery(parameterMap);
    }

    @Override
    public long findTokenCountByNativeQuery(Map<String, Object> parameterMap) {
        return tokenDataManager.findTokenCountByNativeQuery(parameterMap);
    }
}
