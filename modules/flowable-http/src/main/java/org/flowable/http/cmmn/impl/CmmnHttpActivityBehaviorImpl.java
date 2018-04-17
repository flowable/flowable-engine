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
package org.flowable.http.cmmn.impl;

import static org.flowable.http.ExpressionUtils.getBooleanFromField;
import static org.flowable.http.ExpressionUtils.getStringFromField;
import static org.flowable.http.ExpressionUtils.getStringSetFromField;
import static org.flowable.http.HttpActivityExecutor.HTTP_TASK_REQUEST_FIELD_INVALID;

import java.util.Collections;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.ssl.SSLContextBuilder;
import org.flowable.bpmn.model.MapExceptionEntry;
import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.HttpClientConfig;
import org.flowable.cmmn.engine.impl.behavior.CoreCmmnActivityBehavior;
import org.flowable.cmmn.engine.impl.persistence.entity.PlanItemInstanceEntity;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.cmmn.model.FlowableHttpRequestHandler;
import org.flowable.cmmn.model.FlowableHttpResponseHandler;
import org.flowable.cmmn.model.HttpServiceTask;
import org.flowable.cmmn.model.ImplementationType;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.delegate.Expression;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.http.ExpressionUtils;
import org.flowable.http.HttpActivityExecutor;
import org.flowable.http.HttpRequest;
import org.flowable.http.NopErrorPropagator;
import org.flowable.http.cmmn.impl.handler.ClassDelegateHttpHandler;
import org.flowable.http.cmmn.impl.handler.DelegateExpressionHttpHandler;
import org.flowable.http.delegate.HttpRequestHandler;
import org.flowable.http.delegate.HttpResponseHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class provides http task for cmmn models
 *
 * @author martin.grofcik
 */
public class CmmnHttpActivityBehaviorImpl extends CoreCmmnActivityBehavior {

    private static final Logger LOGGER = LoggerFactory.getLogger(CmmnHttpActivityBehaviorImpl.class);

    // HttpRequest method (GET,POST,PUT etc)
    protected String requestMethod;
    // HttpRequest URL (http://flowable.org)
    protected String requestUrl;
    // Line separated HTTP body headers(Optional)
    protected String requestHeaders;
    // HttpRequest body expression (Optional)
    protected String requestBody;
    // Timeout in seconds for the body (Optional)
    protected String requestTimeout;
    // HttpRequest retry disable HTTP redirects (Optional)
    protected String disallowRedirects;
    // Comma separated list of HTTP body status codes to fail, for example 400,5XX (Optional)
    protected String failStatusCodes;
    // Comma separated list of HTTP body status codes to handle, for example 404,3XX (Optional)
    protected String handleStatusCodes;
    // Flag to ignore exceptions (Optional)
    protected String ignoreException;
    // Flag to save request variables. default is false (Optional)
    protected String saveRequestVariables;
    // Flag to save response variables. default is false (Optional)
    protected String saveResponseParameters;
    // Variable name for response body
    protected String responseVariableName;
    // Flag to save the response variables as a transient variable. Default is false (Optional).
    protected String saveResponseParametersTransient;
    // Flag to save the response variable as an ObjectNode instead of a String
    protected String saveResponseVariableAsJson;
    // Prefix for the execution variable names (Optional)
    protected String resultVariablePrefix;

    protected HttpServiceTask serviceTask;
    protected HttpActivityExecutor httpActivityExecutor;

    public CmmnHttpActivityBehaviorImpl() {
        org.flowable.cmmn.engine.HttpClientConfig config = CommandContextUtil.getCmmnEngineConfiguration().getHttpClientConfig();
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

        this.httpActivityExecutor = new HttpActivityExecutor(httpClientBuilder, new NopErrorPropagator(), 
                CommandContextUtil.getCmmnEngineConfiguration().getObjectMapper());
    }


    @Override
    public void execute(CommandContext commandContext, PlanItemInstanceEntity planItemInstanceEntity) {
        HttpRequest request = new HttpRequest();

        try {
            request.setMethod(ExpressionUtils.getStringFromField(createExpression(requestMethod), planItemInstanceEntity));
            request.setUrl(ExpressionUtils.getStringFromField(createExpression(requestUrl), planItemInstanceEntity));
            request.setHeaders(ExpressionUtils.getStringFromField(createExpression(requestHeaders), planItemInstanceEntity));
            request.setBody(ExpressionUtils.getStringFromField(createExpression(requestBody), planItemInstanceEntity));
            request.setTimeout(ExpressionUtils.getIntFromField(createExpression(requestTimeout), planItemInstanceEntity));
            request.setNoRedirects(ExpressionUtils.getBooleanFromField(createExpression(disallowRedirects), planItemInstanceEntity));
            request.setIgnoreErrors(ExpressionUtils.getBooleanFromField(createExpression(ignoreException), planItemInstanceEntity));
            request.setSaveRequest(ExpressionUtils.getBooleanFromField(createExpression(saveRequestVariables), planItemInstanceEntity));
            request.setSaveResponse(ExpressionUtils.getBooleanFromField(createExpression(saveResponseParameters), planItemInstanceEntity));
            request.setSaveResponseTransient(getBooleanFromField(createExpression(saveResponseParametersTransient), planItemInstanceEntity));
            request.setSaveResponseAsJson(getBooleanFromField(createExpression(saveResponseVariableAsJson), planItemInstanceEntity));
            request.setPrefix(ExpressionUtils.getStringFromField(createExpression(resultVariablePrefix), planItemInstanceEntity));

            String failCodes = ExpressionUtils.getStringFromField(createExpression(failStatusCodes), planItemInstanceEntity);
            String handleCodes = ExpressionUtils.getStringFromField(createExpression(handleStatusCodes), planItemInstanceEntity);

            if (failCodes != null) {
                request.setFailCodes(getStringSetFromField(failCodes));
            }
            if (handleCodes != null) {
                request.setHandleCodes(getStringSetFromField(handleCodes));
            }

            if (request.getPrefix() == null) {
                request.setPrefix(planItemInstanceEntity.getElementId());
            }

            // Save request fields
            if (request.isSaveRequest()) {
                planItemInstanceEntity.setVariable(request.getPrefix() + "RequestMethod", request.getMethod());
                planItemInstanceEntity.setVariable(request.getPrefix() + "RequestUrl", request.getUrl());
                planItemInstanceEntity.setVariable(request.getPrefix() + "RequestHeaders", request.getHeaders());
                planItemInstanceEntity.setVariable(request.getPrefix() + "RequestBody", request.getBody());
                planItemInstanceEntity.setVariable(request.getPrefix() + "RequestTimeout", request.getTimeout());
                planItemInstanceEntity.setVariable(request.getPrefix() + "DisallowRedirects", request.isNoRedirects());
                planItemInstanceEntity.setVariable(request.getPrefix() + "FailStatusCodes", failCodes);
                planItemInstanceEntity.setVariable(request.getPrefix() + "HandleStatusCodes", handleCodes);
                planItemInstanceEntity.setVariable(request.getPrefix() + "IgnoreException", request.isIgnoreErrors());
                planItemInstanceEntity.setVariable(request.getPrefix() + "SaveRequestVariables", request.isSaveRequest());
                planItemInstanceEntity.setVariable(request.getPrefix() + "SaveResponseParameters", request.isSaveResponse());
            }

        } catch (Exception e) {
            if (e instanceof FlowableException) {
                throw (FlowableException) e;
            } else {
                throw new FlowableException(HTTP_TASK_REQUEST_FIELD_INVALID + " in execution " + planItemInstanceEntity.getId(), e);
            }
        }

        httpActivityExecutor.validate(request);
        
        CmmnEngineConfiguration cmmnEngineConfiguration = CommandContextUtil.getCmmnEngineConfiguration();
        HttpClientConfig httpClientConfig = cmmnEngineConfiguration.getHttpClientConfig();

        httpActivityExecutor.execute(
                request,
                planItemInstanceEntity,
                planItemInstanceEntity.getId(),
                createHttpRequestHandler(serviceTask.getHttpRequestHandler(), cmmnEngineConfiguration),
                createHttpResponseHandler(serviceTask.getHttpResponseHandler(), cmmnEngineConfiguration),
                getStringFromField(createExpression(responseVariableName), planItemInstanceEntity),
                Collections.<MapExceptionEntry>emptyList(),
                httpClientConfig.getSocketTimeout(),
                httpClientConfig.getConnectTimeout(),
                httpClientConfig.getConnectionRequestTimeout()
        );

        CommandContextUtil.getAgenda().planCompletePlanItemInstanceOperation(planItemInstanceEntity);

    }

    protected Expression createExpression(String expressionString) {
        if (StringUtils.isEmpty(expressionString)) {
            return null;
        }
        return CommandContextUtil.getCmmnEngineConfiguration().getExpressionManager().createExpression(expressionString);
    }

    protected HttpRequestHandler createHttpRequestHandler(FlowableHttpRequestHandler flowableHandler, CmmnEngineConfiguration cmmnEngineConfiguration) {
        HttpRequestHandler handler = null;

        if (flowableHandler != null) {
            if (ImplementationType.IMPLEMENTATION_TYPE_CLASS.equalsIgnoreCase(flowableHandler.getImplementationType())) {
                handler = new ClassDelegateHttpHandler(flowableHandler.getImplementation(),
                        flowableHandler.getFieldExtensions());

            } else if (ImplementationType.IMPLEMENTATION_TYPE_DELEGATEEXPRESSION.equalsIgnoreCase(flowableHandler.getImplementationType())) {
                handler = new DelegateExpressionHttpHandler(cmmnEngineConfiguration.getExpressionManager().createExpression(flowableHandler.getImplementation()),
                        flowableHandler.getFieldExtensions());
            }
        }
        return handler;
    }

    protected HttpResponseHandler createHttpResponseHandler(FlowableHttpResponseHandler handler, CmmnEngineConfiguration cmmnEngineConfiguration) {
        HttpResponseHandler responseHandler = null;

        if (handler != null) {
            if (ImplementationType.IMPLEMENTATION_TYPE_CLASS.equalsIgnoreCase(handler.getImplementationType())) {
                responseHandler = new ClassDelegateHttpHandler(handler.getImplementation(),
                        handler.getFieldExtensions());

            } else if (ImplementationType.IMPLEMENTATION_TYPE_DELEGATEEXPRESSION.equalsIgnoreCase(handler.getImplementationType())) {
                responseHandler = new DelegateExpressionHttpHandler(cmmnEngineConfiguration.getExpressionManager().createExpression(handler.getImplementation()),
                        handler.getFieldExtensions());
            }
        }
        return responseHandler;
    }

    public void setServiceTask(HttpServiceTask serviceTask) {
        this.serviceTask = serviceTask;
    }

}
