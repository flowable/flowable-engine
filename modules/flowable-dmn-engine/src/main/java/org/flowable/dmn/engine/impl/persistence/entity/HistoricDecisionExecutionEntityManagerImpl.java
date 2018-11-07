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

import org.flowable.common.engine.impl.persistence.entity.data.DataManager;
import org.flowable.dmn.api.DmnHistoricDecisionExecution;
import org.flowable.dmn.engine.DmnEngineConfiguration;
import org.flowable.dmn.engine.impl.HistoricDecisionExecutionQueryImpl;
import org.flowable.dmn.engine.impl.persistence.entity.data.HistoricDecisionExecutionDataManager;

/**
 * @author Tijs Rademakers
 * @author Joram Barrez
 */
public class HistoricDecisionExecutionEntityManagerImpl extends AbstractEntityManager<HistoricDecisionExecutionEntity> implements HistoricDecisionExecutionEntityManager {

    protected HistoricDecisionExecutionDataManager historicDecisionExecutionDataManager;

    public HistoricDecisionExecutionEntityManagerImpl(DmnEngineConfiguration dmnEngineConfiguration, 
                    HistoricDecisionExecutionDataManager historicDecisionExecutionDataManager) {
        
        super(dmnEngineConfiguration);
        this.historicDecisionExecutionDataManager = historicDecisionExecutionDataManager;
    }

    @Override
    protected DataManager<HistoricDecisionExecutionEntity> getDataManager() {
        return historicDecisionExecutionDataManager;
    }
    
    @Override
    public void deleteHistoricDecisionExecutionsByDeploymentId(String deploymentId) {
        historicDecisionExecutionDataManager.deleteHistoricDecisionExecutionsByDeploymentId(deploymentId);
    }

    @Override
    public List<DmnHistoricDecisionExecution> findHistoricDecisionExecutionsByQueryCriteria(HistoricDecisionExecutionQueryImpl decisionExecutionQuery) {
        return historicDecisionExecutionDataManager.findHistoricDecisionExecutionsByQueryCriteria(decisionExecutionQuery);
    }

    @Override
    public long findHistoricDecisionExecutionCountByQueryCriteria(HistoricDecisionExecutionQueryImpl decisionExecutionQuery) {
        return historicDecisionExecutionDataManager.findHistoricDecisionExecutionCountByQueryCriteria(decisionExecutionQuery);
    }

    @Override
    public List<DmnHistoricDecisionExecution> findHistoricDecisionExecutionsByNativeQuery(Map<String, Object> parameterMap) {
        return historicDecisionExecutionDataManager.findHistoricDecisionExecutionsByNativeQuery(parameterMap);
    }

    @Override
    public long findHistoricDecisionExecutionCountByNativeQuery(Map<String, Object> parameterMap) {
        return historicDecisionExecutionDataManager.findHistoricDecisionExecutionCountByNativeQuery(parameterMap);
    }

    public HistoricDecisionExecutionDataManager getHistoricDecisionExecutionDataManager() {
        return historicDecisionExecutionDataManager;
    }

    public void setHistoricDecisionExecutionDataManager(HistoricDecisionExecutionDataManager historicDecisionExecutionDataManager) {
        this.historicDecisionExecutionDataManager = historicDecisionExecutionDataManager;
    }

}
