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

import org.flowable.idm.api.Privilege;
import org.flowable.idm.api.PrivilegeQuery;
import org.flowable.idm.engine.IdmEngineConfiguration;
import org.flowable.idm.engine.impl.PrivilegeQueryImpl;
import org.flowable.idm.engine.impl.persistence.entity.data.PrivilegeDataManager;

/**
 * @author Joram Barrez
 */
public class PrivilegeEntityManagerImpl
    extends AbstractIdmEngineEntityManager<PrivilegeEntity, PrivilegeDataManager>
    implements PrivilegeEntityManager {

    public PrivilegeEntityManagerImpl(IdmEngineConfiguration idmEngineConfiguration, PrivilegeDataManager privilegeDataManager) {
        super(idmEngineConfiguration, privilegeDataManager);
    }

    @Override
    public PrivilegeQuery createNewPrivilegeQuery() {
        return new PrivilegeQueryImpl(getCommandExecutor());
    }

    @Override
    public List<Privilege> findPrivilegeByQueryCriteria(PrivilegeQueryImpl query) {
        return dataManager.findPrivilegeByQueryCriteria(query);
    }

    @Override
    public long findPrivilegeCountByQueryCriteria(PrivilegeQueryImpl query) {
        return dataManager.findPrivilegeCountByQueryCriteria(query);
    }

    @Override
    public List<Privilege> findPrivilegeByNativeQuery(Map<String, Object> parameterMap) {
        return dataManager.findPrivilegeByNativeQuery(parameterMap);
    }

    @Override
    public long findPrivilegeCountByNativeQuery(Map<String, Object> parameterMap) {
        return dataManager.findPrivilegeCountByNativeQuery(parameterMap);
    }

}
