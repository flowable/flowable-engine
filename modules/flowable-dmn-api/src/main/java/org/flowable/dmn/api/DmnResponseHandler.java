package org.flowable.dmn.api;

import java.util.List;
import java.util.Map;

public interface DmnResponseHandler {

	Map<String, Object> handleResponse(List<Map<String, Object>> decisionResult, String finaldecisionTableKeyValue);

}
