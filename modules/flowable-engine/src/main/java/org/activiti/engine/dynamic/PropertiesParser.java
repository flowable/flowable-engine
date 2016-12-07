package org.activiti.engine.dynamic;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.activiti.bpmn.model.FlowElement;

/**
 * Created by Pardo David on 5/12/2016.
 */
public interface PropertiesParser {
	ObjectNode parseElement(FlowElement flowElement, ObjectNode flowElementNode, ObjectMapper mapper);
	boolean supports(FlowElement flowElement);
}
