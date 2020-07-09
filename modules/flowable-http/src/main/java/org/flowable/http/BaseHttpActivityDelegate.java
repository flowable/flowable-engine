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

import static org.flowable.http.ExpressionUtils.getBooleanFromField;
import static org.flowable.http.ExpressionUtils.getIntFromField;
import static org.flowable.http.ExpressionUtils.getStringFromField;
import static org.flowable.http.ExpressionUtils.getStringSetFromField;
import static org.flowable.http.HttpActivityExecutor.HTTP_TASK_REQUEST_HEADERS_INVALID;

import java.io.IOException;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.delegate.Expression;
import org.flowable.common.engine.api.variable.VariableContainer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.MissingNode;

/**
 * @author Filip Hrisafov
 */
public abstract class BaseHttpActivityDelegate {

    // HttpRequest method (GET,POST,PUT etc)
    protected Expression requestMethod;
    // HttpRequest URL (http://flowable.org)
    protected Expression requestUrl;
    // Line separated HTTP body headers(Optional)
    protected Expression requestHeaders;
    // HttpRequest body expression (Optional)
    protected Expression requestBody;
    // HttpRequest body encoding expression, for example UTF-8 (Optional)
    protected Expression requestBodyEncoding;
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
    // Flag to save request variables. Default is false (Optional)
    protected Expression saveRequestVariables;
    // Flag to save response variables. Default is false (Optional)
    protected Expression saveResponseParameters;
    // Variable name for response body
    protected Expression responseVariableName;
    // Flag to save the response variables as a transient variable. Default is false (Optional).
    protected Expression saveResponseParametersTransient;
    // Flag to save the response variable as an ObjectNode instead of a String
    protected Expression saveResponseVariableAsJson;
    // Prefix for the execution variable names (Optional)
    protected Expression resultVariablePrefix;

    protected ErrorPropagator errorPropagator;
    protected HttpRequestValidator requestValidator;

    public BaseHttpActivityDelegate(ErrorPropagator errorPropagator) {
        this(errorPropagator, null);
    }

    public BaseHttpActivityDelegate(ErrorPropagator errorPropagator, HttpRequestValidator requestValidator) {
        this.errorPropagator = errorPropagator;
        this.requestValidator = requestValidator == null ? createRequestValidator() : requestValidator;
    }

    protected HttpRequestValidator createRequestValidator() {
        return new DefaultHttpRequestValidator();
    }

    protected HttpRequest createRequest(VariableContainer variableContainer, String prefix) {
        HttpRequest request = new HttpRequest();

        request.setMethod(getStringFromField(requestMethod, variableContainer));
        request.setUrl(getStringFromField(requestUrl, variableContainer));
        request.setHttpHeaders(getRequestHeaders(variableContainer));
        request.setBody(getStringFromField(requestBody, variableContainer));
        request.setBodyEncoding(getStringFromField(requestBodyEncoding, variableContainer));
        request.setTimeout(getIntFromField(requestTimeout, variableContainer));
        request.setNoRedirects(getBooleanFromField(disallowRedirects, variableContainer));
        request.setIgnoreErrors(getBooleanFromField(ignoreException, variableContainer));
        request.setSaveRequest(getBooleanFromField(saveRequestVariables, variableContainer));
        request.setSaveResponse(getBooleanFromField(saveResponseParameters, variableContainer));
        request.setSaveResponseTransient(getBooleanFromField(saveResponseParametersTransient, variableContainer));
        request.setSaveResponseAsJson(getBooleanFromField(saveResponseVariableAsJson, variableContainer));
        request.setPrefix(getStringFromField(resultVariablePrefix, variableContainer));

        String failCodes = getStringFromField(failStatusCodes, variableContainer);
        String handleCodes = getStringFromField(handleStatusCodes, variableContainer);

        if (failCodes != null) {
            request.setFailCodes(getStringSetFromField(failCodes));
        }
        if (handleCodes != null) {
            request.setHandleCodes(getStringSetFromField(handleCodes));
        }

        if (request.getPrefix() == null) {
            request.setPrefix(prefix);
        }

        // Save request fields
        if (request.isSaveRequest()) {
            variableContainer.setVariable(request.getPrefix() + "RequestMethod", request.getMethod());
            variableContainer.setVariable(request.getPrefix() + "RequestUrl", request.getUrl());
            variableContainer.setVariable(request.getPrefix() + "RequestHeaders", request.getHttpHeadersAsString());
            variableContainer.setVariable(request.getPrefix() + "RequestBody", request.getBody());
            variableContainer.setVariable(request.getPrefix() + "RequestBodyEncoding", request.getBodyEncoding());
            variableContainer.setVariable(request.getPrefix() + "RequestTimeout", request.getTimeout());
            variableContainer.setVariable(request.getPrefix() + "DisallowRedirects", request.isNoRedirects());
            variableContainer.setVariable(request.getPrefix() + "FailStatusCodes", failCodes);
            variableContainer.setVariable(request.getPrefix() + "HandleStatusCodes", handleCodes);
            variableContainer.setVariable(request.getPrefix() + "IgnoreException", request.isIgnoreErrors());
            variableContainer.setVariable(request.getPrefix() + "SaveRequestVariables", request.isSaveRequest());
            variableContainer.setVariable(request.getPrefix() + "SaveResponseParameters", request.isSaveResponse());
        }
        return request;
    }

    protected void saveResponseFields(VariableContainer variableContainer, HttpRequest request, HttpResponse response, ObjectMapper objectMapper)
            throws IOException {
        // Save response fields
        if (response != null) {
            // Save response body only by default
            if (request.isSaveResponse()) {
                if (request.isSaveResponseTransient()) {
                    variableContainer.setTransientVariable(request.getPrefix() + "ResponseProtocol", response.getProtocol());
                    variableContainer.setTransientVariable(request.getPrefix() + "ResponseStatusCode", response.getStatusCode());
                    variableContainer.setTransientVariable(request.getPrefix() + "ResponseReason", response.getReason());
                    variableContainer.setTransientVariable(request.getPrefix() + "ResponseHeaders", response.getHttpHeadersAsString());
                } else {
                    variableContainer.setVariable(request.getPrefix() + "ResponseProtocol", response.getProtocol());
                    variableContainer.setVariable(request.getPrefix() + "ResponseStatusCode", response.getStatusCode());
                    variableContainer.setVariable(request.getPrefix() + "ResponseReason", response.getReason());
                    variableContainer.setVariable(request.getPrefix() + "ResponseHeaders", response.getHttpHeadersAsString());
                }
            }

            if (!response.isBodyResponseHandled()) {
                String responseVariableName = ExpressionUtils.getStringFromField(this.responseVariableName, variableContainer);
                String varName = StringUtils.isNotEmpty(responseVariableName) ? responseVariableName : request.getPrefix() + "ResponseBody";
                Object varValue = request.isSaveResponseAsJson() && response.getBody() != null ? objectMapper.readTree(response.getBody()) : response.getBody();
                if (varValue instanceof MissingNode) {
                    varValue = null;
                }
                if (request.isSaveResponseTransient()) {
                    variableContainer.setTransientVariable(varName, varValue);
                } else {
                    variableContainer.setVariable(varName, varValue);
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

                        errorPropagator.propagateError(variableContainer, code);
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
    }

    protected HttpHeaders getRequestHeaders(VariableContainer variableContainer) {
        try {
            String headersString = getStringFromField(requestHeaders, variableContainer);
            return HttpHeaders.parseFromString(headersString);
        } catch (FlowableIllegalArgumentException ex) {
            throw new FlowableException(HTTP_TASK_REQUEST_HEADERS_INVALID, ex);
        }
    }
}
