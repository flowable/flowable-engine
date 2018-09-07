package org.flowable.cmmn.engine.impl.persistence.entity;

import org.flowable.common.engine.impl.persistence.entity.AbstractEntity;

public abstract class AbstractCmmnEngineEntity extends AbstractEntity {

    public String getIdPrefix() {
        return CmmnEngineEntityConstants.CMMN_ENGINE_ID_PREFIX;
    }
}
