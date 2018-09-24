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
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
        if ((decisionTableNode.get("modelVersion") == null || decisionTableNode.get("modelVersion").isNull()) && decisionTableNode.has("name")) {
            String modelName = decisionTableNode.get("name").asText();
            LOGGER.info("Decision table model with name " + modelName + " found with version < v2; migrating to v3");

            ObjectNode decisionTableObjectNode = (ObjectNode) decisionTableNode;
            decisionTableObjectNode.put("modelVersion", "3");

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
                            String operatorValue = null;
                            String expressionValue = null;

                            if (ruleNode.get(inputExpressionId) != null && !ruleNode.get(inputExpressionId).isNull()) {
                                String oldExpression = ruleNode.get(inputExpressionId).asText();

                                if (StringUtils.isNotEmpty(oldExpression)) {
                                    if (oldExpression.indexOf(' ') != -1) {
                                        operatorValue = oldExpression.substring(0, oldExpression.indexOf(' '));
                                        expressionValue = oldExpression.substring(oldExpression.indexOf(' ') + 1);
                                    } else { // no prefixed operator
                                        expressionValue = oldExpression;
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

                                    // determine type is null
                                    if (StringUtils.isEmpty(inputExpressionIds.get(inputExpressionId))) {
                                        String expressionType = determineExpressionType(expressionValue);
                                        inputExpressionIds.put(inputExpressionId, expressionType);
                                    }
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
                        }
                    }

                    Iterator<String> ruleProperty = ruleNode.fieldNames();
                    while (ruleProperty.hasNext()) {
                        String expressionId = ruleProperty.next();
                        if (!inputExpressionIds.containsKey(expressionId)) {
                            if (ruleNode.hasNonNull(expressionId)) {
                                String expressionValue = ruleNode.get(expressionId).asText();

                                // remove outer escape quotes
                                if (StringUtils.isNotEmpty(expressionValue) && expressionValue.startsWith("\"") && expressionValue
                                    .endsWith("\"")) {
                                    expressionValue = expressionValue.substring(1, expressionValue.length() - 1);
                                }

                                // if build in date function
                                if (expressionValue.startsWith("fn_date(")) {
                                    expressionValue = expressionValue.substring(9, expressionValue.lastIndexOf('\''));

                                } else if (expressionValue.startsWith("date:toDate(")) {
                                    expressionValue = expressionValue.substring(13, expressionValue.lastIndexOf('\''));
                                }

                                newRuleNode.put(expressionId, expressionValue);
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
                decisionTableObjectNode.replace("rules", newRuleNodes);
            }

            LOGGER.info("Decision table model " + modelName + " migrated to v2");
        }

        return decisionTableNode;
    }

    public static JsonNode migrateModelV3(JsonNode decisionTableNode, ObjectMapper objectMapper) {
        // migrate to v2
        decisionTableNode = migrateModel(decisionTableNode, objectMapper);

        // migrate to v3
        if (decisionTableNode.has("modelVersion") && decisionTableNode.get("modelVersion").asText().equals("2") && decisionTableNode.has("name")) {
            String modelName = decisionTableNode.get("name").asText();
            LOGGER.info("Decision table model " + modelName + " found with version v2; migrating to v3");

            ObjectNode decisionTableObjectNode = (ObjectNode) decisionTableNode;
            decisionTableObjectNode.put("modelVersion", "3");

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

            // split input rule nodes
            JsonNode ruleNodes = decisionTableNode.get("rules");

            if (ruleNodes != null && !ruleNodes.isNull()) {
                for (JsonNode ruleNode : ruleNodes) {

                    for (String inputExpressionId : inputExpressionIds.keySet()) {

                        // get operator
                        String inputExpressionOperatorId = inputExpressionId + "_operator";

                        if (ruleNode.has(inputExpressionOperatorId)) {
                            if (ruleNode.get(inputExpressionOperatorId) != null && !ruleNode.get(inputExpressionOperatorId).isNull()) {
                                String oldInputExpressionOperatorValue = ruleNode.get(inputExpressionOperatorId).asText();
                                String inputType = inputExpressionIds.get(inputExpressionId);
                                try {
                                    String newInputExpressionOperatorValue = transformCollectionOperation(oldInputExpressionOperatorValue, inputType);

                                    // replace operator value
                                    ((ObjectNode) ruleNode).put(inputExpressionOperatorId, newInputExpressionOperatorValue);
                                } catch (IllegalArgumentException iae) {
                                    LOGGER.warn("Skipping model migration; could not transform collection operator for model name: " + modelName, iae);
                                }
                            }
                        }
                    }
                }
            }

            LOGGER.info("Decision table model " + modelName + " migrated to v3");
        }

        return decisionTableNode;
    }

    protected static String transformCollectionOperation(String operatorValue, String inputType) {
        if (StringUtils.isEmpty(operatorValue) || StringUtils.isEmpty(inputType)) {
            throw new IllegalArgumentException("operator value and input type must be present");
        }

        if ("collection".equalsIgnoreCase(inputType)) {
            switch (operatorValue) {
                case "IN":
                    return "ALL OF";
                case "NOT IN":
                    return "NONE OF";
                case "ANY":
                    return "ANY OF";
                case "NOT ANY":
                    return "NOT ALL OF";
                default:
                    return operatorValue;
            }
        } else {
            switch (operatorValue) {
                case "IN":
                    return "IS IN";
                case "NOT IN":
                    return "IS NOT IN";
                case "ANY":
                    return "IS IN";
                case "NOT ANY":
                    return "IS NOT IN";
                default:
                    return operatorValue;
            }
        }
    }

    public static String determineExpressionType(String expressionValue) {
        String expressionType = null;
        if (!"-".equals(expressionValue)) {
            expressionType = "string";
            if (NumberUtils.isCreatable(expressionValue)) {
                expressionType = "number";
            } else {
                try {
                    new SimpleDateFormat("yyyy-MM-dd").parse(expressionValue);
                    expressionType = "date";
                } catch (ParseException pe) {
                    if ("true".equalsIgnoreCase(expressionValue) || "false".equalsIgnoreCase(expressionType)) {
                        expressionType = "boolean";
                    }
                }
            }
        }
        return expressionType;
    }

    public static String formatCollectionExpression(String containsOperator, String inputVariable, String expressionValue) {
        String containsPrefixAndMethod = getDMNContainsExpressionMethod(containsOperator);
        StringBuilder stringBuilder = new StringBuilder();

        if (containsPrefixAndMethod != null) {
            stringBuilder.append("${");
            stringBuilder.append(containsPrefixAndMethod);
            stringBuilder.append("(");
            stringBuilder.append(formatCollectionExpressionValue(inputVariable));
            stringBuilder.append(", ");

            String formattedExpressionValue = formatCollectionExpressionValue(expressionValue);
            stringBuilder.append(formattedExpressionValue);

            stringBuilder.append(")}");
        } else {
            stringBuilder.append(containsOperator);
            stringBuilder.append(" ");
            stringBuilder.append(formatCollectionExpressionValue(expressionValue));
        }

        return stringBuilder.toString();
    }

    public static boolean isCollectionOperator(String operator) {
        return "IN".equals(operator) || "NOT IN".equals(operator) || "ANY".equals(operator) || "NOT ANY".equals(operator) ||
            "IS IN".equals(operator) || "IS NOT IN".equals(operator) ||
            "ALL OF".equals(operator) || "NONE OF".equals(operator) || "NOT ALL OF".equals(operator) || "ALL OF".equals(operator);
    }

    protected static String getDMNContainsExpressionMethod(String containsOperator) {
        if (StringUtils.isEmpty(containsOperator)) {
            throw new IllegalArgumentException("containsOperator must be provided");
        }

        String containsPrefixAndMethod;

        switch (containsOperator) {
            case "IS IN":
            case "ALL OF":
            case "IN":
                containsPrefixAndMethod = "collection:allOf";
                break;
            case "IS NOT IN":
            case "NONE OF":
            case "NOT IN":
                containsPrefixAndMethod = "collection:noneOf";
                break;
            case "ANY OF":
            case "ANY":
                containsPrefixAndMethod = "collection:anyOf";
                break;
            case "NOT ALL OF":
            case "NOT ANY":
                containsPrefixAndMethod = "collection:notAllOf";
                break;
            default:
                containsPrefixAndMethod = null;
        }

        return containsPrefixAndMethod;
    }

    protected static String formatCollectionExpressionValue(String expressionValue) {
        if (StringUtils.isEmpty(expressionValue)) {
            return "\"\"";
        }

        StringBuilder formattedExpressionValue = new StringBuilder();

        // if multiple values
        if (expressionValue.contains(",")) {
            formattedExpressionValue.append("'");

            List<String> formattedValues = split(expressionValue);
            formattedExpressionValue.append(StringUtils.join(formattedValues, ','));
        } else {
            String formattedValue = expressionValue;
            formattedExpressionValue.append(formattedValue);
        }

        // if multiple values
        if (expressionValue.contains(",")) {
            formattedExpressionValue.append("'");
        }

        return formattedExpressionValue.toString();
    }

    protected static List<String> split(String str) {
        String regex;
        if (str.contains("\"")) {
            // only split on comma between matching quotes
            regex = ",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)";
        } else {
            regex = ",";
        }
        return Stream.of(str.split(regex))
            .map(elem -> elem.trim())
            .collect(Collectors.toList());
    }
}
