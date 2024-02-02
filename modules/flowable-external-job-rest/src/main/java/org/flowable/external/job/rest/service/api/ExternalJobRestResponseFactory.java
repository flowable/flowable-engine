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
package org.flowable.external.job.rest.service.api;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.rest.util.RestUrlBuilder;
import org.flowable.common.rest.variable.BooleanRestVariableConverter;
import org.flowable.common.rest.variable.DateRestVariableConverter;
import org.flowable.common.rest.variable.DoubleRestVariableConverter;
import org.flowable.common.rest.variable.EngineRestVariable;
import org.flowable.common.rest.variable.InstantRestVariableConverter;
import org.flowable.common.rest.variable.IntegerRestVariableConverter;
import org.flowable.common.rest.variable.JsonObjectRestVariableConverter;
import org.flowable.common.rest.variable.LocalDateRestVariableConverter;
import org.flowable.common.rest.variable.LocalDateTimeRestVariableConverter;
import org.flowable.common.rest.variable.LongRestVariableConverter;
import org.flowable.common.rest.variable.RestVariableConverter;
import org.flowable.common.rest.variable.ShortRestVariableConverter;
import org.flowable.common.rest.variable.StringRestVariableConverter;
import org.flowable.external.job.rest.service.api.acquire.AcquiredExternalWorkerJobResponse;
import org.flowable.external.job.rest.service.api.query.ExternalWorkerJobResponse;
import org.flowable.job.api.AcquiredExternalWorkerJob;
import org.flowable.job.api.ExternalWorkerJob;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author Filip Hrisafov
 */
public class ExternalJobRestResponseFactory {

    protected ObjectMapper objectMapper;
    protected List<RestVariableConverter> variableConverters = new ArrayList<>();

    public ExternalJobRestResponseFactory(ObjectMapper objectMapper, Collection<RestVariableConverter> additionalRestVariableConverters) {
        this.objectMapper = objectMapper;
        initializeVariableConverters();
        variableConverters.addAll(additionalRestVariableConverters);
    }

    public List<AcquiredExternalWorkerJobResponse> createAcquiredExternalWorkerJobResponseList(List<AcquiredExternalWorkerJob> jobs) {
        RestUrlBuilder urlBuilder = createUrlBuilder();
        List<AcquiredExternalWorkerJobResponse> responseList = new ArrayList<>(jobs.size());
        for (AcquiredExternalWorkerJob job : jobs) {
            responseList.add(createAcquiredExternalWorkerJobResponse(job, urlBuilder));
        }

        return responseList;
    }

    public List<ExternalWorkerJobResponse> createExternalWorkerJobResponseList(List<ExternalWorkerJob> jobs) {
        RestUrlBuilder urlBuilder = createUrlBuilder();
        List<ExternalWorkerJobResponse> responseList = new ArrayList<>(jobs.size());
        for (ExternalWorkerJob instance : jobs) {
            responseList.add(createExternalWorkerJobResponse(instance, urlBuilder));
        }
        return responseList;
    }

    public ExternalWorkerJobResponse createExternalWorkerJobResponse(ExternalWorkerJob job) {
        return createExternalWorkerJobResponse(job, createUrlBuilder());
    }

    protected ExternalWorkerJobResponse createExternalWorkerJobResponse(ExternalWorkerJob job, RestUrlBuilder urlBuilder) {
        return createExternalWorkerJobResponse(job, urlBuilder, ExternalWorkerJobResponse::new);
    }

    protected AcquiredExternalWorkerJobResponse createAcquiredExternalWorkerJobResponse(AcquiredExternalWorkerJob job, RestUrlBuilder urlBuilder) {
        AcquiredExternalWorkerJobResponse response = createExternalWorkerJobResponse(job, urlBuilder, AcquiredExternalWorkerJobResponse::new);
        Map<String, Object> variables = job.getVariables();
        List<EngineRestVariable> restVariables = new ArrayList<>(variables.size());
        for (Map.Entry<String, Object> variable : variables.entrySet()) {
            restVariables.add(createRestVariable(variable.getKey(), variable.getValue()));
        }

        response.setVariables(restVariables);

        return response;
    }

    protected <T extends ExternalWorkerJobResponse> T createExternalWorkerJobResponse(ExternalWorkerJob job, RestUrlBuilder urlBuilder,
            Supplier<T> responseSupplier) {
        T response = responseSupplier.get();
        response.setId(job.getId());
        response.setUrl(urlBuilder.buildUrl(ExternalJobRestUrls.URL_JOB, job.getId()));
        response.setCorrelationId(job.getCorrelationId());
        response.setProcessInstanceId(job.getProcessInstanceId());
        response.setProcessDefinitionId(job.getProcessDefinitionId());
        response.setExecutionId(job.getExecutionId());
        response.setScopeId(job.getScopeId());
        response.setSubScopeId(job.getSubScopeId());
        response.setScopeDefinitionId(job.getScopeDefinitionId());
        response.setScopeType(job.getScopeType());
        response.setElementId(job.getElementId());
        response.setElementName(job.getElementName());
        response.setRetries(job.getRetries());
        response.setExceptionMessage(job.getExceptionMessage());
        response.setDueDate(job.getDuedate());
        response.setCreateTime(job.getCreateTime());
        response.setTenantId(job.getTenantId());

        response.setLockOwner(job.getLockOwner());
        response.setLockExpirationTime(job.getLockExpirationTime());

        return response;
    }

    protected EngineRestVariable createRestVariable(String name, Object value) {
        EngineRestVariable restVariable = new EngineRestVariable();
        restVariable.setName(name);

        if (value != null) {
            RestVariableConverter converter = null;
            for (RestVariableConverter c : variableConverters) {
                if (c.getVariableType().isAssignableFrom(value.getClass())) {
                    converter = c;
                    break;
                }
            }

            if (converter != null) {
                converter.convertVariableValue(value, restVariable);
                restVariable.setType(converter.getRestTypeName());
            } else {
                // Revert to default conversion, which is the
                // serializable/byte-array form
                if (value instanceof Byte[] || value instanceof byte[]) {
                    restVariable.setType("binary");
                } else {
                    restVariable.setType("serializable");
                }
            }
        }

        return restVariable;
    }

    public Object getVariableValue(EngineRestVariable restVariable) {
        Object value;

        if (restVariable.getType() != null) {
            // Try locating a converter if the type has been specified
            RestVariableConverter converter = null;
            for (RestVariableConverter conv : variableConverters) {
                if (conv.getRestTypeName().equals(restVariable.getType())) {
                    converter = conv;
                    break;
                }
            }
            if (converter == null) {
                throw new FlowableIllegalArgumentException("Variable '" + restVariable.getName() + "' has unsupported type: '" + restVariable.getType() + "'.");
            }
            value = converter.getVariableValue(restVariable);

        } else {
            // Revert to type determined by REST-to-Java mapping when no
            // explicit type has been provided
            value = restVariable.getValue();
        }
        return value;
    }

    protected RestUrlBuilder createUrlBuilder() {
        return RestUrlBuilder.fromCurrentRequest();
    }

    protected void initializeVariableConverters() {
        variableConverters.add(new StringRestVariableConverter());
        variableConverters.add(new IntegerRestVariableConverter());
        variableConverters.add(new LongRestVariableConverter());
        variableConverters.add(new ShortRestVariableConverter());
        variableConverters.add(new DoubleRestVariableConverter());
        variableConverters.add(new BooleanRestVariableConverter());
        variableConverters.add(new DateRestVariableConverter());
        variableConverters.add(new InstantRestVariableConverter());
        variableConverters.add(new LocalDateRestVariableConverter());
        variableConverters.add(new LocalDateTimeRestVariableConverter());
        variableConverters.add(new JsonObjectRestVariableConverter(objectMapper));
    }

}
