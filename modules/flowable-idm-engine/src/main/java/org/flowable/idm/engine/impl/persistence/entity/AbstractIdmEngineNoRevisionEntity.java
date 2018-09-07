package org.flowable.idm.engine.impl.persistence.entity;

import org.flowable.common.engine.impl.persistence.entity.AbstractEntityNoRevision;

public abstract class AbstractIdmEngineNoRevisionEntity extends AbstractEntityNoRevision {

    public String getIdPrefix() {
        return IdmEngineEntityConstants.IDM_ENGINE_ID_PREFIX;
    }
}
