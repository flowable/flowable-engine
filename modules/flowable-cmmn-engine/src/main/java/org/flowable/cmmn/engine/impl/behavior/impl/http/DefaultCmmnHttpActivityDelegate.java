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
package org.flowable.cmmn.engine.impl.behavior.impl.http;

import static org.flowable.bpmn.model.ImplementationType.IMPLEMENTATION_TYPE_CLASS;
import static org.flowable.bpmn.model.ImplementationType.IMPLEMENTATION_TYPE_DELEGATEEXPRESSION;
import static org.flowable.common.engine.impl.util.ExceptionUtil.sneakyThrow;

import java.util.concurrent.CompletableFuture;

import org.flowable.cmmn.api.delegate.DelegatePlanItemInstance;
import org.flowable.cmmn.api.delegate.PlanItemFutureJavaDelegate;
import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.HttpClientConfig;
import org.flowable.cmmn.engine.impl.behavior.impl.http.handler.ClassDelegateHttpHandler;
import org.flowable.cmmn.engine.impl.behavior.impl.http.handler.DelegateExpressionHttpHandler;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.cmmn.model.FlowableHttpRequestHandler;
import org.flowable.cmmn.model.FlowableHttpResponseHandler;
import org.flowable.cmmn.model.HttpServiceTask;
import org.flowable.cmmn.model.ImplementationType;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.async.AsyncTaskInvoker;
import org.flowable.common.engine.api.variable.VariableContainer;
import org.flowable.http.common.api.HttpRequest;
import org.flowable.http.common.api.HttpResponse;
import org.flowable.http.common.api.client.FlowableHttpClient;
import org.flowable.http.common.api.delegate.HttpRequestHandler;
import org.flowable.http.common.api.delegate.HttpResponseHandler;
import org.flowable.http.common.impl.BaseHttpActivityDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Filip Hrisafov
 */
public class DefaultCmmnHttpActivityDelegate extends BaseHttpActivityDelegate implements
        PlanItemFutureJavaDelegate<BaseHttpActivityDelegate.ExecutionData> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultCmmnHttpActivityDelegate.class);

    public DefaultCmmnHttpActivityDelegate() {
        this(null);
    }

    public DefaultCmmnHttpActivityDelegate(FlowableHttpClient httpClient) {
        super(httpClient);
    }

    @Override
    protected FlowableHttpClient createHttpClient() {
        HttpClientConfig config = CommandContextUtil.getCmmnEngineConfiguration().getHttpClientConfig();
        return config.determineHttpClient();
    }

    @Override
    public CompletableFuture<ExecutionData> execute(DelegatePlanItemInstance planItemInstance, AsyncTaskInvoker taskInvoker) {
        HttpRequest request;

        HttpServiceTask httpServiceTask = (HttpServiceTask) planItemInstance.getPlanItemDefinition();
        try {
            request = createRequest(planItemInstance, httpServiceTask.getId());

        } catch (Exception e) {
            if (e instanceof FlowableException) {
                throw (FlowableException) e;
            } else {
                throw new FlowableException(HTTP_TASK_REQUEST_FIELD_INVALID + " in execution " + planItemInstance.getId(), e);
            }
        }

        CmmnEngineConfiguration cmmnEngineConfiguration = CommandContextUtil.getCmmnEngineConfiguration();
        HttpRequestHandler httpRequestHandler = createHttpRequestHandler(httpServiceTask.getHttpRequestHandler(), cmmnEngineConfiguration);

        if (httpRequestHandler != null) {
            httpRequestHandler.handleHttpRequest(planItemInstance, request, null);
        }

        // Validate request
        validateRequest(request);

        boolean parallelInSameTransaction;
        if (httpServiceTask.getParallelInSameTransaction() != null) {
            parallelInSameTransaction = httpServiceTask.getParallelInSameTransaction();
        } else {
            parallelInSameTransaction = cmmnEngineConfiguration.getHttpClientConfig().isDefaultParallelInSameTransaction();
        }

        return prepareAndExecuteRequest(request, parallelInSameTransaction, taskInvoker);
    }

    @Override
    public void afterExecution(DelegatePlanItemInstance planItemInstance, ExecutionData result) {

        HttpRequest request = result.getRequest();
        HttpResponse response = result.getResponse();

        Throwable resultException = result.getException();
        if (resultException != null) {
            if (request.isIgnoreErrors()) {
                LOGGER.info("Error ignored while processing http task in planItemInstance {}", planItemInstance.getId(), resultException);
                planItemInstance.setVariable(request.getPrefix() + "ErrorMessage", resultException.getMessage());
                return;
            }

            sneakyThrow(resultException);
        }

        CmmnEngineConfiguration cmmnEngineConfiguration = CommandContextUtil.getCmmnEngineConfiguration();
        HttpServiceTask httpServiceTask = (HttpServiceTask) planItemInstance.getPlanItemDefinition();

        try {
            // Pass request through response handler
            HttpResponseHandler httpResponseHandler = createHttpResponseHandler(httpServiceTask.getHttpResponseHandler(), cmmnEngineConfiguration);

            if (httpResponseHandler != null) {
                httpResponseHandler.handleHttpResponse(planItemInstance, response);
            }

            // Save response fields in the planItemInstance
            saveResponseFields(planItemInstance, request, response, cmmnEngineConfiguration.getObjectMapper());
        } catch (Exception ex) {
            if (request.isIgnoreErrors()) {
                LOGGER.info("Error ignored while processing http task in planItemInstance {}", planItemInstance.getId(), ex);
                planItemInstance.setVariable(request.getPrefix() + "ErrorMessage", ex.getMessage());
            } else {
                sneakyThrow(ex);
            }
        }
    }

    protected HttpRequestHandler createHttpRequestHandler(FlowableHttpRequestHandler handler, CmmnEngineConfiguration cmmnEngineConfiguration) {
        HttpRequestHandler requestHandler = null;

        if (handler != null) {
            if (IMPLEMENTATION_TYPE_CLASS.equalsIgnoreCase(handler.getImplementationType())) {
                requestHandler = new ClassDelegateHttpHandler(handler.getImplementation(),
                        handler.getFieldExtensions());

            } else if (IMPLEMENTATION_TYPE_DELEGATEEXPRESSION.equalsIgnoreCase(handler.getImplementationType())) {
                requestHandler = new DelegateExpressionHttpHandler(
                        cmmnEngineConfiguration.getExpressionManager().createExpression(handler.getImplementation()),
                        handler.getFieldExtensions());
            }
        }
        return requestHandler;
    }

    protected HttpResponseHandler createHttpResponseHandler(FlowableHttpResponseHandler handler, CmmnEngineConfiguration cmmnEngineConfiguration) {
        HttpResponseHandler responseHandler = null;

        if (handler != null) {
            if (ImplementationType.IMPLEMENTATION_TYPE_CLASS.equalsIgnoreCase(handler.getImplementationType())) {
                responseHandler = new ClassDelegateHttpHandler(handler.getImplementation(),
                        handler.getFieldExtensions());

            } else if (ImplementationType.IMPLEMENTATION_TYPE_DELEGATEEXPRESSION.equalsIgnoreCase(handler.getImplementationType())) {
                responseHandler = new DelegateExpressionHttpHandler(
                        cmmnEngineConfiguration.getExpressionManager().createExpression(handler.getImplementation()),
                        handler.getFieldExtensions());
            }
        }
        return responseHandler;
    }

    @Override
    protected void propagateError(VariableContainer container, String code) {
        // NOP
    }

}
