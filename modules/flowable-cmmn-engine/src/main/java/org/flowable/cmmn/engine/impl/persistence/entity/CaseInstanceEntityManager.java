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
package org.flowable.cmmn.engine.impl.persistence.entity;

import java.util.Date;
import java.util.List;
import java.util.Map;

import org.flowable.cmmn.api.history.HistoricCaseInstance;
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.api.runtime.CaseInstanceQuery;
import org.flowable.common.engine.impl.persistence.entity.EntityManager;
import org.flowable.variable.service.impl.persistence.entity.VariableInstanceEntity;

/**
 * @author Joram Barrez
 */
public interface CaseInstanceEntityManager extends EntityManager<CaseInstanceEntity> {

    CaseInstanceQuery createCaseInstanceQuery();

    CaseInstanceEntity create(HistoricCaseInstance historicCaseInstanceEntity, Map<String, VariableInstanceEntity> variables);

    List<CaseInstanceEntity> findCaseInstancesByCaseDefinitionId(String caseDefinitionId);

    List<CaseInstance> findByCriteria(CaseInstanceQuery query);

    List<CaseInstance> findWithVariablesByCriteria(CaseInstanceQuery query);

    long countByCriteria(CaseInstanceQuery query);

    void delete(String caseInstanceId, boolean cascade, String deleteReason);
    
    void updateCaseInstanceBusinessKey(CaseInstanceEntity caseInstanceEntity, String businessKey);
    
    void updateCaseInstanceBusinessStatus(CaseInstanceEntity caseInstanceEntity, String businessStatus);

    void updateLockTime(String caseInstanceId, String lockOwner, Date lockTime);

    void clearLockTime(String caseInstanceId);

    void clearAllLockTimes(String lockOwner);
}
