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

import org.activiti.engine.impl.Page;
import org.activiti.engine.impl.persistence.entity.data.DataManager;
import org.activiti.idm.api.Capability;
import org.activiti.idm.api.CapabilityQuery;
import org.activiti.idm.engine.IdmEngineConfiguration;
import org.activiti.idm.engine.impl.CapabilityQueryImpl;
import org.activiti.idm.engine.impl.persistence.entity.data.CapabilityDataManager;

/**
 * @author Joram Barrez
 */
public class CapabilityEntityManagerImpl extends AbstractEntityManager<CapabilityEntity> implements CapabilityEntityManager {

  protected CapabilityDataManager capabilityDataManager;
  
  public CapabilityEntityManagerImpl(IdmEngineConfiguration idmEngineConfiguration, CapabilityDataManager capabilityDataManager) {
    super(idmEngineConfiguration);
    this.capabilityDataManager = capabilityDataManager;
  }

  @Override
  protected DataManager<CapabilityEntity> getDataManager() {
    return capabilityDataManager;
  }
  
  @Override
  public CapabilityQuery createNewCapabilityQuery() {
    return new CapabilityQueryImpl(getCommandExecutor());
  }

  @Override
  public List<Capability> findCapabilityByQueryCriteria(CapabilityQueryImpl query, Page page) {
    return capabilityDataManager.findCapabilityByQueryCriteria(query, page);
  }

  @Override
  public long findCapabilityCountByQueryCriteria(CapabilityQueryImpl query) {
    return capabilityDataManager.findCapabilityCountByQueryCriteria(query);
  }

  @Override
  public List<Capability> findCapabilityByNativeQuery(Map<String, Object> parameterMap, int firstResult, int maxResults) {
    return capabilityDataManager.findCapabilityByNativeQuery(parameterMap, firstResult, maxResults);
  }

  @Override
  public long findCapabilityCountByNativeQuery(Map<String, Object> parameterMap) {
    return capabilityDataManager.findCapabilityCountByNativeQuery(parameterMap);
  }

}
