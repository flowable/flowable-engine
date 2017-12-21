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

import org.flowable.idm.api.Privilege;
import org.flowable.idm.engine.IdmEngineConfiguration;
import org.flowable.idm.engine.impl.PrivilegeQueryImpl;
import org.flowable.idm.engine.impl.persistence.entity.PrivilegeEntity;
import org.flowable.idm.engine.impl.persistence.entity.PrivilegeEntityImpl;
import org.flowable.idm.engine.impl.persistence.entity.data.AbstractIdmDataManager;
import org.flowable.idm.engine.impl.persistence.entity.data.PrivilegeDataManager;

/**
 * @author Joram Barrez
 */
public class MybatisPrivilegeDataManager extends AbstractIdmDataManager<PrivilegeEntity> implements PrivilegeDataManager {

    public MybatisPrivilegeDataManager(IdmEngineConfiguration idmEngineConfiguration) {
        super(idmEngineConfiguration);
    }

    @Override
    public PrivilegeEntity create() {
        return new PrivilegeEntityImpl();
    }

    @Override
    public Class<? extends PrivilegeEntity> getManagedEntityClass() {
        return PrivilegeEntityImpl.class;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Privilege> findPrivilegeByQueryCriteria(PrivilegeQueryImpl query) {
        return getDbSqlSession().selectList("selectPrivilegeByQueryCriteria", query);
    }

    @Override
    public long findPrivilegeCountByQueryCriteria(PrivilegeQueryImpl query) {
        return (Long) getDbSqlSession().selectOne("selectPrivilegeCountByQueryCriteria", query);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Privilege> findPrivilegeByNativeQuery(Map<String, Object> parameterMap) {
        return getDbSqlSession().selectListWithRawParameter("selectPrivilegeByNativeQuery", parameterMap);
    }

    @Override
    public long findPrivilegeCountByNativeQuery(Map<String, Object> parameterMap) {
        return (Long) getDbSqlSession().selectOne("selectPrivilegeCountByNativeQuery", parameterMap);
    }

}
