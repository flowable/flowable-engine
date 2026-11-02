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
package org.flowable.cmmn.api.migration;

public class ChangePlanItemDefinitionWithNewTargetIdsMapping {
    
    protected String existingPlanItemDefinitionId;
    protected String newPlanItemId;
    protected String newPlanItemDefinitionId;

    public ChangePlanItemDefinitionWithNewTargetIdsMapping(String existingPlanItemDefinitionId, String newPlanItemId, String newPlanItemDefinitionId) {
        this.existingPlanItemDefinitionId = existingPlanItemDefinitionId;
        this.newPlanItemId = newPlanItemId;
        this.newPlanItemDefinitionId = newPlanItemDefinitionId;
    }

    public String getExistingPlanItemDefinitionId() {
        return existingPlanItemDefinitionId;
    }

    public void setExistingPlanItemDefinitionId(String existingPlanItemDefinitionId) {
        this.existingPlanItemDefinitionId = existingPlanItemDefinitionId;
    }

    public String getNewPlanItemId() {
        return newPlanItemId;
    }

    public void setNewPlanItemId(String newPlanItemId) {
        this.newPlanItemId = newPlanItemId;
    }

    public String getNewPlanItemDefinitionId() {
        return newPlanItemDefinitionId;
    }

    public void setNewPlanItemDefinitionId(String newPlanItemDefinitionId) {
        this.newPlanItemDefinitionId = newPlanItemDefinitionId;
    }
}
