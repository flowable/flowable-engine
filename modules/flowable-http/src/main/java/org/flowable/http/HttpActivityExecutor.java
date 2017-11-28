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

import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpMessage;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.flowable.bpmn.model.MapExceptionEntry;
import org.flowable.engine.cfg.HttpClientConfig;
import org.flowable.engine.common.api.FlowableException;
import org.flowable.engine.common.api.delegate.Expression;
import org.flowable.engine.common.api.variable.VariableContainer;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.http.delegate.HttpRequestHandler;
import org.flowable.http.delegate.HttpResponseHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import static org.flowable.http.ExpressionUtils.getStringFromField;

/**
 * An executor behavior for HTTP requests.
 *
 * @author Harsha Teja Kanna.
 * @author martin.grofcik
 */
public class HttpActivityExecutor {

    private static final Logger LOGGER = LoggerFactory.getLogger(HttpActivityExecutor.class);

    private static final long serialVersionUID = 1L;

    // Validation constants
    public static final String HTTP_TASK_REQUEST_METHOD_REQUIRED = "requestMethod is required";
    public static final String HTTP_TASK_REQUEST_METHOD_INVALID = "requestMethod is invalid";
    public static final String HTTP_TASK_REQUEST_URL_REQUIRED = "requestUrl is required";
    public static final String HTTP_TASK_REQUEST_HEADERS_INVALID = "requestHeaders are invalid";
    public static final String HTTP_TASK_REQUEST_FIELD_INVALID = "request fields are invalid";

    protected final Timer timer = new Timer(true);
    protected final CloseableHttpClient client;
    protected final ErrorPropagator errorPropagator;

    public HttpActivityExecutor(CloseableHttpClient client, ErrorPropagator errorPropagator) {
        this.client = client;
        this.errorPropagator = errorPropagator;
    }

    public void execute(HttpRequest request, VariableContainer execution, String executionId,
                        HttpRequestHandler flowableHttpRequestHandler, HttpResponseHandler flowableHttpResponseHandler,
                        Expression responseVariableName,
                        List<MapExceptionEntry> mapExceptions) {

        validate(request);

        try {
            HttpResponse response = perform(execution, request, flowableHttpRequestHandler, flowableHttpResponseHandler);
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

                            errorPropagator.propagateError(execution, code);
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
                execution.setVariable(request.getPrefix() + ".errorMessage", e.getMessage());

            } else {
                if (!errorPropagator.mapException(e, (ExecutionEntity) execution, mapExceptions)) {
                    if (e instanceof FlowableException) {
                        throw (FlowableException) e;
                    } else {
                        throw new FlowableException("Error occurred while processing http task in execution " + executionId, e);
                    }
                }
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

    public HttpResponse perform(VariableContainer execution, final HttpRequest requestInfo,
                                HttpRequestHandler httpRequestHandler,
                                HttpResponseHandler httpResponseHandler) {

        HttpRequestBase request;
        CloseableHttpResponse response = null;

        try {
            if (httpRequestHandler != null) {
                httpRequestHandler.handleHttpRequest(execution, requestInfo, client);
            }
        } catch (Exception e) {
            throw new FlowableException("Exception while invoking HttpRequestHandler: " + e.getMessage(), e);
        }

        try {
            URIBuilder uri = new URIBuilder(requestInfo.getUrl());
            switch (requestInfo.getMethod()) {
                case "GET": {
                    request = new HttpGet(uri.toString());
                    break;
                }
                case "POST": {
                    HttpPost post = new HttpPost(uri.toString());
                    if (requestInfo.getBody() != null) {
                        post.setEntity(new StringEntity(requestInfo.getBody()));
                    }
                    request = post;
                    break;
                }
                case "PUT": {
                    HttpPut put = new HttpPut(uri.toString());
                    put.setEntity(new StringEntity(requestInfo.getBody()));
                    request = put;
                    break;
                }
                case "DELETE": {
                    request = new HttpDelete(uri.toString());
                    break;
                }
                default: {
                    throw new FlowableException(requestInfo.getMethod() + " HTTP method not supported");
                }
            }

            if (requestInfo.getHeaders() != null) {
                setHeaders(request, requestInfo.getHeaders());
            }

            setConfig(request, requestInfo, CommandContextUtil.getProcessEngineConfiguration().getHttpClientConfig());

            if (requestInfo.getTimeout() > 0) {
                timer.schedule(new TimeoutTask(request), requestInfo.getTimeout());
            }

            response = client.execute(request);

            HttpResponse responseInfo = new HttpResponse();

            if (response.getStatusLine() != null) {
                responseInfo.setStatusCode(response.getStatusLine().getStatusCode());
                responseInfo.setProtocol(response.getStatusLine().getProtocolVersion().toString());
                responseInfo.setReason(response.getStatusLine().getReasonPhrase());
            }

            if (response.getAllHeaders() != null) {
                responseInfo.setHeaders(getHeadersAsString(response.getAllHeaders()));
            }

            if (response.getEntity() != null) {
                responseInfo.setBody(EntityUtils.toString(response.getEntity()));
            }

            try {
                if ( httpResponseHandler!= null) {
                    httpResponseHandler.handleHttpResponse(execution, responseInfo);
                }
            } catch (Exception e) {
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

    protected void setConfig(final HttpRequestBase base, final HttpRequest requestInfo, final HttpClientConfig config) {
        base.setConfig(RequestConfig.custom()
                .setRedirectsEnabled(!requestInfo.isNoRedirects())
                .setSocketTimeout(config.getSocketTimeout())
                .setConnectTimeout(config.getConnectTimeout())
                .setConnectionRequestTimeout(config.getConnectionRequestTimeout())
                .build());
    }

    protected String getHeadersAsString(final Header[] headers) {
        StringBuilder hb = new StringBuilder();
        for (Header header : headers) {
            hb.append(header.getName()).append(": ").append(header.getValue()).append('\n');
        }
        return hb.toString();
    }

    protected void setHeaders(final HttpMessage base, final String headers) throws IOException {
        try (BufferedReader reader = new BufferedReader(new StringReader(headers))) {
            String line = reader.readLine();
            while (line != null) {
                int colonIndex = line.indexOf(":");
                if (colonIndex > 0) {
                    String headerName = line.substring(0, colonIndex);
                    if (line.length() > colonIndex + 2) {
                        base.addHeader(headerName, line.substring(colonIndex + 1));
                    } else {
                        base.addHeader(headerName, null);
                    }
                    line = reader.readLine();

                } else {
                    throw new FlowableException(HTTP_TASK_REQUEST_HEADERS_INVALID);
                }
            }
        }
    }

    protected static class TimeoutTask extends TimerTask {
        private HttpRequestBase request;

        public TimeoutTask(HttpRequestBase request) {
            this.request = request;
        }

        @Override
        public void run() {
            if (request != null) {
                request.abort();
            }
        }
    }

}
