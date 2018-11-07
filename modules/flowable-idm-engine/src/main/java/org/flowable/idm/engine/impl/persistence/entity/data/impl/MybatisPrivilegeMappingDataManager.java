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

import org.flowable.idm.api.PrivilegeMapping;
import org.flowable.idm.engine.IdmEngineConfiguration;
import org.flowable.idm.engine.impl.persistence.entity.PrivilegeMappingEntity;
import org.flowable.idm.engine.impl.persistence.entity.PrivilegeMappingEntityImpl;
import org.flowable.idm.engine.impl.persistence.entity.data.AbstractIdmDataManager;
import org.flowable.idm.engine.impl.persistence.entity.data.PrivilegeMappingDataManager;

public class MybatisPrivilegeMappingDataManager extends AbstractIdmDataManager<PrivilegeMappingEntity> implements PrivilegeMappingDataManager {

    public MybatisPrivilegeMappingDataManager(IdmEngineConfiguration idmEngineConfiguration) {
        super(idmEngineConfiguration);
    }

    @Override
    public PrivilegeMappingEntity create() {
        return new PrivilegeMappingEntityImpl();
    }

    @Override
    public Class<? extends PrivilegeMappingEntity> getManagedEntityClass() {
        return PrivilegeMappingEntityImpl.class;
    }

    @Override
    public void deleteByPrivilegeId(String privilegeId) {
        getDbSqlSession().delete("deleteByPrivilegeId", privilegeId, getManagedEntityClass());
    }

    @Override
    public void deleteByPrivilegeIdAndUserId(String privilegeId, String userId) {
        Map<String, String> params = new HashMap<>();
        params.put("privilegeId", privilegeId);
        params.put("userId", userId);
        getDbSqlSession().delete("deleteByPrivilegeIdAndUserId", params, getManagedEntityClass());
    }

    @Override
    public void deleteByPrivilegeIdAndGroupId(String privilegeId, String groupId) {
        Map<String, String> params = new HashMap<>();
        params.put("privilegeId", privilegeId);
        params.put("groupId", groupId);
        getDbSqlSession().delete("deleteByPrivilegeIdAndGroupId", params, getManagedEntityClass());
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public List<PrivilegeMapping> getPrivilegeMappingsByPrivilegeId(String privilegeId) {
        return (List<PrivilegeMapping>) getDbSqlSession().selectList("selectPrivilegeMappingsByPrivilegeId", privilegeId); 
    }

}
