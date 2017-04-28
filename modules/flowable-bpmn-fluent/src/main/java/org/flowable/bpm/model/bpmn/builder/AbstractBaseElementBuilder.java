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

import java.util.Collection;

import org.flowable.bpm.model.bpmn.BpmnModelException;
import org.flowable.bpm.model.bpmn.BpmnModelInstance;
import org.flowable.bpm.model.bpmn.instance.BaseElement;
import org.flowable.bpm.model.bpmn.instance.BpmnModelElementInstance;
import org.flowable.bpm.model.bpmn.instance.CompensateEventDefinition;
import org.flowable.bpm.model.bpmn.instance.Definitions;
import org.flowable.bpm.model.bpmn.instance.Error;
import org.flowable.bpm.model.bpmn.instance.ErrorEventDefinition;
import org.flowable.bpm.model.bpmn.instance.Escalation;
import org.flowable.bpm.model.bpmn.instance.EscalationEventDefinition;
import org.flowable.bpm.model.bpmn.instance.ExtensionElements;
import org.flowable.bpm.model.bpmn.instance.Message;
import org.flowable.bpm.model.bpmn.instance.MessageEventDefinition;
import org.flowable.bpm.model.bpmn.instance.Signal;
import org.flowable.bpm.model.bpmn.instance.SignalEventDefinition;

public abstract class AbstractBaseElementBuilder<B extends AbstractBaseElementBuilder<B, E>, E extends BaseElement>
        extends AbstractBpmnModelElementBuilder<B, E> {

    protected AbstractBaseElementBuilder(BpmnModelInstance modelInstance, E element, Class<?> selfType) {
        super(modelInstance, element, selfType);
    }

    protected <T extends BpmnModelElementInstance> T createInstance(Class<T> typeClass) {
        return modelInstance.newInstance(typeClass);
    }

    protected <T extends BaseElement> T createInstance(Class<T> typeClass, String identifier) {
        T instance = createInstance(typeClass);
        if (identifier != null) {
            instance.setId(identifier);
        }
        return instance;
    }

    protected <T extends BpmnModelElementInstance> T createChild(Class<T> typeClass) {
        return createChild(element, typeClass);
    }

    protected <T extends BaseElement> T createChild(Class<T> typeClass, String identifier) {
        return createChild(element, typeClass, identifier);
    }

    protected <T extends BpmnModelElementInstance> T createChild(BpmnModelElementInstance parent, Class<T> typeClass) {
        T instance = createInstance(typeClass);
        parent.addChildElement(instance);
        return instance;
    }

    protected <T extends BaseElement> T createChild(BpmnModelElementInstance parent, Class<T> typeClass, String identifier) {
        T instance = createInstance(typeClass, identifier);
        parent.addChildElement(instance);
        return instance;
    }

    protected <T extends BpmnModelElementInstance> T createSibling(Class<T> typeClass) {
        T instance = createInstance(typeClass);
        element.getParentElement().addChildElement(instance);
        return instance;
    }

    protected <T extends BaseElement> T createSibling(Class<T> typeClass, String identifier) {
        T instance = createInstance(typeClass, identifier);
        element.getParentElement().addChildElement(instance);
        return instance;
    }

    protected <T extends BpmnModelElementInstance> T getCreateSingleChild(Class<T> typeClass) {
        return getCreateSingleChild(element, typeClass);
    }

    protected <T extends BpmnModelElementInstance> T getCreateSingleChild(BpmnModelElementInstance parent, Class<T> typeClass) {
        Collection<T> childrenOfType = parent.getChildElementsByType(typeClass);
        if (childrenOfType.isEmpty()) {
            return createChild(parent, typeClass);
        } else {
            if (childrenOfType.size() > 1) {
                throw new BpmnModelException("Element " + parent + " of type " +
                        parent.getElementType().getTypeName() + " has more than one child element of type " +
                        typeClass.getName());
            } else {
                return childrenOfType.iterator().next();
            }
        }
    }

    protected <T extends BpmnModelElementInstance> T getCreateSingleExtensionElement(Class<T> typeClass) {
        ExtensionElements extensionElements = getCreateSingleChild(ExtensionElements.class);
        return getCreateSingleChild(extensionElements, typeClass);
    }

    protected Message findMessageForName(String messageName) {
        Collection<Message> messages = modelInstance.getModelElementsByType(Message.class);
        for (Message message : messages) {
            if (messageName.equals(message.getName())) {
                // return already existing message for message name
                return message;
            }
        }

        // create new message for non existing message name
        Definitions definitions = modelInstance.getDefinitions();
        Message message = createChild(definitions, Message.class);
        message.setName(messageName);

        return message;
    }

    protected MessageEventDefinition createMessageEventDefinition(String messageName) {
        Message message = findMessageForName(messageName);
        MessageEventDefinition messageEventDefinition = createInstance(MessageEventDefinition.class);
        messageEventDefinition.setMessage(message);
        return messageEventDefinition;
    }

    protected MessageEventDefinition createEmptyMessageEventDefinition() {
        return createInstance(MessageEventDefinition.class);
    }

    protected Signal findSignalForName(String signalName) {
        Collection<Signal> signals = modelInstance.getModelElementsByType(Signal.class);
        for (Signal signal : signals) {
            if (signalName.equals(signal.getName())) {
                // return already existing signal for signal name
                return signal;
            }
        }

        // create new signal for non existing signal name
        Definitions definitions = modelInstance.getDefinitions();
        Signal signal = createChild(definitions, Signal.class);
        signal.setName(signalName);

        return signal;
    }

    protected SignalEventDefinition createSignalEventDefinition(String signalName) {
        Signal signal = findSignalForName(signalName);
        SignalEventDefinition signalEventDefinition = createInstance(SignalEventDefinition.class);
        signalEventDefinition.setSignal(signal);
        return signalEventDefinition;
    }

    protected ErrorEventDefinition findErrorDefinitionForCode(String errorCode) {
        Collection<ErrorEventDefinition> definitions = modelInstance.getModelElementsByType(ErrorEventDefinition.class);
        for (ErrorEventDefinition definition : definitions) {
            Error error = definition.getError();
            if (error != null && error.getErrorCode().equals(errorCode)) {
                return definition;
            }
        }
        return null;
    }

    protected Error findErrorForNameAndCode(String errorCode) {
        Collection<Error> errors = modelInstance.getModelElementsByType(Error.class);
        for (Error error : errors) {
            if (errorCode.equals(error.getErrorCode())) {
                // return already existing error
                return error;
            }
        }

        // create new error
        Definitions definitions = modelInstance.getDefinitions();
        Error error = createChild(definitions, Error.class);
        error.setErrorCode(errorCode);

        return error;
    }

    protected ErrorEventDefinition createEmptyErrorEventDefinition() {
        return createInstance(ErrorEventDefinition.class);
    }

    protected ErrorEventDefinition createErrorEventDefinition(String errorCode) {
        Error error = findErrorForNameAndCode(errorCode);
        ErrorEventDefinition errorEventDefinition = createInstance(ErrorEventDefinition.class);
        errorEventDefinition.setError(error);
        return errorEventDefinition;
    }

    protected Escalation findEscalationForCode(String escalationCode) {
        Collection<Escalation> escalations = modelInstance.getModelElementsByType(Escalation.class);
        for (Escalation escalation : escalations) {
            if (escalationCode.equals(escalation.getEscalationCode())) {
                // return already existing escalation
                return escalation;
            }
        }

        Definitions definitions = modelInstance.getDefinitions();
        Escalation escalation = createChild(definitions, Escalation.class);
        escalation.setEscalationCode(escalationCode);
        return escalation;
    }

    protected EscalationEventDefinition createEscalationEventDefinition(String escalationCode) {
        Escalation escalation = findEscalationForCode(escalationCode);
        EscalationEventDefinition escalationEventDefinition = createInstance(EscalationEventDefinition.class);
        escalationEventDefinition.setEscalation(escalation);
        return escalationEventDefinition;
    }

    protected CompensateEventDefinition createCompensateEventDefinition() {
        return createInstance(CompensateEventDefinition.class);
    }

    /**
     * Sets the identifier of the element.
     *
     * @param identifier the identifier to set
     * @return the builder object
     */
    public B id(String identifier) {
        element.setId(identifier);
        return myself;
    }

    /**
     * Add an extension element to the element.
     *
     * @param extensionElement the extension element to add
     * @return the builder object
     */
    public B addExtensionElement(BpmnModelElementInstance extensionElement) {
        ExtensionElements extensionElements = getCreateSingleChild(ExtensionElements.class);
        extensionElements.addChildElement(extensionElement);
        return myself;
    }
}
