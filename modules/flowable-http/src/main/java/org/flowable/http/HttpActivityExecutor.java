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

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpMessage;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.flowable.bpmn.model.MapExceptionEntry;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.variable.VariableContainer;
import org.flowable.engine.delegate.BpmnError;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.impl.bpmn.helper.ErrorPropagation;
import org.flowable.http.common.api.HttpHeaders;
import org.flowable.http.delegate.HttpRequestHandler;
import org.flowable.http.delegate.HttpResponseHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.MissingNode;

/**
 * An executor behavior for HTTP requests.
 *
 * @author Harsha Teja Kanna.
 * @author martin.grofcik
 */
public class HttpActivityExecutor {

    private static final Logger LOGGER = LoggerFactory.getLogger(HttpActivityExecutor.class);
    private static final Pattern PLUS_CHARACTER_PATTERN = Pattern.compile("\\+");
    private static final String ENCODED_PLUS_CHARACTER = "%2B";
    private static final Pattern SPACE_CHARACTER_PATTERN = Pattern.compile(" ");
    private static final String ENCODED_SPACE_CHARACTER = "%20";

    // Validation constants
    public static final String HTTP_TASK_REQUEST_METHOD_REQUIRED = "requestMethod is required";
    public static final String HTTP_TASK_REQUEST_METHOD_INVALID = "requestMethod is invalid";
    public static final String HTTP_TASK_REQUEST_URL_REQUIRED = "requestUrl is required";
    public static final String HTTP_TASK_REQUEST_HEADERS_INVALID = "requestHeaders are invalid";
    public static final String HTTP_TASK_REQUEST_FIELD_INVALID = "request fields are invalid";

    protected final HttpClientBuilder clientBuilder;
    protected final ErrorPropagator errorPropagator;
    protected ObjectMapper objectMapper;

    public HttpActivityExecutor(HttpClientBuilder clientBuilder, ErrorPropagator errorPropagator, ObjectMapper objectMapper) {
        this.clientBuilder = clientBuilder;
        this.errorPropagator = errorPropagator;
        this.objectMapper = objectMapper;
    }

    public void execute(HttpRequest request, VariableContainer variableContainer, String executionId,
                        HttpRequestHandler flowableHttpRequestHandler, HttpResponseHandler flowableHttpResponseHandler,
                        String responseVariableName,
                        List<MapExceptionEntry> mapExceptions, int socketTimeout, int connectTimeout, int connectionRequestTimeout) {
        validate(request);

        CloseableHttpClient client = null;
        try {
            client = clientBuilder.build();

            HttpResponse response = perform(client, variableContainer, request, flowableHttpRequestHandler, flowableHttpResponseHandler,
                    socketTimeout,
                    connectTimeout,
                    connectionRequestTimeout);
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

                    String code = Integer.toString(response.getStatusCode());

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

        } catch (Exception e) {
            if (request.isIgnoreErrors()) {
                LOGGER.info("Error ignored while processing http task in execution {}", executionId, e);
                variableContainer.setVariable(request.getPrefix() + "ErrorMessage", e.getMessage());
            } else {
                if (!errorPropagator.mapException(e, variableContainer, mapExceptions)) {
                    if (e instanceof FlowableException) {
                        throw (FlowableException) e;
                    } else {
                        throw new FlowableException("Error occurred while processing http task in execution " + executionId, e);
                    }
                }
            }
        } finally {
            try {
                client.close();
                LOGGER.debug("HTTP client is closed");
            } catch (Throwable e) {
                LOGGER.error("Could not close http client", e);
            }
        }

    }


    // HttpRequest validation
    public void validate(final HttpRequest request) throws FlowableException {
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

    public HttpResponse perform(CloseableHttpClient client, VariableContainer execution, final HttpRequest requestInfo,
                                HttpRequestHandler httpRequestHandler,
                                HttpResponseHandler httpResponseHandler,
                                int socketTimeout, int connectTimeout, int connectionRequestTimeout) {

        HttpRequestBase request;
        CloseableHttpResponse response = null;

        try {
            if (httpRequestHandler != null) {
                httpRequestHandler.handleHttpRequest(execution, requestInfo, client);
            }
        } catch (Exception e) {
            if (e instanceof BpmnError) {
                ErrorPropagation.propagateError(((BpmnError) e), ((DelegateExecution) execution));
                return null;
            }

            throw new FlowableException("Exception while invoking HttpRequestHandler: " + e.getMessage(), e);
        }

        try {
            URI uri = createUri(requestInfo.getUrl());
            switch (requestInfo.getMethod()) {
                case "GET": {
                    request = new HttpGet(uri);
                    break;
                }
                case "POST": {
                    HttpPost post = new HttpPost(uri);
                    setRequestEntity(requestInfo, post);
                    request = post;
                    break;
                }
                case "PUT": {
                    HttpPut put = new HttpPut(uri);
                    setRequestEntity(requestInfo, put);
                    request = put;
                    break;
                }
                case "DELETE": {
                    request = new HttpDelete(uri);
                    break;
                }
                default: {
                    throw new FlowableException(requestInfo.getMethod() + " HTTP method not supported");
                }
            }

            if (requestInfo.getHttpHeaders() != null) {
                setHeaders(request, requestInfo.getHttpHeaders());
            }

            setConfig(request, requestInfo,
                    socketTimeout,
                    connectTimeout,
                    connectionRequestTimeout);

            response = client.execute(request);

            HttpResponse responseInfo = new HttpResponse();

            if (response.getStatusLine() != null) {
                responseInfo.setStatusCode(response.getStatusLine().getStatusCode());
                responseInfo.setProtocol(response.getStatusLine().getProtocolVersion().toString());
                responseInfo.setReason(response.getStatusLine().getReasonPhrase());
            }

            if (response.getAllHeaders() != null) {
                responseInfo.setHttpHeaders(getHttpHeaders(response.getAllHeaders()));
            }

            if (response.getEntity() != null) {
                responseInfo.setBody(EntityUtils.toString(response.getEntity()));
            }

            try {
                if ( httpResponseHandler!= null) {
                    httpResponseHandler.handleHttpResponse(execution, responseInfo);
                }
            } catch (Exception e) {
                if (e instanceof BpmnError) {
                    ErrorPropagation.propagateError(((BpmnError) e), ((DelegateExecution) execution));
                    return null;
                }

                throw new FlowableException("Exception while invoking HttpResponseHandler: " + e.getMessage(), e);
            }

            return responseInfo;

        } catch (final ClientProtocolException e) {
            throw new FlowableException("HTTP exception occurred", e);
        } catch (final IOException e) {
            throw new FlowableException("IO exception occurred", e);
        } catch (final URISyntaxException e) {
            throw new FlowableException("Invalid URL exception occurred", e);
        } finally {
            if (response != null) {
                try {
                    response.close();
                } catch (Throwable e) {
                    LOGGER.error("Could not close http response", e);
                }
            }
        }
    }

    protected void setRequestEntity(HttpRequest requestInfo, HttpEntityEnclosingRequestBase requestBase) throws UnsupportedEncodingException {
        if (requestInfo.getBody() != null) {
            if (StringUtils.isNotEmpty(requestInfo.getBodyEncoding())) {
                requestBase.setEntity(new StringEntity(requestInfo.getBody(), requestInfo.getBodyEncoding()));
            } else {
                requestBase.setEntity(new StringEntity(requestInfo.getBody()));
            }
        }
    }

    protected void setConfig(final HttpRequestBase base, final HttpRequest requestInfo, int socketTimeout, int connectTimeout, int connectionRequestTimeout) {
        base.setConfig(RequestConfig.custom()
                .setRedirectsEnabled(!requestInfo.isNoRedirects())
                .setSocketTimeout(requestInfo.getTimeout() == 0 ? socketTimeout : requestInfo.getTimeout())
                .setConnectTimeout(connectTimeout)
                .setConnectionRequestTimeout(connectionRequestTimeout)
                .build());
    }

    protected String getHeadersAsString(final Header[] headers) {
        StringBuilder hb = new StringBuilder();
        for (Header header : headers) {
            hb.append(header.getName()).append(": ").append(header.getValue()).append('\n');
        }
        return hb.toString();
    }

    protected HttpHeaders getHttpHeaders(Header[] headers) {
        HttpHeaders httpHeaders = new HttpHeaders();
        for (Header header : headers) {
            httpHeaders.add(header.getName(), header.getValue());
        }
        return httpHeaders;
    }

    protected void setHeaders(final HttpMessage base, final HttpHeaders headers) {
        for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
            String headerName = entry.getKey();
            for (String headerValue : entry.getValue()) {
                base.addHeader(headerName, headerValue);
            }
        }
    }

    protected URI createUri(String url) throws URISyntaxException {
        String uri = SPACE_CHARACTER_PATTERN.matcher(url).replaceAll(ENCODED_SPACE_CHARACTER);
        return new URI(PLUS_CHARACTER_PATTERN.matcher(uri).replaceAll(ENCODED_PLUS_CHARACTER));
    }

}
