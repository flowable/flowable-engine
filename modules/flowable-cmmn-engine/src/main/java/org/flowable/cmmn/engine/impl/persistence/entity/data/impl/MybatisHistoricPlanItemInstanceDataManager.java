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

import java.util.Collection;
import java.util.List;

import org.flowable.cmmn.api.history.HistoricPlanItemInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.impl.history.HistoricPlanItemInstanceQueryImpl;
import org.flowable.cmmn.engine.impl.persistence.entity.HistoricPlanItemInstanceEntity;
import org.flowable.cmmn.engine.impl.persistence.entity.HistoricPlanItemInstanceEntityImpl;
import org.flowable.cmmn.engine.impl.persistence.entity.data.AbstractCmmnDataManager;
import org.flowable.cmmn.engine.impl.persistence.entity.data.HistoricPlanItemInstanceDataManager;
import org.flowable.common.engine.impl.persistence.cache.CachedEntityMatcherAdapter;

/**
 * @author Dennis Federico
 */
public class MybatisHistoricPlanItemInstanceDataManager extends AbstractCmmnDataManager<HistoricPlanItemInstanceEntity> implements HistoricPlanItemInstanceDataManager {

    protected CachedEntityMatcherAdapter<HistoricPlanItemInstanceEntity> historicPlanItemInstanceByCaseDefinitionIdMatcher = new CachedEntityMatcherAdapter<>() {
        @Override
        public boolean isRetained(HistoricPlanItemInstanceEntity entity, Object param) {
            return entity.getCaseDefinitionId().equals(param);
        }
    };

    public MybatisHistoricPlanItemInstanceDataManager(CmmnEngineConfiguration cmmnEngineConfiguration) {
        super(cmmnEngineConfiguration);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<HistoricPlanItemInstance> findByCriteria(HistoricPlanItemInstanceQueryImpl query) {
        setSafeInValueLists(query);
        return getDbSqlSession().selectList("selectHistoricPlanItemInstancesByQueryCriteria", query, getManagedEntityClass());
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<HistoricPlanItemInstance> findByCaseDefinitionId(String caseDefinitionId) {
        List<? extends HistoricPlanItemInstance> list = getList("selectHistoricPlanItemInstancesByCaseDefinitionId", caseDefinitionId, historicPlanItemInstanceByCaseDefinitionIdMatcher, true);
        return (List<HistoricPlanItemInstance>) list;
    }

    @Override
    public long countByCriteria(HistoricPlanItemInstanceQueryImpl query) {
        setSafeInValueLists(query);
        return (Long) getDbSqlSession().selectOne("selectHistoricPlanItemInstancesCountByQueryCriteria", query);
    }

    @Override
    public void deleteByCaseDefinitionId(String caseDefinitionId) {
        getDbSqlSession().delete("deleteHistoricPlanItemInstanceByCaseDefinitionId", caseDefinitionId, getManagedEntityClass());
    }
    
    @Override
    public void bulkDeleteHistoricPlanItemInstancesForCaseInstanceIds(Collection<String> caseInstanceIds) {
        getDbSqlSession().delete("bulkDeleteHistoricPlanItemInstancesByCaseInstanceIds", createSafeInValuesList(caseInstanceIds), getManagedEntityClass());
    }
    
    @Override
    public void deleteHistoricPlanItemInstancesForNonExistingCaseInstances() {
        getDbSqlSession().delete("bulkDeleteHistoricPlanItemInstancesForNonExistingCaseInstances", null, getManagedEntityClass());
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<HistoricPlanItemInstance> findWithVariablesByCriteria(HistoricPlanItemInstanceQueryImpl historicPlanItemInstanceQuery) {
        setSafeInValueLists(historicPlanItemInstanceQuery);
        return getDbSqlSession().selectList("selectHistoricPlanItemInstancesWithLocalVariablesByQueryCriteria", historicPlanItemInstanceQuery,
                getManagedEntityClass());
    }

    @Override
    public Class<? extends HistoricPlanItemInstanceEntity> getManagedEntityClass() {
        return HistoricPlanItemInstanceEntityImpl.class;
    }

    @Override
    public HistoricPlanItemInstanceEntity create() {
        return new HistoricPlanItemInstanceEntityImpl();
    }

    @Override
    public HistoricPlanItemInstanceEntity create(PlanItemInstance planItemInstance) {
        return new HistoricPlanItemInstanceEntityImpl(planItemInstance);
    }
    
    protected void setSafeInValueLists(HistoricPlanItemInstanceQueryImpl planItemInstanceQuery) {
        if (planItemInstanceQuery.getInvolvedGroups() != null) {
            planItemInstanceQuery.setSafeInvolvedGroups(createSafeInValuesList(planItemInstanceQuery.getInvolvedGroups()));
        }
        if(planItemInstanceQuery.getCaseInstanceIds() != null) {
            planItemInstanceQuery.setSafeCaseInstanceIds(createSafeInValuesList(planItemInstanceQuery.getCaseInstanceIds()));
        }

    }
}
