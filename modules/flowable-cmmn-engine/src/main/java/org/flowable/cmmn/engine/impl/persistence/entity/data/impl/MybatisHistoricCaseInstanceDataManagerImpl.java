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

import java.util.List;

import org.flowable.cmmn.api.history.HistoricCaseInstance;
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.impl.history.HistoricCaseInstanceQueryImpl;
import org.flowable.cmmn.engine.impl.persistence.entity.HistoricCaseInstanceEntity;
import org.flowable.cmmn.engine.impl.persistence.entity.HistoricCaseInstanceEntityImpl;
import org.flowable.cmmn.engine.impl.persistence.entity.data.AbstractCmmnDataManager;
import org.flowable.cmmn.engine.impl.persistence.entity.data.HistoricCaseInstanceDataManager;
import org.flowable.cmmn.engine.impl.persistence.entity.data.impl.matcher.HistoricCaseInstanceByCaseDefinitionIdMatcher;

/**
 * @author Joram Barrez
 */
public class MybatisHistoricCaseInstanceDataManagerImpl extends AbstractCmmnDataManager<HistoricCaseInstanceEntity> implements HistoricCaseInstanceDataManager {
    
    protected HistoricCaseInstanceByCaseDefinitionIdMatcher historicCaseInstanceByCaseDefinitionIdMatcher = new HistoricCaseInstanceByCaseDefinitionIdMatcher();

    public MybatisHistoricCaseInstanceDataManagerImpl(CmmnEngineConfiguration cmmnEngineConfiguration) {
        super(cmmnEngineConfiguration);
    }

    @Override
    public Class<? extends HistoricCaseInstanceEntity> getManagedEntityClass() {
        return HistoricCaseInstanceEntityImpl.class;
    }

    @Override
    public HistoricCaseInstanceEntity create() {
        return new HistoricCaseInstanceEntityImpl();
    }
    
    @Override
    public HistoricCaseInstanceEntity create(CaseInstance caseInstance) {
        return new HistoricCaseInstanceEntityImpl(caseInstance);
    }

    @Override
    public List<HistoricCaseInstanceEntity> findHistoricCaseInstancesByCaseDefinitionId(String caseDefinitionId) {
        return getList("selectHistoricCaseInstancesByCaseDefinitionId", caseDefinitionId, historicCaseInstanceByCaseDefinitionIdMatcher, true);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<HistoricCaseInstance> findByCriteria(HistoricCaseInstanceQueryImpl query) {
        setSafeInValueLists(query);
        return getDbSqlSession().selectList("selectHistoricCaseInstancesByQueryCriteria", query, getManagedEntityClass());
    }

    @Override
    public long countByCriteria(HistoricCaseInstanceQueryImpl query) {
        setSafeInValueLists(query);
        return (Long) getDbSqlSession().selectOne("selectHistoricCaseInstanceCountByQueryCriteria", query);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<HistoricCaseInstance> findWithVariablesByQueryCriteria(HistoricCaseInstanceQueryImpl historicCaseInstanceQuery) {
        setSafeInValueLists(historicCaseInstanceQuery);
        return getDbSqlSession().selectList("selectHistoricCaseInstancesWithVariablesByQueryCriteria", historicCaseInstanceQuery, getManagedEntityClass());
    }


    @Override
    public void deleteByCaseDefinitionId(String caseDefinitionId) {
        getDbSqlSession().delete("deleteHistoricCaseInstanceByCaseDefinitionId", caseDefinitionId, getManagedEntityClass());
    }

    @Override
    public void deleteHistoricCaseInstances(HistoricCaseInstanceQueryImpl historicCaseInstanceQuery) {
        getDbSqlSession().delete("bulkDeleteHistoricCaseInstances", historicCaseInstanceQuery, getManagedEntityClass());
    }
    
    protected void setSafeInValueLists(HistoricCaseInstanceQueryImpl caseInstanceQuery) {
        if (caseInstanceQuery.getInvolvedGroups() != null) {
            caseInstanceQuery.setSafeInvolvedGroups(createSafeInValuesList(caseInstanceQuery.getInvolvedGroups()));
        }
        
        if (caseInstanceQuery.getOrQueryObjects() != null && !caseInstanceQuery.getOrQueryObjects().isEmpty()) {
            for (HistoricCaseInstanceQueryImpl orCaseInstanceQuery : caseInstanceQuery.getOrQueryObjects()) {
                setSafeInValueLists(orCaseInstanceQuery);
            }
        }
    }
}
