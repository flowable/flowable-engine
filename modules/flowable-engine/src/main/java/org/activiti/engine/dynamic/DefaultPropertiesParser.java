package org.activiti.engine.dynamic;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.activiti.bpmn.model.FlowElement;

/**
 * Used for {@link FlowElement} currently not supported by the {@link org.activiti.engine.DynamicBpmnService}
 * and elements who are not parsed.
 *
 * Created by Pardo David on 5/12/2016.
 */
public class DefaultPropertiesParser extends BasePropertiesParser {
	@Override
	protected ObjectNode createPropertiesNode(FlowElement flowElement, ObjectNode flowElementNode, ObjectMapper objectMapper) {
		return null;
	}

	@Override
	public boolean supports(FlowElement flowElement) {
		return false;
	}
}
