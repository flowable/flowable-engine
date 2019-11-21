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

import org.flowable.idm.api.PrivilegeMapping;
import org.flowable.idm.engine.IdmEngineConfiguration;
import org.flowable.idm.engine.impl.persistence.entity.data.PrivilegeMappingDataManager;

public class PrivilegeMappingEntityManagerImpl
    extends AbstractIdmEngineEntityManager<PrivilegeMappingEntity, PrivilegeMappingDataManager>
    implements PrivilegeMappingEntityManager {

    public PrivilegeMappingEntityManagerImpl(IdmEngineConfiguration idmEngineConfiguration, PrivilegeMappingDataManager privilegeMappingDataManager) {
        super(idmEngineConfiguration, privilegeMappingDataManager);
    }

    @Override
    public void deleteByPrivilegeId(String privilegeId) {
        dataManager.deleteByPrivilegeId(privilegeId);
    }

    @Override
    public void deleteByPrivilegeIdAndUserId(String privilegeId, String userId) {
        dataManager.deleteByPrivilegeIdAndUserId(privilegeId, userId);
    }

    @Override
    public void deleteByPrivilegeIdAndGroupId(String privilegeId, String groupId) {
        dataManager.deleteByPrivilegeIdAndGroupId(privilegeId, groupId);
    }
    
    @Override
    public List<PrivilegeMapping> getPrivilegeMappingsByPrivilegeId(String privilegeId) {
        return dataManager.getPrivilegeMappingsByPrivilegeId(privilegeId);
    }

}
