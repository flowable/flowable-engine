package org.flowable.engine.impl.persistence.entity;

import org.flowable.common.engine.impl.persistence.entity.AbstractEntity;

public abstract class AbstractBpmnEngineEntity extends AbstractEntity {

    public String getIdPrefix() {
        return BpmnEngineEntityConstants.BPMN_ENGINE_ID_PREFIX;
    }
}
