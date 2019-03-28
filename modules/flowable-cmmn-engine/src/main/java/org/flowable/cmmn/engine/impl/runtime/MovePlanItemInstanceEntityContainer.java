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
package org.flowable.cmmn.engine.impl.runtime;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flowable.cmmn.engine.impl.persistence.entity.PlanItemInstanceEntity;
import org.flowable.cmmn.model.CmmnModel;
import org.flowable.cmmn.model.PlanItem;

public class MovePlanItemInstanceEntityContainer {

    protected List<PlanItemInstanceEntity> planItemInstances;
    protected String caseInstanceId;
    protected String caseDefinitionId;
    protected String tenantId;
    protected List<String> moveToPlanItemDefinitionIds;
    protected CmmnModel cmmnModel;
    protected String newAssigneeId;
    protected Map<String, PlanItemInstanceEntity> continueParentPlanItemInstanceMap = new HashMap<>();
    protected Map<String, PlanItemMoveEntry> moveToPlanItemMap = new HashMap<>();

    public MovePlanItemInstanceEntityContainer(List<PlanItemInstanceEntity> planItemInstances, List<String> moveToPlanItemDefinitionIds) {
        this.planItemInstances = planItemInstances;
        this.moveToPlanItemDefinitionIds = moveToPlanItemDefinitionIds;
    }

    public void addMoveToPlanItem(String planItemId, PlanItemMoveEntry planItemMoveEntry) {
        moveToPlanItemMap.put(planItemId, planItemMoveEntry);
    }

    public void addMoveToPlanItem(String planItemId, PlanItem originalPlanItem, PlanItem newPlanItem) {
        moveToPlanItemMap.put(planItemId, new PlanItemMoveEntry(originalPlanItem, newPlanItem));
    }

    public void addMoveToPlanItemDefinition(String planItemId, PlanItem originalPlanItem) {
        moveToPlanItemMap.put(planItemId, new PlanItemMoveEntry(originalPlanItem, originalPlanItem));
    }

    public List<PlanItemInstanceEntity> getPlanItemInstances() {
        return planItemInstances;
    }

    public void setPlanItemInstances(List<PlanItemInstanceEntity> planItemInstances) {
        this.planItemInstances = planItemInstances;
    }

    public String getCaseInstanceId() {
        return caseInstanceId;
    }

    public void setCaseInstanceId(String caseInstanceId) {
        this.caseInstanceId = caseInstanceId;
    }

    public String getCaseDefinitionId() {
        return caseDefinitionId;
    }

    public void setCaseDefinitionId(String caseDefinitionId) {
        this.caseDefinitionId = caseDefinitionId;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public List<String> getMoveToPlanItemDefinitionIds() {
        return moveToPlanItemDefinitionIds;
    }

    public void setMoveToPlanItemDefinitionIds(List<String> moveToPlanItemDefinitionIds) {
        this.moveToPlanItemDefinitionIds = moveToPlanItemDefinitionIds;
    }

    public CmmnModel getCmmnModel() {
        return cmmnModel;
    }

    public void setCmmnModel(CmmnModel cmmnModel) {
        this.cmmnModel = cmmnModel;
    }

    public String getNewAssigneeId() {
        return newAssigneeId;
    }

    public void setNewAssigneeId(String newAssigneeId) {
        this.newAssigneeId = newAssigneeId;
    }

    public Map<String, PlanItemInstanceEntity> getContinueParentPlanItemInstanceMap() {
        return continueParentPlanItemInstanceMap;
    }

    public void setContinueParentPlanItemInstanceMap(Map<String, PlanItemInstanceEntity> continueParentPlanItemInstanceMap) {
        this.continueParentPlanItemInstanceMap = continueParentPlanItemInstanceMap;
    }
    
    public void addContinueParentPlanItemInstance(String planItemInstanceId, PlanItemInstanceEntity continueParentPlanItemInstance) {
        continueParentPlanItemInstanceMap.put(planItemInstanceId, continueParentPlanItemInstance);
    }
    
    public PlanItemInstanceEntity getContinueParentPlanItemInstance(String planItemInstanceId) {
        return continueParentPlanItemInstanceMap.get(planItemInstanceId);
    }

    public Map<String, PlanItemMoveEntry> getMoveToPlanItemMap() {
        return moveToPlanItemMap;
    }

    public void setMoveToPlanItemMap(Map<String, PlanItemMoveEntry> moveToPlanItemMap) {
        this.moveToPlanItemMap = moveToPlanItemMap;
    }

    public PlanItemMoveEntry getMoveToPlanItem(String planItemId) {
        return moveToPlanItemMap.get(planItemId);
    }

    public List<PlanItemMoveEntry> getMoveToPlanItems() {
        return new ArrayList<>(moveToPlanItemMap.values());
    }

    public static class PlanItemMoveEntry {

        protected PlanItem originalPlanItem;
        protected PlanItem newPlanItem;

        public PlanItemMoveEntry(PlanItem originalPlanItem, PlanItem newPlanItem) {
            this.originalPlanItem = originalPlanItem;
            this.newPlanItem = newPlanItem;
        }

        public PlanItem getOriginalPlanItem() {
            return originalPlanItem;
        }
        
        public PlanItem getNewPlanItem() {
            return newPlanItem;
        }
    }
}
