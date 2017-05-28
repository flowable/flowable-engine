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
package org.flowable.http;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.flowable.bpmn.model.MapExceptionEntry;
import org.flowable.engine.common.api.FlowableException;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.Expression;
import org.flowable.engine.impl.bpmn.behavior.AbstractBpmnActivityBehavior;
import org.flowable.engine.impl.bpmn.helper.ErrorPropagation;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An activity behavior for HTTP requests.
 *
 * @author Harsha Teja Kanna.
 */
public abstract class HttpActivityBehavior extends AbstractBpmnActivityBehavior {

    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(HttpActivityBehavior.class);

    // HttpRequest method (GET,POST,PUT etc)
    private Expression requestMethod;
    // HttpRequest URL (http://flowable.org)
    private Expression requestUrl;
    // Line separated HTTP body headers(Optional)
    private Expression requestHeaders;
    // HttpRequest body expression (Optional)
    private Expression requestBody;
    // Timeout in seconds for the body (Optional)
    private Expression requestTimeout;
    // HttpRequest retry disable HTTP redirects (Optional)
    private Expression disallowRedirects;
    // Comma separated list of Http body status codes to fail, for example 400,5XX (Optional)
    private Expression failStatusCodes;
    // Comma separated list of Http body status codes to handle, for example 404,3XX (Optional)
    private Expression handleStatusCodes;
    // Flag to ignore exceptions (Optional)
    private Expression ignoreException;
    // Flag to save request variables. default is false (Optional)
    private Expression saveRequestVariables;
    // Prefix for the execution variable names (Optional)
    private Expression resultVariablePrefix;
    // Exception mapping
    private List<MapExceptionEntry> mapExceptions;

    // Validation constants
    public static final String HTTP_TASK_REQUEST_METHOD_REQUIRED = "requestMethod is required";
    public static final String HTTP_TASK_REQUEST_METHOD_INVALID = "requestMethod is invalid";
    public static final String HTTP_TASK_REQUEST_URL_REQUIRED = "requestUrl is required";
    public static final String HTTP_TASK_REQUEST_URL_INVALID = "requestUrl is invalid";

    private int getIntFromField(Expression expression, DelegateExecution execution) {
        if (expression != null) {
            Object value = expression.getValue(execution);
            if (value != null) {
                return Integer.parseInt(value.toString());
            }
        }
        return 0;
    }

    private String getStringFromField(Expression expression, DelegateExecution execution) {
        if (expression != null) {
            Object value = expression.getValue(execution);
            if (value != null) {
                return value.toString();
            }
        }
        return null;
    }

    private Set<String> getStringSetFromField(Expression expression, DelegateExecution execution) {
        if (expression != null) {
            Object value = expression.getValue(execution);
            if (value != null) {
                String[] codes = value.toString().split(",");
                return new HashSet<String>(Arrays.asList(codes));
            }
        }
        return null;
    }

    // HttpRequest validation
    private void validate(final HttpRequest request) throws FlowableException {
        if (request.method == null) {
            throw new FlowableException(HTTP_TASK_REQUEST_METHOD_REQUIRED);
        }

        switch (request.method) {
            case "GET":
            case "POST":
            case "PUT":
            case "DELETE":
                break;
            default:
                throw new FlowableException(HTTP_TASK_REQUEST_METHOD_INVALID);
        }

        if (request.url == null) {
            throw new FlowableException(HTTP_TASK_REQUEST_URL_REQUIRED);
        }
    }

    @Override
    public void execute(DelegateExecution execution) {

        HttpRequest request = new HttpRequest();

        try {
            request.method = getStringFromField(requestMethod, execution);
            request.url = getStringFromField(requestUrl, execution);
            request.headers = getStringFromField(requestHeaders, execution);
            request.body = getStringFromField(requestBody, execution);
            request.timeout = getIntFromField(requestTimeout, execution);
            request.noRedirects = Boolean.parseBoolean(getStringFromField(disallowRedirects, execution));
            request.failCodes = getStringSetFromField(failStatusCodes, execution);
            request.handleCodes = getStringSetFromField(handleStatusCodes, execution);
            request.ignoreErrors = Boolean.parseBoolean(getStringFromField(ignoreException, execution));
            request.saveRequest = Boolean.parseBoolean(getStringFromField(saveRequestVariables, execution));
            request.prefix = getStringFromField(resultVariablePrefix, execution);

        } catch (Exception e) {
            throw new FlowableException("Invalid fields in execution " + execution.getId(), e);
        }

        validate(request);

        if (request.prefix == null) {
            request.prefix = execution.getCurrentFlowElement().getId();
        }

        // Save request fields
        if (request.saveRequest) {
            execution.setVariable(request.prefix + ".requestMethod", request.method);
            execution.setVariable(request.prefix + ".requestUrl", request.url);
            execution.setVariable(request.prefix + ".requestHeaders", request.headers);
            execution.setVariable(request.prefix + ".requestBody", request.body);
            execution.setVariable(request.prefix + ".requestTimeout", request.timeout);
            execution.setVariable(request.prefix + ".disallowRedirects", request.noRedirects);
            execution.setVariable(request.prefix + ".ignoreException", request.ignoreErrors);
            execution.setVariable(request.prefix + ".saveRequestVariables", request.saveRequest);
        }

        try {
            HttpResponse response = perform(request);
            // Save response fields
            if (response != null) {
                execution.setVariable(request.prefix + ".responseStatusCode", response.statusCode);
                execution.setVariable(request.prefix + ".responseHeaders", response.headers);
                execution.setVariable(request.prefix + ".responseBody", response.body);

                // Handle http status codes
                if ((request.noRedirects && response.statusCode >= 300) || response.statusCode >= 400) {

                    String code = Integer.toString(response.statusCode);

                    Set<String> handleCodes = request.handleCodes;
                    if (handleCodes != null && !handleCodes.isEmpty()) {
                        if (handleCodes.contains(code)
                                || (code.startsWith("5") && (handleCodes.contains("5XX")))
                                || (code.startsWith("4") && (handleCodes.contains("4XX")))
                                || (code.startsWith("3") && (handleCodes.contains("3XX")))) {
                            ErrorPropagation.propagateError("HTTP" + code, execution);
                            return;
                        }
                    }

                    Set<String> failCodes = request.failCodes;
                    if (failCodes != null && !failCodes.isEmpty()) {
                        if (failCodes.contains(code)
                                || (code.startsWith("5") && (failCodes.contains("5XX")))
                                || (code.startsWith("4") && (failCodes.contains("4XX")))
                                || (code.startsWith("3") && (failCodes.contains("3XX")))) {
                            throw new FlowableException("HTTP" + code);
                        }
                    }
                }
            }
        } catch (Exception e) {
            boolean ignore = Boolean.parseBoolean(getStringFromField(ignoreException, execution));
            if (ignore) {
                log.info("Error ignored while processing http task in execution " + execution.getId(), e);
                execution.setVariable(request.prefix + ".errorMessage", e.getMessage());
            } else {
                if (ErrorPropagation.mapException(e, (ExecutionEntity) execution, mapExceptions)) {
                    return;
                } else {
                    if (e instanceof FlowableException) {
                        throw (FlowableException) e;
                    } else {
                        throw new FlowableException("Error occurred while processing http task in execution " + execution.getId(), e);
                    }
                }
            }
        }

        leave(execution);
    }

    /**
     * This should be implemented by subclasses for actual HTTP request handling.
     *
     * @param execution
     * @param request
     * @return
     */
    protected abstract HttpResponse perform(HttpRequest request);

    // Setters and getters
    public Expression getRequestMethod() {
        return requestMethod;
    }

    public void setRequestMethod(Expression requestMethod) {
        this.requestMethod = requestMethod;
    }

    public Expression getRequestUrl() {
        return requestUrl;
    }

    public void setRequestUrl(Expression requestUrl) {
        this.requestUrl = requestUrl;
    }

    public Expression getRequestHeaders() {
        return requestHeaders;
    }

    public void setRequestHeaders(Expression requestHeaders) {
        this.requestHeaders = requestHeaders;
    }

    public Expression getRequestBody() {
        return requestBody;
    }

    public void setRequestBody(Expression requestBody) {
        this.requestBody = requestBody;
    }

    public Expression getRequestTimeout() {
        return requestTimeout;
    }

    public void setRequestTimeout(Expression requestTimeout) {
        this.requestTimeout = requestTimeout;
    }

    public Expression getDisallowRedirects() {
        return disallowRedirects;
    }

    public void setDisallowRedirects(Expression disallowRedirects) {
        this.disallowRedirects = disallowRedirects;
    }

    public Expression getFailStatusCodes() {
        return failStatusCodes;
    }

    public void setFailStatusCodes(Expression failStatusCodes) {
        this.failStatusCodes = failStatusCodes;
    }

    public Expression getHandleStatusCodes() {
        return handleStatusCodes;
    }

    public void setHandleStatusCodes(Expression handleStatusCodes) {
        this.handleStatusCodes = handleStatusCodes;
    }

    public Expression getIgnoreException() {
        return ignoreException;
    }

    public void setIgnoreException(Expression ignoreException) {
        this.ignoreException = ignoreException;
    }

    public Expression getSaveRequestVariables() {
        return saveRequestVariables;
    }

    public void setSaveRequestVariables(Expression saveRequestVariables) {
        this.saveRequestVariables = saveRequestVariables;
    }

    public Expression getResultVariablePrefix() {
        return resultVariablePrefix;
    }

    public void setResultVariablePrefix(Expression resultVariablePrefix) {
        this.resultVariablePrefix = resultVariablePrefix;
    }

    public List<MapExceptionEntry> getMapExceptions() {
        return mapExceptions;
    }

    public void setMapExceptions(List<MapExceptionEntry> mapExceptions) {
        this.mapExceptions = mapExceptions;
    }
}
