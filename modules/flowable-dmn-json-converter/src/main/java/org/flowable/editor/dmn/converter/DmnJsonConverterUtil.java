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
package org.flowable.editor.dmn.converter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Yvo Swillens
 */
public class DmnJsonConverterUtil {

    protected static final Logger LOGGER = LoggerFactory.getLogger(DmnJsonConverterUtil.class);

    public static String getValueAsString(String name, JsonNode objectNode) {
        String propertyValue = null;
        JsonNode jsonNode = objectNode.get(name);
        if (jsonNode != null && !jsonNode.isNull()) {
            propertyValue = jsonNode.asText();
        }
        return propertyValue;
    }

    public static JsonNode migrateModel(JsonNode decisionTableNode, ObjectMapper objectMapper) {

        // check if model is version 1
        if (decisionTableNode.get("modelVersion") == null || decisionTableNode.get("modelVersion").isNull()) {
            // split input rule nodes into operator and expression nodes
            //
            // determine input node ids
            JsonNode inputExpressionNodes = decisionTableNode.get("inputExpressions");
            Map<String, String> inputExpressionIds = new HashMap<>();

            if (inputExpressionNodes != null && !inputExpressionNodes.isNull()) {
                for (JsonNode inputExpressionNode : inputExpressionNodes) {
                    if (inputExpressionNode.get("id") != null && !inputExpressionNode.get("id").isNull()) {
                        String inputId = inputExpressionNode.get("id").asText();

                        String inputType = null;
                        if (inputExpressionNode.get("type") != null && !inputExpressionNode.get("type").isNull()) {
                            inputType = inputExpressionNode.get("type").asText();
                        }

                        inputExpressionIds.put(inputId, inputType);
                    }
                }
            }
            
            JsonNode outputExpressionNodes = decisionTableNode.get("outputExpressions");
            Map<String, String> outputExpressionIds = new HashMap<>();
            if (outputExpressionNodes != null && !outputExpressionNodes.isNull()) {
                for (JsonNode outputExpressionNode : outputExpressionNodes) {
                    if (outputExpressionNode.get("id") != null && !outputExpressionNode.get("id").isNull()) {
                        String outputId = outputExpressionNode.get("id").asText();

                        String outputType = null;
                        if (outputExpressionNode.get("type") != null && !outputExpressionNode.get("type").isNull()) {
                            outputType = outputExpressionNode.get("type").asText();
                        }

                        outputExpressionIds.put(outputId, outputType);
                    }
                }
            }
            // split input rule nodes
            JsonNode ruleNodes = decisionTableNode.get("rules");
            ArrayNode newRuleNodes = objectMapper.createArrayNode();

            if (ruleNodes != null && !ruleNodes.isNull()) {
                for (JsonNode ruleNode : ruleNodes) {
                    ObjectNode newRuleNode = objectMapper.createObjectNode();

                    for (String inputExpressionId : inputExpressionIds.keySet()) {
                        if (ruleNode.has(inputExpressionId)) {
                            String operatorId = inputExpressionId + "_operator";
                            String expressionId = inputExpressionId + "_expression";
                            String listOperatorId = inputExpressionId + "_list_operator";
                            String operatorValue = null;
                            String expressionValue = null;
                            String listOperatorValue = null;

                            if (ruleNode.get(inputExpressionId) != null && !ruleNode.get(inputExpressionId).isNull()) {
                                String oldExpression = ruleNode.get(inputExpressionId).asText();
                                if(inputExpressionIds.get(inputExpressionId).equals("list")) {
                                    if(oldExpression.startsWith(".containsString")) {
                                        listOperatorValue = "containsString";
                                        operatorValue = "==";
                                        expressionValue = oldExpression.substring(16, oldExpression.length() - 1);
                                    }else if(oldExpression.startsWith(".containsRegex")) {
                                        listOperatorValue = "containsString";
                                        operatorValue = "regex";
                                        expressionValue = oldExpression.substring(15, oldExpression.length() - 1);
                                    }else if(oldExpression.startsWith(".containsNumber") || oldExpression.startsWith(".containsDate")) {
                                        String methodName = "";
                                        if (oldExpression.startsWith(".containsNumber")) {
                                            listOperatorValue = "containsNumber";
                                            methodName = oldExpression.substring(16, oldExpression.indexOf('('));
                                            expressionValue = oldExpression.substring(oldExpression.indexOf('(') + 1, oldExpression.length() - 1);
                                        }else if (oldExpression.startsWith(".containsDate")) {
                                            listOperatorValue = "containsDate";
                                            methodName = oldExpression.substring(14, oldExpression.indexOf('('));
                                            expressionValue = oldExpression.substring(oldExpression.indexOf('(') + 1, oldExpression.length() - 1);
                                        }
                                        switch (methodName) {
                                        case "Equals":
                                            operatorValue = "==";
                                            break;
                                        case "NotEquals":
                                            operatorValue = "!=";
                                            break;
                                        case "LessThan":
                                            operatorValue = ">";
                                            break;
                                        case "GreaterThan":
                                            operatorValue = "<";
                                            break;
                                        case "LessThanOrEquals":
                                            operatorValue = ">=";
                                            break;
                                        case "GreaterThanOrEquals":
                                            operatorValue = "<=";
                                            break;
                                        }
                                    }else if(oldExpression.startsWith(".containsExpression")) {
                                        listOperatorValue = "containsExpression";
                                        operatorValue = "==";
                                        expressionValue = oldExpression.substring(19, oldExpression.length() - 1);
                                    }
                                }else if (StringUtils.isNotEmpty(oldExpression)) {
                                        if (oldExpression.indexOf(' ') != -1) {
                                            //set list operator value
                                            operatorValue = oldExpression.substring(0, oldExpression.indexOf(' '));
                                            expressionValue = oldExpression.substring(oldExpression.indexOf(' ') + 1);
                                        } else { // no prefixed operator
                                            expressionValue = oldExpression;
                                        }
    
                                        // determine type is null
                                        if (StringUtils.isEmpty(inputExpressionIds.get(inputExpressionId))) {
                                            String expressionType = determineExpressionType(expressionValue);
                                            inputExpressionIds.put(inputExpressionId, expressionType);
                                        }
                                    }
                                // remove outer escape quotes
                                if (expressionValue.startsWith("\"") && expressionValue.endsWith("\"")) {
                                    expressionValue = expressionValue.substring(1, expressionValue.length() - 1);
                                }

                                // if build in date function
                                if (expressionValue.startsWith("fn_date(")) {
                                    expressionValue = expressionValue.substring(9, expressionValue.lastIndexOf('\''));
                                    
                                } else if (expressionValue.startsWith("date:toDate(")) {
                                    expressionValue = expressionValue.substring(13, expressionValue.lastIndexOf('\''));
                                }
                            }

                            // add new operator kv
                            if (StringUtils.isNotEmpty(operatorValue)) {
                                newRuleNode.put(operatorId, operatorValue);
                            } else { // default value
                                newRuleNode.put(operatorId, "==");
                            }

                            // add new expression kv
                            if (StringUtils.isNotEmpty(expressionValue)) {
                                newRuleNode.put(expressionId, expressionValue);
                            } else { // default value
                                newRuleNode.put(expressionId, "-");
                            }
                            
                            if (StringUtils.isNotEmpty(listOperatorValue)) {
                                newRuleNode.put(listOperatorId, listOperatorValue);
                            } else { // default value
                                newRuleNode.put(listOperatorId, "containsString");
                            }
                        }
                    }

                    Iterator<String> ruleProperty = ruleNode.fieldNames();
                    while (ruleProperty.hasNext()) {
                        String outputExpressionId = ruleProperty.next();
                        if (!inputExpressionIds.containsKey(outputExpressionId)) { // is output expression
                            String outputExpressionValue = ruleNode.get(outputExpressionId).asText();
                            
                            String operatorId = outputExpressionId + "_operator";
                            String expressionId = outputExpressionId;
                            String listOperatorId = outputExpressionId + "_list_operator";
                            String operatorValue = null;
                            String expressionValue = null;
                            String listOperatorValue = null;

                            if(outputExpressionIds.get(outputExpressionId).equals("list")) {
                                outputExpressionValue = outputExpressionValue.substring(outputExpressionValue.indexOf('.'));
                                if(outputExpressionValue.startsWith(".append")) {
                                    listOperatorValue = "append";
                                    expressionValue = outputExpressionValue.substring(8, outputExpressionValue.length() - 1);
                                }else if(outputExpressionValue.startsWith(".remove")) {
                                    listOperatorValue = "remove";
                                    expressionValue = outputExpressionValue.substring(8, outputExpressionValue.length() - 1);
                                }else if(outputExpressionValue.startsWith(".clear")) {
                                    listOperatorValue = "clear";
                                    expressionValue = outputExpressionValue.substring(7, outputExpressionValue.length() - 1);
                                }
                            
                                operatorValue = determineExpressionType(expressionValue);

                            }else {
                                expressionValue = outputExpressionValue;
                            }
                            // remove outer escape quotes
                            if (StringUtils.isNotEmpty(expressionValue) && expressionValue.startsWith("\"") && expressionValue.endsWith("\"")) {
                                expressionValue = expressionValue.substring(1, expressionValue.length() - 1);
                            }

                            // if build in date function
                            if (expressionValue.startsWith("fn_date(")) {
                                expressionValue = expressionValue.substring(9, expressionValue.lastIndexOf('\''));
                                
                            } else if (expressionValue.startsWith("date:toDate(")) {
                                expressionValue = expressionValue.substring(13, expressionValue.lastIndexOf('\''));
                            }
                            
                            // add new operator kv
                            if (StringUtils.isNotEmpty(operatorValue)) {
                                newRuleNode.put(operatorId, operatorValue);
                            } else { // default value
                                newRuleNode.put(operatorId, "string");
                            }

                            // add new expression kv
                            if (StringUtils.isNotEmpty(expressionValue)) {
                                newRuleNode.put(expressionId, expressionValue);
                            } else { // default value
                                newRuleNode.put(expressionId, "-");
                            }
                            
                            if (StringUtils.isNotEmpty(listOperatorValue)) {
                                newRuleNode.put(listOperatorId, listOperatorValue);
                            } else { // default value
                                newRuleNode.put(listOperatorId, "append");
                            }
                        }
                    }

                    newRuleNodes.add(newRuleNode);
                }

                // set input expression nodes types
                if (inputExpressionNodes != null && !inputExpressionNodes.isNull()) {
                    for (JsonNode inputExpressionNode : inputExpressionNodes) {
                        if (inputExpressionNode.get("id") != null && !inputExpressionNode.get("id").isNull()) {
                            String inputId = inputExpressionNode.get("id").asText();
                            ((ObjectNode) inputExpressionNode).put("type", inputExpressionIds.get(inputId));
                        }
                    }
                }

                // replace rules node
                ObjectNode decisionTableObjectNode = (ObjectNode) decisionTableNode;
                decisionTableObjectNode.replace("rules", newRuleNodes);
            }
        }

        return decisionTableNode;
    }

    public static String determineExpressionType(String expressionValue) {
        String expressionType = null;
        if (!"-".equals(expressionValue)) {
            expressionType = "expression";
            if (NumberUtils.isNumber(expressionValue)) {
                expressionType = "number";
            } else {
                try {
                    new SimpleDateFormat("yyyy-MM-dd").parse(expressionValue);
                    expressionType = "date";
                } catch (ParseException pe) {
                    if ("true".equalsIgnoreCase(expressionValue) || "false".equalsIgnoreCase(expressionType)) {
                        expressionType = "boolean";
                    }
                    if(expressionValue.startsWith("\"")) {
                        expressionType = "string";
                    }
                }
            }
        }else {
            expressionType = "string";
        }
        return expressionType;
    }
}
