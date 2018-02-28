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
package org.flowable.identitylink.service.impl.persistence.entity.data.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flowable.engine.common.impl.db.AbstractDataManager;
import org.flowable.engine.common.impl.db.CachedEntityMatcher;
import org.flowable.identitylink.service.impl.persistence.entity.IdentityLinkEntity;
import org.flowable.identitylink.service.impl.persistence.entity.IdentityLinkEntityImpl;
import org.flowable.identitylink.service.impl.persistence.entity.data.IdentityLinkDataManager;
import org.flowable.identitylink.service.impl.persistence.entity.data.impl.cachematcher.IdentityLinksByProcInstMatcher;
import org.flowable.identitylink.service.impl.persistence.entity.data.impl.cachematcher.IdentityLinksByScopeIdAndTypeMatcher;

/**
 * @author Joram Barrez
 */
public class MybatisIdentityLinkDataManager extends AbstractDataManager<IdentityLinkEntity> implements IdentityLinkDataManager {

    protected CachedEntityMatcher<IdentityLinkEntity> identityLinkByProcessInstanceMatcher = new IdentityLinksByProcInstMatcher();
    protected CachedEntityMatcher<IdentityLinkEntity> identityLinksByScopeIdAndTypeMatcher = new IdentityLinksByScopeIdAndTypeMatcher();

    @Override
    public Class<? extends IdentityLinkEntity> getManagedEntityClass() {
        return IdentityLinkEntityImpl.class;
    }

    @Override
    public IdentityLinkEntity create() {
        return new IdentityLinkEntityImpl();
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<IdentityLinkEntity> findIdentityLinksByTaskId(String taskId) {
        return getDbSqlSession().selectList("selectIdentityLinksByTask", taskId);
    }

    @Override
    public List<IdentityLinkEntity> findIdentityLinksByProcessInstanceId(String processInstanceId) {
        return getList("selectIdentityLinksByProcessInstance", processInstanceId, identityLinkByProcessInstanceMatcher, true);
    }
    
    @Override
    public List<IdentityLinkEntity> findIdentityLinksByScopeIdAndType(String scopeId, String scopeType) {
        Map<String, String> parameters = new HashMap<>();
        parameters.put("scopeId", scopeId);
        parameters.put("scopeType", scopeType);
        return getList("selectIdentityLinksByScopeIdAndType", parameters, identityLinksByScopeIdAndTypeMatcher, true);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<IdentityLinkEntity> findIdentityLinksByScopeDefinitionIdAndType(String scopeDefinitionId, String scopeType) {
        Map<String, String> parameters = new HashMap<>();
        parameters.put("scopeDefinitionId", scopeDefinitionId);
        parameters.put("scopeType", scopeType);
        return getDbSqlSession().selectList("selectIdentityLinksByScopeDefinitionAndType", parameters);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<IdentityLinkEntity> findIdentityLinksByProcessDefinitionId(String processDefinitionId) {
        return getDbSqlSession().selectList("selectIdentityLinksByProcessDefinition", processDefinitionId);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<IdentityLinkEntity> findIdentityLinkByTaskUserGroupAndType(String taskId, String userId, String groupId, String type) {
        Map<String, String> parameters = new HashMap<>();
        parameters.put("taskId", taskId);
        parameters.put("userId", userId);
        parameters.put("groupId", groupId);
        parameters.put("type", type);
        return getDbSqlSession().selectList("selectIdentityLinkByTaskUserGroupAndType", parameters);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<IdentityLinkEntity> findIdentityLinkByProcessInstanceUserGroupAndType(String processInstanceId, String userId, String groupId, String type) {
        Map<String, String> parameters = new HashMap<>();
        parameters.put("processInstanceId", processInstanceId);
        parameters.put("userId", userId);
        parameters.put("groupId", groupId);
        parameters.put("type", type);
        return getDbSqlSession().selectList("selectIdentityLinkByProcessInstanceUserGroupAndType", parameters);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<IdentityLinkEntity> findIdentityLinkByProcessDefinitionUserAndGroup(String processDefinitionId, String userId, String groupId) {
        Map<String, String> parameters = new HashMap<>();
        parameters.put("processDefinitionId", processDefinitionId);
        parameters.put("userId", userId);
        parameters.put("groupId", groupId);
        return getDbSqlSession().selectList("selectIdentityLinkByProcessDefinitionUserAndGroup", parameters);
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public List<IdentityLinkEntity> findIdentityLinkByScopeIdScopeTypeUserGroupAndType(String scopeId, String scopeType, String userId, String groupId, String type) {
        Map<String, String> parameters = new HashMap<>();
        parameters.put("scopeId", scopeId);
        parameters.put("scopeType", scopeType);
        parameters.put("userId", userId);
        parameters.put("groupId", groupId);
        parameters.put("type", type);
        return getDbSqlSession().selectList("selectIdentityLinkByScopeIdScopeTypeUserGroupAndType", parameters);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<IdentityLinkEntity> findIdentityLinkByScopeDefinitionScopeTypeUserAndGroup(String scopeDefinitionId, String scopeType, String userId, String groupId) {
        Map<String, String> parameters = new HashMap<>();
        parameters.put("scopeDefinitionId", scopeDefinitionId);
        parameters.put("scopeType", scopeType);
        parameters.put("userId", userId);
        parameters.put("groupId", groupId);
        return getDbSqlSession().selectList("selectIdentityLinkByScopeDefinitionScopeTypeUserAndGroup", parameters);
    }

    @Override
    public void deleteIdentityLinksByProcDef(String processDefId) {
        getDbSqlSession().delete("deleteIdentityLinkByProcDef", processDefId, IdentityLinkEntityImpl.class);
    }
    
    @Override
    public void deleteIdentityLinksByScopeIdAndScopeType(String scopeId, String scopeType) {
        Map<String, String> parameters = new HashMap<>();
        parameters.put("scopeId", scopeId);
        parameters.put("scopeType", scopeType);
        getDbSqlSession().delete("deleteIdentityLinksByScopeIdAndScopeType", parameters, IdentityLinkEntityImpl.class);
    }

}
