package org.flowable.engine.impl.eventregistry;

import org.flowable.bpmn.model.BaseElement;
import org.flowable.eventregistry.api.runtime.EventInstance;
import org.flowable.variable.api.delegate.VariableScope;

public interface BpmnEventInstanceOutParameterHandler {

    void handleOutParameters(VariableScope variableScope, BaseElement baseElement, EventInstance eventInstance);
}
