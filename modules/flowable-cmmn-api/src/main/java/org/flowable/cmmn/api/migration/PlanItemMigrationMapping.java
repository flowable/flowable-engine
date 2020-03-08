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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Valentin Zickner
 */
public abstract class PlanItemMigrationMapping {

    public abstract List<String> getFromPlanItemIds();

    public abstract List<String> getToPlanItemIds();

    public static PlanItemMigrationMapping.OneToOneMapping createMappingFor(String fromPlanItemId, String toPlanItemId) {
        return new OneToOneMapping(fromPlanItemId, toPlanItemId);
    }

    public static PlanItemMigrationMapping.ManyToOneMapping createMappingFor(List<String> fromPlanItemIds, String toPlanItemId) {
        return new ManyToOneMapping(fromPlanItemIds, toPlanItemId);
    }

    public static class OneToOneMapping extends PlanItemMigrationMapping implements PlanItemMigrationMappingOptions.SingleToPlanItemOptions<OneToOneMapping> {

        public String fromPlanItemId;
        public String toPlanItemId;
        protected String withNewAssignee;
        protected Map<String, Object> withLocalVariables = new LinkedHashMap<>();

        public OneToOneMapping(String fromPlanItemId, String toPlanItemId) {
            this.fromPlanItemId = fromPlanItemId;
            this.toPlanItemId = toPlanItemId;
        }


        @Override
        public List<String> getFromPlanItemIds() {
            return Collections.singletonList(fromPlanItemId);
        }

        public String getFromPlanItemId() {
            return fromPlanItemId;
        }

        @Override
        public List<String> getToPlanItemIds() {
            return Collections.singletonList(toPlanItemId);
        }

        public String getToPlanItemId() {
            return toPlanItemId;
        }

        @Override
        public OneToOneMapping withNewAssignee(String newAssigneeId) {
            this.withNewAssignee = newAssigneeId;
            return this;
        }

        @Override
        public String getWithNewAssignee() {
            return this.withNewAssignee;
        }

        @Override
        public OneToOneMapping withLocalVariable(String variableName, Object variableValue) {
            this.withLocalVariables.put(variableName, variableValue);
            return this;
        }

        @Override
        public OneToOneMapping withLocalVariables(Map<String, Object> variables) {
            this.withLocalVariables.putAll(variables);
            return this;
        }

        @Override
        public Map<String, Object> getPlanItemLocalVariables() {
            return this.withLocalVariables;
        }
    }

    public static class ManyToOneMapping extends PlanItemMigrationMapping implements PlanItemMigrationMappingOptions.SingleToPlanItemOptions<ManyToOneMapping> {

        public List<String> fromPlanItemIds;
        public String toPlanItemId;
        protected String withNewAssignee;
        protected Map<String, Object> withLocalVariables = new LinkedHashMap<>();

        public ManyToOneMapping(List<String> fromPlanItemIds, String toPlanItemId) {
            this.fromPlanItemIds = fromPlanItemIds;
            this.toPlanItemId = toPlanItemId;
        }

        @Override
        public List<String> getFromPlanItemIds() {
            return this.fromPlanItemIds;
        }

        @Override
        public List<String> getToPlanItemIds() {
            return Collections.singletonList(this.toPlanItemId);
        }

        public String getToPlanItemId() {
            return toPlanItemId;
        }

        @Override
        public ManyToOneMapping withNewAssignee(String newAssigneeId) {
            this.withNewAssignee = newAssigneeId;
            return this;
        }

        @Override
        public String getWithNewAssignee() {
            return this.withNewAssignee;
        }

        @Override
        public ManyToOneMapping withLocalVariable(String variableName, Object variableValue) {
            this.withLocalVariables.put(variableName, variableValue);
            return this;
        }

        @Override
        public ManyToOneMapping withLocalVariables(Map<String, Object> variables) {
            this.withLocalVariables.putAll(variables);
            return this;
        }

        @Override
        public Map<String, Object> getPlanItemLocalVariables() {
            return this.withLocalVariables;
        }
    }

}
