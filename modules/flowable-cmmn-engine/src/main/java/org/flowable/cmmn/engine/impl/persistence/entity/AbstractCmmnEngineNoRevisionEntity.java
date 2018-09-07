package org.flowable.cmmn.engine.impl.persistence.entity;

import org.flowable.common.engine.impl.persistence.entity.AbstractEntityNoRevision;

public abstract class AbstractCmmnEngineNoRevisionEntity extends AbstractEntityNoRevision {

    public String getIdPrefix() {
        return CmmnEngineEntityConstants.CMMN_ENGINE_ID_PREFIX;
    }
}
