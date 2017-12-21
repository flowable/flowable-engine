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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flowable.idm.engine.IdmEngineConfiguration;
import org.flowable.idm.engine.impl.persistence.entity.IdentityInfoEntity;
import org.flowable.idm.engine.impl.persistence.entity.IdentityInfoEntityImpl;
import org.flowable.idm.engine.impl.persistence.entity.data.AbstractIdmDataManager;
import org.flowable.idm.engine.impl.persistence.entity.data.IdentityInfoDataManager;

/**
 * @author Joram Barrez
 */
public class MybatisIdentityInfoDataManager extends AbstractIdmDataManager<IdentityInfoEntity> implements IdentityInfoDataManager {

    public MybatisIdentityInfoDataManager(IdmEngineConfiguration idmEngineConfiguration) {
        super(idmEngineConfiguration);
    }

    @Override
    public Class<? extends IdentityInfoEntity> getManagedEntityClass() {
        return IdentityInfoEntityImpl.class;
    }

    @Override
    public IdentityInfoEntity create() {
        return new IdentityInfoEntityImpl();
    }

    @Override
    public List<IdentityInfoEntity> findIdentityInfoDetails(String identityInfoId) {
        return getDbSqlSession().getSqlSession().selectList("selectIdentityInfoDetails", identityInfoId);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<IdentityInfoEntity> findIdentityInfoByUserId(String userId) {
        return getDbSqlSession().selectList("selectIdentityInfoByUserId", userId);
    }

    @Override
    public IdentityInfoEntity findUserInfoByUserIdAndKey(String userId, String key) {
        Map<String, String> parameters = new HashMap<>();
        parameters.put("userId", userId);
        parameters.put("key", key);
        return (IdentityInfoEntity) getDbSqlSession().selectOne("selectIdentityInfoByUserIdAndKey", parameters);
    }

    @Override
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public List<String> findUserInfoKeysByUserIdAndType(String userId, String type) {
        Map<String, String> parameters = new HashMap<>();
        parameters.put("userId", userId);
        parameters.put("type", type);
        return (List) getDbSqlSession().getSqlSession().selectList("selectIdentityInfoKeysByUserIdAndType", parameters);
    }

}
