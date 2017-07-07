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
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;

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
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.util.EntityUtils;
import org.flowable.bpmn.model.FieldExtension;
import org.flowable.bpmn.model.FlowableHttpRequestHandler;
import org.flowable.bpmn.model.FlowableHttpResponseHandler;
import org.flowable.bpmn.model.HttpServiceTask;
import org.flowable.bpmn.model.ImplementationType;
import org.flowable.bpmn.model.ServiceTask;
import org.flowable.engine.cfg.HttpClientConfig;
import org.flowable.engine.common.api.FlowableException;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.Expression;
import org.flowable.engine.impl.bpmn.parser.FieldDeclaration;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.el.FixedValue;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.http.HttpActivityBehavior;
import org.flowable.http.HttpRequest;
import org.flowable.http.HttpResponse;
import org.flowable.http.delegate.HttpRequestHandler;
import org.flowable.http.delegate.HttpResponseHandler;
import org.flowable.http.impl.handler.ClassDelegateHttpHandler;
import org.flowable.http.impl.handler.DelegateExpressionHttpHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of HttpActivityBehavior using Apache HTTP Client
 *
 * @author Harsha Teja Kanna.
 */
public class HttpActivityBehaviorImpl extends HttpActivityBehavior {

    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = LoggerFactory.getLogger(HttpActivityBehaviorImpl.class);
    
    protected HttpServiceTask httpServiceTask;

    protected final Timer timer = new Timer(true);
    protected final CloseableHttpClient client;

    public HttpActivityBehaviorImpl() {  
        HttpClientConfig config = CommandContextUtil.getProcessEngineConfiguration().getHttpClientConfig();
        HttpClientBuilder httpClientBuilder = HttpClientBuilder.create();

        // https settings
        if (config.isDisableCertVerify()) {
            try {
                SSLContextBuilder builder = new SSLContextBuilder();
                builder.loadTrustMaterial(null, new TrustSelfSignedStrategy());
                httpClientBuilder.setSSLSocketFactory(
                        new SSLConnectionSocketFactory(builder.build(), new HostnameVerifier() {
                            public boolean verify(String s, SSLSession sslSession) {
                                return true;
                            }
                        }));
                
            } catch (Exception e) {
                LOGGER.error("Could not configure HTTP client SSL self signed strategy", e);
            }
        }

        // request retry settings
        int retryCount = 0;
        if (config.getRequestRetryLimit() > 0) {
            retryCount = config.getRequestRetryLimit();
        }
        httpClientBuilder.setRetryHandler(new DefaultHttpRequestRetryHandler(retryCount, false));

        // Build http client
        client = httpClientBuilder.build();
        LOGGER.info("HTTP client is initialized");

        // Shutdown hook to close the http client
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                if (client != null) {
                    try {
                        client.close();
                        LOGGER.info("HTTP client is closed");
                    } catch (Throwable e) {
                        LOGGER.error("Could not close http client", e);
                    }
                }
            }
        });
    }

    @Override
    public HttpResponse perform(final DelegateExecution execution, final HttpRequest requestInfo) {

        HttpRequestBase request = null;
        CloseableHttpResponse response = null;
        
        ProcessEngineConfigurationImpl processEngineConfiguration = CommandContextUtil.getProcessEngineConfiguration();
        
        try {
            if (httpServiceTask.getHttpRequestHandler() != null) {
                HttpRequestHandler httpRequestHandler = createHttpRequestHandler(httpServiceTask.getHttpRequestHandler(), processEngineConfiguration);
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
                if (httpServiceTask.getHttpResponseHandler() != null) {
                    HttpResponseHandler httpResponseHandler = createHttpResponseHandler(httpServiceTask.getHttpResponseHandler(), processEngineConfiguration);
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
        } catch (final FlowableException e) {
            throw e;
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
                String[] header = line.split(":");
                if (header.length == 2) {
                    base.addHeader(header[0], header[1]);
                    line = reader.readLine();
                } else {
                    throw new FlowableException(HTTP_TASK_REQUEST_HEADERS_INVALID);
                }
            }
        }
    }
    
    protected HttpRequestHandler createHttpRequestHandler(FlowableHttpRequestHandler handler, ProcessEngineConfigurationImpl processEngineConfiguration) {
        HttpRequestHandler requestHandler = null;

        if (ImplementationType.IMPLEMENTATION_TYPE_CLASS.equalsIgnoreCase(handler.getImplementationType())) {
            requestHandler = new ClassDelegateHttpHandler(handler.getImplementation(), 
                            createFieldDeclarations(handler.getFieldExtensions(), processEngineConfiguration));
        
        } else if (ImplementationType.IMPLEMENTATION_TYPE_DELEGATEEXPRESSION.equalsIgnoreCase(handler.getImplementationType())) {
            requestHandler = new DelegateExpressionHttpHandler(processEngineConfiguration.getExpressionManager().createExpression(handler.getImplementation()),
                            createFieldDeclarations(handler.getFieldExtensions(), processEngineConfiguration));   
        }
        return requestHandler;
    }
    
    protected HttpResponseHandler createHttpResponseHandler(FlowableHttpResponseHandler handler, ProcessEngineConfigurationImpl processEngineConfiguration) {
        HttpResponseHandler responseHandler = null;

        if (ImplementationType.IMPLEMENTATION_TYPE_CLASS.equalsIgnoreCase(handler.getImplementationType())) {
            responseHandler = new ClassDelegateHttpHandler(handler.getImplementation(), 
                            createFieldDeclarations(handler.getFieldExtensions(), processEngineConfiguration));
        
        } else if (ImplementationType.IMPLEMENTATION_TYPE_DELEGATEEXPRESSION.equalsIgnoreCase(handler.getImplementationType())) {
            responseHandler = new DelegateExpressionHttpHandler(processEngineConfiguration.getExpressionManager().createExpression(handler.getImplementation()),
                            createFieldDeclarations(handler.getFieldExtensions(), processEngineConfiguration));   
        }
        return responseHandler;
    }
    
    protected List<FieldDeclaration> createFieldDeclarations(List<FieldExtension> fieldList, ProcessEngineConfigurationImpl processEngineConfiguration) {
        List<FieldDeclaration> fieldDeclarations = new ArrayList<>();

        for (FieldExtension fieldExtension : fieldList) {
            FieldDeclaration fieldDeclaration = null;
            if (StringUtils.isNotEmpty(fieldExtension.getExpression())) {
                fieldDeclaration = new FieldDeclaration(fieldExtension.getFieldName(), Expression.class.getName(), 
                                processEngineConfiguration.getExpressionManager().createExpression(fieldExtension.getExpression()));
            } else {
                fieldDeclaration = new FieldDeclaration(fieldExtension.getFieldName(), Expression.class.getName(), 
                                new FixedValue(fieldExtension.getStringValue()));
            }

            fieldDeclarations.add(fieldDeclaration);
        }
        return fieldDeclarations;
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
    
    public void setServiceTask(ServiceTask serviceTask) {
        this.httpServiceTask = (HttpServiceTask) serviceTask;
    }
}
