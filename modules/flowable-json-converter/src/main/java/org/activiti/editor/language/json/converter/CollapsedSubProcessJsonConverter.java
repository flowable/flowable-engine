package org.activiti.editor.language.json.converter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.activiti.bpmn.model.*;
import org.activiti.editor.language.json.converter.util.JsonConverterUtil;

import java.util.List;
import java.util.Map;

/**
 * Created by Pardo David on 6/01/2017.
 */
public class CollapsedSubProcessJsonConverter extends BaseBpmnJsonConverter {

	public static void fillTypes(Map<String, Class<? extends BaseBpmnJsonConverter>> convertersToBpmnMap,
								 Map<Class<? extends BaseElement>, Class<? extends BaseBpmnJsonConverter>> convertersToJsonMap) {

		fillJsonTypes(convertersToBpmnMap);
		fillBpmnTypes(convertersToJsonMap);
	}

	public static void fillJsonTypes(Map<String, Class<? extends BaseBpmnJsonConverter>> convertersToBpmnMap) {
		convertersToBpmnMap.put(STENCIL_COLLAPSED_SUB_PROCESS, CollapsedSubProcessJsonConverter.class);
	}

	public static void fillBpmnTypes(Map<Class<? extends BaseElement>, Class<? extends BaseBpmnJsonConverter>> convertersToJsonMap) {
		convertersToJsonMap.put(CollapsedSubProcess.class, CollapsedSubProcessJsonConverter.class);
	}

	@Override
	protected void convertElementToJson(ObjectNode propertiesNode, BaseElement baseElement) {
		//code based on the subprocessconverter.
		CollapsedSubProcess subProcess = (CollapsedSubProcess) baseElement;
		propertiesNode.put("activitytype", "SubprocessCollapsed");
		propertiesNode.put("subprocesstype", "collapsed");

		//create the childshapes array.
		ArrayNode subProcessShapesArrayNode = objectMapper.createArrayNode();
		GraphicInfo graphicInfo = model.getGraphicInfo(subProcess.getId());
		processor.processFlowElements(subProcess, model, subProcessShapesArrayNode,0,0);

		//a collapsed subprocess also has canvas information. To avoid complex recalculations the info was stored in the bpmnModel
		ObjectNode canvasNode = objectMapper.createObjectNode();

		canvasNode.put(EDITOR_SHAPE_ID,subProcess.getId());
		canvasNode.put(EDITOR_CHILD_SHAPES,subProcessShapesArrayNode);
		canvasNode.set(EDITOR_STENCIL,objectMapper.createObjectNode().put(EDITOR_STENCIL_ID,"BPMNDiagram"));

		GraphicInfo canvasGraphicInfo = model.getGraphicInfo(subProcess.getId() + "-canvas");
		ObjectNode boundsNode = objectMapper.createObjectNode();
		ObjectNode lowerRightBoundsNode = objectMapper.createObjectNode();
		lowerRightBoundsNode.put(EDITOR_BOUNDS_X,canvasGraphicInfo.getWidth());
		lowerRightBoundsNode.put(EDITOR_BOUNDS_Y,canvasGraphicInfo.getHeight());

		ObjectNode upperLeftBoundsNode = objectMapper.createObjectNode();
		upperLeftBoundsNode.put(EDITOR_BOUNDS_X,0);
		upperLeftBoundsNode.put(EDITOR_BOUNDS_Y,0);

		boundsNode.set(EDITOR_BOUNDS_UPPER_LEFT,upperLeftBoundsNode);
		boundsNode.set(EDITOR_BOUNDS_LOWER_RIGHT,lowerRightBoundsNode);

		canvasNode.put(EDITOR_BOUNDS,boundsNode);

		ArrayNode canvasChildShapesNode = objectMapper.createArrayNode();
		canvasChildShapesNode.add(canvasNode);
		flowElementNode.put("childShapes", canvasChildShapesNode);

		//TODO: transaction is not supported for collapsed subprocess yet.

		BpmnJsonConverterUtil.convertDataPropertiesToJson(subProcess.getDataObjects(), propertiesNode);
	}

	@Override
	protected BaseElement convertJsonToElement(JsonNode elementNode, JsonNode modelNode, Map<String, JsonNode> shapeMap) {
		CollapsedSubProcess subProcess = new CollapsedSubProcess();
		JsonNode childShapesArray = elementNode.get(EDITOR_CHILD_SHAPES);
		//not sure if we can avoid this... It would be nice if canvas information is stored...
		if(childShapesArray.isArray() && ((ArrayNode)childShapesArray).size() > 0){
			JsonNode canvasNode = childShapesArray.get(0);
			double x = canvasNode.get(EDITOR_BOUNDS).get(EDITOR_BOUNDS_LOWER_RIGHT).get(EDITOR_BOUNDS_X).doubleValue();
			double y = canvasNode.get(EDITOR_BOUNDS).get(EDITOR_BOUNDS_LOWER_RIGHT).get(EDITOR_BOUNDS_Y).doubleValue();

			GraphicInfo graphicInfo = new GraphicInfo();
			graphicInfo.setX(0.0);
			graphicInfo.setY(0.0);
			graphicInfo.setWidth(x);
			graphicInfo.setHeight(y);
			model.addGraphicInfo(elementNode.get("resourceId").asText() + "-canvas",graphicInfo);

			processor.processJsonElements(canvasNode.get(EDITOR_CHILD_SHAPES), modelNode, subProcess, shapeMap, model);
			JsonNode processDataPropertiesNode = elementNode.get(EDITOR_SHAPE_PROPERTIES).get(PROPERTY_DATA_PROPERTIES);
			if (processDataPropertiesNode != null) {
				List<ValuedDataObject> dataObjects = BpmnJsonConverterUtil.convertJsonToDataProperties(processDataPropertiesNode, subProcess);
				subProcess.setDataObjects(dataObjects);
				subProcess.getFlowElements().addAll(dataObjects);
			}
		}

		return subProcess;
	}

	@Override
	protected String getStencilId(BaseElement baseElement) {
		return STENCIL_COLLAPSED_SUB_PROCESS;
	}
}
