package org.activiti.engine.impl.bpmn.parser.handler;

import org.activiti.bpmn.model.BaseElement;
import org.activiti.bpmn.model.CollapsedSubProcess;

/**
 * Created by Pardo David on 17/01/2017.
 */
public class CollapsedSubprocesParseHandler  extends SubProcessParseHandler{
	@Override
	protected Class<? extends BaseElement> getHandledType() {
		return CollapsedSubProcess.class;
	}
}
