package org.flowable.dmn.engine.impl.persistence.entity;

import org.flowable.common.engine.impl.persistence.entity.AbstractEntityNoRevision;

public abstract class AbstractDmnEngineNoRevisionEntity extends AbstractEntityNoRevision {

    public String getIdPrefix() {
        return DmnEngineEntityConstants.DMN_ENGINE_ID_PREFIX;
    }
}
