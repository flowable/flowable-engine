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

import org.flowable.bpm.model.bpmn.BpmnModelInstance;
import org.flowable.bpm.model.bpmn.instance.UserTask;
import org.flowable.bpm.model.bpmn.instance.flowable.FlowableFormData;
import org.flowable.bpm.model.bpmn.instance.flowable.FlowableFormField;
import org.flowable.bpm.model.bpmn.instance.flowable.FlowableTaskListener;

import java.util.List;

public abstract class AbstractUserTaskBuilder<B extends AbstractUserTaskBuilder<B>>
        extends AbstractTaskBuilder<B, UserTask> {

    protected AbstractUserTaskBuilder(BpmnModelInstance modelInstance, UserTask element, Class<?> selfType) {
        super(modelInstance, element, selfType);
    }

    /**
     * Sets the implementation of the build user task.
     *
     * @param implementation the implementation to set
     * @return the builder object
     */
    public B implementation(String implementation) {
        element.setImplementation(implementation);
        return myself;
    }

    /* Flowable extensions */

    /**
     * Sets the Flowable attribute assignee.
     *
     * @param flowableAssignee the assignee to set
     * @return the builder object
     */
    public B flowableAssignee(String flowableAssignee) {
        element.setFlowableAssignee(flowableAssignee);
        return myself;
    }

    /**
     * Sets the Flowable candidate groups attribute.
     *
     * @param flowableCandidateGroups the candidate groups to set
     * @return the builder object
     */
    public B flowableCandidateGroups(String flowableCandidateGroups) {
        element.setFlowableCandidateGroups(flowableCandidateGroups);
        return myself;
    }

    /**
     * Sets the Flowable candidate groups attribute.
     *
     * @param flowableCandidateGroups the candidate groups to set
     * @return the builder object
     */
    public B flowableCandidateGroups(List<String> flowableCandidateGroups) {
        element.setFlowableCandidateGroupsList(flowableCandidateGroups);
        return myself;
    }

    /**
     * Sets the Flowable candidate users attribute.
     *
     * @param flowableCandidateUsers the candidate users to set
     * @return the builder object
     */
    public B flowableCandidateUsers(String flowableCandidateUsers) {
        element.setFlowableCandidateUsers(flowableCandidateUsers);
        return myself;
    }

    /**
     * Sets the Flowable candidate users attribute.
     *
     * @param flowableCandidateUsers the candidate users to set
     * @return the builder object
     */
    public B flowableCandidateUsers(List<String> flowableCandidateUsers) {
        element.setFlowableCandidateUsersList(flowableCandidateUsers);
        return myself;
    }

    /**
     * Sets the Flowable due date attribute.
     *
     * @param flowableDueDate the due date of the user task
     * @return the builder object
     */
    public B flowableDueDate(String flowableDueDate) {
        element.setFlowableDueDate(flowableDueDate);
        return myself;
    }

    /**
     * Sets the Flowable form handler class attribute.
     *
     * @param flowableFormHandlerClass the class name of the form handler
     * @return the builder object
     */
    @SuppressWarnings("rawtypes")
    public B flowableFormHandlerClass(Class flowableFormHandlerClass) {
        return flowableFormHandlerClass(flowableFormHandlerClass.getName());
    }

    /**
     * Sets the Flowable form handler class attribute.
     *
     * @param fullQualifiedClassName the class name of the form handler
     * @return the builder object
     */
    public B flowableFormHandlerClass(String fullQualifiedClassName) {
        element.setFlowableFormHandlerClass(fullQualifiedClassName);
        return myself;
    }

    /**
     * Sets the Flowable form key attribute.
     *
     * @param flowableFormKey the form key to set
     * @return the builder object
     */
    public B flowableFormKey(String flowableFormKey) {
        element.setFlowableFormKey(flowableFormKey);
        return myself;
    }

    /**
     * Sets the Flowable priority attribute.
     *
     * @param flowablePriority the priority of the user task
     * @return the builder object
     */
    public B flowablePriority(String flowablePriority) {
        element.setFlowablePriority(flowablePriority);
        return myself;
    }

    /**
     * Creates a new Flowable form field extension element.
     *
     * @return the builder object
     */
    public FlowableUserTaskFormFieldBuilder flowableFormField() {
        FlowableFormData flowableFormData = getCreateSingleExtensionElement(FlowableFormData.class);
        FlowableFormField flowableFormField = createChild(flowableFormData, FlowableFormField.class);
        return new FlowableUserTaskFormFieldBuilder(modelInstance, element, flowableFormField);
    }

    /**
     * Add a class based task listener with specified event name
     *
     * @param eventName - event names to listen to
     * @param listenerClass - a string representing a class
     * @return the builder object
     */
    @SuppressWarnings("rawtypes")
    public B flowableTaskListenerClass(String eventName, Class listenerClass) {
        return flowableTaskListenerClass(eventName, listenerClass.getName());
    }

    /**
     * Add a class based task listener with specified event name
     *
     * @param eventName - event names to listen to
     * @param fullQualifiedClassName - a string representing a class
     * @return the builder object
     */
    public B flowableTaskListenerClass(String eventName, String fullQualifiedClassName) {
        FlowableTaskListener executionListener = createInstance(FlowableTaskListener.class);
        executionListener.setFlowableEvent(eventName);
        executionListener.setFlowableClass(fullQualifiedClassName);

        addExtensionElement(executionListener);

        return myself;
    }

    public B flowableTaskListenerExpression(String eventName, String expression) {
        FlowableTaskListener executionListener = createInstance(FlowableTaskListener.class);
        executionListener.setFlowableEvent(eventName);
        executionListener.setFlowableExpression(expression);

        addExtensionElement(executionListener);

        return myself;
    }

    public B flowableTaskListenerDelegateExpression(String eventName, String delegateExpression) {
        FlowableTaskListener executionListener = createInstance(FlowableTaskListener.class);
        executionListener.setFlowableEvent(eventName);
        executionListener.setFlowableDelegateExpression(delegateExpression);

        addExtensionElement(executionListener);

        return myself;
    }
}
