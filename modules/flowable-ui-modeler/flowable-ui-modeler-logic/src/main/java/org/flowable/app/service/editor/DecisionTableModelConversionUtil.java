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
package org.flowable.app.service.editor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.lang3.StringUtils;
import org.flowable.app.domain.editor.Model;
import org.flowable.app.service.exception.InternalServerErrorException;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @author Yvo Swillens
 */
public class DecisionTableModelConversionUtil {

    protected static ObjectMapper objectMapper = new ObjectMapper();

    public static Model convertModel(Model decisionTableModel) {

        if (StringUtils.isNotEmpty(decisionTableModel.getModelEditorJson())) {
            try {
                JsonNode decisionTableNode = objectMapper.readTree(decisionTableModel.getModelEditorJson());

                if (decisionTableNode.get("modelVersion") == null || decisionTableNode.get("modelVersion").isNull()) {
                    // split input rule nodes into operator and expression nodes
                    //
                    // determine input node ids
                    JsonNode inputExpressionNodes = decisionTableNode.get("inputExpressions");
                    List<String> inputExpressionIds = new ArrayList<>();

                    if (inputExpressionNodes != null && !inputExpressionNodes.isNull()) {
                        for (JsonNode inputExpressionNode : inputExpressionNodes) {
                            if (inputExpressionNode.get("id") != null && !inputExpressionNode.get("id").isNull()) {
                                String inputId = inputExpressionNode.get("id").asText();
                                inputExpressionIds.add(inputId);
                            }
                        }
                    }

                    // split input rule nodes
                    JsonNode ruleNodes = decisionTableNode.get("rules");
                    ArrayNode newRuleNodes = objectMapper.createArrayNode();

                    if (ruleNodes != null && !ruleNodes.isNull()) {
                        for (JsonNode ruleNode : ruleNodes) {
                            ObjectNode newRuleNode = objectMapper.createObjectNode();

                            for (String inputExpressionId : inputExpressionIds) {
                                if (ruleNode.has(inputExpressionId)) {
                                    String operatorId = inputExpressionId + "_operator";
                                    String expressionId = inputExpressionId + "_expression";
                                    String operatorValue = null;
                                    String expressionValue = null;

                                    if (ruleNode.get(inputExpressionId) != null && !ruleNode.get(inputExpressionId).isNull()) {
                                        String oldExpression = ruleNode.get(inputExpressionId).asText();

                                        if (StringUtils.isNotEmpty(oldExpression)) {
                                            operatorValue = oldExpression.substring(0, oldExpression.indexOf(' '));
                                            expressionValue = oldExpression.substring(oldExpression.indexOf(' ') + 1);

                                            // remove outer escape quotes
                                            if (expressionValue.startsWith("\"") && expressionValue.endsWith("\"")) {
                                                expressionValue = expressionValue.substring(1, expressionValue.length() - 1);
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
                                String outputExpressionId = ruleProperty.next();
                                if (!inputExpressionIds.contains(outputExpressionId)) { // is output expression
                                    String outputExpressionValue = ruleNode.get(outputExpressionId).asText();
                                    // remove outer escape quotes
                                    if (StringUtils.isNotEmpty(outputExpressionValue) && outputExpressionValue.startsWith("\"") && outputExpressionValue.endsWith("\"")) {
                                        outputExpressionValue = outputExpressionValue.substring(1, outputExpressionValue.length() - 1);
                                    }
                                    newRuleNode.put(outputExpressionId, outputExpressionValue);
                                }
                            }

                            newRuleNodes.add(newRuleNode);
                        }

                        // replace rules node
                        ObjectNode decisionTableObjectNode = (ObjectNode) decisionTableNode;
                        decisionTableObjectNode.replace("rules", newRuleNodes);

                        // replace editor json
                        decisionTableModel.setModelEditorJson(decisionTableObjectNode.toString());
                    }
                }
            } catch (Exception e) {
                throw new InternalServerErrorException(String.format("Error converting decision table %s to new model version", decisionTableModel.getName()));
            }
        }

        return decisionTableModel;
    }
}
