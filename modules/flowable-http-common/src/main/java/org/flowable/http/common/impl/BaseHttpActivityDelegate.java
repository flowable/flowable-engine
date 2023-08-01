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
package org.flowable.http.common.impl;

import java.io.IOException;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.apache.commons.lang3.StringUtils;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.async.AsyncTaskInvoker;
import org.flowable.common.engine.api.delegate.Expression;
import org.flowable.common.engine.api.variable.VariableContainer;
import org.flowable.http.common.api.HttpHeaders;
import org.flowable.http.common.api.HttpRequest;
import org.flowable.http.common.api.HttpResponse;
import org.flowable.http.common.api.HttpResponseVariableType;
import org.flowable.http.common.api.client.AsyncExecutableHttpRequest;
import org.flowable.http.common.api.client.ExecutableHttpRequest;
import org.flowable.http.common.api.client.FlowableHttpClient;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.MissingNode;
import com.fasterxml.jackson.databind.node.NullNode;

/**
 * @author Filip Hrisafov
 */
public abstract class BaseHttpActivityDelegate {

    // Validation constants
    public static final String HTTP_TASK_REQUEST_METHOD_REQUIRED = "requestMethod is required";
    public static final String HTTP_TASK_REQUEST_METHOD_INVALID = "requestMethod is invalid";
    public static final String HTTP_TASK_REQUEST_URL_REQUIRED = "requestUrl is required";
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
    /**
     * @deprecated use {@link #responseVariableType} instead
     */
    // Flag to save the response variable as an ObjectNode instead of a String
    @Deprecated
    protected Expression saveResponseVariableAsJson;

    protected Expression responseVariableType;
    // Prefix for the execution variable names (Optional)
    protected Expression resultVariablePrefix;

    protected FlowableHttpClient httpClient;

    public BaseHttpActivityDelegate() {
        this(null);
    }

    public BaseHttpActivityDelegate(FlowableHttpClient httpClient) {
        this.httpClient = httpClient == null ? createHttpClient() : httpClient;
    }

    protected abstract FlowableHttpClient createHttpClient();

    protected RequestData createRequest(VariableContainer variableContainer, String fallbackPrefix) {
        HttpRequest request = new HttpRequest();
        RequestData requestData = new RequestData();
        requestData.setHttpRequest(request);

        request.setMethod(ExpressionUtils.getStringFromField(requestMethod, variableContainer));
        request.setUrl(ExpressionUtils.getStringFromField(requestUrl, variableContainer));
        request.setHttpHeaders(getRequestHeaders(variableContainer));
        request.setBody(ExpressionUtils.getStringFromField(requestBody, variableContainer));
        request.setBodyEncoding(ExpressionUtils.getStringFromField(requestBodyEncoding, variableContainer));
        request.setTimeout(ExpressionUtils.getIntFromField(requestTimeout, variableContainer));
        request.setNoRedirects(ExpressionUtils.getBooleanFromField(disallowRedirects, variableContainer));
        requestData.setIgnoreErrors(ExpressionUtils.getBooleanFromField(ignoreException, variableContainer));
        requestData.setSaveRequest(ExpressionUtils.getBooleanFromField(saveRequestVariables, variableContainer));
        requestData.setSaveResponse(ExpressionUtils.getBooleanFromField(saveResponseParameters, variableContainer));
        requestData.setSaveResponseTransient(ExpressionUtils.getBooleanFromField(saveResponseParametersTransient, variableContainer));
        requestData.setResponseVariableType(determineResponseVariableType(variableContainer));
        requestData.setPrefix(ExpressionUtils.getStringFromField(resultVariablePrefix, variableContainer));

        String failCodes = ExpressionUtils.getStringFromField(failStatusCodes, variableContainer);
        String handleCodes = ExpressionUtils.getStringFromField(handleStatusCodes, variableContainer);

        if (failCodes != null) {
            requestData.setFailCodes(ExpressionUtils.getStringSetFromField(failCodes));
        }
        if (handleCodes != null) {
            requestData.setHandleCodes(ExpressionUtils.getStringSetFromField(handleCodes));
        }

        if (requestData.getPrefix() == null) {
            requestData.setPrefix(fallbackPrefix);
        }

        // Save request fields
        if (requestData.isSaveRequest()) {
            String prefix = requestData.getPrefix();
            variableContainer.setVariable(prefix + "RequestMethod", request.getMethod());
            variableContainer.setVariable(prefix + "RequestUrl", request.getUrl());
            variableContainer.setVariable(prefix + "RequestHeaders", request.getHttpHeadersAsString());
            variableContainer.setVariable(prefix + "RequestBody", request.getBody());
            variableContainer.setVariable(prefix + "RequestBodyEncoding", request.getBodyEncoding());
            variableContainer.setVariable(prefix + "RequestTimeout", request.getTimeout());
            variableContainer.setVariable(prefix + "DisallowRedirects", request.isNoRedirects());
            variableContainer.setVariable(prefix + "FailStatusCodes", failCodes);
            variableContainer.setVariable(prefix + "HandleStatusCodes", handleCodes);
            variableContainer.setVariable(prefix + "IgnoreException", requestData.isIgnoreErrors());
            variableContainer.setVariable(prefix + "SaveRequestVariables", requestData.isSaveRequest());
            variableContainer.setVariable(prefix + "SaveResponseParameters", requestData.isSaveResponse());
        }
        return requestData;
    }

    protected void saveResponseFields(VariableContainer variableContainer, RequestData request, HttpResponse response, ObjectMapper objectMapper)
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
                Object varValue = determineResponseVariableValue(request, response, objectMapper);

                if (request.isSaveResponseTransient()) {
                    variableContainer.setTransientVariable(varName, varValue);
                } else {
                    variableContainer.setVariable(varName, varValue);
                }
            }

            // Handle http status codes
            if ((request.isNoRedirects() && response.getStatusCode() >= 300) || response.getStatusCode() >= 400) {

                String code = Integer.toString(response.getStatusCode());

                Set<String> handleCodes = request.getHandleCodes();
                if (handleCodes != null && !handleCodes.isEmpty()) {
                    if (handleCodes.contains(code)
                            || (code.startsWith("5") && handleCodes.contains("5XX"))
                            || (code.startsWith("4") && handleCodes.contains("4XX"))
                            || (code.startsWith("3") && handleCodes.contains("3XX"))) {

                        propagateError(variableContainer, code);
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

    protected Object determineResponseVariableValue(RequestData request, HttpResponse response, ObjectMapper objectMapper) throws IOException {
        HttpResponseVariableType responseVariableType = request.getResponseVariableType();
        String contentType = getContentTypeWithoutCharset(response);
        switch (responseVariableType) {
            case AUTO:
                if (isTextContentType(contentType)) {
                    return response.getBody();
                } else if (isJsonContentType(contentType)) {
                    return readJsonTree(response, objectMapper);
                } else {
                    return response.getBodyBytes();
                }
            case STRING: {
                return response.getBody();
            }
            case JSON: {
                return readJsonTree(response, objectMapper);
            }
            case BYTES: {
                return response.getBodyBytes();
            }
            case BASE64: {
                byte[] bodyBytes = response.getBodyBytes();
                return Base64.getEncoder().encodeToString(bodyBytes);
            }
            default: {
                throw new FlowableException("Unsupported response variable type: " + responseVariableType);
            }
        }
    }

    protected Object readJsonTree(HttpResponse response, ObjectMapper objectMapper) throws JsonProcessingException {
        Object varValue;
        if (response.getBody() != null) {
            varValue = objectMapper.readTree(response.getBody());
        } else {
            varValue = response.getBody();
        }
        if (varValue instanceof MissingNode || varValue instanceof NullNode) {
            varValue = null;
        }
        return varValue;
    }

    protected boolean isJsonContentType(String contentType) {
        if (contentType != null) {
            return contentType != null && contentType.endsWith("/json") || contentType.endsWith("+json");
        }
        return false;
    }

    protected boolean isTextContentType(String contentType) {
        if (contentType != null) {
            return contentType.startsWith("text/") || contentType.endsWith("/xml") || contentType.endsWith("+xml");
        }
        return false;
    }

    protected String getContentTypeWithoutCharset(HttpResponse response) {
        List<String> contentTypeHeader = response.getHttpHeaders().get("Content-Type");
        if (contentTypeHeader != null && !contentTypeHeader.isEmpty()) {
            String contentType = contentTypeHeader.get(0);
            if (contentType.indexOf(';') >= 0) {
                contentType = contentType.split(";")[0]; // remove charset if present
            }
            return contentType;
        }
        return null;
    }

    protected HttpResponseVariableType determineResponseVariableType(VariableContainer variableContainer) {
        // We use string as default, when neither is set, for backwards compatibility reasons.
        HttpResponseVariableType effectiveResponseVariableType = HttpResponseVariableType.STRING;
        if (responseVariableType != null && saveResponseVariableAsJson != null) {
            throw new FlowableException(
                    "Only one of responseVariableType or saveResponseVariableAsJson can be set, not both. saveResponseVariableAsJson is deprecated, please use responseVariableType instead.");
        }
        if (responseVariableType != null) {
            String responseVariableTypeString = ExpressionUtils.getStringFromField(responseVariableType, variableContainer).toUpperCase(Locale.ROOT);
            if (responseVariableTypeString == null) {
                effectiveResponseVariableType = HttpResponseVariableType.AUTO;
            } else if (responseVariableTypeString.equalsIgnoreCase("true")) {
                // Backwards compatibility - if the responseVariableType is set to true, then we assume it's JSON
                effectiveResponseVariableType = HttpResponseVariableType.JSON;
            } else {
                effectiveResponseVariableType = HttpResponseVariableType.valueOf(responseVariableTypeString.toUpperCase(Locale.ROOT));
            }
        } else if (saveResponseVariableAsJson != null) {
            // Backwards compatibility - if the saveResponseVariableAsJson is set, then we assume it's JSON otherwise it's a string (old default).
            boolean saveResponseAsJson = ExpressionUtils.getBooleanFromField(saveResponseVariableAsJson, variableContainer);
            effectiveResponseVariableType = saveResponseAsJson ? HttpResponseVariableType.JSON : HttpResponseVariableType.STRING;
        }
        return effectiveResponseVariableType;
    }

    protected CompletableFuture<ExecutionData> prepareAndExecuteRequest(RequestData request, boolean parallelInSameTransaction, AsyncTaskInvoker taskInvoker) {
        ExecutableHttpRequest httpRequest = httpClient.prepareRequest(request.getHttpRequest());

        if (!parallelInSameTransaction) {
            CompletableFuture<ExecutionData> future = new CompletableFuture<>();

            try {
                HttpResponse response = httpRequest.call();
                future.complete(new ExecutionData(request, response));
            } catch (Exception ex) {
                future.complete(new ExecutionData(request, null, ex));
            }
            return future;
        } else if (httpRequest instanceof AsyncExecutableHttpRequest) {

            return ((AsyncExecutableHttpRequest) httpRequest).callAsync()
                    .handle((response, throwable) -> new ExecutionData(request, response, throwable));
        }
        return taskInvoker.submit(() -> {
            try {
                return new ExecutionData(request, httpRequest.call());
            } catch (Exception ex) {
                return new ExecutionData(request, null, ex);
            }
        });
    }

    // HttpRequest validation

    protected void validateRequest(HttpRequest request) throws FlowableException {
        if (request.getMethod() == null) {
            throw new FlowableException(HTTP_TASK_REQUEST_METHOD_REQUIRED);
        }

        switch (request.getMethod()) {
            case "GET":
            case "POST":
            case "PUT":
            case "PATCH":
            case "DELETE":
                break;
            default:
                throw new FlowableException(HTTP_TASK_REQUEST_METHOD_INVALID);
        }

        if (request.getUrl() == null) {
            throw new FlowableException(HTTP_TASK_REQUEST_URL_REQUIRED);
        }
    }
    protected HttpHeaders getRequestHeaders(VariableContainer variableContainer) {
        try {
            String headersString = ExpressionUtils.getStringFromField(requestHeaders, variableContainer);
            return HttpHeaders.parseFromString(headersString);
        } catch (FlowableIllegalArgumentException ex) {
            throw new FlowableException(HTTP_TASK_REQUEST_HEADERS_INVALID, ex);
        }
    }

    protected abstract void propagateError(VariableContainer container, String code);

    public static class ExecutionData {

        protected RequestData request;
        protected HttpResponse response;
        protected Throwable exception;

        public ExecutionData(RequestData request, HttpResponse response) {
            this(request, response, null);
        }

        public ExecutionData(RequestData request, HttpResponse response, Throwable exception) {
            this.request = request;
            this.response = response;
            this.exception = exception;
        }

        public RequestData getRequest() {
            return request;
        }

        public HttpResponse getResponse() {
            return response;
        }

        public Throwable getException() {
            return exception;
        }
    }

    public static class RequestData {

        protected HttpRequest httpRequest;
        protected Set<String> failCodes;
        protected Set<String> handleCodes;
        protected boolean ignoreErrors;
        protected boolean saveRequest;
        protected boolean saveResponse;
        protected boolean saveResponseTransient;

        protected HttpResponseVariableType responseVariableType;
        protected String prefix;

        public HttpRequest getHttpRequest() {
            return httpRequest;
        }

        public void setHttpRequest(HttpRequest httpRequest) {
            this.httpRequest = httpRequest;
        }

        public Set<String> getFailCodes() {
            return failCodes;
        }

        public void setFailCodes(Set<String> failCodes) {
            this.failCodes = failCodes;
        }

        public Set<String> getHandleCodes() {
            return handleCodes;
        }

        public void setHandleCodes(Set<String> handleCodes) {
            this.handleCodes = handleCodes;
        }

        public boolean isIgnoreErrors() {
            return ignoreErrors;
        }

        public void setIgnoreErrors(boolean ignoreErrors) {
            this.ignoreErrors = ignoreErrors;
        }

        public boolean isSaveRequest() {
            return saveRequest;
        }

        public void setSaveRequest(boolean saveRequest) {
            this.saveRequest = saveRequest;
        }

        public boolean isSaveResponse() {
            return saveResponse;
        }

        public void setSaveResponse(boolean saveResponse) {
            this.saveResponse = saveResponse;
        }

        public boolean isSaveResponseTransient() {
            return saveResponseTransient;
        }

        public void setSaveResponseTransient(boolean saveResponseTransient) {
            this.saveResponseTransient = saveResponseTransient;
        }

        public HttpResponseVariableType getResponseVariableType() {
            return responseVariableType;
        }

        public void setResponseVariableType(HttpResponseVariableType responseVariableType) {
            this.responseVariableType = responseVariableType;
        }

        public String getPrefix() {
            return prefix;
        }

        public void setPrefix(String prefix) {
            this.prefix = prefix;
        }

        public boolean isNoRedirects() {
            return httpRequest.isNoRedirects();
        }
    }
}
