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
package org.flowable.editor.language.json.converter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.flowable.bpmn.model.Activity;
import org.flowable.bpmn.model.Artifact;
import org.flowable.bpmn.model.Association;
import org.flowable.bpmn.model.BaseElement;
import org.flowable.bpmn.model.BoundaryEvent;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.CompensateEventDefinition;
import org.flowable.bpmn.model.ConditionalEventDefinition;
import org.flowable.bpmn.model.DataAssociation;
import org.flowable.bpmn.model.DataStoreReference;
import org.flowable.bpmn.model.ErrorEventDefinition;
import org.flowable.bpmn.model.EscalationEventDefinition;
import org.flowable.bpmn.model.Event;
import org.flowable.bpmn.model.EventDefinition;
import org.flowable.bpmn.model.ExtensionAttribute;
import org.flowable.bpmn.model.ExtensionElement;
import org.flowable.bpmn.model.FieldExtension;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.FlowElementsContainer;
import org.flowable.bpmn.model.FlowNode;
import org.flowable.bpmn.model.FormProperty;
import org.flowable.bpmn.model.FormValue;
import org.flowable.bpmn.model.Gateway;
import org.flowable.bpmn.model.GraphicInfo;
import org.flowable.bpmn.model.IOParameter;
import org.flowable.bpmn.model.ImplementationType;
import org.flowable.bpmn.model.Lane;
import org.flowable.bpmn.model.MapExceptionEntry;
import org.flowable.bpmn.model.MessageEventDefinition;
import org.flowable.bpmn.model.MessageFlow;
import org.flowable.bpmn.model.MultiInstanceLoopCharacteristics;
import org.flowable.bpmn.model.Process;
import org.flowable.bpmn.model.SendEventServiceTask;
import org.flowable.bpmn.model.SequenceFlow;
import org.flowable.bpmn.model.ServiceTask;
import org.flowable.bpmn.model.SignalEventDefinition;
import org.flowable.bpmn.model.StartEvent;
import org.flowable.bpmn.model.SubProcess;
import org.flowable.bpmn.model.TerminateEventDefinition;
import org.flowable.bpmn.model.TimerEventDefinition;
import org.flowable.bpmn.model.UserTask;
import org.flowable.bpmn.model.VariableAggregationDefinition;
import org.flowable.bpmn.model.VariableAggregationDefinitions;
import org.flowable.bpmn.model.VariableListenerEventDefinition;
import org.flowable.editor.constants.EditorJsonConstants;
import org.flowable.editor.constants.StencilConstants;
import org.flowable.editor.language.json.converter.util.CollectionUtils;
import org.flowable.editor.language.json.converter.util.JsonConverterUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * @author Tijs Rademakers
 * @author Zheng Ji
 */
public abstract class BaseBpmnJsonConverter implements EditorJsonConstants, StencilConstants {

    protected static final Logger LOGGER = LoggerFactory.getLogger(BaseBpmnJsonConverter.class);

    public static final String NAMESPACE = "http://flowable.org/modeler";

    protected ObjectMapper objectMapper = new ObjectMapper();
    protected ActivityProcessor processor;
    protected BpmnModel model;
    protected ObjectNode flowElementNode;
    protected double subProcessX;
    protected double subProcessY;
    protected ArrayNode shapesArrayNode;

    public void convertToJson(BpmnJsonConverterContext converterContext, BaseElement baseElement, ActivityProcessor processor, BpmnModel model,
            FlowElementsContainer container, ArrayNode shapesArrayNode, double subProcessX, double subProcessY) {

        this.model = model;
        this.processor = processor;
        this.subProcessX = subProcessX;
        this.subProcessY = subProcessY;
        this.shapesArrayNode = shapesArrayNode;
        GraphicInfo graphicInfo = model.getGraphicInfo(baseElement.getId());

        String stencilId = null;
        if (baseElement instanceof ServiceTask) {
            ServiceTask serviceTask = (ServiceTask) baseElement;
            if ("mail".equalsIgnoreCase(serviceTask.getType())) {
                stencilId = STENCIL_TASK_MAIL;
            } else if ("camel".equalsIgnoreCase(serviceTask.getType())) {
                stencilId = STENCIL_TASK_CAMEL;
            } else if ("mule".equalsIgnoreCase(serviceTask.getType())) {
                stencilId = STENCIL_TASK_MULE;
            } else if ("http".equalsIgnoreCase(serviceTask.getType())) {
                stencilId = STENCIL_TASK_HTTP;
            } else if ("dmn".equalsIgnoreCase(serviceTask.getType())) {
                stencilId = STENCIL_TASK_DECISION;
            } else if ("shell".equalsIgnoreCase(serviceTask.getType())) {
                stencilId = STENCIL_TASK_SHELL;
            } else {
                stencilId = getStencilId(baseElement);
            }
        } else {
            stencilId = getStencilId(baseElement);
        }

        flowElementNode = BpmnJsonConverterUtil.createChildShape(baseElement.getId(), stencilId, graphicInfo.getX() - subProcessX + graphicInfo.getWidth(),
                graphicInfo.getY() - subProcessY + graphicInfo.getHeight(), graphicInfo.getX() - subProcessX, graphicInfo.getY() - subProcessY);
        shapesArrayNode.add(flowElementNode);
        ObjectNode propertiesNode = objectMapper.createObjectNode();
        propertiesNode.put(PROPERTY_OVERRIDE_ID, baseElement.getId());

        if (baseElement instanceof FlowElement) {
            FlowElement flowElement = (FlowElement) baseElement;
            if (StringUtils.isNotEmpty(flowElement.getName())) {
                propertiesNode.put(PROPERTY_NAME, flowElement.getName());
            }

            if (StringUtils.isNotEmpty(flowElement.getDocumentation())) {
                propertiesNode.put(PROPERTY_DOCUMENTATION, flowElement.getDocumentation());
            }
        }

        convertElementToJson(propertiesNode, baseElement, converterContext);

        flowElementNode.set(EDITOR_SHAPE_PROPERTIES, propertiesNode);
        ArrayNode outgoingArrayNode = objectMapper.createArrayNode();

        if (baseElement instanceof FlowNode) {
            FlowNode flowNode = (FlowNode) baseElement;
            for (SequenceFlow sequenceFlow : flowNode.getOutgoingFlows()) {
                outgoingArrayNode.add(BpmnJsonConverterUtil.createResourceNode(sequenceFlow.getId()));
            }

            for (MessageFlow messageFlow : model.getMessageFlows().values()) {
                if (messageFlow.getSourceRef().equals(flowNode.getId())) {
                    outgoingArrayNode.add(BpmnJsonConverterUtil.createResourceNode(messageFlow.getId()));
                }
            }
            for (Artifact artifact : model.getMainProcess().getArtifacts()) {
                if (artifact instanceof  Association){
                    Association association= (Association) artifact;
                    if (association.getSourceRef().equals(flowNode.getId())){
                        outgoingArrayNode.add(BpmnJsonConverterUtil.createResourceNode(artifact.getId()));
                    }

                }
            }
        }

        if (baseElement instanceof Activity) {

            Activity activity = (Activity) baseElement;
            for (BoundaryEvent boundaryEvent : activity.getBoundaryEvents()) {
                outgoingArrayNode.add(BpmnJsonConverterUtil.createResourceNode(boundaryEvent.getId()));
            }

            propertiesNode.put(PROPERTY_ASYNCHRONOUS, activity.isAsynchronous());
            propertiesNode.put(PROPERTY_EXCLUSIVE, !activity.isNotExclusive());
            propertiesNode.put(PROPERTY_FOR_COMPENSATION,activity.isForCompensation());

            if (activity.getLoopCharacteristics() != null) {
                MultiInstanceLoopCharacteristics loopDef = activity.getLoopCharacteristics();
                if (StringUtils.isNotEmpty(loopDef.getLoopCardinality()) || StringUtils.isNotEmpty(loopDef.getInputDataItem()) || StringUtils.isNotEmpty(loopDef.getCompletionCondition())) {

                    if (!loopDef.isSequential()) {
                        propertiesNode.put(PROPERTY_MULTIINSTANCE_TYPE, "Parallel");
                    } else {
                        propertiesNode.put(PROPERTY_MULTIINSTANCE_TYPE, "Sequential");
                    }

                    if (StringUtils.isNotEmpty(loopDef.getLoopCardinality())) {
                        propertiesNode.put(PROPERTY_MULTIINSTANCE_CARDINALITY, loopDef.getLoopCardinality());
                    }
                    if (StringUtils.isNotEmpty(loopDef.getInputDataItem())) {
                        propertiesNode.put(PROPERTY_MULTIINSTANCE_COLLECTION, loopDef.getInputDataItem());
                    }
                    if (StringUtils.isNotEmpty(loopDef.getElementVariable())) {
                        propertiesNode.put(PROPERTY_MULTIINSTANCE_VARIABLE, loopDef.getElementVariable());
                    }
                    if (StringUtils.isNotEmpty(loopDef.getCompletionCondition())) {
                        propertiesNode.put(PROPERTY_MULTIINSTANCE_CONDITION, loopDef.getCompletionCondition());
                    }
                    if (StringUtils.isNotEmpty(loopDef.getElementIndexVariable())) {
                        propertiesNode.put(PROPERTY_MULTIINSTANCE_INDEX_VARIABLE, loopDef.getElementIndexVariable());
                    }

                    processVariableAggregationDefinitions(loopDef.getAggregations(), propertiesNode);
                }
            }

            if (activity instanceof UserTask) {
                BpmnJsonConverterUtil.convertListenersToJson(((UserTask) activity).getTaskListeners(), false, propertiesNode);
            }

            if (CollectionUtils.isNotEmpty(activity.getDataInputAssociations())) {
                for (DataAssociation dataAssociation : activity.getDataInputAssociations()) {
                    if (model.getFlowElement(dataAssociation.getSourceRef()) != null) {
                        createDataAssociation(dataAssociation, true, activity);
                    }
                }
            }

            if (CollectionUtils.isNotEmpty(activity.getDataOutputAssociations())) {
                for (DataAssociation dataAssociation : activity.getDataOutputAssociations()) {
                    if (model.getFlowElement(dataAssociation.getTargetRef()) != null) {
                        createDataAssociation(dataAssociation, false, activity);
                        outgoingArrayNode.add(BpmnJsonConverterUtil.createResourceNode(dataAssociation.getId()));
                    }
                }
            }

        } else if (baseElement instanceof Gateway) {
            Gateway gateway = (Gateway) baseElement;
            propertiesNode.put(PROPERTY_ASYNCHRONOUS, gateway.isAsynchronous());
            propertiesNode.put(PROPERTY_EXCLUSIVE, !gateway.isNotExclusive());
        }

        if (baseElement instanceof FlowElement) {
            BpmnJsonConverterUtil.convertListenersToJson(((FlowElement) baseElement).getExecutionListeners(), true, propertiesNode);
        }

        for (Artifact artifact : container.getArtifacts()) {
            if (artifact instanceof Association) {
                Association association = (Association) artifact;
                if (StringUtils.isNotEmpty(association.getSourceRef()) && association.getSourceRef().equals(baseElement.getId())) {
                    outgoingArrayNode.add(BpmnJsonConverterUtil.createResourceNode(association.getId()));
                }
            }
        }

        if (baseElement instanceof DataStoreReference) {
            for (Process process : model.getProcesses()) {
                processDataStoreReferences(process, baseElement.getId(), outgoingArrayNode);
            }
        }

        flowElementNode.set("outgoing", outgoingArrayNode);
    }

    protected void processVariableAggregationDefinitions(VariableAggregationDefinitions aggregations, ObjectNode propertiesNode) {
        if (aggregations == null) {
            propertiesNode.putNull(PROPERTY_MULTIINSTANCE_VARIABLE_AGGREGATIONS);
            return;
        }

        ObjectNode aggregationsNode = propertiesNode.putObject(PROPERTY_MULTIINSTANCE_VARIABLE_AGGREGATIONS);


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


    protected void processDataStoreReferences(FlowElementsContainer container, String dataStoreReferenceId, ArrayNode outgoingArrayNode) {
        for (FlowElement flowElement : container.getFlowElements()) {
            if (flowElement instanceof Activity) {
                Activity activity = (Activity) flowElement;

                if (CollectionUtils.isNotEmpty(activity.getDataInputAssociations())) {
                    for (DataAssociation dataAssociation : activity.getDataInputAssociations()) {
                        if (dataStoreReferenceId.equals(dataAssociation.getSourceRef())) {
                            outgoingArrayNode.add(BpmnJsonConverterUtil.createResourceNode(dataAssociation.getId()));
                        }
                    }
                }

            } else if (flowElement instanceof SubProcess) {
                processDataStoreReferences((SubProcess) flowElement, dataStoreReferenceId, outgoingArrayNode);
            }
        }
    }

    protected void createDataAssociation(DataAssociation dataAssociation, boolean incoming, Activity activity) {
        String sourceRef = null;
        String targetRef = null;
        if (incoming) {
            sourceRef = dataAssociation.getSourceRef();
            targetRef = activity.getId();

        } else {
            sourceRef = activity.getId();
            targetRef = dataAssociation.getTargetRef();
        }

        ObjectNode flowNode = BpmnJsonConverterUtil.createChildShape(dataAssociation.getId(), STENCIL_DATA_ASSOCIATION, 172, 212, 128, 212);
        ArrayNode dockersArrayNode = objectMapper.createArrayNode();
        ObjectNode dockNode = objectMapper.createObjectNode();

        dockNode.put(EDITOR_BOUNDS_X, model.getGraphicInfo(sourceRef).getWidth() / 2.0);
        dockNode.put(EDITOR_BOUNDS_Y, model.getGraphicInfo(sourceRef).getHeight() / 2.0);
        dockersArrayNode.add(dockNode);

        if (model.getFlowLocationGraphicInfo(dataAssociation.getId()).size() > 2) {
            for (int i = 1; i < model.getFlowLocationGraphicInfo(dataAssociation.getId()).size() - 1; i++) {
                GraphicInfo graphicInfo = model.getFlowLocationGraphicInfo(dataAssociation.getId()).get(i);
                dockNode = objectMapper.createObjectNode();
                dockNode.put(EDITOR_BOUNDS_X, graphicInfo.getX());
                dockNode.put(EDITOR_BOUNDS_Y, graphicInfo.getY());
                dockersArrayNode.add(dockNode);
            }
        }

        dockNode = objectMapper.createObjectNode();
        dockNode.put(EDITOR_BOUNDS_X, model.getGraphicInfo(targetRef).getWidth() / 2.0);
        dockNode.put(EDITOR_BOUNDS_Y, model.getGraphicInfo(targetRef).getHeight() / 2.0);
        dockersArrayNode.add(dockNode);
        flowNode.set("dockers", dockersArrayNode);
        ArrayNode outgoingArrayNode = objectMapper.createArrayNode();
        outgoingArrayNode.add(BpmnJsonConverterUtil.createResourceNode(targetRef));
        flowNode.set("outgoing", outgoingArrayNode);
        flowNode.set("target", BpmnJsonConverterUtil.createResourceNode(targetRef));

        ObjectNode propertiesNode = objectMapper.createObjectNode();
        propertiesNode.put(PROPERTY_OVERRIDE_ID, dataAssociation.getId());

        flowNode.set(EDITOR_SHAPE_PROPERTIES, propertiesNode);
        shapesArrayNode.add(flowNode);
    }

    public void convertToBpmnModel(JsonNode elementNode, JsonNode modelNode, ActivityProcessor processor, BaseElement parentElement,
            Map<String, JsonNode> shapeMap, BpmnModel bpmnModel, BpmnJsonConverterContext converterContext) {

        this.processor = processor;
        this.model = bpmnModel;

        BaseElement baseElement = convertJsonToElement(elementNode, modelNode, shapeMap, converterContext);
        baseElement.setId(BpmnJsonConverterUtil.getElementId(elementNode));

        if (baseElement instanceof FlowElement) {
            FlowElement flowElement = (FlowElement) baseElement;
            flowElement.setName(getPropertyValueAsString(PROPERTY_NAME, elementNode));
            flowElement.setDocumentation(getPropertyValueAsString(PROPERTY_DOCUMENTATION, elementNode));

            BpmnJsonConverterUtil.convertJsonToListeners(elementNode, flowElement);

            if (baseElement instanceof Activity) {
                Activity activity = (Activity) baseElement;
                activity.setAsynchronous(getPropertyValueAsBoolean(PROPERTY_ASYNCHRONOUS, elementNode));
                activity.setNotExclusive(!getPropertyValueAsBoolean(PROPERTY_EXCLUSIVE, elementNode));
                activity.setForCompensation(getPropertyValueAsBoolean(PROPERTY_FOR_COMPENSATION, elementNode));
                String multiInstanceType = getPropertyValueAsString(PROPERTY_MULTIINSTANCE_TYPE, elementNode);
                String multiInstanceCardinality = getPropertyValueAsString(PROPERTY_MULTIINSTANCE_CARDINALITY, elementNode);
                String multiInstanceCollection = getPropertyValueAsString(PROPERTY_MULTIINSTANCE_COLLECTION, elementNode);
                String multiInstanceCondition = getPropertyValueAsString(PROPERTY_MULTIINSTANCE_CONDITION, elementNode);
                String multiInstanceIndexVariable = getPropertyValueAsString(PROPERTY_MULTIINSTANCE_INDEX_VARIABLE, elementNode);

                if (StringUtils.isNotEmpty(multiInstanceType) && !"none".equalsIgnoreCase(multiInstanceType)) {

                    String multiInstanceVariable = getPropertyValueAsString(PROPERTY_MULTIINSTANCE_VARIABLE, elementNode);

                    MultiInstanceLoopCharacteristics multiInstanceObject = new MultiInstanceLoopCharacteristics();
                    if ("sequential".equalsIgnoreCase(multiInstanceType)) {
                        multiInstanceObject.setSequential(true);
                    } else {
                        multiInstanceObject.setSequential(false);
                    }
                    multiInstanceObject.setLoopCardinality(multiInstanceCardinality);
                    multiInstanceObject.setInputDataItem(multiInstanceCollection);
                    multiInstanceObject.setElementVariable(multiInstanceVariable);
                    multiInstanceObject.setCompletionCondition(multiInstanceCondition);
                    multiInstanceObject.setElementIndexVariable(multiInstanceIndexVariable);

                    multiInstanceObject.setAggregations(convertJsonToVariableAggregationDefinitions(getProperty(PROPERTY_MULTIINSTANCE_VARIABLE_AGGREGATIONS, elementNode)));

                    activity.setLoopCharacteristics(multiInstanceObject);
                }

            } else if (baseElement instanceof Gateway) {
                Gateway gateway= (Gateway) baseElement;
                gateway.setAsynchronous(getPropertyValueAsBoolean(PROPERTY_ASYNCHRONOUS, elementNode));
                gateway.setNotExclusive(!getPropertyValueAsBoolean(PROPERTY_EXCLUSIVE, elementNode));
                JsonNode flowOrderNode = getProperty(PROPERTY_SEQUENCEFLOW_ORDER, elementNode);

                if (flowOrderNode != null) {
                    flowOrderNode = BpmnJsonConverterUtil.validateIfNodeIsTextual(flowOrderNode);
                    JsonNode orderArray = flowOrderNode.get("sequenceFlowOrder");
                    if (orderArray != null && orderArray.size() > 0) {
                        for (JsonNode orderNode : orderArray) {
                            ExtensionElement orderElement = new ExtensionElement();
                            orderElement.setName("EDITOR_FLOW_ORDER");
                            orderElement.setElementText(orderNode.asText());
                            flowElement.addExtensionElement(orderElement);
                        }
                    }
                }
            }
        }

        if (baseElement instanceof FlowElement) {
            FlowElement flowElement = (FlowElement) baseElement;
            if (flowElement instanceof SequenceFlow) {
                ExtensionElement idExtensionElement = new ExtensionElement();
                idExtensionElement.setName("EDITOR_RESOURCEID");
                idExtensionElement.setElementText(elementNode.get(EDITOR_SHAPE_ID).asText());
                flowElement.addExtensionElement(idExtensionElement);
            }

            if (parentElement instanceof Process) {
                ((Process) parentElement).addFlowElement(flowElement);

            } else if (parentElement instanceof SubProcess) {
                ((SubProcess) parentElement).addFlowElement(flowElement);

            } else if (parentElement instanceof Lane) {
                Lane lane = (Lane) parentElement;
                lane.getFlowReferences().add(flowElement.getId());
                lane.getParentProcess().addFlowElement(flowElement);
            }

        } else if (baseElement instanceof Artifact) {
            Artifact artifact = (Artifact) baseElement;
            if (parentElement instanceof Process) {
                ((Process) parentElement).addArtifact(artifact);

            } else if (parentElement instanceof SubProcess) {
                ((SubProcess) parentElement).addArtifact(artifact);

            } else if (parentElement instanceof Lane) {
                Lane lane = (Lane) parentElement;
                lane.getFlowReferences().add(artifact.getId());
                lane.getParentProcess().addArtifact(artifact);
            }
        }
    }

    protected abstract void convertElementToJson(ObjectNode propertiesNode, BaseElement baseElement,
        BpmnJsonConverterContext converterContext);

    protected abstract BaseElement convertJsonToElement(JsonNode elementNode, JsonNode modelNode, Map<String, JsonNode> shapeMap,
        BpmnJsonConverterContext converterContext);

    protected abstract String getStencilId(BaseElement baseElement);

    protected void setPropertyValue(String name, String value, ObjectNode propertiesNode) {
        if (StringUtils.isNotEmpty(value)) {
            propertiesNode.put(name, value);
        }
    }

    protected void addFormProperties(List<FormProperty> formProperties, ObjectNode propertiesNode) {
        if (CollectionUtils.isEmpty(formProperties)) {
            return;
        }

        ObjectNode formPropertiesNode = objectMapper.createObjectNode();
        ArrayNode propertiesArrayNode = objectMapper.createArrayNode();
        for (FormProperty property : formProperties) {
            ObjectNode propertyItemNode = objectMapper.createObjectNode();
            propertyItemNode.put(PROPERTY_FORM_ID, property.getId());
            propertyItemNode.put(PROPERTY_FORM_NAME, property.getName());
            propertyItemNode.put(PROPERTY_FORM_TYPE, property.getType());
            if (StringUtils.isNotEmpty(property.getExpression())) {
                propertyItemNode.put(PROPERTY_FORM_EXPRESSION, property.getExpression());
            } else {
                propertyItemNode.putNull(PROPERTY_FORM_EXPRESSION);
            }
            if (StringUtils.isNotEmpty(property.getVariable())) {
                propertyItemNode.put(PROPERTY_FORM_VARIABLE, property.getVariable());
            } else {
                propertyItemNode.putNull(PROPERTY_FORM_VARIABLE);
            }
            if (StringUtils.isNotEmpty(property.getDefaultExpression())) {
                propertyItemNode.put(PROPERTY_FORM_DEFAULT, property.getDefaultExpression());
            } else {
                propertyItemNode.putNull(PROPERTY_FORM_DEFAULT);
            }
            if (StringUtils.isNotEmpty(property.getDatePattern())) {
                propertyItemNode.put(PROPERTY_FORM_DATE_PATTERN, property.getDatePattern());
            }
            if (CollectionUtils.isNotEmpty(property.getFormValues())) {
                ArrayNode valuesNode = objectMapper.createArrayNode();
                for (FormValue formValue : property.getFormValues()) {
                    ObjectNode valueNode = objectMapper.createObjectNode();
                    valueNode.put(PROPERTY_FORM_ENUM_VALUES_NAME, formValue.getName());
                    valueNode.put(PROPERTY_FORM_ENUM_VALUES_ID, formValue.getId());
                    valuesNode.add(valueNode);
                }
                propertyItemNode.set(PROPERTY_FORM_ENUM_VALUES, valuesNode);
            }
            propertyItemNode.put(PROPERTY_FORM_REQUIRED, property.isRequired());
            propertyItemNode.put(PROPERTY_FORM_READABLE, property.isReadable());
            propertyItemNode.put(PROPERTY_FORM_WRITABLE, property.isWriteable());

            propertiesArrayNode.add(propertyItemNode);
        }

        formPropertiesNode.set("formProperties", propertiesArrayNode);
        propertiesNode.set(PROPERTY_FORM_PROPERTIES, formPropertiesNode);
    }

    protected void addEventOutParameters(List<ExtensionElement> eventParameterElements, ObjectNode propertiesNode) {
        if (CollectionUtils.isEmpty(eventParameterElements)) {
            return;
        }

        ObjectNode valueNode = objectMapper.createObjectNode();
        ArrayNode arrayNode = objectMapper.createArrayNode();
        for (ExtensionElement element : eventParameterElements) {
            ObjectNode itemNode = objectMapper.createObjectNode();
            itemNode.put(PROPERTY_EVENT_REGISTRY_PARAMETER_EVENTNAME, element.getAttributeValue(null, "source"));
            itemNode.put(PROPERTY_EVENT_REGISTRY_PARAMETER_EVENTTYPE, element.getAttributeValue(null, "sourceType"));
            itemNode.put(PROPERTY_EVENT_REGISTRY_PARAMETER_VARIABLENAME, element.getAttributeValue(null, "target"));

            arrayNode.add(itemNode);
        }

        valueNode.set("outParameters", arrayNode);
        propertiesNode.set(PROPERTY_EVENT_REGISTRY_OUT_PARAMETERS, valueNode);
    }

    protected void addEventOutIOParameters(List<IOParameter> eventParameters, ObjectNode propertiesNode) {
        if (CollectionUtils.isEmpty(eventParameters)) {
            return;
        }

        ObjectNode valueNode = objectMapper.createObjectNode();
        ArrayNode arrayNode = objectMapper.createArrayNode();
        for (IOParameter parameter : eventParameters) {
            ObjectNode itemNode = objectMapper.createObjectNode();
            if (StringUtils.isNotEmpty(parameter.getSourceExpression())) {
                itemNode.put(PROPERTY_EVENT_REGISTRY_PARAMETER_EVENTNAME, parameter.getSourceExpression());
            } else {
                itemNode.put(PROPERTY_EVENT_REGISTRY_PARAMETER_EVENTNAME, parameter.getSource());
            }

            itemNode.put(PROPERTY_EVENT_REGISTRY_PARAMETER_EVENTTYPE, parameter.getAttributeValue(null, "sourceType"));
            itemNode.put(PROPERTY_EVENT_REGISTRY_PARAMETER_VARIABLENAME, parameter.getTarget());

            arrayNode.add(itemNode);
        }

        valueNode.set("outParameters", arrayNode);
        propertiesNode.set(PROPERTY_EVENT_REGISTRY_OUT_PARAMETERS, valueNode);
    }

    protected void addEventInParameters(List<ExtensionElement> eventParameterElements, ObjectNode propertiesNode) {
        if (CollectionUtils.isEmpty(eventParameterElements)) {
            return;
        }

        ObjectNode valueNode = objectMapper.createObjectNode();
        ArrayNode arrayNode = objectMapper.createArrayNode();
        for (ExtensionElement element : eventParameterElements) {
            ObjectNode itemNode = objectMapper.createObjectNode();
            itemNode.put(PROPERTY_EVENT_REGISTRY_PARAMETER_VARIABLENAME, element.getAttributeValue(null, "source"));
            itemNode.put(PROPERTY_EVENT_REGISTRY_PARAMETER_EVENTNAME, element.getAttributeValue(null, "target"));
            itemNode.put(PROPERTY_EVENT_REGISTRY_PARAMETER_EVENTTYPE, element.getAttributeValue(null, "targetType"));

            arrayNode.add(itemNode);
        }

        valueNode.set("inParameters", arrayNode);
        propertiesNode.set(PROPERTY_EVENT_REGISTRY_IN_PARAMETERS, valueNode);
    }

    protected void addEventInIOParameters(List<IOParameter> eventParameters, ObjectNode propertiesNode) {
        if (CollectionUtils.isEmpty(eventParameters)) {
            return;
        }

        ObjectNode valueNode = objectMapper.createObjectNode();
        ArrayNode arrayNode = objectMapper.createArrayNode();
        for (IOParameter parameter : eventParameters) {
            ObjectNode itemNode = objectMapper.createObjectNode();
            if (StringUtils.isNotEmpty(parameter.getSourceExpression())) {
                itemNode.put(PROPERTY_EVENT_REGISTRY_PARAMETER_VARIABLENAME, parameter.getSourceExpression());
            } else {
                itemNode.put(PROPERTY_EVENT_REGISTRY_PARAMETER_VARIABLENAME, parameter.getSource());
            }

            itemNode.put(PROPERTY_EVENT_REGISTRY_PARAMETER_EVENTNAME, parameter.getTarget());
            itemNode.put(PROPERTY_EVENT_REGISTRY_PARAMETER_EVENTTYPE, parameter.getAttributeValue(null, "targetType"));

            arrayNode.add(itemNode);
        }

        valueNode.set("inParameters", arrayNode);
        propertiesNode.set(PROPERTY_EVENT_REGISTRY_IN_PARAMETERS, valueNode);
    }

    protected void addEventCorrelationParameters(List<ExtensionElement> eventParameterElements, ObjectNode propertiesNode) {
        if (CollectionUtils.isEmpty(eventParameterElements)) {
            return;
        }

        ObjectNode valueNode = objectMapper.createObjectNode();
        ArrayNode arrayNode = objectMapper.createArrayNode();
        for (ExtensionElement element : eventParameterElements) {
            ObjectNode itemNode = objectMapper.createObjectNode();
            itemNode.put(PROPERTY_EVENT_REGISTRY_CORRELATIONNAME, element.getAttributeValue(null, "name"));
            itemNode.put(PROPERTY_EVENT_REGISTRY_CORRELATIONTYPE, element.getAttributeValue(null, "nameType"));
            itemNode.put(PROPERTY_EVENT_REGISTRY_CORRELATIONVALUE, element.getAttributeValue(null, "value"));

            arrayNode.add(itemNode);
        }

        valueNode.set("correlationParameters", arrayNode);
        propertiesNode.set(PROPERTY_EVENT_REGISTRY_CORRELATION_PARAMETERS, valueNode);
    }

    protected void addReceiveEventExtensionElements(JsonNode elementNode, FlowElement flowElement) {
        String eventKey = getPropertyValueAsString(PROPERTY_EVENT_REGISTRY_EVENT_KEY, elementNode);
        if (StringUtils.isNotEmpty(eventKey)) {
            addFlowableExtensionElementWithValue("eventType", eventKey, flowElement);
            addFlowableExtensionElementWithValue("eventName", getPropertyValueAsString(PROPERTY_EVENT_REGISTRY_EVENT_NAME, elementNode), flowElement);
            convertJsonToEventOutParameters(elementNode, flowElement);
            convertJsonToEventCorrelationParameters(elementNode, "eventCorrelationParameter", flowElement);

            addFlowableExtensionElementWithValue("channelKey", getPropertyValueAsString(PROPERTY_EVENT_REGISTRY_CHANNEL_KEY, elementNode), flowElement);
            addFlowableExtensionElementWithValue("channelName", getPropertyValueAsString(PROPERTY_EVENT_REGISTRY_CHANNEL_NAME, elementNode), flowElement);
            addFlowableExtensionElementWithValue("channelType", getPropertyValueAsString(PROPERTY_EVENT_REGISTRY_CHANNEL_TYPE, elementNode), flowElement);
            addFlowableExtensionElementWithValue("channelDestination", getPropertyValueAsString(PROPERTY_EVENT_REGISTRY_CHANNEL_DESTINATION, elementNode), flowElement);

            String fixedValue = getPropertyValueAsString(PROPERTY_EVENT_REGISTRY_KEY_DETECTION_FIXED_VALUE, elementNode);
            String jsonField = getPropertyValueAsString(PROPERTY_EVENT_REGISTRY_KEY_DETECTION_JSON_FIELD, elementNode);
            String jsonPointer = getPropertyValueAsString(PROPERTY_EVENT_REGISTRY_KEY_DETECTION_JSON_POINTER, elementNode);
            if (StringUtils.isNotEmpty(fixedValue)) {
                addFlowableExtensionElementWithValue("keyDetectionType", "fixedValue", flowElement);
                addFlowableExtensionElementWithValue("keyDetectionValue", fixedValue, flowElement);

            } else if (StringUtils.isNotEmpty(jsonField)) {
                addFlowableExtensionElementWithValue("keyDetectionType", "jsonField", flowElement);
                addFlowableExtensionElementWithValue("keyDetectionValue", jsonField, flowElement);

            } else if (StringUtils.isNotEmpty(jsonPointer)) {
                addFlowableExtensionElementWithValue("keyDetectionType", "jsonPointer", flowElement);
                addFlowableExtensionElementWithValue("keyDetectionValue", jsonPointer, flowElement);
            }
        }
    }

    protected void addEventRegistryProperties(FlowElement flowElement, ObjectNode propertiesNode) {
        String eventType = getExtensionValue("eventType", flowElement);
        if (StringUtils.isNotEmpty(eventType)) {
            setPropertyValue(PROPERTY_EVENT_REGISTRY_EVENT_KEY, eventType, propertiesNode);
            setPropertyValue(PROPERTY_EVENT_REGISTRY_EVENT_NAME, getExtensionValue("eventName", flowElement), propertiesNode);
            addEventOutParameters(flowElement.getExtensionElements().get("eventOutParameter"), propertiesNode);
            addEventCorrelationParameters(flowElement.getExtensionElements().get("eventCorrelationParameter"), propertiesNode);

            setPropertyValue(PROPERTY_EVENT_REGISTRY_CHANNEL_KEY, getExtensionValue("channelKey", flowElement), propertiesNode);
            setPropertyValue(PROPERTY_EVENT_REGISTRY_CHANNEL_NAME, getExtensionValue("channelName", flowElement), propertiesNode);
            setPropertyValue(PROPERTY_EVENT_REGISTRY_CHANNEL_TYPE, getExtensionValue("channelType", flowElement), propertiesNode);
            setPropertyValue(PROPERTY_EVENT_REGISTRY_CHANNEL_DESTINATION, getExtensionValue("channelDestination", flowElement), propertiesNode);

            String keyDetectionType = getExtensionValue("keyDetectionType", flowElement);
            String keyDetectionValue = getExtensionValue("keyDetectionValue", flowElement);
            if (StringUtils.isNotEmpty(keyDetectionType) && StringUtils.isNotEmpty(keyDetectionValue)) {
                if ("fixedValue".equalsIgnoreCase(keyDetectionType)) {
                    setPropertyValue(PROPERTY_EVENT_REGISTRY_KEY_DETECTION_FIXED_VALUE, keyDetectionValue, propertiesNode);

                } else if ("jsonField".equalsIgnoreCase(keyDetectionType)) {
                    setPropertyValue(PROPERTY_EVENT_REGISTRY_KEY_DETECTION_JSON_FIELD, keyDetectionValue, propertiesNode);

                } else if ("jsonPointer".equalsIgnoreCase(keyDetectionType)) {
                    setPropertyValue(PROPERTY_EVENT_REGISTRY_KEY_DETECTION_JSON_POINTER, keyDetectionValue, propertiesNode);
                }
            }
        }
    }

    protected String getExtensionValue(String name, FlowElement flowElement) {
        List<ExtensionElement> extensionElements = flowElement.getExtensionElements().get(name);
        if (extensionElements != null && extensionElements.size() > 0) {
            return extensionElements.get(0).getElementText();
        }

        return null;
    }

    protected void addMapException(List<MapExceptionEntry> exceptions, ObjectNode propertiesNode) {
        ObjectNode exceptionsNode = objectMapper.createObjectNode();
        ArrayNode itemsNode = objectMapper.createArrayNode();
        for (MapExceptionEntry exception : exceptions) {
            ObjectNode propertyItemNode = objectMapper.createObjectNode();

            if (StringUtils.isNotEmpty(exception.getClassName())) {
                propertyItemNode.put(PROPERTY_SERVICETASK_EXCEPTION_CLASS, exception.getClassName());
            }
            if (StringUtils.isNotEmpty(exception.getErrorCode())) {
                propertyItemNode.put(PROPERTY_SERVICETASK_EXCEPTION_CODE, exception.getErrorCode());
            }
            propertyItemNode.put(PROPERTY_SERVICETASK_EXCEPTION_CHILDREN, Boolean.toString(exception.isAndChildren()));


            itemsNode.add(propertyItemNode);
        }

        exceptionsNode.set("exceptions", itemsNode);
        propertiesNode.set(PROPERTY_SERVICETASK_EXCEPTIONS, exceptionsNode);
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

    protected void addEventProperties(Event event, ObjectNode propertiesNode) {
        List<EventDefinition> eventDefinitions = event.getEventDefinitions();
        if (eventDefinitions.size() == 1) {

            EventDefinition eventDefinition = eventDefinitions.get(0);
            if (eventDefinition instanceof ErrorEventDefinition) {
                ErrorEventDefinition errorDefinition = (ErrorEventDefinition) eventDefinition;
                if (StringUtils.isNotEmpty(errorDefinition.getErrorCode())) {
                    propertiesNode.put(PROPERTY_ERRORREF, errorDefinition.getErrorCode());
                    if (StringUtils.isNotEmpty(errorDefinition.getErrorVariableName())) {
                        propertiesNode.put(PROPERTY_ERROR_VARIABLE_NAME, errorDefinition.getErrorVariableName());
                    }
                    
                    if (errorDefinition.getErrorVariableTransient() != null) {
                        propertiesNode.put(PROPERTY_ERROR_VARIABLE_TRANSIENT, errorDefinition.getErrorVariableTransient());
                    }
                    
                    if (errorDefinition.getErrorVariableLocalScope() != null) {
                        propertiesNode.put(PROPERTY_ERROR_VARIABLE_LOCAL_SCOPE, errorDefinition.getErrorVariableLocalScope());
                    }
                }

            } else if (eventDefinition instanceof SignalEventDefinition) {
                SignalEventDefinition signalDefinition = (SignalEventDefinition) eventDefinition;

                String signalRef = signalDefinition.getSignalRef();
                if (StringUtils.isNotEmpty(signalRef)) {
                    propertiesNode.put(PROPERTY_SIGNALREF, signalRef);
                }
                String signalExpression = signalDefinition.getSignalExpression();
                if (StringUtils.isNotEmpty(signalExpression)) {
                    propertiesNode.put(PROPERTY_SIGNALEXPRESSION, signalExpression);
                }

            } else if (eventDefinition instanceof MessageEventDefinition) {
                MessageEventDefinition messageDefinition = (MessageEventDefinition) eventDefinition;

                String messageRef = messageDefinition.getMessageRef();
                if (StringUtils.isNotEmpty(messageRef)) {
                    propertiesNode.put(PROPERTY_MESSAGEREF, messageRef);
                }
                String messageExpression = messageDefinition.getMessageExpression();
                if (StringUtils.isNotEmpty(messageExpression)) {
                    propertiesNode.put(PROPERTY_MESSAGEEXPRESSION, messageExpression);
                }

            } else if (eventDefinition instanceof ConditionalEventDefinition) {
                ConditionalEventDefinition conditionalDefinition = (ConditionalEventDefinition) eventDefinition;
                if (StringUtils.isNotEmpty(conditionalDefinition.getConditionExpression())) {
                    propertiesNode.put(PROPERTY_CONDITIONAL_EVENT_CONDITION, conditionalDefinition.getConditionExpression());
                }

            } else if (eventDefinition instanceof EscalationEventDefinition) {
                EscalationEventDefinition escalationDefinition = (EscalationEventDefinition) eventDefinition;
                if (StringUtils.isNotEmpty(escalationDefinition.getEscalationCode())) {
                    propertiesNode.put(PROPERTY_ESCALATIONREF, escalationDefinition.getEscalationCode());
                }

            } else if (eventDefinition instanceof TimerEventDefinition) {
                TimerEventDefinition timerDefinition = (TimerEventDefinition) eventDefinition;
                if (StringUtils.isNotEmpty(timerDefinition.getCalendarName())) {
                    propertiesNode.put(PROPERTY_CALENDAR_NAME, timerDefinition.getCalendarName());
                }
                if (StringUtils.isNotEmpty(timerDefinition.getTimeDuration())) {
                    propertiesNode.put(PROPERTY_TIMER_DURATON, timerDefinition.getTimeDuration());
                }
                if (StringUtils.isNotEmpty(timerDefinition.getTimeCycle())) {
                    propertiesNode.put(PROPERTY_TIMER_CYCLE, timerDefinition.getTimeCycle());
                }
                if (StringUtils.isNotEmpty(timerDefinition.getTimeDate())) {
                    propertiesNode.put(PROPERTY_TIMER_DATE, timerDefinition.getTimeDate());
                }
                if (StringUtils.isNotEmpty(timerDefinition.getEndDate())) {
                    propertiesNode.put(PROPERTY_TIMER_CYCLE_END_DATE, timerDefinition.getEndDate());
                }
            } else if (eventDefinition instanceof TerminateEventDefinition) {
                TerminateEventDefinition terminateEventDefinition = (TerminateEventDefinition) eventDefinition;
                propertiesNode.put(PROPERTY_TERMINATE_ALL, terminateEventDefinition.isTerminateAll());
                propertiesNode.put(PROPERTY_TERMINATE_MULTI_INSTANCE, terminateEventDefinition.isTerminateMultiInstance());
                
            } else if (eventDefinition instanceof VariableListenerEventDefinition) {
                VariableListenerEventDefinition variableListenerEventDefinition = (VariableListenerEventDefinition) eventDefinition;
                if (StringUtils.isNotEmpty(variableListenerEventDefinition.getVariableName())) {
                    propertiesNode.put(PROPERTY_VARIABLE_LISTENER_VARIABLE_NAME, variableListenerEventDefinition.getVariableName());
                }
                
                if (StringUtils.isNotEmpty(variableListenerEventDefinition.getVariableChangeType())) {
                    propertiesNode.put(PROPERTY_VARIABLE_LISTENER_VARIABLE_CHANGE_TYPE, variableListenerEventDefinition.getVariableChangeType());
                }

            } else if (eventDefinition instanceof CompensateEventDefinition) {
                CompensateEventDefinition compensateEventDefinition = (CompensateEventDefinition) eventDefinition;
                if (StringUtils.isNotEmpty(compensateEventDefinition.getActivityRef())) {
                    propertiesNode.put(PROPERTY_COMPENSATION_ACTIVITY_REF, compensateEventDefinition.getActivityRef());
                }
            }
        }
    }

    protected void convertJsonToFormProperties(JsonNode objectNode, BaseElement element) {

        JsonNode formPropertiesNode = getProperty(PROPERTY_FORM_PROPERTIES, objectNode);
        if (formPropertiesNode != null) {
            formPropertiesNode = BpmnJsonConverterUtil.validateIfNodeIsTextual(formPropertiesNode);
            JsonNode propertiesArray = formPropertiesNode.get("formProperties");
            if (propertiesArray != null) {
                for (JsonNode formNode : propertiesArray) {
                    JsonNode formIdNode = formNode.get(PROPERTY_FORM_ID);
                    if (formIdNode != null && StringUtils.isNotEmpty(formIdNode.asText())) {

                        FormProperty formProperty = new FormProperty();
                        formProperty.setId(formIdNode.asText());
                        formProperty.setName(getValueAsString(PROPERTY_FORM_NAME, formNode));
                        formProperty.setType(getValueAsString(PROPERTY_FORM_TYPE, formNode));
                        formProperty.setExpression(getValueAsString(PROPERTY_FORM_EXPRESSION, formNode));
                        formProperty.setVariable(getValueAsString(PROPERTY_FORM_VARIABLE, formNode));
                        formProperty.setDefaultExpression(getValueAsString(PROPERTY_FORM_DEFAULT, formNode));
                        if ("date".equalsIgnoreCase(formProperty.getType())) {
                            formProperty.setDatePattern(getValueAsString(PROPERTY_FORM_DATE_PATTERN, formNode));

                        } else if ("enum".equalsIgnoreCase(formProperty.getType())) {
                            JsonNode enumValuesNode = formNode.get(PROPERTY_FORM_ENUM_VALUES);
                            if (enumValuesNode != null) {
                                List<FormValue> formValueList = new ArrayList<>();
                                for (JsonNode enumNode : enumValuesNode) {
                                    if (enumNode.get(PROPERTY_FORM_ENUM_VALUES_ID) != null && !enumNode.get(PROPERTY_FORM_ENUM_VALUES_ID).isNull() && enumNode.get(PROPERTY_FORM_ENUM_VALUES_NAME) != null
                                            && !enumNode.get(PROPERTY_FORM_ENUM_VALUES_NAME).isNull()) {

                                        FormValue formValue = new FormValue();
                                        formValue.setId(enumNode.get(PROPERTY_FORM_ENUM_VALUES_ID).asText());
                                        formValue.setName(enumNode.get(PROPERTY_FORM_ENUM_VALUES_NAME).asText());
                                        formValueList.add(formValue);

                                    } else if (enumNode.get("value") != null && !enumNode.get("value").isNull()) {
                                        FormValue formValue = new FormValue();
                                        formValue.setId(enumNode.get("value").asText());
                                        formValue.setName(enumNode.get("value").asText());
                                        formValueList.add(formValue);
                                    }
                                }
                                formProperty.setFormValues(formValueList);
                            }
                        }

                        formProperty.setRequired(getValueAsBoolean(PROPERTY_FORM_REQUIRED, formNode));
                        formProperty.setReadable(getValueAsBoolean(PROPERTY_FORM_READABLE, formNode));
                        formProperty.setWriteable(getValueAsBoolean(PROPERTY_FORM_WRITABLE, formNode));

                        if (element instanceof StartEvent) {
                            ((StartEvent) element).getFormProperties().add(formProperty);
                        } else if (element instanceof UserTask) {
                            ((UserTask) element).getFormProperties().add(formProperty);
                        }
                    }
                }
            }
        }
    }

    protected void convertJsonToTimerDefinition(JsonNode objectNode, Event event) {

        String timeDate = getPropertyValueAsString(PROPERTY_TIMER_DATE, objectNode);
        String timeCycle = getPropertyValueAsString(PROPERTY_TIMER_CYCLE, objectNode);
        String timeDuration = getPropertyValueAsString(PROPERTY_TIMER_DURATON, objectNode);
        String endDate = getPropertyValueAsString(PROPERTY_TIMER_CYCLE_END_DATE, objectNode);
        String calendarName = getPropertyValueAsString(PROPERTY_CALENDAR_NAME, objectNode);

        TimerEventDefinition eventDefinition = new TimerEventDefinition();
        if (StringUtils.isNotEmpty(calendarName)){
            eventDefinition.setCalendarName(calendarName);
        }
        if (StringUtils.isNotEmpty(timeDate)) {
            eventDefinition.setTimeDate(timeDate);

        } else if (StringUtils.isNotEmpty(timeCycle)) {
            eventDefinition.setTimeCycle(timeCycle);

        } else if (StringUtils.isNotEmpty(timeDuration)) {
            eventDefinition.setTimeDuration(timeDuration);
        }

        if (StringUtils.isNotEmpty(endDate)) {
            eventDefinition.setEndDate(endDate);
        }

        event.getEventDefinitions().add(eventDefinition);
    }

    protected void convertJsonToSignalDefinition(JsonNode objectNode, Event event) {
        SignalEventDefinition eventDefinition = new SignalEventDefinition();

        String signalRef = getPropertyValueAsString(PROPERTY_SIGNALREF, objectNode);
        if (StringUtils.isNotEmpty(signalRef)) {
            eventDefinition.setSignalRef(signalRef);
        }

        String signalExpression = getPropertyValueAsString(PROPERTY_SIGNALEXPRESSION, objectNode);
        if (StringUtils.isNotEmpty(signalExpression)) {
            eventDefinition.setSignalExpression(signalExpression);
        }

        boolean isAsync = getPropertyValueAsBoolean(PROPERTY_ASYNCHRONOUS, objectNode);
        if (isAsync) {
            eventDefinition.setAsync(isAsync);
        }

        event.getEventDefinitions().add(eventDefinition);
    }

    protected void convertJsonToCompensationDefinition(JsonNode objectNode, Event event) {
        CompensateEventDefinition eventDefinition = new CompensateEventDefinition();
        String activityRef = getPropertyValueAsString(PROPERTY_COMPENSATION_ACTIVITY_REF, objectNode);
        eventDefinition.setActivityRef(activityRef);
        event.getEventDefinitions().add(eventDefinition);
    }

    protected void convertJsonToMessageDefinition(JsonNode objectNode, Event event) {
        MessageEventDefinition eventDefinition = new MessageEventDefinition();

        String messageRef = getPropertyValueAsString(PROPERTY_MESSAGEREF, objectNode);
        if (StringUtils.isNotEmpty(messageRef)) {
            eventDefinition.setMessageRef(messageRef);
        }

        String messageExpression = getPropertyValueAsString(PROPERTY_MESSAGEEXPRESSION, objectNode);
        if (StringUtils.isNotEmpty(messageExpression)) {
            eventDefinition.setMessageExpression(messageExpression);
        }

        event.getEventDefinitions().add(eventDefinition);
    }

    protected void convertJsonToEventOutParameters(JsonNode objectNode, FlowElement event) {
        JsonNode parametersNode = getProperty(PROPERTY_EVENT_REGISTRY_OUT_PARAMETERS, objectNode);
        parametersNode = BpmnJsonConverterUtil.validateIfNodeIsTextual(parametersNode);
        if (parametersNode != null && parametersNode.get("outParameters") != null) {
            parametersNode = BpmnJsonConverterUtil.validateIfNodeIsTextual(parametersNode);
            JsonNode parameterArray = parametersNode.get("outParameters");
            for (JsonNode parameterNode : parameterArray) {
                if (parameterNode.get(PROPERTY_EVENT_REGISTRY_PARAMETER_EVENTNAME) != null && !parameterNode.get(PROPERTY_EVENT_REGISTRY_PARAMETER_EVENTNAME).isNull()) {
                    ExtensionElement extensionElement = addFlowableExtensionElement("eventOutParameter", event);
                    String eventName = parameterNode.get(PROPERTY_EVENT_REGISTRY_PARAMETER_EVENTNAME).asText();
                    String eventType = parameterNode.get(PROPERTY_EVENT_REGISTRY_PARAMETER_EVENTTYPE).asText();
                    String variableName = parameterNode.get(PROPERTY_EVENT_REGISTRY_PARAMETER_VARIABLENAME).asText();

                    addExtensionAttribute("source", eventName, extensionElement);
                    addExtensionAttribute("sourceType", eventType, extensionElement);
                    addExtensionAttribute("target", variableName, extensionElement);
                }
            }
        }
    }

    protected void convertJsonToOutIOParameters(JsonNode objectNode, SendEventServiceTask task) {
        JsonNode parametersNode = getProperty(PROPERTY_EVENT_REGISTRY_OUT_PARAMETERS, objectNode);
        parametersNode = BpmnJsonConverterUtil.validateIfNodeIsTextual(parametersNode);
        if (parametersNode != null && parametersNode.get("outParameters") != null) {
            parametersNode = BpmnJsonConverterUtil.validateIfNodeIsTextual(parametersNode);
            JsonNode parameterArray = parametersNode.get("outParameters");
            for (JsonNode parameterNode : parameterArray) {
                if (parameterNode.get(PROPERTY_EVENT_REGISTRY_PARAMETER_EVENTNAME) != null && !parameterNode.get(PROPERTY_EVENT_REGISTRY_PARAMETER_EVENTNAME).isNull()) {
                    IOParameter parameterObject = new IOParameter();

                    String eventName = parameterNode.get(PROPERTY_EVENT_REGISTRY_PARAMETER_EVENTNAME).asText();
                    String eventType = parameterNode.get(PROPERTY_EVENT_REGISTRY_PARAMETER_EVENTTYPE).asText();
                    String variableName = parameterNode.get(PROPERTY_EVENT_REGISTRY_PARAMETER_VARIABLENAME).asText();

                    parameterObject.setSource(eventName);
                    parameterObject.addAttribute(createExtensionAttribute("sourceType", eventType));

                    if ((variableName.contains("${") || variableName.contains("#{")) && variableName.contains("}")) {
                        parameterObject.setTargetExpression(variableName);
                    } else {
                        parameterObject.setTarget(variableName);
                    }

                    task.getEventOutParameters().add(parameterObject);
                }
            }
        }
    }

    protected void convertJsonToInParameters(JsonNode objectNode, Event event) {
        JsonNode parametersNode = getProperty(PROPERTY_EVENT_REGISTRY_IN_PARAMETERS, objectNode);
        parametersNode = BpmnJsonConverterUtil.validateIfNodeIsTextual(parametersNode);
        if (parametersNode != null && parametersNode.get("inParameters") != null) {
            parametersNode = BpmnJsonConverterUtil.validateIfNodeIsTextual(parametersNode);
            JsonNode parameterArray = parametersNode.get("inParameters");
            for (JsonNode parameterNode : parameterArray) {
                if (parameterNode.get(PROPERTY_EVENT_REGISTRY_PARAMETER_VARIABLENAME) != null && !parameterNode.get(PROPERTY_EVENT_REGISTRY_PARAMETER_VARIABLENAME).isNull()) {
                    ExtensionElement extensionElement = addFlowableExtensionElement("eventInParameter", event);
                    String variableName = parameterNode.get(PROPERTY_EVENT_REGISTRY_PARAMETER_VARIABLENAME).asText();
                    String eventName = parameterNode.get(PROPERTY_EVENT_REGISTRY_PARAMETER_EVENTNAME).asText();
                    String eventType = parameterNode.get(PROPERTY_EVENT_REGISTRY_PARAMETER_EVENTTYPE).asText();

                    addExtensionAttribute("source", variableName, extensionElement);
                    addExtensionAttribute("target", eventName, extensionElement);
                    addExtensionAttribute("targetType", eventType, extensionElement);
                }
            }
        }
    }

    protected void convertJsonToInIOParameters(JsonNode objectNode, SendEventServiceTask task) {
        JsonNode parametersNode = getProperty(PROPERTY_EVENT_REGISTRY_IN_PARAMETERS, objectNode);
        parametersNode = BpmnJsonConverterUtil.validateIfNodeIsTextual(parametersNode);
        if (parametersNode != null && parametersNode.get("inParameters") != null) {
            parametersNode = BpmnJsonConverterUtil.validateIfNodeIsTextual(parametersNode);
            JsonNode parameterArray = parametersNode.get("inParameters");
            for (JsonNode parameterNode : parameterArray) {
                if (parameterNode.get(PROPERTY_EVENT_REGISTRY_PARAMETER_VARIABLENAME) != null && !parameterNode.get(PROPERTY_EVENT_REGISTRY_PARAMETER_VARIABLENAME).isNull()) {
                    IOParameter parameterObject = new IOParameter();

                    String variableName = parameterNode.get(PROPERTY_EVENT_REGISTRY_PARAMETER_VARIABLENAME).asText();
                    if ((variableName.contains("${") || variableName.contains("#{")) && variableName.contains("}")) {
                        parameterObject.setSourceExpression(variableName);
                    } else {
                        parameterObject.setSource(variableName);
                    }

                    String eventName = parameterNode.get(PROPERTY_EVENT_REGISTRY_PARAMETER_EVENTNAME).asText();
                    String eventType = parameterNode.get(PROPERTY_EVENT_REGISTRY_PARAMETER_EVENTTYPE).asText();

                    parameterObject.setTarget(eventName);
                    parameterObject.addAttribute(createExtensionAttribute("targetType", eventType));

                    task.getEventInParameters().add(parameterObject);
                }
            }
        }
    }

    protected void convertJsonToEventCorrelationParameters(JsonNode objectNode, String correlationPropertyName, FlowElement flowElement) {
        JsonNode parametersNode = getProperty(PROPERTY_EVENT_REGISTRY_CORRELATION_PARAMETERS, objectNode);
        parametersNode = BpmnJsonConverterUtil.validateIfNodeIsTextual(parametersNode);
        if (parametersNode != null && parametersNode.get("correlationParameters") != null) {
            parametersNode = BpmnJsonConverterUtil.validateIfNodeIsTextual(parametersNode);
            JsonNode parameterArray = parametersNode.get("correlationParameters");
            for (JsonNode parameterNode : parameterArray) {
                if (parameterNode.get(PROPERTY_EVENT_REGISTRY_CORRELATIONNAME) != null && !parameterNode.get(PROPERTY_EVENT_REGISTRY_CORRELATIONNAME).isNull()) {
                    ExtensionElement extensionElement = addFlowableExtensionElement(correlationPropertyName, flowElement);
                    String name = parameterNode.get(PROPERTY_EVENT_REGISTRY_CORRELATIONNAME).asText();
                    String type = parameterNode.get(PROPERTY_EVENT_REGISTRY_CORRELATIONTYPE).asText();
                    String value = parameterNode.get(PROPERTY_EVENT_REGISTRY_CORRELATIONVALUE).asText();

                    addExtensionAttribute("name", name, extensionElement);
                    addExtensionAttribute("type", type, extensionElement);
                    addExtensionAttribute("value", value, extensionElement);
                }
            }
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
    
    protected void convertJsonToVariableListenerDefinition(JsonNode objectNode, Event event) {
        String variableName = getPropertyValueAsString(PROPERTY_VARIABLE_LISTENER_VARIABLE_NAME, objectNode);
        VariableListenerEventDefinition eventDefinition = new VariableListenerEventDefinition();
        if (StringUtils.isNotEmpty(variableName)) {
            eventDefinition.setVariableName(variableName);
            
            String variableChangeType = getPropertyValueAsString(PROPERTY_VARIABLE_LISTENER_VARIABLE_CHANGE_TYPE, objectNode);
            if (StringUtils.isNotEmpty(variableChangeType)) {
                eventDefinition.setVariableChangeType(variableChangeType);
            }
        }
        event.getEventDefinitions().add(eventDefinition);
    }

    protected void convertJsonToConditionalDefinition(JsonNode objectNode, Event event) {
        String condition = getPropertyValueAsString(PROPERTY_CONDITIONAL_EVENT_CONDITION, objectNode);
        ConditionalEventDefinition eventDefinition = new ConditionalEventDefinition();
        if (StringUtils.isNotEmpty(condition)) {
            eventDefinition.setConditionExpression(condition);
        }
        event.getEventDefinitions().add(eventDefinition);
    }

    protected void convertJsonToEscalationDefinition(JsonNode objectNode, Event event) {
        String escalationRef = getPropertyValueAsString(PROPERTY_ESCALATIONREF, objectNode);
        EscalationEventDefinition eventDefinition = new EscalationEventDefinition();
        eventDefinition.setEscalationCode(escalationRef);
        event.getEventDefinitions().add(eventDefinition);
    }

    protected void convertJsonToErrorDefinition(JsonNode objectNode, Event event) {
        String errorRef = getPropertyValueAsString(PROPERTY_ERRORREF, objectNode);
        String errorVariableName = getPropertyValueAsString(PROPERTY_ERROR_VARIABLE_NAME, objectNode);
        Boolean errorVariableLocalScope = JsonConverterUtil.getPropertyValueAsBoolean(PROPERTY_ERROR_VARIABLE_LOCAL_SCOPE, objectNode, true);
        Boolean errorVariableTransient = JsonConverterUtil.getPropertyValueAsBoolean(PROPERTY_ERROR_VARIABLE_TRANSIENT, objectNode, true);

        ErrorEventDefinition eventDefinition = new ErrorEventDefinition();
        eventDefinition.setErrorCode(errorRef);
        eventDefinition.setErrorVariableName(errorVariableName);
        eventDefinition.setErrorVariableLocalScope(errorVariableLocalScope);
        eventDefinition.setErrorVariableTransient(errorVariableTransient);

        event.getEventDefinitions().add(eventDefinition);
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

    protected void addField(String name, JsonNode elementNode, ServiceTask task) {
        FieldExtension field = new FieldExtension();
        field.setFieldName(name.substring(8));
        String value = getPropertyValueAsString(name, elementNode);
        if (StringUtils.isNotEmpty(value)) {
            if ((value.contains("${") || value.contains("#{")) && value.contains("}")) {
                field.setExpression(value);
            } else {
                field.setStringValue(value);
            }
            task.getFieldExtensions().add(field);
        }
    }

    protected void addField(String name, String propertyName, JsonNode elementNode, ServiceTask task) {
        addField(name, propertyName, null, elementNode, task);
    }

    protected void addField(String name, String propertyName, String defaultValue, JsonNode elementNode, ServiceTask task) {
        FieldExtension field = new FieldExtension();
        field.setFieldName(name);
        String value = getPropertyValueAsString(propertyName, elementNode);
        if (StringUtils.isNotEmpty(value)) {
            if ((value.contains("${") || value.contains("#{")) && value.contains("}")) {
                field.setExpression(value);
            } else {
                field.setStringValue(value);
            }
            task.getFieldExtensions().add(field);
        } else if (StringUtils.isNotEmpty(defaultValue)) {
            field.setStringValue(defaultValue);
            task.getFieldExtensions().add(field);
        }
    }

    protected String getPropertyValueAsString(String name, JsonNode objectNode) {
        return JsonConverterUtil.getPropertyValueAsString(name, objectNode);
    }

    protected boolean getPropertyValueAsBoolean(String name, JsonNode objectNode) {
        return JsonConverterUtil.getPropertyValueAsBoolean(name, objectNode);
    }

    protected List<String> getPropertyValueAsList(String name, JsonNode objectNode) {
        return JsonConverterUtil.getPropertyValueAsList(name, objectNode);
    }

    protected JsonNode getProperty(String name, JsonNode objectNode) {
        return JsonConverterUtil.getProperty(name, objectNode);
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

    protected ExtensionElement addFlowableExtensionElement(String name, FlowElement flowElement) {
        ExtensionElement extensionElement = new ExtensionElement();
        extensionElement.setName(name);
        extensionElement.setNamespace("http://flowable.org/bpmn");
        extensionElement.setNamespacePrefix("flowable");
        flowElement.addExtensionElement(extensionElement);
        return extensionElement;
    }

    protected ExtensionElement addFlowableExtensionElementWithValue(String name, String value, FlowElement flowElement) {
        ExtensionElement extensionElement = null;
        if (StringUtils.isNotEmpty(value)) {
            extensionElement = addFlowableExtensionElement(name, flowElement);
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
}
