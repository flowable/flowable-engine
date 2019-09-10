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

import org.flowable.idm.api.Group;
import org.flowable.idm.engine.IdmEngineConfiguration;
import org.flowable.idm.engine.impl.GroupQueryImpl;
import org.flowable.idm.engine.impl.persistence.entity.GroupEntity;
import org.flowable.idm.engine.impl.persistence.entity.GroupEntityImpl;
import org.flowable.idm.engine.impl.persistence.entity.data.AbstractIdmDataManager;
import org.flowable.idm.engine.impl.persistence.entity.data.GroupDataManager;

/**
 * @author Joram Barrez
 */
public class MybatisGroupDataManager extends AbstractIdmDataManager<GroupEntity> implements GroupDataManager {

    public MybatisGroupDataManager(IdmEngineConfiguration idmEngineConfiguration) {
        super(idmEngineConfiguration);
    }

    @Override
    public Class<? extends GroupEntity> getManagedEntityClass() {
        return GroupEntityImpl.class;
    }

    @Override
    public GroupEntity create() {
        return new GroupEntityImpl();
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<Group> findGroupByQueryCriteria(GroupQueryImpl query) {
        return getDbSqlSession().selectList("selectGroupByQueryCriteria", query, getManagedEntityClass());
    }

    @Override
    public long findGroupCountByQueryCriteria(GroupQueryImpl query) {
        return (Long) getDbSqlSession().selectOne("selectGroupCountByQueryCriteria", query);
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<Group> findGroupsByUser(String userId) {
        return getDbSqlSession().selectList("selectGroupsByUserId", userId);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Group> findGroupsByPrivilegeId(String privilegeId) {
        return getDbSqlSession().selectList("selectGroupsWithPrivilegeId", privilegeId);
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<Group> findGroupsByNativeQuery(Map<String, Object> parameterMap) {
        return getDbSqlSession().selectListWithRawParameter("selectGroupByNativeQuery", parameterMap);
    }

    @Override
    public long findGroupCountByNativeQuery(Map<String, Object> parameterMap) {
        return (Long) getDbSqlSession().selectOne("selectGroupCountByNativeQuery", parameterMap);
    }

}
