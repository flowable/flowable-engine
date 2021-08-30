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
package org.flowable.cmmn.engine.impl.persistence.entity.data.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flowable.cmmn.api.history.HistoricMilestoneInstance;
import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.impl.history.HistoricMilestoneInstanceQueryImpl;
import org.flowable.cmmn.engine.impl.persistence.entity.HistoricMilestoneInstanceEntity;
import org.flowable.cmmn.engine.impl.persistence.entity.HistoricMilestoneInstanceEntityImpl;
import org.flowable.cmmn.engine.impl.persistence.entity.data.AbstractCmmnDataManager;
import org.flowable.cmmn.engine.impl.persistence.entity.data.HistoricMilestoneInstanceDataManager;

/**
 * @author Joram Barrez
 */
public class MybatisHistoricMilestoneInstanceDataManager extends AbstractCmmnDataManager<HistoricMilestoneInstanceEntity> implements HistoricMilestoneInstanceDataManager {

    public MybatisHistoricMilestoneInstanceDataManager(CmmnEngineConfiguration cmmnEngineConfiguration) {
        super(cmmnEngineConfiguration);
    }

    @Override
    public Class<? extends HistoricMilestoneInstanceEntity> getManagedEntityClass() {
        return HistoricMilestoneInstanceEntityImpl.class;
    }

    @Override
    public HistoricMilestoneInstanceEntity create() {
        return new HistoricMilestoneInstanceEntityImpl();
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public List<HistoricMilestoneInstance> findHistoricMilestoneInstancesByQueryCriteria(HistoricMilestoneInstanceQueryImpl query) {
        return getDbSqlSession().selectList("selectHistoricMilestoneInstancesByQueryCriteria", query, getManagedEntityClass());
    }
    
    @Override
    public long findHistoricMilestoneInstancesCountByQueryCriteria(HistoricMilestoneInstanceQueryImpl query) {
        return (Long) getDbSqlSession().selectOne("selectHistoricMilestoneInstanceCountByQueryCriteria", query);
    }
    
    @Override
    public void deleteHistoricMilestoneInstancesForNonExistingCaseInstances() {
        getDbSqlSession().delete("bulkDeleteHistoricMilestoneInstancesForNonExistingCaseInstances", null, getManagedEntityClass());
    }

    @Override
    public long countChangeTenantIdCmmnHistoricMilestoneInstances(String sourceTenantId, boolean onlyInstancesFromDefaultTenantDefinitions) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("sourceTenantId", sourceTenantId);
        parameters.put("defaultTenantId", getDefaultTenantId(sourceTenantId));
        parameters.put("onlyInstancesFromDefaultTenantDefinitions", onlyInstancesFromDefaultTenantDefinitions);
        return (long) getDbSqlSession().selectOne("countChangeTenantIdCmmnHistoricMilestoneInstances", parameters);
    }

    @Override
    public long changeTenantIdCmmnHistoricMilestoneInstances(String sourceTenantId, String targetTenantId, boolean onlyInstancesFromDefaultTenantDefinitions) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("sourceTenantId", sourceTenantId);
        parameters.put("targetTenantId", targetTenantId);
        parameters.put("defaultTenantId", getDefaultTenantId(sourceTenantId));
        parameters.put("onlyInstancesFromDefaultTenantDefinitions", onlyInstancesFromDefaultTenantDefinitions);
        return (long) getDbSqlSession().update("changeTenantIdCmmnHistoricMilestoneInstances", parameters);
    }
    
}
