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
package org.flowable.engine.impl.delegate;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.flowable.bpmn.model.VariableAggregationDefinition;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.variable.api.persistence.entity.VariableInstance;
import org.flowable.variable.api.types.VariableType;
import org.flowable.variable.api.types.VariableTypes;
import org.flowable.variable.service.VariableService;
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
 * @author Filip Hrisafov
 */
public class JsonVariableAggregator implements VariableAggregator {

    protected final ProcessEngineConfigurationImpl processEngineConfiguration;

    public JsonVariableAggregator(ProcessEngineConfigurationImpl processEngineConfiguration) {
        this.processEngineConfiguration = processEngineConfiguration;
    }

    @Override
    public Object aggregateSingle(DelegateExecution execution, VariableAggregationDefinition aggregation) {
        ObjectNode objectNode = processEngineConfiguration.getObjectMapper().createObjectNode();

        VariableService variableService = processEngineConfiguration.getVariableServiceConfiguration().getVariableService();
        VariableTypes variableTypes = processEngineConfiguration.getVariableServiceConfiguration().getVariableTypes();

        for (VariableAggregationDefinition.Variable definition : aggregation.getDefinitions()) {
            String targetVarName = null;
            if (StringUtils.isNotEmpty(definition.getTargetExpression())) {
                Object value = processEngineConfiguration.getExpressionManager()
                        .createExpression(definition.getTargetExpression())
                        .getValue(execution);
                if (value != null) {
                    targetVarName = value.toString();
                }
            } else if (StringUtils.isNotEmpty(definition.getTarget())) {
                targetVarName = definition.getTarget();
            } else if (StringUtils.isNotEmpty(definition.getSource())) {
                targetVarName = definition.getSource();
            }

            if (targetVarName != null) {
                VariableInstance varInstance = null;
                if (StringUtils.isNotEmpty(definition.getSource())) {
                    varInstance = execution.getVariableInstanceLocal(definition.getSource());

                } else if (StringUtils.isNotEmpty(definition.getSourceExpression())) {
                    Object sourceValue = processEngineConfiguration.getExpressionManager()
                            .createExpression(definition.getSourceExpression())
                            .getValue(execution);
                    VariableType variableType = variableTypes.findVariableType(sourceValue);
                    // This is a fake variable instance so we can get the type of it
                    varInstance = variableService.createVariableInstance(targetVarName, variableType, sourceValue);
                }

                if (varInstance != null) {

                    if (StringType.TYPE_NAME.equals(varInstance.getTypeName()) || LongStringType.TYPE_NAME.equals(varInstance.getTypeName())) {
                        objectNode.put(targetVarName, (String) varInstance.getValue());
                    } else if (JsonType.TYPE_NAME.equals(varInstance.getTypeName())) {
                        objectNode.set(targetVarName, (JsonNode) varInstance.getValue());
                    } else if (BooleanType.TYPE_NAME.equals(varInstance.getTypeName())) {
                        objectNode.put(targetVarName, (Boolean) varInstance.getValue());
                    } else if (ShortType.TYPE_NAME.equals(varInstance.getTypeName())) {
                        objectNode.put(targetVarName, (Short) varInstance.getValue());
                    } else if (IntegerType.TYPE_NAME.equals(varInstance.getTypeName())) {
                        objectNode.put(targetVarName, (Integer) varInstance.getValue());
                    } else if (LongType.TYPE_NAME.equals(varInstance.getTypeName())) {
                        objectNode.put(targetVarName, (Long) varInstance.getValue());
                    } else if (DoubleType.TYPE_NAME.equals(varInstance.getTypeName())) {
                        objectNode.put(targetVarName, (Double) varInstance.getValue());
                    } else if (ShortType.TYPE_NAME.equals(varInstance.getTypeName())) {
                        objectNode.put(targetVarName, (String) varInstance.getValue());
                    } else if (NullType.TYPE_NAME.equals(varInstance.getTypeName())) {
                        objectNode.putNull(targetVarName);
                    }

                    //TODO other types

                    //TODO what if we support collection of content items, data objects etc.
                    // i.e. collection of the var instance type, for unknown types.
                }

            }
        }

        return objectNode;
    }

    @Override
    public Object aggregateMulti(DelegateExecution execution, List<? extends VariableInstance> instances, VariableAggregationDefinition aggregation) {
        ObjectMapper objectMapper = processEngineConfiguration.getObjectMapper();
        ArrayNode arrayNode = objectMapper.createArrayNode();
        for (VariableInstance instance : instances) {
            arrayNode.add((JsonNode) instance.getValue());
        }

        return arrayNode;
    }
}
