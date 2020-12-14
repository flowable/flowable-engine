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

import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.flowable.bpmn.model.VariableAggregationDefinition;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.variable.VariableAggregator;
import org.flowable.engine.delegate.variable.VariableAggregatorContext;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.variable.api.persistence.entity.VariableInstance;
import org.flowable.variable.api.types.VariableType;
import org.flowable.variable.api.types.VariableTypes;
import org.flowable.variable.service.VariableService;
import org.flowable.variable.service.impl.types.BooleanType;
import org.flowable.variable.service.impl.types.ByteArrayType;
import org.flowable.variable.service.impl.types.DateType;
import org.flowable.variable.service.impl.types.DoubleType;
import org.flowable.variable.service.impl.types.InstantType;
import org.flowable.variable.service.impl.types.IntegerType;
import org.flowable.variable.service.impl.types.JodaDateTimeType;
import org.flowable.variable.service.impl.types.JodaDateType;
import org.flowable.variable.service.impl.types.JsonType;
import org.flowable.variable.service.impl.types.LocalDateTimeType;
import org.flowable.variable.service.impl.types.LocalDateType;
import org.flowable.variable.service.impl.types.LongStringType;
import org.flowable.variable.service.impl.types.LongType;
import org.flowable.variable.service.impl.types.NullType;
import org.flowable.variable.service.impl.types.ShortType;
import org.flowable.variable.service.impl.types.StringType;
import org.flowable.variable.service.impl.types.UUIDType;

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
    public Object aggregateSingleVariable(DelegateExecution execution, VariableAggregatorContext context) {
        ObjectNode objectNode = processEngineConfiguration.getObjectMapper().createObjectNode();

        VariableService variableService = processEngineConfiguration.getVariableServiceConfiguration().getVariableService();
        VariableTypes variableTypes = processEngineConfiguration.getVariableServiceConfiguration().getVariableTypes();

        for (VariableAggregationDefinition.Variable definition : context.getDefinition().getDefinitions()) {
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
                    varInstance = execution.getVariableInstance(definition.getSource());

                } else if (StringUtils.isNotEmpty(definition.getSourceExpression())) {
                    Object sourceValue = processEngineConfiguration.getExpressionManager()
                            .createExpression(definition.getSourceExpression())
                            .getValue(execution);
                    VariableType variableType = variableTypes.findVariableType(sourceValue);
                    // This is a fake variable instance so we can get the type of it
                    varInstance = variableService.createVariableInstance(targetVarName, variableType, sourceValue);
                }

                if (varInstance != null) {

                    String varInstanceTypeName = varInstance.getTypeName();

                    switch (varInstanceTypeName) {
                        case StringType.TYPE_NAME:
                        case LongStringType.TYPE_NAME:
                            objectNode.put(targetVarName, (String) varInstance.getValue());
                            break;
                        case JsonType.TYPE_NAME:
                            objectNode.set(targetVarName, (JsonNode) varInstance.getValue());
                            break;
                        case BooleanType.TYPE_NAME:
                            objectNode.put(targetVarName, (Boolean) varInstance.getValue());
                            break;
                        case ShortType.TYPE_NAME:
                            objectNode.put(targetVarName, (Short) varInstance.getValue());
                            break;
                        case IntegerType.TYPE_NAME:
                            objectNode.put(targetVarName, (Integer) varInstance.getValue());
                            break;
                        case LongType.TYPE_NAME:
                            objectNode.put(targetVarName, (Long) varInstance.getValue());
                            break;
                        case DoubleType.TYPE_NAME:
                            objectNode.put(targetVarName, (Double) varInstance.getValue());
                            break;
                        case DateType.TYPE_NAME:
                            objectNode.put(targetVarName, ((Date) varInstance.getValue()).toInstant().toString());
                            break;
                        case NullType.TYPE_NAME:
                            objectNode.putNull(targetVarName);
                            break;
                        case InstantType.TYPE_NAME:
                        case LocalDateType.TYPE_NAME:
                        case LocalDateTimeType.TYPE_NAME:
                        case JodaDateType.TYPE_NAME:
                        case JodaDateTimeType.TYPE_NAME:
                        case UUIDType.TYPE_NAME:
                            // For all these types it is OK to use toString as their string representation is what we want to have
                            objectNode.put(targetVarName, varInstance.getValue().toString());
                            break;
                        case ByteArrayType.TYPE_NAME:
                            objectNode.put(targetVarName, (byte[]) varInstance.getValue());
                            break;
                        default:
                            if (VariableAggregatorContext.OVERVIEW.equals(context.getState())) {
                                // We can only use the aggregated variable if we are in an overview state
                                Object value = varInstance.getValue();
                                if (value instanceof JsonNode) {
                                    objectNode.set(targetVarName, (JsonNode) value);
                                } else {
                                    throw new FlowableException("Cannot aggregate overview variable: " + varInstance);
                                }
                            } else {
                                throw new FlowableException("Cannot aggregate variable: " + varInstance);
                            }
                    }
                }

            }
        }

        return objectNode;
    }

    @Override
    public Object aggregateMultiVariables(DelegateExecution execution, List<? extends VariableInstance> instances, VariableAggregatorContext context) {
        ObjectMapper objectMapper = processEngineConfiguration.getObjectMapper();
        ArrayNode arrayNode = objectMapper.createArrayNode();
        for (VariableInstance instance : instances) {
            arrayNode.add((JsonNode) instance.getValue());
        }

        return arrayNode;
    }
}
