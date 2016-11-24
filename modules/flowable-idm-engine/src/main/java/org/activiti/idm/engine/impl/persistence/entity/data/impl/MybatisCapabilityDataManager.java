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
package org.activiti.idm.engine.impl.persistence.entity.data.impl;

import java.util.List;
import java.util.Map;

import org.activiti.engine.common.impl.Page;
import org.activiti.idm.api.Capability;
import org.activiti.idm.engine.IdmEngineConfiguration;
import org.activiti.idm.engine.impl.CapabilityQueryImpl;
import org.activiti.idm.engine.impl.persistence.entity.CapabilityEntity;
import org.activiti.idm.engine.impl.persistence.entity.CapabilityEntityImpl;
import org.activiti.idm.engine.impl.persistence.entity.data.AbstractDataManager;
import org.activiti.idm.engine.impl.persistence.entity.data.CapabilityDataManager;

/**
 * @author Joram Barrez
 */
public class MybatisCapabilityDataManager extends AbstractDataManager<CapabilityEntity> implements CapabilityDataManager {

  public MybatisCapabilityDataManager(IdmEngineConfiguration idmEngineConfiguration) {
    super(idmEngineConfiguration);
  }

  @Override
  public CapabilityEntity create() {
    return new CapabilityEntityImpl();
  }

  @Override
  public Class<? extends CapabilityEntity> getManagedEntityClass() {
    return CapabilityEntityImpl.class;
  }

  @Override
  @SuppressWarnings("unchecked")
  public List<Capability> findCapabilityByQueryCriteria(CapabilityQueryImpl query, Page page) {
    return getDbSqlSession().selectList("selectCapabilityByQueryCriteria", query, page);
  }

  @Override
  public long findCapabilityCountByQueryCriteria(CapabilityQueryImpl query) {
    return (Long) getDbSqlSession().selectOne("selectCapabilityCountByQueryCriteria", query);
  }

  @Override
  @SuppressWarnings("unchecked")
  public List<Capability> findCapabilityByNativeQuery(Map<String, Object> parameterMap, int firstResult, int maxResults) {
    return getDbSqlSession().selectListWithRawParameter("selectCapabilityByNativeQuery", parameterMap, firstResult, maxResults);
  }

  @Override
  public long findCapabilityCountByNativeQuery(Map<String, Object> parameterMap) {
    return (Long) getDbSqlSession().selectOne("selectCapabilityCountByNativeQuery", parameterMap);
  }

}
