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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * @author Dennis
 */
@JsonDeserialize(using = ProcessInstanceActivityMigrationMapping.ProcessInstanceActivityMigrationMappingDeSerializer.class)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public abstract class ProcessInstanceActivityMigrationMapping {

    protected String withNewAssignee;

    public abstract List<String> getFromActivityIds();

    public abstract List<String> getToActivityIds();

    public String getWithNewAssignee() {
        return withNewAssignee;
    }

    public static ProcessInstanceActivityMigrationMapping.OneToOneMapping createMappingFor(String fromActivityId, String toActivityId) {
        return new OneToOneMapping(fromActivityId, toActivityId);
    }

    public static ProcessInstanceActivityMigrationMapping.OneToManyMapping createMappingFor(String fromActivityId, List<String> toActivityIds) {
        return new OneToManyMapping(fromActivityId, toActivityIds);
    }

    public static ProcessInstanceActivityMigrationMapping.ManyToOneMapping createMappingFor(List<String> fromActivityIds, String toActivityId) {
        return new ManyToOneMapping(fromActivityIds, toActivityId);
    }

    @JsonDeserialize(as = OneToOneMapping.class)
    public static class OneToOneMapping extends ProcessInstanceActivityMigrationMapping implements ProcessInstanceActivityMigrationMappingOptions.SingleToActivityOptions<OneToOneMapping> {

        public String fromActivityId;
        public String toActivityId;
        @JsonSerialize
        @JsonDeserialize
        protected Map<String, Object> withLocalVariables = new LinkedHashMap<>();

        @JsonCreator
        public OneToOneMapping(@JsonProperty("fromActivityId") String fromActivityId, @JsonProperty("toActivityId") String toActivityId) {
            this.fromActivityId = fromActivityId;
            this.toActivityId = toActivityId;
        }

        @Override
        @JsonIgnore
        public List<String> getFromActivityIds() {
            ArrayList<String> list = new ArrayList<>();
            list.add(fromActivityId);
            return list;
        }

        @Override
        @JsonIgnore
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
        public OneToOneMapping withNewAssignee(String newAssigneeId) {
            this.withNewAssignee = newAssigneeId;
            return this;
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
        @JsonIgnore
        public Map<String, Object> getActivityLocalVariables() {
            return withLocalVariables;
        }

        @Override
        public String toString() {
            return "OneToOneMapping{" + "fromActivityId='" + fromActivityId + '\'' + ", toActivityId='" + toActivityId + '\'' + '}';
        }
    }

    @JsonDeserialize(as = OneToManyMapping.class)
    public static class OneToManyMapping extends ProcessInstanceActivityMigrationMapping implements ProcessInstanceActivityMigrationMappingOptions.MultipleToActivityOptions<OneToManyMapping> {

        public String fromActivityId;
        public List<String> toActivityIds;
        @JsonSerialize
        @JsonDeserialize
        protected Map<String, Map<String, Object>> withLocalVariables = new LinkedHashMap<>();

        @JsonCreator
        public OneToManyMapping(@JsonProperty("fromActivityId") String fromActivityId, @JsonProperty("toActivityIds") List<String> toActivityIds) {
            this.fromActivityId = fromActivityId;
            this.toActivityIds = toActivityIds;
        }

        @Override
        @JsonIgnore
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
        public OneToManyMapping withNewAssignee(String newAssigneeId) {
            this.withNewAssignee = newAssigneeId;
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
        @JsonIgnore
        public Map<String, Map<String, Object>> getActivitiesLocalVariables() {
            return withLocalVariables;
        }

        @Override
        public String toString() {
            return "OneToManyMapping{" + "fromActivityId='" + fromActivityId + '\'' + ", toActivityIds=" + toActivityIds + '}';
        }
    }

    @JsonDeserialize(as = ManyToOneMapping.class)
    public static class ManyToOneMapping extends ProcessInstanceActivityMigrationMapping implements ProcessInstanceActivityMigrationMappingOptions.SingleToActivityOptions<ManyToOneMapping> {

        public List<String> fromActivityIds;
        public String toActivityId;
        @JsonSerialize
        @JsonDeserialize
        protected Map<String, Object> withLocalVariables = new LinkedHashMap<>();

        @JsonCreator
        public ManyToOneMapping(@JsonProperty("fromActivityIds") List<String> fromActivityIds, @JsonProperty("toActivityId") String toActivityId) {
            this.fromActivityIds = fromActivityIds;
            this.toActivityId = toActivityId;
        }

        @Override
        public List<String> getFromActivityIds() {
            return new ArrayList<>(fromActivityIds);
        }

        @Override
        @JsonIgnore
        public List<String> getToActivityIds() {
            ArrayList<String> list = new ArrayList<>();
            list.add(toActivityId);
            return list;
        }

        public String getToActivityId() {
            return toActivityId;
        }

        @Override
        public ManyToOneMapping withNewAssignee(String newAssigneeId) {
            this.withNewAssignee = newAssigneeId;
            return this;
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
        @JsonIgnore
        public Map<String, Object> getActivityLocalVariables() {
            return withLocalVariables;
        }

        @Override
        public String toString() {
            return "ManyToOneMapping{" + "fromActivityIds=" + fromActivityIds + ", toActivityId='" + toActivityId + '\'' + '}';
        }
    }

    public static class ProcessInstanceActivityMigrationMappingDeSerializer extends JsonDeserializer<ProcessInstanceActivityMigrationMapping> {

        protected Predicate<JsonNode> isNotNullNode = jsonNode -> jsonNode != null && !jsonNode.isNull();
        protected Predicate<JsonNode> isSingleTextValue = jsonNode -> isNotNullNode.test(jsonNode) && jsonNode.isTextual();
        protected Predicate<JsonNode> isMultiValue = jsonNode -> isNotNullNode.test(jsonNode) && jsonNode.isArray();

        @Override
        public ProcessInstanceActivityMigrationMapping deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JsonProcessingException {

            ObjectMapper mapper = (ObjectMapper) jsonParser.getCodec();
            ObjectNode rootNode = mapper.readTree(jsonParser);

            Class<? extends ProcessInstanceActivityMigrationMapping> mappingClass = null;
            if (isMultiValue.test(rootNode.get("fromActivityIds")) && isSingleTextValue.test(rootNode.get("toActivityId"))) {
                mappingClass = ManyToOneMapping.class;
            }
            if (isSingleTextValue.test(rootNode.get("toActivityId")) && isSingleTextValue.test(rootNode.get("fromActivityId"))) {
                mappingClass = OneToOneMapping.class;
            }
            if (isMultiValue.test(rootNode.get("toActivityIds")) && isSingleTextValue.test(rootNode.get("fromActivityId"))) {
                mappingClass = OneToManyMapping.class;
            }

            return mapper.treeToValue(rootNode, mappingClass);
        }
    }
}