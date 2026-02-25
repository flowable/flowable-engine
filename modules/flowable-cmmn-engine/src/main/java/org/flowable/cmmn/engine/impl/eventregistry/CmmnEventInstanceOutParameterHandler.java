package org.flowable.cmmn.engine.impl.eventregistry;

import org.flowable.cmmn.model.BaseElement;
import org.flowable.eventregistry.api.runtime.EventInstance;
import org.flowable.variable.api.delegate.VariableScope;

public interface CmmnEventInstanceOutParameterHandler {

    void handleOutParameters(VariableScope variableScope, BaseElement baseElement, EventInstance eventInstance);

}
