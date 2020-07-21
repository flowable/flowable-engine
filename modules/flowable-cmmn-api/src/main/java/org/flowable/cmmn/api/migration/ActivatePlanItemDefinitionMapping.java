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

import java.util.LinkedHashMap;
import java.util.Map;

public class ActivatePlanItemDefinitionMapping extends PlanItemDefinitionMapping {

    protected String newAssignee;
    protected Map<String, Object> withLocalVariables = new LinkedHashMap<>();

    public ActivatePlanItemDefinitionMapping(String planItemDefinitionId, String newAssignee, Map<String, Object> withLocalVariables) {
        super(planItemDefinitionId);
        this.newAssignee = newAssignee;
        this.withLocalVariables = withLocalVariables;
    }
    
    public ActivatePlanItemDefinitionMapping(String planItemDefinitionId) {
        super(planItemDefinitionId);
    }

    public String getNewAssignee() {
        return newAssignee;
    }

    public void setNewAssignee(String newAssignee) {
        this.newAssignee = newAssignee;
    }

    public Map<String, Object> getWithLocalVariables() {
        return withLocalVariables;
    }

    public void setWithLocalVariables(Map<String, Object> withLocalVariables) {
        this.withLocalVariables = withLocalVariables;
    }
}
