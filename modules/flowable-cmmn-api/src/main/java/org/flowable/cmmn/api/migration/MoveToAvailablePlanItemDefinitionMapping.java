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

public class MoveToAvailablePlanItemDefinitionMapping extends PlanItemDefinitionMapping {
    
    protected Map<String, Object> withLocalVariables = new LinkedHashMap<>();

    public MoveToAvailablePlanItemDefinitionMapping(String planItemDefinitionId) {
        super(planItemDefinitionId);
    }
    
    public MoveToAvailablePlanItemDefinitionMapping(String planItemDefinitionId, Map<String, Object> withLocalVariables) {
        super(planItemDefinitionId);
        this.withLocalVariables = withLocalVariables;
    }

    public MoveToAvailablePlanItemDefinitionMapping(String planItemDefinitionId, String condition) {
        super(planItemDefinitionId, condition);
    }

    public Map<String, Object> getWithLocalVariables() {
        return withLocalVariables;
    }

    public void setWithLocalVariables(Map<String, Object> withLocalVariables) {
        this.withLocalVariables = withLocalVariables;
    }
}
