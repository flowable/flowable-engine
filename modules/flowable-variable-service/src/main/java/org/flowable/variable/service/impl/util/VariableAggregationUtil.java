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
package org.flowable.variable.service.impl.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.variable.api.delegate.VariableScope;
import org.flowable.variable.api.persistence.entity.VariableInstance;
import org.flowable.variable.api.types.VariableType;
import org.flowable.variable.api.types.VariableTypes;
import org.flowable.variable.service.VariableService;
import org.flowable.variable.service.VariableServiceConfiguration;
import org.flowable.variable.service.impl.aggregation.VariableAggregation;
import org.flowable.variable.service.impl.aggregation.VariableAggregationInfo;
import org.flowable.variable.service.impl.persistence.entity.VariableInstanceEntity;
import org.flowable.variable.service.impl.types.BooleanType;
import org.flowable.variable.service.impl.types.DoubleType;
import org.flowable.variable.service.impl.types.IntegerType;
import org.flowable.variable.service.impl.types.JsonType;
import org.flowable.variable.service.impl.types.LongStringType;
import org.flowable.variable.service.impl.types.LongType;
import org.flowable.variable.service.impl.types.NullType;
import org.flowable.variable.service.impl.types.ShortType;
import org.flowable.variable.service.impl.types.StringType;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * @author Joram Barrez
 */
public class VariableAggregationUtil {

    public static void copyCreatedVariableForAggregation(VariableAggregationInfo variableAggregationInfo, VariableInstance variableInstance, VariableServiceConfiguration variableServiceConfiguration) {

        Optional<VariableAggregation> matchingVariableAggregation = variableAggregationInfo.getVariableAggregations()
            .stream()
            .filter(variableAggregation -> variableInstance.getName().equals(variableAggregation.getSource()))
            .findFirst();
        if (matchingVariableAggregation.isPresent()) {

                VariableService variableService = variableServiceConfiguration.getVariableService();
                VariableTypes variableTypes = variableServiceConfiguration.getVariableTypes();
                VariableType jsonVariableType = variableTypes.getVariableType(JsonType.TYPE_NAME);

            // Copy of variable should not be associated with original instance nor with the current executionId/scopeId,
            // as these variables should not be returned in the regular variable queries.
            //
            // The subScopeId needs to be set to the actual scope that does the variable aggregation,
            // because the variables need to be deleted when the instance would be deleted without doing the aggregation.
            VariableInstanceEntity variableInstanceCopy = variableService
                .createVariableInstance(variableInstance.getName(), jsonVariableType);
            variableInstanceCopy.setScopeId(variableAggregationInfo.getInstanceId());
            variableInstanceCopy.setSubScopeId(variableAggregationInfo.getBeforeAggregationScopeId());
            variableInstanceCopy.setScopeType(ScopeTypes.VARIABLE_AGGREGATION);

            // The variable value is stored as an ObjectNode instead of the actual value.
            // The reason for this is:
            // - it gets stored as json on the actual gathering anyway
            // - this way extra metadata can be stored
            ObjectMapper objectMapper = variableServiceConfiguration.getObjectMapper();
            ObjectNode objectNode = objectMapper.createObjectNode();

            objectNode.put("type", variableInstance.getTypeName());

            if (StringType.TYPE_NAME.equals(variableInstance.getTypeName()) || LongStringType.TYPE_NAME.equals(variableInstance.getTypeName())) {
                objectNode.put("value", (String) variableInstance.getValue());
            } else if (BooleanType.TYPE_NAME.equals(variableInstance.getTypeName())) {
                objectNode.put("value", (Boolean) variableInstance.getValue());
            } else if (ShortType.TYPE_NAME.equals(variableInstance.getTypeName())) {
                objectNode.put("value", (Short) variableInstance.getValue());
            } else if (IntegerType.TYPE_NAME.equals(variableInstance.getTypeName())) {
                objectNode.put("value", (Integer) variableInstance.getValue());
            } else if (LongType.TYPE_NAME.equals(variableInstance.getTypeName())) {
                objectNode.put("value", (Long) variableInstance.getValue());
            } else if (DoubleType.TYPE_NAME.equals(variableInstance.getTypeName())) {
                objectNode.put("value", (Double) variableInstance.getValue());
            } else if (ShortType.TYPE_NAME.equals(variableInstance.getTypeName())) {
                objectNode.put("value", (String) variableInstance.getValue());
            } else if (NullType.TYPE_NAME.equals(variableInstance.getTypeName())) {
                objectNode.putNull("value");
            }

            /*

            TODO: other types.
            TODO: or move toJson to VariabeType interface? Or always conversion from/to string? Add serialization to variabletype?

             variableTypes.addType(new NullType());
            variableTypes.addType(new DateType());
            variableTypes.addType(new InstantType());
            variableTypes.addType(new LocalDateType());
            variableTypes.addType(new LocalDateTimeType());
            variableTypes.addType(new JodaDateType());
            variableTypes.addType(new JodaDateTimeType());
            variableTypes.addType(new UUIDType());
            variableTypes.addType(new JsonType(getMaxLengthString(), objectMapper, jsonVariableTypeTrackObjects));
            // longJsonType only needed for reading purposes
            variableTypes.addType(JsonType.longJsonType(getMaxLengthString(), objectMapper, jsonVariableTypeTrackObjects));
            variableTypes.addType(new AggregatedVariableType());
            variableTypes.addType(new ByteArrayType());
            variableTypes.addType(new SerializableType(serializableVariableTypeTrackDeserializedObjects));
            if (customPostVariableTypes != null) {
                for (VariableType customVariableType : customPostVariableTypes) {
                    variableTypes.addType(customVariableType);
                }
            }
             */

            variableInstanceCopy.setValue(objectNode);

            variableService.insertVariableInstance(variableInstanceCopy);
        }

    }

    public static void aggregateVariablesForOneInstance(VariableAggregationInfo variableAggregationInfo, List<VariableInstanceEntity> variableInstances, VariableServiceConfiguration variableServiceConfiguration) {

        ObjectMapper objectMapper = variableServiceConfiguration.getObjectMapper();
        VariableService variableService = variableServiceConfiguration.getVariableService();

        Map<String, ObjectNode> aggregatedVariables = new HashMap<>();

        for (VariableInstanceEntity variableInstance : variableInstances) {

            ObjectNode variableValue = (ObjectNode) variableInstance.getValue();

            List<VariableAggregation> matchingVariableAggregations = variableAggregationInfo.getVariableAggregations().stream()
                .filter(variableAggregation -> variableAggregation.getSource().equals(variableInstance.getName()))
                .collect(Collectors.toList());

            // The value is set to what is defined in the target of the aggregation definition
            for (VariableAggregation matchingVariableAggregation : matchingVariableAggregations) {

                String targetArrayVariableName = matchingVariableAggregation.getTargetArrayVariable();
                ObjectNode variableObjectNode = aggregatedVariables.get(targetArrayVariableName);
                if (variableObjectNode == null) {
                    variableObjectNode = objectMapper.createObjectNode();
                    variableObjectNode.put("timestamp", variableServiceConfiguration.getClock().getCurrentTime().getTime());

                    // TODO: expressions for target array gets re-evaluated now ... this is potentially wrong vs the moment of gathering the variable ...
                    // This might be ok when the moment of aggregation is set/documented to the moment of completion?
                    aggregatedVariables.put(targetArrayVariableName, variableObjectNode);
                }

                variableObjectNode.set(matchingVariableAggregation.getTarget(), variableValue.get("value"));
            }

            // After aggregation, the original variable isn't needed anymore
            variableService.deleteVariableInstance(variableInstance);
        }

        VariableTypes variableTypes = variableServiceConfiguration.getVariableTypes();
        VariableType jsonVariableType = variableTypes.getVariableType(JsonType.TYPE_NAME);

        for (String aggregatedVariableName : aggregatedVariables.keySet()) {
            VariableInstanceEntity aggregatedObjectNodeVariable = variableService
                .createVariableInstance(aggregatedVariableName, jsonVariableType);
            aggregatedObjectNodeVariable.setScopeType(ScopeTypes.VARIABLE_AGGREGATION);
            aggregatedObjectNodeVariable.setScopeId(variableAggregationInfo.getInstanceId());
            aggregatedObjectNodeVariable.setSubScopeId(variableAggregationInfo.getAggregationScopeId());
            aggregatedObjectNodeVariable.setValue(aggregatedVariables.get(aggregatedVariableName));
            variableService.insertVariableInstance(aggregatedObjectNodeVariable);
        }

    }

    public static void aggregateVariablesOfAllInstances(VariableScope variableScope, List<VariableInstanceEntity> variableInstances, VariableServiceConfiguration variableServiceConfiguration) {

        ObjectMapper objectMapper = variableServiceConfiguration.getObjectMapper();
        Map<String, ArrayNode> arrayVariables = new HashMap<>();
        for (VariableInstanceEntity variableInstance : variableInstances) {

            String targetArrayVariableName = variableInstance.getName(); // name was set before to the target array variable name
            ArrayNode arrayNodeVariable = arrayVariables.get(targetArrayVariableName);
            if (arrayNodeVariable == null) {
                arrayNodeVariable = objectMapper.createArrayNode();
                arrayVariables.put(targetArrayVariableName, arrayNodeVariable);
            }

            // TODO: validate if it's actually an objectnode
            arrayNodeVariable.add((ObjectNode) variableInstance.getValue());

            variableServiceConfiguration.getVariableService().deleteVariableInstance(variableInstance);
        }

        // Sort objectNodes and remove metadata
        // TODO: sort is only really needed for sequential MO
        for (String arrayVariableName : arrayVariables.keySet()) {
            ArrayNode arrayVariable = arrayVariables.get(arrayVariableName);

            List<ObjectNode> list = new ArrayList<>(arrayVariable.size());
            for (JsonNode jsonNode : arrayVariable) {
                // The copied objectNode is a deepCopy. This is needed because below the metadata (e.g. timestamp) is removed.
                // As the objectNode is a variable too, this would trigger a variable update if the same instance is used.
                list.add((ObjectNode) jsonNode.deepCopy());
            }

            // Sort
            list.sort((jsonNode1, jsonNode2) -> {
                Long timestamp1 = jsonNode1.get("timestamp").asLong();
                Long timestamp2 = jsonNode2.get("timestamp").asLong();
                return timestamp1.compareTo(timestamp2);
            });

            // Add back to original list and remove metadata
            arrayVariable.removeAll();
            for (ObjectNode objectNode : list) {
                objectNode.remove("timestamp");

                arrayVariable.add(objectNode);
            }

            variableScope.setVariable(arrayVariableName, arrayVariable); // calling setVariable (instead of variableService#insert will make sure history is created correctly)
        }

    }

}
