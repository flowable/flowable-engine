package org.flowable.app.engine.impl.persistence.entity;

import org.flowable.common.engine.impl.persistence.entity.AbstractEntityNoRevision;

public abstract class AbstractAppEngineNoRevisionEntity extends AbstractEntityNoRevision {

    public String getIdPrefix() {
        return AppEngineEntityConstants.APP_ENGINE_ID_PREFIX;
    }
}
