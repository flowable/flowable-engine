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

package org.flowable.dmn.engine.impl.persistence.entity;

import java.util.List;
import java.util.Map;

import org.flowable.common.engine.impl.persistence.entity.AbstractEngineEntityManager;
import org.flowable.dmn.api.DmnHistoricDecisionExecution;
import org.flowable.dmn.engine.DmnEngineConfiguration;
import org.flowable.dmn.engine.impl.HistoricDecisionExecutionQueryImpl;
import org.flowable.dmn.engine.impl.persistence.entity.data.HistoricDecisionExecutionDataManager;

/**
 * @author Tijs Rademakers
 * @author Joram Barrez
 */
public class HistoricDecisionExecutionEntityManagerImpl
    extends AbstractEngineEntityManager<DmnEngineConfiguration, HistoricDecisionExecutionEntity, HistoricDecisionExecutionDataManager>
    implements HistoricDecisionExecutionEntityManager {

    public HistoricDecisionExecutionEntityManagerImpl(DmnEngineConfiguration dmnEngineConfiguration, 
                    HistoricDecisionExecutionDataManager historicDecisionExecutionDataManager) {
        
        super(dmnEngineConfiguration, historicDecisionExecutionDataManager);
    }
    
    @Override
    public void deleteHistoricDecisionExecutionsByDeploymentId(String deploymentId) {
        dataManager.deleteHistoricDecisionExecutionsByDeploymentId(deploymentId);
    }

    @Override
    public List<DmnHistoricDecisionExecution> findHistoricDecisionExecutionsByQueryCriteria(HistoricDecisionExecutionQueryImpl decisionExecutionQuery) {
        return dataManager.findHistoricDecisionExecutionsByQueryCriteria(decisionExecutionQuery);
    }

    @Override
    public long findHistoricDecisionExecutionCountByQueryCriteria(HistoricDecisionExecutionQueryImpl decisionExecutionQuery) {
        return dataManager.findHistoricDecisionExecutionCountByQueryCriteria(decisionExecutionQuery);
    }

    @Override
    public List<DmnHistoricDecisionExecution> findHistoricDecisionExecutionsByNativeQuery(Map<String, Object> parameterMap) {
        return dataManager.findHistoricDecisionExecutionsByNativeQuery(parameterMap);
    }

    @Override
    public long findHistoricDecisionExecutionCountByNativeQuery(Map<String, Object> parameterMap) {
        return dataManager.findHistoricDecisionExecutionCountByNativeQuery(parameterMap);
    }

    @Override
    public void delete(HistoricDecisionExecutionQueryImpl query) {
        dataManager.delete(query);
    }
}
