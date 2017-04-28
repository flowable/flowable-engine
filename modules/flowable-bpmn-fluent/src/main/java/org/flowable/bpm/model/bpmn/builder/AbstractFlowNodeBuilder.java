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
package org.flowable.bpm.model.bpmn.builder;

import org.flowable.bpm.model.bpmn.BpmnModelException;
import org.flowable.bpm.model.bpmn.BpmnModelInstance;
import org.flowable.bpm.model.bpmn.instance.Activity;
import org.flowable.bpm.model.bpmn.instance.BusinessRuleTask;
import org.flowable.bpm.model.bpmn.instance.CallActivity;
import org.flowable.bpm.model.bpmn.instance.ConditionExpression;
import org.flowable.bpm.model.bpmn.instance.EndEvent;
import org.flowable.bpm.model.bpmn.instance.EventBasedGateway;
import org.flowable.bpm.model.bpmn.instance.ExclusiveGateway;
import org.flowable.bpm.model.bpmn.instance.FlowNode;
import org.flowable.bpm.model.bpmn.instance.Gateway;
import org.flowable.bpm.model.bpmn.instance.InclusiveGateway;
import org.flowable.bpm.model.bpmn.instance.IntermediateCatchEvent;
import org.flowable.bpm.model.bpmn.instance.IntermediateThrowEvent;
import org.flowable.bpm.model.bpmn.instance.ManualTask;
import org.flowable.bpm.model.bpmn.instance.ParallelGateway;
import org.flowable.bpm.model.bpmn.instance.ReceiveTask;
import org.flowable.bpm.model.bpmn.instance.ScriptTask;
import org.flowable.bpm.model.bpmn.instance.SendTask;
import org.flowable.bpm.model.bpmn.instance.SequenceFlow;
import org.flowable.bpm.model.bpmn.instance.ServiceTask;
import org.flowable.bpm.model.bpmn.instance.SubProcess;
import org.flowable.bpm.model.bpmn.instance.Transaction;
import org.flowable.bpm.model.bpmn.instance.UserTask;
import org.flowable.bpm.model.bpmn.instance.flowable.FlowableExecutionListener;
import org.flowable.bpm.model.bpmn.instance.flowable.FlowableFailedJobRetryTimeCycle;
import org.flowable.bpm.model.xml.instance.ModelElementInstance;

public abstract class AbstractFlowNodeBuilder<B extends AbstractFlowNodeBuilder<B, E>, E extends FlowNode>
        extends AbstractFlowElementBuilder<B, E> {

    private SequenceFlowBuilder currentSequenceFlowBuilder;

    protected AbstractFlowNodeBuilder(BpmnModelInstance modelInstance, E element, Class<?> selfType) {
        super(modelInstance, element, selfType);
    }

    private SequenceFlowBuilder getCurrentSequenceFlowBuilder() {
        if (currentSequenceFlowBuilder == null) {
            SequenceFlow sequenceFlow = createSibling(SequenceFlow.class);
            currentSequenceFlowBuilder = sequenceFlow.builder();
        }
        return currentSequenceFlowBuilder;
    }

    public B condition(String name, String condition) {
        if (name != null) {
            getCurrentSequenceFlowBuilder().name(name);
        }
        ConditionExpression conditionExpression = createInstance(ConditionExpression.class);
        conditionExpression.setTextContent(condition);
        getCurrentSequenceFlowBuilder().condition(conditionExpression);
        return myself;
    }

    protected void connectTarget(FlowNode target) {
        getCurrentSequenceFlowBuilder().from(element).to(target);
        currentSequenceFlowBuilder = null;
    }

    public B sequenceFlowId(String sequenceFlowId) {
        getCurrentSequenceFlowBuilder().id(sequenceFlowId);
        return myself;
    }

    private <T extends FlowNode> T createTarget(Class<T> typeClass) {
        return createTarget(typeClass, null);
    }

    protected <T extends FlowNode> T createTarget(Class<T> typeClass, String identifier) {
        T target = createSibling(typeClass, identifier);

        connectTarget(target);
        return target;
    }

    public ServiceTaskBuilder serviceTask() {
        return createTarget(ServiceTask.class).builder();
    }

    public ServiceTaskBuilder serviceTask(String id) {
        return createTarget(ServiceTask.class, id).builder();
    }

    public SendTaskBuilder sendTask() {
        return createTarget(SendTask.class).builder();
    }

    public SendTaskBuilder sendTask(String id) {
        return createTarget(SendTask.class, id).builder();
    }

    public UserTaskBuilder userTask() {
        return createTarget(UserTask.class).builder();
    }

    public UserTaskBuilder userTask(String id) {
        return createTarget(UserTask.class, id).builder();
    }

    public BusinessRuleTaskBuilder businessRuleTask() {
        return createTarget(BusinessRuleTask.class).builder();
    }

    public BusinessRuleTaskBuilder businessRuleTask(String id) {
        return createTarget(BusinessRuleTask.class, id).builder();
    }

    public ScriptTaskBuilder scriptTask() {
        return createTarget(ScriptTask.class).builder();
    }

    public ScriptTaskBuilder scriptTask(String id) {
        return createTarget(ScriptTask.class, id).builder();
    }

    public ReceiveTaskBuilder receiveTask() {
        return createTarget(ReceiveTask.class).builder();
    }

    public ReceiveTaskBuilder receiveTask(String id) {
        return createTarget(ReceiveTask.class, id).builder();
    }

    public ManualTaskBuilder manualTask() {
        return createTarget(ManualTask.class).builder();
    }

    public ManualTaskBuilder manualTask(String id) {
        return createTarget(ManualTask.class, id).builder();
    }

    public EndEventBuilder endEvent() {
        return createTarget(EndEvent.class).builder();
    }

    public EndEventBuilder endEvent(String id) {
        return createTarget(EndEvent.class, id).builder();
    }

    public ParallelGatewayBuilder parallelGateway() {
        return createTarget(ParallelGateway.class).builder();
    }

    public ParallelGatewayBuilder parallelGateway(String id) {
        return createTarget(ParallelGateway.class, id).builder();
    }

    public ExclusiveGatewayBuilder exclusiveGateway() {
        return createTarget(ExclusiveGateway.class).builder();
    }

    public InclusiveGatewayBuilder inclusiveGateway() {
        return createTarget(InclusiveGateway.class).builder();
    }

    public EventBasedGatewayBuilder eventBasedGateway() {
        return createTarget(EventBasedGateway.class).builder();
    }

    public ExclusiveGatewayBuilder exclusiveGateway(String id) {
        return createTarget(ExclusiveGateway.class, id).builder();
    }

    public InclusiveGatewayBuilder inclusiveGateway(String id) {
        return createTarget(InclusiveGateway.class, id).builder();
    }

    public IntermediateCatchEventBuilder intermediateCatchEvent() {
        return createTarget(IntermediateCatchEvent.class).builder();
    }

    public IntermediateCatchEventBuilder intermediateCatchEvent(String id) {
        return createTarget(IntermediateCatchEvent.class, id).builder();
    }

    public IntermediateThrowEventBuilder intermediateThrowEvent() {
        return createTarget(IntermediateThrowEvent.class).builder();
    }

    public IntermediateThrowEventBuilder intermediateThrowEvent(String id) {
        return createTarget(IntermediateThrowEvent.class, id).builder();
    }

    public CallActivityBuilder callActivity() {
        return createTarget(CallActivity.class).builder();
    }

    public CallActivityBuilder callActivity(String id) {
        return createTarget(CallActivity.class, id).builder();
    }

    public SubProcessBuilder subProcess() {
        return createTarget(SubProcess.class).builder();
    }

    public SubProcessBuilder subProcess(String id) {
        return createTarget(SubProcess.class, id).builder();
    }

    public TransactionBuilder transaction() {
        Transaction transaction = createTarget(Transaction.class);
        return new TransactionBuilder(modelInstance, transaction);
    }

    public TransactionBuilder transaction(String id) {
        Transaction transaction = createTarget(Transaction.class, id);
        return new TransactionBuilder(modelInstance, transaction);
    }

    public Gateway findLastGateway() {
        FlowNode lastGateway = element;
        while (true) {
            try {
                lastGateway = lastGateway.getPreviousNodes().singleResult();
                if (lastGateway instanceof Gateway) {
                    return (Gateway) lastGateway;
                }
            }
            catch (BpmnModelException e) {
                throw new BpmnModelException("Unable to determine an unique previous gateway of " + lastGateway.getId(), e);
            }
        }
    }

    @SuppressWarnings("rawtypes")
    public AbstractGatewayBuilder moveToLastGateway() {
        return findLastGateway().builder();
    }

    @SuppressWarnings("rawtypes")
    public AbstractFlowNodeBuilder moveToNode(String identifier) {
        ModelElementInstance instance = modelInstance.getModelElementById(identifier);
        if (instance instanceof FlowNode) {
            return ((FlowNode) instance).builder();
        } else {
            throw new BpmnModelException("Flow node not found for id " + identifier);
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public <T extends AbstractActivityBuilder> T moveToActivity(String identifier) {
        ModelElementInstance instance = modelInstance.getModelElementById(identifier);
        if (instance instanceof Activity) {
            return (T) ((Activity) instance).builder();
        } else {
            throw new BpmnModelException("Activity not found for id " + identifier);
        }
    }

    @SuppressWarnings("rawtypes")
    public AbstractFlowNodeBuilder connectTo(String identifier) {
        ModelElementInstance target = modelInstance.getModelElementById(identifier);
        if (target == null) {
            throw new BpmnModelException("Unable to connect " + element.getId() + " to element " + identifier + " cause it not exists.");
        }
        if (!(target instanceof FlowNode)) {
            throw new BpmnModelException("Unable to connect " + element.getId() + " to element " + identifier + " cause its not a flow node.");
        }
        FlowNode targetNode = (FlowNode) target;
        connectTarget(targetNode);
        return targetNode.builder();
    }

    /**
     * Sets the Flowable async attribute for the build flow node.
     *
     * @param async boolean value to set
     * @return the builder object
     */
    public B flowableAsync(boolean async) {
        element.setFlowableAsync(async);
        return myself;
    }

    /**
     * Sets the Flowable async attribute to true.
     *
     * @return the builder object
     */
    public B flowableAsync() {
        element.setFlowableAsync(true);
        return myself;
    }

    /**
     * Sets the Flowable exclusive attribute to true.
     *
     * @return the builder object
     */
    public B notFlowableExclusive() {
        element.setFlowableExclusive(false);
        return myself;
    }

    /**
     * Sets the Flowable exclusive attribute for the build flow node.
     *
     * @param exclusive boolean value to set
     * @return the builder object
     */
    public B flowableExclusive(boolean exclusive) {
        element.setFlowableExclusive(exclusive);
        return myself;
    }

    /**
     * Sets the Flowable failedJobRetryTimeCycle attribute for the build flow node.
     *
     * @param retryTimeCycle the retry time cycle value to set
     * @return the builder object
     */
    public B flowableFailedJobRetryTimeCycle(String retryTimeCycle) {
        FlowableFailedJobRetryTimeCycle failedJobRetryTimeCycle = createInstance(FlowableFailedJobRetryTimeCycle.class);
        failedJobRetryTimeCycle.setTextContent(retryTimeCycle);

        addExtensionElement(failedJobRetryTimeCycle);

        return myself;
    }

    @SuppressWarnings("rawtypes")
    public B flowableExecutionListenerClass(String eventName, Class listenerClass) {
        return flowableExecutionListenerClass(eventName, listenerClass.getName());
    }

    public B flowableExecutionListenerClass(String eventName, String fullQualifiedClassName) {
        FlowableExecutionListener executionListener = createInstance(FlowableExecutionListener.class);
        executionListener.setFlowableEvent(eventName);
        executionListener.setFlowableClass(fullQualifiedClassName);

        addExtensionElement(executionListener);

        return myself;
    }

    public B flowableExecutionListenerExpression(String eventName, String expression) {
        FlowableExecutionListener executionListener = createInstance(FlowableExecutionListener.class);
        executionListener.setFlowableEvent(eventName);
        executionListener.setFlowableExpression(expression);

        addExtensionElement(executionListener);

        return myself;
    }

    public B flowableExecutionListenerDelegateExpression(String eventName, String delegateExpression) {
        FlowableExecutionListener executionListener = createInstance(FlowableExecutionListener.class);
        executionListener.setFlowableEvent(eventName);
        executionListener.setFlowableDelegateExpression(delegateExpression);

        addExtensionElement(executionListener);

        return myself;
    }
}
