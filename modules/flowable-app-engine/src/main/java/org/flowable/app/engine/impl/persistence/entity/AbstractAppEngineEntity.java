package org.flowable.app.engine.impl.persistence.entity;

import org.flowable.common.engine.impl.persistence.entity.AbstractEntity;

public abstract class AbstractAppEngineEntity extends AbstractEntity {

    public String getIdPrefix() {
        return AppEngineEntityConstants.APP_ENGINE_ID_PREFIX;
    }
}
