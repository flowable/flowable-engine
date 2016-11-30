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
package org.activiti.idm.engine.impl.persistence.entity;

import java.util.List;
import java.util.Map;

import org.activiti.engine.common.impl.Page;
import org.activiti.engine.common.impl.persistence.entity.data.DataManager;
import org.activiti.idm.api.Privilege;
import org.activiti.idm.api.PrivilegeQuery;
import org.activiti.idm.engine.IdmEngineConfiguration;
import org.activiti.idm.engine.impl.PrivilegeQueryImpl;
import org.activiti.idm.engine.impl.persistence.entity.data.PrivilegeDataManager;

/**
 * @author Joram Barrez
 */
public class PrivilegeEntityManagerImpl extends AbstractEntityManager<PrivilegeEntity> implements PrivilegeEntityManager {

  protected PrivilegeDataManager privilegeDataManager;
  
  public PrivilegeEntityManagerImpl(IdmEngineConfiguration idmEngineConfiguration, PrivilegeDataManager privilegeDataManager) {
    super(idmEngineConfiguration);
    this.privilegeDataManager = privilegeDataManager;
  }

  @Override
  protected DataManager<PrivilegeEntity> getDataManager() {
    return privilegeDataManager;
  }
  
  @Override
  public PrivilegeQuery createNewPrivilegeQuery() {
    return new PrivilegeQueryImpl(getCommandExecutor());
  }

  @Override
  public List<Privilege> findPrivilegeByQueryCriteria(PrivilegeQueryImpl query, Page page) {
    return privilegeDataManager.findPrivilegeByQueryCriteria(query, page);
  }

  @Override
  public long findPrivilegeCountByQueryCriteria(PrivilegeQueryImpl query) {
    return privilegeDataManager.findPrivilegeCountByQueryCriteria(query);
  }

  @Override
  public List<Privilege> findPrivilegeByNativeQuery(Map<String, Object> parameterMap, int firstResult, int maxResults) {
    return privilegeDataManager.findPrivilegeByNativeQuery(parameterMap, firstResult, maxResults);
  }

  @Override
  public long findPrivilegeCountByNativeQuery(Map<String, Object> parameterMap) {
    return privilegeDataManager.findPrivilegeCountByNativeQuery(parameterMap);
  }

}
