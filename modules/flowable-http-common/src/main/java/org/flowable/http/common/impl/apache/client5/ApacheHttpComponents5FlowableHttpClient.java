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
package org.flowable.http.common.impl.apache.client5;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.hc.client5.http.async.HttpAsyncClient;
import org.apache.hc.client5.http.async.methods.SimpleBody;
import org.apache.hc.client5.http.async.methods.SimpleHttpResponse;
import org.apache.hc.client5.http.async.methods.SimpleResponseConsumer;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.entity.mime.HttpMultipartMode;
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;
import org.apache.hc.client5.http.impl.DefaultHttpRequestRetryStrategy;
import org.apache.hc.client5.http.impl.async.CloseableHttpAsyncClient;
import org.apache.hc.client5.http.impl.async.HttpAsyncClientBuilder;
import org.apache.hc.client5.http.impl.async.HttpAsyncClients;
import org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManagerBuilder;
import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.apache.hc.client5.http.ssl.DefaultClientTlsStrategy;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.client5.http.ssl.TrustSelfSignedStrategy;
import org.apache.hc.core5.concurrent.FutureCallback;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpVersion;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.ByteArrayEntity;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.nio.AsyncRequestProducer;
import org.apache.hc.core5.http.nio.entity.AsyncEntityProducers;
import org.apache.hc.core5.http.nio.support.AsyncRequestBuilder;
import org.apache.hc.core5.io.CloseMode;
import org.apache.hc.core5.io.ModalCloseable;
import org.apache.hc.core5.ssl.SSLContextBuilder;
import org.apache.hc.core5.util.TimeValue;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.http.common.api.HttpHeaders;
import org.flowable.http.common.api.HttpRequest;
import org.flowable.http.common.api.HttpResponse;
import org.flowable.http.common.api.MultiValuePart;
import org.flowable.http.common.api.client.AsyncExecutableHttpRequest;
import org.flowable.http.common.api.client.FlowableAsyncHttpClient;
import org.flowable.http.common.impl.HttpClientConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Filip Hrisafov
 */
public class ApacheHttpComponents5FlowableHttpClient implements FlowableAsyncHttpClient {

    private static final Pattern PLUS_CHARACTER_PATTERN = Pattern.compile("\\+");
    private static final String ENCODED_PLUS_CHARACTER = "%2B";
    private static final Pattern SPACE_CHARACTER_PATTERN = Pattern.compile(" ");
    private static final String ENCODED_SPACE_CHARACTER = "%20";

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    protected HttpAsyncClient client;
    protected boolean closeClient;
    protected int socketTimeout;
    protected int connectTimeout;
    protected int connectionRequestTimeout;

    public ApacheHttpComponents5FlowableHttpClient(HttpClientConfig config) {
        HttpAsyncClientBuilder httpClientBuilder = HttpAsyncClients.custom();

        PoolingAsyncClientConnectionManagerBuilder managerBuilder = PoolingAsyncClientConnectionManagerBuilder.create()
                .setMaxConnTotal(500);
        // https settings
        if (config.isDisableCertVerify()) {

            try {
                SSLContextBuilder builder = SSLContextBuilder.create();
                builder.loadTrustMaterial(null, TrustSelfSignedStrategy.INSTANCE);
                managerBuilder.setTlsStrategy(new DefaultClientTlsStrategy(builder.build(), NoopHostnameVerifier.INSTANCE));

            } catch (Exception e) {
                logger.error("Could not configure HTTP client SSL self signed strategy", e);
            }
        }

        // request retry settings
        int retryCount = 0;
        if (config.getRequestRetryLimit() > 0) {
            retryCount = config.getRequestRetryLimit();
        }
        httpClientBuilder.setRetryStrategy(new DefaultHttpRequestRetryStrategy(retryCount, TimeValue.ZERO_MILLISECONDS));

        // system settings
        if (config.isUseSystemProperties()) {
            httpClientBuilder.useSystemProperties();
            managerBuilder.useSystemProperties();
        }

        httpClientBuilder.setConnectionManager(managerBuilder.build());

        CloseableHttpAsyncClient client = httpClientBuilder.build();
        client.start();
        this.client = client;
        this.closeClient = true;

        this.socketTimeout = config.getSocketTimeout();
        this.connectTimeout = config.getConnectTimeout();
        this.connectionRequestTimeout = config.getConnectionRequestTimeout();
    }

    public ApacheHttpComponents5FlowableHttpClient(HttpAsyncClient client, int socketTimeout, int connectTimeout,
            int connectionRequestTimeout) {
        this.client = client;
        this.socketTimeout = socketTimeout;
        this.connectTimeout = connectTimeout;
        this.connectionRequestTimeout = connectionRequestTimeout;
    }

    public void close() {
        if (closeClient && client instanceof ModalCloseable) {
            ((ModalCloseable) client).close(CloseMode.GRACEFUL);
        }
    }

    @Override
    public AsyncExecutableHttpRequest prepareRequest(HttpRequest requestInfo) {
        try {
            AsyncRequestBuilder request;
            URI uri = createUri(requestInfo.getUrl());
            switch (requestInfo.getMethod()) {
                case "GET": {
                    request = AsyncRequestBuilder.get(uri);
                    break;
                }
                case "POST": {
                    request = AsyncRequestBuilder.post(uri);
                    setRequestEntity(requestInfo, request);
                    break;
                }
                case "PUT": {
                    request = AsyncRequestBuilder.put(uri);
                    setRequestEntity(requestInfo, request);
                    break;
                }
                case "PATCH": {
                    request = AsyncRequestBuilder.patch(uri);
                    setRequestEntity(requestInfo, request);
                    break;
                }
                case "DELETE": {
                    request = AsyncRequestBuilder.delete(uri);
                    setRequestEntity(requestInfo, request);
                    break;
                }
                default: {
                    throw new FlowableException(requestInfo.getMethod() + " HTTP method not supported");
                }
            }

            if (requestInfo.getHttpHeaders() != null) {
                setHeaders(request, requestInfo.getHttpHeaders());
            }

            return new ApacheHttpComponentsExecutableHttpRequest(request.build(), createRequestConfig(requestInfo));
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

    protected void setRequestEntity(HttpRequest requestInfo, AsyncRequestBuilder requestBase) throws UnsupportedEncodingException {
        if (requestInfo.getBody() != null) {
            if (StringUtils.isNotEmpty(requestInfo.getBodyEncoding())) {
                requestBase.setEntity(AsyncEntityProducers.create(requestInfo.getBody(), Charset.forName(requestInfo.getBodyEncoding())));
            } else {
                requestBase.setEntity(AsyncEntityProducers.create(requestInfo.getBody()));
            }
        } else if (requestInfo.getMultiValueParts() != null) {
            MultipartEntityBuilder entityBuilder = MultipartEntityBuilder.create();
            entityBuilder.setMode(HttpMultipartMode.LEGACY);
            for (MultiValuePart part : requestInfo.getMultiValueParts()) {
                String name = part.getName();
                Object value = part.getBody();
                if (value instanceof byte[]) {
                    entityBuilder.addBinaryBody(name, (byte[]) value, ContentType.DEFAULT_BINARY, part.getFilename());
                } else if (value instanceof String) {
                    entityBuilder.addTextBody(name, (String) value);
                } else if (value != null) {
                    throw new FlowableIllegalArgumentException("Value of type " + value.getClass() + " is not supported as multi part content");
                }
            }

            try (HttpEntity multiPartEntity = entityBuilder.build();
                 ByteArrayOutputStream outputStream = new ByteArrayOutputStream((int) multiPartEntity.getContentLength())) {
                multiPartEntity.writeTo(outputStream);
                requestBase.setEntity(outputStream.toByteArray(), ContentType.parse(multiPartEntity.getContentType()));
            } catch (IOException e) {
                throw new FlowableException("Cannot create multi part entity", e);
            }
        }
    }

    protected void setHeaders(AsyncRequestBuilder base, HttpHeaders headers) {
        for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
            String headerName = entry.getKey();
            for (String headerValue : entry.getValue()) {
                base.addHeader(headerName, headerValue);
            }
        }
    }

    protected RequestConfig createRequestConfig(HttpRequest request) {
        return RequestConfig.custom()
                .setRedirectsEnabled(!request.isNoRedirects())
                .setResponseTimeout(request.getTimeout() == 0 ? socketTimeout : request.getTimeout(), TimeUnit.MILLISECONDS)
                .setConnectTimeout(connectTimeout, TimeUnit.MILLISECONDS)
                .setConnectionRequestTimeout(connectionRequestTimeout, TimeUnit.MILLISECONDS)
                .build();
    }

    protected HttpResponse toFlowableHttpResponse(SimpleHttpResponse response) {
        HttpResponse responseInfo = new HttpResponse();

        responseInfo.setStatusCode(response.getCode());
        responseInfo.setReason(response.getReasonPhrase());
        responseInfo.setProtocol((response.getVersion() != null ? response.getVersion() : HttpVersion.HTTP_1_1).toString());

        HttpHeaders headers = null;
        Iterator<org.apache.hc.core5.http.Header> headerIterator = response.headerIterator();
        while (headerIterator.hasNext()) {
            if (headers == null) {
                headers = new HttpHeaders();
            }

            org.apache.hc.core5.http.Header header = headerIterator.next();
            headers.add(header.getName(), header.getValue());
        }

        responseInfo.setHttpHeaders(headers);

        SimpleBody body = response.getBody();
        if (body != null) {
            if (body.isText()) {
                responseInfo.setBody(body.getBodyText());
                responseInfo.setBodyBytes(body.getBodyBytes());
            } else {
                try {
                    // We are creating a fake entity in order to rely on the creation of a String using the EntityUtils
                    // They contain some special logic for picking the default charset based on the content type
                    // (in case the content type doesn't have a charset)
                    responseInfo.setBody(EntityUtils.toString(new ByteArrayEntity(body.getBodyBytes(), body.getContentType())));
                    responseInfo.setBodyBytes(body.getBodyBytes());
                } catch (IOException | ParseException e) {
                    throw new FlowableException("Failed to read body");
                }
            }
        }

        return responseInfo;

    }

    protected class ApacheHttpComponentsExecutableHttpRequest implements AsyncExecutableHttpRequest {

        protected final AsyncRequestProducer request;
        protected final RequestConfig requestConfig;

        public ApacheHttpComponentsExecutableHttpRequest(AsyncRequestProducer request, RequestConfig requestConfig) {
            this.request = request;
            this.requestConfig = requestConfig;
        }

        @Override
        public CompletableFuture<HttpResponse> callAsync() {
            CompletableFuture<HttpResponse> responseFuture = new CompletableFuture<>();

            HttpClientContext context = HttpClientContext.create();
            context.setRequestConfig(requestConfig);
            client.execute(
                    request,
                    SimpleResponseConsumer.create(),
                    null,
                    context,
                    new FutureCallback<SimpleHttpResponse>() {

                        @Override
                        public void completed(SimpleHttpResponse result) {
                            responseFuture.complete(toFlowableHttpResponse(result));
                        }

                        @Override
                        public void failed(Exception ex) {
                            responseFuture.completeExceptionally(ex);
                        }

                        @Override
                        public void cancelled() {
                            responseFuture.cancel(true);
                        }
                    }
            );

            return responseFuture;
        }
    }
}
