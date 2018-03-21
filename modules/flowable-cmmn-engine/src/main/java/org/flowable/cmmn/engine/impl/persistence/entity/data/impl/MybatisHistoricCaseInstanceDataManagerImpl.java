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

import java.util.Collections;
import java.util.List;

import org.flowable.cmmn.api.history.HistoricCaseInstance;
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
    public List<HistoricCaseInstanceEntity> findHistoricCaseInstancesByCaseDefinitionId(String caseDefinitionId) {
        return getList("selectHistoricCaseInstancesByCaseDefinitionId", caseDefinitionId, historicCaseInstanceByCaseDefinitionIdMatcher, true);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<HistoricCaseInstance> findByCriteria(HistoricCaseInstanceQueryImpl query) {
        return getDbSqlSession().selectList("selectHistoricCaseInstancesByQueryCriteria", query);
    }

    @Override
    public long countByCriteria(HistoricCaseInstanceQueryImpl query) {
        return (Long) getDbSqlSession().selectOne("selectHistoricCaseInstanceCountByQueryCriteria", query);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<HistoricCaseInstance> findWithVariablesByQueryCriteria(HistoricCaseInstanceQueryImpl historicCaseInstanceQuery) {
        // paging doesn't work for combining process instances and variables
        // due to an outer join, so doing it in-memory

        int firstResult = historicCaseInstanceQuery.getFirstResult();
        int maxResults = historicCaseInstanceQuery.getMaxResults();

        // setting max results, limit to 20000 results for performance reasons
        if (historicCaseInstanceQuery.getCaseVariablesLimit() != null) {
            historicCaseInstanceQuery.setMaxResults(historicCaseInstanceQuery.getCaseVariablesLimit());
        } else {
            historicCaseInstanceQuery.setMaxResults(getCmmnEngineConfiguration().getHistoricCaseQueryLimit());
        }
        historicCaseInstanceQuery.setFirstResult(0);

        List<HistoricCaseInstance> instanceList = getDbSqlSession().selectListWithRawParameterNoCacheCheck("selectHistoricCaseInstancesWithVariablesByQueryCriteria", historicCaseInstanceQuery);

        if (instanceList != null && !instanceList.isEmpty()) {
            if (firstResult > 0) {
                if (firstResult <= instanceList.size()) {
                    int toIndex = firstResult + Math.min(maxResults, instanceList.size() - firstResult);
                    return instanceList.subList(firstResult, toIndex);
                } else {
                    return Collections.EMPTY_LIST;
                }
            } else {
                int toIndex = maxResults > 0 ? Math.min(maxResults, instanceList.size()) : instanceList.size();
                return instanceList.subList(0, toIndex);
            }
        }

        return instanceList;
    }


    @Override
    public void deleteByCaseDefinitionId(String caseDefinitionId) {
        getDbSqlSession().delete("deleteHistoricCaseInstanceByCaseDefinitionId", caseDefinitionId, getManagedEntityClass());
    }

}
