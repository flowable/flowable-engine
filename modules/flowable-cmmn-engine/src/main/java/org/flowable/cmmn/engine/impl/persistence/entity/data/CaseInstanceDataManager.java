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
package org.flowable.cmmn.engine.impl.persistence.entity.data;

import java.util.Date;
import java.util.List;
import java.util.Map;

import org.flowable.cmmn.api.history.HistoricCaseInstance;
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.engine.impl.persistence.entity.CaseInstanceEntity;
import org.flowable.cmmn.engine.impl.runtime.CaseInstanceQueryImpl;
import org.flowable.common.engine.impl.persistence.entity.data.DataManager;
import org.flowable.variable.service.impl.persistence.entity.VariableInstanceEntity;

/**
 * @author Joram Barrez
 */
public interface CaseInstanceDataManager extends DataManager<CaseInstanceEntity> {

    CaseInstanceEntity create(HistoricCaseInstance historicCaseInstance, Map<String, VariableInstanceEntity> variables);

    List<CaseInstanceEntity> findCaseInstancesByCaseDefinitionId(String caseDefinitionId);

    CaseInstanceEntity findCaseInstanceEntityEagerFetchPlanItemInstances(String caseInstanceId, String planItemInstanceId);

    List<CaseInstance> findByCriteria(CaseInstanceQueryImpl query);

    List<CaseInstance> findWithVariablesByCriteria(CaseInstanceQueryImpl query);

    long countByCriteria(CaseInstanceQueryImpl query);

    void updateLockTime(String caseInstanceId, Date lockDate, String lockOwner, Date expirationTime);

    void clearLockTime(String caseInstanceId);

    void clearAllLockTimes(String lockOwner);
}
