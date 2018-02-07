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
package org.flowable.dmn.engine.impl.persistence.entity.data.impl;

import java.util.List;
import java.util.Map;

import org.flowable.dmn.api.DmnHistoricDecisionExecution;
import org.flowable.dmn.engine.DmnEngineConfiguration;
import org.flowable.dmn.engine.impl.HistoricDecisionExecutionQueryImpl;
import org.flowable.dmn.engine.impl.persistence.entity.HistoricDecisionExecutionEntity;
import org.flowable.dmn.engine.impl.persistence.entity.HistoricDecisionExecutionEntityImpl;
import org.flowable.dmn.engine.impl.persistence.entity.data.AbstractDmnDataManager;
import org.flowable.dmn.engine.impl.persistence.entity.data.HistoricDecisionExecutionDataManager;

/**
 * @author Tijs Rademakers
 */
public class MybatisHistoricDecisionExecutionDataManager extends AbstractDmnDataManager<HistoricDecisionExecutionEntity> implements HistoricDecisionExecutionDataManager {

    public MybatisHistoricDecisionExecutionDataManager(DmnEngineConfiguration dmnEngineConfiguration) {
        super(dmnEngineConfiguration);
    }

    @Override
    public Class<? extends HistoricDecisionExecutionEntity> getManagedEntityClass() {
        return HistoricDecisionExecutionEntityImpl.class;
    }

    @Override
    public HistoricDecisionExecutionEntity create() {
        return new HistoricDecisionExecutionEntityImpl();
    }
    
    @Override
    public void deleteHistoricDecisionExecutionsByDeploymentId(String deploymentId) {
        getDbSqlSession().delete("deleteHistoricDecisionExecutionsByDeploymentId", deploymentId, getManagedEntityClass());
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<DmnHistoricDecisionExecution> findHistoricDecisionExecutionsByQueryCriteria(HistoricDecisionExecutionQueryImpl decisionExecutionQuery) {
        return getDbSqlSession().selectList("selectHistoricDecisionExecutionsByQueryCriteria", decisionExecutionQuery);
    }

    @Override
    public long findHistoricDecisionExecutionCountByQueryCriteria(HistoricDecisionExecutionQueryImpl decisionExecutionQuery) {
        return (Long) getDbSqlSession().selectOne("selectHistoricDecisionExecutionCountByQueryCriteria", decisionExecutionQuery);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<DmnHistoricDecisionExecution> findHistoricDecisionExecutionsByNativeQuery(Map<String, Object> parameterMap) {
        return getDbSqlSession().selectListWithRawParameter("selectHistoricDecisionExecutionsByNativeQuery", parameterMap);
    }

    @Override
    public long findHistoricDecisionExecutionCountByNativeQuery(Map<String, Object> parameterMap) {
        return (Long) getDbSqlSession().selectOne("selectHistoricDecisionExecutionCountByNativeQuery", parameterMap);
    }
}
