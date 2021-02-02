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
package org.flowable.cmmn.editor.json.converter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.flowable.cmmn.editor.constants.CmmnStencilConstants;
import org.flowable.cmmn.editor.constants.EditorJsonConstants;
import org.flowable.cmmn.editor.json.converter.CmmnJsonConverter.CmmnModelIdHelper;
import org.flowable.cmmn.editor.json.converter.util.CmmnModelJsonConverterUtil;
import org.flowable.cmmn.editor.json.converter.util.CollectionUtils;
import org.flowable.cmmn.model.Association;
import org.flowable.cmmn.model.BaseElement;
import org.flowable.cmmn.model.CmmnModel;
import org.flowable.cmmn.model.CompletionNeutralRule;
import org.flowable.cmmn.model.Criterion;
import org.flowable.cmmn.model.ExtensionAttribute;
import org.flowable.cmmn.model.ExtensionElement;
import org.flowable.cmmn.model.FieldExtension;
import org.flowable.cmmn.model.GraphicInfo;
import org.flowable.cmmn.model.ImplementationType;
import org.flowable.cmmn.model.ManualActivationRule;
import org.flowable.cmmn.model.PlanFragment;
import org.flowable.cmmn.model.PlanItem;
import org.flowable.cmmn.model.PlanItemControl;
import org.flowable.cmmn.model.PlanItemDefinition;
import org.flowable.cmmn.model.RepetitionRule;
import org.flowable.cmmn.model.RequiredRule;
import org.flowable.cmmn.model.ServiceTask;
import org.flowable.cmmn.model.Stage;
import org.flowable.cmmn.model.Task;
import org.flowable.cmmn.model.VariableAggregationDefinition;
import org.flowable.cmmn.model.VariableAggregationDefinitions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * @author Tijs Rademakers
 */
public abstract class BaseCmmnJsonConverter implements EditorJsonConstants, CmmnStencilConstants {

    protected static final Logger LOGGER = LoggerFactory.getLogger(BaseCmmnJsonConverter.class);

    public static final String NAMESPACE = "http://flowable.org/modeler";

    protected ObjectMapper objectMapper = new ObjectMapper();

    public void convertToJson(BaseElement baseElement, ActivityProcessor processor, CmmnModel model, PlanFragment planFragment,
            ArrayNode shapesArrayNode, CmmnJsonConverterContext converterContext, double subProcessX, double subProcessY) {

        if (!(baseElement instanceof PlanItem)) {
            return;
        }

        PlanItem planItem = (PlanItem) baseElement;
        PlanItemDefinition planItemDefinition = planItem.getPlanItemDefinition();

        GraphicInfo graphicInfo = model.getGraphicInfo(planItem.getId());

        String stencilId = getStencilId(baseElement);

        ObjectNode planItemNode = CmmnJsonConverterUtil.createChildShape(baseElement.getId(), stencilId, graphicInfo.getX() - subProcessX + graphicInfo.getWidth(),
                graphicInfo.getY() - subProcessY + graphicInfo.getHeight(), graphicInfo.getX() - subProcessX, graphicInfo.getY() - subProcessY);
        shapesArrayNode.add(planItemNode);
        ObjectNode propertiesNode = objectMapper.createObjectNode();
        propertiesNode.put(PROPERTY_OVERRIDE_ID, planItemDefinition.getId());

        if (StringUtils.isNotEmpty(planItemDefinition.getName())) {
            propertiesNode.put(PROPERTY_NAME, planItemDefinition.getName());
        }

        if (StringUtils.isNotEmpty(planItemDefinition.getDocumentation())) {
            propertiesNode.put(PROPERTY_DOCUMENTATION, planItemDefinition.getDocumentation());
        }
        
        if (planItemDefinition instanceof Task) {
            Task task = (Task) planItemDefinition;
            if (task.isBlocking()){
                propertiesNode.put(PROPERTY_IS_BLOCKING, task.isBlocking());
            }
            if (StringUtils.isNotEmpty(task.getBlockingExpression())){
                propertiesNode.put(PROPERTY_IS_BLOCKING_EXPRESSION, task.getBlockingExpression());
            }

            if (task.isAsync()) {
                propertiesNode.put(PROPERTY_IS_ASYNC, task.isAsync());
            }
            if (task.isExclusive()) {
                propertiesNode.put(PROPERTY_IS_EXCLUSIVE, task.isExclusive());
            }
            
        } else if (planItemDefinition instanceof Stage) {
            Stage stage = (Stage) planItemDefinition;
            if (stage.isAutoComplete()) {
                propertiesNode.put(PROPERTY_IS_AUTOCOMPLETE, stage.isAutoComplete());
            }
            if (StringUtils.isNotEmpty(stage.getAutoCompleteCondition())) {
                propertiesNode.put(PROPERTY_AUTOCOMPLETE_CONDITION, stage.getAutoCompleteCondition());
            }
        }
        
        if (planItem.getItemControl() != null) {
            convertPlanItemControlToJson(planItem, propertiesNode);
        }

        convertElementToJson(planItemNode, propertiesNode, processor, baseElement, model, converterContext);

        planItemNode.set(EDITOR_SHAPE_PROPERTIES, propertiesNode);
        ArrayNode outgoingArrayNode = objectMapper.createArrayNode();

        if (CollectionUtils.isNotEmpty(planItem.getEntryCriteria())) {
            convertCriteria(planItem.getEntryCriteria(), planItemDefinition, planItemNode, model, processor, converterContext, shapesArrayNode,
                            outgoingArrayNode, subProcessX, subProcessY);
        }

        if (CollectionUtils.isNotEmpty(planItem.getExitCriteria())) {
            convertCriteria(planItem.getExitCriteria(), planItemDefinition, planItemNode, model, processor, converterContext, shapesArrayNode,
                            outgoingArrayNode, subProcessX, subProcessY);
        }
        
        if (CollectionUtils.isNotEmpty(planItem.getOutgoingAssociations())) {
            for (Association association : planItem.getOutgoingAssociations()) {
                outgoingArrayNode.add(CmmnJsonConverterUtil.createResourceNode(association.getId()));
            }
        
        } else if (!model.getAssociations().isEmpty() && CollectionUtils.isEmpty(planItem.getOutgoingAssociations()) && 
                CollectionUtils.isEmpty(planItem.getIncomingAssociations())) {
            
            for (Association association : model.getAssociations()) {
                if (planItem.getId().equals(association.getSourceRef())) {
                    outgoingArrayNode.add(CmmnJsonConverterUtil.createResourceNode(association.getId()));
                
                } else if (planItem.getId().equals(association.getTargetRef())) {
                    outgoingArrayNode.add(CmmnJsonConverterUtil.createResourceNode(association.getId()));
                }
            }
        }

        planItemNode.set("outgoing", outgoingArrayNode);
    }

    protected void convertPlanItemControlToJson(PlanItem planItem, ObjectNode propertiesNode) {
        RepetitionRule repetitionRule = planItem.getItemControl().getRepetitionRule(); 
        if (repetitionRule != null) {
            propertiesNode.put(PROPERTY_REPETITION_ENABLED, true);
            
            if (StringUtils.isNotEmpty(repetitionRule.getCondition())) {
                propertiesNode.put(PROPERTY_REPETITION_RULE_CONDITION, repetitionRule.getCondition());
            }
            if (!RepetitionRule.DEFAULT_REPETITION_COUNTER_VARIABLE_NAME.equals(repetitionRule.getRepetitionCounterVariableName())) {
                propertiesNode.put(PROPERTY_REPETITION_RULE_VARIABLE_NAME, repetitionRule.getRepetitionCounterVariableName());
            }

            convertVariableAggregationDefinitionsToJson(repetitionRule.getAggregations(), propertiesNode);
        }
        
        ManualActivationRule manualActivationRule = planItem.getItemControl().getManualActivationRule();
        if (manualActivationRule != null) {
            propertiesNode.put(PROPERTY_MANUAL_ACTIVATION_ENABLED, true);
            
            if (StringUtils.isNotEmpty(manualActivationRule.getCondition())) {
                propertiesNode.put(PROPERTY_MANUAL_ACTIVATION_RULE_CONDITION, manualActivationRule.getCondition());
            }
        }
        
        RequiredRule requiredRule = planItem.getItemControl().getRequiredRule();
        if (requiredRule != null) {
            propertiesNode.put(PROPERTY_REQUIRED_ENABLED, true);
            
            if (StringUtils.isNotEmpty(requiredRule.getCondition())) {
                propertiesNode.put(PROPERTY_REQUIRED_RULE_CONDITION, requiredRule.getCondition());
            }
        }
        
        CompletionNeutralRule completionNeutralRule = planItem.getItemControl().getCompletionNeutralRule();
        if (completionNeutralRule != null) {
            propertiesNode.put(PROPERTY_COMPLETION_NEUTRAL_ENABLED, true);
            
            if (StringUtils.isNotEmpty(completionNeutralRule.getCondition())) {
                propertiesNode.put(PROPERTY_COMPLETION_NEUTRAL_RULE_CONDITION, completionNeutralRule.getCondition());
            }
        }
    }

    protected void convertVariableAggregationDefinitionsToJson(VariableAggregationDefinitions aggregations, ObjectNode propertiesNode) {
        if (aggregations == null) {
            propertiesNode.putNull(PROPERTY_REPETITION_VARIABLE_AGGREGATIONS);
            return;
        }

        ObjectNode aggregationsNode = propertiesNode.putObject(PROPERTY_REPETITION_VARIABLE_AGGREGATIONS);


        Collection<VariableAggregationDefinition> aggregationsCollection = aggregations.getAggregations();
        ArrayNode itemsArray = aggregationsNode.putArray("aggregations");
        for (VariableAggregationDefinition aggregation : aggregationsCollection) {
            ObjectNode aggregationNode = itemsArray.addObject();

            aggregationNode.put("target", aggregation.getTarget());
            aggregationNode.put("targetExpression", aggregation.getTarget());

            String implementationType = aggregation.getImplementationType();
            if (ImplementationType.IMPLEMENTATION_TYPE_DELEGATEEXPRESSION.equals(implementationType)) {
                aggregationNode.put("delegateExpression", aggregation.getImplementation());
            } else if (ImplementationType.IMPLEMENTATION_TYPE_CLASS.equals(implementationType)) {
                aggregationNode.put("class", aggregation.getImplementation());
            }

            aggregationNode.put("storeAsTransient", aggregation.isStoreAsTransientVariable());
            aggregationNode.put("createOverview", aggregation.isCreateOverviewVariable());

            ArrayNode definitionsArray = aggregationNode.putArray("definitions");

            for (VariableAggregationDefinition.Variable definition : aggregation.getDefinitions()) {
                ObjectNode definitionNode = definitionsArray.addObject();

                definitionNode.put("source", definition.getSource());
                definitionNode.put("sourceExpression", definition.getSourceExpression());
                definitionNode.put("target", definition.getTarget());
                definitionNode.put("targetExpression", definition.getTargetExpression());
            }
        }
    }

    public void convertToCmmnModel(JsonNode elementNode, JsonNode modelNode, ActivityProcessor processor, BaseElement parentElement,
            Map<String, JsonNode> shapeMap, CmmnModel cmmnModel, CmmnJsonConverterContext converterContext, CmmnModelIdHelper cmmnModelIdHelper) {

        BaseElement baseElement = convertJsonToElement(elementNode, modelNode, processor, parentElement, shapeMap, cmmnModel, converterContext, cmmnModelIdHelper);
        baseElement.setId(CmmnJsonConverterUtil.getElementId(elementNode));

        if (baseElement instanceof PlanItemDefinition) {
            PlanItemDefinition planItemDefinition = (PlanItemDefinition) baseElement;
            planItemDefinition.setName(CmmnJsonConverterUtil.getPropertyValueAsString(PROPERTY_NAME, elementNode));
            planItemDefinition.setDocumentation(CmmnJsonConverterUtil.getPropertyValueAsString(PROPERTY_DOCUMENTATION, elementNode));

            if (planItemDefinition instanceof Task) {
                handleTaskProperties(elementNode, planItemDefinition);
            }

            Stage stage = (Stage) parentElement;
            stage.addPlanItemDefinition(planItemDefinition);

            PlanItem planItem = new PlanItem();
            planItem.setId("planItem" + cmmnModelIdHelper.nextPlanItemId());
            planItem.setName(planItemDefinition.getName());
            planItem.setPlanItemDefinition(planItemDefinition);
            planItem.setDefinitionRef(planItemDefinition.getId());

            ArrayNode outgoingNode = (ArrayNode) elementNode.get("outgoing");
            if (outgoingNode != null && outgoingNode.size() > 0) {
                for (JsonNode outgoingChildNode : outgoingNode) {
                    JsonNode resourceNode = outgoingChildNode.get(EDITOR_SHAPE_ID);
                    if (resourceNode != null) {
                        String criterionRefId = resourceNode.asText();
                        planItem.addCriteriaRef(criterionRefId);
                    }
                }
            }
            
            handleRequiredRule(elementNode, planItem);
            handleRepetitionRule(elementNode, planItem);
            handleManualActivationRule(elementNode, planItem);
            handleCompletionNeutralRule(elementNode, planItem);

            planItemDefinition.setPlanItemRef(planItem.getId());

            stage.addPlanItem(planItem);
            planItem.setParent(stage);
        }
    }

    protected void handleTaskProperties(JsonNode elementNode, PlanItemDefinition planItemDefinition) {
        Task task = (Task) planItemDefinition;
        task.setBlocking(CmmnJsonConverterUtil.getPropertyValueAsBoolean(PROPERTY_IS_BLOCKING, elementNode));
        task.setBlockingExpression(CmmnJsonConverterUtil.getPropertyValueAsString(PROPERTY_IS_BLOCKING_EXPRESSION, elementNode));
        task.setAsync(CmmnJsonConverterUtil.getPropertyValueAsBoolean(PROPERTY_IS_ASYNC, elementNode));
        task.setExclusive(CmmnJsonConverterUtil.getPropertyValueAsBoolean(PROPERTY_IS_EXCLUSIVE, elementNode));
    }
    
    protected void handleRequiredRule(JsonNode elementNode, PlanItem planItem) {
        boolean isRequired = CmmnJsonConverterUtil.getPropertyValueAsBoolean(PROPERTY_REQUIRED_ENABLED, elementNode);
        String requiredCondition = CmmnJsonConverterUtil.getPropertyValueAsString(PROPERTY_REQUIRED_RULE_CONDITION, elementNode);
        if (isRequired || StringUtils.isNotEmpty(requiredCondition)) {
            RequiredRule requiredRule = new RequiredRule();
            
            if (StringUtils.isNotEmpty(requiredCondition)) {
                requiredRule.setCondition(requiredCondition);
            }
            
            if (planItem.getItemControl() == null) {
                planItem.setItemControl(new PlanItemControl());
            }
            planItem.getItemControl().setRequiredRule(requiredRule);
        }
    }

    protected void handleRepetitionRule(JsonNode elementNode, PlanItem planItem) {
        boolean repetitionEnabled = CmmnJsonConverterUtil.getPropertyValueAsBoolean(PROPERTY_REPETITION_ENABLED, elementNode);
        String repetitionCondition = CmmnJsonConverterUtil.getPropertyValueAsString(PROPERTY_REPETITION_RULE_CONDITION, elementNode);
        if (repetitionEnabled || StringUtils.isNotEmpty(repetitionCondition)) { // Assume checking the checkbox was forgotten
            RepetitionRule repetitionRule = new RepetitionRule();
            
            if (StringUtils.isNotEmpty(repetitionCondition)) {
                repetitionRule.setCondition(repetitionCondition);
            }
            
            String repetitionCounterVariableName = CmmnJsonConverterUtil.getPropertyValueAsString(PROPERTY_REPETITION_RULE_VARIABLE_NAME, elementNode);
            if (StringUtils.isNotEmpty(repetitionCounterVariableName)) {
                repetitionRule.setRepetitionCounterVariableName(repetitionCounterVariableName);
            }

            repetitionRule.setAggregations(convertJsonToVariableAggregationDefinitions(CmmnJsonConverterUtil.getProperty(CmmnStencilConstants.PROPERTY_REPETITION_VARIABLE_AGGREGATIONS, elementNode)));
            
            if (planItem.getItemControl() == null) {
                planItem.setItemControl(new PlanItemControl());
            }
            planItem.getItemControl().setRepetitionRule(repetitionRule);
        }
    }

    protected VariableAggregationDefinitions convertJsonToVariableAggregationDefinitions(JsonNode node) {
        if (node == null) {
            return null;
        }

        JsonNode aggregationsNode = node.path("aggregations");
        if (aggregationsNode.size() < 0) {
            return null;
        }

        VariableAggregationDefinitions aggregations = new VariableAggregationDefinitions();

        for (JsonNode aggregationNode : aggregationsNode) {
            VariableAggregationDefinition aggregation = new VariableAggregationDefinition();

            aggregation.setTarget(StringUtils.defaultIfBlank(aggregationNode.path("target").asText(null), null));
            aggregation.setTargetExpression(StringUtils.defaultIfBlank(aggregationNode.path("targetExpression").asText(null), null));

            String delegateExpression = aggregationNode.path("delegateExpression").asText(null);
            String customClass = aggregationNode.path("class").asText(null);
            if (StringUtils.isNotBlank(delegateExpression)) {
                aggregation.setImplementationType(ImplementationType.IMPLEMENTATION_TYPE_DELEGATEEXPRESSION);
                aggregation.setImplementation(delegateExpression);
            } else if (StringUtils.isNotBlank(customClass)) {
                aggregation.setImplementationType(ImplementationType.IMPLEMENTATION_TYPE_CLASS);
                aggregation.setImplementation(aggregationNode.path("class").asText(null));
            }

            if (aggregationNode.path("storeAsTransient").asBoolean(false)) {
                aggregation.setStoreAsTransientVariable(true);
            }

            if (aggregationNode.path("createOverview").asBoolean(false)) {
                aggregation.setCreateOverviewVariable(true);
            }

            for (JsonNode definitionNode : aggregationNode.path("definitions")) {
                VariableAggregationDefinition.Variable definition = new VariableAggregationDefinition.Variable();

                definition.setSource(StringUtils.defaultIfBlank(definitionNode.path("source").asText(null), null));
                definition.setSourceExpression(StringUtils.defaultIfBlank(definitionNode.path("sourceExpression").asText(null), null));
                definition.setTarget(StringUtils.defaultIfBlank(definitionNode.path("target").asText(null), null));
                definition.setTargetExpression(StringUtils.defaultIfBlank(definitionNode.path("targetExpression").asText(null), null));

                aggregation.addDefinition(definition);
            }

            aggregations.getAggregations().add(aggregation);
        }

        return aggregations;

    }

    protected void handleManualActivationRule(JsonNode elementNode, PlanItem planItem) {
        boolean manualActivationEnabled = CmmnJsonConverterUtil.getPropertyValueAsBoolean(PROPERTY_MANUAL_ACTIVATION_ENABLED, elementNode);
        String manualActivationCondition = CmmnJsonConverterUtil.getPropertyValueAsString(PROPERTY_MANUAL_ACTIVATION_RULE_CONDITION, elementNode);
        if (manualActivationEnabled || StringUtils.isNotEmpty(manualActivationCondition)) {
            ManualActivationRule manualActivationRule = new ManualActivationRule();
            
            if (StringUtils.isNotEmpty(manualActivationCondition)) {
                manualActivationRule.setCondition(manualActivationCondition);
            }
            
            if (planItem.getItemControl() == null) {
                planItem.setItemControl(new PlanItemControl());
            }
            planItem.getItemControl().setManualActivationRule(manualActivationRule);
        }
    }
    
    protected void handleCompletionNeutralRule(JsonNode elementNode, PlanItem planItem) {
        boolean completionNeutralEnabled = CmmnJsonConverterUtil.getPropertyValueAsBoolean(PROPERTY_COMPLETION_NEUTRAL_ENABLED, elementNode);
        String completionNeutralCondition = CmmnJsonConverterUtil.getPropertyValueAsString(PROPERTY_COMPLETION_NEUTRAL_RULE_CONDITION, elementNode);
        if (completionNeutralEnabled || StringUtils.isNotEmpty(completionNeutralCondition)) {
            CompletionNeutralRule completionNeutralRule = new CompletionNeutralRule();
            
            if (StringUtils.isNotEmpty(completionNeutralCondition)) {
                completionNeutralRule.setCondition(completionNeutralCondition);
            }
            
            if (planItem.getItemControl() == null) {
                planItem.setItemControl(new PlanItemControl());
            }
            planItem.getItemControl().setCompletionNeutralRule(completionNeutralRule);
        }
    }

    protected abstract void convertElementToJson(ObjectNode elementNode, ObjectNode propertiesNode, ActivityProcessor processor,
        BaseElement baseElement, CmmnModel cmmnModel, CmmnJsonConverterContext converterContext);

    protected abstract BaseElement convertJsonToElement(JsonNode elementNode, JsonNode modelNode, ActivityProcessor processor,
                    BaseElement parentElement, Map<String, JsonNode> shapeMap, CmmnModel cmmnModel, CmmnJsonConverterContext converterContext, CmmnModelIdHelper cmmnModelIdHelper);

    protected abstract String getStencilId(BaseElement baseElement);

    protected void convertCriteria(List<Criterion> criteria, PlanItemDefinition criterionParentDefinition, ObjectNode criterionParentPlanItemNode, CmmnModel model, ActivityProcessor processor, 
                    CmmnJsonConverterContext converterContext, ArrayNode shapesArrayNode, ArrayNode outgoingArrayNode, double subProcessX, double subProcessY) {
        
        for (Criterion criterion : criteria) {
            GraphicInfo criterionGraphicInfo = model.getGraphicInfo(criterion.getId());
            ObjectNode criterionNode = CmmnJsonConverterUtil.createChildShape(criterion.getId(), criterion.isEntryCriterion() ? STENCIL_ENTRY_CRITERION : STENCIL_EXIT_CRITERION,
                    criterionGraphicInfo.getX() - subProcessX + criterionGraphicInfo.getWidth(), criterionGraphicInfo.getY() - subProcessY + criterionGraphicInfo.getHeight(),
                    criterionGraphicInfo.getX() - subProcessX, criterionGraphicInfo.getY() - subProcessY);

            Stage attachedToStage = null;
            if (criterion.isExitCriterion() && criterionParentDefinition instanceof Stage) {
                Stage criterionStage = (Stage) criterionParentDefinition;
                if (!criterionStage.isPlanModel()) {
                    attachedToStage = criterionStage;
                }
            }
            
            if (attachedToStage != null) {
                ArrayNode planItemChildShapes = (ArrayNode) criterionParentPlanItemNode.get(EDITOR_CHILD_SHAPES);
                planItemChildShapes.add(criterionNode);
                outgoingArrayNode.add(CmmnJsonConverterUtil.createResourceNode(criterion.getId()));
            } else {
                shapesArrayNode.add(criterionNode);
            }
            
            ObjectNode criterionPropertiesNode = objectMapper.createObjectNode();
            criterionPropertiesNode.put(PROPERTY_OVERRIDE_ID, criterion.getId());
            new CriterionJsonConverter().convertElementToJson(criterionNode, criterionPropertiesNode, processor, criterion, model, converterContext);
            criterionNode.set(EDITOR_SHAPE_PROPERTIES, criterionPropertiesNode);

            if (CollectionUtils.isNotEmpty(criterion.getOutgoingAssociations())) {
                ArrayNode criterionOutgoingArrayNode = objectMapper.createArrayNode();
                for (Association association : criterion.getOutgoingAssociations()) {
                    criterionOutgoingArrayNode.add(CmmnJsonConverterUtil.createResourceNode(association.getId()));
                }

                criterionNode.set("outgoing", criterionOutgoingArrayNode);
            }

            if (attachedToStage == null) {
                outgoingArrayNode.add(CmmnJsonConverterUtil.createResourceNode(criterion.getId()));
            }
        }
    }

    protected void addFieldExtensions(List<FieldExtension> extensions, ObjectNode propertiesNode) {
        ObjectNode fieldExtensionsNode = objectMapper.createObjectNode();
        ArrayNode itemsNode = objectMapper.createArrayNode();
        for (FieldExtension extension : extensions) {
            ObjectNode propertyItemNode = objectMapper.createObjectNode();
            propertyItemNode.put(PROPERTY_SERVICETASK_FIELD_NAME, extension.getFieldName());
            if (StringUtils.isNotEmpty(extension.getStringValue())) {
                propertyItemNode.put(PROPERTY_SERVICETASK_FIELD_STRING_VALUE, extension.getStringValue());
            }
            if (StringUtils.isNotEmpty(extension.getExpression())) {
                propertyItemNode.put(PROPERTY_SERVICETASK_FIELD_EXPRESSION, extension.getExpression());
            }
            itemsNode.add(propertyItemNode);
        }

        fieldExtensionsNode.set("fields", itemsNode);
        propertiesNode.set(PROPERTY_SERVICETASK_FIELDS, fieldExtensionsNode);
    }

    protected void addField(String name, String propertyName, JsonNode elementNode, ServiceTask task) {
       addField(name, propertyName, null, elementNode, task);
    }
    
    protected void addField(String name, String propertyName, String defaultValue, JsonNode elementNode, ServiceTask task) {
        FieldExtension field = new FieldExtension();
        field.setFieldName(name);
        String value = CmmnJsonConverterUtil.getPropertyValueAsString(propertyName, elementNode);
        if (StringUtils.isNotEmpty(value)) {
            field.setStringValue(value);
            task.getFieldExtensions().add(field);
        } else if (StringUtils.isNotEmpty(defaultValue)) {
            field.setStringValue(defaultValue);
            task.getFieldExtensions().add(field);
        }
    }
    
    protected ExtensionElement addFlowableExtensionElement(String name, PlanItemDefinition planItemDefinition) {
        ExtensionElement extensionElement = new ExtensionElement();
        extensionElement.setName(name);
        extensionElement.setNamespace("http://flowable.org/cmmn");
        extensionElement.setNamespacePrefix("flowable");
        planItemDefinition.addExtensionElement(extensionElement);
        return extensionElement;
    }
    
    protected ExtensionElement addFlowableExtensionElementWithValue(String name, String value, PlanItemDefinition planItemDefinition) {
        ExtensionElement extensionElement = null;
        if (StringUtils.isNotEmpty(value)) {
            extensionElement = addFlowableExtensionElement(name, planItemDefinition);
            extensionElement.setElementText(value);
        }
        
        return extensionElement;
    }
    
    public void addExtensionAttribute(String name, String value, ExtensionElement extensionElement) {
        ExtensionAttribute attribute = new ExtensionAttribute(name);
        attribute.setValue(value);
        extensionElement.addAttribute(attribute);
    }
    
    public ExtensionAttribute createExtensionAttribute(String name, String value) {
        ExtensionAttribute attribute = new ExtensionAttribute(name);
        attribute.setValue(value);
        return attribute;
    }

    protected void setPropertyValue(String name, String value, ObjectNode propertiesNode) {
        if (StringUtils.isNotEmpty(value)) {
            propertiesNode.put(name, value);
        }
    }

    protected String getValueAsString(String name, JsonNode objectNode) {
        String propertyValue = null;
        JsonNode propertyNode = objectNode.get(name);
        if (propertyNode != null && !propertyNode.isNull()) {
            propertyValue = propertyNode.asText();
        }
        return propertyValue;
    }

    protected boolean getValueAsBoolean(String name, JsonNode objectNode) {
        boolean propertyValue = false;
        JsonNode propertyNode = objectNode.get(name);
        if (propertyNode != null && !propertyNode.isNull()) {
            propertyValue = propertyNode.asBoolean();
        }
        return propertyValue;
    }
    
    protected String getPropertyValueAsString(String name, JsonNode objectNode) {
        return CmmnModelJsonConverterUtil.getPropertyValueAsString(name, objectNode);
    }
    
    protected JsonNode getProperty(String name, JsonNode objectNode) {
        return CmmnModelJsonConverterUtil.getProperty(name, objectNode);
    }
    
    protected String getExtensionValue(String name, PlanItemDefinition planItemDefinition) {
        List<ExtensionElement> extensionElements = planItemDefinition.getExtensionElements().get(name);
        if (extensionElements != null && extensionElements.size() > 0) {
            return extensionElements.get(0).getElementText();
        }
        
        return null;
    }

    protected List<String> getValueAsList(String name, JsonNode objectNode) {
        List<String> resultList = new ArrayList<>();
        JsonNode valuesNode = objectNode.get(name);
        if (valuesNode != null) {
            for (JsonNode valueNode : valuesNode) {
                if (valueNode.get("value") != null && !valueNode.get("value").isNull()) {
                    resultList.add(valueNode.get("value").asText());
                }
            }
        }
        return resultList;
    }

    protected String convertListToCommaSeparatedString(List<String> stringList) {
        String resultString = null;
        if (stringList != null && stringList.size() > 0) {
            StringBuilder expressionBuilder = new StringBuilder();
            for (String singleItem : stringList) {
                if (expressionBuilder.length() > 0) {
                    expressionBuilder.append(",");
                }
                expressionBuilder.append(singleItem);
            }
            resultString = expressionBuilder.toString();
        }
        return resultString;
    }
}
