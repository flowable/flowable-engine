package org.flowable.variable.service.impl.persistence.entity;

import org.flowable.common.engine.impl.persistence.entity.AbstractEntity;

public abstract class AbstractVariableServiceEntity extends AbstractEntity {

    public String getIdPrefix() {
        return VariableServiceEntityConstants.VARIABLE_SERVICE_ID_PREFIX;
    }
}
