package org.flowable.idm.engine.impl.persistence.entity;

import org.flowable.common.engine.impl.persistence.entity.AbstractEntity;

public abstract class AbstractIdmEngineEntity extends AbstractEntity {

    public String getIdPrefix() {
        return IdmEngineEntityConstants.IDM_ENGINE_ID_PREFIX;
    }
}
