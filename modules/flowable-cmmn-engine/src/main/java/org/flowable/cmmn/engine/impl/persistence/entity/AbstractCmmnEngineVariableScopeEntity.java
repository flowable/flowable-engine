package org.flowable.cmmn.engine.impl.persistence.entity;

import org.flowable.variable.service.impl.persistence.entity.VariableScopeImpl;

public abstract class AbstractCmmnEngineVariableScopeEntity extends VariableScopeImpl {

    private static final long serialVersionUID = 1L;

    public String getIdPrefix() {
        return CmmnEngineEntityConstants.CMMN_ENGINE_ID_PREFIX;
    }
}
