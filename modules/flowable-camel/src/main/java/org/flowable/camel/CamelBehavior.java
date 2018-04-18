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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.camel.CamelContext;
import org.apache.camel.Endpoint;
import org.apache.camel.Exchange;
import org.flowable.bpmn.model.Activity;
import org.flowable.bpmn.model.MapExceptionEntry;
import org.flowable.bpmn.model.Process;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.delegate.Expression;
import org.flowable.common.engine.impl.context.Context;
import org.flowable.engine.compatibility.Flowable5CompatibilityHandler;
import org.flowable.engine.delegate.BpmnError;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.impl.bpmn.behavior.AbstractBpmnActivityBehavior;
import org.flowable.engine.impl.bpmn.helper.ErrorPropagation;
import org.flowable.engine.impl.delegate.ActivityBehavior;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.util.Flowable5Util;
import org.flowable.engine.impl.util.ProcessDefinitionUtil;

/**
 * This abstract class takes the place of the now-deprecated CamelBehaviour class (which can still be used for legacy compatibility) and significantly improves on its flexibility. Additional
 * implementations can be created that change the way in which Flowable interacts with Camel per your specific needs.
 * <p>
 * Three out-of-the-box implementations of CamelBehavior are provided: (1) CamelBehaviorDefaultImpl: Works just like CamelBehaviour does; copies variables into and out of Camel as or from properties.
 * (2) CamelBehaviorBodyAsMapImpl: Works by copying variables into and out of Camel using a Map<String,Object> object in the body. (3) CamelBehaviorCamelBodyImpl: Works by copying a single variable
 * value into Camel as a String body and copying the Camel body into that same variable. The process variable in must be named "camelBody".
 * <p>
 * This class has two subclasses one for Spring and one for CDI, and each having the 3 behavioral implementations discussed above. Alternative implementations for Spring or CDI contexts should 
 * now extend one one or the other. 
 * <p>
 * The chosen implementation can be set using extension elements:
 * <p>
 * <pre>
 * {@code
 * <serviceTask id="serviceTask1" flowable:type="camel">
 *   <extensionElements>
 *     <flowable:field name="camelBehaviorClass" stringValue="org.flowable.camel.impl.CamelBehaviorCamelBodyImpl" />
 *   </extensionElements>
 * </serviceTask>
 * }
 * </pre>
 * <p>
 * Note also that the manner in which variables are copied to the process engine from Camel has changed. It will always copy Camel properties to the process variables set; they can safely be ignored,
 * of course, if not required. It will conditionally copy the Camel body to the "camelBody" variable if it is of type java.lang.String, OR it will copy the Camel body to individual variables within
 * the process engine if it is of type Map<String,Object>.
 *
 * @author Ryan Johnston (@rjfsu), Tijs Rademakers, Saeid Mirzaei, Zach Visagie
 * @version 5.12
 */
public abstract class CamelBehavior extends AbstractBpmnActivityBehavior implements ActivityBehavior {

    private static final long serialVersionUID = 1L;
    protected Expression camelContext;
    protected CamelContext camelContextObj;
    protected List<MapExceptionEntry> mapExceptions;

    protected abstract void setPropertTargetVariable(FlowableEndpoint endpoint);

    public enum TargetType {
        BODY_AS_MAP, BODY, PROPERTIES
    }

    protected TargetType toTargetType;

    protected void updateTargetVariables(FlowableEndpoint endpoint) {
        toTargetType = null;
        if (endpoint.isCopyVariablesToBodyAsMap())
            toTargetType = TargetType.BODY_AS_MAP;
        else if (endpoint.isCopyCamelBodyToBody())
            toTargetType = TargetType.BODY;
        else if (endpoint.isCopyVariablesToProperties())
            toTargetType = TargetType.PROPERTIES;

        if (toTargetType == null)
            setPropertTargetVariable(endpoint);
    }

    protected void copyVariables(Map<String, Object> variables, Exchange exchange, FlowableEndpoint endpoint) {
        switch (toTargetType) {
            case BODY_AS_MAP:
                copyVariablesToBodyAsMap(variables, exchange);
                break;

            case BODY:
                copyVariablesToBody(variables, exchange);
                break;

            case PROPERTIES:
                copyVariablesToProperties(variables, exchange);
        }
    }

    @Override
    public void execute(DelegateExecution execution) {
        setAppropriateCamelContext(execution);

        final FlowableEndpoint endpoint = createEndpoint(execution);
        final Exchange exchange = createExchange(execution, endpoint);

        try {
            endpoint.process(exchange);
        } catch (Exception e) {
            throw new FlowableException("Exception while processing exchange", e);
        }
        execution.setVariables(ExchangeUtils.prepareVariables(exchange, endpoint));

        boolean isV5Execution = false;
        if ((Context.getCommandContext() != null && Flowable5Util.isFlowable5ProcessDefinitionId(Context.getCommandContext(), execution.getProcessDefinitionId())) ||
                (Context.getCommandContext() == null && Flowable5Util.getFlowable5CompatibilityHandler() != null)) {

            isV5Execution = true;
        }

        if (!handleCamelException(exchange, execution, isV5Execution)) {
            if (isV5Execution) {
                Flowable5CompatibilityHandler compatibilityHandler = Flowable5Util.getFlowable5CompatibilityHandler();
                compatibilityHandler.leaveExecution(execution);
                return;
            }
            leave(execution);
        }
    }

    protected FlowableEndpoint createEndpoint(DelegateExecution execution) {
        String uri = "flowable://" + getProcessDefinitionKey(execution) + ":" + execution.getCurrentActivityId();
        return getEndpoint(uri);
    }

    protected FlowableEndpoint getEndpoint(String key) {
        for (Endpoint e : camelContextObj.getEndpoints()) {
            if (e.getEndpointKey().equals(key) && (e instanceof FlowableEndpoint)) {
                return (FlowableEndpoint) e;
            }
        }
        throw new FlowableException("Endpoint not defined for " + key);
    }

    protected Exchange createExchange(DelegateExecution activityExecution, FlowableEndpoint endpoint) {
        Exchange ex = endpoint.createExchange();
        ex.setProperty(FlowableProducer.PROCESS_ID_PROPERTY, activityExecution.getProcessInstanceId());
        ex.setProperty(FlowableProducer.EXECUTION_ID_PROPERTY, activityExecution.getId());
        Map<String, Object> variables = activityExecution.getVariables();
        updateTargetVariables(endpoint);
        copyVariables(variables, ex, endpoint);
        return ex;
    }

    protected boolean handleCamelException(Exchange exchange, DelegateExecution execution, boolean isV5Execution) {
        Exception camelException = exchange.getException();
        boolean notHandledByCamel = exchange.isFailed() && camelException != null;
        if (notHandledByCamel) {
            if (camelException instanceof BpmnError) {
                if (isV5Execution) {
                    Flowable5CompatibilityHandler compatibilityHandler = Flowable5Util.getFlowable5CompatibilityHandler();
                    compatibilityHandler.propagateError((BpmnError) camelException, execution);
                    return true;
                }
                ErrorPropagation.propagateError((BpmnError) camelException, execution);
                return true;
            } else {
                if (isV5Execution) {
                    Flowable5CompatibilityHandler ompatibilityHandler = Flowable5Util.getFlowable5CompatibilityHandler();
                    if (ompatibilityHandler.mapException(camelException, execution, mapExceptions)) {
                        return true;
                    } else {
                        throw new FlowableException("Unhandled exception on camel route", camelException);
                    }
                }

                if (ErrorPropagation.mapException(camelException, (ExecutionEntity) execution, mapExceptions)) {
                    return true;
                } else {
                    throw new FlowableException("Unhandled exception on camel route", camelException);
                }
            }
        }
        return false;
    }

    protected void copyVariablesToProperties(Map<String, Object> variables, Exchange exchange) {
        for (Map.Entry<String, Object> var : variables.entrySet()) {
            exchange.setProperty(var.getKey(), var.getValue());
        }
    }

    protected void copyVariablesToBodyAsMap(Map<String, Object> variables, Exchange exchange) {
        exchange.getIn().setBody(new HashMap<>(variables));
    }

    protected void copyVariablesToBody(Map<String, Object> variables, Exchange exchange) {
        Object camelBody = variables.get(ExchangeUtils.CAMELBODY);
        if (camelBody != null) {
            exchange.getIn().setBody(camelBody);
        }
    }

    protected String getProcessDefinitionKey(DelegateExecution execution) {
        Process process = ProcessDefinitionUtil.getProcess(execution.getProcessDefinitionId());
        return process.getId();
    }

    protected boolean isASync(DelegateExecution execution) {
        boolean async = false;
        if (execution.getCurrentFlowElement() instanceof Activity) {
            async = ((Activity) execution.getCurrentFlowElement()).isAsynchronous();
        }
        return async;
    }

    protected abstract void setAppropriateCamelContext(DelegateExecution execution);

    protected String getStringFromField(Expression expression, DelegateExecution execution) {
        if (expression != null) {
            Object value = expression.getValue(execution);
            if (value != null) {
                return value.toString();
            }
        }
        return null;
    }

    public void setCamelContext(Expression camelContext) {
        this.camelContext = camelContext;
    }
}
