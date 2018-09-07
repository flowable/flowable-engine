package org.flowable.engine.impl.persistence.entity;

import org.flowable.variable.service.impl.persistence.entity.VariableScopeImpl;

public abstract class AbstractBpmnEngineVariableScopeEntity extends VariableScopeImpl {

    private static final long serialVersionUID = 1L;

    public String getIdPrefix() {
        return BpmnEngineEntityConstants.BPMN_ENGINE_ID_PREFIX;
    }
}
