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
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.flowable.cmmn.editor.constants.CmmnStencilConstants;
import org.flowable.cmmn.editor.constants.EditorJsonConstants;
import org.flowable.cmmn.editor.json.converter.CmmnJsonConverter.CmmnModelIdHelper;
import org.flowable.cmmn.editor.json.converter.util.CollectionUtils;
import org.flowable.cmmn.model.Association;
import org.flowable.cmmn.model.BaseElement;
import org.flowable.cmmn.model.CmmnModel;
import org.flowable.cmmn.model.Criterion;
import org.flowable.cmmn.model.FieldExtension;
import org.flowable.cmmn.model.GraphicInfo;
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

    public void convertToJson(BaseElement baseElement, ActivityProcessor processor, CmmnModel model, PlanFragment planFragment, ArrayNode shapesArrayNode, double subProcessX, double subProcessY) {

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

        convertElementToJson(planItemNode, propertiesNode, processor, baseElement, model);

        planItemNode.set(EDITOR_SHAPE_PROPERTIES, propertiesNode);
        ArrayNode outgoingArrayNode = objectMapper.createArrayNode();

        if (CollectionUtils.isNotEmpty(planItem.getEntryCriteria())) {
            convertCriteria(planItem.getEntryCriteria(), model, processor, shapesArrayNode, outgoingArrayNode, subProcessX, subProcessY);
        }

        if (CollectionUtils.isNotEmpty(planItem.getExitCriteria())) {
            convertCriteria(planItem.getExitCriteria(), model, processor, shapesArrayNode, outgoingArrayNode, subProcessX, subProcessY);
        }
        
        if (CollectionUtils.isNotEmpty(planItem.getOutgoingAssociations())) {
            for (Association association : planItem.getOutgoingAssociations()) {
                outgoingArrayNode.add(CmmnJsonConverterUtil.createResourceNode(association.getId()));
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
    }

    public void convertToCmmnModel(JsonNode elementNode, JsonNode modelNode, ActivityProcessor processor, BaseElement parentElement,
            Map<String, JsonNode> shapeMap, CmmnModel cmmnModel, CmmnModelIdHelper cmmnModelIdHelper) {

        BaseElement baseElement = convertJsonToElement(elementNode, modelNode, processor, parentElement, shapeMap, cmmnModel, cmmnModelIdHelper);
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
            
            if (planItem.getItemControl() == null) {
                planItem.setItemControl(new PlanItemControl());
            }
            planItem.getItemControl().setRepetitionRule(repetitionRule);
        }
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

    protected abstract void convertElementToJson(ObjectNode elementNode, ObjectNode propertiesNode, ActivityProcessor processor, BaseElement baseElement, CmmnModel cmmnModel);

    protected abstract BaseElement convertJsonToElement(JsonNode elementNode, JsonNode modelNode, ActivityProcessor processor,
                    BaseElement parentElement, Map<String, JsonNode> shapeMap, CmmnModel cmmnModel, CmmnModelIdHelper cmmnModelIdHelper);

    protected abstract String getStencilId(BaseElement baseElement);

    protected void convertCriteria(List<Criterion> criteria, CmmnModel model, ActivityProcessor processor, ArrayNode shapesArrayNode, ArrayNode outgoingArrayNode, double subProcessX, double subProcessY) {
        for (Criterion criterion : criteria) {
            GraphicInfo criterionGraphicInfo = model.getGraphicInfo(criterion.getId());
            ObjectNode criterionNode = CmmnJsonConverterUtil.createChildShape(criterion.getId(), criterion.isEntryCriterion() ? STENCIL_ENTRY_CRITERION : STENCIL_EXIT_CRITERION,
                    criterionGraphicInfo.getX() - subProcessX + criterionGraphicInfo.getWidth(), criterionGraphicInfo.getY() - subProcessY + criterionGraphicInfo.getHeight(),
                    criterionGraphicInfo.getX() - subProcessX, criterionGraphicInfo.getY() - subProcessY);

            shapesArrayNode.add(criterionNode);
            ObjectNode criterionPropertiesNode = objectMapper.createObjectNode();
            criterionPropertiesNode.put(PROPERTY_OVERRIDE_ID, criterion.getId());
            new CriterionJsonConverter().convertElementToJson(criterionNode, criterionPropertiesNode, processor, criterion, model);
            criterionNode.set(EDITOR_SHAPE_PROPERTIES, criterionPropertiesNode);

            if (CollectionUtils.isNotEmpty(criterion.getOutgoingAssociations())) {
                ArrayNode criterionOutgoingArrayNode = objectMapper.createArrayNode();
                for (Association association : criterion.getOutgoingAssociations()) {
                    criterionOutgoingArrayNode.add(CmmnJsonConverterUtil.createResourceNode(association.getId()));
                }

                criterionNode.set("outgoing", criterionOutgoingArrayNode);
            }

            outgoingArrayNode.add(CmmnJsonConverterUtil.createResourceNode(criterion.getId()));
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
