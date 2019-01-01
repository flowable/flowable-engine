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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flowable.cmmn.api.repository.CaseDefinition;
import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.flowable.cmmn.engine.impl.persistence.entity.PlanItemInstanceEntity;

public class CaseInstanceChangeState {

    protected String caseInstanceId;
    protected CaseDefinition caseDefinitionToMigrateTo;
    protected Map<String, Object> caseVariables = new HashMap<>();
    protected Map<String, List<PlanItemInstance>> currentStageInstances;
    protected List<MovePlanItemInstanceEntityContainer> movePlanItemInstanceEntityContainers;
    protected HashMap<String, PlanItemInstanceEntity> createdStageInstances = new HashMap<>();

    public CaseInstanceChangeState() {
    }

    public String getCaseInstanceId() {
        return caseInstanceId;
    }

    public CaseInstanceChangeState setCaseInstanceId(String caseInstanceId) {
        this.caseInstanceId = caseInstanceId;
        return this;
    }

    public CaseDefinition getCaseDefinitionToMigrateTo() {
        return caseDefinitionToMigrateTo;
    }

    public CaseInstanceChangeState setCaseDefinitionToMigrateTo(CaseDefinition caseDefinitionToMigrateTo) {
        this.caseDefinitionToMigrateTo = caseDefinitionToMigrateTo;
        return this;
    }

    public Map<String, Object> getCaseVariables() {
        return caseVariables;
    }

    public CaseInstanceChangeState setCaseVariables(Map<String, Object> caseVariables) {
        this.caseVariables = caseVariables;
        return this;
    }

    public Map<String, List<PlanItemInstance>> getCurrentStageInstances() {
        return currentStageInstances;
    }

    public CaseInstanceChangeState setCurrentStageInstances(Map<String, List<PlanItemInstance>> currentStageInstances) {
        this.currentStageInstances = currentStageInstances;
        return this;
    }

    public List<MovePlanItemInstanceEntityContainer> getMovePlanItemInstanceEntityContainers() {
        return movePlanItemInstanceEntityContainers;
    }

    public CaseInstanceChangeState setMovePlanItemInstanceEntityContainers(List<MovePlanItemInstanceEntityContainer> movePlanItemInstanceEntityContainers) {
        this.movePlanItemInstanceEntityContainers = movePlanItemInstanceEntityContainers;
        return this;
    }

    public HashMap<String, PlanItemInstanceEntity> getCreatedStageInstances() {
        return createdStageInstances;
    }

    public CaseInstanceChangeState setCreatedStageInstances(HashMap<String, PlanItemInstanceEntity> createdStageInstances) {
        this.createdStageInstances = createdStageInstances;
        return this;
    }
    
    public void addCreatedStageInstance(String key, PlanItemInstanceEntity planItemInstance) {
        this.createdStageInstances.put(key, planItemInstance);
    }
}
