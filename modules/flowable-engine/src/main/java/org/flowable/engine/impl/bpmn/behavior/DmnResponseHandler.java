package org.flowable.engine.impl.bpmn.behavior;

import java.util.List;
import java.util.Map;

import org.flowable.engine.delegate.DelegateExecution;

public interface DmnResponseHandler {
	
	public void handleResponse(DelegateExecution execution, List<Map<String, Object>> decisionResult, String finaldecisionTableKeyValue);

}
