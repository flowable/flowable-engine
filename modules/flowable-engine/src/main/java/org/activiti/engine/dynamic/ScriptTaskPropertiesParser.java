package org.activiti.engine.dynamic;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.activiti.bpmn.model.FlowElement;
import org.activiti.bpmn.model.ScriptTask;

/**
 * Created by Pardo David on 5/12/2016.
 */
public class ScriptTaskPropertiesParser extends BasePropertiesParser {
	@Override
	protected ObjectNode createPropertiesNode(FlowElement flowElement, ObjectNode flowElementNode, ObjectMapper objectMapper) {
		ScriptTask scriptTask = (ScriptTask) flowElement;

		ObjectNode scriptTextNode = objectMapper.createObjectNode();
		putPropertyValue(BPMN_MODEL_VALUE,scriptTask.getScript(),scriptTextNode);
		putPropertyValue(DYNAMIC_VALUE,flowElementNode.path(SCRIPT_TASK_SCRIPT).textValue(),scriptTextNode);


		ObjectNode propertiesNode = objectMapper.createObjectNode();
		propertiesNode.put(SCRIPT_TASK_SCRIPT,scriptTextNode);
		return propertiesNode;
	}

	@Override
	public boolean supports(FlowElement flowElement) {
		return flowElement instanceof ScriptTask;
	}
}
