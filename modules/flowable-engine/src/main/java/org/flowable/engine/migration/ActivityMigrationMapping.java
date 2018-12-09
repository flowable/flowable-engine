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
package org.flowable.engine.migration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Dennis
 */
public abstract class ActivityMigrationMapping {

    protected String toCallActivityId;
    protected Integer callActivityProcessDefinitionVersion;
    protected String fromCallActivityId;

    public abstract List<String> getFromActivityIds();

    public abstract List<String> getToActivityIds();

    public boolean isToParentProcess() {
        return this.fromCallActivityId != null;
    }

    public boolean isToCallActivity() {
        return this.toCallActivityId != null;
    }

    public String getToCallActivityId() {
        return toCallActivityId;
    }

    public Integer getCallActivityProcessDefinitionVersion() {
        return callActivityProcessDefinitionVersion;
    }

    public String getFromCallActivityId() {
        return fromCallActivityId;
    }

    public static ActivityMigrationMapping.OneToOneMapping createMappingFor(String fromActivityId, String toActivityId) {
        return new OneToOneMapping(fromActivityId, toActivityId);
    }

    public static ActivityMigrationMapping.OneToManyMapping createMappingFor(String fromActivityId, List<String> toActivityIds) {
        return new OneToManyMapping(fromActivityId, toActivityIds);
    }

    public static ActivityMigrationMapping.ManyToOneMapping createMappingFor(List<String> fromActivityIds, String toActivityId) {
        return new ManyToOneMapping(fromActivityIds, toActivityId);
    }

    public static class OneToOneMapping extends ActivityMigrationMapping implements ActivityMigrationMappingOptions.SingleToActivityOptions<OneToOneMapping> {

        public String fromActivityId;
        public String toActivityId;
        protected String withNewAssignee;
        protected Map<String, Object> withLocalVariables = new LinkedHashMap<>();

        public OneToOneMapping(String fromActivityId, String toActivityId) {
            this.fromActivityId = fromActivityId;
            this.toActivityId = toActivityId;
        }

        @Override
        public List<String> getFromActivityIds() {
            ArrayList<String> list = new ArrayList<>();
            list.add(fromActivityId);
            return list;
        }

        @Override
        public List<String> getToActivityIds() {
            ArrayList<String> list = new ArrayList<>();
            list.add(toActivityId);
            return list;
        }

        public String getFromActivityId() {
            return fromActivityId;
        }

        public String getToActivityId() {
            return toActivityId;
        }

        @Override
        public OneToOneMapping inParentProcessOfCallActivityId(String fromCallActivityId) {
            this.fromCallActivityId = fromCallActivityId;
            this.toCallActivityId = null;
            this.callActivityProcessDefinitionVersion = null;
            return this;
        }

        @Override
        public OneToOneMapping inSubProcessOfCallActivityId(String toCallActivityId) {
            this.toCallActivityId = toCallActivityId;
            this.callActivityProcessDefinitionVersion = null;
            this.fromCallActivityId = null;
            return this;
        }

        @Override
        public OneToOneMapping inSubProcessOfCallActivityId(String toCallActivityId, int subProcessDefVersion) {
            this.toCallActivityId = toCallActivityId;
            this.callActivityProcessDefinitionVersion = subProcessDefVersion;
            this.fromCallActivityId = null;
            return this;
        }

        @Override
        public OneToOneMapping withNewAssignee(String newAssigneeId) {
            this.withNewAssignee = newAssigneeId;
            return this;
        }

        @Override
        public String getWithNewAssignee() {
            return withNewAssignee;
        }

        @Override
        public OneToOneMapping withLocalVariable(String variableName, Object variableValue) {
            withLocalVariables.put(variableName, variableValue);
            return this;
        }

        @Override
        public OneToOneMapping withLocalVariables(Map<String, Object> variables) {
            withLocalVariables.putAll(variables);
            return this;
        }

        @Override
        public Map<String, Object> getActivityLocalVariables() {
            return withLocalVariables;
        }

        @Override
        public String toString() {
            return "OneToOneMapping{" + "fromActivityId='" + fromActivityId + '\'' + ", toActivityId='" + toActivityId + '\'' + '}';
        }
    }

    public static class OneToManyMapping extends ActivityMigrationMapping implements ActivityMigrationMappingOptions.MultipleToActivityOptions<OneToManyMapping> {

        public String fromActivityId;
        public List<String> toActivityIds;
        protected Map<String, Map<String, Object>> withLocalVariables = new LinkedHashMap<>();

        public OneToManyMapping(String fromActivityId, List<String> toActivityIds) {
            this.fromActivityId = fromActivityId;
            this.toActivityIds = toActivityIds;
        }

        @Override
        public List<String> getFromActivityIds() {
            ArrayList<String> list = new ArrayList<>();
            list.add(fromActivityId);
            return list;
        }

        @Override
        public List<String> getToActivityIds() {
            return new ArrayList<>(toActivityIds);
        }

        public String getFromActivityId() {
            return fromActivityId;
        }

        @Override
        public OneToManyMapping inParentProcessOfCallActivityId(String fromCallActivityId) {
            this.fromCallActivityId = fromCallActivityId;
            this.toCallActivityId = null;
            this.callActivityProcessDefinitionVersion = null;
            return this;
        }

        @Override
        public OneToManyMapping inSubProcessOfCallActivityId(String toCallActivityId) {
            this.toCallActivityId = toCallActivityId;
            this.callActivityProcessDefinitionVersion = null;
            this.fromCallActivityId = null;
            return this;
        }

        @Override
        public OneToManyMapping inSubProcessOfCallActivityId(String toCallActivityId, int subProcessDefVersion) {
            this.toCallActivityId = toCallActivityId;
            this.callActivityProcessDefinitionVersion = subProcessDefVersion;
            this.fromCallActivityId = null;
            return this;
        }

        @Override
        public OneToManyMapping withLocalVariableForActivity(String toActivity, String variableName, Object variableValue) {
            Map<String, Object> activityVariables = withLocalVariables.computeIfAbsent(toActivity, key -> new HashMap<>());
            activityVariables.put(variableName, variableValue);
            return this;
        }

        @Override
        public OneToManyMapping withLocalVariablesForActivity(String toActivity, Map<String, Object> variables) {
            Map<String, Object> activityVariables = withLocalVariables.computeIfAbsent(toActivity, key -> new HashMap<>());
            activityVariables.putAll(variables);
            return this;
        }

        @Override
        public OneToManyMapping withLocalVariableForAllActivities(String variableName, Object variableValue) {
            toActivityIds.forEach(id -> withLocalVariableForActivity(id, variableName, variableValue));
            return this;
        }

        @Override
        public OneToManyMapping withLocalVariablesForAllActivities(Map<String, Object> variables) {
            toActivityIds.forEach(id -> withLocalVariablesForActivity(id, variables));
            return this;
        }

        @Override
        public OneToManyMapping withLocalVariables(Map<String, Map<String, Object>> mappingVariables) {
            withLocalVariables.putAll(mappingVariables);
            return this;
        }

        @Override
        public Map<String, Map<String, Object>> getActivitiesLocalVariables() {
            return withLocalVariables;
        }

        @Override
        public String toString() {
            return "OneToManyMapping{" + "fromActivityId='" + fromActivityId + '\'' + ", toActivityIds=" + toActivityIds + '}';
        }
    }

    public static class ManyToOneMapping extends ActivityMigrationMapping implements ActivityMigrationMappingOptions.SingleToActivityOptions<ManyToOneMapping> {

        public List<String> fromActivityIds;
        public String toActivityId;
        protected String withNewAssignee;
        protected Map<String, Object> withLocalVariables = new LinkedHashMap<>();

        public ManyToOneMapping(List<String> fromActivityIds, String toActivityId) {
            this.fromActivityIds = fromActivityIds;
            this.toActivityId = toActivityId;
        }

        @Override
        public List<String> getFromActivityIds() {
            return new ArrayList<>(fromActivityIds);
        }

        @Override
        public List<String> getToActivityIds() {
            ArrayList<String> list = new ArrayList<>();
            list.add(toActivityId);
            return list;
        }

        public String getToActivityId() {
            return toActivityId;
        }

        @Override
        public ManyToOneMapping inParentProcessOfCallActivityId(String fromCallActivityId) {
            this.fromCallActivityId = fromCallActivityId;
            this.toCallActivityId = null;
            this.callActivityProcessDefinitionVersion = null;
            return this;
        }

        @Override
        public ManyToOneMapping inSubProcessOfCallActivityId(String toCallActivityId) {
            this.toCallActivityId = toCallActivityId;
            this.callActivityProcessDefinitionVersion = null;
            this.fromCallActivityId = null;
            return this;
        }

        @Override
        public ManyToOneMapping inSubProcessOfCallActivityId(String toCallActivityId, int subProcessDefVersion) {
            this.toCallActivityId = toCallActivityId;
            this.callActivityProcessDefinitionVersion = subProcessDefVersion;
            this.fromCallActivityId = null;
            return this;
        }

        @Override
        public ManyToOneMapping withNewAssignee(String newAssigneeId) {
            this.withNewAssignee = newAssigneeId;
            return this;
        }

        @Override
        public String getWithNewAssignee() {
            return withNewAssignee;
        }

        @Override
        public ManyToOneMapping withLocalVariable(String variableName, Object variableValue) {
            withLocalVariables.put(variableName, variableValue);
            return this;
        }

        @Override
        public ManyToOneMapping withLocalVariables(Map<String, Object> variables) {
            withLocalVariables.putAll(variables);
            return this;
        }

        @Override
        public Map<String, Object> getActivityLocalVariables() {
            return withLocalVariables;
        }

        @Override
        public String toString() {
            return "ManyToOneMapping{" + "fromActivityIds=" + fromActivityIds + ", toActivityId='" + toActivityId + '\'' + '}';
        }
    }

}