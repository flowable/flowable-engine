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
package org.flowable.http.bpmn.impl;

import static org.flowable.bpmn.model.ImplementationType.IMPLEMENTATION_TYPE_CLASS;
import static org.flowable.bpmn.model.ImplementationType.IMPLEMENTATION_TYPE_DELEGATEEXPRESSION;
import static org.flowable.http.ExpressionUtils.getBooleanFromField;
import static org.flowable.http.ExpressionUtils.getIntFromField;
import static org.flowable.http.ExpressionUtils.getStringFromField;
import static org.flowable.http.ExpressionUtils.getStringSetFromField;

import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.ssl.SSLContextBuilder;
import org.flowable.bpmn.model.FieldExtension;
import org.flowable.bpmn.model.FlowableHttpRequestHandler;
import org.flowable.bpmn.model.FlowableHttpResponseHandler;
import org.flowable.bpmn.model.HttpServiceTask;
import org.flowable.bpmn.model.ImplementationType;
import org.flowable.bpmn.model.MapExceptionEntry;
import org.flowable.bpmn.model.ServiceTask;
import org.flowable.engine.cfg.HttpClientConfig;
import org.flowable.engine.common.api.FlowableException;
import org.flowable.engine.common.api.delegate.Expression;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.impl.bpmn.behavior.AbstractBpmnActivityBehavior;
import org.flowable.engine.impl.bpmn.parser.FieldDeclaration;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.el.FixedValue;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.http.HttpActivityExecutor;
import org.flowable.http.HttpRequest;
import org.flowable.http.bpmn.impl.handler.ClassDelegateHttpHandler;
import org.flowable.http.bpmn.impl.handler.DelegateExpressionHttpHandler;
import org.flowable.http.delegate.HttpRequestHandler;
import org.flowable.http.delegate.HttpResponseHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of HttpActivityBehavior using Apache HTTP Client
 *
 * @author Harsha Teja Kanna.
 * @author Joram Barrez
 */
public class HttpActivityBehaviorImpl extends AbstractBpmnActivityBehavior {

    public static final String HTTP_TASK_REQUEST_FIELD_INVALID = "request fields are invalid";

    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = LoggerFactory.getLogger(HttpActivityBehaviorImpl.class);

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
    // Exception mapping
    protected List<MapExceptionEntry> mapExceptions;

    protected HttpServiceTask httpServiceTask;
    protected HttpActivityExecutor httpActivityExecutor;

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
                            @Override
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

        this.httpActivityExecutor = new HttpActivityExecutor(httpClientBuilder, new ProcessErrorPropagator(), 
                CommandContextUtil.getProcessEngineConfiguration().getObjectMapper());
    }

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
            request.setSaveResponseTransient(getBooleanFromField(saveResponseParametersTransient, execution));
            request.setSaveResponseAsJson(getBooleanFromField(saveResponseVariableAsJson, execution));
            request.setPrefix(getStringFromField(resultVariablePrefix, execution));

            String failCodes = getStringFromField(failStatusCodes, execution);
            String handleCodes = getStringFromField(handleStatusCodes, execution);

            if (failCodes != null) {
                request.setFailCodes(getStringSetFromField(failCodes));
            }
            if (handleCodes != null) {
                request.setHandleCodes(getStringSetFromField(handleCodes));
            }

            if (request.getPrefix() == null) {
                request.setPrefix(execution.getCurrentFlowElement().getId());
            }

            // Save request fields
            if (request.isSaveRequest()) {
                execution.setVariable(request.getPrefix() + "RequestMethod", request.getMethod());
                execution.setVariable(request.getPrefix() + "RequestUrl", request.getUrl());
                execution.setVariable(request.getPrefix() + "RequestHeaders", request.getHeaders());
                execution.setVariable(request.getPrefix() + "RequestBody", request.getBody());
                execution.setVariable(request.getPrefix() + "RequestTimeout", request.getTimeout());
                execution.setVariable(request.getPrefix() + "DisallowRedirects", request.isNoRedirects());
                execution.setVariable(request.getPrefix() + "FailStatusCodes", failCodes);
                execution.setVariable(request.getPrefix() + "HandleStatusCodes", handleCodes);
                execution.setVariable(request.getPrefix() + "IgnoreException", request.isIgnoreErrors());
                execution.setVariable(request.getPrefix() + "SaveRequestVariables", request.isSaveRequest());
                execution.setVariable(request.getPrefix() + "SaveResponseParameters", request.isSaveResponse());
            }

        } catch (Exception e) {
            if (e instanceof FlowableException) {
                throw (FlowableException) e;
            } else {
                throw new FlowableException(HTTP_TASK_REQUEST_FIELD_INVALID + " in execution " + execution.getId(), e);
            }
        }

        httpActivityExecutor.validate(request);
        
        ProcessEngineConfigurationImpl processEngineConfiguration = CommandContextUtil.getProcessEngineConfiguration();
        HttpClientConfig httpClientConfig = CommandContextUtil.getProcessEngineConfiguration().getHttpClientConfig();

        httpActivityExecutor.execute(
                request,
                execution,
                execution.getId(),
                createHttpRequestHandler(httpServiceTask.getHttpRequestHandler(), processEngineConfiguration),
                createHttpResponseHandler(httpServiceTask.getHttpResponseHandler(), processEngineConfiguration),
                getStringFromField(responseVariableName, execution),
                mapExceptions,
                httpClientConfig.getSocketTimeout(),
                httpClientConfig.getConnectTimeout(),
                httpClientConfig.getConnectionRequestTimeout());

        leave(execution);
    }

    protected HttpRequestHandler createHttpRequestHandler(FlowableHttpRequestHandler handler, ProcessEngineConfigurationImpl processEngineConfiguration) {
        HttpRequestHandler requestHandler = null;

        if (handler != null) {
            if (IMPLEMENTATION_TYPE_CLASS.equalsIgnoreCase(handler.getImplementationType())) {
                requestHandler = new ClassDelegateHttpHandler(handler.getImplementation(),
                        createFieldDeclarations(handler.getFieldExtensions(), processEngineConfiguration));

            } else if (IMPLEMENTATION_TYPE_DELEGATEEXPRESSION.equalsIgnoreCase(handler.getImplementationType())) {
                requestHandler = new DelegateExpressionHttpHandler(processEngineConfiguration.getExpressionManager().createExpression(handler.getImplementation()),
                        createFieldDeclarations(handler.getFieldExtensions(), processEngineConfiguration));
            }
        }
        return requestHandler;
    }

    protected HttpResponseHandler createHttpResponseHandler(FlowableHttpResponseHandler handler, ProcessEngineConfigurationImpl processEngineConfiguration) {
        HttpResponseHandler responseHandler = null;

        if (handler != null) {
            if (ImplementationType.IMPLEMENTATION_TYPE_CLASS.equalsIgnoreCase(handler.getImplementationType())) {
                responseHandler = new ClassDelegateHttpHandler(handler.getImplementation(),
                        createFieldDeclarations(handler.getFieldExtensions(), processEngineConfiguration));

            } else if (ImplementationType.IMPLEMENTATION_TYPE_DELEGATEEXPRESSION.equalsIgnoreCase(handler.getImplementationType())) {
                responseHandler = new DelegateExpressionHttpHandler(processEngineConfiguration.getExpressionManager().createExpression(handler.getImplementation()),
                        createFieldDeclarations(handler.getFieldExtensions(), processEngineConfiguration));
            }
        }
        return responseHandler;
    }

    protected List<FieldDeclaration> createFieldDeclarations(List<FieldExtension> fieldList, ProcessEngineConfigurationImpl processEngineConfiguration) {
        List<FieldDeclaration> fieldDeclarations = new ArrayList<>();

        for (FieldExtension fieldExtension : fieldList) {
            FieldDeclaration fieldDeclaration;
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

    public void setServiceTask(ServiceTask serviceTask) {
        this.httpServiceTask = (HttpServiceTask) serviceTask;
    }

}
