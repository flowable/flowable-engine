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
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
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

    private static final Logger LOGGER = LoggerFactory.getLogger(HttpActivityBehavior.class);
    
    private static final long serialVersionUID = 1L;
    
    // Validation constants
    public static final String HTTP_TASK_REQUEST_METHOD_REQUIRED = "requestMethod is required";
    public static final String HTTP_TASK_REQUEST_METHOD_INVALID = "requestMethod is invalid";
    public static final String HTTP_TASK_REQUEST_URL_REQUIRED = "requestUrl is required";
    public static final String HTTP_TASK_REQUEST_URL_INVALID = "requestUrl is invalid";
    public static final String HTTP_TASK_REQUEST_HEADERS_INVALID = "requestHeaders are invalid";
    public static final String HTTP_TASK_REQUEST_FIELD_INVALID = "request fields are invalid";

    // HttpRequest method (GET,POST,PUT etc)
    protected Expression requestMethod;
    // HttpRequest URL (http://flowable.org)
    protected Expression requestUrl;
    // Line separated HTTP body headers(Optional)
    protected Expression requestHeaders;
    // HttpRequest body expression (Optional)
    protected Expression requestBody;
    // Timeout in seconds for the body (Optional)
    protected Expression requestTimeout;
    // HttpRequest retry disable HTTP redirects (Optional)
    protected Expression disallowRedirects;
    // Comma separated list of HTTP body status codes to fail, for example 400,5XX (Optional)
    protected Expression failStatusCodes;
    // Comma separated list of HTTP body status codes to handle, for example 404,3XX (Optional)
    protected Expression handleStatusCodes;
    // Flag to ignore exceptions (Optional)
    protected Expression ignoreException;
    // Flag to save request variables. default is false (Optional)
    protected Expression saveRequestVariables;
    // Flag to save response variables. default is false (Optional)
    protected Expression saveResponseParameters;
    // Variable name for response body
    protected Expression responseVariableName;
    // Prefix for the execution variable names (Optional)
    protected Expression resultVariablePrefix;
    // Exception mapping
    protected List<MapExceptionEntry> mapExceptions;

    @Override
    public void execute(DelegateExecution execution) {

        HttpRequest request = new HttpRequest();

        try {
            request.setMethod(getStringFromField(requestMethod, execution));
            request.setUrl(getStringFromField(requestUrl, execution));
            request.setHeaders(getStringFromField(requestHeaders, execution));
            request.setBody(getStringFromField(requestBody, execution));
            request.setTimeout(getIntFromField(requestTimeout, execution));
            request.setNoRedirects(getBooleanFromField(disallowRedirects, execution));
            request.setIgnoreErrors(getBooleanFromField(ignoreException, execution));
            request.setSaveRequest(getBooleanFromField(saveRequestVariables, execution));
            request.setSaveResponse(getBooleanFromField(saveResponseParameters, execution));
            request.setPrefix(getStringFromField(resultVariablePrefix, execution));

            String failCodes = getStringFromField(failStatusCodes, execution);
            String handleCodes = getStringFromField(handleStatusCodes, execution);

            if (failCodes != null) {
                request.setFailCodes(getStringSetFromField(failCodes));
            }
            if (handleCodes != null) {
                request.setHandleCodes(getStringSetFromField(handleCodes));
            }

            validate(request);

            if (request.getPrefix() == null) {
                request.setPrefix(execution.getCurrentFlowElement().getId());
            }

            // Save request fields
            if (request.isSaveRequest()) {
                execution.setVariable(request.getPrefix() + ".requestMethod", request.getMethod());
                execution.setVariable(request.getPrefix() + ".requestUrl", request.getUrl());
                execution.setVariable(request.getPrefix() + ".requestHeaders", request.getHeaders());
                execution.setVariable(request.getPrefix() + ".requestBody", request.getBody());
                execution.setVariable(request.getPrefix() + ".requestTimeout", request.getTimeout());
                execution.setVariable(request.getPrefix() + ".disallowRedirects", request.isNoRedirects());
                execution.setVariable(request.getPrefix() + ".failStatusCodes", failCodes);
                execution.setVariable(request.getPrefix() + ".handleStatusCodes", handleCodes);
                execution.setVariable(request.getPrefix() + ".ignoreException", request.isIgnoreErrors());
                execution.setVariable(request.getPrefix() + ".saveRequestVariables", request.isSaveRequest());
                execution.setVariable(request.getPrefix() + ".saveResponseParameters", request.isSaveResponse());
            }

        } catch (Exception e) {
            if (e instanceof FlowableException) {
                throw (FlowableException) e;
            } else {
                throw new FlowableException(HTTP_TASK_REQUEST_FIELD_INVALID + " in execution " + execution.getId(), e);
            }
        }

        try {
            HttpResponse response = perform(execution, request);
            // Save response fields
            if (response != null) {
                // Save response body only by default
                if (request.isSaveResponse()) {
                    execution.setVariable(request.getPrefix() + ".responseProtocol", response.getProtocol());
                    execution.setVariable(request.getPrefix() + ".responseStatusCode", response.getStatusCode());
                    execution.setVariable(request.getPrefix() + ".responseReason", response.getReason());
                    execution.setVariable(request.getPrefix() + ".responseHeaders", response.getHeaders());
                }
                
                if (!response.isBodyResponseHandled()) {
                    String responseVariableValue = getStringFromField(responseVariableName, execution);
                    if (StringUtils.isNotEmpty(responseVariableValue)) {
                        execution.setVariable(responseVariableValue, response.getBody());
                    } else {
                        execution.setVariable(request.getPrefix() + ".responseBody", response.getBody());
                    }
                }

                // Handle http status codes
                if ((request.isNoRedirects() && response.getStatusCode() >= 300) || response.getStatusCode() >= 400) {

                    String code = Integer.toString(response.statusCode);

                    Set<String> handleCodes = request.getHandleCodes();
                    if (handleCodes != null && !handleCodes.isEmpty()) {
                        if (handleCodes.contains(code)
                                || (code.startsWith("5") && handleCodes.contains("5XX"))
                                || (code.startsWith("4") && handleCodes.contains("4XX"))
                                || (code.startsWith("3") && handleCodes.contains("3XX"))) {
                            
                            ErrorPropagation.propagateError("HTTP" + code, execution);
                            return;
                        }
                    }

                    Set<String> failCodes = request.getFailCodes();
                    if (failCodes != null && !failCodes.isEmpty()) {
                        if (failCodes.contains(code)
                                || (code.startsWith("5") && failCodes.contains("5XX"))
                                || (code.startsWith("4") && failCodes.contains("4XX"))
                                || (code.startsWith("3") && failCodes.contains("3XX"))) {
                            
                            throw new FlowableException("HTTP" + code);
                        }
                    }
                }
            }
            
        } catch (Exception e) {
            if (request.isIgnoreErrors()) {
                LOGGER.info("Error ignored while processing http task in execution {}", execution.getId(), e);
                execution.setVariable(request.getPrefix() + ".errorMessage", e.getMessage());
                
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
    protected abstract HttpResponse perform(final DelegateExecution execution, final HttpRequest request);
    
    protected int getIntFromField(Expression expression, DelegateExecution execution) {
        if (expression != null) {
            Object value = expression.getValue(execution);
            if (value != null) {
                return Integer.parseInt(value.toString());
            }
        }
        return 0;
    }

    protected boolean getBooleanFromField(final Expression expression, final DelegateExecution execution) {
        if (expression != null) {
            Object value = expression.getValue(execution);
            if (value != null) {
                return Boolean.parseBoolean(value.toString());
            }
        }
        return false;
    }

    protected String getStringFromField(final Expression expression, final DelegateExecution execution) {
        if (expression != null) {
            Object value = expression.getValue(execution);
            if (value != null) {
                return value.toString();
            }
        }
        return null;
    }

    protected Set<String> getStringSetFromField(final String field) {
        String[] codes = field.split(",");
        Set<String> codeSet = new HashSet<>(Arrays.asList(codes));
        Collections.addAll(codeSet, codes);
        return codeSet;
    }

    // HttpRequest validation
    protected void validate(final HttpRequest request) throws FlowableException {
        if (request.getMethod() == null) {
            throw new FlowableException(HTTP_TASK_REQUEST_METHOD_REQUIRED);
        }

        switch (request.getMethod()) {
            case "GET":
            case "POST":
            case "PUT":
            case "DELETE":
                break;
            default:
                throw new FlowableException(HTTP_TASK_REQUEST_METHOD_INVALID);
        }

        if (request.getUrl() == null) {
            throw new FlowableException(HTTP_TASK_REQUEST_URL_REQUIRED);
        }
    }

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

    public Expression getSaveResponseParameters() {
        return saveResponseParameters;
    }

    public void setSaveResponseParameters(Expression saveResponseParameters) {
        this.saveResponseParameters = saveResponseParameters;
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
