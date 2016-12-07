package org.activiti.engine.dynamic;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.activiti.bpmn.model.FlowElement;
import org.activiti.engine.DynamicBpmnConstants;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

/**
 * Created by Pardo David on 5/12/2016.
 */
public abstract class BasePropertiesParser implements PropertiesParser, DynamicBpmnConstants, PropertiesParserConstants {

	@Override
	public ObjectNode parseElement(FlowElement flowElement, ObjectNode flowElementNode, ObjectMapper mapper) {
		ObjectNode resultNode = mapper.createObjectNode();
		resultNode.put(ELEMENT_ID,flowElement.getId());
		resultNode.put(ELEMENT_TYPE, flowElement.getClass().getSimpleName());
		if(supports(flowElement)){
			resultNode.put(ELEMENT_PROPERTIES, createPropertiesNode(flowElement,flowElementNode,mapper));
		}
		return resultNode;
	}

	protected abstract ObjectNode createPropertiesNode(FlowElement flowElement, ObjectNode flowElementNode, ObjectMapper objectMapper);
	public abstract boolean supports(FlowElement flowElement);

	protected void putPropertyValue(String key, String value, ObjectNode propertiesNode){
		if (StringUtils.isNotBlank(value)) {
			propertiesNode.put(key,value);
		}
	}

	protected void putPropertyValue(String key, List<String> values, ObjectNode propertiesNode){
		//we don't set a node value if the collection is null.
		//An empty collection is a indicator. if a task has candidate users you can only overrule it by putting an empty array as dynamic candidate users
		if(values != null){
			ArrayNode arrayNode = propertiesNode.putArray(key);
			for(String value : values){
				arrayNode.add(value);
			}
		}
	}

	protected void putPropertyValue(String key, JsonNode node, ObjectNode propertiesNode){
		if(node != null){
			if(!node.isMissingNode() && !node.isNull()){
				propertiesNode.set(key,node);
			}
		}
	}
}
