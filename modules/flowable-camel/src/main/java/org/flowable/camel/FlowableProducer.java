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
package org.flowable.camel;

import java.util.Map;

import org.apache.camel.Exchange;
import org.apache.camel.impl.DefaultProducer;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.engine.IdentityService;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.compatibility.Flowable5CompatibilityHandler;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.util.Flowable5Util;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ProcessInstance;

/**
 * @author Saeid Mirzaei
 * @author Maciej Próchniak
 * @author Arnold Schrijver
 */
public class FlowableProducer extends DefaultProducer {

    protected IdentityService identityService;

    protected RuntimeService runtimeService;

    protected RepositoryService repositoryService;

    public static final String PROCESS_KEY_PROPERTY = "PROCESS_KEY_PROPERTY";

    public static final String PROCESS_ID_PROPERTY = "PROCESS_ID_PROPERTY";

    public static final String EXECUTION_ID_PROPERTY = "EXECUTION_ID_PROPERTY";

    private final long timeout;

    private final long timeResolution;

    private String processKey;

    private String activity;

    public FlowableProducer(FlowableEndpoint endpoint, long timeout, long timeResolution) {
        super(endpoint);
        String[] path = endpoint.getEndpointKey().split(":");
        processKey = path[1].replace("//", "");
        if (path.length > 2) {
            activity = path[2];
        }
        this.timeout = timeout;
        this.timeResolution = timeResolution;
    }

    @Override
    public void process(Exchange exchange) throws Exception {
        if (shouldStartProcess()) {
            ProcessInstance pi = startProcess(exchange);
            copyResultToCamel(exchange, pi);
        } else {
            signal(exchange);
        }
    }

    protected void copyResultToCamel(Exchange exchange, ProcessInstance pi) {
        exchange.setProperty(PROCESS_ID_PROPERTY, pi.getProcessInstanceId());

        Map<String, Object> returnVars = getFlowableEndpoint().getReturnVarMap();

        if (returnVars != null && returnVars.size() > 0) {

            Map<String, Object> processVariables = null;
            if (repositoryService.isFlowable5ProcessDefinition(pi.getProcessDefinitionId())) {
                Flowable5CompatibilityHandler compatibilityHandler = Flowable5Util.getFlowable5CompatibilityHandler();
                processVariables = compatibilityHandler.getVariables(pi);
            } else {
                processVariables = ((ExecutionEntity) pi).getVariables();
            }

            if (processVariables != null) {
                for (String variableName : returnVars.keySet()) {
                    if (processVariables.containsKey(variableName)) {
                        exchange.setProperty(variableName, processVariables.get(variableName));
                    }
                }
            }
        }
    }

    protected boolean shouldStartProcess() {
        return activity == null;
    }

    protected void signal(Exchange exchange) {
        String processInstanceId = findProcessInstanceId(exchange);
        String executionId = exchange.getProperty(EXECUTION_ID_PROPERTY, String.class);

        boolean firstTime = true;
        long initialTime = System.currentTimeMillis();

        Execution execution = null;
        while (firstTime || (timeout > 0 && (System.currentTimeMillis() - initialTime < timeout))) {
            try {
                Thread.sleep(timeResolution);
            } catch (InterruptedException e) {
                throw new FlowableException("error occurred while waiting for activity=" + activity + " for processInstanceId=" + processInstanceId);
            }
            firstTime = false;

            if (executionId != null) {
                execution = runtimeService.createExecutionQuery()
                        .executionId(executionId)
                        .activityId(activity)
                        .singleResult();

            } else {
                execution = runtimeService.createExecutionQuery()
                        .processDefinitionKey(processKey)
                        .processInstanceId(processInstanceId)
                        .activityId(activity)
                        .singleResult();
            }

            if (execution != null) {
                break;
            }
        }
        if (execution == null) {
            throw new FlowableException("Couldn't find activity " + activity + " for processId " + processInstanceId +
                    " in defined timeout of " + timeout + " ms.");
        }

        runtimeService.setVariables(execution.getId(), ExchangeUtils.prepareVariables(exchange, getFlowableEndpoint()));
        runtimeService.trigger(execution.getId());
    }

    protected String findProcessInstanceId(Exchange exchange) {
        String processInstanceId = exchange.getProperty(PROCESS_ID_PROPERTY, String.class);
        if (processInstanceId != null) {
            return processInstanceId;
        }
        String key = exchange.getProperty(PROCESS_KEY_PROPERTY, String.class);
        ProcessInstance processInstance = runtimeService.createProcessInstanceQuery().processInstanceBusinessKey(key).singleResult();

        if (processInstance == null) {
            throw new FlowableException("Could not start process instance with business key " + key);
        }
        return processInstance.getId();
    }

    protected ProcessInstance startProcess(Exchange exchange) {
        FlowableEndpoint endpoint = getFlowableEndpoint();
        String key = exchange.getProperty(PROCESS_KEY_PROPERTY, String.class);
        try {
            if (endpoint.isSetProcessInitiator()) {
                setProcessInitiator(ExchangeUtils.prepareInitiator(exchange, endpoint));
            }

            if (key == null) {
                return runtimeService.startProcessInstanceByKey(processKey, ExchangeUtils.prepareVariables(exchange, endpoint));
            } else {
                return runtimeService.startProcessInstanceByKey(processKey, key, ExchangeUtils.prepareVariables(exchange, endpoint));
            }

        } finally {
            if (endpoint.isSetProcessInitiator()) {
                setProcessInitiator(null);
            }
        }
    }

    protected void setProcessInitiator(String processInitiator) {
        if (identityService == null) {
            throw new FlowableException("IdentityService is missing and must be provided to set process initiator.");
        }
        identityService.setAuthenticatedUserId(processInitiator);
    }

    protected FlowableEndpoint getFlowableEndpoint() {
        return (FlowableEndpoint) getEndpoint();
    }

    public void setIdentityService(IdentityService identityService) {
        this.identityService = identityService;
    }

    public void setRuntimeService(RuntimeService runtimeService) {
        this.runtimeService = runtimeService;
    }

    public void setRepositoryService(RepositoryService repositoryService) {
        this.repositoryService = repositoryService;
    }
}
