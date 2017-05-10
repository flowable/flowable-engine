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
package org.flowable.http.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.net.URISyntaxException;

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
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContextBuilder;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.flowable.engine.cfg.HttpClientConfig;
import org.flowable.engine.common.api.FlowableException;
import org.flowable.engine.impl.context.Context;
import org.flowable.http.HttpActivityBehavior;
import org.flowable.http.HttpRequest;
import org.flowable.http.HttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of HttpActivityBehavior using Apache HTTP Client
 *
 * @author Harsha Teja Kanna.
 */
public class HttpActivityBehaviorImpl extends HttpActivityBehavior {

    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(HttpActivityBehaviorImpl.class);

    protected static final CloseableHttpClient client;

    static {
        HttpClientConfig config = Context.getProcessEngineConfiguration().getHttpClientConfig();
        if (config == null) {
            config = new HttpClientConfig();
        }
        
        HttpClientBuilder httpClientBuilder = HttpClientBuilder.create();

        // https settings
        SSLConnectionSocketFactory sslsf = null;
        try {
            SSLContextBuilder builder = new SSLContextBuilder();
            builder.loadTrustMaterial(null, new TrustSelfSignedStrategy());
            sslsf = new SSLConnectionSocketFactory(builder.build(), SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
        } catch (Exception e) {
            log.error("Could not configure HTTP client to use SSL", e);
        }

        if (sslsf != null) {
            httpClientBuilder.setSSLSocketFactory(sslsf);
        }

        // request retry settings
        int retryCount = 0;
        if (config.getRequestRetryLimit() > 0) {
            retryCount = config.getRequestRetryLimit();
        }
        httpClientBuilder.setRetryHandler(new DefaultHttpRequestRetryHandler(retryCount, false));

        // Build http client
        client = httpClientBuilder.build();
        log.info("HTTP client is initialized");

        // Shutdown hook to close the http client
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                if (client != null) {
                    try {
                        client.close();
                        log.info("HTTP client is closed");
                    } catch (Throwable e) {
                        log.error("Could not close http client", e);
                    }
                }
            }
        });
    }

    protected void setConfig(HttpRequestBase base, HttpClientConfig config) {
        base.setConfig(RequestConfig.custom()
                .setSocketTimeout(config.getSocketTimeout())
                .setConnectTimeout(config.getConnectTimeout())
                .setConnectionRequestTimeout(config.getConnectionRequestTimeout())
                .build());
    }

    protected String getHeadersAsString(Header[] headers) {
        StringBuilder hb = new StringBuilder();
        for (Header header : headers) {
            hb.append(header.getName()).append(":").append(header.getValue()).append("\n");
        }
        return hb.toString();
    }

    protected void setHeaders(HttpMessage base, String headers) throws IOException {
        try (BufferedReader reader = new BufferedReader(new StringReader(headers))) {
            String line = reader.readLine();
            while (line != null) {
                String[] header = line.split(":");
                if (header.length == 2) {
                    base.addHeader(header[0], header[1]);
                    line = reader.readLine();
                } else {
                    throw new FlowableException(String.format("RequestInfo header %s is invalid", line));
                }
            }
        }
    }

    @Override
    public HttpResponse perform(HttpRequest requestInfo) {

        HttpRequestBase request = null;
        CloseableHttpResponse response = null;
        try {
            URIBuilder uri = new URIBuilder(requestInfo.getUrl());
            switch (requestInfo.getMethod()) {
                case "GET": {
                    request = new HttpGet(uri.toString());
                    break;
                }
                case "POST": {
                    HttpPost post = new HttpPost(uri.toString());
                    post.setEntity(new StringEntity(requestInfo.getBody()));
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
                    HttpDelete delete = new HttpDelete(uri.toString());
                    request = delete;
                    break;
                }
                default: {
                    throw new FlowableException(requestInfo.getMethod() + " HTTP method not supported");
                }
            }

            HttpClientConfig config = Context.getProcessEngineConfiguration().getHttpClientConfig();
            if (config != null) {
                setConfig(request, config);
            }

            if (requestInfo.getHeaders() != null) {
                setHeaders(request, requestInfo.getHeaders());
            }

            response = client.execute(request);

            HttpResponse responseInfo = new HttpResponse(response.getStatusLine().getStatusCode());

            if (response.getAllHeaders() != null) {
                responseInfo.setHeaders(getHeadersAsString(response.getAllHeaders()));
            }

            if (response.getEntity() != null) {
                responseInfo.setBody(EntityUtils.toString(response.getEntity()));
            }

            return responseInfo;

        } catch (ClientProtocolException e) {
            throw new FlowableException("HTTP exception occurred", e);
        } catch (IOException e) {
            throw new FlowableException("IO exception occurred", e);
        } catch (URISyntaxException e) {
            throw new FlowableException("Invalid URL exception occurred", e);
        } catch (FlowableException e) {
            throw e;
        } finally {
            if (response != null) {
                try {
                    response.close();
                } catch (Throwable e) {
                    log.error("Could not close http response", e);
                }
            }
        }
    }
}
