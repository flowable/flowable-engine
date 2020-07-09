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
package org.flowable.http.client;

import static org.flowable.http.HttpActivityExecutor.HTTP_TASK_REQUEST_HEADERS_INVALID;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
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
import org.flowable.common.engine.api.FlowableException;
import org.flowable.http.HttpHeaders;
import org.flowable.http.HttpRequest;
import org.flowable.http.HttpRequestValidator;
import org.flowable.http.HttpResponse;

/**
 * @author Filip Hrisafov
 */
public class ApacheHttpComponentsFlowableHttpClient implements FlowableHttpClient {

    private static final Pattern PLUS_CHARACTER_PATTERN = Pattern.compile("\\+");
    private static final String ENCODED_PLUS_CHARACTER = "%2B";
    private static final Pattern SPACE_CHARACTER_PATTERN = Pattern.compile(" ");
    private static final String ENCODED_SPACE_CHARACTER = "%20";

    protected HttpClientBuilder clientBuilder;
    protected HttpRequestValidator requestValidator;
    protected int socketTimeout;
    protected int connectTimeout;
    protected int connectionRequestTimeout;

    public ApacheHttpComponentsFlowableHttpClient(HttpClientBuilder clientBuilder, HttpRequestValidator requestValidator) {
        this(clientBuilder, requestValidator, 5000, 5000, 5000);
    }

    public ApacheHttpComponentsFlowableHttpClient(HttpClientBuilder clientBuilder, HttpRequestValidator requestValidator, int socketTimeout, int connectTimeout,
            int connectionRequestTimeout) {
        this.clientBuilder = clientBuilder;
        this.requestValidator = requestValidator;
        this.socketTimeout = socketTimeout;
        this.connectTimeout = connectTimeout;
        this.connectionRequestTimeout = connectionRequestTimeout;
    }

    @Override
    public ExecutableHttpRequest prepareRequest(HttpRequest requestInfo) {
        requestValidator.validateRequest(requestInfo);
        try {
            HttpRequestBase request;
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

            setConfig(request, requestInfo);
            return new ApacheHttpComponentsExecutableHttpRequest(request);
        } catch (URISyntaxException ex) {
            throw new FlowableException("Invalid URL exception occurred", ex);
        } catch (IOException ex) {
            throw new FlowableException("IO exception occurred", ex);
        }
    }

    protected URI createUri(String url) throws URISyntaxException {
        String uri = SPACE_CHARACTER_PATTERN.matcher(url).replaceAll(ENCODED_SPACE_CHARACTER);
        return new URI(PLUS_CHARACTER_PATTERN.matcher(uri).replaceAll(ENCODED_PLUS_CHARACTER));
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

    protected void setHeaders(final HttpMessage base, final HttpHeaders headers) {
        for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
            String headerName = entry.getKey();
            for (String headerValue : entry.getValue()) {
                base.addHeader(headerName, headerValue);
            }
        }
    }

    protected void setConfig(HttpRequestBase base, HttpRequest requestInfo) {
        base.setConfig(RequestConfig.custom()
                .setRedirectsEnabled(!requestInfo.isNoRedirects())
                .setSocketTimeout(requestInfo.getTimeout() == 0 ? socketTimeout : requestInfo.getTimeout())
                .setConnectTimeout(connectTimeout)
                .setConnectionRequestTimeout(connectionRequestTimeout)
                .build());
    }

    protected HttpResponse toFlowableHttpResponse(CloseableHttpResponse response) throws IOException {
        HttpResponse responseInfo = new HttpResponse();

        if (response.getStatusLine() != null) {
            responseInfo.setStatusCode(response.getStatusLine().getStatusCode());
            responseInfo.setProtocol(response.getStatusLine().getProtocolVersion().toString());
            responseInfo.setReason(response.getStatusLine().getReasonPhrase());
        }

        if (response.getAllHeaders() != null) {
            responseInfo.setHttpHeaders(getHeaders(response.getAllHeaders()));
        }

        if (response.getEntity() != null) {
            responseInfo.setBody(EntityUtils.toString(response.getEntity()));
        }

        return responseInfo;

    }

    protected HttpHeaders getHeaders(Header[] headers) {
        HttpHeaders httpHeaders = new HttpHeaders();
        for (Header header : headers) {
            httpHeaders.add(header.getName(), header.getValue());
        }
        return httpHeaders;
    }

    protected class ApacheHttpComponentsExecutableHttpRequest implements ExecutableHttpRequest {

        protected final HttpRequestBase request;

        public ApacheHttpComponentsExecutableHttpRequest(HttpRequestBase request) {
            this.request = request;
        }

        @Override
        public HttpResponse call() {
            try (CloseableHttpClient httpClient = clientBuilder.build()) {

                try (CloseableHttpResponse response = httpClient.execute(request)) {
                    return toFlowableHttpResponse(response);
                }

            } catch (ClientProtocolException ex) {
                throw new FlowableException("HTTP exception occurred", ex);
            } catch (IOException ex) {
                throw new FlowableException("IO exception occurred", ex);
            }
        }
    }
}
