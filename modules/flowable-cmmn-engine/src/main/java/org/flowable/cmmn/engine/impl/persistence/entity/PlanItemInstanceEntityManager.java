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

import java.util.List;

import org.flowable.cmmn.api.history.HistoricPlanItemInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstanceQuery;
import org.flowable.cmmn.engine.impl.runtime.PlanItemInstanceQueryImpl;
import org.flowable.common.engine.impl.persistence.entity.EntityManager;
import org.flowable.task.service.impl.persistence.entity.TaskEntity;

/**
 * @author Joram Barrez
 */
public interface PlanItemInstanceEntityManager extends EntityManager<PlanItemInstanceEntity> {

    PlanItemInstanceEntity create(HistoricPlanItemInstance historicPlanItemInstance);

    /**
     * Returns a builder to create a new plan item instance.
     * @return the plan item instance builder
     */
    PlanItemInstanceEntityBuilder createPlanItemInstanceEntityBuilder();

    PlanItemInstanceQuery createPlanItemInstanceQuery();

    long countByCriteria(PlanItemInstanceQuery planItemInstanceQuery);
    
    List<PlanItemInstance> findByCriteria(PlanItemInstanceQuery planItemInstanceQuery);

    List<PlanItemInstance> findWithVariablesByCriteria(PlanItemInstanceQueryImpl planItemInstanceQuery);

    List<PlanItemInstanceEntity> findByCaseInstanceId(String caseInstanceId);

    List<PlanItemInstanceEntity> findByStagePlanItemInstanceId(String stagePlanItemInstanceId);
    
    List<PlanItemInstanceEntity> findByCaseInstanceIdAndPlanItemId(String caseInstanceId, String planItemId);

    List<PlanItemInstanceEntity> findByStageInstanceIdAndPlanItemId(String stageInstanceId, String planItemId);

    PlanItemInstanceEntity updateHumanTaskPlanItemInstanceAssignee(TaskEntity taskEntity, String assignee);

    PlanItemInstanceEntity updateHumanTaskPlanItemInstanceCompletedBy(TaskEntity taskEntity, String assignee);
    
    void updatePlanItemInstancesCaseDefinitionId(String caseInstanceId, String caseDefinitionId);

    void deleteSentryRelatedData(String planItemId);

    void deleteByCaseDefinitionId(String caseDefinitionId);

    void deleteByStageInstanceId(String stageInstanceId);

    void deleteByCaseInstanceId(String caseInstanceId);

}
