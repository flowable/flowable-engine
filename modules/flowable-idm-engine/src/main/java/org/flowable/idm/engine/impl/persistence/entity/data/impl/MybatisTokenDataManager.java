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
package org.flowable.idm.engine.impl.persistence.entity.data.impl;

import java.util.List;
import java.util.Map;

import org.flowable.idm.api.Token;
import org.flowable.idm.engine.IdmEngineConfiguration;
import org.flowable.idm.engine.impl.TokenQueryImpl;
import org.flowable.idm.engine.impl.persistence.entity.TokenEntity;
import org.flowable.idm.engine.impl.persistence.entity.TokenEntityImpl;
import org.flowable.idm.engine.impl.persistence.entity.data.AbstractIdmDataManager;
import org.flowable.idm.engine.impl.persistence.entity.data.TokenDataManager;

/**
 * @author Tijs Rademakers
 */
public class MybatisTokenDataManager extends AbstractIdmDataManager<TokenEntity> implements TokenDataManager {

    public MybatisTokenDataManager(IdmEngineConfiguration idmEngineConfiguration) {
        super(idmEngineConfiguration);
    }

    @Override
    public Class<? extends TokenEntity> getManagedEntityClass() {
        return TokenEntityImpl.class;
    }

    @Override
    public TokenEntity create() {
        return new TokenEntityImpl();
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<Token> findTokenByQueryCriteria(TokenQueryImpl query) {
        return getDbSqlSession().selectList("selectTokenByQueryCriteria", query, getManagedEntityClass());
    }

    @Override
    public long findTokenCountByQueryCriteria(TokenQueryImpl query) {
        return (Long) getDbSqlSession().selectOne("selectTokenCountByQueryCriteria", query);
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<Token> findTokensByNativeQuery(Map<String, Object> parameterMap) {
        return getDbSqlSession().selectListWithRawParameter("selectTokenByNativeQuery", parameterMap);
    }

    @Override
    public long findTokenCountByNativeQuery(Map<String, Object> parameterMap) {
        return (Long) getDbSqlSession().selectOne("selectTokenCountByNativeQuery", parameterMap);
    }
}
